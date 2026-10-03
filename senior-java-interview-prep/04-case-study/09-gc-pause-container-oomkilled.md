# Case 09 — Tăng `-Xmx` xong thì GC pause dài hơn, rồi pod bị Kubernetes `OOMKilled`

> **Chủ đề:** Đọc GC log, G1 tuning, humongous allocation, ZGC, `MaxRAMPercentage`, bộ nhớ ngoài heap (direct, metaspace, thread, GC), NMT, CPU throttling
> **Module liên quan:** [M05 §8 — Các Garbage Collector & cách chọn](../01-giao-trinh/05-jvm-memory-gc-performance.md#p8) · [M05 §7 — Nền tảng GC](../01-giao-trinh/05-jvm-memory-gc-performance.md#p7) · [M05 §3 — Runtime data areas](../01-giao-trinh/05-jvm-memory-gc-performance.md#p3) · [M05 §11 — JVM flags & JVM trong container](../01-giao-trinh/05-jvm-memory-gc-performance.md#p11) · [M05 §14 — Runbook GC cao](../01-giao-trinh/05-jvm-memory-gc-performance.md#p14) · [M16 §6 — JVM trong container](../01-giao-trinh/16-devops-build-cloud-security.md#p6)
> **Độ khó:** ⭐⭐⭐⭐ (Senior)
> **Thời gian tự giải gợi ý:** 45 phút

---

## 1. Bối cảnh hệ thống

`reco-service` trả gợi ý sản phẩm cho trang chủ và trang chi tiết sản phẩm của một sàn thương mại điện tử.

| Thành phần | Chi tiết |
|---|---|
| Stack | Java 21, Spring Boot 3.2 (WebFlux/Netty), Lettuce (Redis), Elasticsearch Java client, Kafka consumer cập nhật model |
| Bộ nhớ ứng dụng | Feature store trong heap ~1,1 GB (vector đặc trưng của 2 triệu sản phẩm, cập nhật mỗi 10 phút), response JSON 200 KB – 2 MB |
| Triển khai | K8s 10 pod; `requests = limits`: **memory 4Gi**, CPU 2 |
| JVM ban đầu | `-XX:+UseG1GC -Xms2g -Xmx2g`, GC log bật sẵn |
| SLO | p99 < 250 ms ở 3.000 RPS toàn cụm |

**Thay đổi gây ra sự cố:** để "giảm số lần GC" trước mùa sale, một PR đổi Dockerfile:
```diff
-ENV JAVA_OPTS="-XX:+UseG1GC -Xms2g -Xmx2g"
+ENV JAVA_OPTS="-XX:+UseG1GC -Xms3584m -Xmx3584m -XX:MaxGCPauseMillis=500"
```
Lý do ghi trong PR: "Pod có 4Gi mà heap chỉ 2g thì phí; GC log thấy young GC 3–4 lần/giây; nới pause goal để G1 bớt áp lực".

## 2. Triệu chứng

**Sau deploy 2 ngày:**
```
reco-service p99 latency:      180 ms → 1.4 s (dao động, có đỉnh 3 s)
jvm_gc_pause_seconds_max:      45 ms  → 1.2 s (young), 2.8 s (full)
Số lần young GC/giây:          3.5    → 1.1     ← mục tiêu "GC ít hơn" đạt được, nhưng…
Pod restart:                   0      → 31 lần/ngày toàn cụm
```

**`kubectl describe pod reco-service-6b9d-tx7mn`:**
```
    Last State:     Terminated
      Reason:       OOMKilled
      Exit Code:    137
      Started:      Tue, 06 Oct 2026 09:12:40 +0700
      Finished:     Tue, 06 Oct 2026 13:47:02 +0700
    Restart Count:  4
```
Không có `OutOfMemoryError` trong log, **không có heap dump** dù đã bật `-XX:+HeapDumpOnOutOfMemoryError`.

**Grafana — một pod trước khi bị kill:**
```
container_memory_working_set_bytes    3.62 GB → 3.81 → 3.95 → 4.02 GB (bị kill)
jvm_memory_used_bytes{area="heap"}    dao động 1.4 – 3.4 GB (bình thường)
jvm_memory_used_bytes{area="nonheap"} ~ 290 MB (ổn định)
jvm_buffer_memory_used_bytes{id="direct"}  120 MB → 410 MB lúc giờ cao điểm
container_cpu_cfs_throttled_periods (tỷ lệ)  8% → 34%
```

## 3. Câu hỏi đặt ra

1. Vì sao heap lớn hơn lại cho pause **dài hơn**? Đọc GC log thế nào để chứng minh?
2. Vì sao pod bị kill khi heap vẫn còn chỗ, và không có `OutOfMemoryError`?
3. Bộ nhớ của một process Java trong container gồm những gì? Tính lại ngân sách cho limit 4Gi.
4. Nên tune G1 hay chuyển ZGC? Đánh đổi là gì?

> ✋ **Dừng lại và tự giải trước.** Viết công thức `container limit ≥ ?` cho JVM, điền số của case này, và chỉ ra con số nào đã bị quên.

## 4. Điều tra từng bước

### Bước 1 — Hai sự cố, hai cơ chế
Tách bạch ngay từ đầu:
| | GC pause dài | OOMKilled |
|---|---|---|
| Ai gây ra | JVM (stop-the-world) | Kernel cgroup OOM killer |
| Bằng chứng | GC log, `jvm_gc_pause_seconds` | `Reason: OOMKilled`, exit 137, `container_memory_working_set_bytes` chạm limit |
| Vùng nhớ | Heap | **Tổng** RSS của process (heap + mọi thứ ngoài heap) |

### Bước 2 — Đọc GC log: vì sao pause dài?
GC log (`-Xlog:gc*:file=/logs/gc.log:time,uptime,level,tags`) lúc cao điểm:
```
[2026-10-06T11:02:17.884+0700][7311.402s][info][gc,start    ] GC(5120) Pause Young (Normal) (G1 Evacuation Pause)
[2026-10-06T11:02:17.884+0700][7311.402s][info][gc,heap     ] GC(5120) Eden regions: 1890->0(1874)
[2026-10-06T11:02:17.884+0700][7311.402s][info][gc,heap     ] GC(5120) Survivor regions: 96->118(237)
[2026-10-06T11:02:17.884+0700][7311.402s][info][gc,heap     ] GC(5120) Old regions: 1190->1214
[2026-10-06T11:02:17.884+0700][7311.402s][info][gc,heap     ] GC(5120) Humongous regions: 212->37
[2026-10-06T11:02:17.884+0700][7311.402s][info][gc,phases   ] GC(5120)   Evacuate Collection Set: 702.5ms
[2026-10-06T11:02:17.884+0700][7311.402s][info][gc,phases   ] GC(5120)   Post Evacuate Collection Set: 31.2ms
[2026-10-06T11:02:18.612+0700][7312.130s][info][gc          ] GC(5120) Pause Young (Normal) (G1 Evacuation Pause) 3388M->1369M(3584M) 728.114ms
[2026-10-06T11:02:18.612+0700][7312.130s][info][gc,cpu      ] GC(5120) User=0.62s Sys=0.03s Real=0.73s
...
[2026-10-06T11:05:41.207+0700][7514.725s][info][gc          ] GC(5161) Pause Young (Concurrent Start) (G1 Humongous Allocation) 2950M->1402M(3584M) 488.903ms
[2026-10-06T11:09:03.551+0700][7717.069s][info][gc          ] GC(5203) To-space exhausted
[2026-10-06T11:09:06.402+0700][7719.920s][info][gc          ] GC(5204) Pause Full (G1 Compaction Pause) 3561M->1311M(3584M) 2847.551ms
```
Đọc từng điểm:
1. **Eden 1.890 region** (region 1 MB) ≈ 1,9 GB — trước đây với heap 2 GB, eden chỉ ~600 MB. G1 tự co giãn young gen theo pause goal; `MaxGCPauseMillis=500` cho phép nó chọn eden rất lớn.
2. **Pause young tỷ lệ với lượng object còn sống phải copy**, không phải kích thước eden. Workload này có nhiều object "sống vừa" (vector đặc trưng của lần refresh model, batch Kafka đang xử lý, response đang ghi) → eden lớn hơn = thời gian giữa hai lần GC dài hơn = **nhiều object còn sống tại thời điểm GC** hơn → copy nhiều hơn (Evacuate Collection Set 702 ms).
3. **`User=0.62s Real=0.73s`** với 2 GC worker thread: lẽ ra `Real ≈ User / 2 ≈ 0,31 s`. `Real` gần bằng `User` nghĩa là các thread GC **không chạy song song được** — chúng bị **CFS throttle** (pod limit 2 CPU, mà ứng dụng + Netty event loop + GC cùng tranh quota). Metric throttling 34% xác nhận.
4. **Humongous regions 212**: với heap 3,5 GB, G1 chọn region 1 MB → mọi object ≥ 512 KB (một nửa region) là humongous: `byte[]` của response JSON 600 KB – 2 MB, mảng `float[]` vector lớn. Humongous được cấp phát thẳng vào old gen, kích hoạt concurrent cycle sớm, gây phân mảnh.
5. **`To-space exhausted` → `Pause Full` 2,8 s**: không còn region trống để evacuate → G1 rơi vào Full GC (song song từ JDK 10 — JEP 307 — nhưng vẫn stop-the-world, mark-compact toàn bộ heap 3,5 GB). Heap lớn → Full GC lâu hơn.

Công cụ hỗ trợ: GCeasy/GCViewer để vẽ phân bố pause; JFR (`jdk.GCPhasePause`, `jdk.ObjectAllocationOutsideTLAB`) để tìm ai cấp phát object lớn:
```bash
jcmd 1 JFR.start name=gc duration=120s settings=profile filename=/dumps/gc.jfr
jfr print --events jdk.ObjectAllocationOutsideTLAB /dumps/gc.jfr | grep -A3 'objectClass = byte\[\]' | head
# stackTrace: ... com.fasterxml.jackson.databind.ObjectMapper.writeValueAsBytes ← RecoController.render
```

### Bước 3 — Vì sao OOMKilled? Bật Native Memory Tracking
Thêm `-XX:NativeMemoryTracking=summary` cho 1 pod canary (chi phí ~vài % CPU/bộ nhớ), đợi tới giờ cao điểm:
```bash
$ kubectl exec reco-service-canary -- jcmd 1 VM.native_memory summary scale=MB
Native Memory Tracking:

Total: reserved=6020MB, committed=4515MB
-                 Java Heap (reserved=3584MB, committed=3584MB)
-                     Class (reserved=1046MB, committed=181MB)       (classes #31204)
-                    Thread (reserved=263MB, committed=38MB)         (thread #259)
-                      Code (reserved=244MB, committed=72MB)
-                        GC (reserved=176MB, committed=176MB)
-                  Compiler (reserved=4MB, committed=4MB)
-                  Internal (reserved=11MB, committed=11MB)
-                     Other (reserved=402MB, committed=402MB)        ← direct ByteBuffer (Netty, Lettuce, ES client)
-                    Symbol (reserved=27MB, committed=27MB)
-    Native Memory Tracking (reserved=6MB, committed=6MB)
-        Shared class space (reserved=13MB, committed=13MB)
-               Arena Chunk (reserved=1MB, committed=1MB)
```
`committed` 4.515 MB > 4.096 MB limit. Process chưa bị kill ngay vì không phải trang nào committed cũng đã thực sự được chạm tới (RSS < committed) — nhưng khi heap được dùng hết lúc cao điểm và Netty cấp thêm direct buffer, RSS vượt 4 GiB → kernel giết process. Kernel không ném exception cho JVM → không có `OutOfMemoryError`, không có heap dump.

Ngân sách thực tế:
```
Heap (Xmx)                                   3584 MB   (87,5% limit!)
Metaspace + class space                       ~180 MB
Code cache                                    ~ 72 MB
GC data structures (G1 remembered sets…)      ~176 MB   (tỷ lệ với heap — heap to hơn thì vùng này to hơn)
Thread stacks (259 thread, phần đã chạm)      ~ 40 MB
Direct buffers (Netty/Lettuce/ES)             120–410 MB (MaxDirectMemorySize mặc định ≈ Xmx → gần như KHÔNG giới hạn)
Khác (symbol, internal, malloc arena glibc)   ~ 50–150 MB
-------------------------------------------------------------
Tổng lúc cao điểm                            ≈ 4.2–4.6 GB  > 4.0 GiB
```
Với `-Xmx2g` trước đây, tổng ≈ 2,9–3,1 GB — dư 1 GB nên không ai để ý đến phần ngoài heap.

### Bước 4 — Đối chiếu metric
`container_memory_working_set_bytes − (heap used + nonheap used)` ≈ 600–700 MB — đúng bằng phần direct + GC + thread + code mà Micrometer `jvm_memory_used_bytes` không bao quát đầy đủ. Bài học: **heap metric không phải memory metric của container**.

## 5. Nguyên nhân gốc

1. **Đặt `-Xmx` bằng 87,5% memory limit** mà không tính bộ nhớ ngoài heap; `MaxDirectMemorySize` để mặc định (≈ heap) nên direct memory không có trần.
2. **Nới `MaxGCPauseMillis=500`** khiến G1 chọn young gen rất lớn; với workload nhiều object sống vừa, mỗi lần young GC phải copy nhiều hơn → pause dài hơn.
3. **Region 1 MB + response/vector lớn** → humongous allocation dày đặc → concurrent cycle sớm, phân mảnh, `To-space exhausted` → Full GC.
4. **CPU limit 2** → GC song song bị throttle, pause kéo dài thêm.
5. Thay đổi JVM flags không qua load test với cấu hình container thật.

## 6. Giải pháp

### Ngắn hạn (trong ngày)
```bash
JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=62 -XX:InitialRAMPercentage=62 \
           -XX:MaxDirectMemorySize=384m -XX:MaxMetaspaceSize=256m -XX:ReservedCodeCacheSize=128m \
           -XX:+ExitOnOutOfMemoryError -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/dumps"
```
- Heap ≈ 2,5 GB (62% × 4 GiB), bỏ `MaxGCPauseMillis=500` (về mặc định 200 ms).
- Trần direct memory → nếu Netty vượt, JVM ném `OutOfMemoryError: Cannot reserve ... bytes of direct buffer memory` (có log, có restart sạch) thay vì bị kernel giết âm thầm.
- `MaxRAMPercentage` thay `-Xmx` cứng → đổi limit trong K8s là heap đổi theo. (Nếu có cả hai, `-Xmx` thắng.)

### Dài hạn
**(1) Giảm nguồn gốc áp lực bộ nhớ:**
- Ghi response JSON **streaming** thẳng ra Netty buffer (`Jackson2JsonEncoder` của WebFlux với `Flux`) thay vì `writeValueAsBytes` toàn bộ → hết `byte[]` 2 MB.
- Feature store: chuyển vector sang mảng nguyên thủy liền khối, cập nhật model theo kiểu "xây bản mới rồi đổi tham chiếu" ngoài giờ cao điểm; cân nhắc off-heap (memory-mapped file) cho bảng đặc trưng lớn.
- `-XX:G1HeapRegionSize=4m` → ngưỡng humongous lên 2 MB, phần lớn object lớn còn lại thành object thường.

**(2) Thử nghiệm Generational ZGC trên canary (JDK 21):**
```bash
JAVA_OPTS="-XX:+UseZGC -XX:+ZGenerational -XX:MaxRAMPercentage=60 -XX:MaxDirectMemorySize=384m ..."
# JDK 23+: generational là mặc định của ZGC; JDK 24 bỏ chế độ non-generational
```
Kết quả canary 72 giờ (cùng tải):

| Chỉ số | G1 (sau tune) | Generational ZGC |
|---|---|---|
| GC pause p99 / max | 38 ms / 110 ms | 0,05 ms / 0,4 ms |
| p99 latency API | 210 ms | 160 ms |
| CPU trung bình pod | 52% | 61% |
| Allocation stall | – | 0 (sau khi để heap 60% + CPU 3) |
| RSS | 3,3 GB | 3,5 GB |

Quyết định: dùng ZGC cho `reco-service` (latency là ưu tiên, chấp nhận +9% CPU và nâng CPU limit lên 3); giữ G1 cho các service batch/throughput.

**(3) CPU:** nâng `limits.cpu` lên 3 (hoặc bỏ CPU limit, giữ `requests`) và đặt `-XX:ActiveProcessorCount` khớp; theo dõi throttling.

**(4) Quy tắc ngân sách bộ nhớ** (đưa vào base image/Helm chart chung):
```
limits.memory ≥ Heap + MaxMetaspaceSize + MaxDirectMemorySize + ReservedCodeCacheSize
                + (số thread × ~1 MB) + GC overhead (~5–10% heap) + ~150 MB dự phòng
```
và **đo** bằng NMT dưới tải trước khi chốt, không chỉ cộng lý thuyết.

### So sánh lựa chọn
| Lựa chọn | Ưu | Nhược | Khi nào |
|---|---|---|---|
| Tăng heap | Ít GC hơn (về số lần) | Pause không chắc ngắn hơn; Full GC dài hơn; ăn vào phần ngoài heap → OOMKilled | Khi live set thực sự sát Xmx và còn RAM ngoài heap |
| Tune G1 (`MaxGCPauseMillis`, `G1HeapRegionSize`, `G1NewSizePercent`, IHOP) | Không đổi collector, rủi ro thấp | Hiệu quả có giới hạn, dễ "tune mù" | Mặc định cho hầu hết service; đổi từng flag một, có số liệu |
| Generational ZGC | Pause < 1 ms, gần như không phụ thuộc kích thước heap | Tốn CPU hơn, cần headroom heap (nếu không → allocation stall), không dùng compressed oops | Service nhạy latency, heap trung bình–lớn, JDK 21+ |
| Parallel GC | Throughput cao nhất | Pause dài, tỷ lệ với heap | Batch, job xử lý dữ liệu không nhạy latency |
| Giảm allocation (streaming, tái sử dụng buffer, cấu trúc gọn) | Giải quyết gốc, lợi cho mọi collector | Tốn công sửa code | Luôn là bước đầu khi allocation rate cao |
| Tăng memory limit pod | Đơn giản | Tốn tiền × số pod; không chữa direct memory không trần | Sau khi đã đo ngân sách thật |

### Kết quả
Pause p99 từ 1,2 s xuống < 1 ms (ZGC), p99 API 160 ms, 0 OOMKilled trong 30 ngày; ngân sách bộ nhớ trở thành một phần của Helm chart chung.

## 7. Phòng ngừa

**Alert:**
```promql
# RSS container gần limit (cảnh báo trước khi bị kill)
max by (pod) (container_memory_working_set_bytes{container="app"} / container_spec_memory_limit_bytes{container="app"}) > 0.9
# Pod bị OOMKilled (kube-state-metrics)
increase(kube_pod_container_status_restarts_total[15m]) > 0
  and on (pod) kube_pod_container_status_last_terminated_reason{reason="OOMKilled"} == 1
# GC pause
max by (pod) (jvm_gc_pause_seconds_max) > 0.3
# CPU throttling
rate(container_cpu_cfs_throttled_periods_total[5m]) / rate(container_cpu_cfs_periods_total[5m]) > 0.2
# Direct memory
jvm_buffer_memory_used_bytes{id="direct"} / jvm_buffer_total_capacity_bytes{id="direct"} > 0.9   # hoặc so với MaxDirectMemorySize
```

**Quy trình thay đổi JVM flag:**
- Mọi thay đổi flag là một **thay đổi hiệu năng**: cần giả thuyết, load test trên cấu hình container thật (cùng limit CPU/RAM), số liệu trước/sau (pause p99/max, allocation rate, RSS, throttling), canary rồi mới rollout.
- Chỉ đổi **một** tham số mỗi lần.
- Base image chung đặt sẵn: `MaxRAMPercentage`, `MaxDirectMemorySize`, `MaxMetaspaceSize`, GC log, `ExitOnOutOfMemoryError`, heap dump vào volume; cấm `-Xmx` cứng trong Dockerfile của từng service (lint trong CI).

**Code review checklist:**
- [ ] Có tạo `byte[]`/`String` lớn (toàn bộ response, file) thay vì streaming không?
- [ ] Thư viện dùng Netty/NIO (Redis, ES, gRPC, Kafka): đã tính direct memory chưa?
- [ ] Thay đổi `-Xmx`/`MaxRAMPercentage`/limit: đã tính lại ngân sách ngoài heap chưa?

## 8. Cách kể lại trong phỏng vấn (STAR, ~2 phút)

- **S:** "Service gợi ý sản phẩm của chúng tôi chạy trên pod 4Gi, heap 2 GB. Một PR tăng heap lên 3,5 GB và nới pause goal của G1 lên 500 ms để 'giảm số lần GC'. Hai ngày sau p99 từ 180 ms lên 1,4 giây, GC pause lên tới 1,2 giây, và pod bị Kubernetes OOMKilled 31 lần/ngày mà không có OutOfMemoryError hay heap dump."
- **T:** "Tôi được giao phân tích và đưa ra cấu hình JVM đúng cho mùa sale."
- **A:** "Tôi tách hai vấn đề. Với pause: GC log cho thấy eden phình lên 1,9 GB vì pause goal 500 ms, mỗi young GC phải copy nhiều object sống vừa hơn; `User` gần bằng `Real` chứng tỏ GC thread bị CPU throttle; 212 humongous region do response JSON lớn trên region 1 MB; cuối cùng là `To-space exhausted` và Full GC 2,8 giây. Với OOMKilled: bật NMT trên canary, tổng committed 4,5 GB — heap 3,5 GB cộng 400 MB direct buffer của Netty không có trần, GC structures, metaspace, code cache. Tôi hạ heap xuống 62% bằng `MaxRAMPercentage`, đặt trần direct/metaspace/code cache, bỏ pause goal 500. Dài hạn, ghi JSON dạng streaming, tăng region size, và chạy canary Generational ZGC 72 giờ so với G1 đã tune."
- **R:** "Với ZGC, pause p99 dưới 1 ms, p99 API 160 ms, đánh đổi 9% CPU; 0 OOMKilled trong 30 ngày. Tôi đưa công thức ngân sách bộ nhớ và các trần ngoài heap vào base image chung, cấm `-Xmx` cứng, và thêm alert working set > 90% limit."

## 9. Câu hỏi mở rộng

<details>
<summary>1. Heap lớn hơn có bao giờ làm pause ngắn hơn không?</summary>

Có thể — nếu nguyên nhân pause là **thiếu chỗ** (live set sát Xmx khiến GC chạy liên tục, mixed GC không kịp, Full GC do evacuation failure), thì thêm headroom giúp G1 có region trống, giảm Full GC. Nhưng pause của young/mixed GC tỷ lệ với lượng object sống phải copy và kích thước remembered set, không tỷ lệ nghịch với heap. Và Full GC (nếu xảy ra) dài hơn với heap lớn. Quy tắc: heap ≈ 2–3 lần live set sau GC cho G1; đo trước khi tăng.
</details>

<details>
<summary>2. Vì sao OOMKilled không có heap dump? Làm sao điều tra khi pod đã chết?</summary>

Kernel gửi SIGKILL — JVM không có cơ hội chạy bất kỳ code nào (không shutdown hook, không heap dump, không flush log). Điều tra bằng: metric lịch sử (`container_memory_working_set_bytes`, `jvm_*`), NMT trên một pod canary chạy cùng tải (`VM.native_memory baseline` rồi `summary.diff`), JFR liên tục ghi ra volume (mất vài giây cuối nhưng có xu hướng), và giảm heap tạm thời để phần tăng trưởng ngoài heap lộ ra dưới dạng Java OOM có kiểm soát (đặt `MaxDirectMemorySize` để biến direct leak thành `OutOfMemoryError: Cannot reserve ... direct buffer memory`).
</details>

<details>
<summary>3. Trong GC log, <code>User</code>, <code>Sys</code>, <code>Real</code> nói lên điều gì?</summary>

`User`/`Sys` là tổng CPU time của **mọi** GC thread; `Real` là thời gian đồng hồ. Với N thread song song hiệu quả, `Real ≈ (User + Sys) / N`. Nếu `Real ≈ User + Sys` hoặc lớn hơn → GC thread không được chạy song song: CPU throttling (cgroup quota), máy quá tải, hoặc chỉ có 1 CPU khả dụng. Nếu `Real` lớn hơn hẳn `User + Sys` → thread đang chờ thứ khác: swap, I/O (ghi log GC lên đĩa chậm), page fault (heap chưa pre-touch), hoặc hypervisor steal time. Đây là cách phân biệt "GC làm nhiều việc" với "GC bị môi trường làm chậm".
</details>

<details>
<summary>4. G1, ZGC, Shenandoah, Parallel — chọn thế nào cho service Spring Boot?</summary>

G1 là mặc định hợp lý cho hầu hết service (cân bằng throughput/latency, pause mục tiêu ~200 ms, heap vài trăm MB tới vài chục GB). ZGC (Generational từ JDK 21) khi latency tail là ưu tiên và chấp nhận thêm CPU/bộ nhớ: pause dưới mili-giây bất kể heap. Shenandoah (có trong build OpenJDK của Red Hat/Temurin…) cùng mục tiêu như ZGC. Parallel cho batch cần throughput tối đa. Serial chỉ cho container rất nhỏ (JVM tự chọn khi < 2 CPU hoặc < ~1792 MB). Luôn chốt bằng load test trên cấu hình container thật, so pause p99/max, CPU, RSS và throughput.
</details>

<details>
<summary>5. <code>-Xms</code> bằng <code>-Xmx</code> trên Kubernetes có nên không? Còn <code>AlwaysPreTouch</code>?</summary>

Với `requests = limits` cho memory (khuyến nghị cho Java vì memory không nén được), đặt heap ban đầu bằng tối đa (`InitialRAMPercentage = MaxRAMPercentage`) giúp tránh resize heap lúc chạy và lộ vấn đề thiếu RAM ngay khi khởi động. `-XX:+AlwaysPreTouch` chạm mọi trang heap lúc khởi động: tránh page fault (và tránh "RSS tăng dần" gây hiểu nhầm) nhưng khởi động chậm hơn và RSS cao ngay từ đầu — hợp với service chạy dài, cần latency ổn định; cân nhắc với startup probe.
</details>

<details>
<summary>6. Java 8 trong container khác gì?</summary>

Trước **8u191**, JVM không đọc giới hạn cgroup: heap mặc định = 1/4 RAM **của node** (64 GB node → 16 GB heap trong container 2 GB) → OOMKilled gần như chắc chắn; `availableProcessors()` trả số CPU của node → quá nhiều GC/ForkJoin thread. 8u131–8u190 có cờ thử nghiệm `-XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap`. Từ 8u191 có `UseContainerSupport` và `MaxRAMPercentage`; hỗ trợ **cgroup v2** chỉ từ 8u372 (JDK 11.0.16, JDK 15+) — image JDK cũ trên node cgroup v2 lại "mù" giới hạn. GC log Java 8 dùng `-XX:+PrintGCDetails -Xloggc:` thay vì `-Xlog`.
</details>

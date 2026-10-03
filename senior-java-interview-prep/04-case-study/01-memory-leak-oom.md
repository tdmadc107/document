# Case 01 — Heap tăng dần qua nhiều ngày rồi `OutOfMemoryError`: cache không giới hạn và `ThreadLocal` không được dọn

> **Chủ đề:** Memory leak, heap dump, Eclipse MAT, cache, `ThreadLocal` trong thread pool
> **Module liên quan:** [M05 §10 — Memory leak & các loại OOM](../01-giao-trinh/05-jvm-memory-gc-performance.md#p10) · [M05 §12 — jcmd, jstat, MAT, JFR](../01-giao-trinh/05-jvm-memory-gc-performance.md#p12) · [M05 §14 — Runbook OOM](../01-giao-trinh/05-jvm-memory-gc-performance.md#p14) · [M04 §13 — ThreadLocal & memory leak](../01-giao-trinh/04-concurrency.md#p13) · [M12 §2 — Caffeine vs distributed cache](../01-giao-trinh/12-redis-caching.md#phan-2) · [M05 §9 — Reference types](../01-giao-trinh/05-jvm-memory-gc-performance.md#p9)
> **Độ khó:** ⭐⭐⭐ (Senior)
> **Thời gian tự giải gợi ý:** 40 phút

---

## 1. Bối cảnh hệ thống

`pricing-service` tính giá bán cho một sàn thương mại điện tử (giá niêm yết, khuyến mãi, giá riêng cho từng nhóm khách hàng).

| Thành phần | Chi tiết |
|---|---|
| Stack | Java 21, Spring Boot 3.2, Spring MVC (Tomcat, 200 worker thread), PostgreSQL 15, Redis 7 |
| Triển khai | Kubernetes, 8 pod, mỗi pod `requests = limits = 4Gi` RAM, 2 CPU |
| JVM | `-XX:+UseG1GC -XX:MaxRAMPercentage=70` → heap tối đa ≈ **2.8 GB**; có `-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/dumps -XX:+ExitOnOutOfMemoryError`, `/dumps` mount PVC |
| Tải | ~1.500 RPS toàn cụm giờ cao điểm (~190 RPS/pod), p99 bình thường ≈ 120 ms |
| Quan sát | Micrometer → Prometheus → Grafana; log JSON → Loki; GC log bật sẵn (`-Xlog:gc*`) |

Ba tuần trước, bản **v3.14** ra mắt tính năng "giá cá nhân hóa": với khách hàng thuộc nhóm VIP/B2B (≈ 2,5% request), giá được tính bằng một rule engine tốn ~40 ms, nên đội đã thêm một **cache trong bộ nhớ**. Cùng bản đó thêm **audit trail**: mỗi bước tính giá ghi một `PriceAuditEntry` vào một context theo request, cuối request đẩy sang Kafka.

## 2. Triệu chứng

**Alert lúc 03:12 thứ Bảy:**
```
[FIRING] KubePodRestartHigh  pricing-service-7c9f6d-x2k8p restarts=1 reason=Error exitCode=3
[FIRING] HighLatencyP99      pricing-service p99=2.48s (threshold 500ms) for 10m
```

**Log cuối cùng của pod trước khi chết:**
```
2026-09-19T03:11:47.902Z  WARN  [http-nio-8080-exec-131] o.a.c.c.C.[.[.[/].[dispatcherServlet] Servlet.service() ... threw exception
java.lang.OutOfMemoryError: Java heap space
java.lang.OutOfMemoryError: Java heap space
Dumping heap to /dumps/java_pid1.hprof ...
Heap dump file created [2683417728 bytes in 21.384 secs]
Terminating due to java.lang.OutOfMemoryError: Java heap space
```

**GC log ~15 phút trước OOM** — Full GC liên tục mà thu hồi rất ít:
```
[2026-09-19T02:58:03.114+0000][452311.220s][info][gc] GC(18822) Pause Young (Normal) (G1 Evacuation Pause) 2741M->2698M(2868M) 87.412ms
[2026-09-19T02:58:04.902+0000][452313.008s][info][gc] GC(18823) To-space exhausted
[2026-09-19T02:58:08.337+0000][452316.443s][info][gc] GC(18824) Pause Full (G1 Compaction Pause) 2790M->2741M(2868M) 3214.512ms
[2026-09-19T02:58:15.870+0000][452323.976s][info][gc] GC(18826) Pause Full (G1 Compaction Pause) 2802M->2744M(2868M) 3307.091ms
```

**Grafana (panel "Live data size after GC" — metric `jvm_gc_live_data_size_bytes`)**, 7 ngày gần nhất của cùng một pod:

```
Ngày     | D0 (restart) | D1    | D2    | D3    | D4    | D5    | D5+15h
live set | 0.48 GB      | 0.83  | 1.19  | 1.52  | 1.88  | 2.23  | 2.74 → OOM
```
- Đường "răng cưa" `heap used` có **đáy tăng đều ~350 MB/ngày** ở cả 8 pod.
- Tỷ lệ thời gian GC tăng từ 0,4% (D1) lên 31% (D5+15h); p99 tăng theo.
- Trước v3.14, pod chạy hàng tuần với live set phẳng ~0,45 GB.

**Phàn nàn từ người dùng:** CSKH nhận phản ánh "giá hiển thị chậm, đôi lúc lỗi 503 lúc sáng sớm"; team B2B báo "audit log của khách A lại chứa bước tính giá của khách B".

## 3. Câu hỏi đặt ra

Người phỏng vấn thường hỏi:
1. Bằng chứng nào cho thấy đây là **leak** chứ không phải heap quá nhỏ?
2. Bạn thu thập gì, bằng công cụ nào, và làm sao để việc thu thập **không làm sự cố tệ hơn**?
3. Đọc heap dump như thế nào để ra được nguyên nhân gốc?
4. Biện pháp ngắn hạn và dài hạn? Làm sao chắc chắn không tái diễn?
5. Chi tiết "audit log của khách A chứa dữ liệu khách B" gợi ý điều gì?

> ✋ **Dừng lại và tự giải trước.** Viết ra giấy: 3 giả thuyết, công cụ cho từng giả thuyết, và kết quả nào sẽ xác nhận/bác bỏ nó. Sau đó mới đọc tiếp.

## 4. Điều tra từng bước

### Bước 0 — Ổn định hệ thống, giữ bằng chứng
- Pod đã tự restart nhờ `ExitOnOutOfMemoryError`; heap dump nằm trên PVC → **không mất bằng chứng**.
- Biện pháp tạm: đặt CronJob rolling restart mỗi 48 giờ (`kubectl rollout restart deploy/pricing-service`) — xấu nhưng mua được thời gian trong cuối tuần.
- Không tăng `-Xmx`: nếu là leak, tăng heap chỉ dời thời điểm chết và làm Full GC dài hơn.

### Bước 1 — Leak hay heap thiếu?
| Giả thuyết | Dấu hiệu nếu đúng | Thực tế |
|---|---|---|
| H1. Heap quá nhỏ cho workload | Live set **ổn định** ở mức cao, không phụ thuộc uptime | ❌ Live set tăng tuyến tính theo uptime |
| H2. Spike tạm thời (một request tải dữ liệu lớn) | Tăng đột ngột rồi giảm; OOM ngẫu nhiên | ❌ Tăng đều, có thể dự báo |
| H3. Leak (object không cần nhưng còn reachable) | Đáy răng cưa tăng đều; bắt đầu từ một bản deploy | ✅ Bắt đầu ngay sau v3.14 |

Xác nhận nhanh trên một pod đang sống (uptime 3 ngày) bằng `jstat` (rất nhẹ, không gây pause):
```bash
$ kubectl exec -it pricing-service-7c9f6d-q8w2n -- jstat -gcutil 1 5000 4
  S0     S1     E      O      M     CCS    YGC     YGCT     FGC    FGCT     CGC    CGCT       GCT
  0.00 100.00  41.18  61.27  96.81  93.02  11203  402.118     0    0.000   412   18.211   420.329
  0.00 100.00  83.53  61.30  96.81  93.02  11203  402.118     0    0.000   412   18.211   420.329
  0.00 100.00  12.94  61.88  96.81  93.02  11204  402.159     0    0.000   412   18.211   420.370
  0.00 100.00  55.29  61.88  96.81  93.02  11204  402.159     0    0.000   412   18.211   420.370
```
Old gen (`O`) ~62% và chỉ tăng; sau các concurrent cycle (`CGC`) không giảm về mức cũ → phù hợp H3.

### Bước 2 — Class histogram: *class nào* đang tăng?
Rẻ hơn heap dump nhiều (vẫn kích hoạt một Full GC ~1–2 s, nên chạy ngoài giờ cao điểm). Lấy hai lần cách nhau 6 giờ:
```bash
$ kubectl exec pricing-service-7c9f6d-q8w2n -- jcmd 1 GC.class_histogram | head -12 > histo-0900.txt
# ... 6 giờ sau ...
$ kubectl exec pricing-service-7c9f6d-q8w2n -- jcmd 1 GC.class_histogram | head -12 > histo-1500.txt
```
```
 num   #instances(09:00→15:00)      #bytes(09:00→15:00)   class name
   1:  3,912,004 → 4,560,118     312,960,320 → 364,809,440  [B (java.base)
   2:  3,880,551 → 4,521,903      93,133,224 → 108,525,672  java.lang.String
   3:  1,401,227 → 1,503,880      67,258,896 →  72,186,240  com.acme.pricing.PriceQuote
   4:  1,401,227 → 1,503,880      56,049,080 →  60,155,200  com.acme.pricing.PriceQuery
   5:  1,402,016 → 1,504,712      44,864,512 →  48,150,784  java.util.concurrent.ConcurrentHashMap$Node
   6:    812,433 → 1,121,590      38,996,784 →  53,836,320  com.acme.pricing.audit.PriceAuditEntry
   7:      9,846 →     10,104      61,511,720 →  77,921,560  [Ljava.lang.Object;
```
Hai "nghi phạm" có số instance **chỉ tăng**: cặp `PriceQuery/PriceQuote/ConcurrentHashMap$Node` (cùng số lượng → một map) và `PriceAuditEntry` (lẽ ra chỉ sống trong một request, vài trăm instance là cùng).

### Bước 3 — Heap dump và MAT
Đã có dump 2,7 GB từ lúc OOM; lấy thêm một dump trên pod đang sống để so sánh — **rút pod khỏi Service trước** vì dump là STW ~20 s:
```bash
kubectl label pod pricing-service-7c9f6d-q8w2n app.kubernetes.io/serving=false --overwrite  # selector của Service yêu cầu serving=true
kubectl exec pricing-service-7c9f6d-q8w2n -- jcmd 1 GC.heap_dump /dumps/live-d3.hprof
kubectl exec pricing-service-7c9f6d-q8w2n -- gzip /dumps/live-d3.hprof
kubectl cp pricing-service-7c9f6d-q8w2n:/dumps/live-d3.hprof.gz ./live-d3.hprof.gz
```
> Heap dump chứa PII (tên, mã khách hàng, token trong String) → phân tích trên máy được cấp quyền, xóa sau khi xong. Mở bằng MAT cần RAM ≈ 1,5× kích thước dump (`-Xmx6g` trong `MemoryAnalyzer.ini`).

**Leak Suspects** báo hai suspect. **Dominator Tree** (Group by class):
```
Class Name                                                    | Objects | Shallow Heap | Retained Heap | %
-----------------------------------------------------------------------------------------------------------
com.acme.pricing.cache.PersonalizedPriceCache                 |       1 |           16 | 1,002,418,904 | 37.4%
java.lang.Thread                                              |     231 |       25,872 |   944,105,272 | 35.2%
  (200 × "http-nio-8080-exec-*", mỗi thread retained ~4,6 MB)
org.springframework.boot.loader.launch.LaunchedClassLoader    |       1 |           96 |    88,311,520 |  3.3%
```

**Path to GC Roots** (exclude weak/soft references) cho một `PriceQuery`:
```
com.acme.pricing.PriceQuery @ 0x7a1c3e2f8
'- key java.util.concurrent.ConcurrentHashMap$Node @ 0x7a1c3e2d0
   '- [883211] java.util.concurrent.ConcurrentHashMap$Node[4194304] @ 0x6f0000000
      '- table java.util.concurrent.ConcurrentHashMap @ 0x6c2a11b80
         '- cache com.acme.pricing.cache.PersonalizedPriceCache @ 0x6c2a11b70
            '- singletonObjects java.util.concurrent.ConcurrentHashMap (DefaultListableBeanFactory) ...
```
Một bean singleton giữ map 4 triệu bucket. **OQL** để xem key trông thế nào:
```sql
SELECT q.productId, q.customerId, toString(q.requestedAt) FROM com.acme.pricing.PriceQuery q
```
```
productId  customerId  requestedAt
88123      551920      2026-09-17T08:14:02.118331Z
88123      551920      2026-09-17T08:14:02.904127Z   ← cùng khách, cùng sản phẩm, khác thời điểm
88123      551920      2026-09-17T08:14:05.330019Z
```
→ Key có **timestamp tới micro-giây** → không bao giờ trùng → cache không bao giờ hit, và không bao giờ bị xóa.

**Path to GC Roots** cho một `PriceAuditEntry`:
```
com.acme.pricing.audit.PriceAuditEntry @ 0x75e0a1c40
'- [6411] java.lang.Object[9785] @ 0x75d900000
   '- elementData java.util.ArrayList @ 0x75d8ff2a8           size = 6,523
      '- auditTrail com.acme.pricing.context.PricingContext @ 0x75d8ff290
         '- value java.lang.ThreadLocal$ThreadLocalMap$Entry @ 0x75d8ff270
            '- [9] java.lang.ThreadLocal$ThreadLocalMap$Entry[16]
               '- table java.lang.ThreadLocal$ThreadLocalMap
                  '- threadLocals java.lang.Thread @ 0x6c3f0a1e8  http-nio-8080-exec-47
                     '- <Thread, GC root>
```
Worker thread của Tomcat sống mãi → `ThreadLocalMap` của nó sống mãi → `PricingContext` tích lũy 6.523 audit entry từ **nhiều request khác nhau**. Đây cũng là lý do audit của khách A chứa dữ liệu khách B.

### Bước 4 — Xác nhận bằng số liệu nghiệp vụ
- Metric tự chế `pricing.personalized.cache.size` (đội đã gắn gauge) = 2,2 triệu entry ở D5; hit rate tính từ log: **0,3%**.
- Đếm request kết thúc bằng exception trong `PriceService` (Loki): ~1,5% (chủ yếu `PromotionExpiredException` trả 409) — khớp với tốc độ tích lũy audit entry.

## 5. Nguyên nhân gốc

**Lỗi 1 — cache không giới hạn với key không bao giờ lặp lại:**
```java
public record PriceQuery(long productId, long customerId, String segment, Instant requestedAt) {}

@Component
public class PersonalizedPriceCache {
    private final Map<PriceQuery, PriceQuote> cache = new ConcurrentHashMap<>();   // ❌ không bound, không TTL

    public PriceQuote get(PriceQuery q, Function<PriceQuery, PriceQuote> loader) {
        return cache.computeIfAbsent(q, loader);                                   // ❌ requestedAt làm key luôn mới
    }
}
```
`requestedAt` được thêm vào record để rule engine biết khung giờ khuyến mãi — một thay đổi "vô hại" ở model đã biến cache thành **danh sách ghi mãi không xóa**.

**Lỗi 2 — `ThreadLocal` chỉ được dọn ở happy path:**
```java
public final class PricingContext {
    private static final ThreadLocal<PricingContext> CURRENT = ThreadLocal.withInitial(PricingContext::new);
    private final List<PriceAuditEntry> auditTrail = new ArrayList<>();

    public static PricingContext current() { return CURRENT.get(); }
    public void record(PriceAuditEntry e) { auditTrail.add(e); }
    public List<PriceAuditEntry> drain() { var copy = List.copyOf(auditTrail); auditTrail.clear(); return copy; }
}

@Service
public class PriceService {
    public PriceQuote quote(PriceRequest req) {
        PriceQuote q = engine.calculate(req);                         // có thể ném PromotionExpiredException
        auditPublisher.publish(PricingContext.current().drain());     // ❌ không chạy khi có exception
        return q;
    }
}
```
Khi exception xảy ra, `drain()` không được gọi; các entry ở lại trong `PricingContext` của **thread**, request sau trên cùng thread nối thêm vào và publish nhầm cả entry cũ (lỗi dữ liệu + rò rỉ thông tin giữa khách hàng).

Vì sao GC không thu được: `ThreadLocalMap.Entry` giữ **key** bằng `WeakReference<ThreadLocal>` nhưng **value** bằng strong reference; ở đây `ThreadLocal` là `static final` nên key không bao giờ bị thu, và thread của pool không bao giờ chết.

## 6. Giải pháp

### Ngắn hạn (trong ngày)
1. Tắt cache cá nhân hóa bằng feature flag `pricing.personalized-cache.enabled=false` (cache đang có hit rate 0,3%, tắt gần như không ảnh hưởng latency).
2. Hotfix `try/finally` cho `PricingContext` (5 dòng code, rủi ro thấp) → deploy.
3. Giữ CronJob restart 48h cho đến khi live set phẳng 3 ngày liên tiếp, rồi gỡ.

### Dài hạn
**Cache có giới hạn, key đúng nghĩa:**
```java
public record PriceKey(long productId, String segment, long priceListVersion) {}   // không có timestamp, không có customerId thừa

@Configuration
class PricingCacheConfig {
    @Bean
    Cache<PriceKey, PriceQuote> personalizedPriceCache(MeterRegistry registry) {
        Cache<PriceKey, PriceQuote> cache = Caffeine.newBuilder()
                .maximumSize(200_000)                         // ~200k × ~450 B ≈ 90 MB — đã tính trước
                .expireAfterWrite(Duration.ofMinutes(5))      // giá đổi theo khung giờ khuyến mãi
                .recordStats()
                .build();
        CaffeineCacheMetrics.monitor(registry, cache, "personalizedPrice");   // cache_size, cache_gets{result=hit|miss}, cache_evictions
        return cache;
    }
}
```
Khung giờ khuyến mãi được xử lý bằng `priceListVersion` (tăng khi bảng giá/khuyến mãi đổi) thay vì nhét thời điểm request vào key.

**`ThreadLocal` luôn được dọn — đặt ở biên request, trong `finally`:**
```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
class PricingContextFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        try {
            chain.doFilter(req, res);
        } finally {
            PricingContext.remove();                 // CURRENT.remove() — không chỉ clear list
        }
    }
}
```
Và `PriceService` publish audit trong `finally` (audit cả request thất bại — vốn là yêu cầu nghiệp vụ).

Phương án sạch hơn nữa: bỏ `ThreadLocal`, truyền `PricingContext` tường minh như tham số, hoặc dùng bean `@RequestScope`. Trên Java 21 có `ScopedValue` (preview ở JDK 21, chính thức từ JDK 25) — giá trị chỉ tồn tại trong phạm vi `ScopedValue.where(...).run(...)`, tự hết hiệu lực khi ra khỏi scope, không thể "quên remove".

### So sánh các lựa chọn cho cache
| Lựa chọn | Ưu điểm | Nhược điểm | Khi nào chọn |
|---|---|---|---|
| `ConcurrentHashMap` + tự dọn định kỳ | Không thêm dependency | Tự viết eviction dễ sai, không có metrics, không LRU/LFU | Gần như không bao giờ cho cache thật |
| **Caffeine** (`maximumSize`/`maximumWeight` + TTL) | W-TinyLFU hit rate cao, lock-free, metrics sẵn, `refreshAfterWrite` | Mỗi pod một bản → không nhất quán giữa pod, tốn heap × số pod | Dữ liệu đọc nhiều, chấp nhận stale vài phút — **chọn ở đây** |
| Redis (Spring Cache) | Dùng chung giữa pod, không tốn heap | +1 network hop (~0,5–1 ms), serialize, thêm điểm lỗi | Dữ liệu lớn, cần nhất quán giữa pod, hoặc cache đắt để dựng lại |
| `SoftReference` / `WeakHashMap` | "Tự dọn" khi thiếu bộ nhớ | Soft ref chỉ bị thu khi heap gần đầy → GC áp lực cao, latency khó đoán; `WeakHashMap` dọn theo key chứ không theo tuổi | Hầu như không dùng cho cache nghiệp vụ |
| Cache 2 tầng (Caffeine L1 + Redis L2) | Latency thấp + chia sẻ | Phức tạp: invalidation qua pub/sub | Hot key cực nóng ở quy mô lớn |

### Kết quả
Sau 7 ngày: live set phẳng 0,47–0,52 GB, GC time < 0,5%, p99 quay về 115 ms, hit rate cache mới 78% (rule engine được gọi ít hơn 4 lần → CPU giảm 18%).

## 7. Phòng ngừa

**Monitoring & alert (PromQL):**
```promql
# Live set sau GC vượt 70% heap tối đa trong 30 phút
max_over_time(jvm_gc_live_data_size_bytes[30m]) / jvm_gc_max_data_size_bytes > 0.7

# Live set tăng đều: dự báo chạm max trong 24h tới
predict_linear(jvm_gc_live_data_size_bytes[12h], 24*3600) > jvm_gc_max_data_size_bytes

# Cache mà hit rate thấp là cache vô dụng (hoặc key sai)
sum(rate(cache_gets_total{result="hit"}[1h])) by (cache)
  / sum(rate(cache_gets_total[1h])) by (cache) < 0.2
```

**Test:**
- **Soak test** 12–24 giờ trên staging với tải thật (k6/Gatling replay), kiểm tra `jvm_gc_live_data_size_bytes` phẳng — gắn vào pipeline release cho service có thay đổi về cache/context.
- Unit test cho filter: gọi chain ném exception → assert `PricingContext` đã bị `remove()`.
- Bật JFR liên tục trên production (`-XX:StartFlightRecording=...,settings=default`); sự kiện `jdk.OldObjectSample` (bật `old-objects`/path-to-gc-roots khi cần) giúp thấy "object sống lâu được cấp phát ở đâu" mà không cần heap dump.

**Code review checklist:**
- [ ] Mọi `Map`/`List` là field của bean singleton hoặc `static`: **có giới hạn kích thước không? ai xóa phần tử?**
- [ ] Key của cache: có thành phần biến thiên liên tục (timestamp, requestId, UUID) không? `equals/hashCode` có ổn định không?
- [ ] Mọi `ThreadLocal.set()` có `remove()` tương ứng trong `finally` ở **cùng tầng** không? Có chạy trên thread pool không?
- [ ] Cache có metrics (size, hit rate, eviction) chưa?

**Quy trình:** heap dump path luôn mount volume; postmortem không đổ lỗi; thêm mục "bộ nhớ" vào checklist thiết kế tính năng mới.

## 8. Cách kể lại trong phỏng vấn (STAR, ~1,5 phút)

- **S — Situation:** "Service tính giá của tôi chạy trên K8s, 8 pod, heap 2,8 GB, ~1.500 RPS. Sau một bản release thêm cache và audit trail, các pod cứ 5–6 ngày lại OOM, p99 tăng từ 120 ms lên 2,5 s trước khi chết."
- **T — Task:** "Tôi được giao tìm nguyên nhân gốc và sửa dứt điểm, trong lúc vẫn giữ hệ thống chạy được qua cuối tuần."
- **A — Action:** "Đầu tiên tôi đặt restart định kỳ để cầm cự và giữ heap dump trên PVC. Tôi xác nhận là leak chứ không phải thiếu heap bằng metric live set sau GC tăng tuyến tính ~350 MB/ngày, bắt đầu đúng từ bản release. So sánh hai class histogram cách nhau 6 giờ cho thấy `PriceQuery` và `PriceAuditEntry` chỉ tăng. Trong MAT, dominator tree chỉ ra hai thủ phạm: một `ConcurrentHashMap` trong bean cache giữ 1 GB — key chứa timestamp nên không bao giờ hit — và 200 worker thread của Tomcat mỗi thread giữ ~4,6 MB qua `ThreadLocal` không được dọn khi có exception. Tôi hotfix `try/finally`, tắt cache bằng feature flag, sau đó thay bằng Caffeine có `maximumSize` và TTL, sửa key."
- **R — Result:** "Live set phẳng ở 0,5 GB, p99 về 115 ms, hit rate 78% nên CPU còn giảm 18%. Chúng tôi phát hiện thêm và sửa một lỗi rò rỉ dữ liệu audit giữa khách hàng. Sau đó tôi thêm alert `predict_linear` trên live set, soak test 24 giờ trong pipeline và mục checklist review cho cache/ThreadLocal."

## 9. Câu hỏi mở rộng

<details>
<summary>1. Nếu không có metric <code>jvm_gc_live_data_size_bytes</code>, làm sao phân biệt leak với heap thiếu?</summary>

Dùng GC log: lấy giá trị heap **sau** mỗi Full GC/mixed GC (hoặc sau concurrent cycle của G1) và vẽ theo thời gian. Leak: đường này tăng theo uptime. Heap thiếu: đường này phẳng nhưng sát `Xmx`, GC dày đặc ngay từ đầu. Cũng có thể dùng `jstat -gcutil` nhiều lần (cột `O` ngay sau GC) hoặc `jcmd GC.heap_info`. Đừng nhìn "heap used" tức thời — nó luôn răng cưa.
</details>

<details>
<summary>2. Heap 16 GB thì lấy heap dump thế nào cho an toàn? Có cách nào tránh heap dump không?</summary>

Heap dump là STW, thời gian tỷ lệ với heap (16 GB có thể mất 1–2 phút) và cần đĩa ≥ heap. Rút instance khỏi load balancer trước, đảm bảo đĩa, nén trước khi chuyển, xử lý như dữ liệu nhạy cảm. Thay thế nhẹ hơn: so sánh `GC.class_histogram` nhiều lần; JFR với sự kiện `jdk.OldObjectSample` (lấy mẫu object sống lâu kèm stack cấp phát và có thể kèm path-to-GC-roots khi dump); async-profiler chế độ `alloc` hoặc `--live` (chỉ giữ mẫu của object còn sống). Với heap rất lớn, có thể dump trên một instance canary chạy cùng bản lỗi nhưng heap nhỏ hơn để leak lộ nhanh.
</details>

<details>
<summary>3. Vì sao không dùng <code>WeakHashMap</code> hoặc <code>SoftReference</code> làm cache cho xong?</summary>

`WeakHashMap` xóa entry khi **key** không còn strong reference ở nơi khác — với key là record tạo mới mỗi request, entry bị xóa ngay ở GC kế tiếp (cache vô dụng); với key được giữ ở nơi khác thì không bao giờ xóa. `SoftReference` chỉ bị thu khi JVM sắp hết heap: heap luôn đầy, GC làm việc nhiều, pause khó đoán, và chính sách thu (`SoftRefLRUPolicyMSPerMB`) phụ thuộc heap trống chứ không phụ thuộc giá trị nghiệp vụ. Cache nghiệp vụ cần chính sách tường minh: kích thước tối đa + TTL + metrics.
</details>

<details>
<summary>4. Với virtual threads (Java 21), vấn đề <code>ThreadLocal</code> còn không?</summary>

Virtual thread thường được tạo mới cho mỗi task và không pool, nên value của `ThreadLocal` chết cùng thread — kiểu leak "tích lũy trên worker thread" ít xảy ra hơn. Nhưng xuất hiện vấn đề khác: hàng triệu virtual thread mỗi cái một bản `ThreadLocal` nặng (ví dụ cache `SimpleDateFormat`, buffer 64 KB) → tốn bộ nhớ khủng khiếp; và nếu ai đó pool virtual thread (anti-pattern) thì lại leak. Hướng đi của JDK là `ScopedValue` (immutable, có phạm vi rõ, rẻ khi kế thừa sang structured concurrency). Thread pool truyền thống (`@Async`, Kafka listener, `ForkJoinPool`) vẫn cần `remove()` trong `finally`.
</details>

<details>
<summary>5. RSS của pod tăng dần nhưng heap và live set phẳng — bạn điều tra gì?</summary>

Đó là leak **ngoài heap**. Bật `-XX:NativeMemoryTracking=summary`, chụp `jcmd <pid> VM.native_memory baseline` rồi `summary.diff` sau vài giờ để xem vùng nào tăng (Thread, Class/Metaspace, Internal, Other = direct buffer). Kiểm tra: số thread (`jvm_threads_live_threads`), Metaspace (`VM.classloader_stats` — leak classloader khi dùng Groovy/scripting/proxy động), direct buffer (`jvm_buffer_memory_used_bytes{id="direct"}` — Netty `ByteBuf` không `release()`), `GZIPInputStream`/`Inflater` không `close()`. Nếu NMT phẳng mà RSS vẫn tăng: phân mảnh glibc malloc arena — thử `MALLOC_ARENA_MAX=2` hoặc jemalloc. Xem thêm case 09.
</details>

<details>
<summary>6. Bạn giải thích thế nào với PM vì sao bug "audit log lẫn dữ liệu khách hàng" nghiêm trọng hơn OOM?</summary>

OOM là sự cố **khả dụng** — hệ thống tự hồi phục sau restart. Lẫn dữ liệu giữa khách hàng là sự cố **toàn vẹn và bảo mật** — có thể vi phạm hợp đồng B2B/quy định bảo vệ dữ liệu cá nhân, phải đánh giá phạm vi ảnh hưởng (truy vết audit event nào bị nhiễm trong 3 tuần), thông báo cho bên liên quan, và sửa dữ liệu. Vì vậy hotfix `try/finally` được ưu tiên deploy trước cả fix cache, kèm job quét audit để đánh dấu các bản ghi bị nhiễm.
</details>

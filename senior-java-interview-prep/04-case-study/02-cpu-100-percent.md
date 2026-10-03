# Case 02 — CPU 100% trên một số pod: regex "thảm họa" (catastrophic backtracking)

> **Chủ đề:** CPU cao, `top -H`, thread dump, async-profiler, ReDoS, CPU throttling trong container
> **Module liên quan:** [M05 §14 — Runbook CPU 100%](../01-giao-trinh/05-jvm-memory-gc-performance.md#p14) · [M05 §12 — jcmd, jstack, async-profiler](../01-giao-trinh/05-jvm-memory-gc-performance.md#p12) · [M16 §6 — JVM trong container, CPU throttling](../01-giao-trinh/16-devops-build-cloud-security.md#p6) · [M08 §3 — Data binding & validation](../01-giao-trinh/08-spring-web-rest-security.md#p3) · [M02 §3 — HashMap internals](../01-giao-trinh/02-collections-generics.md#phan-3) · [M14 §4 — Bulkhead](../01-giao-trinh/14-microservices-system-design.md#p4)
> **Độ khó:** ⭐⭐⭐ (Senior)
> **Thời gian tự giải gợi ý:** 35 phút

---

## 1. Bối cảnh hệ thống

`onboarding-service` phục vụ đăng ký doanh nghiệp và **import danh sách nhân viên** (CSV, tối đa 5.000 dòng) cho một nền tảng HR SaaS.

| Thành phần | Chi tiết |
|---|---|
| Stack | Java 21, Spring Boot 3.3, Spring MVC (Tomcat 200 thread), Hibernate Validator 8, PostgreSQL |
| Triển khai | Kubernetes, 12 pod; `requests.cpu=1`, `limits.cpu=2`, RAM 2Gi; G1GC |
| Gateway | Kong, timeout upstream 30 s, **retry 2 lần** cho lỗi 502/503/504 |
| Tải | ~600 RPS bình thường; endpoint import ~20 lần/giờ, mỗi lần 1–3 s |

Mỗi dòng CSV được map thành record `EmployeeRow` và validate bằng Bean Validation trước khi lưu.

## 2. Triệu chứng

**10:05 thứ Hai — alert:**
```
[FIRING] PodCPUThrottlingHigh   onboarding-service-6d8b-7tq4m throttled=68% for 5m
[FIRING] PodCPUThrottlingHigh   onboarding-service-6d8b-k2l9x throttled=71% for 5m
[FIRING] ReadinessProbeFailing  onboarding-service-6d8b-7tq4m /actuator/health/readiness timeout
```

**`kubectl top pods` (10:09):**
```
NAME                               CPU(cores)   MEMORY(bytes)
onboarding-service-6d8b-7tq4m      1998m        1210Mi
onboarding-service-6d8b-k2l9x      2000m        1187Mi
onboarding-service-6d8b-p0zz1      1996m        1203Mi
onboarding-service-6d8b-bb31c      212m         1150Mi
... (8 pod còn lại ~150–250m)
```
- 3 pod chạm đúng CPU limit; 10 phút sau thêm 2 pod nữa "nóng" → sự cố **lan dần**.
- Memory bình thường, GC pause bình thường (`jvm_gc_pause_seconds_max` < 30 ms).
- `tomcat_threads_busy_threads` của pod nóng: 4–6 (không phải cạn thread).
- Gateway log: `POST /v1/companies/8812/employees:import` → **504** sau 30 s, rồi retry 2 lần sang pod khác.
- Người dùng: "Trang đăng ký treo, lúc được lúc không" — vì 3/12 pod không trả lời health check, request bị dồn sang pod còn lại, p99 toàn service từ 180 ms lên 4,2 s.

## 3. Câu hỏi đặt ra

1. CPU 100% có thể do những nhóm nguyên nhân nào? Loại trừ chúng theo thứ tự nào?
2. Từ "pod ăn CPU" đến "dòng code ăn CPU" bằng những lệnh gì?
3. Vì sao sự cố **lan** sang các pod khác dù không có deploy mới?
4. Sửa thế nào để không chỉ hết lỗi lần này mà cả lớp lỗi tương tự?

> ✋ **Dừng lại và tự giải trước.** Liệt kê 4 nhóm nguyên nhân CPU cao trong JVM và dấu hiệu nhận biết từng nhóm, rồi mới đọc tiếp.

## 4. Điều tra từng bước

### Bước 0 — Ổn định
- Restart 3 pod nóng (`kubectl delete pod ...`) **sau khi** lấy thread dump của một pod (bước 2) — mất 30 giây, nhưng giữ được bằng chứng.
- Tạm **tắt retry ở gateway** cho route import (retry đang nhân bản request độc hại sang pod khỏe).

### Bước 1 — Phân loại: GC, JIT, hay code ứng dụng?
| Nhóm nguyên nhân | Dấu hiệu | Kết quả |
|---|---|---|
| GC liên tục (heap đầy/leak) | Thread `GC Thread#`, `G1 Conc#` ăn CPU; `jstat` thấy `GCT` tăng nhanh | ❌ GC bình thường |
| JIT (deopt storm, code cache đầy) | `C2 CompilerThread` ăn CPU; log "CodeCache is full" | ❌ |
| Contention/spin | Nhiều thread RUNNABLE trên cùng CAS loop/`synchronized` | ❌ chỉ 1–2 thread nóng |
| Code ứng dụng nóng (vòng lặp, regex, serialize khổng lồ) | Thread `http-nio-*` ăn ~100% một core | ✅ (xem bước 2) |

### Bước 2 — Thread nào trong JVM?
Image có `procps`; nếu không, dùng ephemeral debug container (`kubectl debug -it <pod> --image=eclipse-temurin:21-jdk --target=app`).
```bash
$ kubectl exec -it onboarding-service-6d8b-7tq4m -- top -H -b -n 1 -p 1 | head -12
    PID USER      PR  NI    VIRT    RES    SHR S  %CPU  %MEM     TIME+ COMMAND
     74 app       20   0 4318472   1.2g  28412 R  99.7  58.9   3:08.41 http-nio-8080-e
     91 app       20   0 4318472   1.2g  28412 R  98.1  58.9   2:41.77 http-nio-8080-e
     23 app       20   0 4318472   1.2g  28412 S   0.7  58.9   0:12.03 GC Thread#0
     31 app       20   0 4318472   1.2g  28412 S   0.3  58.9   0:31.55 C2 CompilerThre
```
Hai worker thread của Tomcat, mỗi cái ~100% một core → đúng bằng CPU limit 2 → pod bị throttle, health check không có CPU để chạy.

Lấy **3 thread dump cách nhau 10 s**:
```bash
for i in 1 2 3; do kubectl exec onboarding-service-6d8b-7tq4m -- jcmd 1 Thread.print -l > td$i.txt; sleep 10; done
grep -A 22 'nid=74 ' td1.txt
```
> JDK 21 in `nid` ở dạng **thập phân** (`#19 [74] ... nid=74`), nên grep thẳng TID từ `top -H`. JDK 8/11/17 in dạng hex (`nid=0x4a`) → cần `printf '%x\n' 74` trước khi grep.

```
"http-nio-8080-exec-17" #63 [74] daemon prio=5 os_prio=0 cpu=187331.22ms elapsed=190.07s tid=0x00007f2c3c0218a0 nid=74 runnable  [0x00007f2bf7cfd000]
   java.lang.Thread.State: RUNNABLE
	at java.util.regex.Pattern$BmpCharPropertyGreedy.match(java.base@21.0.4/Pattern.java:4502)
	at java.util.regex.Pattern$GroupHead.match(java.base@21.0.4/Pattern.java:4969)
	at java.util.regex.Pattern$Loop.matchInit(java.base@21.0.4/Pattern.java:5100)
	at java.util.regex.Pattern$Prolog.match(java.base@21.0.4/Pattern.java:5024)
	at java.util.regex.Pattern$BmpCharProperty.match(java.base@21.0.4/Pattern.java:4134)
	at java.util.regex.Pattern$Loop.match(java.base@21.0.4/Pattern.java:5090)
	at java.util.regex.Pattern$GroupTail.match(java.base@21.0.4/Pattern.java:5000)
	at java.util.regex.Pattern$Ques.match(java.base@21.0.4/Pattern.java:4410)
	at java.util.regex.Pattern$BmpCharPropertyGreedy.match(java.base@21.0.4/Pattern.java:4509)
	at java.util.regex.Pattern$GroupHead.match(java.base@21.0.4/Pattern.java:4969)
	at java.util.regex.Pattern$Loop.match(java.base@21.0.4/Pattern.java:5078)
	... (lặp lại hàng trăm frame Loop/GroupTail/Ques/GroupHead)
	at java.util.regex.Matcher.match(java.base@21.0.4/Matcher.java:1767)
	at java.util.regex.Matcher.matches(java.base@21.0.4/Matcher.java:728)
	at org.hibernate.validator.internal.constraintvalidators.bv.PatternValidator.isValid(PatternValidator.java:66)
	...
	at com.acme.onboarding.importer.EmployeeImportService.validateRows(EmployeeImportService.java:88)
	at com.acme.onboarding.importer.EmployeeImportController.importEmployees(EmployeeImportController.java:41)
```
Hai chi tiết quyết định:
- `cpu=187331ms elapsed=190s` → thread đốt CPU **liên tục** suốt 190 giây — trong khi gateway đã timeout ở giây thứ 30. Tomcat không giết thread khi client bỏ đi.
- Cả 3 dump, cùng thread vẫn ở trong `java.util.regex.Pattern$Loop` → không phải "tình cờ đang ở đó".

### Bước 3 — Xác nhận bằng profiler (không bị safepoint bias)
```bash
kubectl exec onboarding-service-6d8b-k2l9x -- /opt/async-profiler/bin/asprof -d 30 -e cpu -f /tmp/cpu.html 1
```
Flame graph: **97,8%** mẫu nằm dưới `EmployeeImportService.validateRows → PatternValidator.isValid → Matcher.matches → Pattern$Loop.match`. (Nếu container không cho dùng `perf_events`, dùng `-e itimer`.)

### Bước 4 — Tìm input gây lỗi
- MDC `requestId` trong log JSON → `companyId=8812`, file `nhanvien_T9.csv`, 4.870 dòng.
- Trích các giá trị email không hợp lệ: nhiều dòng thiếu đuôi tên miền, ví dụ `phongkinhdoanhchinhanhhcm2024@gmail` (35 ký tự).
- Tái hiện cục bộ trên JDK 21 (đo `matches()` với pattern hiện tại):

| Input | Độ dài | Kết quả | Thời gian |
|---|---|---|---|
| `nguyenvana@gmail.com` | 20 | true | < 1 ms |
| `nguyenvanthanhcong1990@gmail` | 28 | false | ~370 ms |
| `phongkinhdoanhchinhanhhcm2024@gmail` | 35 | false | ~47 s |
| `nguyenvanthanhcongkinhdoanh199@gmail` | 36 | false | ~94 s |

Mỗi ký tự thêm vào → thời gian **gấp đôi** → độ phức tạp hàm mũ. Một file có vài chục dòng như vậy = một thread bị chiếm hàng giờ.

### Bước 5 — Vì sao bây giờ mới xảy ra, và vì sao lan?
`git log -L` trên `EmployeeRow.java`: hai tuần trước có commit *"Giới hạn local-part tối đa 64 ký tự theo RFC 5321"* đổi `*` thành `{1,64}`. Pattern cũ chạy ổn vì từ **JDK 9**, engine regex của Java có **memoization** cho vòng lặp group `*`/`+` không giới hạn, nên nhiều ví dụ ReDoS kinh điển dạng `(x+)*` không còn bùng nổ. Nhưng **quantifier có giới hạn `{m,n}`** (và backreference) không được tối ưu này → quay lại backtracking hàm mũ. Đo lại: cùng input 35 ký tự, pattern cũ `( ... )*` chạy < 1 ms.

Lan sang pod khác: gateway timeout 30 s → retry 2 lần → mỗi lần retry rơi vào một pod mới và chiếm thêm 1 core ở đó; khách hàng thấy lỗi lại bấm "Import" → thêm 3 request nữa.

## 5. Nguyên nhân gốc

```java
public record EmployeeRow(
        @NotBlank String fullName,
        // ❌ group có quantifier lồng: ([..]+[._-]?){1,64} — với "aaaa...a@gmail" có 2^n cách chia local-part
        @Pattern(regexp = "^([a-zA-Z0-9]+[._-]?){1,64}@([a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}$")
        String email,
        String department) {}
```
Cơ chế: `[a-zA-Z0-9]+` bên trong và `{1,64}` bên ngoài cùng có thể "ăn" một chuỗi chữ cái; `[._-]?` là tùy chọn nên không phân tách được ranh giới. Khi phần sau `@` không khớp (thiếu `.com`), engine thử **mọi cách chia** chuỗi `phongkinhdoanh...2024` thành các nhóm trước khi kết luận `false` — ~2ⁿ cách.

Các yếu tố góp phần:
1. Không giới hạn độ dài **trước** khi chạy regex.
2. Validate 5.000 dòng **đồng bộ trên thread request** — không có bulkhead.
3. Gateway retry request không idempotent và tốn kém.
4. Công việc CPU-bound không thể hủy: `Matcher` không kiểm tra `Thread.interrupted()`.

## 6. Giải pháp

### Ngắn hạn (trong giờ)
1. Tắt retry ở gateway cho route import; rate limit route này 5 request/phút/tenant.
2. Hotfix: kiểm tra độ dài và cấu trúc thô trước regex (`@Size(max = 254)`, có đúng một `@`, phần domain có dấu chấm).
3. Restart các pod đang nóng (thread kẹt không thể dừng bằng cách nào khác).

### Dài hạn
**Viết lại regex không còn nhập nhằng (mỗi ký tự chỉ có một cách khớp):**
```java
public final class EmailRules {
    // local-part: các khối chữ-số, ngăn cách bởi đúng MỘT ký tự [._-]; không có quantifier lồng chồng lấn
    static final Pattern EMAIL = Pattern.compile(
            "^[A-Za-z0-9]+(?:[._-][A-Za-z0-9]+)*@(?:[A-Za-z0-9-]+\\.)+[A-Za-z]{2,}$");

    public static boolean isValid(String s) {
        if (s == null || s.length() > 254) return false;      // giới hạn tổng (RFC 5321)
        int at = s.indexOf('@');
        if (at <= 0 || at > 64 || at != s.lastIndexOf('@')) return false;  // giới hạn local-part bằng code, không bằng {1,64}
        return EMAIL.matcher(s).matches();
    }
}
```
Nếu buộc phải giữ cấu trúc cũ, dùng **atomic group** `(?>...)` hoặc **possessive quantifier** `++` để cấm backtrack vào bên trong: `^(?>[a-zA-Z0-9]+[._-]?){1,64}@...` chạy < 1 ms với cùng input.

**Tách import khỏi thread request (bulkhead + async):**
```java
@PostMapping("/v1/companies/{id}/employees:import")
ResponseEntity<ImportJob> importEmployees(@PathVariable long id, @RequestParam MultipartFile file) {
    ImportJob job = importJobs.create(id, file);            // lưu file, trả 202 + jobId ngay
    importExecutor.submit(() -> importService.run(job));    // ThreadPoolExecutor(4, 4, queue 50, AbortPolicy) riêng
    return ResponseEntity.accepted().body(job);
}
```
Pool riêng giới hạn 4 thread → tệ nhất import chỉ chiếm 4 core-slot trên cả cụm pod đó, API đăng ký vẫn sống. Kèm deadline cho mỗi job (kiểm tra thời gian giữa các dòng).

**Lưới an toàn: regex có deadline** — bọc input bằng `CharSequence` kiểm tra thời gian trong `charAt()`:
```java
record DeadlineCharSequence(CharSequence inner, long deadlineNanos) implements CharSequence {
    public char charAt(int i) {
        if (System.nanoTime() > deadlineNanos) throw new RegexTimeoutException();
        return inner.charAt(i);
    }
    public int length() { return inner.length(); }
    public CharSequence subSequence(int s, int e) { return new DeadlineCharSequence(inner.subSequence(s, e), deadlineNanos); }
    public String toString() { return inner.toString(); }
}
// EMAIL.matcher(new DeadlineCharSequence(input, System.nanoTime() + 50_000_000)).matches();
```

### So sánh lựa chọn
| Lựa chọn | Ưu | Nhược | Khi dùng |
|---|---|---|---|
| Viết lại regex không nhập nhằng | Gốc rễ, không thêm dependency | Cần người hiểu regex; dễ tái phạm ở regex khác | Luôn làm |
| Atomic group / possessive | Sửa nhanh, giữ nguyên logic | Có thể đổi ngữ nghĩa khớp nếu không cẩn thận | Hotfix regex sẵn có |
| Giới hạn độ dài input | Rẻ, chặn được phần lớn | Với n = 30 vẫn có thể mất hàng trăm ms | Luôn làm, ở mọi input từ bên ngoài |
| RE2/J (engine thời gian tuyến tính) | Loại bỏ cả lớp lỗi ReDoS | Không hỗ trợ backreference/lookaround; thêm dependency | Regex do **người dùng/cấu hình** cung cấp |
| `CharSequence` có deadline | Bảo vệ mọi regex, kể cả chưa biết | Overhead nhỏ ở `charAt`; chỉ là lưới an toàn | Nơi chạy regex trên input không tin cậy |
| Bulkhead + async job | Cô lập sự cố, không ảnh hưởng API khác | Thay đổi API (202 + polling/webhook) | Tác vụ nặng, input lớn |

### Kết quả
File 4.870 dòng import trong 1,4 s; 5 pod về CPU ~200m; thêm 2 regex khác trong codebase được phát hiện có cùng dạng nhờ rà soát.

## 7. Phòng ngừa

- **Alert**: `rate(container_cpu_cfs_throttled_periods_total[5m]) / rate(container_cpu_cfs_periods_total[5m]) > 0.25` theo pod; JFR liên tục với `jdk.ThreadCPULoad` để biết thread nào ăn CPU ngay cả khi sự cố đã qua.
- **Static analysis**: bật rule ReDoS của SonarQube (`java:S5852` — slow regular expressions) và/hoặc công cụ kiểm tra regex trong CI; review mọi `Pattern`/`@Pattern`/`String.matches`/`replaceAll`/`split` với regex phức tạp.
- **Test**: với mỗi regex validate input bên ngoài, thêm test input đối kháng có timeout:
```java
@ParameterizedTest
@ValueSource(ints = {30, 50, 100, 1000})
void emailValidationIsLinear(int n) {
    String evil = "a".repeat(n) + "@gmail";
    assertTimeoutPreemptively(Duration.ofMillis(50), () -> EmailRules.isValid(evil));
}
```
- **Kiến trúc**: gateway không retry POST không idempotent; endpoint xử lý file luôn async + bulkhead; mọi input có giới hạn kích thước (body, số dòng, độ dài trường).
- **Code review checklist**: quantifier lồng nhau `(a+)+`, `(a*)*`, `(a+){m,n}`; alternation chồng lấn `(a|ab)*`; `.*` lặp nhiều lần `(.*x){n}`; regex có chạy trên input do người dùng kiểm soát không? Có giới hạn độ dài trước không?

### Góc lịch sử: `HashMap` vòng lặp vô hạn ở Java 7
Một nguyên nhân CPU 100% kinh điển khác mà interviewer thích hỏi: nhiều thread cùng `put` vào một `HashMap` (thường là `static` cache) trên **Java 7**. Khi resize, `transfer()` chèn node vào **đầu** bucket mới nên đảo ngược thứ tự danh sách; hai thread resize đồng thời có thể tạo **vòng** `A → B → A`. Mọi `get()` sau đó rơi vào bucket này lặp mãi: thread dump thấy nhiều thread RUNNABLE ở `java.util.HashMap.getEntry`/`HashMap.get`, CPU 100%, không bao giờ tự hết. Java 8 chia bucket thành danh sách "lo/hi" giữ nguyên thứ tự nên không còn tạo vòng khi resize, nhưng `HashMap` vẫn **không thread-safe**: mất entry, `size` sai, và đã có báo cáo treo trong thao tác cây (`TreeNode`) khi bị sửa đồng thời. Cách sửa duy nhất: `ConcurrentHashMap` (hoặc đồng bộ hóa), không phải "nâng JDK".

## 8. Cách kể lại trong phỏng vấn (STAR, ~1,5 phút)

- **S:** "Service onboarding của chúng tôi có 12 pod, mỗi pod 2 CPU. Một sáng thứ Hai, 3 pod chạm 100% CPU limit, health check fail, rồi lan sang thêm 2 pod; p99 toàn service từ 180 ms lên hơn 4 s."
- **T:** "Tôi trực on-call, cần khôi phục dịch vụ và tìm nguyên nhân."
- **A:** "Tôi loại trừ GC và JIT bằng metric GC và `top -H`: hai thread `http-nio` mỗi cái ăn trọn một core. Thread dump ba lần cách nhau 10 giây đều thấy thread nằm trong `java.util.regex.Pattern$Loop`, và cột `cpu=` cho thấy nó đã đốt 187 giây CPU trong khi gateway timeout từ giây thứ 30; async-profiler xác nhận 98% CPU ở validator email. Input là các email thiếu đuôi tên miền trong file import. Một commit hai tuần trước đổi `*` thành `{1,64}` để giới hạn độ dài — trên JDK 9+ vòng lặp `*` được memoize nên an toàn, còn `{1,64}` thì không, thành backtracking hàm mũ: 35 ký tự mất 47 giây. Tôi tắt retry của gateway cho route đó, restart pod nóng, hotfix kiểm tra độ dài trước regex, rồi viết lại regex không nhập nhằng và chuyển import sang job async với pool riêng."
- **R:** "File gần 5.000 dòng import trong 1,4 giây. Chúng tôi bật rule ReDoS trong Sonar, tìm ra thêm 2 regex cùng dạng, thêm test input đối kháng có timeout và quy tắc gateway không retry POST."

## 9. Câu hỏi mở rộng

<details>
<summary>1. Vì sao thread vẫn chạy sau khi gateway đã trả 504? Làm sao dừng nó?</summary>

Servlet container không có cơ chế giết thread khi client ngắt kết nối; việc đóng socket chỉ được phát hiện khi ứng dụng **ghi** response. Code CPU-bound như `Matcher.matches()` không kiểm tra cờ interrupt, nên kể cả `Future.cancel(true)` cũng không dừng được. Cách đúng: thiết kế code dài có điểm kiểm tra hủy (kiểm tra `Thread.interrupted()` hoặc deadline giữa các dòng), dùng `CharSequence` có deadline cho regex, và chạy tác vụ nặng trong pool có giới hạn. `Thread.stop()` đã bị vô hiệu hóa (ném `UnsupportedOperationException` từ JDK 20).
</details>

<details>
<summary>2. "CPU 100%" trên dashboard nhưng app thực tế đang bị throttle — phân biệt thế nào?</summary>

Trong container có CPU limit, "100%" thường nghĩa là chạm **quota** CFS chứ không phải hết CPU của node. Xem `container_cpu_cfs_throttled_periods_total`/`throttled_seconds_total` hoặc `/sys/fs/cgroup/cpu.stat` (`nr_throttled`, `throttled_usec`). Throttling cũng có thể xảy ra khi CPU **trung bình** thấp nhưng có burst ngắn (GC song song nhiều thread đốt quota trong vài ms) — p99 tăng mà dashboard CPU trung bình chỉ 40%. Trong case này là CPU thật bị ăn bởi 2 thread, throttling là hệ quả.
</details>

<details>
<summary>3. Vì sao dùng async-profiler thay vì VisualVM sampler hay một số APM?</summary>

Profiler dựa trên `Thread.getAllStackTraces`/JVMTI chỉ lấy mẫu tại **safepoint** → báo sai hotspot (safepoint bias), đặc biệt với vòng lặp counted đã được JIT bỏ safepoint poll. async-profiler dùng `perf_events` + `AsyncGetCallTrace` nên lấy mẫu ở bất kỳ điểm nào, thấy cả frame native/kernel, overhead thấp, chạy được trên production. JFR cũng không bị safepoint bias cho method sampling và luôn có sẵn trong JDK.
</details>

<details>
<summary>4. Nếu <code>top -H</code> cho thấy các thread "GC Thread#" ăn CPU thì sao?</summary>

Đó là triệu chứng, không phải nguyên nhân: GC chạy liên tục vì heap gần đầy (leak — xem case 01), allocation rate quá cao, hoặc heap quá nhỏ. Bước tiếp: `jstat -gcutil` (cột `O`, `FGC`, `GCT`), GC log (loại pause, `To-space exhausted`, Full GC), metric live set sau GC; nếu live set tăng → heap dump; nếu allocation rate cao → async-profiler `-e alloc`.
</details>

<details>
<summary>5. Tại sao pattern <code>^([a-zA-Z0-9]+)*@example\.com$</code> trong sách lại chạy nhanh trên JDK 17/21?</summary>

Từ JDK 9, `java.util.regex` ghi nhớ (memoize) các vị trí đã thử thất bại cho vòng lặp group greedy không giới hạn (`*`, `+`) trong nhiều trường hợp, nên nhiều ví dụ ReDoS kinh điển không còn bùng nổ. Tối ưu này **không** áp dụng cho quantifier có giới hạn `{m,n}`, cho pattern có backreference, và một số cấu trúc khác như `(.*a){12}` — vẫn hàm mũ. Kết luận cho phỏng vấn: đừng dựa vào tối ưu của engine; hãy viết regex không nhập nhằng, giới hạn độ dài và đo bằng input đối kháng trên đúng JDK đang chạy.
</details>

<details>
<summary>6. Một thread nóng thì restart pod là xong — vì sao phải tìm nguyên nhân gốc kỹ như vậy?</summary>

Vì input gây lỗi do **người ngoài** kiểm soát: đây là lỗ hổng ReDoS (OWASP: Denial of Service). Kẻ xấu chỉ cần vài request nhỏ là chiếm hết CPU toàn cụm, và retry/autoscaling còn khuếch đại. Restart chỉ xóa triệu chứng; không sửa thì sự cố lặp lại bất cứ lúc nào, kể cả có chủ đích.
</details>

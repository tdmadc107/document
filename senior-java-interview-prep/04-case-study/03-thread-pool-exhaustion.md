# Case 03 — p99 tăng vọt, request timeout hàng loạt: downstream chậm không có timeout làm cạn thread Tomcat

> **Chủ đề:** Thread pool exhaustion, cascading failure, timeout, bulkhead, circuit breaker, fallback
> **Module liên quan:** [M14 §4 — Resilience: timeout, retry, circuit breaker, bulkhead](../01-giao-trinh/14-microservices-system-design.md#p4) · [M08 §7 — HTTP clients, timeout & connection pool](../01-giao-trinh/08-spring-web-rest-security.md#p7) · [M04 §8 — ThreadPoolExecutor & sizing](../01-giao-trinh/04-concurrency.md#p8) · [M05 §13 — Little's Law, đo hiệu năng](../01-giao-trinh/05-jvm-memory-gc-performance.md#p13) · [M07 §12 — Actuator & observability](../01-giao-trinh/07-spring-core-boot.md#p12) · [M07 §14 — Virtual threads](../01-giao-trinh/07-spring-core-boot.md#p14) · [M16 §7 — Kubernetes probes](../01-giao-trinh/16-devops-build-cloud-security.md#p7)
> **Độ khó:** ⭐⭐⭐ (Senior)
> **Thời gian tự giải gợi ý:** 35 phút

---

## 1. Bối cảnh hệ thống

`order-service` của một sàn bán lẻ: xem giỏ hàng, tính phí vận chuyển, đặt hàng, xem lịch sử đơn.

```
Mobile/Web ─► API Gateway (timeout 15 s) ─► order-service (6 pod) ─┬─► PostgreSQL (HikariCP 20/pod)
                                                                    ├─► inventory-service (nội bộ)
                                                                    └─► CarrierX API (đối tác giao hàng, Internet)
```

| Thành phần | Chi tiết |
|---|---|
| Stack | Java 21, Spring Boot 3.2, Spring MVC, Tomcat (`threads.max=200`, `accept-count=100`, `max-connections=8192`) |
| Triển khai | K8s 6 pod, 2 CPU / 2Gi; liveness và readiness đều là `/actuator/health/*` **trên cùng port 8080** |
| Tải | ~720 RPS toàn cụm (~120 RPS/pod); trong đó `GET /cart/shipping-quote` ~25 RPS/pod |
| Bình thường | CarrierX p99 ≈ 400 ms; `order-service` p99 ≈ 250 ms; CPU ~35% |

## 2. Triệu chứng

**Timeline tối thứ Sáu (giờ VN):**
```
19:40  CarrierX bắt đầu suy giảm (sau này mới biết: sự cố load balancer phía họ, giữ TCP mở nhưng không trả response)
19:41  order-service p99 250 ms → 15 s (chạm timeout gateway); error rate 504 từ 0,1% → 38%
19:43  Pod bắt đầu bị restart do liveness fail; restart storm 14 lần trong 15 phút
19:45  Lỗi lan sang cả các endpoint không liên quan: GET /orders/history, POST /orders
20:01  Khôi phục sau khi tắt tính năng báo giá vận chuyển trực tiếp (feature flag)
```

**Grafana — pod `order-service-5f7c-abc12`:**
```
tomcat_threads_busy_threads           18  →  200  (= max) trong ~8 giây
tomcat_threads_config_max_threads     200
process_cpu_usage                     0.35 → 0.04         ← CPU GIẢM khi sự cố
hikaricp_connections_active           6   →  1
http_server_requests_seconds p99      0.25 s → 15 s (bị gateway cắt)
http_client_requests_seconds_count{uri="/v2/quotes"}  gần như ngừng tăng   ← call không bao giờ kết thúc nên không được ghi nhận
```

**Log gateway:**
```
2026-09-25T19:41:22+07:00 [error] upstream timed out (110: Connection timed out) while reading response header from upstream,
  upstream: "http://10.42.3.17:8080/cart/shipping-quote?cartId=...", request_time=15.001
2026-09-25T19:42:05+07:00 [error] upstream timed out ... upstream: "http://10.42.3.17:8080/orders/history?page=0", request_time=15.000
```

**K8s events:**
```
Warning  Unhealthy  pod/order-service-5f7c-abc12  Liveness probe failed: Get "http://10.42.3.17:8080/actuator/health/liveness":
         context deadline exceeded (Client.Timeout exceeded while awaiting headers)
Normal   Killing    pod/order-service-5f7c-abc12  Container app failed liveness probe, will be restarted
```

**Log ứng dụng:** gần như **không có lỗi nào** trong 5 phút đầu — chính điều này làm on-call mất thời gian.

## 3. Câu hỏi đặt ra

1. Vì sao CPU giảm, DB nhàn rỗi, log không có lỗi, mà service lại "chết"?
2. Vì sao `GET /orders/history` — không gọi CarrierX — cũng timeout?
3. Liveness probe đã làm sự cố tệ hơn như thế nào?
4. Thiết kế lại lời gọi sang CarrierX thế nào để một đối tác chậm không kéo sập cả service?

> ✋ **Dừng lại và tự giải trước.** Gợi ý: dùng định luật Little để tính cần bao nhiêu thread khi CarrierX phản hồi sau 30 giây.

## 4. Điều tra từng bước

### Bước 1 — Mô hình USE cho từng tài nguyên
| Tài nguyên | Utilization | Saturation | Errors | Kết luận |
|---|---|---|---|---|
| CPU | 4% | không throttle | – | Không phải |
| Heap/GC | bình thường | – | – | Không phải |
| DB pool (Hikari) | 1/20 active | pending = 0 | 0 timeout | Không phải |
| **Tomcat worker thread** | **200/200** | **hàng đợi kết nối tăng** | 504 ở gateway | ✅ Bão hòa |

Thread bận 100% nhưng CPU gần 0 → thread đang **chờ** một thứ gì đó (I/O, lock).

### Bước 2 — Thread dump: thread đang chờ cái gì?
```bash
kubectl exec order-service-5f7c-abc12 -- jcmd 1 Thread.print > td1.txt
grep -c '"http-nio-8080-exec' td1.txt                         # 200
grep -A 25 '"http-nio-8080-exec' td1.txt | grep -E '^\s+at com\.acme' | sort | uniq -c | sort -rn
    196  at com.acme.order.shipping.CarrierXClient.quote(CarrierXClient.java:44)
      3  at com.acme.order.history.OrderHistoryController.list(OrderHistoryController.java:31)
      1  at com.acme.order.checkout.CheckoutController.place(CheckoutController.java:58)
```
Một stack điển hình:
```
"http-nio-8080-exec-143" #187 [201] daemon prio=5 os_prio=0 cpu=412.55ms elapsed=118.40s tid=0x00007f1a2c41e2a0 nid=201 runnable  [0x00007f19c5dfc000]
   java.lang.Thread.State: RUNNABLE
	at sun.nio.ch.SocketDispatcher.read0(java.base@21.0.4/Native Method)
	at sun.nio.ch.SocketDispatcher.read(java.base@21.0.4/SocketDispatcher.java:48)
	at sun.nio.ch.NioSocketImpl.tryRead(java.base@21.0.4/NioSocketImpl.java:256)
	at sun.nio.ch.NioSocketImpl.implRead(java.base@21.0.4/NioSocketImpl.java:307)
	at sun.nio.ch.NioSocketImpl.read(java.base@21.0.4/NioSocketImpl.java:346)
	at java.net.Socket$SocketInputStream.read(java.base@21.0.4/Socket.java:1099)
	at sun.security.ssl.SSLSocketInputRecord.read(java.base@21.0.4/SSLSocketInputRecord.java:489)
	...
	at sun.net.www.http.HttpClient.parseHTTPHeader(java.base@21.0.4/HttpClient.java:827)
	at sun.net.www.http.HttpClient.parseHTTP(java.base@21.0.4/HttpClient.java:759)
	at sun.net.www.protocol.http.HttpURLConnection.getInputStream0(java.base@21.0.4/HttpURLConnection.java:1720)
	at java.net.HttpURLConnection.getResponseCode(java.base@21.0.4/HttpURLConnection.java:531)
	at org.springframework.http.client.SimpleClientHttpResponse.getStatusCode(SimpleClientHttpResponse.java:55)
	at org.springframework.web.client.DefaultResponseErrorHandler.hasError(DefaultResponseErrorHandler.java:...)
	at org.springframework.web.client.RestTemplate.handleResponse(RestTemplate.java:...)
	at org.springframework.web.client.RestTemplate.doExecute(RestTemplate.java:...)
	at com.acme.order.shipping.CarrierXClient.quote(CarrierXClient.java:44)
```
Ba điểm cần đọc ra:
- **`Thread.State: RUNNABLE`** dù thread đang chờ mạng: blocking socket read nằm trong native code nên JVM báo RUNNABLE. Đừng chỉ đếm `WAITING`/`BLOCKED` để tìm thread "kẹt".
- `cpu=412ms elapsed=118s` → 99,7% thời gian thread không làm gì.
- Stack đi qua `SimpleClientHttpResponse` → đây là `RestTemplate` dùng `HttpURLConnection` mặc định, **timeout = 0 (vô hạn)**.

Dump thứ hai sau 15 s: cùng các thread vẫn ở đúng chỗ đó → kẹt thật sự.

### Bước 3 — Vì sao endpoint khác cũng chết?
Mọi endpoint dùng **chung** 200 thread. Khi 196 thread kẹt ở CarrierX, request `GET /orders/history` vẫn được NIO connector nhận (tới `max-connections`) nhưng nằm chờ trong hàng đợi đến khi có thread rảnh — không bao giờ có trước khi gateway hết 15 s. Đây là **cascading failure trong một process**: một dependency chậm chiếm hết tài nguyên dùng chung.

### Bước 4 — Định lượng bằng định luật Little
`L = λ × W` (số request đồng thời = throughput × thời gian xử lý):
- Bình thường: 25 RPS × 0,4 s = **10 thread** cho báo giá.
- Sự cố: CarrierX giữ kết nối ~∞; kể cả nếu "chỉ" 30 s: 25 × 30 = **750 thread** > 200.
- Thời gian để cạn pool: 200 / 25 RPS ≈ **8 giây** — khớp với Grafana.

### Bước 5 — Vì sao restart storm?
Liveness probe gọi `/actuator/health/liveness` trên cùng connector → cũng xếp hàng chờ thread → timeout 1 s × 3 lần → kubelet giết container. Pod mới khởi động (~25 s), nhận traffic, 8 giây sau lại cạn → lại bị giết. Restart không chữa được gì mà còn làm mất request đang xử lý và dồn tải sang pod còn sống.

## 5. Nguyên nhân gốc

```java
@Component
public class CarrierXClient {
    private final RestTemplate rest = new RestTemplate();   // ❌ SimpleClientHttpRequestFactory: connect/read timeout vô hạn

    public ShippingQuote quote(Cart cart) {
        return rest.postForObject("https://api.carrierx.example/v2/quotes",
                QuoteRequest.from(cart), ShippingQuote.class);   // ❌ không timeout, không circuit breaker, không fallback
    }
}
```
Và cấu hình probe:
```yaml
livenessProbe:
  httpGet: { path: /actuator/health/liveness, port: 8080 }   # ❌ chung connector với traffic
  timeoutSeconds: 1
  failureThreshold: 3
```
Nguyên nhân gốc là **thiếu ranh giới thời gian và ranh giới tài nguyên** cho một dependency bên ngoài: không timeout, không giới hạn số call đồng thời, không đường lui khi đối tác hỏng. Sự cố của CarrierX chỉ là tác nhân kích hoạt.

## 6. Giải pháp

### Ngắn hạn (đã làm lúc 19:58)
1. Bật feature flag `shipping.live-quote.enabled=false` → trả phí ship ước tính theo bảng giá tĩnh (fallback nghiệp vụ đã có sẵn cho app cũ).
2. Tạm nới `livenessProbe.failureThreshold` lên 10 để chấm dứt restart storm.

### Dài hạn
**(1) Timeout ở mọi tầng, chọn theo p99 của dependency:**
```java
@Bean
RestClient carrierXRestClient(RestClient.Builder builder) {
    var cm = PoolingHttpClientConnectionManagerBuilder.create()
            .setMaxConnPerRoute(30).setMaxConnTotal(30)
            .setDefaultConnectionConfig(ConnectionConfig.custom()
                    .setConnectTimeout(Timeout.ofSeconds(1))
                    .setSocketTimeout(Timeout.ofMillis(1500))
                    .build())
            .build();
    var http = HttpClients.custom()
            .setConnectionManager(cm)
            .setDefaultRequestConfig(RequestConfig.custom()
                    .setConnectionRequestTimeout(Timeout.ofMillis(200))   // chờ mượn connection
                    .setResponseTimeout(Timeout.ofMillis(1500))           // p99 400 ms → 1,5 s là đủ biên
                    .build())
            .build();
    return builder.baseUrl("https://api.carrierx.example")
                  .requestFactory(new HttpComponentsClientHttpRequestFactory(http))
                  .build();                               // builder do Boot inject → có metrics http.client.requests
}
```

**(2) Bulkhead + circuit breaker + fallback (Resilience4j):**
```yaml
resilience4j:
  bulkhead:
    instances:
      carrierX:
        max-concurrent-calls: 20      # tối đa 20/200 thread được phép đứng chờ CarrierX
        max-wait-duration: 0          # hết chỗ → từ chối ngay, không xếp hàng
  circuitbreaker:
    instances:
      carrierX:
        sliding-window-type: TIME_BASED
        sliding-window-size: 10        # 10 giây gần nhất
        minimum-number-of-calls: 20
        failure-rate-threshold: 50
        slow-call-duration-threshold: 1s
        slow-call-rate-threshold: 50
        wait-duration-in-open-state: 15s
        permitted-number-of-calls-in-half-open-state: 5
```
```java
@Service
class ShippingQuoteService {
    @Bulkhead(name = "carrierX")
    @CircuitBreaker(name = "carrierX", fallbackMethod = "estimatedQuote")
    public ShippingQuote quote(Cart cart) {
        return carrierX.post().uri("/v2/quotes").body(QuoteRequest.from(cart))
                       .retrieve().body(ShippingQuote.class);
    }

    ShippingQuote estimatedQuote(Cart cart, Throwable t) {
        log.warn("CarrierX unavailable ({}), fallback to rate table", t.getClass().getSimpleName());
        return rateTable.estimate(cart).markEstimated();   // UI hiển thị "phí dự kiến", chốt lại khi tạo vận đơn
    }
}
```
Không thêm retry cho báo giá: dữ liệu không quan trọng tới mức đáng nhân đôi tải lên đối tác đang yếu; có cache báo giá 10 phút theo `(kho, vùng, khối lượng làm tròn)`.

**(3) Tách health check khỏi traffic, và liveness không phụ thuộc dependency:**
```yaml
management:
  server.port: 8081                       # connector riêng, thread riêng
  endpoint.health.probes.enabled: true
  endpoint.health.group.liveness.include: livenessState         # chỉ "JVM còn sống không"
  endpoint.health.group.readiness.include: readinessState,db    # không đưa CarrierX vào readiness
```
Readiness có `db` vì không có DB thì pod thật sự không phục vụ được; nhưng **không** đưa dependency bên ngoài vào — nếu không, CarrierX hỏng sẽ làm *tất cả* pod NotReady cùng lúc.

### So sánh các lựa chọn
| Biện pháp | Giải quyết gì | Giá phải trả / giới hạn |
|---|---|---|
| Timeout (connect, response, pool acquire) | Giới hạn thời gian mỗi thread bị giữ | Một mình nó chưa đủ: 25 RPS × 1,5 s = 38 thread vẫn bị giữ khi đối tác chậm hàng loạt |
| Bulkhead (semaphore) | Giới hạn **số thread** dependency được chiếm | Phải chọn ngưỡng; call bị từ chối cần fallback |
| Bulkhead (thread pool riêng) | Cô lập mạnh hơn, có thể timeout bằng `Future` | Thêm context switch, mất ThreadLocal/MDC nếu không propagate |
| Circuit breaker | Fail fast khi đối tác hỏng, cho đối tác thời gian hồi phục | Cấu hình sai (mặc định `slowCallDurationThreshold=60s`) = vô dụng |
| Retry | Che lỗi tạm thời | Khuếch đại tải lên dependency đang yếu; chỉ khi idempotent + backoff + jitter |
| Fallback / cache | Giữ trải nghiệm người dùng | Dữ liệu ước tính/cũ; phải được nghiệp vụ chấp nhận |
| WebClient (reactive) | Không chiếm thread khi chờ | Đổi mô hình lập trình; vẫn cần timeout, nếu không thì kết nối và bộ nhớ chồng chất |
| Virtual threads (`spring.threads.virtual.enabled=true`) | Bỏ giới hạn 200 thread | **Không** chữa gốc: không timeout thì hàng nghìn virtual thread + socket treo, connection pool HTTP cạn, bộ nhớ tăng; vẫn cần timeout + bulkhead |

## 7. Phòng ngừa

**Alert:**
```promql
# Thread Tomcat gần bão hòa
max by (pod) (tomcat_threads_busy_threads / tomcat_threads_config_max_threads) > 0.8

# Circuit breaker mở
max by (name) (resilience4j_circuitbreaker_state{state="open"}) == 1

# Bulkhead hết chỗ (mọi slot đều đang bị chiếm) kéo dài
max_over_time(resilience4j_bulkhead_available_concurrent_calls[2m]) == 0
```

**Test:**
- Chaos/fault-injection test trong staging: WireMock/Toxiproxy làm CarrierX trả chậm 30 s hoặc giữ kết nối; tiêu chí đạt: `GET /orders/history` p99 không đổi, `shipping-quote` trả fallback < 1,6 s.
- Unit test cho cấu hình client: assert `RestClient` có timeout (đọc từ bean), tránh ai đó lại `new RestTemplate()`.
- ArchUnit rule: cấm `new RestTemplate()`/`RestClient.create()` ngoài package `config`.

**Code review checklist cho mọi lời gọi ra ngoài:**
- [ ] Connect timeout, response timeout, pool acquire timeout — có, và dựa trên p99 của dependency?
- [ ] Giới hạn concurrency (bulkhead) cho dependency không thuộc quyền kiểm soát?
- [ ] Circuit breaker được cấu hình tường minh (không dùng mặc định)?
- [ ] Có fallback nghiệp vụ hợp lý, hoặc lỗi rõ ràng (503) thay vì treo?
- [ ] Retry: chỉ cho thao tác idempotent, có backoff + jitter, chỉ ở một tầng?
- [ ] Timeout tầng dưới < timeout tầng trên (deadline propagation)?

**Quy trình:** mỗi dependency bên ngoài có "dependency card" ghi SLA, p99, timeout, fallback, người liên hệ; game day định kỳ.

## 8. Cách kể lại trong phỏng vấn (STAR, ~1,5 phút)

- **S:** "Order service của chúng tôi gọi API báo giá của một đối tác vận chuyển. Một tối thứ Sáu, đối tác gặp sự cố giữ kết nối mà không trả lời; trong 8 giây cả 200 thread Tomcat của mỗi pod bị chiếm, p99 lên 15 giây, 38% request lỗi, kể cả các endpoint không liên quan, và pod bị liveness probe restart liên tục."
- **T:** "Tôi là người xử lý chính: khôi phục dịch vụ và đảm bảo một đối tác chậm không thể kéo sập service lần nữa."
- **A:** "Metric cho thấy thread bận 100% nhưng CPU 4% và DB rảnh — thread đang chờ I/O. Thread dump: 196/200 thread nằm trong `socketRead` của `RestTemplate` mặc định, timeout vô hạn. Tôi tắt báo giá trực tiếp bằng feature flag để dùng bảng giá ước tính, nới liveness để dừng restart storm — 20 phút là phục hồi. Sau đó tôi thêm timeout theo p99 của đối tác, bulkhead 20 call đồng thời, circuit breaker cấu hình tường minh với slow-call threshold 1 giây, fallback bảng giá, tách management port và bỏ dependency ngoài khỏi probe."
- **R:** "Ba tuần sau đối tác lại sự cố 40 phút: circuit breaker mở sau ~10 giây, người dùng thấy 'phí dự kiến', các endpoint khác không bị ảnh hưởng. Chúng tôi đưa checklist resilience vào review và chạy fault-injection test hằng quý."

## 9. Câu hỏi mở rộng

<details>
<summary>1. Tăng <code>server.tomcat.threads.max</code> lên 1000 có giải quyết được không?</summary>

Không. Theo Little: đối tác treo vô hạn thì cần vô hạn thread — 1000 chỉ kéo dài thời gian cạn từ 8 s lên 40 s. Mỗi platform thread tốn stack (~1 MB reserve), context switch, và khi đối tác hồi phục, 1000 request dồn về cùng lúc có thể làm sập DB. Kích thước pool phải dựa trên tài nguyên phía sau (CPU, DB connection), còn chờ đợi phải bị chặn bằng timeout và bulkhead.
</details>

<details>
<summary>2. Chọn giá trị timeout thế nào? Timeout 1,5 s nhưng gateway 15 s có ổn không?</summary>

Lấy từ p99/p99.9 thực đo của dependency cộng biên (ở đây p99 400 ms → 1,5 s), đối chiếu với SLA người dùng. Timeout phải giảm dần theo chiều gọi (deadline propagation): gateway 15 s > order-service xử lý ~3 s > từng call xuống dưới 1,5 s, nếu không tầng trên đã bỏ cuộc mà tầng dưới vẫn làm việc vô ích. Có retry thì tổng `attempts × timeout + backoff` vẫn phải nằm trong ngân sách của tầng trên — dùng `TimeLimiter`/deadline tổng.
</details>

<details>
<summary>3. Semaphore bulkhead và ThreadPool bulkhead khác nhau gì? Chọn cái nào?</summary>

Semaphore bulkhead chỉ đếm số call đồng thời, chạy trên thread của caller: nhẹ, giữ nguyên ThreadLocal/MDC/transaction context, hợp với code blocking và virtual threads; nhưng không tự ngắt được call đang chạy — vẫn phụ thuộc timeout của HTTP client. ThreadPool bulkhead chạy call trên pool riêng và trả `CompletionStage`, có thể bỏ chờ bằng `TimeLimiter`, cô lập mạnh hơn, nhưng tốn thread, thêm độ trễ chuyển thread và phải propagate context. Với MVC + HTTP client đã có timeout, semaphore là đủ và đơn giản hơn.
</details>

<details>
<summary>4. Nếu bật virtual threads ngay từ đầu thì sự cố có xảy ra không?</summary>

Tomcat không còn giới hạn 200 thread nên `GET /orders/history` vẫn được phục vụ lâu hơn — nhưng: hàng chục nghìn virtual thread treo trên socket, mỗi cái giữ stack trên heap và một kết nối HTTP; pool connection tới CarrierX (nếu có giới hạn) cạn và mọi call xếp hàng; nếu đoạn code chờ nằm trong `synchronized` (trước JDK 24) thì carrier thread bị pin và có thể cạn cả carrier pool. Virtual threads thay đổi **chi phí** của chờ đợi, không loại bỏ nhu cầu timeout và giới hạn concurrency.
</details>

<details>
<summary>5. Vì sao <code>http_client_requests_seconds</code> không báo động trong sự cố này?</summary>

Timer chỉ ghi nhận khi call **kết thúc**. Call treo vô hạn không bao giờ kết thúc nên không xuất hiện trong histogram — dashboard latency của client trông "bình thường" hoặc trống. Những metric đáng tin hơn trong tình huống này: số thread bận, `LongTaskTimer` cho call đang chạy (active tasks + duration), số kết nối đang leased của HTTP client pool, và trạng thái circuit breaker. Đây là lý do nên có alert trên **saturation**, không chỉ trên latency của request đã hoàn thành.
</details>

<details>
<summary>6. Thiết kế liveness và readiness probe đúng cho service Spring Boot?</summary>

Liveness trả lời "process có cần bị giết không" — chỉ nên fail khi app ở trạng thái không tự hồi phục (deadlock, `LivenessState.BROKEN`); không phụ thuộc DB hay dependency ngoài. Readiness trả lời "có nên nhận traffic không" — fail khi đang khởi động, đang shutdown, hoặc mất dependency **bắt buộc** (DB). Đặt management port riêng để probe không xếp hàng sau traffic; dùng `startupProbe` cho thời gian khởi động dài thay vì nới `initialDelaySeconds`. Probe sai cách biến một sự cố suy giảm thành sự cố sập toàn phần.
</details>

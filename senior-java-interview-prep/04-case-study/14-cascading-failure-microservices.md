# Case 14 — Một service gợi ý sản phẩm chậm kéo sập checkout toàn sàn: cascading failure và retry storm

> **Chủ đề:** Cascading failure, Little's Law, timeout, retry storm, circuit breaker, bulkhead, fallback, load shedding, health probe, Resilience4j
> **Module liên quan:** [M14 §4 — Resilience: timeout, retry, circuit breaker, bulkhead](../01-giao-trinh/14-microservices-system-design.md#p4) · [M14 §7 — Observability](../01-giao-trinh/14-microservices-system-design.md#p7) · [M08 §7 — HTTP clients, timeout & connection pool](../01-giao-trinh/08-spring-web-rest-security.md#p7) · [M04 §8 — ThreadPoolExecutor](../01-giao-trinh/04-concurrency.md#p8) · [M04 §14 — Virtual Threads](../01-giao-trinh/04-concurrency.md#p14) · [M16 §7 — Kubernetes probes](../01-giao-trinh/16-devops-build-cloud-security.md#p7) · [M07 §12 — Actuator](../01-giao-trinh/07-spring-core-boot.md#p12)
> **Độ khó:** ⭐⭐⭐⭐ (Senior)
> **Thời gian tự giải gợi ý:** 45 phút

---

## 1. Bối cảnh hệ thống

```
                           ┌──────────────► cart-service
 App/Web ─► API Gateway ─► checkout-bff ──┼──────────────► pricing-service
  (retry 3)   (retry 2)    (40 pod,       ├──────────────► payment-service
                            Tomcat 200    └──────────────► recommendation-service ─► feature store (Elasticsearch)
                            threads/pod)                    ("Mua kèm thường được chọn" ở trang checkout)
```

| Thông số | Giá trị |
|---|---|
| Checkout page load | ~1.200 RPS lúc cao điểm buổi tối |
| `checkout-bff` | Spring Boot 3.1, Java 17, Spring MVC, `server.tomcat.threads.max=200`, `accept-count=100` |
| Gọi recommendation | `RestTemplate` tạo bằng `new RestTemplate()` — **không connect/read timeout** |
| recommendation p99 bình thường | 120 ms |
| Gateway | Timeout 10 s, retry 2 lần cho 5xx và timeout |
| Mobile app | Retry 3 lần khi lỗi mạng/5xx |
| K8s probes | `livenessProbe` và `readinessProbe` cùng trỏ `/actuator/health` (bao gồm health của mọi dependency qua custom `HealthIndicator`) |

Code BFF (tuần tự, đồng bộ):

```java
@GetMapping("/checkout/{cartId}")
public CheckoutView view(@PathVariable String cartId) {
    Cart cart = cartClient.get(cartId);
    PriceQuote quote = pricingClient.quote(cart);
    List<Product> recs = recommendationClient.forCart(cart);   // widget phụ, không bắt buộc
    return CheckoutView.of(cart, quote, recs);
}
```

---

## 2. Triệu chứng

Tối thứ Sáu, 20:47 — đội search reindex Elasticsearch của feature store, cluster bị merge segment nặng.

```
20:49 [ALERT] recommendation-service p99 = 28.400 ms
20:51 [ALERT] checkout-bff p99 = 10.000 ms (timeout gateway), 5xx = 12%
20:53 [ALERT] checkout-bff pods restarting: 9/40 (Liveness probe failed)
20:55 [ALERT] api-gateway upstream checkout-bff: 503 = 64%
20:58 [ALERT] cart-service, pricing-service: RPS tăng ×4,1 so với baseline
21:02 [ALERT] Order success rate = 4% (SLO 99,5%) — SEV1
```

Kubernetes events:

```
Warning  Unhealthy  pod/checkout-bff-7d9c-2mxkq  Liveness probe failed: Get "http://10.2.3.41:8080/actuator/health": context deadline exceeded (Client.Timeout exceeded while awaiting headers)
Normal   Killing    pod/checkout-bff-7d9c-2mxkq  Container checkout-bff failed liveness probe, will be restarted
```

Thread dump một pod BFF (trước khi bị restart):

```
"http-nio-8080-exec-200" #231 daemon prio=5 RUNNABLE
   at sun.nio.ch.SocketDispatcher.read0(Native Method)
   ...
   at org.springframework.web.client.RestTemplate.doExecute(RestTemplate.java:...)
   at com.shop.bff.client.RecommendationClient.forCart(RecommendationClient.java:37)
# 200/200 thread http-nio ở cùng stack này
```

Đường doanh thu: sập 47 phút, ước tính mất ~18.000 đơn.

---

## 3. Câu hỏi đặt ra

1. Vì sao một widget **không bắt buộc** lại làm sập luồng checkout, và vì sao cart/pricing — không liên quan tới recommendation — cũng bị quá tải?
2. Vì sao pod BFF bị restart, và việc restart làm tình hình tốt hơn hay tệ hơn?
3. Mitigate trong 5 phút đầu thế nào?
4. Thiết kế resilience đúng: timeout bao nhiêu, retry ở đâu, circuit breaker/bulkhead cấu hình ra sao, fallback là gì?

> ✋ **Dừng lại và tự giải trước.** Dùng Little's Law: 1.200 RPS × 28 s = bao nhiêu request đồng thời? So với 40 × 200 thread.

---

## 4. Điều tra từng bước

### Bước 1 — Xác định dependency gây nghẽn (RED từng dependency)

Dashboard client metric `http_client_requests_seconds` theo `uri`/`clientName`: chỉ recommendation có latency tăng; cart/pricing/payment latency bình thường **trước** 20:55. Trace (Tempo) của request checkout chậm: span `GET /recommendations` chiếm 99% thời gian.

### Bước 2 — Little's Law cho thấy cạn thread là tất yếu

```
Request đồng thời cần = throughput × latency = 1.200 RPS × 28 s ≈ 33.600
Năng lực = 40 pod × 200 thread = 8.000
```

Mọi thread Tomcat bị giữ chờ recommendation (không read timeout) → request mới xếp hàng trong `accept-count` rồi bị từ chối/timeout → **mọi** endpoint của BFF chết, kể cả endpoint không gọi recommendation.

### Bước 3 — Retry storm

```
1 lần người dùng bấm = 1 × (1 + 3 retry app) × (1 + 2 retry gateway) = tối đa 12 request vào BFF
```

Metric gateway: RPS vào BFF tăng từ 1.200 lên ~4.900. Mỗi request BFF mới (trước khi treo ở recommendation) vẫn gọi cart và pricing trước → cart/pricing chịu tải ×4,1 dù chúng không hỏng gì.

### Bước 4 — Liveness probe khuếch đại sự cố

`/actuator/health` gồm `RecommendationHealthIndicator` gọi recommendation → probe timeout (1 s) → liveness fail 3 lần → kubelet **restart** pod. Pod mới mất 40 s khởi động (JIT nguội), 39 pod còn lại nhận thêm tải → cạn nhanh hơn → bị restart tiếp. Trong lúc đó readiness cũng fail → Service gỡ pod khỏi endpoint → dồn tải lên số pod ít hơn. Vòng xoáy tự củng cố.

Ngoài ra, health endpoint chạy trên **cùng thread pool Tomcat** đã cạn → kể cả khi không gọi dependency, probe cũng không được phục vụ.

### Bước 5 — Mitigation đã làm trong sự cố (timeline)

| Thời gian | Hành động | Tác dụng |
|---|---|---|
| 21:08 | Tắt widget bằng feature flag `checkout.recommendations.enabled=false` (Spring Cloud Config refresh) | BFF không gọi recommendation nữa, thread được giải phóng dần |
| 21:12 | Tắt retry gateway cho route checkout | Tải về ~1.300 RPS |
| 21:15 | Đổi liveness sang `/actuator/health/liveness` (group không chứa dependency) | Dừng vòng restart |
| 21:34 | Order success rate về 99% | |

Flag đã tồn tại nhưng không có trong runbook → mất 20 phút để nghĩ tới.

---

## 5. Nguyên nhân gốc

1. **Không có timeout** khi gọi dependency không quan trọng; dependency chậm giữ tài nguyên dùng chung (thread Tomcat) → cạn kiệt toàn cục.
2. **Không cô lập tài nguyên (bulkhead)** giữa luồng quan trọng (checkout) và tính năng phụ (gợi ý).
3. **Retry nhiều tầng** (app ×4, gateway ×3) khuếch đại tải lên cả service khỏe.
4. **Liveness probe phụ thuộc dependency** → Kubernetes restart pod khỏe về logic, giảm năng lực đúng lúc cần nhất.
5. Fallback và feature flag không được tự động hóa (circuit breaker) và không có trong runbook.

---

## 6. Giải pháp

### 6.1 Ngắn hạn (tuần đầu)

- Timeout cho mọi HTTP client trong BFF; mặc định toàn công ty: connect 200 ms, read theo p99.9 của dependency.
- Probe: tách `liveness` (chỉ trạng thái nội bộ) và `readiness` (không gồm dependency phụ):
  ```yaml
  management:
    endpoint.health.probes.enabled: true
    endpoint.health.group:
      liveness.include: livenessState
      readiness.include: readinessState, db
  ```
- Gateway: bỏ retry cho route checkout, app mobile giảm retry còn 1 lần với backoff + jitter (bản release kế tiếp).
- Đưa flag "tắt recommendation" vào runbook và dashboard.

### 6.2 Dài hạn — thiết kế resilience nhiều lớp

**Lớp 1 — Timeout & connection pool riêng cho mỗi dependency:**

```java
@Bean
RestClient recommendationClient(RestClient.Builder builder) {
    var cm = PoolingHttpClientConnectionManagerBuilder.create()
            .setMaxConnTotal(50).setMaxConnPerRoute(50)                     // không dùng chung pool
            .setDefaultConnectionConfig(ConnectionConfig.custom()
                    .setConnectTimeout(Timeout.ofMilliseconds(200))
                    .setSocketTimeout(Timeout.ofMilliseconds(300)).build())
            .build();
    var http = HttpClients.custom().setConnectionManager(cm)
            .setDefaultRequestConfig(RequestConfig.custom()
                    .setConnectionRequestTimeout(Timeout.ofMilliseconds(50))  // chờ lấy connection
                    .setResponseTimeout(Timeout.ofMilliseconds(300)).build())
            .build();
    return builder.baseUrl("http://recommendation-service")
            .requestFactory(new HttpComponentsClientHttpRequestFactory(http)).build();
}
```

**Lớp 2 — Resilience4j: circuit breaker + bulkhead + fallback, không retry:**

```yaml
resilience4j:
  circuitbreaker:
    instances:
      recommendation:
        sliding-window-type: TIME_BASED
        sliding-window-size: 10                  # 10 giây gần nhất
        minimum-number-of-calls: 20
        failure-rate-threshold: 30
        slow-call-duration-threshold: 250ms
        slow-call-rate-threshold: 50
        wait-duration-in-open-state: 15s
        permitted-number-of-calls-in-half-open-state: 5
        automatic-transition-from-open-to-half-open-enabled: true
        record-exceptions:
          - org.springframework.web.client.ResourceAccessException
          - org.springframework.web.client.HttpServerErrorException
  bulkhead:
    instances:
      recommendation:
        max-concurrent-calls: 25                 # mỗi pod tối đa 25 thread được phép chờ recommendation
        max-wait-duration: 0                     # không chờ: vượt ngưỡng → fallback ngay
```

```java
@Component
@RequiredArgsConstructor
class RecommendationGateway {
    private final RestClient recommendationClient;
    private final Cache<String, List<Product>> popularByCategory;   // Caffeine, làm mới mỗi 10 phút

    @CircuitBreaker(name = "recommendation", fallbackMethod = "fallback")
    @Bulkhead(name = "recommendation")
    public List<Product> forCart(Cart cart) {
        return recommendationClient.get().uri("/recommendations?cart={id}", cart.id())
                .retrieve().body(new ParameterizedTypeReference<>() {});
    }

    private List<Product> fallback(Cart cart, Throwable ex) {
        // suy giảm có kiểm soát: danh sách phổ biến theo danh mục, hoặc rỗng → UI ẩn widget
        return Objects.requireNonNullElse(popularByCategory.getIfPresent(cart.mainCategory()), List.of());
    }
}
```

**Lớp 3 — Song song hóa và đưa tính năng phụ ra khỏi critical path:**

```java
public CheckoutView view(String cartId) {
    Cart cart = cartClient.get(cartId);
    var quoteF = CompletableFuture.supplyAsync(() -> pricingClient.quote(cart), ioExecutor);
    var recsF  = CompletableFuture.supplyAsync(() -> recommendations.forCart(cart), ioExecutor)
                                  .completeOnTimeout(List.of(), 350, TimeUnit.MILLISECONDS);  // ngân sách cứng
    return CheckoutView.of(cart, quoteF.join(), recsF.join());
}
```

Phương án tốt hơn về kiến trúc: trang checkout **không** gọi recommendation ở server; frontend tải widget bằng request riêng (lazy load). Lỗi widget không bao giờ chặn checkout.

**Lớp 4 — Retry đúng chỗ:** chỉ một tầng retry (BFF → dependency idempotent, tối đa 1 lần, backoff + jitter, có retry budget ~10%); gateway và app không retry 5xx của checkout; tôn trọng `Retry-After`.

**Lớp 5 — Load shedding:** khi BFF quá tải, từ chối sớm bằng 503 thay vì để mọi request đều chậm:

```yaml
server:
  tomcat:
    threads.max: 200
    accept-count: 50          # hàng đợi ngắn: thà từ chối nhanh còn hơn chờ 10 s
```

Ở gateway: rate limit theo route và ưu tiên (đặt hàng, thanh toán > trang xem), adaptive concurrency limit (ví dụ Envoy adaptive concurrency hoặc thư viện Netflix `concurrency-limits`).

### 6.3 So sánh các biện pháp

| Biện pháp | Bảo vệ khỏi | Tác dụng từ khi nào | Đánh đổi |
|---|---|---|---|
| Timeout | Treo vô hạn | Ngay call đầu tiên | Chọn sai (quá ngắn) gây lỗi giả |
| Bulkhead | Một dependency chiếm hết thread | Ngay lập tức | Phải định cỡ; từ chối khi vượt |
| Circuit breaker | Tiếp tục gọi dependency đang hỏng | Sau khi đủ mẫu lỗi | Cấu hình mặc định Resilience4j quá "lỳ" |
| Fallback | Trải nghiệm vỡ | Khi lỗi/CB mở | Có thể che giấu lỗi nếu không có metric |
| Retry + budget | Lỗi thoáng qua | — | Khuếch đại tải nếu không giới hạn |
| Load shedding | Quá tải tổng | Khi vượt ngưỡng | Một phần người dùng bị từ chối |
| Async/lazy widget | Coupling luồng chính | Thiết kế | Thay đổi frontend |
| Virtual threads | Cạn thread platform | — | **Không** chữa được: dependency/connection pool vẫn cạn, latency vẫn cao |

---

## 7. Phòng ngừa

**Monitoring & alert**

- Dashboard RED cho **từng dependency** phía client, trạng thái circuit breaker (`resilience4j_circuitbreaker_state`), số lần bulkhead từ chối, số lần fallback.
- Alert Tomcat `tomcat_threads_busy_threads / max > 0,8` trong 2 phút.
- Alert khi tỷ lệ RPS gateway/RPS người dùng tăng (dấu hiệu retry storm).

**Test**

- Chaos test định kỳ (Toxiproxy/Chaos Mesh): thêm latency 30 s vào recommendation trên staging dưới tải 1.200 RPS — tiêu chí: checkout success ≥ 99%, p99 tăng < 400 ms.
- Test tự động cho cấu hình CB (M14 bài 4.1) và test kiểm tra mọi `RestClient` bean đều có timeout (ArchUnit hoặc test duyệt bean).

**Checklist review cho mọi lời gọi đồng bộ**

- [ ] Timeout connect/read/acquire-connection được đặt rõ, dựa trên p99 của dependency?
- [ ] Dependency này là **bắt buộc** hay **tùy chọn** cho luồng? Nếu tùy chọn: fallback là gì?
- [ ] Bulkhead/connection pool riêng?
- [ ] Retry ở tầng nào? Tổng hệ số khuếch đại qua mọi tầng là bao nhiêu?
- [ ] Health/liveness có phụ thuộc dependency không?

**Quy trình:** mỗi service có "dependency matrix" (bắt buộc/tùy chọn, timeout, fallback, flag tắt), runbook liệt kê feature flag degrade; game day mỗi quý.

---

## 8. Cách kể lại trong phỏng vấn (STAR)

- **Situation:** "Tối thứ Sáu, feature store của recommendation bị reindex, latency lên 28 giây. Checkout-BFF gọi recommendation đồng bộ không có timeout để hiện widget 'mua kèm'. Trong 15 phút, toàn bộ checkout sập 47 phút, success rate còn 4%."
- **Task:** "Mình là người xử lý chính phía BFF: khôi phục checkout, rồi dẫn dắt postmortem và thiết kế lại resilience."
- **Action:** "Thread dump cho thấy 200/200 thread Tomcat chờ socket recommendation — Little's Law: 1.200 RPS × 28 s cần ~33 nghìn thread, mình chỉ có 8 nghìn. Retry ở app và gateway nhân tải lên 4 lần, đánh cả vào cart và pricing; liveness probe gọi dependency nên Kubernetes restart pod, làm mất thêm năng lực. Mình tắt widget bằng flag, tắt retry gateway, tách probe. Sau đó đưa vào timeout 300 ms, connection pool riêng, bulkhead 25, circuit breaker theo slow-call rate, fallback danh sách phổ biến; gọi song song với ngân sách 350 ms; chỉ retry ở một tầng; và chaos test định kỳ."
- **Result:** "Chaos test với latency 30 giây trên staging: checkout vẫn 99,7% thành công, p99 tăng 60 ms. Ba tháng sau recommendation sự cố thật 20 phút, khách chỉ thấy widget biến mất. Bài học: tính năng phụ phải được phép hỏng một cách im lặng, và timeout, bulkhead quan trọng hơn circuit breaker vì chúng bảo vệ từ call đầu tiên."

---

## 9. Câu hỏi mở rộng

<details>
<summary>1. Thứ tự Retry và CircuitBreaker trong Resilience4j quan trọng thế nào?</summary>

Mặc định aspect order: `Retry(CircuitBreaker(RateLimiter(TimeLimiter(Bulkhead(fn)))))`. Retry bọc ngoài nên mỗi lần thử đều đi qua CB và được CB đếm; khi CB mở, các lần retry bị từ chối ngay (`CallNotPermittedException`) — phải cấu hình không retry exception này. Nếu đảo, một "call" của CB gồm nhiều lần thử → CB phản ứng chậm.
</details>

<details>
<summary>2. Chọn giá trị timeout thế nào cho "đúng"?</summary>

Dựa trên phân phối latency thực tế của dependency (p99/p99.9) cộng biên nhỏ, và trong ngân sách thời gian của request cha (deadline propagation: tầng dưới phải ngắn hơn tầng trên). Với dependency tùy chọn, timeout là quyết định sản phẩm: "chờ tối đa bao lâu cho widget này?" — ở đây 300–350 ms. Đo lại định kỳ vì latency thay đổi.
</details>

<details>
<summary>3. Chuyển BFF sang virtual threads (Java 21) có tránh được sự cố không?</summary>

Không hoàn toàn. Không còn giới hạn 200 thread, nhưng 33.600 request đồng thời vẫn giữ socket, bộ nhớ, connection pool HTTP (vẫn có giới hạn), và đẩy tải xuống dependency đang hỏng. Latency người dùng vẫn 28 s. Timeout và bulkhead dạng semaphore vẫn bắt buộc — thậm chí quan trọng hơn vì không còn "giới hạn tự nhiên".
</details>

<details>
<summary>4. Circuit breaker đặt per-instance có vấn đề gì không? Có nên dùng service mesh?</summary>

40 pod có 40 trạng thái CB độc lập, mỗi pod tự học — thường chấp nhận được. Service mesh (Istio/Envoy) cung cấp timeout, retry, outlier detection ở tầng hạ tầng, thống nhất giữa nhiều ngôn ngữ; nhưng fallback mang tính nghiệp vụ (trả danh sách phổ biến) vẫn phải ở ứng dụng. Cẩn thận cấu hình retry ở cả mesh và app → lại thành retry nhiều tầng.
</details>

<details>
<summary>5. Phân biệt load shedding và rate limiting.</summary>

Rate limiting giới hạn theo hạn mức đã định trước (theo client/API key), kể cả khi hệ thống còn rảnh. Load shedding phản ứng theo **tình trạng quá tải hiện tại** (CPU, concurrency, queue), loại bỏ request ít quan trọng để giữ phần quan trọng chạy được. Hai cơ chế bổ sung cho nhau.
</details>

<details>
<summary>6. Khi nào readiness nên bao gồm dependency?</summary>

Chỉ khi pod **thực sự không thể** phục vụ bất kỳ request nào nếu thiếu dependency đó và việc gỡ pod khỏi Service có ích (ví dụ DB riêng của pod). Với dependency dùng chung (DB chung, service khác), nếu readiness phụ thuộc nó thì mọi pod cùng not-ready một lúc → 503 toàn phần thay vì suy giảm. Liveness thì không bao giờ phụ thuộc dependency.
</details>

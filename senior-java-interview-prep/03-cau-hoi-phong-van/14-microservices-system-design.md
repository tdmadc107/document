# Câu hỏi phỏng vấn — Module 14: Microservices & System Design

> Giáo trình tương ứng: [Module 14 — Microservices & System Design](../01-giao-trinh/14-microservices-system-design.md)

> **Cách dùng:** đọc câu hỏi, **tự trả lời thành tiếng** (≈30 giây cho ý chính, 2–3 phút cho phần chi tiết) **trước khi** mở đáp án. Với các **đề thiết kế** ở nhóm K, bấm giờ 35–45 phút, tự vẽ sơ đồ trên giấy theo khung 5 bước (yêu cầu → ước lượng → high-level → deep dive → trade-off), rồi mới so với dàn ý đáp án.
>
> **Mức độ:** 🟢 Cơ bản — 🟡 Senior — 🔴 Xoáy sâu. Câu có nhãn **🎯 Tình huống** là câu "bạn sẽ làm gì"; nhãn **📐 Thiết kế** là đề system design.

## Mục lục

| Nhóm | Chủ đề | Câu hỏi |
|---|---|---|
| A | [Monolith, microservices & cách tách service](#nhom-a) | Q1–Q5 |
| B | [Giao tiếp giữa service, API Gateway & BFF](#nhom-b) | Q6–Q9 |
| C | [Service discovery & cấu hình](#nhom-c) | Q10–Q12 |
| D | [Resilience](#nhom-d) | Q13–Q19 |
| E | [Dữ liệu phân tán: 2PC, Saga, CQRS, idempotency](#nhom-e) | Q20–Q25 |
| F | [Consistency, CAP/PACELC & distributed ID](#nhom-f) | Q26–Q29 |
| G | [Observability](#nhom-g) | Q30–Q32 |
| H | [Triển khai an toàn & tiến hóa API](#nhom-h) | Q33–Q35 |
| I | [Bảo mật giữa các service](#nhom-i) | Q36–Q37 |
| J | [Phương pháp system design & building blocks](#nhom-j) | Q38–Q43 |
| K | [Đề thiết kế (system design prompts)](#nhom-k) | Q44–Q50 |

---

<a id="nhom-a"></a>
## A. Monolith, microservices & cách tách service

### Q1. 🟢 Phân biệt monolith, modular monolith, microservices và distributed monolith.

<details><summary>Đáp án</summary>

**Trả lời ngắn:** **Monolith**: một codebase, một tiến trình, thường một DB chung. **Modular monolith**: vẫn một đơn vị deploy nhưng chia **module có ranh giới rõ** (API nội bộ, package private, mỗi module sở hữu schema). **Microservices** (theo Newman): service **deploy độc lập**, mô hình quanh **business domain**, **che giấu thông tin** — đặc biệt không chia sẻ DB. **Distributed monolith** (anti-pattern): nhiều service nhưng phải deploy lock-step, chung DB, gọi đồng bộ chằng chịt — nhận đủ nhược điểm của cả hai.

**Giải thích chi tiết:**

| | Monolith/Modular | Microservices |
|---|---|---|
| Vận hành | Đơn giản | Phức tạp (network, discovery, observability, nhiều pipeline) |
| Transaction | ACID local | Saga, eventual consistency |
| Debug | Stack trace | Distributed tracing |
| Deploy/scale độc lập | Không | Có |
| Fault isolation | Một OOM sập toàn bộ | Cô lập (nếu có resilience) |

- Spring Modulith (`ApplicationModules.of(App.class).verify()`) hoặc ArchUnit giữ ranh giới module bằng test.

**Câu hỏi nối tiếp:**
- *Modular monolith có lợi gì khi sau này tách service?* — Ranh giới đã rõ, module giao tiếp qua event nội bộ (`@ApplicationModuleListener`) → tách ra chỉ là đổi transport.

**⚠️ Câu trả lời gây điểm trừ:**
- "Microservices = service nhỏ" (kích thước không phải tiêu chí chính).
- Không biết khái niệm distributed monolith.

**📖 Ôn lại:** [Phần 1 — Monolith, Modular Monolith, Microservices](../01-giao-trinh/14-microservices-system-design.md#p1)

</details>

### Q2. 🟡 Khi nào nên và không nên dùng microservices? Định luật Conway liên quan gì?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** **Nên** khi: nhiều team (> ~3–5) va chạm trên một codebase; các phần có nhu cầu scale/SLA rất khác nhau; domain đủ ổn định để vẽ ranh giới; tổ chức có năng lực DevOps (CI/CD, container, observability). **Không nên** khi: startup đang tìm product-market fit, team nhỏ, chưa có tự động hóa. Lời khuyên phổ biến: **bắt đầu bằng modular monolith** ("MonolithFirst"), tách khi có lý do cụ thể. **Conway**: kiến trúc phản chiếu cấu trúc giao tiếp của tổ chức → Inverse Conway Maneuver: tổ chức team theo kiến trúc mong muốn.

**Giải thích chi tiết:**
- Microservices là giải pháp cho **vấn đề tổ chức và scale độc lập**, đổi lấy độ phức tạp phân tán.
- Chi phí ẩn: mỗi service cần pipeline, dashboard, alert, on-call, versioning API.

**Câu hỏi nối tiếp:**
- *Đo thế nào để biết việc tách có hiệu quả?* — Deploy frequency, lead time, change failure rate, MTTR (DORA), số lần một thay đổi đụng nhiều service.

**⚠️ Câu trả lời gây điểm trừ:**
- "Microservices luôn scale tốt hơn" mà không nói chi phí.

**📖 Ôn lại:** [Phần 1 — Trade-off](../01-giao-trinh/14-microservices-system-design.md#p1)

</details>

### Q3. 🟡 Bạn tách service theo tiêu chí nào? Dấu hiệu nào cho thấy ranh giới sai?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Tách theo **business capability** (Order, Payment, Inventory, Shipping...) và **bounded context** (DDD) — vùng mà một mô hình có nghĩa nhất quán. "Product" trong Catalog (mô tả, ảnh), Inventory (SKU, số lượng), Shipping (cân nặng, kích thước) là **các mô hình khác nhau**; context chỉ chia sẻ **ID + event**. Dấu hiệu sai: một tính năng thường phải sửa **nhiều service**; giao tiếp **chatty** (một request gọi qua lại 10 lần); transaction nghiệp vụ quan trọng phải trải nhiều service; tách theo **tầng kỹ thuật** (UserDAO-service).

**Giải thích chi tiết:**
- Tiêu chí: high cohesion, low coupling; service sở hữu dữ liệu và **invariant** của nó; đủ nhỏ để một team sở hữu, đủ lớn để không cần distributed transaction cho mọi thao tác.
- Context mapping: Customer–Supplier, Conformist, **Anti-Corruption Layer**, Shared Kernel, Published Language.
- Ví dụ "Price": giá niêm yết (Catalog) khác giá chốt khi đặt (Ordering — phải snapshot vào đơn).

**Câu hỏi nối tiếp:**
- *Nano-services có vấn đề gì?* — Quá nhiều network hop, distributed transaction khắp nơi, chi phí vận hành vượt lợi ích.

**⚠️ Câu trả lời gây điểm trừ:**
- Tạo một `Product` chung cho cả hệ thống (god object) trong shared library.

**📖 Ôn lại:** [Phần 1 — Cách tách service](../01-giao-trinh/14-microservices-system-design.md#p1)

</details>

### Q4. 🔴 🎯 Tình huống: monolith 500k dòng, module Notification được 30 chỗ gọi trực tiếp và đọc bảng `users`. Lập kế hoạch tách thành service không downtime.

<details><summary>Đáp án</summary>

**Trả lời ngắn:** **Strangler Fig**, tách code trước rồi tách dữ liệu: (1) gom 30 chỗ gọi về một interface `NotificationPort` trong monolith; (2) thêm implementation thứ hai phát event `NotificationRequested` **mang sẵn email/tên** (event-carried state) qua **outbox**; (3) dựng service mới tiêu thụ event, **không đọc bảng `users`**; (4) **feature flag** chuyển dần % traffic, so sánh kết quả; (5) xóa code cũ. Rollback = tắt flag.

**Giải thích chi tiết:**
- Trước khi tách: lý do kinh doanh rõ (deploy độc lập, scale gửi email), **observability và CI/CD** sẵn sàng.
- Dữ liệu: ai sở hữu `users`? — monolith (sau là Customer service). Notification nhận dữ liệu cần thiết qua event hoặc giữ bản sao nhỏ (preference, device token) của riêng nó.
- Metric thành công: tỉ lệ gửi thành công, latency end-to-end, không có gửi trùng (idempotency key `orderId+type`), số deploy độc lập.

**Câu hỏi nối tiếp:**
- *Sao không để service mới đọc thẳng DB monolith cho nhanh?* — Tạo coupling schema → không deploy độc lập; là bước đầu của distributed monolith.

**⚠️ Câu trả lời gây điểm trừ:**
- "Big bang rewrite" hoặc tách dữ liệu trước code.

**📖 Ôn lại:** [Phần 1 — Strangler Fig & Góc nhìn Senior](../01-giao-trinh/14-microservices-system-design.md#p1)

</details>

### Q5. 🟡 Vì sao nhiều service dùng chung database hoặc shared library chứa domain model là anti-pattern?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** **Chung DB**: schema là API ngầm — đổi cột phá vỡ service khác, không deploy độc lập, không biết ai đọc/ghi gì, tranh chấp tài nguyên DB. **Shared library chứa domain model**: mọi service phải nâng cấp đồng loạt khi model đổi → lock-step deploy. Cả hai dẫn tới **distributed monolith**.

**Giải thích chi tiết:**
- Database per service có thể là **schema riêng trên cùng cluster** (rẻ, vẫn chia sẻ tài nguyên) hoặc **DB riêng** (cô lập hoàn toàn, polyglot persistence).
- Shared library chấp nhận được: tiện ích kỹ thuật (logging, tracing, client SDK sinh từ contract) — không chứa domain.

**Câu hỏi nối tiếp:**
- *Báo cáo cần join nhiều service?* — CQRS read model/data warehouse nhận event/CDC, không join thẳng DB của service khác.

**⚠️ Câu trả lời gây điểm trừ:**
- "Dùng chung DB cho tiện, mỗi service một số bảng" mà không thấy coupling.

**📖 Ôn lại:** [Phần 1 — Lỗi thường gặp](../01-giao-trinh/14-microservices-system-design.md#p1) · [Phần 5 — Database per service](../01-giao-trinh/14-microservices-system-design.md#p5)

</details>

---

<a id="nhom-b"></a>
## B. Giao tiếp giữa service, API Gateway & BFF

### Q6. 🟢 Khi nào gọi đồng bộ, khi nào bất đồng bộ giữa các service?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Quy tắc thực tế: **query đồng bộ, command/side effect lan truyền bất đồng bộ** khi có thể. Sync khi user chờ kết quả ngay; async cho thông báo sự kiện, workflow dài, fan-out. Tránh **chuỗi gọi đồng bộ sâu** A → B → C → D: latency **cộng dồn**, availability **nhân dồn**.

**Giải thích chi tiết:**

| | Sync (REST/gRPC) | Async (Kafka/RabbitMQ) |
|---|---|---|
| Phản hồi | Ngay | Eventual |
| Temporal coupling | Có | Không |
| Debug | Dễ hơn | Khó hơn |

- Chuỗi sync bắt buộc → **deadline tổng**, timeout mỗi tầng nhỏ hơn tầng trên.

**Câu hỏi nối tiếp:**
- *Làm sao giảm call sync khi cần dữ liệu service khác?* — Giữ bản sao cục bộ qua event (ECST), hoặc API composition ở BFF.

**⚠️ Câu trả lời gây điểm trừ:**
- "Mọi thứ nên async" hoặc "mọi thứ REST cho đơn giản".

**📖 Ôn lại:** [Phần 2 — Đồng bộ vs bất đồng bộ](../01-giao-trinh/14-microservices-system-design.md#p2)

</details>

### Q7. 🟡 REST và gRPC khác nhau thế nào? Khi nào chọn gRPC? Tiến hóa `.proto` an toàn ra sao?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** REST/JSON: text, dễ debug (curl), native trình duyệt, hợp **public API/đối tác**. gRPC: Protobuf nhị phân trên HTTP/2, hợp đồng `.proto` bắt buộc + sinh code, **streaming** 4 kiểu, **deadline/cancellation có sẵn** và propagate qua chuỗi gọi → hợp **service-to-service nội bộ hiệu năng cao**. Tiến hóa proto: **không đổi số field**; field bị xóa thì `reserved` số đó; thêm field mới với số mới.

**Giải thích chi tiết:**
```java
var stub = InventoryServiceGrpc.newBlockingStub(channel)
        .withDeadlineAfter(300, TimeUnit.MILLISECONDS);   // server biết deadline còn lại
```
```java
// REST (Spring 6.1+ RestClient) — luôn có timeout
var f = new SimpleClientHttpRequestFactory();
f.setConnectTimeout(Duration.ofMillis(300));
f.setReadTimeout(Duration.ofSeconds(1));
RestClient client = builder.baseUrl("http://inventory-service").requestFactory(f).build();
```
- Trình duyệt gọi gRPC cần gRPC-Web/proxy.
- Boot 2/Java 8: `RestTemplate`/`WebClient`; `RestClient` từ Spring Framework 6.1.

**Câu hỏi nối tiếp:**
- *Server gRPC biết client đã bỏ cuộc thế nào?* — `Context.current().isCancelled()` → dừng sớm.

**⚠️ Câu trả lời gây điểm trừ:**
- "gRPC luôn tốt hơn REST" hoặc tái sử dụng số field đã xóa.

**📖 Ôn lại:** [Phần 2 — REST vs gRPC](../01-giao-trinh/14-microservices-system-design.md#p2)

</details>

### Q8. 🔴 Vì sao gRPC qua Kubernetes `Service` thường bị mất cân bằng tải? Sửa thế nào?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Kubernetes `Service` (kube-proxy) cân bằng ở **L4 — theo connection**. gRPC dùng HTTP/2 giữ **một connection lâu dài** và multiplex mọi request trên đó → mọi request của một client dồn vào **một pod**. Sửa: **client-side load balancing** (headless service + resolver DNS + policy `round_robin`), hoặc **L7 proxy/service mesh** (Envoy, Istio, Linkerd) cân bằng **per request**.

**Giải thích chi tiết:**
- Headless service (`clusterIP: None`) trả về IP từng pod; client tự giữ nhiều subchannel.
- Pod mới scale lên không nhận traffic cho tới khi client re-resolve → đặt `MAX_CONNECTION_AGE` phía server để buộc client kết nối lại định kỳ.
- Vấn đề tương tự với mọi giao thức connection dài (HTTP keep-alive với ít client).

**Câu hỏi nối tiếp:**
- *L4 vs L7 LB khác gì?* — Xem Q40.

**⚠️ Câu trả lời gây điểm trừ:**
- "Thêm replica là đều tải".

**📖 Ôn lại:** [Phần 2 — Góc nhìn Senior](../01-giao-trinh/14-microservices-system-design.md#p2) · [Phần 11 — Load balancer](../01-giao-trinh/14-microservices-system-design.md#p11)

</details>

### Q9. 🟡 API Gateway và BFF khác nhau thế nào? Gateway không nên làm gì?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** **API Gateway**: điểm vào duy nhất — routing, TLS termination, xác thực JWT, rate limit, CORS, logging/tracing, canary routing. **BFF**: mỗi loại client (mobile, web, đối tác) một backend riêng **do team frontend sở hữu**, aggregate và định dạng dữ liệu tối ưu cho client đó. Gateway **không** chứa business logic (thành monolith mới) và **không retry POST không idempotent** (trừ tiền 2 lần).

**Giải thích chi tiết:**
```yaml
spring.cloud.gateway.routes:          # Spring Cloud 2025.0+: spring.cloud.gateway.server.webflux.routes
  - id: orders
    uri: lb://order-service
    predicates: [ Path=/api/orders/** ]
    filters:
      - StripPrefix=1
      - name: RequestRateLimiter      # token bucket trên Redis
        args: { redis-rate-limiter.replenishRate: 50, redis-rate-limiter.burstCapacity: 100, key-resolver: "#{@userKeyResolver}" }
      - name: Retry
        args: { retries: 2, methods: GET }   # chỉ method idempotent
```
- BFF aggregate nên gọi **song song** (`CompletableFuture`, virtual threads) và **degrade**: thiếu reviews vẫn trả trang (`partial=true`); thiếu catalog → 503.
- `completeOnTimeout` không hủy tác vụ bên dưới — HTTP client vẫn cần timeout riêng.
- Thay thế/kết hợp: GraphQL federation.

**Câu hỏi nối tiếp:**
- *Một gateway cho mọi client có vấn đề gì?* — Chứa logic riêng từng client → nút cổ chai tổ chức.

**⚠️ Câu trả lời gây điểm trừ:**
- Đặt logic tính giá/khuyến mãi trong gateway.

**📖 Ôn lại:** [Phần 2 — API Gateway & BFF](../01-giao-trinh/14-microservices-system-design.md#p2)

</details>

---

<a id="nhom-c"></a>
## C. Service discovery & cấu hình

### Q10. 🟢 Client-side và server-side discovery khác nhau thế nào? Eureka chọn AP hay CP?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** **Client-side** (Eureka + Spring Cloud LoadBalancer): client tra registry, tự chọn instance và gọi trực tiếp — LB linh hoạt (zone-aware, weighted) nhưng mỗi ngôn ngữ cần thư viện. **Server-side** (Kubernetes Service, AWS ALB): client gọi một địa chỉ ổn định, hạ tầng phân phối — trong suốt với client. **Eureka ưu tiên AP**: khi mất nhiều heartbeat, **self-preservation** giữ registration thay vì xóa hết.

**Giải thích chi tiết:**
- Eureka: service đăng ký + heartbeat (mặc định 30s), client cache registry → sau khi instance chết, có độ trễ tới khi cache hết hạn → cần retry/timeout.
- Kubernetes: DNS `order-service.shop.svc.cluster.local`; kube-proxy/eBPF phân phối tới Pod **Ready** — readiness probe chính là health check của registry.
- `@LoadBalanced RestClient.Builder` được hỗ trợ từ Spring Cloud 2023.0.

**Câu hỏi nối tiếp:**
- *Trên K8s còn cần Eureka không?* — Thường không; dùng Service DNS (hoặc Spring Cloud Kubernetes).

**⚠️ Câu trả lời gây điểm trừ:**
- Hard-code IP instance trong cấu hình.

**📖 Ôn lại:** [Phần 3 — Service discovery](../01-giao-trinh/14-microservices-system-design.md#p3)

</details>

### Q11. 🔴 🎯 Tình huống: DB chậm 60 giây, Kubernetes restart **toàn bộ** pod của service, hệ thống sập lâu hơn cả sự cố DB. Lỗi ở đâu? Cấu hình probe và shutdown đúng thế nào?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** **Liveness probe kiểm tra DB** → DB chậm → liveness fail → K8s restart **mọi** pod → cold start đồng loạt → **cascading restart**. Liveness chỉ kiểm tra **tiến trình còn tự phục vụ được** (không deadlock); **readiness** mới phản ánh khả năng nhận traffic (có thể include DB). Thêm **graceful shutdown** (`server.shutdown=graceful`) và `preStop` sleep để kube-proxy kịp gỡ pod khỏi endpoints trước khi app ngừng nhận request.

**Giải thích chi tiết:**
```yaml
readinessProbe:
  httpGet: { path: /actuator/health/readiness, port: 8080 }
  periodSeconds: 5
livenessProbe:
  httpGet: { path: /actuator/health/liveness, port: 8080 }
  initialDelaySeconds: 30
lifecycle:
  preStop: { exec: { command: ["sleep", "10"] } }
```
```properties
management.endpoint.health.group.readiness.include=readinessState,db
server.shutdown=graceful
spring.lifecycle.timeout-per-shutdown-phase=20s
```
- Spring Boot mặc định không đưa DB vào liveness group.
- Kể cả readiness phụ thuộc DB cũng cần cân nhắc: DB chết → mọi pod NotReady → 503 toàn bộ (đôi khi tốt hơn là vẫn Ready và trả lỗi/degrade cho từng endpoint).

**Câu hỏi nối tiếp:**
- *`startupProbe` dùng khi nào?* — App khởi động lâu; tránh liveness giết pod trước khi start xong.

**⚠️ Câu trả lời gây điểm trừ:**
- "Tăng `failureThreshold` của liveness" mà không bỏ kiểm tra dependency.

**📖 Ôn lại:** [Phần 3 — Service discovery (probe)](../01-giao-trinh/14-microservices-system-design.md#p3)

</details>

### Q12. 🟡 Quản lý cấu hình và secret trong microservices thế nào?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** 12-factor: config tách khỏi code, inject theo môi trường; secret **không** nằm plaintext trong git. **Spring Cloud Config** (backend Git/Vault, client dùng `spring.config.import=configserver:` từ Boot 2.4+), refresh bằng `@RefreshScope` + `/actuator/refresh` hoặc **Spring Cloud Bus** broadcast. **K8s ConfigMap/Secret**: mount thành file (cập nhật lan truyền sau độ trễ) — env var **không đổi tới khi restart**. Secret K8s chỉ **base64** → bật encryption at rest, RBAC, hoặc External Secrets/Vault.

**Giải thích chi tiết:**
- `spring.config.import=configtree:/etc/secrets/` đọc mỗi file là một property.
- Thay đổi config là **một kiểu deploy**: review, version, rollout dần, rollback nhanh; `@Validated` trên `@ConfigurationProperties` để fail-fast.
- Boot < 2.4 dùng `bootstrap.yml` cho Config client.

**Câu hỏi nối tiếp:**
- *Refresh config giá trị không hợp lệ?* — Validation từ chối khi rebind, log cảnh báo, giữ giá trị cũ.

**⚠️ Câu trả lời gây điểm trừ:**
- "Secret K8s đã mã hóa rồi".

**📖 Ôn lại:** [Phần 3 — Quản lý cấu hình](../01-giao-trinh/14-microservices-system-design.md#p3)

</details>

---

<a id="nhom-d"></a>
## D. Resilience

### Q13. 🟢 Cascading failure là gì? Little's Law giải thích nó thế nào?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Một dependency chậm làm caller **giữ tài nguyên** (thread, connection) lâu → cạn tài nguyên → caller không phục vụ được cả request **không liên quan** → tầng trên timeout + retry khuếch đại tải → lan ra toàn hệ thống. **Little's Law**: `số request đồng thời = throughput × latency` — latency tăng 10 lần cần gấp 10 lần thread/connection để giữ throughput.

**Giải thích chi tiết:**
```
Payment chậm 5s → 200 thread của Order bị chiếm → Order không phục vụ request khác
→ Gateway timeout + retry ×3 → tải Order ×3 → Order sập → mọi service gọi Order treo
```
- Ví dụ: 200 RPS × 5s = 1000 request đồng thời > 200 thread Tomcat → mọi endpoint xếp hàng.
- Biện pháp: timeout, bulkhead, circuit breaker, retry có kiểm soát, load shedding.

**Câu hỏi nối tiếp:**
- *Virtual threads có giải quyết không?* — Không còn cạn thread, nhưng connection pool, bộ nhớ, dependency phía sau vẫn cạn.

**⚠️ Câu trả lời gây điểm trừ:**
- "Tăng thread pool lên 2000".

**📖 Ôn lại:** [Phần 4 — Cascading failure](../01-giao-trinh/14-microservices-system-design.md#p4)

</details>

### Q14. 🟡 Chọn timeout thế nào? Deadline propagation là gì?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** **Mọi** lời gọi mạng phải có timeout: connect, read/response, và **timeout lấy connection từ pool** (HikariCP `connectionTimeout`, `connectionRequestTimeout`). Giá trị dựa trên **p99/p99.9 latency của dependency** + biên nhỏ, không phải số tròn tùy hứng. **Deadline propagation**: request vào có ngân sách 2s, đã dùng 1.5s thì gọi xuống chỉ còn 0.5s; timeout tầng dưới phải **nhỏ hơn** tầng trên, nếu không tầng dưới làm việc vô ích sau khi tầng trên đã bỏ cuộc.

**Giải thích chi tiết:**
- gRPC propagate deadline sẵn; REST phải tự truyền (header) và tính phần còn lại.
- Timeout + bulkhead quan trọng hơn circuit breaker: bảo vệ **từ call đầu tiên**; CB chỉ phản ứng sau khi đủ lỗi.
- Resilience4j `TimeLimiter` cho `CompletableFuture`; với call blocking, timeout phải đặt ở chính HTTP client.

**Câu hỏi nối tiếp:**
- *Timeout mặc định của các client?* — Nhiều client mặc định vô hạn hoặc rất dài → luôn cấu hình tường minh.

**⚠️ Câu trả lời gây điểm trừ:**
- Timeout tầng dưới (10s) lớn hơn tầng trên (2s).

**📖 Ôn lại:** [Phần 4 — Timeout](../01-giao-trinh/14-microservices-system-design.md#p4)

</details>

### Q15. 🟡 Khi nào được retry? Vì sao cần exponential backoff + jitter? Retry storm và retry budget là gì?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Chỉ retry khi lỗi **tạm thời** (timeout, 503, connection reset, 429 có `Retry-After`) **và** thao tác **idempotent** (GET/PUT/DELETE hoặc POST có idempotency key). Backoff tránh dội tải; **jitter** (full jitter: `random(0, min(cap, base·2^n))`) tránh mọi client retry cùng thời điểm. **Retry storm**: 3 tầng, mỗi tầng 4 lần thử → tầng cuối nhận `4³ = 64` lần tải. Biện pháp: retry **ở một tầng**, **retry budget** (~10% request), không retry khi CB OPEN, tôn trọng `Retry-After`.

**Giải thích chi tiết:**
```java
long expo = Math.min(cap.toMillis(), base.toMillis() * (1L << attempt));
long sleep = ThreadLocalRandom.current().nextLong(expo + 1);   // full jitter
```
```yaml
resilience4j.retry.instances.payment:
  max-attempts: 3
  wait-duration: 200ms
  enable-exponential-backoff: true
  exponential-backoff-multiplier: 2
  enable-randomized-wait: true
  randomized-wait-factor: 0.5
```
- Retry budget: token bucket nạp theo request thành công; retry chỉ khi còn token (gRPC retry throttling, Envoy retry budget).

**Câu hỏi nối tiếp:**
- *Retry lỗi 400/404?* — Không; lỗi nghiệp vụ/khách hàng không tự khỏi.

**⚠️ Câu trả lời gây điểm trừ:**
- Retry POST thanh toán không có idempotency key.

**📖 Ôn lại:** [Phần 4 — Retry với backoff + jitter](../01-giao-trinh/14-microservices-system-design.md#p4)

</details>

### Q16. 🟢 Circuit breaker hoạt động thế nào? Các trạng thái?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** **CLOSED**: cho qua, đếm kết quả trong sliding window (COUNT_BASED hoặc TIME_BASED); failure rate hoặc slow call rate ≥ ngưỡng (sau `minimumNumberOfCalls`) → **OPEN**: từ chối ngay (`CallNotPermittedException`) → **fail fast**, giải phóng tài nguyên, cho dependency hồi phục. Hết `waitDurationInOpenState` → **HALF_OPEN**: cho một số call thử; thành công → CLOSED, lỗi → OPEN. Resilience4j còn `DISABLED`, `FORCED_OPEN`, `METRICS_ONLY`.

**Giải thích chi tiết:**
- CB là **per instance**: 50 pod có 50 trạng thái độc lập — thường ổn.
- Theo dõi `resilience4j_circuitbreaker_state` (Micrometer), `/actuator/circuitbreakers`.
- Java 8/Boot 2 cũ: Netflix Hystrix (đã ngừng phát triển) → Resilience4j.

**Câu hỏi nối tiếp:**
- *CB có thay được timeout không?* — Không; CB cần lỗi tích lũy, timeout bảo vệ từng call.

**⚠️ Câu trả lời gây điểm trừ:**
- Nhầm CB với retry.

**📖 Ôn lại:** [Phần 4 — Circuit breaker](../01-giao-trinh/14-microservices-system-design.md#p4)

</details>

### Q17. 🔴 Những bẫy khi dùng Resilience4j: giá trị mặc định, thứ tự Retry/CircuitBreaker, phân loại exception, self-invocation.

<details><summary>Đáp án</summary>

**Trả lời ngắn:** (1) **Mặc định nguy hiểm**: `minimumNumberOfCalls=100`, `slidingWindowSize=100`, `waitDurationInOpenState=60s`, `slowCallDurationThreshold=60s` → service ít traffic CB không bao giờ mở; call 59s không bị tính chậm → **luôn tự cấu hình**. (2) **Thứ tự aspect mặc định** (ngoài → trong): `Retry(CircuitBreaker(RateLimiter(TimeLimiter(Bulkhead(fn)))))` → mỗi lần retry đi qua CB; cấu hình Retry **không** retry `CallNotPermittedException`. (3) **Lỗi nghiệp vụ** (thẻ bị từ chối, 4xx) phải vào `ignore-exceptions` — không mở circuit, không retry. (4) Gọi method `@CircuitBreaker` từ cùng class → **AOP bị bỏ qua**.

**Giải thích chi tiết:**
```yaml
resilience4j.circuitbreaker.instances.payment:
  sliding-window-size: 20
  minimum-number-of-calls: 10
  failure-rate-threshold: 50
  slow-call-duration-threshold: 1s
  slow-call-rate-threshold: 60
  wait-duration-in-open-state: 10s
  permitted-number-of-calls-in-half-open-state: 3
  record-exceptions: [ org.springframework.web.client.ResourceAccessException, java.util.concurrent.TimeoutException ]
  ignore-exceptions: [ com.shop.payment.CardDeclinedException ]
```
```java
@Retry(name = "payment")
@CircuitBreaker(name = "payment", fallbackMethod = "authorizeFallback")
@Bulkhead(name = "payment")
public PaymentResult authorize(PaymentRequest req) { ... }
private PaymentResult authorizeFallback(PaymentRequest req, CallNotPermittedException ex) {
  return PaymentResult.pending(req.orderId());     // suy giảm có kiểm soát, KHÔNG giả thành công
}
```
- Chữ ký fallback sai (khác kiểu trả về/tham số) → lỗi runtime "fallback method not found".
- CB bọc ngoài Retry → một "call" của CB gồm nhiều lần thử → CB phản ứng chậm hơn.
- Programmatic `Decorators.ofSupplier(...).withCircuitBreaker(cb).withRetry(retry)` — lớp gọi sau bọc ngoài.

**Câu hỏi nối tiếp:**
- *Test CB thế nào?* — WireMock trả 500, assert trạng thái qua `CircuitBreakerRegistry`.

**⚠️ Câu trả lời gây điểm trừ:**
- Dùng cấu hình mặc định trên production.

**📖 Ôn lại:** [Phần 4 — Resilience4j với Spring Boot](../01-giao-trinh/14-microservices-system-design.md#p4)

</details>

### Q18. 🟡 Bulkhead là gì? Semaphore vs ThreadPool bulkhead? Còn cần không khi dùng virtual threads?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Bulkhead cô lập tài nguyên như vách ngăn tàu: dependency A chậm không chiếm hết tài nguyên dùng cho B. **SemaphoreBulkhead**: giới hạn số call đồng thời (`maxConcurrentCalls`), chạy trên thread caller, nhẹ. **ThreadPoolBulkhead**: mỗi dependency một pool + queue riêng, trả `CompletionStage`. Với **virtual threads** càng cần semaphore bulkhead: thread không còn là giới hạn tự nhiên, nên connection pool và dependency phía sau dễ bị dội.

**Giải thích chi tiết:**
```yaml
resilience4j.bulkhead.instances.payment:
  max-concurrent-calls: 30
  max-wait-duration: 50ms     # vượt → từ chối nhanh
```
- Mức hạ tầng: connection pool riêng mỗi dependency, tách pool cho endpoint quan trọng, tách cluster cho tenant lớn.
- Với timeout 1s + bulkhead 30 cho Payment: tối đa 30 call đồng thời, phần dư bị từ chối ngay → endpoint khác vẫn khỏe.

**Câu hỏi nối tiếp:**
- *Chọn `maxConcurrentCalls` thế nào?* — Little's Law: RPS mục tiêu × p99 latency của dependency, cộng biên.

**⚠️ Câu trả lời gây điểm trừ:**
- Một thread pool chung cho mọi dependency.

**📖 Ôn lại:** [Phần 4 — Bulkhead](../01-giao-trinh/14-microservices-system-design.md#p4)

</details>

### Q19. 🟡 Fallback nên và không nên làm gì? Load shedding là gì?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Fallback hợp lệ: giá trị mặc định, **dữ liệu cache cũ (stale)**, chức năng suy giảm (ẩn khuyến nghị), chuyển sang xử lý async ("đơn đang được xử lý"). **Không**: gọi một dependency khác cũng mong manh, che giấu lỗi nghiệp vụ (fallback "thanh toán thành công" là thảm họa), trả dữ liệu giả không đánh dấu. **Load shedding** (SRE): khi quá tải, chủ động từ chối sớm (503/429) request ít quan trọng thay vì để mọi request cùng chậm; ưu tiên theo criticality.

**Giải thích chi tiết:**
- Rate limiter phía client (Resilience4j `RateLimiter`) tôn trọng quota đối tác: `limit-for-period: 100`, `limit-refresh-period: 1s`, `timeout-duration: 0`.
- Fallback phải có metric/log để không che lỗi nhiều ngày.

**Câu hỏi nối tiếp:**
- *Fallback cho Payment khi CB OPEN?* — `PENDING` + xử lý sau qua queue, báo user rõ ràng.

**⚠️ Câu trả lời gây điểm trừ:**
- Fallback trả `null` hoặc danh sách rỗng mà không ai biết.

**📖 Ôn lại:** [Phần 4 — Rate limiter, load shedding, fallback](../01-giao-trinh/14-microservices-system-design.md#p4)

</details>

---

<a id="nhom-e"></a>
## E. Dữ liệu phân tán: 2PC, Saga, CQRS, idempotency

### Q20. 🟢 Database per service đặt ra những bài toán gì và giải quyết thế nào?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Mỗi service **sở hữu** dữ liệu; service khác chỉ truy cập qua API/event. Bài toán: (1) **query xuyên service** → API composition (BFF ghép) hoặc **CQRS read model**; (2) **transaction xuyên service** → **Saga**; (3) **tham chiếu dữ liệu** → chỉ giữ ID (không FK xuyên DB), sao chép dữ liệu cần thiết qua event và chấp nhận độ trễ.

**Giải thích chi tiết:**
- Có thể là schema riêng trên cùng cluster (rẻ) hoặc DB riêng (cô lập, polyglot).
- Phát event tin cậy: **transactional outbox**.

**Câu hỏi nối tiếp:**
- *API composition có nhược điểm gì?* — Latency = call chậm nhất, availability nhân dồn, khó phân trang/sort xuyên service.

**⚠️ Câu trả lời gây điểm trừ:**
- "Join qua DB link giữa các service".

**📖 Ôn lại:** [Phần 5 — Database per service](../01-giao-trinh/14-microservices-system-design.md#p5)

</details>

### Q21. 🟡 Vì sao microservices hầu như không dùng 2PC?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** 2PC (XA/JTA) đảm bảo atomicity nhưng **blocking**: coordinator chết sau PREPARE → participant **in-doubt giữ lock** tới khi coordinator hồi phục; latency cao (nhiều round-trip + fsync); coordinator là điểm lỗi đơn; availability nhân dồn; Kafka, hầu hết NoSQL và REST API bên thứ ba **không hỗ trợ XA**. Thay bằng **Saga + outbox + idempotency**.

**Giải thích chi tiết:**
```
Coordinator ── PREPARE ──► A, B  (ghi log, giữ lock) ── YES
            ── ghi quyết định COMMIT ── COMMIT ──► A, B
```
- Bên trong DB phân tán (Spanner, CockroachDB), commit kiểu 2PC kết hợp đồng thuận vẫn dùng — đó là chuyện khác.

**Câu hỏi nối tiếp:**
- *`@Transactional` bao quanh HTTP call sang service khác thì rollback có hoàn tác bên kia?* — Không.

**⚠️ Câu trả lời gây điểm trừ:**
- "Dùng JTA/Atomikos giữa các microservice là chuẩn".

**📖 Ôn lại:** [Phần 5 — Two-Phase Commit](../01-giao-trinh/14-microservices-system-design.md#p5)

</details>

### Q22. 🔴 Saga orchestration vs choreography. Compensatable, pivot, retriable step là gì?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Saga = chuỗi local transaction T1…Tn; Tk lỗi → chạy compensation Ck-1…C1 theo thứ tự ngược. **Choreography**: service phản ứng với event của nhau — đơn giản cho 2–4 bước nhưng luồng ẩn, khó theo dõi. **Orchestration**: orchestrator gửi command/nhận reply, luồng tập trung trong **state machine** — hợp nhiều bước/nhánh, cần timeout/monitor; rủi ro orchestrator phình logic. Bước: **compensatable** (hoàn tác được), **pivot** (điểm không quay lại, ví dụ capture tiền), **retriable** (sau pivot, phải luôn thành công khi retry).

**Giải thích chi tiết:**
```java
@Transactional
public void on(PaymentAuthorized evt) {
  OrderSaga s = sagas.findById(evt.orderId()).orElseThrow();
  if (s.state != SagaState.STARTED) return;           // duplicate/muộn → idempotent
  s.state = SagaState.PAYMENT_AUTHORIZED;
  s.deadline = Instant.now().plusSeconds(30);
  outbox.add("inventory.commands", s.orderId, new ReserveStock(s.orderId));
}
@Scheduled(fixedDelay = 5000) @Transactional
public void timeouts() { sagas.findExpired(Instant.now()).forEach(this::handleTimeout); }
```
- Saga state có `@Version` chống hai reply xử lý đồng thời; mỗi bước = cập nhật state + outbox trong **một** transaction.
- Đặt bước khó hoàn tác nhất làm **pivot** ở cuối phần compensatable (vé máy bay không hoàn sau khi giữ khách sạn, thuê xe).
- Framework: **Temporal** (durable execution), Camunda/Zeebe, Axon, Eventuate Tram.

**Câu hỏi nối tiếp:**
- *Lỗi Shipping sau khi đã capture tiền?* — Retry vô hạn có backoff + alert; **không** void payment sau pivot.

**⚠️ Câu trả lời gây điểm trừ:**
- Compensation không idempotent (void 2 lần → hoàn tiền 2 lần); saga không có timeout → đơn kẹt `PENDING`.

**📖 Ôn lại:** [Phần 5 — Saga orchestration vs choreography](../01-giao-trinh/14-microservices-system-design.md#p5)

</details>

### Q23. 🔴 Saga thiếu tính chất nào của ACID? Countermeasure là gì?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Thiếu **Isolation** (chỉ còn ACD): service khác có thể thấy trạng thái trung gian (đơn `PENDING`, tiền đã giữ nhưng chưa chốt) → lost update, dirty read ở mức nghiệp vụ. Countermeasure (Chris Richardson): **semantic lock** (`*_PENDING` báo bản ghi đang trong saga), **commutative updates** (cộng/trừ thay vì set), **reread value** (kiểm tra lại trước khi ghi — optimistic), **by value** (chọn cơ chế chặt hơn cho giao dịch rủi ro cao).

**Giải thích chi tiết:**
- Ví dụ: user hủy đơn khi saga đang `PAYMENT_AUTHORIZED` → thao tác hủy phải kiểm tra semantic lock và phối hợp với saga (đánh dấu cancel-requested) thay vì xóa thẳng.
- UI hiển thị trạng thái trung gian ("đang xử lý").

**Câu hỏi nối tiếp:**
- *Nghiệp vụ nào cần strong consistency thì sao?* — Giữ gọn trong **một service/một DB transaction** (số dư ví, tồn kho).

**⚠️ Câu trả lời gây điểm trừ:**
- "Saga đảm bảo ACID như transaction".

**📖 Ôn lại:** [Phần 5 — Thiếu isolation trong saga](../01-giao-trinh/14-microservices-system-design.md#p5)

</details>

### Q24. 🟡 CQRS là gì? Xử lý "ghi xong đọc không thấy" thế nào?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Tách **mô hình ghi** (chuẩn hóa, giữ invariant) và **mô hình đọc** (phi chuẩn hóa, tối ưu cho màn hình/tìm kiếm), read model cập nhật từ event (outbox/CDC → Kafka → projection). Ưu: scale đọc/ghi độc lập, query xuyên service hiệu quả, nhiều view. Nhược: **eventual consistency**, thêm hạ tầng, phải rebuild projection. Read-your-writes: **trả dữ liệu từ write side ngay trong response**, hoặc client chờ version/ETag, hoặc đọc write side trong N giây sau khi ghi.

**Giải thích chi tiết:**
```
Command ─► Order Service (PostgreSQL) ─outbox/CDC─► Kafka ─► Order History View (Mongo/PG)
                                                           └► Search (Elasticsearch)
Query ─────────────────────────────────────────────────────► read models
```
- Projection phải idempotent và chịu out-of-order (version check).

**Câu hỏi nối tiếp:**
- *CQRS có bắt buộc event sourcing?* — Không; hay đi cùng nhưng độc lập.

**⚠️ Câu trả lời gây điểm trừ:**
- Áp CQRS cho CRUD đơn giản.

**📖 Ôn lại:** [Phần 5 — Outbox và CQRS](../01-giao-trinh/14-microservices-system-design.md#p5)

</details>

### Q25. 🔴 "Làm sao đảm bảo không trừ tiền khách hai lần?" — trả lời theo lớp.

<details><summary>Đáp án</summary>

**Trả lời ngắn:** (1) **Client sinh Idempotency-Key** cho mỗi thao tác, gửi lại cùng key khi retry; (2) API lưu key với **unique constraint** (`INSERT ... ON CONFLICT DO NOTHING`), cùng key khác body → 422, đang xử lý → 409, đã xong → replay response; (3) **truyền key xuống PSP**; (4) consumer event **dedup theo eventId**; (5) **reconciliation** cuối ngày với sao kê PSP — lưới an toàn cuối vì không cơ chế nào hoàn hảo.

**Giải thích chi tiết:**
```java
@PostMapping("/payments")
public ResponseEntity<PaymentResponse> pay(@RequestHeader("Idempotency-Key") String key,
                                           @RequestBody @Valid PaymentRequest req) {
  String hash = sha256(canonicalJson(req));
  Optional<IdempotencyRecord> existing = idem.tryInsertInProgress(key, hash);
  if (existing.isPresent()) {
    var r = existing.get();
    if (!r.requestHash().equals(hash)) return ResponseEntity.unprocessableEntity().build();
    if (r.status() == Status.IN_PROGRESS) return ResponseEntity.status(HttpStatus.CONFLICT).build();
    return ResponseEntity.status(r.responseCode()).body(r.responseBody(PaymentResponse.class));
  }
  PaymentResponse resp = paymentService.charge(req, key);
  idem.complete(key, 201, resp);
  return ResponseEntity.status(201).body(resp);
}
```
- Lưu kết quả 4xx để replay; lỗi 5xx tạm thời thì xóa record cho retry; TTL record (24h); `IN_PROGRESS` treo cần hết hạn/đối soát.
- PSP timeout → trạng thái **UNKNOWN**: không retry với key mới; query PSP theo key/đối soát.

**Câu hỏi nối tiếp:**
- *User bấm thanh toán ở 2 tab?* — Hai key khác nhau → cần ràng buộc nghiệp vụ: một payment active cho mỗi order (`unique(order_id) WHERE status IN (...)`).

**⚠️ Câu trả lời gây điểm trừ:**
- Kiểm tra bằng SELECT rồi INSERT; tạo key mới cho mỗi lần retry.

**📖 Ôn lại:** [Phần 5 — Idempotency xuyên service](../01-giao-trinh/14-microservices-system-design.md#p5)

</details>

---

<a id="nhom-f"></a>
## F. Consistency, CAP/PACELC & distributed ID

### Q26. 🟢 Kể các mô hình consistency từ mạnh đến yếu, kèm ví dụ.

<details><summary>Đáp án</summary>

**Trả lời ngắn:** **Linearizability** (đọc luôn thấy ghi mới nhất như thể một bản sao — etcd, ZooKeeper, Spanner) → **Sequential** (mọi node thấy cùng thứ tự) → **Causal** (quan hệ nhân quả đúng thứ tự) → **Read-your-writes** (user thấy chính dữ liệu mình vừa ghi) → **Monotonic reads** (không "đi lùi thời gian") → **Eventual** (ngừng ghi thì hội tụ — DNS, read model CQRS).

**Giải thích chi tiết:**
- Replication lag với read replica gây vi phạm read-your-writes và monotonic reads → đọc primary trong N giây sau khi ghi, gắn user vào một replica, hoặc theo dõi LSN/GTID tối thiểu.

**Câu hỏi nối tiếp:**
- *Giỏ hàng cần mức nào?* — Read-your-writes; số dư ví/tồn kho flash sale cần strong.

**⚠️ Câu trả lời gây điểm trừ:**
- Chỉ biết "strong" và "eventual".

**📖 Ôn lại:** [Phần 6 — Các mô hình consistency](../01-giao-trinh/14-microservices-system-design.md#p6)

</details>

### Q27. 🟡 Giải thích CAP và PACELC. Quorum `R + W > N` nghĩa là gì?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** **CAP**: khi có **network partition**, chọn **C** (linearizable, từ chối nếu không chắc dữ liệu mới nhất) hoặc **A** (mọi node sống đều trả lời, có thể cũ). Partition không phải lựa chọn → CAP thực chất là "khi partition: C hay A". **PACELC**: *if Partition → A or C; Else → Latency or Consistency* — kể cả không partition, replication đồng bộ tốn latency. **Quorum**: N replica, ghi chờ W ack, đọc R replica; `R + W > N` → tập đọc và ghi giao nhau → đọc thấy bản mới nhất (trừ trường hợp biên như sloppy quorum, ghi đồng thời).

**Giải thích chi tiết:**

| Hệ thống | P → | E → |
|---|---|---|
| Cassandra, DynamoDB (mặc định) | A | L |
| Spanner, CockroachDB | C | C |
| PostgreSQL primary + async replica | failover có thể mất dữ liệu | L |
| Kafka `acks=all`, `min.insync.replicas=2` | C | C/L tùy `acks` |

- CAP nói về định nghĩa rất hẹp; thực tế chọn khác nhau **theo từng thao tác**.

**Câu hỏi nối tiếp:**
- *N=3, W=1, R=1?* — Nhanh nhưng có thể đọc cũ; W=2, R=2 cho consistency mạnh hơn.

**⚠️ Câu trả lời gây điểm trừ:**
- "Chọn 2 trong 3, hệ thống của tôi là CA".

**📖 Ôn lại:** [Phần 6 — CAP & PACELC](../01-giao-trinh/14-microservices-system-design.md#p6)

</details>

### Q28. 🟡 So sánh auto-increment, UUIDv4, UUIDv7, Snowflake làm ID. Vì sao UUIDv4 làm PK hại hiệu năng?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Auto-increment: gọn nhưng điểm nghẽn, lộ số lượng, khó merge shard. **UUIDv4** random → insert rải khắp **B-tree** → page split, phân mảnh, cache miss → write amplification trên bảng lớn (nhất là InnoDB clustered index). **UUIDv7** (RFC 9562): 48 bit timestamp ms ở đầu → **gần như tăng dần**, thân thiện index, vẫn sinh phân tán; 128 bit, lộ thời điểm tạo. **Snowflake**: 64 bit (`BIGINT`), sắp theo thời gian, rất nhanh; cần **worker id duy nhất**, phụ thuộc đồng hồ.

**Giải thích chi tiết:**
- Snowflake: `1 bit dấu | 41 bit timestamp (~69 năm) | 10 bit machine (1024 node) | 12 bit sequence (4096 ID/ms/node)`.
- JDK chưa có factory UUIDv7 → thư viện (`java-uuid-generator`) hoặc tự viết; cùng ms phần random không đảm bảo tăng dần nếu không dùng counter.
- Không lộ ID tuần tự ra API công khai (IDOR, lộ doanh số).

**Câu hỏi nối tiếp:**
- *Benchmark thế nào?* — Insert 5 triệu dòng, đo thời gian và `pg_relation_size` của index.

**⚠️ Câu trả lời gây điểm trừ:**
- "UUID nào cũng như nhau".

**📖 Ôn lại:** [Phần 6 — Distributed ID](../01-giao-trinh/14-microservices-system-design.md#p6)

</details>

### Q29. 🔴 Viết Snowflake generator: xử lý đồng hồ lùi và cấp workerId cho 50 pod autoscale thế nào?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Đồng hồ lùi **ít** (vài ms): dùng tiếp timestamp cũ; lùi **nhiều**: **từ chối sinh ID** + báo động (hoặc chờ). Hết 4096 sequence trong ms → spin chờ ms kế. WorkerId: **lease** trong DB/Redis có TTL + renew định kỳ; không renew được thì **ngừng sinh ID**; pod mới chỉ lấy id có lease đã hết hạn → không hai pod cùng id đồng thời.

**Giải thích chi tiết:**
```java
public synchronized long nextId() {
  long ts = System.currentTimeMillis();
  if (ts < lastTs) {
    if (lastTs - ts > 5) throw new IllegalStateException("Clock moved backwards " + (lastTs - ts) + "ms");
    ts = lastTs;
  }
  if (ts == lastTs) {
    seq = (seq + 1) & MAX_SEQ;
    if (seq == 0) while ((ts = System.currentTimeMillis()) <= lastTs) Thread.onSpinWait();
  } else seq = 0;
  lastTs = ts;
  return ((ts - EPOCH) << (WORKER_BITS + SEQ_BITS)) | (workerId << SEQ_BITS) | seq;
}
```
```sql
UPDATE worker_lease SET owner = :pod, expires_at = now() + interval '30 seconds'
WHERE worker_id = (SELECT worker_id FROM worker_lease WHERE expires_at < now()
                   LIMIT 1 FOR UPDATE SKIP LOCKED)
RETURNING worker_id;
```
- Renew mỗi 10s. StatefulSet ordinal là cách đơn giản nếu dùng được.
- Pod bị pause (GC) quá lease → pod khác lấy id → cần kiểm tra lease còn hiệu lực trước khi sinh (giống fencing).

**Câu hỏi nối tiếp:**
- *Vì sao `synchronized` chấp nhận được?* — Phần critical rất ngắn; throughput hàng triệu ID/s mỗi node.

**⚠️ Câu trả lời gây điểm trừ:**
- Dùng random workerId hoặc hash hostname (va chạm).

**📖 Ôn lại:** [Phần 6 — Distributed ID & bài 6.3](../01-giao-trinh/14-microservices-system-design.md#p6)

</details>

---

<a id="nhom-g"></a>
## G. Observability

### Q30. 🟢 Ba trụ cột observability là gì? Four golden signals, RED, USE?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** **Logs** (chuyện gì đã xảy ra, chi tiết, tốn kém), **Metrics** (bao nhiêu, nhanh thế nào, xu hướng, rẻ), **Traces** (request đi qua đâu, chậm ở đâu, có sampling). **Four golden signals** (Google SRE): Latency, Traffic, Errors, Saturation. **RED** cho service: Rate, Errors, Duration. **USE** cho tài nguyên: Utilization, Saturation, Errors.

**Giải thích chi tiết:**

| | Công cụ phổ biến |
|---|---|
| Logs | ELK/EFK, Loki |
| Metrics | Micrometer → Prometheus → Grafana |
| Traces | OpenTelemetry → Jaeger/Tempo/Zipkin |

- **SLI/SLO/error budget**: SLI "tỉ lệ request < 300ms và không lỗi", SLO 99.9%/30 ngày, budget 0.1% → hết budget thì ưu tiên ổn định.

**Câu hỏi nối tiếp:**
- *Alert theo gì?* — Triệu chứng ảnh hưởng user (SLO burn rate), không theo nguyên nhân (CPU 80%) → tránh alert fatigue.

**⚠️ Câu trả lời gây điểm trừ:**
- "Observability = có log".

**📖 Ôn lại:** [Phần 7 — Ba trụ cột](../01-giao-trinh/14-microservices-system-design.md#p7)

</details>

### Q31. 🟡 Distributed tracing hoạt động thế nào trong Spring Boot 3? Head vs tail sampling? Vì sao trace bị mất qua `@Async`?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Trace = cây **span**; context lan qua header W3C **`traceparent`** (HTTP) và header Kafka/RabbitMQ. Spring Boot 3 dùng **Micrometer Observation + Micrometer Tracing** (thay Spring Cloud Sleuth của Boot 2) với bridge OpenTelemetry/Brave; `traceId`/`spanId` tự vào **MDC** → có trong log. **Head-based** sampling (quyết định ở đầu, ví dụ 10%) rẻ nhưng có thể bỏ sót trace lỗi; **tail-based** (OTel Collector quyết định sau khi trace xong, giữ mọi trace lỗi/chậm) tốn tài nguyên collector. Trace mất qua `@Async`/`CompletableFuture` với executor tự tạo vì context nằm trong ThreadLocal → dùng `ContextPropagatingTaskDecorator` / `ContextExecutorService.wrap`.

**Giải thích chi tiết:**
```yaml
management:
  tracing.sampling.probability: 0.1
  otlp.tracing.endpoint: http://otel-collector:4318/v1/traces
  metrics.distribution.percentiles-histogram.http.server.requests: true
logging.pattern.correlation: "[${spring.application.name:},%X{traceId:-},%X{spanId:-}] "
```
```java
Observation.createNotStarted("checkout", observations)
    .lowCardinalityKeyValue("payment.method", cart.paymentMethod().name())  // metric + span
    .highCardinalityKeyValue("cart.id", cart.id())                         // chỉ span
    .observe(() -> doCheckout(cart));
```
- Correlation id nghiệp vụ (`orderId`) trong MDC/header cho luồng async kéo dài hàng giờ (trace thường chỉ bao một request).
- Kafka: bật `spring.kafka.template/listener.observation-enabled=true`.

**Câu hỏi nối tiếp:**
- *Exemplars là gì?* — Liên kết điểm metric p99 tới trace cụ thể.

**⚠️ Câu trả lời gây điểm trừ:**
- Sampling 100% trên production lưu lượng lớn.

**📖 Ôn lại:** [Phần 7 — Correlation ID và distributed tracing](../01-giao-trinh/14-microservices-system-design.md#p7)

</details>

### Q32. 🔴 🎯 Tình huống: Prometheus OOM sau khi một team thêm metric mới. Nguyên nhân? Và bạn thiết kế alert SLO thế nào?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** **High cardinality**: tag `userId`, `orderId` hoặc URL chưa template hóa (`/orders/123`) → hàng triệu time series → Prometheus OOM. Giá trị cao-cardinality chỉ đưa vào **log/trace** (`highCardinalityKeyValue`), metric chỉ tag ít giá trị. Alert SLO theo **multi-window burn rate**: ví dụ burn rate 14.4 trong 1h (xác nhận bằng cửa sổ 5m) → page; burn rate 6 trong 6h → page; thấp hơn → ticket.

**Giải thích chi tiết:**
```promql
# Error rate 5 phút
sum(rate(http_server_requests_seconds_count{application="order-service",status=~"5.."}[5m]))
  / sum(rate(http_server_requests_seconds_count{application="order-service"}[5m]))
# p99
histogram_quantile(0.99, sum by (le, uri) (rate(http_server_requests_seconds_bucket{application="order-service"}[5m])))
```
- Burn rate = tốc độ tiêu error budget so với tốc độ "đều" trong cửa sổ SLO; 14.4 trong 1h ≈ tiêu 2% budget 30 ngày.
- Log: JSON có cấu trúc (Boot 3.4+ `logging.structured.format.console: ecs`), không log PII/secret, async appender.

**Câu hỏi nối tiếp:**
- *Vì sao không alert "error rate > 1%" tĩnh?* — Dễ ồn với traffic thấp, chậm với sự cố lớn; burn rate gắn với tác động tới SLO.

**⚠️ Câu trả lời gây điểm trừ:**
- "Tăng RAM cho Prometheus".

**📖 Ôn lại:** [Phần 7 — Góc nhìn Senior & bài 7.2](../01-giao-trinh/14-microservices-system-design.md#p7)

</details>

---

<a id="nhom-h"></a>
## H. Triển khai an toàn & tiến hóa API

### Q33. 🟢 So sánh rolling, blue-green, canary, shadow deploy. Feature flag giúp gì?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** **Rolling** (mặc định K8s): thay dần pod, ít tài nguyên, hai version chạy song song, rollback chậm. **Blue-green**: dựng môi trường mới, chuyển toàn bộ traffic một lần, rollback tức thì, tốn gấp đôi tài nguyên, DB chung phải tương thích cả hai. **Canary**: 1% → 5% → 25% → 100% theo metric, giới hạn blast radius, cần routing L7 + metric tốt (Argo Rollouts/Flagger). **Shadow**: nhân bản traffic thật, bỏ response — cẩn thận side effect. **Feature flag** tách **deployment khỏi release**: deploy code ẩn, bật dần, tắt ngay khi sự cố không cần redeploy.

**Giải thích chi tiết:**
- Canary không có phân tích metric tự động = rolling chậm.
- Flag là nợ kỹ thuật → dọn định kỳ; SDK cache local, không gọi remote trong vòng lặp nóng. Chuẩn API: OpenFeature.

**Câu hỏi nối tiếp:**
- *Canary tự rollback thế nào?* — AnalysisTemplate query Prometheus (error rate < 1%, p99 < 500ms), `failureLimit`.

**⚠️ Câu trả lời gây điểm trừ:**
- Không biết blue-green vẫn cần DB tương thích ngược.

**📖 Ôn lại:** [Phần 8 — Chiến lược deploy](../01-giao-trinh/14-microservices-system-design.md#p8)

</details>

### Q34. 🟡 Thay đổi API nào an toàn, thay đổi nào phá vỡ? Làm sao phát hiện breaking change trước khi deploy?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** **An toàn** (additive): thêm endpoint, thêm field optional ở response, thêm field optional có default ở request. **Phá vỡ**: xóa/đổi tên field, đổi kiểu, đổi ngữ nghĩa, thêm field bắt buộc ở request, đổi status code, **thêm enum value** (với client nghiêm ngặt), xóa query param. Client là **tolerant reader** (bỏ qua field lạ — Spring Boot mặc định `FAIL_ON_UNKNOWN_PROPERTIES=false`). Phát hiện sớm: **consumer-driven contract testing** (Pact, Spring Cloud Contract) trong CI.

**Giải thích chi tiết:**
- Versioning khi bắt buộc phá vỡ: URL `/v2/orders` hoặc media type; chạy song song, deprecate có thời hạn (header `Deprecation`, `Sunset`), theo dõi ai còn gọi v1.
- Enum mới: `READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE` + `@JsonEnumDefaultValue`.
- Rolling/canary → version N và N+1 chạy đồng thời → tương thích hai chiều trong giai đoạn chuyển tiếp.

**Câu hỏi nối tiếp:**
- *Đổi `amount` int → string?* — Phá vỡ; thêm field mới, expand-contract.

**⚠️ Câu trả lời gây điểm trừ:**
- Đổi tên field JSON trong một lần deploy.

**📖 Ôn lại:** [Phần 8 — Thay đổi tương thích ngược](../01-giao-trinh/14-microservices-system-design.md#p8)

</details>

### Q35. 🔴 🎯 Tình huống: đổi cột `customer_name` → `full_name` trên bảng 200 triệu dòng, rolling deploy, không downtime, rollback được.

<details><summary>Đáp án</summary>

**Trả lời ngắn:** **Expand/contract** (parallel change): (1) **expand** — thêm cột `full_name` nullable (migration riêng); (2) deploy N+1: **ghi cả hai cột**, đọc `full_name` fallback `customer_name`; (3) **backfill theo lô** `UPDATE ... WHERE full_name IS NULL`; (4) deploy N+2: chỉ dùng `full_name`; (5) **contract** — xóa cột cũ khi chắc không version nào dùng. Mỗi bước tương thích với version trước → rollback được.

**Giải thích chi tiết:**
- Rollback app dễ, rollback **dữ liệu** khó → không chạy migration phá vỡ cùng lúc deploy code.
- Bảng lớn: backfill theo batch nhỏ + nghỉ giữa batch để tránh lock dài/replication lag; index mới dùng `CREATE INDEX CONCURRENTLY` (PostgreSQL), MySQL dùng gh-ost/pt-online-schema-change.
- Flyway/Liquibase: mỗi bước một migration.

**Câu hỏi nối tiếp:**
- *Ghi hai cột có lệch khi version cũ còn chạy?* — Version cũ chỉ ghi `customer_name` → backfill/trigger tạm đồng bộ cho tới khi hết version cũ.

**⚠️ Câu trả lời gây điểm trừ:**
- `ALTER TABLE RENAME COLUMN` cùng lúc deploy code mới.

**📖 Ôn lại:** [Phần 8 — Expand/contract](../01-giao-trinh/14-microservices-system-design.md#p8)

</details>

---

<a id="nhom-i"></a>
## I. Bảo mật giữa các service

### Q36. 🟡 Zero trust trong microservices nghĩa là gì? mTLS và service mesh giúp gì?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Không tin mạng nội bộ — "ở trong VPC là an toàn" là sai; mọi call service-to-service cần **xác thực danh tính service** và **mã hóa**. **mTLS**: cả hai phía trình certificate → server biết chính xác service nào gọi; khó ở cấp phát và **xoay vòng** cert cho hàng trăm service. **Service mesh** (Istio, Linkerd): proxy tự động mTLS, cert ngắn hạn (SPIFFE ID `spiffe://cluster.local/ns/shop/sa/order-service`), tự xoay vòng, **authorization policy** (chỉ `order-service` được `POST /authorizations` của payment).

**Giải thích chi tiết:**
- Spring Boot 3.1+ **SSL bundles**: `server.ssl.client-auth=need`, client `RestClient` dùng bundle.
- Không tắt kiểm tra TLS (`trustAll`) "cho nhanh".

**Câu hỏi nối tiếp:**
- *mTLS có thay JWT được không?* — Không: mTLS xác thực **service**, JWT mang danh tính/quyền **user**.

**⚠️ Câu trả lời gây điểm trừ:**
- "Gateway đã xác thực rồi, service nội bộ không cần".

**📖 Ôn lại:** [Phần 9 — Zero trust & mTLS](../01-giao-trinh/14-microservices-system-design.md#p9)

</details>

### Q37. 🔴 Propagate JWT của user qua các service hay dùng token exchange? Confused deputy là gì?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Mỗi service **tự validate** JWT (chữ ký qua JWKS **cache**, `exp`, `iss`, `aud`) — defense in depth. Propagate token gốc giúp service sau biết "ai", nhưng token `aud` rộng dùng được ở mọi service → một service bị chiếm là lạm dụng được khắp nơi. Tốt hơn: **OAuth 2.0 Token Exchange (RFC 8693)** đổi sang token **audience hẹp** cho service đích. Danh tính service thuần (job batch): **client credentials**. **Confused deputy**: service A quyền rộng bị user lừa gọi B thay mặt mình → luôn mang danh tính user và **kiểm tra quyền ở B**.

**Giải thích chi tiết:**
```java
http.authorizeHttpRequests(a -> a
        .requestMatchers("/actuator/health/**").permitAll()
        .requestMatchers(HttpMethod.POST, "/orders").hasAuthority("SCOPE_orders:write")
        .anyRequest().authenticated())
    .oauth2ResourceServer(o -> o.jwt(Customizer.withDefaults()))
    .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
// spring.security.oauth2.resourceserver.jwt.issuer-uri: https://idp.example.com/realms/shop
```
- Không đưa PII/secret vào JWT (chỉ base64); token ngắn hạn (5–15 phút); không log header `Authorization`.
- Chặn `alg: none`, luôn kiểm tra `aud`/`iss`.

**Câu hỏi nối tiếp:**
- *Thu hồi JWT trước hạn?* — TTL ngắn + denylist `jti` (Redis, TTL = thời gian còn lại).

**⚠️ Câu trả lời gây điểm trừ:**
- Gọi JWKS endpoint mỗi request (IdP thành điểm nghẽn).

**📖 Ôn lại:** [Phần 9 — JWT và propagation danh tính](../01-giao-trinh/14-microservices-system-design.md#p9)

</details>

---

<a id="nhom-j"></a>
## J. Phương pháp system design & building blocks

### Q38. 🟢 Bạn tiếp cận một bài system design interview 45 phút thế nào? Lỗi hay gặp?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Khung (Alex Xu): **(1) Làm rõ yêu cầu** 3–10' — functional, non-functional (QPS, latency, availability, consistency, durability), quy mô, ràng buộc; **(2) Ước lượng** 3–5' — QPS trung bình/đỉnh, read:write, storage, bandwidth, cache; **(3) High-level** 10–15' — API, data model, sơ đồ, *xin đồng thuận*; **(4) Deep dive** 10–25' — 2–3 thành phần khó nhất, bottleneck, failure mode, trade-off; **(5) Tổng kết** — cải tiến, monitoring, mở rộng.

**Giải thích chi tiết:**
- Câu hỏi làm rõ mẫu: DAU? read/write? realtime? giữ dữ liệu bao lâu? mất vài bản ghi được không? một region hay toàn cầu? cần thứ tự/exactly-once?
- Lỗi: nhảy vào vẽ Kafka/Redis/K8s ngay; thiết kế "Google scale" cho 10k user; chỉ liệt kê công nghệ không nói trade-off; im lặng lâu (hãy nghĩ thành tiếng); bỏ qua failure mode ("Redis chết thì sao?").

**Câu hỏi nối tiếp:**
- *Thêm một building block thì phải nói gì?* — Nó chậm/chết thì hệ thống hành xử thế nào (fail-open/closed, degrade).

**⚠️ Câu trả lời gây điểm trừ:**
- Không viết yêu cầu ra, thiết kế lan man không có điểm neo.

**📖 Ôn lại:** [Phần 10 — Khung 4 bước](../01-giao-trinh/14-microservices-system-design.md#p10)

</details>

### Q39. 🟡 Ước lượng nhanh: mạng xã hội 100M DAU, mỗi user đăng 1 bài/ngày, đọc feed 20 lần/ngày. Rút ra quyết định gì?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** 1 ngày ≈ 10^5 s. Write ≈ 100M/10^5 ≈ **1.000/s** (đỉnh ×2–3 ≈ 3.000/s). Read ≈ 2 tỷ/10^5 ≈ **20.000/s** (đỉnh ≈ 60.000/s) → **read-heavy** → cache, read replica, **precompute feed**. Storage bài: 100M × 1KB = 100GB/ngày ≈ **36TB/năm** (chưa ×3 replication). Media: 10% có ảnh 500KB → 5TB/ngày → object storage + CDN.

**Giải thích chi tiết:**
- Con số cần thuộc: RAM ~100ns, SSD random ~16–100µs, RTT cùng DC ~0.5ms, liên lục địa ~150ms; 1 tháng ≈ 2.5×10^6 s, 1 năm ≈ 3×10^7 s.
- Năng lực thô: Java web instance 1k–10k RPS; PostgreSQL một node vài nghìn–vài chục nghìn query đơn giản/s; Redis ~100k+ ops/s; Kafka broker hàng trăm MB/s.
- Ước lượng cần **đúng bậc độ lớn** và **dẫn tới quyết định** ("20k read QPS > một DB → cache hit ≥ 90% + replicas").

**Câu hỏi nối tiếp:**
- *Cache cần bao nhiêu RAM?* — 80/20: cache ~20% dữ liệu nóng; 10M đối tượng × 1KB ≈ 10GB.

**⚠️ Câu trả lời gây điểm trừ:**
- Tính chi li tới chữ số cuối nhưng không rút ra quyết định nào.

**📖 Ôn lại:** [Phần 10 — Ước lượng nhanh](../01-giao-trinh/14-microservices-system-design.md#p10)

</details>

### Q40. 🟢 L4 vs L7 load balancer? Các thuật toán cân bằng tải?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** **L4** quyết định theo IP/port, cân bằng **connection**, rất nhanh (AWS NLB, kube-proxy, LVS). **L7** hiểu HTTP/gRPC: routing theo path/header/cookie, canary, retry, rate limit, auth, cân bằng **per request** (Nginx, HAProxy http, Envoy, ALB, Spring Cloud Gateway). Thuật toán: round robin, weighted RR, least connections, least response time, IP hash/consistent hash (sticky), random + **power of two choices** (chọn ngẫu nhiên 2, lấy cái ít tải hơn — gần tối ưu, ít trạng thái).

**Giải thích chi tiết:**
- Health check active/passive, connection draining khi gỡ node, bản thân LB phải HA.
- Sticky session là dấu hiệu **stateful** — ưu tiên stateless, session lưu Redis/JWT.

**Câu hỏi nối tiếp:**
- *Vì sao least connections hợp với request dài/không đều?* — Round robin đếm request, không đếm tải thực đang giữ.

**⚠️ Câu trả lời gây điểm trừ:**
- Không phân biệt connection-level và request-level balancing (bẫy gRPC — Q8).

**📖 Ôn lại:** [Phần 11 — Load balancer](../01-giao-trinh/14-microservices-system-design.md#p11)

</details>

### Q41. 🔴 Chọn shard key thế nào? Consistent hashing giải quyết gì? Resharding 16 → 64 shard không downtime?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Shard key: **cardinality cao, phân phối đều, khớp mẫu truy cập chính** (query phổ biến chạm 1 shard). Range → range query tốt nhưng hot spot; hash `% N` → đều nhưng **đổi N di chuyển gần hết dữ liệu**; **consistent hashing** → thêm/bớt node chỉ di chuyển ~`1/N` dữ liệu (cần **virtual node** để đều); directory → linh hoạt nhưng thêm service tra cứu. Resharding: dùng **logical shard** (ví dụ 1024 logical → 16 physical), di chuyển từng logical shard (copy + CDC catch-up + cutover).

**Giải thích chi tiết:**
```java
public N nodeFor(String key) {
  Map.Entry<Long, N> e = ring.ceilingEntry(hash(key));      // node đầu tiên theo chiều kim đồng hồ
  return (e != null ? e : ring.firstEntry()).getValue();     // quay vòng
}
```
- Ví dụ `orders` 5 tỷ dòng: shard theo `customerId` (lịch sử khách single-shard); nhúng shard id vào `orderId` (bit trong Snowflake) → tra theo orderId single-shard; báo cáo theo merchant → CQRS/OLAP (ClickHouse) qua CDC.
- Vấn đề: hot shard (celebrity), join/transaction xuyên shard, unique constraint toàn cục.
- Redis Cluster dùng **16384 slot cố định** — biến thể fixed partitions.

**Câu hỏi nối tiếp:**
- *% key di chuyển khi 4 → 5 node?* — Consistent hashing ~20%, modulo ~80%.

**⚠️ Câu trả lời gây điểm trừ:**
- Shard theo cột có ít giá trị (country) hoặc theo thời gian cho dữ liệu ghi nóng.

**📖 Ôn lại:** [Phần 11 — Sharding & consistent hashing](../01-giao-trinh/14-microservices-system-design.md#p11)

</details>

### Q42. 🟡 Single-leader, multi-leader và leaderless replication khác nhau thế nào? Split brain?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** **Single-leader** (PostgreSQL, MySQL): ghi leader, đọc follower; async nhanh nhưng failover có thể mất dữ liệu + replication lag; sync bền nhưng follower chết chặn ghi (thường semi-sync). **Multi-leader**: ghi nhiều region, phải giải **xung đột** (LWW mất dữ liệu, CRDT, logic hợp nhất). **Leaderless** (Dynamo, Cassandra): quorum `R + W > N`, read repair, hinted handoff, anti-entropy. **Split brain**: hai node cùng nghĩ mình là leader → ghi phân kỳ → chống bằng **fencing token**, consensus (Raft/Paxos).

**Giải thích chi tiết:**
- Failover: phát hiện (timeout — chọn quá ngắn gây failover giả), bầu leader, chuyển client.
- Search (Elasticsearch) **không** làm source of truth: đồng bộ từ DB qua CDC/event, rebuild bằng alias swap.

**Câu hỏi nối tiếp:**
- *Đọc từ replica ảnh hưởng UX ra sao?* — Vi phạm read-your-writes (Q26).

**⚠️ Câu trả lời gây điểm trừ:**
- "Replica async không bao giờ mất dữ liệu".

**📖 Ôn lại:** [Phần 11 — Replication](../01-giao-trinh/14-microservices-system-design.md#p11)

</details>

### Q43. 🟢 CDN hoạt động thế nào? Invalidate nội dung CDN ra sao?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** CDN phục vụ nội dung tĩnh (và API cache được) từ **edge gần user** → giảm latency và tải origin. **Pull CDN**: edge lấy từ origin khi miss theo `Cache-Control`; **Push CDN**: chủ động upload (nội dung lớn, ít đổi). Invalidate: ưu tiên **versioned URL** (`app.3f9a1c.js`) thay vì purge; purge API cho trường hợp khẩn.

**Giải thích chi tiết:**
- Asset có hash: `Cache-Control: public, max-age=31536000, immutable`.
- Nội dung động: `s-maxage`, `stale-while-revalidate`; nội dung riêng tư: **signed URL**.
- Cache theo user → `private`, không để CDN cache response có dữ liệu cá nhân.

**Câu hỏi nối tiếp:**
- *Các vấn đề cache phía server?* — Stampede, penetration, avalanche, hot key, inconsistency (xem Module 12).

**⚠️ Câu trả lời gây điểm trừ:**
- Cache response có cookie/session trên CDN công khai.

**📖 Ôn lại:** [Phần 11 — CDN & Caching](../01-giao-trinh/14-microservices-system-design.md#p11) · [Module 12 — Sự cố cache](../01-giao-trinh/12-redis-caching.md#phan-11)

</details>

---

<a id="nhom-k"></a>
## K. Đề thiết kế (system design prompts)

> Mỗi đáp án là **dàn ý** theo khung 5 bước — khi luyện, hãy tự nói đủ từng bước trước khi xem.

### Q44. 🟡 📐 Thiết kế URL shortener (100M URL mới/ngày, read:write 10:1, lưu 10 năm).

<details><summary>Đáp án</summary>

**Trả lời ngắn:** API `POST /api/v1/urls` + `GET /{code}` → 302. Mã = **Base62 của unique ID** (Snowflake) 7 ký tự. Lưu KV/sharded DB theo `code`, **Redis cache-aside** cho redirect, click event **async qua Kafka** cho analytics. Trade-off chính: 301 (giảm tải, mất analytics) vs 302 (đếm được click).

**Giải thích chi tiết:**
1. **Yêu cầu** — FR: rút gọn, redirect, tùy chọn custom alias/hết hạn/analytics. NFR: redirect latency thấp, HA, mã không trùng, khó đoán (tùy).
2. **Ước lượng** — write ≈ 100M/10^5 ≈ **1.160/s**; read ≈ **11.600/s**; 10 năm ≈ 365 tỷ bản ghi × ~100B ≈ **36,5TB**; `62^7 ≈ 3,5×10^12` > 365×10^9 → **7 ký tự**.
3. **High-level**
```
Client ─► CDN/LB ─► Shortener API (stateless) ─► Redis (code → longUrl) ─miss─► KV / sharded DB (PK code)
                          ├─► ID generator (Snowflake)
                          └─► Kafka (click events) ─► Analytics
```
4. **Deep dive**
   - Sinh mã: **hash + xử lý va chạm** (cùng longUrl → cùng mã, phải kiểm tra DB, Bloom filter giúp) vs **base62(ID)** (không trùng; ID tuần tự → đoán được → xáo bằng bijective mapping nếu cần).
   - Cache: phân phối power law → hit ratio cao; TTL + LRU.
   - Analytics không làm chậm redirect: `send()` không `join()`, `max.block.ms` nhỏ, best-effort.
   - Hết hạn: lazy delete khi đọc + job dọn.
5. **Trade-off & failure** — Redis chết → đọc DB (bulkhead); ID generator là phụ thuộc → nhiều worker id; lạm dụng → rate limit tạo URL, quét URL độc hại (Safe Browsing).

**Câu hỏi nối tiếp:**
- *Custom alias trùng?* — Unique constraint trên `code`, trả 409.
- *Cùng longUrl tạo nhiều lần?* — Base62(ID) tạo mã mới; muốn dedup cần bảng tra ngược `hash(longUrl) → code`.

**⚠️ Câu trả lời gây điểm trừ:**
- Không ước lượng độ dài mã; dùng 301 rồi nói "có analytics".

**📖 Ôn lại:** [Phần 12 — URL shortener](../01-giao-trinh/14-microservices-system-design.md#p12)

</details>

### Q45. 🟡 📐 Thiết kế distributed rate limiter cho API Gateway (100 req/phút/user, nhiều instance, thêm < 5ms).

<details><summary>Đáp án</summary>

**Trả lời ngắn:** **Token bucket** trong **Redis + Lua** (atomic giữa các instance), key `rate_limit:{clientId}` (hash tag cho Cluster), trả `429` + `Retry-After` + `X-RateLimit-Remaining`. Quyết định **fail-open** cho API thường, **fail-closed** cho endpoint đắt (OTP). Nhiều tầng: IP ở edge, user/API key ở gateway, tài nguyên đắt ở service.

**Giải thích chi tiết:**
1. **Yêu cầu** — giới hạn theo user/IP/API key, rule cấu hình được, chính xác toàn cục (không theo pod), latency thấp, HA.
2. **Ước lượng** — mỗi request 1 round-trip Redis (~0,5ms); 50k RPS → Redis Cluster vài shard; bộ nhớ O(1)/client với token bucket (Hash 2 field).
3. **High-level** — Client → Gateway filter → Redis (Lua) → allow/deny; rule store (DB/config) cache trong gateway.
4. **Deep dive**

| Thuật toán | Ưu | Nhược |
|---|---|---|
| Token bucket | Burst có kiểm soát, O(1) | 2 tham số |
| Leaky bucket | Đầu ra mượt | Không burst |
| Fixed window | Đơn giản | Burst 2× ở biên |
| Sliding log (ZSET) | Chính xác | O(limit) bộ nhớ |
| Sliding counter | Xấp xỉ tốt, O(1) | Xấp xỉ |

```lua
local tokens = tonumber(state[1]) or capacity
tokens = math.min(capacity, tokens + math.max(0, now - ts) * rate / 1000)
if tokens >= requested then tokens = tokens - requested; allowed = 1 end
```
   - Thời gian: dùng `redis.call('TIME')` để có một nguồn đồng hồ duy nhất.
   - Giảm latency: limiter local (Bucket4j/Resilience4j) với hạn mức chia cho số instance + đồng bộ định kỳ, chấp nhận sai số.
5. **Trade-off & failure** — Redis chết: fail-open + limiter local dự phòng; hot key (một client cực lớn) → limiter local trước; chính xác vs latency.

**Câu hỏi nối tiếp:**
- *Chứng minh fixed window bị burst?* — 100 request ở giây 59 + 100 ở giây 61 = 200 trong 2 giây.

**⚠️ Câu trả lời gây điểm trừ:**
- `GET` rồi `INCR` không atomic; đếm trong bộ nhớ từng pod.

**📖 Ôn lại:** [Phần 12 — Rate limiter](../01-giao-trinh/14-microservices-system-design.md#p12) · [Module 12 — Rate limiting](../01-giao-trinh/12-redis-caching.md#phan-13)

</details>

### Q46. 🟡 📐 Thiết kế notification system (10M push + 1M SMS + 5M email/ngày, soft real-time).

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Notification API (validate, auth, rate limit, **idempotency key**) → lấy preference/template → đẩy vào **queue tách theo channel và priority** → worker từng channel gọi provider (APNs, FCM, SMS, SES) với retry backoff + DLQ → log trạng thái. Bulkhead theo channel, rate limit theo user và theo provider, dedup "hiếm khi trùng" (không exactly-once tới thiết bị).

**Giải thích chi tiết:**
1. **Yêu cầu** — push/SMS/email; tôn trọng opt-out/preference/quiet hours; retry khi provider lỗi; theo dõi trạng thái; không spam.
2. **Ước lượng** — 16M/ngày ≈ 185/s trung bình; marketing campaign tạo peak ×10–100 → queue hấp thụ; SMS là đắt nhất → quota.
3. **High-level**
```
Services ─► Notification API ─► Preference DB · Template service · Notification log (status)
                 └─► queues: push-ios | push-android | sms | email  (× priority: OTP vs marketing)
                         └─► workers ─► APNs / FCM / SMS provider / SES ─► retry ─► DLQ ─► tracking
```
4. **Deep dive**
   - Tách queue theo channel: SMS chậm không ảnh hưởng email/push; OTP tách khỏi marketing (worker riêng).
   - Dedup: key `orderId + type`, bảng log unique constraint, worker kiểm tra trước khi gửi.
   - Retry: lỗi tạm thời → backoff; token thiết bị không hợp lệ (unregistered) → xóa token, **không** retry.
   - Rate limit per user (N marketing/ngày) và per provider (Resilience4j RateLimiter theo quota).
5. **Trade-off & failure** — provider chết → failover provider dự phòng; observability: số theo trạng thái (queued/sent/delivered/failed), lag queue, lỗi theo provider; template/i18n, opt-out bắt buộc theo luật.

**Câu hỏi nối tiếp:**
- *Kafka hay RabbitMQ cho queue?* — RabbitMQ hợp task queue có priority/TTL/DLX; Kafka nếu cần replay/throughput rất cao.

**⚠️ Câu trả lời gây điểm trừ:**
- Một queue chung cho mọi channel và mọi priority.

**📖 Ôn lại:** [Phần 12 — Notification system](../01-giao-trinh/14-microservices-system-design.md#p12)

</details>

### Q47. 🔴 📐 Thiết kế luồng order/payment e-commerce: không oversell, không trừ tiền hai lần, chịu lỗi PSP.

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Order service **reserve stock** (update có điều kiện, TTL 15') → Order `AWAITING_PAYMENT` + outbox → user thanh toán qua PSP → **webhook** (verify HMAC, dedup theo PSP event id) → Payment `CAPTURED` + outbox → Order commit reservation → `PAID` → Shipping/Notification. Chống trừ hai lần bằng idempotency key nhiều lớp + **payment state machine** + **reconciliation**.

**Giải thích chi tiết:**
1. **Yêu cầu** — nhiều sản phẩm/đơn, PSP bên ngoài, không oversell, không double charge, chịu lỗi từng thành phần, chịu flash sale.
2. **Ước lượng** — ví dụ 1M đơn/ngày ≈ 12/s, đỉnh flash sale ×100–1000 → tồn kho là điểm nóng.
3. **High-level**
```
User ─► Gateway ─► Order ─(sync reserve)─► Inventory
                     └─ outbox ─► Kafka ─► Payment ◄─ webhook ─ PSP
                                             └─ outbox ─► Kafka ─► Order (PAID) ─► Shipping, Notification
```
4. **Deep dive**
   - Tồn kho: `UPDATE inventory SET available = available - :qty, reserved = reserved + :qty WHERE sku = :sku AND available >= :qty` (0 dòng → hết hàng). Flash sale: trừ trước trên Redis Lua + ghi DB async + đối soát; waiting room ở edge.
   - Reservation TTL: hết hạn → release, `EXPIRED`; race "thanh toán đúng lúc hết hạn" → quy tắc nghiệp vụ (cố cấp hàng hoặc hoàn tiền tự động).
   - Payment state machine `CREATED → AUTHORIZED → CAPTURED → REFUNDED | FAILED | VOIDED`, chuyển bằng update có điều kiện `WHERE status = ...`.
   - **PSP timeout = UNKNOWN**: không retry với key mới; query PSP theo idempotency key; job đối soát payment `PENDING` lâu.
   - Webhook đến **trước** response đồng bộ: `paymentId` tạo trước khi gọi PSP; webhook `UPDATE ... SET status='CAPTURED' WHERE id=? AND status IN ('CREATED','AUTHORIZED')`; response đến sau thấy đã CAPTURED thì bỏ qua.
   - Ledger double-entry, append-only, tiền dùng `BigDecimal`/số nguyên đơn vị nhỏ nhất.
5. **Trade-off & failure** — không gọi PSP **trong** DB transaction giữ lock tồn kho; không tin redirect phía trình duyệt; reconciliation hằng ngày với settlement PSP là lưới an toàn cuối.

**Câu hỏi nối tiếp:**
- *Webhook đến 2 lần?* — Dedup theo PSP event id (unique) + state machine.
- *User bấm thanh toán ở 2 tab?* — Một payment active mỗi order (unique có điều kiện).

**⚠️ Câu trả lời gây điểm trừ:**
- `SELECT available` rồi `UPDATE` không điều kiện; tạo idempotency key mới mỗi lần retry.

**📖 Ôn lại:** [Phần 12 — Luồng order/payment](../01-giao-trinh/14-microservices-system-design.md#p12) · [Phần 5 — Idempotency xuyên service](../01-giao-trinh/14-microservices-system-design.md#p5)

</details>

### Q48. 🔴 📐 Thiết kế hệ thống flash sale: 1 triệu user trong 1 phút tranh 10.000 sản phẩm.

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Mục tiêu là **chặn phần lớn traffic sớm và rẻ**: CDN cho trang tĩnh, **waiting room/virtual queue** ở edge làm phẳng peak, **tồn kho trên Redis** trừ bằng Lua atomic (mỗi user tối đa 1/SKU), thành công → event vào queue → tạo đơn **bất đồng bộ, idempotent** trong DB (unique `(user_id, sku)`), job **đối soát** Redis vs DB. Mua hàng fail-closed khi Redis lỗi — không oversell.

**Giải thích chi tiết:**
1. **Yêu cầu** — FR: xem sản phẩm, mua, giới hạn 1/user/SKU, thanh toán trong thời hạn. NFR: không oversell, p99 nút mua thấp, chịu peak, chống bot.
2. **Ước lượng** — 1M user/60s ≈ **16.7k req/s trung bình**, peak vài giây đầu ×5–10 → **~100k+ req/s**; chỉ 10.000 request thành công → >99% bị từ chối → phải từ chối **rẻ** (không chạm DB).
3. **High-level**
```
User ─► CDN (trang tĩnh, countdown) ─► Waiting room / token ─► Gateway (rate limit user/IP, chống bot)
     ─► Flash API ─► Redis: Lua { kiểm tra còn hàng, SISMEMBER bought:{sku}, DECR, SADD } ─► OK/SOLD_OUT
                     └─ OK ─► Stream/Kafka "orders:flash" ─► Order workers ─► PostgreSQL (unique user+sku)
```
4. **Deep dive**
   - Hot key tồn kho: một key/SKU trên một shard; nếu một SKU quá nóng → chia tồn kho thành N bucket key, L1 cache cho trang chi tiết (Caffeine vài giây).
   - Idempotency `POST /buy` với `Idempotency-Key`; consumer idempotent; hold đơn có TTL thanh toán, hết hạn trả tồn kho về Redis.
   - Warm-up tồn kho và Bloom filter SKU trước giờ mở bán; TTL có jitter.
5. **Trade-off & failure** — Redis là nguồn tồn kho tạm → failover có thể mất ghi → đối soát + ràng buộc DB; Redis lỗi → nút mua fail-closed, trang xem vẫn phục vụ từ L1/DB; fairness của waiting room vs đơn giản.

**Câu hỏi nối tiếp:**
- *Vì sao không trừ kho trực tiếp trong DB?* — 100k req/s cùng vài row → lock contention; DB chỉ nhận ~10k ghi thành công đã được lọc.

**⚠️ Câu trả lời gây điểm trừ:**
- "Scale DB lên thật to"; không có cơ chế chặn traffic sớm.

**📖 Ôn lại:** [Dự án mini — Phần B](../01-giao-trinh/14-microservices-system-design.md#du-an-mini) · [Module 12 — Dự án FlashSale](../01-giao-trinh/12-redis-caching.md#du-an-mini) · [Phần 12 — order/payment](../01-giao-trinh/14-microservices-system-design.md#p12)

</details>

### Q49. 🟡 📐 Thiết kế ứng dụng chat 1-1 và nhóm (50M DAU, 40 tin/user/ngày, lưu 5 năm).

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Client giữ **WebSocket** tới gateway; **session/presence registry** biết user đang ở gateway nào; tin nhắn ghi vào **wide-column store** (Cassandra/ScyllaDB) partition theo `conversationId`, sắp theo message id (Snowflake); fan-out qua message bus tới gateway của người nhận; offline → push notification. Thứ tự theo conversation, idempotency theo `clientMessageId`.

**Giải thích chi tiết:**
1. **Yêu cầu** — 1-1, nhóm, online/offline, đã gửi/đã nhận/đã đọc, lịch sử, đa thiết bị.
2. **Ước lượng** — 50M × 40 = 2 tỷ tin/ngày → **~20k write/s**, đỉnh ~50–60k/s; 200GB/ngày → 5 năm ≈ 365TB, ×3 ≈ **1,1PB**; 20% DAU online → **10M WebSocket**; gateway ~50–100k kết nối/node → **100–200 node**.
3. **High-level**
```
Client ─WS─► Chat gateway (stateful) ─► Chat service ─► Message store (Cassandra, PK conversationId)
                 ▲                          └─► bus (Kafka/Redis pub-sub theo gateway) ─► gateway người nhận
                 └── Presence/session registry (Redis: userId → gatewayId, TTL heartbeat)
                                                └─► offline ─► Notification service (push)
```
4. **Deep dive**
   - ID: Snowflake → sắp theo thời gian trong conversation; client gửi `clientMessageId` để dedup khi retry.
   - Nhóm lớn: fan-out on read (đọc từ store) thay vì ghi vào hộp thư từng thành viên.
   - Read receipt: cập nhật con trỏ "đã đọc tới message X" mỗi user/conversation, không ghi từng tin.
   - Gateway chết: client reconnect với backoff + jitter, đồng bộ lại từ message id cuối.
5. **Trade-off & failure** — consistency: thứ tự trong conversation đủ, không cần toàn cục; WebSocket stateful → LB L7 hỗ trợ upgrade, rolling deploy cần drain kết nối dần; E2E encryption làm khó tìm kiếm phía server.

**Câu hỏi nối tiếp:**
- *Vì sao wide-column thay vì PostgreSQL?* — Write nặng, truy cập theo (conversation, thời gian), scale ngang dễ.

**⚠️ Câu trả lời gây điểm trừ:**
- Polling HTTP mỗi giây cho 10M client; không có presence registry.

**📖 Ôn lại:** [Phần 10 — Bài 10.1 (ước lượng chat)](../01-giao-trinh/14-microservices-system-design.md#p10) · [Phần 11 — Building blocks](../01-giao-trinh/14-microservices-system-design.md#p11)

</details>

### Q50. 🔴 📐 Thiết kế hệ thống đặt vé xem phim (chọn ghế, giữ ghế khi thanh toán, phim bom tấn mở bán).

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Mô hình `seat_hold(show_id, seat_id)` với **unique constraint** (hoặc trạng thái ghế + optimistic lock) để **hai người không giữ cùng ghế**; giữ ghế có **TTL** (ví dụ 10 phút) chờ thanh toán; thanh toán thành công → `BOOKED`; hết hạn → nhả. Mở bán bom tấn → **waiting room**, sơ đồ ghế đọc từ cache có độ trễ chấp nhận được, thao tác giữ ghế luôn đi DB/nguồn sự thật.

**Giải thích chi tiết:**
1. **Yêu cầu (câu hỏi làm rõ)** — Giữ ghế bao lâu? Chọn nhiều ghế liền nhau? Hai người chọn cùng ghế? Thanh toán thất bại? Peak khi mở bán? Hủy/hoàn vé?
2. **Ước lượng** — ví dụ 1.000 rạp × 10 phòng × 5 suất × 200 ghế = 10M ghế/ngày; peak mở bán: 500k user trong vài phút → waiting room.
3. **High-level**
```
User ─► CDN (lịch chiếu) ─► Waiting room ─► Booking API
          ├─► Seat map (cache Redis, cập nhật theo event, chấp nhận trễ vài giây)
          └─► Hold seats (DB transaction: INSERT seat_hold ... unique(show_id, seat_id), expires_at)
                 └─► Payment (idempotency key) ─► webhook ─► BOOKED + outbox ─► vé điện tử/notification
Job/delayed message: release hold hết hạn
```
4. **Deep dive**
   - Giữ nhiều ghế atomic: một transaction insert tất cả hold; vi phạm unique → rollback cả nhóm, báo ghế đã bị giữ.
   - Lựa chọn: unique constraint (đơn giản, đúng) vs `SELECT ... FOR UPDATE` (lock bi quan) vs Redis `SET NX PX` cho từng ghế (nhanh nhưng cần đối soát với DB).
   - Race "thanh toán xong đúng lúc hold hết hạn": chuyển `BOOKED` bằng update có điều kiện `WHERE status='HELD' AND hold_id=?`; thất bại → hoàn tiền tự động.
5. **Trade-off & failure** — sơ đồ ghế stale → user chọn ghế đã bị giữ → lỗi rõ ràng, cập nhật lại sơ đồ; fairness waiting room; bot → CAPTCHA/rate limit.

**Câu hỏi nối tiếp:**
- *Vì sao không giữ ghế chỉ trong Redis?* — Được cho tốc độ, nhưng Redis failover có thể mất hold → cần DB là nguồn sự thật hoặc đối soát.

**⚠️ Câu trả lời gây điểm trừ:**
- Kiểm tra ghế trống bằng SELECT rồi INSERT không có ràng buộc (double booking).

**📖 Ôn lại:** [Phần 10 — Bài 10.2 (đặt vé xem phim)](../01-giao-trinh/14-microservices-system-design.md#p10) · [Phần 12 — order/payment](../01-giao-trinh/14-microservices-system-design.md#p12)

</details>

# Case 08 — `Connection is not available, request timed out after 30000ms`: cạn HikariCP vì transaction dài, leak và pool sizing sai

> **Chủ đề:** HikariCP, transaction giữ connection khi gọi HTTP, `idle in transaction`, leak detection, pool sizing, `max_connections`, PgBouncer
> **Module liên quan:** [M09 §2 — Connection pooling với HikariCP](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#2-hikaricp) · [M09 §11 — Transaction dài & gọi hệ thống ngoài](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#11-transactions) · [M09 §6 — OSIV](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#6-fetching) · [M11 §10 — Connection limits](../01-giao-trinh/11-database-sql.md#phan-10) · [M14 §4 — Timeout, circuit breaker, bulkhead](../01-giao-trinh/14-microservices-system-design.md#p4) · [M14 §5 — Idempotency, outbox](../01-giao-trinh/14-microservices-system-design.md#p5) · [M16 §7 — Kubernetes (HPA)](../01-giao-trinh/16-devops-build-cloud-security.md#p7)
> **Độ khó:** ⭐⭐⭐ (Senior)
> **Thời gian tự giải gợi ý:** 40 phút

---

## 1. Bối cảnh hệ thống

`checkout-service` của một chuỗi bán lẻ online: đặt hàng và thanh toán qua cổng thanh toán bên thứ ba.

| Thành phần | Chi tiết |
|---|---|
| Stack | Java 21, Spring Boot 3.2, Spring Data JPA, PostgreSQL 15 (8 vCPU, `max_connections = 200`), HikariCP mặc định (`maximum-pool-size=10`, `connection-timeout=30000`) |
| Triển khai | K8s, HPA 4 → 8 pod theo CPU; Tomcat 200 thread/pod |
| Dependency | Cổng thanh toán PayGW (HTTPS), p99 bình thường 350 ms, client có read timeout 10 s |
| Tải | Ngày thường 120 RPS checkout toàn cụm; Black Friday dự kiến 600 RPS |
| DB dùng chung | Ngoài checkout còn `order-query-service` (6 pod × 10) và batch đối soát (1 × 5) cùng kết nối vào DB này |

## 2. Triệu chứng

**Black Friday, 20:02** — PayGW bắt đầu chậm (p99 350 ms → 6–8 s). **20:04** — lỗi hàng loạt ở checkout:
```
2026-11-27T20:04:31.552+07:00 ERROR [http-nio-8080-exec-77] o.h.engine.jdbc.spi.SqlExceptionHelper :
  HikariPool-1 - Connection is not available, request timed out after 30000ms (total=10, active=10, idle=0, waiting=184)
org.springframework.orm.jpa.JpaSystemException: Unable to acquire JDBC Connection
Caused by: java.sql.SQLTransientConnectionException: HikariPool-1 - Connection is not available, request timed out after 30000ms ...
```
- Không chỉ `POST /checkout` — cả `GET /cart`, `GET /orders/{id}` (không gọi PayGW) cũng lỗi 500 sau đúng 30 giây.
- Grafana (`checkout-service`):
```
hikaricp_connections_active         10 / max 10   (liên tục)
hikaricp_connections_pending        0  →  184
hikaricp_connections_acquire  p99   0.4 ms → 30 s
hikaricp_connections_usage    p99   45 ms  → 8.1 s      ← mỗi connection bị giữ 8 giây
tomcat_threads_busy_threads         35 → 200
process_cpu_usage                   0.55 → 0.08
```

**20:10 — on-call "chữa cháy":** tăng `maximum-pool-size` lên 50 và deploy. HPA đồng thời scale lên 8 pod. **20:16:**
```
org.postgresql.util.PSQLException: FATAL: sorry, too many clients already
```
- Pod mới khởi động thất bại (Hikari không tạo được connection ban đầu) → CrashLoopBackOff.
- `order-query-service` và batch đối soát **cũng** không kết nối được DB → sự cố lan sang hệ thống khác.
- DB: 197 connection, RAM tăng 9 GB, CPU 95% (lock contention + context switch).

**Một tín hiệu cũ bị bỏ qua:** từ 2 tuần trước, `hikaricp_connections_active` lúc 3 giờ sáng (gần như không có traffic) không về 0 mà về 2, 3, 4… tăng dần sau mỗi đêm cho đến khi pod được deploy lại.

## 3. Câu hỏi đặt ra

1. Vì sao PayGW chậm lại làm cạn **connection DB**, và làm chết cả endpoint không liên quan?
2. Vì sao tăng pool từ 10 lên 50 làm sự cố tệ hơn?
3. Đường `active` không về 0 lúc 3 giờ sáng nói lên điều gì?
4. Thiết kế lại thế nào? Pool bao nhiêu là đúng?

> ✋ **Dừng lại và tự giải trước.** Dùng định luật Little: 600 RPS checkout toàn cụm chia cho 4 pod, mỗi checkout giữ connection 8 s → mỗi pod cần bao nhiêu connection? So với pool 10? Rồi tính tổng connection vào DB sau khi pool = 50 và HPA lên 8 pod.

## 4. Điều tra từng bước

### Bước 1 — Connection đang bị giữ để làm gì? Hỏi chính DB
```sql
SELECT pid, application_name, state,
       now() - xact_start   AS xact_age,
       now() - state_change AS in_state_for,
       left(query, 70)      AS last_query
FROM pg_stat_activity
WHERE datname = 'shop' AND usename = 'checkout'
ORDER BY xact_start;
```
```
  pid  | application_name | state               | xact_age | in_state_for | last_query
-------+------------------+---------------------+----------+--------------+-----------------------------------------------------------------------
 48211 | checkout         | idle in transaction | 00:00:07 | 00:00:07     | insert into payment_attempt (amount,created_at,order_id,status,id) val
 48215 | checkout         | idle in transaction | 00:00:06 | 00:00:06     | insert into payment_attempt (amount,created_at,order_id,status,id) val
 48230 | checkout         | idle in transaction | 00:00:05 | 00:00:05     | insert into payment_attempt ...
 ... (36/40 dòng giống hệt)
 47102 | checkout         | idle in transaction | 2 days 03:11:45 | 2 days 03:11:45 | select id, order_id, amount from settlement_item where status = 'PEN
```
- **`idle in transaction`**: DB không làm gì — transaction đã mở, đã chạy câu `INSERT`, và đang **chờ ứng dụng**. Ứng dụng đang làm gì trong 5–7 giây đó? (bước 2)
- Dòng cuối: một transaction mở **2 ngày** — dấu hiệu leak (bước 3).

### Bước 2 — Thread dump: ứng dụng làm gì khi đang giữ connection?
```
"http-nio-8080-exec-77" #143 [151] daemon prio=5 ... runnable
   java.lang.Thread.State: RUNNABLE
	at sun.nio.ch.SocketDispatcher.read0(java.base@21.0.4/Native Method)
	...
	at org.apache.hc.client5.http.impl.classic.InternalHttpClient.doExecute(InternalHttpClient.java:...)
	at org.springframework.web.client.DefaultRestClient$DefaultRequestBodyUriSpec.exchangeInternal(DefaultRestClient.java:...)
	at com.acme.checkout.payment.PayGwClient.charge(PayGwClient.java:61)
	at com.acme.checkout.CheckoutService.checkout(CheckoutService.java:48)
	at com.acme.checkout.CheckoutService$$SpringCGLIB$$0.checkout(<generated>)        ← đi qua proxy: transaction đang mở
	at com.acme.checkout.CheckoutController.checkout(CheckoutController.java:33)
```
Và 150 thread khác ở trạng thái chờ connection:
```
"http-nio-8080-exec-12" #78 [86] daemon prio=5 ... waiting on condition
   java.lang.Thread.State: TIMED_WAITING (parking)
	at jdk.internal.misc.Unsafe.park(java.base@21.0.4/Native Method)
	- parking to wait for  <0x00000000c5a1b2c8> (a java.util.concurrent.SynchronousQueue$Transferer)
	...
	at com.zaxxer.hikari.util.ConcurrentBag.borrow(ConcurrentBag.java:...)
	at com.zaxxer.hikari.pool.HikariPool.getConnection(HikariPool.java:...)
	...
	at com.acme.cart.CartController.get(CartController.java:27)                       ← endpoint không liên quan PayGW
```
Code:
```java
@Service
public class CheckoutService {
    @Transactional
    public CheckoutResult checkout(CheckoutCommand cmd) {
        Order order = orderRepo.findByIdForUpdate(cmd.orderId());           // SELECT ... FOR UPDATE: lock dòng order
        PaymentAttempt attempt = attemptRepo.save(PaymentAttempt.start(order)); // INSERT
        PayGwResponse res = payGw.charge(order, attempt.getId());            // ❌ HTTP 0,35–8 s, vẫn giữ connection + row lock
        attempt.complete(res);
        order.markPaid(res);
        return CheckoutResult.of(order);
    }
}
```

### Bước 3 — Leak: ai mượn connection mà không trả?
Bật leak detection trên 1 pod (đổi cấu hình qua ConfigMap, không cần build):
```yaml
spring.datasource.hikari.leak-detection-threshold: 60000   # > transaction dài nhất hợp lệ
```
```
WARN  com.zaxxer.hikari.pool.ProxyLeakTask : Connection leak detection triggered for org.postgresql.jdbc.PgConnection@5f1a7c3
      on thread scheduling-1, stack trace follows
java.lang.Exception: Apparent connection leak detected
	at com.zaxxer.hikari.HikariDataSource.getConnection(HikariDataSource.java:127)
	at com.acme.checkout.settlement.SettlementExporter.exportPending(SettlementExporter.java:52)
	at com.acme.checkout.settlement.SettlementJob.run(SettlementJob.java:19)
```
```java
public void exportPending() throws SQLException, IOException {
    Connection c = dataSource.getConnection();                       // ❌ không try-with-resources
    c.setAutoCommit(false);
    PreparedStatement ps = c.prepareStatement("select id, order_id, amount from settlement_item where status = 'PENDING' ...");
    ResultSet rs = ps.executeQuery();
    sftp.upload(toCsv(rs));                                          // ném IOException khi SFTP đối tác lỗi (≈ 1 lần/đêm)
    c.commit();
    c.close();                                                       // không bao giờ tới đây khi có exception
}
```
Mỗi lần SFTP lỗi, một connection bị giữ **vĩnh viễn** ở trạng thái `idle in transaction` (và giữ snapshot MVCC → cản autovacuum). Pool 10 hiệu dụng chỉ còn 6–8 trước Black Friday.

### Bước 4 — Định lượng
- Nhu cầu connection cho checkout (Little): 600 RPS / 4 pod = 150 RPS/pod × 8 s = **1.200 connection/pod** khi PayGW chậm. Pool 10 hay 50 đều vô nghĩa.
- Khi PayGW bình thường: 150 × 0,4 s = 60 connection/pod — **vẫn vượt** 10. Hệ thống đã sống sót ngày thường chỉ vì tải thấp (30 RPS/pod × 0,4 s = 12, xấp xỉ giới hạn).
- Tổng connection vào DB sau khi "chữa cháy": 8 pod × 50 + 6 × 10 + 5 = **465 > 200**.
- DB 8 vCPU: theo công thức tham khảo `core × 2 + spindle` ≈ 17–20 connection **thực sự làm việc song song** là hiệu quả; vài trăm connection chỉ tăng context switch, lock contention và RAM (mỗi backend PostgreSQL vài MB).

## 5. Nguyên nhân gốc

1. **Transaction bao quanh lời gọi HTTP**: connection và row lock bị giữ trong suốt thời gian chờ PayGW. Thời gian giữ connection = thời gian của dependency chậm nhất → pool cạn ngay khi dependency chậm.
2. **Connection leak** trong job đối soát (thiếu try-with-resources ở nhánh exception) âm thầm giảm dung lượng pool.
3. **`connection-timeout` 30 s mặc định**: request chờ connection giữ luôn Tomcat thread 30 s → cạn Tomcat → mọi endpoint chết.
4. **Pool sizing không tính tổng**: tăng pool × HPA scale vượt `max_connections` của DB dùng chung → sự cố lan sang service khác.

## 6. Giải pháp

### Ngắn hạn (đêm Black Friday)
1. Rollback pool về 10 (hạ tổng connection dưới 200), giới hạn HPA `maxReplicas` 6.
2. Bật circuit breaker cho PayGW với slow-call threshold 2 s (đã có thư viện, chỉ đổi cấu hình) → khi PayGW chậm, checkout trả "thanh toán đang bận, thử lại sau" trong vài ms thay vì giữ connection.
3. `connection-timeout: 2000` để fail fast.
4. Kill các session leak trên DB: `SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE state = 'idle in transaction' AND now() - state_change > interval '10 minutes';`

### Dài hạn
**(1) Không bao giờ gọi mạng khi đang giữ transaction — chia thành các transaction ngắn:**
```java
@Service
@RequiredArgsConstructor
public class CheckoutService {                                  // KHÔNG @Transactional ở đây
    private final TransactionTemplate tx;

    public CheckoutResult checkout(CheckoutCommand cmd) {
        // tx1 (~5 ms): kiểm tra & ghi attempt PENDING với idempotency key
        PaymentAttempt attempt = tx.execute(s -> {
            Order order = orderRepo.findById(cmd.orderId()).orElseThrow();
            order.ensurePayable();
            return attemptRepo.save(PaymentAttempt.pending(order, cmd.idempotencyKey()));
        });

        // ngoài transaction: gọi PayGW (timeout 3 s, circuit breaker, idempotency key gửi kèm)
        PayGwResponse res = payGw.charge(attempt);

        // tx2 (~5 ms): cập nhật kết quả, điều kiện trạng thái để không đè kết quả từ webhook
        return tx.execute(s -> {
            int updated = attemptRepo.completeIfPending(attempt.getId(), res.status(), res.gatewayRef());
            if (updated == 1 && res.isSuccess()) orderRepo.markPaid(attempt.getOrderId());
            return CheckoutResult.of(attempt.getOrderId(), res);
        });
    }
}
```
Trạng thái "treo" (pod chết giữa chừng, timeout không rõ kết quả) được xử lý bằng webhook của PayGW + job đối soát truy vấn trạng thái theo `idempotencyKey` cho attempt `PENDING` quá 5 phút. Row lock `FOR UPDATE` bỏ đi: tính đúng được đảm bảo bằng unique constraint `(order_id) WHERE status IN ('PENDING','SUCCESS')` và UPDATE có điều kiện.

**(2) Sửa leak — để framework quản lý tài nguyên:**
```java
public void exportPending() {
    List<SettlementRow> rows = jdbc.query(
            "select id, order_id, amount from settlement_item where status = 'PENDING' order by id",
            SettlementRow.MAPPER);                                  // JdbcTemplate: luôn đóng ResultSet/Statement/Connection
    sftp.upload(toCsv(rows));                                       // I/O ngoài connection
    jdbc.update("update settlement_item set status = 'EXPORTED' where id = any(?)", toArray(rows));
}
```
Nếu buộc phải dùng JDBC thô: `try (Connection c = ds.getConnection(); PreparedStatement ps = ...; ResultSet rs = ...) { ... }` và rollback trong `catch`.

**(3) Cấu hình pool và lưới an toàn:**
```yaml
spring:
  jpa.open-in-view: false
  datasource.hikari:
    maximum-pool-size: 15
    minimum-idle: 15
    connection-timeout: 2000          # fail fast: thà trả 503 nhanh còn hơn treo Tomcat 30 s
    max-lifetime: 1680000             # < timeout của firewall/LB/DB
    leak-detection-threshold: 20000   # bật thường trực ở production, alert theo log
```
```sql
-- phía PostgreSQL, cho role ứng dụng
ALTER ROLE checkout SET idle_in_transaction_session_timeout = '30s';
ALTER ROLE checkout SET statement_timeout = '15s';
```
**Ngân sách connection** (ghi vào tài liệu kiến trúc, review khi đổi HPA):
```
checkout        8 pod (maxReplicas) × 15 = 120   (+ surge rolling deploy 25% → 150)
order-query     6 pod × 10                =  60   → chuyển sang read replica
batch           1 × 5                     =   5
admin/migration                           =  10
Tổng primary ≈ 165 / max_connections 200 (≤ 80%)  ✔
```
Khi số pod tiếp tục tăng: đặt **PgBouncer** (transaction pooling) giữa app và DB để hàng trăm connection phía client dồn về ~40 connection thật.

### So sánh lựa chọn
| Lựa chọn | Hiệu quả | Giá phải trả |
|---|---|---|
| Tăng `maximum-pool-size` | Chỉ có ích khi pool thật sự nhỏ hơn nhu cầu **hợp lý** (transaction ngắn) | Vượt `max_connections`, DB quá tải; che giấu transaction dài |
| **Rút ngắn transaction (tách HTTP ra ngoài)** | Thời gian giữ connection từ giây xuống mili-giây — chữa gốc | Phải thiết kế trạng thái trung gian, idempotency, đối soát |
| Thanh toán bất đồng bộ (outbox + webhook) | Checkout không phụ thuộc latency của PayGW | UX "đang xử lý", hệ thống phức tạp hơn |
| `connection-timeout` ngắn | Fail fast, không kéo theo Tomcat | Lỗi trả về sớm hơn khi quá tải — phải có thông báo/ retry phía client |
| Bulkhead trước tầng DB (semaphore theo use case) | Use case chậm không chiếm hết pool | Thêm cấu hình, chọn ngưỡng |
| PgBouncer transaction pooling | Nhiều pod mà ít connection thật | Không dùng được session state (`SET`, advisory lock theo session, `LISTEN`); prepared statement cần driver/PgBouncer hỗ trợ |
| Virtual threads | Không còn giới hạn Tomcat thread | Pool DB trở thành nút cổ chai duy nhất — vẫn cần transaction ngắn và giới hạn concurrency |

### Kết quả
Cyber Monday (3 ngày sau, 720 RPS checkout): `hikaricp_connections_usage` p99 12 ms, `pending` = 0, tổng connection vào DB 140; PayGW lại chậm 15 phút nhưng chỉ ảnh hưởng thanh toán (circuit breaker mở), giỏ hàng và tra cứu đơn bình thường.

## 7. Phòng ngừa

**Alert:**
```promql
# Có thread chờ connection kéo dài
max_over_time(hikaricp_connections_pending[1m]) > 0
# Connection bị giữ lâu bất thường
histogram_quantile(0.99, sum by (le, pool) (rate(hikaricp_connections_usage_seconds_bucket[5m]))) > 0.5
# Phía DB (postgres_exporter): transaction idle trong transaction
pg_stat_activity_max_tx_duration{state="idle in transaction"} > 30
```
Và alert trên log `Apparent connection leak detected`.

**Guard trong code — phát hiện gọi HTTP khi đang trong transaction:**
```java
class NoHttpInTransactionInterceptor implements ClientHttpRequestInterceptor {
    @Override
    public ClientHttpResponse intercept(HttpRequest req, byte[] body, ClientHttpRequestExecution exec) throws IOException {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            meterRegistry.counter("http.client.in_transaction", "host", req.getURI().getHost()).increment();
            if (strictMode) throw new IllegalStateException("HTTP call inside DB transaction: " + req.getURI());
        }
        return exec.execute(req, body);
    }
}
```
`strictMode = true` trong test/staging → mọi vi phạm nổ ngay; production chỉ đếm metric.

**Test:** load test có fault injection (PayGW trễ 5 s) và assert `GET /cart` p99 không đổi; integration test cho job đối soát ở nhánh lỗi SFTP, assert `hikaricp_connections_active` về 0.

**Code review checklist:**
- [ ] Method `@Transactional` có gọi HTTP, Kafka `send().get()`, SFTP, email, sleep/retry không?
- [ ] Mọi `getConnection()`/`Statement`/`ResultSet` đều trong try-with-resources (hoặc dùng `JdbcTemplate`)?
- [ ] Thay đổi `maximum-pool-size` hoặc `maxReplicas`: đã cập nhật **ngân sách connection** của DB chưa?
- [ ] `connection-timeout` có ngắn hơn timeout của request phía trên không?

## 8. Cách kể lại trong phỏng vấn (STAR, ~2 phút)

- **S:** "Đêm Black Friday, cổng thanh toán chậm lên 8 giây và checkout service của chúng tôi trả lỗi 'Connection is not available after 30000ms' hàng loạt — kể cả các API giỏ hàng không gọi cổng thanh toán. On-call tăng pool từ 10 lên 50 trong lúc HPA scale lên 8 pod, và DB dùng chung báo 'too many clients', kéo theo hai service khác."
- **T:** "Tôi vào hỗ trợ xử lý sự cố và sau đó dẫn dắt phần sửa gốc."
- **A:** "Tôi hỏi thẳng DB qua `pg_stat_activity`: đa số connection ở trạng thái `idle in transaction` 5–7 giây sau một câu INSERT — tức DB đang chờ ứng dụng. Thread dump cho thấy ứng dụng đang gọi HTTP sang cổng thanh toán bên trong method `@Transactional`. Có cả một connection 'idle in transaction' 2 ngày — leak detection của Hikari chỉ ra job đối soát không đóng connection khi SFTP lỗi. Đêm đó chúng tôi rollback pool, giới hạn HPA, bật circuit breaker và hạ connection-timeout xuống 2 giây. Sau đó tôi tách checkout thành hai transaction ngắn, gọi cổng thanh toán ở giữa với idempotency key và job đối soát cho trạng thái treo; sửa leak bằng `JdbcTemplate`; đặt `idle_in_transaction_session_timeout` phía DB; và lập ngân sách connection cho toàn DB theo `maxReplicas`."
- **R:** "Cyber Monday tải cao hơn 20%, thời gian giữ connection p99 từ 8 giây xuống 12 ms, không có pending; khi cổng thanh toán lại chậm thì chỉ thanh toán bị ảnh hưởng. Chúng tôi thêm interceptor phát hiện gọi HTTP trong transaction — nó bắt được thêm 4 chỗ vi phạm ở các service khác."

## 9. Câu hỏi mở rộng

<details>
<summary>1. Công thức chọn pool size? Vì sao pool nhỏ thường nhanh hơn pool lớn?</summary>

Điểm xuất phát (wiki HikariCP trích PostgreSQL): `connections ≈ core_count × 2 + effective_spindle_count` cho **toàn bộ DB**, rồi chia cho số instance và kiểm chứng bằng load test. DB chỉ thực thi song song được khoảng số core; thêm connection làm tăng context switch, tranh chấp lock/buffer, và RAM — throughput không tăng, latency tăng. Phía ứng dụng, Little's Law cho nhu cầu: `RPS × thời gian giữ connection`. Nếu nhu cầu vượt xa con số DB chịu được thì phải giảm thời gian giữ (transaction ngắn, query nhanh), không phải tăng pool.
</details>

<details>
<summary>2. Pool deadlock là gì? Pool tối thiểu để tránh?</summary>

Khi một thread cần **đồng thời** nhiều connection (transaction ngoài + method `REQUIRES_NEW`, hoặc code tự mở connection thứ hai trong transaction), nếu tất cả connection đã bị các thread đang giữ 1 và chờ cái thứ 2 chiếm hết → không ai tiến được, chờ tới `connectionTimeout`. Pool tối thiểu: `Tn × (Cm − 1) + 1` với `Tn` số thread tối đa đồng thời, `Cm` số connection tối đa một thread cần. Cách tốt hơn là loại bỏ nhu cầu nhiều connection lồng nhau (ví dụ audit sau commit qua `@TransactionalEventListener`/outbox).
</details>

<details>
<summary>3. <code>leakDetectionThreshold</code> có nên bật trên production? Đặt bao nhiêu?</summary>

Có — chi phí rất thấp (một task hẹn giờ cho mỗi lần mượn). Đặt lớn hơn thời gian giữ connection dài nhất **hợp lệ** (ví dụ 20–60 s), nếu không sẽ báo động giả cho batch dài. Đây chỉ là cảnh báo: nếu connection được trả sau đó, Hikari log "Previously reported leaked connection ... was returned to the pool (unleaked)". Kết hợp với `idle_in_transaction_session_timeout` phía PostgreSQL để leak không giữ lock/snapshot mãi mãi.
</details>

<details>
<summary>4. Lỗi lẻ tẻ <code>Connection reset</code>/<code>This connection has been closed</code> sau khi hệ thống "nghỉ" lâu — nguyên nhân?</summary>

Firewall/NAT/LB (hoặc DB với `wait_timeout` ở MySQL) cắt các kết nối TCP idle lâu mà không báo cho client; Hikari mượn ra một connection "chết". Cách xử lý: `max-lifetime` ngắn hơn timeout idle của hạ tầng vài chục giây, `keepalive-time` để ping connection idle, và để Hikari kiểm tra connection khi mượn (JDBC4 `isValid()`). Đừng tắt các cơ chế này để "tiết kiệm".
</details>

<details>
<summary>5. PgBouncer transaction pooling có bẫy gì với ứng dụng Java?</summary>

Trong transaction mode, mỗi transaction có thể chạy trên một server connection khác, nên mọi trạng thái **theo session** đều không đáng tin: `SET` (search_path, timezone), advisory lock theo session, `LISTEN/NOTIFY`, temp table, và server-side prepared statement của pgjdbc (sau `prepareThreshold` lần thực thi). Cách xử lý: `prepareThreshold=0` (tắt server-side prepare) hoặc dùng PgBouncer ≥ 1.21 với `max_prepared_statements`; chuyển `SET` thành `SET LOCAL` trong transaction; dùng advisory lock mức transaction (`pg_advisory_xact_lock`). Hikari vẫn dùng phía ứng dụng nhưng với pool nhỏ.
</details>

<details>
<summary>6. OSIV liên quan gì đến cạn pool?</summary>

Với OSIV bật (mặc định trong Spring Boot), `EntityManager` mở suốt request; với chế độ xử lý connection mặc định mà Spring cấu hình cho Hibernate (`DELAYED_ACQUISITION_AND_HOLD`), một khi session đã lấy connection (kể cả để lazy load ở tầng view), connection có thể bị giữ tới khi request kết thúc — gồm cả thời gian serialize JSON, gọi service khác trong controller. Hậu quả giống hệt transaction dài: `hikaricp_connections_usage` tăng theo thời gian xử lý request chứ không theo thời gian truy vấn. Tắt OSIV là một phần của bản sửa (xem case 05).
</details>

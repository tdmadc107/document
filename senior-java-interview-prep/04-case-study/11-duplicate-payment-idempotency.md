# Case 11 — Khách hàng bị trừ tiền hai lần: retry sau timeout và thiết kế idempotency key

> **Chủ đề:** Idempotency cho POST, retry ở gateway/client, check-then-act race, state machine thanh toán, đối soát (reconciliation)
> **Module liên quan:** [M08 §6 — Idempotency key, ETag](../01-giao-trinh/08-spring-web-rest-security.md#p6) · [M08 §7 — HTTP client & timeout](../01-giao-trinh/08-spring-web-rest-security.md#p7) · [M14 §5 — Idempotency xuyên service, saga](../01-giao-trinh/14-microservices-system-design.md#p5) · [M14 §4 — Retry, backoff](../01-giao-trinh/14-microservices-system-design.md#p4) · [M11 §8 — Locking](../01-giao-trinh/11-database-sql.md#phan-8) · [M12 §12 — Distributed lock](../01-giao-trinh/12-redis-caching.md#phan-12) · [M06 §7 — State pattern](../01-giao-trinh/06-design-principles-patterns.md#p7)
> **Độ khó:** ⭐⭐⭐⭐ (Senior)
> **Thời gian tự giải gợi ý:** 40 phút

---

## 1. Bối cảnh hệ thống

Ứng dụng mua sắm/ví điện tử (minh họa). Khách đặt hàng trên mobile app, bấm "Thanh toán" bằng thẻ đã liên kết.

```
Mobile app ──HTTPS──► API Gateway (Spring Cloud Gateway) ──► order-service ──► payment-service ──► PSP (cổng thanh toán)
   (OkHttp, retry          (timeout 3s, Retry filter            (Boot 3.1)        (Boot 3.1,         p50 600ms
    on IOException)         retries=2 cho mọi method)                              PostgreSQL 15)     p99 1,2s
```

| Thông số | Giá trị |
|---|---|
| Giao dịch thanh toán | ~35.000/giờ bình thường, **~120.000/giờ** trưa ngày lương (12:00–13:00) |
| Giá trị trung bình | 1,4 triệu VND |
| Timeout gateway → order | 3 s |
| Timeout order → payment | 10 s (RestTemplate) |
| Timeout payment → PSP | 30 s |
| PSP | API `POST /v2/charges` nhận `merchantTxnRef` (mã giao dịch phía merchant), có API tra cứu `GET /v2/charges?merchantTxnRef=` |

Code thanh toán hiện tại:

```java
@Transactional
public PaymentResult pay(long orderId, String cardToken) {
    if (paymentRepo.existsByOrderIdAndStatus(orderId, PaymentStatus.SUCCESS)) {   // check
        throw new AlreadyPaidException(orderId);
    }
    Order order = orderClient.get(orderId);
    String txnRef = UUID.randomUUID().toString();                                 // mỗi lần gọi một ref mới!
    PspResponse r = pspClient.charge(txnRef, cardToken, order.total());           // gọi PSP 0,6–6 s
    paymentRepo.save(new Payment(orderId, txnRef, r.status(), order.total()));    // act
    return PaymentResult.of(r);
}
```

---

## 2. Triệu chứng

Thứ Hai, ngày nhận lương. Từ 12:20, PSP gặp sự cố một phần phía ngân hàng phát hành, latency charge p99 lên **4–6 s** (vẫn thành công phần lớn).

```
12:24 [ALERT] api-gateway: 504 rate /api/orders/*/pay = 14% (ngưỡng 2%)
12:31 [ALERT] payment-service: PSP latency p99 = 5,8s
13:05 CSKH: 63 phiếu "bị trừ tiền 2 lần", 9 phiếu "bị trừ 3 lần"
14:10 Ngân hàng đối tác gọi điện: tỷ lệ chargeback dispute tăng bất thường
```

Log gateway:

```
12:26:41.102 WARN  o.s.c.g.f.f.RetryGatewayFilterFactory : Retrying POST /api/orders/7731204/pay attempt=1 cause=TimeoutException
12:26:44.108 WARN  o.s.c.g.f.f.RetryGatewayFilterFactory : Retrying POST /api/orders/7731204/pay attempt=2 cause=TimeoutException
12:26:47.110 ERROR reactor.netty... : 504 GATEWAY_TIMEOUT POST /api/orders/7731204/pay
```

Log payment-service cho cùng order:

```
12:26:38.120 INFO  PaymentService : charge order=7731204 txnRef=0c9e...  amount=2_150_000
12:26:41.118 INFO  PaymentService : charge order=7731204 txnRef=5a71...  amount=2_150_000
12:26:44.125 INFO  PaymentService : charge order=7731204 txnRef=e4b2...  amount=2_150_000
12:26:43.904 INFO  PaymentService : PSP SUCCESS txnRef=0c9e...
12:26:46.877 INFO  PaymentService : PSP SUCCESS txnRef=5a71...
12:26:49.312 INFO  PaymentService : PSP SUCCESS txnRef=e4b2...
```

Ứng dụng hiển thị "Thanh toán thất bại, vui lòng thử lại" (do 504) → nhiều khách bấm thanh toán **lần nữa**.

---

## 3. Câu hỏi đặt ra

1. Có bao nhiêu nguồn tạo request trùng? Vì sao check `existsByOrderIdAndStatus` không chặn được?
2. Thiết kế idempotency key thế nào: ai sinh key, lưu ở đâu, ràng buộc gì, trả gì cho request trùng **đang xử lý**?
3. Khi gọi PSP bị timeout, payment-service nên làm gì — retry, báo lỗi, hay gì khác?
4. Làm sao biết chính xác ai bị trừ trùng để hoàn tiền, và làm sao phát hiện sớm lần sau?

> ✋ **Dừng lại và tự giải trước.** Vẽ sơ đồ thời gian của 3 request cho cùng order và chỉ ra chỗ hai request cùng vượt qua bước kiểm tra.

---

## 4. Điều tra từng bước

### Bước 1 — Định lượng thiệt hại từ DB

```sql
SELECT order_id, COUNT(*) AS n, SUM(amount) AS total
FROM payment
WHERE status = 'SUCCESS' AND created_at >= '2026-09-28 12:00'
GROUP BY order_id HAVING COUNT(*) > 1
ORDER BY n DESC;
-- 1.287 order, trong đó 141 order bị 3 lần; tổng tiền trừ thừa ≈ 1,92 tỷ VND
```

### Bước 2 — Lần theo nguồn gốc request trùng

Dùng `traceId` và header `X-Request-Id` từ gateway:

| Nguồn | Bằng chứng | Tỷ lệ trong 1.287 order |
|---|---|---|
| Gateway retry POST sau timeout 3 s | Cùng `X-Request-Id` gốc, nhiều upstream attempt | ~71% |
| Người dùng bấm lại sau khi app báo thất bại | `X-Request-Id` khác nhau, cách nhau 10–60 s | ~24% |
| OkHttp retry khi `IOException` (mất sóng 4G) | Cùng request, connection reset phía client | ~5% |

Phát hiện: Gateway được cấu hình `Retry` filter dùng chung cho mọi route, gồm cả `POST`:

```yaml
spring:
  cloud:
    gateway:
      default-filters:
        - name: Retry
          args:
            retries: 2
            methods: GET,POST          # ← POST không idempotent
            exceptions: java.util.concurrent.TimeoutException, java.io.IOException
```

### Bước 3 — Vì sao check trong service không chặn được

```
Request A: exists? → false ── gọi PSP (5,8 s) ───────────────────────── save SUCCESS
Request B:        (3 s sau) exists? → false ── gọi PSP (5,7 s) ──────────────── save SUCCESS
```

`existsByOrderIdAndStatus(..., SUCCESS)` chỉ thấy payment **đã thành công và đã commit**. Request đang gọi PSP chưa có bản ghi nào → request trùng đi qua (check-then-act race). Không có unique constraint nào trên `payment(order_id)`. Mỗi lần gọi sinh `txnRef` mới nên PSP coi là ba giao dịch độc lập — **PSP có hỗ trợ chống trùng theo `merchantTxnRef`, nhưng ta tự vô hiệu hóa nó**.

### Bước 4 — Vì sao app báo "thất bại" trong khi tiền đã trừ

Client nhận 504 vì gateway timeout sau 3 s, trong khi payment-service vẫn đang chờ PSP và sau đó thành công. Timeout ở tầng trên **ngắn hơn** tầng dưới (3 s < 10 s < 30 s) — đúng hướng về deadline, nhưng tầng trên đã **diễn giải timeout thành thất bại** thay vì "chưa biết kết quả".

---

## 5. Nguyên nhân gốc

1. **Thao tác không idempotent bị retry ở nhiều tầng**: gateway retry POST, client retry, người dùng bấm lại.
2. **Không có idempotency key xuyên suốt**: mỗi lần gọi sinh `txnRef` mới, nên cả hệ thống lẫn PSP không nhận ra trùng.
3. **Check-then-act không có ràng buộc DB**: kiểm tra bằng SELECT, không có unique constraint, không có trạng thái "đang xử lý".
4. **Timeout bị hiểu sai thành thất bại**: UI bảo người dùng "thử lại" trong khi kết quả là *unknown*.

Yếu tố góp phần: không có job đối soát trong ngày với PSP (chỉ đối soát T+1 bằng file settlement), nên sự cố chỉ được phát hiện qua phản ánh của khách.

---

## 6. Giải pháp

### 6.1 Ngắn hạn (trong ngày)

1. Tắt retry cho `POST` ở gateway (`methods: GET`), deploy config trong 15 phút.
2. Hotfix DB: chặn hai payment thành công cho một order bằng **partial unique index** (PostgreSQL):
   ```sql
   CREATE UNIQUE INDEX CONCURRENTLY ux_payment_order_active
       ON payment(order_id) WHERE status IN ('PENDING', 'SUCCESS');
   ```
   và đổi code: insert `PENDING` **trước** khi gọi PSP; vi phạm unique → trả `409`.
3. Chạy script đối soát với API tra cứu PSP, sinh danh sách 1.287 order trùng, hoàn tiền (refund) qua PSP với `refundRef` cố định theo `paymentId` (để script chạy lại cũng không hoàn hai lần), gửi thông báo xin lỗi.
4. App: thông điệp khi 504 đổi thành "Đang xác nhận thanh toán…" và chuyển sang màn hình polling trạng thái.

### 6.2 Dài hạn — idempotency key theo lớp

**Lớp 1 — Client sinh key cho mỗi *ý định* thanh toán.** App sinh `Idempotency-Key` (UUID v4) khi màn hình xác nhận được mở cho order đó, lưu lại cục bộ, gửi **cùng key** cho mọi lần retry và cả khi người dùng bấm lại.

**Lớp 2 — API lưu key với unique constraint và state machine:**

```sql
CREATE TABLE idempotency_record (
    scope         VARCHAR(32)  NOT NULL,          -- 'payment'
    idem_key      VARCHAR(80)  NOT NULL,          -- userId + ':' + key client gửi
    request_hash  CHAR(64)     NOT NULL,          -- SHA-256 của body chuẩn hóa
    status        VARCHAR(16)  NOT NULL,          -- IN_PROGRESS | COMPLETED
    resource_id   BIGINT,                         -- paymentId
    response_code INT,
    response_body JSONB,
    locked_until  TIMESTAMPTZ  NOT NULL,          -- lease cho bản ghi IN_PROGRESS
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (scope, idem_key)
);
```

```java
@Service
@RequiredArgsConstructor
public class IdempotentExecutor {
    private final IdempotencyRepository repo;

    public <T> IdemResult<T> execute(String key, String hash, Class<T> type, Supplier<IdemOutcome<T>> action) {
        // INSERT ... ON CONFLICT DO NOTHING — transaction ngắn, tự commit ngay
        boolean acquired = repo.tryInsertInProgress(key, hash, Duration.ofSeconds(60));
        if (!acquired) {
            IdempotencyRecord r = repo.find(key);
            if (!r.requestHash().equals(hash)) return IdemResult.mismatch();              // 422
            if (r.status() == COMPLETED)      return IdemResult.replay(r.response(type));  // trả y hệt lần đầu
            if (r.lockedUntil().isAfter(now())) return IdemResult.inFlight();             // 409 + Retry-After: 2
            if (!repo.tryTakeOverExpired(key)) return IdemResult.inFlight();               // lease hết hạn: CAS giành quyền
        }
        IdemOutcome<T> out = action.get();
        if (out.isRetryableFailure()) repo.delete(key);    // 5xx tạm thời: cho phép thử lại với cùng key
        else repo.complete(key, out.status(), out.body()); // thành công hoặc lỗi nghiệp vụ 4xx: lưu để replay
        return IdemResult.fresh(out);
    }
}
```

Điểm quan trọng: bản ghi `IN_PROGRESS` được commit **trước** khi gọi PSP (transaction ngắn, không giữ transaction DB trong suốt lời gọi mạng). Request trùng đến khi bản ghi đang `IN_PROGRESS` → `409 Conflict` + `Retry-After`, client polling `GET /payments?orderId=` thay vì tạo giao dịch mới.

**Lớp 3 — Unique constraint nghiệp vụ và state machine của payment** (hàng phòng thủ cuối, kể cả khi lớp idempotency bị bỏ qua bởi một client khác như admin tool):

```
          create                    PSP success
 (none) ─────────► PENDING ─────────────────────► SUCCESS ──refund──► REFUNDED
                     │  PSP declined
                     ├──────────────────────────► FAILED
                     │  timeout / 5xx / mất kết nối
                     └──────────────────────────► UNKNOWN ──inquiry──► SUCCESS | FAILED
```

```java
@Transactional
public void markSucceeded(long paymentId, String pspTxnId) {
    int n = jdbc.update("""
        UPDATE payment SET status='SUCCESS', psp_txn_id=?, updated_at=now()
        WHERE id=? AND status IN ('PENDING','UNKNOWN')
        """, pspTxnId, paymentId);
    if (n == 0) log.warn("Bỏ qua chuyển trạng thái trùng/không hợp lệ payment={}", paymentId);
}
```

Khi có thêm trạng thái `UNKNOWN`, partial unique index ở 6.1 được mở rộng thành `WHERE status IN ('PENDING','UNKNOWN','SUCCESS')` — một giao dịch chưa rõ kết quả cũng phải chặn attempt mới cho cùng order.

**Lớp 4 — Truyền key xuống PSP.** `merchantTxnRef` **không** sinh ngẫu nhiên mỗi lần gọi mà = `paymentId` (sinh một lần khi tạo bản ghi PENDING). Retry tới PSP với cùng ref → PSP trả lại kết quả giao dịch cũ thay vì charge mới.

**Lớp 5 — Timeout = UNKNOWN, không phải FAILED.** Khi gọi PSP timeout: đánh dấu `UNKNOWN`, **không** retry charge mù quáng; job `PaymentInquiryJob` gọi API tra cứu theo `merchantTxnRef` với backoff (5 s, 15 s, 60 s, 5 phút) để chốt trạng thái; app hiển thị "đang xác nhận".

**Lớp 6 — Đối soát.** Job mỗi 15 phút so `payment` với API tra cứu PSP cho mọi giao dịch `UNKNOWN`/`PENDING` > 2 phút; job T+1 so với file settlement: phát hiện "PSP có, ta không có" (charge mồ côi → refund tự động) và "ta có, PSP không có".

### 6.3 Xử lý request trùng đang bay (in-flight)

| Cách | Hành vi | Ưu | Nhược |
|---|---|---|---|
| Trả `409` + `Retry-After` | Client chờ rồi gọi lại/polling | Đơn giản, không giữ thread | Client phải hiểu 409 |
| Chờ kết quả (long-poll ≤ N giây) | Request thứ hai block tới khi request đầu xong | Trải nghiệm liền mạch | Giữ thread/connection; cần cơ chế notify (Pub/Sub) |
| Trả `202 Accepted` + link trạng thái | Mọi thanh toán là bất đồng bộ | Chuẩn cho tác vụ lâu | Thay đổi API/UI lớn |

Chọn `409 + Retry-After` cho API hiện tại, lộ trình dài hơn chuyển sang mô hình `202` + webhook/polling.

### 6.4 Lưu idempotency key ở đâu

| Phương án | Ưu | Nhược | Khi nào |
|---|---|---|---|
| Bảng DB + unique constraint | Bền, cùng transaction được với dữ liệu nghiệp vụ, audit | Thêm ghi DB mỗi request | **Mặc định cho tiền** |
| Redis `SET key val NX EX 86400` | Rất nhanh, TTL tự dọn | Mất dữ liệu khi failover/eviction → lọt trùng; không atomic với DB | Fast-path chặn trùng phía trước DB, API không phải tiền |
| Cả hai | Redis chặn 99% trùng sớm, DB là chân lý | Hai nơi phải nhất quán | Tải rất cao |
| Chỉ unique constraint nghiệp vụ (`order_id`) | Đơn giản, không cần key | Không replay được response; không phân biệt "thử lại" với "thanh toán lần 2 hợp lệ" (trả góp, thanh toán từng phần) | Bổ trợ |

---

## 7. Phòng ngừa

**Monitoring & alert**

- Metric `payments_duplicate_detected_total` (từ query đối soát chạy mỗi 5 phút): alert khi > 0.
- Tỷ lệ `UNKNOWN` theo phút và tuổi của `UNKNOWN` lâu nhất: alert khi > 1% hoặc > 10 phút.
- Gateway: dashboard retry theo route và method; **policy-as-code** chặn cấu hình retry cho `POST/PATCH` nếu route không khai báo `idempotent: true`.

**Test**

- Integration test (Testcontainers PostgreSQL + WireMock PSP chậm 5 s): 10 request đồng thời cùng key → đúng 1 lần PSP được gọi; cùng key khác body → 422.
- Chaos test: PSP timeout 100% trong 1 phút → không có payment trùng, mọi giao dịch về trạng thái terminal sau khi PSP hồi phục.

**Checklist review**

- [ ] Mọi `POST` thay đổi tiền/tồn kho có idempotency key hoặc khóa nghiệp vụ duy nhất?
- [ ] Có unique constraint ở DB (không chỉ SELECT kiểm tra)?
- [ ] Mã tham chiếu gửi sang bên thứ ba có ổn định qua các lần retry?
- [ ] Timeout được xử lý là "unknown" và có cơ chế tra cứu/đối soát?
- [ ] Retry chỉ ở **một** tầng, có backoff + jitter, chỉ cho lỗi tạm thời?

---

## 8. Cách kể lại trong phỏng vấn (STAR)

- **Situation:** "Trưa ngày lương, PSP chậm lên 5–6 giây. Gateway của bọn mình timeout 3 giây và retry cả POST; app báo thất bại nên khách bấm lại. Kết quả 1.287 đơn bị trừ tiền 2–3 lần, khoảng 1,9 tỷ đồng."
- **Task:** "Mình phụ trách payment-service: chặn trùng ngay, hoàn tiền chính xác và thiết kế lại để không thể tái diễn."
- **Action:** "Trong một giờ đầu tắt retry POST ở gateway và thêm partial unique index để một order chỉ có một payment PENDING/SUCCESS. Viết script đối soát với API tra cứu PSP để lập danh sách hoàn tiền, refund dùng ref cố định để script chạy lại an toàn. Sau đó thiết kế idempotency theo lớp: client sinh key theo ý định thanh toán, bảng idempotency với unique constraint và trạng thái IN_PROGRESS có lease, merchantTxnRef ổn định gửi sang PSP, timeout chuyển trạng thái UNKNOWN và tra cứu thay vì charge lại, cộng job đối soát 15 phút."
- **Result:** "Hoàn tiền xong trong 26 giờ, không sót và không hoàn trùng. Sáu tháng sau, kể cả hai lần PSP sự cố, số giao dịch trùng bằng 0; alert đối soát bắt được 3 charge mồ côi và tự refund. Mình rút ra: idempotency phải xuyên suốt từ client tới bên thứ ba, và đối soát là lưới an toàn bắt buộc cho hệ thống tiền."

---

## 9. Câu hỏi mở rộng

<details>
<summary>1. Vì sao không dùng distributed lock (Redis) theo orderId thay cho idempotency key?</summary>

Lock chỉ ngăn hai request chạy **đồng thời**; request thứ hai đến sau khi lock được nhả vẫn charge lần nữa nếu không có trạng thái lưu bền. Lock TTL còn có thể hết hạn giữa lời gọi PSP chậm (xem Case 15). Idempotency record là trạng thái bền + kết quả để replay; lock nếu có chỉ là tối ưu giảm tranh chấp.
</details>

<details>
<summary>2. Idempotency record IN_PROGRESS bị kẹt khi pod crash giữa chừng thì sao?</summary>

Dùng lease (`locked_until`). Hết lease, request kế tiếp giành quyền bằng UPDATE có điều kiện (CAS). Nhưng trước khi charge lại phải **tra cứu PSP theo merchantTxnRef** — có thể lần trước đã charge thành công rồi mới crash. Vì ref ổn định nên tra cứu luôn trả lời được.
</details>

<details>
<summary>3. Key nên có scope thế nào? TTL bao lâu?</summary>

Gắn với chủ thể (`userId:key` hoặc `merchantId:key`) để tránh va chạm và tránh người khác "đoán" key để đọc response. TTL dài hơn cửa sổ retry thực tế của client (thường 24 h–7 ngày, Stripe dùng 24 h). Sau TTL, unique constraint nghiệp vụ vẫn bảo vệ.
</details>

<details>
<summary>4. Response lỗi có nên lưu để replay không?</summary>

Lỗi nghiệp vụ xác định (thẻ bị từ chối, số dư không đủ — 4xx) nên lưu: replay cùng kết quả là đúng ngữ nghĩa. Lỗi tạm thời (5xx, timeout) không lưu kết quả cuối — hoặc xóa record để cho phép thử lại, hoặc giữ trạng thái UNKNOWN và trả 409/202 tới khi tra cứu xong. Không bao giờ để một lỗi tạm thời bị "đóng băng" thành kết quả vĩnh viễn.
</details>

<details>
<summary>5. Kafka consumer xử lý `PaymentSucceeded` thì idempotency thế nào?</summary>

At-least-once nên consumer dedup theo `eventId` (bảng `processed_message` cùng transaction với thay đổi nghiệp vụ), hoặc thao tác tự idempotent (`UPDATE order SET status='PAID' WHERE id=? AND status='AWAITING_PAYMENT'`). Idempotency HTTP và idempotency consumer là hai lớp khác nhau, cần cả hai.
</details>

<details>
<summary>6. Nếu người dùng thật sự muốn thanh toán lần hai (đơn trước bị hủy) thì key có chặn nhầm không?</summary>

Key gắn với *ý định thanh toán* (attempt), không gắn cứng với order. Khi payment trước kết thúc ở trạng thái FAILED/REFUNDED, app sinh key mới cho lần thử mới; partial unique index chỉ chặn trạng thái `PENDING/UNKNOWN/SUCCESS`, nên một order có thể có nhiều attempt thất bại nhưng tối đa một attempt thành công.
</details>

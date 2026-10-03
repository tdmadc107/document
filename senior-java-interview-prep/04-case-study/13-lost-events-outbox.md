# Case 13 — Đơn hàng đã lưu nhưng sự kiện "OrderCreated" thỉnh thoảng biến mất: dual write và transactional outbox

> **Chủ đề:** Dual-write problem, KafkaTemplate bất đồng bộ, graceful shutdown, transactional outbox, Debezium CDC, polling publisher, ordering, duplicate
> **Module liên quan:** [M13 §10 — Outbox + CDC, idempotent consumer](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p10) · [M13 §3 — Kafka Producer internals](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p3) · [M13 §11 — Duplicate, ordering, replay](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p11) · [M14 §5 — Dữ liệu phân tán, outbox](../01-giao-trinh/14-microservices-system-design.md#p5) · [M09 §11 — Spring Transactions, @TransactionalEventListener](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#11-transactions) · [M07 §14 — Graceful shutdown](../01-giao-trinh/07-spring-core-boot.md#p14) · [M16 §7 — Kubernetes cho developer](../01-giao-trinh/16-devops-build-cloud-security.md#p7)
> **Độ khó:** ⭐⭐⭐⭐ (Senior)
> **Thời gian tự giải gợi ý:** 40 phút

---

## 1. Bối cảnh hệ thống

```
             ┌───────────────── order-service (12 pod, Boot 3.2) ─────────────────┐
 Client ───► │ @Transactional placeOrder():                                       │
             │    orderRepo.save(order)            ──► PostgreSQL 15 (orders)     │
             │    kafkaTemplate.send("order.events", orderId, OrderCreated)       │
             └──────────────────────────────────────┬─────────────────────────────┘
                                                    ▼
                         Kafka "order.events" (12 partition, RF=3, min.insync.replicas=2)
                ┌──────────────────┬───────────────────────┬─────────────────────┐
                ▼                  ▼                       ▼                     ▼
        inventory-service   notification-service    fulfillment-service    analytics (ClickHouse sink)
        (giữ hàng)          (email/SMS xác nhận)    (tạo phiếu xuất kho)
```

| Thông số | Giá trị |
|---|---|
| Đơn hàng | ~240.000 đơn/ngày, đỉnh 120 đơn/s |
| Producer | `acks=all`, idempotence bật (mặc định Kafka ≥ 3.0), `linger.ms=20`, `delivery.timeout.ms=120000` |
| Deploy | Rolling update 3–5 lần/ngày, `terminationGracePeriodSeconds: 30` |
| Kafka | Rolling restart broker hàng tháng để patch OS |

Code hiện tại:

```java
@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orders;
    private final KafkaTemplate<String, OrderCreated> kafka;

    @Transactional
    public Order placeOrder(PlaceOrderCommand cmd) {
        Order order = orders.save(Order.from(cmd));
        kafka.send("order.events", order.getId().toString(), OrderCreated.from(order));  // fire-and-forget
        return order;
    }   // commit sau khi send() đã trả về (send chỉ đưa vào buffer)
}
```

---

## 2. Triệu chứng

- Báo cáo đối soát hằng ngày (so `orders` với bảng `fulfillment_ticket`) cho thấy **80–150 đơn/ngày (~0,05%)** không có phiếu xuất kho. Khách không nhận email xác nhận, hàng không được giữ → có đơn bị bán vượt tồn kho.
- Ngược lại, inventory-service thỉnh thoảng log:

```
WARN  InventoryHandler : OrderCreated for unknown order 98812731 — order not found when calling order-service GET /orders/98812731 (404)
```

- Phân bố theo thời gian: các đơn mất event **tập trung theo cụm**, trùng với giờ deploy order-service và với đợt rolling restart Kafka ngày 14 (một cụm 2.300 đơn trong 6 phút).

Log order-service vào đợt restart broker (chỉ ở mức DEBUG nên không ai thấy):

```
DEBUG o.s.k.c.KafkaTemplate : Failed to send: ProducerRecord(topic=order.events, partition=null, key=98804410, ...)
org.apache.kafka.common.errors.TimeoutException: Expiring 37 record(s) for order.events-4:120003 ms has passed since batch creation
```

---

## 3. Câu hỏi đặt ra

1. Liệt kê mọi kịch bản khiến DB và Kafka lệch nhau với code hiện tại (cả "mất event" lẫn "event ma").
2. Vì sao chuyển `send()` sang sau commit (`afterCommit`) vẫn chưa đủ?
3. Thiết kế transactional outbox: schema, publisher (polling hay CDC), đảm bảo thứ tự và xử lý duplicate.
4. Làm sao bù lại các event đã mất?

> ✋ **Dừng lại và tự giải trước.** Vẽ trục thời gian: `save` → `send` (vào buffer) → `commit` → producer I/O thread gửi batch → broker ack; đánh dấu mọi điểm crash/lỗi có thể xảy ra.

---

## 4. Điều tra từng bước

### Bước 1 — Định lượng và tìm mẫu

```sql
-- Analytics đã sink order.events vào ClickHouse; so với Postgres (export hằng giờ)
SELECT toStartOfFiveMinute(o.created_at) AS bucket, count() AS missing
FROM orders_snapshot o
LEFT ANTI JOIN order_events e ON e.order_id = o.id
WHERE o.created_at >= today() - 14
GROUP BY bucket HAVING missing > 5 ORDER BY bucket;
```

Kết quả: cụm lớn trùng thời gian deploy (pod bị terminate) và đợt broker restart; còn lại rải rác 5–20 đơn/ngày.

### Bước 2 — Hiểu `KafkaTemplate.send()` thực sự làm gì

`send()` chỉ serialize và đưa record vào **RecordAccumulator** (buffer trong bộ nhớ), trả về `CompletableFuture` (Spring Kafka 3.x). Việc gửi thật do thread I/O của producer làm sau đó. Code không kiểm tra future → mọi lỗi gửi (sau tối đa `delivery.timeout.ms` = 120 s) **bị nuốt**.

### Bước 3 — Ba kịch bản mất event được xác nhận

| # | Kịch bản | Bằng chứng |
|---|---|---|
| A | **Pod bị kill khi record còn trong buffer.** Rolling deploy: SIGTERM → Spring graceful shutdown chờ request HTTP đang chạy tới `timeout-per-shutdown-phase=30s`, trong khi K8s chỉ cho `terminationGracePeriodSeconds: 30` → SIGKILL đến trước khi bean producer được `close()` và flush buffer | Cụm mất event trùng thời điểm pod terminate; log không có "Closing the Kafka producer" |
| B | **Broker leader chuyển đổi lâu** (rolling restart, ISR co lại dưới `min.insync.replicas`) → batch hết hạn sau 120 s → `TimeoutException` nhưng future không ai đọc | Log DEBUG ở trên |
| C | **DB commit thất bại sau khi đã send** → event "ma" cho đơn không tồn tại | Inventory nhận `OrderCreated` cho đơn 404; Postgres log `could not serialize access` / unique violation ở flush lúc commit |

Kịch bản C xảy ra vì Hibernate flush (INSERT thật) khi commit — nếu vi phạm constraint hoặc deadlock, transaction rollback, nhưng record đã nằm trong buffer producer và sẽ được gửi.

### Bước 4 — Vì sao "gửi sau commit" chưa đủ

```java
TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
    @Override public void afterCommit() { kafka.send(...); }
});
```

Loại bỏ kịch bản C (không còn event ma), nhưng A và B vẫn còn: process crash/kill giữa commit và lúc producer nhận ack → **mất event vĩnh viễn**, vì không còn ai nhớ phải gửi lại. DB và Kafka là hai hệ thống, không có transaction chung (Kafka không tham gia XA). Bất kỳ "ghi hai nơi" nào cũng có cửa sổ lệch.

---

## 5. Nguyên nhân gốc

**Dual write không nguyên tử** giữa PostgreSQL và Kafka: trạng thái "đã có đơn" và "đã phát event" được ghi vào hai hệ thống độc lập, không có cơ chế bù khi một bên thất bại. Các yếu tố khuếch đại:

- `send()` fire-and-forget, lỗi chỉ log DEBUG.
- Shutdown không đủ thời gian flush producer (grace period K8s = timeout phase của Spring, cộng thêm thời gian drain HTTP).
- Không có đối soát tự động giữa nguồn sự thật (DB) và luồng event.

---

## 6. Giải pháp

### 6.1 Ngắn hạn

1. Xử lý kết quả `send()`: log ERROR + metric `order_event_publish_failed_total`, và ghi đơn lỗi vào bảng `event_retry` (best effort).
2. Graceful shutdown: `terminationGracePeriodSeconds: 60`, `preStop: sleep 10` (chờ endpoint bị gỡ khỏi Service), `spring.lifecycle.timeout-per-shutdown-phase: 40s`.
3. Chuyển `send()` sang `@TransactionalEventListener(phase = AFTER_COMMIT)` để chấm dứt event ma.
4. **Bù event đã mất**: job đối soát lấy danh sách đơn không có event trong 14 ngày (từ query ở bước 1), publish lại `OrderCreated` với **eventId xác định** (`UUIDv5(orderId + "OrderCreated")`) để consumer dedup nếu thực ra đã nhận. Fulfillment kiểm tra đơn còn hợp lệ trước khi tạo phiếu.

Các bước này giảm mất event từ ~0,05% xuống ~0,003% nhưng **không đưa về 0** — cần outbox.

### 6.2 Dài hạn — Transactional Outbox

```sql
CREATE TABLE outbox (
    id             UUID         PRIMARY KEY,      -- = eventId, consumer dedup theo giá trị này
    aggregatetype  VARCHAR(64)  NOT NULL,         -- 'Order' → topic order.events
    aggregateid    VARCHAR(64)  NOT NULL,         -- orderId → Kafka key (thứ tự theo đơn)
    type           VARCHAR(64)  NOT NULL,         -- 'OrderCreated'
    payload        JSONB        NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);
```

```java
@Transactional
public Order placeOrder(PlaceOrderCommand cmd) {
    Order order = orders.save(Order.from(cmd));
    OrderCreated evt = OrderCreated.from(order, UUID.randomUUID());
    outbox.save(OutboxEvent.of("Order", order.getId().toString(), "OrderCreated", json.valueToTree(evt), evt.eventId()));
    return order;                // đơn + event commit cùng lúc, hoặc không gì cả
}
```

**Phương án publisher A — Debezium CDC (chọn cho order-service):**

```json
{
  "name": "order-outbox",
  "config": {
    "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
    "plugin.name": "pgoutput",
    "database.hostname": "order-db", "database.dbname": "orders",
    "database.user": "debezium", "database.password": "${file:/secrets/db.properties:password}",
    "topic.prefix": "orderdb",
    "table.include.list": "public.outbox",
    "slot.name": "order_outbox_slot",
    "heartbeat.interval.ms": "10000",
    "transforms": "outbox",
    "transforms.outbox.type": "io.debezium.transforms.outbox.EventRouter",
    "transforms.outbox.route.by.field": "aggregatetype",
    "transforms.outbox.route.topic.replacement": "order.events",
    "transforms.outbox.table.field.event.key": "aggregateid",
    "transforms.outbox.table.fields.additional.placement": "id:header:eventId,type:header:eventType"
  }
}
```

- Debezium đọc WAL theo thứ tự commit → event của cùng `aggregateid` ra Kafka theo đúng thứ tự, cùng partition (key = orderId).
- Ứng dụng có thể `DELETE` row outbox ngay trong cùng transaction (WAL vẫn chứa INSERT) hoặc dọn bằng job theo `created_at`.
- Kafka Connect chạy với `exactly.once.source.support=enabled` (Kafka 3.3+) giảm duplicate khi connector restart, nhưng consumer **vẫn phải idempotent**.

**Phương án publisher B — Polling publisher** (khi chưa có Kafka Connect):

```java
@Scheduled(fixedDelay = 200)
@SchedulerLock(name = "outboxRelay", lockAtMostFor = "PT30S")   // một relay active để giữ thứ tự toàn cục
@Transactional
public void relay() {
    List<OutboxRow> batch = jdbc.query("""
        SELECT * FROM outbox ORDER BY created_at, id LIMIT 500 FOR UPDATE SKIP LOCKED
        """, rowMapper);
    List<CompletableFuture<?>> acks = batch.stream()
        .map(r -> kafka.send(new ProducerRecord<>("order.events", r.aggregateId(), r.payload())))
        .collect(toList());
    CompletableFuture.allOf(acks.toArray(CompletableFuture[]::new)).join();   // chờ broker ack cả batch
    jdbc.batchUpdate("DELETE FROM outbox WHERE id = ?", batch.stream().map(r -> new Object[]{r.id()}).toList());
}
```

Lưu ý thứ tự: `created_at` lấy giờ lúc INSERT, nhưng transaction commit muộn hơn có thể có `created_at` nhỏ hơn một row đã được relay → thứ tự toàn cục không tuyệt đối. Với yêu cầu "thứ tự theo đơn", điều này chấp nhận được vì các event của cùng một đơn sinh tuần tự. Nếu cần chặt hơn, dùng CDC.

### 6.3 So sánh

| Tiêu chí | Gửi trong transaction (cũ) | Gửi after-commit | Outbox + polling | Outbox + Debezium | Kafka transaction + DB (chained) |
|---|---|---|---|---|---|
| Mất event | Có | Có (crash sau commit) | Không | Không | Có cửa sổ (commit 2 hệ thống tuần tự) |
| Event ma | Có | Không | Không | Không | Có thể |
| Duplicate | Ít | Ít | Có (crash sau send, trước delete) | Có (connector restart) | Ít |
| Độ trễ | Thấp | Thấp | 200 ms–1 s | ~100–500 ms | Thấp |
| Hạ tầng thêm | Không | Không | Không | Kafka Connect, replication slot | Không |
| Rủi ro vận hành | — | — | Bảng phình, polling tải DB | **Slot giữ WAL → đầy đĩa primary** | Phức tạp, dễ hiểu sai |

### 6.4 Phía consumer: duplicate và thứ tự

```java
@KafkaListener(topics = "order.events", groupId = "fulfillment")
@Transactional
public void on(@Payload OrderCreated evt, @Header("eventId") String eventId) {
    if (jdbc.update("INSERT INTO processed_message(consumer_group, message_id) VALUES ('fulfillment', ?) ON CONFLICT DO NOTHING", eventId) == 0) return;
    ticketService.createFor(evt);       // chỉ chạy một lần mỗi eventId
}
```

Với event cập nhật trạng thái (`OrderPaid`, `OrderCancelled`), consumer dùng `version` của aggregate: `UPDATE ... WHERE id=? AND version < ?` để bỏ qua event cũ đến muộn.

---

## 7. Phòng ngừa

**Monitoring & alert**

- `outbox_oldest_unpublished_age_seconds` (polling) hoặc lag của connector (`MilliSecondsBehindSource` của Debezium): alert > 60 s.
- PostgreSQL: `SELECT slot_name, pg_size_pretty(pg_wal_lsn_diff(pg_current_wal_lsn(), confirmed_flush_lsn)) FROM pg_replication_slots;` alert khi > 5 GB; đặt `max_slot_wal_keep_size` (PG 13+) làm cầu chì.
- Đối soát tự động mỗi giờ: số đơn trong DB vs số `OrderCreated` trong analytics, alert khi lệch > 0 sau 10 phút.
- Metric lỗi `send()` của mọi producer, log ERROR chứ không DEBUG.

**Test**

- Integration test (Testcontainers Postgres + Kafka + Debezium): kill container Kafka Connect giữa chừng, chèn 10.000 đơn → consumer nhận đủ 10.000 eventId phân biệt (có thể nhận trùng, nhưng dedup đúng).
- Test rollback: ném exception sau `outbox.save` → không có event nào ra Kafka.

**Checklist review**

- [ ] Có thay đổi nào ghi DB **và** gửi message/gọi API trong cùng một luồng mà không có outbox?
- [ ] Mọi `KafkaTemplate.send()` có xử lý kết quả?
- [ ] Event có `eventId` ổn định và consumer có dedup?
- [ ] Key Kafka có đảm bảo thứ tự theo aggregate?
- [ ] Grace period K8s > thời gian drain HTTP + flush producer + đóng consumer?

**Quy trình:** ADR (Architecture Decision Record) quy định "mọi domain event phát qua outbox", template service có sẵn module outbox; ArchUnit rule cấm inject `KafkaTemplate` vào tầng service nghiệp vụ.

---

## 8. Cách kể lại trong phỏng vấn (STAR)

- **Situation:** "Order-service lưu đơn vào Postgres rồi gọi `kafkaTemplate.send` trong cùng method `@Transactional`. Khoảng 0,05% đơn mỗi ngày không có event, có ngày mất 2.300 đơn khi Kafka rolling restart; ngược lại inventory thỉnh thoảng nhận event cho đơn không tồn tại."
- **Task:** "Mình được giao tìm nguyên nhân, bù dữ liệu bị mất và đưa tỷ lệ mất event về 0."
- **Action:** "Mình đối chiếu thời điểm mất event với lịch deploy và restart broker, và chỉ ra ba kịch bản: pod bị kill khi record còn trong buffer producer, batch hết hạn khi leader chuyển đổi mà future không ai kiểm tra, và DB rollback sau khi đã send. Ngắn hạn: xử lý kết quả send, tăng grace period, gửi after-commit và một job bù event với eventId xác định. Dài hạn: transactional outbox với Debezium Outbox Event Router, key là orderId để giữ thứ tự, consumer dedup theo eventId, giám sát replication slot và đối soát mỗi giờ."
- **Result:** "Ba tháng sau khi rollout, đối soát không phát hiện đơn nào thiếu event; độ trễ end-to-end p99 khoảng 400 ms. Bài học: không có 'gửi cẩn thận hơn' nào giải được dual write — phải đưa việc phát event vào cùng transaction với dữ liệu."

---

## 9. Câu hỏi mở rộng

<details>
<summary>1. Sao không dùng Kafka transaction (`transaction-id-prefix`) kết hợp `@Transactional` của DB?</summary>

Spring có thể đồng bộ hai transaction manager (Kafka transaction commit ngay sau DB commit — kiểu "best effort 1PC"), nhưng đó vẫn là hai commit tuần tự: crash giữa hai commit → lệch. Kafka transaction đảm bảo exactly-once trong phạm vi Kafka→Kafka, không tạo nguyên tử với Postgres.
</details>

<details>
<summary>2. Rủi ro lớn nhất khi vận hành Debezium với PostgreSQL là gì?</summary>

Replication slot: connector dừng (lỗi, bảo trì) → slot giữ WAL → ổ đĩa primary đầy → sự cố toàn bộ DB. Phải giám sát lag theo byte, đặt `max_slot_wal_keep_size`, có runbook xóa/tạo lại slot (kèm snapshot lại). Ngoài ra: failover primary có thể làm mất slot (PG < 17 không đồng bộ slot sang standby), cần kế hoạch re-snapshot.
</details>

<details>
<summary>3. Polling publisher chạy nhiều instance song song có vấn đề gì?</summary>

`FOR UPDATE SKIP LOCKED` tránh gửi trùng giữa các instance, nhưng có thể đảo thứ tự event của cùng aggregate (instance A lấy event 1, instance B lấy event 2 và gửi trước). Giải pháp: một relay active (ShedLock/leader election), hoặc phân mảnh theo `hash(aggregateid) % N` để mỗi aggregate chỉ do một relay xử lý.
</details>

<details>
<summary>4. Outbox có thay được việc consumer idempotent không?</summary>

Không. Outbox đảm bảo **ít nhất một lần** (không mất), không đảm bảo đúng một lần: relay crash sau khi gửi nhưng trước khi đánh dấu, connector restart từ offset cũ, rebalance phía consumer… đều sinh duplicate. Consumer phải dedup theo eventId hoặc thao tác idempotent.
</details>

<details>
<summary>5. Payload outbox nên chứa gì: chỉ id hay toàn bộ trạng thái?</summary>

Event-carried state transfer (đủ dữ liệu) giúp consumer không phải gọi lại order-service (giảm coupling, không bị 404 do đọc replica lag). Event notification (chỉ id) payload nhỏ nhưng consumer phải gọi lại API. Với outbox nên chứa đủ dữ liệu tại thời điểm sự kiện, có `schemaVersion`, và quản lý schema bằng Schema Registry.
</details>

<details>
<summary>6. Nếu service dùng MongoDB thay vì Postgres thì outbox thế nào?</summary>

MongoDB hỗ trợ multi-document transaction (replica set) nên vẫn ghi `orders` và `outbox` cùng transaction; publisher đọc bằng Change Streams hoặc Debezium MongoDB connector. Nếu không dùng transaction, nhúng mảng `pendingEvents` vào chính document đơn hàng (ghi một document là nguyên tử) rồi relay đọc và xóa.
</details>

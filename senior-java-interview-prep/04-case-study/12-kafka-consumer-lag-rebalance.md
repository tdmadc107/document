# Case 12 — Consumer lag lên hàng triệu, rebalance liên tục và một poison message chặn partition

> **Chủ đề:** Kafka consumer group, `max.poll.interval.ms`, rebalance loop, poison pill, DefaultErrorHandler, retry topic, DLT, idempotent consumer
> **Module liên quan:** [M13 §4 — Kafka Consumer internals](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p4) · [M13 §6 — Spring for Apache Kafka](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p6) · [M13 §10 — Idempotent consumer](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p10) · [M13 §11 — Duplicate, backpressure, replay](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p11) · [M13 §12 — Monitoring](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p12) · [M04 §8 — ThreadPoolExecutor](../01-giao-trinh/04-concurrency.md#p8) · [M08 §7 — HTTP client timeout](../01-giao-trinh/08-spring-web-rest-security.md#p7)
> **Độ khó:** ⭐⭐⭐⭐ (Senior)
> **Thời gian tự giải gợi ý:** 45 phút

---

## 1. Bối cảnh hệ thống

`loyalty-service` (minh họa) cộng điểm thưởng cho khách khi đơn hàng hoàn tất. Nó tiêu thụ topic `order.events` do order-service phát.

```
order-service ──► Kafka topic "order.events" (24 partition, RF=3, key = customerId, retention 7 ngày)
                       │
                       ▼  consumer group "loyalty"
          loyalty-service: 6 pod × concurrency 4 = 24 consumer thread
                       │  mỗi record: (1) gọi CRM API lấy hạng thành viên  (2) tính điểm  (3) INSERT point_ledger
                       ▼
               PostgreSQL (point_ledger, customer_points)       CRM (hệ thống bên thứ ba, REST)
```

| Thông số | Giá trị |
|---|---|
| Throughput bình thường | 1.500–3.000 msg/s |
| Ngày sale 11/11 | **9.000 msg/s** đỉnh |
| Thời gian xử lý/record | ~40 ms bình thường (CRM p50 25 ms) |
| CRM lúc sale | p50 300 ms, p99 **2,5 s**, có lúc treo tới 20 s |
| Config consumer | `max.poll.records=500`, `max.poll.interval.ms=300000` (mặc định), `enable.auto.commit=false`, `AckMode.BATCH` |
| HTTP client gọi CRM | `RestTemplate` mặc định — **không read timeout** |

Cấu hình error handler (do một đồng nghiệp thêm để "không bao giờ mất message"):

```java
@Bean
DefaultErrorHandler errorHandler() {
    return new DefaultErrorHandler(new FixedBackOff(1_000L, FixedBackOff.UNLIMITED_ATTEMPTS));
}
```

---

## 2. Triệu chứng

Ngày 11/11, từ 20:05:

```
20:12 [ALERT] kafka consumer group "loyalty" lag = 1.240.000 (ngưỡng 50.000)
20:40 [ALERT] kafka consumer group "loyalty" lag = 3.900.000
21:30 [ALERT] loyalty: rebalance rate = 46/giờ
22:15 CSKH: "đơn đã giao 2 tiếng mà chưa cộng điểm"; vài khách khác: "được cộng điểm gấp đôi"
```

Log consumer (lặp lại liên tục trên nhiều pod):

```
WARN  o.a.k.c.c.i.ConsumerCoordinator : [Consumer clientId=consumer-loyalty-3, groupId=loyalty]
  consumer poll timeout has expired. This means the time between subsequent calls to poll() was longer
  than the configured max.poll.interval.ms, which typically implies that the poll loop is spending too
  much time processing messages. You can address this either by increasing max.poll.interval.ms or by
  reducing the maximum size of batches returned in poll() with max.poll.records.
ERROR o.s.k.l.KafkaMessageListenerContainer : Consumer exception
  org.apache.kafka.clients.consumer.CommitFailedException: Offset commit cannot be completed since the
  consumer is not part of an active group for auto partition assignment; it is likely that the consumer
  was kicked out of the group.
INFO  o.a.k.c.c.i.ConsumerCoordinator : [...] Revoke previously assigned partitions order.events-5, order.events-17
```

Và trên một pod, partition 7:

```
ERROR o.s.k.l.DefaultErrorHandler : Backoff FixedBackOff{interval=1000, currentAttempts=18734, maxAttempts=unlimited}
  exhausted for order.events-7@88123412
java.lang.NullPointerException: Cannot invoke "java.math.BigDecimal.multiply(java.math.BigDecimal)"
  because the return value of "com.shop.loyalty.OrderCompleted.netAmount()" is null
```

```bash
kafka-consumer-groups.sh --bootstrap-server kafka:9092 --describe --group loyalty
# GROUP   TOPIC         PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG      CONSUMER-ID
# loyalty order.events  3          91022110        91190534        168424   -
# loyalty order.events  7          88123412        88294001        170589   consumer-loyalty-2-7f3...
# loyalty order.events  12         90554121        90722877        168756   -
# ...  (nhiều partition không có CONSUMER-ID → group đang rebalance)
```

---

## 3. Câu hỏi đặt ra

1. Vì sao lag tăng ở **mọi** partition, nhưng partition 7 lại có hành vi khác (CURRENT-OFFSET không đổi)?
2. Vì sao có khách được cộng điểm **gấp đôi** trong khi consumer "không xử lý kịp"?
3. Mitigate ngay đêm đó thế nào mà không mất message?
4. Thiết kế lại xử lý lỗi và throughput thế nào cho đợt sale sau?

> ✋ **Dừng lại và tự giải trước.** Tính thử: 500 record × thời gian xử lý mỗi record lúc CRM chậm = bao nhiêu giây so với 300 s?

---

## 4. Điều tra từng bước

### Bước 1 — Phân loại lag

Theo M13 §4.6: lag tăng đều mọi partition → **năng lực xử lý** không đủ; một partition offset đứng yên → **poison pill hoặc consumer kẹt**. Ở đây có cả hai.

```bash
# Theo dõi offset partition 7 trong 2 phút: không đổi
watch -n 10 "kafka-consumer-groups.sh --bootstrap-server kafka:9092 --describe --group loyalty | grep ' 7 '"
```

### Bước 2 — Tính toán thời gian xử lý một batch

```
Lúc sale: CRM p50 300 ms, có đợt 2,5 s
500 record × 0,3 s  = 150 s     (vẫn < 300 s, nhưng sát)
500 record × 0,8 s  = 400 s     (> 300 s → bị đá khỏi group)
1 record treo 20 s (không timeout) cộng dồn → dễ vượt
```

Consumer vượt `max.poll.interval.ms` → client chủ động rời group → **rebalance** → partition chuyển sang thread khác → thread mới **xử lý lại batch từ offset đã commit** (vì batch cũ chưa kịp commit) → cũng chậm như vậy → lại bị đá → **rebalance loop**. Trong mỗi vòng, nhiều record được xử lý (INSERT ledger) nhưng offset không commit được (`CommitFailedException`) → khi xử lý lại, **cộng điểm lần hai**. Đây là nguồn gốc "điểm gấp đôi".

Eager rebalance (assignor mặc định cũ `RangeAssignor` vẫn đứng đầu danh sách, chưa chuyển hẳn sang cooperative) làm **toàn group dừng** mỗi lần → throughput thực tế gần 0 trong lúc lag tăng.

### Bước 3 — Thread dump xác nhận thread kẹt ở đâu

```bash
kubectl exec loyalty-6c9f-xk2p -- jcmd 1 Thread.print | grep -A 12 'org.springframework.kafka.KafkaListenerEndpointContainer#0-2-C-1'
#   java.lang.Thread.State: RUNNABLE
#     at sun.nio.ch.SocketDispatcher.read0(Native Method)
#     ...
#     at org.springframework.web.client.RestTemplate.doExecute(RestTemplate.java:...)
#     at com.shop.loyalty.crm.CrmClient.getTier(CrmClient.java:41)
```

Ba lần dump cách nhau 5 s: 17/24 consumer thread đứng ở `socketRead` của CRM → thiếu **read timeout**.

### Bước 4 — Phân tích poison message ở partition 7

```bash
kafka-console-consumer.sh --bootstrap-server kafka:9092 --topic order.events \
  --partition 7 --offset 88123412 --max-messages 1 --property print.headers=true
# schemaVersion:2  {"orderId":...,"customerId":"C-5512","netAmount":null,"status":"COMPLETED", ...}
```

Một bản ghi do order-service phát cho đơn **0 đồng** (đổi 100% bằng voucher) có `netAmount = null`. Code `netAmount().multiply(rate)` ném NPE. Error handler retry **vô hạn** mỗi 1 s → partition 7 bị chặn vĩnh viễn; 170.000 record phía sau (của các khách có key băm vào partition 7) không được xử lý. Vì blocking retry nằm trong vòng poll, Spring Kafka pause/seek — thread vẫn "sống", không có alert lỗi rõ ràng ngoài log.

### Bước 5 — Các metric xác nhận

| Metric (JMX/Micrometer) | Quan sát |
|---|---|
| `kafka_consumer_coordinator_rebalance_rate_per_hour` | 46 (bình thường ~0) |
| `kafka_consumer_fetch_manager_records_lag_max` | 170k/partition, tăng đều |
| `kafka_consumer_coordinator_commit_rate` | rơi gần 0 |
| `spring_kafka_listener_seconds` (p99) | 2,6 s/record |
| CRM client latency p99 | 2,5 s, max 21 s |

---

## 5. Nguyên nhân gốc

1. **Xử lý đồng bộ phụ thuộc dependency chậm, không timeout**, với `max.poll.records=500` → thời gian xử lý batch vượt `max.poll.interval.ms` → rebalance loop.
2. **Error handler retry vô hạn** cho cả lỗi không thể tự khỏi (NPE do dữ liệu) → poison pill chặn partition.
3. **Consumer không idempotent** (`INSERT point_ledger` không có khóa dedup) → mỗi vòng xử lý lại sinh cộng điểm trùng.
4. Eager rebalance và không có static membership → mỗi lần rebalance dừng cả group.

Yếu tố góp phần: hợp đồng event không quy định `netAmount` bắt buộc; không có contract test giữa order-service và loyalty-service; alert lag theo số message, không theo thời gian trễ.

---

## 6. Giải pháp

### 6.1 Ngắn hạn (đêm 11/11)

1. **Cắt nguồn chậm**: thêm read timeout 800 ms cho CRM và fallback dùng hạng thành viên cache (Caffeine, TTL 1 h) khi CRM lỗi — hạng thành viên ít đổi, chấp nhận được.
2. **Giảm batch & nới interval** qua config (không cần build lại):
   ```yaml
   spring.kafka.consumer.max-poll-records: 50
   spring.kafka.consumer.properties.max.poll.interval.ms: 600000
   ```
3. **Gỡ poison pill có kiểm soát**: deploy bản vá error handler giới hạn retry + DLT (6.2). Record lỗi đi DLT, partition 7 chạy tiếp. Không dùng `--reset-offsets --shift-by 1` vì phải dừng cả group và dễ bỏ sót thêm record.
4. **Chặn cộng trùng**: thêm unique index `point_ledger(order_id, reason)` dùng `CREATE UNIQUE INDEX CONCURRENTLY` sau khi xóa bản trùng (script đối chiếu theo `order_id`), và đổi INSERT thành `ON CONFLICT DO NOTHING`.
5. Scale lên 24 consumer thread hiện có là tối đa (24 partition) → scale pod không giúp thêm; ưu tiên tăng tốc từng record.

Lag về 0 sau 2 giờ 40 phút. Điểm cộng trùng của 3.112 khách được thu hồi bằng script, kèm thông báo.

### 6.2 Dài hạn — xử lý lỗi đúng

```java
@Configuration
class KafkaErrorConfig {

    @Bean
    DefaultErrorHandler errorHandler(KafkaTemplate<Object, Object> template) {
        var recoverer = new DeadLetterPublishingRecoverer(template,
                (rec, ex) -> new TopicPartition(rec.topic() + ".DLT", rec.partition()));
        var backOff = new ExponentialBackOffWithMaxRetries(3);    // tổng ~3,5 s, << max.poll.interval.ms
        backOff.setInitialInterval(500);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(2_000);

        var handler = new DefaultErrorHandler(recoverer, backOff);
        handler.addNotRetryableExceptions(                       // lỗi dữ liệu: retry vô ích → DLT ngay
                InvalidEventException.class, NullPointerException.class,
                JsonProcessingException.class);
        return handler;
    }
}
```

```yaml
spring:
  kafka:
    consumer:
      key-deserializer: org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
      properties:
        spring.deserializer.value.delegate.class: org.springframework.kafka.support.serializer.JsonDeserializer
        partition.assignment.strategy: org.apache.kafka.clients.consumer.CooperativeStickyAssignor
        group.instance.id: ${POD_NAME}          # static membership (StatefulSet); concurrency > 1 thì Spring tự thêm hậu tố -n
        max.poll.interval.ms: 300000
      max-poll-records: 100
```

`ErrorHandlingDeserializer` bọc lỗi deserialize thành `DeserializationException` → handler đưa thẳng vào DLT thay vì kẹt trong `poll()`. Validate event ngay đầu listener (`InvalidEventException`) thay vì để NPE xảy ra sâu bên trong.

**Phân loại lỗi:**

| Loại lỗi | Ví dụ | Xử lý |
|---|---|---|
| Dữ liệu sai/không parse được | `netAmount=null`, JSON hỏng | DLT ngay, alert, sửa dữ liệu rồi **replay từ DLT** bằng tool |
| Dependency tạm thời, ngắn | DB deadlock, CRM 503 lẻ tẻ | Blocking retry 3 lần (giữ thứ tự) |
| Dependency sự cố kéo dài | CRM down 10 phút | Non-blocking: `@RetryableTopic` (retry topic 30 s, 2 phút, 10 phút) hoặc fallback dữ liệu cache |

Vì cộng điểm là phép **giao hoán** (thứ tự giữa các đơn của cùng khách không quan trọng khi có dedup), mất thứ tự do retry topic là chấp nhận được. Với luồng cần thứ tự (sổ cái số dư, state machine), chỉ dùng blocking retry + pause container.

### 6.3 Dài hạn — throughput

- **Bỏ lời gọi CRM khỏi đường nóng**: loyalty-service tiêu thụ topic `crm.member-tier` (compacted) để giữ bảng hạng thành viên cục bộ — chuyển từ "gọi đồng bộ" sang "event-carried state transfer".
- **Batch listener** + bulk insert:
  ```java
  @KafkaListener(topics = "order.events", groupId = "loyalty", batch = "true")
  public void onBatch(List<ConsumerRecord<String, OrderCompleted>> records) {
      List<LedgerRow> rows = records.stream().map(this::toLedgerRow).toList();
      ledgerRepo.batchInsertIgnoreDuplicates(rows);   // INSERT ... ON CONFLICT DO NOTHING, batch 100
  }
  ```
  Với batch listener, lỗi một record xử lý bằng `BatchListenerFailedException(msg, index)` để error handler biết record nào lỗi, commit phần trước và DLT đúng record.
- **Song song theo key** khi cần vượt số partition: Confluent Parallel Consumer (`ProcessingOrder.KEY`) giữ thứ tự theo `customerId` nhưng xử lý nhiều key đồng thời trong một partition.
- **Tăng partition có kế hoạch** (24 → 48): thay đổi ánh xạ key → partition, chỉ làm khi consumer idempotent và chấp nhận đảo thứ tự tạm thời trong cửa sổ chuyển đổi.
- **Autoscale bằng KEDA** theo lag, `maxReplicaCount` ≤ số partition / concurrency.

### 6.4 Idempotent consumer

```sql
ALTER TABLE point_ledger ADD COLUMN event_id UUID;
CREATE UNIQUE INDEX CONCURRENTLY ux_point_ledger_event ON point_ledger(event_id);
```

```java
@Transactional
public void apply(OrderCompleted evt) {
    int inserted = jdbc.update("""
        INSERT INTO point_ledger(event_id, customer_id, order_id, points)
        VALUES (?, ?, ?, ?) ON CONFLICT (event_id) DO NOTHING
        """, evt.eventId(), evt.customerId(), evt.orderId(), points(evt));
    if (inserted == 1) {
        jdbc.update("UPDATE customer_points SET balance = balance + ? WHERE customer_id = ?",
                points(evt), evt.customerId());
    }
}
```

`eventId` do producer sinh (ổn định qua retry/outbox), không dùng offset.

### 6.5 So sánh các hướng xử lý chậm

| Cách | Hiệu quả | Rủi ro / chi phí |
|---|---|---|
| Giảm `max.poll.records` | Batch ngắn hơn, ít vượt interval | Nhiều poll hơn, throughput giảm nhẹ |
| Tăng `max.poll.interval.ms` | Chịu batch dài | Consumer kẹt thật được phát hiện muộn hơn |
| Timeout + fallback dependency | Chặn đúng gốc | Cần dữ liệu fallback hợp lý |
| Worker pool + `pause()`/`resume()` | Song song cao | Tự quản lý commit offset theo thứ tự — dễ sai |
| Parallel Consumer theo key | Song song cao, giữ thứ tự theo key | Thêm thư viện, commit offset dạng encoded |
| Batch listener + bulk I/O | Giảm round-trip DB/API | Xử lý lỗi từng record phức tạp hơn |

---

## 7. Phòng ngừa

**Monitoring & alert**

- Alert theo **time lag** (thời gian trễ của record cũ nhất chưa xử lý, Kafka Lag Exporter/kminion), ví dụ > 5 phút, thay vì số message.
- Alert khi **một partition** có offset không đổi > 5 phút trong khi log-end tăng (dấu hiệu poison pill).
- Alert `rebalance-rate-per-hour > 5`, `commit-rate` giảm đột ngột, số record vào DLT > 0 (kèm link tới tool replay).
- Metric latency của từng dependency gọi trong listener.

**Test**

- Contract test (Spring Cloud Contract/Pact) cho schema event giữa order-service và loyalty-service; Schema Registry với compatibility `BACKWARD` và field bắt buộc rõ ràng.
- Integration test Testcontainers Kafka: gửi một record hỏng giữa 100 record hợp lệ → 99 được xử lý, 1 vào DLT, không chặn partition.
- Test idempotency: gửi cùng event hai lần → ledger chỉ có một dòng.
- Load test với dependency giả lập chậm 2 s → không có rebalance.

**Checklist review cho consumer**

- [ ] Mọi lời gọi I/O trong listener có timeout? Tổng thời gian xấu nhất của một batch < `max.poll.interval.ms`?
- [ ] Error handler có giới hạn retry, phân loại lỗi không retry, có DLT và có người sở hữu DLT?
- [ ] Có `ErrorHandlingDeserializer`?
- [ ] Consumer idempotent theo `eventId`?
- [ ] Cooperative assignor + static membership khi chạy trên Kubernetes?

---

## 8. Cách kể lại trong phỏng vấn (STAR)

- **Situation:** "Đêm 11/11, loyalty-service tiêu thụ topic 24 partition, lag lên gần 4 triệu message, group rebalance 46 lần mỗi giờ; một số khách không được cộng điểm, một số lại được cộng gấp đôi."
- **Task:** "Mình on-call, cần đưa lag về 0 trong đêm mà không mất event, rồi thiết kế lại để đợt sale sau không lặp lại."
- **Action:** "Thread dump cho thấy phần lớn consumer thread treo ở socket read tới CRM — không có read timeout; 500 record × vài trăm ms vượt `max.poll.interval.ms` nên consumer bị đá liên tục, xử lý lại batch chưa commit, nên cộng điểm trùng. Partition 7 đứng yên do một event `netAmount=null` bị retry vô hạn. Mình thêm timeout 800 ms và fallback hạng thành viên từ cache, giảm `max.poll.records` xuống 50, deploy error handler giới hạn retry với DLT và phân loại lỗi không retry, thêm unique index để chặn cộng trùng. Dài hạn, mình bỏ lời gọi CRM khỏi đường nóng bằng topic compacted, chuyển sang batch listener, cooperative sticky assignor và static membership, alert theo time lag và partition đứng yên."
- **Result:** "Lag về 0 sau 2 giờ 40 phút. Sale 12/12 với 11 nghìn msg/s, time lag max 14 giây, không rebalance ngoài lúc deploy, DLT nhận 37 record dữ liệu lỗi và được replay sau khi sửa. Bài học: consumer phải được thiết kế cho at-least-once và cho dependency chậm ngay từ đầu."

---

## 9. Câu hỏi mở rộng

<details>
<summary>1. `session.timeout.ms` và `max.poll.interval.ms` khác nhau thế nào? Vì sao heartbeat vẫn chạy mà consumer vẫn bị đá?</summary>

Heartbeat được gửi bởi background thread, chứng minh **process** còn sống (`session.timeout.ms`, mặc định 45 s từ Kafka 3.0). `max.poll.interval.ms` chứng minh **vòng xử lý** không kẹt — tính giữa hai lần gọi `poll()`. Thread xử lý treo ở socket read vẫn có heartbeat bình thường, nhưng không gọi `poll()` → vượt interval → client tự gửi LeaveGroup.
</details>

<details>
<summary>2. Khi nào chọn blocking retry, khi nào `@RetryableTopic`?</summary>

Blocking (`DefaultErrorHandler`) giữ thứ tự trong partition nhưng chặn partition trong lúc backoff — hợp với lỗi ngắn và luồng cần thứ tự (ledger, state machine). Non-blocking đẩy record sang retry topic, partition chính chạy tiếp nhưng mất thứ tự — hợp với event độc lập (email, webhook, cộng điểm có dedup). Tổng backoff blocking phải nhỏ hơn `max.poll.interval.ms`.
</details>

<details>
<summary>3. Có nên commit offset rồi mới xử lý để tránh xử lý trùng?</summary>

Đó là at-most-once: crash sau commit là **mất** message. Với cộng điểm/tiền, chọn at-least-once + idempotent consumer. Exactly-once của Kafka chỉ áp dụng Kafka→Kafka (transaction + `sendOffsetsToTransaction`); Kafka→DB thì lưu offset cùng transaction DB hoặc dedup.
</details>

<details>
<summary>4. Replay DLT thế nào cho an toàn?</summary>

Tool đọc DLT (header chứa topic/partition/offset gốc và exception), cho phép lọc theo loại lỗi, sửa/bỏ qua, rồi publish lại vào topic gốc hoặc topic "replay" riêng. An toàn nhờ consumer idempotent. Không replay tự động vô điều kiện — dễ tạo vòng lặp poison pill mới.
</details>

<details>
<summary>5. Tăng số partition có giải quyết lag không? Rủi ro gì?</summary>

Có, nếu nút thắt là số consumer song song. Nhưng: key → partition thay đổi (hash mod N) → record mới của cùng key có thể nằm partition khác record cũ → mất thứ tự tạm thời; không giảm được partition sau đó; nhiều partition tăng chi phí broker và thời gian rebalance. Thường tối ưu xử lý từng record và batch I/O trước khi tăng partition.
</details>

<details>
<summary>6. KIP-848 (`group.protocol=consumer`) thay đổi gì cho tình huống này?</summary>

Assignment chuyển sang phía broker, rebalance hoàn toàn incremental, không còn "global barrier" — một consumer chậm/bị đá không làm cả group dừng. GA từ Kafka 4.0 (client và broker đều phải hỗ trợ). Tuy vậy nó không chữa được gốc: xử lý quá chậm và poison pill vẫn phải xử lý như trên.
</details>

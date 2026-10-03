# Câu hỏi phỏng vấn — Module 13: Messaging & Event-Driven (Kafka, RabbitMQ)

> Giáo trình tương ứng: [Module 13 — Messaging & Event-Driven: Kafka, RabbitMQ](../01-giao-trinh/13-messaging-kafka-rabbitmq.md)

> **Cách dùng:** đọc câu hỏi, **tự trả lời thành tiếng** (≈30 giây cho ý chính, 2–3 phút cho phần chi tiết) **trước khi** mở đáp án. Sau đó so với "Trả lời ngắn", luyện tiếp các "Câu hỏi nối tiếp", và ghi lại câu nào bạn rơi vào "⚠️ Câu trả lời gây điểm trừ" để ôn theo link 📖.
>
> **Mức độ:** 🟢 Cơ bản — 🟡 Senior — 🔴 Xoáy sâu. Câu có nhãn **🎯 Tình huống** là câu "bạn sẽ làm gì".

## Mục lục

| Nhóm | Chủ đề | Câu hỏi |
|---|---|---|
| A | [Nền tảng messaging bất đồng bộ](#nhom-a) | Q1–Q4 |
| B | [Kiến trúc Kafka](#nhom-b) | Q5–Q11 |
| C | [Kafka Producer](#nhom-c) | Q12–Q16 |
| D | [Kafka Consumer](#nhom-d) | Q17–Q24 |
| E | [Ordering, key, retention & compaction](#nhom-e) | Q25–Q28 |
| F | [Spring for Apache Kafka](#nhom-f) | Q29–Q33 |
| G | [Schema evolution](#nhom-g) | Q34–Q36 |
| H | [RabbitMQ](#nhom-h) | Q37–Q42 |
| I | [Kafka vs RabbitMQ](#nhom-i) | Q43–Q44 |
| J | [Pattern event-driven: idempotency, outbox, inbox, event sourcing, saga](#nhom-j) | Q45–Q52 |
| K | [Duplicate, ordering, backpressure, replay & vận hành](#nhom-k) | Q53–Q57 |

---

<a id="nhom-a"></a>
## A. Nền tảng messaging bất đồng bộ

### Q1. 🟢 Vì sao cần messaging bất đồng bộ? Cái giá phải trả là gì?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Gọi đồng bộ tạo ba loại coupling: **temporal** (A và B phải cùng sống), **availability** (availability của A ≈ tích availability các phụ thuộc), **load** (peak của A dồn thẳng vào B). Broker ở giữa giúp **decoupling theo thời gian**, **load leveling**, **fan-out** và scale consumer. Cái giá: **eventual consistency**, thêm hệ thống phân tán phải vận hành, debug khó (cần correlation id/tracing), và phải tự xử lý **duplicate, ordering, poison message**.

**Giải thích chi tiết:**
- Dùng async cho: tác vụ có thể trễ (email, search index), fan-out sự kiện nghiệp vụ, tích hợp hệ thống có tốc độ khác nhau, ingest dữ liệu lớn (log, clickstream, CDC).
- Giữ sync khi caller **cần câu trả lời ngay** để trả cho user (kiểm tra số dư trước khi rút tiền).
- "Đưa vào queue là xong" là sai: broker chỉ chuyển trách nhiệm, không xóa việc xử lý lỗi.

**Câu hỏi nối tiếp:**
- *Dùng Kafka như RPC (gửi request, chờ reply ở topic khác) được không?* — Được về kỹ thuật nhưng độ trễ cao, phức tạp, mất lợi ích async; hạn chế dùng.

**⚠️ Câu trả lời gây điểm trừ:**
- "Async luôn nhanh hơn" — async giảm coupling, không làm nghiệp vụ hoàn thành nhanh hơn.
- Không nhắc tới duplicate/ordering/eventual consistency.

**📖 Ôn lại:** [Phần 1 — Vì sao cần messaging](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p1)

</details>

### Q2. 🟢 Phân biệt Queue, Pub/Sub và Log.

<details><summary>Đáp án</summary>

**Trả lời ngắn:** **Queue** (point-to-point): mỗi message một consumer nhận, **xóa sau ack** (RabbitMQ queue, SQS). **Pub/Sub**: mỗi subscriber nhận bản sao (RabbitMQ fanout/topic exchange → mỗi subscriber một queue). **Log** (Kafka): append-only, **không xóa khi đọc**, mỗi consumer group tự giữ **offset**, có thể **replay**; ordering đảm bảo **trong partition**.

**Giải thích chi tiết:**

| Tiêu chí | Queue | Pub/Sub | Log (Kafka) |
|---|---|---|---|
| Sau khi đọc | Xóa | Xóa khỏi queue subscriber | Giữ theo retention |
| Nhiều nhóm độc lập | Không (cạnh tranh) | Có | Có (consumer group) |
| Replay | Không | Không | **Có** |
| Routing linh hoạt | Trung bình | **Cao** | Thấp (topic + key) |
| Trạng thái đọc ở | Broker (ack từng message) | Broker | Consumer (offset trong `__consumer_offsets`) |

**Câu hỏi nối tiếp:**
- *Khi nào log tốt hơn queue nhờ replay?* — Đồng bộ sang Elasticsearch/data lake: rebuild index bằng cách đọc lại từ đầu.

**⚠️ Câu trả lời gây điểm trừ:**
- "Kafka là một message queue như RabbitMQ" mà không nói đến log/offset/replay.

**📖 Ôn lại:** [Phần 1 — Ba mô hình](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p1)

</details>

### Q3. 🟡 Command và Event khác nhau thế nào? Ai sở hữu schema?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** **Command** = "hãy làm X" (`ChargePayment`): có người nhận chủ định, có thể bị từ chối, thường qua queue; **consumer sở hữu** schema (đó là API của consumer). **Event** = "X đã xảy ra" (`PaymentCharged`): sự thật quá khứ, bất biến, tên ở thì quá khứ, producer không quan tâm ai nghe; **producer sở hữu** schema.

**Giải thích chi tiết:**
- Payload event chuẩn: `eventId` (UUIDv7), `eventType`, `version`, `occurredAt`, `aggregateId`, `payload`.
- Đặt tên event như command (`SendEmailEvent`) → producer ngầm điều khiển consumer → coupling ngược.

**Câu hỏi nối tiếp:**
- *Vì sao `eventId` do producer sinh quan trọng?* — Là khóa dedup ổn định cho consumer (offset có thể đổi khi republish).

**⚠️ Câu trả lời gây điểm trừ:**
- Coi command và event là một, đặt tên tùy ý.

**📖 Ôn lại:** [Phần 1 — Message, Command, Event](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p1)

</details>

### Q4. 🟡 🎯 Tình huống: checkout gọi đồng bộ Inventory (99.9%), Payment (99.95%), Shipping (99.5%), Email (99%). Availability tổng là bao nhiêu và bạn đổi gì?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** `0.999 × 0.9995 × 0.995 × 0.99 ≈ 98.4%` (~5,9 ngày downtime/năm). Chuyển **Email** và **Shipping** (nếu báo giá không chặn đặt hàng) sang async qua event `OrderPlaced` → còn `0.999 × 0.9995 ≈ 99.85%`. Payment cân nhắc kỹ: user cần biết kết quả; có thể authorize sync và capture async.

**Giải thích chi tiết:**
- Event phải được phát **tin cậy** (transactional outbox) — nếu không, đổi sang async làm mất email khi crash.
- Trả `202 Accepted` + trạng thái cho bước async; UI hiển thị "đang xử lý".

**Câu hỏi nối tiếp:**
- *Làm sao user biết email đã gửi thất bại?* — Trạng thái notification trong DB + DLT + alert; không chặn checkout.

**⚠️ Câu trả lời gây điểm trừ:**
- Cộng availability thay vì nhân; hoặc chuyển cả Payment sang async mà không bàn UX.

**📖 Ôn lại:** [Phần 1 — Bài tập 1.2](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p1)

</details>

---

<a id="nhom-b"></a>
## B. Kiến trúc Kafka

### Q5. 🟢 Giải thích broker, topic, partition, offset. Partition là đơn vị của những gì?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** **Broker** là tiến trình Kafka lưu partition trên đĩa. **Topic** là tên logic = tập các partition. **Partition** là **append-only log có thứ tự** — là đơn vị **song song**, **phân phối dữ liệu** và **đảm bảo ordering**. **Offset** là số int64 tăng dần của record, chỉ có nghĩa trong partition đó. Mỗi partition có RF bản sao: một **leader** nhận produce/fetch, các **follower** kéo dữ liệu từ leader.

**Giải thích chi tiết:**
- Record gồm `key`, `value`, `headers`, `timestamp`.
- Partition trên đĩa chia thành **segment** (`.log`, `.index` sparse, `.timeindex`), tên file = base offset; chỉ active segment được ghi; retention xóa **cả segment**.
- Số partition quyết định parallelism tối đa của một consumer group.

**Câu hỏi nối tiếp:**
- *Tìm record theo offset thế nào?* — Binary search trên sparse index (mỗi ~4KiB một entry) rồi quét tuần tự đoạn ngắn.

**⚠️ Câu trả lời gây điểm trừ:**
- "Offset là id toàn cục của message trong topic".

**📖 Ôn lại:** [Phần 2 — Khái niệm cốt lõi](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p2)

</details>

### Q6. 🟡 Vì sao Kafka đạt throughput rất cao dù lưu trên đĩa?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** (1) **Ghi tuần tự** (append-only) — sequential I/O nhanh cả trên HDD; (2) dựa vào **OS page cache** thay vì cache trong heap JVM (tránh GC, sống qua restart process); (3) **zero-copy** (`sendfile`) từ page cache ra socket khi không dùng TLS; (4) **batch + nén cả batch** (lz4, zstd); (5) consumer **pull** theo tốc độ của mình.

**Giải thích chi tiết:**
- Producer gom batch theo partition (`batch.size`, `linger.ms`); broker lưu batch nén nguyên trạng, consumer giải nén.
- Bật TLS làm mất zero-copy (phải mã hóa trong user space) → CPU broker tăng.
- Index sparse giúp file index nhỏ, nằm trọn trong page cache.

**Câu hỏi nối tiếp:**
- *Vì sao không nên cấp heap quá lớn cho broker?* — RAM nên để cho page cache; heap broker thường vài GB.

**⚠️ Câu trả lời gây điểm trừ:**
- "Vì Kafka lưu mọi thứ trong RAM".

**📖 Ôn lại:** [Phần 2 — Segment và index](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p2)

</details>

### Q7. 🔴 Giải thích ISR, LEO, High Watermark. Consumer đọc được tới đâu?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** **LEO** (Log End Offset) là offset kế tiếp sẽ ghi trên một replica. **ISR** là tập replica (gồm leader) đang theo kịp; follower bị loại nếu không fetch kịp tới LEO của leader trong `replica.lag.time.max.ms` (30s). **High Watermark** là offset lớn nhất đã có trên **mọi** replica trong ISR. Consumer (`read_uncommitted`) chỉ đọc được **dưới HW** → không bao giờ đọc record có thể bị mất khi đổi leader.

**Giải thích chi tiết:**
```
Leader P0:     [0][1][2][3][4][5][6]   LEO = 7
Follower B2:   [0][1][2][3][4][5]      LEO = 6
Follower B3:   [0][1][2][3][4]         LEO = 5
                              HW = 5 → consumer đọc 0..4
```
- Leader mới được chọn **từ ISR** → không mất record đã commit (dưới HW).
- **Leader epoch** (KIP-101): follower truncate log theo epoch thay vì HW, tránh lệch/mất dữ liệu khi đổi leader liên tiếp.
- Consumer `read_committed` đọc tới **LSO** (Last Stable Offset) — thấp hơn hoặc bằng HW.

**Câu hỏi nối tiếp:**
- *Follower chậm làm gì với producer `acks=all`?* — Trước khi bị loại khỏi ISR, leader phải chờ nó → produce latency tăng; sau khi bị loại, ISR co lại.

**⚠️ Câu trả lời gây điểm trừ:**
- "Consumer đọc ngay khi leader ghi xong".

**📖 Ôn lại:** [Phần 2 — Replication, ISR, High Watermark](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p2)

</details>

### Q8. 🟡 Cấu hình `RF=3, min.insync.replicas=2, acks=all` chịu được lỗi gì? Vì sao `min.insync.replicas=3` là bẫy?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Producer chỉ nhận thành công khi record nằm trên mọi replica trong ISR **và** ISR có ≥2 thành viên. Mất 1 broker: vẫn ghi được, không mất dữ liệu. Mất 2 broker: **từ chối ghi** (`NotEnoughReplicasException`) — ưu tiên consistency hơn availability. `min.insync.replicas=3` với RF=3: chỉ cần **một broker restart** (rolling upgrade) là mọi producer `acks=all` lỗi.

**Giải thích chi tiết:**
- `acks=all` mà `min.insync.replicas=1` thì "all" có thể chỉ là leader → mất dữ liệu khi leader chết.
- `NotEnoughReplicasException` là retriable: producer retry tới hết `delivery.timeout.ms` rồi mới báo lỗi qua callback.
- Đặt replica ở các rack/AZ khác nhau (`broker.rack`).

**Câu hỏi nối tiếp:**
- *Đọc có bị ảnh hưởng khi chỉ còn 1 broker?* — Đọc vẫn được (dữ liệu dưới HW).

**⚠️ Câu trả lời gây điểm trừ:**
- `replication.factor=1` trên production "cho tiết kiệm".

**📖 Ôn lại:** [Phần 2 — Replication, ISR, High Watermark](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p2)

</details>

### Q9. 🔴 `unclean.leader.election.enable` là gì? Tái hiện kịch bản mất dữ liệu.

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Mặc định `false`: nếu ISR trống, partition **offline** thay vì chọn replica tụt hậu làm leader. Bật `true` = ưu tiên **availability**, chấp nhận **mất dữ liệu**. Kịch bản: follower B3 rời ISR → leader ghi thêm 1000 record (chỉ B1, B2 có) → B1, B2 chết → B3 lên leader với log thiếu → B1 quay lại **truncate** theo leader mới → mất vĩnh viễn 1000 record.

**Giải thích chi tiết:**
- Chỉ bật cho dữ liệu "best effort" (metrics, log tạm), **không bao giờ** cho topic tài chính.
- Metric cảnh báo: `OfflinePartitionsCount > 0`.

**Câu hỏi nối tiếp:**
- *Partition offline thì làm gì?* — Khôi phục broker có dữ liệu trong ISR; chỉ bật unclean tạm thời cho topic đó nếu chấp nhận mất và có quyết định nghiệp vụ.

**⚠️ Câu trả lời gây điểm trừ:**
- Bật unclean "để hệ thống luôn sống" mà không nói đến mất dữ liệu.

**📖 Ôn lại:** [Phần 2 — Leader election](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p2)

</details>

### Q10. 🟢 ZooKeeper mode và KRaft khác nhau thế nào?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** ZooKeeper mode lưu metadata ở ZooKeeper ensemble riêng, một broker làm controller; failover controller chậm vì phải load toàn bộ metadata. **KRaft** lưu metadata trong topic nội bộ `__cluster_metadata`, đồng thuận bằng **Raft** giữa quorum controller (3 hoặc 5 node); standby controller đã có metadata trong bộ nhớ → failover nhanh, hỗ trợ hàng triệu partition. KRaft production-ready từ 3.3; **Kafka 4.0 loại bỏ ZooKeeper**.

**Giải thích chi tiết:**
- `process.roles=broker|controller|broker,controller` — combined chỉ cho dev/cluster nhỏ.
- Bớt một hệ thống phân tán phải vận hành và bảo mật.

**Câu hỏi nối tiếp:**
- *Lỗi kinh điển khi chạy Kafka trong Docker/K8s?* — `advertised.listeners` sai: bootstrap được nhưng client sau đó gọi hostname nội bộ không resolve.

**⚠️ Câu trả lời gây điểm trừ:**
- Không biết Kafka 4.0 đã bỏ ZooKeeper.

**📖 Ôn lại:** [Phần 2 — ZooKeeper vs KRaft](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p2)

</details>

### Q11. 🟡 Chọn số partition thế nào? Tăng partition sau này có vấn đề gì?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** `partitions ≥ max(target_throughput / throughput_mỗi_partition_phía_producer, target_throughput / throughput_mỗi_consumer)`, cộng dư địa tăng trưởng và tính **tổng consumer** của mọi instance (4 pod × concurrency 3 = 12 → cần ≥12). Có thể **tăng** nhưng **không giảm**; tăng partition **phá mapping key → partition** (`murmur2(key) % N`) → ordering theo key bị ảnh hưởng trong giai đoạn chuyển tiếp.

**Giải thích chi tiết:**
- Quá nhiều partition: tốn file handle, leader election/recovery lâu hơn, end-to-end latency tăng, tốn bộ nhớ batch producer.
- Consumer > partition → consumer thừa ngồi chơi.
- Cần tăng partition cho topic key-ordering: tạo topic mới nhiều partition hơn + migration có kế hoạch (dual-write/bridge, chuyển consumer khi đã drain).

**Câu hỏi nối tiếp:**
- *Follower fetching là gì?* — KIP-392: consumer đọc từ replica cùng AZ (`client.rack`) giảm chi phí cross-AZ.

**⚠️ Câu trả lời gây điểm trừ:**
- "Cứ tạo 1000 partition cho chắc".

**📖 Ôn lại:** [Phần 2 — Góc nhìn Senior](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p2)

</details>

---

<a id="nhom-c"></a>
## C. Kafka Producer

### Q12. 🟢 Giải thích `acks=0`, `acks=1`, `acks=all`.

<details><summary>Đáp án</summary>

**Trả lời ngắn:** `0`: không chờ broker — nhanh nhất, **mất dữ liệu im lặng**. `1`: leader ghi vào log của nó là ack — leader chết trước khi follower copy → **mất**. `all` (`-1`): mọi replica trong ISR đã nhận — an toàn nhất, **phải đi kèm `min.insync.replicas ≥ 2`**. Từ Kafka 3.0 mặc định `acks=all` + idempotence.

**Giải thích chi tiết:**
- `retries` mặc định `Integer.MAX_VALUE` nhưng bị chặn bởi **`delivery.timeout.ms`** (120s) — tổng thời gian từ `send()` tới thành công/thất bại; ràng buộc `delivery.timeout.ms ≥ linger.ms + request.timeout.ms`.
- `retry.backoff.ms` 100ms, từ 3.7 có `retry.backoff.max.ms` (exponential).

**Câu hỏi nối tiếp:**
- *Ghi đè `acks=1` thì idempotence ra sao?* — Nếu không set `enable.idempotence` tường minh thì idempotence bị **tắt ngầm**; nếu set tường minh `true` thì báo lỗi config. Kiểm tra log `ProducerConfig values`.

**⚠️ Câu trả lời gây điểm trừ:**
- "`acks=all` là đủ an toàn" mà không nhắc `min.insync.replicas`.

**📖 Ôn lại:** [Phần 3 — acks, retries và delivery timeout](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p3)

</details>

### Q13. 🟡 `send()` hoạt động bên trong thế nào? `linger.ms`, `batch.size`, nén ảnh hưởng gì? Lỗi producer hay gặp?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** `send()` **bất đồng bộ**: serialize → partitioner → **RecordAccumulator** (batch theo partition, `buffer.memory` 32MiB) → **Sender thread** gửi khi batch đầy hoặc hết `linger.ms`. Buffer đầy → `send()` **block** tới `max.block.ms` (60s) rồi `TimeoutException` (backpressure). Tăng `linger.ms` (5–50ms), `batch.size` (64–256KiB) + `lz4`/`zstd` → throughput tăng mạnh, latency tăng nhẹ.

**Giải thích chi tiết:**
- `linger.ms` mặc định 5 từ Kafka 4.0 (trước đó 0).
- Partitioner: có key → `murmur2(key) % numPartitions`; không key → **sticky partitioning** (KIP-480, cải tiến KIP-794).
- Lỗi hay gặp:
  - Tạo `KafkaProducer` **mỗi message** (producer thread-safe, nên dùng chung một instance).
  - `send(...).get()` trong vòng lặp → biến thành đồng bộ, throughput giảm 10–100 lần.
  - Bỏ qua callback lỗi → mất message im lặng.
  - Làm việc blocking trong callback (chạy trên Sender thread) → nghẽn mọi `send`.
  - Không `close()`/`flush()` khi shutdown → mất batch trong accumulator.

**Câu hỏi nối tiếp:**
- *Custom partitioner rủi ro gì?* — Dễ tạo hot partition.

**⚠️ Câu trả lời gây điểm trừ:**
- "Gửi xong Kafka rồi commit DB là an toàn" — dual-write (Q47).

**📖 Ôn lại:** [Phần 3 — Kafka Producer internals](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p3)

</details>

### Q14. 🔴 Retry ở producer gây duplicate và đảo thứ tự thế nào? Idempotent producer giải quyết ra sao và giới hạn của nó?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Request 1 ghi thành công nhưng **ack mất** → retry → **duplicate**. Với `max.in.flight > 1`, batch 1 lỗi rồi retry **sau** batch 2 → **đảo thứ tự**. `enable.idempotence=true` (mặc định từ 3.0): broker cấp **Producer ID (PID)**, mỗi batch mang **sequence number** per partition; broker từ chối sequence đã thấy hoặc nhảy cóc → không duplicate, giữ thứ tự với `max.in.flight ≤ 5`. Giới hạn: chỉ trong **một phiên producer** (PID mất khi restart) và **per partition**; không chống việc **ứng dụng** gọi `send()` hai lần.

**Giải thích chi tiết:**
- Duplicate từ ứng dụng (outbox relay crash, user bấm 2 lần) vẫn phải dedup ở consumer bằng `eventId`.
- Muốn PID ổn định qua restart → `transactional.id` (Q15).

**Câu hỏi nối tiếp:**
- *Làm sao tái hiện duplicate?* — Tắt idempotence, `acks=1`, Toxiproxy gây timeout giữa producer và broker; consumer kiểm tra `seq` theo key.

**⚠️ Câu trả lời gây điểm trừ:**
- "Bật idempotence là exactly-once end-to-end".

**📖 Ôn lại:** [Phần 3 — Idempotent producer](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p3)

</details>

### Q15. 🔴 Kafka transaction hoạt động thế nào? `transactional.id`, fencing, transaction marker, LSO, `read_committed`?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Transaction cho phép **ghi nguyên tử nhiều partition/topic** và **commit offset consumer trong cùng transaction** (consume-transform-produce). **Transaction coordinator** quản lý trạng thái trong `__transaction_state`. Mỗi `transactional.id` có **epoch**; `initTransactions()` tăng epoch → instance cũ (zombie) bị **fence** (`ProducerFencedException`). Commit = coordinator ghi **marker** COMMIT/ABORT vào mọi partition liên quan. Consumer `read_committed` chỉ đọc tới **LSO** và lọc record bị abort; mặc định `read_uncommitted` **thấy cả record bị abort**.

**Giải thích chi tiết:**
```java
producer.initTransactions();
while (running) {
  var records = consumer.poll(Duration.ofMillis(500));
  if (records.isEmpty()) continue;
  producer.beginTransaction();
  try {
    Map<TopicPartition, OffsetAndMetadata> offsets = new HashMap<>();
    for (var r : records) {
      producer.send(new ProducerRecord<>("payment-results", r.key(), process(r.value())));
      offsets.put(new TopicPartition(r.topic(), r.partition()), new OffsetAndMetadata(r.offset() + 1));
    }
    producer.sendOffsetsToTransaction(offsets, consumer.groupMetadata());
    producer.commitTransaction();
  } catch (ProducerFencedException | OutOfOrderSequenceException e) {
    producer.close(); throw e;                    // zombie: không phục hồi
  } catch (KafkaException e) {
    producer.abortTransaction(); resetToLastCommitted(consumer);
  }
}
```
- Từ Kafka 2.5 (KIP-447) dùng `consumer.groupMetadata()` → không cần một `transactional.id` mỗi partition.
- Chi phí: round-trip tới coordinator + marker → gom nhiều record mỗi transaction.
- `transaction.timeout.ms` (60s): transaction treo chặn LSO → consumer `read_committed` **đứng hình**.

**Câu hỏi nối tiếp:**
- *Kafka Streams bật EOS thế nào?* — `processing.guarantee=exactly_once_v2`.

**⚠️ Câu trả lời gây điểm trừ:**
- Quên `read_committed` ở consumer rồi khẳng định exactly-once.

**📖 Ôn lại:** [Phần 3 — Transactions & EOS](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p3)

</details>

### Q16. 🟡 "Kafka có exactly-once" — câu này đúng tới đâu?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** EOS của Kafka là **effectively-once trong phạm vi Kafka → Kafka** (đọc Kafka, ghi Kafka, commit offset cùng transaction). Ngay khi có **side effect bên ngoài** (ghi DB, gọi API thanh toán, gửi email) thì transaction Kafka **không bao trùm**. Câu trả lời chuẩn: "Exactly-once *delivery* là không thể trong trường hợp tổng quát; ta đạt *exactly-once processing effect* bằng **at-least-once + idempotency**" (hoặc lưu offset cùng transaction DB).

**Giải thích chi tiết:**
- Kafka → DB: idempotent consumer (bảng dedup) hoặc lưu offset trong DB (Q21).
- Gọi API ngoài: truyền **idempotency key** sang API đó.
- Spring: `@Transactional` JPA + Kafka chỉ là best-effort 1PC (Q33).

**Câu hỏi nối tiếp:**
- *RabbitMQ có exactly-once không?* — Không; at-least-once + dedup.

**⚠️ Câu trả lời gây điểm trừ:**
- "Bật `enable.idempotence` và transaction là hết duplicate ở mọi nơi".

**📖 Ôn lại:** [Phần 3 — Góc nhìn Senior: exactly-once](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p3)

</details>

---

<a id="nhom-d"></a>
## D. Kafka Consumer

### Q17. 🟢 Consumer group hoạt động thế nào? Chuyện gì xảy ra khi số consumer lớn hơn số partition?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Trong một group, **mỗi partition gán cho tối đa một consumer** → giữ ordering per partition; một consumer có thể nhận nhiều partition. Các group khác nhau đọc độc lập, offset riêng lưu trong `__consumer_offsets`. Consumer > partition → **consumer thừa idle**, còn làm rebalance thường xuyên hơn.

**Giải thích chi tiết:**
- **Group coordinator** (một broker) quản lý membership; với classic protocol, một consumer làm **group leader** tính assignment.
- 20 pod cho topic 6 partition → 14 pod idle.
- Autoscale (KEDA) theo lag phải giới hạn tối đa = số partition.

**Câu hỏi nối tiếp:**
- *Muốn song song hơn số partition?* — Confluent Parallel Consumer (song song theo key) hoặc worker pool + quản lý offset cẩn thận (Q24).

**⚠️ Câu trả lời gây điểm trừ:**
- "Thêm consumer luôn tăng throughput".

**📖 Ôn lại:** [Phần 4 — Consumer group](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p4)

</details>

### Q18. 🔴 🎯 Tình huống: consumer gọi API ngoài 1s/record, `max.poll.records=500`. Log liên tục "member has left the group", `CommitFailedException`, cùng record bị xử lý lặp lại. Giải thích và sửa.

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Đây là **rebalance loop**. 500 × 1s = 500s > `max.poll.interval.ms` (300s) → consumer **tự rời group** → partition chuyển cho consumer khác → commit thất bại → batch được xử lý **lại** → lại chậm → lặp vô hạn. Heartbeat (background thread) vẫn chạy nên `session.timeout.ms` không phải thủ phạm. Sửa: **giảm `max.poll.records`**; hoặc xử lý trong executor + `pause()`/`resume()` partition (vẫn `poll()` để giữ membership); hoặc tăng `max.poll.interval.ms` (đơn giản nhưng chậm phát hiện consumer kẹt).

**Giải thích chi tiết:**

| Config | Mặc định | Chứng minh điều gì |
|---|---|---|
| `heartbeat.interval.ms` | 3s | Background thread gửi heartbeat |
| `session.timeout.ms` | 45s (từ 3.0) | **Process còn sống** |
| `max.poll.interval.ms` | 300s | **Vòng xử lý không bị kẹt** |
| `max.poll.records` | 500 | Số record mỗi lần poll |

- Mọi call ra ngoài phải có **timeout**; một call treo 5 phút = bị đá khỏi group.
- Xử lý song song trong executor thì phải commit đúng offset liên tục đã xong (không commit "nhảy cóc").

**Câu hỏi nối tiếp:**
- *Vì sao `pause()` không gây rebalance?* — Container vẫn gọi `poll()` (trả rỗng cho partition bị pause) nên không vượt `max.poll.interval.ms`.

**⚠️ Câu trả lời gây điểm trừ:**
- Tăng `session.timeout.ms` để "sửa".
- Scale thêm pod (vẫn lặp vì mỗi consumer vẫn chậm).

**📖 Ôn lại:** [Phần 4 — Vòng đời poll và timeout](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p4)

</details>

### Q19. 🟡 Eager rebalance và cooperative rebalance khác nhau thế nào? Static membership và KIP-848 giải quyết gì?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** **Eager** (Range, RoundRobin, Sticky cũ): mọi consumer **thu hồi tất cả** partition → stop-the-world cả group. **Cooperative** (`CooperativeStickyAssignor`): chỉ thu hồi partition **thực sự cần chuyển**, qua 2 vòng nhỏ; consumer khác vẫn chạy. **Static membership** (`group.instance.id`): consumer restart trong `session.timeout.ms` lấy lại đúng partition **không gây rebalance** — hữu ích khi rolling deploy (tên pod StatefulSet làm id). **KIP-848** (`group.protocol=consumer`, GA trong Kafka 4.0): assignment tính **phía broker**, rebalance hoàn toàn incremental.

**Giải thích chi tiết:**
- Từ Kafka 3.0 mặc định `partition.assignment.strategy=[RangeAssignor, CooperativeStickyAssignor]` để nâng cấp rolling sang cooperative.
- Rebalance xảy ra khi: consumer join/leave/crash, vượt `max.poll.interval.ms`, số partition đổi, regex subscription khớp topic mới.
```java
consumer.subscribe(List.of("orders"), new ConsumerRebalanceListener() {
  public void onPartitionsRevoked(Collection<TopicPartition> p) { consumer.commitSync(currentOffsets(p)); }
  public void onPartitionsAssigned(Collection<TopicPartition> p) { /* seek theo offset lưu ở DB nếu có */ }
  public void onPartitionsLost(Collection<TopicPartition> p) { /* đã thuộc người khác, KHÔNG commit */ }
});
```

**Câu hỏi nối tiếp:**
- *Rebalance liên tục khi deploy K8s?* — Bật static membership, graceful shutdown (`wakeup()` → commit → `close()`), cooperative assignor.

**⚠️ Câu trả lời gây điểm trừ:**
- Không biết cooperative rebalance tồn tại.

**📖 Ôn lại:** [Phần 4 — Rebalance và assignor](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p4)

</details>

### Q20. 🟡 Commit offset quyết định at-most-once/at-least-once thế nào? Auto commit nguy hiểm khi nào? `commitSync` vs `commitAsync`?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** **At-most-once**: commit **trước** xử lý (crash → mất). **At-least-once**: xử lý **rồi** commit (crash trước commit → xử lý lại → duplicate). Offset commit là offset **kế tiếp** cần đọc (`offset + 1`). Auto commit commit bên trong `poll()` cho record của lần poll **trước** — an toàn nếu xử lý đồng bộ trong vòng lặp, nhưng nếu **đẩy record sang thread khác** rồi poll tiếp thì có thể commit record chưa xong → **mất message**. `commitSync` block + retry; `commitAsync` không block, **không retry** (tránh offset cũ đè mới) → mẫu: async trong vòng lặp, sync trong `finally`/`onPartitionsRevoked`.

**Giải thích chi tiết:**
```
At-most-once:  poll → COMMIT → process
At-least-once: poll → process → COMMIT
Exactly-once:  Kafka→Kafka: transaction | Kafka→DB: offset cùng DB transaction hoặc idempotent consumer
```
- Spring Kafka mặc định `enable.auto.commit=false` và container tự commit theo AckMode.

**Câu hỏi nối tiếp:**
- *Commit `record.offset()` thay vì `+1`?* — Mỗi lần restart xử lý lại 1 record.

**⚠️ Câu trả lời gây điểm trừ:**
- Commit rồi mới ghi DB bất đồng bộ.

**📖 Ôn lại:** [Phần 4 — Commit offset và delivery semantics](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p4)

</details>

### Q21. 🔴 Làm sao đạt exactly-once khi consumer Kafka ghi vào PostgreSQL mà không cần bảng dedup?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Lưu **offset vào DB trong cùng transaction** với kết quả xử lý; `enable.auto.commit=false`, không dựa vào offset trên Kafka (chỉ commit để monitoring). Khi được gán partition (`onPartitionsAssigned`) thì **`seek`** tới offset lưu trong DB. Crash ở bất kỳ điểm nào: hoặc có cả kết quả + offset, hoặc không có gì.

**Giải thích chi tiết:**
```java
public void onPartitionsAssigned(Collection<TopicPartition> parts) {
  for (TopicPartition tp : parts)
    consumer.seek(tp, offsetRepo.findNextOffset(group, tp).orElse(0L));
}
// vòng lặp:
for (ConsumerRecord<String, String> r : records) {
  txTemplate.executeWithoutResult(s -> {
    projectionRepo.upsert(parse(r.value()));
    offsetRepo.save(group, r.topic(), r.partition(), r.offset() + 1);
  });
}
```
- Bảng `consumer_offsets(group, topic, partition, next_offset)`.
- Đánh đổi: lag monitoring bằng công cụ chuẩn không thấy offset thật (trừ khi vẫn commit lên Kafka); mọi xử lý phải nằm trong cùng DB.

**Câu hỏi nối tiếp:**
- *So với bảng dedup?* — Dedup dùng được cho mọi kiểu side effect trong DB và không cần tự quản offset; cách lưu offset hợp với projection thuần.

**⚠️ Câu trả lời gây điểm trừ:**
- "Dùng `@Transactional` của JPA trên listener là đủ".

**📖 Ôn lại:** [Phần 4 — Bài tập 4.3](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p4)

</details>

### Q22. 🟡 Poison pill là gì và xử lý ra sao?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Record consumer **không bao giờ** xử lý thành công (JSON hỏng, schema sai, vi phạm business rule, bug). Với at-least-once ngây thơ: lỗi → không commit → poll lại → lỗi → **partition kẹt vĩnh viễn**, lag tăng mãi. Hai loại: **lỗi deserialize** (xảy ra trong `poll()`, vòng lặp không bắt được) → `ErrorHandlingDeserializer` hoặc deserialize thủ công từ `byte[]`; **lỗi xử lý** → phân loại retryable (timeout, 503 → retry có backoff giới hạn) vs non-retryable (validation → **DLT ngay**).

**Giải thích chi tiết:**
- DLT phải có header lỗi, topic/partition/offset gốc để điều tra và republish.
- Dấu hiệu: lag tăng ở **một** partition, log lỗi lặp lại cùng offset.
- Không "catch mọi exception rồi log và bỏ qua" — mất message không dấu vết.

**Câu hỏi nối tiếp:**
- *Consumer đọc DLT dùng deserializer nào?* — `ByteArrayDeserializer`, vì giá trị có thể là byte gốc hỏng.

**⚠️ Câu trả lời gây điểm trừ:**
- Retry vô hạn mọi lỗi.

**📖 Ôn lại:** [Phần 4 — Poison pill](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p4) · [Phần 6 — DefaultErrorHandler](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p6)

</details>

### Q23. 🟡 🎯 Tình huống: dashboard cho thấy consumer lag tăng. Bạn chẩn đoán thế nào?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Xem **hình dạng** lag: tăng đều **mọi partition** → consumer không đủ năng lực (kiểm tra downstream DB/API latency, scale consumer ≤ số partition, tối ưu batch). Tăng ở **một partition** → hot key, poison pill (log lỗi lặp cùng offset) hoặc consumer cụ thể bị kẹt (thread dump). Răng cưa đều → bình thường với batch processing. Cảnh báo theo **time lag** hơn là số message.

**Giải thích chi tiết:**
```bash
kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group billing
# PARTITION CURRENT-OFFSET LOG-END-OFFSET LAG
# 3         8000           95000          87000  ← partition nóng hoặc consumer kẹt
```
- `lag = logEndOffset − committedOffset`.
- Kiểm tra thêm: rebalance liên tục (pod OOMKill, GC dài), hanging transaction (consumer `read_committed` đứng yên — Q57).
- Công cụ: Kafka Lag Exporter, kminion, Burrow.

**Câu hỏi nối tiếp:**
- *Group không hoạt động 7 ngày rồi start lại?* — Offset bị xóa sau `offsets.retention.minutes` → `auto.offset.reset` áp dụng → có thể mất/đọc lại toàn bộ.

**⚠️ Câu trả lời gây điểm trừ:**
- "Tăng số pod" ngay mà không xem phân bố lag theo partition.

**📖 Ôn lại:** [Phần 4 — Consumer lag](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p4) · [Phần 12 — Runbook](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p12)

</details>

### Q24. 🔴 `KafkaConsumer` có thread-safe không? Muốn xử lý song song nhiều hơn số partition và shutdown an toàn thì làm sao?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** **Không** (trừ `wakeup()`). Mô hình chuẩn: một consumer mỗi thread. Song song hơn số partition: **Confluent Parallel Consumer** (song song theo key, giữ ordering per key) hoặc worker pool + quản lý offset cẩn thận (chỉ commit offset liên tục đã xong). Shutdown: thread khác gọi `consumer.wakeup()` → `poll()` ném `WakeupException` → `commitSync` → `close()` (rời group ngay, rebalance sớm thay vì đợi session timeout).

**Giải thích chi tiết:**
- Worker pool không giữ ordering trong partition (record sau có thể xong trước).
- Virtual threads không tăng parallelism vượt số partition — giới hạn ở mô hình partition.
- `auto.offset.reset` có chủ đích: service mới cần lịch sử → `earliest`; notification realtime → `latest`.

**Câu hỏi nối tiếp:**
- *Commit offset khi xử lý song song?* — Theo dõi "low watermark" đã hoàn tất liên tục cho từng partition; commit tới đó.

**⚠️ Câu trả lời gây điểm trừ:**
- Chia sẻ một `KafkaConsumer` cho nhiều thread.

**📖 Ôn lại:** [Phần 4 — Góc nhìn Senior](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p4)

</details>

---

<a id="nhom-e"></a>
## E. Ordering, key, retention & compaction

### Q25. 🟢 Kafka đảm bảo thứ tự ở đâu? Những gì có thể phá vỡ thứ tự?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** **Chỉ trong một partition**; không có ordering toàn cục. Muốn thứ tự cho một thực thể → **cùng key**. Phá vỡ bởi: (1) producer retry khi tắt idempotence và `max.in.flight > 1`; (2) **tăng số partition**; (3) nhiều producer cùng ghi một key; (4) consumer xử lý song song record cùng partition; (5) **retry topic** (record lỗi đi đường vòng).

**Giải thích chi tiết:**
- Giữ thứ tự ở nguồn: cùng key, idempotent producer, một thread/partition, blocking retry.
- Chịu được mất thứ tự ở đích: version check, state machine (Q53).

**Câu hỏi nối tiếp:**
- *Key `null` thì sao?* — Sticky partitioner rải event cùng thực thể ra nhiều partition → mất thứ tự.

**⚠️ Câu trả lời gây điểm trừ:**
- "Kafka đảm bảo thứ tự trong topic".

**📖 Ôn lại:** [Phần 5 — Đảm bảo thứ tự](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p5)

</details>

### Q26. 🟡 🎯 Tình huống: key = `tenantId`, một tenant chiếm 40% traffic → một partition lag nặng. Bạn đổi thiết kế thế nào?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Nguyên tắc: **key = đơn vị nhỏ nhất cần ordering** (thường là aggregate id). Nếu ordering chỉ cần theo `accountId` → key = `tenantId:accountId` → tải phân phối theo account. Hoặc tách **topic riêng** cho tenant lớn với nhiều partition. Nếu bắt buộc ordering cho thực thể lớn → tách key phụ (`tenantId#shard`) và xử lý thứ tự ở consumer bằng version.

**Giải thích chi tiết:**

| Key | Ordering | Phân phối |
|---|---|---|
| `null` | Không | Đều |
| `orderId` | Theo đơn | Rất đều |
| `customerId` | Theo khách | Khách B2B lớn → hot |
| `tenantId` | Theo tenant | Kém |
| `country` | Theo nước | Rất kém |

- Tránh key ngẫu nhiên (mất ordering hoàn toàn).
- Đổi key trên topic đang chạy → thời gian chuyển tiếp event cũ/mới của cùng thực thể ở partition khác → cần drain hoặc version check.

**Câu hỏi nối tiếp:**
- *Có thể chỉ tăng partition?* — Không giải quyết: một key vẫn chỉ vào một partition.

**⚠️ Câu trả lời gây điểm trừ:**
- "Thêm consumer" hoặc "tăng partition".

**📖 Ôn lại:** [Phần 5 — Chọn key](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p5)

</details>

### Q27. 🟡 Retention hoạt động thế nào? Consumer chết lâu hơn retention thì sao?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** `cleanup.policy=delete`: xóa **cả segment** cũ theo `retention.ms` (mặc định 7 ngày) và/hoặc `retention.bytes` (mỗi partition) → dữ liệu có thể sống lâu hơn một chút. Consumer chết lâu hơn retention → offset trỏ vào dữ liệu đã xóa → `auto.offset.reset` áp dụng → **mất dữ liệu im lặng** nếu là `latest`.

**Giải thích chi tiết:**
- Đặt retention theo: thời gian tối đa consumer có thể chậm/chết + nhu cầu replay + dung lượng đĩa.
- **Tiered storage** (KIP-405, GA từ 3.9): segment cũ xuống object storage → retention dài chi phí thấp.
- Đặt `retention.ms=1h` "cho tiết kiệm" trong khi consumer batch chạy mỗi ngày là lỗi kinh điển.

**Câu hỏi nối tiếp:**
- *Offset của group cũng có retention?* — `offsets.retention.minutes` (7 ngày) khi group không hoạt động.

**⚠️ Câu trả lời gây điểm trừ:**
- "Kafka xóa message khi consumer đọc xong".

**📖 Ôn lại:** [Phần 5 — Retention](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p5)

</details>

### Q28. 🔴 Log compaction là gì? Tombstone? Dùng cho gì và không dùng cho gì?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** `cleanup.policy=compact`: Kafka đảm bảo giữ **ít nhất bản ghi mới nhất cho mỗi key**; bản cũ bị log cleaner dọn dần; offset giữ nguyên. **Tombstone** = `value = null` đánh dấu xóa key, bị xóa hẳn sau `delete.retention.ms` (24h). Dùng cho changelog/snapshot trạng thái ("profile mới nhất của mỗi customer"), `__consumer_offsets`, KTable, **event-carried state transfer**. **Không** dùng cho dòng sự kiện mà mỗi event đều quan trọng (giao dịch tài khoản) — consumer không đảm bảo thấy mọi phiên bản trung gian.

**Giải thích chi tiết:**
```
Trước:  k1=A k2=B k1=C k3=D k2=null k1=E   (offset 0..5)
Sau:                   k3=D k2=null k1=E   (offset 3,4,5)
```
- Active segment không bao giờ bị compact; điều khiển bằng `min.cleanable.dirty.ratio`, `min.compaction.lag.ms`.
- Gửi record **không key** vào topic compacted → broker từ chối.
- Có thể `compact,delete`.
- Topic compacted lâu năm → schema compatibility nên `*_TRANSITIVE`.

**Câu hỏi nối tiếp:**
- *Service mới cần bản sao dữ liệu customer?* — Đọc topic compacted từ đầu để dựng local store, không gọi API.

**⚠️ Câu trả lời gây điểm trừ:**
- "Compaction xóa message trùng lặp".

**📖 Ôn lại:** [Phần 5 — Log compaction](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p5)

</details>

---

<a id="nhom-f"></a>
## F. Spring for Apache Kafka

### Q29. 🟢 `@KafkaListener(concurrency = "3")` tạo ra bao nhiêu consumer/thread? Bao nhiêu partition là đủ khi chạy 4 pod?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** `ConcurrentMessageListenerContainer` với concurrency 3 tạo **3 `KafkaMessageListenerContainer`**, mỗi cái **một `KafkaConsumer` + một thread**. 4 pod × 3 = **12 consumer** trong group → cần **≥12 partition**, nếu không một số consumer idle.

**Giải thích chi tiết:**
```java
@KafkaListener(topics = "orders", groupId = "inventory-service", concurrency = "3")
public void onOrderPlaced(@Payload OrderPlaced evt,
                          @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                          @Header(KafkaHeaders.OFFSET) long offset) {
  inventoryService.reserve(evt);     // ném exception → error handler xử lý
}
```
- Batch listener (`batch = "true"`) tăng throughput khi ghi DB theo lô.
- `KafkaTemplate.send()` từ Spring Kafka 3.0 trả `CompletableFuture` (trước đó `ListenableFuture`).
- Bật observation (`spring.kafka.template/listener.observation-enabled=true`) để trace qua header.

**Câu hỏi nối tiếp:**
- *Quản lý topic thế nào trên production?* — Thường tắt auto-create phía broker, quản lý bằng GitOps (Terraform, Strimzi `KafkaTopic`) thay vì `NewTopic` bean.

**⚠️ Câu trả lời gây điểm trừ:**
- Nghĩ concurrency là số thread xử lý **trong** một consumer.

**📖 Ôn lại:** [Phần 6 — @KafkaListener và concurrency](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p6)

</details>

### Q30. 🟢 Các AckMode trong Spring Kafka?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Khi `enable.auto.commit=false` (mặc định của Spring): `RECORD` — commit sau mỗi record; **`BATCH` (mặc định)** — sau khi xử lý hết record của một lần `poll()`; `TIME`/`COUNT`/`COUNT_TIME`; `MANUAL` — khi gọi `ack.acknowledge()`, gom commit như BATCH; `MANUAL_IMMEDIATE` — commit ngay khi `acknowledge()`.

**Giải thích chi tiết:**
```java
@KafkaListener(topics = "payments", groupId = "ledger", containerFactory = "manualAckFactory")
public void onPayment(PaymentEvent evt, Acknowledgment ack) {
  ledger.apply(evt);
  ack.acknowledge();
}
```
- BATCH: crash giữa batch → xử lý lại cả phần đã xong của batch → listener phải idempotent.
- RECORD: ít xử lý lại hơn nhưng nhiều commit hơn (chậm hơn).

**Câu hỏi nối tiếp:**
- *Gọi `acknowledge()` từ thread khác?* — Với MANUAL được queue lại cho consumer thread; tránh vì dễ commit nhảy cóc.

**⚠️ Câu trả lời gây điểm trừ:**
- "Spring Kafka mặc định auto commit mỗi 5s".

**📖 Ôn lại:** [Phần 6 — AckMode](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p6)

</details>

### Q31. 🟡 Cấu hình xử lý lỗi listener với `DefaultErrorHandler` + DLT như thế nào? Mặc định nó làm gì?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Từ Spring Kafka 2.8, `DefaultErrorHandler` thay `SeekToCurrentErrorHandler`. Listener ném exception → handler **seek** về offset record lỗi để poll lại → retry theo `BackOff`; hết lượt → **recoverer** (ví dụ `DeadLetterPublishingRecoverer` → topic `.DLT`) rồi commit để đi tiếp. **Mặc định** `FixedBackOff(0, 9)` → tối đa **10 lần giao** rồi **log và bỏ qua**. Lỗi không thể khỏi bằng retry → `addNotRetryableExceptions` để vào DLT ngay.

**Giải thích chi tiết:**
```java
@Bean
DefaultErrorHandler errorHandler(KafkaTemplate<Object, Object> template) {
  var recoverer = new DeadLetterPublishingRecoverer(template,
      (rec, ex) -> new TopicPartition(rec.topic() + ".DLT", rec.partition()));
  var backOff = new ExponentialBackOffWithMaxRetries(4);
  backOff.setInitialInterval(500); backOff.setMultiplier(2.0); backOff.setMaxInterval(5_000);
  var h = new DefaultErrorHandler(recoverer, backOff);
  h.addNotRetryableExceptions(ValidationException.class, JsonProcessingException.class);
  return h;      // Boot tự gắn CommonErrorHandler duy nhất vào container factory
}
```
- Deserialize lỗi: `ErrorHandlingDeserializer` bọc delegate → `DeserializationException` thuộc danh sách không retry → thẳng DLT.
- Record DLT có header `kafka_dlt-exception-message`, `-stacktrace`, `-original-topic/partition/offset`.
- DLT nên có **≥ số partition** topic gốc (recoverer giữ nguyên partition).
- `spring.json.trusted.packages` liệt kê cụ thể; `*` có rủi ro deserialization gadget; header `__TypeId__` là FQCN → coupling package giữa producer/consumer.

**Câu hỏi nối tiếp:**
- *Tổng backoff nên bao nhiêu?* — Nhỏ hơn nhiều so với `max.poll.interval.ms`; blocking retry chặn partition suốt thời gian đó.

**⚠️ Câu trả lời gây điểm trừ:**
- Không cấu hình `ErrorHandlingDeserializer` → JSON hỏng làm container log lỗi vô hạn.

**📖 Ôn lại:** [Phần 6 — DefaultErrorHandler và DLT](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p6)

</details>

### Q32. 🔴 Blocking retry và non-blocking retry (`@RetryableTopic`) khác nhau thế nào? Cho ví dụ bug khi chọn sai.

<details><summary>Đáp án</summary>

**Trả lời ngắn:** **Blocking** (`DefaultErrorHandler`): retry tại chỗ, **giữ ordering** nhưng **chặn partition** suốt backoff. **Non-blocking** (`@RetryableTopic`): record lỗi được **publish sang retry topic** (delay tăng dần) rồi DLT; consumer retry topic **pause partition** tới hạn → partition gốc không bị chặn, nhưng **mất ordering**. Bug: topic `order-status` dùng `@RetryableTopic` → `PAID` lỗi đi retry 3s, `SHIPPED` thành công ngay → 3s sau `PAID` ghi đè `SHIPPED`.

**Giải thích chi tiết:**
```java
@RetryableTopic(attempts = "4",
    backoff = @Backoff(delay = 1_000, multiplier = 3.0, maxDelay = 30_000),
    exclude = { ValidationException.class })
@KafkaListener(topics = "notifications", groupId = "email-service")
public void send(NotificationEvent evt) { emailClient.send(evt); }

@DltHandler
public void dlt(NotificationEvent evt, @Header(KafkaHeaders.EXCEPTION_MESSAGE) String err) { alerting.raise(evt, err); }
```

| | Blocking | Non-blocking |
|---|---|---|
| Ordering | Giữ | Mất |
| Chặn partition | Có | Không |
| Phù hợp | Ledger, state machine | Email, webhook (event độc lập, lỗi ngoài kéo dài) |

- Khắc phục bug: version check ở consumer (`event.version > current.version`) hoặc quay lại blocking retry.

**Câu hỏi nối tiếp:**
- *Retry topic chờ bằng cách nào?* — Header thời điểm đến hạn + pause/resume partition, không `sleep`.

**⚠️ Câu trả lời gây điểm trừ:**
- "Non-blocking luôn tốt hơn vì không chặn".

**📖 Ôn lại:** [Phần 6 — Non-blocking retry](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p6)

</details>

### Q33. 🔴 Listener có `@Transactional` (JPA) và gửi Kafka bên trong. DB và Kafka có nguyên tử không?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** **Không**. Kết hợp `JpaTransactionManager` với `KafkaTransactionManager` chỉ là **best-effort 1PC**: commit cái này rồi cái kia; crash giữa hai bước → lệch. Spring đã deprecate `ChainedKafkaTransactionManager`. Kafka không hỗ trợ XA. Muốn DB + event nhất quán → **Transactional Outbox**; phía consumer → **idempotent consumer**.

**Giải thích chi tiết:**
```yaml
spring.kafka.producer.transaction-id-prefix: order-svc-tx-   # bật transactional producer
spring.kafka.consumer.isolation-level: read_committed
```
- Khi container có `KafkaTransactionManager`, Spring tự gửi offset vào transaction (consume-process-produce EOS trong Kafka).
- Thứ tự commit quyết định kiểu lỗi: commit DB trước → crash → event mất (nhưng offset chưa commit → xử lý lại → cần idempotent); commit Kafka trước → event "ma".

**Câu hỏi nối tiếp:**
- *`TransactionSynchronization.afterCommit` gửi Kafka có ổn không?* — Vẫn mất nếu crash giữa commit và send.

**⚠️ Câu trả lời gây điểm trừ:**
- "Có `@Transactional` bao quanh nên DB và Kafka cùng commit".

**📖 Ôn lại:** [Phần 6 — Transactions trong Spring Kafka](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p6) · [Phần 10 — Outbox](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p10)

</details>

---

<a id="nhom-g"></a>
## G. Schema evolution

### Q34. 🟢 Vì sao cần Schema Registry? Record Avro trên Kafka trông thế nào?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Event là **hợp đồng giữa các team**; producer đổi tên field là mọi consumer vỡ, và dữ liệu cũ vẫn nằm trong topic. Cần định dạng có schema, quy tắc tương thích và nơi lưu tập trung để **kiểm tra trước khi deploy**. Wire format: `[magic byte 0][schema ID 4 byte][Avro binary]`; consumer lấy **writer schema** theo ID + **reader schema** của code → Avro resolution.

**Giải thích chi tiết:**
- Subject mặc định `TopicNameStrategy` → `<topic>-value`.
- Production: `auto.register.schemas=false`, đăng ký qua CI/CD; kiểm tra compatibility trong CI (Maven plugin `test-compatibility`).

| Định dạng | Ưu | Nhược |
|---|---|---|
| JSON không schema | Dễ đọc | Không kiểm soát tương thích |
| Avro | Gọn, evolution tốt | Cần schema để đọc |
| Protobuf | Gọn, đa ngôn ngữ | Kỷ luật đánh số field |

**Câu hỏi nối tiếp:**
- *Dùng Java serialization cho message?* — Coupling vào class Java + lỗ hổng bảo mật.

**⚠️ Câu trả lời gây điểm trừ:**
- "JSON là đủ, các team tự thống nhất qua Confluence".

**📖 Ôn lại:** [Phần 7 — Schema Registry](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p7)

</details>

### Q35. 🟡 BACKWARD, FORWARD, FULL compatibility khác nhau thế nào? Nâng cấp producer hay consumer trước?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** **BACKWARD** (mặc định): schema mới đọc được dữ liệu ghi bằng schema cũ → nâng **consumer trước**; an toàn: xóa field, thêm field **có default**. **FORWARD**: schema cũ đọc được dữ liệu mới → nâng **producer trước**; an toàn: thêm field, xóa field có default. **FULL**: cả hai → thứ tự bất kỳ; chỉ thêm/xóa field có default. `*_TRANSITIVE`: so với **mọi** version trước.

**Giải thích chi tiết:**

| Thay đổi | BACKWARD | FORWARD | FULL |
|---|---|---|---|
| Thêm field có default | ✅ | ✅ | ✅ |
| Thêm field không default | ❌ | ✅ | ❌ |
| Xóa field có default | ✅ | ✅ | ✅ |
| Xóa field không default | ✅ | ❌ | ❌ |
| `int → long` | ✅ (promotion) | ❌ | ❌ |
| Đổi tên | ❌ (trừ alias) | ❌ | ❌ |

- Topic retention dài/compacted → `BACKWARD_TRANSITIVE` hoặc `FULL_TRANSITIVE` vì consumer mới phải đọc mọi version lịch sử.

**Câu hỏi nối tiếp:**
- *Registry đặt `NONE`?* — Deploy được breaking change, consumer vỡ lúc runtime.

**⚠️ Câu trả lời gây điểm trừ:**
- Nhầm hướng BACKWARD/FORWARD và thứ tự nâng cấp.

**📖 Ôn lại:** [Phần 7 — Các mức compatibility](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p7)

</details>

### Q36. 🔴 🎯 Tình huống: đổi `amount` (long, VND) thành `money {amount: decimal, currency}` trên topic có 5 team consumer, retention 30 ngày, không downtime.

<details><summary>Đáp án</summary>

**Trả lời ngắn:** **Expand–contract**: (1) v2 thêm `money` optional (default null), producer **ghi cả hai**; (2) các consumer chuyển sang đọc `money` (fallback `amount` nếu null); (3) xác minh không còn ai đọc field cũ (code search, version consumer đã deploy); (4) chờ **hết retention 30 ngày** để dữ liệu chỉ còn bản có `money`; (5) v3 bỏ `amount` (cần `amount` có default để tương thích). Mỗi bước đều tương thích → rollback = lùi version producer.

**Giải thích chi tiết:**
- Thay đổi phá vỡ thật sự (đổi kiểu, đổi ngữ nghĩa) mà không expand-contract được → **topic mới** `orders.v2`, chạy song song, bridge chuyển đổi.
- Đổi tên đơn giản: thêm field mới + `aliases` phía reader.
- Topic compacted không có "hết retention" → cần republish bản mới cho mọi key hoặc giữ field cũ lâu dài.

**Câu hỏi nối tiếp:**
- *Metric nào chứng minh sẵn sàng bước contract?* — Mọi consumer group đã chạy version ≥ X (label deploy), không còn log fallback dùng `amount`.

**⚠️ Câu trả lời gây điểm trừ:**
- "Thông báo các team rồi deploy cùng lúc".

**📖 Ôn lại:** [Phần 7 — Schema evolution](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p7)

</details>

---

<a id="nhom-h"></a>
## H. RabbitMQ

### Q37. 🟢 Mô tả mô hình AMQP 0-9-1 và các loại exchange.

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Publisher gửi vào **exchange** kèm **routing key**; **binding** nối exchange với **queue**; broker **push** message tới consumer theo prefetch. **Connection** (TCP, đắt) chứa nhiều **channel** (nhẹ, không thread-safe). Exchange: **direct** (routing key == binding key), **topic** (mẫu `*` = đúng một từ, `#` = 0+ từ), **fanout** (copy tới mọi queue), **headers** (so header, `x-match=all/any`). Default exchange `""` bind mọi queue theo tên.

**Giải thích chi tiết:**
- `order.*.vn` khớp `order.created.vn`; `order.#` khớp `order`, `order.created.vn.hcm`.
- `payment.*` không khớp `payment` (cần đúng 2 từ).
```java
@Bean Queue emailQueue() {
  return QueueBuilder.durable("email.order-created").quorum()
      .deadLetterExchange("order.dlx").deadLetterRoutingKey("email.order-created.dlq")
      .deliveryLimit(5).build();
}
```

**Câu hỏi nối tiếp:**
- *Mở connection mỗi message?* — Sai; dùng `CachingConnectionFactory`.

**⚠️ Câu trả lời gây điểm trừ:**
- "Publisher gửi thẳng vào queue".

**📖 Ôn lại:** [Phần 8 — Mô hình AMQP & exchange](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p8)

</details>

### Q38. 🟢 Ack/nack/requeue và prefetch trong RabbitMQ hoạt động thế nào?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** **Auto ack**: broker coi đã giao ngay khi gửi → at-most-once, crash là mất. **Manual ack**: `basic.ack`; `basic.nack/reject` với `requeue=true` → quay lại queue (dễ vòng lặp nóng với poison message), `requeue=false` → **dead-letter** (nếu có DLX) hoặc bỏ. Mất connection trước khi ack → message **requeue** (`redelivered=true`) → at-least-once → cần idempotency. **Prefetch** (`basic.qos`) = số message chưa ack tối đa mỗi consumer: quá nhỏ → throughput thấp; quá lớn → phân phối lệch, tốn RAM, crash là giao lại nhiều. Spring AMQP mặc định 250.

**Giải thích chi tiết:**
- Thường đặt prefetch 10–300 tùy thời gian xử lý.
- Theo dõi `messages_unacknowledged` tăng bất thường = consumer treo.

**Câu hỏi nối tiếp:**
- *Auto ack + xử lý lâu?* — Crash mất message.

**⚠️ Câu trả lời gây điểm trừ:**
- Không biết requeue gây giao lại vô hạn.

**📖 Ôn lại:** [Phần 8 — Ack, nack, prefetch](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p8)

</details>

### Q39. 🟡 Khi nào message bị dead-letter? Làm retry có delay bằng TTL + DLX thế nào?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Dead-letter khi: `reject/nack` với `requeue=false`; hết **TTL** (per-message hoặc per-queue); vượt `x-max-length` (`drop-head`); quorum queue vượt `delivery-limit`. Retry có delay: nack → DLX "retry" → queue `retry.5s` (`x-message-ttl=5000`, `x-dead-letter-exchange=work`) → hết TTL tự quay về queue chính; đếm số lần qua header `x-death`; vượt N → **parking-lot** queue.

**Giải thích chi tiết:**
```
work.queue ──nack(requeue=false)──► DLX "retry" ──► retry.5s (TTL 5s, DLX = "work")
      ▲                                                       │
      └──────────────── hết TTL, dead-letter quay về ─────────┘
```
- Dùng **TTL per-queue** cho mỗi mức delay vì TTL per-message chỉ được kiểm tra ở **đầu queue** (classic) → message TTL dài chặn message TTL ngắn phía sau.
- Plugin `delayed_message_exchange` là lựa chọn khác nhưng hạn chế về mở rộng/độ bền.

**Câu hỏi nối tiếp:**
- *Đọc số lần retry trong Spring?* — `messageProperties.getXDeathHeader()` cộng `count`.

**⚠️ Câu trả lời gây điểm trừ:**
- Retry bằng `Thread.sleep` trong listener.

**📖 Ôn lại:** [Phần 8 — DLX và TTL](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p8)

</details>

### Q40. 🔴 Làm sao để RabbitMQ "không mất message"? Publisher confirms, mandatory, quorum queue.

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Cần **đủ ba phía**: (1) **Broker**: queue durable + message persistent; dữ liệu quan trọng dùng **quorum queue** (Raft, cần đa số node; classic mirrored queue đã bị loại trong RabbitMQ 4.0). (2) **Publisher**: **publisher confirms** (broker ack khi đã nhận trách nhiệm), **mandatory + return** cho message không route được; republish khi nack/timeout. (3) **Consumer**: manual ack sau khi xử lý xong + idempotency (republish có thể tạo duplicate).

**Giải thích chi tiết:**
```yaml
spring:
  rabbitmq:
    publisher-confirm-type: correlated
    publisher-returns: true
    template.mandatory: true
```
```java
rabbit.setConfirmCallback((corr, ack, cause) -> { if (!ack) log.error("NACK {} {}", corr.getId(), cause); });
rabbit.setReturnsCallback(r -> log.error("Unroutable rk={}", r.getRoutingKey()));
rabbit.convertAndSend("order.events", "order.created.vn", evt, m -> {
  m.getMessageProperties().setMessageId(evt.eventId());          // dedup phía consumer
  m.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
  return m;
}, new CorrelationData(evt.eventId()));
```
- Quorum queue 4.0 có `delivery-limit` mặc định 20; không hợp cho queue tạm/exclusive.
- Publish vào exchange không có binding mà không bật mandatory → message biến mất.

**Câu hỏi nối tiếp:**
- *Có confirms rồi sao vẫn duplicate?* — Broker đã lưu nhưng confirm không về → publisher republish.

**⚠️ Câu trả lời gây điểm trừ:**
- "Queue durable là đủ" mà không có confirms.

**📖 Ôn lại:** [Phần 8 — Durability](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p8)

</details>

### Q41. 🟡 🎯 Tình huống: một message lỗi làm consumer Spring AMQP CPU 100%, log ngập cùng một stacktrace. Nguyên nhân và sửa?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Spring AMQP mặc định `defaultRequeueRejected=true`: exception trong listener → message **requeue** → giao lại ngay → lỗi → **vòng lặp vô hạn**. Sửa: `default-requeue-rejected=false` + DLX; ném `AmqpRejectAndDontRequeueException` cho lỗi không thể retry; bật retry stateless có giới hạn; hoặc quorum queue `delivery-limit`.

**Giải thích chi tiết:**
```yaml
spring.rabbitmq.listener.simple:
  acknowledge-mode: auto            # ack khi return, nack khi exception
  prefetch: 50
  default-requeue-rejected: false   # exception → không requeue → DLX
  retry:
    enabled: true
    max-attempts: 3
    initial-interval: 1s
    multiplier: 2.0
```
- Retry stateless trong bộ nhớ chặn consumer thread trong thời gian backoff → giữ ngắn; delay dài dùng TTL + DLX (Q39).

**Câu hỏi nối tiếp:**
- *Metric nào phát hiện sớm?* — Redeliver rate cao.

**⚠️ Câu trả lời gây điểm trừ:**
- "Restart consumer" — message vẫn nằm đầu queue.

**📖 Ôn lại:** [Phần 8 — Góc nhìn Senior](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p8)

</details>

### Q42. 🟡 RabbitMQ giữ thứ tự khi nào? Vì sao RabbitMQ "thích" queue ngắn?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Một queue + **một consumer** → giữ thứ tự; nhiều consumer hoặc requeue → mất thứ tự. Cần ordering theo key → **consistent hash exchange** chia nhiều queue, mỗi queue **Single Active Consumer** (`x-single-active-consumer`). RabbitMQ tối ưu cho **queue ngắn**: queue hàng triệu message tốn RAM/đĩa, kích hoạt **memory/disk alarm** → broker **block mọi publisher** (flow control) — khác triết lý Kafka (log dài là bình thường).

**Giải thích chi tiết:**
- Backpressure: `x-max-length` + `overflow=reject-publish` để publisher nhận nack sớm.
- Queue mồ côi của consumer đã gỡ bỏ phình mãi → đặt TTL/max-length, dọn định kỳ.

**Câu hỏi nối tiếp:**
- *RabbitMQ Streams?* — Từ 3.9: log append-only, replay, đọc không xóa — tiến gần mô hình Kafka.

**⚠️ Câu trả lời gây điểm trừ:**
- "RabbitMQ luôn FIFO nên luôn đúng thứ tự".

**📖 Ôn lại:** [Phần 8 — Góc nhìn Senior](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p8)

</details>

---

<a id="nhom-i"></a>
## I. Kafka vs RabbitMQ

### Q43. 🟡 Khi nào chọn Kafka, khi nào RabbitMQ?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** **Kafka** khi cần replay, nhiều consumer đọc lại lịch sử, event sourcing/CDC/analytics, throughput rất cao, retention dài, stream processing, exactly-once Kafka→Kafka. **RabbitMQ** khi cần routing linh hoạt (direct/topic/headers), priority, per-message TTL, delayed message, **task queue** competing consumers, ack từng message, DLX có sẵn, vận hành nhẹ ở quy mô vừa. Nhiều công ty dùng **cả hai**: Kafka làm event backbone, RabbitMQ/SQS làm task queue nội bộ.

**Giải thích chi tiết:**

| Tiêu chí | Kafka | RabbitMQ |
|---|---|---|
| Mô hình | Log phân vùng, consumer **pull** | Smart broker, **push** |
| Sau khi tiêu thụ | Giữ (replay) | Xóa khi ack |
| Scale consumer | Giới hạn bởi partition | Thêm consumer vào queue |
| Ack | Offset tích lũy | Từng message |
| Retry/DLQ | Tự xây (retry topic, DLT) | DLX, TTL, delivery-limit sẵn |
| Exactly-once | Kafka→Kafka | Không |

**Câu hỏi nối tiếp:**
- *Câu trả lời phỏng vấn tốt có dạng gì?* — "Với yêu cầu A, B, C chọn X vì..., chấp nhận đánh đổi..." chứ không phải "X tốt hơn Y".

**⚠️ Câu trả lời gây điểm trừ:**
- "Kafka nhanh hơn nên luôn dùng Kafka".

**📖 Ôn lại:** [Phần 9 — Bảng quyết định](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p9)

</details>

### Q44. 🟢 🎯 Tình huống: chọn công nghệ cho (a) xuất hóa đơn PDF, (b) CDC MySQL → data lake, (c) audit log giữ 1 năm, (d) giao việc cho 50 worker có ưu tiên VIP, (e) event sourcing ví.

<details><summary>Đáp án</summary>

**Trả lời ngắn:** (a) **RabbitMQ** — task queue, ack từng việc. (b) **Kafka** + Debezium/Kafka Connect. (c) **Kafka** (retention dài, tiered storage) hoặc đẩy tiếp vào object storage. (d) **RabbitMQ** — priority queue (`x-max-priority`), competing consumers. (e) Event store thường là **DB** (PostgreSQL/EventStoreDB) với optimistic concurrency theo aggregate; **Kafka để phân phối** event.

**Giải thích chi tiết:**
- Mỗi lựa chọn nêu ≥2 lý do: replay, throughput, routing, ack model, hệ sinh thái connector.

**Câu hỏi nối tiếp:**
- *Vì sao Kafka không hợp làm event store chính?* — Không đọc hiệu quả "mọi event của aggregate X", không có optimistic concurrency theo aggregate (Q51).

**⚠️ Câu trả lời gây điểm trừ:**
- Một công nghệ cho cả năm trường hợp không lý do.

**📖 Ôn lại:** [Phần 9 — Bài tập 9.1](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p9)

</details>

---

<a id="nhom-j"></a>
## J. Pattern event-driven: idempotency, outbox, inbox, event sourcing, saga

### Q45. 🟢 Idempotent consumer là gì? Có những cách nào?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Mọi broker thực tế giao **at-least-once** → consumer phải cho kết quả như một lần dù nhận nhiều lần. Ba cách: (1) **thao tác tự nhiên idempotent** — upsert theo khóa, `SET status='PAID'` (không phải `balance = balance + x`); (2) **bảng dedup** `processed_message` trong **cùng transaction** với thay đổi nghiệp vụ; (3) **version/sequence check** — chỉ áp dụng nếu version mới hơn.

**Giải thích chi tiết:**
- Quy tắc vàng: thiết kế consumer như thể message đến **ít nhất một lần, có thể nhiều lần, có thể không theo thứ tự**.
- Nguồn duplicate: producer retry, outbox relay crash, consumer crash trước commit, rebalance, replay, RabbitMQ requeue.

**Câu hỏi nối tiếp:**
- *Side effect ngoài DB (gọi PSP)?* — Truyền idempotency key sang API đó.

**⚠️ Câu trả lời gây điểm trừ:**
- "Dùng exactly-once của Kafka nên không cần idempotent".

**📖 Ôn lại:** [Phần 10 — Idempotent consumer](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p10) · [Phần 11 — Nguồn gốc duplicate](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p11)

</details>

### Q46. 🟡 Viết idempotent consumer với bảng dedup. Vì sao "SELECT rồi INSERT" sai? Dùng id nào làm khóa?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** `INSERT ... ON CONFLICT DO NOTHING` (PostgreSQL) / `INSERT IGNORE` (MySQL) dựa vào **unique constraint**, kiểm tra số dòng ảnh hưởng; 0 dòng → duplicate → bỏ qua. Toàn bộ trong **cùng DB transaction** với thay đổi nghiệp vụ. "SELECT rồi INSERT" có **race**: hai consumer (sau rebalance) cùng thấy "chưa có". Khóa = **`eventId` ổn định do producer sinh**, không phải offset (offset đổi khi republish).

**Giải thích chi tiết:**
```java
@KafkaListener(topics = "payments", groupId = "ledger")
@Transactional
public void on(PaymentCaptured evt) {
  int inserted = jdbc.update("""
      INSERT INTO processed_message(consumer_group, message_id)
      VALUES ('ledger', ?) ON CONFLICT DO NOTHING""", evt.eventId());
  if (inserted == 0) return;                       // duplicate
  accounts.credit(evt.accountId(), evt.amount());  // đúng 1 lần mỗi eventId
}
```
- PK `(consumer_group, message_id)`; dọn bản ghi cũ hơn retention topic + biên an toàn.
- Isolation `READ COMMITTED` đủ vì unique constraint chặn.

**Câu hỏi nối tiếp:**
- *Test thế nào?* — Gửi cùng event 5 lần, 2 lần đồng thời từ 2 thread (`CountDownLatch`); số dư chỉ tăng một lần.

**⚠️ Câu trả lời gây điểm trừ:**
- Dedup bằng cache in-memory trong JVM.

**📖 Ôn lại:** [Phần 10 — Idempotent consumer](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p10)

</details>

### Q47. 🔴 Dual-write problem là gì? Transactional Outbox giải quyết thế nào?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Ghi DB và gửi Kafka trong cùng method: Kafka thành công nhưng DB commit fail → **event ma**; DB commit OK nhưng send lỗi/crash → **mất event**; gửi trong `afterCommit` vẫn mất nếu crash giữa hai bước. Không có transaction chung DB–Kafka (Kafka không hỗ trợ XA). **Outbox**: ghi event vào bảng `outbox` **trong cùng DB transaction** với dữ liệu nghiệp vụ; tiến trình riêng (polling hoặc CDC) chuyển outbox → broker; consumer idempotent vì có thể duplicate.

**Giải thích chi tiết:**
```java
@Transactional
public Order place(PlaceOrderCommand cmd) {
  Order order = orders.save(Order.create(cmd));
  OrderPlaced evt = new OrderPlaced(UUID.randomUUID().toString(), order.getId(), cmd.customerId(), order.getTotal(), Instant.now());
  outbox.save(new OutboxEvent(UUID.fromString(evt.eventId()), "Order", order.getId(), "OrderPlaced", json.valueToTree(evt)));
  return order;                     // commit cả hai, hoặc không gì
}
```
- Bảng outbox: `id` (= eventId), `aggregatetype` (route topic), `aggregateid` (Kafka key → ordering), `type`, `payload`, `created_at`.
- Phải nói được **từng điểm crash** và vì sao vẫn đúng: crash trước commit → không có gì; sau commit trước relay → relay gửi sau; relay gửi xong chưa đánh dấu → gửi lại → consumer dedup.

**Câu hỏi nối tiếp:**
- *Listen-to-yourself (gửi Kafka rồi chính service consume để ghi DB)?* — Là cách khác, nhưng đọc-sau-ghi không nhất quán ngay.

**⚠️ Câu trả lời gây điểm trừ:**
- "Gửi Kafka trong `@Transactional`, lỗi thì rollback".

**📖 Ôn lại:** [Phần 10 — Dual-write và Transactional Outbox](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p10)

</details>

### Q48. 🔴 Polling publisher vs CDC (Debezium) cho outbox: trade-off và pitfall?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** **Polling**: `@Scheduled` + `SELECT ... FOR UPDATE SKIP LOCKED`, gửi và chờ ack, rồi DELETE/đánh dấu — không thêm hạ tầng, độ trễ theo chu kỳ, bảng outbox có thể phình; nhiều relay song song có thể **đảo thứ tự** giữa event cùng aggregate. **CDC**: Debezium đọc WAL/binlog, **Outbox Event Router SMT** route theo `aggregatetype`, key = `aggregateid` — gần realtime, không query thêm, nhưng thêm Kafka Connect và pitfall **replication slot**: connector dừng → slot giữ WAL → **đầy đĩa DB primary**.

**Giải thích chi tiết:**
```java
@Scheduled(fixedDelay = 500)
@Transactional
public void relay() {
  List<OutboxEvent> batch = jdbc.query(
      "SELECT * FROM outbox ORDER BY created_at LIMIT 100 FOR UPDATE SKIP LOCKED", mapper);
  for (OutboxEvent e : batch) kafkaTemplate.send("order.events", e.aggregateId(), e.payload().toString()).join();
  jdbc.batchUpdate("DELETE FROM outbox WHERE id = ?", batch.stream().map(e -> new Object[]{e.id()}).toList());
}
```
- Giữ thứ tự với polling: một relay active (ShedLock/leader election) hoặc phân mảnh theo `aggregateid`.
- Với CDC có thể **xóa row outbox ngay trong cùng transaction** — Debezium vẫn bắt INSERT từ WAL → bảng không phình.
- Giám sát `pg_replication_slots` (lag byte), đặt `max_slot_wal_keep_size` (PostgreSQL 13+).

**Câu hỏi nối tiếp:**
- *PostgreSQL cần cấu hình gì cho Debezium?* — `wal_level=logical`, plugin `pgoutput`.

**⚠️ Câu trả lời gây điểm trừ:**
- Không biết rủi ro replication slot đầy đĩa.

**📖 Ôn lại:** [Phần 10 — Transactional Outbox](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p10)

</details>

### Q49. 🟡 Inbox pattern là gì, khi nào dùng?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** "Outbox phía consumer": consumer **ghi message nhận được vào bảng `inbox`** (khóa = messageId, unique) rồi commit offset ngay; worker riêng xử lý inbox sau. Lợi: dedup tự nhiên, tách **nhận** (nhanh, không vượt `max.poll.interval.ms`) khỏi **xử lý** (chậm, retry có kiểm soát), có thể sắp xếp lại theo sequence, lưu vết đầy đủ. Chi phí: thêm bảng, worker, độ trễ.

**Giải thích chi tiết:**
- Dùng khi xử lý phức tạp/chậm hoặc cần audit message đã nhận.
- Worker inbox cần cơ chế claim (`SKIP LOCKED`), retry, trạng thái `NEW/DONE/FAILED`.

**Câu hỏi nối tiếp:**
- *So với idempotent consumer thường?* — Inbox thêm khả năng tách nhịp nhận/xử lý; dedup thường đủ khi xử lý nhanh.

**⚠️ Câu trả lời gây điểm trừ:**
- Nhầm inbox với DLQ.

**📖 Ôn lại:** [Phần 10 — Inbox pattern](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p10)

</details>

### Q50. 🟡 Event notification khác Event-carried state transfer thế nào?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** **Event notification**: payload tối thiểu `{orderId, type}`, consumer **gọi ngược API** producer khi cần dữ liệu → runtime coupling, có thể thundering herd vào producer, nhưng đọc được trạng thái mới nhất. **ECST**: event mang **đủ dữ liệu** cần thiết → không gọi ngược, tăng availability, consumer giữ **bản sao cục bộ** (kết hợp topic compacted); đổi lại coupling vào **schema**, message lớn hơn, dữ liệu có thể cũ (nhưng đúng tại thời điểm event).

**Giải thích chi tiết:**

| | Notification | ECST |
|---|---|---|
| Payload | Nhỏ | Đầy đủ |
| Gọi ngược producer | Có | Không |
| Coupling | Runtime | Schema |

**Câu hỏi nối tiếp:**
- *Khi nào notification hợp hơn?* — Dữ liệu nhạy cảm/lớn, consumer cần bản mới nhất tại lúc xử lý.

**⚠️ Câu trả lời gây điểm trừ:**
- Không biết hai kiểu này có trade-off.

**📖 Ôn lại:** [Phần 10 — Event notification vs ECST](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p10)

</details>

### Q51. 🔴 Event sourcing là gì? Ưu nhược? Có nên dùng Kafka làm event store?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Lưu **chuỗi event** thay vì trạng thái hiện tại; trạng thái = fold các event. Ưu: audit trail đầy đủ, tái dựng trạng thái tại mọi thời điểm, nhiều projection (kết hợp **CQRS**). Nhược: phức tạp, schema evolution event lâu năm (upcasting), cần **snapshot**, query qua projection (eventual), xóa dữ liệu cá nhân khó (crypto-shredding). Kafka **không** hợp làm event store chính: không đọc hiệu quả "mọi event của aggregate X", không có **optimistic concurrency** theo aggregate → dùng DB/EventStoreDB/Axon làm store, Kafka để **phân phối**.

**Giải thích chi tiết:**
```java
sealed interface AccountEvent permits Opened, Deposited, Withdrawn {}
// Event store: events(aggregate_id, version, type, payload) UNIQUE(aggregate_id, version)
// → hai command đồng thời cùng ghi version N+1 → một bên thất bại (optimistic concurrency)
void apply(AccountEvent e) {
  switch (e) {
    case Opened o -> {}
    case Deposited d -> balance += d.amount();
    case Withdrawn w -> balance -= w.amount();
  }
  version++;
}
```

**Câu hỏi nối tiếp:**
- *Snapshot khi nào?* — Mỗi N event hoặc khi rehydrate chậm; snapshot là cache, event vẫn là nguồn sự thật.

**⚠️ Câu trả lời gây điểm trừ:**
- "Event sourcing = dùng Kafka".

**📖 Ôn lại:** [Phần 10 — Event sourcing](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p10)

</details>

### Q52. 🟡 Saga choreography hoạt động thế nào? Compensating transaction cần tính chất gì? Khi nào chuyển orchestration?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Saga = chuỗi **local transaction**, mỗi bước publish event kích hoạt bước tiếp; lỗi thì chạy **compensating transaction** hoàn tác bước trước. Choreography: không điều phối trung tâm. Compensation phải **idempotent** và **luôn thành công được** (retry tới khi xong). Saga thiếu **isolation** → dùng semantic lock (`*_PENDING`). Khi > 3–4 bước hoặc cần trả lời "đơn X đang ở bước nào" → **orchestration**.

**Giải thích chi tiết:**
```
Order: tạo PENDING + outbox OrderCreated ─► Payment: authorize ─► PaymentAuthorized
─► Inventory: reserve thất bại ─► StockReservationFailed
─► Payment: void (bù trừ) ─► PaymentVoided ─► Order: CANCELLED (bù trừ)
```
- Mỗi bước: local transaction + outbox; mỗi consumer idempotent.
- Lưới an toàn: **timeout saga** — job quét đơn `PENDING` quá N phút để hủy.
- Bộ ba "Outbox + Idempotent consumer + Saga" là câu trả lời chuẩn cho nhất quán không dùng 2PC.

**Câu hỏi nối tiếp:**
- *Nhược điểm choreography?* — Luồng nghiệp vụ ẩn trong nhiều service, dễ vòng phụ thuộc event, khó theo dõi.

**⚠️ Câu trả lời gây điểm trừ:**
- Compensation = "rollback DB" (không thể sau khi đã commit local).

**📖 Ôn lại:** [Phần 10 — Saga choreography](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p10)

</details>

---

<a id="nhom-k"></a>
## K. Duplicate, ordering, backpressure, replay & vận hành

### Q53. 🟡 Event đến không đúng thứ tự ở consumer — xử lý thế nào?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Ưu tiên giữ thứ tự ở nguồn (cùng key, idempotent producer, một thread/partition, blocking retry). Ở đích: **version check** (`UPDATE ... WHERE id=? AND version < ?` — event cũ đến muộn bị bỏ qua), **state machine** chặn chuyển trạng thái lùi (`SHIPPED` không về `PAID`), **buffer & reorder** theo sequence (phức tạp), hoặc thiết kế event **commutative**.

**Giải thích chi tiết:**
```sql
INSERT INTO order_view(order_id, status, version) VALUES (:id, :s, :v)
ON CONFLICT (order_id) DO UPDATE SET status = EXCLUDED.status, version = EXCLUDED.version
WHERE order_view.version < EXCLUDED.version;
```

**Câu hỏi nối tiếp:**
- *Version lấy ở đâu?* — Aggregate version ở producer (ví dụ `@Version` JPA) đưa vào event.

**⚠️ Câu trả lời gây điểm trừ:**
- So sánh bằng `occurredAt` giữa các máy (lệch đồng hồ).

**📖 Ôn lại:** [Phần 11 — Xử lý mất thứ tự](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p11)

</details>

### Q54. 🟡 Backpressure trong Kafka và RabbitMQ khác nhau thế nào?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** **Kafka**: log là bộ đệm tự nhiên, consumer **pull** theo tốc độ của mình; lag tăng nhưng broker không sập (miễn đĩa/retention đủ). Phía consumer: `pause()`/`resume()` khi downstream quá tải, giới hạn `max.poll.records`, concurrency. Phía producer: `buffer.memory` + `max.block.ms`. **RabbitMQ**: prefetch giới hạn in-flight; queue quá dài → memory/disk alarm → **block publisher**; `x-max-length` + `overflow=reject-publish` để nack sớm. Ingest HTTP: trả `429/503` thay vì nhận vô hạn.

**Giải thích chi tiết:**
```java
@Scheduled(fixedDelay = 1000)
void check() {
  var c = registry.getListenerContainer("orderProjection");
  int waiting = ds.getHikariPoolMXBean().getThreadsAwaitingConnection();
  if (waiting > 10 && !c.isPauseRequested()) c.pause();
  else if (waiting == 0 && c.isPauseRequested()) c.resume();
}
```
- Autoscale theo lag: **KEDA** Kafka scaler, tối đa = số partition.

**Câu hỏi nối tiếp:**
- *Pause có gây rebalance?* — Không, container vẫn `poll()`.

**⚠️ Câu trả lời gây điểm trừ:**
- Autoscale consumer vượt số partition rồi kết luận "scale không có tác dụng do code chậm".

**📖 Ôn lại:** [Phần 11 — Backpressure](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p11)

</details>

### Q55. 🔴 🎯 Tình huống: bug làm `order_view.total` sai 3 ngày. Bạn replay để sửa thế nào mà không gửi lại 1 triệu email và không downtime?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Không đụng group đang phục vụ. Dựng **group mới** `order-projection-v2` đọc từ `earliest` (hoặc `--to-datetime`) ghi vào **bảng mới** `order_view_v2`; khi **bắt kịp** (lag ≈ 0 ổn định, đối soát số bản ghi/tổng tiền khớp) thì chuyển read traffic (feature flag hoặc `CREATE OR REPLACE VIEW`), rồi xóa group/bảng cũ. Email không gửi lại vì **consumer projection tách khỏi consumer có side effect** (group khác); consumer phải idempotent; dữ liệu phải còn trong retention.

**Giải thích chi tiết:**
```bash
# Reset offset chỉ khi group inactive; luôn dry-run trước
kafka-consumer-groups.sh --bootstrap-server localhost:9092 --group order-projection \
  --topic orders --reset-offsets --to-datetime 2026-10-01T00:00:00.000 --dry-run
# --to-earliest | --to-latest | --to-offset N | --shift-by -1000 | --by-duration PT2H
```
- Reset khi consumer còn chạy → lệnh thất bại hoặc bị ghi đè offset.
- Replay từ DLT: tool đọc DLT, sửa/lọc, republish vào topic gốc **giữ eventId**, ghi log kiểm toán.
- Runbook: tiêu chí bắt kịp, điểm quay lui, kiểm tra.

**Câu hỏi nối tiếp:**
- *Dữ liệu đã hết retention?* — Rebuild từ nguồn sự thật (DB của producer, snapshot, data lake).

**⚠️ Câu trả lời gây điểm trừ:**
- Reset offset group production về earliest khi tất cả consumer (kể cả email) chung group/topic.

**📖 Ôn lại:** [Phần 11 — Replay](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p11)

</details>

### Q56. 🟡 Những metric Kafka/RabbitMQ nào cần cảnh báo? URP là gì và chẩn đoán ra sao?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Kafka: `UnderReplicatedPartitions` (>0 kéo dài), **`UnderMinIsrPartitionCount` > 0 → page** (producer `acks=all` lỗi), **`OfflinePartitionsCount` > 0 → page**, `ActiveControllerCount` ≠ 1, `RequestHandlerAvgIdlePercent` < 0.3, produce latency p99, disk > 80%, consumer lag theo thời gian, producer `record-error-rate`. RabbitMQ: `messages_ready`, `messages_unacknowledged`, publish vs ack rate, redeliver rate, memory/disk alarm, số connection/channel. **URP** = partition có ISR < RF: tập trung **một broker** → vấn đề cục bộ (đĩa, mạng, GC); rải khắp cluster → tải/mạng chung, hoặc throttle reassign.

**Giải thích chi tiết:**
- Observability luồng async: propagate `traceparent` qua header; log `eventId`, `correlationId`, `topic-partition@offset`; đo **end-to-end latency** = thời điểm xử lý − `occurredAt`; dashboard DLT count.
- Công cụ: JMX Exporter + Prometheus/Grafana, Kafka Lag Exporter/kminion/Burrow, Cruise Control, Strimzi; `rabbitmq_prometheus`.

**Câu hỏi nối tiếp:**
- *Producer timeout đột ngột?* — Kiểm URP/UnderMinIsr, broker quá tải, `buffer.memory` cạn.

**⚠️ Câu trả lời gây điểm trừ:**
- Chỉ theo dõi CPU/RAM broker.

**📖 Ôn lại:** [Phần 12 — Monitoring & vận hành](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p12)

</details>

### Q57. 🔴 🎯 Tình huống: consumer `read_committed` của một partition đứng yên, lag tăng, không có lỗi trong log consumer. Nghi gì và xử lý ra sao?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Nghi **hanging transaction**: một producer transactional mở transaction rồi chết/treo, chưa commit/abort → **LSO** của partition bị "ghim" → consumer `read_committed` không đọc vượt LSO dù HW đã đi xa. Chờ `transaction.timeout.ms` để coordinator tự abort; nếu vẫn kẹt (bug, producer cũ) dùng `kafka-transactions.sh --find-hanging` rồi `--abort` (KIP-664, Kafka 3.0+).

**Giải thích chi tiết:**
- Phân biệt: consumer `read_uncommitted` (group khác) vẫn tiến → khẳng định vấn đề ở LSO.
- Phòng ngừa: `transaction.timeout.ms` hợp lý, đóng producer đúng cách, giám sát LSO lag (HW − LSO).
- Các nguyên nhân khác của "đứng yên": poison pill bị retry vô hạn (có log lỗi), consumer thread kẹt (thread dump), partition bị `pause()` mà không resume.

**Câu hỏi nối tiếp:**
- *Vì sao `transactional.id` phải ổn định qua restart?* — Để `initTransactions()` fence zombie và hoàn tất/abort transaction dở dang của instance cũ.

**⚠️ Câu trả lời gây điểm trừ:**
- Restart consumer hoặc reset offset mà không tìm nguyên nhân.

**📖 Ôn lại:** [Phần 12 — Runbook](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p12) · [Phần 3 — Transactions & EOS](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p3)

</details>

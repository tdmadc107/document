# Case 06 — Flash sale bán vượt tồn kho (overselling): race condition trên hot row

> **Chủ đề:** Lost update, read-check-write, atomic conditional UPDATE, optimistic/pessimistic locking, Redis Lua, xử lý bất đồng bộ, deadlock
> **Module liên quan:** [M09 §10 — Optimistic vs pessimistic locking](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#10-locking) · [M11 §6 — Isolation level & anomalies](../01-giao-trinh/11-database-sql.md#phan-6) · [M11 §8 — Locking & deadlock](../01-giao-trinh/11-database-sql.md#phan-8) · [M12 §8 — Lua script](../01-giao-trinh/12-redis-caching.md#phan-8) · [M12 — Dự án mini FlashSale](../01-giao-trinh/12-redis-caching.md#du-an-mini) · [M14 §5 — Idempotency, outbox](../01-giao-trinh/14-microservices-system-design.md#p5) · [M04 §2 — Atomicity & race condition](../01-giao-trinh/04-concurrency.md#p2)
> **Độ khó:** ⭐⭐⭐⭐ (Senior / system design)
> **Thời gian tự giải gợi ý:** 45 phút

---

## 1. Bối cảnh hệ thống

Sàn thương mại điện tử chạy chương trình "12.12 — giờ vàng": 500 điện thoại giá 1.212.000đ, mở bán đúng 12:00, mỗi khách tối đa 1 máy.

| Thành phần | Chi tiết |
|---|---|
| Stack | Java 21, Spring Boot 3.2, Spring Data JPA, MySQL 8.0 (InnoDB, `REPEATABLE READ` mặc định), Redis 7 (cache trang sản phẩm) |
| Triển khai | `order-service` 12 pod (đã scale trước sự kiện), Hikari 40/pod; MySQL primary 16 vCPU + 2 replica |
| Tải dự kiến | 40.000 người chờ sẵn; đỉnh thực tế **9.200 RPS** vào `POST /orders` trong 10 giây đầu |
| Luồng cũ | Trang sản phẩm đọc tồn kho từ Redis (cache 5 s) → nút "Mua" → `POST /orders` → kiểm tra & trừ kho trong MySQL → tạo đơn → chuyển sang thanh toán |

## 2. Triệu chứng

**12:00:47** — dashboard kinh doanh: `inventory.available = 0` cho SKU `IP15-128-BLK-FS`. Mọi thứ có vẻ ổn.

**12:20** — đội vận hành kho báo:
```sql
SELECT count(*) AS orders, sum(quantity) AS units
FROM orders WHERE sku_id = 880123 AND campaign_id = 1212 AND status IN ('PENDING_PAYMENT','PAID');
--  orders | units
--  731    | 731          ← tồn kho ban đầu: 500
```

**Log ứng dụng:** không có exception nào bất thường; một số `SoldOutException` (39.000+) như mong đợi.

**Binlog MySQL (`mysqlbinlog --base64-output=DECODE-ROWS -vv`), cùng một giây:**
```
### UPDATE `shop`.`inventory`
### WHERE  @1=880123 /* sku_id */  @2=412 /* available */  @3=88 /* reserved */
### SET    @1=880123               @2=411                  @3=89
...
### UPDATE `shop`.`inventory`
### WHERE  @1=880123  @2=411  @3=89
### SET    @1=880123  @2=411  @3=89          ← "trừ" nhưng giá trị không đổi
### UPDATE `shop`.`inventory`
### WHERE  @1=880123  @2=411  @3=89
### SET    @1=880123  @2=411  @3=89
```
Nhiều transaction cùng ghi **cùng một giá trị** `available = 411` — mỗi cái tin rằng mình vừa trừ 1 từ 412.

**Hậu quả:** 231 đơn phải hủy, mỗi khách được bồi thường voucher 200.000đ; mạng xã hội lan truyền "sàn lừa đảo flash sale".

## 3. Câu hỏi đặt ra

1. Vì sao có `@Transactional` mà vẫn bán vượt? Vì sao tồn kho vẫn về đúng 0 chứ không âm?
2. Có những cách nào để trừ kho đúng dưới tải 9.000 RPS trên **một dòng**? So sánh chúng.
3. Nếu dùng Redis để chịu tải, làm sao đảm bảo không bán vượt khi Redis failover hoặc khi tạo đơn thất bại?
4. Giỏ hàng nhiều SKU thì có rủi ro gì thêm?

> ✋ **Dừng lại và tự giải trước.** Vẽ timeline hai transaction T1, T2 cùng đọc `available = 412` và chỉ ra chỗ sai. Sau đó viết câu SQL trừ kho an toàn mà không cần `SELECT ... FOR UPDATE`.

## 4. Điều tra từng bước

### Bước 1 — Đọc code trừ kho
```java
@Service
@RequiredArgsConstructor
public class PlaceOrderService {
    @Transactional
    public Order place(long userId, long skuId, int qty) {
        Inventory inv = inventoryRepo.findBySkuId(skuId);              // SELECT (snapshot read, không lock)
        if (inv.getAvailable() < qty) throw new SoldOutException(skuId);
        inv.setAvailable(inv.getAvailable() - qty);                    // tính trong Java
        inv.setReserved(inv.getReserved() + qty);
        Order order = orderRepo.save(Order.pending(userId, skuId, qty, campaignPrice(skuId)));
        return order;                                                  // flush: UPDATE inventory SET available=?, reserved=? WHERE id=?
    }
}
```
Đây là mẫu **read–check–write** không nguyên tử.

### Bước 2 — Dựng timeline
```
Thời gian  T1 (user A)                          T2 (user B)
t0         BEGIN                                BEGIN
t1         SELECT available → 412               
t2                                              SELECT available → 412   (snapshot, không bị chặn)
t3         check 412 >= 1 ✓                     check 412 >= 1 ✓
t4         UPDATE SET available = 411  (X lock)
t5                                              UPDATE SET available = 411 → chờ lock của T1
t6         INSERT order A; COMMIT (nhả lock)
t7                                              (có lock) ghi đè available = 411; INSERT order B; COMMIT
Kết quả:   2 đơn, kho chỉ giảm 1   → LOST UPDATE
```
- `@Transactional` chỉ đảm bảo **nguyên tử** (all-or-nothing) cho từng transaction, không ngăn hai transaction cùng dựa trên một giá trị cũ.
- InnoDB ở `REPEATABLE READ`: `SELECT` thường là **consistent read** từ snapshot, không lock. `UPDATE` thì lock dòng, nhưng giá trị ghi (`411`) được tính sẵn trong Java từ snapshot cũ.
- Vì giá trị ghi luôn là "đọc − 1", kho giảm chậm hơn số đơn rồi vẫn chạm 0 — nên dashboard trông bình thường.

### Bước 3 — Tái hiện bằng test đồng thời
```java
@Test
void noOversellUnderConcurrency() throws Exception {
    seedInventory(SKU, 100);
    int users = 400;
    var start = new CountDownLatch(1);
    try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
        List<Future<?>> fs = IntStream.range(0, users)
                .mapToObj(u -> pool.submit(() -> { start.await(); return tryPlace(u, SKU, 1); }))
                .toList();
        start.countDown();                                     // thả 400 request cùng lúc
        for (var f : fs) f.get();
    }
    assertThat(orderRepo.countBySkuId(SKU)).isEqualTo(100);   // code cũ: 137–162 tùy lần chạy
    assertThat(inventoryRepo.findBySkuId(SKU).getAvailable()).isZero();
}
```
(Chạy với MySQL thật qua Testcontainers, pool đủ lớn; H2 không tái hiện đúng hành vi lock của InnoDB.)

### Bước 4 — Vì sao Redis cache không giúp?
Trang sản phẩm đọc tồn kho từ Redis với TTL 5 s → chỉ để **hiển thị**. Nó không tham gia vào quyết định trừ kho, và 9.200 RPS vẫn dội thẳng vào một dòng MySQL.

## 5. Nguyên nhân gốc

- **Lost update** do read–check–write trên dữ liệu chia sẻ mà không có cơ chế kiểm soát đồng thời (không lock, không version, không điều kiện trong câu UPDATE).
- Test chỉ chạy tuần tự; load test trước sự kiện đo throughput nhưng **không kiểm tra bất biến** "số đơn ≤ tồn kho".
- Thiết kế đẩy toàn bộ tải ghi vào một hot row trong DB quan hệ.

## 6. Giải pháp

### Ngắn hạn
Đổi trừ kho thành **một câu UPDATE có điều kiện** (deploy trước đợt sale kế tiếp 3 ngày sau):
```java
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
           UPDATE inventory
              SET available = available - :qty, reserved = reserved + :qty
            WHERE sku_id = :skuId AND available >= :qty
           """, nativeQuery = true)
    int tryReserve(long skuId, int qty);
}

@Transactional
public Order place(long userId, long skuId, int qty) {
    Order order = orderRepo.save(Order.pending(userId, skuId, qty, campaignPrice(skuId)));
    if (inventoryRepo.tryReserve(skuId, qty) == 0) {        // 0 dòng → hết hàng → rollback cả đơn
        throw new SoldOutException(skuId);
    }
    return order;                                           // UPDATE để CUỐI → giữ row lock ngắn nhất
}
```
`UPDATE` trong InnoDB là **current read**: đọc phiên bản mới nhất đã commit và lock dòng, nên điều kiện `available >= :qty` được đánh giá trên giá trị thật. (PostgreSQL ở `READ COMMITTED` cũng đánh giá lại `WHERE` trên phiên bản dòng mới nhất sau khi chờ lock.) Ràng buộc phòng thủ: `CHECK (available >= 0)` (MySQL 8.0.16+ thực thi CHECK).

### Dài hạn — kiến trúc flash sale nhiều lớp
```
Client ─► Gateway (rate limit theo user, chống bot)
       ─► order-service: Redis Lua "giữ chỗ" (O(1), ~50k ops/s)  ── hết → trả "Hết hàng" ngay (99% request dừng ở đây)
       ─► (giữ chỗ OK) ghi lệnh vào Kafka `flash-orders` (key = skuId) ─► trả 202 "Đang xử lý" + orderToken
       ─► consumer: tạo đơn + UPDATE có điều kiện trong MySQL (nguồn sự thật cuối cùng), idempotent theo orderToken
       ─► hết hạn thanh toán 15 phút: hủy đơn, cộng lại kho ở cả DB và Redis
       ─► job đối soát mỗi phút: Redis stock + đơn đang giữ == DB available
```
Script Lua (nguyên tử vì Redis thực thi script đơn luồng):
```lua
-- KEYS[1] = fs:{880123}:stock   KEYS[2] = fs:{880123}:buyers   (hash tag {..} → cùng slot trong Redis Cluster)
-- ARGV[1] = userId, ARGV[2] = qty
if redis.call('SISMEMBER', KEYS[2], ARGV[1]) == 1 then return -2 end      -- đã mua rồi (giới hạn 1/khách)
local stock = tonumber(redis.call('GET', KEYS[1]) or '0')
local qty = tonumber(ARGV[2])
if stock < qty then return -1 end                                          -- hết hàng
redis.call('DECRBY', KEYS[1], qty)
redis.call('SADD', KEYS[2], ARGV[1])
return stock - qty
```
```java
private static final RedisScript<Long> RESERVE = RedisScript.of(new ClassPathResource("lua/reserve.lua"), Long.class);

public ReserveResult reserve(long skuId, long userId) {
    List<String> keys = List.of("fs:{" + skuId + "}:stock", "fs:{" + skuId + "}:buyers");
    Long r = redis.execute(RESERVE, keys, String.valueOf(userId), "1");
    return switch (r.intValue()) {
        case -1 -> ReserveResult.SOLD_OUT;
        case -2 -> ReserveResult.ALREADY_BOUGHT;
        default -> ReserveResult.RESERVED;
    };
}
```
Nguyên tắc an toàn: Redis chỉ là **cổng lọc** để chặn 99% tải; quyết định cuối cùng vẫn là `UPDATE ... WHERE available >= ?` trong MySQL. Nếu consumer thất bại vĩnh viễn → trả chỗ lại vào Redis (`INCRBY` + `SREM`) qua luồng bù trừ. Nếu Redis failover làm mất vài lần `DECRBY` (replication bất đồng bộ), Redis có thể cho qua nhiều hơn tồn kho, nhưng DB vẫn chặn → không bán vượt, chỉ có một số khách nhận "rất tiếc, hết hàng" muộn hơn.

### So sánh các phương án (số liệu load test minh họa trên staging, 1 SKU, 9.000 RPS)
| Phương án | Đúng? | Throughput trừ kho thành công | p99 API | Ưu | Nhược |
|---|---|---|---|---|---|
| Read–check–write (cũ) | ❌ lost update | ~3.000/s | 220 ms | – | Bán vượt |
| `@Version` + retry 3 lần | ✅ | ~400/s | 1,9 s | Không giữ lock; tốt khi xung đột hiếm | Xung đột cực cao trên hot row → đa số retry thất bại, phí CPU/DB, retry storm |
| `SELECT ... FOR UPDATE` | ✅ | ~900/s | 2,4 s, có `Lock wait timeout` | Dễ hiểu, cho phép logic phức tạp giữa đọc và ghi | Lock giữ suốt transaction, request xếp hàng, cạn connection pool; nguy cơ deadlock với nhiều SKU |
| **UPDATE có điều kiện** | ✅ | ~1.600/s | 600 ms | Một câu, không đọc trước, lock ngắn | Vẫn tuần tự hóa trên một dòng; khó kèm logic phức tạp |
| **Redis Lua + async + DB guard** | ✅ | Redis ~50.000 ops/s; DB ghi đều ~1.000/s | 15 ms (trả 202) | Chịu tải đỉnh, phản hồi tức thì | Phức tạp: bù trừ, đối soát, UX "đang xử lý", vận hành Redis/Kafka |
| Sharded counter (chia 500 thành 10 dòng × 50) | ✅ | ~gấp N lần | – | Giảm tranh chấp hot row trong DB | Phân bổ không đều, "hết ở dòng này còn ở dòng kia" phải xử lý |

### Góc phụ: deadlock khi giỏ hàng nhiều SKU
Sau khi chuyển sang `UPDATE` có điều kiện, checkout giỏ hàng (nhiều SKU) bắt đầu lẻ tẻ lỗi:
```
com.mysql.cj.jdbc.exceptions.MySQLTransactionRollbackException: Deadlock found when trying to get lock; try restarting transaction
```
`SHOW ENGINE INNODB STATUS` → mục `LATEST DETECTED DEADLOCK`:
```
*** (1) TRANSACTION: ... UPDATE inventory SET available = available - 1 ... WHERE sku_id = 880123 ...
*** (1) HOLDS THE LOCK(S):  RECORD LOCKS ... index PRIMARY of table `shop`.`inventory` ... sku_id 770045
*** (1) WAITING FOR THIS LOCK TO BE GRANTED: ... sku_id 880123
*** (2) TRANSACTION: ... UPDATE inventory ... WHERE sku_id = 770045 ...
*** (2) HOLDS THE LOCK(S): ... sku_id 880123
*** (2) WAITING FOR THIS LOCK TO BE GRANTED: ... sku_id 770045
*** WE ROLL BACK TRANSACTION (2)
```
User A có giỏ `[770045, 880123]`, user B có `[880123, 770045]` → khóa ngược thứ tự. Sửa: **sắp xếp `skuId` tăng dần** trước khi trừ kho (thứ tự khóa nhất quán), giữ transaction ngắn, và retry 1–2 lần cho lỗi deadlock (`CannotAcquireLockException`/`DeadlockLoserDataAccessException` trong Spring) vì InnoDB đã rollback nạn nhân, chạy lại là an toàn.

## 7. Phòng ngừa

- **Bất biến nghiệp vụ thành alert**: mỗi phút `sum(order.qty where status giữ hàng) + available == initial_stock`; lệch → page on-call ngay trong sự kiện (12:00:47 thay vì 12:20).
- **Ràng buộc DB**: `CHECK (available >= 0)`, unique `(campaign_id, sku_id, user_id)` cho giới hạn 1 máy/khách.
- **Test đồng thời** cho mọi logic "đọc rồi ghi" trên tài nguyên chia sẻ (số dư, tồn kho, voucher, ghế): `CountDownLatch` + nhiều thread + DB thật; assert bất biến, không chỉ assert "không lỗi".
- **Load test trước sự kiện** kiểm tra **tính đúng** (số đơn ≤ tồn kho) chứ không chỉ throughput/latency.
- **Code review checklist**:
  - [ ] Có mẫu `x = repo.find(); if (x.value ...) x.setValue(x.value - n)` trên dữ liệu nhiều người cùng sửa không?
  - [ ] Cơ chế kiểm soát đồng thời là gì: atomic UPDATE, `@Version`, `FOR UPDATE`, hay single-writer?
  - [ ] Khi lock nhiều dòng: thứ tự khóa có nhất quán không? Có lock timeout không?
  - [ ] Hot row: đã ước lượng tranh chấp ở đỉnh tải chưa?

## 8. Cách kể lại trong phỏng vấn (STAR, ~2 phút)

- **S:** "Sàn của chúng tôi mở flash sale 500 điện thoại, đỉnh 9.200 RPS vào API đặt hàng. Kết quả bán ra 731 máy — vượt 231, phải hủy đơn và bồi thường."
- **T:** "Tôi được giao phân tích nguyên nhân và thiết kế lại luồng trừ kho cho đợt sale ba ngày sau."
- **A:** "Binlog cho thấy nhiều transaction cùng ghi `available = 411`, tức là lost update: code đọc tồn kho, kiểm tra, rồi ghi giá trị tính trong Java. Ở `REPEATABLE READ` của InnoDB, `SELECT` thường đọc snapshot không lock, nên `@Transactional` không cứu được. Tôi viết test 400 thread trên MySQL Testcontainers tái hiện bán vượt, rồi đổi sang một câu `UPDATE ... WHERE available >= qty` kiểm tra số dòng ảnh hưởng — kịp cho đợt sale sau. Về dài hạn, tôi so sánh optimistic, pessimistic, atomic update và Redis bằng load test: với hot row, optimistic retry gần như vô dụng, `FOR UPDATE` làm cạn pool. Thiết kế cuối: Redis Lua giữ chỗ và chặn 99% request, ghi lệnh qua Kafka, consumer tạo đơn với `UPDATE` có điều kiện trong MySQL làm chốt chặn cuối, cộng job đối soát. Trong lúc đó tôi cũng xử lý deadlock của giỏ nhiều SKU bằng cách sắp xếp thứ tự khóa."
- **R:** "Đợt sale sau 1.000 máy, 61.000 người: bán đúng 1.000, API p99 15 ms, MySQL CPU dưới 40%. Bất biến tồn kho trở thành alert theo phút, và test đồng thời thành bắt buộc cho mọi logic trừ tài nguyên."

## 9. Câu hỏi mở rộng

<details>
<summary>1. Đặt isolation <code>SERIALIZABLE</code> có giải quyết được không?</summary>

Về tính đúng thì có, nhưng giá rất đắt và hành vi khác nhau theo DB. MySQL `SERIALIZABLE` biến `SELECT` thường thành `SELECT ... FOR SHARE`: hai transaction cùng giữ shared lock rồi cùng muốn nâng lên exclusive để UPDATE → **deadlock** hàng loạt trên hot row. PostgreSQL dùng SSI: không chặn nhưng ném `could not serialize access` (SQLSTATE 40001) cho các transaction xung đột → phải retry, và trên hot row tỷ lệ thất bại rất cao. Cả hai đều tệ hơn một câu UPDATE có điều kiện.
</details>

<details>
<summary>2. Optimistic locking tốt hơn pessimistic khi nào? Vì sao nó thua ở flash sale?</summary>

Optimistic (`@Version`) tốt khi xung đột **hiếm** và chu trình đọc–sửa dài (form chỉnh sửa qua UI, nhiều giây/phút): không giữ lock, scale đọc tốt, chỉ trả giá khi thật sự xung đột. Ở flash sale, hàng nghìn transaction/giây cùng sửa một dòng: gần như mọi transaction đều xung đột, mỗi lần thất bại đã tốn trọn một transaction (đọc, insert order, rollback), retry làm tải nhân lên — throughput thành công thấp hơn cả pessimistic. Quy tắc: xung đột cao trên một dòng → atomic update hoặc single-writer (hàng đợi tuần tự).
</details>

<details>
<summary>3. Giữ chỗ nhưng khách không thanh toán thì sao?</summary>

Tách `available` và `reserved`: giữ chỗ chuyển từ available sang reserved kèm `expires_at` (15 phút). Thanh toán thành công → reserved giảm (bán thật). Hết hạn → job (hoặc delayed message) chuyển lại về available bằng UPDATE có điều kiện trên trạng thái đơn (`WHERE status = 'PENDING_PAYMENT'`) để không đua với callback thanh toán đến muộn; đồng thời trả lại chỗ trong Redis. Mọi bước phải idempotent vì job và callback có thể chạy trùng.
</details>

<details>
<summary>4. Khách bấm "Mua" hai lần hoặc app tự retry — làm sao không tạo 2 đơn?</summary>

Client sinh **idempotency key** (`orderToken`) cho mỗi lần bấm, server lưu `(key → kết quả)` với unique constraint; request trùng trả lại kết quả cũ. Kết hợp ràng buộc nghiệp vụ unique `(campaign_id, sku_id, user_id)` và tập `buyers` trong Lua. Consumer Kafka cũng idempotent theo `orderToken` vì Kafka giao "at-least-once".
</details>

<details>
<summary>5. Nếu dùng <code>SELECT ... FOR UPDATE</code>, cần cấu hình gì để không kéo sập hệ thống?</summary>

Đặt lock timeout ngắn (MySQL `innodb_lock_wait_timeout` mặc định 50 s — quá dài; đặt theo session hoặc dùng hint `jakarta.persistence.lock.timeout`; MySQL 8 hỗ trợ `NOWAIT`/`SKIP LOCKED`), giữ transaction ngắn (không gọi HTTP khi đang giữ lock), khóa theo thứ tự nhất quán, giới hạn concurrency trước khi vào DB (bulkhead/semaphore theo SKU) để không cạn connection pool. Với work queue nhiều worker, `FOR UPDATE SKIP LOCKED` cho phép mỗi worker lấy dòng khác nhau.
</details>

<details>
<summary>6. Vì sao không dùng <code>synchronized</code> hoặc <code>ReentrantLock</code> trong Java cho đơn giản?</summary>

Lock trong JVM chỉ có hiệu lực **trong một process**; với 12 pod, mỗi pod có lock riêng → vẫn race giữa các pod. Ngoài ra lock JVM bao quanh transaction còn có bẫy: nếu lock được nhả trước khi transaction commit (lock bên trong method `@Transactional`), transaction khác vẫn đọc được giá trị cũ. Muốn lock phân tán thì phải dùng DB hoặc Redis/ZooKeeper — và với bài toán đếm, atomic operation ở nơi lưu dữ liệu luôn đơn giản và đúng hơn lock.
</details>

# Case 10 — Giá hiển thị sai sau khi cập nhật và DB "sập" đúng giờ flash sale: cache inconsistency + cache stampede

> **Chủ đề:** Redis cache-aside, thứ tự cập nhật DB/cache, replica lag, cache breakdown/avalanche, cache hai tầng
> **Module liên quan:** [M12 §10 — Nhất quán giữa DB và cache](../01-giao-trinh/12-redis-caching.md#phan-10) · [M12 §11 — Penetration, breakdown, avalanche, hot key](../01-giao-trinh/12-redis-caching.md#phan-11) · [M12 §2 — Caffeine vs distributed cache](../01-giao-trinh/12-redis-caching.md#phan-2) · [M12 §14 — Tích hợp Spring](../01-giao-trinh/12-redis-caching.md#phan-14) · [M09 §11 — Spring Transactions](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#11-transactions) · [M13 §10 — Outbox + CDC](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p10)
> **Độ khó:** ⭐⭐⭐⭐ (Senior)
> **Thời gian tự giải gợi ý:** 40–45 phút

---

## 1. Bối cảnh hệ thống

Một sàn thương mại điện tử (minh họa) với `product-service` phục vụ trang chi tiết sản phẩm, trang danh mục và widget giá trong giỏ hàng.

```
                 ┌──────────────┐
 Mobile/Web ───► │ API Gateway  │──► product-service (40 pod, Spring Boot 3.2, Java 21)
                 └──────────────┘          │  cache-aside
                                           ├──► Redis Cluster (3 master + 3 replica)
                                           ├──► MySQL 8.0 primary  (ghi)
                                           └──► MySQL 8.0 replica ×2 (đọc khi cache miss)
 pricing-admin (back-office) ──► PUT /internal/products/{id}/price
 price-import job (19:55 mỗi ngày) ──► cập nhật ~8.000 giá cho chương trình khuyến mãi tối
 checkout-service ──► đọc giá trực tiếp từ MySQL primary (nguồn sự thật khi thanh toán)
```

| Thông số | Giá trị |
|---|---|
| Số sản phẩm đang bán | ~1,2 triệu SKU, 50.000 SKU "nóng" |
| Đọc trang chi tiết | 6.000 QPS bình thường, **18.000 QPS** lúc 20:00 (flash sale) |
| Cache hit ratio | 98,5% |
| TTL cache | Cố định 30 phút (`RedisCacheConfiguration.entryTtl(Duration.ofMinutes(30))`) |
| Warm-up | Job 19:30 nạp sẵn 50.000 SKU nóng vào Redis trước flash sale |
| Read routing | `@Transactional(readOnly = true)` → `AbstractRoutingDataSource` trỏ sang replica |
| HikariCP | `maximumPoolSize=20` mỗi pod → 800 connection tối đa tới replica |
| p99 bình thường | 25 ms |

---

## 2. Triệu chứng

**Vấn đề A — giá "lệch" (kéo dài nhiều tuần, tần suất thấp):**

- CSKH nhận 30–50 phiếu/tuần: "Trang sản phẩm hiện 199.000đ, vào giỏ hàng thành 249.000đ". Một số khách chụp màn hình và đòi bán đúng giá hiển thị.
- Đội pricing xác nhận giá mới đã lưu đúng trong DB từ nhiều phút trước.
- Hiện tượng tự hết sau tối đa ~30 phút (đúng bằng TTL) hoặc khi ai đó bấm "Purge cache" trong admin.
- Ước lượng từ log audit: khoảng **0,2–0,4%** lượt cập nhật giá để lại cache cũ; tỷ lệ tăng vọt lên ~3% trong đợt price-import 19:55.

**Vấn đề B — sự cố 20:00 ngày khuyến mãi (SEV1, 11 phút):**

```
20:00:02 [ALERT] product-service p99 latency 8.420 ms (SLO 200 ms)
20:00:04 [ALERT] mysql-replica-1 CPU 100%, Threads_running 612
20:00:05 [ALERT] product-service 5xx rate 23%
20:00:09 [ALERT] checkout-service: dependency product-service error rate 18%
```

Log ứng dụng:

```
2026-09-09T20:00:03.118+07:00 WARN  [http-nio-8080-exec-187] c.z.h.p.HikariPool : replicaPool - Connection is not available, request timed out after 3000ms (total=20, active=20, idle=0, waiting=184)
2026-09-09T20:00:03.402+07:00 ERROR [http-nio-8080-exec-92] o.a.c.c.C.[.[.[/].[dispatcherServlet] : Servlet.service() threw exception
org.springframework.dao.DataAccessResourceFailureException: Unable to acquire JDBC Connection
```

Grafana: số lệnh `GET` Redis trả `nil` nhảy từ ~90/s lên **~17.000/s** trong 2 giây đầu sau 20:00:00; số query `SELECT ... FROM product ... WHERE id = ?` trên replica tăng từ 100 QPS lên 9.000 QPS, trong đó một SKU flash sale (iPhone giảm giá) chiếm 4.200 QPS.

---

## 3. Câu hỏi đặt ra

Người phỏng vấn thường hỏi theo hai tầng:

1. Vì sao cache lại giữ giá cũ dù giá mới đã nằm trong DB? Có bao nhiêu kịch bản race có thể dẫn tới điều đó? Vì sao tỷ lệ tăng mạnh trong đợt import?
2. Vì sao đúng 20:00:00 hệ thống sập dù cache hit ratio "bình thường" là 98,5%?
3. Bạn sẽ mitigate ngay trong đêm thế nào, và sửa dài hạn thế nào? Mỗi lựa chọn đánh đổi gì?
4. Giá ở checkout có nên đọc từ cache không?

> ✋ **Dừng lại và tự giải trước khi đọc tiếp.** Viết ra giấy: (a) ít nhất hai chuỗi thời gian (timeline) giữa thread ghi và thread đọc dẫn tới cache cũ; (b) lý do mọi key hết hạn cùng lúc; (c) ba biện pháp cho stampede và trade-off của từng cái.

---

## 4. Điều tra từng bước

### Bước 1 — Xác nhận cache thực sự khác DB (không phải do CDN hay client)

```bash
# Lấy giá trong Redis và TTL còn lại của key bị khách phản ánh
redis-cli -c -h redis-0 GET "catalog:product:v1:884213"
redis-cli -c -h redis-0 TTL "catalog:product:v1:884213"     # → 1412 (còn ~23 phút)
```

```sql
-- So với primary và replica
SELECT id, price, updated_at FROM product WHERE id = 884213;   -- primary: 249000, 19:56:41.208
```

Kết quả: Redis giữ `price=199000`, DB là `249000`. TTL còn 1.412 s → key được **set lúc ~19:56:48**, tức là **sau** thời điểm cập nhật DB (19:56:41). Đây là dấu hiệu then chốt: không phải "quên xóa cache", mà là **ai đó đã nạp lại giá cũ vào cache sau khi giá mới được ghi**.

### Bước 2 — Đọc code đường ghi

```java
@Service
@RequiredArgsConstructor
public class PriceService {
    private final ProductRepository repo;
    private final StringRedisTemplate redis;
    private final SearchIndexClient searchIndex;
    private final AuditPublisher audit;

    @Transactional
    public void updatePrice(long id, BigDecimal newPrice, String actor) {
        redis.delete(RedisKeys.product(id));          // (1) xóa cache TRƯỚC
        Product p = repo.findById(id).orElseThrow();
        p.setPrice(newPrice);                         // (2) dirty checking, flush lúc commit
        audit.publish(new PriceChanged(id, newPrice, actor));
        searchIndex.reindex(id);                      // (3) HTTP sang search, 200–800 ms
    }                                                 // (4) COMMIT ở đây
}
```

Ghi chú: giữa (1) và (4) có tới vài trăm ms tới gần 1 giây. Trong khoảng đó **mọi request đọc** đều miss cache, đọc DB (thấy giá cũ vì chưa commit) và **set lại giá cũ** với TTL 30 phút.

### Bước 3 — Đọc code đường đọc

```java
@Transactional(readOnly = true)   // → route sang replica
public ProductDto getProduct(long id) {
    String key = RedisKeys.product(id);
    String cached = redis.opsForValue().get(key);
    if (cached != null) return json.read(cached);
    ProductDto dto = mapper.toDto(repo.findById(id).orElseThrow());   // đọc REPLICA
    redis.opsForValue().set(key, json.write(dto), Duration.ofMinutes(30));
    return dto;
}
```

Hai giả thuyết race được xác nhận bằng tracing (Tempo/Jaeger) cho key 884213:

```
Race 1 — xóa trước commit:
  T_write: DEL cache ─────── UPDATE (chưa commit) ── reindex 600ms ── COMMIT
  T_read :        miss ─ SELECT (thấy giá cũ) ─ SET cache=cũ (TTL 30')
Race 2 — replica lag (kể cả khi xóa sau commit):
  T_write: COMMIT primary ─ DEL cache
  T_read :                       miss ─ SELECT replica (lag 1,8s → giá cũ) ─ SET cache=cũ
```

### Bước 4 — Vì sao đợt import 19:55 tệ hơn

```sql
-- Trên replica, trong lúc import
SHOW REPLICA STATUS\G
-- Seconds_Behind_Source: 4   (bình thường 0)
```

Price-import cập nhật 8.000 dòng theo transaction lớn → replica apply chậm → lag 2–4 s, kéo dài cửa sổ của Race 2. Đồng thời import diễn ra lúc traffic đang tăng trước flash sale → xác suất có request đọc rơi vào cửa sổ cao hơn nhiều.

### Bước 5 — Vì sao 20:00:00 mọi thứ sập

```bash
# Phân bố TTL của các key nóng, lấy mẫu lúc 19:45
redis-cli -c -h redis-0 --scan --pattern 'catalog:product:v1:*' | head -2000 \
  | xargs -n1 redis-cli -c -h redis-0 TTL | sort -n | uniq -c | sort -rn | head
#  1873  899
#   97   898
```

Gần như toàn bộ key có cùng TTL: warm-up 19:30 nạp 50.000 key với TTL cố định 30 phút → **cùng hết hạn lúc 20:00:00**, đúng giây mở flash sale (**cache avalanche**). Trong số đó có SKU iPhone 4.200 QPS (**cache breakdown / hot key**): mỗi lần hết hạn, loader mất ~300 ms dưới tải → khoảng `4.200 × 0,3 ≈ 1.260` request cùng chạy một query → pool replica 20 connection/pod cạn → request treo 3 s chờ connection → Tomcat thread cạn → 5xx lan sang checkout.

```sql
-- Bằng chứng trên replica
SELECT info, COUNT(*) FROM information_schema.processlist
WHERE command <> 'Sleep' GROUP BY info ORDER BY 2 DESC LIMIT 3;
-- select p1_0.id, p1_0.price, ... from product p1_0 where p1_0.id=?   | 588
```

`redis-cli --hotkeys` (sau khi đặt `maxmemory-policy allkeys-lfu` trên node staging để tái hiện) xác nhận key `catalog:product:v1:771002` nóng nhất.

---

## 5. Nguyên nhân gốc

| # | Nguyên nhân | Loại |
|---|---|---|
| 1 | Xóa cache **trước** khi transaction commit, và transaction bao quanh một lời gọi HTTP chậm → cửa sổ race hàng trăm ms | Bug code |
| 2 | Đường nạp cache đọc từ **replica có lag** → kể cả xóa đúng thời điểm vẫn nạp lại giá cũ | Thiết kế |
| 3 | TTL **cố định** + warm-up hàng loạt → hàng chục nghìn key hết hạn cùng giây | Cấu hình |
| 4 | Không có cơ chế **single-flight/mutex** cho hot key → stampede vào DB | Thiết kế |
| 5 | Không có rate limit/bulkhead bảo vệ DB khi cache miss hàng loạt | Thiếu phòng thủ |

Yếu tố góp phần: không có metric "độ lệch cache", nên lỗi giá chỉ được phát hiện qua phản ánh của khách.

---

## 6. Giải pháp

### 6.1 Ngắn hạn (trong đêm và tuần đầu)

1. **Ngay lúc sự cố:** bật feature flag "serve stale" (giữ bản sao giá trong L1 Caffeine 60 s đã có sẵn cho trang danh mục), scale replica read và giới hạn concurrency của đường cache-miss bằng `Semaphore(50)` mỗi pod — request vượt ngưỡng trả trang không có widget giá phụ thay vì 5xx.
2. **Tách warm-up:** nạp key với TTL ngẫu nhiên, và chạy refresh cho 500 SKU flash sale mỗi 60 s trước khi hết hạn.
3. **Chuyển `redis.delete` ra sau commit** và đưa `searchIndex.reindex` ra khỏi transaction (hotfix một dòng logic, rủi ro thấp).
4. Bổ sung nút "Purge theo SKU" và hướng dẫn CSKH dùng khi khách phản ánh.

### 6.2 Dài hạn — tính nhất quán

**(a) Delete-after-commit + đọc primary khi nạp cache cho dữ liệu nhạy cảm:**

```java
@Service
@RequiredArgsConstructor
public class PriceService {
    private final ProductRepository repo;
    private final ApplicationEventPublisher events;

    @Transactional
    public void updatePrice(long id, BigDecimal newPrice, String actor) {
        Product p = repo.findById(id).orElseThrow();
        p.changePrice(newPrice);                         // version++ (@Version)
        events.publishEvent(new ProductChanged(id, p.getVersion()));
    }
}

@Component
@RequiredArgsConstructor
class ProductCacheInvalidator {
    private final StringRedisTemplate redis;
    private final ScheduledExecutorService scheduler;     // bean riêng, có tên thread rõ ràng
    private final InvalidationRetryQueue retryQueue;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChanged(ProductChanged e) {
        String key = RedisKeys.product(e.id());
        deleteOrEnqueue(key);
        // delayed double delete: delay > replica lag p99 + thời gian một lần nạp cache
        scheduler.schedule(() -> deleteOrEnqueue(key), 2, TimeUnit.SECONDS);
    }

    private void deleteOrEnqueue(String key) {
        try { redis.delete(key); }
        catch (RuntimeException ex) { retryQueue.offer(key); }   // Redis timeout → retry, không nuốt lỗi
    }
}
```

Đường đọc: khi **cache miss**, nạp từ **primary** với giới hạn concurrency (số miss nhỏ so với tổng đọc nên primary chịu được), hoặc giữ replica nhưng dựa vào double delete.

**(b) Ghi cache có version** để giá cũ không thể đè giá mới (Lua compare-and-set như ở M12 §10.2):

```java
private static final RedisScript<Long> SET_IF_NEWER = new DefaultRedisScript<>("""
    local cur = redis.call('HGET', KEYS[1], 'v')
    if cur and tonumber(cur) >= tonumber(ARGV[1]) then return 0 end
    redis.call('HSET', KEYS[1], 'v', ARGV[1], 'd', ARGV[2])
    redis.call('EXPIRE', KEYS[1], ARGV[3])
    return 1
    """, Long.class);
```

Lưu ý: version chỉ chặn trường hợp "đã có giá mới trong cache rồi bị giá cũ đè". Nếu key đã bị xóa và giá cũ được set vào chỗ trống thì version không giúp — cần kết hợp double delete hoặc CDC.

**(c) CDC-based invalidation** (khi có nhiều đường ghi: admin, import job, script SQL tay của DBA):

```
MySQL binlog (ROW) ──► Debezium ──► Kafka "catalogdb.shop.product" ──► cache-invalidator
                                                                         └─ DEL key (idempotent)
```

Ưu điểm: không bỏ sót đường ghi nào, có retry/thứ tự theo log, tách khỏi code nghiệp vụ. Nhược: thêm hạ tầng, độ trễ 200–800 ms, phải giám sát lag connector.

### 6.3 Dài hạn — chống stampede/avalanche

```java
@Component
@RequiredArgsConstructor
public class ProductReader {
    private final Cache<Long, ProductDto> l1 = Caffeine.newBuilder()
            .maximumSize(20_000)
            .expireAfterWrite(Duration.ofSeconds(5))          // L1 rất ngắn: giới hạn độ stale
            .build();
    private final StringRedisTemplate redis;
    private final ProductLoader loader;                       // đọc primary, có Semaphore bảo vệ
    private final ExecutorService refresher;

    public ProductDto get(long id) {
        // Caffeine.get(key, fn): single-flight trong một JVM — 1 loader/khóa/pod
        return l1.get(id, this::fromRedisOrLoad);
    }

    private ProductDto fromRedisOrLoad(long id) {
        String key = RedisKeys.product(id);
        CacheEntry e = read(key);                             // {data, logicalExpireAt}, Redis TTL dài
        if (e != null && e.logicalExpireAt().isAfter(Instant.now())) return e.data();
        if (tryLock("lock:" + key, Duration.ofSeconds(3))) {  // SET NX PX + token, unlock bằng Lua
            refresher.submit(() -> reload(id, key));          // chỉ 1 pod nạp lại
        }
        if (e != null) return e.data();                       // logical expiry: trả bản cũ ngay
        return loader.loadWithBoundedWait(id);                // lần đầu: chờ có giới hạn
    }

    private void reload(long id, String key) {
        ProductDto fresh = loader.load(id);
        Duration jitter = Duration.ofSeconds(ThreadLocalRandom.current().nextInt(0, 120));
        write(key, new CacheEntry(fresh, Instant.now().plus(Duration.ofMinutes(10)).plus(jitter)));
    }
}
```

Invalidate L1 trên 40 pod khi giá đổi: invalidator publish `PUBLISH cache-inval product:{id}` (Redis Pub/Sub) → mỗi pod `l1.invalidate(id)`. Pub/Sub là fire-and-forget, nên TTL L1 ngắn (5 s) là lưới an toàn khi mất message.

### 6.4 So sánh các phương án

| Phương án | Giải quyết | Chi phí / rủi ro | Khi nào dùng |
|---|---|---|---|
| Delete-after-commit | Race 1 | Gần như miễn phí | Luôn luôn |
| Delayed double delete | Race 2 (replica lag) | Thêm scheduler, delay phải > lag; lag đột biến vẫn lọt | Khi đọc replica, chưa có CDC |
| Nạp cache từ primary | Race 2 | Tăng tải primary lúc miss hàng loạt | Dữ liệu nhạy cảm, miss rate thấp |
| Version + Lua CAS | Ghi đè bằng dữ liệu cũ | Phải có version trong entity; không chặn "set vào chỗ trống" | Bổ trợ |
| CDC invalidation | Mọi đường ghi, có retry | Debezium + Kafka, độ trễ ~ trăm ms | Nhiều đường ghi, đội đã có Kafka |
| Mutex Redis | Stampede liên pod | Request chờ, cần TTL lock và timeout chờ | Key nóng, chấp nhận chờ ngắn |
| Logical expiry | Stampede, không ai phải chờ | Trả dữ liệu stale ngắn; code phức tạp hơn | Hot key, dữ liệu chấp nhận stale vài giây |
| TTL jitter | Avalanche | Gần như miễn phí | Luôn luôn |
| L1 Caffeine + Redis | Hot key, Redis lỗi | Stale theo TTL L1, cần kênh invalidate | Hot key cực nóng, đọc >> ghi |

**Về giá ở checkout:** checkout tiếp tục đọc DB primary (nguồn sự thật). Trang sản phẩm được phép stale vài giây; nhưng khi giá ở checkout khác giá khách vừa thấy, UI phải hiển thị thông báo "Giá đã thay đổi" thay vì lặng lẽ tính giá khác. Đó là quyết định sản phẩm, không chỉ kỹ thuật.

---

## 7. Phòng ngừa

**Monitoring & alert**

- Job "cache auditor": mỗi phút lấy mẫu ngẫu nhiên 200 SKU, so Redis với primary, xuất metric `cache_stale_ratio`; alert nếu > 0,1% trong 10 phút.
- Metric miss theo key prefix (`cache_gets_total{result="miss"}` của Micrometer cho Spring Cache, hoặc counter tự viết); alert khi miss rate tăng gấp 5 lần baseline trong 1 phút.
- Dashboard Hikari `hikaricp_connections_pending`, replica lag `Seconds_Behind_Source`, Redis `keyspace_misses`.
- Biểu đồ phân bố TTL còn lại (sampling) để phát hiện "tường hết hạn".

**Test**

- Test tái hiện race với `CountDownLatch` ép thứ tự (M12 bài 10.1, 10.2) chạy trong CI bằng Testcontainers (MySQL primary/replica + Redis).
- Load test kịch bản "hot key hết hạn ở đỉnh tải" (Gatling: 5.000 req/s vào 1 key, TTL 5 s) — tiêu chí: số lần loader chạy ≤ số pod mỗi chu kỳ.

**Checklist review cho code có cache**

- [ ] Xóa/cập nhật cache có nằm **sau commit** không? Có retry khi Redis lỗi không?
- [ ] Transaction có chứa lời gọi mạng không (kéo dài cửa sổ race, giữ connection)?
- [ ] Đường nạp cache đọc từ đâu (primary/replica)? Lag tối đa bao nhiêu?
- [ ] TTL có jitter không? Có key nào cực nóng cần mutex/logical expiry/L1?
- [ ] Dữ liệu này có được phép stale không, stale tối đa bao lâu? Ai là nguồn sự thật khi ra quyết định tiền?

**Quy trình**

- Warm-up trước sự kiện lớn phải review cùng SRE; game day "Redis flush" mỗi quý để kiểm tra DB chịu được cold cache có kiểm soát.

---

## 8. Cách kể lại trong phỏng vấn (STAR, ~2 phút)

- **Situation:** "Product-service của bọn mình phục vụ khoảng 6 nghìn QPS, lên 18 nghìn lúc flash sale, cache-aside bằng Redis trước MySQL có replica. Có hai vấn đề: khách thấy giá cũ sau khi đổi giá, và một tối flash sale DB replica lên 100% CPU đúng 20:00, 5xx 23% trong 11 phút."
- **Task:** "Mình là người dẫn điều tra và đề xuất fix, mục tiêu vừa chặn sự cố trong đêm vừa loại bỏ gốc rễ trước đợt sale tiếp theo hai tuần sau."
- **Action:** "Mình so TTL còn lại của key với `updated_at` trong DB và thấy key được set lại **sau** khi giá mới đã ghi — chứng minh có race chứ không phải quên xóa. Trace cho thấy hai race: code xóa cache trước commit với một lời gọi HTTP chậm trong transaction, và đường nạp cache đọc từ replica lag vài giây. Còn sự cố 20:00 là do warm-up 19:30 nạp 50 nghìn key cùng TTL 30 phút. Mình chuyển invalidation sang after-commit kèm delayed double delete 2 giây, sau đó thêm CDC bằng Debezium; cho hot key dùng logical expiry với mutex Redis và L1 Caffeine 5 giây; TTL thêm jitter; và một job auditor đo tỷ lệ cache lệch."
- **Result:** "Tỷ lệ cache lệch từ ~0,3% xuống dưới 0,005%, phiếu CSKH về giá gần như về 0. Đợt sale sau, 22 nghìn QPS, số query DB lúc 20:00 chỉ tăng khoảng 8%, p99 giữ dưới 60 ms. Bài học mình rút ra là cache luôn cần một chiến lược nhất quán được viết rõ và một thước đo, chứ không phải chỉ `@Cacheable`."

---

## 9. Câu hỏi mở rộng

<details>
<summary>1. Vì sao không "cập nhật cache" (set giá mới) thay vì xóa cache?</summary>

Hai thread ghi đồng thời có thể set cache theo thứ tự ngược với thứ tự commit DB → cache giữ giá trị cũ hơn DB cho tới hết TTL. Ngoài ra giá trị cache thường là DTO tổng hợp nhiều bảng, đường ghi khó dựng lại đúng; và lãng phí khi ghi nhiều đọc ít. Xóa cache để lần đọc sau tự nạp là mặc định an toàn hơn; chỉ "update cache" khi có version CAS (Lua) hoặc khi Redis là nguồn sự thật duy nhất.
</details>

<details>
<summary>2. Delay của delayed double delete chọn bao nhiêu? Nếu lag replica đột biến 30 s thì sao?</summary>

Delay ≥ replica lag p99 + thời gian một lần nạp cache (ví dụ lag p99 1 s + nạp 300 ms → 1,5–2 s). Lag đột biến vượt delay thì vẫn lọt, nên cần: alert lag; khi lag vượt ngưỡng thì đường nạp cache chuyển sang đọc primary; hoặc nạp cache **luôn** từ primary cho dữ liệu nhạy cảm. Lưu ý CDC không tự giải quyết replica lag: invalidation theo binlog của primary có thể đến trước khi replica bắt kịp, và một request đọc replica vẫn nạp lại giá cũ. Cách triệt để là để consumer CDC **ghi giá trị mới kèm version** vào cache (Lua CAS) thay vì chỉ xóa, hoặc nạp từ primary.
</details>

<details>
<summary>3. `@CacheEvict` trên method `@Transactional` có an toàn không?</summary>

Không đảm bảo. Evict chạy khi method trả về, cùng lúc hoặc trước khi transaction commit tùy thứ tự advisor. Dùng `RedisCacheManager.builder(...).transactionAware()` (bọc bằng `TransactionAwareCacheDecorator`, trì hoãn put/evict tới after-commit) hoặc tự evict trong `@TransactionalEventListener(AFTER_COMMIT)`. Và đừng quên self-invocation làm annotation mất tác dụng.
</details>

<details>
<summary>4. Invalidate L1 Caffeine trên 40 pod thế nào? Có dùng được Redis client-side caching không?</summary>

Cách phổ biến: Redis Pub/Sub hoặc một Kafka topic "cache-invalidation" mà mỗi pod subscribe; TTL L1 ngắn làm lưới an toàn vì Pub/Sub có thể mất message khi pod reconnect. Redis 6+ có **client-side caching** (`CLIENT TRACKING`, RESP3) — server tự gửi invalidation cho client đã đọc key; Lettuce hỗ trợ qua `ClientSideCaching`. Ưu điểm là không tự viết kênh invalidate; nhược điểm là tốn bộ nhớ tracking phía server và cần hiểu rõ chế độ (default vs broadcast).
</details>

<details>
<summary>5. Logical expiry và mutex khác nhau thế nào về trải nghiệm người dùng?</summary>

Mutex: chỉ một request nạp, các request khác phải chờ (spin có giới hạn) rồi đọc lại → p99 tăng bằng thời gian nạp. Logical expiry: mọi request nhận ngay bản cũ, một request nạp nền → p99 không đổi nhưng chấp nhận stale ngắn. Chọn logical expiry cho dữ liệu hiển thị (giá xem trước, mô tả), mutex cho dữ liệu cần mới hơn mà chấp nhận chờ.
</details>

<details>
<summary>6. Nếu Redis Cluster sập hoàn toàn thì sao?</summary>

Đó là avalanche dạng toàn phần. Phòng thủ: L1 Caffeine với TTL dài hơn ở chế độ degrade, circuit breaker quanh Redis (fail fast thay vì chờ timeout mỗi lệnh), bulkhead/semaphore giới hạn số query DB đồng thời, load shedding ở gateway cho trang ít quan trọng. Và Redis phải có HA (replica khác AZ), persistence phù hợp, warm-up có kiểm soát khi khôi phục.
</details>

# Module 12 — Redis & Caching

> **Mục tiêu:** sau module này bạn giải thích được caching nhiều tầng và các chỉ số hit ratio/TTL; chọn đúng giữa local cache (Caffeine) và distributed cache (Redis); hiểu kiến trúc và cấu trúc dữ liệu bên trong Redis kèm độ phức tạp; vận hành Redis an toàn (persistence, eviction, replication, Sentinel, Cluster); thiết kế các caching pattern và giữ nhất quán giữa DB & cache; xử lý cache penetration/breakdown/avalanche, big key/hot key; hiện thực distributed lock, rate limiter, leaderboard, idempotency key; tích hợp Spring Cache/Spring Data Redis đúng cách và debug sự cố production.
> **Cấp độ:** Cơ bản → Nâng cao (Senior)
> **Thời lượng gợi ý:** 6 ngày (≈ 30 giờ)
> **Yêu cầu trước:** Module 11 (Database & SQL), Spring Boot cơ bản, Java concurrency cơ bản.
> **Nguồn tham khảo:**
> - Ngoài: [redis.io/docs](https://redis.io/docs/latest/) — *Data types*, *Keyspace notifications*, *Persistence*, *Replication*, *High availability with Sentinel*, *Scale with Redis Cluster* & *Cluster specification*, *Transactions*, *Programmability (Lua/Functions)*, *Pipelining*, *Key eviction*, *Distributed Locks with Redis*, *Latency monitoring*, *Memory optimization*.
> - Ngoài: [Spring Data Redis Reference](https://docs.spring.io/spring-data/redis/reference/) và [Spring Framework — Cache Abstraction](https://docs.spring.io/spring-framework/reference/integration/cache.html).
> - Ngoài: [Redisson Wiki/Docs](https://redisson.org/docs/) — *Distributed locks and synchronizers*.
> - Ngoài: [Caffeine Wiki](https://github.com/ben-manes/caffeine/wiki) — *Efficiency* (W-TinyLFU), *Refresh*, *Eviction*.
> - Ngoài: Martin Kleppmann, *"How to do distributed locking"* (blog, 2016) và phản hồi của antirez *"Is Redlock safe?"*; sách *Designing Data-Intensive Applications* — Ch.8 *The Trouble with Distributed Systems* (fencing token, process pause), Ch.5 *Replication*.

## Mục lục
1. [Nền tảng caching](#phan-1)
2. [Local cache với Caffeine vs distributed cache](#phan-2)
3. [Kiến trúc Redis — vì sao nhanh](#phan-3)
4. [Cấu trúc dữ liệu & internals](#phan-4)
5. [Thiết kế key, expiration & eviction](#phan-5)
6. [Persistence: RDB, AOF, hybrid](#phan-6)
7. [Replication, Sentinel & Redis Cluster](#phan-7)
8. [Transaction, Lua script & pipelining](#phan-8)
9. [Các caching pattern](#phan-9)
10. [Nhất quán giữa DB và cache](#phan-10)
11. [Sự cố cache: penetration, breakdown, avalanche, big/hot key](#phan-11)
12. [Distributed lock với Redis](#phan-12)
13. [Rate limiting & các use case kinh điển](#phan-13)
14. [Tích hợp Spring](#phan-14)
15. [Giám sát & vận hành](#phan-15)
16. [Dự án mini của module](#du-an-mini)
17. [Checklist tự đánh giá](#checklist-tu-danh-gia)

> **Chuẩn bị môi trường:**
> ```bash
> docker run -d --name redis -p 6379:6379 redis:7.4 redis-server --appendonly yes
> docker exec -it redis redis-cli
> ```
> Các ví dụ `redis-cli` dưới đây chạy được trên Redis 7.x (hoặc Valkey 7.2+/8.x — bản fork mã nguồn mở tương thích giao thức).

---

<a id="phan-1"></a>
## 1. Nền tảng caching

### 1.1 Khái niệm
**Cache** là bản sao dữ liệu đặt ở nơi truy cập **nhanh hơn / rẻ hơn** nguồn gốc, đổi lại có thể **cũ (stale)**. Mọi quyết định caching là trade-off giữa **độ tươi**, **hiệu năng**, **độ phức tạp** và **chi phí bộ nhớ**.

Các tầng cache trong một request web điển hình:

```
Browser cache  →  CDN / edge  →  API Gateway / reverse proxy (Nginx)  →
App local cache (Caffeine, trong JVM)  →  Distributed cache (Redis)  →
DB buffer pool / page cache  →  Disk
```

| Tầng | Latency điển hình | Phạm vi | Dùng cho | Invalidate |
|---|---|---|---|---|
| Client/browser (`Cache-Control`, `ETag`) | 0 (không cần mạng) | 1 người dùng | Static asset, response GET ít đổi | Khó — chỉ chờ hết hạn hoặc đổi URL (fingerprint) |
| CDN | ~10–50ms (gần user) | Toàn cầu | Ảnh, JS/CSS, trang công khai | Purge API, TTL |
| Local in-process | ~ns–µs | 1 instance JVM | Dữ liệu tra cứu, config, đọc cực nóng | Mỗi node tự hết hạn, khó đồng bộ |
| Distributed (Redis) | ~0.2–1ms (cùng DC) | Mọi instance | Session, dữ liệu chia sẻ, counter | Xóa key là mọi node thấy |
| DB buffer pool | ~µs khi hit | DB | Tự động | Tự động |

> Con số latency nên nhớ (cỡ độ lớn): đọc RAM ~100ns; round-trip trong cùng datacenter ~0.5ms; đọc SSD ngẫu nhiên ~100µs; query DB đơn giản qua mạng 1–5ms; query phức tạp 10ms–giây.

### 1.2 Hit ratio, TTL & các chỉ số

- **Hit ratio** = hits / (hits + misses). Latency trung bình ≈ `hit_ratio × t_cache + (1 − hit_ratio) × (t_cache + t_origin)`. Từ 90% lên 99% hit ratio giảm số request xuống DB **10 lần** — đó là lý do vài phần trăm hit ratio rất đáng giá.
- **TTL (time-to-live)**: giới hạn trên của độ cũ. TTL ngắn → tươi hơn, hit ratio thấp hơn; TTL dài → ngược lại. TTL cũng là "lưới an toàn" khi invalidation thất bại.
- **Cái gì đáng cache?** Dữ liệu được **đọc nhiều hơn ghi rất nhiều**, tốn kém để tính, chấp nhận được độ cũ nhất định, và có **phân bố truy cập lệch** (một phần nhỏ key chiếm phần lớn truy cập — power law). Dữ liệu đọc ngẫu nhiên đều (long tail) thì cache ít hiệu quả.
- **Working set**: tập dữ liệu đang được truy cập thường xuyên. Cache cần đủ lớn để chứa working set, không cần chứa toàn bộ dữ liệu.

> 💡 **Góc nhìn Senior:**
> - Cache **che giấu** vấn đề hiệu năng thay vì sửa nó. Hãy chắc rằng hệ thống **vẫn sống** khi cache trống (cold start, Redis restart, flush nhầm) — nếu DB chỉ sống được nhờ cache 99% hit ratio thì cache đã trở thành thành phần **critical** và phải được thiết kế HA như DB.
> - Cache làm phát sinh câu hỏi **nhất quán** (phần 10) — "There are only two hard things in Computer Science: cache invalidation and naming things" (Phil Karlton).
> - Đo trước khi cache: profile để biết thực sự bottleneck ở đâu.

> ⚠️ **Lỗi thường gặp:** cache dữ liệu theo người dùng với key không chứa userId (lộ dữ liệu người khác); cache response lỗi/rỗng với TTL dài; không có TTL ("cache vĩnh viễn") rồi quên invalidate; cache object khổng lồ (cả danh sách 50.000 sản phẩm trong một key).

### 🛠 Bài tập phần 1

**Bài 1.1 — Tính toán hit ratio (Cơ bản)**
- Đề bài: API sản phẩm 5.000 req/s, DB query 20ms, Redis 0.5ms. Tính latency trung bình và số query/giây tới DB ở hit ratio 0%, 80%, 95%, 99%. DB chỉ chịu được 800 query/s — hit ratio tối thiểu là bao nhiêu?
- Tiêu chí: có công thức, bảng kết quả, kết luận về rủi ro khi cache bị flush.

**Bài 1.2 — HTTP caching (Trung bình)**
- Đề bài: với Spring Boot, endpoint `GET /products/{id}` trả `ETag` (hash của `version`) và `Cache-Control: max-age=60`. Request kèm `If-None-Match` trùng phải trả `304`.
- Tiêu chí: test bằng `curl -i` chứng minh 200 → 304; giải thích khác nhau giữa `no-cache`, `no-store`, `private`, `max-age`, `s-maxage`.

**Bài 1.3 — Phân tích phân bố truy cập (Nâng cao)**
- Đề bài: sinh 1 triệu request theo phân bố Zipf (s=1.0) trên 100.000 key. Mô phỏng cache LRU dung lượng 1%, 5%, 10%, 20% số key và đo hit ratio. Lặp lại với phân bố đều.
- Tiêu chí: biểu đồ hit ratio theo kích thước cache cho hai phân bố, kết luận khi nào cache đáng đầu tư.

<details>
<summary>Gợi ý lời giải</summary>

**1.1** Latency ≈ `0.5 + (1 − h) × 20` ms. h=0%: 20.5ms, 5.000 q/s vào DB; h=80%: 4.5ms, 1.000 q/s; h=95%: 1.5ms, 250 q/s; h=99%: 0.7ms, 50 q/s. DB 800 q/s → cần h ≥ 84%. Khi flush cache: 5.000 q/s ập vào DB → sập → cần warm-up, request coalescing (phần 11).

**1.2** `ShallowEtagHeaderFilter` tạo ETag từ body (vẫn tốn CPU sinh response); tự sinh ETag từ `version` tốt hơn: `ResponseEntity.ok().eTag("\"" + p.getVersion() + "\"").cacheControl(CacheControl.maxAge(60, SECONDS))`, kiểm tra `WebRequest.checkNotModified(etag)`.

**1.3** Dùng `LinkedHashMap(accessOrder=true)` + `removeEldestEntry` để mô phỏng LRU. Với Zipf, cache 5–10% key thường đạt hit ratio cao (trên 50–70% tùy tham số); với phân bố đều hit ratio ≈ tỷ lệ kích thước cache.
</details>

---

<a id="phan-2"></a>
## 2. Local cache với Caffeine vs distributed cache

### 2.1 Caffeine
Caffeine là thư viện local cache hiệu năng cao cho Java (kế thừa Guava Cache), được Spring Boot hỗ trợ sẵn.

```java
LoadingCache<Long, Product> cache = Caffeine.newBuilder()
    .maximumSize(10_000)                         // hoặc maximumWeight + weigher
    .expireAfterWrite(Duration.ofMinutes(10))    // hết hạn cứng sau khi ghi
    .refreshAfterWrite(Duration.ofMinutes(1))    // sau 1 phút, lần đọc kế tiếp kích hoạt reload bất đồng bộ
    .recordStats()
    .build(id -> productRepository.findById(id).orElse(null));

Product p = cache.get(42L);                      // miss → gọi loader, các thread khác chờ chung (không stampede)
CacheStats s = cache.stats();                    // hitRate(), evictionCount(), loadSuccessCount()...
```

**Ba kiểu hết hạn/làm mới:**
- `expireAfterWrite(d)`: entry bị loại sau `d` kể từ lúc ghi → lần đọc sau là **miss đồng bộ** (người dùng chờ load).
- `expireAfterAccess(d)`: loại nếu không ai truy cập trong `d` (hợp với session-like data).
- `refreshAfterWrite(d)`: **không** loại entry; sau `d`, lần đọc đầu tiên vẫn trả **giá trị cũ ngay lập tức** và kích hoạt reload bất đồng bộ (cần `LoadingCache`/`AsyncLoadingCache`). Kết hợp `refreshAfterWrite(1m) + expireAfterWrite(10m)`: dữ liệu nóng luôn tươi trong ~1 phút mà người dùng không bao giờ chờ; dữ liệu nguội tự rơi ra sau 10 phút.
- `expireAfter(Expiry)`: TTL riêng cho từng entry.

**W-TinyLFU — vì sao Caffeine có hit ratio cao:**
- LRU thuần dễ bị **cache pollution**: một lần quét lớn (scan) đẩy hết dữ liệu nóng ra. LFU thuần không thích nghi khi xu hướng thay đổi và tốn bộ nhớ đếm.
- Caffeine dùng **Window TinyLFU**: một **window** LRU nhỏ (mặc định ~1% dung lượng) đón entry mới; phần chính là **Segmented LRU** (probation + protected ~80%). Khi entry rời window muốn vào vùng chính, một **admission filter** so tần suất ước lượng của nó với "nạn nhân" sắp bị loại — chỉ cho vào nếu tần suất cao hơn.
- Tần suất ước lượng bằng **Count-Min Sketch** (4 bit mỗi counter, rất tiết kiệm), định kỳ **chia đôi** (aging) để quên lịch sử cũ.
- Kích thước window được **tự điều chỉnh** (hill climbing) theo workload — thiên về recency hay frequency.

### 2.2 Local vs distributed

| | Local (Caffeine) | Distributed (Redis) |
|---|---|---|
| Latency | Nanosecond, không serialize | ~0.2–1ms + serialize/deserialize |
| Dung lượng | Giới hạn bởi heap (ảnh hưởng GC) | Hàng chục–trăm GB, scale bằng cluster |
| Nhất quán giữa các instance | ❌ Mỗi pod một bản, lệch nhau | ✅ Một bản chung |
| Sống sót qua restart/deploy | ❌ | ✅ (tùy persistence) |
| Chia sẻ state (session, counter, lock) | ❌ | ✅ |
| Phụ thuộc mạng | Không | Có — Redis chậm/chết ảnh hưởng ứng dụng |

**Cache 2 tầng (near cache / multi-level):** L1 Caffeine TTL ngắn (vài giây–1 phút) + L2 Redis. Invalidation L1 giữa các node: Redis Pub/Sub hoặc **client-side caching** (Redis 6+ `CLIENT TRACKING` — server gửi thông báo invalidate cho client đã đọc key; Lettuce hỗ trợ qua `ClientSideCaching`, Redisson có `RLocalCachedMap`).

> 💡 **Góc nhìn Senior:**
> - Local cache cho dữ liệu **ít đổi, đọc cực nóng, chấp nhận lệch vài giây** (danh mục, cấu hình, tỷ giá, feature flag). Hot key trên Redis (phần 11) thường được giải bằng chính L1.
> - Cache trong heap lớn → tăng thời gian GC và nguy cơ OOM. Luôn giới hạn `maximumSize`/`maximumWeight`; theo dõi kích thước thật (đối tượng Java tốn bộ nhớ hơn bạn nghĩ).
> - Không bao giờ tự viết cache bằng `ConcurrentHashMap` không giới hạn — đó là memory leak có hẹn.

> ⚠️ **Lỗi thường gặp:** trả về object mutable từ local cache rồi code gọi sửa nó → làm hỏng dữ liệu trong cache cho mọi request khác. Dùng immutable object (record) hoặc copy.

### 🛠 Bài tập phần 2

**Bài 2.1 — Caffeine cơ bản (Cơ bản)**
- Đề bài: cache tỷ giá ngoại tệ (loader giả lập chậm 200ms) với `maximumSize`, `expireAfterWrite`, `recordStats`.
- Tiêu chí: test chứng minh 100 thread đồng thời `get` cùng key khi miss chỉ gọi loader **một lần**; in `hitRate`.

**Bài 2.2 — refreshAfterWrite (Trung bình)**
- Đề bài: so sánh latency p99 của hai cấu hình: `expireAfterWrite(5s)` và `refreshAfterWrite(5s) + expireAfterWrite(60s)` dưới tải đều 500 req/s trong 60s.
- Tiêu chí: biểu đồ/bảng p50, p99; giải thích vì sao refresh loại bỏ được các "spike" định kỳ; điều gì xảy ra nếu loader ném exception khi refresh?

**Bài 2.3 — Benchmark chính sách loại bỏ (Nâng cao)**
- Đề bài: dùng JMH hoặc mô phỏng đơn giản, so sánh hit ratio giữa Caffeine và LRU tự viết (`LinkedHashMap`) trên workload Zipf có xen kẽ các đợt quét tuần tự (scan) 50.000 key.
- Tiêu chí: số liệu chứng minh W-TinyLFU chống scan pollution; giải thích bằng cơ chế admission filter.

<details>
<summary>Gợi ý lời giải</summary>

**2.1**
```java
AtomicInteger calls = new AtomicInteger();
LoadingCache<String, BigDecimal> rates = Caffeine.newBuilder().maximumSize(100)
    .expireAfterWrite(Duration.ofMinutes(5)).recordStats()
    .build(ccy -> { calls.incrementAndGet(); Thread.sleep(200); return new BigDecimal("25000"); });
ExecutorService ex = Executors.newFixedThreadPool(100);
List<Callable<BigDecimal>> tasks = Collections.nCopies(100, () -> rates.get("USD"));
ex.invokeAll(tasks);
assertEquals(1, calls.get());
```
**2.2** Khi loader lỗi lúc refresh, Caffeine giữ giá trị cũ và log lỗi; lần đọc sau sẽ thử refresh lại. Entry vẫn hết hạn ở mốc `expireAfterWrite`.

**2.3** Trong đợt scan, LRU bị thay toàn bộ nội dung → hit ratio rơi về ~0 rồi phải hồi phục; Caffeine từ chối nhận key scan (tần suất 1) vào vùng chính nên hit ratio gần như không đổi.
</details>

---

<a id="phan-3"></a>
## 3. Kiến trúc Redis — vì sao nhanh

### 3.1 Mô hình thực thi
- Redis xử lý **lệnh** trên **một thread chính** với **event loop** (I/O multiplexing: `epoll` trên Linux, `kqueue` trên BSD/macOS). Thread chính: nhận kết nối sẵn sàng → đọc request → **thực thi lệnh** → ghi response.
- **Vì sao một thread lại nhanh:**
  1. Dữ liệu **trong RAM**, cấu trúc dữ liệu được tối ưu kỹ (encoding gọn cho tập nhỏ).
  2. Không có chi phí lock/context switch giữa các thread khi truy cập dữ liệu; mỗi lệnh **atomic** một cách tự nhiên.
  3. I/O non-blocking multiplexing phục vụ hàng chục nghìn kết nối.
  4. Giao thức RESP đơn giản, parse nhanh.
  Bottleneck thường là **mạng** và **bộ nhớ**, không phải CPU. Một instance thường đạt ~100k ops/s trở lên cho lệnh đơn giản, hơn nữa khi pipelining.
- **Redis 6+ I/O threads** (`io-threads 4`, `io-threads-do-reads yes`): các thread phụ chỉ làm việc **đọc/parse socket và ghi response**; **thực thi lệnh vẫn đơn luồng**. Hữu ích khi CPU bị chiếm bởi xử lý mạng (nhiều client, payload lớn).
- **Background threads** (từ trước đó): đóng file, fsync AOF, **lazy free** (`UNLINK`, `FLUSHALL ASYNC`, `lazyfree-lazy-eviction`...) — giải phóng bộ nhớ của key lớn ngoài thread chính.
- **Fork** cho persistence (BGSAVE, BGREWRITEAOF) — process con dùng copy-on-write.

### 3.2 Hệ quả của đơn luồng

> ⚠️ **Một lệnh chậm chặn TẤT CẢ client.** Các lệnh O(N) trên tập lớn là thủ phạm:
> `KEYS *`, `FLUSHALL` (đồng bộ), `HGETALL`/`SMEMBERS`/`LRANGE 0 -1` trên key hàng triệu phần tử, `DEL` một key lớn (giải phóng hàng triệu phần tử), `SORT`, `SUNION`/`ZUNIONSTORE` trên tập lớn, Lua script chạy lâu.
> Thay thế: `SCAN`/`HSCAN`/`SSCAN`/`ZSCAN`, `UNLINK` thay `DEL`, phân trang `LRANGE`/`ZRANGE` theo khoảng, cấu hình `rename-command KEYS ""` hoặc ACL chặn lệnh nguy hiểm.

> 💡 **Góc nhìn Senior:** Vì đơn luồng, muốn tận dụng máy nhiều core thì chạy **nhiều instance/shard** (Redis Cluster) thay vì "nâng cấp CPU". Ngoài ra, fork trên instance có dataset lớn (vài chục GB) có thể mất hàng trăm ms (copy page table) và gây spike latency; bật **Transparent Huge Pages** làm copy-on-write tốn bộ nhớ hơn nhiều → Redis khuyến cáo tắt THP.

### 🛠 Bài tập phần 3

**Bài 3.1 — Benchmark cơ bản (Cơ bản)**
- Đề bài: chạy `redis-benchmark -t set,get -n 1000000 -q` rồi với `-P 16` (pipeline) và `-d 1024` (payload 1KB).
- Tiêu chí: bảng ops/s; giải thích vì sao pipeline tăng throughput nhiều lần.

**Bài 3.2 — Lệnh chậm chặn mọi người (Trung bình)**
- Đề bài: tạo một Set 5 triệu phần tử (bằng Lua hoặc pipeline). Trong khi một client chạy `redis-cli --latency`, client khác chạy `SMEMBERS` key đó rồi `DEL` key đó; lặp lại với `SSCAN` và `UNLINK`.
- Tiêu chí: ghi lại latency max trong mỗi trường hợp, giải thích.

**Bài 3.3 — I/O threads (Nâng cao)**
- Đề bài: trên máy ≥ 8 core, benchmark `GET` với payload 16KB, 500 kết nối, `io-threads 1` vs `4`. (Dùng `memtier_benchmark` nếu có.)
- Tiêu chí: số liệu; giải thích vì sao I/O threads giúp ở workload này nhưng không giúp cho lệnh `ZUNIONSTORE` nặng CPU.

<details>
<summary>Gợi ý lời giải</summary>

**3.2**
```bash
redis-cli EVAL "for i=1,5000000 do redis.call('SADD', KEYS[1], i) end" 1 big:set   # chính lệnh này cũng chặn server vài giây!
redis-cli --latency            # cửa sổ khác
redis-cli SMEMBERS big:set > /dev/null
redis-cli DEL big:set          # vài trăm ms–giây bị chặn
```
`UNLINK` trả về ngay, giải phóng bộ nhớ ở background thread.

**3.3** I/O threads song song hóa phần đọc/ghi socket và copy buffer 16KB; `ZUNIONSTORE` tốn CPU ở phần **thực thi** — vẫn trên thread chính.
</details>

---

<a id="phan-4"></a>
## 4. Cấu trúc dữ liệu & internals

Redis lưu mọi key dưới dạng `redisObject` có **type** (kiểu logic) và **encoding** (cách lưu vật lý). Tập nhỏ dùng encoding gọn (listpack, intset) để tiết kiệm bộ nhớ và thân thiện CPU cache; vượt ngưỡng thì chuyển sang cấu trúc tổng quát. Xem bằng `OBJECT ENCODING key`.

### 4.1 Bảng tổng hợp

| Type | Encoding (Redis 7.x) | Lệnh chính & độ phức tạp | Use case |
|---|---|---|---|
| **String** | `int` (số nguyên 64-bit), `embstr` (≤ 44 byte), `raw` (SDS) — tối đa 512MB | `GET/SET` O(1), `INCR/INCRBY` O(1), `APPEND` O(1) khấu hao, `GETRANGE` O(N) | Cache object (JSON), counter, distributed lock, idempotency key |
| **Hash** | `listpack` (≤ `hash-max-listpack-entries` 128 và mỗi giá trị ≤ 64 byte) → `hashtable` | `HGET/HSET/HINCRBY` O(1), `HGETALL` O(N) | Object nhiều field (giỏ hàng, profile), counter theo nhóm |
| **List** | `quicklist` (danh sách liên kết các node listpack); list rất nhỏ có thể là `listpack` (7.2+) | `LPUSH/RPOP` O(1), `LINDEX/LSET` O(N), `LRANGE` O(S+N), `BLPOP/BLMOVE` | Queue đơn giản, timeline gần nhất (`LPUSH` + `LTRIM`) |
| **Set** | `intset` (toàn số nguyên, ≤ 512), `listpack` (7.2+, ≤ 128), `hashtable` | `SADD/SISMEMBER/SREM` O(1), `SINTER` O(N×M) tệ nhất, `SMEMBERS` O(N) | Tag, danh sách duy nhất, quan hệ bạn bè (giao/hợp) |
| **Sorted Set (ZSet)** | `listpack` (≤ 128 phần tử, ≤ 64 byte) → `skiplist` + `hashtable` | `ZADD` O(log N), `ZSCORE` O(1), `ZRANK` O(log N), `ZRANGE` O(log N + M), `ZRANGEBYSCORE` O(log N + M) | Leaderboard, sliding window rate limit, delay queue, index phụ theo thời gian |
| **Bitmap** | String (tối đa 2^32 bit) | `SETBIT/GETBIT` O(1), `BITCOUNT` O(N), `BITOP` O(N), `BITFIELD` | Điểm danh hằng ngày, user active (userId làm offset), feature flag theo user |
| **HyperLogLog** | String đặc biệt, ≤ **12KB**, sai số chuẩn **0,81%** | `PFADD` O(1), `PFCOUNT` O(1) với 1 key, `PFMERGE` | Đếm số phần tử phân biệt xấp xỉ (UV theo ngày) |
| **Stream** | Radix tree của các listpack | `XADD` O(1), `XREADGROUP`, `XACK` O(1), `XRANGE` O(log N + M) | Message queue bền vững có consumer group, event sourcing nhẹ |
| **Geo** | Sorted Set với score = geohash 52-bit | `GEOADD` O(log N), `GEOSEARCH` O(N + log M) | Tìm cửa hàng/tài xế gần nhất |

### 4.2 Sorted Set & skiplist

ZSet lớn dùng **hai** cấu trúc song song:
- **hashtable** `member → score`: `ZSCORE` O(1), kiểm tra tồn tại.
- **skiplist** sắp theo `(score, member)`: nhiều tầng danh sách liên kết, tầng trên "nhảy cóc" qua tầng dưới; mỗi node được thăng tầng với xác suất 1/4 (tối đa 32 tầng) → tìm kiếm/chèn O(log N) kỳ vọng. Redis bổ sung **span** ở mỗi liên kết để tính **rank** trong O(log N) (`ZRANK`).

Vì sao skiplist thay vì cây cân bằng (red-black, AVL)? Theo antirez: dễ hiện thực & debug hơn, range query (`ZRANGEBYSCORE`) chỉ là đi tuần tự ở tầng đáy, tiêu thụ bộ nhớ có thể tinh chỉnh qua xác suất, hiệu năng tương đương.

```
L3: head ------------------------------> 50 --------------------> NIL
L2: head ----------> 20 ---------------> 50 ---------> 80 ------> NIL
L1: head -> 10 ----> 20 ----> 35 ------> 50 -> 60 ---> 80 -> 95 -> NIL
```

### 4.3 Ví dụ redis-cli theo use case

```bash
# String: counter & cache có TTL
SET product:42 '{"id":42,"name":"Bút","price":5000}' EX 600
INCR page:view:2024-06-01
SET lock:order:1001 "uuid-abc" NX PX 30000

# Hash: giỏ hàng
HSET cart:user:7 sku:1 2 sku:9 1
HINCRBY cart:user:7 sku:1 1
HGETALL cart:user:7
EXPIRE cart:user:7 604800

# List: 100 thông báo gần nhất
LPUSH notif:user:7 "Đơn 1001 đã giao"
LTRIM notif:user:7 0 99
LRANGE notif:user:7 0 19

# Set: tag & bạn chung
SADD user:1:friends 2 3 4
SADD user:2:friends 1 3 5
SINTER user:1:friends user:2:friends       # → 3

# Sorted Set: leaderboard
ZADD lb:2024-06 1500 "alice" 1200 "bob" 1800 "chi"
ZINCRBY lb:2024-06 400 "bob"
ZREVRANGE lb:2024-06 0 9 WITHSCORES         # top 10 (7.x: ZRANGE lb:2024-06 0 9 REV WITHSCORES)
ZREVRANK lb:2024-06 "bob"                   # hạng (0-based)

# Bitmap: điểm danh tháng 6, userId = 1001
SETBIT checkin:2024-06:user:1001 0 1        # ngày 1
SETBIT checkin:2024-06:user:1001 4 1        # ngày 5
BITCOUNT checkin:2024-06:user:1001          # số ngày đã điểm danh
# Hoặc DAU: SETBIT dau:2024-06-01 <userId> 1 ; BITOP AND để tìm user active cả 7 ngày

# HyperLogLog: UV
PFADD uv:2024-06-01 "u1" "u2" "u3" "u1"
PFCOUNT uv:2024-06-01                       # 3 (xấp xỉ)
PFMERGE uv:2024-06-w1 uv:2024-06-01 uv:2024-06-02

# Stream: queue có consumer group
XADD orders:events * type CREATED orderId 1001
XGROUP CREATE orders:events billing $ MKSTREAM
XREADGROUP GROUP billing worker-1 COUNT 10 BLOCK 5000 STREAMS orders:events >
XACK orders:events billing 1717200000000-0
XPENDING orders:events billing                     # message đã giao nhưng chưa ACK
XAUTOCLAIM orders:events billing worker-2 60000 0  # 6.2+: nhận lại message treo > 60s

# Geo
GEOADD stores 105.8342 21.0278 "hoan-kiem" 106.7009 10.7769 "quan-1"
GEOSEARCH stores FROMLONLAT 105.85 21.03 BYRADIUS 5 km ASC WITHDIST

# Xem encoding
OBJECT ENCODING cart:user:7                 # "listpack"
MEMORY USAGE cart:user:7
```

> 💡 **Góc nhìn Senior:**
> - **Hash nhỏ tiết kiệm bộ nhớ đáng kể** nhờ listpack. Mẹo kinh điển (Instagram): thay vì 300 triệu key String `media:{id} → userId`, gom vào hash `media:{id/1000}` với field `{id%1000}` → mỗi hash ≤ 1000 field (chỉnh `hash-max-listpack-entries`) → giảm bộ nhớ nhiều lần. Đổi lại: không đặt TTL riêng cho từng field được (Redis 7.4 mới thêm `HEXPIRE` cho field).
> - Chọn cấu trúc theo **mẫu truy cập**, không theo "hình dạng dữ liệu": cần đọc một field → Hash; luôn đọc/ghi cả object → String JSON có thể đơn giản hơn.
> - Mỗi key có overhead (~50–70 byte cho dictEntry + redisObject + SDS key) → hàng trăm triệu key nhỏ tốn bộ nhớ cho overhead nhiều hơn dữ liệu.

> ⚠️ **Lỗi thường gặp:** dùng List làm queue quan trọng (mất message khi consumer chết sau `RPOP` — dùng `LMOVE`/`BLMOVE` sang list "processing" hoặc dùng Stream); dùng `KEYS`/`SMEMBERS` trong code production; ZSet leaderboard dùng score `double` cho số tiền lớn (mất chính xác ngoài 2^53).

### 🛠 Bài tập phần 4

**Bài 4.1 — Chọn đúng cấu trúc (Cơ bản)**
- Đề bài: với mỗi yêu cầu, chọn type + thiết kế key + lệnh: (a) đếm lượt xem bài viết; (b) danh sách 50 bài đọc gần đây của user, không trùng; (c) số user online phân biệt trong ngày cho 10 triệu user; (d) user đã điểm danh 7 ngày liên tiếp chưa; (e) top 100 sản phẩm bán chạy tuần.
- Tiêu chí: mỗi lời giải có lệnh redis-cli chạy được và độ phức tạp.

**Bài 4.2 — Encoding & bộ nhớ (Trung bình)**
- Đề bài: lưu 1 triệu cặp `userId → điểm` theo 3 cách: 1 triệu key String; 1 Hash lớn; 1.000 Hash nhỏ (bucket theo `userId / 1000`). Đo `INFO memory` (`used_memory`) mỗi cách.
- Tiêu chí: bảng so sánh, xác nhận encoding bằng `OBJECT ENCODING`, giải thích ngưỡng `hash-max-listpack-entries`.

**Bài 4.3 — Recent-items với ZSet (Nâng cao)**
- Đề bài: hiện thực "50 sản phẩm xem gần đây, không trùng, mới nhất trước" với ZSet (score = timestamp) bằng một Lua script atomic: thêm, cắt còn 50, đặt TTL 30 ngày. Viết bằng Java (Lettuce/Spring `RedisTemplate`).
- Tiêu chí: test đồng thời 100 thread không làm vượt 50 phần tử; so sánh với cách dùng List (`LREM` + `LPUSH` + `LTRIM`) về độ phức tạp.

<details>
<summary>Gợi ý lời giải</summary>

**4.1** (a) `INCR post:{id}:views`; (b) ZSet `recent:{uid}` score=timestamp + `ZREMRANGEBYRANK 0 -51`; (c) HyperLogLog `PFADD online:2024-06-01 uid` (≈12KB) hoặc Bitmap `SETBIT` (10 triệu bit ≈ 1,2MB, chính xác); (d) Bitmap theo user, `BITFIELD`/`GETRANGE` 7 bit cuối; (e) ZSet `ZINCRBY sales:2024-w23 qty sku`, `ZREVRANGE 0 99`.

**4.3**
```lua
-- KEYS[1]=recent:{uid}  ARGV[1]=sku ARGV[2]=nowMillis ARGV[3]=max ARGV[4]=ttlSec
redis.call('ZADD', KEYS[1], ARGV[2], ARGV[1])
redis.call('ZREMRANGEBYRANK', KEYS[1], 0, -(tonumber(ARGV[3]) + 1))
redis.call('EXPIRE', KEYS[1], ARGV[4])
return redis.call('ZCARD', KEYS[1])
```
List: `LREM` là O(N) — với N = 50 không đáng kể, nhưng ZSet tự khử trùng và rõ nghĩa hơn.
</details>

---

<a id="phan-5"></a>
## 5. Thiết kế key, expiration & eviction

### 5.1 Đặt tên key
- Quy ước phân cấp bằng dấu `:` — `{service}:{entity}:{id}[:{field}]`, ví dụ `order-svc:order:1001`, `catalog:product:42:v3`.
- Thêm **version** vào key (`...:v3`) để thay đổi định dạng serialize mà không phải flush — phiên bản mới đọc key mới, key cũ tự hết hạn.
- Key ngắn vừa phải (key cũng tốn RAM) nhưng **dễ đọc** quan trọng hơn khi debug.
- Trong Cluster dùng **hash tag** `{...}` cho các key cần thao tác chung (phần 7).
- Multi-tenant: tiền tố tenant để dễ kiểm soát và tránh lộ dữ liệu.

### 5.2 Expiration
```bash
SET k v EX 60            # TTL 60 giây (PX: mili giây, EXAT/PXAT: thời điểm tuyệt đối)
EXPIRE k 120 ; TTL k ; PTTL k ; PERSIST k
SET k v2 KEEPTTL         # 6.0+: SET mặc định XÓA TTL cũ! cần KEEPTTL để giữ
EXPIRE k 60 NX           # 7.0+: chỉ đặt nếu chưa có TTL (XX, GT, LT tương tự)
```

Redis xóa key hết hạn bằng **hai cơ chế kết hợp**:
1. **Lazy (passive) expiration**: khi client truy cập key, Redis kiểm tra hạn; hết hạn thì xóa và trả như không tồn tại.
2. **Active expiration**: chu kỳ nền (theo `hz`, mặc định 10 lần/giây) lấy mẫu ngẫu nhiên một số key có TTL (mặc định 20), xóa key hết hạn; nếu tỷ lệ hết hạn trong mẫu cao (> 25%) thì lặp lại ngay, có giới hạn thời gian CPU mỗi chu kỳ.

Hệ quả: key hết hạn **có thể vẫn chiếm bộ nhớ** một thời gian; số liệu `DBSIZE` có thể bao gồm key đã hết hạn chưa bị dọn. Replica không tự xóa key hết hạn — chờ lệnh `DEL` từ master (nhưng khi đọc vẫn trả về như đã hết hạn, từ Redis 3.2).

### 5.3 Eviction (khi chạm `maxmemory`)

| Policy | Áp dụng cho | Thuật toán |
|---|---|---|
| `noeviction` (**mặc định**) | — | Từ chối lệnh ghi (lỗi `OOM command not allowed`), lệnh đọc vẫn chạy |
| `allkeys-lru` | Mọi key | LRU xấp xỉ |
| `allkeys-lfu` (4.0+) | Mọi key | LFU xấp xỉ (counter logarit 8-bit + decay) |
| `allkeys-random` | Mọi key | Ngẫu nhiên |
| `volatile-lru` / `volatile-lfu` / `volatile-random` | Chỉ key có TTL | Như trên |
| `volatile-ttl` | Chỉ key có TTL | Ưu tiên key sắp hết hạn |

- LRU/LFU là **xấp xỉ**: Redis lấy mẫu `maxmemory-samples` (mặc định 5) key và loại key "tệ nhất" trong mẫu (cùng một pool ứng viên). Tăng lên 10 gần với LRU thật hơn, tốn CPU hơn.
- LFU có tham số `lfu-log-factor`, `lfu-decay-time`. Xem tần suất: `OBJECT FREQ key` (cần policy LFU).

**Chọn policy:**
- Redis **thuần cache**: `allkeys-lru` hoặc `allkeys-lfu` (phân bố lệch, dữ liệu nóng ổn định → LFU thường tốt hơn).
- Redis **vừa cache vừa lưu dữ liệu không được mất** (session, lock, queue): `volatile-*` và chỉ đặt TTL cho key cache — nhưng tốt hơn hết là **tách instance**: một cho cache (evict), một cho dữ liệu (noeviction + persistence).
- Redis làm **primary store / queue**: `noeviction` + cảnh báo bộ nhớ sớm.

> 💡 **Góc nhìn Senior:** Luôn đặt `maxmemory` (để chừa RAM cho fork COW, buffer client, fragmentation — thường 60–75% RAM máy nếu có persistence). Không đặt `maxmemory` trên Linux → Redis dùng tới khi bị **OOM killer** giết. Lưu ý `volatile-*` mà không key nào có TTL thì hành vi giống `noeviction`.

> ⚠️ **Lỗi thường gặp:** `SET` lại một key có TTL mà quên `EX`/`KEEPTTL` → key trở thành vĩnh viễn; `INCR` một counter mới rồi `EXPIRE` ở lệnh riêng — nếu app chết giữa hai lệnh, counter tồn tại vĩnh viễn (dùng Lua hoặc `SET k 0 EX 60 NX` trước); hàng triệu key cùng TTL hết hạn cùng lúc (avalanche, phần 11).

### 🛠 Bài tập phần 5

**Bài 5.1 — Quy ước key (Cơ bản)**
- Đề bài: viết tài liệu quy ước key cho một hệ thống TMĐT gồm: cache sản phẩm, giỏ hàng, session, OTP, rate limit đăng nhập, leaderboard flash sale. Ghi rõ type, TTL, ai ghi/ai đọc.
- Tiêu chí: có version, có tenant/prefix service, TTL hợp lý có giải thích; viết class Java `RedisKeys` tạo key tập trung.

**Bài 5.2 — Quan sát expiration (Trung bình)**
- Đề bài: tạo 1 triệu key TTL 10s bằng pipeline, không truy cập lại. Theo dõi `INFO keyspace` (`expires`), `INFO stats` (`expired_keys`), `used_memory` mỗi giây trong 60s.
- Tiêu chí: biểu đồ cho thấy key được dọn dần bởi active expiry; thử thay `hz 10` → `hz 100` và so sánh.

**Bài 5.3 — Eviction thực nghiệm (Nâng cao)**
- Đề bài: `maxmemory 50mb`, workload Zipf ghi/đọc liên tục 200k key giá trị 1KB. So sánh hit ratio (`keyspace_hits/(hits+misses)`) của `allkeys-lru`, `allkeys-lfu`, `allkeys-random`, và với `maxmemory-samples` 3/5/10.
- Tiêu chí: bảng kết quả; giải thích; thử `noeviction` để thấy lỗi OOM phía client Java.

<details>
<summary>Gợi ý lời giải</summary>

**5.1**
```java
public final class RedisKeys {
  private static final String P = "shop";
  public static String product(long id) { return P + ":catalog:product:v2:" + id; }      // String JSON, TTL 10m ± jitter
  public static String cart(long uid)   { return P + ":cart:" + uid; }                    // Hash, TTL 7d (gia hạn khi sửa)
  public static String otp(String phone){ return P + ":otp:" + phone; }                    // String, TTL 120s
  public static String loginRl(String ip){ return P + ":rl:login:" + ip; }                 // ZSet sliding window, TTL 60s
}
```
**5.2** Với `hz` cao, active expiry chạy thường xuyên hơn → bộ nhớ giảm nhanh hơn, CPU nền cao hơn chút.

**5.3** Workload lệch ổn định: LFU ≥ LRU > random; samples tăng → hit ratio LRU tăng nhẹ. `noeviction`: Lettuce ném `RedisCommandExecutionException: OOM command not allowed when used memory > 'maxmemory'`.
</details>

---

<a id="phan-6"></a>
## 6. Persistence: RDB, AOF, hybrid

### 6.1 RDB (snapshot)
- Chụp toàn bộ dataset ra file nhị phân `dump.rdb` theo lịch (`save 3600 1 300 100 60 10000`) hoặc thủ công `BGSAVE`.
- Cơ chế: `fork()` → process con ghi snapshot từ bộ nhớ dùng chung **copy-on-write**; process cha tiếp tục phục vụ. Trang nào bị ghi trong lúc snapshot thì được copy → workload ghi nặng có thể làm bộ nhớ tăng tới ~2x trong lúc BGSAVE.
- **Ưu**: file gọn, khởi động lại nhanh, tiện backup/chuyển sang nơi khác, dùng cho full sync replica. **Nhược**: mất dữ liệu kể từ snapshot cuối (vài phút); fork tốn kém với dataset lớn.
- `SAVE` (đồng bộ) **chặn server** — không dùng trên production.

### 6.2 AOF (Append Only File)
- Ghi lại **mỗi lệnh ghi** vào file. Khôi phục bằng cách phát lại.
- `appendfsync`:
  - `always`: fsync mỗi lệnh ghi — an toàn nhất, chậm nhất.
  - `everysec` (**mặc định, khuyến nghị**): fsync mỗi giây bởi background thread — mất tối đa ~1 giây dữ liệu (có thể hơn khi đĩa bị nghẽn).
  - `no`: để OS quyết định (thường ~30 giây trên Linux).
- **AOF rewrite** (`BGREWRITEAOF`, tự động theo `auto-aof-rewrite-percentage 100` / `auto-aof-rewrite-min-size 64mb`): fork tạo file AOF mới **tối giản** từ trạng thái hiện tại (1 triệu `INCR` → một lệnh `SET`).
- **Redis 7 multi-part AOF**: thư mục `appendonlydir` gồm một file *base* + các file *incremental* + manifest, rewrite không còn cần buffer gộp lớn như trước.
- **Hybrid** (`aof-use-rdb-preamble yes`, mặc định bật từ 5.0): phần base của AOF ở định dạng RDB (load nhanh) + phần đuôi là lệnh AOF (mất ít dữ liệu).

| | RDB | AOF (everysec) | Hybrid |
|---|---|---|---|
| Mất dữ liệu khi crash | Vài phút | ~1 giây | ~1 giây |
| Kích thước file | Nhỏ | Lớn hơn (giảm nhờ rewrite) | Trung bình |
| Tốc độ restart | Nhanh | Chậm hơn (phát lại lệnh) | Nhanh |
| Ảnh hưởng hiệu năng | Fork định kỳ | fsync mỗi giây + fork khi rewrite | Như AOF |

> 💡 **Góc nhìn Senior:**
> - Redis làm **cache thuần** có thể tắt persistence hoàn toàn (`save ""`, `appendonly no`) — restart là cache trống, chấp nhận được nếu hệ thống chịu được cold start. Nhưng **cẩn thận**: master tắt persistence + tự động restart → master trống → replica đồng bộ theo → **xóa sạch dữ liệu ở replica**. Redis docs cảnh báo rõ trường hợp này.
> - Redis **không phải** DB bền vững kiểu PostgreSQL: kể cả `appendfsync always`, replication vẫn async → failover có thể mất ghi đã xác nhận. Lệnh `WAIT numreplicas timeout` giảm rủi ro nhưng không biến Redis thành hệ thống strongly consistent.
> - Backup: copy file RDB định kỳ ra object storage; thử **restore** thường xuyên.

> ⚠️ **Lỗi thường gặp:** để disk đầy → AOF ghi lỗi → Redis từ chối ghi (`MISCONF`); fork thất bại do `vm.overcommit_memory=0` → bật `vm.overcommit_memory = 1` như Redis khuyến cáo.

### 🛠 Bài tập phần 6

**Bài 6.1 — Thử nghiệm mất dữ liệu (Cơ bản)**
- Đề bài: với 3 cấu hình (chỉ RDB `save 60 1000`, AOF everysec, không persistence), ghi counter tăng liên tục rồi `docker kill -s KILL redis` và khởi động lại.
- Tiêu chí: ghi lại giá trị trước/sau, kết luận lượng dữ liệu mất cho mỗi cấu hình.

**Bài 6.2 — AOF rewrite (Trung bình)**
- Đề bài: thực hiện 1 triệu `INCR` trên cùng key, đo kích thước thư mục AOF; chạy `BGREWRITEAOF` và đo lại; xem nội dung file base/incr và manifest.
- Tiêu chí: giải thích cấu trúc multi-part AOF và vai trò RDB preamble.

**Bài 6.3 — Copy-on-write & latency fork (Nâng cao)**
- Đề bài: nạp ~2GB dữ liệu, chạy workload ghi nặng (`redis-benchmark -t set -r 1000000`) đồng thời `BGSAVE`. Theo dõi `INFO persistence` (`rdb_last_cow_size`), `INFO stats` (`latest_fork_usec`), RSS của process.
- Tiêu chí: giải thích số liệu; tính toán nên đặt `maxmemory` bao nhiêu trên máy 16GB.

<details>
<summary>Gợi ý lời giải</summary>

**6.1** RDB: mất mọi thay đổi kể từ snapshot cuối; AOF everysec: mất ≤ ~1s; không persistence: mất hết.

**6.3** `latest_fork_usec` tăng theo kích thước bộ nhớ (copy page table, ~10–20ms/GB tùy phần cứng/ảo hóa). `rdb_last_cow_size` cho biết bộ nhớ copy thêm. Máy 16GB: chừa cho OS + COW + buffer → `maxmemory` ~ 8–10GB tùy tỷ lệ ghi.
</details>

---

<a id="phan-7"></a>
## 7. Replication, Sentinel & Redis Cluster

### 7.1 Replication
- `REPLICAOF host port`. Master gửi luồng lệnh ghi **bất đồng bộ** tới replica.
- **Full sync**: replica mới (hoặc lệch quá xa) → master `BGSAVE` → gửi RDB → gửi các lệnh tích lũy trong lúc đó. Có thể dùng diskless (`repl-diskless-sync yes`, mặc định từ 7.0).
- **Partial sync (PSYNC)**: replica gửi `replication ID + offset`; nếu offset vẫn nằm trong **replication backlog** (`repl-backlog-size`, mặc định 1MB — **thường quá nhỏ**, nên tăng lên hàng trăm MB) thì chỉ gửi phần thiếu.
- Replica mặc định **read-only**; đọc từ replica = chấp nhận dữ liệu trễ.
- `min-replicas-to-write 1` + `min-replicas-max-lag 10`: master từ chối ghi khi không đủ replica khỏe → hạn chế mất dữ liệu khi master bị cô lập.

### 7.2 Sentinel (HA cho mô hình master–replica)
- Tiến trình Sentinel (≥ 3, đặt ở các máy/zone khác nhau) giám sát master/replica, **tự động failover** và đóng vai **service discovery**: client hỏi Sentinel "master của `mymaster` là ai?".
- **SDOWN** (subjectively down): một Sentinel không nhận được phản hồi trong `down-after-milliseconds`. **ODOWN** (objectively down): số Sentinel đồng ý ≥ **quorum**. Để thực sự failover, một Sentinel phải được **đa số** Sentinel bầu làm leader → chọn replica tốt nhất (priority, offset replication lớn nhất) → `REPLICAOF NO ONE` → cấu hình lại các replica khác.
- Không scale ghi — vẫn chỉ một master.

```properties
# sentinel.conf
sentinel monitor mymaster 10.0.0.1 6379 2
sentinel down-after-milliseconds mymaster 5000
sentinel failover-timeout mymaster 60000
```

```yaml
# Spring Boot
spring.data.redis.sentinel:
  master: mymaster
  nodes: 10.0.0.11:26379,10.0.0.12:26379,10.0.0.13:26379
```

### 7.3 Redis Cluster (sharding + HA)
- Keyspace chia thành **16384 hash slot**: `slot = CRC16(key) mod 16384`. Mỗi master giữ một tập slot; mỗi master có replica; failover tự động bởi chính các node (không cần Sentinel).
- **Vì sao 16384?** (antirez): các node gửi heartbeat chứa bitmap slot của mình; 16384 bit = 2KB — đủ nhỏ cho gói tin gossip; và cluster được thiết kế cho tối đa ~1000 master nên 16384 slot là đủ mịn.
- Node giao tiếp qua **cluster bus** (port thường = port dữ liệu + 10000) bằng giao thức gossip.
- **Hash tag**: chỉ phần trong `{...}` đầu tiên được băm → `{user:7}:cart` và `{user:7}:profile` cùng slot.
- **Redirect**:
  - `MOVED <slot> <host:port>`: slot đã thuộc node khác **vĩnh viễn** → client cập nhật bảng slot cache và gửi lại.
  - `ASK <slot> <host:port>`: slot **đang migrate**; key có thể đã chuyển sang node đích → client gửi `ASKING` rồi lệnh tới node đích **chỉ cho lần này**, không cập nhật bảng slot.
  - Smart client (Lettuce, Jedis Cluster, Redisson) tự xử lý; Lettuce nên bật **topology refresh** (`enablePeriodicRefresh`, `enableAllAdaptiveRefreshTriggers`) để không bị kẹt sau failover.
- **Giới hạn**:
  - Lệnh multi-key (`MGET`, `MSET`, `SINTER`, `RENAME`, transaction, Lua) chỉ chạy khi **mọi key cùng slot** — nếu không: lỗi `CROSSSLOT Keys in request don't hash to the same slot`.
  - Chỉ có database 0 (`SELECT` không dùng được).
  - Cluster có thể mất ghi đã xác nhận (async replication) trong lúc failover/partition.
- Thêm/bớt node: di chuyển slot (`redis-cli --cluster reshard`/`rebalance`) — online.

```bash
redis-cli -c -p 7000 SET user:7 "x"      # -c: theo redirect
redis-cli -p 7000 CLUSTER KEYSLOT "{user:7}:cart"
redis-cli -p 7000 CLUSTER NODES
redis-cli -p 7000 MSET a 1 b 2           # (error) CROSSSLOT ...
redis-cli -p 7000 MSET {u7}:a 1 {u7}:b 2 # OK
```

> 💡 **Góc nhìn Senior:**
> - Chọn mô hình: dữ liệu vừa một máy và chỉ cần HA → **Sentinel** (đơn giản hơn, mọi lệnh multi-key hoạt động). Cần vượt RAM/throughput một máy → **Cluster**. Dịch vụ managed (ElastiCache, Azure Cache, Memorystore) che bớt vận hành nhưng hành vi failover vẫn phải hiểu.
> - Lạm dụng hash tag (mọi key của một tenant lớn cùng một tag) → **dồn hết vào một slot** → hot shard. Hash tag chỉ cho nhóm key nhỏ cần atomic cùng nhau.
> - Đọc từ replica (`ReadFrom.REPLICA_PREFERRED` trong Lettuce) tăng throughput đọc nhưng chấp nhận stale — không dùng cho lock, rate limit, counter cần chính xác.

> ⚠️ **Lỗi thường gặp:** client cấu hình cứng IP master (không qua Sentinel) → failover xong app vẫn ghi vào node cũ (giờ là replica, lỗi `READONLY`); Lua script truy cập key không truyền qua `KEYS[]` (cluster không định tuyến được); dùng `KEYS`/`SCAN` trên cluster mà chỉ quét một node.

### 🛠 Bài tập phần 7

**Bài 7.1 — Master–replica (Cơ bản)**
- Đề bài: Docker Compose 1 master + 2 replica. Ghi trên master, đọc trên replica; `INFO replication` xem offset. Thử ghi trên replica.
- Tiêu chí: giải thích lỗi `READONLY`; đo lag khi ghi nặng.

**Bài 7.2 — Sentinel failover với Spring Boot (Trung bình)**
- Đề bài: thêm 3 Sentinel; app Spring Boot ghi counter mỗi 100ms qua Sentinel. `docker stop` master.
- Tiêu chí: đo thời gian app ghi lỗi; số giá trị bị mất; log của Sentinel (`+sdown`, `+odown`, `+switch-master`). Giải thích vì sao có thể mất ghi.

**Bài 7.3 — Cluster, hash tag & resharding (Nâng cao)**
- Đề bài: tạo cluster 3 master + 3 replica (`redis-cli --cluster create ... --cluster-replicas 1`). Viết app Lettuce ghi/đọc liên tục 10.000 key; trong lúc chạy, thêm master thứ 4 và reshard 4000 slot.
- Tiêu chí: app không lỗi (hoặc lỗi được retry) trong lúc reshard; log được ít nhất một `MOVED`/`ASK`; hiện thực thao tác giỏ hàng atomic (Lua) trên 2 key bằng hash tag.

<details>
<summary>Gợi ý lời giải</summary>

**7.2** Thời gian gián đoạn ≈ `down-after-milliseconds` + thời gian bầu leader & promote (vài giây). Ghi đã được master cũ xác nhận nhưng chưa replicate sẽ mất.

**7.3**
```java
ClusterTopologyRefreshOptions topo = ClusterTopologyRefreshOptions.builder()
    .enablePeriodicRefresh(Duration.ofSeconds(30)).enableAllAdaptiveRefreshTriggers().build();
RedisClusterClient client = RedisClusterClient.create("redis://localhost:7000");
client.setOptions(ClusterClientOptions.builder().topologyRefreshOptions(topo).build());
```
Key giỏ hàng: `cart:{u7}:items`, `cart:{u7}:meta` → cùng slot, chạy chung Lua được.
</details>

---

<a id="phan-8"></a>
## 8. Transaction, Lua script & pipelining

### 8.1 MULTI/EXEC/WATCH
```bash
MULTI
DECRBY stock:sku1 1
LPUSH orders:pending "1001"
EXEC          # các lệnh xếp hàng và chạy liên tiếp, không xen lệnh client khác
```
- **Atomic về mặt cô lập** (không xen kẽ), nhưng **không có rollback**: nếu một lệnh lỗi lúc chạy (ví dụ `INCR` trên Hash), các lệnh khác **vẫn được thực thi**. Lỗi cú pháp lúc xếp hàng thì cả transaction bị hủy.
- Không thể dùng kết quả của lệnh trước làm input lệnh sau bên trong MULTI (vì kết quả chỉ có lúc EXEC).
- **WATCH** = optimistic locking (CAS): nếu key bị sửa bởi client khác giữa `WATCH` và `EXEC` → `EXEC` trả về null, client tự retry.

```bash
WATCH balance:7
GET balance:7            # đọc 100 ở client
MULTI
SET balance:7 90
EXEC                     # (nil) nếu ai đó đã sửa balance:7 → retry
```

### 8.2 Lua script (EVAL) & Redis Functions
- Script được thực thi **atomic** trên thread chính — không lệnh nào xen vào. Cho phép logic điều kiện (đọc → quyết định → ghi) mà MULTI không làm được.
- `EVAL script numkeys key... arg...`; `SCRIPT LOAD` + `EVALSHA` để không gửi lại body (client lib tự xử lý `NOSCRIPT`).
- **Quy tắc**: mọi key truy cập phải truyền qua `KEYS` (để Cluster định tuyến); script phải nhanh (chạy lâu chặn toàn bộ server; `lua-time-limit`/`busy-reply-threshold` 5s chỉ cho phép `SCRIPT KILL` nếu script chưa ghi); script nên **deterministic**.
- Redis 7: **Functions** (`FUNCTION LOAD`, `FCALL`) — script được lưu, replicate và persist như dữ liệu, có tên & thư viện; thay thế dần EVAL cho logic dùng chung.

```bash
# Trừ kho có điều kiện (không âm) — atomic
EVAL "local s = tonumber(redis.call('GET', KEYS[1]) or '0')
      if s >= tonumber(ARGV[1]) then return redis.call('DECRBY', KEYS[1], ARGV[1]) else return -1 end" 1 stock:sku1 2
```

### 8.3 Pipelining
- Gửi nhiều lệnh **không chờ** từng phản hồi → tiết kiệm RTT. 10.000 lệnh × RTT 0,5ms = 5s; pipeline còn vài chục ms.
- Pipeline **không atomic** (lệnh client khác có thể xen giữa) — khác MULTI. Có thể kết hợp MULTI trong pipeline.
- Chia batch hợp lý (vài trăm–vài nghìn lệnh) để không chiếm quá nhiều bộ nhớ output buffer.

```java
// Spring Data Redis
List<Object> results = redisTemplate.executePipelined((RedisCallback<Object>) conn -> {
  for (long id : ids) conn.stringCommands().get(("catalog:product:v2:" + id).getBytes(StandardCharsets.UTF_8));
  return null;
});
// Lettuce: API async tự nhiên pipelining trên cùng kết nối; có thể setAutoFlushCommands(false) + flushCommands()
```

| | MULTI/EXEC | Lua | Pipeline |
|---|---|---|---|
| Atomic (không xen) | ✅ | ✅ | ❌ |
| Logic điều kiện bên trong | ❌ (chỉ WATCH) | ✅ | ❌ |
| Rollback | ❌ | ❌ (ghi trước lỗi vẫn giữ) | ❌ |
| Giảm RTT | Một phần | ✅ (1 round-trip) | ✅ |
| Cluster | Cùng slot | Cùng slot | Mỗi lệnh đi đúng node (client tách) |

### 🛠 Bài tập phần 8

**Bài 8.1 — MULTI không rollback (Cơ bản)**
- Đề bài: tạo MULTI gồm `SET a 1`, `HSET a f v` (sai kiểu), `SET b 2`. Quan sát kết quả `EXEC` và giá trị `a`, `b`. Thử thêm lệnh sai cú pháp.
- Tiêu chí: giải thích khác biệt lỗi lúc xếp hàng vs lúc thực thi.

**Bài 8.2 — WATCH vs Lua (Trung bình)**
- Đề bài: chuyển điểm thưởng giữa 2 user (không âm) bằng (a) WATCH + MULTI có retry, (b) Lua. Chạy 50 thread đồng thời.
- Tiêu chí: tổng điểm bảo toàn; so sánh số lần retry của (a) và throughput hai cách.

**Bài 8.3 — Pipeline warm-up cache (Nâng cao)**
- Đề bài: nạp 1 triệu sản phẩm từ DB vào Redis khi khởi động. So sánh: từng lệnh `SET`, pipeline batch 1000, `MSET` theo batch (lưu ý không đặt TTL được với MSET), Lua batch. Trên Cluster thì sao?
- Tiêu chí: bảng thời gian; giải pháp đặt TTL kèm jitter; xử lý cross-slot khi chạy trên Cluster.

<details>
<summary>Gợi ý lời giải</summary>

**8.1** `EXEC` trả `OK`, `WRONGTYPE...`, `OK` → `a=1`, `b=2` vẫn được ghi. Lỗi cú pháp (`SET a`) lúc xếp hàng → `EXECABORT`.

**8.2**
```lua
-- KEYS[1]=pts:{from} KEYS[2]=pts:{to} ARGV[1]=amount
local f = tonumber(redis.call('GET', KEYS[1]) or '0')
local a = tonumber(ARGV[1])
if f < a then return 0 end
redis.call('DECRBY', KEYS[1], a); redis.call('INCRBY', KEYS[2], a)
return 1
```
Trên Cluster: hai user khác slot → không thể; hoặc dùng hash tag chung (không thực tế với user bất kỳ) → chuyển điểm nên làm ở DB.

**8.3** Pipeline `SET k v EX ttl` với `ttl = base + random(0..base/10)`. Cluster: nhóm key theo slot (`SlotHash.getSlot`) hoặc để Lettuce tự chia lệnh đơn trong pipeline async.
</details>

---

<a id="phan-9"></a>
## 9. Các caching pattern

### 9.1 Cache-aside (lazy loading) — phổ biến nhất
Ứng dụng tự quản lý cache:
- **Đọc**: tìm cache → hit thì trả; miss thì đọc DB → ghi cache (kèm TTL) → trả.
- **Ghi**: ghi DB → **xóa** key cache (không cập nhật — xem phần 10).

```java
public Product getProduct(long id) {
  String key = RedisKeys.product(id);
  String json = redis.opsForValue().get(key);
  if (json != null) return NULL_MARKER.equals(json) ? null : mapper.readValue(json, Product.class);
  Product p = productRepository.findById(id).orElse(null);
  Duration ttl = p == null ? Duration.ofMinutes(1) : jitter(Duration.ofMinutes(10));   // null caching chống penetration
  redis.opsForValue().set(key, p == null ? NULL_MARKER : mapper.writeValueAsString(p), ttl);
  return p;
}

@Transactional
public void updateProduct(Product p) {
  productRepository.save(p);
  // Xóa cache SAU KHI commit (xem 10.2): nếu xóa trước commit, request khác có thể nạp lại dữ liệu cũ
  TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
    @Override public void afterCommit() { redis.delete(RedisKeys.product(p.getId())); }
  });
}
```
Ưu: đơn giản, chỉ cache dữ liệu thực sự được đọc, Redis chết thì vẫn chạy (chậm). Nhược: miss đầu tiên chậm; có cửa sổ không nhất quán; logic cache rải trong code.

### 9.2 Read-through
Cache tự nạp từ nguồn khi miss — ứng dụng chỉ nói chuyện với cache. Ví dụ: Caffeine `LoadingCache`, Spring `@Cacheable` (về mặt sử dụng), Redisson `RMapCache` với `MapLoader`. Code gọn, logic nạp tập trung.

### 9.3 Write-through
Ghi vào cache, cache **đồng bộ** ghi xuống DB rồi mới xác nhận. Dữ liệu cache luôn mới; nhưng ghi chậm hơn (2 nơi) và cache chứa cả dữ liệu không ai đọc. Hiếm khi dùng thuần với Redis (Redis không tự ghi xuống DB); Redisson `MapWriter` hỗ trợ mô hình này.

### 9.4 Write-behind (write-back)
Ghi vào cache, xác nhận ngay, **ghi xuống DB bất đồng bộ** theo lô. Throughput ghi rất cao (gộp nhiều cập nhật — ví dụ đếm lượt xem, like); rủi ro **mất dữ liệu** nếu cache chết trước khi flush, khó đảm bảo thứ tự & nhất quán. Dùng cho dữ liệu chấp nhận mất mát nhỏ hoặc có cơ chế bền (Redis AOF + Stream làm buffer).

### 9.5 Refresh-ahead
Chủ động làm mới entry **trước khi** hết hạn (khi entry được truy cập và đã gần hết hạn). Caffeine `refreshAfterWrite` là hiện thực tiêu biểu; với Redis có thể dùng "logical expiration" (phần 11.2) hoặc **probabilistic early expiration** (XFetch): mỗi lần đọc, với xác suất tăng dần khi gần hết hạn, một request tự nạp lại trước.

| Pattern | Đọc | Ghi | Nhất quán | Rủi ro chính | Ví dụ dùng |
|---|---|---|---|---|---|
| Cache-aside | App nạp khi miss | DB rồi xóa cache | Eventual, cửa sổ nhỏ | Stampede khi miss | Đa số use case |
| Read-through | Cache tự nạp | — | Như trên | Phụ thuộc thư viện | Caffeine, Spring Cache |
| Write-through | Luôn hit | Cache + DB đồng bộ | Mạnh hơn | Ghi chậm, cache phình | Cấu hình, dữ liệu đọc ngay sau ghi |
| Write-behind | Luôn hit | Cache rồi DB bất đồng bộ | Yếu | Mất dữ liệu | Counter, view, like |
| Refresh-ahead | Gần như luôn hit | — | Tốt cho dữ liệu nóng | Nạp thừa dữ liệu nguội | Trang chủ, giá, tỷ giá |

### 🛠 Bài tập phần 9

**Bài 9.1 — Cache-aside chuẩn (Cơ bản)**
- Đề bài: hiện thực `ProductService` cache-aside như trên với `StringRedisTemplate` + Jackson.
- Tiêu chí: test Testcontainers (Redis + PostgreSQL) chứng minh: lần 2 không gọi DB; sau update đọc thấy giá trị mới; sản phẩm không tồn tại không gọi DB lần 2 trong 1 phút.

**Bài 9.2 — Write-behind cho lượt xem (Trung bình)**
- Đề bài: lượt xem bài viết `HINCRBY views:pending {postId} 1`; job mỗi 5s đọc & reset hash một cách atomic (`RENAME` sang key tạm hoặc Lua `HGETALL`+`DEL`), batch `UPDATE posts SET views = views + ?` xuống DB.
- Tiêu chí: 1 triệu lượt xem đồng thời → DB nhận đúng tổng; job chết giữa chừng không mất dữ liệu đã lấy ra (gợi ý: rename sang `views:processing:{ts}`, xóa chỉ sau khi DB commit; job sau xử lý lại key processing còn sót — chấp nhận at-least-once hay cần idempotency?).

**Bài 9.3 — Probabilistic early expiration (Nâng cao)**
- Đề bài: hiện thực XFetch: lưu kèm `delta` (thời gian tính toán) và `expiry`; mỗi lần đọc nếu `now - delta * beta * ln(rand()) >= expiry` thì tự recompute.
- Tiêu chí: mô phỏng 1.000 req/s trên 1 key TTL 10s, recompute 500ms — so sánh số lần recompute đồng thời và p99 so với cache-aside thường.

<details>
<summary>Gợi ý lời giải</summary>

**9.2**
```lua
-- KEYS[1]=views:pending KEYS[2]=views:processing:<ts>
if redis.call('EXISTS', KEYS[1]) == 0 then return 0 end
redis.call('RENAME', KEYS[1], KEYS[2])
return 1
```
Sau đó `HGETALL` key processing → batch update DB → `DEL`. Nếu job chết sau commit DB nhưng trước `DEL` → cộng trùng. Để chính xác tuyệt đối: ghi `batch_id` vào bảng `applied_batches` trong cùng transaction DB và bỏ qua batch đã áp dụng.

**9.3**
```java
boolean shouldRecompute(long deltaMs, long expiryMs, double beta) {
  return System.currentTimeMillis() - deltaMs * beta * Math.log(ThreadLocalRandom.current().nextDouble()) >= expiryMs;
}
```
`ln(rand)` âm → vế trái tăng ngẫu nhiên; càng gần hết hạn xác suất càng cao; thường chỉ một vài request recompute sớm, không có stampede.
</details>

---

<a id="phan-10"></a>
## 10. Nhất quán giữa DB và cache

DB và Redis là **hai hệ thống**, không có transaction chung → luôn tồn tại cửa sổ không nhất quán. Mục tiêu: **thu nhỏ cửa sổ** và đảm bảo **cuối cùng sẽ hội tụ** (TTL là lưới an toàn cuối).

### 10.1 Cập nhật cache hay xóa cache?

**Cập nhật cache khi ghi** (`DB update → cache set`) có race:
```
T1: update DB price=100          T2: update DB price=200
                                 T2: set cache price=200
T1: set cache price=100   ← cache = 100, DB = 200 → sai cho tới khi hết TTL
```
Ngoài ra lãng phí khi dữ liệu ghi nhiều đọc ít, và giá trị cache có thể là kết quả tính toán từ nhiều bảng.

**Xóa cache** (`DB update → cache delete`) — khuyến nghị (cách Facebook mô tả trong paper *Scaling Memcache at Facebook*): lần đọc sau tự nạp giá trị mới. Vẫn còn một race hiếm:
```
Cache trống.  T1 (đọc): miss → đọc DB (giá cũ=100)
              T2 (ghi): update DB=200 → delete cache
              T1: set cache = 100   ← cũ!
```
Xảy ra khi T1 đọc DB trước khi T2 ghi nhưng ghi cache sau khi T2 xóa — hiếm vì ghi cache thường nhanh hơn ghi DB, nhưng có thể xảy ra với GC pause, mạng chậm.

**Thứ tự "xóa cache rồi ghi DB"** thì tệ hơn: giữa hai bước, request đọc nạp lại giá trị cũ vào cache và giữ tới hết TTL.

### 10.2 Các kỹ thuật thu nhỏ cửa sổ

1. **Xóa sau khi commit** (`afterCommit`/`@TransactionalEventListener(phase = AFTER_COMMIT)`), không xóa bên trong transaction chưa commit — nếu không, request khác có thể nạp lại dữ liệu cũ (chưa commit) ngay sau khi xóa.
2. **Delayed double delete**: xóa → ghi DB → chờ một khoảng (lớn hơn thời gian một lần đọc DB + set cache, ví dụ 500ms–1s) → xóa lần nữa (bất đồng bộ, qua scheduled executor hoặc delay queue). Giảm race 10.1 và race do replica lag (đọc từ replica cũ rồi nạp vào cache).
3. **Retry xóa khi thất bại**: xóa cache lỗi (Redis timeout) → đưa vào queue retry; nếu không, cache cũ sống tới hết TTL.
4. **CDC-based invalidation**: đọc binlog/WAL (Debezium; Canal cho MySQL) → consumer xóa/cập nhật cache. Ưu: tách khỏi code nghiệp vụ, không sót đường ghi nào (kể cả ghi tay bằng SQL, job batch), có retry & thứ tự theo log. Nhược: thêm hạ tầng (Kafka...), độ trễ vài trăm ms.
5. **Version/timestamp**: lưu `version` cùng giá trị cache, chỉ ghi cache nếu version mới hơn (Lua so sánh) → chống ghi đè bằng dữ liệu cũ.
6. **Lease** (Facebook memcache): khi miss, cache cấp một token; delete làm token mất hiệu lực → set bằng token cũ bị từ chối.
7. **TTL ngắn** cho dữ liệu nhạy cảm + chấp nhận eventual consistency.

```java
// Delayed double delete
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void onProductChanged(ProductChangedEvent e) {
  String key = RedisKeys.product(e.id());
  redis.delete(key);
  scheduler.schedule(() -> redis.delete(key), 1, TimeUnit.SECONDS);
}
```

```lua
-- Chỉ ghi cache nếu version mới hơn. KEYS[1]=key ARGV[1]=version ARGV[2]=json ARGV[3]=ttl
local cur = redis.call('HGET', KEYS[1], 'v')
if cur and tonumber(cur) >= tonumber(ARGV[1]) then return 0 end
redis.call('HSET', KEYS[1], 'v', ARGV[1], 'd', ARGV[2])
redis.call('EXPIRE', KEYS[1], ARGV[3])
return 1
```

> 💡 **Góc nhìn Senior:** Không có giải pháp nào cho nhất quán **mạnh** giữa DB và cache mà vẫn rẻ. Câu trả lời tốt trong thiết kế: (1) phân loại dữ liệu — số dư tài khoản, tồn kho lúc thanh toán **đọc từ DB** (hoặc Redis là nguồn sự thật duy nhất), mô tả sản phẩm chấp nhận stale vài giây; (2) dùng cache-aside + delete after commit + TTL; (3) thêm CDC khi có nhiều đường ghi; (4) có công cụ **purge thủ công** cho vận hành.

> ⚠️ **Lỗi thường gặp:** `@CacheEvict` trên method `@Transactional` — mặc định evict chạy **sau khi method trả về**, tức là cùng lúc proxy transaction commit; nếu thứ tự proxy (advisor order) không đúng, evict có thể xảy ra **trước commit**. Ngoài ra `RedisCacheManager` có tùy chọn `transactionAware()` để trì hoãn put/evict tới sau commit.

### 🛠 Bài tập phần 10

**Bài 10.1 — Tái hiện race update-cache (Cơ bản)**
- Đề bài: viết test hai thread cập nhật giá cùng sản phẩm theo chiến lược "update DB → set cache", chèn `Thread.sleep` để tạo thứ tự xấu.
- Tiêu chí: chứng minh cache ≠ DB; đổi sang "update DB → delete cache" và chứng minh hết lỗi trong kịch bản này.

**Bài 10.2 — Race của delete + replica lag (Trung bình)**
- Đề bài: dùng PostgreSQL primary/replica (từ Module 11, bài 10.2, `recovery_min_apply_delay = 1s`); đọc cache-miss từ replica. Tái hiện việc cache bị nạp lại giá trị cũ sau khi đã xóa. Sửa bằng delayed double delete.
- Tiêu chí: test tái hiện được ổn định; chọn delay hợp lý và giải thích.

**Bài 10.3 — CDC invalidation (Nâng cao)**
- Đề bài: Docker Compose: MySQL (binlog ROW) hoặc PostgreSQL (logical replication) + Debezium Server/Kafka Connect + Kafka + Redis. Consumer Spring Kafka xóa key `catalog:product:v2:{id}` khi nhận change event bảng `products`.
- Tiêu chí: cập nhật bằng SQL tay (không qua app) vẫn làm cache được invalidate trong < 2s; consumer idempotent; xử lý khi Redis tạm thời không khả dụng (retry/DLT).

<details>
<summary>Gợi ý lời giải</summary>

**10.1** Dùng `CountDownLatch` để ép thứ tự: T1 ghi DB → chờ → T2 ghi DB + set cache → T1 set cache.

**10.2** Delay ≥ replica lag tối đa + thời gian nạp cache. Với lag 1s, delay 1,5–2s. Giải pháp tốt hơn: đọc từ primary khi cache miss cho dữ liệu vừa đổi, hoặc không đọc replica cho đường nạp cache.

**10.3**
```java
@KafkaListener(topics = "dbserver1.shop.products")
public void onChange(ConsumerRecord<String, String> rec) {
  JsonNode payload = mapper.readTree(rec.value()).path("payload");
  JsonNode row = payload.path("op").asText().equals("d") ? payload.path("before") : payload.path("after");
  redis.delete(RedisKeys.product(row.path("id").asLong()));   // delete là idempotent
}
```
Cấu hình `DefaultErrorHandler` với backoff + `DeadLetterPublishingRecoverer`.
</details>

---

<a id="phan-11"></a>
## 11. Sự cố cache: penetration, breakdown, avalanche, big/hot key

### 11.1 Cache penetration (xuyên thủng) — truy vấn dữ liệu **không tồn tại**
Request với id không có trong DB (do bug hoặc tấn công: `id=-1`, id ngẫu nhiên) → luôn miss → luôn chạm DB.

Giải pháp:
- **Cache giá trị null** (null object/marker) với TTL ngắn (30s–5 phút). Nhược: tốn bộ nhớ nếu kẻ tấn công dùng vô số id → kết hợp các cách dưới.
- **Bloom filter**: cấu trúc xác suất trả lời "chắc chắn không có" hoặc "có thể có" (false positive, **không** có false negative). Nạp toàn bộ id hợp lệ vào bloom filter; request id không có trong filter → trả 404 ngay. Redis Stack/Redis 8 có module **RedisBloom** (`BF.ADD`, `BF.EXISTS`); Redisson có `RBloomFilter` (dựa trên Bitmap); hoặc Guava `BloomFilter` ở local. Lưu ý: bloom filter chuẩn **không xóa** được phần tử (cần rebuild định kỳ hoặc dùng Cuckoo filter).
- **Validate input** từ sớm (định dạng id, phạm vi), rate limit theo IP/user.

```java
RBloomFilter<Long> bf = redisson.getBloomFilter("bf:product-ids");
bf.tryInit(10_000_000L, 0.01);          // 10 triệu phần tử, false positive 1% (≈ 11,4MB)
bf.add(42L);
if (!bf.contains(id)) return Optional.empty();   // chắc chắn không tồn tại
```

### 11.2 Cache breakdown (hotspot key hết hạn) — một **key nóng** hết hạn
Key cực nóng (sản phẩm flash sale, 50.000 req/s) hết hạn → hàng nghìn request đồng thời miss → cùng truy vấn DB và cùng tính lại (**cache stampede / thundering herd / dog-piling**).

Giải pháp:
- **Mutex / single-flight**: chỉ một request được nạp lại; các request khác chờ ngắn rồi đọc cache, hoặc trả giá trị cũ.
  - Trong một JVM: Caffeine `get(key, loader)` hoặc `ConcurrentHashMap.computeIfAbsent` của `CompletableFuture` (request coalescing).
  - Giữa nhiều instance: lock Redis `SET lock:key token NX PX 3000`.
- **Logical expiration**: key Redis **không đặt TTL** (hoặc TTL rất dài); giá trị chứa trường `expireAt` logic. Khi đọc thấy đã quá `expireAt` → vẫn trả giá trị cũ ngay, đồng thời một request (lấy được mutex) nạp lại bất đồng bộ. Người dùng không bao giờ chờ; đổi lại chấp nhận stale ngắn.
- **Không bao giờ để key nóng hết hạn**: refresh định kỳ bằng job (refresh-ahead), hoặc XFetch (bài 9.3).

```java
public Product getHot(long id) {
  String key = RedisKeys.product(id);
  CacheEntry<Product> e = read(key);                       // {data, expireAt}
  if (e != null && e.expireAt().isAfter(Instant.now())) return e.data();
  String lockKey = "lock:" + key, token = UUID.randomUUID().toString();
  if (Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(lockKey, token, Duration.ofSeconds(5)))) {
    executor.submit(() -> {
      try { write(key, new CacheEntry<>(load(id), Instant.now().plusSeconds(60))); }
      finally { releaseLock(lockKey, token); }             // Lua compare-and-delete (phần 12)
    });
  }
  if (e != null) return e.data();                          // trả dữ liệu cũ trong lúc làm mới
  return loadWithShortWait(id);                            // lần đầu chưa có gì: chờ ngắn/đọc DB có giới hạn
}
```

### 11.3 Cache avalanche (tuyết lở) — **nhiều key** mất cùng lúc
Nguyên nhân: hàng loạt key cùng TTL hết hạn cùng thời điểm (nạp cache lúc deploy/warm-up với TTL cố định), hoặc **Redis sập/restart**.

Giải pháp:
- **TTL jitter**: `ttl = base + random(0, base × 10–20%)`.
- **HA cho Redis** (Sentinel/Cluster, nhiều AZ), persistence để restart có dữ liệu.
- **Cache nhiều tầng**: L1 Caffeine đỡ khi Redis có vấn đề.
- **Circuit breaker + fallback** (Resilience4j) quanh Redis và DB; **rate limit/bulkhead** để DB không bị đánh sập; trả dữ liệu mặc định/degraded.
- **Warm-up** có kiểm soát trước khi mở traffic.

### 11.4 Big key & hot key

**Big key**: String > ~10KB–1MB, hoặc Hash/List/Set/ZSet hàng trăm nghìn–triệu phần tử (ngưỡng tùy tổ chức).
Tác hại: lệnh trên key chậm và chặn server; mạng bị chiếm (đọc 5MB mỗi request); phân bố bộ nhớ lệch giữa các node Cluster; `DEL` chặn; migrate slot chậm/timeout.
Phát hiện: `redis-cli --bigkeys` (lấy mẫu bằng SCAN, báo key lớn nhất mỗi type), `redis-cli --memkeys` (7.0+), `MEMORY USAGE key`, phân tích file RDB offline (ví dụ `rdb-tools`/`redis-rdb-cli`).
Xử lý: chia nhỏ (hash bucket theo `id % N`), nén giá trị, chỉ lưu field cần thiết, xóa bằng `UNLINK` hoặc xóa dần bằng `HSCAN` + `HDEL`.

**Hot key**: một key nhận lượng truy cập lớn bất thường (sản phẩm flash sale, cấu hình toàn cục) → một node Cluster (một thread) quá tải trong khi node khác rảnh.
Phát hiện: `redis-cli --hotkeys` (cần policy LFU để có `OBJECT FREQ`), `MONITOR` trong thời gian ngắn (rất tốn — chỉ dùng thận trọng), thống kê phía client (Lettuce command latency / metrics), proxy.
Xử lý: **L1 local cache** cho key nóng (TTL vài giây); **nhân bản key** (`product:42:copy:{0..N-1}`, đọc ngẫu nhiên một bản, ghi cập nhật mọi bản); đọc từ replica; với counter ghi nóng — chia nhỏ counter (`counter:{0..N}`) rồi cộng khi đọc.

> 💡 **Góc nhìn Senior:** Phỏng vấn viên ở Việt Nam/Trung Quốc rất hay hỏi bộ ba **穿透 / 击穿 / 雪崩** (penetration / breakdown / avalanche). Hãy phân biệt rõ: penetration = dữ liệu **không tồn tại**; breakdown = **một** key nóng hết hạn; avalanche = **nhiều** key/toàn bộ cache mất. Mỗi loại có giải pháp khác nhau — và luôn kèm biện pháp bảo vệ DB (rate limit, circuit breaker).

> ⚠️ **Lỗi thường gặp:** mutex lock không có TTL → instance giữ lock chết → không ai nạp lại được nữa; request chờ mutex bằng vòng lặp `sleep` không giới hạn → cạn thread pool; cache null nhưng quên invalidate khi dữ liệu đó được tạo sau đó.

### 🛠 Bài tập phần 11

**Bài 11.1 — Chống penetration (Cơ bản)**
- Đề bài: tấn công `GET /products/{random-id}` 2.000 req/s với id không tồn tại. Đo số query DB/giây trước và sau khi thêm (a) null caching, (b) Bloom filter (Redisson hoặc Guava).
- Tiêu chí: số liệu trước/sau; tính bộ nhớ bloom filter cho 10 triệu id ở 1% và 0,1% false positive; cách đồng bộ bloom filter khi thêm sản phẩm mới.

**Bài 11.2 — Stampede & mutex (Trung bình)**
- Đề bài: key nóng TTL 5s, loader chậm 300ms, 1.000 req/s. Đo số lần loader được gọi mỗi lần key hết hạn với (a) cache-aside thường, (b) mutex Redis, (c) logical expiration.
- Tiêu chí: biểu đồ số lần gọi loader & p99 latency; nêu trade-off của mỗi cách.

**Bài 11.3 — Big key & hot key trong Cluster (Nâng cao)**
- Đề bài: trên Redis Cluster 3 master, tạo một Hash 2 triệu field và một String bị đọc 30.000 lần/giây. Phát hiện bằng `--bigkeys`, `--hotkeys` (đặt `maxmemory-policy allkeys-lfu`), `INFO commandstats`, CPU từng node. Khắc phục: chia Hash thành 1.000 bucket; hot key dùng L1 Caffeine 2s.
- Tiêu chí: số liệu CPU node trước/sau; xóa Hash lớn mà không làm latency vượt 10ms (gợi ý `UNLINK` hoặc `HSCAN`+`HDEL` theo lô).

<details>
<summary>Gợi ý lời giải</summary>

**11.1** Kích thước bloom filter: `m = -n·ln(p) / (ln 2)^2` → n=10^7, p=0,01: ≈ 9,6×10^7 bit ≈ 11,4MB, k ≈ 7 hàm băm; p=0,001: ≈ 17,1MB, k ≈ 10. Thêm sản phẩm mới: `bf.add(id)` ngay khi tạo (sau commit); rebuild định kỳ vì không xóa được.

**11.2** (a) mỗi lần hết hạn có ~300 lời gọi loader (1.000 req/s × 0,3s); (b) 1 lời gọi, các request khác chờ/retry → p99 tăng ~300ms; (c) 1 lời gọi, p99 không tăng, dữ liệu stale tối đa ~300ms.

**11.3**
```bash
redis-cli -c -p 7000 --bigkeys
redis-cli -p 7000 CONFIG SET maxmemory-policy allkeys-lfu && redis-cli -p 7000 --hotkeys
redis-cli -p 7000 INFO commandstats
```
</details>

---

<a id="phan-12"></a>
## 12. Distributed lock với Redis

### 12.1 Lock đơn instance đúng cách
```bash
SET lock:order:1001 <random-token> NX PX 30000
#   NX: chỉ đặt nếu chưa tồn tại   PX: tự hết hạn (chống deadlock khi client chết)
```
Giải phóng **an toàn**: chỉ xóa nếu token là của mình (tránh xóa lock của client khác khi lock của mình đã hết hạn và người khác đã lấy) — phải atomic bằng Lua:

```lua
if redis.call('GET', KEYS[1]) == ARGV[1] then
  return redis.call('DEL', KEYS[1])
else
  return 0
end
```

```java
public boolean tryLock(String key, String token, Duration ttl) {
  return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key, token, ttl));
}
private static final RedisScript<Long> UNLOCK = new DefaultRedisScript<>(
  "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end", Long.class);
public void unlock(String key, String token) { redis.execute(UNLOCK, List.of(key), token); }
```

**Sai lầm kinh điển:** `SETNX` rồi `EXPIRE` ở lệnh riêng (không atomic — chết giữa chừng là lock vĩnh viễn); `DEL` không kiểm tra token; TTL quá ngắn so với thời gian xử lý (lock hết hạn khi vẫn đang làm → hai client cùng giữ lock).

### 12.2 Redisson & watchdog
```java
RLock lock = redisson.getLock("lock:order:1001");
// Không truyền leaseTime → watchdog tự gia hạn
if (lock.tryLock(3, TimeUnit.SECONDS)) {          // chờ tối đa 3s
  try { process(); } finally { lock.unlock(); }
}
// Có leaseTime → KHÔNG có watchdog, tự hết hạn sau 10s:
lock.tryLock(3, 10, TimeUnit.SECONDS);
```
- **Watchdog**: khi không truyền `leaseTime`, Redisson đặt TTL = `lockWatchdogTimeout` (mặc định **30s**) và định kỳ (khoảng mỗi 1/3 thời gian đó, ~10s) gia hạn lại khi thread vẫn giữ lock. Client chết → không ai gia hạn → lock tự hết hạn sau ≤ 30s.
- Redisson lưu lock dưới dạng **Hash** (`field = UUID:threadId`, `value = số lần reentrant`) → hỗ trợ **reentrant**, và dùng Pub/Sub để thông báo cho client đang chờ thay vì polling.
- Các biến thể: `FairLock`, `ReadWriteLock`, `MultiLock`, `Semaphore`, `CountDownLatch`.
- `unlock()` từ thread khác thread giữ lock → `IllegalMonitorStateException`.

### 12.3 Redlock & tranh luận
**Vấn đề**: lock trên một master; master chết **trước khi** replicate key lock → replica được promote **không có** key → client khác lấy được lock → hai client cùng giữ lock.

**Redlock** (antirez): N (thường 5) master **độc lập**; client lấy lock trên từng node với timeout ngắn; thành công nếu lấy được trên **đa số** (≥ N/2+1) và tổng thời gian lấy < TTL; thời gian hiệu lực = TTL − thời gian đã tốn − độ trôi đồng hồ.

**Phản biện của Martin Kleppmann (2016)**:
1. **Process pause** (GC stop-the-world, swap, CPU bị tranh chấp) hoặc độ trễ mạng: client lấy lock, bị dừng lâu hơn TTL, lock hết hạn, client khác lấy lock, client đầu tỉnh dậy và **vẫn nghĩ mình giữ lock** → ghi đè. Không thuật toán lock dựa trên TTL nào tự giải quyết được điều này.
2. Redlock dựa vào **giả định thời gian** (đồng hồ không nhảy, độ trễ có giới hạn) — không an toàn trong mô hình bất đồng bộ.
3. Kết luận: nếu lock chỉ để **hiệu quả** (tránh làm trùng việc, thỉnh thoảng trùng cũng không sao) → một Redis instance là đủ, Redlock thừa; nếu lock để **đúng đắn** → Redlock không đủ, cần **fencing token** + hệ thống đồng thuận (ZooKeeper, etcd).

**Fencing token**: mỗi lần cấp lock kèm một số **tăng đơn điệu**; tài nguyên đích (DB, storage) ghi nhớ token lớn nhất đã thấy và **từ chối** request có token nhỏ hơn:

```sql
UPDATE inventory SET qty = :qty, fence = :token
WHERE sku = :sku AND fence < :token;      -- client "zombie" với token cũ bị từ chối
```

antirez phản hồi ("Is Redlock safe?") rằng có thể dùng giá trị random làm điều kiện CAS và Redlock có kiểm tra thời gian sau khi lấy lock; tranh luận vẫn chưa ngã ngũ — điều quan trọng trong phỏng vấn là **trình bày được cả hai phía** và đưa ra lựa chọn theo yêu cầu.

> 💡 **Góc nhìn Senior — chọn công cụ:**
> - Chống chạy trùng cron job, chống double-submit: Redis lock đơn giản (hoặc ShedLock với Redis/JDBC).
> - Bảo vệ tính đúng đắn dữ liệu (trừ tiền, tồn kho): **đừng dựa vào distributed lock** — dùng ràng buộc DB (unique constraint, atomic update có điều kiện, optimistic version) làm hàng phòng thủ cuối. Lock chỉ giảm contention.
> - Cần lock "chuẩn": etcd/ZooKeeper (có session/lease gắn với kết nối, revision tăng đơn điệu dùng làm fencing token), hoặc PostgreSQL advisory lock.

> ⚠️ **Lỗi thường gặp:** đặt `unlock()` ngoài `finally`; lock với `leaseTime` cố định ngắn hơn thời gian xử lý thực tế lúc tải cao; dùng lock trong `@Transactional` và unlock **trước** khi transaction commit → client khác lấy lock và đọc dữ liệu chưa commit (phải unlock sau commit).

### 🛠 Bài tập phần 12

**Bài 12.1 — Lock tự viết (Cơ bản)**
- Đề bài: hiện thực `RedisLock` (tryLock với timeout chờ, unlock bằng Lua). 20 thread cùng tăng một counter trong file/biến dùng chung có lock bảo vệ.
- Tiêu chí: kết quả đúng; test chứng minh unlock bằng token sai không xóa lock của người khác.

**Bài 12.2 — Lock hết hạn giữa chừng (Trung bình)**
- Đề bài: tái hiện: client A lấy lock TTL 2s, xử lý 5s (giả lập GC pause bằng sleep); client B lấy được lock ở giây thứ 2. Sửa bằng (a) Redisson watchdog, (b) fencing token kiểm tra ở DB.
- Tiêu chí: chứng minh (a) giải quyết được trường hợp xử lý lâu nhưng **không** giải quyết được trường hợp process bị treo hoàn toàn (watchdog cũng bị treo — giải thích), (b) giải quyết cả hai.

**Bài 12.3 — Bài luận Redlock (Nâng cao)**
- Đề bài: đọc bài của Kleppmann và antirez; viết 1–2 trang tóm tắt lập luận mỗi bên, và đề xuất cho 3 tình huống: chạy job đối soát cuối ngày duy nhất; giới hạn mỗi user chỉ có một phiên thanh toán; ghi file vào object storage chung.
- Tiêu chí: lập luận có dẫn chứng, chọn công cụ cụ thể (Redis/Redlock/etcd/DB constraint) cho từng tình huống.

<details>
<summary>Gợi ý lời giải</summary>

**12.1**
```java
public boolean tryLock(String key, String token, Duration ttl, Duration wait) throws InterruptedException {
  long deadline = System.nanoTime() + wait.toNanos();
  do {
    if (Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key, token, ttl))) return true;
    Thread.sleep(20 + ThreadLocalRandom.current().nextInt(30));   // backoff có jitter
  } while (System.nanoTime() < deadline);
  return false;
}
```
**12.2** Watchdog chạy trên thread của Redisson trong cùng JVM: GC stop-the-world dừng **mọi** thread kể cả watchdog → nếu pause > 30s lock vẫn hết hạn. Fencing token: `INCR fence:order:1001` lúc lấy lock, gửi kèm vào câu UPDATE có điều kiện `fence < :token`.

**12.3** Job đối soát: Redis lock + job idempotent (bảng `job_runs` unique theo ngày) là đủ. Phiên thanh toán: unique constraint DB (`user_id` trong bảng `active_checkout`). Ghi object storage: dùng conditional write (ETag/`If-Match`) hoặc fencing token do etcd cấp.
</details>

---

<a id="phan-13"></a>
## 13. Rate limiting & các use case kinh điển

### 13.1 Rate limiting

**Fixed window** — đơn giản nhất:
```bash
# key theo user + phút hiện tại
INCR rl:api:user:7:202406011030
EXPIRE rl:api:user:7:202406011030 60 NX     # 7.0+; hoặc gói INCR+EXPIRE vào Lua
```
Nhược: **burst ở ranh giới cửa sổ** — 100 request ở giây 59 + 100 request ở giây 61 = 200 request trong 2 giây dù giới hạn 100/phút.

**Sliding window log (ZSET)** — chính xác:
```lua
-- KEYS[1]=rl:{user} ARGV[1]=nowMs ARGV[2]=windowMs ARGV[3]=limit ARGV[4]=requestId
redis.call('ZREMRANGEBYSCORE', KEYS[1], 0, ARGV[1] - ARGV[2])
local count = redis.call('ZCARD', KEYS[1])
if count < tonumber(ARGV[3]) then
  redis.call('ZADD', KEYS[1], ARGV[1], ARGV[4])
  redis.call('PEXPIRE', KEYS[1], ARGV[2])
  return 1
end
return 0
```
Nhược: bộ nhớ O(limit) mỗi key — không hợp với limit lớn (10.000 req/phút). Biến thể **sliding window counter**: hai counter cửa sổ cố định, ước lượng `prev × (1 − elapsed/window) + current` — O(1) bộ nhớ, sai số nhỏ (cách Cloudflare mô tả).

**Token bucket** — cho phép burst có kiểm soát, tốc độ trung bình ổn định:
```lua
-- KEYS[1]=tb:{user}  ARGV[1]=capacity ARGV[2]=refillPerSec ARGV[3]=nowMs ARGV[4]=requested
local capacity = tonumber(ARGV[1])
local rate     = tonumber(ARGV[2])
local now      = tonumber(ARGV[3])
local req      = tonumber(ARGV[4])
local b = redis.call('HMGET', KEYS[1], 'tokens', 'ts')
local tokens = tonumber(b[1]) or capacity
local ts     = tonumber(b[2]) or now
tokens = math.min(capacity, tokens + (math.max(0, now - ts) / 1000.0) * rate)
local allowed = 0
if tokens >= req then tokens = tokens - req; allowed = 1 end
redis.call('HSET', KEYS[1], 'tokens', tokens, 'ts', now)
redis.call('PEXPIRE', KEYS[1], math.ceil(capacity / rate * 1000) * 2)
return allowed
```

> Lưu ý thời gian: truyền `now` từ client thì các instance phải đồng bộ đồng hồ; có thể dùng `redis.call('TIME')` trong script (Redis 5+ mặc định replicate theo hiệu ứng — *effects replication* — nên gọi lệnh không deterministic trong script là hợp lệ).

| Thuật toán | Chính xác | Bộ nhớ | Burst | Ghi chú |
|---|---|---|---|---|
| Fixed window | Thấp (biên) | O(1) | Gấp đôi ở biên | Dễ nhất |
| Sliding log (ZSET) | Cao | O(limit) | Không | Limit nhỏ, quan trọng (đăng nhập, OTP) |
| Sliding counter | Khá | O(1) | Ít | Quy mô lớn |
| Token bucket | Cao | O(1) | Có kiểm soát (capacity) | API public; Bucket4j (có backend Redis), Spring Cloud Gateway `RedisRateLimiter` |
| Leaky bucket | Cao | O(1) | Không — làm mượt đầu ra | Hàng đợi |

### 13.2 Leaderboard
```bash
ZINCRBY lb:game:2024-06 50 "user:7"
ZREVRANGE lb:game:2024-06 0 9 WITHSCORES        # top 10
ZREVRANK lb:game:2024-06 "user:7"               # hạng của tôi
ZREVRANGE lb:game:2024-06 <rank-5> <rank+5> WITHSCORES   # xung quanh tôi
```
Đồng điểm → xếp theo thời gian đạt điểm: mã hóa score = `points × 10^k + (MAX_TS − ts)` (chú ý giới hạn chính xác 2^53 của double). Leaderboard hàng trăm triệu user: chia theo bucket/khu vực, hoặc tính hạng xấp xỉ cho người ngoài top.

### 13.3 Session store
Spring Session Data Redis (`@EnableRedisHttpSession` hoặc chỉ cần dependency + `spring.session.store-type`/auto-config) lưu HTTP session vào Redis dạng Hash → ứng dụng stateless, scale ngang không cần sticky session. Lưu ý: session phải serializable (khuyến nghị JSON serializer), session lớn làm mỗi request chậm, đặt `maxInactiveInterval`. Với JWT, Redis dùng cho **danh sách thu hồi** (denylist `jti` có TTL = thời gian còn lại của token) và refresh token.

### 13.4 Idempotency key
Client gửi `Idempotency-Key` (UUID) cho request có side-effect (thanh toán). Server:
```bash
SET idem:payment:<key> '{"status":"PROCESSING"}' NX EX 86400
# OK  → request đầu tiên: xử lý, rồi SET idem:payment:<key> '{"status":"DONE","response":...}' XX KEEPTTL
# nil → đã có: nếu DONE trả lại response đã lưu; nếu PROCESSING trả 409/202 "đang xử lý"
```
Lưu thêm hash của request body để phát hiện cùng key nhưng nội dung khác (trả 422). Với giao dịch tiền, nên lưu idempotency **trong DB cùng transaction** với nghiệp vụ (unique constraint) — Redis làm lớp chặn nhanh phía trước.

### 13.5 Pub/Sub vs Streams

| | Pub/Sub | Streams |
|---|---|---|
| Lưu trữ | Không — fire-and-forget | Có — log append-only, giới hạn bằng `MAXLEN`/`MINID` |
| Subscriber offline | **Mất message** | Đọc lại từ ID đã xử lý |
| Consumer group, ACK, retry | ❌ | ✅ (`XREADGROUP`, `XACK`, PEL, `XAUTOCLAIM`) |
| Fan-out | Mọi subscriber nhận | Mỗi group nhận đủ; trong group chia tải |
| Use case | Invalidate L1 cache, thông báo realtime không quan trọng, chat presence | Queue công việc, event nhẹ khi chưa cần Kafka |
| Cluster | Thông điệp broadcast toàn cluster (7.0+ có **sharded pub/sub** `SPUBLISH`) | Một stream nằm trên một slot |

> 💡 **Góc nhìn Senior:** Streams tốt cho queue quy mô vừa, nhưng không thay Kafka khi cần retention dài (ngày/tuần trên đĩa rẻ), replay lớn, throughput rất cao, hệ sinh thái connector. Và nhớ rằng dữ liệu Stream vẫn nằm trong RAM và phụ thuộc persistence/replication async của Redis.

### 🛠 Bài tập phần 13

**Bài 13.1 — Rate limit đăng nhập (Cơ bản)**
- Đề bài: giới hạn 5 lần đăng nhập sai / 15 phút / username bằng sliding window ZSET (Lua). Spring `HandlerInterceptor` trả `429` kèm header `Retry-After`.
- Tiêu chí: test lần thứ 6 bị chặn; sau 15 phút được thử lại; đăng nhập đúng thì xóa key.

**Bài 13.2 — Token bucket cho API Gateway (Trung bình)**
- Đề bài: hiện thực token bucket (Lua trên) cho 3 gói: FREE 10 req/s burst 20, PRO 100 req/s burst 200, ENTERPRISE không giới hạn. Chạy trên 3 instance Spring Boot sau load balancer.
- Tiêu chí: test tải (k6) cho thấy giới hạn đúng **toàn cục** (không phải theo instance); trả header `X-RateLimit-Remaining`; xử lý khi Redis lỗi (fail-open hay fail-closed? lập luận).

**Bài 13.3 — Idempotent payment API (Nâng cao)**
- Đề bài: `POST /payments` với `Idempotency-Key`. Kết hợp Redis (chặn nhanh, trạng thái PROCESSING/DONE) và DB (unique constraint trên key trong cùng transaction tạo payment).
- Tiêu chí: 100 request đồng thời cùng key → đúng 1 payment, 99 request còn lại nhận cùng response (hoặc 409 khi đang xử lý); request cùng key khác body → 422; instance chết khi đang PROCESSING → sau timeout client retry được (giải thích cơ chế).

<details>
<summary>Gợi ý lời giải</summary>

**13.1** `requestId` cho ZADD phải duy nhất (UUID) — nếu dùng timestamp, hai request cùng ms ghi đè nhau. Đăng nhập đúng: `DEL rl:login:{username}`.

**13.2** Fail-open (cho qua khi Redis lỗi) giữ dịch vụ sống nhưng mất bảo vệ; fail-closed an toàn cho endpoint đắt/nhạy cảm. Thường: fail-open + circuit breaker + giới hạn local dự phòng (Bucket4j in-memory với ngưỡng / số instance).

**13.3** PROCESSING đặt TTL ngắn (ví dụ 60s) riêng; chuyển sang DONE với TTL 24h. Nếu instance chết, key PROCESSING hết hạn → retry được; DB unique constraint chặn tạo payment trùng nếu thực ra đã commit → khi đó đọc payment đã có và trả lại.
</details>

---

<a id="phan-14"></a>
## 14. Tích hợp Spring

### 14.1 Lettuce vs Jedis

| | **Lettuce** (mặc định trong Spring Boot) | **Jedis** |
|---|---|---|
| I/O | Non-blocking, dựa trên Netty | Blocking socket |
| Thread-safety | Một connection **dùng chung** an toàn cho nhiều thread (trừ lệnh blocking & transaction) | Connection **không** thread-safe → cần `JedisPool` |
| API | Sync, async (`RedisFuture`), reactive (Project Reactor) | Sync (đơn giản, dễ debug) |
| Cluster/Sentinel | Hỗ trợ đầy đủ, topology refresh | Hỗ trợ |
| Lưu ý | Lệnh blocking (`BLPOP`) hoặc `MULTI` cần connection riêng → Spring dùng pool riêng cho các trường hợp này nếu cấu hình | Kích thước pool phải tính như DB pool |

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 500ms            # command timeout — luôn đặt! mặc định 60s quá dài cho cache
      connect-timeout: 1s
      lettuce:
        pool:                   # cần commons-pool2; chỉ thực sự cần cho lệnh blocking/transaction
          enabled: true
          max-active: 16
```

### 14.2 RedisTemplate & serializer
- `RedisTemplate<Object, Object>` mặc định dùng **`JdkSerializationRedisSerializer`** cho cả key và value → key trong Redis trông như `\xac\xed\x00\x05t\x00\x0bproduct:42` — khó đọc, không dùng được từ ngôn ngữ khác, đổi class là vỡ (`serialVersionUID`), và **Java deserialization là lỗ hổng bảo mật** nếu dữ liệu không tin cậy.
- `StringRedisTemplate`: key/value là String — đơn giản, tự serialize JSON bằng Jackson.
- Cấu hình template với JSON:

```java
@Bean
RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory cf, ObjectMapper om) {
  RedisTemplate<String, Object> t = new RedisTemplate<>();
  t.setConnectionFactory(cf);
  t.setKeySerializer(RedisSerializer.string());
  t.setHashKeySerializer(RedisSerializer.string());
  var json = new GenericJackson2JsonRedisSerializer(om.copy());   // lưu kèm @class để deserialize đa hình
  t.setValueSerializer(json);
  t.setHashValueSerializer(json);
  return t;
}
```

| Serializer | Ưu | Nhược |
|---|---|---|
| `JdkSerializationRedisSerializer` | Không cần cấu hình | Khó đọc, to, rủi ro bảo mật, gắn chặt class |
| `StringRedisSerializer` | Rõ ràng | Chỉ String |
| `GenericJackson2JsonRedisSerializer` | Đọc được, đa hình (lưu type info) | Lưu tên class trong dữ liệu → đổi package là vỡ; cần cẩn thận với default typing (bảo mật) |
| `Jackson2JsonRedisSerializer<T>` | Gọn, không có type info | Một kiểu cố định |
| Protobuf/Kryo/Smile (tự viết) | Nhỏ, nhanh | Phức tạp hơn, khó debug |

(Spring Data Redis 4.x bổ sung các serializer cho Jackson 3; nguyên tắc lựa chọn không đổi.)

### 14.3 Spring Cache abstraction

```java
@Configuration
@EnableCaching
class CacheConfig {
  @Bean
  RedisCacheManager cacheManager(RedisConnectionFactory cf) {
    var json = RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer());
    var defaults = RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofMinutes(10))
        .disableCachingNullValues()
        .prefixCacheNameWith("shop:")                    // key: shop:products::42
        .serializeValuesWith(json);
    return RedisCacheManager.builder(cf)
        .cacheDefaults(defaults)
        .withCacheConfiguration("rates", defaults.entryTtl(Duration.ofSeconds(30)))
        .transactionAware()                              // put/evict sau khi transaction commit
        .build();
  }
}

@Service
class ProductService {
  @Cacheable(cacheNames = "products", key = "#id", sync = true)   // sync=true không cho phép kèm "unless"
  public Product get(long id) { ... }

  @CachePut(cacheNames = "products", key = "#p.id")      // luôn chạy method, ghi kết quả vào cache
  public Product update(Product p) { ... }

  @CacheEvict(cacheNames = "products", key = "#id")
  public void delete(long id) { ... }

  @CacheEvict(cacheNames = "products", allEntries = true)   // trên Redis: quét & xóa theo pattern — tốn kém!
  public void reloadAll() { ... }
}
```

Chi tiết cần nắm:
- **Key mặc định** (`SimpleKeyGenerator`): không tham số → `SimpleKey.EMPTY`; 1 tham số → chính tham số đó; nhiều tham số → `SimpleKey(params...)`. Hai method khác nhau dùng chung `cacheNames` với cùng tham số → **đụng key**. Nên ghi `key` rõ ràng bằng SpEL hoặc custom `KeyGenerator`.
- `sync = true`: chỉ một thread trong **một JVM** nạp giá trị cho cùng key (chống stampede cục bộ, không phải phân tán). Khi `sync = true` thì không được dùng `unless`, chỉ được khai báo một cache, và không kết hợp với annotation cache khác trên cùng method — Spring ném `IllegalStateException` lúc khởi tạo.
- `condition` (đánh giá **trước** khi gọi) vs `unless` (đánh giá **sau**, có `#result`).
- TTL theo từng cache qua `withCacheConfiguration`; muốn jitter cần custom `RedisCacheWriter`/TTL function (Spring Data Redis 3.2+ có `TtlFunction`).
- `@CachePut` và `@Cacheable` không nên đặt trên cùng method.

**Self-invocation pitfall** — lỗi phỏng vấn kinh điển:

```java
@Service
class ReportService {
  public Report build(long id) {
    return compute(id);            // gọi nội bộ qua "this" → KHÔNG đi qua proxy → @Cacheable bị bỏ qua
  }
  @Cacheable("reports")
  public Report compute(long id) { ... }
}
```
Spring Cache (giống `@Transactional`, `@Async`) hiện thực bằng **proxy AOP**; chỉ lời gọi từ bên ngoài bean mới bị chặn. Cách sửa: tách method sang bean khác (khuyến nghị); inject chính bean qua `@Lazy` self-reference / `ObjectProvider`; hoặc dùng AspectJ weaving (`@EnableCaching(mode = AdviceMode.ASPECTJ)`). Tương tự: method `private`/`final` không được proxy.

> 💡 **Góc nhìn Senior:**
> - Spring Cache tiện cho cache-aside đơn giản, nhưng **giấu** chi tiết quan trọng: không có TTL jitter mặc định, không chống stampede phân tán, `allEntries=true` dùng SCAN/KEYS trên Redis, lỗi Redis mặc định ném exception làm hỏng request → cấu hình `CacheErrorHandler` để **log và bỏ qua** lỗi cache (cache lỗi không nên làm sập nghiệp vụ).
> - Có thể ghép L1 + L2: `CompositeCacheManager` không tự đồng bộ hai tầng; dùng thư viện (ví dụ JetCache, Redisson `RLocalCachedMap`) hoặc tự viết `Cache` hai tầng có Pub/Sub invalidate.
> - Đặt **command timeout** ngắn (100–500ms) cho Redis cache: Redis chậm thì thà miss còn hơn treo thread.

```java
@Bean
CacheErrorHandler cacheErrorHandler() {
  return new SimpleCacheErrorHandler() {
    @Override public void handleCacheGetError(RuntimeException e, Cache cache, Object key) {
      log.warn("Cache get failed {}::{}", cache.getName(), key, e);   // coi như miss
    }
    @Override public void handleCachePutError(RuntimeException e, Cache cache, Object key, Object value) {
      log.warn("Cache put failed {}::{}", cache.getName(), key, e);
    }
  };
}
// Đăng ký qua CachingConfigurer#errorHandler()
```

> ⚠️ **Lỗi thường gặp:** cache entity JPA có lazy collection (serialize kích hoạt lazy loading hoặc `LazyInitializationException`) → cache DTO; cache `Optional`/`Page` của Spring Data (khó serialize JSON); đổi cấu trúc class mà không đổi version prefix → lỗi deserialize hàng loạt sau deploy.

### 🛠 Bài tập phần 14

**Bài 14.1 — Spring Cache với Redis (Cơ bản)**
- Đề bài: cấu hình `RedisCacheManager` với JSON serializer, TTL khác nhau cho 2 cache, prefix tên service. Áp `@Cacheable`/`@CachePut`/`@CacheEvict` cho CRUD sản phẩm.
- Tiêu chí: key trong Redis đọc được (`redis-cli KEYS 'shop:*'` trên môi trường dev), TTL đúng; test Testcontainers kiểm chứng số lần gọi repository.

**Bài 14.2 — Bẫy self-invocation & key collision (Trung bình)**
- Đề bài: viết test tái hiện (a) self-invocation làm cache không hoạt động; (b) hai method `findByCategory(long)` và `findByBrand(long)` cùng `cacheNames="products"` trả dữ liệu lẫn lộn. Sửa cả hai.
- Tiêu chí: test đỏ trước khi sửa, xanh sau khi sửa; giải thích bằng cơ chế proxy và `SimpleKeyGenerator`.

**Bài 14.3 — Cache bền bỉ trước sự cố (Nâng cao)**
- Đề bài: thêm `CacheErrorHandler`, command timeout 200ms, circuit breaker Resilience4j quanh truy cập Redis, L1 Caffeine 5s. Dùng Toxiproxy (Testcontainers) để giả lập Redis chậm 2s và Redis chết.
- Tiêu chí: API vẫn trả kết quả đúng (từ DB/L1) với p99 < 300ms khi Redis chậm; không có exception lộ ra client; metric (Micrometer) cho thấy cache errors và trạng thái circuit breaker.

<details>
<summary>Gợi ý lời giải</summary>

**14.2** (a) `Mockito.verify(repo, times(2))` dù gọi 2 lần cùng id qua `build()`. Sửa: chuyển `compute` sang `ReportCalculator` bean riêng. (b) Cả hai tạo key `products::5` → đụng nhau. Sửa: `key = "'category:' + #id"` / `"'brand:' + #id"` hoặc tách cacheNames.

**14.3** Lettuce: `spring.data.redis.timeout=200ms`. Bọc `RedisCache` bằng decorator gọi qua `CircuitBreaker.decorateSupplier`; khi OPEN thì coi như miss ngay. Toxiproxy: `proxy.toxics().latency("lat", ToxicDirection.DOWNSTREAM, 2000)`.
</details>

---

<a id="phan-15"></a>
## 15. Giám sát & vận hành

### 15.1 Lệnh chẩn đoán

```bash
INFO server | clients | memory | persistence | stats | replication | cpu | commandstats | keyspace
# memory: used_memory, used_memory_rss, mem_fragmentation_ratio (RSS/used; >1.5 phân mảnh, <1 đang swap!),
#         maxmemory, evicted_keys (ở stats)
# stats:  instantaneous_ops_per_sec, keyspace_hits, keyspace_misses, expired_keys, evicted_keys,
#         rejected_connections, latest_fork_usec
# clients: connected_clients, blocked_clients
# replication: role, connected_slaves, master_repl_offset, lag của từng replica

SLOWLOG GET 10            # lệnh chạy lâu hơn slowlog-log-slower-than (mặc định 10000 µs = 10ms)
SLOWLOG LEN ; SLOWLOG RESET
CONFIG SET slowlog-log-slower-than 5000

CONFIG SET latency-monitor-threshold 100      # ms
LATENCY LATEST ; LATENCY HISTORY command ; LATENCY DOCTOR

MEMORY USAGE key [SAMPLES 0] ; MEMORY STATS ; MEMORY DOCTOR
CLIENT LIST               # ai đang kết nối, omem (output buffer), cmd cuối, idle
CLIENT NO-EVICT on        # 7.0+ cho client quản trị
DEBUG OBJECT key          # (cẩn thận, có thể bị vô hiệu hóa trên managed service)
```

```bash
redis-cli --latency -h host           # đo latency liên tục từ client
redis-cli --latency-history -i 5
redis-cli --intrinsic-latency 30      # chạy TRÊN máy Redis: latency nội tại của OS/VM
redis-cli --stat                      # dashboard dòng lệnh
redis-cli --bigkeys ; redis-cli --memkeys ; redis-cli --hotkeys
```

### 15.2 KEYS → SCAN
`KEYS pattern` là O(N) trên **toàn keyspace**, chặn server — với 50 triệu key có thể chặn nhiều giây. Dùng `SCAN` có cursor:

```bash
SCAN 0 MATCH "shop:catalog:*" COUNT 1000 TYPE string
# trả về cursor mới; lặp tới khi cursor = 0. Đặc tính: có thể trả trùng, key thêm/xóa trong lúc quét
# có thể có hoặc không — client phải chịu được. COUNT chỉ là gợi ý lượng công việc mỗi lần.
```

```java
try (Cursor<String> c = redis.scan(ScanOptions.scanOptions().match("shop:catalog:*").count(1000).build())) {
  List<String> batch = new ArrayList<>();
  while (c.hasNext()) {
    batch.add(c.next());
    if (batch.size() == 500) { redis.unlink(batch); batch.clear(); }
  }
  if (!batch.isEmpty()) redis.unlink(batch);
}
// Trên Cluster: phải SCAN trên TỪNG master node.
```

### 15.3 Các chỉ số cần cảnh báo (alert)

| Chỉ số | Ngưỡng gợi ý | Ý nghĩa |
|---|---|---|
| `used_memory / maxmemory` | > 80% | Sắp evict/OOM |
| `evicted_keys` tăng | > 0 với instance không phải cache thuần | Mất dữ liệu |
| Hit ratio | Giảm đột ngột | Đổi key format, flush, avalanche |
| `mem_fragmentation_ratio` | > 1,5 hoặc < 1 | Phân mảnh (bật `activedefrag`) / đang swap |
| `connected_clients` | Gần `maxclients` (mặc định 10000) | Connection leak |
| `blocked_clients` | Tăng bất thường | Lệnh blocking treo |
| Replication lag / `master_link_status:down` | > vài giây | Replica tụt hậu, rủi ro failover mất dữ liệu |
| `latest_fork_usec` | > 500ms | Fork chậm — dataset lớn hoặc THP |
| `rdb_last_bgsave_status`, `aof_last_write_status` | ≠ ok | Persistence lỗi |
| Slowlog | Có lệnh mới > 10ms | Big key, lệnh O(N) |
| Latency p99 phía client | > SLO | Mạng, server, hay client (GC)? |

Công cụ: `redis_exporter` + Prometheus + Grafana; Micrometer metric của Lettuce (`lettuce.command.completion`) phía ứng dụng.

> 💡 **Góc nhìn Senior — checklist bảo mật & cấu hình production:** bật `requirepass`/**ACL** (Redis 6+) với user tối thiểu quyền; **không** expose Redis ra Internet (vô số máy bị chiếm qua Redis mở — ghi SSH key bằng `CONFIG SET dir`); TLS khi đi qua mạng không tin cậy; chặn/đổi tên `FLUSHALL`, `KEYS`, `CONFIG`, `DEBUG` cho user ứng dụng; đặt `maxmemory` + policy; `vm.overcommit_memory=1`; tắt THP; `tcp-keepalive`; giới hạn `client-output-buffer-limit` cho pubsub/replica.

> ⚠️ **Lỗi thường gặp:** chẩn đoán "Redis chậm" mà thực ra là **client chậm** (GC pause ở JVM, pool cạn, chạy lệnh tuần tự không pipeline) hoặc **mạng** — so latency đo ở server (`LATENCY`, slowlog) với latency đo ở client trước khi kết luận; dùng `MONITOR` lâu trên production (giảm throughput mạnh).

### 🛠 Bài tập phần 15

**Bài 15.1 — Đọc INFO (Cơ bản)**
- Đề bài: chạy một workload hỗn hợp, thu thập `INFO` mỗi 10s trong 5 phút; tính hit ratio, ops/s, tỷ lệ phân mảnh, số key evicted/expired.
- Tiêu chí: script (bash/Java) tự tính các chỉ số; giải thích từng chỉ số.

**Bài 15.2 — Thay KEYS bằng SCAN (Trung bình)**
- Đề bài: một đoạn code legacy dùng `redisTemplate.keys("session:*")` để đếm user online và xóa session hết hạn trên keyspace 5 triệu key. Đo ảnh hưởng latency lên client khác. Viết lại bằng SCAN + UNLINK theo lô (và tốt hơn: dùng cấu trúc dữ liệu riêng — ZSET online theo timestamp).
- Tiêu chí: latency max của client khác trước/sau; giải pháp thay thế không cần quét.

**Bài 15.3 — Dashboard & alert (Nâng cao)**
- Đề bài: Docker Compose Redis + `redis_exporter` + Prometheus + Grafana + app Spring Boot có Micrometer. Dựng dashboard (memory, hit ratio, ops/s, latency p99 phía client, evictions, replication lag) và 5 alert rule ở bảng 15.3. Gây sự cố có chủ đích (big key, maxmemory thấp, kill replica) để kích hoạt alert.
- Tiêu chí: ảnh chụp dashboard khi sự cố; mỗi alert có runbook ngắn (nguyên nhân khả dĩ, lệnh chẩn đoán, cách xử lý).

<details>
<summary>Gợi ý lời giải</summary>

**15.1**
```bash
redis-cli INFO stats | awk -F: '/keyspace_hits/{h=$2} /keyspace_misses/{m=$2} END{printf "hit_ratio=%.4f\n", h/(h+m)}'
```
**15.2** Online user: `ZADD online <nowMs> <userId>` mỗi request (hoặc mỗi phút), đếm `ZCOUNT online <now-5m> +inf`, dọn `ZREMRANGEBYSCORE online -inf <now-5m>`. Session hết hạn thì Redis tự xử lý TTL — không cần quét.

**15.3** Ví dụ rule:
```yaml
- alert: RedisMemoryHigh
  expr: redis_memory_used_bytes / redis_memory_max_bytes > 0.8
  for: 5m
- alert: RedisEvictingKeys
  expr: increase(redis_evicted_keys_total[5m]) > 0
```
</details>

---

<a id="du-an-mini"></a>
## Dự án mini — "FlashSale": hệ thống bán hàng chớp nhoáng với Redis

**Bối cảnh:** sàn TMĐT mở flash sale 1.000 sản phẩm giá sốc lúc 12:00, dự kiến 50.000 người dùng đồng thời, 20.000 req/s vào trang chi tiết và 5.000 req/s vào nút "Mua". Stack: Spring Boot 3, PostgreSQL (nguồn sự thật đơn hàng), Redis (Sentinel hoặc Cluster), Caffeine. Thời lượng 2 ngày.

### Yêu cầu chức năng
1. **Trang chi tiết sản phẩm** `GET /flash/{sku}`: cache 2 tầng (Caffeine 2s + Redis), chống penetration (Bloom filter các SKU flash sale), chống breakdown (logical expiration hoặc mutex), TTL có jitter.
2. **Tồn kho trên Redis**: warm-up tồn kho vào Redis trước giờ mở bán; mua hàng trừ kho atomic bằng Lua (kiểm tra còn hàng, mỗi user chỉ mua 1 sản phẩm mỗi SKU — dùng Set `bought:{sku}`), trả kết quả ngay.
3. **Tạo đơn bất đồng bộ**: mua thành công → `XADD` vào Stream `orders:flash`; consumer group ghi đơn vào PostgreSQL idempotent (unique `(user_id, sku)`), `XACK`; message treo được `XAUTOCLAIM`.
4. **Rate limit**: token bucket theo user (5 req/s) và theo IP (50 req/s) cho endpoint mua.
5. **Idempotency**: `POST /flash/{sku}/buy` nhận `Idempotency-Key`.
6. **Leaderboard**: top 10 SKU bán chạy realtime (ZSET), API `GET /flash/leaderboard`.
7. **Đối soát**: job sau sale so sánh tồn kho Redis vs số đơn trong DB, báo lệch.
8. **Đóng sale**: dọn key theo prefix bằng SCAN + UNLINK, không dùng KEYS.

### Yêu cầu phi chức năng
- **Không oversell**: test 10.000 request mua đồng thời trên SKU có 100 sản phẩm → đúng 100 đơn trong DB, tồn kho Redis = 0, không user nào có 2 đơn cùng SKU.
- `GET /flash/{sku}` p99 < 20ms ở 5.000 req/s trên máy dev; `POST .../buy` p99 < 50ms (không tính tạo đơn bất đồng bộ).
- Redis lỗi/chậm (Toxiproxy) → trang chi tiết vẫn phục vụ từ L1/DB với circuit breaker; nút mua trả lỗi rõ ràng (fail-closed) chứ không oversell.
- Redis key có quy ước, version, TTL cho mọi key tạm; serializer JSON; command timeout ≤ 300ms.
- Có dashboard/metric: hit ratio L1/L2, số lần loader được gọi, tỷ lệ bị rate limit, độ dài pending của Stream.
- Test tích hợp với Testcontainers (Redis, PostgreSQL).

### Tiêu chí chấm (100 điểm)
| Hạng mục | Điểm |
|---|---|
| Đúng đắn khi đồng thời: không oversell, giới hạn 1/user, idempotency (có test tải chứng minh) | 25 |
| Cache 2 tầng + chống penetration/breakdown/avalanche | 20 |
| Stream + consumer group: at-least-once, idempotent, xử lý message treo | 15 |
| Rate limiting phân tán đúng thuật toán | 10 |
| Khả năng chịu lỗi Redis (timeout, circuit breaker, fail-open/closed có lập luận) | 10 |
| Quan sát: metric, slowlog sạch, không big key, không KEYS | 10 |
| README: kiến trúc, trade-off nhất quán Redis ↔ DB, kết quả benchmark, đối soát | 10 |

---

<a id="checklist-tu-danh-gia"></a>
## Checklist tự đánh giá

- [ ] Tôi kể được các tầng cache từ browser tới DB, tính được ảnh hưởng của hit ratio lên tải DB, và biết khi nào **không** nên cache.
- [ ] Tôi giải thích được W-TinyLFU, khác biệt `expireAfterWrite` / `expireAfterAccess` / `refreshAfterWrite` của Caffeine.
- [ ] Tôi lập luận được khi nào dùng local cache, distributed cache, hay cả hai tầng và cách invalidate L1.
- [ ] Tôi giải thích được vì sao Redis đơn luồng mà nhanh, I/O threads của Redis 6+ làm gì và không làm gì.
- [ ] Tôi nêu được encoding và độ phức tạp của các lệnh chính trên String, Hash, List, Set, ZSet (skiplist), Bitmap, HyperLogLog, Stream, Geo, và chọn đúng cấu trúc cho use case.
- [ ] Tôi đặt được quy ước key có version, hiểu lazy + active expiration và chọn đúng eviction policy.
- [ ] Tôi so sánh được RDB, AOF (các mức fsync), hybrid và biết rủi ro khi tắt persistence trên master.
- [ ] Tôi giải thích replication (full/partial sync), Sentinel (quorum, failover) và Cluster (16384 slot, hash tag, MOVED/ASK, CROSSSLOT).
- [ ] Tôi phân biệt MULTI/EXEC (không rollback), WATCH (CAS), Lua (atomic có logic), pipeline (giảm RTT, không atomic).
- [ ] Tôi hiện thực được cache-aside đúng (xóa sau commit) và giải thích read-through, write-through, write-behind, refresh-ahead.
- [ ] Tôi phân tích được các race giữa DB và cache, và áp dụng delayed double delete, version check, CDC invalidation.
- [ ] Tôi phân biệt và xử lý được cache penetration (null cache, Bloom filter), breakdown (mutex, logical expiry), avalanche (jitter, HA, multi-level), big key & hot key.
- [ ] Tôi viết được distributed lock an toàn (SET NX PX + Lua release), hiểu watchdog Redisson, tranh luận Redlock và fencing token.
- [ ] Tôi hiện thực được rate limiter fixed window, sliding window (ZSET), token bucket (Lua) và biết trade-off.
- [ ] Tôi thiết kế được leaderboard, session store, idempotency key, và chọn đúng Pub/Sub hay Streams.
- [ ] Tôi cấu hình được Spring Cache với Redis (TTL, serializer JSON, error handler) và giải thích bẫy self-invocation, key collision.
- [ ] Tôi so sánh được Lettuce và Jedis, biết vì sao không dùng JDK serializer mặc định.
- [ ] Tôi dùng được INFO, SLOWLOG, LATENCY, MEMORY USAGE, `--bigkeys`, `--hotkeys`, và luôn dùng SCAN thay KEYS.

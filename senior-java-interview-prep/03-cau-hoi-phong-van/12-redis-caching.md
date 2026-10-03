# Câu hỏi phỏng vấn — Module 12: Redis & Caching

> Giáo trình tương ứng: [Module 12 — Redis & Caching](../01-giao-trinh/12-redis-caching.md)

> **Cách dùng:** đọc câu hỏi, **tự trả lời thành tiếng** (≈30 giây cho phần ngắn, 2–3 phút cho phần chi tiết) **trước khi** mở đáp án. Sau đó so với "Trả lời ngắn", tự hỏi tiếp các "Câu hỏi nối tiếp", và đánh dấu câu nào bạn rơi vào mục "⚠️ Câu trả lời gây điểm trừ" để ôn lại theo link 📖.
>
> **Mức độ:** 🟢 Cơ bản — 🟡 Senior — 🔴 Xoáy sâu. Câu có nhãn **🎯 Tình huống** là câu "bạn sẽ làm gì".

## Mục lục

| Nhóm | Chủ đề | Câu hỏi |
|---|---|---|
| A | [Nền tảng caching & local cache](#nhom-a) | Q1–Q5 |
| B | [Kiến trúc Redis & cấu trúc dữ liệu](#nhom-b) | Q6–Q12 |
| C | [Key, expiration, eviction & persistence](#nhom-c) | Q13–Q19 |
| D | [Replication, Sentinel & Cluster](#nhom-d) | Q20–Q25 |
| E | [Transaction, Lua & pipelining](#nhom-e) | Q26–Q29 |
| F | [Caching pattern & nhất quán DB–cache](#nhom-f) | Q30–Q36 |
| G | [Sự cố cache: penetration, breakdown, avalanche, big/hot key](#nhom-g) | Q37–Q42 |
| H | [Distributed lock](#nhom-h) | Q43–Q47 |
| I | [Rate limiting & use case kinh điển](#nhom-i) | Q48–Q52 |
| J | [Tích hợp Spring](#nhom-j) | Q53–Q57 |
| K | [Giám sát & vận hành](#nhom-k) | Q58–Q60 |

---

<a id="nhom-a"></a>
## A. Nền tảng caching & local cache

### Q1. 🟢 Khi nào nên cache, khi nào không? Hit ratio ảnh hưởng tải DB thế nào?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Cache dữ liệu **đọc nhiều hơn ghi rất nhiều**, tốn kém để tính, **chấp nhận được độ cũ** nhất định và có phân bố truy cập lệch (power law). Không cache dữ liệu cần nhất quán mạnh (số dư lúc trừ tiền), dữ liệu đọc ngẫu nhiên đều (long tail), hoặc khi chưa đo được bottleneck. Hit ratio từ 90% lên 99% giảm tải DB **10 lần** — vài phần trăm hit ratio rất đáng giá.

**Giải thích chi tiết:**
- Công thức: `latency ≈ t_cache + (1 − h) × t_origin`; tải DB = `(1 − h) × RPS`.
- Các tầng cache: browser (`Cache-Control`, `ETag`) → CDN → reverse proxy → **local in-process** (Caffeine, ns–µs) → **distributed** (Redis, ~0.2–1ms) → DB buffer pool → disk.
- **Working set**: cache chỉ cần chứa tập dữ liệu đang nóng, không cần toàn bộ dữ liệu.
- TTL là **giới hạn trên của độ cũ** và là lưới an toàn khi invalidation thất bại.
- Cache **che giấu** vấn đề hiệu năng. Nếu DB chỉ sống được nhờ hit ratio 99% thì cache đã thành thành phần **critical**: phải thiết kế HA, warm-up, và bảo vệ DB khi cache trống.

**Câu hỏi nối tiếp:**
- *Cache dữ liệu theo user thì cần lưu ý gì?* — Key phải chứa userId/tenant, nếu không sẽ lộ dữ liệu người khác; cân nhắc `Cache-Control: private` ở tầng HTTP.
- *Có nên cache response lỗi/rỗng?* — Rỗng thì có (null caching chống penetration) nhưng TTL ngắn; lỗi 5xx thì không.

**⚠️ Câu trả lời gây điểm trừ:**
- "Cứ chậm là thêm Redis" — không nói đến đo đạc, nhất quán, cold start.
- Không nhắc TTL hoặc nói "cache vĩnh viễn rồi invalidate khi ghi" mà không có lưới an toàn.

**📖 Ôn lại:** [Phần 1 — Nền tảng caching](../01-giao-trinh/12-redis-caching.md#phan-1)

</details>

### Q2. 🟡 🎯 Tình huống: API sản phẩm 5.000 req/s, DB query 20ms, Redis 0.5ms, DB chỉ chịu 800 query/s. Hit ratio tối thiểu là bao nhiêu và rủi ro lớn nhất là gì?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Cần `(1 − h) × 5000 ≤ 800` → **h ≥ 84%**. Rủi ro lớn nhất: cache bị flush/restart → 5.000 q/s ập vào DB → DB sập → cache không bao giờ được nạp lại (vòng xoáy chết). Cần warm-up, request coalescing, rate limit/circuit breaker bảo vệ DB.

**Giải thích chi tiết:**

| Hit ratio | Latency TB ≈ `0.5 + (1−h)×20` | Query DB/s |
|---|---|---|
| 0% | 20.5ms | 5.000 |
| 80% | 4.5ms | 1.000 (quá tải) |
| 95% | 1.5ms | 250 |
| 99% | 0.7ms | 50 |

- Khi hệ thống "phụ thuộc" cache: Redis phải HA (Sentinel/Cluster), có persistence để restart không trống hoàn toàn, có L1 Caffeine đỡ.
- Bảo vệ DB: bulkhead/semaphore giới hạn số query đồng thời từ đường cache-miss, trả dữ liệu degraded khi vượt ngưỡng.
- Warm-up có kiểm soát trước khi mở traffic (pipeline nạp các key nóng nhất, TTL có jitter).

**Câu hỏi nối tiếp:**
- *Làm sao biết key nào là nóng nhất để warm-up?* — Thống kê truy cập (log, metric), hoặc `OBJECT FREQ` với policy LFU, hoặc dump từ instance cũ.
- *Nếu không thể warm-up?* — Mở traffic dần (canary/ramp-up), coalescing request cùng key, giới hạn concurrency xuống DB.

**⚠️ Câu trả lời gây điểm trừ:**
- Chỉ tính ra con số 84% mà không bàn về cold start/flush.
- "Scale DB lên là xong" mà không định lượng.

**📖 Ôn lại:** [Phần 1 — Nền tảng caching](../01-giao-trinh/12-redis-caching.md#phan-1) · [Phần 11 — Sự cố cache](../01-giao-trinh/12-redis-caching.md#phan-11)

</details>

### Q3. 🟢 Phân biệt `expireAfterWrite`, `expireAfterAccess`, `refreshAfterWrite` trong Caffeine.

<details><summary>Đáp án</summary>

**Trả lời ngắn:** `expireAfterWrite` loại entry sau `d` kể từ lúc ghi → lần đọc sau là **miss đồng bộ**. `expireAfterAccess` loại nếu không ai đọc trong `d`. `refreshAfterWrite` **không loại**: sau `d`, lần đọc đầu tiên vẫn trả **giá trị cũ ngay** và kích hoạt reload **bất đồng bộ**. Kết hợp `refreshAfterWrite(1m) + expireAfterWrite(10m)`: key nóng luôn tươi mà user không phải chờ, key nguội tự rơi ra.

**Giải thích chi tiết:**
```java
LoadingCache<Long, Product> cache = Caffeine.newBuilder()
    .maximumSize(10_000)
    .expireAfterWrite(Duration.ofMinutes(10))
    .refreshAfterWrite(Duration.ofMinutes(1))   // cần LoadingCache/AsyncLoadingCache
    .recordStats()
    .build(id -> repo.findById(id).orElse(null));
```
- `cache.get(key)` khi miss: chỉ **một** thread gọi loader, các thread khác chờ chung kết quả → chống stampede **trong một JVM**.
- Refresh mà loader ném exception → Caffeine giữ giá trị cũ, log lỗi, lần đọc sau thử lại; entry vẫn hết hạn ở mốc `expireAfterWrite`.
- `expireAfter(Expiry)` cho TTL riêng từng entry.

**Câu hỏi nối tiếp:**
- *Vì sao refresh làm giảm p99?* — `expireAfterWrite` tạo "spike" định kỳ khi key nóng hết hạn (user chờ loader); refresh chuyển việc nạp sang nền.
- *Refresh có làm dữ liệu cũ hơn không?* — Có thể cũ thêm tối đa thời gian một lần load; đổi lấy latency ổn định.

**⚠️ Câu trả lời gây điểm trừ:**
- Nói `refreshAfterWrite` "xóa entry rồi nạp lại" (sai — không xóa).
- Dùng `refreshAfterWrite` với `Cache` thường (không có loader).

**📖 Ôn lại:** [Phần 2 — Local cache với Caffeine](../01-giao-trinh/12-redis-caching.md#phan-2)

</details>

### Q4. 🔴 Vì sao Caffeine có hit ratio cao hơn LRU thuần? Giải thích W-TinyLFU.

<details><summary>Đáp án</summary>

**Trả lời ngắn:** LRU thuần bị **cache pollution** — một đợt scan lớn đẩy hết dữ liệu nóng ra. Caffeine dùng **Window TinyLFU**: một window LRU nhỏ (~1%) đón entry mới, vùng chính là Segmented LRU (probation + protected ~80%); entry rời window chỉ được **admit** vào vùng chính nếu **tần suất ước lượng** của nó cao hơn "nạn nhân" sắp bị loại. Tần suất đo bằng **Count-Min Sketch** 4-bit, định kỳ chia đôi (aging).

**Giải thích chi tiết:**
```
mới ─► [Window LRU ~1%] ─► admission filter (freq(candidate) > freq(victim)?) ─► [Probation] ⇄ [Protected ~80%]
                                   │ không
                                   ▼
                                 loại bỏ
```
- **Count-Min Sketch**: nhiều hàng counter, mỗi key băm vào một ô mỗi hàng, lấy min → ước lượng tần suất với bộ nhớ rất nhỏ; có thể đếm dư, không đếm thiếu.
- **Aging**: khi tổng số lần tăng đạt ngưỡng, mọi counter chia đôi → quên lịch sử cũ, thích nghi khi xu hướng đổi (khắc phục điểm yếu của LFU thuần).
- **Hill climbing**: tự điều chỉnh kích thước window theo workload (thiên recency hay frequency).
- Kết quả: trong đợt scan, key scan có tần suất 1 bị từ chối → hit ratio gần như không đổi; LRU rơi về ~0.

**Câu hỏi nối tiếp:**
- *Redis dùng LRU/LFU thế nào?* — Xấp xỉ: lấy mẫu `maxmemory-samples` key và loại key tệ nhất; LFU dùng counter logarit 8-bit + decay (`lfu-log-factor`, `lfu-decay-time`).
- *Khi nào LRU vẫn ổn?* — Workload thiên recency mạnh, không có scan.

**⚠️ Câu trả lời gây điểm trừ:**
- "Caffeine nhanh vì dùng ConcurrentHashMap" — nhầm giữa tốc độ truy cập và hit ratio.
- Nói Caffeine là LFU thuần.

**📖 Ôn lại:** [Phần 2 — Local cache với Caffeine](../01-giao-trinh/12-redis-caching.md#phan-2)

</details>

### Q5. 🟡 Local cache vs distributed cache — khi nào dùng cái nào, khi nào dùng cả hai? Invalidate L1 giữa các pod ra sao?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Local (Caffeine) nhanh cỡ ns, không serialize, nhưng mỗi pod một bản lệch nhau, mất khi restart, tốn heap. Redis dùng chung, sống qua deploy, chia sẻ được session/counter/lock nhưng tốn ~0.5ms + serialize và phụ thuộc mạng. Dùng **2 tầng** (L1 TTL vài giây + L2 Redis) cho dữ liệu đọc cực nóng, ít đổi, chấp nhận lệch vài giây. Invalidate L1 bằng Redis Pub/Sub hoặc **client-side caching** (`CLIENT TRACKING`, Redis 6+).

**Giải thích chi tiết:**

| | Local (Caffeine) | Distributed (Redis) |
|---|---|---|
| Latency | ns, không serialize | ~0.2–1ms + serialize |
| Dung lượng | Giới hạn bởi heap (ảnh hưởng GC) | Chục–trăm GB, scale bằng cluster |
| Nhất quán giữa pod | ❌ | ✅ |
| Sống qua restart | ❌ | ✅ (tùy persistence) |
| Shared state (lock, counter) | ❌ | ✅ |

- L1 là cách chuẩn để giải **hot key** trên Redis.
- `CLIENT TRACKING`: server ghi nhớ client đã đọc key nào, gửi invalidation khi key đổi; Lettuce có `ClientSideCaching`, Redisson có `RLocalCachedMap`.
- Pub/Sub là fire-and-forget: pod mất kết nối lúc đó sẽ không nhận → **TTL ngắn ở L1 là bắt buộc** làm lưới an toàn.

**Câu hỏi nối tiếp:**
- *Rủi ro khi trả object từ local cache?* — Object mutable bị code gọi sửa → hỏng dữ liệu cho mọi request khác. Dùng `record`/immutable hoặc copy.
- *Tự viết cache bằng `ConcurrentHashMap` được không?* — Không giới hạn kích thước = memory leak có hẹn; không có eviction/TTL/stats.

**⚠️ Câu trả lời gây điểm trừ:**
- "Local cache luôn tốt hơn vì nhanh hơn" — bỏ qua nhất quán giữa pod và GC.
- Không giới hạn `maximumSize`/`maximumWeight`.

**📖 Ôn lại:** [Phần 2 — Local vs distributed](../01-giao-trinh/12-redis-caching.md#phan-2)

</details>

---

<a id="nhom-b"></a>
## B. Kiến trúc Redis & cấu trúc dữ liệu

### Q6. 🟢 Redis đơn luồng, vậy tại sao nhanh?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** (1) dữ liệu trong **RAM** với cấu trúc tối ưu (encoding gọn cho tập nhỏ); (2) **một thread thực thi lệnh** nên không tốn lock/context switch, mỗi lệnh atomic tự nhiên; (3) **I/O multiplexing** (`epoll`/`kqueue`) phục vụ hàng chục nghìn kết nối; (4) giao thức RESP parse nhanh. Bottleneck thường là **mạng và bộ nhớ**, không phải CPU — một instance đạt ~100k ops/s trở lên với lệnh đơn giản, hơn nữa khi pipeline.

**Giải thích chi tiết:**
- Event loop: chọn socket sẵn sàng → đọc request → thực thi → ghi response.
- Không hoàn toàn "một thread": có background thread cho đóng file, fsync AOF, **lazy free** (`UNLINK`, `FLUSHALL ASYNC`); process con `fork()` cho BGSAVE/BGREWRITEAOF (copy-on-write).
- Hệ quả: **một lệnh chậm chặn tất cả client**.
- Muốn dùng nhiều core → chạy **nhiều shard** (Cluster), không phải "nâng CPU".

**Câu hỏi nối tiếp:**
- *Atomic tự nhiên nghĩa là gì?* — `INCR` từ 100 client đồng thời không mất cập nhật; nhưng chuỗi nhiều lệnh thì không atomic (cần Lua/MULTI).
- *Fork trên dataset 30GB ảnh hưởng gì?* — Copy page table mất hàng trăm ms → spike latency; THP làm COW tốn hơn → tắt THP.

**⚠️ Câu trả lời gây điểm trừ:**
- "Vì Redis viết bằng C" hoặc "vì Redis đa luồng".
- Không nêu được hệ quả "lệnh O(N) chặn mọi người".

**📖 Ôn lại:** [Phần 3 — Kiến trúc Redis](../01-giao-trinh/12-redis-caching.md#phan-3)

</details>

### Q7. 🟡 Redis 6 có "multi-threading" — vậy còn đơn luồng không? I/O threads giúp được workload nào?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** I/O threads (`io-threads 4`, `io-threads-do-reads yes`) chỉ song song hóa **đọc/parse socket và ghi response**; **thực thi lệnh vẫn đơn luồng**. Giúp khi CPU bị chiếm bởi xử lý mạng (nhiều client, payload lớn — ví dụ GET 16KB với 500 kết nối); **không** giúp lệnh nặng CPU như `ZUNIONSTORE`, `SORT`, Lua lâu.

**Giải thích chi tiết:**
- Tính atomic của lệnh vẫn được giữ vì execution vẫn tuần tự trên main thread.
- Bật I/O threads trên máy ít core hoặc workload nhẹ có thể không lợi, thậm chí tệ hơn do overhead đồng bộ.
- Với managed service (ElastiCache…), nhà cung cấp có tối ưu riêng (enhanced I/O) — vẫn cùng nguyên tắc.

**Câu hỏi nối tiếp:**
- *Vậy muốn scale CPU cho lệnh thì sao?* — Shard dữ liệu (Cluster), giảm độ phức tạp lệnh, chuyển tính toán nặng ra ngoài Redis.
- *Background thread làm gì?* — fsync AOF, close file, lazy free.

**⚠️ Câu trả lời gây điểm trừ:**
- "Redis 6 đa luồng nên không cần lo lệnh chậm nữa".

**📖 Ôn lại:** [Phần 3 — Mô hình thực thi](../01-giao-trinh/12-redis-caching.md#phan-3)

</details>

### Q8. 🟡 🎯 Tình huống: latency Redis production thỉnh thoảng nhảy lên vài trăm ms. Bạn nghi ngờ những lệnh nào và thay thế ra sao?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Nghi lệnh **O(N) trên tập lớn** chặn main thread: `KEYS *`, `FLUSHALL` đồng bộ, `HGETALL`/`SMEMBERS`/`LRANGE 0 -1` trên key triệu phần tử, `DEL` big key, `SORT`, `SUNION`/`ZUNIONSTORE` lớn, Lua chạy lâu. Kiểm tra bằng `SLOWLOG GET`, `LATENCY DOCTOR`, `--bigkeys`. Thay bằng `SCAN`/`HSCAN`/`SSCAN`/`ZSCAN`, `UNLINK` thay `DEL`, phân trang `ZRANGE`/`LRANGE`, và chặn lệnh nguy hiểm bằng ACL/`rename-command`.

**Giải thích chi tiết:**
- Ngoài lệnh chậm, spike định kỳ còn do **fork** (BGSAVE/AOF rewrite — xem `latest_fork_usec`), **active expiry** dọn hàng loạt key cùng hết hạn, swap (`mem_fragmentation_ratio < 1`).
- Phải so latency phía server (slowlog, `LATENCY`) với phía client: nếu server sạch thì nghi **client** (GC pause, pool cạn, gọi tuần tự không pipeline) hoặc **mạng**.
- `redis-cli --intrinsic-latency` chạy trên máy Redis để đo latency nội tại của VM.

**Câu hỏi nối tiếp:**
- *SCAN có đảm bảo gì?* — Có thể trả trùng; key thêm/xóa trong lúc quét có thể có hoặc không; `COUNT` chỉ là gợi ý. Trên Cluster phải SCAN từng master.
- *Xóa Hash 2 triệu field mà không chặn?* — `UNLINK` (giải phóng ở background) hoặc `HSCAN` + `HDEL` theo lô.

**⚠️ Câu trả lời gây điểm trừ:**
- Đề xuất ngay "tăng RAM/CPU" mà không chẩn đoán.
- Dùng `MONITOR` lâu trên production để debug (làm giảm throughput mạnh).

**📖 Ôn lại:** [Phần 3 — Hệ quả của đơn luồng](../01-giao-trinh/12-redis-caching.md#phan-3) · [Phần 15 — Giám sát](../01-giao-trinh/12-redis-caching.md#phan-15)

</details>

### Q9. 🟢 Chọn cấu trúc dữ liệu Redis cho: đếm lượt xem, 50 bài đọc gần đây không trùng, số user online phân biệt trong ngày, điểm danh, top 100 sản phẩm bán chạy.

<details><summary>Đáp án</summary>

**Trả lời ngắn:**
- Lượt xem: **String** `INCR post:{id}:views` — O(1).
- Bài đọc gần đây không trùng: **Sorted Set** score = timestamp, `ZADD` + `ZREMRANGEBYRANK key 0 -51`.
- UV trong ngày: **HyperLogLog** (`PFADD`, ≤12KB, sai số 0,81%) hoặc **Bitmap** (10 triệu user ≈ 1,2MB, chính xác) nếu userId là số liên tục.
- Điểm danh: **Bitmap** theo user/tháng, `SETBIT` + `BITCOUNT`.
- Top bán chạy: **Sorted Set** `ZINCRBY sales:2024-w23 qty sku`, `ZRANGE ... REV 0 99`.

**Giải thích chi tiết:**

| Đếm phân biệt | Bộ nhớ | Chính xác | Lấy danh sách |
|---|---|---|---|
| Set | O(N) — lớn | ✅ | ✅ |
| Bitmap | N bit theo **giá trị id lớn nhất** | ✅ | Có (quét bit) |
| HyperLogLog | ≤12KB cố định | ~0,81% | ❌ |

- Chọn cấu trúc theo **mẫu truy cập**, không theo hình dạng dữ liệu: cần đọc từng field → Hash; luôn đọc/ghi cả object → String JSON đơn giản hơn.
- Bitmap với id thưa (UUID, id rất lớn) thì lãng phí — `SETBIT key 4000000000 1` cấp phát ~500MB.

**Câu hỏi nối tiếp:**
- *User active cả 7 ngày?* — `BITOP AND` 7 bitmap DAU rồi `BITCOUNT`.
- *Gộp UV theo tuần?* — `PFMERGE` các HLL ngày.

**⚠️ Câu trả lời gây điểm trừ:**
- Dùng Set lưu 10 triệu userId chỉ để đếm.
- Dùng List + kiểm tra trùng bằng `LRANGE` toàn bộ.

**📖 Ôn lại:** [Phần 4 — Cấu trúc dữ liệu & internals](../01-giao-trinh/12-redis-caching.md#phan-4)

</details>

### Q10. 🔴 Sorted Set được hiện thực thế nào? Vì sao Redis chọn skiplist thay vì cây cân bằng?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** ZSet nhỏ (≤128 phần tử, giá trị ≤64 byte) dùng **listpack**; lớn hơn dùng **hai cấu trúc song song**: **hashtable** `member → score` (`ZSCORE` O(1)) và **skiplist** sắp theo `(score, member)` (`ZADD`/`ZRANK` O(log N), range O(log N + M)). Skiplist được chọn vì dễ hiện thực/debug, range query chỉ là đi tuần tự tầng đáy, tinh chỉnh bộ nhớ qua xác suất, hiệu năng tương đương cây.

**Giải thích chi tiết:**
```
L3: head ------------------------------> 50 --------------------> NIL
L2: head ----------> 20 ---------------> 50 ---------> 80 ------> NIL
L1: head -> 10 ----> 20 ----> 35 ------> 50 -> 60 ---> 80 -> 95 -> NIL
```
- Mỗi node thăng tầng với xác suất **1/4**, tối đa 32 tầng → tìm/chèn O(log N) kỳ vọng.
- Redis lưu **span** (số node nhảy qua) trên mỗi liên kết → tính **rank** trong O(log N).
- Score là `double` → số nguyên chính xác tới 2^53; mã hóa số tiền lớn hoặc ghép (điểm, thời gian) phải tính giới hạn này.

**Câu hỏi nối tiếp:**
- *Use case ZSet ngoài leaderboard?* — Sliding window rate limit, delay queue (score = thời điểm thực thi), index phụ theo thời gian, recent items.
- *`OBJECT ENCODING` trả gì cho ZSet 10 phần tử?* — `listpack` (Redis 7; trước đó `ziplist`).

**⚠️ Câu trả lời gây điểm trừ:**
- "ZSet dùng cây đỏ-đen" hoặc "dùng heap".
- Không biết `ZSCORE` là O(1) nhờ hashtable.

**📖 Ôn lại:** [Phần 4 — Sorted Set & skiplist](../01-giao-trinh/12-redis-caching.md#phan-4)

</details>

### Q11. 🟡 Encoding nhỏ (listpack, intset) là gì? Mẹo "hash bucketing" tiết kiệm bộ nhớ hoạt động thế nào?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Mỗi key có **type** logic và **encoding** vật lý. Tập nhỏ lưu dạng mảng liền kề gọn (listpack, intset) — tiết kiệm bộ nhớ, thân thiện CPU cache; vượt ngưỡng (`hash-max-listpack-entries` 128, giá trị ≤64 byte…) thì chuyển sang hashtable/skiplist. Mẹo Instagram: thay 300 triệu key String `media:{id}` bằng Hash `media:{id/1000}` field `{id%1000}` → mỗi hash nhỏ giữ encoding listpack → giảm bộ nhớ nhiều lần.

**Giải thích chi tiết:**
- Mỗi key top-level tốn ~50–70 byte overhead (dictEntry + redisObject + SDS) → hàng trăm triệu key nhỏ thì overhead lớn hơn dữ liệu.
- Đánh đổi của bucketing: không đặt TTL riêng từng field được (Redis 7.4 mới có `HEXPIRE`), thao tác phức tạp hơn, phải chỉnh ngưỡng listpack (tăng ngưỡng → thao tác O(N) trên listpack chậm hơn).
- Kiểm tra: `OBJECT ENCODING key`, `MEMORY USAGE key`.

**Câu hỏi nối tiếp:**
- *Một Hash 1 triệu field có tốt hơn 1.000 hash nhỏ?* — Không: hash lớn thành hashtable (mất lợi ích listpack) và thành **big key**.
- *String số nguyên lưu thế nào?* — Encoding `int`; chuỗi ≤44 byte là `embstr`.

**⚠️ Câu trả lời gây điểm trừ:**
- Không biết khái niệm encoding, cho rằng Hash luôn là hashtable.

**📖 Ôn lại:** [Phần 4 — Cấu trúc dữ liệu & internals](../01-giao-trinh/12-redis-caching.md#phan-4)

</details>

### Q12. 🟡 Dùng Redis List làm queue công việc có vấn đề gì? Khi nào dùng Stream?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** `RPOP` lấy message ra khỏi list ngay → consumer chết sau khi pop là **mất message** (không có ACK). Sửa bằng `LMOVE`/`BLMOVE` sang list "processing" rồi xóa khi xong (reliable queue), hoặc dùng **Stream** với consumer group: `XREADGROUP`, `XACK`, **PEL** (pending entries list), `XAUTOCLAIM` nhận lại message treo.

**Giải thích chi tiết:**
```bash
XADD orders:events * type CREATED orderId 1001
XGROUP CREATE orders:events billing $ MKSTREAM
XREADGROUP GROUP billing worker-1 COUNT 10 BLOCK 5000 STREAMS orders:events >
XACK orders:events billing 1717200000000-0
XAUTOCLAIM orders:events billing worker-2 60000 0   # nhận message treo > 60s
```
- Stream: at-least-once → consumer phải idempotent.
- Giới hạn độ dài bằng `MAXLEN ~`/`MINID`, nếu không stream phình mãi trong RAM.
- Stream vẫn là dữ liệu RAM, replication async → không thay Kafka khi cần retention dài, replay lớn, throughput rất cao.

**Câu hỏi nối tiếp:**
- *Message bị xử lý lỗi nhiều lần?* — Đếm delivery count trong `XPENDING`, vượt ngưỡng thì chuyển sang stream "dead letter".

**⚠️ Câu trả lời gây điểm trừ:**
- Dùng Pub/Sub làm queue quan trọng (subscriber offline là mất).

**📖 Ôn lại:** [Phần 4 — Ví dụ theo use case](../01-giao-trinh/12-redis-caching.md#phan-4) · [Phần 13 — Pub/Sub vs Streams](../01-giao-trinh/12-redis-caching.md#phan-13)

</details>

---

<a id="nhom-c"></a>
## C. Key, expiration, eviction & persistence

### Q13. 🟢 Bạn đặt tên key Redis theo quy ước nào? Vì sao nên có version trong key?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Phân cấp bằng `:` — `{service}:{entity}:{id}[:{field}]`, ví dụ `catalog:product:v2:42`; có tiền tố tenant khi multi-tenant; tập trung tạo key vào một class (`RedisKeys`). **Version** trong key cho phép đổi định dạng serialize mà không cần flush: code mới đọc key mới, key cũ tự hết hạn theo TTL.

**Giải thích chi tiết:**
- Key ngắn vừa phải nhưng **dễ đọc** quan trọng hơn khi debug.
- Trên Cluster, dùng **hash tag** `{...}` cho nhóm key nhỏ cần thao tác atomic chung (`cart:{u7}:items`, `cart:{u7}:meta`).
- Ghi rõ type, TTL, ai ghi/ai đọc cho từng loại key trong tài liệu.

**Câu hỏi nối tiếp:**
- *Deploy đổi cấu trúc class DTO mà không đổi version thì sao?* — Lỗi deserialize hàng loạt sau deploy; trong rolling deploy, pod cũ và mới đọc/ghi cùng key với 2 format.

**⚠️ Câu trả lời gây điểm trừ:**
- Key không chứa userId/tenant cho dữ liệu riêng tư.
- Dùng `KEYS prefix*` để xóa khi đổi format.

**📖 Ôn lại:** [Phần 5 — Thiết kế key](../01-giao-trinh/12-redis-caching.md#phan-5)

</details>

### Q14. 🟡 Redis xóa key hết hạn như thế nào? Key hết hạn có chiếm bộ nhớ không?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Kết hợp **lazy expiration** (khi client truy cập, thấy hết hạn thì xóa và trả như không tồn tại) và **active expiration** (chu kỳ nền theo `hz`, mặc định 10 lần/s, lấy mẫu ~20 key có TTL, xóa key hết hạn; nếu tỷ lệ hết hạn > 25% thì lặp lại, có giới hạn thời gian CPU). Vì vậy key hết hạn **có thể vẫn chiếm bộ nhớ** một lúc.

**Giải thích chi tiết:**
- `DBSIZE` có thể bao gồm key đã hết hạn chưa dọn.
- Replica không tự xóa key hết hạn — chờ `DEL` từ master, nhưng khi đọc vẫn trả như đã hết hạn (từ 3.2).
- Hàng triệu key cùng hết hạn → active expiry làm việc nhiều → có thể gây spike latency (thêm một lý do cho TTL jitter).

**Câu hỏi nối tiếp:**
- *Tăng `hz` thì sao?* — Dọn nhanh hơn, CPU nền cao hơn chút.
- *Theo dõi bằng gì?* — `INFO keyspace` (`expires`), `INFO stats` (`expired_keys`).

**⚠️ Câu trả lời gây điểm trừ:**
- "Mỗi key có một timer riêng, đúng giờ là xóa".

**📖 Ôn lại:** [Phần 5 — Expiration](../01-giao-trinh/12-redis-caching.md#phan-5)

</details>

### Q15. 🟡 Những lỗi TTL "âm thầm" nào hay gặp trong code?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** (1) `SET` lại một key có TTL mà không có `EX`/`KEEPTTL` → **TTL bị xóa**, key thành vĩnh viễn. (2) `INCR` rồi `EXPIRE` ở lệnh riêng → app chết giữa hai lệnh → counter vĩnh viễn. (3) Hàng triệu key cùng TTL → hết hạn cùng lúc (avalanche).

**Giải thích chi tiết:**
```bash
SET k v2 KEEPTTL          # 6.0+: giữ TTL cũ
EXPIRE k 60 NX            # 7.0+: chỉ đặt nếu chưa có TTL
```
- Gói `INCR` + `EXPIRE` vào Lua, hoặc `SET k 0 EX 60 NX` trước rồi `INCR`.
- `RedisTemplate.opsForValue().set(k, v)` không truyền `Duration` cũng là SET không TTL.
- Spring `@Cacheable` lấy TTL từ `RedisCacheConfiguration.entryTtl`; mặc định không TTL nếu không cấu hình.

**Câu hỏi nối tiếp:**
- *Làm sao phát hiện key không TTL trên prod?* — SCAN + `TTL` lấy mẫu; metric `expires` vs `keys` trong `INFO keyspace`.

**⚠️ Câu trả lời gây điểm trừ:**
- Không biết `SET` mặc định xóa TTL cũ.

**📖 Ôn lại:** [Phần 5 — Expiration](../01-giao-trinh/12-redis-caching.md#phan-5)

</details>

### Q16. 🟡 Các eviction policy của Redis? Chọn policy nào cho Redis cache thuần, và cho Redis vừa cache vừa lưu session/lock?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Mặc định `noeviction` (từ chối ghi khi chạm `maxmemory`). Cache thuần → `allkeys-lru` hoặc `allkeys-lfu` (phân bố lệch ổn định → LFU thường tốt hơn). Vừa cache vừa dữ liệu không được mất → `volatile-*` và chỉ đặt TTL cho key cache — nhưng tốt nhất là **tách instance**: một cho cache (evict), một cho dữ liệu (noeviction + persistence).

**Giải thích chi tiết:**

| Policy | Phạm vi | Ghi chú |
|---|---|---|
| `noeviction` | — | Lỗi `OOM command not allowed`, đọc vẫn chạy |
| `allkeys-lru` / `allkeys-lfu` / `allkeys-random` | Mọi key | |
| `volatile-lru` / `-lfu` / `-random` / `-ttl` | Chỉ key có TTL | Không key nào có TTL → hành xử như noeviction |

- LRU/LFU là **xấp xỉ**: lấy mẫu `maxmemory-samples` (mặc định 5) key; tăng lên 10 gần LRU thật hơn, tốn CPU hơn.
- **Luôn** đặt `maxmemory` (thường 60–75% RAM nếu có persistence để chừa cho fork COW, buffer, fragmentation); không đặt → OOM killer.

**Câu hỏi nối tiếp:**
- *Alert gì?* — `evicted_keys` tăng trên instance không phải cache thuần = mất dữ liệu; `used_memory/maxmemory > 80%`.

**⚠️ Câu trả lời gây điểm trừ:**
- Nói mặc định là LRU.
- Để lock/session chung instance cache với `allkeys-lru` → lock bị evict.

**📖 Ôn lại:** [Phần 5 — Eviction](../01-giao-trinh/12-redis-caching.md#phan-5)

</details>

### Q17. 🟢 So sánh RDB, AOF và hybrid persistence.

<details><summary>Đáp án</summary>

**Trả lời ngắn:** **RDB** = snapshot định kỳ bằng `fork()` + copy-on-write: file gọn, restart nhanh, nhưng mất dữ liệu kể từ snapshot cuối (vài phút). **AOF** = ghi lại mọi lệnh ghi, `appendfsync everysec` (mặc định) mất tối đa ~1 giây; file lớn hơn, cần rewrite. **Hybrid** (`aof-use-rdb-preamble yes`, mặc định từ 5.0): base dạng RDB + đuôi AOF → restart nhanh và mất ít.

**Giải thích chi tiết:**

| | RDB | AOF everysec | Hybrid |
|---|---|---|---|
| Mất dữ liệu khi crash | Vài phút | ~1s | ~1s |
| Restart | Nhanh | Chậm hơn | Nhanh |
| Chi phí | Fork định kỳ | fsync/giây + fork khi rewrite | Như AOF |

- `appendfsync always` an toàn nhất nhưng chậm; `no` để OS flush (~30s).
- Redis 7 **multi-part AOF**: thư mục `appendonlydir` gồm base + incremental + manifest.
- `SAVE` đồng bộ chặn server — không dùng trên prod.

**Câu hỏi nối tiếp:**
- *Cache thuần có cần persistence không?* — Có thể tắt (`save ""`, `appendonly no`) nếu chịu được cold start — nhưng xem bẫy ở Q19.
- *Backup thế nào?* — Copy RDB định kỳ ra object storage và **thử restore**.

**⚠️ Câu trả lời gây điểm trừ:**
- "Bật AOF always là Redis bền như PostgreSQL" — replication vẫn async, failover vẫn mất ghi.

**📖 Ôn lại:** [Phần 6 — Persistence](../01-giao-trinh/12-redis-caching.md#phan-6)

</details>

### Q18. 🔴 Máy 16GB RAM chạy Redis có persistence: đặt `maxmemory` bao nhiêu? Giải thích fork, copy-on-write, THP, `vm.overcommit_memory`.

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Khoảng **8–10GB** tùy tỷ lệ ghi. Khi BGSAVE/AOF rewrite, `fork()` tạo process con dùng chung trang nhớ (COW); trang nào bị ghi trong lúc snapshot sẽ bị copy → workload ghi nặng có thể làm RSS tăng gần **2x**. Cần chừa RAM cho COW, client buffer, fragmentation và OS. Tắt **Transparent Huge Pages** (COW với trang 2MB tốn hơn nhiều, gây spike latency) và đặt `vm.overcommit_memory = 1` để fork không thất bại.

**Giải thích chi tiết:**
- `latest_fork_usec`: fork phải copy page table, ~10–20ms/GB tùy phần cứng/ảo hóa → dataset vài chục GB có thể chặn hàng trăm ms.
- `rdb_last_cow_size` cho biết bộ nhớ copy thêm.
- Disk đầy → AOF ghi lỗi → Redis từ chối ghi (`MISCONF`).
- Shard nhỏ (nhiều instance nhỏ) giảm chi phí fork mỗi lần.

**Câu hỏi nối tiếp:**
- *Chỉ số nào cảnh báo?* — `latest_fork_usec > 500ms`, `rdb_last_bgsave_status`/`aof_last_write_status ≠ ok`, `mem_fragmentation_ratio`.

**⚠️ Câu trả lời gây điểm trừ:**
- Đặt `maxmemory` = 100% RAM hoặc không đặt.
- Không biết fork/COW làm bộ nhớ tăng.

**📖 Ôn lại:** [Phần 6 — Persistence](../01-giao-trinh/12-redis-caching.md#phan-6) · [Phần 3 — Hệ quả của đơn luồng](../01-giao-trinh/12-redis-caching.md#phan-3)

</details>

### Q19. 🔴 🎯 Tình huống: team tắt persistence trên master cho "nhanh", master có auto-restart (systemd/K8s). Sau một lần crash, toàn bộ replica trống trơn. Chuyện gì đã xảy ra?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Master crash → tự restart **trống** (không có RDB/AOF) → replica kết nối lại thấy master mới, **full sync** theo master → **dữ liệu replica bị xóa sạch** theo. Redis docs cảnh báo đúng trường hợp này. Sửa: bật persistence trên master, hoặc tắt auto-restart để Sentinel kịp promote replica, hoặc dùng Sentinel/Cluster để failover thay vì restart.

**Giải thích chi tiết:**
- Replication ID của master mới khác → không PSYNC được → full sync = replica nhận RDB rỗng.
- Nếu restart nhanh hơn `down-after-milliseconds` của Sentinel, Sentinel thậm chí không kịp phát hiện để failover.
- Với cache thuần chấp nhận cold start thì tắt persistence là OK — nhưng phải hiểu hệ quả này và có kế hoạch warm-up.

**Câu hỏi nối tiếp:**
- *Persistence chỉ bật trên replica được không?* — Được (giảm tải fork ở master), nhưng vẫn phải chặn kịch bản master restart trống.

**⚠️ Câu trả lời gây điểm trừ:**
- "Replica có bản sao nên không mất" — không hiểu replica luôn đi theo master.

**📖 Ôn lại:** [Phần 6 — Góc nhìn Senior](../01-giao-trinh/12-redis-caching.md#phan-6) · [Phần 7 — Replication](../01-giao-trinh/12-redis-caching.md#phan-7)

</details>

---

<a id="nhom-d"></a>
## D. Replication, Sentinel & Cluster

### Q20. 🟢 Replication của Redis hoạt động thế nào? Full sync và partial sync khác gì?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Master gửi luồng lệnh ghi **bất đồng bộ** tới replica. **Full sync**: replica mới hoặc lệch quá xa → master BGSAVE gửi RDB (hoặc diskless, mặc định từ 7.0) rồi gửi lệnh tích lũy. **Partial sync (PSYNC)**: replica gửi `replication ID + offset`; nếu offset còn trong **replication backlog** thì chỉ gửi phần thiếu.

**Giải thích chi tiết:**
- `repl-backlog-size` mặc định **1MB — thường quá nhỏ**; đứt mạng vài giây dưới tải ghi nặng là tràn → full sync liên tục. Nên tăng lên hàng trăm MB.
- Replica mặc định read-only; đọc từ replica = chấp nhận stale.
- `min-replicas-to-write 1` + `min-replicas-max-lag 10`: master từ chối ghi khi không đủ replica khỏe.

**Câu hỏi nối tiếp:**
- *Đọc replica có dùng cho lock/rate limit được không?* — Không; dữ liệu trễ.

**⚠️ Câu trả lời gây điểm trừ:**
- Nói replication là đồng bộ.

**📖 Ôn lại:** [Phần 7 — Replication](../01-giao-trinh/12-redis-caching.md#phan-7)

</details>

### Q21. 🟡 Sentinel failover diễn ra thế nào? Quorum khác majority ra sao? Vì sao cần ít nhất 3 Sentinel?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Một Sentinel không thấy master phản hồi trong `down-after-milliseconds` → **SDOWN**. Số Sentinel đồng ý ≥ **quorum** → **ODOWN**. Để thực sự failover, một Sentinel phải được **đa số** Sentinel bầu làm leader → chọn replica tốt nhất (priority, offset lớn nhất) → `REPLICAOF NO ONE` → cấu hình lại replica khác và thông báo master mới. Cần ≥3 Sentinel ở các zone khác nhau để vẫn có đa số khi mất một node/zone.

**Giải thích chi tiết:**
```properties
sentinel monitor mymaster 10.0.0.1 6379 2      # quorum = 2
sentinel down-after-milliseconds mymaster 5000
```
```yaml
spring.data.redis.sentinel:          # Boot 2.x: spring.redis.sentinel
  master: mymaster
  nodes: 10.0.0.11:26379,10.0.0.12:26379,10.0.0.13:26379
```
- Sentinel cũng là **service discovery**: client hỏi "master của mymaster là ai?".
- Thời gian gián đoạn ≈ `down-after-milliseconds` + bầu leader + promote (vài giây).
- Không scale ghi — vẫn một master.

**Câu hỏi nối tiếp:**
- *Ghi trong lúc failover có mất không?* — Có: ghi đã được master cũ xác nhận mà chưa replicate sẽ mất.
- *Split-brain?* — Master cũ bị cô lập vẫn nhận ghi → mất khi quay lại làm replica; giảm bằng `min-replicas-to-write`.

**⚠️ Câu trả lời gây điểm trừ:**
- Nhầm quorum là đủ để failover.
- Đặt 2 Sentinel hoặc đặt cùng máy với master.

**📖 Ôn lại:** [Phần 7 — Sentinel](../01-giao-trinh/12-redis-caching.md#phan-7)

</details>

### Q22. 🟡 Redis Cluster chia dữ liệu thế nào? Vì sao 16384 slot? Hash tag dùng khi nào?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Keyspace chia **16384 hash slot**, `slot = CRC16(key) mod 16384`; mỗi master giữ một tập slot và có replica, failover tự động giữa các node (không cần Sentinel). 16384 vì heartbeat gossip chứa bitmap slot: 16384 bit = **2KB**, đủ nhỏ, và cluster thiết kế cho tối đa ~1000 master nên đủ mịn. **Hash tag** `{...}`: chỉ phần trong ngoặc đầu tiên được băm → nhóm key nhỏ cùng slot để chạy multi-key/Lua.

**Giải thích chi tiết:**
- Multi-key (`MGET`, `MSET`, `SINTER`, transaction, Lua) chỉ chạy khi mọi key cùng slot → nếu không: `CROSSSLOT`.
- Chỉ có database 0.
- Lạm dụng hash tag (mọi key của tenant lớn chung tag) → dồn vào một slot → **hot shard**.
- Thêm/bớt node: reshard online (`redis-cli --cluster reshard/rebalance`).

**Câu hỏi nối tiếp:**
- *Vì sao không dùng consistent hashing?* — Slot cố định là một dạng "virtual node" tường minh: di chuyển slot giữa node rõ ràng, bảng slot nhỏ để client cache.

**⚠️ Câu trả lời gây điểm trừ:**
- Nghĩ Cluster tự hỗ trợ transaction đa key bất kỳ.

**📖 Ôn lại:** [Phần 7 — Redis Cluster](../01-giao-trinh/12-redis-caching.md#phan-7)

</details>

### Q23. 🔴 Phân biệt `MOVED` và `ASK`. Client Java cần cấu hình gì để không "kẹt" sau failover?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** `MOVED <slot> <host:port>`: slot đã thuộc node khác **vĩnh viễn** → client cập nhật bảng slot cache và gửi lại. `ASK`: slot **đang migrate**, key có thể đã sang node đích → client gửi `ASKING` + lệnh tới node đích **chỉ lần này**, **không** cập nhật bảng slot. Với Lettuce, bật **topology refresh** định kỳ và adaptive.

**Giải thích chi tiết:**
```java
ClusterTopologyRefreshOptions topo = ClusterTopologyRefreshOptions.builder()
    .enablePeriodicRefresh(Duration.ofSeconds(30))
    .enableAllAdaptiveRefreshTriggers()    // refresh khi gặp MOVED/ASK, reconnect...
    .build();
client.setOptions(ClusterClientOptions.builder().topologyRefreshOptions(topo).build());
```
- Trong lúc migrate, các key của slot nằm rải ở cả hai node — vì thế ASK chỉ là chuyển hướng tạm.
- Không refresh topology → sau failover client vẫn gửi tới node chết/node đã thành replica → timeout hoặc lỗi kéo dài.

**Câu hỏi nối tiếp:**
- *Spring Boot cấu hình thế nào?* — `spring.data.redis.lettuce.cluster.refresh.period` và `.adaptive=true` (Boot 2.3+ có thuộc tính tương ứng dưới `spring.redis.*`).

**⚠️ Câu trả lời gây điểm trừ:**
- "MOVED và ASK như nhau, đều redirect".

**📖 Ôn lại:** [Phần 7 — Redis Cluster](../01-giao-trinh/12-redis-caching.md#phan-7)

</details>

### Q24. 🔴 Redis có thể mất ghi đã xác nhận (acknowledged write) không? `WAIT` có biến Redis thành strongly consistent không?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** **Có**. Replication async: master ack cho client rồi mới gửi cho replica; master chết trước khi replicate → replica được promote không có ghi đó. Cả Sentinel và Cluster đều có thể mất ghi khi failover/partition. `WAIT numreplicas timeout` chặn client tới khi N replica nhận ghi — **giảm** rủi ro, nhưng không phải consensus: ghi vẫn được áp dụng trên master dù WAIT timeout, và failover có thể chọn replica chưa nhận.

**Giải thích chi tiết:**
- `min-replicas-to-write`/`min-replicas-max-lag`: master bị cô lập ngừng nhận ghi sau một thời gian → giới hạn cửa sổ mất.
- `appendfsync always` chỉ bảo vệ khỏi crash **cùng node**, không bảo vệ khỏi failover.
- Kết luận thiết kế: dữ liệu không được mất (tiền, đơn hàng) → nguồn sự thật là DB; Redis là cache/lớp tăng tốc.

**Câu hỏi nối tiếp:**
- *Điều này ảnh hưởng distributed lock thế nào?* — Lock trên master mất khi failover → hai client cùng giữ lock (động lực của Redlock, Q45).

**⚠️ Câu trả lời gây điểm trừ:**
- "Có replica nên không mất dữ liệu".

**📖 Ôn lại:** [Phần 6 — Góc nhìn Senior](../01-giao-trinh/12-redis-caching.md#phan-6) · [Phần 7](../01-giao-trinh/12-redis-caching.md#phan-7)

</details>

### Q25. 🟡 🎯 Tình huống: sau failover, app báo lỗi `READONLY You can't write against a read only replica`. Nguyên nhân và cách chọn giữa Sentinel và Cluster?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** App cấu hình **cứng IP master** (không qua Sentinel/không refresh topology) → sau failover master cũ thành replica, app vẫn ghi vào đó. Sửa: kết nối qua Sentinel (client hỏi Sentinel địa chỉ master) hoặc dùng cluster client có topology refresh. Chọn: dữ liệu vừa một máy, chỉ cần HA → **Sentinel** (đơn giản, mọi lệnh multi-key chạy); cần vượt RAM/throughput một máy → **Cluster**.

**Giải thích chi tiết:**
- Managed service (ElastiCache, Azure Cache, Memorystore) cung cấp endpoint primary dùng DNS — nhưng client cần xử lý DNS TTL/reconnect.
- Các lỗi khác trên Cluster: Lua truy cập key không qua `KEYS[]`; `SCAN` chỉ quét một node.

**Câu hỏi nối tiếp:**
- *Read từ replica trong Lettuce?* — `ReadFrom.REPLICA_PREFERRED`, chấp nhận stale; không dùng cho lock, counter, rate limit.

**⚠️ Câu trả lời gây điểm trừ:**
- Restart app như "giải pháp" mà không sửa cấu hình kết nối.

**📖 Ôn lại:** [Phần 7 — Replication, Sentinel & Redis Cluster](../01-giao-trinh/12-redis-caching.md#phan-7)

</details>

---

<a id="nhom-e"></a>
## E. Transaction, Lua & pipelining

### Q26. 🟢 `MULTI/EXEC` của Redis có rollback không?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** **Không**. MULTI/EXEC chỉ đảm bảo **cô lập** (các lệnh chạy liên tiếp, không xen lệnh client khác). Lệnh lỗi lúc **thực thi** (ví dụ `HSET` lên key String) thì các lệnh khác **vẫn chạy**. Chỉ lỗi cú pháp lúc xếp hàng mới hủy cả transaction (`EXECABORT`).

**Giải thích chi tiết:**
```bash
MULTI
SET a 1
HSET a f v       # WRONGTYPE lúc EXEC
SET b 2
EXEC             # → OK, WRONGTYPE..., OK  → a=1, b=2 vẫn được ghi
```
- Không dùng được kết quả lệnh trước làm input lệnh sau (kết quả chỉ có lúc EXEC).
- Spring: `RedisTemplate.execute(SessionCallback)` để MULTI trên cùng connection; hoặc `setEnableTransactionSupport(true)` để gắn với `@Transactional` — khi đó lệnh đọc trong transaction trả `null` vì bị xếp hàng.

**Câu hỏi nối tiếp:**
- *Vậy muốn "đọc rồi quyết định rồi ghi" atomic?* — WATCH (optimistic) hoặc Lua.

**⚠️ Câu trả lời gây điểm trừ:**
- So sánh MULTI với transaction ACID của DB.

**📖 Ôn lại:** [Phần 8 — MULTI/EXEC/WATCH](../01-giao-trinh/12-redis-caching.md#phan-8)

</details>

### Q27. 🟡 WATCH và Lua script — khác nhau thế nào, khi nào dùng cái nào?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** **WATCH** là optimistic locking (CAS): nếu key bị sửa giữa `WATCH` và `EXEC` thì `EXEC` trả `nil`, client tự retry — tốt khi tranh chấp thấp, nhiều round-trip. **Lua** chạy **atomic** trên server, có logic điều kiện, một round-trip, không retry — tốt khi tranh chấp cao; nhưng script phải ngắn vì chặn server.

**Giải thích chi tiết:**
```lua
-- Trừ kho không âm, atomic
local s = tonumber(redis.call('GET', KEYS[1]) or '0')
if s >= tonumber(ARGV[1]) then return redis.call('DECRBY', KEYS[1], ARGV[1]) else return -1 end
```
- Tranh chấp cao (50 thread chuyển điểm cùng user) → WATCH retry nhiều, throughput giảm; Lua ổn định.
- Lua **không rollback**: lệnh ghi trước khi lỗi vẫn giữ.

**Câu hỏi nối tiếp:**
- *Chuyển điểm giữa 2 user trên Cluster?* — 2 key khác slot thì không chạy chung Lua được; hash tag chung cho mọi user là phi thực tế → nghiệp vụ này nên làm ở DB.

**⚠️ Câu trả lời gây điểm trừ:**
- Dùng `GET` rồi `SET` từ Java để "trừ kho" (race).

**📖 Ôn lại:** [Phần 8 — Transaction, Lua script & pipelining](../01-giao-trinh/12-redis-caching.md#phan-8)

</details>

### Q28. 🟡 Pipelining khác MULTI và Lua thế nào? Khi nào dùng?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Pipeline gửi nhiều lệnh **không chờ** từng response → tiết kiệm RTT (10.000 lệnh × 0,5ms = 5s → còn vài chục ms), nhưng **không atomic**. Dùng cho batch đọc/ghi độc lập: warm-up cache, MGET-like với TTL, nạp dữ liệu.

**Giải thích chi tiết:**

| | MULTI/EXEC | Lua | Pipeline |
|---|---|---|---|
| Atomic | ✅ | ✅ | ❌ |
| Logic điều kiện | ❌ (chỉ WATCH) | ✅ | ❌ |
| Giảm RTT | Một phần | ✅ | ✅ |
| Cluster | Cùng slot | Cùng slot | Client tách lệnh theo node |

```java
List<Object> results = redisTemplate.executePipelined((RedisCallback<Object>) conn -> {
  for (long id : ids) conn.stringCommands().get(key(id).getBytes(UTF_8));
  return null;
});
```
- Chia batch vài trăm–vài nghìn lệnh để không phình output buffer.
- Lettuce async API tự nhiên pipeline trên một connection.

**Câu hỏi nối tiếp:**
- *Warm-up 1 triệu key có TTL?* — `MSET` không đặt TTL được → pipeline `SET k v EX ttl` với TTL có jitter.

**⚠️ Câu trả lời gây điểm trừ:**
- "Pipeline là transaction".

**📖 Ôn lại:** [Phần 8 — Pipelining](../01-giao-trinh/12-redis-caching.md#phan-8)

</details>

### Q29. 🔴 Những quy tắc khi viết Lua script cho Redis (đặc biệt trên Cluster)? Redis Functions là gì?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** (1) Mọi key truy cập phải truyền qua `KEYS[]` để Cluster định tuyến, và tất cả phải **cùng slot**; (2) script phải **nhanh** — chạy lâu chặn toàn server; quá `busy-reply-threshold` (5s) chỉ `SCRIPT KILL` được nếu script chưa ghi, nếu đã ghi thì chỉ còn `SHUTDOWN NOSAVE`; (3) nên deterministic; (4) dùng `EVALSHA` để không gửi lại body. **Functions** (Redis 7): `FUNCTION LOAD` + `FCALL` — script có tên, được persist và replicate như dữ liệu, thay dần EVAL cho logic dùng chung.

**Giải thích chi tiết:**
- Từ Redis 5, mặc định **effects replication** → gọi `TIME`/random trong script là hợp lệ (replica nhận lệnh kết quả, không chạy lại script).
- Spring: `DefaultRedisScript<Long>` + `redisTemplate.execute(script, keys, args)` tự xử lý `NOSCRIPT`.
- Script cache mất khi restart/failover → EVALSHA lỗi `NOSCRIPT`; thư viện tự fallback EVAL.

**Câu hỏi nối tiếp:**
- *Script cần đọc key tên động tính trong script?* — Không làm được an toàn trên Cluster; phải tính tên key ở client và truyền vào.

**⚠️ Câu trả lời gây điểm trừ:**
- Viết Lua vòng lặp hàng triệu phần tử trên production.

**📖 Ôn lại:** [Phần 8 — Lua script & Redis Functions](../01-giao-trinh/12-redis-caching.md#phan-8)

</details>

---

<a id="nhom-f"></a>
## F. Caching pattern & nhất quán DB–cache

### Q30. 🟢 Mô tả cache-aside (lazy loading): luồng đọc và luồng ghi.

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Ứng dụng tự quản lý cache. **Đọc**: tìm cache → hit thì trả; miss thì đọc DB → ghi cache kèm TTL → trả. **Ghi**: ghi DB → **xóa** key cache **sau khi commit** (không cập nhật). Ưu: đơn giản, chỉ cache dữ liệu thực sự được đọc, Redis chết vẫn chạy (chậm hơn). Nhược: miss đầu chậm, có cửa sổ không nhất quán, logic cache rải trong code.

**Giải thích chi tiết:**
```java
public Product getProduct(long id) {
  String key = RedisKeys.product(id);
  String json = redis.opsForValue().get(key);
  if (json != null) return NULL_MARKER.equals(json) ? null : mapper.readValue(json, Product.class);
  Product p = repo.findById(id).orElse(null);
  Duration ttl = p == null ? Duration.ofMinutes(1) : jitter(Duration.ofMinutes(10)); // null caching
  redis.opsForValue().set(key, p == null ? NULL_MARKER : mapper.writeValueAsString(p), ttl);
  return p;
}

@Transactional
public void updateProduct(Product p) {
  repo.save(p);
  TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
    @Override public void afterCommit() { redis.delete(RedisKeys.product(p.getId())); }
  });
}
```

**Câu hỏi nối tiếp:**
- *Vì sao cache DTO chứ không cache entity JPA?* — Entity có lazy collection → serialize kích hoạt lazy load hoặc `LazyInitializationException`; còn gắn chặt schema DB.

**⚠️ Câu trả lời gây điểm trừ:**
- Ghi: "cập nhật cache rồi cập nhật DB".
- Không có TTL, không xử lý null.

**📖 Ôn lại:** [Phần 9 — Cache-aside](../01-giao-trinh/12-redis-caching.md#phan-9)

</details>

### Q31. 🟡 So sánh read-through, write-through, write-behind, refresh-ahead. Cho ví dụ dùng mỗi loại.

<details><summary>Đáp án</summary>

**Trả lời ngắn:** **Read-through**: cache tự nạp khi miss (Caffeine `LoadingCache`, `@Cacheable`). **Write-through**: ghi vào cache, cache ghi DB **đồng bộ** rồi mới ack — tươi nhưng ghi chậm, cache phình. **Write-behind**: ghi cache, ack ngay, flush DB **bất đồng bộ theo lô** — throughput cao, rủi ro mất dữ liệu (view, like). **Refresh-ahead**: làm mới trước khi hết hạn (Caffeine `refreshAfterWrite`, XFetch) cho dữ liệu nóng.

**Giải thích chi tiết:**

| Pattern | Nhất quán | Rủi ro chính | Ví dụ |
|---|---|---|---|
| Cache-aside | Eventual, cửa sổ nhỏ | Stampede khi miss | Đa số use case |
| Read-through | Như trên | Phụ thuộc thư viện | Caffeine, Spring Cache |
| Write-through | Mạnh hơn | Ghi chậm | Cấu hình, dữ liệu đọc ngay sau ghi |
| Write-behind | Yếu | Mất dữ liệu | Counter, view |
| Refresh-ahead | Tốt cho dữ liệu nóng | Nạp thừa dữ liệu nguội | Trang chủ, tỷ giá |

- Write-behind cho lượt xem: `HINCRBY views:pending {postId} 1`; job mỗi 5s `RENAME` sang `views:processing:{ts}` (atomic), batch `UPDATE ... SET views = views + ?`, xóa key processing **sau** khi DB commit. Muốn chính xác tuyệt đối: lưu `batch_id` vào bảng `applied_batches` trong cùng transaction.
- **XFetch** (probabilistic early expiration): `now − delta × beta × ln(rand()) ≥ expiry` → recompute sớm với xác suất tăng dần khi gần hết hạn.

**Câu hỏi nối tiếp:**
- *Redis có tự write-through xuống DB không?* — Không; cần lib (Redisson `MapWriter`) hoặc tự code.

**⚠️ Câu trả lời gây điểm trừ:**
- Dùng write-behind cho số dư tài khoản.

**📖 Ôn lại:** [Phần 9 — Các caching pattern](../01-giao-trinh/12-redis-caching.md#phan-9)

</details>

### Q32. 🔴 Khi ghi DB, nên **cập nhật** cache hay **xóa** cache? Vẽ các race condition.

<details><summary>Đáp án</summary>

**Trả lời ngắn:** **Xóa** (cách Facebook mô tả trong *Scaling Memcache at Facebook*). Cập nhật cache có race hai writer ghi đè giá trị cũ lên giá trị mới, và lãng phí khi ghi nhiều đọc ít. Xóa vẫn còn một race hiếm (reader chậm nạp lại giá trị cũ) nhưng cửa sổ nhỏ hơn nhiều. Thứ tự "xóa cache rồi ghi DB" thì tệ nhất.

**Giải thích chi tiết:**

Race khi **update cache**:
```
T1: update DB price=100          T2: update DB price=200
                                 T2: set cache 200
T1: set cache 100   ← cache=100, DB=200 → sai tới hết TTL
```
Race còn lại khi **delete cache**:
```
Cache trống. T1 (đọc): miss → đọc DB (giá cũ 100)
             T2 (ghi): update DB=200 → delete cache
             T1: set cache = 100   ← cũ!
```
- Race thứ hai cần T1 đọc DB **trước** T2 ghi nhưng set cache **sau** T2 xóa — hiếm vì set cache nhanh hơn ghi DB, nhưng xảy ra với GC pause, mạng chậm.
- "Xóa cache → ghi DB": giữa hai bước, reader nạp giá trị cũ vào cache và giữ tới hết TTL.
- Giá trị cache thường là kết quả tổng hợp nhiều bảng → "cập nhật" đúng còn khó hơn.

**Câu hỏi nối tiếp:**
- *Thu nhỏ race thứ hai thế nào?* — Delayed double delete, version check khi set, lease (xem Q34, Q35).

**⚠️ Câu trả lời gây điểm trừ:**
- "Dùng transaction để DB và Redis nhất quán" — hai hệ thống không có transaction chung.
- Không nêu được race nào.

**📖 Ôn lại:** [Phần 10 — Cập nhật hay xóa cache](../01-giao-trinh/12-redis-caching.md#phan-10)

</details>

### Q33. 🟡 Xóa cache bên trong hay sau transaction? `@CacheEvict` đặt trên method `@Transactional` có an toàn không?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Phải xóa **sau khi commit** (`afterCommit`, `@TransactionalEventListener(phase = AFTER_COMMIT)`). Xóa trước commit → request khác miss, đọc DB vẫn thấy dữ liệu cũ (chưa commit), nạp lại cache cũ. `@CacheEvict` + `@Transactional` phụ thuộc thứ tự advisor; nếu evict chạy trước khi transaction commit thì gặp đúng lỗi trên. Dùng `RedisCacheManager.builder(cf).transactionAware()` để trì hoãn put/evict tới sau commit.

**Giải thích chi tiết:**
```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void onProductChanged(ProductChangedEvent e) {
  redis.delete(RedisKeys.product(e.id()));
}
```
- Xóa thất bại (Redis timeout) → đưa vào queue retry; nếu không, cache cũ sống tới hết TTL.
- Tương tự với lock: unlock phải sau commit (Q46).

**Câu hỏi nối tiếp:**
- *`afterCommit` chạy trên thread nào, lỗi có rollback DB không?* — Cùng thread, sau commit → lỗi không rollback được; phải tự log/retry.

**⚠️ Câu trả lời gây điểm trừ:**
- Gọi `redis.delete` ở dòng đầu của method `@Transactional`.

**📖 Ôn lại:** [Phần 10 — Kỹ thuật thu nhỏ cửa sổ](../01-giao-trinh/12-redis-caching.md#phan-10) · [Phần 14 — Spring Cache](../01-giao-trinh/12-redis-caching.md#phan-14)

</details>

### Q34. 🔴 Delayed double delete là gì? Khi nào cần, chọn delay bao nhiêu? Liên quan gì tới replica lag?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Xóa cache → ghi DB/commit → **chờ một khoảng** → xóa lần nữa (bất đồng bộ). Lần xóa thứ hai dọn giá trị cũ mà một reader chậm (race Q32) hoặc reader đọc **replica đang trễ** đã nạp vào cache. Delay ≥ replica lag tối đa + thời gian một lần đọc DB và set cache (ví dụ lag 1s → delay 1,5–2s).

**Giải thích chi tiết:**
```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void onProductChanged(ProductChangedEvent e) {
  String key = RedisKeys.product(e.id());
  redis.delete(key);
  scheduler.schedule(() -> redis.delete(key), 1, TimeUnit.SECONDS);
}
```
- Scheduler trong JVM mất khi pod chết → với dữ liệu quan trọng dùng delay queue bền (Kafka/Redis ZSet).
- Thay thế tốt hơn cho replica lag: **đường nạp cache đọc từ primary** khi vừa ghi (read-your-writes) hoặc không đọc replica cho cache.
- Không bao giờ "đúng tuyệt đối" — chỉ thu nhỏ xác suất; TTL vẫn là lưới an toàn.

**Câu hỏi nối tiếp:**
- *Delay quá dài/ngắn?* — Ngắn: không che được lag; dài: cửa sổ stale dài hơn và thêm một lần miss.

**⚠️ Câu trả lời gây điểm trừ:**
- Dùng `Thread.sleep` trong request thread để chờ xóa lần hai.

**📖 Ôn lại:** [Phần 10 — Nhất quán giữa DB và cache](../01-giao-trinh/12-redis-caching.md#phan-10)

</details>

### Q35. 🔴 Hệ thống có nhiều đường ghi (app, batch job, SQL tay). Làm sao invalidate cache không sót? Version check và lease giải quyết gì?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Dùng **CDC-based invalidation**: Debezium (hoặc Canal cho MySQL) đọc binlog/WAL → Kafka → consumer xóa key cache. Không sót đường ghi nào (kể cả SQL tay), có retry và thứ tự theo log; đổi lại thêm hạ tầng và độ trễ vài trăm ms. **Version check**: chỉ ghi cache nếu version mới hơn (Lua so sánh) → chống ghi đè bằng dữ liệu cũ. **Lease** (Facebook memcache): miss nhận token, delete làm token mất hiệu lực → set bằng token cũ bị từ chối.

**Giải thích chi tiết:**
```java
@KafkaListener(topics = "dbserver1.shop.products")
public void onChange(ConsumerRecord<String, String> rec) throws Exception {
  JsonNode payload = mapper.readTree(rec.value()).path("payload");
  JsonNode row = "d".equals(payload.path("op").asText()) ? payload.path("before") : payload.path("after");
  redis.delete(RedisKeys.product(row.path("id").asLong()));   // delete idempotent
}
```
```lua
-- Chỉ ghi nếu version mới hơn. KEYS[1]=key ARGV[1]=version ARGV[2]=json ARGV[3]=ttl
local cur = redis.call('HGET', KEYS[1], 'v')
if cur and tonumber(cur) >= tonumber(ARGV[1]) then return 0 end
redis.call('HSET', KEYS[1], 'v', ARGV[1], 'd', ARGV[2])
redis.call('EXPIRE', KEYS[1], ARGV[3])
return 1
```
- Consumer CDC cần `DefaultErrorHandler` + backoff + DLT khi Redis tạm chết.

**Câu hỏi nối tiếp:**
- *Version lấy ở đâu?* — Cột `@Version` của JPA/optimistic locking, hoặc `updated_at` đơn điệu.

**⚠️ Câu trả lời gây điểm trừ:**
- "Mỗi chỗ ghi nhớ gọi xóa cache" — không chịu được job batch/SQL tay.

**📖 Ôn lại:** [Phần 10 — Kỹ thuật thu nhỏ cửa sổ](../01-giao-trinh/12-redis-caching.md#phan-10)

</details>

### Q36. 🟡 🎯 Tình huống: PM muốn cache số dư ví và tồn kho để trang thanh toán nhanh hơn. Bạn trả lời thế nào?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** **Phân loại dữ liệu**: số dư, tồn kho **tại thời điểm trừ** phải đọc/ghi ở nguồn sự thật (DB với atomic update có điều kiện, hoặc Redis là nguồn sự thật duy nhất có đối soát); chỉ hiển thị (tồn kho "còn hàng") thì cache được, chấp nhận stale vài giây. Không có cách rẻ nào cho nhất quán **mạnh** giữa DB và cache.

**Giải thích chi tiết:**
- Trừ tiền: `UPDATE wallet SET balance = balance - :amt WHERE id = :id AND balance >= :amt` — DB là hàng phòng thủ cuối.
- Flash sale: Redis làm nguồn tồn kho (Lua trừ atomic), đơn hàng ghi DB bất đồng bộ idempotent, job đối soát sau sale.
- Hiển thị: cache-aside + delete after commit + TTL ngắn; có công cụ **purge thủ công**.

**Câu hỏi nối tiếp:**
- *Redis là nguồn tồn kho mà Redis failover mất ghi?* — Có thể oversell/undersell nhỏ → cần đối soát với DB và ràng buộc ở DB khi tạo đơn.

**⚠️ Câu trả lời gây điểm trừ:**
- Đồng ý cache số dư với TTL 5 phút và trừ tiền dựa trên giá trị cache.

**📖 Ôn lại:** [Phần 10 — Góc nhìn Senior](../01-giao-trinh/12-redis-caching.md#phan-10) · [Dự án mini FlashSale](../01-giao-trinh/12-redis-caching.md#du-an-mini)

</details>

---

<a id="nhom-g"></a>
## G. Sự cố cache: penetration, breakdown, avalanche, big/hot key

### Q37. 🟢 Phân biệt cache penetration, cache breakdown và cache avalanche.

<details><summary>Đáp án</summary>

**Trả lời ngắn:** **Penetration** (xuyên thủng): truy vấn dữ liệu **không tồn tại** → luôn miss → luôn chạm DB (bug hoặc tấn công id ngẫu nhiên). **Breakdown** (hotspot invalid): **một** key cực nóng hết hạn → hàng nghìn request cùng miss, cùng nạp (stampede). **Avalanche** (tuyết lở): **nhiều** key/toàn bộ cache mất cùng lúc (cùng TTL, Redis sập).

**Giải thích chi tiết:**

| Sự cố | Nguyên nhân | Giải pháp chính |
|---|---|---|
| Penetration | Key không tồn tại | Null caching TTL ngắn, Bloom filter, validate input, rate limit |
| Breakdown | 1 key nóng hết hạn | Mutex/single-flight, logical expiration, refresh-ahead |
| Avalanche | Nhiều key mất cùng lúc | TTL jitter, HA Redis, L1, circuit breaker, warm-up |

- Mọi trường hợp đều cần biện pháp **bảo vệ DB** (rate limit, bulkhead, circuit breaker).

**Câu hỏi nối tiếp:**
- *Cache null có nhược điểm gì?* — Tốn bộ nhớ khi bị tấn công bằng vô số id; phải invalidate khi bản ghi được tạo sau đó.

**⚠️ Câu trả lời gây điểm trừ:**
- Nhầm lẫn ba khái niệm hoặc dùng một giải pháp cho cả ba.

**📖 Ôn lại:** [Phần 11 — Sự cố cache](../01-giao-trinh/12-redis-caching.md#phan-11)

</details>

### Q38. 🟡 Bloom filter chống penetration thế nào? Tính kích thước cho 10 triệu id ở 1% false positive. Hạn chế?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Bloom filter trả lời "**chắc chắn không có**" hoặc "**có thể có**" (false positive, **không** false negative). Nạp mọi id hợp lệ; id không có trong filter → 404 ngay, không chạm DB. `m = −n·ln(p)/(ln 2)²` → n=10⁷, p=1% ≈ 9,6×10⁷ bit ≈ **11,4MB**, k≈7 hàm băm (0,1% ≈ 17,1MB, k≈10). Hạn chế: **không xóa** được phần tử (rebuild định kỳ hoặc Cuckoo filter), phải đồng bộ khi thêm id mới.

**Giải thích chi tiết:**
```java
RBloomFilter<Long> bf = redisson.getBloomFilter("bf:product-ids");
bf.tryInit(10_000_000L, 0.01);
bf.add(42L);                                     // khi tạo sản phẩm, sau commit
if (!bf.contains(id)) return Optional.empty();   // chắc chắn không tồn tại
```
- Lựa chọn: RedisBloom (`BF.ADD`, `BF.EXISTS`) trên Redis Stack/Redis 8, Redisson `RBloomFilter` (dựa trên Bitmap), Guava `BloomFilter` local.
- Kết hợp null caching + validate input + rate limit theo IP.

**Câu hỏi nối tiếp:**
- *Thêm sản phẩm mới mà quên `add` vào filter?* — False negative do lỗi vận hành → sản phẩm mới trả 404; phải add trong luồng tạo (sau commit) và rebuild định kỳ.

**⚠️ Câu trả lời gây điểm trừ:**
- Nói Bloom filter có thể false negative, hoặc xóa phần tử được.

**📖 Ôn lại:** [Phần 11 — Cache penetration](../01-giao-trinh/12-redis-caching.md#phan-11)

</details>

### Q39. 🔴 Key nóng 1.000 req/s, TTL 5s, loader 300ms. So sánh: cache-aside thường, mutex Redis, logical expiration.

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Cache-aside thường: mỗi lần hết hạn ~**300 lời gọi loader** (1.000 req/s × 0,3s) → DB bị đánh. Mutex: **1** lời gọi, các request khác chờ/retry → p99 tăng ~300ms. Logical expiration: **1** lời gọi chạy nền, mọi request trả giá trị cũ ngay → p99 không tăng, chấp nhận stale ~300ms.

**Giải thích chi tiết:**
```java
public Product getHot(long id) {
  CacheEntry<Product> e = read(key);                         // {data, expireAt}, key không đặt TTL ngắn
  if (e != null && e.expireAt().isAfter(Instant.now())) return e.data();
  String token = UUID.randomUUID().toString();
  if (Boolean.TRUE.equals(redis.opsForValue().setIfAbsent("lock:" + key, token, Duration.ofSeconds(5)))) {
    executor.submit(() -> {
      try { write(key, new CacheEntry<>(load(id), Instant.now().plusSeconds(60))); }
      finally { releaseLock("lock:" + key, token); }         // Lua compare-and-delete
    });
  }
  if (e != null) return e.data();                            // trả dữ liệu cũ
  return loadWithShortWait(id);                              // chưa có gì: chờ ngắn, có giới hạn
}
```
- Trong một JVM: Caffeine `get(key, loader)` hoặc `computeIfAbsent` của `CompletableFuture` (request coalescing). `@Cacheable(sync = true)` chỉ chống stampede **trong một JVM**.
- Khác: refresh-ahead bằng job, XFetch.

**Câu hỏi nối tiếp:**
- *Mutex lock không TTL thì sao?* — Instance giữ lock chết → không ai nạp lại được nữa.
- *Request chờ mutex bằng vòng `sleep` không giới hạn?* — Cạn thread pool; phải có deadline và fallback.

**⚠️ Câu trả lời gây điểm trừ:**
- "Đặt TTL dài hơn là xong" — chỉ dời thời điểm stampede.

**📖 Ôn lại:** [Phần 11 — Cache breakdown](../01-giao-trinh/12-redis-caching.md#phan-11) · [Phần 9 — Refresh-ahead](../01-giao-trinh/12-redis-caching.md#phan-9)

</details>

### Q40. 🟢 Làm sao phòng cache avalanche?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** (1) **TTL jitter**: `ttl = base + random(0, 10–20% base)` để key không hết hạn đồng loạt; (2) **HA** cho Redis (Sentinel/Cluster, nhiều AZ) + persistence; (3) **cache nhiều tầng** (L1 Caffeine đỡ khi Redis lỗi); (4) **circuit breaker + rate limit/bulkhead** quanh Redis và DB, trả dữ liệu degraded; (5) **warm-up** có kiểm soát trước khi mở traffic.

**Giải thích chi tiết:**
- Nguyên nhân hay gặp: warm-up lúc deploy nạp hàng loạt với TTL cố định → 10 phút sau tất cả cùng hết hạn.
- Spring Data Redis 3.2+ có `RedisCacheWriter.TtlFunction` để tính TTL động (jitter).
- Bảo vệ DB là phần quan trọng nhất — avalanche không thể loại trừ hoàn toàn (Redis có thể chết).

**Câu hỏi nối tiếp:**
- *Redis chết hẳn, app nên làm gì?* — Command timeout ngắn, circuit breaker mở → coi như miss, đi DB qua bulkhead; endpoint không thiết yếu trả degraded.

**⚠️ Câu trả lời gây điểm trừ:**
- Chỉ nêu jitter mà bỏ qua kịch bản Redis sập.

**📖 Ôn lại:** [Phần 11 — Cache avalanche](../01-giao-trinh/12-redis-caching.md#phan-11)

</details>

### Q41. 🟡 Big key là gì, tác hại, phát hiện và xử lý thế nào?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** String > ~10KB–1MB hoặc Hash/List/Set/ZSet hàng trăm nghìn–triệu phần tử. Tác hại: lệnh trên key chậm và **chặn server**, chiếm băng thông, lệch bộ nhớ giữa các node Cluster, `DEL` chặn, migrate slot chậm. Phát hiện: `redis-cli --bigkeys`, `--memkeys`, `MEMORY USAGE`, phân tích RDB offline. Xử lý: chia nhỏ (bucket `id % N`), nén, chỉ lưu field cần, xóa bằng `UNLINK` hoặc `HSCAN` + `HDEL` theo lô.

**Giải thích chi tiết:**
- Ví dụ xấu: cache cả danh sách 50.000 sản phẩm trong một key → mỗi request đọc vài MB.
- Thay vì đọc toàn bộ: phân trang (`ZRANGE` theo khoảng), lưu ID list + MGET chi tiết.
- Xóa big key: `UNLINK` trả về ngay, giải phóng ở background thread; bật `lazyfree-lazy-eviction`, `lazyfree-lazy-expire` cho eviction/expire.

**Câu hỏi nối tiếp:**
- *Big key ảnh hưởng replication?* — Lệnh trên key lớn sinh output lớn, có thể làm tràn replica buffer → full sync.

**⚠️ Câu trả lời gây điểm trừ:**
- Dùng `HGETALL` trên key 2 triệu field để "kiểm tra kích thước".

**📖 Ôn lại:** [Phần 11 — Big key & hot key](../01-giao-trinh/12-redis-caching.md#phan-11)

</details>

### Q42. 🔴 🎯 Tình huống: flash sale, một SKU bị đọc 30.000 lần/s. Trên Redis Cluster 3 master, một node CPU 100%, hai node rảnh. Bạn xử lý thế nào?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Đây là **hot key** — một key nằm trên một slot, một node, một thread. Thêm node không giúp. Giải pháp theo thứ tự: (1) **L1 local cache** (Caffeine TTL 1–2s) trên mỗi pod — giảm gần hết đọc vào Redis; (2) **nhân bản key** `product:42:copy:{0..N-1}` trên nhiều slot, đọc ngẫu nhiên một bản, ghi cập nhật mọi bản; (3) đọc từ replica nếu chấp nhận stale; (4) với **counter ghi nóng** — chia counter `stock:42:{0..N}` rồi cộng khi đọc.

**Giải thích chi tiết:**
- Phát hiện: `redis-cli --hotkeys` (cần `maxmemory-policy` LFU), `INFO commandstats`, CPU từng node, metric phía client; `MONITOR` chỉ vài giây.
- L1 trade-off: lệch tối đa vài giây giữa các pod — chấp nhận với trang chi tiết; **không** dùng cho tồn kho lúc trừ.
- Tồn kho flash sale: trừ bằng Lua trên **một** key (atomic) — nếu vẫn quá nóng thì chia tồn kho thành nhiều bucket (mỗi bucket một phần số lượng) và client chọn bucket còn hàng.

**Câu hỏi nối tiếp:**
- *Copy key thì invalidate thế nào?* — Ghi/xóa tất cả bản (pipeline); TTL ngắn làm lưới an toàn.
- *Biết trước key nóng?* — Warm-up L1 và Redis trước giờ mở bán.

**⚠️ Câu trả lời gây điểm trừ:**
- "Thêm node cho cluster" hoặc "reshard" — hot key vẫn ở một slot.

**📖 Ôn lại:** [Phần 11 — Big key & hot key](../01-giao-trinh/12-redis-caching.md#phan-11) · [Dự án mini FlashSale](../01-giao-trinh/12-redis-caching.md#du-an-mini)

</details>

---

<a id="nhom-h"></a>
## H. Distributed lock

### Q43. 🟢 Viết distributed lock với một Redis instance đúng cách.

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Lấy lock: `SET lock:key <random-token> NX PX 30000` — **một lệnh atomic**, có TTL chống deadlock khi client chết. Nhả lock: **Lua compare-and-delete** — chỉ xóa nếu token là của mình, tránh xóa lock của người khác khi lock mình đã hết hạn.

**Giải thích chi tiết:**
```java
public boolean tryLock(String key, String token, Duration ttl) {
  return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key, token, ttl));
}
private static final RedisScript<Long> UNLOCK = new DefaultRedisScript<>(
  "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end", Long.class);
public void unlock(String key, String token) { redis.execute(UNLOCK, List.of(key), token); }
```
- Chờ lock: vòng thử có **deadline** và backoff có jitter.
- `unlock()` luôn trong `finally`.

**Câu hỏi nối tiếp:**
- *TTL đặt bao nhiêu?* — Lớn hơn thời gian xử lý p99.9 đáng kể, hoặc dùng watchdog gia hạn (Q44).

**⚠️ Câu trả lời gây điểm trừ:**
- `SETNX` rồi `EXPIRE` ở lệnh riêng; `DEL` không kiểm tra token; token cố định (không random).

**📖 Ôn lại:** [Phần 12 — Lock đơn instance](../01-giao-trinh/12-redis-caching.md#phan-12)

</details>

### Q44. 🟡 Redisson watchdog hoạt động thế nào? Khi nào watchdog không cứu được?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Khi **không** truyền `leaseTime`, Redisson đặt TTL = `lockWatchdogTimeout` (mặc định **30s**) và cứ ~1/3 thời gian đó (~10s) gia hạn khi thread còn giữ lock; client chết → không ai gia hạn → lock tự hết sau ≤30s. Truyền `leaseTime` thì **không** có watchdog. Watchdog không cứu được khi **cả JVM bị dừng** (GC stop-the-world, swap) lâu hơn TTL — watchdog cũng bị dừng.

**Giải thích chi tiết:**
```java
RLock lock = redisson.getLock("lock:order:1001");
if (lock.tryLock(3, TimeUnit.SECONDS)) {          // chờ tối đa 3s, có watchdog
  try { process(); } finally { lock.unlock(); }
}
lock.tryLock(3, 10, TimeUnit.SECONDS);             // leaseTime 10s, KHÔNG watchdog
```
- Lưu dạng **Hash** (`field = UUID:threadId`, value = số lần reentrant) → **reentrant**; dùng Pub/Sub báo cho client chờ thay vì polling.
- `unlock()` từ thread khác → `IllegalMonitorStateException`.
- Biến thể: `FairLock`, `ReadWriteLock`, `MultiLock`, `Semaphore`, `CountDownLatch`.

**Câu hỏi nối tiếp:**
- *Dùng Redisson lock trong code reactive?* — Lock gắn với threadId, mà code reactive nhảy thread; khi đó dùng API reactive của Redisson hoặc truyền `threadId` tường minh.

**⚠️ Câu trả lời gây điểm trừ:**
- "Có watchdog nên lock không bao giờ hết hạn sai".

**📖 Ôn lại:** [Phần 12 — Redisson & watchdog](../01-giao-trinh/12-redis-caching.md#phan-12)

</details>

### Q45. 🔴 Redlock là gì? Tóm tắt tranh luận Kleppmann vs antirez và fencing token.

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Vấn đề: lock trên master, master chết trước khi replicate → replica mới không có lock → hai client cùng giữ. **Redlock**: N (thường 5) master độc lập, lấy lock trên **đa số** trong thời gian < TTL. **Kleppmann** phản biện: process pause (GC, swap) hoặc trễ mạng làm client tưởng còn giữ lock khi đã hết hạn — **không lock dựa trên TTL nào tự giải quyết được**; Redlock còn dựa vào giả định thời gian. Kết luận: lock để **hiệu quả** → 1 Redis đủ; lock để **đúng đắn** → cần **fencing token** + hệ thống đồng thuận (ZooKeeper, etcd).

**Giải thích chi tiết:**
- Fencing token: mỗi lần cấp lock kèm số **tăng đơn điệu**; tài nguyên đích từ chối token nhỏ hơn token lớn nhất đã thấy:
```sql
UPDATE inventory SET qty = :qty, fence = :token
WHERE sku = :sku AND fence < :token;   -- client "zombie" token cũ bị từ chối
```
- antirez ("Is Redlock safe?"): có thể dùng giá trị random làm điều kiện CAS, Redlock kiểm tra thời gian sau khi lấy lock. Tranh luận chưa ngã ngũ — trong phỏng vấn cần **trình bày cả hai phía** và chọn theo yêu cầu.
- etcd/ZooKeeper: lease/session gắn với kết nối, revision/zxid tăng đơn điệu dùng làm fencing token.

**Câu hỏi nối tiếp:**
- *Redis có cấp fencing token được không?* — `INCR fence:resource` khi lấy lock, nhưng bản thân Redis có thể mất ghi khi failover → token có thể lặp; tài nguyên đích vẫn phải kiểm tra.

**⚠️ Câu trả lời gây điểm trừ:**
- "Dùng Redlock là an toàn tuyệt đối".
- Không biết process pause là vấn đề.

**📖 Ôn lại:** [Phần 12 — Redlock & tranh luận](../01-giao-trinh/12-redis-caching.md#phan-12)

</details>

### Q46. 🔴 Lock được lấy bên trong method `@Transactional` và unlock ở `finally` — có lỗi gì?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** `finally` chạy **trước** khi proxy `@Transactional` commit → lock được nhả khi dữ liệu chưa commit → client khác lấy lock, đọc dữ liệu **cũ** (chưa thấy thay đổi), ghi đè → lost update. Sửa: lấy lock **bên ngoài** transaction (lock → gọi bean transactional → unlock), hoặc unlock trong `afterCompletion`.

**Giải thích chi tiết:**
```java
public void placeOrder(Cmd cmd) {                 // KHÔNG @Transactional ở đây
  RLock lock = redisson.getLock("lock:sku:" + cmd.sku());
  lock.lock();
  try { orderTxService.create(cmd); }             // bean khác, @Transactional → commit xong mới trả về
  finally { lock.unlock(); }
}
```
- Kể cả có lock, vẫn cần ràng buộc DB (unique, `UPDATE ... WHERE qty >= ?`, `@Version`) làm hàng phòng thủ cuối — lock chỉ giảm contention.
- Tương tự bẫy self-invocation: gọi method `@Transactional` cùng class qua `this` sẽ không có transaction.

**Câu hỏi nối tiếp:**
- *Có cần lock khi đã có optimistic locking?* — Thường không cho đúng đắn; lock chỉ giảm số lần retry khi tranh chấp cao.

**⚠️ Câu trả lời gây điểm trừ:**
- Không nhận ra thứ tự unlock/commit.

**📖 Ôn lại:** [Phần 12 — Lỗi thường gặp](../01-giao-trinh/12-redis-caching.md#phan-12)

</details>

### Q47. 🟡 🎯 Tình huống: chọn cơ chế cho (a) job đối soát cuối ngày chạy đúng một lần, (b) mỗi user chỉ một phiên thanh toán, (c) ghi file vào object storage chung.

<details><summary>Đáp án</summary>

**Trả lời ngắn:** (a) Redis lock đơn giản / **ShedLock** + job **idempotent** (bảng `job_runs` unique theo ngày). (b) **Unique constraint DB** (`user_id` trong `active_checkout`) — đúng đắn không dựa vào lock. (c) **Conditional write** (ETag/`If-Match`) hoặc fencing token từ etcd.

**Giải thích chi tiết:**
- Nguyên tắc: lock để **hiệu quả** (tránh làm trùng việc) → Redis; lock để **đúng đắn** → ràng buộc ở tài nguyên đích (DB constraint, CAS, fencing).
- ShedLock hỗ trợ backend Redis/JDBC, có `lockAtMostFor`/`lockAtLeastFor`.
- PostgreSQL advisory lock là lựa chọn tốt khi đã có PostgreSQL và lock gắn với session DB.

**Câu hỏi nối tiếp:**
- *Job chạy 2 lần dù có lock?* — Do lock hết hạn/failover → bảng `job_runs` unique làm job thứ hai thất bại an toàn.

**⚠️ Câu trả lời gây điểm trừ:**
- Một câu trả lời "dùng Redlock" cho cả ba.

**📖 Ôn lại:** [Phần 12 — Góc nhìn Senior chọn công cụ](../01-giao-trinh/12-redis-caching.md#phan-12)

</details>

---

<a id="nhom-i"></a>
## I. Rate limiting & use case kinh điển

### Q48. 🟡 So sánh các thuật toán rate limit: fixed window, sliding log, sliding counter, token bucket, leaky bucket.

<details><summary>Đáp án</summary>

**Trả lời ngắn:** **Fixed window** (`INCR` key theo phút) đơn giản, O(1), nhưng burst **gấp đôi** ở ranh giới. **Sliding log** (ZSET timestamp) chính xác nhưng bộ nhớ O(limit). **Sliding counter** ước lượng `prev × (1 − elapsed/window) + current`, O(1), sai số nhỏ (Cloudflare). **Token bucket** cho burst có kiểm soát (capacity) và tốc độ trung bình ổn định — phổ biến cho API public. **Leaky bucket** làm mượt đầu ra, không burst.

**Giải thích chi tiết:**
```lua
-- Sliding log. KEYS[1]=rl:{user} ARGV: nowMs, windowMs, limit, requestId
redis.call('ZREMRANGEBYSCORE', KEYS[1], 0, ARGV[1] - ARGV[2])
if redis.call('ZCARD', KEYS[1]) < tonumber(ARGV[3]) then
  redis.call('ZADD', KEYS[1], ARGV[1], ARGV[4])
  redis.call('PEXPIRE', KEYS[1], ARGV[2])
  return 1
end
return 0
```
- `requestId` phải duy nhất (UUID) — dùng timestamp thì hai request cùng ms ghi đè.
- Sliding log hợp với limit nhỏ, quan trọng (đăng nhập, OTP).
- Thư viện: Bucket4j (backend Redis), Spring Cloud Gateway `RedisRateLimiter` (token bucket).

**Câu hỏi nối tiếp:**
- *Fixed window `INCR` rồi `EXPIRE` có vấn đề gì?* — Không atomic → `EXPIRE ... NX` (7.0+) hoặc Lua.

**⚠️ Câu trả lời gây điểm trừ:**
- Rate limit bằng biến đếm trong JVM khi chạy nhiều instance (giới hạn theo pod, không toàn cục).

**📖 Ôn lại:** [Phần 13 — Rate limiting](../01-giao-trinh/12-redis-caching.md#phan-13)

</details>

### Q49. 🔴 Viết token bucket bằng Lua. Lấy thời gian ở đâu? Redis lỗi thì fail-open hay fail-closed?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Lưu `tokens` và `ts` trong Hash; mỗi request: refill `tokens = min(cap, tokens + elapsed × rate)`, nếu đủ thì trừ; toàn bộ trong Lua để atomic. Thời gian: truyền `now` từ client (cần đồng bộ đồng hồ các instance) hoặc `redis.call('TIME')` trong script (hợp lệ nhờ effects replication). Redis lỗi: **fail-open** cho API thông thường (giữ dịch vụ sống) kèm circuit breaker + limit local dự phòng; **fail-closed** cho endpoint đắt/nhạy cảm (đăng nhập, OTP, thanh toán).

**Giải thích chi tiết:**
```lua
-- KEYS[1]=tb:{user} ARGV: capacity, refillPerSec, nowMs, requested
local cap, rate, now, req = tonumber(ARGV[1]), tonumber(ARGV[2]), tonumber(ARGV[3]), tonumber(ARGV[4])
local b = redis.call('HMGET', KEYS[1], 'tokens', 'ts')
local tokens = tonumber(b[1]) or cap
local ts = tonumber(b[2]) or now
tokens = math.min(cap, tokens + (math.max(0, now - ts) / 1000.0) * rate)
local allowed = 0
if tokens >= req then tokens = tokens - req; allowed = 1 end
redis.call('HSET', KEYS[1], 'tokens', tokens, 'ts', now)
redis.call('PEXPIRE', KEYS[1], math.ceil(cap / rate * 1000) * 2)
return allowed
```
- Limit local dự phòng: Bucket4j in-memory với ngưỡng = limit / số instance.
- Trả `429` + `Retry-After`, `X-RateLimit-Remaining`.

**Câu hỏi nối tiếp:**
- *Vì sao `math.max(0, now - ts)`?* — Đồng hồ client lệch có thể làm `now < ts` → tránh token âm.
- *Key rate limit nên đặt TTL bao nhiêu?* — Đủ để bucket refill đầy (cap/rate) × hệ số an toàn; quá ngắn thì user được reset đầy.

**⚠️ Câu trả lời gây điểm trừ:**
- Đọc Hash về Java, tính, rồi ghi lại (race).

**📖 Ôn lại:** [Phần 13 — Rate limiting](../01-giao-trinh/12-redis-caching.md#phan-13)

</details>

### Q50. 🟡 Thiết kế leaderboard bằng Redis: top 10, hạng của tôi, xung quanh tôi, xử lý đồng điểm.

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Sorted Set: `ZINCRBY lb:game:2024-06 50 user:7`; top 10 `ZRANGE lb 0 9 REV WITHSCORES`; hạng `ZREVRANK lb user:7` (O(log N)); xung quanh `ZRANGE ... REV` với `rank±5`. Đồng điểm → ai đạt trước xếp trên: mã hóa `score = points × 10^k + (MAX_TS − ts)`, nhớ giới hạn chính xác **2^53** của double.

**Giải thích chi tiết:**
- Leaderboard theo kỳ: key theo tháng/tuần, TTL sau khi kỳ kết thúc + archive xuống DB.
- Hàng trăm triệu user: chia theo bucket/khu vực, hoặc chỉ tính chính xác top N, người ngoài top trả hạng xấp xỉ (histogram điểm).
- Ghi điểm vẫn nên có nguồn sự thật ở DB để rebuild khi Redis mất dữ liệu.

**Câu hỏi nối tiếp:**
- *Mặc định Redis xếp đồng điểm thế nào?* — Theo thứ tự lexicographic của member — không theo thời gian.

**⚠️ Câu trả lời gây điểm trừ:**
- `ORDER BY score LIMIT` trên DB mỗi request ở quy mô lớn.

**📖 Ôn lại:** [Phần 13 — Leaderboard](../01-giao-trinh/12-redis-caching.md#phan-13) · [Phần 4 — Sorted Set](../01-giao-trinh/12-redis-caching.md#phan-4)

</details>

### Q51. 🔴 🎯 Tình huống: `POST /payments` có `Idempotency-Key`. 100 request đồng thời cùng key, instance có thể chết giữa chừng. Thiết kế?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Hai lớp: **Redis** chặn nhanh — `SET idem:payment:<key> PROCESSING NX EX 60`; thắng thì xử lý, xong `SET ... DONE+response XX EX 86400`; thua thì DONE → trả response đã lưu, PROCESSING → `409`/`202`. **DB** là nguồn sự thật — unique constraint trên idempotency key **trong cùng transaction** tạo payment. Instance chết → key PROCESSING hết hạn sau 60s → client retry; nếu payment thật ra đã commit, unique constraint chặn → đọc payment cũ và trả lại.

**Giải thích chi tiết:**
- Lưu **hash request body** cùng key: cùng key nhưng body khác → `422`.
- PROCESSING TTL ngắn tách khỏi DONE TTL dài.
- Không chỉ dựa vào Redis: Redis có thể mất ghi khi failover → hai request cùng thắng `NX` → DB constraint cứu.

**Câu hỏi nối tiếp:**
- *Gọi PSP (cổng thanh toán) bên ngoài thì sao?* — Truyền idempotency key xuống PSP; nếu PSP không hỗ trợ, cần trạng thái "đã gửi" trong DB + đối soát.

**⚠️ Câu trả lời gây điểm trừ:**
- `GET` kiểm tra rồi `SET` (không atomic).
- Chỉ dùng Redis cho idempotency giao dịch tiền.

**📖 Ôn lại:** [Phần 13 — Idempotency key](../01-giao-trinh/12-redis-caching.md#phan-13)

</details>

### Q52. 🟢 Redis Pub/Sub và Streams khác nhau thế nào? Khi nào chuyển sang Kafka?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** **Pub/Sub** fire-and-forget: không lưu, subscriber offline là **mất**, không ACK — hợp cho invalidate L1, thông báo realtime không quan trọng. **Streams** là log append-only có consumer group, ACK, PEL, `XAUTOCLAIM` — queue quy mô vừa. Chuyển Kafka khi cần retention dài (ngày/tuần trên đĩa rẻ), replay lớn, throughput rất cao, hệ sinh thái connector.

**Giải thích chi tiết:**

| | Pub/Sub | Streams |
|---|---|---|
| Lưu trữ | Không | Có (`MAXLEN`/`MINID`) |
| Subscriber offline | Mất | Đọc lại từ ID |
| Consumer group/ACK | ❌ | ✅ |
| Cluster | Broadcast toàn cluster (7.0+ `SPUBLISH` sharded) | Một stream trên một slot |

- Streams vẫn trong RAM, replication async.

**Câu hỏi nối tiếp:**
- *Pub/Sub trên Cluster tốn gì?* — Pub/Sub thường broadcast mọi node → tốn băng thông cluster bus; dùng sharded pub/sub.

**⚠️ Câu trả lời gây điểm trừ:**
- Dùng Pub/Sub cho event nghiệp vụ cần đảm bảo giao.

**📖 Ôn lại:** [Phần 13 — Pub/Sub vs Streams](../01-giao-trinh/12-redis-caching.md#phan-13)

</details>

---

<a id="nhom-j"></a>
## J. Tích hợp Spring

### Q53. 🟢 Lettuce và Jedis khác nhau thế nào? Spring Boot dùng mặc định cái nào?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Spring Boot 2.x/3.x mặc định **Lettuce**: non-blocking trên Netty, một connection **dùng chung an toàn** cho nhiều thread, có sync/async/reactive, hỗ trợ Cluster/Sentinel tốt với topology refresh. **Jedis**: blocking socket, connection không thread-safe → cần `JedisPool` tính kích thước như DB pool; API sync đơn giản.

**Giải thích chi tiết:**
```yaml
spring:
  data:
    redis:              # Boot 2.x: spring.redis.*
      host: localhost
      timeout: 500ms    # command timeout — luôn đặt! mặc định 60s quá dài cho cache
      connect-timeout: 1s
      lettuce:
        pool:           # cần commons-pool2; chủ yếu cho lệnh blocking/transaction
          enabled: true
          max-active: 16
```
- Lệnh blocking (`BLPOP`) và `MULTI` cần connection riêng với Lettuce.

**Câu hỏi nối tiếp:**
- *Vì sao command timeout quan trọng?* — Redis chậm thì "thà miss còn hơn treo thread"; timeout 60s làm cạn thread pool Tomcat.

**⚠️ Câu trả lời gây điểm trừ:**
- Tạo pool Lettuce lớn như DB pool cho mọi lệnh "cho chắc".

**📖 Ôn lại:** [Phần 14 — Lettuce vs Jedis](../01-giao-trinh/12-redis-caching.md#phan-14)

</details>

### Q54. 🟡 Vì sao không nên dùng `JdkSerializationRedisSerializer` mặc định? Chọn serializer nào?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** `RedisTemplate<Object,Object>` mặc định dùng JDK serialization → key/value dạng nhị phân khó đọc (`\xac\xed...`), to, không dùng được từ ngôn ngữ khác, đổi class là vỡ (`serialVersionUID`), và **Java deserialization là lỗ hổng bảo mật**. Dùng `StringRedisTemplate` + Jackson tự quản, hoặc `GenericJackson2JsonRedisSerializer`/`Jackson2JsonRedisSerializer<T>`.

**Giải thích chi tiết:**
```java
@Bean
RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory cf, ObjectMapper om) {
  RedisTemplate<String, Object> t = new RedisTemplate<>();
  t.setConnectionFactory(cf);
  t.setKeySerializer(RedisSerializer.string());
  t.setHashKeySerializer(RedisSerializer.string());
  var json = new GenericJackson2JsonRedisSerializer(om.copy());
  t.setValueSerializer(json);
  t.setHashValueSerializer(json);
  return t;
}
```

| Serializer | Nhược điểm chính |
|---|---|
| JDK | Khó đọc, to, rủi ro bảo mật |
| `GenericJackson2Json` | Lưu tên class (`@class`) → đổi package là vỡ; cẩn thận default typing |
| `Jackson2Json<T>` | Một kiểu cố định |
| Protobuf/Kryo | Khó debug |

**Câu hỏi nối tiếp:**
- *Cache `Page`/`Optional` của Spring Data?* — Khó serialize JSON; cache DTO/record của riêng bạn.

**⚠️ Câu trả lời gây điểm trừ:**
- "Dùng mặc định cho nhanh" trên production.

**📖 Ôn lại:** [Phần 14 — RedisTemplate & serializer](../01-giao-trinh/12-redis-caching.md#phan-14)

</details>

### Q55. 🟡 `@Cacheable`: key mặc định sinh thế nào? `sync = true` làm gì? `condition` khác `unless`?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** `SimpleKeyGenerator`: không tham số → `SimpleKey.EMPTY`; 1 tham số → chính nó; nhiều → `SimpleKey(params...)`. Hai method khác nhau dùng chung `cacheNames` với cùng tham số → **đụng key** (`products::5`). `sync = true`: chỉ một thread **trong một JVM** nạp giá trị cho key (không phân tán); không dùng kèm `unless`, chỉ một cache, không kèm annotation cache khác. `condition` đánh giá **trước** khi gọi; `unless` **sau**, có `#result`.

**Giải thích chi tiết:**
```java
@Cacheable(cacheNames = "products", key = "'category:' + #id")
public List<ProductDto> findByCategory(long id) { ... }

@Cacheable(cacheNames = "products", key = "'brand:' + #id")
public List<ProductDto> findByBrand(long id) { ... }
```
- TTL theo cache qua `withCacheConfiguration`; jitter cần `TtlFunction` (Spring Data Redis 3.2+).
- `@CacheEvict(allEntries = true)` trên Redis = quét & xóa theo pattern — tốn kém.
- Không đặt `@CachePut` và `@Cacheable` trên cùng method.

**Câu hỏi nối tiếp:**
- *`disableCachingNullValues()` ảnh hưởng gì?* — Không cache null → mất chống penetration; muốn cache null phải chấp nhận `NullValue` placeholder.

**⚠️ Câu trả lời gây điểm trừ:**
- Nghĩ `sync = true` là distributed lock.

**📖 Ôn lại:** [Phần 14 — Spring Cache abstraction](../01-giao-trinh/12-redis-caching.md#phan-14)

</details>

### Q56. 🔴 Vì sao `@Cacheable` không hoạt động khi gọi method từ cùng class? Sửa thế nào?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Spring Cache (như `@Transactional`, `@Async`) hiện thực bằng **proxy AOP**; gọi nội bộ qua `this` không đi qua proxy → annotation bị bỏ qua. Method `private`/`final` cũng không được proxy. Sửa: tách method sang **bean khác** (khuyến nghị); self-inject qua `@Lazy`/`ObjectProvider`; hoặc AspectJ weaving (`@EnableCaching(mode = AdviceMode.ASPECTJ)`).

**Giải thích chi tiết:**
```java
@Service
class ReportService {
  public Report build(long id) { return compute(id); }   // this.compute → không qua proxy
  @Cacheable("reports") public Report compute(long id) { ... }
}
```
- Spring Boot mặc định dùng CGLIB proxy (subclass) → method `final` không override được.
- Test: `verify(repo, times(2))` dù gọi 2 lần cùng id qua `build()` → chứng minh cache không chạy.

**Câu hỏi nối tiếp:**
- *JDK dynamic proxy khác CGLIB thế nào?* — JDK proxy theo interface; CGLIB tạo subclass. Boot 2+ mặc định `proxyTargetClass=true`.

**⚠️ Câu trả lời gây điểm trừ:**
- "Do thiếu `@EnableCaching`" khi cache chạy ở chỗ khác.

**📖 Ôn lại:** [Phần 14 — Self-invocation pitfall](../01-giao-trinh/12-redis-caching.md#phan-14)

</details>

### Q57. 🟡 🎯 Tình huống: Redis chậm 2s, mọi API đều timeout dù chỉ dùng Redis làm cache. Bạn làm gì để cache lỗi không kéo sập nghiệp vụ?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** (1) **Command timeout ngắn** (100–500ms); (2) `CacheErrorHandler` **log và coi như miss** thay vì ném exception; (3) **circuit breaker** (Resilience4j) quanh truy cập Redis — OPEN thì bỏ qua Redis ngay; (4) **L1 Caffeine** vài giây đỡ đọc; (5) bulkhead bảo vệ DB khỏi lưu lượng dồn về.

**Giải thích chi tiết:**
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
}   // đăng ký qua CachingConfigurer#errorHandler()
```
- Kiểm thử bằng Toxiproxy (Testcontainers) giả lập latency 2s và Redis chết; metric Micrometer cho cache errors và trạng thái CB.
- Ngoại lệ: chức năng mà Redis là nguồn sự thật (tồn kho flash sale, lock) → **fail-closed** rõ ràng, không "bỏ qua".

**Câu hỏi nối tiếp:**
- *Đo latency Redis phía app bằng gì?* — Micrometer metric của Lettuce (`lettuce.command.completion`).

**⚠️ Câu trả lời gây điểm trừ:**
- Tăng timeout lên để "không lỗi nữa".

**📖 Ôn lại:** [Phần 14 — Góc nhìn Senior](../01-giao-trinh/12-redis-caching.md#phan-14) · [Phần 11 — Avalanche](../01-giao-trinh/12-redis-caching.md#phan-11)

</details>

---

<a id="nhom-k"></a>
## K. Giám sát & vận hành

### Q58. 🟢 Những chỉ số Redis nào bạn theo dõi và cảnh báo?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Bộ nhớ (`used_memory/maxmemory > 80%`), `evicted_keys` tăng, **hit ratio** (`keyspace_hits/(hits+misses)`) giảm đột ngột, `mem_fragmentation_ratio` (>1,5 phân mảnh, <1 đang swap), `connected_clients` gần `maxclients`, `blocked_clients`, replication lag/`master_link_status`, `latest_fork_usec`, trạng thái persistence, slowlog, và **latency p99 phía client**.

**Giải thích chi tiết:**
```bash
INFO memory | stats | clients | replication | persistence | commandstats
SLOWLOG GET 10                     # mặc định > 10ms
LATENCY DOCTOR
redis-cli --latency-history -i 5
```
- Công cụ: `redis_exporter` + Prometheus + Grafana; phía app: Micrometer Lettuce.
- Mỗi alert cần runbook: nguyên nhân khả dĩ, lệnh chẩn đoán, cách xử lý.

**Câu hỏi nối tiếp:**
- *Hit ratio giảm đột ngột sau deploy?* — Đổi format key/version, đổi serializer, flush nhầm, avalanche.

**⚠️ Câu trả lời gây điểm trừ:**
- Chỉ theo dõi CPU và RAM của máy.

**📖 Ôn lại:** [Phần 15 — Các chỉ số cần cảnh báo](../01-giao-trinh/12-redis-caching.md#phan-15)

</details>

### Q59. 🟡 Checklist bảo mật và cấu hình Redis production?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Bật **ACL** (Redis 6+) user tối thiểu quyền; **không expose ra Internet** (Redis mở bị chiếm qua `CONFIG SET dir` ghi SSH key); TLS khi qua mạng không tin cậy; chặn/đổi tên `FLUSHALL`, `KEYS`, `CONFIG`, `DEBUG` với user ứng dụng; đặt `maxmemory` + policy; `vm.overcommit_memory=1`; tắt THP; `tcp-keepalive`; giới hạn `client-output-buffer-limit` cho pubsub/replica; tăng `repl-backlog-size`.

**Giải thích chi tiết:**
- Tách instance cache (evict) và instance dữ liệu (noeviction + persistence).
- Backup RDB định kỳ + thử restore.
- Managed service vẫn cần hiểu failover, giới hạn lệnh bị vô hiệu hóa.

**Câu hỏi nối tiếp:**
- *Tại sao giới hạn output buffer cho pubsub?* — Subscriber chậm làm buffer phình → Redis OOM; vượt giới hạn thì ngắt client.

**⚠️ Câu trả lời gây điểm trừ:**
- "Redis trong mạng nội bộ nên không cần mật khẩu".

**📖 Ôn lại:** [Phần 15 — Góc nhìn Senior](../01-giao-trinh/12-redis-caching.md#phan-15)

</details>

### Q60. 🟡 🎯 Tình huống: code legacy dùng `redisTemplate.keys("session:*")` để đếm user online trên 5 triệu key. Bạn sửa thế nào?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** `KEYS` là O(N) toàn keyspace, chặn server nhiều giây. Ngắn hạn: thay bằng `SCAN` + xử lý theo lô (trên Cluster phải SCAN **từng master**). Đúng hơn: **đổi cấu trúc dữ liệu** để không cần quét — ZSET `online` với score = timestamp hoạt động cuối: `ZADD online <now> <userId>`, đếm `ZCOUNT online <now-5m> +inf`, dọn `ZREMRANGEBYSCORE online -inf <now-5m>`. Session hết hạn thì để TTL tự xử lý.

**Giải thích chi tiết:**
```java
try (Cursor<String> c = redis.scan(ScanOptions.scanOptions().match("session:*").count(1000).build())) {
  List<String> batch = new ArrayList<>();
  while (c.hasNext()) {
    batch.add(c.next());
    if (batch.size() == 500) { redis.unlink(batch); batch.clear(); }
  }
  if (!batch.isEmpty()) redis.unlink(batch);
}
```
- Đếm UV xấp xỉ thì HyperLogLog theo ngày.
- Chặn `KEYS` bằng ACL cho user ứng dụng để không tái phạm.

**Câu hỏi nối tiếp:**
- *SCAN có thể bỏ sót?* — Key tồn tại suốt quá trình quét sẽ được trả ít nhất một lần; key thêm/xóa trong lúc quét thì không đảm bảo.

**⚠️ Câu trả lời gây điểm trừ:**
- "Chạy `KEYS` vào ban đêm".

**📖 Ôn lại:** [Phần 15 — KEYS → SCAN](../01-giao-trinh/12-redis-caching.md#phan-15)

</details>

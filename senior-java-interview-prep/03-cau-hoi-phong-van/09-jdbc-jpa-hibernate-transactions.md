# Câu hỏi phỏng vấn — Module 09: JDBC, JPA/Hibernate, Spring Data & Transactions

> 📚 Giáo trình tương ứng: [Module 09 — Data Access: JDBC, JPA/Hibernate, Spring Data & Transactions](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md)

> **Cách dùng:** đọc câu hỏi, **tự trả lời thành tiếng** trong khoảng 30–90 giây (như đang ngồi trước người phỏng vấn) rồi mới mở phần đáp án. So sánh câu trả lời của bạn với mục *Trả lời ngắn* trước, sau đó đọc *Giải thích chi tiết* và tự trả lời luôn các *Câu hỏi nối tiếp*. Câu nào trả lời vấp → đánh dấu, quay lại mục *📖 Ôn lại* trong giáo trình.
>
> **Ký hiệu cấp độ:** 🟢 Cơ bản (Junior/Mid phải trả lời trôi chảy) · 🟡 Senior (cần cơ chế + trade-off) · 🔴 Xoáy sâu (câu "lọc" Senior, thường kèm kịch bản production).

## Mục lục

| Nhóm | Chủ đề | Câu hỏi |
|---|---|---|
| [A](#nhom-a) | JDBC & connection pool (HikariCP) | Q1–Q8 |
| [B](#nhom-b) | Persistence context, entity lifecycle, flush | Q9–Q17 |
| [C](#nhom-c) | Sinh ID & batching | Q18–Q20 |
| [D](#nhom-d) | Mapping quan hệ, kế thừa, equals/hashCode | Q21–Q26 |
| [E](#nhom-e) | Fetching, N+1, OSIV | Q27–Q32 |
| [F](#nhom-f) | Second-level cache & lựa chọn kiểu query | Q33–Q35 |
| [G](#nhom-g) | Spring Data JPA | Q36–Q40 |
| [H](#nhom-h) | Locking & concurrency control | Q41–Q45 |
| [I](#nhom-i) | Spring Transactions chuyên sâu | Q46–Q56 |
| [J](#nhom-j) | Transaction phân tán, MyBatis, schema migration | Q57–Q59 |

---

<a id="nhom-a"></a>
## A. JDBC & connection pool (HikariCP)

### Q1. 🟢 `Statement` và `PreparedStatement` khác nhau thế nào? Vì sao `PreparedStatement` chống được SQL injection?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `PreparedStatement` gửi câu SQL có placeholder `?` và giá trị tham số **tách biệt**; giá trị không bao giờ tham gia vào bước parse SQL nên không thể "biến" thành cú pháp SQL. Ngoài an toàn, nó còn cho phép DB/driver cache execution plan (đỡ hard parse) và type-safe (`setBigDecimal`, `setObject(OffsetDateTime)`). `Statement` nối chuỗi thì vừa injection vừa sinh ra mỗi giá trị một câu SQL khác nhau.

**Giải thích chi tiết:**
- Với `Statement`, input `x' OR '1'='1` được nối vào chuỗi rồi DB parse cả câu → điều kiện bị thay đổi.
- Với `PreparedStatement`, DB nhận "khuôn" câu lệnh và giá trị riêng (hoặc driver escape đúng chuẩn nếu là client-side prepare) — giá trị luôn chỉ là **literal**.
- Hiệu năng: Oracle soft parse thay vì hard parse (shared pool), PgJDBC chuyển sang server-side prepared statement sau `prepareThreshold` (mặc định 5) lần; MySQL cần `cachePrepStmts=true`.
- Giới hạn quan trọng: **không** tham số hóa được tên bảng, tên cột, `ORDER BY`, chiều sort, và số lượng phần tử `IN` biến đổi.

**Câu hỏi nối tiếp:**
- *JPA có bị injection không?* → Có, nếu nối chuỗi vào JPQL (`"... where name = '" + name + "'"`). Luôn dùng `:param`. MyBatis: `${}` là nối chuỗi, `#{}` mới là bind.
- *`IN` với danh sách động xử lý sao?* → Sinh đúng số `?`, hoặc PostgreSQL `= ANY(?)` với `createArrayOf`; Hibernate có `in_clause_parameter_padding` để giảm số câu SQL khác nhau.

**⚠️ Câu trả lời gây điểm trừ:**
- "PreparedStatement escape ký tự nháy đơn nên an toàn" — mô tả sai cơ chế và bỏ qua phần không tham số hóa được.
- "Dùng PreparedStatement là hết injection" — quên `ORDER BY`/tên cột phải whitelist.

**📖 Ôn lại:** [1.2 Statement vs PreparedStatement](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#1-jdbc)

</details>

### Q2. 🟡 Đoạn code sau có vấn đề gì? Sửa thế nào?

```java
public List<User> search(String keyword, String sortBy) throws SQLException {
    String sql = "SELECT * FROM users WHERE name LIKE ? ORDER BY " + sortBy;
    try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
        ps.setString(1, "%" + keyword + "%");
        ...
    }
}
```

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Dù dùng `PreparedStatement`, `sortBy` vẫn được **nối chuỗi** → SQL injection (`sortBy = "name; DROP TABLE users"` hoặc dạng blind injection qua `CASE WHEN ... THEN`). Sửa bằng **whitelist** ánh xạ giá trị client → tên cột hợp lệ. Ngoài ra `keyword` chứa `%`/`_` sẽ thành wildcard — cần escape ký tự LIKE.

**Giải thích chi tiết:**
```java
private static final Map<String, String> SORTABLE = Map.of(
        "name", "u.name", "created", "u.created_at");

String col = Optional.ofNullable(SORTABLE.get(sortBy))
        .orElseThrow(() -> new IllegalArgumentException("Invalid sort"));
String dir = asc ? "ASC" : "DESC";                         // boolean, không nhận chuỗi từ client
String sql = "SELECT id, name FROM users u WHERE u.name LIKE ? ESCAPE '\\' ORDER BY " + col + " " + dir;
ps.setString(1, "%" + escapeLike(keyword) + "%");          // escape \, %, _
```
- `SELECT *` cũng nên thay bằng danh sách cột cần.
- `LIKE '%x%'` không dùng được B-Tree index → bảng lớn cần full-text/trigram (xem Module 11).

**Câu hỏi nối tiếp:**
- *Spring Data `Sort` có an toàn không?* → `Sort.by(property)` được Spring Data kiểm tra với metamodel của entity (property không tồn tại → exception), nhưng với native query hoặc `JpaSort.unsafe` thì phải tự whitelist.

**⚠️ Câu trả lời gây điểm trừ:**
- "Không sao, đã dùng PreparedStatement rồi."
- Sửa bằng cách "lọc ký tự `;`" — blacklist luôn bị vượt qua.

**📖 Ôn lại:** [1.2 — lỗi thường gặp với ORDER BY/IN](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#1-jdbc)

</details>

### Q3. 🟡 Vì sao JDBC batch nhanh hơn nhiều so với insert từng dòng? Có gì cần lưu ý theo từng driver?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Mỗi `executeUpdate()` là một **network round-trip**; insert 10.000 dòng với RTT 1 ms tốn ~10 s chỉ để chờ mạng, chưa kể commit/fsync mỗi câu nếu autoCommit. Batch gom nhiều câu vào một round-trip, chạy trong một transaction. Nhưng phải bật cấu hình driver: MySQL `rewriteBatchedStatements=true`, PostgreSQL `reWriteBatchedInserts=true` thì batch insert mới được viết lại thành multi-values `INSERT ... VALUES (...),(...)`.

**Giải thích chi tiết:**
- Gọi `executeBatch()` theo lô (500–1000) để không giữ quá nhiều bộ nhớ.
- `executeBatch()` trả `int[]`, có thể là `SUCCESS_NO_INFO (-2)` khi driver rewrite → không dựa vào đó đếm chính xác.
- Lỗi giữa batch → `BatchUpdateException`; driver có thể dừng hoặc chạy tiếp tùy loại → luôn bọc batch trong transaction để rollback toàn bộ.
- Với ETL thuần (hàng triệu dòng), `COPY` (PostgreSQL) / `LOAD DATA` (MySQL) nhanh hơn batch nhiều lần.

**Câu hỏi nối tiếp:**
- *Hibernate có batch tự động không?* → Có nếu `hibernate.jdbc.batch_size > 0`, ID không phải `IDENTITY`, và nên bật `order_inserts/order_updates` (xem Q18–Q20).
- *Batch quá lớn có hại gì?* → Bộ nhớ phía client, transaction dài giữ lock/undo/WAL, packet vượt `max_allowed_packet` (MySQL).

**⚠️ Câu trả lời gây điểm trừ:**
- "Batch nhanh vì DB xử lý song song" — sai bản chất (lợi ích chính là round-trip + commit).
- Không biết batch mặc định của MySQL vẫn gửi từng câu nếu thiếu `rewriteBatchedStatements`.

**📖 Ôn lại:** [1.3 Batch](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#1-jdbc)

</details>

### Q4. 🔴 Kịch bản: job export `SELECT * FROM transaction` chạy tốt ở UAT (10k dòng) nhưng OOM ở production (20 triệu dòng) trên PostgreSQL. Bạn chẩn đoán và sửa thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** PgJDBC mặc định **load toàn bộ ResultSet vào heap**, bất kể bạn duyệt từng dòng. Muốn stream bằng cursor phải có đủ ba điều kiện: `autoCommit=false`, `fetchSize > 0`, statement `TYPE_FORWARD_ONLY`. Cách an toàn hơn cho job dài là **keyset pagination** theo PK (`WHERE id > :lastId ORDER BY id LIMIT 1000`) — mỗi trang một transaction ngắn, resume được khi lỗi.

**Giải thích chi tiết:**
| Cách | Bộ nhớ | Transaction | Nhược điểm |
|---|---|---|---|
| Cursor streaming (`fetchSize=1000`) | Ổn định | **Một transaction dài** suốt job | Giữ snapshot MVCC → chặn VACUUM/bloat (PG), history list dài (InnoDB); lỗi giữa chừng phải chạy lại từ đầu |
| Keyset theo PK | Ổn định | Nhiều transaction ngắn | Cần index phù hợp; dữ liệu thay đổi giữa các trang (thường chấp nhận được với export) |
| OFFSET | Ổn định | Ngắn | Trang sâu O(offset) → càng về sau càng chậm, có thể trùng/sót |

- Spring Data: `Stream<T>` + `@QueryHints(HINT_FETCH_SIZE)` phải chạy trong transaction và **đóng stream** (try-with-resources); với entity nhớ `em.detach`/`clear` để persistence context không phình.
- MySQL: streaming bằng `fetchSize = Integer.MIN_VALUE` hoặc `useCursorFetch=true`. Oracle mặc định fetch 10 dòng — quá nhỏ cho report, tăng lên 100–1000.

**Câu hỏi nối tiếp:**
- *Chứng minh đã hết OOM thế nào?* → Chạy với `-Xmx256m`, theo dõi heap/GC log ổn định, đo thời gian transaction mở.
- *Nếu cần snapshot nhất quán toàn bộ export?* → Chạy trên replica với `REPEATABLE READ` read-only, hoặc export từ snapshot/backup, chấp nhận transaction dài ở replica.

**⚠️ Câu trả lời gây điểm trừ:**
- "Tăng `-Xmx`" — chỉ dời sự cố sang lần dữ liệu tăng tiếp.
- "Đặt `setFetchSize(1000)`" nhưng quên `autoCommit=false` → PgJDBC vẫn load hết.

**📖 Ôn lại:** [1.4 ResultSet và fetch size](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#1-jdbc)

</details>

### Q5. 🟢 Vì sao cần connection pool? Kể vài thuộc tính HikariCP quan trọng và giá trị bạn hay đặt.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Mở connection vật lý tốn kém (TCP, TLS, xác thực, PostgreSQL còn fork một backend process) — vài đến hàng chục ms. Pool giữ sẵn N connection để cho mượn. HikariCP là pool mặc định của Spring Boot 2+. Các thuộc tính chính: `maximumPoolSize` (mặc định 10), `minimumIdle` (khuyến nghị bằng max → pool cố định), `connectionTimeout` (mặc định 30 s, nên 2–5 s để fail fast), `maxLifetime` (ngắn hơn timeout của DB/firewall/LB), `keepaliveTime`, `leakDetectionThreshold`.

**Giải thích chi tiết:**
- `connectionTimeout` 30 s mặc định nghĩa là khi pool cạn, thread Tomcat treo 30 s → thread pool web cạn theo → sự cố lan rộng. Fail fast giúp hệ thống phục hồi.
- `maxLifetime` phải nhỏ hơn `wait_timeout` (MySQL) hay idle timeout của firewall/NAT vài chục giây; Hikari thêm biến thiên ngẫu nhiên để không retire hàng loạt cùng lúc.
- `idleTimeout` chỉ có tác dụng khi `minimumIdle < maximumPoolSize`.
- `keepaliveTime` ping connection idle để tránh bị firewall cắt → lỗi "connection reset" sau giờ thấp điểm.

**Câu hỏi nối tiếp:**
- *Pool trả connection về ở trạng thái "bẩn" thì sao?* → Hikari reset `autoCommit`, isolation, readOnly… và rollback transaction dở khi close, nhưng đừng dựa vào đó làm thiết kế — luôn commit/rollback tường minh.
- *Metric nào cần theo dõi?* → `hikaricp.connections.active/idle/pending`, `acquire`, `usage`, `timeout`.

**⚠️ Câu trả lời gây điểm trừ:**
- "Pool càng lớn càng tốt."
- Không biết giá trị mặc định 30 s của `connectionTimeout` gây hại thế nào.

**📖 Ôn lại:** [2. Connection pooling với HikariCP](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#2-hikaricp)

</details>

### Q6. 🟡 Bạn chọn `maximumPoolSize` thế nào? Hệ thống có 10 pod, mỗi pod pool 50, PostgreSQL `max_connections=200` — vấn đề gì xảy ra?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Pool size phải tính cho **toàn bộ DB**, không phải cho từng instance. Điểm khởi đầu theo wiki HikariCP: `connections ≈ core_count × 2 + effective_spindle_count` (DB 8 core SSD ≈ 17). Tổng connection = `số pod × maximumPoolSize` (+ batch job, công cụ admin, pod surge khi rolling deploy). 10 × 50 = 500 > 200 → pod mới không lấy được connection, hoặc nếu DB cho phép thì RAM DB cạn (mỗi backend process vài MB) và context switching tăng.

**Giải thích chi tiết:**
- DB chỉ thực thi song song thực sự ~số core; connection thêm vào chỉ tăng tranh chấp CPU/lock. **Pool nhỏ + hàng đợi phía ứng dụng** thường cho throughput cao hơn, latency thấp hơn.
- Ví dụ phân bổ: DB 16 vCPU, A 8 pod × 10, B 4 pod × 10, batch 1 × 5 = 125; rolling deploy surge 25% → ~155 < 80% `max_connections=300`.
- Khi số pod lớn: PgBouncer transaction pooling (mất session state: `SET`, advisory lock theo session, `LISTEN`).
- Đo bằng load test và metric `pending`, `usage` p99 rồi điều chỉnh — công thức chỉ là điểm xuất phát.

**Câu hỏi nối tiếp:**
- *HPA scale lên 30 pod thì sao?* → Phải tính theo số pod **tối đa**; giới hạn maxReplicas hoặc giảm pool/pod, hoặc PgBouncer.
- *Khi nào cần pool lớn hơn công thức?* → Khi phần lớn thời gian connection là chờ I/O mạng/DB lock — nhưng thường đó là dấu hiệu nên sửa thiết kế (transaction ngắn lại).

**⚠️ Câu trả lời gây điểm trừ:**
- "Đặt 100 cho chắc."
- Tính pool cho một instance mà quên nhân số instance.

**📖 Ôn lại:** [2.2 Sizing — công thức và tư duy](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#2-hikaricp)

</details>

### Q7. 🔴 Production báo hàng loạt lỗi `SQLTransientConnectionException: shop-pool - Connection is not available, request timed out after 30000ms`. Bạn điều tra thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Lỗi này nghĩa là thread chờ mượn connection quá `connectionTimeout` — pool đã bão hòa. Tôi **không** tăng pool size ngay mà tìm vì sao connection bị giữ lâu: nhìn metric `hikaricp.connections.usage` (p99 thời gian giữ), `pending`, `active`; bật `leakDetectionThreshold` để có stacktrace nơi mượn connection; kiểm tra thread dump xem các thread đang giữ connection làm gì. Bốn nguyên nhân gốc phổ biến: (1) transaction bao quanh I/O mạng (HTTP, Kafka) hoặc OSIV giữ connection suốt request; (2) query chậm/thiếu index hoặc chờ lock DB; (3) connection leak; (4) pool deadlock do `REQUIRES_NEW` lồng nhau.

**Giải thích chi tiết:**
1. **Định lượng:** `active == max` và `pending > 0` kéo dài → bão hòa. `usage` p99 tăng vọt → giữ lâu; `usage` bình thường nhưng lưu lượng tăng → thật sự thiếu capacity.
2. **Thread dump** (`jstack`, Actuator `/threaddump`): nhiều thread kẹt ở `HikariPool.getConnection` (chờ) và nhóm thread đang giữ connection kẹt ở `SocketInputStream.read` của HTTP client → transaction gọi API ngoài.
3. **Phía DB:** `pg_stat_activity` (state `idle in transaction` → app giữ transaction mà không làm gì; `wait_event_type = Lock` → chờ lock), slow query log.
4. **Leak:** log `Connection leak detection triggered ... stack trace follows` chỉ thẳng dòng code; nếu sau đó "Previously reported leaked connection was returned" thì là chậm, không phải leak.
5. **Sửa theo gốc:** đưa I/O mạng ra ngoài transaction, tắt OSIV, thêm index, sửa leak (try-with-resources), bỏ `REQUIRES_NEW` lồng (dùng `@TransactionalEventListener`/outbox), đặt `connectionTimeout` 2–5 s để fail fast, thêm bulkhead/rate limit.

**Câu hỏi nối tiếp:**
- *Vì sao 30 s timeout làm sự cố tệ hơn?* → Thread Tomcat bị giữ 30 s mỗi request → web thread pool cạn → health check fail → pod bị restart dây chuyền.
- *Alert nào nên có?* → `max_over_time(hikaricp_connections_pending[1m]) > 0` kéo dài, `hikaricp_connections_timeout_total` tăng.

**⚠️ Câu trả lời gây điểm trừ:**
- "Tăng `maximumPoolSize` lên 100" — thường làm DB quá tải hơn và che giấu nguyên nhân.
- "Restart service" như giải pháp.

**📖 Ôn lại:** [2.4 Leak detection và quan sát pool](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#2-hikaricp)

</details>

### Q8. 🔴 Pool size = 10. Service A `@Transactional` gọi service B `@Transactional(propagation = REQUIRES_NEW)`. Bắn 20 request đồng thời thì toàn bộ treo đến timeout. Giải thích và sửa.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Đây là **pool deadlock**. `REQUIRES_NEW` tạm treo transaction ngoài (vẫn giữ connection thứ nhất) và cần connection thứ hai. 10 request đầu mỗi cái giữ 1 connection rồi cùng chờ connection thứ hai — không ai trả → treo đến `connectionTimeout`. Công thức pool tối thiểu tránh deadlock: `pool_size = Tn × (Cm − 1) + 1` với `Tn` số thread đồng thời, `Cm` số connection tối đa một thread cần đồng thời. 20 thread × (2−1) + 1 = 21.

**Giải thích chi tiết:**
- Cách sửa, ưu tiên theo thứ tự:
  1. **Loại bỏ nhu cầu 2 connection cùng lúc trên một thread**: ghi audit/event vào bảng **outbox** ngay trong transaction ngoài (chung connection); hoặc xử lý sau commit **bất đồng bộ** (`@TransactionalEventListener` + `@Async`) để thread gốc trả connection trước. Lưu ý: listener `AFTER_COMMIT` chạy đồng bộ + `REQUIRES_NEW` **vẫn** cần connection thứ hai, vì connection đầu chỉ được trả ở bước cleanup sau khi các callback after-commit chạy xong.
  2. **Giới hạn concurrency** trước khi vào service (bulkhead, Tomcat `max-threads`) để `Tn` nhỏ hơn.
  3. Tăng pool ≥ `Tn × (Cm − 1) + 1` với `Tn` = số thread tối đa có thể vào đoạn code — chỉ khi DB chịu được.
- `REQUIRES_NEW` còn có bẫy thứ hai: nếu inner cố khóa dòng mà outer đang giữ lock → **tự deadlock** (outer chờ inner xong, inner chờ lock của outer) — DB không phát hiện được vì là hai transaction của cùng một thread chờ nhau qua tầng ứng dụng, chỉ hết khi lock timeout.

**Câu hỏi nối tiếp:**
- *Tái hiện thế nào trong test?* → `maximum-pool-size=5`, 20 request đồng thời, quan sát `SQLTransientConnectionException`.
- *`NESTED` có cần connection thứ hai không?* → Không, nó dùng savepoint trên cùng connection (nhưng xem Q56 về NESTED với JPA).

**⚠️ Câu trả lời gây điểm trừ:**
- Đổ lỗi cho DB deadlock — DB không hề có chu trình lock, nghẽn nằm ở pool phía ứng dụng.
- Không biết `REQUIRES_NEW` cần connection riêng.

**📖 Ôn lại:** [2.2 — Deadlock do pool](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#2-hikaricp) · [11.3 Kịch bản REQUIRES_NEW](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#11-transactions)

</details>

---

<a id="nhom-b"></a>
## B. Persistence context, entity lifecycle, flush

### Q9. 🟢 Kể 4 trạng thái của một entity JPA và cách chuyển giữa chúng.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **Transient** (vừa `new`, chưa gắn persistence context), **Managed** (nằm trong persistence context — mọi thay đổi được dirty checking ghi xuống khi flush), **Detached** (có id nhưng context đã đóng/clear/detach — thay đổi không được lưu trừ khi `merge`), **Removed** (đã `remove`, sẽ `DELETE` khi flush). Chuyển: `persist` (transient→managed), `find`/query (DB→managed), `detach/clear/close` (managed→detached), `merge` (detached→trả về **bản managed khác**), `remove` (managed→removed).

**Giải thích chi tiết:**
- Persistence context là identity map `Map<EntityKey, Object>` + snapshot trạng thái ban đầu = first-level cache, phạm vi một `EntityManager` (thường một transaction trong Spring).
- `EntityManager` (JPA) ≈ `Session` (Hibernate). Spring Boot 3 dùng Hibernate 6 và `jakarta.persistence.*` (Boot 2 dùng `javax.persistence`).
- Entity trả về từ service sau khi transaction kết thúc (OSIV tắt) là **detached** → truy cập lazy association ném `LazyInitializationException`.

**Câu hỏi nối tiếp:**
- *`persist` có `INSERT` ngay không?* → Không nhất thiết; chỉ khi flush — trừ ID `IDENTITY` phải insert ngay để lấy id.
- *`find` hai lần cùng id trong một transaction?* → Một `SELECT`, và `a == b` là `true`.

**⚠️ Câu trả lời gây điểm trừ:**
- Nhầm "detached" là "đã xóa".
- Nói `merge` biến chính object truyền vào thành managed.

**📖 Ôn lại:** [3.2 Bốn trạng thái của entity](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#3-persistence-context)

</details>

### Q10. 🟡 `persist` và `merge` khác nhau thế nào? Đoạn code sau có bug gì?

```java
@Transactional
public void rename(OrderDto dto) {
    Order detached = mapper.toEntity(dto);   // có id
    em.merge(detached);
    detached.setNote("renamed");              // muốn lưu thêm note
}
```

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `persist` nhận entity transient, biến **chính nó** thành managed, trả `void`. `merge` nhận transient hoặc detached, **copy trạng thái** vào một bản managed (load từ context/DB, có thể thêm `SELECT`) và **trả về bản managed đó** — object truyền vào vẫn detached. Bug: `setNote` gọi trên object detached nên **không được lưu**; phải dùng giá trị trả về: `Order managed = em.merge(detached); managed.setNote(...)`.

**Giải thích chi tiết:**
| | `persist(e)` | `merge(e)` |
|---|---|---|
| Đầu vào | Transient (detached → `PersistentObjectException: detached entity passed to persist`) | Transient hoặc detached |
| Trả về | `void` | Bản sao managed |
| SQL | `INSERT` khi flush | Có thể `SELECT` trước rồi `UPDATE` khi flush |

- `merge` với DTO→entity còn nguy hiểm: field nào DTO không có (null) sẽ **ghi đè null** lên DB. Với use case update, cách chuẩn là `findById` rồi set từng field cần đổi (dirty checking lo phần còn lại).
- Spring Data: `entity = repo.save(entity)` — luôn dùng giá trị trả về.

**Câu hỏi nối tiếp:**
- *Vì sao `merge` có thể gây lost update?* → Ghi đè toàn bộ trạng thái từ object cũ client gửi lên; cần `@Version` trong DTO để phát hiện.

**⚠️ Câu trả lời gây điểm trừ:**
- "Hai cái như nhau, merge dùng cho update."
- Không nhận ra object truyền vào `merge` vẫn detached.

**📖 Ôn lại:** [3.3 persist vs merge](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#3-persistence-context)

</details>

### Q11. 🔴 Entity `Product` dùng UUID sinh ngay trong constructor. Gọi `productRepository.saveAll(list)` 1.000 sản phẩm thấy 1.000 câu `SELECT` trước 1.000 `INSERT`. Vì sao và sửa thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `SimpleJpaRepository.save()` gọi `entityInformation.isNew(entity) ? em.persist(entity) : em.merge(entity)`. `isNew` mặc định dựa vào **id null** (hoặc 0 với primitive) hoặc `@Version` null. Id đã gán sẵn → bị coi là "không mới" → `merge` → Hibernate `SELECT` để tìm bản trong DB trước khi insert. Sửa: implement `Persistable<ID>` với cờ `@Transient isNew` (đặt false ở `@PostPersist`/`@PostLoad`), hoặc thêm `@Version` kiểu wrapper (null = mới).

**Giải thích chi tiết:**
```java
@Entity
public class Product implements Persistable<UUID> {
    @Id private UUID id = UuidV7.next();
    @Transient private boolean isNew = true;
    @Override public boolean isNew() { return isNew; }
    @PostPersist @PostLoad void markNotNew() { this.isNew = false; }
    @Override public UUID getId() { return id; }
}
```
- Sau khi sửa: 0 `SELECT`, `INSERT` được batch (với `hibernate.jdbc.batch_size` + `order_inserts`).
- Chọn UUID v7/ULID thay vì v4 để không phân mảnh B-Tree index (insert ngẫu nhiên → page split).

**Câu hỏi nối tiếp:**
- *Cách nào khác?* → Dùng `em.persist` trực tiếp trong custom repository; hoặc dùng `@Version Long version` (null khi mới).
- *Đo số SQL bằng gì?* → datasource-proxy / Hibernate statistics / `SQLStatementCountValidator` trong test.

**⚠️ Câu trả lời gây điểm trừ:**
- "Hibernate luôn select trước insert" — không hiểu nhánh `persist`/`merge`.
- Đề xuất bỏ hẳn id tự gán sang `IDENTITY` (làm mất batch insert — xem Q18).

**📖 Ôn lại:** [3.3 — save() của Spring Data](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#3-persistence-context)

</details>

### Q12. 🟢 Dirty checking là gì? Trong method `@Transactional`, sau khi load entity và đổi field, có cần gọi `save()` không?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Khi load entity, Hibernate lưu một **snapshot** giá trị các thuộc tính. Lúc flush, nó so sánh trạng thái hiện tại với snapshot; khác → sinh `UPDATE`. Vì vậy với entity **managed** trong transaction, **không cần** gọi `save()` — gọi cũng không sai nhưng thừa và làm người đọc hiểu nhầm.

**Giải thích chi tiết:**
- Chi phí flush tỉ lệ với số entity managed × số thuộc tính, và bộ nhớ gấp đôi (snapshot). Load 100.000 entity chỉ để đọc → lãng phí.
- Cho use case chỉ đọc: `@Transactional(readOnly = true)` (Hibernate không giữ snapshot, `FlushMode.MANUAL`) hoặc DTO projection (không vào persistence context).
- Mặc định `UPDATE` cập nhật **tất cả cột**; `@DynamicUpdate` chỉ cập nhật cột đổi (đổi lại mất cache câu SQL, ít lợi cho batching).
- Bytecode enhancement (`enableDirtyTracking`) theo dõi thay đổi thay vì so sánh toàn bộ.

**Câu hỏi nối tiếp:**
- *Vô tình đổi field của entity trong method chỉ định đọc thì sao?* → Nếu không `readOnly`, thay đổi bị ghi xuống DB khi commit — bug "dữ liệu tự đổi" khó tìm. Lý do nên map sang DTO sớm.

**⚠️ Câu trả lời gây điểm trừ:**
- "Phải gọi `repository.save()` thì mới update."
- Không biết `readOnly` ảnh hưởng tới dirty checking.

**📖 Ôn lại:** [3.4 Dirty checking](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#3-persistence-context)

</details>

### Q13. 🟡 Flush khác commit thế nào? Khi nào Hibernate flush? Các `FlushMode` có ý nghĩa gì?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **Flush** = gửi các thay đổi đang chờ trong persistence context xuống DB dưới dạng SQL, vẫn **trong** transaction (có thể rollback). **Commit** = kết thúc transaction, làm thay đổi bền vững. Với `FlushMode.AUTO` (mặc định JPA), Hibernate flush (1) trước commit, (2) trước khi chạy JPQL/Criteria có thể bị ảnh hưởng bởi thay đổi chưa flush, (3) khi gọi `flush()` thủ công.

**Giải thích chi tiết:**
| FlushMode | Hành vi |
|---|---|
| `AUTO` | Flush trước commit và trước query liên quan; với native query trong chế độ JPA Hibernate flush để đúng spec |
| `COMMIT` | Chỉ flush khi commit — query có thể không thấy thay đổi chưa flush |
| `MANUAL` (Hibernate) | Chỉ khi gọi `flush()` — dùng cho read-only |
| `ALWAYS` (Hibernate) | Trước mọi query |

- Đây là **transactional write-behind**: cho phép gom SQL thành batch, nhưng lỗi constraint chỉ lộ ra lúc flush/commit — xa dòng code gây ra (xem Q15).

**Câu hỏi nối tiếp:**
- *Flush rồi nhưng rollback thì sao?* → SQL đã chạy trên DB nhưng nằm trong transaction → rollback xóa sạch; các transaction khác (RC trở lên) không bao giờ thấy.
- *`em.find` có trigger flush không?* → Không cần — nó tìm trong context trước.

**⚠️ Câu trả lời gây điểm trừ:**
- "Flush là commit."
- "Gọi `flush()` để dữ liệu được lưu vĩnh viễn ngay."

**📖 Ôn lại:** [3.5 Flush và FlushMode](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#3-persistence-context)

</details>

### Q14. 🔴 Bảng `coupon(code UNIQUE)`. Đoạn code sau ném lỗi vi phạm unique constraint. Vì sao?

```java
@Transactional
public void reissue(String code) {
    Coupon old = couponRepo.findByCode(code).orElseThrow();
    couponRepo.delete(old);
    couponRepo.save(new Coupon(code, LocalDate.now().plusDays(30)));
}
```

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Hibernate không thực thi SQL theo thứ tự bạn gọi mà theo thứ tự của `ActionQueue` khi flush: **insert → update → xóa phần tử collection → insert phần tử collection → delete**. Nên `INSERT` coupon mới chạy **trước** `DELETE` coupon cũ → trùng `code`. Sửa: gọi `flush()` sau khi xóa (`couponRepo.delete(old); couponRepo.flush();`), hoặc **cập nhật** entity cũ thay vì xóa-tạo, hoặc (PostgreSQL) constraint `DEFERRABLE INITIALLY DEFERRED`.

**Giải thích chi tiết:**
- Thứ tự này có lý do: insert trước để FK của các dòng mới hợp lệ, delete sau cùng để tránh vi phạm FK khi xóa cha còn con.
- Cách "update thay vì xóa-tạo" thường đúng về mặt nghiệp vụ hơn (giữ id, lịch sử).
- Với `@Transactional` test bao quanh và rollback cuối test, lỗi chỉ xuất hiện nếu có flush → test có thể **xanh giả** — nên có test không bọc transaction hoặc flush tường minh.

**Câu hỏi nối tiếp:**
- *Tương tự với `orphanRemoval` thay thế phần tử cùng unique key?* → Cùng vấn đề; flush sau khi remove.
- *Có cấu hình đổi thứ tự không?* → `order_inserts/order_updates` chỉ gom theo bảng, không đưa delete lên trước.

**⚠️ Câu trả lời gây điểm trừ:**
- "Do transaction chưa commit nên delete chưa có hiệu lực" — gần đúng nhưng không giải thích được thứ tự SQL.
- Đề xuất tách thành hai transaction (mất atomicity).

**📖 Ôn lại:** [3.5 — thứ tự SQL khi flush](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#3-persistence-context)

</details>

### Q15. 🟡 Đoạn code sau muốn trả HTTP 409 khi email trùng nhưng lỗi không bao giờ được bắt. Vì sao?

```java
@Transactional
public UserDto register(RegisterCmd cmd) {
    try {
        User u = userRepo.save(new User(cmd.email()));
        return UserDto.from(u);
    } catch (DataIntegrityViolationException e) {
        throw new EmailAlreadyUsedException();
    }
}
```

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Với ID không phải `IDENTITY`, `save()` chỉ đưa entity vào persistence context; `INSERT` chạy lúc **flush trước commit** — tức là sau khi method đã return, khi proxy `@Transactional` commit. Exception ném ra từ proxy, ngoài khối `try`. Sửa: `userRepo.saveAndFlush(...)` (hoặc `em.flush()`) trong `try`; tốt hơn nữa là để exception đi ra và map ở `@ControllerAdvice`, hoặc kiểm tra trước bằng `existsByEmail` **kèm** unique constraint làm chốt chặn cuối.

**Giải thích chi tiết:**
- Nếu bắt lỗi sau `flush()` bên trong transaction: transaction có thể đã bị đánh dấu rollback-only/ở trạng thái lỗi (PostgreSQL: "current transaction is aborted") → không nên tiếp tục ghi trong cùng transaction.
- `existsByEmail` một mình không đủ: hai request đồng thời cùng thấy "chưa có" → race. Unique constraint luôn là nguồn sự thật.

**Câu hỏi nối tiếp:**
- *Nếu ID là `IDENTITY` thì sao?* → `INSERT` chạy ngay khi `persist` → bắt được trong `try` (nhưng mất batch).
- *Map exception ở đâu là đẹp nhất?* → Tầng web (`@ExceptionHandler(DataIntegrityViolationException)`) kiểm tra tên constraint → 409.

**⚠️ Câu trả lời gây điểm trừ:**
- "Do Spring nuốt exception."
- Chỉ dùng `existsByEmail` để kiểm tra, không có unique constraint.

**📖 Ôn lại:** [3.6 — Góc nhìn Senior về write-behind](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#3-persistence-context)

</details>

### Q16. 🔴 Bạn phải import 1 triệu đơn hàng (mỗi đơn 3 line) qua JPA. Code `for (...) repo.save(...)` trong một `@Transactional` chạy càng lúc càng chậm rồi OOM. Thiết kế lại thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Persistence context giữ mọi entity + snapshot → phình bộ nhớ và dirty checking mỗi lần flush ngày càng chậm (O(n) entity). Sửa: (1) ID `SEQUENCE` + pooled optimizer (không `IDENTITY`); (2) `hibernate.jdbc.batch_size=50`, `order_inserts=true`, PostgreSQL `reWriteBatchedInserts=true`; (3) `em.flush(); em.clear();` mỗi `batch_size` entity; (4) chia thành nhiều transaction (ví dụ mỗi 10.000 đơn) để tránh transaction khổng lồ, undo/WAL lớn và có thể resume. Nếu chỉ là ETL thuần không cần logic domain → `JdbcTemplate.batchUpdate` hoặc `COPY`, nhanh hơn nhiều lần.

**Giải thích chi tiết:**
```java
public void importAll(Iterator<OrderDto> it) {
    while (it.hasNext()) {
        List<OrderDto> chunk = take(it, 10_000);
        tx.executeWithoutResult(s -> {
            for (int i = 0; i < chunk.size(); i++) {
                em.persist(OrderMapper.toEntity(chunk.get(i)));   // cascade lines
                if ((i + 1) % 50 == 0) { em.flush(); em.clear(); }
            }
        });
    }
}
```
- Không có `order_inserts`, xen kẽ `Order`, `OrderLine`, `Order`... sẽ cắt batch liên tục (batch chỉ gom câu lệnh giống hệt liên tiếp).
- Đọc CSV dạng streaming, không load toàn bộ file.
- Sau bulk load lớn, chạy `ANALYZE` để statistics cập nhật (Module 11).

**Câu hỏi nối tiếp:**
- *Chunk lỗi thì sao?* → Mỗi chunk rollback riêng; ghi nhận dòng lỗi, tiếp tục chunk khác (`TransactionTemplate` — Q53).
- *Vì sao không dùng `saveAll`?* → Vẫn đi qua `save()` → `merge` nếu id gán sẵn (Q11) và không tự `clear()`.

**⚠️ Câu trả lời gây điểm trừ:**
- "Tăng heap" hoặc "dùng `saveAll` cho nhanh".
- Bật `batch_size` nhưng giữ `IDENTITY` và tưởng đã batch.

**📖 Ôn lại:** [3.6 First-level cache](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#3-persistence-context) · [4.3 Batching](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#4-id-batching)

</details>

### Q17. 🟡 Sau khi chạy bulk JPQL `UPDATE Order o SET o.status = 'EXPIRED' WHERE ...`, code đọc lại entity `Order` đã load trước đó vẫn thấy status cũ. Vì sao?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Bulk update/delete JPQL chạy thẳng SQL xuống DB, **bỏ qua persistence context** — entity đã load trong context không được cập nhật nên trở nên stale. Và query sau đó dù chạy SQL thì các dòng trả về vẫn được "resolve" về entity đã có trong context (trạng thái trong bộ nhớ thắng). Sửa: Spring Data `@Modifying(clearAutomatically = true, flushAutomatically = true)`, hoặc `em.refresh()`/`em.clear()`.

**Giải thích chi tiết:**
- `flushAutomatically`: flush thay đổi đang chờ **trước** bulk, tránh bulk đè/bỏ sót thay đổi chưa ghi.
- `clearAutomatically`: clear context **sau** bulk — nhưng nếu không flush trước thì mọi thay đổi chưa flush bị **mất**.
- Bulk còn bỏ qua: `@Version` (phải tự `SET o.version = o.version + 1`), lifecycle callback (`@PreUpdate`, auditing `updatedAt`), cascade, và L2C (Hibernate invalidate cả region).
- Thiếu `@Modifying` trên query update → `InvalidDataAccessApiUsageException`.

**Câu hỏi nối tiếp:**
- *Khi nào nên dùng bulk thay vì load từng entity?* → Thay đổi hàng nghìn dòng theo điều kiện đơn giản, không cần logic domain; đổi lại mất callback/optimistic locking.

**⚠️ Câu trả lời gây điểm trừ:**
- "Do cache L2" — nhầm tầng cache.
- Không biết bulk bỏ qua `@Version` và auditing.

**📖 Ôn lại:** [3.6 First-level cache](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#3-persistence-context) · [9.5 @Modifying](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#9-spring-data)

</details>

---

<a id="nhom-c"></a>
## C. Sinh ID & batching

### Q18. 🟡 Vì sao `GenerationType.IDENTITY` làm mất JDBC batch insert? Bạn chọn chiến lược ID nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Với `IDENTITY`, id chỉ có sau khi DB thực hiện `INSERT`. Hibernate cần id ngay khi `persist` (để đưa vào identity map) nên phải `INSERT` **ngay lập tức**, từng câu một → không gom batch, phá write-behind. `SEQUENCE` lấy id trước bằng `nextval` → insert để dồn tới flush và batch được. Trên PostgreSQL/Oracle tôi chọn `SEQUENCE` + pooled optimizer (`allocationSize=50`); MySQL không có sequence thì cân nhắc `IDENTITY` (chấp nhận mất batch) hoặc ID sinh ở app (UUID v7/Snowflake/TSID).

**Giải thích chi tiết:**
| Strategy | Batch? | Ghi chú |
|---|---|---|
| `IDENTITY` | Không | Insert ngay khi persist |
| `SEQUENCE` | Có | Tốt nhất trên PG/Oracle, kết hợp optimizer |
| `TABLE` | Có nhưng chậm | Row-lock contention — tránh |
| `AUTO` | — | Hibernate 5+ trên MySQL chọn `TABLE` → nên khai báo rõ |
| `UUID` (JPA 3.1) | Có | v4 ngẫu nhiên phân mảnh B-Tree; ưu tiên v7 |

**Câu hỏi nối tiếp:**
- *UUID làm PK trên MySQL có vấn đề gì?* → InnoDB clustered index theo PK → UUID v4 insert ngẫu nhiên, page split, mọi secondary index chứa PK 16 byte (Module 11).
- *Id có lỗ hổng (gap) sau restart?* → Bình thường; đừng dùng surrogate id làm số hóa đơn liên tục.

**⚠️ Câu trả lời gây điểm trừ:**
- Để mặc định `AUTO` mà không biết nó chọn gì.
- "IDENTITY nhanh nhất vì DB tự sinh."

**📖 Ôn lại:** [4.1 Các chiến lược @GeneratedValue](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#4-id-batching)

</details>

### Q19. 🔴 Hai instance ứng dụng cùng insert vào bảng `payment` và thỉnh thoảng bị `duplicate key value violates unique constraint "payment_pkey"`. Entity có `allocationSize = 50`. Bạn nghi điều gì?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Nghi **sequence trong DB `INCREMENT BY 1` không khớp `allocationSize = 50`**. Pooled optimizer coi giá trị `nextval` là biên trên của một khối 50 id và tự cấp phát phần còn lại trong bộ nhớ. Nếu sequence chỉ tăng 1, hai node gọi `nextval` liên tiếp nhận 101 và 102 nhưng đều tự cấp các khối gần như trùng nhau → trùng khóa. Sửa: `ALTER SEQUENCE payment_seq INCREMENT BY 50` sau khi `setval` vượt `max(id)`; giữ `ddl-auto=validate` (Hibernate 6 có `hibernate.id.sequence.increment_size_mismatch_strategy`, mặc định `EXCEPTION`).

**Giải thích chi tiết:**
```sql
-- Migration sửa (chạy khi dừng ghi, hoặc chấp nhận gap)
SELECT setval('payment_seq', (SELECT max(id) FROM payment) + 50);
ALTER SEQUENCE payment_seq INCREMENT BY 50;
```
- Pooled optimizer an toàn khi có nhiều writer (script SQL, app khác gọi `nextval` trực tiếp) — khác optimizer `hilo` cũ.
- Lỗi này thường do Flyway script viết tay `CREATE SEQUENCE ... INCREMENT BY 1` hoặc do ai đó tắt validate.

**Câu hỏi nối tiếp:**
- *Vì sao trên một node lỗi ít xuất hiện?* → Một node cấp tuần tự từ khối của nó; trùng xảy ra khi khối của các node chồng lấn hoặc sau restart.
- *Oracle `CACHE` của sequence khác `allocationSize` thế nào?* → `CACHE` là bộ nhớ đệm phía DB (giảm contention dictionary), `allocationSize` là cấp phát phía Hibernate; hai thứ độc lập.

**⚠️ Câu trả lời gây điểm trừ:**
- "Sequence bị lỗi race condition" — sequence của DB luôn atomic.
- Sửa bằng cách đặt `allocationSize = 1` (mất lợi ích giảm round-trip).

**📖 Ôn lại:** [4.2 Sequence + pooled optimizer](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#4-id-batching)

</details>

### Q20. 🟡 Bạn đã đặt `hibernate.jdbc.batch_size=50` nhưng log vẫn thấy các `INSERT` được gửi lẻ. Kiểm tra những gì?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Checklist: (1) ID có phải `IDENTITY` không (nếu có → không batch insert); (2) có bật `order_inserts`/`order_updates` không — persist xen kẽ nhiều loại entity sẽ cắt batch; (3) driver có rewrite không (`reWriteBatchedInserts` PG, `rewriteBatchedStatements` MySQL) — nếu không, batch JDBC vẫn tới DB nhưng mỗi câu riêng; (4) log có đánh lừa không — `show_sql` in từng câu kể cả khi batch, phải xem bằng datasource-proxy (`batch: true, size: 50`) hoặc Hibernate statistics; (5) entity `@Version` cần `batch_versioned_data=true` (mặc định true từ Hibernate 5).

**Giải thích chi tiết:**
```properties
spring.jpa.properties.hibernate.jdbc.batch_size=50
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true
spring.datasource.hikari.data-source-properties.reWriteBatchedInserts=true
```
- `@DynamicUpdate` làm mỗi UPDATE có thể khác nhau → giảm khả năng batch update.
- Batch chỉ gom các câu **giống hệt nhau liên tiếp**.

**Câu hỏi nối tiếp:**
- *Batch delete thì sao?* → Bulk JPQL `DELETE ... WHERE id IN :ids` thay vì load rồi remove từng cái (nhưng mất cascade/callback).

**⚠️ Câu trả lời gây điểm trừ:**
- Kết luận "batch không chạy" chỉ dựa vào `show_sql`.

**📖 Ôn lại:** [4.3 Cấu hình batching trong Hibernate](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#4-id-batching)

</details>

---

<a id="nhom-d"></a>
## D. Mapping quan hệ, kế thừa, equals/hashCode

### Q21. 🟢 Owning side và `mappedBy` là gì? Đoạn code sau lưu `order_line.order_id` bằng gì?

```java
Order order = orderRepo.findById(id).orElseThrow();
OrderLine line = new OrderLine(product, 2);
order.getLines().add(line);   // lines: @OneToMany(mappedBy = "order", cascade = ALL)
```

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Trong DB quan hệ 1-N chỉ có **một** FK (`order_line.order_id`), nhưng trong Java có hai tham chiếu. JPA cần biết phía nào sở hữu cột FK — **owning side** là phía `@ManyToOne` (có `@JoinColumn`); phía `@OneToMany(mappedBy = "order")` là **inverse side**, chỉ để điều hướng, thay đổi ở đây bị bỏ qua khi ghi FK. Code trên: line được persist (nhờ cascade) nhưng `line.order` null → `order_id = NULL` (hoặc lỗi nếu cột `NOT NULL`).

**Giải thích chi tiết:**
```java
public void addLine(OrderLine line) {   // helper method giữ HAI phía đồng bộ
    lines.add(line);
    line.setOrder(this);
}
public void removeLine(OrderLine line) {
    lines.remove(line);
    line.setOrder(null);
}
```
- Helper method là bắt buộc với quan hệ hai chiều; collection nên được đóng gói (không expose setter thay cả list).

**Câu hỏi nối tiếp:**
- *Chỉ cần một chiều thì nên giữ chiều nào?* → `@ManyToOne` (rẻ nhất, khớp với FK). `@OneToMany` một chiều có vấn đề (Q22).

**⚠️ Câu trả lời gây điểm trừ:**
- "Hibernate tự set FK vì đã add vào list."
- Nhầm `mappedBy` nằm ở phía `@ManyToOne`.

**📖 Ôn lại:** [5.1 Owning side và mappedBy](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#5-mapping)

</details>

### Q22. 🟡 `@OneToMany` một chiều (không `mappedBy`, không `@JoinColumn`) có vấn đề gì về schema và hiệu năng?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Hibernate coi đó là quan hệ qua **bảng join** (`orders_lines`) — thêm một bảng thừa, mỗi lần đọc phải JOIN thêm, và với `List` (bag) thì thêm/xóa một phần tử khiến Hibernate `DELETE` toàn bộ dòng của cha trong bảng join rồi `INSERT` lại. Thêm `@JoinColumn(name = "order_id")` bỏ được bảng join nhưng vẫn sinh thêm `UPDATE` để set FK sau `INSERT`. Tốt nhất: hai chiều với `mappedBy`, hoặc chỉ `@ManyToOne`.

**Giải thích chi tiết:**
- Collection rất lớn (một user có 1 triệu event) thì **đừng map** `@OneToMany` — query riêng có phân trang.
- Một chiều + `@JoinColumn`: thứ tự là `INSERT line (order_id null)` → `UPDATE line SET order_id = ?` → cột FK không thể `NOT NULL` (trừ khi `@JoinColumn(nullable = false)` với Hibernate mới hơn xử lý được, nhưng vẫn tốn).

**Câu hỏi nối tiếp:**
- *`@ElementCollection` có vấn đề tương tự không?* → Có: không có identity riêng, thay đổi một phần tử thường xóa và insert lại toàn bộ.

**⚠️ Câu trả lời gây điểm trừ:**
- "Một chiều đơn giản hơn nên tốt hơn."

**📖 Ôn lại:** [5.1 — lỗi thường gặp](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#5-mapping)

</details>

### Q23. 🟡 Phân biệt `CascadeType.REMOVE` và `orphanRemoval = true`. Khi nào cascade là sai?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `CascadeType.REMOVE` xóa con **khi xóa cha**. `orphanRemoval = true` xóa con **khi con bị gỡ khỏi collection** của cha (kể cả cha vẫn còn). Cascade chỉ hợp lý trên quan hệ cha → con **thuộc về** cha (aggregate root → thành phần, ví dụ `Order` → `OrderLine`). Sai khi đặt trên `@ManyToOne` (xóa một `OrderLine` cascade REMOVE lên `Order` = xóa luôn đơn hàng) hoặc `@ManyToMany` (xóa post xóa luôn Tag mà post khác đang dùng).

**Giải thích chi tiết:**
- Với `orphanRemoval`, **không gán collection mới**: `order.setLines(new ArrayList<>())` → `HibernateException: A collection with cascade="all-delete-orphan" was no longer referenced...`. Dùng `lines.clear()` + `addAll`.
- `CascadeType.REMOVE` xóa từng con bằng từng `DELETE` (phải load con lên trước). Hàng nghìn con → bulk delete hoặc `ON DELETE CASCADE` ở DB (`@OnDelete(action = OnDeleteAction.CASCADE)`).

**Câu hỏi nối tiếp:**
- *Cascade PERSIST/MERGE có nên đặt trên ManyToMany không?* → Có thể (tạo Tag mới cùng Post), nhưng không bao giờ REMOVE/ALL.

**⚠️ Câu trả lời gây điểm trừ:**
- Đặt `cascade = ALL` mọi nơi "cho tiện".
- Cho rằng hai khái niệm giống nhau.

**📖 Ôn lại:** [5.2 Cascade và orphanRemoval](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#5-mapping)

</details>

### Q24. 🟡 `Post`–`Tag` map `@ManyToMany` bằng `List`. Gỡ 1 tag khỏi post có 5 tag sinh ra SQL gì? Bạn thiết kế lại thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Với `List` không `@OrderColumn` (bag), Hibernate không xác định được dòng nào trong bảng join cần xóa → `DELETE FROM post_tag WHERE post_id = ?` rồi `INSERT` lại 4 dòng còn lại. Dùng `Set` thì chỉ một `DELETE` chính xác. Thực tế tôi **mặc định dùng entity trung gian** `PostTag` (`@EmbeddedId` + hai `@ManyToOne` với `@MapsId`) vì yêu cầu thêm cột (`created_at`, `created_by`) gần như luôn đến.

**Giải thích chi tiết:**
```java
@Entity class PostTag {
    @EmbeddedId PostTagId id;
    @ManyToOne(fetch = LAZY) @MapsId("postId") Post post;
    @ManyToOne(fetch = LAZY) @MapsId("tagId")  Tag tag;
    Instant createdAt;
}
@Embeddable record PostTagId(Long postId, Long tagId) implements Serializable {}
```
- Cascade trên ManyToMany: chỉ `PERSIST`, `MERGE`.

**Câu hỏi nối tiếp:**
- *Dùng `Set` thì cần chú ý gì?* → `equals/hashCode` của entity phải ổn định (Q26).

**⚠️ Câu trả lời gây điểm trừ:**
- Không biết khái niệm "bag" và hành vi delete-all-reinsert.

**📖 Ôn lại:** [5.3 @ManyToMany và các cạm bẫy](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#5-mapping)

</details>

### Q25. 🟡 So sánh các chiến lược kế thừa JPA. Bạn chọn cái nào cho `Payment` → `CardPayment`, `BankTransferPayment`?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `SINGLE_TABLE` (một bảng + discriminator; query đa hình nhanh nhất, không JOIN, nhưng cột subclass phải nullable), `JOINED` (bảng cha + bảng con, chuẩn hóa, ràng buộc được, nhưng JOIN nhiều bảng), `TABLE_PER_CLASS` (mỗi lớp cụ thể một bảng, query đa hình dùng `UNION ALL`, không dùng được IDENTITY), `@MappedSuperclass` (không phải kế thừa entity, chỉ chia sẻ field). Với `Payment`, tôi thường chọn `SINGLE_TABLE` + `CHECK` constraint theo discriminator — cân bằng tốt nhất giữa hiệu năng và toàn vẹn.

**Giải thích chi tiết:**
```sql
ALTER TABLE payment ADD CONSTRAINT ck_card
  CHECK (type <> 'CARD' OR card_last4 IS NOT NULL);
```
- Đa số "kế thừa" trong dự án chỉ là chia sẻ cột audit → `@MappedSuperclass`.
- Cân nhắc composition over inheritance trước khi map kế thừa; ghi quyết định thành ADR.

**Câu hỏi nối tiếp:**
- *Khi nào `JOINED` hợp lý hơn?* → Subclass có nhiều cột riêng, cần `NOT NULL`/FK riêng, và ít khi query đa hình.

**⚠️ Câu trả lời gây điểm trừ:**
- Chọn theo "đẹp về OOP" mà không nói đến SQL sinh ra.

**📖 Ôn lại:** [5.5 Chiến lược kế thừa](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#5-mapping)

</details>

### Q26. 🔴 Bạn viết `equals/hashCode` cho entity JPA thế nào? Dùng Lombok `@Data` trên entity có vấn đề gì?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Ưu tiên **business key bất biến** (mã đơn hàng, email nếu không đổi). Nếu chỉ có id tự sinh: `equals` so sánh id khi **khác null**, `hashCode` trả **hằng số theo class** (`getClass().hashCode()`) để giá trị không đổi trước/sau persist — nếu hashCode phụ thuộc id, entity bỏ vào `HashSet` khi id null sẽ "mất tích" sau khi persist gán id. `@Data` sinh equals/hashCode/toString trên **mọi field**, kể cả quan hệ → vòng lặp vô hạn `StackOverflowError` giữa hai phía, kích hoạt lazy load (N+1 hoặc `LazyInitializationException` khi log), và hashCode đổi khi field đổi.

**Giải thích chi tiết:**
```java
@Override public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof OrderLine other)) return false;
    return id != null && id.equals(other.getId());   // dùng getter: an toàn với proxy
}
@Override public int hashCode() { return getClass().hashCode(); }
```
- Với Hibernate proxy: so sánh qua getter, dùng `Hibernate.getClass(o)` hoặc `instanceof` thay vì `getClass() ==` (proxy là subclass).
- Hash hằng số làm `HashSet` lớn suy biến thành list — chấp nhận được vì collection entity thường nhỏ; collection lớn thì không nên map.

**Câu hỏi nối tiếp:**
- *Vì sao không dùng mặc định `Object.equals`?* → Hai instance cùng dòng DB từ hai persistence context khác nhau sẽ không bằng nhau → lỗi khi merge vào `Set`.
- *Lombok an toàn dùng gì?* → `@Getter/@Setter`, tự viết equals/hashCode; `@ToString.Exclude` cho quan hệ.

**⚠️ Câu trả lời gây điểm trừ:**
- "Dùng `Objects.hash(id)`" mà không nhận ra id null trước persist.
- "Dùng `@Data` cho nhanh."

**📖 Ôn lại:** [5.6 equals/hashCode cho entity](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#5-mapping)

</details>

---

<a id="nhom-e"></a>
## E. Fetching, N+1, OSIV

### Q27. 🟢 Mặc định fetch type của các quan hệ JPA là gì? Vì sao Senior khuyên "mọi quan hệ đều LAZY"?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `@ManyToOne`, `@OneToOne` mặc định **EAGER**; `@OneToMany`, `@ManyToMany`, `@ElementCollection` mặc định LAZY. Khuyên đổi tất cả sang LAZY vì EAGER là quyết định **toàn cục** không tắt được theo từng query: JPQL vẫn phải load association EAGER bằng query phụ → N+1 ẩn, và mọi màn hình đều trả giá cho dữ liệu chỉ vài màn hình cần. Fetch được quyết định **theo use case** tại query (JOIN FETCH, EntityGraph, DTO).

**Giải thích chi tiết:**
- LAZY dùng **proxy** (subclass sinh bằng ByteBuddy) cho `@ManyToOne`, collection wrapper (`PersistentBag`, `PersistentSet`) cho collection; truy cập lần đầu cần persistence context còn mở.
- Bẫy: `@OneToOne(mappedBy=...)` phía inverse **không lazy được** bằng proxy (Hibernate không biết trả null hay proxy) → luôn bị load. Giải pháp: `@MapsId` chia sẻ PK và chỉ map phía owning, hoặc bytecode enhancement.

**Câu hỏi nối tiếp:**
- *`em.find` với association EAGER sinh SQL gì?* → Thường JOIN; còn JPQL thì query phụ sau đó.

**⚠️ Câu trả lời gây điểm trừ:**
- "EAGER để tránh LazyInitializationException."

**📖 Ôn lại:** [6.1 Mặc định của JPA](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#6-fetching)

</details>

### Q28. 🟢 N+1 query là gì? Làm sao phát hiện nó trước khi lên production?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Một query lấy N bản ghi cha, sau đó mỗi lần truy cập association lazy của từng bản ghi lại sinh một query → 1 + N query. Dev có 5 dòng không ai thấy; production 500 dòng → endpoint chậm vài giây. Phát hiện: bật log SQL (`org.hibernate.SQL=debug`), Hibernate statistics (`generate_statistics=true`), APM đếm span SQL/request, và **integration test assert số câu SQL** (datasource-proxy, `SQLStatementCountValidator`) cho các endpoint quan trọng.

**Giải thích chi tiết:**
```java
List<Order> orders = em.createQuery("SELECT o FROM Order o", Order.class).getResultList(); // 1
for (Order o : orders) o.getCustomer().getName();                                            // +N
```
- N+1 còn ẩn ở tầng view/serialize khi bật OSIV (Jackson chạm vào association).

**Câu hỏi nối tiếp:**
- *Có thể tự động hóa phát hiện không?* → Test đếm query chạy trong CI; một số đội dùng Hypersistence Optimizer hoặc rule APM cảnh báo request > X query.

**⚠️ Câu trả lời gây điểm trừ:**
- Chỉ nói "dùng EAGER để sửa" (EAGER vẫn N+1 với JPQL).

**📖 Ôn lại:** [6.2 N+1 problem](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#6-fetching)

</details>

### Q29. 🟡 Kể các cách sửa N+1 và trade-off của từng cách.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** (a) **JOIN FETCH** — một query, nhưng fetch collection sẽ nhân dòng và không phân trang được; (b) **`@EntityGraph`** — khai báo association cần fetch, dùng được với derived query; (c) **batch fetching** (`default_batch_fetch_size` / `@BatchSize`) — N+1 thành 1 + ceil(N/size), ít xâm lấn, làm lưới an toàn; (d) **DTO projection** — chỉ lấy cột cần, không vào persistence context, tốt nhất cho màn hình đọc.

**Giải thích chi tiết:**
| Cách | Ưu | Nhược |
|---|---|---|
| JOIN FETCH | 1 query | Tích Descartes khi fetch nhiều collection; HHH000104 khi phân trang |
| EntityGraph | Tái sử dụng, khai báo | Vẫn là JOIN — cùng hạn chế |
| Batch fetching | Không sửa query, an toàn với phân trang | Vẫn nhiều hơn 1 query |
| DTO projection | Nhẹ nhất, không dirty check | Không dùng cho use case ghi |

- Chiến lược tổng thể: tất cả LAZY + `default_batch_fetch_size` 16–100 + OSIV tắt + DTO cho đọc + JOIN FETCH/EntityGraph cho use case ghi load aggregate + test đếm SQL.
- Hibernate 6 tự loại trùng root entity khi fetch collection; Hibernate 5 cần `DISTINCT`.

**Câu hỏi nối tiếp:**
- *Khi nào bỏ JPA cho query đọc?* → Report/analytics phức tạp → native SQL, jOOQ, `JdbcTemplate` (CQRS nhẹ).

**⚠️ Câu trả lời gây điểm trừ:**
- Chỉ biết một cách duy nhất là JOIN FETCH.

**📖 Ôn lại:** [6.3 Các cách sửa N+1](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#6-fetching)

</details>

### Q30. 🟡 `LazyInitializationException` xảy ra khi nào? Vì sao Open Session In View (OSIV) là anti-pattern dù nó "sửa" được lỗi này?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Xảy ra khi truy cập association LAZY sau khi persistence context đã đóng — điển hình service trả entity, controller/Jackson serialize chạm vào `order.getLines()`. OSIV (Spring Boot bật **mặc định** `spring.jpa.open-in-view=true`, có in cảnh báo khi khởi động) giữ `EntityManager` mở suốt request nên lazy load chạy được. Nó là anti-pattern vì: (1) **giữ connection lâu** — mỗi lazy load ở tầng view mượn connection trong toàn bộ thời gian request → pool cạn dưới tải; (2) **N+1 ẩn** ở tầng serialize; (3) lazy load chạy **ngoài transaction** ở chế độ auto-commit → mỗi lần đọc một snapshot khác nhau, không nhất quán; (4) mờ ranh giới tầng.

**Giải thích chi tiết:**
- Cách đúng: `spring.jpa.open-in-view=false`; service trả **DTO** đã đủ dữ liệu, fetch theo use case.
- **Không** dùng `hibernate.enable_lazy_load_no_trans=true` — còn tệ hơn: mỗi lazy load mở session + connection mới.
- Lộ trình tắt OSIV ở dự án đang chạy: bật log cảnh báo, viết test cho các endpoint, chuyển từng controller sang DTO, đo `hikaricp.connections.usage` giảm.

**Câu hỏi nối tiếp:**
- *Trong Struts legacy, `OpenSessionInViewFilter` gây vấn đề gì?* → Giống hệt: JSP truy cập lazy collection, N+1 trong JSP (Module 10).

**⚠️ Câu trả lời gây điểm trừ:**
- "Bật OSIV hoặc đổi sang EAGER là xong."
- Không biết Spring Boot bật OSIV mặc định.

**📖 Ôn lại:** [6.4 LazyInitializationException và OSIV](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#6-fetching)

</details>

### Q31. 🔴 Query `SELECT p FROM Post p LEFT JOIN FETCH p.comments LEFT JOIN FETCH p.tags` ném `MultipleBagFetchException`. Đồng nghiệp đổi `List` thành `Set` và hết lỗi. Bạn có đồng ý không?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Không. "Bag" là `List` không `@OrderColumn`; fetch hai bag cùng lúc tạo **tích Descartes** (post 50 comment × 20 tag = 1.000 dòng) và Hibernate không thể khử trùng chính xác nên từ chối. Đổi sang `Set` chỉ làm hết exception, **tích Descartes vẫn còn** — dữ liệu truyền về bùng nổ, chậm và tốn bộ nhớ. Cách đúng: **tách nhiều query trong cùng transaction** — query 1 fetch `comments`, query 2 fetch `tags` cho cùng danh sách post; persistence context ghép kết quả.

**Giải thích chi tiết:**
```java
List<Post> posts = em.createQuery(
    "SELECT DISTINCT p FROM Post p LEFT JOIN FETCH p.comments WHERE p.id IN :ids", Post.class)
    .setParameter("ids", ids).getResultList();
em.createQuery(
    "SELECT DISTINCT p FROM Post p LEFT JOIN FETCH p.tags WHERE p IN :posts", Post.class)
    .setParameter("posts", posts).getResultList();
// posts giờ đã có cả comments và tags
```
- Hoặc batch fetching cho một trong hai collection.

**Câu hỏi nối tiếp:**
- *Kết hợp phân trang thì sao?* → Query 0 lấy trang id, rồi hai query fetch theo id, sắp xếp lại theo thứ tự id (tối đa 4 SQL: count + ids + comments + tags).

**⚠️ Câu trả lời gây điểm trừ:**
- Đồng ý đổi `Set` mà không nói đến tích Descartes.

**📖 Ôn lại:** [6.5 MultipleBagFetchException](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#6-fetching)

</details>

### Q32. 🔴 Log production xuất hiện `HHH000104: firstResult/maxResults specified with collection fetch; applying in memory!` (Hibernate 6: `HHH90003004`). Nghĩa là gì, nguy hiểm thế nào, sửa ra sao?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Query có `JOIN FETCH` collection kèm phân trang. Áp `LIMIT` lên dòng đã join sẽ cắt ngang collection, nên Hibernate **bỏ LIMIT ở SQL, tải toàn bộ kết quả** rồi cắt trang trong bộ nhớ. Bảng nhỏ thì chỉ chậm; bảng lớn thì OOM. Sửa: **hai bước** — (1) phân trang trên id của root, (2) fetch theo `id IN (:ids)` kèm JOIN FETCH, giữ lại thứ tự sort; hoặc trang chỉ chứa root + batch fetching. Bật `hibernate.query.fail_on_pagination_over_collection_fetch=true` để biến cảnh báo thành exception ngay từ dev.

**Giải thích chi tiết:**
```java
@Query(value = "SELECT o.id FROM Order o WHERE o.status = :s",
       countQuery = "SELECT count(o) FROM Order o WHERE o.status = :s")
Page<Long> findIds(@Param("s") Status s, Pageable p);

@Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.lines WHERE o.id IN :ids")
List<Order> findWithLinesByIds(@Param("ids") List<Long> ids);
// sắp lại: Map<Long, Order> byId; ids.stream().map(byId::get).toList()
```

**Câu hỏi nối tiếp:**
- *Vì sao trước đây không ai thấy?* → Dev/UAT ít dữ liệu; cảnh báo chỉ là log WARN — nên có rule CI/log alert cho mã cảnh báo này.

**⚠️ Câu trả lời gây điểm trừ:**
- "Chỉ là warning, bỏ qua được."

**📖 Ôn lại:** [6.6 Phân trang với fetch join](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#6-fetching)

</details>

---

<a id="nhom-f"></a>
## F. Second-level cache & lựa chọn kiểu query

### Q33. 🟡 First-level cache và second-level cache khác nhau thế nào? Khi nào bạn bật L2C?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** First-level cache là persistence context — phạm vi một `EntityManager`/transaction, luôn bật. Second-level cache (L2C) phạm vi `SessionFactory` (toàn JVM hoặc phân tán), tắt mặc định, lưu entity dạng **dehydrated** (mảng giá trị). Tôi chỉ bật L2C cho **dữ liệu tham chiếu đọc nhiều, ít đổi** (quốc gia, tiền tệ, danh mục) với `READ_ONLY`/`READ_WRITE`, không bật cho dữ liệu giao dịch.

**Giải thích chi tiết:**
- Concurrency strategies: `READ_ONLY` (nhanh nhất, update → exception), `NONSTRICT_READ_WRITE` (có cửa sổ stale), `READ_WRITE` (soft-lock), `TRANSACTIONAL` (JTA).
- Cạm bẫy: L2C chỉ thấy thay đổi đi **qua Hibernate** — SQL trực tiếp/ứng dụng khác update → cache stale; nhiều instance với cache local → mỗi node một bản (cần cache phân tán/invalidation hoặc TTL ngắn); bulk JPQL invalidate cả region.
- Thường cache ở tầng service (Spring Cache + Redis cache DTO) dễ kiểm soát hơn L2C.

**Câu hỏi nối tiếp:**
- *DBA update giá bằng `psql`, app vẫn đọc giá cũ — xử lý?* → Evict qua API admin (`emf.getCache().evict(Product.class, id)`), TTL ở provider, hoặc không cache entity đó.

**⚠️ Câu trả lời gây điểm trừ:**
- "Bật L2C cho mọi entity để giảm tải DB."

**📖 Ôn lại:** [7. Second-level cache & query cache](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#7-cache)

</details>

### Q34. 🔴 Vì sao query cache của Hibernate thường không mang lại lợi ích, thậm chí có hại?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Query cache chỉ lưu **danh sách id** kết quả theo (câu query + tham số); entity vẫn phải lấy từ L2C (nếu entity không ở L2C → N lần load từng entity = N+1). Và khi **bất kỳ** dòng nào của bảng liên quan thay đổi, mọi query cache trên bảng đó bị invalidate (theo timestamps region). Với bảng ghi thường xuyên, hit-rate gần 0 nhưng vẫn tốn chi phí lưu và invalidate, thêm contention trên timestamps region.

**Giải thích chi tiết:**
- Chỉ hợp: query trên bảng gần như bất biến, tham số ít biến thể, chạy rất thường xuyên.
- Thay thế tốt hơn: cache kết quả DTO ở tầng service với key và TTL rõ ràng, invalidate theo sự kiện nghiệp vụ.

**Câu hỏi nối tiếp:**
- *Bật query cache cần gì?* → `hibernate.cache.use_query_cache=true` + hint `org.hibernate.cacheable=true` trên từng query.

**⚠️ Câu trả lời gây điểm trừ:**
- Tưởng query cache lưu nguyên object kết quả.

**📖 Ôn lại:** [7.3 Query cache](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#7-cache)

</details>

### Q35. 🟡 Khi nào bạn dùng JPQL, Criteria API, native SQL? Có rủi ro gì khi test native query bằng H2?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** JPQL cho query tĩnh hướng entity (`@Query` được validate lúc bootstrap); Criteria API (+ static metamodel `Order_`) cho **filter động** type-safe, đổi tên field báo lỗi compile; native SQL cho report/analytics phức tạp và tính năng riêng của DB (`FOR UPDATE SKIP LOCKED`, `jsonb`, `LATERAL`, recursive CTE, hint). Test native query PostgreSQL bằng H2 là sai: cú pháp và hành vi khác → test xanh giả; phải dùng **Testcontainers** với đúng DB và phiên bản.

**Giải thích chi tiết:**
- Hibernate 6 HQL đã hỗ trợ CTE, window function, set operation, `LIMIT/OFFSET` — nhưng vẫn giới hạn so với SQL thật.
- Native query: gắn vendor, không tự invalidate L2C chính xác, lỗi chỉ thấy runtime.
- Kiến trúc CQRS nhẹ: command qua JPA (aggregate, dirty checking, optimistic lock), query đọc qua SQL tối ưu trả DTO.

**Câu hỏi nối tiếp:**
- *Spring Data có gì cho filter động ngoài Criteria?* → `Specification`, Querydsl, Query by Example.

**⚠️ Câu trả lời gây điểm trừ:**
- "Luôn dùng JPQL để độc lập DB" kể cả cho report phức tạp.
- "Dùng H2 cho nhanh."

**📖 Ôn lại:** [8. JPQL, Criteria API, native SQL](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#8-query)

</details>

---

<a id="nhom-g"></a>
## G. Spring Data JPA

### Q36. 🟢 Spring Data JPA tạo implementation cho interface repository thế nào? Gọi `repo.save()` ngoài service transaction thì có transaction không?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Spring tạo **JDK dynamic proxy** cho interface, mặc định ủy quyền tới `SimpleJpaRepository`; derived query (`findByCustomerIdAndStatus...`) được parse tên method thành JPQL lúc khởi động. `SimpleJpaRepository` có sẵn `@Transactional(readOnly = true)` ở mức class và `@Transactional` cho các method ghi (`save`, `delete`...) → gọi `save()` ngoài service transaction vẫn chạy trong **transaction riêng của nó**, commit ngay sau method.

**Giải thích chi tiết:**
- Hệ quả: gọi `repo.save(a); repo.save(b);` không có `@Transactional` ở service = hai transaction độc lập — `b` lỗi thì `a` vẫn đã commit.
- Method derived quá 3 điều kiện → chuyển sang `@Query` cho dễ đọc.
- `findAll()` không phân trang trên bảng lớn là nguy cơ OOM.

**Câu hỏi nối tiếp:**
- *Custom method trong repository thì sao?* → Tạo interface `XxxRepositoryCustom` + class `XxxRepositoryImpl`; tự đặt `@Transactional` nếu cần.

**⚠️ Câu trả lời gây điểm trừ:**
- "Repository không có transaction nên save không lưu."

**📖 Ôn lại:** [9.1 Repository và query derivation](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#9-spring-data)

</details>

### Q37. 🟡 Closed projection và open projection khác nhau thế nào về SQL sinh ra?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **Closed projection** là interface chỉ có getter khớp thuộc tính entity → Spring Data sinh `SELECT o.id, o.code` chỉ đúng cột cần. **Open projection** dùng `@Value("#{target.customer.name}")` (SpEL) → Spring buộc **load entity đầy đủ** rồi tính biểu thức → mất lợi ích tối ưu, có thể kéo theo lazy load. Ngoài ra có class/record DTO projection và dynamic projection (`<T> List<T> findByStatus(Status s, Class<T> type)`).

**Giải thích chi tiết:**
- Record DTO với `@Query("SELECT new com.shop.OrderSummary(o.id, c.name, o.total) FROM Order o JOIN o.customer c ...")` là cách rõ ràng nhất cho màn hình đọc.
- Nested closed projection (`CustomerView getCustomer()`) với Hibernate có thể vẫn load cả entity lồng — kiểm tra SQL thực tế.

**Câu hỏi nối tiếp:**
- *Kiểm chứng thế nào?* → Test bật log SQL hoặc đếm cột trong câu SELECT.

**⚠️ Câu trả lời gây điểm trừ:**
- Không biết projection có thể vẫn load entity đầy đủ.

**📖 Ôn lại:** [9.2 Projections](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#9-spring-data)

</details>

### Q38. 🟡 `Page`, `Slice` và keyset pagination — chọn cái nào cho API danh sách đơn hàng 10 triệu dòng?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `Page<T>` chạy thêm câu **count** — trên bảng lớn với filter phức tạp, count có thể đắt hơn cả query dữ liệu. `Slice<T>` lấy `size + 1` dòng để biết còn trang sau, không count — hợp infinite scroll. Nhưng cả hai vẫn dùng **OFFSET**, trang sâu O(offset). Với 10 triệu dòng, tôi chọn **keyset/seek pagination**: `WHERE (created_at, id) < (:c, :id) ORDER BY created_at DESC, id DESC LIMIT 20` với index `(created_at DESC, id DESC)` — ổn định O(log n), không trùng/sót khi dữ liệu thay đổi. Spring Data 3.1+ có `ScrollPosition`/`Window<T>`.

**Giải thích chi tiết:**
- `id` làm tie-breaker để thứ tự duy nhất.
- API trả cursor opaque (Base64 của `{createdAt, id}`).
- Nhược: không nhảy tới trang bất kỳ, khó hiển thị tổng số trang (dùng số ước lượng từ statistics).

**Câu hỏi nối tiếp:**
- *Admin cần nhảy trang thì sao?* → OFFSET với deferred join (Module 11) hoặc giới hạn số trang tối đa.

**⚠️ Câu trả lời gây điểm trừ:**
- "Dùng `Page` mặc định là được" mà không nghĩ tới count và OFFSET.

**📖 Ôn lại:** [9.4 Page vs Slice vs keyset](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#9-spring-data)

</details>

### Q39. 🟡 `repo.deleteByStatus(Status.EXPIRED)` trên bảng 2 triệu dòng chạy rất lâu. Vì sao?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Derived delete (và `deleteAll()`) **load toàn bộ entity khớp** vào persistence context rồi `em.remove` từng cái → 2 triệu entity trong bộ nhớ + 2 triệu câu `DELETE` (+ cascade, callback). Dùng bulk: `@Modifying @Query("DELETE FROM Order o WHERE o.status = :s")` hoặc `deleteAllInBatch()`, và **chia lô** (theo id range) để tránh transaction khổng lồ, lock lâu, replication lag.

**Giải thích chi tiết:**
- Bulk delete bỏ qua cascade JPA → con phải xóa trước hoặc DB có `ON DELETE CASCADE`.
- Dữ liệu theo thời gian nên **partition** và `DROP/DETACH PARTITION` thay vì `DELETE` hàng triệu dòng (Module 11).
- Auditing JPA (`@LastModifiedDate`) dựa trên callback → bulk update không cập nhật.

**Câu hỏi nối tiếp:**
- *Vì sao Spring Data làm vậy?* → Để tôn trọng cascade/callback/lifecycle của JPA — đúng nhưng đắt.

**⚠️ Câu trả lời gây điểm trừ:**
- "Do thiếu index" — có thể góp phần nhưng không phải nguyên nhân chính.

**📖 Ôn lại:** [9.1 — cảnh báo deleteAll/deleteBy](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#9-spring-data)

</details>

### Q40. 🟡 Dùng `Specification` có `root.fetch("customer")` với `findAll(spec, pageable)` thì lỗi. Vì sao và sửa sao?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Với `Pageable`, Spring Data dùng **cùng Specification** để dựng câu **count**. Count query trả `Long`, không có entity root để fetch → Hibernate báo lỗi kiểu "query specified join fetching, but the owner of the fetched association was not present in the select list". Sửa: kiểm tra `query.getResultType()` — nếu là `Long.class`/`long.class` (count) thì dùng `join` thường hoặc bỏ qua, ngược lại mới `fetch`.

**Giải thích chi tiết:**
```java
static Specification<Order> withCustomer() {
    return (root, q, cb) -> {
        if (Long.class != q.getResultType() && long.class != q.getResultType()) {
            root.fetch("customer", JoinType.LEFT);
        }
        return null;   // null = không thêm điều kiện
    };
}
```
- Specification trả `null` = bỏ qua điều kiện — tiện cho filter tùy chọn.
- Nếu không cần tổng số, dùng `Slice` (Spring Data 3 fluent API `findBy(spec, q -> q.as(Dto.class).page(...))`).

**Câu hỏi nối tiếp:**
- *JOIN trong spec với quan hệ 1-N có làm sai count không?* → Có thể nhân dòng; dùng `query.distinct(true)` hoặc subquery `EXISTS`.

**⚠️ Câu trả lời gây điểm trừ:**
- Không biết Spring Data tái dùng spec cho count query.

**📖 Ôn lại:** [9.3 Specifications](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#9-spring-data)

</details>

---

<a id="nhom-h"></a>
## H. Locking & concurrency control

### Q41. 🟢 Lost update là gì? `@Version` ngăn nó thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Hai transaction cùng đọc stock = 10, A ghi 7, B (vẫn thấy 10) ghi 8 → thay đổi của A bị mất. `READ COMMITTED` không ngăn được kiểu read-modify-write này. Với `@Version`, mọi `UPDATE` Hibernate sinh ra có thêm điều kiện: `UPDATE product SET stock = ?, version = 6 WHERE id = ? AND version = 5`. Nếu 0 dòng bị ảnh hưởng → `OptimisticLockException` (JPA) / `StaleObjectStateException` (Hibernate) → Spring dịch thành `ObjectOptimisticLockingFailureException`.

**Giải thích chi tiết:**
- Dùng cho cả "long conversation" qua nhiều request HTTP: gửi `version` cho client (ETag/hidden field), khi submit so sánh → HTTP 409/412.
- Kiểu `@Version`: `int/long/short/Instant`; ưu tiên số nguyên.
- `OPTIMISTIC_FORCE_INCREMENT`: tăng version của aggregate root khi chỉ con thay đổi.

**Câu hỏi nối tiếp:**
- *Bulk JPQL update có tăng version không?* → Không, phải tự `SET version = version + 1`.

**⚠️ Câu trả lời gây điểm trừ:**
- "Có `@Transactional` rồi thì không bị lost update."

**📖 Ôn lại:** [10.1–10.2 Optimistic locking](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#10-locking)

</details>

### Q42. 🟡 Kịch bản flash sale: 500 request đồng thời mua cùng sản phẩm tồn kho 100. Bạn chọn optimistic, pessimistic hay cách khác? Vì sao?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Với một **hot row** contention cao, optimistic sẽ gây **retry storm** (hầu hết request thất bại và retry). Lựa chọn đầu tiên của tôi là **atomic update**: `UPDATE product SET stock = stock - :q WHERE id = :id AND stock >= :q` và kiểm tra số dòng ảnh hưởng — một round-trip, lock ngắn nhất, không bao giờ âm. Nếu cần đọc–tính toán phức tạp trước khi ghi → `PESSIMISTIC_WRITE` (`SELECT ... FOR UPDATE`) có lock timeout. Optimistic hợp với xung đột hiếm và chu trình đọc–sửa dài qua UI.

**Giải thích chi tiết:**
| Cách | Khi nào | Giá phải trả |
|---|---|---|
| Atomic update | Counter, tồn kho, số dư đơn giản | Logic phải biểu diễn được bằng SQL |
| Pessimistic | Contention cao, retry đắt | Chờ lock, nguy cơ deadlock, giữ connection — luôn đặt timeout, khóa theo thứ tự |
| Optimistic | Contention thấp, conversation dài | Xử lý exception/retry |
| Sharded counter / queue / Redis + đối soát | Hàng nghìn TPS trên một dòng | Phức tạp, eventual consistency |

- Đơn nhiều sản phẩm: khóa/cập nhật theo **thứ tự id tăng dần** để tránh deadlock.

**Câu hỏi nối tiếp:**
- *Kiểm chứng?* → Load test 500 thread: đúng 100 đơn thành công, stock = 0; so sánh throughput, p99, số exception giữa ba cách.

**⚠️ Câu trả lời gây điểm trừ:**
- "Dùng `synchronized` trong Java" — vô dụng khi chạy nhiều instance.
- "Optimistic luôn tốt hơn vì không lock."

**📖 Ôn lại:** [10.3 — Góc nhìn Senior: chọn gì?](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#10-locking)

</details>

### Q43. 🔴 Đoạn code sau retry khi optimistic lock fail nhưng retry không bao giờ thành công. Vì sao?

```java
@Service
public class StockService {
    @Transactional
    @Retryable(retryFor = ObjectOptimisticLockingFailureException.class, maxAttempts = 3)
    public void decrease(Long id, int qty) {
        Product p = repo.findById(id).orElseThrow();
        p.decrease(qty);
    }
}
```

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Retry phải bọc **ngoài** transaction để mỗi lần thử là một transaction mới với persistence context mới (đọc version mới nhất). Khi đặt cả hai annotation trên cùng method, thứ tự advice phụ thuộc cấu hình `order`; nếu transaction interceptor nằm ngoài retry interceptor, các lần retry chạy **trong cùng transaction** đã bị đánh dấu rollback-only và persistence context vẫn giữ entity version cũ → lần nào cũng fail. Thêm nữa, exception optimistic lock thường chỉ ném ra **lúc flush/commit** — tức là ở tầng proxy transaction, ngoài phạm vi retry bên trong.

**Giải thích chi tiết:**
```java
@Service
class StockFacade {                                   // bean ngoài: chỉ retry
    @Retryable(retryFor = ObjectOptimisticLockingFailureException.class, maxAttempts = 3,
               backoff = @Backoff(delay = 50, multiplier = 2, random = true))
    public void decrease(Long id, int qty) { stockTx.decrease(id, qty); }
}
@Service
class StockTx {                                        // bean trong: transaction
    @Transactional
    public void decrease(Long id, int qty) { ... }
}
```
- Hoặc retry ngoài + `TransactionTemplate` bên trong.
- Backoff có jitter (`random = true`) để tránh các request retry đồng loạt.
- Chỉ retry thao tác **không có tương tác người dùng**; xung đột từ form UI thì trả 409 cho người dùng tự quyết.

**Câu hỏi nối tiếp:**
- *Retry cho deadlock/serialization failure (`40001`, `40P01`)?* → Cùng nguyên tắc: bọc ngoài transaction, retry toàn bộ.

**⚠️ Câu trả lời gây điểm trừ:**
- "Tăng `maxAttempts`."
- Không biết exception ném ra lúc commit.

**📖 Ôn lại:** [10.2 — cảnh báo @Retryable](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#10-locking)

</details>

### Q44. 🔴 Thiết kế job queue trên database để 5 worker lấy job song song mà không xử lý trùng. Có bẫy gì khi dùng `SELECT ... FOR UPDATE`?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Dùng `SELECT ... FROM job WHERE status = 'PENDING' ORDER BY created_at LIMIT 10 FOR UPDATE SKIP LOCKED` — mỗi worker khóa một nhóm job khác nhau, không chờ nhau (PostgreSQL 9.5+, MySQL 8.0+, Oracle). Trong JPA: `@Lock(PESSIMISTIC_WRITE)` + hint `jakarta.persistence.lock.timeout = -2` (Hibernate hiểu là SKIP LOCKED) hoặc native query. Bẫy lớn: `FOR UPDATE` **ngoài transaction** (auto-commit) → lock nhả ngay sau câu lệnh → vô tác dụng; Spring Data ném `TransactionRequiredException` cho `@Lock` ngoài transaction, còn JDBC thuần thì im lặng.

**Giải thích chi tiết:**
Hai mô hình:
1. **Giữ transaction suốt lúc xử lý:** đơn giản, worker chết → rollback tự nhả lock; nhưng giữ connection lâu (ảnh hưởng pool) và giữ snapshot.
2. **Lease:** trong transaction ngắn, `UPDATE ... SET status='PROCESSING', locked_until = now() + 5 min` rồi commit; worker khác nhặt lại job quá hạn → handler phải **idempotent**.
- Đặt lock timeout cho mọi pessimistic lock; index trên `(status, created_at)`.

**Câu hỏi nối tiếp:**
- *Dùng cho outbox relay thế nào?* → Scheduler đọc `WHERE published_at IS NULL ... FOR UPDATE SKIP LOCKED`, publish, set `published_at`; crash giữa publish và update → gửi lại (at-least-once), consumer dedupe.

**⚠️ Câu trả lời gây điểm trừ:**
- Dùng `synchronized`/lock JVM.
- Không nhắc idempotency khi job có thể chạy lại.

**📖 Ôn lại:** [10.3 Pessimistic locking và SKIP LOCKED](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#10-locking)

</details>

### Q45. 🟡 Isolation level mặc định của PostgreSQL, MySQL, Oracle là gì? Vì sao cùng một code Spring chạy khác nhau trên hai DB?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** PostgreSQL và Oracle mặc định **READ COMMITTED**, MySQL InnoDB mặc định **REPEATABLE READ**. `@Transactional` mặc định `Isolation.DEFAULT` = dùng mặc định của DB → cùng code nhưng hành vi đọc khác nhau (non-repeatable read có/không), khả năng phát hiện lost update khác nhau (PostgreSQL RR báo lỗi serialization, MySQL RR thì không), và lượng gap lock/deadlock khác nhau.

**Giải thích chi tiết:**
| Isolation | Ghi chú thực tế |
|---|---|
| READ UNCOMMITTED | PostgreSQL thực thi như READ COMMITTED |
| READ COMMITTED | Mặc định PG, Oracle, SQL Server |
| REPEATABLE READ | Mặc định MySQL; PG RR = snapshot isolation, không phantom nhưng còn write skew |
| SERIALIZABLE | PG dùng SSI → lỗi `40001`, app phải retry; Oracle SERIALIZABLE thực chất là snapshot isolation |

- Oracle chỉ có READ COMMITTED và SERIALIZABLE.
- Chi tiết MVCC, gap lock, write skew: Module 11.

**Câu hỏi nối tiếp:**
- *Đặt isolation trong Spring?* → `@Transactional(isolation = REPEATABLE_READ)` — chỉ hiệu lực khi **bắt đầu** transaction mới (xem Q48).

**⚠️ Câu trả lời gây điểm trừ:**
- Nhớ nhầm MySQL mặc định READ COMMITTED.

**📖 Ôn lại:** [10.4 Isolation levels và anomaly](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#10-locking)

</details>

---

<a id="nhom-i"></a>
## I. Spring Transactions chuyên sâu

### Q46. 🟢 `@Transactional` hoạt động bên dưới như thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Spring bọc bean bằng **AOP proxy** (Spring Boot mặc định CGLIB — subclass, kể cả khi bean implement interface). Lời gọi đi qua proxy → `TransactionInterceptor` hỏi `PlatformTransactionManager` (`JpaTransactionManager`, `DataSourceTransactionManager`, `JtaTransactionManager`) để bắt đầu hoặc tham gia transaction (mượn connection, `setAutoCommit(false)`), gọi method thật, rồi commit hoặc rollback theo rollback rule. Connection/`EntityManager` của transaction hiện tại được **gắn vào thread** qua `TransactionSynchronizationManager` (các `ThreadLocal`).

**Giải thích chi tiết:**
```
Caller ─► Proxy ─► TransactionInterceptor
                     1. getTransaction(definition)   → mượn connection, autoCommit=false
                     2. invoke target method
                     3a. exception khớp rule → rollback
                     3b. ngược lại → commit
```
- Vì gắn theo thread: code chạy trong thread khác (`@Async`, `CompletableFuture.supplyAsync`, parallel stream) **không** thuộc transaction đó.
- Reactive (WebFlux/R2DBC) dùng `ReactiveTransactionManager`, context gắn vào Reactor `Context` thay vì ThreadLocal.

**Câu hỏi nối tiếp:**
- *Đặt `@Transactional` ở tầng nào?* → Service (use case); không ở controller, không rải tùy tiện trên repository.
- *JDK proxy vs CGLIB?* → JDK proxy chỉ proxy interface; CGLIB tạo subclass nên không proxy được method `final`/`private`.

**⚠️ Câu trả lời gây điểm trừ:**
- "Spring sửa bytecode của class" (đó là AspectJ weaving, không phải mặc định).
- Không nhắc ThreadLocal → không giải thích được vì sao `@Async` mất transaction.

**📖 Ôn lại:** [11.1 Kiến trúc: PlatformTransactionManager và proxy](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#11-transactions)

</details>

### Q47. 🟢 Đoạn code sau mong mỗi báo cáo chạy trong một transaction riêng, nhưng thực tế không có transaction nào. Vì sao? Kể thêm các trường hợp `@Transactional` bị bỏ qua âm thầm.

```java
@Service
public class ReportService {
    public void generateAll(List<Long> ids) {
        for (long id : ids) generateOne(id);
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void generateOne(long id) { ... }
}
```

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **Self-invocation**: `generateOne` được gọi qua `this`, không đi qua proxy → annotation không có tác dụng. Năm trường hợp `@Transactional` bị bỏ qua âm thầm: (1) gọi nội bộ trong cùng class; (2) method `private` (với CGLIB còn `final`/`static`; trước Spring 6 chỉ method `public` được áp dụng, Spring 6 hỗ trợ thêm `protected`/package-private với class-based proxy); (3) bean không do Spring quản lý (`new ReportService()`); (4) gọi trong constructor/`@PostConstruct`; (5) exception bị catch và nuốt bên trong method → không có gì để rollback.

**Giải thích chi tiết:**
Cách sửa self-invocation, theo thứ tự ưu tiên:
1. Tách `generateOne` sang bean khác (sạch nhất).
2. `TransactionTemplate` trong vòng lặp (Q53).
3. Self-injection `@Lazy @Autowired ReportService self;` rồi `self.generateOne(id)`.
4. AspectJ weaving (`@EnableTransactionManagement(mode = AdviceMode.ASPECTJ)`).
- Debug: `logging.level.org.springframework.transaction.interceptor=TRACE` → thấy "Getting transaction for [...]"; với self-invocation sẽ không có dòng này cho method bên trong. Hoặc assert `TransactionSynchronizationManager.isActualTransactionActive()` trong test.

**Câu hỏi nối tiếp:**
- *Có cùng vấn đề với `@Async`, `@Cacheable`, `@Retryable` không?* → Có, tất cả đều dựa trên proxy.

**⚠️ Câu trả lời gây điểm trừ:**
- "Do `REQUIRES_NEW` sai cấu hình."
- Đề xuất đặt `@Transactional` lên `generateAll` (sẽ thành một transaction lớn — khác mục tiêu).

**📖 Ôn lại:** [11.2 Self-invocation, private/final method](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#11-transactions)

</details>

### Q48. 🟡 Giải thích các propagation `REQUIRED`, `REQUIRES_NEW`, `NESTED`, `MANDATORY`, `NOT_SUPPORTED`, `NEVER`, `SUPPORTS` kèm kịch bản thực tế.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `REQUIRED` (mặc định) tham gia transaction đang có hoặc tạo mới. `REQUIRES_NEW` tạm treo transaction ngoài và mở transaction độc lập (connection mới) — dùng cho audit/log lỗi phải còn dù nghiệp vụ rollback. `NESTED` tạo **savepoint** trong transaction ngoài — rollback inner chỉ về savepoint (import batch, dòng lỗi rollback riêng). `MANDATORY` bắt buộc có transaction ngoài, không có thì ném `IllegalTransactionStateException`. `SUPPORTS` có thì tham gia, không thì chạy không transaction. `NOT_SUPPORTED` tạm treo transaction, chạy không transaction (tác vụ dài như sinh file). `NEVER` có transaction thì ném exception — bảo vệ method gọi API ngoài không bao giờ bị bọc trong transaction.

**Giải thích chi tiết:**
- **Isolation, timeout, readOnly** chỉ có hiệu lực khi **bắt đầu** transaction mới. Method `REQUIRED` tham gia transaction có sẵn thì các thuộc tính này bị bỏ qua (đặt `validateExistingTransaction=true` trên transaction manager để Spring báo lỗi khi isolation/readOnly không khớp).
- `REQUIRES_NEW`: inner commit trước outer; inner **không thấy** dữ liệu chưa commit của outer; nguy cơ pool deadlock (Q8) và tự deadlock nếu cùng khóa một dòng.
- `NESTED` cần JDBC savepoint (`DataSourceTransactionManager`); với JPA là vùng nguy hiểm (Q56).

**Câu hỏi nối tiếp:**
- *Kiểm tra propagation trong test?* → `TransactionSynchronizationManager.isActualTransactionActive()`, `getCurrentTransactionName()`.

**⚠️ Câu trả lời gây điểm trừ:**
- Nói `NESTED` và `REQUIRES_NEW` giống nhau.
- Không biết isolation bị bỏ qua khi tham gia transaction có sẵn.

**📖 Ôn lại:** [11.3 Propagation](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#11-transactions) · [11.4 Isolation trong @Transactional](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#11-transactions)

</details>

### Q49. 🔴 Code sau ném `UnexpectedRollbackException: Transaction silently rolled back because it has been marked as rollback-only`. Giải thích và đưa ra cách sửa.

```java
@Service class OrderService {
    @Transactional
    public void placeOrder(Order o) {
        orderRepo.save(o);
        try {
            loyaltyService.addPoints(o);      // @Transactional (REQUIRED), ném RuntimeException
        } catch (RuntimeException e) {
            log.warn("Loyalty failed, ignore");
        }
    }
}
```

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `addPoints` dùng `REQUIRED` nên **tham gia cùng transaction vật lý**. Khi exception đi qua proxy của `addPoints`, Spring không thể rollback riêng phần đó nên **đánh dấu rollback-only** cho toàn bộ transaction. `placeOrder` bắt exception và kết thúc bình thường → proxy ngoài cố commit, thấy cờ rollback-only → rollback và ném `UnexpectedRollbackException` (đơn hàng cũng mất). Sửa tùy nghiệp vụ: (1) nếu điểm thưởng độc lập với đơn → `addPoints` dùng `REQUIRES_NEW`; (2) bỏ `@Transactional` ở `addPoints` và để nó không ném exception làm hỏng transaction; (3) tốt nhất: cộng điểm **sau commit** bằng event (`@TransactionalEventListener`) hoặc outbox, retry riêng.

**Giải thích chi tiết:**
- Spring làm vậy để tránh trạng thái "nửa vời": inner đã có thể ghi một phần trước khi lỗi.
- Với JPA, exception từ Hibernate (ví dụ constraint violation khi flush) còn khiến persistence context không dùng được nữa → càng không thể "nuốt" lỗi rồi đi tiếp.
- `REQUIRES_NEW` kéo theo connection thứ hai → tính lại pool (Q8).
- `globalRollbackOnParticipationFailure=false` trên transaction manager có thể đổi hành vi, nhưng hiếm khi là lựa chọn đúng.

**Câu hỏi nối tiếp:**
- *Nếu `addPoints` không có `@Transactional` mà vẫn ném exception, outer catch thì sao?* → Không có proxy inner nên không đánh dấu rollback-only → outer commit bình thường (nhưng những gì `addPoints` đã ghi trước khi lỗi vẫn được commit — cẩn thận).

**⚠️ Câu trả lời gây điểm trừ:**
- "Bỏ try-catch đi" mà không phân tích nghiệp vụ.
- Không biết khái niệm rollback-only.

**📖 Ôn lại:** [11.3 — Kịch bản 1 REQUIRED và UnexpectedRollbackException](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#11-transactions)

</details>

### Q50. 🟢 Method sau ném `IOException` ở dòng thứ hai. Header có được lưu không?

```java
@Transactional
public void importFile(Path p) throws IOException {
    repo.save(header);
    List<String> lines = Files.readAllLines(p);   // IOException
    ...
}
```

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **Có — transaction vẫn COMMIT.** Mặc định Spring chỉ rollback với `RuntimeException` và `Error`; **checked exception → commit** (kế thừa quy ước EJB: checked exception là "kết quả nghiệp vụ dự kiến"). Sửa: `@Transactional(rollbackFor = Exception.class)`, hoặc quy ước đội: exception nghiệp vụ là unchecked.

**Giải thích chi tiết:**
```java
@Transactional(rollbackFor = Exception.class)          // rollback cả checked
@Transactional(noRollbackFor = BusinessWarning.class)  // ngoại lệ không rollback
```
- Spring Framework 6.2 thêm tùy chọn toàn cục `@EnableTransactionManagement(rollbackOn = RollbackOn.ALL_EXCEPTIONS)` — kiểm tra phiên bản dự án trước khi dùng.
- Đọc file nên làm **trước** khi mở transaction (I/O không cần nằm trong transaction).

**Câu hỏi nối tiếp:**
- *Kotlin thì sao?* → Kotlin không có checked exception ở mức ngôn ngữ nhưng exception Java checked ném ra vẫn theo rule này → nhiều dự án Kotlin đặt `rollbackFor = Exception.class` mặc định.

**⚠️ Câu trả lời gây điểm trừ:**
- "Có exception thì rollback."

**📖 Ôn lại:** [11.5 Rollback rules](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#11-transactions)

</details>

### Q51. 🟡 `@Transactional(readOnly = true)` thực sự làm gì? Có dùng để định tuyến sang read replica được không?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Với Hibernate: session được đặt read-only (không giữ snapshot cho entity load) và `FlushMode.MANUAL` → tiết kiệm bộ nhớ và CPU dirty checking. Spring gọi `Connection.setReadOnly(true)` → driver/DB có thể tối ưu (PostgreSQL `SET TRANSACTION READ ONLY` → ghi sẽ lỗi). Có thể định tuyến replica bằng `AbstractRoutingDataSource` dựa trên `TransactionSynchronizationManager.isCurrentTransactionReadOnly()` — **bắt buộc** bọc bằng `LazyConnectionDataSourceProxy` để connection chỉ được lấy sau khi transaction đã biết là read-only.

**Giải thích chi tiết:**
```java
public class RoutingDataSource extends AbstractRoutingDataSource {
    @Override protected Object determineCurrentLookupKey() {
        return TransactionSynchronizationManager.isCurrentTransactionReadOnly() ? "replica" : "primary";
    }
}
@Bean DataSource dataSource(RoutingDataSource r) { return new LazyConnectionDataSourceProxy(r); }
```
- `readOnly` không phải cơ chế bảo mật: gọi `flush()` thủ công vẫn có thể ghi nếu DB không chặn.
- Replica có **replication lag** → read-your-writes: dữ liệu vừa ghi có thể chưa thấy (giải pháp ở Module 11).
- Method `readOnly` tham gia transaction ghi có sẵn → cờ bị bỏ qua (Q48).

**Câu hỏi nối tiếp:**
- *Vì sao thiếu `LazyConnectionDataSourceProxy` thì routing luôn ra primary?* → `JpaTransactionManager` lấy connection **ngay khi bắt đầu** transaction, trước khi cờ readOnly được gắn vào `TransactionSynchronizationManager`.

**⚠️ Câu trả lời gây điểm trừ:**
- "readOnly chỉ là gợi ý, không có tác dụng gì."
- Không nhắc replication lag khi nói về replica.

**📖 Ôn lại:** [11.6 readOnly = true](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#11-transactions)

</details>

### Q52. 🔴 Review code sau. Bạn thiết kế lại luồng checkout thế nào?

```java
@Transactional
public void checkout(Cart cart) {
    Order o = orderRepo.save(Order.from(cart));
    paymentClient.charge(o);          // HTTP 2–30 giây
    emailClient.sendConfirmation(o);
}
```

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Ba vấn đề: (1) giữ connection (và lock nếu có) suốt thời gian gọi mạng → đối tác chậm là pool cạn, cả hệ thống treo; (2) **không nhất quán**: payment thành công nhưng commit DB thất bại (hoặc ngược lại), email gửi cho đơn có thể bị rollback; (3) retry không an toàn nếu không có idempotency. Thiết kế lại: chia thành các **transaction ngắn** — tx1 tạo order `PENDING` → gọi payment **ngoài transaction** với idempotency key, timeout, circuit breaker → tx2 cập nhật `PAID`/`PAYMENT_FAILED` → email gửi **sau commit** qua `@TransactionalEventListener(AFTER_COMMIT)` hoặc outbox; thêm job đối soát cho đơn treo `PENDING`.

**Giải thích chi tiết:**
```java
public void checkout(Cart cart) {                       // KHÔNG @Transactional
    OrderId id = orderTx.createPending(cart);            // tx1
    PaymentResult r;
    try {
        r = paymentClient.charge(id, idempotencyKey(id)); // timeout 3s, circuit breaker
    } catch (TimeoutException e) {
        return;                                          // để job đối soát hỏi trạng thái payment
    }
    orderTx.applyPaymentResult(id, r);                   // tx2: PAID/FAILED + publish OrderPaid
}
```
- `@Transactional(timeout = 5)` làm lưới an toàn; dùng `Propagation.NEVER` trên method gọi API ngoài để chặn ai đó bọc nó vào transaction.
- Idempotency key cho phép retry charge mà không trừ tiền hai lần.
- Đối soát: job quét order `PENDING` > 5 phút, hỏi trạng thái payment provider.

**Câu hỏi nối tiếp:**
- *Vì sao không dùng 2PC giữa DB và payment?* → API HTTP không hỗ trợ XA; 2PC blocking, latency cao (Q57).
- *Đo hiệu quả?* → Load test với WireMock chậm 5 s, pool 10, 100 request: bản cũ timeout lấy connection, bản mới không.

**⚠️ Câu trả lời gây điểm trừ:**
- "Tăng pool size và timeout."
- Chỉ chuyển email ra sau mà vẫn giữ HTTP payment trong transaction.

**📖 Ôn lại:** [11.7 Transaction dài và gọi hệ thống bên ngoài](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#11-transactions)

</details>

### Q53. 🟡 Khi nào bạn dùng `TransactionTemplate` thay vì `@Transactional`?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Khi ranh giới transaction phụ thuộc vòng lặp/điều kiện (mỗi chunk một transaction), khi muốn tránh self-invocation mà không tách bean, khi cần transaction trong code không phải Spring bean, hoặc khi cần nhiều cấu hình (propagation/isolation/timeout) khác nhau trong cùng class. Spring Boot tự cấu hình sẵn bean `TransactionTemplate`.

**Giải thích chi tiết:**
```java
for (List<Row> chunk : Lists.partition(rows, 500)) {
    try {
        tx.executeWithoutResult(status -> repo.saveAll(chunk.stream().map(Row::toEntity).toList()));
        ok += chunk.size();
    } catch (DataAccessException e) {
        failed += chunk.size();          // chunk lỗi rollback riêng, chunk khác không ảnh hưởng
    }
}
```
- `status.setRollbackOnly()` để rollback mà không ném exception.
- Ranh giới transaction hiển thị rõ ngay trong code → dễ review hơn annotation trong một số luồng phức tạp.

**Câu hỏi nối tiếp:**
- *Nhược điểm?* → Code dài hơn, trộn hạ tầng vào logic; với use case thông thường `@Transactional` vẫn gọn hơn.

**⚠️ Câu trả lời gây điểm trừ:**
- "TransactionTemplate là cách cũ, không còn dùng."

**📖 Ôn lại:** [11.8 Programmatic: TransactionTemplate](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#11-transactions)

</details>

### Q54. 🔴 Dùng `@TransactionalEventListener` có những bẫy gì? Vì sao gửi event Kafka trong listener `AFTER_COMMIT` vẫn có thể mất event?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Bẫy chính: (1) không có transaction đang chạy → listener **không được gọi** trừ khi `fallbackExecution = true`; (2) trong `AFTER_COMMIT`, transaction đã commit nhưng resource vẫn gắn thread → ghi DB với `REQUIRED` sẽ **không bao giờ được commit** — phải dùng `REQUIRES_NEW` (Spring 6.1+ còn từ chối listener có `@Transactional` không phải `REQUIRES_NEW`/`NOT_SUPPORTED`); (3) exception trong listener không rollback được gì; (4) chạy đồng bộ trên thread request → chậm response nếu không kèm `@Async`. Mất event vì đây là **dual-write**: DB đã commit, app crash (hoặc Kafka lỗi) trước khi gửi xong → event mất vĩnh viễn. Cần **Transactional Outbox** để đảm bảo at-least-once.

**Giải thích chi tiết:**
```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)   // mặc định là AFTER_COMMIT
@Async
public void on(OrderPlaced e) { emailClient.send(e.orderId()); }
```
- Phase: `BEFORE_COMMIT` (còn trong transaction — có thể ghi thêm và lỗi sẽ rollback), `AFTER_COMMIT`, `AFTER_ROLLBACK`, `AFTER_COMPLETION`.
- Dùng AFTER_COMMIT cho side-effect "best-effort" (email thông báo, xóa cache); dùng outbox cho event mà hệ thống khác phụ thuộc.
- Outbox: ghi bản ghi event vào bảng `outbox` trong cùng transaction nghiệp vụ, relay (polling `SKIP LOCKED` hoặc CDC Debezium) đẩy sang Kafka, consumer idempotent.

**Câu hỏi nối tiếp:**
- *Listener `@Async` mất gì?* → Mất SecurityContext/MDC nếu không propagate; exception chỉ log ở executor.

**⚠️ Câu trả lời gây điểm trừ:**
- "AFTER_COMMIT đảm bảo event luôn được gửi."
- Ghi DB trong AFTER_COMMIT với `REQUIRED` và không hiểu vì sao dữ liệu không lưu.

**📖 Ôn lại:** [11.9 @TransactionalEventListener](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#11-transactions)

</details>

### Q55. 🟡 Đặt `@Transactional` trên class test (`@DataJpaTest` mặc định có) có rủi ro gì?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Mỗi test chạy trong một transaction và **rollback** cuối test → che giấu các lỗi chỉ xảy ra lúc flush/commit: vi phạm constraint (Q14, Q15), `LazyInitializationException` (vì persistence context còn mở suốt test), hành vi `AFTER_COMMIT` listener không bao giờ chạy, `REQUIRES_NEW` thấy dữ liệu khác với thực tế. Nên có ít nhất một nhóm test tích hợp **không** bọc transaction (dọn dữ liệu bằng truncate/Testcontainers mới), hoặc gọi `flush()` tường minh trong test.

**Giải thích chi tiết:**
- Test đếm số SQL cũng bị sai lệch: entity đã nằm trong first-level cache từ bước setup → không thấy N+1.
- Dùng `TestTransaction.flagForCommit()`/`end()` khi cần kiểm tra hành vi commit trong test có transaction.
- Test nên chạy trên DB thật bằng Testcontainers.

**Câu hỏi nối tiếp:**
- *Làm sao tách dữ liệu giữa các test không có transaction?* → Truncate bảng trong `@AfterEach`, hoặc dùng dữ liệu có khóa ngẫu nhiên riêng cho mỗi test.

**⚠️ Câu trả lời gây điểm trừ:**
- "Test có `@Transactional` là best practice, không có rủi ro."

**📖 Ôn lại:** [11.9 — checklist review @Transactional (mục 8)](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#11-transactions)

</details>

### Q56. 🔴 Bạn muốn import file 10.000 dòng: dòng lỗi thì bỏ qua, các dòng khác commit cùng nhau. Đồng nghiệp đề xuất `Propagation.NESTED` với Spring Data JPA. Bạn đánh giá thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `NESTED` dùng **JDBC savepoint** — hợp với `DataSourceTransactionManager` (JDBC/MyBatis). Với `JpaTransactionManager`, `nestedTransactionAllowed` mặc định `false`; dù bật, savepoint chỉ rollback ở mức JDBC connection, **không khôi phục persistence context** — entity trong bộ nhớ vẫn mang thay đổi đã bị rollback, và lần flush sau có thể ghi lại chúng hoặc gặp trạng thái không nhất quán; exception từ Hibernate còn làm session không dùng tiếp được. Vì vậy NESTED với JPA là vùng nguy hiểm. Tôi chọn: **validate trước** để loại dòng lỗi, rồi ghi theo chunk bằng `TransactionTemplate`; chunk lỗi thì chia nhỏ/ghi từng dòng trong transaction riêng để xác định dòng hỏng.

**Giải thích chi tiết:**
- Với JDBC thuần/MyBatis: `NESTED` ổn — `rollback to savepoint` cho dòng lỗi, phần còn lại commit cùng nhau.
- PostgreSQL: sau một lỗi trong transaction, mọi câu lệnh tiếp theo báo "current transaction is aborted" **trừ khi** rollback về savepoint — đó là lý do savepoint cần thiết nếu muốn đi tiếp.
- Nếu yêu cầu "tất cả dòng hợp lệ phải commit nguyên tử cùng nhau": validate toàn bộ trước (không DB), rồi ghi một lần; vi phạm constraint DB còn lại (trùng khóa) xử lý bằng `INSERT ... ON CONFLICT DO NOTHING` và báo cáo.

**Câu hỏi nối tiếp:**
- *NESTED khác REQUIRES_NEW thế nào?* → NESTED cùng connection, commit cùng outer; REQUIRES_NEW connection riêng, commit độc lập.

**⚠️ Câu trả lời gây điểm trừ:**
- "NESTED là transaction lồng thật sự như REQUIRES_NEW."
- Đồng ý dùng NESTED với JPA mà không nhắc persistence context.

**📖 Ôn lại:** [11.3 — Kịch bản 3 NESTED](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#11-transactions)

</details>

---

<a id="nhom-j"></a>
## J. Transaction phân tán, MyBatis, schema migration

### Q57. 🔴 Service cần lưu đơn hàng vào DB và gửi event `OrderCreated` lên Kafka một cách nhất quán. Đồng nghiệp viết `kafkaTemplate.send()` ngay trong method `@Transactional`. Bạn đánh giá và đề xuất gì?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Đó là dual-write: `send()` có thể thành công rồi transaction DB rollback (consumer nhận event về đơn không tồn tại), hoặc DB commit nhưng gửi Kafka thất bại/app crash (mất event). `@Transactional` không bao được cả DB lẫn Kafka một cách nguyên tử (đồng bộ hóa transaction Kafka–DB của Spring chỉ là best-effort, vẫn có cửa sổ lỗi). 2PC/XA thì Kafka không hỗ trợ, lại blocking và chậm. Đề xuất: **Transactional Outbox** — ghi order + bản ghi `outbox` trong cùng một transaction cục bộ; relay (polling với `FOR UPDATE SKIP LOCKED` hoặc CDC Debezium) publish lên Kafka → at-least-once; consumer **idempotent** (dedupe theo event id).

**Giải thích chi tiết:**
| | 2PC/XA | Outbox | Saga |
|---|---|---|---|
| Đảm bảo | Nguyên tử (khi mọi resource hỗ trợ) | At-least-once, eventual | Nhất quán cuối cùng qua compensating |
| Nhược | Blocking khi coordinator chết (in-doubt), latency, ít hệ thống hỗ trợ | Cần relay, consumer idempotent, độ trễ | Phức tạp, phải thiết kế bù trừ |
| Dùng khi | Hai DB truyền thống cùng hỗ trợ XA (hiếm trong microservices) | DB + broker | Quy trình nhiều service |

- Bảng outbox: `(id, aggregate_id, type, payload jsonb, created_at, published_at)`; giữ thứ tự theo aggregate (partition key Kafka = aggregate_id).

**Câu hỏi nối tiếp:**
- *Relay crash sau khi publish nhưng trước khi đánh dấu `published_at`?* → Gửi lại lần nữa → chính vì vậy consumer phải idempotent.
- *Polling vs CDC?* → Polling đơn giản, thêm tải DB và độ trễ; CDC đọc WAL/binlog, độ trễ thấp, thêm hạ tầng (Kafka Connect).

**⚠️ Câu trả lời gây điểm trừ:**
- "Dùng `@Transactional` là đủ."
- "Gửi Kafka trong `AFTER_COMMIT` là đảm bảo" (Q54).

**📖 Ôn lại:** [12.1 Transaction phân tán: 2PC/XA vs Outbox](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#12-distributed-mybatis-migration)

</details>

### Q58. 🟡 So sánh JPA/Hibernate với MyBatis. Dự án ngân hàng với nhiều báo cáo phức tạp nên chọn gì?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** JPA/Hibernate là ORM: map object ↔ bảng, quản lý trạng thái (dirty checking, cache L1/L2, lazy) — mạnh cho domain phong phú, CRUD và ghi theo aggregate, nhưng "ma thuật ẩn" dễ gây hiệu năng bất ngờ. MyBatis là SQL mapper: bạn viết SQL, MyBatis map kết quả — kiểm soát SQL hoàn toàn, hợp query phức tạp, báo cáo, schema legacy "lạ", đội mạnh SQL (rất phổ biến ở ngân hàng Việt Nam). Với nhiều báo cáo: có thể **kết hợp** — JPA cho ghi, MyBatis/jOOQ/`JdbcTemplate` cho đọc, dùng chung DataSource và transaction manager.

**Giải thích chi tiết:**
- MyBatis: `#{}` = bind parameter của `PreparedStatement`; `${}` = **nối chuỗi** → SQL injection (chỉ dùng cho phần đã whitelist như tên cột sort).
- MyBatis dùng `DataSourceTransactionManager`; `JpaTransactionManager` cũng expose JDBC connection cho code JDBC/MyBatis trong cùng transaction → trộn được trong một transaction (lưu ý flush JPA trước khi MyBatis đọc dữ liệu vừa sửa).

**Câu hỏi nối tiếp:**
- *Nhược điểm MyBatis?* → SQL lặp lại, mapping thủ công, không dirty checking, refactor schema phải sửa nhiều XML.

**⚠️ Câu trả lời gây điểm trừ:**
- Chọn theo sở thích mà không nêu tiêu chí.
- Không biết khác biệt `#{}` và `${}`.

**📖 Ôn lại:** [12.2 MyBatis — so sánh ngắn](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#12-distributed-mybatis-migration)

</details>

### Q59. 🟢 Vì sao không dùng `spring.jpa.hibernate.ddl-auto=update` ở production? Bạn quản lý schema và đổi tên cột không downtime thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `ddl-auto=update` không xóa cột, không đổi kiểu an toàn, không có lịch sử, không review được, có thể chạy DDL khóa bảng ngoài ý muốn. Production nên `validate` hoặc `none`, schema quản lý bằng **Flyway** (`V1__init.sql`, bảng `flyway_schema_history` với checksum) hoặc **Liquibase** (changeSet, `DATABASECHANGELOG`, rollback khai báo). Đổi tên cột không downtime dùng **expand/contract**: thêm cột mới → code ghi cả hai, đọc cũ + backfill theo lô → code đọc cột mới → release sau mới xóa cột cũ — vì khi rolling deploy, phiên bản cũ và mới chạy song song trên cùng schema.

**Giải thích chi tiết:**
- Sửa file migration đã chạy → checksum mismatch → app không khởi động (đúng như mong muốn).
- DDL nguy hiểm: tạo index trên bảng lớn → PostgreSQL `CREATE INDEX CONCURRENTLY` (không chạy được trong transaction → cấu hình Flyway non-transactional cho script đó); MySQL online DDL / gh-ost / pt-online-schema-change.
- Nhiều pod cùng khởi động chạy migration: Flyway/Liquibase có lock, nhưng migration dài làm pod kẹt ở startup và bị liveness probe kill → chạy migration như **Job riêng** trước deploy.

**Câu hỏi nối tiếp:**
- *Rollback schema thế nào?* → Thường rollback **code** chứ không rollback schema; migration forward-only, mỗi bước tương thích ngược.

**⚠️ Câu trả lời gây điểm trừ:**
- "Dùng `update` cho tiện, Hibernate tự lo."
- Đổi tên cột trực tiếp trong một release.

**📖 Ôn lại:** [12.3 Schema migration với Flyway / Liquibase](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#12-distributed-mybatis-migration) · [Module 11 — Migration không downtime](../01-giao-trinh/11-database-sql.md#phan-13)

</details>

---

> ✅ **Tự kiểm tra sau khi luyện:** đối chiếu với [Checklist tự đánh giá của Module 09](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#checklist-tu-danh-gia). Nếu trả lời trôi chảy ≥ 80% câu 🟡 và ≥ 60% câu 🔴, bạn đã sẵn sàng cho phần Data Access trong phỏng vấn Senior.

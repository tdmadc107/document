# Module 09 — Data Access: JDBC, JPA/Hibernate, Spring Data & Transactions

> **Mục tiêu:** sau module này bạn giải thích được con đường một câu SQL đi từ code Java xuống database (JDBC → connection pool → driver), giải thích được cách Hibernate quản lý entity (persistence context, dirty checking, flush), thiết kế được mapping và chiến lược fetching không gây N+1, chọn đúng chiến lược locking, cấu hình `@Transactional` chính xác (propagation, isolation, rollback rules) và debug được các sự cố production kinh điển: cạn connection pool, `LazyInitializationException`, `UnexpectedRollbackException`, lost update, transaction kéo dài vì gọi HTTP bên ngoài.
> **Cấp độ:** Cơ bản → Nâng cao (Senior)
> **Thời lượng gợi ý:** 7 ngày (≈ 35–40 giờ)
> **Yêu cầu trước:** Module về Java Core & Collections, Concurrency, Spring Core/Spring Boot (IoC, AOP proxy); SQL cơ bản (JOIN, index, transaction).
> **Nguồn tham khảo:**
> - Trong kho: [`Ebook IT/OCA_Oracle_Database_SQL_Exam_Guide_Exam_1Z0071.pdf`](../../Ebook%20IT/OCA_Oracle_Database_SQL_Exam_Guide_Exam_1Z0071.pdf) — ôn SQL nền tảng: SELECT, JOIN, subquery, DML và transaction control (COMMIT/ROLLBACK/SAVEPOINT).
> - Trong kho: [`Ebook IT/Design Pattern.pdf`](../../Ebook%20IT/Design%20Pattern.pdf) — Proxy pattern (nền tảng để hiểu `@Transactional` và lazy loading).
> - Ngoài:
>   - Hibernate ORM User Guide — https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html
>   - Jakarta Persistence 3.1 Specification — https://jakarta.ee/specifications/persistence/3.1/
>   - Spring Data JPA Reference — https://docs.spring.io/spring-data/jpa/reference/
>   - Spring Framework Reference, phần *Data Access → Transaction Management* — https://docs.spring.io/spring-framework/reference/data-access/transaction.html
>   - HikariCP README & wiki *About Pool Sizing* — https://github.com/brettwooldridge/HikariCP/wiki/About-Pool-Sizing
>   - Vlad Mihalcea, *High-Performance Java Persistence* (sách) và blog https://vladmihalcea.com
>   - Flyway docs https://documentation.red-gate.com/flyway, Liquibase docs https://docs.liquibase.com
>   - PostgreSQL docs, chương *Concurrency Control* — https://www.postgresql.org/docs/current/mvcc.html

## Mục lục
1. [JDBC nền tảng](#1-jdbc)
2. [Connection pooling với HikariCP](#2-hikaricp)
3. [JPA/Hibernate: entity lifecycle, persistence context, flush](#3-persistence-context)
4. [Chiến lược sinh ID và batching](#4-id-batching)
5. [Mapping quan hệ, kế thừa, embeddable](#5-mapping)
6. [Fetching: LAZY/EAGER, N+1 và các cách sửa](#6-fetching)
7. [Second-level cache & query cache](#7-cache)
8. [JPQL, Criteria API, native SQL](#8-query)
9. [Spring Data JPA](#9-spring-data)
10. [Concurrency control: optimistic vs pessimistic locking](#10-locking)
11. [Spring Transactions chuyên sâu](#11-transactions)
12. [Transaction phân tán, MyBatis, schema migration](#12-distributed-mybatis-migration)
13. [Dự án mini của module](#du-an-mini)
14. [Checklist tự đánh giá](#checklist-tu-danh-gia)

---

<a id="1-jdbc"></a>
## 1. JDBC nền tảng

### 1.1 Khái niệm
**JDBC (Java Database Connectivity)** là API chuẩn (`java.sql`, `javax.sql`) để Java nói chuyện với database quan hệ. Mọi thứ phía trên — Hibernate, Spring `JdbcTemplate`, MyBatis, jOOQ — rốt cuộc đều gọi JDBC. Vì vậy, khi production có vấn đề về hiệu năng DB, Senior phải "nhìn xuyên" ORM xuống tầng JDBC: bao nhiêu câu SQL, bao nhiêu round-trip, giữ connection bao lâu.

Các interface chính:

| Interface | Vai trò |
|---|---|
| `DataSource` | Factory tạo `Connection`; trong production luôn là pool (HikariCP). Thay thế `DriverManager`. |
| `Connection` | Một phiên làm việc vật lý (TCP socket + session trên DB). Mang trạng thái transaction: `autoCommit`, `isolation`, `readOnly`. |
| `Statement` | Thực thi SQL dạng chuỗi tĩnh. |
| `PreparedStatement` | SQL có tham số `?`, được precompile; chống SQL injection. |
| `CallableStatement` | Gọi stored procedure. |
| `ResultSet` | Con trỏ duyệt kết quả. |

```java
import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;

public class AccountDao {
    private final DataSource ds;

    public AccountDao(DataSource ds) { this.ds = ds; }

    public BigDecimal findBalance(long accountId) throws SQLException {
        String sql = "SELECT balance FROM account WHERE id = ?";
        // try-with-resources: đóng ResultSet -> PreparedStatement -> Connection theo thứ tự ngược
        try (Connection con = ds.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal("balance") : null;
            }
        }
    }

    /** Chuyển tiền thủ công bằng JDBC transaction. */
    public void transfer(long from, long to, BigDecimal amount) throws SQLException {
        try (Connection con = ds.getConnection()) {
            boolean oldAutoCommit = con.getAutoCommit();
            con.setAutoCommit(false);                 // bắt đầu transaction
            try (PreparedStatement debit = con.prepareStatement(
                     "UPDATE account SET balance = balance - ? WHERE id = ? AND balance >= ?");
                 PreparedStatement credit = con.prepareStatement(
                     "UPDATE account SET balance = balance + ? WHERE id = ?")) {
                debit.setBigDecimal(1, amount); debit.setLong(2, from); debit.setBigDecimal(3, amount);
                if (debit.executeUpdate() != 1) throw new SQLException("Insufficient funds");
                credit.setBigDecimal(1, amount); credit.setLong(2, to);
                credit.executeUpdate();
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(oldAutoCommit);     // trả connection về pool ở trạng thái sạch
            }
        }
    }
}
```

### 1.2 `Statement` vs `PreparedStatement` và SQL injection

```java
// ❌ SQL injection: name = "x' OR '1'='1"  -> trả về toàn bộ bảng
String sql = "SELECT * FROM users WHERE name = '" + name + "'";
stmt.executeQuery(sql);

// ✅ Tham số được gửi tách biệt khỏi câu lệnh, không bao giờ được parse như SQL
PreparedStatement ps = con.prepareStatement("SELECT * FROM users WHERE name = ?");
ps.setString(1, name);
```

Lợi ích của `PreparedStatement`:
1. **An toàn:** giá trị tham số không tham gia vào quá trình parse SQL.
2. **Hiệu năng:** DB có thể cache execution plan (Oracle shared pool, PostgreSQL server-side prepared statement sau `prepareThreshold` lần — mặc định 5 trong PgJDBC). Statement cache phía driver: MySQL `cachePrepStmts=true`, `prepStmtCacheSize`.
3. **Type-safety** với `setTimestamp`, `setBigDecimal`, ...

> ⚠️ **Lỗi thường gặp:** `PreparedStatement` **không** tham số hóa được tên bảng, tên cột, `ORDER BY` hay danh sách `IN` có độ dài biến đổi. Code kiểu `"ORDER BY " + sortColumn` vẫn bị injection. Giải pháp: **whitelist** (map từ giá trị client → tên cột hợp lệ). Với `IN`, sinh đúng số `?` hoặc dùng `= ANY(?)` với array trên PostgreSQL.

> ⚠️ Với JPA cũng vậy: `em.createQuery("... where name = '" + name + "'")` là JPQL injection. Luôn dùng `:param`.

### 1.3 Batch
Mỗi `executeUpdate()` là một **network round-trip**. Insert 10.000 dòng từng cái một với RTT 1 ms = 10 giây chỉ để chờ mạng.

```java
String sql = "INSERT INTO event(id, type, payload) VALUES (?, ?, ?)";
try (Connection con = ds.getConnection();
     PreparedStatement ps = con.prepareStatement(sql)) {
    con.setAutoCommit(false);
    int i = 0;
    for (Event e : events) {
        ps.setLong(1, e.id()); ps.setString(2, e.type()); ps.setString(3, e.payload());
        ps.addBatch();
        if (++i % 500 == 0) ps.executeBatch();   // flush theo lô, tránh giữ quá nhiều bộ nhớ
    }
    ps.executeBatch();
    con.commit();
}
```

Lưu ý theo driver:
- **MySQL Connector/J**: cần `rewriteBatchedStatements=true` thì batch insert mới được viết lại thành một câu `INSERT ... VALUES (...),(...),...`; nếu không, driver vẫn gửi từng câu.
- **PostgreSQL (PgJDBC)**: `reWriteBatchedInserts=true` gộp insert thành multi-values.
- `executeBatch()` trả về `int[]`; giá trị có thể là `Statement.SUCCESS_NO_INFO (-2)` khi driver rewrite → không dựa vào đó để đếm chính xác.
- Lỗi giữa batch → `BatchUpdateException`; hành vi tiếp tục hay dừng phụ thuộc driver → luôn chạy batch trong transaction để rollback toàn bộ.

### 1.4 ResultSet và fetch size
`fetchSize` = số dòng driver lấy mỗi lần round-trip khi duyệt `ResultSet`.

| Driver | Mặc định | Ghi chú |
|---|---|---|
| Oracle | 10 dòng | Quá nhỏ cho report → tăng lên 100–1000 (`defaultRowPrefetch`). |
| PostgreSQL | Lấy **toàn bộ** kết quả vào bộ nhớ | Chỉ dùng cursor khi `autoCommit=false` **và** `fetchSize > 0` và statement `TYPE_FORWARD_ONLY`. |
| MySQL | Lấy toàn bộ | Streaming với `fetchSize = Integer.MIN_VALUE`, hoặc `useCursorFetch=true` + fetchSize > 0. |

> 💡 **Góc nhìn Senior:** sự cố OOM kinh điển — job export "SELECT * FROM transaction" chạy ổn ở UAT (10k dòng) nhưng OOM ở production (20 triệu dòng) vì PostgreSQL driver load hết vào heap. Cách sửa: stream với fetch size (JDBC), `Stream<T>` + `@QueryHints(HINT_FETCH_SIZE)` trong Spring Data (phải trong transaction và nhớ `close()` stream), hoặc **keyset pagination** (`WHERE id > :lastId ORDER BY id LIMIT 1000`) — an toàn nhất vì không giữ transaction dài.

### 1.5 Các bẫy JDBC trên production
- **Quên đóng resource** → connection leak → pool cạn → toàn bộ request treo ở `getConnection()`. Luôn dùng try-with-resources.
- **Trả connection về pool ở trạng thái bẩn** (autoCommit=false, isolation đã đổi, readOnly=true). HikariCP reset các thuộc tính này khi trả về, nhưng **không** rollback hộ business logic dang dở nếu bạn tự quản lý — luôn commit/rollback rõ ràng (HikariCP có rollback transaction chưa commit khi connection được close, nhưng đừng dựa vào đó làm thiết kế).
- **`Statement.setQueryTimeout`**: nên đặt để query xấu không giữ connection vô hạn; Spring có `@Transactional(timeout=...)` và `spring.jpa.properties.jakarta.persistence.query.timeout`.
- **So sánh `Timestamp`/time zone**: dùng `java.time` (`setObject(i, OffsetDateTime)`) với JDBC 4.2+, thống nhất UTC ở DB và JVM (`hibernate.jdbc.time_zone=UTC`).

### 🛠 Bài tập phần 1

**Bài 1.1 — DAO an toàn với injection (Cơ bản)**
- Đề bài: viết `UserDao.search(String keyword, String sortBy, boolean asc)` dùng JDBC thuần, tìm user theo `name LIKE`, sắp xếp theo cột client truyền vào.
- Tiêu chí đạt: không có chuỗi nối trực tiếp từ input vào SQL ngoài whitelist; test với `keyword = "x' OR '1'='1"` và `sortBy = "name; DROP TABLE users"` không gây lỗi bảo mật (trả về rỗng / ném `IllegalArgumentException`).

**Bài 1.2 — Đo lợi ích batch (Trung bình)**
- Đề bài: insert 50.000 dòng vào PostgreSQL (Testcontainers) theo 3 cách: từng câu + autoCommit; batch 500 không `reWriteBatchedInserts`; batch 500 có `reWriteBatchedInserts=true`.
- Tiêu chí đạt: bảng kết quả thời gian 3 cách; giải thích vì sao chênh lệch (round-trip, commit/fsync mỗi câu).

**Bài 1.3 — Export 5 triệu dòng không OOM (Nâng cao)**
- Đề bài: viết job export bảng lớn ra CSV với heap `-Xmx256m`.
- Tiêu chí đạt: chạy thành công; chứng minh bằng log GC/heap rằng bộ nhớ ổn định; so sánh cách streaming-cursor và keyset pagination về thời gian giữ transaction.

<details>
<summary>Gợi ý lời giải</summary>

- 1.1: `Map<String,String> SORTABLE = Map.of("name","u.name","created","u.created_at")`; `String col = Optional.ofNullable(SORTABLE.get(sortBy)).orElseThrow(...)`; keyword truyền qua `ps.setString(1, "%" + escapeLike(keyword) + "%")` (escape `%` và `_`).
- 1.2: kỳ vọng cách 1 chậm nhất (mỗi câu một commit → fsync WAL), cách 3 nhanh nhất (một câu multi-values cho mỗi lô).
- 1.3: PostgreSQL cần `con.setAutoCommit(false); ps.setFetchSize(1000);`. Keyset: `SELECT ... WHERE id > ? ORDER BY id LIMIT 1000` lặp đến khi rỗng — mỗi trang là transaction ngắn, có thể resume khi lỗi.

</details>

---

<a id="2-hikaricp"></a>
## 2. Connection pooling với HikariCP

### 2.1 Tại sao cần pool
Mở một connection vật lý tốn: TCP handshake, TLS, xác thực, khởi tạo session phía DB (PostgreSQL fork một backend process cho mỗi connection). Mất từ vài ms đến hàng chục ms. Pool giữ sẵn N connection và cho mượn. HikariCP là pool mặc định của Spring Boot 2+.

```yaml
spring:
  datasource:
    url: jdbc:postgresql://db:5432/shop
    username: shop
    password: ${DB_PASSWORD}
    hikari:
      pool-name: shop-pool
      maximum-pool-size: 20        # mặc định 10
      minimum-idle: 20             # mặc định = maximum-pool-size (pool cố định — khuyến nghị của HikariCP)
      connection-timeout: 3000     # ms chờ mượn connection (mặc định 30000) — fail fast
      max-lifetime: 1680000        # 28 phút; phải NGẮN hơn timeout của DB/firewall/LB vài chục giây
      idle-timeout: 600000         # chỉ có tác dụng khi minimum-idle < maximum-pool-size
      keepalive-time: 120000       # ping connection idle để tránh bị firewall cắt
      leak-detection-threshold: 20000  # log stacktrace nếu connection bị giữ > 20s
```

### 2.2 Sizing — công thức và tư duy
Wiki HikariCP *About Pool Sizing* trích công thức của PostgreSQL:

```
connections = (core_count * 2) + effective_spindle_count
```

Ví dụ DB server 8 core, SSD (spindle ≈ 1) → ~17 connection **cho toàn bộ DB**, không phải cho mỗi instance ứng dụng. Ý tưởng then chốt: DB chỉ thực thi song song được ~số core; thêm connection quá mức chỉ tăng context switching, lock contention và bộ nhớ. Pool nhỏ + hàng đợi phía ứng dụng thường cho **throughput cao hơn và latency thấp hơn** pool lớn.

Tính toán tổng thể:
```
tổng connection tới DB = số instance × maximumPoolSize (+ batch jobs, + công cụ admin)
```
Với 10 pod × 50 connection = 500 connection vào PostgreSQL có `max_connections=200` → pod mới không khởi động được, hoặc DB ngốn RAM (mỗi backend process vài MB). Khi đó cần PgBouncer (transaction pooling) hoặc giảm pool size.

**Deadlock do pool:** nếu một thread cần đồng thời giữ `Cm` connection (ví dụ transaction ngoài giữ 1 connection, gọi method `REQUIRES_NEW` cần connection thứ 2), với `Tn` thread đồng thời, pool tối thiểu để tránh deadlock là:
```
pool_size = Tn × (Cm − 1) + 1
```
Ví dụ 20 thread, mỗi thread cần 2 connection → tối thiểu 21. Nếu pool = 10 và 10 request cùng vào: mỗi request giữ 1 connection rồi chờ connection thứ 2 → **treo toàn bộ** đến khi `connectionTimeout`.

### 2.3 Các timeout và ý nghĩa

| Thuộc tính | Ý nghĩa | Lời khuyên |
|---|---|---|
| `connectionTimeout` | Thời gian tối đa chờ mượn connection từ pool | 2–5 s để fail fast; 30 s mặc định làm request treo, thread pool Tomcat cạn theo. |
| `maxLifetime` | Tuổi thọ tối đa một connection, sau đó bị retire (khi không được dùng) | Nhỏ hơn `wait_timeout` (MySQL), idle timeout của firewall/NAT/LB. HikariCP thêm biến thiên ngẫu nhiên để tránh retire hàng loạt. |
| `idleTimeout` | Connection idle quá lâu bị đóng (khi pool > minimumIdle) | Không tác dụng với pool cố định. |
| `keepaliveTime` | Ping connection idle định kỳ | Tránh "connection reset" sau khi firewall cắt phiên idle. |
| `validationTimeout` | Thời gian kiểm tra connection còn sống | Phải < `connectionTimeout`. |
| `leakDetectionThreshold` | Cảnh báo connection bị giữ quá lâu | Bật ở staging/production với ngưỡng > thời gian transaction dài nhất hợp lệ. |

### 2.4 Leak detection và quan sát pool
Khi bật `leakDetectionThreshold`, HikariCP log `Connection leak detection triggered for ... on thread ..., stack trace follows` kèm stacktrace nơi connection được mượn. Đây là **cảnh báo** (connection có thể chỉ chậm chứ chưa leak); nếu sau đó trả về sẽ có log "Previously reported leaked connection ... was returned".

Metrics (Micrometer, tự có trong Spring Boot Actuator):
- `hikaricp.connections.active`, `idle`, `pending` (số thread đang chờ) — `pending > 0` kéo dài là dấu hiệu bão hòa.
- `hikaricp.connections.acquire` (thời gian mượn), `hikaricp.connections.usage` (thời gian giữ), `hikaricp.connections.timeout` (số lần timeout).

> 💡 **Góc nhìn Senior:** "Pool cạn" hầu như **không** được sửa bằng cách tăng pool size. Nguyên nhân gốc thường là: (1) giữ connection quá lâu — transaction bao quanh lời gọi HTTP/Kafka, OSIV giữ connection suốt request render view; (2) query chậm do thiếu index; (3) leak; (4) deadlock pool do `REQUIRES_NEW` lồng nhau. Nhìn `usage` p99 và `pending` trước khi chỉnh con số.

> ⚠️ **Lỗi thường gặp:** đặt `maximum-pool-size: 100` "cho chắc" trên mỗi pod rồi scale HPA lên 30 pod → DB sụp vì 3000 connection. Luôn tính tổng connection theo số pod tối đa.

### 🛠 Bài tập phần 2

**Bài 2.1 — Tính pool size (Cơ bản)**
- Đề bài: DB PostgreSQL 16 vCPU, `max_connections=300`, 3 service (A: 8 pod, B: 4 pod, C: batch 1 pod). Đề xuất `maximumPoolSize` cho từng service và giải thích.
- Tiêu chí đạt: tổng connection < 80% `max_connections` khi scale tối đa; có tính dự phòng cho rolling deploy (pod cũ + pod mới cùng sống).

**Bài 2.2 — Tái hiện pool deadlock (Trung bình)**
- Đề bài: Spring Boot với `maximum-pool-size=5`; service A `@Transactional` gọi service B `@Transactional(propagation = REQUIRES_NEW)`. Bắn 20 request đồng thời.
- Tiêu chí đạt: tái hiện được `SQLTransientConnectionException: ... Connection is not available, request timed out`; giải thích bằng công thức `Tn × (Cm − 1) + 1`; đưa ra 2 cách sửa.

**Bài 2.3 — Phát hiện leak (Nâng cao)**
- Đề bài: cố tình viết một DAO quên đóng connection trong nhánh exception. Bật leak detection, expose metrics qua Actuator/Prometheus, viết alert rule.
- Tiêu chí đạt: log leak chỉ đúng dòng code; dashboard thấy `active` tăng dần không giảm; alert khi `pending > 0` quá 1 phút.

<details>
<summary>Gợi ý lời giải</summary>

- 2.1: theo công thức, DB 16 core ≈ 33 connection "hiệu quả" — nhưng con số thực tế còn phụ thuộc tỉ lệ thời gian chờ I/O. Một phương án: A 8 pod × 10 = 80, B 4 × 10 = 40, C 1 × 5 = 5 → 125; khi rolling deploy surge 25% → ~155 < 240. Đo thực tế bằng load test rồi điều chỉnh.
- 2.2: cách sửa: (a) tăng pool ≥ `Tn × (Cm−1) + 1` với `Tn` = số thread Tomcat tối đa; (b) loại bỏ `REQUIRES_NEW` lồng — ghi audit sau commit bằng `@TransactionalEventListener` hoặc outbox; (c) giới hạn concurrency (bulkhead) trước khi vào service.
- 2.3: `spring.datasource.hikari.leak-detection-threshold=2000`; PromQL ví dụ: `max_over_time(hikaricp_connections_pending[1m]) > 0`.

</details>

---

<a id="3-persistence-context"></a>
## 3. JPA/Hibernate: entity lifecycle, persistence context, flush

### 3.1 Khái niệm
- **JPA (Jakarta Persistence)** là đặc tả (`jakarta.persistence.*` từ JPA 3.0; trước đó `javax.persistence`). **Hibernate ORM** là implementation phổ biến nhất (Spring Boot 3 dùng Hibernate 6).
- **`EntityManager`** (JPA) ≈ **`Session`** (Hibernate): đơn vị làm việc, bọc một **persistence context**.
- **Persistence context** = một `Map<EntityKey, Object>` (identity map) chứa các entity đang được quản lý + snapshot trạng thái ban đầu của chúng. Đây chính là **first-level cache**.

### 3.2 Bốn trạng thái của entity

```
           new Order()
               │
          [TRANSIENT] ──persist()──► [MANAGED] ◄──find()/query── DB
                                     │   ▲   │
                       remove()      │   │   │ detach()/clear()/close()
                                     ▼   │   ▼
                                [REMOVED]│ [DETACHED]
                                         └──merge() (trả về bản MANAGED khác)
```

| Trạng thái | Có id? | Trong persistence context? | Thay đổi có được lưu? |
|---|---|---|---|
| Transient | Thường chưa | Không | Không |
| Managed | Có | Có | Có — tự động qua dirty checking khi flush |
| Detached | Có | Không (context đã đóng/clear) | Không, trừ khi `merge` |
| Removed | Có | Có, đánh dấu xóa | `DELETE` khi flush |

```java
@Transactional
public void lifecycleDemo() {
    Order o = new Order("ORD-1");          // transient
    em.persist(o);                          // managed (chưa chắc đã INSERT ngay)
    o.setStatus(Status.PAID);               // không cần gọi save/update — dirty checking lo
    em.flush();                             // INSERT + UPDATE được gửi xuống DB
    em.detach(o);                           // detached
    o.setStatus(Status.SHIPPED);            // không được lưu
    Order managed = em.merge(o);            // copy trạng thái của o vào bản managed
    // o vẫn detached; managed mới là đối tượng được quản lý
    em.remove(managed);                     // removed -> DELETE khi flush/commit
}
```

### 3.3 `persist` vs `merge` (và `save` của Spring Data)

| | `persist(e)` | `merge(e)` |
|---|---|---|
| Đầu vào | Transient (dùng với detached → `EntityExistsException`/`PersistentObjectException: detached entity passed to persist`) | Transient hoặc detached |
| Trả về | `void` — chính `e` trở thành managed | **Bản sao managed** — `e` không đổi trạng thái |
| SQL | `INSERT` (khi flush; ngay lập tức nếu IDENTITY) | Có thể `SELECT` trước để load bản managed rồi copy trạng thái |

`SimpleJpaRepository.save()` trong Spring Data: `entityInformation.isNew(entity) ? em.persist(entity) : em.merge(entity)`. `isNew` mặc định dựa vào id `null` (hoặc 0 với primitive) hoặc `@Version` null. Hệ quả: entity với **id gán tay** (UUID tự sinh trong constructor, mã nghiệp vụ) luôn bị coi là "không mới" → `merge` → thêm một `SELECT` thừa cho mỗi insert. Sửa: implement `Persistable<ID>` với `isNew()` dựa trên cờ `@Transient`, hoặc thêm `@Version`.

> ⚠️ **Lỗi thường gặp:** `repository.save(entity)` trong một method `@Transactional` với entity đã managed — vô ích (dirty checking đã đủ), và với detached entity thì phải dùng **giá trị trả về**: `entity = repo.save(entity)`.

### 3.4 Dirty checking
Khi load entity, Hibernate lưu **snapshot** (mảng giá trị các thuộc tính). Lúc flush, nó so sánh từng thuộc tính hiện tại với snapshot; khác → sinh `UPDATE`.

Hệ quả hiệu năng:
- Chi phí flush ∝ số entity managed × số thuộc tính. Load 100.000 entity trong một transaction để đọc → flush tốn CPU + bộ nhớ gấp đôi (snapshot).
- Giải pháp cho đọc thuần: `@Transactional(readOnly = true)` (Spring + Hibernate đặt session read-only và `FlushMode.MANUAL` → không giữ snapshot, không dirty check), hoặc query DTO projection (không phải entity → không vào persistence context).
- Bytecode enhancement (`hibernate-enhance-maven-plugin` với `enableDirtyTracking`) cho phép tracking thay đổi thay vì so sánh toàn bộ.
- Mặc định `UPDATE` cập nhật **tất cả cột**; `@DynamicUpdate` chỉ cập nhật cột thay đổi (đổi lại mất cache câu SQL và ít lợi cho JDBC batching).

### 3.5 Flush và FlushMode
**Flush** = đồng bộ thay đổi trong persistence context xuống DB (gửi SQL), **không phải commit**. Flush xảy ra:
1. Trước khi commit transaction.
2. Trước khi chạy một query JPQL/Criteria có thể bị ảnh hưởng bởi thay đổi chưa flush (`FlushModeType.AUTO`).
3. Khi gọi `em.flush()` thủ công.

| FlushMode | Hành vi |
|---|---|
| `AUTO` (JPA, mặc định) | Flush trước commit và trước query liên quan. Hibernate native API: với **native SQL query**, Hibernate không biết bảng nào bị ảnh hưởng — trong chế độ JPA (bootstrap qua JPA) Hibernate flush trước native query cho đúng spec; dùng Session API native có thể không. |
| `COMMIT` | Chỉ flush khi commit — query có thể không thấy thay đổi chưa flush. |
| `MANUAL` (Hibernate) | Chỉ flush khi gọi `flush()` — dùng cho read-only. |
| `ALWAYS` (Hibernate) | Flush trước mọi query. |

**Thứ tự SQL khi flush** (Hibernate `ActionQueue`): insert → update → xóa phần tử collection → insert phần tử collection → delete. Không theo thứ tự bạn gọi! Ví dụ kinh điển: xóa một entity có unique key rồi persist entity mới cùng key trong cùng flush → `INSERT` chạy trước `DELETE` → vi phạm unique constraint. Sửa: gọi `em.flush()` sau `remove`.

### 3.6 First-level cache: lợi và hại
- `em.find(Order.class, 1L)` gọi hai lần trong cùng persistence context → chỉ một `SELECT` (identity map: `a == b` là `true`).
- JPQL query **luôn** chạy SQL, nhưng các dòng trả về được "resolve" vào entity đã có trong context — trạng thái trong bộ nhớ thắng dữ liệu mới từ DB (tránh ghi đè thay đổi chưa flush). Cần dữ liệu mới: `em.refresh(entity)`.
- Bulk update (`UPDATE Order o SET ...` qua JPQL) bỏ qua persistence context → entity đã load trở nên **stale**. Spring Data: `@Modifying(clearAutomatically = true)`.
- Batch xử lý lớn: phải `flush()` + `clear()` định kỳ, nếu không context phình to → OOM và dirty checking ngày càng chậm.

```java
@Transactional
public void importOrders(List<OrderDto> dtos) {
    int batchSize = 50;  // = hibernate.jdbc.batch_size
    for (int i = 0; i < dtos.size(); i++) {
        em.persist(OrderMapper.toEntity(dtos.get(i)));
        if ((i + 1) % batchSize == 0) {
            em.flush();   // gửi batch INSERT
            em.clear();   // giải phóng persistence context
        }
    }
}
```

> 💡 **Góc nhìn Senior:** persistence context là **unit of work** + **identity map** + **transactional write-behind** (Fowler, *Patterns of Enterprise Application Architecture*). Write-behind là thứ cho phép Hibernate gom SQL thành batch — nhưng cũng là lý do lỗi constraint chỉ xuất hiện lúc commit, cách xa dòng code gây ra. Khi cần bắt lỗi tại chỗ (ví dụ trả 409 khi trùng email), gọi `saveAndFlush` / `em.flush()` trong `try`.

> ⚠️ **Lỗi thường gặp:** catch `DataIntegrityViolationException` quanh `repo.save()` nhưng lỗi không bao giờ bị bắt — vì `INSERT` chỉ chạy khi commit, sau khi method đã return.

### 🛠 Bài tập phần 3

**Bài 3.1 — Quan sát lifecycle (Cơ bản)**
- Đề bài: bật `spring.jpa.show-sql` (hoặc tốt hơn: datasource-proxy / p6spy) và viết test chứng minh: (a) đổi field của entity managed sinh `UPDATE` mà không gọi `save`; (b) `find` hai lần chỉ một `SELECT`; (c) đổi entity detached không sinh SQL.
- Tiêu chí đạt: test assert số câu SQL (ví dụ dùng thư viện `datasource-proxy` hoặc `db-util` của Vlad Mihalcea / `SQLStatementCountValidator`).

**Bài 3.2 — Bẫy thứ tự flush (Trung bình)**
- Đề bài: bảng `coupon(code UNIQUE)`. Trong một transaction: xóa coupon `ABC` và tạo coupon mới `ABC`.
- Tiêu chí đạt: tái hiện lỗi vi phạm unique; giải thích bằng thứ tự `ActionQueue`; sửa bằng 2 cách khác nhau.

**Bài 3.3 — `Persistable` cho id tự gán (Nâng cao)**
- Đề bài: entity `Product` dùng UUID v7 sinh ở constructor. Đo số SQL khi `saveAll` 1.000 sản phẩm trước và sau khi implement `Persistable`.
- Tiêu chí đạt: trước: 1.000 `SELECT` + 1.000 `INSERT`; sau: 0 `SELECT`, `INSERT` được batch.

<details>
<summary>Gợi ý lời giải</summary>

- 3.2: cách 1 `em.remove(old); em.flush(); em.persist(new)`; cách 2 cập nhật entity cũ thay vì xóa-tạo; (cách 3 trên PostgreSQL: constraint `DEFERRABLE INITIALLY DEFERRED`).
- 3.3:
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
Bật `hibernate.jdbc.batch_size=50`, `hibernate.order_inserts=true`.

</details>

---

<a id="4-id-batching"></a>
## 4. Chiến lược sinh ID và batching

### 4.1 Các chiến lược `@GeneratedValue`

| Strategy | Cơ chế | Batch insert? | Ghi chú |
|---|---|---|---|
| `IDENTITY` | Cột auto-increment (`AUTO_INCREMENT`, `GENERATED ... AS IDENTITY`) | **Không** | Hibernate phải `INSERT` **ngay khi persist** để biết id → tắt JDBC batching cho insert, phá write-behind. |
| `SEQUENCE` | Gọi `nextval` của sequence | Có | Lựa chọn tốt nhất trên PostgreSQL/Oracle. Kết hợp optimizer để giảm round-trip. |
| `TABLE` | Bảng giả lập sequence, khóa dòng | Có nhưng chậm | Tránh dùng: row-lock contention, cần transaction riêng. |
| `AUTO` | Để provider chọn | — | Hibernate 5+ trên MySQL chọn `TABLE` khi không có sequence (rất tệ) — nên khai báo rõ ràng. |
| `UUID` (JPA 3.1 `GenerationType.UUID`) | Sinh ở app | Có | UUID v4 ngẫu nhiên gây phân mảnh B-tree index (page split); ưu tiên UUID v7 / ULID (tăng theo thời gian). |

### 4.2 Sequence + pooled optimizer

```java
@Entity
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "payment_seq")
    @SequenceGenerator(name = "payment_seq", sequenceName = "payment_seq", allocationSize = 50)
    private Long id;
}
```
```sql
-- INCREMENT BY phải khớp allocationSize khi dùng pooled optimizer
CREATE SEQUENCE payment_seq START WITH 1 INCREMENT BY 50;
```

`allocationSize = 50` (mặc định JPA là 50) + optimizer **pooled** (mặc định của Hibernate khi allocationSize > 1): mỗi lần gọi `nextval` được giá trị `N`, Hibernate tự cấp phát 50 id trong bộ nhớ → 1 round-trip cho 50 entity. Vì giá trị sequence trong DB là "biên trên", các hệ thống khác (script SQL, app khác) gọi `nextval` trực tiếp vẫn không đụng id → **an toàn khi nhiều writer**, khác với optimizer `hilo` cũ.

> ⚠️ **Lỗi thường gặp:** sequence trong DB `INCREMENT BY 1` nhưng `allocationSize = 50` → Hibernate cấp các id trùng nhau giữa các node → `duplicate key`. Hibernate 6 có kiểm tra schema (`hibernate.id.sequence.increment_size_mismatch_strategy`, mặc định `EXCEPTION`) — nhưng nếu tắt validate thì bug ngầm. Flyway script phải khớp.

> 💡 Id có "lỗ" (gap) sau khi restart app là **bình thường** — đừng dùng id surrogate làm số hóa đơn liên tục. Số hóa đơn liên tục theo luật kế toán cần bảng counter riêng với lock.

### 4.3 Cấu hình batching trong Hibernate

```properties
spring.jpa.properties.hibernate.jdbc.batch_size=50
spring.jpa.properties.hibernate.order_inserts=true     # gom INSERT cùng bảng lại với nhau
spring.jpa.properties.hibernate.order_updates=true
spring.jpa.properties.hibernate.batch_versioned_data=true  # mặc định true từ Hibernate 5; cho phép batch entity có @Version
# PostgreSQL: gộp batch thành multi-values
spring.datasource.hikari.data-source-properties.reWriteBatchedInserts=true
```

Không có `order_inserts`, persist xen kẽ `Order`, `OrderLine`, `Order`, `OrderLine`... sẽ cắt batch liên tục (batch chỉ gom câu lệnh **giống hệt nhau** liên tiếp).

Batch delete/update hàng loạt: dùng bulk JPQL (`DELETE FROM OrderLine l WHERE l.order.id IN :ids`) thay vì load rồi remove từng entity — nhưng nhớ bulk bỏ qua cascade, lifecycle callback, optimistic locking và persistence context.

### 🛠 Bài tập phần 4

**Bài 4.1 — IDENTITY vs SEQUENCE (Cơ bản)**
- Đề bài: cùng một entity, persist 10.000 bản ghi với `IDENTITY` và với `SEQUENCE(allocationSize=50)` trên PostgreSQL.
- Tiêu chí đạt: đếm số câu SQL và số batch thực tế (log của datasource-proxy); giải thích vì sao IDENTITY không batch.

**Bài 4.2 — Mismatch allocationSize (Trung bình)**
- Đề bài: tạo sequence `INCREMENT BY 1`, entity `allocationSize = 50`, chạy 2 instance app cùng insert.
- Tiêu chí đạt: tái hiện (hoặc chứng minh bằng lập luận nếu validate chặn) lỗi trùng khóa; viết Flyway migration sửa an toàn trên dữ liệu đang có.

**Bài 4.3 — Import 1 triệu dòng (Nâng cao)**
- Đề bài: import CSV 1 triệu đơn hàng (mỗi đơn 3 line) qua JPA trong < 60 giây trên máy dev.
- Tiêu chí đạt: dùng SEQUENCE + batch + order_inserts + flush/clear; heap ổn định; so sánh với `COPY` của PostgreSQL / `JdbcTemplate.batchUpdate` và đưa ra khuyến nghị khi nào bỏ JPA.

<details>
<summary>Gợi ý lời giải</summary>

- 4.2: migration: `SELECT setval('payment_seq', (SELECT max(id) FROM payment) + 50); ALTER SEQUENCE payment_seq INCREMENT BY 50;` — chạy khi dừng ghi hoặc chấp nhận gap.
- 4.3: chia thành transaction mỗi 10.000 đơn (tránh transaction khổng lồ và undo/WAL lớn), `flush/clear` mỗi 50. Với ETL thuần, `COPY`/`JdbcTemplate` nhanh hơn nhiều lần — JPA chỉ đáng khi cần logic domain/cascade.

</details>

---
<a id="5-mapping"></a>
## 5. Mapping quan hệ, kế thừa, embeddable

### 5.1 `@ManyToOne` / `@OneToMany` hai chiều, owning side và `mappedBy`
Trong DB, quan hệ một-nhiều chỉ có **một** foreign key (`order_line.order_id`). Trong Java lại có hai tham chiếu (`line.order` và `order.lines`). JPA cần biết phía nào "sở hữu" cột FK — **owning side** — và chỉ thay đổi ở owning side mới được ghi xuống DB.

- Phía `@ManyToOne` (có `@JoinColumn`) là owning side.
- Phía `@OneToMany(mappedBy = "order")` là **inverse side** — chỉ để điều hướng.

```java
@Entity
@Table(name = "orders")
public class Order {
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderLine> lines = new ArrayList<>();

    // Helper methods giữ HAI phía đồng bộ — bắt buộc với quan hệ hai chiều
    public void addLine(OrderLine line) {
        lines.add(line);
        line.setOrder(this);
    }
    public void removeLine(OrderLine line) {
        lines.remove(line);
        line.setOrder(null);
    }
}

@Entity
public class OrderLine {
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)   // đổi mặc định EAGER -> LAZY
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // equals/hashCode: KHÔNG dùng id tự sinh thô (id null trước persist) — xem 5.6
}
```

> ⚠️ **Lỗi thường gặp:**
> 1. Chỉ `order.getLines().add(line)` mà không set `line.setOrder(order)` → `order_id` ghi xuống là `NULL` (inverse side bị bỏ qua).
> 2. `@OneToMany` **một chiều** không có `mappedBy` và không có `@JoinColumn` → Hibernate tạo **bảng join** `orders_lines` thừa, và khi thêm/xóa phần tử sẽ `DELETE` toàn bộ rồi `INSERT` lại. Nếu muốn một chiều, ít nhất thêm `@JoinColumn(name="order_id")` (vẫn sinh thêm `UPDATE` để set FK) — tốt nhất dùng hai chiều hoặc chỉ `@ManyToOne`.
> 3. `@OneToMany` trên collection rất lớn (một user có 1 triệu event) — đừng map; query riêng có phân trang.

### 5.2 Cascade và orphanRemoval
- `cascade = PERSIST/MERGE/REMOVE/REFRESH/DETACH/ALL`: lan truyền thao tác **entity-state** từ cha sang con. Chỉ hợp lý trên quan hệ **cha → con thuộc về cha** (aggregate root → thành phần), gần như không bao giờ trên `@ManyToOne` (xóa một `OrderLine` cascade REMOVE lên `Order` = xóa luôn đơn hàng!).
- `orphanRemoval = true`: con bị gỡ khỏi collection → `DELETE`. Khác `CascadeType.REMOVE` (chỉ xóa con khi xóa cha).

> ⚠️ Với `orphanRemoval`, **không** được gán collection mới: `order.setLines(new ArrayList<>())` → `HibernateException: A collection with cascade="all-delete-orphan" was no longer referenced by the owning entity instance`. Dùng `lines.clear()` + `addAll`.

> 💡 `CascadeType.REMOVE` xóa từng con bằng từng câu `DELETE` (phải load con lên trước). Với hàng nghìn con, dùng bulk delete hoặc `ON DELETE CASCADE` ở DB (`@OnDelete(action = OnDeleteAction.CASCADE)` của Hibernate).

### 5.3 `@ManyToMany` và các cạm bẫy
```java
@Entity
public class Post {
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})  // KHÔNG dùng REMOVE/ALL
    @JoinTable(name = "post_tag",
        joinColumns = @JoinColumn(name = "post_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new HashSet<>();   // Set, không phải List
}
```
Cạm bẫy:
1. **`List` (bag) với `@ManyToMany`:** gỡ một tag → Hibernate xóa **tất cả** dòng của post trong bảng join rồi insert lại phần còn lại. Dùng `Set` (hoặc `@OrderColumn`) để có delete chính xác một dòng.
2. **`CascadeType.REMOVE`/`ALL` trên ManyToMany:** xóa post → xóa luôn Tag đang được post khác dùng.
3. **Bảng join cần thêm cột** (`created_at`, `role`): phải chuyển thành entity trung gian (`PostTag` với `@EmbeddedId` hoặc id riêng) và hai quan hệ `@ManyToOne`. Senior thường **mặc định** dùng entity trung gian vì yêu cầu thêm cột gần như luôn đến.

### 5.4 `@Embeddable` — value object
```java
@Embeddable
public record Money(@Column(name = "amount") BigDecimal amount,
                   @Column(name = "currency", length = 3) String currency) {}
// Records làm embeddable được hỗ trợ từ Hibernate 6.2

@Entity
public class Invoice {
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "total_amount")),
        @AttributeOverride(name = "currency", column = @Column(name = "total_currency"))
    })
    private Money total;
}
```
Embeddable không có identity, không có bảng riêng, sống chết cùng entity chủ → mô hình hóa tốt value object trong DDD. `@ElementCollection` dùng cho collection value object (bảng riêng, nhưng không có lifecycle riêng; cẩn thận: thay đổi một phần tử thường xóa và insert lại toàn bộ).

### 5.5 Chiến lược kế thừa

| Strategy | Cấu trúc bảng | Ưu | Nhược |
|---|---|---|---|
| `SINGLE_TABLE` (mặc định) | Một bảng + cột discriminator | Query đa hình nhanh nhất, không JOIN | Cột của subclass phải nullable → khó dùng `NOT NULL`; bảng "thưa". |
| `JOINED` | Bảng cha + bảng mỗi subclass, PK = FK | Chuẩn hóa, ràng buộc được | Query đa hình JOIN nhiều bảng; insert vào nhiều bảng. |
| `TABLE_PER_CLASS` | Mỗi lớp cụ thể một bảng đủ cột | Không JOIN khi query lớp cụ thể | Query đa hình dùng `UNION ALL`; không dùng được IDENTITY. |
| `@MappedSuperclass` | Không phải kế thừa entity — chỉ chia sẻ field (id, audit) | Đơn giản | Không query đa hình được. |

> 💡 **Góc nhìn Senior:** đa số trường hợp "kế thừa" chỉ là chia sẻ cột audit → `@MappedSuperclass`. Khi thật sự cần đa hình (ví dụ `Payment` → `CardPayment`, `BankTransferPayment`), `SINGLE_TABLE` + check constraint theo discriminator thường là cân bằng tốt nhất cho hiệu năng. Cân nhắc "composition over inheritance" (Design Pattern) trước khi map kế thừa.

### 5.6 `equals`/`hashCode` cho entity
- Không dùng mặc định của Lombok `@Data` (bao cả quan hệ → vòng lặp `StackOverflowError`, kích hoạt lazy load).
- Ưu tiên **business key** bất biến (mã đơn hàng, email). Nếu chỉ có id tự sinh: `equals` so sánh id khi không null, `hashCode` trả về **hằng số theo class** (`getClass().hashCode()`) để giá trị không đổi trước/sau persist — tránh mất phần tử trong `HashSet`.

```java
@Override public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof OrderLine other)) return false;
    return id != null && id.equals(other.getId());
}
@Override public int hashCode() { return getClass().hashCode(); }
```
(Với proxy Hibernate, dùng `Hibernate.getClass(o)` / so sánh qua getter thay vì field.)

### 🛠 Bài tập phần 5

**Bài 5.1 — Aggregate Order (Cơ bản)**
- Đề bài: map `Order` – `OrderLine` hai chiều với helper method, cascade ALL, orphanRemoval.
- Tiêu chí đạt: test: thêm 3 line → 1 INSERT order + 3 INSERT line; xóa 1 line khỏi collection → đúng 1 `DELETE`; xóa order → xóa hết line.

**Bài 5.2 — ManyToMany bag vs set (Trung bình)**
- Đề bài: `Post`–`Tag` với `List` rồi đổi sang `Set`. Gỡ 1 tag khỏi post có 5 tag.
- Tiêu chí đạt: ghi lại SQL ở 2 trường hợp (List: 1 DELETE all + 4 INSERT; Set: 1 DELETE); chuyển sang entity trung gian `PostTag(createdAt)`.

**Bài 5.3 — So sánh kế thừa (Nâng cao)**
- Đề bài: map `Payment` với 3 subclass theo cả 3 strategy; chạy query đa hình `SELECT p FROM Payment p WHERE p.createdAt > :t` trên 1 triệu dòng.
- Tiêu chí đạt: so sánh SQL sinh ra, thời gian, khả năng đặt `NOT NULL`; viết ADR (Architecture Decision Record) chọn strategy.

<details>
<summary>Gợi ý lời giải</summary>

- 5.2: entity trung gian:
```java
@Entity class PostTag {
  @EmbeddedId PostTagId id;
  @ManyToOne(fetch = LAZY) @MapsId("postId") Post post;
  @ManyToOne(fetch = LAZY) @MapsId("tagId")  Tag tag;
  Instant createdAt;
}
@Embeddable record PostTagId(Long postId, Long tagId) implements Serializable {}
```
- 5.3: SINGLE_TABLE: 1 câu SELECT không join; JOINED: LEFT JOIN 3 bảng con; TABLE_PER_CLASS: UNION ALL 3 bảng. NOT NULL cho cột con chỉ dễ ở JOINED/TABLE_PER_CLASS; SINGLE_TABLE dùng `CHECK (type <> 'CARD' OR card_last4 IS NOT NULL)`.

</details>

---

<a id="6-fetching"></a>
## 6. Fetching: LAZY/EAGER, N+1 và các cách sửa

### 6.1 Mặc định của JPA

| Quan hệ | Mặc định | Khuyến nghị |
|---|---|---|
| `@ManyToOne`, `@OneToOne` | **EAGER** | Đổi thành `LAZY` |
| `@OneToMany`, `@ManyToMany`, `@ElementCollection` | LAZY | Giữ LAZY |

Quy tắc của Senior: **mọi quan hệ đều LAZY**, quyết định fetch **theo use case** tại query (JOIN FETCH, EntityGraph, DTO). EAGER là quyết định toàn cục, không tắt được ở từng query (JPQL vẫn phải load EAGER association bằng query phụ → N+1 ẩn).

LAZY được hiện thực bằng **proxy** (`@ManyToOne`: subclass sinh bởi ByteBuddy) hoặc **collection wrapper** (`PersistentBag`, `PersistentSet`). Truy cập lần đầu → query khởi tạo, cần persistence context còn mở.

> ⚠️ `@OneToOne(mappedBy=...)` phía inverse **không thể** lazy bằng proxy (Hibernate không biết có hay không có dòng con để trả `null` hay proxy) → luôn bị load. Giải pháp: dùng `@MapsId` chia sẻ PK và chỉ map phía owning, hoặc bytecode enhancement.

### 6.2 N+1 problem
```java
List<Order> orders = em.createQuery("SELECT o FROM Order o", Order.class).getResultList(); // 1 query
for (Order o : orders) {
    o.getCustomer().getName();   // mỗi order khởi tạo proxy customer -> N query
}
```
Với 500 order → 501 query. Ở môi trường dev với 5 dòng không ai thấy; production thì endpoint 2 giây.

**Phát hiện:** log SQL (`org.hibernate.SQL=debug`), Hibernate Statistics (`hibernate.generate_statistics=true`), datasource-proxy + assertion số query trong test, APM (số span SQL trên một request).

### 6.3 Các cách sửa N+1

**(a) JOIN FETCH**
```java
@Query("SELECT o FROM Order o JOIN FETCH o.customer WHERE o.status = :status")
List<Order> findWithCustomer(@Param("status") Status status);

// Fetch collection: Hibernate 6 tự loại trùng root entity; Hibernate 5 cần DISTINCT
@Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.lines WHERE o.id IN :ids")
List<Order> findWithLines(@Param("ids") List<Long> ids);
```

**(b) `@EntityGraph`** — khai báo association cần fetch, tái sử dụng với query derivation:
```java
@EntityGraph(attributePaths = {"customer", "lines"})
List<Order> findByStatus(Status status);
```

**(c) Batch fetching** — khởi tạo nhiều proxy/collection trong một query `IN (...)`:
```properties
spring.jpa.properties.hibernate.default_batch_fetch_size=50
```
hoặc `@BatchSize(size = 50)` trên association. N+1 thành 1 + ceil(N/50). Ít xâm lấn, hợp làm "lưới an toàn" mặc định.

**(d) DTO projection** — cho màn hình đọc, không cần entity:
```java
public record OrderSummary(Long id, String customerName, BigDecimal total) {}

@Query("""
       SELECT new com.shop.OrderSummary(o.id, c.name, o.total)
       FROM Order o JOIN o.customer c WHERE o.status = :status
       """)
List<OrderSummary> findSummaries(@Param("status") Status status);
```
DTO không vào persistence context → không dirty check, không lazy, chỉ lấy đúng cột cần.

### 6.4 `LazyInitializationException` và Open Session In View
`LazyInitializationException: could not initialize proxy - no Session` xảy ra khi truy cập association LAZY sau khi persistence context đóng — điển hình: service trả entity, controller/Jackson serialize, chạm vào `order.getLines()`.

**Open Session In View (OSIV)** "sửa" bằng cách giữ `EntityManager` mở suốt request (Spring Boot: `spring.jpa.open-in-view=true` **mặc định**, và in cảnh báo `spring.jpa.open-in-view is enabled by default...` khi khởi động).

Vì sao OSIV là **anti-pattern**:
1. **Giữ connection quá lâu**: sau khi transaction service commit, mỗi lazy load ở tầng view/serialize mượn connection lại (ở chế độ auto-commit), kéo dài thời gian chiếm connection trong toàn request → pool cạn dưới tải.
2. **N+1 ẩn ở tầng view**: Jackson serialize một danh sách sẽ âm thầm bắn hàng trăm query, không ai thấy ở service.
3. **Truy cập DB ngoài transaction**: mỗi lazy load là một transaction auto-commit riêng → không nhất quán (đọc dữ liệu của các snapshot khác nhau).
4. **Mờ ranh giới tầng**: tầng web phụ thuộc vào trạng thái persistence.

Cách đúng: `spring.jpa.open-in-view=false`; service trả **DTO** đã đủ dữ liệu (fetch theo use case). Không dùng `hibernate.enable_lazy_load_no_trans=true` — còn tệ hơn (mỗi lazy load mở session + connection mới).

### 6.5 `MultipleBagFetchException`
```java
// ❌ org.hibernate.loader.MultipleBagFetchException: cannot simultaneously fetch multiple bags
SELECT p FROM Post p LEFT JOIN FETCH p.comments LEFT JOIN FETCH p.tags
```
"Bag" = `List` không có `@OrderColumn`. Fetch hai bag cùng lúc tạo **tích Descartes** (post có 50 comment × 20 tag = 1.000 dòng) và Hibernate không thể khử trùng chính xác nên từ chối.

Sửa:
- **Tốt nhất:** tách thành nhiều query trong cùng transaction — query 1 fetch `comments`, query 2 fetch `tags` cho cùng danh sách post; persistence context ghép lại.
- Đổi sang `Set` — hết exception **nhưng vẫn tích Descartes** (dữ liệu trả về bùng nổ). Không phải cách sửa thật.

```java
List<Post> posts = em.createQuery(
    "SELECT DISTINCT p FROM Post p LEFT JOIN FETCH p.comments WHERE p.id IN :ids", Post.class)
    .setParameter("ids", ids).getResultList();
em.createQuery(
    "SELECT DISTINCT p FROM Post p LEFT JOIN FETCH p.tags WHERE p IN :posts", Post.class)
    .setParameter("posts", posts).getResultList();
// posts giờ đã có cả comments và tags được khởi tạo
```

### 6.6 Phân trang với fetch join (HHH000104)
```java
@Query("SELECT o FROM Order o LEFT JOIN FETCH o.lines")
Page<Order> findAll(Pageable pageable);
```
Log: `HHH000104: firstResult/maxResults specified with collection fetch; applying in memory!` (Hibernate 6 có thông báo tương tự với mã `HHH90003004`). Nghĩa là Hibernate **tải toàn bộ** kết quả join rồi cắt trang trong bộ nhớ — vì `LIMIT` trên dòng join sẽ cắt ngang collection. Với bảng lớn → OOM. Bật `hibernate.query.fail_on_pagination_over_collection_fetch=true` để biến cảnh báo thành exception.

Cách đúng — **hai bước**:
```java
@Query(value = "SELECT o.id FROM Order o WHERE o.status = :s",
       countQuery = "SELECT count(o) FROM Order o WHERE o.status = :s")
Page<Long> findIds(@Param("s") Status s, Pageable p);           // 1) phân trang trên id

@Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.lines WHERE o.id IN :ids")
List<Order> findWithLinesByIds(@Param("ids") List<Long> ids);   // 2) fetch theo id (nhớ giữ lại thứ tự sort)
```
Hoặc dùng batch fetching (`default_batch_fetch_size`) với trang chỉ chứa root entity.

> 💡 **Góc nhìn Senior:** chiến lược fetch tổng quát cho dự án: (1) tất cả LAZY, (2) `default_batch_fetch_size` 16–100 làm lưới an toàn, (3) OSIV tắt, (4) màn hình đọc dùng DTO projection, (5) use case ghi load aggregate bằng JOIN FETCH/EntityGraph, (6) test tích hợp assert số câu SQL cho endpoint quan trọng để N+1 không tái xuất hiện.

### 🛠 Bài tập phần 6

**Bài 6.1 — Bắt N+1 bằng test (Cơ bản)**
- Đề bài: endpoint `GET /orders` trả danh sách order kèm tên customer. Viết integration test assert endpoint chỉ chạy tối đa 2 câu SELECT.
- Tiêu chí đạt: test đỏ với code ngây thơ; xanh sau khi sửa bằng JOIN FETCH hoặc DTO.

**Bài 6.2 — Tắt OSIV an toàn (Trung bình)**
- Đề bài: một app có OSIV bật và controller trả entity. Tắt OSIV, sửa mọi `LazyInitializationException` mà không bật `enable_lazy_load_no_trans`.
- Tiêu chí đạt: không còn entity nào đi ra khỏi tầng service; load test chứng minh `hikaricp.connections.usage` giảm.

**Bài 6.3 — Trang 20 post kèm comments và tags (Nâng cao)**
- Đề bài: API phân trang post, mỗi post kèm toàn bộ comments và tags; 100.000 post.
- Tiêu chí đạt: không HHH000104, không MultipleBagFetchException, không tích Descartes; tối đa 4 câu SQL/request (count + ids + comments + tags); giữ đúng thứ tự sort.

<details>
<summary>Gợi ý lời giải</summary>

- 6.1: dùng `datasource-proxy` với `ProxyDataSourceBuilder.countQuery()` và `QueryCountHolder.get(...).getSelect()`; hoặc bật Hibernate statistics `sessionFactory.getStatistics().getPrepareStatementCount()`.
- 6.3: query ids phân trang → `findWithComments(ids)` → `findWithTags(posts)` → sắp xếp lại theo thứ tự ids: `Map<Long,Post> byId; ids.stream().map(byId::get).toList()`.

</details>

---

<a id="7-cache"></a>
## 7. Second-level cache & query cache

### 7.1 Khái niệm
- **First-level cache**: persistence context, phạm vi một `EntityManager`/transaction, luôn bật.
- **Second-level cache (L2C)**: phạm vi `SessionFactory` (toàn ứng dụng/JVM, hoặc phân tán), tắt mặc định. Lưu entity ở dạng **dehydrated** (mảng giá trị), không phải object. Provider: Ehcache 3 / Caffeine qua JCache (JSR-107), Infinispan, Hazelcast.

```properties
spring.jpa.properties.hibernate.cache.use_second_level_cache=true
spring.jpa.properties.hibernate.cache.region.factory_class=jcache
spring.jpa.properties.jakarta.persistence.sharedCache.mode=ENABLE_SELECTIVE
```
```java
@Entity
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Country { ... }
```

### 7.2 Concurrency strategies

| Strategy | Dùng khi | Ghi chú |
|---|---|---|
| `READ_ONLY` | Dữ liệu tham chiếu không bao giờ đổi | Nhanh nhất; update → exception. |
| `NONSTRICT_READ_WRITE` | Hiếm khi đổi, chấp nhận stale ngắn | Invalidate sau commit; có cửa sổ đọc dữ liệu cũ. |
| `READ_WRITE` | Đọc nhiều, ghi thỉnh thoảng | Dùng soft-lock khi đang cập nhật; nhất quán mạnh hơn. |
| `TRANSACTIONAL` | Cache hỗ trợ JTA (Infinispan) | Phức tạp, ít dùng. |

### 7.3 Query cache
`hibernate.cache.use_query_cache=true` + `query.setHint("org.hibernate.cacheable", true)`. Query cache lưu **danh sách id** kết quả cho (câu query + tham số); entity vẫn lấy từ L2C. Khi **bất kỳ** dòng nào của bảng liên quan thay đổi, mọi query cache trên bảng đó bị invalidate (theo timestamp region) → với bảng ghi thường xuyên, hit-rate gần 0 mà chi phí invalidate cao.

> 💡 **Góc nhìn Senior:**
> - L2C hợp với **dữ liệu tham chiếu** (quốc gia, cấu hình, danh mục sản phẩm ít đổi), đọc nhiều. Không hợp với dữ liệu giao dịch.
> - L2C chỉ thấy thay đổi đi **qua Hibernate**. Ai đó update bằng SQL trực tiếp/ứng dụng khác → cache stale. Bulk JPQL/native update khiến Hibernate invalidate cả region.
> - Chạy nhiều instance với cache local → mỗi node một bản khác nhau → cần cache phân tán hoặc invalidation (Infinispan/Hazelcast), hoặc TTL ngắn.
> - Thường thì cache ở tầng service (Spring Cache + Redis, cache DTO) dễ kiểm soát hơn L2C. Xem module Caching/Redis.

### 🛠 Bài tập phần 7

**Bài 7.1 — Bật L2C cho dữ liệu tham chiếu (Cơ bản)**
- Đề bài: bật L2C với Caffeine/Ehcache JCache cho entity `Country`, `Currency` (READ_ONLY).
- Tiêu chí đạt: Hibernate statistics cho thấy `secondLevelCacheHitCount` tăng; lần `find` thứ hai ở transaction khác không sinh SQL.

**Bài 7.2 — Stale cache do SQL ngoài (Trung bình)**
- Đề bài: entity `Product` READ_WRITE trong L2C; chạy `UPDATE product SET price = ...` bằng `psql`. Quan sát app.
- Tiêu chí đạt: giải thích vì sao app đọc giá cũ; đưa ra 3 giải pháp (TTL, evict qua API admin, không cache entity này).

<details>
<summary>Gợi ý lời giải</summary>

- 7.2: evict: `entityManagerFactory.getCache().evict(Product.class, id)`; hoặc `SessionFactory.getCache().evictEntityData(Product.class)`. TTL cấu hình ở provider (Ehcache `expiry`).

</details>

---

<a id="8-query"></a>
## 8. JPQL, Criteria API, native SQL

### 8.1 So sánh

| | JPQL / HQL | Criteria API | Native SQL |
|---|---|---|---|
| Dạng | Chuỗi, hướng entity | Builder type-safe (với static metamodel `Order_`) | SQL thật của DB |
| Kiểm tra lỗi | Lúc khởi động (named query, `@Query` Spring Data được validate khi bootstrap) | Compile time (với metamodel) | Runtime |
| Query động | Kém (nối chuỗi) | Tốt | Kém |
| Tính năng DB | Giới hạn (Hibernate 6 đã hỗ trợ CTE, window function, set operations, `LIMIT`/`OFFSET` trong HQL) | Giới hạn | Đầy đủ: `FOR UPDATE SKIP LOCKED`, `jsonb`, `LATERAL`, recursive CTE, hints |
| Kết quả | Entity, DTO, scalar | Entity, DTO, Tuple | Scalar/Tuple, DTO qua `@SqlResultSetMapping`, hoặc entity nếu đủ cột |

### 8.2 Criteria API cho filter động
```java
public List<Order> search(OrderFilter f) {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Order> cq = cb.createQuery(Order.class);
    Root<Order> o = cq.from(Order.class);
    List<Predicate> ps = new ArrayList<>();
    if (f.status() != null)   ps.add(cb.equal(o.get(Order_.status), f.status()));
    if (f.from() != null)     ps.add(cb.greaterThanOrEqualTo(o.get(Order_.createdAt), f.from()));
    if (f.customer() != null) ps.add(cb.like(cb.lower(o.join(Order_.customer).get(Customer_.name)),
                                             "%" + f.customer().toLowerCase() + "%"));
    cq.where(ps.toArray(Predicate[]::new)).orderBy(cb.desc(o.get(Order_.createdAt)));
    return em.createQuery(cq).setMaxResults(100).getResultList();
}
```
Static metamodel (`Order_`) sinh bởi `hibernate-jpamodelgen` annotation processor — đổi tên field sẽ báo lỗi compile thay vì lỗi runtime.

### 8.3 Native query và khi nào dùng
```java
@Query(value = """
    SELECT date_trunc('day', created_at) AS day, sum(total) AS revenue
    FROM orders WHERE created_at >= :from
    GROUP BY 1 ORDER BY 1
    """, nativeQuery = true)
List<DailyRevenue> revenueSince(@Param("from") Instant from);   // interface projection: getDay(), getRevenue()
```
Dùng native cho: report/analytics phức tạp, tính năng riêng DB, tối ưu query cụ thể. Nhược: gắn chặt vendor, không tự invalidate L2C chính xác, lỗi chỉ thấy lúc runtime → cần integration test với DB thật (Testcontainers), **không** dùng H2 để test native query PostgreSQL.

> 💡 **Góc nhìn Senior:** JPA mạnh cho **ghi** (aggregate, dirty checking, optimistic locking) và đọc đơn giản; với **đọc phức tạp/report**, đừng ép JPQL — dùng native SQL, jOOQ, hoặc `JdbcTemplate`. Kiến trúc CQRS nhẹ: command qua JPA, query qua SQL tối ưu trả DTO.

### 🛠 Bài tập phần 8

**Bài 8.1 — Search động (Cơ bản)**
- Đề bài: viết search đơn hàng với 5 filter tùy chọn bằng Criteria API + metamodel.
- Tiêu chí đạt: filter null bị bỏ qua; đổi tên field entity → lỗi compile.

**Bài 8.2 — Report bằng window function (Trung bình)**
- Đề bài: top 3 sản phẩm bán chạy nhất mỗi danh mục trong tháng — viết bằng native SQL (`ROW_NUMBER() OVER (PARTITION BY ...)`) và thử bằng HQL Hibernate 6.
- Tiêu chí đạt: kết quả trả về DTO; integration test với Testcontainers PostgreSQL.

<details>
<summary>Gợi ý lời giải</summary>

- 8.2: `SELECT * FROM (SELECT p.category_id, p.id, sum(l.qty) s, ROW_NUMBER() OVER (PARTITION BY p.category_id ORDER BY sum(l.qty) DESC) rn FROM order_line l JOIN product p ON ... GROUP BY p.category_id, p.id) t WHERE rn <= 3`. Hibernate 6 HQL hỗ trợ window function và subquery trong `FROM`.

</details>

---
<a id="9-spring-data"></a>
## 9. Spring Data JPA

### 9.1 Repository và query derivation
```java
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    // Derived query: Spring parse tên method -> JPQL
    List<Order> findByCustomerIdAndStatusOrderByCreatedAtDesc(Long customerId, Status status);
    boolean existsByCode(String code);
    long countByStatus(Status status);
    Optional<Order> findFirstByCustomerIdOrderByCreatedAtDesc(Long customerId);

    // Query tường minh khi tên method quá dài hoặc cần JOIN FETCH
    @Query("SELECT o FROM Order o JOIN FETCH o.customer WHERE o.code = :code")
    Optional<Order> findByCodeWithCustomer(@Param("code") String code);
}
```
Bên dưới: Spring tạo **JDK dynamic proxy** cho interface, mặc định ủy quyền tới `SimpleJpaRepository` — class này có sẵn `@Transactional(readOnly = true)` ở mức class và `@Transactional` cho các method ghi (`save`, `delete`...). Vì vậy gọi `repo.save()` ngoài service transaction vẫn chạy trong transaction riêng của nó.

> ⚠️ `findAll()` không phân trang trên bảng lớn; `deleteAll()` load toàn bộ rồi xóa từng entity (dùng `deleteAllInBatch()` hoặc bulk query); `deleteByStatus(...)` derived cũng load trước rồi xóa từng cái.

### 9.2 Projections
```java
// 1) Interface-based closed projection: Spring chỉ SELECT các cột cần
public interface OrderView {
    Long getId();
    String getCode();
    @Value("#{target.customer.name}")  // open projection -> mất tối ưu, load cả entity
    String getCustomerName();
}
List<OrderView> findByStatus(Status status);

// 2) Class/record-based DTO
public record OrderDto(Long id, String code) {}
List<OrderDto> findByCustomerId(Long customerId);

// 3) Dynamic projection
<T> List<T> findByStatus(Status status, Class<T> type);
```
Closed projection (chỉ getter khớp thuộc tính) cho phép Spring sinh `SELECT o.id, o.code`; open projection (`@Value` SpEL) buộc load entity đầy đủ.

### 9.3 Specifications
```java
public final class OrderSpecs {
    public static Specification<Order> hasStatus(Status s) {
        return (root, q, cb) -> s == null ? null : cb.equal(root.get("status"), s);
    }
    public static Specification<Order> createdAfter(Instant t) {
        return (root, q, cb) -> t == null ? null : cb.greaterThan(root.get("createdAt"), t);
    }
}
Page<Order> page = repo.findAll(
    Specification.where(OrderSpecs.hasStatus(st)).and(OrderSpecs.createdAfter(from)),
    PageRequest.of(0, 20, Sort.by("createdAt").descending()));
```
Specification trả `null` = bỏ qua điều kiện. Cẩn thận JOIN trong specification + count query: nếu dùng `root.fetch(...)` thì count query sẽ lỗi — kiểm tra `q.getResultType()` (`Long.class` là count) trước khi fetch.

### 9.4 Pagination: `Page` vs `Slice` vs keyset
- `Page<T>`: thêm một câu **count** — trên bảng lớn với filter phức tạp, count có thể đắt hơn cả query dữ liệu.
- `Slice<T>`: lấy `size + 1` dòng để biết còn trang sau không — không count. Hợp với infinite scroll.
- **OFFSET** sâu (`OFFSET 1000000`) chậm vì DB vẫn phải duyệt qua các dòng bị bỏ. **Keyset/seek pagination** (`WHERE (created_at, id) < (:lastCreated, :lastId) ORDER BY created_at DESC, id DESC LIMIT 20`) ổn định O(log n) với index phù hợp. Spring Data 3.1+ hỗ trợ `ScrollPosition`/`Window<T>` (`KeysetScrollPosition`).

### 9.5 `@Modifying` và bulk operations
```java
@Modifying(clearAutomatically = true, flushAutomatically = true)
@Query("UPDATE Order o SET o.status = :to WHERE o.status = :from AND o.createdAt < :before")
int bulkUpdateStatus(@Param("from") Status from, @Param("to") Status to, @Param("before") Instant before);
```
- Thiếu `@Modifying` → `InvalidDataAccessApiUsageException` (Spring coi là SELECT).
- `flushAutomatically`: flush thay đổi đang chờ trước khi bulk (tránh bulk update đè/bỏ sót).
- `clearAutomatically`: clear persistence context sau bulk để không đọc entity stale — nhưng cũng **bỏ** mọi thay đổi chưa flush (nếu không flushAutomatically).
- Bulk bỏ qua `@Version` (tự tăng version bằng tay: `SET o.version = o.version + 1`), lifecycle callback, cascade, L2C entity cache.

### 9.6 Auditing
```java
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
class JpaConfig {
    @Bean AuditorAware<String> auditorProvider() {
        return () -> Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                             .map(Authentication::getName);
    }
}

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class Auditable {
    @CreatedDate  @Column(updatable = false) private Instant createdAt;
    @LastModifiedDate                        private Instant updatedAt;
    @CreatedBy    @Column(updatable = false) private String createdBy;
    @LastModifiedBy                          private String updatedBy;
}
```
Auditing dựa trên JPA callback → bulk update không cập nhật `updatedAt`. Lịch sử thay đổi đầy đủ (ai đổi gì, giá trị trước/sau): Hibernate Envers (`@Audited`) hoặc CDC (Debezium).

> 💡 **Góc nhìn Senior:** Spring Data rất năng suất nhưng che giấu SQL. Quy tắc đội: (1) bật log SQL ở dev + test đếm query; (2) method derived dài quá 3 điều kiện → chuyển sang `@Query`; (3) repository chỉ trả entity cho use case ghi, trả projection cho use case đọc; (4) không dùng `findAll()` không phân trang; (5) không phụ thuộc `save()` để "update" entity managed.

### 🛠 Bài tập phần 9

**Bài 9.1 — Repository đầy đủ (Cơ bản)**
- Đề bài: viết `OrderRepository` với 3 derived query, 1 `@Query` JOIN FETCH, 1 interface projection, 1 record projection.
- Tiêu chí đạt: test xác nhận closed projection chỉ SELECT đúng cột (xem log SQL).

**Bài 9.2 — Search API với Specification + Slice (Trung bình)**
- Đề bài: `GET /orders?status=&from=&to=&customer=&page=&size=` dùng Specification, trả `Slice` DTO.
- Tiêu chí đạt: không count query; khi filter theo customer có JOIN đúng; không N+1.

**Bài 9.3 — Keyset pagination cho 10 triệu dòng (Nâng cao)**
- Đề bài: so sánh OFFSET và keyset (dùng `ScrollPosition` hoặc tự viết) ở trang thứ 50.000.
- Tiêu chí đạt: `EXPLAIN ANALYZE` cho thấy keyset dùng index scan, thời gian ổn định; xử lý tie-break bằng `id`.

<details>
<summary>Gợi ý lời giải</summary>

- 9.2: dùng `JpaSpecificationExecutor.findBy(spec, q -> q.as(OrderDto.class).sortBy(...).page(...))` (Spring Data 3 fluent API) hoặc tự viết Criteria trả `Slice` bằng `setMaxResults(size + 1)`.
- 9.3: index `(created_at DESC, id DESC)`; điều kiện row-value `(created_at, id) < (?, ?)` hoạt động tốt trên PostgreSQL.

</details>

---

<a id="10-locking"></a>
## 10. Concurrency control: optimistic vs pessimistic locking

### 10.1 Bài toán lost update
Hai người cùng mở form sửa sản phẩm (stock = 10). A trừ 3 → ghi 7. B trừ 2 (vẫn thấy 10) → ghi 8. Kết quả 8 thay vì 5. Isolation `READ COMMITTED` (mặc định của PostgreSQL/Oracle) **không** ngăn được kiểu read-modify-write qua hai transaction như vậy, đặc biệt khi "read" và "write" ở hai request HTTP khác nhau.

### 10.2 Optimistic locking với `@Version`
```java
@Entity
public class Product {
    @Id private Long id;
    private int stock;
    @Version private long version;   // int/long/short/Instant (Timestamp)
}
```
Mọi `UPDATE` do Hibernate sinh ra thêm điều kiện version:
```sql
UPDATE product SET stock = ?, version = 6 WHERE id = ? AND version = 5
```
Nếu 0 dòng bị ảnh hưởng → `OptimisticLockException` (JPA) / `StaleObjectStateException` (Hibernate) → Spring dịch thành `ObjectOptimisticLockingFailureException` (con của `OptimisticLockingFailureException`).

Ứng dụng qua nhiều request (long conversation): gửi `version` cho client (ETag/hidden field), khi submit so sánh — phát hiện thay đổi đồng thời mà không giữ lock.

```java
@Transactional
public void updatePrice(Long id, BigDecimal price, long expectedVersion) {
    Product p = repo.findById(id).orElseThrow();
    if (p.getVersion() != expectedVersion) throw new ConflictException();   // HTTP 409 / 412
    p.setPrice(price);
}

// Retry cho xung đột ngắn (không có tương tác người dùng)
@Retryable(retryFor = ObjectOptimisticLockingFailureException.class, maxAttempts = 3,
           backoff = @Backoff(delay = 50, multiplier = 2, random = true))
@Transactional
public void decreaseStock(Long id, int qty) { ... }
```
> ⚠️ `@Retryable` phải nằm **ngoài** `@Transactional` (retry cả transaction mới). Nếu retry nằm trong transaction đã rollback-only (và persistence context đang giữ entity version cũ) thì vô ích. Khi đặt cả hai annotation trên cùng một method, thứ tự advice phụ thuộc cấu hình `order` — an toàn nhất là để method retry ở một bean gọi sang method `@Transactional` ở bean khác.

`LockModeType.OPTIMISTIC_FORCE_INCREMENT`: tăng version của aggregate root khi chỉ con thay đổi (ví dụ thêm `OrderLine` cũng phải tăng version `Order`).

### 10.3 Pessimistic locking
```java
public interface ProductRepository extends JpaRepository<Product, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)                        // SELECT ... FOR UPDATE
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findForUpdate(@Param("id") Long id);
}
```

| Lock mode | SQL (PostgreSQL) | Ý nghĩa |
|---|---|---|
| `PESSIMISTIC_READ` | `FOR SHARE` | Người khác đọc được, không sửa được |
| `PESSIMISTIC_WRITE` | `FOR UPDATE` (Hibernate có thể dùng `FOR NO KEY UPDATE` tùy dialect/phiên bản) | Độc quyền |
| `PESSIMISTIC_FORCE_INCREMENT` | `FOR UPDATE` + tăng version | |

**Work queue với SKIP LOCKED** — nhiều worker lấy job không tranh nhau:
```sql
SELECT * FROM job WHERE status = 'PENDING'
ORDER BY created_at
LIMIT 10
FOR UPDATE SKIP LOCKED;
```
Trong JPA: `@Lock(PESSIMISTIC_WRITE)` + hint `jakarta.persistence.lock.timeout = -2` (Hibernate hiểu -2 là `SKIP LOCKED`), hoặc native query. Hỗ trợ: PostgreSQL 9.5+, MySQL 8.0+, Oracle.

> 💡 **Góc nhìn Senior — chọn gì?**
> - **Optimistic**: xung đột hiếm, chu trình đọc–sửa dài (qua UI), cần scale đọc; giá phải trả: xử lý exception/retry.
> - **Pessimistic**: xung đột thường xuyên trên cùng dòng (hot row: tồn kho flash sale, số dư ví), chi phí retry lớn; giá phải trả: chờ lock, nguy cơ **deadlock**, giữ connection. Luôn đặt lock timeout và **khóa theo thứ tự nhất quán** (ví dụ chuyển tiền: khóa account id nhỏ trước) để tránh deadlock.
> - **Atomic update** thường tốt nhất cho counter: `UPDATE product SET stock = stock - :q WHERE id = :id AND stock >= :q` — kiểm tra số dòng ảnh hưởng, không cần lock tường minh.
> - Hot row cực nóng (hàng nghìn TPS): chia nhỏ (sharded counter), hàng đợi tuần tự (single writer), hoặc Redis + đối soát.

> ⚠️ **Lỗi thường gặp:** `SELECT ... FOR UPDATE` ngoài transaction (auto-commit) → lock được nhả ngay khi câu lệnh xong — vô tác dụng. Spring Data báo lỗi `TransactionRequiredException` cho `@Lock` ngoài transaction; JDBC thuần thì im lặng.

### 10.4 Isolation levels và anomaly

| Isolation | Dirty read | Non-repeatable read | Phantom | Ghi chú |
|---|---|---|---|---|
| READ UNCOMMITTED | Có thể | Có thể | Có thể | PostgreSQL thực thi như READ COMMITTED |
| READ COMMITTED | Không | Có thể | Có thể | Mặc định PostgreSQL, Oracle, SQL Server |
| REPEATABLE READ | Không | Không | Có thể (theo chuẩn) | Mặc định MySQL InnoDB; PostgreSQL RR = snapshot isolation, không phantom nhưng vẫn có **write skew** |
| SERIALIZABLE | Không | Không | Không | PostgreSQL dùng SSI → có thể ném lỗi serialization (`40001`), app phải retry |

Oracle chỉ hỗ trợ READ COMMITTED và SERIALIZABLE. MySQL InnoDB REPEATABLE READ dùng next-key lock cho locking read để tránh phantom. Đọc thêm chương *Concurrency Control* trong tài liệu PostgreSQL.

### 🛠 Bài tập phần 10

**Bài 10.1 — Tái hiện lost update (Cơ bản)**
- Đề bài: 50 thread cùng trừ stock 1 đơn vị từ 100, không có lock.
- Tiêu chí đạt: stock cuối ≠ 50; thêm `@Version` + retry → đúng 50; ghi lại số lần retry.

**Bài 10.2 — Ba cách chống overselling (Trung bình)**
- Đề bài: cùng bài toán, hiện thực bằng (a) optimistic + retry, (b) `PESSIMISTIC_WRITE`, (c) atomic `UPDATE ... WHERE stock >= :q`. Load test 200 thread.
- Tiêu chí đạt: cả ba đúng; bảng so sánh throughput, p99 latency, số exception; kết luận khi nào chọn cái nào.

**Bài 10.3 — Job queue SKIP LOCKED + deadlock (Nâng cao)**
- Đề bài: (1) hiện thực job queue với 5 worker dùng `FOR UPDATE SKIP LOCKED`, chứng minh không job nào xử lý hai lần; (2) tái hiện deadlock chuyển tiền A→B và B→A đồng thời, sửa bằng khóa theo thứ tự id.
- Tiêu chí đạt: log PostgreSQL `deadlock detected` ở phiên bản lỗi; phiên bản sửa chạy 10.000 giao dịch ngẫu nhiên không deadlock, tổng tiền bảo toàn.

<details>
<summary>Gợi ý lời giải</summary>

- 10.2: kỳ vọng (c) nhanh nhất (1 round-trip, lock ngắn nhất), (b) ổn định nhưng tuần tự hóa trên hot row, (a) tệ dần khi contention tăng (retry storm).
- 10.3: 
```java
@Transactional
public void transfer(long a, long b, BigDecimal amt) {
    long first = Math.min(a, b), second = Math.max(a, b);
    Account x = repo.findForUpdate(first).orElseThrow();
    Account y = repo.findForUpdate(second).orElseThrow();
    Account from = (a == first) ? x : y, to = (a == first) ? y : x;
    from.debit(amt); to.credit(amt);
}
```

</details>

---
<a id="11-transactions"></a>
## 11. Spring Transactions chuyên sâu

### 11.1 Kiến trúc: `PlatformTransactionManager` và proxy
Spring trừu tượng hóa transaction qua `PlatformTransactionManager` (`DataSourceTransactionManager` cho JDBC/MyBatis, `JpaTransactionManager` cho JPA, `JtaTransactionManager` cho XA; reactive có `ReactiveTransactionManager`). Transaction hiện tại (connection/EntityManager) được **gắn vào thread** qua `TransactionSynchronizationManager` (các `ThreadLocal`).

`@Transactional` hoạt động nhờ **AOP proxy** (Proxy pattern — xem `Design Pattern.pdf`):

```
Caller ──► [Proxy của OrderService] ──► TransactionInterceptor
                                          │ 1. getTransaction(definition)  -> mượn connection, setAutoCommit(false)
                                          │ 2. invoke target method (OrderService thật)
                                          │ 3a. exception khớp rollback rule -> rollback
                                          │ 3b. ngược lại -> commit
                                          ▼
                                       OrderService (bean thật)
```
Spring Boot mặc định dùng **CGLIB** proxy (subclass), kể cả khi bean implement interface (`spring.aop.proxy-target-class=true`).

### 11.2 Self-invocation, private/final method — bẫy kinh điển
```java
@Service
public class ReportService {
    public void generateAll() {
        for (long id : ids) {
            generateOne(id);              // ❌ gọi qua "this" -> KHÔNG đi qua proxy -> không có transaction mới
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void generateOne(long id) { ... }
}
```
Các trường hợp `@Transactional` **bị bỏ qua âm thầm**:
1. Gọi nội bộ trong cùng class (`this.method()`).
2. Method `private` (proxy không override được). Với CGLIB, method `final`/`static` cũng không. Spring 6 hỗ trợ method `protected`/package-private với class-based proxy; trước đó chỉ `public` mới được áp dụng.
3. Bean không do Spring quản lý (`new OrderService()`).
4. Gọi trong constructor / `@PostConstruct` (proxy chưa bao quanh hoặc transaction infrastructure chưa sẵn sàng).
5. Exception bị catch và nuốt bên trong method → không có gì để rollback.

Cách sửa self-invocation: tách sang bean khác (sạch nhất); inject chính mình (`@Lazy @Autowired ReportService self`); dùng `TransactionTemplate`; hoặc AspectJ weaving (`mode = AdviceMode.ASPECTJ`).

### 11.3 Propagation — từng loại với kịch bản

| Propagation | Có transaction ngoài | Không có transaction ngoài | Kịch bản điển hình |
|---|---|---|---|
| `REQUIRED` (mặc định) | Tham gia transaction ngoài | Tạo mới | Hầu hết service method |
| `REQUIRES_NEW` | **Tạm treo** transaction ngoài, tạo transaction mới độc lập (connection mới) | Tạo mới | Ghi audit/log lỗi phải còn dù nghiệp vụ rollback; cấp số chứng từ |
| `NESTED` | Tạo **savepoint** trong transaction ngoài; rollback inner chỉ về savepoint | Tạo mới (như REQUIRED) | Import batch: dòng lỗi rollback riêng, các dòng khác vẫn commit cùng nhau |
| `MANDATORY` | Tham gia | **Ném** `IllegalTransactionStateException` | Method domain bắt buộc chạy trong transaction của caller (repository nội bộ) |
| `SUPPORTS` | Tham gia | Chạy không transaction | Method đọc có thể được gọi từ cả hai ngữ cảnh |
| `NOT_SUPPORTED` | Tạm treo, chạy không transaction | Chạy không transaction | Gọi tác vụ dài/không cần transaction (sinh file) từ trong transaction |
| `NEVER` | **Ném** exception | Chạy không transaction | Bảo vệ method gọi API bên ngoài không bao giờ bị bọc trong transaction |

**Kịch bản 1 — REQUIRED và `UnexpectedRollbackException`:**
```java
@Service class OrderService {
    @Transactional
    public void placeOrder(Order o) {
        orderRepo.save(o);
        try {
            loyaltyService.addPoints(o);     // REQUIRED, ném RuntimeException
        } catch (RuntimeException e) {
            log.warn("Loyalty failed, ignore");   // nghĩ rằng đã "nuốt" lỗi
        }
    }   // commit -> UnexpectedRollbackException: Transaction silently rolled back
        //           because it has been marked as rollback-only
}
@Service class LoyaltyService {
    @Transactional
    public void addPoints(Order o) { ...; throw new IllegalStateException(); }
}
```
Vì `addPoints` tham gia cùng transaction vật lý, khi exception đi qua proxy của nó, Spring đánh dấu **rollback-only** toàn bộ transaction. Outer catch exception nhưng không thể "bỏ đánh dấu". Sửa: `addPoints` dùng `REQUIRES_NEW` (nếu điểm thưởng có thể độc lập), hoặc không đặt `@Transactional` ở đó, hoặc xử lý bất đồng bộ sau commit.

**Kịch bản 2 — REQUIRES_NEW cho audit:**
```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void logFailure(String action, String reason) { auditRepo.save(new Audit(action, reason)); }
```
Lưu ý: (1) cần connection thứ hai → nguy cơ pool deadlock (mục 2.2); (2) inner commit trước outer — nếu outer rollback, audit vẫn còn (đó là mục đích), nhưng inner **không thấy** dữ liệu chưa commit của outer (và nếu cùng khóa một dòng với outer → tự deadlock: outer chờ inner, inner chờ lock outer giữ).

**Kịch bản 3 — NESTED:** cần `DataSourceTransactionManager` (JDBC savepoint). Với `JpaTransactionManager`, `nestedTransactionAllowed` mặc định `false`; dù bật, savepoint chỉ áp dụng ở JDBC connection, **không** khôi phục trạng thái persistence context — entity trong bộ nhớ vẫn mang thay đổi đã bị rollback. Vì vậy NESTED với JPA là vùng nguy hiểm.

### 11.4 Isolation trong `@Transactional`
```java
@Transactional(isolation = Isolation.REPEATABLE_READ)
```
Spring gọi `Connection.setTransactionIsolation(...)` khi bắt đầu transaction và khôi phục khi kết thúc. `Isolation.DEFAULT` = dùng mức mặc định của DB. Chỉ có hiệu lực khi **bắt đầu** transaction mới — method `REQUIRED` tham gia transaction đã có thì isolation khai báo bị bỏ qua (đặt `validateExistingTransaction=true` trên transaction manager để Spring báo lỗi khi không khớp). `JpaTransactionManager` hỗ trợ isolation tùy chỉnh thông qua `HibernateJpaDialect`.

### 11.5 Rollback rules
Mặc định Spring **chỉ rollback với `RuntimeException` và `Error`**. **Checked exception → COMMIT** (kế thừa quy ước EJB: checked exception là "kết quả nghiệp vụ dự kiến").

```java
@Transactional
public void importFile(Path p) throws IOException {
    repo.save(header);
    Files.readAllLines(p);   // ném IOException -> transaction VẪN COMMIT header!
}

@Transactional(rollbackFor = Exception.class)          // rollback cả checked
@Transactional(noRollbackFor = BusinessWarning.class)  // ngoại lệ không rollback
```
Spring Framework 6.2 bổ sung tùy chọn toàn cục `@EnableTransactionManagement(rollbackOn = RollbackOn.ALL_EXCEPTIONS)` — kiểm tra phiên bản dự án trước khi dùng. Nhiều đội đặt quy ước: exception nghiệp vụ là unchecked, hoặc luôn `rollbackFor = Exception.class`.

### 11.6 `readOnly = true`
- Với JPA/Hibernate: session được đặt read-only (không giữ snapshot cho entity load) và `FlushMode.MANUAL` → tiết kiệm bộ nhớ/CPU dirty checking.
- Gọi `Connection.setReadOnly(true)` → driver/DB có thể tối ưu (PostgreSQL `SET TRANSACTION READ ONLY` → ghi sẽ lỗi; MySQL tối ưu transaction read-only).
- Có thể dùng để **định tuyến sang read replica**: `AbstractRoutingDataSource` dựa trên `TransactionSynchronizationManager.isCurrentTransactionReadOnly()` + bọc bằng `LazyConnectionDataSourceProxy` (để chọn datasource sau khi transaction đã biết là read-only).
- Không phải "bảo mật": trong Hibernate, gọi `flush()` thủ công vẫn có thể ghi (nếu DB không chặn). Và lưu ý replication lag: đọc-sau-ghi trên replica có thể không thấy dữ liệu mới.

### 11.7 Transaction dài và gọi hệ thống bên ngoài
```java
@Transactional
public void checkout(Cart cart) {
    Order o = orderRepo.save(Order.from(cart));
    paymentClient.charge(o);          // ❌ HTTP 2–30 giây trong khi giữ connection + lock
    emailClient.sendConfirmation(o);  // ❌ email đã gửi nhưng transaction có thể rollback sau đó
}
```
Vấn đề: (1) connection bị giữ suốt thời gian gọi mạng → pool cạn khi đối tác chậm; (2) lock DB bị giữ lâu → contention; (3) **không nhất quán**: payment thành công nhưng commit DB thất bại (hoặc ngược lại), email gửi cho đơn hàng không tồn tại.

Thiết kế đúng:
- Tách thành các bước transaction ngắn: tạo order `PENDING` (tx1) → gọi payment ngoài transaction (có idempotency key) → cập nhật `PAID` (tx2); kèm job đối soát cho trạng thái treo.
- Side-effect sau commit: `@TransactionalEventListener(phase = AFTER_COMMIT)` hoặc **Transactional Outbox** (ghi event vào bảng outbox trong cùng transaction, relay đẩy sang Kafka).
- Đặt `@Transactional(timeout = 5)` làm lưới an toàn.

### 11.8 Programmatic: `TransactionTemplate`
```java
@Service
@RequiredArgsConstructor
public class ImportService {
    private final TransactionTemplate tx;   // Spring Boot tự cấu hình bean TransactionTemplate
    private final RowRepository repo;

    public ImportResult importRows(List<Row> rows) {
        int ok = 0, failed = 0;
        for (List<Row> chunk : Lists.partition(rows, 500)) {   // Guava
            try {
                tx.executeWithoutResult(status -> repo.saveAll(chunk.stream().map(Row::toEntity).toList()));
                ok += chunk.size();
            } catch (DataAccessException e) {
                failed += chunk.size();   // chunk lỗi rollback riêng, chunk khác không ảnh hưởng
            }
        }
        return new ImportResult(ok, failed);
    }
}
```
Dùng khi: ranh giới transaction phụ thuộc vòng lặp/điều kiện, tránh self-invocation, cần transaction trong code không phải bean. Có thể tạo `TransactionTemplate` riêng với propagation/isolation/timeout khác. `status.setRollbackOnly()` để rollback mà không ném exception.

### 11.9 `@TransactionalEventListener`
```java
@Service
@RequiredArgsConstructor
class OrderService {
    private final ApplicationEventPublisher events;
    @Transactional
    public void place(Order o) {
        repo.save(o);
        events.publishEvent(new OrderPlaced(o.getId()));   // chưa xử lý ngay
    }
}

@Component
class OrderNotifier {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)  // mặc định là AFTER_COMMIT
    public void on(OrderPlaced e) { emailClient.send(e.orderId()); }
}
```
Chi tiết quan trọng:
- Các phase: `BEFORE_COMMIT`, `AFTER_COMMIT`, `AFTER_ROLLBACK`, `AFTER_COMPLETION`.
- Không có transaction đang chạy → listener **không được gọi** trừ khi `fallbackExecution = true`.
- Trong `AFTER_COMMIT`, transaction đã commit nhưng resource vẫn gắn với thread: ghi DB ở đây với `REQUIRED` sẽ **không được commit**. Phải dùng `@Transactional(propagation = REQUIRES_NEW)`; Spring 6.1+ còn kiểm tra và từ chối listener khai báo `@Transactional` không phải `REQUIRES_NEW`/`NOT_SUPPORTED`.
- Exception trong listener AFTER_COMMIT không rollback được gì và mặc định chỉ được log/lan ra caller sau commit → nếu side-effect quan trọng (gửi event Kafka), app crash ngay sau commit sẽ **mất** event. Đó là lý do cần **outbox** cho đảm bảo at-least-once.
- Kết hợp `@Async` để không chặn thread request.

> 💡 **Góc nhìn Senior — checklist review `@Transactional`:**
> 1. Đặt ở tầng service (use case), không ở controller, không rải trên repository tùy tiện.
> 2. Không có I/O mạng bên trong.
> 3. Checked exception có cần rollback không?
> 4. Có gọi nội bộ (self-invocation) không?
> 5. Có `REQUIRES_NEW` lồng nhau → tính lại pool size?
> 6. Method đọc có `readOnly = true`?
> 7. Có catch-và-nuốt exception từ method `@Transactional` khác (→ UnexpectedRollbackException)?
> 8. Test có `@Transactional` trên class test → rollback sau mỗi test, che giấu lỗi chỉ xảy ra lúc commit/flush (constraint, lazy loading). Có ít nhất vài test không bọc transaction.

### 🛠 Bài tập phần 11

**Bài 11.1 — Bộ test propagation (Cơ bản)**
- Đề bài: viết test cho cả 7 loại propagation, mỗi loại một kịch bản có và không có transaction ngoài.
- Tiêu chí đạt: assert bằng `TransactionSynchronizationManager.isActualTransactionActive()` và `getCurrentTransactionName()`; test MANDATORY/NEVER assert đúng exception.

**Bài 11.2 — Debug UnexpectedRollbackException và checked exception (Trung bình)**
- Đề bài: tái hiện (a) kịch bản 1 ở mục 11.3; (b) method ném `IOException` vẫn commit; (c) self-invocation làm `REQUIRES_NEW` mất tác dụng.
- Tiêu chí đạt: mỗi lỗi có test đỏ → sửa → test xanh; giải thích bằng cơ chế proxy và rollback-only.

**Bài 11.3 — Checkout không giữ transaction qua HTTP (Nâng cao)**
- Đề bài: refactor `checkout` (mục 11.7) với WireMock giả lập payment chậm 5 giây; pool 10 connection; 100 request đồng thời.
- Tiêu chí đạt: phiên bản cũ có timeout lấy connection; phiên bản mới không; trạng thái nhất quán khi payment lỗi/timeout (order `PAYMENT_FAILED` hoặc được đối soát); email chỉ gửi sau commit.

<details>
<summary>Gợi ý lời giải</summary>

- 11.1: ví dụ NESTED cần `DataSourceTransactionManager` hoặc bật `jpaTransactionManager.setNestedTransactionAllowed(true)` và chỉ dùng JDBC bên trong.
- 11.2 (c): bật log `logging.level.org.springframework.transaction.interceptor=TRACE` để thấy "Getting transaction for [...]" — với self-invocation sẽ không có dòng này cho method bên trong.
- 11.3: luồng: `tx1: createPending()` → `paymentClient.charge(orderId, idempotencyKey)` (timeout 3 s, circuit breaker) → `tx2: markPaid()/markFailed()`; publish `OrderPaid` trong tx2 → listener AFTER_COMMIT gửi email (hoặc outbox). Job đối soát các order `PENDING` quá 5 phút bằng API truy vấn trạng thái payment.

</details>

---

<a id="12-distributed-mybatis-migration"></a>
## 12. Transaction phân tán, MyBatis, schema migration

### 12.1 Transaction phân tán: 2PC/XA vs Outbox
Khi một thao tác chạm hai resource (DB + message broker, hoặc hai DB):
- **2PC / XA** (`JtaTransactionManager` + Atomikos/Narayana): coordinator yêu cầu các resource *prepare* rồi *commit*. Nhược: blocking khi coordinator chết giữa chừng (resource giữ lock ở trạng thái in-doubt), latency cao, nhiều hệ thống hiện đại (Kafka, phần lớn NoSQL, REST API) không hỗ trợ XA. Hiếm dùng trong microservices.
- **Transactional Outbox**: ghi thay đổi nghiệp vụ + bản ghi event vào bảng `outbox` trong **cùng một transaction cục bộ**; một relay (polling hoặc CDC như Debezium) đọc outbox và publish lên broker → at-least-once, consumer phải **idempotent**.
- **Saga** (choreography/orchestration) với compensating transaction cho quy trình nhiều service.

Chi tiết các pattern này thuộc module Microservices / Messaging — ở đây cần nắm: **không bao giờ** giả định `@Transactional` bao được cả `kafkaTemplate.send()` lẫn DB một cách nguyên tử (trừ cấu hình đặc biệt và vẫn chỉ là best-effort đồng bộ hóa).

### 12.2 MyBatis — so sánh ngắn

| Tiêu chí | JPA/Hibernate | MyBatis |
|---|---|---|
| Mô hình | ORM: map object ↔ bảng, quản lý trạng thái | SQL mapper: bạn viết SQL, MyBatis map kết quả |
| Kiểm soát SQL | Gián tiếp (cần hiểu sâu để tránh N+1, cartesian) | Hoàn toàn |
| Dirty checking, cache L1/L2, lazy | Có | Không dirty checking; có cache session/namespace đơn giản; lazy loading tùy chọn |
| Phù hợp | Domain model phong phú, CRUD nhiều, write-heavy với aggregate | Query phức tạp, report, DB legacy schema "lạ", đội mạnh SQL (phổ biến ở ngân hàng VN) |
| Rủi ro | Ma thuật ẩn, hiệu năng bất ngờ | Nhiều SQL lặp lại, mapping thủ công, `${}` gây SQL injection (phải dùng `#{}`) |

```xml
<!-- MyBatis mapper: #{} = PreparedStatement parameter; ${} = nối chuỗi (nguy hiểm) -->
<select id="findByStatus" resultType="com.shop.OrderDto">
  SELECT id, code, total FROM orders WHERE status = #{status} ORDER BY created_at DESC
</select>
```
MyBatis dùng chung `DataSourceTransactionManager` của Spring nên `@Transactional` hoạt động như JDBC. Một dự án có thể dùng cả hai (JPA cho ghi, MyBatis/jOOQ cho đọc) nếu dùng chung DataSource và transaction manager phù hợp (`JpaTransactionManager` cũng expose JDBC connection cho code JDBC trong cùng transaction).

### 12.3 Schema migration với Flyway / Liquibase
**Không bao giờ** dùng `spring.jpa.hibernate.ddl-auto=update` ở production: không xóa cột, không đổi kiểu an toàn, không có lịch sử, không review được. Production nên `validate` hoặc `none`.

**Flyway:** file SQL có version `V1__init.sql`, `V2__add_order_status.sql`, `R__views.sql` (repeatable). Lịch sử trong bảng `flyway_schema_history` (version, checksum). Sửa file đã chạy → checksum mismatch → app không khởi động.

**Liquibase:** changelog (XML/YAML/SQL) gồm các `changeSet` (id + author), bảng `DATABASECHANGELOG`; hỗ trợ rollback khai báo, precondition, context/labels, trừu tượng hóa đa DB.

**Zero-downtime migration (expand/contract)** — vì khi rolling deploy, phiên bản cũ và mới chạy song song trên cùng schema:
1. *Expand*: thêm cột mới nullable / bảng mới (tương thích với code cũ).
2. Deploy code ghi cả hai (cột cũ + mới); backfill dữ liệu theo lô nhỏ.
3. Deploy code đọc cột mới.
4. *Contract*: xóa cột cũ ở release sau.

> ⚠️ **Lỗi thường gặp:**
> - `ALTER TABLE ... ADD COLUMN ... NOT NULL DEFAULT ...` hay tạo index trên bảng lớn khóa bảng lâu (tùy DB/phiên bản). PostgreSQL: `CREATE INDEX CONCURRENTLY` (không chạy được trong transaction → Flyway cần tách script / cấu hình không transactional); MySQL: online DDL / gh-ost / pt-online-schema-change.
> - Đổi tên cột trực tiếp → phiên bản app cũ đang chạy lỗi ngay.
> - Nhiều instance khởi động cùng lúc chạy migration: Flyway/Liquibase dùng lock (bảng lock/advisory lock) — nhưng migration dài làm các pod kẹt ở startup và bị liveness probe kill. Cân nhắc chạy migration như một Job riêng trước deploy.

### 🛠 Bài tập phần 12

**Bài 12.1 — Flyway cơ bản (Cơ bản)**
- Đề bài: chuyển một dự án từ `ddl-auto=update` sang Flyway: baseline schema hiện tại, thêm 2 migration, `ddl-auto=validate`.
- Tiêu chí đạt: app khởi động trên DB rỗng và trên DB đã có dữ liệu (dùng `baselineOnMigrate`); Testcontainers test chạy toàn bộ migration.

**Bài 12.2 — Đổi tên cột không downtime (Trung bình)**
- Đề bài: đổi `customer.fullname` → `customer.display_name` theo expand/contract trong 3 release.
- Tiêu chí đạt: viết migration + code cho từng release; chứng minh tại mỗi thời điểm, phiên bản N và N+1 đều chạy đúng.

**Bài 12.3 — Outbox tối giản (Nâng cao)**
- Đề bài: khi tạo order, ghi bảng `outbox(id, aggregate_id, type, payload jsonb, created_at, published_at)` trong cùng transaction; một scheduler đọc bằng `FOR UPDATE SKIP LOCKED` và publish (giả lập) rồi set `published_at`.
- Tiêu chí đạt: kill app giữa chừng không mất event; chạy 2 instance không publish trùng trong điều kiện bình thường; consumer idempotent theo `id`.

<details>
<summary>Gợi ý lời giải</summary>

- 12.2: R1: `ADD COLUMN display_name`, code ghi cả hai, đọc cũ; backfill `UPDATE ... WHERE display_name IS NULL` theo lô. R2: đọc mới, vẫn ghi cả hai. R3: ngừng ghi cũ, `DROP COLUMN fullname`.
- 12.3: relay: `@Scheduled(fixedDelay=500) @Transactional` → `SELECT ... WHERE published_at IS NULL ORDER BY created_at LIMIT 100 FOR UPDATE SKIP LOCKED` → publish → `UPDATE published_at`. Publish thành công nhưng crash trước khi update → gửi lại (at-least-once) → consumer dedupe.

</details>

---

<a id="du-an-mini"></a>
## Dự án mini

### "Mini e-commerce order service" — chịu tải và đúng dữ liệu

**Bối cảnh:** xây dựng service quản lý đơn hàng cho một cửa hàng trực tuyến có flash sale. Stack: Spring Boot 3, Spring Data JPA (Hibernate 6), PostgreSQL, HikariCP, Flyway, Testcontainers.

**Yêu cầu chức năng**
1. Danh mục: `Category`, `Product` (giá, tồn kho, `@Version`), `Tag` (many-to-many qua entity trung gian).
2. Đơn hàng: `Order` (aggregate root) – `OrderLine` hai chiều, cascade + orphanRemoval; `Money` embeddable; auditing `createdAt/createdBy/updatedAt`.
3. API:
   - `POST /orders` — đặt hàng nhiều sản phẩm, trừ tồn kho **không bao giờ âm** khi 500 request đồng thời mua cùng sản phẩm.
   - `GET /orders?status&from&to&customer&cursor` — tìm kiếm với Specification, phân trang **keyset**, trả DTO.
   - `GET /orders/{id}` — chi tiết kèm lines và product name, tối đa 2 câu SQL.
   - `POST /orders/{id}/pay` — gọi payment gateway giả lập (WireMock, chậm ngẫu nhiên 0–3 s, lỗi 10%) **không** giữ transaction qua lời gọi HTTP; idempotent theo `Idempotency-Key`.
   - `POST /products/import` — import CSV 200.000 sản phẩm.
4. Sau khi đơn được thanh toán: publish event `OrderPaid` qua outbox; một worker giả lập gửi email sau commit.
5. Bảng `Country`/`Currency` dùng second-level cache READ_ONLY.

**Yêu cầu phi chức năng**
- `spring.jpa.open-in-view=false`, `ddl-auto=validate`, schema hoàn toàn bằng Flyway.
- ID dùng SEQUENCE + pooled optimizer; `hibernate.jdbc.batch_size`, `order_inserts` bật; import 200.000 dòng < 30 s trên máy dev.
- HikariCP: pool size có tính toán và ghi lý do; `connectionTimeout` ≤ 3 s; leak detection bật ở profile staging; metrics qua Actuator/Prometheus.
- Không N+1: mỗi endpoint đọc có integration test assert số câu SQL.
- Không có `@Transactional` bao I/O mạng; mọi method đọc `readOnly = true`; rollback rule rõ ràng cho checked exception.
- Load test (k6/Gatling): 500 request đồng thời `POST /orders` trên cùng sản phẩm tồn kho 100 → đúng 100 đơn thành công, tồn kho = 0, không deadlock.

**Tiêu chí chấm (100 điểm)**

| Hạng mục | Điểm |
|---|---|
| Mapping đúng (owning side, helper method, equals/hashCode, cascade hợp lý, embeddable, entity trung gian) | 15 |
| Fetching: không N+1, không HHH000104, không MultipleBagFetchException, có test đếm SQL | 15 |
| Concurrency: chống oversell đúng dưới tải, giải thích lựa chọn optimistic/pessimistic/atomic | 15 |
| Transaction design: ranh giới đúng, không giữ transaction qua HTTP, outbox + AFTER_COMMIT, idempotency | 20 |
| Hiệu năng: batching import, keyset pagination, pool sizing có số liệu | 15 |
| Vận hành: Flyway (có ít nhất 1 migration expand/contract), metrics Hikari, leak detection, cấu hình production-ready | 10 |
| Tài liệu: README với sơ đồ, ADR cho 3 quyết định quan trọng, kết quả load test | 10 |

---

<a id="checklist-tu-danh-gia"></a>
## Checklist tự đánh giá
- [ ] Tôi giải thích được vì sao `PreparedStatement` chống SQL injection và những phần nào của câu SQL nó **không** tham số hóa được.
- [ ] Tôi cấu hình được JDBC batch cho MySQL/PostgreSQL và biết khi nào driver load toàn bộ `ResultSet` vào bộ nhớ.
- [ ] Tôi tính được pool size cho nhiều instance, biết công thức tránh pool deadlock và ý nghĩa `connectionTimeout`, `maxLifetime`, `keepaliveTime`, `leakDetectionThreshold`.
- [ ] Tôi vẽ được sơ đồ 4 trạng thái entity và giải thích khác biệt `persist`/`merge`, vì sao `save()` với id tự gán sinh thêm `SELECT`.
- [ ] Tôi giải thích được dirty checking, flush vs commit, các FlushMode và thứ tự SQL khi flush.
- [ ] Tôi biết vì sao `IDENTITY` tắt batch insert và cấu hình được SEQUENCE + pooled optimizer khớp với DB.
- [ ] Tôi map đúng quan hệ hai chiều (owning side, `mappedBy`, helper method) và biết cạm bẫy `@ManyToMany` với `List`, `CascadeType.REMOVE`, `orphanRemoval`.
- [ ] Tôi so sánh được 3 chiến lược kế thừa và chọn có lý do.
- [ ] Tôi phát hiện và sửa được N+1 bằng ít nhất 4 cách, giải thích được HHH000104 và MultipleBagFetchException.
- [ ] Tôi lập luận được vì sao Open Session In View là anti-pattern và cách tắt nó an toàn.
- [ ] Tôi biết khi nào nên/không nên dùng second-level cache và query cache.
- [ ] Tôi chọn đúng giữa JPQL, Criteria, native SQL, Spring Data projection, và biết khi nào bỏ JPA cho query đọc.
- [ ] Tôi dùng thành thạo Spring Data: derivation, `@Query`, projections, Specification, `Page`/`Slice`/keyset, `@Modifying`, auditing.
- [ ] Tôi hiện thực được optimistic locking có retry, pessimistic locking có timeout, `SKIP LOCKED` queue, và tránh deadlock bằng thứ tự khóa.
- [ ] Tôi nêu được bảng isolation level, anomaly và mặc định của PostgreSQL/MySQL/Oracle.
- [ ] Tôi giải thích được cơ chế proxy của `@Transactional`, liệt kê 5 trường hợp nó bị bỏ qua âm thầm.
- [ ] Tôi đưa ra được kịch bản thực tế cho cả 7 loại propagation và giải thích `UnexpectedRollbackException`.
- [ ] Tôi nhớ rollback rule mặc định (checked exception commit) và cách thay đổi.
- [ ] Tôi thiết kế được luồng nghiệp vụ gọi hệ thống ngoài mà không giữ transaction dài, dùng `TransactionTemplate`, `@TransactionalEventListener`, outbox.
- [ ] Tôi so sánh được 2PC/XA với outbox/saga, JPA với MyBatis.
- [ ] Tôi viết được migration Flyway/Liquibase theo expand/contract để deploy không downtime.

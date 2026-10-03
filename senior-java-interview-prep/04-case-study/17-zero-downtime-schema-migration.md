# Case 17 — Đổi tên và tách cột trên bảng 200 triệu dòng không downtime trong lúc rolling deploy

> **Chủ đề:** Expand/contract (parallel change), dual write, backfill theo lô, online DDL (MySQL/PostgreSQL/Oracle), metadata lock, gh-ost, Flyway, replication lag, CDC, kế hoạch rollback
> **Module liên quan:** [M11 §13 — Migration dữ liệu không downtime](../01-giao-trinh/11-database-sql.md#phan-13) · [M09 §12 — Schema migration với Flyway/Liquibase](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#12-distributed-mybatis-migration) · [M11 §8 — Locking](../01-giao-trinh/11-database-sql.md#phan-8) · [M11 §10 — Replication & connection limits](../01-giao-trinh/11-database-sql.md#phan-10) · [M14 §8 — Triển khai an toàn & tiến hóa API](../01-giao-trinh/14-microservices-system-design.md#p8) · [M16 §7 — Rolling update & tương thích ngược](../01-giao-trinh/16-devops-build-cloud-security.md#p7) · [M13 §10 — CDC](../01-giao-trinh/13-messaging-kafka-rabbitmq.md#p10)
> **Độ khó:** ⭐⭐⭐⭐⭐ (Senior — bài thiết kế & kế hoạch)
> **Thời gian tự giải gợi ý:** 50 phút

---

## 1. Bối cảnh hệ thống

`customer-service` (minh họa) của một ví điện tử, sở hữu bảng `customer`.

```
customer-service (30 pod, Boot 3.2, JPA/Hibernate 6, Flyway chạy lúc startup)
        │
MySQL 8.0.36 primary ──► replica ×2 (đọc báo cáo, failover)
        │ binlog ROW
        └──► Debezium ──► Kafka "customerdb.customer" ──► data warehouse, search-indexer, fraud-service
```

| Thông số | Giá trị |
|---|---|
| Bảng `customer` | **200 triệu dòng**, ~180 GB (data + index), PK `id BIGINT AUTO_INCREMENT` |
| Ghi | ~3.000 write/s (đăng ký, cập nhật hồ sơ, lần đăng nhập cuối) |
| Đọc | ~15.000 read/s (phần lớn theo PK, qua cache) |
| Rolling deploy | 30 pod, `maxSurge=25%`, mất ~12 phút → **phiên bản cũ và mới cùng chạy** |
| SLO replica lag | < 5 s |

Yêu cầu nghiệp vụ (cho tính năng đăng nhập bằng số điện thoại và eKYC):

1. Đổi `mobile VARCHAR(20) NOT NULL` (lưu lộn xộn: `0912 345 678`, `84912345678`, `+84-912-345-678`) thành `phone_e164 VARCHAR(16)` đã chuẩn hóa (`+84912345678`), có **unique index**.
2. Tách `full_name VARCHAR(200)` thành `family_name` và `given_name` (ví dụ "Nguyễn Văn An" → `Nguyễn` / `Văn An`).

---

## 2. Hiện trạng

Sự cố 6 tháng trước, khi một release chứa migration Flyway:

```sql
-- V37__rename_email.sql
ALTER TABLE customer RENAME COLUMN email TO email_address;
```

Diễn biến:

```
14:02:10 Pod mới đầu tiên khởi động, Flyway chạy V37
14:02:10 ALTER chờ metadata lock (MDL) vì một query báo cáo dài 6 phút đang chạy trên primary
14:02:11 → mọi SELECT/UPDATE mới trên customer xếp hàng SAU yêu cầu MDL exclusive của ALTER
14:02:40 HikariPool: Connection is not available, request timed out after 30000ms — toàn bộ 30 pod
14:08:02 Query báo cáo xong, ALTER chạy (rename là thao tác metadata, tức thì), hàng đợi giải phóng
14:08:03 29 pod cũ: java.sql.SQLSyntaxErrorException: Unknown column 'c1_0.email' in 'field list'
14:20:15 Rolling deploy xong, lỗi hết. Tổng: ~18 phút gián đoạn đăng nhập/đăng ký
```

Hai bài học: (1) DDL "tức thì" vẫn cần MDL exclusive và có thể chặn dây chuyền; (2) đổi tên cột trong một bước phá vỡ phiên bản app đang chạy song song.

Các ràng buộc vận hành khác: Flyway chạy lúc startup trên mọi pod; job batch (đối soát, gửi email) và 2 script vận hành ghi trực tiếp vào bảng; data warehouse map cột theo tên.

---

## 3. Ràng buộc & câu hỏi đặt ra

**Ràng buộc**

1. Không downtime; mỗi bước phải tương thích với phiên bản app trước **và** sau.
2. Replica lag < 5 s; Debezium và các consumer downstream không được vỡ.
3. Mọi release phải rollback được trong 1 giờ **bằng cách deploy lại code cũ**, không rollback schema.
4. Backfill không được làm tăng p99 ghi quá 20%.

**Câu hỏi đặt ra**

- Chia việc thành bao nhiêu release, mỗi release thay đổi schema gì và code gì?
- Backfill 200 triệu dòng thế nào để không gây lag, không ghi đè dữ liệu mới, và chạy lại được khi dừng giữa chừng?
- Tạo unique index trên bảng 180 GB thế nào? Khác gì giữa MySQL, PostgreSQL, Oracle?
- Rollback ở từng bước ra sao? Khi nào là "điểm không quay lại"?

> ✋ **Dừng lại và tự lập bảng các release** (cột: schema, code ghi, code đọc, điều kiện chuyển bước, rollback) trước khi đọc tiếp.

---

## 4. Phương án

| Phương án | Mô tả | Ưu | Nhược | Đánh giá |
|---|---|---|---|---|
| A. ALTER trực tiếp (rename + đổi kiểu) | Một migration, một release | Nhanh | Phá app cũ khi rolling; đổi kiểu = rebuild bảng 180 GB, lag replica hàng giờ | ❌ |
| B. Expand/contract, **dual write trong app** | Thêm cột → app ghi cả hai → backfill → đọc cột mới → bỏ cột cũ | Logic chuẩn hóa ở Java (dễ test), kiểm soát tốt | Mọi đường ghi phải được cập nhật (batch, script) | ✅ **Chọn** |
| C. Expand/contract, **trigger đồng bộ** | Trigger `BEFORE INSERT/UPDATE` tính cột mới | Bắt mọi đường ghi | Logic chuẩn hóa tên/điện thoại trong SQL khó; trigger tăng chi phí ghi; xung đột với công cụ online DDL dùng trigger (pt-osc) | Dùng làm lưới an toàn nếu không kiểm soát được mọi writer |
| D. Bảng mới + đồng bộ (gh-ost/CDC) | Tạo bảng `customer_v2`, đồng bộ, cut-over | Thay đổi cấu trúc lớn cùng lúc | Phức tạp nhất, cut-over rủi ro, FK/consumer phải đổi | Quá mức cho yêu cầu này |

Cách xử lý DDL nặng trên từng hệ quản trị:

| Thao tác | MySQL 8.0 | PostgreSQL 12+ | Oracle 19c |
|---|---|---|---|
| Thêm cột nullable | `ALGORITHM=INSTANT` (8.0.12+; vị trí bất kỳ từ 8.0.29) — vẫn cần MDL exclusive ngắn | Metadata-only (default không volatile cũng instant từ PG 11) — cần `ACCESS EXCLUSIVE` ngắn | Metadata-only |
| Tạo index lớn | Online DDL `ALGORITHM=INPLACE, LOCK=NONE` cho phép DML, **nhưng replica áp DDL sau khi primary xong → lag bằng thời gian build**; dùng **gh-ost**/pt-osc | `CREATE INDEX CONCURRENTLY` (ngoài transaction; lỗi giữa chừng để lại index `INVALID` cần drop) | `CREATE INDEX ... ONLINE` |
| Unique/NOT NULL trên dữ liệu có sẵn | Build index cũng là bước kiểm tra trùng | `CREATE UNIQUE INDEX CONCURRENTLY`; NOT NULL qua `CHECK ... NOT VALID` → `VALIDATE` | `ENABLE NOVALIDATE` → `VALIDATE` |
| Đổi tên cột | Metadata nhưng phá app cũ | Metadata nhưng phá app cũ | Metadata; có **Edition-Based Redefinition** cho phép app cũ/mới thấy "phiên bản" schema khác nhau |
| Xóa cột | `ALGORITHM=INSTANT` từ 8.0.29, trước đó rebuild | Metadata-only (đánh dấu dropped) | `SET UNUSED` (nhanh) rồi `DROP UNUSED COLUMNS` lúc thấp điểm |
| Chống chờ lock dây chuyền | `SET SESSION lock_wait_timeout = 5` | `SET lock_timeout = '3s'` | `DDL_LOCK_TIMEOUT` |

---

## 5. Kế hoạch triển khai từng bước

| Release | Schema | Code ghi | Code đọc | Điều kiện sang bước sau |
|---|---|---|---|---|
| R0 | — (hạ tầng) | — | — | Flyway tách thành K8s Job; dashboard sẵn sàng |
| R1 Expand | Thêm 3 cột nullable | Cũ | Cũ | Migration xong, không lỗi |
| R2 Dual write | — | **Cả hai** | Cũ | 100% pod là R2; mọi writer khác đã cập nhật |
| (Job) Backfill | Tạo unique index sau backfill | — | — | 0 dòng lệch khi kiểm tra |
| R3 Switch read | — | Cả hai | **Mới** (flag) | 2 tuần ổn định, consumer downstream đã chuyển |
| R4 Stop old | Đặt DEFAULT cho cột cũ | **Chỉ mới** | Mới | Hết cửa sổ rollback (≥ 2 tuần) |
| R5 Contract | Xóa cột cũ | Mới | Mới | — |

### R0 — Chuẩn bị

- Tách Flyway khỏi startup: `spring.flyway.enabled=false` trong app; pipeline chạy `flyway migrate` bằng một K8s Job **trước** khi rolling deploy, với user DB có quyền DDL (app dùng user không có quyền DDL).
- Mọi migration bắt đầu bằng giới hạn chờ lock và chạy lại được:
  ```sql
  SET SESSION lock_wait_timeout = 5;    -- chờ MDL tối đa 5 s, thất bại thì Job retry sau
  ```
- Kiểm tra trước khi chạy: không có transaction dài trên bảng (`information_schema.innodb_trx`, `performance_schema.metadata_locks`).
- Kiểm kê writer của bảng `customer`: `customer-service`, job đối soát, 2 script vận hành (grep repo + `performance_schema.events_statements_summary_by_digest` theo user DB).

### R1 — Expand

```sql
-- V41__customer_expand_phone_name.sql
SET SESSION lock_wait_timeout = 5;
ALTER TABLE customer
    ADD COLUMN phone_e164  VARCHAR(16)  NULL,
    ADD COLUMN family_name VARCHAR(100) NULL,
    ADD COLUMN given_name  VARCHAR(100) NULL,
    ALGORITHM = INSTANT;
```

App cũ không bị ảnh hưởng: Hibernate chỉ select cột có trong entity. Rủi ro cần kiểm tra: code JDBC `SELECT *` đọc cột theo **chỉ số**, hoặc `INSERT INTO customer VALUES (...)` không liệt kê cột → grep toàn bộ codebase và script.

### R2 — Dual write

```java
@Entity
@Table(name = "customer")
public class Customer {
    @Id private Long id;
    @Column(name = "mobile", nullable = false) private String mobile;          // cũ — vẫn là nguồn đọc
    @Column(name = "full_name", nullable = false) private String fullName;      // cũ
    @Column(name = "phone_e164") private String phoneE164;                      // mới
    @Column(name = "family_name") private String familyName;                    // mới
    @Column(name = "given_name") private String givenName;                      // mới

    public void changeMobile(String raw) {
        this.mobile = raw;
        this.phoneE164 = PhoneNormalizer.toE164(raw, "VN");    // libphonenumber
    }
    public void changeFullName(String name) {
        this.fullName = name;
        NameParts p = VietnameseNameSplitter.split(name);
        this.familyName = p.family();
        this.givenName = p.given();
    }
}
```

Job đối soát và 2 script vận hành được sửa tương tự (hoặc gọi API của customer-service thay vì ghi thẳng). Tạm thời thêm trigger **chỉ để phát hiện** writer bị sót: ghi log vào bảng `migration_audit` khi một UPDATE đổi `mobile` mà không đổi `phone_e164` (gỡ sau backfill).

Điểm mấu chốt: **backfill chỉ bắt đầu khi 100% pod đã là R2**. Nếu backfill chạy lúc còn pod R1, pod R1 cập nhật `mobile` mà không cập nhật `phone_e164` → dữ liệu mới bị lệch ngay sau khi backfill đi qua.

### Backfill — keyset, có kiểm tra điều kiện, throttle theo lag, chạy lại được

```java
@Component
@RequiredArgsConstructor
class CustomerBackfillJob {
    private final JdbcTemplate jdbc;
    private final ReplicaLagProbe lag;          // đọc bảng heartbeat (pt-heartbeat) trên replica
    private final CheckpointRepository checkpoint;

    public void run() throws InterruptedException {
        long lastId = checkpoint.load("customer_phone_name");      // chạy tiếp từ chỗ dừng
        int chunk = 2_000;
        while (true) {
            List<Row> rows = jdbc.query("""
                SELECT id, mobile, full_name FROM customer
                WHERE id > ? AND phone_e164 IS NULL ORDER BY id LIMIT ?
                """, rowMapper, lastId, chunk);
            if (rows.isEmpty()) break;

            List<Object[]> args = rows.stream().map(r -> {
                NameParts n = VietnameseNameSplitter.split(r.fullName());
                return new Object[]{ PhoneNormalizer.toE164(r.mobile(), "VN"), n.family(), n.given(),
                                     r.id(), r.mobile(), r.fullName() };
            }).toList();
            // Chỉ ghi nếu dòng chưa bị app (R2) cập nhật trong lúc ta tính toán
            jdbc.batchUpdate("""
                UPDATE customer SET phone_e164 = ?, family_name = ?, given_name = ?
                WHERE id = ? AND phone_e164 IS NULL AND mobile = ? AND full_name = ?
                """, args);

            lastId = rows.get(rows.size() - 1).id();
            checkpoint.save("customer_phone_name", lastId);

            Duration l = lag.current();
            if (l.compareTo(Duration.ofSeconds(2)) > 0) Thread.sleep(l.toMillis() * 2);   // throttle
            else Thread.sleep(50);
        }
    }
}
```

- Mỗi batch là một transaction ngắn (auto-commit của `batchUpdate` với `rewriteBatchedStatements=true`), không giữ lock lâu.
- Tốc độ mục tiêu ~3.000 dòng/s → 200 triệu dòng ≈ 19 giờ; chạy liên tục có throttle, ưu tiên ban đêm.
- **Binlog và CDC**: 200 triệu UPDATE dạng ROW sinh hàng trăm GB binlog và 200 triệu change event cho Debezium. Trước khi chạy: kiểm tra dung lượng đĩa và `binlog_expire_logs_seconds`, báo đội data — consumer downstream phải chịu được (hoặc lọc event có cờ backfill; ví dụ chạy job bằng user DB riêng và để consumer bỏ qua thay đổi chỉ chạm vào 3 cột mới).
- Số điện thoại không chuẩn hóa được (rác) → `toE164` trả `null`, dòng vẫn được tách tên, id được ghi vào bảng `phone_invalid` để nghiệp vụ xử lý. Vì job đi theo keyset `id > lastId` và lưu checkpoint, các dòng này không bị quét lại. Unique index của MySQL cho phép nhiều giá trị `NULL`.

**Kiểm tra sau backfill** (theo lô, trên replica):

```sql
SELECT COUNT(*) FROM customer
WHERE id BETWEEN ? AND ? AND phone_e164 IS NULL
  AND id NOT IN (SELECT id FROM phone_invalid);                 -- kỳ vọng 0

SELECT phone_e164, COUNT(*) FROM customer
WHERE phone_e164 IS NOT NULL GROUP BY phone_e164 HAVING COUNT(*) > 1 LIMIT 100;   -- trùng số → xử lý trước khi tạo unique
```

**Tạo unique index bằng gh-ost** (không dùng online DDL trực tiếp vì replica sẽ lag hàng giờ):

```bash
gh-ost --host=mysql-replica-1 --database=shop --table=customer \
  --alter="ADD UNIQUE INDEX ux_customer_phone_e164 (phone_e164)" \
  --max-lag-millis=2000 --chunk-size=2000 --max-load=Threads_running=40 \
  --critical-load=Threads_running=200 --cut-over=default \
  --postpone-cut-over-flag-file=/tmp/ghost.postpone --execute
```

gh-ost tạo bảng bóng, copy theo chunk, đọc binlog để áp thay đổi đang diễn ra, tự throttle theo lag; cut-over bằng rename nguyên tử, có thể hoãn tới khung giờ thấp điểm. Lưu ý gh-ost hạn chế với bảng có foreign key và trigger — trigger phát hiện writer ở R2 nên gỡ trước bước này.

### R3 — Chuyển đọc sang cột mới (sau feature flag)

```java
public String phoneForDisplay(Customer c) {
    return flags.isOn("customer.read-new-columns") ? c.getPhoneE164() : c.getMobile();
}
```

Vẫn ghi cả hai cột → rollback chỉ là tắt flag. Tính năng đăng nhập bằng số điện thoại dùng `ux_customer_phone_e164`. Downstream (warehouse, search, fraud) chuyển sang cột mới trong giai đoạn này; Debezium tự phát hiện cột mới qua schema history.

### R4 — Ngừng ghi cột cũ

Cột cũ là `NOT NULL` → nếu R4 ngừng ghi, INSERT sẽ lỗi. Trước R4, migration đặt default (thao tác metadata):

```sql
-- V44__customer_old_columns_default.sql
SET SESSION lock_wait_timeout = 5;
ALTER TABLE customer ALTER COLUMN mobile SET DEFAULT '', ALTER COLUMN full_name SET DEFAULT '';
```

Entity R4 bỏ hẳn mapping `mobile`, `full_name`; `spring.jpa.hibernate.ddl-auto=validate` vẫn qua vì Hibernate chỉ kiểm tra cột được map.

### R5 — Contract (sau tối thiểu 2 tuần ổn định R4)

```sql
-- V45__customer_drop_old_columns.sql
SET SESSION lock_wait_timeout = 5;
ALTER TABLE customer DROP COLUMN mobile, DROP COLUMN full_name, ALGORITHM = INSTANT;   -- MySQL 8.0.29+
```

Trước khi xóa: xác nhận không còn query nào chạm cột cũ (`performance_schema` digest 14 ngày), mọi consumer CDC đã bỏ cột, có backup logic (export `id, mobile, full_name` ra object storage) phục vụ tra soát.

---

## 6. Rủi ro & rollback

| Bước | Rủi ro | Phát hiện | Rollback |
|---|---|---|---|
| R1 | MDL chờ query dài → chặn dây chuyền | `lock_wait_timeout` làm migration fail nhanh | Retry Job lúc khác; cột thêm vào vô hại, không cần xóa |
| R2 | Writer bị sót ghi cột cũ không cập nhật cột mới | Trigger audit, query đối chiếu | Deploy lại R1 code; chạy lại backfill sau khi sửa (job idempotent) |
| Backfill | Lag replica, đầy đĩa binlog, flood CDC | Metric lag/heartbeat, disk, lag connector | Dừng job (checkpoint), chạy tiếp sau |
| Backfill | Ghi đè dữ liệu mới bằng dữ liệu cũ | Điều kiện `mobile = ? AND phone_e164 IS NULL` | — (được chặn bởi thiết kế) |
| Unique index | Số điện thoại trùng giữa các tài khoản | Query kiểm tra trùng trước | Xử lý nghiệp vụ trước khi tạo; gh-ost có thể hủy trước cut-over |
| R3 | Lỗi chuẩn hóa hiển thị sai | So sánh mẫu, phản hồi người dùng | Tắt flag (giây) |
| R4 | Cần quay lại R2/R3 trong khi cột cũ đã stale | — | Quay về R3 an toàn (đọc mới, ghi cả hai); quay về R2 (đọc cũ) cần **reverse backfill** — vì vậy chỉ sang R4 khi đã chắc chắn |
| R5 | Xóa cột còn được dùng ở đâu đó | Digest query, consumer list | **Điểm không quay lại** — chỉ phục hồi từ export/backup |

Nguyên tắc: **rollback code, không rollback schema**. Mỗi migration chỉ tiến (forward-only); "rollback" một expand là để nguyên cột thừa.

---

## 7. Phòng ngừa & quy trình

- **Checklist review migration** (bắt buộc trong PR):
  - [ ] Lệnh lấy lock gì, giữ bao lâu? Có `lock_wait_timeout`/`lock_timeout`?
  - [ ] Phiên bản app trước có chạy được với schema mới không? Phiên bản sau có chạy được với schema cũ không (khi rollback)?
  - [ ] DDL có rebuild bảng không? Kích thước bảng? Có cần gh-ost / `CONCURRENTLY` / `ONLINE`?
  - [ ] Ảnh hưởng replica lag, binlog/WAL, CDC consumer?
  - [ ] Có `DROP`/`RENAME` trong cùng release với thay đổi code ngừng dùng cột không? (cấm)
- **CI**: lint migration (ví dụ squawk cho PostgreSQL, rule tự viết cho MySQL: cấm `RENAME COLUMN`, `MODIFY` trên bảng > 10 triệu dòng nếu không có nhãn được duyệt); chạy toàn bộ migration trên bản clone dữ liệu kích thước production hằng tuần, đo thời gian lock.
- **Vận hành**: Flyway là Job riêng, app không có quyền DDL; dashboard MDL wait, replica lag, binlog disk; runbook backfill có nút dừng.
- **Test**: Testcontainers chạy migration + test tương thích "code N với schema N+1" và "code N+1 với schema N" trong pipeline.

---

## 8. Cách kể lại trong phỏng vấn (STAR)

- **Situation:** "Bảng `customer` 200 triệu dòng, 180 GB, MySQL 8 với Debezium phía sau. Cần chuẩn hóa số điện thoại thành cột mới có unique index và tách họ tên, không downtime. Sáu tháng trước một lệnh rename cột đơn giản đã gây 18 phút gián đoạn vì metadata lock và vì pod cũ còn đọc cột cũ."
- **Task:** "Mình thiết kế và dẫn kế hoạch migration, với điều kiện mọi bước rollback được bằng cách deploy lại code."
- **Action:** "Đầu tiên tách Flyway ra Job riêng với `lock_wait_timeout`. Sau đó làm expand/contract qua 5 release: thêm cột instant; dual write trong entity và sửa mọi writer khác, có trigger tạm để phát hiện writer bị sót; backfill bằng job keyset có checkpoint, UPDATE có điều kiện để không đè dữ liệu mới, throttle theo replica lag, và báo trước cho đội data về 200 triệu change event; tạo unique index bằng gh-ost để replica không lag; chuyển đọc qua feature flag; ngừng ghi cột cũ sau khi đặt DEFAULT; xóa cột sau hai tuần."
- **Result:** "Toàn bộ mất 5 tuần, không có phút downtime nào; replica lag tối đa 2,8 giây trong lúc backfill; phát hiện 41 nghìn số điện thoại rác và 1.200 cặp trùng được xử lý trước khi tạo unique index. Checklist migration sau đó thành quy định chung của phòng."

---

## 9. Câu hỏi mở rộng

<details>
<summary>1. Nếu là PostgreSQL thì kế hoạch khác ở đâu?</summary>

Thêm cột nullable là metadata-only nhưng vẫn cần `ACCESS EXCLUSIVE` ngắn → `SET lock_timeout = '3s'`. Unique index dùng `CREATE UNIQUE INDEX CONCURRENTLY` (ngoài transaction — Flyway cần cấu hình script không transactional, ví dụ file `.conf` kèm `executeInTransaction=false`); nếu lỗi giữa chừng để lại index `INVALID`, phải drop rồi tạo lại. Backfill tạo dead tuple → theo dõi autovacuum/bloat, có thể tăng `autovacuum_vacuum_cost_limit` tạm thời. Logical replication (Debezium) cũng nhận 200 triệu event.
</details>

<details>
<summary>2. Oracle có công cụ gì đặc biệt?</summary>

`CREATE INDEX ... ONLINE`, `ALTER TABLE ... SET UNUSED COLUMN` (ẩn cột ngay, xóa vật lý sau), `DBMS_REDEFINITION` để tái cấu trúc bảng online, và **Edition-Based Redefinition (EBR)**: editioning view + crossedition trigger cho phép app cũ và mới thấy cấu trúc khác nhau cùng lúc — hữu ích cho rename trong hệ thống có nhiều PL/SQL.
</details>

<details>
<summary>3. Dual write trong app hay trigger — chọn thế nào?</summary>

App: logic phức tạp (libphonenumber, tách tên) viết bằng Java dễ test; nhưng phải kiểm soát mọi writer. Trigger: bắt mọi writer, nhưng logic trong SQL khó, tăng chi phí ghi, ẩn khỏi developer, và cản trở gh-ost. Khi writer không kiểm soát được (nhiều hệ thống ghi chung DB), trigger hoặc CDC đồng bộ là lựa chọn an toàn hơn.
</details>

<details>
<summary>4. Vì sao không dùng view hoặc generated column làm "bí danh" cho tên cột mới?</summary>

Có thể cho rename đơn thuần: MySQL generated column `phone_e164 AS (mobile) VIRTUAL` hoặc view đổi tên cột giúp app mới đọc tên mới trong lúc app cũ dùng tên cũ. Nhưng ở đây cần **chuẩn hóa** dữ liệu (không biểu diễn được bằng biểu thức đơn giản) và unique index trên giá trị đã chuẩn hóa, nên cần cột thật. Ngoài ra JPA ghi qua view có hạn chế.
</details>

<details>
<summary>5. gh-ost khác pt-online-schema-change thế nào?</summary>

pt-osc dùng **trigger** trên bảng gốc để đồng bộ thay đổi sang bảng mới — tăng tải ghi, khó tạm dừng, xung đột với trigger có sẵn. gh-ost **không dùng trigger**, đọc binlog (thường từ replica) để áp thay đổi, có thể pause/throttle thực sự, cut-over có thể hoãn. Cả hai đều cần dung lượng đĩa gấp đôi bảng và có giới hạn với FK.
</details>

<details>
<summary>6. Làm sao test được "code N chạy với schema N+1"?</summary>

Trong CI: Testcontainers dựng DB, chạy migration tới N+1, rồi chạy bộ integration test của **artifact phiên bản N** (đã build trước đó) trên DB đó; ngược lại chạy code N+1 trên schema N (cho trường hợp code deploy trước migration hoặc rollback). Đây là test hợp đồng giữa code và schema, bắt được lỗi `SELECT *` theo chỉ số, cột NOT NULL thiếu default, v.v.
</details>

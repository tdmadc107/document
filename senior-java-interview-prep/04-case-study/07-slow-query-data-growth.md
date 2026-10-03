# Case 07 — Query báo cáo chậm dần khi bảng chạm 50 triệu dòng: index sai, ép kiểu ngầm và OFFSET sâu

> **Chủ đề:** Slow query log, `EXPLAIN`/`EXPLAIN ANALYZE`, composite & covering index, leftmost prefix, implicit type conversion, keyset pagination, online DDL
> **Module liên quan:** [M11 §3 — Index chuyên sâu](../01-giao-trinh/11-database-sql.md#phan-3) · [M11 §4 — Execution plan & statistics](../01-giao-trinh/11-database-sql.md#phan-4) · [M11 §5 — Quy trình tối ưu query & slow query log](../01-giao-trinh/11-database-sql.md#phan-5) · [M11 §9 — Pagination ở quy mô lớn](../01-giao-trinh/11-database-sql.md#phan-9) · [M11 §10 — Partitioning](../01-giao-trinh/11-database-sql.md#phan-10) · [M11 §13 — Migration không downtime](../01-giao-trinh/11-database-sql.md#phan-13) · [M09 §1 — JDBC nền tảng](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#1-jdbc)
> **Độ khó:** ⭐⭐⭐ (Senior)
> **Thời gian tự giải gợi ý:** 40 phút

---

## 1. Bối cảnh hệ thống

Cổng thanh toán cho merchant (shop online). Merchant xem "Lịch sử giao dịch" và xuất file đối soát.

| Thành phần | Chi tiết |
|---|---|
| Stack | Java 17, Spring Boot 3.1, Spring Data JPA + vài native query, MySQL 8.0 (InnoDB), 1 primary + 2 read replica |
| Bảng | `payment_transaction`: **50 triệu dòng**, tăng ~1,5 triệu/ngày dịp cao điểm; 41 GB data + index; buffer pool 24 GB |
| Phân bố | 38.000 merchant; top 20 merchant chiếm 55% giao dịch (merchant lớn nhất: 6 triệu dòng) |
| Endpoint | `GET /merchant/transactions?from&to&status&page&size=20` (đọc replica); `GET /merchant/transactions/by-ref/{ref}`; job xuất CSV đối soát hằng đêm |

```sql
CREATE TABLE payment_transaction (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  merchant_id   BIGINT       NOT NULL,
  external_ref  VARCHAR(64)  NOT NULL,          -- mã đơn của merchant, có merchant dùng toàn số
  status        VARCHAR(16)  NOT NULL,
  amount        DECIMAL(15,2) NOT NULL,
  created_at    DATETIME(3)  NOT NULL,
  ... 14 cột khác (JSON metadata, card_bin, ...)
  KEY idx_merchant   (merchant_id),
  KEY idx_created_at (created_at),
  KEY idx_external_ref (external_ref)
);
```

## 2. Triệu chứng

**Xu hướng 6 tháng (Grafana, p95 của `GET /merchant/transactions`):**
```
Tháng   |  T3    T4    T5    T6     T7     T8 (50M dòng)
p95     |  120ms 180ms 450ms 1.2s   3.8s   9.1s
```

**Phàn nàn:** merchant lớn báo "trang lịch sử giao dịch quay mãi", job xuất đối soát đêm kéo dài từ 20 phút lên **5 giờ 40 phút**, chạy lấn sang giờ làm việc và làm replica lag 90 giây (merchant thấy giao dịch vừa thanh toán "biến mất").

**Slow query log (replica, `long_query_time = 1`):**
```
# Time: 2026-09-12T09:14:22.318842Z
# User@Host: merchant_api[merchant_api] @ 10.20.3.41 []  Id: 918273
# Query_time: 8.912334  Lock_time: 0.000012 Rows_sent: 20  Rows_examined: 4018220
SET timestamp=1757668462;
SELECT id, external_ref, status, amount, created_at FROM payment_transaction
 WHERE merchant_id = 10023 AND status IN ('SUCCESS','REFUNDED')
   AND created_at >= '2026-06-01' AND created_at < '2026-09-01'
 ORDER BY created_at DESC LIMIT 20 OFFSET 4000;

# Query_time: 21.402771  Lock_time: 0.000009 Rows_sent: 1  Rows_examined: 50112904
SELECT ... FROM payment_transaction WHERE external_ref = 20260911887123;
```

**`pt-query-digest` (24 giờ) — top theo tổng thời gian:**
```
# Rank Query ID           Response time    Calls  R/Call   Item
#    1 0x9F3A...C1        48211.2s 61.0%   18402   2.6199  SELECT payment_transaction (listing)
#    2 0x77B0...4E        19877.5s 25.1%     930  21.3737  SELECT payment_transaction (by external_ref)
#    3 0x1C2D...9A         6420.1s  8.1%   21011   0.3056  SELECT COUNT(*) payment_transaction
```

## 3. Câu hỏi đặt ra

1. Vì sao query nhanh lúc 5 triệu dòng lại chậm lúc 50 triệu dù vẫn "có index"?
2. Query theo `external_ref` có index riêng mà vẫn quét 50 triệu dòng — vì sao?
3. Bạn sẽ thiết kế index nào, viết lại query thế nào? Thêm index vào bảng 50 triệu dòng trên production ra sao?
4. Job xuất đối soát 5 giờ 40 phút — nguyên nhân và cách sửa?

> ✋ **Dừng lại và tự giải trước.** Với query listing, hãy viết index composite bạn đề xuất (thứ tự cột!) và giải thích vì sao không phải `(merchant_id, status, created_at)`.

## 4. Điều tra từng bước

### Bước 1 — `EXPLAIN` query listing
```
mysql> EXPLAIN SELECT ... (query listing ở trên);
+----+-------------+---------------------+-------+-----------------------------+----------------+---------+------+---------+----------+----------------------------------+
| id | select_type | table               | type  | possible_keys               | key            | key_len | ref  | rows    | filtered | Extra                            |
+----+-------------+---------------------+-------+-----------------------------+----------------+---------+------+---------+----------+----------------------------------+
|  1 | SIMPLE      | payment_transaction | range | idx_merchant,idx_created_at | idx_created_at | 7       | NULL | 9214330 |     0.50 | Using where; Backward index scan |
+----+-------------+---------------------+-------+-----------------------------+----------------+---------+------+---------+----------+----------------------------------+
```
`EXPLAIN ANALYZE` (MySQL 8.0.18+) cho thấy thực tế:
```
-> Limit/Offset: 20/4000 row(s)  (actual time=8911..8912 rows=20 loops=1)
    -> Filter: ((payment_transaction.merchant_id = 10023) and (payment_transaction.`status` in ('SUCCESS','REFUNDED')))
         (cost=1.02e+6 rows=46071) (actual time=0.9..8909 rows=4020 loops=1)
        -> Index range scan on payment_transaction using idx_created_at over ('2026-06-01 00:00:00.000' <= created_at < '2026-09-01 00:00:00.000') (reverse)
             (cost=1.02e+6 rows=9214330) (actual time=0.07..8120 rows=4018220 loops=1)
```
Đọc plan:
- Optimizer chọn `idx_created_at` (quét ngược để khỏi sort), rồi với **mỗi** dòng phải nhảy về clustered index (PK) lấy `merchant_id`, `status` để lọc → 4 triệu lần lookup ngẫu nhiên để lấy được 4.020 dòng khớp.
- Phương án còn lại `idx_merchant` cũng tệ với merchant lớn: lấy 6 triệu dòng của merchant rồi **filesort**.
- Hồi bảng 5 triệu dòng, cùng plan chỉ quét ~400.000 entry và vừa trong buffer pool → 120 ms. Khối lượng tăng 10 lần, working set vượt buffer pool → I/O đĩa → chậm hơn 70 lần. **Plan không đổi, dữ liệu đổi.**

### Bước 2 — Query theo `external_ref`
```
mysql> EXPLAIN SELECT * FROM payment_transaction WHERE external_ref = 20260911887123;
| type | possible_keys    | key  | rows     | Extra       |
| ALL  | idx_external_ref | NULL | 49871022 | Using where |

mysql> SHOW WARNINGS;
| Warning | 1739 | Cannot use ref access on index 'idx_external_ref' due to type or collation conversion on field 'external_ref' |
```
`external_ref` là `VARCHAR`, nhưng tham số là **số**. MySQL so sánh chuỗi với số bằng cách đổi **từng giá trị của cột** sang số (`'00123' = 123`, `'123abc' = 123` đều đúng) → không thể dùng B-Tree xếp theo chuỗi → full table scan. Từ đâu ra tham số số? Code Java:
```java
@Entity
public class PaymentTransaction {
    @Column(name = "external_ref") private Long externalRef;   // ❌ ánh xạ Long cho cột VARCHAR
}
// Spring Data: findByMerchantIdAndExternalRef(Long merchantId, Long externalRef) → driver gửi tham số kiểu số
```
Field được đổi từ `String` sang `Long` trong một refactor "vì merchant nào cũng dùng mã số", và không ai nhận ra vì schema validation tắt (`ddl-auto=none`).

### Bước 3 — `COUNT(*)` cho tổng số trang
Mỗi request listing kèm `SELECT COUNT(*) ... WHERE merchant_id = ? AND ...` (do `Page<T>` của Spring Data) → với merchant lớn đếm hàng triệu dòng mỗi lần chuyển trang. 21.000 lần/ngày, 0,3 s mỗi lần.

### Bước 4 — Job xuất đối soát
```java
for (int page = 0; ; page++) {                                     // ❌ OFFSET tăng dần
    List<Row> rows = jdbc.query("SELECT ... WHERE created_at >= ? AND created_at < ? ORDER BY id LIMIT 5000 OFFSET ?",
                                mapper, from, to, page * 5000);
    if (rows.isEmpty()) break;
    csv.write(rows);
}
```
Với 2 triệu dòng/ngày: trang thứ k phải đọc rồi bỏ `k × 5000` dòng → tổng số dòng đọc ≈ n²/(2 × 5000) = **400 triệu** dòng cho 2 triệu dòng hữu ích. Độ phức tạp bậc hai — chạy 20 phút khi 300.000 dòng/ngày, 5 giờ 40 khi 2 triệu.

## 5. Nguyên nhân gốc

| # | Vấn đề | Cơ chế |
|---|---|---|
| 1 | Thiếu index composite phù hợp với `WHERE merchant_id = ? AND created_at BETWEEN ? ORDER BY created_at` | Hai index đơn cột buộc optimizer chọn giữa "quét theo thời gian rồi lọc merchant" và "lấy theo merchant rồi sort"; cả hai đều tỷ lệ với kích thước bảng |
| 2 | Ép kiểu ngầm VARCHAR ↔ số do mapping Java sai | Hàm chuyển kiểu áp lên cột → không sargable → full scan |
| 3 | OFFSET pagination trong job batch | O(n²) theo số dòng |
| 4 | `COUNT(*)` chính xác mỗi request | Đếm lại hàng triệu dòng cho một con số người dùng hiếm khi cần |

Yếu tố góp phần: không có dashboard theo dõi `Rows_examined / Rows_sent`; query được review khi bảng còn nhỏ; không có test schema validation.

## 6. Giải pháp

### Ngắn hạn
1. Sửa mapping `externalRef` về `String` (hotfix trong ngày) → query theo ref từ 21 s xuống **0,8 ms**.
2. Job đối soát chuyển sang keyset theo PK (sửa 10 dòng code) → 5 giờ 40 xuống **6 phút**:
```java
long lastId = 0;
while (true) {
    List<Row> rows = jdbc.query("""
            SELECT id, merchant_id, external_ref, status, amount, created_at FROM payment_transaction
             WHERE created_at >= ? AND created_at < ? AND id > ?
             ORDER BY id LIMIT 5000""", mapper, from, to, lastId);
    if (rows.isEmpty()) break;
    csv.write(rows);
    lastId = rows.get(rows.size() - 1).id();
}
```
MySQL seek theo PK từ `lastId` nên mỗi lô chỉ đọc ~5.000 dòng. Vì `id` AUTO_INCREMENT tăng theo thời gian, cách gọn hơn nữa: dùng `idx_created_at` tìm `minId`/`maxId` của ngày một lần, rồi quét `id BETWEEN ? AND ?` theo lô — khỏi lọc `created_at` ở từng lô.

### Dài hạn — index đúng cho query listing
**Quy tắc thứ tự cột:** cột so sánh **bằng** trước, rồi cột **sắp xếp/range**; cột lọc thêm có thể đặt sau để index "lọc tại chỗ".
```sql
ALTER TABLE payment_transaction
  ADD INDEX idx_merchant_created (merchant_id, created_at, status),
  ALGORITHM = INPLACE, LOCK = NONE;
```
- `(merchant_id, created_at)` → seek thẳng tới merchant, đọc đúng khoảng thời gian **theo thứ tự đã sắp** → không filesort, dừng sau `LIMIT`.
- Thêm `status` ở cuối: lọc `status IN (...)` ngay trên entry index (Index Condition Pushdown) trước khi nhảy về bảng.
- **Vì sao không `(merchant_id, status, created_at)`?** Với `status IN ('SUCCESS','REFUNDED')`, index này cho hai khoảng `created_at` riêng biệt (mỗi status một khoảng) → kết quả không còn xếp theo `created_at` toàn cục → filesort. Nó chỉ tốt nếu luôn lọc **một** status.
- InnoDB secondary index chứa sẵn PK (`id`) → `(merchant_id, created_at, status)` thực chất là `(merchant_id, created_at, status, id)`, dùng được cho keyset `(created_at, id)`.

**Listing: keyset thay OFFSET, bỏ `COUNT(*)` chính xác**
```sql
-- Trang đầu
SELECT id, external_ref, status, amount, created_at FROM payment_transaction
 WHERE merchant_id = :m AND status IN (:statuses) AND created_at >= :from AND created_at < :to
 ORDER BY created_at DESC, id DESC LIMIT 21;                       -- lấy 21 để biết còn trang sau
-- Trang sau: cursor = (created_at, id) của dòng cuối
   AND (created_at < :c OR (created_at = :c AND id < :id))
   AND created_at <= :c                                            -- điều kiện dư giúp MySQL seek
```
API trả `nextCursor` (Base64 của `{createdAt, id}`); UI đổi từ "trang 1…200" sang "Xem thêm". Tổng số hiển thị "khoảng 1,2 triệu giao dịch" lấy từ bảng thống kê cập nhật mỗi 15 phút.

**Thêm index an toàn trên 50 triệu dòng:**
- MySQL 8 Online DDL (`ALGORITHM=INPLACE, LOCK=NONE`) cho phép đọc/ghi trong lúc build, nhưng vẫn tốn I/O và khi chạy qua replication, replica áp dụng DDL **tuần tự** → lag. Đội chọn **gh-ost** (bảng bóng + binlog, có throttle theo replica lag): 52 phút, lag < 3 s.
- Chạy giờ thấp điểm, kiểm tra dung lượng đĩa (index mới ~2,1 GB), có kế hoạch hủy.
- Sau khi tạo: `ANALYZE TABLE payment_transaction;` và so sánh `EXPLAIN` trước/sau; xóa `idx_merchant` (đã thừa — là tiền tố của index mới) để giảm chi phí ghi.

**Kết quả `EXPLAIN ANALYZE` sau khi sửa:**
```
-> Limit: 21 row(s)  (actual time=0.31..1.92 rows=21 loops=1)
    -> Index range scan on payment_transaction using idx_merchant_created over (merchant_id = 10023 AND '2026-06-01' <= created_at < '2026-09-01') (reverse),
         with index condition: (payment_transaction.`status` in ('SUCCESS','REFUNDED'))  (actual time=0.30..1.90 rows=21 loops=1)
```
p95 listing: 9,1 s → **14 ms**; replica CPU 85% → 22%.

### So sánh lựa chọn
| Lựa chọn | Ưu | Nhược | Khi nào |
|---|---|---|---|
| Composite index `(merchant_id, created_at, status)` | Giải quyết gốc cho query chính, rẻ | Tốn ~2 GB, thêm chi phí mỗi INSERT | Luôn làm cho access pattern chính |
| Covering index (thêm mọi cột SELECT) | Không cần quay về bảng (`Using index`) | Index phình to, ghi chậm; khó khi SELECT nhiều cột | Query nóng chỉ cần vài cột |
| `FORCE INDEX` | Sửa nhanh plan sai | Cứng nhắc, sai khi dữ liệu đổi; tên index đổi là lỗi | Tạm thời, có ghi chú |
| Keyset pagination | O(log n + limit) ổn định, không trùng/sót | Không nhảy trang tùy ý | API danh sách, export, batch |
| Deferred join với OFFSET | Giữ được "nhảy trang" | Vẫn O(offset) trên index | Màn hình admin bắt buộc có số trang |
| Partition theo tháng (`RANGE (created_at)`) | Partition pruning; xóa dữ liệu cũ bằng `DROP PARTITION` | Ở MySQL, PK và mọi unique key phải chứa cột partition; query không lọc theo thời gian phải quét mọi partition | Bảng append-only có retention |
| Archive dữ liệu > 13 tháng ra kho lạnh | Bảng nóng nhỏ lại, buffer pool hiệu quả | Cần luồng truy vấn dữ liệu cũ riêng | Dữ liệu tăng không ngừng |
| Đẩy báo cáo sang OLAP (ClickHouse/Elasticsearch) | Lọc/tổng hợp tùy ý rất nhanh | Thêm hệ thống, độ trễ đồng bộ (CDC) | Báo cáo đa chiều, ad-hoc |

## 7. Phòng ngừa

- **Slow query log + pt-query-digest hằng ngày**, xếp hạng theo **tổng thời gian** và theo tỷ lệ `Rows_examined / Rows_sent` (> 1.000 là cờ đỏ dù query đang nhanh).
- Alert khi p95 của một query tăng > 2 lần so với trung bình 7 ngày; dashboard `performance_schema.events_statements_summary_by_digest`.
- **Test với dữ liệu kích thước thật**: môi trường perf có bản sao (đã ẩn danh) của bảng lớn; mỗi query mới phải kèm `EXPLAIN` trong PR.
- **Schema validation**: `spring.jpa.hibernate.ddl-auto=validate` trong integration test (Testcontainers + Flyway) bắt lỗi kiểu `found [varchar], but expecting [bigint]` ngay ở CI.
- **Code review checklist cho query**:
  - [ ] Index nào phục vụ query này? Thứ tự cột có theo "bằng → sort/range" không?
  - [ ] Có hàm/biểu thức/ép kiểu trên cột trong `WHERE` không (`DATE(created_at)`, so sánh VARCHAR với số, collation khác nhau khi JOIN)?
  - [ ] Phân trang: OFFSET có thể sâu không? Có `COUNT(*)` không cần thiết không?
  - [ ] Ước lượng: khi bảng gấp 10 lần thì query đọc bao nhiêu dòng?
- **Quản lý vòng đời dữ liệu**: chính sách retention/archiving được quyết định khi tạo bảng, không phải khi bảng đã 50 triệu dòng.

## 8. Cách kể lại trong phỏng vấn (STAR, ~1,5 phút)

- **S:** "Bảng giao dịch của cổng thanh toán tăng lên 50 triệu dòng; trong 6 tháng p95 trang lịch sử giao dịch từ 120 ms lên 9 giây, job đối soát đêm từ 20 phút lên gần 6 tiếng, còn làm replica lag."
- **T:** "Tôi phụ trách tối ưu mà không được dừng hệ thống."
- **A:** "Tôi dùng pt-query-digest trên slow log để xếp hạng theo tổng thời gian: ba query chiếm 94%. `EXPLAIN ANALYZE` của query listing cho thấy MySQL quét ngược index `created_at` và lọc merchant từng dòng — 4 triệu dòng để lấy 20. Query thứ hai có index nhưng full scan 50 triệu dòng: `SHOW WARNINGS` báo không dùng được index do ép kiểu — entity ánh xạ `Long` cho cột `VARCHAR`. Job đối soát dùng OFFSET nên độ phức tạp bậc hai. Tôi sửa mapping và đổi job sang keyset theo id ngay trong ngày; sau đó thêm index `(merchant_id, created_at, status)` bằng gh-ost có throttle theo replica lag, chuyển listing sang cursor pagination và bỏ `COUNT(*)` chính xác."
- **R:** "Listing còn 14 ms, query theo mã đơn 0,8 ms, job đối soát 6 phút, CPU replica từ 85% xuống 22%. Tôi đưa `ddl-auto=validate` vào CI, bắt buộc kèm `EXPLAIN` cho query mới, và thêm báo cáo slow log hằng ngày theo tỷ lệ rows examined/sent."

## 9. Câu hỏi mở rộng

<details>
<summary>1. Vì sao optimizer chọn sai index? Làm sao giúp nó chọn đúng mà không <code>FORCE INDEX</code>?</summary>

Optimizer dựa trên ước lượng chi phí từ **statistics** (cardinality của index, phân bố dữ liệu). Với dữ liệu lệch (merchant lớn 6 triệu dòng, merchant nhỏ vài trăm), một ước lượng trung bình sai cho cả hai đầu. Biện pháp: tạo index phù hợp để không còn phải "chọn giữa hai cái tệ"; `ANALYZE TABLE` khi phân bố đổi nhiều; histogram của MySQL 8 (`ANALYZE TABLE t UPDATE HISTOGRAM ON status`) cho cột không có index; tăng `innodb_stats_persistent_sample_pages` cho bảng lớn. `FORCE INDEX` chỉ là băng dán tạm vì nó đóng băng quyết định trong khi dữ liệu tiếp tục thay đổi.
</details>

<details>
<summary>2. Nhiều index thì có hại gì? Làm sao biết index nào thừa?</summary>

Mỗi index là một B-Tree phải cập nhật ở mỗi INSERT/UPDATE cột liên quan/DELETE → ghi chậm hơn, tốn buffer pool (đẩy dữ liệu nóng ra khỏi RAM), tăng dung lượng backup và replication. Tìm index thừa: index là **tiền tố** của index khác (`idx_merchant` vs `idx_merchant_created`), index không được dùng (`sys.schema_unused_indexes` trong MySQL, `pg_stat_user_indexes.idx_scan = 0` trong PostgreSQL — theo dõi đủ lâu, gồm cả job cuối tháng). MySQL 8 có **invisible index** để "tắt thử" trước khi xóa hẳn.
</details>

<details>
<summary>3. Trên PostgreSQL, bug ép kiểu có xảy ra giống hệt không?</summary>

Không giống hệt: PostgreSQL không tự ép `varchar = bigint` mà báo lỗi `operator does not exist: character varying = bigint` — lỗi lộ ra ngay. Nhưng biến thể khác vẫn có: so sánh `numeric` với `bigint`, `timestamp` với `timestamptz`, hay driver gửi tham số kiểu khác làm planner phải cast phía cột; dùng hàm trên cột (`lower(email)`) không có expression index. Trên SQL Server, bẫy kinh điển là JDBC gửi chuỗi dạng `NVARCHAR` cho cột `VARCHAR` (`sendStringParametersAsUnicode=true` mặc định) → index scan thay vì seek; Oracle tương tự khi so sánh `VARCHAR2` với `NUMBER`.
</details>

<details>
<summary>4. Covering index là gì? Có nên làm covering cho query listing này không?</summary>

Covering index chứa mọi cột query cần (WHERE, ORDER BY, SELECT) nên DB trả kết quả chỉ từ index, không quay về bảng (MySQL: `Using index`; PostgreSQL: Index Only Scan, phụ thuộc visibility map). Ở đây listing đã chỉ đọc 21 dòng sau khi có index đúng, nên 21 lần lookup về clustered index là không đáng kể — thêm `amount`, `external_ref` vào index sẽ làm index to gấp đôi mà lợi ích nhỏ. Covering đáng làm cho query nóng đọc **nhiều** dòng nhưng ít cột, như `COUNT`/`SUM` theo merchant trong ngày.
</details>

<details>
<summary>5. Keyset pagination xử lý "nhảy tới trang 50" và sắp xếp theo cột không duy nhất thế nào?</summary>

Keyset không nhảy trang tùy ý — chấp nhận đánh đổi này cho API/infinite scroll; nếu nghiệp vụ bắt buộc, dùng deferred join (OFFSET trên index covering rồi join lấy 20 dòng) và giới hạn độ sâu tối đa. Với cột sắp xếp không duy nhất (`created_at` trùng nhau), luôn thêm cột duy nhất làm tie-breaker (`id`) vào cả `ORDER BY`, điều kiện cursor và index, nếu không sẽ trùng/sót dòng tại ranh giới trang.
</details>

<details>
<summary>6. Khi nào nên nghĩ tới partitioning hoặc tách sang hệ thống khác thay vì chỉ thêm index?</summary>

Khi vấn đề là **khối lượng tổng thể** chứ không phải một access pattern: bảng tăng không ngừng và cần xóa dữ liệu cũ (partition theo thời gian → `DROP PARTITION` thay vì `DELETE` hàng triệu dòng), working set vượt RAM dù index đúng, hoặc nhu cầu truy vấn đa chiều/ad-hoc (lọc theo 10 tiêu chí tùy ý, tổng hợp) mà OLTP không phục vụ hiệu quả — khi đó CDC sang ClickHouse/Elasticsearch cho báo cáo. Partitioning không làm query nhanh hơn nếu query không lọc theo khóa partition, và ở MySQL có ràng buộc PK/unique phải chứa cột partition.
</details>

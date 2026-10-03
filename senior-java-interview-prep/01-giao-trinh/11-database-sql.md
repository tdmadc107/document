# Module 11 — Database & SQL cho Senior

> **Mục tiêu:** sau module này bạn giải thích được mô hình quan hệ, chuẩn hóa và lúc nào nên phi chuẩn hóa; viết được SQL nâng cao (window function, recursive CTE, UPSERT) trên PostgreSQL / MySQL / Oracle; thiết kế index dựa trên cấu trúc B-Tree và đọc được execution plan; hiểu sâu transaction, isolation level, MVCC, locking để debug deadlock và lost update; thiết kế được hệ thống dữ liệu ở quy mô lớn (pagination, partitioning, sharding, replication, migration không downtime) và lựa chọn đúng giữa SQL và NoSQL.
> **Cấp độ:** Cơ bản → Nâng cao (Senior)
> **Thời lượng gợi ý:** 8 ngày (≈ 40 giờ)
> **Yêu cầu trước:** Module về Java Core, JDBC cơ bản; nên học trước Module JPA/Hibernate (hoặc học song song).
> **Nguồn tham khảo:**
> - Trong kho: [`Ebook IT/OCA_Oracle_Database_SQL_Exam_Guide_Exam_1Z0071.pdf`](../../Ebook%20IT/OCA_Oracle_Database_SQL_Exam_Guide_Exam_1Z0071.pdf) — các chương về SELECT, single-row functions (NVL, DECODE, TO_CHAR…), group functions & GROUP BY/HAVING, joins, subqueries, set operators, DML/transaction control, sequences & indexes.
> - Trong kho: [`Ebook IT/Pro Oracle Database 18c Administration, 3rd Edition`](../../Ebook%20IT/Pro%20Oracle%20Database%2018c%20Administration,%203rd%20Edition%20-%20Ebook.pdf) — phần kiến trúc instance/memory (SGA, shared pool), undo, redo, tables & indexes, partitioning, optimizer statistics.
> - Ngoài: [PostgreSQL Documentation](https://www.postgresql.org/docs/current/) — chương *Concurrency Control* (MVCC, isolation, explicit locking), *Indexes*, *Performance Tips* (EXPLAIN), *Routine Vacuuming*.
> - Ngoài: [MySQL 8.0 Reference Manual — The InnoDB Storage Engine](https://dev.mysql.com/doc/refman/8.0/en/innodb-storage-engine.html) — *InnoDB Locking and Transaction Model*, *Clustered and Secondary Indexes*, *Multi-Versioning*.
> - Ngoài: *Oracle Database Concepts* (docs.oracle.com) — chương *Data Concurrency and Consistency*, *Indexes and Index-Organized Tables*.
> - Ngoài: [Use The Index, Luke](https://use-the-index-luke.com/) (Markus Winand) — toàn bộ, đặc biệt phần *Anatomy of an Index*, *The Where Clause*, *Partial Results* (pagination).
> - Ngoài: *Designing Data-Intensive Applications* (Martin Kleppmann) — Ch.3 *Storage and Retrieval*, Ch.5 *Replication*, Ch.6 *Partitioning*, Ch.7 *Transactions*.

## Mục lục
1. [Mô hình quan hệ & chuẩn hóa](#phan-1)
2. [SQL nâng cao](#phan-2)
3. [Index chuyên sâu](#phan-3)
4. [Execution plan, thuật toán JOIN & statistics](#phan-4)
5. [Quy trình tối ưu query & slow query log](#phan-5)
6. [Transaction, ACID & isolation level](#phan-6)
7. [MVCC — bên dưới nắp capo](#phan-7)
8. [Locking & deadlock](#phan-8)
9. [Pagination ở quy mô lớn](#phan-9)
10. [Partitioning, sharding, replication & connection limits](#phan-10)
11. [Stored procedure, PL/SQL & đặc thù Oracle](#phan-11)
12. [NoSQL & chọn SQL hay NoSQL](#phan-12)
13. [Migration dữ liệu không downtime (expand/contract)](#phan-13)
14. [Dự án mini của module](#du-an-mini)
15. [Checklist tự đánh giá](#checklist-tu-danh-gia)

> **Chuẩn bị môi trường:** dùng Docker để có cả 3 DB, chạy được mọi ví dụ trong module.
> ```bash
> docker run -d --name pg  -e POSTGRES_PASSWORD=pass -p 5432:5432 postgres:16
> docker run -d --name my  -e MYSQL_ROOT_PASSWORD=pass -p 3306:3306 mysql:8.0
> docker run -d --name ora -e ORACLE_PASSWORD=pass -p 1521:1521 gvenzl/oracle-free:slim
> ```

---

<a id="phan-1"></a>
## 1. Mô hình quan hệ & chuẩn hóa

### 1.1 Khái niệm
**Relational model** (E. F. Codd, 1970): dữ liệu được tổ chức thành các **relation** (bảng), mỗi relation là tập hợp các **tuple** (dòng) có cùng tập **attribute** (cột). Các khái niệm cốt lõi:

| Khái niệm | Ý nghĩa | Ví dụ |
|---|---|---|
| **Super key** | Tập cột xác định duy nhất một dòng | `{id, email}` |
| **Candidate key** | Super key tối thiểu (bỏ bớt cột nào cũng mất tính duy nhất) | `{id}`, `{email}` |
| **Primary key** | Candidate key được chọn làm định danh chính | `id` |
| **Foreign key** | Cột tham chiếu tới key của bảng khác, đảm bảo **referential integrity** | `orders.customer_id → customers.id` |
| **Functional dependency (FD)** | `A → B`: biết A thì xác định duy nhất B | `zip_code → city` |

**Natural key vs surrogate key:**
- Natural key (CMND/CCCD, email, mã số thuế) có ý nghĩa nghiệp vụ nhưng **có thể thay đổi** (người dùng đổi email) và thường dài.
- Surrogate key (`BIGINT` auto-increment, sequence, UUID) ổn định, ngắn. Thực tế: dùng surrogate làm PK + `UNIQUE` constraint trên natural key.

> 💡 **Góc nhìn Senior — UUID làm PK:** UUIDv4 ngẫu nhiên gây **insert ngẫu nhiên** vào B-Tree → page split, buffer pool kém hiệu quả, đặc biệt tệ với InnoDB vì PK chính là clustered index (dữ liệu bị xé vụn). Nếu cần ID sinh ở client/phân tán, dùng **UUIDv7** hoặc ULID / Snowflake ID (có tiền tố thời gian → gần tuần tự). Lưu UUID dạng `BINARY(16)`/`uuid` thay vì `VARCHAR(36)`.

### 1.2 Các dạng chuẩn (Normal Forms)

Xét bảng chưa chuẩn hóa dùng xuyên suốt:

```
order_flat(order_id, order_date, customer_id, customer_name, customer_city,
           product_ids = "P1,P2", product_names = "Bút,Vở", quantities = "2,5")
```

**1NF — giá trị nguyên tử (atomic), không có nhóm lặp.** Cột `product_ids = "P1,P2"` vi phạm 1NF → tách thành mỗi dòng một sản phẩm:

```
order_line(order_id, product_id, order_date, customer_id, customer_name,
           customer_city, product_name, quantity)    -- PK = (order_id, product_id)
```

**2NF — 1NF + không có partial dependency** (thuộc tính non-key phụ thuộc vào *một phần* của composite key). Ở trên `order_date, customer_id → phụ thuộc order_id` và `product_name → phụ thuộc product_id` → vi phạm. Tách:

```
orders(order_id PK, order_date, customer_id, customer_name, customer_city)
products(product_id PK, product_name)
order_items(order_id, product_id, quantity)   -- PK = (order_id, product_id)
```

**3NF — 2NF + không có transitive dependency** (`order_id → customer_id → customer_name`). Tách tiếp:

```
customers(customer_id PK, customer_name, customer_city)
orders(order_id PK, order_date, customer_id FK)
```

**BCNF (Boyce–Codd)** — với mọi FD không tầm thường `X → Y`, X phải là super key. 3NF cho phép ngoại lệ khi Y là một phần của candidate key; BCNF thì không. Ví dụ kinh điển:

```
teaching(student, course, teacher)
FD: (student, course) → teacher ;  teacher → course   (mỗi giảng viên dạy đúng 1 môn)
```
Bảng này ở 3NF (course là prime attribute) nhưng **không** BCNF vì `teacher` không phải super key → bất thường cập nhật: đổi môn của giảng viên phải sửa nhiều dòng. Tách BCNF: `teacher_course(teacher PK, course)` + `student_teacher(student, teacher)` — đổi lại **mất khả năng enforce** FD `(student, course) → teacher` bằng constraint đơn giản. Đây là trade-off kinh điển: BCNF không phải lúc nào cũng *dependency-preserving*.

**Ba loại bất thường (anomaly) mà chuẩn hóa loại bỏ:**
- *Update anomaly*: đổi tên khách hàng phải sửa N dòng, sót một dòng là dữ liệu mâu thuẫn.
- *Insert anomaly*: không thể thêm sản phẩm mới khi chưa có đơn hàng.
- *Delete anomaly*: xóa đơn hàng cuối cùng làm mất luôn thông tin khách hàng.

### 1.3 Khi nào phi chuẩn hóa (denormalize)
Chuẩn hóa tối ưu cho **ghi đúng**; phi chuẩn hóa tối ưu cho **đọc nhanh**. Các kỹ thuật thường gặp:

| Kỹ thuật | Ví dụ | Chi phí phải trả |
|---|---|---|
| Cột dẫn xuất / đếm sẵn | `posts.comment_count` | Cập nhật đồng thời → hot row, cần `UPDATE ... SET c = c + 1` |
| Sao chép cột để tránh JOIN | `order_items.product_name` | Đồng bộ khi đổi tên (nhưng với đơn hàng thì **đây là đúng** — snapshot tại thời điểm mua!) |
| Bảng tổng hợp / materialized view | `daily_revenue` | Độ trễ dữ liệu, job refresh |
| Lưu JSON (`jsonb`, `JSON`) | thuộc tính động của sản phẩm | Khó ràng buộc, khó index (cần GIN/generated column) |
| Read model riêng (CQRS) | Elasticsearch cho search | Eventual consistency, pipeline đồng bộ |

> 💡 **Góc nhìn Senior:** Phân biệt **"dữ liệu lịch sử" vs "dữ liệu trùng lặp"**. Giá sản phẩm trong `order_items.unit_price` *không phải* denormalization — đó là sự thật tại thời điểm đặt hàng, bắt buộc phải lưu. Phỏng vấn viên hay gài câu này. Quy tắc thực tế: chuẩn hóa tới 3NF cho OLTP, denormalize có chủ đích khi có **số liệu đo** chứng minh JOIN là bottleneck, và luôn ghi rõ "nguồn sự thật" (source of truth) là ở đâu.

> ⚠️ **Lỗi thường gặp:**
> - Lưu danh sách ID dạng chuỗi `"1,2,3"` (vi phạm 1NF) → không index được, không FK được, `LIKE '%,2,%'` full scan.
> - Bỏ FK "cho nhanh" rồi để rác dữ liệu orphan. Nếu thật sự bỏ FK (sharding, write-heavy), phải có job kiểm tra toàn vẹn.
> - EAV (Entity–Attribute–Value) cho mọi thứ → query cực khó, không có type safety. Ngày nay ưu tiên `jsonb` + generated column.

### 🛠 Bài tập phần 1

**Bài 1.1 — Chuẩn hóa bảng Excel (Cơ bản)**
- Đề bài: Bộ phận nhân sự gửi file `employee_project(emp_id, emp_name, dept_id, dept_name, dept_manager, project_code, project_name, hours)`. Mỗi nhân viên thuộc một phòng ban, làm nhiều dự án.
- Yêu cầu / tiêu chí đạt: liệt kê đầy đủ các FD; đưa về 3NF; viết DDL (PostgreSQL) có PK, FK, NOT NULL, UNIQUE phù hợp; chỉ ra mỗi loại anomaly trong bảng gốc bằng một ví dụ cụ thể.

**Bài 1.2 — BCNF và trade-off (Trung bình)**
- Đề bài: Bảng `booking(room, start_time, end_time, customer)` với ràng buộc một phòng không được trùng giờ. Thêm thuộc tính `room_type` mà `room → room_type`.
- Yêu cầu: xác định dạng chuẩn hiện tại, tách về BCNF, rồi trả lời: ràng buộc "không trùng giờ" có thể enforce bằng constraint không? (Gợi ý PostgreSQL `EXCLUDE USING gist`). Tiêu chí: DDL chạy được, insert hai booking chồng giờ bị từ chối.

**Bài 1.3 — Denormalize có chủ đích (Nâng cao)**
- Đề bài: Trang danh sách bài viết hiển thị `title, author_name, comment_count, last_comment_at`, 5.000 req/s, bảng `comments` 200 triệu dòng.
- Yêu cầu: đề xuất 2 phương án (cột đếm sẵn cập nhật đồng bộ vs bảng tổng hợp cập nhật bất đồng bộ qua event). Phân tích: hot row contention khi bài viết viral, độ trễ chấp nhận được, cách tự sửa khi đếm sai (reconcile job). Tiêu chí: có bảng so sánh, có SQL cho cả hai phương án.

<details>
<summary>Gợi ý lời giải</summary>

**1.1** FD: `emp_id → emp_name, dept_id`; `dept_id → dept_name, dept_manager`; `(emp_id, project_code) → hours`; `project_code → project_name`. 3NF:
```sql
CREATE TABLE department (dept_id INT PRIMARY KEY, dept_name VARCHAR(100) NOT NULL UNIQUE, manager_id INT);
CREATE TABLE employee   (emp_id INT PRIMARY KEY, emp_name VARCHAR(100) NOT NULL,
                         dept_id INT NOT NULL REFERENCES department(dept_id));
CREATE TABLE project    (project_code VARCHAR(20) PRIMARY KEY, project_name VARCHAR(200) NOT NULL);
CREATE TABLE assignment (emp_id INT REFERENCES employee, project_code VARCHAR(20) REFERENCES project,
                         hours NUMERIC(6,2) NOT NULL CHECK (hours >= 0),
                         PRIMARY KEY (emp_id, project_code));
```
**1.2** `room → room_type` mà `room` không là super key → không BCNF. Tách `room(room PK, room_type)`. Ràng buộc chồng giờ:
```sql
CREATE EXTENSION IF NOT EXISTS btree_gist;
CREATE TABLE booking (
  room TEXT NOT NULL REFERENCES room(room),
  during TSTZRANGE NOT NULL,
  customer TEXT NOT NULL,
  EXCLUDE USING gist (room WITH =, during WITH &&)
);
```
**1.3** Phương án đồng bộ: `UPDATE posts SET comment_count = comment_count + 1 WHERE id = ?` trong cùng transaction insert comment → đúng ngay nhưng bài viral làm row lock của `posts` thành điểm nghẽn. Phương án bất đồng bộ: ghi event, consumer gom batch (`+37` mỗi giây) → giảm contention, chấp nhận trễ vài giây. Reconcile: job đêm `UPDATE posts p SET comment_count = c.cnt FROM (SELECT post_id, count(*) cnt FROM comments GROUP BY post_id) c WHERE p.id = c.post_id AND p.comment_count <> c.cnt`.
</details>

---

<a id="phan-2"></a>
## 2. SQL nâng cao

Dữ liệu mẫu (chạy được trên PostgreSQL; MySQL/Oracle chỉ cần đổi kiểu dữ liệu):

```sql
CREATE TABLE customers (id INT PRIMARY KEY, name VARCHAR(100), city VARCHAR(50), referrer_id INT);
CREATE TABLE orders    (id INT PRIMARY KEY, customer_id INT REFERENCES customers(id),
                        created_at DATE, amount NUMERIC(12,2), status VARCHAR(20));
CREATE TABLE employees (id INT PRIMARY KEY, name VARCHAR(100), manager_id INT REFERENCES employees(id),
                        dept VARCHAR(20), salary NUMERIC(12,2));

INSERT INTO customers VALUES (1,'An','HN',NULL),(2,'Bình','HCM',1),(3,'Chi','HN',2),(4,'Dũng','DN',NULL);
INSERT INTO orders VALUES
 (101,1,'2024-01-05',500,'PAID'),(102,1,'2024-01-20',300,'PAID'),(103,2,'2024-01-21',900,'CANCELLED'),
 (104,2,'2024-02-02',150,'PAID'),(105,3,'2024-02-10',700,'PAID'),(106,1,'2024-02-15',200,'PAID');
INSERT INTO employees VALUES
 (1,'CEO',NULL,'BOD',100),(2,'CTO',1,'IT',80),(3,'Lead A',2,'IT',50),(4,'Dev 1',3,'IT',30),
 (5,'Dev 2',3,'IT',30),(6,'CFO',1,'FIN',80),(7,'Accountant',6,'FIN',25);
```

### 2.1 JOIN types

| JOIN | Kết quả | Ghi chú |
|---|---|---|
| `INNER JOIN` | Chỉ các cặp khớp điều kiện | |
| `LEFT [OUTER] JOIN` | Toàn bộ bảng trái + phần khớp bên phải (NULL nếu không khớp) | Phổ biến nhất trong báo cáo |
| `RIGHT JOIN` | Ngược lại | Thường viết lại thành LEFT cho dễ đọc |
| `FULL OUTER JOIN` | Hợp của LEFT và RIGHT | **MySQL không hỗ trợ** → `LEFT ... UNION ... RIGHT` |
| `CROSS JOIN` | Tích Descartes | Sinh lịch, ma trận |
| `SELF JOIN` | Bảng join chính nó | Quan hệ cha-con |
| *Semi-join* (`EXISTS`/`IN`) | Dòng bên trái **có** dòng khớp | Không nhân bản dòng |
| *Anti-join* (`NOT EXISTS`) | Dòng bên trái **không có** dòng khớp | |

**Bẫy kinh điển — điều kiện ở `ON` vs `WHERE` với LEFT JOIN:**

```sql
-- (A) Khách hàng kèm đơn PAID; khách không có đơn PAID vẫn xuất hiện với NULL
SELECT c.name, o.id FROM customers c
LEFT JOIN orders o ON o.customer_id = c.id AND o.status = 'PAID';

-- (B) Lọc ở WHERE biến LEFT JOIN thành INNER JOIN một cách âm thầm (Dũng biến mất)
SELECT c.name, o.id FROM customers c
LEFT JOIN orders o ON o.customer_id = c.id
WHERE o.status = 'PAID';
```

**Bẫy `NOT IN` với NULL:**

```sql
-- Nếu subquery trả về bất kỳ NULL nào, NOT IN trả về rỗng (vì x <> NULL là UNKNOWN)
SELECT name FROM customers WHERE id NOT IN (SELECT referrer_id FROM customers);   -- 0 dòng!
SELECT name FROM customers c WHERE NOT EXISTS
  (SELECT 1 FROM customers r WHERE r.referrer_id = c.id);                          -- đúng: Chi, Dũng
```

### 2.2 Subquery vs JOIN, correlated subquery

- **Non-correlated subquery** chạy độc lập một lần: `WHERE amount > (SELECT AVG(amount) FROM orders)`.
- **Correlated subquery** tham chiếu cột bên ngoài, về mặt logic chạy **một lần cho mỗi dòng** ngoài:

```sql
-- Đơn hàng lớn hơn trung bình của CHÍNH khách hàng đó
SELECT o.* FROM orders o
WHERE o.amount > (SELECT AVG(o2.amount) FROM orders o2 WHERE o2.customer_id = o.customer_id);
```

Optimizer hiện đại thường **unnest** (biến subquery thành join/semi-join), nên "subquery luôn chậm hơn JOIN" là **ngộ nhận**. Nhưng:
- MySQL 5.5 trở về trước tối ưu `IN (subquery)` rất tệ (chạy dạng dependent subquery). MySQL 5.6+ có semi-join optimization, 8.0.16+ còn chuyển `EXISTS` thành semi-join.
- Scalar subquery trong `SELECT` list (`SELECT (SELECT count(*) FROM ... WHERE x = o.id)`) thường vẫn chạy N lần → nên viết lại bằng JOIN + GROUP BY hoặc `LATERAL`.
- JOIN với bảng 1-N rồi `COUNT(*)` có thể **nhân bản dòng** (fan-out) → số liệu sai. `EXISTS` không bị.

```sql
-- PostgreSQL LATERAL (MySQL 8.0.14+ cũng có; Oracle 12c+ có LATERAL / CROSS APPLY):
-- 2 đơn mới nhất mỗi khách
SELECT c.name, x.id, x.created_at
FROM customers c
CROSS JOIN LATERAL (
  SELECT o.id, o.created_at FROM orders o
  WHERE o.customer_id = c.id ORDER BY o.created_at DESC LIMIT 2
) x;
```

### 2.3 GROUP BY / HAVING và thứ tự thực thi logic

Thứ tự **logic** (không phải vật lý): `FROM/JOIN → WHERE → GROUP BY → HAVING → window functions → SELECT → DISTINCT → ORDER BY → LIMIT/OFFSET`.

Hệ quả:
- `WHERE` không dùng được aggregate (`WHERE SUM(x) > 10` sai) → dùng `HAVING`.
- Không dùng alias của SELECT trong `WHERE` (PostgreSQL/Oracle báo lỗi; MySQL cho phép trong `HAVING`/`ORDER BY`).
- Lọc trước ở `WHERE` rẻ hơn lọc sau ở `HAVING` vì giảm số dòng phải group.

```sql
SELECT customer_id, COUNT(*) AS cnt, SUM(amount) AS total
FROM orders
WHERE status = 'PAID'                 -- lọc dòng trước
GROUP BY customer_id
HAVING SUM(amount) >= 500             -- lọc nhóm sau
ORDER BY total DESC;
```

> ⚠️ **Lỗi thường gặp:** MySQL với `sql_mode` thiếu `ONLY_FULL_GROUP_BY` (mặc định có từ 5.7.5) cho phép `SELECT name, MAX(amount) ... GROUP BY customer_id` và trả về giá trị `name` **tùy ý**. Code chạy "đúng" trên dev, sai trên prod. Luôn bật `ONLY_FULL_GROUP_BY`.

`COUNT(*)` đếm mọi dòng; `COUNT(col)` bỏ NULL; `COUNT(DISTINCT col)`. Aggregate có điều kiện:

```sql
SELECT customer_id,
       COUNT(*) FILTER (WHERE status = 'PAID')                 AS paid_cnt,   -- PostgreSQL
       SUM(CASE WHEN status = 'CANCELLED' THEN 1 ELSE 0 END)   AS cancel_cnt  -- chuẩn, chạy mọi DB
FROM orders GROUP BY customer_id;
```

### 2.4 Window functions

Window function tính toán trên một "cửa sổ" dòng **mà không gộp dòng** như GROUP BY. Cú pháp: `fn() OVER (PARTITION BY ... ORDER BY ... ROWS/RANGE BETWEEN ...)`. Có trên PostgreSQL, Oracle (từ 8i), MySQL 8.0+.

```sql
SELECT id, customer_id, amount,
  ROW_NUMBER() OVER (PARTITION BY customer_id ORDER BY amount DESC) AS rn,     -- 1,2,3 (không trùng)
  RANK()       OVER (ORDER BY amount DESC)                          AS rnk,    -- 1,2,2,4 (có nhảy)
  DENSE_RANK() OVER (ORDER BY amount DESC)                          AS drnk,   -- 1,2,2,3
  LAG(amount)  OVER (PARTITION BY customer_id ORDER BY created_at)  AS prev_amount,
  LEAD(created_at) OVER (PARTITION BY customer_id ORDER BY created_at) AS next_order_date,
  SUM(amount)  OVER (PARTITION BY customer_id ORDER BY created_at
                     ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS running_total,
  AVG(amount)  OVER (ORDER BY created_at ROWS BETWEEN 2 PRECEDING AND CURRENT ROW) AS moving_avg_3,
  amount * 100.0 / SUM(amount) OVER (PARTITION BY customer_id)      AS pct_of_customer
FROM orders WHERE status = 'PAID';
```

**Top-N per group** (câu hỏi phỏng vấn kinh điển):

```sql
SELECT * FROM (
  SELECT o.*, ROW_NUMBER() OVER (PARTITION BY customer_id ORDER BY amount DESC) rn
  FROM orders o
) t WHERE rn <= 2;
```

**`ROWS` vs `RANGE`:** khi có `ORDER BY` mà không chỉ định frame, mặc định là `RANGE BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW` — các dòng **cùng giá trị ORDER BY** (peers) được tính chung. Running total với ngày trùng nhau sẽ "nhảy cóc". Muốn cộng dồn từng dòng → ghi rõ `ROWS`.

**Gaps & islands** (chuỗi ngày đăng nhập liên tiếp):

```sql
-- Mỗi chuỗi ngày liên tiếp có cùng (login_date - row_number) → group theo đó
SELECT user_id, MIN(login_date) AS start_d, MAX(login_date) AS end_d, COUNT(*) AS streak
FROM (
  SELECT user_id, login_date,
         login_date - CAST(ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY login_date) AS INT) AS grp
  FROM (SELECT DISTINCT user_id, login_date FROM logins) d
) t
GROUP BY user_id, grp;
```

### 2.5 CTE & recursive CTE

CTE (`WITH`) giúp chia query thành các bước có tên. Lưu ý về **materialization**: PostgreSQL < 12 luôn materialize CTE (là "optimization fence"); từ 12 tự inline nếu CTE được tham chiếu một lần và không có side-effect, có thể ép bằng `AS MATERIALIZED` / `NOT MATERIALIZED`.

```sql
-- Cây tổ chức: mọi cấp dưới của CTO kèm độ sâu và đường dẫn
WITH RECURSIVE org AS (                         -- Oracle: bỏ chữ RECURSIVE (hoặc dùng CONNECT BY)
  SELECT id, name, manager_id, 1 AS depth, CAST(name AS VARCHAR(1000)) AS path
  FROM employees WHERE id = 2                   -- anchor member
  UNION ALL
  SELECT e.id, e.name, e.manager_id, o.depth + 1, CAST(o.path || ' > ' || e.name AS VARCHAR(1000))
  FROM employees e JOIN org o ON e.manager_id = o.id   -- recursive member
  WHERE o.depth < 20                            -- chặn vòng lặp vô hạn khi dữ liệu có chu trình
)
SELECT * FROM org ORDER BY path;
```

Oracle cú pháp truyền thống (rất hay gặp trong code cũ ở doanh nghiệp Việt Nam):

```sql
SELECT LEVEL, LPAD(' ', 2*(LEVEL-1)) || name AS tree, SYS_CONNECT_BY_PATH(name, '/') AS path
FROM employees
START WITH id = 2
CONNECT BY NOCYCLE PRIOR id = manager_id
ORDER SIBLINGS BY name;
```

Recursive CTE còn dùng để **sinh dãy** (lịch ngày cho báo cáo có ngày không phát sinh):

```sql
WITH RECURSIVE d(day) AS (
  SELECT DATE '2024-01-01' UNION ALL SELECT day + 1 FROM d WHERE day < DATE '2024-01-31'
)
SELECT d.day, COALESCE(SUM(o.amount), 0) AS revenue
FROM d LEFT JOIN orders o ON o.created_at = d.day AND o.status = 'PAID'
GROUP BY d.day ORDER BY d.day;
-- PostgreSQL có sẵn: generate_series('2024-01-01'::date, '2024-01-31', '1 day')
```

### 2.6 UPSERT — MERGE / ON CONFLICT / ON DUPLICATE KEY

Bài toán: "có thì cập nhật, chưa có thì thêm". Cách ngây thơ `SELECT` rồi `INSERT`/`UPDATE` trong code Java là **race condition** (hai request cùng thấy "chưa có", cùng INSERT → một cái lỗi duplicate key hoặc tệ hơn tạo trùng nếu thiếu unique constraint).

```sql
-- PostgreSQL 9.5+ : cần UNIQUE/PK trên cột conflict
CREATE TABLE product_stock (sku VARCHAR(30) PRIMARY KEY, qty INT NOT NULL, updated_at TIMESTAMP);
INSERT INTO product_stock (sku, qty, updated_at) VALUES ('SKU-1', 10, now())
ON CONFLICT (sku) DO UPDATE
  SET qty = product_stock.qty + EXCLUDED.qty, updated_at = EXCLUDED.updated_at
  WHERE product_stock.updated_at < EXCLUDED.updated_at;   -- tùy chọn: chỉ update nếu mới hơn
-- ON CONFLICT DO NOTHING: idempotent insert
```

```sql
-- MySQL: ON DUPLICATE KEY UPDATE (khớp với BẤT KỲ unique key nào, không chỉ PK!)
INSERT INTO product_stock (sku, qty, updated_at) VALUES ('SKU-1', 10, NOW()) AS new
ON DUPLICATE KEY UPDATE qty = product_stock.qty + new.qty, updated_at = new.updated_at;
-- Cú pháp cũ VALUES(qty) bị deprecated từ 8.0.20 → dùng row alias "AS new" như trên.
```

```sql
-- Oracle (và SQL Server, PostgreSQL 15+): MERGE chuẩn SQL:2003
MERGE INTO product_stock t
USING (SELECT 'SKU-1' AS sku, 10 AS qty FROM dual) s
ON (t.sku = s.sku)
WHEN MATCHED THEN UPDATE SET t.qty = t.qty + s.qty, t.updated_at = SYSTIMESTAMP
WHEN NOT MATCHED THEN INSERT (sku, qty, updated_at) VALUES (s.sku, s.qty, SYSTIMESTAMP);
```

> 💡 **Góc nhìn Senior:**
> - `ON CONFLICT` của PostgreSQL là **atomic** với concurrent insert (dựa vào speculative insertion trên unique index). `MERGE` của Oracle/PostgreSQL **không** đảm bảo như vậy: hai session MERGE cùng key đồng thời vẫn có thể cùng đi nhánh `NOT MATCHED` → một bên dính `ORA-00001`/unique violation. Production cần retry hoặc bắt exception.
> - MySQL `ON DUPLICATE KEY UPDATE` trên bảng có **nhiều unique key** hành vi khó đoán (khớp key nào trước); còn lấy gap lock/next-key lock → nguồn deadlock nổi tiếng khi batch upsert song song. Ngoài ra AUTO_INCREMENT vẫn bị "đốt" giá trị khi rơi vào nhánh update.
> - Batch upsert từ Java: dùng `JdbcTemplate.batchUpdate` + `rewriteBatchedStatements=true` (MySQL Connector/J) hoặc `reWriteBatchedInserts=true` (pgJDBC) để giảm round-trip.

> ⚠️ **Lỗi thường gặp:** dùng `REPLACE INTO` của MySQL như upsert — thực chất là **DELETE rồi INSERT**: kích hoạt ON DELETE CASCADE, đổi auto-increment id, mất các cột không được truyền vào.

### 🛠 Bài tập phần 2

**Bài 2.1 — Báo cáo doanh thu theo tháng (Cơ bản)**
- Đề bài: với dữ liệu mẫu, viết query trả về mỗi tháng: số đơn PAID, doanh thu, số khách hàng phân biệt, và doanh thu tháng trước (`LAG`), % tăng trưởng.
- Tiêu chí: chạy đúng trên PostgreSQL và MySQL 8 (khác nhau chỗ nào hàm cắt tháng thì ghi chú); tháng đầu tiên growth = NULL, không chia cho 0.

**Bài 2.2 — Top-N, dedup và running total (Trung bình)**
- Đề bài: (a) Mỗi phòng ban, lấy 2 nhân viên lương cao nhất, nếu đồng lương thì lấy cả (gợi ý chọn đúng hàm rank). (b) Bảng `events(user_id, event_type, payload, created_at)` có bản ghi trùng do retry — xóa trùng, giữ bản ghi sớm nhất. (c) Số dư tài khoản cộng dồn theo từng giao dịch.
- Tiêu chí: (b) phải chạy được trên cả PostgreSQL (dùng `ctid` hoặc id) và MySQL (lưu ý lỗi "can't specify target table for update in FROM clause").

**Bài 2.3 — Hệ thống giới thiệu đa cấp & upsert đồng thời (Nâng cao)**
- Đề bài: (a) Với `customers.referrer_id`, tính cho mỗi khách tổng số người được giới thiệu trực tiếp + gián tiếp (mọi cấp), phát hiện chu trình. (b) Viết Java (JDBC) chạy 50 thread cùng upsert `product_stock` cho cùng 10 SKU, mỗi thread +1 qty 1.000 lần. Chứng minh tổng qty cuối = 50.000 với `ON CONFLICT`, và tái hiện lỗi với cách "SELECT rồi INSERT/UPDATE".
- Tiêu chí: có số liệu trước/sau; giải thích vì sao cách ngây thơ sai.

<details>
<summary>Gợi ý lời giải</summary>

**2.1**
```sql
WITH m AS (
  SELECT date_trunc('month', created_at)::date AS month,   -- MySQL: DATE_FORMAT(created_at,'%Y-%m-01')
         COUNT(*) AS orders, SUM(amount) AS revenue, COUNT(DISTINCT customer_id) AS customers
  FROM orders WHERE status = 'PAID' GROUP BY 1
)
SELECT m.*, LAG(revenue) OVER (ORDER BY month) AS prev_revenue,
       ROUND(100.0 * (revenue - LAG(revenue) OVER (ORDER BY month))
             / NULLIF(LAG(revenue) OVER (ORDER BY month), 0), 2) AS growth_pct
FROM m ORDER BY month;
```
**2.2** (a) dùng `DENSE_RANK()` hoặc `RANK()` với `<= 2`. (b) PostgreSQL:
```sql
DELETE FROM events e USING (
  SELECT id, ROW_NUMBER() OVER (PARTITION BY user_id, event_type, payload ORDER BY created_at, id) rn FROM events
) d WHERE e.id = d.id AND d.rn > 1;
```
MySQL: bọc thêm một lớp derived table `DELETE FROM events WHERE id IN (SELECT id FROM (SELECT id, ROW_NUMBER() ... rn FROM events) x WHERE rn > 1);`. (c) `SUM(amount) OVER (PARTITION BY account_id ORDER BY created_at, id ROWS UNBOUNDED PRECEDING)`.

**2.3** (a) recursive CTE bắt đầu từ mỗi khách làm root, mang theo mảng path (`ARRAY[id]`), dừng khi `NOT r.id = ANY(path)`; PostgreSQL 14+ có `CYCLE id SET is_cycle USING path`. (b) Cách ngây thơ: hai thread cùng `SELECT qty` = 5, cùng `UPDATE SET qty = 6` → mất cập nhật; hoặc cùng thấy chưa có → duplicate key. Với `ON CONFLICT ... SET qty = product_stock.qty + 1` phép cộng diễn ra trên phiên bản mới nhất của dòng dưới row lock → tổng chính xác.
</details>

---

<a id="phan-3"></a>
## 3. Index chuyên sâu

### 3.1 Cấu trúc B-Tree (B+Tree)

Hầu hết index mặc định (PostgreSQL `btree`, InnoDB, Oracle B*Tree) là **B+Tree**:

```
                      [ root:  50 | 120 ]
                     /         |         \
         [branch: 10|30]  [branch: 70|90]  [branch: 150|200]
            /   |   \         ...                ...
   [leaf 1..9]<->[leaf 10..29]<->[leaf 30..49]<-> ...   ← doubly linked list, khóa đã sắp xếp
     key → pointer (ROWID / ctid / PK value)
```

Đặc điểm cần nhớ:
- **Cân bằng**: mọi leaf cùng độ sâu. Fan-out lớn (một page 8–16KB chứa hàng trăm khóa) → bảng 1 tỷ dòng chỉ sâu **3–5 tầng**. Tra cứu = O(log N) page reads, thực tế root & branch luôn nằm trong cache.
- **Leaf nối nhau** → range scan (`BETWEEN`, `>`, `ORDER BY`) chỉ cần tìm điểm đầu rồi đi ngang.
- Một index lookup có 3 bước (theo *Use The Index, Luke*): **tree traversal** (rẻ, cố định) → **đi theo leaf chain** (tỷ lệ với số khóa khớp) → **truy cập bảng** (table access — thường là phần đắt nhất vì mỗi dòng có thể nằm ở một page khác nhau = random I/O).
- Chi phí ghi: mỗi INSERT/UPDATE/DELETE phải cập nhật **mọi index** liên quan; page đầy → **page split**. Bảng có 10 index thì ghi chậm đáng kể.

### 3.2 Clustered vs secondary index (InnoDB), heap table & IOT (Oracle/PostgreSQL)

**InnoDB — mọi bảng là clustered index:**
- Dữ liệu dòng được lưu **ngay trong leaf của PK index** (bảng *chính là* B+Tree sắp theo PK).
- Không có PK → InnoDB dùng UNIQUE index đầu tiên có cột NOT NULL; không có nữa → tự tạo hidden `DB_ROW_ID` 6 byte (và tất cả bảng như vậy dùng chung một bộ đếm toàn cục → contention).
- **Secondary index** lưu `(cột index, giá trị PK)`. Tra qua secondary index = 2 lần duyệt cây: secondary → lấy PK → duyệt clustered index (gọi là *bookmark lookup*/"back to table"/回表).
- Hệ quả: **PK nên ngắn và tăng dần**. PK là `VARCHAR(255)` hay UUID → mọi secondary index đều phình to theo.

**Oracle & PostgreSQL — heap table:**
- Dòng nằm ở **heap** không có thứ tự; mọi index (kể cả PK) đều là "secondary", leaf trỏ tới vị trí vật lý: `ROWID` (Oracle) / `ctid` (PostgreSQL, = (block, offset)).
- Lookup qua index chỉ cần 1 cây + 1 lần đọc heap page.
- PostgreSQL: UPDATE tạo tuple mới ở vị trí mới → mọi index phải thêm entry mới, trừ khi là **HOT update** (Heap-Only Tuple: không cột nào có index bị đổi và page còn chỗ) — vì vậy nên để `fillfactor` < 100 cho bảng update nhiều.
- **Oracle IOT (Index-Organized Table)**: `CREATE TABLE ... ORGANIZATION INDEX` — tương tự clustered index của InnoDB. Hợp với bảng tra cứu theo PK, bảng mapping nhiều-nhiều. Secondary index trên IOT dùng *logical rowid* (PK + guess vật lý).
- PostgreSQL có lệnh `CLUSTER` nhưng chỉ sắp xếp lại **một lần**, không duy trì.

```sql
-- Oracle IOT
CREATE TABLE user_role (
  user_id NUMBER, role_id NUMBER,
  CONSTRAINT pk_user_role PRIMARY KEY (user_id, role_id)
) ORGANIZATION INDEX;
```

### 3.3 Composite index & quy tắc leftmost prefix

Index `(a, b, c)` được sắp theo `a`, trong cùng `a` thì theo `b`, rồi theo `c` — giống danh bạ điện thoại sắp theo (họ, tên đệm, tên).

| Điều kiện | Dùng được index `(a,b,c)`? |
|---|---|
| `a = 1` | ✅ |
| `a = 1 AND b = 2` | ✅ |
| `a = 1 AND b = 2 AND c = 3` | ✅ (thứ tự viết trong WHERE không quan trọng) |
| `b = 2` / `b = 2 AND c = 3` | ❌ seek (có thể full index scan; Oracle/MySQL 8.0.13+ có *skip scan* khi `a` ít giá trị) |
| `a = 1 AND c = 3` | ⚠️ seek theo `a`, `c` chỉ lọc trong leaf (index filter / ICP) |
| `a > 1 AND b = 2` | ⚠️ range trên `a` → `b` không còn dùng để seek (chỉ filter) |
| `a = 1 ORDER BY b` | ✅ tránh được sort |
| `a = 1 ORDER BY c` | ❌ phải sort |

**Nguyên tắc chọn thứ tự cột:**
1. Cột so sánh **bằng** (`=`, `IN`) đặt trước, cột **range** (`>`, `<`, `BETWEEN`, `LIKE 'x%'`) đặt sau cùng.
2. Ưu tiên tái sử dụng: một index `(a, b)` phục vụ được cả query lọc theo `a` → không cần index riêng `(a)` (index `(a)` riêng là **thừa**, chỉ tốn ghi).
3. Không mặc định "cột selectivity cao nhất đặt trước" — đó là lời khuyên chưa đầy đủ; điều kiện bằng/range và khả năng phục vụ nhiều query quan trọng hơn.

### 3.4 Covering index (index-only scan)

Nếu index chứa **mọi cột** mà query cần (WHERE + SELECT + ORDER BY), DB không cần đọc bảng → bỏ được bước đắt nhất.

```sql
-- PostgreSQL 11+: INCLUDE thêm cột vào leaf mà không thuộc khóa sắp xếp
CREATE INDEX idx_orders_cust_date ON orders (customer_id, created_at) INCLUDE (amount, status);
SELECT created_at, amount FROM orders WHERE customer_id = 1 ORDER BY created_at DESC;
-- EXPLAIN: "Index Only Scan" (cần visibility map cập nhật — VACUUM thường xuyên)

-- MySQL: secondary index luôn ngầm chứa PK → index (customer_id, created_at, amount)
-- phủ được cả "SELECT id, created_at, amount". EXPLAIN Extra: "Using index"
```

### 3.5 Selectivity & cardinality

- **Cardinality** = số giá trị phân biệt của cột (`gender` ≈ 2–3, `email` ≈ số dòng).
- **Selectivity** = tỷ lệ dòng được chọn bởi điều kiện (`status = 'CANCELLED'` chọn 1% → selectivity tốt).
- Optimizer chọn index khi ước lượng số dòng khớp **đủ nhỏ**. Ngưỡng không cố định — phụ thuộc clustering factor (mức độ dữ liệu theo thứ tự index nằm liền nhau trên đĩa), cost random vs sequential I/O (`random_page_cost` của PostgreSQL). Thường từ vài % trở lên, full scan có thể rẻ hơn vì đọc tuần tự, đọc theo multi-block.
- Cột phân bố lệch (skew): `status` có 99% `DONE` và 1% `PENDING` — index vô dụng cho `DONE` nhưng rất tốt cho `PENDING`. Cần **histogram** trong statistics để optimizer biết. PostgreSQL còn có **partial index**:

```sql
CREATE INDEX idx_orders_pending ON orders (created_at) WHERE status = 'PENDING';  -- nhỏ gọn, cực nhanh
```

### 3.6 Function-based index

```sql
-- Query tìm email không phân biệt hoa thường
SELECT * FROM users WHERE LOWER(email) = 'an@x.vn';

CREATE INDEX idx_users_lower_email ON users (LOWER(email));          -- PostgreSQL, Oracle
CREATE INDEX idx_users_lower_email ON users ((LOWER(email)));        -- MySQL 8.0.13+ (functional key part, cần 2 lớp ngoặc)
-- MySQL cũ: thêm generated column rồi index
ALTER TABLE users ADD email_lower VARCHAR(255) AS (LOWER(email)) STORED, ADD INDEX (email_lower);
```

Biểu thức trong query phải **khớp** với biểu thức index (Oracle cần hàm `DETERMINISTIC` nếu là hàm tự viết).

### 3.7 Khi nào index KHÔNG được dùng

```sql
-- 1) Hàm/biểu thức trên cột  → không sargable
WHERE YEAR(created_at) = 2024                 -- ❌
WHERE created_at >= '2024-01-01' AND created_at < '2025-01-01'   -- ✅
WHERE TRUNC(created_at) = DATE '2024-01-05'   -- ❌ (Oracle hay gặp)
WHERE amount * 1.1 > 1000                     -- ❌ → WHERE amount > 1000 / 1.1  ✅

-- 2) Implicit type conversion: phone là VARCHAR
WHERE phone = 0912345678                      -- ❌ MySQL ép phone sang số cho mỗi dòng → full scan
WHERE phone = '0912345678'                    -- ✅
-- Oracle: so sánh VARCHAR2 với NUMBER → Oracle áp TO_NUMBER(phone) lên cột → mất index.
-- Java: setLong() cho cột VARCHAR, hoặc khác collation/charset khi JOIN 2 bảng (utf8 vs utf8mb4) cũng gây ra.

-- 3) LIKE bắt đầu bằng wildcard
WHERE name LIKE 'Ngu%'                        -- ✅ range scan
WHERE name LIKE '%yen'                        -- ❌ → full-text index, trigram (pg_trgm GIN), hoặc Elasticsearch

-- 4) OR giữa các cột khác nhau
WHERE customer_id = 1 OR status = 'PENDING'   -- ⚠️ cần index trên cả 2 cột; DB có thể dùng
                                              -- index merge (MySQL) / BitmapOr (PostgreSQL), hoặc viết lại UNION ALL

-- 5) Phủ định & NULL
WHERE status <> 'DONE'                        -- thường full scan
WHERE col IS NULL                             -- Oracle B-Tree KHÔNG lưu entry mà mọi cột đều NULL → không dùng được
                                              -- (mẹo: index (col, 0)); PostgreSQL/MySQL lưu NULL nên dùng được

-- 6) Composite index không khớp leftmost prefix (mục 3.3)
-- 7) Optimizer ước lượng full scan rẻ hơn (bảng nhỏ, selectivity kém, statistics cũ)
```

### 3.8 Các loại index khác (tổng quan)

| Loại | DB | Dùng cho | Lưu ý |
|---|---|---|---|
| **Hash** | PostgreSQL (WAL-logged từ v10), MySQL MEMORY; InnoDB *adaptive hash index* tự động | Chỉ `=` | Không range, không sort |
| **Bitmap** | Oracle | Cột cardinality thấp trong **data warehouse**, kết hợp AND/OR nhiều điều kiện | **Tránh trong OLTP**: một entry bitmap phủ nhiều dòng → DML đồng thời lock lẫn nhau, deadlock |
| **GIN** | PostgreSQL | `jsonb` (`@>`), array, full-text (`tsvector`), trigram | Ghi chậm (có `fastupdate` pending list) |
| **GiST / SP-GiST** | PostgreSQL | Geo (PostGIS), range type, exclusion constraint | |
| **BRIN** | PostgreSQL | Bảng log khổng lồ, dữ liệu tương quan thứ tự vật lý (timestamp append-only) | Siêu nhỏ (vài KB cho hàng trăm GB) |
| **Full-text** | MySQL `FULLTEXT`, Oracle Text | Tìm kiếm văn bản | Tiếng Việt cần tokenizer phù hợp (ngram) |

> 💡 **Góc nhìn Senior:**
> - Index là **trade-off đọc/ghi + bộ nhớ**. Trước khi thêm index, hỏi: query này chạy bao nhiêu lần/giây? Bảng ghi bao nhiêu lần/giây? Có index nào hiện có mở rộng được không?
> - Dọn index thừa: PostgreSQL `pg_stat_user_indexes.idx_scan = 0`; MySQL `sys.schema_unused_indexes`, `sys.schema_redundant_indexes`; Oracle `ALTER INDEX ... MONITORING USAGE` hoặc `DBA_INDEX_USAGE` (12.2+). MySQL 8 / Oracle có **invisible index** để "thử bỏ" index an toàn trước khi drop thật.
> - Tạo index trên bảng lớn production: PostgreSQL `CREATE INDEX CONCURRENTLY` (không chặn ghi, nhưng nếu fail để lại index `INVALID` phải drop); MySQL 8 InnoDB online DDL `ALGORITHM=INPLACE, LOCK=NONE`; Oracle `CREATE INDEX ... ONLINE`.

> ⚠️ **Lỗi thường gặp:**
> - "Index mọi cột" → ghi chậm, buffer pool bị index chiếm.
> - Tạo index `(a)` và `(a, b)` song song — `(a)` thừa.
> - Index trên cột boolean/`status` trong OLTP mà không phải partial index → optimizer không dùng.
> - Quên rằng FK trong PostgreSQL/Oracle **không tự tạo index** trên cột con → DELETE bảng cha full scan bảng con, và ở Oracle còn gây **table lock** trên bảng con (unindexed foreign key). MySQL InnoDB thì tự tạo.

### 🛠 Bài tập phần 3

**Bài 3.1 — Sargable rewrite (Cơ bản)**
- Đề bài: tạo bảng `orders_big` 2 triệu dòng (dùng `generate_series`). Viết lại 5 điều kiện non-sargable sau thành sargable: `DATE(created_at) = '2024-03-01'`, `SUBSTR(phone,1,3) = '091'`, `amount + 100 > 500`, `COALESCE(status,'NEW') = 'NEW'`, `customer_code = 12345` (cột VARCHAR).
- Tiêu chí: với mỗi câu, chụp `EXPLAIN ANALYZE` trước/sau, thời gian giảm rõ rệt, giải thích tại sao.

**Bài 3.2 — Thiết kế composite index cho bộ query (Trung bình)**
- Đề bài: cho 4 query thường xuyên trên `orders_big`:
  (Q1) `WHERE customer_id = ? ORDER BY created_at DESC LIMIT 20`;
  (Q2) `WHERE customer_id = ? AND status = ?`;
  (Q3) `WHERE status = 'PENDING' AND created_at < now() - interval '30 min'`;
  (Q4) `SELECT customer_id, SUM(amount) WHERE created_at >= ? GROUP BY customer_id`.
- Yêu cầu: thiết kế **tối đa 3 index** phục vụ tốt nhất cả 4 query; giải thích thứ tự cột. Tiêu chí: plan của Q1 không có Sort node; Q3 dùng partial index; có đo kích thước index (`pg_relation_size`).

**Bài 3.3 — Clustered index & UUID (Nâng cao)**
- Đề bài: trên MySQL 8, tạo 2 bảng giống nhau, một bảng PK `BIGINT AUTO_INCREMENT`, một bảng PK `BINARY(16)` UUIDv4 ngẫu nhiên, cùng 2 secondary index. Insert 5 triệu dòng bằng Java batch.
- Yêu cầu: so sánh thời gian insert, kích thước (`information_schema.TABLES.DATA_LENGTH`, `INDEX_LENGTH`), `Innodb_buffer_pool_reads`. Lặp lại với UUIDv7. Tiêu chí: có bảng số liệu và giải thích bằng cơ chế page split / clustered index.

<details>
<summary>Gợi ý lời giải</summary>

**3.1** Tạo dữ liệu:
```sql
CREATE TABLE orders_big AS
SELECT g AS id, (random()*100000)::int AS customer_id,
       now() - (random()*365 || ' days')::interval AS created_at,
       (random()*1000)::numeric(12,2) AS amount,
       (ARRAY['PAID','PENDING','CANCELLED','DONE'])[1 + (random()*3)::int] AS status,
       lpad((random()*99999)::int::text, 5, '0') AS customer_code
FROM generate_series(1, 2000000) g;
ANALYZE orders_big;
```
Rewrite: `created_at >= '2024-03-01' AND created_at < '2024-03-02'`; `phone LIKE '091%'` (PostgreSQL cần `text_pattern_ops` hoặc collation "C" để LIKE dùng B-Tree); `amount > 400`; `(status = 'NEW' OR status IS NULL)`; `customer_code = '12345'`.

**3.2**
```sql
-- Q1 cần (customer_id, created_at) để tránh sort; Q2 dùng (customer_id, status)
CREATE INDEX ix_cust_date   ON orders_big (customer_id, created_at DESC);
CREATE INDEX ix_cust_status ON orders_big (customer_id, status);
CREATE INDEX ix_pending     ON orders_big (created_at) WHERE status = 'PENDING';
-- Q4: (created_at) INCLUDE (customer_id, amount) nếu range hẹp; nếu range rộng → full scan + HashAggregate là hợp lý.
```
Thảo luận: có thể gộp Q1+Q2 thành `(customer_id, status, created_at)` không? Q1 không lọc status → index này phải đọc mọi status và sort lại → không tối ưu.

**3.3** UUIDv4 thường cho insert chậm hơn nhiều lần khi dữ liệu vượt buffer pool, kích thước lớn hơn ~ 1.5–2x do page split để lại page nửa trống và secondary index chứa PK 16 byte. UUIDv7 gần với AUTO_INCREMENT vì tăng theo thời gian.
</details>

---

<a id="phan-4"></a>
## 4. Execution plan, thuật toán JOIN & statistics

### 4.1 Đọc execution plan

**PostgreSQL:**
```sql
EXPLAIN SELECT ...;                                  -- plan ước lượng, không chạy
EXPLAIN (ANALYZE, BUFFERS, FORMAT TEXT) SELECT ...;  -- CHẠY THẬT, có thời gian & số dòng thực
-- Cẩn thận: EXPLAIN ANALYZE với UPDATE/DELETE sẽ sửa dữ liệu → bọc trong BEGIN; ... ROLLBACK;
```
```
Limit  (cost=0.43..8.95 rows=20 width=32) (actual time=0.031..0.058 rows=20 loops=1)
  Buffers: shared hit=24
  ->  Index Scan Backward using ix_cust_date on orders_big
        (cost=0.43..85.6 rows=200 width=32) (actual time=0.030..0.054 rows=20 loops=1)
        Index Cond: (customer_id = 42)
Planning Time: 0.15 ms
Execution Time: 0.08 ms
```
Cách đọc: cây đọc **từ trong ra ngoài, từ dưới lên**; `cost=startup..total` là đơn vị tương đối; `rows` ước lượng vs `actual rows` — **chênh lệch lớn (10x+) là dấu hiệu statistics sai** và gốc của hầu hết plan tệ; `loops` — tổng thời gian thực của node = `actual time × loops`; `Buffers: shared hit` (cache) vs `read` (đĩa).

Node thường gặp: `Seq Scan`, `Index Scan`, `Index Only Scan`, `Bitmap Index Scan` + `Bitmap Heap Scan` (gom ctid rồi đọc heap theo thứ tự vật lý), `Nested Loop`, `Hash Join`, `Merge Join`, `Sort` (để ý `Sort Method: external merge Disk` → thiếu `work_mem`), `HashAggregate`, `GroupAggregate`.

**MySQL:**
```sql
EXPLAIN SELECT ...;                    -- dạng bảng
EXPLAIN FORMAT=TREE SELECT ...;        -- 8.0.16+
EXPLAIN ANALYZE SELECT ...;            -- 8.0.18+, chạy thật
```
Cột quan trọng của dạng bảng: `type` (tốt → tệ: `system`, `const`, `eq_ref`, `ref`, `range`, `index` (full index scan), `ALL` (full table scan)); `key` (index được chọn); `rows`; `filtered`; `Extra`: `Using index` (covering), `Using where`, `Using index condition` (ICP), `Using filesort`, `Using temporary` (bảng tạm — hay đi kèm GROUP BY/DISTINCT không có index phù hợp).

**Oracle:**
```sql
EXPLAIN PLAN FOR SELECT * FROM orders WHERE customer_id = 1;
SELECT * FROM TABLE(DBMS_XPLAN.DISPLAY);              -- plan ước lượng

-- Plan THỰC TẾ với số dòng thật (A-Rows) vs ước lượng (E-Rows)
SELECT /*+ GATHER_PLAN_STATISTICS */ * FROM orders WHERE customer_id = 1;
SELECT * FROM TABLE(DBMS_XPLAN.DISPLAY_CURSOR(NULL, NULL, 'ALLSTATS LAST'));
-- Hoặc theo sql_id lấy từ V$SQL: DBMS_XPLAN.DISPLAY_CURSOR('&sql_id', NULL, 'ALLSTATS LAST')
```
Operation thường gặp: `TABLE ACCESS FULL`, `TABLE ACCESS BY INDEX ROWID [BATCHED]`, `INDEX UNIQUE SCAN`, `INDEX RANGE SCAN`, `INDEX FAST FULL SCAN`, `INDEX SKIP SCAN`, `NESTED LOOPS`, `HASH JOIN`, `MERGE JOIN`, `SORT ORDER BY`. Phần `Predicate Information` cho biết điều kiện nào là `access` (dùng để seek) và nào là `filter` (lọc sau) — cực kỳ quan trọng.

> ⚠️ **Lỗi thường gặp:** `EXPLAIN PLAN` trong Oracle **không** dùng bind peeking và luôn coi bind là VARCHAR2 → plan hiển thị có thể **khác** plan thực tế đang chạy. Khi debug production, luôn dùng `DISPLAY_CURSOR` / AWR (`DBMS_XPLAN.DISPLAY_AWR`).

### 4.2 Thuật toán JOIN

| Thuật toán | Cách hoạt động | Tốt khi | Độ phức tạp |
|---|---|---|---|
| **Nested Loop** | Với mỗi dòng bảng ngoài (outer), tìm dòng khớp ở bảng trong (inner) — lý tưởng là qua index | Outer ít dòng + inner có index trên cột join; OLTP, trả về nhanh dòng đầu | O(N × log M) với index; O(N × M) nếu không |
| **Hash Join** | Build hash table từ bảng nhỏ hơn (build side), quét bảng lớn (probe side) để tra | Hai bảng lớn, **chỉ điều kiện bằng**, không có index phù hợp; báo cáo | O(N + M), tốn bộ nhớ (spill ra đĩa nếu thiếu `work_mem`/`hash_area_size`) |
| **Merge Join** (sort-merge) | Sắp xếp hai bên theo khóa join rồi trộn | Hai bên đã sắp sẵn (từ index), điều kiện range/bằng, dữ liệu lớn | O(N log N + M log M), O(N + M) nếu đã sort |

Ghi chú theo DB: MySQL trước 8.0.18 **chỉ có** nested loop (và Block Nested Loop); 8.0.18+ có hash join; MySQL **không có** sort-merge join. PostgreSQL và Oracle có đủ ba loại.

> 💡 **Góc nhìn Senior:** Nested loop với **ước lượng sai** là thủ phạm số 1 của query "lúc nhanh lúc chậm": optimizer nghĩ bảng ngoài có 1 dòng (chọn nested loop), thực tế có 500.000 dòng → 500.000 lần index lookup. Nhìn `loops=500000` trong `EXPLAIN ANALYZE` là biết. Cách sửa: cập nhật statistics, extended statistics cho cột tương quan, viết lại query — **hint là phương án cuối**.

### 4.3 Statistics

Optimizer dựa vào cost-based model; input là **statistics**: số dòng, số block, số giá trị phân biệt (NDV), tỷ lệ NULL, min/max, **histogram** (cho dữ liệu lệch), most common values (PostgreSQL), clustering factor (Oracle).

```sql
-- PostgreSQL
ANALYZE orders;                                              -- autovacuum cũng tự analyze
SELECT attname, n_distinct, most_common_vals FROM pg_stats WHERE tablename = 'orders';
ALTER TABLE orders ALTER COLUMN status SET STATISTICS 1000;  -- tăng độ chi tiết histogram
CREATE STATISTICS st_city_zip (dependencies, ndistinct) ON city, zip FROM addresses;  -- cột tương quan

-- MySQL
ANALYZE TABLE orders;
ANALYZE TABLE orders UPDATE HISTOGRAM ON status WITH 64 BUCKETS;   -- 8.0+

-- Oracle
EXEC DBMS_STATS.GATHER_TABLE_STATS(ownname => 'APP', tabname => 'ORDERS', cascade => TRUE);
SELECT column_name, num_distinct, histogram FROM user_tab_col_statistics WHERE table_name = 'ORDERS';
```

**Vấn đề tương quan (correlation):** optimizer mặc định coi các điều kiện **độc lập**: `city = 'HN' AND district = 'Ba Đình'` → ước lượng = P(city) × P(district), thấp hơn thực tế rất nhiều vì district đã hàm ý city. → Extended statistics (PostgreSQL `CREATE STATISTICS`, Oracle column group `DBMS_STATS.CREATE_EXTENDED_STATS`).

> ⚠️ **Lỗi thường gặp:**
> - Bulk load hàng triệu dòng vào bảng rồi query ngay trong cùng job khi statistics vẫn nói bảng "rỗng" → nested loop thảm họa. Luôn `ANALYZE` sau bulk load.
> - Bảng staging/temp table: PostgreSQL autovacuum **không** analyze temp table.
> - Oracle: job thu thập statistics ban đêm đổi plan → "sáng nay hệ thống chậm mà không ai deploy gì". Dùng SQL Plan Management (SQL plan baseline) cho query trọng yếu.

### 🛠 Bài tập phần 4

**Bài 4.1 — Đọc plan (Cơ bản)**
- Đề bài: với `orders_big` và một bảng `customers_big` 100.000 dòng, chạy `EXPLAIN (ANALYZE, BUFFERS)` cho: (a) lookup một khách theo id; (b) join toàn bộ hai bảng tính tổng tiền theo city; (c) lấy 10 đơn mới nhất của một khách.
- Tiêu chí: với mỗi plan, chỉ ra thuật toán join, node tốn thời gian nhất, số dòng ước lượng vs thực tế, buffer hit/read.

**Bài 4.2 — Ép optimizer đổi thuật toán (Trung bình)**
- Đề bài: dùng `SET enable_hashjoin = off; SET enable_mergejoin = off;` (PostgreSQL) để buộc nested loop cho query (b) ở trên; so sánh thời gian. Sau đó làm ngược lại cho query (a).
- Tiêu chí: bảng so sánh 3 thuật toán × 2 query; giải thích vì sao optimizer chọn đúng ở trạng thái mặc định. Ghi chú: đây chỉ là thí nghiệm, không dùng trên production.

**Bài 4.3 — Statistics sai & tương quan (Nâng cao)**
- Đề bài: tạo bảng `addresses(city, district, ...)` 1 triệu dòng, mỗi district thuộc đúng 1 city. Query `WHERE city = 'HN' AND district = 'Ba Đình'` join với bảng khác. Quan sát ước lượng `rows`, sau đó tạo extended statistics và so sánh plan. Thử thêm: insert 1 triệu dòng mới rồi query ngay khi tắt autovacuum cho bảng đó.
- Tiêu chí: chứng minh bằng plan rằng ước lượng sai dẫn tới chọn sai thuật toán join, và extended stats/ANALYZE sửa được.

<details>
<summary>Gợi ý lời giải</summary>

**4.1** (a) `Index Scan using customers_big_pkey`, rows=1. (b) `Hash Join` với `customers_big` làm build side (nhỏ hơn), `HashAggregate` ở trên, `Seq Scan` cả hai. (c) Nếu có index `(customer_id, created_at DESC)`: `Limit → Index Scan`, không có `Sort`.

**4.2** Nested loop cho (b) phải lookup 2 triệu lần → chậm hơn hash join hàng chục lần (nếu inner có index) hoặc không chạy xong (nếu không có index). Hash join cho (a) phải quét toàn bảng để build → chậm hơn index lookup hàng nghìn lần.

**4.3**
```sql
ALTER TABLE addresses SET (autovacuum_enabled = false);
-- trước: rows ước lượng = N * P(city) * P(district) rất nhỏ → Nested Loop
CREATE STATISTICS st_addr (dependencies) ON city, district FROM addresses;
ANALYZE addresses;
-- sau: rows ≈ thực tế → Hash Join
```
</details>

---

<a id="phan-5"></a>
## 5. Quy trình tối ưu query & slow query log

### 5.1 Quy trình có hệ thống (đừng đoán mò)

1. **Đo & tìm đúng query**: không tối ưu query chạy 1 lần/ngày mất 2s mà bỏ qua query chạy 2.000 lần/giây mất 20ms. Xếp hạng theo **tổng thời gian** (`calls × mean_time`).
2. **Tái hiện** với tham số thật (bind value thật — data skew!) và dữ liệu cỡ production.
3. **Đọc plan thực tế** (`EXPLAIN ANALYZE` / `DISPLAY_CURSOR`), tìm: node tốn nhất, chênh lệch ước lượng, full scan bất thường, sort/hash spill ra đĩa, loops lớn.
4. **Giả thuyết & sửa** theo thứ tự rẻ → đắt: viết lại query (sargable, bỏ `SELECT *`, bỏ N+1) → statistics → index → thay đổi schema (denormalize, partition) → cache → thay kiến trúc.
5. **Đo lại**, kiểm tra **tác động phụ** (index mới làm chậm ghi? plan query khác có đổi?).
6. **Giám sát hồi quy** (regression) sau deploy.

### 5.2 Công cụ tìm slow query

```sql
-- PostgreSQL: extension pg_stat_statements (thêm vào shared_preload_libraries)
CREATE EXTENSION pg_stat_statements;
SELECT query, calls, round(total_exec_time) total_ms, round(mean_exec_time,2) mean_ms, rows
FROM pg_stat_statements ORDER BY total_exec_time DESC LIMIT 10;
-- log từng query chậm: log_min_duration_statement = 500  (ms)
-- auto_explain: tự log plan của query chậm (auto_explain.log_min_duration)
-- Query đang chạy / bị chặn:
SELECT pid, now() - query_start AS dur, state, wait_event_type, wait_event, query
FROM pg_stat_activity WHERE state <> 'idle' ORDER BY dur DESC;
```

```sql
-- MySQL
SET GLOBAL slow_query_log = ON;
SET GLOBAL long_query_time = 0.5;                 -- giây
SET GLOBAL log_queries_not_using_indexes = ON;    -- cẩn thận: rất ồn
-- Phân tích file: mysqldumpslow -s t -t 10 slow.log   hoặc  pt-query-digest slow.log (Percona Toolkit)
SELECT * FROM sys.statement_analysis ORDER BY total_latency DESC LIMIT 10;   -- performance_schema
SHOW PROCESSLIST;
```

```sql
-- Oracle
SELECT sql_id, executions, ROUND(elapsed_time/1e6) AS elapsed_s,
       ROUND(elapsed_time/NULLIF(executions,0)/1e3, 2) AS avg_ms, buffer_gets, sql_text
FROM v$sql ORDER BY elapsed_time DESC FETCH FIRST 10 ROWS ONLY;
-- AWR / ASH report (cần Diagnostics Pack license), SQL Monitor cho query dài
```

### 5.3 Các pattern chậm hay gặp phía ứng dụng Java

| Vấn đề | Triệu chứng | Cách sửa |
|---|---|---|
| **N+1 query** (JPA lazy loading) | 1 query list + N query con, log đầy SELECT giống nhau | `JOIN FETCH`, `@EntityGraph`, `@BatchSize`, DTO projection |
| `SELECT *` | Không covering được, tốn băng thông, đọc cả LOB | Chọn cột cần thiết |
| Chunk quá lớn / quá nhỏ | IN list 10.000 phần tử; hoặc gọi DB trong vòng lặp | Batch 500–1000; temp table/`= ANY(?)` array |
| Không dùng bind variable | Oracle hard parse liên tục, shared pool phân mảnh | `PreparedStatement` (xem phần 11) |
| Transaction dài giữ lock | Query khác chờ lock (`wait_event_type = Lock`) | Thu nhỏ transaction, không gọi HTTP trong transaction |
| `COUNT(*)` trên bảng lớn cho phân trang | Mỗi trang tốn vài giây | Ước lượng (`pg_class.reltuples`), cache, "có trang tiếp theo không" thay vì tổng |
| ORM sinh `OFFSET` lớn | Trang càng sau càng chậm | Keyset pagination (phần 9) |

> 💡 **Góc nhìn Senior:** "Query chậm" thường **không phải** do bản thân query mà do **chờ** — chờ lock, chờ I/O, chờ connection pool (HikariCP `connectionTimeout`), CPU DB bão hòa vì query khác. Luôn nhìn **wait events** trước khi kết luận. Tương tự, query chạy nhanh trên SQL client nhưng chậm từ ứng dụng thường do **khác tham số** (bind peeking/plan cache với giá trị lệch), **khác kiểu dữ liệu** (implicit conversion từ JDBC), hoặc **khác session setting** (`optimizer_mode`, `search_path`, NLS).

### 🛠 Bài tập phần 5

**Bài 5.1 — Bật và đọc slow query log (Cơ bản)**
- Đề bài: bật slow log trên MySQL (`long_query_time = 0.1`) và `pg_stat_statements` trên PostgreSQL. Chạy một script sinh tải gồm 5 loại query (có 2 loại thiếu index).
- Tiêu chí: tìm ra top 3 query theo tổng thời gian bằng `pt-query-digest`/`mysqldumpslow` và `pg_stat_statements`; viết báo cáo ngắn.

**Bài 5.2 — Diệt N+1 (Trung bình)**
- Đề bài: Spring Boot + JPA, entity `Customer` 1-N `Order`. Endpoint `/customers` trả 100 khách kèm danh sách đơn. Bật `spring.jpa.properties.hibernate.generate_statistics=true` và đếm số query.
- Tiêu chí: giảm từ 101 query xuống ≤ 2 bằng hai cách khác nhau (`JOIN FETCH` và `@BatchSize`/`default_batch_fetch_size`); nêu nhược điểm của `JOIN FETCH` khi kết hợp phân trang (HHH90003004 / "firstResult/maxResults specified with collection fetch; applying in memory").

**Bài 5.3 — Điều tra sự cố "lúc nhanh lúc chậm" (Nâng cao)**
- Đề bài: bảng `tickets(status, assignee_id, ...)` 5 triệu dòng, 98% `CLOSED`. Query bằng PreparedStatement `WHERE status = ?`. Trên PostgreSQL, sau 5 lần execute, driver chuyển sang **generic plan** (`plan_cache_mode`, `prepareThreshold`). Tái hiện việc query với `status = 'OPEN'` bị dùng plan full scan.
- Tiêu chí: tái hiện được bằng Java (pgJDBC `prepareThreshold=5` mặc định) hoặc `PREPARE`/`EXECUTE` trong psql; đưa ra 2 cách khắc phục và so sánh (`plan_cache_mode = force_custom_plan`, partial index, tách query theo giá trị). Liên hệ với *bind peeking* & *adaptive cursor sharing* của Oracle.

<details>
<summary>Gợi ý lời giải</summary>

**5.2**
```java
@Query("select distinct c from Customer c left join fetch c.orders where c.id in :ids")
List<Customer> findWithOrders(@Param("ids") List<Long> ids);
// Hoặc: spring.jpa.properties.hibernate.default_batch_fetch_size=100 → 1 + ceil(100/100) query
```
Phân trang + collection fetch: Hibernate tải toàn bộ rồi cắt trong bộ nhớ. Giải pháp: query 1 lấy page id, query 2 fetch theo `id in (...)`.

**5.3**
```sql
PREPARE q(text) AS SELECT * FROM tickets WHERE status = $1;
EXECUTE q('CLOSED'); -- x5 lần → planner cân nhắc generic plan
EXPLAIN EXECUTE q('OPEN');   -- quan sát: "status = $1" (generic) thay vì giá trị cụ thể
SET plan_cache_mode = force_custom_plan;
```
Oracle: bind peeking lấy giá trị lần parse đầu tiên; 11g+ adaptive cursor sharing tạo child cursor mới khi phát hiện bind-sensitive. Histogram trên `status` là điều kiện cần.
</details>

---

<a id="phan-6"></a>
## 6. Transaction, ACID & isolation level

### 6.1 ACID

| Tính chất | Ý nghĩa | Cơ chế hiện thực |
|---|---|---|
| **Atomicity** | Tất cả hoặc không gì cả | Undo log (InnoDB, Oracle), tuple version + commit log (PostgreSQL) |
| **Consistency** | Chuyển DB từ trạng thái hợp lệ sang hợp lệ (constraint, invariant) | Constraint + **logic ứng dụng** (Kleppmann: chữ C thực ra là trách nhiệm của app) |
| **Isolation** | Transaction đồng thời không thấy trạng thái dở dang của nhau (ở mức độ được chọn) | Lock, MVCC, SSI |
| **Durability** | Commit rồi thì không mất kể cả khi crash | **Write-Ahead Log**: redo log (InnoDB, Oracle), WAL (PostgreSQL) được fsync lúc commit |

> 💡 **Góc nhìn Senior — Durability có giá:** InnoDB `innodb_flush_log_at_trx_commit=1` (mặc định, fsync mỗi commit) vs `2`/`0` (nhanh hơn, có thể mất ~1s giao dịch khi crash OS/mất điện). PostgreSQL `synchronous_commit = off` tương tự. Với replication async, "commit thành công" **chưa** có nghĩa là bản ghi đã nằm ở replica — failover có thể mất dữ liệu đã commit.

### 6.2 Các hiện tượng bất thường (anomalies)

| Anomaly | Mô tả | Ví dụ |
|---|---|---|
| **Dirty read** | Đọc dữ liệu chưa commit của transaction khác | T1 trừ tiền chưa commit, T2 đọc số dư đã trừ; T1 rollback |
| **Dirty write** | Ghi đè dữ liệu chưa commit của transaction khác | Mọi DB thực tế đều ngăn (row lock) |
| **Non-repeatable read** (read skew) | Đọc cùng dòng hai lần trong 1 transaction ra kết quả khác | T1 đọc giá = 100; T2 update giá = 120 & commit; T1 đọc lại = 120 |
| **Phantom read** | Cùng một điều kiện, lần đọc sau xuất hiện/biến mất **dòng** | T1 `COUNT(*) WHERE dept='IT'` = 5; T2 insert nhân viên IT; T1 đếm lại = 6 |
| **Lost update** | Hai transaction read-modify-write cùng dòng, một cập nhật bị ghi đè | Cả hai đọc stock = 10, cùng ghi 9 → bán 2 nhưng chỉ trừ 1 |
| **Write skew** | Hai transaction đọc cùng tập dữ liệu, mỗi bên ghi vào **dòng khác nhau** dựa trên điều kiện đã đọc → phá vỡ invariant | Quy tắc "ít nhất 1 bác sĩ trực": 2 bác sĩ cùng thấy có 2 người trực, cùng xin nghỉ → 0 người trực |

### 6.3 Isolation level theo chuẩn SQL và thực tế từng DB

Chuẩn SQL định nghĩa 4 mức theo anomaly mà nó ngăn được:

| Level (chuẩn) | Dirty read | Non-repeatable read | Phantom |
|---|---|---|---|
| READ UNCOMMITTED | có thể | có thể | có thể |
| READ COMMITTED | ✅ ngăn | có thể | có thể |
| REPEATABLE READ | ✅ | ✅ | có thể |
| SERIALIZABLE | ✅ | ✅ | ✅ |

**Thực tế khác chuẩn rất nhiều** (đây là phần phỏng vấn viên Senior thích hỏi):

| | **MySQL InnoDB** | **PostgreSQL** | **Oracle** |
|---|---|---|---|
| **Mặc định** | **REPEATABLE READ** | **READ COMMITTED** | **READ COMMITTED** |
| Mức hỗ trợ | Đủ 4 mức | RU (hoạt động như RC), RC, RR, SERIALIZABLE | Chỉ RC, SERIALIZABLE (+ READ ONLY) |
| RR thực chất là | Snapshot cho đọc thường; đọc có lock (`FOR UPDATE`, UPDATE/DELETE) đọc **bản mới nhất** + next-key lock | **Snapshot Isolation** thật sự | — |
| Phantom ở RR | Đọc snapshot: không thấy; locking read: ngăn bằng gap lock | Không xảy ra (snapshot) | — |
| Lost update ở RR | **Không phát hiện** khi app đọc rồi ghi (read-modify-write ở code) | **Phát hiện**: lỗi `could not serialize access due to concurrent update` → app phải retry | — (RC: không phát hiện) |
| SERIALIZABLE | Mọi `SELECT` thường thành `SELECT ... FOR SHARE` (lock-based) → nhiều lock, dễ deadlock | **SSI** (Serializable Snapshot Isolation, từ 9.1) – optimistic, ngăn cả write skew, có thể ném lỗi serialization | Thực chất là **Snapshot Isolation** – lỗi `ORA-08177`, **không** ngăn write skew |
| Write skew | Có ở RR | Có ở RR, **ngăn** ở SERIALIZABLE | Có ở cả SERIALIZABLE |

```sql
-- Đặt isolation level
SET TRANSACTION ISOLATION LEVEL REPEATABLE READ;                  -- chuẩn, chạy cả 3 DB (Oracle chỉ RC/SERIALIZABLE)
SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED;           -- MySQL cho session
SELECT @@transaction_isolation;                                   -- MySQL 8
SHOW transaction_isolation;                                       -- PostgreSQL
```

```java
// Spring
@Transactional(isolation = Isolation.REPEATABLE_READ)
public void transfer(...) { ... }
// JDBC: connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
```

### 6.4 Demo lost update & write skew (chạy bằng 2 cửa sổ psql)

```sql
CREATE TABLE account (id INT PRIMARY KEY, balance INT);
INSERT INTO account VALUES (1, 100);

-- Lost update ở READ COMMITTED (PostgreSQL mặc định)
-- Session A                                  -- Session B
BEGIN;                                        BEGIN;
SELECT balance FROM account WHERE id=1; -- 100
                                              SELECT balance FROM account WHERE id=1; -- 100
UPDATE account SET balance = 90 WHERE id=1;   -- (app tính 100-10)
COMMIT;
                                              UPDATE account SET balance = 80 WHERE id=1;  -- (app tính 100-20)
                                              COMMIT;   -- balance = 80, mất khoản trừ 10 của A!
-- Ở REPEATABLE READ (PostgreSQL): UPDATE của B báo lỗi
-- ERROR: could not serialize access due to concurrent update
```

**Cách chống lost update** (theo thứ tự ưu tiên):
1. **Atomic update** do DB tự tính: `UPDATE account SET balance = balance - 10 WHERE id = 1 AND balance >= 10;` → kiểm tra số dòng ảnh hưởng.
2. **Pessimistic lock**: `SELECT ... FOR UPDATE` rồi mới tính.
3. **Optimistic lock**: cột `version`: `UPDATE ... SET balance = ?, version = version + 1 WHERE id = ? AND version = ?` — 0 dòng ảnh hưởng ⇒ có người sửa trước ⇒ retry/báo lỗi. JPA `@Version` làm sẵn (ném `OptimisticLockException`).
4. Isolation level cao hơn (PostgreSQL RR/SERIALIZABLE) + **retry**.

**Write skew** (bác sĩ trực):

```sql
CREATE TABLE doctors (name TEXT PRIMARY KEY, on_call BOOLEAN);
INSERT INTO doctors VALUES ('Alice', true), ('Bob', true);
-- Cả A và B, ở REPEATABLE READ:
BEGIN ISOLATION LEVEL REPEATABLE READ;
SELECT count(*) FROM doctors WHERE on_call;          -- 2 → "còn người khác trực, mình nghỉ được"
UPDATE doctors SET on_call = false WHERE name = 'Alice';   -- B: name = 'Bob'
COMMIT;                                              -- cả hai commit thành công → 0 người trực!
-- SERIALIZABLE ở PostgreSQL: một bên bị lỗi 40001 serialization_failure
-- Cách khác (mọi DB): SELECT ... FROM doctors WHERE on_call FOR UPDATE; (khóa các dòng làm tiền đề)
-- Khi tiền đề là "không tồn tại dòng" (đặt phòng trống) → không có gì để khóa → materialize conflict
-- (tạo sẵn bảng slot để khóa) hoặc dùng SERIALIZABLE / exclusion constraint.
```

> 💡 **Góc nhìn Senior:**
> - Chạy ở SERIALIZABLE (PostgreSQL) hoặc RR (PostgreSQL) thì ứng dụng **bắt buộc có retry** cho SQLSTATE `40001` (và deadlock `40P01`). Viết retry ở tầng ngoài cùng của transaction (Spring Retry `@Retryable` bọc ngoài `@Transactional`, không phải bên trong).
> - Nhiều team chuyển MySQL từ RR sang **READ COMMITTED** để giảm gap lock & deadlock (đặc biệt khi binlog format là ROW — điều kiện bắt buộc để dùng RC an toàn với replication). Hiểu rõ trade-off: mất repeatable read cho các query đọc nhiều lần trong một transaction.
> - Spring `@Transactional` mặc định `Isolation.DEFAULT` = dùng mặc định của DB → **cùng một code chạy trên MySQL và PostgreSQL có hành vi khác nhau**.

> ⚠️ **Lỗi thường gặp:**
> - Tin rằng "đã có `@Transactional` thì không bị race condition". Transaction ≠ serializability ở mức mặc định.
> - `@Transactional` trên method `private` hoặc gọi nội bộ (self-invocation) → không có transaction.
> - Giữ transaction mở trong lúc gọi API ngoài → giữ lock & connection, kéo dài history MVCC.

### 🛠 Bài tập phần 6

**Bài 6.1 — Tái hiện anomaly (Cơ bản)**
- Đề bài: dùng 2 session, tái hiện **non-repeatable read** ở READ COMMITTED và chứng minh nó biến mất ở REPEATABLE READ, trên cả MySQL và PostgreSQL. Thử tái hiện **dirty read** trên MySQL (`READ UNCOMMITTED`) và PostgreSQL (giải thích vì sao không được).
- Tiêu chí: có log các bước theo thời gian (T1, T2...), kết quả từng SELECT.

**Bài 6.2 — Bốn cách chống lost update (Trung bình)**
- Đề bài: service Spring Boot `POST /products/{id}/purchase` trừ tồn kho. Viết JMeter/k6 hoặc `ExecutorService` bắn 200 request đồng thời vào sản phẩm có stock = 100.
- Yêu cầu: hiện thực 4 phiên bản (ngây thơ, atomic update, `FOR UPDATE`/`@Lock(PESSIMISTIC_WRITE)`, `@Version` + retry). Tiêu chí: phiên bản ngây thơ cho ra số âm hoặc bán vượt; 3 phiên bản còn lại đúng 100 đơn thành công, stock = 0; so sánh throughput & p99 latency.

**Bài 6.3 — Write skew trong đặt phòng họp (Nâng cao)**
- Đề bài: bảng `meeting(room, start_ts, end_ts)`. Quy tắc: không chồng giờ. Logic app: `SELECT count(*) ... WHERE room=? AND overlaps` → nếu 0 thì `INSERT`.
- Yêu cầu: chứng minh logic trên bị write skew ở RC và RR (PostgreSQL & MySQL). Đưa ra và cài đặt 3 giải pháp: (1) SERIALIZABLE + retry (PostgreSQL), (2) materialize conflict (bảng `room_slot` 15 phút + `FOR UPDATE`), (3) exclusion constraint. Tiêu chí: test đồng thời 100 request không tạo booking chồng giờ; so sánh độ phức tạp.

<details>
<summary>Gợi ý lời giải</summary>

**6.1** PostgreSQL coi `READ UNCOMMITTED` như `READ COMMITTED` vì MVCC luôn chỉ trả về version đã commit → không bao giờ có dirty read.

**6.2**
```java
// Atomic update
@Modifying
@Query("update Product p set p.stock = p.stock - 1 where p.id = :id and p.stock > 0")
int decrement(@Param("id") Long id);   // trả 0 → hết hàng

// Pessimistic
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select p from Product p where p.id = :id")
Optional<Product> findForUpdate(@Param("id") Long id);

// Optimistic: @Version Long version; bọc retry ngoài transaction
@Retryable(retryFor = ObjectOptimisticLockingFailureException.class, maxAttempts = 5,
           backoff = @Backoff(delay = 20, random = true))
public void purchase(Long id) { txTemplate.executeWithoutResult(s -> doPurchase(id)); }
```
Atomic update thường nhanh nhất; optimistic tệ nhất khi contention cao (retry storm) nhưng tốt khi contention thấp.

**6.3** (1) `BEGIN ISOLATION LEVEL SERIALIZABLE` – một bên nhận `40001`. (2) Pre-insert slot rows, mỗi booking `SELECT ... FROM room_slot WHERE room=? AND slot BETWEEN ? AND ? FOR UPDATE` theo **thứ tự slot tăng dần** (tránh deadlock). (3) `EXCLUDE USING gist (room WITH =, tstzrange(start_ts,end_ts) WITH &&)` — đơn giản & mạnh nhất nhưng chỉ có ở PostgreSQL.
</details>

---

<a id="phan-7"></a>
## 7. MVCC — bên dưới nắp capo

### 7.1 Ý tưởng
**MVCC (Multi-Version Concurrency Control)**: thay vì khóa để đọc, DB giữ **nhiều phiên bản** của một dòng; mỗi transaction đọc phiên bản phù hợp với **snapshot** của nó. Kết quả: **reader không chặn writer, writer không chặn reader** (writer vẫn chặn writer trên cùng dòng). Đây là lý do PostgreSQL/Oracle/InnoDB có concurrency cao hơn hẳn mô hình chỉ dùng lock (SQL Server mặc định, DB2).

Snapshot được lấy khi nào?
- **READ COMMITTED**: mỗi **câu lệnh** một snapshot mới → thấy dữ liệu của transaction khác vừa commit giữa hai câu lệnh.
- **REPEATABLE READ / Snapshot**: một snapshot cho **cả transaction** (PostgreSQL: lúc câu lệnh đầu tiên; InnoDB: lúc consistent read đầu tiên, hoặc ngay khi `START TRANSACTION WITH CONSISTENT SNAPSHOT`).

### 7.2 InnoDB & Oracle: bản mới tại chỗ + undo log

- Mỗi dòng InnoDB có cột ẩn `DB_TRX_ID` (transaction sửa gần nhất) và `DB_ROLL_PTR` (con trỏ tới bản ghi undo).
- UPDATE sửa dòng **tại chỗ** trong clustered index, bản cũ được ghi vào **undo log** → tạo thành chuỗi phiên bản (version chain).
- Khi đọc, InnoDB dùng **read view** (danh sách transaction đang active lúc tạo snapshot): nếu `DB_TRX_ID` không nhìn thấy được → lần theo `DB_ROLL_PTR` dựng lại bản cũ từ undo.
- **Purge thread** dọn undo khi không còn read view nào cần. Chỉ số theo dõi: **History list length** trong `SHOW ENGINE INNODB STATUS`.
- Oracle tương tự: block được sửa tại chỗ, bản cũ ở **undo tablespace**; đọc nhất quán dựa trên **SCN** (System Change Number) — block có SCN mới hơn snapshot sẽ được dựng lại bằng undo (consistent read / CR block).

```sql
-- MySQL: theo dõi transaction dài và history list
SELECT trx_id, trx_started, TIMESTAMPDIFF(SECOND, trx_started, NOW()) AS age_s, trx_query
FROM information_schema.innodb_trx ORDER BY trx_started;
SHOW ENGINE INNODB STATUS\G      -- tìm "History list length"
```

> ⚠️ **Sự cố production kinh điển:**
> - **InnoDB**: một transaction mở hàng giờ (job báo cáo, connection bị leak giữ `autocommit=0`, session debug quên commit) → purge không dọn được undo → history list tăng hàng triệu → mọi query chậm dần (phải lần chain dài), undo tablespace phình to.
> - **Oracle `ORA-01555: snapshot too old`**: query dài cần bản cũ nhưng undo đã bị ghi đè (undo_retention không đủ). Thường gặp khi fetch-across-commit (vừa duyệt cursor vừa commit trong vòng lặp PL/SQL).

### 7.3 PostgreSQL: mỗi phiên bản là một tuple mới

- Mỗi tuple có `xmin` (transaction tạo ra) và `xmax` (transaction xóa/cập nhật). UPDATE = đánh dấu `xmax` cho tuple cũ + **insert tuple mới** (không có undo log riêng — bản cũ nằm ngay trong heap).
- Tuple "chết" (dead tuple) chiếm chỗ cho tới khi **VACUUM** dọn → nếu không dọn kịp: **bloat** (bảng/index phình to, scan chậm).
- **autovacuum** chạy theo ngưỡng `autovacuum_vacuum_scale_factor` (mặc định 0.2 = 20% số dòng thay đổi) — với bảng 500 triệu dòng thì phải chờ 100 triệu dead tuple mới chạy → nên chỉnh riêng cho bảng lớn.
- `VACUUM` thường: đánh dấu chỗ trống tái sử dụng, cập nhật **visibility map** (giúp index-only scan), không trả dung lượng cho OS, không khóa ghi. `VACUUM FULL`: viết lại bảng, trả dung lượng, nhưng **khóa ACCESS EXCLUSIVE** → dùng `pg_repack` trên production.
- **Transaction ID wraparound**: XID là 32 bit; VACUUM phải "freeze" tuple cũ. Nếu bị chặn quá lâu, PostgreSQL buộc dừng nhận ghi để bảo vệ dữ liệu — sự cố nghiêm trọng hiếm nhưng có thật.

```sql
SELECT xmin, xmax, ctid, * FROM account;                        -- xem phiên bản
SELECT relname, n_live_tup, n_dead_tup, last_autovacuum
FROM pg_stat_user_tables ORDER BY n_dead_tup DESC LIMIT 10;      -- bảng nhiều dead tuple
SELECT pid, now() - xact_start AS age, state, query
FROM pg_stat_activity WHERE xact_start IS NOT NULL ORDER BY age DESC;  -- transaction cũ chặn vacuum
ALTER TABLE orders SET (autovacuum_vacuum_scale_factor = 0.01, fillfactor = 90);
```

| | InnoDB / Oracle | PostgreSQL |
|---|---|---|
| Bản cũ ở đâu | Undo log/tablespace | Ngay trong heap (dead tuple) |
| UPDATE | Sửa tại chỗ | Insert tuple mới (+ cập nhật mọi index trừ khi HOT) |
| Dọn dẹp | Purge (tự động, nền) | VACUUM / autovacuum |
| Rollback | Tốn (áp undo ngược) | Rẻ (chỉ đánh dấu transaction aborted) |
| Hậu quả transaction dài | History list dài / ORA-01555 | Bloat, vacuum không dọn được |

> 💡 **Góc nhìn Senior:** MVCC chuyển chi phí từ **lock** sang **storage & dọn dẹp**. Câu hỏi "vì sao Uber chuyển từ PostgreSQL sang MySQL (2016)" xoay quanh đúng điểm này: write amplification do UPDATE phải cập nhật mọi index và WAL lớn trong replication. Hãy biết cả hai mặt — PostgreSQL đã cải thiện nhiều (HOT, index deduplication v13...).

### 🛠 Bài tập phần 7

**Bài 7.1 — Quan sát phiên bản (Cơ bản)**
- Đề bài: trên PostgreSQL, tạo bảng nhỏ, UPDATE cùng một dòng 5 lần; mỗi lần xem `xmin, xmax, ctid`. Dùng extension `pageinspect` (`heap_page_items`) để thấy các tuple chết.
- Tiêu chí: giải thích từng giá trị; chạy `VACUUM` và quan sát thay đổi.

**Bài 7.2 — Bloat do transaction dài (Trung bình)**
- Đề bài: mở session A `BEGIN ISOLATION LEVEL REPEATABLE READ; SELECT 1;` và để nguyên. Session B update 1 triệu lần một bảng 1.000 dòng. Đo kích thước bảng (`pg_total_relation_size`), `n_dead_tup`, thời gian `SELECT count(*)` trước và sau khi đóng session A + VACUUM.
- Tiêu chí: số liệu chứng minh transaction dài chặn vacuum; đề xuất cấu hình phòng ngừa (`idle_in_transaction_session_timeout`, giám sát `xact_start`).

**Bài 7.3 — History list InnoDB (Nâng cao)**
- Đề bài: tái hiện history list length tăng trên MySQL bằng một transaction RR mở dài + workload update nặng (sysbench hoặc Java). Vẽ biểu đồ latency của một query đọc theo thời gian.
- Tiêu chí: giải thích bằng cơ chế read view + undo chain; đề xuất cảnh báo (alert) cụ thể bạn sẽ đặt trên production.

<details>
<summary>Gợi ý lời giải</summary>

**7.1**
```sql
CREATE EXTENSION pageinspect;
SELECT lp, t_xmin, t_xmax, t_ctid FROM heap_page_items(get_raw_page('account', 0));
```
Sau 5 update thấy 6 tuple; 5 tuple có `t_xmax` ≠ 0. Sau VACUUM, line pointer của tuple chết được giải phóng (hoặc thành redirect nếu là HOT chain).

**7.2** Với session A mở, VACUUM báo "dead row versions cannot be removed yet". Phòng ngừa: `idle_in_transaction_session_timeout = '60s'`, alert khi `max(now() - xact_start) > 5 phút`, HikariCP `leakDetectionThreshold`.

**7.3** Alert: `History list length > 1.000.000` hoặc transaction trong `innodb_trx` sống > 10 phút; kèm thông tin `trx_mysql_thread_id` để kill.
</details>

---

<a id="phan-8"></a>
## 8. Locking & deadlock

### 8.1 Row lock & table lock

- **Row-level lock**: InnoDB khóa trên **index record** (không phải trên dòng vật lý!) — UPDATE với WHERE không dùng được index sẽ khóa **mọi dòng đã quét** (ở RR) → gần như khóa cả bảng.
- **Shared (S) vs Exclusive (X)**: `SELECT ... FOR SHARE` (MySQL 8 / PostgreSQL; MySQL cũ: `LOCK IN SHARE MODE`) vs `FOR UPDATE`/UPDATE/DELETE.
- **Intention lock** (IS/IX) ở mức bảng giúp kiểm tra nhanh xung đột với table lock.
- **Table lock / metadata lock**: DDL (`ALTER TABLE`) cần lock mạnh. MySQL **metadata lock (MDL)**: một `ALTER` chờ sau một transaction dài đang đọc bảng → mọi query mới sau `ALTER` cũng xếp hàng chờ → "cả bảng đứng hình". PostgreSQL tương tự với `ACCESS EXCLUSIVE` → luôn đặt `lock_timeout` khi chạy DDL trên production.
- Oracle: **không bao giờ leo thang lock** (lock escalation) và không có read lock; thông tin lock dòng nằm ngay trong block (ITL). SQL Server thì có lock escalation.

### 8.2 Gap lock & next-key lock (InnoDB, REPEATABLE READ)

Để ngăn phantom cho **locking read**, InnoDB khóa cả **khoảng trống** giữa các index record:
- **Record lock**: khóa một index record.
- **Gap lock**: khóa khoảng giữa hai record (không cho INSERT vào khoảng đó). Gap lock giữa các transaction **không xung đột nhau** — chỉ chặn insert.
- **Next-key lock** = record lock + gap lock trước record đó. Là mặc định cho scan ở RR.
- **Insert intention lock**: loại gap lock đặc biệt khi INSERT; xung đột với gap lock của transaction khác.

```sql
-- MySQL, RR. Bảng t(id PK, age INDEX) có age = 10, 20, 30
-- Session A
BEGIN; SELECT * FROM t WHERE age = 20 FOR UPDATE;
-- Khóa: next-key (10, 20] trên index age + gap (20, 30) + record lock PK của dòng đó
-- Session B
INSERT INTO t (id, age) VALUES (100, 15);   -- BỊ CHẶN (nằm trong gap (10,20])
INSERT INTO t (id, age) VALUES (101, 25);   -- BỊ CHẶN (gap (20,30))
INSERT INTO t (id, age) VALUES (102, 35);   -- OK

-- Xem lock đang giữ (MySQL 8):
SELECT engine_transaction_id, index_name, lock_type, lock_mode, lock_status, lock_data
FROM performance_schema.data_locks;
```

Quy tắc đáng nhớ: tìm kiếm **bằng trên unique index** khớp đúng một dòng → chỉ record lock (không gap). Tìm theo non-unique index, theo range, hoặc **không tìm thấy dòng nào** → gap/next-key lock. Vì vậy pattern "SELECT ... FOR UPDATE xem có chưa, chưa thì INSERT" với hai transaction cùng key chưa tồn tại → cả hai lấy gap lock (không xung đột), rồi cả hai INSERT → chờ nhau → **deadlock**.

Ở **READ COMMITTED**, InnoDB tắt gap lock cho tìm kiếm/scan (chỉ còn dùng cho kiểm tra FK và duplicate key) và nhả lock của dòng không khớp WHERE sớm → ít deadlock hơn.

### 8.3 SELECT FOR UPDATE và các biến thể

```sql
SELECT * FROM jobs WHERE status = 'NEW' ORDER BY id LIMIT 10 FOR UPDATE SKIP LOCKED;
-- Mẫu "job queue" trên DB: nhiều worker lấy job không trùng nhau, không chờ nhau.
-- Có ở PostgreSQL 9.5+, MySQL 8.0+, Oracle (SKIP LOCKED có từ lâu).

SELECT * FROM account WHERE id = 1 FOR UPDATE NOWAIT;          -- lỗi ngay nếu bị khóa
SELECT * FROM account WHERE id = 1 FOR UPDATE WAIT 3;          -- Oracle: chờ tối đa 3s
SET lock_timeout = '3s';                                       -- PostgreSQL
SET innodb_lock_wait_timeout = 3;                              -- MySQL (mặc định 50s!)
-- PostgreSQL còn có FOR NO KEY UPDATE (UPDATE thường dùng) — không chặn insert bảng con có FK
```

```java
// Spring Data JPA
@Lock(LockModeType.PESSIMISTIC_WRITE)
@QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
Optional<Account> findById(Long id);
```

### 8.4 Deadlock: nguyên nhân, chẩn đoán, phòng tránh

Deadlock = chu trình chờ: T1 giữ A chờ B, T2 giữ B chờ A. DB phát hiện (InnoDB ngay lập tức bằng wait-for graph; PostgreSQL sau `deadlock_timeout` = 1s; Oracle ~3s) và **rollback một transaction** (victim): MySQL `ERROR 1213 (40001)`, PostgreSQL `40P01`, Oracle `ORA-00060` (Oracle chỉ rollback **câu lệnh**, không phải cả transaction!).

```sql
-- Tái hiện kinh điển: chuyển tiền ngược chiều
-- T1: UPDATE account SET balance=balance-10 WHERE id=1;   T2: UPDATE account SET balance=balance-10 WHERE id=2;
-- T1: UPDATE account SET balance=balance+10 WHERE id=2;   T2: UPDATE account SET balance=balance+10 WHERE id=1;  → deadlock
```

**Chẩn đoán:**
- MySQL: `SHOW ENGINE INNODB STATUS\G` → mục `LATEST DETECTED DEADLOCK` (chỉ lần gần nhất) — bật `innodb_print_all_deadlocks = ON` để ghi mọi deadlock vào error log. Đọc: transaction nào, câu lệnh nào, **HOLDS THE LOCK(S)** gì, **WAITING FOR** lock gì (index nào, `lock_mode X locks gap before rec` ...).
- PostgreSQL: server log chứa `DETAIL: Process X waits for ShareLock on transaction Y; blocked by process Z...` kèm câu lệnh; `log_lock_waits = on`; xem chặn hiện tại: `SELECT pid, pg_blocking_pids(pid), query FROM pg_stat_activity WHERE cardinality(pg_blocking_pids(pid)) > 0;`
- Oracle: alert log + **trace file** chứa "Deadlock graph"; `V$LOCK`, `V$SESSION.BLOCKING_SESSION`.

**Phòng tránh:**
1. **Khóa theo thứ tự nhất quán** (ví dụ id tăng dần): `transfer(a, b)` luôn khóa `min(a,b)` trước.
2. Transaction **ngắn**, ít câu lệnh; không tương tác người dùng/HTTP trong transaction.
3. Có index phù hợp cho UPDATE/DELETE để không khóa thừa.
4. Cân nhắc READ COMMITTED (MySQL) để bỏ gap lock.
5. Batch DML theo thứ tự PK, chia nhỏ batch.
6. **Retry** khi gặp deadlock — deadlock là hiện tượng bình thường ở hệ thống đồng thời cao, code phải chịu được.

> 💡 **Góc nhìn Senior:** Phân biệt **deadlock** (DB tự giải quyết trong vài ms–giây) với **lock wait / blocking chain** (không có chu trình, một transaction dài chặn hàng trăm cái khác cho tới timeout). Cái thứ hai thường gây sự cố nặng hơn: connection pool cạn kiệt, ứng dụng timeout dây chuyền. Đặt `lock_timeout`/`innodb_lock_wait_timeout` ngắn cho request OLTP.

> ⚠️ **Lỗi thường gặp:** Oracle — FK không có index trên bảng con → UPDATE PK/DELETE bảng cha lấy **table lock** trên bảng con → deadlock khó hiểu. Kiểm tra bằng query tìm "unindexed foreign keys" trước khi go-live.

### 🛠 Bài tập phần 8

**Bài 8.1 — Tái hiện & đọc deadlock (Cơ bản)**
- Đề bài: tái hiện deadlock chuyển tiền trên MySQL và PostgreSQL. Lấy log deadlock và chú thích từng dòng.
- Tiêu chí: chỉ ra victim, lock đang giữ, lock đang chờ; viết lại code Java để tránh deadlock bằng sắp xếp thứ tự khóa.

**Bài 8.2 — Gap lock deadlock khi "check rồi insert" (Trung bình)**
- Đề bài: MySQL RR, bảng `user_device(user_id, device_id, UNIQUE(user_id, device_id))`. Hai transaction cùng `SELECT ... WHERE user_id=? AND device_id=? FOR UPDATE` (không có dòng) rồi `INSERT`.
- Tiêu chí: tái hiện deadlock, giải thích bằng `performance_schema.data_locks`; đưa ra 2 cách sửa (`INSERT ... ON DUPLICATE KEY UPDATE`/`INSERT IGNORE` + bắt lỗi, hoặc chuyển RC) và chứng minh hết deadlock.

**Bài 8.3 — Job queue trên DB (Nâng cao)**
- Đề bài: hiện thực bảng `outbox_jobs` và 8 worker Java lấy job song song bằng `FOR UPDATE SKIP LOCKED`, xử lý, đánh dấu DONE. Mô phỏng worker chết giữa chừng.
- Tiêu chí: không job nào bị xử lý hai lần khi worker sống; job của worker chết được nhặt lại (giải thích vì sao — transaction rollback nhả lock, hoặc cơ chế lease `locked_until`); đo throughput khi tăng số worker; so sánh với cách không có `SKIP LOCKED`.

<details>
<summary>Gợi ý lời giải</summary>

**8.1**
```java
public void transfer(long from, long to, BigDecimal amt) {
  long first = Math.min(from, to), second = Math.max(from, to);
  Account a = repo.findForUpdate(first).orElseThrow();
  Account b = repo.findForUpdate(second).orElseThrow();
  // trừ/cộng theo from/to
}
```
**8.2** Log sẽ có cả hai transaction giữ `lock_mode X locks gap before rec` và chờ `insert intention`. Sửa: bỏ bước SELECT FOR UPDATE, dùng `INSERT IGNORE` (hoặc `ON DUPLICATE KEY UPDATE id=id`) và kiểm tra affected rows.

**8.3**
```sql
WITH picked AS (
  SELECT id FROM outbox_jobs WHERE status = 'NEW' ORDER BY id LIMIT 10 FOR UPDATE SKIP LOCKED
)
UPDATE outbox_jobs j SET status = 'PROCESSING', locked_until = now() + interval '5 min', worker = ?
FROM picked WHERE j.id = picked.id RETURNING j.*;
```
Hai kiểu: (a) giữ transaction trong suốt lúc xử lý (đơn giản, worker chết → rollback tự nhả, nhưng giữ connection lâu); (b) lease: commit ngay trạng thái PROCESSING + `locked_until`, worker khác nhặt lại job quá hạn — cần handler idempotent.
</details>

---

<a id="phan-9"></a>
## 9. Pagination ở quy mô lớn

### 9.1 OFFSET và vì sao nó chậm

```sql
SELECT * FROM orders ORDER BY created_at DESC, id DESC LIMIT 20 OFFSET 200000;
```
DB vẫn phải **đọc và bỏ đi 200.000 dòng** đầu → độ phức tạp O(offset + limit). Trang 1 nhanh, trang 10.000 chậm; crawler/bot thích nhảy trang sâu → DB bị quá tải. Ngoài ra OFFSET **không ổn định**: có dòng mới chèn vào giữa hai lần gọi → trùng hoặc sót bản ghi giữa các trang.

### 9.2 Keyset (seek) pagination

Nhớ giá trị khóa sắp xếp của dòng cuối trang trước, dùng làm điều kiện:

```sql
-- Index: (created_at DESC, id DESC) — id để phá hòa (tie-breaker), đảm bảo thứ tự duy nhất
-- Trang đầu
SELECT id, created_at, amount FROM orders ORDER BY created_at DESC, id DESC LIMIT 20;
-- Trang tiếp: last = (created_at = '2024-02-15 10:00', id = 106)
SELECT id, created_at, amount FROM orders
WHERE (created_at, id) < ('2024-02-15 10:00', 106)          -- row value comparison: PostgreSQL ✅
ORDER BY created_at DESC, id DESC LIMIT 20;

-- Dạng mở rộng (Oracle không hỗ trợ so sánh tuple <, và MySQL cũ dùng index kém với row constructor):
WHERE created_at < :c OR (created_at = :c AND id < :id)
-- MySQL/Oracle tối ưu hơn khi thêm điều kiện dư thừa giúp seek:
WHERE created_at <= :c AND (created_at < :c OR id < :id)
```

| | OFFSET | Keyset |
|---|---|---|
| Hiệu năng trang sâu | O(offset) | O(log N + limit) — ổn định |
| Nhảy tới trang bất kỳ | ✅ | ❌ (chỉ next/prev) |
| Ổn định khi dữ liệu thay đổi | ❌ trùng/sót | ✅ |
| Hiển thị tổng số trang | dễ (nhưng `COUNT(*)` đắt) | thường bỏ |
| Phù hợp | Admin nhỏ, bảng nhỏ | Infinite scroll, API public, export, batch job |

API thường trả **cursor** mã hóa (Base64 của `{created_at, id}`) thay vì lộ thẳng giá trị. Spring Data 3.1+ có `ScrollPosition`/`Window` hỗ trợ keyset (`KeysetScrollPosition`).

**Deferred join** — khi buộc phải dùng OFFSET (cần nhảy trang):
```sql
SELECT o.* FROM orders o
JOIN (SELECT id FROM orders ORDER BY created_at DESC, id DESC LIMIT 20 OFFSET 200000) t ON t.id = o.id
ORDER BY o.created_at DESC, o.id DESC;
-- Subquery chỉ quét index (covering), chỉ 20 dòng phải đọc bảng → nhanh hơn nhiều với InnoDB
```

**Oracle**: `OFFSET :n ROWS FETCH NEXT 20 ROWS ONLY` (12c+); trước 12c phải dùng ROWNUM lồng 3 tầng (xem phần 11).

> 💡 **Góc nhìn Senior:** Batch job xử lý toàn bảng (re-index, migrate) **tuyệt đối** không dùng OFFSET — dùng keyset theo PK: `WHERE id > :last ORDER BY id LIMIT 1000`. Còn tổng số bản ghi: chỉ hiển thị "khoảng 1,2 triệu" từ statistics, hoặc tính bất đồng bộ và cache.

### 🛠 Bài tập phần 9

**Bài 9.1 — Đo OFFSET (Cơ bản)**
- Đề bài: với `orders_big` 2 triệu dòng, đo thời gian lấy trang ở offset 0, 10k, 100k, 1M, 1.9M.
- Tiêu chí: biểu đồ thời gian theo offset; so với keyset ở cùng vị trí.

**Bài 9.2 — API cursor pagination (Trung bình)**
- Đề bài: Spring Boot endpoint `GET /orders?customerId=&cursor=&size=` trả `{items, nextCursor}`, sắp `created_at DESC, id DESC`.
- Tiêu chí: cursor Base64-URL opaque; không trùng/sót khi chèn dữ liệu mới trong lúc duyệt (viết test chứng minh); plan dùng index, không Sort.

**Bài 9.3 — Export 50 triệu dòng (Nâng cao)**
- Đề bài: xuất bảng 50 triệu dòng ra CSV mà không làm ảnh hưởng production. So sánh 3 cách: OFFSET, keyset theo PK, JDBC streaming (`setFetchSize`, PostgreSQL cần `autocommit=false`; MySQL `fetchSize=Integer.MIN_VALUE` hoặc `useCursorFetch=true`).
- Tiêu chí: đo thời gian, bộ nhớ heap JVM, thời gian transaction mở (liên hệ MVCC phần 7) — chọn và bảo vệ phương án.

<details>
<summary>Gợi ý lời giải</summary>

**9.2**
```java
record Cursor(Instant createdAt, long id) {}
String encode(Cursor c) { return Base64.getUrlEncoder().withoutPadding()
    .encodeToString((c.createdAt().toEpochMilli() + ":" + c.id()).getBytes(StandardCharsets.UTF_8)); }

@Query(value = """
  SELECT * FROM orders WHERE customer_id = :cid
    AND (CAST(:ts AS timestamptz) IS NULL OR (created_at, id) < (:ts, :id))
  ORDER BY created_at DESC, id DESC LIMIT :size""", nativeQuery = true)
```
Mẹo: lấy `size + 1` dòng để biết còn trang tiếp không. Lưu ý: điều kiện `:ts IS NULL OR ...` có thể làm plan kém — tốt hơn tách 2 query cho trang đầu và trang tiếp.

**9.3** Streaming một cursor giữ **một transaction dài** (giữ snapshot → bloat/undo). Keyset theo PK với batch 10k: mỗi batch là một transaction ngắn, có thể resume khi lỗi, chạy trên replica → thường là lựa chọn tốt nhất.
</details>

---

<a id="phan-10"></a>
## 10. Partitioning, sharding, replication & connection limits

### 10.1 Partitioning (trong một DB)

Chia một bảng logic thành nhiều phần vật lý theo khóa:
- **Range**: theo thời gian (`orders_2024_01`...) — phổ biến nhất.
- **List**: theo giá trị rời rạc (vùng, tenant).
- **Hash**: phân đều theo hash của khóa.

```sql
-- PostgreSQL declarative partitioning (10+)
CREATE TABLE events (
  id BIGINT GENERATED ALWAYS AS IDENTITY, created_at TIMESTAMPTZ NOT NULL, payload JSONB,
  PRIMARY KEY (id, created_at)                      -- PK/UNIQUE phải chứa partition key
) PARTITION BY RANGE (created_at);
CREATE TABLE events_2024_01 PARTITION OF events FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');
-- Xóa dữ liệu cũ: DETACH/DROP partition — tức thì, không tạo dead tuple như DELETE hàng triệu dòng
ALTER TABLE events DETACH PARTITION events_2024_01;  -- PostgreSQL 14+: ... CONCURRENTLY

-- Oracle: interval partitioning tự tạo partition mới
CREATE TABLE events (id NUMBER, created_at DATE, payload CLOB)
PARTITION BY RANGE (created_at) INTERVAL (NUMTOYMINTERVAL(1, 'MONTH'))
(PARTITION p0 VALUES LESS THAN (DATE '2024-01-01'));
```

Lợi ích: **partition pruning** (query có điều kiện partition key chỉ quét partition liên quan), quản lý vòng đời dữ liệu (drop partition cũ), bảo trì từng phần (vacuum, rebuild index). Hạn chế: query **không** có partition key phải quét mọi partition; MySQL bắt buộc partition key nằm trong mọi unique key và **không hỗ trợ FK** trên bảng partitioned; quá nhiều partition (hàng nghìn) làm planning chậm.

### 10.2 Sharding (nhiều DB)

Chia dữ liệu ra **nhiều server** khi một node không chịu nổi ghi/dung lượng. Chiến lược:
- **Range-based** (theo id/thời gian): dễ hiểu, nhưng dễ **hotspot** (mọi ghi mới vào shard cuối).
- **Hash-based** (`hash(user_id) % N`): phân đều, nhưng đổi N thì phải di chuyển gần hết dữ liệu → dùng **consistent hashing** hoặc **nhiều shard logic cố định** (ví dụ 1024 virtual shard) ánh xạ lên ít node vật lý.
- **Directory-based** (bảng tra cứu tenant → shard): linh hoạt, thêm một thành phần phải HA.

Chọn **shard key** là quyết định khó đảo ngược: nên là khóa có trong hầu hết query (tenant_id, user_id), phân bố đều, và giữ dữ liệu hay truy cập cùng nhau cùng một shard.

Cái giá phải trả: JOIN chéo shard, transaction chéo shard (2PC hoặc Saga), unique constraint toàn cục, ID toàn cục (Snowflake), báo cáo tổng hợp (cần data warehouse), rebalancing. Công cụ: Vitess (MySQL), Citus (PostgreSQL), ShardingSphere (tầng Java/proxy).

> 💡 **Góc nhìn Senior:** Sharding là **phương án cuối**. Trước đó: tối ưu query/index, scale-up phần cứng (một server hiện đại chịu được rất nhiều), read replica, cache, tách bảng lạnh/nóng, partition, tách service theo domain (mỗi service DB riêng). Phỏng vấn viên đánh giá cao ứng viên biết **trì hoãn** sharding có lý do.

### 10.3 Replication

| Mô hình | Mô tả | Ví dụ |
|---|---|---|
| **Single-leader** (master–replica) | Ghi vào primary, replica nhận log và áp lại; đọc có thể từ replica | MySQL binlog replication, PostgreSQL streaming (WAL), Oracle Data Guard |
| **Multi-leader** | Nhiều node nhận ghi, cần giải quyết xung đột | MySQL Group Replication multi-primary, multi-DC |
| **Leaderless** | Ghi/đọc quorum (W + R > N) | Cassandra, DynamoDB |

**Đồng bộ vs bất đồng bộ:**
- *Async* (mặc định MySQL, PostgreSQL): primary commit không chờ replica → nhanh, nhưng failover có thể **mất giao dịch** đã commit.
- *Semi-sync* (MySQL): chờ ít nhất một replica **nhận** (chưa chắc đã áp dụng) event.
- *Sync* (PostgreSQL `synchronous_standby_names` + `synchronous_commit = on/remote_apply`): an toàn hơn, latency cao hơn, replica chết có thể chặn ghi.

**Replication lag** và các hệ quả (Kleppmann Ch.5):
- **Read-your-writes**: user cập nhật hồ sơ rồi tải lại trang (đọc từ replica chậm) → thấy dữ liệu cũ, tưởng lưu thất bại. Giải pháp: đọc dữ liệu *của chính user* từ primary; đọc từ primary trong N giây sau khi ghi; ghi nhớ vị trí log (LSN/GTID) của lần ghi và chỉ đọc replica đã bắt kịp vị trí đó (MySQL `WAIT_FOR_EXECUTED_GTID_SET`, PostgreSQL so `pg_last_wal_replay_lsn()`).
- **Monotonic reads**: hai lần đọc liên tiếp rơi vào hai replica có lag khác nhau → dữ liệu "đi lùi thời gian". Giải pháp: gắn user vào một replica cố định (sticky).
- **Consistent prefix reads**: thấy câu trả lời trước câu hỏi (với dữ liệu phân tán).

Theo dõi lag: MySQL `SHOW REPLICA STATUS` (`Seconds_Behind_Source`, 8.0.22+; trước đó `SHOW SLAVE STATUS`), PostgreSQL `pg_stat_replication` (`replay_lag`).

**Định tuyến đọc/ghi trong Spring:**

```java
public class RoutingDataSource extends AbstractRoutingDataSource {
  @Override protected Object determineCurrentLookupKey() {
    return TransactionSynchronizationManager.isCurrentTransactionReadOnly() ? "replica" : "primary";
  }
}
// Bọc bằng LazyConnectionDataSourceProxy để connection chỉ được lấy SAU KHI
// transaction đã được thiết lập (readOnly flag đã có) — nếu không, routing luôn trả "primary".
@Bean DataSource dataSource(RoutingDataSource routing) { return new LazyConnectionDataSourceProxy(routing); }

@Transactional(readOnly = true) public List<Order> list() { ... }  // → replica
```

### 10.4 Connection limits

- **PostgreSQL**: mỗi connection là một **process** (vài MB RAM, context switch); `max_connections` mặc định 100. Hàng nghìn connection trực tiếp → hiệu năng sụp. Giải pháp: **PgBouncer** (transaction pooling) — lưu ý ở transaction mode không dùng được session state (`SET`, advisory lock theo session, `LISTEN`), prepared statement chỉ được hỗ trợ ở mức protocol từ PgBouncer 1.21.
- **MySQL**: thread-per-connection, `max_connections` mặc định 151; tốt hơn PostgreSQL về số connection nhưng vẫn có giới hạn.
- **Oracle**: dedicated server (process/thread per session) hoặc shared server; DRCP cho pool phía DB.
- **Phép tính quan trọng**: tổng connection = `số instance ứng dụng × pool size`. 20 pod × HikariCP `maximumPoolSize=50` = 1.000 connection → vượt giới hạn khi autoscale.
- Kích thước pool **nhỏ hơn bạn nghĩ**: HikariCP wiki gợi ý điểm khởi đầu `connections ≈ (core_count × 2) + effective_spindle_count`. Pool to hơn chỉ làm DB tranh chấp CPU/lock nhiều hơn.

> ⚠️ **Lỗi thường gặp:** `maximumPoolSize` lớn để "chữa" lỗi `Connection is not available, request timed out` — trong khi nguyên nhân thật là transaction dài, query chậm, hoặc gọi HTTP bên trong `@Transactional` giữ connection. Bật `leakDetectionThreshold` và xem metric `hikaricp_connections_pending`.

### 🛠 Bài tập phần 10

**Bài 10.1 — Partition theo tháng (Cơ bản)**
- Đề bài: tạo bảng `events` partition theo tháng trên PostgreSQL với 12 partition, nạp 10 triệu dòng.
- Tiêu chí: `EXPLAIN` chứng minh partition pruning với điều kiện thời gian; so sánh thời gian xóa dữ liệu 1 tháng bằng `DELETE` vs `DROP/DETACH PARTITION`.

**Bài 10.2 — Read-your-writes (Trung bình)**
- Đề bài: dựng PostgreSQL primary + 1 replica (Docker, streaming replication). Spring Boot dùng `RoutingDataSource`. Giả lập lag bằng `recovery_min_apply_delay = '5s'` trên replica.
- Tiêu chí: chứng minh được lỗi "cập nhật xong đọc không thấy"; hiện thực một giải pháp read-your-writes (ví dụ: lưu thời điểm ghi cuối của user trong session/Redis, trong 10s sau đó route về primary); test tự động.

**Bài 10.3 — Thiết kế sharding (Nâng cao)**
- Đề bài: hệ thống ví điện tử 50 triệu user, 20.000 TPS ghi giao dịch. Thiết kế sharding: shard key, số shard logic, ánh xạ vật lý, cách sinh ID toàn cục, xử lý chuyển tiền giữa 2 user ở 2 shard, báo cáo đối soát toàn hệ thống, cách thêm node.
- Tiêu chí: tài liệu thiết kế 1–2 trang, có sơ đồ, nêu rõ trade-off và phương án rebalancing không downtime.

<details>
<summary>Gợi ý lời giải</summary>

**10.1** `EXPLAIN SELECT count(*) FROM events WHERE created_at >= '2024-03-01' AND created_at < '2024-04-01';` chỉ thấy `events_2024_03`. `DELETE` 800k dòng mất vài giây + để lại dead tuple; `DROP` mất vài ms.

**10.2** Lưu `lastWriteAt` theo userId (TTL 10s) trong Redis; `determineCurrentLookupKey()` kiểm tra thêm `ReadYourWritesContext.mustUsePrimary()`.

**10.3** Shard key = `user_id` (mọi truy vấn ví đều theo user). 4096 shard logic → `shard = hash(user_id) % 4096`, bảng mapping shard logic → cụm vật lý (ban đầu 8 cụm). ID: Snowflake (timestamp + shardId + sequence). Chuyển tiền chéo shard: Saga — trừ ở shard A (ghi giao dịch PENDING + outbox) → cộng ở shard B (idempotent theo transfer_id) → hoàn tất; bù trừ khi lỗi. Đối soát: CDC (Debezium) đổ về data warehouse. Thêm node: di chuyển nguyên shard logic (copy + catch-up qua binlog + cutover ngắn).
</details>

---

<a id="phan-11"></a>
## 11. Stored procedure, PL/SQL & đặc thù Oracle

### 11.1 PL/SQL cơ bản

Nhiều hệ thống ngân hàng, viễn thông, bảo hiểm, chính phủ điện tử ở Việt Nam chạy Oracle với rất nhiều logic trong PL/SQL. Senior Java cần **đọc hiểu và gọi được**, dù không viết mới.

```sql
CREATE OR REPLACE PROCEDURE transfer_money (
  p_from IN NUMBER, p_to IN NUMBER, p_amount IN NUMBER, p_result OUT VARCHAR2
) AS
  v_balance account.balance%TYPE;
  e_insufficient EXCEPTION;
BEGIN
  SELECT balance INTO v_balance FROM account WHERE id = p_from FOR UPDATE;
  IF v_balance < p_amount THEN RAISE e_insufficient; END IF;

  UPDATE account SET balance = balance - p_amount WHERE id = p_from;
  UPDATE account SET balance = balance + p_amount WHERE id = p_to;
  p_result := 'OK';
EXCEPTION
  WHEN e_insufficient THEN p_result := 'INSUFFICIENT_FUNDS';
  WHEN NO_DATA_FOUND  THEN p_result := 'ACCOUNT_NOT_FOUND';
  -- Không COMMIT ở đây: để caller (Java) quản lý transaction
END;
/
```

Gọi từ Java:

```java
try (CallableStatement cs = conn.prepareCall("{call transfer_money(?, ?, ?, ?)}")) {
  cs.setLong(1, 1L); cs.setLong(2, 2L); cs.setBigDecimal(3, new BigDecimal("10"));
  cs.registerOutParameter(4, Types.VARCHAR);
  cs.execute();
  String result = cs.getString(4);
}
// Spring: SimpleJdbcCall, hoặc JPA @Procedure / StoredProcedureQuery
```

Khái niệm PL/SQL hay gặp: `%TYPE`/`%ROWTYPE`, cursor (`FOR rec IN (SELECT ...) LOOP`), `BULK COLLECT` + `FORALL` (giảm context switch SQL ↔ PL/SQL), package (spec + body, có state theo session), `PRAGMA AUTONOMOUS_TRANSACTION` (ghi log độc lập với transaction chính), `REF CURSOR` trả result set về Java.

**Khi nào dùng stored procedure:**
- ✅ Xử lý dữ liệu khối lớn ngay tại DB (batch đối soát cuối ngày) — tránh kéo hàng triệu dòng qua mạng.
- ✅ Hệ thống legacy, nhiều client (Java, .NET, báo cáo) cùng dùng một logic.
- ✅ Bảo mật: chỉ cấp quyền EXECUTE, không cấp quyền bảng.

**Khi nào tránh:**
- ❌ Logic nghiệp vụ phức tạp, thay đổi thường xuyên: khó version control, unit test, code review, debug, CI/CD.
- ❌ Scale: DB là tài nguyên đắt và khó scale ngang nhất; đẩy CPU vào DB là đi ngược.
- ❌ Vendor lock-in: chuyển DB gần như viết lại.
- ❌ Logic bị chia đôi giữa Java và DB → không ai nắm toàn bộ.

### 11.2 ROWNUM vs FETCH FIRST

```sql
-- SAI: ROWNUM được gán TRƯỚC khi ORDER BY → lấy 10 dòng bất kỳ rồi mới sắp xếp
SELECT * FROM orders WHERE ROWNUM <= 10 ORDER BY amount DESC;

-- ĐÚNG (trước 12c): sắp xếp trong subquery
SELECT * FROM (SELECT * FROM orders ORDER BY amount DESC) WHERE ROWNUM <= 10;

-- Phân trang trước 12c (3 tầng, kinh điển):
SELECT * FROM (
  SELECT t.*, ROWNUM rn FROM (SELECT * FROM orders ORDER BY created_at DESC, id DESC) t
  WHERE ROWNUM <= :end_row
) WHERE rn > :start_row;

-- 12c+ (chuẩn SQL:2008):
SELECT * FROM orders ORDER BY amount DESC FETCH FIRST 10 ROWS ONLY;
SELECT * FROM orders ORDER BY created_at DESC OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY;
SELECT * FROM orders ORDER BY amount DESC FETCH FIRST 10 ROWS WITH TIES;   -- lấy cả dòng đồng hạng

-- Bẫy: WHERE ROWNUM = 2 hoặc ROWNUM > 1 luôn trả về rỗng (dòng đầu không được gán 1 nên không có dòng 2).
```

### 11.3 Sequence & identity

```sql
CREATE SEQUENCE seq_orders START WITH 1 INCREMENT BY 1 CACHE 1000;
INSERT INTO orders (id, ...) VALUES (seq_orders.NEXTVAL, ...);
-- 12c+: identity column (bên dưới vẫn là sequence)
CREATE TABLE t (id NUMBER GENERATED BY DEFAULT ON NULL AS IDENTITY PRIMARY KEY, ...);
```

- Sequence **không đảm bảo liên tục** (gap khi rollback, khi restart instance mất cache, RAC mỗi node cache riêng → không tăng theo thời gian). Không dùng sequence cho số hóa đơn đòi hỏi liên tục theo luật — cần bảng counter khóa dòng.
- `CACHE` nhỏ (hoặc `NOCACHE`) → contention trên dictionary khi insert nhiều. Với Hibernate: `@SequenceGenerator(allocationSize = 50)` phải **khớp** `INCREMENT BY 50` của sequence (pooled optimizer), nếu không sẽ ra ID trùng hoặc âm.

### 11.4 Bind variables, hard parse & soft parse

Khi nhận câu SQL, Oracle băm text để tìm trong **shared pool (library cache)**:
- **Hard parse**: chưa có → kiểm tra cú pháp, ngữ nghĩa, quyền, **tối ưu hóa** (sinh plan) — tốn CPU và cần latch/mutex trên shared pool (tài nguyên dùng chung → **không scale**).
- **Soft parse**: đã có cursor giống hệt → dùng lại plan.

```java
// TỆ: mỗi id là một câu SQL khác nhau → hard parse liên tục, shared pool đầy cursor rác,
// và SQL injection!
stmt.executeQuery("SELECT * FROM orders WHERE customer_id = " + id);

// ĐÚNG: một câu SQL dùng chung, chỉ đổi giá trị bind
PreparedStatement ps = conn.prepareStatement("SELECT * FROM orders WHERE customer_id = ?");
ps.setLong(1, id);
```

Liên quan: `CURSOR_SHARING=FORCE` (Oracle tự thay literal bằng bind — giải pháp tình thế cho app cũ), **bind peeking** + **adaptive cursor sharing** (11g+) cho dữ liệu lệch, statement cache phía driver (`oracle.jdbc.implicitStatementCacheSize`) để tránh cả soft parse. Theo dõi: `V$SQL` có nhiều dòng giống nhau chỉ khác literal; AWR "parse count (hard)".

> 💡 **Góc nhìn Senior:** IN-list động (`IN (?, ?, ?)` với số phần tử khác nhau) cũng sinh ra nhiều câu SQL khác nhau. Hibernate có `hibernate.query.in_clause_parameter_padding=true` để làm tròn số tham số lên lũy thừa của 2 → giảm số cursor. Oracle giới hạn 1.000 phần tử trong IN list (`ORA-01795`).

### 11.5 Hàm Oracle hay gặp & khác biệt

| Oracle | Chuẩn / PostgreSQL / MySQL | Ghi chú |
|---|---|---|
| `NVL(a, b)` | `COALESCE(a, b)` (chuẩn), `IFNULL` (MySQL) | `NVL` luôn tính cả `b`; `COALESCE` short-circuit và nhận nhiều tham số |
| `NVL2(a, b, c)` | `CASE WHEN a IS NOT NULL THEN b ELSE c END` | |
| `DECODE(x, 1,'A', 2,'B', 'Khác')` | `CASE x WHEN 1 THEN 'A' ... END` | `DECODE` coi `NULL = NULL` là khớp, `CASE` thì không |
| `SYSDATE`, `SYSTIMESTAMP` | `now()`, `CURRENT_TIMESTAMP` | `SYSDATE` theo giờ **server OS**, không theo session time zone |
| `TO_CHAR`, `TO_DATE`, `TO_NUMBER` | `to_char`, `CAST`, `STR_TO_DATE` | Phụ thuộc NLS → luôn truyền format rõ ràng |
| `DUAL` | không cần (`SELECT 1`) | Oracle 23ai đã cho phép bỏ `FROM DUAL` |
| `||` nối chuỗi | `||` (PG), `CONCAT()` (MySQL) | |
| `MINUS` | `EXCEPT` | |
| Kiểu `DATE` có cả giờ phút giây | `DATE` chỉ có ngày (PG/MySQL) | Nguồn bug khi `WHERE d = DATE '2024-01-01'` |

> ⚠️ **Bẫy lớn nhất của Oracle: chuỗi rỗng `''` là `NULL`.** `WHERE name = ''` không bao giờ đúng; `INSERT ''` vào cột `NOT NULL` → `ORA-01400`; `LENGTH('')` là `NULL`. Code Java chạy đúng trên PostgreSQL có thể vỡ khi chuyển sang Oracle và ngược lại.

### 🛠 Bài tập phần 11

**Bài 11.1 — ROWNUM và phân trang Oracle (Cơ bản)**
- Đề bài: trên Oracle Free (Docker), viết 3 phiên bản lấy top 5 đơn hàng giá trị cao nhất: ROWNUM sai, ROWNUM đúng, `FETCH FIRST`. Thêm phân trang trang 3 (size 10) theo cả 2 cú pháp.
- Tiêu chí: kết quả đúng, giải thích vì sao bản sai sai.

**Bài 11.2 — Gọi PL/SQL từ Spring (Trung bình)**
- Đề bài: viết procedure `transfer_money` (như trên) và một function trả về `SYS_REFCURSOR` danh sách giao dịch theo tài khoản. Gọi bằng `SimpleJdbcCall` trong Spring Boot.
- Tiêu chí: transaction do Spring quản lý (procedure không commit), rollback khi Java ném exception sau khi gọi procedure — viết test chứng minh.

**Bài 11.3 — Hard parse storm (Nâng cao)**
- Đề bài: viết chương trình Java chạy 100.000 query bằng nối chuỗi và 100.000 query bằng `PreparedStatement`. Đo thời gian, và trên Oracle so sánh `V$SYSSTAT` (`parse count (hard)`, `parse count (total)`) trước/sau, số cursor trong `V$SQL`.
- Tiêu chí: bảng số liệu; giải thích vì sao vấn đề trầm trọng hơn khi tăng số thread (contention trên shared pool), và vì sao `CURSOR_SHARING=FORCE` chỉ là giải pháp tạm.

<details>
<summary>Gợi ý lời giải</summary>

**11.2**
```java
SimpleJdbcCall call = new SimpleJdbcCall(jdbcTemplate).withProcedureName("TRANSFER_MONEY");
Map<String, Object> out = call.execute(new MapSqlParameterSource()
    .addValue("P_FROM", 1).addValue("P_TO", 2).addValue("P_AMOUNT", 10));
String result = (String) out.get("P_RESULT");
```
Đặt trong method `@Transactional`; `JdbcTemplate` dùng chung connection của transaction → exception sau đó rollback cả thay đổi của procedure.

**11.3**
```sql
SELECT name, value FROM v$sysstat WHERE name IN ('parse count (hard)', 'parse count (total)');
SELECT COUNT(*) FROM v$sql WHERE sql_text LIKE 'SELECT * FROM orders WHERE customer_id =%';
```
Literal: ~100.000 hard parse và hàng chục nghìn cursor; bind: 1 hard parse. Với nhiều thread, hard parse cần latch/mutex trên library cache → CPU tăng vọt, "library cache: mutex X" xuất hiện trong wait events.
</details>

---

<a id="phan-12"></a>
## 12. NoSQL & chọn SQL hay NoSQL

### 12.1 Các họ NoSQL

| Họ | Đại diện | Mô hình | Thế mạnh | Điểm yếu |
|---|---|---|---|---|
| **Document** | MongoDB, Couchbase | JSON/BSON document, schema linh hoạt | Dữ liệu dạng cây tự nhiên (hồ sơ, catalog sản phẩm nhiều thuộc tính), đọc cả aggregate một lần | JOIN hạn chế (`$lookup`), dữ liệu nhiều-nhiều khó, dễ trùng lặp |
| **Wide-column** | Cassandra, ScyllaDB, HBase | Partition key + clustering columns, LSM-tree | **Ghi cực lớn**, scale ngang tuyến tính, multi-DC, time-series, IoT, message history | Phải thiết kế bảng theo **query** (query-first), không JOIN, không ad-hoc query, tunable consistency phức tạp |
| **Key-value** | Redis, DynamoDB, etcd | Key → value | Latency thấp, cache, session, counter | Truy vấn theo giá trị khó |
| **Search engine** | Elasticsearch, OpenSearch | Inverted index | Full-text, fuzzy, faceted search, log analytics | **Không phải nguồn sự thật**: near-real-time (refresh ~1s), không transaction |
| **Graph** | Neo4j | Node + edge | Quan hệ nhiều tầng (gợi ý bạn bè, phát hiện gian lận) | Scale ngang khó |

Kiến thức nền cần nhớ:
- **CAP**: khi có network partition, phải chọn Consistency hoặc Availability. Kleppmann khuyên dùng khái niệm cụ thể hơn (linearizability, causal consistency) thay vì gán nhãn "CP/AP" cho cả hệ thống. **PACELC** bổ sung: khi không có partition, đánh đổi Latency vs Consistency.
- **LSM-tree vs B-Tree** (Kleppmann Ch.3): LSM (Cassandra, RocksDB) ghi tuần tự vào memtable + SSTable → ghi nhanh, đọc phải kiểm tra nhiều tầng (bloom filter giúp), cần compaction. B-Tree (đa số RDBMS) đọc ổn định, ghi tại chỗ.
- MongoDB hiện có multi-document ACID transaction (từ 4.0, sharded từ 4.2), nên "NoSQL không có transaction" là kiến thức cũ — nhưng dùng nhiều thì chi phí cao.

### 12.2 Chọn thế nào

Mặc định cho hệ thống nghiệp vụ: **bắt đầu với RDBMS** (PostgreSQL/MySQL/Oracle) — dữ liệu có quan hệ, cần transaction, cần ad-hoc query/báo cáo, team quen thuộc. Thêm NoSQL **cho nhu cầu cụ thể** (polyglot persistence):

| Nhu cầu | Lựa chọn hợp lý |
|---|---|
| Đơn hàng, thanh toán, tồn kho, kế toán | RDBMS |
| Tìm kiếm sản phẩm có lọc theo facet, gõ sai chính tả, tiếng Việt có/không dấu | Elasticsearch (đồng bộ từ RDBMS qua CDC/outbox) |
| Lịch sử chat, event IoT hàng trăm nghìn ghi/giây, truy vấn theo (device_id, thời gian) | Cassandra / ScyllaDB |
| Catalog sản phẩm thuộc tính động, CMS | MongoDB, hoặc PostgreSQL `jsonb` + GIN |
| Cache, session, rate limit, leaderboard | Redis (Module 12) |
| Log tập trung, observability | Elasticsearch/OpenSearch, Loki, ClickHouse |
| Phân tích OLAP | ClickHouse, BigQuery, data warehouse |

```js
// MongoDB: một document chứa aggregate đơn hàng (embedding)
db.orders.insertOne({ _id: 106, customerId: 1, createdAt: ISODate("2024-02-15"),
  items: [{ sku: "SKU-1", name: "Bút", qty: 2, price: 50 }], status: "PAID" });
db.orders.createIndex({ customerId: 1, createdAt: -1 });   // composite index — leftmost prefix vẫn áp dụng!
```

```sql
-- Cassandra (CQL): bảng thiết kế cho đúng một query "tin nhắn mới nhất của một cuộc hội thoại"
CREATE TABLE messages_by_conversation (
  conversation_id uuid, bucket text,           -- bucket theo tháng để partition không phình vô hạn
  message_id timeuuid, sender_id uuid, body text,
  PRIMARY KEY ((conversation_id, bucket), message_id)
) WITH CLUSTERING ORDER BY (message_id DESC);
```

> 💡 **Góc nhìn Senior:** Mỗi kho dữ liệu thêm vào là thêm một hệ thống phải vận hành, backup, giám sát, đồng bộ. Câu trả lời tốt trong phỏng vấn nêu rõ: **nguồn sự thật** ở đâu, đồng bộ sang các kho phụ bằng gì (Transactional Outbox + CDC/Debezium thay vì dual-write), và xử lý thế nào khi chúng lệch nhau (reindex từ nguồn sự thật).

> ⚠️ **Lỗi thường gặp:** dual-write (code ghi DB rồi ghi Elasticsearch) → một bên thành công một bên lỗi → lệch dữ liệu vĩnh viễn. Chọn MongoDB "vì schema linh hoạt" rồi tự xây lại JOIN, transaction, ràng buộc trong code.

### 🛠 Bài tập phần 12

**Bài 12.1 — Mô hình hóa đa kho (Cơ bản)**
- Đề bài: cho tính năng "bài viết + bình luận + lượt thích", mô hình hóa trên PostgreSQL và MongoDB (embedding vs referencing).
- Tiêu chí: nêu query nào nhanh/chậm ở mỗi mô hình; giới hạn 16MB document ảnh hưởng thế nào nếu embed bình luận.

**Bài 12.2 — Bảng Cassandra theo query (Trung bình)**
- Đề bài: ứng dụng IoT: 100k thiết bị gửi số đo mỗi 10s. Query: (1) số đo mới nhất của thiết bị, (2) số đo của thiết bị trong khoảng thời gian, (3) mọi thiết bị vượt ngưỡng trong 5 phút qua.
- Tiêu chí: thiết kế bảng CQL cho từng query, tính kích thước partition, giải thích vì sao query (3) cần bảng riêng hoặc hệ thống khác.

**Bài 12.3 — Đồng bộ PostgreSQL → Elasticsearch (Nâng cao)**
- Đề bài: thiết kế (và nếu có thời gian, dựng bằng Docker: PostgreSQL + Debezium + Kafka + Elasticsearch) pipeline đồng bộ bảng `products` sang ES.
- Tiêu chí: xử lý thứ tự sự kiện (dùng version/LSN để bỏ event cũ), xóa mềm/xóa cứng, reindex toàn bộ không downtime bằng alias; nêu SLA độ trễ.

<details>
<summary>Gợi ý lời giải</summary>

**12.1** Embed bình luận: đọc bài + bình luận một lần rất nhanh, nhưng bài viral có hàng trăm nghìn bình luận vượt 16MB và mọi update bình luận phải ghi lại document lớn → dùng referencing (collection `comments` với index `{postId:1, createdAt:-1}`) + embed vài bình luận mới nhất.

**12.2** (1)+(2): `PRIMARY KEY ((device_id, day), ts) WITH CLUSTERING ORDER BY (ts DESC)` — mỗi partition ~8.640 dòng/ngày. (3) cần partition theo thời gian (`(minute_bucket), device_id`) hoặc xử lý streaming (Kafka Streams/Flink) — Cassandra không quét toàn cụm hiệu quả.

**12.3** ES index với `external` versioning (`version_type=external`, version = LSN hoặc `updated_at` epoch) để event cũ đến sau bị bỏ qua. Reindex: tạo `products_v2`, nạp từ snapshot, phát lại event từ offset Kafka lúc bắt đầu snapshot, chuyển alias `products` sang v2 nguyên tử.
</details>

---

<a id="phan-13"></a>
## 13. Migration dữ liệu không downtime (expand/contract)

### 13.1 Vấn đề
Deploy rolling (Kubernetes) nghĩa là **phiên bản cũ và mới của ứng dụng chạy song song** với **cùng một schema**. Mọi thay đổi schema phải tương thích với cả hai. Ngoài ra DDL trên bảng lớn có thể khóa bảng hàng giờ.

### 13.2 Mẫu expand/contract (parallel change)

Ví dụ: đổi tên cột `users.fullname` → `users.display_name`.

| Bước | Thay đổi | Ứng dụng |
|---|---|---|
| 1. **Expand** | `ALTER TABLE users ADD display_name VARCHAR(200) NULL;` (thêm cột nullable là thao tác rẻ) | v1 vẫn chạy bình thường |
| 2. **Dual write** | Deploy v2: ghi **cả hai** cột, đọc cột cũ | v1 + v2 cùng tồn tại an toàn |
| 3. **Backfill** | Copy dữ liệu cũ theo batch nhỏ (keyset theo PK, sleep giữa batch, theo dõi replication lag) | |
| 4. **Switch read** | Deploy v3: đọc cột mới, vẫn ghi cả hai (để rollback về v2 được) | |
| 5. **Contract** | Deploy v4: chỉ ghi cột mới. Sau một thời gian an toàn: `ALTER TABLE users DROP COLUMN fullname;` | |

```sql
-- Backfill an toàn (PostgreSQL), chạy lặp lại tới khi 0 dòng
UPDATE users SET display_name = fullname
WHERE id IN (SELECT id FROM users WHERE display_name IS NULL AND id > :last_id ORDER BY id LIMIT 5000);
```

### 13.3 Thao tác DDL nguy hiểm & cách an toàn

| Thao tác | Rủi ro | Cách an toàn |
|---|---|---|
| Thêm cột `NOT NULL DEFAULT x` | Bản cũ rewrite cả bảng. PostgreSQL 11+, MySQL 8.0.12+ (`ALGORITHM=INSTANT`), Oracle 11g+ làm tức thì (metadata only) | Kiểm tra phiên bản; nếu cũ: thêm nullable → backfill → thêm constraint |
| Thêm `NOT NULL` vào cột có sẵn | Quét toàn bảng dưới lock | PostgreSQL: `ADD CONSTRAINT ... CHECK (col IS NOT NULL) NOT VALID` → `VALIDATE CONSTRAINT` (lock nhẹ) |
| Thêm FK | Quét bảng con dưới lock | PostgreSQL `NOT VALID` rồi `VALIDATE`; Oracle `ENABLE NOVALIDATE` |
| Tạo index | Chặn ghi | `CONCURRENTLY` (PG), `ONLINE` (Oracle), online DDL (MySQL) |
| Đổi kiểu cột | Rewrite bảng | Expand/contract với cột mới |
| Đổi tên cột/bảng | Phá vỡ phiên bản app cũ ngay lập tức | Expand/contract, hoặc view tạm |
| `ALTER` trên MySQL bảng rất lớn | Lock MDL, replication lag lớn | `gh-ost`, `pt-online-schema-change` |
| Mọi DDL | Chờ lock sau transaction dài → chặn dây chuyền | `SET lock_timeout = '3s'` + retry |

**Công cụ migration**: Flyway / Liquibase chạy trong pipeline. Quy tắc: migration **chỉ tiến** (forward-only), mỗi migration tương thích ngược với phiên bản app đang chạy, tách migration "expand" và "contract" vào các release khác nhau. Không chạy migration nặng lúc app khởi động trên nhiều pod cùng lúc (Flyway dùng lock bảng lịch sử nhưng migration dài làm readiness probe fail) — tách thành Job riêng.

> 💡 **Góc nhìn Senior:** Hỏi bản thân trước mỗi migration: (1) Lệnh này lấy lock gì và trong bao lâu? (2) Phiên bản app trước đó có chạy được với schema mới không? (3) Rollback thế nào — rollback **code** chứ không rollback schema? (4) Ảnh hưởng tới replication lag và CDC consumer (Debezium cũng phải hiểu schema mới)?

> ⚠️ **Lỗi thường gặp:** JPA `ddl-auto=update` trên production; xóa cột trong cùng release ngừng dùng cột đó (pod cũ còn chạy → lỗi SQL hàng loạt trong lúc rolling update); Hibernate entity map cột mới là `NOT NULL` trong khi dữ liệu cũ chưa backfill.

### 🛠 Bài tập phần 13

**Bài 13.1 — Kế hoạch migration (Cơ bản)**
- Đề bài: viết kế hoạch expand/contract (các bước, nội dung migration Flyway, thay đổi code mỗi release) để tách `customers.address` (một chuỗi) thành `province`, `district`, `street`.
- Tiêu chí: mỗi bước có điều kiện chuyển tiếp và phương án rollback.

**Bài 13.2 — Đo lock khi DDL (Trung bình)**
- Đề bài: trên PostgreSQL, mở transaction dài đọc bảng `orders_big`; ở session khác chạy `ALTER TABLE orders_big ADD COLUMN note text;`; ở session thứ ba chạy SELECT thường.
- Tiêu chí: chứng minh SELECT ở session 3 bị chặn sau ALTER (hàng đợi lock); lặp lại với `SET lock_timeout='2s'` và giải thích vì sao an toàn hơn.

**Bài 13.3 — Zero-downtime đổi kiểu khóa chính (Nâng cao)**
- Đề bài: bảng `events` 500 triệu dòng có PK `INT` sắp tràn 2^31. Lập và thử nghiệm (trên dữ liệu thu nhỏ 5 triệu dòng) quy trình chuyển sang `BIGINT` không downtime, gồm cả FK từ bảng khác.
- Tiêu chí: script đầy đủ; app chạy liên tục (script Java ghi/đọc mỗi 10ms) không có lỗi trong suốt quá trình; đo thời gian lock lớn nhất.

<details>
<summary>Gợi ý lời giải</summary>

**13.2** `ALTER` cần `ACCESS EXCLUSIVE` → xếp hàng chờ transaction dài; SELECT mới cần `ACCESS SHARE`, xung đột với yêu cầu `ACCESS EXCLUSIVE` đang chờ → cũng xếp hàng. `lock_timeout` khiến ALTER bỏ cuộc sau 2s thay vì chặn mọi người vô thời hạn; retry vào lúc khác.

**13.3** Các bước: thêm cột `id_new BIGINT`; trigger đồng bộ `NEW.id_new := NEW.id` cho ghi mới; backfill theo batch; `CREATE UNIQUE INDEX CONCURRENTLY ... (id_new)`; thêm cột `BIGINT` tương ứng ở bảng con cũng làm tương tự; trong một transaction ngắn với `lock_timeout`: đổi sequence owner, `ALTER TABLE ... DROP CONSTRAINT events_pkey, ADD CONSTRAINT events_pkey PRIMARY KEY USING INDEX events_id_new_idx`, đổi tên cột; tạo lại FK bằng `NOT VALID` + `VALIDATE`. Lock lớn nhất chỉ là bước đổi tên/swap constraint (ms).
</details>

---

<a id="du-an-mini"></a>
## Dự án mini — "ShopDB": tầng dữ liệu cho sàn thương mại điện tử

**Bối cảnh:** xây dựng tầng dữ liệu cho một sàn TMĐT nhỏ, dùng Spring Boot 3 + PostgreSQL (bắt buộc), tùy chọn bản chạy MySQL 8 hoặc Oracle để so sánh. Thời lượng 2 ngày.

### Yêu cầu chức năng
1. **Schema** chuẩn 3NF cho: `customers`, `addresses`, `products`, `categories` (cây nhiều cấp), `inventory`, `orders`, `order_items`, `payments`. Có PK, FK (kèm index), CHECK, UNIQUE phù hợp; quản lý bằng Flyway.
2. **Sinh dữ liệu**: 100k khách, 50k sản phẩm, 5 triệu đơn hàng, phân bố lệch thực tế (20% khách tạo 80% đơn; 95% đơn `DELIVERED`).
3. **API đặt hàng** `POST /orders`: trừ tồn kho nhiều sản phẩm trong một transaction, không bán vượt, không deadlock khi hai đơn cùng chứa các sản phẩm theo thứ tự khác nhau; idempotent theo header `Idempotency-Key` (upsert vào bảng `idempotency_keys`).
4. **API lịch sử đơn** `GET /customers/{id}/orders` phân trang keyset, cursor opaque.
5. **Báo cáo** (SQL thuần): doanh thu theo ngày có ngày trống = 0, top 3 sản phẩm mỗi danh mục theo doanh thu tháng, tỷ lệ khách quay lại (đặt đơn thứ 2 trong 30 ngày), cây danh mục kèm tổng doanh thu cộng dồn từ danh mục con (recursive CTE).
6. **Job dọn đơn hết hạn**: các đơn `PENDING` quá 30 phút được hủy và hoàn tồn kho, chạy song song 4 worker bằng `FOR UPDATE SKIP LOCKED`.
7. **Migration không downtime**: thêm cột `orders.channel` (`WEB`/`APP`/`POS`) `NOT NULL` theo quy trình expand/contract, có backfill.

### Yêu cầu phi chức năng
- `POST /orders` p99 < 100ms ở 200 req/s (k6/JMeter), 0 lỗi oversell trong bài test 1.000 request đồng thời vào sản phẩm stock = 100.
- `GET /customers/{id}/orders` trang bất kỳ < 20ms; mọi query báo cáo < 2s trên 5 triệu đơn.
- Không có query N+1 (kiểm chứng bằng Hibernate statistics hoặc datasource-proxy trong test).
- Có retry cho lỗi serialization/deadlock (SQLSTATE `40001`, `40P01`) ở tầng service.
- `pg_stat_statements` bật sẵn; README có mục "Top 5 query & plan" kèm `EXPLAIN (ANALYZE, BUFFERS)`.
- Test tích hợp dùng Testcontainers.

### Tiêu chí chấm (100 điểm)
| Hạng mục | Điểm |
|---|---|
| Schema & chuẩn hóa, constraint, migration Flyway | 15 |
| Tính đúng đắn khi đồng thời (oversell, deadlock, idempotency) có test chứng minh | 25 |
| Thiết kế index & plan (có bằng chứng EXPLAIN, không index thừa) | 20 |
| SQL báo cáo (window function, CTE, recursive CTE) đúng & nhanh | 15 |
| Pagination keyset & job SKIP LOCKED | 10 |
| Migration expand/contract có kịch bản rolling deploy | 10 |
| README: quyết định thiết kế, trade-off, số liệu benchmark | 5 |

---

<a id="checklist-tu-danh-gia"></a>
## Checklist tự đánh giá

- [ ] Tôi đưa được một bảng về 3NF/BCNF, chỉ ra các anomaly, và giải thích khi nào denormalize có chủ đích.
- [ ] Tôi phân biệt điều kiện ở `ON` và `WHERE` trong LEFT JOIN, và biết bẫy `NOT IN` với NULL.
- [ ] Tôi viết được top-N per group, running total, gaps & islands bằng window function, và biết khác biệt `ROWS` vs `RANGE`.
- [ ] Tôi viết được recursive CTE (và `CONNECT BY` của Oracle) có chống vòng lặp.
- [ ] Tôi biết cú pháp UPSERT trên cả 3 DB và tính atomic của từng loại khi đồng thời.
- [ ] Tôi vẽ được B+Tree, giải thích clustered index InnoDB vs heap table Oracle/PostgreSQL, IOT, và vì sao PK ngẫu nhiên có hại.
- [ ] Tôi thiết kế composite index theo leftmost prefix (bằng trước, range sau), covering index, partial/function-based index.
- [ ] Tôi liệt kê ít nhất 6 lý do index không được dùng và cách viết lại sargable.
- [ ] Tôi đọc được `EXPLAIN ANALYZE` (PostgreSQL, MySQL) và `DBMS_XPLAN.DISPLAY_CURSOR` (Oracle), nhận ra ước lượng sai.
- [ ] Tôi giải thích được nested loop / hash / merge join và khi nào mỗi loại tốt.
- [ ] Tôi có quy trình tối ưu query dựa trên số liệu (pg_stat_statements, slow log, wait events).
- [ ] Tôi giải thích được 6 anomaly (dirty read → write skew) và tái hiện được lost update, write skew.
- [ ] Tôi nhớ isolation mặc định: MySQL REPEATABLE READ, PostgreSQL & Oracle READ COMMITTED, và khác biệt thực tế của RR/SERIALIZABLE giữa các DB.
- [ ] Tôi giải thích MVCC của InnoDB/Oracle (undo) vs PostgreSQL (tuple + VACUUM) và hậu quả của transaction dài.
- [ ] Tôi giải thích gap lock/next-key lock, đọc được log deadlock và có ít nhất 4 cách phòng tránh.
- [ ] Tôi chọn đúng giữa pessimistic, optimistic lock và atomic update; code có retry.
- [ ] Tôi hiện thực được keyset pagination với tie-breaker và cursor opaque.
- [ ] Tôi phân biệt partitioning vs sharding, chọn shard key, và xử lý replication lag (read-your-writes).
- [ ] Tôi tính được tổng số connection DB theo số pod × pool size và biết vì sao pool nhỏ thường tốt hơn.
- [ ] Tôi đọc/gọi được PL/SQL từ Java, biết bẫy ROWNUM, sequence cache, chuỗi rỗng = NULL, và tầm quan trọng của bind variable.
- [ ] Tôi lập luận được khi nào dùng MongoDB, Cassandra, Elasticsearch bên cạnh RDBMS và cách đồng bộ không dual-write.
- [ ] Tôi lên được kế hoạch migration expand/contract và biết DDL nào nguy hiểm trên bảng lớn.

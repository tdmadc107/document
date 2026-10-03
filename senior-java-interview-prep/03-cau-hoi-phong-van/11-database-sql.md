# Câu hỏi phỏng vấn — Module 11: Database & SQL cho Senior

> 📚 Giáo trình tương ứng: [Module 11 — Database & SQL cho Senior](../01-giao-trinh/11-database-sql.md)

> **Cách dùng:** đọc câu hỏi, **tự trả lời thành tiếng** trước khi mở đáp án. Với câu đọc SQL/plan, hãy viết ra giấy kết quả bạn dự đoán (số dòng, index có được dùng không) rồi mới đối chiếu. Nếu có Docker, chạy lại ví dụ trên PostgreSQL/MySQL/Oracle như phần "Chuẩn bị môi trường" của giáo trình — người phỏng vấn Senior rất hay hỏi "trên MySQL thì khác gì?".
>
> **Ký hiệu cấp độ:** 🟢 Cơ bản · 🟡 Senior · 🔴 Xoáy sâu.

## Mục lục

| Nhóm | Chủ đề | Câu hỏi |
|---|---|---|
| [A](#nhom-a) | Mô hình quan hệ & chuẩn hóa | Q1–Q5 |
| [B](#nhom-b) | SQL nâng cao | Q6–Q15 |
| [C](#nhom-c) | Index chuyên sâu | Q16–Q25 |
| [D](#nhom-d) | Execution plan, JOIN, statistics, tối ưu | Q26–Q31 |
| [E](#nhom-e) | Transaction, isolation, MVCC | Q32–Q39 |
| [F](#nhom-f) | Locking & deadlock | Q40–Q45 |
| [G](#nhom-g) | Pagination | Q46–Q47 |
| [H](#nhom-h) | Partitioning, sharding, replication, connection | Q48–Q52 |
| [I](#nhom-i) | Oracle, PL/SQL, bind variable | Q53–Q57 |
| [J](#nhom-j) | NoSQL & migration không downtime | Q58–Q60 |

---

<a id="nhom-a"></a>
## A. Mô hình quan hệ & chuẩn hóa

### Q1. 🟢 Natural key và surrogate key khác nhau thế nào? Dùng UUID làm primary key có vấn đề gì?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Natural key (email, CCCD, mã số thuế) có ý nghĩa nghiệp vụ nhưng **có thể thay đổi** và thường dài. Surrogate key (`BIGINT` sequence/identity, UUID) ổn định, ngắn. Thực tế: surrogate làm PK + `UNIQUE` constraint trên natural key. UUID v4 ngẫu nhiên làm PK gây **insert ngẫu nhiên** vào B-Tree → page split, page nửa trống, buffer pool kém hiệu quả — đặc biệt tệ với InnoDB vì PK là clustered index và mọi secondary index chứa PK (16 byte). Nếu cần ID sinh phân tán, dùng **UUID v7**/ULID/Snowflake (có tiền tố thời gian → gần tuần tự), lưu dạng `uuid`/`BINARY(16)` chứ không phải `VARCHAR(36)`.

**Giải thích chi tiết:**
- PK natural key thay đổi → phải cập nhật mọi FK tham chiếu.
- Benchmark kinh điển (MySQL, 5 triệu dòng): UUID v4 insert chậm hơn nhiều lần khi dữ liệu vượt buffer pool, kích thước lớn hơn ~1.5–2x; UUID v7 gần với AUTO_INCREMENT.

**Câu hỏi nối tiếp:**
- *Khi nào vẫn dùng UUID?* → ID cần sinh ở client/offline, merge dữ liệu nhiều nguồn, không muốn lộ số lượng bản ghi qua id tuần tự.

**⚠️ Câu trả lời gây điểm trừ:**
- "UUID tốt hơn vì không trùng" mà không biết chi phí với B-Tree.

**📖 Ôn lại:** [1.1 Khái niệm — Góc nhìn Senior về UUID](../01-giao-trinh/11-database-sql.md#phan-1)

</details>

### Q2. 🟢 Giải thích 1NF, 2NF, 3NF bằng ví dụ. Chuẩn hóa loại bỏ những bất thường (anomaly) nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **1NF**: giá trị nguyên tử, không nhóm lặp (không lưu `product_ids = "P1,P2"`). **2NF**: 1NF + không có **partial dependency** — thuộc tính non-key không phụ thuộc vào một phần của composite key (trong `order_line(order_id, product_id, ..., product_name)`, `product_name` chỉ phụ thuộc `product_id`). **3NF**: 2NF + không có **transitive dependency** (`order_id → customer_id → customer_name`). Chuẩn hóa loại bỏ: *update anomaly* (đổi tên khách phải sửa N dòng), *insert anomaly* (không thêm được sản phẩm khi chưa có đơn), *delete anomaly* (xóa đơn cuối mất luôn thông tin khách).

**Giải thích chi tiết:**
```
order_flat(...)  →  customers(customer_id PK, name, city)
                    orders(order_id PK, order_date, customer_id FK)
                    products(product_id PK, product_name)
                    order_items(order_id, product_id, quantity)  PK = (order_id, product_id)
```
- Quy tắc thực tế: OLTP chuẩn hóa tới 3NF, denormalize có chủ đích khi có số liệu.

**Câu hỏi nối tiếp:**
- *Functional dependency là gì?* → `A → B`: biết A thì xác định duy nhất B (`zip_code → city`).

**⚠️ Câu trả lời gây điểm trừ:**
- Thuộc định nghĩa nhưng không đưa được ví dụ cụ thể.

**📖 Ôn lại:** [1.2 Các dạng chuẩn](../01-giao-trinh/11-database-sql.md#phan-1)

</details>

### Q3. 🟡 BCNF khác 3NF ở đâu? Có khi nào chuẩn hóa lên BCNF lại là đánh đổi không?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** BCNF yêu cầu với **mọi** FD không tầm thường `X → Y`, X phải là super key; 3NF cho phép ngoại lệ khi Y là prime attribute (thuộc candidate key). Ví dụ `teaching(student, course, teacher)` với `(student, course) → teacher` và `teacher → course`: ở 3NF nhưng không BCNF vì `teacher` không là super key → đổi môn của giảng viên phải sửa nhiều dòng. Tách BCNF: `teacher_course(teacher PK, course)` + `student_teacher(student, teacher)` — đổi lại **mất khả năng enforce** `(student, course) → teacher` bằng constraint đơn giản. BCNF không phải lúc nào cũng *dependency-preserving*.

**Giải thích chi tiết:**
- Khi tách mất ràng buộc, phải enforce bằng trigger hoặc logic ứng dụng → thêm rủi ro.
- Ràng buộc kiểu "không chồng giờ đặt phòng" có thể enforce bằng PostgreSQL `EXCLUDE USING gist (room WITH =, during WITH &&)`.

**Câu hỏi nối tiếp:**
- *Thực tế bạn có chuẩn hóa lên BCNF không?* → Đa số schema 3NF đã là BCNF; chỉ trường hợp FD đặc biệt mới cần cân nhắc.

**⚠️ Câu trả lời gây điểm trừ:**
- "BCNF luôn tốt hơn 3NF."

**📖 Ôn lại:** [1.2 — BCNF](../01-giao-trinh/11-database-sql.md#phan-1)

</details>

### Q4. 🟡 Bảng `order_items` có cột `product_name` và `unit_price` copy từ bảng `products`. Đó có phải denormalization không? Khi nào bạn chủ động denormalize?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **Không** — đó là **dữ liệu lịch sử**: sự thật tại thời điểm đặt hàng, bắt buộc phải lưu (giá sản phẩm đổi sau này không được làm đổi hóa đơn cũ). Đây là câu phỏng vấn viên hay gài. Denormalize chủ động khi có **số liệu đo** chứng minh JOIN/aggregate là bottleneck: cột đếm sẵn (`posts.comment_count`), sao chép cột tránh JOIN, bảng tổng hợp/materialized view, `jsonb` cho thuộc tính động, read model riêng (Elasticsearch, CQRS). Luôn ghi rõ **nguồn sự thật** ở đâu và cơ chế đồng bộ/đối soát.

**Giải thích chi tiết:**
| Kỹ thuật | Chi phí |
|---|---|
| Cột đếm sẵn | Hot row khi bài viết viral; cần `UPDATE ... SET c = c + 1` |
| Bảng tổng hợp | Độ trễ dữ liệu, job refresh |
| JSON | Khó ràng buộc, cần GIN/generated column |
| Read model | Eventual consistency, pipeline đồng bộ |
- Đếm sai theo thời gian → reconcile job định kỳ so với `count(*)` thật.

**Câu hỏi nối tiếp:**
- *Comment count cho bài viral 5.000 req/s?* → Cập nhật bất đồng bộ theo batch (`+37` mỗi giây) qua event để tránh hot row lock.

**⚠️ Câu trả lời gây điểm trừ:**
- "Đó là denormalization, nên bỏ và JOIN sang `products`."

**📖 Ôn lại:** [1.3 Khi nào phi chuẩn hóa](../01-giao-trinh/11-database-sql.md#phan-1)

</details>

### Q5. 🟡 Review schema: bảng `user` có cột `role_ids VARCHAR(255) = "1,3,7"`, bảng `product_attribute(entity_id, attr_name, attr_value)` cho mọi thuộc tính, và team bỏ hết FK "cho nhanh". Bạn góp ý gì?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** (1) `role_ids` dạng chuỗi vi phạm 1NF: không index được, không FK được, query `LIKE '%,3,%'` full scan, cập nhật dễ sai → dùng bảng nối `user_role(user_id, role_id)`. (2) EAV cho mọi thứ: query cực khó (pivot), mất type safety và constraint → ngày nay ưu tiên `jsonb` + generated column/GIN cho thuộc tính động thật sự, cột thường cho thuộc tính cố định. (3) Bỏ FK → dữ liệu orphan; nếu thật sự phải bỏ (sharding, write cực nặng) thì cần job kiểm tra toàn vẹn định kỳ.

**Giải thích chi tiết:**
- Lưu ý FK trong PostgreSQL/Oracle **không tự tạo index** trên cột con → nhớ tạo index (Q25, Q45).
- Chi phí FK khi ghi thường nhỏ so với chi phí dữ liệu rác.

**Câu hỏi nối tiếp:**
- *PostgreSQL array `int[]` có được không?* → Được với GIN index cho query chứa phần tử, nhưng vẫn không có FK — chỉ hợp tag/thuộc tính không cần toàn vẹn tham chiếu.

**⚠️ Câu trả lời gây điểm trừ:**
- Chấp nhận cả ba vì "chạy được".

**📖 Ôn lại:** [1.3 — Lỗi thường gặp](../01-giao-trinh/11-database-sql.md#phan-1)

</details>

---

<a id="nhom-b"></a>
## B. SQL nâng cao

### Q6. 🟢 Hai query sau khác nhau thế nào về kết quả?

```sql
-- (A)
SELECT c.name, o.id FROM customers c
LEFT JOIN orders o ON o.customer_id = c.id AND o.status = 'PAID';
-- (B)
SELECT c.name, o.id FROM customers c
LEFT JOIN orders o ON o.customer_id = c.id
WHERE o.status = 'PAID';
```

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** (A) trả **mọi khách hàng**; khách không có đơn PAID vẫn xuất hiện với `o.id = NULL`. (B) điều kiện ở `WHERE` lọc **sau** khi join: các dòng không khớp có `o.status = NULL` → `NULL = 'PAID'` là UNKNOWN → bị loại → LEFT JOIN âm thầm biến thành INNER JOIN (khách không có đơn PAID biến mất).

**Giải thích chi tiết:**
- Quy tắc: điều kiện lọc **bảng bên phải** của LEFT JOIN đặt ở `ON`; điều kiện lọc bảng bên trái đặt ở `WHERE`.
- Muốn "khách không có đơn PAID nào": `LEFT JOIN ... ON ... AND o.status='PAID' WHERE o.id IS NULL` (anti-join) hoặc `NOT EXISTS`.

**Câu hỏi nối tiếp:**
- *Với INNER JOIN có khác không?* → Không, đặt ở ON hay WHERE cho cùng kết quả.

**⚠️ Câu trả lời gây điểm trừ:**
- "Hai câu giống nhau."

**📖 Ôn lại:** [2.1 JOIN types — bẫy ON vs WHERE](../01-giao-trinh/11-database-sql.md#phan-2)

</details>

### Q7. 🟢 Query sau trả về 0 dòng dù chắc chắn có khách hàng chưa giới thiệu ai. Vì sao?

```sql
SELECT name FROM customers WHERE id NOT IN (SELECT referrer_id FROM customers);
```

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Subquery trả về có giá trị `NULL` (khách không có người giới thiệu). `x NOT IN (a, b, NULL)` tương đương `x <> a AND x <> b AND x <> NULL`; vế `x <> NULL` là UNKNOWN → cả biểu thức không bao giờ TRUE → 0 dòng. Sửa: dùng `NOT EXISTS` (không bị ảnh hưởng bởi NULL), hoặc thêm `WHERE referrer_id IS NOT NULL` trong subquery.

**Giải thích chi tiết:**
```sql
SELECT name FROM customers c
WHERE NOT EXISTS (SELECT 1 FROM customers r WHERE r.referrer_id = c.id);
```
- `NOT EXISTS` còn thường được optimizer chuyển thành anti-join hiệu quả.

**Câu hỏi nối tiếp:**
- *`IN` (không NOT) có bị NULL ảnh hưởng không?* → Không làm sai kết quả: dòng khớp vẫn TRUE, NULL chỉ làm các so sánh khác thành UNKNOWN.

**⚠️ Câu trả lời gây điểm trừ:**
- "Do subquery chậm" hoặc không nhận ra vai trò của NULL.

**📖 Ôn lại:** [2.1 — Bẫy NOT IN với NULL](../01-giao-trinh/11-database-sql.md#phan-2)

</details>

### Q8. 🟡 "Subquery luôn chậm hơn JOIN" — bạn đồng ý không? Khi nào subquery thật sự gây vấn đề?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Không đồng ý — đó là ngộ nhận. Optimizer hiện đại thường **unnest** subquery thành join/semi-join. Nhưng có các trường hợp thật: (1) MySQL ≤ 5.5 tối ưu `IN (subquery)` rất tệ (dependent subquery), 5.6+ có semi-join, 8.0.16+ chuyển `EXISTS` thành semi-join; (2) **scalar subquery trong SELECT list** thường vẫn chạy N lần → viết lại bằng JOIN + GROUP BY hoặc `LATERAL`; (3) ngược lại, JOIN bảng 1-N rồi `COUNT`/`SUM` có thể **nhân bản dòng (fan-out)** → số liệu sai, trong khi `EXISTS` không bị.

**Giải thích chi tiết:**
```sql
-- 2 đơn mới nhất mỗi khách (PostgreSQL LATERAL; MySQL 8.0.14+; Oracle 12c+ LATERAL/CROSS APPLY)
SELECT c.name, x.id, x.created_at
FROM customers c
CROSS JOIN LATERAL (
  SELECT o.id, o.created_at FROM orders o
  WHERE o.customer_id = c.id ORDER BY o.created_at DESC LIMIT 2
) x;
```
- Correlated subquery về logic chạy một lần cho mỗi dòng ngoài; thực tế phụ thuộc optimizer — đọc plan để kết luận.

**Câu hỏi nối tiếp:**
- *Ví dụ fan-out?* → `SELECT c.id, SUM(o.amount), COUNT(p.id) FROM customers c JOIN orders o ... JOIN payments p ...` — tổng tiền bị nhân với số payment.

**⚠️ Câu trả lời gây điểm trừ:**
- Khẳng định tuyệt đối một chiều mà không dựa vào plan.

**📖 Ôn lại:** [2.2 Subquery vs JOIN, correlated subquery](../01-giao-trinh/11-database-sql.md#phan-2)

</details>

### Q9. 🟢 Thứ tự thực thi logic của một câu SELECT là gì? Vì sao không dùng được aggregate trong `WHERE`?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Thứ tự logic: `FROM/JOIN → WHERE → GROUP BY → HAVING → window functions → SELECT → DISTINCT → ORDER BY → LIMIT/OFFSET`. `WHERE` chạy trước khi group nên chưa có aggregate → dùng `HAVING` để lọc nhóm. Alias trong SELECT chưa tồn tại lúc `WHERE` chạy (PostgreSQL/Oracle báo lỗi; MySQL cho phép alias trong `HAVING`/`ORDER BY`). Lọc ở `WHERE` rẻ hơn ở `HAVING` vì giảm số dòng phải group.

**Giải thích chi tiết:**
```sql
SELECT customer_id, COUNT(*) AS cnt, SUM(amount) AS total
FROM orders
WHERE status = 'PAID'           -- lọc dòng trước
GROUP BY customer_id
HAVING SUM(amount) >= 500       -- lọc nhóm sau
ORDER BY total DESC;
```
- Đây là thứ tự **logic**, không phải vật lý — optimizer có thể đẩy điều kiện xuống (predicate pushdown).
- Window function chạy sau `HAVING` → muốn lọc theo kết quả window phải bọc subquery/CTE.

**Câu hỏi nối tiếp:**
- *`COUNT(*)` vs `COUNT(col)`?* → `COUNT(col)` bỏ NULL.

**⚠️ Câu trả lời gây điểm trừ:**
- Viết `WHERE SUM(x) > 10` hoặc lọc theo `rn` của ROW_NUMBER ngay trong WHERE.

**📖 Ôn lại:** [2.3 GROUP BY / HAVING và thứ tự thực thi logic](../01-giao-trinh/11-database-sql.md#phan-2)

</details>

### Q10. 🟡 Query sau chạy "đúng" trên dev nhưng production trả tên khách sai. Vì sao?

```sql
SELECT customer_id, name, MAX(amount) FROM orders_with_name GROUP BY customer_id;
```

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Trên MySQL thiếu `ONLY_FULL_GROUP_BY` trong `sql_mode`, cột `name` không nằm trong GROUP BY cũng không là aggregate vẫn được chấp nhận và MySQL trả về giá trị **tùy ý** trong nhóm (không đảm bảo là dòng có `MAX(amount)`). Dev và prod khác `sql_mode` hoặc khác thứ tự vật lý dữ liệu → kết quả khác. PostgreSQL/Oracle báo lỗi ngay. Sửa: bật `ONLY_FULL_GROUP_BY` (mặc định từ 5.7.5); nếu cần "dòng có amount lớn nhất của mỗi khách" thì dùng window function `ROW_NUMBER()` (top-N per group).

**Giải thích chi tiết:**
- Nếu `name` phụ thuộc hàm vào `customer_id` (PK), MySQL 5.7+ nhận diện functional dependency và cho phép một cách đúng đắn.
- Thống nhất `sql_mode` giữa môi trường bằng cấu hình server/migration.

**Câu hỏi nối tiếp:**
- *`ANY_VALUE(name)` dùng khi nào?* → Khi bạn chấp nhận giá trị bất kỳ một cách có chủ đích.

**⚠️ Câu trả lời gây điểm trừ:**
- "Do dữ liệu prod bị lỗi."

**📖 Ôn lại:** [2.3 — lỗi thường gặp ONLY_FULL_GROUP_BY](../01-giao-trinh/11-database-sql.md#phan-2)

</details>

### Q11. 🟡 Khác nhau giữa `ROW_NUMBER`, `RANK`, `DENSE_RANK`? Viết query lấy 2 nhân viên lương cao nhất mỗi phòng ban, đồng lương thì lấy cả.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Với các giá trị 100, 90, 90, 80: `ROW_NUMBER` → 1,2,3,4 (không trùng, phá hòa tùy ý nếu không có tie-breaker); `RANK` → 1,2,2,4 (có nhảy); `DENSE_RANK` → 1,2,2,3 (không nhảy). "Top 2, đồng lương lấy cả" → `DENSE_RANK() <= 2` (lấy 2 mức lương cao nhất) hoặc `RANK() <= 2` (lấy những người có hạng ≤ 2) — phải hỏi lại nghiệp vụ muốn nghĩa nào.

**Giải thích chi tiết:**
```sql
SELECT * FROM (
  SELECT e.*, DENSE_RANK() OVER (PARTITION BY dept ORDER BY salary DESC) AS rk
  FROM employees e
) t
WHERE rk <= 2;
```
- Không lọc `rk` trực tiếp trong `WHERE` cùng cấp vì window chạy sau WHERE (Q9).
- Window function có trên PostgreSQL, Oracle (từ 8i), MySQL 8.0+. MySQL 5.7 phải dùng biến session hoặc correlated subquery.
- Oracle 12c+: `FETCH FIRST 2 ROWS WITH TIES` cho top-N toàn bảng (không theo nhóm).

**Câu hỏi nối tiếp:**
- *Xóa bản ghi trùng giữ bản sớm nhất?* → `ROW_NUMBER() OVER (PARTITION BY user_id, event_type, payload ORDER BY created_at, id)` rồi xóa `rn > 1`; MySQL phải bọc thêm derived table tránh lỗi "can't specify target table".

**⚠️ Câu trả lời gây điểm trừ:**
- Dùng `ROW_NUMBER` cho yêu cầu "đồng lương lấy cả".

**📖 Ôn lại:** [2.4 Window functions](../01-giao-trinh/11-database-sql.md#phan-2)

</details>

### Q12. 🔴 Query tính số dư cộng dồn sau cho kết quả "nhảy cóc" ở những ngày có nhiều giao dịch. Vì sao?

```sql
SELECT id, created_at, amount,
       SUM(amount) OVER (PARTITION BY account_id ORDER BY created_at) AS running_balance
FROM transactions;
```

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Khi có `ORDER BY` mà không chỉ định frame, frame mặc định là `RANGE BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW`. `RANGE` coi các dòng **cùng giá trị ORDER BY** (peers) là một khối → mọi giao dịch cùng `created_at` nhận cùng một tổng (đã cộng cả khối). Sửa: ghi rõ `ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW` và thêm **tie-breaker** để thứ tự xác định: `ORDER BY created_at, id`.

**Giải thích chi tiết:**
```sql
SUM(amount) OVER (PARTITION BY account_id ORDER BY created_at, id
                  ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW)
```
- Thiếu tie-breaker thì ngay cả `ROWS` cũng cho kết quả không xác định giữa các lần chạy.
- Moving average 3 kỳ: `AVG(x) OVER (ORDER BY d ROWS BETWEEN 2 PRECEDING AND CURRENT ROW)`.

**Câu hỏi nối tiếp:**
- *Khi nào `RANGE` lại là đúng?* → Khi muốn gộp theo giá trị, ví dụ `RANGE BETWEEN INTERVAL '7 days' PRECEDING AND CURRENT ROW` (cửa sổ thời gian, PostgreSQL 11+).

**⚠️ Câu trả lời gây điểm trừ:**
- Không biết frame mặc định là `RANGE`.

**📖 Ôn lại:** [2.4 — ROWS vs RANGE](../01-giao-trinh/11-database-sql.md#phan-2)

</details>

### Q13. 🟡 Viết recursive CTE lấy toàn bộ cấp dưới của một quản lý kèm độ sâu. Làm sao chống vòng lặp vô hạn? Oracle cũ viết thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Recursive CTE gồm **anchor member** (điểm bắt đầu) `UNION ALL` **recursive member** (join bảng với chính CTE). Chống vòng lặp: giới hạn độ sâu (`WHERE depth < 20`), mang theo path và dừng khi gặp lại id (PostgreSQL 14+ có mệnh đề `CYCLE`). Oracle truyền thống: `START WITH ... CONNECT BY NOCYCLE PRIOR id = manager_id` với `LEVEL`, `SYS_CONNECT_BY_PATH`.

**Giải thích chi tiết:**
```sql
WITH RECURSIVE org AS (                           -- Oracle: bỏ RECURSIVE
  SELECT id, name, manager_id, 1 AS depth, CAST(name AS VARCHAR(1000)) AS path
  FROM employees WHERE id = 2
  UNION ALL
  SELECT e.id, e.name, e.manager_id, o.depth + 1, CAST(o.path || ' > ' || e.name AS VARCHAR(1000))
  FROM employees e JOIN org o ON e.manager_id = o.id
  WHERE o.depth < 20
)
SELECT * FROM org ORDER BY path;
```
```sql
SELECT LEVEL, LPAD(' ', 2*(LEVEL-1)) || name AS tree, SYS_CONNECT_BY_PATH(name, '/') AS path
FROM employees START WITH id = 2
CONNECT BY NOCYCLE PRIOR id = manager_id ORDER SIBLINGS BY name;
```
- Recursive CTE còn dùng sinh dãy ngày cho báo cáo có ngày trống (PostgreSQL có sẵn `generate_series`).
- PostgreSQL < 12 luôn materialize CTE (optimization fence); 12+ tự inline nếu tham chiếu một lần, ép bằng `MATERIALIZED`/`NOT MATERIALIZED`.

**Câu hỏi nối tiếp:**
- *Cây danh mục kèm tổng doanh thu cộng dồn từ con?* → Recursive CTE sinh cặp (ancestor, descendant) rồi JOIN doanh thu và GROUP BY ancestor.

**⚠️ Câu trả lời gây điểm trừ:**
- Load cả cây về Java rồi đệ quy gọi DB từng node (N+1).

**📖 Ôn lại:** [2.5 CTE & recursive CTE](../01-giao-trinh/11-database-sql.md#phan-2)

</details>

### Q14. 🔴 Code Java sau dùng để "có thì cộng thêm, chưa có thì tạo" tồn kho. Có vấn đề gì? Viết lại trên PostgreSQL, MySQL và Oracle.

```java
Integer qty = jdbc.queryForObject("SELECT qty FROM product_stock WHERE sku = ?", Integer.class, sku);  // null nếu chưa có
if (qty == null) jdbc.update("INSERT INTO product_stock(sku, qty) VALUES (?, ?)", sku, delta);
else            jdbc.update("UPDATE product_stock SET qty = ? WHERE sku = ?", qty + delta, sku);
```

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **Race condition**: hai request cùng thấy "chưa có" → cùng INSERT → một bên lỗi duplicate key (hoặc tạo trùng nếu thiếu unique); hai request cùng đọc `qty=5` → cùng ghi 6 → **lost update**. Dùng UPSERT nguyên tử với phép cộng do DB tính: PostgreSQL `INSERT ... ON CONFLICT (sku) DO UPDATE SET qty = product_stock.qty + EXCLUDED.qty`; MySQL `INSERT ... AS new ON DUPLICATE KEY UPDATE qty = product_stock.qty + new.qty`; Oracle `MERGE`.

**Giải thích chi tiết:**
```sql
-- PostgreSQL 9.5+ (cần UNIQUE/PK trên sku)
INSERT INTO product_stock (sku, qty) VALUES (:sku, :delta)
ON CONFLICT (sku) DO UPDATE SET qty = product_stock.qty + EXCLUDED.qty;
-- MySQL 8.0.19+ (VALUES(qty) deprecated từ 8.0.20)
INSERT INTO product_stock (sku, qty) VALUES (?, ?) AS new
ON DUPLICATE KEY UPDATE qty = product_stock.qty + new.qty;
-- Oracle / PostgreSQL 15+
MERGE INTO product_stock t USING (SELECT :sku sku, :delta qty FROM dual) s ON (t.sku = s.sku)
WHEN MATCHED THEN UPDATE SET t.qty = t.qty + s.qty
WHEN NOT MATCHED THEN INSERT (sku, qty) VALUES (s.sku, s.qty);
```
Khác biệt khi đồng thời:
- `ON CONFLICT` của PostgreSQL **atomic** với concurrent insert (speculative insertion trên unique index).
- `MERGE` (Oracle/PostgreSQL) **không** đảm bảo: hai session cùng đi nhánh `NOT MATCHED` → một bên `ORA-00001`/unique violation → cần retry.
- MySQL `ON DUPLICATE KEY UPDATE` khớp với **bất kỳ** unique key nào (nhiều unique key → khó đoán), lấy gap/next-key lock → nguồn deadlock khi batch upsert song song; vẫn "đốt" giá trị AUTO_INCREMENT.
- **Không** dùng `REPLACE INTO` làm upsert: thực chất DELETE rồi INSERT (kích hoạt ON DELETE CASCADE, đổi id, mất cột không truyền).

**Câu hỏi nối tiếp:**
- *Batch upsert từ Java?* → `JdbcTemplate.batchUpdate` + `rewriteBatchedStatements` (MySQL) / `reWriteBatchedInserts` (pgJDBC).

**⚠️ Câu trả lời gây điểm trừ:**
- "Bọc trong `@Transactional` là hết race" — ở READ COMMITTED vẫn lost update.

**📖 Ôn lại:** [2.6 UPSERT](../01-giao-trinh/11-database-sql.md#phan-2)

</details>

### Q15. 🔴 Bảng `logins(user_id, login_date)`. Viết query tìm các chuỗi ngày đăng nhập liên tiếp (streak) của mỗi user.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Bài toán **gaps & islands**. Mẹo: trong một chuỗi ngày liên tiếp, `login_date - ROW_NUMBER()` là **hằng số**. Khử trùng ngày trước, đánh số theo user, trừ đi số thứ tự để ra khóa nhóm, rồi GROUP BY khóa đó lấy `MIN`, `MAX`, `COUNT`.

**Giải thích chi tiết:**
```sql
SELECT user_id, MIN(login_date) AS start_d, MAX(login_date) AS end_d, COUNT(*) AS streak
FROM (
  SELECT user_id, login_date,
         login_date - CAST(ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY login_date) AS INT) AS grp
  FROM (SELECT DISTINCT user_id, login_date FROM logins) d
) t
GROUP BY user_id, grp
ORDER BY user_id, start_d;
```
- Ví dụ ngày 1,2,3,5,6 → rn 1,2,3,4,5 → grp 0,0,0,1,1 → hai đảo.
- Phải `DISTINCT` trước, nếu không một ngày đăng nhập hai lần làm lệch số thứ tự.
- MySQL: `DATE_SUB(login_date, INTERVAL rn DAY)`; Oracle: `login_date - rn` với kiểu `DATE`.
- Biến thể: dùng `LAG` để đánh dấu điểm bắt đầu đảo (`CASE WHEN login_date - LAG(login_date) > 1 THEN 1 END`) rồi cộng dồn.

**Câu hỏi nối tiếp:**
- *Streak dài nhất mỗi user?* → Bọc thêm một lớp, `MAX(streak)` hoặc `ROW_NUMBER` theo streak giảm dần.

**⚠️ Câu trả lời gây điểm trừ:**
- Đề xuất kéo toàn bộ dữ liệu về Java để duyệt vòng lặp cho bảng hàng trăm triệu dòng.

**📖 Ôn lại:** [2.4 — Gaps & islands](../01-giao-trinh/11-database-sql.md#phan-2)

</details>

---

<a id="nhom-c"></a>
## C. Index chuyên sâu

### Q16. 🟢 Mô tả cấu trúc B+Tree index. Vì sao bảng 1 tỷ dòng chỉ cần 3–5 lần đọc page để tìm một khóa?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** B+Tree **cân bằng** (mọi leaf cùng độ sâu), node là page 8–16KB chứa hàng trăm khóa → **fan-out lớn** nên chiều cao chỉ log_{fan-out}(N) ≈ 3–5 tầng cho 1 tỷ dòng; root và branch gần như luôn nằm trong cache. Leaf chứa khóa đã sắp xếp + con trỏ tới dòng (ROWID/ctid/PK), các leaf **nối nhau** thành danh sách liên kết đôi → range scan chỉ cần tìm điểm đầu rồi đi ngang.

**Giải thích chi tiết:**
Một index lookup có 3 bước (*Use The Index, Luke*):
1. **Tree traversal** — rẻ, cố định.
2. **Đi theo leaf chain** — tỉ lệ với số khóa khớp.
3. **Truy cập bảng** — thường đắt nhất vì mỗi dòng có thể ở page khác nhau (random I/O).
- Chi phí ghi: mỗi INSERT/UPDATE/DELETE phải cập nhật mọi index liên quan; page đầy → page split.

**Câu hỏi nối tiếp:**
- *Vì sao index "trả về nhiều dòng" lại chậm hơn full scan?* → Bước 3 là random I/O cho từng dòng; full scan đọc tuần tự multi-block.

**⚠️ Câu trả lời gây điểm trừ:**
- Nhầm B-Tree với binary tree.

**📖 Ôn lại:** [3.1 Cấu trúc B-Tree](../01-giao-trinh/11-database-sql.md#phan-3)

</details>

### Q17. 🟡 Clustered index của InnoDB khác heap table của PostgreSQL/Oracle thế nào? Hệ quả với việc chọn primary key?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** InnoDB: **bảng chính là B+Tree sắp theo PK** — dữ liệu dòng nằm trong leaf của PK index. **Secondary index** lưu `(cột index, giá trị PK)` → tra qua secondary = 2 lần duyệt cây (secondary → PK → clustered). Hệ quả: **PK nên ngắn và tăng dần**; PK dài (VARCHAR, UUID) làm mọi secondary index phình theo. PostgreSQL/Oracle: dòng nằm ở **heap** không thứ tự; mọi index (kể cả PK) đều trỏ tới vị trí vật lý (`ctid`/`ROWID`) → lookup = 1 cây + 1 lần đọc heap.

**Giải thích chi tiết:**
- InnoDB không có PK → dùng UNIQUE NOT NULL đầu tiên → không có nữa tự tạo `DB_ROW_ID` ẩn (dùng chung bộ đếm toàn cục → contention).
- PostgreSQL: UPDATE tạo tuple mới → mọi index thêm entry mới, trừ **HOT update** (không cột index nào đổi và page còn chỗ) → đặt `fillfactor` < 100 cho bảng update nhiều.
- Oracle **IOT** (`ORGANIZATION INDEX`) ≈ clustered index, hợp bảng tra theo PK/bảng mapping. PostgreSQL `CLUSTER` chỉ sắp xếp lại một lần, không duy trì.

**Câu hỏi nối tiếp:**
- *Covering index trên MySQL tận dụng điều gì?* → Secondary index ngầm chứa PK → `(customer_id, created_at, amount)` phủ được `SELECT id, created_at, amount`.

**⚠️ Câu trả lời gây điểm trừ:**
- "Mọi DB đều lưu dữ liệu theo PK."

**📖 Ôn lại:** [3.2 Clustered vs secondary index, heap table & IOT](../01-giao-trinh/11-database-sql.md#phan-3)

</details>

### Q18. 🟡 Có index `(a, b, c)`. Những điều kiện nào dưới đây dùng được index để seek?

```sql
(1) WHERE a = 1                      (5) WHERE a = 1 AND c = 3
(2) WHERE a = 1 AND b = 2            (6) WHERE a > 1 AND b = 2
(3) WHERE b = 2 AND c = 3            (7) WHERE a = 1 ORDER BY b
(4) WHERE c = 3 AND b = 2 AND a = 1  (8) WHERE a = 1 ORDER BY c
```

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** (1) ✅; (2) ✅; (3) ❌ không seek được (có thể full index scan; Oracle/MySQL 8.0.13+ có *skip scan* khi `a` ít giá trị); (4) ✅ — thứ tự viết trong WHERE không quan trọng; (5) ⚠️ seek theo `a`, `c` chỉ lọc trong leaf (index filter / ICP); (6) ⚠️ range trên `a` → `b` không còn dùng để seek, chỉ filter; (7) ✅ và tránh được sort; (8) ❌ phải sort theo `c`.

**Giải thích chi tiết:**
- Index sắp như danh bạ theo (họ, tên đệm, tên): phải biết "họ" mới seek được.
- Nguyên tắc thứ tự cột: cột so sánh **bằng** (`=`, `IN`) trước, cột **range** sau cùng; ưu tiên một index phục vụ nhiều query — `(a, b)` thay cho cả `(a)` → index `(a)` riêng là **thừa**.
- "Cột selectivity cao nhất đặt trước" là lời khuyên chưa đầy đủ.

**Câu hỏi nối tiếp:**
- *Nên đổi index thành `(a, c, b)` cho (5) không?* → Chỉ khi (5) quan trọng và không phá các query khác; đo bằng plan.

**⚠️ Câu trả lời gây điểm trừ:**
- Cho rằng (3) dùng được vì "có cột của index".

**📖 Ôn lại:** [3.3 Composite index & leftmost prefix](../01-giao-trinh/11-database-sql.md#phan-3)

</details>

### Q19. 🔴 Bảng `orders` 50 triệu dòng có 4 query chạy thường xuyên. Thiết kế **tối đa 3 index** phục vụ tốt nhất.

```sql
Q1: WHERE customer_id = ? ORDER BY created_at DESC LIMIT 20
Q2: WHERE customer_id = ? AND status = ?
Q3: WHERE status = 'PENDING' AND created_at < now() - interval '30 min'
Q4: SELECT customer_id, SUM(amount) FROM orders WHERE created_at >= ? GROUP BY customer_id
```

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:**
```sql
CREATE INDEX ix_cust_date   ON orders (customer_id, created_at DESC);   -- Q1: seek + không Sort
CREATE INDEX ix_cust_status ON orders (customer_id, status);            -- Q2
CREATE INDEX ix_pending     ON orders (created_at) WHERE status = 'PENDING';  -- Q3: partial index
```
Q4: nếu khoảng thời gian hẹp → `(created_at) INCLUDE (customer_id, amount)` (index-only scan); nếu khoảng rộng (phần lớn bảng) → full scan + HashAggregate là hợp lý, hoặc bảng tổng hợp/partition theo thời gian.

**Giải thích chi tiết:**
- Gộp Q1+Q2 thành `(customer_id, status, created_at)`? Q1 không lọc status → phải đọc mọi status rồi sort lại → không tối ưu. Ngược lại `(customer_id, created_at)` cho Q2 vẫn seek theo customer và lọc status trong leaf — nếu mỗi khách ít đơn, có thể bỏ `ix_cust_status` để tiết kiệm ghi.
- Partial index cho `PENDING` nhỏ gọn vì `PENDING` chỉ chiếm tỉ lệ nhỏ; index thường trên `status` sẽ vô dụng với giá trị phổ biến.
- MySQL không có partial index → `(status, created_at)` hoặc generated column.
- Kiểm chứng: `EXPLAIN (ANALYZE, BUFFERS)` — Q1 không có Sort node; đo `pg_relation_size` từng index.

**Câu hỏi nối tiếp:**
- *Trước khi thêm index bạn hỏi gì?* → Query chạy bao nhiêu lần/giây, bảng ghi bao nhiêu lần/giây, index hiện có nào mở rộng được.

**⚠️ Câu trả lời gây điểm trừ:**
- Tạo 4–6 index đơn cột cho từng cột xuất hiện trong WHERE.

**📖 Ôn lại:** [3.3–3.5 và Bài 3.2](../01-giao-trinh/11-database-sql.md#phan-3)

</details>

### Q20. 🟡 Covering index là gì? Vì sao trên PostgreSQL đôi khi đã có covering index mà plan vẫn phải đọc heap?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Covering index chứa **mọi cột** query cần (WHERE + SELECT + ORDER BY) → DB không cần truy cập bảng, bỏ bước đắt nhất (index-only scan). PostgreSQL 11+ có `INCLUDE` để thêm cột vào leaf mà không thuộc khóa sắp xếp. Nhưng PostgreSQL phải kiểm tra **visibility** của tuple (MVCC); nó chỉ bỏ qua heap cho các page được **visibility map** đánh dấu all-visible — map này do VACUUM cập nhật. Bảng ghi nhiều mà autovacuum chậm → `Heap Fetches` cao trong plan.

**Giải thích chi tiết:**
```sql
CREATE INDEX idx_orders_cust_date ON orders (customer_id, created_at) INCLUDE (amount, status);
SELECT created_at, amount FROM orders WHERE customer_id = 1 ORDER BY created_at DESC;
-- EXPLAIN: Index Only Scan ... Heap Fetches: N
```
- MySQL: `EXPLAIN` Extra `Using index`; secondary index đã ngầm chứa PK.
- Đánh đổi: index to hơn, ghi chậm hơn — chỉ cho query nóng.

**Câu hỏi nối tiếp:**
- *Vì sao `SELECT *` phá covering?* → Cần mọi cột → bắt buộc đọc bảng.

**⚠️ Câu trả lời gây điểm trừ:**
- Không biết liên hệ giữa index-only scan và VACUUM.

**📖 Ôn lại:** [3.4 Covering index](../01-giao-trinh/11-database-sql.md#phan-3) · [7.3 PostgreSQL VACUUM](../01-giao-trinh/11-database-sql.md#phan-7)

</details>

### Q21. 🟡 Cột `status` có 99% `DONE`, 1% `PENDING`. Index trên `status` có hữu ích không? Optimizer quyết định dùng index dựa vào đâu?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Hữu ích cho `PENDING` (selectivity 1% — tốt) nhưng vô dụng cho `DONE` (full scan rẻ hơn). Optimizer cần **histogram/most common values** trong statistics mới biết phân bố lệch; nếu không, nó ước lượng đều (~50%) và có thể bỏ index. Giải pháp tốt nhất trên PostgreSQL: **partial index** `WHERE status = 'PENDING'` — nhỏ, nhanh. Optimizer quyết định dựa trên ước lượng số dòng khớp, clustering factor, chi phí random vs sequential I/O (`random_page_cost`) — không có ngưỡng % cố định.

**Giải thích chi tiết:**
- **Cardinality** = số giá trị phân biệt; **selectivity** = tỉ lệ dòng được chọn.
- Bẫy với bind variable: query `WHERE status = ?` dùng chung một plan cho cả hai giá trị → generic plan có thể chọn full scan cho `PENDING` (Q29).
- MySQL 8: `ANALYZE TABLE ... UPDATE HISTOGRAM ON status`.

**Câu hỏi nối tiếp:**
- *Index trên cột boolean?* → Thường vô ích trừ partial index cho giá trị hiếm.

**⚠️ Câu trả lời gây điểm trừ:**
- "Cardinality thấp thì không bao giờ nên index."

**📖 Ôn lại:** [3.5 Selectivity & cardinality](../01-giao-trinh/11-database-sql.md#phan-3)

</details>

### Q22. 🟢 Những điều kiện nào sau đây khiến index trên cột tương ứng **không** được dùng? Viết lại cho sargable.

```sql
(1) WHERE YEAR(created_at) = 2024
(2) WHERE phone = 0912345678            -- phone VARCHAR
(3) WHERE name LIKE '%yen'
(4) WHERE amount * 1.1 > 1000
(5) WHERE LOWER(email) = 'an@x.vn'
(6) WHERE status <> 'DONE'
```

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Cả 6 đều có vấn đề. (1) hàm trên cột → `created_at >= '2024-01-01' AND created_at < '2025-01-01'`; (2) **implicit conversion** — DB ép `phone` sang số cho mỗi dòng → `phone = '0912345678'`; (3) wildcard đầu → không seek được, cần full-text/trigram (`pg_trgm` GIN) hoặc Elasticsearch; (4) biểu thức trên cột → `amount > 1000 / 1.1`; (5) hàm trên cột → **function-based index** `ON users (LOWER(email))` (MySQL 8.0.13+ cần hai lớp ngoặc `((LOWER(email)))`); (6) phủ định thường full scan — nếu tập còn lại nhỏ thì viết thành `IN ('PENDING', 'NEW')` hoặc partial index.

**Giải thích chi tiết:**
- Các lý do khác: OR giữa các cột khác nhau (cần index cả hai — index merge/BitmapOr — hoặc viết lại `UNION ALL`), composite không khớp leftmost prefix, optimizer ước lượng full scan rẻ hơn, Oracle B-Tree **không lưu** entry khi mọi cột đều NULL → `IS NULL` không dùng index (mẹo `(col, 0)`).
- Oracle hay gặp `TRUNC(created_at) = DATE '...'` — cũng non-sargable.

**Câu hỏi nối tiếp:**
- *`LIKE 'Ngu%'` trên PostgreSQL có dùng B-Tree không?* → Chỉ khi collation "C" hoặc index với `text_pattern_ops`.

**⚠️ Câu trả lời gây điểm trừ:**
- Không nhận ra (2) là implicit conversion.

**📖 Ôn lại:** [3.6 Function-based index](../01-giao-trinh/11-database-sql.md#phan-3) · [3.7 Khi nào index KHÔNG được dùng](../01-giao-trinh/11-database-sql.md#phan-3)

</details>

### Q23. 🔴 Kịch bản: query chạy 5 ms trên SQL client nhưng 3 giây khi gọi từ ứng dụng Java. Bạn nghi ngờ những gì?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** (1) **Khác kiểu dữ liệu tham số**: app gọi `setLong()`/`setInt()` cho cột `VARCHAR`, hoặc `setString` (NVARCHAR) cho cột VARCHAR trên SQL Server/Oracle → implicit conversion trên **cột** → mất index; JOIN hai bảng khác charset/collation (utf8 vs utf8mb4) cũng vậy. (2) **Khác tham số/plan**: SQL client dùng literal (custom plan), app dùng bind → generic plan hoặc bind peeking với giá trị lệch (Q29). (3) **Khác session setting**: `search_path`, NLS (Oracle), `optimizer_mode`, isolation. (4) **Chờ chứ không chạy**: chờ connection pool, chờ lock do transaction khác của app, network/fetch size (Oracle mặc định fetch 10 dòng → nhiều round-trip). (5) N+1 — thực ra là hàng trăm query nhỏ.

**Giải thích chi tiết:**
- Cách xác định: lấy **plan thực tế** của chính câu do app chạy (`auto_explain` PostgreSQL, `DBMS_XPLAN.DISPLAY_CURSOR(sql_id)` Oracle, `performance_schema` MySQL) thay vì `EXPLAIN` lại bằng tay; xem wait events (`pg_stat_activity.wait_event`), metric Hikari `acquire`.
- Oracle `EXPLAIN PLAN` không dùng bind peeking và coi bind là VARCHAR2 → có thể khác plan thật.

**Câu hỏi nối tiếp:**
- *Hibernate làm gì gây implicit conversion?* → Map field `Long` cho cột `VARCHAR`, hoặc `String` với `nationalized=true` → kiểm tra mapping kiểu.

**⚠️ Câu trả lời gây điểm trừ:**
- "Do mạng chậm" mà không kiểm chứng.

**📖 Ôn lại:** [3.7 — implicit conversion](../01-giao-trinh/11-database-sql.md#phan-3) · [5.3 — Góc nhìn Senior](../01-giao-trinh/11-database-sql.md#phan-5)

</details>

### Q24. 🟡 Ngoài B-Tree, bạn biết những loại index nào và dùng khi nào? Vì sao không dùng bitmap index trong OLTP?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **Hash** (chỉ `=`, không range/sort; PostgreSQL WAL-logged từ v10; InnoDB có adaptive hash index tự động); **Bitmap** (Oracle — cột cardinality thấp trong data warehouse, kết hợp AND/OR nhiều điều kiện); **GIN** (PostgreSQL — `jsonb @>`, array, full-text, trigram); **GiST/SP-GiST** (geo, range type, exclusion constraint); **BRIN** (bảng log khổng lồ append-only, dữ liệu tương quan thứ tự vật lý — index chỉ vài KB); **Full-text** (MySQL `FULLTEXT`, Oracle Text; tiếng Việt cần tokenizer phù hợp). Bitmap tránh trong OLTP vì một entry bitmap phủ **nhiều dòng** → DML đồng thời khóa lẫn nhau, gây deadlock.

**Giải thích chi tiết:**
- GIN ghi chậm (có `fastupdate` pending list).
- BRIN chỉ hiệu quả khi dữ liệu được ghi theo thứ tự cột (timestamp tăng dần).

**Câu hỏi nối tiếp:**
- *Tìm kiếm `LIKE '%abc%'` trên PostgreSQL?* → `pg_trgm` + GIN.

**⚠️ Câu trả lời gây điểm trừ:**
- Đề xuất bitmap index cho cột `status` của bảng đơn hàng OLTP.

**📖 Ôn lại:** [3.8 Các loại index khác](../01-giao-trinh/11-database-sql.md#phan-3)

</details>

### Q25. 🔴 Bạn cần thêm index cho bảng 500 triệu dòng đang chạy production, và cũng muốn dọn bớt index thừa. Quy trình thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **Tạo index không chặn ghi:** PostgreSQL `CREATE INDEX CONCURRENTLY` (không chạy trong transaction; nếu fail để lại index `INVALID` phải drop và làm lại; đặt `lock_timeout`), MySQL 8 InnoDB online DDL `ALGORITHM=INPLACE, LOCK=NONE` (hoặc gh-ost/pt-online-schema-change), Oracle `CREATE INDEX ... ONLINE`; chạy giờ thấp điểm, theo dõi I/O và replication lag. **Dọn index thừa:** tìm index không dùng (PostgreSQL `pg_stat_user_indexes.idx_scan = 0` qua đủ một chu kỳ nghiệp vụ; MySQL `sys.schema_unused_indexes`, `sys.schema_redundant_indexes`; Oracle `DBA_INDEX_USAGE` 12.2+), index trùng tiền tố (`(a)` khi đã có `(a, b)`); "thử bỏ" an toàn bằng **invisible index** (MySQL 8/Oracle) trước khi drop thật.

**Giải thích chi tiết:**
- Không drop index đang phục vụ unique/FK hoặc query chỉ chạy cuối tháng/cuối năm — kiểm tra đủ chu kỳ.
- Nhớ: FK trong PostgreSQL/Oracle **không tự tạo index** trên cột con → DELETE bảng cha full scan bảng con, ở Oracle còn gây table lock bảng con (Q45). MySQL InnoDB tự tạo.
- Migration tạo index đặt trong Flyway script riêng, cấu hình non-transactional (PostgreSQL CONCURRENTLY).

**Câu hỏi nối tiếp:**
- *Vì sao index thừa có hại?* → Mỗi ghi phải cập nhật, chiếm buffer pool, kéo dài vacuum, tăng WAL/replication.

**⚠️ Câu trả lời gây điểm trừ:**
- `CREATE INDEX` bình thường giữa giờ cao điểm.
- Drop index dựa vào thống kê một ngày.

**📖 Ôn lại:** [3.8 — Góc nhìn Senior và lỗi thường gặp](../01-giao-trinh/11-database-sql.md#phan-3) · [13.3 Thao tác DDL nguy hiểm](../01-giao-trinh/11-database-sql.md#phan-13)

</details>

---

<a id="nhom-d"></a>
## D. Execution plan, JOIN, statistics, tối ưu

### Q26. 🟢 Đọc plan PostgreSQL sau và cho biết bạn nhìn vào những gì.

```
Nested Loop  (cost=0.85..1250.40 rows=12 width=48) (actual time=0.05..2840.11 rows=480000 loops=1)
  ->  Index Scan using ix_addr_city_district on addresses a
        (cost=0.42..8.45 rows=12 width=16) (actual time=0.03..310.2 rows=480000 loops=1)
        Index Cond: ((city = 'HN') AND (district = 'Ba Đình'))
  ->  Index Scan using customers_pkey on customers c
        (cost=0.43..103.2 rows=1 width=32) (actual time=0.004..0.005 rows=1 loops=480000)
        Index Cond: (id = a.customer_id)
Planning Time: 0.3 ms
Execution Time: 2901.5 ms
```

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Đọc cây từ trong ra ngoài, từ dưới lên. Điểm quan trọng nhất: **ước lượng `rows=12` nhưng thực tế `rows=480000`** — sai 40.000 lần. Vì nghĩ chỉ có 12 dòng, optimizer chọn **Nested Loop**, dẫn tới 480.000 lần index lookup vào `customers` (`loops=480000`; thời gian thực của node = actual time × loops). Gốc rễ: statistics coi `city` và `district` **độc lập** (P(city) × P(district)) trong khi district đã hàm ý city. Sửa: extended statistics `CREATE STATISTICS ... (dependencies) ON city, district` + `ANALYZE` → ước lượng đúng → Hash Join.

**Giải thích chi tiết:**
- `cost=startup..total` là đơn vị tương đối, không phải ms.
- `EXPLAIN (ANALYZE, BUFFERS)` thêm `shared hit` (cache) vs `read` (đĩa).
- `EXPLAIN ANALYZE` **chạy thật** → với UPDATE/DELETE bọc `BEGIN; ... ROLLBACK;`.
- Node cần để ý: `Sort Method: external merge Disk` (thiếu `work_mem`), Seq Scan bất thường trên bảng lớn.

**Câu hỏi nối tiếp:**
- *Chênh lệch bao nhiêu là đáng lo?* → 10x trở lên thường là gốc của plan tệ.

**⚠️ Câu trả lời gây điểm trừ:**
- "Đã dùng index rồi nên plan tốt."

**📖 Ôn lại:** [4.1 Đọc execution plan](../01-giao-trinh/11-database-sql.md#phan-4) · [4.3 Statistics — tương quan](../01-giao-trinh/11-database-sql.md#phan-4)

</details>

### Q27. 🟡 Trong `EXPLAIN` của MySQL, cột `type` và `Extra` cho bạn biết gì? Với Oracle bạn xem plan thật bằng cách nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** MySQL `type` từ tốt đến tệ: `system`, `const`, `eq_ref`, `ref`, `range`, `index` (full **index** scan), `ALL` (full **table** scan). `Extra`: `Using index` (covering), `Using where`, `Using index condition` (ICP), `Using filesort` (sort không nhờ index), `Using temporary` (bảng tạm, hay đi với GROUP BY/DISTINCT không có index phù hợp). MySQL 8.0.18+ có `EXPLAIN ANALYZE` (chạy thật), 8.0.16+ có `FORMAT=TREE`. Oracle: `EXPLAIN PLAN` chỉ là ước lượng và **không dùng bind peeking** (coi bind là VARCHAR2) → có thể khác plan đang chạy; plan thật dùng `/*+ GATHER_PLAN_STATISTICS */` + `DBMS_XPLAN.DISPLAY_CURSOR(sql_id, NULL, 'ALLSTATS LAST')` để so E-Rows vs A-Rows, hoặc AWR.

**Giải thích chi tiết:**
- Oracle phần `Predicate Information`: `access` (dùng để seek) vs `filter` (lọc sau) — rất quan trọng khi đánh giá composite index.
- Operation Oracle: `TABLE ACCESS FULL`, `INDEX RANGE SCAN`, `INDEX SKIP SCAN`, `TABLE ACCESS BY INDEX ROWID BATCHED`, `HASH JOIN`...

**Câu hỏi nối tiếp:**
- *`type = index` có tốt không?* → Không hẳn — đọc toàn bộ index, chỉ tốt hơn `ALL` khi index nhỏ hơn bảng nhiều.

**⚠️ Câu trả lời gây điểm trừ:**
- Nhầm `type=index` là "đã dùng index tốt".

**📖 Ôn lại:** [4.1 Đọc execution plan — MySQL & Oracle](../01-giao-trinh/11-database-sql.md#phan-4)

</details>

### Q28. 🟡 So sánh Nested Loop, Hash Join và Merge Join. Mỗi loại tốt khi nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **Nested Loop**: với mỗi dòng bảng ngoài, tìm dòng khớp bảng trong (lý tưởng qua index) — tốt khi outer ít dòng và inner có index, OLTP, cần dòng đầu nhanh; O(N × log M) có index. **Hash Join**: build hash table từ bảng nhỏ, probe bằng bảng lớn — tốt cho hai bảng lớn, **chỉ điều kiện bằng**, không có index phù hợp (báo cáo); O(N + M), tốn bộ nhớ (spill ra đĩa nếu thiếu `work_mem`). **Merge Join**: sắp xếp hai bên theo khóa rồi trộn — tốt khi hai bên đã sắp sẵn (từ index), dữ liệu lớn, điều kiện bằng/range.

**Giải thích chi tiết:**
- MySQL trước 8.0.18 **chỉ có** nested loop (và Block Nested Loop); 8.0.18+ có hash join; MySQL **không có** sort-merge join. PostgreSQL và Oracle có đủ ba.
- Thủ phạm số 1 của query "lúc nhanh lúc chậm": nested loop với ước lượng sai (Q26).
- Thí nghiệm (không dùng production): `SET enable_hashjoin = off` để thấy chênh lệch.

**Câu hỏi nối tiếp:**
- *Sửa plan sai bằng hint được không?* → Phương án cuối; trước đó: statistics, extended stats, viết lại query.

**⚠️ Câu trả lời gây điểm trừ:**
- "Hash join luôn nhanh nhất."

**📖 Ôn lại:** [4.2 Thuật toán JOIN](../01-giao-trinh/11-database-sql.md#phan-4)

</details>

### Q29. 🔴 Bảng `tickets` 5 triệu dòng, 98% `CLOSED`. Query `WHERE status = ?` từ Java khi thì 2 ms, khi thì 4 giây với `status = 'OPEN'`. Giải thích và sửa.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Vấn đề **plan cache với dữ liệu lệch**. PostgreSQL: PgJDBC dùng server-side prepared statement sau `prepareThreshold` (mặc định 5) lần; sau vài lần custom plan, planner có thể chuyển sang **generic plan** (không biết giá trị cụ thể) → chọn full scan vì "trung bình" mỗi giá trị chiếm nhiều dòng → `OPEN` (2%) bị full scan. Oracle tương tự với **bind peeking**: plan tạo từ giá trị của lần parse đầu (ví dụ `CLOSED` → full scan) dùng lại cho `OPEN`; 11g+ có **adaptive cursor sharing** tạo child cursor khi phát hiện bind-sensitive (cần histogram). Sửa: PostgreSQL `plan_cache_mode = force_custom_plan` (cho session/role/query), **partial index** `WHERE status = 'OPEN'`, hoặc tách query riêng theo giá trị hiếm; Oracle bảo đảm có histogram, SQL Plan Baseline cho query trọng yếu.

**Giải thích chi tiết:**
```sql
PREPARE q(text) AS SELECT * FROM tickets WHERE status = $1;
EXECUTE q('CLOSED');  -- lặp 5 lần
EXPLAIN EXECUTE q('OPEN');   -- thấy "status = $1" → generic plan
SET plan_cache_mode = force_custom_plan;
```
- Các nguyên nhân khác của "lúc nhanh lúc chậm": statistics cũ sau bulk load, job thu thập statistics ban đêm đổi plan (Oracle), cache lạnh/nóng, chờ lock.

**Câu hỏi nối tiếp:**
- *Vì sao không đơn giản dùng literal?* → Mất lợi ích parse/plan cache, rủi ro injection, Oracle hard parse storm (Q54).

**⚠️ Câu trả lời gây điểm trừ:**
- "Thêm index trên status" — đã có index, vấn đề là plan không dùng nó.

**📖 Ôn lại:** [Bài 5.3 — Điều tra sự cố "lúc nhanh lúc chậm"](../01-giao-trinh/11-database-sql.md#phan-5) · [11.4 Bind variables](../01-giao-trinh/11-database-sql.md#phan-11)

</details>

### Q30. 🟡 Statistics của optimizer gồm những gì? Những tình huống nào làm statistics sai?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Số dòng, số block, số giá trị phân biệt (NDV), tỉ lệ NULL, min/max, **histogram** (dữ liệu lệch), most common values (PostgreSQL), clustering factor (Oracle). Statistics sai khi: (1) **bulk load** rồi query ngay khi statistics vẫn nói bảng "rỗng" → nested loop thảm họa — luôn `ANALYZE` sau bulk load; (2) **temp table/staging** — autovacuum PostgreSQL không analyze temp table; (3) **cột tương quan** — optimizer coi điều kiện độc lập → cần extended statistics; (4) job thu thập ban đêm (Oracle) đổi plan → "sáng nay chậm mà không ai deploy" → SQL Plan Baseline.

**Giải thích chi tiết:**
```sql
ANALYZE orders;                                                      -- PostgreSQL
ALTER TABLE orders ALTER COLUMN status SET STATISTICS 1000;
CREATE STATISTICS st_city_zip (dependencies, ndistinct) ON city, zip FROM addresses;
ANALYZE TABLE orders UPDATE HISTOGRAM ON status WITH 64 BUCKETS;     -- MySQL 8
EXEC DBMS_STATS.GATHER_TABLE_STATS('APP', 'ORDERS', cascade => TRUE);  -- Oracle
```

**Câu hỏi nối tiếp:**
- *Oracle column group?* → `DBMS_STATS.CREATE_EXTENDED_STATS` cho cột tương quan.

**⚠️ Câu trả lời gây điểm trừ:**
- Không biết optimizer cần statistics.

**📖 Ôn lại:** [4.3 Statistics](../01-giao-trinh/11-database-sql.md#phan-4)

</details>

### Q31. 🔴 Kịch bản: CPU database lên 95% sau đợt tăng traffic, ứng dụng timeout hàng loạt. Bạn tiếp cận thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Đi theo số liệu, không đoán: (1) **Tìm đúng query** theo **tổng thời gian** (`calls × mean_time`), không phải query chậm nhất — `pg_stat_statements`, MySQL `sys.statement_analysis`/slow log + `pt-query-digest`, Oracle `V$SQL`/AWR; (2) xem **đang chạy/đang chờ gì**: `pg_stat_activity` (`wait_event_type`), `SHOW PROCESSLIST`, ASH — nhiều session chờ lock/IO hay thực sự đốt CPU; (3) **tái hiện** với tham số thật và dữ liệu cỡ production, đọc **plan thật**; (4) sửa theo thứ tự rẻ → đắt: viết lại query (sargable, bỏ `SELECT *`, diệt N+1) → statistics → index → schema (denormalize, partition) → cache → kiến trúc; (5) đo lại và kiểm tra tác động phụ; (6) giám sát hồi quy. Giảm tải tạm thời: rate limit/feature flag tắt tính năng nặng, đẩy đọc sang replica.

**Giải thích chi tiết:**
- Pattern phía Java hay gặp: N+1, `SELECT *` đọc cả LOB, IN-list 10.000 phần tử hoặc gọi DB trong vòng lặp, không bind variable (Oracle hard parse), `COUNT(*)` cho phân trang, OFFSET sâu, transaction dài giữ lock.
- "Query chậm" thường là **chờ** (lock, I/O, connection pool) → luôn nhìn wait events trước.
- Không tăng pool size của app khi DB đã quá tải CPU — chỉ làm tệ hơn.

**Câu hỏi nối tiếp:**
- *Nếu thủ phạm là một query mới deploy?* → Rollback/feature flag trước, tối ưu sau.

**⚠️ Câu trả lời gây điểm trừ:**
- "Nâng cấp server DB" là bước đầu tiên.
- Tối ưu query chạy 1 lần/ngày mất 2s mà bỏ qua query 2.000 lần/giây mất 20ms.

**📖 Ôn lại:** [5. Quy trình tối ưu query & slow query log](../01-giao-trinh/11-database-sql.md#phan-5)

</details>

---

<a id="nhom-e"></a>
## E. Transaction, isolation, MVCC

### Q32. 🟢 ACID là gì và mỗi tính chất được DB hiện thực bằng cơ chế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **Atomicity** — tất cả hoặc không gì: undo log (InnoDB, Oracle), tuple version + commit log (PostgreSQL). **Consistency** — chuyển từ trạng thái hợp lệ sang hợp lệ: constraint + **logic ứng dụng** (Kleppmann: chữ C thực ra là trách nhiệm của app). **Isolation** — transaction đồng thời không thấy trạng thái dở dang của nhau ở mức được chọn: lock, MVCC, SSI. **Durability** — commit rồi không mất kể cả crash: **Write-Ahead Log** (redo log InnoDB/Oracle, WAL PostgreSQL) được fsync lúc commit.

**Giải thích chi tiết:**
- Durability có giá: InnoDB `innodb_flush_log_at_trx_commit=1` (mặc định, fsync mỗi commit) vs `2`/`0` nhanh hơn nhưng có thể mất ~1 s giao dịch khi crash; PostgreSQL `synchronous_commit = off` tương tự.
- Với replication async, "commit thành công" **chưa** có nghĩa đã ở replica → failover có thể mất dữ liệu đã commit.

**Câu hỏi nối tiếp:**
- *Vì sao insert từng dòng autocommit chậm?* → Mỗi commit một lần fsync WAL.

**⚠️ Câu trả lời gây điểm trừ:**
- Chỉ đọc định nghĩa, không nói được cơ chế.

**📖 Ôn lại:** [6.1 ACID](../01-giao-trinh/11-database-sql.md#phan-6)

</details>

### Q33. 🟢 Kể các hiện tượng bất thường (anomaly) khi chạy đồng thời, mỗi loại một ví dụ.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **Dirty read** — đọc dữ liệu chưa commit (T1 trừ tiền chưa commit, T2 đọc số dư đã trừ, T1 rollback). **Dirty write** — ghi đè dữ liệu chưa commit (mọi DB thực tế đều ngăn bằng row lock). **Non-repeatable read** — đọc cùng dòng hai lần ra kết quả khác. **Phantom read** — cùng điều kiện, lần sau xuất hiện/biến mất **dòng**. **Lost update** — hai transaction read-modify-write cùng dòng, một cập nhật bị ghi đè. **Write skew** — hai transaction đọc cùng tập dữ liệu, mỗi bên ghi vào **dòng khác nhau** dựa trên điều kiện đã đọc → phá invariant (hai bác sĩ cùng xin nghỉ, không còn ai trực).

**Giải thích chi tiết:**
- Chuẩn SQL định nghĩa isolation level theo dirty read/non-repeatable read/phantom; lost update và write skew **không** có trong bảng chuẩn nhưng là thứ gây bug thật nhiều nhất.

**Câu hỏi nối tiếp:**
- *Mức nào ngăn được write skew?* → SERIALIZABLE của PostgreSQL (SSI); Oracle SERIALIZABLE thì **không** (Q36).

**⚠️ Câu trả lời gây điểm trừ:**
- Không phân biệt được non-repeatable read (giá trị đổi) và phantom (tập dòng đổi).

**📖 Ôn lại:** [6.2 Các hiện tượng bất thường](../01-giao-trinh/11-database-sql.md#phan-6)

</details>

### Q34. 🟡 Isolation level mặc định và hành vi thực tế của REPEATABLE READ, SERIALIZABLE trên MySQL, PostgreSQL, Oracle khác nhau thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Mặc định: MySQL InnoDB **REPEATABLE READ**, PostgreSQL và Oracle **READ COMMITTED**. Oracle chỉ hỗ trợ RC và SERIALIZABLE (+ READ ONLY); PostgreSQL coi READ UNCOMMITTED như RC. **RR**: MySQL — snapshot cho đọc thường nhưng locking read (`FOR UPDATE`, UPDATE/DELETE) đọc **bản mới nhất** + next-key lock; **không phát hiện** lost update kiểu read-modify-write. PostgreSQL RR là **snapshot isolation** thật — phát hiện lost update (`could not serialize access due to concurrent update`), không phantom, nhưng còn write skew. **SERIALIZABLE**: MySQL lock-based (SELECT thường thành `FOR SHARE`) → nhiều lock, dễ deadlock; PostgreSQL **SSI** — optimistic, ngăn cả write skew, ném lỗi `40001` cần retry; Oracle SERIALIZABLE thực chất là snapshot isolation (`ORA-08177`), **không** ngăn write skew.

**Giải thích chi tiết:**
- Spring `Isolation.DEFAULT` = mặc định của DB → cùng code, hành vi khác nhau giữa MySQL và PostgreSQL.
- Nhiều team chuyển MySQL sang RC để giảm gap lock/deadlock (yêu cầu binlog format ROW).

**Câu hỏi nối tiếp:**
- *Chạy ở RR/SERIALIZABLE trên PostgreSQL cần gì phía app?* → Retry cho `40001` (và deadlock `40P01`) ở tầng ngoài cùng của transaction.

**⚠️ Câu trả lời gây điểm trừ:**
- Coi bảng chuẩn SQL là hành vi thực tế của mọi DB.

**📖 Ôn lại:** [6.3 Isolation level theo chuẩn SQL và thực tế từng DB](../01-giao-trinh/11-database-sql.md#phan-6)

</details>

### Q35. 🔴 Hai session cùng trừ tiền tài khoản trên PostgreSQL (READ COMMITTED) như sau. Số dư cuối là bao nhiêu? Đổi sang REPEATABLE READ thì sao? Nêu các cách chống.

```sql
-- balance = 100
-- A: BEGIN; SELECT balance ... -- 100
-- B: BEGIN; SELECT balance ... -- 100
-- A: UPDATE account SET balance = 90 WHERE id=1; COMMIT;   -- app tính 100-10
-- B: UPDATE account SET balance = 80 WHERE id=1; COMMIT;   -- app tính 100-20
```

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Ở RC: số dư cuối **80** — mất khoản trừ 10 của A (**lost update**). Ở RR (PostgreSQL): UPDATE của B báo `ERROR: could not serialize access due to concurrent update` → B rollback, app phải retry (đọc lại 90, ghi 70). MySQL RR thì **không** báo lỗi — vẫn mất cập nhật. Các cách chống theo thứ tự ưu tiên: (1) **atomic update** do DB tính: `UPDATE account SET balance = balance - 10 WHERE id = 1 AND balance >= 10` + kiểm tra số dòng ảnh hưởng; (2) **pessimistic**: `SELECT ... FOR UPDATE` rồi mới tính; (3) **optimistic**: cột `version` (`... WHERE id = ? AND version = ?`, 0 dòng → retry/báo lỗi; JPA `@Version`); (4) isolation cao hơn (PostgreSQL RR/SERIALIZABLE) + retry.

**Giải thích chi tiết:**
- Atomic update nhanh nhất (1 round-trip, lock ngắn nhất); optimistic tệ khi contention cao (retry storm) nhưng tốt khi contention thấp.
- Lỗi tư duy phổ biến: "đã có `@Transactional` thì không race" — transaction ≠ serializability ở mức mặc định.

**Câu hỏi nối tiếp:**
- *Chuyển tiền giữa hai tài khoản với FOR UPDATE thì cần chú ý gì?* → Khóa theo thứ tự id tăng dần để tránh deadlock (Q43).

**⚠️ Câu trả lời gây điểm trừ:**
- Trả lời 70 ở RC.
- Cho rằng MySQL RR cũng báo lỗi như PostgreSQL.

**📖 Ôn lại:** [6.4 Demo lost update & write skew](../01-giao-trinh/11-database-sql.md#phan-6)

</details>

### Q36. 🔴 Logic đặt phòng họp: `SELECT count(*) FROM meeting WHERE room=? AND <chồng giờ>` → nếu 0 thì `INSERT`. Hai người đặt cùng lúc, cùng khung giờ — chuyện gì xảy ra ở RC và RR? Sửa thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Cả hai transaction cùng đọc thấy 0 (không có dòng nào để khóa), cùng INSERT → hai booking chồng giờ. Đây là **write skew** dạng phantom: tiền đề là "**không tồn tại** dòng", nên RC lẫn RR (PostgreSQL snapshot, MySQL RR với đọc thường) đều không ngăn được. Sửa: (1) **SERIALIZABLE + retry** trên PostgreSQL (SSI phát hiện, một bên nhận `40001`); (2) **materialize conflict** — tạo sẵn bảng `room_slot` (mỗi slot 15 phút), booking `SELECT ... FOR UPDATE` các slot liên quan theo **thứ tự tăng dần**; (3) **exclusion constraint** PostgreSQL `EXCLUDE USING gist (room WITH =, tstzrange(start_ts, end_ts) WITH &&)` — đơn giản và mạnh nhất nhưng chỉ có ở PostgreSQL.

**Giải thích chi tiết:**
- Ví dụ "bác sĩ trực" khác ở chỗ tiền đề là các dòng **có tồn tại** → có thể `SELECT ... FROM doctors WHERE on_call FOR UPDATE` để khóa.
- MySQL: locking read (`FOR UPDATE`) ở RR lấy gap lock trên khoảng → chặn insert, nhưng hai bên cùng giữ gap lock rồi cùng insert → deadlock (Q41); một bên bị rollback — đúng nhưng cần retry.
- Oracle SERIALIZABLE **không** ngăn write skew.

**Câu hỏi nối tiếp:**
- *Cách nào ít phụ thuộc vendor nhất?* → Materialize conflict (bảng slot) hoặc khóa dòng `room` (`SELECT ... FROM room WHERE id = ? FOR UPDATE`) để tuần tự hóa theo phòng.

**⚠️ Câu trả lời gây điểm trừ:**
- "Kiểm tra trong code Java trước khi insert là đủ."
- Đề xuất `synchronized` trong ứng dụng nhiều instance.

**📖 Ôn lại:** [6.4 — Write skew](../01-giao-trinh/11-database-sql.md#phan-6) · [Bài 6.3](../01-giao-trinh/11-database-sql.md#phan-6)

</details>

### Q37. 🟡 MVCC là gì? Snapshot được lấy khi nào ở READ COMMITTED và REPEATABLE READ?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** MVCC (Multi-Version Concurrency Control): DB giữ **nhiều phiên bản** của một dòng; mỗi transaction đọc phiên bản phù hợp với **snapshot** của nó → **reader không chặn writer, writer không chặn reader** (writer vẫn chặn writer trên cùng dòng). RC: mỗi **câu lệnh** một snapshot mới → thấy dữ liệu vừa commit giữa hai câu. RR/Snapshot: một snapshot cho **cả transaction** — PostgreSQL lấy ở câu lệnh đầu tiên; InnoDB ở consistent read đầu tiên (hoặc ngay khi `START TRANSACTION WITH CONSISTENT SNAPSHOT`).

**Giải thích chi tiết:**
- MVCC là lý do PostgreSQL/Oracle/InnoDB có concurrency cao hơn mô hình chỉ dùng lock (SQL Server mặc định, DB2).
- MVCC chuyển chi phí từ **lock** sang **storage & dọn dẹp** (undo, dead tuple) — Q38, Q39.
- PostgreSQL không bao giờ có dirty read vì MVCC chỉ trả version đã commit.

**Câu hỏi nối tiếp:**
- *Snapshot RR của InnoDB có áp dụng cho UPDATE không?* → Không, UPDATE/locking read đọc bản mới nhất (current read).

**⚠️ Câu trả lời gây điểm trừ:**
- "MVCC nghĩa là không có lock."

**📖 Ôn lại:** [7.1 Ý tưởng MVCC](../01-giao-trinh/11-database-sql.md#phan-7)

</details>

### Q38. 🔴 Kịch bản: MySQL production chậm dần trong vài giờ, mọi query đọc đều tăng latency dù traffic không đổi. Một kỹ sư nói có job báo cáo đang chạy từ sáng. Liên hệ thế nào? PostgreSQL và Oracle gặp vấn đề tương tự ra sao?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** InnoDB sửa dòng **tại chỗ**, bản cũ vào **undo log** thành version chain (`DB_TRX_ID`, `DB_ROLL_PTR`). Transaction mở dài (job báo cáo RR, connection leak giữ `autocommit=0`, session debug quên commit) giữ **read view** cũ → purge thread **không dọn được** undo → **History list length** tăng hàng triệu → mọi query đọc phải lần chain dài để dựng phiên bản → chậm dần; undo tablespace phình. Kiểm tra: `information_schema.innodb_trx` (transaction lâu nhất), `SHOW ENGINE INNODB STATUS` → History list length; xử lý: kill/commit transaction đó, sửa job (keyset theo PK, transaction ngắn, chạy trên replica). PostgreSQL: bản cũ là **dead tuple** trong heap; transaction dài chặn VACUUM → **bloat** bảng/index, scan chậm. Oracle: undo bị ghi đè khi `undo_retention` không đủ → query dài gặp **`ORA-01555: snapshot too old`** (hay gặp với fetch-across-commit).

**Giải thích chi tiết:**
| | InnoDB / Oracle | PostgreSQL |
|---|---|---|
| Bản cũ | Undo log/tablespace | Dead tuple trong heap |
| UPDATE | Sửa tại chỗ | Insert tuple mới (+ cập nhật mọi index trừ HOT) |
| Dọn | Purge | VACUUM/autovacuum |
| Rollback | Tốn (áp undo ngược) | Rẻ |
| Transaction dài | History list dài / ORA-01555 | Bloat |
- Phòng ngừa: alert khi transaction sống > 5–10 phút hoặc History list length > 1.000.000; PostgreSQL `idle_in_transaction_session_timeout`; HikariCP `leakDetectionThreshold`.

**Câu hỏi nối tiếp:**
- *Vì sao "Uber chuyển từ PostgreSQL sang MySQL (2016)"?* → Write amplification: UPDATE phải cập nhật mọi index và WAL lớn trong replication; PostgreSQL đã cải thiện nhiều (HOT, index deduplication v13).

**⚠️ Câu trả lời gây điểm trừ:**
- "Job báo cáo chỉ đọc nên không ảnh hưởng ai."

**📖 Ôn lại:** [7.2 InnoDB & Oracle: undo log](../01-giao-trinh/11-database-sql.md#phan-7) · [7.3 PostgreSQL](../01-giao-trinh/11-database-sql.md#phan-7)

</details>

### Q39. 🔴 VACUUM của PostgreSQL làm gì? Bảng `orders` 500 triệu dòng bị bloat nặng — bạn xử lý và phòng ngừa thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `VACUUM` thường đánh dấu chỗ của dead tuple để tái sử dụng, cập nhật **visibility map** (giúp index-only scan) và freeze tuple cũ; **không** trả dung lượng cho OS và **không** khóa ghi. `VACUUM FULL` viết lại bảng, trả dung lượng nhưng khóa **ACCESS EXCLUSIVE** → không dùng trên production đang chạy, thay bằng **`pg_repack`**. Với bảng lớn: autovacuum mặc định chờ 20% dòng thay đổi (`autovacuum_vacuum_scale_factor = 0.2`) → 100 triệu dead tuple mới chạy → chỉnh riêng cho bảng (`0.01`), tăng worker/cost limit; giảm write amplification bằng `fillfactor` < 100 để có HOT update; tìm và loại transaction dài chặn vacuum; dữ liệu theo thời gian thì partition và drop partition cũ thay vì DELETE.

**Giải thích chi tiết:**
```sql
SELECT relname, n_live_tup, n_dead_tup, last_autovacuum
FROM pg_stat_user_tables ORDER BY n_dead_tup DESC LIMIT 10;
SELECT pid, now() - xact_start AS age, state, query
FROM pg_stat_activity WHERE xact_start IS NOT NULL ORDER BY age DESC;
ALTER TABLE orders SET (autovacuum_vacuum_scale_factor = 0.01, fillfactor = 90);
```
- **Transaction ID wraparound**: XID 32 bit; nếu VACUUM bị chặn quá lâu không freeze được, PostgreSQL buộc dừng nhận ghi để bảo vệ dữ liệu — hiếm nhưng nghiêm trọng → giám sát `age(datfrozenxid)`.

**Câu hỏi nối tiếp:**
- *VACUUM báo "dead row versions cannot be removed yet"?* → Có transaction (hoặc replication slot, prepared transaction) cũ hơn đang giữ snapshot.

**⚠️ Câu trả lời gây điểm trừ:**
- Chạy `VACUUM FULL` giữa giờ làm việc.

**📖 Ôn lại:** [7.3 PostgreSQL: mỗi phiên bản là một tuple mới](../01-giao-trinh/11-database-sql.md#phan-7)

</details>

---

<a id="nhom-f"></a>
## F. Locking & deadlock

### Q40. 🟡 Trên MySQL InnoDB, vì sao một câu `UPDATE ... WHERE note = 'x'` (cột `note` không có index) có thể làm "đứng" cả bảng? Còn `ALTER TABLE` thì sao?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** InnoDB khóa trên **index record**, không phải dòng vật lý. Không có index phù hợp → phải quét clustered index và ở RR **khóa mọi dòng đã quét** (kèm gap) → gần như khóa cả bảng cho đến khi transaction kết thúc. Với DDL: MySQL **metadata lock (MDL)** — `ALTER TABLE` phải chờ sau một transaction dài đang đọc bảng; trong lúc chờ, **mọi query mới** tới bảng cũng xếp hàng sau `ALTER` → "cả bảng đứng hình". PostgreSQL tương tự với `ACCESS EXCLUSIVE`. Phòng: index cho điều kiện UPDATE/DELETE; DDL production luôn đặt `lock_timeout` (PostgreSQL) / `lock_wait_timeout` (MySQL) và retry.

**Giải thích chi tiết:**
- Shared (S) vs Exclusive (X): `FOR SHARE` (MySQL 8/PostgreSQL; MySQL cũ `LOCK IN SHARE MODE`) vs `FOR UPDATE`/UPDATE/DELETE. Intention lock (IS/IX) ở mức bảng.
- Oracle không bao giờ leo thang lock và không có read lock; SQL Server có lock escalation.
- RC trên InnoDB nhả lock của dòng không khớp WHERE sớm và tắt gap lock cho scan.

**Câu hỏi nối tiếp:**
- *Xem ai đang chặn ai?* → MySQL `performance_schema.data_locks`, `sys.innodb_lock_waits`; PostgreSQL `pg_blocking_pids(pid)`.

**⚠️ Câu trả lời gây điểm trừ:**
- "Row lock chỉ khóa dòng thỏa điều kiện."

**📖 Ôn lại:** [8.1 Row lock & table lock](../01-giao-trinh/11-database-sql.md#phan-8)

</details>

### Q41. 🔴 MySQL RR, bảng `user_device` có `UNIQUE(user_id, device_id)`. Hai transaction cùng chạy "`SELECT ... WHERE user_id=? AND device_id=? FOR UPDATE` (không có dòng) → `INSERT`" với cùng key và bị deadlock. Giải thích bằng gap lock.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Locking read **không tìm thấy dòng** trên RR → InnoDB lấy **gap lock** trên khoảng nơi key đó sẽ nằm (để chống phantom). Gap lock giữa các transaction **không xung đột nhau** → cả hai đều lấy được. Sau đó cả hai INSERT → mỗi INSERT cần **insert intention lock** trên khoảng đó, xung đột với gap lock của bên kia → chờ nhau → **deadlock**; InnoDB phát hiện ngay và rollback một bên. Sửa: bỏ bước SELECT FOR UPDATE, dùng `INSERT IGNORE`/`INSERT ... ON DUPLICATE KEY UPDATE id = id` và kiểm tra affected rows (hoặc INSERT rồi bắt lỗi duplicate key); hoặc chuyển RC (bỏ gap lock cho tìm kiếm).

**Giải thích chi tiết:**
- Các loại lock: **record lock**, **gap lock** (chặn insert vào khoảng), **next-key lock** = record + gap trước nó (mặc định cho scan ở RR), **insert intention lock**.
- Quy tắc: tìm **bằng trên unique index** khớp đúng một dòng → chỉ record lock; non-unique index, range, hoặc không tìm thấy → gap/next-key lock.
- Ví dụ: index `age` có 10, 20, 30; `SELECT ... WHERE age = 20 FOR UPDATE` khóa (10, 20] + (20, 30) → INSERT age 15 và 25 bị chặn, 35 thì không.
- Log deadlock sẽ thấy cả hai giữ `lock_mode X locks gap before rec` và chờ `insert intention`.

**Câu hỏi nối tiếp:**
- *PostgreSQL có gap lock không?* → Không; PostgreSQL dùng predicate lock (SIRead) chỉ ở SERIALIZABLE; pattern trên ở PostgreSQL sẽ ra unique violation thay vì deadlock.

**⚠️ Câu trả lời gây điểm trừ:**
- "Không thể deadlock vì chưa có dòng nào để khóa."

**📖 Ôn lại:** [8.2 Gap lock & next-key lock](../01-giao-trinh/11-database-sql.md#phan-8)

</details>

### Q42. 🟡 Các biến thể của `SELECT ... FOR UPDATE` và cách giới hạn thời gian chờ lock trên từng DB?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `FOR UPDATE NOWAIT` — lỗi ngay nếu dòng bị khóa; `FOR UPDATE SKIP LOCKED` — bỏ qua dòng đang bị khóa (job queue nhiều worker; PostgreSQL 9.5+, MySQL 8.0+, Oracle); `FOR UPDATE WAIT 3` (Oracle) chờ tối đa 3 s; PostgreSQL `SET lock_timeout = '3s'`; MySQL `innodb_lock_wait_timeout` (mặc định **50 s** — quá dài cho OLTP); PostgreSQL còn có `FOR NO KEY UPDATE` (UPDATE thường dùng) không chặn insert bảng con có FK. JPA: `@Lock(PESSIMISTIC_WRITE)` + hint `jakarta.persistence.lock.timeout`.

**Giải thích chi tiết:**
```sql
WITH picked AS (
  SELECT id FROM outbox_jobs WHERE status = 'NEW' ORDER BY id LIMIT 10 FOR UPDATE SKIP LOCKED
)
UPDATE outbox_jobs j SET status = 'PROCESSING', locked_until = now() + interval '5 min'
FROM picked WHERE j.id = picked.id RETURNING j.*;
```
- Mẫu lease: commit ngay trạng thái PROCESSING + `locked_until`; worker chết thì job quá hạn được nhặt lại → handler idempotent.

**Câu hỏi nối tiếp:**
- *`FOR UPDATE` ngoài transaction?* → Auto-commit nhả lock ngay — vô tác dụng.

**⚠️ Câu trả lời gây điểm trừ:**
- Không biết mặc định 50 s của MySQL.

**📖 Ôn lại:** [8.3 SELECT FOR UPDATE và các biến thể](../01-giao-trinh/11-database-sql.md#phan-8)

</details>

### Q43. 🟢 Deadlock là gì? Ví dụ kinh điển và các cách phòng tránh?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Deadlock là chu trình chờ: T1 giữ A chờ B, T2 giữ B chờ A. Ví dụ: chuyển tiền ngược chiều đồng thời — T1 trừ tài khoản 1 rồi cộng tài khoản 2, T2 trừ tài khoản 2 rồi cộng tài khoản 1. DB phát hiện và rollback một transaction (victim). Phòng tránh: (1) **khóa theo thứ tự nhất quán** (luôn khóa `min(a,b)` trước); (2) transaction **ngắn**, không gọi HTTP/chờ người dùng trong transaction; (3) index phù hợp cho UPDATE/DELETE để không khóa thừa; (4) cân nhắc RC trên MySQL để bỏ gap lock; (5) batch DML theo thứ tự PK, chia nhỏ; (6) **retry** khi gặp deadlock — ở hệ thống đồng thời cao, deadlock là bình thường, code phải chịu được.

**Giải thích chi tiết:**
```java
public void transfer(long from, long to, BigDecimal amt) {
    long first = Math.min(from, to), second = Math.max(from, to);
    Account a = repo.findForUpdate(first).orElseThrow();
    Account b = repo.findForUpdate(second).orElseThrow();
    // trừ/cộng theo from/to
}
```
- Mã lỗi: MySQL `1213 (40001)`, PostgreSQL `40P01`, Oracle `ORA-00060`.

**Câu hỏi nối tiếp:**
- *Retry ở đâu?* → Bọc ngoài toàn bộ transaction (không bên trong).

**⚠️ Câu trả lời gây điểm trừ:**
- "Tăng lock timeout để tránh deadlock" — không liên quan.

**📖 Ôn lại:** [8.4 Deadlock](../01-giao-trinh/11-database-sql.md#phan-8)

</details>

### Q44. 🔴 Production có deadlock và cả "blocking chain" làm cạn connection pool. Bạn chẩn đoán từng loại trên MySQL/PostgreSQL/Oracle thế nào? Loại nào nguy hiểm hơn?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **Deadlock** có chu trình, DB tự giải quyết trong vài ms–giây (InnoDB phát hiện ngay bằng wait-for graph; PostgreSQL sau `deadlock_timeout` = 1 s; Oracle ~3 s). **Blocking chain** không có chu trình: một transaction dài chặn hàng trăm transaction khác cho tới timeout → connection pool cạn, timeout dây chuyền — thường **nguy hiểm hơn**. Chẩn đoán deadlock: MySQL `SHOW ENGINE INNODB STATUS` mục `LATEST DETECTED DEADLOCK` (bật `innodb_print_all_deadlocks = ON` để ghi mọi deadlock), đọc HOLDS THE LOCK(S) / WAITING FOR (index nào, loại lock); PostgreSQL server log `Process X waits for ShareLock on transaction Y; blocked by process Z`, `log_lock_waits = on`; Oracle alert log + trace file "Deadlock graph". Chẩn đoán blocking: PostgreSQL `pg_blocking_pids(pid)` trên `pg_stat_activity`; MySQL `sys.innodb_lock_waits`, `performance_schema.data_locks`; Oracle `V$SESSION.BLOCKING_SESSION`.

**Giải thích chi tiết:**
- Lưu ý Oracle `ORA-00060` chỉ rollback **câu lệnh** bị chọn, không phải cả transaction → app phải tự rollback/xử lý.
- Phòng blocking: `lock_timeout`/`innodb_lock_wait_timeout` ngắn cho request OLTP, `idle_in_transaction_session_timeout`, không gọi I/O ngoài trong transaction, alert transaction sống lâu.

**Câu hỏi nối tiếp:**
- *Xử lý tức thời blocking chain?* → Xác định "đầu chuỗi" (blocker gốc), kill session đó (`pg_terminate_backend`, `KILL`), sau đó tìm nguyên nhân trong code.

**⚠️ Câu trả lời gây điểm trừ:**
- Gộp hai hiện tượng làm một; tăng pool size để "chữa".

**📖 Ôn lại:** [8.4 — Chẩn đoán và Góc nhìn Senior](../01-giao-trinh/11-database-sql.md#phan-8)

</details>

### Q45. 🟡 Trên Oracle, DELETE một dòng ở bảng cha gây deadlock/chờ khó hiểu với các session đang ghi bảng con. Bạn nghi điều gì?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **Unindexed foreign key**: cột FK ở bảng con không có index → khi UPDATE PK hoặc DELETE ở bảng cha, Oracle phải lấy **table lock** (mức share) trên bảng con để đảm bảo toàn vẹn → chặn/deadlock với các session đang DML trên bảng con. Đồng thời DELETE bảng cha phải full scan bảng con. Sửa: tạo index trên cột FK; kiểm tra danh sách "unindexed foreign keys" trước go-live. PostgreSQL cũng không tự tạo index cho FK (gây full scan, không gây table lock như Oracle); MySQL InnoDB tự tạo.

**Giải thích chi tiết:**
- Query kiểm tra: so `USER_CONS_COLUMNS` của constraint loại `R` với `USER_IND_COLUMNS` (cột đầu index phải khớp cột FK).
- Đây là lỗi kinh điển ở hệ thống Oracle cũ của ngân hàng/viễn thông.

**Câu hỏi nối tiếp:**
- *Thêm FK vào bảng lớn đang chạy?* → PostgreSQL `NOT VALID` rồi `VALIDATE CONSTRAINT`; Oracle `ENABLE NOVALIDATE`.

**⚠️ Câu trả lời gây điểm trừ:**
- "Oracle bị lỗi lock escalation" — Oracle không leo thang lock.

**📖 Ôn lại:** [8.4 — lỗi thường gặp Oracle](../01-giao-trinh/11-database-sql.md#phan-8) · [3.8 — FK không tự tạo index](../01-giao-trinh/11-database-sql.md#phan-3)

</details>

---

<a id="nhom-g"></a>
## G. Pagination

### Q46. 🟡 Vì sao `LIMIT 20 OFFSET 200000` chậm? Viết keyset pagination cho danh sách sắp `created_at DESC`, chạy được trên cả PostgreSQL và Oracle.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** DB vẫn phải **đọc và bỏ đi 200.000 dòng** đầu → O(offset + limit); trang càng sâu càng chậm, bot nhảy trang sâu làm DB quá tải; ngoài ra OFFSET **không ổn định** — dữ liệu chèn vào giữa hai lần gọi gây trùng/sót. Keyset: nhớ khóa sắp xếp của dòng cuối trang trước và seek tiếp, với **tie-breaker** `id` để thứ tự duy nhất, index `(created_at DESC, id DESC)`.

**Giải thích chi tiết:**
```sql
-- PostgreSQL: row value comparison
SELECT id, created_at, amount FROM orders
WHERE (created_at, id) < (:c, :id)
ORDER BY created_at DESC, id DESC LIMIT 20;

-- Oracle (không hỗ trợ so sánh tuple <) / MySQL cũ: dạng mở rộng + điều kiện dư giúp seek
SELECT id, created_at, amount FROM orders
WHERE created_at <= :c AND (created_at < :c OR id < :id)
ORDER BY created_at DESC, id DESC
FETCH FIRST 20 ROWS ONLY;          -- Oracle 12c+; MySQL/PG: LIMIT 20
```
- API trả cursor opaque (Base64 của `{created_at, id}`); lấy `size + 1` dòng để biết còn trang tiếp. Spring Data 3.1+: `ScrollPosition`/`Window`.
- Nhược: không nhảy trang bất kỳ, không có tổng số trang.

**Câu hỏi nối tiếp:**
- *Điều kiện `:ts IS NULL OR (...)` cho trang đầu có vấn đề gì?* → Có thể làm plan kém; tách hai query cho trang đầu và trang sau.

**⚠️ Câu trả lời gây điểm trừ:**
- Keyset chỉ theo `created_at` không có tie-breaker → sót dòng trùng timestamp.

**📖 Ôn lại:** [9.1–9.2 OFFSET và keyset pagination](../01-giao-trinh/11-database-sql.md#phan-9)

</details>

### Q47. 🟡 Màn hình admin bắt buộc cho nhảy tới trang bất kỳ và hiển thị tổng số trang trên bảng 50 triệu dòng. Bạn làm sao cho nhanh?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** (1) **Deferred join**: subquery chỉ quét index (covering) để lấy 20 id ở offset, rồi join lại bảng — chỉ 20 dòng phải đọc bảng (đặc biệt hiệu quả với InnoDB); (2) **tổng số**: không `COUNT(*)` mỗi request — hiển thị "khoảng 1,2 triệu" từ statistics (`pg_class.reltuples`), tính bất đồng bộ và cache, hoặc chỉ trả "có trang tiếp không"; (3) giới hạn độ sâu trang tối đa / yêu cầu bộ lọc để thu hẹp; (4) batch job xử lý toàn bảng **tuyệt đối** không dùng OFFSET — dùng keyset theo PK.

**Giải thích chi tiết:**
```sql
SELECT o.* FROM orders o
JOIN (SELECT id FROM orders ORDER BY created_at DESC, id DESC LIMIT 20 OFFSET 200000) t
  ON t.id = o.id
ORDER BY o.created_at DESC, o.id DESC;
```
- Oracle trước 12c phân trang bằng ROWNUM lồng 3 tầng (Q53); 12c+ `OFFSET ... FETCH NEXT`.
- Export lớn: keyset theo PK mỗi batch một transaction ngắn, chạy trên replica → tốt hơn streaming cursor (giữ transaction dài) và OFFSET.

**Câu hỏi nối tiếp:**
- *Spring Data `Page` có vấn đề gì?* → Luôn chạy count query (Module 09).

**⚠️ Câu trả lời gây điểm trừ:**
- "Cache toàn bộ bảng vào Redis."

**📖 Ôn lại:** [9.2 — Deferred join và Góc nhìn Senior](../01-giao-trinh/11-database-sql.md#phan-9)

</details>

---

<a id="nhom-h"></a>
## H. Partitioning, sharding, replication, connection

### Q48. 🟡 Partitioning khác sharding thế nào? Lợi ích và hạn chế của partitioning?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **Partitioning** chia một bảng logic thành nhiều phần vật lý **trong cùng một DB** (range theo thời gian, list theo vùng/tenant, hash). **Sharding** chia dữ liệu ra **nhiều server DB**. Lợi ích partitioning: **partition pruning** (query có partition key chỉ quét phần liên quan), quản lý vòng đời — `DROP/DETACH PARTITION` tức thì thay vì DELETE hàng triệu dòng tạo dead tuple, bảo trì từng phần. Hạn chế: query **không** có partition key quét mọi partition; PK/UNIQUE phải chứa partition key (PostgreSQL, MySQL); MySQL không hỗ trợ FK trên bảng partitioned; quá nhiều partition làm planning chậm.

**Giải thích chi tiết:**
```sql
CREATE TABLE events (
  id BIGINT GENERATED ALWAYS AS IDENTITY, created_at TIMESTAMPTZ NOT NULL, payload JSONB,
  PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);
CREATE TABLE events_2024_01 PARTITION OF events FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');
ALTER TABLE events DETACH PARTITION events_2024_01;   -- PG 14+: ... CONCURRENTLY
```
- Oracle interval partitioning tự tạo partition mới.

**Câu hỏi nối tiếp:**
- *Partition có thay index được không?* → Không; pruning giảm phạm vi, trong partition vẫn cần index.

**⚠️ Câu trả lời gây điểm trừ:**
- Dùng hai khái niệm như nhau.

**📖 Ôn lại:** [10.1 Partitioning](../01-giao-trinh/11-database-sql.md#phan-10)

</details>

### Q49. 🔴 Thiết kế sharding cho ví điện tử 50 triệu user, 20.000 TPS ghi. Shard key, ID, chuyển tiền giữa hai shard, thêm node — bạn làm thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Trước hết phải chứng minh cần sharding (đã tối ưu query/index, scale-up, replica, cache, tách service?) — sharding là **phương án cuối**. Thiết kế: **shard key = `user_id`** (mọi truy vấn ví đều theo user, phân bố đều); **nhiều shard logic cố định** (ví dụ 4096, `hash(user_id) % 4096`) ánh xạ qua bảng mapping lên ít cụm vật lý (ban đầu 8) → thêm node chỉ cần **di chuyển nguyên shard logic** (copy + catch-up qua binlog/WAL + cutover ngắn), không rehash toàn bộ. **ID toàn cục**: Snowflake (timestamp + shardId + sequence). **Chuyển tiền chéo shard**: **Saga** — trừ ở shard A (ghi giao dịch PENDING + outbox) → cộng ở shard B idempotent theo `transfer_id` → hoàn tất; lỗi thì bù trừ. **Báo cáo/đối soát toàn hệ thống**: CDC (Debezium) đổ về data warehouse.

**Giải thích chi tiết:**
| Chiến lược | Ưu | Nhược |
|---|---|---|
| Range | Dễ hiểu | Hotspot ở shard mới nhất |
| Hash (`% N`) | Phân đều | Đổi N phải di chuyển gần hết dữ liệu → dùng shard logic cố định/consistent hashing |
| Directory | Linh hoạt | Thêm thành phần phải HA |
- Cái giá: JOIN chéo shard, transaction chéo shard (2PC hoặc Saga), unique constraint toàn cục, rebalancing. Công cụ: Vitess, Citus, ShardingSphere.

**Câu hỏi nối tiếp:**
- *Một merchant lớn nhận hàng triệu giao dịch — hot shard?* → Tách tài khoản merchant thành sub-account/sharded counter, gom số dư bất đồng bộ.

**⚠️ Câu trả lời gây điểm trừ:**
- Shard ngay từ đầu mà không cân nhắc phương án đơn giản hơn.
- `hash % 8` vật lý trực tiếp, không tính đến việc thêm node.

**📖 Ôn lại:** [10.2 Sharding](../01-giao-trinh/11-database-sql.md#phan-10) · [Bài 10.3](../01-giao-trinh/11-database-sql.md#phan-10)

</details>

### Q50. 🟡 Replication đồng bộ, bán đồng bộ, bất đồng bộ khác nhau thế nào? Rủi ro khi failover?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **Async** (mặc định MySQL, PostgreSQL): primary commit không chờ replica → nhanh nhưng failover có thể **mất giao dịch đã commit**. **Semi-sync** (MySQL): chờ ít nhất một replica **nhận** event (chưa chắc đã áp dụng). **Sync** (PostgreSQL `synchronous_standby_names` + `synchronous_commit = on/remote_apply`): an toàn hơn, latency cao hơn, replica chết có thể chặn ghi (cần nhiều standby). Mô hình: single-leader (MySQL binlog, PostgreSQL streaming WAL, Oracle Data Guard), multi-leader (cần giải quyết xung đột), leaderless (quorum W + R > N — Cassandra, DynamoDB).

**Giải thích chi tiết:**
- Theo dõi lag: MySQL `SHOW REPLICA STATUS` (`Seconds_Behind_Source`, 8.0.22+), PostgreSQL `pg_stat_replication.replay_lag`.
- Chọn mức đồng bộ theo RPO của nghiệp vụ: giao dịch tài chính thường cần ít nhất semi-sync/sync tới một standby.

**Câu hỏi nối tiếp:**
- *Replica có giúp scale ghi không?* → Không, chỉ scale đọc và HA.

**⚠️ Câu trả lời gây điểm trừ:**
- "Commit thành công là dữ liệu đã an toàn ở mọi node."

**📖 Ôn lại:** [10.3 Replication](../01-giao-trinh/11-database-sql.md#phan-10)

</details>

### Q51. 🔴 Sau khi chuyển đọc sang read replica, người dùng phàn nàn "cập nhật hồ sơ xong tải lại vẫn thấy dữ liệu cũ", và đôi khi dữ liệu "đi lùi". Giải thích và giải pháp.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Do **replication lag**. "Cập nhật xong không thấy" là vi phạm **read-your-writes**; "dữ liệu đi lùi" là vi phạm **monotonic reads** (hai lần đọc rơi vào hai replica có lag khác nhau). Giải pháp read-your-writes: đọc dữ liệu **của chính user** từ primary; route về primary trong N giây sau lần ghi cuối của user (lưu `lastWriteAt` trong session/Redis); hoặc ghi nhớ vị trí log (LSN/GTID) của lần ghi và chỉ đọc từ replica đã bắt kịp (MySQL `WAIT_FOR_EXECUTED_GTID_SET`, PostgreSQL so `pg_last_wal_replay_lsn()`). Monotonic reads: gắn user vào một replica cố định (sticky).

**Giải thích chi tiết:**
```java
public class RoutingDataSource extends AbstractRoutingDataSource {
    @Override protected Object determineCurrentLookupKey() {
        if (ReadYourWritesContext.mustUsePrimary()) return "primary";   // vừa ghi trong 10s
        return TransactionSynchronizationManager.isCurrentTransactionReadOnly() ? "replica" : "primary";
    }
}
// Bọc LazyConnectionDataSourceProxy để quyết định sau khi transaction đã biết readOnly
```
- Kiểm thử: replica với `recovery_min_apply_delay = '5s'` để tái hiện lag ổn định.

**Câu hỏi nối tiếp:**
- *Consistent prefix reads là gì?* → Thấy câu trả lời trước câu hỏi khi dữ liệu nằm ở các partition có lag khác nhau.

**⚠️ Câu trả lời gây điểm trừ:**
- "Bật replication đồng bộ cho mọi replica" mà không tính tới latency/availability.

**📖 Ôn lại:** [10.3 — Replication lag](../01-giao-trinh/11-database-sql.md#phan-10)

</details>

### Q52. 🟡 PostgreSQL `max_connections=200`, hệ thống 20 pod × Hikari pool 50. Vấn đề gì? PgBouncer có những lưu ý gì?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Tổng connection = **số pod × pool size** = 1.000 > 200 → pod mới không kết nối được khi autoscale. PostgreSQL mỗi connection là một **process** (vài MB RAM, context switch) — hàng nghìn connection trực tiếp làm hiệu năng sụp. Pool nên **nhỏ hơn bạn nghĩ** (điểm khởi đầu `core × 2 + spindle`). Khi nhiều pod: **PgBouncer** transaction pooling — nhưng ở transaction mode không dùng được session state (`SET`, advisory lock theo session, `LISTEN/NOTIFY`); prepared statement chỉ được hỗ trợ ở mức protocol từ PgBouncer 1.21 (trước đó phải tắt server-side prepare ở driver, ví dụ `prepareThreshold=0`).

**Giải thích chi tiết:**
- MySQL thread-per-connection, `max_connections` mặc định 151; Oracle dedicated/shared server, DRCP.
- Lỗi `Connection is not available, request timed out` thường không do pool nhỏ mà do transaction dài, query chậm, HTTP trong `@Transactional` → xem `hikaricp_connections_pending`, bật `leakDetectionThreshold`.

**Câu hỏi nối tiếp:**
- *Tính pool khi rolling deploy?* → Cộng thêm số pod surge (pod cũ + mới cùng sống).

**⚠️ Câu trả lời gây điểm trừ:**
- Tăng `max_connections` lên 2.000.

**📖 Ôn lại:** [10.4 Connection limits](../01-giao-trinh/11-database-sql.md#phan-10) · [Module 09 — HikariCP](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#2-hikaricp)

</details>

---

<a id="nhom-i"></a>
## I. Oracle, PL/SQL, bind variable

### Q53. 🟢 Query Oracle sau muốn lấy 10 đơn giá trị cao nhất. Có đúng không?

```sql
SELECT * FROM orders WHERE ROWNUM <= 10 ORDER BY amount DESC;
```

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **Sai.** `ROWNUM` được gán **trước** `ORDER BY` → lấy 10 dòng bất kỳ rồi mới sắp xếp 10 dòng đó. Đúng (trước 12c): sắp xếp trong subquery rồi lọc ROWNUM bên ngoài; 12c+: `FETCH FIRST 10 ROWS ONLY` (chuẩn SQL:2008), có `WITH TIES` để lấy cả dòng đồng hạng.

**Giải thích chi tiết:**
```sql
SELECT * FROM (SELECT * FROM orders ORDER BY amount DESC) WHERE ROWNUM <= 10;
-- Phân trang trước 12c (3 tầng):
SELECT * FROM (
  SELECT t.*, ROWNUM rn FROM (SELECT * FROM orders ORDER BY created_at DESC, id DESC) t
  WHERE ROWNUM <= :end_row
) WHERE rn > :start_row;
-- 12c+
SELECT * FROM orders ORDER BY created_at DESC OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY;
```
- Bẫy: `WHERE ROWNUM = 2` hoặc `ROWNUM > 1` luôn rỗng (dòng đầu không được gán 1 nên không bao giờ có dòng 2).

**Câu hỏi nối tiếp:**
- *Vì sao phân trang 3 tầng đặt `ROWNUM <= :end_row` ở tầng giữa?* → Cho phép optimizer dùng top-N stop key, không phải đánh số toàn bộ.

**⚠️ Câu trả lời gây điểm trừ:**
- "Đúng, giống LIMIT."

**📖 Ôn lại:** [11.2 ROWNUM vs FETCH FIRST](../01-giao-trinh/11-database-sql.md#phan-11)

</details>

### Q54. 🟡 Hard parse và soft parse trong Oracle là gì? Vì sao nối chuỗi SQL có thể làm sập hệ thống Oracle dưới tải?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Oracle băm text câu SQL để tìm trong **shared pool (library cache)**. **Hard parse** (chưa có): kiểm tra cú pháp, ngữ nghĩa, quyền và **tối ưu hóa** sinh plan — tốn CPU và cần latch/mutex trên shared pool (tài nguyên dùng chung → **không scale**). **Soft parse**: đã có cursor giống hệt → dùng lại plan. Nối literal (`WHERE customer_id = 123`) → mỗi giá trị là một câu SQL khác → hard parse liên tục, shared pool đầy cursor rác, nhiều thread tranh chấp mutex (wait event `library cache: mutex X`) → CPU tăng vọt; kèm theo SQL injection. Dùng `PreparedStatement` với bind variable → một hard parse, phần còn lại soft parse (driver statement cache còn tránh cả soft parse).

**Giải thích chi tiết:**
- `CURSOR_SHARING=FORCE`: Oracle tự thay literal bằng bind — giải pháp tình thế cho app cũ không sửa được code.
- IN-list động (`IN (?, ?, ?)` số phần tử khác nhau) cũng sinh nhiều câu khác nhau → Hibernate `hibernate.query.in_clause_parameter_padding=true`; Oracle giới hạn 1.000 phần tử trong IN list (`ORA-01795`).
- Theo dõi: `V$SQL` có nhiều dòng chỉ khác literal; `V$SYSSTAT` `parse count (hard)`.

**Câu hỏi nối tiếp:**
- *Bind variable có mặt trái không?* → Dữ liệu lệch + bind peeking → plan không tối ưu cho một số giá trị (Q29).

**⚠️ Câu trả lời gây điểm trừ:**
- Chỉ nói bind variable chống injection, không biết khía cạnh hiệu năng.

**📖 Ôn lại:** [11.4 Bind variables, hard parse & soft parse](../01-giao-trinh/11-database-sql.md#phan-11)

</details>

### Q55. 🟡 Khi nào nên và không nên đặt logic trong stored procedure? Gọi procedure từ Java trong một `@Transactional` thì procedure có nên `COMMIT` không?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Nên: xử lý dữ liệu khối lớn ngay tại DB (đối soát cuối ngày — tránh kéo hàng triệu dòng qua mạng), hệ thống legacy nhiều client cùng dùng một logic, bảo mật chỉ cấp quyền EXECUTE. Không nên: logic nghiệp vụ phức tạp, thay đổi thường xuyên (khó version control, unit test, review, CI/CD), đẩy CPU vào DB — tài nguyên đắt và khó scale ngang nhất, vendor lock-in, logic chia đôi Java/DB không ai nắm toàn bộ. Procedure **không nên COMMIT** — để caller (Spring) quản lý transaction; `JdbcTemplate`/`SimpleJdbcCall` dùng chung connection của transaction → exception sau đó rollback cả thay đổi của procedure.

**Giải thích chi tiết:**
```java
try (CallableStatement cs = conn.prepareCall("{call transfer_money(?, ?, ?, ?)}")) {
    cs.setLong(1, 1L); cs.setLong(2, 2L); cs.setBigDecimal(3, new BigDecimal("10"));
    cs.registerOutParameter(4, Types.VARCHAR);
    cs.execute();
    String result = cs.getString(4);
}
// Spring: SimpleJdbcCall; JPA: @Procedure / StoredProcedureQuery
```
- PL/SQL hay gặp: `%TYPE/%ROWTYPE`, `BULK COLLECT` + `FORALL`, package (có state theo session), `REF CURSOR` trả result set, `PRAGMA AUTONOMOUS_TRANSACTION` (ghi log độc lập — tương tự `REQUIRES_NEW`).

**Câu hỏi nối tiếp:**
- *Package state có vấn đề gì với connection pool?* → State gắn session vật lý → request sau mượn connection có thể thấy state của request trước; lỗi `ORA-04068` khi package bị compile lại.

**⚠️ Câu trả lời gây điểm trừ:**
- "Stored procedure luôn nhanh hơn nên đưa hết logic vào DB."

**📖 Ôn lại:** [11.1 PL/SQL cơ bản](../01-giao-trinh/11-database-sql.md#phan-11)

</details>

### Q56. 🟢 Code Java chạy đúng trên PostgreSQL nhưng lỗi khi chuyển sang Oracle: `INSERT` chuỗi rỗng vào cột `NOT NULL` báo `ORA-01400`. Vì sao? Kể thêm vài khác biệt Oracle hay gây bug.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Oracle coi **chuỗi rỗng `''` là `NULL`**: `WHERE name = ''` không bao giờ đúng, `LENGTH('')` là `NULL`, insert `''` vào cột `NOT NULL` → `ORA-01400`. Khác biệt khác: kiểu `DATE` của Oracle **có cả giờ phút giây** (`WHERE d = DATE '2024-01-01'` bỏ sót dòng có giờ); `NVL(a, b)` luôn tính cả `b` còn `COALESCE` short-circuit và nhận nhiều tham số; `DECODE` coi `NULL = NULL` là khớp còn `CASE` thì không; `SYSDATE` theo giờ **server OS**, không theo session time zone; `TO_DATE/TO_CHAR` phụ thuộc NLS → luôn truyền format; `MINUS` thay `EXCEPT`; `FROM DUAL` (23ai đã cho bỏ).

**Giải thích chi tiết:**
- Code đa DB: chuẩn hóa chuỗi rỗng thành null ở tầng ứng dụng, hoặc kiểm tra `IS NULL` thay vì `= ''`.
- Với cột DATE có giờ: dùng range `d >= :day AND d < :day + 1` thay vì `TRUNC(d) = :day` (giữ sargable).

**Câu hỏi nối tiếp:**
- *Hibernate/JPA có che được khác biệt này không?* → Không với giá trị dữ liệu; chỉ dialect lo phần cú pháp.

**⚠️ Câu trả lời gây điểm trừ:**
- "Do driver Oracle lỗi."

**📖 Ôn lại:** [11.5 Hàm Oracle hay gặp & khác biệt](../01-giao-trinh/11-database-sql.md#phan-11)

</details>

### Q57. 🟢 Có dùng sequence để sinh số hóa đơn liên tục theo luật kế toán được không?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **Không.** Sequence **không đảm bảo liên tục**: có gap khi rollback (giá trị đã lấy không trả lại), khi restart instance mất phần cache, trên Oracle RAC mỗi node cache riêng nên còn không tăng theo thời gian. Số hóa đơn liên tục cần **bảng counter** với khóa dòng (`SELECT ... FOR UPDATE` rồi tăng trong cùng transaction tạo hóa đơn) — chấp nhận tuần tự hóa (có thể chia counter theo series/chi nhánh để giảm contention).

**Giải thích chi tiết:**
```sql
CREATE SEQUENCE seq_orders START WITH 1 INCREMENT BY 1 CACHE 1000;
CREATE TABLE t (id NUMBER GENERATED BY DEFAULT ON NULL AS IDENTITY PRIMARY KEY);  -- 12c+
```
- `CACHE` nhỏ/`NOCACHE` → contention khi insert nhiều.
- Với Hibernate pooled optimizer: `allocationSize` phải **khớp** `INCREMENT BY` (Module 09, Q19).

**Câu hỏi nối tiếp:**
- *Bảng counter có thành hot row không?* → Có; giảm bằng cách giữ transaction cấp số thật ngắn, hoặc cấp số ở bước cuối sau khi mọi kiểm tra đã xong.

**⚠️ Câu trả lời gây điểm trừ:**
- "Dùng sequence `NOCACHE` là liên tục" — rollback vẫn tạo gap.

**📖 Ôn lại:** [11.3 Sequence & identity](../01-giao-trinh/11-database-sql.md#phan-11)

</details>

---

<a id="nhom-j"></a>
## J. NoSQL & migration không downtime

### Q58. 🟡 Khi nào bạn chọn MongoDB, Cassandra, Elasticsearch bên cạnh RDBMS? Đồng bộ dữ liệu giữa chúng thế nào cho an toàn?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Mặc định cho hệ thống nghiệp vụ: **bắt đầu với RDBMS** (quan hệ, transaction, ad-hoc query, báo cáo). Thêm NoSQL cho nhu cầu cụ thể (polyglot persistence): **Elasticsearch** cho full-text/faceted search, gõ sai chính tả, tiếng Việt có/không dấu, log analytics — **không phải nguồn sự thật** (near-real-time, không transaction); **Cassandra/ScyllaDB** cho ghi cực lớn, time-series, IoT, lịch sử chat, multi-DC — thiết kế bảng theo query; **MongoDB** (hoặc PostgreSQL `jsonb` + GIN) cho catalog thuộc tính động, CMS; **Redis** cho cache, session, rate limit. Đồng bộ: **Transactional Outbox + CDC (Debezium)** từ nguồn sự thật — **không dual-write** (code ghi DB rồi ghi ES → một bên lỗi là lệch vĩnh viễn); có cơ chế reindex từ nguồn sự thật.

**Giải thích chi tiết:**
- ES với `version_type=external` (version = LSN/`updated_at`) để event cũ đến muộn bị bỏ qua; reindex không downtime bằng alias (`products_v2` rồi chuyển alias nguyên tử).
- "NoSQL không có transaction" là kiến thức cũ: MongoDB có multi-document ACID từ 4.0 (sharded từ 4.2), nhưng chi phí cao.
- CAP: khi có partition chọn C hoặc A; PACELC bổ sung đánh đổi Latency vs Consistency khi không có partition.

**Câu hỏi nối tiếp:**
- *Mỗi kho thêm vào tốn gì?* → Vận hành, backup, giám sát, đồng bộ, kỹ năng đội.

**⚠️ Câu trả lời gây điểm trừ:**
- Chọn MongoDB "vì schema linh hoạt" rồi tự xây JOIN/transaction/ràng buộc trong code.

**📖 Ôn lại:** [12. NoSQL & chọn SQL hay NoSQL](../01-giao-trinh/11-database-sql.md#phan-12)

</details>

### Q59. 🔴 Thiết kế bảng Cassandra cho ứng dụng IoT: 100k thiết bị gửi số đo mỗi 10 giây; cần (1) số đo mới nhất của thiết bị, (2) số đo trong khoảng thời gian, (3) mọi thiết bị vượt ngưỡng trong 5 phút qua.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Cassandra thiết kế **query-first**: mỗi query một bảng phù hợp. (1)+(2): `PRIMARY KEY ((device_id, day), ts) WITH CLUSTERING ORDER BY (ts DESC)` — partition theo thiết bị + **bucket ngày** để partition không phình vô hạn (~8.640 dòng/ngày/thiết bị); số đo mới nhất = `LIMIT 1` trên partition hôm nay; khoảng thời gian = range trên `ts` trong các bucket liên quan. (3) Không thể quét toàn cụm hiệu quả theo điều kiện giá trị → cần bảng riêng partition theo thời gian (`((minute_bucket), device_id)` ghi chỉ khi vượt ngưỡng) hoặc xử lý **streaming** (Kafka Streams/Flink) phát cảnh báo ngay khi nhận dữ liệu.

**Giải thích chi tiết:**
```sql
CREATE TABLE readings_by_device (
  device_id uuid, day date, ts timestamp, value double,
  PRIMARY KEY ((device_id, day), ts)
) WITH CLUSTERING ORDER BY (ts DESC);
```
- Cassandra dùng LSM-tree: ghi tuần tự memtable + SSTable → ghi nhanh; đọc kiểm tra nhiều tầng (bloom filter), cần compaction (TimeWindowCompactionStrategy hợp time-series + TTL).
- Không JOIN, không ad-hoc query; denormalize theo từng query là bình thường.
- Partition quá lớn (hàng trăm MB) → hotspot và chậm — bucket là bắt buộc.

**Câu hỏi nối tiếp:**
- *Vì sao không dùng secondary index của Cassandra cho (3)?* → Secondary index phải hỏi mọi node, kém với cardinality cao và dữ liệu lớn.

**⚠️ Câu trả lời gây điểm trừ:**
- Thiết kế một bảng chuẩn hóa như RDBMS rồi query bằng `ALLOW FILTERING`.

**📖 Ôn lại:** [12.1–12.2 Các họ NoSQL và cách chọn](../01-giao-trinh/11-database-sql.md#phan-12) · [Bài 12.2](../01-giao-trinh/11-database-sql.md#phan-12)

</details>

### Q60. 🔴 Bảng `events` 500 triệu dòng có PK `INT` sắp tràn 2^31. Chuyển sang `BIGINT` không downtime thế nào? Những DDL nào khác cần cẩn thận trên production?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Dùng **expand/contract** vì `ALTER COLUMN TYPE` sẽ rewrite cả bảng dưới lock. Các bước (PostgreSQL): (1) thêm cột `id_new BIGINT` (nullable — thao tác rẻ); (2) trigger đồng bộ `NEW.id_new := NEW.id` cho ghi mới; (3) **backfill theo batch** (keyset theo PK, nghỉ giữa batch, theo dõi replication lag); (4) `CREATE UNIQUE INDEX CONCURRENTLY ... (id_new)`; (5) làm tương tự cho cột FK ở bảng con; (6) trong **một transaction ngắn có `lock_timeout`**: đổi sequence owner, `DROP CONSTRAINT events_pkey, ADD CONSTRAINT events_pkey PRIMARY KEY USING INDEX ...`, đổi tên cột; (7) tạo lại FK bằng `NOT VALID` rồi `VALIDATE CONSTRAINT`. Lock lớn nhất chỉ là bước swap (ms).

**Giải thích chi tiết:**
DDL nguy hiểm khác:
| Thao tác | Cách an toàn |
|---|---|
| Thêm cột `NOT NULL DEFAULT` | PostgreSQL 11+, MySQL 8.0.12+ (`INSTANT`), Oracle 11g+ làm tức thì; bản cũ → nullable → backfill → constraint |
| Thêm `NOT NULL` vào cột có sẵn | `CHECK (col IS NOT NULL) NOT VALID` → `VALIDATE` |
| Tạo index | `CONCURRENTLY` / `ONLINE` / online DDL |
| Đổi tên cột/bảng | Expand/contract hoặc view tạm — đổi trực tiếp phá app cũ đang chạy |
| `ALTER` bảng rất lớn MySQL | gh-ost, pt-online-schema-change |
| Mọi DDL | `SET lock_timeout = '3s'` + retry (tránh chặn dây chuyền sau transaction dài) |
- Bốn câu hỏi trước mỗi migration: lệnh lấy lock gì, bao lâu? phiên bản app trước có chạy với schema mới không? rollback thế nào (rollback **code**, không rollback schema)? ảnh hưởng replication lag và CDC consumer?

**Câu hỏi nối tiếp:**
- *Vì sao `DROP COLUMN` cùng release ngừng dùng cột là sai?* → Pod cũ còn chạy trong rolling update → lỗi SQL hàng loạt; phải để release sau.

**⚠️ Câu trả lời gây điểm trừ:**
- `ALTER TABLE events ALTER COLUMN id TYPE BIGINT` giữa giờ làm việc.
- Dùng `ddl-auto=update` của Hibernate.

**📖 Ôn lại:** [13. Migration dữ liệu không downtime](../01-giao-trinh/11-database-sql.md#phan-13) · [Bài 13.3](../01-giao-trinh/11-database-sql.md#phan-13)

</details>

---

> ✅ **Tự kiểm tra sau khi luyện:** đối chiếu với [Checklist tự đánh giá của Module 11](../01-giao-trinh/11-database-sql.md#checklist-tu-danh-gia). Với các câu 🔴 về locking/MVCC, hãy tự tái hiện bằng hai cửa sổ `psql`/`mysql` ít nhất một lần — trả lời có trải nghiệm thực tế luôn thuyết phục hơn trả lời thuộc lòng. Phần JPA/Hibernate và `@Transactional` xem thêm [bộ câu hỏi Module 09](09-jdbc-jpa-hibernate-transactions.md).

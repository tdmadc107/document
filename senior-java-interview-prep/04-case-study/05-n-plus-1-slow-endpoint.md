# Case 05 — Endpoint từ 80 ms lên 6 giây sau khi "chỉ thêm một field": N+1 với lazy collection và OSIV

> **Chủ đề:** Hibernate N+1, `LAZY`/`EAGER`, Open Session In View, JOIN FETCH, `@EntityGraph`, batch fetching, DTO projection, phân trang + fetch join
> **Module liên quan:** [M09 §6 — Fetching, N+1 và các cách sửa](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#6-fetching) · [M09 §3 — Persistence context](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#3-persistence-context) · [M09 §9 — Spring Data JPA: projection, pagination](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#9-spring-data) · [M09 §2 — HikariCP metrics](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#2-hikaricp) · [M11 §9 — Pagination ở quy mô lớn](../01-giao-trinh/11-database-sql.md#phan-9) · [M15 §6 — Spring Boot test slices](../01-giao-trinh/15-testing.md#p6)
> **Độ khó:** ⭐⭐ (Mid → Senior)
> **Thời gian tự giải gợi ý:** 30 phút

---

## 1. Bối cảnh hệ thống

Cổng B2B của một nhà phân phối hàng tiêu dùng: các đại lý xem lịch sử đơn đặt hàng.

| Thành phần | Chi tiết |
|---|---|
| Stack | Java 21, Spring Boot 3.2, Spring Data JPA, Hibernate 6.4, MapStruct, PostgreSQL 15 (RDS, RTT ~1 ms) |
| Endpoint | `GET /b2b/orders?page=0&size=200` — màn hình "Lịch sử đơn hàng", mặc định 200 đơn/trang |
| Dữ liệu | `orders` 14 triệu dòng; `order_item` 160 triệu dòng (TB 11 dòng/đơn); `product` 85.000 dòng; đại lý lớn nhất có 180.000 đơn |
| Cấu hình | `spring.jpa.open-in-view` **không khai báo** (mặc định `true`); Hikari pool 30/pod; 4 pod |
| Tải | trung bình ~6 RPS, đỉnh ~25 RPS cho endpoint này |

Mô hình:
```java
@Entity @Table(name = "orders")
public class Order {
    @Id Long id;
    Long dealerId;
    Instant createdAt;
    @Enumerated(EnumType.STRING) OrderStatus status;
    BigDecimal total;
    @OneToMany(mappedBy = "order") List<OrderItem> items = new ArrayList<>();   // LAZY (mặc định)
}

@Entity
public class OrderItem {
    @Id Long id;
    @ManyToOne(fetch = FetchType.LAZY) Order order;
    @ManyToOne(fetch = FetchType.LAZY) Product product;
    int quantity;
    BigDecimal unitPrice;
}
```

## 2. Triệu chứng

Sprint vừa rồi, frontend yêu cầu hiển thị **tên và SKU sản phẩm** của từng dòng trong danh sách đơn. Developer thêm vào DTO:
```diff
 public record OrderView(Long id, Instant createdAt, OrderStatus status, BigDecimal total,
+                        List<OrderItemView> items) {}
+public record OrderItemView(String sku, String productName, int quantity, BigDecimal unitPrice) {}
```
MapStruct tự sinh code map `order.getItems()` và `item.getProduct().getName()`. Test unit và review đều qua; trên môi trường dev (20 đơn mẫu) API trả trong 120 ms.

**Sau release (Grafana):**
```
http_server_requests_seconds{uri="/b2b/orders"}   p50: 45 ms → 2.9 s     p95: 80 ms → 6.1 s
hikaricp_connections_usage_seconds p99:            0.06 s → 6.0 s
hikaricp_connections_pending (max):                0 → 41
RDS CPUUtilization:                                15% → 58%
```

**APM trace của một request (đại lý D-1027):**
```
GET /b2b/orders  6,084 ms
 ├─ SELECT ... FROM orders WHERE dealer_id=? ORDER BY created_at DESC LIMIT ? OFFSET ?    11 ms
 ├─ SELECT count(*) FROM orders WHERE dealer_id=?                                        38 ms
 ├─ SELECT ... FROM order_item WHERE order_id=?          × 200                     (~4,8 ms mỗi câu)
 └─ SELECT ... FROM product WHERE id=?                   × 941                     (~4,4 ms mỗi câu)
 Total SQL spans: 1,143
```

**pg_stat_statements (1 giờ):**
```
 calls    | mean_ms | query
----------+---------+-----------------------------------------------------------------------
 4,312,880|   0.41  | select oi1_0.order_id,oi1_0.id,... from order_item oi1_0 where oi1_0.order_id=$1
 9,870,112|   0.12  | select p1_0.id,p1_0.name,p1_0.sku,... from product p1_0 where p1_0.id=$1
```
Mỗi câu riêng lẻ chỉ tốn 0,1–0,4 ms **phía DB**; phần còn lại của ~4,5 ms/câu trong trace là round-trip mạng, driver, hàng đợi trên DB đang bận và hydrate entity. Vấn đề là **số lượng**, nên tối ưu từng câu (thêm index) không giúp gì.

## 3. Câu hỏi đặt ra

1. Vì sao một thay đổi chỉ ở DTO lại sinh hơn 1.000 câu SQL? Code nào phát ra chúng — service hay controller?
2. Vì sao Hikari `pending` tăng và các endpoint khác cũng chậm theo?
3. Bạn sẽ sửa thế nào? Cách "hiển nhiên" `JOIN FETCH` + `Pageable` có bẫy gì?
4. Làm sao để N+1 không quay lại ở PR sau?

> ✋ **Dừng lại và tự giải trước.** Viết ra số câu SQL trước và sau khi sửa cho trang 200 đơn, với từng phương án bạn nghĩ tới.

## 4. Điều tra từng bước

### Bước 1 — Đếm SQL theo request
Bật thống kê Hibernate trên staging:
```yaml
spring.jpa.properties.hibernate.generate_statistics: true
logging.level.org.hibernate.stat: DEBUG
logging.level.org.hibernate.SQL: DEBUG
```
```
o.h.e.i.StatisticalLoggingSessionEventListener : Session Metrics {
    1203400 nanoseconds spent acquiring 1 JDBC connections;
    0 nanoseconds spent releasing 0 JDBC connections;
    48211900 nanoseconds spent preparing 1143 JDBC statements;
    5120443100 nanoseconds spent executing 1143 JDBC statements;
    0 nanoseconds spent executing 0 JDBC batches;
    ...
}
```
1.143 câu, ~5,1 giây thuần chờ DB (RTT + thực thi) → 1 câu đơn + 1 count + 200 câu `order_item` (mỗi đơn một câu) + 941 câu `product` (mỗi sản phẩm **khác nhau** một câu — persistence context đã khử trùng những sản phẩm lặp lại).

### Bước 2 — Câu SQL được phát ra ở đâu?
Đặt breakpoint (hoặc dùng datasource-proxy log kèm stack trace) tại câu `select ... from order_item`:
```
at org.hibernate.collection.spi.AbstractPersistentCollection.initialize(...)
at org.hibernate.collection.spi.PersistentBag.iterator(...)
at com.acme.b2b.order.OrderMapperImpl.orderItemListToOrderItemViewList(OrderMapperImpl.java:58)
at com.acme.b2b.order.OrderMapperImpl.toView(OrderMapperImpl.java:31)
at com.acme.b2b.order.OrderController.list(OrderController.java:42)          ← tầng CONTROLLER
```
```java
@GetMapping("/b2b/orders")
Page<OrderView> list(@AuthenticationPrincipal Dealer dealer, Pageable pageable) {
    return orderService.findByDealer(dealer.id(), pageable)     // service @Transactional(readOnly = true) đã KẾT THÚC
                       .map(orderMapper::toView);               // lazy loading xảy ra ở đây, ngoài transaction
}
```
Service đã trả `Page<Order>` và transaction đã đóng, nhưng nhờ **OSIV** (`OpenEntityManagerInViewInterceptor`), `EntityManager` vẫn mở suốt request nên lazy loading "chạy được" — mỗi lần khởi tạo collection/proxy là một câu SQL ở chế độ auto-commit. Log khởi động đã cảnh báo từ lâu:
```
WARN  JpaBaseConfiguration$JpaWebConfiguration : spring.jpa.open-in-view is enabled by default. Therefore, database
      queries may be performed during view rendering. Explicitly configure spring.jpa.open-in-view to disable this warning
```

### Bước 3 — Vì sao cả service chậm theo?
Với OSIV, connection được mượn ở câu SQL đầu tiên và **giữ đến hết request** (6 s). Đỉnh 25 RPS × 6 s = 150 connection cần đồng thời, trong khi cả cụm chỉ có 4 pod × 30 = 120 connection → `pending` 41, các endpoint khác chờ connection (`hikaricp_connections_acquire` p99 tăng). Một endpoint N+1 trở thành sự cố toàn service — xem thêm case 08.

### Bước 4 — Thử cách sửa "hiển nhiên" và gặp bẫy
```java
@Query(value = "SELECT o FROM Order o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product WHERE o.dealerId = :dealerId",
       countQuery = "SELECT count(o) FROM Order o WHERE o.dealerId = :dealerId")
Page<Order> findByDealerWithItems(Long dealerId, Pageable pageable);
```
Kết quả: số câu SQL còn 2, nhưng log báo:
```
WARN  org.hibernate.orm.query : HHH90003004: firstResult/maxResults specified with collection fetch; applying in memory
```
SQL thực tế **không có `LIMIT`**: Hibernate không thể `LIMIT 200` trên các dòng đã join (sẽ cắt ngang collection), nên tải **toàn bộ** đơn của đại lý rồi cắt trang trong RAM. Với đại lý D-1027 (180.000 đơn × 11 dòng ≈ 2 triệu dòng kết quả), request mất 19 s và heap tăng 1,3 GB — tệ hơn N+1. Bật `hibernate.query.fail_on_pagination_over_collection_fetch=true` để biến cảnh báo này thành exception ngay từ test.

## 5. Nguyên nhân gốc

1. Thêm field vào DTO khiến mapper **duyệt association LAZY** (`items`, `item.product`) cho 200 đơn → N+1 hai tầng (1 + N + M).
2. **OSIV bật mặc định** che giấu lỗi: thay vì `LazyInitializationException` ngay khi dev chạy thử (tín hiệu tốt!), lazy loading âm thầm chạy ở tầng controller, ngoài transaction, và giữ connection cả request.
3. Không có kiểm soát số câu SQL trong test; dữ liệu dev quá nhỏ (20 đơn) nên không ai thấy.
4. Trang mặc định 200 đơn + `Page` (luôn chạy thêm `count(*)`).

## 6. Giải pháp

### Ngắn hạn (hotfix trong ngày)
Bật batch fetching toàn cục — một dòng cấu hình, không đổi code:
```yaml
spring.jpa.properties.hibernate.default_batch_fetch_size: 100
```
Collection `items` của 200 đơn được khởi tạo bằng `... WHERE order_id IN (?, ?, ... 100 tham số)` → 2 câu; 941 product → 10 câu. Tổng ~14 câu, p95 về ~180 ms. Đây là **lưới an toàn**, chưa phải thiết kế đúng.

### Dài hạn — fetch theo use case, ra khỏi OSIV
**(a) Phân trang hai bước: phân trang trên id, fetch join theo id**
```java
public interface OrderRepository extends JpaRepository<Order, Long> {
    @Query("SELECT o.id FROM Order o WHERE o.dealerId = :dealerId ORDER BY o.createdAt DESC, o.id DESC")
    Slice<Long> findIdsByDealer(Long dealerId, Pageable pageable);          // Slice: không chạy count(*)

    @Query("""
           SELECT DISTINCT o FROM Order o
           LEFT JOIN FETCH o.items i
           LEFT JOIN FETCH i.product
           WHERE o.id IN :ids
           """)
    List<Order> findWithItemsByIds(List<Long> ids);
}

@Service
@Transactional(readOnly = true)
public class OrderQueryService {
    public Slice<OrderView> findByDealer(long dealerId, Pageable pageable) {
        Slice<Long> ids = orderRepo.findIdsByDealer(dealerId, pageable);
        Map<Long, Order> byId = orderRepo.findWithItemsByIds(ids.getContent()).stream()
                .collect(Collectors.toMap(Order::getId, Function.identity()));
        List<OrderView> views = ids.getContent().stream()                 // giữ đúng thứ tự sort của bước 1
                .map(byId::get).map(orderMapper::toView).toList();          // map TRONG transaction
        return new SliceImpl<>(views, pageable, ids.hasNext());
    }
}
```
2 câu SQL, dữ liệu đúng 200 đơn. Chỉ fetch **một** bag (`items`) cùng `@ManyToOne` (`product`) nên không gặp `MultipleBagFetchException`.

**(b) Hoặc DTO projection cho màn hình chỉ đọc** — không entity, không persistence context, chỉ lấy cột cần:
```java
public record OrderItemRow(Long orderId, String sku, String productName, int quantity, BigDecimal unitPrice) {}

@Query("""
       SELECT new com.acme.b2b.order.OrderItemRow(i.order.id, p.sku, p.name, i.quantity, i.unitPrice)
       FROM OrderItem i JOIN i.product p
       WHERE i.order.id IN :orderIds
       """)
List<OrderItemRow> findItemRows(List<Long> orderIds);
```
Câu 1 lấy page `orders` (projection các cột header), câu 2 lấy toàn bộ dòng của 200 đơn, ghép bằng `Collectors.groupingBy(OrderItemRow::orderId)`. Nhanh nhất và ít bộ nhớ nhất (~95 ms), đổi lại phải viết query/DTO riêng.

**(c) Tắt OSIV**
```yaml
spring.jpa.open-in-view: false
```
Sau khi tắt, chạy toàn bộ test E2E: mọi chỗ còn lazy loading ngoài service sẽ nổ `LazyInitializationException` — đó là danh sách việc cần sửa, không phải lý do bật lại. **Không** dùng `hibernate.enable_lazy_load_no_trans=true` (mỗi lazy load mở session và connection riêng — còn tệ hơn).

**(d) Giảm phạm vi**: trang mặc định 50, tối đa 100; `Slice` thay `Page` (UI dùng "Xem thêm"); với đại lý lớn, chuyển sang keyset pagination theo `(created_at, id)` — xem case 07.

### So sánh phương án
| Phương án | Số câu SQL (trang 200 đơn) | Ưu | Nhược |
|---|---|---|---|
| Hiện trạng (lazy + OSIV) | 1 + 1 + 200 + 941 = 1.143 | Không phải làm gì | 6 s, giữ connection, DB CPU cao |
| `default_batch_fetch_size=100` | ~14 | 1 dòng cấu hình, an toàn làm mặc định | Vẫn nhiều round-trip; không chữa OSIV |
| `@BatchSize` / `@Fetch(FetchMode.SUBSELECT)` trên association | ~3–14 | Cục bộ từng association | SUBSELECT lặp lại query gốc làm subquery (có thể đắt) |
| `JOIN FETCH` + `Pageable` | 2 nhưng **phân trang trong RAM** | Viết nhanh | HHH90003004: tải toàn bộ, có thể OOM |
| **Hai bước: ids + fetch join** | 2–3 | Đúng trang, dùng entity được | Hai query, phải giữ thứ tự sort |
| `@EntityGraph(attributePaths = {"items", "items.product"})` | Giống JOIN FETCH | Khai báo gọn, tái dùng với derived query | Cùng bẫy phân trang với collection |
| **DTO projection** | 2 | Nhanh nhất, ít bộ nhớ nhất, không lazy | Viết query/DTO riêng cho từng màn hình |

### Kết quả
p95 6,1 s → 95 ms (DTO projection), RDS CPU 58% → 17%, `hikaricp_connections_usage` p99 60 ms; đại lý lớn nhất không còn rủi ro OOM.

## 7. Phòng ngừa

**Test đếm số câu SQL** (datasource-proxy hoặc Hibernate `Statistics`):
```java
@SpringBootTest @AutoConfigureMockMvc @Testcontainers
class OrderListQueryCountIT {
    @Autowired MockMvc mvc;
    @Autowired EntityManagerFactory emf;

    @Test
    void listOrders_usesConstantNumberOfQueries() throws Exception {
        seed(dealerId = 1L, orders = 200, itemsPerOrder = 10);              // dữ liệu đủ lớn để lộ N+1
        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();

        mvc.perform(get("/b2b/orders?size=200").with(dealer(1L))).andExpect(status().isOk());

        assertThat(stats.getPrepareStatementCount()).isLessThanOrEqualTo(3);  // không phụ thuộc số đơn/số dòng
    }
}
```
(Cần `hibernate.generate_statistics=true` trong profile test.)

**Cấu hình mặc định cho mọi dự án:**
- `spring.jpa.open-in-view=false`
- `hibernate.default_batch_fetch_size=50..100` (lưới an toàn)
- `hibernate.query.fail_on_pagination_over_collection_fetch=true`
- Mọi `@ManyToOne`/`@OneToOne` khai báo `fetch = LAZY` tường minh.

**Monitoring:** APM alert khi số SQL span/request > 50; dashboard `pg_stat_statements` top theo `calls` (không chỉ theo `mean_time`); `hikaricp_connections_usage` p99.

**Code review checklist:**
- [ ] Thêm field vào response: field đó đến từ association nào? Đã được fetch trong query chưa?
- [ ] Entity có đi ra khỏi tầng service (controller, Jackson, MapStruct ở controller) không?
- [ ] Query có `JOIN FETCH` collection kèm `Pageable` không?
- [ ] Có test đếm query cho endpoint danh sách không? Dữ liệu test có đủ nhiều phần tử?

## 8. Cách kể lại trong phỏng vấn (STAR, ~1,5 phút)

- **S:** "Cổng B2B của chúng tôi có màn hình lịch sử đơn hàng, 200 đơn/trang. Sau một release chỉ thêm tên sản phẩm vào response, p95 nhảy từ 80 ms lên 6 giây, DB CPU từ 15% lên gần 60%, và các API khác cũng chậm vì thiếu connection."
- **T:** "Tôi được giao xử lý vì là người nắm tầng persistence."
- **A:** "Trace APM cho thấy 1.143 câu SQL mỗi request: một câu `order_item` cho mỗi đơn và một câu `product` cho mỗi sản phẩm — N+1 hai tầng. Stack trace chỉ ra lazy loading xảy ra trong MapStruct ở controller, sau khi transaction service đã đóng, nhờ OSIV bật mặc định — nên connection bị giữ suốt 6 giây. Hotfix là `default_batch_fetch_size` đưa về 14 câu. Tôi thử `JOIN FETCH` với `Pageable` thì gặp cảnh báo HHH90003004 — Hibernate phân trang trong RAM, với đại lý lớn sẽ tải 2 triệu dòng. Bản sửa chính là DTO projection hai bước: phân trang trên id rồi lấy toàn bộ dòng của trang bằng một query; đồng thời tắt OSIV và đổi `Page` sang `Slice`."
- **R:** "p95 về 95 ms, DB CPU về 17%. Tôi thêm integration test assert số câu SQL ≤ 3 cho các endpoint danh sách và đưa ba cấu hình JPA mặc định vào template dự án; từ đó hai PR khác bị test chặn trước khi merge."

## 9. Câu hỏi mở rộng

<details>
<summary>1. Sao không đổi <code>items</code> sang <code>FetchType.EAGER</code> cho xong?</summary>

EAGER là quyết định **toàn cục** cho mọi use case: endpoint chỉ cần header đơn cũng phải tải items. Với JPQL/Spring Data derived query, Hibernate vẫn tải EAGER association bằng **query phụ cho từng entity** (vẫn N+1, chỉ là bị ẩn), và không thể tắt EAGER theo từng query. Quy tắc: mọi association đều LAZY, chọn chiến lược fetch tại query theo use case.
</details>

<details>
<summary>2. Nếu cần fetch cả <code>items</code> và <code>payments</code> (hai <code>List</code>) cùng lúc?</summary>

`JOIN FETCH` hai bag → `MultipleBagFetchException`; đổi sang `Set` thì hết exception nhưng tạo **tích Descartes** (11 items × 3 payments = 33 dòng mỗi đơn). Cách đúng: chia thành nhiều query trong cùng transaction — query 1 fetch `items` cho danh sách id, query 2 fetch `payments` cho cùng danh sách; persistence context tự ghép vào cùng instance `Order`. Hoặc dùng batch fetching cho collection thứ hai, hoặc DTO projection riêng cho từng phần.
</details>

<details>
<summary>3. Hibernate 6 có còn cần <code>DISTINCT</code> khi <code>JOIN FETCH</code> collection không?</summary>

Hibernate 6 tự khử trùng root entity trong kết quả khi fetch collection, nên `DISTINCT` không còn bắt buộc ở JPQL; Hibernate 5 cần `DISTINCT` (và hint `hibernate.query.passDistinctThrough=false` để không đẩy `DISTINCT` xuống SQL, tránh DB phải sort/hash dư thừa). Viết `DISTINCT` vẫn vô hại và giúp code chạy đúng trên cả hai phiên bản.
</details>

<details>
<summary>4. Batch fetching và <code>SUBSELECT</code> khác nhau thế nào?</summary>

Batch fetching (`@BatchSize`/`default_batch_fetch_size`) khởi tạo các proxy/collection chưa load theo lô `WHERE id IN (?, ..., ?)` — số câu = 1 + ceil(N/size). `FetchMode.SUBSELECT` (chỉ cho collection) khởi tạo **toàn bộ** collection của mọi entity được load bởi query gốc trong một câu, bằng cách nhúng lại query gốc làm subquery: `WHERE order_id IN (SELECT id FROM orders WHERE dealer_id = ? ...)`. SUBSELECT chỉ 2 câu nhưng nếu query gốc đắt hoặc có phân trang thì subquery có thể đắt/không giữ `LIMIT` như mong đợi; batch fetching dự đoán được hơn.
</details>

<details>
<summary>5. Tắt OSIV trên một codebase lớn đang chạy — làm thế nào an toàn?</summary>

Đo trước: bật `generate_statistics`/datasource-proxy để tìm các endpoint có SQL phát ra sau khi transaction service kết thúc. Tắt OSIV trên staging, chạy full test E2E + replay traffic, thu thập mọi `LazyInitializationException`. Sửa dần bằng cách cho service trả DTO (map trong transaction) hoặc fetch đủ dữ liệu. Có thể tắt OSIV theo từng nhóm URL bằng cách tự đăng ký `OpenEntityManagerInViewInterceptor` chỉ cho path cũ trong thời gian chuyển đổi. Kết quả đo được thường là `hikaricp_connections_usage` giảm rõ rệt.
</details>

<details>
<summary>6. Vì sao 941 câu <code>product</code> chứ không phải 2.200 (200 đơn × 11 dòng)?</summary>

Persistence context (first-level cache) đảm bảo mỗi `Product` id chỉ có một instance trong một session: khi proxy của product id 501 đã được khởi tạo, các `OrderItem` khác trỏ tới id 501 dùng lại instance đó, không query nữa. Vì vậy số câu bằng số sản phẩm **khác nhau** trong trang. Đây cũng là lý do N+1 khó thấy ở dev: dữ liệu mẫu ít sản phẩm, lặp lại nhiều.
</details>

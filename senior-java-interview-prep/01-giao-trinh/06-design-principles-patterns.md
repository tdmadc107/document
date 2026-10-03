# Module 06 — Clean Code, SOLID, Design Patterns & Architecture

> **Mục tiêu:** sau module này bạn viết được code Java dễ đọc, dễ thay đổi và dễ test; nhận diện được code smell và refactor an toàn; giải thích và áp dụng được SOLID bằng ví dụ thật (không chỉ thuộc định nghĩa); hiện thực đúng và biết *khi nào không nên dùng* các GoF pattern hay gặp, chỉ ra được chúng trong JDK và Spring (đặc biệt Proxy trong Spring AOP); so sánh được layered, hexagonal, clean architecture; áp dụng DDD tactical (entity, value object, aggregate, repository, domain event) và hiểu bounded context, CQRS; thiết kế API tốt và review code với tư cách Senior.
> **Cấp độ:** Cơ bản → Nâng cao (Senior)
> **Thời lượng gợi ý:** 7 ngày (≈ 28–30 giờ)
> **Yêu cầu trước:** Module 01 (Java Core & OOP), Module 02 (Collections & Generics), Module 03 (Concurrency — cho phần Singleton thread-safe). Biết Spring cơ bản là lợi thế cho phần Proxy/AOP.
> **Nguồn tham khảo:**
> - Trong kho: [`Ebook IT/Clean Code.pdf`](../../Ebook%20IT/Clean%20Code.pdf) — Robert C. Martin, *Clean Code*: ch.2 Meaningful Names, ch.3 Functions, ch.4 Comments, ch.5 Formatting, ch.6 Objects and Data Structures (Law of Demeter), ch.7 Error Handling, ch.10 Classes (SRP, cohesion), ch.12 Emergence, ch.17 Smells and Heuristics
> - Trong kho: [`Ebook IT/Design Pattern.pdf`](../../Ebook%20IT/Design%20Pattern.pdf) — tài liệu design pattern (đọc song song với phần 5–7)
> - Trong kho: [`Ebook IT/Newman_Building-Microservices.pdf`](../../Ebook%20IT/Newman_Building-Microservices.pdf) — Sam Newman, *Building Microservices*: phần mô hình hóa service theo bounded context, coupling & cohesion, information hiding
> - Trong kho: [`Java/Head First Java 2nd edition.pdf`](../../Java/Head%20First%20Java%202nd%20edition.pdf) — kế thừa, đa hình, interface (nền tảng OOP); [`Java/OCP-Oracle-Certified-Professional-Java-SE-17-Developer-Study-Guide-Exam-1Z0-829pdf.pdf`](../../Java/OCP-Oracle-Certified-Professional-Java-SE-17-Developer-Study-Guide-Exam-1Z0-829pdf.pdf) — records, sealed classes, immutable objects, encapsulation
> - Ngoài:
>   - Gamma, Helm, Johnson, Vlissides — *Design Patterns: Elements of Reusable Object-Oriented Software* (GoF, 1994)
>   - Joshua Bloch — *Effective Java* (3rd ed.): Item 1 (static factory), Item 2 (Builder), Item 3 (Singleton/enum), Item 13 (clone), Item 15–17 (accessibility, immutability), Item 18 (composition over inheritance), Item 19 (design for inheritance or prohibit it), Item 64 (refer to objects by interfaces), Item 69–77 (exceptions)
>   - Martin Fowler — *Refactoring: Improving the Design of Existing Code* (2nd ed.) và https://refactoring.com/catalog/ ; bliki: AnemicDomainModel, CQRS, TellDontAsk
>   - Robert C. Martin — *Clean Architecture* (2017); Alistair Cockburn — "Hexagonal Architecture" (https://alistair.cockburn.us/hexagonal-architecture/)
>   - Eric Evans — *Domain-Driven Design* (2003); Vaughn Vernon — *Implementing Domain-Driven Design* (2013) và loạt bài "Effective Aggregate Design"
>   - Spring Framework Reference — "Proxying Mechanisms" (AOP): https://docs.spring.io/spring-framework/reference/core/aop/proxying.html
>   - Google Engineering Practices — Code Review: https://google.github.io/eng-practices/review/ ; Google API Design Guide: https://cloud.google.com/apis/design ; RFC 9457 (Problem Details for HTTP APIs)

## Mục lục
1. [Clean Code: naming, functions, comments, error handling](#p1)
2. [Code smells & refactoring](#p2)
3. [SOLID](#p3)
4. [Các nguyên tắc khác: DRY, KISS, YAGNI, composition, Law of Demeter, cohesion/coupling](#p4)
5. [Creational patterns: Singleton, Factory Method, Abstract Factory, Builder, Prototype](#p5)
6. [Structural patterns: Adapter, Decorator, Proxy, Facade, Composite](#p6)
7. [Behavioral patterns: Strategy, Template Method, Observer, Chain of Responsibility, Command, State, Iterator](#p7)
8. [Anti-patterns](#p8)
9. [Kiến trúc: layered, hexagonal, clean architecture](#p9)
10. [Domain-Driven Design & CQRS](#p10)
11. [Nguyên tắc thiết kế API](#p11)
12. [Code review với tư cách Senior](#p12)
13. [Dự án mini của module](#du-an-mini)
14. [Checklist tự đánh giá](#checklist-tu-danh-gia)

---

<a id="p1"></a>
## 1. Clean Code: naming, functions, comments, error handling

### 1.1 Khái niệm
"Clean code" không phải là thẩm mỹ cá nhân mà là **chi phí thay đổi**. Code được *đọc* nhiều hơn *viết* hàng chục lần; mỗi phút người đọc phải dừng lại để hiểu là chi phí thật của team. Robert C. Martin (Clean Code, ch.1) mô tả code sạch là code "đọc như văn xuôi được viết tốt" và "trông như được viết bởi người quan tâm".

### 1.2 Đặt tên (Clean Code ch.2)
| Nguyên tắc | Tệ | Tốt |
|---|---|---|
| Tên thể hiện ý định | `int d; // elapsed days` | `int elapsedDays;` |
| Tránh thông tin sai lệch | `accountList` (thực ra là `Set`) | `accounts` |
| Phân biệt có nghĩa | `getActiveAccount()`, `getActiveAccountInfo()`, `getActiveAccountData()` | một tên duy nhất, rõ nghĩa |
| Đọc được, tìm được | `genymdhms`, `7` (magic number) | `generationTimestamp`, `MAX_RETRIES = 7` |
| Class là danh từ, method là động từ | `class ProcessData`, `void invoice()` | `class InvoiceProcessor`, `void sendInvoice()` |
| Boolean đọc như câu hỏi | `flag`, `status` | `isExpired`, `hasPermission`, `canRetry` |
| Một từ cho một khái niệm | `fetch`/`retrieve`/`get` lẫn lộn | thống nhất trong codebase |
| Dùng ngôn ngữ domain | `Map<String, List<Object>> data` | `Map<CustomerId, List<Order>> ordersByCustomer` |

Độ dài tên tỉ lệ với phạm vi: biến vòng lặp 3 dòng tên `i` là ổn; field sống trong cả class cần tên đầy đủ.

### 1.3 Hàm (Clean Code ch.3)
- **Nhỏ** và **làm một việc** ở **một mức trừu tượng** (stepdown rule: đọc từ trên xuống như một câu chuyện, mỗi hàm gọi các hàm ở mức thấp hơn kế tiếp).
- **Ít tham số** (0–2 lý tưởng, ≥ 4 là tín hiệu cần *parameter object*). **Tránh boolean flag argument** (`render(true)`) — nó tuyên bố hàm làm hai việc.
- **Không side effect ẩn**: `checkPassword()` mà lại khởi tạo session là "nói dối".
- **Command–Query Separation**: hàm hoặc *làm* gì đó (command, trả `void`) hoặc *trả lời* gì đó (query, không đổi trạng thái).
- **Trả về** `Optional`/collection rỗng thay vì `null`.

```java
// TRƯỚC: 1 hàm làm 4 việc, flag argument, magic number, mức trừu tượng lẫn lộn
public double calc(Order o, boolean vip) {
    double t = 0;
    for (Item i : o.getItems()) t += i.getPrice() * i.getQty();
    if (vip) t = t * 0.9;
    if (t > 1000) t = t - 50;
    if (o.getCountry().equals("VN")) t = t * 1.1;
    return t;
}

// SAU: mỗi hàm một việc, đọc như văn xuôi, dùng BigDecimal cho tiền
public Money totalPayable(Order order, Customer customer) {
    Money subtotal = order.subtotal();
    Money afterDiscount = discountPolicy.apply(subtotal, customer);
    return taxPolicy.applyTo(afterDiscount, order.shippingCountry());
}
```

### 1.4 Comment (Clean Code ch.4)
- Comment tốt nhất là comment bạn **không cần viết** vì code đã tự giải thích (đổi tên biến, tách hàm).
- Comment **có giá trị**: giải thích *tại sao* (quyết định nghiệp vụ, workaround bug của thư viện kèm link ticket), cảnh báo hậu quả, Javadoc của **API public**, `TODO` có ticket.
- Comment **có hại**: lặp lại code, nhật ký thay đổi (đã có git), code bị comment-out, comment sai lệch vì code đã đổi mà comment không đổi — *comment nói dối còn tệ hơn không có comment*.

```java
// Tệ: lặp lại code
i++; // tăng i

// Tốt: giải thích "tại sao"
// Ngân hàng đối tác từ chối batch > 500 dòng (xem PAY-1234), nên chia nhỏ ở đây.
private static final int PARTNER_BATCH_LIMIT = 500;
```

### 1.5 Xử lý lỗi (Clean Code ch.7, Effective Java Item 69–77)
- Dùng **exception thay vì error code**; tách luồng xử lý lỗi khỏi luồng chính.
- **Không trả về `null`, không truyền `null`** vào hàm (trả `Optional`, collection rỗng, Null Object).
- **Exception phù hợp với mức trừu tượng** (Item 73): repository không ném `SQLException` lên tầng service; bọc lại kèm *cause*.
- **Checked vs unchecked**: checked cho tình huống caller *có thể phục hồi một cách hợp lý* (Item 70); hầu hết framework hiện đại (Spring) dùng unchecked. Không lạm dụng checked exception — nó phá vỡ Open/Closed (thêm một exception buộc sửa mọi tầng phía trên).
- **Không nuốt exception**: `catch (Exception e) {}` là tội ác. Hoặc xử lý, hoặc log *một lần* rồi ném lại có ngữ cảnh, không log-and-rethrow ở mọi tầng (log trùng lặp).
- **Đừng dùng exception cho control flow** (Item 69).
- Luôn `try-with-resources` cho `AutoCloseable`.

```java
public class OrderRepository {
    private final javax.sql.DataSource ds;
    public OrderRepository(javax.sql.DataSource ds) { this.ds = ds; }

    public java.util.Optional<Order> findById(long id) {
        String sql = "SELECT id, status FROM orders WHERE id = ?";
        try (var con = ds.getConnection(); var ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? java.util.Optional.of(new Order(rs.getLong(1), rs.getString(2)))
                                 : java.util.Optional.empty();
            }
        } catch (java.sql.SQLException e) {
            // Dịch sang exception cùng mức trừu tượng, giữ cause, thêm ngữ cảnh
            throw new DataAccessFailure("Cannot load order id=" + id, e);
        }
    }

    public record Order(long id, String status) {}
    public static class DataAccessFailure extends RuntimeException {
        public DataAccessFailure(String msg, Throwable cause) { super(msg, cause); }
    }
}
```

> 💡 **Góc nhìn Senior:** Clean Code là sách hay nhưng có nhiều quy tắc *cực đoan* đã bị phản biện (ví dụ hàm "không quá 4 dòng" dẫn tới hàng chục hàm nhỏ khiến người đọc phải nhảy khắp nơi — xem tranh luận "A Philosophy of Software Design" của John Ousterhout về *deep modules*). Senior không áp dụng máy móc; tiêu chí cuối cùng là: **người khác (hoặc bạn 6 tháng sau) có hiểu và thay đổi an toàn được không?** Và các quy tắc này nên được *tự động hóa* (formatter, Checkstyle/PMD/SpotBugs/Sonar, Error Prone) để review tập trung vào thiết kế thay vì dấu cách.

> ⚠️ **Lỗi thường gặp:**
> - Bắt `Exception`/`Throwable` rộng, làm mất `InterruptedException` (phải khôi phục cờ: `Thread.currentThread().interrupt()`).
> - Ném exception mất cause: `throw new ServiceException(e.getMessage())` → mất stack trace gốc.
> - `Optional` làm field, tham số hoặc trong collection — `Optional` được thiết kế cho **giá trị trả về**.

### 🛠 Bài tập phần 1

**Bài 1.1 — Đổi tên (Cơ bản)**
- Đề bài: Refactor đoạn sau chỉ bằng cách đổi tên và trích hằng số:
  ```java
  public List<int[]> get(List<int[]> l) {
      List<int[]> r = new ArrayList<>();
      for (int[] x : l) if (x[0] == 4) r.add(x);
      return r;
  }
  ```
  (ngữ cảnh: game dò mìn, `x[0]` là trạng thái ô, `4` nghĩa là "được cắm cờ").
- Tiêu chí đạt: người không biết ngữ cảnh đọc là hiểu; bonus: thay `int[]` bằng class `Cell` có `isFlagged()` (Clean Code ch.2 dùng chính ví dụ này).

**Bài 1.2 — Tách hàm (Trung bình)**
- Đề bài: Viết một hàm 60 dòng `importUsers(Path csv)` (đọc file, parse, validate email, kiểm tra trùng, lưu DB, gửi mail chào mừng, ghi log thống kê). Sau đó refactor theo stepdown rule.
- Tiêu chí đạt: hàm cấp cao nhất ≤ 10 dòng, đọc như mục lục; không còn flag argument; lỗi từng dòng CSV không làm dừng cả batch (thu thập báo cáo lỗi).

**Bài 1.3 — Chính sách exception cho một module (Nâng cao)**
- Đề bài: Thiết kế hệ thống exception cho một service thanh toán: phân cấp exception (domain vs infrastructure), checked hay unchecked, mapping sang HTTP status (400/404/409/422/503), chỗ nào log, có retry hay không.
- Tiêu chí đạt: sơ đồ phân cấp; `@RestControllerAdvice` (hoặc handler tương đương) trả RFC 9457 Problem Details; giải thích vì sao không log ở tầng repository.

<details>
<summary>Gợi ý lời giải</summary>

Bài 1.1:
```java
private static final int FLAGGED = 4;
public List<Cell> getFlaggedCells(List<Cell> gameBoard) {
    List<Cell> flaggedCells = new ArrayList<>();
    for (Cell cell : gameBoard) if (cell.isFlagged()) flaggedCells.add(cell);
    return flaggedCells;
}
// Java 16+: return gameBoard.stream().filter(Cell::isFlagged).toList();
```

Bài 1.3 — gợi ý phân cấp: `PaymentException` (abstract, unchecked) → `DomainException` (`InsufficientFundsException` → 422, `PaymentNotFoundException` → 404, `DuplicatePaymentException` → 409) và `InfrastructureException` (`GatewayTimeoutException` → 503, retryable). Chỉ log ở *biên* (advice / message listener), một lần, kèm correlation id; tầng dưới chỉ ném kèm ngữ cảnh.

</details>

---

<a id="p2"></a>
## 2. Code smells & refactoring

### 2.1 Khái niệm
**Code smell** (Fowler & Beck) là *triệu chứng* bề mặt gợi ý một vấn đề thiết kế sâu hơn — không phải bug, nhưng làm thay đổi tốn kém. **Refactoring** là thay đổi cấu trúc bên trong *mà không đổi hành vi bên ngoài*, thực hiện qua các bước nhỏ, an toàn, có test bảo vệ.

### 2.2 Danh mục smell hay gặp & refactoring tương ứng
| Smell | Biểu hiện | Refactoring |
|---|---|---|
| **Long Method** | Hàm dài, cần comment để chia "đoạn" | Extract Method, Replace Temp with Query, Decompose Conditional |
| **Large Class / God Class** | Class hàng nghìn dòng, chục dependency | Extract Class, Extract Interface, tách theo trách nhiệm |
| **Long Parameter List** | ≥ 4 tham số, tham số luôn đi cùng nhau | Introduce Parameter Object, Preserve Whole Object |
| **Primitive Obsession** | `String email`, `long amount`, `String currency` khắp nơi | Replace Primitive with Object (value object: `Email`, `Money`) |
| **Data Clumps** | Cùng nhóm field/tham số lặp lại (`street, city, zip`) | Extract Class (`Address`) |
| **Switch Statements** lặp lại trên type code | `switch(type)` ở nhiều nơi | Replace Conditional with Polymorphism; sealed + pattern matching nếu tập kiểu đóng |
| **Feature Envy** | Method dùng dữ liệu của class khác nhiều hơn của mình | Move Method |
| **Shotgun Surgery** | Một thay đổi phải sửa nhiều class | Move Method/Field, gom trách nhiệm |
| **Divergent Change** | Một class bị sửa vì nhiều lý do khác nhau | Extract Class (vi phạm SRP) |
| **Duplicated Code** | Copy–paste | Extract Method, Pull Up Method, Template Method |
| **Message Chains** | `a.getB().getC().getD().doX()` | Hide Delegate (Law of Demeter) |
| **Middle Man** | Class chỉ chuyển tiếp lời gọi | Remove Middle Man, Inline Class |
| **Speculative Generality** | Abstraction "để sau này dùng" nhưng chỉ có 1 implementation | Collapse Hierarchy, Inline Class (YAGNI) |
| **Refused Bequest** | Subclass không dùng/ném `UnsupportedOperationException` cho method cha | Replace Inheritance with Delegation (vi phạm LSP) |
| **Mutable Data / Global Data** | Static mutable, setter khắp nơi | Encapsulate Variable, làm immutable |
| **Comments** (dùng như chất khử mùi) | Comment giải thích đoạn code khó hiểu | Extract Method với tên tốt |

### 2.3 Quy trình refactor an toàn
1. Có **test bao phủ hành vi** trước (nếu là legacy chưa có test: viết *characterization test* — chụp lại hành vi hiện tại, kể cả hành vi "sai", theo Michael Feathers, *Working Effectively with Legacy Code*).
2. Bước nhỏ, mỗi bước compile + test xanh, commit thường xuyên.
3. Dùng refactoring tự động của IDE (Rename, Extract Method, Inline, Change Signature) — an toàn hơn sửa tay.
4. **Không trộn refactor với thay đổi hành vi trong cùng commit/PR** — reviewer không thể kiểm tra cả hai cùng lúc.
5. *Boy Scout Rule* (Clean Code ch.1): để code sạch hơn một chút so với lúc bạn đến — nhưng giữ phạm vi trong PR.

Ví dụ **Replace Conditional with Polymorphism** dùng sealed interface (Java 17+):
```java
// TRƯỚC: switch trên type code lặp lại ở nhiều nơi
class Shipping {
    double cost(String type, double weightKg) {
        switch (type) {
            case "STANDARD": return 2.0 * weightKg;
            case "EXPRESS":  return 5.0 + 3.0 * weightKg;
            case "PICKUP":   return 0;
            default: throw new IllegalArgumentException(type);
        }
    }
}

// SAU: mỗi kiểu tự biết tính phí; compiler kiểm tra đủ trường hợp
sealed interface ShippingMethod permits Standard, Express, Pickup {
    double cost(double weightKg);
}
record Standard() implements ShippingMethod { public double cost(double w) { return 2.0 * w; } }
record Express()  implements ShippingMethod { public double cost(double w) { return 5.0 + 3.0 * w; } }
record Pickup()   implements ShippingMethod { public double cost(double w) { return 0; } }

// Hoặc, khi hành vi thuộc về nơi khác (ví dụ tầng hiển thị), dùng pattern matching switch (Java 21):
class ShippingLabel {
    static String label(ShippingMethod m) {
        return switch (m) {               // exhaustive: thêm kiểu mới mà quên xử lý → lỗi compile
            case Standard s -> "3-5 ngày";
            case Express e  -> "24 giờ";
            case Pickup p   -> "Nhận tại cửa hàng";
        };
    }
}
```

> 💡 **Góc nhìn Senior:** Refactoring là hoạt động *liên tục* trong lúc làm feature ("make the change easy, then make the easy change" — Kent Beck), không phải một dự án "refactor 3 tháng" tách rời mà business khó chấp nhận. Khi cần refactor lớn, dùng *Strangler Fig* / *Branch by Abstraction*: đưa abstraction vào, chuyển dần từng caller, xóa code cũ — luôn có thể deploy giữa chừng. Đo bằng metric thực tế: thời gian lead time của thay đổi, số bug quay lại ở module đó (hotspot = churn cao × độ phức tạp cao, theo Adam Tornhill, *Your Code as a Crime Scene*).

> ⚠️ **Lỗi thường gặp:** Refactor không có test; PR "refactor" 3.000 dòng kèm đổi logic; tạo abstraction trước khi có trường hợp thứ hai ("rule of three": lặp lại lần thứ ba mới trừu tượng hóa).

### 🛠 Bài tập phần 2

**Bài 2.1 — Gọi tên smell (Cơ bản)**
- Đề bài: Cho class `ReportService` (tự viết hoặc lấy từ project cũ) dài ≥ 300 dòng. Liệt kê ít nhất 6 smell kèm dòng code và refactoring đề xuất.
- Tiêu chí đạt: mỗi smell gọi đúng tên theo bảng 2.2, đề xuất cụ thể.

**Bài 2.2 — Gilded Rose kata (Trung bình)**
- Đề bài: Làm bài kata "Gilded Rose" (https://github.com/emilybache/GildedRose-Refactoring-Kata, bản Java): viết characterization test trước, refactor, rồi thêm tính năng "Conjured items".
- Tiêu chí đạt: test bao phủ trước khi sửa; lịch sử commit thể hiện các bước nhỏ; không còn `if` lồng 5 tầng; thêm tính năng mới chỉ cần thêm code, không sửa logic cũ.

**Bài 2.3 — Primitive obsession → Value Object (Nâng cao)**
- Đề bài: Trong một codebase dùng `BigDecimal amount` + `String currency` rời rạc, giới thiệu `Money` (immutable record) và chuyển dần từng caller theo branch-by-abstraction, đảm bảo mỗi commit đều build + test xanh.
- Tiêu chí đạt: `Money` chặn cộng khác loại tiền, chuẩn hóa scale theo currency, `equals` đúng (`BigDecimal.equals` phân biệt scale — `2.0` ≠ `2.00`!); có test.

<details>
<summary>Gợi ý lời giải</summary>

Bài 2.3:
```java
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

public record Money(BigDecimal amount, Currency currency) {
    public Money {
        Objects.requireNonNull(amount); Objects.requireNonNull(currency);
        amount = amount.setScale(currency.getDefaultFractionDigits(), RoundingMode.HALF_EVEN); // chuẩn hóa scale
    }
    public static Money of(String amount, String currencyCode) {
        return new Money(new BigDecimal(amount), Currency.getInstance(currencyCode));
    }
    public Money plus(Money other) {
        requireSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }
    public boolean isGreaterThan(Money other) {
        requireSameCurrency(other);
        return amount.compareTo(other.amount) > 0;
    }
    private void requireSameCurrency(Money other) {
        if (!currency.equals(other.currency))
            throw new IllegalArgumentException("Currency mismatch: " + currency + " vs " + other.currency);
    }
}
```
Nhờ chuẩn hóa scale trong compact constructor, `Money.of("2.0","USD").equals(Money.of("2.00","USD"))` là `true`. Lưu ý VND có 0 chữ số thập phân.

</details>

---

<a id="p3"></a>
## 3. SOLID

SOLID là 5 nguyên tắc thiết kế hướng đối tượng do Robert C. Martin tổng hợp. Mục tiêu chung: **giảm chi phí thay đổi** bằng cách quản lý dependency.

### 3.1 S — Single Responsibility Principle
> *"A module should be responsible to one, and only one, actor."* (Clean Architecture) — hay phát biểu cũ: *"một class chỉ nên có một lý do để thay đổi"*.

"Trách nhiệm" = **một nhóm người/stakeholder yêu cầu thay đổi**, không phải "làm một việc".

```java
// VI PHẠM: 3 actor (kế toán → tính lương, HR → báo cáo giờ, DBA → lưu trữ) cùng sửa một class
class Employee {
    java.math.BigDecimal calculatePay() { /* quy tắc của phòng kế toán */ return null; }
    String reportHours()               { /* định dạng của HR */ return null; }
    void save()                        { /* SQL */ }
}

// TUÂN THỦ: tách theo actor; dữ liệu dùng chung là một record đơn giản
record EmployeeData(long id, String name, java.math.BigDecimal hourlyRate, int hoursWorked) {}
class PayCalculator   { java.math.BigDecimal calculatePay(EmployeeData e) { return e.hourlyRate().multiply(java.math.BigDecimal.valueOf(e.hoursWorked())); } }
class HourReporter    { String reportHours(EmployeeData e) { return e.name() + ": " + e.hoursWorked() + "h"; } }
class EmployeeRepository { void save(EmployeeData e) { /* JDBC/JPA */ } }
```
Rủi ro thật của vi phạm: kế toán đổi cách tính "giờ làm thường" trong một hàm private dùng chung → báo cáo của HR sai mà không ai biết.

### 3.2 O — Open/Closed Principle
> *"Software entities should be open for extension, but closed for modification."* (Bertrand Meyer, 1988)

Thêm hành vi mới bằng **thêm code** (class mới, bean mới), không sửa code đã ổn định.

```java
// VI PHẠM: mỗi phương thức thanh toán mới phải sửa class này
class PaymentProcessor {
    void pay(String method, long amount) {
        if (method.equals("CARD")) { /* ... */ }
        else if (method.equals("MOMO")) { /* ... */ }
        else if (method.equals("VNPAY")) { /* ... */ }    // lại sửa, lại test lại toàn bộ
    }
}

// TUÂN THỦ: strategy + registry; thêm ZaloPay = thêm một class
interface PaymentGateway {
    String method();
    void pay(long amount);
}
final class CardGateway implements PaymentGateway { public String method() { return "CARD"; } public void pay(long a) { /*...*/ } }
final class MomoGateway implements PaymentGateway { public String method() { return "MOMO"; } public void pay(long a) { /*...*/ } }

final class PaymentService {
    private final java.util.Map<String, PaymentGateway> gateways;
    PaymentService(java.util.List<PaymentGateway> all) {   // Spring tự inject mọi bean PaymentGateway
        this.gateways = all.stream().collect(java.util.stream.Collectors.toUnmodifiableMap(PaymentGateway::method, g -> g));
    }
    void pay(String method, long amount) {
        var gw = gateways.get(method);
        if (gw == null) throw new IllegalArgumentException("Unsupported method " + method);
        gw.pay(amount);
    }
}
```
OCP không có nghĩa "không bao giờ sửa": bạn chọn **trục thay đổi** dự đoán được (thêm phương thức thanh toán) để mở; đừng cố mở mọi trục (vi phạm YAGNI).

### 3.3 L — Liskov Substitution Principle
> *Nếu S là subtype của T thì object kiểu T có thể được thay bằng object kiểu S mà không làm hỏng tính đúng đắn của chương trình.* (Barbara Liskov, 1987)

Subtype phải tôn trọng **hợp đồng** của supertype: không *tăng* precondition, không *giảm* postcondition, giữ invariant, không ném exception mới bất ngờ.

```java
// VI PHẠM kinh điển: Square extends Rectangle
class Rectangle {
    protected int width, height;
    void setWidth(int w)  { width = w; }
    void setHeight(int h) { height = h; }
    int area() { return width * height; }
}
class Square extends Rectangle {
    @Override void setWidth(int w)  { width = w; height = w; }   // phá postcondition của setWidth
    @Override void setHeight(int h) { width = h; height = h; }
}
class Client {
    static void resize(Rectangle r) {
        r.setWidth(5); r.setHeight(4);
        assert r.area() == 20 : "LSP bị vi phạm, area=" + r.area();   // Square → 16
    }
}

// TUÂN THỦ: không ép quan hệ "is-a" theo toán học; dùng abstraction chung, immutable
sealed interface Shape permits Rect, Sq { int area(); }
record Rect(int width, int height) implements Shape { public int area() { return width * height; } }
record Sq(int side) implements Shape { public int area() { return side * side; } }
```
Ví dụ LSP trong JDK: `Collections.unmodifiableList(...)` trả về `List` nhưng `add()` ném `UnsupportedOperationException` — một sự "thỏa hiệp" đã được ghi trong Javadoc ("optional operation"), nhưng vẫn là nguồn bug phổ biến (ví dụ `List.of(...)` rồi gọi `add`). Smell liên quan: *Refused Bequest*.

### 3.4 I — Interface Segregation Principle
> *Client không nên bị buộc phụ thuộc vào method nó không dùng.*

```java
// VI PHẠM: "fat interface"
interface Worker { void work(); void eat(); void attendMeeting(); }
class Robot implements Worker {
    public void work() { /*...*/ }
    public void eat() { throw new UnsupportedOperationException(); }        // Refused bequest
    public void attendMeeting() { throw new UnsupportedOperationException(); }
}

// TUÂN THỦ: interface nhỏ theo vai trò (role interface)
interface Workable { void work(); }
interface Feedable { void eat(); }
class Human implements Workable, Feedable { public void work() {} public void eat() {} }
class Robot2 implements Workable { public void work() {} }
```
Trong thực tế: tách `OrderReader` (query) và `OrderWriter` (command) để service chỉ đọc không thấy method ghi; Spring Data cho phép định nghĩa repository chỉ với các method cần (`Repository<T, ID>` thay vì `JpaRepository` đầy đủ).

### 3.5 D — Dependency Inversion Principle
> *Module cấp cao không nên phụ thuộc module cấp thấp; cả hai phụ thuộc abstraction. Abstraction không phụ thuộc chi tiết; chi tiết phụ thuộc abstraction.*

Điểm mấu chốt: **interface thuộc về phía sử dụng** (module cấp cao định nghĩa cái nó cần), không thuộc về phía hiện thực.

```java
// VI PHẠM: logic nghiệp vụ phụ thuộc trực tiếp hạ tầng
class OrderServiceBad {
    private final MySqlOrderDao dao = new MySqlOrderDao();          // new trực tiếp, khó test
    private final SmtpMailer mailer = new SmtpMailer("smtp.acme.vn");
    void place(String orderId) { dao.insert(orderId); mailer.send("Đã đặt " + orderId); }
}
class MySqlOrderDao { void insert(String id) {} }
class SmtpMailer { SmtpMailer(String host) {} void send(String msg) {} }

// TUÂN THỦ: domain định nghĩa port; hạ tầng hiện thực; wiring ở composition root (Spring)
interface OrderStore { void save(String orderId); }          // thuộc package domain/application
interface Notifier   { void notifyPlaced(String orderId); }

final class OrderService {
    private final OrderStore store; private final Notifier notifier;
    OrderService(OrderStore store, Notifier notifier) { this.store = store; this.notifier = notifier; } // constructor injection
    void place(String orderId) { store.save(orderId); notifier.notifyPlaced(orderId); }
}
final class JdbcOrderStore implements OrderStore { public void save(String id) { /* JDBC */ } }      // package infrastructure
final class EmailNotifier  implements Notifier   { public void notifyPlaced(String id) { /* SMTP */ } }
```
Phân biệt ba khái niệm hay bị trộn lẫn:
- **DIP** — nguyên tắc về *hướng* phụ thuộc giữa các module.
- **IoC (Inversion of Control)** — framework gọi code của bạn thay vì bạn gọi framework ("Hollywood principle").
- **DI (Dependency Injection)** — kỹ thuật cung cấp dependency từ bên ngoài (constructor/setter/field injection). Spring IoC container là một DI container.

> 💡 **Góc nhìn Senior:**
> - SOLID là *công cụ quản lý dependency*, không phải mục tiêu tự thân. Áp dụng quá mức tạo ra "interface cho mọi class", 7 tầng gián tiếp cho CRUD đơn giản. Tiêu chí: có **trục thay đổi thật** hoặc **nhu cầu test thật** không?
> - Constructor injection > field injection: dependency tường minh, field `final` (immutable, thread-safe khi publish), test không cần Spring, constructor dài là *tín hiệu SRP bị vi phạm* rất trực quan.
> - Trong phỏng vấn, đừng chỉ đọc định nghĩa — hãy kể một vi phạm bạn đã gặp, hậu quả của nó, và cách bạn sửa.

> ⚠️ **Lỗi thường gặp:** Hiểu SRP là "class chỉ có một method"; hiểu DIP là "mọi class phải có interface `XxxService` + `XxxServiceImpl`" (nếu chỉ có một implementation và không có nhu cầu thay thế/test double đặc biệt, interface đó thường là nhiễu — Mockito mock được class).

### 🛠 Bài tập phần 3

**Bài 3.1 — Nhận diện vi phạm (Cơ bản)**
- Đề bài: Viết 5 đoạn code ngắn, mỗi đoạn vi phạm một nguyên tắc SOLID, trộn thứ tự. Đổi với bạn học để nhận diện và sửa.
- Tiêu chí đạt: mỗi đoạn có giải thích "hậu quả cụ thể khi requirement thay đổi" chứ không chỉ "vi phạm nguyên tắc X".

**Bài 3.2 — Notification service theo OCP + DIP (Trung bình)**
- Đề bài: Thiết kế `NotificationService` gửi thông báo qua Email, SMS, Push; người dùng có tùy chọn kênh; thêm kênh Zalo không sửa code cũ. Viết unit test không dùng Spring, dùng fake implementation.
- Tiêu chí đạt: thêm kênh mới = thêm 1 class + cấu hình; test chạy < 1 giây; không có `instanceof`/`switch` theo kênh trong service.

**Bài 3.3 — LSP trong collection API (Nâng cao)**
- Đề bài: Viết `ReadOnlyList<E>` *không* kế thừa `List` và so sánh với `Collections.unmodifiableList`. Phân tích ưu/nhược của thiết kế "optional operation" trong Java Collections Framework so với tách interface đọc/ghi (như Kotlin `List`/`MutableList`).
- Tiêu chí đạt: bài viết 1 trang có ví dụ bug thực tế do `UnsupportedOperationException`; đề cập Java 21 `SequencedCollection` và lý do JDK chọn tương thích ngược.

<details>
<summary>Gợi ý lời giải</summary>

Bài 3.2 — khung:
```java
interface NotificationChannel { ChannelType type(); void send(UserId user, Message msg); }
enum ChannelType { EMAIL, SMS, PUSH, ZALO }

final class NotificationService {
    private final Map<ChannelType, NotificationChannel> channels;
    private final PreferenceRepository prefs;
    NotificationService(List<NotificationChannel> all, PreferenceRepository prefs) {
        this.channels = new EnumMap<>(ChannelType.class);
        all.forEach(c -> channels.put(c.type(), c));
        this.prefs = prefs;
    }
    void notify(UserId user, Message msg) {
        for (ChannelType t : prefs.channelsOf(user)) {
            NotificationChannel c = channels.get(t);
            if (c != null) c.send(user, msg);   // cân nhắc: lỗi một kênh không chặn kênh khác
        }
    }
}
```
Test: `FakeChannel` ghi lại message vào list, `InMemoryPreferenceRepository`.

Bài 3.3: thiết kế optional operation giúp JCF nhỏ gọn (ít interface) nhưng chuyển lỗi từ compile-time sang runtime. Kotlin tách read-only/mutable ở mức type (dù runtime vẫn là `java.util.List`).

</details>

---

<a id="p4"></a>
## 4. Các nguyên tắc khác: DRY, KISS, YAGNI, composition, Law of Demeter, cohesion/coupling

### 4.1 DRY, KISS, YAGNI
- **DRY — Don't Repeat Yourself** (*The Pragmatic Programmer*): *"Every piece of knowledge must have a single, unambiguous, authoritative representation within a system."* DRY nói về **tri thức**, không phải về *văn bản code giống nhau*. Hai đoạn code giống hệt nhau nhưng phục vụ hai quy tắc nghiệp vụ độc lập (sẽ thay đổi khác nhau) **không nên** gộp — gộp lại tạo coupling sai ("wrong abstraction is worse than duplication" — Sandi Metz).
- **KISS — Keep It Simple**: chọn giải pháp đơn giản nhất đáp ứng yêu cầu. Đơn giản ≠ dễ (simple vs easy — Rich Hickey): đơn giản là ít đan xen khái niệm.
- **YAGNI — You Aren't Gonna Need It** (Extreme Programming): không xây thứ chưa cần. Chi phí của tính năng "để sẵn": xây, test, bảo trì, và độ phức tạp cản trở thay đổi thật sau này.

### 4.2 Composition over inheritance (Effective Java Item 18, 19)
Kế thừa (implementation inheritance) phá vỡ encapsulation: subclass phụ thuộc vào *chi tiết hiện thực* của superclass (**fragile base class problem**).

```java
import java.util.*;

// VI PHẠM: đếm số phần tử đã thêm — kết quả SAI vì HashSet.addAll gọi add() bên trong
class InstrumentedHashSet<E> extends HashSet<E> {
    private int addCount = 0;
    @Override public boolean add(E e) { addCount++; return super.add(e); }
    @Override public boolean addAll(Collection<? extends E> c) { addCount += c.size(); return super.addAll(c); }
    int getAddCount() { return addCount; }
}
// new InstrumentedHashSet<String>().addAll(List.of("a","b","c")) → addCount = 6, không phải 3

// TUÂN THỦ: composition + forwarding (wrapper / Decorator)
class InstrumentedSet<E> implements Set<E> {
    private final Set<E> delegate;
    private int addCount = 0;
    InstrumentedSet(Set<E> delegate) { this.delegate = delegate; }
    @Override public boolean add(E e) { addCount++; return delegate.add(e); }
    @Override public boolean addAll(Collection<? extends E> c) { addCount += c.size(); return delegate.addAll(c); }
    int getAddCount() { return addCount; }
    // forwarding methods còn lại
    @Override public int size() { return delegate.size(); }
    @Override public boolean isEmpty() { return delegate.isEmpty(); }
    @Override public boolean contains(Object o) { return delegate.contains(o); }
    @Override public Iterator<E> iterator() { return delegate.iterator(); }
    @Override public Object[] toArray() { return delegate.toArray(); }
    @Override public <T> T[] toArray(T[] a) { return delegate.toArray(a); }
    @Override public boolean remove(Object o) { return delegate.remove(o); }
    @Override public boolean containsAll(Collection<?> c) { return delegate.containsAll(c); }
    @Override public boolean retainAll(Collection<?> c) { return delegate.retainAll(c); }
    @Override public boolean removeAll(Collection<?> c) { return delegate.removeAll(c); }
    @Override public void clear() { delegate.clear(); }
    @Override public boolean equals(Object o) { return delegate.equals(o); }
    @Override public int hashCode() { return delegate.hashCode(); }
    @Override public String toString() { return delegate.toString(); }
}
```
Khi nào kế thừa vẫn hợp lý: quan hệ "is-a" thật sự *và* superclass được **thiết kế và tài liệu hóa cho kế thừa** (Item 19) — ví dụ `AbstractList`, `HttpServlet`. Nếu không, hãy `final` class (hoặc `sealed` để kiểm soát danh sách subclass).

### 4.3 Law of Demeter (Clean Code ch.6)
"Chỉ nói chuyện với bạn thân": method `m` của object `O` chỉ nên gọi method của: `O`, tham số của `m`, object do `m` tạo ra, field của `O`. Tránh **train wreck**:

```java
// VI PHẠM: biết quá nhiều về cấu trúc bên trong
String city = order.getCustomer().getAddress().getCity();
if (customer.getWallet().getBalance().compareTo(price) >= 0)
    customer.getWallet().setBalance(customer.getWallet().getBalance().subtract(price));

// TỐT HƠN: "Tell, Don't Ask" — bảo object làm việc thay vì lấy dữ liệu ra tự xử lý
String city = order.shippingCity();
customer.pay(price);         // Customer tự kiểm tra và trừ ví, giữ invariant bên trong
```
Lưu ý: Law of Demeter áp dụng cho **object có hành vi**; với **data structure** (DTO, record) hoặc fluent API/stream (`list.stream().filter().map()`), chuỗi gọi là bình thường (Clean Code ch.6 phân biệt "objects vs data structures").

### 4.4 Cohesion & Coupling
- **Cohesion (gắn kết)**: mức độ các phần tử trong một module cùng phục vụ một mục đích. Mục tiêu: **cao**. Dấu hiệu thấp: class `Utils`/`Helper`/`Manager` chứa đủ thứ; nhóm method chỉ dùng nhóm field riêng (tách được thành class khác — chỉ số LCOM).
- **Coupling (phụ thuộc)**: mức độ module biết về nhau. Mục tiêu: **thấp**. Các mức (từ tệ tới tốt): content coupling (sửa nội bộ của nhau) → common/global coupling (biến toàn cục) → control coupling (truyền flag điều khiển) → stamp coupling (truyền cả object lớn khi chỉ cần một field) → data coupling (chỉ truyền dữ liệu cần thiết).
- Ở mức kiến trúc/microservice (Newman, *Building Microservices*): **high cohesion** = những gì thay đổi cùng nhau nằm cùng nhau; **loose coupling** = thay đổi một service không buộc deploy service khác. Các dạng coupling: domain, pass-through, common (DB dùng chung), content (đọc thẳng DB của service khác — tệ nhất).
- Công cụ đo/ép: ArchUnit (test kiến trúc), JDepend, module system (JPMS) hoặc Maven/Gradle multi-module để chặn dependency ngược.

> 💡 **Góc nhìn Senior:** Mọi nguyên tắc ở đây đều có điểm cân bằng. Bạn nên nói được *khi nào vi phạm có chủ đích*: chấp nhận duplication giữa hai bounded context để tránh coupling; chấp nhận kế thừa trong framework nội bộ có tài liệu rõ; chấp nhận "message chain" trên DTO. Câu hỏi đúng không phải "có vi phạm DRY không?" mà là "khi requirement X thay đổi, phải sửa bao nhiêu chỗ và rủi ro thế nào?".

> ⚠️ **Lỗi thường gặp:** Tạo `CommonUtils` trong một shared library dùng cho 20 microservice "cho DRY" → mọi thay đổi của lib buộc nâng version đồng loạt (distributed monolith).

### 🛠 Bài tập phần 4

**Bài 4.1 — Fragile base class (Cơ bản)**
- Đề bài: Chạy `InstrumentedHashSet` và chứng minh `addCount` sai; sửa bằng composition; viết test.
- Tiêu chí đạt: test đỏ với phiên bản kế thừa, xanh với phiên bản composition; giải thích vì sao bản kế thừa có thể "đúng" ở JDK này và sai ở JDK khác.

**Bài 4.2 — Tell, Don't Ask (Trung bình)**
- Đề bài: Refactor service sau sao cho `Account` giữ invariant "số dư không âm, không vượt hạn mức rút trong ngày":
  ```java
  if (acc.getBalance().compareTo(amt) >= 0 && acc.getWithdrawnToday().add(amt).compareTo(acc.getDailyLimit()) <= 0) {
      acc.setBalance(acc.getBalance().subtract(amt));
      acc.setWithdrawnToday(acc.getWithdrawnToday().add(amt));
  }
  ```
- Tiêu chí đạt: không còn setter public; `acc.withdraw(amount, clock)` ném exception domain rõ nghĩa; test các trường hợp biên.

**Bài 4.3 — ArchUnit (Nâng cao)**
- Đề bài: Viết test ArchUnit cho một project 3 tầng: `controller` không được truy cập `repository`; `domain` không phụ thuộc `org.springframework..`; không có vòng phụ thuộc giữa các package `..feature..`.
- Tiêu chí đạt: test fail khi cố tình vi phạm; tích hợp vào CI.

<details>
<summary>Gợi ý lời giải</summary>

Bài 4.1: `HashSet.addAll` (kế thừa từ `AbstractCollection`) gọi `add` cho từng phần tử — đây là *chi tiết hiện thực* không có trong hợp đồng; nếu JDK đổi cách hiện thực, kết quả của subclass thay đổi theo.

Bài 4.3:
```java
@AnalyzeClasses(packages = "com.acme.shop")
class ArchitectureTest {
    @ArchTest static final ArchRule layers = layeredArchitecture().consideringAllDependencies()
        .layer("Controller").definedBy("..controller..")
        .layer("Service").definedBy("..service..")
        .layer("Repository").definedBy("..repository..")
        .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
        .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller")
        .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service");

    @ArchTest static final ArchRule domainIsPure = noClasses().that().resideInAPackage("..domain..")
        .should().dependOnClassesThat().resideInAPackage("org.springframework..");

    @ArchTest static final ArchRule noCycles = slices().matching("com.acme.shop.(*)..").should().beFreeOfCycles();
}
```

</details>

---

<a id="p5"></a>
## 5. Creational patterns: Singleton, Factory Method, Abstract Factory, Builder, Prototype

> Cách học pattern hiệu quả cho phỏng vấn Senior: với mỗi pattern nắm **(1) vấn đề nó giải quyết, (2) cấu trúc tối thiểu, (3) ví dụ trong JDK/Spring, (4) khi nào KHÔNG dùng, (5) cách viết hiện đại với Java 17/21** (lambda, record, sealed, enum). Người phỏng vấn ít quan tâm sơ đồ UML hơn là bạn biết trade-off.

### 5.1 Singleton
**Vấn đề:** bảo đảm chỉ có một instance và một điểm truy cập toàn cục (ví dụ registry, cấu hình đọc một lần).

**Các biến thể và độ an toàn:**

```java
// (1) Eager — đơn giản, thread-safe nhờ class initialization (JVMS §5.5); instance tạo khi class được init
public final class EagerConfig {
    private static final EagerConfig INSTANCE = new EagerConfig();
    private EagerConfig() {}
    public static EagerConfig getInstance() { return INSTANCE; }
}

// (2) Lazy KHÔNG thread-safe — hai thread có thể cùng thấy null và tạo 2 instance
final class NaiveLazy {
    private static NaiveLazy instance;
    private NaiveLazy() {}
    static NaiveLazy getInstance() {
        if (instance == null) instance = new NaiveLazy();   // race condition
        return instance;
    }
}

// (3) synchronized method — đúng nhưng mọi lần gọi đều phải lấy lock
final class SyncLazy {
    private static SyncLazy instance;
    private SyncLazy() {}
    static synchronized SyncLazy getInstance() {
        if (instance == null) instance = new SyncLazy();
        return instance;
    }
}

// (4) Double-checked locking — BẮT BUỘC volatile (đúng từ Java 5, JSR-133)
final class DclLazy {
    private static volatile DclLazy instance;
    private final java.util.Map<String, String> settings;
    private DclLazy() { settings = java.util.Map.of("env", "prod"); }
    static DclLazy getInstance() {
        DclLazy local = instance;                 // đọc volatile một lần (tối ưu nhỏ)
        if (local == null) {
            synchronized (DclLazy.class) {
                local = instance;
                if (local == null) instance = local = new DclLazy();
            }
        }
        return local;
    }
}

// (5) Initialization-on-demand holder — lazy, thread-safe, không lock: được khuyến nghị cho lazy singleton
final class HolderLazy {
    private HolderLazy() {}
    private static final class Holder { static final HolderLazy INSTANCE = new HolderLazy(); }
    static HolderLazy getInstance() { return Holder.INSTANCE; }  // Holder chỉ init khi lần đầu được truy cập
}

// (6) Enum — Effective Java Item 3: "a single-element enum type is often the best way to implement a singleton"
enum IdGenerator {
    INSTANCE;
    private final java.util.concurrent.atomic.AtomicLong seq = new java.util.concurrent.atomic.AtomicLong();
    long next() { return seq.incrementAndGet(); }
}
```

**Vì sao DCL cần `volatile`?** `instance = new DclLazy()` gồm 3 bước: cấp phát bộ nhớ, chạy constructor, gán reference. Không có `volatile`, compiler/CPU được phép sắp xếp lại để *gán reference trước khi constructor chạy xong* → thread khác thấy `instance != null` ở lần kiểm tra thứ nhất (ngoài `synchronized`) và dùng object **chưa khởi tạo xong**. `volatile` tạo quan hệ *happens-before* giữa ghi và đọc (Module 03).

**Phá vỡ singleton và cách phòng:**
| Tấn công | Phòng |
|---|---|
| Reflection: `ctor.setAccessible(true); ctor.newInstance()` | Ném exception trong constructor nếu instance đã tồn tại; **enum** miễn nhiễm (JVM cấm tạo enum bằng reflection) |
| Serialization: deserialize tạo object mới | Thêm `private Object readResolve() { return INSTANCE; }`; **enum** tự đảm bảo |
| `clone()` | Không implement `Cloneable` |
| Nhiều class loader | Mỗi loader có một "singleton" riêng (phần 2, Module 05) |

**Singleton của GoF vs singleton scope của Spring:** Spring bean `singleton` là *một instance cho mỗi bean definition trong một ApplicationContext* — không phải toàn cục JVM, không chặn `new`, và quan trọng nhất: **được inject** chứ không truy cập tĩnh qua `getInstance()`. Đây là cách nên dùng trong ứng dụng hiện đại.

> 💡 **Góc nhìn Senior:** Singleton dạng `getInstance()` tĩnh thường bị coi là **anti-pattern** vì: global mutable state, phụ thuộc ẩn (không thấy trong constructor), khó thay bằng test double, khó chạy test song song. Hãy dùng DI container quản lý vòng đời. Nếu buộc phải dùng singleton thủ công (thư viện không có DI): enum hoặc holder idiom; và giữ singleton **stateless hoặc immutable**.

### 5.2 Factory Method
**Vấn đề:** tách việc *tạo object* khỏi code *sử dụng object*, để subclass/implementation quyết định lớp cụ thể.

Có hai thứ hay bị gọi chung là "factory":
1. **GoF Factory Method**: một method (thường abstract) trong class "creator", subclass override để quyết định tạo sản phẩm nào. Ví dụ JDK kinh điển: `Collection.iterator()` — mỗi collection trả về iterator riêng của nó.
2. **Static factory method** (Effective Java Item 1, *không phải* GoF): `List.of()`, `Integer.valueOf()` (có cache −128..127), `Optional.of()`, `Path.of()`, `EnumSet.of()` (trả `RegularEnumSet` hoặc `JumboEnumSet` tùy số phần tử). Ưu điểm so với constructor: có tên, không bắt buộc tạo object mới (cache/flyweight), có thể trả subtype ẩn, giảm lặp generic.

```java
// GoF Factory Method
abstract class ReportExporter {
    // template sử dụng sản phẩm
    public final byte[] export(java.util.List<String[]> rows) {
        Formatter f = createFormatter();          // factory method
        StringBuilder sb = new StringBuilder(f.header());
        rows.forEach(r -> sb.append(f.row(r)));
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
    protected abstract Formatter createFormatter();

    interface Formatter { String header(); String row(String[] cols); }
}
class CsvExporter extends ReportExporter {
    @Override protected Formatter createFormatter() {
        return new Formatter() {
            public String header() { return ""; }
            public String row(String[] c) { return String.join(",", c) + "\n"; }
        };
    }
}

// Cách hiện đại hay gặp hơn: factory dạng registry + Supplier (không cần subclass)
final class ParserFactory {
    private static final java.util.Map<String, java.util.function.Supplier<Parser>> REGISTRY = java.util.Map.of(
            "json", JsonParser::new,
            "xml",  XmlParser::new);
    static Parser forType(String type) {
        var s = REGISTRY.get(type);
        if (s == null) throw new IllegalArgumentException("No parser for " + type);
        return s.get();
    }
    interface Parser { Object parse(String s); }
    static final class JsonParser implements Parser { public Object parse(String s) { return s; } }
    static final class XmlParser  implements Parser { public Object parse(String s) { return s; } }
}
```
Trong Spring: `BeanFactory` (bản thân container là factory), `FactoryBean<T>` (bean tạo bean — ví dụ `SqlSessionFactoryBean`), `@Bean` method là một dạng factory method.

### 5.3 Abstract Factory
**Vấn đề:** tạo **họ (family)** các object liên quan đảm bảo nhất quán với nhau, mà không chỉ định lớp cụ thể.

```java
// Họ sản phẩm: storage + queue cho từng cloud — không được trộn S3 với Pub/Sub
interface BlobStorage { void put(String key, byte[] data); }
interface MessageQueue { void publish(String topic, String msg); }

interface CloudFactory {                      // abstract factory
    BlobStorage storage();
    MessageQueue queue();
}
final class AwsFactory implements CloudFactory {
    public BlobStorage storage() { return (k, d) -> System.out.println("S3 put " + k); }
    public MessageQueue queue()  { return (t, m) -> System.out.println("SQS publish " + t); }
}
final class GcpFactory implements CloudFactory {
    public BlobStorage storage() { return (k, d) -> System.out.println("GCS put " + k); }
    public MessageQueue queue()  { return (t, m) -> System.out.println("Pub/Sub publish " + t); }
}

final class ArchiveService {                  // client chỉ biết abstraction
    private final BlobStorage storage; private final MessageQueue queue;
    ArchiveService(CloudFactory f) { this.storage = f.storage(); this.queue = f.queue(); }
    void archive(String key, byte[] data) { storage.put(key, data); queue.publish("archived", key); }
}
```
JDK: `javax.xml.parsers.DocumentBuilderFactory`, `TransformerFactory`; JDBC: `Connection` là factory cho họ `Statement`, `PreparedStatement`, `CallableStatement` của cùng một driver. Nhược điểm: thêm *loại sản phẩm mới* vào họ buộc sửa mọi factory.

### 5.4 Builder
**Vấn đề:** tạo object có nhiều tham số (nhiều tùy chọn), tránh *telescoping constructor* (`new Pizza(size, true, false, true, null, 2)`) và tránh JavaBeans setter (object ở trạng thái dở dang, không immutable).

```java
import java.time.Duration;
import java.util.*;

public final class HttpClientConfig {                  // immutable
    private final String baseUrl;                      // bắt buộc
    private final Duration connectTimeout;             // tùy chọn
    private final Duration readTimeout;
    private final int maxRetries;
    private final Map<String, String> defaultHeaders;

    private HttpClientConfig(Builder b) {
        this.baseUrl = b.baseUrl;
        this.connectTimeout = b.connectTimeout;
        this.readTimeout = b.readTimeout;
        this.maxRetries = b.maxRetries;
        this.defaultHeaders = Map.copyOf(b.defaultHeaders);   // defensive copy
    }

    public static Builder builder(String baseUrl) { return new Builder(baseUrl); } // tham số bắt buộc ở đây

    public static final class Builder {
        private final String baseUrl;
        private Duration connectTimeout = Duration.ofSeconds(2);
        private Duration readTimeout = Duration.ofSeconds(5);
        private int maxRetries = 0;
        private final Map<String, String> defaultHeaders = new LinkedHashMap<>();

        private Builder(String baseUrl) { this.baseUrl = Objects.requireNonNull(baseUrl); }
        public Builder connectTimeout(Duration d) { this.connectTimeout = d; return this; }
        public Builder readTimeout(Duration d)    { this.readTimeout = d; return this; }
        public Builder maxRetries(int n)          { this.maxRetries = n; return this; }
        public Builder header(String k, String v) { this.defaultHeaders.put(k, v); return this; }

        public HttpClientConfig build() {             // validate invariant TẠI ĐÂY, một lần
            if (maxRetries < 0 || maxRetries > 10) throw new IllegalStateException("maxRetries out of range");
            if (readTimeout.compareTo(connectTimeout) < 0) throw new IllegalStateException("readTimeout < connectTimeout");
            return new HttpClientConfig(this);
        }
    }

    @Override public String toString() {
        return "HttpClientConfig[" + baseUrl + ", connect=" + connectTimeout + ", read=" + readTimeout
                + ", retries=" + maxRetries + ", headers=" + defaultHeaders + "]";
    }

    public static void main(String[] args) {
        var cfg = HttpClientConfig.builder("https://api.acme.vn")
                .readTimeout(Duration.ofSeconds(10)).maxRetries(3).header("Accept", "application/json").build();
        System.out.println(cfg);
    }
}
```
JDK/Spring: `StringBuilder` (builder theo nghĩa rộng), `java.net.http.HttpRequest.newBuilder()`, `HttpClient.newBuilder()`, `Stream.builder()`, `Locale.Builder`, Spring `UriComponentsBuilder`, `WebClient.builder()`, `MockMvcRequestBuilders`; Lombok `@Builder`.

Biến thể: **step builder** (mỗi bước trả interface khác nhau để compiler ép thứ tự/thuộc tính bắt buộc); với **record** nhỏ (≤ 3–4 field bắt buộc) thì constructor + compact constructor validate là đủ, không cần builder.

### 5.5 Prototype
**Vấn đề:** tạo object mới bằng cách **sao chép** một object mẫu (khi khởi tạo tốn kém hoặc cấu hình phức tạp).

Trong Java, `Object.clone()` + `Cloneable` là thiết kế có nhiều vấn đề (Effective Java Item 13): `Cloneable` không có method `clone` (marker interface "kỳ lạ"), mặc định là **shallow copy**, không gọi constructor, xung đột với field `final`, ném checked `CloneNotSupportedException`. Khuyến nghị: **copy constructor** hoặc **copy factory**.

```java
import java.util.*;

final class Document {
    private final String title;
    private final List<String> paragraphs;          // mutable bên trong → cần deep copy
    private final Map<String, String> metadata;

    Document(String title, List<String> paragraphs, Map<String, String> metadata) {
        this.title = title; this.paragraphs = paragraphs; this.metadata = metadata;
    }

    /** Copy constructor — deep copy các collection mutable */
    Document(Document prototype) {
        this(prototype.title, new ArrayList<>(prototype.paragraphs), new HashMap<>(prototype.metadata));
    }

    Document withTitle(String newTitle) {           // "wither" trên bản sao
        return new Document(newTitle, new ArrayList<>(paragraphs), new HashMap<>(metadata));
    }
    void addParagraph(String p) { paragraphs.add(p); }
    @Override public String toString() { return title + paragraphs + metadata; }

    public static void main(String[] args) {
        Document template = new Document("Hợp đồng mẫu", new ArrayList<>(List.of("Điều 1", "Điều 2")),
                new HashMap<>(Map.of("lang", "vi")));
        Document c1 = new Document(template).withTitle("Hợp đồng KH A");
        c1.addParagraph("Điều 3 riêng cho A");
        System.out.println(template);   // không bị ảnh hưởng nhờ deep copy
        System.out.println(c1);
    }
}
```
Lưu ý: **Spring `prototype` scope** là khái niệm khác — container tạo bean *mới* mỗi lần yêu cầu (không sao chép), và container *không quản lý* destroy callback của prototype bean. Inject prototype vào singleton chỉ xảy ra một lần → cần `ObjectProvider<T>`/`@Lookup` để lấy instance mới mỗi lần.

> 💡 **Góc nhìn Senior:** Ưu tiên **immutable object** (record, `List.copyOf`) — khi object bất biến, nhu cầu prototype/clone gần như biến mất (chia sẻ an toàn, "sao chép" bằng wither). Builder phù hợp cho object cấu hình nhiều tùy chọn và API public; với DTO nội bộ, record là đủ.

> ⚠️ **Lỗi thường gặp:**
> - DCL thiếu `volatile`; tạo singleton có state mutable không đồng bộ.
> - Builder không validate trong `build()` → object không hợp lệ "lọt" ra ngoài.
> - Lombok `@Builder` trên entity JPA mà quên `@NoArgsConstructor` / để mất giá trị mặc định của field (cần `@Builder.Default`).
> - Shallow copy collection rồi sửa bản sao làm hỏng bản gốc.

### 🛠 Bài tập phần 5

**Bài 5.1 — Phá singleton (Cơ bản)**
- Đề bài: Với `EagerConfig` (implement `Serializable`) và `IdGenerator` (enum), viết code phá singleton bằng reflection và serialization.
- Tiêu chí đạt: phá được `EagerConfig` cả 2 cách; sửa bằng guard trong constructor + `readResolve`; chứng minh enum không phá được (reflection ném `IllegalArgumentException: Cannot reflectively create enum objects`).

**Bài 5.2 — Stress test DCL (Trung bình)**
- Đề bài: Viết test 100 thread đồng loạt gọi `getInstance()` (dùng `CountDownLatch` làm cổng xuất phát) cho `NaiveLazy`, `SyncLazy`, `DclLazy`, `HolderLazy`; constructor có `Thread.sleep(10)` để mở rộng cửa sổ race.
- Tiêu chí đạt: `NaiveLazy` tạo > 1 instance (đếm bằng `AtomicInteger` trong constructor); các biến thể còn lại đúng 1; giải thích vì sao lỗi thiếu `volatile` của DCL rất khó tái hiện trên x86 (memory model mạnh) nhưng vẫn sai theo JMM. Bonus: dùng jcstress.

**Bài 5.3 — Builder có compile-time safety (Nâng cao)**
- Đề bài: Viết *step builder* cho `EmailMessage` bắt buộc theo thứ tự `from → to → subject → (body | htmlBody) → build`, với `cc`, `attachments` tùy chọn ở bước cuối.
- Tiêu chí đạt: gọi `build()` khi thiếu `subject` là **lỗi compile**; object immutable; so sánh trade-off với builder thường (độ phức tạp, số interface).

<details>
<summary>Gợi ý lời giải</summary>

Bài 5.1 — phá bằng reflection:
```java
var ctor = EagerConfig.class.getDeclaredConstructor();
ctor.setAccessible(true);
EagerConfig second = ctor.newInstance();
System.out.println(second == EagerConfig.getInstance()); // false
```
Guard: trong constructor `if (INSTANCE != null) throw new IllegalStateException("Singleton");` (an toàn vì `INSTANCE` được gán trong `<clinit>`, lần gọi constructor đầu tiên `INSTANCE` còn null).

Bài 5.3 — khung:
```java
public interface FromStep { ToStep from(String from); }
public interface ToStep { SubjectStep to(String... to); }
public interface SubjectStep { BodyStep subject(String s); }
public interface BodyStep { FinalStep body(String text); FinalStep htmlBody(String html); }
public interface FinalStep { FinalStep cc(String... cc); FinalStep attach(Path p); EmailMessage build(); }
// Một class private Steps implements tất cả interface; EmailMessage.builder() trả FromStep.
```

</details>

---

<a id="p6"></a>
## 6. Structural patterns: Adapter, Decorator, Proxy, Facade, Composite

### 6.1 Adapter
**Vấn đề:** cho hai interface không tương thích làm việc với nhau — bọc một class có sẵn (thường là thư viện bên thứ ba/legacy) để nó khớp interface mà code của ta mong đợi.

```java
// Interface của hệ thống ta (port)
interface SmsSender { void send(String phoneE164, String text); }

// SDK bên thứ ba — không sửa được, API khác hẳn
final class VendorSmsClient {
    int submit(String countryCode, String localNumber, String content, boolean unicode) {
        System.out.printf("Vendor: +%s %s (%s)%n", countryCode, localNumber, content);
        return 0; // 0 = OK
    }
}

// Object adapter (composition) — được ưa chuộng hơn class adapter (kế thừa)
final class VendorSmsAdapter implements SmsSender {
    private final VendorSmsClient client;
    VendorSmsAdapter(VendorSmsClient client) { this.client = client; }

    @Override public void send(String phoneE164, String text) {
        // +84901234567 → ("84", "901234567")
        String cc = phoneE164.substring(1, 3), local = phoneE164.substring(3);
        boolean unicode = !text.chars().allMatch(c -> c < 128);   // tiếng Việt có dấu → unicode
        int code = client.submit(cc, local, text, unicode);
        if (code != 0) throw new IllegalStateException("SMS vendor error code " + code); // dịch lỗi
    }
}
```
JDK: `Arrays.asList(T[])` (mảng → `List`), `InputStreamReader` (byte stream → char stream), `Collections.enumeration`/`Collections.list` (Iterator ↔ Enumeration). Spring MVC: `HandlerAdapter` cho phép `DispatcherServlet` gọi nhiều loại handler khác nhau (`@Controller` method, `HttpRequestHandler`...). Trong hexagonal architecture (phần 9), mọi "adapter" bọc hạ tầng là Adapter pattern ở mức kiến trúc — **anti-corruption layer** của DDD cũng vậy.

### 6.2 Decorator
**Vấn đề:** thêm trách nhiệm cho object **một cách động**, có thể **xếp chồng**, mà không tạo bùng nổ subclass (`LoggingCachingRetryingRepository`...).

```java
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

interface PriceService { long priceOf(String sku); }

final class RemotePriceService implements PriceService {          // component thật
    public long priceOf(String sku) { System.out.println("  → remote call " + sku); return sku.length() * 1000L; }
}

final class CachingPriceService implements PriceService {         // decorator 1
    private final PriceService inner; private final Map<String, Long> cache = new ConcurrentHashMap<>();
    CachingPriceService(PriceService inner) { this.inner = inner; }
    public long priceOf(String sku) { return cache.computeIfAbsent(sku, inner::priceOf); }
}

final class RetryingPriceService implements PriceService {        // decorator 2
    private final PriceService inner; private final int attempts;
    RetryingPriceService(PriceService inner, int attempts) { this.inner = inner; this.attempts = attempts; }
    public long priceOf(String sku) {
        RuntimeException last = null;
        for (int i = 1; i <= attempts; i++) {
            try { return inner.priceOf(sku); } catch (RuntimeException e) { last = e; }
        }
        throw last;
    }
}

final class TimingPriceService implements PriceService {          // decorator 3
    private final PriceService inner;
    TimingPriceService(PriceService inner) { this.inner = inner; }
    public long priceOf(String sku) {
        long t0 = System.nanoTime();
        try { return inner.priceOf(sku); }
        finally { System.out.printf("priceOf(%s) took %d µs%n", sku, (System.nanoTime() - t0) / 1000); }
    }
}

class DecoratorDemo {
    public static void main(String[] args) {
        // Thứ tự xếp chồng có ý nghĩa: timing đo cả cache; retry chỉ bọc remote call
        PriceService svc = new TimingPriceService(new CachingPriceService(new RetryingPriceService(new RemotePriceService(), 3)));
        svc.priceOf("SKU-1"); svc.priceOf("SKU-1");
    }
}
```
JDK: `java.io` là ví dụ kinh điển — `new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(f))))`; `Collections.unmodifiableList`, `Collections.synchronizedMap`, `Collections.checkedList`. Spring: `TransactionAwareDataSourceProxy`, `LazyConnectionDataSourceProxy` (tên "Proxy" nhưng về cấu trúc là decorator), `ContentCachingRequestWrapper`, `HttpServletRequestWrapper`. Resilience4j `Decorators.ofSupplier(...).withRetry(...).withCircuitBreaker(...)`.

### 6.3 Proxy — và cách Spring AOP hoạt động
**Vấn đề:** cung cấp một vật thay thế (surrogate) để **kiểm soát truy cập** tới object thật: lazy loading (virtual proxy), kiểm tra quyền (protection proxy), gọi từ xa (remote proxy), thêm cross-cutting concern (transaction, cache, security, metrics).

**Decorator vs Proxy** — cấu trúc gần như giống hệt; khác ở *ý định*: decorator *thêm* hành vi và do client chủ động xếp chồng; proxy *kiểm soát* truy cập, thường trong suốt với client và do hạ tầng/framework tạo ra, có thể quản lý vòng đời object thật.

**JDK dynamic proxy** — tạo lúc runtime một class implement các **interface** cho trước, chuyển mọi lời gọi tới `InvocationHandler`:

```java
import java.lang.reflect.*;
import java.util.*;

interface AccountService {
    void transfer(String from, String to, long amount);
    long balance(String id);
}

final class SimpleAccountService implements AccountService {
    public void transfer(String from, String to, long amount) { System.out.println("transfer " + amount); }
    public long balance(String id) { return 1_000; }
}

final class TxHandler implements InvocationHandler {
    private final Object target;
    TxHandler(Object target) { this.target = target; }

    @Override public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        boolean readOnly = method.getName().startsWith("balance");
        System.out.println("BEGIN TX" + (readOnly ? " (read-only)" : ""));
        try {
            Object result = method.invoke(target, args);
            System.out.println("COMMIT");
            return result;
        } catch (InvocationTargetException e) {      // bóc exception thật ra
            System.out.println("ROLLBACK");
            throw e.getCause();
        }
    }

    @SuppressWarnings("unchecked")
    static <T> T wrap(T target, Class<T> iface) {
        return (T) Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[]{iface}, new TxHandler(target));
    }

    public static void main(String[] args) {
        AccountService svc = wrap(new SimpleAccountService(), AccountService.class);
        svc.transfer("A", "B", 500);
        System.out.println(svc.balance("A"));
        System.out.println(svc.getClass());          // ví dụ: class $Proxy0 hoặc jdk.proxy1.$Proxy0 (tùy package/visibility của interface)
        System.out.println(svc instanceof SimpleAccountService);   // false — chỉ là AccountService
    }
}
```

**CGLIB (và ByteBuddy)** — sinh **subclass** của class đích lúc runtime, override các method để chèn interceptor. Không cần interface.

| | JDK dynamic proxy | CGLIB (Spring dùng bản repackaged `org.springframework.cglib`) |
|---|---|---|
| Cơ chế | Implement interface | Kế thừa class |
| Yêu cầu | Target có interface; chỉ proxy method của interface | Class và method **không `final`**; không proxy được `private`, `static` |
| Inject theo kiểu | Phải inject bằng **interface** (inject bằng class cụ thể → `BeanNotOfRequiredTypeException`) | Inject bằng class được |
| Constructor | Không liên quan | Spring dùng Objenesis để tạo instance proxy không gọi constructor (từ Spring 4) |

**Spring AOP chọn loại nào?** Spring Framework: nếu bean implement ít nhất một interface → JDK proxy, ngược lại → CGLIB; `proxyTargetClass=true` ép dùng CGLIB. **Spring Boot 2.x+ mặc định `spring.aop.proxy-target-class=true`** → CGLIB cho hầu hết bean. `@Transactional`, `@Async`, `@Cacheable`, `@Retryable`, `@PreAuthorize`, `@Validated` đều hoạt động qua proxy (do `BeanPostProcessor` tạo trong bean lifecycle — `postProcessAfterInitialization`).

**Hệ quả quan trọng nhất — self-invocation:**
```java
@org.springframework.stereotype.Service
public class OrderService {
    public void placeOrders(java.util.List<String> ids) {
        for (String id : ids) placeOne(id);     // gọi qua "this" → KHÔNG đi qua proxy → không có transaction!
    }

    @org.springframework.transaction.annotation.Transactional
    public void placeOne(String id) { /* ... */ }
}
```
Proxy chỉ chặn lời gọi **từ bên ngoài** đi qua nó; `this.placeOne()` gọi thẳng object thật. Tương tự, `@Transactional` trên method `private` (với proxy-based AOP) không có tác dụng (Spring 6 hỗ trợ thêm method `protected`/package-visible với CGLIB proxy). Cách xử lý:
1. **Tách method sang bean khác** (cách sạch nhất).
2. Inject chính mình qua proxy: `@Lazy @Autowired OrderService self;` hoặc `ObjectProvider<OrderService>`.
3. `AopContext.currentProxy()` với `@EnableAspectJAutoProxy(exposeProxy = true)` (gắn code với Spring AOP — ít được khuyến nghị).
4. Dùng AspectJ weaving (compile-time/load-time) thay cho proxy.
5. Dùng `TransactionTemplate` lập trình tường minh.

Proxy ở nơi khác: Hibernate lazy-loading (`HibernateProxy` — truy cập ngoài session → `LazyInitializationException`; `entity.getClass()` trả về class proxy, nên `equals` dùng `getClass()` dễ sai), Spring Data repository (proxy của interface), `@Configuration` class (CGLIB để `@Bean` method gọi lẫn nhau trả về cùng singleton — tắt bằng `proxyBeanMethods = false`), RMI/gRPC stub (remote proxy).

### 6.4 Facade
**Vấn đề:** cung cấp một interface **đơn giản, cấp cao** cho một hệ thống con phức tạp.

```java
// Hệ thống con
final class InventoryService { boolean reserve(String sku, int qty) { return true; } void release(String sku, int qty) {} }
final class PaymentService2  { String charge(String customerId, long amount) { return "PAY-1"; } }
final class ShippingService  { String createShipment(String orderId) { return "SHIP-9"; } }

// Facade — controller chỉ cần gọi một method
final class CheckoutFacade {
    private final InventoryService inventory; private final PaymentService2 payment; private final ShippingService shipping;
    CheckoutFacade(InventoryService i, PaymentService2 p, ShippingService s) { inventory = i; payment = p; shipping = s; }

    public String checkout(String orderId, String customerId, String sku, int qty, long amount) {
        if (!inventory.reserve(sku, qty)) throw new IllegalStateException("Out of stock");
        try {
            payment.charge(customerId, amount);
        } catch (RuntimeException e) {
            inventory.release(sku, qty);          // bù trừ (compensation)
            throw e;
        }
        return shipping.createShipment(orderId);
    }
}
```
JDK/Spring: `JdbcTemplate`, `RestTemplate`/`RestClient` (facade trên JDBC/HTTP API cấp thấp), SLF4J (facade trên Logback/Log4j2), `javax.faces.context.FacesContext`. Lưu ý: facade không nên trở thành God object chứa logic nghiệp vụ — nó *điều phối*; trong kiến trúc phân tầng, *application service* thường đóng vai facade.

### 6.5 Composite
**Vấn đề:** biểu diễn cấu trúc **cây part–whole** và cho client xử lý object đơn lẻ và nhóm object **một cách đồng nhất**.

```java
import java.util.*;

sealed interface PricingRule permits PercentOff, FixedOff, AllOf, BestOf {
    long apply(long price);
}
record PercentOff(int percent) implements PricingRule {
    public long apply(long p) { return p - p * percent / 100; }
}
record FixedOff(long amount) implements PricingRule {
    public long apply(long p) { return Math.max(0, p - amount); }
}
record AllOf(List<PricingRule> rules) implements PricingRule {       // composite: áp lần lượt
    public long apply(long p) { for (PricingRule r : rules) p = r.apply(p); return p; }
}
record BestOf(List<PricingRule> rules) implements PricingRule {      // composite: chọn giá tốt nhất
    public long apply(long p) { return rules.stream().mapToLong(r -> r.apply(p)).min().orElse(p); }
}

class CompositeDemo {
    public static void main(String[] args) {
        PricingRule promo = new BestOf(List.of(
                new PercentOff(20),
                new AllOf(List.of(new FixedOff(50_000), new PercentOff(5)))));
        System.out.println(promo.apply(300_000));   // min(240000, 237500) = 237500
    }
}
```
JDK/Spring: `java.awt.Container` chứa `Component`; Spring `CompositeCacheManager`, `CompositePropertySource`, `HandlerMethodArgumentResolverComposite`; Spring Security `DelegatingPasswordEncoder` (gần composite + strategy); cây AST, menu, quyền theo nhóm.

> 💡 **Góc nhìn Senior:** Hiểu proxy của Spring là "điểm phân loại" ứng viên Senior. Hãy trả lời được: *@Transactional không chạy trong trường hợp nào?* (self-invocation, method private/final, class final với CGLIB, bean không do Spring quản lý — `new` thủ công, exception checked không rollback mặc định — chỉ `RuntimeException`/`Error` rollback, exception bị nuốt trong method); *vì sao inject bằng class cụ thể bị lỗi khi dùng JDK proxy?*; *proxy được tạo ở bước nào của bean lifecycle?*

> ⚠️ **Lỗi thường gặp:**
> - Decorator quên forward một số method (hoặc `equals/hashCode`) → hành vi lệch.
> - Thứ tự decorator/advice sai: retry *bên ngoài* transaction (mỗi lần thử một transaction mới) khác hẳn retry *bên trong* transaction (thử lại trong transaction đã hỏng). Với Spring, kiểm soát bằng `@Order` của aspect.
> - Adapter "rò rỉ" kiểu dữ liệu của vendor ra ngoài (trả về `VendorResponse`) — mất ý nghĩa cách ly.

### 🛠 Bài tập phần 6

**Bài 6.1 — Adapter cho hai SDK (Cơ bản)**
- Đề bài: Định nghĩa `GeoCoder { Optional<LatLng> lookup(String address); }` và viết adapter cho hai "SDK" giả có API khác nhau (một trả `double[]`, một ném exception khi không tìm thấy).
- Tiêu chí đạt: client không biết SDK nào; exception vendor được dịch; test cho cả hai adapter dùng chung một bộ *contract test* (abstract test class).

**Bài 6.2 — Mini AOP bằng dynamic proxy (Trung bình)**
- Đề bài: Viết annotation `@Timed` và `@Retry(times=3)`; viết `ProxyFactory.create(target, iface)` dùng JDK dynamic proxy đọc annotation trên method của *class hiện thực* (không chỉ interface) và áp dụng hành vi tương ứng.
- Tiêu chí đạt: chạy đúng; tái hiện được vấn đề self-invocation (method A gọi method B có `@Retry` trong cùng class → không retry) và giải thích.

**Bài 6.3 — Chứng minh các giới hạn của @Transactional (Nâng cao)**
- Đề bài: Tạo project Spring Boot + H2 với các case: self-invocation, method `private`, method `final` trên class được proxy bằng CGLIB, checked exception, exception bị catch bên trong. Mỗi case viết một test kiểm tra dữ liệu có bị rollback không (dùng `TransactionSynchronizationManager.isActualTransactionActive()` để log).
- Tiêu chí đạt: bảng kết quả 5 case + giải thích; sửa từng case bằng cách phù hợp; in `AopUtils.isCglibProxy(bean)`/`isJdkDynamicProxy(bean)` khi bật/tắt `spring.aop.proxy-target-class`.

<details>
<summary>Gợi ý lời giải</summary>

Bài 6.2 — đọc annotation trên class hiện thực:
```java
Method impl = target.getClass().getMethod(method.getName(), method.getParameterTypes());
Retry retry = impl.getAnnotation(Retry.class);
int attempts = retry == null ? 1 : retry.times();
for (int i = 1; ; i++) {
    try { return impl.invoke(target, args); }
    catch (InvocationTargetException e) { if (i >= attempts) throw e.getCause(); }
}
```
Annotation cần `@Retention(RetentionPolicy.RUNTIME)` và `@Target(ElementType.METHOD)`.

Bài 6.3 — kỳ vọng: self-invocation → không transaction; private → không transaction; final method với CGLIB → không bị intercept (và field của proxy là null nếu method truy cập field trực tiếp — lỗi NPE khó hiểu!); checked exception → commit (trừ khi `rollbackFor = Exception.class`); exception bị nuốt → commit (hoặc `UnexpectedRollbackException` nếu đã bị đánh dấu rollback-only bởi transaction con tham gia cùng).

</details>

---

<a id="p7"></a>
## 7. Behavioral patterns: Strategy, Template Method, Observer, Chain of Responsibility, Command, State, Iterator

### 7.1 Strategy
**Vấn đề:** định nghĩa một họ thuật toán, đóng gói từng cái và cho phép **thay thế lúc runtime** — loại bỏ `if/else`/`switch` theo loại thuật toán.

Trong Java hiện đại, strategy thường chỉ là một **functional interface** + lambda:

```java
import java.math.BigDecimal;
import java.util.*;
import java.util.function.UnaryOperator;

public class StrategyDemo {
    @FunctionalInterface
    interface DiscountStrategy { BigDecimal apply(BigDecimal amount); }

    // Strategy là enum: tập hữu hạn, có tên, serialize được, dễ lưu cấu hình trong DB
    enum Tier implements DiscountStrategy {
        STANDARD { public BigDecimal apply(BigDecimal a) { return a; } },
        SILVER   { public BigDecimal apply(BigDecimal a) { return a.multiply(new BigDecimal("0.95")); } },
        GOLD     { public BigDecimal apply(BigDecimal a) { return a.multiply(new BigDecimal("0.90")); } }
    }

    static BigDecimal checkout(BigDecimal amount, DiscountStrategy strategy) { return strategy.apply(amount); }

    public static void main(String[] args) {
        var amount = new BigDecimal("1000000");
        System.out.println(checkout(amount, Tier.GOLD));
        System.out.println(checkout(amount, a -> a.subtract(new BigDecimal("50000"))));   // ad-hoc lambda strategy

        // Comparator là strategy kinh điển của JDK
        List<String> names = new ArrayList<>(List.of("Bình", "an", "Châu"));
        names.sort(Comparator.comparing((String s) -> s.toLowerCase(Locale.ROOT))
                .thenComparing(Comparator.reverseOrder()));
        System.out.println(names);
    }
}
```
JDK/Spring: `Comparator`, `RejectedExecutionHandler` của `ThreadPoolExecutor`, `java.util.function.*`; Spring `PlatformTransactionManager` (JDBC/JPA/JTA), `ResourceLoader`, `PasswordEncoder`, `HandlerMethodArgumentResolver`, `RetryPolicy`/`BackOffPolicy` của Spring Retry. Chọn strategy theo cấu hình: `Map<String, Strategy>` inject bởi Spring (như ví dụ OCP ở phần 3.2).

### 7.2 Template Method
**Vấn đề:** định nghĩa **khung thuật toán** trong method của lớp cha, để lớp con hiện thực các bước cụ thể mà không đổi cấu trúc tổng thể.

```java
import java.util.*;

abstract class DataImportJob<T> {
    // template method: final để subclass không phá vỡ thứ tự
    public final ImportReport run(List<String> lines) {
        var report = new ImportReport();
        beforeImport();                                      // hook (tùy chọn override)
        for (String line : lines) {
            try {
                T record = parse(line);                      // bước bắt buộc
                validate(record);
                save(record);
                report.ok++;
            } catch (IllegalArgumentException e) {
                report.errors.add(line + " → " + e.getMessage());
            }
        }
        afterImport(report);
        return report;
    }
    protected abstract T parse(String line);
    protected void validate(T record) {}                     // hook mặc định không làm gì
    protected abstract void save(T record);
    protected void beforeImport() {}
    protected void afterImport(ImportReport r) {}

    static final class ImportReport { int ok; final List<String> errors = new ArrayList<>();
        @Override public String toString() { return "ok=" + ok + " errors=" + errors; } }
}

final class UserImportJob extends DataImportJob<UserImportJob.User> {
    record User(String email, int age) {}
    private final List<User> db = new ArrayList<>();
    @Override protected User parse(String line) {
        String[] p = line.split(",");
        if (p.length != 2) throw new IllegalArgumentException("expected 2 columns");
        return new User(p[0].trim(), Integer.parseInt(p[1].trim()));   // NumberFormatException là IllegalArgumentException
    }
    @Override protected void validate(User u) { if (!u.email().contains("@")) throw new IllegalArgumentException("bad email"); }
    @Override protected void save(User u) { db.add(u); }

    public static void main(String[] args) {
        System.out.println(new UserImportJob().run(List.of("a@x.vn,30", "bad,20", "b@x.vn,abc", "c@x.vn,41")));
    }
}
```
JDK: `AbstractList` (bạn chỉ cần `get` và `size`), `InputStream.read(byte[],int,int)` gọi `read()` trừu tượng, `HttpServlet.service()` gọi `doGet/doPost`. Spring: `AbstractApplicationContext.refresh()`; các lớp `*Template` (`JdbcTemplate`, `TransactionTemplate`, `RestTemplate`) là **template method dạng composition**: khung cố định, phần thay đổi truyền vào qua **callback** (`RowMapper`, `TransactionCallback`) — tức là kết hợp với Strategy, tránh được ràng buộc kế thừa.

**Template Method vs Strategy:** Template Method dùng *kế thừa*, biến đổi *một phần* thuật toán, quyết định ở compile-time; Strategy dùng *composition*, thay *toàn bộ* thuật toán, quyết định lúc runtime. Ưu tiên Strategy/callback trừ khi khung có nhiều bước và hook chia sẻ state.

### 7.3 Observer
**Vấn đề:** khi một object (subject) đổi trạng thái, tự động thông báo cho các object phụ thuộc (observer) — **giảm coupling** giữa nơi phát sinh sự kiện và nơi phản ứng.

```java
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

final class EventBus<E> {
    private final List<Consumer<? super E>> listeners = new CopyOnWriteArrayList<>(); // an toàn khi đăng ký/hủy lúc đang phát

    /** Trả về handle để hủy đăng ký — tránh memory leak (Module 05, phần 10) */
    AutoCloseable subscribe(Consumer<? super E> listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    void publish(E event) {
        for (var l : listeners) {
            try { l.accept(event); }
            catch (RuntimeException ex) {             // một listener lỗi không chặn listener khác
                System.err.println("Listener failed: " + ex);
            }
        }
    }
}

record OrderPlaced(String orderId, long amount) {}

class ObserverDemo {
    public static void main(String[] args) throws Exception {
        EventBus<OrderPlaced> bus = new EventBus<>();
        try (AutoCloseable s1 = bus.subscribe(e -> System.out.println("Email: cảm ơn đơn " + e.orderId()));
             AutoCloseable s2 = bus.subscribe(e -> System.out.println("Loyalty: +" + e.amount() / 1000 + " điểm"))) {
            bus.publish(new OrderPlaced("O-1", 250_000));
        }
        bus.publish(new OrderPlaced("O-2", 100_000));   // không ai nghe nữa
    }
}
```
JDK: `java.util.Observer/Observable` (**deprecated từ Java 9** — không thread-safe, không generic, `Observable` là class), `java.beans.PropertyChangeListener`, `java.util.concurrent.Flow` (Reactive Streams, Java 9). Spring: `ApplicationEventPublisher` + `@EventListener`; `@TransactionalEventListener(phase = AFTER_COMMIT)` để chỉ phản ứng khi transaction đã commit (gửi email sau khi đơn hàng thực sự được lưu).

Lưu ý production với Spring events: **mặc định đồng bộ, cùng thread, cùng transaction** — listener chậm làm chậm request; exception trong listener (sync) lan ra publisher. Dùng `@Async` cho listener cần tách, nhưng khi đó mất transaction context và có thể mất event nếu app crash → với sự kiện quan trọng cần **Transactional Outbox** (lưu event vào DB cùng transaction rồi publish ra message broker).

### 7.4 Chain of Responsibility
**Vấn đề:** truyền request qua một **chuỗi handler**; mỗi handler quyết định xử lý, sửa đổi, hoặc chuyển tiếp — người gửi không biết ai sẽ xử lý.

```java
import java.util.*;

record Request(String user, String path, Map<String, String> headers) {}
record Response(int status, String body) {}

interface Handler { Response handle(Request req, Chain chain); }
interface Chain { Response proceed(Request req); }

final class Pipeline {
    private final List<Handler> handlers; private final Chain terminal;
    Pipeline(List<Handler> handlers, Chain terminal) { this.handlers = List.copyOf(handlers); this.terminal = terminal; }

    Response execute(Request req) { return chainAt(0).proceed(req); }

    private Chain chainAt(int index) {
        return req -> index < handlers.size() ? handlers.get(index).handle(req, chainAt(index + 1)) : terminal.proceed(req);
    }

    public static void main(String[] args) {
        Handler logging = (req, chain) -> {
            long t0 = System.nanoTime();
            Response r = chain.proceed(req);
            System.out.printf("%s %s → %d (%d µs)%n", req.user(), req.path(), r.status(), (System.nanoTime() - t0) / 1000);
            return r;
        };
        Handler auth = (req, chain) -> req.headers().containsKey("Authorization")
                ? chain.proceed(req) : new Response(401, "Unauthorized");      // dừng chuỗi
        Handler rateLimit = new Handler() {
            private final Map<String, Integer> counts = new HashMap<>();
            public Response handle(Request req, Chain chain) {
                int c = counts.merge(req.user(), 1, Integer::sum);
                return c > 2 ? new Response(429, "Too Many Requests") : chain.proceed(req);
            }
        };
        var pipeline = new Pipeline(List.of(logging, auth, rateLimit), req -> new Response(200, "OK " + req.path()));
        var withAuth = Map.of("Authorization", "Bearer x");
        for (int i = 0; i < 3; i++) pipeline.execute(new Request("an", "/orders", withAuth));
        pipeline.execute(new Request("binh", "/orders", Map.of()));
    }
}
```
JDK/Spring: Servlet `Filter` + `FilterChain`, **Spring Security `SecurityFilterChain`** (chuỗi ~15 filter), Spring MVC `HandlerInterceptor`, `java.util.logging.Logger` chuyển log lên logger cha, interceptor của OkHttp/gRPC/Feign, Netty `ChannelPipeline`. Rủi ro: thứ tự handler sai (auth sau logging body nhạy cảm), không handler nào xử lý (cần terminal mặc định), chuỗi dài khó debug.

### 7.5 Command
**Vấn đề:** đóng gói một **yêu cầu thành object** → có thể xếp hàng, log, gửi qua mạng, retry, **undo/redo**, thực thi trễ.

```java
import java.util.*;

interface Command { void execute(); void undo(); }

final class TextBuffer { final StringBuilder text = new StringBuilder(); }

final class InsertCommand implements Command {
    private final TextBuffer buf; private final int pos; private final String s;
    InsertCommand(TextBuffer buf, int pos, String s) { this.buf = buf; this.pos = pos; this.s = s; }
    public void execute() { buf.text.insert(pos, s); }
    public void undo()    { buf.text.delete(pos, pos + s.length()); }
}

final class DeleteCommand implements Command {
    private final TextBuffer buf; private final int from, to; private String removed;
    DeleteCommand(TextBuffer buf, int from, int to) { this.buf = buf; this.from = from; this.to = to; }
    public void execute() { removed = buf.text.substring(from, to); buf.text.delete(from, to); }
    public void undo()    { buf.text.insert(from, removed); }
}

final class Editor {
    private final Deque<Command> undo = new ArrayDeque<>(), redo = new ArrayDeque<>();
    void run(Command c) { c.execute(); undo.push(c); redo.clear(); }
    void undo() { if (!undo.isEmpty()) { Command c = undo.pop(); c.undo(); redo.push(c); } }
    void redo() { if (!redo.isEmpty()) { Command c = redo.pop(); c.execute(); undo.push(c); } }

    public static void main(String[] args) {
        var buf = new TextBuffer(); var ed = new Editor();
        ed.run(new InsertCommand(buf, 0, "Xin chào"));
        ed.run(new InsertCommand(buf, 8, " Senior"));
        ed.run(new DeleteCommand(buf, 0, 4));
        System.out.println(buf.text);   // "chào Senior"
        ed.undo(); System.out.println(buf.text);   // "Xin chào Senior"
        ed.undo(); ed.redo(); System.out.println(buf.text);
    }
}
```
JDK: `Runnable`/`Callable` gửi vào `ExecutorService` (command + invoker), `javax.swing.Action`. Ở mức kiến trúc: *command* trong CQRS, message trong hàng đợi (Kafka/RabbitMQ), job của Spring Batch/Quartz. Khi command đi qua mạng/được retry → cần **idempotency key**.

### 7.6 State
**Vấn đề:** object thay đổi hành vi theo **trạng thái nội tại**; thay vì `switch(state)` rải khắp các method, mỗi trạng thái là một object biết các chuyển trạng thái hợp lệ.

```java
import java.util.*;

public class OrderStateDemo {
    enum Status {
        NEW {
            Status pay()    { return PAID; }
            Status cancel() { return CANCELLED; }
        },
        PAID {
            Status ship()   { return SHIPPED; }
            Status cancel() { return REFUNDING; }       // hủy sau thanh toán → hoàn tiền
        },
        SHIPPED   { Status deliver() { return DELIVERED; } },
        DELIVERED, CANCELLED, REFUNDING;

        // Mặc định: chuyển trạng thái không hợp lệ
        Status pay()     { throw illegal("pay"); }
        Status ship()    { throw illegal("ship"); }
        Status deliver() { throw illegal("deliver"); }
        Status cancel()  { throw illegal("cancel"); }
        IllegalStateException illegal(String action) { return new IllegalStateException("Cannot " + action + " when " + this); }
    }

    static final class Order {
        private Status status = Status.NEW;
        private final List<String> history = new ArrayList<>(List.of("NEW"));
        void pay()     { transition(status.pay()); }
        void ship()    { transition(status.ship()); }
        void deliver() { transition(status.deliver()); }
        void cancel()  { transition(status.cancel()); }
        private void transition(Status next) { status = next; history.add(next.name()); }
    }

    public static void main(String[] args) {
        Order o = new Order();
        o.pay(); o.ship();
        try { o.cancel(); } catch (IllegalStateException e) { System.out.println(e.getMessage()); } // Cannot cancel when SHIPPED
        o.deliver();
        System.out.println(o.history);
    }
}
```
**State vs Strategy:** cấu trúc giống nhau; khác ở chỗ *ai* đổi object: Strategy do **client** chọn từ ngoài, các strategy không biết nhau; State do **chính các state** quyết định trạng thái kế tiếp. Với state machine phức tạp (guard, action, persist, phân tán) cân nhắc Spring Statemachine hoặc mô hình hóa bằng bảng chuyển trạng thái; trong DB nhớ dùng **optimistic locking** (`@Version`) để hai request không cùng chuyển trạng thái.

### 7.7 Iterator
**Vấn đề:** duyệt phần tử của một tập hợp **mà không lộ cấu trúc bên trong**.

```java
import java.util.*;

/** Khoảng số với bước nhảy — duyệt lười, không tạo list */
record Range(int from, int toExclusive, int step) implements Iterable<Integer> {
    Range { if (step <= 0) throw new IllegalArgumentException("step > 0"); }

    @Override public Iterator<Integer> iterator() {
        return new Iterator<>() {
            private int next = from;
            @Override public boolean hasNext() { return next < toExclusive; }
            @Override public Integer next() {
                if (!hasNext()) throw new NoSuchElementException();   // hợp đồng của Iterator
                int v = next; next += step; return v;
            }
        };
    }

    public static void main(String[] args) {
        for (int x : new Range(0, 20, 5)) System.out.print(x + " ");    // 0 5 10 15
        System.out.println();
        List<Integer> list = new ArrayList<>(List.of(1, 2, 3, 4));
        try { for (Integer i : list) if (i == 2) list.remove(i); }      // fail-fast
        catch (ConcurrentModificationException e) { System.out.println("CME!"); }
        list.removeIf(i -> i % 2 == 0);                                  // cách đúng
        System.out.println(list);
    }
}
```
Kiến thức liên quan: iterator của `ArrayList`/`HashMap` là **fail-fast** (dựa vào `modCount`, *best-effort*, không bảo đảm trong môi trường đa luồng); iterator của `ConcurrentHashMap`/`CopyOnWriteArrayList` là **weakly consistent / snapshot** (không ném CME). `Spliterator` (Java 8) là iterator hỗ trợ chia nhỏ cho parallel stream. Phân biệt external iteration (`for`, `Iterator`) và internal iteration (`forEach`, stream).

> 💡 **Góc nhìn Senior:** Nhiều behavioral pattern trong Java hiện đại "tan" vào ngôn ngữ: Strategy/Command = lambda; Iterator = `Iterable` + stream; Observer = reactive streams/event; State = enum hoặc sealed + pattern matching. Điều người phỏng vấn muốn nghe là bạn **nhận ra** pattern trong framework đang dùng hằng ngày (Filter chain, `JdbcTemplate`, `@EventListener`, `Comparator`) và chọn hình thức *nhẹ nhất* đủ dùng, thay vì dựng class hierarchy kiểu sách GoF 1994.

> ⚠️ **Lỗi thường gặp:**
> - Observer: listener đăng ký mà không hủy (leak), listener ném exception làm hỏng publisher, gọi listener trong lúc giữ lock (deadlock).
> - Template Method với quá nhiều hook → subclass phải hiểu toàn bộ lớp cha (fragile base class).
> - Iterator tự viết không ném `NoSuchElementException` hoặc `hasNext()` có side effect.

### 🛠 Bài tập phần 7

**Bài 7.1 — Strategy thay switch (Cơ bản)**
- Đề bài: Viết `ShippingFeeCalculator` có 4 hãng vận chuyển (GHN, GHTK, ViettelPost, tự giao) với công thức khác nhau; ban đầu dùng `switch`, sau đó refactor sang Strategy với `Map<Carrier, FeeStrategy>`.
- Tiêu chí đạt: thêm hãng mới không sửa calculator; unit test cho từng strategy độc lập.

**Bài 7.2 — Order workflow (Trung bình)**
- Đề bài: Kết hợp **State** (vòng đời đơn hàng như 7.6) + **Observer** (phát `OrderStatusChanged` cho Email/Loyalty/Analytics) + **Command** (mỗi hành động `PayOrder`, `ShipOrder`, `CancelOrder` là command có `idempotencyKey`, được log và có thể retry).
- Tiêu chí đạt: chuyển trạng thái không hợp lệ bị chặn; command trùng `idempotencyKey` chỉ thực thi một lần; listener lỗi không làm hỏng chuyển trạng thái; test đầy đủ.

**Bài 7.3 — Validation chain có thể cấu hình (Nâng cao)**
- Đề bài: Xây dựng engine kiểm tra giao dịch chống gian lận dạng Chain of Responsibility: các rule (`AmountLimitRule`, `VelocityRule` — quá N giao dịch/phút, `BlacklistRule`, `GeoMismatchRule`) có thứ tự và bật/tắt từ file cấu hình YAML/JSON; mỗi rule trả `ALLOW`, `DENY(reason)`, hoặc `REVIEW`; hỗ trợ chế độ "short-circuit" và "collect all".
- Tiêu chí đạt: thêm rule mới không sửa engine; `VelocityRule` thread-safe dưới tải đồng thời (test 50 thread); đo latency p99 của chuỗi < 1ms cho 10 rule (JMH hoặc đo đơn giản).

<details>
<summary>Gợi ý lời giải</summary>

Bài 7.2 — idempotency đơn giản trong bộ nhớ:
```java
final class CommandBus {
    private final Set<String> processed = ConcurrentHashMap.newKeySet();
    void dispatch(OrderCommand cmd) {
        if (!processed.add(cmd.idempotencyKey())) return;   // đã xử lý → bỏ qua (trả kết quả cũ nếu cần)
        cmd.execute();
    }
}
```
Trên production: lưu key trong DB với unique constraint, cùng transaction với thay đổi trạng thái.

Bài 7.3 — `VelocityRule` dùng `ConcurrentHashMap<String, Deque<Long>>` + `compute` để cập nhật cửa sổ trượt nguyên tử theo key, hoặc bucket theo phút với `LongAdder`. Kết quả rule: `sealed interface Decision permits Allow, Deny, Review` với `record Deny(String reason)`.

</details>

---

<a id="p8"></a>
## 8. Anti-patterns

### 8.1 God Object / God Class
Một class "biết tất cả, làm tất cả": `OrderManager` 5.000 dòng, 40 dependency, dùng ở mọi nơi. Hậu quả: merge conflict liên tục, không ai dám sửa, test cần dựng cả thế giới. Dấu hiệu: tên chung chung (`Manager`, `Helper`, `Util`, `Processor`), constructor > 7 tham số, file bị sửa trong > 30% số PR. Cách xử lý: xác định các *actor/trục thay đổi* (SRP), Extract Class theo nhóm field–method gắn kết, đẩy hành vi vào domain object, tách theo use case (một application service cho mỗi nhóm use case).

### 8.2 Anemic Domain Model — một cuộc tranh luận
Martin Fowler (bliki "AnemicDomainModel", 2003) gọi đây là anti-pattern: domain object chỉ có getter/setter, toàn bộ logic nằm trong service "transaction script".

```java
// Anemic: invariant nằm ở đâu đó trong service (hoặc không ở đâu cả)
class Account { private long balance; public long getBalance() { return balance; } public void setBalance(long b) { balance = b; } }
class AccountService {
    void withdraw(Account a, long amount) {
        if (a.getBalance() < amount) throw new IllegalStateException("Insufficient");
        a.setBalance(a.getBalance() - amount);       // service khác có thể gọi setBalance(-1000) tùy ý
    }
}

// Rich: object tự bảo vệ invariant
class RichAccount {
    private long balance;
    void withdraw(long amount) {
        if (amount <= 0) throw new IllegalArgumentException("amount > 0");
        if (balance < amount) throw new IllegalStateException("Insufficient funds");
        balance -= amount;
    }
    long balance() { return balance; }
}
```
**Hai phía của tranh luận:**
- *Ủng hộ rich model (DDD)*: invariant tập trung một chỗ, không thể tạo trạng thái sai, ngôn ngữ domain thể hiện trong code, dễ test logic thuần không cần mock.
- *Chấp nhận anemic*: với ứng dụng **CRUD đơn giản**, logic ít, rich model là over-engineering; phong cách hàm (dữ liệu bất biến + hàm thuần — "data-oriented programming" với record + sealed + pattern matching trong Java 21) tách dữ liệu và hành vi một cách *có chủ đích* nhưng vẫn giữ tính đúng nhờ immutable và kiểu dữ liệu chặt.
- Kết luận Senior: vấn đề thật không phải "có method hay không" mà là **invariant có được bảo vệ ở một nơi duy nhất không** và mức độ phức tạp của domain có xứng với chi phí của DDD không.

### 8.3 Service Locator
Thay vì nhận dependency qua constructor, class tự "hỏi" một registry toàn cục:
```java
class ReportService {
    void generate() {
        var repo = ServiceLocator.get(ReportRepository.class);   // dependency ẩn
        var mailer = ServiceLocator.get(Mailer.class);
    }
}
```
Vì sao bị coi là anti-pattern (Mark Seemann): dependency **ẩn** (đọc constructor không biết class cần gì), lỗi thiếu dependency chỉ lộ lúc runtime, test phải cấu hình registry toàn cục. Trong Spring, gọi `applicationContext.getBean(...)` trong code nghiệp vụ là service locator. Ngoại lệ hợp lý: code hạ tầng/framework, tra cứu động theo tên lúc runtime (khi đó inject `Map<String, Strategy>` hoặc `ObjectProvider` vẫn tốt hơn).

### 8.4 Các anti-pattern khác cần biết tên
| Anti-pattern | Mô tả ngắn |
|---|---|
| **Golden Hammer** | Dùng một công nghệ/pattern cho mọi bài toán ("cái gì cũng Kafka", "cái gì cũng microservice") |
| **Lava Flow / Dead Code** | Code không ai dám xóa vì không biết còn dùng không |
| **Copy-Paste Programming** | Nhân bản logic → sửa bug một chỗ, quên chỗ khác |
| **Magic Numbers/Strings** | Hằng số không tên rải rác |
| **Big Ball of Mud** | Hệ thống không có cấu trúc rõ ràng, mọi thứ phụ thuộc mọi thứ |
| **Distributed Monolith** | Microservice phải deploy cùng nhau, gọi đồng bộ dây chuyền, dùng chung DB |
| **Premature Optimization / Premature Abstraction** | Tối ưu/trừu tượng hóa trước khi có số liệu/trường hợp thứ hai |
| **Singletonitis** | Lạm dụng singleton tĩnh → global state |
| **Exception swallowing / Pokemon exception handling** | `catch (Exception e) {}` — "gotta catch 'em all" |
| **Open Session In View** (Spring/JPA) | Giữ persistence context tới tầng view → lazy loading ngầm, N+1 query, giữ connection lâu. Spring Boot bật mặc định và in cảnh báo; nên tắt `spring.jpa.open-in-view=false` |

> 💡 **Góc nhìn Senior:** Gọi tên được anti-pattern giúp review và thảo luận nhanh hơn, nhưng đừng dùng nó để "dán nhãn" đồng nghiệp. Hãy kèm *hậu quả cụ thể* và *đề xuất từng bước* (thường là refactor dần, không viết lại).

> ⚠️ **Lỗi thường gặp:** Biến mọi entity JPA thành "rich model" có logic gọi repository/service bên trong — domain object không nên phụ thuộc hạ tầng; logic cần nhiều aggregate thuộc về *domain service* hoặc *application service*.

### 🛠 Bài tập phần 8

**Bài 8.1 — Gọi tên anti-pattern (Cơ bản)**
- Đề bài: Liệt kê 3 anti-pattern bạn từng gặp trong dự án thật (hoặc open source), mô tả hậu quả đo được (bug, thời gian sửa).
- Tiêu chí đạt: mỗi mục có "triệu chứng → hậu quả → đề xuất sửa từng bước".

**Bài 8.2 — Từ anemic sang rich (Trung bình)**
- Đề bài: Cho `ShoppingCart` anemic (list item public, service tính tổng, áp mã giảm giá, kiểm tra tồn kho tối đa 10 sản phẩm/loại). Chuyển thành rich model: `cart.addItem(product, qty)`, `cart.applyCoupon(coupon, clock)`, `cart.total()`.
- Tiêu chí đạt: không còn setter/collection mutable lộ ra (`List.copyOf` khi trả về); mọi invariant được test; service mỏng chỉ còn điều phối (load → gọi domain → save).

**Bài 8.3 — Gỡ God class (Nâng cao)**
- Đề bài: Lấy một God class thực tế (≥ 1.000 dòng) từ dự án của bạn hoặc open source. Viết kế hoạch tách thành các class gắn kết, theo thứ tự các PR nhỏ có thể merge độc lập.
- Tiêu chí đạt: sơ đồ trước/sau; mỗi PR < 400 dòng thay đổi và có test; xác định rủi ro và cách rollback.

<details>
<summary>Gợi ý lời giải</summary>

Bài 8.2 — khung:
```java
public final class ShoppingCart {
    private static final int MAX_PER_PRODUCT = 10;
    private final Map<ProductId, Integer> items = new LinkedHashMap<>();
    private Coupon coupon;

    public void addItem(Product p, int qty) {
        if (qty <= 0) throw new IllegalArgumentException("qty > 0");
        int newQty = items.getOrDefault(p.id(), 0) + qty;
        if (newQty > MAX_PER_PRODUCT) throw new CartLimitExceeded(p.id(), MAX_PER_PRODUCT);
        items.put(p.id(), newQty);
    }
    public void applyCoupon(Coupon c, Clock clock) {
        if (c.isExpired(clock)) throw new CouponExpired(c.code());
        this.coupon = c;
    }
    public Map<ProductId, Integer> items() { return Map.copyOf(items); }
}
```
Bài 8.3: dùng git log để tìm cụm method thay đổi cùng nhau (change coupling), mỗi cụm là ứng viên một class mới.

</details>

---

<a id="p9"></a>
## 9. Kiến trúc: layered, hexagonal, clean architecture

### 9.1 Layered (N-tier) architecture
```
┌──────────────────────┐
│ Presentation (REST)  │  @RestController, DTO, validation đầu vào
├──────────────────────┤
│ Service / Application│  @Service, @Transactional, điều phối use case
├──────────────────────┤
│ Persistence (DAO)    │  @Repository, JPA entity, SQL
├──────────────────────┤
│ Database             │
└──────────────────────┘
Phụ thuộc đi từ trên xuống.
```
- **Ưu**: quen thuộc, dễ bắt đầu, phù hợp CRUD.
- **Nhược**: domain/service phụ thuộc vào persistence (entity JPA thường lan lên tận controller); logic nghiệp vụ bị "kéo" về phía DB; khó test service mà không có DB; "layer" thường chỉ là chuyển tiếp (pass-through) → nhiều boilerplate.
- **Strict** (chỉ gọi tầng ngay dưới) vs **relaxed** (được bỏ qua tầng).
- Biến thể nên biết: **package-by-layer** (`controller/`, `service/`, `repository/`) vs **package-by-feature** (`order/`, `payment/`, mỗi package chứa đủ các tầng) — package-by-feature cho cohesion cao hơn và cho phép dùng `package-private` để ẩn chi tiết.

### 9.2 Hexagonal Architecture (Ports & Adapters — Alistair Cockburn, 2005)
Ý tưởng: **ứng dụng (domain + use case) ở trung tâm, không biết gì về thế giới bên ngoài**. Giao tiếp qua **port** (interface do ứng dụng định nghĩa) và **adapter** (hiện thực kỹ thuật).

```
           Driving (primary) side                     Driven (secondary) side
  ┌───────────────┐                                         ┌──────────────────┐
  │ REST adapter  │──┐                                  ┌──►│ JPA adapter      │
  └───────────────┘  │   ┌──────────────────────────┐   │   └──────────────────┘
  ┌───────────────┐  ├──►│ Input port (use case)    │   │   ┌──────────────────┐
  │ Kafka consumer│──┤   │    Application core      │───┼──►│ Kafka publisher  │
  └───────────────┘  │   │    (domain model)        │   │   └──────────────────┘
  ┌───────────────┐  │   │ Output port (interface)  │───┘   ┌──────────────────┐
  │ CLI / Test    │──┘   └──────────────────────────┘──────►│ Payment API client│
  └───────────────┘                                         └──────────────────┘
  Mọi mũi tên phụ thuộc mã nguồn đều hướng VÀO lõi.
```

```java
// (Minh họa: mỗi type public nằm trong một file riêng)
// ===== core: domain + application (không import Spring/JPA) =====
package com.acme.order.core;

public record OrderId(String value) {}

public final class Order {
    private final OrderId id; private final long amount; private boolean paid;
    public Order(OrderId id, long amount) { if (amount <= 0) throw new IllegalArgumentException(); this.id = id; this.amount = amount; }
    public void markPaid() { if (paid) throw new IllegalStateException("Already paid"); paid = true; }
    public OrderId id() { return id; } public long amount() { return amount; } public boolean isPaid() { return paid; }
}

// Input port (driving) — use case
public interface PayOrderUseCase { void pay(OrderId id); }

// Output ports (driven) — do core định nghĩa theo nhu cầu của nó
public interface LoadOrderPort { java.util.Optional<Order> load(OrderId id); }
public interface SaveOrderPort { void save(Order order); }
public interface ChargePaymentPort { void charge(OrderId id, long amount); }

// Application service hiện thực use case
public final class PayOrderService implements PayOrderUseCase {
    private final LoadOrderPort load; private final SaveOrderPort save; private final ChargePaymentPort payment;
    public PayOrderService(LoadOrderPort l, SaveOrderPort s, ChargePaymentPort p) { load = l; save = s; payment = p; }
    @Override public void pay(OrderId id) {
        Order order = load.load(id).orElseThrow(() -> new IllegalArgumentException("Order not found: " + id.value()));
        payment.charge(order.id(), order.amount());
        order.markPaid();
        save.save(order);
    }
}

// ===== adapters (package khác, được phép import Spring/JPA) =====
// @RestController class OrderController { private final PayOrderUseCase useCase; ... POST /orders/{id}/payment → useCase.pay(...) }
// @Component class OrderJpaAdapter implements LoadOrderPort, SaveOrderPort { map OrderEntity <-> Order }
// @Component class StripePaymentAdapter implements ChargePaymentPort { gọi HTTP, dịch lỗi }
// @Configuration: @Bean PayOrderUseCase payOrder(...) { return new PayOrderService(...); }  // composition root
```
Lợi ích: test use case bằng **in-memory adapter** (nhanh, không cần Spring/DB); đổi DB/message broker chỉ đổi adapter; core không bị "nhiễm" annotation framework. Chi phí: nhiều interface và **mapping** (entity JPA ↔ domain, DTO ↔ domain), có thể thừa cho CRUD đơn giản.

### 9.3 Clean Architecture (Robert C. Martin, 2012/2017)
Các vòng tròn đồng tâm: **Entities** (quy tắc nghiệp vụ toàn doanh nghiệp) → **Use Cases** (quy tắc của ứng dụng) → **Interface Adapters** (controller, presenter, gateway) → **Frameworks & Drivers** (web, DB, UI). **Dependency Rule**: phụ thuộc mã nguồn chỉ hướng **vào trong**; vòng trong không biết tên bất cứ thứ gì ở vòng ngoài. Dữ liệu vượt ranh giới dưới dạng cấu trúc đơn giản (không truyền entity JPA hay `HttpServletRequest` vào trong).

Hexagonal, Onion (Jeffrey Palermo), Clean Architecture **cùng một tư tưởng**: domain độc lập, DIP ở mức kiến trúc; khác chủ yếu ở thuật ngữ và số vòng.

| | Layered | Hexagonal / Clean |
|---|---|---|
| Trung tâm | Database | Domain/use case |
| Hướng phụ thuộc | Trên → dưới (domain → persistence) | Ngoài → trong (persistence → domain) |
| Test logic | Thường cần DB/mock repository | Unit test thuần với fake adapter |
| Chi phí khởi đầu | Thấp | Cao hơn (port, mapping) |
| Hợp với | CRUD, ứng dụng nhỏ, team mới | Domain phức tạp, sống lâu, nhiều kênh vào/ra |

> 💡 **Góc nhìn Senior:** Kiến trúc là *quyết định có trade-off*, nên trả lời phỏng vấn theo dạng "tùy vào...": độ phức tạp domain, tuổi thọ dự kiến, kích thước team, yêu cầu thay đổi hạ tầng. Một cách thực dụng: **package-by-feature + hexagonal "nhẹ"** — chỉ tách port ở chỗ có lý do (gọi hệ thống ngoài, message broker), cho phép dùng entity JPA làm domain model khi mapping không đem lại giá trị, và **ép ranh giới bằng ArchUnit / Spring Modulith / multi-module** thay vì chỉ bằng quy ước.

> ⚠️ **Lỗi thường gặp:** "Clean architecture" hình thức: tạo đủ 4 tầng + interface cho mọi thứ nhưng use case vẫn chỉ là CRUD chuyển tiếp, domain vẫn anemic, và `@Transactional`/`@Entity` vẫn rò rỉ vào core.

### 🛠 Bài tập phần 9

**Bài 9.1 — Package-by-layer → package-by-feature (Cơ bản)**
- Đề bài: Tổ chức lại một project Spring Boot nhỏ (Product, Order, Customer) từ package-by-layer sang package-by-feature; hạ visibility của các class không cần public.
- Tiêu chí đạt: số class `public` giảm; build và test xanh; giải thích lợi ích.

**Bài 9.2 — Hexagonal "Pay Order" (Trung bình)**
- Đề bài: Hiện thực đầy đủ ví dụ 9.2 với Spring Boot: REST adapter, JPA adapter (H2), payment adapter giả lập gọi HTTP (WireMock), và một **CLI adapter** thứ hai gọi cùng use case.
- Tiêu chí đạt: module `core` (Maven/Gradle module riêng) không có dependency Spring/JPA — build fail nếu thêm; unit test use case dùng in-memory adapter chạy < 100ms; integration test cho từng adapter.

**Bài 9.3 — Viết ADR (Nâng cao)**
- Đề bài: Viết một *Architecture Decision Record* (theo mẫu Michael Nygard: Context, Decision, Status, Consequences) cho lựa chọn "layered hay hexagonal" cho một service thật (hoặc giả định: service quản lý khoản vay với quy tắc nghiệp vụ phức tạp, tích hợp 3 hệ thống ngân hàng).
- Tiêu chí đạt: nêu ít nhất 2 phương án, tiêu chí đánh giá, hệ quả tích cực/tiêu cực, điều kiện để xem xét lại quyết định.

<details>
<summary>Gợi ý lời giải</summary>

Bài 9.2 — cấu trúc module gợi ý:
```
order-core/          (chỉ JDK + test libs)   domain, port, PayOrderService
order-adapter-web/   (spring-web)           OrderController, DTO, exception mapping
order-adapter-jpa/   (spring-data-jpa)      OrderEntity, OrderJpaAdapter, mapper
order-adapter-payment/ (RestClient)         HttpPaymentAdapter
order-app/           (spring-boot)          @SpringBootApplication, @Configuration wiring
```
In-memory adapter cho test:
```java
final class InMemoryOrders implements LoadOrderPort, SaveOrderPort {
    final Map<OrderId, Order> store = new HashMap<>();
    public Optional<Order> load(OrderId id) { return Optional.ofNullable(store.get(id)); }
    public void save(Order o) { store.put(o.id(), o); }
}
```

</details>

---

<a id="p10"></a>
## 10. Domain-Driven Design & CQRS

### 10.1 DDD tactical — các khối xây dựng
| Khái niệm | Định nghĩa | Ví dụ Java |
|---|---|---|
| **Entity** | Có **định danh** (identity) xuyên suốt vòng đời; hai entity bằng nhau nếu cùng ID dù thuộc tính khác | `Order`, `Customer` — `equals` theo ID |
| **Value Object** | Không có định danh, xác định bởi **giá trị**; **immutable**; tự validate | `Money`, `Email`, `Address`, `DateRange` — `record` là lựa chọn tự nhiên |
| **Aggregate** | Cụm entity + value object được coi là **một đơn vị nhất quán**; truy cập qua **aggregate root** | `Order` (root) chứa `OrderLine`s |
| **Repository** | Ảo giác "collection trong bộ nhớ" của aggregate; **một repository cho mỗi aggregate root** | `OrderRepository.findById/save` |
| **Domain Service** | Logic nghiệp vụ không thuộc tự nhiên về một entity nào | `TransferService.transfer(from, to, money)` |
| **Domain Event** | Sự kiện có ý nghĩa nghiệp vụ đã xảy ra, đặt tên ở **thì quá khứ** | `OrderPlaced`, `PaymentFailed` |
| **Factory** | Đóng gói việc tạo aggregate phức tạp | `Order.place(customer, cart, clock)` |

**Quy tắc thiết kế aggregate** (Vaughn Vernon, "Effective Aggregate Design"):
1. Bảo vệ **invariant thực sự** bên trong ranh giới aggregate (ví dụ "tổng tiền đơn không vượt hạn mức", "không quá 50 dòng").
2. Thiết kế aggregate **nhỏ**.
3. Tham chiếu aggregate khác **bằng ID**, không bằng object reference (`CustomerId customerId` thay vì `Customer customer`).
4. **Một transaction chỉ sửa một aggregate**; nhất quán giữa các aggregate là **eventual consistency** qua domain event.

```java
import java.time.Instant;
import java.util.*;

record OrderId(UUID value) { static OrderId newId() { return new OrderId(UUID.randomUUID()); } }
record CustomerId(String value) {}
record ProductId(String value) {}
record Money(long amountVnd) {                           // value object (rút gọn)
    Money { if (amountVnd < 0) throw new IllegalArgumentException("negative"); }
    Money plus(Money o) { return new Money(amountVnd + o.amountVnd); }
    Money times(int q) { return new Money(Math.multiplyExact(amountVnd, q)); }
}

sealed interface OrderEvent permits OrderPlaced, OrderLineAdded {}
record OrderPlaced(OrderId orderId, CustomerId customerId, Money total, Instant at) implements OrderEvent {}
record OrderLineAdded(OrderId orderId, ProductId productId, int qty) implements OrderEvent {}

final class OrderLine {                                   // entity bên trong aggregate (không có repository riêng)
    private final ProductId productId; private int qty; private final Money unitPrice;
    OrderLine(ProductId p, int q, Money price) { productId = p; qty = q; unitPrice = price; }
    Money subtotal() { return unitPrice.times(qty); }
    void increase(int more) { qty += more; }
    ProductId productId() { return productId; }
}

final class Order {                                       // aggregate root
    private static final int MAX_LINES = 50;
    private static final Money MAX_TOTAL = new Money(500_000_000);

    private final OrderId id;
    private final CustomerId customerId;                  // tham chiếu aggregate khác bằng ID
    private final Map<ProductId, OrderLine> lines = new LinkedHashMap<>();
    private boolean placed;
    private final List<OrderEvent> pendingEvents = new ArrayList<>();

    private Order(OrderId id, CustomerId customerId) { this.id = id; this.customerId = customerId; }
    static Order draft(CustomerId customerId) { return new Order(OrderId.newId(), customerId); }   // factory

    void addLine(ProductId product, int qty, Money unitPrice) {
        if (placed) throw new IllegalStateException("Order already placed");
        if (qty <= 0) throw new IllegalArgumentException("qty must be > 0");
        OrderLine line = lines.get(product);
        if (line == null) {
            if (lines.size() >= MAX_LINES) throw new IllegalStateException("Too many lines");
            lines.put(product, new OrderLine(product, qty, unitPrice));
        } else {
            line.increase(qty);
        }
        if (total().amountVnd() > MAX_TOTAL.amountVnd()) throw new IllegalStateException("Exceeds order limit"); // invariant
        pendingEvents.add(new OrderLineAdded(id, product, qty));
    }

    void place(java.time.Clock clock) {
        if (lines.isEmpty()) throw new IllegalStateException("Empty order");
        if (placed) return;                               // idempotent
        placed = true;
        pendingEvents.add(new OrderPlaced(id, customerId, total(), clock.instant()));
    }

    Money total() { return lines.values().stream().map(OrderLine::subtotal).reduce(new Money(0), Money::plus); }

    /** Repository/application service lấy event ra để publish sau khi lưu (thường qua outbox) */
    List<OrderEvent> pullEvents() { var e = List.copyOf(pendingEvents); pendingEvents.clear(); return e; }

    @Override public boolean equals(Object o) { return o instanceof Order other && id.equals(other.id); } // entity: theo ID
    @Override public int hashCode() { return id.hashCode(); }
}
```
Lưu ý khi dùng JPA cho aggregate: entity JPA cần constructor không tham số, field không `final`; `equals/hashCode` theo ID có vấn đề khi ID sinh bởi DB (null trước khi persist) → dùng ID do ứng dụng sinh (UUID/ULID) hoặc natural key; Spring Data hỗ trợ domain event qua `@DomainEvents`/`AbstractAggregateRoot.registerEvent()`.

### 10.2 DDD strategic — bounded context
- **Ubiquitous Language**: ngôn ngữ chung giữa dev và chuyên gia nghiệp vụ, xuất hiện *nguyên văn* trong code.
- **Bounded Context**: ranh giới mà trong đó một model và ngôn ngữ có ý nghĩa nhất quán. Cùng từ "Product" mang nghĩa khác nhau ở *Catalog* (mô tả, hình ảnh), *Inventory* (số lượng tồn, kho), *Pricing* (giá, khuyến mãi), *Shipping* (kích thước, khối lượng). Đừng cố tạo một class `Product` khổng lồ phục vụ tất cả.
- **Context Map**: quan hệ giữa các context — *Shared Kernel*, *Customer/Supplier*, *Conformist*, **Anti-Corruption Layer (ACL)** (lớp dịch để model của bên ngoài/legacy không "làm bẩn" model của ta), *Open Host Service / Published Language*, *Separate Ways*.
- Bounded context là **ứng viên tự nhiên cho ranh giới microservice** (Newman, *Building Microservices*): high cohesion bên trong, loose coupling giữa các context; nhưng một context có thể gồm nhiều service, và bắt đầu bằng **modular monolith** theo context thường an toàn hơn tách microservice sớm.
- Subdomain: **core** (lợi thế cạnh tranh — đầu tư DDD), **supporting**, **generic** (mua/dùng sẵn: auth, email).

### 10.3 CQRS (Command Query Responsibility Segregation) — giới thiệu
Tách **mô hình ghi** (command: thay đổi trạng thái, đi qua aggregate, bảo vệ invariant) khỏi **mô hình đọc** (query: tối ưu cho hiển thị, có thể denormalize, không đi qua domain model).

```
           Command side                                   Query side
POST /orders ─► CommandHandler ─► Order aggregate ─► DB ghi (chuẩn hóa)
                                        │ domain event (outbox → Kafka)
                                        ▼
                               Projector cập nhật ──► Read model (bảng phẳng, Elasticsearch, Redis)
GET /orders?customer=… ◄────────────────────────────── Query handler (SQL đơn giản / search)
```
- **Mức nhẹ**: cùng DB, nhưng phía đọc dùng query/projection riêng (JPQL DTO projection, jOOQ, view) thay vì load aggregate rồi map — đã mang lại nhiều lợi ích mà ít chi phí.
- **Mức đầy đủ**: kho đọc riêng, cập nhật bất đồng bộ → **eventual consistency** (người dùng có thể không thấy ngay dữ liệu vừa ghi — cần xử lý UX: read-your-writes, trả về dữ liệu từ command response).
- Thường đi kèm (nhưng **không bắt buộc**) **Event Sourcing**: lưu chuỗi sự kiện thay cho trạng thái hiện tại.
- Fowler cảnh báo: CQRS thêm độ phức tạp đáng kể; chỉ áp dụng cho *một phần* hệ thống nơi tải đọc/ghi hoặc mô hình đọc/ghi khác nhau rõ rệt.

> 💡 **Góc nhìn Senior:** Ranh giới aggregate là quyết định quan trọng nhất của DDD tactical: quá lớn → contention (nhiều người cùng sửa một aggregate → optimistic lock fail liên tục), load chậm; quá nhỏ → invariant phải kiểm tra xuyên aggregate. Câu hỏi để chốt ranh giới: "*invariant nào phải đúng ngay lập tức (trong cùng transaction), và cái nào chấp nhận đúng sau vài giây?*".

> ⚠️ **Lỗi thường gặp:** Áp DDD tactical cho mọi CRUD; aggregate tham chiếu object của aggregate khác và sửa cả hai trong một transaction; dùng chung một model `Customer` cho mọi microservice qua thư viện chung (phá bounded context).

### 🛠 Bài tập phần 10

**Bài 10.1 — Entity hay Value Object? (Cơ bản)**
- Đề bài: Phân loại và giải thích: `Address` trong đơn hàng, `Address` trong hệ thống quản lý bưu cục, `Money`, `BankAccount`, `Seat` trong rạp phim (đặt vé theo ghế cụ thể) vs vé sự kiện đứng (không số ghế), `Email`.
- Tiêu chí đạt: cùng một khái niệm có thể là entity ở context này và value object ở context khác — giải thích được.

**Bài 10.2 — Aggregate Order hoàn chỉnh (Trung bình)**
- Đề bài: Mở rộng `Order` ở 10.1: thêm `removeLine`, `cancel` (chỉ khi chưa giao), áp mã giảm giá (value object `Coupon` có hạn dùng); viết `OrderRepository` in-memory và JPA; publish event qua Spring `ApplicationEventPublisher` sau khi lưu.
- Tiêu chí đạt: test mọi invariant; không có setter public; event được publish đúng một lần sau commit (`@TransactionalEventListener`).

**Bài 10.3 — Context map & CQRS nhẹ (Nâng cao)**
- Đề bài: Cho hệ thống thương mại điện tử: vẽ context map với ≥ 5 bounded context (Catalog, Ordering, Inventory, Payment, Shipping, Identity), chỉ rõ quan hệ (ACL, Customer/Supplier...). Sau đó hiện thực CQRS nhẹ cho "Lịch sử đơn hàng của khách": phía ghi dùng aggregate `Order`, phía đọc là bảng `order_summary` được cập nhật bởi listener của `OrderPlaced`.
- Tiêu chí đạt: sơ đồ có giải thích; query đọc không join bảng của aggregate; xử lý trường hợp event đến hai lần (idempotent projector).

<details>
<summary>Gợi ý lời giải</summary>

Bài 10.1: `Address` trong đơn hàng → value object (đổi địa chỉ = thay giá trị); trong hệ thống bưu cục, một địa điểm có thể là entity (có mã, lịch sử). `Seat` có số cụ thể → entity (được đặt riêng lẻ); vé đứng → chỉ đếm số lượng (value). `BankAccount` → entity (số tài khoản, số dư thay đổi theo thời gian).

Bài 10.3 — projector idempotent:
```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
void on(OrderPlaced e) {
    jdbc.update("""
        INSERT INTO order_summary(order_id, customer_id, total, placed_at) VALUES (?,?,?,?)
        ON CONFLICT (order_id) DO NOTHING""",              // PostgreSQL; MySQL: INSERT IGNORE / ON DUPLICATE KEY
        e.orderId().value(), e.customerId().value(), e.total().amountVnd(), Timestamp.from(e.at()));
}
```
Lưu ý: listener AFTER_COMMIT chạy sau commit nên nếu app crash giữa chừng event bị mất — production dùng outbox + message broker.

</details>

---

<a id="p11"></a>
## 11. Nguyên tắc thiết kế API

API ở đây gồm cả **Java API** (public class/method của thư viện, module) và **HTTP API** (REST). Cả hai chung một nguyên tắc: API là **hợp đồng** — dễ thêm, rất khó bỏ (Joshua Bloch: *"public APIs, like diamonds, are forever"*).

### 11.1 Java API
- **Tối thiểu hóa bề mặt public** (Effective Java Item 15): mặc định private/package-private; `final` class nếu không thiết kế cho kế thừa; JPMS `exports` chỉ package API.
- **Immutable khi có thể** (Item 17); trả về interface (`List`, không phải `ArrayList` — Item 64); trả collection rỗng chứ không `null` (Item 54); `Optional` cho giá trị trả về có thể vắng.
- **Kiểm tra tham số sớm** (Item 49): `Objects.requireNonNull`, `IllegalArgumentException` với thông điệp rõ.
- **Defensive copy** khi nhận/trả object mutable (Item 50).
- Tránh danh sách tham số dài, cùng kiểu đứng cạnh nhau (`transfer(String from, String to)` dễ đảo nhầm → dùng kiểu riêng `AccountId`); cẩn thận overloading gây mơ hồ (Item 52).
- **Tài liệu hóa hợp đồng**: Javadoc cho precondition, postcondition, exception, thread-safety, null-handling.
- **Tiến hóa tương thích ngược**: thêm method vào interface dùng `default`; đánh `@Deprecated(since = "2.3", forRemoval = true)` trước khi xóa; tuân thủ semantic versioning; kiểm tra bằng công cụ so sánh binary compatibility (japicmp, Revapi).

### 11.2 HTTP/REST API
- **Resource-oriented**: danh từ số nhiều (`/orders/{id}/items`), động từ là HTTP method. Hành động không phải CRUD: sub-resource (`POST /orders/{id}/cancellation`) hoặc custom method (`POST /orders/{id}:cancel` theo Google AIP).
- **Ngữ nghĩa HTTP method**: `GET` (safe, idempotent), `PUT` (idempotent — thay thế toàn bộ), `DELETE` (idempotent), `PATCH` (cập nhật một phần; không bảo đảm idempotent), `POST` (không idempotent → dùng header **`Idempotency-Key`** cho thao tác như thanh toán).
- **Status code đúng**: 200/201 (+ `Location`)/202 (xử lý bất đồng bộ)/204; 400 (dữ liệu sai cú pháp/validation), 401 (chưa xác thực) vs 403 (không có quyền), 404, 409 (xung đột trạng thái/phiên bản), 412 (precondition — `If-Match` sai ETag), 422 (đúng cú pháp nhưng vi phạm nghiệp vụ), 429 (rate limit + `Retry-After`), 500/502/503/504.
- **Định dạng lỗi chuẩn**: **RFC 9457 Problem Details** (thay RFC 7807) — Spring 6 hỗ trợ sẵn `ProblemDetail`/`ErrorResponse`:
  ```json
  { "type": "https://api.acme.vn/problems/insufficient-funds", "title": "Insufficient funds",
    "status": 422, "detail": "Balance 30000 VND is less than 50000 VND", "instance": "/payments/9f1c",
    "traceId": "4bf92f3577b34da6" }
  ```
- **Pagination**: offset/limit đơn giản nhưng chậm và trùng/sót dữ liệu khi dữ liệu thay đổi; **cursor/keyset pagination** (`?after=<opaque cursor>&limit=50`) cho dữ liệu lớn. Luôn đặt giới hạn `limit` tối đa.
- **Filtering/sorting** nhất quán (`?status=PAID&sort=-createdAt`), **field naming** nhất quán (camelCase hoặc snake_case — chọn một), thời gian theo ISO-8601 có timezone (UTC), tiền tệ dùng số nguyên đơn vị nhỏ nhất hoặc string decimal + mã tiền tệ (không dùng `double`).
- **Versioning**: URI (`/v1/...`), header, hoặc media type. Tốt nhất là **tiến hóa không phá vỡ**: chỉ *thêm* field tùy chọn; client phải bỏ qua field lạ (tolerant reader); không đổi nghĩa field cũ; xóa field = version mới + thời gian deprecation (header `Deprecation`/`Sunset`).
- **Concurrency control**: `ETag` + `If-Match` (optimistic locking ở mức HTTP) để tránh lost update.
- **Bảo mật & vận hành**: không lộ ID tuần tự nhạy cảm (IDOR — luôn kiểm tra quyền trên resource), rate limit, correlation/trace id (`traceparent` của W3C Trace Context), timeout rõ ràng, tài liệu **OpenAPI** sinh tự động hoặc API-first.

> 💡 **Góc nhìn Senior:** Thiết kế API bắt đầu từ **use case của người gọi**, không từ schema DB (đừng expose entity JPA trực tiếp — lộ cấu trúc nội bộ, lazy loading lỗi khi serialize, vòng lặp JSON, không tiến hóa độc lập được). Viết ví dụ request/response và code phía client *trước* khi hiện thực. Với API nội bộ giữa các service, cân nhắc consumer-driven contract test (Spring Cloud Contract, Pact) để phát hiện breaking change trước khi deploy.

> ⚠️ **Lỗi thường gặp:** Trả 200 kèm `{"success": false}`; dùng `GET` cho thao tác có side effect; API thanh toán không idempotent nên client retry khi timeout → trừ tiền hai lần; trả `500` cho lỗi validation; stack trace trong response.

### 🛠 Bài tập phần 11

**Bài 11.1 — Review một API (Cơ bản)**
- Đề bài: Review danh sách endpoint sau và viết lại: `GET /getAllOrders`, `POST /order/delete?id=5`, `POST /updateOrderStatus` (body `{id, status}`), `GET /orders?page=1000000`, lỗi luôn trả 200 với `{"error": "..."}`.
- Tiêu chí đạt: bảng "trước → sau" với method, path, status code, lý do.

**Bài 11.2 — Idempotent payment API (Trung bình)**
- Đề bài: Hiện thực `POST /payments` với header `Idempotency-Key` bằng Spring Boot: lần gọi trùng key + cùng body trả lại đúng response cũ; trùng key khác body trả 422; hai request đồng thời cùng key chỉ một được xử lý.
- Tiêu chí đạt: lưu key với unique constraint trong DB; test đồng thời (2 thread); lỗi trả RFC 9457 `ProblemDetail`.

**Bài 11.3 — Tiến hóa API không phá vỡ (Nâng cao)**
- Đề bài: API v1 trả `{"name": "Nguyễn Văn A"}`; yêu cầu mới cần tách `firstName`/`lastName` và bỏ `name` sau 6 tháng. Thiết kế lộ trình (expand → migrate → contract), header deprecation, contract test cho client cũ, và tương tự cho một Java library (`getName()` → `getFirstName()/getLastName()`).
- Tiêu chí đạt: không có thời điểm nào client cũ bị vỡ; có kế hoạch đo lường client nào còn dùng field cũ (log/metric) trước khi xóa.

<details>
<summary>Gợi ý lời giải</summary>

Bài 11.1 (một phần): `GET /orders?cursor=…&limit=50`; `DELETE /orders/5` → 204; `PATCH /orders/5` body `{"status":"CANCELLED"}` hoặc `POST /orders/5/cancellation` → 200/409 nếu trạng thái không cho phép; lỗi → status 4xx/5xx + Problem Details.

Bài 11.2 — bảng `idempotency_keys(key PK, request_hash, status, response_body, created_at)`; trong transaction: `INSERT ... ` (bắt lỗi duplicate key → đọc bản ghi cũ: nếu `request_hash` khác → 422; nếu đang `IN_PROGRESS` → 409 hoặc chờ; nếu `DONE` → trả response cũ). Đặt TTL dọn key cũ (ví dụ 24h).

</details>

---

<a id="p12"></a>
## 12. Code review với tư cách Senior

### 12.1 Mục tiêu của code review
Theo Google Engineering Practices: chuẩn mực là **"approve khi thay đổi cải thiện sức khỏe tổng thể của codebase, dù chưa hoàn hảo"** — không có code hoàn hảo, chỉ có code *tốt hơn*. Mục tiêu: phát hiện lỗi sớm, chia sẻ tri thức (bus factor), giữ nhất quán thiết kế, mentoring. Không phải nơi để thể hiện hay "gác cổng" cá nhân.

### 12.2 Xem gì, theo thứ tự ưu tiên
1. **Thiết kế & tính đúng**: thay đổi có giải quyết đúng vấn đề không? Có đặt đúng chỗ (module/layer) không? Có cách đơn giản hơn không?
2. **Tính đúng ở biên**: null, rỗng, overflow, timezone, encoding, số âm, phân trang, input cực lớn.
3. **Concurrency**: shared mutable state, race condition, check-then-act, lock ordering, `ThreadLocal` không remove, `@Async`/`CompletableFuture` mất context, thread pool không giới hạn.
4. **Transaction & dữ liệu**: ranh giới `@Transactional`, self-invocation, gọi HTTP bên trong transaction (giữ connection DB lâu), N+1 query, thiếu index cho query mới, migration có tương thích ngược (expand/contract) để rolling deploy không lỗi.
5. **Lỗi & độ bền**: exception bị nuốt, retry không idempotent, thiếu timeout khi gọi ra ngoài, circuit breaker, log có đủ ngữ cảnh (và không log PII/secret).
6. **Bảo mật**: SQL/command injection, kiểm tra quyền trên resource (IDOR), validate input, secret trong code, deserialization không an toàn, dependency có CVE.
7. **Hiệu năng**: độ phức tạp thuật toán trên đường nóng, allocation trong vòng lặp, cache không giới hạn (Module 05), query trong vòng lặp.
8. **Test**: test hành vi (không phải hiện thực), có test cho case lỗi và biên, test không flaky (không `sleep`, không phụ thuộc thời gian thật — inject `Clock`).
9. **Khả năng đọc**: tên, cấu trúc, comment "tại sao", nhất quán với codebase.
10. **Vận hành**: metric/log/trace cho tính năng mới, feature flag, kế hoạch rollback, tài liệu/ADR nếu là quyết định kiến trúc.

Những gì **nên tự động hóa** thay vì review bằng người: format (Spotless/google-java-format), lint (Checkstyle, PMD, Error Prone, SpotBugs), coverage, dependency scan (OWASP Dependency-Check, Dependabot/Renovate), ArchUnit, SonarQube quality gate.

### 12.3 Cách viết comment review
- **Phân loại mức độ** rõ ràng: `blocking:` / `suggestion:` / `nit:` / `question:` / `praise:` (theo "Conventional Comments").
- **Hỏi thay vì ra lệnh**, nói về *code* không nói về *người*: "Hàm này có thể được gọi đồng thời từ hai consumer không? Nếu có thì `counter++` sẽ mất cập nhật" thay vì "Bạn viết sai concurrency rồi".
- **Giải thích lý do + đưa ví dụ/link**, đề xuất cụ thể (đoạn code gợi ý).
- Khen điều làm tốt — review cũng là phản hồi tích cực.
- Thảo luận dài > 2 vòng → gọi trao đổi trực tiếp, rồi ghi kết luận vào PR.

```text
blocking: `chargeCustomer()` gọi payment gateway bên trong @Transactional.
Nếu gateway chậm 30s, connection DB bị giữ 30s → pool (10 connection) cạn khi có 10 request đồng thời.
Đề xuất: tách thành (1) tx lưu Order(PENDING) → (2) gọi gateway ngoài tx với timeout 3s
→ (3) tx cập nhật trạng thái. Kèm Idempotency-Key để retry an toàn.

nit: `List<Order> list` → `pendingOrders` cho rõ nghĩa.

praise: test case cho race condition dùng CountDownLatch rất hay 👍
```

### 12.4 Vai trò Senior trong quy trình review
- **Giữ PR nhỏ** (lý tưởng < 400 dòng thay đổi), mô tả PR có: *bối cảnh, thay đổi gì, vì sao, cách test, rủi ro, ảnh chụp/log*. Tách refactor khỏi thay đổi hành vi.
- **Thời gian phản hồi** nhanh (trong vòng một ngày làm việc) — review chậm làm giảm throughput của cả team hơn là review chưa kỹ.
- Với junior: ưu tiên vài điểm quan trọng nhất, giải thích nguyên tắc đằng sau, có thể pair review; đừng để 40 comment `nit`.
- Khi *được* review: không phòng thủ, cảm ơn, giải thích quyết định bằng dữ liệu/trade-off; nếu reviewer hỏi "tại sao", thường nghĩa là code/comment cần rõ hơn.
- Biến các comment lặp lại thành **checklist**, rule lint, ArchUnit test, hoặc tài liệu hướng dẫn — giảm tải cho review sau.

> 💡 **Góc nhìn Senior:** Câu hỏi phỏng vấn "Bạn review code thế nào?" muốn nghe: thứ tự ưu tiên (thiết kế/đúng/an toàn trước, style sau, style giao cho tool), cách giao tiếp tôn trọng, cân bằng giữa chất lượng và tốc độ giao hàng, và ví dụ một lần review của bạn đã ngăn được sự cố production (cụ thể: race condition, N+1, migration khóa bảng...).

> ⚠️ **Lỗi thường gặp:** "LGTM" trong 30 giây cho PR 2.000 dòng; tranh cãi style mà tool đã quyết; chặn PR vì sở thích cá nhân không có lý do kỹ thuật; review chỉ diff mà không đọc ngữ cảnh xung quanh (code gọi tới, migration, cấu hình).

### 🛠 Bài tập phần 12

**Bài 12.1 — Review một đoạn code (Cơ bản)**
- Đề bài: Review đoạn sau và viết comment theo định dạng Conventional Comments:
  ```java
  @Service
  public class CouponService {
      private static Map<String, Integer> usage = new HashMap<>();
      @Autowired private CouponRepository repo;

      @Transactional
      public boolean use(String code, String userId) {
          try {
              Coupon c = repo.findByCode(code);
              if (c.getExpiry().before(new Date())) return false;
              if (usage.getOrDefault(code, 0) >= c.getLimit()) return false;
              usage.put(code, usage.getOrDefault(code, 0) + 1);
              notifyMarketing(code, userId);   // gọi HTTP 2-5 giây
              return true;
          } catch (Exception e) { return false; }
      }
      private void notifyMarketing(String code, String userId) { /* RestTemplate call */ }
  }
  ```
- Tiêu chí đạt: tìm được ít nhất 8 vấn đề (gợi ý: thread-safety, trạng thái trong bộ nhớ khi chạy nhiều instance, check-then-act, NPE, nuốt exception, HTTP trong transaction, field injection, `Date`/thời gian không test được, return boolean mất lý do...), phân loại mức độ đúng.

**Bài 12.2 — Viết lại sau review (Trung bình)**
- Đề bài: Viết lại `CouponService` đã sửa mọi vấn đề blocking: đếm lượt dùng bằng DB (atomic update `UPDATE coupon SET used = used + 1 WHERE code = ? AND used < limit`), `Clock` inject, kết quả trả về là sealed type nêu lý do, gọi marketing qua event sau commit.
- Tiêu chí đạt: test đồng thời 100 thread với limit 10 → đúng 10 lần thành công; không gọi HTTP trong transaction.

**Bài 12.3 — Review checklist của team (Nâng cao)**
- Đề bài: Soạn checklist review 1 trang cho team Java/Spring của bạn, chia mục theo 12.2, kèm cấu hình tool tự động hóa (Spotless, Error Prone/SpotBugs, ArchUnit, OWASP) trong `pom.xml`/`build.gradle` và template mô tả PR.
- Tiêu chí đạt: checklist cụ thể (không chung chung kiểu "code phải sạch"); pipeline CI fail khi vi phạm rule tự động.

<details>
<summary>Gợi ý lời giải</summary>

Bài 12.1 — các vấn đề chính:
1. `static HashMap` dùng đồng thời → không thread-safe (blocking).
2. State trong bộ nhớ → sai khi chạy nhiều instance/restart (blocking): phải lưu DB.
3. Check-then-act (`getOrDefault` rồi `put`) → vượt limit dưới tải (blocking).
4. `repo.findByCode` có thể trả null → NPE, bị nuốt bởi `catch` → trả `false` sai nghĩa.
5. `catch (Exception e) { return false; }` nuốt mọi lỗi, không log (blocking).
6. Gọi HTTP 2–5s trong `@Transactional` → giữ connection DB; lỗi HTTP làm "dùng coupon" thất bại dù không liên quan (blocking).
7. Field injection → khó test; dùng constructor injection.
8. `new Date()` → không test được; dùng `Clock` + `java.time`.
9. Trả `boolean` → caller không biết lý do (hết hạn? hết lượt? không tồn tại?).
10. Không có idempotency: user bấm hai lần dùng hai lượt.

Bài 12.2 — then chốt:
```java
@Modifying
@Query("update Coupon c set c.used = c.used + 1 where c.code = :code and c.used < c.limit and c.expiry > :now")
int tryConsume(@Param("code") String code, @Param("now") Instant now);   // 1 = thành công, 0 = hết lượt/hết hạn
```
Câu `UPDATE` có điều kiện là atomic ở mức DB (row lock), đúng với nhiều instance.

</details>

---

<a id="du-an-mini"></a>
## Dự án mini

### "Mini Food Delivery — Ordering Context"

**Mục tiêu:** xây dựng một service đặt món (bounded context *Ordering*) áp dụng có chủ đích các nguyên tắc và pattern trong module, *và giải thích được vì sao chọn/không chọn từng pattern*.

**Yêu cầu chức năng**
1. Khách tạo giỏ hàng từ thực đơn của một nhà hàng, thêm/bớt món, áp mã giảm giá; đặt đơn (`Order` aggregate với invariant: một nhà hàng/đơn, tối đa 30 món, tổng ≤ 5 triệu VND, không đặt khi nhà hàng đóng cửa).
2. Vòng đời đơn: `PLACED → ACCEPTED → PREPARING → DELIVERING → DELIVERED`, cùng `CANCELLED`/`REJECTED` theo quy tắc (dùng **State**).
3. Tính phí giao hàng theo nhiều chiến lược (khoảng cách, giờ cao điểm, thành viên VIP) — **Strategy** + **Composite** (kết hợp quy tắc khuyến mãi).
4. Thanh toán qua 2 cổng giả lập có API khác nhau — **Adapter**, bọc thêm retry/timing — **Decorator**; API `POST /orders/{id}/payment` **idempotent**.
5. Khi đơn đổi trạng thái, phát **domain event**; các listener (thông báo, tích điểm, analytics) phản ứng sau commit — **Observer**.
6. Pipeline xử lý request gồm xác thực giả lập, rate limit, logging — **Chain of Responsibility** (Servlet filter hoặc tự xây).
7. Màn hình "Đơn hàng của tôi" dùng **read model** riêng (CQRS nhẹ).

**Yêu cầu phi chức năng**
- Kiến trúc **hexagonal**: module `core` không phụ thuộc Spring/JPA (kiểm tra bằng build và **ArchUnit**); package-by-feature.
- Java 21 (record, sealed, pattern matching switch), Spring Boot 3.x, H2/PostgreSQL (Testcontainers cho integration test).
- Unit test core ≥ 85% line coverage, chạy < 5 giây; integration test cho mỗi adapter; một test đồng thời cho idempotency.
- API lỗi theo RFC 9457; OpenAPI được sinh; README có sơ đồ kiến trúc và **ít nhất 3 ADR** (ví dụ: hexagonal vs layered; State bằng enum vs thư viện; CQRS nhẹ vs đầy đủ).
- Mỗi pattern dùng phải có ghi chú ngắn trong README: *vấn đề → vì sao pattern này → phương án thay thế đã cân nhắc*. Có ít nhất **một chỗ bạn cố ý KHÔNG dùng pattern** và giải thích (YAGNI).
- Lịch sử commit sạch: các PR/commit nhỏ, có ít nhất một commit thuần refactor.

**Tiêu chí chấm (100 điểm)**
| Hạng mục | Điểm |
|---|---|
| Domain model: aggregate, value object, invariant được bảo vệ, event | 20 |
| Pattern dùng đúng chỗ, có giải thích trade-off (kể cả chỗ không dùng) | 20 |
| Kiến trúc hexagonal + ranh giới được kiểm tra tự động | 15 |
| Thiết kế API (REST đúng ngữ nghĩa, idempotency, lỗi chuẩn, phân trang) | 15 |
| Chất lượng test (unit nhanh, integration, concurrency) | 15 |
| Clean code (tên, hàm, xử lý lỗi), ADR và README | 10 |
| Tự review: một file `SELF_REVIEW.md` liệt kê 5 điểm yếu còn lại và hướng cải thiện | 5 |

---

<a id="checklist-tu-danh-gia"></a>
## Checklist tự đánh giá
- [ ] Tôi áp dụng được các quy tắc đặt tên, viết hàm, comment và xử lý exception của Clean Code, và biết khi nào không nên áp dụng máy móc.
- [ ] Tôi gọi đúng tên ≥ 10 code smell và refactoring tương ứng, và refactor an toàn với test bảo vệ.
- [ ] Tôi giải thích từng nguyên tắc SOLID bằng ví dụ vi phạm → hậu quả → cách sửa, và phân biệt DIP, IoC, DI.
- [ ] Tôi giải thích DRY (về tri thức, không phải văn bản), KISS, YAGNI, composition over inheritance (fragile base class), Law of Demeter, cohesion/coupling.
- [ ] Tôi viết được 6 biến thể Singleton, giải thích vì sao DCL cần `volatile`, vì sao enum singleton an toàn, và khác biệt với singleton scope của Spring.
- [ ] Tôi phân biệt GoF Factory Method với static factory method, Abstract Factory, và viết Builder có validate + immutable.
- [ ] Tôi giải thích được Prototype và vì sao nên tránh `clone()`; phân biệt với prototype scope của Spring.
- [ ] Tôi phân biệt Adapter, Decorator, Proxy, Facade, Composite và chỉ ra ví dụ trong JDK/Spring.
- [ ] Tôi giải thích được JDK dynamic proxy vs CGLIB, Spring AOP chọn loại nào, và mọi trường hợp `@Transactional` không có tác dụng (self-invocation...).
- [ ] Tôi hiện thực được Strategy, Template Method, Observer, Chain of Responsibility, Command (undo/redo), State, Iterator theo phong cách Java hiện đại, và phân biệt State vs Strategy, Template Method vs Strategy.
- [ ] Tôi nhận diện được God object, anemic domain model (và trình bày được cả hai phía tranh luận), service locator và các anti-pattern phổ biến khác.
- [ ] Tôi so sánh được layered, hexagonal, clean architecture và chọn theo bối cảnh; ép được ranh giới bằng ArchUnit/module.
- [ ] Tôi thiết kế được aggregate theo 4 quy tắc của Vernon, phân biệt entity và value object, dùng domain event; giải thích bounded context, context map, ACL và CQRS (khi nào không nên dùng).
- [ ] Tôi thiết kế được Java API và REST API: tối thiểu bề mặt, immutable, status code đúng, idempotency, RFC 9457, phân trang cursor, tiến hóa không phá vỡ.
- [ ] Tôi review code theo thứ tự ưu tiên rõ ràng, viết comment mang tính xây dựng có phân loại mức độ, và tự động hóa những gì có thể.

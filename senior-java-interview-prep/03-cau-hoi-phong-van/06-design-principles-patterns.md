# Câu hỏi phỏng vấn — Module 06: Clean Code, SOLID, Design Patterns & Architecture

> Giáo trình tương ứng: [Module 06 — Clean Code, SOLID, Design Patterns & Architecture](../01-giao-trinh/06-design-principles-patterns.md)

**Cách dùng:** đọc câu hỏi, **tự trả lời thành tiếng** (như đang ngồi trước interviewer) trong 1–2 phút, *rồi* mới mở "Đáp án". So sánh với phần **Trả lời ngắn** (ý phải có trong 30 giây đầu), sau đó đọc **Giải thích chi tiết** và tự trả lời luôn các **Câu hỏi nối tiếp**. Mục **⚠️ Câu trả lời gây điểm trừ** liệt kê những câu nói khiến interviewer đánh giá bạn chưa tới level Senior — tránh nói như vậy.

**Mức độ:** 🟢 Cơ bản (phải trả lời trôi chảy) · 🟡 Senior (trả lời kèm trade-off, kinh nghiệm thật) · 🔴 Xoáy sâu (interviewer đào tới cơ chế/edge case). Câu có nhãn **[Tình huống]** là câu "bạn sẽ làm gì"; **[Đọc code]** là câu "đoạn code sau có vấn đề gì / in ra gì".

Tổng: **58 câu** — 16 🟢 · 26 🟡 · 16 🔴.

## Mục lục

| Nhóm | Chủ đề | Câu hỏi |
|---|---|---|
| [A](#nhom-a) | Clean Code, xử lý lỗi, code smell & refactoring | Q1–Q7 |
| [B](#nhom-b) | SOLID | Q8–Q14 |
| [C](#nhom-c) | DRY/KISS/YAGNI, composition, Law of Demeter, cohesion/coupling | Q15–Q19 |
| [D](#nhom-d) | Creational patterns | Q20–Q27 |
| [E](#nhom-e) | Structural patterns & Spring AOP proxy | Q28–Q35 |
| [F](#nhom-f) | Behavioral patterns | Q36–Q43 |
| [G](#nhom-g) | Anti-patterns | Q44–Q46 |
| [H](#nhom-h) | Kiến trúc: layered, hexagonal, clean | Q47–Q49 |
| [I](#nhom-i) | DDD & CQRS | Q50–Q54 |
| [J](#nhom-j) | Thiết kế API & code review | Q55–Q58 |

---

<a id="nhom-a"></a>
## A. Clean Code, xử lý lỗi, code smell & refactoring

### Q1. 🟢 Với bạn "clean code" nghĩa là gì? Làm sao biết code của team có "sạch" hay không?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Clean code là code có **chi phí thay đổi thấp**: người khác (hoặc chính mình 6 tháng sau) đọc hiểu nhanh và sửa an toàn. Cụ thể: tên thể hiện ý định, hàm nhỏ làm một việc ở một mức trừu tượng, không side effect ẩn, xử lý lỗi rõ ràng, có test bảo vệ. Đo bằng hệ quả: lead time thay đổi, số bug quay lại ở một module, thời gian onboard — không đo bằng "đẹp".

**Giải thích chi tiết:**
- Code được *đọc* nhiều hơn *viết* hàng chục lần → tối ưu cho người đọc. Các quy tắc cụ thể (Clean Code ch.2–7): tên đọc như câu (`isExpired`, `elapsedDays`), boolean flag argument là dấu hiệu hàm làm hai việc, Command–Query Separation, trả collection rỗng/`Optional` thay vì `null`, comment giải thích *tại sao* chứ không lặp lại *cái gì*.
- Ví dụ refactor điển hình: hàm `calc(Order o, boolean vip)` vừa cộng tiền, vừa giảm giá, vừa tính thuế → tách thành `totalPayable(order, customer)` gọi `discountPolicy.apply(...)`, `taxPolicy.applyTo(...)`, dùng `Money`/`BigDecimal` cho tiền.
- Senior không áp dụng máy móc: quy tắc "hàm ≤ 4 dòng" có thể tạo hàng chục hàm nông khiến người đọc phải nhảy khắp nơi (tranh luận "deep modules" của John Ousterhout). Tiêu chí cuối cùng vẫn là *hiểu và sửa an toàn*.
- Phần "thẩm mỹ" (format, import, naming convention) nên **tự động hóa** (Spotless, Checkstyle, Error Prone, Sonar) để review tập trung vào thiết kế.

**Câu hỏi nối tiếp:**
- *Comment thế nào là tốt?* → Comment giải thích quyết định nghiệp vụ, workaround kèm link ticket, cảnh báo hậu quả, Javadoc API public. Comment lặp lại code, code bị comment-out, nhật ký thay đổi (đã có git) là có hại; comment sai lệch còn tệ hơn không có.
- *Metric nào bạn từng dùng?* → Hotspot = churn cao × độ phức tạp cao (Adam Tornhill), cyclomatic complexity, tỉ lệ bug theo module, thời gian review PR.

**⚠️ Câu trả lời gây điểm trừ:**
- "Clean code là code đẹp, ngắn" — không gắn với chi phí thay đổi.
- Đọc thuộc lòng quy tắc của Uncle Bob mà không biết khi nào không nên áp dụng.
- "Code tốt không cần comment" một cách tuyệt đối.

**📖 Ôn lại:** [§1 Clean Code](../01-giao-trinh/06-design-principles-patterns.md#p1)

</details>

### Q2. 🟢 Checked vs unchecked exception: khi nào dùng loại nào? Vì sao Spring gần như chỉ dùng unchecked?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Checked exception dành cho tình huống caller **có thể phục hồi một cách hợp lý** và *nên* bị buộc xử lý (Effective Java Item 70); unchecked cho lỗi lập trình hoặc lỗi mà caller thường không làm gì được ngoài báo lên. Spring dùng unchecked (`DataAccessException`...) vì checked exception lan qua mọi tầng, buộc mọi chữ ký method thay đổi khi thêm một loại lỗi (phá Open/Closed) và không đi được qua lambda/stream.

**Giải thích chi tiết:**
- Checked exception là một phần của **hợp đồng** method: thêm `throws NewException` ở tầng thấp buộc sửa toàn bộ caller phía trên → coupling theo chiều dọc.
- Lambda/`Stream.map` không cho ném checked exception → code bọc `try/catch` xấu xí, hay bị nuốt lỗi.
- Quy tắc đi kèm: exception phải **phù hợp mức trừu tượng** (Item 73) — repository không ném `SQLException` lên service; bọc lại kèm *cause*:

```java
} catch (SQLException e) {
    throw new DataAccessFailure("Cannot load order id=" + id, e); // giữ cause + thêm ngữ cảnh
}
```

- Thiết kế phân cấp thực tế cho một service: `DomainException` (404/409/422 — lỗi nghiệp vụ, không retry) và `InfrastructureException` (503 — có thể retry), map sang HTTP ở một `@RestControllerAdvice` duy nhất.

**Câu hỏi nối tiếp:**
- *`InterruptedException` thì sao?* → Không được nuốt; nếu không ném tiếp được thì khôi phục cờ `Thread.currentThread().interrupt()`.
- *Có dùng exception cho control flow không?* → Không (Item 69): chậm (stack trace) và che ý định; dùng `Optional`/kết quả kiểu sealed.

**⚠️ Câu trả lời gây điểm trừ:**
- "Checked exception là tốt hơn vì an toàn hơn" mà không nói chi phí.
- Không biết Spring dịch exception (`@Repository` + `PersistenceExceptionTranslationPostProcessor`).

**📖 Ôn lại:** [§1.5 Xử lý lỗi](../01-giao-trinh/06-design-principles-patterns.md#p1)

</details>

### Q3. 🟡 [Đọc code] Đoạn xử lý lỗi sau có những vấn đề gì?

```java
public Order load(long id) {
    try {
        return repository.findById(id);
    } catch (Exception e) {
        log.error("Load order failed: " + e.getMessage());
        throw new ServiceException(e.getMessage());
    }
}
// Tầng controller lại: catch (ServiceException e) { log.error("Error", e); throw e; }
```

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** (1) Mất **cause** → mất stack trace gốc; (2) **log-and-rethrow** ở mọi tầng → một lỗi sinh nhiều dòng log trùng; (3) bắt `Exception` quá rộng (nuốt cả `InterruptedException`, lỗi lập trình); (4) log bằng string concatenation và không có ngữ cảnh (`id`); (5) message của exception hạ tầng có thể bị đẩy ra client.

**Giải thích chi tiết:**
- Viết đúng:

```java
public Order load(long id) {
    try {
        return repository.findById(id);
    } catch (DataAccessException e) {                       // bắt đúng loại
        throw new OrderLoadException("Cannot load order id=" + id, e); // cause + ngữ cảnh, KHÔNG log ở đây
    }
}
```

- Nguyên tắc: **log một lần ở biên** (`@RestControllerAdvice`, message listener, scheduled job) kèm correlation id; tầng dưới chỉ ném kèm ngữ cảnh.
- `log.error("...", e)` (truyền exception làm tham số cuối) để SLF4J in stack trace; dùng placeholder `{}`.
- Nếu đã nuốt `InterruptedException` → phải `Thread.currentThread().interrupt()`.

**Câu hỏi nối tiếp:**
- *Vì sao không log ở repository?* → Repository không biết lỗi có được xử lý (retry, fallback) ở tầng trên không; log ở dưới gây nhiễu và trùng lặp.
- *Message lỗi trả cho client nên lấy từ đâu?* → Từ error catalog/`ProblemDetail` được thiết kế, không phải `e.getMessage()` của SQL.

**⚠️ Câu trả lời gây điểm trừ:**
- Chỉ thấy "nên log stack trace" mà không thấy vấn đề log trùng và mất cause.
- Đề xuất `catch (Exception e) { return null; }`.

**📖 Ôn lại:** [§1.5 Xử lý lỗi & Lỗi thường gặp](../01-giao-trinh/06-design-principles-patterns.md#p1)

</details>

### Q4. 🟡 Có nên dùng `Optional` làm field, tham số method, hoặc phần tử trong collection không?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Không. `Optional` được thiết kế cho **giá trị trả về** có thể vắng. Làm field thì không `Serializable`, tốn thêm object, entity JPA không map được; làm tham số buộc caller bọc giá trị và vẫn có thể truyền `null` cho chính `Optional`; trong collection thì vô nghĩa — cứ bỏ phần tử vắng ra.

**Giải thích chi tiết:**
- Thay thế: field nullable + getter trả `Optional.ofNullable(field)`; tham số tùy chọn → overload hoặc parameter object/builder; collection → collection rỗng (Item 54).
- `Optional.get()` không kiểm tra là code smell — dùng `orElseThrow()`, `map`, `orElse`/`orElseGet` (lưu ý `orElse(expensive())` luôn gọi hàm, `orElseGet` thì lazy).
- Không trả `Optional<List<T>>` — trả list rỗng.

**Câu hỏi nối tiếp:**
- *Spring có hỗ trợ inject `Optional<T>` không?* → Có (dependency tùy chọn) — đây là ngữ cảnh framework, khác với dùng trong domain API; `ObjectProvider<T>` thường linh hoạt hơn.
- *Null Object pattern khác `Optional` thế nào?* → Null Object là implementation "không làm gì" của interface, caller không cần kiểm tra gì cả; hợp với strategy/listener mặc định.

**⚠️ Câu trả lời gây điểm trừ:** "Dùng `Optional` khắp nơi để tránh NPE" — cho thấy chưa hiểu mục đích thiết kế.

**📖 Ôn lại:** [§1.3–1.5 & Lỗi thường gặp](../01-giao-trinh/06-design-principles-patterns.md#p1)

</details>

### Q5. 🟢 Code smell là gì? Kể 5 smell bạn hay gặp và refactoring tương ứng.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Code smell là *triệu chứng* bề mặt gợi ý vấn đề thiết kế sâu hơn — không phải bug nhưng làm thay đổi tốn kém. Ví dụ: Long Method → Extract Method; God Class → Extract Class theo trách nhiệm; Primitive Obsession → Value Object (`Money`, `Email`); Switch lặp trên type code → Replace Conditional with Polymorphism (hoặc sealed + pattern matching); Feature Envy → Move Method.

**Giải thích chi tiết:**

| Smell | Dấu hiệu | Refactoring |
|---|---|---|
| Long Parameter List | ≥ 4 tham số, hay đi cùng nhau | Introduce Parameter Object |
| Data Clumps | `street, city, zip` lặp lại | Extract Class `Address` |
| Shotgun Surgery | 1 thay đổi sửa nhiều class | Move Method/Field, gom trách nhiệm |
| Divergent Change | 1 class bị sửa vì nhiều lý do | Extract Class (vi phạm SRP) |
| Message Chains | `a.getB().getC().doX()` | Hide Delegate (Law of Demeter) |
| Speculative Generality | interface "để sau" chỉ có 1 impl | Collapse Hierarchy (YAGNI) |
| Refused Bequest | subclass ném `UnsupportedOperationException` | Replace Inheritance with Delegation (LSP) |

- Refactoring = đổi cấu trúc **không đổi hành vi**, bước nhỏ, có test bảo vệ, dùng refactoring tự động của IDE.

**Câu hỏi nối tiếp:**
- *Shotgun Surgery và Divergent Change khác nhau thế nào?* → Ngược nhau: một thay đổi → nhiều class (trách nhiệm bị rải) vs một class → nhiều loại thay đổi (trách nhiệm bị gộp).
- *"Rule of three" là gì?* → Lặp lại lần thứ ba mới trừu tượng hóa, tránh abstraction sai.

**⚠️ Câu trả lời gây điểm trừ:** Gọi tên smell nhưng không nêu được refactoring cụ thể hoặc hậu quả khi requirement thay đổi.

**📖 Ôn lại:** [§2 Code smells & refactoring](../01-giao-trinh/06-design-principles-patterns.md#p2)

</details>

### Q6. 🟡 [Tình huống] Bạn nhận một module legacy 5.000 dòng không có test, business muốn thêm tính năng gấp. Bạn tiếp cận thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Không "viết lại" và không "refactor 3 tháng". Tôi (1) viết **characterization test** chụp lại hành vi hiện tại quanh vùng sẽ sửa; (2) "make the change easy, then make the easy change" — refactor nhỏ, đúng phạm vi cần cho tính năng; (3) nếu cần thay đổi lớn thì dùng **Branch by Abstraction / Strangler Fig** để luôn deploy được giữa chừng; (4) tách commit refactor và commit đổi hành vi.

**Giải thích chi tiết:**
- Characterization test (Michael Feathers): gọi code với input thực tế, assert output *hiện tại* — kể cả hành vi "sai" — để mọi thay đổi ngoài ý muốn đều bị phát hiện. Có thể dùng approval/snapshot test cho output lớn.
- Tìm *seam* để chèn test: tách dependency (DB, HTTP, `new Date()`) ra sau interface hoặc inject `Clock`.
- Branch by Abstraction: đưa interface vào, implementation cũ là adapter; viết implementation mới; chuyển caller dần (có thể bằng feature flag); xóa code cũ.
- Ưu tiên theo *hotspot* (churn × complexity): vùng hay bị sửa và phức tạp nhất đáng đầu tư trước.
- Mỗi bước compile + test xanh, commit nhỏ, PR nhỏ (< 400 dòng) để review được.

**Câu hỏi nối tiếp:**
- *Làm sao thuyết phục business cho thời gian refactor?* → Không xin "dự án refactor"; gắn refactor vào từng feature, đo bằng lead time/bug rate, trình bày rủi ro cụ thể.
- *Khi nào viết lại là hợp lý?* → Hiếm: công nghệ chết (không vá bảo mật được), hoặc module nhỏ + hành vi được đặc tả đầy đủ; kể cả khi đó vẫn strangler từng phần.

**⚠️ Câu trả lời gây điểm trừ:**
- "Em sẽ viết lại cho sạch" ngay từ đầu.
- Refactor không có test, hoặc PR 3.000 dòng vừa refactor vừa đổi logic.

**📖 Ôn lại:** [§2.3 Quy trình refactor an toàn](../01-giao-trinh/06-design-principles-patterns.md#p2)

</details>

### Q7. 🔴 [Đọc code] Đoạn sau in ra gì? Vì sao đây là lý do nên có value object `Money`?

```java
var a = new BigDecimal("2.0");
var b = new BigDecimal("2.00");
System.out.println(a.equals(b));
System.out.println(a.compareTo(b) == 0);
Set<BigDecimal> prices = new HashSet<>(List.of(a));
System.out.println(prices.contains(b));
```

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** In `false`, `true`, `false`. `BigDecimal.equals` so sánh cả **giá trị và scale** (2.0 có scale 1, 2.00 có scale 2), `compareTo` chỉ so giá trị. Khi tiền là `BigDecimal` + `String currency` rời rạc (primitive obsession), bug kiểu này rải khắp codebase. Value object `Money` chuẩn hóa scale theo currency trong constructor, chặn cộng khác loại tiền, và có `equals` đúng.

**Giải thích chi tiết:**

```java
public record Money(BigDecimal amount, Currency currency) {
    public Money {
        Objects.requireNonNull(amount); Objects.requireNonNull(currency);
        amount = amount.setScale(currency.getDefaultFractionDigits(), RoundingMode.HALF_EVEN); // VND: 0 chữ số
    }
    public Money plus(Money other) {
        if (!currency.equals(other.currency)) throw new IllegalArgumentException("Currency mismatch");
        return new Money(amount.add(other.amount), currency);
    }
}
```

- Sau chuẩn hóa, `Money.of("2.0","USD").equals(Money.of("2.00","USD"))` là `true`; dùng được làm key `HashMap`/`HashSet`.
- Migration an toàn trong codebase lớn: branch by abstraction — giới thiệu `Money`, chuyển từng caller, mỗi commit build + test xanh.
- Liên quan: `TreeSet<BigDecimal>` dùng `compareTo` nên coi 2.0 và 2.00 là một phần tử, còn `HashSet` coi là hai → hành vi khác nhau giữa hai Set (vi phạm "consistent with equals").

**Câu hỏi nối tiếp:**
- *Vì sao không dùng `double` cho tiền?* → Không biểu diễn chính xác số thập phân (0.1 + 0.2 ≠ 0.3); dùng `BigDecimal` hoặc `long` đơn vị nhỏ nhất.
- *`new BigDecimal(0.1)` vs `BigDecimal.valueOf(0.1)`?* → Constructor double lấy giá trị nhị phân xấp xỉ (0.1000000000000000055...); dùng `valueOf` hoặc constructor `String`.

**⚠️ Câu trả lời gây điểm trừ:** Trả lời `true` cho dòng đầu; hoặc biết kết quả nhưng không liên hệ được tới thiết kế value object.

**📖 Ôn lại:** [§2 Bài 2.3 Primitive obsession → Value Object](../01-giao-trinh/06-design-principles-patterns.md#p2)

</details>

---

<a id="nhom-b"></a>
## B. SOLID

### Q8. 🟢 Single Responsibility Principle: "một lý do để thay đổi" nghĩa là gì? Có phải class chỉ nên có một method?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Không. Phát biểu chính xác (Clean Architecture): *một module chỉ chịu trách nhiệm với một **actor*** — một nhóm stakeholder yêu cầu thay đổi. Class `Employee` có `calculatePay()` (kế toán), `reportHours()` (HR), `save()` (DBA) vi phạm SRP vì ba nhóm người cùng sửa một class, dù mỗi method "làm một việc".

**Giải thích chi tiết:**
- Rủi ro thật: kế toán đổi cách tính "giờ làm thường" trong một hàm private dùng chung → báo cáo của HR sai mà không ai biết.
- Cách tách: `PayCalculator`, `HourReporter`, `EmployeeRepository` dùng chung một record dữ liệu `EmployeeData`; có thể thêm facade nếu caller cần một điểm vào.
- Dấu hiệu vi phạm hay gặp: constructor 8–10 dependency, class bị sửa trong rất nhiều PR không liên quan (Divergent Change), tên chung chung (`Manager`, `Helper`).

**Câu hỏi nối tiếp:**
- *SRP ở mức microservice?* → "Những gì thay đổi cùng nhau đặt cùng nhau" — high cohesion theo bounded context.
- *Tách quá mức thì sao?* → Logic một use case bị rải 10 class nông, khó theo dõi; tiêu chí vẫn là trục thay đổi thật.

**⚠️ Câu trả lời gây điểm trừ:** "SRP là mỗi class một method/làm một việc" — định nghĩa sai phổ biến nhất.

**📖 Ôn lại:** [§3.1 SRP](../01-giao-trinh/06-design-principles-patterns.md#p3)

</details>

### Q9. 🟡 Cho ví dụ Open/Closed Principle bạn áp dụng thật với Spring. OCP có nghĩa là "không bao giờ sửa code cũ"?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Ví dụ thanh toán: thay vì `if (method == "CARD") ... else if ("MOMO")...` trong một class (mỗi cổng mới phải sửa và test lại cả class), tôi định nghĩa `PaymentGateway { String method(); void pay(...); }`, mỗi cổng là một bean; `PaymentService` nhận `List<PaymentGateway>` do Spring inject và dựng `Map<method, gateway>`. Thêm ZaloPay = thêm một class. OCP **không** có nghĩa không bao giờ sửa: ta chọn *trục thay đổi dự đoán được* để mở, không mở mọi trục.

**Giải thích chi tiết:**

```java
final class PaymentService {
    private final Map<String, PaymentGateway> gateways;
    PaymentService(List<PaymentGateway> all) {
        this.gateways = all.stream().collect(Collectors.toUnmodifiableMap(PaymentGateway::method, g -> g));
    }
    void pay(String method, long amount) {
        var gw = gateways.get(method);
        if (gw == null) throw new IllegalArgumentException("Unsupported method " + method);
        gw.pay(amount);
    }
}
```

- `toUnmodifiableMap` ném lỗi khi hai bean trùng `method()` → lỗi cấu hình lộ ra **lúc startup**, tốt hơn lúc runtime.
- Có thể inject `Map<String, PaymentGateway>` trực tiếp (key = bean name) nhưng gắn nghiệp vụ vào tên bean — kém tường minh hơn.
- Mở mọi trục (plugin cho mọi thứ) vi phạm YAGNI và tăng gián tiếp.

**Câu hỏi nối tiếp:**
- *Thêm một kênh mới nhưng cần cấu hình riêng (API key)?* → `@ConfigurationProperties` riêng + `@ConditionalOnProperty` để bật/tắt bean theo môi trường.
- *Liên hệ Strategy pattern?* → Đây chính là Strategy + registry.

**⚠️ Câu trả lời gây điểm trừ:** Chỉ đọc định nghĩa Meyer; hoặc cho rằng OCP nghĩa là mọi class đều phải có interface và extension point.

**📖 Ôn lại:** [§3.2 OCP](../01-giao-trinh/06-design-principles-patterns.md#p3)

</details>

### Q10. 🟡 [Đọc code] Đoạn sau lỗi gì? Nó liên quan nguyên tắc nào?

```java
List<String> roles = List.of("USER");
roles.add("ADMIN");

class Square extends Rectangle {
    @Override void setWidth(int w)  { width = w; height = w; }
    @Override void setHeight(int h) { width = h; height = h; }
}
```

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `List.of(...).add` ném `UnsupportedOperationException` lúc runtime; `Square extends Rectangle` phá postcondition của `setWidth` (client gọi `setWidth(5); setHeight(4)` mong `area()==20` nhưng nhận 16). Cả hai liên quan **Liskov Substitution Principle**: subtype phải thay được supertype mà không phá hợp đồng — không tăng precondition, không giảm postcondition, giữ invariant, không ném exception bất ngờ.

**Giải thích chi tiết:**
- JDK chọn thiết kế "optional operation" cho Collections: `List` có `add` nhưng implementation bất biến được phép ném exception (ghi trong Javadoc). Ưu: ít interface; nhược: lỗi chuyển từ compile-time sang runtime. Kotlin tách `List`/`MutableList` ở mức type. Java 21 thêm `SequencedCollection` nhưng vẫn giữ tương thích ngược.
- Sửa Square/Rectangle: đừng ép quan hệ "is-a" theo toán học; dùng abstraction chung bất biến:

```java
sealed interface Shape permits Rect, Sq { int area(); }
record Rect(int width, int height) implements Shape { public int area() { return width * height; } }
record Sq(int side) implements Shape { public int area() { return side * side; } }
```

- Smell tương ứng: Refused Bequest → Replace Inheritance with Delegation.

**Câu hỏi nối tiếp:**
- *Làm sao phát hiện vi phạm LSP sớm?* → Contract test (abstract test class) chạy cho mọi implementation của interface.
- *Immutable giải quyết LSP thế nào?* → Không có setter nên không có postcondition mâu thuẫn; `Sq` và `Rect` chỉ chia sẻ hợp đồng `area()`.

**⚠️ Câu trả lời gây điểm trừ:** Nói `List.of` trả `ArrayList`; hoặc cho rằng LSP chỉ là "subclass phải override đủ method".

**📖 Ôn lại:** [§3.3 LSP](../01-giao-trinh/06-design-principles-patterns.md#p3)

</details>

### Q11. 🟢 Interface Segregation Principle là gì? Ví dụ trong code Spring hằng ngày?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Client không nên bị buộc phụ thuộc vào method nó không dùng — chia "fat interface" thành các **role interface** nhỏ. Ví dụ: tách `OrderReader` (query) và `OrderWriter` (command) để service chỉ đọc không thấy method ghi; với Spring Data, định nghĩa repository kế thừa `Repository<T, ID>` và chỉ khai báo method cần thay vì `JpaRepository` đầy đủ (có cả `deleteAll()`).

**Giải thích chi tiết:**
- Vi phạm kinh điển: `Worker { work(); eat(); attendMeeting(); }` → `Robot` phải ném `UnsupportedOperationException` cho `eat()` (vừa vi phạm ISP vừa LSP).
- Lợi ích: thay đổi một role interface không buộc compile lại/ test lại client không liên quan; test double nhỏ hơn; ý định rõ hơn.
- Port trong hexagonal architecture là ISP ở mức kiến trúc: `LoadOrderPort`, `SaveOrderPort` thay vì một `OrderGateway` to.

**Câu hỏi nối tiếp:**
- *Có phải càng nhiều interface nhỏ càng tốt?* → Không — chia theo nhu cầu client thật, không chia cơ học mỗi method một interface.

**⚠️ Câu trả lời gây điểm trừ:** Nhầm ISP với "mỗi class phải có interface".

**📖 Ôn lại:** [§3.4 ISP](../01-giao-trinh/06-design-principles-patterns.md#p3)

</details>

### Q12. 🟡 Phân biệt Dependency Inversion Principle, Inversion of Control và Dependency Injection.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **DIP** là nguyên tắc về *hướng* phụ thuộc: module cấp cao và cấp thấp cùng phụ thuộc abstraction, và **abstraction thuộc về phía sử dụng** (domain định nghĩa `OrderStore`, hạ tầng implement `JdbcOrderStore`). **IoC** là framework gọi code của bạn thay vì bạn gọi framework ("Hollywood principle"). **DI** là kỹ thuật cung cấp dependency từ bên ngoài (constructor/setter/field) — Spring IoC container là một DI container.

**Giải thích chi tiết:**
- Có thể dùng DI mà vẫn vi phạm DIP: inject `MySqlOrderDao` cụ thể vào service domain — dependency vẫn hướng từ nghiệp vụ xuống hạ tầng.
- Có thể tuân thủ DIP không cần container: tự wiring ở `main` (composition root).
- Điểm mấu chốt của DIP: interface nằm trong package domain/application; package infrastructure import domain, không ngược lại → ép được bằng ArchUnit/multi-module.

```java
interface OrderStore { void save(String orderId); }               // domain định nghĩa cái nó cần
final class OrderService {
    private final OrderStore store;
    OrderService(OrderStore store) { this.store = store; }       // constructor injection
}
final class JdbcOrderStore implements OrderStore { /* infra */ }
```

**Câu hỏi nối tiếp:**
- *Service Locator có phải IoC không?* → Là một dạng IoC nhưng không phải DI; dependency bị ẩn (xem Q45).
- *Vì sao constructor injection được ưa chuộng?* → Dependency tường minh, field `final`, test không cần Spring, constructor dài là tín hiệu vi phạm SRP.

**⚠️ Câu trả lời gây điểm trừ:** Dùng ba khái niệm như từ đồng nghĩa; "DIP là dùng `@Autowired`".

**📖 Ôn lại:** [§3.5 DIP](../01-giao-trinh/06-design-principles-patterns.md#p3)

</details>

### Q13. 🔴 Team bạn quy định mọi service phải có `XxxService` interface + `XxxServiceImpl`. Đó có phải là áp dụng DIP? Bạn có giữ quy ước này không?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Thường là không — nếu interface chỉ có đúng một implementation, nằm cùng package, và được định nghĩa bởi chính phía implement thì đó là **nghi thức**, không phải DIP. Tôi chỉ tạo interface khi có lý do thật: nhiều implementation/strategy, ranh giới module (port gọi hệ thống ngoài), cần test double mà mock class không phù hợp, hoặc là API public của library. Mockito mock được class; Spring CGLIB proxy được class.

**Giải thích chi tiết:**
- Chi phí của interface thừa: gấp đôi file, điều hướng IDE kém, đổi chữ ký phải sửa hai nơi, "Speculative Generality".
- Lý do lịch sử của quy ước: Spring cũ dùng JDK dynamic proxy (cần interface) cho AOP; từ **Spring Boot 2.0** mặc định `proxy-target-class=true` (CGLIB) nên lý do này không còn.
- Khi interface *có* giá trị: port trong hexagonal (`ChargePaymentPort` → adapter Stripe/VNPay/fake), API giữa module trong modular monolith, library dùng nhiều nơi.
- Thay đổi quy ước trong team: đề xuất bằng ADR, áp dụng cho code mới, không refactor ồ ạt code cũ.

**Câu hỏi nối tiếp:**
- *Nếu dùng JDK proxy mà inject bằng class cụ thể?* → `BeanNotOfRequiredTypeException` vì proxy chỉ implement interface.
- *Interface có giúp "dễ đổi implementation sau này"?* → IDE "Extract Interface" mất vài giây khi thật sự cần — YAGNI.

**⚠️ Câu trả lời gây điểm trừ:** "Có interface mới là chuẩn SOLID"; hoặc ngược lại "interface là vô dụng" mà không phân biệt tình huống.

**📖 Ôn lại:** [§3.5 Góc nhìn Senior & Lỗi thường gặp](../01-giao-trinh/06-design-principles-patterns.md#p3)

</details>

### Q14. 🟡 Java 21 có sealed interface + pattern matching `switch`. Dùng `switch` trên kiểu như vậy có vi phạm OCP / "Replace Conditional with Polymorphism" không?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Không nhất thiết. Đây là trade-off kinh điển (expression problem): **polymorphism** dễ thêm *kiểu mới* nhưng khó thêm *thao tác mới*; **sealed + switch exhaustive** dễ thêm *thao tác mới* (một hàm mới) và compiler báo lỗi ở mọi `switch` khi thêm kiểu mới — nên không còn rủi ro "quên một case" như `switch(String type)` cũ. Chọn theo trục thay đổi: tập kiểu đóng, ổn định (trạng thái, kết quả, AST) → sealed + switch; tập kiểu mở, hay thêm (cổng thanh toán, plugin) → polymorphism/strategy.

**Giải thích chi tiết:**

```java
sealed interface ShippingMethod permits Standard, Express, Pickup { double cost(double kg); }
record Standard() implements ShippingMethod { public double cost(double w) { return 2.0 * w; } }
// Hành vi thuộc tầng khác (hiển thị) — không nhét vào domain:
static String label(ShippingMethod m) {
    return switch (m) {                 // exhaustive: không cần default
        case Standard s -> "3-5 ngày";
        case Express e  -> "24 giờ";
        case Pickup p   -> "Nhận tại cửa hàng";
    };
}
```

- Lưu ý: thêm `default ->` vào switch sealed sẽ **mất** kiểm tra exhaustive → bỏ `default`.
- Smell thật sự là `switch` trên *type code dạng String/int lặp lại* ở nhiều nơi, không có compiler kiểm tra.

**Câu hỏi nối tiếp:**
- *Data-oriented programming là gì?* → Dữ liệu bất biến (record) + kiểu tổng (sealed) + hàm thuần dùng pattern matching; tách dữ liệu khỏi hành vi có chủ đích (xem Q44).

**⚠️ Câu trả lời gây điểm trừ:** "Mọi switch đều là smell, phải đổi thành polymorphism" — trả lời giáo điều, không biết tính năng Java hiện đại.

**📖 Ôn lại:** [§2.3 Replace Conditional with Polymorphism (sealed)](../01-giao-trinh/06-design-principles-patterns.md#p2)

</details>

---

<a id="nhom-c"></a>
## C. DRY/KISS/YAGNI, composition, Law of Demeter, cohesion/coupling

### Q15. 🟢 DRY nghĩa là gì? Hai đoạn code giống hệt nhau có luôn phải gộp lại?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** DRY (*The Pragmatic Programmer*) nói về **tri thức**: mỗi mẩu tri thức có *một* biểu diễn có thẩm quyền — không phải "văn bản code giống nhau". Hai đoạn giống hệt nhưng phục vụ hai quy tắc nghiệp vụ độc lập (sẽ thay đổi khác nhau) thì **không nên** gộp: gộp tạo coupling sai — "wrong abstraction is worse than duplication" (Sandi Metz).

**Giải thích chi tiết:**
- Ví dụ: validate độ dài mã cho `Voucher` và `ReferralCode` hiện đều là 8 ký tự; gộp vào một hàm → khi marketing đổi voucher thành 10 ký tự, referral bị đổi theo.
- KISS: giải pháp đơn giản nhất đáp ứng yêu cầu (simple ≠ easy). YAGNI: không xây thứ chưa cần — chi phí gồm xây, test, bảo trì và cản trở thay đổi thật.
- Câu hỏi đúng không phải "có vi phạm DRY không" mà là "khi requirement X đổi, phải sửa mấy chỗ, rủi ro thế nào?".

**Câu hỏi nối tiếp:**
- *Duplication giữa hai microservice/bounded context?* → Thường chấp nhận để tránh coupling (xem Q18).

**⚠️ Câu trả lời gây điểm trừ:** "DRY là không bao giờ copy-paste, thấy giống là gộp."

**📖 Ôn lại:** [§4.1 DRY, KISS, YAGNI](../01-giao-trinh/06-design-principles-patterns.md#p4)

</details>

### Q16. 🟡 [Đọc code] `addCount` bằng bao nhiêu? Vì sao? Sửa thế nào?

```java
class InstrumentedHashSet<E> extends HashSet<E> {
    private int addCount = 0;
    @Override public boolean add(E e) { addCount++; return super.add(e); }
    @Override public boolean addAll(Collection<? extends E> c) { addCount += c.size(); return super.addAll(c); }
    int getAddCount() { return addCount; }
}
var s = new InstrumentedHashSet<String>();
s.addAll(List.of("a", "b", "c"));
System.out.println(s.getAddCount());
```

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** In `6`, không phải 3: `HashSet.addAll` (kế thừa từ `AbstractCollection`) gọi `add()` cho từng phần tử, và `add()` đã bị override nên đếm thêm lần nữa. Đây là **fragile base class problem** — subclass phụ thuộc chi tiết hiện thực không nằm trong hợp đồng. Sửa bằng **composition + forwarding** (wrapper/Decorator) implement `Set<E>` và ủy quyền cho `Set` bên trong (Effective Java Item 18).

**Giải thích chi tiết:**
- Bỏ override `addAll` thì "đúng" — nhưng chỉ đúng *với hiện thực JDK hiện tại*; nếu JDK đổi `addAll` không gọi `add`, kết quả lại sai. Không thể viết subclass đúng mà không đọc source của lớp cha.
- Composition:

```java
class InstrumentedSet<E> implements Set<E> {
    private final Set<E> delegate; private int addCount;
    InstrumentedSet(Set<E> delegate) { this.delegate = delegate; }
    public boolean add(E e) { addCount++; return delegate.add(e); }
    public boolean addAll(Collection<? extends E> c) { addCount += c.size(); return delegate.addAll(c); }
    // ...forward các method còn lại (size, contains, iterator, equals, hashCode...)
}
```

- Kế thừa vẫn hợp lý khi có quan hệ "is-a" thật *và* lớp cha **được thiết kế, tài liệu hóa cho kế thừa** (Item 19: `AbstractList`, `HttpServlet`). Nếu không, đánh dấu class `final` (hoặc `sealed`).

**Câu hỏi nối tiếp:**
- *Wrapper có nhược điểm gì?* → Phải forward nhiều method (dễ quên `equals/hashCode`); không hợp với callback framework truyền `this` (SELF problem).
- *Ví dụ composition trong JDK?* → `Collections.unmodifiableList`, `synchronizedMap`, `java.io` stream.

**⚠️ Câu trả lời gây điểm trừ:** Trả lời 3; hoặc sửa bằng cách xóa override `addAll` và cho là xong.

**📖 Ôn lại:** [§4.2 Composition over inheritance](../01-giao-trinh/06-design-principles-patterns.md#p4)

</details>

### Q17. 🟡 Law of Demeter và "Tell, Don't Ask" là gì? `list.stream().filter(...).map(...).toList()` có vi phạm không?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Law of Demeter: method chỉ nên gọi method của chính object, tham số, object nó tạo ra, và field của nó — tránh "train wreck" `order.getCustomer().getAddress().getCity()` vì caller biết quá nhiều cấu trúc bên trong. Tell, Don't Ask: bảo object *làm việc* (`customer.pay(price)`) thay vì lấy dữ liệu ra tự xử lý rồi set lại. Stream/fluent API và chuỗi gọi trên **data structure** (DTO, record) **không** vi phạm — LoD áp dụng cho object có hành vi.

**Giải thích chi tiết:**

```java
// Ask: invariant "số dư không âm" nằm ngoài Customer, ai cũng setBalance được
if (customer.getWallet().getBalance().compareTo(price) >= 0)
    customer.getWallet().setBalance(customer.getWallet().getBalance().subtract(price));
// Tell: Customer tự bảo vệ invariant
customer.pay(price);
```

- Hệ quả thực tế của vi phạm: đổi cấu trúc `Wallet` buộc sửa mọi nơi đào sâu vào nó (Shotgun Surgery); logic nghiệp vụ trùng lặp ở nhiều service.
- Fluent builder/stream trả về cùng loại abstraction ở mỗi bước, không lộ cấu trúc nội bộ → không vi phạm.

**Câu hỏi nối tiếp:**
- *Giải quyết bằng Hide Delegate có thể dẫn tới smell gì?* → Middle Man — class chỉ toàn method forward; cân bằng giữa hai smell.

**⚠️ Câu trả lời gây điểm trừ:** "Không được có quá một dấu chấm trên một dòng" — hiểu máy móc.

**📖 Ôn lại:** [§4.3 Law of Demeter](../01-giao-trinh/06-design-principles-patterns.md#p4)

</details>

### Q18. 🔴 [Tình huống] Team đề xuất tạo `common-utils` (DTO chung, `Customer` model, helper, exception) dùng cho 20 microservice "cho DRY". Bạn đánh giá thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Tôi phản đối phần lớn đề xuất. Shared library chứa **model nghiệp vụ** dùng chung tạo coupling giữa các service: mọi thay đổi `Customer` buộc nâng version đồng loạt và thường deploy cùng nhau → **distributed monolith**, phá bounded context. Chỉ nên chia sẻ thứ **ổn định, không mang nghiệp vụ**: thư viện hạ tầng (logging/tracing config, security starter, error format), có versioning ngữ nghĩa và tương thích ngược. Model giữa service chia sẻ qua **hợp đồng** (OpenAPI/Avro schema), mỗi service tự có DTO.

**Giải thích chi tiết:**
- `Customer` ở Ordering (địa chỉ giao hàng), Billing (thông tin thuế), Marketing (preference) là các model khác nhau — một class chung phình to và mọi team phải đồng ý mới sửa được.
- Coupling ở mức kiến trúc (Newman): domain, pass-through, common (dùng chung DB), content (đọc DB service khác — tệ nhất). Shared lib nghiệp vụ là dạng coupling ẩn.
- Nếu vẫn cần lib chung: nhỏ, cohesion cao (một lib một mục đích, không phải "utils"), không kéo dependency transitive nặng, semantic versioning, kiểm tra binary compatibility (japicmp/Revapi), service được phép ở version cũ.
- Chấp nhận duplication có chủ đích giữa service, kèm consumer-driven contract test (Pact, Spring Cloud Contract) để bắt breaking change.

**Câu hỏi nối tiếp:**
- *Làm sao đo cohesion trong một codebase?* → LCOM, change coupling trong git log (file nào hay sửa cùng nhau).
- *Platform starter nội bộ khác gì `common-utils`?* → Starter chỉ cung cấp hạ tầng có `@ConditionalOnMissingBean` để service override, không chứa nghiệp vụ.

**⚠️ Câu trả lời gây điểm trừ:** "Đồng ý, DRY là tốt" mà không nhắc coupling/deploy cùng nhau; hoặc "không bao giờ dùng shared library".

**📖 Ôn lại:** [§4.4 Cohesion & Coupling](../01-giao-trinh/06-design-principles-patterns.md#p4)

</details>

### Q19. 🟢 Cohesion và coupling là gì? Làm sao *ép* được ranh giới thay vì chỉ dựa vào quy ước?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Cohesion là mức độ các phần tử trong một module cùng phục vụ một mục đích (muốn **cao**); coupling là mức độ các module biết về nhau (muốn **thấp**). Ép ranh giới bằng công cụ: **ArchUnit** test (controller không gọi repository, domain không import Spring, không vòng phụ thuộc), **multi-module Maven/Gradle** (module core không có dependency Spring nên build fail nếu import), JPMS `exports`, Spring Modulith.

**Giải thích chi tiết:**
- Thang coupling (tệ → tốt): content → common/global → control (truyền flag) → stamp (truyền object to khi chỉ cần 1 field) → data.
- Dấu hiệu cohesion thấp: class `Utils/Helper/Manager`, nhóm method chỉ dùng nhóm field riêng (tách được).

```java
@ArchTest static final ArchRule domainIsPure = noClasses().that().resideInAPackage("..domain..")
    .should().dependOnClassesThat().resideInAPackage("org.springframework..");
@ArchTest static final ArchRule noCycles = slices().matching("com.acme.shop.(*)..").should().beFreeOfCycles();
```

**Câu hỏi nối tiếp:**
- *Package-by-feature giúp gì?* → Dùng được `package-private` để ẩn chi tiết, tăng cohesion (xem Q49).

**⚠️ Câu trả lời gây điểm trừ:** Định nghĩa đúng nhưng không biết cách đo/ép trong thực tế.

**📖 Ôn lại:** [§4.4 Cohesion & Coupling, Bài 4.3 ArchUnit](../01-giao-trinh/06-design-principles-patterns.md#p4)

</details>

---

<a id="nhom-d"></a>
## D. Creational patterns

### Q20. 🟢 Viết Singleton thread-safe trong Java. Có những cách nào, bạn chọn cách nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Các cách đúng: (1) **eager** `static final` — thread-safe nhờ class initialization; (2) `synchronized` method — đúng nhưng mỗi lần gọi đều lấy lock; (3) **double-checked locking với `volatile`**; (4) **initialization-on-demand holder** — lazy, không lock; (5) **enum** một phần tử (Effective Java Item 3) — chống cả reflection và serialization. Tôi chọn enum hoặc holder nếu buộc phải tự viết; còn trong ứng dụng Spring thì để container quản lý (bean singleton scope) và inject.

**Giải thích chi tiết:**

```java
final class HolderLazy {
    private HolderLazy() {}
    private static final class Holder { static final HolderLazy INSTANCE = new HolderLazy(); }
    static HolderLazy getInstance() { return Holder.INSTANCE; } // Holder chỉ init khi lần đầu truy cập
}

enum IdGenerator {
    INSTANCE;
    private final AtomicLong seq = new AtomicLong();
    long next() { return seq.incrementAndGet(); }
}
```

- Holder đúng vì JVM bảo đảm khởi tạo class (`<clinit>`) chạy đúng một lần và có happens-before với mọi thread truy cập sau đó (JLS 12.4.2).
- Lazy naive `if (instance == null) instance = new ...` có race: hai thread cùng thấy `null` → hai instance.

**Câu hỏi nối tiếp:**
- *Enum singleton có nhược điểm gì?* → Không kế thừa class khác được, không lazy theo nghĩa tách biệt (nhưng enum chỉ init khi được dùng lần đầu), "lạ" với một số framework.

**⚠️ Câu trả lời gây điểm trừ:** Chỉ viết được bản lazy naive; viết DCL mà quên `volatile`.

**📖 Ôn lại:** [§5.1 Singleton](../01-giao-trinh/06-design-principles-patterns.md#p5)

</details>

### Q21. 🔴 [Đọc code] Double-checked locking sau đây sai ở đâu? Vì sao test trên laptop chạy mãi vẫn không thấy lỗi?

```java
final class Config {
    private static Config instance;
    private Map<String, String> settings;          // không final
    private int timeoutMs;
    private Config() { settings = loadFromDisk(); timeoutMs = 3000; }
    static Config getInstance() {
        if (instance == null) {
            synchronized (Config.class) {
                if (instance == null) instance = new Config();
            }
        }
        return instance;
    }
}
```

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Thiếu `volatile` trên `instance`. `instance = new Config()` gồm cấp phát, chạy constructor, gán reference; không có `volatile`, compiler/CPU được phép sắp xếp để reference **được công bố trước khi constructor xong**. Thread khác kiểm tra lần thứ nhất (ngoài `synchronized`) thấy `instance != null` và dùng object **chưa khởi tạo xong** (`settings` có thể là `null`). `volatile` tạo quan hệ happens-before giữa ghi và đọc (đúng từ Java 5, JSR-133).

**Giải thích chi tiết:**
- Lần đọc ngoài `synchronized` không có happens-before với lần ghi trong `synchronized` của thread khác → data race theo Java Memory Model.
- Khó tái hiện vì x86 có memory model mạnh (TSO: không reorder store-store) và JIT hiếm khi reorder kiểu này trong trường hợp đơn giản; nhưng code vẫn **sai theo JMM** và có thể lộ trên ARM (Apple Silicon, Graviton) hoặc khi JIT inline khác đi. Công cụ kiểm chứng: **jcstress**.
- Bản đúng:

```java
private static volatile Config instance;
static Config getInstance() {
    Config local = instance;                  // đọc volatile một lần
    if (local == null) {
        synchronized (Config.class) {
            local = instance;
            if (local == null) instance = local = new Config();
        }
    }
    return local;
}
```

- Nếu **mọi** field đều `final` (và `this` không thoát ra trong constructor), *final field semantics* của JMM bảo đảm thread khác thấy giá trị final đã khởi tạo, nên DCL thiếu `volatile` "tình cờ" an toàn cho các field đó. Nhưng chỉ cần thêm một field không final (như `timeoutMs` ở trên, có thể bị thấy là `0`) là hỏng — đừng dựa vào điều này. Đơn giản nhất: dùng holder idiom.

**Câu hỏi nối tiếp:**
- *`volatile` có làm `count++` an toàn không?* → Không — chỉ bảo đảm visibility/ordering, không atomic; dùng `AtomicInteger`/`LongAdder`.

**⚠️ Câu trả lời gây điểm trừ:** "Code đúng vì đã có `synchronized`"; "thêm `volatile` để tránh cache CPU" mà không nói về reordering/happens-before.

**📖 Ôn lại:** [§5.1 Vì sao DCL cần volatile](../01-giao-trinh/06-design-principles-patterns.md#p5)

</details>

### Q22. 🟡 Singleton có thể bị phá bằng những cách nào? Vì sao enum miễn nhiễm?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** (1) **Reflection**: `ctor.setAccessible(true); ctor.newInstance()`; (2) **Serialization**: deserialize tạo object mới; (3) `clone()` nếu implement `Cloneable`; (4) **nhiều ClassLoader** — mỗi loader có "singleton" riêng. Enum miễn nhiễm vì JVM cấm tạo enum bằng reflection (`IllegalArgumentException: Cannot reflectively create enum objects`) và serialization của enum chỉ ghi tên hằng, khi đọc trả về đúng hằng có sẵn.

**Giải thích chi tiết:**
- Phòng reflection cho class thường: trong constructor `if (INSTANCE != null) throw new IllegalStateException();` (an toàn vì lần gọi đầu tiên trong `<clinit>` thì `INSTANCE` còn null).
- Phòng serialization: `private Object readResolve() { return INSTANCE; }` và các field nên `transient`.
- ClassLoader: app server/plugin system, hot reload (Spring DevTools có restart classloader) → hai "singleton" tĩnh; đây cũng là nguồn memory leak classloader (Module 05).

**Câu hỏi nối tiếp:**
- *Singleton có state mutable thì sao?* → Phải thread-safe; tốt nhất giữ stateless hoặc immutable.

**⚠️ Câu trả lời gây điểm trừ:** Không biết `readResolve`; cho rằng `private` constructor là đủ.

**📖 Ôn lại:** [§5.1 Phá vỡ singleton và cách phòng](../01-giao-trinh/06-design-principles-patterns.md#p5)

</details>

### Q23. 🟡 Singleton của GoF khác singleton scope của Spring thế nào? Vì sao `getInstance()` tĩnh bị coi là anti-pattern?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Spring singleton là **một instance cho mỗi bean definition trong một ApplicationContext** — không phải toàn JVM, không chặn `new`, và quan trọng nhất là **được inject** chứ không truy cập tĩnh. `getInstance()` tĩnh bị coi là anti-pattern vì: global mutable state, dependency ẩn (không thấy trong constructor), khó thay bằng test double, khó chạy test song song, vòng đời không kiểm soát được.

**Giải thích chi tiết:**
- Hai context (ví dụ test với cấu hình khác nhau, hoặc parent/child context) → hai instance của cùng bean.
- Hệ quả thực tế: test A sửa state của singleton tĩnh làm test B fail ngẫu nhiên tùy thứ tự chạy.
- Spring bean singleton vẫn phải **thread-safe** vì mọi request thread dùng chung — field mutable như `SimpleDateFormat`, `ArrayList` cache là nguồn race condition (Module 07).

**Câu hỏi nối tiếp:**
- *Khi nào vẫn phải dùng singleton thủ công?* → Library không có DI container; khi đó enum/holder và giữ stateless.

**⚠️ Câu trả lời gây điểm trừ:** "Spring singleton giống hệt GoF singleton"; không nhắc thread-safety của bean singleton.

**📖 Ôn lại:** [§5.1 Singleton GoF vs Spring](../01-giao-trinh/06-design-principles-patterns.md#p5)

</details>

### Q24. 🟢 Phân biệt GoF Factory Method, static factory method và Abstract Factory. Cho ví dụ trong JDK/Spring.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **Factory Method (GoF)**: method (thường abstract) trong class creator, subclass override để quyết định tạo sản phẩm nào — ví dụ `Collection.iterator()`. **Static factory method** (Effective Java Item 1, không phải GoF): `List.of()`, `Integer.valueOf()`, `Optional.of()`, `EnumSet.of()` — có tên, có thể cache, có thể trả subtype ẩn. **Abstract Factory**: tạo **họ** object liên quan nhất quán — `DocumentBuilderFactory`, JDBC `Connection` tạo `Statement`/`PreparedStatement` của cùng driver.

**Giải thích chi tiết:**
- `EnumSet.of(...)` trả `RegularEnumSet` (≤ 64 hằng, dùng một `long`) hoặc `JumboEnumSet` — client không biết, đó là ưu điểm "trả subtype ẩn".
- Spring: `BeanFactory` (container là factory), `FactoryBean<T>` (bean tạo bean, ví dụ `SqlSessionFactoryBean`), method `@Bean` là factory method.
- Abstract Factory nhược điểm: thêm *loại sản phẩm mới* vào họ buộc sửa mọi factory.
- Cách hiện đại: registry `Map<String, Supplier<Parser>>` thay cho hierarchy subclass.

**Câu hỏi nối tiếp:**
- *Nhược điểm static factory?* → Class chỉ có constructor private không kế thừa được; khó tìm trong Javadoc (quy ước tên `of`, `from`, `valueOf`, `getInstance`, `newInstance`).

**⚠️ Câu trả lời gây điểm trừ:** Gọi mọi method tạo object là "Factory pattern" mà không phân biệt.

**📖 Ôn lại:** [§5.2 Factory Method, §5.3 Abstract Factory](../01-giao-trinh/06-design-principles-patterns.md#p5)

</details>

### Q25. 🟡 Khi nào dùng Builder? Bạn từng gặp bug gì với Lombok `@Builder`?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Builder cho object **nhiều tham số tùy chọn**, tránh telescoping constructor và tránh JavaBeans setter (object dở dang, không immutable). Builder tốt: tham số bắt buộc ở `builder(...)`, giá trị mặc định rõ, **validate invariant trong `build()`**, defensive copy collection, object kết quả immutable. Bug Lombok hay gặp: `@Builder` làm **mất giá trị mặc định** của field (cần `@Builder.Default`); dùng trên entity JPA quên `@NoArgsConstructor`/`@AllArgsConstructor` → JPA không khởi tạo được.

**Giải thích chi tiết:**

```java
public HttpClientConfig build() {
    if (maxRetries < 0 || maxRetries > 10) throw new IllegalStateException("maxRetries out of range");
    if (readTimeout.compareTo(connectTimeout) < 0) throw new IllegalStateException("readTimeout < connectTimeout");
    return new HttpClientConfig(this);   // constructor private, Map.copyOf(headers)
}
```

- Với record nhỏ (3–4 field bắt buộc) thì constructor + compact constructor validate là đủ.
- Step builder: mỗi bước trả interface khác nhau để compiler ép thứ tự/thuộc tính bắt buộc — an toàn hơn nhưng nhiều interface; chỉ đáng cho API public.
- JDK/Spring: `HttpRequest.newBuilder()`, `HttpClient.newBuilder()`, `UriComponentsBuilder`, `WebClient.builder()`.

**Câu hỏi nối tiếp:**
- *Builder có thread-safe không?* → Không cần — builder là object cục bộ; object kết quả immutable mới cần an toàn khi chia sẻ.

**⚠️ Câu trả lời gây điểm trừ:** Builder không validate gì, `build()` chỉ `new`; dùng builder cho mọi DTO 2 field.

**📖 Ôn lại:** [§5.4 Builder & Lỗi thường gặp](../01-giao-trinh/06-design-principles-patterns.md#p5)

</details>

### Q26. 🟡 Prototype pattern trong Java: vì sao nên tránh `clone()`? Spring `prototype` scope có liên quan không?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `Cloneable`/`Object.clone()` là thiết kế lỗi (Effective Java Item 13): `Cloneable` là marker interface không có method `clone`, mặc định **shallow copy**, không gọi constructor, xung đột với field `final`, ném checked `CloneNotSupportedException`. Dùng **copy constructor/copy factory** với deep copy collection mutable, hoặc tốt hơn là immutable object + "wither". Spring `prototype` scope là khái niệm **khác**: container tạo instance *mới* mỗi lần yêu cầu, không sao chép.

**Giải thích chi tiết:**

```java
Document(Document prototype) {   // copy constructor — deep copy phần mutable
    this(prototype.title, new ArrayList<>(prototype.paragraphs), new HashMap<>(prototype.metadata));
}
```

- Shallow copy rồi sửa list của bản sao → hỏng bản gốc — bug thật hay gặp với template cấu hình/email.
- Spring prototype: container **không** quản lý destroy callback; inject prototype vào singleton chỉ xảy ra một lần → dùng `ObjectProvider<T>`/`@Lookup` (Module 07).

**Câu hỏi nối tiếp:**
- *Khi object immutable thì cần prototype không?* → Gần như không: chia sẻ an toàn, "sao chép có sửa đổi" bằng wither tạo object mới.

**⚠️ Câu trả lời gây điểm trừ:** Nhầm Spring prototype scope là Prototype pattern; khuyên implement `Cloneable`.

**📖 Ôn lại:** [§5.5 Prototype](../01-giao-trinh/06-design-principles-patterns.md#p5)

</details>

### Q27. 🔴 [Đọc code] Kết quả là gì? Liên quan gì tới static factory method?

```java
Integer a = 127, b = 127;
Integer c = 128, d = 128;
System.out.println(a == b);
System.out.println(c == d);
System.out.println(c.equals(d));
System.out.println(new Integer(5) == Integer.valueOf(5));
```

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `true`, `false` (với cấu hình mặc định), `true`, `false`. Autoboxing gọi `Integer.valueOf(int)` — một **static factory** có **cache** các giá trị −128..127 (flyweight), nên 127 trả về cùng object còn 128 tạo object mới. `new Integer(5)` luôn tạo object mới (constructor đã `@Deprecated(forRemoval = true)` từ Java 16). Bài học: so sánh wrapper bằng `equals`, và đây là ví dụ ưu điểm "không bắt buộc tạo object mới" của static factory so với constructor.

**Giải thích chi tiết:**
- Giới hạn trên của cache có thể nới bằng `-XX:AutoBoxCacheMax=<n>` (hoặc property `java.lang.Integer.IntegerCache.high`) → kết quả dòng 2 phụ thuộc cấu hình JVM — càng là lý do không dùng `==`.
- Tương tự: `Long.valueOf`, `Short`, `Byte`, `Character` (0..127), `Boolean.valueOf` (TRUE/FALSE). `Double/Float` không cache.
- Bug production kinh điển: `if (order.getUserId() == currentUserId)` với `Long` — chạy đúng với user id nhỏ khi test, sai với id lớn trên production.

**Câu hỏi nối tiếp:**
- *Unboxing `null` thì sao?* → `NullPointerException` (ví dụ `int x = map.get(key)` khi key không tồn tại).

**⚠️ Câu trả lời gây điểm trừ:** Trả lời `true` cho cả hai; hoặc biết kết quả nhưng không giải thích được cơ chế cache của `valueOf`.

**📖 Ôn lại:** [§5.2 Static factory method](../01-giao-trinh/06-design-principles-patterns.md#p5)

</details>

---

<a id="nhom-e"></a>
## E. Structural patterns & Spring AOP proxy

### Q28. 🟢 Adapter, Decorator, Proxy, Facade khác nhau thế nào? Mỗi cái cho một ví dụ trong JDK/Spring.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Khác nhau ở **ý định**: **Adapter** đổi interface không tương thích thành interface client mong đợi (`InputStreamReader`, `Arrays.asList`, Spring MVC `HandlerAdapter`); **Decorator** *thêm* trách nhiệm động, xếp chồng được, cùng interface (`BufferedReader(new InputStreamReader(...))`, `Collections.unmodifiableList`); **Proxy** *kiểm soát truy cập* tới object thật — lazy, quyền, remote, transaction — thường do framework tạo, trong suốt với client (Spring AOP proxy, `HibernateProxy`); **Facade** cung cấp interface **đơn giản** cho cả một hệ thống con (`JdbcTemplate`, SLF4J).

**Giải thích chi tiết:**
- Decorator và Proxy có cấu trúc gần như giống hệt (cùng implement interface, giữ reference tới object bên trong). Khác biệt: decorator do *client* chủ động xếp chồng để thêm hành vi; proxy do *hạ tầng* tạo, có thể quản lý vòng đời object thật (virtual proxy tạo object khi cần).
- Tên class trong Spring không phải lúc nào cũng khớp pattern: `LazyConnectionDataSourceProxy` về cấu trúc là decorator.
- Adapter ở mức kiến trúc: adapter trong hexagonal, **anti-corruption layer** của DDD.

**Câu hỏi nối tiếp:**
- *Adapter "rò rỉ" là gì?* → Adapter trả về `VendorResponse` của SDK ra ngoài → client lại phụ thuộc vendor; adapter phải dịch cả kiểu dữ liệu lẫn lỗi.
- *Facade khác God object thế nào?* → Facade *điều phối*, không chứa logic nghiệp vụ; application service thường đóng vai facade.

**⚠️ Câu trả lời gây điểm trừ:** Thuộc sơ đồ UML nhưng không phân biệt được ý định, không có ví dụ thật.

**📖 Ôn lại:** [§6 Structural patterns](../01-giao-trinh/06-design-principles-patterns.md#p6)

</details>

### Q29. 🟡 [Đọc code] Hai cách xếp chồng decorator sau khác nhau thế nào về hành vi?

```java
PriceService a = new TimingPriceService(new CachingPriceService(new RetryingPriceService(remote, 3)));
PriceService b = new RetryingPriceService(new CachingPriceService(new TimingPriceService(remote)), 3);
```

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Thứ tự xếp chồng **có ý nghĩa**. Với `a`: timing đo cả thời gian khi trúng cache; retry chỉ bọc remote call (cache miss mới retry); kết quả retry thành công được cache. Với `b`: timing chỉ đo remote call (không thấy được hit rate qua latency); retry bọc ngoài cache — một lỗi remote khiến thử lại cả lookup cache, và mỗi lần thử được log thời gian riêng. Cách `a` thường là ý định đúng.

**Giải thích chi tiết:**
- Lỗi tương tự với Spring AOP: retry *ngoài* transaction (mỗi lần thử một transaction mới) khác hẳn retry *trong* transaction (thử lại trong transaction đã bị đánh dấu rollback-only → vô nghĩa). Kiểm soát bằng `@Order` của aspect (Module 07).
- `CachingPriceService` dùng `ConcurrentHashMap.computeIfAbsent(sku, inner::priceOf)`: gọi remote *bên trong* `computeIfAbsent` sẽ giữ lock của bin trong lúc I/O → thread khác truy cập cùng bin bị chặn; ổn cho demo, production nên dùng Caffeine (`AsyncLoadingCache`) hoặc Spring Cache.
- Resilience4j làm điều tương tự có kiểm soát: `Decorators.ofSupplier(...).withRetry(...).withCircuitBreaker(...)` — thứ tự cũng quan trọng.

**Câu hỏi nối tiếp:**
- *Decorator quên forward `equals/hashCode` gây gì?* → Object bọc trong `Set`/`Map` hành xử khác object gốc.

**⚠️ Câu trả lời gây điểm trừ:** "Hai cách như nhau vì đều có đủ ba decorator."

**📖 Ôn lại:** [§6.2 Decorator & Lỗi thường gặp](../01-giao-trinh/06-design-principles-patterns.md#p6)

</details>

### Q30. 🟡 JDK dynamic proxy và CGLIB khác nhau thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** JDK dynamic proxy (`java.lang.reflect.Proxy`) sinh lúc runtime một class **implement các interface** cho trước, mọi lời gọi chuyển tới `InvocationHandler` — target phải có interface và chỉ proxy được method của interface; proxy không phải instance của class đích. CGLIB (Spring dùng bản repackaged `org.springframework.cglib`) sinh **subclass** của class đích, override method để chèn interceptor — không cần interface nhưng class/method không được `final`, không proxy được `private`/`static`.

**Giải thích chi tiết:**

| | JDK proxy | CGLIB |
|---|---|---|
| Cơ chế | implement interface | kế thừa class |
| Inject bằng class cụ thể | ❌ `BeanNotOfRequiredTypeException` | ✅ |
| Giới hạn | chỉ method interface | không `final` class/method, không `private` |
| Constructor | không liên quan | Spring dùng Objenesis để tạo proxy không gọi constructor |

```java
AccountService svc = (AccountService) Proxy.newProxyInstance(
        iface.getClassLoader(), new Class<?>[]{AccountService.class}, new TxHandler(target));
svc instanceof SimpleAccountService;   // false
```

- Trong `InvocationHandler` nhớ bóc `InvocationTargetException` (`throw e.getCause()`) để caller nhận exception thật.

**Câu hỏi nối tiếp:**
- *Method `final` trên bean CGLIB thì sao?* → Không bị intercept; tệ hơn, method đó chạy trên *instance proxy* (field chưa được inject vì Objenesis không gọi constructor) → NPE khó hiểu.

**⚠️ Câu trả lời gây điểm trừ:** "CGLIB nhanh hơn nên luôn dùng" — không nói giới hạn; không biết hệ quả inject theo class.

**📖 Ôn lại:** [§6.3 Proxy — JDK vs CGLIB](../01-giao-trinh/06-design-principles-patterns.md#p6)

</details>

### Q31. 🔴 Spring AOP chọn JDK proxy hay CGLIB? Proxy được tạo ở bước nào trong vòng đời bean?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Spring Framework: bean implement ≥ 1 interface → JDK proxy, ngược lại → CGLIB; `proxyTargetClass=true` ép CGLIB. **Spring Boot 2.0+ mặc định `spring.aop.proxy-target-class=true`** → CGLIB cho hầu hết bean. Proxy được tạo bởi `BeanPostProcessor` (`AbstractAutoProxyCreator`, ví dụ `AnnotationAwareAspectJAutoProxyCreator`) ở **`postProcessAfterInitialization`** — tức là sau `@PostConstruct`/`afterPropertiesSet`; container lưu và inject **proxy**, không phải object gốc.

**Giải thích chi tiết:**
- Ngoại lệ: khi có circular dependency, proxy có thể được tạo *sớm* qua `getEarlyBeanReference()` (cache cấp 3 — Module 07).
- Hệ quả của "proxy tạo sau init": gọi method `@Transactional` của chính bean trong `@PostConstruct` không có transaction (đang chạy trên object gốc).
- `@Transactional`, `@Async`, `@Cacheable`, `@Retryable`, `@PreAuthorize`, `@Validated` đều dựa trên proxy. `@Async` dùng `AsyncAnnotationBeanPostProcessor` (một `AbstractAdvisingBeanPostProcessor`) chứ không qua auto-proxy creator chung.
- Kiểm tra: `AopUtils.isCglibProxy(bean)`, `AopUtils.isJdkDynamicProxy(bean)`; class proxy có tên dạng `OrderService$$SpringCGLIB$$0` (Spring 6).

**Câu hỏi nối tiếp:**
- *Vì sao Boot đổi mặc định sang CGLIB?* → Tránh lỗi `BeanNotOfRequiredTypeException` khi dev inject bằng class cụ thể — lỗi rất phổ biến.

**⚠️ Câu trả lời gây điểm trừ:** "Spring dùng CGLIB vì nhanh hơn"; không biết proxy tạo ở BPP after-initialization.

**📖 Ôn lại:** [§6.3 Spring AOP chọn loại nào](../01-giao-trinh/06-design-principles-patterns.md#p6)

</details>

### Q32. 🔴 [Đọc code] `placeOrders` được gọi từ controller. Mỗi đơn có được xử lý trong transaction riêng không? Sửa thế nào?

```java
@Service
public class OrderService {
    public void placeOrders(List<String> ids) {
        for (String id : ids) placeOne(id);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void placeOne(String id) { /* insert order, trừ tồn kho */ }
}
```

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Không — **không có transaction nào cả**. `placeOne(id)` là `this.placeOne(id)`: lời gọi nội bộ đi thẳng vào object gốc, **không qua proxy**, nên `TransactionInterceptor` không chạy (self-invocation). Mỗi lệnh SQL chạy auto-commit; lỗi giữa chừng để lại dữ liệu dở dang. Cách sửa tốt nhất: **tách `placeOne` sang bean khác** (ví dụ `OrderPlacer`) và gọi qua bean đó.

**Giải thích chi tiết:**
Các cách sửa, theo thứ tự ưu tiên:
1. Tách sang bean khác — sạch nhất, ý định rõ.
2. `TransactionTemplate` lập trình tường minh:

```java
for (String id : ids) {
    txTemplate.executeWithoutResult(status -> doPlaceOne(id));   // txTemplate cấu hình PROPAGATION_REQUIRES_NEW
}
```

3. Self-injection qua proxy: `@Lazy` + inject chính mình, hoặc `ObjectProvider<OrderService>` — chạy được nhưng gây vòng tròn "giả", dễ gây khó hiểu.
4. `AopContext.currentProxy()` với `@EnableAspectJAutoProxy(exposeProxy = true)` — gắn code nghiệp vụ vào Spring AOP, ít khuyến nghị.
5. AspectJ weaving (compile-time/load-time, `mode = AdviceMode.ASPECTJ`) — chặn được cả self-invocation, đổi lại build phức tạp.

- Cách debug nhanh: đặt breakpoint, xem `this.getClass()` (object gốc) và stack trace có `TransactionInterceptor` hay không; hoặc log `TransactionSynchronizationManager.isActualTransactionActive()`.

**Câu hỏi nối tiếp:**
- *Nếu `placeOrders` cũng có `@Transactional` (REQUIRED)?* → Cả vòng lặp chạy trong **một** transaction; `REQUIRES_NEW` của `placeOne` vẫn bị bỏ qua → một đơn lỗi rollback tất cả.
- *Áp dụng cho `@Async`, `@Cacheable`, `@PreAuthorize`?* → Đều giống nhau; với `@PreAuthorize` thì là **lỗ hổng bảo mật** (bỏ qua kiểm tra quyền).

**⚠️ Câu trả lời gây điểm trừ:** "Có, mỗi đơn một transaction vì đã có `REQUIRES_NEW`"; hoặc chỉ biết một cách sửa là self-injection.

**📖 Ôn lại:** [§6.3 Self-invocation](../01-giao-trinh/06-design-principles-patterns.md#p6)

</details>

### Q33. 🔴 Liệt kê các trường hợp `@Transactional` "không có tác dụng" hoặc không rollback như mong đợi.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** (1) **Self-invocation**; (2) method `private` (Spring 6 hỗ trợ thêm `protected`/package-private với CGLIB, nhưng `private` không bao giờ); (3) method/class `final` với CGLIB; (4) object tạo bằng `new`, không phải bean; (5) bean bị tạo quá sớm bởi `BeanPostProcessor` → "not eligible for auto-proxying"; (6) **checked exception → commit** (mặc định chỉ rollback `RuntimeException`/`Error`); (7) exception bị **nuốt** trong method; (8) gọi trong `@PostConstruct` (proxy chưa tồn tại); (9) `@Async` + `@Transactional` — chạy trên thread khác, không chia sẻ transaction của caller; (10) không có `PlatformTransactionManager` phù hợp/nhiều transaction manager mà chọn nhầm.

**Giải thích chi tiết:**
- Rollback checked exception: `@Transactional(rollbackFor = Exception.class)`.
- Nuốt exception nhưng transaction con đã đánh dấu rollback-only:

```java
@Transactional
public void outer() {
    try { inner.save(); }                 // inner: @Transactional (REQUIRED) ném RuntimeException
    catch (RuntimeException e) { /* nuốt */ }
}   // commit → UnexpectedRollbackException: Transaction silently rolled back because it has been marked as rollback-only
```

- Lý do: `inner` *tham gia* transaction của `outer`; khi nó ném lỗi, interceptor đánh dấu toàn bộ transaction rollback-only. Muốn `inner` độc lập → `REQUIRES_NEW` (cẩn thận: mượn thêm connection; pool nhỏ + tải cao có thể deadlock pool).
- Transaction chỉ bao *thread hiện tại*: `CompletableFuture.supplyAsync`, parallel stream bên trong không thuộc transaction.

**Câu hỏi nối tiếp:**
- *`readOnly = true` làm gì?* → Gợi ý cho transaction manager/driver (Hibernate tắt dirty checking/flush, một số DB route sang replica); không phải cơ chế bảo mật.
- *Gọi HTTP trong `@Transactional` có vấn đề gì?* → Giữ connection DB suốt thời gian chờ → cạn pool (xem Q58).

**⚠️ Câu trả lời gây điểm trừ:** Chỉ kể được self-invocation; nghĩ mọi exception đều rollback.

**📖 Ôn lại:** [§6.5 Góc nhìn Senior (@Transactional không chạy)](../01-giao-trinh/06-design-principles-patterns.md#p6)

</details>

### Q34. 🔴 Ngoài Spring AOP, proxy còn xuất hiện ở đâu trong một ứng dụng Spring/JPA, và gây bug gì?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** (1) **Hibernate lazy proxy** (`HibernateProxy`): truy cập ngoài session → `LazyInitializationException`; `entity.getClass()` trả class proxy → `equals` dùng `getClass() != o.getClass()` sai; (2) **Spring Data repository** là proxy của interface; (3) class **`@Configuration`** được CGLIB để các method `@Bean` gọi lẫn nhau trả về cùng singleton (tắt bằng `proxyBeanMethods = false`); (4) **scoped proxy** cho bean `request`/`session` scope inject vào singleton; (5) remote proxy: gRPC/RMI stub, HTTP interface client (`@HttpExchange`), Feign.

**Giải thích chi tiết:**
- `equals` cho entity nên dùng `instanceof` (hoặc `Hibernate.getClass(o)`) và so sánh theo ID/natural key; truy cập field của proxy qua getter chứ không trực tiếp `o.id` (field của proxy chưa được nạp).
- `@Configuration` full mode: gọi `dataSource()` bên trong `jdbcTemplate()` vẫn trả bean singleton nhờ CGLIB; lite mode (`@Component` + `@Bean`) thì tạo object mới — bug khó thấy.
- Open Session In View (`spring.jpa.open-in-view=true` mặc định) "che" `LazyInitializationException` bằng cách giữ session tới tầng view → N+1 và giữ connection lâu; nên tắt.

**Câu hỏi nối tiếp:**
- *Vì sao auto-configuration của Boot dùng `proxyBeanMethods = false`?* → Giảm chi phí CGLIB, startup nhanh, hợp với native image; dependency truyền qua tham số `@Bean` method.

**⚠️ Câu trả lời gây điểm trừ:** Chỉ biết proxy của `@Transactional`.

**📖 Ôn lại:** [§6.3 Proxy ở nơi khác](../01-giao-trinh/06-design-principles-patterns.md#p6)

</details>

### Q35. 🟢 Composite pattern là gì? Cho một ví dụ nghiệp vụ.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Composite biểu diễn cấu trúc **cây part–whole** và cho client xử lý object đơn lẻ và nhóm object **đồng nhất** qua cùng interface. Ví dụ quy tắc khuyến mãi: `PercentOff`, `FixedOff` là lá; `AllOf` (áp lần lượt) và `BestOf` (chọn giá tốt nhất) là composite chứa danh sách `PricingRule` — client chỉ gọi `rule.apply(price)`.

**Giải thích chi tiết:**

```java
sealed interface PricingRule permits PercentOff, FixedOff, AllOf, BestOf { long apply(long price); }
record AllOf(List<PricingRule> rules) implements PricingRule {
    public long apply(long p) { for (PricingRule r : rules) p = r.apply(p); return p; }
}
record BestOf(List<PricingRule> rules) implements PricingRule {
    public long apply(long p) { return rules.stream().mapToLong(r -> r.apply(p)).min().orElse(p); }
}
```

- JDK/Spring: `java.awt.Container`, `CompositeCacheManager`, `CompositePropertySource`, `HandlerMethodArgumentResolverComposite`.
- Thường kết hợp Strategy (mỗi rule là một strategy) và có thể nạp cây từ cấu hình JSON/DB.

**Câu hỏi nối tiếp:**
- *Rủi ro?* → Cây quá sâu/vòng lặp nếu cấu hình từ DB — cần validate; hiệu năng khi cây lớn.

**⚠️ Câu trả lời gây điểm trừ:** Chỉ nêu ví dụ cây thư mục trong sách mà không liên hệ được bài toán thực tế.

**📖 Ôn lại:** [§6.5 Composite](../01-giao-trinh/06-design-principles-patterns.md#p6)

</details>

---

<a id="nhom-f"></a>
## F. Behavioral patterns

### Q36. 🟢 Strategy pattern trong Java hiện đại trông như thế nào? Kể ví dụ trong JDK và Spring.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Strategy đóng gói một họ thuật toán để thay thế lúc runtime, loại bỏ `if/else` theo loại thuật toán. Trong Java hiện đại, strategy thường chỉ là **functional interface + lambda**, hoặc **enum implement interface** khi tập strategy hữu hạn, có tên, cần lưu cấu hình. JDK: `Comparator`, `RejectedExecutionHandler`. Spring: `PlatformTransactionManager`, `PasswordEncoder`, `HandlerMethodArgumentResolver`; chọn theo cấu hình bằng `Map<String, Strategy>` inject.

**Giải thích chi tiết:**

```java
enum Tier implements DiscountStrategy {
    STANDARD { public BigDecimal apply(BigDecimal a) { return a; } },
    GOLD     { public BigDecimal apply(BigDecimal a) { return a.multiply(new BigDecimal("0.90")); } }
}
checkout(amount, Tier.GOLD);
checkout(amount, a -> a.subtract(new BigDecimal("50000")));   // strategy ad-hoc bằng lambda
```

- Enum strategy: serialize/lưu DB dễ (`tier = 'GOLD'`), có `values()` để liệt kê.
- Bean strategy: khi strategy cần dependency (gọi API, repository).

**Câu hỏi nối tiếp:**
- *Strategy khác State thế nào?* → Xem Q41.

**⚠️ Câu trả lời gây điểm trừ:** Dựng interface + 5 class + factory theo sách 1994 cho bài toán một lambda giải quyết được.

**📖 Ôn lại:** [§7.1 Strategy](../01-giao-trinh/06-design-principles-patterns.md#p7)

</details>

### Q37. 🟡 Template Method khác Strategy thế nào? `JdbcTemplate` thuộc pattern nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Template Method dùng **kế thừa**: lớp cha định nghĩa khung thuật toán (method `final`), lớp con hiện thực *một phần* các bước; quyết định ở compile-time. Strategy dùng **composition**: thay *toàn bộ* thuật toán lúc runtime. `JdbcTemplate`, `TransactionTemplate`, `RestTemplate` là **template method dạng composition**: khung cố định (lấy connection, xử lý exception, đóng resource), phần thay đổi truyền vào qua **callback** (`RowMapper`, `TransactionCallback`) — tức là kết hợp với Strategy, tránh ràng buộc kế thừa.

**Giải thích chi tiết:**
- Template Method kinh điển: `AbstractList` (chỉ cần `get`, `size`), `HttpServlet.service()` gọi `doGet/doPost`, `AbstractApplicationContext.refresh()`.
- Nhược: nhiều hook → subclass phải hiểu toàn bộ lớp cha (fragile base class); khó test bước riêng lẻ; Java chỉ đơn kế thừa.
- Ưu tiên callback/strategy trừ khi khung có nhiều bước và hook cần chia sẻ state.

**Câu hỏi nối tiếp:**
- *Vì sao template method thường `final`?* → Để subclass không phá vỡ thứ tự các bước.

**⚠️ Câu trả lời gây điểm trừ:** "Hai pattern giống nhau"; không nhận ra `JdbcTemplate` dùng callback.

**📖 Ôn lại:** [§7.2 Template Method](../01-giao-trinh/06-design-principles-patterns.md#p7)

</details>

### Q38. 🟡 Observer trong Spring là gì? Những rủi ro khi dùng `ApplicationEventPublisher` + `@EventListener` trên production?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Spring triển khai Observer bằng `ApplicationEventPublisher.publishEvent(...)` + `@EventListener`. Rủi ro: listener **mặc định đồng bộ, cùng thread, cùng transaction** — listener chậm làm chậm request, exception trong listener lan ngược về publisher (có thể rollback transaction); listener chạy *trước khi commit* nên có thể gửi email cho đơn hàng sau đó bị rollback. Dùng `@TransactionalEventListener(phase = AFTER_COMMIT)` cho side effect sau commit; nhưng event in-memory **không bền** — crash sau commit là mất → sự kiện quan trọng dùng **Transactional Outbox**.

**Giải thích chi tiết:**
- `@Async` trên listener: tách thread nhưng mất transaction context, `SecurityContext`, MDC (cần `TaskDecorator`).
- `@TransactionalEventListener` khi publish **ngoài** transaction → listener không được gọi (trừ `fallbackExecution = true`).
- Observer tự viết: dùng `CopyOnWriteArrayList` cho danh sách listener, một listener lỗi không chặn listener khác, trả handle để hủy đăng ký (tránh leak), không gọi listener khi đang giữ lock (deadlock).
- `java.util.Observable` deprecated từ Java 9 (không thread-safe, không generic).

**Câu hỏi nối tiếp:**
- *Outbox hoạt động thế nào?* → Ghi event vào bảng `outbox` cùng transaction với nghiệp vụ; relay (poller hoặc CDC như Debezium) publish ra broker, at-least-once → consumer idempotent.

**⚠️ Câu trả lời gây điểm trừ:** "Event của Spring là async, không ảnh hưởng request"; không biết `@TransactionalEventListener`.

**📖 Ôn lại:** [§7.3 Observer](../01-giao-trinh/06-design-principles-patterns.md#p7)

</details>

### Q39. 🟡 Chain of Responsibility xuất hiện ở đâu trong stack Spring web? Rủi ro thường gặp là gì?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Servlet `Filter` + `FilterChain`, **Spring Security `SecurityFilterChain`** (chuỗi ~15 filter), Spring MVC `HandlerInterceptor`, interceptor của OkHttp/Feign/gRPC, Netty `ChannelPipeline`. Mỗi handler xử lý, sửa đổi, dừng chuỗi (trả 401/429) hoặc chuyển tiếp. Rủi ro: **thứ tự sai** (log body nhạy cảm trước khi xác thực; rate limit sau phần tốn tài nguyên), không có handler cuối mặc định, chuỗi dài khó debug, handler giữ state không thread-safe.

**Giải thích chi tiết:**

```java
interface Handler { Response handle(Request req, Chain chain); }
Handler auth = (req, chain) -> req.headers().containsKey("Authorization")
        ? chain.proceed(req) : new Response(401, "Unauthorized");   // dừng chuỗi
```

- Ví dụ rate limiter trong chuỗi dùng `HashMap` đếm request → không thread-safe dưới tải; dùng `ConcurrentHashMap.merge` hoặc token bucket nguyên tử; và bộ đếm in-memory sai khi chạy nhiều instance.
- Debug Spring Security: `logging.level.org.springframework.security=TRACE` liệt kê filter chạy theo thứ tự.

**Câu hỏi nối tiếp:**
- *Filter khác Interceptor thế nào?* → Filter thuộc Servlet container, áp mọi request; Interceptor thuộc DispatcherServlet, biết `HandlerMethod` (Module 08).

**⚠️ Câu trả lời gây điểm trừ:** Chỉ nêu ví dụ "xử lý đơn nghỉ phép qua các cấp quản lý" mà không liên hệ stack đang dùng.

**📖 Ôn lại:** [§7.4 Chain of Responsibility](../01-giao-trinh/06-design-principles-patterns.md#p7)

</details>

### Q40. 🟢 Command pattern giải quyết vấn đề gì? Trong hệ thống phân tán cần lưu ý gì thêm?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Command đóng gói **yêu cầu thành object** → có thể xếp hàng, log, gửi qua mạng, retry, undo/redo, thực thi trễ. JDK: `Runnable`/`Callable` gửi vào `ExecutorService`. Kiến trúc: command trong CQRS, message trong Kafka/RabbitMQ, job Quartz/Spring Batch. Khi command đi qua mạng hoặc được retry → cần **idempotency key** để thực thi trùng không gây hậu quả trùng.

**Giải thích chi tiết:**
- Undo/redo: mỗi command có `execute()`/`undo()`; editor giữ hai stack; command mới xóa redo stack. Command phải lưu đủ state để undo (ví dụ `DeleteCommand` lưu đoạn bị xóa).
- Idempotency trên production: lưu key trong DB với **unique constraint**, cùng transaction với thay đổi trạng thái; trùng key → trả kết quả cũ.

**Câu hỏi nối tiếp:**
- *Command khác event thế nào?* → Command là *yêu cầu* (mệnh lệnh, có thể bị từ chối, một handler); event là *sự thật đã xảy ra* (thì quá khứ, nhiều subscriber).

**⚠️ Câu trả lời gây điểm trừ:** Không nhắc idempotency khi nói về command qua message queue.

**📖 Ôn lại:** [§7.5 Command](../01-giao-trinh/06-design-principles-patterns.md#p7)

</details>

### Q41. 🟡 State pattern khác Strategy thế nào? Mô hình hóa vòng đời đơn hàng bạn làm ra sao?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Cấu trúc giống nhau; khác ở **ai đổi object**: Strategy do *client* chọn từ ngoài, các strategy không biết nhau; State do *chính các state* quyết định trạng thái kế tiếp. Vòng đời đơn hàng: tôi dùng **enum với method chuyển trạng thái** (mặc định ném `IllegalStateException`, mỗi trạng thái override chuyển hợp lệ), aggregate `Order` gọi `status = status.pay()`; lưu DB kèm **optimistic locking `@Version`** để hai request không cùng chuyển trạng thái.

**Giải thích chi tiết:**

```java
enum Status {
    NEW  { Status pay() { return PAID; }    Status cancel() { return CANCELLED; } },
    PAID { Status ship() { return SHIPPED; } Status cancel() { return REFUNDING; } },
    SHIPPED { Status deliver() { return DELIVERED; } },
    DELIVERED, CANCELLED, REFUNDING;
    Status pay()    { throw illegal("pay"); }
    Status ship()   { throw illegal("ship"); }
    Status deliver(){ throw illegal("deliver"); }
    Status cancel() { throw illegal("cancel"); }
    IllegalStateException illegal(String a) { return new IllegalStateException("Cannot " + a + " when " + this); }
}
```

- Ưu: mọi chuyển trạng thái hợp lệ ở một chỗ, không còn `switch(status)` rải khắp service.
- State machine phức tạp (guard, action, persist, phân tán, saga): bảng chuyển trạng thái trong DB, Spring Statemachine, hoặc workflow engine (Temporal, Camunda).
- Cạnh tranh: hai request "cancel" và "ship" cùng lúc → không có `@Version` thì cả hai đọc `PAID` và cùng ghi → trạng thái cuối phụ thuộc ai ghi sau.

**Câu hỏi nối tiếp:**
- *Thay vì optimistic lock?* → `UPDATE orders SET status='SHIPPED' WHERE id=? AND status='PAID'` (compare-and-set), kiểm tra số dòng cập nhật.

**⚠️ Câu trả lời gây điểm trừ:** "State và Strategy giống nhau"; quên vấn đề đồng thời khi chuyển trạng thái.

**📖 Ôn lại:** [§7.6 State](../01-giao-trinh/06-design-principles-patterns.md#p7)

</details>

### Q42. 🟡 [Đọc code] Đoạn sau chạy thế nào? Iterator fail-fast và weakly consistent khác nhau ra sao?

```java
List<Integer> list = new ArrayList<>(List.of(1, 2, 3, 4));
for (Integer i : list) {
    if (i == 2) list.remove(i);
}
```

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Ném `ConcurrentModificationException`: for-each dùng `Iterator` của `ArrayList`, iterator **fail-fast** so `modCount` với giá trị lúc tạo; `list.remove(i)` (gọi `remove(Object)` vì `i` là `Integer`) làm thay đổi `modCount` → lần `next()` sau phát hiện và ném. Cách đúng: `list.removeIf(i -> i == 2)` hoặc dùng `iterator.remove()`. Iterator của `ConcurrentHashMap`/`CopyOnWriteArrayList` là **weakly consistent/snapshot** — không ném CME, có thể không thấy thay đổi đồng thời.

**Giải thích chi tiết:**
- Edge case thú vị: nếu xóa phần tử **áp chót** (ví dụ `i == 3` trong list 4 phần tử), vòng lặp kết thúc mà *không* ném CME vì `hasNext()` trả `false` (cursor == size) — fail-fast chỉ là *best-effort*, không được dựa vào để đảm bảo đúng.
- Fail-fast không phải cơ chế thread-safety: hai thread sửa `ArrayList` có thể làm hỏng dữ liệu mà không ném CME.
- Iterator tự viết phải tuân hợp đồng: `next()` ném `NoSuchElementException` khi hết; `hasNext()` không có side effect.

**Câu hỏi nối tiếp:**
- *`list.remove(2)` với `List<Integer>` xóa gì?* → Xóa phần tử ở **index 2** (overload `remove(int)`), không phải giá trị 2.

**⚠️ Câu trả lời gây điểm trừ:** "Chạy bình thường, xóa số 2"; nghĩ fail-fast bảo đảm phát hiện mọi sửa đổi.

**📖 Ôn lại:** [§7.7 Iterator](../01-giao-trinh/06-design-principles-patterns.md#p7)

</details>

### Q43. 🔴 [Tình huống] Thiết kế engine kiểm tra giao dịch chống gian lận: nhiều rule (hạn mức, tần suất, blacklist, lệch vị trí), bật/tắt và đổi thứ tự bằng cấu hình, kết quả ALLOW/DENY/REVIEW. Bạn dùng pattern gì và lưu ý gì?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **Chain of Responsibility** cho pipeline rule (thứ tự, bật/tắt từ cấu hình), mỗi rule là một **Strategy** (bean implement `FraudRule`), kết quả là **sealed type** `Decision permits Allow, Deny, Review`. Engine hỗ trợ hai chế độ: short-circuit (dừng ở DENY đầu tiên) và collect-all (thu mọi lý do để audit). Lưu ý: rule có state (velocity) phải **thread-safe** và đúng khi **nhiều instance** (state ở Redis), latency budget (p99 < vài ms), mỗi rule có metric và timeout, rule lỗi thì fail-open hay fail-closed phải là quyết định nghiệp vụ rõ ràng.

**Giải thích chi tiết:**

```java
sealed interface Decision permits Allow, Deny, Review {}
record Allow() implements Decision {}
record Deny(String reason) implements Decision {}
record Review(String reason) implements Decision {}

interface FraudRule { String name(); Decision evaluate(Transaction tx); }

final class FraudEngine {
    private final List<FraudRule> rules;   // đã lọc + sắp theo cấu hình (YAML/DB) lúc khởi tạo
    Decision evaluate(Transaction tx) {
        Decision worst = new Allow();
        for (FraudRule r : rules) {
            Decision d = r.evaluate(tx);
            if (d instanceof Deny) return d;              // short-circuit
            if (d instanceof Review) worst = d;
        }
        return worst;
    }
}
```

- `VelocityRule` (quá N giao dịch/phút): in-memory dùng `ConcurrentHashMap<String, Deque<Long>>` + `compute` để cập nhật nguyên tử theo key, giới hạn kích thước map; multi-instance dùng Redis sorted set/INCR + EXPIRE (Lua script để nguyên tử).
- Thêm rule = thêm bean + dòng cấu hình (OCP). Cấu hình đổi lúc runtime → nạp lại an toàn (immutable list, swap reference `volatile`/`AtomicReference`).
- Kiểm chứng: test đồng thời 50 thread cho rule có state; benchmark JMH cho chuỗi 10 rule.

**Câu hỏi nối tiếp:**
- *Rule gọi dịch vụ ngoài (geo-IP) chậm?* → Timeout ngắn + cache + circuit breaker; fallback là REVIEW thay vì chặn toàn bộ giao dịch.
- *Làm sao giải thích được quyết định cho đội vận hành?* → Lưu "decision trace" (rule nào, kết quả, lý do) kèm transaction id.

**⚠️ Câu trả lời gây điểm trừ:** Một method 300 dòng `if/else`; không nhắc thread-safety/multi-instance của rule có state.

**📖 Ôn lại:** [§7 Bài 7.3 Validation chain có thể cấu hình](../01-giao-trinh/06-design-principles-patterns.md#p7)

</details>

---

<a id="nhom-g"></a>
## G. Anti-patterns

### Q44. 🟡 Anemic Domain Model có thật sự là anti-pattern? Trình bày cả hai phía.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Fowler gọi là anti-pattern: entity chỉ có getter/setter, mọi logic ở service → invariant không được bảo vệ ở một chỗ, ai cũng `setBalance(-1000)` được. Phía ngược lại: với ứng dụng **CRUD logic mỏng**, rich model là over-engineering; và phong cách **data-oriented** (record bất biến + sealed + hàm thuần) tách dữ liệu và hành vi *có chủ đích* mà vẫn an toàn nhờ immutable và kiểu chặt. Kết luận của tôi: câu hỏi thật là **invariant có được bảo vệ ở một nơi duy nhất không**, và domain có đủ phức tạp để đáng chi phí DDD không.

**Giải thích chi tiết:**

```java
class RichAccount {
    private long balance;
    void withdraw(long amount) {
        if (amount <= 0) throw new IllegalArgumentException("amount > 0");
        if (balance < amount) throw new IllegalStateException("Insufficient funds");
        balance -= amount;
    }
}
```

- Rich model dễ unit test thuần (không mock), ngôn ngữ domain hiện ra trong code.
- Sai lầm ngược: entity JPA gọi repository/service bên trong để "rich" → domain phụ thuộc hạ tầng. Logic cần nhiều aggregate → domain service/application service.
- Thực tế với JPA: không setter public, method nghiệp vụ trên entity, constructor `protected` không tham số cho Hibernate.

**Câu hỏi nối tiếp:**
- *Service lúc đó còn làm gì?* → Điều phối: load → gọi domain → save → publish event; transaction boundary.

**⚠️ Câu trả lời gây điểm trừ:** Một chiều ("anemic luôn sai" hoặc "rich model là lý thuyết suông") không có tiêu chí.

**📖 Ôn lại:** [§8.2 Anemic Domain Model](../01-giao-trinh/06-design-principles-patterns.md#p8)

</details>

### Q45. 🟢 Service Locator là gì, vì sao bị coi là anti-pattern? `applicationContext.getBean(...)` trong code nghiệp vụ thì sao?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Service Locator: class tự "hỏi" một registry toàn cục để lấy dependency thay vì nhận qua constructor. Bị coi là anti-pattern (Mark Seemann) vì dependency **ẩn** (đọc constructor không biết class cần gì), thiếu dependency chỉ lộ lúc runtime, test phải cấu hình registry toàn cục. Gọi `applicationContext.getBean()` trong code nghiệp vụ chính là service locator → thay bằng constructor injection, `ObjectProvider<T>` (lấy lazy/prototype) hoặc inject `Map<String, Strategy>` (tra động theo tên).

**Giải thích chi tiết:**
- Ngoại lệ hợp lý: code hạ tầng/framework, factory cần tạo bean động mà số lượng/loại chỉ biết lúc runtime — vẫn nên bọc trong một class hạ tầng duy nhất.
- `ObjectProvider` vẫn tường minh: xuất hiện trong constructor, có `getIfAvailable()`, `stream()`.

**Câu hỏi nối tiếp:**
- *Liên hệ IoC?* → Service locator vẫn là IoC theo nghĩa rộng nhưng không phải DI (xem Q12).

**⚠️ Câu trả lời gây điểm trừ:** "Dùng `getBean` cho tiện, không sao."

**📖 Ôn lại:** [§8.3 Service Locator](../01-giao-trinh/06-design-principles-patterns.md#p8)

</details>

### Q46. 🟡 [Tình huống] `OrderManager` 5.000 dòng, 40 dependency, bị sửa trong 1/3 số PR. Bạn gỡ nó thế nào mà không dừng phát triển?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Không viết lại. (1) Đo: dùng git log tìm **change coupling** — cụm method/field hay thay đổi cùng nhau là ứng viên class mới; xác định actor/trục thay đổi (SRP). (2) Bảo vệ bằng characterization test quanh cụm sắp tách. (3) Tách từng cụm thành class gắn kết (theo use case: `OrderPlacement`, `OrderCancellation`, `OrderQuery`), `OrderManager` tạm thời **ủy quyền** sang class mới để caller không đổi. (4) Chuyển caller dần, cuối cùng xóa method ủy quyền. Mỗi bước là PR nhỏ (< 400 dòng), deploy được độc lập, có kế hoạch rollback.

**Giải thích chi tiết:**
- Dấu hiệu God class: tên chung chung, constructor > 7 tham số, merge conflict liên tục, test cần dựng "cả thế giới".
- Đẩy hành vi vào domain object (Tell, Don't Ask) để service mỏng lại.
- Thêm ArchUnit rule/Sonar quality gate để class mới không phình lại.
- Kèm anti-pattern hay đi chung: **Open Session In View** bật mặc định trong Boot (`spring.jpa.open-in-view=true`) che giấu lazy loading ngầm, N+1, giữ connection lâu — tắt khi refactor tầng service.

**Câu hỏi nối tiếp:**
- *Làm sao chứng minh với manager là đáng làm?* → Số liệu: % PR chạm file, số bug/hotfix từ module, thời gian review; kế hoạch theo PR nhỏ không chặn feature.

**⚠️ Câu trả lời gây điểm trừ:** "Tạo branch refactor dài 2 tháng rồi merge một lần."

**📖 Ôn lại:** [§8.1 God Object, Bài 8.3](../01-giao-trinh/06-design-principles-patterns.md#p8)

</details>

---

<a id="nhom-h"></a>
## H. Kiến trúc: layered, hexagonal, clean

### Q47. 🟡 So sánh layered architecture với hexagonal/clean architecture.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Layered: Presentation → Service → Persistence, phụ thuộc đi **từ trên xuống**, trung tâm thực tế là **database** — domain phụ thuộc persistence, entity JPA lan lên controller. Hexagonal (Ports & Adapters) / Clean / Onion: **domain + use case ở trung tâm**, định nghĩa **port** (interface) theo nhu cầu của mình; adapter (REST, JPA, Kafka, HTTP client) ở ngoài implement port — mọi phụ thuộc mã nguồn hướng **vào trong** (DIP ở mức kiến trúc). Layered rẻ để bắt đầu, hợp CRUD; hexagonal tốn port + mapping nhưng test use case không cần Spring/DB, đổi hạ tầng chỉ đổi adapter, hợp domain phức tạp sống lâu.

**Giải thích chi tiết:**

```java
public interface PayOrderUseCase { void pay(OrderId id); }              // input port
public interface LoadOrderPort { Optional<Order> load(OrderId id); }   // output port do core định nghĩa
public final class PayOrderService implements PayOrderUseCase {        // core: không import Spring/JPA
    public void pay(OrderId id) {
        Order order = load.load(id).orElseThrow(...);
        payment.charge(order.id(), order.amount());
        order.markPaid();
        save.save(order);
    }
}
// adapters: @RestController → PayOrderUseCase; @Component OrderJpaAdapter implements LoadOrderPort, SaveOrderPort
```

- Hexagonal, Onion, Clean là **cùng một tư tưởng**, khác thuật ngữ và số vòng.
- Clean Architecture Dependency Rule: dữ liệu vượt ranh giới dưới dạng cấu trúc đơn giản — không truyền `HttpServletRequest` hay entity JPA vào trong.

**Câu hỏi nối tiếp:**
- *`@Transactional` đặt ở đâu trong hexagonal?* → Ở application service (có thể cho phép annotation Spring ở tầng application, giữ domain thuần), hoặc ở adapter/composition root bằng decorator transaction bọc use case.

**⚠️ Câu trả lời gây điểm trừ:** "Hexagonal luôn tốt hơn layered"; vẽ được sơ đồ nhưng không nói được chi phí.

**📖 Ôn lại:** [§9 Kiến trúc](../01-giao-trinh/06-design-principles-patterns.md#p9)

</details>

### Q48. 🔴 [Tình huống] Service mới quản lý khoản vay với quy tắc nghiệp vụ phức tạp, tích hợp 3 ngân hàng. Bạn chọn kiến trúc gì, và làm sao để ranh giới không bị "rò rỉ" sau 6 tháng?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Chọn **hexagonal "thực dụng" + package-by-feature**: core (domain khoản vay, use case) thuần Java; port cho từng tích hợp ngân hàng (mỗi ngân hàng một adapter + anti-corruption layer dịch model của họ); cho phép dùng entity JPA làm domain model ở chỗ mapping không đem lại giá trị. Ranh giới được **ép tự động**: module Maven/Gradle `core` không có dependency Spring/JPA (build fail nếu import), ArchUnit trong CI, hoặc Spring Modulith. Ghi quyết định bằng **ADR** (context, phương án, hệ quả, điều kiện xem xét lại).

**Giải thích chi tiết:**
- Tiêu chí chọn: độ phức tạp domain (cao), số kênh vào/ra (REST + 3 bank + có thể batch/Kafka), tuổi thọ dự kiến (dài), kích thước team.
- Cấu trúc module gợi ý: `loan-core` (chỉ JDK + test libs), `loan-adapter-web`, `loan-adapter-jpa`, `loan-adapter-bank-x`, `loan-app` (wiring `@Configuration`, composition root).
- Test: use case với in-memory adapter (< 100 ms), contract test chung cho các adapter ngân hàng, integration test từng adapter (WireMock, Testcontainers).
- "Clean architecture hình thức" cần tránh: đủ 4 tầng + interface cho mọi thứ nhưng use case chỉ chuyển tiếp CRUD, domain vẫn anemic, `@Entity`/`@Transactional` rò vào core.

**Câu hỏi nối tiếp:**
- *Khi nào xem lại quyết định?* → Nếu đa số use case chỉ là CRUD và chi phí mapping vượt lợi ích → nới lỏng (dùng entity làm model, bỏ port không cần).

**⚠️ Câu trả lời gây điểm trừ:** Chọn kiến trúc theo sở thích, không có tiêu chí; "team sẽ tự giữ quy ước".

**📖 Ôn lại:** [§9.3 Góc nhìn Senior & Bài 9.3 ADR](../01-giao-trinh/06-design-principles-patterns.md#p9)

</details>

### Q49. 🟢 Package-by-layer và package-by-feature khác nhau thế nào? Bạn chọn cách nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Package-by-layer: `controller/`, `service/`, `repository/` — mọi class phải `public` để tầng khác gọi, một feature rải khắp các package. Package-by-feature: `order/`, `payment/`, mỗi package chứa đủ các tầng của feature → **cohesion cao**, dùng được `package-private` để ẩn chi tiết (chỉ API của feature là public), xóa/tách feature thành service dễ hơn. Tôi chọn package-by-feature (có thể chia layer bên trong feature).

**Giải thích chi tiết:**
- Package-by-feature là bước đầu của **modular monolith**; Spring Modulith kiểm tra được module nào truy cập internal của module khác.
- Đo lợi ích: số class `public` giảm rõ rệt.

**Câu hỏi nối tiếp:**
- *Code dùng chung giữa feature để đâu?* → Package `shared`/`common` nhỏ, chỉ chứa thứ thật sự chung (value object chung như `Money`); cẩn thận không thành "utils".

**⚠️ Câu trả lời gây điểm trừ:** Không biết lợi ích về visibility/cohesion.

**📖 Ôn lại:** [§9.1 Layered — biến thể package](../01-giao-trinh/06-design-principles-patterns.md#p9)

</details>

---

<a id="nhom-i"></a>
## I. DDD & CQRS

### Q50. 🟢 Entity khác Value Object thế nào? `Address` là entity hay value object?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **Entity** có định danh xuyên suốt vòng đời — hai entity bằng nhau nếu cùng ID dù thuộc tính khác (`Order`, `Customer`). **Value Object** không có định danh, xác định bởi giá trị, **immutable**, tự validate (`Money`, `Email`, `DateRange`) — `record` là lựa chọn tự nhiên. `Address` **tùy bounded context**: trong đơn hàng là value object (đổi địa chỉ = thay giá trị); trong hệ thống quản lý bưu cục, một địa điểm có mã, lịch sử → entity.

**Giải thích chi tiết:**
- Ví dụ khác: ghế rạp phim có số cụ thể, đặt riêng lẻ → entity; vé đứng sự kiện chỉ đếm số lượng → value.
- Entity `equals/hashCode` theo ID; với JPA ID sinh bởi DB thì ID là `null` trước persist → dùng ID do ứng dụng sinh (UUID/ULID) hoặc natural key, hoặc hashCode hằng theo class.
- Value object loại bỏ primitive obsession và gom validation một chỗ.

**Câu hỏi nối tiếp:**
- *Value object trong JPA map thế nào?* → `@Embeddable` (Hibernate 6.2+ hỗ trợ record làm embeddable) hoặc `AttributeConverter`.

**⚠️ Câu trả lời gây điểm trừ:** "Entity là class có `@Entity`, value object là DTO."

**📖 Ôn lại:** [§10.1 DDD tactical](../01-giao-trinh/06-design-principles-patterns.md#p10)

</details>

### Q51. 🔴 Aggregate là gì? Nêu các quy tắc thiết kế aggregate và cách bạn chốt ranh giới aggregate.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Aggregate là cụm entity + value object được coi là **một đơn vị nhất quán**, truy cập qua **aggregate root**. Quy tắc (Vaughn Vernon): (1) bảo vệ **invariant thật sự** trong ranh giới; (2) thiết kế aggregate **nhỏ**; (3) tham chiếu aggregate khác **bằng ID**; (4) **một transaction chỉ sửa một aggregate** — nhất quán giữa aggregate là eventual qua domain event. Chốt ranh giới bằng câu hỏi: *invariant nào phải đúng ngay trong cùng transaction, cái nào chấp nhận đúng sau vài giây?*

**Giải thích chi tiết:**

```java
final class Order {                                    // aggregate root
    private final CustomerId customerId;              // tham chiếu bằng ID, không phải Customer
    private final Map<ProductId, OrderLine> lines = new LinkedHashMap<>();
    void addLine(ProductId p, int qty, Money price) {
        if (placed) throw new IllegalStateException("Order already placed");
        // ... thêm/tăng dòng, kiểm tra MAX_LINES và tổng tiền ≤ hạn mức (invariant)
        pendingEvents.add(new OrderLineAdded(id, p, qty));
    }
}
```

- Quá lớn → **contention**: nhiều người cùng sửa một aggregate → optimistic lock fail liên tục; load chậm. Quá nhỏ → invariant phải kiểm tra xuyên aggregate (cần saga/kiểm tra bù).
- Ví dụ: "tồn kho không âm" thuộc aggregate `Inventory`, không thuộc `Order`; đặt hàng → event `OrderPlaced` → Inventory reserve, nếu thiếu thì `ReservationFailed` → bù trừ.
- Một repository cho mỗi aggregate root; `OrderLine` không có repository riêng.

**Câu hỏi nối tiếp:**
- *Domain event được publish thế nào?* → Aggregate gom event (`pullEvents()` hoặc `AbstractAggregateRoot.registerEvent()` của Spring Data); publish sau khi save, qua outbox để không mất (Q54).

**⚠️ Câu trả lời gây điểm trừ:** Aggregate = "entity có nhiều quan hệ JPA"; sửa nhiều aggregate trong một transaction mà không biết hệ quả.

**📖 Ôn lại:** [§10.1 Quy tắc thiết kế aggregate & Góc nhìn Senior](../01-giao-trinh/06-design-principles-patterns.md#p10)

</details>

### Q52. 🟡 Bounded context là gì? Nó liên quan thế nào tới ranh giới microservice? Anti-Corruption Layer dùng khi nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Bounded context là ranh giới mà trong đó một model và **ubiquitous language** có nghĩa nhất quán — "Product" ở Catalog (mô tả, ảnh), Inventory (tồn kho), Pricing (giá), Shipping (kích thước) là các model khác nhau. Bounded context là **ứng viên tự nhiên** cho ranh giới microservice (cohesion cao bên trong, coupling lỏng giữa các context), nhưng nên bắt đầu bằng **modular monolith** theo context trước khi tách. **ACL** là lớp dịch khi tích hợp với hệ thống ngoài/legacy để model của họ không "làm bẩn" model của ta.

**Giải thích chi tiết:**
- Context map: Shared Kernel, Customer/Supplier, Conformist, ACL, Open Host Service/Published Language, Separate Ways.
- Subdomain: core (đầu tư DDD), supporting, generic (mua/dùng sẵn: auth, email).
- ACL trong code = adapter + translator: nhận `LegacyCustomerDto` với field mã hóa kiểu cũ, trả domain object của ta, dịch lỗi.

**Câu hỏi nối tiếp:**
- *Một context có thể gồm nhiều service không?* → Có (ví dụ tách phần đọc/ghi hoặc phần scale khác nhau), nhưng một service không nên trải nhiều context.

**⚠️ Câu trả lời gây điểm trừ:** "Mỗi bảng/entity một microservice"; một class `Customer` dùng chung mọi service.

**📖 Ôn lại:** [§10.2 DDD strategic](../01-giao-trinh/06-design-principles-patterns.md#p10)

</details>

### Q53. 🔴 CQRS là gì, khi nào nên và không nên dùng? Xử lý eventual consistency ở phía người dùng thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** CQRS tách **mô hình ghi** (command đi qua aggregate, bảo vệ invariant) khỏi **mô hình đọc** (tối ưu cho hiển thị, denormalize, không đi qua domain model). Mức **nhẹ**: cùng DB, phía đọc dùng DTO projection/jOOQ/view thay vì load aggregate — lợi nhiều, chi phí ít. Mức **đầy đủ**: kho đọc riêng cập nhật bất đồng bộ từ event → eventual consistency. Dùng khi tải hoặc hình dạng dữ liệu đọc/ghi khác nhau rõ rệt, cho *một phần* hệ thống; không dùng cho CRUD đơn giản. Event Sourcing thường đi kèm nhưng **không bắt buộc**.

**Giải thích chi tiết:**
- UX với eventual consistency: trả dữ liệu cần thiết ngay trong **response của command**; read-your-writes (đọc từ write model cho chính user vừa ghi trong vài giây); UI hiển thị trạng thái "đang xử lý"; client polling/push.
- Projector phải **idempotent** (event có thể đến hai lần — at-least-once):

```java
jdbc.update("""
    INSERT INTO order_summary(order_id, customer_id, total, placed_at) VALUES (?,?,?,?)
    ON CONFLICT (order_id) DO NOTHING""", ...);   // PostgreSQL
```

- Rebuild read model: replay event từ outbox/log (Kafka retention, event store).

**Câu hỏi nối tiếp:**
- *Event đến sai thứ tự?* → Partition theo aggregate ID (Kafka giữ thứ tự trong partition), version trong event, bỏ qua event có version cũ hơn.

**⚠️ Câu trả lời gây điểm trừ:** "CQRS = phải có hai database và event sourcing"; không nhắc chi phí và eventual consistency.

**📖 Ôn lại:** [§10.3 CQRS](../01-giao-trinh/06-design-principles-patterns.md#p10)

</details>

### Q54. 🔴 [Tình huống] Sau khi lưu `Order`, bạn cần cập nhật read model và gửi thông báo. Dùng `@TransactionalEventListener(AFTER_COMMIT)` có đủ không? Thiết kế đảm bảo không mất event?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Không đủ cho sự kiện quan trọng: listener `AFTER_COMMIT` chạy *sau* commit trong cùng process — nếu app crash/deploy giữa commit và listener, event **mất vĩnh viễn**; ngược lại publish lên Kafka *trước* commit thì có thể phát event cho dữ liệu đã rollback (dual-write problem). Giải pháp: **Transactional Outbox** — ghi event vào bảng `outbox` **trong cùng transaction** với aggregate; relay (poller `SELECT ... FOR UPDATE SKIP LOCKED` hoặc CDC Debezium) publish ra broker và đánh dấu đã gửi; consumer **idempotent** vì at-least-once.

**Giải thích chi tiết:**
- Spring Modulith có sẵn Event Publication Registry (lưu event vào DB, phát lại event chưa hoàn tất khi khởi động).
- Bảng outbox: `id, aggregate_type, aggregate_id, type, payload, created_at, status/sent_at`; thứ tự theo aggregate giữ bằng key partition = aggregate ID.
- Consumer idempotent: bảng `processed_message(message_id PK)` cùng transaction với xử lý, hoặc upsert/điều kiện version.
- `AFTER_COMMIT` vẫn hợp cho side effect *không quan trọng* (cache evict, metrics).

**Câu hỏi nối tiếp:**
- *Poller chạy nhiều instance?* → `SKIP LOCKED` để mỗi instance lấy batch khác nhau; hoặc ShedLock để một instance chạy.
- *Ghi DB trong listener AFTER_COMMIT?* → Cần `@Transactional(propagation = REQUIRES_NEW)` (Module 07).

**⚠️ Câu trả lời gây điểm trừ:** "Gửi Kafka trong `@Transactional` là được vì có rollback" — Kafka không tham gia transaction DB.

**📖 Ôn lại:** [§10.3 Bài 10.3 & §7.3 Observer](../01-giao-trinh/06-design-principles-patterns.md#p10)

</details>

---

<a id="nhom-j"></a>
## J. Thiết kế API & code review

### Q55. 🟡 Nguyên tắc thiết kế một Java API public (library dùng chung)? Làm sao tiến hóa mà không phá client?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** API là hợp đồng — dễ thêm, rất khó bỏ ("public APIs, like diamonds, are forever"). Nguyên tắc: **tối thiểu bề mặt public** (private/package-private mặc định, `final` class, JPMS `exports` chỉ package API), **immutable**, trả interface (`List` không phải `ArrayList`), trả rỗng thay `null`, kiểm tra tham số sớm, defensive copy, tránh tham số cùng kiểu đứng cạnh nhau (dùng kiểu riêng `AccountId`), Javadoc hợp đồng (null, exception, thread-safety). Tiến hóa: thêm method interface bằng `default`, `@Deprecated(since, forRemoval = true)` trước khi xóa, semantic versioning, kiểm tra binary compatibility tự động (japicmp, Revapi) trong CI.

**Giải thích chi tiết:**
- Thay đổi phá binary compatibility hay gặp: đổi kiểu trả về, thêm method abstract vào interface, đổi class thành interface, thu hẹp visibility.
- Lộ trình đổi `getName()` → `getFirstName()/getLastName()`: thêm mới (expand) → đánh deprecated + đo ai còn dùng → xóa ở major version (contract).

**Câu hỏi nối tiếp:**
- *Overloading gây vấn đề gì?* → Mơ hồ khi gọi với `null` hoặc lambda; Item 52 — ưu tiên tên khác nhau.

**⚠️ Câu trả lời gây điểm trừ:** Không biết khái niệm binary compatibility; đổi chữ ký method public trong bản patch.

**📖 Ôn lại:** [§11.1 Java API](../01-giao-trinh/06-design-principles-patterns.md#p11)

</details>

### Q56. 🔴 [Tình huống] API v1 trả `{"name": "Nguyễn Văn A"}`. Yêu cầu mới cần `firstName`/`lastName` và bỏ `name` sau 6 tháng. Bạn triển khai thế nào để không client nào bị vỡ?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Theo **expand → migrate → contract**: (1) *Expand*: thêm `firstName`, `lastName` song song với `name` (thay đổi không phá vỡ vì client là tolerant reader); (2) *Migrate*: đánh dấu `name` deprecated trong OpenAPI, gửi header `Deprecation`/`Sunset`, thông báo consumer, **đo** ai còn dùng (log/metric theo client id); (3) *Contract*: chỉ xóa `name` khi metric về 0 hoặc hết hạn đã thông báo — nếu cần xóa sớm thì ra `/v2`. Consumer-driven contract test (Pact, Spring Cloud Contract) chặn breaking change trong CI.

**Giải thích chi tiết:**
- Nguyên tắc không phá vỡ: chỉ *thêm* field tùy chọn; không đổi nghĩa field cũ; không đổi kiểu; enum mới có thể phá client parse cứng — tài liệu hóa "client phải chịu được giá trị enum lạ".
- Ghi dữ liệu: trong giai đoạn chuyển tiếp, request gửi `name` vẫn phải được chấp nhận (tách tên theo quy tắc đã thống nhất) hoặc ưu tiên field mới khi có cả hai.
- Phía DB cũng theo expand/contract để rolling deploy an toàn: thêm cột mới → backfill → code đọc cột mới → bỏ cột cũ ở release sau.

**Câu hỏi nối tiếp:**
- *Versioning bằng URI hay header?* → URI (`/v1`) phổ biến, dễ route ở gateway/cache; header/media type sạch hơn nhưng khó dùng; quan trọng hơn là tránh phải version (Module 08).

**⚠️ Câu trả lời gây điểm trừ:** Đổi `name` thành `firstName`/`lastName` ngay trong v1; hoặc tạo v2 cho mọi thay đổi nhỏ.

**📖 Ôn lại:** [§11.2 HTTP/REST API & Bài 11.3](../01-giao-trinh/06-design-principles-patterns.md#p11)

</details>

### Q57. 🟡 Bạn review code thế nào? Thứ tự ưu tiên và cách viết comment?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Mục tiêu (Google Engineering Practices): approve khi thay đổi **cải thiện sức khỏe tổng thể codebase**, dù chưa hoàn hảo. Thứ tự: (1) thiết kế & tính đúng — giải quyết đúng vấn đề, đặt đúng chỗ; (2) biên (null, rỗng, overflow, timezone); (3) concurrency; (4) transaction & dữ liệu (ranh giới tx, N+1, migration tương thích ngược); (5) lỗi & độ bền (timeout, retry idempotent); (6) bảo mật (injection, IDOR, secret); (7) hiệu năng; (8) test; (9) dễ đọc; (10) vận hành (metric, log, rollback). Style giao cho tool. Comment phân loại `blocking:`/`suggestion:`/`nit:`/`question:`/`praise:`, hỏi thay vì ra lệnh, nói về code không nói về người, kèm lý do + đề xuất cụ thể.

**Giải thích chi tiết:**

```text
blocking: `chargeCustomer()` gọi payment gateway bên trong @Transactional.
Gateway chậm 30s → connection DB bị giữ 30s → pool 10 connection cạn khi có 10 request đồng thời.
Đề xuất: (1) tx lưu Order(PENDING) → (2) gọi gateway ngoài tx, timeout 3s → (3) tx cập nhật trạng thái,
kèm Idempotency-Key để retry an toàn.
```

- Vai trò Senior: giữ PR nhỏ (< 400 dòng), phản hồi trong một ngày làm việc, với junior ưu tiên vài điểm quan trọng nhất, biến comment lặp lại thành lint rule/ArchUnit/checklist; thảo luận > 2 vòng thì gọi trực tiếp rồi ghi kết luận vào PR.

**Câu hỏi nối tiếp:**
- *Kể một lần review của bạn ngăn được sự cố?* → Chuẩn bị sẵn câu chuyện cụ thể (race condition, N+1, migration khóa bảng) theo dạng tình huống → phát hiện → hậu quả tránh được.

**⚠️ Câu trả lời gây điểm trừ:** "Em xem format và đặt tên trước"; "LGTM" cho PR 2.000 dòng; chặn PR vì sở thích cá nhân.

**📖 Ôn lại:** [§12 Code review](../01-giao-trinh/06-design-principles-patterns.md#p12)

</details>

### Q58. 🔴 [Đọc code] Review đoạn code sau. Liệt kê vấn đề theo mức độ nghiêm trọng.

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
}
```

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **Blocking**: (1) `static HashMap` dùng đồng thời → không thread-safe; (2) state đếm **trong bộ nhớ** → sai khi chạy nhiều instance/restart; (3) **check-then-act** → vượt limit dưới tải; (4) `catch (Exception) { return false; }` nuốt mọi lỗi, không log — kể cả NPE khi `findByCode` trả null; (5) **gọi HTTP 2–5s trong `@Transactional`** → giữ connection DB, cạn pool; lỗi marketing làm "dùng coupon" thất bại dù không liên quan. **Suggestion**: field injection → constructor injection; `new Date()` → inject `Clock` + `java.time`; trả `boolean` mất lý do (hết hạn? hết lượt? không tồn tại?) → sealed result; không idempotent (bấm hai lần dùng hai lượt).

**Giải thích chi tiết:**
Viết lại phần cốt lõi — đếm lượt bằng **UPDATE có điều kiện**, nguyên tử ở mức DB (row lock), đúng với nhiều instance:

```java
@Modifying
@Query("update Coupon c set c.used = c.used + 1 " +
       "where c.code = :code and c.used < c.limit and c.expiry > :now")
int tryConsume(@Param("code") String code, @Param("now") Instant now);   // 1 = thành công, 0 = hết lượt/hết hạn

@Transactional
public CouponResult use(String code, UserId user) {
    if (redemptions.existsByCodeAndUser(code, user)) return CouponResult.alreadyUsed();   // + unique constraint
    if (couponRepo.tryConsume(code, clock.instant()) == 0) return resolveReason(code);
    redemptions.save(new Redemption(code, user));
    events.publishEvent(new CouponUsed(code, user));      // marketing nghe @TransactionalEventListener(AFTER_COMMIT) + @Async
    return CouponResult.ok();
}
```

- Test chứng minh: 100 thread đồng thời, limit 10 → đúng 10 lần thành công.

**Câu hỏi nối tiếp:**
- *Vì sao không dùng `synchronized` hoặc `ConcurrentHashMap`?* → Chỉ đúng trong một JVM; nhiều pod vẫn vượt limit.
- *Optimistic lock `@Version` thì sao?* → Đúng nhưng dưới tải cao nhiều lần fail-retry; UPDATE có điều kiện gọn hơn cho counter.

**⚠️ Câu trả lời gây điểm trừ:** Chỉ thấy field injection và `Date`; đề xuất `ConcurrentHashMap` như lời giải cuối.

**📖 Ôn lại:** [§12 Bài 12.1 & 12.2](../01-giao-trinh/06-design-principles-patterns.md#p12)

</details>

# Module 15 — Testing & Chất lượng phần mềm

> **Mục tiêu:** sau module này bạn giải thích được vì sao và test cái gì ở từng tầng (unit / integration / contract / E2E), viết được test JUnit 5 + AssertJ + Mockito sạch và bền, biết khi nào **không** nên mock, áp dụng TDD có kỷ luật, tổ chức test Spring Boot nhanh (slice test, context caching, Testcontainers), test được code bất đồng bộ/đa luồng, đo chất lượng test bằng mutation testing thay vì chỉ nhìn coverage, giữ kiến trúc bằng ArchUnit, chẩn đoán flaky test và dựng quality gate (SonarQube, SpotBugs, Checkstyle) trong CI.
> **Cấp độ:** Cơ bản → Nâng cao (Senior)
> **Thời lượng gợi ý:** 7 ngày (≈ 35–40 giờ)
> **Yêu cầu trước:** Module Java Core, Concurrency, Spring Boot, JPA/Database (các module trước trong giáo trình). Biết Maven hoặc Gradle ở mức chạy được `mvn test`.
> **Nguồn tham khảo:**
> - Trong kho: [`Ebook IT/Clean Code.pdf`](../../Ebook%20IT/Clean%20Code.pdf) — Chapter 9 "Unit Tests" (ba luật TDD, F.I.R.S.T., giữ test sạch), Chapter 3 "Functions" (áp dụng khi refactor ở bước Refactor của TDD).
> - Trong kho: [`Ebook IT/Docker - Up _ Running.pdf`](../../Ebook%20IT/Docker%20-%20Up%20_%20Running.pdf) — nền tảng container dùng cho Testcontainers.
> - Ngoài: [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/), [Mockito docs](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html), [AssertJ](https://assertj.github.io/doc/), [Spring Boot — Testing](https://docs.spring.io/spring-boot/reference/testing/index.html), [Spring Framework — Testing](https://docs.spring.io/spring-framework/reference/testing.html), [Testcontainers](https://java.testcontainers.org/), [ArchUnit User Guide](https://www.archunit.org/userguide/html/000_Index.html), [Pact docs](https://docs.pact.io/), [Spring Cloud Contract](https://docs.spring.io/spring-cloud-contract/reference/), [PIT](https://pitest.org/), [JaCoCo](https://www.jacoco.org/jacoco/trunk/doc/), [Awaitility](https://github.com/awaitility/awaitility/wiki/Usage).
> - Sách: Vladimir Khorikov — *Unit Testing Principles, Practices, and Patterns* (Manning, 2020): Chương 1–2 (mục tiêu của unit test, classical vs London), Chương 4 (bốn trụ cột của test tốt), Chương 5 (mock và tính giòn của test), Chương 8 (integration test). Freeman & Pryce — *Growing Object-Oriented Software, Guided by Tests* (GOOS): Phần I–II (outside-in TDD, mock roles not objects).

## Mục lục
1. [Chiến lược kiểm thử: pyramid, trophy và "test cái gì ở đâu"](#p1)
2. [JUnit 5 chuyên sâu + AssertJ](#p2)
3. [Test doubles và Mockito](#p3)
4. [Classical vs London school, over-mocking](#p4)
5. [TDD: Red – Green – Refactor qua một kata](#p5)
6. [Spring Boot testing: full context vs slice, context caching](#p6)
7. [Testcontainers: integration test với DB, Redis, Kafka thật](#p7)
8. [Contract testing: Spring Cloud Contract & Pact](#p8)
9. [Test code bất đồng bộ & đa luồng](#p9)
10. [Coverage (JaCoCo) và mutation testing (PIT)](#p10)
11. [Architecture tests với ArchUnit](#p11)
12. [Performance / load testing: JMeter, Gatling, k6](#p12)
13. [Flaky tests: nguyên nhân gốc và cách trị](#p13)
14. [Static analysis & code review](#p14)
15. [Dự án mini của module](#du-an-mini)
16. [Checklist tự đánh giá](#checklist-tu-danh-gia)

---

<a id="p1"></a>
## 1. Chiến lược kiểm thử: pyramid, trophy và "test cái gì ở đâu"

### 1.1 Vì sao phải test — mục tiêu thật sự
Test không phải để "đạt 80% coverage". Theo Khorikov, mục tiêu của test là **cho phép dự án phát triển bền vững**: khi codebase lớn lên, tốc độ thêm tính năng không bị chậm lại vì sợ làm vỡ thứ khác. Một bộ test tốt:

- Bắt được regression (lỗi tái phát) sớm.
- Cho phép **refactor tự tin** — đây là giá trị lớn nhất và hay bị quên.
- Là tài liệu sống mô tả hành vi hệ thống.

Một bộ test tồi thì ngược lại: chạy chậm, vỡ mỗi lần refactor dù hành vi không đổi (false positive), không bắt được bug thật (false negative) — và team bắt đầu `@Disabled` hàng loạt.

### 1.2 Bốn trụ cột của một test tốt (Khorikov, Ch.4)

| Trụ cột | Ý nghĩa | Test vi phạm điển hình |
|---|---|---|
| **Protection against regressions** | Test chạy qua nhiều code có ý nghĩa nghiệp vụ, bắt được bug | Test getter/setter, test chỉ kiểm tra mock được gọi |
| **Resistance to refactoring** | Không vỡ khi đổi cấu trúc mà hành vi giữ nguyên | Verify thứ tự gọi private collaborator, assert SQL string sinh ra |
| **Fast feedback** | Chạy nhanh | Test khởi động full Spring context cho mỗi class |
| **Maintainability** | Dễ đọc, dễ setup | Test 200 dòng arrange, phụ thuộc DB dùng chung |

Hai trụ cột đầu quyết định **độ chính xác** (accuracy) của test; trụ cột 3 và 4 là chi phí. Điểm mấu chốt: **resistance to refactoring gần như không thể thỏa hiệp** — một test giòn còn tệ hơn không có test, vì nó làm team mất niềm tin vào cả bộ test.

### 1.3 Test pyramid, testing trophy, honeycomb

```
        Pyramid (Mike Cohn)          Trophy (Kent C. Dodds)       Honeycomb (Spotify, microservices)
             /\                           ___                           ___
            /E2E\                        |E2E|                         /   \  Integrated (ít)
           /------\                    __|___|__                      /-----\
          / Integr \                  |Integration|  <- nhiều nhất   | Integ- | <- nhiều nhất
         /----------\                  \_________/                   | ration |
        /    Unit    \                  | Unit  |                     \-----/
       /--------------\                 |Static |                      \___/  Implementation detail (ít)
```

- **Pyramid:** nhiều unit test (nhanh, rẻ), ít integration, rất ít E2E. Phù hợp domain logic phức tạp (ngân hàng, pricing, rule engine).
- **Trophy:** nhấn mạnh integration test + static analysis. Phù hợp app "mỏng logic", chủ yếu glue code CRUD.
- **Honeycomb:** cho microservice — phần lớn là test service ở biên (HTTP vào, DB/Kafka thật qua Testcontainers), ít test chi tiết implementation, ít test "integrated" xuyên nhiều service.

> 💡 **Góc nhìn Senior:** Không có hình dạng "đúng" chung cho mọi hệ thống. Câu hỏi đúng là: *logic phức tạp nằm ở đâu?* Nếu nằm trong domain model → đầu tư unit test. Nếu nằm ở query SQL, mapping JSON, cấu hình Spring Security, transaction boundary → unit test với mock **không chứng minh được gì**, cần integration test. Khi phỏng vấn, trả lời "tùy vào nơi rủi ro tập trung" kèm ví dụ sẽ ghi điểm hơn đọc thuộc hình kim tự tháp.

### 1.4 Test cái gì ở tầng nào

| Tầng | Phạm vi | Dùng gì | Nên test | Không nên test |
|---|---|---|---|---|
| Unit | 1 đơn vị **hành vi** (có thể nhiều class) | JUnit, AssertJ, (ít) Mockito | Domain rule, tính toán, state machine, validation, mapping thuần | Framework (Spring đã test rồi), getter/setter |
| Slice / component | 1 tầng của Spring | `@WebMvcTest`, `@DataJpaTest`, `@JsonTest` | Mapping URL, validation `@Valid`, status code, JSON format, query JPA | Logic nghiệp vụ (đã có unit test) |
| Integration | Service + hạ tầng thật | `@SpringBootTest` + Testcontainers | Transaction, query native, migration Flyway, Kafka serialization, cache | Từng nhánh if/else |
| Contract | Biên giữa 2 service | Pact, Spring Cloud Contract | Request/response mà consumer phụ thuộc | Logic nội bộ của provider |
| E2E | Toàn hệ thống | Selenium/Playwright, REST-assured trên staging | Vài luồng "happy path" sống còn (đặt hàng, thanh toán) | Mọi biến thể — quá chậm, quá flaky |
| Non-functional | Hiệu năng, bảo mật | Gatling, k6, ZAP | Latency p99, throughput, leak | — |

**Unit là gì?** Hai trường phái (chi tiết ở phần 4):
- **Classical (Detroit):** unit = một đơn vị *hành vi*, có thể gồm nhiều class; chỉ cô lập *các test với nhau* (không chia sẻ state), chỉ mock dependency **shared/out-of-process** (DB, SMTP, message bus).
- **London (mockist):** unit = một class; mock mọi collaborator.

### 1.5 Phân loại code để quyết định chiến lược (Khorikov, Ch.7)

```
                     Nhiều collaborator
                           ^
     Controllers          |      Overcomplicated code
  (orchestration,         |      (vừa logic vừa I/O)  -> REFACTOR tách ra
   integration test)      |
  ------------------------+------------------------------> Độ phức tạp / ý nghĩa domain
     Trivial code          |      Domain model / algorithms
  (không cần test)         |      (unit test kỹ nhất)
```

Mục tiêu thiết kế: đẩy logic về góc phải-dưới (domain thuần, không I/O → dễ unit test), để controller/application service mỏng (test bằng integration test vài case). Đây chính là "Functional core, imperative shell" / Hexagonal architecture.

```java
// ❌ Overcomplicated: vừa tính toán vừa gọi I/O -> phải mock mọi thứ mới test được
public class OrderService {
    public void checkout(long orderId) {
        Order o = repo.findById(orderId).orElseThrow();
        BigDecimal discount = BigDecimal.ZERO;
        if (o.total().compareTo(new BigDecimal("1000000")) > 0 && customerClient.isVip(o.customerId())) {
            discount = o.total().multiply(new BigDecimal("0.1"));
        }
        o.setTotal(o.total().subtract(discount));
        repo.save(o);
        mailer.send(o.customerEmail(), "Thanks");
    }
}

// ✅ Tách: logic thuần nằm ở domain, service chỉ điều phối
public record Pricing(BigDecimal vipThreshold, BigDecimal vipRate) {
    public BigDecimal finalTotal(BigDecimal total, boolean vip) {
        if (vip && total.compareTo(vipThreshold) > 0) {
            return total.subtract(total.multiply(vipRate));
        }
        return total;
    }
}
// -> Pricing test bằng unit test thuần, 0 mock, chạy trong micro giây.
```

> ⚠️ **Lỗi thường gặp:**
> - "Ice-cream cone": rất nhiều test E2E/manual, ít unit test → suite chạy 1 tiếng, flaky, không ai tin.
> - Viết unit test cho controller bằng cách mock service rồi `verify(service).doX()` — test này chỉ lặp lại implementation, không bảo vệ gì.
> - Đo "unit test" bằng số class được test thay vì số **hành vi** được test.

### 🛠 Bài tập phần 1

**Bài 1.1 — Phân loại test (Cơ bản)**
- Đề bài: Cho danh sách 10 test của một dự án (tự liệt kê từ project bạn đang làm, hoặc dùng: test getter User, test `PriceCalculator.applyCoupon`, test `GET /orders/{id}` trả 404, test query `findTopByStatus`, test gửi email khi đăng ký, test Flyway migration, test luồng đặt hàng trên staging, test JSON của `OrderDto`, test `@PreAuthorize` của endpoint admin, test retry khi gọi payment timeout).
- Yêu cầu / tiêu chí đạt: Gán mỗi test vào tầng phù hợp (unit/slice/integration/contract/E2E/không cần test) và giải thích 1 câu cho mỗi lựa chọn dựa trên 4 trụ cột.

**Bài 1.2 — Đánh giá test theo 4 trụ cột (Trung bình)**
- Đề bài: Viết 2 test cho class `OrderService` ở ví dụ 1.5 (bản ❌): một bản mock toàn bộ (repo, customerClient, mailer) và verify từng lời gọi; một bản sau khi refactor sang `Pricing`.
- Yêu cầu: Chấm điểm cả hai theo 4 trụ cột (thang 1–5). Sau đó đổi implementation (ví dụ gọi `customerClient.isVip` **trước** khi kiểm tra ngưỡng) và quan sát test nào vỡ dù hành vi không đổi.

**Bài 1.3 — Chiến lược test cho một microservice (Nâng cao)**
- Đề bài: Viết một tài liệu chiến lược test 1 trang cho service "Payment" (REST API, PostgreSQL, publish event Kafka, gọi cổng thanh toán bên ngoài).
- Yêu cầu: Chỉ ra hình dạng (pyramid/honeycomb), công cụ cho từng tầng, cái gì mock/cái gì dùng thật, thời gian chạy mục tiêu cho từng stage CI (vd: unit < 1 phút, integration < 5 phút), và cách xử lý cổng thanh toán bên ngoài (WireMock + contract test).

<details>
<summary>Gợi ý lời giải</summary>

- 1.1: getter → không cần test; `applyCoupon` → unit; 404 → `@WebMvcTest`; `findTopByStatus` → `@DataJpaTest` + Testcontainers; email khi đăng ký → unit với fake mailer hoặc integration với GreenMail; Flyway → integration; luồng đặt hàng → E2E (1 case); JSON DTO → `@JsonTest`; `@PreAuthorize` → `@WebMvcTest` + `@WithMockUser` hoặc `@SpringBootTest` (method security cần proxy); retry → unit với fake clock hoặc integration với WireMock trả lỗi.
- 1.2: bản mock-all thường được Resistance to refactoring = 1–2 vì `verify(customerClient).isVip(...)` ràng buộc thứ tự/sự kiện gọi; khi đổi thứ tự hoặc short-circuit, `verify` hay `InOrder` vỡ. Bản `Pricing` đạt 5/5 ở trụ cột 2,3,4.
- 1.3: Honeycomb. Unit cho `Money`, state machine `PaymentStatus`. Integration: `@SpringBootTest` + PostgreSQL & Kafka Testcontainers + WireMock cho gateway. Contract: Pact giữa Order-service (consumer) và Payment. E2E: 1 luồng trên staging với sandbox của gateway. Mock duy nhất ở biên **unmanaged dependency** (gateway bên ngoài); DB là **managed dependency** → dùng thật.

</details>

---

<a id="p2"></a>
## 2. JUnit 5 chuyên sâu + AssertJ

### 2.1 Kiến trúc JUnit 5
JUnit 5 = **JUnit Platform** (nền tảng chạy test, `Launcher`, tích hợp IDE/Maven Surefire/Gradle) + **JUnit Jupiter** (API & engine viết test mới: `org.junit.jupiter.api.*`) + **JUnit Vintage** (engine chạy test JUnit 3/4 cũ). Nhờ tách Platform, Spock, Cucumber, ArchUnit… đều có engine riêng chạy chung.

> Ghi chú phiên bản: JUnit 6.0 (phát hành cuối 2025) thống nhất version cho Platform/Jupiter/Vintage và yêu cầu Java 17+. API Jupiter về cơ bản giữ nguyên; mọi khái niệm trong phần này áp dụng cho cả 5.x và 6.x. Luôn import BOM `org.junit:junit-bom` để các artifact cùng version.

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.junit</groupId>
      <artifactId>junit-bom</artifactId>
      <version>5.11.4</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
<dependencies>
  <dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
  </dependency>
  <dependency>
    <groupId>org.assertj</groupId>
    <artifactId>assertj-core</artifactId>
    <version>3.26.3</version>
    <scope>test</scope>
  </dependency>
</dependencies>
```
(Với Spring Boot, `spring-boot-starter-test` đã kéo sẵn JUnit Jupiter, AssertJ, Mockito, Hamcrest, JSONassert, JsonPath với version được quản lý.)

### 2.2 Lifecycle

```java
import org.junit.jupiter.api.*;

class LifecycleDemoTest {

    @BeforeAll   // static (mặc định), chạy 1 lần trước mọi test của class
    static void initAll() { System.out.println("beforeAll"); }

    @BeforeEach  // chạy trước MỖI test, trên instance mới
    void init() { System.out.println("beforeEach " + this.hashCode()); }

    @Test void a() { System.out.println("a"); }
    @Test void b() { System.out.println("b"); }

    @AfterEach  void tearDown() { System.out.println("afterEach"); }
    @AfterAll   static void tearDownAll() { System.out.println("afterAll"); }
}
```

- Mặc định `@TestInstance(Lifecycle.PER_METHOD)`: **mỗi test method chạy trên một instance mới** → field không bị rò state giữa các test. Đây là cơ chế cách ly quan trọng nhất.
- `@TestInstance(Lifecycle.PER_CLASS)`: một instance cho cả class → `@BeforeAll` không cần static (tiện với Kotlin, với `@Nested`), nhưng phải tự reset state.
- Thứ tự chạy các test method là **xác định nhưng cố ý không hiển nhiên**. Đừng phụ thuộc thứ tự; nếu thật sự cần (test kịch bản), dùng `@TestMethodOrder(OrderAnnotation.class)` + `@Order`.

### 2.3 Các annotation hay dùng

```java
@DisplayName("Giỏ hàng")
class CartTest {

    @Test
    @DisplayName("tổng tiền của giỏ rỗng là 0")
    void emptyCartTotalIsZero() { ... }

    @Test @Tag("slow") void heavyComputation() { ... }   // lọc bằng Surefire <groups>/<excludedGroups>

    @Test @Disabled("JIRA-123: chờ fix upstream") void pending() { }

    @RepeatedTest(50) void shouldBeStable() { ... }      // săn flaky test

    @Test @Timeout(value = 2, unit = TimeUnit.SECONDS) void fast() { ... }

    @Test void writesFile(@TempDir Path dir) throws IOException {   // thư mục tạm tự xóa
        Path f = dir.resolve("out.txt");
        Files.writeString(f, "hello");
        assertThat(f).hasContent("hello");
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    @EnabledIfEnvironmentVariable(named = "CI", matches = "true")
    void onlyOnCiLinux() { }
}
```

### 2.4 Parameterized tests

```java
class PasswordPolicyTest {

    private final PasswordPolicy policy = new PasswordPolicy(8);

    @ParameterizedTest(name = "[{index}] \"{0}\" hợp lệ")
    @ValueSource(strings = {"Abcdef1!", "S3cure#Pass", "xY9$xY9$"})
    void validPasswords(String pwd) {
        assertThat(policy.isValid(pwd)).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "short1!"})
    void invalidPasswords(String pwd) {
        assertThat(policy.isValid(pwd)).isFalse();
    }

    @ParameterizedTest(name = "{0} VND, vip={1} -> {2}")
    @CsvSource(textBlock = """
        500000,  false, 500000
        2000000, false, 2000000
        2000000, true,  1800000
        """)
    void pricing(BigDecimal total, boolean vip, BigDecimal expected) {
        var pricing = new Pricing(new BigDecimal("1000000"), new BigDecimal("0.1"));
        assertThat(pricing.finalTotal(total, vip)).isEqualByComparingTo(expected);
    }

    @ParameterizedTest
    @MethodSource("transitions")
    void stateMachine(OrderStatus from, OrderEvent event, OrderStatus to) {
        assertThat(from.on(event)).isEqualTo(to);
    }

    static Stream<Arguments> transitions() {
        return Stream.of(
            Arguments.of(OrderStatus.NEW, OrderEvent.PAY, OrderStatus.PAID),
            Arguments.of(OrderStatus.PAID, OrderEvent.SHIP, OrderStatus.SHIPPED)
        );
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = {"SHIPPED", "CANCELLED"})
    void terminalStatesCannotBeCancelled(OrderStatus s) {
        assertThatThrownBy(() -> s.on(OrderEvent.CANCEL))
            .isInstanceOf(IllegalStateException.class);
    }
}
```
Cần thêm artifact `junit-jupiter-params` (đã có trong `junit-jupiter` aggregate). `@CsvSource` chuyển đổi kiểu ngầm định (String → BigDecimal, enum, LocalDate…); kiểu tự định nghĩa thì dùng `ArgumentConverter` hoặc `ArgumentsAggregator`.

### 2.5 `@Nested` — tổ chức test theo ngữ cảnh

```java
@DisplayName("Account")
class AccountTest {

    Account account;

    @BeforeEach void newAccount() { account = new Account(); }

    @Nested
    @DisplayName("khi số dư = 0")
    class WhenEmpty {
        @Test void withdrawFails() {
            assertThatThrownBy(() -> account.withdraw(Money.of(1)))
                .isInstanceOf(InsufficientFundsException.class);
        }
    }

    @Nested
    @DisplayName("sau khi nạp 100")
    class AfterDeposit {
        @BeforeEach void deposit() { account.deposit(Money.of(100)); }  // chạy SAU BeforeEach của lớp ngoài

        @Test void balanceIs100() { assertThat(account.balance()).isEqualTo(Money.of(100)); }

        @Test void canWithdraw50() {
            account.withdraw(Money.of(50));
            assertThat(account.balance()).isEqualTo(Money.of(50));
        }
    }
}
```
`@Nested` phải là **inner class không static**. Báo cáo test sẽ hiện dạng cây "Account > sau khi nạp 100 > canWithdraw50" — đọc như spec.

### 2.6 Assertions: JUnit vs AssertJ

```java
// JUnit built-in
assertEquals(3, list.size());
assertThrows(IllegalArgumentException.class, () -> parse("x"));
assertAll("user",
    () -> assertEquals("An", user.name()),
    () -> assertEquals(30, user.age()));   // chạy hết, báo mọi lỗi cùng lúc

// AssertJ — fluent, message lỗi rõ hơn, nhiều assertion chuyên biệt
assertThat(list).hasSize(3)
                .extracting(Order::status)
                .containsExactlyInAnyOrder(NEW, PAID, NEW);

assertThat(orders)
    .filteredOn(o -> o.total().compareTo(BigDecimal.TEN) > 0)
    .extracting(Order::id, Order::status)
    .containsExactly(tuple(1L, PAID), tuple(3L, NEW));

assertThatThrownBy(() -> service.withdraw(acc, Money.of(1_000)))
    .isInstanceOf(InsufficientFundsException.class)
    .hasMessageContaining("balance")
    .hasNoCause();

assertThatCode(() -> validator.validate(dto)).doesNotThrowAnyException();

assertThat(actualDto)
    .usingRecursiveComparison()
    .ignoringFields("id", "createdAt")
    .isEqualTo(expectedDto);

assertThat(Optional.of("x")).contains("x");
assertThat(new BigDecimal("1.0")).isEqualByComparingTo("1.00");   // equals() của BigDecimal so cả scale!

// Soft assertions: thu thập mọi lỗi
SoftAssertions.assertSoftly(s -> {
    s.assertThat(user.name()).isEqualTo("An");
    s.assertThat(user.email()).endsWith("@x.com");
});
```

> 💡 **Góc nhìn Senior:** Message lỗi là "UX của test". Khi CI đỏ lúc 2h sáng, `expected: <true> but was: <false>` (từ `assertTrue(list.contains(x))`) vô dụng; `Expecting ArrayList: [...] to contain: [...] but could not find: [...]` của AssertJ cho biết ngay vấn đề. Quy tắc team: cấm `assertTrue(a.equals(b))`, dùng assertion chuyên biệt. Có thể tự viết custom assertion (`AbstractAssert`) cho domain object hay dùng: `assertThat(order).isPaid().hasTotal("100.00")`.

### 2.7 Extension model
JUnit 4 có `@RunWith` (chỉ 1 runner) và `@Rule`. JUnit 5 thay bằng **Extension API** — nhiều extension cùng lúc, mỗi extension implement các callback:

| Interface | Khi nào được gọi |
|---|---|
| `BeforeAllCallback`, `BeforeEachCallback`, `AfterEachCallback`, `AfterAllCallback` | Quanh lifecycle |
| `BeforeTestExecutionCallback` / `AfterTestExecutionCallback` | Sát quanh thân test (đo thời gian) |
| `ParameterResolver` | Inject tham số vào constructor/method test |
| `TestInstancePostProcessor` | Sau khi tạo instance (Spring dùng để inject `@Autowired`) |
| `ExecutionCondition` | Quyết định bật/tắt test |
| `TestExecutionExceptionHandler` | Xử lý exception từ test |
| `TestWatcher` | Nhận kết quả test (chụp screenshot khi fail…) |

Ví dụ extension đo thời gian và inject `Clock` cố định:

```java
public class FixedClockExtension implements ParameterResolver, BeforeTestExecutionCallback, AfterTestExecutionCallback {

    private static final ExtensionContext.Namespace NS = ExtensionContext.Namespace.create(FixedClockExtension.class);

    @Override
    public boolean supportsParameter(ParameterContext pc, ExtensionContext ec) {
        return pc.getParameter().getType() == Clock.class;
    }

    @Override
    public Object resolveParameter(ParameterContext pc, ExtensionContext ec) {
        return Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
    }

    @Override
    public void beforeTestExecution(ExtensionContext ctx) {
        ctx.getStore(NS).put("start", System.nanoTime());
    }

    @Override
    public void afterTestExecution(ExtensionContext ctx) {
        long start = ctx.getStore(NS).remove("start", long.class);
        long ms = (System.nanoTime() - start) / 1_000_000;
        if (ms > 500) System.err.printf("SLOW TEST %s: %d ms%n", ctx.getDisplayName(), ms);
    }
}

@ExtendWith(FixedClockExtension.class)
class InvoiceTest {
    @Test void dueDateIs30DaysLater(Clock clock) {
        var invoice = Invoice.issue(clock);
        assertThat(invoice.dueDate()).isEqualTo(LocalDate.of(2026, 1, 31));
    }
}
```
- `@ExtendWith` (khai báo) vs `@RegisterExtension` (field, cho phép cấu hình bằng code, ví dụ `WireMockExtension.newInstance().options(...).build()`).
- Dữ liệu giữa các callback lưu trong `ExtensionContext.Store` (đừng dùng field static — vỡ khi chạy song song).
- Tạo **meta-annotation** để gom: `@Target(TYPE) @Retention(RUNTIME) @ExtendWith(...) @Tag("integration") public @interface IntegrationTest {}`.

### 2.8 Chạy song song

```properties
# src/test/resources/junit-platform.properties
junit.jupiter.execution.parallel.enabled=true
junit.jupiter.execution.parallel.mode.default=same_thread
junit.jupiter.execution.parallel.mode.classes.default=concurrent
junit.jupiter.execution.parallel.config.strategy=dynamic
```
Cấu hình trên chạy các class song song, method trong class tuần tự — an toàn hơn. Tài nguyên dùng chung (file, port, system property) khai báo bằng `@ResourceLock("SYSTEM_PROPERTIES")`; test không thể chạy song song đánh dấu `@Isolated`.

> ⚠️ **Lỗi thường gặp:**
> - Quên `@Test` từ `org.junit.jupiter.api` mà import nhầm `org.junit.Test` (JUnit 4) → test không chạy nếu thiếu Vintage engine, **build vẫn xanh**. Bật `failIfNoTests`/kiểm tra số test trong báo cáo CI.
> - Surefire quá cũ (< 2.22) không nhận JUnit Platform → 0 test chạy.
> - `assertTimeoutPreemptive` chạy code trong **thread khác** → `ThreadLocal`, `@Transactional` của Spring, `SecurityContext` không có hiệu lực bên trong. Dùng `assertTimeout` (không preemptive) hoặc `@Timeout`.
> - `assertEquals(expected, actual)` đảo tham số → message lỗi gây hiểu nhầm. AssertJ tránh được vì `assertThat(actual)` rõ ràng.
> - So sánh `BigDecimal` bằng `isEqualTo` → `1.0 != 1.00`.

### 🛠 Bài tập phần 2

**Bài 2.1 — Parameterized test cho bộ chuyển số thành chữ (Cơ bản)**
- Đề bài: Viết `VietnameseNumberSpeller.spell(long n)` cho 0–999 (vd 105 → "một trăm linh năm", 21 → "hai mươi mốt", 15 → "mười lăm") và test bằng `@CsvSource` ít nhất 15 case, có `name` hiển thị rõ ràng.
- Tiêu chí: các case biên (0, 10, 11, 15, 21, 24, 25, 100, 101, 110, 999) đều có; test cho số âm dùng `assertThatThrownBy`.

**Bài 2.2 — `@Nested` + custom assertion (Trung bình)**
- Đề bài: Viết test cho `ShoppingCart` (add, remove, applyCoupon, checkout) tổ chức bằng `@Nested` theo trạng thái (rỗng / có hàng / đã áp coupon / đã checkout). Viết `CartAssert extends AbstractAssert<CartAssert, ShoppingCart>` với `hasItemCount`, `hasTotal`.
- Tiêu chí: báo cáo test đọc được như spec; không có test nào có hơn 1 hành động "Act".

**Bài 2.3 — Extension tự viết (Nâng cao)**
- Đề bài: Viết `@RetryOnFailure(times = 3)` — **không phải** để giấu flaky test, mà để báo cáo: test pass sau khi retry vẫn bị ghi vào file `flaky-report.json` (tên test, số lần thử, exception). Gợi ý: `TestExecutionExceptionHandler` + `InvocationInterceptor`.
- Tiêu chí: extension dùng `ExtensionContext.Store`, an toàn khi bật parallel execution; có test cho chính extension bằng `EngineTestKit` (artifact `junit-platform-testkit`).

<details>
<summary>Gợi ý lời giải</summary>

- 2.1: tách hàm `spellTens`, `spellUnits`; quy tắc "mốt" (đơn vị 1 khi hàng chục ≥ 2), "lăm" (đơn vị 5 khi hàng chục ≥ 1), "linh" (hàng chục 0, hàng trăm > 0, đơn vị > 0).
- 2.2:
```java
public class CartAssert extends AbstractAssert<CartAssert, ShoppingCart> {
    private CartAssert(ShoppingCart actual) { super(actual, CartAssert.class); }
    public static CartAssert assertThat(ShoppingCart c) { return new CartAssert(c); }
    public CartAssert hasItemCount(int n) {
        isNotNull();
        if (actual.items().size() != n)
            failWithMessage("Expected cart to have <%s> items but had <%s>: %s", n, actual.items().size(), actual.items());
        return this;
    }
}
```
- 2.3: `InvocationInterceptor.interceptTestMethod(invocation, ...)` chỉ gọi `invocation.proceed()` được **một lần**; để retry, dùng `TestTemplateInvocationContextProvider` (giống `@RepeatedTest`) hoặc tham khảo cách thư viện `junit-pioneer` (`@RetryingTest`) làm. Ghi kết quả vào Store gốc (`ctx.getRoot().getStore(...)`) và flush trong `AfterAllCallback`/`CloseableResource`. `EngineTestKit.engine("junit-jupiter").selectors(selectClass(X.class)).execute().testEvents().assertStatistics(s -> s.succeeded(1))`.

</details>

---

<a id="p3"></a>
## 3. Test doubles và Mockito

### 3.1 Năm loại test double (Gerard Meszaros, *xUnit Test Patterns*)

| Loại | Mục đích | Ví dụ |
|---|---|---|
| **Dummy** | Chỉ để lấp tham số, không dùng | `new Order(null /*auditor không dùng*/)` |
| **Stub** | Trả dữ liệu đóng sẵn cho **input** của SUT | `when(rateApi.rate("USD")).thenReturn(25_000)` |
| **Spy** | Stub + ghi lại lời gọi để kiểm tra sau | `FakeMailer` có `List<Mail> sent` |
| **Mock** | Kiểm tra **output/side-effect** (lời gọi ra ngoài) | `verify(mailer).send(...)` |
| **Fake** | Implementation chạy được nhưng đơn giản | `InMemoryOrderRepository` dùng `HashMap` |

Phân biệt cốt lõi theo Khorikov: **stub mô phỏng dữ liệu đi vào** (incoming interaction) → **không bao giờ verify stub** (verify lời gọi query là ràng buộc vào implementation). **Mock mô phỏng tương tác đi ra** (outgoing, có side-effect) → verify là hợp lý **nếu** tương tác đó là một phần hành vi quan sát được từ bên ngoài (vd gửi email, publish event sang hệ thống khác).

Đây cũng là nguyên lý **Command–Query Separation**: query → stub; command → mock.

### 3.2 Mockito cơ bản

```java
@ExtendWith(MockitoExtension.class)   // strict stubs mặc định
class TransferServiceTest {

    @Mock AccountRepository accounts;
    @Mock EventPublisher events;
    @InjectMocks TransferService service;   // inject qua constructor (ưu tiên), setter, field

    @Test
    void transfersMoneyAndPublishesEvent() {
        var from = new Account(1L, Money.of(100));
        var to   = new Account(2L, Money.of(0));
        when(accounts.findById(1L)).thenReturn(Optional.of(from));   // stub
        when(accounts.findById(2L)).thenReturn(Optional.of(to));

        service.transfer(1L, 2L, Money.of(30));

        assertThat(from.balance()).isEqualTo(Money.of(70));          // assert state
        assertThat(to.balance()).isEqualTo(Money.of(30));
        verify(events).publish(new MoneyTransferred(1L, 2L, Money.of(30)));  // verify command ra ngoài
        verifyNoMoreInteractions(events);
    }

    @Test
    void failsWhenAccountMissing() {
        when(accounts.findById(anyLong())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.transfer(1L, 2L, Money.of(1)))
            .isInstanceOf(AccountNotFoundException.class);
        verifyNoInteractions(events);
    }
}
```

Các API hay dùng:

```java
when(repo.findById(1L)).thenReturn(Optional.of(a), Optional.empty()); // lần 1, lần 2+
when(client.call()).thenThrow(new TimeoutException());
when(client.call()).thenAnswer(inv -> { Thread.sleep(10); return "ok"; });
doThrow(new IOException()).when(storage).delete("x");                 // cho method void
doNothing().when(storage).delete(any());

verify(mailer, times(2)).send(any());
verify(mailer, never()).send(argThat(m -> m.to().endsWith("@spam.com")));
verify(mailer, timeout(1000)).send(any());                            // chờ tối đa 1s (async)
InOrder inOrder = inOrder(audit, repo);
inOrder.verify(audit).log(any());
inOrder.verify(repo).save(any());

// Argument matchers: nếu một tham số dùng matcher thì TẤT CẢ phải là matcher
verify(repo).save(eq(42L), any(Order.class));  // ✅
// verify(repo).save(42L, any(Order.class));   // ❌ InvalidUseOfMatchersException
```

### 3.3 ArgumentCaptor

```java
@Captor ArgumentCaptor<Email> emailCaptor;

@Test
void sendsWelcomeEmail() {
    service.register(new RegisterCommand("an@x.com", "An"));

    verify(mailer).send(emailCaptor.capture());
    Email sent = emailCaptor.getValue();
    assertThat(sent.to()).isEqualTo("an@x.com");
    assertThat(sent.subject()).contains("Chào mừng");
    assertThat(sent.body()).contains("An");
}
```
Dùng captor khi đối tượng được tạo **bên trong** SUT và cần assert nhiều field. Không dùng captor với `when(...)` (stub) — dùng matcher. Với nhiều lần gọi: `captor.getAllValues()`.

### 3.4 Spy và những cái bẫy

```java
List<String> real = new ArrayList<>();
List<String> spy = spy(real);

when(spy.get(0)).thenReturn("x");     // ❌ gọi spy.get(0) THẬT trước -> IndexOutOfBoundsException
doReturn("x").when(spy).get(0);        // ✅ không gọi phương thức thật
```

- `spy()` **sao chép** state của object gốc; sửa `real` sau đó không ảnh hưởng `spy`.
- Spy trên class có `final` method: Mockito 5 dùng inline mock maker mặc định nên mock được final class/method, nhưng tốn chi phí instrumentation.
- Partial mock là **mùi thiết kế**: nếu cần stub một nửa class để test nửa kia, class đó đang làm 2 việc → tách ra.

### 3.5 Mock static, constructor, final

```java
@Test
void usesFixedUuid() {
    UUID fixed = UUID.fromString("00000000-0000-0000-0000-000000000001");
    try (MockedStatic<UUID> mocked = mockStatic(UUID.class)) {   // chỉ có hiệu lực trong thread hiện tại + trong try
        mocked.when(UUID::randomUUID).thenReturn(fixed);
        assertThat(idGenerator.next()).isEqualTo(fixed.toString());
    }
}

try (MockedConstruction<HttpClientWrapper> mc = mockConstruction(HttpClientWrapper.class,
        (mock, ctx) -> when(mock.get(any())).thenReturn("{}"))) {
    service.fetch();
    assertThat(mc.constructed()).hasSize(1);
}
```

> 💡 **Góc nhìn Senior:** `mockStatic` là công cụ **chữa cháy** cho legacy code, không phải thiết kế. Phụ thuộc vào thời gian, UUID, random → inject `Clock`, `Supplier<UUID>`, `RandomGenerator`. Code mới mà cần `mockStatic` = thiết kế có vấn đề. Lưu ý: static mock chỉ áp dụng cho **thread gọi `mockStatic`** — code chạy trong `CompletableFuture`/executor sẽ thấy method thật.

### 3.6 Strict stubs
`MockitoExtension` mặc định dùng `Strictness.STRICT_STUBS`:
- Stub không được dùng trong test → `UnnecessaryStubbingException` (giữ test gọn, phát hiện test đã lỗi thời).
- Stub với argument A nhưng code gọi với argument B → `PotentialStubbingProblem` (báo sớm thay vì trả `null` khó hiểu).
- Nới lỏng có chủ đích: `lenient().when(...)` hoặc `@MockitoSettings(strictness = Strictness.LENIENT)` — hạn chế, phải có lý do.

### 3.7 Mockito trên JDK hiện đại
- Mockito 5: mockito-core dùng **inline mock maker** mặc định (không cần `mockito-inline` riêng), yêu cầu Java 11+.
- Từ JDK 21, việc agent tự attach động (dynamic agent loading) sinh cảnh báo và trong tương lai sẽ bị chặn mặc định (JEP 451). Mockito khuyến nghị cấu hình nó làm `-javaagent` trong Surefire:

```xml
<plugin>
  <artifactId>maven-dependency-plugin</artifactId>
  <executions><execution><goals><goal>properties</goal></goals></execution></executions>
</plugin>
<plugin>
  <artifactId>maven-surefire-plugin</artifactId>
  <configuration>
    <argLine>@{argLine} -javaagent:${org.mockito:mockito-core:jar}</argLine>
  </configuration>
</plugin>
```
(`@{argLine}` giữ lại argLine do JaCoCo đặt vào — xem phần 10.)

> ⚠️ **Lỗi thường gặp:**
> - Mock **value object / entity** (`mock(Money.class)`, `mock(Order.class)`) → chỉ cần `new` chúng. Chỉ mock *role* (interface) ở ranh giới.
> - Mock kiểu bạn không sở hữu (`RestTemplate`, `JdbcTemplate`, `KafkaTemplate`) — GOOS: "Only mock types you own". Mock chúng thì test chỉ kiểm tra bạn gọi API theo cách bạn *nghĩ* là đúng. Hãy bọc bằng adapter của bạn và test adapter bằng integration test (WireMock, Testcontainers).
> - `when(mock.method())` trên `@Mock` mà quên `MockitoExtension` → NPE vì field null.
> - `@InjectMocks` im lặng không inject nếu không khớp kiểu → field null. Ưu tiên tự `new Service(mockA, mockB)` trong `@BeforeEach` cho rõ ràng.
> - Verify lời gọi query (`verify(repo).findById(1L)`) — vô nghĩa và giòn.

### 🛠 Bài tập phần 3

**Bài 3.1 — Phân loại double (Cơ bản)**
- Đề bài: Viết `NotificationService.notifyOverdue(LocalDate today)`: lấy hóa đơn quá hạn từ `InvoiceRepository`, gửi SMS qua `SmsGateway` cho từng khách, ghi log qua `AuditLog`. Viết test sử dụng: 1 stub, 1 mock, 1 fake (`InMemoryAuditLog`).
- Tiêu chí: không có `verify` nào trên stub; test pass với `MockitoExtension` strict.

**Bài 3.2 — ArgumentCaptor & InOrder (Trung bình)**
- Đề bài: `CheckoutService` phải (1) trừ kho, (2) tạo payment, (3) publish `OrderPlaced` **chỉ khi** payment thành công. Viết test chứng minh: event chứa đúng orderId/total (dùng captor), event không publish khi payment fail, và kho được hoàn lại khi payment fail.
- Tiêu chí: mỗi test có 1 lý do để fail; giải thích vì sao ở đây dùng `InOrder` là hợp lý (hay không).

**Bài 3.3 — Gỡ `mockStatic` khỏi legacy (Nâng cao)**
- Đề bài: Cho class legacy dùng `LocalDateTime.now()`, `UUID.randomUUID()` và `new RestTemplate()` bên trong method. Bước 1: viết characterization test bằng `mockStatic`/`mockConstruction`. Bước 2: refactor để inject `Clock`, `IdGenerator`, `RateClient`. Bước 3: viết lại test không dùng static mock, xóa test cũ.
- Tiêu chí: test bước 1 và bước 3 cùng pass trên cùng hành vi; diff refactor không đổi public API ngoài constructor.

<details>
<summary>Gợi ý lời giải</summary>

- 3.1: `InvoiceRepository` → stub (`when(repo.findOverdue(today)).thenReturn(...)`), `SmsGateway` → mock (`verify(sms).send("0901...", contains("quá hạn"))`), `AuditLog` → fake, assert `fakeAudit.entries()`.
- 3.2: `InOrder` hợp lý cho "trừ kho trước khi publish" chỉ nếu thứ tự là yêu cầu nghiệp vụ quan sát được (ví dụ consumer của event giả định kho đã trừ). Nếu không, tránh `InOrder`. Test fail-path: `when(payment.charge(any())).thenReturn(PaymentResult.declined())` → `verify(events, never()).publish(any())` và `verify(inventory).release(orderId)`.
- 3.3: Sau refactor:
```java
public LegacyReport(Clock clock, Supplier<UUID> ids, RateClient rates) { ... }
// test
var svc = new LegacyReport(Clock.fixed(Instant.parse("2026-03-01T00:00:00Z"), ZoneOffset.UTC),
                           () -> UUID.fromString("..."), currency -> new BigDecimal("25000"));
```
Giữ constructor cũ không tham số gọi constructor mới với `Clock.systemUTC()`… để không vỡ caller.

</details>

---

<a id="p4"></a>
## 4. Classical vs London school, over-mocking

### 4.1 Hai trường phái

| | Classical (Detroit, Kent Beck) | London (mockist, GOOS) |
|---|---|---|
| Unit | Đơn vị hành vi | Một class |
| Cô lập | Cô lập các **test** với nhau | Cô lập **class** khỏi collaborator |
| Double | Chỉ cho shared/out-of-process dependency | Cho mọi collaborator mutable |
| Ưu | Test bền với refactor, ít setup giả | Lỗi chỉ ra chính xác class hỏng; thúc đẩy thiết kế interface (outside-in) |
| Nhược | Một bug có thể làm đỏ nhiều test | Test gắn chặt implementation → giòn; dễ "test passes, system broken" |

GOOS dùng mock như **công cụ thiết kế**: viết acceptance test từ ngoài vào, khi cần một collaborator chưa tồn tại thì khai báo *role* (interface) và mock nó → khám phá protocol giữa các object. Câu nổi tiếng: **"Mock roles, not objects"** và **"Only mock types you own"**.

Khorikov tổng hợp: mock chỉ nên dùng cho **unmanaged dependency** — dependency out-of-process mà tương tác với nó *quan sát được từ bên ngoài* (SMTP, message bus tới hệ thống khác, API bên thứ ba). **Managed dependency** (DB chỉ app của bạn dùng) → dùng thật trong integration test, vì tương tác với nó là chi tiết implementation.

### 4.2 Over-mocking: triệu chứng và hậu quả

```java
// ❌ Over-mocked: test này mô tả lại từng dòng implementation
@Test
void createOrder() {
    when(mapper.toEntity(dto)).thenReturn(entity);
    when(priceCalculator.calculate(entity)).thenReturn(BigDecimal.TEN);
    when(repo.save(entity)).thenReturn(entity);
    when(mapper.toDto(entity)).thenReturn(resultDto);

    var r = service.create(dto);

    assertThat(r).isSameAs(resultDto);
    verify(mapper).toEntity(dto);
    verify(priceCalculator).calculate(entity);
    verify(entity).setTotal(BigDecimal.TEN);
    verify(repo).save(entity);
    verify(mapper).toDto(entity);
}
```
Triệu chứng:
- Test dài bằng (hoặc hơn) code được test, cấu trúc "phản chiếu" từng dòng.
- Mock trả về mock (`when(a.getB()).thenReturn(b); when(b.getC())...`) — vi phạm Law of Demeter.
- Đổi tên/gộp một private collaborator → hàng chục test đỏ, không bug nào thật.
- Test xanh nhưng production lỗi vì mock trả dữ liệu không thể có thật (vd `repo.save` trả entity không có id).

```java
// ✅ Classical: dùng mapper thật, calculator thật, fake repository
@Test
void createOrder_calculatesTotalAndPersists() {
    var repo = new InMemoryOrderRepository();
    var service = new OrderService(new OrderMapper(), new PriceCalculator(), repo);

    OrderDto r = service.create(new CreateOrderDto(List.of(new Line("SKU1", 2, "5.00"))));

    assertThat(r.total()).isEqualByComparingTo("10.00");
    assertThat(repo.findById(r.id())).isPresent();
}
```

> 💡 **Góc nhìn Senior:** Hỏi "nếu tôi refactor nội bộ mà không đổi hành vi, test này có đỏ không?" Nếu có → test đang test implementation. Ở phỏng vấn, khi được hỏi "bạn mock những gì?", câu trả lời tốt: *"Tôi mock ở biên hệ thống — những thứ tôi không kiểm soát và có side-effect ra ngoài. Bên trong, tôi dùng object thật hoặc fake. Repository của chính service thì tôi test với DB thật qua Testcontainers."*

### 4.3 Fake tốt hơn mock khi nào?
- Khi interaction phức tạp (repository có save/find/delete): một `InMemoryRepository` 20 dòng thay cho hàng chục `when(...)`.
- Rủi ro: fake lệch hành vi so với thật (vd fake không enforce unique constraint). Giải pháp: **contract test cho fake** — cùng một bộ test abstract chạy cho cả `InMemoryRepo` và `JpaRepo` (Testcontainers).

```java
abstract class OrderRepositoryContract {
    protected abstract OrderRepository repo();

    @Test void savesAndFinds() {
        var o = repo().save(Order.newOrder("c1"));
        assertThat(repo().findById(o.id())).contains(o);
    }
    @Test void rejectsDuplicateExternalId() { ... }
}
class InMemoryOrderRepositoryTest extends OrderRepositoryContract { ... }
@Testcontainers class JpaOrderRepositoryTest extends OrderRepositoryContract { ... }
```

> ⚠️ **Lỗi thường gặp:** chuyển từ "mock mọi thứ" sang "không mock gì" rồi gọi API bên thứ ba thật trong test → chậm, tốn tiền, flaky. Cân bằng: thật cho managed dependency, WireMock/mock cho unmanaged.

### 🛠 Bài tập phần 4

**Bài 4.1 — Nhận diện over-mocking (Cơ bản)**
- Đề bài: Lấy 3 test có nhiều `verify` nhất trong một project bạn từng làm (hoặc project mẫu Spring PetClinic). Với mỗi test, liệt kê `verify` nào kiểm tra hành vi quan sát được, `verify` nào kiểm tra implementation.
- Tiêu chí: đề xuất bản viết lại cho ít nhất 1 test.

**Bài 4.2 — Refactor test sang classical (Trung bình)**
- Đề bài: Viết lại test over-mocked ở 4.2 theo classical, sau đó thực hiện 3 refactor nội bộ (gộp mapper vào service, đổi `PriceCalculator` thành static method, đổi thứ tự set field). Ghi lại số test đỏ ở mỗi phiên bản.
- Tiêu chí: bản classical 0 test đỏ qua cả 3 refactor.

**Bài 4.3 — Contract test cho fake (Nâng cao)**
- Đề bài: Xây `OrderRepositoryContract` như 4.3 với ≥ 6 test (save, find, update optimistic locking, unique constraint, phân trang, xóa). Chạy cho `InMemoryOrderRepository` và `JpaOrderRepository` (PostgreSQL Testcontainers).
- Tiêu chí: fake phải pass toàn bộ contract (bạn sẽ phải cài unique constraint và version check vào fake); ghi lại các khác biệt hành vi bạn phát hiện.

<details>
<summary>Gợi ý lời giải</summary>

- 4.2: phiên bản mock-all đỏ ở cả 3 refactor (verify mapper, `mockStatic` cần thêm, `InOrder`/`verify(entity).setTotal`). Bản classical chỉ assert output (`total`, tồn tại trong repo).
- 4.3: Khác biệt hay gặp: JPA chỉ ném `DataIntegrityViolationException` khi **flush**, không phải khi `save` → test cần `saveAndFlush`; optimistic lock (`@Version`) ném `ObjectOptimisticLockingFailureException`. Fake cần mô phỏng: lưu bản sao (không trả cùng reference, nếu không caller sửa object sẽ "tự lưu"), tăng version mỗi lần save, check version cũ.

</details>

---

<a id="p5"></a>
## 5. TDD: Red – Green – Refactor qua một kata

### 5.1 Chu trình và ba luật
Ba luật của TDD (Robert C. Martin, *Clean Code* Ch.9):
1. Không viết production code trước khi có một test thất bại.
2. Không viết nhiều test hơn mức đủ để thất bại (không compile cũng là thất bại).
3. Không viết nhiều production code hơn mức đủ để test hiện tại pass.

Chu trình:
- 🔴 **Red:** viết test nhỏ nhất cho hành vi tiếp theo, chạy thấy đỏ (và đỏ **đúng lý do**).
- 🟢 **Green:** viết code đơn giản nhất cho xanh (được phép "fake it", hard-code).
- 🔵 **Refactor:** loại trùng lặp, đặt tên lại, tách method — *cả production code lẫn test code* — giữ xanh.

Clean Code cũng nêu **F.I.R.S.T.**: Fast, Independent, Repeatable, Self-validating, Timely.

### 5.2 Kata: String Calculator (Roy Osherove), làm từng bước

**Yêu cầu:** `int add(String numbers)`: chuỗi rỗng → 0; "1" → 1; "1,2" → 3; số lượng tùy ý; hỗ trợ `\n` làm separator; delimiter tùy biến `//;\n1;2`; số âm → exception liệt kê tất cả số âm; số > 1000 bị bỏ qua.

**Vòng 1 — rỗng**
```java
class StringCalculatorTest {
    StringCalculator calc = new StringCalculator();

    @Test void emptyStringIsZero() { assertThat(calc.add("")).isZero(); }
}
// 🔴 không compile -> tạo class
public class StringCalculator {
    public int add(String numbers) { return 0; }  // 🟢 fake it
}
```

**Vòng 2 — một số**
```java
@Test void singleNumber() { assertThat(calc.add("7")).isEqualTo(7); }
// 🟢
public int add(String numbers) {
    if (numbers.isEmpty()) return 0;
    return Integer.parseInt(numbers);
}
```

**Vòng 3 — hai số, rồi nhiều số (triangulation)**
```java
@ParameterizedTest
@CsvSource(delimiter = '|', textBlock = """
    1,2      | 3
    1,2,3,4  | 10
    """)
void commaSeparated(String input, int expected) { assertThat(calc.add(input)).isEqualTo(expected); }
// 🟢
public int add(String numbers) {
    if (numbers.isEmpty()) return 0;
    return Arrays.stream(numbers.split(",")).mapToInt(Integer::parseInt).sum();
}
```
🔵 Refactor: lúc này code đã gọn; nhánh `isEmpty` vẫn cần vì `"".split(",")` trả `[""]`.

**Vòng 4 — newline**
```java
@Test void newlineAsSeparator() { assertThat(calc.add("1\n2,3")).isEqualTo(6); }
// 🟢: split("[,\n]")
```

**Vòng 5 — custom delimiter**
```java
@Test void customDelimiter() { assertThat(calc.add("//;\n1;2")).isEqualTo(3); }
// 🟢
public int add(String input) {
    if (input.isEmpty()) return 0;
    String delimiterRegex = "[,\n]";
    String body = input;
    if (input.startsWith("//")) {
        int nl = input.indexOf('\n');
        delimiterRegex = Pattern.quote(input.substring(2, nl));   // quote: delimiter "." hay "|" là ký tự regex!
        body = input.substring(nl + 1);
    }
    return Arrays.stream(body.split(delimiterRegex)).mapToInt(Integer::parseInt).sum();
}
```
🔵 Refactor: tách `record Parsed(String delimiterRegex, String body)` và method `parse(String)`; test vẫn xanh.

**Vòng 6 — số âm**
```java
@Test void negativesNotAllowed() {
    assertThatThrownBy(() -> calc.add("1,-2,3,-5"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("negatives not allowed: [-2, -5]");
}
```

**Vòng 7 — bỏ qua > 1000**, viết test `add("2,1001") == 2` và biên `add("1000,1") == 1001`.

**Kết quả sau refactor cuối:**
```java
public class StringCalculator {
    private static final String DEFAULT_DELIMITERS = "[,\n]";

    public int add(String input) {
        if (input.isEmpty()) return 0;
        Parsed p = Parsed.of(input);
        List<Integer> nums = Arrays.stream(p.body().split(p.delimiterRegex()))
                                   .map(Integer::parseInt).toList();
        List<Integer> negatives = nums.stream().filter(n -> n < 0).toList();
        if (!negatives.isEmpty()) throw new IllegalArgumentException("negatives not allowed: " + negatives);
        return nums.stream().filter(n -> n <= 1000).mapToInt(Integer::intValue).sum();
    }

    record Parsed(String delimiterRegex, String body) {
        static Parsed of(String input) {
            if (!input.startsWith("//")) return new Parsed(DEFAULT_DELIMITERS, input);
            int nl = input.indexOf('\n');
            return new Parsed(Pattern.quote(input.substring(2, nl)), input.substring(nl + 1));
        }
    }
}
```

### 5.3 TDD ngoài đời thật
- **Inside-out (classical):** bắt đầu từ domain nhỏ nhất, xây dần ra ngoài. Phù hợp thuật toán, domain logic.
- **Outside-in (London, GOOS):** bắt đầu bằng một acceptance test *thất bại* ở biên (HTTP), rồi TDD từng tầng vào trong, mock role chưa có. "Double loop TDD": vòng ngoài acceptance test, vòng trong unit test.
- TDD với **bug fix**: luôn viết test tái hiện bug (đỏ) trước khi sửa → bug không quay lại.
- TDD với legacy: viết **characterization test** chụp hành vi hiện tại (kể cả hành vi sai) trước khi refactor (Michael Feathers, *Working Effectively with Legacy Code*).

> 💡 **Góc nhìn Senior:** TDD không phải tôn giáo. Giá trị chính là **thiết kế** (code testable tự nhiên ít coupling) và **feedback loop ngắn**. Với spike/khám phá công nghệ, viết code thử rồi vứt đi, sau đó TDD lại bản thật. Khi được hỏi "bạn có làm TDD không?", trả lời trung thực + ngữ cảnh: "TDD cho domain logic và bug fix; với glue code/cấu hình tôi viết integration test sau".

> ⚠️ **Lỗi thường gặp:**
> - Bỏ qua bước Refactor → TDD chỉ còn là "test-first" và code vẫn bẩn.
> - Bước nhảy quá lớn (viết test cho cả tính năng) → ở Red quá lâu, debug khó.
> - Không chạy test để thấy đỏ trước → test có thể luôn xanh (assert sai, quên `@Test`).
> - Test bám implementation do viết test *sau* khi đã nghĩ xong implementation.

### 🛠 Bài tập phần 5

**Bài 5.1 — Hoàn thành String Calculator (Cơ bản)**
- Đề bài: Làm lại kata ở 5.2 từ đầu, commit sau **mỗi** bước Red/Green/Refactor (dùng message `red: ...`, `green: ...`, `refactor: ...`). Thêm: delimiter nhiều ký tự `//[***]\n1***2***3` và nhiều delimiter `//[*][%]\n1*2%3`.
- Tiêu chí: lịch sử git cho thấy ≥ 15 commit nhỏ; không commit "green" nào chứa code thừa không có test.

**Bài 5.2 — Bowling kata, outside-in (Trung bình)**
- Đề bài: TDD class `BowlingGame` (`roll(int pins)`, `score()`): spare, strike, frame 10 có bonus ball, perfect game = 300.
- Tiêu chí: test có helper `rollMany(n, pins)`, `rollSpare()`, `rollStrike()` (refactor test code); không quá 1 assert/test cho test logic tính điểm.

**Bài 5.3 — Double-loop TDD cho REST endpoint (Nâng cao)**
- Đề bài: Bắt đầu bằng acceptance test `@SpringBootTest(webEnvironment = RANDOM_PORT)` cho `POST /api/carts/{id}/checkout` (đỏ). Sau đó TDD vào trong: controller (`@WebMvcTest`), application service (unit, fake repository), domain `Cart` (unit). Acceptance test chỉ xanh khi mọi tầng xong.
- Tiêu chí: thời gian chạy toàn bộ unit test < 2s; acceptance test 1–2 case; mô tả trong README thứ tự các test bạn viết.

<details>
<summary>Gợi ý lời giải</summary>

- 5.1: regex cho nhiều delimiter: `Pattern.compile("\\[(.*?)]")` lấy từng delimiter, `Pattern.quote` từng cái rồi nối bằng `|`.
- 5.2: Thuật toán chuẩn: duyệt 10 frame với chỉ số `roll`; strike → `10 + rolls[i+1] + rolls[i+2]`, `i += 1`; spare → `10 + rolls[i+2]`, `i += 2`; còn lại cộng 2 lần, `i += 2`.
- 5.3: Dùng `TestRestTemplate` hoặc `RestClient`/`WebTestClient` cho acceptance; DB thật qua Testcontainers hoặc H2 tạm thời (phần 7 giải thích vì sao nên tránh H2).

</details>

---

<a id="p6"></a>
## 6. Spring Boot testing: full context vs slice, context caching

### 6.1 Bức tranh tổng thể

| Annotation | Nạp gì | Không nạp | Dùng để test |
|---|---|---|---|
| `@SpringBootTest` | Toàn bộ application context (như production) | — | Integration, luồng end-to-end trong service |
| `@WebMvcTest(XController.class)` | MVC infrastructure: `@Controller`, `@ControllerAdvice`, `Converter`, `Filter`, `WebMvcConfigurer`, Jackson, Security (nếu có) | `@Service`, `@Repository`, `@Component` thường | Mapping, validation, status code, JSON, security rule ở tầng web |
| `@WebFluxTest` | Tương tự cho WebFlux, có `WebTestClient` | | |
| `@DataJpaTest` | JPA: `EntityManager`, Spring Data repository, DataSource, Flyway/Liquibase; mỗi test `@Transactional` + rollback | Web, service | Query, mapping entity, constraint |
| `@JdbcTest`, `@DataJdbcTest`, `@DataMongoTest`, `@DataRedisTest` | Hạ tầng dữ liệu tương ứng | | |
| `@JsonTest` | Jackson `ObjectMapper`, `@JsonComponent`, `JacksonTester` | | Serialization/deserialization |
| `@RestClientTest` | `RestTemplateBuilder`/`RestClient.Builder` + `MockRestServiceServer` | | Client HTTP gọi service khác |

Slice test hoạt động nhờ `@TypeExcludeFilters` + `@ImportAutoConfiguration` chỉ chọn một nhóm auto-configuration (danh sách nằm trong file `META-INF/spring/org.springframework.boot.test.autoconfigure.*.imports`).

> Ghi chú phiên bản: Spring Boot 4 tách module (ví dụ `spring-boot-webmvc-test`, `spring-boot-data-jpa-test`) và đổi package của một số annotation test autoconfigure. Khái niệm giữ nguyên; khi nâng cấp hãy theo migration guide chính thức để sửa import.

### 6.2 `@WebMvcTest` + MockMvc

```java
@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired MockMvc mvc;
    @MockitoBean OrderService orderService;   // Spring Framework 6.2+ / Boot 3.4+

    @Test
    void returnsOrder() throws Exception {
        when(orderService.find(1L)).thenReturn(Optional.of(new OrderDto(1L, "PAID", new BigDecimal("10.00"))));

        mvc.perform(get("/api/orders/{id}", 1).accept(MediaType.APPLICATION_JSON))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.id").value(1))
           .andExpect(jsonPath("$.status").value("PAID"))
           .andExpect(jsonPath("$.total").value(10.00));
    }

    @Test
    void returns404WhenMissing() throws Exception {
        when(orderService.find(99L)).thenReturn(Optional.empty());
        mvc.perform(get("/api/orders/99"))
           .andExpect(status().isNotFound())
           .andExpect(jsonPath("$.title").value("Order not found"));    // ProblemDetail (RFC 9457)
    }

    @Test
    void validatesRequestBody() throws Exception {
        mvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"customerId": null, "lines": []}
                    """))
           .andExpect(status().isBadRequest());
        verifyNoInteractions(orderService);
    }
}
```

Từ Spring Framework 6.2 còn có `MockMvcTester` tích hợp AssertJ:
```java
@Autowired MockMvcTester mvc;

@Test
void returnsOrder() {
    when(orderService.find(1L)).thenReturn(Optional.of(new OrderDto(1L, "PAID", BigDecimal.TEN)));
    assertThat(mvc.get().uri("/api/orders/1"))
        .hasStatusOk()
        .bodyJson().extractingPath("$.status").isEqualTo("PAID");
}
```

**Security trong slice test:** nếu có Spring Security trên classpath, `@WebMvcTest` bật auto-configuration bảo mật mặc định của Boot (mọi endpoint yêu cầu xác thực, CSRF bật). Class `@Configuration` của bạn khai báo bean `SecurityFilterChain` **không** được slice quét tới → phải `@Import(SecurityConfig.class)` nếu muốn test đúng rule thật. Dùng `@WithMockUser(roles = "ADMIN")` hoặc `.with(jwt().authorities(...))`, và `.with(csrf())` cho POST khi CSRF bật.

### 6.3 `@MockBean` → `@MockitoBean`
- `@MockBean`/`@SpyBean` (Spring Boot) bị **deprecated từ Boot 3.4** (đánh dấu for removal và đã bị loại bỏ ở Boot 4), thay bằng `@MockitoBean`/`@MockitoSpyBean` của **Spring Framework 6.2** (`org.springframework.test.context.bean.override.mockito`).
- Khác biệt đáng chú ý: `@MockitoBean` dựa trên cơ chế *Bean Override* chung của Spring TestContext; chỉ khai báo trên field của test class (hoặc từ bản mới hơn: trên type-level/`@Nested`); không hỗ trợ đặt trên `@Configuration` như `@MockBean` từng cho phép.
- Cũng có `@TestBean` để thay bean bằng instance do một static factory method tạo — hữu ích cho fake.

### 6.4 Context caching — vì sao suite chậm
Spring TestContext Framework **cache ApplicationContext** giữa các test class trong cùng JVM. Khóa cache (`MergedContextConfiguration`) gồm: các class cấu hình, active profiles, property sources (`@TestPropertySource`, `properties=` của `@SpringBootTest`), context customizers — **bao gồm tập hợp các bean được override bởi `@MockitoBean`/`@MockBean`**, `webEnvironment`, initializers…

Hệ quả:
```java
@SpringBootTest @MockitoBean(types = PaymentClient.class) class ATest {}   // context #1
@SpringBootTest class BTest { @MockitoBean MailSender mail; }               // context #2 (khác bộ mock)
@SpringBootTest(properties = "feature.x=true") class CTest {}               // context #3
@SpringBootTest @ActiveProfiles("it") class DTest {}                         // context #4
```
Mỗi context Spring Boot cỡ vừa mất 5–20s để khởi động, giữ connection pool, Kafka consumer, thread pool… 50 test class với 30 tổ hợp khác nhau = vài phút chỉ để start context, và có thể cạn connection DB.

- Kích thước cache mặc định: 32 context (`spring.test.context.cache.maxSize`), LRU. Context bị đẩy ra sẽ được đóng.
- Xem thống kê: bật log `org.springframework.test.context.cache=DEBUG` → in hit/miss.
- `@DirtiesContext` **phá cache** — context bị đóng và tạo lại cho test sau. Dùng sai chỗ là thủ phạm số 1 làm suite chậm.

> 💡 **Góc nhìn Senior — chiến lược tối ưu suite Spring:**
> 1. Tạo **một (hoặc vài) base class/meta-annotation** cho integration test với cấu hình cố định: cùng profile, cùng Testcontainers, cùng bộ `@MockitoBean` (chỉ mock unmanaged dependency như payment gateway). Mọi test kế thừa → 1 context dùng chung.
> 2. Không `@MockitoBean` "tiện tay" trong từng test class. Cần biến thể → dùng fake bean có thể cấu hình qua API (vd `FakePaymentGateway.willDecline()`), reset trong `@AfterEach`.
> 3. Unit test không dùng Spring. Slice test thay cho full context khi chỉ test một tầng.
> 4. Thay `@DirtiesContext` bằng dọn dữ liệu (truncate bảng, `@Sql`), hoặc reset state bean.
> 5. Bật Gradle/Surefire fork hợp lý: context cache **theo JVM**; `forkCount` > 1 nghĩa là mỗi fork có cache riêng (song song nhưng tốn RAM).

### 6.5 `@SpringBootTest` webEnvironment và transaction pitfall

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CheckoutIT {
    @Autowired TestRestTemplate rest;          // hoặc WebTestClient / RestClient với @LocalServerPort
    @Autowired OrderRepository repo;

    @Test
    void checkout() {
        var resp = rest.postForEntity("/api/carts/1/checkout", null, OrderDto.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(repo.findById(resp.getBody().id())).isPresent();
    }
}
```
- `MOCK` (mặc định): không start server thật, dùng MockMvc; `RANDOM_PORT`/`DEFINED_PORT`: start Tomcat/Netty thật.
- **Bẫy `@Transactional` trên test:** với `MOCK`, request chạy *cùng thread* → nằm trong transaction của test và bị rollback — tiện nhưng **che giấu bug**:
  - Lazy loading "chạy được" trong test (session còn mở) nhưng `LazyInitializationException` ở production.
  - Không flush → constraint violation, lỗi SQL không bao giờ xảy ra.
  - `@TransactionalEventListener(AFTER_COMMIT)` không bao giờ chạy vì không commit.
- Với `RANDOM_PORT`, request chạy trên thread server → **không** nằm trong transaction của test, dữ liệu commit thật, rollback test không dọn được. Phải dọn bằng `@Sql(executionPhase = AFTER_TEST_METHOD)` hoặc truncate.

### 6.6 `@DataJpaTest`

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)   // không thay bằng H2
@Testcontainers
class OrderRepositoryTest {

    @Container @ServiceConnection
    static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired OrderRepository repo;
    @Autowired TestEntityManager em;

    @Test
    void findsTopPaidOrdersByTotal() {
        em.persist(order("PAID", "100"));
        em.persist(order("PAID", "300"));
        em.persist(order("NEW", "999"));
        em.flush(); em.clear();          // ép SQL thật chạy + tránh đọc từ persistence context (L1 cache)

        var top = repo.findTop2ByStatusOrderByTotalDesc(OrderStatus.PAID);

        assertThat(top).extracting(Order::getTotal)
                       .usingElementComparator(BigDecimal::compareTo)
                       .containsExactly(new BigDecimal("300"), new BigDecimal("100"));
    }
}
```
`em.flush(); em.clear();` là thói quen quan trọng: không clear thì `find` trả object từ L1 cache, không chứng minh mapping/query đúng. Có thể bật `spring.jpa.properties.hibernate.generate_statistics` hoặc dùng thư viện như datasource-proxy để **assert số câu SQL** (phát hiện N+1).

### 6.7 `@JsonTest`

```java
@JsonTest
class OrderDtoJsonTest {
    @Autowired JacksonTester<OrderDto> json;

    @Test
    void serializes() throws Exception {
        var dto = new OrderDto(1L, "PAID", new BigDecimal("10.50"));
        assertThat(json.write(dto)).isEqualToJson("""
            {"id":1,"status":"PAID","total":10.50}
            """);
        assertThat(json.write(dto)).doesNotHaveJsonPath("$.internalNote");
    }

    @Test
    void deserializesDates() throws Exception {
        var dto = json.parseObject("""
            {"id":1,"status":"PAID","total":1,"createdAt":"2026-01-01T10:00:00Z"}""");
        assertThat(dto.createdAt()).isEqualTo(Instant.parse("2026-01-01T10:00:00Z"));
    }
}
```
JSON là **public contract** — test JSON bảo vệ khỏi việc đổi tên field/format ngày làm vỡ client.

> ⚠️ **Lỗi thường gặp:**
> - `@SpringBootTest` cho mọi thứ → suite 20 phút.
> - Dùng H2 thay PostgreSQL/MySQL → khác dialect, không có `jsonb`, `ON CONFLICT`, khác hành vi lock/isolation, sequence. Test xanh, production đỏ.
> - `@WebMvcTest` không kèm tên controller → nạp **mọi** controller, buộc phải `@MockitoBean` mọi service của chúng.
> - Quên rằng `@MockitoBean` mặc định reset sau mỗi test (`MockReset.AFTER`) — đừng stub trong `@BeforeAll`.
> - Dùng `@SpyBean`/`@MockitoSpyBean` trên bean có proxy AOP (`@Transactional`) rồi ngạc nhiên vì hành vi khác.

### 🛠 Bài tập phần 6

**Bài 6.1 — Bộ slice test cho một resource (Cơ bản)**
- Đề bài: Với `ProductController` (CRUD + validation `@NotBlank name`, `@Positive price`), viết `@WebMvcTest`: 200, 201 + header `Location`, 400 với ProblemDetail liệt kê field lỗi, 404, 403 cho user không phải ADMIN khi DELETE.
- Tiêu chí: không có test nào khởi động full context; dùng `@MockitoBean` (không dùng `@MockBean` deprecated).

**Bài 6.2 — Đo và giảm số context (Trung bình)**
- Đề bài: Tạo project có 10 test class `@SpringBootTest`, mỗi class dùng một tổ hợp `@MockitoBean`/properties khác nhau. Bật log `org.springframework.test.context.cache=DEBUG`, ghi lại số context được tạo và tổng thời gian. Refactor về 1 base class + fake bean có thể cấu hình.
- Tiêu chí: số context giảm còn ≤ 2; báo cáo thời gian trước/sau.

**Bài 6.3 — Săn bug bị `@Transactional` che giấu (Nâng cao)**
- Đề bài: Viết service trả về entity `Order` có `@OneToMany(fetch = LAZY) lines`, controller serialize trực tiếp (`spring.jpa.open-in-view=false`). Viết 2 test: `@SpringBootTest` + `@Transactional` + MockMvc (xanh) và `@SpringBootTest(RANDOM_PORT)` không transaction (đỏ với `LazyInitializationException`). Sửa bug bằng DTO + `JOIN FETCH`/`@EntityGraph`.
- Tiêu chí: giải thích bằng lời vì sao test thứ nhất xanh; thêm assert số câu SQL để chứng minh không N+1.

<details>
<summary>Gợi ý lời giải</summary>

- 6.1: `.andExpect(header().string("Location", endsWith("/api/products/10")))`; ProblemDetail của Boot: bật `spring.mvc.problemdetails.enabled=true` hoặc tự viết `@RestControllerAdvice` xử lý `MethodArgumentNotValidException` thêm property `errors`. Security: `@Import(SecurityConfig.class)` + `@WithMockUser(roles="USER")` → `status().isForbidden()`.
- 6.2: Base class:
```java
@SpringBootTest
@ActiveProfiles("it")
@Import(TestFakesConfig.class)   // định nghĩa FakePaymentGateway, FakeMailer là @Bean @Primary
public abstract class AbstractIT {
    @Autowired protected FakePaymentGateway payments;
    @AfterEach void resetFakes() { payments.reset(); }
}
```
- 6.3: Test 1 xanh vì transaction của test giữ Hibernate Session mở suốt request (cùng thread) → lazy load thành công. Đếm SQL: dùng `Statistics stats = emf.unwrap(SessionFactory.class).getStatistics(); stats.clear(); ... assertThat(stats.getPrepareStatementCount()).isEqualTo(1);`.

</details>

---

<a id="p7"></a>
## 7. Testcontainers: integration test với DB, Redis, Kafka thật

### 7.1 Testcontainers là gì, hoạt động thế nào
Thư viện Java điều khiển Docker (qua Docker API, docker-java) để khởi động container dùng một lần cho test. Mỗi container lộ port ngẫu nhiên trên host (`getMappedPort`) → không đụng port, chạy song song an toàn. Một container phụ **Ryuk** (`testcontainers/ryuk`) theo dõi JVM; khi JVM chết (kể cả bị kill), Ryuk dọn các container gắn label của session → không rò container.

Yêu cầu: môi trường có Docker-API-compatible runtime (Docker Desktop, Docker Engine, Podman, Colima, hoặc Testcontainers Cloud). Trong CI, runner phải có Docker (GitHub Actions `ubuntu-latest` có sẵn).

> Ghi chú phiên bản: Testcontainers for Java 2.x đổi tên artifact module (dạng `testcontainers-postgresql`) và một số package; ví dụ dưới dùng tọa độ 1.x (`org.testcontainers:postgresql`). Luôn dùng BOM `org.testcontainers:testcontainers-bom`.

### 7.2 Vòng đời container

```java
@Testcontainers
class PgTest {
    @Container   // field static -> start 1 lần cho cả class; field instance -> start lại cho MỖI test (chậm!)
    static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("app").withUsername("app").withPassword("secret");
}
```

**Singleton container pattern** — một container cho toàn bộ JVM, tái sử dụng giữa các test class (và tương thích context caching của Spring):

```java
public abstract class AbstractIntegrationTest {
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:16-alpine");
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);
    static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("apache/kafka:3.8.0"));

    static {
        Startables.deepStart(PG, REDIS, KAFKA).join();   // start song song
        // KHÔNG gọi stop(): Ryuk dọn khi JVM kết thúc
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", PG::getJdbcUrl);
        r.add("spring.datasource.username", PG::getUsername);
        r.add("spring.datasource.password", PG::getPassword);
        r.add("spring.data.redis.host", REDIS::getHost);
        r.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        r.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }
}
```
(`org.testcontainers.kafka.KafkaContainer` dùng image `apache/kafka`; class cũ `org.testcontainers.containers.KafkaContainer` dùng `confluentinc/cp-kafka`.)

### 7.3 `@ServiceConnection` (Spring Boot 3.1+)
Thay cho `@DynamicPropertySource`, Boot tự tạo `ConnectionDetails` bean từ container:

```java
@TestConfiguration(proxyBeanMethods = false)
public class ContainersConfig {
    @Bean @ServiceConnection
    PostgreSQLContainer<?> postgres() { return new PostgreSQLContainer<>("postgres:16-alpine"); }

    @Bean @ServiceConnection(name = "redis")
    GenericContainer<?> redis() { return new GenericContainer<>("redis:7-alpine").withExposedPorts(6379); }

    @Bean @ServiceConnection
    KafkaContainer kafka() { return new KafkaContainer(DockerImageName.parse("apache/kafka:3.8.0")); }
}

@SpringBootTest
@Import(ContainersConfig.class)
class OrderFlowIT { ... }
```
Container khai báo là bean → vòng đời gắn với ApplicationContext → được cache cùng context. Bonus: dùng cùng config cho **local dev** bằng `SpringApplication.from(App::main).with(ContainersConfig.class).run(args)` trong `src/test/java/TestApp.java` (chạy app với DB container mà không cần docker-compose).

### 7.4 Test Kafka end-to-end với Awaitility

```java
@SpringBootTest
@Import(ContainersConfig.class)
class OrderEventsIT {

    @Autowired OrderService orderService;
    @Autowired KafkaTemplate<String, String> kafka;
    @Autowired InventoryRepository inventory;

    @Test
    void publishesOrderPlacedEvent() {
        try (var consumer = testConsumer("orders.placed")) {
            orderService.place(new PlaceOrder("c1", List.of(new Line("SKU1", 2))));

            ConsumerRecord<String, String> rec =
                KafkaTestUtils.getSingleRecord(consumer, "orders.placed", Duration.ofSeconds(10));
            assertThatJson(rec.value()).node("customerId").isEqualTo("c1");   // json-unit-assertj
        }
    }

    @Test
    void consumesPaymentCompletedAndReservesStock() {
        kafka.send("payments.completed", "o-1", """
            {"orderId":"o-1","sku":"SKU1","qty":2}""");

        await().atMost(Duration.ofSeconds(10))
               .pollInterval(Duration.ofMillis(200))
               .untilAsserted(() ->
                   assertThat(inventory.reserved("SKU1")).isEqualTo(2));
    }
}
```

### 7.5 Tối ưu tốc độ và độ ổn định
- **Pin version image** (`postgres:16.4-alpine`), không dùng `latest` → test lặp lại được.
- Singleton container + context caching = mỗi JVM chỉ start mỗi container 1 lần.
- `withReuse(true)` + `testcontainers.reuse.enable=true` trong `~/.testcontainers.properties`: giữ container sống **giữa các lần chạy** trên máy dev (không dùng ở CI). Ryuk không dọn container reuse.
- Dọn dữ liệu giữa test: `TRUNCATE ... RESTART IDENTITY CASCADE` trong `@AfterEach`/`@Sql`, hoặc dùng schema riêng cho mỗi test class. Không tạo container mới cho mỗi test.
- Wait strategy: `Wait.forLogMessage(".*ready to accept connections.*", 2)`, `Wait.forHttp("/health")`, `Wait.forHealthcheck()`. Module chuyên dụng (`PostgreSQLContainer`, `KafkaContainer`) đã có wait strategy đúng.
- Init DB: dùng **chính Flyway/Liquibase migration** của app → test luôn chạy trên schema thật, đồng thời kiểm tra migration.
- Mock API HTTP bên ngoài: WireMock (`WireMockExtension`) hoặc `MockServerContainer`.
- Toxiproxy (`ToxiproxyContainer`): mô phỏng mạng chậm/đứt để test timeout, retry, circuit breaker.

> 💡 **Góc nhìn Senior:** Testcontainers đổi cán cân chi phí của integration test: DB thật chỉ tốn vài giây một lần cho cả suite. Điều đó cho phép áp dụng lời khuyên của Khorikov — **test managed dependency bằng instance thật**. Câu hỏi phỏng vấn hay gặp: "vì sao không dùng H2?" → khác SQL dialect, khác locking/isolation, khác kiểu dữ liệu (jsonb, array, UUID), khác hành vi sequence/identity; migration Flyway viết cho Postgres có thể không chạy trên H2; test xanh không chứng minh gì.

> ⚠️ **Lỗi thường gặp:**
> - Gọi `container.stop()` trong `@AfterAll` khi đang dùng singleton → class sau dùng container đã chết.
> - Field `@Container` không static → container khởi động lại cho mỗi test.
> - Mix `@Container` (vòng đời JUnit) với context caching của Spring: context được cache trỏ tới container đã bị JUnit stop sau class trước → lỗi connection refused ở class sau. Dùng singleton pattern hoặc `@ServiceConnection` bean.
> - CI chạy trong Docker (Docker-in-Docker) mà không cấu hình `TESTCONTAINERS_HOST_OVERRIDE`/socket → không kết nối được container.
> - Image pull chậm/rate limit Docker Hub ở CI → dùng registry mirror (`hub.image.name.prefix`) hoặc cache.

### 🛠 Bài tập phần 7

**Bài 7.1 — Repository test với PostgreSQL thật (Cơ bản)**
- Đề bài: Viết `@DataJpaTest` + Testcontainers PostgreSQL cho repository có một native query dùng `jsonb` (`where attributes @> cast(:filter as jsonb)`) và một query phân trang.
- Tiêu chí: schema tạo bởi Flyway; không có H2 trên classpath; test chạy lại 3 lần cho kết quả giống nhau.

**Bài 7.2 — Singleton containers cho cả suite (Trung bình)**
- Đề bài: Tạo `AbstractIntegrationTest` dùng PostgreSQL + Redis + Kafka như 7.2 (hoặc `@ServiceConnection`). Viết 4 test class kế thừa. Đo tổng thời gian; đếm số lần container start (xem `docker ps` hoặc log).
- Tiêu chí: mỗi container start đúng 1 lần cho cả suite; chỉ 1 Spring context.

**Bài 7.3 — Resilience test với Toxiproxy (Nâng cao)**
- Đề bài: Service gọi Redis làm cache với timeout 200ms và fallback về DB. Dùng `ToxiproxyContainer` chèn latency 500ms vào Redis; kiểm tra request vẫn thành công qua fallback trong < 400ms, và metric `cache.fallback` tăng.
- Tiêu chí: test không dùng `Thread.sleep`; có cả case Redis down hoàn toàn (`toxic` cắt kết nối).

<details>
<summary>Gợi ý lời giải</summary>

- 7.1: `@AutoConfigureTestDatabase(replace = NONE)` + `@ServiceConnection`; kiểm tra Flyway chạy bằng `spring.flyway.enabled=true` (mặc định khi có Flyway trên classpath).
- 7.2: `Startables.deepStart(...)`; với Spring: chỉ một bộ `@MockitoBean`/property duy nhất trên base class.
- 7.3:
```java
static Network net = Network.newNetwork();
static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withNetwork(net).withNetworkAliases("redis");
static ToxiproxyContainer toxi = new ToxiproxyContainer("ghcr.io/shopify/toxiproxy:2.9.0").withNetwork(net);
// ToxiproxyClient client = new ToxiproxyClient(toxi.getHost(), toxi.getControlPort());
// Proxy p = client.createProxy("redis", "0.0.0.0:8666", "redis:6379");
// p.toxics().latency("lat", ToxicDirection.DOWNSTREAM, 500);
// app trỏ spring.data.redis.port = toxi.getMappedPort(8666)
```
Đo bằng `MeterRegistry` (`registry.get("cache.fallback").counter().count()`).

</details>

---

<a id="p8"></a>
## 8. Contract testing: Spring Cloud Contract & Pact

### 8.1 Vấn đề
Microservice A (consumer) gọi B (provider). Unit test của A mock B; unit test của B không biết A dùng gì. B đổi tên field `totalAmount` → `total`: cả hai bộ test đều xanh, production vỡ. E2E test bắt được nhưng chậm, flaky, cần môi trường đầy đủ.

**Contract test** kiểm tra riêng biên giữa hai bên: consumer khai báo kỳ vọng; provider chứng minh đáp ứng kỳ vọng đó — mỗi bên chạy độc lập trong pipeline của mình.

### 8.2 Consumer-driven với Pact
1. Consumer viết test với Pact mock server → sinh file **pact** (JSON) mô tả các interaction.
2. Publish pact lên **Pact Broker** (hoặc PactFlow).
3. Provider chạy **verification**: Pact replay request vào provider thật (với state setup) và so response.
4. `can-i-deploy` trên broker trả lời: phiên bản X của consumer có tương thích với phiên bản đang chạy ở production của provider không → gate deploy.

```java
// Consumer side (pact-jvm, JUnit 5)
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "order-service")
class OrderClientPactTest {

    @Pact(consumer = "billing-service")
    V4Pact orderExists(PactDslWithProvider builder) {
        return builder
            .given("order 42 exists")
            .uponReceiving("get order 42")
                .path("/api/orders/42").method("GET")
            .willRespondWith()
                .status(200)
                .body(new PactDslJsonBody()
                    .integerType("id", 42)
                    .stringMatcher("status", "NEW|PAID|SHIPPED", "PAID")
                    .decimalType("total", 100.50))
            .toPact(V4Pact.class);
    }

    @Test
    void fetchesOrder(MockServer server) {
        var client = new OrderClient(RestClient.create(server.getUrl()));
        var order = client.get(42);
        assertThat(order.status()).isEqualTo("PAID");
    }
}

// Provider side
@Provider("order-service")
@PactBroker   // hoặc @PactFolder("pacts")
@SpringBootTest(webEnvironment = RANDOM_PORT)
class OrderProviderPactTest {
    @LocalServerPort int port;

    @BeforeEach void target(PactVerificationContext ctx) { ctx.setTarget(new HttpTestTarget("localhost", port)); }

    @TestTemplate @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verify(PactVerificationContext ctx) { ctx.verifyInteraction(); }

    @State("order 42 exists")
    void order42() { repo.save(new Order(42L, PAID, new BigDecimal("100.50"))); }
}
```

Nguyên tắc viết contract tốt: dùng **matcher theo kiểu** (`integerType`, `stringMatcher`) thay vì giá trị cứng; chỉ khai báo field consumer **thực sự dùng** (Postel's law — provider thêm field mới không làm vỡ contract).

### 8.3 Spring Cloud Contract (provider-driven trong hệ sinh thái Spring)
- Contract viết bằng Groovy/YAML/Kotlin DSL **trong repo provider**.
- Plugin sinh **test tự động** cho provider (kế thừa base class bạn cấu hình, thường dùng `RestAssuredMockMvc`) và sinh **WireMock stubs** đóng gói thành artifact `*-stubs.jar`.
- Consumer dùng `@AutoConfigureStubRunner(ids = "com.acme:order-service:+:stubs:8090")` để chạy stub đó.

```yaml
# src/test/resources/contracts/order/get_order.yml
request:
  method: GET
  url: /api/orders/42
response:
  status: 200
  headers:
    Content-Type: application/json
  body:
    id: 42
    status: PAID
  matchers:
    body:
      - path: $.status
        type: by_regex
        value: "NEW|PAID|SHIPPED"
```

| | Pact | Spring Cloud Contract |
|---|---|---|
| Ai viết contract | Consumer (code test) | Thường provider (hoặc consumer gửi PR vào repo provider) |
| Đa ngôn ngữ | Rất tốt (JS, Go, Python, .NET…) | Chủ yếu JVM (có Docker cho ngôn ngữ khác) |
| Hạ tầng | Pact Broker, `can-i-deploy` | Artifact repository (Nexus/Artifactory) |
| Messaging | Có (message pacts) | Có (Spring Cloud Stream, Kafka) |

> 💡 **Góc nhìn Senior:** Contract test **không** thay thế functional test của provider — nó chỉ kiểm tra *hình dạng* và vài *trạng thái*. Giá trị lớn nhất là cho phép bỏ phần lớn E2E test xuyên service và deploy độc lập. Với API public (nhiều consumer không biết), dùng OpenAPI + kiểm tra breaking change (openapi-diff) thay vì consumer-driven.

> ⚠️ **Lỗi thường gặp:** contract quá chặt (giá trị cụ thể, thứ tự field) → vỡ vì lý do không quan trọng; quên verify contract trong pipeline của provider; không versioning/tag theo môi trường nên `can-i-deploy` vô nghĩa.

### 🛠 Bài tập phần 8

**Bài 8.1 — Pact consumer test (Cơ bản)**
- Đề bài: `billing-service` gọi `GET /api/customers/{id}` của `customer-service`, dùng `id`, `email`, `tier`. Viết consumer pact cho 2 interaction: tồn tại (200) và không tồn tại (404).
- Tiêu chí: file pact sinh ra trong `target/pacts`; dùng matcher kiểu, không hard-code toàn bộ body.

**Bài 8.2 — Provider verification (Trung bình)**
- Đề bài: Viết provider test cho `customer-service` đọc pact từ thư mục (`@PactFolder`), có `@State` setup dữ liệu bằng repository (Testcontainers hoặc fake).
- Tiêu chí: đổi tên field `tier` → `level` trên provider làm verification đỏ với message rõ ràng.

**Bài 8.3 — Pipeline với Pact Broker (Nâng cao)**
- Đề bài: Chạy Pact Broker bằng docker-compose (image `pactfoundation/pact-broker` + Postgres). Cấu hình CI (GitHub Actions) cho consumer publish pact với version = git SHA, branch; provider verify và publish kết quả; bước deploy gọi `pact-broker can-i-deploy --pacticipant billing-service --version $SHA --to-environment production`.
- Tiêu chí: sơ đồ luồng + file workflow; mô phỏng một thay đổi breaking bị chặn.

<details>
<summary>Gợi ý lời giải</summary>

- 8.1: 404: `.given("customer 999 does not exist")...willRespondWith().status(404)`; client phải map 404 → `Optional.empty()` hoặc exception, và test assert điều đó.
- 8.2: `@State("customer 1 exists") void c1() { repo.save(...); }`; khi chạy `mvn test -Dpact.verifier.publishResults=true` cần `pact.provider.version`.
- 8.3: Ngoài `can-i-deploy`, sau khi deploy gọi `pact-broker record-deployment --environment production` để broker biết version nào đang chạy. Dùng webhook của broker để trigger provider verification khi có pact mới ("contract_requiring_verification_published").

</details>

---

<a id="p9"></a>
## 9. Test code bất đồng bộ & đa luồng

### 9.1 Vì sao khó
- Kết quả không có ngay khi method trả về → test phải **chờ**.
- `Thread.sleep(2000)`: hoặc quá ngắn (flaky trên CI chậm), hoặc quá dài (suite chậm). **Gần như luôn sai.**
- Race condition chỉ xuất hiện với interleaving hiếm → test xanh 999/1000 lần.

### 9.2 Chiến lược theo thứ tự ưu tiên
1. **Tách logic khỏi threading:** logic thuần test đồng bộ; phần threading mỏng. Ví dụ inject `Executor` và trong test dùng `Runnable::run` (direct executor) → code chạy đồng bộ.
2. **Điều khiển thời gian:** inject `Clock`; với scheduler, inject abstraction để test "tua" thời gian. Reactor có `StepVerifier.withVirtualTime`.
3. **Chờ có điều kiện:** Awaitility — poll đến khi điều kiện thỏa hoặc timeout.
4. **Đồng bộ tường minh:** `CountDownLatch`, `CyclicBarrier` để tạo interleaving mong muốn.
5. **Stress/concurrency testing:** chạy nhiều luồng nhiều lần, hoặc dùng công cụ chuyên dụng (jcstress cho memory model, Lincheck cho cấu trúc dữ liệu concurrent).

```java
// 1. Direct executor
class ReportServiceTest {
    @Test
    void generatesReportAsync() {
        Executor direct = Runnable::run;
        var store = new InMemoryReportStore();
        var svc = new ReportService(direct, store);

        svc.requestReport("r1");

        assertThat(store.get("r1")).isPresent();   // không cần chờ
    }
}

// 3. Awaitility
await().atMost(Duration.ofSeconds(5))
       .pollDelay(Duration.ZERO)
       .pollInterval(Duration.ofMillis(100))
       .ignoreExceptions()
       .untilAsserted(() -> assertThat(repo.findStatus("o-1")).isEqualTo(PAID));

// Chờ một điều kiện "trong suốt" khoảng thời gian (khẳng định điều gì KHÔNG xảy ra)
await().during(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(3))
       .until(() -> mailer.sent().isEmpty());

// CompletableFuture
CompletableFuture<Result> f = service.computeAsync(input);
assertThat(f).succeedsWithin(Duration.ofSeconds(2))
             .extracting(Result::value).isEqualTo(42);
```

### 9.3 Chứng minh race condition bằng test

```java
class CounterTest {

    @RepeatedTest(20)
    void incrementIsThreadSafe() throws Exception {
        var counter = new Counter();             // thử với int++ (đỏ) rồi AtomicLong/LongAdder (xanh)
        int threads = 16, perThread = 10_000;
        var start = new CountDownLatch(1);       // cho tất cả thread xuất phát cùng lúc -> tăng va chạm
        var pool = Executors.newFixedThreadPool(threads);
        try {
            List<Future<?>> fs = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                fs.add(pool.submit(() -> {
                    start.await();
                    for (int i = 0; i < perThread; i++) counter.increment();
                    return null;
                }));
            }
            start.countDown();
            for (var f : fs) f.get(10, TimeUnit.SECONDS);   // propagate exception từ worker
        } finally {
            pool.shutdownNow();
        }
        assertThat(counter.value()).isEqualTo((long) threads * perThread);
    }
}
```
Lưu ý: test xanh **không chứng minh** thread-safe — chỉ là không tìm thấy lỗi. Test đỏ thì chắc chắn có bug.

**Test optimistic locking / double-spend với DB thật:** hai thread cùng rút tiền từ một tài khoản (Testcontainers PostgreSQL), dùng `CyclicBarrier(2)` để cả hai đọc số dư trước khi ghi; assert đúng một giao dịch thành công và số dư không âm.

### 9.4 Spring `@Async`, `@Scheduled`, event
- `@Async` trong `@SpringBootTest` chạy trên executor thật → dùng Awaitility. Hoặc trong profile test, thay `TaskExecutor` bằng `SyncTaskExecutor`.
- `@Scheduled`: không test bằng cách chờ cron; gọi trực tiếp method job, test lịch bằng `CronExpression.parse("0 0 2 * * *").next(...)`.
- `@TransactionalEventListener`: chỉ chạy sau commit → test với transaction thật đã commit (không `@Transactional` trên test).

> 💡 **Góc nhìn Senior:** Mỗi `Thread.sleep` trong test là một khoản nợ: cộng dồn làm suite chậm và là nguồn flaky số 1. Rule trong code review: cấm `Thread.sleep` trong test trừ khi có comment giải thích. Với hệ thống event-driven, thiết kế **observable completion** (callback, metric, trạng thái truy vấn được) để test có cái mà chờ.

> ⚠️ **Lỗi thường gặp:**
> - Exception trong thread worker bị nuốt → test xanh. Luôn `future.get()` hoặc thu exception vào list và assert rỗng.
> - Không shutdown executor → thread rò sang test sau.
> - `verify(mock).method()` ngay sau khi gọi async → flaky; dùng `verify(mock, timeout(2000)).method()`.
> - Awaitility `atMost` quá lớn (60s) che giấu regression hiệu năng.

### 🛠 Bài tập phần 9

**Bài 9.1 — Thay `Thread.sleep` bằng Awaitility (Cơ bản)**
- Đề bài: Cho test dùng `Thread.sleep(3000)` để chờ consumer Kafka xử lý. Viết lại bằng Awaitility, và một bản bằng direct executor (tách logic handler ra test riêng).
- Tiêu chí: thời gian test giảm; chạy 50 lần (`@RepeatedTest`) không fail.

**Bài 9.2 — Tái hiện race condition (Trung bình)**
- Đề bài: Viết `InventoryService.reserve(sku, qty)` kiểu check-then-act (`if (stock >= qty) stock -= qty`) với `HashMap`. Viết test đồng thời chứng minh overselling. Sửa bằng 3 cách: `synchronized`, `ConcurrentHashMap.compute`, và DB `UPDATE ... SET stock = stock - ? WHERE sku = ? AND stock >= ?`.
- Tiêu chí: test đỏ ổn định (≥ 90% lần chạy) với bản lỗi, xanh với cả 3 bản sửa.

**Bài 9.3 — Virtual time với Reactor hoặc scheduler tự viết (Nâng cao)**
- Đề bài: Viết `RetryingClient` thử lại với exponential backoff 1s, 2s, 4s (tối đa 3 lần). Test rằng tổng thời gian chờ đúng và lần thứ 4 không xảy ra — mà test chạy < 100ms.
- Tiêu chí: dùng `StepVerifier.withVirtualTime` (nếu Reactor) hoặc inject `Sleeper` interface (nếu imperative) với fake ghi lại các khoảng sleep.

<details>
<summary>Gợi ý lời giải</summary>

- 9.2: để tăng xác suất va chạm, thêm `CountDownLatch` start và lặp nhiều lần; bản SQL: kiểm tra `updatedRows == 1`, nếu 0 → hết hàng.
- 9.3 imperative:
```java
interface Sleeper { void sleep(Duration d) throws InterruptedException; }
class RecordingSleeper implements Sleeper {
    final List<Duration> slept = new ArrayList<>();
    public void sleep(Duration d) { slept.add(d); }
}
// assert: sleeper.slept == [1s, 2s, 4s], và client.call() ném exception sau 4 lần gọi (1 + 3 retry)
```
Reactor: `StepVerifier.withVirtualTime(() -> client.call().retryWhen(Retry.backoff(3, Duration.ofSeconds(1))))...thenAwait(Duration.ofSeconds(7))...`. Lưu ý `Retry.backoff` mặc định có jitter — đặt `.jitter(0)` để test xác định.

</details>

---

<a id="p10"></a>
## 10. Coverage (JaCoCo) và mutation testing (PIT)

### 10.1 JaCoCo hoạt động thế nào
JaCoCo chạy dưới dạng **Java agent**, instrument bytecode khi class được nạp (on-the-fly), ghi lại probe nào được chạy vào `jacoco.exec`, sau đó goal `report` đối chiếu với class file để tạo HTML/XML.

Các loại chỉ số: **instruction**, **line**, **branch** (mỗi nhánh của if/switch), **cyclomatic complexity**, method, class. **Branch coverage** có giá trị hơn line coverage.

```xml
<plugin>
  <groupId>org.jacoco</groupId>
  <artifactId>jacoco-maven-plugin</artifactId>
  <version>0.8.12</version>
  <executions>
    <execution><id>prepare</id><goals><goal>prepare-agent</goal></goals></execution>  <!-- đặt property argLine -->
    <execution><id>report</id><phase>verify</phase><goals><goal>report</goal></goals></execution>
    <execution>
      <id>check</id><goals><goal>check</goal></goals>
      <configuration>
        <rules>
          <rule>
            <element>BUNDLE</element>
            <limits>
              <limit><counter>BRANCH</counter><value>COVEREDRATIO</value><minimum>0.70</minimum></limit>
            </limits>
          </rule>
        </rules>
        <excludes><exclude>**/config/**</exclude><exclude>**/*Application.class</exclude></excludes>
      </configuration>
    </execution>
  </executions>
</plugin>
```
> ⚠️ Nếu bạn tự đặt `<argLine>` trong Surefire mà không có `@{argLine}`, agent JaCoCo bị ghi đè → coverage 0%.

Với multi-module, dùng goal `report-aggregate` trong một module tổng hợp. Integration test (Failsafe) dùng `prepare-agent-integration`/`report-integration` hoặc gộp file exec bằng `merge`.

### 10.2 Giới hạn của coverage
Coverage chỉ trả lời "dòng này **có chạy** khi test không?", không trả lời "test có **kiểm tra** kết quả của dòng này không?".

```java
@Test
void coverageWithoutAssertion() {
    calculator.computeTax(order);   // 100% line coverage cho computeTax, 0 assertion
}
```
- 100% coverage + 0 assert = 0 bảo vệ.
- Đặt target cứng (vd 90%) → **Goodhart's law**: người ta viết test vô nghĩa để đạt số.
- Coverage thấp là tín hiệu *có vấn đề*; coverage cao **không** là tín hiệu *ổn*.

> 💡 **Góc nhìn Senior:** Dùng coverage như công cụ **tìm vùng chưa test** (nhất là trong diff của PR: "new code coverage" trên SonarQube), không dùng làm KPI. Gate hợp lý: coverage trên **code mới** ≥ 80%, và không cho giảm tổng.

### 10.3 Mutation testing với PIT
PIT tạo các **mutant** — bản sao bytecode bị sửa nhỏ (đổi `<` thành `<=`, `+` thành `-`, bỏ lời gọi void method, trả về `null`/0/false/empty…) — rồi chạy test liên quan với từng mutant:
- Test đỏ → mutant bị **killed** (tốt: test phát hiện được thay đổi).
- Test vẫn xanh → mutant **survived** (test yếu: thay đổi hành vi mà không ai biết).
- Không test nào chạy qua → **no coverage**.

**Mutation score** = killed / tổng mutant. Đây là thước đo **chất lượng assertion** mà coverage không có.

```xml
<plugin>
  <groupId>org.pitest</groupId>
  <artifactId>pitest-maven</artifactId>
  <version>1.17.0</version>
  <dependencies>
    <dependency>
      <groupId>org.pitest</groupId>
      <artifactId>pitest-junit5-plugin</artifactId>
      <version>1.2.1</version>
    </dependency>
  </dependencies>
  <configuration>
    <targetClasses><param>com.acme.billing.domain.*</param></targetClasses>
    <targetTests><param>com.acme.billing.*Test</param></targetTests>
    <mutators><mutator>STRONGER</mutator></mutators>
    <mutationThreshold>75</mutationThreshold>
    <threads>4</threads>
    <timestampedReports>false</timestampedReports>
  </configuration>
</plugin>
```
Chạy: `mvn test-compile org.pitest:pitest-maven:mutationCoverage`. Kết quả HTML ở `target/pit-reports`.

Ví dụ mutant sống sót:
```java
public boolean isEligibleForFreeShipping(BigDecimal total) {
    return total.compareTo(new BigDecimal("500000")) >= 0;   // PIT đổi >= thành >
}
@Test void freeShippingAbove() { assertThat(isEligibleForFreeShipping(new BigDecimal("600000"))).isTrue(); }
// Mutant ">" vẫn xanh -> thiếu test biên 500000 đúng bằng ngưỡng.
```

**Chi phí và cách dùng thực tế:**
- Chậm: số mutant × thời gian test. Giới hạn vào package domain, dùng **incremental analysis** (`withHistory`) và chạy trên **code thay đổi** của PR (ví dụ Arcmutate/ `scmMutationCoverage` goal).
- **Equivalent mutant**: mutant không đổi hành vi quan sát được (vd đổi điều kiện trong code cache) → không thể kill, bỏ qua có lý do.
- Không chạy PIT cho integration test chậm.

### 🛠 Bài tập phần 10

**Bài 10.1 — Bật JaCoCo + gate (Cơ bản)**
- Đề bài: Thêm JaCoCo vào project, cấu hình `check` với branch coverage ≥ 70%, loại trừ config/DTO. Cố ý viết test không assert để thấy coverage tăng mà chất lượng không tăng.
- Tiêu chí: `mvn verify` fail khi dưới ngưỡng; giải thích vì sao gate này chưa đủ.

**Bài 10.2 — Kill mutants (Trung bình)**
- Đề bài: Chạy PIT trên `StringCalculator` (phần 5) và một class domain có ≥ 5 nhánh (vd tính phí vận chuyển theo vùng, cân nặng, thành viên). Ghi lại mutation score ban đầu, liệt kê mutant sống sót, bổ sung test để đạt ≥ 90%.
- Tiêu chí: với mỗi mutant còn sống, giải thích là equivalent hay thiếu test.

**Bài 10.3 — Mutation testing trong CI chỉ cho code thay đổi (Nâng cao)**
- Đề bài: Cấu hình pipeline chạy PIT incremental (history file được cache giữa các build) và giới hạn `targetClasses` theo danh sách file thay đổi so với `main` (script `git diff --name-only origin/main...HEAD`).
- Tiêu chí: thời gian chạy trên PR nhỏ < 2 phút; build fail nếu mutation score của code mới < 70%.

<details>
<summary>Gợi ý lời giải</summary>

- 10.2: mutant hay sống: boundary (`>=`↔`>`), "negate conditional" ở nhánh else không có assert, "void method call removed" (vd bỏ `audit.log()` mà không ai assert), "return value" (trả `0` thay vì kết quả khi test chỉ kiểm tra không ném exception).
- 10.3: `-DwithHistory` lưu history ở thư mục tmp; trong CI đặt `historyInputFile`/`historyOutputFile` vào thư mục cache. Map đường dẫn `src/main/java/com/acme/X.java` → `com.acme.X` rồi truyền `-DtargetClasses=com.acme.X,com.acme.Y`.

</details>

---

<a id="p11"></a>
## 11. Architecture tests với ArchUnit

### 11.1 Vấn đề: kiến trúc mục rữa dần
Quy ước "controller không gọi repository trực tiếp", "domain không phụ thuộc Spring", "không có vòng phụ thuộc giữa module" thường chỉ nằm trong wiki. Sau 1 năm, 20 người, deadline… quy ước bị vi phạm dần. **ArchUnit** biến quy ước thành **test chạy trong JUnit**: nó phân tích bytecode (`ClassFileImporter`) để dựng đồ thị phụ thuộc và kiểm tra rule.

```xml
<dependency>
  <groupId>com.tngtech.archunit</groupId>
  <artifactId>archunit-junit5</artifactId>
  <version>1.3.0</version>
  <scope>test</scope>
</dependency>
```

```java
@AnalyzeClasses(packages = "com.acme.shop", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule layers = layeredArchitecture().consideringAllDependencies()
        .layer("Web").definedBy("..web..")
        .layer("Application").definedBy("..application..")
        .layer("Domain").definedBy("..domain..")
        .layer("Infrastructure").definedBy("..infrastructure..")
        .whereLayer("Web").mayNotBeAccessedByAnyLayer()
        .whereLayer("Application").mayOnlyBeAccessedByLayers("Web")
        .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Infrastructure", "Web");

    @ArchTest
    static final ArchRule domainIsFrameworkFree = noClasses().that().resideInAPackage("..domain..")
        .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "jakarta.persistence..")
        .because("domain phải test được không cần framework (hexagonal)");

    @ArchTest
    static final ArchRule noCycles = slices().matching("com.acme.shop.(*)..").should().beFreeOfCycles();

    @ArchTest
    static final ArchRule controllersNaming = classes().that().areAnnotatedWith(RestController.class)
        .should().haveSimpleNameEndingWith("Controller")
        .andShould().resideInAPackage("..web..");

    @ArchTest
    static final ArchRule noFieldInjection = noFields().should().beAnnotatedWith(Autowired.class)
        .because("dùng constructor injection");

    @ArchTest
    static final ArchRule noJavaUtilLogging = GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;

    @ArchTest
    static final ArchRule noStdout = GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;
}
```

### 11.2 Áp dụng vào codebase cũ: `FreezingArchRule`
Codebase đã có 300 vi phạm → rule mới đỏ ngay, không ai sửa nổi. `FreezingArchRule.freeze(rule)` lưu vi phạm hiện tại vào "violation store" (file text commit vào repo); chỉ **vi phạm mới** làm test đỏ, vi phạm cũ được sửa dần sẽ tự bị gỡ khỏi store.

```java
@ArchTest
static final ArchRule noRepoInWeb = FreezingArchRule.freeze(
    noClasses().that().resideInAPackage("..web..")
               .should().dependOnClassesThat().resideInAPackage("..repository.."));
```

> 💡 **Góc nhìn Senior:** ArchUnit là công cụ "governance" rẻ nhất cho một tech lead: rule được review trong PR như code, chạy trong vài giây. Kết hợp với **Spring Modulith** (`ApplicationModules.of(App.class).verify()`) cho modular monolith — kiểm tra module chỉ truy cập API public của module khác. Đừng viết rule cho mọi thứ: ưu tiên rule bảo vệ ranh giới quan trọng (domain độc lập, không vòng phụ thuộc, không truy cập internal của module khác).

> ⚠️ **Lỗi thường gặp:** quên `DoNotIncludeTests` → test class vi phạm rule; rule có `that()` không khớp class nào → mặc định ArchUnit 1.x **fail** khi rule rỗng (`archRule.failOnEmptyShould`), đây là tính năng tốt vì phát hiện rule đặt sai package.

### 🛠 Bài tập phần 11

**Bài 11.1 — Rule cơ bản (Cơ bản)**
- Đề bài: Viết 5 rule cho project của bạn: không field injection; `@Service` chỉ nằm ở `..application..`; không dùng `System.out`; không phụ thuộc `java.util.Date`; entity JPA không được dùng trong package `..web..`.
- Tiêu chí: cố ý vi phạm từng rule một lần để thấy message lỗi.

**Bài 11.2 — Hexagonal (Trung bình)**
- Đề bài: Tổ chức project theo `domain`, `application` (ports), `adapters.in.web`, `adapters.out.persistence`. Dùng `Architectures.onionArchitecture()` của ArchUnit để kiểm tra.
- Tiêu chí: adapter không gọi nhau; domain không phụ thuộc bất kỳ adapter nào.

**Bài 11.3 — Freeze và custom condition (Nâng cao)**
- Đề bài: Viết custom `ArchCondition<JavaMethod>`: mọi public method của class `@RestController` có `@PostMapping`/`@PutMapping` phải có tham số `@Valid`. Áp dụng `FreezingArchRule` cho codebase hiện có.
- Tiêu chí: violation store được commit; thêm một endpoint vi phạm mới → test đỏ.

<details>
<summary>Gợi ý lời giải</summary>

- 11.2:
```java
onionArchitecture()
    .domainModels("..domain.model..").domainServices("..domain.service..")
    .applicationServices("..application..")
    .adapter("web", "..adapters.in.web..")
    .adapter("persistence", "..adapters.out.persistence..");
```
- 11.3: `new ArchCondition<>("have @Valid body") { public void check(JavaMethod m, ConditionEvents ev) { boolean ok = m.getParameters().stream().anyMatch(p -> p.isAnnotatedWith(Valid.class)); if (!ok) ev.add(SimpleConditionEvent.violated(m, m.getFullName() + " thiếu @Valid")); } }` áp dụng cho `methods().that().areAnnotatedWith(PostMapping.class)...`.

</details>

---

<a id="p12"></a>
## 12. Performance / load testing: JMeter, Gatling, k6

### 12.1 Các loại test hiệu năng

| Loại | Mục đích | Hình dạng tải |
|---|---|---|
| **Load test** | Xác nhận hệ thống đạt SLO ở tải kỳ vọng | Tăng dần tới mức mục tiêu, giữ ổn định |
| **Stress test** | Tìm điểm gãy, xem hệ thống hỏng thế nào (graceful hay sập dây chuyền) | Tăng vượt mức mục tiêu đến khi lỗi |
| **Spike test** | Đột biến (flash sale) | Nhảy vọt trong vài giây |
| **Soak / endurance** | Rò bộ nhớ, rò connection, tăng dần latency | Tải vừa trong nhiều giờ |
| **Capacity test** | Bao nhiêu instance cho N req/s | Lặp với số replica khác nhau |

### 12.2 Khái niệm cần nắm
- **Throughput** (req/s), **latency** theo **percentile** (p50, p95, p99, p99.9) — *không bao giờ chỉ nhìn average*. Average che giấu đuôi dài; với fan-out (1 request gọi 10 service), p99 của từng service trở thành trải nghiệm thường gặp của user.
- **Little's Law:** `L = λ × W` (số request đồng thời = throughput × latency). 200 req/s × 0.5s = 100 request đang xử lý → cần ≥ 100 thread/connection nếu blocking.
- **Open vs closed workload model:** closed (N user ảo, mỗi user chờ response rồi mới gửi tiếp — JMeter thread group mặc định) → khi server chậm, tải tự giảm, che giấu vấn đề. Open (tốc độ đến cố định, như traffic Internet thật) → Gatling `constantUsersPerSec`, k6 `constant-arrival-rate`.
- **Coordinated omission** (Gil Tene): công cụ closed-model không gửi request trong lúc chờ response chậm → bỏ sót chính những mẫu tệ nhất → p99 đo được đẹp giả tạo. Dùng open model hoặc công cụ hiệu chỉnh (wrk2, HdrHistogram).

### 12.3 Ba công cụ

**JMeter** — GUI để thiết kế, chạy bằng CLI (`jmeter -n -t plan.jmx -l result.jtl -e -o report/`). Nhiều plugin, hỗ trợ nhiều giao thức (HTTP, JDBC, JMS). Nhược: file `.jmx` XML khó review/diff, tốn tài nguyên mỗi thread.

**Gatling** — test viết bằng code (Java/Kotlin/Scala DSL), engine async (Netty), báo cáo HTML đẹp, dễ đưa vào Git và CI.

```java
public class CheckoutSimulation extends Simulation {

    HttpProtocolBuilder http = http.baseUrl("https://staging.shop.local")
        .acceptHeader("application/json");

    FeederBuilder<String> users = csv("users.csv").circular();

    ScenarioBuilder browseAndBuy = scenario("Browse and buy")
        .feed(users)
        .exec(http("list products").get("/api/products?page=0").check(status().is(200)))
        .pause(1, 3)
        .exec(http("add to cart").post("/api/carts/#{userId}/items")
              .body(StringBody("{\"sku\":\"SKU1\",\"qty\":1}")).asJson()
              .check(status().is(201)))
        .exec(http("checkout").post("/api/carts/#{userId}/checkout")
              .check(status().is(201), jsonPath("$.id").saveAs("orderId")));

    {
        setUp(browseAndBuy.injectOpen(
                rampUsersPerSec(1).to(50).during(Duration.ofMinutes(2)),
                constantUsersPerSec(50).during(Duration.ofMinutes(10))))
            .protocols(http)
            .assertions(
                global().responseTime().percentile(99.0).lt(800),
                global().failedRequests().percent().lt(1.0));
    }
}
```

**k6** — script JavaScript, binary Go nhẹ, thresholds làm pass/fail cho CI, tích hợp Grafana.

```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  scenarios: {
    checkout: {
      executor: 'constant-arrival-rate',   // open model
      rate: 100, timeUnit: '1s', duration: '5m',
      preAllocatedVUs: 50, maxVUs: 300,
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<300', 'p(99)<800'],
    http_req_failed: ['rate<0.01'],
  },
};

export default function () {
  const res = http.get(`${__ENV.BASE_URL}/api/products?page=0`);
  check(res, { 'status 200': (r) => r.status === 200 });
  sleep(1);
}
```

### 12.4 Quy trình làm load test đúng
1. Xác định mục tiêu từ SLO (vd p99 < 500ms ở 300 req/s, error < 0.1%).
2. Môi trường giống production (cấu hình JVM, số replica, resource limit, dữ liệu đủ lớn — query nhanh trên bảng 1000 dòng có thể chết trên 10 triệu dòng).
3. **Warm-up** JVM (JIT, connection pool, cache) trước khi đo.
4. Máy sinh tải không được là nút thắt (theo dõi CPU của load generator).
5. Quan sát phía server song song: CPU, GC pause, heap, thread pool, connection pool (HikariCP `pending`), DB slow query, p99 từng dependency.
6. Thay đổi **một biến** mỗi lần; ghi lại kết quả; so sánh với baseline.
7. Đưa test ngắn (smoke performance, 2–5 phút) vào CI nightly với thresholds để bắt regression.

> 💡 **Góc nhìn Senior:** Load test không phải để "lấy một con số", mà để hiểu **nút thắt nằm ở đâu và hệ thống hỏng thế nào**. Một câu trả lời phỏng vấn tốt mô tả: tìm ra connection pool cạn (Hikari timeout) trước CPU, sửa bằng cách giảm thời gian giữ connection (không gọi HTTP trong transaction) thay vì tăng pool vô tội vạ (DB có giới hạn `max_connections`).

> ⚠️ **Lỗi thường gặp:** chỉ báo cáo average; test từ laptop qua VPN; dữ liệu test quá nhỏ hoặc mọi user ảo dùng cùng 1 sản phẩm (cache hit 100%, hoặc lock contention giả tạo); không có think time; quên rằng rate limiter/WAF chặn load generator.

### 🛠 Bài tập phần 12

**Bài 12.1 — k6 smoke test (Cơ bản)**
- Đề bài: Viết script k6 cho 2 endpoint của một app Spring Boot local, open model 20 req/s trong 1 phút, thresholds p95 < 200ms.
- Tiêu chí: k6 trả exit code ≠ 0 khi vi phạm threshold (thử bằng cách thêm `Thread.sleep(300)` vào endpoint).

**Bài 12.2 — Tìm nút thắt (Trung bình)**
- Đề bài: App có endpoint gọi một dependency chậm (WireMock delay 200ms) bên trong `@Transactional`, Hikari pool 10. Chạy Gatling tăng dần tới 100 req/s. Quan sát metric Hikari (`hikaricp.connections.pending`) qua Actuator/Prometheus. Sửa và chạy lại.
- Tiêu chí: báo cáo trước/sau với p50/p95/p99, throughput, error rate; giải thích bằng Little's Law.

**Bài 12.3 — Coordinated omission (Nâng cao)**
- Đề bài: Endpoint thỉnh thoảng (1%) dừng 2s (mô phỏng GC pause). Đo p99 bằng JMeter thread group (closed, 10 thread) và k6 `constant-arrival-rate` cùng throughput trung bình. So sánh và giải thích chênh lệch.
- Tiêu chí: biểu đồ/ bảng percentile cho cả hai; giải thích coordinated omission bằng ví dụ cụ thể của bạn.

<details>
<summary>Gợi ý lời giải</summary>

- 12.2: 10 connection, mỗi request giữ connection ≥ 200ms → tối đa ~50 req/s (10 / 0.2s). Vượt mức đó request xếp hàng chờ connection (`connectionTimeout` 30s mặc định) → latency tăng vọt rồi lỗi. Sửa: gọi dependency **ngoài** transaction, transaction chỉ bao quanh phần ghi DB.
- 12.3: closed model: khi 1 thread bị kẹt 2s, nó không gửi request khác → chỉ 1 mẫu xấu được ghi; trong thực tế, có ~2s × rate request đến trong lúc đó và đều chậm. Open model ghi nhận tất cả → p99 cao hơn nhiều, đúng thực tế hơn.

</details>

---

<a id="p13"></a>
## 13. Flaky tests: nguyên nhân gốc và cách trị

### 13.1 Định nghĩa và tác hại
Flaky test: cùng code, cùng môi trường, lúc xanh lúc đỏ. Tác hại lớn hơn nhiều so với vẻ ngoài: team quen "re-run cho xanh" → bỏ qua cả test đỏ thật; pipeline chậm; mất niềm tin vào cả suite.

### 13.2 Nguyên nhân gốc thường gặp

| Nguyên nhân | Ví dụ | Cách trị |
|---|---|---|
| Thời gian | `LocalDate.now()` gần nửa đêm, timezone CI khác máy dev, DST | Inject `Clock`; cố định `TZ`/`-Duser.timezone=UTC` |
| Async / sleep | `Thread.sleep(500)` chờ consumer | Awaitility, direct executor |
| Thứ tự test / state dùng chung | Test A ghi DB, test B đọc giả định rỗng; static cache; singleton | Dọn dữ liệu, mỗi test tự tạo dữ liệu với id riêng; không dùng static mutable |
| Thứ tự không xác định | `HashMap`/`HashSet` iteration, `SELECT` không `ORDER BY`, `parallelStream` | Assert không phụ thuộc thứ tự (`containsExactlyInAnyOrder`) hoặc thêm `ORDER BY` |
| Tài nguyên ngoài | Gọi API thật, DNS, Docker Hub rate limit | WireMock, pin image, registry mirror |
| Port cố định | `server.port=8080` va chạm khi chạy song song | `RANDOM_PORT`, Testcontainers mapped port |
| Concurrency trong code | Race condition thật | Đây là bug production — sửa code, không sửa test |
| Tài nguyên giới hạn | CI chậm, timeout quá chặt | Timeout dựa trên điều kiện, không dựa trên đồng hồ cứng |
| Random | `new Random()` không seed, dữ liệu sinh ngẫu nhiên vi phạm constraint | Seed cố định, log seed khi fail |
| Rò rỉ giữa các test | Mockito static mock không đóng, `SecurityContextHolder` không clear, `System.setProperty` | try-with-resources, `@AfterEach` dọn, `@ResourceLock` |
| Floating point | `0.1 + 0.2 == 0.3` | `isCloseTo(x, within(1e-9))`, dùng `BigDecimal` cho tiền |

### 13.3 Quy trình xử lý ở cấp team
1. **Phát hiện:** CI ghi lại test nào pass sau retry (Surefire `rerunFailingTestsCount` báo cáo "Flakes"; Gradle `test-retry` plugin; Develocity flaky test detection).
2. **Cách ly (quarantine):** chuyển test flaky sang tag `@Tag("quarantine")` chạy ở job riêng không chặn merge — **có ticket và owner, hạn sửa**.
3. **Tái hiện:** `@RepeatedTest(200)`, chạy với `-Djunit.jupiter.execution.order.random.seed`/`MethodOrderer.Random`, chạy song song, giới hạn CPU (`docker run --cpus=0.5`), đổi timezone.
4. **Sửa gốc**, không tăng timeout/sleep.
5. Theo dõi chỉ số flaky rate của suite như một metric chất lượng.

> 💡 **Góc nhìn Senior:** Retry tự động trong CI là thuốc giảm đau, không phải thuốc chữa. Nếu bật retry, bắt buộc phải **báo cáo** test nào cần retry. Và luôn đặt câu hỏi: "có phải test flaky vì **code** flaky không?" — rất nhiều race condition production đã được phát hiện nhờ một test "thỉnh thoảng đỏ".

### 🛠 Bài tập phần 13

**Bài 13.1 — Sửa 5 test flaky (Cơ bản)**
- Đề bài: Tự viết 5 test flaky minh họa 5 nguyên nhân khác nhau trong bảng (thời gian, thứ tự HashSet, state static, sleep, port cố định). Sửa từng cái.
- Tiêu chí: bản lỗi fail ít nhất 1 lần trong 100 lần chạy (`@RepeatedTest(100)` hoặc điều kiện môi trường); bản sửa pass 100/100.

**Bài 13.2 — Phát hiện phụ thuộc thứ tự (Trung bình)**
- Đề bài: Cấu hình `junit.jupiter.testmethod.order.default=org.junit.jupiter.api.MethodOrderer$Random` và `junit.jupiter.testclass.order.default=org.junit.jupiter.api.ClassOrderer$Random`, chạy suite nhiều lần, tìm test phụ thuộc thứ tự.
- Tiêu chí: tìm cách tái hiện bằng seed cố định; sửa.

**Bài 13.3 — Pipeline quarantine (Nâng cao)**
- Đề bài: Thiết kế quy trình CI: job chính loại trừ tag `quarantine`; job phụ chạy quarantine hằng đêm và báo cáo; script parse báo cáo Surefire XML để liệt kê test "flaky" (pass sau rerun) và tạo issue tự động.
- Tiêu chí: file workflow + script; tài liệu hóa SLA sửa test quarantine (vd 7 ngày).

<details>
<summary>Gợi ý lời giải</summary>

- 13.2: seed được in ra trong log khi dùng `MethodOrderer.Random`; cố định bằng `junit.jupiter.execution.order.random.seed=<seed>` để tái hiện.
- 13.3: Surefire với `-Dsurefire.rerunFailingTestsCount=2` ghi `<flakyFailure>` trong `TEST-*.xml`; parse bằng `xmllint`/`yq`/script Java, gọi `gh issue create`.

</details>

---

<a id="p14"></a>
## 14. Static analysis & code review

### 14.1 Công cụ và vai trò

| Công cụ | Phân tích gì | Ghi chú |
|---|---|---|
| **Checkstyle** | Style, format, naming, import, Javadoc | Nên kết hợp formatter tự động (Spotless + google-java-format / palantir) để khỏi tranh cãi style trong review |
| **PMD** | Code smell trên source (unused, empty catch, complexity), CPD phát hiện copy-paste | |
| **SpotBugs** (kế nhiệm FindBugs) | Bug pattern trên **bytecode**: NPE tiềm ẩn, `equals` không `hashCode`, đồng bộ hóa sai, resource leak, so sánh String bằng `==` | Plugin **FindSecBugs** cho lỗ hổng bảo mật (SQLi, XXE, weak crypto) |
| **Error Prone** (Google) | Lỗi phổ biến ở **compile time** (plugin javac) | Fail build ngay khi biên dịch |
| **NullAway** | Kiểm tra null-safety dựa trên annotation (JSpecify `@Nullable`) | Plugin của Error Prone |
| **SonarQube / SonarCloud** | Tổng hợp: bugs, vulnerabilities, security hotspots, code smells, duplication, coverage (đọc từ JaCoCo XML), technical debt; **Quality Gate** | Tập trung vào "new code" (Clean as You Code) |

```xml
<!-- SpotBugs + FindSecBugs -->
<plugin>
  <groupId>com.github.spotbugs</groupId>
  <artifactId>spotbugs-maven-plugin</artifactId>
  <version>4.8.6.4</version>
  <configuration>
    <effort>Max</effort>
    <threshold>Medium</threshold>
    <excludeFilterFile>spotbugs-exclude.xml</excludeFilterFile>
    <plugins>
      <plugin><groupId>com.h3xstream.findsecbugs</groupId><artifactId>findsecbugs-plugin</artifactId><version>1.13.0</version></plugin>
    </plugins>
  </configuration>
  <executions><execution><goals><goal>check</goal></goals></execution></executions>
</plugin>
```

SonarQube trong CI (Maven):
```bash
mvn -B verify org.sonarsource.scanner.maven:sonar-maven-plugin:sonar \
  -Dsonar.host.url=$SONAR_HOST_URL -Dsonar.token=$SONAR_TOKEN \
  -Dsonar.qualitygate.wait=true      # pipeline fail nếu Quality Gate đỏ
```
Quality Gate gợi ý cho code mới: 0 bug/vulnerability mới mức Blocker/Critical, security hotspots đã review 100%, coverage ≥ 80%, duplication ≤ 3%.

> 💡 **Góc nhìn Senior:** Static analysis có giá trị khi **tín hiệu/nhiễu cao**. Bật mọi rule trên codebase cũ → 10.000 cảnh báo → ai cũng lờ đi. Chiến lược: (1) chỉ gate trên **code mới**, (2) chọn ruleset nhỏ, chất lượng cao, (3) mỗi suppress phải có lý do (`@SuppressFBWarnings(value="...", justification="...")`), (4) tự động format để review tập trung vào logic. Công cụ không thay thế review; nó **giải phóng** reviewer khỏi những việc máy làm được.

### 14.2 Code review checklist (Senior)

**Đúng đắn & thiết kế**
- [ ] Thay đổi giải quyết đúng vấn đề trong ticket? Có hiểu được "tại sao" từ mô tả PR?
- [ ] Ranh giới trách nhiệm hợp lý (SRP), không rò domain logic vào controller/adapter?
- [ ] Xử lý lỗi: không nuốt exception, không `catch (Exception e) {}`, message có ngữ cảnh, không lộ stack trace ra client.
- [ ] Null-safety, `Optional` không dùng làm field/tham số, collection rỗng thay cho null.
- [ ] API public (REST, event, DB schema) có backward compatible? Migration có an toàn khi rolling deploy (expand–contract)?

**Concurrency & tài nguyên**
- [ ] Shared mutable state có được bảo vệ? Bean Spring singleton có field mutable?
- [ ] Resource đóng đúng (try-with-resources), timeout cho mọi lời gọi mạng.
- [ ] Transaction boundary hợp lý; không gọi HTTP/IO dài trong transaction; `@Transactional` trên method private/self-invocation (không có tác dụng).

**Hiệu năng**
- [ ] N+1 query, thiếu index, load cả bảng vào bộ nhớ, phân trang?
- [ ] Vòng lặp gọi I/O, regex compile trong vòng lặp, log nặng ở hot path?

**Bảo mật**
- [ ] Input validation, query tham số hóa, kiểm tra quyền (object-level authorization), không log secret/PII.
- [ ] Dependency mới: license, CVE, có thật sự cần?

**Test**
- [ ] Có test cho hành vi mới và cho bug fix (tái hiện bug)?
- [ ] Test ở đúng tầng, không over-mock, không `Thread.sleep`, đặt tên mô tả hành vi.
- [ ] Test có thể fail? (Đọc assertion — có assert gì không?)

**Vận hành**
- [ ] Log/metric/trace đủ để debug trên production? Feature flag cho thay đổi rủi ro?
- [ ] Cấu hình mới có default an toàn, đã thêm vào tất cả môi trường?

**Văn hóa review:** PR nhỏ (< 400 dòng), review trong ngày, comment về code chứ không về người, phân biệt "blocking" và "nit:", tác giả tự review trước khi gửi, khen điểm tốt. Reviewer chịu trách nhiệm chung về những gì được merge.

> ⚠️ **Lỗi thường gặp:** review chỉ soi style (máy làm được); approve PR 2000 dòng sau 5 phút ("LGTM"); tranh luận dài trong comment thay vì gọi 10 phút; không chạy/đọc test.

### 🛠 Bài tập phần 14

**Bài 14.1 — Dựng quality toolchain (Cơ bản)**
- Đề bài: Thêm Spotless (google-java-format), Checkstyle, SpotBugs + FindSecBugs vào một project Maven; tất cả chạy trong `mvn verify`.
- Tiêu chí: cố ý đưa vào code `String` so sánh bằng `==`, SQL nối chuỗi, `catch` rỗng → build fail với cảnh báo tương ứng.

**Bài 14.2 — SonarQube local + Quality Gate (Trung bình)**
- Đề bài: Chạy SonarQube bằng Docker (`sonarqube:community`), phân tích project kèm báo cáo JaCoCo XML, tạo Quality Gate tùy chỉnh cho new code.
- Tiêu chí: tạo một PR/branch giả lập giảm coverage → gate đỏ; ghi lại các issue Sonar tìm được mà SpotBugs không tìm được và ngược lại.

**Bài 14.3 — Review một PR thật (Nâng cao)**
- Đề bài: Chọn một PR đã merge (của team bạn hoặc open-source như Spring PetClinic) có ≥ 200 dòng. Review theo checklist 14.2, viết ít nhất 8 comment phân loại blocking/non-blocking/nit/praise.
- Tiêu chí: mỗi comment blocking phải nêu rủi ro cụ thể và đề xuất sửa; tổng hợp 3 điểm học được.

<details>
<summary>Gợi ý lời giải</summary>

- 14.1: SpotBugs sẽ báo `ES_COMPARING_STRINGS_WITH_EQ`, FindSecBugs `SQL_INJECTION_JDBC`/`SQL_INJECTION_SPRING_JDBC`; catch rỗng do PMD (`EmptyCatchBlock`) hoặc Checkstyle (`EmptyCatchBlock`) bắt.
- 14.2: `docker run -d -p 9000:9000 sonarqube:community`; cần `jacoco:report` sinh `target/site/jacoco/jacoco.xml` trước khi chạy `sonar`. Property `sonar.coverage.jacoco.xmlReportPaths` nếu đường dẫn khác mặc định.

</details>

---

<a id="du-an-mini"></a>
## Dự án mini

### "Quality-first Order Service"
Xây một service đặt hàng nhỏ với bộ test và quality pipeline đạt chuẩn Senior. Thời lượng 1–2 ngày.

**Yêu cầu chức năng**
1. REST API: `POST /api/orders` (tạo đơn với nhiều dòng hàng), `GET /api/orders/{id}`, `POST /api/orders/{id}/pay`, `POST /api/orders/{id}/cancel`.
2. Domain: `Order` với state machine `NEW → PAID → SHIPPED`, `NEW/PAID → CANCELLED`; tính tổng tiền với coupon (phần trăm, có trần) và miễn phí vận chuyển từ ngưỡng.
3. Lưu PostgreSQL (Flyway migration), cache sản phẩm trong Redis.
4. Khi thanh toán thành công, publish event `OrderPaid` lên Kafka; consumer nội bộ trừ kho.
5. Gọi `payment-gateway` bên ngoài qua HTTP (mô phỏng bằng WireMock trong test), timeout 2s, retry 2 lần cho lỗi 5xx.

**Yêu cầu về test & chất lượng (phi chức năng)**
- Unit test domain (không Spring, không mock) — viết bằng TDD, lịch sử commit thể hiện red/green/refactor cho ít nhất `Order` và `Pricing`.
- `@WebMvcTest` cho controller (validation, mã lỗi ProblemDetail, security: chỉ role `USER` được tạo đơn).
- `@DataJpaTest` + Testcontainers PostgreSQL cho repository, có assert số câu SQL cho truy vấn chi tiết đơn (không N+1).
- `@SpringBootTest` integration: PostgreSQL + Redis + Kafka bằng singleton containers / `@ServiceConnection`; WireMock cho gateway; Awaitility cho consumer; **tối đa 1–2 application context** cho cả suite (chứng minh bằng log cache).
- Contract test Pact (consumer) cho client gọi payment-gateway.
- Concurrency test: hai request `pay` đồng thời cho cùng đơn → đúng một thành công (optimistic locking).
- ArchUnit: domain không phụ thuộc Spring/JPA; không vòng phụ thuộc; controller không truy cập repository.
- JaCoCo branch coverage ≥ 80% cho package `domain`; PIT mutation score ≥ 80% cho `domain`.
- Spotless + SpotBugs (FindSecBugs) chạy trong `mvn verify`.
- k6 script smoke: 50 req/s, 2 phút, p95 < 300ms trên máy local.
- CI (GitHub Actions): job `unit` (< 1 phút), job `integration` (Testcontainers), job `quality` (PIT, Sonar tùy chọn); không có `Thread.sleep` trong test (kiểm tra bằng ArchUnit hoặc grep).

**Tiêu chí chấm (100 điểm)**

| Hạng mục | Điểm |
|---|---|
| Domain đúng, unit test rõ ràng, TDD thể hiện qua commit | 20 |
| Slice test web & JPA đúng tầng, không over-mock | 15 |
| Integration test với Testcontainers ổn định, context được tái sử dụng | 20 |
| Contract test + concurrency test | 10 |
| ArchUnit, coverage, mutation testing đạt ngưỡng | 15 |
| Static analysis + CI pipeline chạy xanh, thời gian hợp lý | 10 |
| README: chiến lược test (tầng nào test gì, vì sao), cách chạy, kết quả k6 | 10 |

Trừ điểm: test flaky (chạy 10 lần CI phải xanh 10), dùng H2, `@DirtiesContext` không lý do, `@MockBean` deprecated, mock value object.

---

<a id="checklist-tu-danh-gia"></a>
## Checklist tự đánh giá

- [ ] Tôi giải thích được 4 trụ cột của test tốt và vì sao "resistance to refactoring" quan trọng nhất.
- [ ] Tôi chọn được hình dạng chiến lược test (pyramid/trophy/honeycomb) dựa trên nơi rủi ro tập trung, và lập luận được.
- [ ] Tôi nắm lifecycle JUnit 5, `@TestInstance`, `@ParameterizedTest` (Value/Csv/Method/Enum source), `@Nested`, và tự viết được một extension.
- [ ] Tôi viết assertion AssertJ có message rõ (recursive comparison, extracting, `assertThatThrownBy`, soft assertions).
- [ ] Tôi phân biệt dummy/stub/spy/mock/fake và biết **không** verify stub.
- [ ] Tôi dùng thành thạo Mockito: `when/thenReturn`, `doReturn` cho spy, `verify` với timeout, `ArgumentCaptor`, `mockStatic` trong try-with-resources, hiểu strict stubs.
- [ ] Tôi giải thích được classical vs London school, managed vs unmanaged dependency, và nhận diện over-mocking.
- [ ] Tôi làm được một kata TDD với các bước red–green–refactor rõ ràng và biết double-loop TDD.
- [ ] Tôi chọn đúng slice test (`@WebMvcTest`, `@DataJpaTest`, `@JsonTest`, `@RestClientTest`) và biết khi nào cần `@SpringBootTest`.
- [ ] Tôi giải thích được cơ chế context caching, vì sao nhiều tổ hợp `@MockitoBean` và `@DirtiesContext` làm suite chậm, và cách khắc phục.
- [ ] Tôi biết `@MockBean` đã deprecated từ Boot 3.4 và thay bằng `@MockitoBean`.
- [ ] Tôi hiểu các bẫy của `@Transactional` trên test (lazy loading, không flush, event after-commit, RANDOM_PORT).
- [ ] Tôi dựng được Testcontainers (singleton, `@ServiceConnection`) cho PostgreSQL/Redis/Kafka và giải thích vì sao không dùng H2.
- [ ] Tôi giải thích được contract testing, consumer-driven (Pact) vs Spring Cloud Contract, và `can-i-deploy`.
- [ ] Tôi test được code async bằng Awaitility/direct executor/virtual time, và viết được test tái hiện race condition.
- [ ] Tôi hiểu giới hạn của coverage và dùng PIT để đánh giá chất lượng assertion.
- [ ] Tôi viết được ArchUnit rule cho layered/hexagonal architecture và dùng `FreezingArchRule` cho codebase cũ.
- [ ] Tôi phân biệt load/stress/spike/soak test, open vs closed model, coordinated omission, và đọc percentile.
- [ ] Tôi liệt kê được ≥ 8 nguyên nhân gốc của flaky test và quy trình xử lý ở cấp team.
- [ ] Tôi dựng được quality gate (Checkstyle/Spotless, SpotBugs/FindSecBugs, SonarQube) và review PR theo checklist có trọng tâm.

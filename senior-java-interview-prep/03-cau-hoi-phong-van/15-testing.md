# Câu hỏi phỏng vấn — Module 15: Testing & Chất lượng phần mềm

> Giáo trình tương ứng: [Module 15 — Testing & Chất lượng phần mềm](../01-giao-trinh/15-testing.md)

**Cách dùng:** đọc câu hỏi, **tự trả lời thành tiếng** (bấm giờ ~30 giây cho phần "trả lời ngắn", 2–3 phút cho phần giải thích) rồi mới mở đáp án. Câu nào trả lời vấp thì đánh dấu và quay lại phần "📖 Ôn lại". Với câu 🔴, hãy luyện thêm phần "Câu hỏi nối tiếp" — interviewer Senior thường đào sâu 2–3 tầng.

**Ký hiệu mức độ:** 🟢 Cơ bản · 🟡 Senior · 🔴 Xoáy sâu · 🎬 Tình huống (scenario)

## Mục lục

1. [Chiến lược test & nguyên lý](#g1) — Q1–Q7
2. [JUnit 5 & AssertJ](#g2) — Q8–Q14
3. [Mockito & test doubles](#g3) — Q15–Q21
4. [TDD](#g4) — Q22–Q24
5. [Spring Boot testing](#g5) — Q25–Q30
6. [Testcontainers](#g6) — Q31–Q34
7. [Contract testing](#g7) — Q35–Q36
8. [Test code bất đồng bộ & đa luồng](#g8) — Q37–Q39
9. [Coverage, mutation testing, ArchUnit](#g9) — Q40–Q43
10. [Performance test, flaky test, static analysis & review](#g10) — Q44–Q48
11. [Tình huống thực tế](#g11) — Q49–Q52

---

<a id="g1"></a>
## 1. Chiến lược test & nguyên lý

### Q1. 🟢 Mục tiêu thật sự của unit test là gì? Một test "tốt" được đánh giá theo tiêu chí nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Mục tiêu là giúp dự án **phát triển bền vững** — thêm tính năng và refactor mà không sợ làm vỡ thứ khác, chứ không phải đạt một con số coverage. Theo Khorikov, test tốt có 4 trụ cột: *protection against regressions*, *resistance to refactoring*, *fast feedback*, *maintainability*; trong đó resistance to refactoring gần như không được thỏa hiệp.

**Giải thích chi tiết:**
- **Protection against regressions:** test chạy qua code có ý nghĩa nghiệp vụ và assert kết quả → bắt được bug thật (tránh *false negative*).
- **Resistance to refactoring:** đổi cấu trúc mà hành vi giữ nguyên thì test không đỏ (tránh *false positive*). Test giòn làm team mất niềm tin, bắt đầu `@Disabled` hàng loạt hoặc "sửa test cho xanh" mà không đọc.
- **Fast feedback & maintainability** là chi phí: test chạy chậm, setup 200 dòng thì không ai muốn chạy/sửa.
- Hai trụ cột đầu quyết định **độ chính xác**; một test không thể tối đa cả 4 cùng lúc (E2E bảo vệ tốt nhưng chậm; test getter nhanh nhưng vô dụng) → phải chọn tầng phù hợp cho từng loại logic.
- Giá trị lớn nhất hay bị quên: **cho phép refactor tự tin** và là tài liệu sống của hành vi.

**Câu hỏi nối tiếp:**
- *Test getter/setter có nên viết không?* — Không: không có logic, không bảo vệ gì, chỉ tốn bảo trì.
- *Một test vỡ khi đổi tên private method thì sao?* — Nó đang test implementation; viết lại để assert output/hành vi quan sát được.

**⚠️ Câu trả lời gây điểm trừ:** "Mục tiêu là đạt 80% coverage"; "test càng nhiều càng tốt"; không nhắc gì tới refactor và chi phí bảo trì test.

**📖 Ôn lại:** [1.1–1.2 Mục tiêu & bốn trụ cột](../01-giao-trinh/15-testing.md#p1)

</details>

### Q2. 🟡 Test pyramid, testing trophy, honeycomb khác nhau thế nào? Với một microservice bạn chọn hình dạng nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Không có hình dạng đúng cho mọi hệ thống — câu hỏi đúng là *rủi ro tập trung ở đâu*. Domain logic phức tạp → pyramid (nhiều unit test). Service mỏng logic, chủ yếu glue code → trophy/honeycomb: phần lớn là integration test ở biên service với DB/Kafka thật (Testcontainers), ít test chi tiết implementation, rất ít test xuyên nhiều service.

**Giải thích chi tiết:**
- **Pyramid (Mike Cohn):** nhiều unit, ít integration, rất ít E2E. Hợp với pricing, rule engine, ngân hàng.
- **Trophy (Kent C. Dodds):** static analysis làm nền, integration test nhiều nhất.
- **Honeycomb (Spotify):** cho microservice — test service qua HTTP vào, DB/Kafka thật ra; "implementation detail tests" và "integrated tests" (xuyên service) đều ít.
- Nếu rủi ro nằm ở query SQL, mapping JSON, cấu hình Spring Security, transaction boundary thì unit test với mock **không chứng minh được gì** — cần integration test.
- Ví dụ trả lời: "Service Payment của tôi: unit test cho `Money`, state machine `PaymentStatus`; integration `@SpringBootTest` + PostgreSQL/Kafka Testcontainers + WireMock cho cổng thanh toán; Pact với Order service; 1 luồng E2E trên staging."

**Câu hỏi nối tiếp:**
- *"Ice-cream cone" là gì?* — Ngược pyramid: rất nhiều E2E/manual, ít unit → suite chậm, flaky, không ai tin.
- *Đặt mục tiêu thời gian CI thế nào?* — Ví dụ unit < 1 phút, integration < 5–10 phút; E2E chạy sau merge/nightly.

**⚠️ Câu trả lời gây điểm trừ:** đọc thuộc "70% unit, 20% integration, 10% E2E" như chân lý mà không gắn với nơi rủi ro tập trung.

**📖 Ôn lại:** [1.3–1.4 Pyramid, trophy, honeycomb; test cái gì ở tầng nào](../01-giao-trinh/15-testing.md#p1)

</details>

### Q3. 🟡 "Unit" trong unit test là gì? So sánh classical (Detroit) và London (mockist) school.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Classical coi unit là **một đơn vị hành vi** (có thể nhiều class), chỉ cô lập *các test với nhau* và chỉ thay thế dependency shared/out-of-process. London coi unit là **một class**, mock mọi collaborator mutable. Classical cho test bền với refactor; London chỉ ra chính xác class hỏng và hỗ trợ thiết kế outside-in nhưng dễ giòn.

**Giải thích chi tiết:**

| | Classical | London (GOOS) |
|---|---|---|
| Cô lập | Test khỏi test | Class khỏi collaborator |
| Double | Chỉ DB ngoài, SMTP, bus… | Mọi collaborator |
| Ưu | Bền với refactor, ít setup giả | Lỗi khoanh vùng rõ; khám phá interface (outside-in) |
| Nhược | 1 bug làm đỏ nhiều test | Gắn implementation → giòn; "test xanh, hệ thống hỏng" |

- GOOS dùng mock như **công cụ thiết kế**: "Mock roles, not objects", "Only mock types you own".
- Thực tế Senior thường lai: classical cho domain, mock ở biên unmanaged dependency.

**Câu hỏi nối tiếp:**
- *Classical có làm debug khó hơn không?* — Có thể nhiều test đỏ cùng lúc, nhưng test đỏ đầu tiên/nhỏ nhất thường chỉ ra lỗi; đổi lại không có false positive khi refactor.

**⚠️ Câu trả lời gây điểm trừ:** "Unit test là test một class, mọi dependency phải mock" — nói như quy tắc tuyệt đối, không biết trường phái khác.

**📖 Ôn lại:** [4.1 Hai trường phái](../01-giao-trinh/15-testing.md#p4)

</details>

### Q4. 🔴 Bạn mock những gì và không mock những gì? Giải thích managed vs unmanaged dependency.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Tôi mock ở **biên hệ thống** — dependency out-of-process mà tương tác với nó **quan sát được từ bên ngoài** (SMTP, message bus tới hệ thống khác, API bên thứ ba): đó là *unmanaged dependency*. DB chỉ service của tôi dùng là *managed dependency* → dùng thật (Testcontainers) vì tương tác với nó là chi tiết implementation. Bên trong, tôi dùng object thật hoặc fake.

**Giải thích chi tiết:**
- Khorikov: tương tác với unmanaged dependency là **một phần contract** (email đã gửi, event đã publish cho service khác) → verify là hợp lý và bền vững.
- Tương tác với managed dependency (câu SQL, thứ tự gọi repository) là implementation → verify sẽ giòn; thay vào đó assert **trạng thái cuối** trong DB thật.
- Phân biệt thêm theo **CQS**: query (dữ liệu đi vào) → stub, **không bao giờ verify**; command (side-effect đi ra) → mock và verify nếu nó quan sát được từ ngoài.
- Ngoại lệ thực tế: DB dùng chung với service khác (shared DB, legacy) thì một phần schema trở thành contract → cân nhắc như unmanaged.

```java
// ✅ verify outgoing command tới hệ thống ngoài
verify(smsGateway).send(eq("0901234567"), contains("quá hạn"));
// ❌ verify query trên managed dependency
verify(invoiceRepository).findOverdue(today);
```

**Câu hỏi nối tiếp:**
- *Kafka topic nội bộ chỉ service mình produce/consume thì sao?* — Gần với managed: test bằng Kafka thật trong Testcontainers, assert kết quả cuối thay vì verify `KafkaTemplate.send`.
- *Gọi API bên thứ ba thật trong test?* — Không: chậm, tốn tiền, flaky; dùng WireMock + contract test.

**⚠️ Câu trả lời gây điểm trừ:** "Mock hết cho nhanh" hoặc ngược lại "không mock gì, gọi thẳng API đối tác trong test".

**📖 Ôn lại:** [4.1 Managed vs unmanaged](../01-giao-trinh/15-testing.md#p4) · [3.1 Năm loại test double](../01-giao-trinh/15-testing.md#p3)

</details>

### Q5. 🟡 Over-mocking là gì? Nhận diện và sửa thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Over-mocking là khi test mô tả lại từng dòng implementation bằng `when`/`verify`. Triệu chứng: test dài bằng code được test, mock trả về mock, refactor nội bộ làm hàng chục test đỏ dù không có bug, hoặc test xanh nhưng production lỗi vì mock trả dữ liệu không thể có thật. Sửa: dùng object thật/fake bên trong, chỉ assert output và trạng thái quan sát được.

**Giải thích chi tiết:**
- Câu hỏi kiểm tra: *"Nếu tôi refactor nội bộ mà không đổi hành vi, test này có đỏ không?"*
- Ví dụ xấu: mock `mapper`, `priceCalculator`, `repo`, rồi `verify(mapper).toEntity(dto)`, `verify(entity).setTotal(...)`.
- Ví dụ tốt: `new OrderService(new OrderMapper(), new PriceCalculator(), new InMemoryOrderRepository())` → assert `total` và đơn đã tồn tại trong repo.
- Mock **value object/entity** (`mock(Money.class)`) là dấu hiệu rõ nhất — chỉ cần `new`.

**Câu hỏi nối tiếp:**
- *Khi nào `InOrder` là hợp lý?* — Khi thứ tự là yêu cầu nghiệp vụ quan sát được (ví dụ phải trừ kho trước khi publish event mà consumer giả định kho đã trừ); còn lại tránh.

**⚠️ Câu trả lời gây điểm trừ:** coi số lượng `verify` là thước đo chất lượng test.

**📖 Ôn lại:** [4.2 Over-mocking](../01-giao-trinh/15-testing.md#p4)

</details>

### Q6. 🔴 Khi nào fake tốt hơn mock? Rủi ro của fake và cách kiểm soát?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Fake (ví dụ `InMemoryOrderRepository` dùng `HashMap`) tốt hơn khi tương tác phức tạp: 20 dòng fake thay cho hàng chục `when(...)` và không gắn test vào thứ tự gọi. Rủi ro là fake **lệch hành vi** so với bản thật (không enforce unique constraint, không tăng version). Kiểm soát bằng **contract test cho fake**: một bộ test abstract chạy cho cả fake và implementation thật (JPA + Testcontainers).

**Giải thích chi tiết:**

```java
abstract class OrderRepositoryContract {
    protected abstract OrderRepository repo();
    @Test void savesAndFinds() { /* ... */ }
    @Test void rejectsDuplicateExternalId() { /* ... */ }
}
class InMemoryOrderRepositoryTest extends OrderRepositoryContract { /* trả fake */ }
@Testcontainers class JpaOrderRepositoryTest extends OrderRepositoryContract { /* trả JPA repo */ }
```

- Khác biệt hay gặp: JPA chỉ ném `DataIntegrityViolationException` khi **flush** (cần `saveAndFlush`); optimistic lock ném `ObjectOptimisticLockingFailureException`; fake phải lưu **bản sao** (nếu trả cùng reference, caller sửa object là "tự lưu").
- Fake cũng hữu ích ở mức Spring: fake bean có API cấu hình (`FakePaymentGateway.willDecline()`) giúp giữ **một** context dùng chung thay vì nhiều tổ hợp `@MockitoBean`.

**Câu hỏi nối tiếp:**
- *Ai bảo trì fake?* — Team sở hữu interface; fake nằm cạnh interface (test fixtures module) để mọi consumer dùng chung.

**⚠️ Câu trả lời gây điểm trừ:** "Fake là anti-pattern vì phải viết thêm code" hoặc dùng fake mà không có cách nào chứng minh nó giống bản thật.

**📖 Ôn lại:** [4.3 Fake tốt hơn mock khi nào?](../01-giao-trinh/15-testing.md#p4)

</details>

### Q7. 🟡 Một class vừa tính toán vừa gọi DB, API, gửi mail — rất khó test. Bạn refactor thế nào để dễ test?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Đây là "overcomplicated code" trong ma trận của Khorikov (vừa phức tạp vừa nhiều collaborator). Tách **logic thuần** (domain, không I/O) ra khỏi phần **điều phối** (application service mỏng). Logic thuần test bằng unit test 0 mock chạy micro giây; phần điều phối test bằng vài integration test. Đây là "functional core, imperative shell" / hexagonal.

**Giải thích chi tiết:**
- Trước: `checkout()` đọc repo, gọi `customerClient.isVip`, tính giảm giá, lưu, gửi mail → phải mock mọi thứ.
- Sau: `record Pricing(threshold, rate) { BigDecimal finalTotal(BigDecimal total, boolean vip) }` — tham số vào, giá trị ra; service chỉ lấy dữ liệu, gọi `Pricing`, lưu và phát side-effect.
- Bốn góc: trivial code (không test), domain/algorithm (unit test kỹ nhất), controller/orchestration (integration test vài case), overcomplicated (refactor trước).
- Inject `Clock`, `Supplier<UUID>`, `RandomGenerator` thay vì gọi `now()`, `randomUUID()` bên trong → không cần `mockStatic`.

**Câu hỏi nối tiếp:**
- *Code legacy không dám refactor?* — Viết characterization test (kể cả bằng `mockStatic`) chụp hành vi hiện tại, refactor, rồi thay bằng test sạch.

**⚠️ Câu trả lời gây điểm trừ:** "Dùng PowerMock/`mockStatic` để test được hết" mà không nói gì tới thiết kế.

**📖 Ôn lại:** [1.5 Phân loại code để quyết định chiến lược](../01-giao-trinh/15-testing.md#p1)

</details>

---

<a id="g2"></a>
## 2. JUnit 5 & AssertJ

### Q8. 🟢 Kiến trúc JUnit 5 gồm những gì? Khác JUnit 4 ở điểm nào quan trọng?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** JUnit 5 = **Platform** (launcher, tích hợp IDE/Maven/Gradle) + **Jupiter** (API & engine mới) + **Vintage** (chạy test JUnit 3/4). So với JUnit 4: thay `@RunWith`/`@Rule` bằng **Extension API** (nhiều extension cùng lúc), có `@Nested`, `@ParameterizedTest`, `@DisplayName`, `@Tag`, test method không cần `public`, assertion có lambda (`assertThrows`, `assertAll`).

**Giải thích chi tiết:**
- Nhờ Platform tách biệt, Spock, Cucumber, ArchUnit… đều chạy chung một launcher.
- Ánh xạ annotation: `@Before` → `@BeforeEach`, `@BeforeClass` → `@BeforeAll`, `@Ignore` → `@Disabled`, `@Category` → `@Tag`.
- Dùng BOM `org.junit:junit-bom` để Platform/Jupiter cùng version; Spring Boot `spring-boot-starter-test` đã kéo sẵn Jupiter, AssertJ, Mockito.
- Ghi chú: JUnit 6 (cuối 2025) thống nhất version, yêu cầu Java 17+, API Jupiter về cơ bản giữ nguyên.

**Câu hỏi nối tiếp:**
- *Migrate dần từ JUnit 4?* — Thêm Vintage engine để test cũ vẫn chạy, viết test mới bằng Jupiter, chuyển dần (OpenRewrite có recipe tự động).

**⚠️ Câu trả lời gây điểm trừ:** không biết Vintage, nghĩ phải viết lại toàn bộ test JUnit 4 một lần.

**📖 Ôn lại:** [2.1 Kiến trúc JUnit 5](../01-giao-trinh/15-testing.md#p2)

</details>

### Q9. 🟢 Lifecycle của một test class JUnit 5? `PER_METHOD` khác `PER_CLASS` thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `@BeforeAll` (1 lần) → với mỗi test: tạo instance mới → `@BeforeEach` → test → `@AfterEach` → cuối cùng `@AfterAll`. Mặc định `PER_METHOD`: mỗi test method chạy trên **instance mới** → field không rò state giữa các test. `PER_CLASS`: một instance cho cả class → `@BeforeAll` không cần `static`, nhưng phải tự reset state.

**Giải thích chi tiết:**
- `PER_METHOD` là cơ chế cách ly quan trọng nhất; đó là lý do field khởi tạo inline (`Calculator calc = new Calculator();`) an toàn.
- `PER_CLASS` hữu ích với Kotlin, với `@Nested` cần `@BeforeAll`, hoặc khi setup đắt — nhưng dễ tạo test phụ thuộc thứ tự.
- Thứ tự test method "xác định nhưng cố ý không hiển nhiên" — đừng phụ thuộc; nếu thật cần dùng `@TestMethodOrder(OrderAnnotation.class)`.
- `@Nested` phải là inner class **không static**; `@BeforeEach` của lớp ngoài chạy trước lớp trong.

**Câu hỏi nối tiếp:**
- *Vì sao `@BeforeAll` mặc định phải static?* — Vì nó chạy trước khi có instance nào (với `PER_METHOD`).

**⚠️ Câu trả lời gây điểm trừ:** nói "các test trong class dùng chung một object" (nhầm với `PER_CLASS`/TestNG).

**📖 Ôn lại:** [2.2 Lifecycle](../01-giao-trinh/15-testing.md#p2)

</details>

### Q10. 🟢 Parameterized test trong JUnit 5 có những nguồn dữ liệu nào? Khi nào dùng?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `@ValueSource` (1 tham số literal), `@NullAndEmptySource`, `@CsvSource`/`@CsvFileSource` (nhiều cột, chuyển kiểu ngầm định), `@MethodSource` (factory `Stream<Arguments>` cho object phức tạp), `@EnumSource`, `@ArgumentsSource` (provider tự viết). Dùng khi cùng một hành vi cần kiểm tra trên nhiều input — đặc biệt các **giá trị biên**.

**Giải thích chi tiết:**

```java
@ParameterizedTest(name = "{0} VND, vip={1} -> {2}")
@CsvSource(textBlock = """
    500000,  false, 500000
    2000000, true,  1800000
    """)
void pricing(BigDecimal total, boolean vip, BigDecimal expected) {
    assertThat(pricing.finalTotal(total, vip)).isEqualByComparingTo(expected);
}
```

- Đặt `name` rõ ràng để báo cáo CI đọc được case nào đỏ.
- Kiểu tự định nghĩa: `ArgumentConverter` hoặc `ArgumentsAggregator`.
- Đừng nhồi nhiều **hành vi khác nhau** vào một parameterized test với cột `expectException` — tách test cho happy path và lỗi.

**Câu hỏi nối tiếp:**
- *Khác `@RepeatedTest`?* — `@RepeatedTest(n)` chạy cùng input n lần, dùng để săn flaky test, không phải để đa dạng input.

**⚠️ Câu trả lời gây điểm trừ:** dùng vòng `for` trong một `@Test` để duyệt nhiều case — case đầu fail là dừng, báo cáo không biết case nào.

**📖 Ôn lại:** [2.4 Parameterized tests](../01-giao-trinh/15-testing.md#p2)

</details>

### Q11. 🟡 Extension model của JUnit 5 hoạt động thế nào? Bạn từng viết extension nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Extension implement các callback interface (`BeforeEachCallback`, `ParameterResolver`, `TestInstancePostProcessor`, `ExecutionCondition`, `TestWatcher`…) và được đăng ký bằng `@ExtendWith` (khai báo) hoặc `@RegisterExtension` (field, cấu hình bằng code). Nhiều extension cùng lúc, khác `@RunWith` chỉ một runner. State giữa các callback lưu trong `ExtensionContext.Store`. `SpringExtension`, `MockitoExtension`, Testcontainers đều là extension.

**Giải thích chi tiết:**
- Ví dụ thực tế: extension inject `Clock` cố định qua `ParameterResolver` và cảnh báo test chậm > 500ms bằng `BeforeTestExecutionCallback`/`AfterTestExecutionCallback`.
- Lưu state vào `ctx.getStore(Namespace.create(MyExt.class))`, **không** dùng field static — vỡ khi chạy song song.
- Gom cấu hình bằng **meta-annotation**: `@ExtendWith(...) @Tag("integration") public @interface IntegrationTest {}`.
- Test chính extension bằng `EngineTestKit` (`junit-platform-testkit`).

**Câu hỏi nối tiếp:**
- *Retry test bằng extension được không?* — `InvocationInterceptor` chỉ `proceed()` được một lần; retry phải dùng `TestTemplateInvocationContextProvider` (như `@RetryingTest` của junit-pioneer) — và phải **báo cáo** test cần retry, không giấu flaky.

**⚠️ Câu trả lời gây điểm trừ:** không biết `@RunWith` đã được thay thế; dùng static field làm state trong extension.

**📖 Ôn lại:** [2.7 Extension model](../01-giao-trinh/15-testing.md#p2)

</details>

### Q12. 🟡 Vì sao nhiều team bắt buộc dùng AssertJ thay vì `assertTrue`/`assertEquals`? Nêu vài assertion hay dùng.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Message lỗi là "UX của test". `assertTrue(list.contains(x))` khi đỏ chỉ in `expected: <true> but was: <false>`; AssertJ in rõ danh sách thực tế và phần tử thiếu. AssertJ còn fluent, có assertion chuyên biệt cho collection, exception, Optional, BigDecimal, recursive comparison, soft assertions.

**Giải thích chi tiết:**

```java
assertThat(orders).extracting(Order::id, Order::status)
                  .containsExactly(tuple(1L, PAID), tuple(3L, NEW));
assertThatThrownBy(() -> service.withdraw(acc, Money.of(1_000)))
    .isInstanceOf(InsufficientFundsException.class).hasMessageContaining("balance");
assertThat(actualDto).usingRecursiveComparison().ignoringFields("id", "createdAt").isEqualTo(expected);
assertThat(new BigDecimal("1.0")).isEqualByComparingTo("1.00"); // equals() của BigDecimal so cả scale
SoftAssertions.assertSoftly(s -> { s.assertThat(u.name()).isEqualTo("An"); s.assertThat(u.email()).endsWith("@x.com"); });
```

- `assertThat(actual)` loại bỏ lỗi đảo tham số `assertEquals(expected, actual)`.
- Có thể viết **custom assertion** (`AbstractAssert`) cho domain: `assertThat(order).isPaid().hasTotal("100.00")`.

**Câu hỏi nối tiếp:**
- *Khi nào dùng soft assertions?* — Khi kiểm tra nhiều field độc lập của một kết quả và muốn thấy mọi lỗi một lần; không dùng để gom nhiều hành vi vào một test.

**⚠️ Câu trả lời gây điểm trừ:** so sánh `BigDecimal` bằng `isEqualTo`/`equals`; `assertTrue(a.equals(b))`.

**📖 Ôn lại:** [2.6 Assertions: JUnit vs AssertJ](../01-giao-trinh/15-testing.md#p2)

</details>

### Q13. 🔴 Build CI xanh nhưng thực ra một số test không hề chạy. Những nguyên nhân nào có thể gây ra điều này và bạn phát hiện thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Hay gặp nhất: import nhầm `org.junit.Test` (JUnit 4) khi không có Vintage engine; Surefire quá cũ (< 2.22) không nhận JUnit Platform; Gradle quên `useJUnitPlatform()`; tên class không khớp pattern Surefire/Failsafe (`*IT` bị Surefire bỏ qua và Failsafe chưa cấu hình); `@Disabled`/tag bị loại trừ; `-DskipTests` còn sót trong pipeline. Phát hiện bằng cách **theo dõi số lượng test** trong báo cáo CI và bật fail khi không có test.

**Giải thích chi tiết:**
- Bật `failIfNoTests`/`surefire.failIfNoSpecifiedTests`; so sánh số test mỗi build (Develocity, báo cáo JUnit XML) — số test giảm đột ngột là tín hiệu.
- ArchUnit/Checkstyle rule cấm import `org.junit.Test` khi đã chuyển hẳn sang Jupiter.
- Mutation testing (PIT) cũng lộ ra test không chạy/không assert: mutant sống hàng loạt.
- Kiểm tra **test có thể đỏ không**: khi viết test mới, luôn thấy nó đỏ một lần (bước Red của TDD).
- `assertTimeoutPreemptive` chạy code ở thread khác → `@Transactional`, `ThreadLocal`, `SecurityContext` không có hiệu lực: test có thể "xanh sai".

**Câu hỏi nối tiếp:**
- *Exception trong thread worker làm test xanh?* — Có, nếu không `future.get()`; luôn propagate exception từ worker.

**⚠️ Câu trả lời gây điểm trừ:** "CI xanh thì chắc chắn test đã chạy hết" — không có cơ chế kiểm tra số test.

**📖 Ôn lại:** [2.8 Chạy song song & lỗi thường gặp](../01-giao-trinh/15-testing.md#p2) · [Module 16 — Surefire vs Failsafe](../01-giao-trinh/16-devops-build-cloud-security.md#p1)

</details>

### Q14. 🟡 Bật chạy song song trong JUnit 5 thế nào? Rủi ro là gì?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Cấu hình trong `junit-platform.properties`: `parallel.enabled=true`, an toàn nhất là chạy **class song song, method trong class tuần tự** (`mode.default=same_thread`, `mode.classes.default=concurrent`). Rủi ro: state dùng chung (static, system property, file, port cố định, DB dùng chung) gây flaky. Khai báo tài nguyên dùng chung bằng `@ResourceLock`, test không thể song song đánh dấu `@Isolated`.

**Giải thích chi tiết:**

```properties
junit.jupiter.execution.parallel.enabled=true
junit.jupiter.execution.parallel.mode.default=same_thread
junit.jupiter.execution.parallel.mode.classes.default=concurrent
junit.jupiter.execution.parallel.config.strategy=dynamic
```

- Với Spring: context cache **theo JVM** — chạy song song trong cùng JVM vẫn chia sẻ context (bean singleton có state mutable sẽ bị tranh chấp). Fork nhiều JVM (`forkCount`) thì mỗi fork có cache riêng → tốn RAM, khởi động nhiều context hơn.
- Dữ liệu DB: mỗi test tự tạo dữ liệu với id riêng thay vì giả định bảng rỗng.

**Câu hỏi nối tiếp:**
- *Song song trong JVM hay fork JVM?* — Unit test thuần: song song trong JVM. Integration test Spring: thường fork vài JVM vừa đủ RAM, mỗi fork 1 context.

**⚠️ Câu trả lời gây điểm trừ:** bật song song toàn bộ method rồi "tăng retry" khi test đỏ ngẫu nhiên.

**📖 Ôn lại:** [2.8 Chạy song song](../01-giao-trinh/15-testing.md#p2)

</details>

---

<a id="g3"></a>
## 3. Mockito & test doubles

### Q15. 🟢 Phân biệt dummy, stub, spy, mock, fake. Vì sao "không bao giờ verify stub"?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Dummy lấp tham số; **stub** trả dữ liệu đóng sẵn cho input của SUT; spy = stub + ghi lại lời gọi; **mock** kiểm tra tương tác đi ra (side-effect); **fake** là implementation chạy được nhưng đơn giản (in-memory). Stub mô phỏng dữ liệu *đi vào* — verify nó là ràng buộc vào cách SUT lấy dữ liệu (implementation), nên giòn và vô nghĩa.

**Giải thích chi tiết:**
- Theo CQS: query → stub; command → mock.
- Trong Mockito, cùng một object `@Mock` có thể đóng vai stub (`when(...)`) hoặc mock (`verify(...)`) — phân biệt theo **cách dùng**, không theo API.
- Ví dụ: `when(rateApi.rate("USD")).thenReturn(25_000)` là stub; `verify(mailer).send(...)` là mock; `InMemoryOrderRepository` là fake.

**Câu hỏi nối tiếp:**
- *Strict stubs có liên quan gì?* — Stub không được dùng sẽ ném `UnnecessaryStubbingException`, giúp phát hiện stub thừa mà không cần verify.

**⚠️ Câu trả lời gây điểm trừ:** dùng "mock" cho mọi loại double và `verify(repo).findById(...)` sau mỗi stub.

**📖 Ôn lại:** [3.1 Năm loại test double](../01-giao-trinh/15-testing.md#p3)

</details>

### Q16. 🟢 `when(...).thenReturn(...)` khác `doReturn(...).when(...)` thế nào? Bẫy của spy là gì?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `when(spy.get(0))` **gọi phương thức thật** trước khi stub (với spy có thể ném exception hoặc gây side-effect), còn `doReturn("x").when(spy).get(0)` không gọi phương thức thật. `doThrow/doNothing/doAnswer` cũng là cách duy nhất stub method `void`. Spy sao chép state của object gốc; partial mock là mùi thiết kế.

**Giải thích chi tiết:**

```java
List<String> spy = spy(new ArrayList<>());
when(spy.get(0)).thenReturn("x");      // ❌ IndexOutOfBoundsException: get(0) thật chạy trước
doReturn("x").when(spy).get(0);         // ✅
doThrow(new IOException()).when(storage).delete("x"); // stub method void
```

- `spy(real)` tạo bản sao: sửa `real` sau đó không ảnh hưởng `spy`.
- Nếu cần stub một nửa class để test nửa kia → class làm hai việc → tách ra.
- Mockito 5 mock được final class/method nhờ inline mock maker mặc định, nhưng tốn chi phí instrumentation.

**Câu hỏi nối tiếp:**
- *`@MockitoSpyBean` trên bean có `@Transactional`?* — Bean là AOP proxy; spy bọc proxy có thể làm hành vi khác mong đợi — tránh, ưu tiên fake.

**⚠️ Câu trả lời gây điểm trừ:** dùng spy làm mặc định "để vừa có hàm thật vừa stub được".

**📖 Ôn lại:** [3.2 Mockito cơ bản](../01-giao-trinh/15-testing.md#p3) · [3.4 Spy và những cái bẫy](../01-giao-trinh/15-testing.md#p3)

</details>

### Q17. 🟡 Khi nào dùng `ArgumentCaptor`? Quy tắc "argument matcher" là gì?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Dùng captor khi object được tạo **bên trong** SUT (email, event) và cần assert nhiều field của nó sau khi `verify`. Không dùng captor với `when(...)` — dùng matcher. Quy tắc matcher: nếu một tham số dùng matcher thì **tất cả** tham số phải là matcher (`eq(42L), any(Order.class)`), nếu không sẽ `InvalidUseOfMatchersException`.

**Giải thích chi tiết:**

```java
@Captor ArgumentCaptor<Email> emailCaptor;

verify(mailer).send(emailCaptor.capture());
Email sent = emailCaptor.getValue();
assertThat(sent.to()).isEqualTo("an@x.com");
assertThat(sent.subject()).contains("Chào mừng");
```

- Nhiều lần gọi: `getAllValues()`.
- Thay thế gọn hơn khi chỉ kiểm tra 1–2 điều kiện: `verify(mailer).send(argThat(m -> m.to().equals("an@x.com")))`.
- Nếu event là record/value object có `equals`, `verify(events).publish(new MoneyTransferred(1L, 2L, Money.of(30)))` là rõ nhất.

**Câu hỏi nối tiếp:**
- *Async thì sao?* — `verify(mailer, timeout(1000)).send(captor.capture())`.

**⚠️ Câu trả lời gây điểm trừ:** dùng captor để lấy tham số rồi assert trong `when` (stub), hoặc trộn literal với matcher.

**📖 Ôn lại:** [3.3 ArgumentCaptor](../01-giao-trinh/15-testing.md#p3)

</details>

### Q18. 🟡 Strict stubs trong Mockito là gì? `UnnecessaryStubbingException` báo hiệu điều gì?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `MockitoExtension` mặc định dùng `Strictness.STRICT_STUBS`: stub không được dùng → `UnnecessaryStubbingException`; stub với argument A nhưng code gọi với argument B → `PotentialStubbingProblem`. Nó giữ test gọn và báo sớm test đã lỗi thời hoặc stub sai tham số (thay vì trả `null` khó hiểu rồi NPE ở chỗ khác).

**Giải thích chi tiết:**
- Stub thừa thường do copy-paste setup hoặc code đã đổi mà test chưa cập nhật → xóa stub.
- Nới lỏng có chủ đích: `lenient().when(...)` cho một stub, `@MockitoSettings(strictness = LENIENT)` cho cả class — phải có lý do (ví dụ setup dùng chung cho `@Nested`).
- `@MockitoBean` trong Spring reset sau mỗi test (`MockReset.AFTER`) — đừng stub trong `@BeforeAll`.

**Câu hỏi nối tiếp:**
- *Quên `@ExtendWith(MockitoExtension.class)`?* — Field `@Mock` null → NPE ở `when(...)`.

**⚠️ Câu trả lời gây điểm trừ:** "gặp lỗi này thì thêm `lenient()` cho hết".

**📖 Ôn lại:** [3.6 Strict stubs](../01-giao-trinh/15-testing.md#p3)

</details>

### Q19. 🔴 `mockStatic` hoạt động thế nào? Vì sao nói nó là "chữa cháy"? Có bẫy gì khi code chạy đa luồng?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `mockStatic(UUID.class)` dùng inline mock maker (bytecode instrumentation qua Java agent) để chặn lời gọi static, **chỉ có hiệu lực trong thread gọi `mockStatic`** và trong phạm vi `try-with-resources`. Nó là chữa cháy cho legacy: code mới cần mock static nghĩa là phụ thuộc ẩn vào thời gian/UUID/random — nên inject `Clock`, `Supplier<UUID>`, `RandomGenerator`.

**Giải thích chi tiết:**

```java
try (MockedStatic<UUID> mocked = mockStatic(UUID.class)) {
    mocked.when(UUID::randomUUID).thenReturn(fixed);
    assertThat(idGenerator.next()).isEqualTo(fixed.toString());
} // ra khỏi try: hành vi thật được khôi phục
```

- Bẫy đa luồng: code chạy trong `CompletableFuture.supplyAsync`/executor sẽ thấy **method thật** → test chập chờn hoặc sai.
- Quên đóng `MockedStatic` (không dùng try-with-resources) → rò sang test sau, gây flaky.
- Quy trình gỡ: (1) characterization test bằng `mockStatic`/`mockConstruction`; (2) refactor inject `Clock`, `IdGenerator`, `RateClient` (giữ constructor cũ gọi constructor mới với `Clock.systemUTC()`); (3) viết lại test không static mock, xóa test cũ.

**Câu hỏi nối tiếp:**
- *Mock `LocalDateTime.now()` được không?* — Được về kỹ thuật, nhưng `Clock.fixed(...)` đơn giản, nhanh và an toàn đa luồng hơn.
- *PowerMock?* — Không còn được duy trì tốt, không hợp JDK hiện đại; Mockito inline thay thế.

**⚠️ Câu trả lời gây điểm trừ:** coi `mockStatic` là cách bình thường để test code mới; không biết giới hạn theo thread.

**📖 Ôn lại:** [3.5 Mock static, constructor, final](../01-giao-trinh/15-testing.md#p3)

</details>

### Q20. 🔴 Nâng lên JDK 21 và Mockito 5, log test xuất hiện cảnh báo về "dynamically loaded agent". Chuyện gì đang xảy ra và bạn xử lý thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Mockito 5 dùng **inline mock maker** mặc định, tự attach Byte Buddy agent vào JVM lúc runtime. JEP 451 (JDK 21) cảnh báo việc nạp agent động và hướng tới chặn mặc định trong tương lai. Cách đúng: nạp Mockito như `-javaagent` tường minh trong Surefire/Gradle, và giữ `@{argLine}` để không ghi đè agent của JaCoCo.

**Giải thích chi tiết:**

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

- `dependency:properties` tạo property trỏ tới đường dẫn jar của artifact.
- Thiếu `@{argLine}` → JaCoCo `prepare-agent` bị ghi đè → coverage 0%.
- Tạm thời có `-XX:+EnableDynamicAgentLoading` để tắt cảnh báo, nhưng đó không phải giải pháp lâu dài.
- Mockito 5 yêu cầu Java 11+, không cần artifact `mockito-inline` riêng nữa.

**Câu hỏi nối tiếp:**
- *Vì sao JDK siết agent động?* — Tính toàn vẹn của nền tảng (integrity by default): code thư viện không nên âm thầm sửa bytecode mà ứng dụng không cho phép.

**⚠️ Câu trả lời gây điểm trừ:** "chỉ là warning, bỏ qua" mà không biết nó sẽ thành lỗi ở JDK tương lai.

**📖 Ôn lại:** [3.7 Mockito trên JDK hiện đại](../01-giao-trinh/15-testing.md#p3) · [10.1 JaCoCo và argLine](../01-giao-trinh/15-testing.md#p10)

</details>

### Q21. 🟡 "Only mock types you own" nghĩa là gì? Vì sao không nên mock `RestTemplate`, `JdbcTemplate`, `KafkaTemplate`?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Mock kiểu của bên thứ ba thì test chỉ kiểm tra bạn gọi API theo cách bạn **nghĩ** là đúng — nếu hiểu sai API (URL, header, serialization, exception khi 4xx), test vẫn xanh. Hãy bọc chúng bằng **adapter của mình** (interface theo ngôn ngữ domain, ví dụ `PaymentGateway`), mock/fake interface đó trong unit test, và test adapter bằng integration test thật (WireMock, Testcontainers).

**Giải thích chi tiết:**
- Mock `RestTemplate.exchange(...)` vừa giòn (nhiều overload, generic) vừa không bắt được lỗi mapping JSON, timeout, status code.
- Adapter test: `@RestClientTest` + `MockRestServiceServer`, hoặc WireMock trả 500/timeout để kiểm tra retry; `KafkaTemplate` → Kafka Testcontainers + consumer test.
- Đây là nguyên tắc GOOS, đi cùng "mock roles, not objects".

**Câu hỏi nối tiếp:**
- *Mock `Repository` của Spring Data thì sao?* — Repository là interface bạn khai báo, nhưng behavior thật (query derivation, flush) do framework sinh → test bằng `@DataJpaTest` thật; trong unit test của service, ưu tiên fake.

**⚠️ Câu trả lời gây điểm trừ:** `when(restTemplate.getForObject(anyString(), eq(Rate.class))).thenReturn(...)` rải khắp test service.

**📖 Ôn lại:** [3.7 Lỗi thường gặp](../01-giao-trinh/15-testing.md#p3) · [4.1 Hai trường phái](../01-giao-trinh/15-testing.md#p4)

</details>

---

<a id="g4"></a>
## 4. TDD

### Q22. 🟢 Mô tả chu trình TDD và ba luật của TDD.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Red (viết test nhỏ nhất cho hành vi tiếp theo, thấy đỏ **đúng lý do**) → Green (code đơn giản nhất cho xanh, được phép hard-code) → Refactor (dọn cả production lẫn test code, giữ xanh). Ba luật (Uncle Bob): không viết production code khi chưa có test đỏ; không viết test nhiều hơn mức đủ để đỏ (không compile cũng là đỏ); không viết production code nhiều hơn mức đủ để xanh.

**Giải thích chi tiết:**
- Triangulation: thêm ví dụ thứ hai để buộc tổng quát hóa ("1,2" → 3 rồi "1,2,3,4" → 10).
- F.I.R.S.T.: Fast, Independent, Repeatable, Self-validating, Timely.
- Bỏ bước Refactor thì TDD chỉ còn "test-first" và code vẫn bẩn.
- Không chạy để thấy đỏ trước → test có thể luôn xanh (assert sai, quên `@Test`).

**Câu hỏi nối tiếp:**
- *Bước nhảy bao lớn là vừa?* — Đủ nhỏ để ở trạng thái đỏ vài phút; nếu debug lâu ở Red thì bước quá lớn, lùi lại.

**⚠️ Câu trả lời gây điểm trừ:** "TDD là viết hết test trước rồi mới code".

**📖 Ôn lại:** [5.1 Chu trình và ba luật](../01-giao-trinh/15-testing.md#p5) · [5.2 Kata String Calculator](../01-giao-trinh/15-testing.md#p5)

</details>

### Q23. 🟡 Inside-out và outside-in TDD khác nhau thế nào? "Double-loop TDD" là gì?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Inside-out (classical) bắt đầu từ domain nhỏ nhất rồi xây ra ngoài — hợp thuật toán, domain logic. Outside-in (London/GOOS) bắt đầu bằng một **acceptance test đỏ ở biên** (HTTP), rồi TDD từng tầng vào trong, mock *role* chưa tồn tại để khám phá interface. Double-loop: vòng ngoài là acceptance test (đỏ lâu), vòng trong là các chu trình unit test nhanh; acceptance chỉ xanh khi mọi tầng xong.

**Giải thích chi tiết:**
- Ví dụ: `@SpringBootTest(RANDOM_PORT)` cho `POST /api/carts/{id}/checkout` (đỏ) → `@WebMvcTest` cho controller → unit test application service với fake repository → unit test `Cart`.
- Outside-in giảm rủi ro "xây domain đẹp nhưng không ai cần"; inside-out giảm mock và giòn.
- Thực tế: outside-in để định hướng, nhưng khi vào domain thì dùng phong cách classical.

**Câu hỏi nối tiếp:**
- *Mock ở outside-in có làm test giòn?* — Có thể; sau khi collaborator thật đã xong, cân nhắc thay mock bằng object thật/fake.

**⚠️ Câu trả lời gây điểm trừ:** không phân biệt được, hoặc cho rằng TDD chỉ áp dụng cho unit test.

**📖 Ôn lại:** [5.3 TDD ngoài đời thật](../01-giao-trinh/15-testing.md#p5)

</details>

### Q24. 🟡 "Bạn có làm TDD không?" — trả lời thế nào cho trung thực mà vẫn thể hiện level Senior? TDD với legacy code ra sao?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Trả lời trung thực kèm ngữ cảnh: "Tôi TDD cho domain logic và **mọi bug fix** (viết test tái hiện bug trước khi sửa); với glue code/cấu hình, tôi viết integration test sau. Với spike khám phá công nghệ, tôi code thử rồi vứt, sau đó TDD bản thật." Với legacy: viết **characterization test** chụp hành vi hiện tại (kể cả hành vi sai) trước khi refactor (Feathers).

**Giải thích chi tiết:**
- Giá trị chính của TDD là **thiết kế** (code testable tự nhiên ít coupling) và **feedback loop ngắn**, không phải "nghi lễ".
- Legacy: tìm *seam* (điểm có thể thay hành vi mà không sửa code tại chỗ), bọc bằng test, refactor từng bước nhỏ; chấp nhận `mockStatic` tạm thời ở bước characterization.
- Bug fix TDD: test đỏ tái hiện bug → sửa → xanh → bug không quay lại; commit test cùng fix.

**Câu hỏi nối tiếp:**
- *Characterization test chụp cả bug thì sao?* — Ghi chú rõ, sửa sau khi đã có lưới an toàn; khi sửa bug thì đổi test có chủ đích.

**⚠️ Câu trả lời gây điểm trừ:** "100% TDD mọi lúc" (khó tin) hoặc "TDD vô dụng, tốn thời gian" (thiếu hiểu biết).

**📖 Ôn lại:** [5.3 TDD ngoài đời thật](../01-giao-trinh/15-testing.md#p5)

</details>

---

<a id="g5"></a>
## 5. Spring Boot testing

### Q25. 🟢 `@SpringBootTest` khác các slice test (`@WebMvcTest`, `@DataJpaTest`, `@JsonTest`…) thế nào? Khi nào dùng gì?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `@SpringBootTest` nạp **toàn bộ** ApplicationContext như production — dùng cho integration test luồng xuyên nhiều tầng. Slice test chỉ nạp một tầng: `@WebMvcTest` (controller, advice, filter, Jackson, security), `@DataJpaTest` (JPA, repository, DataSource, migration, mỗi test rollback), `@JsonTest` (ObjectMapper), `@RestClientTest` (client HTTP + `MockRestServiceServer`). Unit test domain thì **không dùng Spring**.

**Giải thích chi tiết:**
- Slice hoạt động nhờ `@TypeExcludeFilters` + `@ImportAutoConfiguration` chỉ chọn một nhóm auto-configuration.
- Chọn theo câu hỏi cần trả lời: mapping URL/validation/status code → `@WebMvcTest`; query/constraint/mapping entity → `@DataJpaTest`; format JSON (public contract) → `@JsonTest`; transaction + Kafka + cache phối hợp → `@SpringBootTest`.
- `@WebMvcTest(OrderController.class)` — luôn chỉ định controller, nếu không sẽ nạp mọi controller và phải mock mọi service.

**Câu hỏi nối tiếp:**
- *Spring Boot 4 thay đổi gì?* — Tách module test autoconfigure (ví dụ `spring-boot-webmvc-test`), đổi package một số annotation; khái niệm giữ nguyên.

**⚠️ Câu trả lời gây điểm trừ:** "Dùng `@SpringBootTest` cho mọi test cho chắc" → suite 20 phút.

**📖 Ôn lại:** [6.1 Bức tranh tổng thể](../01-giao-trinh/15-testing.md#p6)

</details>

### Q26. 🟡 Viết `@WebMvcTest` cho endpoint có Spring Security, test trả 401/403 không như mong đợi. Vì sao và xử lý thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Khi Spring Security có trên classpath, `@WebMvcTest` bật **auto-configuration bảo mật mặc định của Boot** (mọi endpoint yêu cầu xác thực, CSRF bật), nhưng class `@Configuration` định nghĩa `SecurityFilterChain` của bạn **không** được slice quét tới. Phải `@Import(SecurityConfig.class)` để test đúng rule thật, dùng `@WithMockUser(roles = "ADMIN")` hoặc `.with(jwt().authorities(...))`, và `.with(csrf())` cho POST khi CSRF bật.

**Giải thích chi tiết:**

```java
@WebMvcTest(ProductController.class)
@Import(SecurityConfig.class)
class ProductControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean ProductService service;

    @Test @WithMockUser(roles = "USER")
    void userCannotDelete() throws Exception {
        mvc.perform(delete("/api/products/1").with(csrf()))
           .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
}
```

- Nếu `SecurityConfig` phụ thuộc bean khác (JWT decoder, user service) → cần `@MockitoBean` hoặc `@TestConfiguration` bổ sung.
- Method security (`@PreAuthorize` trên service) cần proxy của bean thật → test ở `@SpringBootTest` hoặc import cấu hình method security + bean thật.
- Từ Spring Framework 6.2 có `MockMvcTester` tích hợp AssertJ.

**Câu hỏi nối tiếp:**
- *Test rule phân quyền có quan trọng không?* — Rất quan trọng: IDOR/Broken Access Control là OWASP A01; rule phân quyền cần test tự động như logic nghiệp vụ.

**⚠️ Câu trả lời gây điểm trừ:** tắt security trong test (`addFilters = false`) cho mọi test rồi kết luận "endpoint đã được bảo vệ".

**📖 Ôn lại:** [6.2 `@WebMvcTest` + MockMvc](../01-giao-trinh/15-testing.md#p6)

</details>

### Q27. 🟡 `@MockBean` khác `@MockitoBean` thế nào? Vì sao phải chuyển?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `@MockBean`/`@SpyBean` của Spring Boot bị **deprecated từ Boot 3.4** (đã gỡ ở Boot 4), thay bằng `@MockitoBean`/`@MockitoSpyBean` của **Spring Framework 6.2** (package `org.springframework.test.context.bean.override.mockito`), dựa trên cơ chế *Bean Override* chung của TestContext. Còn có `@TestBean` để thay bean bằng instance từ static factory method — tiện cho fake.

**Giải thích chi tiết:**
- `@MockitoBean` khai báo trên field của test class (bản mới hơn hỗ trợ cả type-level/`@Nested`); không đặt trên `@Configuration` như `@MockBean` từng cho phép.
- Mặc định reset sau mỗi test (`MockReset.AFTER`).
- Cả hai đều là một phần **khóa cache context** → mỗi tổ hợp bean bị override khác nhau = một context mới.
- Với Boot 2.x/3.0–3.3 vẫn dùng `@MockBean`; khi nâng cấp, đổi import và kiểm tra vị trí khai báo.

**Câu hỏi nối tiếp:**
- *Vì sao `@MockitoBean` "tiện tay" làm suite chậm?* — Xem Q28: mỗi bộ mock khác nhau tạo context mới.

**⚠️ Câu trả lời gây điểm trừ:** không biết `@MockBean` đã deprecated khi ứng tuyển vị trí dùng Boot 3.4+.

**📖 Ôn lại:** [6.3 `@MockBean` → `@MockitoBean`](../01-giao-trinh/15-testing.md#p6)

</details>

### Q28. 🔴 Test suite Spring Boot chạy 25 phút, log cho thấy context khởi động hàng chục lần. Giải thích cơ chế context caching và cách bạn tối ưu.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Spring TestContext **cache ApplicationContext theo JVM**, khóa là `MergedContextConfiguration`: class cấu hình, active profiles, property sources (`@TestPropertySource`, `properties=`), context customizers — **bao gồm tập bean bị `@MockitoBean`/`@MockBean` override** — `webEnvironment`, initializers. Mỗi tổ hợp khác nhau = một context mới (5–20s mỗi lần, giữ pool DB, consumer Kafka). `@DirtiesContext` phá cache. Tối ưu bằng cách **chuẩn hóa cấu hình** để nhiều class dùng chung một context.

**Giải thích chi tiết:**
- Đo trước: bật `logging.level.org.springframework.test.context.cache=DEBUG` để thấy hit/miss, đếm số context.
- Chiến lược:
  1. Một (hoặc vài) **base class/meta-annotation** cho integration test: cùng profile, cùng Testcontainers, cùng bộ `@MockitoBean` (chỉ unmanaged dependency).
  2. Biến thể hành vi → **fake bean cấu hình được** (`FakePaymentGateway.willDecline()`) + reset trong `@AfterEach`, thay vì mock khác nhau mỗi class.
  3. Thay `@DirtiesContext` bằng dọn dữ liệu (`TRUNCATE`, `@Sql`) hoặc reset state bean.
  4. Slice test cho test một tầng; unit test không dùng Spring.
  5. Cache tối đa 32 context (`spring.test.context.cache.maxSize`, LRU); fork nhiều JVM thì mỗi fork có cache riêng.

```java
@SpringBootTest
@ActiveProfiles("it")
@Import({ContainersConfig.class, TestFakesConfig.class})
public abstract class AbstractIT {
    @Autowired protected FakePaymentGateway payments;
    @AfterEach void resetFakes() { payments.reset(); }
}
```

**Câu hỏi nối tiếp:**
- *Context bị đẩy khỏi cache thì sao?* — Bị đóng; nếu container Testcontainers gắn với vòng đời khác có thể gây connection refused (xem Q33).
- *Kết quả thực tế kỳ vọng?* — Từ hàng chục context xuống 1–2; thời gian suite thường giảm nhiều lần.

**⚠️ Câu trả lời gây điểm trừ:** "Mua máy CI mạnh hơn" hoặc "chạy song song nhiều fork" mà không giảm số context.

**📖 Ôn lại:** [6.4 Context caching — vì sao suite chậm](../01-giao-trinh/15-testing.md#p6)

</details>

### Q29. 🔴 Đặt `@Transactional` trên integration test để tự rollback dữ liệu — tiện, nhưng che giấu những bug gì? `MOCK` và `RANDOM_PORT` khác nhau thế nào ở điểm này?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Với `webEnvironment = MOCK` (mặc định), request MockMvc chạy **cùng thread** với test → nằm trong transaction của test → rollback tiện nhưng che bug: lazy loading "chạy được" (session còn mở) trong khi production ném `LazyInitializationException`; không flush nên constraint violation/lỗi SQL không xảy ra; `@TransactionalEventListener(AFTER_COMMIT)` không bao giờ chạy. Với `RANDOM_PORT`, request chạy trên thread server → **không** nằm trong transaction test, dữ liệu commit thật → phải tự dọn.

**Giải thích chi tiết:**
- Tái hiện: entity `Order` có `@OneToMany(fetch = LAZY) lines`, `spring.jpa.open-in-view=false`, controller serialize entity. Test `@Transactional` + MockMvc xanh; test `RANDOM_PORT` đỏ.
- Sửa bug thật: trả DTO, dùng `JOIN FETCH`/`@EntityGraph`; thêm assert số câu SQL để chống N+1.
- Dọn dữ liệu khi không dùng transaction test: `@Sql(executionPhase = AFTER_TEST_METHOD)`, `TRUNCATE ... RESTART IDENTITY CASCADE`, hoặc mỗi test tạo dữ liệu với id riêng.
- Với `@DataJpaTest` (mặc định transactional), dùng `em.flush(); em.clear();` để ép SQL chạy và đọc lại từ DB.

**Câu hỏi nối tiếp:**
- *Vậy không bao giờ dùng `@Transactional` trên test?* — Dùng được cho repository test có `flush/clear`; với test luồng nghiệp vụ end-to-end, ưu tiên commit thật và dọn dữ liệu.
- *`open-in-view` liên quan gì?* — OSIV mặc định `true` giữ session tới khi render view → cũng che lazy loading; nên tắt ở service REST.

**⚠️ Câu trả lời gây điểm trừ:** "Test có `@Transactional` là best practice, rollback sạch sẽ" mà không biết các bẫy.

**📖 Ôn lại:** [6.5 webEnvironment và transaction pitfall](../01-giao-trinh/15-testing.md#p6)

</details>

### Q30. 🟡 Viết test cho một query JPA thế nào cho đúng? Vì sao cần `flush()` và `clear()`?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Dùng `@DataJpaTest` với `@AutoConfigureTestDatabase(replace = NONE)` + PostgreSQL Testcontainers (`@ServiceConnection`), schema tạo bởi chính Flyway migration. Chuẩn bị dữ liệu qua `TestEntityManager`, rồi `em.flush(); em.clear();` — flush ép SQL INSERT chạy thật (lộ constraint), clear xóa persistence context để `find`/query đọc lại từ DB thay vì trả object từ L1 cache.

**Giải thích chi tiết:**
- Không `clear()` → test không chứng minh mapping/query đúng, chỉ chứng minh cache cấp 1 hoạt động.
- Phát hiện N+1: `hibernate.generate_statistics` + `Statistics.getPrepareStatementCount()`, hoặc datasource-proxy để assert số câu SQL.
- So sánh `BigDecimal` trong collection: `usingElementComparator(BigDecimal::compareTo)`.

**Câu hỏi nối tiếp:**
- *Test native query dùng `jsonb`?* — Chỉ chạy được trên PostgreSQL thật → thêm lý do không dùng H2.

**⚠️ Câu trả lời gây điểm trừ:** `save()` rồi `findById()` ngay trong cùng persistence context và kết luận mapping đúng.

**📖 Ôn lại:** [6.6 `@DataJpaTest`](../01-giao-trinh/15-testing.md#p6)

</details>

---

<a id="g6"></a>
## 6. Testcontainers

### Q31. 🟢 Vì sao không nên dùng H2 thay PostgreSQL/MySQL trong integration test?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Vì H2 khác database thật: khác SQL dialect, thiếu kiểu dữ liệu (`jsonb`, array, một số hành vi UUID), khác cú pháp (`ON CONFLICT`, window/CTE đặc thù), khác hành vi locking/isolation/sequence; migration Flyway viết cho Postgres có thể không chạy trên H2. Test xanh trên H2 **không chứng minh** code chạy được trên production. Testcontainers cho DB thật với chi phí vài giây một lần cho cả suite.

**Giải thích chi tiết:**
- H2 "compatibility mode" chỉ mô phỏng một phần.
- Testcontainers đổi cán cân chi phí → áp dụng được lời khuyên của Khorikov: test managed dependency bằng instance thật.
- Ngoại lệ chấp nhận được: thư viện/sample nhỏ không có SQL đặc thù — nhưng trong service production thì không.

**Câu hỏi nối tiếp:**
- *CI không có Docker thì sao?* — Dùng runner có Docker (GitHub Actions `ubuntu-latest` có sẵn), Docker-in-Docker với cấu hình đúng, hoặc Testcontainers Cloud.

**⚠️ Câu trả lời gây điểm trừ:** "H2 nhanh hơn và đủ dùng" mà không biết khác biệt dialect/locking.

**📖 Ôn lại:** [7.5 Góc nhìn Senior](../01-giao-trinh/15-testing.md#p7) · [6 Lỗi thường gặp](../01-giao-trinh/15-testing.md#p6)

</details>

### Q32. 🟡 Testcontainers quản lý vòng đời container thế nào? Field `static` và không `static` khác gì? Singleton container pattern là gì?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Testcontainers điều khiển Docker qua Docker API, mỗi container lộ **port ngẫu nhiên** (`getMappedPort`). Với `@Testcontainers` + `@Container`: field `static` → start 1 lần cho cả class; field instance → start lại cho **mỗi test** (rất chậm). Singleton pattern: khai báo container `static final` trong base class, start trong static initializer, **không stop** — container sống suốt JVM, container phụ **Ryuk** dọn khi JVM kết thúc (kể cả bị kill).

**Giải thích chi tiết:**

```java
public abstract class AbstractIntegrationTest {
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:16-alpine");
    static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("apache/kafka:3.8.0"));
    static { Startables.deepStart(PG, KAFKA).join(); }   // start song song

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", PG::getJdbcUrl);
        r.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }
}
```

- Port ngẫu nhiên → chạy song song an toàn, không đụng DB local.
- Singleton tương thích với context caching của Spring: mọi class kế thừa dùng chung container và (nếu cấu hình giống nhau) chung context.

**Câu hỏi nối tiếp:**
- *Gọi `stop()` trong `@AfterAll` với singleton?* — Class sau dùng container đã chết → lỗi.

**⚠️ Câu trả lời gây điểm trừ:** dùng port cố định 5432, hoặc container non-static cho từng test.

**📖 Ôn lại:** [7.1–7.2 Testcontainers & vòng đời container](../01-giao-trinh/15-testing.md#p7)

</details>

### Q33. 🔴 Suite chạy từng class riêng thì xanh, chạy cả suite thì class thứ hai lỗi "Connection refused" tới PostgreSQL container. Vì sao? `@ServiceConnection` giải quyết thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Đây là xung đột vòng đời: `@Container static` gắn với **vòng đời JUnit của class** (stop sau class), còn ApplicationContext được **Spring cache giữa các class**. Class thứ hai lấy lại context từ cache — context đó trỏ tới URL/port của container đã bị stop → connection refused. Giải pháp: singleton container (không stop) hoặc khai báo container là **bean** với `@ServiceConnection` (Boot 3.1+) để vòng đời container gắn với chính context được cache.

**Giải thích chi tiết:**

```java
@TestConfiguration(proxyBeanMethods = false)
public class ContainersConfig {
    @Bean @ServiceConnection
    PostgreSQLContainer<?> postgres() { return new PostgreSQLContainer<>("postgres:16-alpine"); }
}

@SpringBootTest
@Import(ContainersConfig.class)
class OrderFlowIT { }
```

- `@ServiceConnection` tạo `ConnectionDetails` bean từ container, thay cho `@DynamicPropertySource`.
- Container là bean → start khi context tạo, stop khi context đóng → nhất quán với cache.
- Bonus: dùng cùng config cho dev local: `SpringApplication.from(App::main).with(ContainersConfig.class).run(args)`.

**Câu hỏi nối tiếp:**
- *Nếu hai base class có cấu hình khác nhau?* — Hai context, mỗi context một bộ container; vẫn đúng nhưng tốn tài nguyên → gom cấu hình.

**⚠️ Câu trả lời gây điểm trừ:** "Thêm `@DirtiesContext` cho hết lỗi" — chữa triệu chứng bằng cách làm suite chậm gấp nhiều lần.

**📖 Ôn lại:** [7.3 `@ServiceConnection`](../01-giao-trinh/15-testing.md#p7) · [7 Lỗi thường gặp](../01-giao-trinh/15-testing.md#p7)

</details>

### Q34. 🟡 Làm sao để integration test với Testcontainers vừa nhanh vừa ổn định?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Pin version image (không `latest`); singleton container + context caching để mỗi container chỉ start 1 lần/JVM; dọn dữ liệu bằng `TRUNCATE` thay vì tạo container mới; dùng wait strategy đúng; init schema bằng chính Flyway/Liquibase; `withReuse(true)` chỉ cho máy dev; registry mirror cho CI; WireMock cho API ngoài; Toxiproxy để test timeout/retry.

**Giải thích chi tiết:**
- `Startables.deepStart(...)` start song song nhiều container.
- Wait strategy: `Wait.forLogMessage(...)`, `Wait.forHttp("/health")`, `Wait.forHealthcheck()`; module chuyên dụng đã có sẵn.
- CI Docker-in-Docker: cấu hình `TESTCONTAINERS_HOST_OVERRIDE`/socket; rate limit Docker Hub → `hub.image.name.prefix` trỏ registry nội bộ.
- Testcontainers 2.x đổi tên artifact (`testcontainers-postgresql`) — luôn dùng BOM.

**Câu hỏi nối tiếp:**
- *Test resilience với Redis chậm?* — `ToxiproxyContainer` chèn latency 500ms, assert fallback chạy trong ngưỡng và metric `cache.fallback` tăng, không dùng `Thread.sleep`.

**⚠️ Câu trả lời gây điểm trừ:** dùng `withReuse(true)` trên CI, hoặc `latest` tag rồi ngạc nhiên vì test đỏ khi image mới ra.

**📖 Ôn lại:** [7.5 Tối ưu tốc độ và độ ổn định](../01-giao-trinh/15-testing.md#p7)

</details>

---

<a id="g7"></a>
## 7. Contract testing

### Q35. 🟡 Contract testing giải quyết vấn đề gì mà unit test và E2E không giải quyết tốt? Mô tả luồng consumer-driven với Pact.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Consumer A mock provider B trong test; B không biết A dùng field nào. B đổi `totalAmount` → `total`: cả hai bộ test xanh, production vỡ. E2E bắt được nhưng chậm, flaky, cần môi trường đầy đủ. Contract test kiểm tra riêng **biên** giữa hai bên, mỗi bên chạy độc lập. Pact: consumer viết test với mock server → sinh file pact → publish lên Broker → provider verify bằng cách replay request vào provider thật (với `@State`) → `can-i-deploy` gate deploy.

**Giải thích chi tiết:**
- Contract tốt: dùng **matcher theo kiểu** (`integerType`, `stringMatcher`) thay giá trị cứng; chỉ khai báo field consumer **thực sự dùng** → provider thêm field mới không vỡ.
- Version pact = git SHA + branch; sau deploy gọi `record-deployment` để Broker biết version nào đang ở prod.
- Webhook "contract_requiring_verification_published" để trigger verify ở pipeline provider khi có pact mới.
- Giá trị lớn nhất: bỏ phần lớn E2E xuyên service và **deploy độc lập**.

**Câu hỏi nối tiếp:**
- *Contract test có thay functional test của provider?* — Không: nó chỉ kiểm tra hình dạng và vài trạng thái.

**⚠️ Câu trả lời gây điểm trừ:** nhầm contract test với "test API bằng Postman/OpenAPI schema" mà không có vòng consumer → provider verification.

**📖 Ôn lại:** [8.1–8.2 Vấn đề & Pact](../01-giao-trinh/15-testing.md#p8)

</details>

### Q36. 🔴 So sánh Pact và Spring Cloud Contract. Với API public có nhiều consumer không xác định, bạn làm gì? Những lỗi nào làm contract testing mất giá trị?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Pact: consumer viết contract bằng code, đa ngôn ngữ tốt, hạ tầng Pact Broker + `can-i-deploy`. Spring Cloud Contract: contract (Groovy/YAML/Kotlin DSL) thường nằm **ở repo provider**, plugin sinh test cho provider và WireMock stub `*-stubs.jar` cho consumer (`@AutoConfigureStubRunner`), chủ yếu JVM. API public nhiều consumer không biết trước → consumer-driven không khả thi; dùng **OpenAPI** làm contract + kiểm tra breaking change (openapi-diff) trong CI + versioning.

**Giải thích chi tiết:**

| | Pact | Spring Cloud Contract |
|---|---|---|
| Ai viết | Consumer | Provider (consumer gửi PR) |
| Đa ngôn ngữ | Rất tốt | Chủ yếu JVM |
| Hạ tầng | Pact Broker/PactFlow | Artifact repository |
| Messaging | Message pacts | Spring Cloud Stream, Kafka |

- Lỗi làm mất giá trị: contract quá chặt (giá trị cụ thể, thứ tự field) → vỡ vì lý do không quan trọng; provider không verify trong pipeline; không tag version/environment nên `can-i-deploy` vô nghĩa; contract mô tả field consumer không dùng.

**Câu hỏi nối tiếp:**
- *Event Kafka có cần contract?* — Có: message pact hoặc schema registry (Avro/Protobuf với compatibility mode BACKWARD/FORWARD).

**⚠️ Câu trả lời gây điểm trừ:** "Có E2E rồi, không cần contract test" với hệ thống hàng chục microservice.

**📖 Ôn lại:** [8.3 Spring Cloud Contract](../01-giao-trinh/15-testing.md#p8)

</details>

---

<a id="g8"></a>
## 8. Test code bất đồng bộ & đa luồng

### Q37. 🟢 Vì sao `Thread.sleep()` trong test gần như luôn sai? Thay bằng gì?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `sleep` hoặc **quá ngắn** (flaky trên CI chậm) hoặc **quá dài** (suite chậm, cộng dồn). Thay theo thứ tự ưu tiên: tách logic khỏi threading (inject `Executor`, dùng direct executor `Runnable::run` trong test); điều khiển thời gian (`Clock`, virtual time); chờ có điều kiện bằng **Awaitility**; đồng bộ tường minh bằng `CountDownLatch`.

**Giải thích chi tiết:**

```java
await().atMost(Duration.ofSeconds(5))
       .pollInterval(Duration.ofMillis(100))
       .untilAsserted(() -> assertThat(repo.findStatus("o-1")).isEqualTo(PAID));

assertThat(service.computeAsync(input)).succeedsWithin(Duration.ofSeconds(2))
    .extracting(Result::value).isEqualTo(42);

verify(mailer, timeout(1000)).send(any());   // Mockito chờ tối đa 1s
```

- Khẳng định điều gì **không** xảy ra: `await().during(2s).atMost(3s).until(() -> mailer.sent().isEmpty())`.
- `atMost` quá lớn (60s) che giấu regression hiệu năng.

**Câu hỏi nối tiếp:**
- *Rule code review?* — Cấm `Thread.sleep` trong test trừ khi có comment giải thích.

**⚠️ Câu trả lời gây điểm trừ:** "Tăng sleep lên 5 giây cho chắc".

**📖 Ôn lại:** [9.1–9.2 Chiến lược test async](../01-giao-trinh/15-testing.md#p9)

</details>

### Q38. 🔴 Viết test chứng minh một class có race condition thế nào? Test đa luồng xanh có chứng minh class thread-safe không?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Tạo nhiều thread, dùng `CountDownLatch` để chúng **xuất phát cùng lúc** (tăng va chạm), mỗi thread thao tác nhiều lần, thu `Future` và `get()` để propagate exception, rồi assert bất biến (tổng = threads × perThread). Lặp lại bằng `@RepeatedTest`. Test **đỏ chứng minh có bug**; test **xanh không chứng minh thread-safe** — chỉ là chưa tìm thấy interleaving lỗi. Muốn chặt hơn dùng jcstress (memory model) hoặc Lincheck.

**Giải thích chi tiết:**

```java
@RepeatedTest(20)
void incrementIsThreadSafe() throws Exception {
    var counter = new Counter();
    int threads = 16, perThread = 10_000;
    var start = new CountDownLatch(1);
    var pool = Executors.newFixedThreadPool(threads);
    try {
        List<Future<?>> fs = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            fs.add(pool.submit(() -> { start.await(); for (int i = 0; i < perThread; i++) counter.increment(); return null; }));
        }
        start.countDown();
        for (var f : fs) f.get(10, TimeUnit.SECONDS);
    } finally { pool.shutdownNow(); }
    assertThat(counter.value()).isEqualTo((long) threads * perThread);
}
```

- Với DB: hai thread rút tiền cùng tài khoản (PostgreSQL Testcontainers), `CyclicBarrier(2)` để cả hai đọc số dư trước khi ghi; assert đúng một giao dịch thành công, số dư không âm (optimistic lock hoặc `UPDATE ... WHERE balance >= ?`).
- Bẫy: exception trong worker bị nuốt → test xanh; quên shutdown executor → thread rò sang test sau.

**Câu hỏi nối tiếp:**
- *Test flaky do race thì sửa test hay code?* — Nếu race nằm trong code production, đó là bug production — sửa code.

**⚠️ Câu trả lời gây điểm trừ:** "Chạy 1 lần với 2 thread, xanh là thread-safe".

**📖 Ôn lại:** [9.3 Chứng minh race condition bằng test](../01-giao-trinh/15-testing.md#p9)

</details>

### Q39. 🟡 Test `@Async`, `@Scheduled` và `@TransactionalEventListener` trong Spring thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `@Async`: trong `@SpringBootTest` chạy trên executor thật → chờ bằng Awaitility, hoặc thay `TaskExecutor` bằng `SyncTaskExecutor` trong profile test. `@Scheduled`: không chờ cron — gọi trực tiếp method job, test biểu thức lịch bằng `CronExpression.parse(...).next(...)`. `@TransactionalEventListener(AFTER_COMMIT)`: chỉ chạy sau commit → test với transaction thật đã commit, **không** `@Transactional` trên test.

**Giải thích chi tiết:**
- Thiết kế **observable completion**: trạng thái truy vấn được, metric, callback — để test có cái mà chờ.
- MDC/SecurityContext không tự đi theo sang thread `@Async` → cần `TaskDecorator`; nên có test cho việc này.
- Reactor: `StepVerifier.withVirtualTime(...)`; `Retry.backoff` có jitter mặc định → `.jitter(0)` để test xác định.

**Câu hỏi nối tiếp:**
- *Test retry với backoff 1s, 2s, 4s mà chạy < 100ms?* — Inject interface `Sleeper`; fake ghi lại các khoảng sleep và assert `[1s, 2s, 4s]`.

**⚠️ Câu trả lời gây điểm trừ:** chờ cron chạy thật trong test, hoặc ngạc nhiên vì listener AFTER_COMMIT không chạy trong test `@Transactional`.

**📖 Ôn lại:** [9.4 Spring `@Async`, `@Scheduled`, event](../01-giao-trinh/15-testing.md#p9)

</details>

---

<a id="g9"></a>
## 9. Coverage, mutation testing, ArchUnit

### Q40. 🟢 Line coverage và branch coverage khác nhau thế nào? 100% coverage có nghĩa là code không có bug?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Line coverage đo dòng nào đã chạy; branch coverage đo **mỗi nhánh** của `if/switch` đã được đi qua — có giá trị hơn. Coverage chỉ trả lời "dòng này **có chạy** không", không trả lời "test có **kiểm tra** kết quả không": 100% coverage + 0 assert = 0 bảo vệ. Coverage thấp là tín hiệu có vấn đề; coverage cao **không** là tín hiệu ổn.

**Giải thích chi tiết:**
- JaCoCo là Java agent instrument bytecode lúc nạp class, ghi probe vào `jacoco.exec`, goal `report` tạo HTML/XML.
- Đặt target cứng → Goodhart's law: người ta viết test vô nghĩa để đạt số.
- Gate hợp lý: coverage trên **code mới** ≥ 80% (SonarQube "new code") và không cho giảm tổng; dùng coverage để **tìm vùng chưa test** trong diff của PR.

**Câu hỏi nối tiếp:**
- *Vậy đo chất lượng assertion bằng gì?* — Mutation testing (Q41).

**⚠️ Câu trả lời gây điểm trừ:** "Team tôi bắt buộc 90% coverage nên chất lượng rất tốt".

**📖 Ôn lại:** [10.1–10.2 JaCoCo & giới hạn của coverage](../01-giao-trinh/15-testing.md#p10)

</details>

### Q41. 🔴 Mutation testing là gì? Đọc báo cáo PIT thế nào và áp dụng vào CI ra sao để không quá chậm?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** PIT tạo **mutant** — bản bytecode bị sửa nhỏ (đổi `>=` thành `>`, `+` thành `-`, bỏ lời gọi void, trả `null`/0/false) — rồi chạy test liên quan. Test đỏ → mutant **killed** (tốt); test xanh → **survived** (test yếu); không test nào chạy qua → no coverage. **Mutation score** = killed/tổng — đo chất lượng assertion mà coverage không đo được. Vì chậm (số mutant × thời gian test), giới hạn vào package domain, dùng incremental history và chỉ chạy trên code thay đổi của PR.

**Giải thích chi tiết:**

```java
boolean isEligibleForFreeShipping(BigDecimal total) {
    return total.compareTo(new BigDecimal("500000")) >= 0;   // PIT đổi >= thành >
}
// Test chỉ có case 600000 -> mutant ">" sống: thiếu test biên đúng bằng 500000.
```

- Mutant hay sống: boundary, "negate conditional" ở nhánh không assert, "void method call removed" (không ai assert `audit.log()`), "return value" khi test chỉ kiểm tra không ném exception.
- **Equivalent mutant**: không đổi hành vi quan sát được → không kill được, bỏ qua có lý do.
- CI: `withHistory` với history file được cache; map file thay đổi (`git diff --name-only origin/main...HEAD`) thành `targetClasses`; ngưỡng ví dụ ≥ 70% cho code mới; không chạy PIT cho integration test chậm.

**Câu hỏi nối tiếp:**
- *Mutation score 100% có cần không?* — Không; tập trung domain quan trọng, chấp nhận equivalent mutant.

**⚠️ Câu trả lời gây điểm trừ:** chạy PIT toàn bộ codebase (kể cả integration test) mỗi PR rồi kết luận "mutation testing quá chậm, vô dụng".

**📖 Ôn lại:** [10.3 Mutation testing với PIT](../01-giao-trinh/15-testing.md#p10)

</details>

### Q42. 🟡 Sau khi thêm cấu hình `<argLine>` cho Surefire, báo cáo JaCoCo về 0%. Vì sao?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Goal `jacoco:prepare-agent` hoạt động bằng cách đặt property `argLine` chứa `-javaagent:jacocoagent.jar`. Khai báo `<argLine>` cứng trong Surefire **ghi đè** property đó → test chạy không có agent → không có dữ liệu coverage. Sửa: dùng late property evaluation `<argLine>@{argLine} -Xmx1g ...</argLine>` để giữ giá trị JaCoCo đã đặt.

**Giải thích chi tiết:**
- Tương tự khi thêm `-javaagent` của Mockito (Q20) — luôn nối vào `@{argLine}`.
- Multi-module: `report-aggregate` trong module tổng hợp; integration test (Failsafe) dùng `prepare-agent-integration`/`report-integration` hoặc `merge` các file exec.
- Gate: execution `check` với rule `BRANCH COVEREDRATIO`, loại trừ config/DTO/`*Application`.

**Câu hỏi nối tiếp:**
- *Gradle?* — Plugin `jacoco` tự gắn agent vào task `test`; cẩn thận khi tự set `jvmArgs` ghi đè.

**⚠️ Câu trả lời gây điểm trừ:** "JaCoCo lỗi, bỏ đi" mà không hiểu cơ chế agent.

**📖 Ôn lại:** [10.1 JaCoCo hoạt động thế nào](../01-giao-trinh/15-testing.md#p10)

</details>

### Q43. 🟡 ArchUnit dùng để làm gì? Áp dụng vào codebase cũ đã có hàng trăm vi phạm thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** ArchUnit biến quy ước kiến trúc (controller không gọi repository, domain không phụ thuộc Spring/JPA, không vòng phụ thuộc giữa module, không field injection) thành **test JUnit** phân tích bytecode. Với codebase cũ, bọc rule bằng `FreezingArchRule.freeze(rule)`: vi phạm hiện tại lưu vào violation store (commit vào repo), chỉ **vi phạm mới** làm test đỏ; vi phạm cũ được sửa dần sẽ tự bị gỡ.

**Giải thích chi tiết:**

```java
@AnalyzeClasses(packages = "com.acme.shop", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {
    @ArchTest static final ArchRule domainIsFrameworkFree = noClasses().that().resideInAPackage("..domain..")
        .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "jakarta.persistence..");
    @ArchTest static final ArchRule noCycles = slices().matching("com.acme.shop.(*)..").should().beFreeOfCycles();
}
```

- Là công cụ governance rẻ nhất cho tech lead: rule được review trong PR như code, chạy vài giây.
- Kết hợp Spring Modulith (`ApplicationModules.of(App.class).verify()`) cho modular monolith.
- Quên `DoNotIncludeTests` → class test vi phạm rule; ArchUnit 1.x fail khi rule không khớp class nào (phát hiện rule đặt sai package).

**Câu hỏi nối tiếp:**
- *Viết rule cho mọi thứ?* — Không; ưu tiên ranh giới quan trọng: domain độc lập, không cycle, không truy cập internal của module khác.

**⚠️ Câu trả lời gây điểm trừ:** "Quy ước kiến trúc để trong wiki/Confluence là đủ".

**📖 Ôn lại:** [11. Architecture tests với ArchUnit](../01-giao-trinh/15-testing.md#p11)

</details>

---

<a id="g10"></a>
## 10. Performance test, flaky test, static analysis & review

### Q44. 🟡 Phân biệt load, stress, spike, soak test. Khi đọc kết quả bạn nhìn chỉ số nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Load: xác nhận đạt SLO ở tải kỳ vọng. Stress: vượt mức để tìm điểm gãy và xem hệ thống hỏng thế nào. Spike: đột biến (flash sale). Soak: tải vừa nhiều giờ để lộ rò bộ nhớ/connection. Chỉ số: throughput, **latency theo percentile** (p50/p95/p99/p99.9 — không bao giờ chỉ average), error rate, và phía server: CPU, GC pause, thread/connection pool, slow query.

**Giải thích chi tiết:**
- **Little's Law:** `L = λ × W` — 200 req/s × 0.5s = 100 request đồng thời → cần ≥ 100 thread/connection nếu blocking.
- Fan-out: 1 request gọi 10 service → p99 của từng service thành trải nghiệm thường gặp của user.
- Quy trình: mục tiêu từ SLO → môi trường giống prod (dữ liệu đủ lớn) → warm-up JVM → load generator không là nút thắt → quan sát server → đổi **một biến** mỗi lần → smoke perf test trong CI nightly.
- Công cụ: JMeter (GUI, `.jmx` khó review), Gatling (code, engine async), k6 (JS, thresholds làm gate CI).

**Câu hỏi nối tiếp:**
- *Hikari pool 10, mỗi request giữ connection 200ms — throughput tối đa?* — ~50 req/s (10/0,2s); vượt mức đó request xếp hàng chờ connection. Sửa: đưa lời gọi chậm ra ngoài transaction.

**⚠️ Câu trả lời gây điểm trừ:** chỉ báo cáo average latency; test từ laptop qua VPN với 1000 dòng dữ liệu.

**📖 Ôn lại:** [12. Performance / load testing](../01-giao-trinh/15-testing.md#p12)

</details>

### Q45. 🔴 "Coordinated omission" là gì? Open và closed workload model khác nhau thế nào và vì sao quan trọng?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Closed model: N user ảo, mỗi user chờ response rồi mới gửi tiếp (JMeter thread group mặc định) → khi server chậm, tải **tự giảm**, và công cụ không gửi request trong lúc chờ → bỏ sót đúng những mẫu tệ nhất → p99 đẹp giả tạo. Đó là **coordinated omission** (Gil Tene). Open model: tốc độ đến cố định như traffic Internet thật (Gatling `constantUsersPerSec`, k6 `constant-arrival-rate`) → ghi nhận đúng độ trễ khi hệ thống bị nghẽn.

**Giải thích chi tiết:**
- Ví dụ: endpoint thỉnh thoảng dừng 2s (GC pause). Closed 10 thread: thread bị kẹt chỉ ghi **1** mẫu xấu. Thực tế trong 2s đó có `2s × rate` request đến và đều chậm → open model ghi nhận tất cả → p99 cao hơn nhiều, đúng thực tế hơn.
- Công cụ hiệu chỉnh: wrk2, HdrHistogram.
- k6: `executor: 'constant-arrival-rate'`, `preAllocatedVUs`, `maxVUs` đủ lớn; nếu hết VU, k6 báo dropped iterations — đó cũng là tín hiệu.

**Câu hỏi nối tiếp:**
- *Khi nào closed model hợp lý?* — Hệ thống có số client cố định thật sự (batch worker, kết nối nội bộ có pool cố định).

**⚠️ Câu trả lời gây điểm trừ:** "Tăng số thread JMeter là đủ mô phỏng tải cao".

**📖 Ôn lại:** [12.2 Khái niệm cần nắm](../01-giao-trinh/15-testing.md#p12)

</details>

### Q46. 🟡 Flaky test thường do đâu? Quy trình xử lý ở cấp team?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Nguyên nhân gốc: thời gian (`now()` gần nửa đêm, timezone CI), async/sleep, state dùng chung/thứ tự test, thứ tự không xác định (`HashSet`, `SELECT` không `ORDER BY`), tài nguyên ngoài, port cố định, race condition thật trong code, timeout quá chặt, random không seed, rò rỉ giữa test (static mock, `SecurityContextHolder`), floating point. Quy trình: **phát hiện** (CI ghi test pass sau retry) → **cách ly** (tag quarantine, có ticket/owner/hạn) → **tái hiện** (`@RepeatedTest`, random order có seed, giới hạn CPU, đổi TZ) → **sửa gốc** → theo dõi flaky rate.

**Giải thích chi tiết:**
- Surefire `rerunFailingTestsCount` ghi `<flakyFailure>` trong báo cáo; Gradle test-retry plugin; Develocity flaky detection.
- Random order: `junit.jupiter.testmethod.order.default=...MethodOrderer$Random`, cố định bằng `junit.jupiter.execution.order.random.seed`.
- Retry tự động là thuốc giảm đau: nếu bật thì **bắt buộc báo cáo**; luôn hỏi "test flaky vì **code** flaky không?".

**Câu hỏi nối tiếp:**
- *Ai sửa test quarantine?* — Owner của khu vực code; có SLA (ví dụ 7 ngày), quá hạn thì xóa test hoặc escalate.

**⚠️ Câu trả lời gây điểm trừ:** "Re-run pipeline đến khi xanh" là quy trình chuẩn của team.

**📖 Ôn lại:** [13. Flaky tests](../01-giao-trinh/15-testing.md#p13)

</details>

### Q47. 🟢 Kể tên các công cụ static analysis cho Java và vai trò của từng cái. Quality Gate là gì?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Checkstyle (style, naming, format — nên kết hợp Spotless/google-java-format), PMD (code smell trên source, CPD copy-paste), SpotBugs (bug pattern trên **bytecode**; plugin FindSecBugs cho bảo mật), Error Prone (lỗi ở compile time, plugin javac), NullAway (null-safety theo JSpecify), SonarQube (tổng hợp bugs, vulnerabilities, hotspots, smells, duplication, coverage). **Quality Gate** là tập điều kiện pipeline phải đạt, nên áp cho **code mới**.

**Giải thích chi tiết:**
- Gate gợi ý cho new code: 0 bug/vulnerability Blocker/Critical mới, review 100% security hotspots, coverage ≥ 80%, duplication ≤ 3%; `-Dsonar.qualitygate.wait=true` để pipeline fail.
- Chiến lược trên codebase cũ: chỉ gate code mới, ruleset nhỏ chất lượng cao, mỗi suppress có lý do (`@SuppressFBWarnings(justification=...)`), tự động format để review tập trung vào logic.
- SpotBugs bắt `ES_COMPARING_STRINGS_WITH_EQ`; FindSecBugs bắt `SQL_INJECTION_JDBC`.

**Câu hỏi nối tiếp:**
- *Bật mọi rule trên codebase cũ?* — 10.000 cảnh báo → mọi người lờ đi; tín hiệu/nhiễu thấp thì công cụ vô dụng.

**⚠️ Câu trả lời gây điểm trừ:** "Có Sonar rồi nên không cần review" — công cụ giải phóng reviewer, không thay thế.

**📖 Ôn lại:** [14.1 Công cụ và vai trò](../01-giao-trinh/15-testing.md#p14)

</details>

### Q48. 🟡 Khi review một PR, bạn soi những gì? Văn hóa review bạn muốn xây dựng?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Theo thứ tự ưu tiên: đúng vấn đề và thiết kế (SRP, ranh giới, xử lý lỗi, backward compatibility của API/schema/event), concurrency & tài nguyên (shared state, timeout, transaction boundary, không gọi HTTP trong transaction), hiệu năng (N+1, phân trang), bảo mật (validation, query tham số hóa, object-level authorization, không log secret/PII), **test** (có test cho hành vi mới/bug fix, đúng tầng, không over-mock, có thể fail), vận hành (log/metric/feature flag). Style để máy làm.

**Giải thích chi tiết:**
- Văn hóa: PR nhỏ (< 400 dòng), review trong ngày, comment về code không về người, phân loại **blocking / nit / praise**, tác giả tự review trước, tranh luận dài thì gọi 10 phút.
- Đọc test trước: test mô tả hành vi gì? Có assertion không? Nếu sửa code bừa test có đỏ không?
- Reviewer chịu trách nhiệm chung về những gì được merge.

**Câu hỏi nối tiếp:**
- *PR 2000 dòng?* — Yêu cầu tách (refactor riêng, feature riêng), hoặc review theo commit có cấu trúc; không "LGTM sau 5 phút".

**⚠️ Câu trả lời gây điểm trừ:** review chỉ soi style/đặt tên; không chạy/đọc test.

**📖 Ôn lại:** [14.2 Code review checklist (Senior)](../01-giao-trinh/15-testing.md#p14)

</details>

---

<a id="g11"></a>
## 11. Tình huống thực tế

### Q49. 🔴 🎬 Pipeline CI của team mất 40 phút, hay đỏ ngẫu nhiên; một số dev đề xuất bỏ bớt integration test. Bạn là Senior mới vào team — bạn làm gì?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Không bỏ test theo cảm tính — **đo trước**: thời gian từng stage/module, số Spring context được tạo, test chậm nhất, tỷ lệ flaky. Sau đó xử lý theo đòn bẩy lớn nhất: gom context (base class, fake bean, bỏ `@DirtiesContext`), singleton Testcontainers, chuyển test sai tầng xuống slice/unit, quarantine flaky có owner, song song hóa stage/fork hợp lý, cache dependency. Đặt mục tiêu (PR pipeline < 10 phút) và theo dõi như metric.

**Giải thích chi tiết:**
- Tuần 1: thu thập dữ liệu (Develocity/build scan, log `test.context.cache=DEBUG`, báo cáo Surefire). Thường thấy: 20–30 context do `@MockitoBean` rải rác, container non-static, `Thread.sleep` cộng dồn, E2E chạy trên mỗi PR.
- Quick wins: base class `AbstractIT` + `@ServiceConnection`, xóa `@DirtiesContext`, thay sleep bằng Awaitility.
- Cấu trúc: tách job unit (< 1–2 phút) chạy trước, integration song song, E2E/perf chạy sau merge hoặc nightly; PIT chỉ trên code thay đổi.
- Flaky: quarantine + ticket + SLA; không retry mù.
- Truyền thông: trình bày số liệu trước/sau, đề xuất theo từng PR nhỏ để team thấy hiệu quả.

**Câu hỏi nối tiếp:**
- *Nếu team vẫn muốn xóa test?* — Chỉ xóa test **trùng lặp hoặc sai tầng** (đã có slice test bao phủ) và test giòn không bảo vệ gì; giữ test bảo vệ hành vi quan trọng.

**⚠️ Câu trả lời gây điểm trừ:** "Đồng ý bỏ integration test, chỉ giữ unit test cho nhanh" hoặc "mua runner mạnh hơn" mà không phân tích.

**📖 Ôn lại:** [6.4 Context caching](../01-giao-trinh/15-testing.md#p6) · [7.5 Tối ưu Testcontainers](../01-giao-trinh/15-testing.md#p7) · [13.3 Quy trình flaky](../01-giao-trinh/15-testing.md#p13)

</details>

### Q50. 🔴 🎬 Toàn bộ test xanh, coverage 85%, nhưng sau deploy production lỗi `LazyInitializationException` ở API chi tiết đơn hàng và một lỗi duplicate key khi tạo đơn. Phân tích vì sao test không bắt được và bạn thay đổi chiến lược test thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Hai lỗi đều thuộc loại "chỉ lộ ra với hạ tầng và transaction thật": test chạy trong `@Transactional` (session mở suốt request → lazy load chạy được; không flush/commit → unique constraint không bị kiểm tra), có thể còn dùng H2 hoặc mock repository. Coverage cao vì code đã **chạy**, nhưng không có test nào **quan sát** đúng điều kiện production. Sửa: integration test không transaction (`RANDOM_PORT` hoặc commit thật) với PostgreSQL Testcontainers, `@DataJpaTest` có `flush/clear`, test JSON/DTO, và mutation testing cho domain.

**Giải thích chi tiết:**
- Tái hiện bug bằng test đỏ trước khi sửa (TDD cho bug fix):
  - Lazy: `@SpringBootTest(RANDOM_PORT)` gọi `GET /api/orders/{id}` → đỏ; sửa bằng DTO + `JOIN FETCH`/`@EntityGraph`, assert số câu SQL.
  - Duplicate key: `saveAndFlush` hai lần cùng `externalId` trên Postgres thật → `DataIntegrityViolationException`; xử lý idempotency (unique constraint + bắt lỗi trả 409, hoặc `ON CONFLICT DO NOTHING`).
- Chiến lược mới: thêm `open-in-view=false` ở mọi môi trường (kể cả test), cấm H2, base class integration test commit thật, rule review "test có `@Transactional` phải có lý do".
- Bài học trình bày trong postmortem: "coverage không phải là thước đo chất lượng".

**Câu hỏi nối tiếp:**
- *Làm sao phát hiện sớm loại lỗi này?* — Smoke test sau deploy staging bằng dữ liệu thật hơn; contract/E2E cho luồng sống còn; canary.

**⚠️ Câu trả lời gây điểm trừ:** "Tăng coverage lên 95%" hoặc đổ lỗi cho QA.

**📖 Ôn lại:** [6.5 Transaction pitfall](../01-giao-trinh/15-testing.md#p6) · [6.6 `@DataJpaTest`](../01-giao-trinh/15-testing.md#p6) · [10.2 Giới hạn của coverage](../01-giao-trinh/15-testing.md#p10)

</details>

### Q51. 🟡 🎬 Thiết kế chiến lược test cho service Payment: REST API, PostgreSQL, publish event Kafka, gọi cổng thanh toán bên ngoài (VNPay/MoMo...).

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Hình dạng honeycomb. Unit (không Spring, không mock) cho `Money`, state machine `PaymentStatus`, tính phí. Slice: `@WebMvcTest` cho validation/mã lỗi/phân quyền, `@JsonTest` cho payload. Integration: `@SpringBootTest` + PostgreSQL & Kafka Testcontainers + **WireMock** cho cổng thanh toán (trả 200, 4xx, 5xx, timeout, chữ ký sai). Contract: Pact với Order service (consumer) và event schema. E2E: 1 luồng trên staging dùng sandbox của cổng. Mock **duy nhất** ở biên unmanaged (gateway).

**Giải thích chi tiết:**
- Rủi ro đặc thù payment cần test: **idempotency** (callback/IPN gửi lặp → không ghi nhận hai lần), xác thực chữ ký callback, timeout + retry không trừ tiền hai lần, đối soát trạng thái `PENDING` kẹt, concurrency (hai callback đồng thời → optimistic lock).
- Mục tiêu thời gian: unit < 1 phút, integration < 5 phút; một Spring context duy nhất cho suite.
- Chất lượng: PIT ≥ 80% cho domain; ArchUnit: domain không phụ thuộc Spring; SpotBugs + FindSecBugs.
- Non-functional: k6 smoke 2–5 phút với thresholds trong nightly; Toxiproxy để test gateway chậm.

**Câu hỏi nối tiếp:**
- *Test callback từ cổng thanh toán?* — Test chữ ký hợp lệ/không hợp lệ, callback trùng lặp, callback đến trước khi response đồng bộ trả về.

**⚠️ Câu trả lời gây điểm trừ:** gọi sandbox cổng thanh toán thật trong mọi build, hoặc chỉ có unit test mock toàn bộ.

**📖 Ôn lại:** [1.3–1.4 Chiến lược](../01-giao-trinh/15-testing.md#p1) · [Dự án mini](../01-giao-trinh/15-testing.md#du-an-mini)

</details>

### Q52. 🔴 🎬 Một integration test Kafka đỏ khoảng 1/50 lần trên CI, máy local không bao giờ đỏ. Bạn điều tra thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Coi đó là tín hiệu có thể của **bug thật** chứ không chỉ "test xấu". Thu thập: log/stack trace của các lần đỏ, thời điểm, test chạy trước nó. Tái hiện bằng điều kiện CI: `@RepeatedTest(200)`, giới hạn CPU (`docker run --cpus=0.5`), random order có seed, chạy song song. Kiểm tra các nguyên nhân Kafka điển hình: chờ bằng `sleep`, consumer chưa được assign partition khi producer gửi (`auto.offset.reset=latest`), dữ liệu/offset rò từ test trước (topic/group id dùng chung), thứ tự message giữa các partition, timeout Awaitility quá chặt.

**Giải thích chi tiết:**
- Sửa theo nguyên nhân:
  - Consumer test: `auto.offset.reset=earliest`, group id ngẫu nhiên mỗi test, chờ assignment (`ContainerTestUtils.waitForAssignment`) trước khi gửi.
  - Topic/key riêng cho mỗi test hoặc dữ liệu có id duy nhất; assert trên message của chính test.
  - Thứ tự: chỉ đảm bảo trong một partition → cùng key nếu cần thứ tự; assert không phụ thuộc thứ tự nếu nghiệp vụ không yêu cầu.
  - Thay `sleep` bằng Awaitility với `atMost` hợp lý.
- Nếu phát hiện consumer xử lý trùng/mất message → đó là bug idempotency/commit offset của production, sửa code.
- Trong lúc điều tra: quarantine có ticket, không retry mù.

**Câu hỏi nối tiếp:**
- *Vì sao CI hay lộ flaky hơn local?* — CPU ít hơn, nhiều job song song, Docker chậm hơn, timezone/locale khác → interleaving khác.

**⚠️ Câu trả lời gây điểm trừ:** "Thêm `Thread.sleep(5000)`" hoặc "bật retry 3 lần là xong".

**📖 Ôn lại:** [7.4 Test Kafka với Awaitility](../01-giao-trinh/15-testing.md#p7) · [13. Flaky tests](../01-giao-trinh/15-testing.md#p13)

</details>

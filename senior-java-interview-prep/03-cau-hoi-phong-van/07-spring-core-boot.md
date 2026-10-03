# Câu hỏi phỏng vấn — Module 07: Spring Core & Spring Boot

> Giáo trình tương ứng: [Module 07 — Spring Core & Spring Boot](../01-giao-trinh/07-spring-core-boot.md)

**Cách dùng:** đọc câu hỏi, **tự trả lời thành tiếng** trong 1–2 phút *rồi* mới mở "Đáp án". Phần **Trả lời ngắn** là những ý phải nói được trong 30 giây đầu; **Giải thích chi tiết** là phần đào sâu khi interviewer hỏi tiếp; hãy tự trả lời các **Câu hỏi nối tiếp** trước khi đọc gợi ý. Tránh những câu trong mục **⚠️ Câu trả lời gây điểm trừ**.

**Mức độ:** 🟢 Cơ bản · 🟡 Senior · 🔴 Xoáy sâu. **[Tình huống]** = câu "bạn sẽ làm gì"; **[Đọc code]** = câu đọc code tìm lỗi / dự đoán kết quả. Mặc định Spring Boot 3.x / Spring Framework 6.x; khác biệt với Boot 2 được ghi chú.

Tổng: **57 câu** — 13 🟢 · 28 🟡 · 16 🔴.

## Mục lục

| Nhóm | Chủ đề | Câu hỏi |
|---|---|---|
| [A](#nhom-a) | IoC & Dependency Injection | Q1–Q4 |
| [B](#nhom-b) | ApplicationContext, BeanDefinition, component scan | Q5–Q8 |
| [C](#nhom-c) | `@Configuration`, `@Qualifier`/`@Primary`, conditional bean | Q9–Q12 |
| [D](#nhom-d) | Bean scopes | Q13–Q16 |
| [E](#nhom-e) | Bean lifecycle, BeanPostProcessor | Q17–Q21 |
| [F](#nhom-f) | Circular dependency & 3-level cache | Q22–Q25 |
| [G](#nhom-g) | Spring AOP, `@Transactional` và các annotation dựa trên proxy | Q26–Q32 |
| [H](#nhom-h) | SpEL & Application Events | Q33–Q35 |
| [I](#nhom-i) | Auto-configuration & starters | Q36–Q40 |
| [J](#nhom-j) | Externalized configuration & profiles | Q41–Q44 |
| [K](#nhom-k) | `SpringApplication.run` & custom starter | Q45–Q46 |
| [L](#nhom-l) | Actuator, metrics, logging | Q47–Q50 |
| [M](#nhom-m) | `@Async`, `@Scheduled`, thread pool | Q51–Q53 |
| [N](#nhom-n) | Graceful shutdown, Boot 3, native image, virtual threads | Q54–Q57 |

---

<a id="nhom-a"></a>
## A. IoC & Dependency Injection

### Q1. 🟢 IoC và DI là gì? Spring container mang lại lợi ích gì so với tự `new`?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **IoC** (Inversion of Control) là đảo quyền điều khiển: object không tự tạo dependency mà một bên thứ ba (container) tạo và đưa vào. **DI** là một cách hiện thực IoC — dependency được tiêm qua constructor/setter/field (cách khác là Service Locator). Lợi ích: **loose coupling** (phụ thuộc abstraction), **testability** (truyền mock qua constructor, không cần container), **quản lý vòng đời tập trung** — và quan trọng nhất trong Spring: container bọc bean bằng **proxy** để thêm transaction, cache, security, async một cách trong suốt.

**Giải thích chi tiết:**
- "Tách construction khỏi use" (Clean Code, chương Systems): `new StripeGateway("sk_live_...")` trong service gắn cứng implementation, secret và vòng đời.
- Từ Spring 4.3, class có **một** constructor không cần `@Autowired`.
- Container còn cung cấp: externalized config, event, i18n, resource loading, lifecycle callback.

**Câu hỏi nối tiếp:**
- *IoC có cần container không?* → Không; tự wiring ở `main` (composition root) vẫn là DI. Container giúp khi số bean lớn và cần AOP/lifecycle.
- *Nhược điểm của container?* → "Magic", lỗi cấu hình lộ lúc runtime/startup, startup chậm hơn, khó debug nếu không hiểu proxy.

**⚠️ Câu trả lời gây điểm trừ:** "DI là dùng `@Autowired`"; không nhắc được vai trò proxy của container.

**📖 Ôn lại:** [§1 IoC & DI](../01-giao-trinh/07-spring-core-boot.md#p1)

</details>

### Q2. 🟢 Constructor, setter và field injection — vì sao Spring khuyến nghị constructor injection?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Constructor injection cho phép field `final` (immutable, publish an toàn giữa thread), dependency bắt buộc tường minh, unit test bằng `new Service(mock)` không cần Spring, phát hiện circular dependency ngay lúc startup, và constructor 8–10 tham số là **tín hiệu vi phạm SRP** dễ thấy. Setter dùng cho dependency **tùy chọn**. Field injection: không `final`, ẩn dependency, test phải dùng reflection/Spring, che giấu vòng tròn và class phình to.

**Giải thích chi tiết:**
- Tài liệu Spring: *"use constructors for mandatory dependencies and setter methods or configuration methods for optional dependencies"*.
- Dependency tùy chọn ngay trong constructor: `ObjectProvider<T>` (`getIfAvailable()`, `ifAvailable(...)`) hoặc `Optional<T>`.

```java
@Service
class ReportService {
    private final ReportRepository repo;
    private final ObjectProvider<MetricsPublisher> metrics;   // có thể không có bean
    ReportService(ReportRepository repo, ObjectProvider<MetricsPublisher> metrics) {
        this.repo = repo; this.metrics = metrics;
    }
    void run() { metrics.ifAvailable(m -> m.publish("report.run")); }
}
```

**Câu hỏi nối tiếp:**
- *Constructor quá nhiều tham số thì sao?* → Tách class theo trách nhiệm, không quay về field injection.

**⚠️ Câu trả lời gây điểm trừ:** "Field injection ngắn gọn nên em hay dùng"; không biết lợi ích `final`/test.

**📖 Ôn lại:** [§1.2 Ba kiểu injection](../01-giao-trinh/07-spring-core-boot.md#p1)

</details>

### Q3. 🟡 Có 3 bean cùng implement `Notifier`. Spring chọn bean nào để inject vào tham số `Notifier notifier`? Điều gì thay đổi từ Spring 6.1?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `DefaultListableBeanFactory.resolveDependency` tìm mọi candidate theo kiểu (kể cả generic), rồi lọc theo thứ tự: **`@Qualifier`** → **`@Primary`** → **`@Priority`** → khớp **tên tham số/field với tên bean** (fallback). Không chọn được → `NoUniqueBeanDefinitionException`. Spring 6.1 bỏ `LocalVariableTableParameterNameDiscoverer`: khớp theo tên tham số chỉ còn hoạt động khi compile với **`-parameters`** — project tự cấu hình compiler có thể "đột nhiên" lỗi sau nâng cấp.

**Giải thích chi tiết:**
- `@Qualifier` thắng `@Primary`. Spring 6.2 thêm `@Fallback` (ngược với `@Primary`: chỉ chọn khi không còn candidate khác).
- Inject tất cả: `List<Notifier>` (sắp theo `@Order`/`Ordered`), `Map<String, Notifier>` (key = bean name).
- Qualifier type-safe: tự tạo annotation `@Qualifier @Retention(RUNTIME) @interface Urgent {}`.
- Generic là một phần của kiểu: `Repository<User>` không khớp `Repository<Order>`.

**Câu hỏi nối tiếp:**
- *Spring Boot parent POM có bật `-parameters` không?* → Có (Maven plugin/Gradle plugin của Boot), nhưng library build riêng thì chưa chắc.

**⚠️ Câu trả lời gây điểm trừ:** "Spring lấy bean đầu tiên tìm được"; dựa vào tên biến mà không biết rủi ro.

**📖 Ôn lại:** [§1.3 Container resolve dependency thế nào](../01-giao-trinh/07-spring-core-boot.md#p1)

</details>

### Q4. 🟡 [Đọc code] Hai đoạn sau lỗi gì?

```java
@Service
@RequiredArgsConstructor
class InvoiceService {
    private InvoiceRepository repo;        // (1)
    private final MailSender mail;
}

@Service
class PaymentService {
    @Autowired private GatewayClient client;
    public void pay() { client.charge(); }
}
// test:  new PaymentService().pay();       // (2)
```

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** (1) Lombok `@RequiredArgsConstructor` chỉ đưa vào constructor các field **`final`** (và `@NonNull`) → `repo` không có trong constructor, **không bao giờ được inject**, NPE lúc dùng. (2) Field injection: tạo bằng `new` ngoài Spring thì không ai inject `client` → `NullPointerException`; phải dựng Spring context hoặc dùng reflection (`ReflectionTestUtils`) — dấu hiệu thiết kế khó test.

**Giải thích chi tiết:**
- Sửa: đánh `final` cho mọi dependency bắt buộc; chuyển `PaymentService` sang constructor injection để test `new PaymentService(mockClient)`.
- Tương tự: object tạo bằng `new` (không phải bean) cũng không có proxy → `@Transactional`, `@Async` không chạy.

**Câu hỏi nối tiếp:**
- *Muốn inject vào object không phải bean (ví dụ entity)?* → `@Configurable` + AspectJ weaving — hiếm khi đáng; thường là tín hiệu nên thiết kế lại (truyền dependency qua tham số method/domain service).

**⚠️ Câu trả lời gây điểm trừ:** Không phát hiện lỗi (1).

**📖 Ôn lại:** [§1 Lỗi thường gặp](../01-giao-trinh/07-spring-core-boot.md#p1)

</details>

---

<a id="nhom-b"></a>
## B. ApplicationContext, BeanDefinition, component scan

### Q5. 🟢 `BeanFactory` khác `ApplicationContext` thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `BeanFactory` là interface gốc, chỉ lo tạo/lấy bean, mặc định **lazy**. `ApplicationContext` extends `BeanFactory` và thêm: **tự phát hiện và đăng ký `BeanPostProcessor`/`BeanFactoryPostProcessor`** (thiếu bước này thì `@Autowired`, `@PostConstruct`, `@Transactional` không chạy), **pre-instantiate** mọi singleton non-lazy lúc `refresh()` (lỗi cấu hình lộ ra lúc startup), event publisher, `MessageSource`, `ResourceLoader`, `Environment`.

**Giải thích chi tiết:**
- Implementation: `DefaultListableBeanFactory` (lõi), `AnnotationConfigApplicationContext`, `AnnotationConfigServletWebServerApplicationContext` (Boot MVC), bản reactive cho WebFlux.
- Thực nghiệm: nạp class có `@Autowired` vào `DefaultListableBeanFactory` trần → field null cho tới khi tự `addBeanPostProcessor(AutowiredAnnotationBeanPostProcessor)`.

**Câu hỏi nối tiếp:**
- *Khi nào dùng `BeanFactory` trần?* → Gần như không bao giờ trong ứng dụng; chỉ trong framework/embedded rất hạn chế tài nguyên.

**⚠️ Câu trả lời gây điểm trừ:** "ApplicationContext chỉ là BeanFactory có thêm i18n" — bỏ qua BPP và pre-instantiation.

**📖 Ôn lại:** [§2.1 BeanFactory vs ApplicationContext](../01-giao-trinh/07-spring-core-boot.md#p2)

</details>

### Q6. 🟡 `BeanDefinition` là gì? Container khởi tạo bean qua những pha nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `BeanDefinition` là "bản thiết kế" của bean: class, scope, lazy-init, depends-on, constructor args, property values, init/destroy method, primary, factory method (cho `@Bean`)... Hai pha tách biệt: **(1) đăng ký BeanDefinition** (từ component scan, `@Bean`, `@Import`, `ImportSelector`, `ImportBeanDefinitionRegistrar`, XML, programmatic) → **(2) khởi tạo bean**. Giữa hai pha, `BeanFactoryPostProcessor` có thể sửa definition (đổi scope, thay `${...}`, thêm/xóa definition).

**Giải thích chi tiết:**
- `ConfigurationClassPostProcessor` (một `BeanDefinitionRegistryPostProcessor`) xử lý `@Configuration`, `@ComponentScan`, `@Import`, `@Bean` — kể cả auto-configuration.
- `@EnableJpaRepositories`, `@EnableFeignClients` dùng `ImportBeanDefinitionRegistrar` để đăng ký bean động theo annotation:

```java
public class AuditRegistrar implements ImportBeanDefinitionRegistrar {
    public void registerBeanDefinitions(AnnotationMetadata meta, BeanDefinitionRegistry registry) {
        var attrs = meta.getAnnotationAttributes(EnableAuditing.class.getName());
        registry.registerBeanDefinition("auditService", BeanDefinitionBuilder
            .genericBeanDefinition(AuditService.class).addConstructorArgValue(attrs.get("prefix")).getBeanDefinition());
    }
}
```

**Câu hỏi nối tiếp:**
- *Vì sao không được `getBean()` trong `BeanFactoryPostProcessor`?* → Buộc khởi tạo bean sớm, trước khi BPP được đăng ký → bean "trần", mất autowiring/proxy.

**⚠️ Câu trả lời gây điểm trừ:** Không phân biệt metadata (definition) với instance.

**📖 Ôn lại:** [§2.2 BeanDefinition](../01-giao-trinh/07-spring-core-boot.md#p2)

</details>

### Q7. 🟢 [Tình huống] App báo `No qualifying bean of type 'com.acme.app.service.OrderService'` dù class có `@Service`. Bạn kiểm tra gì?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Kiểm tra **phạm vi component scan**: `@SpringBootApplication` = `@SpringBootConfiguration` + `@EnableAutoConfiguration` + `@ComponentScan` với base package = **package của class main**; class nằm ngoài (ví dụ main ở `com.acme.app.web` còn service ở `com.acme.app.service`) sẽ không được quét. Tiếp theo: bean có bị `@Profile`/`@Conditional` loại không, có bị exclude filter không, class có thật trong classpath của module đang chạy không.

**Giải thích chi tiết:**
- Sửa đúng: đặt class main ở package gốc (`com.acme.app`); hoặc `scanBasePackages` — nhưng đừng scan quá rộng (kéo bean của library).
- Công cụ: `/actuator/beans`, `ctx.getBeanDefinitionNames()`, `--debug` (condition report) cho bean auto-config.
- Bean name mặc định: tên class viết thường chữ đầu (`OrderService` → `orderService`; `URLParser` giữ nguyên theo `Introspector.decapitalize`).

**Câu hỏi nối tiếp:**
- *`@Repository` khác `@Component` ở điểm nào?* → Bật exception translation sang `DataAccessException` (`PersistenceExceptionTranslationPostProcessor`).

**⚠️ Câu trả lời gây điểm trừ:** "Thêm `@Autowired(required=false)`" để hết lỗi.

**📖 Ôn lại:** [§2.3 Component scanning](../01-giao-trinh/07-spring-core-boot.md#p2)

</details>

### Q8. 🟡 Ứng dụng lớn khởi động mất 90 giây. Bạn có những hướng tối ưu nào và trade-off của `spring.main.lazy-initialization=true`?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Đo trước: `BufferingApplicationStartup` + `/actuator/startup` hoặc JFR (`FlightRecorderApplicationStartup`) để biết bean/bước nào chậm. Hướng tối ưu: thu hẹp component scan, bỏ starter/auto-config thừa, chuyển công việc nặng (warm-up cache, gọi HTTP) khỏi `@PostConstruct` sang `ApplicationReadyEvent`/background, Hibernate (tắt `ddl-auto` validate nặng), **CDS/AppCDS**, **CRaC**, hoặc **native image**. `lazy-initialization=true`: startup nhanh nhưng **lỗi cấu hình lộ ra muộn** (lúc request đầu), request đầu chậm, và thời gian khởi tạo bị dồn vào traffic thật — chỉ hợp dev/test/serverless.

**Giải thích chi tiết:**
- Spring Boot mặc định **cấm bean definition overriding** (từ Boot 2.1, `spring.main.allow-bean-definition-overriding=false`) → hai bean trùng tên báo lỗi ngay; đừng bật lại để "sửa" lỗi.
- `spring-context-indexer` đã deprecated từ Spring 6.1 — không đề xuất như giải pháp mới.
- Kubernetes: startup chậm cần `startupProbe` phù hợp thay vì tăng `initialDelaySeconds` của liveness.

**Câu hỏi nối tiếp:**
- *CDS là gì?* → Class Data Sharing — lưu metadata class đã parse vào archive để JVM sau nạp nhanh hơn; Boot 3.3 hỗ trợ trích xuất dễ dàng.

**⚠️ Câu trả lời gây điểm trừ:** Bật lazy-init trên production mà không nhắc trade-off; tối ưu mù không đo.

**📖 Ôn lại:** [§2.3 Góc nhìn Senior](../01-giao-trinh/07-spring-core-boot.md#p2)

</details>

---

<a id="nhom-c"></a>
## C. `@Configuration`, `@Qualifier`/`@Primary`, conditional bean

### Q9. 🟡 [Đọc code] Có bao nhiêu `DataSource` được tạo trong mỗi trường hợp?

```java
@Configuration                                   // (A) — và thử lại với (B): @Component thay cho @Configuration
public class DataConfig {
    @Bean public DataSource dataSource() { return new HikariDataSource(); }
    @Bean public JdbcTemplate jdbcTemplate() { return new JdbcTemplate(dataSource()); }
    @Bean public TransactionManager txManager() { return new DataSourceTransactionManager(dataSource()); }
}
```

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** (A) `@Configuration` (full mode, `proxyBeanMethods = true`): class bị **CGLIB subclass**; lời gọi `dataSource()` bên trong bị intercept và trả về **bean singleton** → chỉ **1** `DataSource`. (B) `@Component` (lite mode, hoặc `proxyBeanMethods = false`): `dataSource()` là gọi method Java thường → **3** `DataSource` (1 bean + 2 object ngoài container, không lifecycle, không đóng khi shutdown) — `JdbcTemplate` và transaction manager dùng **hai pool khác nhau**, transaction không bao được JDBC → bug nghiêm trọng.

**Giải thích chi tiết:**
- Cách viết đúng cho cả hai mode: **nhận dependency qua tham số `@Bean` method**:

```java
@Bean JdbcTemplate jdbcTemplate(DataSource ds) { return new JdbcTemplate(ds); }
```

- Full mode: class/method `@Bean` không được `final`; tên class runtime dạng `DataConfig$$SpringCGLIB$$0`.

**Câu hỏi nối tiếp:**
- *Vì sao auto-configuration dùng lite mode?* → Xem Q10.

**⚠️ Câu trả lời gây điểm trừ:** "Luôn chỉ 1 vì bean là singleton" mà không phân biệt mode.

**📖 Ôn lại:** [§3.1–3.2 Full vs lite mode](../01-giao-trinh/07-spring-core-boot.md#p3)

</details>

### Q10. 🔴 Vì sao toàn bộ auto-configuration của Boot dùng `proxyBeanMethods = false`? Và vì sao method `@Bean` trả về `BeanFactoryPostProcessor` phải là `static`?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `proxyBeanMethods = false` tránh chi phí tạo CGLIB subclass cho hàng trăm class auto-config → startup nhanh, ít bộ nhớ, thân thiện **GraalVM native/AOT** (không sinh class runtime); điều kiện là mọi dependency đều truyền qua tham số `@Bean`. `@Bean` trả `BeanFactoryPostProcessor` (ví dụ `PropertySourcesPlaceholderConfigurer`) phải `static` vì BFPP cần được tạo **rất sớm** — nếu non-static, Spring phải khởi tạo *instance class config* trước để gọi method → class config bị tạo sớm, `@Value`/`@Autowired`/`@PostConstruct` trong class đó không được xử lý. Spring log cảnh báo `@Bean method ... is non-static and returns an object assignable to Spring's BeanFactoryPostProcessor interface`.

**Giải thích chi tiết:**
- Annotation `@AutoConfiguration` mặc định đã là `proxyBeanMethods = false`.
- Lý do tương tự cho `BeanPostProcessor`: khai báo `static @Bean`, và inject dependency lazily (`ObjectProvider`) để không kéo bean khác khởi tạo sớm (Q19).

**Câu hỏi nối tiếp:**
- *Tắt proxy cho config của chính mình có rủi ro gì?* → Chỉ khi code còn gọi method `@Bean` trực tiếp — kiểm tra trước khi tắt.

**⚠️ Câu trả lời gây điểm trừ:** Không giải thích được vì sao "sớm" là vấn đề.

**📖 Ôn lại:** [§3.2 Góc nhìn Senior & Lỗi thường gặp](../01-giao-trinh/07-spring-core-boot.md#p3)

</details>

### Q11. 🟢 `@Primary` và `@Qualifier` khác nhau thế nào? Dùng khi nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `@Primary` = "mặc định khi không nói gì" — đặt trên bean; `@Qualifier` = "chọn chính xác cái này" — đặt ở điểm inject (và bean), **thắng** `@Primary`. Dùng `@Primary` khi có một implementation chính và vài cái đặc biệt; dùng `@Qualifier` khi caller cần chọn rõ ràng. Spring 6.2 có thêm `@Fallback` — bean chỉ được chọn khi không còn candidate khác.

**Giải thích chi tiết:**

```java
@Component @Primary class EmailNotifier implements Notifier { ... }
@Component @Qualifier("sms") class SmsNotifier implements Notifier { ... }

AlertService(Notifier defaultNotifier, @Qualifier("sms") Notifier urgentNotifier) { ... }
```

- Hai `@Primary` cùng kiểu → vẫn `NoUniqueBeanDefinitionException`.
- Khi cần *tất cả* implementation → `List<Notifier>`; chọn theo dữ liệu runtime → `Map<String, Notifier>` hoặc registry.

**Câu hỏi nối tiếp:**
- *Thư viện cung cấp bean mặc định mà app muốn override?* → Library dùng `@ConditionalOnMissingBean` (auto-config) thay vì `@Primary`.

**⚠️ Câu trả lời gây điểm trừ:** Nói `@Primary` thắng `@Qualifier`.

**📖 Ôn lại:** [§3.3 @Qualifier, @Primary](../01-giao-trinh/07-spring-core-boot.md#p3)

</details>

### Q12. 🟡 Các loại conditional bean? Vì sao Spring Boot khuyến cáo không dùng `@ConditionalOnMissingBean` trong cấu hình của ứng dụng?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `@Profile`, `@Conditional(Condition)` (nền tảng), và các `@ConditionalOn*` của Boot: `OnProperty`, `OnClass`, `OnMissingBean`, `OnBean`, `OnWebApplication`, `OnExpression`, `OnCloudPlatform`, `OnThreading` (Boot 3.2). `@ConditionalOnBean/OnMissingBean` đánh giá dựa trên những gì **đã được đăng ký tại thời điểm xử lý**; với user config thứ tự xử lý giữa các class không đảm bảo → kết quả phụ thuộc thứ tự. Auto-config được xử lý **sau** user config (`DeferredImportSelector`) nên mới dùng an toàn.

**Giải thích chi tiết:**

```java
@Bean
@ConditionalOnProperty(prefix = "app.cache", name = "type", havingValue = "redis")
CacheClient redisCacheClient(RedisConnectionFactory f) { return new RedisCacheClient(f); }
```

- Feature toggle bằng property: chú ý `matchIfMissing` để xác định hành vi mặc định; tránh trạng thái hai bean cùng tồn tại.
- Test conditional nhanh bằng `ApplicationContextRunner.withPropertyValues(...)`.
- AOT/native: condition được đánh giá **lúc build** — condition phụ thuộc runtime (giờ hệ thống, env khác lúc build) sẽ sai.

**Câu hỏi nối tiếp:**
- *`@Profile` hay `@ConditionalOnProperty` cho feature flag?* → Property: linh hoạt, bật/tắt từng tính năng; profile cho nhóm đặc tính môi trường.

**⚠️ Câu trả lời gây điểm trừ:** Không biết thứ tự xử lý user config vs auto-config.

**📖 Ôn lại:** [§3.4 Conditional bean](../01-giao-trinh/07-spring-core-boot.md#p3)

</details>

---

<a id="nhom-d"></a>
## D. Bean scopes

### Q13. 🟢 Spring có những scope nào? Singleton bean có thread-safe không?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `singleton` (mặc định, 1 instance/container — không phải JVM), `prototype` (instance mới mỗi lần lấy, container **không gọi destroy**), `request`, `session`, `application`, `websocket`; có thể tự định nghĩa (`@RefreshScope` của Spring Cloud). Singleton **không tự thread-safe**: mọi request thread dùng chung instance → bean phải **stateless**, hoặc state phải thread-safe (`ConcurrentHashMap`, `AtomicLong`, immutable).

**Giải thích chi tiết:**
- Bug production kinh điển: field `SimpleDateFormat` (không thread-safe) hoặc `List` cache trong `@Service` → dữ liệu hỏng/`ArrayIndexOutOfBounds` ngẫu nhiên dưới tải. Dùng `DateTimeFormatter` (immutable).
- Session scope: cẩn thận dung lượng session và replicate session khi scale ngang.

**Câu hỏi nối tiếp:**
- *Prototype có `@PreDestroy` không?* → `@PostConstruct` chạy mỗi lần tạo, `@PreDestroy` không bao giờ chạy → tự dọn tài nguyên.

**⚠️ Câu trả lời gây điểm trừ:** "Singleton của Spring thread-safe vì Spring quản lý."

**📖 Ôn lại:** [§4.1 Các scope](../01-giao-trinh/07-spring-core-boot.md#p4)

</details>

### Q14. 🟡 Inject bean `prototype` vào bean `singleton` thì chuyện gì xảy ra? Các cách giải?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Singleton chỉ được tạo một lần nên dependency prototype cũng chỉ inject **một lần** → trên thực tế thành singleton (mọi request dùng chung một `ShoppingCart`). Cách giải: (1) **`ObjectProvider<T>.getObject()`** mỗi lần cần — khuyến nghị; (2) **`@Lookup`** method injection — Spring CGLIB override method trả bean mới; (3) **scoped proxy** (`proxyMode = TARGET_CLASS`) — nhưng với prototype có bẫy (Q15).

**Giải thích chi tiết:**

```java
@Service
class CheckoutService {
    private final ObjectProvider<ShoppingCart> carts;
    CheckoutService(ObjectProvider<ShoppingCart> carts) { this.carts = carts; }
    void checkout() { ShoppingCart cart = carts.getObject(); /* instance mới */ }
}
```

- Tránh `applicationContext.getBean()` rải rác (Service Locator).
- `jakarta.inject.Provider<T>` cũng dùng được.

**Câu hỏi nối tiếp:**
- *Prototype có thực sự cần không?* → Thường object có state theo use case nên là object thường (`new`) hoặc tạo bởi factory bean, không cần là Spring bean.

**⚠️ Câu trả lời gây điểm trừ:** Nghĩ mỗi lần gọi method của singleton sẽ nhận prototype mới.

**📖 Ôn lại:** [§4.2 Tiêm prototype vào singleton](../01-giao-trinh/07-spring-core-boot.md#p4)

</details>

### Q15. 🔴 [Đọc code] Đoạn sau in ra gì? Vì sao?

```java
@Component
@Scope(value = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)
class Cart { private final List<String> items = new ArrayList<>();
             void add(String s) { items.add(s); } int size() { return items.size(); } }

@Service
class Shop {
    private final Cart cart;
    Shop(Cart cart) { this.cart = cart; }
    void demo() { cart.add("A"); cart.add("B"); System.out.println(cart.size()); }
}
```

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** In `0`. Bean được inject là **CGLIB scoped proxy**; *mỗi lời gọi method* trên proxy đi qua `getTarget()` → `beanFactory.getBean(targetName)` → với prototype là **instance mới**. `add("A")`, `add("B")`, `size()` chạy trên ba `Cart` khác nhau. Với prototype, `ObjectProvider` gần như luôn là lựa chọn đúng: lấy một instance, dùng nó cho cả use case.

**Giải thích chi tiết:**
- Cơ chế: `ScopedProxyFactoryBean` + `SimpleBeanTargetSource` (target lấy lại từ BeanFactory mỗi lần invoke).
- Scoped proxy **đúng nghĩa** cho `request`/`session` scope: mỗi lần gọi tra target trong `RequestContextHolder` (ThreadLocal) của request hiện tại → cùng request cùng instance.

**Câu hỏi nối tiếp:**
- *Sửa thế nào?* → `ObjectProvider<Cart>`; `Cart cart = carts.getObject(); cart.add(...); cart.size()`.

**⚠️ Câu trả lời gây điểm trừ:** Trả lời `2`.

**📖 Ôn lại:** [§4.2 Lỗi thường gặp (scoped proxy + prototype)](../01-giao-trinh/07-spring-core-boot.md#p4)

</details>

### Q16. 🟡 Làm sao inject bean `request` scope vào singleton? Truy cập nó từ method `@Async` thì sao?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Bắt buộc qua **scoped proxy** hoặc `ObjectProvider` — `@RequestScope`/`@SessionScope` mặc định đã có `proxyMode = TARGET_CLASS`. Nếu không có proxy, Spring báo lỗi lúc startup (`Scope 'request' is not active for the current thread` / `No thread-bound request found`). Từ thread `@Async` hoặc thread pool khác: **không có request context** (lưu trong ThreadLocal của request thread) → exception; cần copy dữ liệu cần thiết (correlation id, user) vào tham số hoặc qua `TaskDecorator`.

**Giải thích chi tiết:**
- Ví dụ: `RequestContext` (`@RequestScope`) lưu `correlationId` được `HandlerInterceptor` set trong `preHandle`; service singleton đọc qua proxy.
- Thay thế đơn giản hơn: MDC + `TaskDecorator` cho log; truyền tường minh object ngữ cảnh.

**Câu hỏi nối tiếp:**
- *`RequestContextHolder.setRequestAttributes(attrs, true)` (inheritable)?* → Nguy hiểm với thread pool (thread tái sử dụng giữ context cũ/request đã kết thúc).

**⚠️ Câu trả lời gây điểm trừ:** "Inject thẳng như bean thường."

**📖 Ôn lại:** [§4.2 & Lỗi thường gặp](../01-giao-trinh/07-spring-core-boot.md#p4)

</details>

---

<a id="nhom-e"></a>
## E. Bean lifecycle, BeanPostProcessor

### Q17. 🟡 Mô tả vòng đời một singleton bean từ lúc tạo tới lúc hủy.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Đăng ký `BeanDefinition` → chạy `BeanFactoryPostProcessor` → đăng ký `BeanPostProcessor`. Với từng bean: **instantiate** (constructor injection) → **populate** (field/setter injection) → **Aware** (`BeanNameAware`, `BeanFactoryAware`, rồi `ApplicationContextAware`... qua BPP) → **BPP before-init** (`@PostConstruct` chạy ở đây) → `InitializingBean.afterPropertiesSet()` → custom `initMethod` → **BPP after-init (nơi tạo AOP proxy)** → bean sẵn sàng; sau khi mọi singleton xong: `SmartInitializingSingleton`, `SmartLifecycle.start()`, `ContextRefreshedEvent`. Hủy: `SmartLifecycle.stop()` → `@PreDestroy` → `DisposableBean.destroy()` → destroy-method.

**Giải thích chi tiết:**
- Chi tiết thêm: `InstantiationAwareBPP.postProcessBeforeInstantiation` (có thể trả proxy thay thế), `MergedBeanDefinitionPostProcessor` thu thập metadata `@Autowired`/`@PostConstruct`, đưa `ObjectFactory` vào `singletonFactories` (cache cấp 3) trước populate.
- `@Bean` tự suy luận destroy method `close`/`shutdown` (inferred) — vì vậy `DataSource`, executor được đóng tự động.
- `@PostConstruct` chạy **trước** khi proxy được tạo.

**Câu hỏi nối tiếp:**
- *Nên warm-up cache ở đâu?* → `ApplicationReadyEvent` hoặc `SmartLifecycle`, không phải `@PostConstruct` (Q20).

**⚠️ Câu trả lời gây điểm trừ:** Không nêu được proxy được tạo ở bước nào.

**📖 Ôn lại:** [§5.1 Toàn cảnh vòng đời](../01-giao-trinh/07-spring-core-boot.md#p5)

</details>

### Q18. 🟢 `BeanFactoryPostProcessor` khác `BeanPostProcessor` thế nào? Cho ví dụ có sẵn trong Spring.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** BFPP làm việc với **`BeanDefinition` (metadata)**, chạy **trước** khi bean thường nào được tạo — có thể đổi scope, class, property value, thêm/xóa definition. Ví dụ: `PropertySourcesPlaceholderConfigurer` (thay `${...}`), `ConfigurationClassPostProcessor`. BPP làm việc với **instance** bean, chạy quanh bước initialization của từng bean — có thể bọc proxy, inject, validate. Ví dụ: `AutowiredAnnotationBeanPostProcessor`, `CommonAnnotationBeanPostProcessor` (`@PostConstruct`), `AnnotationAwareAspectJAutoProxyCreator`, `AsyncAnnotationBeanPostProcessor`, `ConfigurationPropertiesBindingPostProcessor`.

**Giải thích chi tiết:**
- Viết BPP riêng: ví dụ `@Timed` — `postProcessAfterInitialization` kiểm tra method có annotation thì bọc bằng `ProxyFactory` (`setProxyTargetClass(true)`).
- BFPP/BPP khai báo `static @Bean`.

**Câu hỏi nối tiếp:**
- *BPP có áp dụng cho chính BPP khác không?* → BPP được tạo trước bean thường; BPP tạo sau không xử lý được BPP tạo trước.

**⚠️ Câu trả lời gây điểm trừ:** Nhầm thời điểm chạy của hai loại.

**📖 Ôn lại:** [§5.2 BPP vs BFPP](../01-giao-trinh/07-spring-core-boot.md#p5)

</details>

### Q19. 🔴 Log startup có dòng `Bean 'xyz' ... is not eligible for getting processed by all BeanPostProcessors (for example: not eligible for auto-proxying)`. Nguyên nhân và hậu quả?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Bean `xyz` bị khởi tạo **quá sớm** — thường vì một `BeanPostProcessor` (hoặc BFPP) phụ thuộc vào nó (trực tiếp hoặc bắc cầu), nên nó được tạo trước khi các BPP khác (đặc biệt auto-proxy creator) được đăng ký. Hậu quả: bean là object "trần", **không có proxy** → `@Transactional`, `@Async`, `@Cacheable`, `@PreAuthorize` trên bean đó **im lặng không chạy**. Sửa: BPP khai báo `static @Bean`, không inject service nghiệp vụ vào BPP, hoặc inject lazily (`ObjectProvider`, `@Lazy`).

**Giải thích chi tiết:**
- Ví dụ thực tế: một BPP custom inject `AuditService` (có `@Transactional`) để ghi log khi tạo bean → `AuditService` và cả repository của nó bị tạo sớm, mất transaction.
- Cách phát hiện: grep log startup tìm "not eligible"; yêu cầu CI fail nếu xuất hiện với package của công ty.
- Cùng họ: method `@Bean` non-static trả BFPP làm class config bị tạo sớm (Q10).

**Câu hỏi nối tiếp:**
- *Vì sao không thấy lỗi ngay?* → Không có exception; chỉ hành vi thiếu (không rollback, không async) — vì vậy rất nguy hiểm.

**⚠️ Câu trả lời gây điểm trừ:** "Chỉ là warning, bỏ qua được."

**📖 Ôn lại:** [§5.2 Góc nhìn Senior](../01-giao-trinh/07-spring-core-boot.md#p5)

</details>

### Q20. 🟡 [Đọc code] Có vấn đề gì với bean sau?

```java
@Service
class ProductCatalog {
    private final ProductRepository repo;
    private Map<String, Product> cache;
    ProductCatalog(ProductRepository repo) { this.repo = repo; }

    @PostConstruct
    void init() {
        cache = loadAll();                        // đọc 2 triệu sản phẩm + gọi API giá
    }

    @Transactional(readOnly = true)
    public Map<String, Product> loadAll() { ... }
}
```

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** (1) `@PostConstruct` chạy **trước khi proxy được tạo** và gọi `loadAll()` qua `this` → **không có transaction** (lazy loading có thể ném `LazyInitializationException`, `readOnly` không áp dụng); (2) I/O nặng trong `@PostConstruct` **chặn startup**, lỗi API ngoài làm app không khởi động được, và readiness/probe dễ timeout; (3) `cache` không `volatile`/không thread-safe nếu sau này được làm mới. Sửa: warm-up trong listener `ApplicationReadyEvent` (gọi qua proxy của bean khác hoặc `TransactionTemplate`), có timeout/fallback, hoặc lazy load với Caffeine.

**Giải thích chi tiết:**

```java
@Component
class CatalogWarmer {
    private final ProductCatalog catalog;     // proxy
    CatalogWarmer(ProductCatalog catalog) { this.catalog = catalog; }
    @EventListener(ApplicationReadyEvent.class)
    void warm() { catalog.refresh(); }        // đi qua proxy → có transaction
}
```

- Muốn app **không nhận traffic** khi cache chưa sẵn: chủ động đặt readiness `REFUSING_TRAFFIC` rồi `ACCEPTING_TRAFFIC` (`AvailabilityChangeEvent.publish`).

**Câu hỏi nối tiếp:**
- *`CommandLineRunner` chạy khi nào so với readiness?* → Trước `ApplicationReadyEvent`/`ACCEPTING_TRAFFIC`; runner lâu làm trễ readiness.

**⚠️ Câu trả lời gây điểm trừ:** Không thấy vấn đề transaction trong `@PostConstruct`.

**📖 Ôn lại:** [§5.2 Góc nhìn Senior (@PostConstruct)](../01-giao-trinh/07-spring-core-boot.md#p5)

</details>

### Q21. 🟡 Sau khi nâng Boot 2 → 3, một số method `@PostConstruct` "không chạy" mà không có lỗi. Vì sao?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Boot 3/Spring 6 dùng **`jakarta.annotation.PostConstruct`/`PreDestroy`**; nếu code vẫn import `javax.annotation.PostConstruct` (do một thư viện cũ còn kéo `javax.annotation-api` lên classpath nên vẫn compile được), Spring 6 **im lặng bỏ qua** method đó. Sửa: đổi import sang `jakarta.*` (OpenRewrite làm tự động), loại dependency `javax.annotation-api` cũ, thêm kiểm tra (ArchUnit/grep CI) cấm `javax.annotation.PostConstruct`.

**Giải thích chi tiết:**
- Cùng họ lỗi migration: `javax.persistence` → `jakarta.persistence`, `javax.validation` → `jakarta.validation`, `javax.servlet` → `jakarta.servlet` (library chưa hỗ trợ Jakarta → `ClassNotFoundException: javax.servlet.Filter`).
- Thay thế không phụ thuộc annotation: `InitializingBean`/`@Bean(initMethod=...)`.

**Câu hỏi nối tiếp:**
- *Bean prototype có `@PreDestroy` không chạy là bình thường?* → Có — container không quản lý destroy của prototype.

**⚠️ Câu trả lời gây điểm trừ:** Không biết thay đổi `javax` → `jakarta`.

**📖 Ôn lại:** [§5.1 Lưu ý jakarta](../01-giao-trinh/07-spring-core-boot.md#p5)

</details>

---

<a id="nhom-f"></a>
## F. Circular dependency & 3-level cache

### Q22. 🟡 Circular dependency là gì? Constructor injection và field injection xử lý khác nhau thế nào? Spring Boot 2.6 thay đổi gì?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `A` cần `B`, `B` cần `A`. Với **constructor injection** cả hai phía: không thể giải (muốn tạo A cần B hoàn chỉnh và ngược lại) → `BeanCurrentlyInCreationException`. Với **field/setter injection** trên singleton: Spring giải được nhờ **early reference** — tạo A (chưa populate), công bố tham chiếu sớm, B nhận tham chiếu sớm đó. Từ **Boot 2.6**, `spring.main.allow-circular-references=false` mặc định → app có vòng tròn fail lúc startup kèm sơ đồ vòng, kể cả khi dùng field injection (Spring Framework thuần vẫn cho phép).

**Giải thích chi tiết:**
- Lý do Boot cấm: vòng tròn là dấu hiệu thiết kế kém; early reference "chưa hoàn chỉnh" có thể bị dùng (B gọi A trong `@PostConstruct` khi A chưa populate xong); xung đột với proxy (Q24).
- Prototype có vòng tròn không bao giờ giải được (không cache).
- Constructor injection làm lộ vòng tròn ngay — đây là *ưu điểm*.

**Câu hỏi nối tiếp:**
- *Cách sửa?* → Xem Q25.

**⚠️ Câu trả lời gây điểm trừ:** "Spring tự giải hết circular dependency nên không sao."

**📖 Ôn lại:** [§6.1 & §6.3](../01-giao-trinh/07-spring-core-boot.md#p6)

</details>

### Q23. 🔴 Giải thích cơ chế 3-level cache của `DefaultSingletonBeanRegistry`. Vì sao không chỉ cần 2 cấp?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Cấp 1 `singletonObjects`: bean hoàn chỉnh. Cấp 2 `earlySingletonObjects`: early reference đã từng được lấy ra. Cấp 3 `singletonFactories`: `ObjectFactory` sinh early reference bằng `getEarlyBeanReference()` qua các `SmartInstantiationAwareBeanPostProcessor`. Cần cấp 3 vì **AOP**: nếu A cần proxy (`@Transactional`), B phải nhận **proxy** của A chứ không phải object gốc, mà proxy bình thường chỉ tạo ở `postProcessAfterInitialization` (cuối vòng đời). Cấp 3 **trì hoãn** quyết định: chỉ khi thật sự có vòng tròn cần early reference thì mới tạo proxy sớm (`AbstractAutoProxyCreator.getEarlyBeanReference`); không vòng tròn thì không tốn chi phí. Kết quả chuyển vào cấp 2 để mọi bên dùng cùng một early reference.

**Giải thích chi tiết:**

```
getBean(A): instantiate A → singletonFactories.put(A, () -> getEarlyBeanReference(A))
  populate A → cần B → getBean(B)
     instantiate B → populate B → cần A → getSingleton(A):
        cấp1? không → cấp2? không → cấp3? có → gọi factory → (proxy của) A → chuyển sang cấp2
     B hoàn chỉnh → cấp1
  initialize A → kiểm tra: bean cuối cùng có == early reference đã phát ra không?
     khác (một BPP khác bọc A thành object khác) → BeanCurrentlyInCreationException "raw version"
```

- Ý tưởng `singletonsCurrentlyInCreation` (set bean đang tạo) là thứ dùng để phát hiện vòng tròn constructor.

**Câu hỏi nối tiếp:**
- *Auto-proxy creator làm gì ở `postProcessAfterInitialization` nếu đã tạo proxy sớm?* → Ghi nhớ bean đã được proxy sớm (`earlyProxyReferences`) để không bọc lần hai, trả bean gốc; container dùng early reference đã phát ra.

**⚠️ Câu trả lời gây điểm trừ:** Thuộc tên 3 map nhưng không giải thích được vai trò của AOP.

**📖 Ôn lại:** [§6.2 Ba cấp cache](../01-giao-trinh/07-spring-core-boot.md#p6)

</details>

### Q24. 🔴 Với `allow-circular-references=true`, vòng A ↔ B (field injection) chạy được khi A có `@Transactional`, nhưng fail với `has been injected into other beans [b] in its raw version` khi A có `@Async`. Vì sao?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `@Transactional` được proxy bởi `AbstractAutoProxyCreator` — có hỗ trợ `getEarlyBeanReference()`, nên B nhận đúng proxy sớm và bean cuối cùng trùng early reference. `@Async` được bọc bởi `AsyncAnnotationBeanPostProcessor` — một `AbstractAdvisingBeanPostProcessor` chỉ bọc proxy ở `postProcessAfterInitialization`, **không tham gia early reference**. B đã nhận A *gốc* (raw), sau đó A bị bọc thành proxy khác → Spring phát hiện bean cuối ≠ reference đã inject và ném lỗi.

**Giải thích chi tiết:**
- Sửa: phá vòng tròn (khuyến nghị); `@Lazy` tại điểm inject A vào B (B nhận lazy proxy, resolve A hoàn chỉnh khi gọi lần đầu); hoặc tách method `@Async` sang bean riêng.
- Bài học: "chạy được" với một annotation không bảo đảm chạy được với annotation khác — circular dependency + proxy là vùng mong manh.

**Câu hỏi nối tiếp:**
- *Còn BPP nào khác gây lỗi tương tự?* → Bất kỳ BPP tự viết nào trả object khác trong `postProcessAfterInitialization` mà không implement `getEarlyBeanReference`.

**⚠️ Câu trả lời gây điểm trừ:** "Do `@Async` lỗi" mà không giải thích cơ chế.

**📖 Ôn lại:** [§6 Bài 6.3 Raw version injected](../01-giao-trinh/07-spring-core-boot.md#p6)

</details>

### Q25. 🟡 [Tình huống] Nâng cấp lên Boot 3, app fail vì `UserService ↔ OrderService` vòng tròn. Bạn xử lý thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Theo thứ tự ưu tiên: (1) **Thiết kế lại** — tách phần dùng chung thành bean thứ ba (ví dụ `UserQueryService`) mà cả hai phụ thuộc, hoặc xác định ai là "owner"; (2) **đảo phụ thuộc bằng event** — A publish event chứa đủ dữ liệu, B lắng nghe; (3) `@Lazy` trên tham số constructor hoặc `ObjectProvider<B>` — tạm thời, ghi ticket technical debt; (4) bật `spring.main.allow-circular-references=true` chỉ như giải pháp tạm khi migrate.

**Giải thích chi tiết:**

```java
@Service
class UserService {
    private final OrderService orders;
    UserService(@Lazy OrderService orders) { this.orders = orders; }   // inject lazy proxy — tạm thời
}
```

- Cách (2): `AccountService.close()` publish `AccountClosedEvent(accountId, email)` → `NotificationService` không cần gọi ngược `findEmail()`. Trade-off: luồng khó theo dõi hơn; nếu cần gửi sau commit → `@TransactionalEventListener`.
- Vòng tròn hay xuất hiện gián tiếp qua self-injection để gọi method `@Transactional`.

**Câu hỏi nối tiếp:**
- *So sánh trade-off (a) tách service và (b) event?* → (a) rõ ràng, đồng bộ, dễ test; (b) giảm coupling nhưng gián tiếp, khó debug, cần quan tâm transaction phase.

**⚠️ Câu trả lời gây điểm trừ:** Bật cờ `allow-circular-references` là xong.

**📖 Ôn lại:** [§6.3 Cách sửa](../01-giao-trinh/07-spring-core-boot.md#p6)

</details>

---

<a id="nhom-g"></a>
## G. Spring AOP, `@Transactional` và các annotation dựa trên proxy

### Q26. 🟢 Giải thích các khái niệm AOP: aspect, join point, pointcut, advice, weaving. Spring AOP khác AspectJ thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** AOP tách **cross-cutting concern** (transaction, logging, security, cache, metrics, retry) khỏi business code. **Aspect**: module chứa logic cross-cutting; **join point**: điểm có thể chèn logic — với Spring AOP **chỉ là method execution trên bean Spring**; **pointcut**: biểu thức chọn join point; **advice**: code chạy tại join point (`@Before`, `@AfterReturning`, `@AfterThrowing`, `@After`, `@Around`); **weaving**: gắn aspect vào code. Spring AOP weave **lúc runtime bằng proxy**; AspectJ weave lúc compile/load-time vào bytecode, hỗ trợ cả field access, constructor, và self-invocation.

**Giải thích chi tiết:**

```java
@Aspect @Component @Order(1)
public class PerformanceAspect {
    @Pointcut("within(com.example.shop.service..*)") void serviceLayer() {}
    @Around("serviceLayer() && @annotation(timed)")
    public Object measure(ProceedingJoinPoint pjp, Timed timed) throws Throwable {
        long start = System.nanoTime();
        try { return pjp.proceed(); }
        finally { /* log nếu chậm hơn timed.warnMs() */ }
    }
}
```

- Pointcut designators: `execution`, `within`, `@annotation`, `@within`, `bean(...)` (riêng Spring), `args`, `this` (kiểu proxy) / `target` (kiểu target).
- Giới hạn pointcut trong package công ty — pointcut quá rộng bọc cả bean hạ tầng, tăng startup, có thể fail với class `final`.

**Câu hỏi nối tiếp:**
- *`@Around` quên gọi `proceed()`?* → Method thật không chạy, trả `null`/default — bug khó thấy.

**⚠️ Câu trả lời gây điểm trừ:** Nghĩ Spring AOP chặn được mọi lời gọi method (kể cả object không phải bean).

**📖 Ôn lại:** [§7.1 Khái niệm AOP](../01-giao-trinh/07-spring-core-boot.md#p7)

</details>

### Q27. 🟡 Spring dùng JDK dynamic proxy hay CGLIB? Spring Boot khác Spring Framework thuần ở điểm nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Spring Framework thuần: bean có ≥ 1 interface → JDK proxy (chỉ implement interface, inject bằng class cụ thể → `BeanNotOfRequiredTypeException`); không có interface → CGLIB (subclass, cần class/method không `final`). **Spring Boot 2.0+** mặc định `spring.aop.proxy-target-class=true` → CGLIB cho hầu hết trường hợp. Spring 6 dùng Objenesis để tạo instance CGLIB **không gọi constructor** của target lần hai.

**Giải thích chi tiết:**
- Hệ quả CGLIB: method `final` không bị intercept và chạy trên instance proxy (field null) → NPE khó hiểu; class `final` (ví dụ Kotlin mặc định `final` → cần plugin `kotlin-spring`/all-open).
- Visibility: Spring 6 cho phép `@Transactional` trên method `protected`/package-private với CGLIB; `private` không bao giờ.
- Kiểm tra: `AopUtils.isCglibProxy`, tên class `$$SpringCGLIB$$`.

**Câu hỏi nối tiếp:**
- *Chi phí proxy?* → Vài trăm ns mỗi lời gọi — không đáng kể so với I/O; chi phí thật là startup và độ phức tạp debug.

**⚠️ Câu trả lời gây điểm trừ:** Không biết mặc định của Boot.

**📖 Ôn lại:** [§7.2 JDK proxy vs CGLIB](../01-giao-trinh/07-spring-core-boot.md#p7)

</details>

### Q28. 🔴 [Đọc code] Bạn kiểm chứng bằng cách nào rằng `inner()` có chạy trong transaction mới hay không? Kết quả là gì và sửa ra sao?

```java
@Service
class AuditService {
    @Transactional
    public void outer() {
        repo.save(new Event("outer"));
        inner();
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void inner() {
        repo.save(new Event("inner"));
        throw new IllegalStateException("boom");
    }
}
```

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `inner()` là self-invocation → **không** có transaction mới; nó chạy trong transaction của `outer()`. Exception lan ra `outer()` → **cả hai** bản ghi bị rollback (ý định có thể là: `inner` rollback riêng, `outer` vẫn commit nếu bắt lỗi). Kiểm chứng: log `TransactionSynchronizationManager.getCurrentTransactionName()` trong `inner()` — sẽ thấy tên transaction của `outer` (`...AuditService.outer`), hoặc bật `logging.level.org.springframework.transaction=TRACE`/`orm.jpa=DEBUG` để xem "Creating new transaction"/"Suspending current transaction". Sửa: tách `inner` sang bean khác, hoặc `TransactionTemplate` với `PROPAGATION_REQUIRES_NEW`.

**Giải thích chi tiết:**

```java
@Service
class AuditService {
    private final InnerAudit innerAudit;                // bean khác → gọi qua proxy
    private final TransactionTemplate requiresNew;
    AuditService(InnerAudit innerAudit, PlatformTransactionManager tm) {
        this.innerAudit = innerAudit;
        this.requiresNew = new TransactionTemplate(tm);
        this.requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }
}
```

- Nhớ: `REQUIRES_NEW` **suspend** transaction ngoài và mượn **thêm một connection** — pool nhỏ + nhiều request đồng thời có thể cạn pool (deadlock chờ connection).

**Câu hỏi nối tiếp:**
- *Có cách nào để self-invocation vẫn có transaction?* → `@Lazy` self-injection, `AopContext.currentProxy()` (`exposeProxy = true`), AspectJ mode — đều kém sạch hơn tách bean.

**⚠️ Câu trả lời gây điểm trừ:** "Inner rollback, outer commit vì đã khai báo REQUIRES_NEW."

**📖 Ôn lại:** [§7.3 Self-invocation](../01-giao-trinh/07-spring-core-boot.md#p7)

</details>

### Q29. 🔴 Exception nào làm `@Transactional` rollback? Giải thích `UnexpectedRollbackException`.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Mặc định chỉ **`RuntimeException` và `Error`** gây rollback; **checked exception → commit** (quy ước kế thừa từ EJB). Tùy chỉnh bằng `rollbackFor`/`noRollbackFor`. `UnexpectedRollbackException`: method bên trong (propagation `REQUIRED`, tham gia transaction ngoài) ném exception → interceptor đánh dấu transaction **rollback-only**; method ngoài bắt và nuốt exception rồi kết thúc bình thường → lúc commit, Spring phát hiện cờ rollback-only, rollback và ném `UnexpectedRollbackException: Transaction silently rolled back because it has been marked as rollback-only`.

**Giải thích chi tiết:**
- Muốn "lỗi bên trong không ảnh hưởng bên ngoài": `inner` dùng `REQUIRES_NEW` (transaction độc lập) hoặc `NESTED` (savepoint, chỉ với JDBC `DataSourceTransactionManager`; JPA không hỗ trợ đầy đủ).
- Nuốt exception trong chính method `@Transactional` (không có transaction con) → interceptor không thấy lỗi → **commit** dữ liệu dở dang.
- `TransactionAspectSupport.currentTransactionStatus().setRollbackOnly()` để rollback mà không ném.

**Câu hỏi nối tiếp:**
- *Vì sao Spring chọn mặc định commit với checked exception?* → Checked exception được coi là "kết quả nghiệp vụ có thể phục hồi"; nhiều team cấu hình `rollbackFor = Exception.class` hoặc chỉ dùng unchecked.

**⚠️ Câu trả lời gây điểm trừ:** "Mọi exception đều rollback."

**📖 Ôn lại:** [§7.4 @Transactional, @Cacheable, @Async đều là AOP](../01-giao-trinh/07-spring-core-boot.md#p7)

</details>

### Q30. 🔴 Bạn viết aspect retry khi gặp `ObjectOptimisticLockingFailureException` cho method có `@Transactional`. Thứ tự aspect quan trọng thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Retry phải bọc **ngoài** transaction: mỗi lần thử là một transaction mới, đọc lại dữ liệu mới nhất. Nếu retry nằm **trong**, nó thử lại bên trong transaction đã bị đánh dấu rollback-only/persistence context đã bẩn → vô nghĩa; hơn nữa exception optimistic lock thường ném lúc **flush/commit** — tức là ở tầng transaction interceptor, nên aspect bên trong không bắt được. Thứ tự theo `@Order`: giá trị **nhỏ hơn** = bọc **ngoài**; transaction advisor mặc định `Ordered.LOWEST_PRECEDENCE` → aspect retry đặt `@Order(Ordered.LOWEST_PRECEDENCE - 1)` (hoặc nhỏ hơn).

**Giải thích chi tiết:**

```java
@Aspect @Component
@Order(Ordered.LOWEST_PRECEDENCE - 1)          // bọc ngoài TransactionInterceptor
class OptimisticRetryAspect {
    @Around("@annotation(retry)")
    Object retry(ProceedingJoinPoint pjp, RetryOnOptimisticLock retry) throws Throwable {
        for (int attempt = 1; ; attempt++) {
            try { return pjp.proceed(); }
            catch (ObjectOptimisticLockingFailureException e) {
                if (attempt >= retry.maxAttempts()) throw e;
                Thread.sleep(ThreadLocalRandom.current().nextLong(10, 50));   // jitter
            }
        }
    }
}
```

- Có thể chỉnh order của advisor hệ thống: `@EnableTransactionManagement(order = ...)`, `@EnableCaching(order = ...)`, `@EnableAsync(order = ...)`.
- Trong cùng một aspect (Spring 5.2.7+): thứ tự loại advice `@Around, @Before, @After, @AfterReturning, @AfterThrowing`.
- Spring Retry `@Retryable` cũng phải nằm ngoài `@Transactional` (thường để retry ở method gọi vào service transactional).

**Câu hỏi nối tiếp:**
- *Retry có an toàn với mọi thao tác?* → Chỉ với thao tác idempotent hoặc có idempotency key; không retry khi đã gọi hệ thống ngoài không idempotent bên trong transaction.

**⚠️ Câu trả lời gây điểm trừ:** "Thứ tự không quan trọng"; đặt retry bên trong transaction.

**📖 Ôn lại:** [§7.5 Thứ tự advice & Bài 7.3](../01-giao-trinh/07-spring-core-boot.md#p7)

</details>

### Q31. 🟡 Self-invocation ảnh hưởng `@Cacheable`, `@Async`, `@PreAuthorize` thế nào? Trường hợp nào là lỗ hổng bảo mật?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Cả ba đều qua proxy nên gọi nội bộ **bỏ qua** advice: `@Cacheable` → không dùng cache (luôn query); `@Async` → chạy **đồng bộ** trên thread hiện tại; `@PreAuthorize` → **bỏ qua kiểm tra quyền** — đây là **lỗ hổng bảo mật** thật: method public không annotation `exportAll()` gọi `this.deleteAll()` có `@PreAuthorize("hasRole('ADMIN')")` → user thường xóa được dữ liệu.

**Giải thích chi tiết:**
- Nguyên tắc security: đặt kiểm tra quyền ở **biên** (method public được gọi từ ngoài/controller), kết hợp URL security deny-by-default; viết test ma trận quyền cho mọi endpoint.
- `@Async` + `@Transactional` trên cùng method: transaction chạy trên thread mới, không chia sẻ transaction của caller; caller không thấy dữ liệu chưa commit của nó.
- `@Async` method `void`: exception chỉ tới `AsyncUncaughtExceptionHandler`.

**Câu hỏi nối tiếp:**
- *Phát hiện mẫu này tự động?* → ArchUnit/rule review: method có annotation security không được gọi qua `this` từ method public khác cùng class; hoặc dùng AspectJ mode.

**⚠️ Câu trả lời gây điểm trừ:** Chỉ biết hệ quả với `@Transactional`.

**📖 Ôn lại:** [§7.4 Bảng annotation dựa trên proxy](../01-giao-trinh/07-spring-core-boot.md#p7)

</details>

### Q32. 🟡 [Tình huống] Dev báo "`@Cacheable`/`@Transactional` không chạy". Checklist debug của bạn?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Câu hỏi đầu tiên: **lời gọi có đi qua proxy không?** Checklist: (1) bean có do Spring quản lý không (không `new`); (2) có gọi qua `this` (self-invocation) không; (3) method có `private`/`final`, class `final` không; (4) đã bật tính năng chưa (`@EnableCaching`, `@EnableAsync`, `@EnableMethodSecurity`; transaction thì Boot tự bật); (5) log startup có "not eligible for auto-proxying" không; (6) import đúng annotation (`org.springframework.transaction.annotation.Transactional` vs `jakarta.transaction.Transactional` — cả hai được hỗ trợ nhưng thuộc tính khác nhau; `javax` cũ ở Boot 3 bị bỏ qua); (7) với transaction: exception là checked hay bị nuốt; (8) có nhiều `TransactionManager`/`CacheManager` không.

**Giải thích chi tiết:**
- Công cụ: breakpoint xem `this.getClass()` có `$$SpringCGLIB$$` không; stack trace có `TransactionInterceptor`/`CacheInterceptor` không; `AopUtils.isAopProxy(bean)`; log `org.springframework.transaction.interceptor=TRACE`.
- Cache: key mặc định từ tham số (`SimpleKey`); object làm key cần `equals/hashCode` đúng.

**Câu hỏi nối tiếp:**
- *Bean bị tạo bởi `@Bean` method trả kiểu interface nhưng inject bằng class?* → Với JDK proxy sẽ lỗi; với CGLIB ổn — nhưng auto-config `@ConditionalOnMissingBean` theo kiểu trả về có thể không thấy bean của bạn.

**⚠️ Câu trả lời gây điểm trừ:** Thử ngẫu nhiên (thêm annotation khắp nơi) mà không có phương pháp.

**📖 Ôn lại:** [§7.5 Góc nhìn Senior & Lỗi thường gặp](../01-giao-trinh/07-spring-core-boot.md#p7)

</details>

---

<a id="nhom-h"></a>
## H. SpEL & Application Events

### Q33. 🟢 `${...}` khác `#{...}` thế nào? Rủi ro bảo mật của SpEL?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `${...}` là **property placeholder** — thay giá trị từ `Environment` (có default `${app.page-size:20}`). `#{...}` là **SpEL** — biểu thức được đánh giá: gọi method, toán tử, tham chiếu bean `@beanName`, Elvis `?:`, selection `.?[]`, projection `.![]`. SpEL còn dùng trong `@PreAuthorize`, `@Cacheable(key=...)`, `@EventListener(condition=...)`, `@ConditionalOnExpression`. Rủi ro: **không bao giờ đánh giá SpEL từ input người dùng** với `StandardEvaluationContext` — có thể gọi `T(java.lang.Runtime).getRuntime().exec(...)` (nhiều CVE SpEL injection); nếu buộc phải dùng thì `SimpleEvaluationContext`.

**Giải thích chi tiết:**

```java
@Value("${app.page-size:20}") int pageSize;
@Value("#{${app.page-size:20} * 2}") int doublePageSize;       // SpEL bọc placeholder
@Value("#{@featureFlags.isEnabled('new-checkout')}") boolean newCheckout;
```

- `@Value` được resolve một lần lúc tạo bean; đổi property runtime không tự cập nhật (cần `@RefreshScope` hoặc đọc từ `Environment`/`@ConfigurationProperties` có cơ chế refresh).

**Câu hỏi nối tiếp:**
- *SpEL trong `@PreAuthorize` có nguy cơ injection không?* → Biểu thức là hằng trong code nên an toàn; nguy hiểm là ghép chuỗi từ input vào biểu thức.

**⚠️ Câu trả lời gây điểm trừ:** Không phân biệt hai cú pháp; không biết rủi ro injection.

**📖 Ôn lại:** [§8.1 SpEL](../01-giao-trinh/07-spring-core-boot.md#p8)

</details>

### Q34. 🟡 `@EventListener` khác `@TransactionalEventListener` thế nào? Những bẫy hay gặp?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `@EventListener` mặc định **đồng bộ, cùng thread, cùng transaction** với publisher: exception trong listener lan ngược về publisher và có thể rollback transaction; listener chạy *trước commit*. `@TransactionalEventListener` gắn với phase của transaction: `AFTER_COMMIT` (mặc định), `BEFORE_COMMIT`, `AFTER_ROLLBACK`, `AFTER_COMPLETION`. Bẫy: (1) publish **ngoài** transaction → listener không được gọi (trừ `fallbackExecution = true`); (2) trong `AFTER_COMMIT`, muốn ghi DB phải `@Transactional(propagation = REQUIRES_NEW)` — Spring 6.1+ báo lỗi nếu dùng `@Transactional` mặc định `REQUIRED` trên listener không async; (3) event in-memory **không bền** (Q35).

**Giải thích chi tiết:**

```java
@TransactionalEventListener                    // AFTER_COMMIT: chỉ gửi khi tx đã commit
void sendEmail(OrderPlacedEvent e) { ... }

@Async
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
void pushToAnalytics(OrderPlacedEvent e) { ... }   // thread khác sau commit
```

- Event là record bất kỳ (Spring 4.2+ không cần extends `ApplicationEvent`); thứ tự listener bằng `@Order`; listener có thể trả event mới để publish tiếp.
- Async toàn cục bằng `SimpleApplicationEventMulticaster` + executor ảnh hưởng **mọi** listener (kể cả event nội bộ của framework) — thường không nên.

**Câu hỏi nối tiếp:**
- *Test "rollback thì không gửi email"?* → `@SpringBootTest`, cho save ném exception (email trùng), assert listener không được gọi.

**⚠️ Câu trả lời gây điểm trừ:** "Event Spring là async."

**📖 Ôn lại:** [§8.2 Application Events](../01-giao-trinh/07-spring-core-boot.md#p8)

</details>

### Q35. 🔴 [Tình huống] Đơn hàng được lưu thành công nhưng thỉnh thoảng khách không nhận được email xác nhận, và Kafka thiếu event `OrderPlaced`. Code dùng `@TransactionalEventListener(AFTER_COMMIT)` để gửi. Nguyên nhân và giải pháp?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Listener `AFTER_COMMIT` chạy *trong process* sau khi commit: nếu pod bị kill (rolling deploy, OOM), executor `@Async` bị đóng khi task còn trong queue, hoặc gửi Kafka lỗi sau commit → event **mất** vì không còn ở đâu để thử lại (dual-write problem). Giải pháp: **Transactional Outbox** — ghi event vào bảng `outbox` **trong cùng transaction** với đơn hàng; relay (poller `SELECT ... FOR UPDATE SKIP LOCKED` theo batch, hoặc CDC Debezium) gửi Kafka rồi đánh dấu `SENT`, retry khi lỗi; consumer **idempotent** vì at-least-once. Spring Modulith có sẵn Event Publication Registry.

**Giải thích chi tiết:**
- Bổ sung vận hành: graceful shutdown (`setWaitForTasksToCompleteOnShutdown(true)` + await termination) giảm mất task nhưng **không** thay được outbox.
- Poller nhiều instance: `SKIP LOCKED` chia batch không trùng; hoặc ShedLock để một instance chạy.
- Theo dõi: metric số event `NEW` tồn đọng, tuổi event cũ nhất → alert.
- Thứ tự: key Kafka = `orderId` để event của cùng đơn vào cùng partition.

**Câu hỏi nối tiếp:**
- *Gửi Kafka trong transaction DB (trước commit) thì sao?* → Có thể phát event cho dữ liệu sau đó rollback; Kafka transaction không phối hợp với transaction DB (không có 2PC thực tế).
- *Outbox dọn dẹp thế nào?* → Xóa/partition theo thời gian sau khi `SENT` và quá thời gian giữ.

**⚠️ Câu trả lời gây điểm trừ:** "Thêm retry trong listener là đủ"; không biết khái niệm outbox.

**📖 Ôn lại:** [§8.2 Góc nhìn Senior (Outbox) & Bài 8.3](../01-giao-trinh/07-spring-core-boot.md#p8)

</details>

---

<a id="nhom-i"></a>
## I. Auto-configuration & starters

### Q36. 🟢 Spring Boot giải quyết vấn đề gì? Starter và auto-configuration là gì?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Boot cung cấp: **starter** — dependency descriptor gom nhóm thư viện tương thích (`spring-boot-starter-web` = spring-webmvc + Jackson + Tomcat embedded...), version quản lý bởi BOM `spring-boot-dependencies`; **auto-configuration** — tự tạo bean hợp lý dựa trên **classpath + property + bean đã có**, theo nguyên tắc "opinionated defaults, back off when you define your own"; cùng embedded server, executable jar, Actuator, externalized config.

**Giải thích chi tiết:**
- "Magic" của Boot chỉ là `@Conditional` + thứ tự xử lý: ví dụ `DataSource` được tạo khi có class `DataSource` trên classpath, có `spring.datasource.url`, và chưa có bean `DataSource` do user định nghĩa (`@ConditionalOnMissingBean`).
- App web "trống" có hàng trăm bean — gần như tất cả từ auto-config.

**Câu hỏi nối tiếp:**
- *Làm sao xem auto-config nào đã chạy?* → `--debug` (Condition Evaluation Report), `/actuator/conditions`.

**⚠️ Câu trả lời gây điểm trừ:** "Boot là framework khác Spring"; không giải thích được auto-config dựa trên điều kiện.

**📖 Ôn lại:** [§9.1 Spring Boot giải quyết gì](../01-giao-trinh/07-spring-core-boot.md#p9)

</details>

### Q37. 🟡 Mô tả chi tiết cơ chế auto-configuration từ `@SpringBootApplication` tới khi bean được tạo.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `@SpringBootApplication` chứa `@EnableAutoConfiguration` → `@Import(AutoConfigurationImportSelector.class)`. Selector này là **`DeferredImportSelector`** — chạy **sau** khi mọi `@Configuration` của user đã được xử lý — đọc danh sách class từ `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` (Boot 2.7+/3.x; Boot ≤ 2.6 dùng `spring.factories`), lọc nhanh bằng `AutoConfigurationImportFilter` dùng metadata precomputed (không cần nạp class), áp `exclude`, sắp theo `@AutoConfiguration(before/after)`/`@AutoConfigureOrder`. Mỗi auto-config là `@Configuration(proxyBeanMethods=false)` với các `@ConditionalOn*` được `ConfigurationClassPostProcessor` đánh giá.

**Giải thích chi tiết:**

```java
@AutoConfiguration(before = SqlInitializationAutoConfiguration.class)
@ConditionalOnClass({ DataSource.class, EmbeddedDatabaseType.class })
@EnableConfigurationProperties(DataSourceProperties.class)
public class MyDataSourceAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(DataSource.class)        // back off nếu user tự định nghĩa
    DataSource dataSource(DataSourceProperties props) { return props.initializeDataSourceBuilder().build(); }
}
```

- `spring.factories` **vẫn** dùng cho các extension point khác: `ApplicationContextInitializer`, `ApplicationListener`, `EnvironmentPostProcessor`, `FailureAnalyzer`.

**Câu hỏi nối tiếp:**
- *Vì sao phải "deferred"?* → Để `@ConditionalOnMissingBean` thấy được bean của user trước khi quyết định back off.

**⚠️ Câu trả lời gây điểm trừ:** Chỉ nói "Boot quét classpath rồi tự tạo bean".

**📖 Ôn lại:** [§9.2 Cơ chế auto-configuration](../01-giao-trinh/07-spring-core-boot.md#p9)

</details>

### Q38. 🟡 [Tình huống] Cần `ObjectMapper` dùng `snake_case` và không fail với property lạ. Bạn cấu hình thế nào? Vì sao không nên tự khai báo `@Bean ObjectMapper`?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Ưu tiên theo thứ tự: (1) property `spring.jackson.property-naming-strategy=SNAKE_CASE`, `spring.jackson.deserialization.fail-on-unknown-properties=false`; (2) bean `Jackson2ObjectMapperBuilderCustomizer` — **cộng dồn** với cấu hình mặc định của Boot. Tự định nghĩa `@Bean ObjectMapper` khiến `JacksonAutoConfiguration` **back off** → mất toàn bộ customization của Boot (module `JavaTimeModule`, các property `spring.jackson.*`, customizer của library khác) — bug hay gặp: ngày giờ bị serialize thành mảng số hoặc lỗi `Java 8 date/time type not supported`.

**Giải thích chi tiết:**

```java
@Bean
Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
    return b -> b.propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                 .featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
}
```

- Lưu ý Boot đã tắt sẵn `FAIL_ON_UNKNOWN_PROPERTIES` và `WRITE_DATES_AS_TIMESTAMPS` — kiểm chứng trước khi thêm.
- Nguyên tắc chung: tìm **customizer**/property trước khi thay thế cả bean auto-config.
- Không `new ObjectMapper()` trong code — lệch cấu hình với converter của MVC.

**Câu hỏi nối tiếp:**
- *Cần hai cấu hình JSON khác nhau (API public và nội bộ)?* → Bean `ObjectMapper` thứ hai có qualifier, tạo từ `Jackson2ObjectMapperBuilder` do Boot inject (giữ module mặc định), không đánh `@Primary`.

**⚠️ Câu trả lời gây điểm trừ:** Khai báo `@Bean @Primary ObjectMapper` mới hoàn toàn mà không biết hệ quả.

**📖 Ôn lại:** [§9 Bài 9.2 Override bean auto-config](../01-giao-trinh/07-spring-core-boot.md#p9)

</details>

### Q39. 🟡 [Tình huống] Thêm `spring-boot-starter-data-jpa` vào một module không dùng DB, app không khởi động: `Failed to configure a DataSource: 'url' attribute is not specified`. Bạn xử lý và debug auto-config thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Đây là auto-config hoạt động **đúng**: thấy `DataSource` trên classpath nên cố tạo DataSource. Sửa gốc: **bỏ dependency thừa** (hoặc đưa vào module thật sự dùng). Exclude (`@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)`) chỉ là biện pháp cuối vì mất cả các bean phụ đi kèm. Debug: `--debug` → Condition Evaluation Report (positive/negative matches, exclusions), `/actuator/conditions`, `/actuator/beans`, `mvn dependency:tree` để tìm ai kéo starter vào (transitive).

**Giải thích chi tiết:**
- Khi bean của bạn "không thay thế được" bean auto-config, kiểm tra: kiểu khai báo của `@Bean` có khớp kiểu trong `@ConditionalOnMissingBean` không; bean có trong vùng scan không; có hai auto-config cùng tạo bean không.
- `FailureAnalyzer` là thứ in khối "APPLICATION FAILED TO START — Description/Action".

**Câu hỏi nối tiếp:**
- *Test module không có DB?* → `@SpringBootTest` với `spring.autoconfigure.exclude` trong test profile, hoặc test slice phù hợp (`@WebMvcTest`).

**⚠️ Câu trả lời gây điểm trừ:** Thêm H2 hoặc exclude bừa để hết lỗi mà không hiểu nguyên nhân.

**📖 Ôn lại:** [§9.3 Debug auto-configuration](../01-giao-trinh/07-spring-core-boot.md#p9)

</details>

### Q40. 🔴 [Tình huống] Sau khi nâng Boot 2.5 → 3.x, bean từ một library nội bộ biến mất, không có lỗi gì, condition report cũng không liệt kê class đó. Vì sao?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Library đăng ký auto-config qua key `org.springframework.boot.autoconfigure.EnableAutoConfiguration` trong **`META-INF/spring.factories`** — **Boot 3.0 đã bỏ** cách đăng ký này (Boot 2.7 hỗ trợ cả hai và cảnh báo deprecation). Class không bao giờ được đọc nên không xuất hiện trong condition report (report chỉ liệt kê class được nạp làm candidate). Sửa library: tạo `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` chứa FQCN, đổi `@Configuration` thành `@AutoConfiguration`, phát hành version mới.

**Giải thích chi tiết:**
- Khác biệt "im lặng" là điều nguy hiểm: không lỗi startup, chỉ thiếu hành vi (ví dụ filter audit không chạy) → cần test tích hợp kiểm tra bean của library tồn tại (`ApplicationContextRunner` `hasSingleBean`).
- Cùng đợt migration: kiểm tra library có hỗ trợ Jakarta không (`javax.servlet.Filter` → `ClassNotFoundException`), `spring-boot-properties-migrator` để phát hiện property đổi tên.

**Câu hỏi nối tiếp:**
- *Nếu library của bên thứ ba chưa cập nhật?* → Tạm `@Import(TheirConfiguration.class)` trong app (nhưng mất thứ tự xử lý deferred → `@ConditionalOnMissingBean` có thể sai) hoặc fork/thay library.

**⚠️ Câu trả lời gây điểm trừ:** Không biết thay đổi `spring.factories` → `AutoConfiguration.imports`.

**📖 Ôn lại:** [§9.2 & Bài 9.3](../01-giao-trinh/07-spring-core-boot.md#p9)

</details>

---

<a id="nhom-j"></a>
## J. Externalized configuration & profiles

### Q41. 🟢 Cùng một property được đặt ở `application.yml`, biến môi trường và command-line. Giá trị nào thắng? Override `spring.datasource.url` trên Kubernetes thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Command-line args > Java system properties (`-D`) > biến môi trường OS > config data (`application.yml`). Trong config data: file **ngoài jar** thắng file trong jar, **profile-specific** (`application-prod.yml`) thắng file chung. Trên Kubernetes override bằng env var nhờ **relaxed binding**: `SPRING_DATASOURCE_URL` (thay `.` bằng `_`, bỏ `-`, viết hoa); secret mount bằng `spring.config.import=configtree:/etc/secrets/`.

**Giải thích chi tiết:**
- Thứ tự đầy đủ (thấp → cao, rút gọn): default properties → `@PropertySource` → config data → `random.*` → OS env → system properties → JNDI → ServletContext/ServletConfig params → `SPRING_APPLICATION_JSON` → command line → thuộc tính test (`@SpringBootTest(properties)`, `@DynamicPropertySource`, `@TestPropertySource`).
- `@PropertySource` được thêm khi context refresh → quá muộn cho `logging.*`, `spring.main.*`.
- `.properties` và `.yml` cùng vị trí → `.properties` thắng.
- Kiểm tra nguồn đang thắng: `/actuator/env/<property>`.

**Câu hỏi nối tiếp:**
- *Env var cho list/map?* → `APP_SERVERS_0_HOST=...` (index), hoặc `SPRING_APPLICATION_JSON`.

**⚠️ Câu trả lời gây điểm trừ:** "application.yml luôn thắng"; không biết relaxed binding.

**📖 Ôn lại:** [§10.1 Thứ tự ưu tiên PropertySource](../01-giao-trinh/07-spring-core-boot.md#p10)

</details>

### Q42. 🟡 `@ConfigurationProperties` hay `@Value`? Viết một ví dụ chuẩn Boot 3.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Dùng **`@ConfigurationProperties`** cho nhóm cấu hình có cấu trúc: type-safe (record/POJO), relaxed binding đầy đủ, **validation `@Validated` fail-fast lúc startup**, metadata cho IDE (`spring-boot-configuration-processor`), immutable bằng constructor binding, hiểu `Duration`/`DataSize` có đơn vị. `@Value` chỉ hợp cho giá trị lẻ hoặc khi cần SpEL.

**Giải thích chi tiết:**

```java
@ConfigurationProperties(prefix = "app.payment")
@Validated
public record PaymentProperties(
        @NotBlank String provider,
        @NotNull URI baseUrl,
        @DefaultValue("5s") Duration connectTimeout,
        @DefaultValue("3") @Min(0) @Max(10) int maxRetries,
        Map<String, String> headers) {}

@SpringBootApplication
@ConfigurationPropertiesScan
public class ShopApplication { }
```

- Boot 3: constructor binding là **mặc định** khi có đúng một constructor có tham số (Boot 2.2–2.7 cần `@ConstructorBinding`; Boot 3 chuyển annotation này sang package `...context.properties.bind`, chỉ cần khi nhiều constructor).
- Cấu hình sai (`max-retries: 70`) → app không khởi động, thông báo rõ field và ràng buộc.

**Câu hỏi nối tiếp:**
- *Refresh cấu hình lúc runtime?* → Spring Cloud `@RefreshScope`/`/actuator/refresh`, hoặc tự thiết kế reload; record immutable cần tạo bean mới.

**⚠️ Câu trả lời gây điểm trừ:** Rải hàng chục `@Value` khắp code và không validate.

**📖 Ôn lại:** [§10.3 @ConfigurationProperties vs @Value](../01-giao-trinh/07-spring-core-boot.md#p10)

</details>

### Q43. 🔴 Team có `application-dev.yml`, `-staging.yml`, `-prod-vn.yml`, `-prod-sg.yml`... và password DB trong file prod. Bạn đánh giá và đề xuất gì?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Hai vấn đề: (1) **secret trong git** — lộ cho mọi người có quyền đọc repo, nằm mãi trong lịch sử → phải **rotate ngay**, chuyển sang Vault/Secret Manager/Kubernetes Secret (đọc bằng `configtree`) hoặc env var được inject lúc deploy; (2) mã hóa **từng môi trường** thành profile → configuration drift, khó biết cấu hình thật đang chạy. Theo 12-factor: build **một** artifact, khác biệt môi trường đưa vào qua env var/config server; profile dùng cho **đặc tính** (`kafka`, `local-mock`, `cloud`), có thể gom bằng `spring.profiles.group`.

**Giải thích chi tiết:**
- Boot 2.4+: `spring.config.activate.on-profile` thay `spring.profiles` cũ; **không** được đặt `spring.profiles.active`/`include` trong file profile-specific (báo lỗi).
- Actuator `/env`, `/configprops`: Boot 3 mặc định che mọi giá trị (`show-values: never`) — đừng bật `always` trên production.
- Thêm scanning secret trong CI (gitleaks, GitHub secret scanning).

```yaml
spring:
  config:
    import: "optional:configtree:/etc/secrets/"   # file /etc/secrets/db/password → property db.password
  datasource:
    password: ${db.password}
```

**Câu hỏi nối tiếp:**
- *Xóa secret khỏi lịch sử git có đủ không?* → Không; coi như đã lộ, phải rotate; xóa lịch sử chỉ giảm lan rộng thêm.

**⚠️ Câu trả lời gây điểm trừ:** "Mã hóa password bằng Jasypt trong file là đủ" mà không nói về key management; không nhắc rotate.

**📖 Ôn lại:** [§10.2 Profiles & Góc nhìn Senior](../01-giao-trinh/07-spring-core-boot.md#p10)

</details>

### Q44. 🟡 [Đọc code] Hai dòng cấu hình sau có vấn đề gì?

```java
@Value("${app.partner.timeout:}") String timeout;      // (1)
@Value("${app.partner.read-timeout}") long readTimeout; // (2) application.yml: read-timeout: 5000
// ...
client.setReadTimeout(Duration.ofSeconds(readTimeout));
```

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** (1) Default **rỗng** che giấu lỗi cấu hình: thiếu property app vẫn khởi động, lỗi chỉ lộ ra khi dùng (parse chuỗi rỗng, hoặc tệ hơn là timeout không được set → vô hạn). Thiếu property bắt buộc thì **nên** fail lúc startup. (2) Đơn vị mơ hồ: `5000` được hiểu là ms trong YAML nhưng code dùng `Duration.ofSeconds` → timeout **5000 giây** (~83 phút). Dùng `@ConfigurationProperties` với kiểu `Duration` và giá trị có đơn vị (`5s`, `500ms`) — mặc định không có đơn vị là ms, hoặc chỉ định bằng `@DurationUnit`.

**Giải thích chi tiết:**
- Bug timeout sai đơn vị là nguyên nhân thật của nhiều sự cố cạn thread pool (Module 08 — HTTP client timeout).
- Validation `@NotNull` + `@DurationMin`/kiểm tra trong compact constructor của record để chặn giá trị phi lý.

**Câu hỏi nối tiếp:**
- *`@Value` với kiểu `Duration` có convert "5s" được không?* → Có (ConversionService của Boot hỗ trợ), nhưng vẫn kém hơn `@ConfigurationProperties` về validation/metadata.

**⚠️ Câu trả lời gây điểm trừ:** Không thấy bug đơn vị.

**📖 Ôn lại:** [§10.3 Lỗi thường gặp](../01-giao-trinh/07-spring-core-boot.md#p10)

</details>

---

<a id="nhom-k"></a>
## K. `SpringApplication.run` & custom starter

### Q45. 🟡 Mô tả các bước chính của `SpringApplication.run()`. Embedded Tomcat được tạo và bắt đầu nhận request ở bước nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Khởi tạo `SpringApplication`: suy ra `WebApplicationType` (SERVLET/REACTIVE/NONE) từ classpath, nạp initializer/listener từ `spring.factories`. `run()`: `ApplicationStartingEvent` → **prepare Environment** (`EnvironmentPostProcessor`, `ConfigDataEnvironmentPostProcessor` nạp `application.yml`, profile; logging khởi tạo ở `ApplicationEnvironmentPreparedEvent`) → banner → **create context** → prepare context (initializer, đăng ký class main) → **`refresh()`**: invoke BFPP (component scan, auto-config) → register BPP → **`onRefresh()` tạo embedded Tomcat** → pre-instantiate singletons → `finishRefresh()` start `SmartLifecycle` (**web server bắt đầu nhận request**), `ContextRefreshedEvent` → `ApplicationStartedEvent` (liveness CORRECT) → chạy **`ApplicationRunner`/`CommandLineRunner`** → `ApplicationReadyEvent` (readiness ACCEPTING_TRAFFIC). Lỗi → `ApplicationFailedEvent`, `FailureAnalyzer`.

**Giải thích chi tiết:**
- Listener của `ApplicationStartingEvent` phải đăng ký qua `SpringApplication.addListeners` hoặc `spring.factories` — lúc đó context chưa tồn tại nên `@Component` vô dụng.
- `CommandLineRunner` chạy lâu (migrate dữ liệu) làm trễ readiness → chỉnh `startupProbe` trên K8s.
- Đo: `BufferingApplicationStartup` + `/actuator/startup`.

**Câu hỏi nối tiếp:**
- *Server đã nhận request trước khi runner chạy xong thì sao?* → Socket có thể đã mở, nhưng readiness chưa `ACCEPTING_TRAFFIC` nên K8s chưa route traffic tới pod (nếu probe cấu hình đúng).

**⚠️ Câu trả lời gây điểm trừ:** Chỉ nói "tạo context rồi chạy Tomcat".

**📖 Ôn lại:** [§11.1 SpringApplication.run](../01-giao-trinh/07-spring-core-boot.md#p11)

</details>

### Q46. 🔴 Bạn viết một starter nội bộ cho 30 service (audit, correlation id, executor chuẩn). Những quy ước và cách test nào bắt buộc?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Quy ước: tên `acme-spring-boot-starter` (tiền tố `spring-boot-starter-*` dành cho starter chính thức); có thể tách module `-autoconfigure` và `-starter`; đăng ký qua `AutoConfiguration.imports`, class dùng `@AutoConfiguration`; **mọi bean `@ConditionalOnMissingBean`** để service override; `@ConditionalOnClass` cho dependency optional (khai báo `<optional>true</optional>`); `@ConditionalOnProperty(prefix="acme.audit", name="enabled", matchIfMissing=true)` để tắt được; property prefix riêng (`acme.*`) bằng `@ConfigurationProperties` có validation; có `spring-boot-configuration-processor` (metadata) và `spring-boot-autoconfigure-processor`. Test bằng **`ApplicationContextRunner`**: default, user override, disabled, thiếu class, property sai, web vs non-web.

**Giải thích chi tiết:**

```java
private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(AuditAutoConfiguration.class));

@Test void backsOffWhenUserDefinesClient() {
    runner.withBean(AuditClient.class, () -> new AuditClient("custom"))
          .run(ctx -> assertThat(ctx.getBean(AuditClient.class).topic()).isEqualTo("custom"));
}
@Test void backsOffWhenClassMissing() {
    runner.withClassLoader(new FilteredClassLoader(AuditClient.class))
          .run(ctx -> assertThat(ctx).doesNotHaveBean(AuditClient.class));
}
```

- Sai lầm: đặt auto-config trong package bị app component scan (thành user config, sai thứ tự `@ConditionalOnMissingBean`); `@ComponentScan` bên trong auto-config (kéo bean ngoài ý muốn).
- Vận hành: semantic versioning, changelog, `FailureAnalyzer` thân thiện cho lỗi cấu hình, không kéo dependency nặng transitive.

**Câu hỏi nối tiếp:**
- *Đăng ký servlet filter từ starter?* → `FilterRegistrationBean` để set order/URL pattern, `@ConditionalOnWebApplication(type = SERVLET)`.

**⚠️ Câu trả lời gây điểm trừ:** Starter là một `@Configuration` + `@ComponentScan` được service import thủ công.

**📖 Ôn lại:** [§11.2 Viết custom starter](../01-giao-trinh/07-spring-core-boot.md#p11)

</details>

---

<a id="nhom-l"></a>
## L. Actuator, metrics, logging

### Q47. 🟢 Actuator cung cấp gì? Những endpoint nào nguy hiểm nếu lộ ra Internet và bạn bảo vệ thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Endpoint vận hành: `health`, `info`, `metrics`, `prometheus`, `env`, `configprops`, `beans`, `conditions`, `loggers` (đổi log level runtime), `threaddump`, `heapdump`, `mappings`, `startup`... Mặc định qua HTTP chỉ expose `health`. Nguy hiểm: **`heapdump`** (chứa toàn bộ bộ nhớ — password, token, PII), `env`/`configprops` (secret), `threaddump`, `beans`, `loggers` (ghi được). Bảo vệ: expose tối thiểu, chạy trên **`management.server.port` riêng** không public qua ingress, `SecurityFilterChain` riêng cho `EndpointRequest.toAnyEndpoint()` (health/info public, còn lại role `OPS`), `show-details: when-authorized`.

**Giải thích chi tiết:**

```yaml
management:
  server: { port: 8081 }
  endpoints.web.exposure.include: health,info,prometheus
  endpoint.health.show-details: when-authorized
```

- Boot 3 sanitize mọi giá trị trong `/env`, `/configprops` mặc định.
- Tách cổng còn giúp health check không bị nghẽn khi thread pool cổng chính cạn (tùy server, cổng quản trị có thể dùng connector/thread riêng).

**Câu hỏi nối tiếp:**
- *Endpoint `shutdown`?* → Mặc định disabled; không bao giờ expose public.

**⚠️ Câu trả lời gây điểm trừ:** `exposure.include: "*"` trên production.

**📖 Ôn lại:** [§12.1 Actuator](../01-giao-trinh/07-spring-core-boot.md#p12)

</details>

### Q48. 🔴 Liveness và readiness probe khác nhau thế nào? Vì sao **không** đưa health check của DB vào liveness?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **Liveness** trả lời "process có còn sống/khỏe không" — fail thì Kubernetes **restart** pod. **Readiness** trả lời "có nhận traffic được không" — fail thì pod bị **rút khỏi Service** (không restart). Nếu đưa DB vào liveness: DB chập chờn vài giây → **mọi pod** fail liveness cùng lúc → K8s restart hàng loạt → khi khởi động lại tất cả cùng mở connection, warm-up → "thundering herd" đánh sập DB hẳn, biến sự cố nhỏ thành outage. Dependency ngoài chỉ nên ảnh hưởng **readiness** (hoặc không ảnh hưởng, nếu app có chiến lược degrade).

**Giải thích chi tiết:**

```yaml
management.endpoint.health:
  probes.enabled: true                      # tự bật khi chạy trên Kubernetes
  group.readiness.include: readinessState, db, redis
# liveness chỉ gồm livenessState (trạng thái nội tại của app)
```

- Chủ động đổi trạng thái: `AvailabilityChangeEvent.publish(publisher, this, ReadinessState.REFUSING_TRAFFIC)` (ví dụ khi warm-up hoặc bảo trì).
- Health indicator tùy chỉnh cho đối tác: chỉ thuộc group readiness; đặt timeout ngắn và cache kết quả để probe không tự gây tải.
- Ngay cả readiness: nếu DB down làm **mọi** pod not-ready → Service không còn endpoint → client nhận 503 ở ingress; cân nhắc có nên để app tự trả lỗi có ý nghĩa/degrade hơn.

**Câu hỏi nối tiếp:**
- *`startupProbe` để làm gì?* → Cho app khởi động chậm có thời gian, tắt liveness/readiness cho tới khi startup xong.

**⚠️ Câu trả lời gây điểm trừ:** "Liveness nên kiểm tra mọi dependency cho chắc."

**📖 Ôn lại:** [§12.1 Health & probes, Góc nhìn Senior](../01-giao-trinh/07-spring-core-boot.md#p12)

</details>

### Q49. 🟡 Metrics với Micrometer: "cardinality explosion" là gì? Observability trong Boot 3 thay đổi gì so với Boot 2?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Micrometer là "SLF4J cho metrics" (`Counter`, `Timer`, `Gauge`, `DistributionSummary`; backend Prometheus/OTLP/Datadog). **Cardinality explosion**: gắn tag có giá trị không giới hạn (`userId`, `orderId`, URL thô có id) → mỗi giá trị là một time series → Prometheus OOM, chi phí tăng vọt. Tag phải thuộc tập nhỏ hữu hạn (Boot dùng URI *template* `/orders/{id}` cho `http.server.requests`). Boot 3: **Micrometer Observation API** (`Observation`, `@Observed`) — instrument một lần sinh cả metric lẫn trace; **Micrometer Tracing** (bridge Brave/OpenTelemetry) **thay Spring Cloud Sleuth** (không hỗ trợ Boot 3); trace id/span id tự vào MDC.

**Giải thích chi tiết:**
- `management.tracing.sampling.probability` mặc định 0.1.
- `@Observed` hoạt động qua aspect → cần AOP và bean `ObservedAspect` (bản mới có property tự cấu hình) — self-invocation cũng làm mất observation.
- Context propagation qua `@Async`/Reactor: `ContextPropagatingTaskDecorator`, Micrometer Context Propagation.
- Tạo `RestClient`/`WebClient` từ builder do Boot inject để có `http.client.requests` + propagate trace.

**Câu hỏi nối tiếp:**
- *Percentile tính ở client hay server?* → `publishPercentileHistogram()` gửi histogram để Prometheus tính percentile tổng hợp được nhiều instance; percentile tính sẵn ở client không cộng gộp được.

**⚠️ Câu trả lời gây điểm trừ:** Gắn `userId` làm tag "để tiện tra cứu".

**📖 Ôn lại:** [§12.2–12.3 Metrics & Observability](../01-giao-trinh/07-spring-core-boot.md#p12)

</details>

### Q50. 🟡 Cấu hình logging trong Boot: vì sao dùng `logback-spring.xml` thay `logback.xml`? Những vấn đề hiệu năng và bảo mật của logging?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `logback.xml` được Logback nạp **trước** khi Spring kịp can thiệp; `logback-spring.xml` cho phép dùng `<springProfile>`, `<springProperty>`. Hiệu năng: dùng placeholder `{}` thay string concatenation (concatenation tốn CPU dù level tắt); log đồng bộ ra disk chậm có thể chặn request thread → `AsyncAppender` (đánh đổi: mất log khi crash, `neverBlock` có thể bỏ log khi đầy). Bảo mật: không log PII/token/password, cấu hình masking. Boot 3.4+ có **structured logging** (`logging.structured.format.console=ecs|logstash|gelf`) ra JSON không cần encoder ngoài; đổi level runtime qua `/actuator/loggers`.

**Giải thích chi tiết:**
- Log group: `logging.group.web=...` để bật DEBUG cho cả cụm package.
- Luôn kèm correlation/trace id trong pattern (MDC) để nối log giữa service.
- Đổi sang Log4j2: `spring-boot-starter-log4j2` + exclude `spring-boot-starter-logging`.

**Câu hỏi nối tiếp:**
- *MDC với thread pool?* → Phải copy MDC sang task (TaskDecorator) và dọn trong `finally`, nếu không log của request trước rò sang request sau.

**⚠️ Câu trả lời gây điểm trừ:** `log.debug("user=" + user)` và log nguyên request body có password.

**📖 Ôn lại:** [§12.4 Logging](../01-giao-trinh/07-spring-core-boot.md#p12)

</details>

---

<a id="nhom-m"></a>
## M. `@Async`, `@Scheduled`, thread pool

### Q51. 🔴 [Đọc code] Dưới tải 1.000 task đồng thời, executor sau chạy bao nhiêu thread? Executor mặc định của `@Async` trong Spring Boot có vấn đề gì?

```java
var ex = new ThreadPoolTaskExecutor();
ex.setCorePoolSize(4);
ex.setMaxPoolSize(50);
ex.setQueueCapacity(10_000);
ex.initialize();
```

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Chỉ **4 thread**. `ThreadPoolExecutor.execute`: < core → tạo thread; ≥ core → **đưa vào queue**; chỉ khi queue **đầy** mới tạo thêm thread tới max; vẫn đầy → reject. Queue 10.000 chưa đầy với 1.000 task nên `max=50` vô nghĩa. Executor mặc định của Boot (`applicationTaskExecutor`): core 8, queue **không giới hạn** → thực tế chỉ 8 thread, task dồn vô hạn → latency tăng dần, có thể OOM, mất task khi restart. Spring Framework thuần (không Boot, không có executor bean): `SimpleAsyncTaskExecutor` — **mỗi task một thread mới**, không pool → có thể tạo hàng nghìn thread.

**Giải thích chi tiết:**

```java
@Bean(name = "mailExecutor")
ThreadPoolTaskExecutor mailExecutor() {
    var ex = new ThreadPoolTaskExecutor();
    ex.setCorePoolSize(4); ex.setMaxPoolSize(16);
    ex.setQueueCapacity(500);                                              // bounded
    ex.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy()); // backpressure
    ex.setThreadNamePrefix("mail-");
    ex.setWaitForTasksToCompleteOnShutdown(true); ex.setAwaitTerminationSeconds(20);
    ex.setTaskDecorator(new MdcTaskDecorator());
    return ex;
}
```

- Định cỡ: CPU-bound ≈ số core; I/O-bound ≈ `cores × (1 + wait/compute)` — nhưng giới hạn thật thường là tài nguyên hạ nguồn (DB pool 10 thì 200 thread chỉ tăng tranh chấp).
- Boot 3.2+ với `spring.threads.virtual.enabled=true`: `applicationTaskExecutor` dùng virtual thread.
- Metric executor (`executor.active`, `executor.queued`) và tên thread có nghĩa để đọc thread dump.

**Câu hỏi nối tiếp:**
- *Muốn tăng thread trước khi queue?* → `allowCoreThreadTimeOut` + core = max, hoặc queue nhỏ; hoặc `SynchronousQueue` (queue capacity 0).

**⚠️ Câu trả lời gây điểm trừ:** Trả lời "50 thread".

**📖 Ôn lại:** [§13.1 @Async & Executor mặc định](../01-giao-trinh/07-spring-core-boot.md#p13)

</details>

### Q52. 🟡 Method `@Async` trả `void` ném exception thì sao? Làm sao giữ MDC, `SecurityContext` khi chạy async?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Với `void`, exception **không về caller**; chỉ tới `AsyncUncaughtExceptionHandler` (mặc định chỉ log) — dễ "nuốt" lỗi. Trả `CompletableFuture<T>` để caller xử lý lỗi/kết hợp. Context lưu trong **ThreadLocal** (MDC, `SecurityContextHolder`, request attributes, transaction) không tự sang thread mới → dùng `TaskDecorator` copy MDC (và dọn trong `finally`), `DelegatingSecurityContextAsyncTaskExecutor`/`DelegatingSecurityContextRunnable` cho security, `ContextPropagatingTaskDecorator` cho trace. Transaction **không** được truyền — method async cần transaction riêng.

**Giải thích chi tiết:**

```java
class MdcTaskDecorator implements TaskDecorator {
    public Runnable decorate(Runnable task) {
        Map<String, String> ctx = MDC.getCopyOfContextMap();          // chụp ở thread gọi
        return () -> {
            Map<String, String> previous = MDC.getCopyOfContextMap();
            if (ctx != null) MDC.setContextMap(ctx); else MDC.clear();
            try { task.run(); }
            finally { if (previous != null) MDC.setContextMap(previous); else MDC.clear(); }
        };
    }
}
```

- `ThreadPoolTaskExecutor.setTaskDecorator` nhận **một** decorator → muốn nhiều loại context thì lồng decorator.
- Lỗi khác: self-invocation → chạy đồng bộ; method `private` → không async.

**Câu hỏi nối tiếp:**
- *`MODE_INHERITABLETHREADLOCAL` cho SecurityContext?* → Nguy hiểm với pool: thread tạo một lần giữ context của user đầu tiên rồi tái sử dụng cho user khác (Module 08).

**⚠️ Câu trả lời gây điểm trừ:** "Exception trong `@Async` sẽ ném về controller."

**📖 Ôn lại:** [§13.1 Lỗi thường gặp với @Async](../01-giao-trinh/07-spring-core-boot.md#p13)

</details>

### Q53. 🟡 `@Scheduled` có những bẫy gì khi chạy trên production với 3 pod?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** (1) Scheduler mặc định của Boot có **pool size = 1** → một job chậm/treo chặn **mọi** job khác (tăng `spring.task.scheduling.pool.size` hoặc đẩy việc nặng sang executor riêng); (2) **mỗi pod chạy job một lần** → 3 pod = 3 lần gửi báo cáo/trừ tiền → dùng **ShedLock** (khóa qua DB/Redis), Quartz cluster mode, hoặc tách job ra K8s CronJob/worker duy nhất; (3) `fixedRate` tính từ lúc *bắt đầu* lần trước, `fixedDelay` từ lúc *kết thúc*; cron nhớ đặt `zone`; (4) exception được log và job vẫn chạy lịch kế tiếp — cần metric/alert cho job lỗi.

**Giải thích chi tiết:**

```java
@Scheduled(cron = "${jobs.report.cron:0 0 2 * * *}", zone = "Asia/Ho_Chi_Minh")
@SchedulerLock(name = "nightlyReport", lockAtMostFor = "10m", lockAtLeastFor = "1m")
void nightlyReport() { ... }
```

- `lockAtMostFor` phải lớn hơn thời gian chạy bình thường (pod giữ lock crash → lock tự hết hạn sau giá trị này); `lockAtLeastFor` chống chạy lại khi đồng hồ giữa các node lệch nhẹ.
- Job phải **idempotent** (chạy lại sau lỗi không gây trùng).

**Câu hỏi nối tiếp:**
- *Job xử lý hàng triệu bản ghi?* → Chia batch, checkpoint, `SKIP LOCKED` để nhiều worker cùng xử lý; cân nhắc Spring Batch.

**⚠️ Câu trả lời gây điểm trừ:** Không biết vấn đề nhiều instance.

**📖 Ôn lại:** [§13.2 @Scheduled](../01-giao-trinh/07-spring-core-boot.md#p13)

</details>

---

<a id="nhom-n"></a>
## N. Graceful shutdown, Boot 3, native image, virtual threads

### Q54. 🟡 Cấu hình graceful shutdown cho Spring Boot chạy trên Kubernetes. Vì sao vẫn có lỗi 502 khi rolling deploy?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `server.shutdown=graceful` (mặc định từ **Boot 3.4**; Boot 2.3–3.3 mặc định `immediate`) + `spring.lifecycle.timeout-per-shutdown-phase` (mặc định 30s). Trình tự: SIGTERM → `context.close()` → readiness `REFUSING_TRAFFIC` → `SmartLifecycle.stop()` (web server ngừng nhận, chờ request đang chạy; message listener dừng) → destroy bean (`@PreDestroy`, đóng `DataSource`, executor chờ task). Vẫn 502 vì **race**: K8s gửi SIGTERM *song song* với việc gỡ endpoint khỏi Service/Ingress (lan truyền mất vài giây) → request vẫn được route tới pod đang đóng. Giải pháp: `preStop` hook `sleep 5–10s`, và `terminationGracePeriodSeconds` > preStop + timeout shutdown.

**Giải thích chi tiết:**
- Executor tự tạo cũng phải `waitForTasksToCompleteOnShutdown` + await termination, nếu không task dở dang bị mất.
- Kafka consumer: dừng poll, commit offset của bản ghi đã xử lý; xử lý dở phải idempotent khi pod khác nhận lại.
- Kiểm chứng: endpoint `/slow` sleep 15s, gọi rồi `kill -TERM` — log `Commencing graceful shutdown. Waiting for active requests to complete`.

**Câu hỏi nối tiếp:**
- *Request dài hơn timeout?* → Bị cắt; thiết kế request dài thành async job (202 + polling).

**⚠️ Câu trả lời gây điểm trừ:** "Bật `server.shutdown=graceful` là đủ."

**📖 Ôn lại:** [§14.1 Graceful shutdown](../01-giao-trinh/07-spring-core-boot.md#p14)

</details>

### Q55. 🟢 Những thay đổi lớn nhất khi lên Spring Boot 3 / Spring Framework 6? Bạn migrate thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Java **17+**; **`javax.*` → `jakarta.*`** (servlet, persistence, validation, annotation); auto-config đăng ký qua `AutoConfiguration.imports`; Spring Security 6 **xóa `WebSecurityConfigurerAdapter`** → `SecurityFilterChain` bean, `requestMatchers`; **Micrometer Tracing** thay Sleuth; **AOT/GraalVM native** chính thức; `ProblemDetail` (RFC 9457); HTTP interface `@HttpExchange`, `RestClient` (6.1/Boot 3.2); **bỏ trailing slash matching** (`/users/` không còn khớp `/users`); Hibernate 6. Migrate: lên Boot 2.7 + Java 17 trước, xử lý deprecation; chạy **OpenRewrite** (`UpgradeSpringBoot_3_0`); `spring-boot-properties-migrator` tạm thời để báo property đổi tên; kiểm tra library bên thứ ba hỗ trợ Jakarta; chạy toàn bộ test + contract test.

**Giải thích chi tiết:**
- Boot 2.x OSS đã hết hỗ trợ (2.7 hết OSS support từ cuối 2023) → lý do bảo mật buộc phải migrate.
- Rủi ro "im lặng": `javax.annotation.PostConstruct` bị bỏ qua, auto-config trong `spring.factories` không được nạp, URL trailing slash trả 404, thay đổi query của Hibernate 6.

**Câu hỏi nối tiếp:**
- *Migrate song song nhiều service thế nào?* → Platform starter/BOM nội bộ nâng trước, service theo từng đợt, canary deploy.

**⚠️ Câu trả lời gây điểm trừ:** Chỉ nói "đổi version trong pom".

**📖 Ôn lại:** [§14.2 Thay đổi của Spring Boot 3](../01-giao-trinh/07-spring-core-boot.md#p14)

</details>

### Q56. 🔴 GraalVM native image với Spring Boot 3: lợi ích, đánh đổi, và vì sao `@Profile` hoạt động khác?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Lợi ích: startup **mili giây**, RSS thấp → hợp serverless, scale-to-zero, CLI, sidecar. Đánh đổi: build lâu (vài phút, nhiều RAM), peak throughput có thể thấp hơn JIT (không có tối ưu dựa trên profile runtime nếu không dùng PGO), debug/profiling khó, library dùng reflection động cần **hints**. **Closed-world assumption**: classpath cố định lúc build; Spring **AOT** chạy phân tích context lúc build, sinh code thay reflection — nên `@Profile`, `@ConditionalOnProperty` được đánh giá **lúc build** và "đóng băng": không thể bật profile khác để kích hoạt bean khác lúc runtime (giá trị property thì vẫn đổi được).

**Giải thích chi tiết:**
- Khai báo hints: `RuntimeHintsRegistrar` + `@ImportRuntimeHints`, `@RegisterReflectionForBinding`; test bằng native test.
- Lệnh: `./mvnw -Pnative native:compile` hoặc `spring-boot:build-image -Pnative`.
- Lựa chọn trung gian: **CDS/AppCDS** (Boot 3.3 hỗ trợ dễ), **CRaC** (checkpoint/restore, Boot 3.2+).
- Không hợp: service chạy lâu cần throughput đỉnh, app nhiều reflection/dynamic proxy từ library chưa hỗ trợ.

**Câu hỏi nối tiếp:**
- *Condition dựa trên giờ hệ thống trong native?* → Sai vì được đánh giá lúc build.

**⚠️ Câu trả lời gây điểm trừ:** "Native luôn nhanh hơn, nên chuyển hết."

**📖 Ôn lại:** [§14.3 GraalVM native & AOT](../01-giao-trinh/07-spring-core-boot.md#p14)

</details>

### Q57. 🔴 Bật `spring.threads.virtual.enabled=true` trên Boot 3.2+/Java 21: thay đổi gì, khi nào có lợi, và những bẫy nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Boot cấu hình Tomcat/Jetty xử lý request bằng **virtual thread**, `applicationTaskExecutor` (`@Async`, MVC async) và scheduler dùng virtual thread, một số integration (Kafka/RabbitMQ listener...) cũng chuyển. Lợi ích: code **blocking** (JDBC, `RestClient`) scale tới hàng chục nghìn request đồng thời vì virtual thread được **unmount** khỏi carrier khi block I/O — không cần reactive. Bẫy: (1) **pinning** — trên JDK 21–23, block bên trong `synchronized` (hoặc native frame) ghim carrier thread → mất lợi ích, có thể deadlock khi carrier cạn (JDK 24, JEP 491 xử lý phần lớn trường hợp `synchronized`); (2) **tài nguyên hạ nguồn không tăng**: 10.000 request vẫn tranh 10 connection Hikari → timeout; giới hạn đồng thời bằng semaphore/bulkhead thay cho kích thước thread pool; (3) không pool virtual thread, `ThreadLocal` nặng × triệu thread tốn bộ nhớ; (4) CPU-bound không nhanh hơn; (5) virtual thread là daemon → app chỉ có scheduler cần `spring.main.keep-alive=true`.

**Giải thích chi tiết:**
- Chẩn đoán pinning: `-Djdk.tracePinnedThreads=full` (JDK 21–23) hoặc JFR event `jdk.VirtualThreadPinned`; thay `synchronized` quanh I/O bằng `ReentrantLock` khi còn ở JDK 21.
- Little's Law cho benchmark: Tomcat 200 thread, upstream 200ms → trần ~1.000 req/s; virtual thread bỏ trần này cho tới khi chạm CPU/mạng hoặc pool DB (throughput ≈ số connection / thời gian query).

**Câu hỏi nối tiếp:**
- *Còn lý do dùng WebFlux?* → Streaming/backpressure thực sự, hàng chục nghìn kết nối dài (SSE/WebSocket), stack đã reactive (Module 08).

**⚠️ Câu trả lời gây điểm trừ:** "Bật virtual thread là app nhanh gấp 10 lần" mà không nhắc nút thắt tài nguyên và pinning.

**📖 Ôn lại:** [§14.4 Virtual threads](../01-giao-trinh/07-spring-core-boot.md#p14)

</details>

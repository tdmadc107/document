# Module 07 — Spring Core & Spring Boot

> **Mục tiêu:** sau module này bạn giải thích được cách Spring IoC container tạo, nối (wire) và quản lý vòng đời bean từ `BeanDefinition` đến `destroy`; hiểu Spring AOP dựa trên proxy và vì sao `@Transactional`/`@Cacheable`/`@Async` "không chạy" trong một số tình huống; nắm cơ chế auto-configuration, externalized configuration, Actuator và luồng khởi động của `SpringApplication.run`; tự viết được một custom starter; cấu hình đúng thread pool cho `@Async`/`@Scheduled`, graceful shutdown, virtual threads; debug được lỗi circular dependency, bean không được tạo, cấu hình bị ghi đè sai trên production.
> **Cấp độ:** Cơ bản → Nâng cao (Senior)
> **Thời lượng gợi ý:** 7 ngày (≈ 35–40 giờ)
> **Yêu cầu trước:** Module Java Core/OOP, Module Concurrency (thread pool, `ExecutorService`), Module Design Patterns (Proxy, Factory, Template Method, Observer)
> **Nguồn tham khảo:**
> - Trong kho: [`Ebook IT/Design Pattern.pdf`](../../Ebook%20IT/Design%20Pattern.pdf) — các pattern Proxy, Factory Method, Singleton, Observer, Template Method (nền tảng của IoC container, AOP và event)
> - Trong kho: [`Ebook IT/Clean Code.pdf`](../../Ebook%20IT/Clean%20Code.pdf) — chương "Systems" (tách construction khỏi use, Dependency Injection, cross-cutting concerns & AOP)
> - Ngoài: Spring Framework Reference — Core Technologies: https://docs.spring.io/spring-framework/reference/core.html (IoC container, AOP, SpEL, events)
> - Ngoài: Spring Boot Reference: https://docs.spring.io/spring-boot/reference/ (auto-configuration, externalized configuration, Actuator, logging, task execution, graceful shutdown, GraalVM native)
> - Ngoài: Spring Boot 3.0 Migration Guide: https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.0-Migration-Guide và Release Notes 2.6 / 3.2 (circular references, virtual threads)
> - Ngoài: Micrometer docs: https://docs.micrometer.io/micrometer/reference/observation.html
> - Sách: Craig Walls — *Spring in Action* (6th ed., Manning) — Part 1 Foundational Spring; Baeldung (https://www.baeldung.com) chỉ dùng làm nguồn tham khảo phụ.

## Mục lục
1. [IoC & Dependency Injection](#p1)
2. [ApplicationContext, BeanDefinition & component scanning](#p2)
3. [@Configuration vs lite mode, @Qualifier/@Primary, conditional bean](#p3)
4. [Bean scopes & tiêm prototype vào singleton](#p4)
5. [Bean lifecycle, BeanPostProcessor & BeanFactoryPostProcessor](#p5)
6. [Circular dependency & cơ chế 3-level cache](#p6)
7. [Spring AOP — proxy, pointcut, advice, self-invocation](#p7)
8. [SpEL & Application Events](#p8)
9. [Spring Boot auto-configuration & starters](#p9)
10. [Externalized configuration, profiles, @ConfigurationProperties](#p10)
11. [Luồng khởi động SpringApplication.run & viết custom starter](#p11)
12. [Actuator, logging & observability](#p12)
13. [@Async, @Scheduled & cấu hình thread pool](#p13)
14. [Graceful shutdown, Spring Boot 3, native image & virtual threads](#p14)
15. [Dự án mini của module](#du-an-mini)
16. [Checklist tự đánh giá](#checklist-tu-danh-gia)

---

<a id="p1"></a>
## 1. IoC & Dependency Injection

### 1.1 Khái niệm
**Inversion of Control (IoC)** là nguyên lý "đảo ngược quyền điều khiển": thay vì object tự `new` các dependency của nó, một bên thứ ba (container) tạo object và đưa dependency vào. **Dependency Injection (DI)** là một cách hiện thực IoC — dependency được "tiêm" qua constructor, setter hoặc field. (Cách khác của IoC là Service Locator — object chủ động hỏi registry; Spring hỗ trợ cả hai nhưng khuyến khích DI.)

Lợi ích cốt lõi (Clean Code, chương *Systems* gọi là "tách construction khỏi use"):
- **Loose coupling:** class phụ thuộc vào abstraction (interface), không biết implementation cụ thể.
- **Testability:** truyền mock/stub qua constructor, không cần container.
- **Quản lý vòng đời tập trung:** singleton, pooling, proxy (transaction, cache, security) được container "bọc" vào trong suốt.

```java
// Không có DI: OrderService tự tạo dependency → khó test, khó thay thế
public class OrderServiceBad {
    private final PaymentGateway gateway = new StripeGateway("sk_live_..."); // hard-coded
}

// Có DI: dependency đến từ bên ngoài
public interface PaymentGateway { void charge(String orderId, long amountCents); }

@Service
public class OrderService {
    private final PaymentGateway gateway;
    private final OrderRepository repository;

    // Spring 4.3+: class chỉ có MỘT constructor thì không cần @Autowired
    public OrderService(PaymentGateway gateway, OrderRepository repository) {
        this.gateway = gateway;
        this.repository = repository;
    }

    public void place(Order order) {
        repository.save(order);
        gateway.charge(order.id(), order.totalCents());
    }
}
```

### 1.2 Ba kiểu injection và vì sao chọn constructor

| Kiểu | Cách viết | Ưu | Nhược |
|---|---|---|---|
| **Constructor** | tham số constructor | field `final` (immutable, thread-safe publication), dependency bắt buộc rõ ràng, test không cần Spring, phát hiện circular dependency sớm, "code smell" khi constructor quá nhiều tham số → nhắc bạn tách class | không giải quyết được vòng tròn constructor (đây là ưu điểm hơn là nhược) |
| **Setter** | `@Autowired void setX(X x)` | dependency **tùy chọn** (optional), có thể re-inject (hiếm) | object có thể ở trạng thái "nửa vời"; không `final` |
| **Field** | `@Autowired private X x;` | ngắn | không `final`, ẩn dependency, phải dùng reflection/Spring để test, dễ phình class, che giấu circular dependency |

Tài liệu Spring khuyến nghị: *"use constructors for mandatory dependencies and setter methods or configuration methods for optional dependencies"*.

```java
@Service
public class ReportService {
    private final ReportRepository repo;           // bắt buộc → constructor
    private Optional<MetricsPublisher> metrics = Optional.empty(); // tùy chọn → setter

    public ReportService(ReportRepository repo) { this.repo = repo; }

    @Autowired(required = false)
    public void setMetrics(MetricsPublisher m) { this.metrics = Optional.of(m); }
}
```

Dependency tùy chọn cũng có thể khai báo ngay trong constructor bằng `ObjectProvider<T>` (`getIfAvailable()`, `getIfUnique()`) hoặc `Optional<T>` — đây là cách gọn nhất trong code hiện đại.

### 1.3 Bên dưới nắp capo: container resolve dependency thế nào?
Khi tạo bean, `AbstractAutowireCapableBeanFactory` chọn constructor (`determineConstructorsFromBeanPostProcessors` → `AutowiredAnnotationBeanPostProcessor`), sau đó với **mỗi tham số** gọi `DefaultListableBeanFactory.resolveDependency(...)`:
1. Tìm tất cả bean candidate có kiểu khớp (kể cả generic: `Repository<User>` khác `Repository<Order>`).
2. Nếu nhiều candidate: lọc theo `@Qualifier` → `@Primary` → `@Priority` → so khớp **tên tham số/field** với tên bean (fallback).
3. Hỗ trợ kiểu đặc biệt: `List<T>`/`Set<T>` (tất cả bean, theo `@Order`), `Map<String, T>` (key là bean name), `Optional<T>`, `ObjectProvider<T>`, `Provider<T>` (jakarta.inject).

> 💡 **Góc nhìn Senior:** Spring 6.1 đã bỏ `LocalVariableTableParameterNameDiscoverer`; việc match theo **tên tham số** chỉ hoạt động khi code được compile với cờ `-parameters`. Spring Boot parent POM/Gradle plugin bật sẵn, nhưng project tự cấu hình compiler (hoặc library build riêng) sẽ gặp lỗi `NoUniqueBeanDefinitionException` "đột nhiên" sau khi nâng cấp. Đừng dựa vào tên tham số — dùng `@Qualifier` tường minh.

> ⚠️ **Lỗi thường gặp:**
> - Dùng `@Autowired` field rồi viết unit test bằng `new Service()` → `NullPointerException`.
> - Dùng Lombok `@RequiredArgsConstructor` nhưng quên `final` → field không được inject.
> - Constructor có 8–10 tham số: đó là tín hiệu vi phạm Single Responsibility, không phải lý do quay về field injection.

### 🛠 Bài tập phần 1

**Bài 1.1 — Refactor field injection (Cơ bản)**
- Đề bài: cho class `NotificationService` dùng 3 field `@Autowired` (`EmailSender`, `SmsSender`, `TemplateEngine`). Chuyển sang constructor injection, field `final`.
- Tiêu chí đạt: viết được JUnit 5 test cho `NotificationService` **không** dùng `@SpringBootTest`, chỉ dùng Mockito; không còn `@Autowired` nào trong class.

**Bài 1.2 — Dependency tùy chọn & nhiều implementation (Trung bình)**
- Đề bài: có interface `DiscountPolicy` với 3 implementation (`SeasonalDiscount`, `VipDiscount`, `CouponDiscount`) đều là `@Component`. Viết `PricingService` áp dụng **tất cả** policy theo thứ tự cố định, và một `AuditLogger` tùy chọn (có thể không tồn tại bean).
- Tiêu chí đạt: thứ tự áp dụng xác định bằng `@Order`; khi xóa `@Component` khỏi `AuditLogger` ứng dụng vẫn khởi động bình thường.

**Bài 1.3 — Tự viết mini IoC container (Nâng cao)**
- Đề bài: viết `MiniContainer` (~150 dòng) nhận danh sách class, tự tìm constructor duy nhất, đệ quy tạo dependency, cache singleton, phát hiện vòng tròn và ném exception có thông điệp dạng `A -> B -> C -> A`.
- Tiêu chí đạt: test với đồ thị 5 class không vòng tròn và 1 đồ thị có vòng tròn; không dùng thư viện ngoài ngoài JDK.

<details>
<summary>Gợi ý lời giải</summary>

- Bài 1.2: inject `List<DiscountPolicy>` (Spring sắp theo `@Order`/`Ordered`) và `ObjectProvider<AuditLogger> auditLogger`, khi dùng gọi `auditLogger.ifAvailable(a -> a.log(...))`.
- Bài 1.3 — khung chính:

```java
public class MiniContainer {
    private final Map<Class<?>, Object> singletons = new HashMap<>();
    private final Set<Class<?>> registered;
    private final Deque<Class<?>> creating = new ArrayDeque<>();

    public MiniContainer(Class<?>... classes) { this.registered = Set.of(classes); }

    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<T> type) {
        Class<?> impl = registered.stream().filter(type::isAssignableFrom).findFirst()
                .orElseThrow(() -> new IllegalStateException("No bean of type " + type));
        Object existing = singletons.get(impl);
        if (existing != null) return (T) existing;
        if (creating.contains(impl)) {
            List<String> path = new ArrayList<>();
            creating.descendingIterator().forEachRemaining(c -> path.add(c.getSimpleName()));
            path.add(impl.getSimpleName());
            throw new IllegalStateException("Circular: " + String.join(" -> ", path));
        }
        creating.push(impl);
        try {
            Constructor<?> ctor = impl.getDeclaredConstructors()[0];
            Object[] args = Arrays.stream(ctor.getParameterTypes()).map(this::getBean).toArray();
            Object bean = ctor.newInstance(args);
            singletons.put(impl, bean);
            return (T) bean;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        } finally {
            creating.pop();
        }
    }
}
```
`creating` chính là ý tưởng của `singletonsCurrentlyInCreation` trong `DefaultSingletonBeanRegistry`.
</details>

---

<a id="p2"></a>
## 2. ApplicationContext, BeanDefinition & component scanning

### 2.1 BeanFactory vs ApplicationContext
- **`BeanFactory`**: interface gốc, chỉ lo tạo/lấy bean (`getBean`), mặc định **lazy** (bean tạo khi được yêu cầu). Implementation trung tâm: `DefaultListableBeanFactory`.
- **`ApplicationContext`**: extends `BeanFactory` và thêm:
  - Tự động phát hiện & đăng ký `BeanPostProcessor`, `BeanFactoryPostProcessor` (với `BeanFactory` thuần phải đăng ký tay — đây là lý do `@Autowired`, `@PostConstruct`, `@Transactional` *không chạy* nếu dùng `BeanFactory` trần).
  - **Pre-instantiate** tất cả singleton không lazy khi `refresh()` → lỗi cấu hình lộ ra lúc startup, không phải lúc request đầu tiên.
  - `ApplicationEventPublisher` (events), `MessageSource` (i18n), `ResourceLoader` (`classpath:`, `file:`), `Environment` (profiles, properties).
- Implementation phổ biến: `AnnotationConfigApplicationContext`, `AnnotationConfigServletWebServerApplicationContext` (Spring Boot web MVC), `AnnotationConfigReactiveWebServerApplicationContext` (WebFlux).

```java
public class PlainSpringDemo {
    public static void main(String[] args) {
        try (var ctx = new AnnotationConfigApplicationContext(AppConfig.class)) {
            OrderService svc = ctx.getBean(OrderService.class);
            System.out.println(svc);
            System.out.println(Arrays.toString(ctx.getBeanDefinitionNames()));
        } // close() → gọi destroy callback của singleton
    }
}

@Configuration
@ComponentScan("com.example.shop")
class AppConfig { }
```

### 2.2 BeanDefinition — "bản thiết kế" của bean
Container **không** làm việc trực tiếp với class mà với `BeanDefinition`: class name, scope, lazy-init, depends-on, constructor args, property values, init/destroy method, `primary`, factory bean/method (cho `@Bean`), role... Các nguồn tạo BeanDefinition:
- Component scanning (`ClassPathBeanDefinitionScanner` quét `@Component` và stereotype `@Service`, `@Repository`, `@Controller`, `@Configuration`).
- `@Bean` method (được `ConfigurationClassPostProcessor` phân tích).
- `@Import`, `ImportSelector`, `ImportBeanDefinitionRegistrar` (cách `@EnableJpaRepositories`, `@EnableFeignClients` đăng ký bean động).
- XML (legacy), hoặc đăng ký programmatic: `GenericApplicationContext.registerBean(...)`.

Hai pha tách biệt: **(1) đăng ký BeanDefinition** → **(2) khởi tạo bean**. Giữa hai pha, `BeanFactoryPostProcessor` có thể sửa BeanDefinition (Phần 5).

### 2.3 Component scanning
- `@SpringBootApplication` = `@SpringBootConfiguration` + `@EnableAutoConfiguration` + `@ComponentScan` (base package = package của class main). Class nằm **ngoài** package này sẽ không được quét — lỗi kinh điển "bean not found".
- Bean name mặc định: tên class với chữ đầu viết thường (`OrderService` → `orderService`; ngoại lệ: `URLParser` giữ nguyên vì 2 ký tự đầu viết hoa — theo `Introspector.decapitalize`).
- `@Repository` còn bật **exception translation** (`PersistenceExceptionTranslationPostProcessor`) → chuyển exception của JPA/JDBC sang `DataAccessException`.
- Filter: `@ComponentScan(includeFilters=..., excludeFilters=...)`.

> 💡 **Góc nhìn Senior:** Thời gian startup của ứng dụng lớn bị ảnh hưởng bởi scanning và số bean. Các kỹ thuật: thu hẹp base package, `spring.main.lazy-initialization=true` (đánh đổi: lỗi cấu hình lộ ra muộn, request đầu chậm — chỉ hợp cho dev/test hoặc serverless), AOT/native (Phần 14). `spring-context-indexer` đã bị deprecate từ Spring 6.1 — đừng đề xuất nó như giải pháp mới.

> ⚠️ **Lỗi thường gặp:** đặt class `@SpringBootApplication` trong package con (`com.acme.app.web`) khiến `com.acme.app.service` không được quét; hoặc có 2 bean cùng tên ở 2 package khác nhau → `ConflictingBeanDefinitionException`. Spring Boot mặc định **cấm overriding** bean definition (`spring.main.allow-bean-definition-overriding=false` từ Boot 2.1).

### 🛠 Bài tập phần 2

**Bài 2.1 — Liệt kê bean (Cơ bản)**
- Đề bài: tạo app Spring Boot web trống, viết `CommandLineRunner` in ra số lượng bean và 20 bean đầu tiên theo tên, kèm class thật của bean.
- Tiêu chí đạt: giải thích được vì sao một app "trống" có hàng trăm bean (gợi ý: auto-configuration).

**Bài 2.2 — Đăng ký bean động (Trung bình)**
- Đề bài: viết annotation `@EnableAuditing(prefix="audit")` dùng `@Import` + `ImportBeanDefinitionRegistrar` để đăng ký bean `AuditService` với property `prefix` lấy từ annotation.
- Tiêu chí đạt: bean xuất hiện khi gắn annotation, không xuất hiện khi bỏ; có test bằng `ApplicationContextRunner`.

**Bài 2.3 — BeanFactory trần (Nâng cao)**
- Đề bài: dùng `DefaultListableBeanFactory` + `AnnotatedBeanDefinitionReader` để nạp một class có `@Autowired` và `@PostConstruct`. Quan sát cái gì không chạy, rồi đăng ký tay các `BeanPostProcessor` cần thiết để nó chạy.
- Tiêu chí đạt: giải thích bằng lời khác biệt BeanFactory/ApplicationContext dựa trên kết quả thực nghiệm.

<details>
<summary>Gợi ý lời giải</summary>

- Bài 2.1: `ctx.getBeanDefinitionCount()`, `ctx.getBean(name).getClass()` — chú ý class dạng `...$$SpringCGLIB$$0` (Spring 6) là proxy.
- Bài 2.2:

```java
public class AuditRegistrar implements ImportBeanDefinitionRegistrar {
    @Override
    public void registerBeanDefinitions(AnnotationMetadata meta, BeanDefinitionRegistry registry) {
        var attrs = meta.getAnnotationAttributes(EnableAuditing.class.getName());
        var bd = BeanDefinitionBuilder.genericBeanDefinition(AuditService.class)
                .addConstructorArgValue(attrs.get("prefix")).getBeanDefinition();
        registry.registerBeanDefinition("auditService", bd);
    }
}
```
- Bài 2.3: `AnnotatedBeanDefinitionReader` thực ra đã gọi `AnnotationConfigUtils.registerAnnotationConfigProcessors` để **đăng ký definition** của các processor, nhưng `BeanFactory` không tự **áp dụng** chúng; phải `factory.addBeanPostProcessor(factory.getBean(AutowiredAnnotationBeanPostProcessor.class))` và tương tự với `CommonAnnotationBeanPostProcessor`. `ApplicationContext.refresh()` làm việc này trong `registerBeanPostProcessors`.
</details>

---

<a id="p3"></a>
## 3. @Configuration vs lite mode, @Qualifier/@Primary, conditional bean

### 3.1 @Configuration (full mode) và proxyBeanMethods
Class `@Configuration` mặc định (`proxyBeanMethods = true`) được Spring **sub-class bằng CGLIB**. Mỗi lời gọi tới một method `@Bean` từ bên trong class đó bị intercept: nếu bean đã tồn tại trong container thì trả về instance đó, đảm bảo đúng ngữ nghĩa singleton.

```java
@Configuration // proxyBeanMethods = true (mặc định)
public class DataConfig {
    @Bean
    public DataSource dataSource() { return new HikariDataSource(); }

    @Bean
    public JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(dataSource()); // trả về CÙNG DataSource singleton nhờ CGLIB
    }

    @Bean
    public TransactionManager txManager() {
        return new DataSourceTransactionManager(dataSource()); // vẫn cùng instance
    }
}
```

### 3.2 Lite mode: @Component + @Bean hoặc proxyBeanMethods = false
Khi `@Bean` nằm trong `@Component` (hoặc `@Configuration(proxyBeanMethods = false)`), class **không** bị CGLIB: gọi `dataSource()` trực tiếp chỉ là gọi method Java thường → **tạo instance mới** mỗi lần, ngoài sự quản lý của container (không có lifecycle, không có proxy).

```java
@Configuration(proxyBeanMethods = false)
public class LiteConfig {
    @Bean
    public DataSource dataSource() { return new HikariDataSource(); }

    @Bean // ĐÚNG cách trong lite mode: nhận dependency qua tham số
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
```

| | Full (`proxyBeanMethods=true`) | Lite (`false` hoặc `@Component`) |
|---|---|---|
| CGLIB subclass | Có | Không |
| Gọi `@Bean` method nội bộ | trả về singleton | tạo object mới (bug!) |
| Startup/memory | chậm hơn chút | nhanh hơn, thân thiện GraalVM native |
| Class/method được `final`? | Không | Có |

> 💡 **Góc nhìn Senior:** Toàn bộ auto-configuration của Spring Boot dùng `proxyBeanMethods = false` (annotation `@AutoConfiguration` mặc định như vậy) để giảm chi phí CGLIB và hỗ trợ native image. Quy tắc thực tế: **luôn truyền dependency qua tham số `@Bean` method**, khi đó full hay lite đều đúng và bạn có thể tắt proxy an toàn.

> ⚠️ **Lỗi thường gặp:** method `@Bean` trả về `BeanFactoryPostProcessor` (vd `PropertySourcesPlaceholderConfigurer`) mà không khai báo `static` → class config bị khởi tạo quá sớm, `@Value`/`@Autowired` trong class đó không được xử lý. Spring log cảnh báo `@Bean method ... is non-static and returns an object assignable to Spring's BeanFactoryPostProcessor interface`.

### 3.3 @Qualifier, @Primary và fallback
```java
public interface Notifier { void send(String msg); }

@Component @Primary
class EmailNotifier implements Notifier { public void send(String m) { /*...*/ } }

@Component @Qualifier("sms")
class SmsNotifier implements Notifier { public void send(String m) { /*...*/ } }

@Service
class AlertService {
    private final Notifier defaultNotifier;  // EmailNotifier nhờ @Primary
    private final Notifier urgentNotifier;   // SmsNotifier nhờ @Qualifier

    AlertService(Notifier defaultNotifier, @Qualifier("sms") Notifier urgentNotifier) {
        this.defaultNotifier = defaultNotifier;
        this.urgentNotifier = urgentNotifier;
    }
}
```
- `@Primary`: "mặc định khi không nói gì". `@Qualifier`: "chọn chính xác cái này" và **thắng** `@Primary`.
- Có thể tạo annotation qualifier riêng có type-safety: `@Qualifier @Retention(RUNTIME) @interface Urgent {}`.
- Spring 6.2 thêm `@Fallback`: ngược với `@Primary` — bean chỉ được chọn khi không còn candidate nào khác.

### 3.4 Conditional bean
- `@Profile("prod")` — dựa trên profile active.
- `@Conditional(MyCondition.class)` — interface `Condition.matches(ConditionContext, AnnotatedTypeMetadata)`; là nền tảng của mọi `@ConditionalOn*` trong Boot.
- Boot cung cấp: `@ConditionalOnProperty(name="feature.x.enabled", havingValue="true", matchIfMissing=false)`, `@ConditionalOnClass`, `@ConditionalOnMissingBean`, `@ConditionalOnBean`, `@ConditionalOnWebApplication`, `@ConditionalOnExpression`, `@ConditionalOnCloudPlatform`, `@ConditionalOnThreading` (Boot 3.2, VIRTUAL/PLATFORM).

```java
@Configuration(proxyBeanMethods = false)
public class CacheConfig {
    @Bean
    @ConditionalOnProperty(prefix = "app.cache", name = "type", havingValue = "redis")
    CacheClient redisCacheClient(RedisConnectionFactory f) { return new RedisCacheClient(f); }

    @Bean
    @ConditionalOnMissingBean(CacheClient.class)
    CacheClient inMemoryCacheClient() { return new InMemoryCacheClient(); }
}
```

> ⚠️ **Lỗi thường gặp:** dùng `@ConditionalOnBean`/`@ConditionalOnMissingBean` trong **cấu hình người dùng** (user config). Điều kiện được đánh giá theo những gì đã đăng ký *tại thời điểm xử lý*; với user config, thứ tự xử lý không đảm bảo → kết quả phụ thuộc thứ tự class. Spring Boot reference khuyến cáo chỉ dùng chúng trong auto-configuration (được xử lý **sau** user config).

### 🛠 Bài tập phần 3

**Bài 3.1 — Chứng minh khác biệt full vs lite (Cơ bản)**
- Đề bài: viết 2 class config giống nhau, một full, một lite, mỗi class có `@Bean Counter counter()` và `@Bean A a() { return new A(counter()); }`, `@Bean B b() { return new B(counter()); }`.
- Tiêu chí đạt: test assert `a.counter == b.counter` với full và `!=` với lite; in tên class thực của config bean.

**Bài 3.2 — Feature toggle (Trung bình)**
- Đề bài: `PaymentGateway` có `StripeGateway` (khi `payment.provider=stripe`), `VnPayGateway` (khi `=vnpay`), `FakeGateway` (khi không cấu hình gì hoặc profile `dev`).
- Tiêu chí đạt: 3 test dùng `ApplicationContextRunner` với `withPropertyValues(...)` kiểm tra đúng bean được tạo; không bao giờ có 2 bean `PaymentGateway` cùng lúc.

**Bài 3.3 — Custom Condition (Nâng cao)**
- Đề bài: viết `@ConditionalOnBusinessHours` (bean chỉ được tạo nếu khởi động trong giờ hành chính theo timezone cấu hình) dùng `Condition` và đọc property `app.timezone` từ `Environment`.
- Tiêu chí đạt: test có `Clock` cố định (gợi ý: đọc thời gian từ property `app.fixed-time` khi có, để test được); giải thích vì sao condition loại này **không phù hợp** cho native image AOT.

<details>
<summary>Gợi ý lời giải</summary>

- Bài 3.1: full mode → class tên dạng `Config$$SpringCGLIB$$0`.
- Bài 3.2: `@ConditionalOnProperty(name="payment.provider", havingValue="stripe")`; với `FakeGateway` dùng `@ConditionalOnProperty(name="payment.provider", matchIfMissing=true, havingValue="fake")` hoặc đặt trong auto-config với `@ConditionalOnMissingBean`.
- Bài 3.3: `context.getEnvironment().getProperty("app.timezone", "Asia/Ho_Chi_Minh")`. AOT đánh giá condition **lúc build** và "đóng băng" kết quả → condition phụ thuộc thời gian chạy sẽ sai.
</details>

---

<a id="p4"></a>
## 4. Bean scopes & tiêm prototype vào singleton

### 4.1 Các scope
| Scope | Ý nghĩa | Ghi chú |
|---|---|---|
| `singleton` (mặc định) | 1 instance / container | **không phải** singleton JVM; 2 context = 2 instance. Phải thread-safe (stateless). |
| `prototype` | instance mới mỗi lần `getBean`/inject | container **không gọi destroy callback** — bạn tự dọn tài nguyên |
| `request` | 1 instance / HTTP request | chỉ trong web context |
| `session` | 1 instance / HTTP session | cẩn thận dung lượng session & sticky session |
| `application` | 1 instance / `ServletContext` | |
| `websocket` | 1 instance / WebSocket session | |

Có thể tự định nghĩa scope (implement `Scope`) — ví dụ Spring Cloud `@RefreshScope`, hoặc `SimpleThreadScope` (có sẵn nhưng không đăng ký mặc định).

> 💡 **Góc nhìn Senior:** singleton bean chứa field mutable (vd `private List<Item> cache = new ArrayList<>()`, `SimpleDateFormat`) là nguồn race condition kinh điển trên production vì mọi request thread dùng chung. Quy tắc: singleton phải stateless, hoặc state phải thread-safe (`ConcurrentHashMap`, `AtomicLong`, immutable).

### 4.2 Vấn đề: tiêm prototype vào singleton
Singleton chỉ được tạo **một lần** → dependency prototype của nó cũng chỉ được inject một lần → "prototype" trở thành singleton trên thực tế.

```java
@Component @Scope("prototype")
class ShoppingCart { final List<String> items = new ArrayList<>(); }

@Service
class CheckoutService {
    private final ShoppingCart cart; // BUG: mọi lời gọi dùng chung 1 cart
    CheckoutService(ShoppingCart cart) { this.cart = cart; }
}
```

**Ba cách giải:**

```java
// Cách 1 (khuyến nghị): ObjectProvider — lấy instance mới mỗi lần gọi
@Service
class CheckoutService1 {
    private final ObjectProvider<ShoppingCart> carts;
    CheckoutService1(ObjectProvider<ShoppingCart> carts) { this.carts = carts; }
    void checkout() { ShoppingCart cart = carts.getObject(); /* cart mới */ }
}

// Cách 2: lookup method injection — Spring CGLIB override method abstract/stub
@Service
abstract class CheckoutService2 {
    @Lookup
    protected abstract ShoppingCart newCart();
    void checkout() { ShoppingCart cart = newCart(); }
}

// Cách 3: scoped proxy — inject 1 proxy, mỗi lần gọi method trên proxy sẽ tra target theo scope
@Component
@Scope(value = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)
class ShoppingCartProxied { /* ... */ }
```

Với `request`/`session` scope tiêm vào singleton, **bắt buộc** phải có proxy hoặc `ObjectProvider` — nếu không Spring báo `ScopeNotActiveException`/`No thread-bound request found` lúc startup. `@RequestScope` và `@SessionScope` mặc định đã có `proxyMode = TARGET_CLASS`.

> ⚠️ **Lỗi thường gặp:**
> - Scoped proxy + **prototype**: *mỗi lần gọi method* trên proxy lại tạo một target mới → `cart.add("A"); cart.size()` trả về 0. Với prototype, `ObjectProvider` gần như luôn là lựa chọn đúng.
> - Truy cập bean `request` scope từ thread `@Async` → không có request context → exception.
> - Dùng `ApplicationContext.getBean()` rải rác trong code (Service Locator) thay cho `ObjectProvider` → khó test, che giấu dependency.

### 🛠 Bài tập phần 4

**Bài 4.1 — Quan sát scope (Cơ bản)**
- Đề bài: tạo 2 bean `SingletonCounter` và `PrototypeCounter`, mỗi bean in `System.identityHashCode(this)` trong constructor. Gọi `getBean` 3 lần mỗi loại.
- Tiêu chí đạt: giải thích kết quả; thêm `@PreDestroy` cho cả hai và chứng minh prototype không được gọi destroy khi đóng context.

**Bài 4.2 — Request-scoped context (Trung bình)**
- Đề bài: tạo bean `RequestContext` (`@RequestScope`) lưu `correlationId` lấy từ header `X-Correlation-Id` (hoặc sinh UUID) bằng `HandlerInterceptor`; service singleton đọc nó để log.
- Tiêu chí đạt: 2 request song song (dùng `MockMvc` hoặc curl) có correlationId khác nhau trong log; giải thích vì sao inject được bean request vào singleton.

**Bài 4.3 — Bẫy scoped proxy prototype (Nâng cao)**
- Đề bài: tái hiện bug "cart luôn rỗng" với `proxyMode = TARGET_CLASS` + prototype; sửa bằng `ObjectProvider`.
- Tiêu chí đạt: test đỏ trước, xanh sau; viết 3–5 câu giải thích cơ chế `ScopedProxyFactoryBean`/`SimpleBeanTargetSource` (target được lấy lại từ BeanFactory mỗi lần invoke).

<details>
<summary>Gợi ý lời giải</summary>

- Bài 4.2: interceptor `preHandle` set `requestContext.setCorrelationId(...)`; bean inject vào singleton thực chất là CGLIB proxy, mỗi method call tra target trong `RequestContextHolder` (ThreadLocal) của request hiện tại. Nhớ `MDC.put("cid", ...)` để log có id.
- Bài 4.3: mỗi lời gọi `proxy.add()` và `proxy.size()` đi qua `getTarget()` → `beanFactory.getBean(targetName)` → prototype mới.
</details>

---

<a id="p5"></a>
## 5. Bean lifecycle, BeanPostProcessor & BeanFactoryPostProcessor

### 5.1 Toàn cảnh vòng đời một singleton bean
```
[Pha đăng ký]
  Đọc cấu hình → BeanDefinition được đăng ký
  BeanDefinitionRegistryPostProcessor (vd ConfigurationClassPostProcessor xử lý @Configuration/@Bean/@Import/@ComponentScan)
  BeanFactoryPostProcessor (vd PropertySourcesPlaceholderConfigurer thay ${...})
  Đăng ký các BeanPostProcessor

[Pha tạo từng bean — AbstractAutowireCapableBeanFactory.doCreateBean]
  1. InstantiationAwareBPP.postProcessBeforeInstantiation (có thể trả về proxy thay thế, hiếm)
  2. Instantiation: gọi constructor (constructor injection xảy ra ở đây)
  3. MergedBeanDefinitionPostProcessor (thu thập metadata @Autowired/@PostConstruct)
  4. Đưa ObjectFactory vào singletonFactories (cache cấp 3 — Phần 6)
  5. Populate properties: field/setter injection (AutowiredAnnotationBeanPostProcessor.postProcessProperties)
  6. Aware callbacks cơ bản: BeanNameAware → BeanClassLoaderAware → BeanFactoryAware
  7. BPP.postProcessBeforeInitialization
       - ApplicationContextAwareProcessor: EnvironmentAware, ResourceLoaderAware,
         ApplicationEventPublisherAware, MessageSourceAware, ApplicationContextAware...
       - CommonAnnotationBeanPostProcessor (InitDestroyAnnotationBeanPostProcessor): @PostConstruct
  8. InitializingBean.afterPropertiesSet()
  9. init-method tùy chỉnh (@Bean(initMethod = "..."))
 10. BPP.postProcessAfterInitialization  ← NƠI TẠO AOP PROXY (AbstractAutoProxyCreator)
 11. Bean sẵn sàng; sau khi TẤT CẢ singleton được tạo: SmartInitializingSingleton.afterSingletonsInstantiated
 12. SmartLifecycle.start() (vd web server, message listener container), ContextRefreshedEvent

[Pha hủy — context.close()]
 13. SmartLifecycle.stop() (theo phase, ngược thứ tự)
 14. @PreDestroy → DisposableBean.destroy() → destroy-method
     (với @Bean, Spring tự suy luận destroy-method "close"/"shutdown" nếu có — inferred)
```

```java
@Component
public class LifecycleBean implements BeanNameAware, ApplicationContextAware,
        InitializingBean, DisposableBean {

    private final Dependency dep;

    public LifecycleBean(Dependency dep) {               // 1. instantiate (+ constructor injection)
        this.dep = dep;
        log("constructor");
    }
    @Autowired void setOther(Other other) { log("setter injection"); } // 2. populate
    @Override public void setBeanName(String name) { log("BeanNameAware " + name); } // 3
    @Override public void setApplicationContext(ApplicationContext c) { log("ApplicationContextAware"); } // 4
    @PostConstruct void postConstruct() { log("@PostConstruct"); }      // 5
    @Override public void afterPropertiesSet() { log("afterPropertiesSet"); } // 6
    @PreDestroy void preDestroy() { log("@PreDestroy"); }                // 8
    @Override public void destroy() { log("DisposableBean.destroy"); }   // 9

    private static void log(String s) { System.out.println("[lifecycle] " + s); }
}

@Component
class LoggingBpp implements BeanPostProcessor {
    @Override
    public Object postProcessBeforeInitialization(Object bean, String name) {
        if (bean instanceof LifecycleBean) System.out.println("[lifecycle] BPP before");
        return bean;
    }
    @Override
    public Object postProcessAfterInitialization(Object bean, String name) {
        if (bean instanceof LifecycleBean) System.out.println("[lifecycle] BPP after (proxy có thể được tạo ở đây)");
        return bean;
    }
}
```

> Lưu ý: `@PostConstruct`/`@PreDestroy` trong Spring 6/Boot 3 là `jakarta.annotation.*` (Boot 2: `javax.annotation.*`). Nếu import nhầm `javax.annotation.PostConstruct` từ một thư viện cũ trên classpath, Spring 6 sẽ **im lặng bỏ qua** method đó.

### 5.2 BeanPostProcessor vs BeanFactoryPostProcessor
| | `BeanFactoryPostProcessor` | `BeanPostProcessor` |
|---|---|---|
| Làm việc với | `BeanDefinition` (metadata) | **instance** bean |
| Thời điểm | trước khi bất kỳ bean thường nào được tạo | quanh bước initialization của từng bean |
| Ví dụ | `PropertySourcesPlaceholderConfigurer`, `ConfigurationClassPostProcessor` | `AutowiredAnnotationBeanPostProcessor`, `CommonAnnotationBeanPostProcessor`, `AnnotationAwareAspectJAutoProxyCreator`, `AsyncAnnotationBeanPostProcessor`, `ConfigurationPropertiesBindingPostProcessor` |
| Có thể | đổi scope, class, property value, thêm/xóa definition | bọc bean thành proxy, validate, inject thêm |

> 💡 **Góc nhìn Senior:**
> - Bean mà `BeanPostProcessor` phụ thuộc vào sẽ được tạo **sớm**, trước khi các BPP khác đăng ký → không đủ điều kiện được auto-proxy. Log điển hình: `Bean 'xyz' of type [...] is not eligible for getting processed by all BeanPostProcessors (for example: not eligible for auto-proxying)`. Hệ quả: `@Transactional` trên bean đó **không chạy**. Cách tránh: BPP khai báo `static @Bean`, inject dependency lazily (`ObjectProvider`).
> - Đừng làm I/O nặng (gọi HTTP, warm-up cache lớn) trong `@PostConstruct`: chặn startup, chạy *trước* khi proxy được tạo (nên gọi method `@Transactional` của chính nó trong `@PostConstruct` sẽ không có transaction). Dùng `ApplicationReadyEvent` hoặc `SmartLifecycle` cho warm-up.

> ⚠️ **Lỗi thường gặp:** `@PostConstruct` trên bean prototype chạy mỗi lần tạo, nhưng `@PreDestroy` không bao giờ chạy → rò rỉ connection/thread nếu bean prototype giữ tài nguyên.

### 🛠 Bài tập phần 5

**Bài 5.1 — In thứ tự lifecycle (Cơ bản)**
- Đề bài: chạy đoạn `LifecycleBean` ở trên, thêm `@Bean(initMethod="customInit", destroyMethod="customDestroy")` cho một bean khác.
- Tiêu chí đạt: output có đủ ≥ 10 bước theo đúng thứ tự; vẽ lại sơ đồ bằng lời của bạn.

**Bài 5.2 — BeanPostProcessor đo thời gian (Trung bình)**
- Đề bài: viết annotation `@Timed` cho method và một `BeanPostProcessor` bọc bean có method `@Timed` bằng `ProxyFactory` (Spring AOP API), log thời gian thực thi.
- Tiêu chí đạt: bean không có `@Timed` không bị bọc; proxy vẫn inject được theo class (dùng CGLIB: `setProxyTargetClass(true)`).

**Bài 5.3 — BeanFactoryPostProcessor (Nâng cao)**
- Đề bài: viết `BeanFactoryPostProcessor` tự động đặt `lazy-init=true` cho mọi bean có tên kết thúc bằng `Report` và in ra cảnh báo nếu phát hiện bean singleton có field không `final` & không `static` (dùng reflection trên `beanClassName`).
- Tiêu chí đạt: khai báo `static @Bean`; log liệt kê đúng; giải thích vì sao không được `getBean()` bên trong BFPP.

<details>
<summary>Gợi ý lời giải</summary>

- Bài 5.2:

```java
@Component
class TimedBpp implements BeanPostProcessor {
    @Override
    public Object postProcessAfterInitialization(Object bean, String name) {
        Class<?> type = AopUtils.getTargetClass(bean);
        boolean hasTimed = Arrays.stream(type.getMethods()).anyMatch(m -> m.isAnnotationPresent(Timed.class));
        if (!hasTimed) return bean;
        ProxyFactory pf = new ProxyFactory(bean);
        pf.setProxyTargetClass(true);
        pf.addAdvice((MethodInterceptor) inv -> {
            Method m = AopUtils.getMostSpecificMethod(inv.getMethod(), type);
            if (!m.isAnnotationPresent(Timed.class)) return inv.proceed();
            long t = System.nanoTime();
            try { return inv.proceed(); }
            finally { System.out.printf("%s took %d µs%n", m.getName(), (System.nanoTime() - t) / 1000); }
        });
        return pf.getProxy();
    }
}
```
- Bài 5.3: `getBean()` trong BFPP buộc khởi tạo bean sớm, trước khi các BPP được đăng ký → bean "trần", mất proxy/autowiring.
</details>

---

<a id="p6"></a>
## 6. Circular dependency & cơ chế 3-level cache

### 6.1 Hiện tượng
`A` cần `B`, `B` cần `A`. Với **constructor injection** cả hai phía → không thể giải: để tạo A cần B hoàn chỉnh, để tạo B cần A hoàn chỉnh → `BeanCurrentlyInCreationException`.

Với **field/setter injection**, Spring (singleton) có thể giải nhờ "early reference": tạo A (chưa populate), công bố một tham chiếu sớm, populate A → tạo B → B lấy tham chiếu sớm của A → B hoàn chỉnh → A hoàn chỉnh.

### 6.2 Ba cấp cache trong `DefaultSingletonBeanRegistry`
1. `singletonObjects` — bean **hoàn chỉnh** (cấp 1).
2. `earlySingletonObjects` — bean **đã instantiate nhưng chưa xong** (early reference) đã từng được lấy ra (cấp 2).
3. `singletonFactories` — `ObjectFactory` sinh early reference; gọi `getEarlyBeanReference()` qua các `SmartInstantiationAwareBeanPostProcessor` (cấp 3).

**Vì sao cần cấp 3 thay vì chỉ cấp 2?** Vì AOP: nếu A cần bị proxy (`@Transactional`), B phải nhận **proxy** của A, không phải object gốc. Nhưng proxy bình thường chỉ được tạo ở `postProcessAfterInitialization` (cuối vòng đời). Cấp 3 cho phép *trì hoãn* quyết định: chỉ khi có ai thật sự cần early reference thì mới gọi `AbstractAutoProxyCreator.getEarlyBeanReference()` để tạo proxy sớm; nếu không có vòng tròn thì không tốn chi phí đó. Kết quả cache vào cấp 2 để mọi người dùng cùng một early reference.

```
getBean(A)
  ├─ instantiate A (raw)  → singletonFactories.put(A, () -> getEarlyBeanReference(A))
  ├─ populate A: cần B → getBean(B)
  │     ├─ instantiate B → singletonFactories.put(B, ...)
  │     ├─ populate B: cần A → getSingleton(A):
  │     │      cấp1? không. cấp2? không. cấp3? có → gọi factory → (proxy của) A
  │     │      → chuyển vào earlySingletonObjects, xóa khỏi singletonFactories
  │     └─ initialize B → singletonObjects.put(B)
  └─ initialize A → kiểm tra: bean cuối cùng có == early reference đã phát ra không?
        nếu một BPP khác đã bọc A thành object khác → BeanCurrentlyInCreationException
        ("...has been injected into other beans in its raw version as part of a circular reference...")
```

### 6.3 Spring Boot 2.6+ cấm mặc định
Từ **Spring Boot 2.6**, `spring.main.allow-circular-references=false` mặc định: app có vòng tròn sẽ fail khi startup với thông báo `The dependencies of some of the beans in the application context form a cycle` kèm sơ đồ vòng. Lý do: vòng tròn là dấu hiệu thiết kế kém, làm early reference "chưa hoàn chỉnh" bị dùng (bug khó đoán khi B gọi A trong `@PostConstruct`), và gây vấn đề với proxy. (Spring Framework thuần vẫn cho phép.)

**Cách sửa (theo thứ tự ưu tiên):**
1. **Thiết kế lại**: tách phần dùng chung thành class thứ ba C mà A và B cùng phụ thuộc; hoặc xác định ai thật sự là "owner".
2. **Đảo phụ thuộc bằng event**: A publish event, B lắng nghe — không còn tham chiếu trực tiếp.
3. **Lazy**: `@Lazy` trên tham số constructor → Spring inject một proxy lazy, phá vòng ở thời điểm tạo.
4. `ObjectProvider<B>` và resolve khi cần.
5. Bật lại `spring.main.allow-circular-references=true` — chỉ là giải pháp tạm khi migrate.

```java
@Service
class UserService {
    private final OrderService orders;
    UserService(@Lazy OrderService orders) { this.orders = orders; } // inject proxy lazy
}
@Service
class OrderService {
    private final UserService users;
    OrderService(UserService users) { this.users = users; }
}
```

> 💡 **Góc nhìn Senior:** Prototype bean có vòng tròn **không bao giờ** được giải (không cache). Vòng tròn cũng thường xuất hiện gián tiếp qua self-injection (bean tự inject chính mình để gọi method `@Transactional` — Phần 7). Khi review code, coi `@Lazy` để phá vòng là technical debt cần ticket.

### 🛠 Bài tập phần 6

**Bài 6.1 — Tái hiện lỗi (Cơ bản)**
- Đề bài: tạo vòng A ↔ B bằng constructor injection trên Spring Boot 3, đọc thông báo lỗi; đổi sang field injection, chạy lại (vẫn fail vì Boot 2.6+); bật `allow-circular-references`.
- Tiêu chí đạt: ghi lại 3 thông báo/hành vi và giải thích từng cái.

**Bài 6.2 — Refactor phá vòng (Trung bình)**
- Đề bài: `AccountService.close()` gọi `NotificationService.notifyClosed()`; `NotificationService` cần `AccountService.findEmail()`. Refactor bằng 2 cách: (a) tách `AccountQueryService`; (b) dùng `ApplicationEvent`.
- Tiêu chí đạt: không dùng `@Lazy`, không bật cờ; so sánh trade-off 2 cách (coupling, transaction, testability).

**Bài 6.3 — Raw version injected (Nâng cao)**
- Đề bài: với `allow-circular-references=true`, tạo vòng A ↔ B (field injection) trong đó A có method `@Async`. Quan sát lỗi `has been injected into other beans [b] in its raw version`. Giải thích vì sao `@Transactional` không gây lỗi này còn `@Async` thì có.
- Tiêu chí đạt: giải thích được: `@Transactional` dùng `AbstractAutoProxyCreator` (hỗ trợ `getEarlyBeanReference`), còn `@Async` dùng `AsyncAnnotationBeanPostProcessor` — một `AbstractAdvisingBeanPostProcessor` chỉ bọc proxy ở `postProcessAfterInitialization`, không tham gia early reference → bean cuối khác early reference.

<details>
<summary>Gợi ý lời giải</summary>

- Bài 6.2(b): `AccountService` publish `AccountClosedEvent(accountId, email)` chứa luôn dữ liệu cần thiết → `NotificationService` không cần gọi ngược. Lưu ý dùng `@TransactionalEventListener` nếu chỉ muốn gửi sau commit.
- Bài 6.3: fix bằng `@Lazy` tại điểm inject A vào B, hoặc tách method `@Async` ra bean khác.
</details>

---

<a id="p7"></a>
## 7. Spring AOP — proxy, pointcut, advice, self-invocation

### 7.1 Khái niệm
AOP tách **cross-cutting concerns** (transaction, logging, security, caching, metrics, retry) khỏi business code. Thuật ngữ:
- **Aspect**: module chứa logic cross-cutting (`@Aspect` class).
- **Join point**: điểm có thể chèn logic — với Spring AOP **chỉ là method execution** trên bean Spring (AspectJ đầy đủ còn hỗ trợ field access, constructor...).
- **Pointcut**: biểu thức chọn join point.
- **Advice**: code chạy tại join point: `@Before`, `@AfterReturning`, `@AfterThrowing`, `@After` (finally), `@Around`.
- **Weaving**: Spring AOP weave **lúc runtime bằng proxy**; AspectJ weave lúc compile (ajc) hoặc load-time (LTW).

```java
@Aspect
@Component
@Order(1) // số nhỏ = ưu tiên cao = bọc ngoài cùng
public class PerformanceAspect {

    @Pointcut("within(com.example.shop.service..*)")
    void serviceLayer() {}

    @Around("serviceLayer() && @annotation(timed)")
    public Object measure(ProceedingJoinPoint pjp, Timed timed) throws Throwable {
        long start = System.nanoTime();
        try {
            return pjp.proceed();            // gọi tiếp chain / target
        } finally {
            long ms = (System.nanoTime() - start) / 1_000_000;
            if (ms > timed.warnMs()) {
                System.out.printf("SLOW %s took %d ms%n", pjp.getSignature().toShortString(), ms);
            }
        }
    }

    @AfterThrowing(pointcut = "serviceLayer()", throwing = "ex")
    public void logError(JoinPoint jp, RuntimeException ex) {
        System.out.println("Error in " + jp.getSignature() + ": " + ex.getMessage());
    }
}

@Retention(RetentionPolicy.RUNTIME) @Target(ElementType.METHOD)
@interface Timed { long warnMs() default 200; }
```

Pointcut designators hay dùng: `execution(* com.x..*Service.*(..))`, `within(...)`, `@annotation(...)`, `@within(...)` (annotation trên class), `bean(*Service)` (riêng Spring), `args(...)`, `this(...)` (kiểu của proxy) / `target(...)` (kiểu của target).

### 7.2 Proxy: JDK dynamic proxy vs CGLIB
| | JDK Dynamic Proxy | CGLIB (bản repackaged trong spring-core) |
|---|---|---|
| Cơ chế | `java.lang.reflect.Proxy` implement **interface** | sinh **subclass** của target lúc runtime |
| Yêu cầu | target implement ≥1 interface | class không `final`; method không `final`/`private` |
| Inject theo class | không được (`BeanNotOfRequiredTypeException`) | được |
| Mặc định | Spring Framework thuần: dùng khi có interface | **Spring Boot 2.0+**: `spring.aop.proxy-target-class=true` → CGLIB cho mọi trường hợp |

Spring 6 dùng Objenesis để tạo instance CGLIB mà **không gọi constructor** của target lần hai.

### 7.3 Self-invocation — bug số 1 trong phỏng vấn Spring
Proxy chỉ chặn được lời gọi **đi qua proxy**. Lời gọi `this.method()` bên trong target đi thẳng vào object gốc → advice không chạy.

```java
@Service
public class OrderService {
    public void placeOrders(List<Order> orders) {
        for (Order o : orders) {
            this.placeOne(o);   // self-invocation: KHÔNG có transaction mới!
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void placeOne(Order o) { /* ... */ }
}
```

Cách sửa:
1. **Tách sang bean khác** (khuyến nghị) — `OrderBatchService` gọi `OrderService.placeOne`.
2. Self-injection: inject chính mình (`@Lazy OrderService self` hoặc `ObjectProvider<OrderService>`) rồi gọi `self.placeOne(o)`.
3. `@EnableAspectJAutoProxy(exposeProxy = true)` + `((OrderService) AopContext.currentProxy()).placeOne(o)` — gắn code vào Spring AOP, ít dùng.
4. Dùng `TransactionTemplate` (programmatic) cho đoạn cần transaction riêng.
5. AspectJ weaving (`@EnableTransactionManagement(mode = AdviceMode.ASPECTJ)`) — weave vào bytecode nên self-invocation cũng được, nhưng phức tạp build.

### 7.4 @Transactional, @Cacheable, @Async đều là AOP
| Annotation | Interceptor | Hệ quả của proxy-based |
|---|---|---|
| `@Transactional` | `TransactionInterceptor` (qua `BeanFactoryTransactionAttributeSourceAdvisor`) | self-invocation không có tx; mặc định chỉ rollback với `RuntimeException`/`Error` (checked exception thì commit!); method `private` không được áp dụng |
| `@Cacheable` | `CacheInterceptor` | gọi nội bộ không dùng cache; key mặc định từ tham số (`SimpleKey`) |
| `@Async` | `AnnotationAsyncExecutionInterceptor` (qua `AsyncAnnotationBeanPostProcessor`) | gọi nội bộ chạy **đồng bộ**; method `void` nuốt exception nếu không cấu hình `AsyncUncaughtExceptionHandler` |
| `@PreAuthorize` | `AuthorizationManagerBeforeMethodInterceptor` | gọi nội bộ **bỏ qua kiểm tra quyền** → lỗ hổng bảo mật |
| `@Retryable` (Spring Retry) | `RetryOperationsInterceptor` | tương tự |

Về visibility: Spring 6.0 cho phép `@Transactional` trên method `protected`/package-private khi dùng **class-based (CGLIB) proxy**; method `private` vẫn không bao giờ được chặn. Interface-based proxy chỉ chặn method public của interface.

### 7.5 Thứ tự advice
- Nhiều aspect trên cùng join point: sắp theo `@Order`/`Ordered` — giá trị **nhỏ** chạy **trước** khi vào và **sau** khi ra (bọc ngoài cùng).
- `@EnableTransactionManagement(order = ...)`, `@EnableCaching(order = ...)`, `@EnableAsync(order=...)` cho phép chỉnh thứ tự advisor hệ thống. Mặc định đều `Ordered.LOWEST_PRECEDENCE`.
- Ví dụ thực tế: aspect **retry** phải bọc **ngoài** transaction (mỗi lần retry là một transaction mới) → order của retry aspect nhỏ hơn order của transaction advisor. Nếu ngược lại, retry chạy *bên trong* một transaction đã bị đánh dấu rollback-only → vô nghĩa.
- Trong cùng một aspect, Spring 5.2.7+: thứ tự theo loại advice `@Around, @Before, @After, @AfterReturning, @AfterThrowing`.

> 💡 **Góc nhìn Senior:** Mỗi proxy layer thêm chi phí nhỏ (vài trăm ns) — không đáng kể so với I/O; nhưng aspect có pointcut quá rộng (`execution(* *(..))`) sẽ bọc cả bean hạ tầng, tăng startup time và gây lỗi khó hiểu (proxy cả `final` class → fail). Luôn giới hạn bằng `within(com.yourcompany..*)`. Khi debug "annotation không chạy", câu hỏi đầu tiên: *lời gọi có đi qua proxy không?* — đặt breakpoint, xem `this.getClass()` có `$$SpringCGLIB$$` không.

> ⚠️ **Lỗi thường gặp:** `@Transactional` trên method của class gọi bằng `new` (không phải bean); bean được tạo quá sớm bởi BPP (Phần 5); bắt exception bên trong method transactional rồi nuốt mất → không rollback; `@Async` + `@Transactional` trên cùng method: transaction chạy trên thread mới, không chia sẻ transaction của caller.

### 🛠 Bài tập phần 7

**Bài 7.1 — Logging aspect (Cơ bản)**
- Đề bài: viết aspect log tên method, tham số (che giấu tham số có tên `password`), thời gian chạy cho mọi method public trong package `service`.
- Tiêu chí đạt: dùng `@Around`; có unit test với `AspectJProxyFactory` hoặc `@SpringBootTest` kiểm tra log (dùng `OutputCaptureExtension`).

**Bài 7.2 — Chứng minh self-invocation (Trung bình)**
- Đề bài: viết service có `outer()` gọi `this.inner()` với `inner()` là `@Transactional(propagation = REQUIRES_NEW)`. Dùng `TransactionSynchronizationManager.getCurrentTransactionName()` để in tên transaction trong `inner()`. Sau đó sửa bằng 2 cách khác nhau.
- Tiêu chí đạt: trước khi sửa: `inner` không có tx riêng; sau khi sửa: có transaction mới; giải thích bằng sơ đồ proxy.

**Bài 7.3 — Retry bọc ngoài transaction (Nâng cao)**
- Đề bài: viết annotation `@RetryOnOptimisticLock(maxAttempts=3)` + aspect bắt `ObjectOptimisticLockingFailureException` và gọi lại `pjp.proceed()`. Method dùng cả `@RetryOnOptimisticLock` và `@Transactional`.
- Tiêu chí đạt: test 2 thread cùng cập nhật một entity có `@Version`; chứng minh với order sai retry thất bại, với order đúng (aspect `@Order(Ordered.LOWEST_PRECEDENCE - 1)` bọc ngoài transaction) thì thành công.

<details>
<summary>Gợi ý lời giải</summary>

- Bài 7.2 cách 1: tách `inner` sang `InnerService`. Cách 2: `TransactionTemplate tt` với `tt.setPropagationBehavior(PROPAGATION_REQUIRES_NEW)`.
- Bài 7.3:

```java
@Aspect @Component
@Order(Ordered.LOWEST_PRECEDENCE - 1) // nhỏ hơn TransactionInterceptor (LOWEST_PRECEDENCE) → bọc ngoài
class OptimisticRetryAspect {
    @Around("@annotation(retry)")
    Object retry(ProceedingJoinPoint pjp, RetryOnOptimisticLock retry) throws Throwable {
        for (int attempt = 1; ; attempt++) {
            try { return pjp.proceed(); }
            catch (ObjectOptimisticLockingFailureException e) {
                if (attempt >= retry.maxAttempts()) throw e;
                Thread.sleep(ThreadLocalRandom.current().nextLong(10, 50)); // jitter
            }
        }
    }
}
```
Lưu ý: exception optimistic lock thường ném ra lúc **commit/flush** — tức là ở tầng transaction interceptor — nên aspect phải ở ngoài mới bắt được.
</details>

---

<a id="p8"></a>
## 8. SpEL & Application Events

### 8.1 Spring Expression Language (SpEL) — ngắn gọn
- `${...}`: **property placeholder** — thay giá trị từ `Environment`.
- `#{...}`: **SpEL** — biểu thức được đánh giá: gọi method, toán tử, tham chiếu bean (`@beanName`), collection selection/projection.

```java
@Component
class SpelDemo {
    @Value("${app.page-size:20}")                  // placeholder với default
    int pageSize;

    @Value("#{${app.page-size:20} * 2}")           // SpEL bọc placeholder
    int doublePageSize;

    @Value("#{systemProperties['user.timezone'] ?: 'UTC'}") // Elvis operator
    String tz;

    @Value("#{@featureFlags.isEnabled('new-checkout')}")    // gọi method của bean
    boolean newCheckout;
}
```
SpEL còn xuất hiện ở: `@PreAuthorize("hasRole('ADMIN') or #id == authentication.name")`, `@Cacheable(key = "#user.id", condition = "#user.active")`, `@ConditionalOnExpression`, `@EventListener(condition = "#event.amount > 1000")`, `@Scheduled(cron = "${jobs.cleanup.cron}")`.

> ⚠️ **Bảo mật:** không bao giờ đánh giá SpEL từ input người dùng với `StandardEvaluationContext` — có thể gọi `T(java.lang.Runtime).getRuntime().exec(...)` (đã có nhiều CVE dạng SpEL injection). Nếu buộc phải dùng, dùng `SimpleEvaluationContext` (giới hạn tính năng).

### 8.2 Application Events
Event là cách triển khai Observer pattern trong container, giúp **giảm coupling** giữa module.

```java
public record OrderPlacedEvent(long orderId, String email, BigDecimal amount) {} // không cần extends ApplicationEvent (Spring 4.2+)

@Service
class OrderService {
    private final ApplicationEventPublisher publisher;
    private final OrderRepository repo;
    OrderService(ApplicationEventPublisher publisher, OrderRepository repo) {
        this.publisher = publisher; this.repo = repo;
    }

    @Transactional
    public void place(Order o) {
        repo.save(o);
        publisher.publishEvent(new OrderPlacedEvent(o.id(), o.email(), o.amount()));
    }
}

@Component
class OrderListeners {
    @EventListener                               // ĐỒNG BỘ, cùng thread, cùng transaction với publisher
    void audit(OrderPlacedEvent e) { /* ghi audit trong cùng tx */ }

    @TransactionalEventListener                  // mặc định phase = AFTER_COMMIT
    void sendEmail(OrderPlacedEvent e) { /* chỉ gửi khi tx đã commit */ }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void pushToAnalytics(OrderPlacedEvent e) { /* chạy thread khác sau commit */ }
}
```

Điểm cần nắm:
- `@EventListener` mặc định **synchronous**: exception trong listener lan ngược về publisher và có thể **rollback** transaction của publisher. Muốn async: `@Async` trên listener hoặc cấu hình `SimpleApplicationEventMulticaster` với executor (ảnh hưởng **mọi** listener — thường không nên).
- `@TransactionalEventListener` phases: `BEFORE_COMMIT`, `AFTER_COMMIT` (mặc định), `AFTER_ROLLBACK`, `AFTER_COMPLETION`. Nếu publish **ngoài** transaction, listener **không được gọi** trừ khi `fallbackExecution = true`.
- Trong listener `AFTER_COMMIT`, transaction cũ đã commit nhưng resource vẫn còn bind; muốn ghi DB phải dùng `@Transactional(propagation = REQUIRES_NEW)` (Spring 6.1+ còn báo lỗi nếu bạn dùng `@Transactional` mặc định REQUIRED trên `@TransactionalEventListener` không async).
- Thứ tự listener: `@Order`. Listener có thể trả về event mới (được publish tiếp).
- Event nội bộ Boot quan trọng: `ApplicationStartingEvent`, `ApplicationEnvironmentPreparedEvent`, `ApplicationPreparedEvent`, `ContextRefreshedEvent`, `ApplicationStartedEvent`, `ApplicationReadyEvent`, `ApplicationFailedEvent`, `ContextClosedEvent`.

> 💡 **Góc nhìn Senior:** Event trong bộ nhớ **không bền vững**: nếu process chết sau commit nhưng trước khi listener `AFTER_COMMIT` gửi email/message → mất event. Với tích hợp quan trọng (gửi sang Kafka), dùng **Transactional Outbox** (ghi event vào bảng outbox trong cùng transaction rồi relay) — Spring Modulith có sẵn "Event Publication Registry" làm việc này. Đây là câu hỏi follow-up rất hay gặp.

### 🛠 Bài tập phần 8

**Bài 8.1 — SpEL (Cơ bản)**
- Đề bài: dùng `SpelExpressionParser` đánh giá các biểu thức: tổng các số chẵn trong list, lọc user có `age > 18` (selection `?[]`), lấy list email (projection `![]`).
- Tiêu chí đạt: 3 test pass; giải thích khác biệt `StandardEvaluationContext` vs `SimpleEvaluationContext`.

**Bài 8.2 — Event sau commit (Trung bình)**
- Đề bài: implement `UserRegisteredEvent`, listener gửi email (giả lập bằng log) chỉ khi đăng ký thành công. Viết test: khi lưu user ném exception (vd email trùng) thì email **không** được gửi.
- Tiêu chí đạt: dùng `@TransactionalEventListener`; test với `@SpringBootTest` + H2/Testcontainers; chứng minh `@EventListener` thường thì email bị gửi sai.

**Bài 8.3 — Outbox đơn giản (Nâng cao)**
- Đề bài: thay listener gửi email bằng bảng `outbox_event(id, type, payload, status, created_at)`; một `@Scheduled` poller đọc `NEW` (dùng `SELECT ... FOR UPDATE SKIP LOCKED`), "gửi" và đánh dấu `SENT`.
- Tiêu chí đạt: kill app giữa chừng rồi chạy lại không mất event; chạy 2 instance không gửi trùng (kiểm bằng log).

<details>
<summary>Gợi ý lời giải</summary>

- Bài 8.1: `parser.parseExpression("#nums.?[#this % 2 == 0]")`, `"users.?[age > 18].![email]"`.
- Bài 8.3: ghi outbox trong **cùng** `@Transactional` với `userRepository.save`; poller: `@Transactional` + native query `SKIP LOCKED` (PostgreSQL/MySQL 8). At-least-once → consumer phải idempotent.
</details>

---

<a id="p9"></a>
## 9. Spring Boot auto-configuration & starters

### 9.1 Spring Boot giải quyết gì?
- **Starters**: dependency descriptor gom nhóm thư viện tương thích (`spring-boot-starter-web` = spring-webmvc + Jackson + Tomcat embedded + validation...). Version được quản lý bởi `spring-boot-dependencies` BOM.
- **Auto-configuration**: tự tạo bean hợp lý dựa trên classpath + property + bean đã có ("opinionated defaults, back off when you define your own").
- **Embedded server**, executable fat jar, Actuator, externalized config.

### 9.2 Cơ chế auto-configuration
1. `@SpringBootApplication` chứa `@EnableAutoConfiguration` → `@Import(AutoConfigurationImportSelector.class)`.
2. `AutoConfigurationImportSelector` (một `DeferredImportSelector` — chạy **sau** khi mọi user `@Configuration` đã được xử lý) đọc danh sách class từ:
   - **Boot 2.7+ / 3.x:** `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` (mỗi dòng 1 class).
   - **Boot ≤ 2.6:** key `org.springframework.boot.autoconfigure.EnableAutoConfiguration` trong `META-INF/spring.factories`. Boot 3.0 **đã bỏ** hỗ trợ đăng ký auto-config qua `spring.factories` — library cũ không migrate sẽ "im lặng" không được nạp.
3. Lọc nhanh bằng `AutoConfigurationImportFilter` (`OnClassCondition`, `OnBeanCondition`, `OnWebApplicationCondition`) dùng metadata precomputed (`spring-autoconfigure-metadata.properties`) để không phải nạp class.
4. Áp dụng `exclude` (`@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)` hoặc `spring.autoconfigure.exclude`).
5. Sắp xếp theo `@AutoConfiguration(before=..., after=...)`, `@AutoConfigureOrder`.
6. Mỗi auto-config class là `@Configuration(proxyBeanMethods=false)` với các `@ConditionalOn*`.

Ví dụ (rút gọn từ ý tưởng của `DataSourceAutoConfiguration`):
```java
@AutoConfiguration(before = SqlInitializationAutoConfiguration.class)
@ConditionalOnClass({ DataSource.class, EmbeddedDatabaseType.class })
@EnableConfigurationProperties(DataSourceProperties.class)
public class MyDataSourceAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(DataSource.class)   // "back off" nếu user tự định nghĩa
    @ConditionalOnProperty(prefix = "spring.datasource", name = "url")
    DataSource dataSource(DataSourceProperties props) {
        return props.initializeDataSourceBuilder().build();
    }
}
```

### 9.3 Debug auto-configuration
- Chạy với `--debug` hoặc `debug=true` → in **Condition Evaluation Report** (Positive matches / Negative matches / Exclusions / Unconditional classes).
- Actuator endpoint `/actuator/conditions` (cần expose).
- `/actuator/beans`, `/actuator/configprops`, `/actuator/env` để xem bean & property thực tế.

> 💡 **Góc nhìn Senior:** "Magic" của Boot chỉ là `@Conditional` + thứ tự xử lý. Khi bean của bạn "không thay thế được" bean auto-config, kiểm tra: (1) kiểu khai báo của `@Bean` có khớp với `@ConditionalOnMissingBean` không (auto-config thường check theo type trả về); (2) bean của bạn có nằm trong vùng component scan không; (3) có hai auto-config cùng tạo bean không. Exclude auto-config là biện pháp cuối, vì sẽ mất cả các bean phụ đi kèm.

> ⚠️ **Lỗi thường gặp:** thêm `spring-boot-starter-data-jpa` cho một module không dùng DB → app fail `Failed to configure a DataSource: 'url' attribute is not specified`. Đây là auto-config hoạt động đúng (thấy class `DataSource` trên classpath) — sửa bằng bỏ dependency thừa, không phải exclude bừa.

### 🛠 Bài tập phần 9

**Bài 9.1 — Đọc Condition Evaluation Report (Cơ bản)**
- Đề bài: chạy app web + JPA với `--debug`, tìm 3 auto-config positive và 3 negative, giải thích điều kiện của mỗi cái.
- Tiêu chí đạt: bảng 6 dòng: tên auto-config — điều kiện — vì sao match/không match.

**Bài 9.2 — Override bean auto-config (Trung bình)**
- Đề bài: thay `ObjectMapper` mặc định để: không fail khi gặp unknown property, ngày theo ISO-8601, `snake_case`. Làm bằng 3 cách: property `spring.jackson.*`, `Jackson2ObjectMapperBuilderCustomizer`, và tự định nghĩa `@Bean ObjectMapper`.
- Tiêu chí đạt: nêu trade-off: cách thứ 3 khiến mọi customization khác của Boot (module JavaTimeModule, property `spring.jackson.*`) bị **mất** vì auto-config back off.

**Bài 9.3 — Tìm auto-config bị bỏ qua (Nâng cao)**
- Đề bài: viết một library nhỏ đăng ký auto-config bằng `spring.factories` kiểu Boot 2, dùng nó trong app Boot 3. Chẩn đoán vì sao bean không xuất hiện và sửa.
- Tiêu chí đạt: giải thích quá trình chẩn đoán (conditions report không hề liệt kê class) và cách sửa bằng file `AutoConfiguration.imports`.

<details>
<summary>Gợi ý lời giải</summary>

- Bài 9.2: ưu tiên `Jackson2ObjectMapperBuilderCustomizer` — cộng dồn với cấu hình mặc định.

```java
@Bean
Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
    return b -> b.propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                 .featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                                    SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
}
```
(Lưu ý Boot đã tắt `FAIL_ON_UNKNOWN_PROPERTIES` và `WRITE_DATES_AS_TIMESTAMPS` mặc định — kiểm chứng điều này là một phần bài tập.)
- Bài 9.3: tạo `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` chứa FQCN, đổi `@Configuration` thành `@AutoConfiguration`.
</details>

---

<a id="p10"></a>
## 10. Externalized configuration, profiles, @ConfigurationProperties

### 10.1 Thứ tự ưu tiên PropertySource
Theo Spring Boot reference (từ **thấp** đến **cao** — nguồn sau ghi đè nguồn trước):
1. Default properties (`SpringApplication.setDefaultProperties`)
2. `@PropertySource` trên `@Configuration` (lưu ý: chỉ được thêm khi context refresh — quá muộn cho `logging.*`, `spring.main.*`)
3. **Config data** (`application.properties`/`.yml`)
4. `RandomValuePropertySource` (`random.*`)
5. Biến môi trường OS
6. Java System properties (`-Dkey=value`)
7. JNDI (`java:comp/env`)
8. `ServletContext` init parameters
9. `ServletConfig` init parameters
10. `SPRING_APPLICATION_JSON` (JSON inline trong env var/system property)
11. **Command line arguments** (`--server.port=9000`)
12. `properties` attribute trên test (`@SpringBootTest(properties=...)`)
13. `@DynamicPropertySource` trong test
14. `@TestPropertySource`
15. Devtools global settings (`~/.config/spring-boot`) khi devtools active

Trong nhóm **config data**, thứ tự (thấp → cao): `application.properties` trong jar → `application-{profile}.properties` trong jar → `application.properties` ngoài jar → `application-{profile}.properties` ngoài jar. Vị trí tìm mặc định: `classpath:/`, `classpath:/config/`, `./`, `./config/`, `./config/*/`. Nếu cùng vị trí có cả `.properties` và `.yml`, `.properties` thắng.

**Relaxed binding:** `spring.datasource.url` có thể được ghi đè bằng env var `SPRING_DATASOURCE_URL` (thay `.` bằng `_`, bỏ `-`, viết hoa). Đây là cách chuẩn khi deploy trên Kubernetes/Docker.

Import thêm nguồn: `spring.config.import=optional:file:./secrets.properties, configtree:/etc/config/` (configtree đọc Kubernetes Secret/ConfigMap mount dạng file), hoặc `vault://`, `configserver:` với Spring Cloud.

### 10.2 Profiles
```yaml
# application.yml
spring:
  application:
    name: shop
  profiles:
    group:
      prod: "prod-db, prod-observability"   # bật prod sẽ bật kèm 2 profile con
server:
  port: 8080
---
spring:
  config:
    activate:
      on-profile: dev          # Boot 2.4+ (thay cho spring.profiles: dev cũ)
logging:
  level:
    com.example: DEBUG
---
spring:
  config:
    activate:
      on-profile: prod
server:
  shutdown: graceful
```
- Kích hoạt: `--spring.profiles.active=prod`, env `SPRING_PROFILES_ACTIVE=prod`. `spring.profiles.default` dùng khi không có profile nào active (mặc định là `default`).
- **Không được** đặt `spring.profiles.active`/`spring.profiles.include` trong file profile-specific hoặc document có `on-profile` (Boot 2.4+ báo lỗi).
- `@Profile("!prod")` / biểu thức `@Profile("cloud & !test")`.

> 💡 **Góc nhìn Senior:** Tránh dùng profile để mã hóa *từng môi trường* (`dev`, `staging`, `prod-sg`, `prod-vn`...) với hàng chục file khác nhau → "configuration drift". Nguyên tắc 12-factor: build **một** artifact, khác biệt môi trường đưa vào qua env var/config server. Profile dùng cho *đặc tính* (vd `kafka`, `local-mock`, `cloud`). Secret không bao giờ nằm trong `application.yml` commit vào git — dùng Vault/Secret Manager/K8s Secret + `configtree`.

### 10.3 @ConfigurationProperties vs @Value
```java
@ConfigurationProperties(prefix = "app.payment")
@Validated
public record PaymentProperties(
        @NotBlank String provider,
        @NotNull URI baseUrl,
        @DefaultValue("5s") Duration connectTimeout,   // "5s", "500ms", "PT5S" đều được
        @DefaultValue("30s") Duration readTimeout,
        @DefaultValue("3") @Min(0) @Max(10) int maxRetries,
        Map<String, String> headers) {
}

@SpringBootApplication
@ConfigurationPropertiesScan          // hoặc @EnableConfigurationProperties(PaymentProperties.class)
public class ShopApplication { }
```
```yaml
app:
  payment:
    provider: vnpay
    base-url: https://sandbox.vnpay.vn
    connect-timeout: 2s
    max-retries: 2
    headers:
      X-Merchant: shop01
```

| | `@ConfigurationProperties` | `@Value` |
|---|---|---|
| Nhóm property có cấu trúc | ✅ type-safe, POJO/record | từng giá trị một |
| Relaxed binding | ✅ đầy đủ | hạn chế (chỉ nên dùng dạng kebab-case canonical) |
| Validation (JSR-380) | ✅ với `@Validated` → **fail-fast lúc startup** | ❌ |
| Metadata cho IDE (autocomplete) | ✅ với `spring-boot-configuration-processor` | ❌ |
| SpEL | ❌ | ✅ |
| Immutable | ✅ constructor binding/record | field injection |

Spring Boot 3: constructor binding là **mặc định** khi class có đúng một constructor có tham số (không cần `@ConstructorBinding` như Boot 2.2–2.7); annotation `@ConstructorBinding` chuyển package sang `org.springframework.boot.context.properties.bind` và chỉ cần khi có nhiều constructor.

> ⚠️ **Lỗi thường gặp:** `@Value("${app.timeout}")` thiếu property → app không khởi động (đúng!), nhưng `@Value("${app.timeout:}")` với default rỗng che giấu lỗi cấu hình; inject `Duration` bằng `@Value` với giá trị `5000` mà không có đơn vị — `@ConfigurationProperties` hiểu mặc định là ms (hoặc theo `@DurationUnit`), còn đọc tay dễ nhầm giây/ms.

### 🛠 Bài tập phần 10

**Bài 10.1 — Thứ tự ghi đè (Cơ bản)**
- Đề bài: đặt `app.greeting` ở 5 nơi: `application.yml` trong jar, `application-dev.yml`, file `./config/application.yml` cạnh jar, env var, command-line. Lần lượt bỏ từng nguồn và ghi lại giá trị hiển thị qua endpoint `/greeting`.
- Tiêu chí đạt: bảng kết quả khớp với thứ tự lý thuyết; tra `/actuator/env/app.greeting` xem nguồn đang thắng.

**Bài 10.2 — Typed properties có validation (Trung bình)**
- Đề bài: chuyển 8 `@Value` rải rác của một module mail sang 1 record `MailProperties` có validation (`host` not blank, `port` 1–65535, `from` là email hợp lệ, `timeouts` kiểu `Duration`).
- Tiêu chí đạt: cấu hình sai `port: 70000` → app fail-fast với thông báo rõ ràng; có metadata (`META-INF/spring-configuration-metadata.json` được sinh ra).

**Bài 10.3 — Secret từ configtree (Nâng cao)**
- Đề bài: giả lập Kubernetes Secret bằng thư mục `/tmp/secrets/db/password` (file chứa password). Dùng `spring.config.import=optional:configtree:/tmp/secrets/` để bind vào `spring.datasource.password`.
- Tiêu chí đạt: password không xuất hiện trong bất kỳ file cấu hình nào của repo; `/actuator/env` hiển thị `******` (sanitize); giải thích `management.endpoint.env.show-values` (Boot 3: mặc định `never`).

<details>
<summary>Gợi ý lời giải</summary>

- Bài 10.1: command-line > env var > `./config/application.yml` (ngoài jar) > `application-dev.yml` (trong jar, nếu profile dev active) > `application.yml` trong jar.
- Bài 10.3: configtree ánh xạ đường dẫn `db/password` thành property `db.password` → trong `application.yml` khai báo `spring.datasource.password: ${db.password}`.
</details>

---

<a id="p11"></a>
## 11. Luồng khởi động SpringApplication.run & viết custom starter

### 11.1 SpringApplication.run — từng bước
```
new SpringApplication(primarySources)
  ├─ deduce WebApplicationType (SERVLET / REACTIVE / NONE) từ classpath
  ├─ nạp BootstrapRegistryInitializer, ApplicationContextInitializer, ApplicationListener
  │    từ META-INF/spring.factories (spring.factories vẫn dùng cho các extension point này!)
  └─ deduce main class

run(args)
  1. tạo DefaultBootstrapContext; lấy SpringApplicationRunListeners (EventPublishingRunListener)
  2. → ApplicationStartingEvent
  3. prepareEnvironment: tạo Environment, thêm command-line args, chạy EnvironmentPostProcessor
       (ConfigDataEnvironmentPostProcessor nạp application.yml, profile...)
     → ApplicationEnvironmentPreparedEvent (LoggingApplicationListener khởi tạo logging ở đây)
  4. in Banner
  5. createApplicationContext (vd AnnotationConfigServletWebServerApplicationContext)
  6. prepareContext: áp ApplicationContextInitializer → ApplicationContextInitializedEvent
       → đăng ký primary source (class main) làm bean definition → ApplicationPreparedEvent
  7. refreshContext → AbstractApplicationContext.refresh():
       prepareBeanFactory → postProcessBeanFactory
       → invokeBeanFactoryPostProcessors (ConfigurationClassPostProcessor: component scan,
            @Import, auto-configuration được xử lý tại đây)
       → registerBeanPostProcessors → initMessageSource → initApplicationEventMulticaster
       → onRefresh() (ServletWebServerApplicationContext TẠO embedded Tomcat ở đây)
       → registerListeners
       → finishBeanFactoryInitialization (pre-instantiate mọi singleton non-lazy)
       → finishRefresh (LifecycleProcessor start SmartLifecycle — web server bắt đầu nhận
            request; ContextRefreshedEvent)
  8. afterRefresh → ApplicationStartedEvent (+ AvailabilityChangeEvent LivenessState.CORRECT)
  9. callRunners: ApplicationRunner & CommandLineRunner (theo @Order)
 10. → ApplicationReadyEvent (+ ReadinessState.ACCEPTING_TRAFFIC)
  Nếu lỗi bất kỳ bước nào → ApplicationFailedEvent, FailureAnalyzer in thông báo thân thiện, context đóng
```

> 💡 **Góc nhìn Senior:**
> - `CommandLineRunner` chạy *trước* khi readiness = ACCEPTING_TRAFFIC; một runner chạy lâu (migrate dữ liệu) sẽ trì hoãn readiness — đó có thể là điều bạn muốn (không nhận traffic khi chưa sẵn sàng), nhưng nhớ chỉnh `startupProbe`/`initialDelaySeconds` trên Kubernetes.
> - Để đo thời gian startup từng bước: `SpringApplication.setApplicationStartup(new BufferingApplicationStartup(2048))` + endpoint `/actuator/startup`, hoặc `FlightRecorderApplicationStartup` với JFR.
> - `FailureAnalyzer` tùy chỉnh (đăng ký qua `spring.factories`) giúp team hiểu lỗi cấu hình nhanh — điểm cộng khi viết library nội bộ.

### 11.2 Viết custom starter
Quy ước (Spring Boot reference, mục "Creating Your Own Auto-configuration"):
- Hai module: `acme-spring-boot-autoconfigure` (code auto-config) và `acme-spring-boot-starter` (pom rỗng kéo autoconfigure + dependency cần thiết). Starter đơn giản có thể gộp làm một.
- Tên: `xxx-spring-boot-starter`. Tiền tố `spring-boot-starter-*` dành riêng cho starter chính thức.
- Property prefix riêng (`acme.*`), không dùng namespace của Spring Boot.
- Mọi bean `@ConditionalOnMissingBean` để người dùng override được; dùng `@ConditionalOnClass` cho dependency optional (khai báo `<optional>true</optional>`).

```java
// acme-spring-boot-autoconfigure
@ConfigurationProperties("acme.audit")
public record AuditProperties(@DefaultValue("true") boolean enabled,
                              @DefaultValue("audit") String topic) {}

@AutoConfiguration
@ConditionalOnClass(AuditClient.class)
@ConditionalOnProperty(prefix = "acme.audit", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(AuditProperties.class)
public class AuditAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    AuditClient auditClient(AuditProperties props) {
        return new AuditClient(props.topic());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    AuditFilter auditFilter(AuditClient client) { return new AuditFilter(client); }
}
```
```
# src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
com.acme.audit.autoconfigure.AuditAutoConfiguration
```
Test với `ApplicationContextRunner` (nhanh, không khởi động server):
```java
class AuditAutoConfigurationTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AuditAutoConfiguration.class));

    @Test void createsClientByDefault() {
        runner.run(ctx -> assertThat(ctx).hasSingleBean(AuditClient.class));
    }
    @Test void backsOffWhenUserDefinesClient() {
        runner.withBean(AuditClient.class, () -> new AuditClient("custom"))
              .run(ctx -> assertThat(ctx.getBean(AuditClient.class).topic()).isEqualTo("custom"));
    }
    @Test void disabledByProperty() {
        runner.withPropertyValues("acme.audit.enabled=false")
              .run(ctx -> assertThat(ctx).doesNotHaveBean(AuditClient.class));
    }
    @Test void backsOffWhenClassMissing() {
        runner.withClassLoader(new FilteredClassLoader(AuditClient.class))
              .run(ctx -> assertThat(ctx).doesNotHaveBean(AuditClient.class));
    }
}
```

> ⚠️ **Lỗi thường gặp:** đặt class auto-config trong package được component scan của app → nó bị nạp như user config, `@ConditionalOnMissingBean` chạy sai thứ tự; `@ComponentScan` bên trong auto-config (kéo theo bean ngoài ý muốn); quên khai báo `spring-boot-autoconfigure-processor` và `spring-boot-configuration-processor` (mất metadata, startup chậm hơn).

### 🛠 Bài tập phần 11

**Bài 11.1 — Lắng nghe các event khởi động (Cơ bản)**
- Đề bài: viết `ApplicationListener<SpringApplicationEvent>` in tên event + timestamp. Vì sao listener của `ApplicationStartingEvent` phải đăng ký qua `SpringApplication.addListeners(...)` hoặc `spring.factories` thay vì `@Component`?
- Tiêu chí đạt: in đủ chuỗi event; trả lời đúng câu hỏi (context chưa tồn tại lúc đó).

**Bài 11.2 — Custom starter "request-logging" (Trung bình)**
- Đề bài: viết starter `reqlog-spring-boot-starter` tự đăng ký servlet `Filter` log method, URI, status, thời gian; property `reqlog.enabled`, `reqlog.exclude-paths` (list), `reqlog.slow-threshold` (Duration).
- Tiêu chí đạt: 4 test `ApplicationContextRunner`/`WebApplicationContextRunner` như ví dụ; dùng trong một app demo chỉ bằng cách thêm dependency.

**Bài 11.3 — FailureAnalyzer (Nâng cao)**
- Đề bài: trong starter trên, nếu `reqlog.slow-threshold` âm thì ném `InvalidReqlogConfigException`; viết `AbstractFailureAnalyzer<InvalidReqlogConfigException>` in mô tả + hành động khắc phục.
- Tiêu chí đạt: output khi startup fail có khối `APPLICATION FAILED TO START` với Description/Action của bạn.

<details>
<summary>Gợi ý lời giải</summary>

- Bài 11.2: đăng ký filter bằng `FilterRegistrationBean<ReqLogFilter>` để set order và URL pattern; dùng `@ConditionalOnWebApplication(type = SERVLET)`.
- Bài 11.3: đăng ký trong `META-INF/spring.factories`:
```
org.springframework.boot.diagnostics.FailureAnalyzer=com.acme.reqlog.ReqlogFailureAnalyzer
```
</details>

---

<a id="p12"></a>
## 12. Actuator, logging & observability

### 12.1 Actuator
`spring-boot-starter-actuator` cung cấp endpoint vận hành. Các endpoint chính: `health`, `info`, `metrics`, `prometheus` (cần `micrometer-registry-prometheus`), `env`, `configprops`, `beans`, `conditions`, `loggers` (đổi log level runtime), `threaddump`, `heapdump`, `mappings`, `scheduledtasks`, `startup`, `shutdown` (mặc định disabled).

**Exposure mặc định:** qua HTTP chỉ có `health` (từ Boot 2.5, `info` không còn được expose mặc định); qua JMX thì Boot 3 cũng chỉ expose `health`.

```yaml
management:
  server:
    port: 8081                  # tách cổng quản trị khỏi cổng public
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,loggers
  endpoint:
    health:
      show-details: when-authorized   # Boot 3 mặc định "never"
      probes:
        enabled: true                 # tự bật khi chạy trên Kubernetes
      group:
        readiness:
          include: readinessState, db, redis
  info:
    git:
      mode: simple
  metrics:
    tags:
      application: ${spring.application.name}
```

**Health & probes:** `/actuator/health/liveness` (app có còn "sống" không — fail thì K8s restart pod) và `/actuator/health/readiness` (có nhận traffic được không — fail thì bị rút khỏi Service).

> 💡 **Góc nhìn Senior — bẫy production kinh điển:** **đừng** đưa health check của dependency ngoài (DB, Redis, API đối tác) vào **liveness**. Khi DB chập chờn, mọi pod fail liveness → K8s restart hàng loạt → "thundering herd" làm DB sập hẳn. Dependency ngoài chỉ nên ảnh hưởng **readiness** (hoặc không ảnh hưởng gì, tùy chiến lược degrade).

**Health indicator tùy chỉnh:**
```java
@Component
class PaymentGatewayHealthIndicator implements HealthIndicator {
    private final PaymentClient client;
    PaymentGatewayHealthIndicator(PaymentClient client) { this.client = client; }

    @Override
    public Health health() {
        try {
            long latency = client.ping();
            return Health.up().withDetail("latencyMs", latency).build();
        } catch (Exception e) {
            return Health.down(e).build(); // cẩn thận: không lộ thông tin nhạy cảm trong details
        }
    }
}
```

**Bảo mật Actuator:**
- `env`, `configprops`, `heapdump`, `threaddump`, `beans` có thể lộ secret/PII — `heapdump` chứa *toàn bộ bộ nhớ*, kể cả password và token. Từng có rất nhiều sự cố lộ dữ liệu do expose `/actuator/heapdump` ra Internet.
- Biện pháp: expose tối thiểu, chạy trên `management.server.port` riêng không public qua ingress, bảo vệ bằng Spring Security:

```java
@Bean
@Order(1)
SecurityFilterChain actuatorChain(HttpSecurity http) throws Exception {
    http.securityMatcher(EndpointRequest.toAnyEndpoint())
        .authorizeHttpRequests(a -> a
            .requestMatchers(EndpointRequest.to(HealthEndpoint.class, InfoEndpoint.class)).permitAll()
            .anyRequest().hasRole("OPS"))
        .httpBasic(Customizer.withDefaults());
    return http.build();
}
```
Boot 3 mặc định sanitize **mọi** giá trị trong `/env` và `/configprops` (`show-values: never`).

### 12.2 Metrics với Micrometer
Micrometer là "SLF4J cho metrics": API trung lập (`Counter`, `Timer`, `Gauge`, `DistributionSummary`), backend tùy chọn (Prometheus, Datadog, OTLP...). Boot tự đo: HTTP server request (`http.server.requests`), JVM (memory, GC, threads), HikariCP pool, Tomcat, executor, cache...

```java
@Service
class CheckoutService {
    private final Counter failed;
    private final Timer timer;
    CheckoutService(MeterRegistry registry) {
        this.failed = Counter.builder("checkout.failed").tag("reason", "payment").register(registry);
        this.timer = Timer.builder("checkout.duration").publishPercentileHistogram().register(registry);
    }
    void checkout(Cart cart) {
        timer.record(() -> { /* ... */ });
    }
}
```

> ⚠️ **Lỗi thường gặp — cardinality explosion:** gắn tag có giá trị không giới hạn (`userId`, `orderId`, URL thô có id) → mỗi giá trị là một time series → Prometheus OOM, chi phí tăng vọt. Tag phải thuộc tập nhỏ hữu hạn. (Boot dùng URI *template* `/orders/{id}` cho `http.server.requests` vì lý do này.)

### 12.3 Observability trong Boot 3
- **Micrometer Observation API** (`Observation`, `@Observed`): một lần instrument → sinh cả metrics *và* tracing span.
- **Micrometer Tracing** thay thế **Spring Cloud Sleuth** (Sleuth không hỗ trợ Boot 3). Bridge: Brave hoặc OpenTelemetry; exporter Zipkin/OTLP.
- `management.tracing.sampling.probability` (mặc định 0.1 = 10%). Trace id/span id tự đưa vào MDC để log correlation.
- Context propagation qua `@Async`, Reactor: cần `ContextPropagatingTaskDecorator` / Micrometer Context Propagation.

### 12.4 Logging
- Mặc định: SLF4J + **Logback**; có thể đổi sang Log4j2 (`spring-boot-starter-log4j2`, exclude `spring-boot-starter-logging`).
- Cấu hình nhanh: `logging.level.root=INFO`, `logging.level.org.hibernate.SQL=DEBUG`, `logging.file.name`, `logging.logback.rollingpolicy.*`, log groups (`logging.group.web=...`).
- Dùng `logback-spring.xml` (không phải `logback.xml`) để có `<springProfile name="prod">` và `<springProperty>` — vì `logback.xml` được Logback nạp **trước** khi Spring kịp can thiệp.
- Boot 3.4+: **structured logging** tích hợp: `logging.structured.format.console=ecs` (hoặc `gelf`, `logstash`) — JSON logs không cần encoder ngoài.
- Đổi level runtime: `POST /actuator/loggers/com.example` với `{"configuredLevel":"DEBUG"}`.

> 💡 **Góc nhìn Senior:** log ở mức DEBUG với string concatenation (`log.debug("x=" + obj)`) vẫn tốn CPU dù không in — dùng placeholder `{}`. Log đồng bộ ra file trên disk chậm có thể chặn request thread — cân nhắc `AsyncAppender` (đánh đổi: có thể mất log khi crash, chọn `neverBlock`). Không log PII/token; cấu hình masking.

### 🛠 Bài tập phần 12

**Bài 12.1 — Actuator an toàn (Cơ bản)**
- Đề bài: bật Actuator, expose `health, info, prometheus, loggers`; `info` hiển thị version từ build (`springBoot { buildInfo() }` hoặc goal `build-info`) và git commit.
- Tiêu chí đạt: `/actuator/heapdump` trả 404; `/actuator/loggers` yêu cầu xác thực; `/actuator/health` public nhưng không show details cho anonymous.

**Bài 12.2 — Liveness/Readiness đúng cách (Trung bình)**
- Đề bài: viết `HealthIndicator` cho dịch vụ đối tác, chỉ đưa vào group `readiness`. Viết endpoint admin để chủ động chuyển `ReadinessState.REFUSING_TRAFFIC` (dùng `AvailabilityChangeEvent.publish`).
- Tiêu chí đạt: khi đối tác down, liveness vẫn `UP`, readiness `DOWN`; viết đoạn manifest K8s probe tương ứng.

**Bài 12.3 — Metrics nghiệp vụ & tracing (Nâng cao)**
- Đề bài: thêm `@Observed(name = "order.place")` cho method đặt hàng; cấu hình Micrometer Tracing + OTLP exporter tới Jaeger/Zipkin chạy bằng Docker; log có `traceId`.
- Tiêu chí đạt: thấy span trong UI; Prometheus có `order_place_seconds_count`; log pattern chứa traceId khớp với UI; giải thích vì sao cần bean `ObservedAspect` (AOP!).

<details>
<summary>Gợi ý lời giải</summary>

- Bài 12.1: `management.endpoint.health.show-details=when-authorized` + `management.endpoint.health.roles=OPS`.
- Bài 12.2: `management.endpoint.health.group.readiness.include=readinessState,partner`; `AvailabilityChangeEvent.publish(eventPublisher, this, ReadinessState.REFUSING_TRAFFIC)`.
- Bài 12.3: `@Observed` hoạt động qua aspect → cần `spring-boot-starter-aop` và bean `ObservedAspect(observationRegistry)` (các bản Boot mới hơn có property `management.observations.annotations.enabled=true` để tự cấu hình — kiểm tra reference của đúng phiên bản bạn dùng); self-invocation cũng làm mất observation.
</details>

---

<a id="p13"></a>
## 13. @Async, @Scheduled & cấu hình thread pool

### 13.1 @Async
```java
@Configuration
@EnableAsync
class AsyncConfig implements AsyncConfigurer {

    @Bean(name = "mailExecutor")
    ThreadPoolTaskExecutor mailExecutor() {
        var ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(4);
        ex.setMaxPoolSize(16);
        ex.setQueueCapacity(500);                    // BOUNDED queue
        ex.setThreadNamePrefix("mail-");
        ex.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy()); // backpressure
        ex.setWaitForTasksToCompleteOnShutdown(true);
        ex.setAwaitTerminationSeconds(20);
        ex.setTaskDecorator(new MdcTaskDecorator()); // truyền MDC/SecurityContext/trace
        ex.initialize();
        return ex;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) ->
                LoggerFactory.getLogger("async").error("Async {} failed", method.getName(), ex);
    }
}

@Service
class MailService {
    @Async("mailExecutor")
    public CompletableFuture<Void> sendWelcome(String email) {
        // ...
        return CompletableFuture.completedFuture(null);
    }
}

class MdcTaskDecorator implements TaskDecorator {
    @Override
    public Runnable decorate(Runnable task) {
        Map<String, String> ctx = MDC.getCopyOfContextMap();
        return () -> {
            Map<String, String> previous = MDC.getCopyOfContextMap();
            if (ctx != null) MDC.setContextMap(ctx); else MDC.clear();
            try { task.run(); }
            finally { if (previous != null) MDC.setContextMap(previous); else MDC.clear(); }
        };
    }
}
```

**Executor mặc định:**
- Spring Framework thuần (không Boot), không có `TaskExecutor` bean: `SimpleAsyncTaskExecutor` — **tạo thread mới cho mỗi task**, không pool → dưới tải cao có thể tạo hàng nghìn thread → OOM.
- Spring Boot: auto-config bean `applicationTaskExecutor` là `ThreadPoolTaskExecutor` với `core-size=8`, `max-size` và `queue-capacity` không giới hạn (Integer.MAX_VALUE). Vì queue **vô hạn**, `ThreadPoolExecutor` không bao giờ tạo thêm thread vượt core → thực tế **chỉ 8 thread**, task dồn hàng đợi vô hạn → latency tăng dần, có thể OOM, mất task khi restart. Cấu hình qua `spring.task.execution.pool.*`.
- Boot 3.2+ với `spring.threads.virtual.enabled=true`: `applicationTaskExecutor` là `SimpleAsyncTaskExecutor` dùng **virtual thread**.

> ⚠️ **Lỗi thường gặp với @Async:**
> - Self-invocation → chạy đồng bộ (Phần 7).
> - Method `private`/class không phải bean → không async.
> - Trả về `void` → exception chỉ đến `AsyncUncaughtExceptionHandler`; nếu không cấu hình → chỉ log. Trả `CompletableFuture` để caller xử lý lỗi.
> - Mất `SecurityContext`, MDC, transaction, request scope ở thread mới (dùng `TaskDecorator`, `DelegatingSecurityContextAsyncTaskExecutor`).
> - Hiểu sai `ThreadPoolExecutor`: thread mới (tới `max`) **chỉ** được tạo khi queue **đầy**. `core=4, max=50, queue=10000` → gần như luôn chỉ 4 thread.

### 13.2 @Scheduled
```java
@Configuration
@EnableScheduling
class SchedulingConfig { }

@Component
class Jobs {
    @Scheduled(fixedRate = 5_000)            // mỗi 5s tính từ lúc BẮT ĐẦU lần trước
    void heartbeat() { }

    @Scheduled(fixedDelay = 10, timeUnit = TimeUnit.SECONDS, initialDelay = 30) // tính từ lúc KẾT THÚC lần trước
    void pollOutbox() { }

    @Scheduled(cron = "${jobs.report.cron:0 0 2 * * *}", zone = "Asia/Ho_Chi_Minh") // giây phút giờ ngày tháng thứ
    void nightlyReport() { }
}
```
- Scheduler mặc định của Boot (`ThreadPoolTaskScheduler`) có **pool size = 1** → một job chậm/treo chặn **mọi** job khác. Tăng `spring.task.scheduling.pool.size` hoặc đẩy công việc nặng sang executor riêng.
- Cùng một job không chạy chồng lên chính nó trên cùng scheduler thread (fixedRate trễ thì lần sau chạy ngay sau khi lần trước xong), nhưng **nhiều instance** (scale ngang 3 pod) → job chạy 3 lần. Giải pháp: **ShedLock** (khóa phân tán qua DB/Redis), Quartz cluster mode, hoặc tách job sang một worker/K8s CronJob duy nhất.
- Exception trong `@Scheduled` được log và job vẫn tiếp tục lịch kế tiếp.

> 💡 **Góc nhìn Senior:** Định cỡ pool theo bản chất công việc: CPU-bound ≈ số core; I/O-bound ≈ `cores × (1 + wait/compute)` (công thức trong JCIP) — nhưng giới hạn thật thường nằm ở *tài nguyên hạ nguồn* (DB connection pool 10 thì 200 async thread chỉ làm tăng tranh chấp). Luôn: queue bounded, rejection policy có chủ đích, metrics executor (`executor.active`, `executor.queued` — Boot tự đăng ký cho executor auto-config), tên thread có ý nghĩa để đọc thread dump.

### 🛠 Bài tập phần 13

**Bài 13.1 — Bẫy queue vô hạn (Cơ bản)**
- Đề bài: tạo `ThreadPoolTaskExecutor` core=2, max=10, queue=Integer.MAX_VALUE; submit 100 task `sleep(1s)`; in số thread active mỗi giây. Lặp lại với queue=5.
- Tiêu chí đạt: giải thích vì sao lần 1 chỉ có 2 thread; lần 2 lên 10 thread và bắt đầu reject (hoặc caller-runs).

**Bài 13.2 — Context propagation (Trung bình)**
- Đề bài: trong request có `X-Correlation-Id` đưa vào MDC và user đã đăng nhập; gọi method `@Async` log ra correlationId và username.
- Tiêu chí đạt: trước khi sửa log thiếu thông tin; sau khi thêm `TaskDecorator` (MDC) và `DelegatingSecurityContextAsyncTaskExecutor` (hoặc decorator tương đương) thì đầy đủ.

**Bài 13.3 — Job an toàn khi scale ngang (Nâng cao)**
- Đề bài: job `@Scheduled` gửi báo cáo lúc 2h sáng; chạy 3 instance cùng lúc (3 port khác nhau, chung PostgreSQL). Tích hợp ShedLock (`@SchedulerLock(name="nightlyReport", lockAtMostFor="10m", lockAtLeastFor="1m")`).
- Tiêu chí đạt: chỉ 1 instance chạy job (đặt cron mỗi phút để test); giải thích ý nghĩa `lockAtMostFor` khi instance giữ lock bị crash, và vấn đề lệch đồng hồ giữa node.

<details>
<summary>Gợi ý lời giải</summary>

- Bài 13.1: thứ tự của `ThreadPoolExecutor.execute`: < core → tạo thread; ≥ core → offer vào queue; queue đầy → tạo thread tới max; vẫn đầy → reject.
- Bài 13.2: `ThreadPoolTaskExecutor` hỗ trợ `setTaskDecorator` (từ Spring 4.3) nhưng chỉ nhận **một** decorator — muốn truyền cả MDC lẫn trace context thì tự lồng decorator (hoặc dùng `CompositeTaskDecorator` nếu phiên bản Spring của bạn có). Security: `SecurityContextHolder` dùng ThreadLocal (xem Module 08).
- Bài 13.3: ShedLock dùng bảng `shedlock(name, lock_until, locked_at, locked_by)`; `lockAtMostFor` phải lớn hơn thời gian chạy bình thường của job để không bị instance khác "cướp" lock khi job còn đang chạy.
</details>

---

<a id="p14"></a>
## 14. Graceful shutdown, Spring Boot 3, native image & virtual threads

### 14.1 Graceful shutdown
Khi pod nhận `SIGTERM` (rolling deploy, scale-down), ứng dụng cần: ngừng nhận request mới, hoàn tất request đang xử lý, đóng tài nguyên theo thứ tự.

```yaml
server:
  shutdown: graceful                      # Boot 3.4+ đã là mặc định; Boot 2.3–3.3 mặc định "immediate"
spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s       # mặc định 30s
```
Trình tự: JVM shutdown hook → `context.close()` → `ContextClosedEvent` → readiness chuyển `REFUSING_TRAFFIC` → `SmartLifecycle.stop()` theo phase (web server ngừng accept, chờ request đang chạy trong giới hạn timeout; message listener container dừng) → destroy singleton (`@PreDestroy`, đóng `DataSource`, executor `waitForTasksToCompleteOnShutdown`).

> 💡 **Góc nhìn Senior — Kubernetes:** có một "race" giữa việc K8s gửi SIGTERM và việc endpoint của pod bị gỡ khỏi Service/Ingress (lan truyền mất vài giây). Nếu app đóng ngay, request vẫn đang được route tới sẽ lỗi 502. Giải pháp phổ biến: `preStop` hook `sleep 5–10s` trước khi SIGTERM tới app; đảm bảo `terminationGracePeriodSeconds` > preStop + `timeout-per-shutdown-phase`. Các executor tự tạo cũng phải cấu hình await termination, nếu không task dở dang bị mất.

### 14.2 Những thay đổi lớn của Spring Boot 3 / Spring Framework 6
| Chủ đề | Boot 2.x | Boot 3.x |
|---|---|---|
| Java baseline | Java 8+ | **Java 17+** |
| Jakarta EE | `javax.*` (Java EE 8) | **`jakarta.*`** (Jakarta EE 9/10): `jakarta.servlet`, `jakarta.persistence`, `jakarta.validation`, `jakarta.annotation` |
| Auto-config registration | `spring.factories` | `AutoConfiguration.imports` (từ 2.7) |
| Tracing | Spring Cloud Sleuth | Micrometer Tracing / Observation API |
| Native | Spring Native (experimental) | **GraalVM native image & AOT** chính thức |
| Error response | tự định nghĩa | `ProblemDetail` (RFC 7807/9457) — Module 08 |
| HTTP client mới | `RestTemplate`, `WebClient` | + HTTP interface (`@HttpExchange`), `RestClient` (6.1/Boot 3.2) |
| Security | `WebSecurityConfigurerAdapter` | bị **xóa** (Security 6) → `SecurityFilterChain` bean |
| URL matching | trailing slash match mặc định | `/users/` **không** còn match `/users` mặc định |
| Hibernate | 5.x | 6.x |

Migration: dùng **OpenRewrite** recipe `org.openrewrite.java.spring.boot3.UpgradeSpringBoot_3_0` để tự đổi `javax` → `jakarta` và nhiều API; cẩn thận với library bên thứ ba chưa hỗ trợ Jakarta (lỗi `ClassNotFoundException: javax.servlet.Filter`). Boot 2.x OSS đã hết hỗ trợ (2.7 hết OSS support từ cuối 2023), nên migration là chủ đề phỏng vấn rất hay hỏi.

### 14.3 GraalVM native image & AOT
- **AOT processing** (lúc build): Spring chạy phần phân tích context *khi build*, sinh mã Java thay cho reflection (bean definition dưới dạng code), và sinh **hints** (reflection, resource, proxy, serialization) cho GraalVM.
- **Native image**: compile ahead-of-time thành binary → startup tính bằng **mili giây**, RSS thấp; đánh đổi: build lâu (vài phút, nhiều RAM), peak throughput có thể thấp hơn JIT (không có profile-guided optimization nếu không dùng PGO), debug/profiling khó hơn.
- **Closed-world assumption**: classpath cố định lúc build; `@Profile`, `@ConditionalOnProperty` được đánh giá **lúc build** → không thể đổi profile kích hoạt bean lúc runtime; reflection/dynamic proxy/resource phải khai báo qua `RuntimeHintsRegistrar`, `@RegisterReflectionForBinding`, `@ImportRuntimeHints`.
- Lệnh: `./mvnw -Pnative native:compile` hoặc `./mvnw spring-boot:build-image -Pnative` (Buildpacks).
- Phù hợp: serverless/FaaS, CLI, scale-to-zero, sidecar nhỏ. Không phù hợp: app dùng nhiều reflection động/library không hỗ trợ, service chạy lâu cần throughput đỉnh. Lựa chọn trung gian: **CDS/AppCDS** (Boot 3.3 hỗ trợ dễ hơn) và **CRaC** (checkpoint/restore, Boot 3.2+).

### 14.4 Virtual threads (Boot 3.2+, Java 21+)
```yaml
spring:
  threads:
    virtual:
      enabled: true
  main:
    keep-alive: true   # virtual thread là daemon; app chỉ có scheduler có thể tự thoát
```
Khi bật, Boot: cấu hình Tomcat/Jetty xử lý request bằng virtual thread; `applicationTaskExecutor` (cho `@Async`, Spring MVC async) và scheduler dùng virtual thread; một số integration (Kafka/RabbitMQ listener, Redis...) cũng chuyển sang.

Ý nghĩa: code **blocking** truyền thống (JDBC, RestClient) vẫn scale tới hàng chục nghìn request đồng thời mà không cần reactive — virtual thread bị "unmount" khỏi carrier thread khi block I/O.

> ⚠️ **Lỗi thường gặp / trade-off:**
> - **Pinning**: trên JDK 21–23, virtual thread block bên trong `synchronized` (hoặc native frame) bị "ghim" vào carrier thread → mất lợi ích, thậm chí deadlock khi carrier pool cạn. JDK 24 (JEP 491) đã xử lý phần lớn trường hợp `synchronized`. Chẩn đoán: `-Djdk.tracePinnedThreads=full` (JDK 21–23) hoặc JFR event `jdk.VirtualThreadPinned`.
> - Virtual thread **không làm tài nguyên hạ nguồn nhiều hơn**: 10.000 request đồng thời vẫn tranh nhau 10 connection HikariCP → timeout. Cần giới hạn đồng thời (semaphore, bulkhead) thay vì giới hạn bằng kích thước thread pool như trước.
> - Không pool virtual thread; `ThreadLocal` nặng × hàng triệu thread → tốn bộ nhớ (Scoped Values là hướng thay thế).
> - CPU-bound work không nhanh hơn.

### 🛠 Bài tập phần 14

**Bài 14.1 — Kiểm chứng graceful shutdown (Cơ bản)**
- Đề bài: endpoint `/slow` sleep 15s. Gọi endpoint, ngay lập tức gửi `kill -TERM <pid>`. So sánh `server.shutdown=immediate` và `graceful`.
- Tiêu chí đạt: với graceful, request trả 200 và log cho thấy server chờ; request mới gửi trong lúc shutdown bị từ chối.

**Bài 14.2 — Migration Boot 2.7 → 3.x (Trung bình)**
- Đề bài: lấy một project Boot 2.7 nhỏ (web + JPA + security + validation), migrate lên Boot 3 bằng OpenRewrite rồi sửa tay phần còn lại.
- Tiêu chí đạt: danh sách mọi thay đổi (javax→jakarta, `SecurityFilterChain`, trailing slash, Hibernate 6 query changes, property bị đổi tên — dùng `spring-boot-properties-migrator` để phát hiện); toàn bộ test xanh.

**Bài 14.3 — Benchmark virtual threads (Nâng cao)**
- Đề bài: endpoint gọi một API giả lập độ trễ 200ms (WireMock hoặc endpoint khác) bằng `RestClient`. Load test (k6/Gatling/wrk) với 2000 concurrent users: (a) Tomcat thread pool mặc định 200; (b) `spring.threads.virtual.enabled=true`. Sau đó thêm truy vấn DB với Hikari `maximum-pool-size=10`.
- Tiêu chí đạt: bảng throughput/p99 latency; giải thích vì sao (b) thắng ở bài toán I/O thuần nhưng khi thêm DB thì nút thắt chuyển sang connection pool; tái hiện pinning bằng `synchronized` (JDK 21) và quan sát bằng JFR.

<details>
<summary>Gợi ý lời giải</summary>

- Bài 14.1: log `Commencing graceful shutdown. Waiting for active requests to complete` và `Graceful shutdown complete`.
- Bài 14.2: thêm tạm `spring-boot-properties-migrator` (runtime) để có báo cáo property đổi tên lúc startup, sau đó gỡ bỏ.
- Bài 14.3: với (a), Little's Law: throughput tối đa ≈ 200 threads / 0.2s = 1000 req/s; (b) không bị giới hạn bởi thread → tới giới hạn CPU/mạng. Khi thêm DB: throughput ≈ 10 conn / thời gian query.
</details>

---

<a id="du-an-mini"></a>
## Dự án mini

### "Order Platform Starter Kit" — 2 ngày

Xây dựng một **custom starter nội bộ** + một **ứng dụng demo** sử dụng nó, bao phủ toàn bộ kiến thức module.

**Yêu cầu chức năng**
1. Module `platform-spring-boot-starter` cung cấp:
   - `@ConfigurationProperties("platform")` (record, có validation) cấu hình: `correlation.header-name`, `audit.enabled`, `async.core-size/max-size/queue-capacity`.
   - Filter correlation-id (đọc/sinh header, đưa vào MDC, trả lại trong response header).
   - Executor `platformTaskExecutor` có bounded queue, `TaskDecorator` truyền MDC; metrics executor qua Micrometer.
   - Aspect `@Audited` ghi audit (method, user, args đã mask, kết quả, thời gian) qua interface `AuditSink` (mặc định log; app có thể override bean).
   - `HealthIndicator` cho một dependency giả lập, chỉ thuộc readiness group.
   - Đăng ký qua `AutoConfiguration.imports`, mọi bean `@ConditionalOnMissingBean`.
2. Ứng dụng `order-service` (Boot 3.x, Java 21):
   - REST tối thiểu: tạo đơn hàng, xem đơn hàng (chi tiết REST ở Module 08).
   - `OrderService.place()` là `@Transactional`, `@Audited`, publish `OrderPlacedEvent`; listener `@TransactionalEventListener` + `@Async` gửi "email" (log).
   - Job `@Scheduled` dọn đơn hàng `PENDING` quá 30 phút, có ShedLock.
   - Profile `local` (H2) và `prod` (PostgreSQL qua env var); không có secret trong repo.

**Yêu cầu phi chức năng**
- Startup không có cảnh báo "not eligible for auto-proxying"; không circular dependency (giữ `allow-circular-references=false`).
- Graceful shutdown được cấu hình và kiểm chứng; executor chờ task hoàn tất.
- Actuator: chỉ `health`, `info`, `prometheus` public trên management port riêng; `loggers` yêu cầu role `OPS`.
- Hỗ trợ bật/tắt virtual threads bằng property, có ghi chú kết quả benchmark ngắn.
- Test: `ApplicationContextRunner` cho starter (≥ 6 case: default, override, disabled, missing class, invalid properties, web vs non-web); `@SpringBootTest` cho luồng đặt hàng (event chỉ gửi sau commit; rollback thì không gửi).

**Tiêu chí chấm (100 điểm)**
| Tiêu chí | Điểm |
|---|---|
| Starter đúng quy ước (naming, imports file, conditional, metadata, back-off) | 20 |
| Lifecycle/AOP đúng: không self-invocation, thứ tự aspect hợp lý (audit bọc ngoài transaction và giải thích lý do), proxy hoạt động | 20 |
| Cấu hình externalized, profile, validation fail-fast, không lộ secret | 15 |
| Async/scheduling an toàn: bounded queue, context propagation, ShedLock, xử lý exception | 15 |
| Actuator & observability (probes đúng, metrics không high-cardinality, trace id trong log) | 15 |
| Chất lượng test & README giải thích quyết định thiết kế (trade-off) | 15 |

---

<a id="checklist-tu-danh-gia"></a>
## Checklist tự đánh giá
- [ ] Tôi giải thích được IoC/DI và lý do chọn constructor injection, kể được ít nhất 4 nhược điểm của field injection.
- [ ] Tôi phân biệt được `BeanFactory` và `ApplicationContext`, giải thích pha đăng ký `BeanDefinition` và pha khởi tạo bean.
- [ ] Tôi giải thích được `@Configuration` full mode (CGLIB, `proxyBeanMethods`) vs lite mode và lỗi gọi `@Bean` method trong lite mode.
- [ ] Tôi chọn đúng `@Primary`, `@Qualifier`, `List<T>`/`Map<String,T>`, `ObjectProvider<T>` cho từng tình huống.
- [ ] Tôi kể được các scope, và 3 cách tiêm prototype/request scope vào singleton cùng bẫy scoped-proxy + prototype.
- [ ] Tôi vẽ lại được toàn bộ bean lifecycle (Aware, BPP before/after, `@PostConstruct`, `afterPropertiesSet`, init-method, destroy) và chỉ ra nơi AOP proxy được tạo.
- [ ] Tôi phân biệt được `BeanFactoryPostProcessor` vs `BeanPostProcessor`, biết vì sao BFPP `@Bean` nên `static`.
- [ ] Tôi giải thích được 3-level cache, vì sao cần cấp 3 (AOP proxy sớm), vì sao constructor cycle không giải được và vì sao Boot 2.6+ cấm cycle mặc định.
- [ ] Tôi giải thích được JDK proxy vs CGLIB, self-invocation, và hệ quả với `@Transactional`, `@Cacheable`, `@Async`, `@PreAuthorize`.
- [ ] Tôi sắp được thứ tự aspect (retry ngoài transaction) và dùng `@Order` đúng.
- [ ] Tôi dùng được `@EventListener` vs `@TransactionalEventListener`, hiểu giới hạn bền vững của event in-memory và pattern Outbox.
- [ ] Tôi mô tả được cơ chế auto-configuration (`AutoConfiguration.imports`, `DeferredImportSelector`, `@ConditionalOn*`) và debug bằng condition report.
- [ ] Tôi thuộc thứ tự ưu tiên externalized configuration và cách override bằng env var trên Kubernetes.
- [ ] Tôi dùng `@ConfigurationProperties` (record, validation, metadata) thay cho `@Value` rải rác.
- [ ] Tôi mô tả được các bước của `SpringApplication.run` và các event khởi động.
- [ ] Tôi tự viết và test được một custom starter bằng `ApplicationContextRunner`.
- [ ] Tôi cấu hình Actuator an toàn, phân biệt liveness/readiness, tránh metrics high-cardinality.
- [ ] Tôi cấu hình đúng thread pool cho `@Async`/`@Scheduled`, hiểu bẫy queue vô hạn và scheduler 1 thread, biết chạy job an toàn khi scale ngang.
- [ ] Tôi cấu hình graceful shutdown phối hợp với Kubernetes (preStop, grace period).
- [ ] Tôi liệt kê được thay đổi chính của Boot 3 (jakarta, Java 17, AOT/native, Micrometer Observation) và trade-off của native image, virtual threads (pinning, nút thắt tài nguyên).

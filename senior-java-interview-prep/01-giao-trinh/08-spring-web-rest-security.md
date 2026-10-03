# Module 08 — Spring MVC, REST API & Spring Security

> **Mục tiêu:** sau module này bạn giải thích được một HTTP request đi qua Servlet container → Filter → `DispatcherServlet` → controller → message converter như thế nào; thiết kế được REST API chuẩn (status code, idempotency, pagination, versioning, ETag, idempotency key, `ProblemDetail`); chọn và cấu hình đúng HTTP client (timeout, connection pool); biết khi nào WebFlux đáng dùng so với MVC + virtual threads; nắm kiến trúc Spring Security (filter chain, authentication, authorization, `SecurityContextHolder`), triển khai được JWT/OAuth2 Resource Server, method security; nhận diện và phòng tránh các lỗ hổng phổ biến trong OWASP API Security Top 10.
> **Cấp độ:** Cơ bản → Nâng cao (Senior)
> **Thời lượng gợi ý:** 7 ngày (≈ 35–40 giờ)
> **Yêu cầu trước:** Module 07 (Spring Core & Boot — đặc biệt bean lifecycle, AOP proxy), kiến thức HTTP cơ bản, Module Concurrency (ThreadLocal, thread pool)
> **Nguồn tham khảo:**
> - Trong kho: [`Ebook IT/Newman_Building-Microservices.pdf`](../../Ebook%20IT/Newman_Building-Microservices.pdf) — các chương về integration (REST, versioning, breaking changes) và security (authentication/authorization giữa service)
> - Trong kho: [`Ebook IT/Design Pattern.pdf`](../../Ebook%20IT/Design%20Pattern.pdf) — Chain of Responsibility (filter chain), Front Controller/Strategy (DispatcherServlet, HandlerAdapter), Adapter
> - Ngoài: Spring Framework Reference — Web on Servlet Stack: https://docs.spring.io/spring-framework/reference/web/webmvc.html ; Web Reactive: https://docs.spring.io/spring-framework/reference/web/webflux.html ; REST Clients: https://docs.spring.io/spring-framework/reference/integration/rest-clients.html
> - Ngoài: Spring Security Reference (Servlet Applications — Architecture, Authentication, Authorization, OAuth2, Exploits/CSRF): https://docs.spring.io/spring-security/reference/
> - Ngoài: Spring Boot Reference — Web, Security, HTTP clients: https://docs.spring.io/spring-boot/reference/
> - Ngoài: RFC 9110 (HTTP Semantics), RFC 9457 (Problem Details, thay RFC 7807), RFC 7519 (JWT), RFC 6749 (OAuth 2.0), RFC 7636 (PKCE), RFC 9700 (OAuth 2.0 Security BCP), OpenID Connect Core 1.0
> - Ngoài: OWASP API Security Top 10 (2023): https://owasp.org/API-Security/editions/2023/en/0x11-t10/ ; OWASP Password Storage Cheat Sheet
> - Sách: Craig Walls — *Spring in Action* (6th ed.) — Part 2 (Spring MVC, REST, Security); Baeldung chỉ dùng làm nguồn phụ.

## Mục lục
1. [Servlet container, Filter vs Interceptor vs AOP](#p1)
2. [DispatcherServlet — luồng xử lý request](#p2)
3. [Controller, data binding & validation](#p3)
4. [Global exception handling & ProblemDetail](#p4)
5. [Thiết kế REST API (resource, method, status code, pagination)](#p5)
6. [REST nâng cao: versioning, idempotency key, ETag, HATEOAS, OpenAPI](#p6)
7. [HTTP clients: RestTemplate, WebClient, RestClient, HTTP interface](#p7)
8. [WebFlux & reactive vs MVC + virtual threads; CORS](#p8)
9. [Kiến trúc Spring Security](#p9)
10. [Authentication: UserDetailsService & password encoding](#p10)
11. [Session vs stateless JWT](#p11)
12. [OAuth2 / OpenID Connect & Resource Server](#p12)
13. [Authorization: URL & method security, CSRF](#p13)
14. [Lỗ hổng phổ biến trong REST API — OWASP API Top 10](#p14)
15. [Dự án mini của module](#du-an-mini)
16. [Checklist tự đánh giá](#checklist-tu-danh-gia)

---

<a id="p1"></a>
## 1. Servlet container, Filter vs Interceptor vs AOP

### 1.1 Servlet container
- **Servlet container** (Tomcat — mặc định trong Spring Boot, Jetty, Undertow) lắng nghe socket, parse HTTP thành `HttpServletRequest`/`HttpServletResponse`, chọn servlet theo URL mapping và gọi `service()`.
- Mô hình **thread-per-request**: mỗi request được xử lý trọn vẹn trên một thread của pool (Tomcat: `server.tomcat.threads.max=200` mặc định, `accept-count=100`, `max-connections=8192` với NIO connector). Khi 200 thread bận (vd chờ DB chậm), request mới xếp hàng → latency tăng → timeout. Boot 3.2+ với virtual threads thay pool bằng virtual thread per request.
- Spring Boot 3 dùng **Jakarta Servlet 6** (`jakarta.servlet.*`); Boot 2 dùng Servlet 4 (`javax.servlet.*`).
- Trong Spring MVC, gần như toàn bộ ứng dụng chạy sau **một** servlet duy nhất: `DispatcherServlet` (Front Controller pattern), mapping `/`.

### 1.2 Ba tầng "chặn" request
```
Client → [Tomcat] → Filter 1 → Filter 2 (Spring Security FilterChainProxy) → ... → DispatcherServlet
            → HandlerInterceptor.preHandle → Controller (proxy AOP → target) → postHandle → afterCompletion
```

| | Servlet `Filter` | Spring `HandlerInterceptor` | Spring AOP |
|---|---|---|---|
| Thuộc về | Servlet spec (container) | Spring MVC (`DispatcherServlet`) | Spring container |
| Phạm vi | **mọi** request vào container (kể cả static, error, request không có handler) | chỉ request được map tới handler | mọi method của bean (không chỉ web) |
| Biết handler/method controller? | Không | Có (`HandlerMethod`) | Có (join point) |
| Sửa/bọc request/response body | Có (wrapper, vd `ContentCachingRequestWrapper`) | Hạn chế | Không liên quan HTTP |
| Dùng cho | security, CORS, encoding, logging thô, compression, correlation id, rate limit | auth theo handler, locale, audit theo controller, timing | transaction, cache, metrics nghiệp vụ |

```java
// Filter: chạy 1 lần mỗi request (OncePerRequestFilter tránh chạy lại khi forward/error dispatch)
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class CorrelationIdFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String cid = Optional.ofNullable(req.getHeader("X-Correlation-Id"))
                .filter(s -> s.matches("[A-Za-z0-9-]{1,64}"))     // không tin input
                .orElse(UUID.randomUUID().toString());
        MDC.put("cid", cid);
        res.setHeader("X-Correlation-Id", cid);
        try {
            chain.doFilter(req, res);
        } finally {
            MDC.remove("cid");                                  // thread được tái sử dụng!
        }
    }
}

// Interceptor: biết handler nào sẽ xử lý
class TimingInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
        req.setAttribute("start", System.nanoTime());
        return true; // false = dừng chain, interceptor tự ghi response
    }
    @Override
    public void afterCompletion(HttpServletRequest req, HttpServletResponse res, Object handler, Exception ex) {
        long ms = (System.nanoTime() - (long) req.getAttribute("start")) / 1_000_000;
        if (handler instanceof HandlerMethod hm) {
            System.out.printf("%s.%s -> %d (%d ms)%n",
                    hm.getBeanType().getSimpleName(), hm.getMethod().getName(), res.getStatus(), ms);
        }
    }
}

@Configuration
class WebConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new TimingInterceptor()).addPathPatterns("/api/**");
    }
}
```

> 💡 **Góc nhìn Senior:** `postHandle` **không** được gọi nếu controller ném exception; dùng `afterCompletion` cho cleanup. Với `@ResponseBody`, response đã được ghi (commit) trước `postHandle` → không thể sửa header ở đó (dùng `ResponseBodyAdvice` thay thế). Filter là `@Component` sẽ được Boot **tự đăng ký** cho mọi URL — nếu bạn đồng thời đăng ký nó trong Spring Security chain, filter chạy **hai lần**. Dùng `FilterRegistrationBean` (`setEnabled(false)`) để chặn đăng ký tự động.

> ⚠️ **Lỗi thường gặp:** quên `MDC.remove`/`ThreadLocal.remove()` trong `finally` → dữ liệu của request trước rò sang request sau trên cùng thread (thread pool tái sử dụng). Đọc `request.getInputStream()` trong filter để log body → controller không đọc được nữa (stream chỉ đọc một lần) — dùng `ContentCachingRequestWrapper`.

### 🛠 Bài tập phần 1

**Bài 1.1 — Thứ tự thực thi (Cơ bản)**
- Đề bài: tạo 2 filter (`@Order(1)`, `@Order(2)`), 1 interceptor, 1 aspect `@Around` trên controller; mỗi cái log "vào/ra". Gọi một endpoint thành công và một endpoint ném exception.
- Tiêu chí đạt: vẽ sơ đồ thứ tự log; chỉ ra `postHandle` không chạy ở trường hợp lỗi.

**Bài 1.2 — Request/response logging (Trung bình)**
- Đề bài: viết filter log method, URI, status, thời gian, và body (giới hạn 2KB, che trường `password`, `cardNumber`) cho `/api/**`.
- Tiêu chí đạt: controller vẫn đọc được body; không log body của `multipart/form-data`; có test `MockMvc`.

**Bài 1.3 — Rate limiting filter (Nâng cao)**
- Đề bài: viết filter giới hạn 10 request/giây theo API key (header `X-API-Key`) dùng thuật toán token bucket tự cài đặt (thread-safe, không thư viện). Vượt hạn mức trả `429` + header `Retry-After`.
- Tiêu chí đạt: test đa luồng 50 request đồng thời chỉ ~10 thành công; giải thích vì sao bộ đếm in-memory sai khi có nhiều instance và hướng giải (Redis + Lua, Bucket4j, rate limit ở API Gateway).

<details>
<summary>Gợi ý lời giải</summary>

- Bài 1.2: bọc `new ContentCachingRequestWrapper(req)` / `ContentCachingResponseWrapper(res)`; sau `chain.doFilter` đọc `getContentAsByteArray()`, **nhớ** gọi `responseWrapper.copyBodyToResponse()`.
- Bài 1.3: mỗi key một bucket `AtomicLong` lưu (tokens, lastRefillNanos) hoặc `synchronized` trên object bucket; `ConcurrentHashMap.computeIfAbsent` để tạo bucket; giới hạn kích thước map (Caffeine) để tránh bị tấn công bằng hàng triệu key giả.
</details>

---

<a id="p2"></a>
## 2. DispatcherServlet — luồng xử lý request

### 2.1 Luồng chính (`DispatcherServlet.doDispatch`)
```
1. checkMultipart (MultipartResolver)
2. getHandler(): duyệt các HandlerMapping theo order
      RequestMappingHandlerMapping (@RequestMapping) → RouterFunctionMapping → SimpleUrlHandlerMapping (static resources)...
   → HandlerExecutionChain = handler (HandlerMethod) + danh sách HandlerInterceptor
   (không tìm thấy → 404; Spring 6.1+/Boot 3.2 ném NoResourceFoundException cho static resource)
3. getHandlerAdapter(): tìm adapter supports(handler) → RequestMappingHandlerAdapter
4. interceptors.preHandle()
5. ha.handle():
      - HandlerMethodArgumentResolver cho từng tham số:
          @RequestBody → RequestResponseBodyMethodProcessor → HttpMessageConverter (Jackson) đọc body
          @PathVariable, @RequestParam, @RequestHeader, @CookieValue, @ModelAttribute (data binding + WebDataBinder),
          Principal, Pageable (Spring Data), @AuthenticationPrincipal...
      - @Valid/@Validated → validation
      - gọi method controller (có thể qua AOP proxy)
      - HandlerMethodReturnValueHandler:
          @ResponseBody/ResponseEntity → HttpMessageConverter ghi response (content negotiation theo Accept)
          String/ModelAndView → view name
6. interceptors.postHandle() (nếu không exception)
7. processDispatchResult():
      - nếu exception → HandlerExceptionResolver (ExceptionHandlerExceptionResolver cho @ExceptionHandler/@ControllerAdvice,
        ResponseStatusExceptionResolver, DefaultHandlerExceptionResolver)
      - nếu có view → ViewResolver.resolveViewName() → View.render() (Thymeleaf...)
8. interceptors.afterCompletion()
```

### 2.2 Các thành phần quan trọng
- **`HandlerMapping`**: request → handler. `RequestMappingHandlerMapping` build sẵn registry lúc startup từ mọi `@RequestMapping` (xem `/actuator/mappings`).
- **`HandlerAdapter`**: cho phép `DispatcherServlet` gọi nhiều kiểu handler khác nhau (Adapter pattern): `@Controller` method, `HttpRequestHandler`, `HandlerFunction` (functional endpoint).
- **`HandlerMethodArgumentResolver`**: có thể tự viết (vd resolve `@CurrentTenant Tenant tenant`).
- **`HttpMessageConverter`**: chuyển body ↔ object: `MappingJackson2HttpMessageConverter` (JSON), `StringHttpMessageConverter`, `ByteArrayHttpMessageConverter`, `ResourceHttpMessageConverter`, Jackson XML nếu có trên classpath. Chọn dựa trên `Content-Type` (đọc) và `Accept` + kiểu trả về (ghi). Không khớp → `415 Unsupported Media Type` / `406 Not Acceptable`.
- **`ViewResolver`**: chỉ dùng cho server-side rendering; với `@RestController` (= `@Controller` + `@ResponseBody`) không đi qua view.

```java
// Custom argument resolver
@Target(ElementType.PARAMETER) @Retention(RetentionPolicy.RUNTIME)
@interface CurrentTenant {}

record Tenant(String id) {}

class TenantArgumentResolver implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter p) {
        return p.hasParameterAnnotation(CurrentTenant.class) && p.getParameterType() == Tenant.class;
    }
    @Override
    public Object resolveArgument(MethodParameter p, ModelAndViewContainer mav,
                                  NativeWebRequest req, WebDataBinderFactory bf) {
        String id = req.getHeader("X-Tenant-Id");
        if (id == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing X-Tenant-Id");
        return new Tenant(id);
    }
}

@Configuration
class TenantWebConfig implements WebMvcConfigurer {
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new TenantArgumentResolver());
    }
}
```

> 💡 **Góc nhìn Senior:**
> - Spring 6 / Boot 3 **bỏ trailing slash matching** mặc định: `GET /users/` không còn khớp `@GetMapping("/users")` → 404 sau khi migrate. Sửa ở client hoặc thêm redirect filter; `setUseTrailingSlashMatch(true)` đã deprecated.
> - Suffix pattern matching (`/users.json`) đã tắt từ Spring 5.3 vì lý do bảo mật (RFD attack).
> - Mặc định `PathPatternParser` (Spring 6) thay `AntPathMatcher` cho MVC — nhanh hơn, cú pháp `{*path}`.
> - `ObjectMapper` được dùng bởi converter là bean của Boot; đừng `new ObjectMapper()` trong code — sẽ lệch cấu hình (date format, module).

### 🛠 Bài tập phần 2

**Bài 2.1 — Đọc mappings (Cơ bản)**
- Đề bài: bật `/actuator/mappings`; liệt kê các handler, interceptor; đặt breakpoint trong `DispatcherServlet.doDispatch` và ghi lại tên class của handler, adapter, converter được dùng cho một request JSON.
- Tiêu chí đạt: ảnh chụp/ghi chú 5 đối tượng chính trên luồng.

**Bài 2.2 — Content negotiation (Trung bình)**
- Đề bài: endpoint `GET /api/products/{id}` trả JSON hoặc CSV tùy header `Accept` (`application/json`, `text/csv`), viết `HttpMessageConverter<ProductDto>` cho CSV.
- Tiêu chí đạt: `Accept: application/xml` → 406; test bằng `MockMvc`.

**Bài 2.3 — Argument resolver đa tenant (Nâng cao)**
- Đề bài: mở rộng `TenantArgumentResolver`: tenant lấy từ JWT claim `tenant_id` (nếu đã đăng nhập) và fallback header chỉ cho endpoint public; validate tenant tồn tại (cache Caffeine).
- Tiêu chí đạt: request có JWT tenant A nhưng header B → dùng A (không để client tự chọn tenant khi đã có token); giải thích vì sao đây là biện pháp chống BOLA (Phần 14).

<details>
<summary>Gợi ý lời giải</summary>

- Bài 2.2: extends `AbstractHttpMessageConverter<ProductDto>` với `super(new MediaType("text", "csv"))`, override `supports`, `writeInternal`; đăng ký qua `WebMvcConfigurer.extendMessageConverters` (dùng `extend...` thay vì `configure...` để **không** xóa converter mặc định).
- Bài 2.3: lấy `SecurityContextHolder.getContext().getAuthentication()`; nếu là `JwtAuthenticationToken` thì đọc `getToken().getClaimAsString("tenant_id")`.
</details>

---

<a id="p3"></a>
## 3. Controller, data binding & validation

### 3.1 Controller cơ bản
```java
@RestController
@RequestMapping("/api/v1/orders")
@Validated // bật method validation cho @PathVariable/@RequestParam (Boot < 3.2 hoặc khi cần validation group)
class OrderController {
    private final OrderService service;
    OrderController(OrderService service) { this.service = service; }

    @GetMapping("/{id}")
    OrderDto get(@PathVariable @Positive long id) {
        return service.find(id);
    }

    @GetMapping
    Page<OrderDto> search(@RequestParam(required = false) OrderStatus status,
                          @RequestParam(defaultValue = "0") @Min(0) int page,
                          @RequestParam(defaultValue = "20") @Max(100) int size) {
        return service.search(status, PageRequest.of(page, size, Sort.by("createdAt").descending()));
    }

    @PostMapping
    ResponseEntity<OrderDto> create(@Valid @RequestBody CreateOrderRequest req, UriComponentsBuilder uri) {
        OrderDto created = service.create(req);
        URI location = uri.path("/api/v1/orders/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);       // 201 + Location
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void cancel(@PathVariable long id) { service.cancel(id); }
}

// DTO bất biến bằng record + Bean Validation (jakarta.validation trong Boot 3; javax.validation trong Boot 2)
record CreateOrderRequest(
        @NotNull Long customerId,
        @NotEmpty @Size(max = 50) List<@Valid OrderLine> lines,
        @Size(max = 500) String note) {}

record OrderLine(@NotBlank String sku, @Positive @Max(1000) int quantity) {}
```

Annotation binding:
- `@PathVariable` — một phần URI, định danh resource.
- `@RequestParam` — query string/form field (filter, paging). Kiểu không chuyển đổi được (vd `?page=abc`) → `MethodArgumentTypeMismatchException` → 400.
- `@RequestBody` — deserialize body bằng `HttpMessageConverter`. JSON sai cú pháp → `HttpMessageNotReadableException` → 400.
- `@RequestHeader`, `@CookieValue`, `@ModelAttribute` (bind form/query vào object).
- Spring 6.1 yêu cầu compile với `-parameters` nếu không ghi tên trong annotation (`@PathVariable("id")`).

### 3.2 Bean Validation
- Cần `spring-boot-starter-validation` (Hibernate Validator) — từ Boot 2.3 **không** còn nằm sẵn trong starter-web.
- `@Valid` (Jakarta) kích hoạt validation và **cascade** vào object con; `@Validated` (Spring) hỗ trợ **validation groups**.
- Lỗi `@RequestBody @Valid` → `MethodArgumentNotValidException`. Lỗi constraint trên tham số method:
  - Spring 6.1/Boot 3.2+: MVC có **built-in method validation** — khi tham số controller có constraint annotation, ném `HandlerMethodValidationException` (không cần `@Validated` trên class).
  - Trước đó: cần `@Validated` trên class → `MethodValidationPostProcessor` (AOP) ném `ConstraintViolationException` → mặc định thành **500** nếu không xử lý (bẫy phổ biến).

**Validation groups:**
```java
interface OnCreate {}
interface OnUpdate {}

record ProductRequest(
        @Null(groups = OnCreate.class) @NotNull(groups = OnUpdate.class) Long id,
        @NotBlank(groups = {OnCreate.class, OnUpdate.class}) String name,
        @PositiveOrZero(groups = {OnCreate.class, OnUpdate.class}) BigDecimal price) {}

@PostMapping  ProductDto create(@Validated(OnCreate.class) @RequestBody ProductRequest r) { ... }
@PutMapping("/{id}") ProductDto update(@Validated(OnUpdate.class) @RequestBody ProductRequest r) { ... }
```
> Groups làm DTO khó đọc khi nhiều use case; nhiều team chọn **DTO riêng cho từng use case** (`CreateProductRequest`, `UpdateProductRequest`) — đơn giản và an toàn hơn trước mass assignment.

**Custom validator (cross-field):**
```java
@Target(ElementType.TYPE) @Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DateRangeValidator.class)
@interface ValidDateRange {
    String message() default "from must be before to";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

@ValidDateRange
record ReportRequest(@NotNull LocalDate from, @NotNull LocalDate to) {}

class DateRangeValidator implements ConstraintValidator<ValidDateRange, ReportRequest> {
    @Override
    public boolean isValid(ReportRequest r, ConstraintValidatorContext ctx) {
        if (r == null || r.from() == null || r.to() == null) return true; // để @NotNull lo
        boolean ok = !r.from().isAfter(r.to()) && r.from().plusYears(1).isAfter(r.to());
        if (!ok) {
            ctx.disableDefaultConstraintViolation();
            ctx.buildConstraintViolationWithTemplate("range must be within 1 year and from <= to")
               .addPropertyNode("to").addConstraintViolation();
        }
        return ok;
    }
}
```
`ConstraintValidator` được Spring tạo qua `SpringConstraintValidatorFactory` → có thể **inject bean** (vd repository để check unique) — nhưng cẩn thận: check unique bằng validator có race condition, constraint DB vẫn là nguồn sự thật.

> 💡 **Góc nhìn Senior:** Tách 3 lớp kiểm tra: (1) **syntactic** ở DTO (Bean Validation), (2) **business rule** ở service/domain (vd tồn kho đủ, trạng thái cho phép), (3) **integrity** ở DB (unique, FK, check constraint). Đừng trả entity JPA trực tiếp qua API — lazy loading exception, lộ field nội bộ, vòng lặp JSON, mass assignment.

> ⚠️ **Lỗi thường gặp:** quên `@Valid` trên `List<OrderLine>` hoặc trên field object con → validation không cascade; dùng `@NotNull` cho String thay vì `@NotBlank`; dùng kiểu primitive `int` cho field optional → không phân biệt được "không gửi" với `0`.

### 🛠 Bài tập phần 3

**Bài 3.1 — CRUD có validation (Cơ bản)**
- Đề bài: viết `ProductController` CRUD với DTO record, validation đầy đủ (tên 1–200 ký tự, giá ≥ 0, sku theo regex `[A-Z]{3}-\d{4}`).
- Tiêu chí đạt: test `@WebMvcTest` cho mỗi trường hợp 201/200/204/400/404; POST trả `Location`.

**Bài 3.2 — Custom validator dùng bean (Trung bình)**
- Đề bài: `@AllowedCurrency` kiểm tra mã tiền tệ nằm trong danh sách lấy từ `@ConfigurationProperties`.
- Tiêu chí đạt: đổi cấu hình không cần sửa code; unit test validator riêng không cần Spring.

**Bài 3.3 — Patch an toàn (Nâng cao)**
- Đề bài: implement `PATCH /api/v1/users/{id}` theo JSON Merge Patch (RFC 7396, `Content-Type: application/merge-patch+json`), chỉ cho phép sửa `displayName`, `avatarUrl`; phân biệt "không gửi" và `null` (xóa giá trị).
- Tiêu chí đạt: gửi `{"role":"ADMIN"}` bị từ chối (400) hoặc bị bỏ qua có log cảnh báo; có test cho 3 trường hợp: không gửi, gửi giá trị, gửi null.

<details>
<summary>Gợi ý lời giải</summary>

- Bài 3.3: nhận `JsonNode` (hoặc `Map<String, Object>`), kiểm tra `fieldNames()` thuộc whitelist; `node.has("avatarUrl") && node.get("avatarUrl").isNull()` → xóa. Hoặc dùng `JsonNullable<T>` (openapi-jackson-nullable). Validate lại object sau khi merge bằng `Validator.validate(...)`.
</details>

---

<a id="p4"></a>
## 4. Global exception handling & ProblemDetail

### 4.1 Cơ chế
`HandlerExceptionResolver` theo thứ tự: `ExceptionHandlerExceptionResolver` (`@ExceptionHandler` trong controller rồi trong `@ControllerAdvice`) → `ResponseStatusExceptionResolver` (`@ResponseStatus`, `ResponseStatusException`) → `DefaultHandlerExceptionResolver` (exception chuẩn của Spring → 4xx). Không resolver nào xử lý → exception lên container → Boot `BasicErrorController` (`/error`).

### 4.2 ProblemDetail (RFC 9457, thay thế RFC 7807)
Spring 6 hỗ trợ `ProblemDetail` (`Content-Type: application/problem+json`) với các field `type`, `title`, `status`, `detail`, `instance` và extension properties. Mọi exception MVC chuẩn đều implement `ErrorResponse`. Bật cho Boot: `spring.mvc.problemdetails.enabled=true` (hoặc tự extends `ResponseEntityExceptionHandler`).

```java
@RestControllerAdvice
class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // Lỗi nghiệp vụ của bạn
    @ExceptionHandler(OrderNotFoundException.class)
    ProblemDetail handleNotFound(OrderNotFoundException ex, HttpServletRequest req) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setType(URI.create("https://api.example.com/problems/order-not-found"));
        pd.setTitle("Order not found");
        pd.setProperty("orderId", ex.orderId());
        return pd;
    }

    @ExceptionHandler(InsufficientStockException.class)
    ProblemDetail handleBusiness(InsufficientStockException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setType(URI.create("https://api.example.com/problems/insufficient-stock"));
        pd.setProperty("sku", ex.sku());
        return pd;
    }

    // Ghi đè format lỗi validation để trả danh sách field
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail pd = ex.getBody();                  // đã có status 400, title...
        pd.setType(URI.create("https://api.example.com/problems/validation"));
        pd.setProperty("errors", ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> Map.of("field", fe.getField(), "message", String.valueOf(fe.getDefaultMessage())))
                .toList());
        return ResponseEntity.badRequest().body(pd);
    }

    // Lưới an toàn: không lộ stack trace / message nội bộ
    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnknown(Exception ex) {
        String errorId = UUID.randomUUID().toString();
        LoggerFactory.getLogger(getClass()).error("Unhandled error {}", errorId, ex);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Unexpected error. Reference: " + errorId);
        pd.setProperty("errorId", errorId);
        return pd;
    }
}
```
Response mẫu:
```json
{
  "type": "https://api.example.com/problems/insufficient-stock",
  "title": "Conflict",
  "status": 409,
  "detail": "Only 2 items left for SKU ABC-0001",
  "instance": "/api/v1/orders",
  "sku": "ABC-0001"
}
```

> 💡 **Góc nhìn Senior:**
> - Thống nhất **một** format lỗi cho toàn hệ thống (nhiều service) — `ProblemDetail` là chuẩn tốt; `type` nên là URI ổn định để client switch logic (không dựa vào `detail` dạng text).
> - `@ExceptionHandler(Exception.class)` "nuốt" cả `AccessDeniedException`/`AuthenticationException` nếu chúng được ném trong controller/service (method security) → trả 500 thay vì 403. Hãy xử lý riêng hoặc rethrow để `ExceptionTranslationFilter` của Spring Security lo.
> - Lỗi xảy ra **trong filter** (vd JWT không hợp lệ) không đi qua `@ControllerAdvice` (chưa tới DispatcherServlet) — phải xử lý ở `AuthenticationEntryPoint`/`AccessDeniedHandler` hoặc filter.
> - Không bao giờ trả `ex.getMessage()` của exception hạ tầng (SQL, stack trace) ra client — lộ cấu trúc DB. `server.error.include-stacktrace=never` (mặc định).

### 🛠 Bài tập phần 4

**Bài 4.1 — ProblemDetail cơ bản (Cơ bản)**
- Đề bài: bật `spring.mvc.problemdetails.enabled`, gọi endpoint với JSON lỗi cú pháp, sai kiểu tham số, method không hỗ trợ, media type không hỗ trợ; ghi lại status và body.
- Tiêu chí đạt: bảng 4 dòng: exception Spring — status — body.

**Bài 4.2 — Error catalog (Trung bình)**
- Đề bài: thiết kế `ErrorCode` enum (code, status, type URI, title) và một base exception `BusinessException(ErrorCode, args)`; một handler duy nhất chuyển thành `ProblemDetail` với property `code`; message hỗ trợ i18n theo `Accept-Language` (`MessageSource`).
- Tiêu chí đạt: thêm lỗi mới chỉ cần thêm 1 dòng enum + 1 dòng `messages.properties`; test với `vi` và `en`.

**Bài 4.3 — Lỗi trong filter và security (Nâng cao)**
- Đề bài: đảm bảo **mọi** lỗi — từ filter rate-limit (429), JWT sai (401), thiếu quyền (403), lỗi controller, lỗi 404 không có handler — đều trả `application/problem+json` cùng format.
- Tiêu chí đạt: test `MockMvc` cho 5 trường hợp; giải thích từng lỗi được xử lý ở tầng nào.

<details>
<summary>Gợi ý lời giải</summary>

- Bài 4.3: `AuthenticationEntryPoint` và `AccessDeniedHandler` tự ghi `ProblemDetail` bằng `ObjectMapper`; filter rate-limit tự ghi response; 404: Spring 6.1+ ném `NoResourceFoundException`/`NoHandlerFoundException` → `ResponseEntityExceptionHandler` xử lý được. Hoặc dùng `HandlerExceptionResolver` bean inject vào filter để "chuyển" exception của filter sang `@ControllerAdvice`: `resolver.resolveException(req, res, null, ex)`.
</details>

---

<a id="p5"></a>
## 5. Thiết kế REST API (resource, method, status code, pagination)

### 5.1 Resource & URI
- URI định danh **resource** (danh từ, số nhiều): `/orders`, `/orders/42`, `/orders/42/items`. Hành động thể hiện bằng **HTTP method**, không bằng động từ trong URI (`/getOrder`, `/createOrder` là anti-pattern).
- Hành động không thuần CRUD: mô hình hóa thành sub-resource hoặc resource "lệnh": `POST /orders/42/cancellation`, `POST /payments/{id}/refunds`. Chấp nhận được `POST /orders/42:cancel` (kiểu Google AIP) nếu team thống nhất.
- Lồng tối đa 1–2 cấp; quan hệ phức tạp dùng filter: `/items?orderId=42`.
- Nhất quán: kebab-case cho path, camelCase (hoặc snake_case) cho JSON — chọn một và áp dụng toàn hệ thống.

### 5.2 HTTP methods — safe & idempotent (RFC 9110)
| Method | Ngữ nghĩa | Safe | Idempotent | Body request |
|---|---|---|---|---|
| GET | đọc | ✅ | ✅ | không nên |
| HEAD | như GET không body | ✅ | ✅ | — |
| OPTIONS | khả năng/CORS preflight | ✅ | ✅ | — |
| POST | tạo/xử lý (server quyết định URI) | ❌ | ❌ | có |
| PUT | thay thế **toàn bộ** resource tại URI (client biết URI) | ❌ | ✅ | có |
| PATCH | sửa một phần | ❌ | ❌ (có thể thiết kế idempotent) | có |
| DELETE | xóa | ❌ | ✅ | không nên |

- **Idempotent** = gọi N lần có **hiệu ứng phía server** giống gọi 1 lần (response có thể khác: DELETE lần 2 có thể trả 404). Quan trọng vì client/proxy/gateway **retry** khi timeout — retry POST có thể tạo đơn hàng trùng → cần idempotency key (Phần 6).
- `PATCH` với `{"op":"increment"}` không idempotent; JSON Merge Patch đặt giá trị tuyệt đối thì idempotent.

### 5.3 Status code nên dùng
| Code | Khi nào |
|---|---|
| 200 OK | GET/PUT/PATCH thành công có body |
| 201 Created | POST tạo mới — kèm `Location` |
| 202 Accepted | xử lý bất đồng bộ — trả link tới resource trạng thái (`/jobs/123`) |
| 204 No Content | DELETE/PUT thành công không body |
| 304 Not Modified | conditional GET, ETag khớp |
| 400 Bad Request | input sai cú pháp/validation |
| 401 Unauthorized | **chưa xác thực** / token không hợp lệ (kèm `WWW-Authenticate`) |
| 403 Forbidden | đã xác thực nhưng **không có quyền** |
| 404 Not Found | không tồn tại — hoặc tồn tại nhưng không được phép biết (che giấu, chống enumeration) |
| 405 / 406 / 415 | method/Accept/Content-Type không hỗ trợ |
| 409 Conflict | xung đột trạng thái (trùng unique, trạng thái không cho phép, version conflict) |
| 412 Precondition Failed | `If-Match` không khớp (optimistic concurrency) |
| 422 Unprocessable Content | cú pháp đúng nhưng vi phạm ngữ nghĩa (nhiều team dùng 400 cho mọi lỗi validation — thống nhất là quan trọng) |
| 428 Precondition Required | bắt buộc gửi `If-Match` |
| 429 Too Many Requests | rate limit — kèm `Retry-After` |
| 500 / 502 / 503 / 504 | lỗi server / upstream lỗi / tạm không phục vụ (kèm `Retry-After`) / upstream timeout |

### 5.4 Pagination, filter, sort
**Offset pagination** (`?page=3&size=20` hoặc `?offset=60&limit=20`):
- Dễ, nhảy trang tùy ý, có tổng số trang.
- Nhược: `OFFSET 100000` buộc DB đọc rồi bỏ 100k dòng → chậm dần; `COUNT(*)` tốn kém trên bảng lớn; dữ liệu chèn/xóa giữa các lần gọi → trùng/sót bản ghi.

**Keyset/cursor pagination** (`?limit=20&after=eyJpZCI6MTIzfQ`):
- `WHERE (created_at, id) < (:lastCreatedAt, :lastId) ORDER BY created_at DESC, id DESC LIMIT 20` — dùng index, hiệu năng ổn định, không trùng/sót.
- Nhược: không nhảy trang tùy ý, cursor phải opaque (base64 của sort key), cần sort key unique (thêm `id` làm tie-breaker).
- Spring Data 3.1+ có `ScrollPosition`/`Window<T>` hỗ trợ keyset.

```java
public record CursorPage<T>(List<T> items, String nextCursor) {}

@GetMapping("/api/v1/events")
CursorPage<EventDto> events(@RequestParam(defaultValue = "50") @Max(200) int limit,
                            @RequestParam(required = false) String after) {
    Cursor c = after == null ? Cursor.START : Cursor.decode(after);     // base64(JSON) có ký HMAC nếu cần chống sửa
    List<EventDto> rows = repo.findPage(c.createdAt(), c.id(), limit + 1); // lấy dư 1 để biết còn trang sau
    String next = rows.size() > limit ? Cursor.of(rows.get(limit - 1)).encode() : null;
    return new CursorPage<>(rows.subList(0, Math.min(limit, rows.size())), next);
}
```

**Filter & sort:** `GET /orders?status=PAID&createdFrom=2026-01-01&sort=createdAt,desc`. **Whitelist** các field được sort/filter (sort theo field tùy ý → lộ field ẩn, hoặc sort trên cột không có index → full scan → DoS). Giới hạn `size` tối đa.

> ⚠️ **Lỗi thường gặp:** trả trực tiếp `Page<T>` của Spring Data ra JSON — cấu trúc JSON phụ thuộc implementation (`PageImpl`), Spring Data 3.3 log cảnh báo và khuyến nghị `@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)` hoặc tự định nghĩa DTO trang. Hãy tự kiểm soát hợp đồng API.

### 🛠 Bài tập phần 5

**Bài 5.1 — Review thiết kế API (Cơ bản)**
- Đề bài: sửa danh sách endpoint sau cho đúng REST: `GET /getAllUsers`, `POST /user/delete?id=5`, `POST /updateUserEmail`, `GET /orders/create?productId=1`, `POST /users/5/orders/7/items/3/update`.
- Tiêu chí đạt: mỗi endpoint mới có method, URI, status code thành công và lỗi chính.

**Bài 5.2 — Offset vs keyset (Trung bình)**
- Đề bài: bảng `event` 5 triệu dòng (sinh dữ liệu bằng `generate_series` trên PostgreSQL). Implement 2 endpoint phân trang; đo thời gian lấy trang 1, 1.000, 100.000; chạy `EXPLAIN ANALYZE`.
- Tiêu chí đạt: bảng so sánh; giải thích vì sao keyset ổn định; index cần tạo `(created_at DESC, id DESC)`.

**Bài 5.3 — Filter động an toàn (Nâng cao)**
- Đề bài: endpoint tìm kiếm đơn hàng với filter tùy chọn (status, khoảng ngày, khoảng tiền, customerId) và sort theo whitelist, dùng JPA `Specification` hoặc Criteria; không có string concatenation SQL.
- Tiêu chí đạt: `?sort=password,asc` → 400; `size=10000` → bị giới hạn 100 hoặc 400; có test SQL injection payload trong filter.

<details>
<summary>Gợi ý lời giải</summary>

- Bài 5.1 ví dụ: `GET /users` (200), `DELETE /users/5` (204/404), `PATCH /users/{id}` với `{"email": ...}` (200/400/409), `POST /orders` (201), `PATCH /orders/7/items/3`.
- Bài 5.3: `Set<String> SORTABLE = Set.of("createdAt", "total")`; duyệt `pageable.getSort()` và reject property ngoài whitelist.
</details>

---

<a id="p6"></a>
## 6. REST nâng cao: versioning, idempotency key, ETag, HATEOAS, OpenAPI

### 6.1 Versioning
| Chiến lược | Ví dụ | Ưu | Nhược |
|---|---|---|---|
| URI path | `/api/v1/orders` | rõ ràng, dễ route ở gateway, dễ cache, phổ biến nhất | "không thuần REST" (resource đổi URI) |
| Query param | `/orders?version=1` | đơn giản | dễ bị quên, cache key phức tạp |
| Header tùy chỉnh | `X-API-Version: 2` | URI sạch | khó test bằng trình duyệt, phải `Vary` |
| Media type | `Accept: application/vnd.acme.order.v2+json` | chuẩn HTTP content negotiation | phức tạp nhất cho client |

Nguyên tắc quan trọng hơn chọn chiến lược (Newman, *Building Microservices*): **tránh breaking change** — thêm field mới (optional) thay vì đổi/xóa; client là *tolerant reader* (bỏ qua field lạ); khi buộc phải breaking, chạy song song v1 & v2, có kế hoạch deprecation (header `Deprecation`, `Sunset`), theo dõi ai còn gọi v1. Spring Framework 7 (Boot 4) bổ sung hỗ trợ API versioning tích hợp; với Boot 3 dùng URI path hoặc custom `RequestCondition`.

### 6.2 Idempotency key cho POST
Client sinh `Idempotency-Key: <uuid>` cho mỗi *thao tác nghiệp vụ*, retry thì gửi **cùng** key.

```java
@PostMapping("/api/v1/payments")
ResponseEntity<PaymentDto> pay(@RequestHeader("Idempotency-Key") @Size(min = 16, max = 64) String key,
                               @Valid @RequestBody PaymentRequest req,
                               @AuthenticationPrincipal Jwt jwt) {
    String scopedKey = jwt.getSubject() + ":" + key;   // key gắn với user, tránh va chạm/lạm dụng
    return idempotencyService.execute(scopedKey, req.fingerprint(), () -> paymentService.pay(req));
}
```
Thiết kế `IdempotencyService`:
1. `INSERT INTO idempotency(key, request_hash, status='IN_PROGRESS', created_at)` — unique constraint trên `key` làm khóa (hoặc Redis `SET key NX EX 86400`).
2. Insert thành công → thực thi nghiệp vụ → lưu `response_status`, `response_body`, `status='COMPLETED'` (lý tưởng trong cùng transaction với nghiệp vụ).
3. Insert thất bại (key đã có): nếu `COMPLETED` và `request_hash` khớp → trả lại response đã lưu; hash khác → `422`/`409` (cùng key, khác payload); đang `IN_PROGRESS` → `409` + `Retry-After`.
4. Hết hạn sau 24h–7 ngày (TTL).

> 💡 **Góc nhìn Senior:** idempotency key chỉ bảo vệ khỏi retry của *cùng client*. Ở tầng messaging (Kafka at-least-once) cần consumer idempotent (bảng processed_message). Với thanh toán, nên kết hợp unique constraint nghiệp vụ (vd `order_id` trong bảng payment) làm lớp bảo vệ cuối.

### 6.3 ETag & conditional requests
- **ETag** = "phiên bản" của representation. Server trả `ETag: "v7"`.
- **Caching:** client gửi `If-None-Match: "v7"` → nếu không đổi trả `304 Not Modified` (không body) → tiết kiệm băng thông.
- **Optimistic concurrency (lost update):** client gửi `PUT` kèm `If-Match: "v7"` → nếu resource đã thành v8 → `412 Precondition Failed`.
- `ShallowEtagHeaderFilter` của Spring: tính MD5 trên body response → tiết kiệm băng thông nhưng **không** tiết kiệm CPU/DB (vẫn chạy controller) và không hỗ trợ `If-Match` cho cập nhật. ETag "deep" dựa trên `@Version` của JPA tốt hơn.

```java
@GetMapping("/api/v1/products/{id}")
ResponseEntity<ProductDto> get(@PathVariable long id, WebRequest request) {
    Product p = repo.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    String etag = "\"" + p.getVersion() + "\"";
    if (request.checkNotModified(etag)) {
        return null;                                  // Spring tự trả 304
    }
    return ResponseEntity.ok().eTag(etag).cacheControl(CacheControl.maxAge(Duration.ofSeconds(60)).cachePrivate())
            .body(ProductDto.from(p));
}

@PutMapping("/api/v1/products/{id}")
ResponseEntity<ProductDto> update(@PathVariable long id,
                                  @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
                                  @Valid @RequestBody UpdateProductRequest req) {
    if (ifMatch == null) return ResponseEntity.status(HttpStatus.PRECONDITION_REQUIRED).build(); // 428
    long expectedVersion = Long.parseLong(ifMatch.replace("\"", "").replace("W/", ""));
    Product updated = service.update(id, expectedVersion, req);   // ném 412 nếu version lệch
    return ResponseEntity.ok().eTag("\"" + updated.getVersion() + "\"").body(ProductDto.from(updated));
}
```

### 6.4 HATEOAS (ngắn gọn)
Mức cao nhất của Richardson Maturity Model: response chứa **link** tới các hành động hợp lệ tiếp theo (`_links.cancel.href` chỉ có khi đơn hàng còn hủy được). Spring HATEOAS cung cấp `EntityModel`, `WebMvcLinkBuilder`, format HAL. Thực tế ít API public dùng triệt để; hiểu khái niệm và trade-off (client có thể "khám phá" API, nhưng tăng kích thước payload và độ phức tạp) là đủ cho phỏng vấn.

### 6.5 OpenAPI với springdoc
- `springdoc-openapi-starter-webmvc-ui` (bản 2.x cho Boot 3; 1.x cho Boot 2 — không lẫn lộn) sinh `/v3/api-docs` và Swagger UI `/swagger-ui.html` từ code.
- Annotation: `@Operation`, `@ApiResponse`, `@Schema`, `@Parameter`, `@Tag`; cấu hình security scheme cho JWT bằng `@SecurityScheme`.
- **Code-first** (sinh spec từ code) vs **API-first/contract-first** (viết `openapi.yaml` trước, sinh interface bằng `openapi-generator`): API-first tốt cho nhiều team/consumer, review hợp đồng trước khi code; phát hiện breaking change bằng công cụ diff (openapi-diff, oasdiff) trong CI.
- Production: tắt Swagger UI hoặc bảo vệ nó (`springdoc.swagger-ui.enabled=false`) — đây chính là "Improper Inventory Management" nếu để lộ API nội bộ.

### 🛠 Bài tập phần 6

**Bài 6.1 — ETag (Cơ bản)**
- Đề bài: thêm ETag dựa trên `@Version` cho `GET /products/{id}`; test bằng `curl -i` với `If-None-Match`.
- Tiêu chí đạt: lần 2 trả 304 không body; sau khi update, trả 200 với ETag mới.

**Bài 6.2 — Lost update (Trung bình)**
- Đề bài: tái hiện lost update: 2 client cùng GET product, cùng PUT với giá khác nhau. Sau đó yêu cầu `If-Match`.
- Tiêu chí đạt: không có `If-Match` → 428; ETag cũ → 412; test đồng thời bằng 2 thread chỉ 1 thành công.

**Bài 6.3 — Idempotency key production-grade (Nâng cao)**
- Đề bài: implement `IdempotencyService` theo thiết kế 6.2 với PostgreSQL; xử lý: request đồng thời cùng key, cùng key khác payload, app crash khi đang `IN_PROGRESS` (record kẹt), TTL dọn dẹp.
- Tiêu chí đạt: test 20 thread gửi cùng key → nghiệp vụ chạy đúng 1 lần, 19 request còn lại nhận 409 hoặc response đã lưu; record `IN_PROGRESS` quá 5 phút được coi là hết hạn và cho phép thực thi lại (giải thích rủi ro).

<details>
<summary>Gợi ý lời giải</summary>

- Bài 6.2: service so sánh `expectedVersion` với `entity.getVersion()` → ném `PreconditionFailedException` (map 412); thêm `@Version` để JPA bắt race giữa lúc đọc và ghi (`ObjectOptimisticLockingFailureException` → cũng map 412).
- Bài 6.3: dùng `INSERT ... ON CONFLICT (key) DO NOTHING RETURNING key` để biết ai thắng; `request_hash = SHA-256(canonical JSON)`. Rủi ro khi "giải phóng" record kẹt: nếu request gốc thật ra vẫn đang chạy (GC pause dài) → thực thi 2 lần; giảm thiểu bằng unique constraint nghiệp vụ.
</details>

---

<a id="p7"></a>
## 7. HTTP clients: RestTemplate, WebClient, RestClient, HTTP interface

### 7.1 Toàn cảnh
| Client | Kiểu | Trạng thái | Khi dùng |
|---|---|---|---|
| `RestTemplate` | đồng bộ, template method | **maintenance mode** từ Spring 5 (chỉ sửa lỗi nhỏ); Spring team đã công bố lộ trình deprecate trong thế hệ Spring 7.x | code cũ |
| `WebClient` | non-blocking, reactive (Reactor), fluent | đầy đủ | WebFlux; gọi song song/streaming; trong MVC cũng dùng được (`.block()`) nhưng kéo theo WebFlux + Reactor |
| `RestClient` | **đồng bộ, fluent API** giống WebClient | Spring 6.1 / Boot 3.2+ | lựa chọn mặc định cho app MVC (đặc biệt với virtual threads) |
| HTTP interface (`@HttpExchange`) | khai báo interface, proxy sinh code (giống Feign) | Spring 6.0+, hỗ trợ RestClient từ 6.1 | client gọn, dễ mock |

```java
// RestClient — tạo từ RestClient.Builder do Boot cấu hình sẵn (có Jackson, observation/metrics, tracing)
@Configuration
class ClientConfig {
    @Bean
    RestClient inventoryRestClient(RestClient.Builder builder, InventoryProperties props) {
        var settings = ClientHttpRequestFactorySettings.defaults()   // Boot 3.4+ API; bản cũ hơn dùng ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofSeconds(2))
                .withReadTimeout(Duration.ofSeconds(3));
        return builder
                .baseUrl(props.baseUrl())
                .requestFactory(ClientHttpRequestFactoryBuilder.detect().build(settings))
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultStatusHandler(HttpStatusCode::is5xxServerError, (req, res) -> {
                    throw new InventoryUnavailableException(res.getStatusCode());
                })
                .build();
    }

    // Declarative HTTP interface dựa trên RestClient
    @Bean
    InventoryClient inventoryClient(RestClient inventoryRestClient) {
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(inventoryRestClient))
                .build().createClient(InventoryClient.class);
    }
}

@HttpExchange("/api/v1/stock")
interface InventoryClient {
    @GetExchange("/{sku}")
    StockDto getStock(@PathVariable String sku);

    @PostExchange("/reservations")
    ReservationDto reserve(@RequestBody ReserveRequest req);
}

// Dùng RestClient trực tiếp
record StockDto(String sku, int available) {}

@Service
class InventoryGateway {
    private final RestClient client;
    InventoryGateway(RestClient inventoryRestClient) { this.client = inventoryRestClient; }

    StockDto stock(String sku) {
        return client.get().uri("/api/v1/stock/{sku}", sku)
                .retrieve()
                .onStatus(s -> s.value() == 404, (req, res) -> { throw new SkuNotFoundException(sku); })
                .body(StockDto.class);
    }
}
```
(Lưu ý: `ClientHttpRequestFactorySettings`/`ClientHttpRequestFactoryBuilder` thuộc Spring Boot và API đã thay đổi giữa 3.2 → 3.4; Boot 3.4+ còn hỗ trợ property `spring.http.client.*`. Luôn đối chiếu reference của đúng phiên bản.)

### 7.2 Timeout & connection pool — phần Senior thực sự bị hỏi
Ba loại timeout khác nhau:
1. **Connect timeout** — thời gian thiết lập TCP (+TLS). Nên ngắn: 1–3s.
2. **Read/response timeout (socket timeout)** — thời gian chờ giữa các gói dữ liệu / chờ response. Theo SLA của upstream (p99 + biên).
3. **Connection request timeout (pool acquire)** — thời gian chờ mượn connection từ pool khi pool cạn. Thiếu cái này → thread treo vô hạn khi pool đầy.

Ngoài ra: **tổng thời gian (deadline)** của cả lời gọi kể cả retry, nên được cấu hình ở tầng resilience (Resilience4j `TimeLimiter`) hoặc propagate deadline từ request gốc.

Mặc định nguy hiểm:
- `RestTemplate()`/`SimpleClientHttpRequestFactory` (JDK `HttpURLConnection`): timeout mặc định **vô hạn** (0) → một upstream treo có thể làm cạn toàn bộ thread pool của Tomcat → **cascading failure**.
- Apache HttpClient 5 `PoolingHttpClientConnectionManager` mặc định `maxTotal=25`, `defaultMaxPerRoute=5` → ở tải cao, request xếp hàng chờ connection, latency tăng mà CPU thấp — dấu hiệu kinh điển. Tăng theo nhu cầu đồng thời thực tế.
- Reactor Netty (WebClient): pool mặc định max connections = `max(số CPU × 2, 16)`, hàng đợi pending acquire có giới hạn và timeout mặc định 45s; không có response timeout mặc định — đặt `responseTimeout`.

```java
// Apache HttpClient 5 với pool & timeout đầy đủ
@Bean
RestClient paymentRestClient(RestClient.Builder builder) {
    var cm = PoolingHttpClientConnectionManagerBuilder.create()
            .setMaxConnTotal(200)
            .setMaxConnPerRoute(50)
            .setDefaultConnectionConfig(ConnectionConfig.custom()
                    .setConnectTimeout(Timeout.ofSeconds(2))
                    .setSocketTimeout(Timeout.ofSeconds(5))
                    .setTimeToLive(TimeValue.ofMinutes(5))        // tránh giữ connection quá lâu (DNS đổi, LB)
                    .build())
            .build();
    CloseableHttpClient http = HttpClients.custom()
            .setConnectionManager(cm)
            .setDefaultRequestConfig(RequestConfig.custom()
                    .setConnectionRequestTimeout(Timeout.ofMillis(500)) // chờ mượn connection
                    .setResponseTimeout(Timeout.ofSeconds(5))
                    .build())
            .evictIdleConnections(TimeValue.ofSeconds(30))
            .build();
    return builder.requestFactory(new HttpComponentsClientHttpRequestFactory(http))
                  .baseUrl("https://payment.internal").build();
}
```

> 💡 **Góc nhìn Senior:**
> - Luôn tạo client từ `RestClient.Builder`/`WebClient.Builder` **do Boot inject** để có metrics `http.client.requests` và trace propagation; `RestClient.create()` tự tạo sẽ mất observability.
> - Retry chỉ cho thao tác idempotent (hoặc có idempotency key), dùng exponential backoff + jitter, và **retry budget** — retry ở nhiều tầng (client, gateway, service mesh) nhân bản tải lên upstream đang yếu (retry storm). Kết hợp circuit breaker (Resilience4j).
> - Connection idle bị LB/NAT cắt ngầm (vd AWS NLB idle timeout 350s) → lỗi `Connection reset`/`NoHttpResponseException` lẻ tẻ. Cấu hình evict idle/TTL nhỏ hơn idle timeout của hạ tầng.
> - Đừng log toàn bộ request/response chứa token/PII từ client interceptor.

### 🛠 Bài tập phần 7

**Bài 7.1 — Migrate RestTemplate → RestClient (Cơ bản)**
- Đề bài: chuyển một service dùng `RestTemplate` (GET, POST, xử lý 404) sang `RestClient` và sang HTTP interface.
- Tiêu chí đạt: test bằng `MockRestServiceServer` (`MockRestServiceServer.bindTo(builder)`) hoặc WireMock; không thay đổi hành vi.

**Bài 7.2 — Chứng minh timeout vô hạn nguy hiểm (Trung bình)**
- Đề bài: dựng upstream giả (WireMock `withFixedDelay(60000)`); service A gọi upstream bằng `RestTemplate` mặc định trong một endpoint; load test 300 concurrent request vào A, đồng thời gọi `/actuator/health` của A.
- Tiêu chí đạt: quan sát health cũng bị treo (Tomcat cạn thread); sửa bằng timeout + circuit breaker; đo lại.

**Bài 7.3 — Pool sizing (Nâng cao)**
- Đề bài: với Apache HttpClient mặc định (maxPerRoute=5), upstream latency 100ms, chạy 100 concurrent request; quan sát p99 và metric thời gian chờ mượn connection. Tính toán theo Little's Law và cấu hình lại pool cho mục tiêu 500 req/s.
- Tiêu chí đạt: báo cáo trước/sau; công thức: cần ≥ 500 × 0.1 = 50 connection tới route đó, cộng biên.

<details>
<summary>Gợi ý lời giải</summary>

- Bài 7.1: `RestClient.create(restTemplate)` cho phép tái sử dụng cấu hình của `RestTemplate` cũ khi migrate từ từ.
- Bài 7.2: health endpoint chạy trên cùng Tomcat thread pool → minh chứng vì sao nên tách `management.server.port` và đặt timeout ở mọi lời gọi ra ngoài (bulkhead).
</details>

---

<a id="p8"></a>
## 8. WebFlux & reactive vs MVC + virtual threads; CORS

### 8.1 Reactive cơ bản
- **Spring WebFlux** chạy trên Reactor Netty (mặc định) hoặc Servlet 3.1+ non-blocking; mô hình **event loop**: một số ít thread (≈ số core) xử lý hàng nghìn connection, I/O không chặn.
- Kiểu dữ liệu Project Reactor: `Mono<T>` (0..1 phần tử), `Flux<T>` (0..N). Lazy — **không có gì xảy ra cho tới khi subscribe**.
- **Backpressure**: subscriber báo upstream mình xử lý được bao nhiêu (`request(n)`) — Reactive Streams spec.
- Toàn bộ chuỗi phải non-blocking: R2DBC thay JDBC, WebClient thay RestTemplate, reactive driver cho Mongo/Redis/Kafka.

```java
@RestController
class PriceController {
    private final WebClient client;
    PriceController(WebClient.Builder builder) { this.client = builder.baseUrl("http://pricing").build(); }

    // Gọi 3 upstream song song, tổng hợp kết quả, timeout và fallback
    @GetMapping("/api/v1/quote/{sku}")
    Mono<Quote> quote(@PathVariable String sku) {
        Mono<Price> price = client.get().uri("/price/{sku}", sku).retrieve().bodyToMono(Price.class)
                .timeout(Duration.ofMillis(800));
        Mono<Stock> stock = client.get().uri("/stock/{sku}", sku).retrieve().bodyToMono(Stock.class)
                .timeout(Duration.ofMillis(800))
                .onErrorReturn(Stock.unknown(sku));                       // degrade
        Mono<Promo> promo = client.get().uri("/promo/{sku}", sku).retrieve().bodyToMono(Promo.class)
                .timeout(Duration.ofMillis(500))
                .onErrorResume(e -> Mono.just(Promo.none()));
        return Mono.zip(price, stock, promo).map(t -> new Quote(t.getT1(), t.getT2(), t.getT3()));
    }
}
```

> ⚠️ **Lỗi chết người:** gọi code blocking (JDBC, `Thread.sleep`, `RestTemplate`, `.block()`) trên event loop thread → chặn hàng trăm request khác. Nếu buộc phải gọi blocking: `Mono.fromCallable(...).subscribeOn(Schedulers.boundedElastic())`. Dùng **BlockHound** trong test để phát hiện. Debug khó vì stack trace không phản ánh luồng logic (dùng `checkpoint()`, `Hooks.onOperatorDebug()` chỉ ở dev). `ThreadLocal` (MDC, `SecurityContextHolder`) không dùng được tự nhiên — phải dùng Reactor `Context` (Spring Security reactive dùng `ReactiveSecurityContextHolder`).

### 8.2 Khi nào WebFlux, khi nào MVC (+ virtual threads)?
| Tiêu chí | Spring MVC (+ virtual threads, Boot 3.2+/Java 21) | WebFlux |
|---|---|---|
| Mô hình lập trình | imperative, dễ đọc, dễ debug | reactive, learning curve cao |
| Scale I/O đồng thời | virtual threads giải quyết phần lớn bài toán "nhiều request chờ I/O" | tốt |
| Streaming, backpressure, SSE/WebSocket lượng lớn | hạn chế | **mạnh** |
| Hệ sinh thái | JDBC/JPA, mọi thư viện blocking | cần driver reactive (R2DBC chưa có tính năng như JPA) |
| Gateway / proxy hiệu năng cao | được | Spring Cloud Gateway (bản gốc) xây trên WebFlux |

> 💡 **Góc nhìn Senior:** Từ khi có virtual threads, lý do "dùng WebFlux để scale" yếu đi nhiều với app CRUD/tích hợp thông thường. WebFlux vẫn đáng dùng khi: cần streaming/backpressure thực sự, xử lý hàng chục nghìn kết nối dài (SSE/WebSocket), hoặc cả stack đã reactive. Không trộn lẫn tùy tiện: app MVC dùng `WebClient` được, nhưng đưa `Mono` vào khắp service layer của app MVC chỉ thêm phức tạp. Cả hai đều không thể vượt giới hạn của tài nguyên hạ nguồn (DB pool).

### 8.3 CORS
- Trình duyệt áp dụng **Same-Origin Policy**: JS ở `https://app.example.com` gọi `https://api.example.com` là cross-origin. CORS là cơ chế server **nới lỏng** bằng header `Access-Control-Allow-*`. CORS **không** phải biện pháp bảo mật cho server (curl/Postman không quan tâm CORS) — nó bảo vệ người dùng trình duyệt.
- Request "không đơn giản" (method khác GET/HEAD/POST, header tùy chỉnh như `Authorization`, `Content-Type: application/json`) → trình duyệt gửi **preflight** `OPTIONS` trước.
- Với Spring Security: phải bật `http.cors(...)` để `CorsFilter` xử lý preflight **trước** khi security chặn (preflight không có token → bị 401 nếu không cấu hình).

```java
@Bean
CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration cfg = new CorsConfiguration();
    cfg.setAllowedOrigins(List.of("https://app.example.com", "https://admin.example.com"));
    cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE"));
    cfg.setAllowedHeaders(List.of("Authorization", "Content-Type", "Idempotency-Key", "If-Match"));
    cfg.setExposedHeaders(List.of("Location", "ETag", "X-Correlation-Id"));
    cfg.setAllowCredentials(true);     // cho cookie; KHÔNG được dùng cùng allowedOrigins("*")
    cfg.setMaxAge(Duration.ofHours(1)); // cache preflight
    var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", cfg);
    return source;
}
// trong SecurityFilterChain: http.cors(Customizer.withDefaults());
```

> ⚠️ **Lỗi thường gặp:** "phản chiếu" (reflect) header `Origin` bất kỳ kèm `Allow-Credentials: true` → mọi website độc hại đều đọc được dữ liệu của user đã đăng nhập. `allowedOriginPatterns("*")` + credentials cũng tương đương — chỉ dùng whitelist cụ thể.

### 🛠 Bài tập phần 8

**Bài 8.1 — Mono/Flux cơ bản (Cơ bản)**
- Đề bài: viết test với `StepVerifier` cho: `Flux.range(1,10)` lọc số chẵn, map bình phương; một `Mono` lỗi được `onErrorResume`; chứng minh "không subscribe thì không chạy".
- Tiêu chí đạt: 3 test pass.

**Bài 8.2 — Aggregation song song (Trung bình)**
- Đề bài: endpoint tổng hợp 3 upstream như ví dụ 8.1, implement 2 phiên bản: WebFlux và MVC + virtual threads (`StructuredTaskScope` nếu dùng preview, hoặc `CompletableFuture` với executor virtual thread). So sánh độ dài code, latency.
- Tiêu chí đạt: cả hai có timeout & fallback; latency tổng ≈ max (không phải sum) của 3 upstream.

**Bài 8.3 — CORS + Security (Nâng cao)**
- Đề bài: SPA ở `http://localhost:5173` gọi API dùng JWT trong header `Authorization`. Tái hiện lỗi preflight bị 401, sửa đúng cách; viết test `MockMvc` gửi `OPTIONS` với `Origin` và `Access-Control-Request-Method`.
- Tiêu chí đạt: origin lạ không nhận header `Access-Control-Allow-Origin`; giải thích vì sao lỗi CORS không chặn được request "simple" POST form từ site độc hại (→ cần CSRF protection nếu dùng cookie).

<details>
<summary>Gợi ý lời giải</summary>

- Bài 8.2 (MVC): `try (var ex = Executors.newVirtualThreadPerTaskExecutor()) { var p = CompletableFuture.supplyAsync(() -> pricing.get(sku), ex).orTimeout(800, MILLISECONDS); ... }`.
- Bài 8.3: `mockMvc.perform(options("/api/orders").header("Origin","http://localhost:5173").header("Access-Control-Request-Method","POST"))` → kỳ vọng `200` và header allow-origin.
</details>

---

<a id="p9"></a>
## 9. Kiến trúc Spring Security

### 9.1 Từ Servlet filter tới SecurityFilterChain
```
Tomcat filter chain
  └─ DelegatingFilterProxy (Servlet Filter, tên "springSecurityFilterChain")
        └─ (lazy lookup bean) FilterChainProxy
              ├─ chọn SecurityFilterChain ĐẦU TIÊN có securityMatcher khớp request
              │     chain #1 /actuator/**   : [... filters ...]
              │     chain #2 /api/**        : [... filters ...]
              │     chain #3 any request    : [... filters ...]
              └─ chạy các security filter của chain đó theo thứ tự
```
- **`DelegatingFilterProxy`**: cầu nối giữa vòng đời Servlet container và Spring `ApplicationContext` — container đăng ký filter trước khi bean Spring sẵn sàng, proxy này trì hoãn việc lấy bean.
- **`FilterChainProxy`**: điểm vào duy nhất của Spring Security; còn áp dụng `HttpFirewall` (`StrictHttpFirewall` chặn URL độc hại như `..`, `;`, `%2F`), dọn `SecurityContextHolder` sau request (tránh rò ThreadLocal).
- **`SecurityFilterChain`**: có thể có nhiều chain với `securityMatcher` khác nhau; sắp bằng `@Order`.

Thứ tự filter tiêu biểu (rút gọn): `DisableEncodeUrlFilter` → `SecurityContextHolderFilter` → `HeaderWriterFilter` → `CorsFilter` → `CsrfFilter` → `LogoutFilter` → (`OAuth2AuthorizationRequestRedirectFilter`, `OAuth2LoginAuthenticationFilter`) → `UsernamePasswordAuthenticationFilter` → `BearerTokenAuthenticationFilter` → `BasicAuthenticationFilter` → `RequestCacheAwareFilter` → `SecurityContextHolderAwareRequestFilter` → `AnonymousAuthenticationFilter` → `SessionManagementFilter` → **`ExceptionTranslationFilter`** → **`AuthorizationFilter`**. Bật log `logging.level.org.springframework.security=TRACE` để xem chain thực tế.

### 9.2 Cấu hình Spring Security 6 (Boot 3)
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity      // bật @PreAuthorize/@PostAuthorize (thay @EnableGlobalMethodSecurity của 5.x)
class SecurityConfig {

    @Bean
    @Order(1)
    SecurityFilterChain apiChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/api/**")
            .csrf(csrf -> csrf.disable())                                  // stateless + bearer token (xem Phần 13)
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/orders").hasAuthority("SCOPE_orders:write")
                .anyRequest().authenticated())                             // deny-by-default
            .oauth2ResourceServer(o -> o.jwt(Customizer.withDefaults()))
            .exceptionHandling(e -> e
                .authenticationEntryPoint(new ProblemDetailAuthEntryPoint())   // 401
                .accessDeniedHandler(new ProblemDetailAccessDeniedHandler())); // 403
        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain webChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/css/**").permitAll()
                .anyRequest().authenticated())
            .formLogin(Customizer.withDefaults());                          // session + CSRF bật mặc định
        return http.build();
    }
}
```
Khác biệt với Spring Security 5.x (thường bị hỏi): `WebSecurityConfigurerAdapter` deprecated ở 5.7 và **bị xóa** ở 6.0; `authorizeRequests` → `authorizeHttpRequests` (dùng `AuthorizationManager`); `antMatchers/mvcMatchers` → `requestMatchers`; lambda DSL là chuẩn (Security 7 bỏ kiểu chaining `.and()`); `SecurityContextHolderFilter` yêu cầu **lưu context tường minh** (không còn tự động lưu vào session mỗi khi thay đổi); authorization được áp dụng cho **mọi dispatcher type** (kể cả `ERROR`, `FORWARD`) mặc định.

### 9.3 Authentication flow
```
Filter (vd UsernamePasswordAuthenticationFilter / BearerTokenAuthenticationFilter)
  → tạo Authentication chưa xác thực (UsernamePasswordAuthenticationToken / BearerTokenAuthenticationToken)
  → AuthenticationManager.authenticate()   (implementation: ProviderManager)
       → duyệt các AuthenticationProvider: supports(type)?
            DaoAuthenticationProvider → UserDetailsService.loadUserByUsername + PasswordEncoder.matches
            JwtAuthenticationProvider → JwtDecoder.decode + JwtAuthenticationConverter
       → không provider nào được → ProviderNotFoundException; sai → BadCredentialsException...
       → (có thể có parent AuthenticationManager)
  → thành công: Authentication đã xác thực (principal, authorities, credentials bị xóa)
  → SecurityContextHolder.getContext().setAuthentication(auth)  (+ lưu SecurityContextRepository nếu stateful)
  → thất bại: AuthenticationEntryPoint / AuthenticationFailureHandler
```
`ExceptionTranslationFilter` bắt `AuthenticationException` → `AuthenticationEntryPoint` (401 / redirect login) và `AccessDeniedException` → nếu user anonymous thì coi như chưa đăng nhập (401), ngược lại `AccessDeniedHandler` (403).

### 9.4 SecurityContextHolder & thread-local
- `SecurityContextHolder` lưu `SecurityContext` (chứa `Authentication`) theo **strategy**:
  - `MODE_THREADLOCAL` (mặc định) — mỗi thread một context.
  - `MODE_INHERITABLETHREADLOCAL` — thread con tạo mới **kế thừa**; nguy hiểm với thread pool (thread được tạo một lần, sau đó tái sử dụng với context cũ của người khác!).
  - `MODE_GLOBAL` — dùng chung toàn JVM (chỉ hợp app desktop).
- Đổi bằng `spring.security.strategy` system property hoặc `SecurityContextHolder.setStrategyName(...)`.
- Với `@Async`/executor: dùng `DelegatingSecurityContextAsyncTaskExecutor`, `DelegatingSecurityContextRunnable`, hoặc `TaskDecorator` sao chép context. Với virtual threads: vẫn ThreadLocal per thread — phải truyền tường minh.
- Reactive: `ReactiveSecurityContextHolder` dựa trên Reactor `Context`.

```java
@GetMapping("/api/v1/me")
Map<String, Object> me(@AuthenticationPrincipal Jwt jwt, Authentication auth) {
    // cũng có thể: SecurityContextHolder.getContext().getAuthentication()
    return Map.of("sub", jwt.getSubject(),
                  "authorities", auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());
}
```

> 💡 **Góc nhìn Senior:** Hiểu filter chain giúp debug 90% vấn đề security: "vì sao trả 401 dù token đúng?" (chain nào khớp? filter nào chạy? log TRACE), "vì sao 403 khi POST?" (CSRF filter chặn trước khi tới authorization). Nhiều `SecurityFilterChain` cần `securityMatcher` rõ ràng; chain không có matcher (match mọi thứ) phải có order **cuối cùng**, nếu không các chain sau không bao giờ được dùng.

### 🛠 Bài tập phần 9

**Bài 9.1 — Quan sát filter chain (Cơ bản)**
- Đề bài: bật log TRACE của Spring Security, gọi một endpoint public, một endpoint cần login khi chưa đăng nhập, và một endpoint khi đã đăng nhập; liệt kê các filter được chạy.
- Tiêu chí đạt: chỉ ra filter nào quyết định trả 401/302 và filter nào quyết định 403.

**Bài 9.2 — Hai filter chain (Trung bình)**
- Đề bài: `/api/**` stateless với HTTP Basic (tạm thời), `/admin/**` form login có session, `/actuator/**` chain riêng chỉ cho role `OPS`.
- Tiêu chí đạt: test `@WebMvcTest`/`@SpringBootTest` + `spring-security-test` (`with(httpBasic(...))`, `@WithMockUser`) cho mỗi chain; đổi thứ tự `@Order` để thấy lỗi.

**Bài 9.3 — Custom AuthenticationProvider (Nâng cao)**
- Đề bài: xác thực service-to-service bằng header `X-API-Key`: viết `ApiKeyAuthenticationFilter` (tạo `ApiKeyAuthenticationToken` chưa xác thực), `ApiKeyAuthenticationProvider` (tra key đã hash SHA-256 trong DB, so sánh constant-time), gán authorities theo key.
- Tiêu chí đạt: key sai → 401 qua `AuthenticationEntryPoint`; không lưu key plaintext; giải thích vì sao dùng `MessageDigest.isEqual` thay vì `equals`.

<details>
<summary>Gợi ý lời giải</summary>

- Bài 9.3: filter gọi `authenticationManager.authenticate(token)`, thành công thì tạo `SecurityContext` mới (`SecurityContextHolder.createEmptyContext()`) và set; thất bại gọi `entryPoint.commence(...)`. Đăng ký `http.addFilterBefore(apiKeyFilter, BearerTokenAuthenticationFilter.class)`. `equals` dừng ở byte khác đầu tiên → timing attack.
</details>

---

<a id="p10"></a>
## 10. Authentication: UserDetailsService & password encoding

### 10.1 UserDetailsService
```java
@Service
class DbUserDetailsService implements UserDetailsService {
    private final UserRepository users;
    DbUserDetailsService(UserRepository users) { this.users = users; }

    @Override
    public UserDetails loadUserByUsername(String username) {
        AppUser u = users.findByEmailIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("not found")); // Spring trả BadCredentials chung chung
        return User.withUsername(u.getEmail())
                .password(u.getPasswordHash())          // dạng "{bcrypt}$2a$10$..."
                .authorities(u.getRoles().stream().map(r -> "ROLE_" + r.name()).toArray(String[]::new))
                .accountLocked(u.isLocked())
                .disabled(!u.isEnabled())
                .build();
    }
}
```
- `hasRole("ADMIN")` kiểm tra authority `ROLE_ADMIN` (tự thêm tiền tố); `hasAuthority("ADMIN")` kiểm tra đúng chuỗi. Nhầm lẫn này gây 403 khó hiểu.
- `DaoAuthenticationProvider` mặc định ẩn `UsernameNotFoundException` thành `BadCredentialsException` (`hideUserNotFoundExceptions=true`) và vẫn chạy `PasswordEncoder` với password giả để **thời gian phản hồi giống nhau** → chống user enumeration.

### 10.2 Password encoding
- **Không bao giờ** lưu plaintext, mã hóa hai chiều, hay hash nhanh (MD5/SHA-256 thuần) — GPU tính hàng tỷ hash/giây. Dùng hàm **chậm, có salt, có tham số chi phí**: **Argon2id** (khuyến nghị đầu của OWASP), **scrypt**, **bcrypt**, PBKDF2 (khi cần FIPS).
- **`DelegatingPasswordEncoder`** (`PasswordEncoderFactories.createDelegatingPasswordEncoder()`): lưu dạng `{id}hash` (vd `{bcrypt}$2a$10$...`, `{argon2@SpringSecurity_v5_8}...`) → cho phép **nâng cấp thuật toán dần dần**: hash cũ vẫn verify được, hash mới dùng thuật toán mặc định; kết hợp `UserDetailsPasswordService` để tự re-hash khi user đăng nhập thành công (`upgradeEncoding`).
- **BCrypt**: `BCryptPasswordEncoder` mặc định strength 10 (2^10 vòng); chỉ dùng **72 byte đầu** của password (password dài hơn bị cắt — cẩn thận với passphrase dài/UTF-8 nhiều byte; sau bản vá CVE-2025-22228, Spring Security từ chối password > 72 byte thay vì cắt im lặng). Chỉnh strength sao cho ~100ms–1s mỗi lần hash trên phần cứng production.
- **Argon2**: `Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()` — memory-hard (chống GPU/ASIC), cần thư viện BouncyCastle.

```java
@Bean
PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder(); // mặc định bcrypt, hiểu nhiều id khác
}
```

> 💡 **Góc nhìn Senior:** Hash chậm là *chủ đích* nhưng cũng là bề mặt **DoS**: endpoint login không rate limit cho phép kẻ tấn công đốt CPU server (mỗi request ~100ms CPU). Cần rate limit theo IP + theo tài khoản, lockout có thời hạn (tránh khóa vĩnh viễn → DoS tài khoản người khác), MFA, kiểm tra password đã bị lộ (HaveIBeenPwned k-anonymity; Spring Security 6.3 có `CompromisedPasswordChecker`). Không tự cài đặt thuật toán hash.

### 🛠 Bài tập phần 10

**Bài 10.1 — Đăng ký/đăng nhập (Cơ bản)**
- Đề bài: endpoint `POST /api/auth/register` lưu user với `DelegatingPasswordEncoder`; đăng nhập bằng HTTP Basic để gọi `/api/me`.
- Tiêu chí đạt: DB lưu dạng `{bcrypt}...`; sai password và sai username trả cùng response, thời gian tương tự.

**Bài 10.2 — Nâng cấp thuật toán (Trung bình)**
- Đề bài: giả sử DB cũ có hash dạng `{MD5}` (hoặc `{sha256}`) — migrate sang Argon2 *mà không bắt user đổi password*.
- Tiêu chí đạt: implement `UserDetailsPasswordService.updatePassword`; sau lần đăng nhập thành công, hash trong DB đổi sang `{argon2...}`; giải thích vì sao không thể migrate offline toàn bộ (vì không có plaintext) và chiến lược bổ sung (hash lồng: argon2(md5(pw))).

**Bài 10.3 — Benchmark & chống brute force (Nâng cao)**
- Đề bài: JMH benchmark bcrypt strength 10/12/14 và Argon2 default trên máy bạn; chọn tham số đạt ~250ms. Thêm cơ chế khóa tạm thời sau 5 lần sai trong 15 phút (theo tài khoản) và rate limit theo IP.
- Tiêu chí đạt: bảng benchmark; test tự động cho lockout; giải thích trade-off UX/bảo mật/DoS.

<details>
<summary>Gợi ý lời giải</summary>

- Bài 10.2: tạo `DelegatingPasswordEncoder("argon2", Map.of("argon2", argon2, "bcrypt", bcrypt, "MD5", new MessageDigestPasswordEncoder("MD5")))`; `DaoAuthenticationProvider` gọi `upgradeEncoding()` → nếu true và có `UserDetailsPasswordService` thì re-encode.
- Bài 10.3: lắng nghe `AuthenticationFailureBadCredentialsEvent` / `AuthenticationSuccessEvent` để đếm số lần sai (lưu Redis có TTL).
</details>

---

<a id="p11"></a>
## 11. Session vs stateless JWT

### 11.1 Session-based (stateful)
- Đăng nhập thành công → server lưu `SecurityContext` vào `HttpSession`, trả cookie `JSESSIONID` (`HttpOnly`, `Secure`, `SameSite`).
- Scale ngang: sticky session hoặc **Spring Session** (Redis/JDBC) lưu session tập trung.
- Ưu: thu hồi tức thì (xóa session), cookie HttpOnly chống đọc từ JS, payload nhỏ. Nhược: cần storage dùng chung, CSRF protection bắt buộc (cookie tự gửi), khó cho mobile/service-to-service.
- **Session fixation**: Spring Security mặc định đổi session id khi đăng nhập (`changeSessionId`).

### 11.2 JWT (JSON Web Token — RFC 7519)
Cấu trúc: `base64url(header).base64url(payload).base64url(signature)`
```json
// header
{ "alg": "RS256", "typ": "JWT", "kid": "2026-09-key" }
// payload (claims)
{ "iss": "https://auth.example.com", "sub": "user-123", "aud": "orders-api",
  "exp": 1790000000, "iat": 1789999100, "jti": "8f2c...", "scope": "orders:read orders:write" }
```
- Payload chỉ được **encode**, không mã hóa — ai cũng đọc được → không đặt dữ liệu nhạy cảm (dùng JWE nếu cần mã hóa).
- **Ký**: `HS256` (HMAC, secret chung — mọi service verify được cũng **ký** được → chỉ hợp khi issuer = verifier) vs `RS256`/`ES256` (khóa bất đối xứng: auth server ký bằng private key, các service verify bằng public key lấy từ **JWKS** endpoint, xoay khóa bằng `kid`).
- Verify bắt buộc: chữ ký với thuật toán **cố định** phía server (chống tấn công `alg: none` và nhầm lẫn RS/HS), `exp`/`nbf` (cho phép clock skew nhỏ), `iss`, `aud` (token cho service A không dùng được cho service B).

**Access token + refresh token:**
- Access token **ngắn hạn** (5–15 phút), stateless, gửi trong `Authorization: Bearer`.
- Refresh token **dài hạn**, chỉ gửi tới auth server để lấy access token mới; lưu phía server (hoặc có `jti` lưu DB) để có thể thu hồi.
- **Refresh token rotation**: mỗi lần refresh cấp refresh token mới và vô hiệu cái cũ; nếu cái cũ bị dùng lại → dấu hiệu bị đánh cắp → thu hồi toàn bộ "family" token.

**Thu hồi (revocation) — trade-off cốt lõi:**
| Cách | Ưu | Nhược |
|---|---|---|
| TTL ngắn + chờ hết hạn | stateless hoàn toàn | cửa sổ rủi ro = TTL |
| Denylist `jti` (Redis, TTL = thời gian còn lại của token) | thu hồi tức thì | mỗi request tra Redis → mất một phần lợi ích stateless |
| `token_version` per user trong DB/cache, so với claim | thu hồi mọi token của user (đổi mật khẩu, logout all) | cần tra cứu |
| Opaque token + introspection (RFC 7662) | kiểm soát hoàn toàn | gọi auth server mỗi request (cache ngắn) |

**Lưu token ở client (SPA):** `localStorage` → bị đọc bởi XSS; cookie `HttpOnly; Secure; SameSite=Strict/Lax` → chống XSS đọc token nhưng cần CSRF protection. Xu hướng hiện nay cho SPA: **BFF (Backend-for-Frontend)** — token giữ ở server, browser chỉ có session cookie.

```java
// Tự phát hành JWT (khi chính service là auth server đơn giản) bằng Spring Security's NimbusJwtEncoder
@Service
class TokenService {
    private final JwtEncoder encoder;
    TokenService(JwtEncoder encoder) { this.encoder = encoder; }

    String issueAccessToken(Authentication auth) {
        Instant now = Instant.now();
        String scope = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(" "));
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("https://auth.example.com")
                .audience(List.of("orders-api"))
                .subject(auth.getName())
                .issuedAt(now)
                .expiresAt(now.plus(Duration.ofMinutes(10)))
                .id(UUID.randomUUID().toString())
                .claim("scope", scope)
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).keyId("2026-09-key").build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
```

> 💡 **Góc nhìn Senior:** "JWT là stateless nên scale tốt" chỉ đúng một nửa — ngay khi cần logout/thu hồi tức thì, bạn lại có state. Câu trả lời tốt trong phỏng vấn: nêu rõ yêu cầu (thu hồi trong bao lâu? bao nhiêu service verify?), rồi chọn: hệ thống nội bộ một app web → session + Spring Session Redis thường đơn giản và an toàn hơn; nhiều microservice/consumer, có IdP → JWT ngắn hạn do IdP (Keycloak, Okta, Auth0, Cognito) phát hành + refresh rotation. **Đừng tự viết auth server** cho production nếu có thể dùng IdP chuẩn hoặc Spring Authorization Server.

> ⚠️ **Lỗi thường gặp:** secret HS256 ngắn/yếu (brute-force offline được); không kiểm tra `aud`; TTL access token 30 ngày; đặt email/số điện thoại/role nhạy cảm trong payload; log nguyên token; dùng JWT làm session cho web app server-rendered (không cần thiết).

### 🛠 Bài tập phần 11

**Bài 11.1 — Giải mã JWT (Cơ bản)**
- Đề bài: lấy một JWT mẫu, decode 3 phần bằng Java (`Base64.getUrlDecoder()`), in header/payload; sửa payload (đổi `sub`) và chứng minh verify thất bại.
- Tiêu chí đạt: giải thích vì sao payload đọc được nhưng không sửa được.

**Bài 11.2 — Login phát JWT + refresh rotation (Trung bình)**
- Đề bài: endpoint `/api/auth/login` (trả access 10 phút + refresh 7 ngày), `/api/auth/refresh` (rotation, lưu refresh token **dạng hash** trong DB với `family_id`), `/api/auth/logout`. Resource server cấu hình bằng `oauth2ResourceServer().jwt()` với public key RSA.
- Tiêu chí đạt: dùng lại refresh token cũ → toàn bộ family bị thu hồi, trả 401; access token sau logout vẫn hợp lệ tới khi hết hạn — ghi nhận trade-off.

**Bài 11.3 — Thu hồi tức thì (Nâng cao)**
- Đề bài: thêm `token_version` cho user (claim `ver`); custom `OAuth2TokenValidator<Jwt>` so sánh với version trong Caffeine/Redis cache (TTL 30s); "logout all devices" tăng version.
- Tiêu chí đạt: sau logout-all, token cũ bị từ chối trong ≤ 30s; đo overhead mỗi request; giải thích lựa chọn TTL cache.

<details>
<summary>Gợi ý lời giải</summary>

- Bài 11.2: `@Bean JwtDecoder jwtDecoder() { return NimbusJwtDecoder.withPublicKey(rsaPublicKey).build(); }`, `@Bean JwtEncoder jwtEncoder() { return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey))); }`.
- Bài 11.3: `decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer), audienceValidator, versionValidator))`.
</details>

---

<a id="p12"></a>
## 12. OAuth2 / OpenID Connect & Resource Server

### 12.1 Vai trò
- **Resource Owner** (user), **Client** (app muốn truy cập), **Authorization Server** (Keycloak, Okta, Spring Authorization Server — phát token), **Resource Server** (API giữ dữ liệu — verify token).
- **OAuth 2.0** là framework **ủy quyền (authorization)**: access token cho phép client gọi API với scope nhất định. **OpenID Connect (OIDC)** là lớp **xác thực (authentication)** trên OAuth2: thêm **ID token** (JWT mô tả user cho *client*), endpoint `userinfo`, scope `openid`, discovery `/.well-known/openid-configuration`.
- ID token dành cho client đọc (biết ai đăng nhập); **không** dùng ID token để gọi API — dùng access token.

### 12.2 Các grant type
**Authorization Code + PKCE** (cho web app, SPA, mobile — mọi client có user):
```
1. Client sinh code_verifier (ngẫu nhiên 43–128 ký tự), code_challenge = BASE64URL(SHA256(code_verifier))
2. Redirect browser → /authorize?response_type=code&client_id=..&redirect_uri=..&scope=openid orders:read
                       &state=<chống CSRF>&code_challenge=..&code_challenge_method=S256
3. User đăng nhập & đồng ý tại Authorization Server
4. AS redirect về redirect_uri?code=AUTH_CODE&state=..   (client kiểm tra state)
5. Client POST /token: grant_type=authorization_code, code, redirect_uri, code_verifier (+ client secret nếu confidential)
6. AS kiểm tra SHA256(code_verifier) == code_challenge → trả access_token (+ refresh_token, id_token)
```
PKCE (RFC 7636) chống **đánh cắp authorization code** (kẻ có code nhưng không có `code_verifier` không đổi được token). OAuth 2.0 Security BCP (RFC 9700) và OAuth 2.1 khuyến nghị PKCE cho **mọi** client, kể cả confidential client.

**Client Credentials** — service-to-service, không có user: client xác thực bằng `client_id` + secret (hoặc private key JWT/mTLS) để lấy token với scope của chính nó.

**Đã lỗi thời:** Implicit grant (token trả qua URL fragment — dễ lộ) và Resource Owner Password Credentials (client cầm password của user) — bị loại khỏi OAuth 2.1.

### 12.3 Spring Boot: Resource Server
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://auth.example.com/realms/shop   # tự discovery JWKS, kiểm tra iss
          audiences: orders-api                               # Boot 3.x hỗ trợ kiểm tra aud
```
```java
@Bean
SecurityFilterChain api(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(a -> a
            .requestMatchers(HttpMethod.GET, "/api/v1/orders/**").hasAuthority("SCOPE_orders:read")
            .requestMatchers(HttpMethod.POST, "/api/v1/orders/**").hasAuthority("SCOPE_orders:write")
            .anyRequest().authenticated())
        .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(keycloakRolesConverter())))
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .csrf(c -> c.disable());
    return http.build();
}

// Mặc định: claim "scope"/"scp" → authority "SCOPE_xxx". Map thêm role từ claim tùy IdP (vd Keycloak realm_access.roles)
@Bean
JwtAuthenticationConverter keycloakRolesConverter() {
    var scopes = new JwtGrantedAuthoritiesConverter();
    var converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(jwt -> {
        Collection<GrantedAuthority> authorities = new ArrayList<>(scopes.convert(jwt));
        Map<String, Object> realm = jwt.getClaimAsMap("realm_access");
        if (realm != null && realm.get("roles") instanceof Collection<?> roles) {
            roles.forEach(r -> authorities.add(new SimpleGrantedAuthority("ROLE_" + r)));
        }
        return authorities;
    });
    converter.setPrincipalClaimName("sub");
    return converter;
}
```
- Resource server cache JWKS và tự refetch khi gặp `kid` mới (xoay khóa).
- Token opaque: `.oauth2ResourceServer(o -> o.opaqueToken(...))` với introspection.
- **Client** (gọi API khác bằng client credentials): `spring-boot-starter-oauth2-client` + `OAuth2AuthorizedClientManager`; Spring Security 6.4+ có tích hợp trực tiếp với `RestClient` (`OAuth2ClientHttpRequestInterceptor`).
- **Login cho web app**: `.oauth2Login()` (authorization code + PKCE, OIDC) — app trở thành OAuth2 client giữ token phía server (mô hình BFF).

> 💡 **Góc nhìn Senior:** Trong microservices: gateway/BFF xác thực user, các service phía sau là resource server verify JWT độc lập (không gọi auth server mỗi request); gọi giữa service: **token exchange** (RFC 8693) hoặc propagate token của user (cẩn thận audience) hoặc client credentials (mất ngữ cảnh user). Newman (*Building Microservices*, chương Security) bàn về các trade-off này, bao gồm vấn đề "confused deputy". Quyền chi tiết (user này được xem order nào) **không** nằm trong scope — vẫn phải kiểm tra ở service (Phần 14, BOLA).

### 🛠 Bài tập phần 12

**Bài 12.1 — Keycloak + Resource Server (Cơ bản)**
- Đề bài: chạy Keycloak bằng Docker, tạo realm `shop`, client `orders-api`, user có role `customer`; cấu hình Spring Boot resource server với `issuer-uri`; lấy token bằng Postman (authorization code + PKCE).
- Tiêu chí đạt: gọi API thành công với token; token hết hạn → 401 kèm `WWW-Authenticate: Bearer error="invalid_token"`.

**Bài 12.2 — Mapping role & scope (Trung bình)**
- Đề bài: dùng `JwtAuthenticationConverter` như ví dụ để map `realm_access.roles`; endpoint admin yêu cầu `ROLE_admin`, endpoint đọc yêu cầu `SCOPE_orders:read`.
- Tiêu chí đạt: test với `jwt()` request post-processor của `spring-security-test` (`.with(jwt().authorities(...))`), không cần Keycloak trong unit test.

**Bài 12.3 — Service-to-service (Nâng cao)**
- Đề bài: `order-service` gọi `inventory-service` bằng client credentials (client riêng trong Keycloak, scope `inventory:reserve`); `inventory-service` chỉ chấp nhận token có `aud=inventory-service`. Token được cache và tự làm mới trước khi hết hạn.
- Tiêu chí đạt: không gọi token endpoint ở mỗi request (đếm bằng log/metrics); token user không dùng được để gọi trực tiếp `inventory:reserve`.

<details>
<summary>Gợi ý lời giải</summary>

- Bài 12.3: cấu hình `spring.security.oauth2.client.registration.inventory.authorization-grant-type=client_credentials`; dùng `OAuth2AuthorizedClientManager` (cache trong `OAuth2AuthorizedClientService`/repository) và interceptor gắn `Bearer` vào `RestClient`. Kiểm tra `aud` bằng `JwtClaimValidator<List<String>>("aud", aud -> aud.contains("inventory-service"))` hoặc property `audiences`.
</details>

---

<a id="p13"></a>
## 13. Authorization: URL & method security, CSRF

### 13.1 Method security
```java
@Configuration
@EnableMethodSecurity   // prePostEnabled=true mặc định; securedEnabled/jsr250Enabled tùy chọn
class MethodSecurityConfig { }

@Service
class OrderQueryService {
    private final OrderRepository repo;
    OrderQueryService(OrderRepository repo) { this.repo = repo; }

    @PreAuthorize("hasRole('ADMIN') or #customerId == authentication.name")
    public List<OrderDto> ordersOf(String customerId) { return repo.findByCustomerId(customerId); }

    @PostAuthorize("returnObject.customerId() == authentication.name or hasRole('SUPPORT')")
    public OrderDto get(long id) { return repo.findDtoById(id).orElseThrow(); }

    @PreAuthorize("@orderPermissions.canCancel(#id, authentication)")  // gọi bean để kiểm tra phức tạp
    public void cancel(long id) { /* ... */ }
}

@Component("orderPermissions")
class OrderPermissions {
    private final OrderRepository repo;
    OrderPermissions(OrderRepository repo) { this.repo = repo; }
    public boolean canCancel(long orderId, Authentication auth) {
        return repo.findOwnerId(orderId).map(owner -> owner.equals(auth.getName())).orElse(false);
    }
}
```
- Hoạt động qua **AOP proxy** (Module 07) → self-invocation **bỏ qua kiểm tra quyền** — lỗ hổng thật, không chỉ là bug.
- `@PostAuthorize` chạy method **rồi** mới kiểm tra → không dùng cho method có side effect.
- `@PostFilter`/`@PreFilter` lọc collection trong bộ nhớ → tệ với dữ liệu lớn; nên lọc ở query (`WHERE owner_id = :me`).
- Meta-annotation: `@PreAuthorize("hasRole('ADMIN')") @interface IsAdmin {}`; Spring Security 6.3+ hỗ trợ template tham số cho meta-annotation.
- Kết hợp: URL security để chặn thô (deny-by-default), method security cho quyền trên đối tượng. Lỗi `AccessDeniedException` từ method → `ExceptionTranslationFilter` → 403 (miễn là không bị `@ExceptionHandler(Exception.class)` nuốt).

### 13.2 CSRF — khi nào quan trọng
**Cross-Site Request Forgery**: site độc hại khiến trình duyệt của nạn nhân gửi request tới site của bạn, trình duyệt **tự động đính kèm cookie** (session) → server tưởng là user thật.
- **Cần** CSRF protection khi: xác thực dựa trên thứ trình duyệt **tự gửi** — session cookie, cookie chứa JWT, HTTP Basic/Digest được trình duyệt nhớ, client certificate.
- **Có thể tắt** khi: API stateless, token gửi bằng header `Authorization: Bearer` do JS chủ động gắn (site khác không gắn được), không dùng cookie để xác thực.
- Spring Security bật CSRF mặc định cho các method thay đổi trạng thái (POST/PUT/PATCH/DELETE). Synchronizer token pattern: token trong session, client gửi lại qua header `X-CSRF-TOKEN`/`X-XSRF-TOKEN` hoặc form field `_csrf`.
- SPA dùng cookie session: `CookieCsrfTokenRepository.withHttpOnlyFalse()` (JS đọc cookie `XSRF-TOKEN` và gửi header `X-XSRF-TOKEN`). Spring Security 6 mặc định `XorCsrfTokenRequestAttributeHandler` (chống BREACH, token được "mask" khác nhau mỗi lần) và **deferred** loading token → SPA cần cấu hình thêm theo hướng dẫn "Single-Page Applications" trong reference (các bản Spring Security 6.x mới có shortcut `csrf.spa()`).
- Lớp phòng thủ bổ sung: cookie `SameSite=Lax/Strict`, kiểm tra `Origin`/`Referer`. `SameSite` không thay thế hoàn toàn CSRF token (subdomain cùng site, trình duyệt cũ, GET có side effect).
- **GET không bao giờ được thay đổi trạng thái** — CSRF protection không bảo vệ GET.

> ⚠️ **Lỗi thường gặp:** `csrf().disable()` "cho hết lỗi 403" trên app dùng session cookie → mở lỗ hổng CSRF; ngược lại, bật CSRF trên API thuần bearer token rồi mất thời gian xử lý token không cần thiết. Tiêu chí quyết định: **trình duyệt có tự gửi credential không?**

### 🛠 Bài tập phần 13

**Bài 13.1 — Ownership check (Cơ bản)**
- Đề bài: thêm `@PreAuthorize` cho `GET /api/v1/orders/{id}` sao cho user chỉ xem được đơn của mình, `SUPPORT` xem tất cả.
- Tiêu chí đạt: test với `@WithMockUser(username="alice")` truy cập đơn của bob → 403 (hoặc 404 nếu bạn chọn che giấu — giải thích lựa chọn).

**Bài 13.2 — Bẫy self-invocation trong security (Trung bình)**
- Đề bài: tạo method public không có annotation `exportAll()` gọi `this.deleteAll()` có `@PreAuthorize("hasRole('ADMIN')")`. Chứng minh user thường xóa được dữ liệu qua `exportAll()`. Sửa.
- Tiêu chí đạt: test đỏ → xanh; viết rule ArchUnit (hoặc checklist review) phát hiện mẫu này.

**Bài 13.3 — CSRF cho SPA dùng cookie (Nâng cao)**
- Đề bài: app BFF có session cookie, SPA gọi `POST /api/orders`. Cấu hình CSRF với `CookieCsrfTokenRepository` cho SPA; viết trang HTML "độc hại" ở origin khác submit form POST tới API.
- Tiêu chí đạt: request từ trang độc hại bị 403; SPA hợp lệ thành công; giải thích vai trò của `SameSite` trong demo.

<details>
<summary>Gợi ý lời giải</summary>

- Bài 13.2: tách `deleteAll` sang bean khác, hoặc đặt `@PreAuthorize` ở *method public được gọi từ ngoài* (`exportAll`) — nguyên tắc: kiểm tra quyền tại **biên** (entry point) của service.
- Bài 13.3:

```java
http.csrf(csrf -> csrf
        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())); // theo mẫu trong Spring Security reference
// Bản Spring Security mới hơn có shortcut: http.csrf(csrf -> csrf.spa());
```
</details>

---

<a id="p14"></a>
## 14. Lỗ hổng phổ biến trong REST API — OWASP API Top 10

### 14.1 OWASP API Security Top 10 (2023)
| # | Tên | Ví dụ | Phòng tránh trong Spring |
|---|---|---|---|
| API1 | **Broken Object Level Authorization (BOLA/IDOR)** | `GET /orders/1002` — đổi id thấy đơn người khác | luôn kiểm tra ownership ở service/query (`findByIdAndOwnerId`), `@PreAuthorize` với bean permission, không tin id từ client cho tenant/owner |
| API2 | Broken Authentication | không rate limit login, JWT không kiểm `aud`/`exp`, secret yếu | dùng IdP chuẩn, resource server đúng cấu hình, rate limit, MFA |
| API3 | **Broken Object Property Level Authorization** (gộp *mass assignment* + *excessive data exposure*) | `PATCH /users/me {"role":"ADMIN"}`; trả cả `passwordHash`, `internalNote` | DTO riêng cho input/output, whitelist field, không bind trực tiếp vào entity, `@JsonIgnore` không đủ — thiết kế DTO |
| API4 | Unrestricted Resource Consumption | `size=1000000`, upload 2GB, regex nặng, GraphQL lồng sâu | giới hạn page size, `spring.servlet.multipart.max-file-size`, timeout, rate limit, quota |
| API5 | Broken Function Level Authorization | user thường gọi được `/api/admin/...` | deny-by-default, phân quyền theo URL + method, tách chain admin |
| API6 | Unrestricted Access to Sensitive Business Flows | bot mua sạch hàng flash sale, spam tạo tài khoản | rate limit theo nghiệp vụ, CAPTCHA, device fingerprint, giới hạn số lượng/người |
| API7 | Server-Side Request Forgery (SSRF) | API nhận `imageUrl` rồi server fetch → truy cập `http://169.254.169.254` (metadata cloud) | whitelist domain, chặn IP nội bộ/link-local sau khi resolve DNS, không follow redirect tùy tiện |
| API8 | Security Misconfiguration | Actuator `heapdump` public, CORS `*` + credentials, stack trace trong lỗi, thiếu header bảo mật | cấu hình mặc định an toàn của Boot 3, review cấu hình, security headers |
| API9 | Improper Inventory Management | API v1 cũ/endpoint debug/Swagger lộ ra internet | danh mục API, tắt version cũ, gateway quản lý |
| API10 | Unsafe Consumption of APIs | tin tưởng mù quáng dữ liệu từ API bên thứ ba | validate response, timeout, TLS, giới hạn kích thước |

### 14.2 BOLA — lỗ hổng số 1
```java
// ❌ Lỗ hổng: chỉ kiểm tra "đã đăng nhập"
@GetMapping("/api/v1/orders/{id}")
OrderDto get(@PathVariable long id) {
    return orderRepository.findById(id).map(OrderDto::from).orElseThrow(NotFoundException::new);
}

// ✅ Ràng buộc owner ngay trong truy vấn; không lộ sự tồn tại của đơn người khác (404 thay vì 403)
@GetMapping("/api/v1/orders/{id}")
OrderDto get(@PathVariable long id, @AuthenticationPrincipal Jwt jwt) {
    return orderRepository.findByIdAndCustomerId(id, jwt.getSubject())
            .map(OrderDto::from)
            .orElseThrow(() -> new OrderNotFoundException(id));
}
```
UUID/ID khó đoán **không** thay thế được kiểm tra quyền (chỉ giảm khả năng enumeration). Multi-tenant: lọc `tenant_id` ở tầng repository (Hibernate `@TenantId` từ Hibernate 6, filter, hoặc Row-Level Security của PostgreSQL) để không phụ thuộc vào việc dev nhớ thêm điều kiện.

### 14.3 Mass assignment
```java
// ❌ Bind trực tiếp entity: client gửi {"email":"x","role":"ADMIN","balance":999999}
@PutMapping("/api/v1/users/me")
User update(@RequestBody User user) { return userRepository.save(user); }

// ✅ DTO chỉ chứa field được phép sửa
record UpdateProfileRequest(@Size(max = 100) String displayName, @URL String avatarUrl) {}

@PutMapping("/api/v1/users/me")
ProfileDto update(@Valid @RequestBody UpdateProfileRequest req, @AuthenticationPrincipal Jwt jwt) {
    return profileService.update(jwt.getSubject(), req);
}
```
Bật `spring.jackson.deserialization.fail-on-unknown-properties=true` cho các endpoint nhạy cảm (hoặc `@JsonIgnoreProperties(ignoreUnknown = false)` trên DTO) để phát hiện client gửi field lạ — đánh đổi: giảm tính "tolerant reader".

### 14.4 Rate limiting & bảo vệ tài nguyên
- Tầng ưu tiên: **API Gateway/ingress** (Spring Cloud Gateway `RequestRateLimiter` với Redis, NGINX, Kong, cloud WAF) → tầng app (Bucket4j, Resilience4j `RateLimiter`) cho quota theo nghiệp vụ.
- Key: theo user/API key/tenant (sau xác thực), theo IP (trước xác thực — cẩn thận NAT và `X-Forwarded-For` giả mạo: chỉ tin header từ proxy tin cậy, `server.forward-headers-strategy=native/framework`).
- Thuật toán: token bucket (cho phép burst), sliding window; trả `429` + `Retry-After` (và tùy chọn header `RateLimit-*`).
- Giới hạn khác: kích thước request body (`server.tomcat.max-swallow-size`, `spring.servlet.multipart.*`), độ sâu JSON/kích thước chuỗi (Jackson 2.15+ có `StreamReadConstraints` mặc định), timeout cho mọi lời gọi ra ngoài, page size tối đa.

### 14.5 Security headers & các điểm khác
- Spring Security mặc định thêm: `X-Content-Type-Options: nosniff`, `Cache-Control: no-cache, no-store...`, `X-Frame-Options: DENY`, `Strict-Transport-Security` (chỉ trên HTTPS), `X-XSS-Protection: 0`. Cấu hình thêm `Content-Security-Policy` cho app có UI.
- Không đặt dữ liệu nhạy cảm trong URL (token, password) — URL bị log ở proxy/access log.
- Log audit cho hành động nhạy cảm (ai, làm gì, lúc nào, từ đâu), không log secret.
- Quản lý dependency: Dependabot/Renovate, OWASP Dependency-Check/Snyk — nhiều sự cố lớn đến từ thư viện (Log4Shell, Spring4Shell CVE-2022-22965 — data binding trên JDK 9+ với WAR trên Tomcat).
- Kiểm thử: test phân quyền tự động cho **mỗi** endpoint (ma trận role × endpoint), DAST (OWASP ZAP) trong pipeline.

> 💡 **Góc nhìn Senior:** Bảo mật là thuộc tính của **hệ thống**, không phải một annotation. Khi review một API, hỏi theo thứ tự: Ai gọi được (authN)? Được gọi chức năng này không (function-level)? Được truy cập *đối tượng* này không (object-level)? Được đọc/ghi *thuộc tính* này không (property-level)? Gọi bao nhiêu lần (resource consumption)? Dữ liệu từ ngoài có bị tin mù quáng không (SSRF, unsafe consumption)? Câu trả lời cho từng câu phải nằm trong code/test, không nằm trong "quy ước miệng".

### 🛠 Bài tập phần 14

**Bài 14.1 — Săn BOLA (Cơ bản)**
- Đề bài: cho một codebase mẫu có 5 endpoint (`/orders/{id}`, `/orders/{id}/invoice`, `/addresses/{id}`, `/users/{id}/avatar`, `/orders?customerId=`). Tìm và sửa các endpoint bị BOLA.
- Tiêu chí đạt: mỗi endpoint có test "user A truy cập tài nguyên của B → 404/403".

**Bài 14.2 — Mass assignment & data exposure (Trung bình)**
- Đề bài: refactor controller đang nhận/trả entity `User` (có `role`, `passwordHash`, `failedLoginCount`) sang DTO input/output riêng; thêm test JSON contract (`@JsonTest`) đảm bảo response không bao giờ chứa `passwordHash`.
- Tiêu chí đạt: gửi `{"role":"ADMIN"}` không làm thay đổi role; test contract fail nếu ai đó thêm field nhạy cảm vào DTO output.

**Bài 14.3 — SSRF & rate limit (Nâng cao)**
- Đề bài: endpoint `POST /api/v1/profile/avatar-from-url` tải ảnh từ URL người dùng cung cấp. Implement an toàn: chỉ `https`, resolve DNS và chặn IP private/loopback/link-local (kể cả IPv6, kể cả sau redirect — tắt follow redirect), giới hạn kích thước 2MB và content-type `image/*`, timeout 3s; endpoint có rate limit 5 lần/phút/user (Bucket4j hoặc tự cài).
- Tiêu chí đạt: test với `http://127.0.0.1`, `http://169.254.169.254/latest/meta-data`, `https://[::1]`, domain trỏ về IP nội bộ, file 10MB đều bị từ chối; lần gọi thứ 6 trong 1 phút → 429.

<details>
<summary>Gợi ý lời giải</summary>

- Bài 14.2: response DTO là record chỉ có field công khai; `@JsonTest` + `JacksonTester` kiểm tra `doesNotHaveJsonPath("$.passwordHash")`.
- Bài 14.3:

```java
static void assertPublicAddress(URI uri) throws UnknownHostException {
    if (!"https".equals(uri.getScheme())) throw new BadRequestException("https only");
    for (InetAddress a : InetAddress.getAllByName(uri.getHost())) {
        if (a.isAnyLocalAddress() || a.isLoopbackAddress() || a.isLinkLocalAddress()
                || a.isSiteLocalAddress() || a.isMulticastAddress()
                || (a instanceof Inet6Address && (a.getAddress()[0] & 0xFE) == 0xFC)) { // fc00::/7 ULA
            throw new BadRequestException("address not allowed");
        }
    }
}
```
Lưu ý **DNS rebinding**: kiểm tra xong rồi client HTTP resolve lại có thể ra IP khác — kết nối trực tiếp tới IP đã kiểm tra (đặt header `Host`/SNI) hoặc dùng egress proxy có whitelist là giải pháp chắc chắn hơn.
</details>

---

<a id="du-an-mini"></a>
## Dự án mini

### "Secure Order API" — 2 ngày

Xây dựng REST API quản lý đơn hàng đa người dùng, bảo mật theo chuẩn production, dùng lại starter ở Module 07 nếu có.

**Yêu cầu chức năng**
1. Resource: `products` (public read, admin write), `orders` (customer tạo/xem/hủy đơn **của mình**, support xem tất cả), `orders/{id}/payments` (tạo thanh toán với `Idempotency-Key`).
2. API theo chuẩn REST: method & status code đúng, `201 + Location`, `ProblemDetail` cho **mọi** lỗi (kể cả 401/403/404/429 từ filter), validation (có ít nhất 1 custom validator cross-field), phân trang keyset cho `GET /orders`, filter/sort theo whitelist.
3. Concurrency: `ETag` + `If-Match` cho cập nhật product (412/428).
4. Security:
   - Resource server JWT (Keycloak qua Docker Compose **hoặc** auth module tự phát hành RS256 + refresh token rotation).
   - URL security deny-by-default + `@PreAuthorize` cho ownership; không BOLA, không mass assignment.
   - CORS cho SPA `http://localhost:5173`; giải thích lựa chọn CSRF (tắt hay bật) trong README.
   - Rate limit: login 5/phút/IP, tạo đơn 30/phút/user.
5. Gọi `inventory-service` giả lập (WireMock) bằng `RestClient`/HTTP interface với timeout, connection pool và xử lý lỗi (fallback hoặc 503 có `Retry-After`).
6. OpenAPI (springdoc) có security scheme Bearer; Swagger UI chỉ bật ở profile `local`.

**Yêu cầu phi chức năng**
- Boot 3.x, Java 21, có thể bật virtual threads; Actuator an toàn (Module 07).
- Không secret trong repo; password (nếu tự quản lý user) bằng `DelegatingPasswordEncoder`.
- Mọi lời gọi ra ngoài có timeout; log có correlation id; không log token/PII.
- Test: `@WebMvcTest` cho controller (với `spring-security-test`), `@SpringBootTest` + Testcontainers cho luồng chính, **ma trận phân quyền** (role × endpoint × kỳ vọng status) chạy tự động, test idempotency đồng thời.

**Tiêu chí chấm (100 điểm)**
| Tiêu chí | Điểm |
|---|---|
| Thiết kế REST đúng (URI, method, status, pagination, ETag, idempotency key) | 20 |
| Validation & error handling thống nhất (`ProblemDetail` ở mọi tầng) | 15 |
| Kiến trúc Spring Security đúng (filter chain, JWT validation iss/aud/exp, entry point/denied handler) | 20 |
| Không có lỗ hổng OWASP API Top 10 hiển nhiên (BOLA, mass assignment, rate limit, misconfiguration) — có test chứng minh | 20 |
| HTTP client production-grade (timeout, pool, error handling, observability) | 10 |
| Chất lượng test, README giải thích quyết định & trade-off (session vs JWT, CSRF, versioning) | 15 |

---

<a id="checklist-tu-danh-gia"></a>
## Checklist tự đánh giá
- [ ] Tôi phân biệt được Servlet Filter, `HandlerInterceptor`, Spring AOP và chọn đúng công cụ cho logging, security, transaction.
- [ ] Tôi mô tả được từng bước của `DispatcherServlet.doDispatch` (HandlerMapping, HandlerAdapter, argument resolver, message converter, exception resolver, ViewResolver).
- [ ] Tôi biết các thay đổi web của Spring 6/Boot 3 (trailing slash, `PathPatternParser`, `jakarta.*`, built-in method validation, `ProblemDetail`).
- [ ] Tôi dùng thành thạo Bean Validation: cascade `@Valid`, groups với `@Validated`, custom `ConstraintValidator` cross-field.
- [ ] Tôi xây được global exception handling trả `ProblemDetail` thống nhất, kể cả lỗi phát sinh trong filter/security.
- [ ] Tôi giải thích được safe vs idempotent, chọn status code đúng (401 vs 403, 400 vs 422, 409, 412, 428, 429).
- [ ] Tôi so sánh được offset vs keyset pagination và implement được keyset.
- [ ] Tôi so sánh được các chiến lược versioning và nguyên tắc tránh breaking change.
- [ ] Tôi thiết kế được idempotency key và dùng ETag cho caching + optimistic concurrency.
- [ ] Tôi chọn được giữa RestTemplate/WebClient/RestClient/HTTP interface; cấu hình được connect/read/pool-acquire timeout và kích thước pool.
- [ ] Tôi giải thích được khi nào WebFlux đáng dùng so với MVC + virtual threads, và nguy cơ blocking trên event loop.
- [ ] Tôi cấu hình CORS đúng với Spring Security và hiểu CORS không phải cơ chế bảo vệ server.
- [ ] Tôi vẽ được kiến trúc `DelegatingFilterProxy` → `FilterChainProxy` → `SecurityFilterChain` → filters, và luồng `AuthenticationManager` → `AuthenticationProvider` → `UserDetailsService`/`JwtDecoder`.
- [ ] Tôi giải thích được `SecurityContextHolder` strategies và cách truyền context sang thread khác.
- [ ] Tôi chọn đúng thuật toán băm mật khẩu (Argon2id/bcrypt), hiểu `DelegatingPasswordEncoder` và giới hạn 72 byte của bcrypt.
- [ ] Tôi so sánh được session vs JWT, giải thích cấu trúc JWT, HS256 vs RS256, refresh token rotation và các chiến lược revocation.
- [ ] Tôi mô tả được Authorization Code + PKCE, Client Credentials, khác biệt OAuth2 vs OIDC, ID token vs access token, và cấu hình Resource Server.
- [ ] Tôi dùng `@PreAuthorize` cho ownership check và biết bẫy self-invocation trong method security.
- [ ] Tôi giải thích được khi nào cần CSRF protection và khi nào tắt được.
- [ ] Tôi kể được OWASP API Top 10 (2023) và cách phòng tránh BOLA, mass assignment, unrestricted resource consumption, SSRF trong Spring.

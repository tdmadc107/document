# Câu hỏi phỏng vấn — Module 08: Spring MVC, REST API & Spring Security

> Giáo trình tương ứng: [Module 08 — Spring MVC, REST API & Spring Security](../01-giao-trinh/08-spring-web-rest-security.md)

**Cách dùng:** đọc câu hỏi, **tự trả lời thành tiếng** trong 1–2 phút *rồi* mới mở "Đáp án". **Trả lời ngắn** là phần phải nói được trong 30 giây; **Giải thích chi tiết** là phần đào sâu; tự trả lời **Câu hỏi nối tiếp** trước khi đọc gợi ý. Mục **⚠️ Câu trả lời gây điểm trừ** là những câu khiến bạn bị đánh giá dưới level Senior.

**Mức độ:** 🟢 Cơ bản · 🟡 Senior · 🔴 Xoáy sâu. **[Tình huống]** = câu "bạn sẽ làm gì"; **[Đọc code]** = câu đọc code tìm lỗi. Mặc định Spring Boot 3.x, Spring Framework 6.x, Spring Security 6.x; khác biệt với Boot 2/Security 5 được ghi chú.

Tổng: **58 câu** — 13 🟢 · 29 🟡 · 16 🔴.

## Mục lục

| Nhóm | Chủ đề | Câu hỏi |
|---|---|---|
| [A](#nhom-a) | Servlet container, Filter / Interceptor / AOP | Q1–Q4 |
| [B](#nhom-b) | DispatcherServlet | Q5–Q8 |
| [C](#nhom-c) | Controller, data binding, validation | Q9–Q13 |
| [D](#nhom-d) | Exception handling & `ProblemDetail` | Q14–Q16 |
| [E](#nhom-e) | Thiết kế REST: method, status code, pagination | Q17–Q21 |
| [F](#nhom-f) | REST nâng cao: versioning, idempotency key, ETag, OpenAPI | Q22–Q25 |
| [G](#nhom-g) | HTTP clients, timeout, connection pool | Q26–Q30 |
| [H](#nhom-h) | WebFlux vs MVC + virtual threads; CORS | Q31–Q34 |
| [I](#nhom-i) | Kiến trúc Spring Security | Q35–Q39 |
| [J](#nhom-j) | Authentication & password | Q40–Q42 |
| [K](#nhom-k) | Session vs JWT | Q43–Q47 |
| [L](#nhom-l) | OAuth2 / OIDC & Resource Server | Q48–Q51 |
| [M](#nhom-m) | Method security & CSRF | Q52–Q53 |
| [N](#nhom-n) | OWASP API Security Top 10 | Q54–Q58 |

---

<a id="nhom-a"></a>
## A. Servlet container, Filter / Interceptor / AOP

### Q1. 🟢 Servlet Filter, `HandlerInterceptor` và Spring AOP khác nhau thế nào? Mỗi cái dùng cho việc gì?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **Filter** thuộc Servlet spec, chạy cho **mọi** request vào container (kể cả static, error dispatch, request không có handler), không biết controller nào sẽ xử lý, có thể bọc request/response — dùng cho security, CORS, correlation id, logging thô, rate limit, compression. **`HandlerInterceptor`** thuộc Spring MVC (`DispatcherServlet`), chỉ cho request đã map tới handler, **biết `HandlerMethod`** — dùng cho audit/timing theo controller, locale, kiểm tra theo handler. **AOP** áp cho method của **mọi bean** (không riêng web) — transaction, cache, metrics nghiệp vụ.

**Giải thích chi tiết:**

```
Client → Tomcat → Filter 1 → Spring Security FilterChainProxy → ... → DispatcherServlet
       → Interceptor.preHandle → Controller (AOP proxy → target) → postHandle → afterCompletion
```

- `OncePerRequestFilter` tránh chạy lại khi forward/error dispatch.
- Interceptor `preHandle` trả `false` để dừng chain (tự ghi response).

**Câu hỏi nối tiếp:**
- *Spring Security là filter hay interceptor?* → Filter (`DelegatingFilterProxy` → `FilterChainProxy`), nên lỗi của nó không đi qua `@ControllerAdvice` (Q16).

**⚠️ Câu trả lời gây điểm trừ:** "Ba cái như nhau, dùng cái nào cũng được."

**📖 Ôn lại:** [§1.2 Ba tầng chặn request](../01-giao-trinh/08-spring-web-rest-security.md#p1)

</details>

### Q2. 🟡 [Đọc code] Filter sau có những bug gì?

```java
@Component
class AuditFilter extends OncePerRequestFilter {
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        MDC.put("user", req.getHeader("X-User"));
        String body = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        log.info("Request {} body={}", req.getRequestURI(), body);
        chain.doFilter(req, res);
    }
}
```

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** (1) Đọc `getInputStream()` **tiêu thụ stream** — stream chỉ đọc một lần, controller nhận body rỗng (`HttpMessageNotReadableException`); (2) `MDC.put` **không remove trong `finally`** → thread của pool được tái sử dụng mang giá trị của request trước sang request sau (log sai user); (3) tin header `X-User` từ client — giả mạo được, và đưa thẳng vào log (log injection); (4) log toàn bộ body — có thể chứa password/thẻ/PII, file upload lớn; (5) là `@Component` nên Boot tự đăng ký cho mọi URL — nếu cũng add vào Security chain sẽ chạy **hai lần**.

**Giải thích chi tiết:**

```java
var wrapped = new ContentCachingRequestWrapper(req);
var wrappedRes = new ContentCachingResponseWrapper(res);
MDC.put("cid", sanitizedCorrelationId(req));
try {
    chain.doFilter(wrapped, wrappedRes);
} finally {
    logBody(mask(truncate(wrapped.getContentAsByteArray(), 2048)));   // sau khi controller đã đọc
    wrappedRes.copyBodyToResponse();                                   // BẮT BUỘC, nếu không client nhận body rỗng
    MDC.remove("cid");
}
```

- User nên lấy từ `SecurityContext` (sau khi xác thực), không từ header tùy ý.
- Không log `multipart/form-data`; giới hạn kích thước; mask trường nhạy cảm.

**Câu hỏi nối tiếp:**
- *Chặn Boot tự đăng ký filter?* → `FilterRegistrationBean<AuditFilter>` với `setEnabled(false)`.

**⚠️ Câu trả lời gây điểm trừ:** Chỉ thấy vấn đề hiệu năng của log.

**📖 Ôn lại:** [§1 Góc nhìn Senior & Lỗi thường gặp](../01-giao-trinh/08-spring-web-rest-security.md#p1)

</details>

### Q3. 🟡 Bạn cần thêm header `X-Request-Time` vào response của `@RestController` bằng `HandlerInterceptor.postHandle` nhưng header không xuất hiện. Vì sao? `postHandle` có chạy khi controller ném exception không?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Với `@ResponseBody`/`ResponseEntity`, `HttpMessageConverter` ghi body và **commit response** ngay trong `handle()` — trước khi `postHandle` chạy, nên thêm header lúc đó vô tác dụng. Dùng **`ResponseBodyAdvice`** (chạy trước khi converter ghi), hoặc filter bọc response. `postHandle` **không** chạy khi controller ném exception; dùng `afterCompletion` cho cleanup (luôn chạy nếu `preHandle` trả `true`).

**Giải thích chi tiết:**

```java
@RestControllerAdvice
class TimingAdvice implements ResponseBodyAdvice<Object> {
    public boolean supports(MethodParameter p, Class<? extends HttpMessageConverter<?>> c) { return true; }
    public Object beforeBodyWrite(Object body, MethodParameter p, MediaType t,
            Class<? extends HttpMessageConverter<?>> c, ServerHttpRequest req, ServerHttpResponse res) {
        res.getHeaders().add("X-Request-Time", Instant.now().toString());
        return body;
    }
}
```

- Thứ tự log khi thành công: filter vào → preHandle → aspect → controller → postHandle → afterCompletion → filter ra. Khi lỗi: không có postHandle; exception resolver chạy rồi afterCompletion.

**Câu hỏi nối tiếp:**
- *Interceptor có áp cho lỗi 404 không có handler?* → Không có handler thì không có chain interceptor của handler đó; filter thì vẫn chạy.

**⚠️ Câu trả lời gây điểm trừ:** Không biết response đã commit.

**📖 Ôn lại:** [§1.2 Góc nhìn Senior](../01-giao-trinh/08-spring-web-rest-security.md#p1)

</details>

### Q4. 🟡 [Tình huống] DB chậm đột ngột (mỗi query 10s). Vài phút sau cả `/actuator/health` cũng không phản hồi. Giải thích theo mô hình thread của Tomcat và cách phòng.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Tomcat (Spring MVC) dùng **thread-per-request**: mặc định `server.tomcat.threads.max=200`, `accept-count=100`, `max-connections=8192`. Mỗi request chờ DB giữ một thread; khi 200 thread đều chờ, request mới (kể cả health check trên cùng connector) xếp hàng → timeout → probe fail. Phòng: **timeout ở mọi lời gọi ra ngoài** (Hikari `connection-timeout`, query timeout, HTTP client timeout), **bulkhead** (giới hạn đồng thời theo dependency), circuit breaker, tách **`management.server.port`** cho actuator, và readiness — không liveness — phụ thuộc DB.

**Giải thích chi tiết:**
- Little's Law: concurrency = throughput × latency. 100 req/s × 10s = 1.000 request đồng thời > 200 thread.
- Hikari `maximum-pool-size=10`: 190 thread còn lại chờ mượn connection (`connection-timeout` mặc định 30s) → càng tệ.
- Virtual threads (Boot 3.2+) bỏ giới hạn 200 thread nhưng **không** bỏ giới hạn connection pool — nút thắt chỉ dịch chuyển; vẫn cần timeout và giới hạn đồng thời.

**Câu hỏi nối tiếp:**
- *Tăng `threads.max` lên 2000?* → Tốn bộ nhớ stack, context switching, và đẩy thêm tải vào DB đang chậm — chữa triệu chứng.

**⚠️ Câu trả lời gây điểm trừ:** "Tăng thread pool và connection pool lên."

**📖 Ôn lại:** [§1.1 Servlet container](../01-giao-trinh/08-spring-web-rest-security.md#p1)

</details>

---

<a id="nhom-b"></a>
## B. DispatcherServlet

### Q5. 🟢 Mô tả luồng một request JSON đi qua `DispatcherServlet` tới controller và quay ra.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `doDispatch`: (1) kiểm tra multipart; (2) `getHandler()` duyệt các `HandlerMapping` (`RequestMappingHandlerMapping` cho `@RequestMapping`) → `HandlerExecutionChain` (handler + interceptor); không có → 404; (3) `getHandlerAdapter()` → `RequestMappingHandlerAdapter`; (4) `preHandle`; (5) `handle()`: các `HandlerMethodArgumentResolver` dựng tham số (`@RequestBody` → `HttpMessageConverter`/Jackson đọc body; `@PathVariable`, `@RequestParam`...), validation, gọi method controller (qua AOP proxy nếu có), `HandlerMethodReturnValueHandler` ghi kết quả bằng converter theo `Accept`; (6) `postHandle`; (7) nếu exception → `HandlerExceptionResolver`; nếu có view → `ViewResolver`; (8) `afterCompletion`.

**Giải thích chi tiết:**
- `DispatcherServlet` là **Front Controller**; `HandlerAdapter` là **Adapter pattern** cho nhiều loại handler (`@Controller`, `HttpRequestHandler`, `HandlerFunction`).
- `@RestController` = `@Controller` + `@ResponseBody` → không qua ViewResolver.
- Xem registry mapping: `/actuator/mappings`.

**Câu hỏi nối tiếp:**
- *Custom một bước?* → `HandlerMethodArgumentResolver` (Q7), `HttpMessageConverter` (Q6), `HandlerExceptionResolver`/`@ControllerAdvice`.

**⚠️ Câu trả lời gây điểm trừ:** "Request vào controller rồi Jackson trả JSON" — không nêu được thành phần nào.

**📖 Ôn lại:** [§2.1 Luồng chính](../01-giao-trinh/08-spring-web-rest-security.md#p2)

</details>

### Q6. 🟡 Content negotiation hoạt động thế nào? Khi nào trả 406, khi nào 415? Vì sao không nên `new ObjectMapper()` trong code?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `HttpMessageConverter` chọn theo **`Content-Type`** khi đọc body và theo **`Accept` + kiểu trả về** khi ghi. Không converter nào đọc được Content-Type của request → **415 Unsupported Media Type**; không converter nào ghi được kiểu client chấp nhận → **406 Not Acceptable**. `ObjectMapper` mà converter dùng là bean do Boot cấu hình (module `JavaTimeModule`, naming, property `spring.jackson.*`); `new ObjectMapper()` lệch cấu hình → JSON trong log/Kafka khác JSON của API, lỗi ngày giờ.

**Giải thích chi tiết:**
- Thêm format (CSV): extends `AbstractHttpMessageConverter<ProductDto>` với `MediaType("text","csv")`, đăng ký bằng `WebMvcConfigurer.extendMessageConverters` (dùng `extend...` để **không xóa** converter mặc định như `configure...`).
- Suffix pattern (`/users.json`) đã tắt từ Spring 5.3 (RFD attack).

**Câu hỏi nối tiếp:**
- *Có Jackson XML trên classpath thì sao?* → Có thể trả XML khi client gửi `Accept: application/xml` (hoặc trình duyệt) — kiểm soát bằng cách chỉ khai báo `produces`.

**⚠️ Câu trả lời gây điểm trừ:** Nhầm 406 với 415.

**📖 Ôn lại:** [§2.2 Các thành phần quan trọng](../01-giao-trinh/08-spring-web-rest-security.md#p2)

</details>

### Q7. 🟡 Bạn muốn controller nhận `@CurrentTenant Tenant tenant`. Viết thế nào, và lấy tenant từ đâu để an toàn?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Viết `HandlerMethodArgumentResolver` (`supportsParameter` kiểm tra annotation + kiểu; `resolveArgument` dựng `Tenant`) và đăng ký qua `WebMvcConfigurer.addArgumentResolvers`. An toàn: khi đã xác thực, tenant phải lấy từ **claim của token** (`tenant_id` trong JWT) — **không** để client tự chọn bằng header; header chỉ dùng cho endpoint public; validate tenant tồn tại (cache). Đây là biện pháp chống **BOLA** đa tenant.

**Giải thích chi tiết:**

```java
public Object resolveArgument(MethodParameter p, ModelAndViewContainer mav,
                              NativeWebRequest req, WebDataBinderFactory bf) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth instanceof JwtAuthenticationToken jwt) {
        return new Tenant(jwt.getToken().getClaimAsString("tenant_id"));   // nguồn sự thật
    }
    String id = req.getHeader("X-Tenant-Id");                               // chỉ cho endpoint public
    if (id == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing X-Tenant-Id");
    return new Tenant(id);
}
```

- Thêm lớp phòng thủ ở repository: Hibernate `@TenantId`, filter, hoặc PostgreSQL Row-Level Security.

**Câu hỏi nối tiếp:**
- *JWT tenant A nhưng header B?* → Dùng A, log cảnh báo; không bao giờ để header ghi đè token.

**⚠️ Câu trả lời gây điểm trừ:** Lấy tenant từ header/query cho mọi request.

**📖 Ôn lại:** [§2.2 Custom argument resolver & Bài 2.3](../01-giao-trinh/08-spring-web-rest-security.md#p2)

</details>

### Q8. 🟢 [Tình huống] Sau khi nâng lên Boot 3, mobile app gọi `GET /api/users/` nhận 404. Vì sao? Còn thay đổi nào về URL matching?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Spring 6 **bỏ trailing slash matching** mặc định: `/users/` không còn khớp `@GetMapping("/users")`. Sửa ở client (đúng nhất); tạm thời thêm filter redirect/rewrite bỏ dấu `/` cuối; `setUseTrailingSlashMatch(true)` đã deprecated. Thay đổi khác: MVC mặc định dùng `PathPatternParser` thay `AntPathMatcher` (nhanh hơn, cú pháp `{*path}`); suffix pattern matching đã tắt từ 5.3; Spring 6.1/Boot 3.2 ném `NoResourceFoundException` cho static resource không tìm thấy.

**Giải thích chi tiết:**
- Spring Security cũng match URL; trailing slash khác nhau giữa security matcher và MVC mapping từng gây lỗ hổng bypass — một lý do Spring đổi mặc định.
- Phát hiện trước khi deploy: contract test với client thật/consumer-driven contract, log 404 tăng đột biến sau deploy.

**Câu hỏi nối tiếp:**
- *Vì sao suffix matching bị tắt?* → Reflected File Download và mơ hồ content negotiation.

**⚠️ Câu trả lời gây điểm trừ:** Không biết thay đổi này.

**📖 Ôn lại:** [§2.2 Góc nhìn Senior](../01-giao-trinh/08-spring-web-rest-security.md#p2)

</details>

---

<a id="nhom-c"></a>
## C. Controller, data binding, validation

### Q9. 🟢 `@PathVariable`, `@RequestParam`, `@RequestBody` dùng khi nào? Lỗi binding trả status gì?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `@PathVariable` — phần của URI, **định danh resource** (`/orders/{id}`). `@RequestParam` — query string/form field cho **filter, paging, sort**. `@RequestBody` — deserialize body (JSON) bằng `HttpMessageConverter` cho dữ liệu tạo/sửa. Lỗi: kiểu không chuyển được (`?page=abc`) → `MethodArgumentTypeMismatchException` → **400**; JSON sai cú pháp → `HttpMessageNotReadableException` → **400**; `@Valid` fail → `MethodArgumentNotValidException` → **400**; thiếu param bắt buộc → `MissingServletRequestParameterException` → 400.

**Giải thích chi tiết:**

```java
@PostMapping
ResponseEntity<OrderDto> create(@Valid @RequestBody CreateOrderRequest req, UriComponentsBuilder uri) {
    OrderDto created = service.create(req);
    URI location = uri.path("/api/v1/orders/{id}").buildAndExpand(created.id()).toUri();
    return ResponseEntity.created(location).body(created);       // 201 + Location
}
```

- Spring 6.1 cần compile với `-parameters` nếu không ghi tên trong annotation (`@PathVariable("id")`).
- DTO request là `record` bất biến, không dùng entity.

**Câu hỏi nối tiếp:**
- *Dùng `int` hay `Integer` cho field optional?* → `Integer`/wrapper để phân biệt "không gửi" (`null`) với `0`.

**⚠️ Câu trả lời gây điểm trừ:** Dùng `@RequestParam` cho dữ liệu tạo mới phức tạp; nhận entity JPA làm `@RequestBody`.

**📖 Ôn lại:** [§3.1 Controller cơ bản](../01-giao-trinh/08-spring-web-rest-security.md#p3)

</details>

### Q10. 🟡 [Đọc code] Request `{"customerId": 1, "lines": [{"sku": "", "quantity": -5}], "note": "   "}` có bị từ chối không?

```java
record CreateOrderRequest(@NotNull Long customerId,
                          @NotEmpty List<OrderLine> lines,
                          @NotNull String note) {}
record OrderLine(@NotBlank String sku, @Positive int quantity) {}

@PostMapping ResponseEntity<?> create(@Valid @RequestBody CreateOrderRequest req) { ... }
```

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **Không** — request lọt qua. `@Valid` chỉ cascade vào object con khi phần tử được đánh dấu: thiếu `@Valid` trên `List<OrderLine>` (hoặc `List<@Valid OrderLine>`) nên `sku` rỗng và `quantity` âm không được kiểm tra. `note` = `"   "` thỏa `@NotNull` — muốn chặn chuỗi trắng phải dùng `@NotBlank`. Sửa: `@NotEmpty @Size(max = 50) List<@Valid OrderLine> lines`, `@Size(max = 500) String note` (và `@NotBlank` nếu bắt buộc).

**Giải thích chi tiết:**
- `@Valid` (Jakarta) kích hoạt validation và cascade; `@Validated` (Spring) hỗ trợ **validation groups**.
- Cần `spring-boot-starter-validation` (từ Boot 2.3 không còn nằm sẵn trong starter-web) — thiếu nó thì `@Valid` **im lặng không làm gì**.
- Luôn giới hạn kích thước collection/chuỗi (chống unrestricted resource consumption).

**Câu hỏi nối tiếp:**
- *Groups hay DTO riêng cho create/update?* → DTO riêng mỗi use case thường rõ và an toàn hơn trước mass assignment; groups làm DTO khó đọc.

**⚠️ Câu trả lời gây điểm trừ:** "Bị từ chối vì đã có `@Valid`."

**📖 Ôn lại:** [§3.2 Bean Validation & Lỗi thường gặp](../01-giao-trinh/08-spring-web-rest-security.md#p3)

</details>

### Q11. 🔴 `GET /orders/{id}` với `@PathVariable @Positive long id`, gọi `/orders/-1`. Boot 3.1 trả 500, Boot 3.2 trả 400. Vì sao?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Trước Spring 6.1, constraint trên tham số method (không phải `@RequestBody`) chỉ được kiểm tra khi class có **`@Validated`** → `MethodValidationPostProcessor` (AOP proxy) ném **`ConstraintViolationException`** — exception này không được MVC map mặc định → **500** nếu không tự xử lý (và nếu thiếu `@Validated` thì constraint bị bỏ qua hoàn toàn). Spring 6.1/Boot 3.2 có **built-in method validation** trong MVC: tham số controller có constraint được kiểm tra bởi chính `RequestMappingHandlerAdapter`, ném **`HandlerMethodValidationException`** → map sẵn **400** (và `ResponseEntityExceptionHandler` có handler cho nó).

**Giải thích chi tiết:**
- Với Boot cũ: thêm `@ExceptionHandler(ConstraintViolationException.class)` trả 400 + danh sách vi phạm.
- Nếu class vẫn có `@Validated` ở 6.1+, cần hiểu cả hai cơ chế có thể cùng tồn tại; Spring khuyến nghị bỏ AOP method validation cho controller để tránh validate hai lần.
- Validation ở service (`@Validated` trên service) vẫn dùng AOP → `ConstraintViolationException` → cần handler.

**Câu hỏi nối tiếp:**
- *Vì sao 500 cho lỗi input là vấn đề?* → Sai ngữ nghĩa, client retry vô ích, alert giả, có thể lộ message nội bộ.

**⚠️ Câu trả lời gây điểm trừ:** Không biết sự khác nhau giữa `MethodArgumentNotValidException` và `ConstraintViolationException`.

**📖 Ôn lại:** [§3.2 Bean Validation](../01-giao-trinh/08-spring-web-rest-security.md#p3)

</details>

### Q12. 🟡 Kiểm tra "email chưa tồn tại" bằng custom `ConstraintValidator` inject repository — có ổn không? Bạn phân lớp validation thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Inject bean vào `ConstraintValidator` được (Spring tạo validator qua `SpringConstraintValidatorFactory`), nhưng kiểm tra unique ở validator có **race condition**: hai request đồng thời cùng thấy "chưa tồn tại" rồi cùng insert. Nguồn sự thật phải là **unique constraint ở DB**, bắt `DataIntegrityViolationException` → 409. Phân 3 lớp: (1) **syntactic** ở DTO (Bean Validation: định dạng, độ dài, range, cross-field); (2) **business rule** ở service/domain (tồn kho đủ, trạng thái cho phép); (3) **integrity** ở DB (unique, FK, check constraint).

**Giải thích chi tiết:**
- Cross-field validator (khoảng ngày `from <= to`, tối đa 1 năm): annotation cấp class + `ConstraintValidator<ValidDateRange, ReportRequest>`, dùng `buildConstraintViolationWithTemplate(...).addPropertyNode("to")` để gắn lỗi vào field.
- Validator nên trả `true` khi giá trị `null` để `@NotNull` lo — tránh trùng thông báo.
- Kiểm tra trước (validator) vẫn có ích cho UX (thông báo sớm), nhưng không thay ràng buộc DB.

**Câu hỏi nối tiếp:**
- *Trả entity JPA trực tiếp ra API có sao không?* → Lazy loading exception, lộ field nội bộ, vòng lặp JSON, mass assignment khi nhận vào.

**⚠️ Câu trả lời gây điểm trừ:** "Validator check unique là đủ, không cần constraint DB."

**📖 Ôn lại:** [§3.2 Custom validator & Góc nhìn Senior](../01-giao-trinh/08-spring-web-rest-security.md#p3)

</details>

### Q13. 🔴 Thiết kế `PATCH /api/v1/users/{id}` cho phép sửa `displayName`, `avatarUrl`, phân biệt "không gửi" với "gửi `null` để xóa", và chặn sửa `role`.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Dùng **JSON Merge Patch** (RFC 7396, `Content-Type: application/merge-patch+json`): field vắng = giữ nguyên, field `null` = xóa, field có giá trị = gán. DTO record thường không phân biệt được vắng/null (cả hai thành `null`) → nhận `JsonNode`/`Map` và kiểm tra `has(...)`, hoặc dùng `JsonNullable<T>` (openapi-jackson-nullable). **Whitelist** field được phép (`displayName`, `avatarUrl`): field khác (như `role`) → 400 hoặc bỏ qua có log cảnh báo. Sau khi merge, **validate lại** object kết quả bằng `Validator`.

**Giải thích chi tiết:**

```java
private static final Set<String> PATCHABLE = Set.of("displayName", "avatarUrl");

@PatchMapping(path = "/{id}", consumes = "application/merge-patch+json")
ProfileDto patch(@PathVariable String id, @RequestBody JsonNode patch, @AuthenticationPrincipal Jwt jwt) {
    patch.fieldNames().forEachRemaining(f -> {
        if (!PATCHABLE.contains(f)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Field not patchable: " + f);
    });
    return profileService.patch(jwt.getSubject(), id, patch);   // kiểm tra id thuộc về user (BOLA)
}
// trong service: if (patch.has("avatarUrl")) profile.setAvatarUrl(patch.get("avatarUrl").isNull() ? null : patch.get("avatarUrl").asText());
```

- Merge Patch đặt giá trị tuyệt đối → **idempotent**; JSON Patch (RFC 6902) có `op: add/remove/...` linh hoạt hơn nhưng phức tạp và có thể không idempotent.
- Kết hợp `If-Match`/ETag để tránh lost update.

**Câu hỏi nối tiếp:**
- *Vì sao không bind thẳng vào entity `User`?* → Mass assignment: client gửi `{"role":"ADMIN","balance":999}` (OWASP API3).

**⚠️ Câu trả lời gây điểm trừ:** Dùng PUT với DTO có mọi field, ghi đè null vào field client không gửi.

**📖 Ôn lại:** [§3 Bài 3.3 Patch an toàn](../01-giao-trinh/08-spring-web-rest-security.md#p3)

</details>

---

<a id="nhom-d"></a>
## D. Exception handling & `ProblemDetail`

### Q14. 🟢 Spring MVC xử lý exception qua những tầng nào? `@ExceptionHandler` trong controller và trong `@RestControllerAdvice` cái nào ưu tiên?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `HandlerExceptionResolver` theo thứ tự: **`ExceptionHandlerExceptionResolver`** (`@ExceptionHandler` trong chính controller **trước**, rồi trong `@ControllerAdvice`) → `ResponseStatusExceptionResolver` (`@ResponseStatus`, `ResponseStatusException`) → `DefaultHandlerExceptionResolver` (exception chuẩn của Spring → 4xx). Không resolver nào xử lý → exception lên container → Boot `BasicErrorController` (`/error`).

**Giải thích chi tiết:**
- Nhiều advice: sắp bằng `@Order`; giới hạn phạm vi bằng `basePackages`/`assignableTypes`.
- Handler chọn exception khớp **gần nhất** trong hierarchy.
- Thực tế: một `@RestControllerAdvice` toàn cục extends `ResponseEntityExceptionHandler` để có sẵn mapping chuẩn trả `ProblemDetail`.

**Câu hỏi nối tiếp:**
- *Exception ném trong filter có vào advice không?* → Không (Q16).

**⚠️ Câu trả lời gây điểm trừ:** try/catch trong từng controller method để tự trả lỗi.

**📖 Ôn lại:** [§4.1 Cơ chế](../01-giao-trinh/08-spring-web-rest-security.md#p4)

</details>

### Q15. 🟡 Thiết kế format lỗi thống nhất cho nhiều service. `ProblemDetail` (RFC 9457) gồm gì và bạn dùng nó thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** RFC 9457 (thay RFC 7807), `Content-Type: application/problem+json`, field: `type` (URI định danh **loại lỗi**, ổn định để client switch logic), `title`, `status`, `detail` (cho người đọc), `instance`, cộng **extension properties** (`errors`, `code`, `traceId`...). Spring 6: `ProblemDetail`, `ErrorResponse` (mọi exception MVC chuẩn đã implement), bật cho Boot bằng `spring.mvc.problemdetails.enabled=true` hoặc extends `ResponseEntityExceptionHandler`. Thiết kế: **error catalog** (`ErrorCode` enum: code, status, type URI, title) + base `BusinessException(ErrorCode, args)` + một handler duy nhất; message i18n qua `MessageSource`.

**Giải thích chi tiết:**

```java
@ExceptionHandler(InsufficientStockException.class)
ProblemDetail handleBusiness(InsufficientStockException ex) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    pd.setType(URI.create("https://api.example.com/problems/insufficient-stock"));
    pd.setProperty("sku", ex.sku());
    return pd;
}
```

- Lỗi validation: override `handleMethodArgumentNotValid` thêm `errors: [{field, message}]`.
- Catch-all `Exception` → 500 với `errorId` để tra log; **không** trả `ex.getMessage()` của exception hạ tầng (lộ cấu trúc DB); `server.error.include-stacktrace=never` (mặc định).

**Câu hỏi nối tiếp:**
- *Client nên dựa vào field nào?* → `type`/`code`, không parse `detail`.

**⚠️ Câu trả lời gây điểm trừ:** Trả `200` kèm `{"success": false}`; mỗi service một format lỗi.

**📖 Ôn lại:** [§4.2 ProblemDetail](../01-giao-trinh/08-spring-web-rest-security.md#p4)

</details>

### Q16. 🔴 Có `@ExceptionHandler(Exception.class)` trả 500. Người dùng thiếu quyền gọi method có `@PreAuthorize` nhận 500 thay vì 403; token sai thì nhận HTML của Tomcat thay vì `ProblemDetail`. Giải thích và sửa.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** (1) `AccessDeniedException` ném từ method security (trong controller/service — **bên trong** DispatcherServlet) bị `@ExceptionHandler(Exception.class)` **bắt trước** khi tới `ExceptionTranslationFilter` của Spring Security → 500. Sửa: xử lý riêng `AccessDeniedException`/`AuthenticationException` (rethrow, hoặc map 403/401 tường minh). (2) Lỗi JWT xảy ra **trong filter** (`BearerTokenAuthenticationFilter`), chưa tới DispatcherServlet nên không đi qua `@ControllerAdvice` → phải xử lý bằng **`AuthenticationEntryPoint`** (401) và **`AccessDeniedHandler`** (403) tự ghi `ProblemDetail`; filter tự viết (rate limit) tự ghi response 429.

**Giải thích chi tiết:**

```java
@ExceptionHandler(AccessDeniedException.class)
void rethrow(AccessDeniedException ex) { throw ex; }    // để ExceptionTranslationFilter xử lý

http.exceptionHandling(e -> e
    .authenticationEntryPoint(new ProblemDetailAuthEntryPoint())     // 401 + WWW-Authenticate
    .accessDeniedHandler(new ProblemDetailAccessDeniedHandler()));   // 403
```

- Cách khác: inject `HandlerExceptionResolver` (bean `handlerExceptionResolver`) vào filter rồi `resolver.resolveException(req, res, null, ex)` để "chuyển" lỗi filter sang advice.
- 404 không có handler: Spring 6.1+ ném `NoResourceFoundException` → `ResponseEntityExceptionHandler` xử lý được.
- Test: `MockMvc` cho 5 trường hợp (401, 403, 404, 429, lỗi controller) đều `application/problem+json`.

**Câu hỏi nối tiếp:**
- *`ExceptionTranslationFilter` quyết định 401 hay 403 thế nào?* → `AuthenticationException` → entry point (401); `AccessDeniedException` → nếu user anonymous coi như chưa đăng nhập (401), ngược lại `AccessDeniedHandler` (403).

**⚠️ Câu trả lời gây điểm trừ:** Không biết filter nằm ngoài phạm vi `@ControllerAdvice`.

**📖 Ôn lại:** [§4.2 Góc nhìn Senior & Bài 4.3](../01-giao-trinh/08-spring-web-rest-security.md#p4)

</details>

---

<a id="nhom-e"></a>
## E. Thiết kế REST: method, status code, pagination

### Q17. 🟢 Safe và idempotent khác nhau thế nào? PUT, PATCH, POST thuộc loại nào và vì sao điều này quan trọng?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **Safe**: không thay đổi trạng thái server (GET, HEAD, OPTIONS). **Idempotent**: gọi N lần có **hiệu ứng phía server** giống gọi 1 lần (response có thể khác — DELETE lần 2 trả 404): GET, PUT, DELETE (và các method safe). **POST** không idempotent; **PATCH** không bảo đảm (Merge Patch đặt giá trị tuyệt đối thì idempotent, `{"op":"increment"}` thì không). Quan trọng vì client, proxy, gateway, service mesh **retry** khi timeout — retry POST có thể tạo đơn/trừ tiền hai lần → cần **`Idempotency-Key`**.

**Giải thích chi tiết:**
- PUT = **thay thế toàn bộ** resource tại URI mà client biết; POST = tạo/xử lý, server quyết định URI (trả 201 + `Location`).
- GET không bao giờ được có side effect (CSRF protection không bảo vệ GET, crawler/prefetch gọi GET).

**Câu hỏi nối tiếp:**
- *DELETE trả 204 hay 404 lần thứ hai?* → Cả hai chấp nhận được; quan trọng là trạng thái server không đổi — chọn và nhất quán.

**⚠️ Câu trả lời gây điểm trừ:** "Idempotent nghĩa là response giống nhau"; "PATCH idempotent".

**📖 Ôn lại:** [§5.2 HTTP methods — safe & idempotent](../01-giao-trinh/08-spring-web-rest-security.md#p5)

</details>

### Q18. 🟢 Phân biệt 401 vs 403, 400 vs 422, 409 vs 412. Khi nào trả 404 thay vì 403?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **401** = chưa xác thực/token không hợp lệ (kèm `WWW-Authenticate`); **403** = đã xác thực nhưng **không có quyền**. **400** = sai cú pháp/validation; **422** = cú pháp đúng nhưng vi phạm ngữ nghĩa nghiệp vụ (nhiều team dùng 400 cho mọi lỗi validation — quan trọng là thống nhất). **409** = xung đột trạng thái (trùng unique, trạng thái không cho phép); **412** = `If-Match` không khớp ETag (optimistic concurrency), **428** = bắt buộc gửi `If-Match`. Trả **404 thay 403** khi resource thuộc người khác và bạn không muốn lộ sự tồn tại (chống enumeration — BOLA).

**Giải thích chi tiết:**
- Khác: 201 + `Location`, 202 (xử lý async + link trạng thái), 204, 304, 405/406/415, 429 + `Retry-After`, 502/503/504.
- Anti-pattern: 200 + `{"error": ...}`; 500 cho lỗi validation; stack trace trong response.

**Câu hỏi nối tiếp:**
- *Rate limit trả gì?* → 429 + `Retry-After` (và tùy chọn `RateLimit-*` headers).

**⚠️ Câu trả lời gây điểm trừ:** Dùng 401 cho thiếu quyền; dùng 500 cho mọi lỗi.

**📖 Ôn lại:** [§5.3 Status code nên dùng](../01-giao-trinh/08-spring-web-rest-security.md#p5)

</details>

### Q19. 🟡 [Tình huống] Review và sửa: `GET /getAllUsers`, `POST /user/delete?id=5`, `POST /updateUserEmail`, `GET /orders/create?productId=1`, `POST /users/5/orders/7/items/3/update`.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** URI định danh **resource** (danh từ số nhiều), hành động là **HTTP method**: `GET /users` (200, có phân trang + giới hạn size); `DELETE /users/5` (204/404); `PATCH /users/{id}` body `{"email": ...}` (200/400/409 nếu trùng); `POST /orders` body `{"productId": 1}` (201 + `Location`) — **GET không được tạo dữ liệu**; `PATCH /orders/7/items/3` (lồng tối đa 1–2 cấp; user đã suy ra từ token) — hoặc `/order-items/3` nếu item có ID toàn cục.

**Giải thích chi tiết:**
- Hành động không thuần CRUD: sub-resource (`POST /orders/42/cancellation`, `POST /payments/{id}/refunds`) hoặc custom method kiểu Google AIP (`POST /orders/42:cancel`) — miễn team thống nhất.
- Nhất quán: kebab-case cho path, một kiểu cho JSON field, thời gian ISO-8601 UTC, tiền dùng số nguyên đơn vị nhỏ nhất hoặc string decimal + currency.
- `GET /orders/create?...` còn là lỗ hổng CSRF (GET có side effect, link/ảnh trên site khác kích hoạt được).

**Câu hỏi nối tiếp:**
- *Đổi email cần xác minh?* → Mô hình hóa thành resource: `POST /users/me/email-change-requests` → 202, xác nhận qua link.

**⚠️ Câu trả lời gây điểm trừ:** Chỉ đổi tên path mà giữ nguyên method sai.

**📖 Ôn lại:** [§5.1 Resource & URI, Bài 5.1](../01-giao-trinh/08-spring-web-rest-security.md#p5)

</details>

### Q20. 🔴 Offset pagination và keyset (cursor) pagination khác nhau thế nào? Viết query keyset cho danh sách sự kiện mới nhất.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **Offset** (`?page=3&size=20` → `LIMIT 20 OFFSET 60`): dễ, nhảy trang tùy ý, có tổng số trang; nhưng `OFFSET 100000` buộc DB đọc rồi bỏ 100k dòng → **chậm dần**, `COUNT(*)` tốn kém trên bảng lớn, và dữ liệu chèn/xóa giữa các lần gọi gây **trùng/sót**. **Keyset** (`?limit=20&after=<cursor>`): `WHERE (created_at, id) < (:lastCreatedAt, :lastId) ORDER BY created_at DESC, id DESC LIMIT 21` — dùng index, hiệu năng ổn định mọi độ sâu, không trùng/sót; nhược: không nhảy trang tùy ý, cần sort key **unique** (thêm `id` làm tie-breaker), cursor **opaque**.

**Giải thích chi tiết:**

```java
@GetMapping("/api/v1/events")
CursorPage<EventDto> events(@RequestParam(defaultValue = "50") @Max(200) int limit,
                            @RequestParam(required = false) String after) {
    Cursor c = after == null ? Cursor.START : Cursor.decode(after);   // base64(JSON), ký HMAC nếu cần chống sửa
    List<EventDto> rows = repo.findPage(c.createdAt(), c.id(), limit + 1);   // lấy dư 1 để biết còn trang
    String next = rows.size() > limit ? Cursor.of(rows.get(limit - 1)).encode() : null;
    return new CursorPage<>(rows.subList(0, Math.min(limit, rows.size())), next);
}
```

- Index cần: `(created_at DESC, id DESC)`; kiểm chứng bằng `EXPLAIN ANALYZE`.
- Row-value comparison `(a, b) < (x, y)` có trên PostgreSQL/MySQL; DB khác viết `a < x OR (a = x AND b < y)`.
- Spring Data 3.1+ có `ScrollPosition`/`Window<T>` hỗ trợ keyset.

**Câu hỏi nối tiếp:**
- *UI cần "trang 1..N"?* → Offset cho vài trang đầu + giới hạn độ sâu; tổng số ước lượng (statistics) thay `COUNT(*)` chính xác.

**⚠️ Câu trả lời gây điểm trừ:** Không biết vấn đề hiệu năng của OFFSET lớn; cursor là ID tăng dần lộ ra ngoài.

**📖 Ôn lại:** [§5.4 Pagination](../01-giao-trinh/08-spring-web-rest-security.md#p5)

</details>

### Q21. 🟡 API tìm kiếm cho phép `?sort=<field>,asc` tự do và trả thẳng `Page<OrderDto>` của Spring Data. Có vấn đề gì?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Sort/filter tự do: (1) **lộ field ẩn** (`sort=passwordHash` dùng để suy ra dữ liệu bằng thứ tự); (2) sort trên cột không index → full scan → **DoS**; (3) nếu ghép chuỗi vào SQL → injection. Phải **whitelist** field được sort/filter, giới hạn `size` tối đa. Trả thẳng `Page<T>`: cấu trúc JSON phụ thuộc implementation `PageImpl` (không phải hợp đồng ổn định) — Spring Data 3.3 cảnh báo và khuyến nghị `@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)` hoặc tự định nghĩa DTO trang.

**Giải thích chi tiết:**

```java
private static final Set<String> SORTABLE = Set.of("createdAt", "total");
for (Sort.Order o : pageable.getSort()) {
    if (!SORTABLE.contains(o.getProperty())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsortable: " + o.getProperty());
}
```

- Filter động: JPA `Specification`/Criteria hoặc QueryDSL/jOOQ — không ghép chuỗi.
- Giới hạn `size`: `spring.data.web.pageable.max-page-size` (mặc định 2000 — nên hạ xuống).

**Câu hỏi nối tiếp:**
- *Test injection thế nào?* → Gửi payload SQL vào filter và assert 400/kết quả rỗng, kiểm tra log query dùng bind parameter.

**⚠️ Câu trả lời gây điểm trừ:** Không thấy rủi ro bảo mật/hiệu năng của sort tự do.

**📖 Ôn lại:** [§5.4 Filter & sort, Lỗi thường gặp](../01-giao-trinh/08-spring-web-rest-security.md#p5)

</details>

---

<a id="nhom-f"></a>
## F. REST nâng cao: versioning, idempotency key, ETag, OpenAPI

### Q22. 🟡 Các chiến lược versioning REST API? Bạn chọn cái nào và quan trọng hơn là gì?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** URI path (`/api/v1/orders` — rõ, dễ route ở gateway, dễ cache, phổ biến nhất), query param (dễ quên, cache key phức tạp), header tùy chỉnh (`X-API-Version` — URI sạch nhưng khó test, cần `Vary`), media type (`application/vnd.acme.order.v2+json` — đúng content negotiation nhưng phức tạp cho client). Tôi thường chọn URI path. Quan trọng hơn: **tránh breaking change** — chỉ thêm field optional, client là *tolerant reader*, không đổi nghĩa field cũ; khi buộc phải breaking thì chạy song song v1/v2, có lộ trình deprecation (header `Deprecation`, `Sunset`), **đo** ai còn gọi v1 trước khi tắt.

**Giải thích chi tiết:**
- Boot 3: versioning bằng URI path hoặc custom `RequestCondition`; Spring Framework 7 (Boot 4) bổ sung hỗ trợ API versioning tích hợp.
- Consumer-driven contract test (Pact, Spring Cloud Contract) và diff OpenAPI trong CI (oasdiff, openapi-diff) để phát hiện breaking change.

**Câu hỏi nối tiếp:**
- *Thêm giá trị enum mới có breaking không?* → Có thể, nếu client parse cứng; tài liệu hóa client phải chịu được giá trị lạ.

**⚠️ Câu trả lời gây điểm trừ:** Tạo version mới cho mọi thay đổi; không có kế hoạch deprecation.

**📖 Ôn lại:** [§6.1 Versioning](../01-giao-trinh/08-spring-web-rest-security.md#p6)

</details>

### Q23. 🔴 Thiết kế `Idempotency-Key` cho `POST /payments`: xử lý request đồng thời cùng key, cùng key khác payload, và server crash giữa chừng.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Client sinh key (UUID) cho mỗi *thao tác nghiệp vụ*, retry gửi **cùng** key. Server: key **gắn với user** (`sub:key`), lưu bảng `idempotency(key PK, request_hash, status, response_status, response_body, created_at)`. (1) `INSERT ... status='IN_PROGRESS'` — **unique constraint** là khóa; insert thắng → thực thi nghiệp vụ → lưu response, `COMPLETED` (lý tưởng cùng transaction với nghiệp vụ). (2) Insert thua (key đã có): `COMPLETED` + hash khớp → **trả lại response đã lưu**; hash khác → **422/409** (cùng key khác payload); `IN_PROGRESS` → **409 + `Retry-After`**. (3) Record kẹt `IN_PROGRESS` quá N phút (crash) → coi hết hạn cho phép chạy lại — rủi ro thực thi hai lần nếu request gốc thật ra vẫn chạy (GC pause dài) → bảo vệ cuối bằng **unique constraint nghiệp vụ** (`payment.order_id`). TTL dọn key 24h–7 ngày.

**Giải thích chi tiết:**

```java
@PostMapping("/api/v1/payments")
ResponseEntity<PaymentDto> pay(@RequestHeader("Idempotency-Key") @Size(min = 16, max = 64) String key,
                               @Valid @RequestBody PaymentRequest req, @AuthenticationPrincipal Jwt jwt) {
    String scopedKey = jwt.getSubject() + ":" + key;
    return idempotencyService.execute(scopedKey, req.fingerprint(), () -> paymentService.pay(req));
}
```

- PostgreSQL: `INSERT ... ON CONFLICT (key) DO NOTHING RETURNING key` để biết ai thắng; `request_hash = SHA-256(canonical JSON)`. Redis: `SET key value NX EX 86400`.
- Nếu nghiệp vụ gọi cổng thanh toán ngoài: truyền idempotency key xuống cổng (Stripe hỗ trợ), vì không thể gói lời gọi ngoài trong transaction DB.
- Idempotency key chỉ chống retry của client; consumer Kafka cần cơ chế riêng (bảng processed_message).

**Câu hỏi nối tiếp:**
- *Test thế nào?* → 20 thread cùng key → nghiệp vụ chạy đúng 1 lần; còn lại nhận 409 hoặc response đã lưu.

**⚠️ Câu trả lời gây điểm trừ:** "Kiểm tra key tồn tại rồi insert" (check-then-act, race); không xử lý hash khác.

**📖 Ôn lại:** [§6.2 Idempotency key & Bài 6.3](../01-giao-trinh/08-spring-web-rest-security.md#p6)

</details>

### Q24. 🟡 ETag dùng cho những mục đích gì? `ShallowEtagHeaderFilter` có đủ không?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Hai mục đích: (1) **caching** — client gửi `If-None-Match: "v7"`, không đổi thì **304** không body, tiết kiệm băng thông; (2) **optimistic concurrency** chống lost update — client gửi `PUT`/`PATCH` kèm `If-Match: "v7"`, resource đã thành v8 → **412**; thiếu `If-Match` khi bắt buộc → **428**. `ShallowEtagHeaderFilter` tính MD5 trên body response: tiết kiệm băng thông nhưng **không** tiết kiệm CPU/DB (controller vẫn chạy) và không hỗ trợ `If-Match` cho cập nhật. ETag "deep" dựa trên `@Version` của JPA tốt hơn.

**Giải thích chi tiết:**

```java
@GetMapping("/api/v1/products/{id}")
ResponseEntity<ProductDto> get(@PathVariable long id, WebRequest request) {
    Product p = repo.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    String etag = "\"" + p.getVersion() + "\"";
    if (request.checkNotModified(etag)) return null;              // Spring tự trả 304
    return ResponseEntity.ok().eTag(etag).body(ProductDto.from(p));
}
```

- Cập nhật: service so `expectedVersion` với version hiện tại → 412; `@Version` bắt race giữa đọc và ghi (`ObjectOptimisticLockingFailureException` → cũng map 412).
- `Cache-Control` (`private`, `max-age`) đi kèm để client/proxy cache đúng.

**Câu hỏi nối tiếp:**
- *Weak ETag `W/"..."`?* → Tương đương ngữ nghĩa, không byte-by-byte; không dùng cho `If-Match` mạnh (so sánh strong).

**⚠️ Câu trả lời gây điểm trừ:** Chỉ biết ETag cho cache, không biết dùng chống lost update.

**📖 Ôn lại:** [§6.3 ETag & conditional requests](../01-giao-trinh/08-spring-web-rest-security.md#p6)

</details>

### Q25. 🟢 HATEOAS là gì? Code-first hay API-first với OpenAPI?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** HATEOAS là mức cao nhất của Richardson Maturity Model: response chứa **link** tới các hành động hợp lệ tiếp theo (link `cancel` chỉ có khi đơn còn hủy được) — client "khám phá" API; Spring HATEOAS hỗ trợ HAL. Ít API public dùng triệt để vì tăng payload và độ phức tạp. OpenAPI: **code-first** (springdoc sinh `/v3/api-docs` từ annotation — nhanh cho team nhỏ) vs **API-first/contract-first** (viết `openapi.yaml` trước, review hợp đồng, sinh interface bằng openapi-generator — tốt cho nhiều team/consumer). Production: tắt hoặc bảo vệ Swagger UI.

**Giải thích chi tiết:**
- springdoc 2.x cho Boot 3 (1.x cho Boot 2 — không lẫn); `@Operation`, `@Schema`, `@SecurityScheme` cho JWT.
- Để lộ Swagger UI/API nội bộ ra Internet là "Improper Inventory Management" (OWASP API9).

**Câu hỏi nối tiếp:**
- *Phát hiện breaking change?* → Diff spec trong CI (oasdiff).

**⚠️ Câu trả lời gây điểm trừ:** Không biết Richardson model hoặc để Swagger UI public trên production.

**📖 Ôn lại:** [§6.4 HATEOAS, §6.5 OpenAPI](../01-giao-trinh/08-spring-web-rest-security.md#p6)

</details>

---

<a id="nhom-g"></a>
## G. HTTP clients, timeout, connection pool

### Q26. 🟢 `RestTemplate`, `WebClient`, `RestClient`, HTTP interface — chọn cái nào cho app Spring MVC mới?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `RestTemplate`: đồng bộ, **maintenance mode** từ Spring 5, có lộ trình deprecate — chỉ cho code cũ. `WebClient`: non-blocking, reactive — cho WebFlux, gọi song song/streaming; trong MVC dùng được nhưng kéo theo Reactor. **`RestClient`** (Spring 6.1/Boot 3.2): đồng bộ, fluent API như WebClient — **lựa chọn mặc định cho app MVC**, đặc biệt với virtual threads. **HTTP interface** (`@HttpExchange`): khai báo interface, proxy sinh code (giống Feign), dựa trên RestClient — client gọn, dễ mock.

**Giải thích chi tiết:**

```java
@HttpExchange("/api/v1/stock")
interface InventoryClient {
    @GetExchange("/{sku}") StockDto getStock(@PathVariable String sku);
}
@Bean InventoryClient inventoryClient(RestClient inventoryRestClient) {
    return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(inventoryRestClient))
            .build().createClient(InventoryClient.class);
}
```

- Luôn tạo từ `RestClient.Builder` **do Boot inject** (có Jackson, metrics `http.client.requests`, trace propagation); `RestClient.create()` tự tạo mất observability.
- Migrate dần: `RestClient.create(restTemplate)` tái dùng cấu hình cũ.

**Câu hỏi nối tiếp:**
- *Test client?* → `MockRestServiceServer.bindTo(builder)` hoặc WireMock.

**⚠️ Câu trả lời gây điểm trừ:** "Dùng WebClient rồi `.block()` khắp nơi trong MVC."

**📖 Ôn lại:** [§7.1 Toàn cảnh HTTP clients](../01-giao-trinh/08-spring-web-rest-security.md#p7)

</details>

### Q27. 🔴 Có những loại timeout nào khi gọi HTTP? `new RestTemplate()` mặc định nguy hiểm thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** (1) **Connect timeout** — thiết lập TCP (+TLS), nên ngắn 1–3s; (2) **read/response (socket) timeout** — chờ dữ liệu/response, theo p99 của upstream + biên; (3) **connection request timeout (pool acquire)** — chờ mượn connection khi pool cạn; thiếu cái này thread treo vô hạn khi pool đầy; (4) **deadline tổng** kể cả retry (Resilience4j `TimeLimiter` hoặc propagate deadline từ request gốc). `new RestTemplate()` dùng `SimpleClientHttpRequestFactory` (JDK `HttpURLConnection`) với timeout **vô hạn** → một upstream treo giữ thread Tomcat mãi → cạn 200 thread → toàn service chết, kể cả endpoint không liên quan → **cascading failure**.

**Giải thích chi tiết:**

```java
CloseableHttpClient http = HttpClients.custom()
    .setConnectionManager(PoolingHttpClientConnectionManagerBuilder.create()
        .setMaxConnTotal(200).setMaxConnPerRoute(50)
        .setDefaultConnectionConfig(ConnectionConfig.custom()
            .setConnectTimeout(Timeout.ofSeconds(2)).setSocketTimeout(Timeout.ofSeconds(5))
            .setTimeToLive(TimeValue.ofMinutes(5)).build())
        .build())
    .setDefaultRequestConfig(RequestConfig.custom()
        .setConnectionRequestTimeout(Timeout.ofMillis(500))   // chờ mượn connection
        .setResponseTimeout(Timeout.ofSeconds(5)).build())
    .evictIdleConnections(TimeValue.ofSeconds(30))
    .build();
```

- Reactor Netty (WebClient): không có response timeout mặc định — đặt `responseTimeout`; pending acquire timeout mặc định 45s.
- Boot 3.4+ có property `spring.http.client.*` và `ClientHttpRequestFactorySettings`; API đổi giữa 3.2 → 3.4, đối chiếu đúng phiên bản.

**Câu hỏi nối tiếp:**
- *Timeout bao nhiêu là hợp lý?* → Dựa trên SLO và p99 thực đo của upstream; timeout của bạn phải nhỏ hơn timeout của caller phía trên (deadline giảm dần theo chuỗi gọi).

**⚠️ Câu trả lời gây điểm trừ:** Chỉ biết một loại timeout; không biết mặc định là vô hạn.

**📖 Ôn lại:** [§7.2 Timeout & connection pool](../01-giao-trinh/08-spring-web-rest-security.md#p7)

</details>

### Q28. 🔴 [Tình huống] Service gọi payment API: CPU thấp, upstream báo latency 100ms nhưng p99 phía bạn là 2s dưới tải 300 req/s. Bạn nghi gì và tính toán thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Nghi **connection pool quá nhỏ** — dấu hiệu kinh điển "latency cao, CPU thấp": request xếp hàng chờ mượn connection. Apache HttpClient 5 mặc định `maxTotal=25`, **`defaultMaxPerRoute=5`** → tối đa 5 request đồng thời tới một host. Little's Law: concurrency cần = throughput × latency = 300 × 0,1 = **30 connection** tới route đó (cộng biên cho dao động/p99, ví dụ 40–50). Kiểm chứng bằng metric thời gian chờ mượn connection/pool pending; cấu hình lại `maxConnPerRoute`, `maxConnTotal`, và `connectionRequestTimeout` để fail nhanh thay vì xếp hàng vô hạn.

**Giải thích chi tiết:**
- Với 5 connection, throughput tối đa ≈ 5 / 0,1s = 50 req/s; 300 req/s → hàng đợi tăng dần.
- Reactor Netty mặc định max connections = `max(số CPU × 2, 16)`.
- Đừng tăng pool vô hạn: upstream cũng có giới hạn; phối hợp rate limit/bulkhead và hỏi capacity của đối tác.

**Câu hỏi nối tiếp:**
- *Connection idle bị LB cắt?* → Xem Q30.

**⚠️ Câu trả lời gây điểm trừ:** "Upstream chậm, báo đối tác" mà không kiểm tra pool phía mình.

**📖 Ôn lại:** [§7.2 Mặc định nguy hiểm & Bài 7.3](../01-giao-trinh/08-spring-web-rest-security.md#p7)

</details>

### Q29. 🟡 Chính sách retry khi gọi service khác: retry cái gì, bao nhiêu lần, và tránh "retry storm" thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Chỉ retry thao tác **idempotent** (GET, PUT, DELETE) hoặc có **idempotency key**; chỉ retry lỗi tạm thời (connect timeout, 502/503/504, 429 theo `Retry-After`), **không** retry 4xx nghiệp vụ. Dùng **exponential backoff + jitter**, số lần nhỏ (2–3), nằm trong **deadline tổng**. Tránh retry storm: retry ở **một tầng** (không client + gateway + mesh cùng retry — tải nhân lên), **retry budget** (giới hạn % request được retry), kết hợp **circuit breaker** (Resilience4j) để ngừng gọi upstream đang chết.

**Giải thích chi tiết:**
- 3 tầng mỗi tầng retry 3 lần → một request gốc thành 27 lời gọi vào upstream đang quá tải.
- Read timeout trên POST không idempotent: không biết server đã xử lý chưa → **không** retry mù; dùng idempotency key hoặc truy vấn trạng thái.

**Câu hỏi nối tiếp:**
- *Fallback khi circuit mở?* → Giá trị cache/mặc định, degrade tính năng, hoặc 503 + `Retry-After` — tùy nghiệp vụ.

**⚠️ Câu trả lời gây điểm trừ:** "Retry 5 lần mỗi 1 giây cho mọi lỗi."

**📖 Ôn lại:** [§7.2 Góc nhìn Senior](../01-giao-trinh/08-spring-web-rest-security.md#p7)

</details>

### Q30. 🟡 [Tình huống] Lỗi `Connection reset` / `NoHttpResponseException` xuất hiện lẻ tẻ khi gọi service nội bộ qua load balancer, chủ yếu sau những lúc ít traffic. Nguyên nhân và cách sửa?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Connection idle trong pool bị **LB/NAT cắt ngầm** sau idle timeout của hạ tầng (ví dụ AWS NLB 350s), client không biết và tái sử dụng connection "chết" → reset. Sửa: **evict idle connections** với thời gian nhỏ hơn idle timeout của hạ tầng, đặt **TTL** connection (cũng giúp nhận IP mới khi DNS đổi), bật validate-after-inactivity; retry an toàn một lần cho request idempotent khi gặp lỗi này.

**Giải thích chi tiết:**
- Apache HttpClient 5: `evictIdleConnections(TimeValue.ofSeconds(30))`, `setTimeToLive(...)`, `setValidateAfterInactivity(...)`.
- Reactor Netty: `maxIdleTime`, `maxLifeTime` trên `ConnectionProvider`.
- Kèm theo: không log toàn bộ request/response chứa token/PII từ client interceptor.

**Câu hỏi nối tiếp:**
- *Vì sao TTL giúp với DNS?* → Connection sống mãi giữ IP cũ sau khi service đổi IP/blue-green; TTL buộc mở lại và resolve lại.

**⚠️ Câu trả lời gây điểm trừ:** Tăng timeout hoặc tắt keep-alive hoàn toàn (mất hiệu năng).

**📖 Ôn lại:** [§7.2 Góc nhìn Senior](../01-giao-trinh/08-spring-web-rest-security.md#p7)

</details>

---

<a id="nhom-h"></a>
## H. WebFlux vs MVC + virtual threads; CORS

### Q31. 🟡 `Mono`/`Flux` là gì? Điều gì xảy ra nếu gọi JDBC hoặc `.block()` trong handler WebFlux?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Kiểu của Project Reactor: `Mono<T>` (0..1 phần tử), `Flux<T>` (0..N); **lazy** — không có gì chạy cho tới khi subscribe; hỗ trợ **backpressure** (`request(n)`). WebFlux chạy trên **event loop** với số thread ≈ số core; gọi code **blocking** (JDBC, `RestTemplate`, `Thread.sleep`, `.block()`) trên event loop chặn thread đó → hàng trăm request khác bị treo theo. Nếu buộc phải gọi blocking: `Mono.fromCallable(...).subscribeOn(Schedulers.boundedElastic())`; phát hiện bằng **BlockHound** trong test.

**Giải thích chi tiết:**

```java
Mono<Price> price = client.get().uri("/price/{sku}", sku).retrieve().bodyToMono(Price.class)
        .timeout(Duration.ofMillis(800));
Mono<Stock> stock = client.get().uri("/stock/{sku}", sku).retrieve().bodyToMono(Stock.class)
        .timeout(Duration.ofMillis(800)).onErrorReturn(Stock.unknown(sku));      // degrade
return Mono.zip(price, stock).map(t -> new Quote(t.getT1(), t.getT2()));        // song song, latency ≈ max
```

- Toàn chuỗi phải non-blocking: R2DBC, WebClient, driver reactive.
- `ThreadLocal` (MDC, `SecurityContextHolder`) không dùng tự nhiên — dùng Reactor `Context`, `ReactiveSecurityContextHolder`.
- Debug: stack trace không phản ánh luồng logic → `checkpoint()`, `Hooks.onOperatorDebug()` (chỉ dev).

**Câu hỏi nối tiếp:**
- *Viết `Mono` mà quên return/subscribe?* → Không có gì xảy ra (ví dụ save không chạy) — bug kinh điển.

**⚠️ Câu trả lời gây điểm trừ:** "WebFlux tự chạy song song nên gọi JDBC cũng được."

**📖 Ôn lại:** [§8.1 Reactive cơ bản](../01-giao-trinh/08-spring-web-rest-security.md#p8)

</details>

### Q32. 🔴 Dự án mới: dùng WebFlux hay Spring MVC + virtual threads? Lập luận của bạn.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Với app CRUD/tích hợp thông thường dùng JDBC/JPA, tôi chọn **MVC + virtual threads** (Boot 3.2+, Java 21): code imperative dễ đọc, dễ debug, dùng được toàn bộ hệ sinh thái blocking, mà vẫn scale tốt khi nhiều request chờ I/O. Chọn **WebFlux** khi: cần **streaming/backpressure** thật, hàng chục nghìn **kết nối dài** (SSE/WebSocket), gateway/proxy hiệu năng cao (Spring Cloud Gateway bản gốc), hoặc cả stack đã reactive (R2DBC, driver reactive). Cả hai đều **không vượt được giới hạn tài nguyên hạ nguồn** (DB pool).

**Giải thích chi tiết:**

| Tiêu chí | MVC + virtual threads | WebFlux |
|---|---|---|
| Mô hình lập trình | imperative, dễ debug | reactive, learning curve cao |
| Nhiều request chờ I/O | tốt | tốt |
| Streaming, backpressure | hạn chế | mạnh |
| Hệ sinh thái | JDBC/JPA và mọi thư viện blocking | cần driver reactive; R2DBC chưa bằng JPA |

- Virtual thread có bẫy pinning (`synchronized` + blocking trên JDK 21–23) và ThreadLocal nặng (Module 07).
- Không trộn tùy tiện: app MVC dùng `WebClient` cho fan-out song song được, nhưng đưa `Mono` khắp service layer chỉ thêm phức tạp; với virtual threads có thể fan-out bằng `CompletableFuture` + executor virtual thread (hoặc structured concurrency khi ổn định).

**Câu hỏi nối tiếp:**
- *Có số liệu hỗ trợ không?* → Benchmark trên workload thật (k6/Gatling): throughput/p99 cho I/O thuần và khi có DB pool 10.

**⚠️ Câu trả lời gây điểm trừ:** "WebFlux luôn nhanh hơn" hoặc chọn theo trào lưu.

**📖 Ôn lại:** [§8.2 Khi nào WebFlux, khi nào MVC](../01-giao-trinh/08-spring-web-rest-security.md#p8)

</details>

### Q33. 🟡 CORS là gì? Vì sao SPA gọi API có JWT bị lỗi preflight 401 và cấu hình đúng với Spring Security ra sao?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Trình duyệt áp **Same-Origin Policy**; **CORS** là cơ chế server **nới lỏng** bằng header `Access-Control-Allow-*` để JS từ origin khác đọc được response. Request "không đơn giản" (header `Authorization`, `Content-Type: application/json`, method PUT/DELETE...) → trình duyệt gửi **preflight `OPTIONS`** không kèm token → nếu Spring Security chặn trước thì 401. Sửa: bật `http.cors(Customizer.withDefaults())` để `CorsFilter` xử lý preflight **trước** authorization, cấu hình `CorsConfigurationSource` với **whitelist origin**. CORS **không** bảo vệ server (curl/Postman bỏ qua CORS) — nó bảo vệ người dùng trình duyệt.

**Giải thích chi tiết:**

```java
CorsConfiguration cfg = new CorsConfiguration();
cfg.setAllowedOrigins(List.of("https://app.example.com"));
cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE"));
cfg.setAllowedHeaders(List.of("Authorization", "Content-Type", "Idempotency-Key", "If-Match"));
cfg.setExposedHeaders(List.of("Location", "ETag", "X-Correlation-Id"));
cfg.setAllowCredentials(true);         // KHÔNG dùng cùng allowedOrigins("*")
cfg.setMaxAge(Duration.ofHours(1));    // cache preflight
```

- CORS không chặn được "simple request" (form POST) từ site độc hại tới server — server vẫn nhận và xử lý; vì vậy app dùng cookie cần **CSRF protection**.

**Câu hỏi nối tiếp:**
- *`exposedHeaders` để làm gì?* → JS chỉ đọc được một số header mặc định; `Location`, `ETag` phải expose tường minh.

**⚠️ Câu trả lời gây điểm trừ:** "CORS là cơ chế bảo mật của server"; `permitAll()` cho `OPTIONS /**` mà không hiểu.

**📖 Ôn lại:** [§8.3 CORS](../01-giao-trinh/08-spring-web-rest-security.md#p8)

</details>

### Q34. 🔴 [Đọc code] Cấu hình CORS sau có lỗ hổng gì?

```java
@Component
class OpenCorsFilter extends OncePerRequestFilter {
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String origin = req.getHeader("Origin");
        if (origin != null) {
            res.setHeader("Access-Control-Allow-Origin", origin);         // "cho tiện mọi môi trường"
            res.setHeader("Access-Control-Allow-Credentials", "true");
        }
        chain.doFilter(req, res);
    }
}
```

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **Phản chiếu Origin bất kỳ** kèm `Allow-Credentials: true` → **mọi website độc hại** có thể gửi request bằng cookie session của nạn nhân và **đọc được response** (dữ liệu cá nhân, token CSRF...) — tương đương tắt Same-Origin Policy cho API. `allowedOriginPatterns("*")` + credentials cũng nguy hiểm tương tự. Sửa: whitelist origin cụ thể theo cấu hình môi trường, dùng `CorsConfigurationSource` của Spring (tự thêm `Vary: Origin`), không tự viết filter.

**Giải thích chi tiết:**
- Thiếu `Vary: Origin` khi trả `Allow-Origin` động → cache/CDN có thể phục vụ header của origin này cho origin khác.
- Whitelist bằng so khớp chính xác; tránh regex lỏng (`.*example.com` khớp `evil-example.com`) và `null` origin (sandboxed iframe, file://).
- Với API thuần bearer token (không cookie) thì không cần `Allow-Credentials`.

**Câu hỏi nối tiếp:**
- *Môi trường dev cần localhost?* → Thêm origin dev qua property của profile `local`, không mở toàn bộ.

**⚠️ Câu trả lời gây điểm trừ:** "Chỉ là CORS, không ảnh hưởng bảo mật."

**📖 Ôn lại:** [§8.3 Lỗi thường gặp (CORS)](../01-giao-trinh/08-spring-web-rest-security.md#p8)

</details>

---

<a id="nhom-i"></a>
## I. Kiến trúc Spring Security

### Q35. 🟢 Spring Security được "cắm" vào Servlet container như thế nào? `DelegatingFilterProxy`, `FilterChainProxy`, `SecurityFilterChain` là gì?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **`DelegatingFilterProxy`** là Servlet Filter (tên `springSecurityFilterChain`) đăng ký với container, cầu nối vòng đời container với `ApplicationContext` — trì hoãn lookup bean vì container đăng ký filter trước khi bean sẵn sàng. Nó ủy quyền cho bean **`FilterChainProxy`** — điểm vào duy nhất của Spring Security: chọn **`SecurityFilterChain` đầu tiên** có `securityMatcher` khớp request, chạy các security filter của chain đó theo thứ tự; còn áp `HttpFirewall` (`StrictHttpFirewall` chặn `..`, `;`, `%2F`...) và dọn `SecurityContextHolder` sau request.

**Giải thích chi tiết:**
- Thứ tự filter tiêu biểu: `SecurityContextHolderFilter` → `HeaderWriterFilter` → `CorsFilter` → `CsrfFilter` → `LogoutFilter` → `UsernamePasswordAuthenticationFilter`/`BearerTokenAuthenticationFilter`/`BasicAuthenticationFilter` → `RequestCacheAwareFilter` → `AnonymousAuthenticationFilter` → `SessionManagementFilter` → **`ExceptionTranslationFilter`** → **`AuthorizationFilter`**.
- Xem chain thực tế: `logging.level.org.springframework.security=TRACE`.
- Chain of Responsibility pattern.

**Câu hỏi nối tiếp:**
- *Thêm filter tự viết vào chain?* → `http.addFilterBefore(apiKeyFilter, BearerTokenAuthenticationFilter.class)`; đừng để Boot tự đăng ký nó lần nữa như servlet filter.

**⚠️ Câu trả lời gây điểm trừ:** "Spring Security dùng interceptor/AOP để kiểm tra URL."

**📖 Ôn lại:** [§9.1 Từ Servlet filter tới SecurityFilterChain](../01-giao-trinh/08-spring-web-rest-security.md#p9)

</details>

### Q36. 🟡 Mô tả luồng authentication trong Spring Security từ filter tới `SecurityContextHolder`.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Filter xác thực (ví dụ `UsernamePasswordAuthenticationFilter`, `BearerTokenAuthenticationFilter`) tạo `Authentication` **chưa xác thực** → gọi `AuthenticationManager.authenticate()` (implementation `ProviderManager`) → duyệt các `AuthenticationProvider` có `supports(type)`: `DaoAuthenticationProvider` (`UserDetailsService.loadUserByUsername` + `PasswordEncoder.matches`), `JwtAuthenticationProvider` (`JwtDecoder.decode` + `JwtAuthenticationConverter`) → thành công trả `Authentication` đã xác thực (principal, authorities, credentials bị xóa) → `SecurityContextHolder.getContext().setAuthentication(auth)` (+ lưu `SecurityContextRepository` nếu stateful) → thất bại: `AuthenticationEntryPoint`/`AuthenticationFailureHandler`.

**Giải thích chi tiết:**
- Không provider nào hỗ trợ → `ProviderNotFoundException`; có thể có parent `AuthenticationManager`.
- Custom: API key service-to-service = `ApiKeyAuthenticationFilter` + `ApiKeyAuthenticationToken` + `ApiKeyAuthenticationProvider` (tra key **đã hash** trong DB, so sánh **constant-time** `MessageDigest.isEqual` để chống timing attack).
- Security 6: phải tạo context mới `SecurityContextHolder.createEmptyContext()` và lưu tường minh qua `SecurityContextRepository` khi tự xác thực (không còn tự lưu vào session).

**Câu hỏi nối tiếp:**
- *`AuthorizationFilter` làm gì?* → Gọi `AuthorizationManager` theo rule của `authorizeHttpRequests`; từ chối → `AccessDeniedException`.

**⚠️ Câu trả lời gây điểm trừ:** Không nêu được vai trò `AuthenticationManager`/`Provider`.

**📖 Ôn lại:** [§9.3 Authentication flow](../01-giao-trinh/08-spring-web-rest-security.md#p9)

</details>

### Q37. 🟡 [Tình huống] App có 3 `SecurityFilterChain`: `/actuator/**`, `/api/**` (JWT) và chain mặc định (form login). Request `/api/orders` có token hợp lệ vẫn bị redirect tới `/login`. Vì sao?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `FilterChainProxy` chọn **chain đầu tiên khớp**. Chain mặc định (không có `securityMatcher` → khớp mọi request) đang có **order nhỏ hơn** (hoặc không có `@Order` nên thứ tự không như ý) → nó xử lý `/api/**` bằng form login, không có `BearerTokenAuthenticationFilter` → redirect `/login`. Sửa: mọi chain cụ thể có `securityMatcher` rõ ràng và `@Order` nhỏ; chain "bắt tất cả" phải có order **lớn nhất**. Debug bằng log TRACE (in ra chain nào được chọn và các filter chạy).

**Giải thích chi tiết:**

```java
@Bean @Order(1) SecurityFilterChain actuator(HttpSecurity http) throws Exception {
    http.securityMatcher(EndpointRequest.toAnyEndpoint()) ... ; return http.build(); }
@Bean @Order(2) SecurityFilterChain api(HttpSecurity http) throws Exception {
    http.securityMatcher("/api/**").oauth2ResourceServer(o -> o.jwt(Customizer.withDefaults())) ... ; return http.build(); }
@Bean @Order(3) SecurityFilterChain web(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(a -> a.anyRequest().authenticated()).formLogin(Customizer.withDefaults()); return http.build(); }
```

- Câu hỏi debug khác: "403 khi POST dù đã đăng nhập" → thường do **`CsrfFilter`** chặn trước khi tới authorization.

**Câu hỏi nối tiếp:**
- *401 dù token đúng?* → Kiểm tra `iss`/`aud`, clock skew, `kid` không có trong JWKS, chain không có resource server.

**⚠️ Câu trả lời gây điểm trừ:** Đoán mò cấu hình mà không biết cơ chế "first match".

**📖 Ôn lại:** [§9.1 & §9.4 Góc nhìn Senior](../01-giao-trinh/08-spring-web-rest-security.md#p9)

</details>

### Q38. 🔴 `SecurityContextHolder` lưu context ở đâu? Điều gì xảy ra với `@Async`, thread pool, virtual threads, và vì sao `MODE_INHERITABLETHREADLOCAL` nguy hiểm?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Mặc định `MODE_THREADLOCAL` — mỗi thread một context; `FilterChainProxy` dọn sau request. Thread khác (`@Async`, executor, `CompletableFuture.supplyAsync`, virtual thread mới) **không có** context → `getAuthentication()` trả `null`. Truyền tường minh: `DelegatingSecurityContextAsyncTaskExecutor`, `DelegatingSecurityContextRunnable/Callable`, hoặc `TaskDecorator` copy context (và dọn trong `finally`). `MODE_INHERITABLETHREADLOCAL` chỉ copy khi **thread con được tạo** — với thread pool, thread tạo một lần rồi tái sử dụng → giữ context của **user đầu tiên** và chạy task của user khác dưới danh tính sai — lỗ hổng phân quyền nghiêm trọng.

**Giải thích chi tiết:**
- `MODE_GLOBAL`: dùng chung toàn JVM — chỉ hợp app desktop.
- Reactive: `ReactiveSecurityContextHolder` dựa trên Reactor `Context`.
- Virtual threads vẫn là ThreadLocal per thread → phải truyền tường minh như platform thread.
- Lưu ý `TaskDecorator` của `ThreadPoolTaskExecutor` chỉ nhận một decorator → lồng nếu cần MDC + security + trace.

**Câu hỏi nối tiếp:**
- *Job `@Scheduled` cần gọi service có `@PreAuthorize`?* → Tạo `Authentication` hệ thống (service account) tường minh cho job, không mượn context người dùng.

**⚠️ Câu trả lời gây điểm trừ:** Đề xuất `MODE_INHERITABLETHREADLOCAL` để "sửa" lỗi null trong `@Async`.

**📖 Ôn lại:** [§9.4 SecurityContextHolder & thread-local](../01-giao-trinh/08-spring-web-rest-security.md#p9)

</details>

### Q39. 🟡 Những thay đổi chính từ Spring Security 5.x lên 6.x (Boot 3) mà bạn phải sửa khi migrate?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `WebSecurityConfigurerAdapter` deprecated ở 5.7 và **bị xóa** ở 6.0 → khai báo bean `SecurityFilterChain`; `authorizeRequests` → **`authorizeHttpRequests`** (dựa trên `AuthorizationManager`); `antMatchers/mvcMatchers` → **`requestMatchers`**; `@EnableGlobalMethodSecurity` → **`@EnableMethodSecurity`**; lambda DSL là chuẩn (Security 7 bỏ chaining `.and()`); `SecurityContextHolderFilter` yêu cầu **lưu context tường minh**; authorization áp cho **mọi dispatcher type** (kể cả `ERROR`, `FORWARD`) — trang lỗi có thể bị 401/403 nếu không `permitAll`; CSRF dùng `XorCsrfTokenRequestAttributeHandler` và **deferred** token → SPA cần cấu hình lại.

**Giải thích chi tiết:**

```java
@Bean
SecurityFilterChain api(HttpSecurity http) throws Exception {
    http.securityMatcher("/api/**")
        .csrf(csrf -> csrf.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(a -> a
            .requestMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()
            .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
            .anyRequest().authenticated())                       // deny-by-default
        .oauth2ResourceServer(o -> o.jwt(Customizer.withDefaults()));
    return http.build();
}
```

**Câu hỏi nối tiếp:**
- *Bảo vệ actuator trong Security 6?* → Chain riêng với `EndpointRequest.toAnyEndpoint()`.

**⚠️ Câu trả lời gây điểm trừ:** Vẫn viết `extends WebSecurityConfigurerAdapter`.

**📖 Ôn lại:** [§9.2 Cấu hình Spring Security 6](../01-giao-trinh/08-spring-web-rest-security.md#p9)

</details>

---

<a id="nhom-j"></a>
## J. Password, UserDetails, authorization cơ bản

### Q40. 🟢 Lưu password người dùng thế nào? Vì sao không dùng SHA-256 (kể cả có salt)?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Dùng hàm băm password **chậm có chủ đích, có salt, tham số chi phí điều chỉnh được**: **Argon2id** (memory-hard, khuyến nghị OWASP), **bcrypt**, **scrypt**, hoặc **PBKDF2** (khi cần FIPS). SHA-256 được thiết kế để **nhanh** — GPU thử hàng tỷ hash/giây → khi DB lộ, brute-force/dictionary attack rất rẻ; salt chỉ chống rainbow table, không làm chậm. Không bao giờ mã hóa hai chiều (encrypt) password, không tự viết thuật toán.

**Giải thích chi tiết:**
- Spring: `PasswordEncoderFactories.createDelegatingPasswordEncoder()` (mặc định bcrypt), `Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()`, `BCryptPasswordEncoder(12)`.
- Tham số: chọn để một lần hash mất khoảng 100–500ms trên phần cứng production — cân bằng an ninh với CPU khi có nhiều login (hash nặng là vector DoS).
- `NoOpPasswordEncoder` chỉ cho test, deprecated.

**Câu hỏi nối tiếp:**
- *Pepper là gì?* → Secret toàn cục (lưu ngoài DB, như trong KMS) trộn thêm; DB lộ mà pepper không lộ thì hash vô dụng với kẻ tấn công.

**⚠️ Câu trả lời gây điểm trừ:** "MD5/SHA-256 + salt là đủ"; "mã hóa AES để còn gửi lại password cho user".

**📖 Ôn lại:** [§10.1 PasswordEncoder](../01-giao-trinh/08-spring-web-rest-security.md#p10)

</details>

### Q41. 🟡 `DelegatingPasswordEncoder` giải quyết vấn đề gì? Migrate hàng triệu password từ SHA-1 sang bcrypt thế nào mà không bắt user đổi password?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Hash lưu kèm **prefix id thuật toán**: `{bcrypt}$2a$10$...`, `{argon2}...`, `{sha256}...`. Encoder đọc prefix để chọn thuật toán khi `matches`, và luôn `encode` bằng thuật toán mặc định mới → **nhiều thuật toán cùng tồn tại**. Migrate: (1) thêm prefix `{legacy-sha1}` cho hash cũ (đăng ký encoder legacy trong map); (2) khi user **đăng nhập thành công**, `upgradeEncoding()` trả true → `UserDetailsPasswordService.updatePassword()` tự lưu hash bcrypt mới (`DaoAuthenticationProvider` gọi tự động); (3) tài khoản không đăng nhập sau N tháng → bọc hash cũ: `bcrypt(sha1(pw))` hoặc buộc reset.

**Giải thích chi tiết:**
- bcrypt chỉ dùng **72 byte** đầu của input — password dài hơn bị cắt; từ Spring Security 6.4.x/6.5 `BCryptPasswordEncoder` từ chối input dài hơn 72 byte (CVE-2025-22228 về việc `matches` trả true với password dài sai phần đuôi). Giới hạn độ dài password ở tầng validation hoặc dùng Argon2.
- Strength mặc định của `BCryptPasswordEncoder` là 10 (2^10 vòng); tăng strength cũng được nâng cấp dần nhờ `upgradeEncoding`.

**Câu hỏi nối tiếp:**
- *Vì sao không hash lại tất cả một lần?* → Không có password gốc; chỉ có thể nâng cấp khi user nhập password hoặc bọc hash.

**⚠️ Câu trả lời gây điểm trừ:** "Gửi email bắt mọi user đổi password" là cách duy nhất.

**📖 Ôn lại:** [§10.1 DelegatingPasswordEncoder](../01-giao-trinh/08-spring-web-rest-security.md#p10)

</details>

### Q42. 🟡 `hasRole("ADMIN")` vs `hasAuthority("ADMIN")`? Và làm sao chống brute-force/enumeration ở endpoint login?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `hasRole("ADMIN")` kiểm tra authority **`ROLE_ADMIN`** (tự thêm prefix `ROLE_`); `hasAuthority("ADMIN")` so khớp chính xác chuỗi. Lỗi kinh điển: lưu `"ADMIN"` rồi dùng `hasRole("ADMIN")` → luôn 403. Với JWT, scope thành `SCOPE_orders:read` → dùng `hasAuthority("SCOPE_orders:read")`. Chống brute-force/enumeration: **rate limit** theo IP và theo username, lockout tạm thời/tăng delay (cẩn thận: lockout cứng = DoS khóa tài khoản người khác), thông báo lỗi **chung** ("sai tên đăng nhập hoặc mật khẩu"), thời gian phản hồi đồng nhất, MFA, kiểm tra password đã lộ.

**Giải thích chi tiết:**
- `DaoAuthenticationProvider` mặc định `hideUserNotFoundExceptions=true` và vẫn chạy hash giả khi user không tồn tại để **thời gian** phản hồi không lộ username tồn tại.
- Endpoint "quên mật khẩu"/"đăng ký" cũng là kênh enumeration — trả cùng thông báo.
- Security 6.3+: `CompromisedPasswordChecker` (`HaveIBeenPwnedRestApiPasswordChecker`, dùng k-anonymity).
- Hash nặng × login spam = DoS CPU → rate limit trước khi hash.

**Câu hỏi nối tiếp:**
- *Role hierarchy?* → Bean `RoleHierarchy` (`ROLE_ADMIN > ROLE_STAFF > ROLE_USER`).

**⚠️ Câu trả lời gây điểm trừ:** Trả "user không tồn tại" vs "sai mật khẩu" khác nhau.

**📖 Ôn lại:** [§10.2 UserDetailsService, roles vs authorities](../01-giao-trinh/08-spring-web-rest-security.md#p10)

</details>

---

<a id="nhom-k"></a>
## K. Session vs JWT

### Q43. 🟡 Session (stateful) hay JWT (stateless) cho một web app/API? Trade-off?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **Session**: server giữ trạng thái, client giữ session id trong cookie `HttpOnly; Secure; SameSite` — **thu hồi tức thì** (xóa session), token nhỏ, nhưng cần session store chia sẻ khi scale ngang (Spring Session + Redis) và CSRF protection. **JWT**: self-contained, verify bằng chữ ký không cần tra DB — hợp cho **nhiều service/resource server**, nhưng **khó thu hồi** trước khi hết hạn, token lớn, dễ bị lưu sai chỗ ở client. Web app một backend: session + Spring Session thường đơn giản và an toàn hơn. Hệ microservice với IdP: access token JWT ngắn hạn cho resource server, còn browser nên dùng mô hình BFF.

**Giải thích chi tiết:**
- Session fixation: Spring Security mặc định đổi session id sau login (`changeSessionId`).
- "Stateless" của JWT là ảo tưởng một phần: revocation, refresh token rotation đều cần trạng thái ở đâu đó.
- Concurrent session control (`maximumSessions(1)`) dễ với session, khó với JWT.

**Câu hỏi nối tiếp:**
- *Spring Session Redis có tác dụng phụ gì?* → Mỗi request đọc/ghi Redis; Redis thành phụ thuộc quan trọng; serialize principal (cẩn thận thay đổi class).

**⚠️ Câu trả lời gây điểm trừ:** "JWT luôn tốt hơn session vì stateless."

**📖 Ôn lại:** [§11.1 Session vs token](../01-giao-trinh/08-spring-web-rest-security.md#p11)

</details>

### Q44. 🟢 Cấu trúc JWT? Payload có được mã hóa không?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Ba phần Base64URL nối bằng dấu chấm: **header** (`alg`, `typ`, `kid`), **payload/claims** (`iss`, `sub`, `aud`, `exp`, `iat`, `nbf`, `jti`, `scope`...), **signature**. JWS chỉ **ký** — payload chỉ được **encode**, ai cũng decode đọc được (jwt.io) → **không** đặt password, PII nhạy cảm, secret vào payload. Chữ ký đảm bảo **toàn vẹn và nguồn gốc**, không đảm bảo bí mật. Cần bí mật thì dùng **JWE** (mã hóa) — ít dùng, phức tạp hơn.

**Giải thích chi tiết:**
- Token nằm trong log, URL, header proxy → coi như công khai với ai thấy được nó.
- Token càng nhiều claim càng to (mỗi request đều gửi) — giữ claim tối thiểu.

**Câu hỏi nối tiếp:**
- *`iat` vs `nbf` vs `exp`?* → Thời điểm phát hành / không hợp lệ trước / hết hạn; cho clock skew nhỏ (Spring mặc định 60s).

**⚠️ Câu trả lời gây điểm trừ:** "JWT được mã hóa nên an toàn chứa thông tin user."

**📖 Ôn lại:** [§11.2 Cấu trúc JWT](../01-giao-trinh/08-spring-web-rest-security.md#p11)

</details>

### Q45. 🔴 Resource server phải validate JWT những gì? Giải thích tấn công `alg: none` và HS/RS confusion. HS256 hay RS256?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Validate: (1) **chữ ký** bằng khóa tin cậy với **thuật toán cố định** phía server (không tin `alg` trong header); (2) `exp`/`nbf` (với clock skew); (3) **`iss`** đúng IdP; (4) **`aud`** chứa service này (chống dùng token của service khác); (5) kiểu token (access chứ không phải ID token), claim/scope cần thiết. **`alg: none`**: thư viện ngây thơ chấp nhận token không chữ ký. **HS/RS confusion**: server kỳ vọng RS256 nhưng thư viện lấy `alg` từ header; attacker đổi thành HS256 và ký bằng **public key** (công khai) như HMAC secret → hợp lệ. **RS256/ES256** (bất đối xứng): IdP giữ private key, resource server chỉ cần public key qua **JWKS** (`kid` chọn key, xoay vòng dễ) — chuẩn cho nhiều service. **HS256** (shared secret): mọi bên verify cũng **tạo** được token → chỉ khi issuer và verifier là một.

**Giải thích chi tiết:**

```java
@Bean JwtDecoder jwtDecoder(@Value("${app.jwt.issuer}") String issuer) {
    NimbusJwtDecoder decoder = JwtDecoders.fromIssuerLocation(issuer);   // JWKS + fix alg theo key
    OAuth2TokenValidator<Jwt> audience = new JwtClaimValidator<List<String>>(
            JwtClaimNames.AUD, aud -> aud != null && aud.contains("order-service"));
    decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
            JwtValidators.createDefaultWithIssuer(issuer), audience));
    return decoder;
}
```

- Boot 3.2+: `spring.security.oauth2.resourceserver.jwt.audiences` thay cho validator tự viết.
- Nimbus (Spring) không chấp nhận `none` và cố định thuật toán theo JWK → an toàn nếu không tự viết parser.
- HS256 secret phải đủ dài (≥ 256 bit ngẫu nhiên), không phải chuỗi "secret".

**Câu hỏi nối tiếp:**
- *IdP xoay key, `kid` mới không có trong cache?* → Nimbus tự refetch JWKS khi gặp `kid` lạ (có rate limit).

**⚠️ Câu trả lời gây điểm trừ:** Chỉ kiểm tra chữ ký và `exp`, bỏ qua `aud`/`iss`.

**📖 Ôn lại:** [§11.3 Validate JWT](../01-giao-trinh/08-spring-web-rest-security.md#p11)

</details>

### Q46. 🔴 Làm sao thu hồi JWT khi user logout/bị khóa? Thiết kế refresh token an toàn.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Không thể "hủy" một JWT đã phát; chọn chiến lược: (1) **access token ngắn hạn** (5–15 phút) — chấp nhận cửa sổ rủi ro nhỏ; (2) **denylist theo `jti`** trong Redis với TTL = thời gian còn lại của token (chỉ lưu token bị thu hồi — nhỏ); (3) **`token_version`/`sessions_valid_after`** trên user — token có version cũ bị từ chối (khóa user = tăng version), cần tra cache mỗi request; (4) **opaque token + introspection** — thu hồi tức thì, đổi lại phụ thuộc IdP. Refresh token: dài hạn, lưu phía server (hash), **rotation** — mỗi lần dùng cấp refresh token mới, vô hiệu cái cũ; **reuse detection** — một refresh token đã dùng bị dùng lại → nghi bị đánh cắp → thu hồi cả **token family**.

**Giải thích chi tiết:**
- Refresh token gắn client/device, lưu trong cookie `HttpOnly; Secure; SameSite=Strict; Path=/auth/refresh`.
- Đổi password, khóa tài khoản → thu hồi mọi refresh token + tăng token version.
- Rotation đồng thời từ nhiều tab có thể gây false positive reuse → cho grace period vài giây.

**Câu hỏi nối tiếp:**
- *Denylist làm mất "stateless"?* → Đúng một phần; nhưng tra Redis O(1) cho token bị thu hồi vẫn rẻ hơn session đầy đủ.

**⚠️ Câu trả lời gây điểm trừ:** "Logout thì xóa token ở client là đủ"; access token sống 30 ngày.

**📖 Ôn lại:** [§11.4 Revocation & refresh token](../01-giao-trinh/08-spring-web-rest-security.md#p11)

</details>

### Q47. 🟡 SPA nên lưu access token ở đâu: `localStorage`, memory hay cookie?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `localStorage`/`sessionStorage`: JS đọc được → **một lỗi XSS là mất token** (exfiltrate gửi đi nơi khác, dùng được ngoài trình duyệt). Memory (biến JS): sống trong tab, mất khi reload, XSS vẫn dùng được trong lúc tab mở nhưng khó lấy lâu dài. **Cookie `HttpOnly; Secure; SameSite`**: JS không đọc được → chống exfiltration qua XSS, nhưng trình duyệt tự gửi → cần **CSRF protection**. Khuyến nghị hiện nay (IETF "OAuth 2.0 for Browser-Based Apps"): mô hình **BFF** — backend-for-frontend giữ token phía server, SPA chỉ có session cookie với BFF; token không bao giờ tới trình duyệt.

**Giải thích chi tiết:**
- Không có lựa chọn nào an toàn nếu có XSS — XSS có thể gửi request thay user trong phiên; mục tiêu là giảm thiệt hại và tầm sử dụng.
- Phòng XSS chính vẫn là output encoding, CSP, không dùng `innerHTML` với dữ liệu người dùng.
- Spring: Spring Cloud Gateway hoặc app `oauth2Login` làm BFF với `TokenRelay`.

**Câu hỏi nối tiếp:**
- *SameSite=Lax đủ chống CSRF chưa?* → Giảm phần lớn nhưng không chặn GET có side effect và subdomain cùng site — vẫn dùng CSRF token cho thao tác thay đổi trạng thái.

**⚠️ Câu trả lời gây điểm trừ:** "localStorage là chuẩn, không có vấn đề."

**📖 Ôn lại:** [§11.5 Lưu token ở client](../01-giao-trinh/08-spring-web-rest-security.md#p11)

</details>

---

<a id="nhom-l"></a>
## L. OAuth2 / OIDC

### Q48. 🟢 OAuth2 và OpenID Connect khác nhau thế nào? ID token khác access token ra sao?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **OAuth2** là framework **ủy quyền** (authorization): cấp cho client **access token** để gọi API thay mặt user với phạm vi (scope) giới hạn — không định nghĩa "user là ai". **OIDC** là lớp **xác thực** (authentication) trên OAuth2: thêm scope `openid`, **ID token** (JWT nói user là ai: `sub`, `email`, `auth_time`, `nonce`), endpoint `userinfo`, discovery (`/.well-known/openid-configuration`). **ID token** dành cho **client** (audience là client_id) để biết ai đăng nhập — **không** gửi tới API. **Access token** dành cho **resource server** (audience là API).

**Giải thích chi tiết:**
- Vai trò OAuth2: resource owner, client, authorization server, resource server.
- Spring: `oauth2Login` (client, OIDC login), `oauth2ResourceServer` (API), Spring Authorization Server (tự dựng IdP); thực tế thường dùng Keycloak/Okta/Auth0/Entra ID.
- "Login with OAuth2" thuần (không OIDC) dễ bị lỗi kiểu dùng access token như bằng chứng danh tính.

**Câu hỏi nối tiếp:**
- *Vì sao không dùng ID token gọi API?* → Sai audience, chứa thông tin cá nhân, không mang scope cho API.

**⚠️ Câu trả lời gây điểm trừ:** "OAuth2 là giao thức đăng nhập."

**📖 Ôn lại:** [§12.1 OAuth2 & OIDC](../01-giao-trinh/08-spring-web-rest-security.md#p12)

</details>

### Q49. 🟡 Mô tả Authorization Code flow + PKCE. Vì sao implicit và password grant bị loại bỏ?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** (1) Client sinh `code_verifier` ngẫu nhiên, tính `code_challenge = BASE64URL(SHA256(verifier))`; (2) redirect user tới `/authorize?response_type=code&client_id&redirect_uri&scope&state&code_challenge&code_challenge_method=S256`; (3) user đăng nhập, đồng ý → IdP redirect về `redirect_uri?code=...&state=...`; (4) client kiểm tra `state` (chống CSRF), POST `/token` với `code` + `code_verifier`; (5) IdP kiểm tra `SHA256(verifier) == challenge` → trả access/ID/refresh token. **PKCE** chống **đánh cắp authorization code** (app khác đăng ký cùng custom scheme, log, referrer): có code mà không có verifier thì không đổi được token. OAuth 2.1 / RFC 9700 (BCP): PKCE bắt buộc cho mọi client; **implicit** bị loại (token đi qua URL fragment, lộ qua history/referrer, không có refresh an toàn); **password grant (ROPC)** bị loại (client thấy password, phá mô hình ủy quyền, không hỗ trợ MFA/SSO).

**Giải thích chi tiết:**
- `redirect_uri` phải khớp **chính xác** đã đăng ký (open redirect → mất code).
- Public client (SPA, mobile) không giữ được secret → PKCE là cơ chế bảo vệ chính; confidential client dùng cả secret/private_key_jwt + PKCE.
- Spring `oauth2Login` tự xử lý state, nonce, PKCE (với public client hoặc khi bật).

**Câu hỏi nối tiếp:**
- *`nonce` khác `state`?* → `nonce` nằm trong ID token, chống replay ID token; `state` gắn request authorize với callback.

**⚠️ Câu trả lời gây điểm trừ:** Khuyên dùng implicit cho SPA hoặc password grant cho "app first-party".

**📖 Ôn lại:** [§12.2 Authorization Code + PKCE](../01-giao-trinh/08-spring-web-rest-security.md#p12)

</details>

### Q50. 🟡 Cấu hình Spring Boot làm resource server với Keycloak: vì sao `@PreAuthorize("hasRole('ADMIN')")` luôn 403 dù token có role admin?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Mặc định `JwtAuthenticationConverter` chỉ map claim `scope`/`scp` thành authority `SCOPE_xxx`; role của Keycloak nằm ở `realm_access.roles` (hoặc `resource_access.<client>.roles`) nên **không được map** → không có `ROLE_ADMIN`. Sửa bằng converter tùy chỉnh đọc claim role và thêm prefix `ROLE_`.

**Giải thích chi tiết:**

```yaml
spring.security.oauth2.resourceserver.jwt:
  issuer-uri: https://sso.example.com/realms/shop     # discovery + JWKS + validate iss
  audiences: order-service                              # Boot 3.2+
```

```java
@Bean JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();   // SCOPE_*
    JwtAuthenticationConverter conv = new JwtAuthenticationConverter();
    conv.setJwtGrantedAuthoritiesConverter(jwt -> {
        Collection<GrantedAuthority> auths = new ArrayList<>(scopes.convert(jwt));
        Map<String, Object> realm = jwt.getClaimAsMap("realm_access");
        if (realm != null && realm.get("roles") instanceof Collection<?> roles)
            roles.forEach(r -> auths.add(new SimpleGrantedAuthority("ROLE_" + r)));
        return auths;
    });
    conv.setPrincipalClaimName("preferred_username");
    return conv;
}
```

- `issuer-uri` khiến app gọi discovery lúc khởi động (IdP down → app không start); có thể dùng `jwk-set-uri` để lazy.
- Boot cũng có property `authority-prefix`, `authorities-claim-name` cho trường hợp claim phẳng.

**Câu hỏi nối tiếp:**
- *Test controller có JWT?* → `mockMvc.perform(get(...).with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))`.

**⚠️ Câu trả lời gây điểm trừ:** Tự parse JWT bằng tay trong filter thay vì dùng `oauth2ResourceServer`.

**📖 Ôn lại:** [§12.3 Resource server với Spring](../01-giao-trinh/08-spring-web-rest-security.md#p12)

</details>

### Q51. 🔴 Service A cần gọi service B: dùng client credentials, chuyển tiếp token của user, hay token exchange? Rủi ro "confused deputy"?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **Client credentials**: A dùng danh tính **của chính nó** — hợp cho job nền, thao tác hệ thống; B không biết user nào. **Token propagation (relay)**: chuyển tiếp access token của user — B biết user, nhưng token phải có `aud` chứa B (hoặc audience rộng — rủi ro: token cho A dùng được ở mọi service) và lộ quyền đầy đủ của user cho mọi hop. **Token exchange (RFC 8693)**: A đổi token user lấy token mới với `aud=B`, scope hẹp, có thể mang `act` (A hành động thay user) — đúng nguyên tắc least privilege nhất. **Confused deputy**: A có quyền cao (client credentials) gọi B **theo yêu cầu của user** mà không kiểm tra user có quyền với tài nguyên đó → user mượn quyền của A. Phòng: B kiểm tra quyền dựa trên danh tính user (token exchange/propagation), hoặc A phải tự authorize đúng trước khi gọi.

**Giải thích chi tiết:**
- Spring: `OAuth2AuthorizedClientManager` + `OAuth2ClientHttpRequestInterceptor` (Security 6.4+) cho RestClient; với WebClient `ServerOAuth2AuthorizedClientExchangeFilterFunction`; token exchange grant hỗ trợ từ Security 6.3.
- Cache token client credentials tới gần hết hạn (Spring tự làm qua `OAuth2AuthorizedClientService`), không xin token mỗi request.
- Mạng nội bộ không đồng nghĩa tin cậy (zero trust): vẫn mTLS/token giữa service.

**Câu hỏi nối tiếp:**
- *Service mesh mTLS có thay thế token không?* → mTLS xác thực **service**, không mang danh tính **user** và quyền của họ — bổ sung, không thay thế.

**⚠️ Câu trả lời gây điểm trừ:** "Mạng nội bộ nên không cần xác thực giữa các service."

**📖 Ôn lại:** [§12.4 Service-to-service](../01-giao-trinh/08-spring-web-rest-security.md#p12)

</details>

---

<a id="nhom-m"></a>
## M. Method security & CSRF

### Q52. 🟡 Dùng `@PreAuthorize` để kiểm tra ownership thế nào? Những bẫy của method security?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Bật `@EnableMethodSecurity` (Security 6, `prePostEnabled=true` mặc định). Ownership: SpEL gọi bean — `@PreAuthorize("@orderAuthz.canView(#id, authentication)")` — logic phức tạp đặt trong bean Java testable, không viết SpEL dài. Bẫy: (1) **self-invocation** — method security dựa trên AOP proxy, gọi nội bộ `this.x()` bỏ qua kiểm tra; (2) `@PostAuthorize` kiểm tra **sau khi** method chạy → method có side effect (ghi DB) đã xảy ra rồi; (3) `@PostFilter` lọc **trong bộ nhớ** sau khi tải hết — chậm, phá phân trang → lọc ở query; (4) đặt annotation trên interface/class không phải bean → không có hiệu lực; (5) quên bật annotation → mọi `@PreAuthorize` âm thầm vô hiệu.

**Giải thích chi tiết:**

```java
@Target(ElementType.METHOD) @Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasRole('ADMIN') or @orderAuthz.isOwner(#orderId, authentication)")
@interface CanAccessOrder {}                         // meta-annotation tái sử dụng

@CanAccessOrder
public OrderDto getOrder(UUID orderId) { ... }
```

- Defense in depth: URL rule ở `SecurityFilterChain` (thô) + method security (chi tiết) + query có điều kiện owner (Q54).
- Test bằng `@WithMockUser` và test âm (user B gọi tài nguyên của A → 403/404).

**Câu hỏi nối tiếp:**
- *`@Secured` vs `@RolesAllowed` vs `@PreAuthorize`?* → Hai cái đầu chỉ kiểm tra role; `@PreAuthorize` có SpEL và tham số method.

**⚠️ Câu trả lời gây điểm trừ:** Không biết method security dựa trên proxy.

**📖 Ôn lại:** [§13.1 Method security](../01-giao-trinh/08-spring-web-rest-security.md#p13)

</details>

### Q53. 🔴 Khi nào cần CSRF protection, khi nào được tắt? Cấu hình CSRF cho SPA dùng cookie session trên Security 6.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** CSRF lợi dụng việc **trình duyệt tự gửi credential** (cookie, Basic auth) kèm request từ site khác. **Cần** khi auth dựa trên cookie/session (kể cả JWT lưu trong cookie). **Được tắt** cho API **chỉ** nhận token qua header `Authorization: Bearer` (trình duyệt không tự gắn header này). Cho SPA dùng cookie: `CookieCsrfTokenRepository.withHttpOnlyFalse()` (JS đọc cookie `XSRF-TOKEN` gửi lại qua header `X-XSRF-TOKEN`). Security 6: token mặc định được **XOR mask** (`XorCsrfTokenRequestAttributeHandler`, chống BREACH) và **deferred** (chỉ load khi cần) → SPA đọc raw cookie sẽ lỗi 403 → dùng `CsrfTokenRequestAttributeHandler` (plain) hoặc handler tùy chỉnh theo hướng dẫn SPA của Spring, và bảo đảm cookie được sinh ra (truy cập token).

**Giải thích chi tiết:**

```java
http.csrf(csrf -> csrf
        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()));  // SPA đọc giá trị raw
// Security 6.4+ có shortcut: csrf.spa()  (kiểm tra theo phiên bản bạn dùng)
```

- Bổ sung: cookie `SameSite=Lax/Strict`, kiểm tra `Origin`/`Referer`, **GET không có side effect** (CSRF token không bảo vệ GET).
- Tắt CSRF "vì Postman báo 403" trên app dùng session là lỗ hổng kinh điển.

**Câu hỏi nối tiếp:**
- *XSS có vượt được CSRF token không?* → Có — XSS đọc được token trong trang; CSRF token chỉ chống request cross-site.

**⚠️ Câu trả lời gây điểm trừ:** "API REST nên luôn `csrf().disable()`" mà không xét cơ chế auth.

**📖 Ôn lại:** [§13.2 CSRF](../01-giao-trinh/08-spring-web-rest-security.md#p13)

</details>

---

<a id="nhom-n"></a>
## N. OWASP API Security & hardening

### Q54. 🔴 [Đọc code] Endpoint sau có lỗ hổng gì? Sửa thế nào, kể cả trong hệ multi-tenant?

```java
@GetMapping("/api/v1/orders/{id}")
@PreAuthorize("isAuthenticated()")
public OrderDto getOrder(@PathVariable Long id) {
    return orderRepository.findById(id).map(OrderDto::from)
            .orElseThrow(() -> new OrderNotFoundException(id));
}
```

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **BOLA/IDOR** (OWASP API1:2023 — lỗ hổng API phổ biến nhất): chỉ kiểm tra *đã đăng nhập*, không kiểm tra **đơn có thuộc về user này không** → user đổi `id` (tuần tự, dễ đoán) để đọc đơn người khác. Sửa: lọc ownership **ngay trong query** — `findByIdAndCustomerId(id, currentUserId)` — và trả **404** (không phải 403) để không xác nhận tài nguyên tồn tại; admin đi đường riêng có kiểm tra role. Đổi sang UUID chỉ giảm khả năng đoán, **không** thay authorization.

**Giải thích chi tiết:**

```java
@GetMapping("/api/v1/orders/{id}")
public OrderDto getOrder(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
    UUID customerId = UUID.fromString(jwt.getSubject());
    return orderRepository.findByIdAndCustomerId(id, customerId).map(OrderDto::from)
            .orElseThrow(() -> new OrderNotFoundException(id));          // 404
}
```

- Multi-tenant: `tenant_id` lấy từ **token**, không lấy từ header/param client gửi; áp tự động bằng Hibernate 6 `@TenantId`/filter, hoặc **PostgreSQL Row-Level Security** làm lớp bảo vệ cuối.
- Áp cho cả update/delete, endpoint lồng (`/orders/{id}/items/{itemId}` — item phải thuộc order đó), export, file download.
- Test: ma trận authorization (user A, user B, admin, anonymous) × endpoint, chạy trong CI.

**Câu hỏi nối tiếp:**
- *Phát hiện BOLA trên hệ thống đang chạy?* → Log truy cập: một user truy cập nhiều id khác nhau bất thường; pentest với 2 tài khoản.

**⚠️ Câu trả lời gây điểm trừ:** "Đã có `@PreAuthorize` nên an toàn" hoặc "dùng UUID là đủ".

**📖 Ôn lại:** [§14.1 OWASP API Top 10 — BOLA](../01-giao-trinh/08-spring-web-rest-security.md#p14)

</details>

### Q55. 🟡 Mass assignment và excessive data exposure là gì? Phòng tránh trong Spring?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Cả hai thuộc **API3:2023 Broken Object Property Level Authorization**. **Mass assignment**: bind request trực tiếp vào entity → client gửi thêm `"role":"ADMIN"`, `"balance":1000000`, `"status":"PAID"` và được lưu. **Excessive data exposure**: trả entity ra JSON → lộ `passwordHash`, email nội bộ, field admin; frontend "ẩn đi" không phải bảo vệ. Phòng: **DTO riêng** cho request (chỉ field được phép sửa, record + Bean Validation) và response (chỉ field cần thiết, theo vai trò nếu khác nhau); không dùng entity ở tầng web; `@JsonTest`/contract test khóa danh sách field của response.

**Giải thích chi tiết:**
- `@JsonIgnore` trên entity là phòng thủ mong manh — thêm field mới quên ignore là lộ.
- Có thể bật `FAIL_ON_UNKNOWN_PROPERTIES` cho request DTO để phát hiện client gửi field lạ (đánh đổi với tolerant reader — chọn có ý thức).
- PATCH: chỉ áp field được whitelist; với JSON Merge Patch phân biệt "không gửi" với `null`.

**Câu hỏi nối tiếp:**
- *Admin và user xem cùng resource với field khác nhau?* → Hai DTO/endpoint riêng, hoặc `@JsonView` (ít khuyến khích vì dễ nhầm).

**⚠️ Câu trả lời gây điểm trừ:** `@RequestBody User user` rồi `userRepository.save(user)`.

**📖 Ôn lại:** [§14.1 API3 Property level authorization](../01-giao-trinh/08-spring-web-rest-security.md#p14)

</details>

### Q56. 🔴 [Tình huống] Tính năng "nhập URL để set avatar": server tải ảnh từ URL người dùng cung cấp. Rủi ro và cách bảo vệ?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **SSRF** (API7:2023): attacker cho server gọi `http://169.254.169.254/latest/meta-data/iam/...` (metadata cloud → lấy credential IAM), `http://localhost:8080/actuator`, Redis/DB nội bộ, quét mạng nội bộ. Bảo vệ nhiều lớp: (1) chỉ **`https`**, chỉ port 443; (2) **resolve DNS** và chặn IP private/loopback/link-local (`10/8`, `172.16/12`, `192.168/16`, `127/8`, `169.254/16`, `::1`, `fc00::/7`, `fe80::/10`, IPv4-mapped IPv6); (3) **kết nối tới chính IP đã kiểm tra** (chống **DNS rebinding** — check xong DNS đổi sang IP nội bộ); (4) **không follow redirect** (hoặc kiểm tra lại từng hop); (5) giới hạn **kích thước**, **content-type**, timeout ngắn, decode ảnh thật rồi re-encode; (6) hạ tầng: **egress proxy** whitelist, IMDSv2 trên AWS, network policy. Tốt nhất: đổi thiết kế — client upload trực tiếp lên storage bằng presigned URL.

**Giải thích chi tiết:**
- Allowlist domain mạnh hơn denylist IP khi nghiệp vụ cho phép.
- Parser URL khác nhau hiểu `http://evil.com@127.0.0.1/` khác nhau → dùng một parser, kiểm tra trên kết quả resolve.
- Cũng thuộc dạng SSRF: webhook URL do khách hàng cấu hình, import từ URL, PDF render HTML.

**Câu hỏi nối tiếp:**
- *Phản hồi lỗi có lộ gì không?* → Thông báo lỗi chi tiết ("connection refused" vs "timeout") giúp quét port — trả lỗi chung.

**⚠️ Câu trả lời gây điểm trừ:** "Chặn chuỗi `localhost` và `127.0.0.1` trong URL là đủ."

**📖 Ôn lại:** [§14.1 API7 SSRF](../01-giao-trinh/08-spring-web-rest-security.md#p14)

</details>

### Q57. 🟡 Rate limiting cho API: đặt ở đâu, theo key nào, thuật toán gì? Còn những giới hạn tài nguyên nào khác (API4)?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Nhiều lớp: **gateway/edge** (Nginx, Spring Cloud Gateway `RequestRateLimiter` + Redis, WAF) chặn thô theo IP; **application** cho rule nghiệp vụ (theo user/API key/tenant, endpoint đắt như login, OTP, export) bằng Bucket4j/Resilience4j `RateLimiter`, state trong Redis khi nhiều instance. Thuật toán: **token bucket** (cho phép burst, phổ biến), sliding window. Trả **429** + `Retry-After`. Cẩn thận key theo IP sau proxy: chỉ tin `X-Forwarded-For` từ proxy tin cậy (`server.forward-headers-strategy=native/framework`) — nếu không attacker giả header để né. API4 còn gồm: giới hạn **page size**, kích thước body/upload, độ sâu/kích thước JSON (Jackson `StreamReadConstraints`), timeout, số phần tử batch, chi phí truy vấn.

**Giải thích chi tiết:**
- Rate limit theo user chỉ áp được sau authentication; trước đó theo IP/fingerprint.
- Tính năng tốn tiền (SMS OTP, email) cần giới hạn riêng — "unrestricted access to sensitive business flows" (API6).
- Rate limit in-memory mỗi instance: giới hạn thực tế = limit × số instance.

**Câu hỏi nối tiếp:**
- *Rate limit khác bulkhead/circuit breaker?* → Rate limit bảo vệ **mình** khỏi caller; bulkhead/circuit breaker bảo vệ mình khỏi **upstream** chậm.

**⚠️ Câu trả lời gây điểm trừ:** Chỉ rate limit theo IP trong bộ nhớ một instance và tin `X-Forwarded-For` vô điều kiện.

**📖 Ôn lại:** [§14.1 API4 Unrestricted resource consumption](../01-giao-trinh/08-spring-web-rest-security.md#p14)

</details>

### Q58. 🟡 [Tình huống] Bạn được giao review bảo mật một REST API Spring Boot trước khi go-live. Checklist của bạn?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Đi theo từng tầng: (1) **Authentication** — mọi endpoint cần auth trừ whitelist tường minh, deny-by-default, JWT validate đủ `iss/aud/exp`; (2) **Function-level** — endpoint admin có role check (API5); (3) **Object-level** — mọi truy cập theo id có kiểm tra ownership (BOLA); (4) **Property-level** — DTO request/response, không lộ field nhạy cảm; (5) **Resource consumption** — rate limit, page size, body size, timeout; (6) **Unsafe consumption** — validate dữ liệu từ API bên thứ ba như input người dùng (API10), SSRF; (7) **Cấu hình** — actuator chỉ `health`/`info` public, Swagger tắt ở prod, error response không có stack trace, security headers (HSTS, `X-Content-Type-Options`, CSP), CORS whitelist, TLS; (8) **Dependency** — quét CVE (OWASP Dependency-Check/Snyk/Dependabot), nâng cấp Boot kịp thời (bài học Spring4Shell, Log4Shell); (9) **Secrets/log** — không hard-code secret, không log token/password/PII; (10) **Kiểm thử** — test ma trận authorization tự động, DAST (OWASP ZAP), pentest.

**Giải thích chi tiết:**
- Inventory (API9): danh sách mọi endpoint/phiên bản đang chạy, tắt v1 cũ không ai quản lý.
- Audit log cho thao tác nhạy cảm (ai, làm gì, lúc nào, kết quả) — tách khỏi log ứng dụng.
- Spring Security mặc định đã thêm nhiều header (`X-Frame-Options: DENY`, `Cache-Control: no-store`...); kiểm tra không ai vô tình tắt.

**Câu hỏi nối tiếp:**
- *Ưu tiên gì nếu chỉ có 2 ngày?* → Ma trận authorization (BOLA/BFLA) và cấu hình lộ (actuator, swagger, error) — rủi ro cao, sửa nhanh.

**⚠️ Câu trả lời gây điểm trừ:** Checklist chỉ có "dùng HTTPS và JWT".

**📖 Ôn lại:** [§14.2 Hardening checklist](../01-giao-trinh/08-spring-web-rest-security.md#p14)

</details>

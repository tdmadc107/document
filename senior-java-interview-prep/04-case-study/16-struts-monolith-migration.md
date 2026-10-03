# Case 16 — Di chuyển monolith Struts 1/2 + JSP + Oracle 10 năm tuổi sang Spring Boot bằng strangler fig

> **Chủ đề:** Legacy modernization, strangler fig, chung sống hai hệ thống, chia sẻ phiên đăng nhập, bảo mật Struts (OGNL CVE), dữ liệu Oracle dùng chung, characterization test, tổ chức đội
> **Module liên quan:** [M10 §1 — Vì sao Senior ở Việt Nam vẫn gặp Struts](../01-giao-trinh/10-struts.md#1-boi-canh) · [M10 §8 — Lịch sử bảo mật & hardening](../01-giao-trinh/10-struts.md#8-security) · [M10 §10 — Chiến lược migration](../01-giao-trinh/10-struts.md#10-migration) · [M10 §7 — Tích hợp Spring & Hibernate](../01-giao-trinh/10-struts.md#7-integration) · [M14 §1 — Monolith, modular monolith, cách tách service](../01-giao-trinh/14-microservices-system-design.md#p1) · [M08 §12 — OAuth2/OIDC](../01-giao-trinh/08-spring-web-rest-security.md#p12) · [M11 §11 — PL/SQL & đặc thù Oracle](../01-giao-trinh/11-database-sql.md#phan-11) · [M15 §1 — Chiến lược kiểm thử](../01-giao-trinh/15-testing.md#p1) · [M06 §9 — Kiến trúc hexagonal/clean](../01-giao-trinh/06-design-principles-patterns.md#p9)
> **Độ khó:** ⭐⭐⭐⭐⭐ (Senior/Lead — bài thiết kế & kế hoạch)
> **Thời gian tự giải gợi ý:** 60 phút

---

## 1. Bối cảnh hệ thống

Một công ty bảo hiểm nhân thọ (minh họa) vận hành **cổng nghiệp vụ nội bộ** "PolicyPortal" từ 2014: đại lý nhập hồ sơ, thẩm định viên duyệt, bộ phận bồi thường xử lý claim, kế toán chạy đối soát cuối tháng.

```
          Đại lý / nhân viên (2.500 user nội bộ, ~180 đồng thời, đỉnh 400 vào cuối tháng)
                     │  HTTPS
              F5 load balancer (sticky session theo JSESSIONID)
                     │
    WebLogic 12c cluster (4 node, Java 8) ── policyportal.war (~1,1 triệu dòng Java)
        ├─ Module cũ (2014): Struts 1.3 + Tiles + DynaActionForm           ~ 420 action
        ├─ Module mới hơn (2017): Struts 2.3.x + Convention plugin + JSP    ~ 430 action
        ├─ Spring 3.2 (service, transaction), Hibernate 3.6 + JDBC thuần
        └─ 1.400 JSP (nhiều scriptlet), jQuery 1.x
                     │
    Oracle 19c (đã nâng từ 11g) ── 900 bảng, 610 package PL/SQL (tính phí, quy tắc thẩm định)
                     │
    Batch: cron + shell gọi Java main; file trao đổi với ngân hàng, cơ quan quản lý
```

---

## 2. Hiện trạng

| Khía cạnh | Thực trạng |
|---|---|
| Bảo mật | Pentest phát hiện ứng dụng dùng Struts 2.3.x (đã EOL), có `fileUpload` interceptor cũ; Struts 1 EOL từ 2013. Bản tin S2-066 (CVE-2023-50164) và S2-067 (CVE-2024-53677) ảnh hưởng luồng upload hồ sơ — WAF đang "vá ảo" |
| Kiểm toán | Đoàn kiểm toán CNTT yêu cầu kế hoạch loại bỏ thành phần EOL trong 18 tháng |
| Vận hành | Deploy 1 lần/tháng, tối Chủ Nhật, downtime 3 giờ; build bằng Ant + một số jar copy tay trong `WEB-INF/lib` |
| Chất lượng | Coverage unit test ~4%, không có test E2E tự động; tài liệu nghiệp vụ nằm trong đầu 3 người kỳ cựu |
| Hiệu năng | Màn hình tra cứu hợp đồng p95 4,2 s (N+1 Hibernate, OSIV) |
| Con người | 12 developer, 4 người thạo Struts, 8 người quen Spring Boot từ dự án khác |
| Nghiệp vụ ẩn | Quy tắc tính phí nằm rải rác: PL/SQL, Action Java, **scriptlet JSP**, và JavaScript phía client |

Thống kê truy cập 90 ngày (từ access log, được dựng thành dashboard trước khi lập kế hoạch):

```
Top 20 URL chiếm 71% request; 310/850 action có < 10 request/tháng; 96 action 0 request trong 90 ngày
Luồng "Tra cứu hợp đồng" 34%, "Nhập hồ sơ mới" 12%, "Duyệt thẩm định" 9%, "Claim" 7%, báo cáo 11%
```

---

## 3. Ràng buộc & câu hỏi đặt ra

**Ràng buộc**

1. Không được ngừng nghiệp vụ trong giờ hành chính; **đóng băng thay đổi** 5 ngày cuối tháng và 3 tuần cuối năm tài chính.
2. Giữ Oracle và các package PL/SQL (đội DBA sở hữu, có quy trình phê duyệt riêng); không đổi schema lõi trong năm đầu.
3. Đăng nhập qua Active Directory hiện có; mọi thao tác phải có audit trail (ai, lúc nào, giá trị trước/sau).
4. Ngân sách 18 tháng, không tăng người; vẫn phải làm tính năng nghiệp vụ mới (~30% công suất).
5. Hạ tầng đích: Kubernetes on-premise (đã có cho các dự án khác), Java 21, Spring Boot 3.

**Câu hỏi người phỏng vấn thường đặt**

- Bạn chọn big-bang, strangler hay nâng cấp tại chỗ? Vì sao?
- Hai hệ thống chung sống thế nào: routing, phiên đăng nhập, giao diện, dữ liệu?
- Làm sao đảm bảo màn hình mới **tính đúng như cũ**?
- Bảo mật trong 18 tháng chuyển đổi xử lý thế nào khi Struts vẫn chạy?
- Kế hoạch đội ngũ và cách đo tiến độ?

> ✋ **Dừng lại và tự phác kế hoạch trước** (một trang): các giai đoạn, tiêu chí chuyển giai đoạn, và điều kiện rollback cho mỗi luồng.

---

## 4. Phương án

| Phương án | Mô tả | Ưu | Nhược | Đánh giá |
|---|---|---|---|---|
| A. Big-bang rewrite | Viết lại toàn bộ trên Boot 3, cắt chuyển một lần | Kiến trúc sạch | 18 tháng không giao giá trị; nghiệp vụ ẩn bị bỏ sót; cắt chuyển rủi ro cực cao | ❌ |
| B. Nâng cấp tại chỗ | Struts 2 → 6.x, viết lại Struts 1 thành Struts 2 | Ít thay đổi kiến trúc | Vẫn kẹt `javax`, Java 8/11; vẫn là framework thu hẹp; Struts 1 vẫn phải viết lại | Chỉ làm **phần tối thiểu** như biện pháp an toàn tạm thời |
| C. Strangler cùng WAR | Thêm `DispatcherServlet` cho `/v2/*` vào WAR cũ | Chia sẻ session, service ngay | Bị khóa vào Spring 5 + `javax` + WebLogic; không lên Boot 3/Java 21 được | Phù hợp nếu đích là Spring 5 — không phải trường hợp này |
| **D. Strangler qua gateway** | Ứng dụng Boot 3 mới đứng cạnh, gateway route theo URL; chuyển từng luồng | Tự do công nghệ, giao giá trị dần, rollback từng luồng | Phải giải quyết SSO, UI đồng nhất, dùng chung DB | ✅ **Chọn** |

Quyết định kiến trúc đích: **modular monolith** Spring Boot 3 (một deployable `policy-platform` với module theo bounded context: `customer`, `policy`, `underwriting`, `claim`, `reporting`), không tách microservices ngay — đội 12 người, một DB Oracle dùng chung, ưu tiên giảm rủi ro hơn độc lập deploy. Ranh giới module được kiểm tra bằng ArchUnit/Spring Modulith để sau này tách được nếu cần.

```
                         ┌──────────────── Spring Boot 3 / Java 21 (K8s) ───────────────┐
                         │ policy-platform: /app/customers/**, /app/policies/**, ...    │
 User ─► F5 ─► Gateway ──┤        Spring Security OIDC ◄── Keycloak (federate AD)       │
   (Spring Cloud Gateway │                                                              │
    hoặc Nginx)          └──────────────────────────────────────────────────────────────┘
                         └──► (mặc định) WebLogic: policyportal.war (Struts 1/2)  ── cùng Oracle 19c
```

---

## 5. Kế hoạch triển khai từng bước

### Giai đoạn 0 (tháng 0–2) — Ổn định và an toàn trước

1. **Kiểm kê**: lập danh mục 850 action từ `struts-config.xml`/`struts.xml`/Convention (config-browser plugin ở môi trường dev), map với access log → biết luồng nào sống/chết. Xóa 96 action không dùng (sau khi xác nhận với nghiệp vụ, kể cả nghiệp vụ cuối năm).
2. **Giảm bề mặt tấn công Struts 2**: nâng lên nhánh 6.x (vẫn `javax`, chạy trên WebLogic 12c/Java 8), chuyển upload sang Action File Upload interceptor, cấu hình hardening:
   ```xml
   <constant name="struts.devMode" value="false"/>
   <constant name="struts.enable.DynamicMethodInvocation" value="false"/>
   <constant name="struts.mapper.alwaysSelectFullNamespace" value="false"/>
   <constant name="struts.ognl.expressionMaxLength" value="256"/>
   <constant name="struts.allowlist.enable" value="true"/>
   ```
   Gỡ plugin không dùng (REST/XStream, config-browser ở production).
3. **Struts 1**: không có bản vá → filter chặn tham số nguy hiểm (`class.*`, `*.class.*` — CVE-2014-0114), WAF rule, ưu tiên các module Struts 1 sớm trong lộ trình.
4. **Build tái lập được**: chuyển Ant → Maven, mọi jar lấy từ Nexus, bật SCA (OWASP Dependency-Check) trong CI, sinh SBOM.
5. **Observability**: correlation id ở F5/gateway, log tập trung, dashboard traffic theo action — chính là thước đo tiến độ sau này.

### Giai đoạn 1 (tháng 2–4) — Nền móng chung sống

**Gateway & feature flag theo route:**

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: customers-new
          uri: http://policy-platform:8080
          predicates:
            - Path=/app/customers/**
        - id: policy-search-canary            # luồng đang chuyển: chỉ nhóm thí điểm
          uri: http://policy-platform:8080
          predicates:
            - Path=/policy/search.action
            - Cookie=pp_pilot, true
          filters:
            - RewritePath=/policy/search.action, /app/policies/search
        - id: legacy-default
          uri: http://weblogic-cluster:7001
          predicates:
            - Path=/**
```

**Đăng nhập — không chia sẻ `HttpSession`, chia sẻ danh tính:** Keycloak federate với AD; ứng dụng mới là OIDC client (`spring-boot-starter-oauth2-client`); ứng dụng cũ được bổ sung filter OIDC (thư viện OIDC cho `javax` servlet) để cùng SSO. Người dùng đăng nhập một lần, hai ứng dụng có session riêng. Trạng thái nghiệp vụ dở dang (wizard nhập hồ sơ nhiều bước) **không** truyền qua session giữa hai hệ thống — mỗi luồng được chuyển trọn vẹn.

> Vì sao không dùng Spring Session + Redis chung? Hai bên khác phiên bản (`javax`/Spring 3.2 vs `jakarta`/Spring 6), attribute session là object Java serialize → thay đổi class ở một bên làm vỡ bên kia. Chia sẻ danh tính qua SSO đơn giản và an toàn hơn.

**UI đồng nhất:** layout Thymeleaf mới sao chép header/menu/CSS hiện có; menu trỏ URL theo cấu hình (một nguồn: bảng `menu_route` đọc bởi cả hai app) để chuyển luồng không cần sửa JSP.

**Dữ liệu:** ứng dụng mới dùng **cùng schema Oracle**, truy cập qua repository riêng (Spring Data JDBC/JPA) nhưng **gọi lại package PL/SQL** cho quy tắc tính phí thay vì viết lại:

```java
@Repository
class PremiumCalculator {
    private final SimpleJdbcCall call;
    PremiumCalculator(DataSource ds) {
        this.call = new SimpleJdbcCall(ds).withCatalogName("PKG_PREMIUM").withFunctionName("CALC_ANNUAL");
    }
    BigDecimal annualPremium(long productId, LocalDate dob, BigDecimal sumAssured) {
        return call.executeFunction(BigDecimal.class, Map.of(
                "P_PRODUCT_ID", productId, "P_DOB", dob, "P_SUM_ASSURED", sumAssured));
    }
}
```

Quy ước schema: mọi thay đổi DDL qua Flyway ở **một** repository `policy-db` (baseline từ schema hiện tại), chỉ được expand/contract tương thích với cả hai ứng dụng (xem [Case 17](17-zero-downtime-schema-migration.md)). Audit trail giữ bằng trigger hiện có, ứng dụng mới set `CLIENT_IDENTIFIER` (`DBMS_SESSION.SET_IDENTIFIER`) để trigger ghi đúng người dùng.

### Giai đoạn 2 (tháng 4–8) — Chuyển luồng đọc

Thứ tự theo **giá trị/rủi ro**: tra cứu hợp đồng (34% traffic, chỉ đọc, đang chậm) → tra cứu khách hàng → báo cáo.

**Lưới an toàn: characterization test + so sánh song song.**

```java
// Golden master ở mức HTTP: cùng input, so sánh dữ liệu nghiệp vụ trích từ HTML/JSON của hai hệ thống
@ParameterizedTest
@CsvFileSource(resources = "/golden/policy-search-cases.csv", numLinesToSkip = 1)
void search_matches_legacy(String policyNo, String customerId, String status) {
    var legacy = legacyClient.searchPolicies(policyNo, customerId, status);   // parse bảng kết quả JSP
    var modern = newClient.searchPolicies(policyNo, customerId, status);
    assertThat(modern).usingRecursiveComparison()
        .ignoringFields("rowCssClass")
        .isEqualTo(legacy);
}
```

Cho luồng đọc, gateway còn chạy **shadow traffic**: bản sao request đi tới app mới, response được so sánh bất đồng bộ (job so khác biệt), người dùng chỉ nhận response từ hệ thống cũ. Khác biệt được phân loại: bug mới / bug cũ người dùng đang dựa vào / khác biệt định dạng chấp nhận được.

Tiêu chí chuyển 100% một luồng: tỷ lệ khác biệt < 0,1% trong 2 tuần, p95 tốt hơn hoặc bằng, nhóm thí điểm (20 user) ký xác nhận, đã qua **một kỳ cuối tháng**.

### Giai đoạn 3 (tháng 8–15) — Chuyển luồng ghi theo bounded context

Thứ tự: Customer → Nhập hồ sơ (wizard, có upload — gỡ được rủi ro S2-066/067) → Thẩm định → Claim.

- Mỗi luồng ghi chuyển **trọn vẹn** (từ bước đầu tới bước cuối của wizard).
- Dữ liệu do luồng mới ghi phải hiển thị đúng ở màn hình cũ (còn chạy cho luồng khác) → test "ghi mới, đọc cũ".
- Validation mới **không chặt hơn** cũ cho dữ liệu lịch sử (dữ liệu bẩn sẵn có), nhưng chặt cho dữ liệu nhập mới; danh sách khác biệt validation được nghiệp vụ duyệt.
- Logic trong scriptlet JSP/JavaScript được trích ra thành rule có test trước khi viết lại.

### Giai đoạn 4 (tháng 15–18) — Tắt hệ thống cũ

Điều kiện: traffic về WebLogic = 0 trong một chu kỳ đầy đủ (gồm cuối tháng và các báo cáo quý); batch đã chuyển sang Spring Batch/K8s CronJob; WAR cũ giữ ở chế độ **chỉ đọc** 3 tháng, sau đó gỡ.

### Tổ chức đội

| Nhóm | Người | Trách nhiệm |
|---|---|---|
| Platform | 2 | Gateway, SSO, CI/CD, observability, template module, ArchUnit rule |
| Squad "Policy & Customer" | 5 (2 người thạo Struts + 3 người Boot) | Luồng tra cứu, khách hàng, nhập hồ sơ |
| Squad "Underwriting & Claim" | 4 | Thẩm định, claim |
| BA/QA nghiệp vụ | 1 + key user từ nghiệp vụ | Kịch bản characterization, ký nghiệm thu |

Pairing chéo người thạo Struts với người thạo Boot; mỗi luồng có ADR ngắn; 30% công suất dành cho yêu cầu nghiệp vụ mới — **chỉ viết trên stack mới** (quy tắc "không thêm action Struts mới").

---

## 6. Rủi ro & rollback

| Rủi ro | Xác suất / tác động | Giảm thiểu | Rollback |
|---|---|---|---|
| Nghiệp vụ ẩn bị bỏ sót (scriptlet, JS, PL/SQL) | Cao / Cao | Characterization test, shadow traffic, key user | Route luồng về hệ thống cũ (đổi config gateway, < 5 phút) |
| Dữ liệu do app mới ghi làm vỡ màn hình cũ | Trung bình / Cao | Test "ghi mới, đọc cũ"; không đổi ngữ nghĩa cột | Route về cũ + script sửa dữ liệu đã chuẩn bị trước |
| Khai thác CVE Struts trong thời gian chuyển đổi | Trung bình / Rất cao | Nâng 6.x, WAF, egress filtering, giám sát process con, ưu tiên luồng upload | Tắt chức năng upload cũ (flag), chuyển sang luồng mới |
| SSO lỗi → không ai đăng nhập được | Thấp / Rất cao | Keycloak HA, giữ đăng nhập AD trực tiếp của app cũ làm đường dự phòng 6 tháng | Bật lại form login cũ |
| Hiệu năng Oracle khi hai app cùng truy cập | Trung bình / Trung bình | Giới hạn pool từng app, AWR so sánh trước/sau | Giảm pool/route |
| Rơi vào kỳ đóng băng cuối tháng/năm | Chắc chắn / Trung bình | Lịch chuyển luồng tránh các tuần đó | — |
| Đội bị kéo sang làm tính năng mới | Cao / Cao | Tỷ lệ 70/30 cam kết với ban điều hành, báo cáo tiến độ bằng % traffic | — |
| "Chuyển dở dang" kéo dài vô hạn (hai hệ thống mãi mãi) | Trung bình / Cao | Mốc tắt từng module cũ trong roadmap, gỡ code cũ ngay khi luồng đạt 100% | — |

Nguyên tắc rollback: **rollback bằng routing, không bằng dữ liệu**. Mỗi luồng giữ code cũ chạy được tới khi luồng mới qua một chu kỳ nghiệp vụ đầy đủ; thay đổi schema luôn tương thích ngược để quay về không cần migrate ngược.

---

## 7. Phòng ngừa & đo lường thành công

**Chỉ số theo dõi (dashboard hằng tuần cho ban điều hành)**

- % request đi qua stack mới (mục tiêu theo quý: 30% → 60% → 90% → 100%).
- Số action Struts còn lại, số CVE mở trên thành phần cũ.
- Tỷ lệ khác biệt shadow traffic theo luồng; số sự cố do migration.
- Lead time deploy (từ 1 lần/tháng → nhiều lần/tuần cho stack mới), p95 các màn hình chính.

**Hàng rào kỹ thuật**

- ArchUnit/Spring Modulith: module không truy cập bảng/package của module khác trực tiếp; không có code mới phụ thuộc `org.apache.struts`.
- CI chặn build nếu thêm entry vào `struts.xml`.
- SCA + SBOM cho cả hai ứng dụng; SLA vá Critical 72 giờ.
- Runbook chuyển/rollback luồng được diễn tập trên staging trước mỗi lần chuyển.

**Bài học quy trình:** quyết định "luồng nào chuyển tiếp theo" dựa trên dữ liệu traffic và rủi ro, không dựa trên module nào dễ code nhất.

---

## 8. Cách kể lại trong phỏng vấn (STAR)

- **Situation:** "Cổng nghiệp vụ của công ty bảo hiểm chạy Struts 1 và Struts 2.3 trên WebLogic, 1,1 triệu dòng, 850 action, Oracle với 600 package PL/SQL. Kiểm toán yêu cầu loại bỏ thành phần EOL trong 18 tháng, trong khi luồng upload đang bị ảnh hưởng bởi CVE Struts."
- **Task:** "Mình là technical lead, chịu trách nhiệm chọn chiến lược, lập kế hoạch và dẫn 12 developer chuyển đổi mà không ngừng nghiệp vụ."
- **Action:** "Hai tháng đầu mình ưu tiên an toàn: kiểm kê action bằng access log, xóa 96 action chết, nâng Struts 2 lên 6.x, hardening OGNL, chuyển build sang Maven có SCA. Sau đó chọn strangler qua gateway với một modular monolith Boot 3; chia sẻ danh tính qua Keycloak thay vì chia sẻ session; dùng chung Oracle và gọi lại PL/SQL tính phí thay vì viết lại. Mỗi luồng có characterization test, shadow traffic so sánh kết quả, nhóm thí điểm qua một kỳ cuối tháng, và rollback bằng một dòng config gateway. Thứ tự chuyển dựa trên traffic và rủi ro: tra cứu trước, upload hồ sơ sớm để gỡ CVE."
- **Result:** "Sau 17 tháng, 100% traffic qua stack mới, WAR cũ ở chế độ chỉ đọc rồi gỡ; có 3 lần rollback luồng, mỗi lần dưới 5 phút, không mất dữ liệu. Tra cứu hợp đồng p95 từ 4,2 giây xuống 600 ms, deploy từ một lần/tháng lên 3 lần/tuần. Bài học lớn nhất: rủi ro nằm ở nghiệp vụ ẩn chứ không phải framework — đầu tư vào test so sánh với hệ thống cũ là quyết định đúng nhất."

---

## 9. Câu hỏi mở rộng

<details>
<summary>1. Vì sao không tách microservices ngay khi đã viết lại?</summary>

Một DB Oracle dùng chung với 900 bảng và PL/SQL đan xen — tách service mà không tách dữ liệu chỉ tạo "distributed monolith" (deploy độc lập trên giấy, coupling qua DB trên thực tế) cộng thêm chi phí mạng, observability, transaction phân tán. Modular monolith với ranh giới được kiểm tra tự động giữ được lựa chọn tách sau này khi có lý do rõ ràng (tải, đội, nhịp release khác nhau).
</details>

<details>
<summary>2. Nếu bắt buộc phải chia sẻ `HttpSession` giữa hai ứng dụng thì làm thế nào?</summary>

Spring Session (JDBC hoặc Redis) ở cả hai phía với **cùng cookie name/domain/path**, và attribute được serialize dạng JSON với schema cố định (cấu hình serializer JSON cho cả hai) thay vì Java serialization. Chỉ đặt vào session dữ liệu tối thiểu (định danh người dùng, ngôn ngữ). Phía legacy cần phiên bản Spring Session tương thích `javax`; hai phiên bản khác nhau phải đọc cùng định dạng — phải có test tích hợp chéo.
</details>

<details>
<summary>3. Làm sao phát hiện nghiệp vụ ẩn trong JSP scriptlet?</summary>

Quét tĩnh JSP tìm `<% ... %>` có phép tính/điều kiện nghiệp vụ, gọi DAO hay service; phỏng vấn key user với kịch bản thực tế; so sánh output của hai hệ thống trên dữ liệu production (shadow traffic, golden master). Mỗi rule tìm được viết thành test trước khi chuyển sang Java service.
</details>

<details>
<summary>4. Viết lại PL/SQL sang Java có nên làm trong dự án này không?</summary>

Không trong 18 tháng đầu: PL/SQL tính phí là logic được kiểm định, có đội DBA sở hữu, và chạy gần dữ liệu. Rủi ro viết lại cao, giá trị chưa rõ. Bọc nó sau một interface (`PremiumCalculator`) để sau này thay thế từng phần nếu cần (ví dụ để test dễ hơn hoặc chuyển DB), đo thời gian gọi và giữ bind variables.
</details>

<details>
<summary>5. Bạn thuyết phục ban điều hành chấp nhận 18 tháng thế nào?</summary>

Trình bày bằng rủi ro và chi phí: rủi ro bảo mật cụ thể (CVE đang mở, kiểm toán), chi phí vận hành (downtime 3 giờ/tháng, deploy chậm), rủi ro con người (tri thức trong đầu 3 người). Kế hoạch giao giá trị **mỗi quý** (màn hình tra cứu nhanh hơn, upload an toàn), đo bằng % traffic — không phải "18 tháng sau mới thấy". Có điểm dừng an toàn ở mỗi giai đoạn.
</details>

<details>
<summary>6. Nếu sau 6 tháng phát hiện tốc độ chuyển đổi chỉ bằng một nửa kế hoạch?</summary>

Xem lại dữ liệu: luồng nào tốn thời gian, vì sao (nghiệp vụ ẩn, chờ nghiệm thu, môi trường)? Cắt phạm vi: các action ít dùng có thể **gộp/bỏ** sau khi thống nhất với nghiệp vụ thay vì chuyển 1:1; báo cáo ít dùng chuyển sang công cụ BI. Bảo vệ tỷ lệ công suất 70/30. Báo sớm cho stakeholder kèm phương án, không đợi đến hạn.
</details>

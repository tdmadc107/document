# Module 10 — Apache Struts (1 & 2) và hệ thống legacy

> **Mục tiêu:** sau module này bạn giải thích được kiến trúc MVC Model 2 và luồng xử lý request của Struts 1 lẫn Struts 2, đọc hiểu và bảo trì được một codebase Struts cũ (struts-config.xml, struts.xml, ActionForm, interceptor, OGNL, Tiles), nhận diện được rủi ro bảo mật kinh điển (OGNL injection, file upload, ClassLoader manipulation) và hardening được hệ thống, đồng thời lập được kế hoạch migration từng bước sang Spring MVC / Spring Boot theo mô hình strangler fig.
> **Cấp độ:** Cơ bản → Nâng cao (Senior)
> **Thời lượng gợi ý:** 4 ngày (≈ 20–24 giờ)
> **Yêu cầu trước:** Servlet/JSP (Filter, Servlet lifecycle, HttpSession), Module Spring Core & Spring MVC, Module 09 (JPA/Hibernate & Transactions).
> **Nguồn tham khảo:**
> - Trong kho: [`Ebook IT/Design Pattern.pdf`](../../Ebook%20IT/Design%20Pattern.pdf) — các pattern Struts dùng: Front Controller (tư tưởng), Command (Action), Chain of Responsibility (interceptor stack), Proxy, Template Method (RequestProcessor).
> - Ngoài:
>   - Apache Struts 2 documentation — https://struts.apache.org/core-developers/ , https://struts.apache.org/getting-started/
>   - Struts security bulletins (S2-001 … S2-0xx) — https://cwiki.apache.org/confluence/display/WW/Security+Bulletins
>   - Struts security guide — https://struts.apache.org/security/
>   - Struts 1 EOL announcement — https://struts.apache.org/struts1eol-announcement.html
>   - NVD: CVE-2017-5638, CVE-2018-11776, CVE-2023-50164, CVE-2024-53677, CVE-2014-0114 — https://nvd.nist.gov
>   - Martin Fowler, *StranglerFigApplication* — https://martinfowler.com/bliki/StranglerFigApplication.html
>   - Spring Framework Reference — Web MVC — https://docs.spring.io/spring-framework/reference/web/webmvc.html

## Mục lục
1. [Vì sao Senior ở Việt Nam vẫn gặp Struts](#1-boi-canh)
2. [MVC Model 2 và Front Controller](#2-model2)
3. [Kiến trúc Struts 1](#3-struts1)
4. [Kiến trúc Struts 2: filter, ActionProxy, interceptor](#4-struts2-core)
5. [Struts 2: ValueStack, OGNL, results, cấu hình](#5-struts2-ognl)
6. [Struts 2: validation, i18n, file upload, double submit](#6-struts2-features)
7. [Tích hợp Spring và Hibernate](#7-integration)
8. [Lịch sử bảo mật và hardening](#8-security)
9. [Struts 2 vs Spring MVC](#9-compare)
10. [Chiến lược migration sang Spring MVC / Spring Boot](#10-migration)
11. [Dự án mini của module](#du-an-mini)
12. [Checklist tự đánh giá](#checklist-tu-danh-gia)

---

<a id="1-boi-canh"></a>
## 1. Vì sao Senior ở Việt Nam vẫn gặp Struts

### 1.1 Bối cảnh
Struts 1 ra đời năm 2000 (Craig McClanahan, dự án Jakarta của Apache) và trở thành framework web Java "mặc định" của giai đoạn 2002–2010. Struts 2 (2007) là sự hợp nhất của WebWork 2 vào thương hiệu Struts — **kiến trúc khác hoàn toàn** Struts 1, chỉ chung cái tên.

Ở Việt Nam, Struts xuất hiện nhiều trong:
- **Ngân hàng, bảo hiểm, chứng khoán:** core banking front-end, hệ thống tín dụng, quản lý hợp đồng xây dựng từ 2005–2015, thường do vendor hoặc outsourcing Nhật Bản phát triển.
- **Khối chính phủ, doanh nghiệp nhà nước:** dịch vụ công, quản lý thuế/hải quan đời đầu, phần mềm nội bộ.
- **Dự án offshore Nhật:** nhiều hệ thống của khách hàng Nhật chạy Struts 1.x + JSP + Oracle hàng chục năm; dự án "maintenance" và "migration" vẫn được giao cho các công ty Việt Nam.

Vì sao chúng còn sống: hệ thống chạy ổn định, logic nghiệp vụ dày đặc và **không có tài liệu**, chi phí viết lại cao, rủi ro nghiệp vụ lớn, quy trình kiểm định (audit, ngân hàng nhà nước) nặng nề.

### 1.2 Senior cần gì ở mảng này
- Đọc hiểu nhanh luồng request trong codebase lạ (từ URL → config → Action → JSP).
- Sửa lỗi mà không phá hệ thống (thread-safety, session, encoding tiếng Việt, validation).
- **Đánh giá rủi ro bảo mật** — Struts có lịch sử RCE nghiêm trọng; một hệ thống ngân hàng chạy Struts 2.3 chưa vá là rủi ro ở cấp độ ban điều hành.
- **Lập kế hoạch hiện đại hóa** — thuyết phục stakeholder bằng phương án từng bước, đo lường được, có thể rollback.

> 💡 **Góc nhìn Senior:** khi phỏng vấn ở công ty có hệ thống legacy, câu trả lời "viết lại toàn bộ bằng Spring Boot + microservices" thường là **điểm trừ**. Nhà tuyển dụng muốn nghe: đánh giá hiện trạng, vá bảo mật trước, viết test đặc tả hành vi (characterization tests), tách dần từng luồng (strangler fig), đo lường và giảm rủi ro.

### 🛠 Bài tập phần 1

**Bài 1.1 — Khảo sát hiện trạng (Cơ bản)**
- Đề bài: cho một codebase legacy (tự tạo hoặc lấy một dự án Struts mẫu trên GitHub), lập bảng kiểm kê: phiên bản Struts, Java, application server, số Action, số JSP, plugin dùng (Tiles, Validator, Spring, REST, Convention), thư viện có CVE.
- Tiêu chí đạt: bảng kiểm kê đầy đủ; chạy OWASP Dependency-Check (hoặc tương đương) và liệt kê CVE mức Critical/High.

**Bài 1.2 — Bản ghi nhớ cho ban lãnh đạo (Trung bình)**
- Đề bài: viết memo 1 trang giải thích rủi ro của việc tiếp tục chạy Struts 1.3 (EOL) cho người không chuyên kỹ thuật.
- Tiêu chí đạt: nêu rủi ro (bảo mật, tuyển dụng, tương thích Java/server), chi phí của việc không làm gì, 3 phương án với ước lượng tương đối và khuyến nghị.

<details>
<summary>Gợi ý lời giải</summary>

- 1.1: đếm Action bằng `grep -c "<action " struts-config.xml` / `struts.xml`; với Convention plugin tìm class kết thúc bằng `Action`. Dependency-Check: `mvn org.owasp:dependency-check-maven:check`.
- 1.2: ba phương án điển hình: (A) giữ nguyên + WAF + cô lập mạng (rẻ, rủi ro còn), (B) nâng cấp lên Struts 2 bản mới nhất (vẫn là viết lại tầng web vì Struts 1 → 2 khác kiến trúc), (C) strangler sang Spring Boot theo từng nghiệp vụ (đầu tư lớn nhất, giảm rủi ro lâu dài).

</details>

---

<a id="2-model2"></a>
## 2. MVC Model 2 và Front Controller

### 2.1 Model 1 vs Model 2
- **Model 1:** request đi thẳng vào JSP; JSP vừa xử lý logic (scriptlet `<% %>`), vừa gọi DB, vừa render. Nhanh làm, nhưng không bảo trì được.
- **Model 2 (MVC trên web):** một **controller servlet** nhận request, gọi model (business logic), chọn view (JSP) để render. JSP chỉ hiển thị.

```
Browser ──► Front Controller (Servlet/Filter) ──► Action/Command (Controller logic)
                                                    │  gọi Service/DAO (Model)
                                                    ▼
                                    chọn view logic name ("success")
                                                    │
                    JSP/Template (View) ◄── forward/redirect ──┘
```

**Front Controller pattern**: một điểm vào duy nhất xử lý các mối quan tâm chung (định tuyến, i18n, bảo mật, validation, binding) rồi ủy quyền cho handler cụ thể (Command pattern). Struts 1 (ActionServlet), Struts 2 (StrutsPrepareAndExecuteFilter), Spring MVC (DispatcherServlet) đều là hiện thực của pattern này.

### 2.2 Forward vs redirect — kiến thức nền bắt buộc
| | Forward (`RequestDispatcher.forward`) | Redirect (`sendRedirect`, HTTP 302/303) |
|---|---|---|
| Round-trip | Không — xử lý trong server | Có — browser gửi request mới |
| URL trên trình duyệt | Không đổi | Đổi |
| Request attributes | Giữ nguyên | Mất (cần session/flash) |
| Dùng khi | Hiển thị view | Sau POST thành công (**PRG — Post/Redirect/Get**) để tránh submit lại khi F5 |

### 🛠 Bài tập phần 2

**Bài 2.1 — Front controller tối giản (Cơ bản)**
- Đề bài: viết một servlet `FrontController` map `*.do`, đọc map `path → Command` từ file properties, mỗi `Command` trả về tên view, controller forward tới `/WEB-INF/jsp/<view>.jsp`.
- Tiêu chí đạt: thêm một chức năng mới chỉ cần thêm một class Command và một dòng cấu hình; JSP đặt trong `WEB-INF` không truy cập trực tiếp được.

**Bài 2.2 — PRG (Trung bình)**
- Đề bài: thêm form tạo sản phẩm; sau POST thành công redirect về trang danh sách với thông báo "Tạo thành công" (flash qua session, hiển thị một lần).
- Tiêu chí đạt: F5 sau khi tạo không tạo bản ghi trùng; thông báo không xuất hiện lại khi tải lại trang.

<details>
<summary>Gợi ý lời giải</summary>

```java
public interface Command { String execute(HttpServletRequest req, HttpServletResponse resp) throws Exception; }

public class FrontController extends HttpServlet {
    private final Map<String, Command> commands = new HashMap<>();
    @Override public void init() { /* load properties: /product/list.do=com.x.ListProductCommand ... */ }
    @Override protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getServletPath();
        Command cmd = commands.get(path);
        if (cmd == null) { resp.sendError(404); return; }
        try {
            String view = cmd.execute(req, resp);
            if (view.startsWith("redirect:")) resp.sendRedirect(req.getContextPath() + view.substring(9));
            else req.getRequestDispatcher("/WEB-INF/jsp/" + view + ".jsp").forward(req, resp);
        } catch (Exception e) { throw new ServletException(e); }
    }
}
```
Flash: lưu message vào session, trang danh sách đọc rồi `session.removeAttribute`.

</details>

---

<a id="3-struts1"></a>
## 3. Kiến trúc Struts 1

### 3.1 Các thành phần
| Thành phần | Vai trò |
|---|---|
| `ActionServlet` | Front controller, khai báo trong `web.xml`, thường map `*.do`. Khởi tạo cấu hình từ `struts-config.xml`. |
| `RequestProcessor` | Xử lý từng request theo chuỗi bước cố định (Template Method). Có thể kế thừa để chèn logic chung (ví dụ kiểm tra đăng nhập). Struts 1.3 chuyển sang `ComposableRequestProcessor` dựa trên Commons Chain. |
| `Action` | Lớp xử lý (Command), method `execute(...)` trả về `ActionForward`. |
| `ActionForm` | JavaBean chứa dữ liệu form, được populate từ request parameter; có `validate()` và `reset()`. Bắt buộc kế thừa `ActionForm` (không phải POJO). |
| `ActionMapping` | Cấu hình của một action (path, type, name form, scope, input, forwards). |
| `ActionForward` | Tên logic → đường dẫn view (forward hoặc redirect). |
| `struts-config.xml` | Khai báo form-beans, action-mappings, global-forwards, global-exceptions, message-resources, plug-in. |
| Tiles | Layout template (header/menu/body/footer) qua `tiles-defs.xml`. |
| Validator | Validation khai báo qua `validator-rules.xml` + `validation.xml`, sinh cả JavaScript phía client. |

### 3.2 Luồng request
```
GET /product/save.do
  └─► ActionServlet.process()
        └─► RequestProcessor.process():
              processPath          → "/product/save"
              processLocale        → Locale vào session
              processContent, processNoCache
              processPreprocess    → hook (mặc định true)
              processMapping       → tìm ActionMapping
              processRoles         → kiểm tra role (nếu khai báo roles)
              processActionForm    → lấy/tạo ActionForm theo scope (request/session)
              processPopulate      → form.reset(); BeanUtils populate parameter vào form
              processValidate      → nếu validate="true": form.validate(); lỗi → forward về "input"
              processForward/Include
              processActionCreate  → lấy Action instance (CACHE — một instance duy nhất)
              processActionPerform → action.execute(mapping, form, req, resp)
              processForwardConfig → forward/redirect theo ActionForward
```

### 3.3 Ví dụ cấu hình và code
```xml
<!-- web.xml -->
<servlet>
  <servlet-name>action</servlet-name>
  <servlet-class>org.apache.struts.action.ActionServlet</servlet-class>
  <init-param><param-name>config</param-name><param-value>/WEB-INF/struts-config.xml</param-value></init-param>
  <load-on-startup>1</load-on-startup>
</servlet>
<servlet-mapping><servlet-name>action</servlet-name><url-pattern>*.do</url-pattern></servlet-mapping>
```
```xml
<!-- struts-config.xml -->
<struts-config>
  <form-beans>
    <form-bean name="productForm" type="com.shop.web.ProductForm"/>
  </form-beans>
  <global-forwards>
    <forward name="login" path="/login.do" redirect="true"/>
  </global-forwards>
  <action-mappings>
    <action path="/product/save" type="com.shop.web.SaveProductAction"
            name="productForm" scope="request" validate="true"
            input="/WEB-INF/jsp/product/edit.jsp">
      <forward name="success" path="/product/list.do" redirect="true"/>
    </action>
  </action-mappings>
  <message-resources parameter="MessageResources"/>
  <plug-in className="org.apache.struts.validator.ValidatorPlugIn">
    <set-property property="pathnames" value="/WEB-INF/validator-rules.xml,/WEB-INF/validation.xml"/>
  </plug-in>
</struts-config>
```
```java
public class ProductForm extends ActionForm {
    private String name;
    private String price;   // ActionForm thường dùng String cho mọi field để giữ lại input sai khi hiển thị lỗi
    // getters/setters...

    @Override
    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        ActionErrors errors = new ActionErrors();
        if (name == null || name.isBlank()) errors.add("name", new ActionMessage("error.name.required"));
        try { new BigDecimal(price); } catch (Exception e) { errors.add("price", new ActionMessage("error.price.invalid")); }
        return errors;
    }
}

public class SaveProductAction extends Action {
    // ❌ KHÔNG được có field mang trạng thái của request — Action là singleton
    private final ProductService productService = ServiceLocator.productService();

    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form,
                                 HttpServletRequest request, HttpServletResponse response) throws Exception {
        ProductForm f = (ProductForm) form;
        productService.create(f.getName(), new BigDecimal(f.getPrice()));
        return mapping.findForward("success");
    }
}
```
Biến thể thường gặp: `DispatchAction` (nhiều method trong một Action, chọn theo tham số `method=save`), `LookupDispatchAction`, `MappingDispatchAction`, `DynaActionForm` (form khai báo trong XML, không cần class).

### 3.4 Thread-safety của Action singleton
`RequestProcessor` tạo **một instance cho mỗi class Action** và dùng chung cho mọi request (giống Servlet). Mọi field instance đều bị nhiều thread truy cập đồng thời.

```java
public class CheckoutAction extends Action {
    private Cart cart;                 // ❌ race condition: user A thấy giỏ hàng của user B
    private SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy"); // ❌ SimpleDateFormat không thread-safe

    public ActionForward execute(...) {
        cart = (Cart) request.getSession().getAttribute("cart");
        ...
    }
}
```
Quy tắc: chỉ giữ dependency **stateless, thread-safe** (service) làm field; dữ liệu theo request đặt trong biến cục bộ, request hoặc session. Thay `SimpleDateFormat` bằng `DateTimeFormatter` (immutable).

> ⚠️ **Lỗi thường gặp:** `ActionForm` scope `session` (mặc định của Struts 1 nếu không khai báo `scope`!) → form cũ còn dữ liệu của lần submit trước, checkbox không tick không được gửi lên nên giá trị cũ "dính" lại (phải xử lý trong `reset()`), và session phình to. Đặt `scope="request"` trừ khi thật sự cần wizard nhiều bước.

> ⚠️ **Mã hóa tiếng Việt:** form POST bị lỗi font khi container đọc parameter theo ISO-8859-1. Cần Filter `request.setCharacterEncoding("UTF-8")` **đặt trước** mọi thứ đọc parameter (trước ActionServlet), JSP `pageEncoding="UTF-8"`, và với GET thì cấu hình `URIEncoding="UTF-8"` trên Tomcat connector (Tomcat 8+ mặc định UTF-8).

### 3.5 Tiles và Validator
```xml
<!-- tiles-defs.xml -->
<tiles-definitions>
  <definition name="base.layout" path="/WEB-INF/jsp/layout/main.jsp">
    <put name="header" value="/WEB-INF/jsp/layout/header.jsp"/>
    <put name="body"   value=""/>
  </definition>
  <definition name="product.list" extends="base.layout">
    <put name="body" value="/WEB-INF/jsp/product/list.jsp"/>
  </definition>
</tiles-definitions>
```
Forward có thể trỏ tới tên definition (`path="product.list"`) khi dùng `TilesRequestProcessor`/`TilesPlugin`.

```xml
<!-- validation.xml (form phải extends ValidatorForm) -->
<formset>
  <form name="productForm">
    <field property="name" depends="required,maxlength">
      <arg key="product.name"/>
      <var><var-name>maxlength</var-name><var-value>100</var-value></var>
    </field>
  </form>
</formset>
```

### 3.6 Struts 1 EOL
Apache công bố Struts 1 **End-of-Life tháng 4/2013**; bản cuối là 1.3.10 (2008). Không còn bản vá bảo mật chính thức. Ví dụ CVE-2014-0114: tham số `class.classLoader...` được Commons BeanUtils populate vào `ActionForm` → thao túng ClassLoader, có thể dẫn tới RCE trên một số môi trường. Cách giảm thiểu cho hệ thống không thể migrate ngay: Filter chặn parameter có tên khớp `(^|\W)[cC]lass\W` (và `.class.`), cập nhật Commons BeanUtils có `SuppressPropertiesBeanIntrospector`, đặt sau WAF.

> 💡 **Góc nhìn Senior:** không có đường "nâng cấp" từ Struts 1 lên Struts 2 — đó là viết lại tầng web. Khi đã phải viết lại, mục tiêu hợp lý hơn thường là Spring MVC/Spring Boot (xem phần 10). Có các fork cộng đồng vá Struts 1 cho Jakarta EE, nhưng cần đánh giá kỹ độ tin cậy và hỗ trợ dài hạn trước khi dùng cho hệ thống tài chính.

### 🛠 Bài tập phần 3

**Bài 3.1 — Đọc luồng Struts 1 (Cơ bản)**
- Đề bài: cho `struts-config.xml` có 10 action, vẽ sơ đồ luồng màn hình (URL → Action → forward → JSP) cho chức năng "tạo hợp đồng".
- Tiêu chí đạt: sơ đồ đúng cả nhánh validate lỗi (`input`) và global-forward; chỉ ra form nào đang scope session.

**Bài 3.2 — Sửa lỗi thread-safety (Trung bình)**
- Đề bài: viết một Action có field `List<String> messages` và `SimpleDateFormat`; viết test bắn 100 request đồng thời (JMeter hoặc test đa luồng gọi `execute` với mock request) để tái hiện dữ liệu lẫn giữa người dùng.
- Tiêu chí đạt: tái hiện được lỗi; sửa xong chạy lại không lỗi; giải thích vì sao.

**Bài 3.3 — Chặn ClassLoader manipulation (Nâng cao)**
- Đề bài: viết `SecurityParamFilter` cho Struts 1 chặn mọi parameter nguy hiểm (`class.`, `.class.`, `Class[`...), log cảnh báo và trả 400.
- Tiêu chí đạt: test với `?class.classLoader.resources.dirContext.docBase=...` bị chặn; parameter hợp lệ như `classification` không bị chặn nhầm.

<details>
<summary>Gợi ý lời giải</summary>

- 3.3:
```java
private static final Pattern BAD = Pattern.compile("(^|\\W)[cC]lass\\W");
public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
    for (String name : Collections.list(req.getParameterNames())) {
        if (BAD.matcher(name).find()) {
            log.warn("Blocked suspicious parameter {} from {}", name, req.getRemoteAddr());
            ((HttpServletResponse) res).sendError(400); return;
        }
    }
    chain.doFilter(req, res);
}
```
`classification` không khớp vì sau `class` là chữ cái (`\w`), không phải ký tự phân cách.

</details>

---

<a id="4-struts2-core"></a>
## 4. Kiến trúc Struts 2: filter, ActionProxy, interceptor

### 4.1 Thay đổi tư duy so với Struts 1
| Struts 1 | Struts 2 |
|---|---|
| Front controller là Servlet (`ActionServlet`) | Front controller là **Filter** (`StrutsPrepareAndExecuteFilter`; `FilterDispatcher` cũ đã deprecated từ 2.1.3 và bị loại bỏ) |
| Action singleton → phải stateless | **Action mới cho mỗi request** → field là dữ liệu của request, an toàn |
| Action phụ thuộc Servlet API (`execute(mapping, form, req, resp)`) | Action là POJO, `execute()` không tham số trả `String`; Servlet API lấy qua `*Aware` hoặc `ServletActionContext` khi cần → dễ test |
| `ActionForm` riêng | Dữ liệu form là **property của chính Action** (hoặc model qua `ModelDriven`) |
| JSP + taglib/JSTL EL | OGNL + ValueStack |
| `RequestProcessor` cố định | **Interceptor stack** cấu hình được (Chain of Responsibility) |

### 4.2 Luồng request
```
HTTP request
  └─► StrutsPrepareAndExecuteFilter
        ├─ prepare: tạo ActionContext, ValueStack, encoding, locale; ActionMapper → ActionMapping (namespace, name, method)
        └─ execute: Dispatcher.serviceAction()
              └─► ActionProxy (tạo từ ConfigurationManager — struts.xml/annotations)
                    └─► ActionInvocation.invoke()
                          ├─ Interceptor 1 (exception) ─┐
                          ├─ Interceptor 2 (i18n)       │ mỗi interceptor gọi invocation.invoke()
                          ├─ ...                        │ để chuyển cho phần tử kế tiếp
                          ├─ params → set request params vào Action qua OGNL
                          ├─ validation, workflow (có lỗi → trả "input", không gọi Action)
                          ├─► Action.execute() → "success"
                          ├─ Result (dispatcher → JSP, redirectAction, stream, json...) được thực thi
                          └─ các interceptor chạy phần "sau" theo thứ tự ngược lại
```
Ba đối tượng then chốt:
- **ActionProxy**: lớp trung gian giữa framework và Action (Proxy pattern); có thể dùng để gọi action từ ngoài HTTP (test).
- **ActionInvocation**: giữ trạng thái thực thi — action, interceptor iterator, result code; mỗi interceptor nhận nó trong `intercept(ActionInvocation)`.
- **ActionContext**: container theo thread (ThreadLocal) chứa ValueStack, parameters, session, application.

### 4.3 Interceptor
Hầu hết tính năng của Struts 2 (binding, validation, upload, i18n, token...) là interceptor. `defaultStack` (trong `struts-default.xml`) gồm theo thứ tự, ví dụ: `exception`, `alias`, `servletConfig`, `i18n`, `prepare`, `chain`, `modelDriven`, `fileUpload`/`actionFileUpload`, `checkbox`, `datetime`, `multiselect`, `staticParams`, `actionMappingParams`, `params`, `conversionError`, `validation`, `workflow`, … (danh sách cụ thể khác nhau theo phiên bản).

```java
public class AuthInterceptor extends AbstractInterceptor {
    @Override
    public String intercept(ActionInvocation invocation) throws Exception {
        Map<String, Object> session = invocation.getInvocationContext().getSession();
        if (session.get("user") == null) {
            return "login";               // dừng chuỗi, Action không được gọi
        }
        long start = System.nanoTime();
        try {
            return invocation.invoke();   // gọi interceptor kế tiếp / Action / Result
        } finally {
            log.info("{} took {} ms", invocation.getProxy().getActionName(), (System.nanoTime() - start) / 1_000_000);
        }
    }
}
```
```xml
<package name="secure" namespace="/admin" extends="struts-default">
  <interceptors>
    <interceptor name="auth" class="com.shop.web.AuthInterceptor"/>
    <interceptor-stack name="secureStack">
      <interceptor-ref name="auth"/>
      <interceptor-ref name="defaultStack"/>
    </interceptor-stack>
  </interceptors>
  <default-interceptor-ref name="secureStack"/>
  <global-results>
    <result name="login" type="redirectAction">
      <param name="actionName">login</param><param name="namespace">/</param>
    </result>
  </global-results>
  ...
</package>
```
Interceptor là **singleton** (một instance cho mỗi khai báo) → phải thread-safe, giống Action của Struts 1.

### 4.4 Action per request, `ActionSupport`, `ModelDriven`, `Preparable`
```java
public class ProductAction extends ActionSupport implements Preparable, ModelDriven<Product> {
    private Long id;
    private Product product = new Product();
    private List<Product> products;
    private final ProductService productService;    // inject bởi Spring (phần 7)

    public ProductAction(ProductService productService) { this.productService = productService; }

    @Override public void prepare() {               // Preparable: chạy trước params (theo defaultStack, nên dùng paramsPrepareParamsStack khi cần id)
        if (id != null) product = productService.find(id);
    }
    @Override public Product getModel() { return product; }   // ModelDriven: params bind vào product thay vì action

    public String list()  { products = productService.findAll(); return SUCCESS; }
    public String input() { return INPUT; }
    public String save()  { productService.save(product); addActionMessage(getText("product.saved")); return SUCCESS; }

    public void setId(Long id) { this.id = id; }
    public List<Product> getProducts() { return products; }
}
```
Hằng số kết quả: `SUCCESS`, `INPUT`, `ERROR`, `LOGIN`, `NONE`. `ActionSupport` cung cấp sẵn `validate()`, `addFieldError`, `addActionError`, `getText` (i18n).

> ⚠️ **Lỗi thường gặp:** vì `params` interceptor bind **mọi** parameter khớp setter/getter-chain, một setter vô tình (ví dụ `setRole`, hay getter trả về entity `getUser().setAdmin(...)` qua `user.admin=true`) trở thành lỗ hổng **mass assignment**. Giới hạn bằng `ParameterNameAware`/`excludeParams`/`acceptParamNames` hoặc `@StrutsParameter` (Struts 6.4+, bắt buộc theo mặc định ở Struts 7) — xem phần 8.

### 🛠 Bài tập phần 4

**Bài 4.1 — Hello Struts 2 (Cơ bản)**
- Đề bài: tạo project Maven Struts 2 (bản mới nhất bạn có thể dùng) với một action `hello` nhận `name` và hiển thị lời chào qua JSP.
- Tiêu chí đạt: filter khai báo đúng trong `web.xml`; `struts.devMode=true` chỉ ở profile dev; unit test gọi trực tiếp `execute()` không cần container.

**Bài 4.2 — Interceptor đo thời gian và kiểm tra đăng nhập (Trung bình)**
- Đề bài: viết `AuthInterceptor` + `TimingInterceptor`, áp dụng cho namespace `/admin`.
- Tiêu chí đạt: chưa đăng nhập → redirect `login`; log thời gian xử lý mỗi action; interceptor không có field mang trạng thái request.

**Bài 4.3 — Đọc mã nguồn ActionInvocation (Nâng cao)**
- Đề bài: đọc `DefaultActionInvocation.invoke()` trong mã nguồn Struts 2, giải thích cơ chế đệ quy gọi interceptor và lý do result được thực thi **trước khi** các interceptor "unwind".
- Tiêu chí đạt: sơ đồ sequence; chỉ ra hệ quả: interceptor không thể thay đổi response sau khi result `dispatcher` đã render (trừ khi dùng `PreResultListener`).

<details>
<summary>Gợi ý lời giải</summary>

- 4.1: `web.xml`:
```xml
<filter>
  <filter-name>struts2</filter-name>
  <filter-class>org.apache.struts2.dispatcher.filter.StrutsPrepareAndExecuteFilter</filter-class>
</filter>
<filter-mapping><filter-name>struts2</filter-name><url-pattern>/*</url-pattern></filter-mapping>
```
(Struts 2.3 dùng package `org.apache.struts2.dispatcher.ng.filter`.)
- 4.3: `invoke()` lấy interceptor kế tiếp từ iterator; nếu còn → `interceptor.intercept(this)`; nếu hết → `invokeActionOnly()` rồi `executeResult()` (nếu chưa thực thi). Vì result thực thi ở "đáy" của chuỗi, response đã commit khi stack unwind → muốn can thiệp trước khi render phải đăng ký `PreResultListener`.

</details>

---
<a id="5-struts2-ognl"></a>
## 5. Struts 2: ValueStack, OGNL, results, cấu hình

### 5.1 ValueStack và OGNL
**OGNL (Object-Graph Navigation Language)** là ngôn ngữ biểu thức để đọc/ghi thuộc tính theo đường dẫn (`product.category.name`), gọi method, tạo collection... Struts 2 dùng OGNL ở **hai chiều**:
- **Ghi**: `params` interceptor biến parameter `product.name=Laptop` thành lời gọi `getProduct().setName("Laptop")`.
- **Đọc**: tag JSP `<s:property value="product.name"/>`.

**ValueStack** là một stack object (root là `CompoundRoot`). Action được push lên stack mỗi request (với `ModelDriven` thì model nằm trên action). Khi đánh giá `name`, OGNL tìm từ đỉnh stack xuống tới object đầu tiên có property `name`. Ngoài root còn **context map** truy cập bằng `#`: `#session`, `#request`, `#parameters`, `#application`, `#attr`.

```jsp
<%@ taglib prefix="s" uri="/struts-tags" %>
<s:form action="product_save" method="post">
  <s:token/>
  <s:textfield name="name" key="product.name"/>
  <s:textfield name="price" key="product.price"/>
  <s:submit key="button.save"/>
</s:form>

<s:iterator value="products" var="p">
  <tr><td><s:property value="#p.name"/></td>      <%-- s:property escape HTML theo mặc định --%>
      <td><s:property value="#p.price"/></td></tr>
</s:iterator>
<s:property value="#session.user.fullName"/>
```

> 💡 **Góc nhìn Senior:** sức mạnh của OGNL (gọi method, truy cập static, tạo object) chính là **bề mặt tấn công**. Gần như mọi RCE nghiêm trọng của Struts 2 đều có dạng: dữ liệu do attacker kiểm soát bị **đánh giá như biểu thức OGNL** (qua header, tên parameter, namespace, thuộc tính tag `%{...}`), rồi biểu thức vượt sandbox (`SecurityMemberAccess`) để gọi `Runtime.exec`. Phần 8 đi sâu.

### 5.2 Results và result types
```xml
<action name="product_*" class="productAction" method="{1}">
  <allowed-methods>list,input,save</allowed-methods>
  <result name="success" type="redirectAction">product_list</result>  <!-- PRG sau save -->
  <result name="input">/WEB-INF/content/product/edit.jsp</result>
  <result name="list">/WEB-INF/content/product/list.jsp</result>
  <result name="invalid.token">/WEB-INF/content/error/double-submit.jsp</result>
</action>
```
| Type | Hành vi |
|---|---|
| `dispatcher` (mặc định) | Forward tới JSP |
| `redirect` | Redirect tới URL |
| `redirectAction` | Redirect tới action khác (PRG) |
| `chain` | Gọi tiếp action khác trong cùng request (hạn chế dùng — khó theo dõi, chia sẻ ValueStack) |
| `stream` | Trả `InputStream` (download file) với `contentType`, `contentDisposition` |
| `json` (plugin) | Serialize action/thuộc tính thành JSON (dùng cho AJAX) |
| `freemarker`, `tiles`, `httpheader`, `plainText` | Template FreeMarker, Tiles definition, chỉ set header/status, text thô |

Dấu `*` + `{1}` là **wildcard mapping**; cùng với `allowed-methods`/strict method invocation (mặc định từ 2.5) để giới hạn method được gọi qua URL.

### 5.3 struts.xml và hằng số quan trọng
```xml
<!DOCTYPE struts PUBLIC "-//Apache Software Foundation//DTD Struts Configuration 6.0//EN"
  "https://struts.apache.org/dtds/struts-6.0.dtd">
<struts>
  <constant name="struts.devMode" value="false"/>                         <!-- true ở production = lộ thông tin, mở debug -->
  <constant name="struts.enable.DynamicMethodInvocation" value="false"/>  <!-- chặn action!method -->
  <constant name="struts.i18n.encoding" value="UTF-8"/>
  <constant name="struts.custom.i18n.resources" value="messages"/>
  <constant name="struts.multipart.maxSize" value="10485760"/>            <!-- 10 MB -->
  <constant name="struts.action.excludePattern" value="/api/.*,/v2/.*"/>  <!-- để Spring MVC xử lý (phần 10) -->

  <package name="default" namespace="/" extends="struts-default" strict-method-invocation="true">
    <default-action-ref name="index"/>
    ...
  </package>
  <include file="struts-product.xml"/>
</struts>
```
Package có `namespace` (tiền tố URL), `extends` (kế thừa interceptor/result của `struts-default` hoặc `json-default`), có thể `abstract`.

### 5.4 Convention plugin (cấu hình bằng quy ước)
Thay vì XML, Convention plugin tìm action theo quy ước: class trong package có tên chứa `action`, `actions`, `struts`, `struts2` và tên kết thúc bằng `Action` (hoặc implement `Action`); URL suy ra từ tên class (`ProductListAction` → `/product-list`), JSP mặc định trong `/WEB-INF/content/` (`product-list-success.jsp`). Annotation `@Namespace`, `@Action`, `@Result`, `@InterceptorRef`, `@ParentPackage` để tùy chỉnh.

> ⚠️ Codebase dùng cả XML lẫn Convention → cùng một URL có thể được map hai nơi, rất khó debug. Plugin **config-browser** (`/config-browser/index.action`) giúp liệt kê toàn bộ cấu hình runtime — chỉ bật ở dev, **không bao giờ** ở production.

### 🛠 Bài tập phần 5

**Bài 5.1 — Thí nghiệm ValueStack (Cơ bản)**
- Đề bài: trong một JSP, dùng `<s:debug/>` (devMode) và thử `<s:property value="name"/>` khi cả action và model (`ModelDriven`) đều có property `name`.
- Tiêu chí đạt: giải thích giá trị nào được hiển thị và vì sao (thứ tự trên stack).

**Bài 5.2 — Download file với result `stream` (Trung bình)**
- Đề bài: action xuất danh sách sản phẩm ra CSV, trả về bằng result `stream`.
- Tiêu chí đạt: tên file tiếng Việt đúng (`Content-Disposition` với `filename*=UTF-8''...`); không đọc toàn bộ file vào bộ nhớ; header `Content-Type: text/csv; charset=UTF-8`.

<details>
<summary>Gợi ý lời giải</summary>

- 5.1: model được push **trên** action → `name` lấy từ model. Muốn lấy của action: `[1].name` hoặc `action.name` tùy cách expose.
- 5.2:
```xml
<result name="success" type="stream">
  <param name="contentType">text/csv; charset=UTF-8</param>
  <param name="inputName">csvStream</param>
  <param name="contentDisposition">attachment; filename*=UTF-8''${encodedFileName}</param>
  <param name="bufferSize">8192</param>
</result>
```
`getCsvStream()` trả `PipedInputStream` hoặc file tạm; `encodedFileName = URLEncoder.encode(name, UTF_8).replace("+", "%20")`.

</details>

---

<a id="6-struts2-features"></a>
## 6. Struts 2: validation, i18n, file upload, double submit

### 6.1 Validation framework
Ba cách, có thể kết hợp:
1. **Override `validate()`** trong action (`ActionSupport` implement `Validateable`).
2. **XML**: file `ProductAction-validation.xml` (hoặc `ProductAction-<alias>-validation.xml` cho từng action alias) đặt cùng package.
3. **Annotation**: `@RequiredStringValidator`, `@IntRangeFieldValidator`, `@Validations`...

```xml
<!-- ProductAction-product_save-validation.xml -->
<validators>
  <field name="name">
    <field-validator type="requiredstring"><message key="error.name.required"/></field-validator>
    <field-validator type="stringlength"><param name="maxLength">100</param>
      <message key="error.name.length"/></field-validator>
  </field>
  <field name="price">
    <field-validator type="required"><message key="error.price.required"/></field-validator>
  </field>
</validators>
```
Interceptor `validation` chạy validator, `workflow` kiểm tra `hasErrors()` → trả `input` mà **không gọi** method action. Lỗi chuyển kiểu (nhập "abc" vào field `BigDecimal`) do `conversionError` interceptor thêm vào field errors.

> ⚠️ Validation chạy cho **mọi method** của action (kể cả `list`, `input`) trừ khi cấu hình `excludeMethods` (mặc định defaultStack loại `input,back,cancel,browse`) hoặc dùng `@SkipValidation`. Lỗi kinh điển: mở trang danh sách cũng báo "Tên không được để trống".

### 6.2 i18n
- `struts.custom.i18n.resources=messages` → `messages.properties`, `messages_vi.properties`, `messages_en.properties`.
- Tìm key theo thứ tự: `ActionClass.properties` → các lớp cha/interface → `package.properties` theo cây package → global resource.
- Trong code: `getText("product.saved")`; trong JSP: `<s:text name="product.name"/>`, thuộc tính `key` của form tag.
- Đổi ngôn ngữ: `i18n` interceptor đọc parameter `request_locale=vi` và lưu vào session.
- Java 9+ đọc file `.properties` mặc định bằng UTF-8 (JEP 226) cho `PropertyResourceBundle`; codebase cũ thường còn file mã hóa `\uXXXX` bằng `native2ascii` — đừng trộn lẫn hai kiểu.

### 6.3 File upload
Struts 2 dùng `MultiPartRequestWrapper` với parser mặc định dựa trên Apache Commons FileUpload (gọi là "jakarta" parser, không liên quan Jakarta EE).

Cách cũ (interceptor `fileUpload`, quy ước setter):
```java
private File upload;              // file tạm
private String uploadContentType;
private String uploadFileName;    // tên do client gửi — KHÔNG tin tưởng
```
Cách mới từ **Struts 6.4.0**: interceptor `actionFileUpload` + action implement `UploadedFilesAware`:
```java
public class AvatarAction extends ActionSupport implements UploadedFilesAware {
    private UploadedFile avatar;

    @Override
    public void withUploadedFiles(List<UploadedFile> files) {
        if (!files.isEmpty()) this.avatar = files.get(0);
    }

    public String execute() throws IOException {
        String ext = switch (avatar.getContentType()) {
            case "image/png" -> ".png";
            case "image/jpeg" -> ".jpg";
            default -> throw new IllegalArgumentException("Unsupported type");
        };
        Path target = storageDir.resolve(UUID.randomUUID() + ext);   // tên do SERVER sinh
        Files.copy(((File) avatar.getContent()).toPath(), target);
        return SUCCESS;
    }
}
```
Quy tắc an toàn cho upload (độc lập framework):
- **Không bao giờ** dùng tên file của client để xây đường dẫn lưu (path traversal `../../webapps/ROOT/shell.jsp`).
- Lưu **ngoài** web root; không cho thực thi; phục vụ lại qua action/CDN với `Content-Disposition` và `X-Content-Type-Options: nosniff`.
- Giới hạn kích thước (`struts.multipart.maxSize`, `maximumSize` của interceptor), whitelist `allowedTypes`/`allowedExtensions`, kiểm tra magic bytes nếu cần, quét malware cho hệ thống tài chính.

> ⚠️ CVE-2023-50164 (S2-066) và CVE-2024-53677 (S2-067) đều nằm ở cơ chế upload cũ: attacker thao túng parameter upload để ghi file ra ngoài thư mục dự kiến (path traversal) và có thể dẫn tới RCE. Bản vá cho S2-067 yêu cầu **chuyển sang cơ chế upload mới** (`actionFileUpload`) — chỉ nâng phiên bản mà vẫn dùng `fileUpload` cũ là chưa đủ.

### 6.4 Token interceptor chống double submit
```jsp
<s:form action="payment_submit">
  <s:token/>   <%-- sinh hidden field token + lưu token vào session --%>
  ...
</s:form>
```
```xml
<action name="payment_submit" class="paymentAction" method="submit">
  <interceptor-ref name="defaultStack"/>
  <interceptor-ref name="tokenSession"/>   <!-- hoặc "token" -->
  <result name="success" type="redirectAction">payment_done</result>
  <result name="invalid.token">/WEB-INF/content/error/double-submit.jsp</result>
</action>
```
- `token`: submit lần hai với token đã dùng → result `invalid.token`.
- `tokenSession`: submit trùng sẽ **chờ và trả lại kết quả** của lần đầu (người dùng không thấy lỗi).
- Token chỉ chống submit lặp từ cùng session/form. Với nghiệp vụ tài chính, vẫn cần **idempotency ở tầng nghiệp vụ/DB** (unique constraint trên mã giao dịch, idempotency key) — token không bảo vệ khi request đến từ hai tab, retry ở tầng mạng, hay chạy nhiều node không chia sẻ session.

### 🛠 Bài tập phần 6

**Bài 6.1 — Validation XML + i18n (Cơ bản)**
- Đề bài: form tạo khách hàng (họ tên, email, số điện thoại VN, ngày sinh) với validation XML và thông báo tiếng Việt/tiếng Anh.
- Tiêu chí đạt: đổi `request_locale` đổi được ngôn ngữ thông báo; trang danh sách không bị validate nhầm; số điện thoại dùng `regex` validator.

**Bài 6.2 — Upload an toàn (Trung bình)**
- Đề bài: chức năng upload ảnh đại diện dùng `actionFileUpload` (Struts ≥ 6.4).
- Tiêu chí đạt: chặn file > 2 MB, chặn `.jsp`/`.html` dù đổi content-type; tên file lưu do server sinh; test với tên file `../../evil.jsp` không ghi ra ngoài thư mục.

**Bài 6.3 — Double submit trong thanh toán (Nâng cao)**
- Đề bài: action chuyển khoản dùng `tokenSession`; sau đó chứng minh token không đủ bằng cách gửi 2 request song song từ 2 tab; bổ sung idempotency ở tầng DB.
- Tiêu chí đạt: với 2 tab, chỉ một giao dịch được ghi nhận; giải thích lớp bảo vệ nào chặn trường hợp nào.

<details>
<summary>Gợi ý lời giải</summary>

- 6.1: `<field-validator type="regex"><param name="regex"><![CDATA[^(0|\+84)(3|5|7|8|9)\d{8}$]]></param>...`; thêm `@SkipValidation` cho `list()`.
- 6.3: sinh `transferRequestId` (UUID) khi render form, lưu cùng giao dịch với `UNIQUE` constraint; lần hai vi phạm unique → trả về kết quả của giao dịch đã có.

</details>

---

<a id="7-integration"></a>
## 7. Tích hợp Spring và Hibernate

### 7.1 Struts 2 + Spring (struts2-spring-plugin)
```xml
<!-- web.xml: Spring root context -->
<context-param>
  <param-name>contextConfigLocation</param-name>
  <param-value>/WEB-INF/applicationContext.xml</param-value>
</context-param>
<listener>
  <listener-class>org.springframework.web.context.ContextLoaderListener</listener-class>
</listener>
```
```xml
<!-- struts.xml -->
<constant name="struts.objectFactory" value="spring"/>            <!-- plugin thường đặt sẵn -->
<constant name="struts.objectFactory.spring.autoWire" value="name"/>  <!-- name | type | constructor | auto -->

<!-- class = id của Spring bean, hoặc tên class đầy đủ (Struts tạo, Spring autowire) -->
<action name="product_*" class="productAction" method="{1}"> ... </action>
```
```xml
<!-- applicationContext.xml: action PHẢI là prototype -->
<bean id="productAction" class="com.shop.web.ProductAction" scope="prototype">
  <constructor-arg ref="productService"/>
</bean>
```
> ⚠️ **Lỗi thường gặp nghiêm trọng:** khai báo action là Spring bean mà quên `scope="prototype"` → action thành **singleton** → dữ liệu form của người dùng này lộ sang người dùng khác (đúng loại lỗi của Struts 1). Khi dùng tên class đầy đủ trong `class=`, Struts tự tạo instance mới mỗi request và nhờ Spring autowire dependency — an toàn hơn.

### 7.2 Hibernate trong hệ thống Struts legacy
Mẫu hay gặp: `Action → Service (Spring @Transactional) → DAO (HibernateTemplate / SessionFactory.getCurrentSession())`. Các vấn đề điển hình:
- **OpenSessionInViewFilter** (Spring) được khai báo trước Struts filter để JSP truy cập lazy collection → mọi vấn đề của OSIV ở Module 09 (giữ connection suốt render, N+1 trong JSP). Khi refactor, thay bằng fetch theo use case + DTO.
- Code cũ dùng `org.springframework.orm.hibernate3.HibernateTemplate`/`HibernateDaoSupport`: Spring 5 đã gỡ bỏ các package hỗ trợ Hibernate 3 và 4 (chỉ còn `orm.hibernate5`) → nâng Spring trong hệ thống legacy kéo theo nâng Hibernate và sửa DAO; nên nhân dịp đó chuyển sang `SessionFactory.getCurrentSession()` hoặc JPA `EntityManager` thay vì tiếp tục dùng `HibernateTemplate`.
- Transaction đặt ở Action (gọi `session.beginTransaction()` trong action) → khó test, rò rỉ transaction khi exception. Chuyển ranh giới transaction về service.
- Thứ tự filter trong `web.xml`: `CharacterEncodingFilter` → security filter → `OpenSessionInViewFilter` (nếu còn) → `StrutsPrepareAndExecuteFilter`.

### 🛠 Bài tập phần 7

**Bài 7.1 — Bắt lỗi action singleton (Cơ bản)**
- Đề bài: cấu hình action là Spring bean **không** có `scope="prototype"`; dùng JMeter 2 user submit form khác nhau đồng thời.
- Tiêu chí đạt: tái hiện được lộ dữ liệu; sửa bằng 2 cách (prototype / dùng class name).

**Bài 7.2 — Struts 2 + Spring + Hibernate/JPA (Trung bình)**
- Đề bài: dựng stack Struts 2 + Spring (Java config hoặc XML) + JPA (Hibernate) + H2/PostgreSQL; service có `@Transactional`.
- Tiêu chí đạt: không dùng OSIV; JSP chỉ nhận DTO; integration test service với Testcontainers.

<details>
<summary>Gợi ý lời giải</summary>

- 7.2: Spring root context cấu hình `LocalContainerEntityManagerFactoryBean`, `JpaTransactionManager`, `@EnableTransactionManagement`; action gọi `productService.listForView()` trả `List<ProductRow>` (record). Nếu dùng Struts 7 (Jakarta EE) thì cần Spring 6; Struts 6 (javax) thì Spring 5.3.

</details>

---

<a id="8-security"></a>
## 8. Lịch sử bảo mật và hardening

### 8.1 Các lỗ hổng nổi tiếng
| Bulletin / CVE | Năm | Phạm vi & cơ chế | Bài học |
|---|---|---|---|
| **S2-045 / CVE-2017-5638** | 2017 | Jakarta Multipart parser: header `Content-Type` độc hại chứa biểu thức OGNL được đánh giá khi xử lý thông báo lỗi → **RCE không cần xác thực**. Ảnh hưởng 2.3.5–2.3.31, 2.5–2.5.10. | Là lỗ hổng trong vụ **Equifax** (2017): bản vá có từ tháng 3/2017 nhưng hệ thống không được cập nhật, dữ liệu của khoảng 147 triệu người bị lộ. Quản lý vá lỗi (patch management) là trách nhiệm kỹ thuật cấp Senior. |
| S2-046 / CVE-2017-5638 (vector khác) | 2017 | Cùng gốc, qua `Content-Disposition`/`Content-Length` của multipart | Chặn một header ở WAF không đủ. |
| S2-052 / CVE-2017-9805 | 2017 | REST plugin dùng XStream deserialize XML không an toàn → RCE | Plugin không dùng = bề mặt tấn công thừa; gỡ bỏ. |
| **S2-057 / CVE-2018-11776** | 2018 | Khi `alwaysSelectFullNamespace=true` và action/result không khai báo namespace (hoặc dùng wildcard namespace), namespace từ URL bị đánh giá như OGNL → RCE. Ảnh hưởng 2.3–2.3.34, 2.5–2.5.16. | Cấu hình "tưởng vô hại" cũng có thể mở RCE. |
| S2-061, S2-062 / CVE-2020-17530, CVE-2021-31805 | 2020–2022 | **Forced double OGNL evaluation**: thuộc tính tag dùng `%{...}` với giá trị do người dùng kiểm soát bị đánh giá hai lần | Không bao giờ đặt input người dùng vào `%{}` trong tag. |
| **S2-066 / CVE-2023-50164** | 2023 | Thao túng parameter upload (ví dụ khác biệt hoa/thường tên parameter) để vượt kiểm tra tên file → path traversal, ghi webshell → RCE. Sửa ở 2.5.33, 6.3.0.2. | Upload là vùng nguy hiểm; tên file client không đáng tin. |
| S2-067 / CVE-2024-53677 | 2024 | Tiếp tục lỗ hổng logic upload trong `FileUploadInterceptor` cũ; phải chuyển sang Action File Upload (6.4.0+) | Có những bản vá đòi hỏi **thay đổi code**, không chỉ nâng version. |
| Struts 1: CVE-2014-0114 | 2014 | ClassLoader manipulation qua `class.*` parameter (Commons BeanUtils) | Struts 1 EOL → phải tự giảm thiểu bằng filter. |

### 8.2 Checklist hardening Struts 2
1. **Nâng lên bản mới nhất của nhánh còn được hỗ trợ** (6.x/7.x tại thời điểm bạn học — kiểm tra trang download và security bulletins). Đăng ký mailing list `announcements@struts.apache.org`.
2. `struts.devMode=false` ở production.
3. `struts.enable.DynamicMethodInvocation=false`; `strict-method-invocation="true"` + `allowed-methods`.
4. Không dùng `%{...}` với dữ liệu người dùng trong thuộc tính tag; không đánh giá OGNL từ input.
5. Giữ cấu hình mặc định của OGNL sandbox: `struts.excludedClasses`, `struts.excludedPackageNames`, `struts.ognl.expressionMaxLength`; từ 6.x có thêm cơ chế allowlist OGNL (`struts.allowlist.enable`) — bật nếu phiên bản hỗ trợ.
6. **Bật yêu cầu khai báo parameter** (`@StrutsParameter` với `struts.parameters.requireAnnotations=true` — có từ 6.4, mặc định bật ở Struts 7) để chống mass assignment.
7. Chuyển sang **Action File Upload** interceptor; whitelist type/extension; lưu ngoài web root.
8. Gỡ plugin không dùng (REST/XStream, config-browser, Convention nếu không dùng); không để `struts.mapper.alwaysSelectFullNamespace=true` khi không cần.
9. Lớp phòng thủ bên ngoài: WAF (virtual patching khi chưa kịp vá), chạy app server với user ít quyền, không cho ghi vào thư mục webapp, egress filtering (chặn reverse shell), giám sát process con lạ (`/bin/sh` sinh ra từ JVM).
10. Quy trình: SCA trong CI (OWASP Dependency-Check, Snyk, Dependabot), SLA vá lỗi Critical (ví dụ 72 giờ), kiểm kê tất cả ứng dụng còn chạy Struts.

> 💡 **Góc nhìn Senior:** khi bản tin bảo mật Critical xuất hiện, quy trình nên có sẵn: (1) xác định ứng dụng bị ảnh hưởng trong 1–2 giờ nhờ SBOM/kiểm kê; (2) giảm thiểu tạm thời (rule WAF, tắt tính năng upload); (3) vá + regression test; (4) săn tìm dấu vết xâm nhập (log request bất thường, file `.jsp` lạ trong webapp, process con). Vụ Equifax cho thấy thất bại thường nằm ở **quy trình**, không phải ở việc thiếu kiến thức.

### 🛠 Bài tập phần 8

**Bài 8.1 — Lập bảng rủi ro (Cơ bản)**
- Đề bài: với ứng dụng Struts 2 bạn dựng ở các phần trước, chạy Dependency-Check, đối chiếu từng CVE với cấu hình thực tế (có dùng upload không? REST plugin? namespace wildcard?).
- Tiêu chí đạt: bảng CVE → có bị ảnh hưởng hay không → lý do → hành động.

**Bài 8.2 — Tái hiện có kiểm soát (Trung bình)**
- Đề bài: trong môi trường lab **cô lập** (Docker, không public), chạy một image Struts phiên bản cũ có lỗ hổng S2-045 dành cho mục đích giáo dục (ví dụ từ Vulhub), quan sát request khai thác **chỉ đọc** (ví dụ in ra `id`), sau đó nâng cấp và xác nhận bị chặn.
- Tiêu chí đạt: báo cáo gồm nguyên nhân gốc, dấu hiệu trong log, cách phát hiện, cách khắc phục. **Chỉ thực hiện trên hệ thống của bạn.**

**Bài 8.3 — Hardening hoàn chỉnh (Nâng cao)**
- Đề bài: áp dụng toàn bộ checklist 8.2 cho ứng dụng CRUD của bạn; viết test tự động kiểm tra: devMode tắt, DMI tắt, gọi method không nằm trong `allowed-methods` trả 404, parameter không có `@StrutsParameter` không được bind.
- Tiêu chí đạt: test chạy trong CI; README mô tả từng biện pháp và lý do.

<details>
<summary>Gợi ý lời giải</summary>

- 8.2: dấu hiệu trong log: header `Content-Type` chứa `%{` hoặc `${`, `#_memberAccess`, `@java.lang.Runtime@`. Rule WAF tạm thời: chặn `Content-Type` chứa `%{`/`${`/`#`. Sau khi nâng cấp, request trả lỗi parse multipart bình thường.
- 8.3: kiểm tra cấu hình runtime bằng `Dispatcher.getInstance().getContainer().getInstance(String.class, "struts.devMode")` trong integration test, hoặc test HTTP với `StrutsJUnit4TestCase`/MockMvc-like harness của Struts (`StrutsTestCase`).

</details>

---

<a id="9-compare"></a>
## 9. Struts 2 vs Spring MVC

| Tiêu chí | Struts 2 | Spring MVC |
|---|---|---|
| Front controller | Filter (`StrutsPrepareAndExecuteFilter`) | Servlet (`DispatcherServlet`) |
| Handler | Action — **instance mới mỗi request**, dữ liệu là field | `@Controller` — **singleton**, dữ liệu là tham số method (`@RequestParam`, `@ModelAttribute`, `@RequestBody`) |
| Binding | OGNL vào property của action | `DataBinder` + `ConversionService`, có `@InitBinder`/allowed fields |
| Validation | XML/annotation riêng của Struts, `validate()` | Bean Validation (`@Valid`, `jakarta.validation`) + `BindingResult` |
| Chuỗi xử lý chung | Interceptor stack | `Filter`, `HandlerInterceptor`, `@ControllerAdvice`, AOP |
| View | JSP + Struts tags, FreeMarker, Tiles | Thymeleaf, JSP, FreeMarker, Mustache; REST: `@RestController` + Jackson |
| REST / JSON | Plugin REST/JSON (hạn chế) | First-class: content negotiation, `ResponseEntity`, `ProblemDetail` |
| Test | `StrutsTestCase`/JUnit plugin | `MockMvc`, `@WebMvcTest`, `WebTestClient` |
| Bảo mật | Tự xây (interceptor) | Spring Security tích hợp sâu |
| Hệ sinh thái | Thu hẹp, chủ yếu bảo trì | Spring Boot, Actuator, observability, cloud-native |
| Lịch sử CVE | Nhiều RCE nghiêm trọng liên quan OGNL | Có CVE (ví dụ Spring4Shell CVE-2022-22965 — data binding + ClassLoader trên JDK 9+ / Tomcat WAR) nhưng ít hơn và mô hình binding ít "động" hơn |

> 💡 **Góc nhìn Senior:** đừng nói "Spring MVC an toàn còn Struts thì không". Spring4Shell cho thấy **data binding** là bề mặt tấn công chung của mọi framework web — nguyên tắc giống nhau: giới hạn trường được bind (DTO riêng cho từng form), vá kịp thời, phòng thủ nhiều lớp.

### 🛠 Bài tập phần 9

**Bài 9.1 — Cùng chức năng, hai framework (Trung bình)**
- Đề bài: hiện thực chức năng "tìm kiếm + tạo sản phẩm" bằng Struts 2 và bằng Spring MVC; so sánh số dòng code, số file cấu hình, độ dễ test.
- Tiêu chí đạt: bảng so sánh có số liệu; nhận xét về thread-safety (field của Action vs tham số method của Controller).

**Bài 9.2 — Đánh giá binding an toàn (Nâng cao)**
- Đề bài: trên cả hai phiên bản ở bài 9.1, thử gửi thêm parameter không có trong form (`role=ADMIN`, `id=1`, `class.module...`). Ghi nhận framework nào bind được gì.
- Tiêu chí đạt: bảng kết quả; với mỗi phiên bản áp dụng biện pháp chặn (Struts: `@StrutsParameter`/`excludeParams`; Spring: DTO riêng, `@InitBinder` + `setAllowedFields`) và chứng minh bằng test.

<details>
<summary>Gợi ý lời giải</summary>

Spring MVC: một `@Controller` với `@GetMapping`/`@PostMapping`, `ProductForm` record + `@Valid`, `BindingResult`; test bằng `@WebMvcTest` + `MockMvc`. Struts 2: action class + `struts.xml` + `-validation.xml` + JSP; test cần container giả lập Struts.

</details>

---

<a id="10-migration"></a>
## 10. Chiến lược migration sang Spring MVC / Spring Boot

### 10.1 Các phương án
| Phương án | Mô tả | Khi nào |
|---|---|---|
| Big-bang rewrite | Viết lại toàn bộ rồi chuyển một lần | Hệ thống nhỏ, nghiệp vụ rõ, có test đầy đủ. Hiếm khi đúng với hệ thống ngân hàng. |
| **Strangler fig** | Dựng hệ thống mới bao quanh hệ thống cũ, chuyển dần từng luồng; route request theo URL; tắt phần cũ khi không còn traffic | Mặc định cho hệ thống lớn, đang vận hành |
| Nâng cấp tại chỗ | Giữ Struts, nâng phiên bản, hardening | Chỉ là bước đệm để giảm rủi ro bảo mật trong lúc migrate |

### 10.2 Hai kiểu "chung sống"
**(A) Cùng WAR — Struts và Spring MVC song song:**
```xml
<!-- web.xml -->
<filter-mapping><filter-name>struts2</filter-name><url-pattern>/*</url-pattern></filter-mapping>

<servlet>
  <servlet-name>spring</servlet-name>
  <servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>
  <load-on-startup>1</load-on-startup>
</servlet>
<servlet-mapping><servlet-name>spring</servlet-name><url-pattern>/v2/*</url-pattern></servlet-mapping>
```
```xml
<!-- struts.xml: để Struts bỏ qua URL của Spring -->
<constant name="struts.action.excludePattern" value="/v2/.*"/>
```
Ưu: chia sẻ `HttpSession`, root `ApplicationContext` (service, DAO, transaction manager) — controller mới tái sử dụng service cũ ngay. Nhược: bị khóa vào phiên bản Servlet API/Spring chung (Struts 6 dùng `javax.servlet` → chỉ đi với Spring 5.x; muốn Spring 6/Boot 3 cần Struts 7 trên Jakarta EE hoặc chọn phương án B).

**(B) Hai ứng dụng sau reverse proxy (Nginx/API Gateway):**
```
                      ┌─► /v2/**, /api/**  ──► Spring Boot 3 app (mới)
Client ─► Nginx/GW ───┤
                      └─► /**  (còn lại)   ──► Struts app (cũ, Tomcat)
```
Ưu: tự do công nghệ (Java 21, Boot 3), deploy độc lập. Nhược: phải chia sẻ phiên đăng nhập (Spring Session + Redis cho cả hai app, hoặc SSO/OIDC với token), giao diện/menu phải đồng bộ, hai codebase truy cập chung DB → cần kỷ luật schema (Flyway quản lý từ một nơi).

### 10.3 Bảng ánh xạ khái niệm
| Struts | Spring MVC / Spring Boot |
|---|---|
| `Action` (Struts 1 `execute`, Struts 2 method) | `@Controller` method với `@GetMapping`/`@PostMapping` |
| `ActionForm` / field của action | DTO (record) + Bean Validation (`@NotBlank`, `@Size`, `@Pattern`) + `@Valid` + `BindingResult` |
| `validate()`, `validation.xml`, `-validation.xml` | Bean Validation annotation, custom `ConstraintValidator`, `Validator` của Spring cho rule chéo field |
| `ActionForward`/result `success` → JSP | Tên view `"product/list"` → Thymeleaf template; `"redirect:/v2/products"` cho PRG |
| `redirectAction` + action message | `RedirectAttributes.addFlashAttribute(...)` |
| Interceptor (auth, logging, timing) | Spring Security filter chain; `HandlerInterceptor`; `OncePerRequestFilter`; Micrometer cho timing |
| `global-exceptions` / exception interceptor | `@ControllerAdvice` + `@ExceptionHandler`, `ProblemDetail` cho API |
| `MessageResources`, `getText`, `<s:text>` | `MessageSource`, `#{key}` trong Thymeleaf, `LocaleResolver` + `LocaleChangeInterceptor` |
| Tiles layout | Thymeleaf Layout Dialect hoặc fragment (`th:replace`, `th:insert`) |
| Token interceptor | Idempotency key ở tầng nghiệp vụ + PRG; CSRF token của Spring Security (khác mục đích: chống CSRF, không chống double submit) |
| `stream` result | `ResponseEntity<StreamingResponseBody>` / `Resource` |
| `fileUpload` interceptor | `MultipartFile`, `spring.servlet.multipart.max-file-size` |
| `ServletActionContext`, `SessionAware` | Tham số method `HttpSession`, `@SessionAttribute`, hoặc bean `@SessionScope` |
| `OpenSessionInViewFilter` + entity trong JSP | Tắt OSIV, service trả DTO |

Ví dụ chuyển một action:
```java
// TRƯỚC: Struts 2
public class CustomerAction extends ActionSupport {
    private CustomerForm customer = new CustomerForm();
    private final CustomerService service;
    public String save() {
        service.create(customer);
        addActionMessage(getText("customer.created"));
        return SUCCESS;   // redirectAction customer_list
    }
    public CustomerForm getCustomer() { return customer; }
}

// SAU: Spring MVC
public record CustomerForm(
        @NotBlank @Size(max = 100) String fullName,
        @NotBlank @Email String email,
        @Pattern(regexp = "^(0|\\+84)(3|5|7|8|9)\\d{8}$") String phone) {}

@Controller
@RequestMapping("/v2/customers")
@RequiredArgsConstructor
class CustomerController {
    private final CustomerService service;   // tái sử dụng service cũ

    @GetMapping("/new")
    String form(Model model) {
        model.addAttribute("customer", new CustomerForm(null, null, null));
        return "customer/form";
    }

    @PostMapping
    String create(@Valid @ModelAttribute("customer") CustomerForm form, BindingResult result,
                  RedirectAttributes ra) {
        if (result.hasErrors()) return "customer/form";             // tương đương result "input"
        service.create(form);
        ra.addFlashAttribute("message", "customer.created");
        return "redirect:/v2/customers";                            // PRG
    }
}
```

### 10.4 Kế hoạch từng bước (mẫu)
1. **Ổn định & an toàn (tuần 0–4):** kiểm kê, nâng Struts lên bản còn hỗ trợ hoặc giảm thiểu (WAF, filter), CI build tái lập được, đưa vào version control/artifact repository chuẩn.
2. **Lưới an toàn bằng test:** viết **characterization test** end-to-end (Playwright/Selenium hoặc HTTP-level) cho các luồng quan trọng nhất — ghi lại hành vi hiện tại, kể cả "bug" mà người dùng đang dựa vào. Thêm observability: log có correlation id, metrics theo URL để biết luồng nào còn được dùng.
3. **Tách tầng nghiệp vụ:** di chuyển logic từ Action/JSP xuống service thuần (không phụ thuộc Servlet/Struts API). Đây thường là bước tốn công nhất và mang lại giá trị lớn nhất.
4. **Dựng nền tảng mới:** Spring MVC trong cùng WAR (phương án A) hoặc Spring Boot sau reverse proxy (phương án B); thống nhất authentication/session; layout Thymeleaf giống giao diện cũ.
5. **Chuyển từng luồng theo thứ tự ưu tiên:** bắt đầu với luồng **ít rủi ro, nhiều giá trị** (màn hình đọc, báo cáo), sau đó tới luồng ghi; mỗi luồng: viết controller mới → chạy song song → chuyển routing (feature flag theo user/nhóm) → theo dõi → gỡ action cũ.
6. **Đo tiến độ:** % URL/traffic đi qua stack mới, số action còn lại, số CVE còn mở.
7. **Tắt hệ thống cũ:** khi traffic về Struts = 0 trong một chu kỳ nghiệp vụ đầy đủ (bao gồm nghiệp vụ cuối tháng/cuối năm!), gỡ Struts, dọn dependency.

> 💡 **Góc nhìn Senior:**
> - Rủi ro lớn nhất không phải kỹ thuật mà là **nghiệp vụ ẩn** trong JSP scriptlet, JavaScript, stored procedure và "workaround" người dùng đã quen. Characterization test và phỏng vấn người dùng quan trọng hơn chọn framework.
> - Chạy song song cả hai phiên bản trên cùng DB → cẩn thận cache (L2C, session cache) và khác biệt validation (luồng cũ chấp nhận dữ liệu mà luồng mới từ chối → dữ liệu "bẩn" có sẵn làm vỡ màn hình mới).
> - Đặt "điều kiện dừng" và rollback rõ ràng cho mỗi luồng chuyển (feature flag về lại URL cũ).

> ⚠️ **Lỗi thường gặp:** chuyển action sang controller nhưng giữ nguyên entity làm form backing object → mass assignment (Spring cũng bind mọi property có setter). Luôn dùng DTO riêng cho form.

### 🛠 Bài tập phần 10

**Bài 10.1 — Bảng ánh xạ cho một module (Cơ bản)**
- Đề bài: với ứng dụng Struts 2 CRUD ở dự án mini, lập bảng: mỗi action/method → controller/endpoint mới, mỗi form → DTO + constraint, mỗi interceptor → cơ chế Spring tương ứng, mỗi JSP → template Thymeleaf.
- Tiêu chí đạt: không bỏ sót action nào (đối chiếu với config-browser hoặc grep struts.xml); ghi chú rủi ro cho từng mục.

**Bài 10.2 — Chung sống trong cùng WAR (Trung bình)**
- Đề bài: thêm `DispatcherServlet` cho `/v2/*` vào ứng dụng Struts 2 (Spring 5.3 nếu Struts 6, Spring 6 nếu Struts 7); controller mới dùng chung service và session đăng nhập.
- Tiêu chí đạt: người dùng đăng nhập ở phần Struts truy cập được `/v2/...` mà không phải đăng nhập lại; `struts.action.excludePattern` hoạt động; test cả hai phía.

**Bài 10.3 — Strangler qua reverse proxy (Nâng cao)**
- Đề bài: tách luồng "khách hàng" sang Spring Boot 3 riêng, Nginx route `/v2/customers/**` sang app mới; chia sẻ session bằng Spring Session + Redis (phía Struts dùng `DelegatingFilterProxy` + `springSessionRepositoryFilter`) hoặc SSO.
- Tiêu chí đạt: feature flag chuyển qua lại giữa luồng cũ/mới không cần deploy; characterization test xanh ở cả hai phiên bản; dashboard thể hiện % traffic qua luồng mới.

<details>
<summary>Gợi ý lời giải</summary>

- 10.2: root context (ContextLoaderListener) chứa service; `DispatcherServlet` có child context chỉ scan package `web.v2`. Session dùng chung tự nhiên vì cùng webapp. Lưu ý thứ tự filter: Struts filter map `/*` vẫn nhận request `/v2/*` nhưng exclude pattern sẽ cho đi qua tới servlet.
- 10.3: Nginx:
```nginx
location /v2/customers/ { proxy_pass http://customer-boot:8080; }
location /             { proxy_pass http://legacy-tomcat:8080; }
```
Feature flag ở tầng gateway (map cookie/nhóm người dùng) hoặc trong app cũ (link menu trỏ tới URL mới). Cookie session phải cùng tên và cùng domain/path.

</details>

---

<a id="du-an-mini"></a>
## Dự án mini

### "Quản lý khách hàng & hợp đồng" — từ Struts 2 sang Spring Boot

**Phần A — Xây dựng Struts 2 CRUD (legacy giả lập)**

Yêu cầu chức năng:
1. CRUD `Customer` (họ tên, email, số điện thoại VN, ngày sinh, hạng khách hàng) và `Contract` (số hợp đồng, khách hàng, giá trị, ngày hiệu lực, trạng thái).
2. Danh sách có tìm kiếm + phân trang; form tạo/sửa với validation XML và thông báo tiếng Việt/Anh (i18n).
3. Đăng nhập đơn giản bằng `AuthInterceptor`; namespace `/admin` cần role ADMIN.
4. Upload file scan hợp đồng (PDF ≤ 5 MB) bằng `actionFileUpload`; download qua result `stream`.
5. Chống double submit khi tạo hợp đồng (`tokenSession`) + unique constraint số hợp đồng.
6. Layout chung (header/menu/footer) bằng Tiles hoặc JSP include.
7. Struts 2 + Spring (action prototype hoặc class name) + JPA/Hibernate + PostgreSQL + Flyway.

**Phần B — Migrate luồng "Khách hàng" sang Spring Boot**
1. Viết characterization test (Playwright hoặc RestAssured + HTML parsing) cho luồng khách hàng trên bản Struts.
2. Dựng ứng dụng Spring Boot 3 (Thymeleaf, Spring Data JPA, Bean Validation, Spring Security) dùng chung DB.
3. Chuyển toàn bộ luồng khách hàng: list/search/create/edit/delete; PRG + flash message; i18n bằng `MessageSource`.
4. Route bằng reverse proxy (hoặc cùng WAR nếu dùng Struts 7 + Spring 6); chia sẻ phiên đăng nhập.
5. Feature flag chuyển giữa luồng cũ và mới.

**Yêu cầu phi chức năng**
- Hardening Struts theo checklist mục 8.2 (devMode off, DMI off, strict method invocation, `@StrutsParameter`, upload mới).
- Không có lỗi thread-safety (action prototype, interceptor stateless) — có test đồng thời.
- Không OSIV; JSP/Thymeleaf chỉ nhận DTO.
- Characterization test xanh trên **cả** bản Struts và bản Spring Boot.
- Dependency-Check không còn CVE Critical/High chưa xử lý (hoặc có ghi chú chấp nhận rủi ro).
- Tài liệu: sơ đồ kiến trúc trước/sau, bảng ánh xạ (bài 10.1), kế hoạch các bước tiếp theo cho luồng hợp đồng.

**Tiêu chí chấm (100 điểm)**

| Hạng mục | Điểm |
|---|---|
| Struts 2 CRUD đúng chức năng, cấu hình rõ ràng (struts.xml/Convention), validation + i18n | 20 |
| Interceptor, upload an toàn, double submit + idempotency | 15 |
| Hardening bảo mật có kiểm chứng bằng test | 15 |
| Tích hợp Spring + JPA đúng (scope, transaction ở service, không OSIV) | 10 |
| Migration: controller/DTO/Bean Validation/Thymeleaf đúng ánh xạ, PRG, i18n | 20 |
| Chung sống & strangler: routing, chia sẻ session, feature flag, characterization test | 15 |
| Tài liệu & kế hoạch tiếp theo | 5 |

---

<a id="checklist-tu-danh-gia"></a>
## Checklist tự đánh giá
- [ ] Tôi giải thích được vì sao hệ thống Struts còn tồn tại ở ngân hàng/bảo hiểm/chính phủ và Senior cần làm gì với chúng.
- [ ] Tôi vẽ được kiến trúc MVC Model 2, phân biệt forward/redirect và áp dụng PRG.
- [ ] Tôi mô tả được luồng request Struts 1 qua `ActionServlet` → `RequestProcessor` → `Action` → `ActionForward`, đọc được `struts-config.xml`, Tiles, Validator.
- [ ] Tôi giải thích được vì sao Action Struts 1 (và interceptor Struts 2, Spring controller) phải thread-safe, còn Action Struts 2 thì không.
- [ ] Tôi mô tả được luồng Struts 2: `StrutsPrepareAndExecuteFilter` → `ActionProxy` → `ActionInvocation` → interceptor stack → Action → Result.
- [ ] Tôi giải thích được ValueStack, OGNL, context map (`#session`, `#parameters`) và vì sao OGNL là bề mặt tấn công.
- [ ] Tôi cấu hình được results (dispatcher, redirectAction, stream, json), wildcard + allowed-methods, Convention plugin.
- [ ] Tôi dùng được validation framework, i18n, upload mới (`actionFileUpload`), token/tokenSession — và biết giới hạn của token.
- [ ] Tôi tích hợp được Struts 2 với Spring (và tránh lỗi action singleton) và Hibernate (không OSIV).
- [ ] Tôi kể được cơ chế và bài học của S2-045 (Equifax), S2-057, S2-061/062, S2-066, S2-067 và CVE-2014-0114 của Struts 1.
- [ ] Tôi áp dụng được checklist hardening Struts 2 và quy trình phản ứng khi có bản tin bảo mật Critical.
- [ ] Tôi so sánh được Struts 2 với Spring MVC về mô hình handler, binding, validation, test, hệ sinh thái.
- [ ] Tôi lập được kế hoạch migration strangler fig với hai kiểu chung sống (cùng WAR / reverse proxy), hiểu ràng buộc `javax` vs `jakarta`.
- [ ] Tôi ánh xạ được Action → Controller, ActionForm → DTO + Bean Validation, interceptor → filter/HandlerInterceptor, JSP/Tiles → Thymeleaf.
- [ ] Tôi tự xây được một ứng dụng Struts 2 CRUD và migrate một luồng của nó sang Spring Boot có characterization test.

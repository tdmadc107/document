# Câu hỏi phỏng vấn — Module 10: Apache Struts (1 & 2) và hệ thống legacy

> 📚 Giáo trình tương ứng: [Module 10 — Apache Struts (1 & 2) và hệ thống legacy](../01-giao-trinh/10-struts.md)

> **Cách dùng:** đọc câu hỏi, **tự trả lời thành tiếng** trước khi mở đáp án — đặc biệt các câu kịch bản (bảo mật, migration), vì ở công ty có hệ thống legacy (ngân hàng, bảo hiểm, dự án offshore Nhật) người phỏng vấn chấm nặng **cách bạn ra quyết định** hơn là thuộc API. Câu nào vấp → quay lại mục *📖 Ôn lại*.
>
> **Ký hiệu cấp độ:** 🟢 Cơ bản · 🟡 Senior · 🔴 Xoáy sâu.

## Mục lục

| Nhóm | Chủ đề | Câu hỏi |
|---|---|---|
| [A](#nhom-a) | Bối cảnh, MVC Model 2, Front Controller | Q1–Q3 |
| [B](#nhom-b) | Struts 1 | Q4–Q10 |
| [C](#nhom-c) | Struts 2: kiến trúc, interceptor, action | Q11–Q17 |
| [D](#nhom-d) | ValueStack, OGNL, results, cấu hình | Q18–Q22 |
| [E](#nhom-e) | Validation, i18n, upload, double submit | Q23–Q27 |
| [F](#nhom-f) | Tích hợp Spring & Hibernate | Q28–Q29 |
| [G](#nhom-g) | Bảo mật & hardening | Q30–Q33 |
| [H](#nhom-h) | So sánh với Spring MVC & migration | Q34–Q38 |

---

<a id="nhom-a"></a>
## A. Bối cảnh, MVC Model 2, Front Controller

### Q1. 🟢 Model 1 và Model 2 khác nhau thế nào? Front Controller trong Struts 1, Struts 2 và Spring MVC là class nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Model 1: request đi thẳng vào JSP, JSP vừa xử lý logic (scriptlet), gọi DB, vừa render — nhanh làm nhưng không bảo trì được. Model 2 (MVC trên web): một **controller** nhận mọi request, gọi model (business logic), chọn view (JSP) để render; JSP chỉ hiển thị. Front Controller: Struts 1 là `ActionServlet` (Servlet), Struts 2 là `StrutsPrepareAndExecuteFilter` (**Filter**), Spring MVC là `DispatcherServlet`.

**Giải thích chi tiết:**
- Front Controller xử lý các mối quan tâm chung (routing, i18n, binding, validation, bảo mật) rồi ủy quyền cho handler cụ thể — handler là **Command pattern** (`Action`, `@Controller` method).
- Đặt JSP trong `/WEB-INF` để không truy cập trực tiếp, buộc mọi request đi qua controller.

**Câu hỏi nối tiếp:**
- *Vì sao Struts 2 dùng Filter thay vì Servlet?* → Filter map `/*` chặn được mọi request (kể cả static), xử lý chain linh hoạt; kế thừa từ WebWork. `FilterDispatcher` cũ đã deprecated từ 2.1.3 và bị loại bỏ.

**⚠️ Câu trả lời gây điểm trừ:**
- Nhầm Struts 2 front controller là Servlet.

**📖 Ôn lại:** [2. MVC Model 2 và Front Controller](../01-giao-trinh/10-struts.md#2-model2)

</details>

### Q2. 🟢 Forward và redirect khác nhau thế nào? PRG là gì và vì sao cần?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Forward (`RequestDispatcher.forward`) xử lý trong server, URL không đổi, request attributes giữ nguyên, không có round-trip. Redirect (`sendRedirect`, 302/303) bảo browser gửi request mới — URL đổi, request attributes mất. **PRG (Post/Redirect/Get)**: sau POST thành công thì redirect sang trang GET, để người dùng nhấn F5 không submit lại form (tạo bản ghi trùng). Thông báo "Tạo thành công" truyền qua session/flash, hiển thị một lần.

**Giải thích chi tiết:**
- Struts 1: `<forward name="success" path="/product/list.do" redirect="true"/>`. Struts 2: result `redirectAction`. Spring MVC: `"redirect:/..."` + `RedirectAttributes.addFlashAttribute`.
- Forward dùng khi hiển thị view hoặc trả lại form có lỗi validation (giữ dữ liệu đã nhập).

**Câu hỏi nối tiếp:**
- *PRG có chống double submit hoàn toàn không?* → Không: double-click trước khi redirect, hai tab, retry mạng vẫn có thể gửi hai lần → cần token và idempotency ở tầng nghiệp vụ (Q26).

**⚠️ Câu trả lời gây điểm trừ:**
- Nói forward "chuyển trang ở browser".

**📖 Ôn lại:** [2.2 Forward vs redirect](../01-giao-trinh/10-struts.md#2-model2)

</details>

### Q3. 🟡 Kịch bản: bạn được tuyển làm Senior cho một hệ thống ngân hàng chạy Struts 1.3 + JSP + Oracle từ 2008, không tài liệu. Ba tháng đầu bạn làm gì?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Không đề xuất "viết lại toàn bộ bằng Spring Boot + microservices" ngay. Thứ tự: (1) **kiểm kê hiện trạng** — phiên bản Struts/Java/app server, số Action, JSP, plugin, thư viện có CVE (OWASP Dependency-Check); (2) **giảm rủi ro bảo mật trước** — Struts 1 đã EOL từ 2013, thêm filter chặn tham số `class.*`, WAF, cô lập mạng; (3) **lưới an toàn bằng test** — characterization test cho các luồng quan trọng, thêm observability (correlation id, metrics theo URL); (4) build tái lập được trong CI; (5) lập **kế hoạch hiện đại hóa** từng bước (strangler fig) có đo lường, có rollback, trình bày rủi ro bằng ngôn ngữ của ban lãnh đạo.

**Giải thích chi tiết:**
- Struts 1 → Struts 2 **không phải nâng cấp** mà là viết lại tầng web; khi đã phải viết lại, đích hợp lý thường là Spring MVC/Spring Boot.
- Ba phương án trình bày cho lãnh đạo: (A) giữ nguyên + WAF + cô lập (rẻ, rủi ro còn), (B) chuyển Struts 2 bản mới (vẫn là viết lại), (C) strangler sang Spring Boot theo nghiệp vụ (đầu tư lớn nhất, giảm rủi ro dài hạn).
- Rủi ro lớn nhất là **nghiệp vụ ẩn** trong JSP scriptlet, JavaScript, stored procedure.

**Câu hỏi nối tiếp:**
- *Làm sao biết luồng nào còn được dùng?* → Access log/metrics theo URL trong một chu kỳ nghiệp vụ đầy đủ (gồm cuối tháng, cuối năm).

**⚠️ Câu trả lời gây điểm trừ:**
- "Viết lại hết bằng microservices trong 6 tháng."
- Không nhắc bảo mật/EOL.

**📖 Ôn lại:** [1. Vì sao Senior ở Việt Nam vẫn gặp Struts](../01-giao-trinh/10-struts.md#1-boi-canh) · [10.4 Kế hoạch từng bước](../01-giao-trinh/10-struts.md#10-migration)

</details>

---

<a id="nhom-b"></a>
## B. Struts 1

### Q4. 🟢 Kể các thành phần chính của Struts 1 và mô tả luồng một request `/product/save.do`.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `ActionServlet` (front controller, map `*.do` trong `web.xml`) → `RequestProcessor` xử lý theo chuỗi bước cố định → tìm `ActionMapping` trong `struts-config.xml` → lấy/tạo `ActionForm` theo scope, `reset()` rồi populate parameter → nếu `validate="true"` gọi `form.validate()`, lỗi thì forward về `input` → lấy instance `Action` (**cache, một instance duy nhất**) → `execute(mapping, form, req, resp)` trả `ActionForward` → forward/redirect tới view (JSP hoặc Tiles definition).

**Giải thích chi tiết:**
- `struts-config.xml`: `form-beans`, `action-mappings`, `global-forwards`, `global-exceptions`, `message-resources`, `plug-in` (Validator, Tiles).
- `ActionForm` bắt buộc kế thừa `ActionForm` (không phải POJO), thường dùng `String` cho mọi field để giữ lại input sai khi hiển thị lỗi.
- Struts 1.3 chuyển `RequestProcessor` sang `ComposableRequestProcessor` dựa trên Commons Chain.

**Câu hỏi nối tiếp:**
- *Đọc nhanh codebase lạ bắt đầu từ đâu?* → URL → `<action path>` trong `struts-config.xml` → class `type` → `forward` → JSP.

**⚠️ Câu trả lời gây điểm trừ:**
- Nói Action được tạo mới mỗi request (đó là Struts 2).

**📖 Ôn lại:** [3.1–3.2 Thành phần và luồng request](../01-giao-trinh/10-struts.md#3-struts1)

</details>

### Q5. 🟡 Muốn kiểm tra đăng nhập cho mọi action trong Struts 1, bạn chèn logic ở đâu? So sánh với cách làm ở Struts 2.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Struts 1 không có interceptor; `RequestProcessor` là **Template Method** — kế thừa và override `processPreprocess()` (trả `false` để dừng xử lý, tự redirect tới login), hoặc dùng `processRoles` với thuộc tính `roles` trên action (dựa trên container security). Cách độc lập framework: một Servlet **Filter** đặt trước `ActionServlet`. Struts 2: viết `Interceptor` và đưa vào interceptor stack của package (Chain of Responsibility), trả result `login` để dừng chuỗi.

**Giải thích chi tiết:**
- Một số codebase cũ dùng "BaseAction" với `execute` kiểm tra session rồi gọi `doExecute` — dễ quên khi thêm action mới không kế thừa.
- Với Struts 1.3 Composable: thêm command vào chain trong `chain-config.xml`.
- Khi migration, Filter là lựa chọn có giá trị lâu dài nhất (tái dùng được cho phần Spring mới) — hoặc Spring Security filter chain.

**Câu hỏi nối tiếp:**
- *Ưu điểm Filter so với RequestProcessor?* → Chạy cho mọi URL kể cả JSP/static, không phụ thuộc phiên bản Struts.

**⚠️ Câu trả lời gây điểm trừ:**
- Kiểm tra login bằng scriptlet trong từng JSP.

**📖 Ôn lại:** [3.1 Các thành phần — RequestProcessor](../01-giao-trinh/10-struts.md#3-struts1) · [4.3 Interceptor](../01-giao-trinh/10-struts.md#4-struts2-core)

</details>

### Q6. 🟢 Đoạn code Struts 1 sau có bug gì khi chạy production?

```java
public class CheckoutAction extends Action {
    private Cart cart;
    private SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy");

    public ActionForward execute(ActionMapping m, ActionForm f,
                                 HttpServletRequest req, HttpServletResponse resp) {
        cart = (Cart) req.getSession().getAttribute("cart");
        String day = fmt.format(new Date());
        ...
    }
}
```

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `Action` của Struts 1 là **singleton** (một instance cho mỗi class, giống Servlet), dùng chung cho mọi request đồng thời. Field `cart` là trạng thái theo request → **race condition**: user A có thể thấy/thanh toán giỏ hàng của user B. `SimpleDateFormat` không thread-safe → định dạng sai hoặc exception dưới tải. Sửa: dữ liệu theo request đặt trong **biến cục bộ**, request hoặc session; thay `SimpleDateFormat` bằng `DateTimeFormatter` (immutable, có thể là `static final`).

**Giải thích chi tiết:**
- Quy tắc: field của Action chỉ chứa dependency **stateless, thread-safe** (service).
- Lỗi này không lộ ở dev/test một người dùng; chỉ lộ dưới tải → tái hiện bằng JMeter hoặc test đa luồng.
- Cùng loại lỗi: interceptor Struts 2, Spring `@Controller`, Servlet — đều singleton.

**Câu hỏi nối tiếp:**
- *Struts 2 Action có bị không?* → Không, Struts 2 tạo action mới mỗi request — trừ khi khai báo action là Spring bean singleton (Q28).

**⚠️ Câu trả lời gây điểm trừ:**
- "Thêm `synchronized` vào `execute`" — tuần tự hóa toàn bộ request.

**📖 Ôn lại:** [3.4 Thread-safety của Action singleton](../01-giao-trinh/10-struts.md#3-struts1)

</details>

### Q7. 🟡 Người dùng báo: bỏ tick checkbox "Nhận email" rồi lưu, nhưng mở lại vẫn thấy được tick. Form là `ActionForm` Struts 1. Nguyên nhân có thể là gì?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Checkbox không tick thì browser **không gửi** parameter → `BeanUtils.populate` không gọi setter → field giữ giá trị cũ. Nếu `ActionForm` ở **scope session** — mặc định của Struts 1 khi không khai báo `scope`! — form của lần submit trước còn nguyên, nên giá trị `true` "dính" lại. Sửa: đặt lại field về `false` trong `reset()` (chạy trước populate), và đặt `scope="request"` trừ khi thật sự cần wizard nhiều bước.

**Giải thích chi tiết:**
```java
@Override
public void reset(ActionMapping mapping, HttpServletRequest request) {
    this.receiveEmail = false;
}
```
- Form scope session còn làm session phình to và lộ dữ liệu giữa các tab.
- Struts 2 xử lý bằng interceptor `checkbox` (tag `<s:checkbox>` sinh thêm hidden field `__checkbox_name`).

**Câu hỏi nối tiếp:**
- *Spring MVC xử lý thế nào?* → Hidden field `_name` do `<form:checkbox>`/Thymeleaf sinh ra; `WebDataBinder` set `false` khi chỉ có marker.

**⚠️ Câu trả lời gây điểm trừ:**
- Đổ lỗi cho cache trình duyệt mà không biết cơ chế checkbox.

**📖 Ôn lại:** [3.4 — lỗi thường gặp ActionForm scope session](../01-giao-trinh/10-struts.md#3-struts1)

</details>

### Q8. 🟢 Kể các biến thể Action/Form thường gặp trong Struts 1 và vai trò của Tiles, Validator.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `DispatchAction` (nhiều method trong một Action, chọn theo parameter như `method=save`), `LookupDispatchAction` (chọn method theo nhãn nút qua message resources), `MappingDispatchAction` (method theo cấu hình mapping), `DynaActionForm` (form khai báo trong XML, không cần class), `ValidatorForm` (dùng Validator). **Tiles**: layout template header/menu/body/footer qua `tiles-defs.xml`, forward trỏ tới tên definition. **Validator**: validation khai báo qua `validator-rules.xml` + `validation.xml`, sinh cả JavaScript phía client.

**Giải thích chi tiết:**
- `DispatchAction` với tham số `method` từ client: cần kiểm soát danh sách method được phép, tránh gọi method nhạy cảm.
- Khi migration: Tiles → Thymeleaf Layout Dialect/fragment; Validator → Bean Validation.

**Câu hỏi nối tiếp:**
- *Validation chỉ ở client có đủ không?* → Không, luôn phải validate phía server (Validator chạy cả hai phía).

**⚠️ Câu trả lời gây điểm trừ:**
- Không phân biệt được Tiles (layout) với Validator.

**📖 Ôn lại:** [3.3 Ví dụ cấu hình và code](../01-giao-trinh/10-struts.md#3-struts1) · [3.5 Tiles và Validator](../01-giao-trinh/10-struts.md#3-struts1)

</details>

### Q9. 🟡 Form POST tiếng Việt lưu vào DB bị lỗi font ("Nguyá»…n"). Bạn kiểm tra những gì?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Container đọc parameter theo ISO-8859-1 nếu chưa đặt encoding. Kiểm tra theo chuỗi: (1) Filter `request.setCharacterEncoding("UTF-8")` (ví dụ Spring `CharacterEncodingFilter`) đặt **trước** mọi thứ đọc parameter — trước `ActionServlet`/Struts filter và trước bất kỳ filter nào gọi `getParameter`; (2) JSP `pageEncoding="UTF-8"` và `contentType="text/html; charset=UTF-8"`; (3) với GET: `URIEncoding="UTF-8"` trên Tomcat connector (Tomcat 8+ mặc định UTF-8); (4) phía DB: charset của connection/cột (MySQL `utf8mb4`, Oracle `AL32UTF8`/`NVARCHAR2`); (5) file `.properties` thông báo.

**Giải thích chi tiết:**
- `setCharacterEncoding` vô tác dụng nếu parameter đã bị đọc trước đó — thứ tự filter quan trọng.
- Struts 2: `struts.i18n.encoding=UTF-8`.
- Debug: log byte nhận được (hex) để xác định lỗi ở tầng nào — đọc request, lưu DB hay hiển thị.

**Câu hỏi nối tiếp:**
- *Java 9+ đọc `.properties` thế nào?* → `PropertyResourceBundle` mặc định UTF-8 (JEP 226); codebase cũ dùng `native2ascii` — đừng trộn.

**⚠️ Câu trả lời gây điểm trừ:**
- Sửa bằng `new String(s.getBytes("ISO-8859-1"), "UTF-8")` rải trong từng Action.

**📖 Ôn lại:** [3.4 — Mã hóa tiếng Việt](../01-giao-trinh/10-struts.md#3-struts1)

</details>

### Q10. 🔴 Struts 1 đã EOL. CVE-2014-0114 là gì? Nếu chưa thể migrate ngay, bạn giảm thiểu rủi ro thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Apache công bố Struts 1 End-of-Life tháng 4/2013 (bản cuối 1.3.10, 2008) — không còn bản vá chính thức. CVE-2014-0114: Commons BeanUtils populate tham số `class.classLoader...` vào `ActionForm` (mọi object đều có `getClass()`) → thao túng ClassLoader, có thể dẫn tới RCE trên một số môi trường. Giảm thiểu: **Filter** chặn parameter có tên khớp `(^|\W)[cC]lass\W` (trả 400 + log), dùng Commons BeanUtils bản có `SuppressPropertiesBeanIntrospector`, đặt sau WAF, chạy app server với quyền tối thiểu, cô lập mạng — và lập kế hoạch migration.

**Giải thích chi tiết:**
```java
private static final Pattern BAD = Pattern.compile("(^|\\W)[cC]lass\\W");
// "class.classLoader..." → chặn; "user.class.x" → chặn; "classification" → KHÔNG chặn
```
- `classification` không khớp vì sau `class` là chữ cái (`\w`), không phải ký tự phân cách.
- Spring4Shell (CVE-2022-22965) là cùng "họ" lỗ hổng: data binding chạm vào `class.module.classLoader` trên JDK 9+.
- Có fork cộng đồng vá Struts 1 cho Jakarta EE nhưng cần đánh giá độ tin cậy trước khi dùng cho hệ thống tài chính.

**Câu hỏi nối tiếp:**
- *Viết test cho filter?* → `?class.classLoader.resources.dirContext.docBase=...` bị 400; parameter hợp lệ như `classification` đi qua.

**⚠️ Câu trả lời gây điểm trừ:**
- "Nâng lên Struts 2 là xong" — không phải nâng cấp mà là viết lại.
- Không biết Struts 1 đã EOL.

**📖 Ôn lại:** [3.6 Struts 1 EOL](../01-giao-trinh/10-struts.md#3-struts1)

</details>

---

<a id="nhom-c"></a>
## C. Struts 2: kiến trúc, interceptor, action

### Q11. 🟢 Struts 2 khác Struts 1 ở những điểm nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Struts 2 thực chất là WebWork 2 mang thương hiệu Struts — **kiến trúc khác hoàn toàn**. Front controller là Filter thay vì Servlet; **Action mới cho mỗi request** (field là dữ liệu của request, an toàn) thay vì singleton; Action là POJO `execute()` không tham số trả `String`, không phụ thuộc Servlet API → dễ test; không có `ActionForm` riêng — dữ liệu form là property của chính Action (hoặc model qua `ModelDriven`); OGNL + ValueStack thay cho EL/taglib; **interceptor stack** cấu hình được thay cho `RequestProcessor` cố định.

**Giải thích chi tiết:**
| Struts 1 | Struts 2 |
|---|---|
| `ActionServlet` | `StrutsPrepareAndExecuteFilter` |
| Action singleton | Action per request |
| `execute(mapping, form, req, resp)` | `execute()` / method tùy ý trả `String` |
| `ActionForm` | Property của Action / `ModelDriven` |
| `RequestProcessor` | Interceptor stack |

- Servlet API trong Struts 2 lấy qua `ServletActionContext` hoặc `*Aware` interface (`SessionAware`, `ServletRequestAware`).

**Câu hỏi nối tiếp:**
- *Thành phần nào của Struts 2 vẫn phải thread-safe?* → Interceptor (singleton), result type, và action nếu là Spring bean singleton.

**⚠️ Câu trả lời gây điểm trừ:**
- "Struts 2 là phiên bản nâng cấp của Struts 1, migrate dễ."

**📖 Ôn lại:** [4.1 Thay đổi tư duy so với Struts 1](../01-giao-trinh/10-struts.md#4-struts2-core)

</details>

### Q12. 🟡 Mô tả luồng xử lý một request trong Struts 2 từ Filter tới Result. Vai trò của `ActionProxy`, `ActionInvocation`, `ActionContext`?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `StrutsPrepareAndExecuteFilter` **prepare**: tạo `ActionContext`, ValueStack, đặt encoding/locale, `ActionMapper` phân tích URL thành `ActionMapping` (namespace, name, method). **Execute**: `Dispatcher.serviceAction()` tạo `ActionProxy` từ cấu hình (struts.xml/annotation) → `ActionInvocation.invoke()` gọi lần lượt các interceptor; mỗi interceptor gọi `invocation.invoke()` để chuyển tiếp → `params` set request params vào action qua OGNL → `validation`, `workflow` (lỗi → trả `input`, không gọi action) → method action trả result code → **Result** được thực thi → các interceptor chạy phần "sau" theo thứ tự ngược lại.

**Giải thích chi tiết:**
- **ActionProxy**: trung gian giữa framework và Action (Proxy pattern), có thể dùng để gọi action ngoài HTTP (test).
- **ActionInvocation**: giữ trạng thái thực thi — action, iterator interceptor, result code.
- **ActionContext**: container theo **ThreadLocal** chứa ValueStack, parameters, session, application.

**Câu hỏi nối tiếp:**
- *Vì sao `ActionContext` là ThreadLocal lại là vấn đề khi chạy tác vụ async?* → Thread khác không thấy context → `ServletActionContext.getRequest()` trả null; phải truyền dữ liệu tường minh.

**⚠️ Câu trả lời gây điểm trừ:**
- Không nhắc được interceptor stack hay vị trí của Result trong chuỗi.

**📖 Ôn lại:** [4.2 Luồng request](../01-giao-trinh/10-struts.md#4-struts2-core)

</details>

### Q13. 🟡 Viết một interceptor kiểm tra đăng nhập và đo thời gian xử lý. Cần lưu ý gì về thread-safety?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Kế thừa `AbstractInterceptor`, trong `intercept(ActionInvocation)`: chưa đăng nhập → `return "login"` (dừng chuỗi, action không được gọi); ngược lại đo thời gian quanh `invocation.invoke()`. Interceptor là **singleton** (một instance cho mỗi khai báo) → không được có field mang trạng thái request; dữ liệu đo thời gian để trong biến cục bộ.

**Giải thích chi tiết:**
```java
public class AuthInterceptor extends AbstractInterceptor {
    @Override
    public String intercept(ActionInvocation invocation) throws Exception {
        Map<String, Object> session = invocation.getInvocationContext().getSession();
        if (session.get("user") == null) return "login";
        long start = System.nanoTime();                  // biến cục bộ — thread-safe
        try {
            return invocation.invoke();
        } finally {
            log.info("{} took {} ms", invocation.getProxy().getActionName(),
                     (System.nanoTime() - start) / 1_000_000);
        }
    }
}
```
```xml
<interceptor-stack name="secureStack">
  <interceptor-ref name="auth"/>
  <interceptor-ref name="defaultStack"/>
</interceptor-stack>
<default-interceptor-ref name="secureStack"/>
<global-results>
  <result name="login" type="redirectAction"><param name="actionName">login</param></result>
</global-results>
```
- Khai báo `<interceptor-ref>` riêng cho một action sẽ **thay thế** stack mặc định — quên thêm `defaultStack` là mất toàn bộ binding/validation.

**Câu hỏi nối tiếp:**
- *Thời gian đo được gồm cả render JSP không?* → Có, vì result chạy trước khi stack unwind (Q14).

**⚠️ Câu trả lời gây điểm trừ:**
- Lưu `startTime` vào field của interceptor.

**📖 Ôn lại:** [4.3 Interceptor](../01-giao-trinh/10-struts.md#4-struts2-core)

</details>

### Q14. 🔴 Một interceptor muốn thêm header HTTP sau khi action chạy xong, bằng cách set header sau dòng `invocation.invoke()`. Header không xuất hiện. Vì sao? Sửa thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Trong `DefaultActionInvocation.invoke()`, khi hết interceptor thì gọi `invokeActionOnly()` rồi **`executeResult()` ngay tại "đáy" chuỗi** — nghĩa là result (ví dụ `dispatcher` render JSP) đã chạy và response thường đã **commit** trước khi các interceptor "unwind". Set header sau `invoke()` là quá muộn. Sửa: đăng ký `PreResultListener` (`invocation.addPreResultListener(...)`) để chạy logic sau action nhưng **trước** result; hoặc dùng Servlet Filter/`HttpServletResponseWrapper`.

**Giải thích chi tiết:**
```java
public String intercept(ActionInvocation invocation) throws Exception {
    invocation.addPreResultListener((inv, resultCode) ->
        ServletActionContext.getResponse().setHeader("X-Result", resultCode));
    return invocation.invoke();
}
```
- Hệ quả khác: interceptor không thể "đổi" result code sau khi result đã thực thi; exception trong JSP xảy ra bên trong `invoke()`.
- Đây là câu kiểm tra ứng viên có thật sự đọc mã nguồn/hiểu cơ chế đệ quy của interceptor chain.

**Câu hỏi nối tiếp:**
- *Spring MVC tương tự thế nào?* → `HandlerInterceptor.postHandle` chạy trước render view nhưng với `@ResponseBody` response đã ghi xong; dùng `ResponseBodyAdvice` hoặc filter.

**⚠️ Câu trả lời gây điểm trừ:**
- "Do thứ tự interceptor sai" mà không giải thích được result chạy ở đâu.

**📖 Ôn lại:** [4.3 Interceptor](../01-giao-trinh/10-struts.md#4-struts2-core) · [Bài 4.3 — Đọc mã nguồn ActionInvocation](../01-giao-trinh/10-struts.md#4-struts2-core)

</details>

### Q15. 🟡 `ModelDriven` và `Preparable` dùng để làm gì? Vì sao khi sửa bản ghi người ta dùng `paramsPrepareParamsStack`?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `ModelDriven<T>`: action trả về model qua `getModel()`, model được push lên ValueStack **trên** action → parameter bind trực tiếp vào model (`name` thay vì `product.name`). `Preparable`: method `prepare()` chạy trước khi bind params — dùng để load entity cần sửa. Vấn đề: với `defaultStack`, `prepare` chạy **trước** `params` nên chưa có `id` để load. `paramsPrepareParamsStack` chạy `params` → `prepare` (load theo id) → `params` lần nữa (bind dữ liệu form lên object vừa load).

**Giải thích chi tiết:**
```java
public class ProductAction extends ActionSupport implements Preparable, ModelDriven<Product> {
    private Long id;
    private Product product = new Product();
    @Override public void prepare() { if (id != null) product = productService.find(id); }
    @Override public Product getModel() { return product; }
    public String save() { productService.save(product); return SUCCESS; }
    public void setId(Long id) { this.id = id; }
}
```
- Bind thẳng lên **entity** = rủi ro mass assignment (Q16); tốt hơn bind lên DTO/form object rồi copy field cho phép.

**Câu hỏi nối tiếp:**
- *Có `name` ở cả action và model thì `<s:property value="name"/>` lấy cái nào?* → Model (nằm trên action trong stack); lấy của action bằng `[1].name`.

**⚠️ Câu trả lời gây điểm trừ:**
- Không biết thứ tự `prepare` vs `params` trong defaultStack.

**📖 Ôn lại:** [4.4 ActionSupport, ModelDriven, Preparable](../01-giao-trinh/10-struts.md#4-struts2-core)

</details>

### Q16. 🔴 Action sau có lỗ hổng gì? Kẻ tấn công khai thác bằng request nào? Sửa ra sao?

```java
public class ProfileAction extends ActionSupport implements SessionAware {
    private Map<String, Object> session;
    public User getUser() { return (User) session.get("user"); }   // để JSP hiển thị
    public void setSession(Map<String, Object> s) { this.session = s; }
    public String update() { userService.save(getUser()); return SUCCESS; }
}
```

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **Mass assignment**: interceptor `params` bind **mọi** parameter khớp chuỗi getter/setter qua OGNL. `getUser()` trả object trong session nên request `POST /profile_update?user.admin=true&user.balance=999999` sẽ gọi `getUser().setAdmin(true)` → leo thang quyền rồi được `save`. Sửa: bind lên **DTO riêng** chỉ chứa field được phép sửa; giới hạn tham số bằng `excludeParams`/`acceptParamNames`, `ParameterNameAware`, hoặc **`@StrutsParameter`** (Struts 6.4+, bật `struts.parameters.requireAnnotations=true`; mặc định bắt buộc ở Struts 7) — chỉ setter/getter có annotation mới được bind.

**Giải thích chi tiết:**
```java
public class ProfileAction extends ActionSupport {
    private ProfileForm form = new ProfileForm();          // chỉ fullName, phone
    @StrutsParameter(depth = 1)
    public ProfileForm getForm() { return form; }
    public String update() { userService.updateProfile(currentUserId(), form); return SUCCESS; }
}
```
- Getter chỉ để hiển thị thì không đặt annotation → không bind được qua chuỗi getter.
- Spring MVC cũng bị mass assignment nếu dùng entity làm `@ModelAttribute` — nguyên tắc chung: **DTO riêng cho từng form**, `@InitBinder` + `setAllowedFields` khi cần.

**Câu hỏi nối tiếp:**
- *Có liên quan tới RCE không?* → Binding qua OGNL là cùng bề mặt tấn công với các lỗ hổng như CVE-2014-0114 (`class.classLoader`) — giới hạn binding là phòng thủ chung.

**⚠️ Câu trả lời gây điểm trừ:**
- "JSP không có field admin nên không ai gửi được" — attacker tự tạo request.

**📖 Ôn lại:** [4.4 — lỗi thường gặp mass assignment](../01-giao-trinh/10-struts.md#4-struts2-core) · [8.2 Checklist hardening (mục 6)](../01-giao-trinh/10-struts.md#8-security)

</details>

### Q17. 🟡 Wildcard mapping `product_*` + `method="{1}"` tiện nhưng có rủi ro gì? Dynamic Method Invocation (DMI) là gì?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Wildcard cho phép URL quyết định **method nào của action được gọi** (`product_delete` → `delete()`). Không giới hạn thì attacker gọi được mọi method public không tham số trả `String` — kể cả method nội bộ không định expose. DMI (`action!method`, ví dụ `/product!delete.action`) còn mở rộng hơn. Phòng: `struts.enable.DynamicMethodInvocation=false`, `strict-method-invocation="true"` (mặc định từ 2.5) + `<allowed-methods>list,input,save</allowed-methods>`.

**Giải thích chi tiết:**
```xml
<package name="default" namespace="/" extends="struts-default" strict-method-invocation="true">
  <action name="product_*" class="productAction" method="{1}">
    <allowed-methods>list,input,save</allowed-methods>
    ...
  </action>
</package>
```
- Kiểm thử: gọi method ngoài danh sách phải trả 404/lỗi config — đưa vào test tự động.

**Câu hỏi nối tiếp:**
- *Spring MVC có vấn đề tương tự không?* → Ít hơn: mỗi endpoint phải được khai báo tường minh bằng `@RequestMapping`.

**⚠️ Câu trả lời gây điểm trừ:**
- Không biết DMI tồn tại hoặc nghĩ nó vô hại.

**📖 Ôn lại:** [5.2 Results và wildcard](../01-giao-trinh/10-struts.md#5-struts2-ognl) · [5.3 struts.xml và hằng số](../01-giao-trinh/10-struts.md#5-struts2-ognl)

</details>

---

<a id="nhom-d"></a>
## D. ValueStack, OGNL, results, cấu hình

### Q18. 🟡 ValueStack và OGNL là gì? OGNL được dùng ở những chiều nào trong Struts 2?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **OGNL** (Object-Graph Navigation Language) là ngôn ngữ biểu thức đọc/ghi thuộc tính theo đường dẫn (`product.category.name`), gọi method, truy cập static, tạo collection. **ValueStack** là stack object (root `CompoundRoot`); action được push lên mỗi request (model của `ModelDriven` nằm trên action). Khi đánh giá `name`, OGNL tìm từ đỉnh stack xuống object đầu tiên có property `name`. Ngoài root có **context map** truy cập bằng `#`: `#session`, `#request`, `#parameters`, `#application`, `#attr`. OGNL dùng **hai chiều**: ghi (`params` interceptor biến `product.name=Laptop` thành `getProduct().setName("Laptop")`) và đọc (tag `<s:property value="product.name"/>`).

**Giải thích chi tiết:**
- `<s:property>` escape HTML mặc định — đừng tắt `escapeHtml` với dữ liệu người dùng (XSS).
- `<s:debug/>` (chỉ devMode) để xem stack khi debug.

**Câu hỏi nối tiếp:**
- *Vì sao OGNL là bề mặt tấn công?* → Q19.

**⚠️ Câu trả lời gây điểm trừ:**
- Coi OGNL chỉ là "EL của Struts" mà không biết nó gọi được method/static.

**📖 Ôn lại:** [5.1 ValueStack và OGNL](../01-giao-trinh/10-struts.md#5-struts2-ognl)

</details>

### Q19. 🔴 Vì sao gần như mọi RCE nghiêm trọng của Struts 2 đều liên quan OGNL? Đoạn JSP sau nguy hiểm thế nào?

```jsp
<s:a id="%{skillName}" href="...">Chi tiết</s:a>   <%-- skillName lấy từ request parameter --%>
```

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Sức mạnh của OGNL (gọi method, truy cập static, tạo object) chính là bề mặt tấn công. Mẫu chung của các RCE: **dữ liệu do attacker kiểm soát bị đánh giá như biểu thức OGNL** — qua header (`Content-Type` ở S2-045), namespace URL (S2-057), tên parameter, hoặc thuộc tính tag `%{...}` — rồi biểu thức vượt sandbox (`SecurityMemberAccess`, `#_memberAccess`) để gọi `Runtime.exec`. Đoạn JSP trên là mẫu **forced double OGNL evaluation** (S2-061/S2-062, CVE-2020-17530/CVE-2021-31805): `%{skillName}` được đánh giá ra giá trị người dùng gửi, và nếu giá trị đó lại là `%{...}` thì có thể bị đánh giá lần nữa → thực thi biểu thức tùy ý.

**Giải thích chi tiết:**
- Quy tắc: **không bao giờ** đặt input người dùng vào `%{}` trong thuộc tính tag; truyền dữ liệu qua property bình thường và để tag escape.
- Giữ cấu hình sandbox mặc định: `struts.excludedClasses`, `struts.excludedPackageNames`, `struts.ognl.expressionMaxLength`; từ 6.x có allowlist OGNL (`struts.allowlist.enable`).
- Dấu hiệu tấn công trong log: `%{`, `${`, `#_memberAccess`, `@java.lang.Runtime@` trong header/parameter.

**Câu hỏi nối tiếp:**
- *Phòng thủ nhiều lớp ngoài vá?* → WAF virtual patching, chạy với user ít quyền, không cho ghi thư mục webapp, egress filtering, giám sát process con lạ (`/bin/sh` sinh từ JVM).

**⚠️ Câu trả lời gây điểm trừ:**
- "Struts có lỗi vì code kém" — không chỉ ra được cơ chế đánh giá biểu thức từ input.

**📖 Ôn lại:** [5.1 — Góc nhìn Senior về OGNL](../01-giao-trinh/10-struts.md#5-struts2-ognl) · [8.1 Các lỗ hổng nổi tiếng](../01-giao-trinh/10-struts.md#8-security)

</details>

### Q20. 🟡 Kể các result type của Struts 2. Vì sao nên hạn chế `chain`? Download file lớn dùng result nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `dispatcher` (mặc định, forward tới JSP), `redirect` (tới URL), `redirectAction` (tới action khác — dùng cho PRG), `chain` (gọi tiếp action khác trong **cùng request**), `stream` (trả `InputStream` — download), `json` (plugin), `freemarker`, `tiles`, `httpheader`, `plainText`. Hạn chế `chain` vì luồng khó theo dõi, các action chia sẻ ValueStack → dữ liệu lẫn nhau, khó test; thường thay bằng gọi service chung hoặc `redirectAction`. Download dùng `stream` với `inputName`, `contentType`, `contentDisposition`, `bufferSize`.

**Giải thích chi tiết:**
```xml
<result name="success" type="stream">
  <param name="contentType">text/csv; charset=UTF-8</param>
  <param name="inputName">csvStream</param>
  <param name="contentDisposition">attachment; filename*=UTF-8''${encodedFileName}</param>
  <param name="bufferSize">8192</param>
</result>
```
- Không đọc toàn bộ file vào bộ nhớ; tên file tiếng Việt dùng `filename*=UTF-8''...` (RFC 5987), encode `+` thành `%20`.

**Câu hỏi nối tiếp:**
- *Tương đương bên Spring?* → `ResponseEntity<StreamingResponseBody>` hoặc `Resource`.

**⚠️ Câu trả lời gây điểm trừ:**
- Dùng `chain` cho PRG (vẫn cùng request → F5 submit lại).

**📖 Ôn lại:** [5.2 Results và result types](../01-giao-trinh/10-struts.md#5-struts2-ognl)

</details>

### Q21. 🔴 Kịch bản: codebase Struts 2 lạ dùng cả `struts.xml` lẫn Convention plugin. URL `/admin/contract-approve` đang trả về trang sai. Bạn tìm ra action xử lý URL này thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** (1) Ở môi trường dev, bật plugin **config-browser** (`/config-browser/index.action`) để xem toàn bộ cấu hình runtime: namespace, action, class, method, result — đây là nguồn sự thật vì nó phản ánh cả XML lẫn annotation; (2) grep `struts.xml` và các file `<include>` theo `namespace="/admin"` và `name="contract-approve"` hoặc wildcard khớp; (3) theo quy ước Convention: class trong package chứa `action/actions/struts/struts2`, tên kết thúc `Action` — `ContractApproveAction` → `/contract-approve`, JSP `/WEB-INF/content/contract-approve-success.jsp`; kiểm tra `@Namespace`, `@Action`, `@Result`; (4) bật log `org.apache.struts2` mức DEBUG để thấy mapping thực tế; (5) kiểm tra `struts.action.excludePattern` và filter phía trước có chặn/chuyển URL không.

**Giải thích chi tiết:**
- Cùng URL được map ở hai nơi là lỗi phổ biến khi trộn XML và Convention — rất khó debug nếu chỉ đọc code.
- config-browser **không bao giờ** bật ở production (lộ toàn bộ cấu hình).
- Ghi lại kết quả thành bảng URL → action → result → JSP — vừa là tài liệu, vừa là đầu vào cho bảng ánh xạ migration.

**Câu hỏi nối tiếp:**
- *Đếm tổng số action để ước lượng migration?* → `grep -c "<action "` cho XML; với Convention tìm class `*Action`; đối chiếu config-browser.

**⚠️ Câu trả lời gây điểm trừ:**
- Chỉ grep XML, bỏ qua Convention/annotation.

**📖 Ôn lại:** [5.4 Convention plugin](../01-giao-trinh/10-struts.md#5-struts2-ognl)

</details>

### Q22. 🟡 Những hằng số nào trong `struts.xml` bạn kiểm tra đầu tiên khi review cấu hình production?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `struts.devMode=false` (true ở production lộ thông tin, mở tính năng debug); `struts.enable.DynamicMethodInvocation=false`; `strict-method-invocation="true"` + `allowed-methods` trên package; `struts.i18n.encoding=UTF-8`; `struts.multipart.maxSize` (giới hạn upload); `struts.mapper.alwaysSelectFullNamespace` **không** để `true` khi không cần (liên quan S2-057); `struts.parameters.requireAnnotations=true` nếu bản 6.4+; cấu hình sandbox OGNL giữ mặc định; `struts.action.excludePattern` đúng phạm vi (nếu chung WAR với Spring MVC).

**Giải thích chi tiết:**
```xml
<constant name="struts.devMode" value="false"/>
<constant name="struts.enable.DynamicMethodInvocation" value="false"/>
<constant name="struts.multipart.maxSize" value="10485760"/>
<constant name="struts.action.excludePattern" value="/api/.*,/v2/.*"/>
```
- Viết integration test kiểm tra các giá trị runtime (đọc từ container của `Dispatcher`) để cấu hình không bị "trôi" khi ai đó sửa.

**Câu hỏi nối tiếp:**
- *Plugin nào nên gỡ?* → REST plugin dùng XStream nếu không dùng (S2-052), config-browser, Convention nếu không dùng.

**⚠️ Câu trả lời gây điểm trừ:**
- Không nhắc devMode/DMI.

**📖 Ôn lại:** [5.3 struts.xml và hằng số quan trọng](../01-giao-trinh/10-struts.md#5-struts2-ognl) · [8.2 Checklist hardening](../01-giao-trinh/10-struts.md#8-security)

</details>

---

<a id="nhom-e"></a>
## E. Validation, i18n, upload, double submit

### Q23. 🟡 Mở trang danh sách sản phẩm (`product_list`) lại hiện lỗi "Tên sản phẩm không được để trống". Vì sao?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Validation (XML `ProductAction-validation.xml`, annotation, hoặc `validate()`) chạy cho **mọi method** của action, kể cả `list`. Interceptor `validation` chạy validator, `workflow` thấy `hasErrors()` → trả `input`. Mặc định `defaultStack` chỉ loại các method `input, back, cancel, browse`. Sửa: `@SkipValidation` trên `list()`, cấu hình `excludeMethods`, hoặc dùng file validation theo alias (`ProductAction-product_save-validation.xml`) để chỉ áp cho action save.

**Giải thích chi tiết:**
- Ba cách validation: override `validate()`/`validateSave()`, XML theo class hoặc alias, annotation (`@RequiredStringValidator`...).
- Lỗi chuyển kiểu do `conversionError` interceptor thêm vào field errors (Q27).
- Validator `regex` cho số điện thoại VN: `^(0|\+84)(3|5|7|8|9)\d{8}$`.

**Câu hỏi nối tiếp:**
- *Tương đương Spring?* → Bean Validation chỉ chạy khi có `@Valid` trên tham số — không có vấn đề này.

**⚠️ Câu trả lời gây điểm trừ:**
- Sửa bằng cách xóa validator.

**📖 Ôn lại:** [6.1 Validation framework](../01-giao-trinh/10-struts.md#6-struts2-features)

</details>

### Q24. 🟢 Struts 2 tìm message i18n theo thứ tự nào? Người dùng đổi ngôn ngữ ra sao?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Thứ tự tìm key: `ActionClass.properties` → các lớp cha/interface → `package.properties` theo cây package → global resource (`struts.custom.i18n.resources=messages` → `messages_vi.properties`, `messages_en.properties`). Code dùng `getText("key")`, JSP dùng `<s:text name="key"/>` hoặc thuộc tính `key` của form tag. Đổi ngôn ngữ: interceptor `i18n` đọc parameter `request_locale=vi` và lưu locale vào session.

**Giải thích chi tiết:**
- Java 9+ đọc `.properties` bằng UTF-8 mặc định; file cũ có thể đã `native2ascii` (`\uXXXX`) — thống nhất một kiểu.
- Khi migration: `MessageSource` + `LocaleResolver` + `LocaleChangeInterceptor` của Spring.

**Câu hỏi nối tiếp:**
- *Thông báo validation đa ngôn ngữ?* → `<message key="error.name.required"/>` trong file validation.

**⚠️ Câu trả lời gây điểm trừ:**
- Hard-code chuỗi tiếng Việt trong Action.

**📖 Ôn lại:** [6.2 i18n](../01-giao-trinh/10-struts.md#6-struts2-features)

</details>

### Q25. 🔴 Hệ thống của bạn dùng interceptor `fileUpload` cũ. Bản tin S2-067 (CVE-2024-53677) vừa ra. Nâng phiên bản Struts có đủ không? Thiết kế upload an toàn thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **Không đủ.** S2-066 (CVE-2023-50164) và S2-067 nằm ở logic upload cũ: attacker thao túng parameter upload (ví dụ khác biệt hoa/thường tên parameter) để vượt kiểm tra tên file → path traversal, ghi webshell → RCE. Bản vá S2-067 yêu cầu **chuyển sang cơ chế mới** `actionFileUpload` + `UploadedFilesAware` (Struts 6.4.0+) — chỉ nâng version mà vẫn dùng `fileUpload` cũ vẫn bị ảnh hưởng. Đây là ví dụ bản vá đòi hỏi **thay đổi code**.

**Giải thích chi tiết:**
Quy tắc upload an toàn (độc lập framework):
- **Không bao giờ** dùng tên file của client để dựng đường dẫn lưu; tên do server sinh (UUID) + extension từ whitelist content type.
- Lưu **ngoài web root**, không cho thực thi; phục vụ lại qua action/CDN với `Content-Disposition: attachment` và `X-Content-Type-Options: nosniff`.
- Giới hạn kích thước (`struts.multipart.maxSize`, `maximumSize`), whitelist `allowedTypes`/`allowedExtensions`, kiểm tra magic bytes, quét malware với hệ thống tài chính.
```java
public class AvatarAction extends ActionSupport implements UploadedFilesAware {
    private UploadedFile avatar;
    @Override public void withUploadedFiles(List<UploadedFile> files) {
        if (!files.isEmpty()) this.avatar = files.get(0);
    }
    public String execute() throws IOException {
        String ext = switch (avatar.getContentType()) {
            case "image/png" -> ".png";
            case "image/jpeg" -> ".jpg";
            default -> throw new IllegalArgumentException("Unsupported type");
        };
        Files.copy(((File) avatar.getContent()).toPath(), storageDir.resolve(UUID.randomUUID() + ext));
        return SUCCESS;
    }
}
```

**Câu hỏi nối tiếp:**
- *Trong lúc chưa kịp sửa code?* → Tắt chức năng upload hoặc rule WAF chặn request multipart bất thường, kiểm tra thư mục webapp có file `.jsp` lạ.

**⚠️ Câu trả lời gây điểm trừ:**
- "Nâng version là xong."
- Lưu file theo `uploadFileName` của client.

**📖 Ôn lại:** [6.3 File upload](../01-giao-trinh/10-struts.md#6-struts2-features) · [8.1 Các lỗ hổng nổi tiếng](../01-giao-trinh/10-struts.md#8-security)

</details>

### Q26. 🟡 `token` và `tokenSession` interceptor khác nhau thế nào? Với chức năng chuyển khoản, token có đủ chống double submit không?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `<s:token/>` sinh hidden field token và lưu vào session. `token`: submit lần hai với token đã dùng → result `invalid.token` (người dùng thấy lỗi). `tokenSession`: submit trùng sẽ **chờ và trả lại kết quả của lần đầu** (người dùng không thấy lỗi). Token **không đủ** cho chuyển khoản: không chặn khi hai tab mở hai form khác nhau, retry ở tầng mạng/proxy, hay nhiều node không chia sẻ session. Cần **idempotency ở tầng nghiệp vụ/DB**: sinh `transferRequestId` khi render form, lưu cùng giao dịch với `UNIQUE` constraint — lần hai vi phạm unique → trả kết quả giao dịch đã có.

**Giải thích chi tiết:**
| Lớp bảo vệ | Chặn được |
|---|---|
| Disable nút submit (JS) | Double-click thông thường |
| PRG | F5 sau khi thành công |
| Token/tokenSession | Submit lặp cùng form, cùng session |
| Idempotency key + unique constraint | Mọi trường hợp lặp, kể cả đa tab, retry, đa node |

**Câu hỏi nối tiếp:**
- *CSRF token của Spring Security có thay token Struts được không?* → Không, khác mục đích: CSRF token chống request giả mạo, không chống double submit.

**⚠️ Câu trả lời gây điểm trừ:**
- "Có token rồi là an toàn tuyệt đối."

**📖 Ôn lại:** [6.4 Token interceptor chống double submit](../01-giao-trinh/10-struts.md#6-struts2-features)

</details>

### Q27. 🟢 Người dùng nhập "abc" vào ô giá (field `BigDecimal price` của action). Chuyện gì xảy ra?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Type conversion của Struts 2 thất bại khi set `price`; interceptor `conversionError` thêm lỗi vào **field errors** (thông báo mặc định kiểu "Invalid field value for field price", tùy chỉnh bằng key `invalid.fieldvalue.price`), giá trị gốc "abc" được giữ để hiển thị lại trong form; `workflow` thấy `hasErrors()` → trả result `input`, method action **không được gọi**.

**Giải thích chi tiết:**
- Struts 1 không có cơ chế này — đó là lý do `ActionForm` hay khai báo mọi field là `String` rồi tự parse trong `validate()`.
- Spring MVC: lỗi binding vào `BindingResult` (`typeMismatch`), có thể map message qua `messages.properties`.

**Câu hỏi nối tiếp:**
- *Thiếu result `input` trong cấu hình action thì sao?* → Lỗi "No result defined for action ... and result input" — rất hay gặp khi thêm validation cho action cũ.

**⚠️ Câu trả lời gây điểm trừ:**
- "Ném `NumberFormatException` ra trang lỗi 500."

**📖 Ôn lại:** [6.1 Validation framework](../01-giao-trinh/10-struts.md#6-struts2-features)

</details>

---

<a id="nhom-f"></a>
## F. Tích hợp Spring & Hibernate

### Q28. 🔴 Sau khi chuyển action Struts 2 sang khai báo bằng Spring bean, khách hàng báo thỉnh thoảng thấy dữ liệu form của người khác. Nguyên nhân?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Action được khai báo là Spring bean **thiếu `scope="prototype"`** → Spring mặc định **singleton** → mọi request dùng chung một action instance, mà action Struts 2 lưu dữ liệu form trong **field** → dữ liệu người dùng này lộ sang người dùng khác (đúng loại lỗi của Struts 1). Sửa: `scope="prototype"` (hoặc `@Scope("prototype")`), hoặc dùng **tên class đầy đủ** trong `class=` của struts.xml — Struts tự tạo instance mới mỗi request và nhờ Spring autowire dependency (an toàn hơn).

**Giải thích chi tiết:**
```xml
<!-- struts.xml: class = id của Spring bean -->
<action name="product_*" class="productAction" method="{1}"> ... </action>
<!-- applicationContext.xml -->
<bean id="productAction" class="com.shop.web.ProductAction" scope="prototype">
  <constructor-arg ref="productService"/>
</bean>
```
- Cấu hình liên quan: `struts.objectFactory=spring`, `struts.objectFactory.spring.autoWire=name|type|constructor|auto`.
- Lỗi chỉ lộ dưới tải đồng thời → test bằng JMeter 2 user submit khác nhau cùng lúc.

**Câu hỏi nối tiếp:**
- *Với component scan và `@Component` trên action?* → Cùng vấn đề — phải `@Scope("prototype")`.

**⚠️ Câu trả lời gây điểm trừ:**
- "Struts 2 action luôn per request nên không thể có lỗi này."

**📖 Ôn lại:** [7.1 Struts 2 + Spring](../01-giao-trinh/10-struts.md#7-integration)

</details>

### Q29. 🟡 Trong hệ thống Struts + Spring + Hibernate cũ, bạn hay gặp những vấn đề gì ở tầng dữ liệu? Thứ tự filter trong `web.xml` nên thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** (1) `OpenSessionInViewFilter` để JSP truy cập lazy collection → giữ connection suốt render, N+1 ẩn trong JSP (mọi vấn đề OSIV của Module 09); (2) DAO dùng `HibernateTemplate`/`HibernateDaoSupport` của `orm.hibernate3` — Spring 5 đã gỡ hỗ trợ Hibernate 3/4 (chỉ còn `orm.hibernate5`) → nâng Spring kéo theo nâng Hibernate và sửa DAO; (3) transaction mở trong Action (`session.beginTransaction()`) → khó test, rò transaction khi exception. Hướng sửa: transaction ở service (`@Transactional`), `getCurrentSession()`/JPA `EntityManager`, JSP chỉ nhận DTO. Thứ tự filter: `CharacterEncodingFilter` → security filter → `OpenSessionInViewFilter` (nếu còn) → `StrutsPrepareAndExecuteFilter`.

**Giải thích chi tiết:**
- Struts 6 dùng `javax.servlet` → đi với Spring 5.3; Struts 7 (Jakarta EE) → Spring 6.
- Khi refactor bỏ OSIV: liệt kê JSP truy cập association, chuyển sang DTO với fetch theo use case.

**Câu hỏi nối tiếp:**
- *Vì sao encoding filter phải đứng đầu?* → Bất kỳ filter nào gọi `getParameter` trước nó sẽ khóa encoding sai (Q9).

**⚠️ Câu trả lời gây điểm trừ:**
- Giữ OSIV vì "đang chạy được".

**📖 Ôn lại:** [7.2 Hibernate trong hệ thống Struts legacy](../01-giao-trinh/10-struts.md#7-integration)

</details>

---

<a id="nhom-g"></a>
## G. Bảo mật & hardening

### Q30. 🟢 S2-045 (CVE-2017-5638) là gì? Vì sao nó nổi tiếng?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Lỗ hổng trong Jakarta Multipart parser: header `Content-Type` độc hại chứa biểu thức OGNL được **đánh giá khi xử lý thông báo lỗi** → **RCE không cần xác thực**, ảnh hưởng 2.3.5–2.3.31 và 2.5–2.5.10. Nổi tiếng vì là lỗ hổng trong vụ **Equifax 2017**: bản vá có từ tháng 3/2017 nhưng hệ thống không được cập nhật, dữ liệu của khoảng 147 triệu người bị lộ. Bài học: thất bại nằm ở **quy trình quản lý vá lỗi**, không phải thiếu kiến thức.

**Giải thích chi tiết:**
- S2-046 cùng gốc nhưng qua `Content-Disposition`/`Content-Length` của multipart → chặn một header ở WAF là không đủ.
- Dấu hiệu trong log: `Content-Type` chứa `%{`, `${`, `#_memberAccess`.

**Câu hỏi nối tiếp:**
- *Senior chịu trách nhiệm gì ở đây?* → Kiểm kê (SBOM), SCA trong CI, SLA vá Critical, theo dõi security bulletin.

**⚠️ Câu trả lời gây điểm trừ:**
- Không biết vụ Equifax hoặc mô tả sai là SQL injection.

**📖 Ôn lại:** [8.1 Các lỗ hổng nổi tiếng](../01-giao-trinh/10-struts.md#8-security)

</details>

### Q31. 🟡 S2-057 (CVE-2018-11776) dạy bài học gì về cấu hình?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Khi `struts.mapper.alwaysSelectFullNamespace=true` và action/result không khai báo namespace (hoặc dùng wildcard namespace), **namespace lấy từ URL bị đánh giá như OGNL** → RCE. Ảnh hưởng 2.3–2.3.34, 2.5–2.5.16. Bài học: cấu hình "tưởng vô hại" cũng mở RCE — review cấu hình là một phần của bảo mật, không chỉ review code; gỡ những gì không dùng.

**Giải thích chi tiết:**
- Liên quan: S2-052 (CVE-2017-9805) — REST plugin dùng XStream deserialize XML không an toàn → plugin không dùng là bề mặt tấn công thừa.
- Phòng: không bật `alwaysSelectFullNamespace` khi không cần, khai báo namespace tường minh cho package/result, nâng phiên bản.

**Câu hỏi nối tiếp:**
- *Làm sao đối chiếu CVE với hệ thống của mình?* → Bảng CVE → điều kiện ảnh hưởng (dùng upload? REST plugin? cấu hình namespace?) → có bị ảnh hưởng không → hành động.

**⚠️ Câu trả lời gây điểm trừ:**
- "Chỉ cần vá code, cấu hình không liên quan."

**📖 Ôn lại:** [8.1 Các lỗ hổng nổi tiếng](../01-giao-trinh/10-struts.md#8-security)

</details>

### Q32. 🔴 Đưa ra checklist hardening cho một ứng dụng Struts 2 đang chạy production.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** (1) Nâng lên bản mới nhất của nhánh còn hỗ trợ (6.x/7.x), đăng ký mailing list announcements; (2) `devMode=false`; (3) DMI tắt, `strict-method-invocation` + `allowed-methods`; (4) không dùng `%{...}` với dữ liệu người dùng; (5) giữ sandbox OGNL mặc định, bật allowlist nếu phiên bản hỗ trợ; (6) `@StrutsParameter` + `requireAnnotations=true` chống mass assignment; (7) chuyển sang `actionFileUpload`, whitelist type, lưu ngoài web root; (8) gỡ plugin không dùng (REST/XStream, config-browser, Convention), không bật `alwaysSelectFullNamespace`; (9) phòng thủ lớp ngoài: WAF, user ít quyền, webapp read-only, egress filtering, giám sát process con; (10) quy trình: SCA trong CI, SLA vá Critical (ví dụ 72 giờ), kiểm kê mọi ứng dụng Struts.

**Giải thích chi tiết:**
- Mỗi biện pháp nên có **test tự động**: devMode tắt, DMI tắt, method ngoài `allowed-methods` trả 404, parameter không có `@StrutsParameter` không được bind.
- Defense in depth: giả định lớp vá có thể bị vượt — lớp mạng/OS giới hạn thiệt hại (không ghi được webshell, không gọi ra ngoài được).

**Câu hỏi nối tiếp:**
- *Ưu tiên nếu chỉ có một tuần?* → Nâng version + upload mới (RCE trực tiếp), devMode/DMI, WAF; các mục còn lại theo lộ trình.

**⚠️ Câu trả lời gây điểm trừ:**
- Chỉ nói "cập nhật version" mà không có lớp phòng thủ/quy trình.

**📖 Ôn lại:** [8.2 Checklist hardening Struts 2](../01-giao-trinh/10-struts.md#8-security)

</details>

### Q33. 🔴 Kịch bản: tối thứ Sáu, Apache công bố bản tin Critical RCE cho Struts 2. Công ty có khoảng 20 ứng dụng Java. Bạn xử lý thế nào trong 24–72 giờ tới?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** (1) **Xác định phạm vi trong 1–2 giờ** nhờ SBOM/kiểm kê: ứng dụng nào dùng Struts, phiên bản nào, có dùng tính năng bị ảnh hưởng không (upload, plugin, cấu hình); (2) **giảm thiểu tạm thời** ngay: rule WAF (virtual patching), tắt tính năng bị ảnh hưởng (ví dụ upload), hạn chế truy cập từ Internet nếu được; (3) **vá + regression test** theo mức độ phơi nhiễm (Internet-facing trước), deploy có rollback; (4) **săn tìm dấu vết xâm nhập**: log request bất thường (`%{`, `#_memberAccess`), file `.jsp` lạ trong webapp, process con lạ, kết nối ra ngoài bất thường; (5) truyền thông với stakeholder theo mốc thời gian; (6) postmortem: cập nhật quy trình, SLA.

**Giải thích chi tiết:**
- Nếu bản vá đòi hỏi đổi code (như S2-067) → đánh giá effort, trong lúc đó giữ biện pháp tạm thời.
- Nếu phát hiện dấu hiệu bị khai thác → kích hoạt incident response: cô lập máy, bảo toàn bằng chứng, xoay vòng credential mà máy đó có thể truy cập.
- Bài học Equifax: thời gian từ bản vá tới triển khai là yếu tố quyết định.

**Câu hỏi nối tiếp:**
- *Không có SBOM thì làm sao?* → Quét repo/artifact (`dependency:tree`, Dependency-Check), quét server (tìm `struts2-core-*.jar` trong `WEB-INF/lib`).

**⚠️ Câu trả lời gây điểm trừ:**
- "Chờ đến thứ Hai lên kế hoạch."
- Chỉ vá mà không kiểm tra đã bị xâm nhập chưa.

**📖 Ôn lại:** [8.2 — Góc nhìn Senior về quy trình phản ứng](../01-giao-trinh/10-struts.md#8-security)

</details>

---

<a id="nhom-h"></a>
## H. So sánh với Spring MVC & migration

### Q34. 🟡 So sánh Struts 2 với Spring MVC. Bạn phản hồi thế nào nếu đồng nghiệp nói "Spring MVC an toàn còn Struts thì không"?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Khác biệt cốt lõi: Struts 2 action **mới mỗi request**, dữ liệu là field, binding bằng OGNL; Spring `@Controller` **singleton**, dữ liệu là tham số method (`@RequestParam`, `@ModelAttribute`, `@RequestBody`), binding bằng `DataBinder` + `ConversionService`. Validation: Struts XML/annotation riêng vs Bean Validation + `BindingResult`. Chuỗi xử lý chung: interceptor stack vs Filter/`HandlerInterceptor`/`@ControllerAdvice`. REST: plugin hạn chế vs first-class. Test: `StrutsTestCase` vs `MockMvc`/`@WebMvcTest`. Hệ sinh thái: Struts chủ yếu bảo trì, Spring có Boot, Security, Actuator. Về bảo mật: không nên nói tuyệt đối — **Spring4Shell (CVE-2022-22965)** cho thấy data binding là bề mặt tấn công chung; Spring ít RCE hơn vì binding ít "động" hơn OGNL, nhưng nguyên tắc giống nhau: giới hạn trường được bind (DTO riêng), vá kịp thời, phòng thủ nhiều lớp.

**Giải thích chi tiết:**
- Spring4Shell: binding chạm `class.module.classLoader` trên JDK 9+, Tomcat deploy WAR — cùng họ với CVE-2014-0114 của Struts 1.

**Câu hỏi nối tiếp:**
- *Vì sao thread-safety ngược nhau?* → Struts 2 action per request nên field an toàn; Spring controller singleton nên mọi trạng thái request phải nằm trong tham số/biến cục bộ.

**⚠️ Câu trả lời gây điểm trừ:**
- Đồng ý "Spring an toàn tuyệt đối".

**📖 Ôn lại:** [9. Struts 2 vs Spring MVC](../01-giao-trinh/10-struts.md#9-compare)

</details>

### Q35. 🟡 Các phương án hiện đại hóa một hệ thống Struts lớn? Vì sao strangler fig thường là mặc định?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** (1) **Big-bang rewrite** — viết lại toàn bộ rồi chuyển một lần: chỉ hợp hệ thống nhỏ, nghiệp vụ rõ, có test đầy đủ; hiếm khi đúng với hệ thống ngân hàng. (2) **Strangler fig** — dựng hệ thống mới bao quanh hệ thống cũ, chuyển dần từng luồng, route request theo URL, tắt phần cũ khi không còn traffic. (3) **Nâng cấp tại chỗ** — giữ Struts, nâng version, hardening: chỉ là bước đệm giảm rủi ro bảo mật trong lúc migrate. Strangler là mặc định vì giao giá trị sớm, rủi ro mỗi bước nhỏ, **rollback được** (feature flag về luồng cũ), và hệ thống vẫn vận hành trong suốt quá trình.

**Giải thích chi tiết:**
- Thứ tự chuyển luồng: ít rủi ro, nhiều giá trị trước (màn hình đọc, báo cáo) → luồng ghi.
- Đo tiến độ: % URL/traffic qua stack mới, số action còn lại, số CVE còn mở.
- Tắt hệ thống cũ chỉ khi traffic = 0 trong một chu kỳ nghiệp vụ đầy đủ (gồm cuối tháng/cuối năm).

**Câu hỏi nối tiếp:**
- *Khi nào big-bang hợp lý?* → Ứng dụng nội bộ nhỏ, ít người dùng, có thể đóng băng nghiệp vụ trong thời gian viết lại.

**⚠️ Câu trả lời gây điểm trừ:**
- Mặc định đề xuất big-bang rewrite.

**📖 Ôn lại:** [10.1 Các phương án](../01-giao-trinh/10-struts.md#10-migration)

</details>

### Q36. 🔴 So sánh hai kiểu "chung sống" khi migrate: Struts và Spring MVC trong cùng WAR vs hai ứng dụng sau reverse proxy. Ràng buộc `javax` vs `jakarta` ảnh hưởng thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **(A) Cùng WAR:** thêm `DispatcherServlet` map `/v2/*`, Struts bỏ qua bằng `struts.action.excludePattern=/v2/.*`. Ưu: dùng chung `HttpSession` và root `ApplicationContext` (service, DAO, transaction manager) → controller mới tái dùng service cũ ngay. Nhược: khóa vào cùng phiên bản Servlet API/Spring — Struts 6 (`javax.servlet`) chỉ đi với Spring 5.3; muốn Spring 6/Boot 3 cần Struts 7 (Jakarta EE). **(B) Reverse proxy** (Nginx/API Gateway route `/v2/**` sang Spring Boot 3, phần còn lại sang Tomcat cũ): tự do công nghệ (Java 21, Boot 3), deploy độc lập; nhược: phải chia sẻ phiên đăng nhập (Spring Session + Redis cho cả hai app, hoặc SSO/OIDC), đồng bộ giao diện/menu, hai codebase chung DB → kỷ luật schema (Flyway quản lý từ một nơi).

**Giải thích chi tiết:**
```nginx
location /v2/customers/ { proxy_pass http://customer-boot:8080; }
location /             { proxy_pass http://legacy-tomcat:8080; }
```
- Phương án B với Spring Session: phía Struts dùng `DelegatingFilterProxy` + `springSessionRepositoryFilter`; cookie session cùng tên, cùng domain/path.
- Phương án A: Struts filter map `/*` vẫn nhận request `/v2/*` nhưng exclude pattern cho đi tiếp tới servlet; `DispatcherServlet` có child context chỉ scan package `web.v2`.
- Feature flag: ở gateway (theo cookie/nhóm người dùng) hoặc trong app cũ (link menu trỏ URL mới).

**Câu hỏi nối tiếp:**
- *Chạy song song chung DB có rủi ro gì?* → Cache (L2C, session cache) lệch, khác biệt validation (dữ liệu cũ "bẩn" làm vỡ màn hình mới).

**⚠️ Câu trả lời gây điểm trừ:**
- Không biết Spring 6 yêu cầu Jakarta EE, đề xuất nhét Spring Boot 3 vào WAR Struts 6.

**📖 Ôn lại:** [10.2 Hai kiểu "chung sống"](../01-giao-trinh/10-struts.md#10-migration)

</details>

### Q37. 🟡 Chuyển action sau sang Spring MVC. Ánh xạ từng khái niệm Struts sang Spring.

```java
public class CustomerAction extends ActionSupport {
    private CustomerForm customer = new CustomerForm();
    public String save() {
        service.create(customer);
        addActionMessage(getText("customer.created"));
        return SUCCESS;          // result: redirectAction customer_list
    }
    public CustomerForm getCustomer() { return customer; }
}
```

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Action → `@Controller` method với `@PostMapping`; field form → DTO (record) + Bean Validation + `@Valid` + `BindingResult`; result `input` → trả lại tên view form khi có lỗi; `redirectAction` + action message → `"redirect:/v2/customers"` + `RedirectAttributes.addFlashAttribute`; `getText` → `MessageSource`; interceptor → Spring Security/`HandlerInterceptor`; `global-exceptions` → `@ControllerAdvice`; Tiles → Thymeleaf layout/fragment.

**Giải thích chi tiết:**
```java
public record CustomerForm(
        @NotBlank @Size(max = 100) String fullName,
        @NotBlank @Email String email,
        @Pattern(regexp = "^(0|\\+84)(3|5|7|8|9)\\d{8}$") String phone) {}

@Controller
@RequestMapping("/v2/customers")
@RequiredArgsConstructor
class CustomerController {
    private final CustomerService service;                 // tái dùng service cũ

    @PostMapping
    String create(@Valid @ModelAttribute("customer") CustomerForm form, BindingResult result,
                  RedirectAttributes ra) {
        if (result.hasErrors()) return "customer/form";    // tương đương result "input"
        service.create(form);
        ra.addFlashAttribute("message", "customer.created");
        return "redirect:/v2/customers";                   // PRG
    }
}
```
- Controller singleton → không có field trạng thái request.
- **Không** dùng entity làm form backing object (mass assignment cũng xảy ra ở Spring).

**Câu hỏi nối tiếp:**
- *Token interceptor thì ánh xạ sang gì?* → Idempotency key ở tầng nghiệp vụ + PRG; CSRF token của Spring Security là chuyện khác.

**⚠️ Câu trả lời gây điểm trừ:**
- Giữ entity làm `@ModelAttribute` cho nhanh.

**📖 Ôn lại:** [10.3 Bảng ánh xạ khái niệm](../01-giao-trinh/10-struts.md#10-migration)

</details>

### Q38. 🔴 Theo bạn, rủi ro lớn nhất khi migrate một luồng nghiệp vụ từ Struts sang Spring Boot là gì? Bạn kiểm soát nó thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Rủi ro lớn nhất **không phải kỹ thuật** mà là **nghiệp vụ ẩn**: logic nằm trong JSP scriptlet, JavaScript, stored procedure, `reset()`/`validate()` của form, và "workaround" người dùng đã quen (kể cả bug họ dựa vào). Kiểm soát: (1) **characterization test** (Playwright/Selenium hoặc HTTP-level) ghi lại hành vi hiện tại, chạy xanh trên **cả** bản cũ và bản mới; (2) phỏng vấn người dùng nghiệp vụ; (3) chạy song song, chuyển bằng **feature flag** theo nhóm người dùng, có điều kiện dừng và rollback; (4) observability so sánh lỗi/latency hai luồng; (5) kiểm tra khác biệt validation với dữ liệu cũ; (6) chờ hết một chu kỳ nghiệp vụ (cuối tháng/năm) trước khi gỡ luồng cũ.

**Giải thích chi tiết:**
- Khác biệt validation: luồng cũ chấp nhận dữ liệu mà luồng mới từ chối → bản ghi cũ "bẩn" làm vỡ màn hình sửa mới → cần profiling dữ liệu và quyết định chuẩn hóa trước.
- Bước tốn công nhất và giá trị nhất thường là **tách logic xuống service thuần** (không phụ thuộc Servlet/Struts API) trước khi viết controller mới.
- Cache: hai stack cùng DB có thể có cache lệch nhau.

**Câu hỏi nối tiếp:**
- *Đo "xong" một luồng thế nào?* → Traffic về action cũ = 0 trong chu kỳ đầy đủ, characterization test xanh, không có ticket hồi quy, action cũ đã gỡ khỏi config.

**⚠️ Câu trả lời gây điểm trừ:**
- "Rủi ro lớn nhất là chọn sai framework."
- Không có chiến lược rollback.

**📖 Ôn lại:** [10.4 Kế hoạch từng bước và Góc nhìn Senior](../01-giao-trinh/10-struts.md#10-migration)

</details>

---

> ✅ **Tự kiểm tra sau khi luyện:** đối chiếu với [Checklist tự đánh giá của Module 10](../01-giao-trinh/10-struts.md#checklist-tu-danh-gia). Với vị trí ở ngân hàng/bảo hiểm/dự án Nhật, hãy chuẩn bị sẵn một câu chuyện thực tế (hoặc từ dự án mini) về việc bạn hardening hoặc migrate một luồng Struts — người phỏng vấn gần như chắc chắn sẽ hỏi.

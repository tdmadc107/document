# Câu hỏi phỏng vấn — Module 01: Java Core & OOP

> Giáo trình tương ứng: [Module 01 — Java Core & OOP](../01-giao-trinh/01-java-core-oop.md)

**Cách dùng:** đọc câu hỏi, **tự trả lời thành tiếng** (như đang ngồi trước interviewer) trong 1–2 phút, *rồi mới* mở "Đáp án". So với phần **Trả lời ngắn** trước — đó là thứ cần nói được trong 30 giây đầu. **Giải thích chi tiết** và **Câu hỏi nối tiếp** là phần phân biệt Senior với Middle. Câu nào ấp úng hoặc sai → bấm link 📖 quay lại giáo trình.

**Mức độ:** 🟢 Cơ bản (phải trả lời trôi chảy) · 🟡 Senior (mức kỳ vọng của vị trí) · 🔴 Xoáy sâu (interviewer đào vào cơ chế/production).
**Dạng câu:** 🧩 tình huống "bạn sẽ làm gì" · 🔍 đọc code "in ra gì / có bug gì".

## Mục lục

| Nhóm | Chủ đề | Câu |
|---|---|---|
| A | [JVM, JDK, class loading, JIT](#nhom-a) | Q1–Q5 |
| B | [Primitive, wrapper, pass-by-value](#nhom-b) | Q6–Q10 |
| C | [String](#nhom-c) | Q11–Q15 |
| D | [Toán tử, overflow, số thực & tiền tệ](#nhom-d) | Q16–Q18 |
| E | [OOP, overloading/overriding, binding](#nhom-e) | Q19–Q26 |
| F | [Abstract class, interface, nested class](#nhom-f) | Q27–Q30 |
| G | [Các method của `Object`](#nhom-g) | Q31–Q36 |
| H | [Immutability](#nhom-h) | Q37–Q39 |
| I | [`static`, `final`, khởi tạo, access modifier](#nhom-i) | Q40–Q44 |
| J | [Enum](#nhom-j) | Q45–Q47 |
| K | [Exceptions](#nhom-k) | Q48–Q53 |
| L | [I/O, NIO.2, serialization](#nhom-l) | Q54–Q55 |
| M | [Reflection & annotations](#nhom-m) | Q56–Q57 |

---

<a id="nhom-a"></a>
## A. JVM, JDK, class loading, JIT

### Q1. 🟢 JDK, JRE và JVM khác nhau thế nào? "Write once, run anywhere" đạt được nhờ đâu?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** JVM là máy ảo thực thi bytecode (một *đặc tả* — HotSpot, OpenJ9, GraalVM là các hiện thực). JRE = JVM + thư viện chuẩn để *chạy*. JDK = JRE + công cụ phát triển (`javac`, `jar`, `jlink`, `jcmd`, `jfr`...). Code được biên dịch thành bytecode độc lập nền tảng; mỗi OS có JVM riêng chạy cùng bytecode đó.

**Giải thích chi tiết:**
- Từ Java 11, OpenJDK không phát hành JRE riêng nữa; thay vào đó dùng `jlink` tạo runtime tối giản chỉ chứa các module cần → giảm kích thước Docker image đáng kể (vài chục MB thay vì vài trăm).
- "Run anywhere" có giới hạn thực tế: native library (JNI), đường dẫn file, charset mặc định (trước Java 18), line separator vẫn phụ thuộc OS.
- Java 11+ (JEP 330) chạy thẳng `java Hello.java` — tiện cho script, không dùng cho production.

**Câu hỏi nối tiếp:**
- *Bytecode có thật sự "chậm" vì được interpret?* — Không: code nóng được JIT (C1/C2) biên dịch ra mã máy, có thể tối ưu theo profile runtime mà compiler AOT không có.
- *Image Docker cho Java nên build thế nào?* — Multi-stage build, base image JRE/`jlink` runtime, cấu hình heap bằng `-XX:MaxRAMPercentage`.

**⚠️ Câu trả lời gây điểm trừ:**
- "JVM là phần mềm của Oracle" — JVM là đặc tả, có nhiều hiện thực.
- Không biết từ Java 11 không còn JRE riêng.

**📖 Ôn lại:** [Phần 1 — JDK, JRE, JVM](../01-giao-trinh/01-java-core-oop.md#phan-1)

</details>

### Q2. 🟡 Mô tả hành trình từ file `Hello.java` cho tới khi CPU chạy mã máy.

<details><summary>Đáp án</summary>

**Trả lời ngắn:** `javac` biên dịch ra `.class` (bytecode + constant pool) → class loader nạp class (parent delegation) → **linking** (verify, prepare gán giá trị mặc định cho static, resolve symbolic reference) → **initialization** chạy `<clinit>` một lần, thread-safe → execution engine **interpret** trước, method nóng được **C1** rồi **C2** biên dịch (tiered compilation); khi giả định tối ưu sai thì **deoptimize**.

**Giải thích chi tiết:**
- *Verification* là nền tảng an toàn: bytecode không nhảy bừa, không tràn operand stack, đúng kiểu.
- *Preparation* chỉ gán 0/`null`/`false`; giá trị bạn viết (`static int x = 5`) được gán trong `<clinit>`.
- `<clinit>` có lock của JVM → đảm bảo chạy đúng một lần dù nhiều thread → cơ sở của holder idiom.
- JIT tối ưu dựa trên profile: inlining, escape analysis (loại allocation), loop unrolling, lock elision. Nạp class mới làm một call site monomorphic thành polymorphic → bản biên dịch bị "made not entrant", quay về interpreter.
- Quan sát được bằng `javap -c -v`, `-XX:+PrintCompilation` (ký hiệu `%` = OSR — On-Stack Replacement).

**Câu hỏi nối tiếp:**
- *Vì sao benchmark bằng `System.nanoTime()` thường sai?* — Warm-up, dead-code elimination, OSR, constant folding; phải dùng JMH.
- *Java 9+ nối chuỗi `"Hi " + n` thành bytecode gì?* — `invokedynamic` gọi `StringConcatFactory.makeConcatWithConstants` (JEP 280).

**⚠️ Câu trả lời gây điểm trừ:**
- Gộp loading/linking/initialization thành một bước; không biết static field có hai lần gán (default rồi giá trị thật).
- "JIT biên dịch toàn bộ chương trình lúc khởi động".

**📖 Ôn lại:** [Phần 1, mục 1.2 — Pipeline đầy đủ](../01-giao-trinh/01-java-core-oop.md#phan-1)

</details>

### Q3. 🟡 🧩 Log production báo `ClassCastException: com.shop.Foo cannot be cast to com.shop.Foo`. Chuyện gì xảy ra?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Trong JVM, một class được định danh bởi cặp **(tên đầy đủ, class loader định nghĩa nó)**. Hai class `com.shop.Foo` do hai loader khác nhau nạp là **hai kiểu khác nhau** → cast lẫn nhau thất bại. Thường gặp trong app server nhiều webapp, hot-reload (Spring DevTools có `RestartClassLoader`), hệ thống plugin, hoặc thư viện bị đóng gói trùng ở hai nơi.

**Giải thích chi tiết:**
- Mô hình **parent delegation**: Bootstrap (nạp `java.base`) → Platform (Java 9+, trước là Extension) → Application. Loader hỏi cha trước, cha không có mới tự nạp → không ai "đánh tráo" được `java.lang.String`.
- Loader tự viết (plugin, app server) thường đảo ngược ưu tiên (child-first) để mỗi webapp có phiên bản thư viện riêng → cùng tên class có thể được nạp hai lần.
- Cách điều tra: in `obj.getClass().getClassLoader()` và `Foo.class.getClassLoader()`; chạy `-verbose:class` / `-Xlog:class+load` để thấy class nạp từ jar nào.
- Cách sửa: đưa kiểu dùng chung (API) lên loader cha (shared lib), chỉ để hiện thực ở loader con; với DevTools, cấu hình `restart.include/exclude` cho jar bị chia sẻ.

**Câu hỏi nối tiếp:**
- *Viết class loader tự nạp cùng một class hai lần, `c1 == c2`?* — `false` nếu parent không tìm thấy class (ví dụ parent = bootstrap); nếu parent là app loader và class có trong classpath thì delegation trả về cùng class → `true`.
- *`Thread.getContextClassLoader()` dùng khi nào?* — Khi code thư viện (nạp ở loader cha) cần nạp class của ứng dụng (loader con): JDBC `ServiceLoader`, JNDI, framework.

**⚠️ Câu trả lời gây điểm trừ:**
- "Do sai version class" mà không nói tới class loader.
- Đề xuất "catch ClassCastException rồi bỏ qua".

**📖 Ôn lại:** [Phần 1, mục 1.2 — Class loader và parent delegation](../01-giao-trinh/01-java-core-oop.md#phan-1)

</details>

### Q4. 🟡 🧩 Phân biệt `ClassNotFoundException` và `NoClassDefFoundError`. Vì sao log lại có `NoClassDefFoundError: Could not initialize class X` dù jar có class X?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** `ClassNotFoundException` là checked exception khi **nạp động** (`Class.forName`, `loadClass`) mà không tìm thấy class. `NoClassDefFoundError` là `LinkageError`: class *có lúc compile* nhưng lúc runtime không định nghĩa được — **hoặc** static initializer của nó đã ném exception trước đó. Lần đầu là `ExceptionInInitializerError`, class rơi vào trạng thái *erroneous*, mọi lần dùng sau đều là `NoClassDefFoundError: Could not initialize class X`.

**Giải thích chi tiết:**
- Tình huống thật: `static final Config CFG = load();` trong `static {}` đọc file/biến môi trường bị thiếu → lần đầu lỗi rõ, nhưng log bị cuộn mất; hàng nghìn request sau chỉ thấy "Could not initialize class" → dễ nghĩ nhầm là thiếu jar.
- Cách điều tra: tìm log **đầu tiên** có `ExceptionInInitializerError` và `Caused by`.
- Phòng tránh: không làm I/O, gọi mạng, đọc config có thể lỗi trong static initializer; dùng lazy init có xử lý lỗi hoặc để DI container khởi tạo.
- Họ hàng: `UnsupportedClassVersionError` (compile JDK 21 — major 65, chạy trên 17 — major 61) → dùng `javac --release 17`; `NoSuchMethodError` khi hai thư viện yêu cầu hai phiên bản khác nhau của cùng dependency (dependency hell, xem `mvn dependency:tree`).

**Câu hỏi nối tiếp:**
- *Khi nào static initializer chạy?* — Lần đầu chủ động sử dụng: `new`, gọi static method, đọc/ghi static field không phải constant, `Class.forName`, khởi tạo class con.

**⚠️ Câu trả lời gây điểm trừ:**
- "Hai cái giống nhau, chỉ khác tên" hoặc cho rằng `NoClassDefFoundError` luôn do thiếu jar.

**📖 Ôn lại:** [Phần 1 — Lỗi thường gặp](../01-giao-trinh/01-java-core-oop.md#phan-1), [Phần 9, mục 9.3](../01-giao-trinh/01-java-core-oop.md#phan-9)

</details>

### Q5. 🔴 🧩 Service Java vừa deploy lên Kubernetes có p99 latency rất cao trong 2–3 phút đầu rồi tự ổn định. Giải thích và đề xuất cách xử lý.

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Đó là **JVM warm-up**: lúc đầu code chạy bằng interpreter/C1, class còn đang được nạp, cache (connection pool, Hibernate metadata, JIT profile) còn lạnh. Khi method nóng được C2 biên dịch, latency mới về mức ổn định. Giải pháp: warm-up trước khi nhận tải thật (readiness probe chỉ bật khi đã warm), giảm thời gian load class bằng **CDS/AppCDS**, hoặc **CRaC**/GraalVM Native Image nếu startup là ưu tiên, và cấu hình CPU limit đủ cho JIT thread.

**Giải thích chi tiết:**
- JIT cần CPU: nếu Pod bị giới hạn 0.5–1 vCPU, compiler thread tranh CPU với request thread → warm-up kéo dài, thậm chí CPU throttling (CFS quota). Đừng đặt CPU limit quá thấp cho service Java.
- Container awareness: Java 10+ (backport 8u191) đọc cgroup limit; dùng `-XX:MaxRAMPercentage=75` thay vì hard-code `-Xmx` lệch với memory limit (lệch → OOMKilled).
- Warm-up có chủ đích: gọi các endpoint chính bằng traffic giả trong startup hook, preload cache, khởi tạo connection pool (`minimumIdle`), Spring `ApplicationReadyEvent`.
- Trade-off: Native Image khởi động trong vài chục ms nhưng mất tối ưu theo profile (throughput đỉnh thấp hơn), cần khai báo reflection; CRaC cần môi trường hỗ trợ checkpoint.
- Deploy kiểu rolling + readiness probe đúng giúp người dùng không thấy giai đoạn lạnh.

**Câu hỏi nối tiếp:**
- *Làm sao biết đó là JIT chứ không phải GC?* — So GC log (`-Xlog:gc`) với thời điểm spike; JFR event Compilation; `-XX:+PrintCompilation`.
- *Deoptimization gây spike giữa chừng được không?* — Có, khi class mới được nạp làm sai giả định (ví dụ call site trở thành megamorphic), nhưng thường nhỏ hơn warm-up.

**⚠️ Câu trả lời gây điểm trừ:**
- "Tăng RAM" hoặc "do GC" mà không có bằng chứng.
- Không nhắc readiness probe / CPU limit — dấu hiệu chưa từng vận hành service trên container.

**📖 Ôn lại:** [Phần 1 — Góc nhìn Senior về warm-up, container](../01-giao-trinh/01-java-core-oop.md#phan-1)

</details>

---

<a id="nhom-b"></a>
## B. Primitive, wrapper, pass-by-value

### Q6. 🟢 Java là pass-by-value hay pass-by-reference? Chứng minh.

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Java **luôn pass-by-value**. Với object, *giá trị được truyền là bản sao của reference*. Vì vậy method sửa được **trạng thái** object (qua bản sao reference) nhưng **không** làm biến của caller trỏ sang object khác.

**Giải thích chi tiết:**

```java
static void reassign(StringBuilder sb) { sb = new StringBuilder("mới"); } // chỉ đổi bản sao
static void mutate(StringBuilder sb)   { sb.append(" + sửa"); }          // sửa object chung
static void swap(Integer x, Integer y) { Integer t = x; x = y; y = t; }  // vô tác dụng

StringBuilder sb = new StringBuilder("gốc");
reassign(sb); // sb vẫn "gốc"
mutate(sb);   // "gốc + sửa"
Integer a = 1, b = 2; swap(a, b); // a=1, b=2
```

Nếu Java pass-by-reference thật (như `ref` trong C#, `&` trong C++), `swap` và `reassign` sẽ có tác dụng.

**Câu hỏi nối tiếp:**
- *Vậy muốn method "trả về nhiều giá trị" thì làm sao?* — Trả về record/object kết quả; tránh dùng tham số mảng 1 phần tử làm "out param".
- *Biến local primitive nằm ở đâu?* — Trong stack frame (hoặc thanh ghi sau JIT); field primitive nằm trong object trên heap.

**⚠️ Câu trả lời gây điểm trừ:**
- "Primitive pass-by-value, object pass-by-reference" — interviewer sẽ bắt bằng ví dụ `swap`.

**📖 Ôn lại:** [Phần 2, mục 2.3 — Pass-by-value](../01-giao-trinh/01-java-core-oop.md#phan-2)

</details>

### Q7. 🟢 🔍 Đoạn code sau in ra gì?

```java
Integer a = 127, b = 127, c = 128, d = 128;
System.out.println(a == b);
System.out.println(c == d);
System.out.println(c.equals(d));
System.out.println(c <= d && c >= d);
Long l = 127L;
System.out.println(l.equals(127));
System.out.println(l == 127);
```

<details><summary>Đáp án</summary>

**Trả lời ngắn:** `true`, `false`, `true`, `true`, `false`, `true`.

**Giải thích chi tiết:**
- Autoboxing gọi `Integer.valueOf`, trả về object cache cho dải **-128..127** (JLS §5.1.7 bắt buộc) → `a == b` cùng object; 128 ngoài cache → hai object khác nhau.
- `<=`, `>=` buộc **unbox** → so sánh giá trị → `true`.
- `l.equals(127)`: `127` được box thành **`Integer`**, `Long.equals` kiểm tra `instanceof Long` → `false`.
- `l == 127`: một vế là kiểu số primitive → numeric equality, `l` được unbox → `true`.
- Cache: `Byte/Short/Long` -128..127, `Character` 0..127, `Boolean` TRUE/FALSE, `Float/Double` không cache. Cận trên của `Integer` chỉnh được bằng `-XX:AutoBoxCacheMax`.

**Câu hỏi nối tiếp:**
- *`new Integer(5) == Integer.valueOf(5)`?* — `false`; constructor luôn tạo object mới, deprecated từ Java 9, forRemoval từ Java 16 (chuẩn bị cho Valhalla).
- *Map<Long, X> `map.get(5)` trả về gì?* — `null`, vì key `Integer(5)` không `equals` `Long(5)`. Bug kinh điển.

**⚠️ Câu trả lời gây điểm trừ:**
- Trả lời đúng output nhưng không giải thích được bằng `Integer.valueOf` và cache.
- Khuyên "tăng AutoBoxCacheMax để `==` chạy đúng" — sửa sai chỗ; phải dùng `equals`.

**📖 Ôn lại:** [Phần 2, mục 2.2 — Wrapper, autoboxing và Integer cache](../01-giao-trinh/01-java-core-oop.md#phan-2)

</details>

### Q8. 🟡 🔍 Method sau thỉnh thoảng ném `NullPointerException`. Vì sao? Sửa thế nào mà vẫn giữ kiểu trả về `int`?

```java
int resolveLimit(Map<String, Integer> cfg, boolean premium) {
    return premium ? cfg.get("premium") : 10;
}
```

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Biểu thức điều kiện `boolean ? Integer : int` có kiểu **`int`** (JLS §15.25, binary numeric promotion), nên `cfg.get("premium")` bị unbox. Key thiếu → `null.intValue()` → NPE. Sửa: `return premium ? cfg.getOrDefault("premium", 10) : 10;` hoặc xử lý null tường minh.

**Giải thích chi tiết:**
- NPE do unboxing rất khó nhìn ra vì không có dấu `.` nào trên dòng code. Java 14+ (JEP 358 — Helpful NPE) in rõ: `Cannot invoke "java.lang.Integer.intValue()" because the return value of "java.util.Map.get(Object)" is null`.
- Các biến thể khác: `int qty = map.get(k);`, `boolean f = boolObj;`, `for (int x : listChuaNull)`, so sánh `Integer` với `int` bằng `==`.
- Trường hợp đặc biệt: `Integer x = flag ? null : 0;` **không** NPE vì kiểu biểu thức là `Integer` (null type + int → lub là `Integer`); nhưng `Integer x = flag ? someNullInteger : 0;` thì NPE.

**Câu hỏi nối tiếp:**
- *Field entity cho cột nullable nên là `int` hay `Integer`?* — `Integer`, nếu không sẽ mất trạng thái "không có giá trị" (null thành 0); bù lại phải xử lý null ở mọi chỗ unbox.

**⚠️ Câu trả lời gây điểm trừ:**
- "Do `cfg` null" — không giải thích được kiểu của ternary.

**📖 Ôn lại:** [Phần 2, mục 2.2 — Cái bẫy NPE khi unboxing](../01-giao-trinh/01-java-core-oop.md#phan-2)

</details>

### Q9. 🟡 Autoboxing tốn kém đến mức nào? Đoạn `Long sum = 0L; for (long i...) sum += i;` có vấn đề gì? `list.remove(1)` trên `List<Integer>` làm gì?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Mỗi vòng lặp `sum += i` với `Long` là unbox → cộng → box → tạo object mới (ngoài cache) → hàng trăm triệu object rác, chậm nhiều lần so với `long`. Bộ nhớ: `Integer` chiếm ~16 byte (header 12 + value 4) so với 4 byte `int`, chưa kể reference 4–8 byte trong collection. `list.remove(1)` xóa phần tử **ở index 1**, vì overload `remove(int)` thắng `remove(Object)` ở pha 1 (không cần boxing).

**Giải thích chi tiết:**
- `List<Integer>` 1 triệu phần tử ~20 MB, `int[]` ~4 MB → với dữ liệu số lớn, dùng mảng primitive, `IntStream`, hoặc thư viện primitive collections (fastutil, Eclipse Collections).
- Escape analysis của C2 *đôi khi* loại được allocation, nhưng không đảm bảo — đừng dựa vào.
- Xóa theo giá trị: `list.remove(Integer.valueOf(1))`.

**Câu hỏi nối tiếp:**
- *Làm sao phát hiện boxing nóng trên production?* — JFR allocation profiling / async-profiler `-e alloc` thấy `Long.valueOf` đứng đầu.

**⚠️ Câu trả lời gây điểm trừ:**
- "JVM tự tối ưu hết rồi" — không có số liệu, không biết escape analysis có giới hạn.

**📖 Ôn lại:** [Phần 2, mục 2.2 & Góc nhìn Senior](../01-giao-trinh/01-java-core-oop.md#phan-2)

</details>

### Q10. 🔴 🧩 Test so sánh ID `Long` bằng `==` vẫn pass, lên production lại sai. Giải thích. Thêm nữa: vì sao không được `synchronized (someInteger)`?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Dữ liệu test thường có ID nhỏ (1, 2, 3) nằm trong cache -128..127 nên `==` "tình cờ" đúng; production ID lớn → hai object `Long` khác nhau → `false`. Luôn dùng `equals`/`Objects.equals`. Với lock: wrapper được cache và chia sẻ toàn JVM, nên `synchronized (Integer.valueOf(1))` ở hai module không liên quan có thể vô tình dùng **chung một monitor** → contention hoặc deadlock khó hiểu; ngược lại `count++` trên `Integer` tạo object mới nên lock đổi object liên tục → không loại trừ được gì.

**Giải thích chi tiết:**
- Wrapper, `String` literal, `LocalDate`, `Optional`... là *value-based class*: identity không có ý nghĩa. Java 16+ có `-XX:DiagnoseSyncOnValueBasedClasses=1|2` để cảnh báo/fail khi synchronize trên chúng; Project Valhalla sẽ biến chúng thành value class không có identity.
- Lock nên là `private final Object lock = new Object()` hoặc `ReentrantLock`.
- Bug liên quan: so sánh `entity.getId() == other.getId()` trong `equals` của entity JPA.

**Câu hỏi nối tiếp:**
- *Code review làm sao bắt được?* — Static analysis (SpotBugs `RC_REF_COMPARISON`, Error Prone `ReferenceEquality`, Sonar rule S4973).

**⚠️ Câu trả lời gây điểm trừ:**
- "Long so sánh bằng `==` được vì nó là số".

**📖 Ôn lại:** [Phần 2 — Góc nhìn Senior & Lỗi thường gặp](../01-giao-trinh/01-java-core-oop.md#phan-2)

</details>

---

<a id="nhom-c"></a>
## C. String

### Q11. 🟢 Vì sao `String` là immutable? Lợi ích và cái giá là gì?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** `String` là `final class`, mảng dữ liệu `private final`, không có method sửa nội dung. Lợi ích: (1) **bảo mật** — tên file, URL, tên class đã kiểm tra không bị sửa sau đó; (2) **thread-safe** miễn phí; (3) **cache hashCode** → key lý tưởng cho `HashMap`; (4) **String pool** khả thi vì chia sẻ literal không sợ bị sửa. Cái giá: mỗi lần "sửa" tạo object mới → cần `StringBuilder` khi build chuỗi.

**Giải thích chi tiết:**
- Hệ quả bảo mật ngược: password trong `String` không xóa được khỏi bộ nhớ, có thể nằm trong pool, lộ qua heap dump → API nhạy cảm dùng `char[]` để ghi đè sau khi dùng.
- Mẫu "immutable + companion mutable" (`String`/`StringBuilder`, `BigInteger`/`MutableBigInteger` nội bộ) là thiết kế có thể áp dụng cho class của bạn.

**Câu hỏi nối tiếp:**
- *Có thể sửa String bằng reflection?* — Trước Java 9 có thể sửa `value` qua reflection; từ JPMS + strong encapsulation (Java 16/17) thì bị chặn trừ khi `--add-opens`. Đừng làm.

**⚠️ Câu trả lời gây điểm trừ:**
- Chỉ nói "vì nó final" — `final` class không làm object immutable; immutable đến từ field private final và không có mutator.

**📖 Ôn lại:** [Phần 3, mục 3.1](../01-giao-trinh/01-java-core-oop.md#phan-3)

</details>

### Q12. 🟡 🔍 Mỗi dòng sau in `true` hay `false`?

```java
String s1 = "java";
String s3 = new String("java");
String s4 = s3.intern();
String s5 = "ja" + "va";
String part = "ja";
String s6 = part + "va";
final String fpart = "ja";
System.out.println(s1 == s3);              // (1)
System.out.println(s1 == s4);              // (2)
System.out.println(s1 == s5);              // (3)
System.out.println(s1 == s6);              // (4)
System.out.println(s1 == (fpart + "va"));  // (5)
System.out.println(s1 == """
        java""");                          // (6)
```

<details><summary>Đáp án</summary>

**Trả lời ngắn:** (1) `false`, (2) `true`, (3) `true`, (4) `false`, (5) `true`, (6) `true`.

**Giải thích chi tiết:**
- `new String(...)` luôn tạo object mới trên heap; literal `"java"` vẫn nằm trong pool.
- `intern()` trả về instance trong pool.
- `"ja" + "va"` là **compile-time constant** (JLS §15.29) → compiler gộp thành `"java"` và dùng literal trong pool.
- `part + "va"`: `part` không phải constant variable → nối lúc runtime → object mới.
- `fpart` là `final` + khởi tạo bằng constant → *constant variable* → biểu thức vẫn là constant → pool.
- Text block là `String` literal bình thường (indentation chung bị cắt) → intern như literal.
- Từ Java 7, String pool (StringTable) nằm trên **heap** (trước đó ở PermGen → `OutOfMemoryError: PermGen space` khi lạm dụng `intern()`); string trong pool vẫn được GC.

**Câu hỏi nối tiếp:**
- *`intern()` khác G1 String Deduplication thế nào?* — `intern()` gộp *object* `String` vào pool (phải gọi tường minh, tốn tra hash table); G1 dedup (`-XX:+UseStringDeduplication`) chạy nền, chỉ gộp **mảng `byte[]`** bên dưới của các String trùng nội dung, không đổi identity.

**⚠️ Câu trả lời gây điểm trừ:**
- Không biết khái niệm compile-time constant, đoán (5) là `false`.

**📖 Ôn lại:** [Phần 3, mục 3.2 — String pool và `intern()`](../01-giao-trinh/01-java-core-oop.md#phan-3)

</details>

### Q13. 🟢 So sánh `+`, `StringBuilder`, `StringBuffer`. Java 9+ đã tối ưu `+` rồi thì `s += x` trong vòng lặp còn vấn đề gì?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** `StringBuilder` mutable, không đồng bộ — mặc định khi build chuỗi. `StringBuffer` có method `synchronized` — legacy, gần như không cần. Java 9+ (JEP 280) biên dịch **một biểu thức** `a + b + c` thành `invokedynamic` tới `StringConcatFactory` (tính trước độ dài, cấp phát một lần) → một biểu thức `+` là ổn. Nhưng trong vòng lặp, mỗi `s += x` vẫn tạo String mới và copy toàn bộ nội dung cũ → **O(n²)** byte copy.

**Giải thích chi tiết:**

```java
String csv = "";
for (String item : items) csv += item + ",";      // O(n^2)

StringBuilder sb = new StringBuilder(items.size() * 16); // đặt capacity, tránh grow
for (String item : items) sb.append(item).append(',');

String csv2 = String.join(",", items);            // gọn nhất
```

- `StringBuilder` grow khoảng `(old << 1) + 2` rồi copy mảng → biết trước kích thước thì đặt capacity.
- `StringBuffer` dù synchronized cũng không làm chuỗi thao tác phức hợp thành atomic; khi cần nhiều thread build chuỗi, thường mỗi thread build riêng rồi gộp.

**Câu hỏi nối tiếp:**
- *Logging `log.debug("x=" + x)` có vấn đề?* — Nối chuỗi chạy kể cả khi debug tắt; dùng placeholder `log.debug("x={}", x)`.

**⚠️ Câu trả lời gây điểm trừ:**
- "Luôn dùng `StringBuffer` cho an toàn" hoặc "compiler tối ưu hết `+=` rồi".

**📖 Ôn lại:** [Phần 3, mục 3.4](../01-giao-trinh/01-java-core-oop.md#phan-3)

</details>

### Q14. 🔴 Compact Strings là gì? `"😀".length()` trả về bao nhiêu? Heap dump cho thấy `byte[]`/`String` chiếm 35% heap — bạn làm gì?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Java 9 (JEP 254) đổi `char[]` thành `byte[] value` + `byte coder` (LATIN1/UTF16). Chuỗi chỉ gồm ký tự Latin-1 dùng 1 byte/ký tự → tiết kiệm ~50% cho chuỗi ASCII. `"😀".length() == 2` vì `length()` đếm **UTF-16 code unit** (emoji là surrogate pair). Với heap nhiều String: tìm nguồn trùng lặp và giảm bằng enum, intern có kiểm soát, G1 string dedup, hoặc sửa chỗ giữ chuỗi lớn không cần thiết.

**Giải thích chi tiết:**
- Tiếng Việt có dấu (`ệ` = U+1EC7) nằm ngoài Latin-1 → cả chuỗi dùng UTF16 (2 byte/ký tự).
- Đếm ký tự hiển thị đúng: `codePointCount`, hoặc `BreakIterator.getCharacterInstance()` sau khi `Normalizer.normalize(s, NFC)` (vì "ế" có thể là 1 code point dựng sẵn hoặc chữ + dấu tổ hợp).
- Phân tích heap: Eclipse MAT → "Duplicate Strings" / dominator tree. Nguồn thường gặp: status `"ACTIVE"` đọc từ DB hàng triệu lần, header HTTP, JSON key, cache chứa payload thô.
- Lịch sử: trước Java 7u6, `substring` chia sẻ mảng gốc → substring nhỏ giữ cả chuỗi khổng lồ (leak); từ 7u6 `substring` copy.

**Câu hỏi nối tiếp:**
- *Cắt chuỗi cho cột DB `VARCHAR(255)` bằng `substring(0, 255)` có an toàn?* — Có thể cắt giữa surrogate pair (ra ký tự hỏng) và độ dài byte UTF-8 khác số ký tự; cắt theo code point và kiểm tra byte length nếu DB tính theo byte.

**⚠️ Câu trả lời gây điểm trừ:**
- "length() trả về số ký tự" và "tăng heap là xong".

**📖 Ôn lại:** [Phần 3, mục 3.3 — Compact Strings & Góc nhìn Senior](../01-giao-trinh/01-java-core-oop.md#phan-3)

</details>

### Q15. 🟡 🧩 Kể những bug liên quan tới String mà bạn từng gặp (hoặc biết) trên production.

<details><summary>Đáp án</summary>

**Trả lời ngắn:** (1) So sánh bằng `==`; (2) `split(".")` — tham số là regex, `.` khớp mọi ký tự → mảng rỗng; (3) `"a,b,,".split(",")` bỏ chuỗi rỗng cuối (length 2) — dùng `split(",", -1)`; (4) `toLowerCase()` không truyền `Locale` → "Turkish i"; (5) `getBytes()`/`new String(bytes)` không chỉ định charset → lỗi font khi đổi môi trường (trước Java 18); (6) `+=` trong vòng lặp; (7) log/toString lộ PII, password trong String.

**Giải thích chi tiết:**
- Turkish i: `"TITLE".toLowerCase()` với locale `tr` ra `"tıtle"` → key cấu hình/enum lookup thất bại. Dùng `toLowerCase(Locale.ROOT)` cho chuỗi kỹ thuật.
- Charset: Java 18 (JEP 400) mặc định UTF-8, nhưng vẫn nên chỉ định `StandardCharsets.UTF_8` để code tương thích Java 8/11/17.
- So sánh null-safe: `"CONST".equals(var)` hoặc `Objects.equals(a, b)`.
- `strip()` (Java 11, Unicode-aware) khác `trim()` (chỉ cắt ký tự ≤ U+0020) — input copy từ web có thể chứa non-breaking space mà `trim()` không cắt.

**Câu hỏi nối tiếp:**
- *Vì sao `String.hashCode` có thể bị tấn công?* — Công thức `31*h + c` dễ tạo collision có chủ đích (hash flooding) → HashMap Java 8 giảm thiểu bằng treeification.

**⚠️ Câu trả lời gây điểm trừ:**
- Chỉ nêu được `==` mà không có ví dụ thực tế nào khác.

**📖 Ôn lại:** [Phần 3 — Góc nhìn Senior & Lỗi thường gặp](../01-giao-trinh/01-java-core-oop.md#phan-3)

</details>

---

<a id="nhom-d"></a>
## D. Toán tử, overflow, số thực & tiền tệ

### Q16. 🟢 🔍 Đoạn code sau in ra gì?

```java
long micros = 24 * 60 * 60 * 1000 * 1000;
System.out.println(micros);
byte b = 10; b += 300; System.out.println(b);
int i = 0; i = i++; System.out.println(i);
System.out.println(1 + 2 + "3" + 4 + 5);
char c = 'A'; System.out.println(c + 1);
System.out.println(Math.abs(Integer.MIN_VALUE));
```

<details><summary>Đáp án</summary>

**Trả lời ngắn:** `500654080`, `54`, `0`, `3345`, `66`, `-2147483648`.

**Giải thích chi tiết:**
- Phép nhân tính bằng **`int`** (mọi toán hạng là `int`), tràn **trước khi** gán cho `long`. Sửa: `24L * 60 * ...`.
- `b += 300` là compound assignment có cast ngầm: `b = (byte)(b + 300)` → `(byte)310 = 54`, tràn âm thầm. Còn `b = b + 1` không compile (`int` không gán được cho `byte`).
- `i = i++`: giá trị cũ (0) được lưu, `i` tăng thành 1, rồi gán lại 0.
- Nối chuỗi từ trái sang phải: `1 + 2 = 3`, rồi `"3" + "3"`, `"33" + 4`, `"334" + 5`.
- `char + int` → `int` (binary numeric promotion) → 66; muốn ký tự phải cast `(char)(c + 1)`.
- `Math.abs(Integer.MIN_VALUE)` vẫn âm vì `+2147483648` không biểu diễn được bằng `int`.

**Câu hỏi nối tiếp:**
- *`1 << 32` bằng bao nhiêu?* — `1`: khoảng dịch của `int` được mask `& 31`.
- *Phát hiện overflow thế nào?* — `Math.addExact/multiplyExact/toIntExact` ném `ArithmeticException`.

**⚠️ Câu trả lời gây điểm trừ:**
- Nói `micros` đúng là 86400000000 vì "đã khai báo long".

**📖 Ôn lại:** [Phần 4, mục 4.1–4.4](../01-giao-trinh/01-java-core-oop.md#phan-4)

</details>

### Q17. 🟡 Tính tiền nên dùng kiểu gì? Kể các bẫy của `BigDecimal`.

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Không bao giờ dùng `double` cho tiền (`0.1 + 0.2 = 0.30000000000000004` do biểu diễn nhị phân IEEE 754). Dùng `BigDecimal` với scale và `RoundingMode` quy định rõ trong domain (ví dụ `HALF_EVEN`), hoặc `long` theo đơn vị nhỏ nhất (cent, đồng). Bẫy của `BigDecimal`: `new BigDecimal(0.1)` mang theo sai số của double; `equals` so cả scale (`2.0 ≠ 2.00`) trong khi `compareTo` thì bằng; `divide` không chỉ định scale/rounding với kết quả vô hạn (`1/3`) → `ArithmeticException`.

**Giải thích chi tiết:**
- Tạo đúng: `new BigDecimal("0.1")` hoặc `BigDecimal.valueOf(0.1)` (đi qua `Double.toString`).
- `HashSet<BigDecimal>` coi `2.0` và `2.00` là 2 phần tử, `TreeSet` coi là 1 (dùng `compareTo`). Chuẩn hóa bằng `stripTrailingZeros()` hoặc `setScale` cố định trong constructor của value object `Money`.
- Chia tiền thành n phần: dùng `divide(n, scale, RoundingMode.DOWN)` rồi dồn phần dư vào các phần đầu để tổng khớp chính xác (allocation).
- Một value object `record Money(BigDecimal amount, Currency currency)` có compact constructor chuẩn hóa scale theo `currency.getDefaultFractionDigits()` → `equals` tự sinh của record đúng nghiệp vụ; cộng khác currency ném exception.

**Câu hỏi nối tiếp:**
- *So sánh `double` thì sao?* — Dùng epsilon hoặc `Double.compare`; `NaN != NaN` (dùng `Double.isNaN`); `1.0/0` là `Infinity` không ném exception, `1/0` (int) ném `ArithmeticException`.
- *`-7 % 3` bằng bao nhiêu?* — `-1` (dấu theo số bị chia); muốn kết quả không âm dùng `Math.floorMod(-7, 3) == 2`.

**⚠️ Câu trả lời gây điểm trừ:**
- "Dùng double rồi `Math.round` là đủ".
- So sánh `BigDecimal` bằng `equals` trong logic nghiệp vụ mà không biết về scale.

**📖 Ôn lại:** [Phần 4, mục 4.3 — Số thực và tiền tệ](../01-giao-trinh/01-java-core-oop.md#phan-4)

</details>

### Q18. 🔴 🧩 Comparator viết `(a, b) -> a.getPriority() - b.getPriority()` làm danh sách thỉnh thoảng sắp xếp sai. Vì sao? Kể thêm các bug overflow "im lặng" thực tế.

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Phép trừ có thể **tràn** khi hai giá trị trái dấu và lớn (ví dụ `Integer.MIN_VALUE - 1` thành số dương lớn) → dấu kết quả sai → thứ tự sai, thậm chí `TimSort` ném `IllegalArgumentException: Comparison method violates its general contract`. Dùng `Integer.compare(a, b)` hoặc `Comparator.comparingInt(...)`.

**Giải thích chi tiết:**
- Bug binary search kinh điển: `mid = (low + high) / 2` tràn khi mảng lớn (tồn tại trong JDK tới 2006) → dùng `low + (high - low) / 2` hoặc `(low + high) >>> 1`.
- Bộ đếm `int` vượt 2^31 sau vài tháng uptime (request counter, sequence) → âm → logic "nếu count > threshold" hỏng.
- Timestamp/thời lượng tính bằng `int` mili giây chỉ chứa được ~24,8 ngày.
- ID tự tăng `INT` trong DB đạt 2.147.483.647 → insert thất bại; migrate sang `BIGINT` trên bảng lớn rất tốn kém → chọn `long`/`BIGINT` ngay từ đầu cho giá trị tăng không giới hạn.
- Phòng thủ: `Math.*Exact` ở chỗ nghiệp vụ quan trọng (tiền, số lượng), test biên với `MAX_VALUE/MIN_VALUE`.

**Câu hỏi nối tiếp:**
- *Viết `saturatingAdd` không dùng long?* — `r = a + b`; overflow khi `((a ^ r) & (b ^ r)) < 0` (hai số cùng dấu, kết quả khác dấu) → trả về `MAX_VALUE` hoặc `MIN_VALUE` theo dấu của `a`.

**⚠️ Câu trả lời gây điểm trừ:**
- Nghĩ Java ném exception khi tràn số nguyên như một số ngôn ngữ khác.

**📖 Ôn lại:** [Phần 4, mục 4.2 & Góc nhìn Senior](../01-giao-trinh/01-java-core-oop.md#phan-4)

</details>

---

<a id="nhom-e"></a>
## E. OOP, overloading/overriding, binding

### Q19. 🟢 Giải thích 4 trụ cột OOP — theo cách bạn áp dụng trong code thật, không phải định nghĩa sách giáo khoa.

<details><summary>Đáp án</summary>

**Trả lời ngắn:**
- **Encapsulation**: bảo vệ **invariant** — expose *hành vi* (`account.withdraw(amount)`) chứ không phải dữ liệu (`setBalance`). Getter/setter cho mọi field *không* phải đóng gói.
- **Abstraction**: interface ổn định, hiện thực thay đổi được (`PaymentGateway` → `VnPayGateway`, `MomoGateway`).
- **Inheritance**: chỉ cho quan hệ is-a thật sự và class cha được thiết kế để kế thừa; coupling mạnh nhất → ưu tiên composition.
- **Polymorphism**: một lời gọi nhiều hành vi theo kiểu runtime — nền tảng của Open/Closed, Strategy, Spring DI.

**Giải thích chi tiết:**
- Ví dụ đóng gói tốt: `Order.addLine(line)` kiểm tra trạng thái (đã thanh toán thì cấm thêm) và cập nhật tổng tiền trong cùng một method → không ai đặt `order.setTotal(...)` lệch với lines.
- Anemic domain model (entity chỉ có getter/setter, logic dồn ở service) là dấu hiệu đóng gói kém — chấp nhận được cho CRUD đơn giản, nhưng nghiệp vụ phức tạp dễ sinh bug vi phạm invariant.

**Câu hỏi nối tiếp:**
- *Lombok `@Data` trên entity có vấn đề gì?* — Sinh setter mọi field (phá đóng gói), `equals/hashCode/toString` trên mọi field (lazy loading, vòng lặp quan hệ hai chiều → `StackOverflowError`).

**⚠️ Câu trả lời gây điểm trừ:**
- Đọc thuộc định nghĩa kèm ví dụ `Animal/Dog/Cat` mà không liên hệ được invariant hay thiết kế thực tế.

**📖 Ôn lại:** [Phần 5, mục 5.1 — Bốn trụ cột](../01-giao-trinh/01-java-core-oop.md#phan-5)

</details>

### Q20. 🟡 🔍 Vì sao "favor composition over inheritance"? Đoạn code sau in ra gì?

```java
public class InstrumentedHashSet<E> extends HashSet<E> {
    private int addCount = 0;
    @Override public boolean add(E e) { addCount++; return super.add(e); }
    @Override public boolean addAll(Collection<? extends E> c) { addCount += c.size(); return super.addAll(c); }
    public int getAddCount() { return addCount; }
}
var s = new InstrumentedHashSet<String>();
s.addAll(List.of("a", "b", "c"));
System.out.println(s.getAddCount());
```

<details><summary>Đáp án</summary>

**Trả lời ngắn:** In ra **6**. `HashSet.addAll` (kế thừa từ `AbstractCollection`) gọi lại `add()` cho từng phần tử — một chi tiết hiện thực ("self-use") mà class con vô tình phụ thuộc → đếm hai lần. Đó là vấn đề **fragile base class**: kế thừa phá vỡ đóng gói vì class con phụ thuộc vào cách class cha được viết, và có thể hỏng khi class cha nâng cấp.

**Giải thích chi tiết:**
- Giải pháp: **composition + forwarding** — `InstrumentedSet<E>` chứa một `Set<E>` và chuyển tiếp mọi lời gọi (`ForwardingSet`), chỉ thêm logic đếm. Bọc được bất kỳ `Set` nào (HashSet, TreeSet, `ConcurrentHashMap.newKeySet()`) — đó là Decorator pattern.
- Dùng kế thừa khi: quan hệ is-a thật, class cha được **thiết kế và document cho kế thừa** (Effective Java Item 19), hoặc template method có kiểm soát (`AbstractList` chỉ cần `get`/`size`). Còn lại đánh dấu `final` hoặc `sealed`.

**Câu hỏi nối tiếp:**
- *Spring dùng kế thừa ở đâu mà vẫn ổn?* — Các lớp `Abstract*` được thiết kế làm extension point có document rõ hook method; còn bean của ứng dụng thì Spring ưu tiên composition qua DI.

**⚠️ Câu trả lời gây điểm trừ:**
- Trả lời 3, hoặc biết là 6 nhưng không giải thích được self-use.

**📖 Ôn lại:** [Phần 5, mục 5.1 — Fragile base class](../01-giao-trinh/01-java-core-oop.md#phan-5)

</details>

### Q21. 🟢 Phân biệt overloading và overriding. Override phải tuân thủ những quy tắc gì?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Overloading: cùng tên, **khác danh sách tham số**, quyết định lúc **compile** theo kiểu khai báo. Overriding: class con định nghĩa lại method cùng chữ ký (sau erasure), quyết định lúc **runtime** theo kiểu thật của object. Quy tắc override: kiểu trả về giống hoặc **covariant**; access **không hẹp hơn**; **không ném checked exception rộng hơn/mới** (được bỏ bớt hoặc ném kiểu con); `static` thì chỉ *hide*, `private` không override được, `final` cấm override.

**Giải thích chi tiết:**
- Luôn dùng `@Override` — viết nhầm `equals(MyClass o)` thay vì `equals(Object o)` sẽ thành overload và compiler sẽ báo lỗi nếu có `@Override`.
- Lý do quy tắc checked exception: code gọi qua kiểu cha chỉ chuẩn bị xử lý exception của cha (Liskov).
- Overload chỉ khác kiểu trả về → lỗi compile.

**Câu hỏi nối tiếp:**
- *Override được constructor không?* — Không; constructor không kế thừa.
- *Class con override method `protected` thành `public` được không?* — Được (mở rộng access).

**⚠️ Câu trả lời gây điểm trừ:**
- "Static method override được" hoặc không biết quy tắc checked exception.

**📖 Ôn lại:** [Phần 5, mục 5.2 — Overloading vs Overriding](../01-giao-trinh/01-java-core-oop.md#phan-5)

</details>

### Q22. 🟡 🔍 Mỗi lời gọi chọn overload nào?

```java
static void f(long x)    { System.out.println("long"); }
static void f(Integer x) { System.out.println("Integer"); }
static void f(int... x)  { System.out.println("varargs"); }
static void g(Object o)  { System.out.println("Object"); }
static void g(String s)  { System.out.println("String"); }

f(5);
g(null);
Object o = "hi";
g(o);
```

<details><summary>Đáp án</summary>

**Trả lời ngắn:** `long`, `String`, `Object`.

**Giải thích chi tiết:** Chọn overload theo 3 pha (JLS §15.12.2), dừng ở pha đầu tiên có ứng viên:
1. Chỉ widening, không boxing/unboxing, không varargs → `f(5)`: `int → long` thắng ngay, boxing sang `Integer` không được xét.
2. Cho phép boxing/unboxing.
3. Cho phép varargs.

Trong một pha, chọn method **cụ thể nhất**: `g(null)` → `String` cụ thể hơn `Object`. Overload chọn theo **kiểu khai báo lúc compile**: `o` có kiểu `Object` nên gọi `g(Object)` dù runtime là `String` — đây là điểm khác cốt lõi với override.

**Câu hỏi nối tiếp:**
- *Nếu thêm `g(Integer)` thì `g(null)`?* — Lỗi compile *ambiguous*: `String` và `Integer` đều cụ thể hơn `Object` nhưng không cái nào là subtype của cái kia.
- *Muốn dispatch theo kiểu runtime của tham số (double dispatch)?* — Visitor pattern, hoặc Java 21 `switch` pattern matching trên sealed type.

**⚠️ Câu trả lời gây điểm trừ:**
- Trả lời `Integer` cho `f(5)` hoặc `String` cho `g(o)`.

**📖 Ôn lại:** [Phần 5, mục 5.2 — Quy trình chọn overload](../01-giao-trinh/01-java-core-oop.md#phan-5)

</details>

### Q23. 🟡 🔍 Đoạn code in ra gì? Thế nào là static binding và dynamic binding?

```java
class Parent {
    String name = "parent";
    static String kind() { return "Parent.static"; }
    String who() { return "Parent.who"; }
}
class Child extends Parent {
    String name = "child";
    static String kind() { return "Child.static"; }
    @Override String who() { return "Child.who"; }
}
Parent p = new Child();
System.out.println(p.name);
System.out.println(p.kind());
System.out.println(p.who());
```

<details><summary>Đáp án</summary>

**Trả lời ngắn:** `parent`, `Parent.static`, `Child.who`. Field và static method dùng **static binding** (theo kiểu khai báo `Parent`) — `Child.name` chỉ *hide* field của cha, `Child.kind()` chỉ *hide* static method. Instance method có thể override dùng **dynamic binding** → theo kiểu runtime `Child`.

**Giải thích chi tiết:**
- Static binding gồm: `private`, `static`, `final` method, constructor, field, và việc chọn overload.
- `Child` object thật ra có **hai** field `name`; `((Child) p).name` trả về `"child"`.
- Gọi static method qua instance (`p.kind()`) là code smell — IDE cảnh báo; luôn gọi qua tên class.

**Câu hỏi nối tiếp:**
- *Vì sao field không đa hình là thiết kế hợp lý?* — Field là chi tiết hiện thực; truy cập qua method mới là API. Giữ field `private` thì vấn đề hiding không xảy ra.

**⚠️ Câu trả lời gây điểm trừ:**
- Trả lời `child` cho dòng đầu.

**📖 Ôn lại:** [Phần 5, mục 5.3 — Static binding vs dynamic binding](../01-giao-trinh/01-java-core-oop.md#phan-5)

</details>

### Q24. 🔴 Dynamic dispatch hoạt động thế nào bên dưới JVM? Gọi method qua interface có chậm không?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** HotSpot dùng **vtable** cho mỗi class (mảng con trỏ method; class con copy vtable của cha và thay entry bị override) → `invokevirtual` tra theo index, O(1). `invokeinterface` dùng **itable** (tra phức tạp hơn một chút). Nhưng thực tế JIT dùng **inline cache** theo profile: call site **monomorphic** (1 kiểu) hoặc **bimorphic** (2 kiểu) được inline trực tiếp kèm type guard — gần như miễn phí; **megamorphic** (≥3 kiểu) phải tra bảng thật và mất cơ hội inline.

**Giải thích chi tiết:**
- Mất inline không chỉ tốn một lần tra bảng; quan trọng hơn là mất các tối ưu dây chuyền (escape analysis, constant folding) vốn cần inline.
- Nếu giả định monomorphic bị phá (nạp class mới), JVM **deoptimize** và biên dịch lại.
- Hệ quả thực tế: interface với 5+ hiện thực gọi xen kẽ trong hot path có thể chậm bất ngờ; nhưng chỉ tối ưu khi profiler (JFR, async-profiler, JITWatch) chỉ ra — đa số code nghiệp vụ không bị ảnh hưởng đáng kể.
- `final` method/class không còn là "tối ưu hiệu năng" đáng kể ở JVM hiện đại vì CHA (Class Hierarchy Analysis) đã biết method chưa bị override.

**Câu hỏi nối tiếp:**
- *Vậy có nên tránh interface cho nhanh?* — Không; thiết kế theo interface là đúng. Chỉ xử lý megamorphic ở hot path đã đo được (ví dụ chuyển sang switch trên sealed type, hoặc tách call site).

**⚠️ Câu trả lời gây điểm trừ:**
- "Gọi interface chậm nên nên dùng class cụ thể" — tối ưu sớm, không hiểu JIT.

**📖 Ôn lại:** [Phần 5, mục 5.3 & Góc nhìn Senior](../01-giao-trinh/01-java-core-oop.md#phan-5)

</details>

### Q25. 🟡 Liskov Substitution Principle là gì? Cho ví dụ vi phạm thực tế và cách phát hiện.

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Class con phải thay thế được class cha **mà không phá hợp đồng** (precondition không chặt hơn, postcondition không lỏng hơn, invariant được giữ). Ví dụ kinh điển: `Square extends Rectangle` — `setWidth` đổi luôn chiều cao, code `r.setWidth(5); r.setHeight(4); assert area == 20` hỏng với `Square`.

**Giải thích chi tiết:**
- Ví dụ thực tế hơn: `ReadOnlyList extends ArrayList` ném `UnsupportedOperationException` ở `add` — chính JDK cũng "vi phạm" bằng các unmodifiable collection (đã được document là optional operation).
- `equals` dùng `getClass()` khiến subclass không bao giờ bằng cha — một dạng LSP vi phạm (xem Q33).
- Quy tắc override (không thu hẹp access, không thêm checked exception) chính là LSP được compiler kiểm tra một phần; phần ngữ nghĩa phải kiểm bằng **contract test** dùng chung cho mọi hiện thực (abstract test class).
- Giải pháp: tách interface theo khả năng (`Shape` có `area()`, không có setter), immutable value object, composition.

**Câu hỏi nối tiếp:**
- *LSP liên quan gì tới sealed class?* — Sealed giới hạn tập hiện thực đã biết, giúp kiểm soát và kiểm thử hợp đồng cho từng subtype.

**⚠️ Câu trả lời gây điểm trừ:**
- Chỉ đọc định nghĩa, không có ví dụ vi phạm hay cách phát hiện.

**📖 Ôn lại:** [Phần 5 — Góc nhìn Senior](../01-giao-trinh/01-java-core-oop.md#phan-5)

</details>

### Q26. 🔴 Sealed class + record + pattern matching là một kiểu "đa hình" khác. Khi nào bạn chọn nó thay cho đa hình OOP cổ điển?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** OOP cổ điển đặt hành vi **trong từng class** — dễ thêm *kiểu mới*, khó thêm *thao tác mới* (phải sửa mọi class). Sealed + record + switch exhaustive (algebraic data type) đặt hành vi **trong switch** — dễ thêm *thao tác mới*, và khi thêm kiểu mới thì **compiler chỉ ra mọi chỗ chưa xử lý**. Chọn ADT khi tập kiểu đóng và ổn định nhưng nhiều thao tác (AST, command/event, kết quả `Success | Failure`, trạng thái); chọn OOP khi tập kiểu mở (plugin, extension point).

**Giải thích chi tiết:**

```java
sealed interface PaymentResult permits Approved, Declined, Pending {}
record Approved(String txId) implements PaymentResult {}
record Declined(String reason) implements PaymentResult {}
record Pending(Duration retryAfter) implements PaymentResult {}

String toMessage(PaymentResult r) {
    return switch (r) {                      // Java 21, không cần default
        case Approved a  -> "OK " + a.txId();
        case Declined d  -> "Từ chối: " + d.reason();
        case Pending p   -> "Thử lại sau " + p.retryAfter();
    };
}
```

- Đây là "expression problem": không cách nào dễ cả hai chiều; chọn theo chiều nào hay thay đổi.
- `non-sealed` mở lại một nhánh cho kế thừa tự do; subclass được `permits` phải là `final`, `sealed` hoặc `non-sealed`.
- Tiến hóa API: thêm subtype vào sealed hierarchy là thay đổi phá vỡ với client có switch exhaustive (runtime `MatchException` nếu client chưa recompile) — cân nhắc khi sealed type nằm trong thư viện public.

**Câu hỏi nối tiếp:**
- *Tránh `default` trong switch trên sealed type vì sao?* — `default` nuốt mất lợi ích kiểm tra exhaustive: thêm subtype mới sẽ rơi vào default thay vì báo lỗi compile.

**⚠️ Câu trả lời gây điểm trừ:**
- "Pattern matching chỉ là cú pháp gọn của instanceof" — bỏ qua giá trị exhaustive checking.

**📖 Ôn lại:** [Phần 5, mục 5.4 — Sealed classes và records](../01-giao-trinh/01-java-core-oop.md#phan-5)

</details>

---

<a id="nhom-f"></a>
## F. Abstract class, interface, nested class

### Q27. 🟢 Từ Java 8 interface có default method, vậy abstract class còn cần không? Khi nào dùng cái nào?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Còn. Abstract class có **trạng thái (instance field)**, constructor, method mọi mức access → dùng để chia sẻ *trạng thái + hiện thực* giữa các class có quan hệ chặt (Template Method, skeletal implementation như `AbstractList`). Interface không có trạng thái, cho phép một class có nhiều vai trò → dùng để định nghĩa *hợp đồng/khả năng*. Default method (Java 8) ra đời để **tiến hóa interface** (thêm `stream()`, `forEach`, `removeIf` vào `Collection`) mà không phá các class implement sẵn có.

**Giải thích chi tiết:**
- Interface: abstract/default/static ngầm định `public`; Java 9 thêm `private`/`private static` method làm helper cho default method; field luôn là `public static final`.
- Static method của interface không kế thừa qua class implement: `List.of(...)` được, `ArrayList.of(...)` không tồn tại.
- Kết hợp phổ biến: interface công khai + abstract class skeletal (`Map` + `AbstractMap`) — client lập trình theo interface, người hiện thực tái sử dụng code (Effective Java Item 20).

**Câu hỏi nối tiếp:**
- *Default method có rủi ro gì?* — Thêm default trùng tên với method có sẵn ở implementation bên ngoài → hành vi thay đổi hoặc lỗi compile phía client; default method không biết invariant của class implement (ví dụ `removeIf` trên collection đồng bộ hóa).

**⚠️ Câu trả lời gây điểm trừ:**
- "Interface không có method có thân" (sai từ Java 8), hoặc "interface có thể có field instance".

**📖 Ôn lại:** [Phần 6, mục 6.1](../01-giao-trinh/01-java-core-oop.md#phan-6)

</details>

### Q28. 🟡 🔍 Diamond problem với default method được giải quyết thế nào? Đoạn code sau có compile không?

```java
interface Flyer   { default String move() { return "fly"; } }
interface Swimmer { default String move() { return "swim"; } }
class Duck implements Flyer, Swimmer { }
```

<details><summary>Đáp án</summary>

**Trả lời ngắn:** **Không compile** — `Duck` kế thừa hai default không liên quan cùng chữ ký nên bắt buộc override. Ba quy tắc: (1) **class thắng** — method trong class/class cha ưu tiên hơn mọi default; (2) **interface cụ thể hơn thắng** — `B extends A` thì default của `B` thắng; (3) còn lại mơ hồ → bắt buộc override, có thể chọn bằng `X.super.m()`.

**Giải thích chi tiết:**

```java
class Duck implements Flyer, Swimmer {
    @Override public String move() { return Flyer.super.move() + "+" + Swimmer.super.move(); }
}
```

- Java không có diamond về **trạng thái** (interface không có field instance) — chỉ có diamond về hành vi, nên giải quyết được bằng quy tắc trên.
- Quy tắc "class thắng" bảo vệ tương thích ngược: thêm default vào interface không đổi hành vi của class đã có method cùng chữ ký.

**Câu hỏi nối tiếp:**
- *Nếu `Swimmer` khai báo `move()` abstract thay vì default?* — Vẫn phải override trong `Duck` (abstract và default xung đột → class phải tự quyết).

**⚠️ Câu trả lời gây điểm trừ:**
- "Java không có đa kế thừa nên không có diamond".

**📖 Ôn lại:** [Phần 6, mục 6.1 — Diamond problem](../01-giao-trinh/01-java-core-oop.md#phan-6)

</details>

### Q29. 🟢 Có mấy loại nested class? Vì sao Effective Java khuyên mặc định dùng static nested class?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** 4 loại: **static nested** (không tham chiếu outer instance — `Map.Entry`, `Builder`, `HashMap.Node`), **inner** (non-static, giữ ngầm `Outer.this` — iterator của collection), **local** (khai báo trong method), **anonymous**. Mặc định dùng static nested vì inner class **giữ tham chiếu ẩn tới outer** → tốn bộ nhớ, có thể **giữ outer không cho GC** (memory leak), và serialize không an toàn.

**Giải thích chi tiết:**
- Inner class tạo bằng `outer.new Inner()`; compiler sinh field `this$0`.
- Từ Java 18 (JDK-8271623), javac bỏ `this$0` nếu inner/anonymous class không dùng tới outer — nhưng đừng dựa vào đó khi code phải chạy nhiều phiên bản.
- Biến local được capture phải **effectively final** vì class nhận *bản sao giá trị* — biến local sống trên stack và có thể chết trước object nested.

**Câu hỏi nối tiếp:**
- *Builder pattern dùng loại nào?* — Static nested (`new Pizza.Builder()` không cần instance `Pizza`).

**⚠️ Câu trả lời gây điểm trừ:**
- Không phân biệt được static nested và inner, hoặc không biết `this$0`.

**📖 Ôn lại:** [Phần 6, mục 6.2 — Nested classes](../01-giao-trinh/01-java-core-oop.md#phan-6)

</details>

### Q30. 🔴 🧩 Heap dump cho thấy hàng trăm object `Screen` 10MB không được giải phóng, GC root path là `EventBus.LISTENERS → Screen$1 → this$0 → Screen`. Phân tích và sửa. Lambda có gây vấn đề tương tự không?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** `Screen$1` là anonymous class tạo trong instance method `register()`, giữ `this$0` trỏ về `Screen`. Listener được đăng ký vào một `static List` sống mãi → `Screen` không bao giờ bị GC. Sửa: cơ chế **unregister** rõ ràng (API `register` trả về `AutoCloseable`/`Subscription`), listener là **static nested class**, `WeakReference` chỉ làm lưới an toàn. Lambda **chỉ** capture `this` nếu thân lambda dùng member của outer — nếu dùng thì leak tương tự.

**Giải thích chi tiết:**

```java
static final class ScreenListener implements Listener {
    private final WeakReference<Screen> ref;
    ScreenListener(Screen s) { this.ref = new WeakReference<>(s); }
    public void onEvent(Event e) { Screen s = ref.get(); if (s != null) s.handle(e); }
}
```

- Khác biệt lambda vs anonymous class: `this` trong lambda là instance bao ngoài, trong anonymous là chính anonymous object; lambda không sinh file `.class` lúc compile mà dùng `invokedynamic` + `LambdaMetafactory` lúc runtime; lambda không capture gì có thể là singleton.
- Mẫu leak tương tự: callback đăng ký vào scheduler/thread pool sống lâu, `ThreadLocal` trong thread pool, cache static, listener Spring `ApplicationEventMulticaster` với bean prototype.
- Quy trình điều tra: `-XX:+HeapDumpOnOutOfMemoryError` → Eclipse MAT → dominator tree → "Path to GC Roots (exclude weak/soft)".

**Câu hỏi nối tiếp:**
- *Vì sao WeakReference chỉ là lưới an toàn?* — Thời điểm thu hồi phụ thuộc GC; trước đó listener vẫn chạy trên object "đã chết" về mặt nghiệp vụ.

**⚠️ Câu trả lời gây điểm trừ:**
- "Tăng heap" hoặc "gọi `System.gc()`".

**📖 Ôn lại:** [Phần 6, mục 6.2 & Góc nhìn Senior](../01-giao-trinh/01-java-core-oop.md#phan-6)

</details>

---

<a id="nhom-g"></a>
## G. Các method của `Object`

### Q31. 🟢 Nêu hợp đồng của `equals` và `hashCode`. Override `equals` mà quên `hashCode` thì sao?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** `equals` phải **reflexive, symmetric, transitive, consistent**, và `x.equals(null) == false`. `hashCode`: ổn định trong một lần chạy nếu object không đổi; **`a.equals(b)` ⇒ cùng hashCode** (bắt buộc); hai object khác nhau không bắt buộc khác hash. Quên `hashCode` → hai object "bằng nhau" có hash khác → `HashSet` chứa cả hai, `HashMap.get` trả `null` vì tìm sai bucket.

**Giải thích chi tiết:**
- `hashCode` trả về hằng số vẫn **đúng hợp đồng** nhưng biến HashMap thành danh sách/cây → O(n)/O(log n).
- `hashCode` mặc định là *identity hash* — **không phải địa chỉ bộ nhớ**: HotSpot sinh giá trị (mặc định theo thuật toán xorshift theo thread) và lưu vào object header lần đầu gọi; `System.identityHashCode` luôn trả giá trị này.
- Field tham gia `equals/hashCode` nên bất biến; so `float/double` bằng `Float.compare/Double.compare` (xử lý `NaN`, `-0.0`); mảng dùng `Arrays.equals/hashCode`.
- Kiểm tra tự động: thư viện **EqualsVerifier** trong unit test.

**Câu hỏi nối tiếp:**
- *Record có tự sinh đúng không?* — Có, dựa trên mọi component; nhưng component là mảng thì so reference — phải override.

**⚠️ Câu trả lời gây điểm trừ:**
- "hashCode là địa chỉ bộ nhớ của object".

**📖 Ôn lại:** [Phần 7, mục 7.1–7.3](../01-giao-trinh/01-java-core-oop.md#phan-7)

</details>

### Q32. 🟡 🔍 Đoạn code sau in ra gì? Bug ở đâu?

```java
class Product {
    final String sku;
    Product(String sku) { this.sku = sku; }
    public boolean equals(Product o) { return o != null && sku.equals(o.sku); }
    @Override public int hashCode() { return sku.hashCode(); }
}
Set<Product> set = new HashSet<>();
set.add(new Product("A"));
System.out.println(set.contains(new Product("A")));
System.out.println(new Product("A").equals(new Product("A")));
```

<details><summary>Đáp án</summary>

**Trả lời ngắn:** `false` rồi `true`. `equals(Product)` là **overload**, không override `equals(Object)`. `HashMap` bên trong `HashSet` gọi `key.equals(k)` với `k` kiểu tĩnh `Object` → chọn `Object.equals(Object)` (so identity) → không tìm thấy. Lời gọi thứ hai có đối số kiểu tĩnh `Product` nên chọn overload mới.

**Giải thích chi tiết:**
- Sửa: `@Override public boolean equals(Object o) { if (this == o) return true; if (!(o instanceof Product p)) return false; return sku.equals(p.sku); }`.
- Có `@Override` trên chữ ký sai thì compiler báo lỗi ngay — lý do luôn dùng `@Override`.
- Đây là giao điểm của hai kiến thức: overload chọn lúc compile theo kiểu khai báo (Q22) và hợp đồng equals.

**Câu hỏi nối tiếp:**
- *`Objects.hash(...)` có nhược điểm gì?* — Tạo mảng varargs và boxing mỗi lần gọi; hot path nên viết tay `31 * r + ...`.

**⚠️ Câu trả lời gây điểm trừ:**
- Trả lời `true` cả hai vì "đã có equals và hashCode".

**📖 Ôn lại:** [Phần 7 — Lỗi thường gặp](../01-giao-trinh/01-java-core-oop.md#phan-7), [Phần 5, mục 5.2](../01-giao-trinh/01-java-core-oop.md#phan-5)

</details>

### Q33. 🔴 Trong `equals`, nên dùng `instanceof` hay `getClass()`? Trường hợp `ColorPoint extends Point` thì sao?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Không có lựa chọn hoàn hảo. `getClass()` giữ symmetric nhưng **vi phạm Liskov** (subclass không thêm field vẫn không bao giờ bằng cha) và **hỏng với Hibernate proxy** (`Employee$HibernateProxy$...` khác class entity). `instanceof` thân thiện kế thừa, nhưng nếu subclass **thêm field tham gia so sánh** thì không thể giữ cả symmetric lẫn transitive — "không có cách nào kế thừa một class instantiable, thêm value component mà vẫn giữ hợp đồng equals" (Effective Java Item 10). Giải pháp: class `final` + `instanceof`, hoặc **composition** (`ColorPoint` chứa `Point`).

**Giải thích chi tiết:**
- `ColorPoint.equals` dùng `instanceof ColorPoint` → `point.equals(cp) == true` nhưng `cp.equals(point) == false` → vi phạm symmetric.
- "Sửa" bằng cách bỏ qua màu khi so với `Point` thường → `cp1(red).equals(p)`, `p.equals(cp2(blue))` đều true nhưng `cp1.equals(cp2)` false → vi phạm transitive.
- Với entity JPA, `instanceof` (hoặc `Hibernate.getClass()` / so sánh class đã unproxy) là lựa chọn thực tế.
- Record và class `final` tránh hoàn toàn vấn đề vì không có subclass.

**Câu hỏi nối tiếp:**
- *Vi phạm symmetric gây bug gì thực tế?* — `list.contains(x)` cho kết quả khác nhau tùy phía gọi `equals`; `HashSet` loại trùng không nhất quán khi thứ tự thêm khác nhau.

**⚠️ Câu trả lời gây điểm trừ:**
- Chọn một cách và cho là "luôn đúng" mà không nêu trade-off.

**📖 Ôn lại:** [Phần 7, mục 7.3 — `instanceof` hay `getClass()`?](../01-giao-trinh/01-java-core-oop.md#phan-7)

</details>

### Q34. 🔴 🧩 Viết `equals/hashCode` cho entity JPA có `@Id @GeneratedValue Long id` như thế nào? Dùng `Objects.hash(id)` có sao không?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Có vấn đề: trước khi persist `id == null`, sau persist `id` có giá trị → **hashCode thay đổi** trong khi entity đang nằm trong `HashSet` (ví dụ collection `@OneToMany Set<OrderLine>`) → không tìm/xóa được nữa; mọi entity mới (id null) lại "bằng nhau". Cách phổ biến: (1) dùng **business key bất biến** (mã đơn, email, ISBN) nếu có; (2) hoặc `equals` so `id` **khi khác null**, còn `hashCode` trả về **hằng số theo class** (`getClass().hashCode()`), chấp nhận HashSet kém hiệu quả cho tập nhỏ.

**Giải thích chi tiết:**

```java
@Override public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Order other)) return false;         // instanceof: chịu được Hibernate proxy
    return id != null && id.equals(other.getId());          // getId() để proxy khởi tạo đúng
}
@Override public int hashCode() { return getClass().hashCode(); } // ổn định qua persist
```

- Một lựa chọn khác: sinh ID phía ứng dụng (UUID/ULID, TSID) ngay trong constructor → id bất biến từ đầu, `equals/hashCode` theo id đơn giản.
- Không dùng Lombok `@Data`/`@EqualsAndHashCode` mặc định trên entity (bao gồm mọi field + quan hệ lazy).

**Câu hỏi nối tiếp:**
- *Vì sao gọi `other.getId()` thay vì `other.id`?* — `other` có thể là proxy chưa khởi tạo; field của proxy là null, getter mới ủy quyền đúng.

**⚠️ Câu trả lời gây điểm trừ:**
- "Dùng IDE generate theo mọi field" hoặc "theo id là xong".

**📖 Ôn lại:** [Phần 7 — Góc nhìn Senior về entity JPA](../01-giao-trinh/01-java-core-oop.md#phan-7)

</details>

### Q35. 🟡 Shallow copy và deep copy khác nhau thế nào? Vì sao `Cloneable` bị coi là thiết kế lỗi, nên dùng gì thay?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Shallow copy copy từng field; field reference vẫn trỏ cùng object. Deep copy copy cả đồ thị object có thể thay đổi. `Object.clone()` mặc định shallow. `Cloneable` lỗi vì: marker interface không có method nhưng lại thay đổi hành vi một method `protected` của `Object`; `clone` bỏ qua constructor (bỏ validate); xung đột với field `final`; ném checked `CloneNotSupportedException`. Thay bằng **copy constructor** hoặc **static factory** (`Team.copyOf(other)`).

**Giải thích chi tiết:**
- Ngoại lệ hợp lý: `array.clone()` là public, đúng kiểu — cách copy mảng được khuyến nghị (vẫn shallow với mảng nhiều chiều).
- Deep copy đồ thị có chu trình: duyệt DFS/BFS với `IdentityHashMap<Node, Node>` (identity chứ không phải `equals`) để không lặp vô hạn và không chia sẻ node.
- Deep copy bằng serialization: chậm và có rủi ro; dùng MapStruct hoặc viết tay cho rõ.
- Object immutable thì không cần copy — chia sẻ trực tiếp.

**Câu hỏi nối tiếp:**
- *Copy constructor có hạn chế gì?* — Không đa hình (biết kiểu tĩnh); giải quyết bằng method `copy()` trừu tượng nếu thật sự cần.

**⚠️ Câu trả lời gây điểm trừ:**
- Cho rằng `clone()` mặc định là deep copy.

**📖 Ôn lại:** [Phần 7, mục 7.5 — `clone`](../01-giao-trinh/01-java-core-oop.md#phan-7)

</details>

### Q36. 🟡 Vì sao `finalize()` bị deprecated? Dọn tài nguyên native/file đúng cách thế nào?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** `finalize` deprecated từ Java 9 và **for removal từ Java 18 (JEP 421)** vì: không đảm bảo được gọi, không biết khi nào; chạy trên một finalizer thread → finalize chậm làm queue ứ và **OOM**; object cần ít nhất 2 chu kỳ GC; có thể "hồi sinh" object; là vector tấn công (finalizer attack). Thay thế: **`AutoCloseable` + try-with-resources** để dọn chủ động; **`java.lang.ref.Cleaner`** (Java 9) chỉ làm lưới an toàn.

**Giải thích chi tiết:**
- Mẫu `Cleaner`: action là static nested class giữ *state* (ví dụ địa chỉ native), **không** tham chiếu tới object được theo dõi — nếu tham chiếu thì object không bao giờ unreachable.
- `cleanable.clean()` chạy đúng một lần dù gọi từ `close()` hay từ Cleaner thread.
- Có thể chạy với `--finalization=disabled` (Java 18+) để kiểm tra ứng dụng/thư viện còn phụ thuộc finalization không.

**Câu hỏi nối tiếp:**
- *Finalizer attack là gì?* — Subclass độc hại override `finalize` để giữ lại `this` của object mà constructor đã ném exception (chưa được validate). Phòng: class `final`, hoặc khai báo `protected final void finalize() {}` rỗng, hoặc validate và ném exception *trước khi* constructor của `Object` chạy xong (ví dụ validate trong static method dùng làm đối số của `this(...)`) — khi đó JVM không đăng ký finalizer cho object.

**⚠️ Câu trả lời gây điểm trừ:**
- Đề xuất dùng `finalize` để đóng connection.

**📖 Ôn lại:** [Phần 7, mục 7.6 — `finalize` đã chết](../01-giao-trinh/01-java-core-oop.md#phan-7)

</details>

---

<a id="nhom-h"></a>
## H. Immutability

### Q37. 🟡 Viết một class immutable cần những gì? `final` trên field có làm object immutable không?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** 5 quy tắc (Effective Java Item 17): không có mutator; class không kế thừa được (`final` hoặc constructor private + factory); mọi field `private final`; **defensive copy** thành phần mutable khi nhận vào và khi trả ra; không để `this` escape trong constructor. `final` chỉ cấm **gán lại reference**: `final List<String> list = new ArrayList<>(); list.add("x");` hoàn toàn hợp lệ.

**Giải thích chi tiết:**

```java
public final class Order {
    private final String id;
    private final List<OrderLine> lines;
    public Order(String id, List<OrderLine> lines) {
        this.id = Objects.requireNonNull(id);
        this.lines = List.copyOf(lines);          // copy TRƯỚC (immutable, chặn null)
        if (this.lines.isEmpty()) throw new IllegalArgumentException(); // validate SAU, trên bản copy
    }
    public List<OrderLine> lines() { return lines; } // đã immutable → trả thẳng
}
```

- **Thứ tự copy trước, validate sau**: validate trên tham số gốc rồi mới copy → thread khác sửa tham số giữa hai bước (**TOCTOU**, Item 50).
- Không dùng `clone()` của tham số kiểu không `final` (ví dụ `Date` subclass độc hại) để defensive copy.
- `final` có 4 ngữ cảnh: biến local/tham số (không gán lại), field (gán đúng 1 lần + đảm bảo JMM), method (cấm override), class (cấm kế thừa).

**Câu hỏi nối tiếp:**
- *Cái giá của immutability?* — Mỗi thay đổi tạo object mới; object lớn sửa nhiều → Builder hoặc companion mutable class. Object ngắn hạn rất rẻ với GC thế hệ hiện đại.
- *`Collections.unmodifiableList(list)` có phải immutable?* — Không, chỉ là **view**; list gốc đổi thì view đổi theo.

**⚠️ Câu trả lời gây điểm trừ:**
- "Chỉ cần mọi field final là immutable".

**📖 Ôn lại:** [Phần 8, mục 8.1](../01-giao-trinh/01-java-core-oop.md#phan-8), [Phần 9, mục 9.2](../01-giao-trinh/01-java-core-oop.md#phan-9)

</details>

### Q38. 🟡 🔍 Record có immutable không? Đoạn code sau có vấn đề gì?

```java
public record Team(String name, List<String> members) {}

List<String> m = new ArrayList<>(List.of("An"));
Team t = new Team("core", m);
m.add("Bình");
System.out.println(t.members());
```

<details><summary>Đáp án</summary>

**Trả lời ngắn:** In `[An, Bình]` — record chỉ **shallowly immutable**: field `final` (reference không đổi) nhưng object được tham chiếu vẫn mutable và caller đang giữ nó. Sửa bằng compact canonical constructor: `public Team { Objects.requireNonNull(name); members = List.copyOf(members); }`.

**Giải thích chi tiết:**
- Accessor `members()` trả về chính list bên trong; sau khi `List.copyOf` thì list đã immutable nên trả thẳng được.
- Component là mảng: `equals/hashCode` tự sinh so reference, và mảng luôn mutable → phải clone khi vào/ra và override `equals/hashCode/toString` — hoặc tránh mảng trong record.
- Record: `final`, extends `java.lang.Record`, không có instance field ngoài component, deserialize **qua canonical constructor** (validate được áp dụng — an toàn hơn class thường).

**Câu hỏi nối tiếp:**
- *Record dùng làm entity JPA được không?* — Không phù hợp (Hibernate cần no-arg constructor, proxy, field mutable); record hợp làm DTO, projection, value object, event.

**⚠️ Câu trả lời gây điểm trừ:**
- "Record là immutable hoàn toàn".

**📖 Ôn lại:** [Phần 8, mục 8.2 — Record và "shallowly immutable"](../01-giao-trinh/01-java-core-oop.md#phan-8)

</details>

### Q39. 🔴 Vì sao object immutable là thread-safe "tự nhiên"? `final` field có đảm bảo gì trong Java Memory Model? Khi nào đảm bảo đó mất?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** JLS §17.5: sau khi constructor kết thúc, mọi thread thấy giá trị đúng của field `final` (và của object đạt được qua field đó tại thời điểm khởi tạo) **mà không cần đồng bộ** — kể cả khi reference được publish qua data race. Không có trạng thái thay đổi thì không có race. Đảm bảo **mất** nếu `this` **escape trong constructor** (đăng ký listener, start thread, gán vào static) → thread khác có thể thấy object chưa khởi tạo xong.

**Giải thích chi tiết:**
- Field không `final` publish qua data race: thread khác có thể thấy reference non-null nhưng field vẫn là giá trị mặc định (do reordering) — đó là lý do double-checked locking cần `volatile`.
- "Freeze" xảy ra ở cuối constructor; JIT chèn barrier (StoreStore) phù hợp.
- Cách publish an toàn cho object mutable: `volatile`, `final` field của object khác, lock, concurrent collection, static initializer.

**Câu hỏi nối tiếp:**
- *Start thread trong constructor sai ở đâu?* — Thread mới có thể chạy trước khi constructor xong, đọc field chưa gán; dùng static factory: tạo object xong rồi mới start/đăng ký.

**⚠️ Câu trả lời gây điểm trừ:**
- "Immutable thread-safe vì JVM tự lock" hoặc không biết khái niệm `this` escape.

**📖 Ôn lại:** [Phần 8, mục 8.3 — Immutability và memory model](../01-giao-trinh/01-java-core-oop.md#phan-8)

</details>

---

<a id="nhom-i"></a>
## I. `static`, `final`, khởi tạo, access modifier

### Q40. 🟡 🔍 `new Derived()` in ra gì? Bug ở đâu?

```java
class Base {
    static { System.out.println("1. Base static"); }
    { System.out.println("3. Base instance block"); }
    Base() { System.out.println("4. Base constructor"); init(); }
    void init() {}
}
class Derived extends Base {
    static { System.out.println("2. Derived static"); }
    private String name = "derived";
    { System.out.println("5. Derived instance block, name=" + name); }
    Derived() { System.out.println("6. Derived constructor"); }
    @Override void init() { System.out.println("   Derived.init name=" + name); }
}
```

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Thứ tự: `1. Base static`, `2. Derived static`, `3. Base instance block`, `4. Base constructor`, `   Derived.init name=null`, `5. Derived instance block, name=derived`, `6. Derived constructor`. Lần `new` thứ hai không in lại static. Bug: **gọi method có thể override trong constructor** — `Derived.init` chạy khi field của `Derived` chưa được gán (`name` vẫn `null`).

**Giải thích chi tiết:**
- Khởi tạo class: class cha trước; static initializer và `static {}` theo thứ tự trong mã nguồn; chạy 1 lần.
- Khởi tạo instance: cấp phát (field = mặc định) → constructor gọi `super(...)`/`this(...)` → constructor cha xong → instance initializer và `{}` của class hiện tại theo thứ tự nguồn → phần thân còn lại.
- Sửa: không gọi overridable method trong constructor (đánh dấu `private`/`final`), hoặc tách bước khởi tạo ra factory.

**Câu hỏi nối tiếp:**
- *Truy cập `Child.CONSTANT` (compile-time constant) có khởi tạo `Child` không?* — Không: constant được inline vào class gọi. Truy cập static field không phải constant thì khởi tạo cả `Parent` và `Child`.

**⚠️ Câu trả lời gây điểm trừ:**
- Cho rằng field `name` đã là `"derived"` khi `init()` chạy.

**📖 Ôn lại:** [Phần 9, mục 9.3 — Thứ tự khởi tạo](../01-giao-trinh/01-java-core-oop.md#phan-9)

</details>

### Q41. 🔴 Nêu các cách viết singleton thread-safe. Vì sao double-checked locking phải có `volatile`? Cách nào chống được reflection và serialization?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** (1) Eager `static final`; (2) lazy `synchronized` method (đơn giản, lock mỗi lần gọi); (3) **double-checked locking + `volatile`**; (4) **holder idiom** — lazy, thread-safe nhờ JVM khởi tạo class có lock và một lần; (5) **enum** — chống được cả reflection lẫn serialization. Không có `volatile`, `instance = new X()` có thể bị reorder: gán reference **trước** khi constructor chạy xong → thread khác thấy object chưa hoàn chỉnh.

**Giải thích chi tiết:**

```java
public final class Config {
    private Config() { /* load nặng */ }
    private static final class Holder { static final Config INSTANCE = new Config(); }
    public static Config getInstance() { return Holder.INSTANCE; }
}
```

- Enum: JVM cấm tạo instance enum bằng reflection (`Cannot reflectively create enum objects`), serialize theo tên → `readObject` trả về đúng hằng. Nhược: không lazy, không kế thừa class khác, khó mock.
- Class thường + `Serializable` cần `readResolve()` để không sinh instance thứ hai.
- Trong ứng dụng Spring, để container quản lý singleton — static singleton khó test, khó thay thế.

**Câu hỏi nối tiếp:**
- *Holder idiom gặp lỗi gì nếu constructor ném exception?* — Lần đầu `ExceptionInInitializerError`, các lần sau `NoClassDefFoundError` — không retry được.

**⚠️ Câu trả lời gây điểm trừ:**
- Viết double-checked locking không có `volatile`, hoặc không giải thích được vì sao cần.

**📖 Ôn lại:** [Phần 9, mục 9.3 & bài 9.2](../01-giao-trinh/01-java-core-oop.md#phan-9), [Phần 10, mục 10.4](../01-giao-trinh/01-java-core-oop.md#phan-10)

</details>

### Q42. 🔴 🧩 Team bạn đổi `public static final int TIMEOUT = 30;` thành `60` trong thư viện `common`, deploy lại `common.jar`, nhưng service `order` (không build lại) vẫn dùng 30. Vì sao?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** `static final` kiểu primitive/`String` khởi tạo bằng constant expression là **compile-time constant** — javac **inline giá trị** vào bytecode của class sử dụng. `order` được biên dịch lúc `TIMEOUT = 30` nên con số 30 nằm trong `.class` của `order`; thay jar `common` không đổi được gì. Phải build lại mọi module phụ thuộc, hoặc tránh dùng constant cho giá trị có thể thay đổi.

**Giải thích chi tiết:**
- Cách tránh: đọc từ cấu hình (Spring `@ConfigurationProperties`), hoặc ép thành không phải constant: `public static final int TIMEOUT = Integer.parseInt("30");`/getter `static int timeout()`.
- Hệ quả khác của inline: truy cập constant **không kích hoạt khởi tạo class** chứa nó.
- Cùng họ "binary compatibility": thêm hằng enum, đổi chữ ký method, đổi default method — xem JLS chương 13; build CI nên rebuild cả cây phụ thuộc thay vì vá jar.

**Câu hỏi nối tiếp:**
- *`public static final List<String> ROLES = List.of(...)` có bị inline không?* — Không (không phải constant expression); và nó không thể sửa vì `List.of` immutable, nhưng `static final ArrayList` thì vẫn sửa được nội dung.

**⚠️ Câu trả lời gây điểm trừ:**
- "Do cache của JVM" hoặc "do classpath load jar cũ" mà không biết constant inlining.

**📖 Ôn lại:** [Phần 9, mục 9.2 — `final` & compile-time constant](../01-giao-trinh/01-java-core-oop.md#phan-9)

</details>

### Q43. 🔴 🧩 Service treo ngay lúc khởi động, thread dump cho thấy hai thread đứng trong `<clinit>` của hai class khác nhau. Chuyện gì xảy ra?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** **Deadlock khởi tạo class**: thread 1 khởi tạo `A` (giữ init lock của `A`), static initializer của `A` dùng `B`; thread 2 đồng thời khởi tạo `B` (giữ lock của `B`), static initializer của `B` dùng `A` → mỗi thread chờ class kia. Sửa bằng cách **phá vòng phụ thuộc** giữa các static initializer, gom khởi tạo về một nơi, hoặc khởi tạo eagerly trên một thread duy nhất lúc startup.

**Giải thích chi tiết:**

```java
class A { static final Object X; static { sleep(100); X = B.Y; } }
class B { static final Object Y; static { sleep(100); Y = A.X; } }
// t1: A.X   t2: B.Y   → treo
```

- Thread dump (`jstack`, `jcmd <pid> Thread.print`) thường hiển thị `RUNNABLE` hoặc "waiting on condition" ở frame `<clinit>`, **không** được báo là deadlock như monitor thường → dễ bỏ sót.
- Biến thể thường gặp: class cha có static field tham chiếu tới class con (`static final Parent DEFAULT = new Child()`), hai thread khởi tạo cha và con cùng lúc.

**Câu hỏi nối tiếp:**
- *Vì sao JVM phải lock khi khởi tạo class?* — Đảm bảo `<clinit>` chạy đúng một lần và thread khác thấy trạng thái đã khởi tạo xong (JLS §12.4.2).

**⚠️ Câu trả lời gây điểm trừ:**
- "Tăng thread pool" hoặc không biết khởi tạo class có lock.

**📖 Ôn lại:** [Phần 9, mục 9.3 — Deadlock khi khởi tạo class](../01-giao-trinh/01-java-core-oop.md#phan-9)

</details>

### Q44. 🔴 Giải thích `protected` khi class con nằm ở package khác. JPMS thay đổi ý nghĩa của `public` thế nào, và bạn gặp gì khi nâng cấp lên Java 17?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Ở package khác, subclass chỉ truy cập member `protected` **qua tham chiếu kiểu chính nó (hoặc con của nó)**, không qua tham chiếu kiểu cha: trong `Dog extends Animal`, `breathe()` và `dog.breathe()` được, `otherAnimal.breathe()` thì lỗi compile. Với JPMS (Java 9), package chỉ dùng được bên ngoài module nếu được `exports` → `public` không còn nghĩa là "mọi nơi". Java 16 (JEP 396) chặn mặc định reflection vào internal của JDK, Java 17 (JEP 403) bỏ hẳn `--illegal-access` → thư viện cũ dùng `setAccessible` vào `java.*` private ném `InaccessibleObjectException`, phải nâng cấp thư viện hoặc thêm `--add-opens`.

**Giải thích chi tiết:**
- Bảng access: `private` (class) < package-private (package) < `protected` (package + subclass) < `public`.
- Package **không phân cấp** về quyền: `com.shop.order` và `com.shop.order.internal` là hai package riêng.
- Thiết kế: chọn mức hẹp nhất (Item 15); class hiện thực package-private, chỉ public interface/factory → dễ refactor.
- Khi migrate lên 17: thư viện hay gặp là Lombok cũ, Mockito/ByteBuddy cũ, một số serializer (Kryo, XStream), Groovy cũ — nâng cấp trước khi nghĩ tới `--add-opens`.

**Câu hỏi nối tiếp:**
- *`--add-opens` khác `--add-exports`?* — `exports` cho truy cập compile/runtime tới type public; `opens` cho phép deep reflection (kể cả private) lúc runtime.

**⚠️ Câu trả lời gây điểm trừ:**
- "`protected` chỉ cho subclass" (quên cả package) hoặc không biết vấn đề reflection khi lên 17.

**📖 Ôn lại:** [Phần 9, mục 9.4 — Access modifiers & packages](../01-giao-trinh/01-java-core-oop.md#phan-9)

</details>

---

<a id="nhom-j"></a>
## J. Enum

### Q45. 🟢 Enum trong Java bên dưới là gì? So sánh enum nên dùng `==` hay `equals`?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Enum là một `final class extends java.lang.Enum<E>`; mỗi hằng là `public static final` instance duy nhất, constructor ngầm định `private`, kèm `values()` và `valueOf(String)`. Enum có field, method, constructor, implement interface, và **constant-specific method** (mỗi hằng một thân method — thực chất là subclass ẩn danh). So sánh nên dùng `==`: an toàn null (không NPE), kiểm tra kiểu lúc compile, và đúng vì mỗi hằng là singleton.

**Giải thích chi tiết:**
- `values()` trả về **mảng mới** mỗi lần → đừng gọi trong hot path; cache vào `private static final` (và trả ra ngoài bằng `List.of`).
- Constructor enum không truy cập được static field của chính enum (hằng được khởi tạo trước) → map tra cứu phải khởi tạo sau các hằng.
- `valueOf` ném `IllegalArgumentException` với tên lạ — bọc lại khi parse input người dùng (trả `Optional`).

**Câu hỏi nối tiếp:**
- *Mở rộng enum được không?* — Không kế thừa được, nhưng có thể cho enum implement interface (`enum BasicOp implements Operation`) và để client cung cấp enum khác cùng interface (Effective Java Item 38).

**⚠️ Câu trả lời gây điểm trừ:**
- "Enum chỉ là tập hằng int".

**📖 Ôn lại:** [Phần 10, mục 10.1 — Enum là class](../01-giao-trinh/01-java-core-oop.md#phan-10)

</details>

### Q46. 🟡 🧩 Vì sao không được lưu `ordinal()` xuống DB? Thêm một hằng mới vào enum dùng trong API giữa các service có rủi ro gì?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** `ordinal()` là vị trí khai báo — chèn hằng vào giữa hay đổi thứ tự là **mọi dữ liệu đã lưu bị lệch nghĩa** mà không có lỗi nào. JPA mặc định `@Enumerated(EnumType.ORDINAL)` — phải đổi sang `STRING` hoặc `AttributeConverter` với **mã ổn định** do bạn định nghĩa (`"N"`, `"P"`...). Với API: consumer cũ nhận giá trị lạ → Jackson ném lỗi deserialize; client có `switch` exhaustive chưa recompile gặp `MatchException`/`IncompatibleClassChangeError` (Java 21).

**Giải thích chi tiết:**
- `EnumType.STRING` cũng có rủi ro khi **đổi tên** hằng → converter với code riêng là an toàn nhất.
- Consumer nên chịu được giá trị lạ: Jackson `READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE` + `@JsonEnumDefaultValue UNKNOWN`, hoặc map sang `String` ở biên.
- Quy trình triển khai an toàn: deploy consumer hiểu giá trị mới trước, producer phát giá trị mới sau (expand → contract).
- Bitmask quyền nên dùng field `bit` riêng thay vì `ordinal()` vì cùng lý do.

**Câu hỏi nối tiếp:**
- *Trong code nội bộ có được dùng `ordinal()` không?* — Gần như chỉ các cấu trúc của JDK (`EnumMap`, `EnumSet`) nên dùng; code nghiệp vụ dùng field tường minh.

**⚠️ Câu trả lời gây điểm trừ:**
- Không biết JPA mặc định là `ORDINAL`.

**📖 Ôn lại:** [Phần 10, mục 10.2 & Góc nhìn Senior](../01-giao-trinh/01-java-core-oop.md#phan-10)

</details>

### Q47. 🟡 `EnumSet` và `EnumMap` được hiện thực thế nào? Dùng enum để mô hình state machine ra sao?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** `EnumSet` là **bit vector**: `RegularEnumSet` dùng một `long` (≤ 64 hằng), `JumboEnumSet` dùng `long[]` → `contains/add/remove` là phép bit O(1), `containsAll/retainAll` là AND/OR theo word. `EnumMap` là **mảng** đánh index theo `ordinal()` → nhanh, gọn hơn `HashMap`, duyệt theo thứ tự khai báo, không cho key `null`. State machine: định nghĩa transition hợp lệ bằng `EnumMap<Status, EnumSet<Status>>` và một method `transitionTo` kiểm tra.

**Giải thích chi tiết:**

```java
private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED = new EnumMap<>(Map.of(
    NEW, EnumSet.of(PAID, CANCELLED),
    PAID, EnumSet.of(SHIPPED, REFUNDED),
    SHIPPED, EnumSet.of(DELIVERED),
    DELIVERED, EnumSet.noneOf(OrderStatus.class),
    CANCELLED, EnumSet.noneOf(OrderStatus.class),
    REFUNDED, EnumSet.noneOf(OrderStatus.class)));

void transitionTo(OrderStatus target) {
    if (!ALLOWED.get(status).contains(target)) throw new InvalidTransitionException(status, target);
    status = target;
}
```

- `EnumSet` thay thế "bit flags" kiểu `int` (Item 36) mà vẫn type-safe.
- `groupingBy(..., () -> new EnumMap<>(Status.class), counting())` cho thống kê theo trạng thái.
- Test bảng (parameterized test) cho mọi cặp trạng thái.

**Câu hỏi nối tiếp:**
- *Khi nào cần thư viện state machine (Spring State Machine)?* — Khi có guard/action phức tạp, trạng thái lồng, cần persist/khôi phục; còn lại enum + map đủ dùng và dễ đọc hơn.

**⚠️ Câu trả lời gây điểm trừ:**
- Dùng chuỗi `if/else` rải rác khắp service cho transition.

**📖 Ôn lại:** [Phần 10, mục 10.3 — EnumSet và EnumMap](../01-giao-trinh/01-java-core-oop.md#phan-10)

</details>

---

<a id="nhom-k"></a>
## K. Exceptions

### Q48. 🟢 Checked và unchecked exception khác nhau thế nào? Khi nào dùng loại nào? Vì sao Spring chọn unchecked?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Checked (`Exception` không thuộc `RuntimeException`) bị **compiler** buộc catch hoặc khai báo `throws` — dành cho lỗi *có thể phục hồi* mà caller *nên* xử lý. Unchecked (`RuntimeException`) dành cho lỗi lập trình/vi phạm precondition hoặc tình huống caller không làm gì hợp lý được. `Error` là lỗi môi trường/JVM, thường không bắt. Spring/Hibernate chọn unchecked (`DataAccessException`) vì checked làm rò rỉ chi tiết hiện thực qua chữ ký method, buộc mọi tầng catch-rethrow vô nghĩa, và không hợp với lambda/stream.

**Giải thích chi tiết:**
- Phân biệt checked chỉ tồn tại ở **compiler**; JVM không biết → Kotlin, Lombok `@SneakyThrows` lợi dụng.
- Exception chuẩn nên dùng: `IllegalArgumentException`, `IllegalStateException`, `NullPointerException` (`Objects.requireNonNull`), `UnsupportedOperationException`.
- Hệ quả quan trọng: `@Transactional` mặc định **chỉ rollback với unchecked và `Error`**; checked exception → **commit** (cần `rollbackFor`).

**Câu hỏi nối tiếp:**
- *Lambda gọi method ném `IOException` thì làm sao?* — Bọc thành `UncheckedIOException`, hoặc functional interface riêng có `throws`.

**⚠️ Câu trả lời gây điểm trừ:**
- "Checked là lỗi lúc compile, unchecked là lỗi lúc runtime".

**📖 Ôn lại:** [Phần 11, mục 11.1 — Hierarchy](../01-giao-trinh/01-java-core-oop.md#phan-11)

</details>

### Q49. 🟡 🔍 Mỗi method trả về gì?

```java
static int a() { int x = 1; try { return x; } finally { x = 2; } }
static StringBuilder b() {
    StringBuilder sb = new StringBuilder("A");
    try { return sb; } finally { sb.append("B"); }
}
static int c() { try { throw new IllegalStateException("boom"); } finally { return 0; } }
static int d() { try { return 1; } finally { return 2; } }
```

<details><summary>Đáp án</summary>

**Trả lời ngắn:** `a()` → `1`; `b()` → `"AB"`; `c()` → `0` và **exception biến mất**; `d()` → `2`.

**Giải thích chi tiết:**
- Giá trị trả về được **lưu lại trước** khi chạy `finally`. Với primitive, sửa biến không ảnh hưởng giá trị đã lưu. Với reference, giá trị đã lưu là reference tới cùng object → `finally` sửa object thì caller thấy thay đổi.
- `return` trong `finally` ghi đè mọi `return`/exception trước đó → **nuốt exception** không dấu vết. Không bao giờ `return`/`throw` trong `finally`.
- `finally` không chạy khi: `System.exit()`, JVM crash/bị kill, vòng lặp vô hạn trong `try`.

**Câu hỏi nối tiếp:**
- *Multi-catch `catch (IOException | SQLException e)` có ràng buộc gì?* — Các kiểu không được có quan hệ cha-con; `e` ngầm định `final`.

**⚠️ Câu trả lời gây điểm trừ:**
- Trả lời `a()` → 2, hoặc cho rằng `c()` vẫn ném exception.

**📖 Ôn lại:** [Phần 11, mục 11.2 — try-catch-finally và các cái bẫy](../01-giao-trinh/01-java-core-oop.md#phan-11)

</details>

### Q50. 🟢 try-with-resources giải quyết vấn đề gì so với try-finally viết tay? Suppressed exception là gì?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** TWR tự đóng resource theo **thứ tự ngược** khai báo, kể cả khi có exception. Nếu thân `try` ném `A` và `close()` ném `B`, `A` được ném ra còn `B` được gắn vào qua `A.addSuppressed(B)` — không mất lỗi gốc. Với try-finally viết tay kiểu Java 6, exception của `close()` trong `finally` **đè mất** exception gốc.

**Giải thích chi tiết:**

```java
try (var in = Files.newBufferedReader(src, UTF_8);
     var out = Files.newBufferedWriter(dst, UTF_8)) {
    in.transferTo(out);
} // đóng out trước, rồi in
```

- Java 9: dùng resource khai báo bên ngoài nếu effectively final: `try (reader) { ... }`.
- `AutoCloseable.close()` `throws Exception`; `Closeable.close()` `throws IOException` và yêu cầu idempotent.
- Lỗi flush cuối thường xuất hiện ở `close()` của writer — bắt rồi bỏ qua là mất lỗi ghi dữ liệu.
- Đọc suppressed: `e.getSuppressed()`; logger chuẩn (Logback/Log4j2) in cả suppressed trong stack trace.

**Câu hỏi nối tiếp:**
- *Resource tạo ra nhưng constructor của resource thứ hai ném exception thì resource thứ nhất có được đóng không?* — Có, TWR đóng mọi resource đã khởi tạo thành công.

**⚠️ Câu trả lời gây điểm trừ:**
- "TWR chỉ là cú pháp gọn hơn" mà không biết suppressed exception.

**📖 Ôn lại:** [Phần 11, mục 11.3 — try-with-resources & suppressed exceptions](../01-giao-trinh/01-java-core-oop.md#phan-11)

</details>

### Q51. 🔴 🧩 Log production đầy dòng `java.lang.NullPointerException` không có message, không có stack trace. Vì sao? Chi phí thật của exception nằm ở đâu?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Đó là tối ưu **`OmitStackTraceInFastThrow`** của HotSpot: khi C2 thấy một *implicit exception* (NPE, `ArithmeticException`, `ArrayIndexOutOfBoundsException`, `ClassCastException`...) bị ném lặp lại nhiều lần tại cùng một chỗ, nó thay bằng một instance dựng sẵn **không có stack trace và message**. Cách xử lý: tìm log **đầu tiên** (còn stack trace) sau lần deploy, hoặc chạy với `-XX:-OmitStackTraceInFastThrow`. Chi phí lớn nhất của exception là **`fillInStackTrace()`** khi tạo (duyệt stack), không phải `throw`.

**Giải thích chi tiết:**
- Exception cho control flow nóng (validate hàng triệu bản ghi bằng exception) tốn CPU đáng kể.
- Exception nghiệp vụ "biết trước" có thể tắt stack trace: constructor `protected Throwable(String msg, Throwable cause, boolean enableSuppression, boolean writableStackTrace)` với `writableStackTrace = false` — đổi lại mất thông tin debug, chỉ dùng cho lỗi business đã có error code rõ.
- Phân biệt log: lỗi business (WARN/INFO, không cần stack trace), lỗi technical (ERROR, có stack trace, kích hoạt alert).

**Câu hỏi nối tiếp:**
- *`catch (Exception e) { log.error(e.getMessage()); }` sai ở đâu?* — Mất stack trace và cause; với NPE fast-throw, message là `null` → log vô dụng. Dùng `log.error("context id={}", id, e)`.

**⚠️ Câu trả lời gây điểm trừ:**
- "Logger cấu hình sai" — không biết tối ưu của JIT.

**📖 Ôn lại:** [Phần 11, mục 11.6 — Chi phí của exception và các vấn đề production](../01-giao-trinh/01-java-core-oop.md#phan-11)

</details>

### Q52. 🟡 Thiết kế exception cho một service (ví dụ thanh toán) như thế nào? Exception translation là gì?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Một exception gốc của domain (unchecked, ví dụ `PaymentException`) mang **error code ổn định**; các nhánh theo bản chất: `ValidationException`, `BusinessRuleException` (không cần stack trace), `ExternalServiceException` (có cờ `retryable`). Tầng web ánh xạ sang HTTP status + Problem Details (RFC 7807/9457) tại **một chỗ** (`@ControllerAdvice`), không lộ stack trace ra client. **Exception translation**: tầng dưới bắt lỗi kỹ thuật (`SQLException`, `IOException`) và ném exception đúng mức trừu tượng của tầng trên, **luôn truyền cause**.

**Giải thích chi tiết:**

```java
try {
    return jdbcLoad(id);
} catch (SQLException e) {
    throw new UserRepositoryException("Không tải được user id=" + id, e); // giữ cause
}
```

- Quy tắc: log **hoặc** ném lại, không làm cả hai (log trùng ở mọi tầng); message có dữ liệu chẩn đoán (id, giá trị) nhưng không chứa secret/PII; failure atomicity — validate trước khi sửa trạng thái.
- Không bắt `Throwable`/`Error` trừ ở tầng ngoài cùng.
- `throw new ServiceException(e.getMessage())` là lỗi phổ biến — mất cause và stack trace gốc.

**Câu hỏi nối tiếp:**
- *Exception "retryable" dùng để làm gì?* — Cho tầng retry (Resilience4j, Spring Retry) quyết định có thử lại không; lỗi validation thì không retry.

**⚠️ Câu trả lời gây điểm trừ:**
- "Bắt `Exception` ở mọi method rồi trả `null`/`false`".

**📖 Ôn lại:** [Phần 11, mục 11.4–11.5](../01-giao-trinh/01-java-core-oop.md#phan-11)

</details>

### Q53. 🟡 🧩 Kể những chỗ mà exception có thể "biến mất im lặng" trong ứng dụng Java.

<details><summary>Đáp án</summary>

**Trả lời ngắn:** (1) `catch` rỗng hoặc chỉ log `getMessage()`; (2) `return`/`throw` trong `finally`; (3) task gửi bằng `ExecutorService.submit()` — exception nằm trong `Future`, không ai gọi `get()` là mất; (4) `CompletableFuture` không có `exceptionally/handle/whenComplete`; (5) nuốt `InterruptedException` mà không khôi phục cờ interrupt; (6) exception khi `close()` trong try-finally viết tay đè exception gốc; (7) `@Transactional` + checked exception → transaction **commit** dù có lỗi.

**Giải thích chi tiết:**
- `execute()` thì exception đi tới `UncaughtExceptionHandler` của thread (mặc định in stderr) — vẫn nên cấu hình handler log đúng chỗ.
- `InterruptedException`: hoặc ném lại, hoặc `Thread.currentThread().interrupt();` để code phía trên biết thread đã bị yêu cầu dừng.
- `@Scheduled` method ném exception: Spring log rồi chạy lần sau — không alert nếu không có metric.
- Phòng: rule static analysis (Sonar "empty catch", "InterruptedException should not be ignored"), metric đếm lỗi theo loại, alert.

**Câu hỏi nối tiếp:**
- *Nếu cố ý bỏ qua exception?* — Đặt tên biến `ignored` và comment lý do.

**⚠️ Câu trả lời gây điểm trừ:**
- Chỉ nêu được catch rỗng.

**📖 Ôn lại:** [Phần 11, mục 11.5–11.6](../01-giao-trinh/01-java-core-oop.md#phan-11)

</details>

---

<a id="nhom-l"></a>
## L. I/O, NIO.2, serialization

### Q54. 🟡 🧩 Service chạy vài ngày thì lỗi `Too many open files`. Bạn nghi ngờ gì trong code I/O? Kể thêm các nguyên tắc đọc/ghi file đúng chuẩn.

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Nghi ngờ **rò rỉ file descriptor**: stream/reader/connection không đóng trong nhánh lỗi; đặc biệt `Files.lines`, `Files.walk`, `Files.list` trả về `Stream` **giữ file handle — phải đóng** (try-with-resources), khác với stream trên collection. Kiểm tra bằng `lsof -p <pid>` hoặc `ls /proc/<pid>/fd | wc -l` để thấy loại handle đang tăng. Nguyên tắc: luôn try-with-resources, luôn chỉ định charset, đọc file lớn theo dòng (không `readAllLines`), ghi file quan trọng bằng **file tạm + `ATOMIC_MOVE`**, chống **path traversal**.

**Giải thích chi tiết:**
- `java.io` theo Decorator pattern: `new BufferedReader(new InputStreamReader(in, UTF_8))`. Byte stream (`InputStream`) vs character stream (`Reader`) — chuyển đổi luôn cần charset.
- Không buffer → mỗi `read()` là một system call; buffer 8 KB giảm hàng nghìn lần.
- `InputStream.read(byte[])` không đảm bảo đọc đủ → dùng `readNBytes`/`readAllBytes`.
- Path traversal:

```java
Path base = Path.of("/srv/uploads").toAbsolutePath().normalize();
Path target = base.resolve(userFileName).normalize();
if (!target.startsWith(base)) throw new SecurityException("Path traversal");
```

- Copy file lớn: `Files.copy`/`FileChannel.transferTo` dùng zero-copy (`sendfile`) — nhanh nhất.

**Câu hỏi nối tiếp:**
- *Vì sao ghi tạm rồi move?* — Người đọc không bao giờ thấy file ghi dở; cần thêm `FileChannel.force(true)` nếu yêu cầu durability khi mất điện.

**⚠️ Câu trả lời gây điểm trừ:**
- "Tăng `ulimit -n`" như giải pháp chính.

**📖 Ôn lại:** [Phần 12, mục 12.1–12.2](../01-giao-trinh/01-java-core-oop.md#phan-12)

</details>

### Q55. 🔴 Vì sao Java deserialization (`ObjectInputStream.readObject`) bị coi là nguy hiểm? Phòng thủ thế nào? `serialVersionUID` để làm gì?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Deserialize dữ liệu không tin cậy có thể dẫn tới **Remote Code Execution** qua *gadget chain* — chuỗi class hợp lệ trong classpath (Commons Collections, Spring, Groovy...) mà `readObject`/`hashCode`/`compareTo` được gọi trong lúc deserialize ghép lại thành lời gọi tùy ý (công cụ ysoserial, các CVE WebLogic/JBoss/Jenkins từ 2015). Cũng có thể DoS (HashSet lồng nhau). Phòng thủ: **không deserialize dữ liệu không tin cậy**; dùng JSON/Protobuf/Avro có schema; nếu bắt buộc thì **`ObjectInputFilter`** (Java 9, JEP 290; filter theo context Java 17, JEP 415) với allow-list và giới hạn depth/refs/bytes.

**Giải thích chi tiết:**

```java
ObjectInputFilter filter = ObjectInputFilter.Config.createFilter(
        "com.myapp.dto.*;java.base/*;!*;maxdepth=10;maxrefs=10000;maxbytes=1048576");
ois.setObjectInputFilter(filter);
```

- Deserialization **không gọi constructor** của class serializable → bỏ qua validate, phá invariant. Vá bằng `readObject` validate (`InvalidObjectException`) hoặc dùng **record** (deserialize qua canonical constructor).
- `serialVersionUID`: nếu không khai báo, JVM tính từ cấu trúc class → thêm một method cũng đổi UID → `InvalidClassException` khi đọc dữ liệu cũ. Luôn khai báo tường minh (`@Serial`, Java 14).
- Serialization còn ẩn trong: session replication Tomcat, cache phân tán dùng JDK serializer (một số cấu hình Redis/Hazelcast), RMI, JMX → kiểm tra khi review hệ thống.

**Câu hỏi nối tiếp:**
- *`transient` dùng khi nào?* — Field không cần/không được serialize (token, cache, dữ liệu dẫn xuất).
- *Singleton serializable giữ tính duy nhất thế nào?* — `readResolve()` trả về instance hiện có, hoặc dùng enum.

**⚠️ Câu trả lời gây điểm trừ:**
- "Chỉ cần implement `Serializable` là xong", không biết gadget chain.

**📖 Ôn lại:** [Phần 12, mục 12.3 — Serialization và rủi ro](../01-giao-trinh/01-java-core-oop.md#phan-12)

</details>

---

<a id="nhom-m"></a>
## M. Reflection & annotations

### Q56. 🔴 Reflection được dùng ở đâu, tốn kém thế nào? Dynamic proxy hoạt động ra sao và vì sao `@Transactional` "không chạy" khi gọi `this.method()`?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Reflection là nền tảng của Spring DI, Hibernate, Jackson, JUnit, Mockito. Chi phí: tra cứu `getDeclaredMethod` tốn kém (phải **cache** `Method`/`Field`), `Method.invoke` chậm hơn gọi trực tiếp (kiểm tra access, boxing, mảng varargs); từ Java 18 (JEP 416) reflection được hiện thực lại trên `MethodHandle`. **Dynamic proxy** (`Proxy.newProxyInstance`) tạo object implement interface lúc runtime, chuyển mọi lời gọi qua `InvocationHandler`; với class không interface, Spring dùng **CGLIB/ByteBuddy** tạo subclass. Gọi `this.method()` bên trong bean là gọi trên object thật, **không đi qua proxy** → advice (transaction, cache, async) bị bỏ qua.

**Giải thích chi tiết:**

```java
(proxy, method, args) -> {
    try { return method.invoke(target, args); }
    catch (InvocationTargetException e) { throw e.getCause(); } // ném lỗi thật, không phải wrapper
}
```

- Method `final`/`private` không bị proxy CGLIB chặn được.
- Sửa self-invocation: tách sang bean khác (sạch nhất), inject self qua `@Lazy`/`ObjectProvider`, hoặc AspectJ weaving.
- `Class.forName(name)` mặc định **khởi tạo class**; `User.class` thì không.
- Reflection phá đóng gói, khó refactor (đổi tên không báo lỗi compile), bị JPMS hạn chế, cần metadata cho GraalVM Native Image → Spring Boot 3 đầu tư AOT.

**Câu hỏi nối tiếp:**
- *Vì sao Micronaut/Quarkus khởi động nhanh hơn Spring truyền thống?* — DI lúc compile (annotation processing) thay vì classpath scanning + reflection lúc runtime.

**⚠️ Câu trả lời gây điểm trừ:**
- Không giải thích được vì sao self-invocation bỏ qua proxy.

**📖 Ôn lại:** [Phần 13, mục 13.1 — Reflection & dynamic proxy](../01-giao-trinh/01-java-core-oop.md#phan-13)

</details>

### Q57. 🟡 🧩 Bạn viết annotation `@Audit` và đọc bằng `method.getAnnotation(Audit.class)` nhưng luôn nhận `null`. Vì sao? Phân biệt annotation processor lúc compile và đọc annotation lúc runtime.

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Gần như chắc chắn thiếu `@Retention(RetentionPolicy.RUNTIME)`. Mặc định là `CLASS`: annotation có trong `.class` nhưng **không đọc được bằng reflection**. `SOURCE` bị bỏ sau compile (`@Override`, Lombok). Annotation tự nó không làm gì — cần bộ xử lý: **annotation processor** (JSR 269) chạy lúc compile và sinh code (Lombok, MapStruct, Dagger) — không tốn chi phí runtime; hoặc **reflection lúc runtime** (Spring, JPA, Bean Validation) — linh hoạt nhưng tốn startup.

**Giải thích chi tiết:**
- Các nguyên nhân khác khiến trả `null`: annotation đặt trên method interface nhưng đọc trên method class hiện thực (annotation method không kế thừa); đọc trên proxy CGLIB thay vì class gốc (dùng `AopUtils.getTargetClass`, `AnnotatedElementUtils`); `@Target` không khớp.
- Thuộc tính annotation chỉ được là primitive, `String`, `Class`, enum, annotation, mảng của chúng — giá trị phải là hằng lúc compile.
- `@Inherited` chỉ áp dụng cho annotation trên **class** (không interface, không method); `@Repeatable` (Java 8) cho phép gắn nhiều lần.
- Composed annotation (`@RestController = @Controller + @ResponseBody`) là tính năng của Spring (`MergedAnnotations`), Java thuần không tự hợp nhất meta-annotation.

**Câu hỏi nối tiếp:**
- *Validator dùng annotation nên cache metadata thế nào?* — `ClassValue<List<FieldRule>>` hoặc `ConcurrentHashMap<Class<?>, ...>`; lần gọi đầu tốn reflection, các lần sau chỉ đọc cache.

**⚠️ Câu trả lời gây điểm trừ:**
- "Annotation tự động thêm hành vi vào method".

**📖 Ôn lại:** [Phần 13, mục 13.2 — Annotations](../01-giao-trinh/01-java-core-oop.md#phan-13)

</details>

---

> ✅ **Tự đánh giá sau khi luyện:** trả lời trôi chảy ≥ 90% câu 🟢, ≥ 75% câu 🟡 và nói được cơ chế + ít nhất một trade-off cho mỗi câu 🔴. Đối chiếu thêm với [Checklist tự đánh giá của Module 01](../01-giao-trinh/01-java-core-oop.md#checklist-tu-danh-gia).

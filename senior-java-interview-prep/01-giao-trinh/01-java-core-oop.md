# Module 01 — Java Core & OOP

> **Mục tiêu:** sau module này bạn giải thích được Java code đi từ file `.java` tới mã máy như thế nào (javac → bytecode → class loading → interpreter/JIT), nắm chắc mô hình bộ nhớ của primitive/reference, String, wrapper; thiết kế được class đúng chuẩn OOP (equals/hashCode, immutability, enum, exception hierarchy); và debug được các lỗi "kinh điển" hay gặp trên production như `NullPointerException` khi unboxing, so sánh `Integer` bằng `==`, rò rỉ bộ nhớ qua inner class, nuốt exception trong `finally`, lỗ hổng deserialization.
> **Cấp độ:** Cơ bản → Nâng cao (Senior)
> **Thời lượng gợi ý:** 7 ngày (≈ 35–40 giờ, gồm cả bài tập và dự án mini)
> **Yêu cầu trước:** Biết cú pháp Java cơ bản (biến, vòng lặp, method, class), đã cài JDK 17 hoặc 21 và một IDE (IntelliJ IDEA / VS Code).
> **Nguồn tham khảo:**
> - Trong kho: [`Java/OCP-Oracle-Certified-Professional-Java-SE-17-Developer-Study-Guide-Exam-1Z0-829pdf.pdf`](../../Java/OCP-Oracle-Certified-Professional-Java-SE-17-Developer-Study-Guide-Exam-1Z0-829pdf.pdf) — Chương 1 (Building Blocks), 2 (Operators), 4 (Core APIs), 5 (Methods), 6 (Class Design), 7 (Beyond Classes: interface, enum, sealed, record, nested class), 11 (Exceptions), 14 (I/O)
> - Trong kho: [`Java/OCP Oracle Certified Professional Java SE 21.pdf`](../../Java/OCP%20Oracle%20Certified%20Professional%20Java%20SE%2021.pdf) — cập nhật cho Java 21
> - Trong kho: [`Java/ocp-oracle-certified-professional-java-se-11-programmer-i-study-guide.pdf`](../../Java/ocp-oracle-certified-professional-java-se-11-programmer-i-study-guide.pdf)
> - Trong kho: [`Java/OCA_Oracle_Certified_Associate_Java_SE_8.pdf`](../../Java/OCA_Oracle_Certified_Associate_Java_SE_8.pdf) — nền tảng Java 8 (nhiều công ty vẫn chạy Java 8)
> - Trong kho: [`Ebook IT/OCP_ Oracle Certified Professional Java SE 8 Programmer II Study Guide_ Exam 1Z0-809.pdf`](../../Ebook%20IT/OCP_%20Oracle%20Certified%20Professional%20Java%20SE%208%20Programmer%20II%20Study%20Guide_%20Exam%201Z0-809.pdf) — Advanced Class Design, Exceptions, I/O, NIO.2
> - Trong kho: [`Java/Head First Java 2nd edition.pdf`](../../Java/Head%20First%20Java%202nd%20edition.pdf), [`Java/thinkjava.pdf`](../../Java/thinkjava.pdf) — đọc nhẹ nhàng để ôn khái niệm OOP
> - Trong kho: [`Ebook IT/Clean Code.pdf`](../../Ebook%20IT/Clean%20Code.pdf) — chương về Error Handling, Objects and Data Structures
> - Trong kho: [`Java/OCP_ Oracle Certified Professional Java SE 11  Exam 1Z0-819 Practice Test.pdf`](../../Java/OCP_%20Oracle%20Certified%20Professional%20Java%20SE%2011%20%20Exam%201Z0-819%20Practice%20Test.pdf) — luyện câu hỏi bẫy
> - Ngoài: *Effective Java 3rd ed.* (Joshua Bloch) — Item 1–3 (static factory, builder, singleton), Item 10–13 (equals, hashCode, toString, clone), Item 17 (minimize mutability), Item 34–37 (enum, EnumSet, EnumMap), Item 69–77 (exceptions), Item 85–90 (serialization)
> - Ngoài: The Java Language Specification (JLS) — https://docs.oracle.com/javase/specs/ (§5 Conversions, §8.4.8 Overriding, §12.4 Initialization of Classes, §15.12 Method Invocation)
> - Ngoài: The Java Virtual Machine Specification (JVMS) — §5 Loading, Linking, and Initializing
> - Ngoài: OpenJDK JEPs — https://openjdk.org/jeps/ (JEP 254 Compact Strings, 280 Indify String Concatenation, 290 Serialization Filtering, 330 Launch Single-File Source, 358 Helpful NPE, 378 Text Blocks, 395 Records, 400 UTF-8 by Default, 409 Sealed Classes, 421 Deprecate Finalization)
> - Ngoài: Java API docs — https://docs.oracle.com/en/java/javase/21/docs/api/

## Mục lục
1. [JDK, JRE, JVM và pipeline biên dịch – chạy](#phan-1)
2. [Kiểu dữ liệu: primitive, reference, wrapper, pass-by-value](#phan-2)
3. [String: immutability, String pool, StringBuilder, compact strings, text blocks](#phan-3)
4. [Toán tử & ép kiểu: những cái bẫy](#phan-4)
5. [OOP: 4 trụ cột, overloading vs overriding, static/dynamic binding](#phan-5)
6. [Abstract class vs interface; nested/inner/anonymous/local class](#phan-6)
7. [Các method của `Object`: equals/hashCode, toString, clone, finalize](#phan-7)
8. [Immutability & defensive copy](#phan-8)
9. [`static`, `final`, thứ tự khởi tạo, access modifiers & packages](#phan-9)
10. [Enum nâng cao](#phan-10)
11. [Exceptions](#phan-11)
12. [I/O & NIO.2, serialization](#phan-12)
13. [Reflection & Annotations](#phan-13)
14. [Dự án mini của module](#du-an-mini)
15. [Checklist tự đánh giá](#checklist-tu-danh-gia)

---

<a id="phan-1"></a>
## 1. JDK, JRE, JVM và pipeline biên dịch – chạy

### 1.1 Khái niệm

| Thành phần | Là gì | Gồm những gì |
|---|---|---|
| **JVM** (Java Virtual Machine) | Máy ảo thực thi bytecode. Là *đặc tả* (JVMS); HotSpot, OpenJ9, GraalVM là các *hiện thực*. | Class loader subsystem, runtime data areas (heap, stack, metaspace…), execution engine (interpreter + JIT), Garbage Collector |
| **JRE** (Java Runtime Environment) | Môi trường *chạy* Java | JVM + thư viện chuẩn (`java.base`, `java.sql`…) |
| **JDK** (Java Development Kit) | Bộ công cụ *phát triển* | JRE + `javac`, `jar`, `javadoc`, `jdb`, `jshell`, `jlink`, `jcmd`, `jfr`, `jpackage`… |

Từ **Java 11**, Oracle/OpenJDK không còn phát hành JRE riêng lẻ. Thay vào đó bạn dùng `jlink` để tạo một runtime tối giản chỉ chứa các module ứng dụng cần (rất hữu ích để giảm kích thước Docker image).

"Write once, run anywhere": code Java biên dịch thành **bytecode** (độc lập nền tảng), JVM của từng hệ điều hành chịu trách nhiệm chạy bytecode đó.

```java
// Hello.java
public class Hello {
    public static void main(String[] args) {
        System.out.println("Xin chào, " + (args.length > 0 ? args[0] : "Java"));
    }
}
```

```bash
javac Hello.java          # tạo Hello.class (bytecode)
java Hello Senior         # JVM nạp class Hello và gọi main
java Hello.java Senior    # Java 11+ (JEP 330): chạy thẳng file nguồn, biên dịch trong bộ nhớ
javap -c -v Hello.class   # xem bytecode, constant pool
```

### 1.2 Bên dưới nắp capo: pipeline đầy đủ

```
 Hello.java ──javac──► Hello.class (bytecode + constant pool)
                                │
                        ┌───────▼────────┐
                        │  Class Loading │  Bootstrap → Platform → Application (parent delegation)
                        └───────┬────────┘
                        ┌───────▼────────┐
                        │    Linking     │  Verify → Prepare (gán default value cho static) → Resolve (symbolic → direct ref)
                        └───────┬────────┘
                        ┌───────▼────────┐
                        │ Initialization │  chạy <clinit>: static initializer + gán giá trị static field
                        └───────┬────────┘
                        ┌───────▼────────────────────────────────────┐
                        │ Execution engine: Interpreter → C1 → C2    │  (Tiered Compilation)
                        └────────────────────────────────────────────┘
```

**1) Class loader và mô hình ủy quyền cha (parent delegation)**

- **Bootstrap class loader** (viết bằng C++, trong JVM): nạp `java.base` (`java.lang.*`, `java.util.*`...).
- **Platform class loader** (Java 9+; trước đó là *Extension class loader* đọc `jre/lib/ext`): nạp các module nền tảng khác.
- **Application (System) class loader**: nạp class từ classpath/module path của bạn.

Khi được yêu cầu nạp một class, loader **hỏi cha trước**; chỉ khi cha không tìm thấy mới tự nạp. Nhờ vậy không ai có thể "đánh tráo" `java.lang.String` bằng class cùng tên trong classpath.

Một class trong JVM được định danh bởi cặp **(tên đầy đủ, class loader đã định nghĩa nó)**. Hai class cùng tên nhưng khác loader là **hai kiểu khác nhau** → đây là nguồn gốc của lỗi kinh điển `ClassCastException: com.x.Foo cannot be cast to com.x.Foo` trong app server, hot-reload (Spring DevTools), hoặc hệ thống plugin.

**2) Linking**
- *Verification*: kiểm tra bytecode hợp lệ (không nhảy lung tung, stack không tràn, kiểu đúng) → nền tảng của tính an toàn.
- *Preparation*: cấp phát bộ nhớ cho static field và gán **giá trị mặc định** (0, `null`, `false`) — chưa phải giá trị bạn viết.
- *Resolution*: chuyển symbolic reference trong constant pool thành tham chiếu trực tiếp (có thể lazy).

**3) Initialization**: chạy method `<clinit>` (gộp các static initializer và phép gán static field theo thứ tự xuất hiện trong mã nguồn). JVM đảm bảo `<clinit>` chạy **đúng một lần, thread-safe** (có lock) — đây là cơ sở của *Initialization-on-demand holder idiom* cho singleton lazy (xem Phần 9).

**4) Execution engine & JIT**
- Ban đầu bytecode được **interpret** (chậm nhưng khởi động nhanh).
- HotSpot đếm số lần gọi method / số vòng lặp. Method "nóng" được biên dịch bởi **C1** (nhanh, tối ưu nhẹ, có profiling), rồi nếu vẫn nóng thì **C2** (tối ưu mạnh: inlining, escape analysis, loop unrolling, lock elision...). Đây là **Tiered Compilation** (mặc định từ Java 8).
- JIT tối ưu *dựa trên giả định* từ profile (ví dụ: call site này chỉ thấy 1 kiểu → inline). Khi giả định sai (nạp class mới), JVM **deoptimize** quay về interpreter.

> 💡 **Góc nhìn Senior:**
> - **Warm-up**: service Java vừa deploy thường có p99 latency cao trong vài phút đầu vì code còn đang interpret/C1. Giải pháp: warm-up traffic trước khi nhận tải thật, readiness probe chặt hơn, hoặc dùng **CDS/AppCDS** (Class Data Sharing) giảm thời gian load class, **GraalVM Native Image** (AOT) cho startup cực nhanh nhưng mất đi tối ưu dựa trên profile, hoặc **CRaC** (Coordinated Restore at Checkpoint).
> - Microbenchmark viết tay bằng `System.nanoTime()` gần như luôn sai do JIT (dead-code elimination, OSR, warm-up). Dùng **JMH**.
> - Trong container, Java 10+ đã *container-aware* (`-XX:+UseContainerSupport` mặc định): đọc giới hạn CPU/RAM của cgroup. Java 8 chỉ có từ 8u191. Hãy cấu hình heap bằng `-XX:MaxRAMPercentage` thay vì hard-code `-Xmx` không khớp với memory limit của Pod.

> ⚠️ **Lỗi thường gặp:**
> - Nhầm `ClassNotFoundException` (checked, xảy ra khi load động bằng `Class.forName`/`loadClass` mà không tìm thấy) với `NoClassDefFoundError` (Error, class *có lúc compile* nhưng lúc runtime không có, **hoặc** static initializer của nó đã ném exception trước đó → mọi lần dùng sau đều `NoClassDefFoundError: Could not initialize class X`).
> - `UnsupportedClassVersionError`: compile bằng JDK 21 (class file major version 65) nhưng chạy trên JRE 17 (61). Dùng `javac --release 17`.
> - Tưởng rằng "Java chậm vì interpret" — với code nóng, C2 sinh mã máy rất tốt, nhiều khi ngang C++.

### 🛠 Bài tập phần 1

**Bài 1.1 — Soi bytecode (Cơ bản)**
- Đề bài: Viết class có method `int sum(int a, int b)` và `String greet(String n)` dùng `"Hi " + n`. Biên dịch rồi chạy `javap -c -v`.
- Tiêu chí đạt: chỉ ra được các instruction `iload`, `iadd`, `ireturn`; với Java 9+ thấy `invokedynamic` + `makeConcatWithConstants` cho phép nối chuỗi; giải thích được *constant pool* là gì.

**Bài 1.2 — Class loader tự viết (Trung bình)**
- Đề bài: Viết `MyClassLoader extends ClassLoader` nạp một file `.class` từ thư mục tùy ý (override `findClass`, dùng `defineClass`). Nạp cùng một class bằng 2 instance loader khác nhau.
- Tiêu chí đạt: in ra `c1 == c2` là `false`; cast object từ loader này sang kiểu của loader kia gây `ClassCastException`; giải thích vì sao.

**Bài 1.3 — Quan sát JIT (Nâng cao)**
- Đề bài: Viết vòng lặp gọi một method nhỏ 1 triệu lần, chạy với `-XX:+PrintCompilation`. Sau đó chạy lại với `-Xint` và so sánh thời gian.
- Tiêu chí đạt: chỉ ra được dòng log method của bạn được biên dịch ở tier 3 rồi tier 4; giải thích ký hiệu `%` (OSR — On-Stack Replacement) và `made not entrant`.

<details>
<summary>Gợi ý lời giải</summary>

Bài 1.2 — đoạn code then chốt:

```java
public class MyClassLoader extends ClassLoader {
    private final Path dir;
    public MyClassLoader(Path dir) { super(null); this.dir = dir; } // parent = bootstrap để KHÔNG delegate về app loader
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            byte[] bytes = Files.readAllBytes(dir.resolve(name.replace('.', '/') + ".class"));
            return defineClass(name, bytes, 0, bytes.length);
        } catch (IOException e) {
            throw new ClassNotFoundException(name, e);
        }
    }
}
// Class<?> c1 = new MyClassLoader(dir).loadClass("demo.Foo");
// Class<?> c2 = new MyClassLoader(dir).loadClass("demo.Foo");
// System.out.println(c1 == c2); // false
```

Lưu ý: nếu để parent là application loader và `demo.Foo` nằm trong classpath, parent delegation sẽ trả về cùng một class → `true`. Đó chính là điểm cần hiểu.

Bài 1.3: dòng log dạng `123  45 %  4  Demo::loop @ 5 (30 bytes)` — `%` là OSR, số `4` là tier C2. `made not entrant` nghĩa là bản biên dịch cũ bị vô hiệu (deopt hoặc thay bằng bản tốt hơn). `-Xint` chậm hơn hàng chục lần.

</details>

---

<a id="phan-2"></a>
## 2. Kiểu dữ liệu: primitive, reference, wrapper, pass-by-value

### 2.1 Primitive vs reference

| Primitive | Kích thước | Giá trị mặc định (field) | Wrapper |
|---|---|---|---|
| `byte` | 8 bit | 0 | `Byte` |
| `short` | 16 bit | 0 | `Short` |
| `int` | 32 bit | 0 | `Integer` |
| `long` | 64 bit | 0L | `Long` |
| `float` | 32 bit IEEE 754 | 0.0f | `Float` |
| `double` | 64 bit IEEE 754 | 0.0d | `Double` |
| `char` | 16 bit unsigned (UTF-16 code unit) | `'\u0000'` | `Character` |
| `boolean` | JVM không quy định cụ thể (thường 1 byte trong mảng, 4 byte trên stack) | `false` | `Boolean` |

- **Primitive**: biến chứa trực tiếp giá trị. Biến local nằm trên **stack frame** (hoặc thanh ghi sau JIT); field của object thì nằm *trong object* trên heap.
- **Reference**: biến chứa *tham chiếu* (thường là địa chỉ, có thể nén — compressed oops) tới object trên **heap**.
- **Biến local không có giá trị mặc định** — compiler bắt buộc gán trước khi đọc (definite assignment). Field và phần tử mảng thì có.

### 2.2 Wrapper, autoboxing và Integer cache

Autoboxing (Java 5): compiler tự chèn `Integer.valueOf(i)` khi cần object và `x.intValue()` khi cần primitive.

```java
Integer a = 127, b = 127;
System.out.println(a == b);        // true  — cùng object trong cache
Integer c = 128, d = 128;
System.out.println(c == d);        // false — 2 object khác nhau
System.out.println(c.equals(d));   // true  — luôn dùng equals để so sánh giá trị

Long l = 127L;
System.out.println(l.equals(127)); // false! 127 được box thành Integer, Long.equals kiểm tra instanceof Long
```

**Integer cache**: `Integer.valueOf(int)` trả về object cache sẵn cho dải **-128..127** (JLS §5.1.7 bắt buộc dải này). Cận trên có thể nâng bằng `-XX:AutoBoxCacheMax=<n>` (hoặc system property `java.lang.Integer.IntegerCache.high`). `Byte`, `Short`, `Long` cache -128..127; `Character` cache 0..127; `Boolean` có `TRUE`/`FALSE`; **`Float`, `Double` không cache**.

`new Integer(5)` luôn tạo object mới — constructor này **deprecated từ Java 9** và **đánh dấu forRemoval từ Java 16** (chuẩn bị cho Project Valhalla, nơi wrapper có thể trở thành value class).

**Cái bẫy NPE khi unboxing:**

```java
Map<String, Integer> stock = new HashMap<>();
int qty = stock.get("SKU-1");            // NPE: null.intValue()

Integer discount = null;
int price = true ? discount : 0;         // NPE! Kiểu của biểu thức ternary là int (binary numeric promotion)
                                          // → discount bị unbox

boolean flag = someBooleanObject;        // NPE nếu null
```

**Chi phí hiệu năng của boxing:**

```java
Long sum = 0L;                       // ❌ Long, không phải long
for (long i = 0; i < 1_000_000_000L; i++) {
    sum += i;                        // mỗi vòng: unbox, cộng, box → hàng trăm triệu object rác
}
```

Đổi `Long` thành `long` có thể nhanh hơn nhiều lần (Effective Java Item 6). Escape analysis của C2 *đôi khi* loại bỏ được allocation, nhưng đừng dựa vào nó.

### 2.3 Pass-by-value — Java **luôn luôn** truyền tham trị

Java truyền **bản sao giá trị của biến**. Với primitive, đó là bản sao con số. Với reference, đó là **bản sao của tham chiếu** (hai biến cùng trỏ tới một object). Vì vậy:
- Method **có thể thay đổi trạng thái** object được truyền vào (qua tham chiếu bản sao).
- Method **không thể** làm biến của caller trỏ sang object khác.

```java
public class PassByValueDemo {
    static void reassign(StringBuilder sb) { sb = new StringBuilder("mới"); }   // chỉ đổi bản sao
    static void mutate(StringBuilder sb)   { sb.append(" + đã sửa"); }          // sửa object chung
    static void swap(Integer x, Integer y) { Integer t = x; x = y; y = t; }      // vô tác dụng với caller

    public static void main(String[] args) {
        StringBuilder sb = new StringBuilder("gốc");
        reassign(sb);
        System.out.println(sb);   // gốc
        mutate(sb);
        System.out.println(sb);   // gốc + đã sửa

        Integer a = 1, b = 2;
        swap(a, b);
        System.out.println(a + " " + b); // 1 2
    }
}
```

> 💡 **Góc nhìn Senior:**
> - Câu trả lời phỏng vấn chuẩn: *"Java is strictly pass-by-value; for objects, the value passed is the reference."* Đừng nói "object pass-by-reference" — interviewer sẽ bắt bẻ bằng ví dụ `swap`.
> - Memory: một `Integer` trên HotSpot 64-bit với compressed oops chiếm **16 byte** (12 byte header + 4 byte value), so với 4 byte của `int`. `List<Integer>` 1 triệu phần tử tốn ~20 MB, `int[]` chỉ ~4 MB. Đây là lý do có các thư viện primitive collections (fastutil, Eclipse Collections) — xem Module 02.
> - Không bao giờ dùng wrapper làm *khóa đồng bộ* (`synchronized (Integer)`) — do cache, các phần code không liên quan có thể vô tình dùng chung lock. Java 16+ còn cảnh báo `-XX:DiagnoseSyncOnValueBasedClasses`.

> ⚠️ **Lỗi thường gặp:**
> - So sánh `Integer`/`Long` bằng `==` — test với số nhỏ thì pass, production với ID lớn thì fail. Bug kinh điển khi so sánh ID entity kiểu `Long`.
> - Dùng `list.remove(1)` trên `List<Integer>` với ý định xóa giá trị 1 — thực ra là xóa phần tử ở **index 1** (overload `remove(int)` được ưu tiên hơn `remove(Object)` vì không cần boxing). Dùng `list.remove(Integer.valueOf(1))`.
> - Trường JPA/DTO kiểu `int` cho cột nullable → mất thông tin "không có giá trị"; ngược lại dùng `Integer` thì phải xử lý null cẩn thận.

### 🛠 Bài tập phần 2

**Bài 2.1 — Dự đoán output (Cơ bản)**
- Đề bài: Không chạy code, dự đoán output rồi kiểm chứng:
  ```java
  Integer x1 = 100, x2 = 100, y1 = 1000, y2 = 1000;
  System.out.println(x1 == x2);
  System.out.println(y1 == y2);
  System.out.println(y1 <= y2 && y1 >= y2);
  System.out.println(new Integer(5) == Integer.valueOf(5));
  Long l = 10L; System.out.println(l.equals(10));
  ```
- Tiêu chí đạt: đúng cả 5 dòng và giải thích từng dòng (gợi ý dòng 3: toán tử `<=` buộc unbox).

**Bài 2.2 — Đo chi phí autoboxing (Trung bình)**
- Đề bài: Viết 2 phiên bản tính tổng 0..500 triệu: dùng `long` và dùng `Long`. Đo thời gian (chạy mỗi phiên bản vài lần để warm-up) và quan sát GC log với `-Xlog:gc`.
- Tiêu chí đạt: ghi lại được chênh lệch thời gian và số lần GC; giải thích nguyên nhân.

**Bài 2.3 — Truy vết bug NPE ẩn (Nâng cao)**
- Đề bài: Cho method `int resolveLimit(Map<String,Integer> cfg, boolean premium) { return premium ? cfg.get("premium") : 10; }`. Viết unit test chứng minh NPE khi key thiếu, sửa method mà vẫn giữ kiểu trả về `int`, và chạy với Java 14+ để xem *Helpful NullPointerException message* (JEP 358).
- Tiêu chí đạt: test đỏ trước → xanh sau; giải thích quy tắc kiểu của toán tử điều kiện (JLS §15.25).

<details>
<summary>Gợi ý lời giải</summary>

Bài 2.1: `true`, `false`, `true`, `false`, `false`.

Bài 2.3: kiểu của `premium ? Integer : int` là `int` → `cfg.get(...)` bị unbox. Sửa:

```java
int resolveLimit(Map<String, Integer> cfg, boolean premium) {
    return premium ? cfg.getOrDefault("premium", 10) : 10;
}
```

Message JEP 358 dạng: `Cannot invoke "java.lang.Integer.intValue()" because the return value of "java.util.Map.get(Object)" is null`.

</details>

---

<a id="phan-3"></a>
## 3. String: immutability, String pool, StringBuilder, compact strings, text blocks

### 3.1 Vì sao String immutable?

`String` là `final class`, field lưu dữ liệu là `private final`, không có method nào sửa nội dung. Lợi ích:
1. **An toàn bảo mật**: tên file, URL, tên class truyền vào `Class.forName`, thông tin kết nối DB không thể bị sửa sau khi đã kiểm tra.
2. **Thread-safe** miễn phí, chia sẻ thoải mái giữa các thread.
3. **Cache hashCode**: hash tính một lần, lưu lại → `String` là key lý tưởng cho `HashMap`.
4. **String pool** khả thi: nhiều biến cùng trỏ tới một literal mà không sợ ai sửa.

### 3.2 String pool và `intern()`

```java
String s1 = "java";                 // literal → lấy từ String pool
String s2 = "java";                 // cùng object với s1
String s3 = new String("java");     // object MỚI trên heap (literal "java" vẫn ở pool)
String s4 = s3.intern();            // trả về instance trong pool
String s5 = "ja" + "va";            // compile-time constant → compiler gộp thành "java"
String part = "ja";
String s6 = part + "va";            // runtime concat → object mới

System.out.println(s1 == s2); // true
System.out.println(s1 == s3); // false
System.out.println(s1 == s4); // true
System.out.println(s1 == s5); // true
System.out.println(s1 == s6); // false
final String fpart = "ja";
System.out.println(s1 == (fpart + "va")); // true — fpart là constant variable
```

- Từ **Java 7**, String pool (StringTable) nằm trên **heap** (trước đó nằm ở PermGen → dễ `OutOfMemoryError: PermGen space` khi lạm dụng `intern()`).
- StringTable là một hash table kích thước cố định (`-XX:StringTableSize`), các string trong pool có thể được GC thu hồi khi không còn tham chiếu.
- G1 có **String Deduplication** (`-XX:+UseStringDeduplication`, JEP 192): gộp các mảng byte trùng nội dung của các `String` khác nhau — khác với `intern()` ở chỗ không gộp object `String`, chỉ gộp mảng bên dưới.

### 3.3 Compact Strings (Java 9, JEP 254)

- Java 8: `String` chứa `char[] value` → mỗi ký tự 2 byte.
- Java 9+: `byte[] value` + `byte coder` (`LATIN1` = 0 hoặc `UTF16` = 1). Chuỗi chỉ gồm ký tự Latin-1 dùng 1 byte/ký tự → tiết kiệm ~50% bộ nhớ cho chuỗi ASCII (thường chiếm phần lớn heap của ứng dụng web).
- Chuỗi tiếng Việt có dấu (`"Việt"`) chứa ký tự ngoài Latin-1 (`ệ` = U+1EC7) → cả chuỗi dùng UTF16.
- `length()` trả về số **UTF-16 code unit**, không phải số ký tự hiển thị. Emoji `"😀".length() == 2` (surrogate pair). Dùng `codePointCount` hoặc `codePoints()` khi cần đếm đúng.

### 3.4 Nối chuỗi: `+`, `StringBuilder`, `StringBuffer`

| | Mutable | Thread-safe | Ghi chú |
|---|---|---|---|
| `String` | ❌ | ✅ (immutable) | |
| `StringBuilder` | ✅ | ❌ | Dùng mặc định khi build chuỗi |
| `StringBuffer` | ✅ | ✅ (method `synchronized`) | Legacy, gần như không cần |

- Java 9+ (JEP 280): `a + b + c` biên dịch thành `invokedynamic` gọi `StringConcatFactory` → JVM chọn chiến lược tối ưu lúc runtime (tính trước độ dài, cấp phát một lần). Vì vậy **một biểu thức** `+` đơn lẻ là hoàn toàn ổn.
- Nhưng **trong vòng lặp**, `s += x` vẫn tạo chuỗi mới mỗi lần → O(n²) bytes copy. Dùng `StringBuilder` (hoặc `String.join`, `Collectors.joining`).

```java
// O(n^2)
String csv = "";
for (String item : items) csv += item + ",";

// O(n)
StringBuilder sb = new StringBuilder(items.size() * 16); // ước lượng capacity, tránh grow nhiều lần
for (String item : items) sb.append(item).append(',');

// Gọn nhất
String csv2 = String.join(",", items);
```

`StringBuilder` bên trong có mảng `byte[]` grow khi đầy (khoảng `(old << 1) + 2`) → copy mảng. Đặt capacity ban đầu hợp lý khi biết trước kích thước.

### 3.5 Các API đáng nhớ (Java 11+)

```java
"  hi  ".strip();          // Unicode-aware (khác trim() chỉ cắt ký tự <= U+0020)
"".isBlank();              // true; "   ".isBlank() cũng true
"a\nb\nc".lines().count(); // 3
"ab".repeat(3);            // "ababab"
"Xin chào %s".formatted("Dat"); // Java 15
String.valueOf((Object) null);  // "null"
```

`substring`: từ **Java 7u6**, `substring` **copy** mảng (trước đó chia sẻ `char[]` gốc với offset → giữ cả chuỗi khổng lồ trong bộ nhớ chỉ vì một substring nhỏ — một memory leak kinh điển).

### 3.6 Text blocks (Java 15, JEP 378)

```java
String json = """
        {
          "name": "Đạt",
          "role": "Senior"
        }
        """;          // vị trí dấu """ đóng quyết định incidental indentation bị cắt
String sql = """
        SELECT id, name \
        FROM users \
        WHERE status = 'ACTIVE'""";   // '\' : nối dòng, không có newline
```

- Line terminator luôn chuẩn hóa thành `\n`. Khoảng trắng cuối dòng bị cắt (dùng `\s` để giữ).
- Text block vẫn là `String` thường → được intern như literal.

> 💡 **Góc nhìn Senior:**
> - Đừng lưu password trong `String` nếu có thể: immutable + có thể nằm trong pool → không xóa được khỏi bộ nhớ, dễ lộ qua heap dump. API như `JPasswordField.getPassword()`, `KeyStore` dùng `char[]` để có thể ghi đè sau khi dùng.
> - Heap dump của service thường cho thấy `byte[]`/`String` chiếm 20–40% heap. Trùng lặp chuỗi (ví dụ status `"ACTIVE"` đọc từ DB hàng triệu lần) có thể giải quyết bằng enum, `intern()` có kiểm soát, hoặc G1 string dedup.
> - `String.hashCode()` là `s[0]*31^(n-1) + ... + s[n-1]` — dễ tạo collision có chủ đích (hash flooding). HashMap Java 8 giảm thiểu bằng treeification (Module 02).
> - `toLowerCase()`/`toUpperCase()` không truyền `Locale` phụ thuộc locale mặc định — bug nổi tiếng "Turkish i": `"TITLE".toLowerCase()` với locale `tr` ra `"tıtle"`. Với key kỹ thuật, dùng `toLowerCase(Locale.ROOT)`.

> ⚠️ **Lỗi thường gặp:**
> - So sánh chuỗi bằng `==`. Luôn dùng `equals` (hoặc `"CONST".equals(var)` / `Objects.equals(a, b)` để tránh NPE).
> - `str.split(".")` — tham số là **regex**, `.` khớp mọi ký tự → mảng rỗng. Dùng `split("\\.")` hoặc `Pattern.quote(".")`.
> - `split` bỏ các chuỗi rỗng ở cuối: `"a,b,,".split(",")` có độ dài 2. Dùng `split(",", -1)` để giữ.
> - `new String(bytes)` / `getBytes()` không chỉ định charset → phụ thuộc platform (trước Java 18). Luôn dùng `StandardCharsets.UTF_8`.

### 🛠 Bài tập phần 3

**Bài 3.1 — String pool quiz (Cơ bản)**
- Đề bài: Viết chương trình in kết quả `==` cho 8 tổ hợp: literal/literal, literal/new, new/new, literal/intern, literal/concat-constant, literal/concat-runtime, literal/`final` concat, literal/text block cùng nội dung.
- Tiêu chí đạt: giải thích được từng kết quả dựa vào khái niệm *compile-time constant* (JLS §15.29).

**Bài 3.2 — Đếm ký tự đúng chuẩn Unicode (Trung bình)**
- Đề bài: Viết `int countVisibleChars(String s)` đếm đúng số ký tự với input chứa emoji và tiếng Việt (cả dạng dựng sẵn NFC lẫn tổ hợp NFD, ví dụ `"ế"`).
- Tiêu chí đạt: `"😀"` → 1; `"Việt"` dạng NFC và NFD đều → 4. Gợi ý: `java.text.Normalizer` và `BreakIterator.getCharacterInstance()`.

**Bài 3.3 — Benchmark nối chuỗi với JMH (Nâng cao)**
- Đề bài: Dùng JMH so sánh 4 cách nối 10.000 chuỗi: `+=` trong vòng lặp, `StringBuilder` không đặt capacity, `StringBuilder` có capacity, `String.join`.
- Tiêu chí đạt: báo cáo kết quả (ops/s và `-prof gc` allocation rate), kết luận có số liệu, giải thích vì sao `+=` tệ dù Java 9+ đã có indify concat.

<details>
<summary>Gợi ý lời giải</summary>

Bài 3.2:

```java
static int countVisibleChars(String s) {
    String n = Normalizer.normalize(s, Normalizer.Form.NFC);
    BreakIterator it = BreakIterator.getCharacterInstance();
    it.setText(n);
    int count = 0;
    while (it.next() != BreakIterator.DONE) count++;
    return count;
}
```

Bài 3.3: khung JMH:

```java
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
public class ConcatBench {
    List<String> items;
    @Setup public void setup() { items = IntStream.range(0, 10_000).mapToObj(i -> "item" + i).toList(); }
    @Benchmark public String plusEq() { String s = ""; for (String i : items) s += i; return s; }
    @Benchmark public String sb() { StringBuilder sb = new StringBuilder(); for (String i : items) sb.append(i); return sb.toString(); }
    @Benchmark public String sbCap() { StringBuilder sb = new StringBuilder(items.size() * 9); for (String i : items) sb.append(i); return sb.toString(); }
    @Benchmark public String join() { return String.join("", items); }
}
```

`+=` trong vòng lặp: mỗi lần tạo String mới và copy toàn bộ nội dung cũ — indify concat chỉ tối ưu *một* biểu thức, không tối ưu xuyên vòng lặp.

</details>

---

<a id="phan-4"></a>
## 4. Toán tử & ép kiểu: những cái bẫy

### 4.1 Numeric promotion và casting

- **Widening** (mở rộng, ngầm định): `byte → short → int → long → float → double`; `char → int`. Lưu ý `long → float` và `int → float` là widening nhưng **có thể mất độ chính xác**: `(float) 16_777_217` → `1.6777216E7`.
- **Narrowing** (thu hẹp) phải cast tường minh, cắt bit cao: `(byte) 200 == -56`.
- **Binary numeric promotion**: trong `a op b`, nếu có `double` → `double`; else `float`; else `long`; else **cả hai thành `int`**. Vì vậy `byte + byte` là `int`.

```java
byte b = 10;
// b = b + 1;      // ❌ compile error: int không gán được cho byte
b += 1;            // ✅ compound assignment có cast ngầm: b = (byte)(b + 1)
b += 300;          // ✅ compile được, kết quả bị tràn âm thầm!

char c = 'A';
System.out.println(c + 1);          // 66  (int)
System.out.println((char) (c + 1)); // B
System.out.println("" + c + 1);     // A1  (nối chuỗi từ trái sang phải)
System.out.println(1 + 2 + "3");    // 33
```

### 4.2 Integer overflow — im lặng và nguy hiểm

```java
long microsPerDay = 24 * 60 * 60 * 1000 * 1000;   // ❌ tính bằng int, tràn trước khi gán: 500654080
long ok           = 24L * 60 * 60 * 1000 * 1000;  // ✅ 86400000000

int mid = (low + high) / 2;        // ❌ tràn khi low + high > Integer.MAX_VALUE (bug binary search kinh điển trong JDK đến 2006)
int mid2 = low + (high - low) / 2; // ✅
int mid3 = (low + high) >>> 1;     // ✅

Math.abs(Integer.MIN_VALUE);       // -2147483648 (âm!)
Math.addExact(Integer.MAX_VALUE, 1); // ném ArithmeticException — dùng cho tiền, số lượng
```

### 4.3 Số thực và tiền tệ

```java
System.out.println(0.1 + 0.2);                 // 0.30000000000000004
System.out.println(0.1 + 0.2 == 0.3);          // false
System.out.println(new BigDecimal(0.1));       // 0.1000000000000000055511151231257827...
System.out.println(new BigDecimal("0.1"));     // 0.1  ✅ hoặc BigDecimal.valueOf(0.1)
System.out.println(new BigDecimal("2.0").equals(new BigDecimal("2.00")));    // false (khác scale)
System.out.println(new BigDecimal("2.0").compareTo(new BigDecimal("2.00"))); // 0
System.out.println(Double.NaN == Double.NaN);  // false; dùng Double.isNaN
System.out.println(1.0 / 0);                   // Infinity (không ném exception)
System.out.println(1 / 0);                     // ArithmeticException
System.out.println(-7 % 3);                    // -1 (dấu theo số bị chia); dùng Math.floorMod(-7, 3) == 2
```

- `BigDecimal.divide` không chỉ định `RoundingMode`/scale với kết quả vô hạn tuần hoàn (`1/3`) → `ArithmeticException`.
- `BigDecimal` dùng làm key `HashMap` hoặc trong `HashSet` → `2.0` và `2.00` là 2 phần tử khác nhau; trong `TreeSet` lại là 1 (vì dùng `compareTo`). Dùng `stripTrailingZeros()` để chuẩn hóa.

### 4.4 Các toán tử dễ nhầm

```java
int i = 0;
i = i++;            // i vẫn là 0: giá trị cũ (0) được lưu, i tăng lên 1, rồi gán lại 0
int x = 5;
int y = x++ + ++x;  // 5 + 7 = 12, x = 7

-8 >> 1;   // -4  (arithmetic shift, giữ bit dấu)
-8 >>> 1;  // 2147483644 (logical shift, điền 0)
1 << 32;   // 1   (shift distance với int được mask & 31)

boolean r = (obj != null) & obj.isValid(); // ❌ & không short-circuit → NPE
boolean r2 = (obj != null) && obj.isValid(); // ✅
```

`instanceof` với pattern matching (Java 16, JEP 394):

```java
if (o instanceof String s && !s.isBlank()) { System.out.println(s.length()); }
```

> 💡 **Góc nhìn Senior:**
> - Tiền tệ: dùng `BigDecimal` (hoặc `long` theo đơn vị nhỏ nhất như cent/đồng) với scale và `RoundingMode` được quy định rõ trong domain (ví dụ `HALF_EVEN` — banker's rounding). Tiền không bao giờ dùng `double`.
> - Overflow im lặng từng gây sự cố thật: bộ đếm `int` vượt 2^31 sau vài tháng uptime, timestamp tính bằng `int` giây, ID tự tăng `int` trong DB vượt giới hạn. Với giá trị tăng dần không giới hạn hãy dùng `long` và `Math.*Exact` ở chỗ quan trọng.
> - So sánh trong `Comparator` bằng phép trừ `a - b` có thể tràn → thứ tự sai. Dùng `Integer.compare(a, b)`.

> ⚠️ **Lỗi thường gặp:**
> - Tin rằng `float`/`double` "đủ chính xác" cho phép so sánh `==`. Hãy so sánh với epsilon hoặc dùng `BigDecimal`.
> - Quên rằng `switch` cũ (không có `->`) **fall-through** nếu thiếu `break`. Switch expression (Java 14) với `->` không fall-through và bắt buộc exhaustive.

### 🛠 Bài tập phần 4

**Bài 4.1 — Bảng bẫy toán tử (Cơ bản)**
- Đề bài: Viết 10 biểu thức "bẫy" (trong phần 4) vào một class, dự đoán kết quả trong comment, chạy và đối chiếu.
- Tiêu chí đạt: giải thích đúng ít nhất 9/10, dẫn được quy tắc JLS liên quan (promotion, compound assignment, shift masking).

**Bài 4.2 — Lớp `Money` an toàn (Trung bình)**
- Đề bài: Viết `record Money(BigDecimal amount, Currency currency)` với `plus`, `minus`, `multiply(BigDecimal factor)`, `allocate(int parts)` (chia đều sao cho tổng các phần bằng đúng số ban đầu, phần dư dồn vào các phần đầu).
- Tiêu chí đạt: constructor chuẩn hóa scale theo `currency.getDefaultFractionDigits()`; cộng khác currency ném `IllegalArgumentException`; `new Money(100.00 VND).allocate(3)` tổng lại đúng 100; `equals` coi `10.0` và `10.00` cùng currency là bằng nhau.

**Bài 4.3 — Phát hiện overflow (Nâng cao)**
- Đề bài: Viết `long safeFactorial(int n)` ném exception khi tràn, và `int saturatingAdd(int a, int b)` (bão hòa ở `MAX_VALUE`/`MIN_VALUE` thay vì quay vòng) **không dùng** `long` hay `Math.addExact`.
- Tiêu chí đạt: unit test với biên `Integer.MAX_VALUE`, `MIN_VALUE`, `-1`, `0`.

<details>
<summary>Gợi ý lời giải</summary>

Bài 4.2 (trích):

```java
public record Money(BigDecimal amount, Currency currency) {
    public Money {
        Objects.requireNonNull(amount); Objects.requireNonNull(currency);
        amount = amount.setScale(currency.getDefaultFractionDigits(), RoundingMode.HALF_EVEN);
    }
    public Money plus(Money o) { check(o); return new Money(amount.add(o.amount), currency); }
    private void check(Money o) {
        if (!currency.equals(o.currency)) throw new IllegalArgumentException("Currency mismatch");
    }
    public List<Money> allocate(int parts) {
        BigDecimal unit = BigDecimal.ONE.movePointLeft(currency.getDefaultFractionDigits());
        BigDecimal base = amount.divide(BigDecimal.valueOf(parts), currency.getDefaultFractionDigits(), RoundingMode.DOWN);
        BigDecimal remainder = amount.subtract(base.multiply(BigDecimal.valueOf(parts)));
        int extra = remainder.divide(unit).intValueExact();
        List<Money> out = new ArrayList<>();
        for (int i = 0; i < parts; i++) out.add(new Money(i < extra ? base.add(unit) : base, currency));
        return out;
    }
}
```

Do compact constructor chuẩn hóa scale nên `equals` tự động của record đúng với yêu cầu.

Bài 4.3: overflow khi cộng xảy ra khi hai số cùng dấu và kết quả khác dấu: `((a ^ r) & (b ^ r)) < 0` với `r = a + b`.

</details>

---

<a id="phan-5"></a>
## 5. OOP: 4 trụ cột, overloading vs overriding, static/dynamic binding

### 5.1 Bốn trụ cột — hiểu đúng thay vì học thuộc

| Trụ cột | Định nghĩa ngắn | Ý nghĩa thực tế cho Senior |
|---|---|---|
| **Encapsulation** (đóng gói) | Ẩn trạng thái bên trong, chỉ lộ hành vi qua API | Bảo vệ **invariant**. Không phải "field private + getter/setter cho mọi field" — setter tràn lan phá vỡ đóng gói. Hãy expose *hành vi* (`account.withdraw(amount)`) thay vì *dữ liệu* (`account.setBalance(...)`). |
| **Abstraction** (trừu tượng) | Chỉ thể hiện điều cần thiết, giấu chi tiết hiện thực | Interface ổn định, hiện thực thay đổi được (`PaymentGateway` có `VnPayGateway`, `MomoGateway`). |
| **Inheritance** (kế thừa) | Class con thừa hưởng trạng thái/hành vi của class cha | Quan hệ **is-a** thật sự. Kế thừa tạo coupling mạnh nhất trong Java → *"Favor composition over inheritance"* (Effective Java Item 18). |
| **Polymorphism** (đa hình) | Một lời gọi, nhiều hành vi tùy kiểu runtime | Cốt lõi của Open/Closed Principle, Strategy pattern, Spring DI. |

**Vấn đề "fragile base class"** — ví dụ kinh điển từ Effective Java Item 18:

```java
public class InstrumentedHashSet<E> extends HashSet<E> {
    private int addCount = 0;
    @Override public boolean add(E e) { addCount++; return super.add(e); }
    @Override public boolean addAll(Collection<? extends E> c) { addCount += c.size(); return super.addAll(c); }
    public int getAddCount() { return addCount; }
}
// s.addAll(List.of("a","b","c")); s.getAddCount() == 6 !!
// Vì HashSet.addAll (kế thừa từ AbstractCollection) gọi lại add() cho từng phần tử → đếm 2 lần.
```

Giải pháp: **composition + forwarding** (wrapper/decorator) — class bao một `Set<E>` và chuyển tiếp lời gọi; không phụ thuộc vào chi tiết hiện thực "tự gọi" (self-use) của class cha.

### 5.2 Overloading vs Overriding

| | Overloading | Overriding |
|---|---|---|
| Ở đâu | Cùng class (hoặc class con) | Class con so với class cha / interface |
| Chữ ký | **Cùng tên, khác danh sách tham số** | **Cùng tên + cùng danh sách tham số** (sau erasure) |
| Kiểu trả về | Tùy ý | Giống hoặc **covariant** (kiểu con) |
| Access modifier | Tùy ý | **Không được hẹp hơn** (protected → public được, public → protected không) |
| Checked exception | Tùy ý | **Không được rộng hơn hoặc thêm mới** checked exception (có thể bỏ bớt, hoặc ném kiểu con) |
| Quyết định lúc | **Compile-time** (static binding) | **Runtime** (dynamic dispatch) |
| `static`/`private`/`final` | Overload được | `static` → *hiding* (không phải override); `private` → không thấy nên không override; `final` → cấm override |

**Quy trình chọn overload (JLS §15.12.2)** — 3 pha, dừng ở pha đầu tiên tìm thấy:
1. Không boxing/unboxing, không varargs (chỉ dùng widening).
2. Cho phép boxing/unboxing.
3. Cho phép varargs.
Trong mỗi pha, chọn method **cụ thể nhất** (most specific); nếu không phân định được → lỗi compile *ambiguous*.

```java
public class OverloadDemo {
    static void f(long x)      { System.out.println("long"); }
    static void f(Integer x)   { System.out.println("Integer"); }
    static void f(int... x)    { System.out.println("varargs"); }
    static void g(Object o)    { System.out.println("Object"); }
    static void g(String s)    { System.out.println("String"); }

    public static void main(String[] args) {
        f(5);          // long     — pha 1: widening int→long thắng boxing
        g(null);       // String   — String cụ thể hơn Object
        Object o = "hi";
        g(o);          // Object   — overload chọn theo KIỂU KHAI BÁO (compile-time), không theo kiểu runtime
    }
}
```

### 5.3 Static binding vs dynamic binding

- **Static binding** (early): `private`, `static`, `final` method, constructor, **field**, và việc *chọn overload* — xác định lúc compile.
- **Dynamic binding** (late): instance method có thể override → JVM chọn hiện thực theo **kiểu runtime** của object.

Bên dưới: HotSpot dùng **vtable** (virtual method table) cho mỗi class: mảng con trỏ method; class con copy vtable của cha và thay các entry bị override. `invokevirtual` tra vtable theo index (O(1)); `invokeinterface` dùng **itable**. JIT còn tối ưu tiếp: nếu call site chỉ thấy 1 kiểu (*monomorphic*) hoặc 2 kiểu (*bimorphic*) thì inline trực tiếp kèm type guard; từ 3 kiểu trở lên (*megamorphic*) thì phải tra bảng thật.

```java
class Parent {
    String name = "parent";
    static String kind() { return "Parent.static"; }
    String who() { return "Parent.who"; }
}
class Child extends Parent {
    String name = "child";                       // field HIDING, không phải override
    static String kind() { return "Child.static"; } // method HIDING
    @Override String who() { return "Child.who"; }
}

Parent p = new Child();
System.out.println(p.name);   // parent       — field: theo kiểu khai báo
System.out.println(p.kind()); // Parent.static — static: theo kiểu khai báo (nên gọi Parent.kind())
System.out.println(p.who());  // Child.who    — instance method: theo kiểu runtime
```

### 5.4 Sealed classes và records (Java 17 LTS)

```java
public sealed interface Shape permits Circle, Square, Rectangle {}
public record Circle(double r) implements Shape {}
public record Square(double side) implements Shape {}
public non-sealed class Rectangle implements Shape { /* ... */ }

static double area(Shape s) {
    return switch (s) {                      // Java 21: pattern matching for switch (JEP 441)
        case Circle c   -> Math.PI * c.r() * c.r();
        case Square sq  -> sq.side() * sq.side();
        case Rectangle r -> 0; // ...
    };                                       // exhaustive nhờ sealed → không cần default
}
```

Sealed + record + pattern matching cho phép mô hình hóa **algebraic data types** — một cách "đa hình" khác: thay vì đặt hành vi trong từng class (OOP cổ điển), ta đặt hành vi trong switch exhaustive (data-oriented). Compiler báo lỗi khi thêm subtype mới mà quên xử lý.

> 💡 **Góc nhìn Senior:**
> - Khi nào dùng kế thừa? Khi class cha được **thiết kế và document để kế thừa** (Effective Java Item 19: "Design and document for inheritance or else prohibit it"). Ngược lại, đánh dấu `final` hoặc dùng sealed.
> - Phỏng vấn hay hỏi "Liskov Substitution Principle": class con phải thay được class cha mà không phá hợp đồng. Ví dụ vi phạm: `Square extends Rectangle` với `setWidth` thay đổi cả chiều cao.
> - Megamorphic call site trong hot path (ví dụ interface có 5+ hiện thực được gọi xen kẽ) có thể làm mất inlining — là một trong những lý do code "đẹp" chậm bất ngờ. Chỉ tối ưu khi profiler chỉ ra.

> ⚠️ **Lỗi thường gặp:**
> - Quên `@Override` → viết nhầm chữ ký (`equals(MyClass o)` thay vì `equals(Object o)`) thành **overload** chứ không override → `HashSet` không dùng nó.
> - Nghĩ rằng static method "override" được. Nó chỉ bị *hide*.
> - Gọi method có thể override **trong constructor** của class cha (xem Phần 9).

### 🛠 Bài tập phần 5

**Bài 5.1 — Overload resolution (Cơ bản)**
- Đề bài: Viết class có các overload `m(int)`, `m(long)`, `m(double)`, `m(Integer)`, `m(Object)`, `m(int...)`. Gọi với đối số kiểu `byte`, `char`, `Long`, `null`, `5f`, không đối số. Dự đoán trước rồi chạy.
- Tiêu chí đạt: dự đoán đúng, giải thích bằng 3 pha của JLS §15.12.2; chỉ ra trường hợp nào gây compile error.

**Bài 5.2 — Composition thay vì kế thừa (Trung bình)**
- Đề bài: Viết lại `InstrumentedHashSet` thành `InstrumentedSet<E>` dùng composition (`ForwardingSet<E> implements Set<E>`).
- Tiêu chí đạt: `addAll` 3 phần tử → `getAddCount() == 3`; class bọc được *bất kỳ* `Set` (HashSet, TreeSet, `ConcurrentHashMap.newKeySet()`).

**Bài 5.3 — Expression evaluator với sealed + pattern matching (Nâng cao)**
- Đề bài: Mô hình hóa biểu thức số học `sealed interface Expr permits Num, Add, Mul, Neg` bằng record. Viết `eval(Expr)` và `simplify(Expr)` (ví dụ `Mul(x, Num(1)) → x`, `Add(x, Num(0)) → x`, `Neg(Neg(x)) → x`) bằng switch pattern matching (Java 21, có record pattern lồng nhau).
- Tiêu chí đạt: không có `default` trong switch; thêm một record mới `Sub` thì compiler báo lỗi ở các switch chưa xử lý.

<details>
<summary>Gợi ý lời giải</summary>

Bài 5.1: `byte` → `m(int)` (widening); `char` → `m(int)`; `Long` → `m(Object)` (pha 1: không có unboxing; `Long` widening reference thành `Object`); `null` → **lỗi compile "reference to m is ambiguous"**: ở pha 1, method varargs `m(int...)` cũng được xét như `m(int[])` với đúng 1 đối số, nên ứng viên là `m(Integer)`, `m(Object)`, `m(int[])`; `Integer` và `int[]` đều cụ thể hơn `Object` nhưng không cái nào là subtype của cái kia → mơ hồ (đã kiểm chứng với javac 21). Bỏ overload varargs thì `m(null)` chọn `m(Integer)`. `5f` → `m(double)`; không đối số → `m(int...)`.

Bài 5.3 (trích):

```java
sealed interface Expr permits Num, Add, Mul, Neg {}
record Num(int v) implements Expr {}
record Add(Expr l, Expr r) implements Expr {}
record Mul(Expr l, Expr r) implements Expr {}
record Neg(Expr e) implements Expr {}

static Expr simplify(Expr e) {
    return switch (e) {
        case Mul(Expr x, Num(int v)) when v == 1 -> simplify(x);
        case Add(Expr x, Num(int v)) when v == 0 -> simplify(x);
        case Neg(Neg(Expr x))                    -> simplify(x);
        case Add(Expr l, Expr r) -> new Add(simplify(l), simplify(r));
        case Mul(Expr l, Expr r) -> new Mul(simplify(l), simplify(r));
        case Neg(Expr x)         -> new Neg(simplify(x));
        case Num n               -> n;
    };
}
```

</details>

---

<a id="phan-6"></a>
## 6. Abstract class vs interface; nested/inner/anonymous/local class

### 6.1 Abstract class vs interface (Java 8+)

| Tiêu chí | Abstract class | Interface |
|---|---|---|
| Kế thừa | Chỉ `extends` **một** class | `implements` **nhiều** interface |
| Trạng thái (instance field) | ✅ có | ❌ chỉ có hằng `public static final` |
| Constructor | ✅ (gọi qua `super(...)`) | ❌ |
| Method có thân | ✅ mọi loại | `default` (Java 8), `static` (Java 8), `private` / `private static` (Java 9) |
| Access của method | Mọi mức | Abstract/default/static ngầm định `public`; private cho helper |
| Mục đích | Chia sẻ **trạng thái + hiện thực** giữa các class có quan hệ chặt (template method) | Định nghĩa **hợp đồng/khả năng** (capability), cho phép một class có nhiều vai trò |

```java
public interface Discountable {
    BigDecimal price();

    default BigDecimal priceAfter(int percent) {              // Java 8: default method
        validate(percent);
        return price().multiply(BigDecimal.valueOf(100 - percent)).divide(BigDecimal.valueOf(100));
    }
    static Discountable of(BigDecimal p) { return () -> p; }  // Java 8: static method (không kế thừa được qua class implement)
    private void validate(int percent) {                       // Java 9: private method dùng chung cho các default method
        if (percent < 0 || percent > 100) throw new IllegalArgumentException("percent");
    }
}
```

**Diamond problem với default method — 3 quy tắc giải quyết:**
1. **Class luôn thắng**: method khai báo trong class (hoặc class cha) ưu tiên hơn mọi default method.
2. **Interface cụ thể hơn thắng**: nếu `B extends A` và cả hai có default `m()`, thì `B.m()` thắng.
3. Còn lại mơ hồ → **bắt buộc override** và có thể chọn bằng `X.super.m()`.

```java
interface Flyer  { default String move() { return "fly"; } }
interface Swimmer { default String move() { return "swim"; } }
class Duck implements Flyer, Swimmer {
    @Override public String move() { return Flyer.super.move() + "+" + Swimmer.super.move(); }
}
```

**Lý do default method ra đời**: *interface evolution* — Java 8 cần thêm `stream()`, `forEach`, `removeIf` vào `Collection` mà không phá vỡ hàng triệu class implement sẵn có.

### 6.2 Nested classes — 4 loại

| Loại | Khai báo | Tham chiếu tới outer instance? | Dùng khi |
|---|---|---|---|
| **Static nested class** | `static class Node` trong class | ❌ | Helper gắn chặt với outer: `Map.Entry`, `Builder`, `HashMap.Node` |
| **Inner class** (non-static) | `class Itr` trong class | ✅ ngầm định (`Outer.this`) | Cần truy cập trạng thái instance của outer: iterator của collection |
| **Local class** | Khai báo trong method | ✅ (nếu trong instance method) | Hiếm dùng |
| **Anonymous class** | `new Interface() { ... }` | ✅ (nếu trong instance method) | Hiện thực nhanh, phần lớn đã thay bằng lambda |

```java
public class Outer {
    private int counter = 0;

    class Inner { void inc() { counter++; } }            // truy cập trực tiếp field của Outer
    static class Nested { /* không có Outer.this */ }

    void demo() {
        Inner in = this.new Inner();                       // cần instance Outer
        Nested n = new Nested();
        int local = 10;                                    // effectively final
        Runnable r = new Runnable() {                      // anonymous class
            @Override public void run() { System.out.println(local + counter + " " + this.getClass()); }
        };
        Runnable l = () -> System.out.println(this.getClass()); // lambda: this = Outer
        // local++; // ❌ nếu bỏ comment: local không còn effectively final → lỗi compile ở trên
    }
}
```

**Lambda khác anonymous class ở chỗ:**
- `this` trong lambda là instance bao ngoài; trong anonymous class là chính anonymous object.
- Lambda không tạo file `.class` riêng lúc compile; dùng `invokedynamic` + `LambdaMetafactory` sinh class lúc runtime. Lambda không capture gì có thể là singleton.
- Lambda **không capture `Outer.this`** nếu không dùng đến member của outer; anonymous class trong instance method **luôn** giữ tham chiếu outer (cho tới Java 18, javac bắt đầu bỏ tham chiếu không dùng đến với inner class — JDK-8271623; đừng dựa vào điều này khi viết code tương thích nhiều phiên bản).

**Vì sao biến local được capture phải effectively final?** Vì lambda/anonymous class nhận **bản sao giá trị** của biến local (biến local sống trên stack và có thể chết trước lambda). Nếu cho phép sửa, bản sao và bản gốc sẽ lệch nhau → khó hiểu và không an toàn với thread.

> 💡 **Góc nhìn Senior:**
> - **Memory leak qua inner class**: inner class/anonymous class giữ `Outer.this`. Nếu instance inner sống lâu (đăng ký làm listener vào một registry static, đưa vào cache, chạy trong thread pool), toàn bộ outer object (có thể là Activity Android, request context, một object lớn) không bị GC. Mặc định hãy viết **static nested class** (Effective Java Item 24), chỉ dùng inner khi thực sự cần.
> - Inner class không serialize an toàn (tên class do compiler sinh, kèm tham chiếu outer ẩn).
> - Abstract class vẫn hữu ích cho **Template Method** (ví dụ `AbstractList` chỉ cần implement `get` và `size`), và các skeletal implementation đi kèm interface (`AbstractMap`, `AbstractSet`) — Effective Java Item 20.

> ⚠️ **Lỗi thường gặp:**
> - Tưởng interface có thể có field instance: `int x = 5;` trong interface thực chất là `public static final`.
> - Thêm `default` method vào interface public của thư viện mà trùng tên với method trong một implementation bên ngoài → hành vi thay đổi bất ngờ (class thắng, nhưng có thể chữ ký khác dẫn tới lỗi compile phía client).
> - Gọi static method của interface qua instance hoặc class implement: `ArrayList.of(...)` không tồn tại; phải là `List.of(...)`.

### 🛠 Bài tập phần 6

**Bài 6.1 — Diamond (Cơ bản)**
- Đề bài: Tạo 3 tình huống diamond với default method: (a) class cha có method cùng tên, (b) interface con override interface cha, (c) hai interface không liên quan. Viết code cho cả 3, ghi chú trường hợp nào compile lỗi và sửa.
- Tiêu chí đạt: giải thích đúng 3 quy tắc.

**Bài 6.2 — Tái hiện leak do inner class (Trung bình)**
- Đề bài: Tạo `class Screen { byte[] data = new byte[10_000_000]; void refresh() {...} void register() { EventBus.LISTENERS.add(new Listener() { public void onEvent(Event e) { refresh(); } }); } }` với `EventBus.LISTENERS` là `static List`. (Listener phải dùng member của `Screen` — từ Java 18 javac bỏ field `this$0` nếu inner/anonymous class không dùng tới outer instance.) Tạo 100 `Screen` rồi bỏ tham chiếu. Chạy với `-Xmx256m`.
- Tiêu chí đạt: tái hiện `OutOfMemoryError`; mở heap dump (`-XX:+HeapDumpOnOutOfMemoryError`, Eclipse MAT hoặc VisualVM) và chỉ ra GC root path `EventBus.LISTENERS → Screen$1 → this$0 → Screen`; sửa bằng static nested class + `WeakReference` hoặc cơ chế unregister.

**Bài 6.3 — Thiết kế plugin rule engine (Nâng cao)**
- Đề bài: Thiết kế interface `Rule<T>` có `boolean test(T)`, các default method `and`, `or`, `negate`, static factory `Rule.not(...)`, `Rule.allOf(...)`, private helper. Viết thêm `AbstractAuditedRule<T>` (template method: `test` final, gọi `doTest` và ghi log thời gian).
- Tiêu chí đạt: so sánh được khi nào thêm hành vi vào interface (default) và khi nào vào abstract class; có unit test.

<details>
<summary>Gợi ý lời giải</summary>

Bài 6.2: anonymous `Listener` trong instance method `register()` capture `Screen.this` (field `this$0`) → mỗi `Screen` với 10MB bị giữ. Sửa:

```java
static final class ScreenListener implements Listener {
    private final WeakReference<Screen> ref;
    ScreenListener(Screen s) { this.ref = new WeakReference<>(s); }
    public void onEvent(Event e) { Screen s = ref.get(); if (s != null) s.handle(e); }
}
```

Tốt hơn nữa: API `register` trả về `AutoCloseable`/`Subscription` để caller unregister rõ ràng — weak reference chỉ là lưới an toàn.

Bài 6.3:

```java
@FunctionalInterface
public interface Rule<T> {
    boolean test(T t);
    default Rule<T> and(Rule<? super T> o) { Objects.requireNonNull(o); return t -> test(t) && o.test(t); }
    default Rule<T> negate() { return t -> !test(t); }
    static <T> Rule<T> not(Rule<T> r) { return r.negate(); }
    @SafeVarargs static <T> Rule<T> allOf(Rule<T>... rules) { return t -> Arrays.stream(rules).allMatch(r -> r.test(t)); }
}
```

</details>

---

<a id="phan-7"></a>
## 7. Các method của `Object`: equals/hashCode, toString, clone, finalize

### 7.1 Hợp đồng `equals` (JavaDoc `Object.equals`)

Với mọi reference không null `x`, `y`, `z`:
1. **Reflexive**: `x.equals(x)` là `true`.
2. **Symmetric**: `x.equals(y)` ⇔ `y.equals(x)`.
3. **Transitive**: `x.equals(y)` và `y.equals(z)` ⇒ `x.equals(z)`.
4. **Consistent**: gọi nhiều lần cho cùng kết quả nếu object không đổi.
5. **Non-null**: `x.equals(null)` là `false`.

### 7.2 Hợp đồng `hashCode`

1. Gọi nhiều lần trên object không đổi → cùng giá trị (trong một lần chạy ứng dụng).
2. **`a.equals(b)` ⇒ `a.hashCode() == b.hashCode()`** (bắt buộc).
3. Hai object khác nhau *không bắt buộc* có hash khác nhau, nhưng hash phân tán tốt giúp hash table nhanh hơn.

Vi phạm (2) → object "biến mất" trong `HashMap`/`HashSet`: `put` vào bucket theo hash A, `get` với object bằng nhau nhưng hash B → tìm sai bucket → `null`.

### 7.3 Viết equals/hashCode chuẩn

```java
public final class Employee {
    private final String id;
    private final String email;
    private final int level;

    public Employee(String id, String email, int level) {
        this.id = Objects.requireNonNull(id);
        this.email = email;
        this.level = level;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;                       // tối ưu nhanh
        if (!(o instanceof Employee other)) return false; // xử lý luôn null; pattern matching Java 16
        return level == other.level
                && id.equals(other.id)
                && Objects.equals(email, other.email);     // null-safe
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, email, level);             // tiện nhưng tạo mảng varargs + boxing
        // Hot path: int r = id.hashCode(); r = 31 * r + Objects.hashCode(email); r = 31 * r + level; return r;
    }

    @Override
    public String toString() {
        return "Employee[id=%s, level=%d]".formatted(id, level); // không in email (PII)
    }
}
```

Hoặc đơn giản dùng **record** (Java 16): `equals`/`hashCode`/`toString` sinh tự động dựa trên tất cả component.

**`instanceof` hay `getClass()`?**
- `getClass() != o.getClass()`: không bao giờ bằng giữa class cha và class con → giữ symmetric nhưng **vi phạm Liskov** (một `Point` con không thêm field gì vẫn không bằng `Point` cha). Hibernate proxy (`Employee$HibernateProxy$...`) cũng sẽ không bằng entity thật.
- `instanceof`: thân thiện kế thừa nhưng nếu class con **thêm field tham gia so sánh** thì không thể vừa giữ symmetric vừa transitive (Effective Java Item 10: *"There is no way to extend an instantiable class and add a value component while preserving the equals contract"*). Giải pháp: class `final`, hoặc composition.

```java
// Vi phạm symmetric:
class Point { int x, y; /* equals dùng instanceof Point */ }
class ColorPoint extends Point { Color c; /* equals dùng instanceof ColorPoint, so cả màu */ }
// point.equals(colorPoint) == true; colorPoint.equals(point) == false
```

### 7.4 `toString`

- Mặc định: `getClass().getName() + "@" + Integer.toHexString(hashCode())`.
- Luôn override cho value class (giúp log/debug). **Không in dữ liệu nhạy cảm** (password, token, số thẻ, PII).
- Cẩn thận `toString` của entity JPA có quan hệ hai chiều → gọi lẫn nhau → `StackOverflowError`, hoặc kích hoạt lazy loading → `LazyInitializationException`/N+1 query khi log. Lombok `@Data`/`@ToString` trên entity là thủ phạm thường gặp.

### 7.5 `clone` — shallow vs deep copy

- `Object.clone()` là `protected native`; class phải implement **marker interface `Cloneable`**, nếu không ném `CloneNotSupportedException`.
- Mặc định là **shallow copy**: copy từng field; field reference vẫn trỏ cùng object.
- Thiết kế `Cloneable` bị xem là lỗi (Effective Java Item 13): marker interface không có method, thay đổi hành vi của method `protected` ở class khác, bỏ qua constructor, xung đột với field `final`.
- **Mảng**: `array.clone()` là public, trả đúng kiểu, là cách copy mảng được khuyến nghị — nhưng vẫn **shallow** (mảng 2 chiều chỉ copy mảng ngoài).

```java
public class Team implements Cloneable {
    private String name;
    private List<String> members = new ArrayList<>();

    @Override
    public Team clone() {                      // covariant return, public
        try {
            Team copy = (Team) super.clone();  // shallow
            copy.members = new ArrayList<>(members); // deep copy phần mutable
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);       // không thể xảy ra
        }
    }

    // Cách được khuyến nghị hơn: copy constructor / static factory
    public Team(Team other) { this.name = other.name; this.members = new ArrayList<>(other.members); }
    public Team() {}
}
```

Deep copy toàn bộ đồ thị object: viết tay (an toàn, rõ ràng), dùng serialization (chậm, rủi ro), hoặc thư viện mapping (MapStruct). Cẩn thận chu trình tham chiếu.

### 7.6 `finalize` — đã chết

- **Deprecated từ Java 9**, **deprecated for removal từ Java 18** (JEP 421). Không đảm bảo được gọi, không biết khi nào, chạy trên finalizer thread duy nhất (finalize chậm → queue ứ → OOM), có thể "hồi sinh" object, làm chậm GC (object cần ít nhất 2 chu kỳ GC), là vector tấn công (finalizer attack vào constructor ném exception).
- Thay thế: **try-with-resources / `AutoCloseable`** cho dọn tài nguyên chủ động; **`java.lang.ref.Cleaner`** (Java 9) làm lưới an toàn.

```java
public final class NativeBuffer implements AutoCloseable {
    private static final Cleaner CLEANER = Cleaner.create();
    private final State state;                 // State KHÔNG được tham chiếu tới NativeBuffer
    private final Cleaner.Cleanable cleanable;

    private static final class State implements Runnable {
        private long address;
        State(long address) { this.address = address; }
        @Override public void run() { /* free(address) */ address = 0; }
    }

    public NativeBuffer(long size) {
        this.state = new State(/* malloc(size) */ 42L);
        this.cleanable = CLEANER.register(this, state);
    }
    @Override public void close() { cleanable.clean(); }   // chạy đúng 1 lần dù gọi nhiều lần
}
```

### 7.7 Các method còn lại của `Object`

`getClass()` (final), `wait()/notify()/notifyAll()` (final — dùng cho monitor, xem module Concurrency), `hashCode`, `equals`, `toString`, `clone`, `finalize`. Mặc định `hashCode` là *identity hash* (không phải địa chỉ bộ nhớ; HotSpot sinh ngẫu nhiên theo thread và lưu vào object header khi lần đầu được gọi) — `System.identityHashCode(o)` luôn trả về giá trị này.

> 💡 **Góc nhìn Senior:**
> - **Entity JPA**: không dùng ID tự sinh (`@GeneratedValue`) cho `equals/hashCode` một cách ngây thơ — trước khi persist ID là `null`, sau khi persist ID thay đổi → hash thay đổi trong lúc object đang nằm trong `HashSet`. Cách phổ biến: dùng natural/business key bất biến, hoặc so ID khi khác null và `hashCode` trả về hằng số theo class (`getClass().hashCode()`).
> - `hashCode` trả về hằng số (`return 42;`) **đúng hợp đồng** nhưng biến HashMap thành danh sách liên kết/cây → O(n)/O(log n).
> - Field tham gia `equals/hashCode` nên **bất biến**. Key mutable trong HashMap là bug thực tế (Module 02).

> ⚠️ **Lỗi thường gặp:**
> - Override `equals` mà quên `hashCode`.
> - Viết `public boolean equals(Employee e)` — overload, không override.
> - So sánh `float`/`double` trong `equals` bằng `==` — dùng `Float.compare`/`Double.compare` (xử lý `NaN` và `-0.0`).
> - Dùng mảng làm field so sánh mà gọi `Objects.equals(arr1, arr2)` (so reference) — phải dùng `Arrays.equals`/`Arrays.hashCode`. Record có field mảng cũng gặp vấn đề này.

### 🛠 Bài tập phần 7

**Bài 7.1 — Tìm lỗi equals (Cơ bản)**
- Đề bài: Viết class `Product(String sku, String name)` chỉ override `equals` (theo `sku`), không override `hashCode`. Thêm 2 product cùng sku vào `HashSet`, kiểm tra `size()` và `contains`.
- Tiêu chí đạt: giải thích kết quả; sửa lại; viết test kiểm tra hợp đồng (có thể dùng thư viện **EqualsVerifier**).

**Bài 7.2 — Vi phạm transitive (Trung bình)**
- Đề bài: Hiện thực `Point` và `ColorPoint extends Point` với 2 cách `equals` khác nhau, chứng minh bằng unit test (a) vi phạm symmetric, (b) "sửa" bằng cách bỏ qua màu khi so với `Point` thường thì vi phạm transitive. Sau đó refactor sang composition (`ColorPoint` chứa `Point`).
- Tiêu chí đạt: 3 bộ test, giải thích vì sao composition giải quyết được.

**Bài 7.3 — Deep copy đồ thị có chu trình (Nâng cao)**
- Đề bài: Cho `class Node { String name; List<Node> neighbors; }` biểu diễn đồ thị có thể có chu trình. Viết `static Node deepCopy(Node root)` sao cho đồ thị kết quả đẳng cấu, không chia sẻ node nào với bản gốc và không lặp vô hạn.
- Tiêu chí đạt: test với chu trình A→B→A và self-loop; kiểm tra bằng `IdentityHashMap` rằng không có node nào bị dùng chung.

<details>
<summary>Gợi ý lời giải</summary>

Bài 7.3: duyệt DFS/BFS với `IdentityHashMap<Node, Node> visited` (dùng identity chứ không dùng `equals`, vì hai node khác nhau có thể "bằng nhau"):

```java
static Node deepCopy(Node root) {
    Map<Node, Node> copies = new IdentityHashMap<>();
    Deque<Node> stack = new ArrayDeque<>();
    copies.put(root, new Node(root.name));
    stack.push(root);
    while (!stack.isEmpty()) {
        Node cur = stack.pop();
        Node curCopy = copies.get(cur);
        for (Node nb : cur.neighbors) {
            Node nbCopy = copies.get(nb);
            if (nbCopy == null) { nbCopy = new Node(nb.name); copies.put(nb, nbCopy); stack.push(nb); }
            curCopy.neighbors.add(nbCopy);
        }
    }
    return copies.get(root);
}
```

</details>

---

<a id="phan-8"></a>
## 8. Immutability & defensive copy

### 8.1 Công thức class immutable (Effective Java Item 17)

1. Không cung cấp method thay đổi trạng thái (mutator).
2. Class không thể bị kế thừa: `final` class (hoặc constructor `private` + static factory).
3. Mọi field `private final`.
4. **Defensive copy** khi nhận vào và khi trả ra các thành phần mutable (`Date`, mảng, `List`...).
5. Không để `this` thoát ra (escape) trong constructor.

```java
public final class Order {
    private final String id;
    private final List<OrderLine> lines;      // OrderLine cũng phải immutable
    private final Instant createdAt;          // Instant immutable, khác java.util.Date

    public Order(String id, List<OrderLine> lines, Instant createdAt) {
        this.id = Objects.requireNonNull(id);
        this.lines = List.copyOf(lines);      // copy + immutable + chặn null (Java 10)
        this.createdAt = Objects.requireNonNull(createdAt);
        if (this.lines.isEmpty()) throw new IllegalArgumentException("Order phải có ít nhất 1 line");
    }

    public List<OrderLine> lines() { return lines; } // đã immutable → trả thẳng, không cần copy

    public Order addLine(OrderLine l) {               // "wither": trả về object mới
        List<OrderLine> copy = new ArrayList<>(lines);
        copy.add(l);
        return new Order(id, copy, createdAt);
    }
}
```

**Thứ tự quan trọng**: copy **trước**, validate **sau** (validate trên bản copy). Nếu validate trên tham số gốc rồi mới copy, thread khác có thể sửa tham số giữa hai bước — lỗ hổng **TOCTOU** (time-of-check/time-of-use), Effective Java Item 50.

```java
public Period(Date start, Date end) {
    this.start = new Date(start.getTime());   // copy trước
    this.end   = new Date(end.getTime());
    if (this.start.compareTo(this.end) > 0) throw new IllegalArgumentException(); // check sau, trên bản copy
}
```

Không dùng `clone()` để defensive copy tham số có kiểu không `final` (`Date` có thể là subclass độc hại với `clone` tùy ý).

### 8.2 Record và "shallowly immutable"

```java
public record Team(String name, List<String> members) {
    public Team {                               // compact canonical constructor
        Objects.requireNonNull(name);
        members = List.copyOf(members);         // nếu thiếu dòng này: caller giữ list gốc và sửa được
    }
}
```

Record chỉ đảm bảo field `final` (reference không đổi), **không** đảm bảo object được tham chiếu bất biến.

### 8.3 Immutability và memory model

Field `final` có đảm bảo đặc biệt trong Java Memory Model (JLS §17.5): sau khi constructor kết thúc (và `this` không escape), mọi thread thấy giá trị đúng của field `final` **mà không cần đồng bộ** — kể cả khi object được publish qua data race. Đây là lý do immutable object là *thread-safe tự nhiên* (chi tiết ở module Concurrency).

> 💡 **Góc nhìn Senior:**
> - Lợi ích: thread-safe, an toàn làm key của `HashMap`, cache được hashCode, dễ lập luận, chia sẻ tự do (flyweight như `BigInteger.ZERO`).
> - Chi phí: mỗi thay đổi tạo object mới → với object lớn sửa nhiều lần, dùng **companion mutable class** (như `String`/`StringBuilder`) hoặc **Builder**. Với GC thế hệ hiện đại, object ngắn hạn rất rẻ — đừng tối ưu sớm.
> - Trong kiến trúc: DTO/event/value object nên immutable (record). Entity JPA thì thường mutable (Hibernate cần setter/no-arg constructor) — giữ đóng gói bằng method nghiệp vụ.
> - `Collections.unmodifiableList(list)` **không** phải immutable: là *view* — list gốc thay đổi thì view thay đổi theo (Module 02).

> ⚠️ **Lỗi thường gặp:**
> - Getter trả về mảng/list nội bộ → caller sửa trực tiếp trạng thái. Trả về bản copy hoặc view unmodifiable.
> - Dùng `java.util.Date`/`Calendar` (mutable) trong value object. Dùng `java.time` (immutable).
> - Để `this` escape trong constructor (đăng ký listener, start thread) → thread khác thấy object chưa khởi tạo xong, mất đảm bảo của `final`.

### 🛠 Bài tập phần 8

**Bài 8.1 — Phá class "immutable" (Cơ bản)**
- Đề bài: Cho class `final class Schedule { private final Date start; private final List<String> tags; ... getters }` không có defensive copy. Viết 3 cách sửa trạng thái từ bên ngoài, rồi vá lại class.
- Tiêu chí đạt: 3 cách tấn công đều thất bại sau khi vá; dùng `java.time.Instant` thay `Date`.

**Bài 8.2 — Builder cho object immutable lớn (Trung bình)**
- Đề bài: Viết `HttpRequestSpec` immutable (method, url, headers `Map<String, List<String>>`, body `byte[]`, timeout `Duration`) với Builder, validate trong `build()`, và method `toBuilder()`.
- Tiêu chí đạt: header map trả về là immutable sâu (cả list bên trong); body được copy cả khi vào và khi ra; test chứng minh sửa builder sau `build()` không ảnh hưởng object đã build.

**Bài 8.3 — Persistent list (Nâng cao)**
- Đề bài: Hiện thực `ImmutableStack<T>` dạng persistent (cons list) với `push`, `pop`, `peek`, `isEmpty` đều O(1), chia sẻ cấu trúc giữa các phiên bản.
- Tiêu chí đạt: `s1 = empty.push(1); s2 = s1.push(2); s3 = s1.push(3)` — `s2` và `s3` cùng chia sẻ node của `s1` (kiểm chứng bằng `==`); thread-safe không cần lock.

<details>
<summary>Gợi ý lời giải</summary>

Bài 8.3:

```java
public abstract sealed class ImmutableStack<T> permits ImmutableStack.Empty, ImmutableStack.Cons {
    @SuppressWarnings("rawtypes") private static final Empty EMPTY = new Empty();
    @SuppressWarnings("unchecked") public static <T> ImmutableStack<T> empty() { return EMPTY; }
    public ImmutableStack<T> push(T v) { return new Cons<>(v, this); }
    public abstract T peek();
    public abstract ImmutableStack<T> pop();
    public abstract boolean isEmpty();

    static final class Empty<T> extends ImmutableStack<T> {
        public T peek() { throw new NoSuchElementException(); }
        public ImmutableStack<T> pop() { throw new NoSuchElementException(); }
        public boolean isEmpty() { return true; }
    }
    static final class Cons<T> extends ImmutableStack<T> {
        private final T head; private final ImmutableStack<T> tail;
        Cons(T head, ImmutableStack<T> tail) { this.head = head; this.tail = tail; }
        public T peek() { return head; }
        public ImmutableStack<T> pop() { return tail; }
        public boolean isEmpty() { return false; }
    }
}
```

Bài 8.2: `headers.entrySet().stream().collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, e -> List.copyOf(e.getValue())))`; `body` dùng `body.clone()` trong constructor và getter.

</details>

---

<a id="phan-9"></a>
## 9. `static`, `final`, thứ tự khởi tạo, access modifiers & packages

### 9.1 `static`

- **Static field**: thuộc về class (một bản duy nhất cho mỗi class loader), lưu trong `Class` mirror object trên heap (Java 8+; metadata class nằm ở **Metaspace** thay cho PermGen).
- **Static method**: không có `this`, không override được (chỉ hide), gọi qua `invokestatic`.
- **Static nested class**: xem Phần 6.
- **Static import**: `import static java.lang.Math.max;`.

### 9.2 `final` — 4 ngữ cảnh

| Đặt ở | Ý nghĩa |
|---|---|
| Biến local / tham số | Không gán lại được |
| Field | Gán đúng 1 lần (tại khai báo, initializer block hoặc **mọi** constructor); có đảm bảo JMM (Phần 8.3) |
| Method | Không được override |
| Class | Không được kế thừa (`String`, `Integer`, record ngầm định `final`) |

`final` **không** làm object bất biến: `final List<String> list = new ArrayList<>(); list.add("x");` hoàn toàn hợp lệ.

**Compile-time constant** (`static final` kiểu primitive/String, khởi tạo bằng constant expression) được **inline** vào class sử dụng. Hệ quả production: đổi giá trị `public static final int TIMEOUT = 30;` trong thư viện A, rebuild A nhưng **không rebuild** B → B vẫn dùng 30. Đồng thời, truy cập constant như vậy **không kích hoạt** khởi tạo class.

### 9.3 Thứ tự khởi tạo

**Khởi tạo class** (chạy 1 lần, khi *lần đầu chủ động sử dụng* — JLS §12.4.1: `new`, gọi static method, đọc/ghi static field không phải constant, reflection `Class.forName`, khởi tạo class con, class chứa `main`):
1. Khởi tạo class cha trước (interface cha *không* được khởi tạo theo cách này).
2. Static field initializer và `static {}` block **theo thứ tự xuất hiện trong mã nguồn**.

**Khởi tạo instance** (mỗi lần `new`):
1. Cấp phát bộ nhớ, mọi field = giá trị mặc định (0/null/false).
2. Gọi constructor → dòng đầu tiên là `this(...)` hoặc `super(...)` (ngầm định `super()`).
3. Constructor cha chạy xong (đệ quy lên tới `Object`).
4. Instance field initializer và `{}` instance block của class hiện tại **theo thứ tự trong mã nguồn**.
5. Phần thân còn lại của constructor.

```java
class Base {
    static { System.out.println("1. Base static"); }
    { System.out.println("3. Base instance block"); }
    Base() {
        System.out.println("4. Base constructor");
        init();                                   // ⚠️ gọi method có thể override trong constructor
    }
    void init() {}
}

class Derived extends Base {
    static { System.out.println("2. Derived static"); }
    private String name = "derived";              // gán SAU KHI Base() chạy xong
    { System.out.println("5. Derived instance block, name=" + name); }
    Derived() { System.out.println("6. Derived constructor"); }
    @Override void init() { System.out.println("   Derived.init thấy name=" + name); } // in "null"!
}

public class InitOrder {
    public static void main(String[] args) {
        new Derived();
        System.out.println("--- lần 2 ---");
        new Derived();                            // không in lại static
    }
}
```

Output:

```
1. Base static
2. Derived static
3. Base instance block
4. Base constructor
   Derived.init thấy name=null
5. Derived instance block, name=derived
6. Derived constructor
--- lần 2 ---
3. Base instance block
...
```

**Initialization-on-demand holder idiom** — lazy singleton thread-safe không cần `synchronized`/`volatile`:

```java
public final class Config {
    private Config() { /* load nặng */ }
    private static final class Holder { static final Config INSTANCE = new Config(); }
    public static Config getInstance() { return Holder.INSTANCE; } // Holder chỉ được khởi tạo khi gọi lần đầu
}
```

Dựa trên việc JVM khởi tạo class có lock và chỉ một lần. Lưu ý: nếu static initializer ném exception, class rơi vào trạng thái *erroneous* → mọi lần dùng sau ném `NoClassDefFoundError` (chỉ lần đầu là `ExceptionInInitializerError`). Production: lỗi đọc config trong `static {}` khiến cả service "chết" theo kiểu khó đọc log.

**Deadlock khi khởi tạo class**: thread 1 khởi tạo `A` (static init của A dùng `B`), thread 2 khởi tạo `B` (static init dùng `A`) → mỗi thread giữ init lock của một class và chờ class kia → deadlock, thread dump hiển thị trạng thái `RUNNABLE` với `in Object.wait()`/"waiting on condition" khó nhận ra. Tránh vòng phụ thuộc giữa các static initializer.

### 9.4 Access modifiers & packages

| Modifier | Cùng class | Cùng package | Subclass khác package | Mọi nơi |
|---|---|---|---|---|
| `private` | ✅ | ❌ | ❌ | ❌ |
| *(default / package-private)* | ✅ | ✅ | ❌ | ❌ |
| `protected` | ✅ | ✅ | ✅ (chỉ qua kế thừa*) | ❌ |
| `public` | ✅ | ✅ | ✅ | ✅ |

\* Ở package khác, subclass chỉ truy cập được member `protected` **qua tham chiếu kiểu chính nó (hoặc con của nó)**, không qua tham chiếu kiểu cha:

```java
package a;
public class Animal { protected void breathe() {} }

package b;
public class Dog extends a.Animal {
    void test(a.Animal other, Dog dog) {
        breathe();          // ✅ this
        dog.breathe();      // ✅ tham chiếu kiểu Dog
        // other.breathe(); // ❌ compile error: tham chiếu kiểu Animal ở package khác
    }
}
```

- Top-level class chỉ có thể `public` hoặc package-private. Một file `.java` có tối đa một top-level `public` class, cùng tên file.
- **Package không phân cấp về quyền truy cập**: `com.shop.order` và `com.shop.order.internal` là hai package hoàn toàn riêng biệt.
- **JPMS (Java 9)**: module chỉ export package được khai báo trong `module-info.java` → *strong encapsulation*: `public` không còn nghĩa là "mọi nơi" nữa. Java 16 (JEP 396) mặc định chặn truy cập reflection vào internal của JDK, Java 17 (JEP 403) bỏ hẳn tùy chọn `--illegal-access` → thư viện cũ dùng reflection vào `sun.*`/`java.*` private sẽ lỗi `InaccessibleObjectException`, cần `--add-opens`.

> 💡 **Góc nhìn Senior:**
> - Mặc định chọn mức truy cập **hẹp nhất** có thể (Effective Java Item 15). Package-private là công cụ thiết kế tốt: class hiện thực để package-private, chỉ public interface/factory.
> - Static mutable state (`static Map cache`) là kẻ thù của testability và concurrency, và là nguồn memory leak (không bao giờ GC được tới khi class loader bị unload — trong app server, giữ class loader của webapp cũ sau redeploy → `OutOfMemoryError: Metaspace`).
> - Spring bean mặc định singleton — gần giống "static state" về mặt chia sẻ giữa thread; field mutable trong bean là bug concurrency tiềm ẩn.

> ⚠️ **Lỗi thường gặp:**
> - Gọi overridable method trong constructor (ví dụ trên) → class con thấy field chưa khởi tạo.
> - Nghĩ `static final List` là hằng số bất biến.
> - Đổi giá trị constant trong thư viện mà không rebuild module phụ thuộc.

### 🛠 Bài tập phần 9

**Bài 9.1 — Dự đoán thứ tự khởi tạo (Cơ bản)**
- Đề bài: Mở rộng ví dụ `InitOrder` với 3 tầng kế thừa, mỗi tầng có static field khởi tạo bằng method có `println`, static block, instance block, constructor có tham số gọi `this(...)`. Dự đoán output trước khi chạy.
- Tiêu chí đạt: đúng hoàn toàn thứ tự; giải thích vì sao truy cập `Child.CONSTANT` (compile-time constant) không in gì nhưng `Child.nonConstant` lại khởi tạo cả `Parent` và `Child`.

**Bài 9.2 — Singleton 5 cách (Trung bình)**
- Đề bài: Viết singleton theo 5 cách: eager, lazy `synchronized`, double-checked locking (`volatile`), holder idiom, enum. Viết test đa luồng (100 thread qua `CountDownLatch`) xác nhận chỉ một instance.
- Tiêu chí đạt: giải thích vì sao double-checked locking **bắt buộc** `volatile` (reordering khi publish); chỉ ra cách nào chống được tấn công reflection (`setAccessible` gọi constructor private) và serialization.

**Bài 9.3 — Tái hiện deadlock khởi tạo class (Nâng cao)**
- Đề bài: Tạo class `A` và `B` mà static initializer phụ thuộc lẫn nhau, khởi tạo từ 2 thread đồng thời. Lấy thread dump bằng `jstack <pid>` hoặc `jcmd <pid> Thread.print`.
- Tiêu chí đạt: tái hiện được treo; chỉ ra trong thread dump dấu hiệu (thread đang ở `<clinit>`); đề xuất cách sửa.

<details>
<summary>Gợi ý lời giải</summary>

Bài 9.2 — double-checked locking đúng:

```java
public final class Dcl {
    private static volatile Dcl instance;
    private Dcl() {}
    public static Dcl get() {
        Dcl local = instance;              // đọc volatile 1 lần (tối ưu)
        if (local == null) {
            synchronized (Dcl.class) {
                local = instance;
                if (local == null) instance = local = new Dcl();
            }
        }
        return local;
    }
}
```

Không có `volatile`, `instance = new Dcl()` có thể được reorder: gán reference trước khi constructor chạy xong → thread khác thấy object chưa hoàn chỉnh. Enum singleton chống được cả reflection (JVM cấm tạo instance enum qua reflection: `IllegalArgumentException: Cannot reflectively create enum objects`) lẫn serialization.

Bài 9.3:

```java
class A { static final Object X; static { sleep(100); X = B.Y; } }
class B { static final Object Y; static { sleep(100); Y = A.X; } }
// Thread t1 = new Thread(() -> System.out.println(A.X)); Thread t2 = new Thread(() -> System.out.println(B.Y));
```

Sửa: phá vòng phụ thuộc, hoặc gom khởi tạo vào một nơi duy nhất.

</details>

---

<a id="phan-10"></a>
## 10. Enum nâng cao

### 10.1 Enum là class

`enum Status { ACTIVE, INACTIVE }` được compiler biến thành `final class Status extends java.lang.Enum<Status>` với mỗi hằng là một `public static final` instance, constructor ngầm định `private`, kèm `values()` (trả về **mảng mới** mỗi lần gọi) và `valueOf(String)`.

```java
public enum OrderStatus {
    NEW("Mới tạo", false) {
        @Override public OrderStatus next() { return PAID; }
    },
    PAID("Đã thanh toán", false) {
        @Override public OrderStatus next() { return SHIPPED; }
    },
    SHIPPED("Đang giao", false) {
        @Override public OrderStatus next() { return DELIVERED; }
    },
    DELIVERED("Đã giao", true) {
        @Override public OrderStatus next() { throw new IllegalStateException("Trạng thái cuối"); }
    };

    private final String label;
    private final boolean terminal;

    OrderStatus(String label, boolean terminal) { this.label = label; this.terminal = terminal; }

    public abstract OrderStatus next();          // constant-specific method (mỗi hằng một body = một subclass ẩn danh)
    public String label() { return label; }
    public boolean isTerminal() { return terminal; }

    private static final Map<String, OrderStatus> BY_LABEL =
            Arrays.stream(values()).collect(Collectors.toUnmodifiableMap(OrderStatus::label, s -> s));
    public static Optional<OrderStatus> fromLabel(String label) { return Optional.ofNullable(BY_LABEL.get(label)); }
}
```

- Constructor của enum **không truy cập được static field** của chính enum (chưa khởi tạo vì hằng enum được khởi tạo trước). Vì vậy map tra cứu phải khởi tạo trong static field/block *sau* các hằng.
- Enum có thể implement interface (ví dụ `enum Operation implements IntBinaryOperator`) — cách mở rộng "enum" mà vẫn giữ type-safe (Effective Java Item 38).
- Enum trong `switch` (Java 14+ switch expression) — compiler kiểm tra exhaustive khi không có `default`.

### 10.2 `ordinal()` — đừng dùng cho logic/persistence

`ordinal()` là vị trí khai báo. Chèn thêm hằng vào giữa → mọi giá trị đã lưu DB bị lệch. JPA: dùng `@Enumerated(EnumType.STRING)` (mặc định là `ORDINAL`!) hoặc `AttributeConverter` với mã ổn định do bạn định nghĩa.

### 10.3 `EnumSet` và `EnumMap`

- **`EnumSet`**: hiện thực bằng **bit vector**. `RegularEnumSet` dùng một `long` (≤ 64 hằng), `JumboEnumSet` dùng `long[]`. `contains/add/remove` là phép bit O(1), `containsAll`/`retainAll` là AND/OR trên word — cực nhanh, thay thế "bit flags" kiểu `int` (Effective Java Item 36).
- **`EnumMap`**: hiện thực bằng **mảng** đánh index theo `ordinal()` → nhanh và gọn hơn `HashMap`, iteration theo thứ tự khai báo. Không cho key `null`.

```java
EnumSet<DayOfWeek> weekend = EnumSet.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
EnumSet<DayOfWeek> workdays = EnumSet.complementOf(weekend);

Map<OrderStatus, Long> countByStatus = orders.stream()
        .collect(Collectors.groupingBy(Order::status, () -> new EnumMap<>(OrderStatus.class), Collectors.counting()));
```

### 10.4 Enum singleton

```java
public enum IdGenerator {
    INSTANCE;
    private final AtomicLong seq = new AtomicLong();
    public long next() { return seq.incrementAndGet(); }
}
```

Effective Java Item 3: *"a single-element enum type is often the best way to implement a singleton"* — an toàn với serialization (enum được serialize theo tên, `readObject` trả về đúng hằng) và reflection. Nhược điểm: không lazy, không kế thừa class khác được, khó mock trong test (trong ứng dụng Spring thì cứ để container quản lý singleton).

> 💡 **Góc nhìn Senior:**
> - Enum là lựa chọn tự nhiên cho **state machine** (transition hợp lệ định nghĩa trong enum), **strategy** cố định, và thay thế `String`/`int` magic constant.
> - **Tiến hóa API**: thêm hằng enum mới là thay đổi phá vỡ tương thích với client đang `switch` exhaustive (Java 21 sẽ ném `MatchException`/`IncompatibleClassChangeError` lúc runtime nếu client chưa recompile). Với enum trong API giữa các service (JSON), consumer nên chịu được giá trị lạ (Jackson: `READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE` với `@JsonEnumDefaultValue`).
> - `valueOf` ném `IllegalArgumentException` với tên lạ — đừng gọi trực tiếp trên input người dùng mà không bắt lỗi.

> ⚠️ **Lỗi thường gặp:**
> - Lưu `ordinal` vào DB.
> - Gọi `values()` trong hot path (cấp phát mảng mới mỗi lần) — cache vào `private static final` (lưu ý: trả ra ngoài thì phải copy hoặc dùng `List.of`).
> - So sánh enum bằng `equals` thì đúng, nhưng `==` an toàn hơn (không NPE, kiểm tra kiểu lúc compile).

### 🛠 Bài tập phần 10

**Bài 10.1 — Enum có hành vi (Cơ bản)**
- Đề bài: Viết `enum Operation { PLUS("+"), MINUS("-"), TIMES("*"), DIVIDE("/") }` với method abstract `double apply(double, double)` và `static Optional<Operation> fromSymbol(String)`.
- Tiêu chí đạt: không dùng `switch`; tra cứu symbol O(1) qua map tĩnh.

**Bài 10.2 — Phân quyền với EnumSet (Trung bình)**
- Đề bài: Mô hình hóa `enum Permission { READ, WRITE, DELETE, ADMIN, ... }` và `Role` chứa `EnumSet<Permission>`. Viết `hasAll`, `hasAny`, merge nhiều role, và serialize tập quyền thành một `long` bitmask để lưu DB (và parse ngược lại).
- Tiêu chí đạt: test round-trip; benchmark (JMH) `EnumSet.containsAll` so với `HashSet.containsAll` cho 20 quyền.

**Bài 10.3 — State machine đơn hàng an toàn (Nâng cao)**
- Đề bài: Mở rộng `OrderStatus` thêm `CANCELLED`, `REFUNDED`; định nghĩa transition hợp lệ bằng `EnumMap<OrderStatus, EnumSet<OrderStatus>>`; viết `Order.transitionTo(OrderStatus target)` ném exception nghiệp vụ khi không hợp lệ; lưu DB bằng mã ổn định (`"N"`, `"P"`, ...) qua converter.
- Tiêu chí đạt: test bảng (parameterized test) bao phủ mọi cặp trạng thái; thêm hằng mới vào giữa enum không làm thay đổi dữ liệu đã lưu.

<details>
<summary>Gợi ý lời giải</summary>

Bài 10.2 — bitmask (giả sử ≤ 64 quyền; mã ổn định nên là một field `bit` riêng thay vì `ordinal()` để chịu được việc chèn hằng):

```java
enum Permission {
    READ(0), WRITE(1), DELETE(2), ADMIN(3);
    final int bit; Permission(int bit) { this.bit = bit; }
}
static long toMask(Set<Permission> ps) { long m = 0; for (Permission p : ps) m |= 1L << p.bit; return m; }
static EnumSet<Permission> fromMask(long m) {
    EnumSet<Permission> s = EnumSet.noneOf(Permission.class);
    for (Permission p : Permission.values()) if ((m & (1L << p.bit)) != 0) s.add(p);
    return s;
}
```

Bài 10.3:

```java
private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED = new EnumMap<>(Map.of(
    NEW, EnumSet.of(PAID, CANCELLED),
    PAID, EnumSet.of(SHIPPED, REFUNDED),
    SHIPPED, EnumSet.of(DELIVERED),
    DELIVERED, EnumSet.noneOf(OrderStatus.class),
    CANCELLED, EnumSet.noneOf(OrderStatus.class),
    REFUNDED, EnumSet.noneOf(OrderStatus.class)));
```

</details>

---

<a id="phan-11"></a>
## 11. Exceptions

### 11.1 Hierarchy

```
Throwable
├── Error                      (lỗi nghiêm trọng của JVM/môi trường — thường KHÔNG bắt)
│   ├── OutOfMemoryError, StackOverflowError (VirtualMachineError)
│   ├── NoClassDefFoundError, ExceptionInInitializerError (LinkageError)
│   └── AssertionError
└── Exception                  (checked — bắt buộc khai báo throws hoặc catch)
    ├── IOException, SQLException, InterruptedException, ...
    └── RuntimeException       (unchecked)
        ├── NullPointerException, IllegalArgumentException, IllegalStateException
        ├── IndexOutOfBoundsException, ClassCastException, ArithmeticException
        ├── UnsupportedOperationException, ConcurrentModificationException
        └── ...
```

- **Checked**: compiler buộc xử lý. Dành cho tình huống *có thể phục hồi* mà caller *nên* xử lý (file không tồn tại, mạng lỗi).
- **Unchecked** (`RuntimeException`): lỗi lập trình (vi phạm precondition) hoặc tình huống caller không thể làm gì hợp lý.
- Kiểm tra checked exception chỉ có ở mức **compiler** (JVM không phân biệt) — Kotlin, Scala, Lombok `@SneakyThrows` lợi dụng điều này.

**Tranh luận checked vs unchecked**: Spring, Hibernate (từ 3.x), hầu hết framework hiện đại chọn unchecked (`DataAccessException`). Checked exception không hợp với lambda/stream (`Function` không khai báo `throws`) và làm rò rỉ chi tiết hiện thực qua chữ ký method.

### 11.2 try-catch-finally và các cái bẫy

```java
static int tricky() {
    int x = 1;
    try {
        return x;           // giá trị trả về (1) được lưu lại TRƯỚC khi chạy finally
    } finally {
        x = 2;              // không ảnh hưởng giá trị đã lưu với primitive
    }
}                           // trả về 1

@SuppressWarnings("finally")
static int worse() {
    try {
        throw new IllegalStateException("lỗi thật");
    } finally {
        return 0;           // ❌ return trong finally NUỐT exception — lỗi thật biến mất không dấu vết
    }
}
```

- `finally` luôn chạy trừ khi: `System.exit()`, JVM crash/bị kill, thread daemon bị dừng khi JVM tắt, hoặc vòng lặp vô hạn trong try.
- **Multi-catch** (Java 7): `catch (IOException | SQLException e)` — biến `e` ngầm định `final`; các kiểu không được có quan hệ cha-con.
- Thứ tự catch: con trước cha, ngược lại lỗi compile (unreachable).

### 11.3 try-with-resources & suppressed exceptions

```java
try (var in = Files.newBufferedReader(src, StandardCharsets.UTF_8);
     var out = Files.newBufferedWriter(dst, StandardCharsets.UTF_8)) {
    in.transferTo(out);                      // Java 10
} // đóng theo thứ tự NGƯỢC: out trước, rồi in — kể cả khi có exception

// Java 9: resource đã khai báo bên ngoài, là final/effectively final
BufferedReader reader = Files.newBufferedReader(src);
try (reader) { ... }
```

Bên dưới, compiler sinh code tương đương: nếu thân `try` ném exception `A` và `close()` ném `B`, thì `A` được ném ra và `B` được gắn vào qua `A.addSuppressed(B)`. Trước Java 7, với try-finally viết tay, `B` **đè mất** `A` — lỗi gốc biến mất.

```java
class Res implements AutoCloseable {
    public void close() { throw new IllegalStateException("close failed"); }
}
try (Res r = new Res()) {
    throw new RuntimeException("body failed");
} catch (RuntimeException e) {
    System.out.println(e.getMessage());                    // body failed
    System.out.println(e.getSuppressed()[0].getMessage()); // close failed
}
```

`AutoCloseable.close()` khai báo `throws Exception`; `Closeable.close()` khai báo `throws IOException` và yêu cầu **idempotent**.

### 11.4 Custom exception & exception translation

```java
public class InsufficientBalanceException extends RuntimeException {
    private final String accountId;
    private final BigDecimal requested;

    public InsufficientBalanceException(String accountId, BigDecimal requested, BigDecimal available) {
        super("Account %s: requested %s, available %s".formatted(accountId, requested, available));
        this.accountId = accountId;
        this.requested = requested;
    }
    public String accountId() { return accountId; }
}

// Exception translation + chaining: tầng trên không phụ thuộc chi tiết tầng dưới, nhưng KHÔNG mất cause
public User findUser(long id) {
    try {
        return jdbcLoad(id);
    } catch (SQLException e) {
        throw new UserRepositoryException("Không tải được user id=" + id, e); // truyền cause!
    }
}
```

### 11.5 Best practices (Effective Java Item 69–77, Clean Code chương Error Handling)

1. Chỉ dùng exception cho tình huống **bất thường**, không dùng cho control flow.
2. Checked cho lỗi có thể phục hồi, runtime cho lỗi lập trình.
3. Ưu tiên exception chuẩn: `IllegalArgumentException`, `IllegalStateException`, `NullPointerException` (`Objects.requireNonNull`), `UnsupportedOperationException`, `IndexOutOfBoundsException`.
4. Ném exception phù hợp với mức trừu tượng (translation), giữ cause.
5. Document exception bằng `@throws`.
6. Message chứa **dữ liệu chẩn đoán** (id, giá trị gây lỗi) — nhưng không chứa secret/PII.
7. **Failure atomicity**: method thất bại nên để object ở trạng thái như trước khi gọi (validate trước khi sửa, hoặc làm trên bản copy).
8. **Không nuốt exception** (`catch (Exception e) {}`). Nếu cố ý bỏ qua, đặt tên biến `ignored` và comment lý do.
9. Hoặc log, hoặc ném lại — **không làm cả hai** (log trùng lặp ở mọi tầng).
10. Không bắt `Throwable`/`Error` (trừ ở tầng ngoài cùng để log rồi để ứng dụng chết đúng cách).

### 11.6 Chi phí của exception và các vấn đề production

- Chi phí lớn nhất là **`fillInStackTrace()`** (duyệt stack khi tạo exception), không phải `throw`. Exception dùng cho control flow nóng (ví dụ validate hàng triệu bản ghi) có thể tốn đáng kể CPU. Có thể tắt stack trace cho exception nghiệp vụ "biết trước" bằng constructor `protected Throwable(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace)` với `writableStackTrace = false` — đổi lại mất thông tin debug.
- **`OmitStackTraceInFastThrow`**: HotSpot với C2 tối ưu các exception *implicit* (NPE, `ArithmeticException`, `ArrayIndexOutOfBoundsException`, `ClassCastException`...) ném lặp lại nhiều lần tại cùng chỗ → thay bằng một exception dựng sẵn **không có stack trace và message**. Trên production bạn thấy log `java.lang.NullPointerException` trống trơn. Tìm log đầu tiên (còn stack trace) hoặc chạy với `-XX:-OmitStackTraceInFastThrow`.
- **`InterruptedException`**: không được nuốt. Nếu không ném lại được, phải khôi phục cờ: `Thread.currentThread().interrupt();` (chi tiết ở module Concurrency).
- **Exception trong thread pool**: exception trong task `submit()` bị giữ trong `Future` — không ai gọi `get()` thì lỗi biến mất im lặng. Với `execute()`, exception đi tới `UncaughtExceptionHandler`.
- `@Transactional` của Spring mặc định chỉ rollback với **unchecked** exception và `Error`; checked exception → **commit**! (dùng `rollbackFor`).

> 💡 **Góc nhìn Senior:**
> - Thiết kế exception cho một service: một exception gốc của domain (`DomainException`) có *error code* ổn định, tầng web ánh xạ sang HTTP status/Problem Details (RFC 7807/9457) tại một chỗ (`@ControllerAdvice`), không để stack trace lộ ra client.
> - Phân biệt lỗi *business* (đoán trước, không cần stack trace, log mức WARN/INFO) với lỗi *technical* (cần stack trace, log ERROR, kích hoạt alert).
> - Với Java 21 virtual threads và structured concurrency, mô hình lan truyền exception giữa các subtask cũng cần chú ý — xem module Concurrency.

> ⚠️ **Lỗi thường gặp:**
> - `catch (Exception e) { log.error(e.getMessage()); }` — mất stack trace và cause; `getMessage()` của NPE cũ có thể là `null`. Dùng `log.error("Context ...", e)`.
> - `throw new ServiceException(e.getMessage())` — mất cause. Dùng `new ServiceException("...", e)`.
> - `return` hoặc `throw` trong `finally`.
> - Đóng resource thủ công không đúng thứ tự, hoặc quên đóng trong nhánh lỗi → rò rỉ connection/file descriptor (`Too many open files`).

### 🛠 Bài tập phần 11

**Bài 11.1 — Dự đoán kết quả try/finally (Cơ bản)**
- Đề bài: Viết 5 method: return trong try + sửa primitive trong finally; return trong try + sửa `StringBuilder` trong finally; throw trong try + return trong finally; exception trong catch + finally; `System.exit(0)` trong try. Dự đoán và chạy.
- Tiêu chí đạt: giải thích đúng cả 5 (đặc biệt vì sao sửa `StringBuilder` trong finally *có* ảnh hưởng tới giá trị trả về).

**Bài 11.2 — Suppressed exceptions (Trung bình)**
- Đề bài: Viết 2 resource giả lập ném exception khi `close()`. Viết cùng một logic theo 2 cách: try-finally kiểu Java 6 và try-with-resources. In ra exception chính và các suppressed.
- Tiêu chí đạt: chứng minh được Java 6 style làm mất exception gốc; với TWR thấy đủ 1 exception chính + 2 suppressed, đúng thứ tự đóng.

**Bài 11.3 — Thiết kế exception hierarchy cho service (Nâng cao)**
- Đề bài: Thiết kế hierarchy cho service thanh toán: `PaymentException` (gốc, unchecked, có `ErrorCode` enum và map `details`), các nhánh `ValidationException`, `BusinessRuleException` (không stack trace), `ExternalServiceException` (có `retryable`). Viết lớp `ExceptionMapper` chuyển exception thành DTO dạng Problem Details. Viết adapter gọi một "gateway" ném `IOException`/`TimeoutException` và translate chúng.
- Tiêu chí đạt: không mất cause; `BusinessRuleException` tạo nhanh (benchmark so với exception có stack trace); unit test cho mapper.

<details>
<summary>Gợi ý lời giải</summary>

Bài 11.1: method 2 — giá trị trả về là *reference* tới cùng `StringBuilder`, finally sửa object đó nên caller thấy thay đổi. Method 5 — finally không chạy.

Bài 11.3 — exception không stack trace:

```java
public class BusinessRuleException extends PaymentException {
    public BusinessRuleException(ErrorCode code, String msg) {
        super(code, msg, null, /*enableSuppression*/ false, /*writableStackTrace*/ false);
    }
}
// PaymentException cần constructor protected tương ứng gọi super(msg, cause, enableSuppression, writableStackTrace)
```

</details>

---

<a id="phan-12"></a>
## 12. I/O & NIO.2, serialization

### 12.1 Byte stream vs character stream

| | Đơn vị | Lớp gốc | Ví dụ |
|---|---|---|---|
| Byte stream | `byte` | `InputStream` / `OutputStream` | `FileInputStream`, `BufferedInputStream`, `ObjectInputStream` |
| Character stream | `char` (đã decode) | `Reader` / `Writer` | `FileReader`, `BufferedReader`, `InputStreamReader` (cầu nối byte→char) |

`java.io` được thiết kế theo **Decorator pattern**: `new BufferedReader(new InputStreamReader(new FileInputStream(f), UTF_8))` — mỗi lớp bọc thêm một khả năng (decode, buffer...).

**Charset**: chuyển byte ↔ char luôn cần charset. Trước **Java 18**, charset mặc định phụ thuộc OS (Windows thường là `windows-1252`/`Cp1258`) → tiếng Việt bị lỗi font khi deploy khác môi trường. Java 18 (JEP 400) mặc định UTF-8. Dù vậy, **luôn chỉ định charset tường minh**.

**Buffering**: mỗi lời gọi `read()` trên `FileInputStream` không buffer là một system call. Đọc file 100MB từng byte = 100 triệu syscall. `BufferedInputStream`/`BufferedReader` (buffer mặc định 8 KB) giảm xuống còn vài chục nghìn. Với `BufferedWriter`/`BufferedOutputStream`, nhớ `flush()`/`close()` nếu không dữ liệu nằm lại trong buffer.

### 12.2 NIO.2 — `Path` và `Files` (Java 7+)

```java
Path dir = Path.of("data", "reports");                     // Java 11; Java 7-10: Paths.get(...)
Files.createDirectories(dir);
Path file = dir.resolve("2026-10.csv");

Files.writeString(file, "id,amount\n1,100\n", StandardCharsets.UTF_8);       // Java 11
String all = Files.readString(file);                                          // Java 11 — chỉ cho file nhỏ
List<String> lines = Files.readAllLines(file);                                // nạp hết vào RAM

try (Stream<String> s = Files.lines(file)) {                                  // lazy — PHẢI đóng
    long total = s.skip(1).mapToLong(l -> Long.parseLong(l.split(",")[1])).sum();
}

try (Stream<Path> paths = Files.walk(dir, 3)) {                               // PHẢI đóng (giữ directory handle)
    paths.filter(p -> p.toString().endsWith(".csv")).forEach(System.out::println);
}

// Ghi an toàn: ghi file tạm rồi move atomic (tránh người đọc thấy file ghi dở)
Path tmp = Files.createTempFile(dir, "report", ".tmp");
Files.writeString(tmp, "...");
Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
```

- `Path` immutable, `normalize()`, `relativize()`, `toAbsolutePath()`, `toRealPath()` (resolve symlink).
- `Files` cung cấp: copy/move/delete (`deleteIfExists`), thuộc tính (`size`, `getLastModifiedTime`, `readAttributes`), `newBufferedReader/Writer`, `newInputStream`, `walkFileTree` (Visitor), `WatchService` theo dõi thay đổi thư mục.
- NIO (Java 1.4) còn có `Channel`, `ByteBuffer` (heap vs direct), `FileChannel.transferTo` (zero-copy, dùng `sendfile` trên Linux), memory-mapped file (`MappedByteBuffer`), và non-blocking `Selector` cho network (nền tảng của Netty) — đọc thêm khi học về hiệu năng I/O.

**Path traversal** — lỗ hổng bảo mật thường gặp khi ghép tên file từ input người dùng:

```java
Path base = Path.of("/srv/uploads").toAbsolutePath().normalize();
Path target = base.resolve(userFileName).normalize();
if (!target.startsWith(base)) throw new SecurityException("Path traversal: " + userFileName); // chặn "../../etc/passwd"
```

### 12.3 Serialization (Java built-in) và rủi ro

```java
public class Session implements Serializable {
    @Serial private static final long serialVersionUID = 1L;  // @Serial: Java 14
    private String userId;
    private transient String accessToken;                   // không được serialize
    private transient Cache cache;                          // dữ liệu dẫn xuất / không serialize được
}
```

- `Serializable` là marker interface. Mọi field non-transient phải serializable (hoặc ném `NotSerializableException`).
- **`serialVersionUID`**: nếu không khai báo, JVM tính từ cấu trúc class (tên, field, method...) → thêm một method cũng làm đổi UID → `InvalidClassException` khi đọc dữ liệu cũ. Luôn khai báo tường minh.
- Deserialization **không gọi constructor** của class serializable (chỉ gọi no-arg constructor của class cha *không* serializable gần nhất) → bỏ qua validate trong constructor, phá invariant. Có thể dùng `readObject` để validate, `readResolve` để giữ singleton.
- **Record** (Java 16) deserialize qua **canonical constructor** → validate được áp dụng. An toàn hơn class thường.

**Rủi ro bảo mật — vì sao Senior cần biết:**
- `ObjectInputStream.readObject()` trên dữ liệu không tin cậy có thể dẫn tới **Remote Code Execution** thông qua *gadget chain*: chuỗi các class hợp lệ trong classpath (Apache Commons Collections, Spring, Groovy...) mà method `readObject`/`hashCode`/`compareTo` được gọi trong quá trình deserialize, ghép lại thành lời gọi tùy ý. Lỗ hổng 2015 (công cụ ysoserial) ảnh hưởng WebLogic, JBoss, Jenkins...
- Cũng có thể gây **DoS** (ví dụ "billion laughs" bằng `HashSet` lồng nhau).
- Biện pháp: **không deserialize dữ liệu không tin cậy** (Effective Java Item 85: *"The best way to avoid serialization exploits is never to deserialize anything"*); dùng định dạng dữ liệu thuần (JSON, Protobuf, Avro) với schema; nếu bắt buộc, dùng **`ObjectInputFilter`** (Java 9, JEP 290; filter theo context Java 17, JEP 415) để allow-list class, giới hạn độ sâu/số object.

```java
ObjectInputFilter filter = ObjectInputFilter.Config.createFilter(
        "com.myapp.dto.*;java.base/*;!*;maxdepth=10;maxrefs=10000;maxbytes=1048576");
try (var ois = new ObjectInputStream(input)) {
    ois.setObjectInputFilter(filter);
    Object o = ois.readObject();
}
```

> 💡 **Góc nhìn Senior:**
> - Java serialization vẫn xuất hiện ngầm: session replication trong Tomcat, cache phân tán (một số cấu hình Redis/Hazelcast mặc định dùng JDK serializer), RMI, JMX. Kiểm tra cấu hình serializer khi review hệ thống.
> - Đọc file lớn: stream theo dòng (`BufferedReader.readLine`, `Files.lines`) — tránh `readAllLines`/`readAllBytes` với file có thể lớn (OOM). Ghi file quan trọng: ghi tạm + `ATOMIC_MOVE`, cân nhắc `FileChannel.force(true)` khi cần durability.
> - Mọi `InputStream`/`Reader`/`Stream` từ `Files.lines`/`Files.walk`/`Files.list` đều giữ file descriptor — quên đóng trên server lâu ngày → `Too many open files`.

> ⚠️ **Lỗi thường gặp:**
> - `new FileReader(file)` (trước Java 11 không nhận charset) → dùng `Files.newBufferedReader(path, UTF_8)`.
> - `InputStream.read(byte[])` không đảm bảo đọc đủ số byte yêu cầu — phải lặp, hoặc dùng `readNBytes`/`readAllBytes` (Java 9/11).
> - Bắt `IOException` khi đóng rồi bỏ qua làm mất lỗi ghi (lỗi flush cuối cùng thường xuất hiện ở `close()`).

### 🛠 Bài tập phần 12

**Bài 12.1 — Thống kê file log (Cơ bản)**
- Đề bài: Đọc file log ~1GB (tự sinh), đếm số dòng theo level (`INFO`, `WARN`, `ERROR`) bằng `Files.lines`.
- Tiêu chí đạt: bộ nhớ heap ổn định khi chạy với `-Xmx64m`; kết quả dùng `EnumMap`; đóng stream đúng cách.

**Bài 12.2 — Copy file: so sánh 4 cách (Trung bình)**
- Đề bài: Copy file 500MB bằng: (1) `FileInputStream.read()` từng byte, (2) có `BufferedInputStream`, (3) `byte[8192]` thủ công, (4) `Files.copy`/`FileChannel.transferTo`.
- Tiêu chí đạt: bảng thời gian; giải thích bằng số system call (dùng `strace -c` trên Linux nếu có thể) và zero-copy.

**Bài 12.3 — Khai thác & phòng thủ deserialization (Nâng cao)**
- Đề bài: Viết class `Range implements Serializable` có invariant `start <= end` kiểm tra trong constructor. Tạo byte stream độc hại (sửa trực tiếp byte hoặc dùng class "giả" cùng tên/UID) để deserialize ra `Range` với `start > end`. Sau đó phòng thủ bằng 3 cách: `readObject` validate, chuyển sang `record`, và `ObjectInputFilter` allow-list.
- Tiêu chí đạt: test chứng minh tấn công thành công trước khi vá và thất bại sau khi vá; giải thích vì sao record an toàn hơn.

<details>
<summary>Gợi ý lời giải</summary>

Bài 12.3 — cách đơn giản để tạo byte độc hại: tạm bỏ kiểm tra trong constructor (hoặc dùng reflection set field) ở một chương trình khác, serialize ra file, rồi deserialize bằng phiên bản class có kiểm tra trong constructor — vẫn đọc được vì constructor không được gọi. Vá:

```java
@Serial
private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    if (start > end) throw new InvalidObjectException("start > end");
}
```

Record: `record Range(int start, int end) implements Serializable { Range { if (start > end) throw new IllegalArgumentException(); } }` — deserialize gọi canonical constructor nên invariant luôn được kiểm tra.

Bài 12.2: (1) chậm hơn hàng trăm lần vì mỗi byte một syscall; (4) nhanh nhất vì kernel copy trực tiếp giữa các file descriptor.

</details>

---

<a id="phan-13"></a>
## 13. Reflection & Annotations

### 13.1 Reflection

Reflection cho phép kiểm tra và thao tác class/field/method/constructor **lúc runtime**. Là nền tảng của Spring (DI, `@Autowired`), Hibernate (map entity), Jackson (serialize JSON), JUnit (tìm `@Test`), Mockito.

```java
Class<?> clazz = Class.forName("com.shop.User");          // nạp + KHỞI TẠO class (mặc định)
Class<User> c2 = User.class;                              // không khởi tạo class
Object user = clazz.getDeclaredConstructor().newInstance(); // Class.newInstance() deprecated từ Java 9

for (Field f : clazz.getDeclaredFields()) {               // getDeclaredXxx: mọi access, chỉ của class này
    f.setAccessible(true);                                // bỏ kiểm tra access (có thể bị JPMS chặn)
    System.out.println(f.getName() + " = " + f.get(user));
}
Method m = clazz.getMethod("getName");                    // getXxx: chỉ public, gồm cả kế thừa
Object name = m.invoke(user);                             // exception gốc bị bọc trong InvocationTargetException
```

**Hiệu năng:**
- `Method.invoke` chậm hơn gọi trực tiếp (kiểm tra access, boxing tham số, mảng varargs). Trước Java 18, 15 lần gọi đầu dùng native accessor, sau đó sinh bytecode accessor (*inflation*). Từ **Java 18 (JEP 416)**, core reflection được hiện thực lại trên `MethodHandle`.
- Tra cứu `getDeclaredMethod` tốn kém → **cache** đối tượng `Method`/`Field` (Spring, Jackson đều cache metadata).
- `MethodHandle` (Java 7, `java.lang.invoke`) — khi lưu trong `static final` thì JIT có thể inline gần bằng gọi trực tiếp.

**Dynamic proxy** (`java.lang.reflect.Proxy`): tạo object implement interface lúc runtime, chuyển mọi lời gọi tới `InvocationHandler` — cơ chế của Spring AOP (JDK proxy), `@Transactional`, Spring Data repository, Feign client. Với class không có interface, Spring dùng **CGLIB/ByteBuddy** tạo subclass (nên method `final`/`private` không bị proxy và self-invocation (`this.method()`) bỏ qua proxy — nguyên nhân kinh điển của `@Transactional` "không hoạt động").

```java
interface OrderService { void place(String id); }

@SuppressWarnings("unchecked")
static <T> T timed(T target, Class<T> iface) {
    return (T) Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[]{iface}, (proxy, method, args) -> {
        long start = System.nanoTime();
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();                         // ném lại exception thật, không phải wrapper
        } finally {
            System.out.printf("%s took %d µs%n", method.getName(), (System.nanoTime() - start) / 1_000);
        }
    });
}
```

### 13.2 Annotations

Annotation là metadata gắn vào khai báo. Tự nó **không làm gì**; cần một "bộ xử lý": compiler, annotation processor lúc compile, hoặc code đọc bằng reflection lúc runtime.

```java
@Retention(RetentionPolicy.RUNTIME)       // SOURCE (bị bỏ sau compile: @Override, Lombok), CLASS (mặc định: có trong .class, không đọc được bằng reflection), RUNTIME
@Target(ElementType.FIELD)                // TYPE, METHOD, FIELD, PARAMETER, CONSTRUCTOR, RECORD_COMPONENT, TYPE_USE...
@Documented
public @interface Length {
    int min() default 0;
    int max() default Integer.MAX_VALUE;
    String message() default "độ dài không hợp lệ";
}
```

- Thuộc tính của annotation chỉ được là: primitive, `String`, `Class`, enum, annotation, hoặc mảng của các kiểu đó; giá trị phải là **hằng số lúc compile**.
- `@Inherited`: annotation trên class được class con "thừa hưởng" (chỉ với class, không với interface/method).
- `@Repeatable` (Java 8): cho phép gắn nhiều lần cùng annotation.
- Meta-annotation & composed annotation: Spring tạo `@RestController = @Controller + @ResponseBody`, `@SpringBootApplication`; Spring dùng `AnnotatedElementUtils`/`MergedAnnotations` để đọc thuộc tính qua nhiều tầng (Java thuần không làm điều này).
- **Annotation processor** (JSR 269, `javax.annotation.processing`): chạy lúc compile, sinh code mới — Lombok (đặc biệt: sửa AST), MapStruct, Dagger, Immutables. Không tốn chi phí runtime như reflection.

> 💡 **Góc nhìn Senior:**
> - Reflection phá vỡ đóng gói và tính an toàn kiểu, khó refactor (đổi tên field không báo lỗi compile), và bị JPMS hạn chế. Trong code ứng dụng, chỉ dùng khi viết framework/thư viện hạ tầng.
> - **GraalVM Native Image** cần khai báo trước mọi thứ dùng reflection (reachability metadata) — lý do Spring Boot 3 đầu tư mạnh vào AOT processing.
> - Startup chậm của ứng dụng Spring lớn một phần do classpath scanning + reflection. Biết điều này để giải thích trade-off với Micronaut/Quarkus (DI lúc compile).

> ⚠️ **Lỗi thường gặp:**
> - Annotation tự viết quên `@Retention(RUNTIME)` → `getAnnotation` trả về `null`, tưởng code đọc sai.
> - Bắt `InvocationTargetException` rồi log nó thay vì `getCause()` → mất lỗi thật.
> - `Class.forName` dùng sai class loader trong môi trường nhiều loader (app server, plugin) — cân nhắc `Thread.currentThread().getContextClassLoader()`.

### 🛠 Bài tập phần 13

**Bài 13.1 — Object inspector (Cơ bản)**
- Đề bài: Viết `String describe(Object o)` in tên class, class cha, interface, và mọi field (kể cả private, kể cả field kế thừa) cùng giá trị.
- Tiêu chí đạt: duyệt lên hết chuỗi `getSuperclass()`; bỏ qua field `static`; xử lý được `InaccessibleObjectException` cho class của JDK.

**Bài 13.2 — Validator bằng annotation (Trung bình)**
- Đề bài: Tự viết các annotation `@NotNull`, `@Length(min,max)`, `@Range(min,max)` và class `Validator.validate(Object) → List<Violation>` dùng reflection, có cache metadata theo class (`ClassValue` hoặc `ConcurrentHashMap<Class<?>, List<FieldRule>>`).
- Tiêu chí đạt: hỗ trợ cả record (đọc qua `getRecordComponents()`); benchmark lần gọi thứ 1 so với lần thứ 10.000 để thấy tác dụng cache.

**Bài 13.3 — Mini DI container (Nâng cao)**
- Đề bài: Viết container nhỏ: quét các class được đăng ký có `@Component`, tạo singleton bằng constructor injection (constructor duy nhất hoặc có `@Inject`), phát hiện **vòng phụ thuộc** và báo lỗi rõ ràng, hỗ trợ `@Timed` trên interface method bằng dynamic proxy.
- Tiêu chí đạt: test cho: tạo đồ thị 4 bean, phát hiện vòng A→B→A, proxy `@Timed` ghi log thời gian; giải thích vì sao gọi `this.method()` bên trong bean không đi qua proxy.

<details>
<summary>Gợi ý lời giải</summary>

Bài 13.3 — phát hiện vòng phụ thuộc bằng tập "đang tạo":

```java
private final Map<Class<?>, Object> singletons = new HashMap<>();
private final Set<Class<?>> creating = new LinkedHashSet<>();

<T> T getBean(Class<T> type) {
    Object existing = singletons.get(type);
    if (existing != null) return type.cast(existing);
    if (!creating.add(type)) throw new IllegalStateException("Circular dependency: " + creating + " -> " + type.getSimpleName());
    try {
        Constructor<?> ctor = pickConstructor(type);
        Object[] args = Arrays.stream(ctor.getParameterTypes()).map(this::getBean).toArray();
        Object bean = wrapWithProxyIfNeeded(ctor.newInstance(args));
        singletons.put(type, bean);
        return type.cast(bean);
    } catch (ReflectiveOperationException e) {
        throw new IllegalStateException("Cannot create " + type, e);
    } finally {
        creating.remove(type);
    }
}
```

Spring cũng phát hiện vòng phụ thuộc constructor injection theo cách tương tự (`BeanCurrentlyInCreationException`).

</details>

---

<a id="du-an-mini"></a>
## Dự án mini

### "Library Core" — lõi nghiệp vụ quản lý thư viện, không dùng framework

**Bối cảnh:** Xây dựng thư viện Java thuần (Java 17 hoặc 21, Maven/Gradle, JUnit 5) làm lõi nghiệp vụ cho hệ thống mượn sách. Không dùng Spring, không dùng DB — lưu trữ bằng file. Mục tiêu là áp dụng *toàn bộ* kiến thức của module.

**Yêu cầu chức năng**
1. **Domain model immutable**: `Book` (record: isbn, title, authors `List<String>`, publishedYear), `Member` (id, name, email, `MembershipTier` enum), `Loan` (id, book isbn, member id, borrowedAt, dueAt, returnedAt có thể null). Có defensive copy, validate trong compact constructor (ISBN-13 checksum, email hợp lệ).
2. **Enum có hành vi**: `MembershipTier { BASIC, SILVER, GOLD }` quy định số sách tối đa, số ngày mượn, công thức phí trễ hạn (constant-specific method), phí tính bằng `BigDecimal` với `RoundingMode` rõ ràng. `LoanStatus` là state machine (`BORROWED → RETURNED | OVERDUE → RETURNED | LOST`) với transition hợp lệ trong `EnumMap<LoanStatus, EnumSet<LoanStatus>>`.
3. **Service** `LibraryService`: `borrow`, `returnBook`, `renew`, `findOverdue(Clock)`. Thời gian lấy từ `java.time.Clock` để test được.
4. **Exception hierarchy**: `LibraryException` (unchecked, có `ErrorCode`), `BookNotAvailableException`, `LoanLimitExceededException`, `InvalidTransitionException`; repository dịch `IOException` sang `StorageException` giữ cause.
5. **Persistence bằng NIO.2**: lưu dữ liệu ra file CSV/JSON-lines dùng `Files.newBufferedWriter(..., UTF_8)`; ghi theo kiểu file tạm + `ATOMIC_MOVE`; đọc lazy bằng `Files.lines` trong try-with-resources; chống path traversal khi export báo cáo theo tên người dùng nhập.
6. **Validator bằng annotation + reflection**: `@NotBlank`, `@Email`, `@Range` trên các command object (`BorrowCommand`), có cache metadata.
7. **Audit proxy**: dùng `java.lang.reflect.Proxy` bọc interface `LibraryService` để log thời gian và kết quả (thành công/exception) mỗi lời gọi.

**Yêu cầu phi chức năng**
- `equals/hashCode` đúng hợp đồng (kiểm bằng EqualsVerifier hoặc test thủ công cho đủ 5 tính chất).
- Không có `Integer`/`Long` so sánh bằng `==`, không `double` cho tiền, mọi I/O chỉ định charset.
- Không rò rỉ resource: mọi stream/reader trong try-with-resources.
- Không dùng Java serialization; nếu thêm tính năng "import backup" bằng `ObjectInputStream` thì bắt buộc có `ObjectInputFilter`.
- Test coverage ≥ 80% cho domain và service; có ít nhất 1 test dùng `Clock.fixed` cho logic quá hạn.
- `README` ngắn mô tả quyết định thiết kế (vì sao record/enum/unchecked exception...).

**Tiêu chí chấm (100 điểm)**

| Hạng mục | Điểm |
|---|---|
| Domain immutable đúng (defensive copy, validate, equals/hashCode) | 20 |
| Enum & state machine (không `ordinal` cho persistence, dùng EnumMap/EnumSet) | 15 |
| Exception design (hierarchy, translation giữ cause, message có ngữ cảnh, không nuốt lỗi) | 15 |
| I/O đúng chuẩn (charset, try-with-resources, ghi atomic, path traversal) | 15 |
| Reflection/annotation validator + proxy hoạt động, có cache | 15 |
| Test (độ bao phủ, test biên, Clock) | 15 |
| Code sạch, đặt tên tốt, README giải thích trade-off | 5 |

---

<a id="checklist-tu-danh-gia"></a>
## Checklist tự đánh giá

- [ ] Tôi giải thích được khác biệt JDK/JRE/JVM, pipeline javac → class loading (loading, linking, initialization) → interpreter/C1/C2, và parent delegation.
- [ ] Tôi phân biệt được `ClassNotFoundException` và `NoClassDefFoundError`, và biết tình huống static initializer lỗi gây `NoClassDefFoundError`.
- [ ] Tôi giải thích được Integer cache, các bẫy autoboxing (`==`, NPE khi unboxing trong ternary, `remove(int)` vs `remove(Object)`).
- [ ] Tôi chứng minh được bằng code rằng Java luôn pass-by-value.
- [ ] Tôi giải thích được String pool, `intern()`, compact strings, vì sao `+=` trong vòng lặp chậm, và vì sao String immutable.
- [ ] Tôi nhận ra được các bẫy overflow, compound assignment, `BigDecimal(double)`, `BigDecimal.equals` vs `compareTo`.
- [ ] Tôi phát biểu được quy tắc override (access, return type, checked exception) và 3 pha chọn overload.
- [ ] Tôi giải thích được static vs dynamic binding, field hiding, method hiding, và vtable ở mức khái niệm.
- [ ] Tôi so sánh được abstract class và interface (Java 8/9+), giải quyết được diamond với default method.
- [ ] Tôi biết 4 loại nested class, vì sao inner class có thể gây memory leak, khác biệt lambda và anonymous class.
- [ ] Tôi viết đúng `equals/hashCode` và giải thích hợp đồng, vấn đề `instanceof` vs `getClass()`, vấn đề với entity JPA.
- [ ] Tôi biết vì sao `clone` và `finalize` nên tránh, và thay thế bằng copy constructor / `Cleaner` / try-with-resources.
- [ ] Tôi tự viết được class immutable đúng chuẩn với defensive copy, và giải thích đảm bảo của `final` trong JMM.
- [ ] Tôi viết ra được thứ tự khởi tạo static/instance qua kế thừa, và giải thích holder idiom, double-checked locking.
- [ ] Tôi giải thích được `protected` ở package khác và tác động của JPMS lên reflection.
- [ ] Tôi dùng enum với field, constant-specific method, EnumSet/EnumMap, và biết vì sao không lưu `ordinal`.
- [ ] Tôi thiết kế được exception hierarchy, dùng try-with-resources, hiểu suppressed exception, exception translation, `OmitStackTraceInFastThrow`, quy tắc rollback của `@Transactional`.
- [ ] Tôi đọc/ghi file đúng charset, đúng cách đóng resource, ghi atomic, chống path traversal.
- [ ] Tôi giải thích được rủi ro của Java deserialization và cách phòng thủ (`ObjectInputFilter`, record, không deserialize dữ liệu không tin cậy).
- [ ] Tôi dùng được reflection, dynamic proxy, annotation tự định nghĩa, và giải thích vì sao self-invocation bỏ qua proxy của Spring.

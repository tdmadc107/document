# Câu hỏi phỏng vấn — Module 05: JVM Internals, Memory, GC & Performance

> Giáo trình tương ứng: [Module 05 — JVM Internals, Memory, GC & Performance Tuning](../01-giao-trinh/05-jvm-memory-gc-performance.md)

**Cách dùng:** đọc câu hỏi, **tự trả lời thành tiếng** (khoảng 30–60 giây, như đang phỏng vấn thật) rồi mới mở "Đáp án". So câu trả lời của bạn với *Trả lời ngắn*, sau đó đọc *Giải thích chi tiết* và tự trả lời tiếp các *Câu hỏi nối tiếp*. Với các câu **[Tình huống]**, interviewer đánh giá **quy trình** nhiều hơn tên công cụ: hãy nói theo trình tự giả thuyết → bằng chứng → công cụ → xác nhận → sửa → phòng ngừa. Câu nào trả lời vấp thì quay lại phần giáo trình ở dòng *📖 Ôn lại*.

**Mức độ:** 🟢 Cơ bản · 🟡 Senior · 🔴 Xoáy sâu. Nhãn **[Tình huống]** là câu hỏi sự cố production, nhãn **[Đọc code]** là câu hỏi đọc code, log hoặc benchmark.

## Mục lục

1. [Kiến trúc JVM & safepoint](#nhom-a) — Q1–Q3
2. [Class loading](#nhom-b) — Q4–Q9
3. [Runtime data areas & bộ nhớ process](#nhom-c) — Q10–Q13
4. [Object layout, compressed oops, escape analysis, TLAB](#nhom-d) — Q14–Q17
5. [Bytecode](#nhom-e) — Q18–Q19
6. [JIT compiler & warm-up](#nhom-f) — Q20–Q24
7. [Nền tảng Garbage Collection](#nhom-g) — Q25–Q29
8. [Các Garbage Collector](#nhom-h) — Q30–Q36
9. [Reference types & Cleaner](#nhom-i) — Q37–Q39
10. [Memory leak & `OutOfMemoryError`](#nhom-j) — Q40–Q43
11. [JVM flags & container](#nhom-k) — Q44–Q47
12. [Công cụ chẩn đoán](#nhom-l) — Q48–Q50
13. [Đo hiệu năng & JMH](#nhom-m) — Q51–Q53
14. [Tình huống sự cố production](#nhom-n) — Q54–Q56

---

<a id="nhom-a"></a>
## 1. Kiến trúc JVM & safepoint

### Q1. 🟢 Phân biệt JVM, JRE, JDK. HotSpot liên quan gì tới "JVM"? "Metaspace", "G1", "C2" có nằm trong đặc tả JVM không?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **JVM** gồm class loader subsystem, runtime data areas và execution engine (interpreter, JIT, GC). **JRE** là JVM cộng thư viện chuẩn. **JDK** là JRE cộng công cụ phát triển (`javac`, `javap`, `jcmd`, `jfr`, `jlink`...). Từ Java 11 không còn phát hành JRE riêng; muốn có runtime tối giản thì dùng `jlink`. JVM trước hết là một **đặc tả** (JVMS). **HotSpot** là bản hiện thực phổ biến nhất, nằm trong OpenJDK và là nền của Oracle JDK, Temurin, Corretto... "Metaspace", "G1", "C2" là khái niệm **của HotSpot**, không có trong JVMS.

**Giải thích chi tiết:**
- Các hiện thực khác: OpenJ9 (Eclipse/IBM), GraalVM (HotSpot cộng Graal JIT, kèm Native Image AOT).
- Phân biệt này quan trọng khi trả lời câu hỏi chi tiết: JVMS chỉ nói có "method area", còn việc HotSpot đặt nó ở Metaspace (native memory) là chi tiết hiện thực.

**Câu hỏi nối tiếp:**
- *Một JVM process có những thread hệ thống nào?* → `VM Thread` (chạy VM operation tại safepoint), `GC Thread#n`, `G1 Conc#n`, `C1/C2 CompilerThread`, `Reference Handler`, `Finalizer`, `Signal Dispatcher`, `Common-Cleaner`.

**⚠️ Câu trả lời gây điểm trừ:**
- "JDK 17 vẫn có JRE riêng để cài trên server."
- Coi "PermGen/Metaspace" là một phần của đặc tả Java.

**📖 Ôn lại:** [Phần 1 — Kiến trúc tổng quan của JVM](../01-giao-trinh/05-jvm-memory-gc-performance.md#p1)

</details>

### Q2. 🟢 Java là ngôn ngữ thông dịch hay biên dịch? Java có chậm hơn C++ không?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Cả hai. `javac` biên dịch source thành bytecode. Khi chạy, JVM **bắt đầu bằng interpreter** và đồng thời profiling, rồi **JIT** biên dịch code nóng thành mã máy native, có tối ưu theo profile thực tế. Ở *steady state*, code đã JIT chạy rất gần C/C++, có khi còn lợi thế nhờ tối ưu speculative (ví dụ inline một call đa hình mà thực tế chỉ có một implementation được dùng). Chi phí thật của Java nằm ở chỗ khác: startup và warm-up, footprint bộ nhớ (object header, pointer), và GC pause.

**Giải thích chi tiết:**
- Các hướng giải quyết startup: CDS/AppCDS, Project Leyden AOT cache (JEP 483, JDK 24), CRaC, GraalVM Native Image.
- Các hướng giải quyết latency: ZGC, Shenandoah.

**Câu hỏi nối tiếp:**
- *"Write once, run anywhere" đến từ đâu?* → Bytecode là tập lệnh cho máy ảo dạng stack-based, độc lập nền tảng. JVM của từng nền tảng dịch bytecode sang mã máy tương ứng.

**⚠️ Câu trả lời gây điểm trừ:**
- "Java là ngôn ngữ thông dịch nên chậm."

**📖 Ôn lại:** [Phần 1.2 — Bên dưới nắp capo](../01-giao-trinh/05-jvm-memory-gc-performance.md#p1)

</details>

### Q3. 🔴 Safepoint là gì? Time-to-safepoint (TTSP) là gì và vì sao nó có thể gây pause dài dù GC rất nhanh?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Safepoint là trạng thái mà mọi Java thread dừng ở vị trí "an toàn", nơi JVM biết chính xác reference nằm ở đâu trên stack và trong register. Các thao tác stop-the-world đều phải qua safepoint: nhiều pha GC, deoptimization, lấy thread dump, redefine class. **TTSP** là thời gian từ lúc JVM yêu cầu dừng tới lúc thread **cuối cùng** dừng. Thread chạy một counted loop dài (vòng lặp đếm bằng `int`) mà JIT đã bỏ safepoint poll có thể làm **cả JVM** phải chờ nó. Kết quả là pause tổng rất dài, trong khi GC log báo bản thân GC chỉ mất vài ms.

**Giải thích chi tiết:**
- Kiểm tra bằng `-Xlog:safepoint` (JDK 17+). Xem phần "Reaching safepoint".
- JDK hiện đại dùng **loop strip mining** (`-XX:+UseCountedLoopSafepoints`, `LoopStripMiningIter`, mặc định 1000) để chèn poll sau mỗi "strip", nhờ vậy TTSP thường nhỏ.
- Profiler dựa trên safepoint (`Thread.getAllStackTraces`, một số APM, VisualVM sampler) bị **safepoint bias**: chúng chỉ lấy mẫu được tại safepoint nên báo sai hotspot.

**Câu hỏi nối tiếp:**
- *Ngoài GC và TTSP còn gì gây "pause"?* → CPU throttling của cgroup (CFS quota), swap, transparent huge pages (xem Q55).

**⚠️ Câu trả lời gây điểm trừ:**
- "Pause của ứng dụng = thời gian GC trong GC log."

**📖 Ôn lại:** [Phần 1.2 — Safepoint](../01-giao-trinh/05-jvm-memory-gc-performance.md#p1)

</details>

---

<a id="nhom-b"></a>
## 2. Class loading

### Q4. 🟢 Một class đi qua những giai đoạn nào trước khi dùng được? Preparation và initialization khác nhau thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Có ba giai đoạn. **Loading**: tìm dữ liệu nhị phân, tạo cấu trúc nội bộ trong Metaspace và `Class` object trên heap. **Linking** gồm *verification* (kiểm tra bytecode hợp lệ, an toàn kiểu, lỗi thì ném `VerifyError`), *preparation* (cấp phát biến `static` và gán **giá trị mặc định** 0/null/false) và *resolution* (đổi symbolic reference thành direct reference; HotSpot làm việc này lazy). **Initialization**: chạy `<clinit>`, tức các phép gán static và khối `static {}` theo thứ tự trong source. Preparation chỉ gán giá trị mặc định. Initialization mới chạy code khởi tạo.

**Giải thích chi tiết:**
- Initialization chỉ xảy ra ở **lần sử dụng chủ động đầu tiên** (active use): `new`, gọi static method, đọc/ghi static field (trừ hằng compile-time), reflection `Class.forName(name)`, khởi tạo subclass (lớp cha được init trước), class chứa `main`.
- JVM bảo đảm việc khởi tạo class là **thread-safe** (JVMS §5.5, có initialization lock). Đây là nền của holder idiom.

**Câu hỏi nối tiếp:**
- *Thứ tự khi `new Child()` lần đầu?* → Static của `Parent`, rồi static của `Child`, rồi instance init cộng constructor của `Parent`, rồi của `Child`. Lần thứ hai bỏ qua toàn bộ phần static.

**⚠️ Câu trả lời gây điểm trừ:**
- "Class được nạp và khởi tạo hết khi JVM khởi động."

**📖 Ôn lại:** [Phần 2.1 — Loading, linking, initialization](../01-giao-trinh/05-jvm-memory-gc-performance.md#p2)

</details>

### Q5. 🟡 **[Đọc code]** Dòng nào in ra `"Config <clinit> chạy"`?

```java
static class Config {
    static final int CONST = 42;
    static final Integer BOXED = 7;
    static { System.out.println("Config <clinit> chạy"); }
}
public static void main(String[] args) throws Exception {
    System.out.println(Config.CONST);                                         // (1)
    Config[] arr = new Config[3];                                             // (2)
    Class<?> c = Config.class;                                                // (3)
    ClassLoader.getSystemClassLoader().loadClass("InitOrder$Config");         // (4)
    System.out.println(Config.BOXED);                                         // (5)
}
```

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Chỉ dòng **(5)** kích hoạt `<clinit>`. (1) `CONST` là **hằng compile-time** (primitive hoặc String có initializer là constant expression) nên được inline vào class gọi, không cần đụng tới `Config`. (2) Tạo mảng `Config[]` không init `Config`. (3) `Config.class` không init. (4) `loadClass` chỉ load, không init (khác với `Class.forName(name)` mặc định có init). (5) `Integer BOXED` **không phải** hằng compile-time vì có autoboxing, nên đọc nó là active use.

**Giải thích chi tiết:**
- Hệ quả ngược của inline hằng: đổi giá trị hằng trong thư viện mà **không compile lại** code gọi thì code gọi vẫn dùng giá trị cũ. Lỗi này hay gặp khi nâng version jar mà không rebuild.

**Câu hỏi nối tiếp:**
- *`static final String NAME = "cfg"` thì sao?* → Là hằng compile-time, được inline. Còn `static final String X = compute();` thì không.

**⚠️ Câu trả lời gây điểm trừ:**
- "Mọi `static final` đều là hằng và được inline."

**📖 Ôn lại:** [Phần 2.1 — ví dụ InitOrder](../01-giao-trinh/05-jvm-memory-gc-performance.md#p2)

</details>

### Q6. 🟢 Parent delegation model là gì? Java 9+ có những class loader dựng sẵn nào, khác Java 8 ra sao?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `loadClass` mặc định làm ba bước: (1) kiểm tra class đã nạp chưa; (2) nếu chưa thì **hỏi parent trước**; (3) parent không tìm được thì mới tự `findClass`. Mô hình này giúp không ai "giả mạo" được `java.lang.String` và class lõi chỉ được nạp một lần. Java 9+ có ba loader: **Bootstrap** (viết bằng C++, nạp `java.base`, `getClassLoader()` trả về `null`), **Platform** (một số module Java SE/JDK, thay cho Extension loader của Java 8 vốn nạp từ `jre/lib/ext`), và **Application/System** (class path và module path).

**Giải thích chi tiết:**
- Viết custom loader mà vẫn giữ delegation thì override **`findClass`**, không override `loadClass`.
- Tomcat `WebappClassLoader` **đảo** thứ tự: tìm trong `WEB-INF/classes` và `WEB-INF/lib` trước, trừ các class `java.*`, để mỗi webapp dùng được phiên bản thư viện riêng.
- **Thread context class loader (TCCL)** cho phép code ở loader cha (`ServiceLoader`, JDBC `DriverManager`, JNDI) nạp class ở loader con.

**Câu hỏi nối tiếp:**
- *Khi nào cần custom class loader?* → Hệ thống plugin, hot reload (Spring DevTools `RestartClassLoader`), cô lập dependency, nạp class sinh động hoặc được mã hoá.

**⚠️ Câu trả lời gây điểm trừ:**
- Nói "Extension ClassLoader" khi được hỏi về Java 17.

**📖 Ôn lại:** [Phần 2.2–2.3 — Parent delegation & custom class loader](../01-giao-trinh/05-jvm-memory-gc-performance.md#p2)

</details>

### Q7. 🟡 **[Tình huống]** Log báo `ClassCastException: com.acme.User cannot be cast to com.acme.User`. Vì sao một class lại không cast được sang chính nó?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Định danh của một class là cặp **(tên đầy đủ, defining class loader)**. Hai loader khác nhau cùng nạp file `com.acme.User` thì sinh ra **hai class khác nhau**. Object tạo từ class này không phải instance của class kia. Lỗi này hay gặp với app server (thư viện vừa nằm trong webapp vừa nằm trong `lib` dùng chung), hot reload (Spring DevTools: object được tạo trước khi restart, hoặc object nằm trong cache/session được deserialize bởi loader cũ), và hệ thống plugin.

**Giải thích chi tiết:**
- Cách điều tra: in `obj.getClass().getClassLoader()` và `User.class.getClassLoader()`; bật `-Xlog:class+load` để xem class được nạp từ đâu.
- Cách sửa: đặt interface hoặc DTO dùng chung ở loader **cha**, để plugin loader dùng parent đó; tránh trùng jar giữa các tầng loader.

**Câu hỏi nối tiếp:**
- *Fat jar có hai phiên bản cùng một thư viện thì sao?* → Class nào được nạp phụ thuộc thứ tự classpath, và hay gây `NoSuchMethodError` ở runtime. Dùng `mvn dependency:tree` để truy.

**⚠️ Câu trả lời gây điểm trừ:**
- "Do khác version JDK" hay "do serialVersionUID".

**📖 Ôn lại:** [Phần 2.2 — Định danh của class](../01-giao-trinh/05-jvm-memory-gc-performance.md#p2)

</details>

### Q8. 🟢 `ClassNotFoundException` và `NoClassDefFoundError` khác nhau thế nào? Vì sao có khi gặp `NoClassDefFoundError: Could not initialize class X` dù class rõ ràng nằm trên classpath?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `ClassNotFoundException` là **checked exception**, ném ra khi **nạp động bằng tên** (`Class.forName`, `loadClass`) mà không tìm thấy class. `NoClassDefFoundError` là **Error**: class có mặt lúc compile nhưng lúc runtime JVM không tìm thấy hoặc **không khởi tạo được**. Nếu `<clinit>` ném exception, lần đầu sẽ nhận `ExceptionInInitializerError`, và **mọi lần sau** nhận `NoClassDefFoundError: Could not initialize class X`. Vì vậy phải tìm trong log **lần lỗi đầu tiên** để thấy nguyên nhân gốc, ví dụ static block đọc config thất bại.

**Giải thích chi tiết:**
- Một biến thể nguy hiểm khác là **deadlock khi khởi tạo class**: hai class có static initializer phụ thuộc vòng nhau và được init đồng thời từ hai thread. Thread dump khi đó hiện `RUNNABLE` nhưng đứng yên ở `<clinit>`.

**Câu hỏi nối tiếp:**
- *Làm sao tránh static initializer gây lỗi khó chẩn đoán?* → Không làm I/O hay đọc config phức tạp trong `static {}`. Chuyển sang khởi tạo tường minh hoặc lazy, có xử lý lỗi rõ ràng.

**⚠️ Câu trả lời gây điểm trừ:**
- "Hai lỗi này giống nhau, chỉ khác tên."

**📖 Ôn lại:** [Phần 2 — Góc nhìn Senior](../01-giao-trinh/05-jvm-memory-gc-performance.md#p2)

</details>

### Q9. 🔴 **[Tình huống]** Ứng dụng trên Tomcat sau khoảng 8 lần redeploy thì gặp `OutOfMemoryError: Metaspace`. Nguyên nhân thường là gì? Bạn chứng minh và sửa thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Đây là **classloader leak**. Một class chỉ được unload khi **class loader định nghĩa nó** không còn reachable. Chỉ cần **một** reference từ bên ngoài trỏ vào một object hoặc class của webapp cũ là toàn bộ class của webapp đó (hàng nghìn class, hàng chục tới hàng trăm MB Metaspace) bị giữ lại. Nguồn gây leak điển hình: `ThreadLocal` trên thread pool dùng chung giữ value là object của webapp; thread do webapp tạo (timer, scheduler) không được dừng, và TCCL của thread đó trỏ tới webapp loader; JDBC driver không được deregister khỏi `DriverManager`; cache static ở thư viện cấp cha; shutdown hook. Cách chứng minh: `jcmd <pid> VM.classloader_stats` cho thấy số loader tăng sau mỗi lần redeploy; lấy heap dump, mở MAT, tìm các instance `WebappClassLoader` cũ rồi chạy **Path to GC Roots** (loại trừ weak/soft) để thấy chuỗi giữ.

**Giải thích chi tiết:**
- Chuỗi giữ thường gặp: `Thread (pool) → threadLocals → Entry.value → PluginObject → Class → WebappClassLoader`.
- Cách sửa: `remove()` ThreadLocal trong `finally`; shutdown executor của webapp trong `contextDestroyed`; deregister driver; đặt `-XX:MaxMetaspaceSize` để leak lộ ra thành OOM rõ ràng thay vì ăn hết RAM container.
- Tomcat in log cảnh báo kiểu "appears to have started a thread named [...] but has failed to stop it" và "created a ThreadLocal ... but failed to remove it".

**Câu hỏi nối tiếp:**
- *Spring Boot fat jar chạy độc lập có bị vấn đề này không?* → Ít bị hơn vì không redeploy trong cùng JVM. Tuy nhiên vẫn gặp với hot reload (DevTools), hệ thống plugin, hoặc code sinh class động liên tục (Groovy script, proxy, CGLIB không được cache).

**⚠️ Câu trả lời gây điểm trừ:**
- "Tăng `MaxMetaspaceSize` là xong."

**📖 Ôn lại:** [Phần 2.4 — Class unloading & classloader leak](../01-giao-trinh/05-jvm-memory-gc-performance.md#p2)

</details>

---

<a id="nhom-c"></a>
## 3. Runtime data areas & bộ nhớ process

### Q10. 🟢 Kể các runtime data area của JVM. Vùng nào chia sẻ giữa các thread, vùng nào riêng? Mỗi vùng cạn thì ném lỗi gì?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Các vùng **chia sẻ**: **Heap** (mọi object, mảng, String pool, `Class` mirror) cạn thì ném `OOM: Java heap space`. **Metaspace** (metadata của class, nằm ở native memory) cạn thì ném `OOM: Metaspace` hoặc `Compressed class space`. **Code cache** (mã do JIT sinh) đầy thì JVM in cảnh báo "CodeCache is full. Compiler has been disabled" và ứng dụng chậm hẳn. Các vùng **riêng từng thread**: **JVM stack** (stack frame) sâu quá thì ném `StackOverflowError`, không tạo được stack mới thì ném `OOM: unable to create native thread`; **PC register**; **native method stack** (HotSpot gộp chung với Java stack). Ngoài ra còn direct buffer và các vùng native khác, cạn thì ném `OOM: Direct buffer memory` hoặc process bị container kill.

**Giải thích chi tiết:**
- Mỗi stack frame gồm **local variable array** (`this` ở slot 0, `long`/`double` chiếm 2 slot), **operand stack**, và frame data.
- Stack của virtual thread được lưu trên **heap** dưới dạng stack chunk khi unmount.

**Câu hỏi nối tiếp:**
- *"Primitive nằm trên stack, object nằm trên heap" đúng không?* → Chỉ đúng một nửa. Primitive là field của object thì nằm trên heap cùng object. Biến local kiểu reference nằm trên stack nhưng object nó trỏ tới nằm trên heap (trừ khi escape analysis loại bỏ việc cấp phát).

**⚠️ Câu trả lời gây điểm trừ:**
- Không biết Metaspace nằm ngoài heap.

**📖 Ôn lại:** [Phần 3.1 — Runtime data areas](../01-giao-trinh/05-jvm-memory-gc-performance.md#p3)

</details>

### Q11. 🟡 Vì sao Java 8 thay PermGen bằng Metaspace? Từ Java 8, giá trị của static field và String pool nằm ở đâu?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** PermGen là một phần của heap với kích thước **cố định** (`-XX:MaxPermSize`, mặc định nhỏ), nên `OOM: PermGen space` rất phổ biến khi ứng dụng dùng nhiều class, proxy hay redeploy. Metaspace (JEP 122) chuyển metadata class ra **native memory**, mặc định **không giới hạn** và tăng dần. GC dọn Metaspace khi chạm ngưỡng high-water mark `MetaspaceSize`. String pool đã chuyển ra heap từ **Java 7**. Từ Java 8, giá trị của static field nằm trong `java.lang.Class` mirror **trên heap**, không nằm trong Metaspace.

**Giải thích chi tiết:**
- Khi bật compressed class pointers, metadata `Klass` nằm trong vùng con **Compressed Class Space** (mặc định 1GB), và vùng này có thể cạn riêng.
- JDK 16 (JEP 387, Elastic Metaspace) cải thiện việc trả bộ nhớ Metaspace về cho OS.
- Metaspace "không giới hạn" mặc định là một rủi ro trong container. Nên đặt `MaxMetaspaceSize` để leak lộ ra sớm.

**Câu hỏi nối tiếp:**
- *Copy flag `-XX:MaxPermSize` sang JDK 17 thì sao?* → Flag đã hết hạn. Tuỳ phiên bản mà JVM bỏ qua kèm cảnh báo hoặc **từ chối khởi động**.

**⚠️ Câu trả lời gây điểm trừ:**
- "Static variable nằm trong Metaspace."

**📖 Ôn lại:** [Phần 3.3 — PermGen → Metaspace](../01-giao-trinh/05-jvm-memory-gc-performance.md#p3)

</details>

### Q12. 🔴 **[Tình huống]** Pod Kubernetes có `limits.memory: 2Gi`, JVM chạy với `-Xmx2g`. Pod liên tục bị restart với exit code 137 (`OOMKilled`) nhưng log ứng dụng không có `OutOfMemoryError` nào. Giải thích và đề xuất cách sửa.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Exit 137 nghĩa là process bị **kernel OOM killer của cgroup** giết bằng SIGKILL khi **RSS** vượt limit. Đây không phải Java OOM, nên không có exception và không có heap dump. RSS của process Java bằng heap **cộng** Metaspace, thread stacks (số thread × stack thực dùng), code cache (tới 240MB), cấu trúc dữ liệu của GC (remembered set của G1 có thể vài phần trăm tới hơn 10% heap), direct buffer (Netty, NIO; mặc định trần ≈ `-Xmx`), malloc arena, JNI... Đặt `-Xmx` bằng limit chắc chắn vượt. Cách sửa: heap khoảng 50–75% limit (`-XX:MaxRAMPercentage=70`); đặt trần cho `MaxMetaspaceSize` và `MaxDirectMemorySize`; đo thực tế bằng **NMT** (`-XX:NativeMemoryTracking=summary` rồi `jcmd <pid> VM.native_memory summary`); đặt `requests.memory = limits.memory`.

**Giải thích chi tiết:**
- Công thức tham khảo: `limit ≥ Xmx + MaxMetaspaceSize + MaxDirectMemorySize + threads × ~1MB + ~250MB`.
- Nếu RSS tăng mà heap ổn định, đó là leak native (xem Q56).
- Phân biệt hai loại: **JVM OOM** là exception có message, có thể dump heap. **Kernel OOM** không có dấu vết trong log Java; chỉ thấy `OOMKilled: true` trong `kubectl describe pod` hoặc dmesg.

**Câu hỏi nối tiếp:**
- *Vì sao 1.000 thread với `-Xss1m` không chắc tốn 1GB RSS?* → Stack chỉ *reserve* 1MB địa chỉ ảo, còn *commit* phần đã chạm tới. Tuy vậy số thread vẫn bị giới hạn bởi `pids.max` và `ulimit -u`.

**⚠️ Câu trả lời gây điểm trừ:**
- "Tăng `-Xmx` lên" hoặc "do memory leak trong heap" khi log không có Java OOM.

**📖 Ôn lại:** [Phần 3 — Góc nhìn Senior (RSS)](../01-giao-trinh/05-jvm-memory-gc-performance.md#p3) · [Phần 11.3 — JVM trong container](../01-giao-trinh/05-jvm-memory-gc-performance.md#p11)

</details>

### Q13. 🟡 `StackOverflowError` và `OutOfMemoryError: unable to create native thread` khác nhau thế nào? Code cache đầy có triệu chứng gì?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `StackOverflowError` xảy ra khi **một** thread dùng hết stack của nó, thường do đệ quy quá sâu. Có thể tăng `-Xss` hoặc sửa thuật toán. `unable to create native thread` xảy ra khi **không tạo được OS thread mới**: chạm `ulimit -u`, `pids.max` của cgroup, `kernel.threads-max`, hoặc hết bộ nhớ ảo hay native. Lỗi này hầu như luôn là **thread leak**, ví dụ tạo executor mới cho mỗi request mà không shutdown, hoặc dùng `newCachedThreadPool` khi có burst. Khi **code cache đầy**, JVM in cảnh báo "CodeCache is full. Compiler has been disabled". JIT ngừng biên dịch, code mới chạy bằng interpreter, CPU tăng và latency tăng dần mà không rõ lý do. Theo dõi bằng `jcmd <pid> Compiler.codecache` và tăng `-XX:ReservedCodeCacheSize` nếu cần.

**Giải thích chi tiết:**
- Độ sâu đệ quy tối đa không cố định, vì frame interpreted lớn hơn frame đã JIT và inlining làm giảm số frame.
- Cách đếm thread: `grep Threads /proc/<pid>/status`, metric `jvm_threads_live_threads`.
- Từ JDK 9, code cache được phân đoạn: non-method, profiled, non-profiled.

**Câu hỏi nối tiếp:**
- *Giảm `-Xss` có lợi gì?* → Khi có hàng nghìn platform thread với stack nông, giảm `-Xss` tiết kiệm địa chỉ ảo và commit. Rủi ro là thread có stack sâu (framework, đệ quy) bị `StackOverflowError`.

**⚠️ Câu trả lời gây điểm trừ:**
- "unable to create native thread nghĩa là heap đầy, tăng `-Xmx`." Tăng heap còn làm giảm phần bộ nhớ native còn lại.

**📖 Ôn lại:** [Phần 3.4–3.5 — Stack frame & Code cache](../01-giao-trinh/05-jvm-memory-gc-performance.md#p3)

</details>

---

<a id="nhom-d"></a>
## 4. Object layout, compressed oops, escape analysis, TLAB

### Q14. 🟡 Trên HotSpot 64-bit, `new Object()`, một `Integer`, một `Long` và một entry của `HashMap<Integer,Integer>` tốn bao nhiêu byte? Điều này ảnh hưởng gì tới thiết kế?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Mặc định có compressed oops và compressed class pointers. Header gồm **mark word 8 byte** cộng **klass pointer 4 byte**, và object được align về bội số 8. `new Object()` = 16 byte (12 header + 4 padding). `Integer` = 16 byte. `Long` = 24 byte. Một entry `HashMap<Integer,Integer>` vào khoảng `Node` 32 byte + key 16 + value 16 + slot 4 trong mảng table ≈ **68 byte để chứa 8 byte dữ liệu thật**. Hệ quả: `List<Integer>` một triệu phần tử tốn khoảng 20MB, trong khi `int[]` cùng kích thước chỉ khoảng 4MB và thân thiện cache CPU hơn nhiều.

**Giải thích chi tiết:**
- Mảng có thêm 4 byte `length`: `new int[10]` = 16 + 40 = 56 byte.
- `String "hello"` (Java 9+, compact strings, LATIN1) tốn khoảng 48 byte. Chuỗi tiếng Việt có dấu nằm ngoài Latin-1 nên dùng coder UTF16, tốn gấp đôi phần dữ liệu.
- Với dữ liệu lớn, cân nhắc primitive collections (fastutil, Eclipse Collections), mảng song song, hoặc chờ Project Valhalla. Đo bằng JOL `GraphLayout` hoặc retained size trong MAT.
- Compact Object Headers (JEP 450 ở JDK 24 dạng experimental, JEP 519 ở JDK 25 dạng product) giảm header xuống 8 byte.

**Câu hỏi nối tiếp:**
- *Ước lượng một cache 1 triệu entry `String → DTO` thế nào?* → Cộng đủ header, String và `byte[]` của nó, node của map, DTO cùng các field. Thực tế thường gấp **3–6 lần** "kích thước dữ liệu". Hãy đo, đừng nhân nhẩm.

**⚠️ Câu trả lời gây điểm trừ:**
- "Integer tốn 4 byte."

**📖 Ôn lại:** [Phần 4.1 — Object tốn bao nhiêu byte](../01-giao-trinh/05-jvm-memory-gc-performance.md#p4)

</details>

### Q15. 🟡 Compressed oops là gì? Vì sao heap 32–40GB có thể chứa **ít** object hơn heap 31GB?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Trên 64-bit, mỗi pointer tốn 8 byte. HotSpot nén reference thành **32-bit offset chia 8** (vì object align 8 byte): địa chỉ thật = `base + (oop << 3)`, đánh địa chỉ được 2³² × 8 = **32GB**. Khi heap vượt khoảng 32GB, compressed oops **tự tắt** và mọi reference thành 8 byte. Lượng bộ nhớ tăng thêm có thể lớn hơn phần heap vừa thêm, nên heap 32–40GB có khi chứa ít object hơn heap 31GB. Nếu thực sự cần heap lớn, hãy nhảy hẳn lên ≥ 48GB, hoặc dùng `-XX:ObjectAlignmentInBytes=16` (đánh địa chỉ được 64GB nhưng tốn thêm padding).

**Giải thích chi tiết:**
- Kiểm tra: `java -Xmx31g -XX:+PrintFlagsFinal -version | grep UseCompressedOops`, hoặc `-Xlog:gc+heap+coops=debug`.
- Có các chế độ zero-based: heap < 4GB không cần shift; < 32GB chỉ shift, không cộng base. Chế độ này nhanh hơn chế độ có base.
- **ZGC không hỗ trợ compressed oops** vì dùng bit của pointer để tô màu (colored pointers), nên footprint cao hơn khi heap nhỏ.

**Câu hỏi nối tiếp:**
- *1 tỉ reference chênh lệch bao nhiêu?* → Khoảng 4GB chỉ riêng phần pointer (8 − 4 byte mỗi reference).

**⚠️ Câu trả lời gây điểm trừ:**
- "Heap càng lớn càng tốt, cứ đặt 40GB."

**📖 Ôn lại:** [Phần 4.2 — Compressed oops](../01-giao-trinh/05-jvm-memory-gc-performance.md#p4)

</details>

### Q16. 🔴 "Java có cấp phát object trên stack không?" Giải thích escape analysis, scalar replacement và vì sao không nên thiết kế dựa vào nó.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** HotSpot **không thực sự cấp phát trên stack**. C2 làm **escape analysis**. Với object **NoEscape** (chỉ dùng cục bộ), C2 áp dụng **scalar replacement**: bỏ hẳn việc cấp phát, thay các field bằng biến local hoặc register. Với object **ArgEscape** (được truyền vào method khác nhưng không thoát khỏi thread), C2 có thể **lock elision**. Object **GlobalEscape** (gán vào field hay static, hoặc return ra ngoài) được cấp phát bình thường. Tối ưu này **mong manh**: nó phụ thuộc vào inlining (method quá lớn hoặc call site megamorphic thì không inline được, và object bị coi là thoát qua tham số), phụ thuộc control flow (object được merge từ hai nhánh), và chỉ áp dụng cho code đã được C2 biên dịch. Vì vậy hãy đo allocation bằng JFR hoặc async-profiler `-e alloc`, đừng giả định.

**Giải thích chi tiết:**

```java
record Vec(double x, double y) { Vec plus(Vec o) { return new Vec(x + o.x, y + o.y); } }
// Sau khi C2 inline plus(): new Vec(i,i).plus(new Vec(1,1)) → thường 0 allocation
```
- Chạy lại với `-XX:-DoEscapeAnalysis` thì lượng cấp phát tăng vọt. Kiểm tra lý do không inline bằng `-XX:+UnlockDiagnosticVMOptions -XX:+PrintInlining`.

**Câu hỏi nối tiếp:**
- *Vì sao method nhỏ, call site đơn hình lại "thân thiện" với JIT?* → Vì chúng inline được, và inlining mở đường cho escape analysis, constant folding, loại bỏ null check.

**⚠️ Câu trả lời gây điểm trừ:**
- "Có, object nhỏ trong method luôn được cấp phát trên stack."

**📖 Ôn lại:** [Phần 4.3 — Escape analysis](../01-giao-trinh/05-jvm-memory-gc-performance.md#p4)

</details>

### Q17. 🟢 Cấp phát một object mới trong Java tốn bao nhiêu chi phí? TLAB là gì?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Rất rẻ. Mỗi thread có một **TLAB** (Thread-Local Allocation Buffer), là một vùng riêng trong Eden. Cấp phát chỉ là **bump-the-pointer** (tăng con trỏ), không cần lock, thường mất vài chục nanosecond, ngang malloc tốt nhất hoặc nhanh hơn. Chi phí thật nằm ở **GC sau đó** và ở việc khởi tạo field. Object chết trẻ gần như "miễn phí", vì young GC chỉ tốn công cho object **còn sống**. Object sống vừa đủ lâu để bị promote rồi mới chết thì đắt.

**Giải thích chi tiết:**
- TLAB hết thì thread xin TLAB mới. Object quá lớn được cấp phát ngoài TLAB, và với G1, object ≥ 50% region là humongous.
- Allocation rate (MB/s) quyết định tần suất young GC. Giảm allocation (bớt object tạm, tránh boxing, tránh `String.split`/regex trên đường nóng) là đòn bẩy mạnh nhất để giảm GC.

**Câu hỏi nối tiếp:**
- *Có nên tự làm object pool để "tránh GC" không?* → Hầu như không, trừ object thật sự đắt như buffer lớn hay connection. Pool giữ object sống lâu, tăng live set và gây promotion, lại phát sinh bug đồng bộ và reset.

**⚠️ Câu trả lời gây điểm trừ:**
- "`new` rất đắt nên phải tái sử dụng object bằng mọi giá."

**📖 Ôn lại:** [Phần 3.2 — Heap, TLAB](../01-giao-trinh/05-jvm-memory-gc-performance.md#p3) · [Phần 7.5](../01-giao-trinh/05-jvm-memory-gc-performance.md#p7)

</details>

---

<a id="nhom-e"></a>
## 5. Bytecode

### Q18. 🟡 **[Đọc code]** `int i = 5; i = i++;` thì `i` bằng bao nhiêu? Vì sao `count += delta` trên field không atomic? Chứng minh bằng bytecode.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `i` vẫn bằng **5**. Bytecode là `iload_1` (đẩy giá trị cũ 5 lên operand stack), `iinc 1,1` (biến local thành 6), rồi `istore_1` (ghi đè lại bằng 5). `count += delta` trên field biên dịch thành `aload_0; dup; getfield; iload_1; iadd; putfield`, tức là **read–modify–write ba bước** riêng rẽ. Thread khác có thể chen vào giữa `getfield` và `putfield`, nên không atomic.

**Giải thích chi tiết:**
- `iinc` chỉ dùng được cho **biến local**. Field phải đi qua `getfield`/`putfield`.
- Đọc bytecode bằng `javap -c -p -v`. Không cần thuộc opcode, nhưng phải đọc được để trả lời những câu như thế này.

**Câu hỏi nối tiếp:**
- *`switch` trên String được biên dịch thế nào?* → Dùng `hashCode()` cộng `lookupswitch`, rồi `equals` để xử lý va chạm hash. Pattern switch (Java 21) dùng `invokedynamic` `typeSwitch` (`SwitchBootstraps`).

**⚠️ Câu trả lời gây điểm trừ:**
- "`i` bằng 6."
- "`+=` là một lệnh nên atomic."

**📖 Ôn lại:** [Phần 5.2 — Đọc bytecode bằng javap](../01-giao-trinh/05-jvm-memory-gc-performance.md#p5)

</details>

### Q19. 🟡 **[Đọc code]** Hai method sau trả về gì? Lambda được biên dịch khác anonymous class thế nào?

```java
int f() { int x = 1; try { return x; } finally { x = 2; } }
int g() { try { throw new RuntimeException("boom"); } finally { return 2; } }
```

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `f()` trả về **1**. Giá trị trả về được lưu vào một biến local tạm **trước khi** chạy `finally`, sau đó `ireturn` giá trị tạm đó. `g()` trả về **2**, và exception "boom" **bị nuốt mất**, vì `return` trong `finally` bỏ qua đường `athrow` (javac cảnh báo với `-Xlint:finally`). Compiler **nhân bản** khối `finally` vào mọi đường thoát. Về lambda: lambda **không** sinh file `.class` ẩn danh như anonymous class. Thân lambda được biên dịch thành một private method tổng hợp (`lambda$task$0`), còn tại chỗ tạo lambda là lệnh `invokedynamic`. Lúc runtime, `LambdaMetafactory` sinh class (hidden class từ Java 15). Lambda không capture biến nào thì được cache thành singleton.

**Giải thích chi tiết:**
- Nối chuỗi `"Hi " + name` trên Java 9+ dùng `invokedynamic` cùng `StringConcatFactory` (JEP 280). Nối chuỗi trong **vòng lặp** vẫn nên dùng `StringBuilder`.
- Record `toString/equals/hashCode` cũng được sinh qua `invokedynamic` (`ObjectMethods`).

**Câu hỏi nối tiếp:**
- *Số lệnh bytecode ít hơn có nghĩa là chạy nhanh hơn không?* → Không. Sau khi JIT, số lệnh bytecode gần như không liên quan tới hiệu năng. Hãy đo bằng JMH.

**⚠️ Câu trả lời gây điểm trừ:**
- "`f()` trả về 2."
- "Lambda chỉ là anonymous class viết gọn."

**📖 Ôn lại:** [Phần 5 — Bytecode & javap](../01-giao-trinh/05-jvm-memory-gc-performance.md#p5)

</details>

---

<a id="nhom-f"></a>
## 6. JIT compiler & warm-up

### Q20. 🟢 Tiered compilation là gì? Mô tả các level và đường đi phổ biến của một method "nóng".

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** HotSpot có ba cách thực thi một method. **Interpreter** khởi động ngay, chậm, và thu thập profile. **C1** biên dịch nhanh, tối ưu vừa phải. **C2** biên dịch chậm nhưng tối ưu mạnh theo profile. Tiered compilation (mặc định từ Java 8) có năm level: 0 là interpreter, 1 là C1 không profiling (cho method tầm thường), 2 là C1 profiling giới hạn, 3 là C1 profiling đầy đủ, 4 là C2. Đường đi phổ biến là **0 → 3 → 4**. Ngưỡng tham khảo: khoảng 200 lần gọi thì lên level 3, khoảng 5000 lần gọi thì lên level 4. Việc biên dịch chạy nền trên các `CompilerThread`; trong lúc chờ, ứng dụng vẫn chạy bản cũ.

**Giải thích chi tiết:**
- **OSR (On-Stack Replacement)**: method chỉ được gọi một lần nhưng có vòng lặp rất dài thì được biên dịch và **thay frame đang chạy** ngay giữa vòng lặp. Trong `-XX:+PrintCompilation`, OSR được đánh dấu `%`.
- Ứng dụng CLI ngắn hạn có thể dùng `-XX:TieredStopAtLevel=1` (chỉ C1) để giảm thời gian khởi động.

**Câu hỏi nối tiếp:**
- *Đọc dòng `812 245 % 4 JitDemo::total @ 9 (40 bytes)` thế nào?* → Thời điểm 812ms, compile id 245, `%` là OSR, level 4 (C2), `@ 9` là bci của vòng lặp, method dài 40 byte bytecode.

**⚠️ Câu trả lời gây điểm trừ:**
- "JIT biên dịch toàn bộ chương trình lúc khởi động."

**📖 Ôn lại:** [Phần 6.1 — Tiered compilation](../01-giao-trinh/05-jvm-memory-gc-performance.md#p6)

</details>

### Q21. 🟡 Inlining quan trọng thế nào? Monomorphic, bimorphic, megamorphic call site là gì và ảnh hưởng tới hiệu năng ra sao?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Inlining là "mẹ của mọi tối ưu": thay lời gọi method bằng thân method, nhờ đó mở đường cho escape analysis, constant folding, loại bỏ null check và range check. Có giới hạn kích thước: method nóng được inline nếu bytecode ≤ `FreqInlineSize` (325 byte trên x64); method không nóng ≤ `MaxInlineSize` (35 byte). Tại mỗi call site, JIT ghi nhận các kiểu thực tế. **Monomorphic** (1 kiểu) và **bimorphic** (2 kiểu) được inline kèm type guard. **Megamorphic** (≥ 3 kiểu) phải gọi qua vtable/itable và **không inline**, nên chậm hơn rõ rệt trên đường nóng. **Class Hierarchy Analysis (CHA)** devirtualize method chỉ có một implementation đang được nạp.

**Giải thích chi tiết:**
- Hệ quả thực tế: method nhỏ, tập trung không chỉ "clean" mà còn thân thiện với JIT. `final` method **không** nhanh hơn rõ rệt, vì CHA đã devirtualize sẵn.
- **Intrinsics**: một số method JDK được thay bằng mã máy viết tay hoặc lệnh CPU đặc biệt, ví dụ `System.arraycopy`, `String.equals`, `Integer.bitCount` (POPCNT), CRC32, AES, CAS.

**Câu hỏi nối tiếp:**
- *Kiểm tra JIT có inline hay không bằng cách nào?* → `-XX:+UnlockDiagnosticVMOptions -XX:+PrintInlining`, rồi tìm các dòng "too big", "megamorphic", "not inline". Hoặc dùng JITWatch.

**⚠️ Câu trả lời gây điểm trừ:**
- "Thêm `final` cho mọi method để tăng tốc."

**📖 Ôn lại:** [Phần 6.2 — Các tối ưu quan trọng của C2](../01-giao-trinh/05-jvm-memory-gc-performance.md#p6)

</details>

### Q22. 🔴 Deoptimization là gì? Những nguyên nhân nào gây ra nó? Deopt lặp lại nhiều lần biểu hiện thế nào trên production?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** C2 tối ưu dựa trên **giả định** (speculation): chỉ có một implementation, nhánh này chưa bao giờ chạy, call site chỉ thấy kiểu X. Khi giả định sai, code bị **deoptimize**: method đánh dấu "made not entrant", quay về interpreter, profile lại rồi biên dịch lại. Nguyên nhân: nạp class mới phá vỡ CHA (interface trước đây chỉ có 1 implementation); gặp kiểu mới tại call site trước đây monomorphic; đi vào nhánh "chưa từng chạy" (uncommon trap); code cache đầy; method bị redefine bởi agent hay hot swap. Deopt lặp lại cho ra hiệu năng "răng cưa" và CPU của compiler thread cao. Có lúc JIT từ bỏ hẳn method đó ("not compilable").

**Giải thích chi tiết:**
- Ví dụ: chạy `total(Shape[])` với toàn `Square` trong pha 1 (monomorphic, được inline). Sang pha 2 với ba kiểu, call site trở thành megamorphic, nên method bị deopt và biên dịch lại chậm hơn.
- Một biến thể production: traffic ban đầu chỉ đi một nhánh (ví dụ lúc warm-up chỉ có request GET), traffic thật đi nhánh khác, dẫn tới deopt storm sau khi deploy.
- Theo dõi bằng `-XX:+PrintCompilation` (dòng `made not entrant`), JFR event `jdk.Deoptimization`.

**Câu hỏi nối tiếp:**
- *Thread nào trong `top -H` cho thấy dấu hiệu này?* → `C2 CompilerThread` ăn CPU kéo dài sau giai đoạn khởi động.

**⚠️ Câu trả lời gây điểm trừ:**
- "Code đã JIT thì mãi mãi là mã máy."

**📖 Ôn lại:** [Phần 6.3 — Deoptimization](../01-giao-trinh/05-jvm-memory-gc-performance.md#p6)

</details>

### Q23. 🟡 **[Tình huống]** Mỗi lần deploy, pod mới nhận traffic ngay và trong 1–2 phút đầu p99 latency tăng gấp 5 lần, CPU cao. Vì sao? Bạn giải quyết thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Đây là **warm-up**. Code còn chạy bằng interpreter hoặc C1, các compiler thread tranh CPU với ứng dụng, class đang được nạp lần đầu, cache và connection pool còn lạnh. Giải pháp: (1) **warm-up có chủ đích** bằng traffic giả hoặc gọi các endpoint chính **trước khi readiness probe báo sẵn sàng**; (2) **slow start** ở load balancer để tăng dần tỉ lệ traffic; (3) dùng **CDS/AppCDS** (`-XX:SharedArchiveFile`, `-XX:+AutoCreateSharedArchive` từ JDK 19) để giảm thời gian nạp class; (4) **Project Leyden AOT cache** (JEP 483, JDK 24); (5) **CRaC** (có trên một số bản JDK); (6) **GraalVM Native Image** (khởi động tính bằng mili giây, đổi lại throughput đỉnh và GC hạn chế hơn, cấu hình reflection phức tạp); (7) **đừng đặt CPU limit quá thấp**: với 0.5 CPU, JIT chạy rất chậm và warm-up kéo dài nhiều phút.

**Giải thích chi tiết:**
- Warm-up phải đi qua **đúng đường code của traffic thật**. Warm-up lệch có thể gây deopt khi traffic thật tới (Q22).
- Đo được hiện tượng này qua JFR event Compilation và đồ thị p99 theo thời gian kể từ lúc pod start.

**Câu hỏi nối tiếp:**
- *Vì sao CPU request/limit thấp ảnh hưởng tới GC và JIT?* → `availableProcessors()` lấy theo quota. Ít CPU thì có ít compiler thread và GC thread, và dưới 2 CPU JVM còn tự chọn Serial GC.

**⚠️ Câu trả lời gây điểm trừ:**
- "Tăng heap" hoặc "do GC" mà không nhắc tới JIT.

**📖 Ôn lại:** [Phần 6.4 — Warm-up trên production](../01-giao-trinh/05-jvm-memory-gc-performance.md#p6)

</details>

### Q24. 🔴 **[Tình huống]** Log production xuất hiện hàng loạt `java.lang.NullPointerException` với **message null và không có stack trace**. Vì sao? Bạn điều tra thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Đây là tối ưu **`OmitStackTraceInFastThrow`** của C2. Khi một số exception ngầm (NPE, `ArrayIndexOutOfBoundsException`, `ClassCastException`, `ArithmeticException`) bị ném lặp đi lặp lại tại cùng một chỗ trong code đã biên dịch, JVM chuyển sang ném một **instance tiền cấp phát** không có stack trace và không có message, để khỏi tốn chi phí `fillInStackTrace`. Mất luôn cả helpful NPE message của JDK 14+. Cách điều tra: tìm **lần xuất hiện đầu tiên** trong log, lúc đó vẫn còn đầy đủ stack trace; hoặc khởi động lại với `-XX:-OmitStackTraceInFastThrow`.

**Giải thích chi tiết:**
- Bài học rộng hơn: **exception tạo ra rất đắt** vì phải chụp stack trace. Đừng dùng exception để điều khiển luồng trên đường nóng. Exception dùng làm tín hiệu có thể override `fillInStackTrace` hoặc dùng constructor có `writableStackTrace=false`.
- Một NPE xảy ra hàng nghìn lần mỗi giây đã là bug cần sửa, bất kể có stack trace hay không.

**Câu hỏi nối tiếp:**
- *Có nên tắt tối ưu này vĩnh viễn trên production không?* → Có thể cân nhắc khi khả năng chẩn đoán quan trọng hơn chút hiệu năng. Lúc đó log sẽ phình to nếu lỗi lặp nhiều, nên kết hợp với rate-limit log.

**⚠️ Câu trả lời gây điểm trừ:**
- "Do logger cấu hình sai."

**📖 Ôn lại:** [Phần 6 — Lỗi thường gặp & Bài 6.3 Fast throw](../01-giao-trinh/05-jvm-memory-gc-performance.md#p6)

</details>

---

<a id="nhom-g"></a>
## 7. Nền tảng Garbage Collection

### Q25. 🟢 GC xác định object nào là "rác" như thế nào? GC roots gồm những gì? Vì sao JVM không dùng reference counting?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** JVM dùng **reachability**: object còn "sống" nếu có đường đi tới nó từ một **GC root**. GC roots gồm: biến local và tham số trong stack frame của các thread đang sống (kể cả giá trị trong register của code đã JIT); static field của class đã nạp (reachable qua class loader); JNI reference; monitor đang bị giữ; thread đang chạy; các object nội bộ của JVM. Reference counting không xử lý được **vòng tham chiếu**: hai object trỏ lẫn nhau sẽ không bao giờ về 0. Với reachability, vòng tham chiếu không còn reachable từ root thì vẫn bị thu hồi.

**Giải thích chi tiết:**
- Mặt trái: object "không còn dùng" nhưng **vẫn reachable** (ví dụ vẫn nằm trong `static Map`) thì **không bao giờ** được thu hồi. Đó chính là bản chất memory leak trong Java.
- GC không bảo đảm **khi nào** thu hồi. Nó chỉ bảo đảm **nếu** đã thu hồi thì object đó không còn reachable.

**Câu hỏi nối tiếp:**
- *Gán `obj = null` có giúp GC không?* → Với biến local thì thường vô ích, vì JIT biết biến không còn dùng (liveness analysis). Chỉ có ý nghĩa với reference sống lâu: phần tử mảng trong cấu trúc dữ liệu tự cài (ví dụ `Stack.pop()`, Effective Java Item 7), hoặc field của object sống lâu.

**⚠️ Câu trả lời gây điểm trừ:**
- "Object bị GC ngay khi ra khỏi scope."

**📖 Ôn lại:** [Phần 7.1 — Object nào là rác](../01-giao-trinh/05-jvm-memory-gc-performance.md#p7)

</details>

### Q26. 🟢 So sánh ba thuật toán mark–sweep, mark–compact và copying.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **Mark–sweep** đánh dấu object sống rồi quét phần còn lại vào free list. Nó không di chuyển object, nhưng gây **phân mảnh** và cấp phát từ free list chậm hơn. **Mark–compact** đánh dấu xong thì dồn object sống về một phía. Không còn phân mảnh, cấp phát lại được bằng bump pointer, nhưng phải cập nhật mọi reference và thời gian tỉ lệ với kích thước heap. **Copying (evacuation)** copy object sống sang vùng trống rồi bỏ cả vùng cũ. Chi phí **tỉ lệ với số object sống** (không phụ thuộc lượng rác) và tự compact luôn, nhưng cần không gian dự trữ (survivor, region trống).

**Giải thích chi tiết:**
- Young gen dùng copying vì phần lớn object đã chết, nên copy được rất ít.
- CMS dùng mark-sweep cho old gen, vì vậy bị phân mảnh và gặp "promotion failed". G1 dùng evacuation theo region. ZGC và Shenandoah thực hiện compaction **đồng thời** với ứng dụng.

**Câu hỏi nối tiếp:**
- *Vì sao young GC rẻ dù tần suất cao?* → Chi phí tỉ lệ với lượng object sống cộng root set, không tỉ lệ với lượng rác.

**⚠️ Câu trả lời gây điểm trừ:**
- "Chi phí GC tỉ lệ với lượng rác."

**📖 Ôn lại:** [Phần 7.2 — Các thuật toán cơ bản](../01-giao-trinh/05-jvm-memory-gc-performance.md#p7)

</details>

### Q27. 🟡 Giả thuyết thế hệ (weak generational hypothesis) là gì? Minor GC xử lý reference từ old gen tới young gen thế nào mà không phải quét cả old gen?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Giả thuyết là "**hầu hết object chết trẻ**" (DTO của request, iterator, `StringBuilder`, lambda capture). Vì vậy heap được chia thành young gen (nhỏ, thu thường xuyên bằng copying) và old gen (lớn, thu ít khi hơn). Object mới nằm ở Eden. Mỗi lần sống sót qua minor GC, tuổi (age) tăng lên. Đạt `MaxTenuringThreshold` (tối đa 15, vì age lưu bằng 4 bit trong mark word) hoặc khi survivor đầy thì object được **promote** lên old. Để xử lý reference old → young, JVM dùng **card table**: heap chia thành card 512 byte, và một **write barrier** đánh dấu card "dirty" khi ghi reference vào object ở old gen. Minor GC chỉ quét các card dirty như thêm root. G1 dùng thêm **remembered set** cho từng region.

**Giải thích chi tiết:**
- Tỉ lệ mặc định với Parallel/Serial: `NewRatio=2` (Old:Young = 2:1), `SurvivorRatio=8` (Eden:S0:S1 = 8:1:1). G1 tự điều chỉnh kích thước young theo pause target, nên **không** nên đặt `-Xmn`/`NewRatio` với G1.
- Tenuring threshold là **động**: khi survivor quá tải, threshold có thể giảm xuống 1, gây promote sớm.

**Câu hỏi nối tiếp:**
- *Cái giá của write barrier là gì?* → Mỗi lệnh ghi reference tốn thêm vài lệnh máy. Collector càng tinh vi thì barrier càng nặng: G1 nặng hơn Parallel, ZGC có thêm load barrier.

**⚠️ Câu trả lời gây điểm trừ:**
- "Minor GC quét toàn bộ heap."

**📖 Ôn lại:** [Phần 7.3 — Giả thuyết thế hệ](../01-giao-trinh/05-jvm-memory-gc-performance.md#p7) · [Phần 3.2](../01-giao-trinh/05-jvm-memory-gc-performance.md#p3)

</details>

### Q28. 🔴 Concurrent marking khó ở chỗ nào? Giải thích tri-color marking, SATB và incremental update. Collector nén (compact) đồng thời với ứng dụng bằng cách nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Trong lúc GC đánh dấu, ứng dụng (mutator) vẫn thay đổi đồ thị object. **Tri-color marking** chia object thành ba màu: trắng (chưa thăm), xám (đã thăm nhưng chưa quét các con), đen (đã xong). Lỗi nguy hiểm xảy ra khi mutator gán một object **trắng** vào một object **đen**, đồng thời xoá mọi đường đi cũ tới object trắng đó. GC sẽ không bao giờ thăm object này và **thu hồi nhầm một object còn sống**. Có hai cách chặn. **SATB** (snapshot-at-the-beginning, dùng bởi G1 và Shenandoah): write barrier ghi lại **giá trị cũ** trước khi bị ghi đè, nên mọi thứ reachable lúc bắt đầu marking đều được giữ. **Incremental update** (CMS): ghi lại **reference mới** và quét lại ở pha remark. Về compaction đồng thời: ZGC dùng **colored pointers** cộng **load barrier**; mỗi lần đọc reference, barrier kiểm tra màu, và nếu object đã bị di chuyển thì tự "chữa" pointer. Shenandoah dùng **load-reference barrier** (từ JDK 13, trước đó dùng Brooks forwarding pointer).

**Giải thích chi tiết:**
- Phân biệt hai khái niệm: **parallel** là nhiều GC thread chạy *trong lúc ứng dụng dừng*; **concurrent** là GC thread chạy *song song với ứng dụng*.
- Đánh đổi: barrier làm giảm throughput vài phần trăm, đổi lại pause gần như không phụ thuộc kích thước heap.

**Câu hỏi nối tiếp:**
- *Vì sao ZGC vẫn có pause, dù dưới 1ms?* → Để quét root của thread (stack) và đồng bộ chuyển pha. Phần việc này không tăng theo kích thước heap.

**⚠️ Câu trả lời gây điểm trừ:**
- "Concurrent GC nghĩa là không bao giờ dừng ứng dụng."

**📖 Ôn lại:** [Phần 7.4 — Stop-the-world, concurrent, parallel](../01-giao-trinh/05-jvm-memory-gc-performance.md#p7)

</details>

### Q29. 🟡 Ba thước đo throughput, latency, footprint của GC là gì? Allocation rate, promotion rate và "premature promotion" là gì? Có nên gọi `System.gc()` không?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **Throughput** là phần trăm thời gian CPU dành cho ứng dụng thay vì GC (batch ưu tiên chỉ số này). **Latency** là độ dài pause (p99, max), quan trọng với API real-time. **Footprint** là lượng bộ nhớ cần dùng. Thường chỉ tối ưu được tối đa hai trong ba. **Allocation rate** (MB/s) quyết định tần suất young GC. **Promotion rate** (MB/s lên old gen) quyết định tần suất GC old hoặc mixed. **Premature promotion** là khi object sống "vừa đủ lâu" để bị promote rồi chết ngay sau đó (cache TTL ngắn, session, buffer lớn), làm old gen đầy nhanh. Đây là kẻ thù của mọi GC generational. Gọi `System.gc()` thủ công **gần như luôn sai**: với G1 nó gây Full GC.

**Giải thích chi tiết:**
- Đòn bẩy mạnh nhất là **giảm allocation và giảm live set**. Tăng young gen thường giảm được tần suất GC mà không tăng pause nhiều.
- Có thể chặn bằng `-XX:+DisableExplicitGC`. Cẩn thận vì một số thư viện NIO cũ dựa vào `System.gc()` để giải phóng direct buffer. Phương án an toàn hơn là `-XX:+ExplicitGCInvokesConcurrent`.

**Câu hỏi nối tiếp:**
- *Đo allocation rate thế nào?* → Từ `jstat -gc` (delta Eden cộng số YGC), JFR `jdk.ObjectAllocationSample`, hoặc JMH `-prof gc` cho từng operation (`gc.alloc.rate.norm`).

**⚠️ Câu trả lời gây điểm trừ:**
- "Gọi `System.gc()` sau mỗi batch để dọn bộ nhớ."

**📖 Ôn lại:** [Phần 7.5 — Ba thước đo](../01-giao-trinh/05-jvm-memory-gc-performance.md#p7)

</details>

---

<a id="nhom-h"></a>
## 8. Các Garbage Collector

### Q30. 🟢 GC mặc định qua các phiên bản Java là gì? Vì sao một service chạy trong pod `limits.cpu: 1` lại đang dùng Serial GC mà không ai cấu hình?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Java 5–8 mặc định dùng **Parallel**. Từ **JDK 9 (JEP 248)** mặc định là **G1**. CMS bị deprecated ở JDK 9 và **bị xoá ở JDK 14**. ZGC đạt production ở JDK 15, có generational ở JDK 21, và generational trở thành mặc định khi `-XX:+UseZGC` từ JDK 23. Tuy nhiên **ergonomics** chỉ chọn G1 trên "server-class machine", tức là **≥ 2 CPU và ≥ 1792MB RAM** nhìn thấy được. Nếu không đạt thì JVM chọn **Serial**. Pod `limits.cpu: 1` khiến JVM thấy 1 CPU, nên Serial được chọn âm thầm. Cách xử lý là **chỉ định GC tường minh**.

**Giải thích chi tiết:**
- Kiểm tra nhanh: `java -XX:+PrintFlagsFinal -version | grep -E "UseSerialGC|UseG1GC"`, hoặc liệt kê `GarbageCollectorMXBean` (Serial hiện `Copy`/`MarkSweepCompact`, G1 hiện `G1 Young Generation`/`G1 Old Generation`).
- Serial không phải lúc nào cũng tệ: với heap nhỏ vài trăm MB và 1 CPU, nó hợp lý. Vấn đề là việc chọn lựa diễn ra **ngoài ý muốn**.

**Câu hỏi nối tiếp:**
- *`MaxHeapSize` mặc định trong container 1GB là bao nhiêu?* → 25% × 1GB = 256MB.

**⚠️ Câu trả lời gây điểm trừ:**
- "Java 17 mặc định dùng CMS" hoặc "mặc định luôn là G1".

**📖 Ôn lại:** [Phần 8.1 — Bức tranh tổng quan](../01-giao-trinh/05-jvm-memory-gc-performance.md#p8)

</details>

### Q31. 🟡 Mô tả cách G1 hoạt động: region, young GC, IHOP, concurrent marking, mixed GC. Vì sao gọi là "Garbage-First"?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** G1 chia heap thành khoảng 2048 **region** bằng nhau (1–32MB, từ JDK 18 cho phép tới 512MB). Mỗi region tại một thời điểm đóng một vai: Eden, Survivor, Old, Humongous hoặc Free. Young/old chỉ là tập region **logic**, không liền kề. Ở pha young-only, G1 chạy **young GC** (STW, evacuate Eden và Survivor). Khi heap occupancy vượt **IHOP** (mặc định 45%, có cơ chế adaptive), G1 bắt đầu **concurrent marking**: Concurrent Start đi kèm một young GC, rồi Concurrent Mark song song với ứng dụng, Remark (STW), Cleanup. Sau đó tới pha **mixed GC**: evacuate young cộng **các old region có nhiều rác nhất trước**. Đó là ý nghĩa của "Garbage-First". Mixed GC lặp lại tới khi lượng rác còn lại dưới `G1HeapWastePercent` (5%). Nếu không theo kịp, G1 phải dùng **Full GC** (đa luồng từ JDK 10).

**Giải thích chi tiết:**
- **Pause target** `-XX:MaxGCPauseMillis=200` (mặc định) là **mục tiêu mềm**. G1 dùng mô hình dự đoán để chọn số region trong collection set sao cho pause gần mục tiêu.
- Các tính năng đáng biết: `-XX:+UseStringDeduplication`; trả bộ nhớ chưa dùng về OS định kỳ (JEP 346).

**Câu hỏi nối tiếp:**
- *Vì sao G1 hợp với đa số service?* → Cân bằng throughput và latency, pause tỉ lệ với live set trong collection set, footprint tốt (hỗ trợ compressed oops), ít phải tuning.

**⚠️ Câu trả lời gây điểm trừ:**
- Mô tả G1 như Parallel với young và old liền khối.

**📖 Ôn lại:** [Phần 8.4 — G1](../01-giao-trinh/05-jvm-memory-gc-performance.md#p8)

</details>

### Q32. 🟡 Một đồng nghiệp đặt `-XX:MaxGCPauseMillis=10 -Xmn2g` cho G1 với mong muốn "pause ngắn và young gen lớn". Bạn nhận xét gì?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Hai flag này **mâu thuẫn**, và cả hai đều có hại. `-Xmn` (hoặc `NewRatio`) **cố định** kích thước young gen, nên vô hiệu hoá pause-time ergonomics của G1: G1 không còn co giãn young để đạt pause target. `MaxGCPauseMillis=10` lại quá tham vọng với G1. G1 sẽ cố co young gen thật nhỏ, khiến GC chạy liên tục, throughput giảm, mixed GC không thu kịp, và rủi ro Full GC tăng. Nếu SLO thật sự cần pause dưới 10ms thì nên cân nhắc **generational ZGC** thay vì ép G1.

**Giải thích chi tiết:**
- Quy trình tuning đúng: xác định SLO (p99 latency, throughput, budget RAM); bắt đầu với mặc định (G1, `Xms = Xmx` hợp lý); bật GC log; tải thử giống production; **đổi một tham số mỗi lần**; so sánh trước/sau.
- 90% "GC tuning" thực chất là ba việc: đặt heap đúng kích thước, giảm allocation và live set, chọn đúng collector.

**Câu hỏi nối tiếp:**
- *Copy danh sách 30 flag từ một blog năm 2014 thì sao?* → Nhiều flag đã bị xoá, JVM báo `Unrecognized VM option` và không khởi động được; số còn lại thường gây hại.

**⚠️ Câu trả lời gây điểm trừ:**
- "G1 bảo đảm pause không vượt `MaxGCPauseMillis`."

**📖 Ôn lại:** [Phần 8.4 — Pause target](../01-giao-trinh/05-jvm-memory-gc-performance.md#p8) · [Phần 8.7 — Chọn GC](../01-giao-trinh/05-jvm-memory-gc-performance.md#p8)

</details>

### Q33. 🔴 Humongous object trong G1 là gì và gây vấn đề gì? "To-space exhausted" / "Evacuation Failure" nghĩa là gì, xử lý thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Object có kích thước **≥ 50% region** là humongous. Nó được cấp phát thẳng vào một hoặc nhiều region **liền kề** trong old gen. Vấn đề: tốn region (object 17MB với region 16MB chiếm 2 region, tức 32MB); có thể gây phân mảnh; kích hoạt marking sớm (log hiện `Pause Young (Concurrent Start) (G1 Humongous Allocation)`). JDK 8u60+ có *eager reclaim* cho humongous không còn được tham chiếu. Cách xử lý: tăng `G1HeapRegionSize`, hoặc sửa code (đọc file theo stream, chia nhỏ buffer, không dựng cả CSV 200MB trong một `byte[]`). **To-space exhausted / Evacuation Failure** nghĩa là G1 không còn region trống để copy object sống. Pause sẽ dài và thường kéo theo Full GC. Cách xử lý: tăng heap, tăng `G1ReservePercent`, giảm IHOP để marking bắt đầu sớm hơn, và giảm live set.

**Giải thích chi tiết:**
- Ví dụ: với region 1MB, ngưỡng humongous là 512KB. `byte[600_000]` là humongous chiếm 1 region (lãng phí khoảng 40%). `byte[2_000_000]` chiếm 2 region. Đổi sang region 4MB thì cả hai đều không còn humongous.
- Xem chi tiết bằng `-Xlog:gc*,gc+humongous=debug`.

**Câu hỏi nối tiếp:**
- *Ứng dụng nào hay dính humongous?* → Export báo cáo, đọc hoặc ghi file lớn vào bộ nhớ, JSON payload khổng lồ, buffer Netty lớn, `ArrayList` có hàng triệu phần tử (mảng nền là humongous).

**⚠️ Câu trả lời gây điểm trừ:**
- Không biết ngưỡng 50% region.
- Giải quyết bằng cách chỉ tăng heap mà không xem lại code.

**📖 Ôn lại:** [Phần 8.4 — Humongous & các sự kiện xấu trong log G1](../01-giao-trinh/05-jvm-memory-gc-performance.md#p8)

</details>

### Q34. 🟡 So sánh G1 và ZGC. ZGC đạt pause dưới 1ms bằng cách nào? Rủi ro chính của ZGC là gì?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** G1 cân bằng throughput và latency, pause tỉ lệ với live set trong collection set (thường vài chục tới vài trăm ms), footprint tốt nhờ hỗ trợ compressed oops. ZGC làm **gần như mọi việc đồng thời**, kể cả **relocate object**. Nó dùng colored pointers và load barrier, nên pause chỉ còn để quét root của thread, thường **< 1ms** và **không tăng theo kích thước heap** (heap có thể tới 16TB). Cái giá: throughput thấp hơn G1 vài phần trăm do barrier, cần thêm CPU và RAM, không có compressed oops. Rủi ro chính là **allocation stall**: allocation rate vượt tốc độ GC thì thread ứng dụng bị chặn chờ bộ nhớ (log ghi `Allocation Stall`). Cách xử lý: heap có headroom 20–30% trở lên, điều chỉnh `SoftMaxHeapSize`, `ConcGCThreads`.

**Giải thích chi tiết:**
- **Generational ZGC** (JDK 21 với `-XX:+ZGenerational`; mặc định từ JDK 23; bản non-generational bị xoá ở JDK 24) giảm mạnh CPU overhead và nguy cơ allocation stall.
- Shenandoah cũng dùng concurrent compaction, pause vài ms, có hỗ trợ compressed oops, nhưng **không có trong bản build của Oracle JDK**.
- Cách chọn: service web thông thường, heap 2–32GB, p99 100–200ms chấp nhận được thì dùng G1. p99/p999 cần dưới 10ms, heap lớn, dư CPU và RAM thì dùng generational ZGC. Batch/ETL dùng Parallel. Heap nhỏ hoặc 1 CPU dùng Serial.

**Câu hỏi nối tiếp:**
- *Làm sao chứng minh nên chuyển sang ZGC?* → Tải thử giống production, so sánh p50/p99/max latency, throughput, RSS và CPU, chạy nhiều lần có warm-up.

**⚠️ Câu trả lời gây điểm trừ:**
- "ZGC luôn tốt hơn G1, cứ chuyển hết sang ZGC."

**📖 Ôn lại:** [Phần 8.5 — ZGC](../01-giao-trinh/05-jvm-memory-gc-performance.md#p8) · [Phần 8.7 — Chọn GC](../01-giao-trinh/05-jvm-memory-gc-performance.md#p8)

</details>

### Q35. 🟡 CMS hoạt động thế nào và vì sao bị loại bỏ? "Concurrent mode failure" và "promotion failed" là gì? Nâng cấp từ Java 8 lên 17 với flag CMS cũ thì sao?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** CMS dùng ParNew cho young gen. Old gen đi qua các pha initial mark (STW ngắn), concurrent mark, remark (STW), rồi concurrent sweep. CMS **không compact**, nên old gen bị phân mảnh. Hai kiểu thất bại kinh điển: **concurrent mode failure** (old gen đầy trước khi CMS kịp thu xong) và **promotion failed** (tổng dung lượng còn đủ nhưng không có khối liền nào đủ lớn, do phân mảnh). Cả hai đều fallback sang **Full GC Serial chạy một thread**, gây pause hàng chục giây với heap lớn. CMS bị deprecated ở JDK 9 (JEP 291) và **bị xoá ở JDK 14** (JEP 363), vì chi phí bảo trì cao và đã có G1, ZGC thay thế. Mang `-XX:+UseConcMarkSweepGC` lên JDK 17 thì tuỳ phiên bản, JVM chỉ cảnh báo rồi dùng mặc định, hoặc **không khởi động được**. Khi nâng cấp phải rà lại toàn bộ flag.

**Giải thích chi tiết:**
- Các flag khác cũng cần rà khi nâng cấp: `-XX:MaxPermSize`, `-XX:+PrintGCDetails` (chuyển sang `-Xlog`), `-XX:+UseBiasedLocking`, `-XX:+AggressiveOpts`.

**Câu hỏi nối tiếp:**
- *Hệ thống buộc phải ở Java 8 thì nên dùng GC nào?* → G1 (ổn định từ 8u40+) thay vì CMS hoặc Parallel cho service cần latency.

**⚠️ Câu trả lời gây điểm trừ:**
- Khuyên dùng CMS cho service mới.

**📖 Ôn lại:** [Phần 8.3 — CMS](../01-giao-trinh/05-jvm-memory-gc-performance.md#p8)

</details>

### Q36. 🔴 **[Đọc code]** Đọc đoạn GC log G1 sau (JDK 17, `-Xlog:gc`) và chẩn đoán.

```
[3600.1s][info][gc] GC(900) Pause Young (Normal) (G1 Evacuation Pause) 1890M->1720M(2048M) 45.2ms
[3601.3s][info][gc] GC(901) Pause Young (Concurrent Start) (G1 Humongous Allocation) 1900M->1760M(2048M) 38.0ms
[3601.9s][info][gc] GC(902) Concurrent Mark Cycle 610.4ms
[3602.4s][info][gc] GC(903) Pause Young (Mixed) (G1 Evacuation Pause) 1950M->1850M(2048M) 60.1ms
[3603.0s][info][gc] GC(904) To-space exhausted
[3603.0s][info][gc] GC(904) Pause Young (Normal) (G1 Evacuation Pause) 2040M->2010M(2048M) 420.7ms
[3606.8s][info][gc] GC(905) Pause Full (G1 Compaction Pause) 2045M->1790M(2048M) 3803.5ms
```

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Heap gần như luôn đầy: sau young GC vẫn còn khoảng 1720MB trên 2048MB, tức **live set khoảng 85%**. Young GC thu được rất ít (1890M → 1720M). Marking bị kích hoạt bởi **humongous allocation**. Mixed GC thu không đáng kể (1950M → 1850M). Tiếp theo là **To-space exhausted**, khiến young pause kéo dài 420ms, và cuối cùng là **Full GC 3.8 giây**. Sau Full GC heap vẫn còn 1790MB, nghĩa là phần lớn dữ liệu **thực sự đang sống**. Có hai khả năng: heap quá nhỏ so với live set, hoặc **memory leak** khiến live set tăng dần. Cần so sánh "heap sau GC" theo thời gian. Nếu nó tăng dần thì là leak: lấy hai heap dump cách nhau một khoảng rồi phân tích trong MAT. Nếu nó ổn định ở mức cao thì tăng heap (G1 cần heap khoảng 2–3 lần live set) hoặc giảm dữ liệu giữ trong bộ nhớ. Song song đó, tìm nguồn humongous allocation.

**Giải thích chi tiết:**
- Mỗi dòng đọc theo cấu trúc: loại pause, nguyên nhân, heap trước → sau (tổng committed), thời gian.
- Các dấu hiệu đỏ: `To-space exhausted`, `Pause Full`, heap sau GC sát `-Xmx`, mixed GC thu được ít.
- Công cụ phân tích: GCeasy, GCViewer, JMC. Bật thêm `-Xlog:gc*` để xem chi tiết từng pha.

**Câu hỏi nối tiếp:**
- *Nếu log thường xuyên có `Pause Full (System.gc())` thì sao?* → Có code hoặc thư viện gọi `System.gc()`. Tìm nguồn qua JFR (`jdk.GarbageCollection` có trường cause) hoặc RMI DGC cũ.

**⚠️ Câu trả lời gây điểm trừ:**
- "Đổi sang ZGC là hết." ZGC cũng sẽ allocation stall khi live set gần đầy heap.

**📖 Ôn lại:** [Phần 8.4 — Ví dụ log G1](../01-giao-trinh/05-jvm-memory-gc-performance.md#p8) · [Phần 14.2 — Runbook GC cao](../01-giao-trinh/05-jvm-memory-gc-performance.md#p14)

</details>

---

<a id="nhom-i"></a>
## 9. Reference types & Cleaner

### Q37. 🟢 Kể bốn loại reference trong Java và khi nào mỗi loại bị GC thu hồi.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **Strong** là reference mặc định: object chỉ bị thu khi không còn reachable. **SoftReference**: bị thu khi JVM **thiếu bộ nhớ**, và được bảo đảm bị clear **trước khi** JVM ném `OutOfMemoryError`. **WeakReference**: bị thu ở **lần GC kế tiếp** nếu object chỉ còn weak reference. Dùng cho metadata gắn kèm object (`WeakHashMap`), canonicalizing map, listener không muốn giữ subscriber. **PhantomReference**: `get()` **luôn trả `null`**. Nó được đưa vào `ReferenceQueue` sau khi object đã unreachable, dùng để dọn tài nguyên native, và là cơ chế nền của `Cleaner`.

**Giải thích chi tiết:**
- `ReferenceQueue`: khi GC clear một reference, nó đưa `Reference` vào queue để code xử lý tiếp, ví dụ xoá entry khỏi map.
- `ThreadLocalMap` dùng `WeakReference` cho **key** nhưng **value là strong**. Đây là nguồn gốc của leak trong thread pool.

**Câu hỏi nối tiếp:**
- *Dùng `WeakReference` làm cache được không?* → Kém. Object chỉ weakly reachable bị thu ngay ở lần GC kế tiếp, nên hit rate rất thấp.

**⚠️ Câu trả lời gây điểm trừ:**
- "PhantomReference dùng để lấy lại object sau khi bị GC."

**📖 Ôn lại:** [Phần 9.1 — Reference types](../01-giao-trinh/05-jvm-memory-gc-performance.md#p9)

</details>

### Q38. 🟡 `WeakHashMap` có những cái bẫy nào? Vì sao không nên dùng `SoftReference` làm cache chính?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Bẫy của `WeakHashMap`: (1) **value giữ strong reference tới key**, ví dụ `map.put(k, new Holder(k))`, khiến key không bao giờ chỉ còn weakly reachable, dẫn tới **leak**; (2) dùng String literal làm key thì không bao giờ bị thu, vì literal đã được intern và reachable từ class; (3) `WeakHashMap` **không thread-safe**; (4) entry chỉ được dọn khi map được truy cập (`expungeStaleEntries`). Với `SoftReference`, HotSpot giữ object soft-reachable trong khoảng `SoftRefLRUPolicyMSPerMB` (mặc định 1000ms) nhân với số MB heap trống, tính từ lần truy cập cuối. Khi heap gần đầy, cache bị **xoá hàng loạt cùng lúc**, gây **cache stampede** đúng lúc hệ thống đang căng thẳng nhất, và GC cũng phải làm thêm việc. Hãy dùng cache có giới hạn rõ ràng: Caffeine với `maximumSize` hoặc `maximumWeight` cộng `expireAfterWrite`, và đo hit rate.

**Giải thích chi tiết:** Cách sửa leak kiểu (1): value chỉ giữ `WeakReference<Key>`, hoặc thiết kế lại để value không trỏ ngược về key.

**Câu hỏi nối tiếp:**
- *Caffeine `weakKeys()` khác `WeakHashMap` ở điểm nào?* → Caffeine thread-safe, so sánh key bằng **identity** (`==`) khi bật `weakKeys`, và có thêm eviction cùng metrics.

**⚠️ Câu trả lời gây điểm trừ:**
- "SoftReference là cách tốt nhất để làm cache nhạy bộ nhớ."

**📖 Ôn lại:** [Phần 9.2–9.3 — WeakHashMap & SoftReference](../01-giao-trinh/05-jvm-memory-gc-performance.md#p9)

</details>

### Q39. 🟡 **[Đọc code]** Vì sao `finalize()` bị loại bỏ? Đoạn dùng `Cleaner` sau có bug gì?

```java
public final class NativeBuffer implements AutoCloseable {
    private static final Cleaner CLEANER = Cleaner.create();
    private long address = malloc(1024);
    private final Cleaner.Cleanable cleanable = CLEANER.register(this, () -> free(this.address));
    @Override public void close() { cleanable.clean(); }
}
```

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `finalize()` deprecated từ Java 9 và **deprecated for removal từ Java 18 (JEP 421)**. Lý do: không biết khi nào chạy, có thể không bao giờ chạy; chỉ có một thread `Finalizer` xử lý, nên finalizer chậm làm hàng đợi phình to và dẫn tới OOM; object có finalizer cần ít nhất hai chu kỳ GC mới được thu; object có thể "hồi sinh"; có nguy cơ finalizer attack. Bug trong đoạn code: lambda cleanup **capture `this`** (qua `this.address`). Cleaner giữ action ở dạng strong reference, nên object **không bao giờ trở thành phantom-reachable**, và Cleaner không bao giờ chạy. Đây là leak. Action phải là một **static nested class**, hoặc lambda chỉ capture **state riêng**, không được trỏ tới object được theo dõi.

**Giải thích chi tiết:**

```java
private static final class State implements Runnable {
    private long address;
    State(long a) { address = a; }
    public void run() { if (address != 0) { free(address); address = 0; } }   // chạy đúng 1 lần
}
private final State state = new State(malloc(1024));
private final Cleaner.Cleanable cleanable = CLEANER.register(this, state);
```
- Effective Java Item 8: API chính phải là `AutoCloseable` cộng try-with-resources. Cleaner chỉ là **lưới an toàn**, tương tự cơ chế leak detection của HikariCP và Netty.

**Câu hỏi nối tiếp:**
- *Vì sao `close()` gọi `cleanable.clean()` thay vì gọi trực tiếp `free`?* → `clean()` bảo đảm action chạy **đúng một lần** và huỷ đăng ký với Cleaner.

**⚠️ Câu trả lời gây điểm trừ:**
- Không thấy bug capture `this`.
- Khuyên dùng `finalize()` để đóng tài nguyên.

**📖 Ôn lại:** [Phần 9.4 — Finalization → Cleaner](../01-giao-trinh/05-jvm-memory-gc-performance.md#p9)

</details>

---

<a id="nhom-j"></a>
## 10. Memory leak & `OutOfMemoryError`

### Q40. 🟢 Java có GC, vậy memory leak trong Java là gì? Kể các nguồn leak phổ biến.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Memory leak trong Java là object **không còn cần** nhưng **vẫn reachable**, nên GC không thu được. Biểu hiện: **mức heap sau GC (live set) tăng dần** theo thời gian, GC ngày càng dày, cuối cùng là OOM. Các nguồn phổ biến: (1) static collection hoặc cache tự chế không giới hạn; (2) listener/callback đăng ký mà không huỷ (lambda capture `this`); (3) `ThreadLocal` trong thread pool không `remove()`; (4) key có `hashCode`/`equals` sai hoặc là object mutable, nên không bao giờ tìm lại được để xoá; (5) inner class non-static giữ ngầm reference tới outer; (6) tài nguyên không đóng (stream, connection); (7) unbounded queue, ví dụ `newFixedThreadPool` khi producer nhanh hơn consumer; (8) classloader leak.

**Giải thích chi tiết:**
- Phân biệt với các hiện tượng giống leak: heap quá nhỏ (live set ổn định nhưng cao), spike tạm thời (một request load 2GB), leak native (RSS tăng nhưng heap ổn định).
- Ví dụ thực tế: key cache gồm `(productId, requestTimestamp)` không bao giờ trùng, nên cache phình vô hạn.

**Câu hỏi nối tiếp:**
- *Heap used tăng liên tục có phải leak không?* → Chưa chắc. Với G1 hay Parallel, heap used **luôn** tăng giữa hai lần GC. Phải nhìn **heap sau GC** qua nhiều giờ.

**⚠️ Câu trả lời gây điểm trừ:**
- "Java có GC nên không có memory leak."

**📖 Ôn lại:** [Phần 10.1–10.2 — Memory leak](../01-giao-trinh/05-jvm-memory-gc-performance.md#p10)

</details>

### Q41. 🟡 Kể các loại `OutOfMemoryError` (theo message) và hướng xử lý cho từng loại.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:**

| Message | Nguyên nhân | Hướng xử lý |
|---|---|---|
| `Java heap space` | Heap không đủ chỗ sau khi đã GC | Heap dump tìm leak; tăng heap nếu live set hợp lý; kiểm tra query hoặc payload quá lớn |
| `GC overhead limit exceeded` | Hơn 98% thời gian dành cho GC mà thu hồi dưới 2% heap | Như trên. **Đừng** "chữa" bằng `-XX:-UseGCOverheadLimit` |
| `Metaspace` / `Compressed class space` | Quá nhiều class hoặc classloader (proxy, Groovy, CGLIB, redeploy leak) | `VM.classloader_stats`, `-Xlog:class+load/unload`, tìm loader leak |
| `unable to create native thread` | Không tạo được OS thread (`ulimit`, `pids.max`, native memory) | Đếm thread, tìm thread leak; dùng pool có giới hạn hoặc virtual thread |
| `Direct buffer memory` | Vượt `MaxDirectMemorySize` (NIO, Netty, gRPC) | Tìm buffer không được release (Netty `ResourceLeakDetector`) |
| `Requested array size exceeds VM limit` | Mảng gần `Integer.MAX_VALUE` phần tử | Lỗi logic (collection mở rộng vô hạn) |
| *(không có exception)* exit 137 | Kernel OOM killer (RSS vượt limit container) | NMT, giảm heap %, kiểm tra off-heap |

**Giải thích chi tiết:** Đọc **chính xác message** là bước đầu tiên, vì mỗi loại có hướng điều tra khác hẳn nhau. Có thêm `Out of swap space?` hoặc `request X bytes for...` kèm file `hs_err_pid`: đây là native allocation thất bại.

**Câu hỏi nối tiếp:**
- *Vì sao tăng `-Xmx` không chữa được `unable to create native thread`?* → Heap lớn hơn còn lấy bớt phần bộ nhớ native và địa chỉ ảo dành cho stack của thread.

**⚠️ Câu trả lời gây điểm trừ:**
- Gặp loại OOM nào cũng "tăng `-Xmx`".

**📖 Ôn lại:** [Phần 10.3 — Bảng các loại OutOfMemoryError](../01-giao-trinh/05-jvm-memory-gc-performance.md#p10)

</details>

### Q42. 🔴 **[Tình huống]** "Service chạy vài ngày thì chậm dần, GC ngày càng nhiều, rồi chết. Restart lại thì chạy tốt vài ngày." Hãy trình bày cách bạn xử lý từ đầu tới cuối.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Trả lời theo trình tự. **Triệu chứng và giả thuyết**: chu kỳ "restart thì khỏi" gợi ý leak, nên kiểm tra metric **old gen / heap sau GC** qua nhiều ngày (tăng đều, ví dụ 200MB mỗi ngày), cùng tần suất GC và p99. **Ổn định hệ thống**: scale out, hoặc restart theo lịch như biện pháp tạm thời, nhưng **giữ bằng chứng** trước khi restart. **Thu thập**: bật sẵn `-XX:+HeapDumpOnOutOfMemoryError` (dump vào volume) và `ExitOnOutOfMemoryError`; khi chưa OOM thì lấy **hai heap dump cách nhau 30–60 phút** (`jcmd <pid> GC.heap_dump`), hoặc nhẹ hơn là so sánh `GC.class_histogram`; rút instance khỏi load balancer trước khi dump vì dump là STW. **Phân tích bằng MAT**: Leak Suspects, Dominator Tree (top retained), **Path to GC Roots** (loại trừ weak/soft), so sánh histogram giữa hai dump. **Nguyên nhân gốc**, ví dụ `ConcurrentHashMap` cache key theo `userId + timestamp`. **Sửa**: Caffeine `maximumSize` cộng `expireAfterWrite`. **Xác nhận**: soak test 12–24 giờ cho thấy live set phẳng. **Phòng ngừa**: alert "old gen after GC > 70% trong 30 phút", soak test trong CI, checklist review "mọi cache phải có giới hạn và metric".

**Giải thích chi tiết:**
- Rủi ro của chính việc chẩn đoán: heap dump là STW và tỉ lệ với heap (heap chục GB mất vài phút); file dump lớn bằng heap used, nên đĩa phải đủ chỗ; file chứa **dữ liệu nhạy cảm** (token, PII) nên phải xử lý như dữ liệu production; dump vào ổ ephemeral của pod sẽ mất khi pod restart.
- Nếu heap ổn định mà RSS tăng thì chuyển sang hướng điều tra native leak (Q56).

**Câu hỏi nối tiếp:**
- *Vì sao không cứ thế tăng `-Xmx`?* → Chỉ trì hoãn sự cố; heap dump lớn hơn, Full GC dài hơn, và nguyên nhân gốc vẫn còn đó.

**⚠️ Câu trả lời gây điểm trừ:**
- Chỉ liệt kê tên công cụ mà không có quy trình, hoặc "restart định kỳ bằng cron là xong".

**📖 Ôn lại:** [Phần 10 — Góc nhìn Senior](../01-giao-trinh/05-jvm-memory-gc-performance.md#p10) · [Phần 14.3 — Runbook OOM](../01-giao-trinh/05-jvm-memory-gc-performance.md#p14) · [Phần 14.4 — Mẫu postmortem](../01-giao-trinh/05-jvm-memory-gc-performance.md#p14)

</details>

### Q43. 🟡 Có nên `catch (OutOfMemoryError e)` để ứng dụng chạy tiếp không? Chiến lược đúng trên production là gì?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Bắt được, vì OOM là một `Error`, nhưng **không nên** để ứng dụng chạy tiếp. Sau OOM, trạng thái ứng dụng không còn đáng tin: một thread nào đó có thể đã chết giữa chừng, lock có thể chưa được nhả, invariant có thể đã vỡ, và ứng dụng "sống dở chết dở" còn tệ hơn chết hẳn. Chiến lược đúng: **`-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=<volume>`** để giữ bằng chứng, cộng **`-XX:+ExitOnOutOfMemoryError`** (hoặc `CrashOnOutOfMemoryError` để có thêm core và `hs_err`) để orchestrator restart instance sạch.

**Giải thích chi tiết:**
- Ngoại lệ hiếm hoi chấp nhận được: thao tác cô lập kiểu "thử cấp phát một mảng rất lớn để xử lý ảnh, thất bại thì báo lỗi cho người dùng". Ngay cả trường hợp này cũng nên kiểm tra kích thước đầu vào trước.
- `HeapDumpPath` phải đủ dung lượng (≥ heap) và nằm trên volume bền vững.

**Câu hỏi nối tiếp:**
- *Spring Boot có health indicator nào liên quan không?* → Liveness probe sẽ phát hiện app treo. Tuy vậy, `ExitOnOutOfMemoryError` phản ứng nhanh và rõ ràng hơn.

**⚠️ Câu trả lời gây điểm trừ:**
- "Catch OOM, log, rồi `System.gc()` và tiếp tục."

**📖 Ôn lại:** [Phần 10.3 — chiến lược production](../01-giao-trinh/05-jvm-memory-gc-performance.md#p10) · [Phần 11.5 — Khi có sự cố](../01-giao-trinh/05-jvm-memory-gc-performance.md#p11)

</details>

---

<a id="nhom-k"></a>
## 11. JVM flags & container

### Q44. 🟢 Có nên đặt `-Xms` bằng `-Xmx` cho server không? Trong container nên dùng `-Xmx` hay `-XX:MaxRAMPercentage`? Dùng cả hai thì sao?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Với server, thường nên đặt **`Xms = Xmx`** để tránh resize heap lúc chạy và **phát hiện thiếu RAM ngay khi khởi động** thay vì giữa giờ cao điểm. Trong container, `-XX:MaxRAMPercentage` (cộng `InitialRAMPercentage`) tiện hơn vì tự co giãn theo limit. Mặc định của nó chỉ là **25%**, thường quá thấp cho service. Giá trị điển hình là 60–75% tuỳ lượng off-heap. Nếu đặt cả hai thì **`-Xmx` thắng**, và percentage bị bỏ qua.

**Giải thích chi tiết:**
- `-XX:+AlwaysPreTouch` chạm vào mọi trang heap lúc khởi động để tránh page fault khi chạy, đổi lại khởi động chậm hơn.
- Xem giá trị thực tế: `java -XX:+PrintFlagsFinal -version`, hoặc trên process đang chạy dùng `jcmd <pid> VM.flags -all` và `VM.command_line`.

**Câu hỏi nối tiếp:**
- *Container nhỏ 512MB thì đặt bao nhiêu phần trăm?* → Thấp hơn, khoảng 50%, vì phần overhead cố định (Metaspace, code cache, thread) chiếm tỉ lệ lớn.

**⚠️ Câu trả lời gây điểm trừ:**
- "Đặt `-Xmx` bằng limit container."

**📖 Ôn lại:** [Phần 11.2 — Flag bộ nhớ](../01-giao-trinh/05-jvm-memory-gc-performance.md#p11)

</details>

### Q45. 🟡 JVM "nhìn" container thế nào qua các phiên bản? Số CPU mà JVM thấy ảnh hưởng tới những gì?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Java 8 **trước 8u191** không đọc giới hạn cgroup: nó thấy toàn bộ RAM và CPU của node, nên heap mặc định bằng 1/4 RAM **node** (ví dụ 16GB trên node 64GB) dù container chỉ được 2GB, dẫn tới OOMKilled. `-XX:+UseContainerSupport` có từ **JDK 10** (backport 8u191) và mặc định bật. Hỗ trợ **cgroup v2** có từ JDK 15 (backport 11.0.16 và 8u372), nên image JDK cũ chạy trên node cgroup v2 lại "mù" giới hạn. Số CPU (`availableProcessors()`, tính từ **CPU quota/limits**; từ JDK 19 và các bản backport 17/11 mới thì không còn dựa vào CPU shares) ảnh hưởng tới: số GC thread, số compiler thread, kích thước `ForkJoinPool.commonPool()` (parallel stream, CF không truyền executor), và lựa chọn Serial nếu dưới 2 CPU. Có thể ép bằng `-XX:ActiveProcessorCount=N`.

**Giải thích chi tiết:**
- Kiểm tra nhanh: `java -XshowSettings:system -version` (JDK 17+) hoặc `-Xlog:os+container=info`.
- CPU throttling (CFS quota) có thể tạo ra "pause" lớn hơn cả GC. Theo dõi metric `container_cpu_cfs_throttled_periods_total`.

**Câu hỏi nối tiếp:**
- *Có nên đặt CPU limit cho service Java không?* → Đây là chủ đề còn tranh luận. Limit quá thấp làm JIT và GC chậm, gây throttling. Nhiều team chỉ đặt request, hoặc đặt limit rộng tay, kèm `ActiveProcessorCount` tường minh.

**⚠️ Câu trả lời gây điểm trừ:**
- "JVM luôn tự nhận giới hạn container" mà không biết về mốc 8u191 và cgroup v2.

**📖 Ôn lại:** [Phần 11.3 — JVM trong container](../01-giao-trinh/05-jvm-memory-gc-performance.md#p11)

</details>

### Q46. 🔴 **[Tình huống]** Service có live set khoảng 900MB sau GC, 300 platform thread, Netty dùng tối đa 256MB direct memory, Metaspace đo được 180MB. Hãy đề xuất cấu hình JVM và `limits.memory`.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Một phương án: **heap ≈ 2.5GB** (`-Xmx2560m`, vì G1 cần khoảng 2–3 lần live set để không phải GC liên tục), **`MaxMetaspaceSize=256m`** (đo được 180MB cộng headroom), **`MaxDirectMemorySize=256m`** (đặt tường minh, nếu không mặc định ≈ Xmx), thread khoảng 300 × 1MB ≈ 300MB (thực dùng thường ít hơn), và khoảng 300MB cho code cache, GC structures, JVM internals. Tổng ≈ 3.7GB, nên chọn **`limits.memory: 4Gi`**, `requests.memory = limits.memory`. Nếu muốn dùng phần trăm thì `MaxRAMPercentage ≈ 62`. Điểm Senior là: đây chỉ là con số khởi đầu. Phải **đo bằng NMT dưới tải thật** (`VM.native_memory summary`) và so với RSS (`ps -o rss`), rồi điều chỉnh.

**Giải thích chi tiết:**
- Công thức: `limit ≥ Xmx + MaxMetaspaceSize + MaxDirectMemorySize + threads × ~1MB + ~250MB`.
- Memory là tài nguyên **không nén được**. Overcommit (`request < limit`) dẫn tới pod bị kill khi node chịu áp lực.
- Bổ sung flag vận hành: GC log xoay vòng, heap dump vào volume, `ExitOnOutOfMemoryError`, JFR liên tục (`maxage`, `maxsize`).
- Có thể cân nhắc giảm thread platform (chuyển sang virtual thread) để tiết kiệm phần stack.

**Câu hỏi nối tiếp:**
- *Vì sao không đặt heap 3.5GB cho "an toàn"?* → Sẽ không còn chỗ cho off-heap, và pod bị OOMKilled khi Netty dùng hết direct memory.

**⚠️ Câu trả lời gây điểm trừ:**
- Chỉ tính heap, hoặc đưa ra con số mà không có công thức và không đề cập tới việc đo kiểm.

**📖 Ôn lại:** [Phần 11 — Góc nhìn Senior & Bài 11.2](../01-giao-trinh/05-jvm-memory-gc-performance.md#p11)

</details>

### Q47. 🟡 Bật GC log thế nào trên Java 8 và Java 9+? Có nên bật GC log và JFR thường trực trên production không?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Java 9+ dùng **Unified Logging** (JEP 158, 271): `-Xlog:gc*:file=/logs/gc.log:time,uptime,level,tags:filecount=10,filesize=20m`. Khi nghi ngờ TTSP thì thêm `-Xlog:safepoint*`. Java 8: `-XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:/logs/gc.log -XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=10 -XX:GCLogFileSize=20M`. **Có, nên bật thường trực.** GC log gần như không tốn chi phí. JFR với cấu hình `default` tốn khoảng 1% (khoảng 2% với `profile`), nên cũng bật thường trực được (`-XX:StartFlightRecording=disk=true,maxage=6h,maxsize=512m,dumponexit=true,...`). Khi sự cố xảy ra, bằng chứng đã có sẵn.

**Giải thích chi tiết:**
- JFR có sẵn (open source) từ JDK 11 (JEP 328) và được backport vào 8u262.
- Đặt file log vào volume, và luôn xoay vòng để không làm đầy đĩa.

**Câu hỏi nối tiếp:**
- *Dùng `-XX:+PrintGCDetails` trên JDK 17 thì sao?* → JVM ánh xạ sang `-Xlog` và in cảnh báo deprecated. Nên chuyển hẳn sang cú pháp mới.

**⚠️ Câu trả lời gây điểm trừ:**
- "GC log làm chậm ứng dụng nên chỉ bật khi debug."

**📖 Ôn lại:** [Phần 11.4 — GC logging](../01-giao-trinh/05-jvm-memory-gc-performance.md#p11) · [Phần 12.3 — JFR](../01-giao-trinh/05-jvm-memory-gc-performance.md#p12)

</details>

---

<a id="nhom-l"></a>
## 12. Công cụ chẩn đoán

### Q48. 🟡 Bạn dùng `jcmd`, `jstat`, `jstack`, `jmap` vào việc gì? Lệnh nào **nguy hiểm** khi chạy trên production giờ cao điểm?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **`jcmd`** là "dao đa năng" và nên được ưu tiên: `VM.flags`, `GC.heap_info`, `GC.class_histogram`, `GC.heap_dump`, `Thread.print -l`, `JFR.start/dump`, `VM.native_memory`, `Compiler.codecache`, `VM.classloader_stats`, `Thread.dump_to_file -format=json` (JDK 21, có cả virtual thread). **`jstat -gcutil <pid> 1000`** cho thống kê GC theo chu kỳ, cực nhẹ (các cột S0 S1 E O M CCS YGC YGCT FGC FGCT GCT). **`jstack -l`** lấy thread dump. **`jmap`** lấy histogram hoặc heap dump. Lệnh nguy hiểm: **`jmap -histo:live`** và **`GC.class_histogram`** đều **kích hoạt Full GC**; **heap dump** là STW và tỉ lệ với heap, có thể mất vài giây tới vài phút.

**Giải thích chi tiết:**
- Thứ tự ưu tiên khi chẩn đoán: metric có sẵn, rồi GC log, JFR, thread dump, async-profiler, và **heap dump để cuối cùng** vì đắt, STW, chứa dữ liệu nhạy cảm.
- Phải chạy công cụ **cùng user và cùng phiên bản JDK** với process. Trong container: `kubectl exec`; nếu image chỉ có JRE thì dùng ephemeral debug container chia sẻ process namespace, hoặc `jattach`.

**Câu hỏi nối tiếp:**
- *Từ `jstat` tính thời gian trung bình mỗi young GC thế nào?* → `YGCT / YGC`. Cột `FGC` tăng nghĩa là vừa có Full GC.

**⚠️ Câu trả lời gây điểm trừ:**
- "Cứ chạy `jmap -histo:live` cho nhanh."

**📖 Ôn lại:** [Phần 12.1 — Công cụ dòng lệnh của JDK](../01-giao-trinh/05-jvm-memory-gc-performance.md#p12)

</details>

### Q49. 🟡 Trong Eclipse MAT, shallow heap và retained heap khác nhau thế nào? Dominator tree và "Path to GC Roots" dùng để làm gì?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **Shallow heap** là kích thước của chính object. **Retained heap** là tổng bộ nhớ được giải phóng nếu object đó bị thu hồi, gồm object và mọi thứ chỉ nó giữ. **Dominator tree**: object X *dominate* Y nếu mọi đường đi từ GC root tới Y đều phải qua X. Sắp xếp theo retained heap sẽ thấy ngay "ai đang giữ nhiều bộ nhớ nhất". **Path to GC Roots** (loại trừ weak/soft reference) trả lời câu hỏi "**vì sao** object này còn sống", tức là chuỗi reference giữ nó. Đây là chìa khoá để tìm nguyên nhân gốc của leak.

**Giải thích chi tiết:**
- Quy trình: chạy **Leak Suspects report**, xem Dominator Tree, rồi Path to GC Roots. **So sánh histogram hai dump** để thấy class nào tăng. **OQL** cho truy vấn như `SELECT * FROM java.util.HashMap s WHERE s.size > 100000`.
- Tuỳ chọn `live` khi dump sẽ chạy Full GC trước, nên dump chỉ chứa object sống. Đây là điều bạn muốn khi tìm leak. Nếu muốn xem rác để tìm nguồn allocation churn thì không dùng `live`.

**Câu hỏi nối tiếp:**
- *Một `HashMap` có shallow 48 byte nhưng retained 1.4GB nghĩa là gì?* → Map đó là "cửa ngõ" duy nhất giữ 1.4GB dữ liệu. Đây là ứng viên leak hàng đầu.

**⚠️ Câu trả lời gây điểm trừ:**
- Sắp xếp theo shallow heap rồi kết luận `byte[]` hay `char[]` là thủ phạm.

**📖 Ôn lại:** [Phần 12.2 — Heap dump & Eclipse MAT](../01-giao-trinh/05-jvm-memory-gc-performance.md#p12)

</details>

### Q50. 🔴 So sánh JFR, async-profiler và VisualVM sampler. "Safepoint bias" là gì? Flame graph đọc thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **JFR** là bộ ghi sự kiện tích hợp sẵn trong JVM, overhead khoảng 1% nên **bật thường trực được**. Nó ghi CPU sampling, allocation, GC, safepoint, lock contention (`jdk.JavaMonitorEnter`), I/O, exception, JIT, container metrics, cộng **custom event** của ứng dụng. Phân tích bằng JMC. **async-profiler** dùng `perf_events` cộng `AsyncGetCallTrace`, nên **không bị safepoint bias** và thấy được cả frame native và kernel. Các chế độ: `cpu`, `wall` (cả thời gian chờ I/O và lock), `alloc`, `lock`, `itimer`. **VisualVM sampler** (và nhiều profiler dựa trên `Thread.getAllStackTraces`/JVMTI) chỉ lấy mẫu **tại safepoint**. Đó là **safepoint bias**: hotspot thật nằm trong vòng lặp counted không có safepoint poll sẽ bị báo sai sang chỗ khác. **Flame graph**: trục x là **tỉ lệ số mẫu**, không phải thời gian; "đỉnh rộng" là nơi tốn CPU; đọc stack từ dưới lên.

**Giải thích chi tiết:**

```bash
jcmd <pid> JFR.start name=incident duration=120s settings=profile filename=/dumps/incident.jfr
./asprof -d 30 -e cpu   -f cpu.html   <pid>
./asprof -d 30 -e alloc -f alloc.html <pid>
./asprof -d 30 -e wall -t -f wall.html <pid>   # tìm I/O chậm, lock
```
- JFR Event Streaming (JDK 14+, `RecordingStream`) cho phép đọc event ngay trong process và đẩy ra metric.

**Câu hỏi nối tiếp:**
- *Endpoint chậm nhưng CPU flame graph không thấy gì nổi bật thì sao?* → Thời gian đang nằm ở **chờ** (I/O, lock, pool), nên dùng chế độ `wall` hoặc xem các JFR event về socket I/O và thread park.

**⚠️ Câu trả lời gây điểm trừ:**
- "Trục x của flame graph là thời gian."
- Tin tuyệt đối vào profiler dựa trên safepoint.

**📖 Ôn lại:** [Phần 12.3 — JFR & JMC](../01-giao-trinh/05-jvm-memory-gc-performance.md#p12) · [Phần 12.4 — async-profiler](../01-giao-trinh/05-jvm-memory-gc-performance.md#p12)

</details>

---

<a id="nhom-m"></a>
## 13. Đo hiệu năng & JMH

### Q51. 🟡 Vì sao microbenchmark tự viết bằng `System.nanoTime()` trong `main` gần như luôn sai? JMH giải quyết những vấn đề đó ra sao?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Có năm lý do: (1) đo lẫn cả interpreter và thời gian JIT vì không warm-up; (2) **dead code elimination**: kết quả không được dùng nên JIT xoá luôn phép tính, cho ra "0 ns"; (3) **constant folding**: input là hằng nên JIT tính sẵn lúc biên dịch; (4) OSR và loop optimization khiến code được đo khác với code thật; (5) GC xảy ra ngẫu nhiên, và profile bị "nhiễm" bởi benchmark chạy trước trong cùng JVM. **JMH** giải quyết bằng cách: **fork JVM riêng** cho mỗi benchmark, có pha **warm-up** và measurement, dùng **`Blackhole`** (hoặc `return`) để chống dead code elimination, đặt input trong `@State` để chống constant folding, hỗ trợ nhiều chế độ (`Throughput`, `AverageTime`, `SampleTime`, `SingleShotTime`), và có các profiler (`-prof gc` cho `gc.alloc.rate.norm`, `perfasm`, `async`).

**Giải thích chi tiết:**
- Luôn báo cáo kết quả kèm **error** (±). Chênh lệch nhỏ hơn error thì không có ý nghĩa.
- JMH trả lời câu hỏi "A nhanh hơn B **trong điều kiện cô lập**", không trả lời "cả hệ thống sẽ nhanh hơn". Câu hỏi sau cần load test và profiling.

**Câu hỏi nối tiếp:**
- *Benchmark trên laptop có vấn đề gì?* → Turbo Boost, chế độ tiết kiệm pin, ứng dụng khác chạy song song, chỉ có 1 fork, chạy trong IDE ở chế độ debug.

**⚠️ Câu trả lời gây điểm trừ:**
- "Chạy vòng lặp một triệu lần rồi chia trung bình là đủ."

**📖 Ôn lại:** [Phần 13.2–13.3 — JMH](../01-giao-trinh/05-jvm-memory-gc-performance.md#p13)

</details>

### Q52. 🔴 **[Đọc code]** Benchmark JMH sau có những lỗi gì?

```java
@State(Scope.Benchmark)
public class ParseBench {
    private static final String INPUT = "12345";
    @Benchmark
    public void parse() {
        Integer.parseInt(INPUT);
    }
    @Benchmark
    public long loop() {
        long s = 0;
        for (int i = 0; i < 1_000_000; i++) s += Integer.parseInt(INPUT);
        return s;
    }
}
```

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Có ba lỗi. (1) `parse()` **bỏ kết quả**, nên JIT có thể xoá phép tính (dead code elimination). Phải `return` kết quả hoặc gọi `bh.consume(...)`. (2) `INPUT` là **`static final` hằng**, nên JIT có thể constant-fold. Phải đặt input trong một field **không final** của `@State`, và nên dùng `@Param` để thử nhiều giá trị. (3) `loop()` tự viết vòng lặp bên trong `@Benchmark`. JIT có thể unroll, hoist hoặc fold cả vòng lặp, và kết quả không còn phản ánh chi phí của một operation. Hãy để JMH tự lặp (mỗi lần gọi là một operation), hoặc dùng `@OperationsPerInvocation(N)` kèm `Blackhole.consume` trong vòng lặp. Ngoài ra còn thiếu `@Warmup`, `@Measurement`, `@Fork` tường minh. Mặc định của JMH chạy rất lâu, nên thường phải chỉnh, và cần chạy ít nhất 2 fork.

**Giải thích chi tiết:**

```java
@State(Scope.Benchmark)
public class ParseBench {
    @Param({"7", "12345", "2147483647"}) String input;
    @Benchmark public int parse() { return Integer.parseInt(input); }
}
```
- Chạy với `-prof gc` để xem số byte cấp phát cho mỗi operation.

**Câu hỏi nối tiếp:**
- *Dấu hiệu nhận ra benchmark sai?* → Kết quả phi lý: gần 0 ns, hoặc không thay đổi khi đổi kích thước input.

**⚠️ Câu trả lời gây điểm trừ:**
- Chỉ thấy lỗi thiếu `Blackhole`, không thấy lỗi hằng số và lỗi vòng lặp.

**📖 Ôn lại:** [Phần 13 — Bài 13.2 Bắt lỗi benchmark](../01-giao-trinh/05-jvm-memory-gc-performance.md#p13)

</details>

### Q53. 🟡 Vì sao báo cáo hiệu năng phải dùng percentile thay cho trung bình? Coordinated omission là gì? Áp dụng Little's Law thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Latency có phân bố đuôi dài. Trung bình **che giấu** các request chậm, nên phải xem p50, p95, **p99, p99.9, max**. Một trang gọi 20 service thì xác suất **ít nhất một** lời gọi rơi vào p99 là 1 − 0.99²⁰ ≈ **18%**, nghĩa là p99 của từng service trở thành trải nghiệm của rất nhiều người dùng. **Coordinated omission** (Gil Tene): load generator kiểu "gửi request tiếp theo khi request trước xong" sẽ tự giảm tải đúng vào lúc hệ thống chậm, và bỏ qua những request lẽ ra đã phải gửi. Kết quả là p99 đẹp giả tạo. Cách tránh: dùng công cụ **constant arrival rate** (wrk2, Gatling hoặc k6 với open model) cộng HdrHistogram. **Little's Law** `L = λ × W`: 1000 rps × 0.2s = **200 request đồng thời**, nên cần ≥ 200 thread (với mô hình thread-per-request) hoặc dùng mô hình non-blocking/virtual thread.

**Giải thích chi tiết:**
- Nguyên tắc: **đo trước, tối ưu sau**; xác định SLO trước (ví dụ "p99 < 200ms ở 1500 rps"); môi trường test phải giống production (JDK, flag, giới hạn container, dữ liệu kích thước thật, đã warm-up).
- **Phương pháp USE** (Brendan Gregg): với mỗi tài nguyên (CPU, RAM, disk, network, thread pool, connection pool), kiểm tra Utilization, Saturation, Errors.

**Câu hỏi nối tiếp:**
- *Throughput khác latency thế nào?* → Throughput là số việc hoàn thành trên đơn vị thời gian, latency là thời gian của một việc. Tăng throughput (ví dụ batching) có thể làm latency tăng.

**⚠️ Câu trả lời gây điểm trừ:**
- Báo cáo "latency trung bình 50ms" là đủ.

**📖 Ôn lại:** [Phần 13.1 — Nguyên tắc đo hiệu năng](../01-giao-trinh/05-jvm-memory-gc-performance.md#p13)

</details>

---

<a id="nhom-n"></a>
## 14. Tình huống sự cố production

### Q54. 🔴 **[Tình huống]** Một pod Java đột nhiên CPU 100% và latency tăng vọt. Trình bày runbook điều tra của bạn.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** (1) **Ổn định trước**: scale out hoặc rút pod khỏi load balancer nhưng **giữ pod lại** để điều tra. (2) Xác định **thread nào** ăn CPU: `top -H -p <pid>` lấy TID, `printf '%x' <tid>` đổi sang hex, rồi lấy **3 thread dump cách nhau 5 giây** (`jcmd <pid> Thread.print -l`) và tìm `nid=0x...`. Cách nhanh và chính xác hơn là **async-profiler `-e cpu` trong 30 giây** để có flame graph. (3) Phân loại theo tên thread. `GC Thread#`, `G1 Conc#`, `ZWorker` nghĩa là **GC liên tục** (heap gần đầy, leak hoặc allocation rate quá cao), nên chuyển sang runbook GC. `C2 CompilerThread` nghĩa là JIT đang làm việc (vừa khởi động, deopt storm, code cache đầy). Thread ứng dụng (`http-nio-*`) nghĩa là code nóng: vòng lặp vô hạn, **regex catastrophic backtracking**, `HashMap` bị hỏng do dùng đồng thời, serialize JSON khổng lồ, spin hoặc busy-wait. Nhiều thread cùng ở một `synchronized` hoặc CAS loop nghĩa là contention, cần JFR `jdk.JavaMonitorEnter` hoặc async-profiler `-e lock`. (4) Sửa nguyên nhân gốc, rồi viết postmortem.

**Giải thích chi tiết:**
- Ví dụ regex: `^([a-zA-Z0-9]+)*@example\.com$` gặp input `"aaaa...a!"` thì backtracking O(2ⁿ). Thread dump nhiều lần đều thấy thread nằm ở `java.util.regex.Pattern$Loop.match`. Sửa bằng cách bỏ quantifier lồng (`^[a-zA-Z0-9]+@example\.com$`) hoặc dùng atomic group/possessive quantifier, và **giới hạn độ dài input**.
- Trong container, nhớ kiểm tra **CPU throttling**: nó cũng biểu hiện là "chậm", nhưng `top` bên trong container có thể không phản ánh đúng.

**Câu hỏi nối tiếp:**
- *Vì sao phải lấy nhiều dump?* → Để phân biệt thread "kẹt" ở một chỗ với thread "tình cờ đang ở đó" khi dump được chụp.

**⚠️ Câu trả lời gây điểm trừ:**
- "Restart pod" mà không thu thập bằng chứng.
- Chỉ nhìn đúng một thread dump.

**📖 Ôn lại:** [Phần 14.1 — Runbook CPU 100%](../01-giao-trinh/05-jvm-memory-gc-performance.md#p14)

</details>

### Q55. 🔴 **[Tình huống]** Metric cho thấy request thỉnh thoảng bị "đứng" 1–2 giây, nhưng GC log chỉ ghi các pause dưới 50ms. Bạn nghi ngờ những gì và kiểm chứng thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Pause không phải do GC. Có bốn nghi phạm. (1) **Time-to-safepoint** dài: một thread chạy counted loop không có safepoint poll khiến cả JVM chờ. Kiểm chứng bằng `-Xlog:safepoint*` (xem "Reaching safepoint") hoặc JFR event safepoint. (2) **CPU throttling của cgroup** (CFS quota): process bị tạm dừng tới hết chu kỳ 100ms, lặp lại nhiều lần. Kiểm chứng bằng `container_cpu_cfs_throttled_periods_total` hoặc `cat /sys/fs/cgroup/cpu.stat`. (3) **Swap hoặc page fault**: `vmstat` (cột `si/so`), transparent huge pages. (4) Nguồn ngoài JVM: lock contention, chờ connection pool, DNS, downstream chậm. Kiểm chứng bằng async-profiler chế độ `wall` hoặc JFR (thread park, socket read). Ngoài ra, chính việc heap dump hoặc `jmap -histo:live` do ai đó chạy cũng gây STW dài.

**Giải thích chi tiết:**
- Nguyên tắc: đối chiếu **thời điểm** spike latency với từng nguồn log (GC, safepoint, throttling, deploy, cron job).
- Trên JDK hiện đại, loop strip mining đã giảm mạnh vấn đề TTSP, nhưng code native (JNI) và một số cấu hình vẫn có thể gây ra nó.

**Câu hỏi nối tiếp:**
- *Cách giảm throttling?* → Tăng hoặc bỏ CPU limit, giảm số GC và compiler thread cho khớp quota (`ActiveProcessorCount`), tránh để burst CPU của GC trùng với giờ cao điểm.

**⚠️ Câu trả lời gây điểm trừ:**
- "Chắc chắn là GC, đổi sang ZGC."

**📖 Ôn lại:** [Phần 14.2 — Runbook GC cao / pause dài](../01-giao-trinh/05-jvm-memory-gc-performance.md#p14) · [Phần 8 — Góc nhìn Senior](../01-giao-trinh/05-jvm-memory-gc-performance.md#p8)

</details>

### Q56. 🔴 **[Tình huống]** RSS của process Java tăng đều mỗi ngày tới khi bị OOMKilled, nhưng heap sau GC hoàn toàn ổn định. Bạn điều tra thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Heap ổn định mà RSS tăng thì đây là **leak native / off-heap**. Các bước: (1) bật **NMT** (`-XX:NativeMemoryTracking=summary`, overhead vài phần trăm), chạy `jcmd <pid> VM.native_memory baseline`, đợi vài giờ rồi chạy `summary.diff` để thấy hạng mục nào tăng: Thread (thread leak), Class (Metaspace), Internal/Other (direct buffer), Code, GC. (2) Nếu hạng mục **Other/Internal** tăng thì nghi **direct buffer**: Netty `ByteBuf` không `release()` (bật `-Dio.netty.leakDetection.level=paranoid` trong test), hoặc NIO buffer. Đặt `MaxDirectMemorySize` để biến leak thành Java OOM rõ ràng. (3) **`Inflater`/`Deflater`** (từ `GZIPInputStream`/`GZIPOutputStream`) không `close()` thì giữ bộ nhớ native của zlib. (4) Nếu NMT **không** giải thích được phần tăng (RSS lớn hơn tổng NMT) thì là bộ nhớ ngoài tầm theo dõi của JVM: thư viện JNI, hoặc **glibc malloc arena** phân mảnh khi có nhiều thread. Thử `MALLOC_ARENA_MAX=2` hoặc đổi sang jemalloc, rồi dùng `pmap`/jemalloc profiling. (5) Đếm thread theo thời gian (`/proc/<pid>/status`), vì thread leak cũng làm RSS tăng.

**Giải thích chi tiết:**
- Heap dump vẫn có ích: số lượng object `DirectByteBuffer`, `Inflater`, `Thread` trong heap trỏ thẳng tới nguồn leak off-heap.
- Sửa xong phải xác nhận bằng soak test, cho thấy RSS phẳng.

**Câu hỏi nối tiếp:**
- *Vì sao không bật NMT `detail` thường trực?* → Overhead và lượng bộ nhớ dùng cao hơn `summary`. Chỉ bật khi điều tra.

**⚠️ Câu trả lời gây điểm trừ:**
- "Heap dump để tìm leak." Heap dump không thấy trực tiếp bộ nhớ native.
- "Giảm `-Xmx` là hết."

**📖 Ôn lại:** [Phần 14.3 — Runbook OOM & off-heap leak](../01-giao-trinh/05-jvm-memory-gc-performance.md#p14) · [Phần 3.1 — Bộ nhớ process](../01-giao-trinh/05-jvm-memory-gc-performance.md#p3) · [Checklist tự đánh giá](../01-giao-trinh/05-jvm-memory-gc-performance.md#checklist-tu-danh-gia)

</details>

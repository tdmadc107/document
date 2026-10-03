# Module 05 — JVM Internals, Memory, GC & Performance Tuning

> **Mục tiêu:** sau module này bạn giải thích được JVM nạp, liên kết, khởi tạo class ra sao, bộ nhớ của một process Java được chia thế nào (không chỉ heap), JIT biến bytecode thành mã máy như thế nào và vì sao ứng dụng cần "warm-up"; hiểu bản chất các Garbage Collector (Serial, Parallel, G1, ZGC, Shenandoah) để chọn và tuning có lý do; đọc được GC log; tìm được memory leak bằng heap dump; xử lý được các sự cố production kinh điển: CPU 100%, GC liên tục, `OutOfMemoryError`, container bị OOMKilled; và đo hiệu năng đúng cách bằng JMH, JFR, async-profiler.
> **Cấp độ:** Cơ bản → Nâng cao (Senior)
> **Thời lượng gợi ý:** 7 ngày (≈ 28–32 giờ, trong đó ít nhất một nửa là thực hành với công cụ)
> **Yêu cầu trước:** Module 01 (Java Core & OOP), Module 02 (Collections), Module 03 (Concurrency). Cần biết dùng terminal Linux cơ bản.
> **Nguồn tham khảo:**
> - Trong kho: [`Java/Head First Java 2nd edition.pdf`](../../Java/Head%20First%20Java%202nd%20edition.pdf) — chương 9 "Life and Death of an Object" (constructors & garbage collection) (stack vs heap, vòng đời object, khi nào object đủ điều kiện bị GC)
> - Trong kho: [`Java/OCA_Oracle_Certified_Associate_Java_SE_8.pdf`](../../Java/OCA_Oracle_Certified_Associate_Java_SE_8.pdf) — phần về object lifecycle và garbage collection (kiến thức nền cho câu hỏi "object nào đủ điều kiện GC")
> - Trong kho: [`Java/OCP-Oracle-Certified-Professional-Java-SE-17-Developer-Study-Guide-Exam-1Z0-829pdf.pdf`](../../Java/OCP-Oracle-Certified-Professional-Java-SE-17-Developer-Study-Guide-Exam-1Z0-829pdf.pdf) và [`Java/OCP Oracle Certified Professional Java SE 21.pdf`](../../Java/OCP%20Oracle%20Certified%20Professional%20Java%20SE%2021.pdf) — phần modules/class path, `static` initialization, records
> - Trong kho: [`Ebook IT/Docker - Up _ Running.pdf`](../../Ebook%20IT/Docker%20-%20Up%20_%20Running.pdf) và [`Ebook IT/Kubernetes Microservices with Docker .pdf`](../../Ebook%20IT/Kubernetes%20Microservices%20with%20Docker%20.pdf) — giới hạn tài nguyên container (cgroups), liên quan tới phần JVM trong container
> - Trong kho: [`Ebook IT/Linux Essential.pdf`](../../Ebook%20IT/Linux%20Essential.pdf) — `top`, `ps`, process, signal (dùng trong runbook)
> - Ngoài:
>   - *The Java Virtual Machine Specification* (JVMS), Java SE 21 — chương 2 (Run-Time Data Areas), chương 5 (Loading, Linking, and Initializing): https://docs.oracle.com/javase/specs/jvms/se21/html/
>   - *HotSpot Virtual Machine Garbage Collection Tuning Guide* (Java 21): https://docs.oracle.com/en/java/javase/21/gctuning/
>   - *Java Troubleshooting Guide* (Oracle): https://docs.oracle.com/en/java/javase/21/troubleshoot/
>   - Scott Oaks — *Java Performance: In-Depth Advice for Tuning and Programming Java 8, 11, and Beyond* (2nd ed., O'Reilly)
>   - Ben Evans, James Gough, Chris Newland — *Optimizing Java* (O'Reilly)
>   - Joshua Bloch — *Effective Java* (3rd ed.), Item 6 (Avoid creating unnecessary objects), Item 7 (Eliminate obsolete object references), Item 8 (Avoid finalizers and cleaners)
>   - Aleksey Shipilëv — blog "JVM Anatomy Quarks": https://shipilev.net/jvm/anatomy-quarks/ ; công cụ JOL: https://github.com/openjdk/jol ; JMH: https://github.com/openjdk/jmh
>   - async-profiler: https://github.com/async-profiler/async-profiler ; Eclipse MAT: https://eclipse.dev/mat/
>   - Các JEP: 122 (Remove PermGen), 248 (G1 default), 363 (Remove CMS), 377 (ZGC production), 379 (Shenandoah production), 439 (Generational ZGC), 474 (ZGC generational mode by default), 328 (Flight Recorder), 421 (Deprecate finalization for removal)

## Mục lục
1. [Kiến trúc tổng quan của JVM](#p1)
2. [Class loading: loading, linking, initialization](#p2)
3. [Runtime data areas: heap, Metaspace, stack, code cache, native](#p3)
4. [Object layout, compressed oops & escape analysis](#p4)
5. [Bytecode cơ bản & `javap`](#p5)
6. [JIT compiler: interpreter, C1/C2, tiered compilation, deoptimization](#p6)
7. [Nền tảng Garbage Collection](#p7)
8. [Các Garbage Collector & cách chọn](#p8)
9. [Reference types: strong, soft, weak, phantom & Cleaner](#p9)
10. [Memory leak trong Java & các loại `OutOfMemoryError`](#p10)
11. [JVM flags quan trọng & JVM trong container](#p11)
12. [Bộ công cụ chẩn đoán: jcmd, jstat, jstack, MAT, JFR, async-profiler](#p12)
13. [Phương pháp đo hiệu năng & JMH](#p13)
14. [Runbook xử lý sự cố: CPU 100%, GC cao, OOM](#p14)
15. [Dự án mini của module](#du-an-mini)
16. [Checklist tự đánh giá](#checklist-tu-danh-gia)

---

<a id="p1"></a>
## 1. Kiến trúc tổng quan của JVM

### 1.1 Khái niệm
**JVM (Java Virtual Machine)** là một *đặc tả* (JVMS) mô tả một máy ảo thực thi file `.class` (bytecode). **HotSpot** là bản hiện thực phổ biến nhất (nằm trong OpenJDK, là nền của Oracle JDK, Temurin, Corretto, Zulu...). Các hiện thực khác: OpenJ9 (IBM/Eclipse), GraalVM (HotSpot + Graal JIT, kèm Native Image AOT).

Phân biệt ba khái niệm hay bị hỏi:

| Khái niệm | Gồm gì |
|---|---|
| **JVM** | Class loader subsystem, runtime data areas, execution engine (interpreter + JIT + GC) |
| **JRE** | JVM + thư viện chuẩn (Java SE API). Từ Java 11 không còn phát hành JRE riêng; dùng `jlink` để tạo runtime tối giản |
| **JDK** | JRE + công cụ phát triển: `javac`, `javap`, `jcmd`, `jstack`, `jfr`, `jlink`, `jshell`... |

Sơ đồ khối của HotSpot:

```
            .java ──javac──► .class (bytecode, platform-independent)
                                   │
┌──────────────────────────────────┼─────────────────────────────────────┐
│ JVM process                      ▼                                     │
│  ┌──────────────────────────────────────────────┐                      │
│  │ Class Loader Subsystem                        │                      │
│  │  Loading → Linking (verify, prepare, resolve) │                      │
│  │  → Initialization (<clinit>)                  │                      │
│  └──────────────────────────────────────────────┘                      │
│  ┌───────────────── Runtime Data Areas ─────────────────────────────┐  │
│  │ Shared giữa các thread: Heap | Metaspace (class metadata)        │  │
│  │                          Code Cache (mã máy do JIT sinh ra)       │  │
│  │ Riêng từng thread: JVM Stack | PC register | Native method stack │  │
│  └──────────────────────────────────────────────────────────────────┘  │
│  ┌──────── Execution Engine ───────┐   ┌──────────────────────────┐    │
│  │ Interpreter | JIT (C1, C2) | GC │◄─►│ JNI / Native libraries   │    │
│  └─────────────────────────────────┘   └──────────────────────────┘    │
└────────────────────────────────────────────────────────────────────────┘
```

### 1.2 Bên dưới nắp capo
- **"Write once, run anywhere"** đến từ việc bytecode là một tập lệnh cho máy ảo dạng *stack-based* (khác với máy thật dạng register-based). JVM chịu trách nhiệm dịch sang mã máy của từng nền tảng.
- **Execution engine** bắt đầu bằng *interpreter* (thông dịch từng lệnh bytecode), đồng thời *profiling* để biết method nào "nóng" (hot) rồi đưa cho JIT biên dịch. Đây là lý do một ứng dụng Java chạy chậm vài giây/phút đầu rồi nhanh dần (warm-up).
- **Safepoint**: trạng thái mà mọi Java thread dừng ở vị trí "an toàn" (JVM biết chính xác reference nằm ở đâu trên stack/register). Các thao tác cần dừng thế giới (stop-the-world — STW) như nhiều pha GC, deoptimization, thu thập thread dump, revoke biased lock (đã bị loại bỏ từ JDK 18) đều đi qua safepoint. *Time-to-safepoint* (TTSP) dài là một nguyên nhân gây pause khó hiểu: một thread đang chạy vòng lặp `int` đếm được (counted loop) đã bị JIT bỏ safepoint poll có thể khiến cả JVM chờ.
- Một JVM process có nhiều thread "hệ thống" bạn sẽ thấy trong thread dump: `GC Thread#0..n`, `G1 Conc#0`, `C2 CompilerThread0`, `C1 CompilerThread0`, `VM Thread` (thực hiện các VM operation tại safepoint), `Reference Handler`, `Finalizer`, `Signal Dispatcher`, `Common-Cleaner`...

> 💡 **Góc nhìn Senior:** Khi được hỏi "Java chậm hơn C++ không?", câu trả lời tốt là: *steady-state* throughput của mã đã JIT thường rất gần C/C++ (thậm chí JIT có lợi thế tối ưu theo profile thực tế, ví dụ inline call đa hình chỉ có một implementation thực sự được dùng). Chi phí thật của Java nằm ở: startup/warm-up, footprint bộ nhớ (object header, pointer), và pause của GC. Các hướng giải quyết hiện đại: CDS/AppCDS, Project Leyden (AOT cache — JEP 483 trong JDK 24), CRaC, GraalVM Native Image; ZGC/Shenandoah cho latency.

> ⚠️ **Lỗi thường gặp:** Nói "Java là ngôn ngữ thông dịch". Sai — Java *bắt đầu* bằng thông dịch nhưng code nóng được biên dịch thành mã máy native. Cũng đừng nhầm JVMS (đặc tả) với HotSpot (hiện thực): ví dụ "Metaspace", "C2", "G1" là khái niệm của HotSpot, không có trong JVMS.

### 🛠 Bài tập phần 1

**Bài 1.1 — Nhận diện JVM đang chạy (Cơ bản)**
- Đề bài: Viết chương trình in ra `java.vm.name`, `java.vm.vendor`, `java.version`, `java.runtime.version`, số CPU (`Runtime.availableProcessors()`), `maxMemory()`, `totalMemory()`, `freeMemory()` và GC đang dùng (qua `ManagementFactory.getGarbageCollectorMXBeans()`).
- Tiêu chí đạt: chạy với `-XX:+UseSerialGC`, `-XX:+UseParallelGC`, `-XX:+UseG1GC`, `-XX:+UseZGC` và giải thích vì sao tên các GC bean khác nhau.

**Bài 1.2 — Đếm thread hệ thống (Trung bình)**
- Đề bài: Chạy một chương trình `Thread.sleep(Long.MAX_VALUE)`, dùng `jcmd <pid> Thread.print` để liệt kê tất cả thread. Phân loại thread nào là của GC, JIT, VM.
- Tiêu chí đạt: bảng phân loại; giải thích vai trò `VM Thread`, `Reference Handler`, `Signal Dispatcher`. So sánh số GC thread khi chạy với `-XX:ParallelGCThreads=2`.

**Bài 1.3 — Quan sát time-to-safepoint (Nâng cao)**
- Đề bài: Chạy với `-Xlog:safepoint` (JDK 17+), tạo một thread chạy counted loop dài trên `int` và một thread khác liên tục cấp phát để kích hoạt GC. Đo "Reaching safepoint" time. Sau đó đổi biến lặp sang `long` và so sánh.
- Tiêu chí đạt: giải thích kết quả dựa trên khái niệm safepoint poll và loop strip mining (`-XX:+UseCountedLoopSafepoints`, `-XX:LoopStripMiningIter`; được bật mặc định cùng G1 từ JDK 10 và với các collector low-latency về sau).

<details>
<summary>Gợi ý lời giải</summary>

```java
import java.lang.management.*;

public class JvmInfo {
    public static void main(String[] args) {
        System.out.println(System.getProperty("java.vm.name") + " / " + System.getProperty("java.vm.vendor"));
        System.out.println("Java " + System.getProperty("java.runtime.version"));
        Runtime rt = Runtime.getRuntime();
        System.out.printf("CPUs=%d max=%dMB total=%dMB free=%dMB%n",
                rt.availableProcessors(), rt.maxMemory() >> 20, rt.totalMemory() >> 20, rt.freeMemory() >> 20);
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            System.out.println("GC bean: " + gc.getName() + " pools=" + String.join(",", gc.getMemoryPoolNames()));
        }
    }
}
```
Kết quả mong đợi: Serial → `Copy` + `MarkSweepCompact`; Parallel → `PS Scavenge` + `PS MarkSweep`; G1 → `G1 Young Generation`, `G1 Old Generation` (và `G1 Concurrent GC` từ JDK 20); ZGC → `ZGC Cycles`/`ZGC Pauses` (hoặc `ZGC Minor/Major ...` với generational ZGC). Mỗi collector đăng ký bean riêng cho từng loại chu kỳ.

Bài 1.3: với JDK hiện đại, loop strip mining chèn safepoint poll sau mỗi "strip" (mặc định 1000 vòng) nên TTSP thường nhỏ. Thử thêm `-XX:-UseCountedLoopSafepoints` để thấy TTSP vọt lên.

</details>

---

<a id="p2"></a>
## 2. Class loading: loading, linking, initialization

### 2.1 Khái niệm
Một class đi qua 3 giai đoạn (JVMS chương 5):

1. **Loading** — tìm dữ liệu nhị phân của class (từ file, jar, mạng, sinh động...), tạo cấu trúc nội bộ trong Metaspace và đối tượng `java.lang.Class` trên heap.
2. **Linking**
   - **Verification**: kiểm tra bytecode hợp lệ, an toàn kiểu (stack map frames). Đây là lý do bytecode "tự viết" sai sẽ ném `VerifyError`.
   - **Preparation**: cấp phát bộ nhớ cho biến `static` và gán **giá trị mặc định** (`0`, `null`, `false`) — *chưa* chạy code khởi tạo.
   - **Resolution**: thay symbolic reference (tên class/method/field trong constant pool) bằng direct reference. HotSpot làm việc này *lười* (lazy) — chỉ khi lệnh bytecode lần đầu được thực thi.
3. **Initialization** — chạy method `<clinit>`: các phép gán `static` và khối `static {}` theo thứ tự xuất hiện trong source.

Initialization chỉ xảy ra khi có **active use** đầu tiên: `new`, gọi static method, đọc/ghi static field (trừ hằng số compile-time), reflection (`Class.forName(name)` mặc định có initialize), khởi tạo subclass (class cha được init trước), class chứa `main`. Những thứ **không** kích hoạt init: khai báo biến kiểu đó, tạo mảng `Foo[]`, truy cập `static final` là hằng compile-time (được inline vào class gọi), `Foo.class`, `ClassLoader.loadClass()`.

```java
public class InitOrder {
    static class Config {
        static final int CONST = 42;                 // hằng compile-time: inline, KHÔNG trigger init
        static final String NAME = "cfg";            // String literal cũng là hằng compile-time
        static final Integer BOXED = 7;              // KHÔNG phải hằng compile-time → trigger init
        static { System.out.println("Config <clinit> chạy"); }
    }

    public static void main(String[] args) throws Exception {
        System.out.println(Config.CONST);            // không in "<clinit> chạy"
        Config[] arr = new Config[3];                // không init
        Class<?> c = Config.class;                   // không init
        ClassLoader.getSystemClassLoader().loadClass("InitOrder$Config"); // load nhưng không init
        System.out.println(Config.BOXED);            // BÂY GIỜ mới init
    }
}
```

### 2.2 Mô hình ủy quyền (parent delegation)
Từ Java 9 (JPMS) có ba class loader dựng sẵn:

| Loader | Nạp gì | Ghi chú |
|---|---|---|
| **Bootstrap** | `java.base` và các module lõi | Viết bằng C++, `getClassLoader()` trả về `null` |
| **Platform** (Java 9+, thay cho *Extension* loader của Java 8) | Một số module Java SE/JDK như `java.sql` | Java 8: `ExtClassLoader` nạp từ `jre/lib/ext` (đã bị bỏ) |
| **Application / System** | Class path và module path của ứng dụng | `ClassLoader.getSystemClassLoader()` |

Thuật toán `loadClass` mặc định: (1) đã nạp chưa (`findLoadedClass`)? (2) chưa → hỏi **parent** trước; (3) parent không tìm được → tự `findClass`. Lợi ích: không ai "giả mạo" được `java.lang.String`, và class lõi chỉ được nạp một lần.

**Định danh của class = (tên đầy đủ, defining class loader).** Cùng file `com.acme.User` nạp bởi hai loader khác nhau là **hai class khác nhau** → `ClassCastException: com.acme.User cannot be cast to com.acme.User` — lỗi kinh điển trong app server, hot reload, plugin.

```java
import java.net.*;
import java.nio.file.*;

public class TwoLoaders {
    public static void main(String[] args) throws Exception {
        URL[] cp = { Path.of("plugins/").toUri().toURL() };
        try (URLClassLoader l1 = new URLClassLoader(cp, null);   // parent = null → chỉ bootstrap
             URLClassLoader l2 = new URLClassLoader(cp, null)) {
            Class<?> a = l1.loadClass("com.acme.Plugin");
            Class<?> b = l2.loadClass("com.acme.Plugin");
            System.out.println(a == b);                          // false
            System.out.println(a.getName().equals(b.getName())); // true
            Object o = a.getDeclaredConstructor().newInstance();
            System.out.println(b.isInstance(o));                 // false
        }
    }
}
```

### 2.3 Custom class loader & phá vỡ delegation
Khi nào cần custom class loader: plugin system, hot reload (Spring DevTools dùng *RestartClassLoader*), cô lập dependency (Tomcat mỗi webapp một `WebappClassLoader`), nạp class được mã hóa/sinh động, instrumentation.

- Muốn giữ delegation: override **`findClass`** (không override `loadClass`).
- Tomcat webapp loader **đảo** thứ tự: tìm trong `WEB-INF/classes` và `WEB-INF/lib` trước, rồi mới hỏi parent (trừ class `java.*`), để webapp được dùng phiên bản thư viện riêng.
- **Thread context class loader (TCCL)**: cơ chế cho code ở loader cha (ví dụ `ServiceLoader`, JDBC `DriverManager`, JNDI) nạp class ở loader con. Quên set/reset TCCL là nguồn gốc nhiều lỗi `ClassNotFoundException` trong framework.

```java
import java.io.IOException;
import java.nio.file.*;

/** Nạp class từ một thư mục riêng, vẫn tôn trọng parent delegation. */
public class DirClassLoader extends ClassLoader {
    private final Path root;

    public DirClassLoader(Path root, ClassLoader parent) {
        super("dir-loader", parent);   // Java 9+: class loader có tên → dễ đọc trong heap dump/stack trace
        this.root = root;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        Path file = root.resolve(name.replace('.', '/') + ".class");
        try {
            byte[] bytes = Files.readAllBytes(file);
            return defineClass(name, bytes, 0, bytes.length);
        } catch (IOException e) {
            throw new ClassNotFoundException(name, e);
        }
    }
}
```

### 2.4 Class unloading & classloader leak
Một class chỉ được **unload** khi *class loader định nghĩa nó* không còn reachable — kéo theo mọi class của loader đó, mọi instance và mọi `Class` object không còn reachable. Class của bootstrap/platform/app loader **không bao giờ** bị unload.

**Classloader leak** (hay gặp khi redeploy webapp trên Tomcat, hot reload, plugin): chỉ cần **một** reference từ "bên ngoài" trỏ vào một object/class của loader cũ là *toàn bộ* các class của webapp cũ (hàng nghìn class, Metaspace vài chục–trăm MB) bị giữ lại. Nguồn gốc điển hình:
- `ThreadLocal` trên thread của pool dùng chung (thread sống lâu hơn webapp) giữ value là object của webapp.
- Thread do webapp tạo không được dừng (timer, scheduler) — thread giữ TCCL = webapp loader.
- JDBC driver đăng ký vào `DriverManager` (nạp ở loader cha) không được deregister.
- Cache static ở thư viện dùng chung (nằm ở loader cha) giữ object của webapp; `java.beans.Introspector` cache; logging framework; shutdown hook.

Triệu chứng: sau N lần redeploy → `OutOfMemoryError: Metaspace`. Tomcat có log cảnh báo "The web application [...] appears to have started a thread named [...] but has failed to stop it".

> 💡 **Góc nhìn Senior:**
> - Khởi tạo class được JVM bảo đảm **thread-safe** (JVMS §5.5 — mỗi class có initialization lock). Đây là nền tảng của *Initialization-on-demand holder idiom* cho singleton lazy mà không cần `synchronized`/`volatile`.
> - Nếu `<clinit>` ném exception: lần đầu nhận `ExceptionInInitializerError`, các lần sau nhận **`NoClassDefFoundError: Could not initialize class X`** — lỗi gây hoang mang vì class rõ ràng có trên classpath. Hãy tìm log của lần lỗi *đầu tiên*.
> - `ClassNotFoundException` (checked, khi nạp động bằng tên: `Class.forName`, `loadClass`) khác `NoClassDefFoundError` (Error, class có lúc compile nhưng không có/không init được lúc runtime). Câu hỏi phỏng vấn rất phổ biến.
> - Deadlock khi khởi tạo class: hai class có `static` initializer phụ thuộc vòng nhau và được init đồng thời từ hai thread → treo vĩnh viễn, thread dump hiển thị trạng thái `RUNNABLE` nhưng đứng ở `<clinit>`.

> ⚠️ **Lỗi thường gặp:**
> - Nghĩ `static final` nào cũng là hằng: chỉ primitive/String với initializer là *constant expression* mới được inline. Hệ quả ngược: đổi giá trị hằng trong thư viện mà không compile lại code gọi → code gọi vẫn dùng giá trị cũ.
> - Fat jar có hai phiên bản cùng một thư viện → class nào được nạp phụ thuộc thứ tự classpath → `NoSuchMethodError` ở runtime. Dùng `mvn dependency:tree` / `-verbose:class` / `-Xlog:class+load` để truy vết.

### 🛠 Bài tập phần 2

**Bài 2.1 — Dự đoán thứ tự khởi tạo (Cơ bản)**
- Đề bài: Viết class cha/con, mỗi class có static block, instance initializer block, constructor, static field gán bằng method có in log. Dự đoán output trước khi chạy `new Child()` hai lần.
- Tiêu chí đạt: dự đoán đúng 100%; giải thích bằng thuật ngữ `<clinit>` và `<init>`.

**Bài 2.2 — Plugin loader có hot reload (Trung bình)**
- Đề bài: Định nghĩa interface `Greeter` (nạp bởi app loader). Viết `PluginHost` nạp implementation từ thư mục `plugins/` bằng `URLClassLoader` mới mỗi khi file `.class` thay đổi (polling 2 giây). Gọi `greet()` mỗi giây.
- Tiêu chí đạt: sửa và compile lại plugin → output đổi mà không restart; interface `Greeter` phải dùng chung (nếu nạp trùng bởi plugin loader sẽ gặp `ClassCastException` — giải thích vì sao); đóng (`close()`) loader cũ.

**Bài 2.3 — Tái hiện và sửa classloader leak (Nâng cao)**
- Đề bài: Từ bài 2.2, trong plugin lưu một object vào `static ThreadLocal` của host trên một thread pool dùng chung và không `remove()`. Reload 200 lần với `-XX:MaxMetaspaceSize=64m`, quan sát `jcmd <pid> VM.classloader_stats` và `jstat -gcmetacapacity`.
- Tiêu chí đạt: tái hiện được `OutOfMemoryError: Metaspace` (hoặc số loader tăng mãi); dùng heap dump + MAT ("Path to GC Roots" từ `DirClassLoader`/`URLClassLoader` cũ) chỉ ra chuỗi reference giữ loader; sửa và chứng minh số class loader ổn định.

<details>
<summary>Gợi ý lời giải</summary>

Bài 2.1 — thứ tự cho lần `new Child()` đầu tiên: static của `Parent` → static của `Child` → instance init + constructor của `Parent` → instance init + constructor của `Child`. Lần thứ hai: bỏ qua toàn bộ phần static.

Bài 2.2 — điểm then chốt:
```java
URLClassLoader loader = new URLClassLoader(new URL[]{pluginDir.toUri().toURL()},
        Greeter.class.getClassLoader()); // parent = loader của interface → Greeter dùng chung
Greeter g = loader.loadClass("com.acme.HelloGreeter")
        .asSubclass(Greeter.class).getDeclaredConstructor().newInstance();
```
Không đặt `Greeter.class` vào thư mục `plugins/` — nếu có, với parent delegation thì parent vẫn thắng; nhưng nếu dùng loader với parent `null` thì sẽ có hai `Greeter` khác nhau.

Bài 2.3 — chuỗi giữ thường thấy trong MAT: `Thread (pool) → threadLocals (ThreadLocalMap) → Entry.value → PluginObject → class PluginObject → URLClassLoader`. Sửa: `try { ... } finally { TL.remove(); }`, hoặc không cho plugin truy cập ThreadLocal của host, shutdown executor riêng của plugin khi unload.

</details>

---

<a id="p3"></a>
## 3. Runtime data areas: heap, Metaspace, stack, code cache, native

### 3.1 Khái niệm
JVMS định nghĩa các vùng dữ liệu runtime; HotSpot hiện thực như sau:

| Vùng | Phạm vi | Chứa gì | Lỗi khi cạn |
|---|---|---|---|
| **Heap** | Chia sẻ | Mọi object và mảng; `Class` mirror; String pool (từ Java 7) | `OutOfMemoryError: Java heap space` |
| **Metaspace** (Java 8+) | Chia sẻ, **native memory** | Metadata của class: method, bytecode, constant pool, annotation... | `OutOfMemoryError: Metaspace` / `Compressed class space` |
| **JVM Stack** | Mỗi thread | Stack frame cho mỗi lần gọi method | `StackOverflowError` (sâu quá), `OOM: unable to create native thread` (không tạo nổi stack mới) |
| **PC register** | Mỗi thread | Địa chỉ lệnh bytecode đang thực thi (undefined khi chạy native) | — |
| **Native method stack** | Mỗi thread | Stack cho code native (JNI). HotSpot gộp với Java stack | `StackOverflowError` |
| **Code cache** | Chia sẻ, native | Mã máy do JIT sinh ra, các stub | Cảnh báo "CodeCache is full. Compiler has been disabled" — JIT ngừng, app chậm hẳn |
| **Direct / native memory khác** | Chia sẻ | `ByteBuffer.allocateDirect`, thread stack, GC data structures, JNI, `Unsafe`, mmap | `OOM: Direct buffer memory`, hoặc process bị OS/container kill |

```
Bộ nhớ process Java (RSS) ≈ Heap (-Xmx)
                          + Metaspace + Compressed class space
                          + Thread stacks (số thread × -Xss)
                          + Code cache (tới 240MB khi bật tiered)
                          + GC overhead (card table, remembered sets của G1: có thể vài % → >10% heap)
                          + Direct buffers (-XX:MaxDirectMemorySize, mặc định ≈ -Xmx)
                          + Symbol table, internal, JNI, malloc arenas (glibc) ...
```

### 3.2 Heap và thế hệ (generations)
Với các collector *generational* truyền thống (Serial, Parallel, G1 theo nghĩa logic):

```
┌──────────────── Young Generation ────────────────┬──────── Old (Tenured) ────────┐
│  Eden            │ Survivor S0 │ Survivor S1      │  object sống lâu, object lớn  │
│ (cấp phát mới)   │  (from)     │   (to)           │                               │
└──────────────────────────────────────────────────┴───────────────────────────────┘
```
- Object mới cấp phát trong **Eden**, cụ thể là trong **TLAB** (Thread-Local Allocation Buffer) của từng thread: cấp phát chỉ là *bump-the-pointer* (tăng con trỏ) — không cần lock, rẻ ngang malloc tốt nhất, thường chỉ vài chục ns.
- **Minor/Young GC**: copy object còn sống từ Eden + survivor "from" sang survivor "to"; mỗi lần sống sót tăng *age*; đạt `MaxTenuringThreshold` (tối đa 15 vì age lưu 4 bit trong mark word) hoặc survivor đầy → **promote** lên Old.
- Tỉ lệ mặc định (Parallel/Serial): `-XX:NewRatio=2` (Old:Young = 2:1), `-XX:SurvivorRatio=8` (Eden:S0:S1 = 8:1:1). G1 tự điều chỉnh kích thước young theo pause target — **không nên** set `-Xmn`/`NewRatio` với G1 vì sẽ vô hiệu hóa cơ chế này.

### 3.3 PermGen → Metaspace (Java 8, JEP 122)
| | PermGen (≤ Java 7) | Metaspace (Java 8+) |
|---|---|---|
| Vị trí | Một phần của heap (liền kề) | Native memory |
| Kích thước | Cố định `-XX:MaxPermSize` (mặc định nhỏ, ~64–82MB) → `OOM: PermGen space` rất phổ biến | Mặc định **không giới hạn** (`-XX:MaxMetaspaceSize`), tăng dần; GC trigger khi chạm `MetaspaceSize` (high-water mark) |
| Interned strings, static | Ở PermGen (Java 6) | String pool đã chuyển ra heap từ Java 7; static field nằm trong `Class` mirror trên heap |

Với compressed class pointers, metadata `Klass` nằm trong vùng con **Compressed Class Space** (`-XX:CompressedClassSpaceSize`, mặc định 1GB) — có thể hết riêng vùng này → `OOM: Compressed class space`. JDK 16 (JEP 387, "Elastic Metaspace") cải thiện việc trả bộ nhớ Metaspace về OS.

### 3.4 Stack frame
Mỗi lần gọi method, JVM push một frame gồm:
- **Local variable array**: `this` (slot 0 với instance method), tham số, biến cục bộ. `long`/`double` chiếm 2 slot.
- **Operand stack**: nơi các lệnh bytecode đẩy/lấy toán hạng.
- **Frame data**: tham chiếu tới runtime constant pool của class, thông tin return, exception table.

Kích thước stack mỗi thread: `-Xss` (mặc định 1MB trên Linux x64; Windows lấy theo binary, thường 1MB). Đệ quy sâu → `StackOverflowError`. Lưu ý: bộ nhớ stack được *reserve* 1MB nhưng chỉ *commit* phần đã chạm tới, nên 1000 thread không chắc tốn 1GB RSS — nhưng tốn địa chỉ ảo và bị giới hạn bởi `ulimit -u`, `pids.max` của cgroup, `kernel.threads-max`.

**Virtual threads (Java 21)**: stack của virtual thread được lưu dưới dạng *stack chunk object* trên **heap** khi unmount, nên tạo hàng triệu virtual thread không tốn native stack — nhưng làm tăng áp lực heap/GC nếu stack sâu.

### 3.5 Code cache
Từ JDK 9 (JEP 197) code cache được **phân đoạn**: *non-method* (stub, interpreter), *profiled* (code C1 có profiling), *non-profiled* (code C2 tối ưu hoàn toàn). Mặc định `-XX:ReservedCodeCacheSize=240m` khi bật tiered compilation. Ứng dụng lớn (nhiều class, Groovy/scripting, nhiều lambda proxy) có thể làm đầy → JIT bị tắt → CPU tăng, latency tăng dần không rõ nguyên nhân. Theo dõi: `jcmd <pid> Compiler.codecache`.

> 💡 **Góc nhìn Senior:** Câu "set `-Xmx` bằng memory limit của container" là **sai** và là nguyên nhân số 1 của `OOMKilled` (exit code 137). RSS = heap + rất nhiều thứ ngoài heap. Quy tắc thực tế: heap ≈ 50–75% limit (`-XX:MaxRAMPercentage=75` cho container ≥ 1–2GB, thấp hơn cho container nhỏ), phần còn lại cho metaspace, thread, direct buffer (Netty!), code cache. Dùng **Native Memory Tracking** (`-XX:NativeMemoryTracking=summary` + `jcmd <pid> VM.native_memory summary`) để có số liệu thật thay vì đoán.

> ⚠️ **Lỗi thường gặp:**
> - "Primitive nằm trên stack, object nằm trên heap" — chỉ đúng một nửa: primitive là *field* của object nằm trên heap cùng object; biến cục bộ kiểu reference nằm trên stack nhưng object nó trỏ tới nằm trên heap (trừ khi escape analysis loại bỏ việc cấp phát).
> - Nghĩ static variable "nằm trong Metaspace": từ Java 8, giá trị static field nằm trong `java.lang.Class` mirror trên heap.

### 🛠 Bài tập phần 3

**Bài 3.1 — Gây và đọc từng loại lỗi (Cơ bản)**
- Đề bài: Viết 3 chương trình nhỏ gây ra: `StackOverflowError` (đệ quy), `OutOfMemoryError: Java heap space` (`-Xmx64m`, thêm vào `List<byte[]>`), `OutOfMemoryError: Metaspace` (`-XX:MaxMetaspaceSize=32m`, sinh class động bằng nhiều `ClassLoader` hoặc `java.lang.reflect.Proxy`/ByteBuddy).
- Tiêu chí đạt: in độ sâu đệ quy đạt được với `-Xss256k` và `-Xss2m`, giải thích quan hệ; chụp lại message lỗi đầy đủ.

**Bài 3.2 — Dự toán RSS (Trung bình)**
- Đề bài: Chạy một ứng dụng Spring Boot (hoặc app bất kỳ có ~200 thread) với `-Xmx512m -XX:NativeMemoryTracking=summary`. So sánh `jcmd VM.native_memory summary` với RSS từ `ps -o rss`.
- Tiêu chí đạt: bảng các thành phần (Java Heap, Class, Thread, Code, GC, Internal, Other...); kết luận heap chiếm bao nhiêu % RSS; đề xuất memory limit container hợp lý.

**Bài 3.3 — Direct memory (Nâng cao)**
- Đề bài: Cấp phát lặp `ByteBuffer.allocateDirect(1MB)` giữ trong list, chạy với `-Xmx128m -XX:MaxDirectMemorySize=64m`. Sau đó bỏ giới hạn `MaxDirectMemorySize`, chạy trong Docker với `--memory=256m`.
- Tiêu chí đạt: trường hợp 1 nhận `OOM: Direct buffer memory`; trường hợp 2 giải thích vì sao container có thể bị OOMKilled thay vì nhận exception (mặc định MaxDirectMemorySize ≈ max heap). Liên hệ Netty (`io.netty.maxDirectMemory`).

<details>
<summary>Gợi ý lời giải</summary>

```java
public class DeepRecursion {
    static int depth = 0;
    static void recurse() { depth++; recurse(); }
    public static void main(String[] args) {
        try { recurse(); } catch (StackOverflowError e) { System.out.println("depth=" + depth); }
    }
}
```
Độ sâu tỉ lệ gần tuyến tính với `-Xss`, nhưng dao động giữa các lần chạy vì frame của code interpreted lớn hơn frame của code đã JIT (và inlining làm giảm số frame).

Metaspace OOM nhanh nhất: vòng lặp tạo `new URLClassLoader(...)` mới và nạp lại cùng một class, giữ loader trong list.

Bài 3.2: thường thấy heap chỉ ~55–70% RSS cho app Spring Boot cỡ trung bình; Class (metaspace) 80–150MB, Thread ≈ số thread × (stack thực dùng), Code 30–60MB, GC (G1) vài chục MB.

</details>

---

<a id="p4"></a>
## 4. Object layout, compressed oops & escape analysis

### 4.1 Khái niệm: một object Java tốn bao nhiêu byte?
Trên HotSpot 64-bit (mặc định, heap < 32GB):

```
┌──────────────────────┬──────────────────┬──────────────┬──────────┐
│ Mark word (8 byte)   │ Klass ptr (4 byte│ fields ...   │ padding  │
│ hash, age, lock bits │ compressed)      │              │ tới bội 8│
└──────────────────────┴──────────────────┴──────────────┴──────────┘
Mảng: thêm 4 byte length sau klass pointer.
```

| Object | Kích thước (compressed oops + compressed class pointers) |
|---|---|
| `new Object()` | 16 byte (12 header + 4 padding) |
| `Integer` | 16 byte (12 header + 4 int) |
| `Long` | 24 byte (12 + 8 → 20, làm tròn 24) |
| `new byte[0]` | 16 byte |
| `new int[10]` | 16 + 40 = 56 byte |
| `String "hello"` (Java 9+, compact strings, LATIN1) | `String` 24 byte + `byte[5]` 24 byte = ~48 byte |
| Mỗi entry `HashMap<Integer,Integer>` | `Node` 32 byte + key `Integer` 16 + value `Integer` 16 + slot trong mảng table 4 byte ≈ 68 byte cho 8 byte "dữ liệu thật" |

Kiểm chứng bằng JOL:
```java
// Dependency: org.openjdk.jol:jol-core:0.17
import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.info.GraphLayout;
import java.util.*;

public class LayoutDemo {
    record Point(int x, int y) {}
    public static void main(String[] args) {
        System.out.println(ClassLayout.parseInstance(new Point(1, 2)).toPrintable());
        Map<Integer, Integer> m = new HashMap<>();
        for (int i = 0; i < 1000; i++) m.put(i + 1000, i + 1000); // > 127 để tránh Integer cache
        System.out.println(GraphLayout.parseInstance(m).toFootprint());
    }
}
```

### 4.2 Compressed oops
**OOP** = ordinary object pointer. Trên 64-bit, pointer 8 byte làm phình bộ nhớ ~ 30–50% so với 32-bit. HotSpot dùng **compressed oops**: lưu reference dạng 32-bit *offset chia 8* (vì object align 8 byte) → địa chỉ thật = `base + (oop << 3)` → đánh địa chỉ được 2³² × 8 = **32GB**.

Hệ quả cần nhớ:
- Heap **≤ ~31GB**: compressed oops bật (mặc định). Heap **> 32GB**: tự tắt → mọi reference thành 8 byte. Heap 32–40GB có thể chứa **ít** object hơn heap 31GB! Nếu cần heap lớn, nhảy hẳn lên ≥ 48GB, hoặc tăng `-XX:ObjectAlignmentInBytes=16` (đánh địa chỉ 64GB nhưng tốn padding).
- Kiểm tra: `java -Xmx31g -XX:+PrintFlagsFinal -version | grep UseCompressedOops`.
- Tùy base address, JVM chọn chế độ *zero-based* (heap < 4GB: không cần shift; < 32GB: shift không cần cộng base) — nhanh hơn chế độ có base.
- **Compact Object Headers** (Project Lilliput): JEP 450 (experimental, JDK 24), JEP 519 (product, JDK 25) — gộp mark word và klass pointer còn 8 byte (`-XX:+UseCompactObjectHeaders`), giảm footprint đáng kể cho ứng dụng nhiều object nhỏ.

### 4.3 Escape analysis
C2 phân tích xem một object có "thoát" (escape) ra khỏi method/thread hay không:
- **NoEscape**: object chỉ dùng cục bộ → **scalar replacement**: không cấp phát object, các field được thay bằng biến cục bộ/register. (HotSpot không thực sự "cấp phát trên stack" — nó loại bỏ hẳn việc cấp phát.)
- **ArgEscape**: truyền vào method khác nhưng không thoát thread → có thể **lock elision** (bỏ `synchronized` trên object không thể bị thread khác thấy).
- **GlobalEscape**: gán vào field static/heap, return ra ngoài → phải cấp phát bình thường.

```java
public class EscapeDemo {
    record Vec(double x, double y) {
        Vec plus(Vec o) { return new Vec(x + o.x, y + o.y); }
    }

    // Sau khi JIT (C2) + inline plus(), các Vec trung gian thường bị scalar-replace → 0 allocation
    static double sumLength(int n) {
        double total = 0;
        for (int i = 0; i < n; i++) {
            Vec v = new Vec(i, i).plus(new Vec(1, 1));   // NoEscape
            total += Math.sqrt(v.x() * v.x() + v.y() * v.y());
        }
        return total;
    }

    public static void main(String[] args) {
        long before = allocated();
        double r = 0;
        for (int k = 0; k < 50; k++) r += sumLength(1_000_000);
        System.out.printf("result=%.1f allocatedMB=%d%n", r, (allocated() - before) >> 20);
    }

    static long allocated() {
        var bean = (com.sun.management.ThreadMXBean) java.lang.management.ManagementFactory.getThreadMXBean();
        return bean.getThreadAllocatedBytes(Thread.currentThread().getId());
    }
}
```
Chạy lại với `-XX:-DoEscapeAnalysis` để thấy lượng cấp phát tăng vọt (hàng GB).

> 💡 **Góc nhìn Senior:**
> - Escape analysis **mong manh**: phụ thuộc inlining (method quá lớn, call site megamorphic → không inline → object "thoát" qua tham số), phụ thuộc control flow (object được merge từ hai nhánh), và chỉ áp dụng cho code đã được C2 biên dịch. Đừng thiết kế dựa vào nó; hãy *đo* bằng JFR allocation profiling / async-profiler `-e alloc`.
> - Footprint quyết định hiệu năng nhiều hơn bạn nghĩ: `List<Integer>` 1 triệu phần tử ≈ 20MB, `int[]` cùng số phần tử ≈ 4MB và thân thiện cache CPU hơn nhiều. Với dữ liệu lớn, cân nhắc primitive collections (Eclipse Collections, fastutil, HPPC), mảng song song, hoặc chờ Project Valhalla (value classes).
> - Mark word còn chứa identity hash code (sau khi gọi `System.identityHashCode`/`Object.hashCode` mặc định) và trạng thái lock — đó là lý do lock trên object đã gọi hashCode từng phải *inflate* trong thời biased locking (biased locking bị disable từ JDK 15, loại bỏ ở JDK 18).

> ⚠️ **Lỗi thường gặp:** Ước lượng bộ nhớ cache bằng "số phần tử × kích thước dữ liệu". Hãy cộng header, pointer, wrapper, node của collection — thực tế thường gấp 3–6 lần. Đo bằng JOL `GraphLayout` hoặc retained size trong MAT.

### 🛠 Bài tập phần 4

**Bài 4.1 — Đo kích thước object (Cơ bản)**
- Đề bài: Dùng JOL in layout của: `Object`, `Integer`, `Long`, `String` rỗng, `String` 10 ký tự Latin và 10 ký tự tiếng Việt có dấu, `int[0]`, `Integer[10]`, record `Point(int,int)`, class có field `boolean, long, byte, Object`.
- Tiêu chí đạt: giải thích field reordering (JVM sắp xếp lại field để giảm padding) và vì sao chuỗi tiếng Việt tốn gấp đôi (UTF16 coder thay vì LATIN1).

**Bài 4.2 — Ngưỡng 32GB (Trung bình)**
- Đề bài: Chạy `java -Xmx<N>g -XX:+PrintFlagsFinal -version` với N = 31, 32, 33 (có thể thêm `-Xlog:gc+heap+coops=debug`) và báo cáo `UseCompressedOops` cùng chế độ narrow oop.
- Tiêu chí đạt: báo cáo đúng; tính toán được với cùng 1 tỉ reference, chênh lệch bộ nhớ là bao nhiêu.

**Bài 4.3 — Phá vỡ escape analysis (Nâng cao)**
- Đề bài: Từ `EscapeDemo`, tạo 3 biến thể khiến allocation quay lại: (a) lưu `Vec` vào field static một lần mỗi 1000 vòng; (b) gọi `plus` thông qua interface có 3 implementation được dùng luân phiên (megamorphic); (c) đặt `-XX:MaxInlineSize`/`-XX:FreqInlineSize` rất nhỏ.
- Tiêu chí đạt: mỗi biến thể có số liệu allocated MB; giải thích nguyên nhân theo NoEscape/ArgEscape/GlobalEscape và inlining.

<details>
<summary>Gợi ý lời giải</summary>

Bài 4.1: class `{boolean a; long b; byte c; Object d;}` — JOL cho thấy JVM đặt `long` trước (hoặc lấp khoảng trống sau header 12 byte bằng field 4 byte/byte nhỏ), tổng thường 24 byte. `String` 10 ký tự Latin: `byte[10]` = 16+10 → 32 byte; tiếng Việt có dấu (ngoài Latin-1, như "ấ", "ệ") → coder UTF16 → `byte[20]` = 40 byte.

Bài 4.2: 1 tỉ reference × (8 − 4) byte = ~4GB chênh lệch chỉ riêng pointer, chưa kể padding.

Bài 4.3 (b): call site thấy ≥ 3 kiểu nhận → megamorphic → C2 dùng virtual call (vtable/itable) thay vì inline → object truyền qua tham số bị coi là escape. Kiểm tra bằng `-XX:+UnlockDiagnosticVMOptions -XX:+PrintInlining` (xem dòng "not inline" / "too big" / "megamorphic").

</details>

---

<a id="p5"></a>
## 5. Bytecode cơ bản & `javap`

### 5.1 Khái niệm
Bytecode là tập ~200 opcode, mỗi opcode 1 byte. Một số nhóm thường gặp:

| Nhóm | Ví dụ |
|---|---|
| Load/store biến cục bộ | `iload_1`, `aload_0` (this), `istore_2`, `astore` |
| Hằng số | `iconst_0..5`, `bipush`, `sipush`, `ldc` (từ constant pool) |
| Số học | `iadd`, `lmul`, `ddiv`, `iinc` |
| Object | `new`, `getfield`, `putfield`, `getstatic`, `checkcast`, `instanceof` |
| Gọi method | `invokestatic`, `invokevirtual` (method instance thường), `invokeinterface`, `invokespecial` (constructor, private, `super.`), `invokedynamic` (lambda, string concat Java 9+, record `toString/equals/hashCode`, pattern switch) |
| Điều khiển | `if_icmpge`, `goto`, `tableswitch`, `lookupswitch`, `ireturn`, `athrow` |
| Đồng bộ | `monitorenter`, `monitorexit` (cho khối `synchronized`; method `synchronized` chỉ là cờ `ACC_SYNCHRONIZED`) |

### 5.2 Đọc bytecode bằng `javap`
```java
public class Bc {
    private int count;
    public int inc(int delta) { count += delta; return count; }
    public String greet(String name) { return "Hi " + name + "!"; }
    public Runnable task() { return () -> System.out.println(count); }
    public void sync(Object lock) { synchronized (lock) { count++; } }
}
```
```
$ javac Bc.java && javap -c -p -v Bc
  public int inc(int);
       0: aload_0
       1: dup
       2: getfield      #7   // Field count:I
       5: iload_1
       6: iadd
       7: putfield      #7   // Field count:I
      10: aload_0
      11: getfield      #7
      14: ireturn

  public java.lang.String greet(java.lang.String);
       0: aload_1
       1: invokedynamic #13,  0   // InvokeDynamic #0:makeConcatWithConstants  (JEP 280, Java 9+)
       6: areturn

  public java.lang.Runnable task();
       0: aload_0
       1: invokedynamic #17,  0   // InvokeDynamic #1:run:(LBc;)Ljava/lang/Runnable;
       6: areturn
  private void lambda$task$0();   // thân lambda được compile thành private method tổng hợp

  public void sync(java.lang.Object);
       ... monitorenter ... monitorexit ... (thêm một monitorexit trong exception handler)
```

Những điều đọc được từ bytecode:
- `count += delta` là **read–modify–write ba bước** (`getfield`, `iadd`, `putfield`) → không atomic → cơ sở để giải thích race condition (Module 03).
- Java 8 compile `"Hi " + name` thành `StringBuilder.append(...)`; Java 9+ dùng `invokedynamic` + `StringConcatFactory` — vẫn nên dùng `StringBuilder` thủ công khi nối trong **vòng lặp**.
- Lambda **không** tạo file `.class` ẩn danh như anonymous class; class được sinh lúc runtime qua `LambdaMetafactory` (hidden class từ Java 15). Lambda không capture được cache thành singleton.
- `synchronized` block sinh hai `monitorexit` (đường bình thường và đường exception) để luôn nhả lock.

> 💡 **Góc nhìn Senior:** Bạn không cần thuộc opcode, nhưng cần *đọc được* `javap -c` để trả lời các câu như "`i++` có atomic không?", "lambda khác anonymous class thế nào?", "`switch` trên String hoạt động ra sao?" (`hashCode()` + `lookupswitch` + `equals`), "`try-finally` với `return` thì sao?" (khối finally được *nhân bản* vào mọi đường thoát). Các thư viện như Spring, Hibernate, Mockito, Lombok, agent APM đều thao tác bytecode (ASM, ByteBuddy, Javassist; từ JDK 24 có Class-File API chính thức — JEP 484).

> ⚠️ **Lỗi thường gặp:** Tối ưu vi mô dựa trên bytecode ("viết thế này ít lệnh bytecode hơn nên nhanh hơn"). Số lệnh bytecode gần như không liên quan tới hiệu năng sau khi JIT; hãy đo bằng JMH.

### 🛠 Bài tập phần 5

**Bài 5.1 — `i++` và `++i` (Cơ bản)**
- Đề bài: So sánh bytecode của `int a = i++;`, `int b = ++i;` và `i = i++;` (biến cục bộ và field).
- Tiêu chí đạt: giải thích vì sao `i = i++;` không đổi `i`; chỉ ra lệnh `iinc` chỉ dùng cho biến cục bộ.

**Bài 5.2 — Switch trên String và enum (Trung bình)**
- Đề bài: `javap -c` một `switch` trên `String` và một trên `enum`; một `switch` pattern matching (Java 21) trên sealed interface.
- Tiêu chí đạt: mô tả cơ chế: String → `hashCode` + `lookupswitch` + `equals`; enum → mảng `$SwitchMap$` tổng hợp (hoặc `invokedynamic` ở JDK mới); pattern switch → `invokedynamic typeSwitch` (`SwitchBootstraps`).

**Bài 5.3 — `finally` và `return` (Nâng cao)**
- Đề bài: Viết method `int f()` có `try { return 1; } finally { x = 2; }` và biến thể `finally { return 2; }`. Đọc bytecode và exception table.
- Tiêu chí đạt: giải thích kết quả trả về, vì sao `return` trong `finally` nuốt exception, và cách compiler nhân bản khối `finally`.

<details>
<summary>Gợi ý lời giải</summary>

`i = i++;` → `iload_1; iinc 1,1; istore_1` — giá trị cũ được load lên operand stack trước khi tăng, sau đó ghi đè lại. Với field: `aload_0; dup; getfield; dup_x1; iconst_1; iadd; putfield` (không có `iinc`).

Bài 5.3: `try { return 1; } finally { x = 2; }` — giá trị 1 được lưu vào biến cục bộ tạm, chạy khối finally, rồi `ireturn` giá trị tạm → trả 1. `finally { return 2; }` → luôn trả 2 và exception trong `try` bị bỏ (không có `athrow` trên đường đó) — javac cảnh báo `-Xlint:finally`.

</details>

---

<a id="p6"></a>
## 6. JIT compiler: interpreter, C1/C2, tiered compilation, deoptimization

### 6.1 Khái niệm
HotSpot có ba "cách" thực thi một method:

| Thành phần | Đặc điểm |
|---|---|
| **Interpreter** | Bắt đầu ngay, không tốn thời gian biên dịch, chậm (gấp hàng chục lần code native). Thu thập profile: số lần gọi, số lần lặp (back-edge), kiểu thực tế tại call site, nhánh nào hay đi |
| **C1** (client compiler) | Biên dịch nhanh, tối ưu vừa phải; có thể chèn code profiling |
| **C2** (server compiler) | Biên dịch chậm, tối ưu mạnh dựa trên profile (speculative optimization) |

**Tiered compilation** (mặc định từ Java 8) có 5 level:

```
Level 0: Interpreter (+ profiling)
Level 1: C1, không profiling          (method tầm thường: getter/setter, hoặc khi C2 bận)
Level 2: C1, profiling giới hạn
Level 3: C1, profiling đầy đủ         ← đường đi phổ biến: 0 → 3 → 4
Level 4: C2, tối ưu hoàn toàn theo profile
```
Ngưỡng tham khảo (HotSpot JDK 17/21): lên level 3 sau khoảng 200 lần gọi (`Tier3InvocationThreshold`) hoặc tổng gọi + vòng lặp ~2000; lên level 4 sau khoảng 5000 lần gọi (`Tier4InvocationThreshold`) / ~15000 tổng. Không tiered (`-XX:-TieredCompilation`): C2 sau 10000 lần (`CompileThreshold`). Việc biên dịch chạy **nền** trên các `CompilerThread`; trong lúc chờ, thread ứng dụng vẫn chạy bản cũ.

**OSR (On-Stack Replacement)**: một method chỉ được gọi một lần nhưng có vòng lặp rất dài (ví dụ `main` chạy benchmark tự viết) — JIT biên dịch và *thay* frame đang chạy giữa vòng lặp. Trong `-XX:+PrintCompilation` OSR được đánh dấu `%`.

### 6.2 Các tối ưu quan trọng của C2
- **Inlining** — "mẹ của mọi tối ưu": thay lời gọi method bằng thân method, mở đường cho escape analysis, constant folding, loại bỏ null check... Giới hạn: method hot được inline nếu bytecode ≤ `FreqInlineSize` (325 byte trên x64), method không hot ≤ `MaxInlineSize` (35 byte); độ sâu `MaxInlineLevel` (15 từ JDK 14, trước đó 9). → Method nhỏ, tập trung không chỉ "clean" mà còn thân thiện với JIT.
- **Devirtualization**: dựa trên *Class Hierarchy Analysis* (CHA — hiện chỉ có một implementation được nạp) và *inline cache* tại call site: **monomorphic** (1 kiểu) và **bimorphic** (2 kiểu) được inline kèm type guard; **megamorphic** (≥ 3 kiểu) → gọi qua vtable/itable, không inline.
- **Intrinsics**: thay một số method JDK bằng mã máy viết tay/lệnh CPU đặc biệt — `System.arraycopy`, `Math.*`, `String.equals/indexOf`, `Integer.bitCount` (POPCNT), CRC32, AES, `Unsafe`/`VarHandle` CAS...
- **Loop optimizations**: unrolling, range-check elimination (bỏ kiểm tra biên mảng khi chứng minh được an toàn), loop-invariant hoisting, **auto-vectorization** (SuperWord → SIMD). Java 16+ có Vector API (incubator) để vectorize tường minh.
- **Lock coarsening / lock elision**, **dead code elimination**, **null check elimination** (null check ngầm qua bắt tín hiệu SIGSEGV), **branch prediction theo profile** (nhánh chưa từng chạy được thay bằng *uncommon trap*).

### 6.3 Deoptimization
C2 tối ưu dựa trên *giả định* (speculation). Khi giả định sai, code bị **deoptimize**: quay về interpreter rồi profile và biên dịch lại. Nguyên nhân:
- Nạp class mới phá vỡ CHA (ví dụ interface trước đây chỉ có 1 implementation).
- Gặp kiểu mới tại call site trước đây monomorphic.
- Nhánh "chưa từng chạy" (uncommon trap) bị đi vào.
- Code cache đầy, hoặc method bị redefine (agent, hot swap).

Trong `PrintCompilation` thấy dòng `made not entrant`. Deoptimization lặp lại nhiều lần trên cùng method → hiệu năng "răng cưa"; có thể bị JIT từ bỏ ("not compilable").

```java
// Chạy: java -XX:+PrintCompilation JitDemo | grep -E "JitDemo|made not entrant"
public class JitDemo {
    interface Shape { double area(); }
    record Square(double s) implements Shape { public double area() { return s * s; } }
    record Circle(double r) implements Shape { public double area() { return Math.PI * r * r; } }
    record Tri(double b, double h) implements Shape { public double area() { return 0.5 * b * h; } }

    static double total(Shape[] shapes) {
        double t = 0;
        for (Shape s : shapes) t += s.area();   // call site: mono → bi → megamorphic
        return t;
    }

    public static void main(String[] args) {
        Shape[] onlySquares = new Shape[10_000];
        java.util.Arrays.fill(onlySquares, new Square(2));
        long t0 = System.nanoTime();
        for (int i = 0; i < 20_000; i++) total(onlySquares);         // monomorphic → inline
        System.out.printf("phase1 %d ms%n", (System.nanoTime() - t0) / 1_000_000);

        Shape[] mixed = new Shape[10_000];
        for (int i = 0; i < mixed.length; i++)
            mixed[i] = switch (i % 3) { case 0 -> new Square(2); case 1 -> new Circle(1); default -> new Tri(1, 2); };
        t0 = System.nanoTime();
        for (int i = 0; i < 20_000; i++) total(mixed);               // deopt → recompile megamorphic
        System.out.printf("phase2 %d ms%n", (System.nanoTime() - t0) / 1_000_000);
    }
}
```

### 6.4 Warm-up trên production
- Pod mới nhận 100% traffic ngay khi khởi động → vài chục giây đầu p99 latency rất cao, CPU cao (vì compiler thread + interpreter). Giải pháp: *warm-up* bằng traffic giả trước khi readiness probe báo sẵn sàng; tăng dần traffic (slow start ở load balancer); **CDS/AppCDS** (`-XX:SharedArchiveFile`, `-XX:+AutoCreateSharedArchive` JDK 19) giảm thời gian nạp class; **Project Leyden AOT cache** (JDK 24 — JEP 483, lưu cả class đã load & link); **CRaC** (checkpoint/restore, có trong một số bản JDK như Azul, Liberica); **GraalVM Native Image** (khởi động mili giây nhưng throughput peak và GC hạn chế hơn, cấu hình reflection phức tạp).
- Ứng dụng CLI ngắn hạn: `-XX:TieredStopAtLevel=1` (chỉ C1) giảm thời gian khởi động/CPU.
- CPU limit thấp trong container (ví dụ 0.5 CPU) làm JIT chạy rất chậm → warm-up kéo dài nhiều phút. Đừng tiết kiệm CPU request quá mức cho service Java.

> 💡 **Góc nhìn Senior:** Vì JIT tối ưu theo profile *thực tế*, "benchmark tự viết với `System.nanoTime()` trong `main`" gần như luôn cho kết quả sai (đo cả interpreter, OSR, dead code elimination). Đó là lý do phải dùng JMH (phần 13). Một điều nữa: những gì giúp JIT (method nhỏ, call site ít đa hình, tránh exception cho control flow, `final`/sealed để CHA chắc chắn) thường cũng là code tốt — nhưng đừng hy sinh thiết kế vì tối ưu vi mô khi chưa có số đo.

> ⚠️ **Lỗi thường gặp:**
> - Tin rằng `final` method nhanh hơn rõ rệt — CHA đã devirtualize method chỉ có một implementation; khác biệt thường không đo được.
> - Dùng exception để điều khiển luồng trong đường nóng: tạo exception tốn kém vì `fillInStackTrace`; C2 có tối ưu "OmitStackTraceInFastThrow" cho một số exception ngầm (NPE, AIOOBE...) → log production xuất hiện `NullPointerException` **không có stack trace** sau một thời gian. Muốn tắt: `-XX:-OmitStackTraceInFastThrow`.

### 🛠 Bài tập phần 6

**Bài 6.1 — Đọc PrintCompilation (Cơ bản)**
- Đề bài: Chạy `JitDemo` với `-XX:+PrintCompilation`. Xác định thời điểm `total` được compile ở level 3, level 4, có OSR không, có `made not entrant` không.
- Tiêu chí đạt: giải thích từng cột của output (timestamp, compile id, cờ `%`, `s`, `!`, `n`, level, kích thước bytecode).

**Bài 6.2 — Mono vs megamorphic (Trung bình)**
- Đề bài: Mở rộng `JitDemo` thành 3 kịch bản đo riêng biệt (mỗi kịch bản một JVM): chỉ 1 kiểu, 2 kiểu, 3 kiểu. Đo thời gian sau warm-up.
- Tiêu chí đạt: số liệu cho thấy bimorphic gần mono, megamorphic chậm hơn rõ; giải thích bằng inline cache. (Làm lại bằng JMH sau khi học phần 13.)

**Bài 6.3 — Fast throw (Nâng cao)**
- Đề bài: Viết vòng lặp gọi method ném NPE ngầm (`obj.toString()` với `obj == null`) hàng trăm nghìn lần, bắt exception và in `e.getStackTrace().length` mỗi 10.000 lần.
- Tiêu chí đạt: quan sát stack trace trở thành rỗng sau khi C2 biên dịch; giải thích; chứng minh `-XX:-OmitStackTraceInFastThrow` khắc phục; nêu hệ quả với việc điều tra lỗi production.

<details>
<summary>Gợi ý lời giải</summary>

Dòng mẫu: `   812  245 %     4       JitDemo::total @ 9 (40 bytes)` — 812ms, compile id 245, `%` = OSR, level 4, `@ 9` = bci của vòng lặp. `made not entrant` xuất hiện khi chuyển sang phase 2 vì gặp kiểu mới tại call site.

Bài 6.3:
```java
public class FastThrow {
    public static void main(String[] args) {
        Object o = null;
        for (int i = 0; i < 300_000; i++) {
            try { o.toString(); }
            catch (NullPointerException e) {
                if (i % 10_000 == 0) System.out.println(i + " -> frames=" + e.getStackTrace().length
                        + " msg=" + e.getMessage());
            }
        }
    }
}
```
Sau vài chục nghìn lần, `frames=0` và message `null` (mất cả helpful NPE message của JDK 14+): JVM ném một instance NPE tiền cấp phát. Trên production, hãy tìm lần xuất hiện *đầu tiên* trong log (lúc vẫn còn stack trace) hoặc tắt tối ưu này.

</details>

---

<a id="p7"></a>
## 7. Nền tảng Garbage Collection

### 7.1 Khái niệm: object nào là "rác"?
Java **không** dùng reference counting (không xử lý được vòng tham chiếu). JVM dùng **reachability**: object là "sống" nếu có đường đi từ một **GC root**:
- Biến cục bộ và tham số trong stack frame của các thread đang sống (kể cả giá trị trong register của code JIT).
- Static field của các class đã nạp (thực chất reachable qua class loader).
- Reference từ JNI (global/local), monitor đang bị giữ (`synchronized`), thread đang chạy, các object nội bộ của JVM (class hệ thống, exception tiền cấp phát...).

Hệ quả: hai object trỏ lẫn nhau nhưng không reachable từ root vẫn bị thu hồi. Ngược lại, một object "không còn dùng" nhưng vẫn reachable (ví dụ còn trong `static Map`) thì **không bao giờ** được thu hồi — đó là bản chất memory leak trong Java.

### 7.2 Các thuật toán cơ bản
| Thuật toán | Cách làm | Ưu | Nhược |
|---|---|---|---|
| **Mark–Sweep** | Đánh dấu object sống, quét bỏ phần còn lại vào free list | Không di chuyển object | **Phân mảnh**; cấp phát từ free list chậm hơn bump pointer |
| **Mark–Compact** | Đánh dấu, rồi dồn object sống về một phía | Không phân mảnh, cấp phát bump pointer | Phải cập nhật mọi reference, tốn thời gian tỉ lệ với heap |
| **Copying (evacuation)** | Copy object sống sang vùng trống, bỏ toàn bộ vùng cũ | Chi phí ∝ **số object sống** (không phụ thuộc lượng rác); tự compact | Cần không gian dự trữ (survivor/region trống) |

### 7.3 Giả thuyết thế hệ (weak generational hypothesis)
*"Hầu hết object chết trẻ"* (request DTO, iterator, StringBuilder, lambda capture...). Vì thế chia heap thành young và old:
- Young nhỏ, thu thường xuyên bằng **copying** — rẻ vì phần lớn đã chết.
- Old lớn, thu ít khi hơn bằng mark–compact hoặc thu dần (G1 mixed) hoặc concurrent (ZGC, Shenandoah).
- Vấn đề: reference từ **old → young**. Để không phải quét cả old gen khi minor GC, JVM dùng **card table** (heap chia thành "card" 512 byte; một **write barrier** đánh dấu card "dirty" khi ghi reference vào object old). G1 dùng thêm **remembered set** cho từng region.

### 7.4 Stop-the-world, concurrent, parallel
- **Parallel**: nhiều GC thread cùng làm việc *trong lúc ứng dụng dừng*.
- **Concurrent**: GC thread làm việc *song song với ứng dụng*. Khó hơn nhiều vì ứng dụng ("mutator") thay đổi đồ thị object trong lúc đánh dấu → cần barrier:
  - **Tri-color marking** (trắng: chưa thăm, xám: đã thăm nhưng chưa quét con, đen: xong). Lỗi nguy hiểm: mutator gán object trắng vào object đen và xóa đường đi cũ → object sống bị thu hồi. Giải pháp: *SATB* (snapshot-at-the-beginning, G1 & Shenandoah: write barrier ghi lại giá trị cũ) hoặc *incremental update* (CMS).
  - **Concurrent compaction** (ZGC, Shenandoah): di chuyển object trong khi ứng dụng chạy, dùng **load barrier** / forwarding để mọi lần đọc reference đều thấy địa chỉ mới.

### 7.5 Ba thước đo — chọn tối đa hai
| Mục tiêu | Ý nghĩa | Ai ưu tiên |
|---|---|---|
| **Throughput** | % thời gian CPU dành cho ứng dụng (không phải GC) | Batch, ETL, tính toán |
| **Latency** | Độ dài pause (p99, max) | API real-time, trading, game server |
| **Footprint** | Bộ nhớ cần thiết | Container nhỏ, nhiều instance |

Hai chỉ số vận hành quan trọng: **allocation rate** (MB/s — quyết định tần suất young GC) và **promotion rate** (MB/s lên old — quyết định tần suất GC old). Object "sống vừa đủ lâu" để bị promote rồi chết ngay (ví dụ cache có TTL ngắn, session, buffer lớn) gây **premature promotion** — kẻ thù của mọi GC generational.

> 💡 **Góc nhìn Senior:** Chi phí của young GC tỉ lệ với **lượng object sống** + kích thước root set (số thread, card dirty), *không* tỉ lệ với lượng rác. Vì vậy: tăng young gen thường giảm tần suất GC mà không tăng pause nhiều; còn giảm *allocation rate* (bớt object tạm, tránh boxing, tái sử dụng buffer có kiểm soát) là đòn bẩy mạnh nhất. Gọi `System.gc()` thủ công gần như luôn sai (với G1 nó gây Full GC; có thể chặn bằng `-XX:+DisableExplicitGC`, nhưng cẩn thận vì một số thư viện NIO cũ dựa vào nó để giải phóng direct buffer — dùng `-XX:+ExplicitGCInvokesConcurrent` thay thế).

> ⚠️ **Lỗi thường gặp:**
> - Gán `obj = null` khắp nơi "để giúp GC": thường vô ích với biến cục bộ (JIT biết biến không còn được dùng — liveness). Chỉ có ý nghĩa với tham chiếu sống lâu: phần tử mảng trong cấu trúc dữ liệu tự cài (ví dụ `Stack.pop()` — Effective Java Item 7), field của object sống lâu.
> - Nói "object được GC ngay khi ra khỏi scope" — không có bảo đảm *khi nào* thu hồi, chỉ có bảo đảm *nếu* thu hồi thì object không còn reachable.

### 🛠 Bài tập phần 7

**Bài 7.1 — Đủ điều kiện GC (Cơ bản)**
- Đề bài: Cho đoạn code tạo các object `a`, `b`, `c` trỏ nhau thành vòng, sau đó gán lại một số biến. Xác định tại mỗi dòng object nào eligible for GC (dạng câu hỏi OCA).
- Tiêu chí đạt: giải thích bằng GC roots, không phải bằng reference counting.

**Bài 7.2 — Đo allocation rate (Trung bình)**
- Đề bài: Viết service giả lập xử lý request: mỗi request parse một JSON 5KB thành `Map<String,Object>`, chạy 10.000 req/s. Đo allocation rate bằng `jstat -gc` (tính delta EU + số lần YGC) hoặc JFR event `jdk.ObjectAllocationSample`.
- Tiêu chí đạt: con số MB/s; tính được tần suất young GC theo kích thước Eden; đề xuất 2 cách giảm allocation và đo lại.

**Bài 7.3 — Tái hiện premature promotion (Nâng cao)**
- Đề bài: Viết cache giữ object 2–5 giây (TTL) với lưu lượng cao, chạy G1 hoặc Parallel với young gen nhỏ (`-Xmn32m` khi dùng Parallel). Bật `-Xlog:gc*,gc+age=trace`. Sau đó tăng young gen / giảm TTL và so sánh.
- Tiêu chí đạt: chỉ ra từ log phân bố tuổi (`age 1..15`) và tần suất old/mixed GC; giải thích vì sao object "sống trung bình" là tệ nhất.

<details>
<summary>Gợi ý lời giải</summary>

Bài 7.2 — công thức: allocation rate ≈ (dung lượng Eden × số young GC trong khoảng t) / t (bỏ qua phần sống sót). Cách giảm: dùng streaming parser (Jackson `JsonParser`) vào POJO thay vì `Map`; tránh `String.split`/regex trong vòng nóng; dùng primitive thay boxing; tái sử dụng `byte[]` buffer qua pool có giới hạn.

Bài 7.3 — trong log `gc+age` thấy object dồn ở các age cao, `Desired survivor size` bị vượt nên "new threshold 1" (tenuring threshold động giảm xuống) → promote sớm. Hướng xử lý: survivor lớn hơn, young lớn hơn, hoặc thiết kế lại cache (off-heap như Chronicle Map, hoặc giữ ít object hơn — ví dụ lưu `byte[]` đã serialize).

</details>

---

<a id="p8"></a>
## 8. Các Garbage Collector & cách chọn

### 8.1 Bức tranh tổng quan
| Collector | Flag | Có từ / trạng thái | Young | Old | Pause | Hợp với |
|---|---|---|---|---|---|---|
| **Serial** | `-XX:+UseSerialGC` | Luôn có | Copying, 1 thread, STW | Mark-compact, 1 thread, STW | Tỉ lệ với heap | Heap nhỏ (< vài trăm MB), 1 CPU, container nhỏ, CLI |
| **Parallel** | `-XX:+UseParallelGC` | **Mặc định Java 5–8** | Copying, nhiều thread, STW | Mark-compact song song, STW | Dài với heap lớn | Batch, throughput tối đa |
| **CMS** | `-XX:+UseConcMarkSweepGC` | Deprecated JDK 9 (JEP 291), **bị xóa JDK 14** (JEP 363) | ParNew | Concurrent mark-sweep (không compact) | Ngắn nhưng có nguy cơ Full GC dài | (Lịch sử) |
| **G1** | `-XX:+UseG1GC` | **Mặc định từ JDK 9** (JEP 248) | Evacuation theo region | Concurrent marking + mixed GC | Mục tiêu mặc định 200ms | Đa số service, heap 4–64GB |
| **ZGC** | `-XX:+UseZGC` | Production JDK 15 (JEP 377); generational JDK 21 (JEP 439), mặc định generational JDK 23 (JEP 474) | Generational (từ 21) | Concurrent mark + concurrent relocate | **< 1ms**, không phụ thuộc heap | Latency nghiêm ngặt, heap rất lớn (tới 16TB) |
| **Shenandoah** | `-XX:+UseShenandoahGC` | Production JDK 15 (JEP 379); có trong OpenJDK build (Temurin, Corretto, Red Hat), **không** có trong Oracle JDK | Không phân thế hệ (chế độ generational experimental từ JDK 24) | Concurrent mark + concurrent evacuation | Vài ms | Latency thấp, heap trung bình–lớn |
| **Epsilon** | `-XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC` | JDK 11 (JEP 318) | Không thu gom gì | — | 0 | Test hiệu năng, job cực ngắn |

**Ergonomics**: nếu không chỉ định, JVM chọn G1 trên "server-class machine" (≥ 2 CPU *và* ≥ 1792MB RAM nhìn thấy được); ngược lại chọn **Serial**. Đây là bẫy trong container: pod `limits.cpu: 1` → JVM thấy 1 CPU → **Serial GC** mà bạn không hề biết. Hãy chỉ định GC tường minh.

### 8.2 Serial & Parallel
- Thiết kế đơn giản, STW toàn phần. Parallel tối đa throughput: tham số mục tiêu `-XX:GCTimeRatio` (mặc định 99 → tối đa 1% thời gian cho GC) và `-XX:MaxGCPauseMillis` (mục tiêu mềm). Có adaptive sizing (`-XX:+UseAdaptiveSizePolicy`).
- Với Java 8 + heap 8GB + Parallel, Full GC vài giây là bình thường — nhiều hệ thống "chậm định kỳ" vì vậy.

### 8.3 CMS (để hiểu lịch sử và trả lời phỏng vấn)
- Old gen dùng **concurrent mark–sweep**: initial mark (STW ngắn) → concurrent mark → remark (STW) → concurrent sweep. **Không compact** → phân mảnh.
- Thất bại kinh điển: **concurrent mode failure** (old đầy trước khi CMS thu xong) và **promotion failed** (đủ dung lượng tổng nhưng không có khối liền đủ lớn do phân mảnh) → fallback **Full GC Serial (một thread!)** → pause hàng chục giây với heap lớn.
- Đã bị loại bỏ vì chi phí bảo trì cao và G1/ZGC đã thay thế.

### 8.4 G1 (Garbage-First) — collector bạn sẽ gặp nhiều nhất
**Cấu trúc**: heap chia thành ~2048 **region** bằng nhau (1–32MB, lũy thừa 2, `-XX:G1HeapRegionSize`; JDK 18+ cho phép tới 512MB). Mỗi region tại một thời điểm đóng vai Eden, Survivor, Old, **Humongous** hoặc Free. Young/old là tập region *logic*, không liền kề.

**Chu kỳ hoạt động**:
```
Young-only phase:
  Young GC (STW, evacuate Eden + Survivor → Survivor/Old), lặp lại...
  Khi heap occupancy > IHOP (InitiatingHeapOccupancyPercent, mặc định 45%, adaptive từ JDK 9):
    Concurrent Start (đi kèm một young GC) → Concurrent Mark (song song app)
    → Remark (STW) → Cleanup (STW ngắn, giải phóng region rỗng hoàn toàn)
Space-reclamation phase:
  Mixed GC (STW): evacuate young + một số old region có NHIỀU RÁC NHẤT (garbage first!)
  lặp lại tới khi lượng rác còn lại không đáng thu (G1HeapWastePercent, mặc định 5%)
→ quay lại young-only.
Fallback: Full GC (STW, đa luồng từ JDK 10 — JEP 307) khi không theo kịp.
```

**Pause target**: `-XX:MaxGCPauseMillis=200` (mặc định). G1 dùng mô hình dự đoán để chọn số region (collection set) sao cho pause gần mục tiêu — đây là *mục tiêu mềm*, không phải bảo đảm. Đặt quá nhỏ (ví dụ 10ms) → young gen bị co lại rất nhỏ → GC liên tục, throughput giảm, mixed GC không thu kịp → Full GC.

**Humongous object**: object ≥ **50% kích thước region** được cấp phát thẳng vào một hoặc nhiều region liền kề trong old gen. Vấn đề: tốn region (object 17MB với region 16MB chiếm 2 region = 32MB), có thể gây phân mảnh và kích hoạt marking sớm. JDK 8u60+ có *eager reclaim* cho humongous không còn reference trong young GC. Khắc phục: tăng `G1HeapRegionSize`, hoặc tránh mảng/`byte[]` khổng lồ (đọc file theo stream, chia nhỏ buffer).

**Các sự kiện xấu trong log G1** cần nhận ra:
- `To-space exhausted` / `Evacuation Failure` — không còn region trống để copy → pause dài, thường tiếp theo là Full GC. Tăng heap, tăng `G1ReservePercent`, giảm IHOP.
- `Pause Full (Allocation Failure)` / `Pause Full (G1 Compaction Pause)` — G1 thua cuộc.
- `Pause Young (Concurrent Start) (G1 Humongous Allocation)` — marking bị kích hoạt bởi humongous allocation.

Ví dụ một dòng log hợp nhất (JDK 17, `-Xlog:gc`):
```
[12.345s][info][gc] GC(42) Pause Young (Normal) (G1 Evacuation Pause) 812M->214M(1024M) 18.532ms
[30.101s][info][gc] GC(57) Pause Young (Concurrent Start) (G1 Humongous Allocation) 640M->600M(1024M) 9.1ms
[30.400s][info][gc] GC(58) Concurrent Mark Cycle 290.2ms
[31.002s][info][gc] GC(61) Pause Young (Mixed) (G1 Evacuation Pause) 700M->410M(1024M) 25.7ms
```
Đọc: trước→sau (tổng heap committed) và thời gian pause.

Tính năng đáng biết: `-XX:+UseStringDeduplication` (gộp `byte[]` của các String trùng nội dung; từ JDK 18 hỗ trợ mọi collector chính), trả bộ nhớ chưa dùng về OS định kỳ (JEP 346, JDK 12).

### 8.5 ZGC
- **Ý tưởng**: làm *gần như mọi thứ* đồng thời với ứng dụng, kể cả **di chuyển (relocate) object**. Pause chỉ còn để quét root của thread — thường **< 1ms**, không tăng theo kích thước heap hay live set.
- **Cơ chế**: *colored pointers* (dùng một số bit của pointer 64-bit để lưu trạng thái: marked, remapped...) và **load barrier** (mỗi lần đọc reference từ heap, một đoạn code nhỏ kiểm tra màu; nếu object đã bị di chuyển thì "tự chữa" pointer). Vì dùng bit của pointer nên ZGC **không hỗ trợ compressed oops** → footprint cao hơn với heap nhỏ.
- **Generational ZGC** (JDK 21, `-XX:+UseZGC -XX:+ZGenerational`; từ JDK 23 là mặc định khi `-XX:+UseZGC`, chế độ non-generational bị xóa ở JDK 24): giảm mạnh CPU overhead và nguy cơ allocation stall nhờ thu gom object trẻ thường xuyên hơn.
- **Rủi ro**: **allocation stall** — khi allocation rate vượt tốc độ GC, thread ứng dụng bị chặn chờ bộ nhớ (log: `Allocation Stall`). Tuning chủ yếu: heap đủ lớn (`-Xmx`, headroom 20–30%+), `-XX:SoftMaxHeapSize`, số thread `-XX:ConcGCThreads`. Throughput thường thấp hơn G1 vài phần trăm vì barrier.

### 8.6 Shenandoah
- Cũng concurrent compaction nhưng cơ chế khác: ban đầu dùng *Brooks forwarding pointer* (một word thêm vào mỗi object), từ JDK 13 chuyển sang **load-reference barrier** và bỏ word thừa. Hỗ trợ compressed oops.
- Pause thường vài ms, độc lập với heap. Có các chế độ heuristics (`adaptive`, `static`, `compact`, `aggressive`).
- Lưu ý phân phối: Oracle JDK không build kèm Shenandoah; các bản OpenJDK khác có.

### 8.7 Chọn GC thế nào?
```
Heap < ~512MB hoặc 1 CPU, footprint quan trọng ............ Serial
Batch/ETL, chỉ quan tâm tổng thời gian chạy .............. Parallel
Service web/API thông thường, heap 2–32GB, p99 ~ 100-200ms OK ... G1 (mặc định, bắt đầu từ đây)
Yêu cầu p99/p999 < 10ms, heap lớn, có dư CPU/RAM ......... Generational ZGC (hoặc Shenandoah)
Java 8 bắt buộc ........................................... G1 (8u40+ ổn) thay vì CMS/Parallel cho service
```
Quy trình đúng: xác định SLO (p99 latency, throughput, budget RAM) → bắt đầu với mặc định (G1) + `-Xms=-Xmx` hợp lý → bật GC log → tải thử giống production → chỉ đổi **một** tham số mỗi lần → so sánh.

> 💡 **Góc nhìn Senior:**
> - 90% "GC tuning" thực ra là: **đặt heap đúng kích thước** + **giảm allocation/live set** + **chọn đúng collector**. Danh sách 30 flag copy từ blog năm 2014 thường gây hại (nhiều flag đã bị xóa, JVM sẽ không khởi động: `Unrecognized VM option`).
> - Pause GC không phải nguồn latency duy nhất: time-to-safepoint, swap, CPU throttling của cgroup (CFS quota) có thể tạo "pause" lớn hơn GC. Đọc log `safepoint` và metric `container_cpu_cfs_throttled_periods_total`.
> - Câu trả lời mẫu cho "G1 vs ZGC": G1 cân bằng throughput/latency, pause tỉ lệ với live set trong collection set, footprint tốt (có compressed oops); ZGC đánh đổi chút throughput + RAM + CPU để có pause gần như hằng số < 1ms.

> ⚠️ **Lỗi thường gặp:**
> - Set `-Xmn` cùng G1 → vô hiệu hóa pause-time ergonomics.
> - Đặt `-XX:MaxGCPauseMillis=10` cho G1 rồi thắc mắc vì sao GC chạy liên tục.
> - Dùng flag CMS (`-XX:+UseConcMarkSweepGC`) khi nâng cấp lên JDK 17/21 → JVM không khởi động được.

### 🛠 Bài tập phần 8

**Bài 8.1 — So sánh 4 collector (Cơ bản)**
- Đề bài: Viết chương trình "allocation churn" (tạo object ngắn hạn + giữ ~30% live set trong một `ArrayList` xoay vòng) chạy 60 giây với `-Xmx2g` cho Serial, Parallel, G1, ZGC. Ghi GC log mỗi lần.
- Tiêu chí đạt: bảng: tổng số pause, tổng thời gian pause, max pause, throughput (số operation hoàn thành). Có nhận xét đúng với lý thuyết.

**Bài 8.2 — Humongous allocation (Trung bình)**
- Đề bài: Với G1, `-Xmx1g -XX:G1HeapRegionSize=1m`, liên tục cấp phát `byte[600_000]` và `byte[2_000_000]`. Bật `-Xlog:gc*,gc+humongous=debug`.
- Tiêu chí đạt: chỉ ra các dòng `G1 Humongous Allocation`; tính số region mỗi mảng chiếm; đổi `G1HeapRegionSize=4m` và so sánh.

**Bài 8.3 — Phân tích GC log thật (Nâng cao)**
- Đề bài: Chạy một ứng dụng Spring Boot dưới tải (`wrk`/`k6`/Gatling) với G1 và ZGC (generational), JDK 21. Đưa log vào GCeasy hoặc GCViewer, hoặc tự viết parser đơn giản.
- Tiêu chí đạt: báo cáo p50/p99/max pause, throughput %, allocation rate, promotion rate, có/không Full GC, Allocation Stall; khuyến nghị collector cho SLO "p99 < 50ms ở 2000 rps" có lý do.

<details>
<summary>Gợi ý lời giải</summary>

Khung chương trình bài 8.1:
```java
import java.util.*;

public class Churn {
    public static void main(String[] args) {
        int liveSlots = 300_000;                    // ~30% của 2GB nếu mỗi phần tử ~2KB
        List<byte[]> live = new ArrayList<>(Collections.nCopies(liveSlots, null));
        Random rnd = new Random(42);
        long ops = 0, sink = 0, end = System.currentTimeMillis() + 60_000;
        while (System.currentTimeMillis() < end) {
            byte[] tmp = new byte[rnd.nextInt(256)];      // rác ngắn hạn
            if ((ops & 63) == 0) live.set(rnd.nextInt(liveSlots), new byte[2048]); // thay object sống
            sink += tmp.length;                           // dùng kết quả để JIT không loại bỏ cấp phát
            ops++;
        }
        System.out.println("ops=" + ops + " sink=" + sink);
    }
}
```
Lệnh: `java -Xms2g -Xmx2g -XX:+UseG1GC -Xlog:gc:file=g1.log Churn`. Kỳ vọng: Parallel có ops cao nhất nhưng max pause lớn nhất; ZGC max pause nhỏ nhất (< 1ms) nhưng ops có thể thấp hơn; Serial tệ nhất về pause với heap 2GB.

Bài 8.2: region 1MB → ngưỡng humongous là 512KB → `byte[600_000]` (~586KB) là humongous chiếm 1 region (lãng phí ~40%); `byte[2_000_000]` chiếm 2 region. Với region 4MB cả hai không còn là humongous.

</details>

---

<a id="p9"></a>
## 9. Reference types: strong, soft, weak, phantom & Cleaner

### 9.1 Khái niệm
| Loại | Khi nào bị thu hồi | `get()` | Dùng cho |
|---|---|---|---|
| **Strong** (`Object o = ...`) | Khi không còn reachable | — | Mặc định |
| **SoftReference** | Khi JVM **thiếu bộ nhớ**; bảo đảm bị clear *trước khi* ném `OutOfMemoryError` | object hoặc `null` | Cache nhạy bộ nhớ (nhưng xem cảnh báo bên dưới) |
| **WeakReference** | Ở lần GC kế tiếp khi chỉ còn weak reference | object hoặc `null` | Metadata gắn kèm object (`WeakHashMap`), canonicalizing map, listener không giữ subscriber |
| **PhantomReference** | Sau khi object đã được xác định là unreachable; dùng với `ReferenceQueue` | **luôn `null`** | Dọn dẹp tài nguyên native sau khi object chết (thay thế `finalize`) |

`ReferenceQueue`: khi GC clear một reference, nó đưa đối tượng `Reference` vào queue để bạn xử lý (ví dụ xóa entry khỏi map).

### 9.2 `WeakHashMap` và cái bẫy của nó
```java
import java.util.*;

public class WeakMapDemo {
    static final class Key { final String id; Key(String id) { this.id = id; } }

    public static void main(String[] args) throws Exception {
        Map<Key, String> meta = new WeakHashMap<>();
        Key k = new Key("u1");
        meta.put(k, "metadata of u1");
        System.out.println(meta.size()); // 1
        k = null;                        // không còn strong ref tới key
        System.gc(); Thread.sleep(100);  // chỉ để demo — không bảo đảm
        System.out.println(meta.size()); // thường là 0: entry bị dọn khi map được truy cập (expungeStaleEntries)
    }
}
```
Cạm bẫy:
- **Key phải dùng identity hoặc equals ổn định** — dùng `String` literal làm key thì không bao giờ bị thu hồi (literal được intern và reachable từ class).
- **Value giữ strong reference tới key** (`map.put(k, new Holder(k))`) → key không bao giờ weakly reachable → leak. Value bị giữ strong bởi map.
- `WeakHashMap` **không thread-safe**. Cache thực tế nên dùng Caffeine (`weakKeys()`, `softValues()`, hoặc tốt hơn: `maximumSize` + `expireAfterWrite`).

### 9.3 SoftReference: vì sao *không nên* làm cache chính
- HotSpot giữ soft reference dựa trên chính sách LRU xấp xỉ: object soft-reachable được giữ ≈ `SoftRefLRUPolicyMSPerMB` (mặc định 1000ms) × số MB heap trống kể từ lần truy cập cuối. Hệ quả khó đoán: heap càng gần đầy, cache bị xóa hàng loạt cùng lúc → **cache stampede** + GC vất vả hơn (soft ref phải xử lý đặc biệt) → hệ thống chậm đúng lúc đang căng thẳng.
- Khuyến nghị: dùng cache có **giới hạn kích thước rõ ràng** (Caffeine `maximumSize/maximumWeight`), đo hit rate.

### 9.4 Finalization (đã lỗi thời) → `Cleaner`
`Object.finalize()`: deprecated từ Java 9, **deprecated for removal từ Java 18 (JEP 421)**. Vấn đề: không biết khi nào chạy (có thể không bao giờ), một thread `Finalizer` duy nhất xử lý — finalizer chậm làm hàng đợi phình to → OOM; object có finalizer cần ít nhất 2 chu kỳ GC mới được thu hồi; có thể "hồi sinh" object; nguy cơ bảo mật (finalizer attack). Effective Java Item 8: tránh cả finalizer lẫn cleaner — chỉ dùng cleaner làm **lưới an toàn** cho tài nguyên native, còn API chính phải là `AutoCloseable` + try-with-resources.

```java
import java.lang.ref.Cleaner;

public final class NativeBuffer implements AutoCloseable {
    private static final Cleaner CLEANER = Cleaner.create();

    // State KHÔNG được tham chiếu tới NativeBuffer (nếu không object sẽ không bao giờ phantom-reachable)
    private static final class State implements Runnable {
        private long address;
        State(long address) { this.address = address; }
        @Override public void run() {                // chạy đúng một lần: bởi close() hoặc bởi Cleaner
            if (address != 0) {
                System.out.println("free native memory @" + address);
                address = 0;
            }
        }
    }

    private final State state;
    private final Cleaner.Cleanable cleanable;

    public NativeBuffer(long size) {
        this.state = new State(fakeMalloc(size));
        this.cleanable = CLEANER.register(this, state); // lưới an toàn nếu quên close()
    }

    @Override public void close() { cleanable.clean(); } // đường chính: tường minh, xác định

    private static long fakeMalloc(long size) { return System.nanoTime(); }

    public static void main(String[] args) {
        try (NativeBuffer b = new NativeBuffer(1024)) { /* dùng b */ }
        new NativeBuffer(2048);   // quên close → Cleaner sẽ dọn "một lúc nào đó"
        System.gc();
    }
}
```
Từ Java 9, PhantomReference được clear tự động khi đưa vào queue, và `Cleaner` dùng chính cơ chế phantom reference + một thread nền.

> 💡 **Góc nhìn Senior:** `ThreadLocal` dùng `WeakReference` cho **key** (chính đối tượng `ThreadLocal`) nhưng **value là strong** — nên khi `ThreadLocal` bị thu hồi, entry "stale" vẫn giữ value cho tới khi map được dọn dẹp ngẫu nhiên hoặc thread chết. Trong thread pool, thread không bao giờ chết → leak. Luôn `remove()` trong `finally`. Đây là câu hỏi kinh điển nối Module 03 với module này.

> ⚠️ **Lỗi thường gặp:** Dùng `WeakReference` cho cache thông thường → cache gần như luôn trống sau mỗi young GC (hit rate thấp), vì object chỉ weakly reachable bị thu ngay.

### 🛠 Bài tập phần 9

**Bài 9.1 — Quan sát 4 loại reference (Cơ bản)**
- Đề bài: Tạo object 10MB, giữ bằng soft/weak/phantom reference (đăng ký `ReferenceQueue`). Bỏ strong reference, gọi GC, rồi cấp phát tới gần đầy heap (`-Xmx64m`).
- Tiêu chí đạt: in thời điểm mỗi reference bị clear/đưa vào queue; kết quả khớp với bảng 9.1.

**Bài 9.2 — Leak qua value của WeakHashMap (Trung bình)**
- Đề bài: Tái hiện leak khi value giữ key. Đo kích thước map sau 1 triệu lần put với key tạm.
- Tiêu chí đạt: chứng minh leak (size không giảm, heap tăng); sửa bằng cách value giữ `WeakReference<Key>` hoặc thiết kế lại; chứng minh size giảm.

**Bài 9.3 — Resource wrapper với Cleaner (Nâng cao)**
- Đề bài: Viết `PooledConnection` dùng `Cleaner` để trả connection về pool nếu người dùng quên `close()`, đồng thời log cảnh báo kèm stack trace nơi connection được mượn (giống "leak detection" của HikariCP/Netty).
- Tiêu chí đạt: không có reference ngược từ action tới object được theo dõi; `close()` gọi nhiều lần an toàn; test chứng minh connection được trả về sau khi bị bỏ rơi + GC.

<details>
<summary>Gợi ý lời giải</summary>

Bài 9.1: weak và phantom vào queue ngay sau GC đầu tiên; soft chỉ bị clear khi áp lực bộ nhớ đủ cao (trước khi OOM). `phantom.get()` luôn `null`.

Bài 9.3 — then chốt:
```java
final class LeakAction implements Runnable {
    private final Pool pool; private final RawConn raw; private final Throwable borrowSite;
    private final java.util.concurrent.atomic.AtomicBoolean done = new java.util.concurrent.atomic.AtomicBoolean();
    LeakAction(Pool p, RawConn r) { pool = p; raw = r; borrowSite = new Throwable("borrowed here"); }
    void closeNormally() { if (done.compareAndSet(false, true)) pool.giveBack(raw); }
    public void run() {                                   // do Cleaner gọi
        if (done.compareAndSet(false, true)) {
            System.err.println("Connection leak detected!"); borrowSite.printStackTrace();
            pool.giveBack(raw);
        }
    }
}
```
`PooledConnection.close()` gọi `action.closeNormally(); cleanable.clean();`. Lưu ý việc chụp `Throwable` tốn kém — HikariCP chỉ làm khi bật `leakDetectionThreshold`.

</details>

---

<a id="p10"></a>
## 10. Memory leak trong Java & các loại `OutOfMemoryError`

### 10.1 Định nghĩa
Memory leak trong Java = object **không còn cần** nhưng **vẫn reachable** nên GC không thu được. Biểu hiện: sau mỗi Full GC/mixed GC, **mức heap nền (baseline/live set) tăng dần** theo thời gian (đồ thị "răng cưa đi lên"), GC ngày càng thường xuyên, cuối cùng OOM hoặc "GC overhead".

Phân biệt với: heap quá nhỏ cho workload (live set ổn định nhưng cao), spike tạm thời (một request load 2GB dữ liệu), và leak native (RSS tăng nhưng heap ổn định).

### 10.2 Các nguồn leak kinh điển
```java
import java.util.*;
import java.util.concurrent.*;

public class LeakCatalog {
    // 1) Static collection không giới hạn — "cache" tự chế
    private static final Map<String, byte[]> CACHE = new HashMap<>();
    static byte[] load(String key) { return CACHE.computeIfAbsent(key, k -> new byte[10_240]); } // không evict

    // 2) Listener/callback đăng ký mà không hủy
    interface Listener { void onEvent(String e); }
    static final List<Listener> LISTENERS = new CopyOnWriteArrayList<>();
    static class Screen { Screen() { LISTENERS.add(e -> render(e)); } void render(String e) {} } // lambda capture this

    // 3) ThreadLocal trong thread pool không remove
    static final ThreadLocal<byte[]> BUFFER = ThreadLocal.withInitial(() -> new byte[1 << 20]);

    // 4) Key có hashCode/equals sai hoặc mutable → không bao giờ tìm lại được để xóa
    static final class BadKey { int id; BadKey(int id) { this.id = id; } } // không override equals/hashCode
    static final Set<BadKey> SEEN = new HashSet<>();

    // 5) Inner class (non-static) giữ ngầm reference tới outer
    class Task implements Runnable { public void run() {} } // giữ LeakCatalog.this

    // 6) Tài nguyên không đóng (stream, connection, ResultSet) → leak native + leak object
    // 7) Unbounded queue: ExecutorService.newFixedThreadPool dùng LinkedBlockingQueue không giới hạn,
    //    producer nhanh hơn consumer → hàng triệu task chờ trong heap.
    // 8) String.intern() / substring trước Java 7u6 (giữ char[] gốc) — lịch sử, đôi khi vẫn bị hỏi.
    // 9) Classloader leak (phần 2.4).
}
```

### 10.3 Bảng các loại `OutOfMemoryError`
| Message | Nguyên nhân | Hướng xử lý |
|---|---|---|
| `Java heap space` | Heap không đủ chỗ cho cấp phát mới sau khi đã GC | Heap dump → tìm leak hoặc tăng heap nếu live set hợp lý; kiểm tra query/payload quá lớn |
| `GC overhead limit exceeded` | (Parallel, và một số collector) > 98% thời gian cho GC mà thu hồi < 2% heap | Giống trên — thường là leak hoặc heap quá nhỏ; đừng tắt `-XX:-UseGCOverheadLimit` để "chữa" |
| `Metaspace` | Quá nhiều class/classloader (dynamic proxy, Groovy script, CGLIB, redeploy leak) | `jcmd VM.classloader_stats`, `-Xlog:class+load/unload`; tìm loader leak; đặt `MaxMetaspaceSize` để lỗi lộ sớm |
| `Compressed class space` | Hết vùng 1GB cho `Klass` | Như trên; tăng `CompressedClassSpaceSize` nếu thực sự cần |
| `unable to create native thread` (JDK cũ: `unable to create new native thread`) | Không tạo được OS thread: `ulimit -u`, `pids.max` của container, hết bộ nhớ ảo/native | Đếm thread (`jcmd Thread.print`, `/proc/<pid>/status`); tìm thread leak (executor tạo mới mỗi request!); dùng pool có giới hạn hoặc virtual thread |
| `Direct buffer memory` | Vượt `MaxDirectMemorySize` (NIO, Netty, gRPC) | Kiểm tra buffer không được release (Netty `ResourceLeakDetector`), tăng giới hạn có tính toán |
| `Requested array size exceeds VM limit` | Mảng > ~`Integer.MAX_VALUE - 2` phần tử | Lỗi logic — ví dụ `StringBuilder`/`ArrayList` mở rộng vô hạn |
| `Out of swap space?` / `request X bytes for ... ` (crash log `hs_err_pid`) | Native allocation thất bại | NMT, kiểm tra JNI/thư viện native, giới hạn container |
| *(không có exception)* Pod bị **OOMKilled**, exit code 137 | Kernel cgroup giết process vì RSS vượt `memory.limit` | Không phải Java OOM! Giảm heap %, NMT, kiểm tra off-heap |

Thêm: `OutOfMemoryError` là `Error` — có thể *bắt* được, nhưng sau OOM trạng thái ứng dụng không đáng tin (thread nào đó có thể đã chết giữa chừng, lock không nhả). Chiến lược production: **`-XX:+HeapDumpOnOutOfMemoryError` + `-XX:+ExitOnOutOfMemoryError`** để orchestrator restart, thay vì để app "sống dở chết dở".

> 💡 **Góc nhìn Senior:** Khi được hỏi "kể một lần bạn xử lý memory leak", hãy kể theo cấu trúc: **triệu chứng** (metric gì: heap sau GC tăng 200MB/ngày, p99 tăng, restart hằng tuần) → **thu thập** (heap dump lúc nào, cách nào an toàn) → **phân tích** (dominator tree, retained heap, path to GC roots) → **nguyên nhân gốc** (ví dụ `ConcurrentHashMap` cache không giới hạn keyed by `userId+timestamp`) → **sửa** (Caffeine `maximumSize` + `expireAfterAccess`) → **phòng ngừa** (alert trên "old gen after GC", test soak 24h, review checklist). Đó là câu trả lời Senior.

> ⚠️ **Lỗi thường gặp:**
> - Tăng `-Xmx` liên tục để "chữa" leak — chỉ trì hoãn sự cố, và heap lớn hơn → heap dump lớn hơn, Full GC dài hơn.
> - Kết luận leak chỉ vì "heap used" tăng: với G1/Parallel heap used *luôn* tăng giữa các lần GC. Phải nhìn **heap sau GC** (live set) qua nhiều giờ.

### 🛠 Bài tập phần 10

**Bài 10.1 — Bộ sưu tập OOM (Cơ bản)**
- Đề bài: Viết một chương trình với tham số dòng lệnh chọn tái hiện: `heap`, `gcoverhead`, `metaspace`, `thread`, `direct`. Ghi lại flag JVM cần dùng cho từng loại.
- Tiêu chí đạt: tái hiện đủ 5 message; với `thread`, chạy trong Docker `--pids-limit=200` để không làm treo máy.

**Bài 10.2 — Leak qua listener (Trung bình)**
- Đề bài: Mô phỏng ứng dụng tạo `Screen` mới mỗi giây (đăng ký listener vào event bus tĩnh) và "đóng" screen cũ mà không hủy đăng ký. Chạy 10 phút với `-Xmx128m`.
- Tiêu chí đạt: biểu đồ heap sau GC tăng dần (từ `jstat -gcutil` hoặc JFR); sửa bằng `unsubscribe` trong `close()` hoặc weak listener; biểu đồ phẳng sau khi sửa.

**Bài 10.3 — Bounded executor (Nâng cao)**
- Đề bài: Tái hiện OOM do `Executors.newFixedThreadPool(4)` nhận task nhanh hơn xử lý. Thay bằng `ThreadPoolExecutor` với `ArrayBlockingQueue` giới hạn và `RejectedExecutionHandler` phù hợp (`CallerRunsPolicy` để tạo backpressure).
- Tiêu chí đạt: phiên bản cũ OOM; phiên bản mới ổn định heap, throughput giảm có kiểm soát; giải thích trade-off của từng rejection policy.

<details>
<summary>Gợi ý lời giải</summary>

- `gcoverhead`: `-Xmx64m -XX:+UseParallelGC`, thêm liên tục các phần tử nhỏ vào `HashMap` (nhiều object nhỏ, mỗi lần GC chỉ thu được rất ít).
- `metaspace`: `-XX:MaxMetaspaceSize=32m`, vòng lặp tạo `java.lang.reflect.Proxy.newProxyInstance` với một `URLClassLoader` mới mỗi lần (giữ loader trong list).
- `thread`: vòng lặp `new Thread(() -> sleep(forever)).start()`.
- `direct`: `-XX:MaxDirectMemorySize=16m`, giữ các `ByteBuffer.allocateDirect(1<<20)` trong list.

Bài 10.3:
```java
var pool = new ThreadPoolExecutor(4, 4, 0, TimeUnit.SECONDS,
        new ArrayBlockingQueue<>(1_000),
        new ThreadPoolExecutor.CallerRunsPolicy());   // producer tự chạy task → tự chậm lại
```
`AbortPolicy` ném `RejectedExecutionException` (phải xử lý ở caller, ví dụ trả HTTP 503); `DiscardPolicy` âm thầm mất task (nguy hiểm); `DiscardOldestPolicy` hợp với dữ liệu "chỉ cần mới nhất".

</details>

---

<a id="p11"></a>
## 11. JVM flags quan trọng & JVM trong container

### 11.1 Các nhóm flag
- `-X...`: tùy chọn không chuẩn nhưng ổn định (`-Xmx`, `-Xss`, `-Xlog`).
- `-XX:+Flag` / `-XX:-Flag` (boolean), `-XX:Flag=value`. Một số cần `-XX:+UnlockDiagnosticVMOptions` hoặc `-XX:+UnlockExperimentalVMOptions`.
- Xem giá trị thực tế: `java -XX:+PrintFlagsFinal -version`, hoặc trên process đang chạy: `jcmd <pid> VM.flags -all`, `jcmd <pid> VM.command_line`.

### 11.2 Bộ nhớ
| Flag | Ý nghĩa | Khuyến nghị |
|---|---|---|
| `-Xms` / `-Xmx` | Heap ban đầu / tối đa | Server: thường đặt `Xms = Xmx` để tránh resize và phát hiện thiếu RAM ngay lúc khởi động |
| `-XX:MaxRAMPercentage=75.0` | Heap tối đa = % RAM *nhìn thấy* (container limit) | Dùng trong container thay cho `-Xmx` cứng; mặc định chỉ **25%** |
| `-XX:InitialRAMPercentage` | Heap ban đầu theo % | Thường bằng Max cho service |
| `-Xss` | Stack mỗi thread | Giảm (512k) nếu có hàng nghìn platform thread và stack nông |
| `-XX:MaxMetaspaceSize` | Trần Metaspace | Đặt (ví dụ 256–512m) để leak lộ ra thành OOM rõ ràng thay vì ăn hết RAM container |
| `-XX:MaxDirectMemorySize` | Trần direct buffer | Đặt tường minh cho app Netty/NIO |
| `-XX:ReservedCodeCacheSize` | Code cache | Tăng nếu thấy "CodeCache is full" |
| `-XX:+AlwaysPreTouch` | Chạm vào mọi trang heap lúc khởi động | Tránh page fault lúc chạy, đổi lại khởi động chậm hơn |

### 11.3 JVM trong container
- **Lịch sử**: Java 8 trước **8u191** *không* đọc giới hạn cgroup → thấy toàn bộ RAM/CPU của node → heap mặc định = 1/4 RAM node (ví dụ 16GB trên node 64GB) trong container giới hạn 2GB → bị OOMKilled. `-XX:+UseContainerSupport` có từ **JDK 10** (backport 8u191), mặc định bật. Hỗ trợ **cgroup v2** từ JDK 15 (backport 11.0.16, 8u372) — image JDK cũ trên node cgroup v2 lại "mù" giới hạn.
- **CPU**: JVM tính `availableProcessors()` từ CPU quota (limits). Từ JDK 19 (và đã backport về các bản 17/11 mới) JVM **không còn** dùng CPU shares (requests) để tính. Số CPU ảnh hưởng tới: số GC thread, số compiler thread, kích thước `ForkJoinPool.commonPool()` (parallel stream, `CompletableFuture` không truyền executor), ergonomics chọn Serial nếu < 2 CPU. Có thể ép bằng `-XX:ActiveProcessorCount=N`.
- **Kiểm tra nhanh**: `java -XshowSettings:system -version` (JDK 17+ hiển thị container metrics) hoặc `-Xlog:os+container=info`.

### 11.4 GC logging
JDK 9+ dùng **Unified Logging** (JEP 158, JEP 271):
```bash
-Xlog:gc*:file=/var/log/app/gc.log:time,uptime,level,tags:filecount=10,filesize=20m
-Xlog:safepoint*:file=/var/log/app/safepoint.log:time,uptime:filecount=5,filesize=10m   # khi nghi ngờ TTSP
```
Java 8 (cần nhớ vì phỏng vấn hỏi khi hệ thống cũ):
```bash
-XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:/var/log/app/gc.log \
-XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=10 -XX:GCLogFileSize=20M
```
GC log gần như không tốn chi phí — **luôn bật trên production**.

### 11.5 Khi có sự cố
```bash
-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/dumps/        # thư mục phải có đủ dung lượng ≥ heap
-XX:+ExitOnOutOfMemoryError                                     # hoặc CrashOnOutOfMemoryError (sinh core + hs_err)
-XX:ErrorFile=/var/log/app/hs_err_pid%p.log
-XX:StartFlightRecording=disk=true,maxage=6h,maxsize=512m,dumponexit=true,filename=/dumps/app.jfr,settings=default
-XX:NativeMemoryTracking=summary                                # chi phí nhỏ (~vài %), bật khi điều tra RSS
```

Một command line tham khảo cho service Spring Boot JDK 21 trong Kubernetes (limit 2Gi, 2 CPU):
```bash
java -XX:+UseG1GC \
     -XX:MaxRAMPercentage=70 -XX:InitialRAMPercentage=70 \
     -XX:MaxMetaspaceSize=256m -XX:MaxDirectMemorySize=128m \
     -Xlog:gc*:file=/logs/gc.log:time,uptime,level,tags:filecount=5,filesize=20m \
     -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/dumps -XX:+ExitOnOutOfMemoryError \
     -XX:StartFlightRecording=disk=true,maxage=2h,maxsize=256m,filename=/dumps/rec.jfr,dumponexit=true \
     -jar app.jar
```

> 💡 **Góc nhìn Senior:** Ghi nhớ quy tắc kích thước: `memory limit ≥ Xmx + MaxMetaspaceSize + MaxDirectMemorySize + (threads × ~1MB) + ~250MB (code cache, GC, JVM internals)`. Đặt `requests.memory = limits.memory` cho service Java (memory không nén được, overcommit sẽ dẫn tới bị kill). Và **heap dump vào ổ ephemeral của pod sẽ biến mất khi pod restart** — mount volume cho `HeapDumpPath`.

> ⚠️ **Lỗi thường gặp:**
> - Dùng cả `-Xmx` và `-XX:MaxRAMPercentage` — `-Xmx` thắng, percentage bị bỏ qua.
> - Copy flag đã bị xóa: `-XX:MaxPermSize` (bị bỏ qua kèm cảnh báo từ JDK 8; flag hết hạn sẽ khiến JVM từ chối khởi động), `-XX:+UseConcMarkSweepGC`, `-XX:+PrintGCDetails` (JDK 9+ ánh xạ cảnh báo sang `-Xlog`), `-XX:+UseBiasedLocking`, `-XX:+AggressiveOpts`.

### 🛠 Bài tập phần 11

**Bài 11.1 — JVM nhìn thấy gì trong container (Cơ bản)**
- Đề bài: Chạy `docker run --memory=1g --cpus=1 eclipse-temurin:21 java -XshowSettings:system -XX:+PrintFlagsFinal -version | grep -E "MaxHeapSize|UseSerialGC|UseG1GC|ActiveProcessorCount"`. Lặp lại với `--cpus=2 --memory=2g`.
- Tiêu chí đạt: giải thích vì sao lần 1 chọn Serial và MaxHeapSize ≈ 256MB.

**Bài 11.2 — Tính memory budget (Trung bình)**
- Đề bài: Cho service: heap live set 900MB sau GC, 300 platform thread, Netty direct memory tối đa 256MB, metaspace đo được 180MB. Đề xuất `-Xmx`/`MaxRAMPercentage`, `MaxMetaspaceSize`, `MaxDirectMemorySize` và `limits.memory`.
- Tiêu chí đạt: có công thức, headroom cho GC (heap ≥ ~2–3 lần live set với G1 để GC không phải làm việc liên tục), kết quả hợp lý.

**Bài 11.3 — Tái hiện OOMKilled (Nâng cao)**
- Đề bài: Viết app cấp phát direct memory không giới hạn và heap 70% limit; chạy trong Docker `--memory=512m`. Quan sát `docker inspect` (`OOMKilled: true`, exit 137). Sau đó sửa bằng `MaxDirectMemorySize` và chứng minh app nhận Java exception thay vì bị kill.
- Tiêu chí đạt: hai kịch bản rõ ràng, giải thích khác biệt giữa *JVM OOM* và *kernel OOM killer*.

<details>
<summary>Gợi ý lời giải</summary>

Bài 11.1: `--cpus=1` → `ActiveProcessorCount=1` → không phải server-class → **Serial**; `MaxHeapSize` = 25% × 1GB = 256MB.

Bài 11.2 (một phương án): heap 2.5GB (`-Xmx2560m`), metaspace 256MB, direct 256MB, thread 300 × 1MB ≈ 300MB (thực dùng thường ít hơn), overhead ~300MB → tổng ≈ 3.7GB → `limits.memory: 4Gi`, `MaxRAMPercentage ≈ 62`. Lý do Senior: không chỉ tính "tối đa có thể" mà còn đo thực tế bằng NMT dưới tải rồi điều chỉnh.

</details>

---

<a id="p12"></a>
## 12. Bộ công cụ chẩn đoán: jcmd, jstat, jstack, MAT, JFR, async-profiler

### 12.1 Công cụ dòng lệnh của JDK
| Công cụ | Dùng để | Lệnh tiêu biểu |
|---|---|---|
| `jps` | Liệt kê JVM process | `jps -lvm` |
| `jcmd` | **Dao đa năng** — nên ưu tiên | `jcmd <pid> help`, `VM.version`, `VM.flags`, `VM.system_properties`, `GC.heap_info`, `GC.class_histogram`, `GC.heap_dump /dumps/h.hprof`, `Thread.print -l`, `JFR.start/JFR.dump/JFR.stop`, `VM.native_memory summary`, `Compiler.codecache`, `VM.classloader_stats`, `Thread.dump_to_file -format=json` (JDK 21, gồm cả virtual thread) |
| `jstat` | Thống kê GC theo chu kỳ, cực nhẹ | `jstat -gcutil <pid> 1000` (S0 S1 E O M CCS YGC YGCT FGC FGCT GCT), `jstat -gc`, `jstat -gccause` |
| `jstack` | Thread dump | `jstack -l <pid>` (kèm thông tin lock `java.util.concurrent`) |
| `jmap` | Heap histogram / dump | `jmap -histo:live <pid>` (**kích hoạt Full GC!**), `jmap -dump:live,format=b,file=h.hprof <pid>` |
| `jhsdb` | Serviceability agent: debug core dump, process treo | `jhsdb jstack --pid <pid>`, `jhsdb jmap --core core --exe java` |
| `jfr` | Đọc file JFR từ dòng lệnh | `jfr summary rec.jfr`, `jfr print --events jdk.GarbageCollection rec.jfr` |

Đọc một thread dump: tìm thread `BLOCKED` (chờ monitor — "waiting to lock <0x...>" và ai đang "locked <0x...>"), `WAITING`/`TIMED_WAITING` trên `parking to wait for <...>` (lock/condition của `j.u.c`), cuối dump có mục **"Found one Java-level deadlock"** nếu có deadlock monitor. Lấy **3–5 dump cách nhau 5–10 giây** để phân biệt thread "kẹt" với thread "tình cờ đang ở đó".

### 12.2 Heap dump & Eclipse MAT
**Lấy heap dump an toàn**:
- Heap dump là thao tác **STW**, thời gian tỉ lệ với heap (vài giây tới vài phút cho heap chục GB), file ≈ heap used. Trên production: rút instance khỏi load balancer trước nếu có thể, đảm bảo đủ dung lượng đĩa, nén (`gzip`) trước khi chuyển.
- `live` option chạy Full GC trước → dump nhỏ hơn, chỉ object sống (thường là điều bạn muốn khi tìm leak). Không dùng `live` khi muốn xem rác (ví dụ tìm nguồn allocation churn).
- Heap dump chứa **dữ liệu nhạy cảm** (password, token, PII trong String) — xử lý như dữ liệu production.

**Khái niệm trong MAT**:
- **Shallow heap**: kích thước của chính object. **Retained heap**: tổng bộ nhớ được giải phóng nếu object này bị thu hồi (object + mọi thứ chỉ nó giữ).
- **Dominator tree**: object X *dominate* Y nếu mọi đường từ GC root tới Y đều đi qua X. Sắp xếp theo retained heap → thấy ngay "ai giữ nhiều bộ nhớ nhất".
- **Leak Suspects report**: báo cáo tự động — điểm khởi đầu tốt.
- **Path to GC Roots** (exclude weak/soft references): trả lời "*vì sao* object này còn sống".
- **Histogram** + so sánh hai heap dump (trước/sau vài giờ) để thấy class nào tăng.
- **OQL**: truy vấn kiểu SQL, ví dụ `SELECT * FROM java.util.HashMap s WHERE s.size > 100000`.

### 12.3 JFR & JMC
**Java Flight Recorder** (open source từ JDK 11 — JEP 328; backport vào 8u262): bộ ghi sự kiện tích hợp sẵn trong JVM, overhead ~1% với cấu hình `default` (~2% với `profile`) → **có thể bật thường trực trên production**. Ghi: CPU sampling (method profiling), allocation (TLAB/sample), GC, safepoint, lock contention (`jdk.JavaMonitorEnter`), I/O file/socket, exception, thread park, class loading, JIT compilation, container metrics; và **custom event** của ứng dụng.

```bash
jcmd <pid> JFR.start name=incident duration=120s settings=profile filename=/dumps/incident.jfr
jcmd <pid> JFR.dump name=incident filename=/dumps/now.jfr      # với recording đang chạy liên tục
```
Mở bằng **JDK Mission Control (JMC)**: Automated Analysis, Method Profiling (flame graph), Memory → Allocation, Lock Instances, GC, Threads.

```java
import jdk.jfr.*;

@Name("com.acme.OrderProcessed")
@Label("Order Processed")
@Category({"Business"})
class OrderProcessedEvent extends Event {
    @Label("Order Id") String orderId;
    @Label("Items") int items;
}

class OrderService {
    void process(String id, int items) {
        var evt = new OrderProcessedEvent();
        evt.begin();
        // ... xử lý ...
        evt.orderId = id; evt.items = items;
        evt.commit();               // gần như miễn phí khi event không được bật
    }
}
```
JDK 14+ có **JFR Event Streaming** (`RecordingStream`) để đọc event trong process và đẩy metric ra ngoài.

### 12.4 async-profiler
- Profiler sampling cho HotSpot, dùng `perf_events` của Linux + API `AsyncGetCallTrace` → **không bị safepoint bias** (profiler dựa trên `Thread.getAllStackTraces`/JVMTI như VisualVM sampler chỉ lấy mẫu tại safepoint → báo sai hotspot). Thấy được cả frame native và kernel.
- Chế độ: `cpu`, `wall` (cả thời gian chờ — tìm I/O chậm, lock), `alloc` (ai cấp phát nhiều), `lock`, `itimer` (khi container không cho dùng perf_events).
- Output: **flame graph** HTML — trục x là tỉ lệ mẫu (không phải thời gian), "đỉnh rộng" là chỗ tốn CPU.
```bash
./asprof -d 30 -e cpu   -f /tmp/cpu.html   <pid>
./asprof -d 30 -e alloc -f /tmp/alloc.html <pid>
./asprof -d 30 -e wall -t -f /tmp/wall.html <pid>     # -t: tách theo thread
```

### 12.5 VisualVM và các công cụ khác
- **VisualVM**: GUI tổng quan (heap, thread, sampler, heap dump) — tiện cho môi trường dev; qua JMX cho remote.
- APM/observability: Micrometer + Prometheus (`jvm_gc_pause_seconds`, `jvm_memory_used_bytes{area="heap"}`), Grafana; OpenTelemetry Java agent; các APM thương mại.
- GC log analyzer: GCeasy, GCViewer, JMC.

> 💡 **Góc nhìn Senior:** Thứ tự ưu tiên khi chẩn đoán production: **metric có sẵn** (Micrometer/Prometheus) → **GC log** (đã bật sẵn) → **JFR** (rẻ, bật được ngay bằng jcmd) → **thread dump** (rẻ) → **async-profiler** (rẻ, cần quyền) → **heap dump** (đắt, STW, nhạy cảm — cuối cùng). Và luôn chạy công cụ với **cùng user** và **cùng phiên bản JDK** với process (trong container: `kubectl exec` vào pod; nếu image chỉ có JRE thì dùng ephemeral debug container chia sẻ process namespace, hoặc `jattach`).

> ⚠️ **Lỗi thường gặp:**
> - Chạy `jmap -histo:live` trên production giờ cao điểm để "xem nhanh" → Full GC vài giây.
> - Tin profiler dựa trên safepoint (một số APM, VisualVM sampler) khi hotspot thật nằm trong vòng lặp counted không có safepoint.
> - Lấy đúng một thread dump rồi kết luận.

### 🛠 Bài tập phần 12

**Bài 12.1 — Đọc jstat (Cơ bản)**
- Đề bài: Chạy `Churn` (phần 8) và `jstat -gcutil <pid> 1000 30`. Giải thích từng cột và tính số young GC/giây, thời gian trung bình mỗi young GC (`YGCT/YGC`).
- Tiêu chí đạt: giải thích đúng; phát hiện được thời điểm có Full GC (cột `FGC` tăng).

**Bài 12.2 — Tìm deadlock bằng thread dump (Trung bình)**
- Đề bài: Viết chương trình 2 thread khóa 2 monitor ngược thứ tự và một biến thể dùng `ReentrantLock`. Dùng `jstack -l` và `jcmd Thread.print`.
- Tiêu chí đạt: chỉ ra được dòng "Found one Java-level deadlock" ở cả hai biến thể; giải thích vai trò `-l` (ownable synchronizers).

**Bài 12.3 — Heap dump hunt (Nâng cao)**
- Đề bài: Chạy chương trình leak bất kỳ ở phần 10 trong 5 phút, lấy 2 heap dump cách nhau 2 phút. Phân tích bằng MAT.
- Tiêu chí đạt: ảnh chụp/ghi chú Dominator Tree, Leak Suspects, Path to GC Roots, so sánh histogram 2 dump; viết báo cáo 1 trang "root cause".

**Bài 12.4 — Flame graph (Nâng cao)**
- Đề bài: Viết endpoint REST (hoặc vòng lặp) cố tình chậm: regex biên dịch lại mỗi lần (`String.matches`), log ở mức debug với string concatenation, `SimpleDateFormat` tạo mới. Profile bằng async-profiler `cpu` và `alloc`, sửa, profile lại.
- Tiêu chí đạt: hai cặp flame graph trước/sau; throughput tăng có số liệu.

<details>
<summary>Gợi ý lời giải</summary>

Bài 12.1: `E` dao động 0→100% theo chu kỳ young GC; `O` tăng dần rồi giảm khi mixed/old GC; `M` (Metaspace) ổn định. `GCT` = tổng thời gian GC tích lũy (giây).

Bài 12.4: trên flame graph CPU sẽ thấy đỉnh rộng tại `java.util.regex.Pattern.compile` (từ `String.matches`) và `SimpleDateFormat.<init>`; flame graph alloc thấy `StringBuilder`/`char[]` từ log. Sửa: `private static final Pattern P = Pattern.compile(...)`, `DateTimeFormatter` (immutable, thread-safe) dạng static final, log dạng tham số `log.debug("x={}", x)`.

</details>

---

<a id="p13"></a>
## 13. Phương pháp đo hiệu năng & JMH

### 13.1 Nguyên tắc
1. **Đo trước, tối ưu sau** ("premature optimization is the root of all evil" — Knuth, đầy đủ câu là *"...yet we should not pass up our opportunities in that critical 3%"*). Mọi thay đổi hiệu năng phải có số liệu trước/sau.
2. **Xác định mục tiêu (SLO)** trước: ví dụ "p99 < 200ms ở 1500 rps, error rate < 0.1%". Không có mục tiêu → không biết khi nào dừng.
3. **Latency vs throughput**:
   - *Throughput*: số việc hoàn thành / đơn vị thời gian.
   - *Latency*: thời gian một việc. Luôn nhìn **phân bố** — p50, p95, **p99, p99.9, max** — không phải trung bình. Một trang gọi 20 service: xác suất *ít nhất một* lời gọi rơi vào p99 = 1 − 0.99²⁰ ≈ 18% → p99 của từng service là trải nghiệm của rất nhiều người dùng.
   - Định luật **Little**: `L = λ × W` (số request đồng thời = throughput × latency). Ví dụ 1000 rps × 0.2s = 200 request đồng thời → cần ≥ 200 thread (mô hình thread-per-request) hoặc mô hình non-blocking/virtual thread.
4. **Phương pháp USE** (Brendan Gregg) cho mỗi tài nguyên (CPU, RAM, disk, network, thread pool, connection pool): **U**tilization, **S**aturation (hàng đợi), **E**rrors.
5. **Coordinated omission** (Gil Tene): load generator dạng "gửi request tiếp theo khi request trước xong" sẽ *bỏ qua* đúng những lúc hệ thống chậm → p99 đẹp giả tạo. Dùng công cụ constant-rate (wrk2, Gatling/k6 với open model) và HdrHistogram.
6. Môi trường test phải **giống production**: cùng JDK, cùng flag, cùng giới hạn container, dữ liệu kích thước thật, đã warm-up.

### 13.2 Vì sao microbenchmark tự viết sai
- Đo cả interpreter + thời gian JIT (không warm-up).
- **Dead code elimination**: kết quả không được dùng → JIT xóa luôn phép tính → "0 ns".
- **Constant folding**: input là hằng → JIT tính sẵn lúc biên dịch.
- OSR của vòng lặp đo khác code thật; loop unrolling/hoisting làm sai lệch.
- GC xảy ra ngẫu nhiên giữa các lần đo; profile bị "nhiễm" bởi benchmark trước trong cùng JVM.

### 13.3 JMH
**JMH** (Java Microbenchmark Harness, của OpenJDK) xử lý những vấn đề trên: fork JVM riêng, warm-up, `Blackhole`, trạng thái `@State`, nhiều chế độ đo.

```java
// pom: org.openjdk.jmh:jmh-core và jmh-generator-annprocess (cùng version, ví dụ 1.37)
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import java.util.*;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 2, jvmArgsAppend = {"-Xms1g", "-Xmx1g"})
@State(Scope.Benchmark)
public class ConcatBenchmark {

    @Param({"10", "100", "1000"})
    int n;

    List<String> parts;

    @Setup(Level.Trial)
    public void setup() {
        parts = new ArrayList<>();
        for (int i = 0; i < n; i++) parts.add("item" + i);
    }

    @Benchmark
    public String plusInLoop() {                 // O(n^2) copy
        String s = "";
        for (String p : parts) s += p;
        return s;                                // return → JMH tự đưa vào Blackhole
    }

    @Benchmark
    public String builder() {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) sb.append(p);
        return sb.toString();
    }

    @Benchmark
    public void joinWithBlackhole(Blackhole bh) {
        bh.consume(String.join("", parts));
    }

    // SAI: kết quả không dùng → có thể bị dead-code elimination
    @Benchmark
    public void wrongDeadCode() {
        String.join("", parts);
    }
}
```
Chạy: `mvn clean package && java -jar target/benchmarks.jar ConcatBenchmark -prof gc` (profiler `gc` in thêm `gc.alloc.rate.norm` — số byte cấp phát mỗi operation, cực hữu ích). Các profiler khác: `-prof stack`, `-prof perfasm` (xem assembly), `-prof async` (tích hợp async-profiler).

Các chế độ: `Throughput` (ops/s), `AverageTime`, `SampleTime` (phân bố, có percentile), `SingleShotTime` (đo cold start). `@State(Scope.Thread)` cho dữ liệu riêng từng thread; `@Threads(4)` để đo contention.

> 💡 **Góc nhìn Senior:** JMH trả lời câu hỏi "A nhanh hơn B *trong điều kiện cô lập*". Nó không trả lời "hệ thống sẽ nhanh hơn" — cái đó cần load test end-to-end và profiling production. Hãy luôn báo cáo kèm *error* (± CI) mà JMH in ra; chênh lệch nhỏ hơn error là không có ý nghĩa. Và trước khi micro-optimize một method, profile để chắc rằng nó thực sự nằm trên đường nóng (thường chỉ < 5% code chiếm > 90% CPU; còn latency thường bị chi phối bởi I/O: DB, network, lock).

> ⚠️ **Lỗi thường gặp:**
> - Chạy benchmark trên laptop có Turbo Boost/tiết kiệm pin, nhiều app khác đang chạy, và chỉ 1 fork.
> - Benchmark trong IDE ở chế độ debug.
> - Báo cáo trung bình latency của load test thay vì percentile; không ghi lại cấu hình (JDK, flag, phần cứng).

### 🛠 Bài tập phần 13

**Bài 13.1 — Benchmark đầu tiên (Cơ bản)**
- Đề bài: Tạo project JMH (archetype `jmh-java-benchmark-archetype`), chạy `ConcatBenchmark` với `-prof gc`.
- Tiêu chí đạt: bảng kết quả theo `n`; nhận xét độ phức tạp `plusInLoop` (bậc hai) vs `builder` (tuyến tính) dựa trên số liệu và `gc.alloc.rate.norm`.

**Bài 13.2 — Bắt lỗi benchmark (Trung bình)**
- Đề bài: Viết 3 benchmark *cố tình sai*: kết quả không dùng; input là `final` hằng số; vòng lặp tự viết bên trong `@Benchmark`. So sánh với phiên bản đúng.
- Tiêu chí đạt: chỉ ra benchmark sai cho kết quả phi lý (gần 0 ns hoặc nhanh bất thường); giải thích cơ chế JIT tương ứng.

**Bài 13.3 — So sánh collection (Nâng cao)**
- Đề bài: Benchmark `ArrayList` vs `LinkedList` cho: duyệt toàn bộ, `get(i)` ngẫu nhiên, chèn đầu danh sách, chèn giữa bằng `ListIterator`; với n = 1K, 100K, 1M. Thêm `int[]` làm baseline cho duyệt.
- Tiêu chí đạt: kết quả có error; giải thích vì sao `LinkedList` thua cả ở những thao tác "lý thuyết O(1)" (cache miss, pointer chasing, object header) — liên hệ phần 4.

<details>
<summary>Gợi ý lời giải</summary>

Bài 13.2 — phiên bản đúng cho hằng số: đặt input trong field của `@State` (không `final`), để JIT không fold được. Với vòng lặp: để JMH lặp (mỗi lần gọi `@Benchmark` là một operation), hoặc dùng `@OperationsPerInvocation(N)` kèm `Blackhole.consume` trong vòng lặp.

Bài 13.3: duyệt `LinkedList` 1M phần tử chậm hơn `ArrayList` nhiều lần vì mỗi node là một object rải rác trên heap (mỗi bước có thể là một cache miss ~100ns), trong khi `ArrayList` duyệt mảng reference liên tục (prefetcher hoạt động tốt), còn `int[]` nhanh nhất vì không có indirection.

</details>

---

<a id="p14"></a>
## 14. Runbook xử lý sự cố: CPU 100%, GC cao, OOM

Phần này viết dưới dạng **runbook** — làm theo từng bước khi sự cố xảy ra. Nguyên tắc chung: **ổn định hệ thống trước** (scale out, rollback, rút instance lỗi khỏi LB nhưng *giữ lại* để điều tra), **thu thập bằng chứng trước khi restart** (thread dump, JFR dump, GC log, heap dump nếu cần), rồi mới phân tích.

### 14.1 Runbook: CPU 100%
```bash
# 1. Process nào? (trong container: CPU throttling cũng biểu hiện như "chậm")
top -c                         # hoặc kubectl top pod
# 2. Thread nào trong JVM ăn CPU?
top -H -p <pid>                # ghi lại TID (thập phân) của các thread top
printf '%x\n' <tid>            # JDK 8/11/17 in nid dạng hex (nid=0x4a3f) → cần đổi sang hex
# 3. Thread dump (3 lần, cách 5 giây) và tìm thread theo nid
jcmd <pid> Thread.print -l > td1.txt ; sleep 5 ; jcmd <pid> Thread.print -l > td2.txt
grep -A 30 'nid=0x4a3f' td1.txt  # JDK 8/11/17
grep -A 30 'nid=19007 ' td1.txt  # JDK 21 in nid dạng thập phân → dùng thẳng TID từ top -H
# 4. Hoặc nhanh và chính xác hơn: profile 30s
./asprof -d 30 -e cpu -f cpu.html <pid>
```
Phân loại nguyên nhân theo tên thread:
| Thread ăn CPU | Nghĩa là | Bước tiếp |
|---|---|---|
| `GC Thread#`, `G1 Conc#`, `ZWorker` | **GC liên tục** — gần đầy heap / leak / allocation rate quá cao | Chuyển sang runbook 14.2 |
| `C2 CompilerThread` | JIT đang biên dịch (khởi động, deopt storm, code cache đầy) | Xem `PrintCompilation`/JFR Compilation, `Compiler.codecache` |
| Thread ứng dụng (`http-nio-*`, `pool-*`) | Code nóng: vòng lặp vô hạn, regex thảm họa (catastrophic backtracking), `HashMap` bị hỏng do dùng đồng thời (Java 7 có thể tạo vòng lặp vô hạn khi resize), serialize JSON khổng lồ, busy-wait/spin | Đọc stack trace qua nhiều dump; flame graph |
| Nhiều thread `RUNNABLE` tại cùng một `synchronized`/CAS loop | Contention, spin | JFR `jdk.JavaMonitorEnter`, async-profiler `-e lock` |

### 14.2 Runbook: GC cao / pause dài / latency tăng dần
1. Xác nhận bằng số liệu: `jstat -gcutil <pid> 1000` — `O` (old) có ở mức cao liên tục sau GC không? `FGC` có tăng không? Metric `jvm_gc_pause_seconds` p99/max; tỉ lệ thời gian GC (`GCT` tăng bao nhiêu mỗi phút).
2. Đọc GC log: loại pause (Young / Mixed / **Full**), nguyên nhân (`Allocation Failure`, `G1 Humongous Allocation`, `Metadata GC Threshold`, `System.gc()`), `To-space exhausted`, `Allocation Stall` (ZGC).
3. Phân loại:
   - **Live set sau GC tăng dần theo thời gian** → leak → runbook 14.3 (heap dump, so sánh 2 dump).
   - **Live set ổn định nhưng gần sát Xmx** → heap thiếu; tăng heap (nếu RAM cho phép) hoặc giảm dữ liệu giữ trong bộ nhớ (cache quá lớn).
   - **Allocation rate rất cao** → young GC dày đặc; profile `alloc` để tìm nguồn cấp phát.
   - **Humongous allocation** → mảng lớn; tăng region size hoặc sửa code.
   - **`System.gc()`** → tìm ai gọi (JFR event `jdk.GarbageCollection` có `cause`; RMI DGC cũ gọi định kỳ).
   - **Pause dài nhưng GC log nói GC ngắn** → xem safepoint log (TTSP), CPU throttling, swap (`vmstat`, `si/so`), transparent huge pages.
4. Chỉ đổi **một** tham số mỗi lần, có so sánh trước/sau.

### 14.3 Runbook: OutOfMemoryError / memory tăng dần
```bash
# 0. Đọc chính xác message OOM trong log (bảng 10.3) — mỗi loại một hướng điều tra khác nhau.
# 1. Nếu đã có -XX:+HeapDumpOnOutOfMemoryError: lấy file .hprof từ HeapDumpPath (volume!).
# 2. Nếu chưa OOM nhưng heap tăng dần: lấy 2 dump cách nhau đủ xa
jcmd <pid> GC.heap_dump /dumps/h1.hprof        # mặc định chỉ object sống (có Full GC)
# ... 30-60 phút sau ...
jcmd <pid> GC.heap_dump /dumps/h2.hprof
# Nhanh, nhẹ hơn heap dump (nhưng cũng có Full GC): so sánh histogram
jcmd <pid> GC.class_histogram | head -30
# 3. RSS tăng nhưng heap ổn định → native: 
jcmd <pid> VM.native_memory baseline ; ... ; jcmd <pid> VM.native_memory summary.diff
# 4. Metaspace:
jcmd <pid> VM.classloader_stats ; jcmd <pid> VM.metaspace
# 5. Thread:
grep Threads /proc/<pid>/status ; jcmd <pid> Thread.print | grep -c '^"'
```
Phân tích trong MAT: Leak Suspects → Dominator Tree (top retained) → Path to GC Roots (exclude weak/soft) → xác định *ai* (class, field, collection) giữ và *vì sao* không xóa → sửa → **soak test** (chạy tải 12–24 giờ) chứng minh live set phẳng.

Off-heap leak hay gặp: Netty `ByteBuf` không `release()` (bật `-Dio.netty.leakDetection.level=paranoid` trong test), `Inflater`/`Deflater` (`GZIPInputStream`) không `close()` (giữ bộ nhớ native zlib), JNI library, glibc malloc arena phân mảnh với nhiều thread (thử `MALLOC_ARENA_MAX=2` hoặc jemalloc).

### 14.4 Mẫu báo cáo sự cố (postmortem) ngắn
```
Tiêu đề: [P1] order-service p99 tăng 3s, 2 pod OOMKilled — 2026-xx-xx
Tác động: 18 phút, ~4% request lỗi 5xx
Dòng thời gian: deploy v2.31 lúc 10:02 → heap after GC tăng 120MB/giờ → 13:40 Full GC liên tục → 13:52 OOM
Bằng chứng: GC log (đính kèm), heap dump h1/h2, dominator tree: ConcurrentHashMap trong PriceCache giữ 1.4GB
Nguyên nhân gốc: key cache gồm (productId, requestTimestamp) → không bao giờ trùng → cache không giới hạn
Khắc phục ngay: rollback v2.30
Khắc phục lâu dài: Caffeine maximumSize=50_000 + expireAfterWrite=5m; alert "old gen after GC > 70% trong 30 phút"; soak test trong CI
Bài học: review mọi cache phải có bound + metric hit rate
```

> 💡 **Góc nhìn Senior:** Trong phỏng vấn, câu hỏi tình huống "service chạy vài ngày thì chậm dần rồi chết, bạn làm gì?" đánh giá **quy trình** hơn là công cụ. Hãy nói được: giả thuyết → bằng chứng cần thu thập → công cụ → cách xác nhận → cách sửa → cách phòng ngừa (alerting, test, review). Đồng thời đề cập rủi ro của chính việc chẩn đoán (heap dump STW, dữ liệu nhạy cảm, đĩa đầy).

> ⚠️ **Lỗi thường gặp:** Restart ngay để "chữa cháy" mà không giữ lại bằng chứng → sự cố lặp lại sau 3 ngày và không ai biết vì sao. Tối thiểu hãy lấy thread dump + `GC.class_histogram` + JFR dump (vài giây) trước khi restart.

### 🛠 Bài tập phần 14

**Bài 14.1 — Tìm thread ăn CPU (Cơ bản)**
- Đề bài: Viết app có 10 thread ngủ và 1 thread chạy vòng lặp vô hạn tính toán (đặt tên thread rõ ràng). Áp dụng runbook 14.1 từ `top -H` tới dòng code.
- Tiêu chí đạt: chỉ ra đúng thread và dòng code; ghi lại từng lệnh đã chạy.

**Bài 14.2 — Regex thảm họa (Trung bình)**
- Đề bài: Endpoint validate email dùng regex `^([a-zA-Z0-9]+){1,64}@example\.com$`, gửi input `"aaaaaaaaaaaaaaaaaaaaaaaaaaaa!"`. Chẩn đoán CPU 100% bằng thread dump và async-profiler. So sánh thời gian với pattern `^([a-zA-Z0-9]+)*@example\.com$` trên cùng input.
- Tiêu chí đạt: giải thích catastrophic backtracking; sửa regex (bỏ nested quantifier, dùng possessive quantifier/atomic group) và giới hạn độ dài input; chứng minh CPU bình thường.

**Bài 14.3 — Diễn tập sự cố (Nâng cao)**
- Đề bài: Làm việc theo cặp: một người cài một lỗi bí mật (leak, humongous, `System.gc()` định kỳ, thread leak, lock contention) vào app mẫu; người kia chỉ được dùng metric/log/công cụ JDK để tìm ra trong 60 phút.
- Tiêu chí đạt: báo cáo postmortem theo mẫu 14.4; thời gian phát hiện; lệnh đã dùng.

<details>
<summary>Gợi ý lời giải</summary>

Bài 14.2: regex `([a-zA-Z0-9]+){1,64}` có số cách chia chuỗi tăng theo cấp số mũ khi không khớp → backtracking O(2ⁿ) (mỗi ký tự thêm vào ≈ gấp đôi thời gian). Lưu ý: từ JDK 9, engine regex có memoization cho vòng lặp group `*`/`+` **không giới hạn**, nên ví dụ kinh điển `([a-zA-Z0-9]+)*` chạy < 1 ms trên JDK 17/21. Quantifier có giới hạn `{m,n}` và backreference không được tối ưu này, nên vẫn bị ReDoS (xem [Case 02](../04-case-study/02-cpu-100-percent.md)). Thread dump nhiều lần đều thấy thread ở `java.util.regex.Pattern$Loop.match` / `Pattern$GroupTail.match` lặp lại sâu. Sửa: `^[a-zA-Z0-9]+@example\.com$` (bỏ nhóm lồng), hoặc `^(?>[a-zA-Z0-9]+)@example\.com$`; giới hạn độ dài input trước khi match (ví dụ ≤ 254 ký tự).

Bài 14.3 — dấu hiệu nhận biết nhanh:
- Leak → live set sau GC tăng.
- Humongous → log `G1 Humongous Allocation`.
- `System.gc()` → `Pause Full (System.gc())` định kỳ.
- Thread leak → số thread tăng (`jvm_threads_live_threads`).
- Lock contention → nhiều thread `BLOCKED` cùng một monitor, JFR `Java Monitor Blocked`.

</details>

---

<a id="du-an-mini"></a>
## Dự án mini

### "JVM Doctor" — chẩn đoán và chữa một service bệnh

**Bối cảnh:** Bạn nhận một service Spring Boot (hoặc Java thuần với `com.sun.net.httpserver.HttpServer`) tên `inventory-service` bị khách phàn nàn "chậm dần, thỉnh thoảng sập". Bạn tự xây dựng service này với **ít nhất 4 "bệnh" cài sẵn**, sau đó đóng vai SRE/Senior để chẩn đoán và chữa, và cuối cùng tạo bộ công cụ giám sát.

**Yêu cầu chức năng**
1. Service có 3 endpoint: `GET /items/{id}` (đọc từ "DB" giả lập trong bộ nhớ với độ trễ 5–20ms), `POST /items/search` (lọc theo regex do client gửi), `GET /report` (tạo báo cáo CSV lớn).
2. Cài các bệnh (bật/tắt bằng feature flag/biến môi trường để so sánh):
   - B1: cache kết quả `GET /items/{id}` trong `static ConcurrentHashMap` không giới hạn, key chứa timestamp.
   - B2: `ThreadLocal<byte[1MB]>` buffer trong thread pool, không `remove()`; thêm một executor được tạo mới mỗi request `/report` mà không shutdown (thread leak).
   - B3: `/report` dựng toàn bộ CSV 50–200MB trong một `String`/`byte[]` (humongous + spike).
   - B4: regex của `/search` biên dịch mỗi lần và dễ bị catastrophic backtracking.
   - (Tùy chọn) B5: `System.gc()` gọi sau mỗi `/report`; B6: `synchronized` toàn cục trên đường nóng.
3. Viết script tải (k6, Gatling, wrk2 hoặc JMH-less Java client) với **constant arrival rate**, chạy ≥ 30 phút.
4. Viết **"Doctor CLI"** nhỏ bằng Java dùng Attach API/JMX (`ManagementFactory`, `com.sun.management.HotSpotDiagnosticMXBean`, `ThreadMXBean`) có thể: in heap usage & GC stats theo chu kỳ, phát hiện deadlock (`findDeadlockedThreads`), lấy top 5 thread theo CPU time, kích hoạt heap dump theo yêu cầu, bắt đầu/dump recording JFR (`jdk.management.jfr` hoặc qua `jcmd`).

**Yêu cầu phi chức năng**
- Chạy trong Docker với `--memory=1g --cpus=2`, JDK 21; cấu hình JVM đầy đủ (GC tường minh, `MaxRAMPercentage`, GC log xoay vòng, heap dump on OOM vào volume, JFR liên tục).
- Sau khi chữa: p99 latency `GET /items/{id}` < 50ms ở 500 rps, live set phẳng trong soak test 2 giờ, không Full GC, số thread ổn định.
- Có dashboard (Micrometer + Prometheus + Grafana, hoặc JMC + ảnh chụp) hiển thị heap after GC, GC pause, allocation rate, số thread, CPU.
- So sánh G1 và generational ZGC trên phiên bản đã chữa (bảng số liệu p50/p99/max latency, throughput, RSS).

**Sản phẩm nộp**
- Mã nguồn service + Doctor CLI + script tải + `Dockerfile`/`docker-compose.yml`.
- Thư mục `evidence/`: GC log, flame graph (cpu, alloc), ảnh MAT (dominator tree, path to GC roots), thread dump trích đoạn.
- `REPORT.md` theo mẫu postmortem 14.4 cho **từng** bệnh: triệu chứng → bằng chứng → nguyên nhân gốc → sửa → số liệu trước/sau.

**Tiêu chí chấm (100 điểm)**
| Hạng mục | Điểm |
|---|---|
| Tái hiện đúng và đo được triệu chứng của ≥ 4 bệnh | 15 |
| Chẩn đoán bằng công cụ phù hợp, có bằng chứng (không "đoán") cho từng bệnh | 25 |
| Sửa đúng nguyên nhân gốc, có số liệu trước/sau | 20 |
| Cấu hình JVM/container hợp lý, giải thích được từng flag | 10 |
| Doctor CLI hoạt động, code sạch, xử lý lỗi | 10 |
| So sánh G1 vs ZGC có phương pháp (warm-up, nhiều lần chạy, percentile) | 10 |
| Chất lượng báo cáo: rõ ràng, có cấu trúc, có bài học/phòng ngừa | 10 |

---

<a id="checklist-tu-danh-gia"></a>
## Checklist tự đánh giá
- [ ] Tôi giải thích được 3 giai đoạn loading/linking/initialization và những hành động nào kích hoạt khởi tạo class.
- [ ] Tôi giải thích được parent delegation, vì sao hai class cùng tên có thể khác nhau, và phân biệt `ClassNotFoundException` với `NoClassDefFoundError`.
- [ ] Tôi mô tả được nguyên nhân và cách tìm classloader leak (redeploy, ThreadLocal, thread không dừng).
- [ ] Tôi vẽ được các runtime data area và biết lỗi nào xảy ra khi từng vùng cạn kiệt.
- [ ] Tôi giải thích được vì sao PermGen bị thay bằng Metaspace và vì sao RSS của process Java lớn hơn `-Xmx`.
- [ ] Tôi ước lượng được kích thước object (header, compressed oops, padding) và giải thích ngưỡng heap 32GB.
- [ ] Tôi giải thích được escape analysis/scalar replacement và giới hạn của nó.
- [ ] Tôi đọc được output `javap -c` cơ bản và giải thích `i++` không atomic, lambda dùng `invokedynamic`.
- [ ] Tôi giải thích được tiered compilation (level 0–4), inlining, mono/bi/megamorphic call site, deoptimization và vấn đề warm-up.
- [ ] Tôi giải thích được GC roots, reachability, các thuật toán mark-sweep/mark-compact/copying, generational hypothesis, card table.
- [ ] Tôi so sánh được Serial, Parallel, CMS, G1, ZGC, Shenandoah (cơ chế, pause, throughput, footprint, phiên bản JDK) và chọn GC theo SLO.
- [ ] Tôi đọc được GC log G1 và nhận diện Full GC, humongous allocation, to-space exhausted, allocation stall.
- [ ] Tôi phân biệt được strong/soft/weak/phantom reference, bẫy của `WeakHashMap`, và dùng `Cleaner` thay `finalize`.
- [ ] Tôi liệt kê được ≥ 6 nguồn memory leak và tất cả các loại `OutOfMemoryError` kèm hướng xử lý.
- [ ] Tôi cấu hình được JVM cho container (MaxRAMPercentage, CPU, GC log, heap dump, JFR) và tính được memory budget.
- [ ] Tôi dùng thành thạo `jcmd`, `jstat`, `jstack`, Eclipse MAT (dominator tree, retained heap, path to GC roots), JFR/JMC và async-profiler.
- [ ] Tôi viết được benchmark JMH đúng và giải thích các bẫy (dead code, constant folding, warm-up, coordinated omission).
- [ ] Tôi tự thực hiện được runbook CPU 100%, GC cao và OOM từ đầu tới cuối, và viết được postmortem.

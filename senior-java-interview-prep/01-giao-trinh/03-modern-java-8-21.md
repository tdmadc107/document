# Module 03 — Modern Java (8 → 21)

> **Mục tiêu:** sau module này bạn giải thích được lambda và Stream API hoạt động bên trong thế nào (invokedynamic, lazy evaluation, fusion pipeline), dùng đúng `Optional`, `java.time`, records, sealed classes, pattern matching; chọn được khi nào dùng parallel stream (và khi nào tuyệt đối không); đánh giá được rủi ro khi migrate một hệ thống từ Java 8 → 11 → 17 → 21.
> **Cấp độ:** Cơ bản → Nâng cao (Senior)
> **Thời lượng gợi ý:** 6 ngày (≈ 30 giờ)
> **Yêu cầu trước:** Module 01 (Java Core & OOP), Module 02 (Collections & Generics)
> **Nguồn tham khảo:**
> - Trong kho: [`Java/OCP-Oracle-Certified-Professional-Java-SE-17-Developer-Study-Guide-Exam-1Z0-829pdf.pdf`](../../Java/OCP-Oracle-Certified-Professional-Java-SE-17-Developer-Study-Guide-Exam-1Z0-829pdf.pdf) — Chương 3 (Making Decisions: switch expression, pattern matching), Chương 4 (Core APIs: date/time), Chương 7 (Beyond Classes: records, sealed classes, enums), Chương 8 (Lambdas and Functional Interfaces), Chương 10 (Streams), Chương 12 (Modules)
> - Trong kho: [`Java/OCP Oracle Certified Professional Java SE 21.pdf`](../../Java/OCP%20Oracle%20Certified%20Professional%20Java%20SE%2021.pdf) — các phần về pattern matching cho switch, record patterns, sequenced collections, virtual threads
> - Trong kho: [`Ebook IT/OCP_ Oracle Certified Professional Java SE 8 Programmer II Study Guide_ Exam 1Z0-809.pdf`](../../Ebook%20IT/OCP_%20Oracle%20Certified%20Professional%20Java%20SE%208%20Programmer%20II%20Study%20Guide_%20Exam%201Z0-809.pdf) — Chương 4 (Functional Programming), Chương 5 (Dates, Strings, and Localization)
> - Trong kho: [`Java/ocp-oracle-certified-professional-java-se-11-programmer-i-study-guide.pdf`](../../Java/ocp-oracle-certified-professional-java-se-11-programmer-i-study-guide.pdf) — `var`, lambda cơ bản
> - Trong kho: [`Java/OCP_ Oracle Certified Professional Java SE 11  Exam 1Z0-819 Practice Test.pdf`](../../Java/OCP_%20Oracle%20Certified%20Professional%20Java%20SE%2011%20%20Exam%201Z0-819%20Practice%20Test.pdf) — luyện câu hỏi bẫy về stream/lambda/modules
> - Trong kho: [`Ebook IT/Clean Code.pdf`](../../Ebook%20IT/Clean%20Code.pdf) — tinh thần viết code dễ đọc (áp dụng khi viết stream pipeline dài)
> - Ngoài: *Effective Java 3rd ed.* (Joshua Bloch) — Chương 7 "Lambdas and Streams" (Item 42–48), Item 55 "Return optionals judiciously", Item 17 "Minimize mutability"
> - Ngoài: Java Language Specification (https://docs.oracle.com/javase/specs/), Javadoc `java.util.stream` package summary (https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/package-summary.html)
> - Ngoài: OpenJDK JEPs — JEP 286 (var), 361 (switch expressions), 378 (text blocks), 395 (records), 409 (sealed classes), 394 (pattern matching instanceof), 441 (pattern matching switch), 440 (record patterns), 431 (sequenced collections), 261 (module system), 321 (HTTP Client), 403 (strong encapsulation) — https://openjdk.org/jeps/
> - Ngoài: Brian Goetz, "Translation of Lambda Expressions" (https://cr.openjdk.org/~briangoetz/lambda/lambda-translation.html)

## Mục lục
1. [Functional interface, lambda & method reference](#p1)
2. [Stream API — nền tảng và cơ chế lazy](#p2)
3. [Collectors chuyên sâu & các bẫy thường gặp](#p3)
4. [Parallel stream, ForkJoinPool & hiệu năng](#p4)
5. [Optional — dùng đúng và anti-pattern](#p5)
6. [java.time — thời gian, time zone, DST](#p6)
7. [Cú pháp mới: var, switch expression, text block](#p7)
8. [Records & sealed classes — mô hình dữ liệu hiện đại](#p8)
9. [Pattern matching (instanceof, switch, record patterns)](#p9)
10. [Sequenced Collections, JPMS, HttpClient](#p10)
11. [Bản đồ phiên bản LTS 8 / 11 / 17 / 21 (và 25) & migration](#p11)
12. [Dự án mini của module](#du-an-mini)
13. [Checklist tự đánh giá](#checklist-tu-danh-gia)

---

<a id="p1"></a>
## 1. Functional interface, lambda & method reference

### 1.1 Khái niệm

**Functional interface** là interface có **đúng một abstract method** (SAM — Single Abstract Method). Nó có thể có thêm `default` method, `static` method, và các method trùng chữ ký với `public` method của `Object` (ví dụ `equals`) — những cái này không tính. Annotation `@FunctionalInterface` không bắt buộc nhưng nên dùng: compiler sẽ báo lỗi nếu ai đó thêm abstract method thứ hai.

**Lambda expression** là cách viết gọn một implementation của functional interface. Kiểu của lambda **phụ thuộc ngữ cảnh** (target typing): cùng một lambda `x -> x + 1` có thể là `Function<Integer,Integer>`, `UnaryOperator<Integer>` hay `IntUnaryOperator` tuỳ chỗ nó được gán.

Các functional interface chuẩn trong `java.util.function` (cần thuộc lòng):

| Interface | Method | Ý nghĩa | Biến thể primitive |
|---|---|---|---|
| `Supplier<T>` | `T get()` | không vào, 1 ra | `IntSupplier`, `BooleanSupplier`... |
| `Consumer<T>` | `void accept(T)` | 1 vào, không ra | `IntConsumer`, `BiConsumer<T,U>` |
| `Function<T,R>` | `R apply(T)` | biến đổi | `IntFunction<R>`, `ToIntFunction<T>`, `IntToLongFunction`... |
| `Predicate<T>` | `boolean test(T)` | điều kiện | `IntPredicate`, `BiPredicate<T,U>` |
| `UnaryOperator<T>` | `T apply(T)` | `Function<T,T>` | `IntUnaryOperator` |
| `BinaryOperator<T>` | `T apply(T,T)` | `BiFunction<T,T,T>` | `IntBinaryOperator` |

```java
import java.util.*;
import java.util.function.*;

public class LambdaBasics {
    public static void main(String[] args) {
        Supplier<List<String>> listFactory = ArrayList::new;
        Predicate<String> notBlank = s -> !s.isBlank();
        Function<String, Integer> length = String::length;
        Function<String, String> trimThenUpper =
                ((Function<String, String>) String::trim).andThen(String::toUpperCase);

        List<String> list = listFactory.get();
        list.addAll(List.of("  java ", "", " stream "));
        list.stream()
            .filter(notBlank.and(s -> s.length() > 2))   // Predicate composition
            .map(trimThenUpper)
            .forEach(System.out::println);               // JAVA, STREAM

        BinaryOperator<Integer> max = BinaryOperator.maxBy(Comparator.naturalOrder());
        System.out.println(max.apply(3, 7));             // 7
        System.out.println(length.apply("hello"));       // 5
    }
}
```

**Method reference** có 4 dạng:

| Dạng | Ví dụ | Tương đương lambda |
|---|---|---|
| Static method | `Integer::parseInt` | `s -> Integer.parseInt(s)` |
| Instance method của **một object cụ thể** (bound) | `System.out::println` | `x -> System.out.println(x)` |
| Instance method của **object tuỳ ý** cùng kiểu (unbound) | `String::toUpperCase` | `s -> s.toUpperCase()` |
| Constructor | `ArrayList::new` | `() -> new ArrayList<>()` |

### 1.2 Capture và "effectively final"

Lambda có thể dùng biến local của method bao ngoài **chỉ khi biến đó final hoặc effectively final** (không bị gán lại sau khi khởi tạo). Lý do:

1. Lambda có thể chạy **sau khi** method đã return (ví dụ submit vào executor). Biến local nằm trên stack frame đã biến mất → Java **copy giá trị** vào lambda lúc tạo. Nếu cho phép gán lại, người đọc sẽ tưởng lambda thấy giá trị mới — nhưng thực ra không. Cấm luôn để tránh nhầm lẫn.
2. Tránh data race: nếu lambda chạy ở thread khác mà cùng sửa biến local, sẽ không có cơ chế đồng bộ nào bảo vệ.

```java
int count = 0;
List.of(1, 2, 3).forEach(x -> count++);   // ❌ compile error: count không effectively final

// "Lách luật" bằng mảng 1 phần tử hoặc AtomicInteger — compile được,
// nhưng nếu stream chạy parallel thì int[] sẽ race condition.
int[] holder = {0};
List.of(1, 2, 3).forEach(x -> holder[0]++); // chạy được, nhưng là code smell
```

Lambda dùng field instance (`this.x`) thì **capture `this`** — nghĩa là lambda giữ reference tới object bao ngoài. Đây là nguồn gốc memory leak kinh điển: đăng ký một lambda listener vào một object sống lâu (event bus, cache) thì toàn bộ object chứa lambda cũng không được GC.

Khác biệt với anonymous class:
- Trong lambda, `this` là object bao ngoài; trong anonymous class, `this` là chính instance anonymous.
- Lambda không tạo scope mới cho biến: không được khai báo biến trùng tên biến local bên ngoài.
- Anonymous class tạo trong instance context, với javac trước Java 18, **luôn** giữ reference tới enclosing instance (field `this$0`) dù không dùng; từ JDK 18 javac bỏ field này khi không cần (trừ một số trường hợp như class Serializable). Lambda thì từ đầu chỉ capture `this` khi thực sự dùng.

### 1.3 Bên dưới nắp capo: invokedynamic & LambdaMetafactory

Lambda **không** được compile thành file `Outer$1.class` như anonymous class. Quá trình:

1. `javac` "desugar" thân lambda thành một **private static method** (hoặc private instance method nếu capture `this`) tên kiểu `lambda$main$0` trong chính class đó.
2. Tại vị trí tạo lambda, `javac` sinh lệnh bytecode **`invokedynamic`** với bootstrap method là `LambdaMetafactory.metafactory`.
3. Lần đầu lệnh này được thực thi, JVM gọi bootstrap → spin ra một class implement functional interface (từ Java 15 là **hidden class**, JEP 371), rồi gắn `CallSite` vào vị trí đó. Các lần sau dùng lại `CallSite` đã link.
4. Lambda **không capture** gì → JVM thường trả về **cùng một instance** mỗi lần (singleton). Lambda có capture → mỗi lần evaluate tạo object mới chứa giá trị capture (nhưng JIT có thể loại bỏ allocation bằng escape analysis).

```bash
javap -c -p LambdaBasics.class
# ... invokedynamic #7,  0  // InvokeDynamic #0:test:()Ljava/util/function/Predicate;
# private static boolean lambda$main$0(java.lang.String);
```

Hệ quả cần biết:
- Lần gọi **đầu tiên** của mỗi lambda có chi phí bootstrap (vài chục micro-giây) → ảnh hưởng startup time; đây là lý do các framework như GraalVM native-image phải xử lý lambda đặc biệt.
- Stack trace của exception trong lambda chứa các frame `lambda$xxx$N` — học cách đọc chúng.
- Không nên dựa vào identity của lambda (`==`) — đặc tả không đảm bảo.

> 💡 **Góc nhìn Senior:**
> - Câu hỏi "lambda có tạo object mới mỗi lần không?" — trả lời chuẩn: *non-capturing lambda thường được cache thành singleton tại call site; capturing lambda tạo instance mới mỗi lần evaluate, nhưng JIT có thể scalar-replace nếu không escape*. Đừng tối ưu sớm; chỉ quan tâm trong hot loop đã profile.
> - Method reference dạng bound (`obj::method`) **evaluate `obj` ngay lúc tạo**: `Supplier<Integer> s = str::length;` với `str == null` ném `NullPointerException` ngay tại dòng gán, còn `() -> str.length()` chỉ ném khi gọi `get()`. Đây là bẫy hay gặp trong câu hỏi phỏng vấn.
> - Checked exception: functional interface chuẩn không khai báo `throws`, nên lambda ném `IOException` phải wrap (`UncheckedIOException`) hoặc tự định nghĩa `ThrowingFunction`. Đừng nuốt exception trong lambda.

> ⚠️ **Lỗi thường gặp:**
> - Dùng lambda có side-effect sửa collection bên ngoài trong `stream().forEach` → vỡ khi chuyển sang parallel.
> - Gặp lỗi "reference to X is ambiguous" khi overload method nhận cả `Callable<T>` và `Runnable` (`executor.submit(() -> foo())`): compiler chọn theo việc lambda body có trả giá trị hay không — cần hiểu để đọc lỗi.
> - Lạm dụng lambda nhiều dòng: lambda > 3–5 dòng nên tách thành method có tên rồi dùng method reference (Effective Java Item 42: "Prefer lambdas to anonymous classes", Item 43: "Prefer method references to lambdas" khi rõ ràng hơn).

### 🛠 Bài tập phần 1

**Bài 1.1 — Composition (Cơ bản)**
- Đề bài: Viết `Predicate<String> isValidEmail` bằng cách kết hợp (`and`, `or`, `negate`) ít nhất 3 predicate nhỏ: không null/blank, có đúng một ký tự `@`, phần domain chứa dấu `.`. Viết `Function<String,String> normalize` = `trim` → `toLowerCase`.
- Tiêu chí đạt: không dùng `if` trong code chính; có unit test cho 5 trường hợp hợp lệ/không hợp lệ; dùng `Predicate.not(...)` (Java 11) ít nhất một lần.

**Bài 1.2 — Bound vs unbound method reference (Trung bình)**
- Đề bài: Viết chương trình chứng minh khác biệt thời điểm NPE giữa `str::length` và `() -> str.length()`. Thêm một ví dụ chứng minh non-capturing lambda trả về cùng instance khi tạo trong vòng lặp còn capturing lambda thì không (in `System.identityHashCode`).
- Tiêu chí đạt: giải thích được output bằng lời, ghi rõ "đây là hành vi của HotSpot, không phải đảm bảo của đặc tả".

**Bài 1.3 — ThrowingFunction (Nâng cao)**
- Đề bài: Định nghĩa `@FunctionalInterface interface ThrowingFunction<T,R,E extends Exception>` và một static helper `unchecked(ThrowingFunction<T,R,?> f)` trả về `Function<T,R>`, wrap checked exception thành `RuntimeException` (giữ nguyên cause). Dùng nó để đọc nội dung danh sách file bằng `Files.readString` trong stream.
- Tiêu chí đạt: `IOException` gốc truy được qua `getCause()`; `UncheckedIOException` được dùng riêng cho `IOException`; có test với file không tồn tại.

<details>
<summary>Gợi ý lời giải</summary>

```java
// 1.1
Predicate<String> notBlank = s -> s != null && !s.isBlank();
Predicate<String> oneAt = s -> s.indexOf('@') >= 0 && s.indexOf('@') == s.lastIndexOf('@');
Predicate<String> domainHasDot = s -> s.substring(s.indexOf('@') + 1).contains(".");
Predicate<String> isValidEmail = notBlank.and(oneAt).and(domainHasDot);
// thứ tự and() là short-circuit → domainHasDot chỉ chạy khi oneAt đúng

// 1.2
String str = null;
Supplier<Integer> lazy = () -> str.length();   // OK, chưa NPE
try { Supplier<Integer> eager = str::length; } // NPE tại đây (Objects.requireNonNull trong bytecode)
catch (NullPointerException e) { System.out.println("NPE lúc tạo method ref"); }

for (int i = 0; i < 3; i++) {
    Runnable r = () -> {};                       // non-capturing → thường cùng identity
    int j = i; Runnable c = () -> System.out.print(j); // capturing → khác identity
    System.out.println(System.identityHashCode(r) + " " + System.identityHashCode(c));
}

// 1.3
@FunctionalInterface
interface ThrowingFunction<T, R, E extends Exception> { R apply(T t) throws E; }

static <T, R> Function<T, R> unchecked(ThrowingFunction<T, R, ?> f) {
    return t -> {
        try { return f.apply(t); }
        catch (IOException e) { throw new UncheckedIOException(e); }
        catch (RuntimeException e) { throw e; }
        catch (Exception e) { throw new RuntimeException(e); }
    };
}
List<String> contents = paths.stream().map(unchecked(Files::readString)).toList();
```
</details>

---

<a id="p2"></a>
## 2. Stream API — nền tảng và cơ chế lazy

### 2.1 Khái niệm

Stream là **một chuỗi phần tử hỗ trợ thao tác tổng hợp (aggregate operations)**, KHÔNG phải cấu trúc lưu trữ. Đặc điểm:

- **Không lưu dữ liệu**: lấy dữ liệu từ source (collection, array, I/O, generator).
- **Functional**: không sửa source (nếu bạn sửa source trong pipeline → hành vi không xác định, có thể `ConcurrentModificationException`).
- **Lazy**: intermediate operation không chạy gì cả cho đến khi có terminal operation.
- **Dùng một lần**: sau terminal operation, stream bị "consumed"; gọi tiếp ném `IllegalStateException: stream has already been operated upon or closed`.
- **Có thể vô hạn**: `Stream.iterate`, `Stream.generate` — cần short-circuit để kết thúc.

Một pipeline = **source → 0..n intermediate operations → 1 terminal operation**.

| Loại | Ví dụ | Ghi chú |
|---|---|---|
| Intermediate **stateless** | `filter`, `map`, `flatMap`, `peek`, `mapMulti` (16) | xử lý từng phần tử độc lập |
| Intermediate **stateful** | `distinct`, `sorted`, `limit`, `skip` | cần nhớ trạng thái; `sorted` phải buffer **toàn bộ** stream |
| Intermediate short-circuit | `limit`, `takeWhile` (9) | biến stream vô hạn thành hữu hạn |
| Terminal | `forEach`, `collect`, `reduce`, `count`, `toList` (16), `min`, `max` | kích hoạt pipeline |
| Terminal short-circuit | `findFirst`, `findAny`, `anyMatch`, `allMatch`, `noneMatch` | dừng sớm |

### 2.2 Lazy evaluation và "vertical" execution

Nhiều người tưởng stream chạy "từng tầng": filter hết → map hết → collect. Thực tế stream chạy **theo chiều dọc**: mỗi phần tử đi qua toàn bộ chuỗi operation rồi mới tới phần tử tiếp theo (trừ khi gặp stateful op như `sorted` — lúc đó là một "barrier").

```java
import java.util.stream.*;

public class LazyDemo {
    public static void main(String[] args) {
        Stream.of("a", "bb", "ccc", "dddd")
              .filter(s -> { System.out.println("filter " + s); return s.length() > 1; })
              .map(s -> { System.out.println("map " + s); return s.toUpperCase(); })
              .findFirst()
              .ifPresent(System.out::println);
    }
}
// Output:
// filter a
// filter bb
// map bb
// BB
// → "ccc", "dddd" KHÔNG bao giờ được xử lý nhờ short-circuit.
```

Thêm `.sorted()` giữa `filter` và `map`: tất cả phần tử đi qua `filter` trước, bị buffer trong `sorted`, rồi mới chảy tiếp xuống `map`. Do đó `sorted` trên stream vô hạn → treo vĩnh viễn (và có thể `OutOfMemoryError`).

### 2.3 Bên dưới nắp capo: Spliterator, Sink, stream flags

- **Source** được bọc trong một `Spliterator` — "iterator có thể chia đôi" (`trySplit`) cho parallel, kèm các **characteristics**: `SIZED`, `SUBSIZED`, `ORDERED`, `DISTINCT`, `SORTED`, `NONNULL`, `IMMUTABLE`, `CONCURRENT`.
- Mỗi intermediate operation tạo ra một `AbstractPipeline` stage nối vào stage trước (linked list). **Chưa có gì chạy**.
- Khi gọi terminal op, các stage được "wrap" thành chuỗi `Sink` lồng nhau (`begin`, `accept`, `end`, `cancellationRequested`). Source đẩy từng phần tử vào sink đầu tiên → các op được **hợp nhất (fused)** thành một vòng lặp duy nhất.
- Stream flags được tối ưu: ví dụ `list.stream().sorted()` trên `TreeSet` (đã `SORTED`) → `sorted()` thành no-op; `distinct()` trên `Set` cũng vậy.
- Từ Java 9, `count()` có thể **không chạy pipeline** nếu biết trước kích thước (`SIZED`) và không có op nào thay đổi số lượng → `peek` bên trong **không được gọi**. Bài học: đừng đặt logic nghiệp vụ trong `peek`.

```java
long n = List.of(1, 2, 3).stream().peek(System.out::println).count();
// Java 8: in 1 2 3. Java 9+: KHÔNG in gì, n = 3.
```

### 2.4 Các operation quan trọng

```java
import java.util.*;
import java.util.stream.*;

record Order(String id, String customer, List<Item> items) {}
record Item(String sku, int qty, double price) {}

public class StreamOps {
    public static void main(String[] args) {
        List<Order> orders = List.of(
            new Order("O1", "an",  List.of(new Item("A", 2, 10.0), new Item("B", 1, 5.0))),
            new Order("O2", "binh", List.of(new Item("A", 1, 10.0))),
            new Order("O3", "an",  List.of()));

        // flatMap: 1 → n. Mỗi Order thành stream các Item, rồi làm phẳng.
        double revenue = orders.stream()
            .flatMap(o -> o.items().stream())
            .mapToDouble(i -> i.qty() * i.price())   // primitive stream → tránh boxing
            .sum();
        System.out.println(revenue); // 35.0

        // mapMulti (Java 16): thay flatMap khi mỗi phần tử sinh ít/0 phần tử, tránh tạo Stream con
        List<String> skus = orders.stream()
            .<String>mapMulti((o, sink) -> o.items().forEach(i -> sink.accept(i.sku())))
            .distinct()
            .toList();
        System.out.println(skus); // [A, B]

        // takeWhile / dropWhile (Java 9) — chỉ có nghĩa trên stream có thứ tự
        System.out.println(Stream.of(1, 2, 3, 10, 4).takeWhile(x -> x < 5).toList()); // [1, 2, 3]

        // iterate 3 tham số (Java 9) — giống for-loop
        Stream.iterate(1, x -> x <= 100, x -> x * 3).forEach(System.out::println); // 1 3 9 27 81

        // reduce
        int sum = IntStream.rangeClosed(1, 10).reduce(0, Integer::sum);
        Optional<String> longest = Stream.of("a", "abc", "ab")
            .reduce((a, b) -> a.length() >= b.length() ? a : b);
        System.out.println(sum + " " + longest.orElseThrow()); // 55 abc
    }
}
```

**`reduce` — 3 dạng và các điều kiện toán học**:
1. `Optional<T> reduce(BinaryOperator<T>)`
2. `T reduce(T identity, BinaryOperator<T>)`
3. `<U> U reduce(U identity, BiFunction<U,T,U> accumulator, BinaryOperator<U> combiner)`

Yêu cầu: `accumulator` phải **associative**, **non-interfering**, **stateless**; `identity` phải là phần tử trung hoà thật (`combiner(identity, u) == u`). Ví dụ sai: `reduce(10, Integer::sum)` cho kết quả khác nhau giữa sequential và parallel (parallel cộng 10 nhiều lần — mỗi chunk một lần). Với kết quả là container mutable (List, StringBuilder) **dùng `collect`**, không dùng `reduce` (reduce tạo object mới mỗi bước → O(n²) copy).

**`Stream.toList()` (Java 16) vs `Collectors.toList()`**:
- `toList()` trả về list **unmodifiable**, **cho phép null**.
- `Collectors.toList()` không đảm bảo kiểu/mutability (thực tế là `ArrayList`) — code cũ có thể đang `add` vào list này; đổi sang `toList()` sẽ gây `UnsupportedOperationException` ở runtime.
- `Collectors.toUnmodifiableList()` (Java 10) **ném NPE nếu có null**.

> 💡 **Góc nhìn Senior:**
> - Dùng primitive stream (`IntStream`, `LongStream`, `DoubleStream`, `mapToInt`) cho tính toán số: tránh boxing/unboxing, giảm áp lực GC rõ rệt với dữ liệu lớn.
> - Stream trên I/O (`Files.lines`, `BufferedReader.lines`, `Files.walk`) giữ file handle → **phải** đóng bằng try-with-resources (`Stream` implement `AutoCloseable`). Quên đóng `Files.walk` trên production → "Too many open files".
> - Stream không phải lúc nào cũng dễ đọc hơn loop. Logic có nhiều nhánh, cần `break` có điều kiện phức tạp, cần xử lý checked exception, hoặc cần cập nhật nhiều biến trạng thái → for-loop rõ hơn (Effective Java Item 45: "Use streams judiciously").

> ⚠️ **Lỗi thường gặp:**
> - Lưu stream vào biến rồi dùng hai lần → `IllegalStateException`. Nếu cần dùng lại, lưu `Supplier<Stream<T>>`.
> - Sửa source collection trong pipeline (`list.stream().forEach(x -> list.remove(x))`) → `ConcurrentModificationException` hoặc kết quả sai.
> - `Stream.of(array)` với `int[]` → ra `Stream<int[]>` có 1 phần tử, không phải `IntStream`. Dùng `Arrays.stream(int[])` hoặc `IntStream.of(...)`.
> - Gọi `sorted()` / `distinct()` trên stream vô hạn trước `limit` → treo.

### 🛠 Bài tập phần 2

**Bài 2.1 — Thứ tự thực thi (Cơ bản)**
- Đề bài: Viết pipeline `Stream.of(5, 3, 8, 1, 9, 2)` với `peek` in log ở mỗi bước: `filter(x > 2)` → `map(x * 10)` → `limit(2)` → `toList()`. Dự đoán output **trước khi chạy**, sau đó chạy và so sánh. Lặp lại với `sorted()` chèn sau `filter`.
- Tiêu chí đạt: viết ra bảng thứ tự log dự đoán; giải thích vì sao `sorted` làm thay đổi thứ tự in log.

**Bài 2.2 — Word frequency (Trung bình)**
- Đề bài: Đọc một file text lớn (≥ 10MB, có thể tự sinh), đếm tần suất từ (lowercase, bỏ dấu câu), in top 20. Dùng `Files.lines` + try-with-resources.
- Tiêu chí đạt: không load toàn file vào `String`; dùng `flatMap` + `Pattern.splitAsStream`; kết quả đúng với một implementation for-loop đối chứng; đo thời gian cả hai cách.

**Bài 2.3 — Custom Spliterator (Nâng cao)**
- Đề bài: Viết `Spliterator<List<T>>` chia một `List<T>` thành các batch kích thước `n` (ví dụ để gửi batch insert DB). Hàm `static <T> Stream<List<T>> batches(List<T> src, int n, boolean parallel)`. Implement `trySplit` để parallel stream chia được.
- Tiêu chí đạt: `characteristics()` khai báo đúng `SIZED | SUBSIZED | ORDERED | IMMUTABLE`; test với `n` không chia hết; parallel cho cùng kết quả (sau `toList()`) như sequential.

<details>
<summary>Gợi ý lời giải</summary>

2.1: không có `sorted`, log xen kẽ theo từng phần tử: `peek-src 5, peek-filter 5, peek-map 50, peek-src 3, peek-filter 3, peek-map 30` → `limit(2)` đủ → dừng, 8/1/9/2 không bao giờ được đọc. Có `sorted`: toàn bộ 6 phần tử qua source + filter trước, rồi `sorted` phát ra `3, 5` cho map.

2.2:
```java
Pattern WORD = Pattern.compile("[^\\p{L}\\p{N}]+");
try (Stream<String> lines = Files.lines(path)) {
    Map<String, Long> freq = lines
        .flatMap(WORD::splitAsStream)
        .filter(w -> !w.isEmpty())
        .map(w -> w.toLowerCase(Locale.ROOT))
        .collect(Collectors.groupingBy(w -> w, Collectors.counting()));
    freq.entrySet().stream()
        .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
        .limit(20)
        .forEach(e -> System.out.println(e.getKey() + "=" + e.getValue()));
}
```
Lưu ý `Locale.ROOT` để tránh bug "Turkish I".

2.3:
```java
final class BatchSpliterator<T> implements Spliterator<List<T>> {
    private final List<T> src; private final int size; private int from; private final int to;
    BatchSpliterator(List<T> src, int size, int from, int to) { ... }
    public boolean tryAdvance(Consumer<? super List<T>> action) {
        if (from >= to) return false;
        int end = Math.min(from + size, to);
        action.accept(src.subList(from, end)); from = end; return true;
    }
    public Spliterator<List<T>> trySplit() {
        int batches = (to - from + size - 1) / size;
        if (batches < 2) return null;
        int mid = from + (batches / 2) * size;          // cắt đúng biên batch
        var prefix = new BatchSpliterator<>(src, size, from, mid);
        from = mid; return prefix;
    }
    public long estimateSize() { return (to - from + size - 1) / size; }
    public int characteristics() { return SIZED | SUBSIZED | ORDERED | IMMUTABLE | NONNULL; }
}
// StreamSupport.stream(new BatchSpliterator<>(src, n, 0, src.size()), parallel)
```
</details>

---

<a id="p3"></a>
## 3. Collectors chuyên sâu & các bẫy thường gặp

### 3.1 `Collector` là gì

`Collector<T, A, R>` gồm 4 hàm + characteristics:
- `supplier()` — tạo container trung gian `A`
- `accumulator()` — `(A, T) -> void`, đưa phần tử vào container
- `combiner()` — gộp hai container (dùng khi parallel)
- `finisher()` — `A -> R`
- characteristics: `IDENTITY_FINISH`, `UNORDERED`, `CONCURRENT`

Đây là **mutable reduction**: hiệu quả hơn `reduce` cho container vì không copy.

### 3.2 `toMap` — ba bẫy kinh điển

```java
import java.util.*;
import java.util.stream.*;

record Emp(String name, String dept, Double salary) {}

public class ToMapPitfalls {
    public static void main(String[] args) {
        List<Emp> emps = List.of(
            new Emp("An", "IT", 1000.0),
            new Emp("Binh", "IT", 1200.0),
            new Emp("Chi", "HR", null));

        // ❌ Bẫy 1: key trùng → IllegalStateException: Duplicate key IT (attempted merging values ...)
        // emps.stream().collect(Collectors.toMap(Emp::dept, Emp::name));

        // ✅ Cung cấp merge function
        Map<String, String> byDept = emps.stream()
            .collect(Collectors.toMap(Emp::dept, Emp::name, (a, b) -> a + "," + b));
        System.out.println(byDept); // {HR=Chi, IT=An,Binh}  (thứ tự HashMap không đảm bảo)

        // ❌ Bẫy 2: value null → NullPointerException (toMap dùng HashMap.merge, merge cấm value null)
        // emps.stream().collect(Collectors.toMap(Emp::name, Emp::salary));

        // ✅ Cách xử lý null value: dùng collect 3 tham số với HashMap::put
        Map<String, Double> salaries = emps.stream()
            .collect(HashMap::new, (m, e) -> m.put(e.name(), e.salary()), HashMap::putAll);
        System.out.println(salaries);

        // ✅ Bẫy 3: cần giữ thứ tự / sorted → chỉ định map factory (tham số thứ 4)
        Map<String, String> ordered = emps.stream()
            .collect(Collectors.toMap(Emp::name, Emp::dept, (a, b) -> a, LinkedHashMap::new));
        System.out.println(ordered); // {An=IT, Binh=IT, Chi=HR}
    }
}
```

> ⚠️ **Lỗi thường gặp trên production:** code chạy đúng trên dữ liệu test (không trùng key) nhưng nổ `IllegalStateException` khi dữ liệu thật có bản ghi trùng (do import lặp, do key không unique như kỳ vọng). Mỗi lần viết `toMap` 2 tham số, hãy tự hỏi: *"key này có chắc unique 100% không, ai đảm bảo?"*. Nếu muốn fail-fast thì cũng nên chủ động ném exception có thông điệp nghiệp vụ rõ ràng.

### 3.3 `groupingBy`, `partitioningBy` và downstream collector

```java
import java.util.*;
import static java.util.stream.Collectors.*;

public class Grouping {
    record Tx(String account, String type, long amount) {}

    public static void main(String[] args) {
        List<Tx> txs = List.of(
            new Tx("A1", "DEBIT", 100), new Tx("A1", "CREDIT", 300),
            new Tx("A2", "DEBIT", 50),  new Tx("A1", "DEBIT", 20));

        // groupingBy mặc định: HashMap<K, ArrayList<T>>
        Map<String, List<Tx>> byAcc = txs.stream().collect(groupingBy(Tx::account));

        // Downstream: tổng tiền theo account
        Map<String, Long> total = txs.stream().collect(groupingBy(Tx::account, summingLong(Tx::amount)));

        // Lồng nhiều cấp + TreeMap để có thứ tự
        Map<String, Map<String, Long>> nested = txs.stream().collect(
            groupingBy(Tx::account, TreeMap::new, groupingBy(Tx::type, summingLong(Tx::amount))));
        System.out.println(nested); // {A1={CREDIT=300, DEBIT=120}, A2={DEBIT=50}}

        // mapping / filtering (9) / flatMapping (9)
        Map<String, Set<String>> typesPerAcc = txs.stream()
            .collect(groupingBy(Tx::account, mapping(Tx::type, toSet())));
        Map<String, List<Tx>> bigOnly = txs.stream()
            .collect(groupingBy(Tx::account, filtering(t -> t.amount() > 60, toList())));
        System.out.println(bigOnly); // A2 vẫn xuất hiện với list rỗng — khác filter() trước groupingBy

        // partitioningBy: luôn có đủ 2 key true/false (kể cả rỗng)
        Map<Boolean, Long> bigSmall = txs.stream()
            .collect(partitioningBy(t -> t.amount() >= 100, counting()));
        System.out.println(bigSmall); // {false=2, true=2}

        // collectingAndThen: hậu xử lý
        Map<String, Tx> maxPerAcc = txs.stream().collect(groupingBy(Tx::account,
            collectingAndThen(maxBy(Comparator.comparingLong(Tx::amount)), Optional::orElseThrow)));

        // teeing (12): hai collector song song rồi gộp
        record Stats(long count, long sum) {}
        Stats s = txs.stream().collect(teeing(counting(), summingLong(Tx::amount), Stats::new));
        System.out.println(s); // Stats[count=4, sum=470]
    }
}
```

Điểm tinh tế:
- `groupingBy` **không chấp nhận key null** → NPE "element cannot be mapped to a null key". Nếu classifier có thể trả null, map sang giá trị sentinel trước.
- `filter(...).collect(groupingBy(...))` khác `groupingBy(..., filtering(...))`: cách đầu **mất luôn nhóm** không có phần tử thỏa mãn, cách sau giữ nhóm với list rỗng.
- `counting()` trả `Long`, `summingInt` trả `Integer`, `averagingX` trả `Double` — hay gây lỗi type khi gán.
- `groupingByConcurrent` trả `ConcurrentMap`, có characteristic `CONCURRENT | UNORDERED` → khi parallel, tất cả thread cùng ghi vào một map thay vì mỗi thread một map rồi merge. Nhanh hơn khi không cần thứ tự.

### 3.4 Viết Collector tùy biến

```java
// Collector gom phần tử thành các chunk kích thước n
static <T> Collector<T, ?, List<List<T>>> chunked(int n) {
    return Collector.of(
        ArrayList<List<T>>::new,
        (acc, t) -> {
            if (acc.isEmpty() || acc.get(acc.size() - 1).size() == n) acc.add(new ArrayList<>());
            acc.get(acc.size() - 1).add(t);
        },
        (left, right) -> { throw new UnsupportedOperationException("sequential only"); });
}
```
Combiner ném exception là cách "trung thực" khi collector không thể gộp đúng ngữ nghĩa ở parallel — nhưng tài liệu hoá rõ ràng. Một collector chuẩn mực phải có combiner đúng.

> 💡 **Góc nhìn Senior:** khi review code, các dấu hiệu cần dừng lại: `toMap` không có merge function, `groupingBy` với classifier có thể null, `Collectors.toList()` mà code sau đó `add`/`remove`, `collect` trong vòng lặp lồng nhau (O(n²) ẩn). Trên tập dữ liệu lớn (hàng triệu bản ghi) `groupingBy` mặc định tạo hàng triệu `ArrayList` nhỏ — cân nhắc downstream collector tính trực tiếp (`summingLong`, `counting`) thay vì gom list rồi mới tính.

### 🛠 Bài tập phần 3

**Bài 3.1 — Báo cáo doanh thu (Cơ bản)**
- Đề bài: Cho `List<Sale(region, product, LocalDate date, BigDecimal amount)>`. Tính: tổng doanh thu theo region (sắp xếp theo tên region), sản phẩm bán chạy nhất mỗi region, và chia sale thành ≥/< 1 triệu bằng `partitioningBy`.
- Tiêu chí đạt: dùng `BigDecimal` cộng bằng `reducing(BigDecimal.ZERO, Sale::amount, BigDecimal::add)`; kết quả là `TreeMap`.

**Bài 3.2 — toMap an toàn (Trung bình)**
- Đề bài: Viết tiện ích `toMapStrict(keyFn, valueFn)` ném `IllegalArgumentException` có thông điệp chứa **cả hai giá trị** bị trùng và key; và `toMapAllowNullValues(keyFn, valueFn)`.
- Tiêu chí đạt: unit test cho key trùng, value null, giữ thứ tự chèn.

**Bài 3.3 — Collector thống kê (Nâng cao)**
- Đề bài: Viết `Collector<Double, ?, Percentiles>` tính p50, p95, p99 (dùng sort mảng ở finisher là chấp nhận được). Combiner phải đúng khi chạy parallel.
- Tiêu chí đạt: kết quả parallel == sequential trên 1 triệu số ngẫu nhiên (seed cố định); giải thích độ phức tạp O(n log n) và bộ nhớ O(n); nêu một cấu trúc xấp xỉ dùng bộ nhớ hằng số (t-digest, HdrHistogram) cho production.

<details>
<summary>Gợi ý lời giải</summary>

```java
// 3.1
Map<String, BigDecimal> byRegion = sales.stream().collect(groupingBy(Sale::region, TreeMap::new,
        reducing(BigDecimal.ZERO, Sale::amount, BigDecimal::add)));
Map<String, String> topProduct = sales.stream().collect(groupingBy(Sale::region,
        collectingAndThen(groupingBy(Sale::product, reducing(BigDecimal.ZERO, Sale::amount, BigDecimal::add)),
            m -> m.entrySet().stream().max(Map.Entry.comparingByValue()).orElseThrow().getKey())));

// 3.2
static <T, K, V> Collector<T, ?, Map<K, V>> toMapStrict(Function<T, K> k, Function<T, V> v) {
    return Collector.of(LinkedHashMap::new,
        (Map<K, V> m, T t) -> {
            K key = k.apply(t); V val = v.apply(t);
            if (m.containsKey(key))
                throw new IllegalArgumentException("Duplicate key " + key + ": " + m.get(key) + " vs " + val);
            m.put(key, val);
        },
        (m1, m2) -> { m2.forEach((key, val) -> { if (m1.containsKey(key)) throw new IllegalArgumentException("Duplicate key " + key); m1.put(key, val); }); return m1; });
}
```
3.3: container là `DoubleArrayList` tự viết hoặc `ArrayList<Double>`; combiner = `addAll`; finisher sort rồi lấy chỉ số `ceil(p * n) - 1`.
</details>

---

<a id="p4"></a>
## 4. Parallel stream, ForkJoinPool & hiệu năng

### 4.1 Parallel stream hoạt động thế nào

`collection.parallelStream()` hoặc `.parallel()`:
1. Spliterator của source được `trySplit` đệ quy thành các chunk.
2. Mỗi chunk là một task chạy trên **`ForkJoinPool.commonPool()`** (dùng thuật toán work-stealing — xem Module 04).
3. Kết quả từng chunk được combine lại.

`commonPool` có parallelism mặc định = `Runtime.getRuntime().availableProcessors() - 1` (thread gọi stream cũng tham gia làm việc). Có thể chỉnh bằng `-Djava.util.concurrent.ForkJoinPool.common.parallelism=N`. Trong container, `availableProcessors()` phản ánh CPU limit (từ 8u191 / 10+ với container support) — nếu pod giới hạn 1 CPU, parallelism có thể = 1 hoặc thấp, parallel stream gần như vô dụng.

### 4.2 Khi nào parallel stream có lợi

Mô hình **NQ** (Brian Goetz): N = số phần tử, Q = chi phí xử lý mỗi phần tử. Parallel đáng cân nhắc khi **N × Q lớn** (kinh nghiệm: > 10.000 "đơn vị công việc"), và:

| Yếu tố | Tốt cho parallel | Xấu cho parallel |
|---|---|---|
| Source | `ArrayList`, array, `IntStream.range` (split O(1), cân bằng) | `LinkedList`, `Stream.iterate`, `BufferedReader.lines()` (split kém) |
| Operation | stateless, CPU-bound, độc lập | `sorted`, `distinct`, `limit` trên stream có thứ tự, `findFirst` |
| Terminal | `sum`, `reduce` associative, `collect` với `groupingByConcurrent` | `forEachOrdered`, `collect` vào `LinkedHashMap` |
| Dữ liệu | primitive, locality tốt | boxed object rải rác trên heap (cache miss) |
| Môi trường | batch job, CLI, máy nhiều core rảnh | **web server** đang phục vụ request song song |

### 4.3 Các bẫy production nghiêm trọng

**Bẫy 1 — Blocking I/O trong parallel stream làm nghẽn toàn JVM**

```java
// ❌ Gọi HTTP/DB trong parallel stream
List<Price> prices = productIds.parallelStream()
    .map(id -> priceClient.fetch(id))   // block 200ms mỗi call
    .toList();
```
Tất cả thread của `commonPool` bị block chờ I/O. Mà `commonPool` được **dùng chung** cho mọi parallel stream và mọi `CompletableFuture.supplyAsync(...)` không chỉ định executor trong cả JVM → các phần khác của ứng dụng bị "đói". Đây là sự cố thực tế: latency toàn service tăng vọt chỉ vì một endpoint báo cáo dùng parallel stream gọi API.

Giải pháp: dùng `CompletableFuture` với **executor riêng** có kích thước phù hợp cho I/O, hoặc virtual threads (Java 21, Module 04).

**Bẫy 2 — Shared mutable state**

```java
List<Integer> result = new ArrayList<>();
IntStream.range(0, 10_000).parallel().forEach(result::add); // ❌ race: mất phần tử hoặc ArrayIndexOutOfBounds
List<Integer> ok = IntStream.range(0, 10_000).parallel().boxed().toList(); // ✅
```

**Bẫy 3 — Thủ thuật "chạy parallel stream trong ForkJoinPool riêng"**

```java
ForkJoinPool pool = new ForkJoinPool(8);
List<R> r = pool.submit(() -> list.parallelStream().map(this::work).toList()).get();
```
Cách này "chạy được" vì task con fork từ trong worker thread của một pool sẽ chạy trong pool đó — nhưng đây là **implementation detail**, không được đặc tả đảm bảo. Dùng được cho tool nội bộ, nhưng với code nghiệp vụ nên dùng `ExecutorService`/`CompletableFuture` tường minh. Và nhớ `shutdown()` pool.

**Bẫy 4 — Thứ tự và kết quả không xác định**: `findAny`, `forEach` trên parallel cho thứ tự ngẫu nhiên. `forEachOrdered` giữ thứ tự nhưng mất phần lớn lợi ích song song. `unordered()` giúp `distinct`/`limit` nhanh hơn khi không cần thứ tự.

### 4.4 Stream vs for-loop: hiệu năng thật

- Với collection nhỏ (vài trăm phần tử) và thao tác đơn giản, for-loop thường nhanh hơn stream một chút (stream có chi phí dựng pipeline, lambda, có thể boxing). Sau khi JIT warm-up, chênh lệch thường nhỏ.
- Với primitive stream (`IntStream.sum`), JIT có thể tối ưu gần như for-loop.
- **Không bao giờ** kết luận hiệu năng bằng `System.currentTimeMillis()` quanh một lần chạy: JIT warm-up, dead-code elimination, GC làm sai số. Dùng **JMH** (Java Microbenchmark Harness).

```java
// Khung JMH tối thiểu (thêm dependency org.openjdk.jmh:jmh-core + jmh-generator-annprocess)
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime) @OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3) @Measurement(iterations = 5) @Fork(1)
public class SumBench {
    @Param({"1000", "1000000"}) int n;
    int[] data;
    @Setup public void setup() { data = new Random(42).ints(n).toArray(); }
    @Benchmark public long loop() { long s = 0; for (int x : data) s += x; return s; }
    @Benchmark public long stream() { return Arrays.stream(data).asLongStream().sum(); }
    @Benchmark public long parallel() { return Arrays.stream(data).parallel().asLongStream().sum(); }
}
```

> 💡 **Góc nhìn Senior:** quy tắc thực dụng: *mặc định sequential; chỉ bật `parallel()` sau khi đo bằng JMH trên phần cứng giống production, với workload CPU-bound, source split tốt, và chắc chắn không chạy trong thread xử lý request của web server*. Trong phỏng vấn, nếu được hỏi "làm sao tăng tốc stream xử lý 1 triệu bản ghi", đừng trả lời ngay "dùng parallel" — hãy hỏi lại: CPU-bound hay I/O-bound? chạy ở đâu? có cần thứ tự?

> ⚠️ **Lỗi thường gặp:** dùng `parallelStream()` trong code xử lý request Spring MVC; dùng `synchronized` trong lambda của parallel stream để "sửa" race (giết luôn lợi ích song song); `reduce` với identity sai; quên rằng `CompletableFuture.supplyAsync` mặc định cũng dùng `commonPool`.

### 🛠 Bài tập phần 4

**Bài 4.1 — Đo bằng JMH (Cơ bản)**
- Đề bài: Dựng project JMH, benchmark tính tổng bình phương của `n` số với 4 cách: for-loop, `IntStream`, `Stream<Integer>` (boxed), parallel `IntStream`; `n ∈ {1_000, 100_000, 10_000_000}`.
- Tiêu chí đạt: bảng kết quả kèm nhận xét; dùng `Blackhole` hoặc return giá trị để tránh dead-code elimination.

**Bài 4.2 — Tái hiện common pool starvation (Trung bình)**
- Đề bài: Viết chương trình: thread A chạy parallel stream 100 phần tử, mỗi phần tử `Thread.sleep(500)`; đồng thời thread B mỗi 100ms gọi `CompletableFuture.supplyAsync(() -> System.nanoTime())` và đo thời gian tới khi hoàn thành.
- Tiêu chí đạt: in ra độ trễ của B tăng đột biến khi A chạy; sửa bằng executor riêng cho B và chứng minh độ trễ trở lại bình thường.

**Bài 4.3 — Phân tích split (Nâng cao)**
- Đề bài: So sánh thời gian parallel sum trên `ArrayList<Integer>`, `LinkedList<Integer>`, `Stream.iterate(...).limit(n)`, `IntStream.range` với n = 5 triệu. Giải thích kết quả dựa trên `Spliterator.trySplit` của từng source.
- Tiêu chí đạt: giải thích được vì sao `Stream.iterate` gần như không song song hoá được (phần tử sau phụ thuộc phần tử trước, spliterator chỉ split theo batch tuần tự).

<details>
<summary>Gợi ý lời giải</summary>

4.2:
```java
new Thread(() -> IntStream.range(0, 100).parallel().forEach(i -> sleep(500))).start();
ExecutorService own = Executors.newFixedThreadPool(2);
for (int i = 0; i < 30; i++) {
    long t0 = System.nanoTime();
    CompletableFuture.supplyAsync(System::nanoTime /*, own */).join();
    System.out.printf("latency %d ms%n", (System.nanoTime() - t0) / 1_000_000);
    sleep(100);
}
```
Không có `own`: latency ~ vài trăm ms (chờ worker rảnh). Có `own`: < 1ms.

4.3: `ArrayList`/`range` split chia đôi theo index O(1), cân bằng. `LinkedList` spliterator phải duyệt để tách batch (kích thước tăng dần 1024, 2048...) → phần lớn công việc tuần tự. `iterate` tương tự, lại còn boxed.
</details>

---

<a id="p5"></a>
## 5. Optional — dùng đúng và anti-pattern

### 5.1 Mục đích thiết kế

`Optional<T>` (Java 8) được thiết kế với mục đích hẹp: **làm kiểu trả về cho method có thể "không có kết quả"**, buộc caller phải xử lý trường hợp rỗng thay vì quên check null. Brian Goetz (kiến trúc sư Java) đã nói rõ: Optional không nhằm thay thế mọi null.

API theo phiên bản:
- Java 8: `of`, `ofNullable`, `empty`, `isPresent`, `ifPresent`, `map`, `flatMap`, `filter`, `orElse`, `orElseGet`, `orElseThrow(Supplier)`, `get`
- Java 9: `ifPresentOrElse`, `or(Supplier<Optional>)`, `stream()`
- Java 10: `orElseThrow()` không tham số (thay thế `get()` — tên rõ ý hơn)
- Java 11: `isEmpty()`

```java
import java.util.*;

public class OptionalDemo {
    record Address(String city) {}
    record User(String name, Address address) {}

    static Map<Long, User> db = Map.of(1L, new User("An", new Address("Hà Nội")),
                                       2L, new User("Bình", null));

    static Optional<User> findUser(long id) { return Optional.ofNullable(db.get(id)); }

    public static void main(String[] args) {
        // Chuỗi map an toàn thay cho if-null lồng nhau
        String city = findUser(2L)
            .map(User::address)          // map trả về Optional.empty nếu address null
            .map(Address::city)
            .orElse("Không rõ");
        System.out.println(city); // Không rõ

        // orElse vs orElseGet: orElse LUÔN evaluate tham số
        findUser(1L).orElse(expensiveDefault());     // expensiveDefault() vẫn chạy!
        findUser(1L).orElseGet(() -> expensiveDefault()); // chỉ chạy khi rỗng

        // Ném exception nghiệp vụ
        User u = findUser(1L).orElseThrow(() -> new NoSuchElementException("User 1 not found"));

        // stream() (9): lọc các Optional rỗng trong một stream
        List<User> found = List.of(1L, 2L, 3L).stream()
            .map(OptionalDemo::findUser)
            .flatMap(Optional::stream)
            .toList();
        System.out.println(found.size()); // 2
    }

    static User expensiveDefault() { System.out.println("tính default tốn kém"); return null; }
}
```

### 5.2 Anti-pattern (và lý do)

| Anti-pattern | Vì sao tệ | Thay bằng |
|---|---|---|
| `if (opt.isPresent()) return opt.get();` | chỉ là null-check dài hơn | `map`/`orElse`/`orElseThrow` |
| `Optional` làm **field** | không `Serializable`; tốn thêm object; JPA/Jackson xử lý lằng nhằng | field nullable + getter trả `Optional` |
| `Optional` làm **tham số method** | caller phải bọc; vẫn có thể truyền `null` cho chính Optional | overload hoặc tham số nullable có doc |
| `Optional<List<T>>` | hai cách biểu diễn "không có gì" | trả `List.of()` rỗng (Effective Java Item 54) |
| `Optional.of(x)` khi x có thể null | NPE ngay lập tức | `Optional.ofNullable(x)` |
| Trả `null` từ method có kiểu `Optional` | phá hoàn toàn hợp đồng | luôn `Optional.empty()` |
| `orElse(createNewEntity())` | side-effect/chi phí chạy cả khi có giá trị | `orElseGet` |
| `Optional<Integer>` trong hot path | boxing + object | `OptionalInt` hoặc giá trị sentinel |
| Dùng Optional làm key của Map/phần tử Set, đồng bộ trên Optional | Optional là **value-based class**, identity không ổn định | dùng giá trị bên trong |

### 5.3 Bên dưới nắp capo

`Optional` là class `final` chứa một field `value`; `Optional.empty()` là singleton. Mỗi `Optional.of` là một allocation — thường bị escape analysis loại bỏ nếu chỉ dùng cục bộ, nhưng **không đảm bảo**. Optional là "value-based class": trong tương lai (Project Valhalla) có thể trở thành value class không có identity, nên code dựa vào `==`, `synchronized`, `identityHashCode` trên Optional sẽ hỏng.

> 💡 **Góc nhìn Senior:**
> - Trong Spring Data, `findById` trả `Optional<T>` — đúng mục đích thiết kế. Repository custom nên theo cùng convention.
> - Ở biên hệ thống (REST DTO, JPA entity), hạn chế Optional. Trong domain logic, Optional giúp mã hoá rõ "có thể không có".
> - Khi phỏng vấn được hỏi "Optional có giải quyết NPE không?" — trả lời: nó giúp **thiết kế API** rõ ràng hơn ở kiểu trả về, không phải là cơ chế loại bỏ null; kết hợp với annotation `@Nullable`/`@NonNull` (JSpecify) và static analysis (NullAway, Error Prone) mới hiệu quả ở quy mô lớn.

> ⚠️ **Lỗi thường gặp:** gọi `get()` không check (gây `NoSuchElementException` — chẳng khác NPE); `map` với hàm trả về `Optional` → được `Optional<Optional<T>>` (phải dùng `flatMap`); dùng `ifPresent` rồi cố trả giá trị từ trong lambda.

### 🛠 Bài tập phần 5

**Bài 5.1 — Refactor null-check (Cơ bản)**
- Đề bài: Refactor đoạn code sau thành chuỗi Optional, không dùng `isPresent`/`get`:
  ```java
  String zip = null;
  if (order != null && order.getCustomer() != null && order.getCustomer().getAddress() != null)
      zip = order.getCustomer().getAddress().getZip();
  if (zip == null) zip = "00000";
  ```
- Tiêu chí đạt: một biểu thức duy nhất; test với từng mắt xích null.

**Bài 5.2 — Đánh giá API (Trung bình)**
- Đề bài: Cho interface có các method: `Optional<List<Order>> findOrders(Optional<String> status)`, `void setDiscount(Optional<BigDecimal> d)`, field `private Optional<String> note;`. Viết lại theo best practice và giải thích từng thay đổi bằng 1–2 câu.
- Tiêu chí đạt: nêu đúng lý do serialization, collection rỗng, overload.

**Bài 5.3 — Chuỗi fallback (Nâng cao)**
- Đề bài: Viết `Optional<Config> resolveConfig(String key)` tìm theo thứ tự: biến môi trường → system property → file `application.properties` → giá trị mặc định trong code (chỉ dùng nếu key nằm trong danh sách cho phép). Mỗi nguồn chỉ được truy cập khi nguồn trước rỗng.
- Tiêu chí đạt: dùng `Optional.or` (Java 9); chứng minh bằng log/mock rằng nguồn sau không bị gọi khi nguồn trước có giá trị.

<details>
<summary>Gợi ý lời giải</summary>

```java
// 5.1
String zip = Optional.ofNullable(order)
    .map(Order::getCustomer).map(Customer::getAddress).map(Address::getZip)
    .orElse("00000");

// 5.2
List<Order> findOrders(String status);            // null/không truyền = tất cả; hoặc overload findOrders()
void setDiscount(BigDecimal d);                    // + clearDiscount()
private String note; public Optional<String> getNote() { return Optional.ofNullable(note); }

// 5.3
return fromEnv(key)
    .or(() -> fromSysProp(key))
    .or(() -> fromFile(key))
    .or(() -> ALLOWED_DEFAULTS.contains(key) ? Optional.of(defaultOf(key)) : Optional.empty());
```
</details>

---

<a id="p6"></a>
## 6. java.time — thời gian, time zone, DST

### 6.1 Vì sao `java.util.Date`/`Calendar` bị thay thế

- `Date` **mutable** (có `setTime`) → không an toàn khi chia sẻ, cần defensive copy.
- `SimpleDateFormat` **không thread-safe** → bug kinh điển: khai báo `static final SimpleDateFormat` dùng chung trong web app → ngày tháng bị trộn lẫn giữa các request, thậm chí `NumberFormatException` ngẫu nhiên.
- Tháng bắt đầu từ 0, năm tính từ 1900, `Date` thực chất là một instant nhưng `toString()` lại in theo time zone mặc định.

`java.time` (JSR-310, Java 8, dựa trên Joda-Time): **immutable, thread-safe**, tách bạch rõ các khái niệm.

### 6.2 Bản đồ các kiểu

| Kiểu | Biểu diễn | Ví dụ | Dùng khi |
|---|---|---|---|
| `Instant` | một điểm trên trục thời gian UTC (giây + nano từ epoch) | `2024-03-31T00:30:00Z` | timestamp sự kiện, log, lưu DB, so sánh thời điểm |
| `LocalDate` | ngày, không time zone | `2024-03-31` | ngày sinh, ngày lễ |
| `LocalTime` | giờ trong ngày | `08:30` | giờ mở cửa |
| `LocalDateTime` | ngày + giờ, **không** time zone | `2024-03-31T02:30` | lịch "8h sáng ở bất cứ đâu" — **không** phải một thời điểm cụ thể |
| `ZonedDateTime` | `LocalDateTime` + `ZoneId` (có luật DST) | `2024-03-31T03:30+02:00[Europe/Berlin]` | hiển thị, tính toán theo lịch của một vùng |
| `OffsetDateTime` | `LocalDateTime` + offset cố định | `2024-03-31T03:30+02:00` | trao đổi qua API/DB (ISO-8601), không cần luật DST |
| `Duration` | khoảng thời gian tính bằng giây/nano | `PT36H` | timeout, đo thời gian |
| `Period` | khoảng theo lịch: năm/tháng/ngày | `P1M` | "thêm 1 tháng" |
| `ZoneId` vs `ZoneOffset` | vùng có luật (`Asia/Ho_Chi_Minh`) vs độ lệch cố định (`+07:00`) | | |

```java
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class TimeBasics {
    public static void main(String[] args) {
        Instant now = Instant.now();
        ZonedDateTime hcm = now.atZone(ZoneId.of("Asia/Ho_Chi_Minh"));
        ZonedDateTime ny  = hcm.withZoneSameInstant(ZoneId.of("America/New_York")); // cùng thời điểm
        ZonedDateTime ny2 = hcm.withZoneSameLocal(ZoneId.of("America/New_York"));   // cùng giờ đồng hồ, KHÁC thời điểm
        System.out.println(hcm + " | " + ny + " | " + ny2);

        LocalDate d = LocalDate.of(2024, 1, 31);
        System.out.println(d.plusMonths(1));            // 2024-02-29 (kẹp về cuối tháng, không ném lỗi)
        System.out.println(ChronoUnit.DAYS.between(LocalDate.of(2024,1,1), LocalDate.of(2025,1,1))); // 366

        DateTimeFormatter f = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"); // thread-safe → static final OK
        System.out.println(LocalDateTime.of(2024, 5, 1, 9, 0).format(f));      // 01/05/2024 09:00
        // ⚠️ "YYYY" là week-based-year: 2024-12-30 format "YYYY" ra 2025!
    }
}
```

### 6.3 DST — Daylight Saving Time: gap và overlap

Việt Nam không có DST, nhưng hệ thống phục vụ khách hàng quốc tế (EU, US, Úc) **chắc chắn** gặp:

- **Gap (spring forward)**: ở `Europe/Berlin`, ngày 2024-03-31 lúc 02:00 đồng hồ nhảy lên 03:00 → giờ địa phương 02:30 **không tồn tại**. `ZonedDateTime.of(2024-03-31T02:30, Berlin)` sẽ **dời tới** 03:30+02:00 (dời theo độ dài gap).
- **Overlap (fall back)**: 2024-10-27 lúc 03:00 lùi về 02:00 → 02:30 xảy ra **hai lần**. Java mặc định chọn offset **sớm hơn** (+02:00); dùng `withLaterOffsetAtOverlap()` để chọn lần thứ hai.

```java
ZoneId berlin = ZoneId.of("Europe/Berlin");
ZonedDateTime gap = ZonedDateTime.of(LocalDateTime.of(2024, 3, 31, 2, 30), berlin);
System.out.println(gap); // 2024-03-31T03:30+02:00[Europe/Berlin]

ZonedDateTime before = ZonedDateTime.of(LocalDateTime.of(2024, 3, 30, 12, 0), berlin);
System.out.println(before.plusDays(1));             // 2024-03-31T12:00+02:00 — cùng giờ đồng hồ (23h thực)
System.out.println(before.plus(Duration.ofDays(1)));// 2024-03-31T13:00+02:00 — đúng 24h thực

ZonedDateTime overlap = ZonedDateTime.of(LocalDateTime.of(2024, 10, 27, 2, 30), berlin);
System.out.println(overlap);                              // ...02:30+02:00
System.out.println(overlap.withLaterOffsetAtOverlap());   // ...02:30+01:00
```

Hệ quả production:
- **Cron job lúc 02:30 giờ địa phương** ở vùng có DST: ngày gap có thể không chạy, ngày overlap chạy **hai lần** (tuỳ scheduler). Đặt lịch theo UTC hoặc tránh khung 01:00–03:00.
- Tính "số giờ làm việc", "phí theo giờ" bằng `LocalDateTime` → sai 1 giờ vào ngày chuyển DST. Phải tính trên `Instant`/`ZonedDateTime`.
- Luật time zone **thay đổi theo chính trị** (một quốc gia bỏ DST). JDK mang theo tzdata; cần cập nhật JDK (hoặc dùng TZUpdater) — đây là lý do lưu `Instant` + `ZoneId` riêng thay vì chỉ lưu offset cho sự kiện tương lai.

### 6.4 Lưu trữ và truyền tải

- **Nguyên tắc vàng**: lưu thời điểm dưới dạng UTC (`Instant`/`TIMESTAMP WITH TIME ZONE`/`timestamptz`), chuyển sang time zone người dùng ở tầng hiển thị.
- Với **sự kiện tương lai theo lịch địa phương** (cuộc họp 9h sáng giờ Berlin tháng sau) → lưu `LocalDateTime` + `ZoneId`, tính ra Instant lúc cần (vì luật DST có thể đổi).
- JDBC 4.2 hỗ trợ trực tiếp `LocalDate`, `LocalDateTime`, `OffsetDateTime` qua `setObject/getObject`. Không phải driver nào cũng hỗ trợ `ZonedDateTime`/`Instant` trực tiếp.
- JVM time zone mặc định (`TimeZone.getDefault()`, `-Duser.timezone`) phụ thuộc máy/container. Container Docker mặc định thường là UTC, máy dev ở Việt Nam là +07 → bug "chạy local đúng, lên server lệch 7 tiếng". Đặt rõ `-Duser.timezone=UTC` và đừng dùng `LocalDateTime.now()` cho timestamp nghiệp vụ.
- Jackson: cần module `jackson-datatype-jsr310` (Spring Boot tự cấu hình) và tắt `WRITE_DATES_AS_TIMESTAMPS` để ra ISO-8601.

### 6.5 `Clock` — để code có thể test

Code gọi `Instant.now()`/`LocalDate.now()` trực tiếp là code **không thể test deterministic** (test "hết hạn sau 30 ngày" phải đợi 30 ngày?). Inject `java.time.Clock`:

```java
import java.time.*;

public class SubscriptionService {
    private final Clock clock;
    public SubscriptionService(Clock clock) { this.clock = clock; }

    public boolean isExpired(Instant startedAt, Duration validity) {
        return Instant.now(clock).isAfter(startedAt.plus(validity));
    }

    public static void main(String[] args) {
        Instant start = Instant.parse("2024-01-01T00:00:00Z");
        Clock fixed = Clock.fixed(Instant.parse("2024-02-01T00:00:00Z"), ZoneOffset.UTC);
        var svc = new SubscriptionService(fixed);
        System.out.println(svc.isExpired(start, Duration.ofDays(30))); // true — deterministic

        Clock later = Clock.offset(fixed, Duration.ofDays(-5)); // "lùi đồng hồ"
        System.out.println(new SubscriptionService(later).isExpired(start, Duration.ofDays(30))); // false
    }
}
```
Trong Spring: khai báo `@Bean Clock clock() { return Clock.systemUTC(); }` và inject; trong test override bằng `Clock.fixed`.

> 💡 **Góc nhìn Senior:**
> - `System.currentTimeMillis()`/`Instant.now()` là **wall clock** — có thể nhảy lùi (NTP sync). Đo **khoảng thời gian** (latency, timeout) phải dùng `System.nanoTime()` (monotonic).
> - `Instant.now()` có độ phân giải micro-giây trên Linux từ Java 9+, nhưng Postgres `timestamp` lưu micro-giây, Oracle `TIMESTAMP(6)` cũng vậy, MySQL `DATETIME` mặc định chỉ giây → round-trip làm mất precision → so sánh `equals` sau khi đọc lại từ DB thất bại trong test. Truncate (`truncatedTo(ChronoUnit.MICROS)`) trước khi lưu.
> - Tiền lãi theo ngày, kỳ thanh toán theo tháng → dùng `Period`/`LocalDate`; SLA "phản hồi trong 4 giờ" → `Duration`/`Instant`.

> ⚠️ **Lỗi thường gặp:** dùng `LocalDateTime` làm timestamp sự kiện; pattern `YYYY` thay vì `yyyy`, `hh` (12h) thay vì `HH`, `mm` (phút) nhầm `MM` (tháng); dùng `ZoneId.of("+07:00")` (offset cố định) thay vì `Asia/Ho_Chi_Minh` cho vùng có DST; quên `Locale` khi format tên tháng.

### 🛠 Bài tập phần 6

**Bài 6.1 — Tính tuổi & ngày làm việc (Cơ bản)**
- Đề bài: Viết `int age(LocalDate birth, Clock clock)` và `long businessDaysBetween(LocalDate from, LocalDate to, Set<LocalDate> holidays)`.
- Tiêu chí đạt: test sinh ngày 29/02; test bằng `Clock.fixed`; không dùng `Date`/`Calendar`.

**Bài 6.2 — Scheduler an toàn DST (Trung bình)**
- Đề bài: Viết `List<Instant> nextRuns(LocalTime at, ZoneId zone, LocalDate fromDate, int days)` trả về các thời điểm chạy job "hàng ngày lúc `at` giờ địa phương". Quy tắc: nếu giờ rơi vào gap → chạy ở thời điểm sau gap; nếu overlap → chỉ chạy **một lần** (lần đầu).
- Tiêu chí đạt: test với `Europe/Berlin`, `at = 02:30`, khoảng ngày bao gồm 31/03/2024 và 27/10/2024; không có ngày nào chạy 2 lần hoặc bị bỏ.

**Bài 6.3 — Đồng hồ trong hệ phân tán (Nâng cao)**
- Đề bài: Một service tính "đơn hàng quá hạn thanh toán 15 phút" chạy trên 3 node, so sánh `createdAt` (từ DB, do node khác ghi) với `Instant.now()`. Phân tích các nguồn sai lệch (clock skew giữa node, NTP nhảy, precision DB) và đề xuất thiết kế. Viết code dùng thời gian của DB (`SELECT now()`/`CURRENT_TIMESTAMP`) làm nguồn duy nhất.
- Tiêu chí đạt: viết 1 trang phân tích + code; nêu được khi nào dùng `nanoTime` và khi nào không dùng được (không so sánh giữa các JVM).

<details>
<summary>Gợi ý lời giải</summary>

```java
// 6.1
static int age(LocalDate birth, Clock clock) { return Period.between(birth, LocalDate.now(clock)).getYears(); }
static long businessDaysBetween(LocalDate from, LocalDate to, Set<LocalDate> holidays) {
    return from.datesUntil(to)                                    // Java 9
        .filter(d -> d.getDayOfWeek() != DayOfWeek.SATURDAY && d.getDayOfWeek() != DayOfWeek.SUNDAY)
        .filter(d -> !holidays.contains(d)).count();
}

// 6.2
static List<Instant> nextRuns(LocalTime at, ZoneId zone, LocalDate from, int days) {
    return from.datesUntil(from.plusDays(days))
        .map(d -> ZonedDateTime.of(d, at, zone))   // gap → tự dời; overlap → offset sớm hơn (lần đầu)
        .map(ZonedDateTime::toInstant).toList();
}
```
Vì mỗi ngày chỉ sinh đúng một `ZonedDateTime`, overlap không bao giờ chạy hai lần — lỗi "chạy hai lần" thường đến từ scheduler kiểu "mỗi phút kiểm tra giờ địa phương == 02:30".

6.3: dùng một nguồn thời gian (DB) cho cả ghi và so sánh: `WHERE status='PENDING' AND created_at < now() - interval '15 minutes'`. Clock skew giữa node có thể vài trăm ms tới vài giây nếu NTP lỗi.
</details>

---

<a id="p7"></a>
## 7. Cú pháp mới: var, switch expression, text block

### 7.1 `var` — local variable type inference (Java 10, JEP 286)

`var` **không** phải dynamic typing: kiểu được compiler suy ra lúc compile và cố định. `var` là "reserved type name", không phải keyword (vẫn đặt được tên biến là `var`, nhưng không đặt tên class là `var`).

Dùng được: biến local có initializer, biến trong for/for-each, try-with-resources, tham số lambda (Java 11 — để gắn annotation: `(@NonNull var x) -> ...`).
Không dùng được: field, tham số method, kiểu trả về, biến không có initializer, `var x = null;`, array initializer `var a = {1,2};`, lambda `var f = () -> 1;` (không có target type).

```java
var map = new HashMap<String, List<Integer>>();  // ✅ tránh lặp kiểu dài
var list = new ArrayList<>();                     // ⚠️ suy ra ArrayList<Object>!
for (var e : map.entrySet()) { /* e là Map.Entry<String, List<Integer>> */ }
var n = 10;            // int
var l = 10L;           // long
var x = getSomething(); // ❌ người đọc không biết kiểu → giảm khả năng đọc
```

> 💡 **Góc nhìn Senior:** quy ước team nên rõ: dùng `var` khi kiểu hiển nhiên từ vế phải (`new`, factory có tên rõ) hoặc kiểu quá dài (generic lồng); tránh khi vế phải là method call mơ hồ. `var` suy ra kiểu **cụ thể** (`ArrayList`) chứ không phải interface (`List`) — thường không sao với biến local. Có thể dùng `var` để giữ kiểu của anonymous class / intersection type — trick hiếm dùng.

### 7.2 Switch expression (Java 14, JEP 361)

```java
enum Day { MON, TUE, WED, THU, FRI, SAT, SUN }

static int workingHours(Day d) {
    return switch (d) {                 // switch là EXPRESSION → trả giá trị
        case MON, TUE, WED, THU -> 8;   // nhiều label, arrow: không fall-through
        case FRI -> {
            int base = 8;
            yield base - 2;             // block cần 'yield' để trả giá trị
        }
        case SAT, SUN -> 0;
        // không cần default: compiler kiểm tra exhaustive cho enum
    };
}
```

Điểm quan trọng:
- Arrow form (`->`) **không fall-through**, không cần `break`.
- Switch expression phải **exhaustive**. Với enum đủ case, compiler tự chèn một nhánh default ẩn ném `MatchException` (Java 21; trước đó `IncompatibleClassChangeError`) nếu runtime xuất hiện hằng enum mới (class enum được compile lại riêng) — an toàn hơn `default` tự viết vì khi thêm hằng enum mới, compile sẽ **báo lỗi** chỗ thiếu.
- Có thể dùng arrow cả trong switch statement (không trả giá trị).
- `yield` thay `break value` (cú pháp preview cũ).
- Switch cổ điển trên `String` dùng `hashCode()` + `equals()`; switch trên `null` ném NPE (trừ khi có `case null` từ Java 21).

### 7.3 Text block (Java 15, JEP 378)

```java
String json = """
    {
      "name": "An",
      "role": "SENIOR"
    }
    """;   // vị trí dấu """ đóng quyết định lượng thụt lề bị loại bỏ

String sql = """
    SELECT id, name \
    FROM users \
    WHERE status = 'ACTIVE'\
    """;   // '\' ở cuối dòng: nối dòng, không thêm \n

String padded = """
    trailing\s
    """;   // '\s' giữ một khoảng trắng (trailing space bình thường bị strip)

String q = """
    SELECT * FROM t WHERE name = '%s'
    """.formatted("an");   // ⚠️ chỉ minh hoạ — KHÔNG ghép SQL bằng format (SQL injection)!
```

Quy tắc: nội dung bắt đầu từ dòng **sau** `"""` mở; "incidental whitespace" (lề chung nhỏ nhất của các dòng, kể cả dòng chứa `"""` đóng) bị loại bỏ; line terminator được chuẩn hoá thành `\n` bất kể OS; trailing whitespace mỗi dòng bị strip.

> ⚠️ **Lỗi thường gặp:** đặt nội dung ngay sau `"""` mở cùng dòng (lỗi compile); quên rằng text block kết thúc bằng `\n` nếu `"""` đóng nằm ở dòng riêng; dùng text block + `formatted` để ghép SQL/JSON từ input người dùng — vẫn là injection.

### 🛠 Bài tập phần 7

**Bài 7.1 — Refactor switch (Cơ bản)**
- Đề bài: Chuyển một switch statement cổ điển 7 case có fall-through (tính phí ship theo vùng) thành switch expression. Thêm một hằng enum mới và quan sát compiler báo lỗi.
- Tiêu chí đạt: không có `default`; giải thích vì sao bỏ `default` lại an toàn hơn.

**Bài 7.2 — Bẫy `var` (Trung bình)**
- Đề bài: Viết 6 dòng dùng `var` mà bạn nghĩ sẽ compile và 6 dòng sẽ không compile; kiểm chứng. Trong đó có: `var` với diamond, `var` với ternary `var x = flag ? 1 : "a";` (suy ra kiểu gì?), `var` trong lambda có annotation.
- Tiêu chí đạt: giải thích kiểu suy ra của ternary (intersection type `Serializable & Comparable<...>`...).

**Bài 7.3 — Template an toàn (Nâng cao)**
- Đề bài: Viết hàm render email HTML từ text block với placeholder `{{name}}`, escape HTML các giá trị (`<`, `>`, `&`, `"`, `'`). So sánh với việc dùng `formatted()` trực tiếp.
- Tiêu chí đạt: test với input `<script>alert(1)</script>`; giải thích vì sao String Templates (JEP 430, preview Java 21–22) bị **rút lại** ở Java 23 và bài học về thiết kế API an toàn.

<details>
<summary>Gợi ý lời giải</summary>

7.2: `var x = flag ? 1 : "a";` → kiểu là intersection `Serializable & Comparable<? extends ...> & Constable & ConstantDesc` (lub của Integer và String) — compile được, nhưng chỉ gọi được method chung. `var f = (@Deprecated var a) -> a;` lỗi vì lambda không có target type; `Function<String,String> f = (@Deprecated var a) -> a;` thì được.

7.3: render bằng `Pattern.compile("\\{\\{(\\w+)}}").matcher(tpl).replaceAll(m -> Matcher.quoteReplacement(escape(vars.get(m.group(1)))))`. String Templates bị rút lại vì cộng đồng phản hồi về cú pháp `STR."..."` và mô hình processor; nhóm Amber muốn thiết kế lại — điểm mấu chốt: cơ chế template nên buộc qua một "processor" có khả năng validate/escape thay vì nối chuỗi.
</details>

---

<a id="p8"></a>
## 8. Records & sealed classes — mô hình dữ liệu hiện đại

### 8.1 Records (Java 16, JEP 395)

Record là **class mang dữ liệu bất biến (nominal tuple)**. Khai báo `record Point(int x, int y) {}` compiler sinh ra:
- class `final` kế thừa `java.lang.Record` (không extends class khác được, nhưng implement interface được)
- field `private final` cho mỗi component
- **canonical constructor** `Point(int x, int y)`
- accessor `x()`, `y()` (không phải `getX()`)
- `equals`, `hashCode`, `toString` dựa trên tất cả component

Ràng buộc: không khai báo được instance field ngoài component (static field thì được), không có instance initializer; có thể thêm method, static factory, nested type, constructor phụ (phải gọi `this(...)` tới canonical).

```java
import java.util.*;

public record Money(String currency, long amountMinor) implements Comparable<Money> {
    // Compact constructor: không khai báo tham số, chạy TRƯỚC khi field được gán.
    // Dùng để validate / chuẩn hoá. Gán lại tham số → giá trị mới sẽ được dùng để gán field.
    public Money {
        Objects.requireNonNull(currency, "currency");
        currency = currency.toUpperCase(Locale.ROOT);
        if (amountMinor < 0) throw new IllegalArgumentException("amount < 0");
    }
    public static Money vnd(long v) { return new Money("VND", v); }
    public Money plus(Money o) {
        if (!currency.equals(o.currency)) throw new IllegalArgumentException("currency mismatch");
        return new Money(currency, Math.addExact(amountMinor, o.amountMinor)); // "wither" trả object mới
    }
    @Override public int compareTo(Money o) { return Long.compare(amountMinor, o.amountMinor); }
}
```

**Giới hạn của "immutability"** — record chỉ **bất biến nông (shallow)**:

```java
record Team(String name, List<String> members) {}

var list = new ArrayList<>(List.of("An"));
var team = new Team("core", list);
list.add("Bình");                          // ❌ team.members() cũng thay đổi!
team.members().add("Chi");                 // ❌ sửa được từ bên ngoài

// ✅ Defensive copy trong compact constructor
record SafeTeam(String name, List<String> members) {
    SafeTeam { members = List.copyOf(members); }   // unmodifiable + copy; NPE nếu có null phần tử
}
```

Với array component: `equals` của record so sánh array bằng **reference** (`Objects.equals` trên array), không so nội dung → hai record có array cùng nội dung vẫn `!equals`. Tránh array trong record, hoặc override `equals/hashCode` (và defensive copy trong accessor).

**Records trong hệ sinh thái**:
- **DTO, value object, kết quả trả về nhiều giá trị, key của Map** (equals/hashCode chuẩn) — rất phù hợp.
- **JPA entity — không phù hợp**: entity cần no-arg constructor, mutable, không final (Hibernate tạo proxy để lazy load). Nhưng record dùng tốt cho **projection** (`SELECT new com.x.Dto(...)` hoặc Spring Data interface/class projection) và `@Embeddable` (Hibernate 6.2+ hỗ trợ record embeddable).
- **Jackson** hỗ trợ record từ 2.12. **Serialization Java**: record deserialize **qua canonical constructor** → validation trong compact constructor được thực thi (an toàn hơn class thường, nơi deserialization bỏ qua constructor).
- Local record (khai báo trong method) rất tiện cho kết quả trung gian của stream. Local record, enum, interface lồng đều ngầm là `static` → không capture biến local hay `this`.

### 8.2 Sealed classes (Java 17, JEP 409)

Sealed class/interface **giới hạn tập subclass được phép** — một dạng "algebraic data type" (sum type).

```java
public sealed interface Shape permits Circle, Rectangle, Square {}
public record Circle(double r) implements Shape {}               // record ngầm final
public record Rectangle(double w, double h) implements Shape {}
public non-sealed class Square implements Shape {                // mở lại cho kế thừa tự do
    final double side; public Square(double s) { side = s; }
}
```

Luật:
- Mỗi subclass được permit **phải** khai báo một trong: `final`, `sealed`, hoặc `non-sealed`.
- Subclass phải nằm **cùng module** (nếu là named module) hoặc **cùng package** (nếu ở unnamed module — trường hợp classpath thông thường).
- Nếu tất cả subclass ở cùng file, có thể bỏ `permits` (compiler tự suy).
- Reflection: `Class.isSealed()`, `getPermittedSubclasses()`.

Lợi ích chính: compiler biết **tập đóng** các kiểu → switch pattern matching (phần 9) kiểm tra **exhaustiveness** mà không cần `default`. Thêm một subtype mới → mọi switch chưa xử lý sẽ **lỗi compile** → refactor an toàn. Đây là cách mô hình hoá domain kiểu `PaymentResult = Success | Declined | Pending` một cách type-safe.

> 💡 **Góc nhìn Senior:**
> - Sealed + record + pattern matching = "data-oriented programming" (bài viết cùng tên của Brian Goetz). Phù hợp cho dữ liệu có tập biến thể cố định (kết quả, sự kiện, AST, command). Ngược lại, khi tập biến thể **mở** cho plugin bên ngoài → giữ polymorphism truyền thống (method ảo).
> - So sánh với enum: enum = tập **instance** cố định (singleton mỗi hằng); sealed = tập **kiểu** cố định, mỗi kiểu có thể có nhiều instance mang dữ liệu khác nhau.
> - Thay đổi tập `permits` trong thư viện public là **breaking change** cho client đang switch exhaustive.

> ⚠️ **Lỗi thường gặp:** coi record là "immutable hoàn toàn" khi có field `List`/`Date`/array; viết getter `getX()` cho record theo thói quen JavaBeans (một số thư viện cũ — EL, BeanUtils — không nhận accessor `x()`); dùng record làm JPA entity; quên rằng compact constructor không được gán `this.x = ...` (field được gán tự động sau đó).

### 🛠 Bài tập phần 8

**Bài 8.1 — Value object (Cơ bản)**
- Đề bài: Viết record `Email(String value)` validate định dạng và chuẩn hoá lowercase; record `DateRange(LocalDate start, LocalDate end)` đảm bảo `start <= end`, có method `contains(LocalDate)`, `overlaps(DateRange)`.
- Tiêu chí đạt: test validate; dùng làm key `HashMap` đúng.

**Bài 8.2 — Bất biến sâu (Trung bình)**
- Đề bài: Viết record `Invoice(String id, List<Line> lines, Map<String,String> metadata, byte[] signature)` thực sự bất biến từ góc nhìn bên ngoài, với `equals` so sánh nội dung `signature`.
- Tiêu chí đạt: test chứng minh sửa list/map/array gốc hoặc giá trị trả về từ accessor không ảnh hưởng record; `equals`/`hashCode` nhất quán.

**Bài 8.3 — Domain sealed (Nâng cao)**
- Đề bài: Mô hình hoá `PaymentResult` sealed gồm `Approved(String txId, Money amount)`, `Declined(String reason, int code)`, `RequiresAction(URI redirect)`, `Failed(Throwable cause)`. Viết service `PaymentGateway.charge(...)` trả `PaymentResult` thay vì ném exception cho các trường hợp nghiệp vụ.
- Tiêu chí đạt: giải thích trade-off "result type" vs exception; thêm variant `Pending` và liệt kê các chỗ compiler báo lỗi (sau khi làm phần 9).

<details>
<summary>Gợi ý lời giải</summary>

```java
record Invoice(String id, List<Line> lines, Map<String, String> metadata, byte[] signature) {
    Invoice {
        lines = List.copyOf(lines); metadata = Map.copyOf(metadata);
        signature = signature.clone();
    }
    @Override public byte[] signature() { return signature.clone(); }   // accessor trả bản copy
    @Override public boolean equals(Object o) {
        return o instanceof Invoice i && id.equals(i.id) && lines.equals(i.lines)
            && metadata.equals(i.metadata) && Arrays.equals(signature, i.signature);
    }
    @Override public int hashCode() {
        return Objects.hash(id, lines, metadata) * 31 + Arrays.hashCode(signature);
    }
}
```
8.3: exception phù hợp cho lỗi bất thường/không thể xử lý tại chỗ; result type phù hợp cho kết quả nghiệp vụ dự kiến (declined là chuyện bình thường) — buộc caller xử lý mọi nhánh, không tốn chi phí stack trace.
</details>

---

<a id="p9"></a>
## 9. Pattern matching (instanceof, switch, record patterns)

### 9.1 Pattern matching cho `instanceof` (Java 16, JEP 394)

```java
// Trước Java 16
if (obj instanceof String) { String s = (String) obj; System.out.println(s.length()); }

// Java 16+: type pattern, biến s chỉ có phạm vi khi pattern match (flow scoping)
if (obj instanceof String s && s.length() > 3) { System.out.println(s.length()); }
if (!(obj instanceof String s)) return;
System.out.println(s.toUpperCase());   // s vẫn trong scope vì nhánh không match đã return
```
`equals` viết gọn: `return o instanceof Point p && x == p.x && y == p.y;`

### 9.2 Pattern matching cho switch (Java 21, JEP 441) & record patterns (Java 21, JEP 440)

```java
sealed interface Shape permits Circle, Rect, Tri {}
record Circle(double r) implements Shape {}
record Rect(double w, double h) implements Shape {}
record Tri(double a, double b, double c) implements Shape {}

static double area(Shape s) {
    return switch (s) {
        case Circle c -> Math.PI * c.r() * c.r();
        case Rect(double w, double h) when w == h -> w * w;          // record pattern + guard
        case Rect(double w, double h) -> w * h;                       // deconstruction
        case Tri(var a, var b, var c) -> {                            // var trong record pattern
            double p = (a + b + c) / 2;
            yield Math.sqrt(p * (p - a) * (p - b) * (p - c));
        }
        // KHÔNG cần default: Shape sealed → compiler biết đã đủ
    };
}

static String describe(Object o) {
    return switch (o) {
        case null -> "null";                       // Java 21: xử lý null tường minh
        case Integer i when i > 100 -> "số lớn " + i;
        case Integer i -> "số " + i;
        case String s -> "chuỗi dài " + s.length();
        case int[] arr -> "mảng int " + arr.length;
        default -> "khác: " + o.getClass().getSimpleName();
    };
}
```

Quy tắc cần nắm:
- **Dominance**: case tổng quát đứng trước case cụ thể → lỗi compile (`case Integer i` trước `case Integer i when i > 100` là lỗi; `case Object o` trước `case String s` là lỗi).
- **Exhaustiveness**: switch có pattern (kể cả switch statement) phải exhaustive. Với sealed hierarchy, liệt kê đủ subtypes là đủ.
- **`null`**: switch cũ ném NPE khi selector null. Switch mới: nếu không có `case null`, vẫn ném NPE (giữ tương thích). Có thể viết `case null, default -> ...`.
- **Guard** `when` (đã thay cho `&&` của bản preview).
- **Record pattern lồng**: `case Line(Point(var x1, var y1), Point(var x2, var y2)) -> ...`.
- Record patterns cũng dùng được với `instanceof`: `if (o instanceof Point(int x, int y)) ...`.
- **Unnamed pattern `_`**: preview ở Java 21 (JEP 443), **chính thức Java 22** (JEP 456): `case Rect(var w, _) -> ...`.
- **Primitive patterns** (`case int i` trên selector primitive) là preview ở các bản sau 21 — chưa dùng trong code production LTS 21.

### 9.3 Bên dưới nắp capo

Switch trên pattern được compile thành `invokedynamic` với bootstrap `java.lang.runtime.SwitchBootstraps.typeSwitch` — trả về chỉ số case match đầu tiên; sau đó là một `tableswitch` thông thường. Nhờ vậy JVM có thể tối ưu (cache theo class) thay vì chuỗi `instanceof` tuyến tính. Nếu runtime gặp subtype không biết (class sealed bị compile lại riêng với subtype mới), switch ném **`MatchException`**.

So sánh với Visitor pattern: Visitor cho phép thêm **operation** mới mà không sửa class, nhưng boilerplate nặng (`accept`/`visit`). Sealed + switch pattern đạt cùng mục tiêu, gọn hơn, và vẫn được compiler kiểm tra đầy đủ — nên trong Java 21, Visitor cho cấu trúc đóng phần lớn có thể thay thế.

> 💡 **Góc nhìn Senior:** pattern matching không thay thế polymorphism. Quy tắc: nếu hành vi là **bản chất** của kiểu (mỗi shape tự biết cách vẽ) → method ảo. Nếu hành vi là **mối quan tâm bên ngoài** (serialize, tính thuế, render report cho nhiều kiểu) và tập kiểu đóng → switch pattern. Tránh `switch (o)` với `default` trên `Object` lan khắp codebase — đó là "instanceof chain" đội lốt.

> ⚠️ **Lỗi thường gặp:** thêm `default` vào switch trên sealed type "cho chắc" → mất lợi ích compiler báo lỗi khi thêm subtype mới; nhầm guard `when` chạy trước khi bind (guard dùng được biến pattern); dùng pattern switch ở project compile với `--release 17` (chỉ là preview ở 17).

### 🛠 Bài tập phần 9

**Bài 9.1 — Refactor instanceof chain (Cơ bản)**
- Đề bài: Viết lại method `format(Object o)` gồm chuỗi `if instanceof ... else if ...` cho `Integer, Long, Double, String, LocalDate, Collection` thành switch pattern.
- Tiêu chí đạt: có `case null`; có guard cho chuỗi rỗng; thứ tự case không vi phạm dominance.

**Bài 9.2 — Expression evaluator (Trung bình)**
- Đề bài: Mô hình hoá AST `sealed interface Expr permits Num, Add, Mul, Neg, Var` bằng records. Viết `eval(Expr, Map<String,Double> env)` và `simplify(Expr)` (vd: `Mul(Num(1), e) → e`, `Add(Num(0), e) → e`, `Neg(Neg(e)) → e`) bằng record patterns lồng nhau.
- Tiêu chí đạt: không dùng `default`; ≥ 10 test; `toString` in biểu thức dạng infix.

**Bài 9.3 — Event processing (Nâng cao)**
- Đề bài: Xây dựng `sealed interface OrderEvent` (Created, ItemAdded, ItemRemoved, Paid, Shipped, Cancelled) và hàm `OrderState apply(OrderState, OrderEvent)` (event sourcing). Dùng pattern matching trên **cặp** `(state, event)` — gợi ý: tạo `record Transition(OrderState s, OrderEvent e)` rồi switch trên nó.
- Tiêu chí đạt: chuyển trạng thái không hợp lệ (Shipped → ItemAdded) ném exception có thông điệp rõ; rebuild state từ list event bằng `reduce` hoặc loop.

<details>
<summary>Gợi ý lời giải</summary>

```java
// 9.2
sealed interface Expr permits Num, Add, Mul, Neg, Var {}
record Num(double v) implements Expr {}
record Add(Expr l, Expr r) implements Expr {}
record Mul(Expr l, Expr r) implements Expr {}
record Neg(Expr e) implements Expr {}
record Var(String name) implements Expr {}

static Expr simplify(Expr e) {
    return switch (e) {
        case Add(Num(var z), var r) when z == 0 -> simplify(r);
        case Mul(Num(var one), var r) when one == 1 -> simplify(r);
        case Mul(Num(var z), var r) when z == 0 -> new Num(0);
        case Neg(Neg(var inner)) -> simplify(inner);
        case Add(var l, var r) -> new Add(simplify(l), simplify(r));
        case Mul(var l, var r) -> new Mul(simplify(l), simplify(r));
        case Neg(var x) -> new Neg(simplify(x));
        case Num n -> n;
        case Var v -> v;
    };
}

// 9.3
return switch (new Transition(state, event)) {
    case Transition(Draft d, ItemAdded(var item)) -> d.add(item);
    case Transition(Draft d, Paid p) when !d.items().isEmpty() -> new PaidOrder(d.items(), p.amount());
    case Transition(PaidOrder po, Shipped s) -> new ShippedOrder(po, s.trackingNo());
    case Transition(var s, var ev) -> throw new IllegalStateException(ev + " không hợp lệ ở trạng thái " + s);
};
```
</details>

---

<a id="p10"></a>
## 10. Sequenced Collections, JPMS, HttpClient

### 10.1 Sequenced Collections (Java 21, JEP 431)

Trước Java 21, lấy phần tử đầu/cuối mỗi collection một kiểu: `list.get(0)` / `list.get(list.size()-1)`, `deque.getFirst()`, `sortedSet.first()`, `linkedHashSet.iterator().next()` (còn phần tử cuối thì phải duyệt hết!). Java 21 thêm 3 interface:

```
SequencedCollection<E>  : addFirst, addLast, getFirst, getLast, removeFirst, removeLast, reversed()
  ├─ List, Deque
  └─ SequencedSet<E>    : reversed() trả SequencedSet  ← LinkedHashSet, SortedSet/NavigableSet
SequencedMap<K,V>       : firstEntry, lastEntry, pollFirstEntry, pollLastEntry, putFirst, putLast,
                          reversed(), sequencedKeySet(), sequencedValues(), sequencedEntrySet()
                          ← LinkedHashMap, SortedMap/NavigableMap
```

```java
var list = new ArrayList<>(List.of(1, 2, 3));
System.out.println(list.getFirst() + " " + list.getLast());   // 1 3
System.out.println(list.reversed());                          // [3, 2, 1] — VIEW, không copy
list.reversed().set(0, 99);                                   // ghi qua view → list = [1, 2, 99]

var lhm = new LinkedHashMap<String, Integer>();
lhm.put("b", 2); lhm.put("a", 1);
lhm.putFirst("z", 0);                                         // {z=0, b=2, a=1}
System.out.println(lhm.lastEntry());                          // a=1
```

Lưu ý: `reversed()` trả **view** O(1); `addFirst` trên `ArrayList` là O(n); `List.of(...).getFirst()` OK nhưng `addFirst` ném `UnsupportedOperationException`; `SortedSet.addFirst` ném `UnsupportedOperationException` (thứ tự do comparator quyết định). Pitfall migration: class tự định nghĩa đã có method `getFirst()`/`reversed()` với kiểu trả về khác (ví dụ trong code Kotlin hoặc class kế thừa `ArrayList`) có thể xung đột khi nâng lên JDK 21.

### 10.2 JPMS — Java Platform Module System (Java 9, JEP 261)

Module = một nhóm package có tên, khai báo rõ **phụ thuộc** và **API công khai** trong `module-info.java`:

```java
module com.shop.order {
    requires java.net.http;                 // phụ thuộc module khác
    requires transitive com.shop.common;    // ai requires order cũng tự động đọc được common
    requires static lombok;                 // chỉ cần lúc compile

    exports com.shop.order.api;             // chỉ package này được truy cập từ ngoài
    exports com.shop.order.spi to com.shop.plugin;  // qualified export
    opens com.shop.order.dto to com.fasterxml.jackson.databind; // cho phép deep reflection lúc runtime

    uses com.shop.order.spi.PricingStrategy;        // ServiceLoader
    provides com.shop.order.spi.PricingStrategy with com.shop.order.internal.DefaultPricing;
}
```

Khái niệm cần phân biệt:
- **`exports`**: package truy cập được ở compile time và runtime (public type), nhưng **không** cho reflection vào private member.
- **`opens`**: cho phép deep reflection (`setAccessible(true)`) — framework như Jackson, Hibernate, Spring cần.
- **Strong encapsulation**: package không export → `public` class bên trong cũng **không** truy cập được từ module khác. "public" không còn là public toàn cục.
- **Module path vs classpath**: JAR trên classpath thuộc **unnamed module** (đọc mọi module, export mọi thứ); JAR không có `module-info` đặt trên module path thành **automatic module** (tên lấy từ `Automatic-Module-Name` trong MANIFEST hoặc tên file).
- **Split package**: cùng một package xuất hiện ở hai module → lỗi khi khởi động. Một trong những rào cản lớn khi modular hoá code cũ.
- `jlink` tạo runtime image tối giản chỉ chứa các module cần → image Docker nhỏ hơn.

**Strong encapsulation của JDK internals** — đây là thứ ảnh hưởng tới **mọi** dự án (kể cả không dùng module):
- Java 9–15: `--illegal-access=permit` mặc định → chỉ cảnh báo "WARNING: An illegal reflective access operation has occurred".
- Java 16 (JEP 396): mặc định `deny`.
- Java 17 (JEP 403): **bỏ hẳn** `--illegal-access`; truy cập reflection vào internals (`sun.misc.*` trừ `Unsafe` trong `jdk.unsupported`, `java.lang` private fields...) ném `InaccessibleObjectException`. Cách tạm: `--add-opens java.base/java.lang=ALL-UNNAMED`. Cách đúng: nâng cấp thư viện (Lombok, Mockito/ByteBuddy, Spring, Hibernate, Groovy, các agent APM).

> 💡 **Góc nhìn Senior:** thực tế đa số ứng dụng Spring Boot **không** dùng `module-info` (chạy trên classpath). Hiểu JPMS chủ yếu để: (1) đọc và xử lý lỗi `InaccessibleObjectException` khi nâng cấp JDK; (2) dùng `jlink`/`jdeps` để giảm kích thước image; (3) thiết kế thư viện có ranh giới API rõ ràng (thêm `Automatic-Module-Name` cho thư viện của bạn là bước tối thiểu). `jdeps --jdk-internals app.jar` liệt kê chỗ dùng API nội bộ JDK — chạy trước khi migrate.

### 10.3 HttpClient (Java 11, JEP 321)

Thay thế `HttpURLConnection` cũ. Hỗ trợ HTTP/1.1 & HTTP/2, sync & async (`CompletableFuture`), WebSocket.

```java
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.concurrent.*;

public class HttpDemo {
    // HttpClient là immutable, thread-safe và giữ connection pool → TẠO MỘT LẦN, DÙNG LẠI
    private static final HttpClient CLIENT = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_2)
        .connectTimeout(Duration.ofSeconds(2))          // chỉ timeout lúc kết nối
        .followRedirects(HttpClient.Redirect.NORMAL)
        .executor(Executors.newFixedThreadPool(4))      // mặc định là cached thread pool
        .build();

    public static void main(String[] args) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create("https://httpbin.org/get"))
            .timeout(Duration.ofSeconds(5))             // timeout cho tới khi nhận response headers
            .header("Accept", "application/json")
            .GET().build();

        HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
        System.out.println(resp.statusCode() + " " + resp.body().length());

        // Async + gọi song song nhiều request
        var futures = java.util.stream.IntStream.range(0, 3)
            .mapToObj(i -> CLIENT.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                                 .thenApply(HttpResponse::statusCode)
                                 .orTimeout(6, TimeUnit.SECONDS))
            .toList();
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        futures.forEach(f -> System.out.println(f.join()));
    }
}
```

Điểm cần biết: `send` không ném exception với status 4xx/5xx — phải tự kiểm tra `statusCode()`; `BodyHandlers.ofString()` buffer toàn bộ body vào RAM (file lớn → `ofFile`/`ofInputStream`); từ Java 21 `HttpClient` implement `AutoCloseable` (`close()`, `shutdown()`); không có retry/circuit breaker sẵn → thêm Resilience4j hoặc dùng client cấp cao (Spring `RestClient`/`WebClient`).

> ⚠️ **Lỗi thường gặp:** tạo `HttpClient` mới cho mỗi request (mất connection reuse, rò rỉ thread selector); không đặt request timeout (mặc định **không có** timeout → thread treo vô hạn khi server đầu kia treo); dùng `ofString()` cho file tải về hàng trăm MB.

### 🛠 Bài tập phần 10

**Bài 10.1 — Sequenced API (Cơ bản)**
- Đề bài: Viết LRU cache dùng `LinkedHashMap` (accessOrder = true, override `removeEldestEntry`) và dùng API Java 21 (`firstEntry`, `lastEntry`, `sequencedKeySet().reversed()`) để in từ "mới dùng nhất" tới "cũ nhất".
- Tiêu chí đạt: test eviction đúng; không dùng `iterator()` để lấy phần tử đầu/cuối.

**Bài 10.2 — Modular hoá (Trung bình)**
- Đề bài: Tạo 3 module `app`, `core`, `plugin.discount` (không dùng build tool, chỉ `javac --module-source-path` và `java --module-path`). `core` định nghĩa SPI `DiscountPolicy`, `plugin.discount` provides implementation, `app` dùng `ServiceLoader`.
- Tiêu chí đạt: `app` không truy cập được class internal của `core` (chứng minh bằng lỗi compile); chạy được bằng một lệnh `java -m app/...`; tạo runtime bằng `jlink` và so sánh kích thước với JDK đầy đủ.

**Bài 10.3 — HTTP client có kỷ luật (Nâng cao)**
- Đề bài: Viết `ResilientHttp` bọc `HttpClient`: timeout kết nối/đọc, retry tối đa 3 lần với exponential backoff + jitter **chỉ** cho lỗi I/O và status 502/503/504 với method idempotent (GET, PUT, DELETE), giới hạn tối đa 50 request đồng thời (Semaphore).
- Tiêu chí đạt: test bằng WireMock hoặc `com.sun.net.httpserver.HttpServer` giả lập 503 hai lần rồi 200; không retry POST; log số lần thử.

<details>
<summary>Gợi ý lời giải</summary>

```java
// 10.1
class Lru<K, V> extends LinkedHashMap<K, V> {
    private final int cap;
    Lru(int cap) { super(16, 0.75f, true); this.cap = cap; }
    @Override protected boolean removeEldestEntry(Map.Entry<K, V> e) { return size() > cap; }
}
lru.sequencedKeySet().reversed().forEach(System.out::println);  // mới nhất → cũ nhất

// 10.2 lệnh mẫu
// javac -d out --module-source-path src $(find src -name "*.java")
// java --module-path out -m app/com.shop.app.Main
// jlink --module-path out:$JAVA_HOME/jmods --add-modules app --output rt --strip-debug --no-header-files --no-man-pages

// 10.3 backoff
long delay = (long) (base * Math.pow(2, attempt)) + ThreadLocalRandom.current().nextLong(base);
```
</details>

---

<a id="p11"></a>
## 11. Bản đồ phiên bản LTS 8 / 11 / 17 / 21 (và 25) & migration

### 11.1 Mô hình phát hành

Từ Java 9, JDK phát hành **mỗi 6 tháng** (tháng 3 và tháng 9). Cứ mỗi **2 năm** có một bản **LTS** (từ Java 17 trở đi; trước đó là 3 năm): 8 (2014), 11 (2018), 17 (2021), 21 (2023), **25 (09/2025)**. Feature mới thường qua các giai đoạn **preview** (`--enable-preview`) → final. Một câu hỏi phỏng vấn hay gặp: *"Feature X có trong Java 17 không?"* — phải phân biệt preview và final.

### 11.2 Bảng tổng hợp

| LTS | Ngôn ngữ / API chính | JVM / runtime | Bị loại bỏ / thay đổi đáng chú ý |
|---|---|---|---|
| **8** (2014) | Lambda, method reference, Stream API, `Optional`, `java.time`, default/static method trong interface, `CompletableFuture`, `StampedLock`, `LongAdder`, Nashorn | PermGen bị thay bằng **Metaspace**; `ConcurrentHashMap` viết lại (CAS + bin lock); `HashMap` treeify bucket | — |
| **11** (2018) | `var` (10), `var` trong lambda, `HttpClient`, `String.isBlank/lines/strip/repeat`, `Files.readString/writeString`, `Collectors.toUnmodifiableList` (10), `Optional.isEmpty`, `Predicate.not`, chạy file đơn `java Main.java`; từ 9: JPMS, `List.of/Set.of/Map.of`, JShell, `Stream.takeWhile`, `Optional.or/stream`, `ProcessHandle`, `VarHandle`, `Flow` (reactive streams) | **G1 mặc định** (9), container awareness (10, backport 8u191), ZGC thử nghiệm (11), Epsilon GC, Flight Recorder mã nguồn mở, TLS 1.3, compact strings (9) | **JEP 320: xoá Java EE & CORBA** khỏi JDK: `javax.xml.bind` (JAXB), `javax.xml.ws` (JAX-WS), `javax.activation`, `javax.annotation` (`@PostConstruct`...), `javax.transaction`; Nashorn bị deprecate; Java Web Start/applet bị bỏ khỏi Oracle JDK |
| **17** (2021) | Switch expression (14), text block (15), records (16), pattern matching `instanceof` (16), **sealed classes** (17), `Stream.toList()` (16), `Stream.mapMulti` (16), helpful NullPointerException (14), `RandomGenerator` API (17) | ZGC & Shenandoah production-ready (15), **strong encapsulation JDK internals** (16 deny mặc định, 17 bỏ `--illegal-access`), Elastic Metaspace, macOS/AArch64 | Nashorn bị **xoá** (15); experimental AOT/Graal JIT bị xoá (17); RMI Activation xoá (17); Security Manager **deprecated for removal** (17, JEP 411); Applet API deprecated for removal; biased locking bị tắt mặc định/deprecated (15) |
| **21** (2023) | **Virtual threads** (JEP 444), **pattern matching cho switch** (441), **record patterns** (440), **Sequenced Collections** (431); preview: structured concurrency, scoped values, unnamed patterns, string templates (sau đó bị rút), unnamed classes & instance main | **Generational ZGC** (JEP 439, bật bằng `-XX:+ZGenerational`), Key Encapsulation Mechanism API | Từ 18: **UTF-8 mặc định** (JEP 400) — ảnh hưởng code đọc file không chỉ định charset trên Windows; finalization deprecated for removal (18, JEP 421); cảnh báo khi load agent động (JEP 451); `Thread.stop/suspend/resume` ném `UnsupportedOperationException` (20) |
| **25** (2025) | Scoped Values **final** (JEP 506), module import declarations, compact source files & instance main methods, flexible constructor bodies (code trước `super(...)`), Stream Gatherers (final ở 24), structured concurrency vẫn **preview** | Compact object headers (JEP 519, product feature, chưa mặc định), generational Shenandoah; từ 24: virtual thread **không còn bị pin** khi gặp `synchronized` (JEP 491); ZGC non-generational bị xoá (24) | Security Manager bị vô hiệu hoá vĩnh viễn (24, JEP 486); 32-bit x86 port bị xoá |

> Ghi chú: bảng ghi phiên bản **final** của tính năng; số trong ngoặc là phiên bản non-LTS nơi tính năng trở thành final.

### 11.3 Checklist migrate 8 → 17/21 (những gì thực sự gây sự cố)

1. **Thư viện Java EE bị xoá (11)**: `ClassNotFoundException: javax.xml.bind.JAXBContext` → thêm dependency `jakarta.xml.bind-api` + implementation (`org.glassfish.jaxb:jaxb-runtime`). Lưu ý xung đột namespace `javax.*` vs `jakarta.*` (chi tiết trong module Spring — Spring Boot 3 yêu cầu Java 17 và Jakarta EE 9+).
2. **Strong encapsulation (16/17)**: `InaccessibleObjectException: Unable to make field ... accessible: module java.base does not "opens" java.lang`. Nâng phiên bản Lombok (≥ 1.18.22 cho 17), Mockito/ByteBuddy, Spring (≥ 5.3.x cho 17), Hibernate, Jackson, Groovy, Gradle/Maven plugin, agent APM. `--add-opens` chỉ là giải pháp tạm.
3. **Build tool & bytecode**: dùng `--release 17` (không chỉ `-source/-target`) để compiler kiểm tra cả API; nâng `maven-compiler-plugin`, `maven-surefire-plugin`; ASM/ByteBuddy phải hỗ trợ class file version mới (Java 17 = 61, Java 21 = 65).
4. **Parse version string**: code cũ parse `System.getProperty("java.version")` dạng `1.8.0_292` sẽ vỡ với `17.0.2` → dùng `Runtime.version().feature()`.
5. **GC thay đổi**: mặc định Parallel (8) → G1 (9+). Các flag GC cũ (`-XX:+UseConcMarkSweepGC` — CMS bị xoá ở 14, `-XX:+PrintGCDetails` → unified logging `-Xlog:gc*`) khiến JVM **không khởi động**. Đo lại throughput/latency sau khi đổi.
6. **Locale/format**: từ Java 9, CLDR là nguồn locale data mặc định → format ngày/tiền tệ/số có thể khác (vd tên tháng viết tắt, ký tự phân cách) → test snapshot vỡ, parse chuỗi ngày từ file cũ lỗi. Tạm thời: `-Djava.locale.providers=COMPAT,CLDR` (COMPAT bị xoá ở 23).
7. **Charset mặc định UTF-8 (18)**: ứng dụng chạy Windows đọc file bằng `FileReader` không chỉ định charset sẽ đổi hành vi.
8. **Container**: Java 8 cũ (< 8u191) không nhận biết giới hạn CPU/RAM của cgroup → heap mặc định quá lớn → OOMKilled. Java 11+ dùng `-XX:MaxRAMPercentage` thay vì `-Xmx` cứng. cgroup v2 cần JDK ≥ 8u372/11.0.16/15+.
9. **`Collectors.toList()` → `Stream.toList()`**: refactor tự động có thể gây `UnsupportedOperationException` khi code sau `add` vào list.
10. **Serialization/`finalize`/`Thread.stop`/Security Manager**: các API deprecated for removal; scan bằng `jdeprscan`.

Công cụ: `jdeps --jdk-internals`, `jdeprscan`, OpenRewrite recipes (`org.openrewrite.java.migrate.UpgradeToJava17/21`), chạy test suite trên JDK mới trong CI trước khi đổi runtime production.

> 💡 **Góc nhìn Senior:** chiến lược migrate an toàn: (1) **build trên JDK mới nhưng `--release` cũ** để phát hiện vấn đề tooling; (2) **chạy trên JDK mới** (runtime) với bytecode cũ — phát hiện vấn đề reflection/GC/locale; (3) cuối cùng mới **nâng `--release`** và dùng feature mới. Mỗi bước deploy canary, so sánh GC log, latency p99, error rate. Câu trả lời phỏng vấn tốt về migration luôn nhắc đến **dependency upgrade**, **thay đổi GC mặc định**, **strong encapsulation**, và **kế hoạch rollback**.

> ⚠️ **Lỗi thường gặp:** chỉ đổi `JAVA_HOME` rồi kết luận "chạy được"; quên đổi base image Docker (vẫn chạy JRE 8); dùng preview feature trong production (bytecode preview chỉ chạy được đúng phiên bản JDK đó với `--enable-preview`).

### 🛠 Bài tập phần 11

**Bài 11.1 — Nhận diện phiên bản (Cơ bản)**
- Đề bài: Cho 15 đoạn code ngắn (tự viết: `var`, `List.of`, `record`, `switch ->`, `"""`, `Stream.toList()`, `instanceof Point(int x, int y)`, `list.getFirst()`, `HttpClient`, `Optional.isEmpty`, `String.repeat`, `Collectors.teeing`, `sealed`, `case null`, `Thread.ofVirtual()`), ghi phiên bản Java **tối thiểu** để compile (không dùng preview).
- Tiêu chí đạt: kiểm chứng bằng `javac --release N` với Docker image các JDK khác nhau.

**Bài 11.2 — Migrate project mẫu (Trung bình)**
- Đề bài: Lấy một project Java 8 nhỏ (hoặc tự tạo: dùng JAXB để parse XML, `SimpleDateFormat` static, reflection vào `String.value`, flag `-XX:+UseConcMarkSweepGC` trong script chạy). Migrate lên Java 21.
- Tiêu chí đạt: viết migration log liệt kê từng lỗi gặp, nguyên nhân, cách sửa; test pass trên JDK 21; dùng `jdeps` và `jdeprscan` và dán output.

**Bài 11.3 — Đề xuất nâng cấp (Nâng cao)**
- Đề bài: Viết tài liệu 1–2 trang (như gửi cho tech lead) đề xuất nâng cấp một hệ thống microservice 20 service đang chạy Java 11 + Spring Boot 2.7 lên Java 21 + Spring Boot 3.x: lợi ích đo được, rủi ro, thứ tự thực hiện, kế hoạch rollback, cách đo hiệu quả.
- Tiêu chí đạt: có nêu javax → jakarta, Hibernate 6, virtual threads (bật có điều kiện), GC, image Docker, test canary.

<details>
<summary>Gợi ý lời giải</summary>

11.1 (đáp án): `var` 10; `List.of` 9; `record` 16; `switch ->` 14; text block 15; `Stream.toList()` 16; record pattern 21; `getFirst()` 21; `HttpClient` 11; `Optional.isEmpty` 11; `String.repeat` 11; `teeing` 12; `sealed` 17; `case null` 21; `Thread.ofVirtual()` 21.

```bash
docker run --rm -v "$PWD":/src -w /src eclipse-temurin:17-jdk javac --release 17 Test.java
```

11.3: lộ trình gợi ý — (1) nâng Spring Boot 2.7 lên bản cuối, xử lý deprecation; (2) đổi runtime sang JDK 17/21 giữ Boot 2.7 (Boot 2.7 chạy được trên 17; kiểm tra hỗ trợ 21 của từng thư viện); (3) Boot 3 + jakarta (OpenRewrite recipe); (4) bật virtual threads (`spring.threads.virtual.enabled=true`, Boot 3.2+) cho service I/O-bound sau khi kiểm tra pinning và giới hạn connection pool.
</details>

---

<a id="du-an-mini"></a>
## Dự án mini: "Order Analytics Engine"

Xây dựng một thư viện + CLI phân tích dữ liệu đơn hàng thương mại điện tử, dùng **toàn bộ** kiến thức Modern Java trong module.

### Bối cảnh
Dữ liệu: file CSV/JSON Lines (tự sinh ≥ 2 triệu dòng bằng generator có seed) gồm các **sự kiện** đơn hàng: `OrderCreated`, `ItemAdded`, `OrderPaid`, `OrderShipped`, `OrderCancelled`, `RefundIssued`; mỗi sự kiện có `orderId`, `timestamp` (ISO-8601 có offset), `customerTimeZone` (ví dụ `Asia/Ho_Chi_Minh`, `Europe/Berlin`, `America/New_York`), dữ liệu riêng.

### Yêu cầu chức năng
1. **Mô hình dữ liệu**: `sealed interface OrderEvent` + records; value object `Money`, `OrderId` có validate trong compact constructor; bất biến sâu.
2. **Parser**: đọc file bằng `Files.lines` (stream, không load toàn bộ), chuyển dòng thành event bằng switch pattern trên loại sự kiện; dòng lỗi được đếm và ghi ra file `errors.log` (không làm dừng xử lý).
3. **Rebuild trạng thái**: từ event stream dựng `OrderState` cho từng đơn (sealed: `Draft`, `Paid`, `Shipped`, `Cancelled`, `Refunded`), chuyển trạng thái sai → ghi lỗi.
4. **Báo cáo** (mỗi báo cáo là một `Collector` hoặc pipeline riêng, có unit test):
   - Doanh thu theo ngày **theo giờ địa phương của khách hàng** (chú ý DST) và theo ngày UTC — so sánh sự khác biệt.
   - Top 10 sản phẩm theo doanh thu, top 10 khách hàng theo số đơn.
   - Phân bố thời gian từ `Paid` → `Shipped` (p50/p95/p99, dùng `Duration`).
   - Tỷ lệ huỷ theo khung giờ trong ngày (`partitioningBy`/`groupingBy`).
5. **Tỷ giá**: gọi một API tỷ giá (có thể là stub server cục bộ dùng `com.sun.net.httpserver`) bằng `HttpClient` với timeout và cache kết quả trong bộ nhớ để quy đổi về VND.
6. **CLI**: `java -jar analytics.jar --input data.jsonl --report revenue-daily --zone local|utc --from 2024-03-01 --to 2024-04-30`.

### Yêu cầu phi chức năng
- Java 21, không dùng preview feature; build bằng Maven/Gradle với `--release 21`.
- Mọi chỗ lấy thời gian hiện tại phải qua `Clock` inject.
- Không `Optional` ở field/tham số; không `toMap` thiếu merge function; mọi `Files.lines` trong try-with-resources.
- Xử lý 2 triệu sự kiện trong < 10 giây trên laptop 8 core, heap tối đa 1GB (`-Xmx1g`). Có benchmark JMH cho ít nhất một report so sánh sequential vs parallel, kèm kết luận có nên bật parallel hay không.
- Test coverage ≥ 80% cho phần domain và collector; có test DST cho ngày 31/03/2024 và 27/10/2024 (Europe/Berlin), 10/03/2024 và 03/11/2024 (America/New_York).
- (Tuỳ chọn) đóng gói thành 2 module JPMS `analytics.core` và `analytics.cli`.

### Tiêu chí chấm (100 điểm)
| Hạng mục | Điểm |
|---|---|
| Mô hình domain đúng (sealed/record/bất biến sâu, validate) | 15 |
| Stream/Collector đúng, dễ đọc, không side-effect, không bẫy `toMap`/null | 20 |
| Xử lý thời gian đúng (DST, UTC vs local, `Clock`, precision) | 20 |
| Pattern matching exhaustive, không `default` thừa | 10 |
| HttpClient đúng (timeout, reuse, xử lý status/lỗi) | 10 |
| Hiệu năng + benchmark JMH có phân tích | 15 |
| Test & tài liệu README (cách chạy, quyết định thiết kế, trade-off) | 10 |

---

<a id="checklist-tu-danh-gia"></a>
## Checklist tự đánh giá

- [ ] Tôi giải thích được lambda được compile thế nào (desugar + `invokedynamic` + `LambdaMetafactory`) và vì sao biến capture phải effectively final.
- [ ] Tôi phân biệt được 4 loại method reference và biết `obj::m` evaluate `obj` ngay lúc tạo.
- [ ] Tôi dự đoán đúng thứ tự thực thi của một stream pipeline có `peek`, `sorted`, `limit`, `findFirst`.
- [ ] Tôi giải thích được stateless vs stateful operation, short-circuit, và vì sao `peek` có thể không chạy với `count()`.
- [ ] Tôi biết ba bẫy của `Collectors.toMap` (key trùng, value null, thứ tự) và cách xử lý.
- [ ] Tôi dùng thành thạo `groupingBy` với downstream (`counting`, `mapping`, `filtering`, `collectingAndThen`, `teeing`) và biết khác biệt `filter` trước vs `filtering` bên trong.
- [ ] Tôi phân biệt `Stream.toList()`, `Collectors.toList()`, `Collectors.toUnmodifiableList()`.
- [ ] Tôi giải thích được parallel stream dùng `ForkJoinPool.commonPool()`, khi nào có lợi (NQ, source split tốt) và vì sao blocking I/O trong parallel stream nguy hiểm cho cả JVM.
- [ ] Tôi đo hiệu năng bằng JMH chứ không bằng `currentTimeMillis`.
- [ ] Tôi nêu được mục đích thiết kế của `Optional` và ít nhất 5 anti-pattern; biết `orElse` vs `orElseGet`.
- [ ] Tôi chọn đúng kiểu `java.time` cho từng tình huống (Instant, LocalDateTime, ZonedDateTime, OffsetDateTime, Duration, Period).
- [ ] Tôi giải thích được DST gap/overlap và hành vi của `ZonedDateTime.of` trong từng trường hợp; khác biệt `plusDays(1)` và `plus(Duration.ofDays(1))`.
- [ ] Tôi dùng `Clock` để code có thể test và dùng `nanoTime` để đo khoảng thời gian.
- [ ] Tôi biết giới hạn của `var` và khi nào không nên dùng.
- [ ] Tôi viết được switch expression exhaustive và text block với `\` và `\s`.
- [ ] Tôi giải thích được record sinh ra những gì, compact constructor, vì sao record chỉ bất biến nông, và vì sao record không hợp làm JPA entity.
- [ ] Tôi nêu được luật của sealed class (final/sealed/non-sealed, cùng package/module) và lợi ích exhaustiveness.
- [ ] Tôi viết được switch pattern matching với record pattern lồng, guard `when`, `case null`, và hiểu dominance.
- [ ] Tôi biết các API Sequenced Collections và `reversed()` là view.
- [ ] Tôi giải thích được `exports` vs `opens`, strong encapsulation và cách xử lý `InaccessibleObjectException`.
- [ ] Tôi dùng `HttpClient` đúng cách (reuse, timeout, check status).
- [ ] Tôi kể được các tính năng chính và breaking change của từng LTS 8/11/17/21 (và 25), cùng checklist migrate thực tế.

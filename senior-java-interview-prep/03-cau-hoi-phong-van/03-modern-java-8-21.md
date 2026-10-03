# Câu hỏi phỏng vấn — Module 03: Modern Java (8 → 21)

> Giáo trình tương ứng: [Module 03 — Modern Java (8 → 21)](../01-giao-trinh/03-modern-java-8-21.md)

**Cách dùng:** đọc câu hỏi, **tự trả lời thành tiếng** trong 1–2 phút (câu đọc code: viết output ra giấy trước) *rồi mới* mở "Đáp án". **Trả lời ngắn** là phần phải nói được trong 30 giây; **Giải thích chi tiết** và **Câu hỏi nối tiếp** là chỗ interviewer phân biệt Senior. Câu nào sai hoặc ấp úng → bấm link 📖 quay lại giáo trình.

**Mức độ:** 🟢 Cơ bản · 🟡 Senior · 🔴 Xoáy sâu. **Dạng câu:** 🧩 tình huống · 🔍 đọc code.

> 💡 Phân biệt **preview** và **final** khi trả lời "tính năng X có từ Java mấy": interviewer rất hay bẫy chỗ này (ví dụ pattern matching cho switch chỉ là preview ở 17, final ở 21).

## Mục lục

| Nhóm | Chủ đề | Câu |
|---|---|---|
| A | [Functional interface, lambda, method reference](#nhom-a) | Q1–Q6 |
| B | [Stream API — cơ chế lazy](#nhom-b) | Q7–Q13 |
| C | [Collectors](#nhom-c) | Q14–Q19 |
| D | [Parallel stream & hiệu năng](#nhom-d) | Q20–Q24 |
| E | [Optional](#nhom-e) | Q25–Q28 |
| F | [java.time, time zone, DST](#nhom-f) | Q29–Q35 |
| G | [`var`, switch expression, text block](#nhom-g) | Q36–Q38 |
| H | [Records & sealed classes](#nhom-h) | Q39–Q44 |
| I | [Pattern matching](#nhom-i) | Q45–Q48 |
| J | [Sequenced Collections, JPMS, HttpClient](#nhom-j) | Q49–Q51 |
| K | [Phiên bản LTS & migration](#nhom-k) | Q52–Q56 |

---

<a id="nhom-a"></a>
## A. Functional interface, lambda, method reference

### Q1. 🟢 Functional interface là gì? Kể các interface chuẩn trong `java.util.function`.

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Interface có **đúng một abstract method** (SAM). Có thể có thêm `default`, `static` method và các method trùng chữ ký `public` method của `Object` (như `equals`) — không tính. `@FunctionalInterface` không bắt buộc nhưng nên dùng để compiler chặn ai đó thêm abstract method thứ hai. Bộ chuẩn: `Supplier<T>` (không vào, 1 ra), `Consumer<T>` (1 vào, không ra), `Function<T,R>`, `Predicate<T>`, `UnaryOperator<T>`, `BinaryOperator<T>`, các biến thể `Bi*` và primitive (`IntPredicate`, `ToIntFunction`, `IntUnaryOperator`...).

**Giải thích chi tiết:**
- Kiểu của lambda phụ thuộc **ngữ cảnh** (target typing): `x -> x + 1` có thể là `Function<Integer,Integer>` hoặc `IntUnaryOperator`.
- Biến thể primitive tránh boxing trong hot path.
- Composition: `Predicate.and/or/negate`, `Predicate.not` (Java 11), `Function.andThen/compose`, `Comparator.thenComparing`.
- `Comparator` là functional interface dù khai báo `equals` (thuộc `Object`).

**Câu hỏi nối tiếp:**
- *`Runnable` và `Callable` khác gì?* — `Callable<V>` trả giá trị và khai báo `throws Exception`; `Runnable` thì không.

**⚠️ Câu trả lời gây điểm trừ:**
- "Interface chỉ có một method" — quên default/static được phép.

**📖 Ôn lại:** [Phần 1, mục 1.1](../01-giao-trinh/03-modern-java-8-21.md#p1)

</details>

### Q2. 🟢 Có mấy loại method reference? Cho ví dụ từng loại.

<details><summary>Đáp án</summary>

**Trả lời ngắn:** 4 loại: **static** (`Integer::parseInt` ≡ `s -> Integer.parseInt(s)`); **bound instance** — method của một object cụ thể (`System.out::println`); **unbound instance** — method của object tùy ý cùng kiểu, object là tham số đầu (`String::toUpperCase` ≡ `s -> s.toUpperCase()`); **constructor** (`ArrayList::new`).

**Giải thích chi tiết:**
- Unbound với hai tham số: `String::compareToIgnoreCase` là `Comparator<String>` ≡ `(a, b) -> a.compareToIgnoreCase(b)`.
- Constructor ref cho mảng: `String[]::new` (dùng với `toArray`).
- Effective Java Item 43: dùng method reference khi nó **rõ ràng hơn** lambda; lambda > 3–5 dòng nên tách thành method có tên (Item 42).

**Câu hỏi nối tiếp:**
- *Bound reference có khác lambda ở thời điểm đánh giá?* — Có, xem Q5.

**⚠️ Câu trả lời gây điểm trừ:**
- Không phân biệt được bound và unbound.

**📖 Ôn lại:** [Phần 1, mục 1.1 — Method reference](../01-giao-trinh/03-modern-java-8-21.md#p1)

</details>

### Q3. 🟡 Vì sao biến local được lambda capture phải "effectively final"? Lambda dùng field thì sao?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Lambda có thể chạy **sau khi** method đã return (submit vào executor), khi stack frame chứa biến local đã mất → Java **copy giá trị** vào lambda lúc tạo. Nếu cho gán lại, người đọc tưởng lambda thấy giá trị mới nhưng không phải; và nếu lambda chạy thread khác mà cùng sửa biến local thì không có cơ chế đồng bộ nào. Dùng field instance (`this.x`) thì lambda **capture `this`** — giữ reference tới object bao ngoài → có thể gây memory leak khi lambda đăng ký vào object sống lâu.

**Giải thích chi tiết:**

```java
int count = 0;
list.forEach(x -> count++);        // ❌ compile error
int[] holder = {0};
list.forEach(x -> holder[0]++);    // compile được nhưng race nếu parallel — code smell
```

- Cách đúng thường là dùng thao tác tổng hợp: `count()`, `reduce`, `collect`.
- Khác anonymous class: `this` trong lambda là object bao ngoài; lambda không tạo scope mới (không khai báo trùng tên biến local); anonymous class trước JDK 18 **luôn** giữ `this$0`, lambda chỉ capture `this` khi thực sự dùng.

**Câu hỏi nối tiếp:**
- *Field instance có phải effectively final không?* — Không cần; lambda đọc qua `this` nên thấy giá trị mới nhất (và có thể race nếu không đồng bộ).

**⚠️ Câu trả lời gây điểm trừ:**
- "Vì Java thích immutable" mà không nói tới copy giá trị và vòng đời stack frame.

**📖 Ôn lại:** [Phần 1, mục 1.2 — Capture và effectively final](../01-giao-trinh/03-modern-java-8-21.md#p1)

</details>

### Q4. 🔴 Lambda được compile và chạy thế nào bên dưới? Mỗi lần evaluate lambda có tạo object mới không?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Lambda **không** sinh file `Outer$1.class` như anonymous class. `javac` desugar thân lambda thành **private (static) method** `lambda$main$0` và sinh lệnh **`invokedynamic`** với bootstrap `LambdaMetafactory.metafactory`. Lần đầu thực thi, JVM gọi bootstrap → sinh class implement functional interface (từ Java 15 là **hidden class**, JEP 371) và link `CallSite`; các lần sau dùng lại. **Non-capturing** lambda thường được cache thành singleton tại call site; **capturing** lambda tạo instance mới mỗi lần evaluate (JIT có thể loại allocation bằng escape analysis).

**Giải thích chi tiết:**
- Lần gọi đầu có chi phí bootstrap (vài chục µs mỗi lambda) → ảnh hưởng startup; GraalVM Native Image phải xử lý lambda đặc biệt lúc build.
- Stack trace chứa frame `lambda$xxx$N` — cần đọc được.
- Không dựa vào identity (`==`) của lambda — đặc tả không đảm bảo.
- Đây cũng là cơ chế nối chuỗi Java 9+ (`StringConcatFactory`) và pattern switch Java 21 (`SwitchBootstraps`).

**Câu hỏi nối tiếp:**
- *Có nên tránh lambda trong hot loop?* — Chỉ khi profiler chỉ ra allocation; thường JIT xử lý tốt.

**⚠️ Câu trả lời gây điểm trừ:**
- "Lambda là cú pháp rút gọn của anonymous class" (về mặt bytecode thì sai).

**📖 Ôn lại:** [Phần 1, mục 1.3 — invokedynamic & LambdaMetafactory](../01-giao-trinh/03-modern-java-8-21.md#p1)

</details>

### Q5. 🟡 🔍 Dòng nào ném `NullPointerException`?

```java
String str = null;
Supplier<Integer> a = () -> str.length();   // (1)
Supplier<Integer> b = str::length;          // (2)
System.out.println(a.get());                // (3)
```

<details><summary>Đáp án</summary>

**Trả lời ngắn:** **Dòng (2)** ném NPE ngay lúc tạo: bound method reference `obj::method` **đánh giá `obj` tại thời điểm tạo** (bytecode có `Objects.requireNonNull`). Lambda (1) chỉ ném khi được gọi — tức là ở (3) nếu chương trình chạy tới đó.

**Giải thích chi tiết:**
- Hệ quả thực tế: `Optional.ofNullable(x).map(service::lookup)` với `service == null` ném ngay khi build pipeline, kể cả khi Optional rỗng.
- Bound reference cũng "chụp" object tại thời điểm tạo: `Supplier<String> s = holder.current()::name;` — đổi `holder.current()` sau đó không ảnh hưởng `s`.

**Câu hỏi nối tiếp:**
- *Unbound `String::length` (là `Function<String,Integer>`) thì sao?* — Không có object để đánh giá sớm; NPE khi gọi `apply(null)`.

**⚠️ Câu trả lời gây điểm trừ:**
- Cho rằng hai dạng hoàn toàn tương đương.

**📖 Ôn lại:** [Phần 1 — Góc nhìn Senior](../01-giao-trinh/03-modern-java-8-21.md#p1)

</details>

### Q6. 🟡 Lambda gọi method ném checked exception (`Files.readString`) thì xử lý thế nào? `executor.submit(() -> compute())` chọn overload nào?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Functional interface chuẩn không khai báo `throws` → phải **wrap**: `IOException` → `UncheckedIOException`, exception khác → `RuntimeException` giữ cause; hoặc tự định nghĩa `ThrowingFunction<T,R,E extends Exception>` cùng helper `unchecked(...)`. **Không nuốt exception** trong lambda. Với `submit`: nếu `compute()` trả giá trị, lambda tương thích cả `Callable` và `Runnable`, compiler chọn **`Callable`** (cụ thể hơn vì trả giá trị); nếu `compute()` là `void` thì chỉ `Runnable`.

**Giải thích chi tiết:**

```java
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

- Trường hợp có nhiều nhánh xử lý lỗi, for-loop thường rõ ràng hơn stream (Effective Java Item 45).
- Với `submit`, exception nằm trong `Future` — không gọi `get()` là mất lỗi.

**Câu hỏi nối tiếp:**
- *Lombok `@SneakyThrows` thì sao?* — Lách compiler (checked chỉ tồn tại ở compile time); tiện nhưng làm caller không biết exception có thể xảy ra.

**⚠️ Câu trả lời gây điểm trừ:**
- `catch (Exception e) { return null; }` trong lambda.

**📖 Ôn lại:** [Phần 1 — Góc nhìn Senior & Lỗi thường gặp](../01-giao-trinh/03-modern-java-8-21.md#p1)

</details>

---

<a id="nhom-b"></a>
## B. Stream API — cơ chế lazy

### Q7. 🟢 Stream khác Collection thế nào? Intermediate và terminal operation là gì?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Stream **không lưu dữ liệu** — là chuỗi thao tác trên một source; **không sửa source**; **lazy** — intermediate operation không chạy cho tới khi có terminal; **dùng một lần** (gọi lại ném `IllegalStateException: stream has already been operated upon or closed`); có thể **vô hạn** (`Stream.iterate/generate`). Pipeline = source → 0..n **intermediate** (trả Stream: `filter`, `map`, `sorted`...) → 1 **terminal** (kích hoạt: `collect`, `forEach`, `reduce`, `count`, `toList`...).

**Giải thích chi tiết:**
- Phân loại thêm: stateless (`filter`, `map`, `flatMap`) vs stateful (`distinct`, `sorted`, `limit`, `skip`); short-circuit (`limit`, `takeWhile`, `findFirst`, `anyMatch`...).
- Cần dùng lại → lưu `Supplier<Stream<T>>`.
- `Stream.of(intArray)` ra `Stream<int[]>` một phần tử → dùng `Arrays.stream(int[])`/`IntStream.of`.

**Câu hỏi nối tiếp:**
- *Sửa source trong pipeline?* — Hành vi không xác định, thường CME (non-interference).

**⚠️ Câu trả lời gây điểm trừ:**
- "Stream là collection kiểu mới".

**📖 Ôn lại:** [Phần 2, mục 2.1](../01-giao-trinh/03-modern-java-8-21.md#p2)

</details>

### Q8. 🟡 🔍 Đoạn code sau in ra gì?

```java
Stream.of("a", "bb", "ccc", "dddd")
      .filter(s -> { System.out.println("filter " + s); return s.length() > 1; })
      .map(s -> { System.out.println("map " + s); return s.toUpperCase(); })
      .findFirst()
      .ifPresent(System.out::println);
```

<details><summary>Đáp án</summary>

**Trả lời ngắn:** `filter a`, `filter bb`, `map bb`, `BB`. Stream chạy **theo chiều dọc**: mỗi phần tử đi qua toàn bộ chuỗi operation rồi mới tới phần tử tiếp theo; `findFirst` short-circuit nên `ccc`, `dddd` không bao giờ được xử lý.

**Giải thích chi tiết:**
- Chèn `.sorted()` giữa `filter` và `map`: `sorted` là **barrier** — mọi phần tử qua `filter` trước (`filter a, bb, ccc, dddd`), được buffer, rồi mới chảy tiếp xuống `map` (`map bb`, `BB`).
- `sorted()`/`distinct()` trên stream vô hạn trước `limit` → treo (và có thể OOM).

**Câu hỏi nối tiếp:**
- *Vì sao thiết kế vertical?* — Cho phép fusion thành một vòng lặp, short-circuit, và xử lý stream vô hạn mà không cần buffer trung gian.

**⚠️ Câu trả lời gây điểm trừ:**
- In tất cả `filter` trước rồi mới `map` (tư duy "từng tầng").

**📖 Ôn lại:** [Phần 2, mục 2.2 — Lazy evaluation và vertical execution](../01-giao-trinh/03-modern-java-8-21.md#p2)

</details>

### Q9. 🔴 🔍 Trên Java 17, đoạn code in gì? Bài học là gì?

```java
long n = List.of(1, 2, 3).stream()
        .peek(x -> System.out.println("audit " + x))
        .count();
System.out.println(n);
```

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Chỉ in `3`. Từ Java 9, `count()` **không chạy pipeline** nếu source `SIZED` và không có operation nào thay đổi số lượng phần tử → `peek` **không được gọi**. Java 8 in `audit 1..3` rồi `3`. Bài học: **không đặt logic nghiệp vụ/side-effect trong `peek`** — nó chỉ dành cho debug; đặc tả cho phép bỏ qua.

**Giải thích chi tiết:**
- Thêm `.filter(x -> true)` thì pipeline phải chạy (filter có thể đổi số lượng) → `peek` được gọi.
- Tối ưu tương tự dựa trên stream flags: `sorted()` trên source đã `SORTED` (TreeSet) là no-op; `distinct()` trên `Set` cũng vậy.
- Side-effect hợp lệ chỉ nên ở terminal `forEach`, và tốt nhất là tránh.

**Câu hỏi nối tiếp:**
- *Code audit/log bằng `peek` bị "mất" sau khi nâng Java 8 → 11 — điều tra thế nào?* — Đây chính là nguyên nhân; chuyển logic ra khỏi `peek`.

**⚠️ Câu trả lời gây điểm trừ:**
- Khẳng định chắc chắn in `audit 1..3`.

**📖 Ôn lại:** [Phần 2, mục 2.3 — Spliterator, Sink, stream flags](../01-giao-trinh/03-modern-java-8-21.md#p2)

</details>

### Q10. 🟡 Stateless và stateful operation khác nhau thế nào? Vì sao điều đó quan trọng với stream lớn hoặc vô hạn?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Stateless (`filter`, `map`, `flatMap`, `mapMulti`) xử lý từng phần tử độc lập, không cần nhớ gì → chạy streaming, bộ nhớ O(1). Stateful (`distinct`, `sorted`, `limit`, `skip`) cần trạng thái: `sorted` phải **buffer toàn bộ** stream, `distinct` giữ set các phần tử đã thấy → bộ nhớ O(n), là barrier, treo với stream vô hạn, và kém song song hóa (đặc biệt `limit/skip` trên stream có thứ tự).

**Giải thích chi tiết:**
- Xử lý file 10 GB bằng `Files.lines(...).sorted()` → OOM; cần external sort hoặc đẩy việc sắp xếp xuống DB.
- `takeWhile/dropWhile` (Java 9) chỉ có nghĩa trên stream có thứ tự.
- `Stream.iterate(seed, hasNext, next)` (Java 9) thay vòng for và tự kết thúc.
- `mapMulti` (Java 16) thay `flatMap` khi mỗi phần tử sinh 0–vài phần tử, tránh tạo Stream con.

**Câu hỏi nối tiếp:**
- *`unordered()` giúp gì?* — Bỏ ràng buộc thứ tự để `distinct`/`limit` chạy song song rẻ hơn.

**⚠️ Câu trả lời gây điểm trừ:**
- Không biết `sorted` buffer toàn bộ stream.

**📖 Ôn lại:** [Phần 2, mục 2.1–2.2](../01-giao-trinh/03-modern-java-8-21.md#p2)

</details>

### Q11. 🔴 Stream được hiện thực thế nào bên trong? Spliterator, Sink, stream flags có vai trò gì?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Source được bọc trong **`Spliterator`** — iterator có thể chia đôi (`trySplit`) cho parallel, kèm characteristics (`SIZED`, `SUBSIZED`, `ORDERED`, `DISTINCT`, `SORTED`, `NONNULL`, `IMMUTABLE`, `CONCURRENT`). Mỗi intermediate op tạo một stage `AbstractPipeline` nối vào stage trước — **chưa chạy gì**. Khi có terminal op, các stage được wrap thành chuỗi **`Sink`** lồng nhau (`begin/accept/end/cancellationRequested`); source đẩy từng phần tử vào sink đầu → các op được **fuse** thành một vòng lặp duy nhất. **Stream flags** cho phép bỏ qua việc thừa (`sorted` trên source đã SORTED, `count` trên source SIZED).

**Giải thích chi tiết:**
- `cancellationRequested` là cách short-circuit (`findFirst`, `limit`) báo cho source dừng.
- Chất lượng `trySplit` quyết định hiệu quả parallel: `ArrayList`/mảng/`IntStream.range` chia đôi theo index O(1) cân bằng; `LinkedList`, `Stream.iterate`, `BufferedReader.lines()` chia kém (tách batch tuần tự 1024, 2048...).
- Tự viết Spliterator khi cần nguồn tùy biến (ví dụ chia list thành batch để insert DB) — khai báo characteristics đúng, `trySplit` cắt đúng biên.

**Câu hỏi nối tiếp:**
- *Khai báo sai characteristics (ví dụ `SIZED` khi không biết size) gây gì?* — Kết quả sai hoặc tối ưu sai (count trả số sai, mảng `toArray` sai kích thước).

**⚠️ Câu trả lời gây điểm trừ:**
- "Mỗi operation tạo một list trung gian".

**📖 Ôn lại:** [Phần 2, mục 2.3 — Bên dưới nắp capo](../01-giao-trinh/03-modern-java-8-21.md#p2)

</details>

### Q12. 🔴 🔍 `reduce` có mấy dạng? Đoạn code sau cho kết quả gì khi chạy tuần tự và song song?

```java
int seq = Stream.of(1, 2, 3).reduce(10, Integer::sum);
int par = Stream.of(1, 2, 3).parallel().reduce(10, Integer::sum);
```

<details><summary>Đáp án</summary>

**Trả lời ngắn:** `seq = 16`; `par` **không xác định** — thường là `36` vì identity `10` được cộng **một lần cho mỗi chunk** (`11 + 12 + 13`). Identity phải là **phần tử trung hòa thật** (`combiner(identity, u) == u`): với phép cộng là `0`. Ba dạng: `reduce(BinaryOperator)` → `Optional<T>`; `reduce(identity, BinaryOperator)`; `reduce(identity, BiFunction accumulator, BinaryOperator combiner)` cho kiểu kết quả khác kiểu phần tử. Accumulator phải **associative, non-interfering, stateless**.

**Giải thích chi tiết:**
- Muốn "cộng thêm 10": `10 + stream.reduce(0, Integer::sum)` hoặc `IntStream.sum()`.
- Kết quả là container mutable (List, StringBuilder, Map) → **dùng `collect`** (mutable reduction), không dùng `reduce` (tạo object mới mỗi bước → O(n²) copy).
- Phép không associative (trừ, trung bình theo cặp) cho kết quả sai khi parallel.

**Câu hỏi nối tiếp:**
- *Cộng `BigDecimal` trong stream?* — `reduce(BigDecimal.ZERO, BigDecimal::add)` hoặc `Collectors.reducing(...)`.

**⚠️ Câu trả lời gây điểm trừ:**
- Trả lời `par = 16`.

**📖 Ôn lại:** [Phần 2, mục 2.4 — reduce](../01-giao-trinh/03-modern-java-8-21.md#p2)

</details>

### Q13. 🟡 🧩 Đồng nghiệp chuyển mọi vòng `for` sang stream "cho hiện đại". Khi review, bạn góp ý thế nào?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Stream không phải lúc nào cũng dễ đọc hơn (Effective Java Item 45 "Use streams judiciously"). Giữ for-loop khi: logic nhiều nhánh, cần `break/continue` có điều kiện phức tạp, cần xử lý checked exception, cần cập nhật nhiều biến trạng thái, hoặc cần chỉ số. Dùng stream khi pipeline biến đổi/lọc/gom nhóm rõ ràng. Kiểm tra thêm các bẫy: stream I/O (`Files.lines`, `Files.walk`) phải **đóng bằng try-with-resources**; không dùng lại stream; không side-effect trong lambda; dùng **primitive stream** cho tính toán số.

**Giải thích chi tiết:**
- Hiệu năng: với collection nhỏ và thao tác đơn giản, for-loop thường nhanh hơn một chút (chi phí dựng pipeline, lambda, boxing); sau warm-up chênh lệch nhỏ; `IntStream.sum` gần bằng for-loop. Muốn kết luận phải đo bằng **JMH**, không bằng `currentTimeMillis`.
- `Files.walk` không đóng → "Too many open files" trên server.
- Đặt tên cho lambda dài bằng method riêng + method reference.

**Câu hỏi nối tiếp:**
- *Đếm tần suất từ trong file 10 MB?* — `Files.lines` trong TWR + `flatMap(Pattern::splitAsStream)` + `toLowerCase(Locale.ROOT)` + `groupingBy(w -> w, counting())`.

**⚠️ Câu trả lời gây điểm trừ:**
- "Stream luôn nhanh hơn" hoặc "stream luôn chậm hơn" mà không có số đo.

**📖 Ôn lại:** [Phần 2 — Góc nhìn Senior & Lỗi thường gặp](../01-giao-trinh/03-modern-java-8-21.md#p2), [Phần 4, mục 4.4](../01-giao-trinh/03-modern-java-8-21.md#p4)

</details>

---

<a id="nhom-c"></a>
## C. Collectors

### Q14. 🟢 `Collector` gồm những thành phần nào? Vì sao `collect` hiệu quả hơn `reduce` cho container?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** `Collector<T, A, R>` gồm: `supplier()` tạo container trung gian `A`; `accumulator()` `(A, T) -> void` đưa phần tử vào; `combiner()` gộp hai container (khi parallel); `finisher()` `A -> R`; và characteristics (`IDENTITY_FINISH`, `UNORDERED`, `CONCURRENT`). Đây là **mutable reduction**: sửa container tại chỗ, không tạo object mới mỗi bước như `reduce`.

**Giải thích chi tiết:**
- Parallel: mỗi chunk có container riêng (supplier), cuối cùng `combiner` gộp → không cần lock.
- `CONCURRENT + UNORDERED` (như `groupingByConcurrent`): mọi thread ghi vào **một** container đồng thời thay vì gộp.
- Viết nhanh bằng `Collector.of(supplier, accumulator, combiner, [finisher], characteristics...)`.

**Câu hỏi nối tiếp:**
- *`collect(HashMap::new, (m, e) -> m.put(...), HashMap::putAll)` dùng khi nào?* — Khi cần cho phép value `null` (`toMap` cấm).

**⚠️ Câu trả lời gây điểm trừ:**
- Không biết vai trò của combiner.

**📖 Ôn lại:** [Phần 3, mục 3.1](../01-giao-trinh/03-modern-java-8-21.md#p3)

</details>

### Q15. 🔴 🔍 Ba lời gọi `toMap` sau có vấn đề gì?

```java
record Emp(String name, String dept, Double salary) {}
List<Emp> emps = List.of(new Emp("An", "IT", 1000.0), new Emp("Binh", "IT", 1200.0), new Emp("Chi", "HR", null));

emps.stream().collect(Collectors.toMap(Emp::dept, Emp::name));                    // (1)
emps.stream().collect(Collectors.toMap(Emp::name, Emp::salary));                  // (2)
emps.stream().collect(Collectors.toMap(Emp::name, Emp::dept, (a, b) -> a));       // (3)
```

<details><summary>Đáp án</summary>

**Trả lời ngắn:** (1) **`IllegalStateException: Duplicate key IT`** — key trùng mà không có merge function. (2) **`NullPointerException`** — `toMap` cấm value `null` (dùng `putIfAbsent`/`merge` nội bộ). (3) Chạy được, nhưng là `HashMap` → **thứ tự không đảm bảo**; cần thứ tự thì truyền map factory: `toMap(..., (a, b) -> a, LinkedHashMap::new)`.

**Giải thích chi tiết:**
- Bẫy production kinh điển: dữ liệu test không trùng key, dữ liệu thật trùng (import lặp, key không unique như tưởng) → nổ lúc runtime. Mỗi lần viết `toMap` 2 tham số tự hỏi: *"ai đảm bảo key unique?"*.
- Muốn fail-fast: tự viết collector `toMapStrict` ném exception có thông điệp nghiệp vụ chứa key và cả hai value.
- Cho phép null value: `collect(HashMap::new, (m, e) -> m.put(e.name(), e.salary()), HashMap::putAll)`.
- Gom nhiều value cho một key → `groupingBy`, không phải `toMap` với merge nối chuỗi.

**Câu hỏi nối tiếp:**
- *Merge function `(a, b) -> b` có ý nghĩa gì?* — "Bản ghi sau thắng" — phải là quyết định nghiệp vụ có chủ đích, không phải để "cho hết lỗi".

**⚠️ Câu trả lời gây điểm trừ:**
- Cho rằng (1) âm thầm ghi đè như `HashMap.put`.

**📖 Ôn lại:** [Phần 3, mục 3.2 — `toMap` — ba bẫy kinh điển](../01-giao-trinh/03-modern-java-8-21.md#p3)

</details>

### Q16. 🟡 `groupingBy` với downstream collector hoạt động thế nào? `filter(...)` trước `groupingBy` khác `groupingBy(..., filtering(...))` ra sao?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** `groupingBy(classifier)` mặc định ra `HashMap<K, List<T>>`; downstream cho phép tính thẳng trên mỗi nhóm: `counting()`, `summingLong`, `mapping(f, toSet())`, `filtering` (9), `flatMapping` (9), `maxBy`, `collectingAndThen`; tham số map factory (`TreeMap::new`) cho thứ tự. `filter` trước `groupingBy` **mất luôn nhóm** không còn phần tử; `filtering` bên trong **giữ nhóm với list rỗng**.

**Giải thích chi tiết:**

```java
Map<String, Map<String, Long>> nested = txs.stream().collect(
    groupingBy(Tx::account, TreeMap::new, groupingBy(Tx::type, summingLong(Tx::amount))));
Map<String, List<Tx>> bigOnly = txs.stream()
    .collect(groupingBy(Tx::account, filtering(t -> t.amount() > 60, toList()))); // A2 vẫn có, list rỗng
```

- `groupingBy` **cấm key null** → NPE "element cannot be mapped to a null key"; map sang sentinel trước.
- Kiểu kết quả: `counting()` → `Long`, `summingInt` → `Integer`, `averagingX` → `Double`.
- Dữ liệu lớn: gom `List` rồi mới tính tạo hàng triệu `ArrayList` nhỏ — tính trực tiếp bằng downstream (`summingLong`, `counting`).

**Câu hỏi nối tiếp:**
- *Group theo enum?* — `groupingBy(Order::status, () -> new EnumMap<>(Status.class), counting())`.

**⚠️ Câu trả lời gây điểm trừ:**
- Group xong rồi duyệt map để tính tổng bằng vòng lặp.

**📖 Ôn lại:** [Phần 3, mục 3.3](../01-giao-trinh/03-modern-java-8-21.md#p3)

</details>

### Q17. 🟡 `partitioningBy`, `collectingAndThen`, `teeing` dùng khi nào?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** `partitioningBy(predicate, [downstream])` chia thành `Map<Boolean, ...>` **luôn có đủ hai key** `true/false` (kể cả rỗng) — khác `groupingBy` với predicate (thiếu key nếu không có phần tử). `collectingAndThen(collector, finisher)` hậu xử lý kết quả: `maxBy` ra `Optional` → `Optional::orElseThrow`; hoặc bọc `toList()` thành unmodifiable. `teeing(c1, c2, merger)` (Java 12) chạy **hai collector trên cùng stream trong một lượt** rồi gộp — ví dụ đếm và tổng cùng lúc.

**Giải thích chi tiết:**

```java
Map<Boolean, Long> bigSmall = txs.stream().collect(partitioningBy(t -> t.amount() >= 100, counting()));
Map<String, Tx> maxPerAcc = txs.stream().collect(groupingBy(Tx::account,
        collectingAndThen(maxBy(comparingLong(Tx::amount)), Optional::orElseThrow)));
record Stats(long count, long sum) {}
Stats s = txs.stream().collect(teeing(counting(), summingLong(Tx::amount), Stats::new));
```

- `teeing` tránh duyệt stream hai lần (với stream từ I/O thì không thể duyệt lại).
- Thống kê số: `summarizingLong` cho count/sum/min/max/avg một lượt.

**Câu hỏi nối tiếp:**
- *Tính p95 latency bằng collector cho 1 tỷ điểm?* — Sort O(n log n) và O(n) bộ nhớ không khả thi; dùng cấu trúc xấp xỉ (HdrHistogram, t-digest).

**⚠️ Câu trả lời gây điểm trừ:**
- Không biết `partitioningBy` luôn có đủ hai key.

**📖 Ôn lại:** [Phần 3, mục 3.3](../01-giao-trinh/03-modern-java-8-21.md#p3)

</details>

### Q18. 🔴 Viết một `Collector` tùy biến. Combiner phải thỏa điều kiện gì? Characteristics `CONCURRENT`, `UNORDERED`, `IDENTITY_FINISH` ảnh hưởng thế nào?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Dùng `Collector.of(supplier, accumulator, combiner, finisher, characteristics...)`. Combiner phải cho **cùng kết quả** như khi chạy tuần tự — gộp container của chunk trái với chunk phải **đúng thứ tự** (nếu collector có thứ tự). `IDENTITY_FINISH`: finisher là identity, framework bỏ qua bước cast/finish. `UNORDERED`: kết quả không phụ thuộc thứ tự gặp phần tử → cho phép tối ưu. `CONCURRENT`: accumulator gọi được đồng thời trên **một** container dùng chung — chỉ áp dụng khi stream cũng unordered hoặc collector `UNORDERED`.

**Giải thích chi tiết:**

```java
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

- Combiner ném exception là cách "trung thực" nếu không gộp đúng ngữ nghĩa được — nhưng phải document; collector chuẩn mực có combiner đúng (với `chunked`, gộp đúng đòi hỏi dồn lại các chunk dở dang).
- Ví dụ percentile collector: container là list số, combiner `addAll`, finisher sort rồi lấy chỉ số `ceil(p·n) − 1`.
- Kiểm thử: chạy cùng dữ liệu (seed cố định) tuần tự và song song, so sánh kết quả.

**Câu hỏi nối tiếp:**
- *`groupingByConcurrent` nhanh hơn `groupingBy` khi nào?* — Parallel và không cần thứ tự trong mỗi nhóm; tránh chi phí merge nhiều map.

**⚠️ Câu trả lời gây điểm trừ:**
- Combiner `(a, b) -> a` (bỏ mất dữ liệu chunk phải).

**📖 Ôn lại:** [Phần 3, mục 3.4 — Viết Collector tùy biến](../01-giao-trinh/03-modern-java-8-21.md#p3)

</details>

### Q19. 🟢 `Stream.toList()`, `Collectors.toList()` và `Collectors.toUnmodifiableList()` khác nhau thế nào?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** `Stream.toList()` (Java 16): list **unmodifiable**, **cho phép null**. `Collectors.toList()`: **không đảm bảo** gì về kiểu/mutability (thực tế là `ArrayList` — nhưng không được dựa vào). `Collectors.toUnmodifiableList()` (Java 10): unmodifiable, **ném NPE nếu có null**. Muốn chắc chắn mutable: `Collectors.toCollection(ArrayList::new)`.

**Giải thích chi tiết:**
- Bẫy refactor: IDE/OpenRewrite đổi `collect(Collectors.toList())` thành `.toList()` → chỗ nào sau đó `add/remove/sort` list sẽ `UnsupportedOperationException` ở runtime.
- Trả immutable ở ranh giới API là tốt; nhưng phải nhất quán và document.

**Câu hỏi nối tiếp:**
- *`toList()` có copy mảng không?* — Có thể dùng trực tiếp mảng từ `toArray()` bọc thành list immutable — rẻ.

**⚠️ Câu trả lời gây điểm trừ:**
- "Ba cái như nhau".

**📖 Ôn lại:** [Phần 2, mục 2.4 — `Stream.toList()` vs `Collectors.toList()`](../01-giao-trinh/03-modern-java-8-21.md#p2)

</details>

---

<a id="nhom-d"></a>
## D. Parallel stream & hiệu năng

### Q20. 🟡 Parallel stream hoạt động thế nào? Chạy trên thread pool nào, bao nhiêu thread?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Spliterator của source được `trySplit` đệ quy thành các chunk; mỗi chunk là một task chạy trên **`ForkJoinPool.commonPool()`** (work-stealing); kết quả các chunk được combine. `commonPool` có parallelism mặc định = **`availableProcessors() - 1`** (thread gọi stream cũng tham gia), chỉnh bằng `-Djava.util.concurrent.ForkJoinPool.common.parallelism=N`.

**Giải thích chi tiết:**
- `commonPool` **dùng chung toàn JVM**: mọi parallel stream và mọi `CompletableFuture.supplyAsync(...)` không chỉ định executor.
- Trong container, `availableProcessors()` phản ánh CPU limit (Java 10+/8u191) → Pod 1 CPU thì parallelism gần như 1 — parallel stream vô dụng, chỉ tốn overhead.
- Thứ tự: `forEach` trên parallel không theo thứ tự; `forEachOrdered` giữ thứ tự nhưng mất phần lớn lợi ích.

**Câu hỏi nối tiếp:**
- *Work-stealing là gì?* — Mỗi worker có deque riêng; rảnh thì "trộm" task từ đuôi deque của worker khác (chi tiết ở Module 04).

**⚠️ Câu trả lời gây điểm trừ:**
- "Parallel stream tạo thread mới cho mỗi phần tử".

**📖 Ôn lại:** [Phần 4, mục 4.1](../01-giao-trinh/03-modern-java-8-21.md#p4)

</details>

### Q21. 🟡 Khi nào parallel stream thực sự có lợi?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Theo mô hình **NQ** (Brian Goetz): N phần tử × Q chi phí mỗi phần tử phải đủ lớn (kinh nghiệm > ~10.000 đơn vị công việc), **và**: source split tốt (`ArrayList`, mảng, `IntStream.range`), operation **stateless, CPU-bound, độc lập**, terminal dễ gộp (`sum`, `reduce` associative, `groupingByConcurrent`), dữ liệu primitive có locality tốt, và môi trường có core rảnh (batch job, CLI) — **không phải** thread xử lý request của web server.

**Giải thích chi tiết:**

| Tốt cho parallel | Xấu cho parallel |
|---|---|
| `ArrayList`, array, `IntStream.range` | `LinkedList`, `Stream.iterate`, `BufferedReader.lines()` |
| stateless, CPU-bound | `sorted`, `distinct`, `limit`, `findFirst` trên stream có thứ tự |
| `sum`, `reduce` associative | `forEachOrdered`, collect vào `LinkedHashMap` |
| primitive | boxed object rải rác (cache miss) |

- Quy tắc thực dụng: **mặc định sequential**; chỉ bật `parallel()` sau khi đo bằng JMH trên phần cứng giống production.
- Khi được hỏi "tăng tốc stream 1 triệu bản ghi", hỏi lại: CPU-bound hay I/O-bound? chạy ở đâu? có cần thứ tự?

**Câu hỏi nối tiếp:**
- *Vì sao `Stream.iterate` gần như không song song được?* — Phần tử sau phụ thuộc phần tử trước; spliterator chỉ tách batch tuần tự.

**⚠️ Câu trả lời gây điểm trừ:**
- "Có nhiều dữ liệu thì dùng parallel".

**📖 Ôn lại:** [Phần 4, mục 4.2](../01-giao-trinh/03-modern-java-8-21.md#p4)

</details>

### Q22. 🔴 🧩 Sau khi deploy endpoint báo cáo mới dùng `productIds.parallelStream().map(id -> priceClient.fetch(id))`, latency của **toàn bộ** service (kể cả endpoint không liên quan) tăng vọt. Phân tích và sửa.

<details><summary>Đáp án</summary>

**Trả lời ngắn:** `priceClient.fetch` là **blocking I/O** (~200 ms). Parallel stream chạy trên `ForkJoinPool.commonPool()` — vài thread **dùng chung toàn JVM** — nên tất cả worker bị block chờ mạng. Mọi parallel stream khác và mọi `CompletableFuture.supplyAsync` không chỉ định executor trong service đều xếp hàng chờ → "đói" toàn cục. Sửa: không dùng parallel stream cho I/O; dùng `CompletableFuture.supplyAsync(..., ioExecutor)` với **executor riêng** có kích thước phù hợp (bulkhead) + timeout, hoặc **virtual threads** (Java 21) — kèm giới hạn đồng thời (Semaphore) để không dội tải sang service giá.

**Giải thích chi tiết:**

```java
List<CompletableFuture<Price>> fs = productIds.stream()
    .map(id -> CompletableFuture.supplyAsync(() -> priceClient.fetch(id), ioExecutor)
                                .orTimeout(2, TimeUnit.SECONDS))
    .toList();
List<Price> prices = fs.stream().map(CompletableFuture::join).toList();

// Java 21
try (var exec = Executors.newVirtualThreadPerTaskExecutor()) { /* submit từng fetch */ }
```

- Tốt hơn nữa: API batch (`fetchAll(ids)`) để giảm số round-trip.
- Cách phát hiện: thread dump thấy các `ForkJoinPool.commonPool-worker-*` đều `WAITING/RUNNABLE` trong socket read; metric latency tăng đồng loạt.

**Câu hỏi nối tiếp:**
- *Dùng `synchronized` trong lambda parallel để sửa race có được không?* — Đúng về kết quả nhưng giết lợi ích song song.

**⚠️ Câu trả lời gây điểm trừ:**
- "Tăng parallelism của commonPool lên 200" — chữa triệu chứng, vẫn chia sẻ pool toàn JVM.

**📖 Ôn lại:** [Phần 4, mục 4.3 — Bẫy 1](../01-giao-trinh/03-modern-java-8-21.md#p4)

</details>

### Q23. 🟡 🔍 Đoạn code sau có vấn đề gì?

```java
List<Integer> result = new ArrayList<>();
IntStream.range(0, 10_000).parallel().forEach(result::add);
System.out.println(result.size());
```

<details><summary>Đáp án</summary>

**Trả lời ngắn:** **Race condition** trên `ArrayList` không thread-safe: kết quả `size` thường < 10.000, có thể có phần tử `null`, hoặc ném `ArrayIndexOutOfBoundsException` khi hai thread cùng grow mảng. Sửa: để stream tự gom: `IntStream.range(0, 10_000).parallel().boxed().toList()` (giữ đúng thứ tự encounter).

**Giải thích chi tiết:**
- Nguyên tắc: lambda trong stream phải **không side-effect** lên state dùng chung; dùng `collect`/`reduce` để framework gộp kết quả theo chunk.
- "Sửa" bằng `Collections.synchronizedList` hay `synchronized` → đúng nhưng tuần tự hóa, và thứ tự phần tử ngẫu nhiên.
- Code dùng `forEach` + side-effect chạy đúng ở sequential nhưng vỡ ngay khi ai đó thêm `.parallel()` — lý do nên tránh từ đầu.

**Câu hỏi nối tiếp:**
- *`parallel().toList()` có giữ thứ tự không?* — Có, giữ encounter order của source có thứ tự.

**⚠️ Câu trả lời gây điểm trừ:**
- "In ra 10000".

**📖 Ôn lại:** [Phần 4, mục 4.3 — Bẫy 2](../01-giao-trinh/03-modern-java-8-21.md#p4)

</details>

### Q24. 🔴 Thủ thuật chạy parallel stream trong `ForkJoinPool` riêng (`pool.submit(() -> list.parallelStream()...).get()`) hoạt động vì sao? Bạn có dùng nó trong code nghiệp vụ không?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Nó "chạy được" vì task con được fork **từ bên trong một worker thread** của pool nào thì chạy trong pool đó, thay vì commonPool. Nhưng đây là **implementation detail**, không được đặc tả đảm bảo — có thể đổi giữa các phiên bản JDK. Dùng tạm cho tool nội bộ thì được (nhớ `shutdown()` pool); code nghiệp vụ nên dùng `ExecutorService`/`CompletableFuture` tường minh, hoặc Java 21 virtual threads/structured concurrency (preview) cho I/O.

**Giải thích chi tiết:**
- `CompletableFuture.supplyAsync(supplier)` không truyền executor cũng dùng commonPool → luôn truyền executor trong code server.
- Đo hiệu năng: JMH với `@Param` kích thước dữ liệu, `@Warmup`, `@Fork`, trả giá trị hoặc dùng `Blackhole` để tránh dead-code elimination.

```java
@Benchmark public long loop()     { long s = 0; for (int x : data) s += x; return s; }
@Benchmark public long parallel() { return Arrays.stream(data).parallel().asLongStream().sum(); }
```

**Câu hỏi nối tiếp:**
- *Pool riêng không shutdown thì sao?* — Rò rỉ thread (worker của ForkJoinPool là daemon nên không chặn JVM thoát, nhưng tạo pool mỗi request là leak tài nguyên).

**⚠️ Câu trả lời gây điểm trừ:**
- Khẳng định đây là API chính thức để "cấu hình thread pool cho parallel stream".

**📖 Ôn lại:** [Phần 4, mục 4.3 — Bẫy 3 & mục 4.4](../01-giao-trinh/03-modern-java-8-21.md#p4)

</details>

---

<a id="nhom-e"></a>
## E. Optional

### Q25. 🟢 `Optional` được thiết kế để làm gì? Kể các method quan trọng theo phiên bản.

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Mục đích hẹp: làm **kiểu trả về** cho method có thể "không có kết quả", buộc caller xử lý trường hợp rỗng. Không nhằm thay mọi `null`. API: Java 8 — `of`, `ofNullable`, `empty`, `map`, `flatMap`, `filter`, `orElse`, `orElseGet`, `orElseThrow(Supplier)`, `ifPresent`; Java 9 — `ifPresentOrElse`, `or`, `stream`; Java 10 — `orElseThrow()` (thay `get()`); Java 11 — `isEmpty()`.

**Giải thích chi tiết:**

```java
String city = findUser(id).map(User::address).map(Address::city).orElse("Không rõ");
List<User> found = ids.stream().map(this::findUser).flatMap(Optional::stream).toList();
Config c = fromEnv(k).or(() -> fromSysProp(k)).or(() -> fromFile(k)).orElseThrow();
```

- Spring Data `findById` trả `Optional<T>` — đúng mục đích.
- `map` với hàm trả `Optional` → `Optional<Optional<T>>`; phải dùng `flatMap`.

**Câu hỏi nối tiếp:**
- *`or` khác `orElseGet`?* — `or` trả `Optional` (chuỗi fallback lazy), `orElseGet` trả giá trị.

**⚠️ Câu trả lời gây điểm trừ:**
- "Optional dùng để loại bỏ NPE khỏi mọi chỗ".

**📖 Ôn lại:** [Phần 5, mục 5.1](../01-giao-trinh/03-modern-java-8-21.md#p5)

</details>

### Q26. 🟢 🔍 Đoạn code sau in "tính default tốn kém" mấy lần?

```java
Optional<User> u = findUser(1L);            // giả sử có user
u.orElse(expensiveDefault());               // (1)
u.orElseGet(() -> expensiveDefault());      // (2)
```

<details><summary>Đáp án</summary>

**Trả lời ngắn:** **Một lần** — ở (1). Tham số của `orElse` là biểu thức thường nên **luôn được đánh giá** trước khi gọi method, kể cả khi Optional có giá trị. `orElseGet` nhận `Supplier` → chỉ chạy khi rỗng.

**Giải thích chi tiết:**
- Bug thực tế: `repo.findByEmail(e).orElse(repo.save(new User(e)))` → **luôn insert** user mới (hoặc vi phạm unique constraint) dù user đã tồn tại.
- `orElse` phù hợp với hằng số/giá trị đã có sẵn; có chi phí hoặc side-effect → `orElseGet`.

**Câu hỏi nối tiếp:**
- *Tương tự với `Map.getOrDefault` và `computeIfAbsent`?* — `getOrDefault(k, expensive())` cũng luôn đánh giá tham số; `computeIfAbsent` thì lazy.

**⚠️ Câu trả lời gây điểm trừ:**
- "Không lần nào vì đã có user".

**📖 Ôn lại:** [Phần 5, mục 5.1 — orElse vs orElseGet](../01-giao-trinh/03-modern-java-8-21.md#p5)

</details>

### Q27. 🟡 Kể các anti-pattern khi dùng `Optional` và cách sửa.

<details><summary>Đáp án</summary>

**Trả lời ngắn:**
- `if (opt.isPresent()) return opt.get();` — chỉ là null-check dài hơn → `map/orElse/orElseThrow`.
- `Optional` làm **field** — không `Serializable`, thêm object, JPA/Jackson lằng nhằng → field nullable + getter trả `Optional`.
- `Optional` làm **tham số** — caller phải bọc, vẫn truyền được `null` cho chính Optional → overload.
- `Optional<List<T>>` — hai cách biểu diễn "không có" → trả list rỗng (Item 54).
- `Optional.of(x)` khi x có thể null → NPE ngay; dùng `ofNullable`.
- **Trả `null`** từ method kiểu `Optional` — phá hợp đồng.
- `orElse(createEntity())` → `orElseGet`.
- `Optional<Integer>` trong hot path → `OptionalInt`.
- Dùng Optional làm key Map, `==`, `synchronized` — Optional là **value-based class**.

**Giải thích chi tiết:**

```java
List<Order> findOrders(String status);   // thay vì Optional<List<Order>> findOrders(Optional<String>)
private String note;
public Optional<String> getNote() { return Optional.ofNullable(note); }
```

**Câu hỏi nối tiếp:**
- *Optional trong DTO JSON?* — Jackson hỗ trợ qua module `jdk8` nhưng làm contract mơ hồ (null vs absent); ở biên hệ thống nên hạn chế.

**⚠️ Câu trả lời gây điểm trừ:**
- Không nêu được lý do (chỉ liệt kê "đừng làm").

**📖 Ôn lại:** [Phần 5, mục 5.2 — Anti-pattern](../01-giao-trinh/03-modern-java-8-21.md#p5)

</details>

### Q28. 🔴 "Optional có giải quyết được NPE không?" Ở quy mô codebase lớn, bạn xử lý null thế nào?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Không. Optional giúp **thiết kế API rõ ràng hơn ở kiểu trả về** ("có thể không có"), không phải cơ chế loại bỏ null — field, tham số, collection, dữ liệu từ DB/JSON vẫn có thể null. Ở quy mô lớn: annotation nullness (`@Nullable`/`@NonNull` — chuẩn **JSpecify**) + static analysis (**NullAway**, Error Prone, IntelliJ inspections) trong CI; validate ở biên (Bean Validation, `Objects.requireNonNull` trong constructor/compact constructor); trả collection rỗng thay vì null.

**Giải thích chi tiết:**
- `Optional` là class `final` với field `value`; `empty()` là singleton; mỗi `Optional.of` là một allocation (thường bị escape analysis loại bỏ nếu cục bộ, nhưng không đảm bảo).
- Value-based class: Project Valhalla có thể biến nó thành value class không identity → code dựa vào `==`/`synchronized`/`identityHashCode` trên Optional sẽ hỏng.
- Java 14+ Helpful NPE (JEP 358) giúp debug nhanh hơn nhưng không phòng ngừa.

**Câu hỏi nối tiếp:**
- *Kotlin xử lý khác gì?* — Nullability nằm trong hệ thống kiểu (`String?`), compiler bắt buộc kiểm tra — điều Java đang tiến tới bằng annotation + tooling.

**⚠️ Câu trả lời gây điểm trừ:**
- "Dùng Optional ở mọi nơi là hết NPE".

**📖 Ôn lại:** [Phần 5, mục 5.3 & Góc nhìn Senior](../01-giao-trinh/03-modern-java-8-21.md#p5)

</details>

---

<a id="nhom-f"></a>
## F. java.time, time zone, DST

### Q29. 🟢 Vì sao `java.util.Date`, `Calendar`, `SimpleDateFormat` bị thay thế?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** `Date` **mutable** (`setTime`) → không an toàn khi chia sẻ, phải defensive copy; `SimpleDateFormat` **không thread-safe** → khai báo `static final` dùng chung trong web app làm ngày tháng bị trộn giữa các request, thậm chí `NumberFormatException` ngẫu nhiên; API khó dùng (tháng từ 0, năm từ 1900, `Date` là instant nhưng `toString` in theo time zone mặc định). `java.time` (JSR-310, Java 8): **immutable, thread-safe**, tách bạch rõ các khái niệm (instant, ngày, giờ địa phương, zone, khoảng thời gian).

**Giải thích chi tiết:**
- `DateTimeFormatter` thread-safe → để `static final` thoải mái.
- Code cũ giao tiếp qua `Date.from(instant)`, `date.toInstant()`, `Timestamp.from/toInstant`.

**Câu hỏi nối tiếp:**
- *Sửa nhanh code cũ dùng `SimpleDateFormat` static?* — Chuyển sang `DateTimeFormatter`; tạm thời dùng `ThreadLocal<SimpleDateFormat>` (nhớ vấn đề ThreadLocal với thread pool).

**⚠️ Câu trả lời gây điểm trừ:**
- Không biết `SimpleDateFormat` không thread-safe.

**📖 Ôn lại:** [Phần 6, mục 6.1](../01-giao-trinh/03-modern-java-8-21.md#p6)

</details>

### Q30. 🟡 Chọn kiểu `java.time` cho từng trường hợp: (a) thời điểm tạo đơn hàng; (b) ngày sinh; (c) cuộc họp 9h sáng giờ Berlin tháng sau; (d) trao đổi timestamp qua REST API; (e) timeout 30 giây; (f) "gia hạn thêm 1 tháng".

<details><summary>Đáp án</summary>

**Trả lời ngắn:**
- (a) **`Instant`** — điểm trên trục UTC; lưu DB dạng `timestamptz`/UTC.
- (b) **`LocalDate`** — không có giờ, không zone.
- (c) **`LocalDateTime` + `ZoneId`** lưu riêng, tính ra `Instant` khi cần — vì luật DST/time zone có thể thay đổi trước ngày họp.
- (d) **`OffsetDateTime`/`Instant`** dạng ISO-8601 có offset (`2024-03-31T03:30:00+02:00` hoặc `...Z`).
- (e) **`Duration`** (giây/nano).
- (f) **`Period`** (năm/tháng/ngày theo lịch).

**Giải thích chi tiết:**
- `LocalDateTime` **không** phải một thời điểm cụ thể ("8h sáng ở bất cứ đâu") — dùng làm timestamp sự kiện là bug.
- `ZonedDateTime` = `LocalDateTime` + `ZoneId` có luật DST — dùng để hiển thị/tính theo lịch một vùng.
- `ZoneId.of("Asia/Ho_Chi_Minh")` (vùng có luật) khác `ZoneOffset.of("+07:00")` (độ lệch cố định) — vùng có DST phải dùng `ZoneId`.
- `withZoneSameInstant` (cùng thời điểm) khác `withZoneSameLocal` (cùng giờ đồng hồ, khác thời điểm).

**Câu hỏi nối tiếp:**
- *Lãi theo ngày vs SLA "phản hồi trong 4 giờ"?* — Lãi theo ngày → `LocalDate`/`Period`; SLA → `Instant`/`Duration`.

**⚠️ Câu trả lời gây điểm trừ:**
- Dùng `LocalDateTime` cho mọi thứ.

**📖 Ôn lại:** [Phần 6, mục 6.2 & 6.4](../01-giao-trinh/03-modern-java-8-21.md#p6)

</details>

### Q31. 🔴 🔍 DST gap và overlap là gì? Đoạn code sau in ra gì?

```java
ZoneId berlin = ZoneId.of("Europe/Berlin");
ZonedDateTime gap = ZonedDateTime.of(LocalDateTime.of(2024, 3, 31, 2, 30), berlin);
System.out.println(gap);
ZonedDateTime before = ZonedDateTime.of(LocalDateTime.of(2024, 3, 30, 12, 0), berlin);
System.out.println(before.plusDays(1));
System.out.println(before.plus(Duration.ofDays(1)));
ZonedDateTime overlap = ZonedDateTime.of(LocalDateTime.of(2024, 10, 27, 2, 30), berlin);
System.out.println(overlap.withLaterOffsetAtOverlap());
```

<details><summary>Đáp án</summary>

**Trả lời ngắn:**
- `2024-03-31T03:30+02:00[Europe/Berlin]` — **gap** (spring forward 02:00 → 03:00): 02:30 không tồn tại, Java **dời tới** theo độ dài gap.
- `2024-03-31T12:00+02:00[...]` — `plusDays(1)` giữ **cùng giờ đồng hồ** (thực tế chỉ 23 giờ trôi qua).
- `2024-03-31T13:00+02:00[...]` — `plus(Duration.ofDays(1))` cộng **đúng 24 giờ thực**.
- `2024-10-27T02:30+01:00[...]` — **overlap** (fall back 03:00 → 02:00): 02:30 xảy ra hai lần; mặc định chọn offset **sớm hơn** (+02:00), `withLaterOffsetAtOverlap()` chọn lần thứ hai (+01:00).

**Giải thích chi tiết:**
- Hệ quả production: tính "số giờ làm việc", phí theo giờ bằng `LocalDateTime` → sai 1 giờ vào ngày chuyển DST; phải tính trên `Instant`/`ZonedDateTime`.
- Việt Nam không có DST, nhưng hệ thống phục vụ khách EU/US/Úc chắc chắn gặp.
- Luật time zone thay đổi theo chính trị → JDK mang tzdata, cần cập nhật JDK (hoặc TZUpdater).

**Câu hỏi nối tiếp:**
- *`Period.ofDays(1)` cộng vào `ZonedDateTime` giống cái nào?* — Giống `plusDays(1)` (theo lịch).

**⚠️ Câu trả lời gây điểm trừ:**
- Cho rằng tạo 02:30 trong gap sẽ ném exception, hoặc `plusDays(1)` luôn bằng 24 giờ.

**📖 Ôn lại:** [Phần 6, mục 6.3 — DST gap và overlap](../01-giao-trinh/03-modern-java-8-21.md#p6)

</details>

### Q32. 🟡 🧩 "Chạy local đúng, lên server lệch 7 tiếng." Nguyên nhân thường gặp và nguyên tắc lưu trữ/truyền tải thời gian?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Code phụ thuộc **time zone mặc định của JVM** (`TimeZone.getDefault()`, `LocalDateTime.now()`, `Date.toString()`, driver JDBC chuyển đổi theo zone mặc định): máy dev ở +07, container Docker mặc định UTC. Nguyên tắc: **lưu thời điểm ở UTC** (`Instant`, `timestamptz`), chuyển sang zone người dùng ở **tầng hiển thị**; đặt rõ `-Duser.timezone=UTC` cho mọi môi trường; không dùng `LocalDateTime.now()` cho timestamp nghiệp vụ; JSON dùng ISO-8601 có offset.

**Giải thích chi tiết:**
- JDBC 4.2 hỗ trợ `LocalDate`, `LocalDateTime`, `OffsetDateTime` qua `setObject/getObject`; không phải driver nào cũng hỗ trợ `ZonedDateTime`/`Instant` trực tiếp.
- Jackson cần `jackson-datatype-jsr310` (Spring Boot tự cấu hình) và tắt `WRITE_DATES_AS_TIMESTAMPS`.
- Hibernate 6: cấu hình `hibernate.jdbc.time_zone=UTC` để chuẩn hóa.
- Sự kiện tương lai theo lịch địa phương: lưu `LocalDateTime` + `ZoneId`.

**Câu hỏi nối tiếp:**
- *So sánh `createdAt` do node khác ghi với `Instant.now()` của node mình có an toàn?* — Clock skew giữa node (NTP) có thể vài trăm ms tới vài giây; dùng một nguồn thời gian duy nhất (ví dụ `now()` của DB trong câu query).

**⚠️ Câu trả lời gây điểm trừ:**
- "Cộng thêm 7 giờ khi đọc".

**📖 Ôn lại:** [Phần 6, mục 6.4 — Lưu trữ và truyền tải](../01-giao-trinh/03-modern-java-8-21.md#p6)

</details>

### Q33. 🟡 Làm sao test được logic "hết hạn sau 30 ngày"? Đo latency nên dùng `currentTimeMillis` hay `nanoTime`?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Inject **`java.time.Clock`** thay vì gọi `Instant.now()` trực tiếp: production dùng `Clock.systemUTC()`, test dùng `Clock.fixed(...)` hoặc `Clock.offset(...)` → test deterministic. Đo **khoảng thời gian** dùng **`System.nanoTime()`** (monotonic); `currentTimeMillis`/`Instant.now()` là **wall clock**, có thể nhảy lùi khi NTP đồng bộ.

**Giải thích chi tiết:**

```java
public boolean isExpired(Instant startedAt, Duration validity) {
    return Instant.now(clock).isAfter(startedAt.plus(validity));
}
// test
var svc = new SubscriptionService(Clock.fixed(Instant.parse("2024-02-01T00:00:00Z"), ZoneOffset.UTC));
```

- Spring: `@Bean Clock clock() { return Clock.systemUTC(); }`, test override bằng bean `Clock.fixed`.
- `nanoTime` chỉ so sánh được **trong cùng một JVM** — không dùng để so giữa các node.
- Bẫy precision: `Instant.now()` có micro/nano-giây; MySQL `DATETIME` mặc định chỉ giây, Postgres/Oracle micro-giây → đọc lại từ DB rồi `equals` thất bại trong test → `truncatedTo(ChronoUnit.MICROS)` trước khi lưu.

**Câu hỏi nối tiếp:**
- *Tính tuổi?* — `Period.between(birth, LocalDate.now(clock)).getYears()`; test sinh ngày 29/02.

**⚠️ Câu trả lời gây điểm trừ:**
- Test bằng `Thread.sleep` hoặc mock static `Instant.now()` khắp nơi.

**📖 Ôn lại:** [Phần 6, mục 6.5 — `Clock`](../01-giao-trinh/03-modern-java-8-21.md#p6)

</details>

### Q34. 🔴 🧩 Job "hàng ngày lúc 02:30 giờ địa phương" cho khách hàng ở Berlin: có ngày không chạy, có ngày chạy hai lần. Vì sao và thiết kế lại thế nào?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Ngày **gap** (31/03) 02:30 không tồn tại → scheduler kiểu "mỗi phút kiểm tra giờ địa phương == 02:30" bỏ qua; ngày **overlap** (27/10) 02:30 xảy ra hai lần → chạy hai lần. Thiết kế lại: tính trước **danh sách `Instant`** cần chạy, mỗi ngày đúng **một** `ZonedDateTime.of(date, at, zone)` (gap tự dời sau gap, overlap chọn offset sớm hơn) → không bỏ, không lặp; hoặc lập lịch theo **UTC**; hoặc tránh khung 01:00–03:00; và làm job **idempotent** (khóa theo `jobName + businessDate`) để chạy lặp cũng vô hại.

**Giải thích chi tiết:**

```java
static List<Instant> nextRuns(LocalTime at, ZoneId zone, LocalDate from, int days) {
    return from.datesUntil(from.plusDays(days))
        .map(d -> ZonedDateTime.of(d, at, zone))
        .map(ZonedDateTime::toInstant)
        .toList();
}
```

- Hành vi DST của từng scheduler (Spring `@Scheduled(cron, zone)`, Quartz, Kubernetes CronJob `timeZone`) khác nhau — phải đọc tài liệu và **viết test cho ngày chuyển DST**.
- Nhiều instance cùng chạy job → cần distributed lock (ShedLock) để không chạy trùng giữa các node.

**Câu hỏi nối tiếp:**
- *Job báo cáo "doanh thu ngày" cho khách nhiều zone?* — Xác định "ngày" theo zone nào (khách, công ty, UTC) là quyết định nghiệp vụ; ngày có DST dài 23 hoặc 25 giờ.

**⚠️ Câu trả lời gây điểm trừ:**
- "Việt Nam không có DST nên không cần quan tâm".

**📖 Ôn lại:** [Phần 6, mục 6.3 & bài 6.2](../01-giao-trinh/03-modern-java-8-21.md#p6)

</details>

### Q35. 🟡 🔍 Đoạn code sau in ra gì?

```java
System.out.println(LocalDate.of(2024, 1, 31).plusMonths(1));
System.out.println(LocalDate.of(2024, 12, 30).format(DateTimeFormatter.ofPattern("YYYY-MM-dd")));
System.out.println(ChronoUnit.DAYS.between(LocalDate.of(2024, 1, 1), LocalDate.of(2025, 1, 1)));
```

<details><summary>Đáp án</summary>

**Trả lời ngắn:** `2024-02-29` (kẹp về ngày cuối tháng, không ném lỗi), `2025-12-30` (**`YYYY` là week-based-year** — 30/12/2024 thuộc tuần 1 của 2025), `366` (2024 năm nhuận).

**Giải thích chi tiết:**
- Pattern dễ nhầm: `yyyy` (năm) vs `YYYY` (week-based-year); `HH` (0–23) vs `hh` (1–12, cần `a`); `mm` (phút) vs `MM` (tháng); `dd` vs `DD` (ngày trong năm).
- Format tên tháng/thứ cần chỉ định `Locale`, nếu không phụ thuộc locale mặc định của máy.
- Bug `YYYY` thường chỉ lộ ra vài ngày cuối tháng 12 — kiểu bug "Twitter/Apple ngày 31/12" nổi tiếng.

**Câu hỏi nối tiếp:**
- *`LocalDate.of(2024, 1, 31).plusMonths(1).plusMonths(1)` có bằng `plusMonths(2)`?* — Không: lần lượt ra `2024-03-29` vs `2024-03-31` — cộng tháng không có tính kết hợp.

**⚠️ Câu trả lời gây điểm trừ:**
- Trả lời `2024-12-30` cho dòng hai.

**📖 Ôn lại:** [Phần 6, mục 6.2 & Lỗi thường gặp](../01-giao-trinh/03-modern-java-8-21.md#p6)

</details>

---

<a id="nhom-g"></a>
## G. `var`, switch expression, text block

### Q36. 🟢 `var` có phải dynamic typing không? Dùng được và không dùng được ở đâu?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Không. `var` (Java 10, JEP 286) là **suy luận kiểu lúc compile** cho biến local — kiểu được cố định, không đổi được. `var` là *reserved type name*, không phải keyword. Dùng được: biến local có initializer, biến for/for-each, try-with-resources, tham số lambda (Java 11, để gắn annotation). Không dùng được: field, tham số method, kiểu trả về, biến không initializer, `var x = null`, `var a = {1, 2}`, `var f = () -> 1` (lambda không có target type).

**Giải thích chi tiết:**

```java
var map = new HashMap<String, List<Integer>>();  // ✅ tránh lặp kiểu dài
var list = new ArrayList<>();                     // ⚠️ ArrayList<Object>!
var x = flag ? 1 : "a";                           // intersection type (Serializable & Comparable<...> ...)
```

- `var` suy ra kiểu **cụ thể** (`ArrayList`), không phải interface (`List`) — thường không sao với biến local.
- Quy ước team: dùng khi kiểu hiển nhiên từ vế phải (`new`, factory tên rõ) hoặc kiểu quá dài; tránh khi vế phải là method call mơ hồ.

**Câu hỏi nối tiếp:**
- *`var` trong lambda để làm gì?* — Gắn annotation: `(@NonNull var x) -> ...`.

**⚠️ Câu trả lời gây điểm trừ:**
- "var giống JavaScript, đổi kiểu được".

**📖 Ôn lại:** [Phần 7, mục 7.1](../01-giao-trinh/03-modern-java-8-21.md#p7)

</details>

### Q37. 🟡 Switch expression (Java 14) khác switch statement cổ điển thế nào? Vì sao bỏ `default` khi switch trên enum lại an toàn hơn?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Switch expression **trả giá trị**; arrow form `->` **không fall-through**, không cần `break`; nhiều label một case (`case SAT, SUN ->`); block dùng `yield` để trả giá trị; bắt buộc **exhaustive**. Với enum đủ case, **không viết `default`** thì khi thêm hằng mới, compiler **báo lỗi** ở mọi switch chưa xử lý; còn runtime gặp hằng lạ (enum compile lại riêng) thì nhánh ẩn ném **`MatchException`** (Java 21; trước đó `IncompatibleClassChangeError`). Viết `default` sẽ nuốt mất kiểm tra này.

**Giải thích chi tiết:**

```java
return switch (d) {
    case MON, TUE, WED, THU -> 8;
    case FRI -> { int base = 8; yield base - 2; }
    case SAT, SUN -> 0;
};
```

- Arrow dùng được cả trong switch statement.
- Switch cổ điển trên `String` dùng `hashCode()` + `equals()`; selector `null` ném NPE (trừ khi có `case null` từ Java 21).

**Câu hỏi nối tiếp:**
- *Thêm hằng enum vào thư viện dùng chung có rủi ro gì?* — Client có switch exhaustive chưa recompile gặp `MatchException` lúc runtime; client đã recompile thì lỗi compile — đều là breaking change.

**⚠️ Câu trả lời gây điểm trừ:**
- "Luôn thêm `default` cho chắc".

**📖 Ôn lại:** [Phần 7, mục 7.2 — Switch expression](../01-giao-trinh/03-modern-java-8-21.md#p7)

</details>

### Q38. 🟢 🔍 Hai text block sau có giá trị gì? Dùng text block + `formatted()` để ghép SQL có ổn không?

```java
String a = """
    Hello
      World
    """;
String b = """
    SELECT * \
    FROM t""";
```

<details><summary>Đáp án</summary>

**Trả lời ngắn:** `a = "Hello\n  World\n"` (độ dài 14) — lề chung nhỏ nhất (kể cả dòng chứa `"""` đóng) bị cắt, giữ thụt lề tương đối, và kết thúc bằng `\n` vì `"""` đóng nằm ở dòng riêng. `b = "SELECT * FROM t"` — `\` cuối dòng nối dòng không thêm `\n`, và không có `\n` cuối vì `"""` đóng ngay sau nội dung. Ghép SQL bằng `formatted()` với input người dùng là **SQL injection** — text block chỉ là `String` literal; phải dùng `PreparedStatement`/bind parameter.

**Giải thích chi tiết:**
- Nội dung bắt đầu từ dòng **sau** `"""` mở (đặt nội dung cùng dòng là lỗi compile).
- Line terminator chuẩn hóa thành `\n` bất kể OS; trailing whitespace mỗi dòng bị strip — dùng `\s` để giữ.
- String Templates (JEP 430, preview Java 21–22) bị **rút lại** ở Java 23 — ý tưởng là buộc ghép chuỗi đi qua "processor" có thể validate/escape; cho tới nay vẫn phải tự escape/bind.

**Câu hỏi nối tiếp:**
- *Text block có được intern không?* — Có, là compile-time constant như literal thường.

**⚠️ Câu trả lời gây điểm trừ:**
- "Text block giữ nguyên mọi khoảng trắng như trong source".

**📖 Ôn lại:** [Phần 7, mục 7.3 — Text block](../01-giao-trinh/03-modern-java-8-21.md#p7)

</details>

---

<a id="nhom-h"></a>
## H. Records & sealed classes

### Q39. 🟢 Khai báo `record Point(int x, int y) {}` thì compiler sinh ra những gì? Record có ràng buộc gì?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Class **`final`** kế thừa `java.lang.Record`; field `private final` cho mỗi component; **canonical constructor**; accessor **`x()`, `y()`** (không phải `getX()`); `equals`, `hashCode`, `toString` dựa trên **mọi** component. Ràng buộc: không extends class khác (implement interface được); không khai báo instance field ngoài component (static được); không instance initializer; constructor phụ phải gọi `this(...)` tới canonical.

**Giải thích chi tiết:**
- Thêm được method, static factory, nested type, implement `Comparable`.
- Local record (trong method) tiện cho kết quả trung gian của stream; local record/enum/interface ngầm `static` → không capture biến local hay `this`.
- Accessor `x()` có thể làm thư viện cũ dựa trên JavaBeans (EL, BeanUtils) không nhận.

**Câu hỏi nối tiếp:**
- *Record có phải "data class" của Kotlin/Lombok `@Value`?* — Gần giống về mục đích, nhưng record là *nominal tuple* với ngữ nghĩa ngôn ngữ (deconstruction trong pattern matching, serialization qua canonical constructor).

**⚠️ Câu trả lời gây điểm trừ:**
- "Record sinh getter `getX()` và setter".

**📖 Ôn lại:** [Phần 8, mục 8.1 — Records](../01-giao-trinh/03-modern-java-8-21.md#p8)

</details>

### Q40. 🟡 Compact constructor hoạt động thế nào? Record có thật sự immutable không?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Compact constructor (`public Money { ... }`) không khai báo tham số, chạy **trước** khi field được gán tự động; dùng để **validate và chuẩn hóa** — gán lại tham số (`currency = currency.toUpperCase(Locale.ROOT)`) thì giá trị mới được dùng để gán field; **không** được viết `this.x = ...`. Record chỉ **bất biến nông**: component là `List`/`Date`/mảng thì object bên trong vẫn sửa được → defensive copy (`List.copyOf`) trong compact constructor; mảng thì clone khi vào/ra và override `equals/hashCode` (mặc định so mảng bằng reference).

**Giải thích chi tiết:**

```java
public record Money(String currency, long amountMinor) {
    public Money {
        Objects.requireNonNull(currency, "currency");
        currency = currency.toUpperCase(Locale.ROOT);
        if (amountMinor < 0) throw new IllegalArgumentException("amount < 0");
    }
    public Money plus(Money o) { /* check currency */ return new Money(currency, Math.addExact(amountMinor, o.amountMinor)); }
}
record SafeTeam(String name, List<String> members) { SafeTeam { members = List.copyOf(members); } }
```

- `List.copyOf` ném NPE nếu có phần tử null — thường là điều mong muốn ở value object.
- Record làm key `HashMap` rất tốt (equals/hashCode chuẩn) — miễn component bất biến.

**Câu hỏi nối tiếp:**
- *Override accessor được không?* — Được (ví dụ trả `signature.clone()` cho mảng), phải giữ kiểu trả về và `public`.

**⚠️ Câu trả lời gây điểm trừ:**
- "Record immutable hoàn toàn nên không cần copy".

**📖 Ôn lại:** [Phần 8, mục 8.1 — Giới hạn của immutability](../01-giao-trinh/03-modern-java-8-21.md#p8)

</details>

### Q41. 🟡 Dùng record ở đâu trong một ứng dụng Spring Boot + JPA? Record có làm JPA entity được không?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Rất hợp cho **DTO request/response, value object, projection, event/message, kết quả trả về nhiều giá trị, key của Map, `@ConfigurationProperties`** (Boot 2.6+ hỗ trợ constructor binding với record). **Không** làm JPA entity được: entity cần no-arg constructor, mutable, class không `final` (Hibernate tạo proxy để lazy load). Nhưng record dùng tốt cho **projection** (`SELECT new com.x.Dto(...)`, Spring Data class projection) và `@Embeddable` (Hibernate 6.2+).

**Giải thích chi tiết:**
- Jackson hỗ trợ record từ 2.12 (Boot 2.5+).
- Java serialization: record deserialize **qua canonical constructor** → validation trong compact constructor được thực thi — an toàn hơn class thường (deserialization bỏ qua constructor).
- Record với Bean Validation: đặt annotation lên component (`record CreateUser(@NotBlank String name, @Email String email) {}`).

**Câu hỏi nối tiếp:**
- *MapStruct với record?* — Hỗ trợ (dùng canonical constructor để tạo target).

**⚠️ Câu trả lời gây điểm trừ:**
- "Thay toàn bộ entity bằng record cho gọn".

**📖 Ôn lại:** [Phần 8, mục 8.1 — Records trong hệ sinh thái](../01-giao-trinh/03-modern-java-8-21.md#p8)

</details>

### Q42. 🟡 Nêu các luật của sealed class (Java 17).

<details><summary>Đáp án</summary>

**Trả lời ngắn:** `sealed class/interface X permits A, B, C` giới hạn tập subclass trực tiếp. Mỗi subclass được permit **phải** khai báo một trong: **`final`**, **`sealed`** (tiếp tục giới hạn), hoặc **`non-sealed`** (mở lại cho kế thừa tự do). Subclass phải nằm **cùng module** (named module) hoặc **cùng package** (unnamed module — trường hợp classpath thông thường). Nếu mọi subclass cùng file thì bỏ được `permits`. Reflection: `Class.isSealed()`, `getPermittedSubclasses()`. Record ngầm `final` nên là subclass permit tự nhiên.

**Giải thích chi tiết:**
- Lợi ích chính: compiler biết **tập đóng** các kiểu → switch pattern kiểm tra **exhaustive** không cần `default`; thêm subtype mới → mọi switch chưa xử lý **lỗi compile** → refactor an toàn.
- Khác `final` class (không ai kế thừa) và package-private constructor (giới hạn mềm, compiler không biết tập đóng).
- Thay đổi `permits` trong thư viện public là **breaking change** cho client switch exhaustive.

**Câu hỏi nối tiếp:**
- *Sealed ở Java 17 nhưng pattern switch ở Java 17 thì sao?* — Pattern matching cho switch chỉ là **preview** ở 17 (final ở 21); ở 17 lợi ích exhaustiveness chủ yếu qua tài liệu và `instanceof`.

**⚠️ Câu trả lời gây điểm trừ:**
- Không biết subclass bắt buộc khai báo `final/sealed/non-sealed`.

**📖 Ôn lại:** [Phần 8, mục 8.2 — Sealed classes](../01-giao-trinh/03-modern-java-8-21.md#p8)

</details>

### Q43. 🔴 🧩 Payment gateway trả về kết quả Approved / Declined / RequiresAction / Failed. Bạn mô hình hóa bằng sealed result type hay exception? Trade-off?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Kết quả **nghiệp vụ dự kiến** (Declined là chuyện bình thường, RequiresAction cần redirect 3-DS) → **sealed result type** + records: buộc caller xử lý **mọi nhánh** (switch exhaustive), không tốn chi phí stack trace, dữ liệu mỗi nhánh có kiểu rõ ràng. **Exception** dành cho tình huống **bất thường** mà tầng hiện tại không xử lý được (mất kết nối, lỗi lập trình) và cần lan truyền lên trên/rollback transaction.

**Giải thích chi tiết:**

```java
sealed interface PaymentResult permits Approved, Declined, RequiresAction, Failed {}
record Approved(String txId, Money amount) implements PaymentResult {}
record Declined(String reason, int code) implements PaymentResult {}
record RequiresAction(URI redirect) implements PaymentResult {}
record Failed(Throwable cause) implements PaymentResult {}
```

- Thêm `Pending` → compiler chỉ ra mọi chỗ cần xử lý — đó là giá trị lớn nhất.
- Trade-off: result type làm chữ ký "ồn" hơn và có thể bị bỏ qua nếu caller không dùng giá trị trả về; exception tự lan truyền và tích hợp sẵn với `@Transactional` rollback và `@ControllerAdvice`.
- Kết hợp thực tế: domain trả result type; tầng adapter dịch lỗi kỹ thuật thành `Failed` hoặc ném exception tùy chính sách retry.

**Câu hỏi nối tiếp:**
- *Ở Java 17 (chưa có pattern switch final) dùng result type thế nào?* — Method ảo trên interface (`fold`/visitor) hoặc chuỗi `instanceof` pattern.

**⚠️ Câu trả lời gây điểm trừ:**
- "Luôn dùng exception" hoặc "luôn dùng result type" mà không nêu tiêu chí.

**📖 Ôn lại:** [Phần 8 — bài 8.3 & Góc nhìn Senior](../01-giao-trinh/03-modern-java-8-21.md#p8)

</details>

### Q44. 🟢 Sealed interface và enum khác nhau thế nào? Khi nào dùng cái nào?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Enum là tập **instance** cố định — mỗi hằng là một singleton, cùng một bộ field. Sealed là tập **kiểu** cố định — mỗi kiểu có thể có **nhiều instance** mang **dữ liệu khác nhau** (Approved có `txId`, Declined có `reason`). Dùng enum cho trạng thái/loại không mang dữ liệu riêng (`OrderStatus`); dùng sealed cho biến thể mang dữ liệu (kết quả, sự kiện, AST, command).

**Giải thích chi tiết:**
- Cả hai đều cho switch exhaustive không cần `default`.
- Enum có `EnumSet/EnumMap`, `values()`, serialize theo tên — tiện cho persistence; sealed + record cần chiến lược serialize polymorphic (Jackson `@JsonTypeInfo` + `@JsonSubTypes`).
- Có thể kết hợp: enum implement sealed interface.

**Câu hỏi nối tiếp:**
- *Sealed type qua JSON giữa các service?* — Cần type discriminator và consumer chịu được loại lạ — giống vấn đề thêm hằng enum.

**⚠️ Câu trả lời gây điểm trừ:**
- "Sealed là enum kiểu mới".

**📖 Ôn lại:** [Phần 8, mục 8.2 — Góc nhìn Senior](../01-giao-trinh/03-modern-java-8-21.md#p8)

</details>

---

<a id="nhom-i"></a>
## I. Pattern matching

### Q45. 🟢 Pattern matching cho `instanceof` (Java 16) là gì? "Flow scoping" nghĩa là gì?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** `if (obj instanceof String s && s.length() > 3)` — kiểm tra kiểu, cast và bind biến trong một bước. Biến pattern chỉ có phạm vi ở nơi compiler **chắc chắn** pattern đã match (flow scoping): `if (!(obj instanceof String s)) return; s.toUpperCase();` — `s` vẫn dùng được sau `if` vì nhánh không match đã return.

**Giải thích chi tiết:**

```java
@Override public boolean equals(Object o) {
    return o instanceof Point p && x == p.x && y == p.y;
}
```

- `obj instanceof String s || s.isEmpty()` → lỗi compile (`s` không chắc đã bind).
- Java 21: record pattern dùng được với `instanceof`: `if (o instanceof Point(int x, int y))`.

**Câu hỏi nối tiếp:**
- *`null instanceof String s`?* — `false`, không NPE.

**⚠️ Câu trả lời gây điểm trừ:**
- Không biết phạm vi của biến pattern.

**📖 Ôn lại:** [Phần 9, mục 9.1](../01-giao-trinh/03-modern-java-8-21.md#p9)

</details>

### Q46. 🟡 🔍 Method sau có compile không? Nếu sửa cho compile thì `describe(null)`, `describe(500)`, `describe(new StringBuilder("ab"))` trả gì?

```java
static String describe(Object o) {
    return switch (o) {
        case CharSequence cs        -> "chars " + cs.length();
        case String s               -> "string " + s;
        case Integer i when i > 100 -> "big";
        case Integer i              -> "int";
        default                     -> "other";
    };
}
```

<details><summary>Đáp án</summary>

**Trả lời ngắn:** **Không compile** — vi phạm **dominance**: `case String s` bị `case CharSequence cs` (tổng quát hơn) che hoàn toàn. Sửa: đặt `case String s` **trước** `CharSequence`. Sau khi sửa: `describe(null)` → **`NullPointerException`** (không có `case null` — giữ tương thích với switch cũ); `describe(500)` → `"big"`; `describe(new StringBuilder("ab"))` → `"chars 2"`.

**Giải thích chi tiết:**
- Thứ tự `case Integer i when i > 100` trước `case Integer i` là đúng; đảo lại cũng là lỗi dominance.
- Xử lý null tường minh: `case null -> "null"` hoặc `case null, default -> "other"`.
- Guard `when` dùng được biến pattern (chạy sau khi bind).
- Switch có pattern (kể cả statement) phải exhaustive — ở đây nhờ `default`.

**Câu hỏi nối tiếp:**
- *Compile với `--release 17` được không?* — Không; pattern switch chỉ là preview ở 17.

**⚠️ Câu trả lời gây điểm trừ:**
- Nói compile được và `describe("x")` trả `"chars 1"`.

**📖 Ôn lại:** [Phần 9, mục 9.2 — Quy tắc cần nắm](../01-giao-trinh/03-modern-java-8-21.md#p9)

</details>

### Q47. 🔴 Record pattern lồng nhau hoạt động thế nào? Switch pattern được compile ra gì, và `MatchException` xuất hiện khi nào?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Record pattern (Java 21, JEP 440) **phân rã** record qua accessor: `case Rect(double w, double h) when w == h -> w * w`; lồng được: `case Line(Point(var x1, var y1), Point(var x2, var y2))`. Switch pattern compile thành **`invokedynamic`** với bootstrap `SwitchBootstraps.typeSwitch` trả chỉ số case match đầu tiên, rồi `tableswitch` — JVM cache theo class thay vì chuỗi `instanceof` tuyến tính. `MatchException` khi runtime gặp **subtype không biết** (sealed hierarchy được compile lại riêng với subtype mới mà switch chưa recompile), hoặc accessor của record ném exception trong lúc phân rã.

**Giải thích chi tiết:**

```java
static Expr simplify(Expr e) {
    return switch (e) {
        case Add(Num(var z), var r) when z == 0     -> simplify(r);
        case Mul(Num(var one), var r) when one == 1 -> simplify(r);
        case Neg(Neg(var inner))                    -> simplify(inner);
        case Add(var l, var r) -> new Add(simplify(l), simplify(r));
        case Mul(var l, var r) -> new Mul(simplify(l), simplify(r));
        case Neg(var x) -> new Neg(simplify(x));
        case Num n -> n;
        case Var v -> v;
    };   // không default — exhaustive nhờ sealed
}
```

- Unnamed pattern `_`: preview ở 21 (JEP 443), **final ở 22** (JEP 456). Primitive patterns vẫn preview sau 21.
- Pattern matching trên **cặp** giá trị: tạo `record Transition(State s, Event e)` rồi switch — gọn cho state machine/event sourcing.

**Câu hỏi nối tiếp:**
- *Exhaustiveness với record pattern lồng tính thế nào?* — Compiler xét tổ hợp các component; thiếu tổ hợp nào là lỗi (ví dụ chỉ có `case Pair(A a, A b)` cho sealed `{A, B}` thì thiếu).

**⚠️ Câu trả lời gây điểm trừ:**
- "Switch pattern chỉ là chuỗi if-instanceof được viết gọn".

**📖 Ôn lại:** [Phần 9, mục 9.2–9.3](../01-giao-trinh/03-modern-java-8-21.md#p9)

</details>

### Q48. 🔴 Pattern matching, Visitor pattern và polymorphism (method ảo) — khi nào chọn cái nào?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Hành vi là **bản chất** của kiểu (mỗi shape tự biết vẽ) → **method ảo**. Hành vi là **mối quan tâm bên ngoài** (serialize, tính thuế, render report) trên **tập kiểu đóng** → **sealed + switch pattern** — đạt mục tiêu của Visitor (thêm operation mà không sửa class) nhưng gọn hơn và vẫn được compiler kiểm tra exhaustive; trong Java 21, Visitor cho cấu trúc đóng phần lớn có thể thay thế. Tập kiểu **mở** cho plugin bên ngoài → giữ polymorphism. Tránh `switch (o)` với `default` trên `Object` lan khắp codebase — đó là "instanceof chain" đội lốt.

**Giải thích chi tiết:**
- Visitor: boilerplate `accept/visit`, nhưng chạy được ở Java cũ và tách operation khỏi cấu trúc.
- "Data-oriented programming" (Brian Goetz): records cho dữ liệu, sealed cho biến thể, pattern matching cho thao tác.
- Thêm `default` vào switch trên sealed type "cho chắc" → mất lợi ích báo lỗi khi thêm subtype.

**Câu hỏi nối tiếp:**
- *Ảnh hưởng hiệu năng?* — Switch pattern dùng `typeSwitch` có cache; method ảo có thể inline khi monomorphic. Hiếm khi là yếu tố quyết định — chọn theo thiết kế.

**⚠️ Câu trả lời gây điểm trừ:**
- "Pattern matching thay thế OOP".

**📖 Ôn lại:** [Phần 9 — Góc nhìn Senior](../01-giao-trinh/03-modern-java-8-21.md#p9)

</details>

---

<a id="nhom-j"></a>
## J. Sequenced Collections, JPMS, HttpClient

### Q49. 🔴 🧩 Nâng lên Java 17, ứng dụng lỗi `InaccessibleObjectException: Unable to make field ... accessible: module java.base does not "opens" java.lang`. Giải thích và xử lý. `exports` khác `opens` thế nào?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Một thư viện (thường bản cũ của Lombok, Mockito/ByteBuddy, Groovy, serializer, agent APM) dùng **deep reflection** (`setAccessible(true)`) vào internals của JDK. Java 9–15 chỉ cảnh báo, Java 16 (JEP 396) chặn mặc định, **Java 17 (JEP 403) bỏ hẳn `--illegal-access`**. Cách đúng: **nâng thư viện** (Lombok ≥ 1.18.22 cho 17, Spring ≥ 5.3.x...); tạm thời: `--add-opens java.base/java.lang=ALL-UNNAMED`. `exports` cho truy cập type `public` lúc compile/runtime nhưng **không** cho reflection vào private; `opens` cho phép deep reflection lúc runtime (Jackson, Hibernate, Spring cần).

**Giải thích chi tiết:**
- JPMS (Java 9, JEP 261): `module-info.java` khai báo `requires` (`transitive`, `static`), `exports [to]`, `opens [to]`, `uses/provides` (ServiceLoader).
- **Strong encapsulation**: package không export thì `public` class bên trong cũng không truy cập được từ module khác.
- JAR trên classpath thuộc **unnamed module**; JAR không có `module-info` trên module path là **automatic module** (tên từ `Automatic-Module-Name`).
- **Split package** (cùng package ở hai module) → lỗi khởi động — rào cản lớn khi modular hóa code cũ.
- Thực tế đa số app Spring Boot chạy classpath, không `module-info`; hiểu JPMS chủ yếu để xử lý lỗi này, dùng `jdeps --jdk-internals` trước khi migrate, và `jlink` giảm image.

**Câu hỏi nối tiếp:**
- *`sun.misc.Unsafe` còn dùng được không?* — Có, nằm trong module `jdk.unsupported` (được export), nhưng đang dần bị thay bằng `VarHandle`/FFM API.

**⚠️ Câu trả lời gây điểm trừ:**
- "Thêm `--add-opens` cho mọi package là xong" như giải pháp lâu dài.

**📖 Ôn lại:** [Phần 10, mục 10.2 — JPMS](../01-giao-trinh/03-modern-java-8-21.md#p10)

</details>

### Q50. 🟡 Dùng `java.net.http.HttpClient` (Java 11) đúng cách thế nào? Kể các lỗi hay gặp.

<details><summary>Đáp án</summary>

**Trả lời ngắn:** `HttpClient` immutable, thread-safe, giữ connection pool → **tạo một lần, dùng lại**. Đặt **cả hai** timeout: `connectTimeout` (builder của client) và `timeout` của từng `HttpRequest` (tới khi nhận response headers) — mặc định **không có** request timeout → thread treo vô hạn khi server đầu kia treo. `send` **không ném exception với 4xx/5xx** — phải tự kiểm `statusCode()`. `BodyHandlers.ofString()` buffer toàn bộ body vào RAM — file lớn dùng `ofFile`/`ofInputStream`. Hỗ trợ HTTP/2, async (`sendAsync` → `CompletableFuture`), WebSocket; từ Java 21 implement `AutoCloseable`.

**Giải thích chi tiết:**

```java
private static final HttpClient CLIENT = HttpClient.newBuilder()
    .version(HttpClient.Version.HTTP_2)
    .connectTimeout(Duration.ofSeconds(2))
    .build();
HttpRequest req = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(5)).GET().build();
```

- Không có retry/circuit breaker sẵn → Resilience4j, hoặc client cấp cao (Spring `RestClient`/`WebClient`).
- Retry chỉ cho lỗi I/O và 502/503/504 với method **idempotent** (GET, PUT, DELETE); exponential backoff + jitter; giới hạn đồng thời bằng Semaphore.
- Executor mặc định là cached thread pool — có thể cấu hình `.executor(...)`.

**Câu hỏi nối tiếp:**
- *Vì sao không retry POST?* — Không idempotent: có thể tạo đơn/trừ tiền hai lần; cần idempotency key nếu muốn retry.

**⚠️ Câu trả lời gây điểm trừ:**
- Tạo `HttpClient` mới cho mỗi request.

**📖 Ôn lại:** [Phần 10, mục 10.3 — HttpClient](../01-giao-trinh/03-modern-java-8-21.md#p10)

</details>

### Q51. 🟡 🔍 Đoạn code sau (Java 21) in ra gì hoặc ném gì?

```java
var list = new ArrayList<>(List.of(1, 2, 3));
list.reversed().set(0, 99);
System.out.println(list + " " + list.getFirst());
var lhm = new LinkedHashMap<String, Integer>();
lhm.put("b", 2); lhm.put("a", 1); lhm.putFirst("z", 0);
System.out.println(lhm + " " + lhm.lastEntry());
List.of(1, 2).addFirst(0);
```

<details><summary>Đáp án</summary>

**Trả lời ngắn:** `[1, 2, 99] 1` — `reversed()` là **view**, ghi qua view sửa list gốc (phần tử 0 của view là phần tử cuối của gốc). `{z=0, b=2, a=1} a=1`. Dòng cuối ném **`UnsupportedOperationException`** — `List.of` immutable.

**Giải thích chi tiết:**
- Sequenced Collections (JEP 431): `SequencedCollection` (`getFirst/getLast/addFirst/addLast/removeFirst/removeLast/reversed`), `SequencedSet`, `SequencedMap` (`firstEntry/lastEntry/pollFirstEntry/putFirst/sequencedKeySet`...).
- `addFirst` trên `ArrayList` là O(n); `SortedSet.addFirst` ném UOE (thứ tự do comparator quyết định).
- Pitfall migration: class tự định nghĩa có sẵn `getFirst()`/`reversed()` với kiểu trả về khác có thể xung đột khi lên 21.

**Câu hỏi nối tiếp:**
- *In LRU cache từ mới nhất đến cũ nhất?* — `lru.sequencedKeySet().reversed()`.

**⚠️ Câu trả lời gây điểm trừ:**
- Nghĩ `reversed()` trả bản sao nên list gốc không đổi.

**📖 Ôn lại:** [Phần 10, mục 10.1 — Sequenced Collections](../01-giao-trinh/03-modern-java-8-21.md#p10)

</details>

---

<a id="nhom-k"></a>
## K. Phiên bản LTS & migration

### Q52. 🟢 Mô hình phát hành của Java hiện nay thế nào? Các bản LTS là những bản nào? Preview feature là gì?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Từ Java 9, JDK phát hành **6 tháng một lần** (tháng 3 và tháng 9). LTS: **8 (2014), 11 (2018), 17 (2021), 21 (2023), 25 (09/2025)** — từ 17 trở đi cứ **2 năm** một LTS (trước đó 3 năm). Tính năng mới thường qua giai đoạn **preview** (`--enable-preview`, có thể đổi hoặc bị rút — như String Templates) rồi mới final. Bytecode preview chỉ chạy đúng phiên bản JDK đó với cờ preview → **không dùng preview trong production**.

**Giải thích chi tiết:**
- Phân biệt "có từ Java X" (final) và "preview ở X": pattern switch preview 17–20, final 21; record preview 14–15, final 16; virtual threads preview 19–20, final 21.
- Lựa chọn vendor JDK (Temurin, Corretto, Zulu, Oracle) ảnh hưởng thời gian hỗ trợ bản vá.

**Câu hỏi nối tiếp:**
- *Nên ở lại LTS hay theo bản 6 tháng?* — Production thường theo LTS để có bản vá dài hạn; bản non-LTS hợp khi team có năng lực nâng cấp nhanh mỗi 6 tháng.

**⚠️ Câu trả lời gây điểm trừ:**
- Không biết Java 21/25 là LTS, hoặc nhầm preview với final.

**📖 Ôn lại:** [Phần 11, mục 11.1 — Mô hình phát hành](../01-giao-trinh/03-modern-java-8-21.md#p11)

</details>

### Q53. 🟡 Kể những thay đổi quan trọng nhất của từng LTS 8, 11, 17, 21.

<details><summary>Đáp án</summary>

**Trả lời ngắn:**
- **8**: lambda, method reference, Stream, `Optional`, `java.time`, default/static method trong interface, `CompletableFuture`; Metaspace thay PermGen; `HashMap` treeify, `ConcurrentHashMap` viết lại.
- **11** (gộp 9–11): JPMS, `List.of/Map.of`, `var`, `HttpClient`, `String.isBlank/strip/lines/repeat`, `Files.readString`; **G1 mặc định**, container awareness; **xóa Java EE & CORBA** (JAXB, JAX-WS, `javax.annotation`...).
- **17** (gộp 12–17): switch expression, text block, **records**, pattern matching `instanceof`, **sealed classes**, `Stream.toList()`, helpful NPE; ZGC/Shenandoah production-ready; **strong encapsulation JDK internals**; xóa Nashorn.
- **21** (gộp 18–21): **virtual threads**, **pattern matching cho switch**, **record patterns**, **Sequenced Collections**; Generational ZGC; **UTF-8 mặc định** (18); finalization deprecated for removal.

**Giải thích chi tiết:**
- **25**: Scoped Values final, flexible constructor bodies (code trước `super(...)`), compact source files & instance main, Stream Gatherers (final từ 24), compact object headers (product feature, chưa mặc định); từ 24 virtual thread **không còn bị pin** khi gặp `synchronized` (JEP 491).
- Spring Boot 3 yêu cầu **Java 17** tối thiểu và Jakarta EE 9+ (`jakarta.*`).

**Câu hỏi nối tiếp:**
- *Dự án bạn đang ở bản nào, vì sao chưa lên bản mới?* — Câu hỏi mở; trả lời bằng ràng buộc thật (thư viện, app server, quy trình kiểm thử) và kế hoạch cụ thể.

**⚠️ Câu trả lời gây điểm trừ:**
- Gán nhầm virtual threads cho 17 hoặc records cho 11.

**📖 Ôn lại:** [Phần 11, mục 11.2 — Bảng tổng hợp](../01-giao-trinh/03-modern-java-8-21.md#p11)

</details>

### Q54. 🔴 🧩 Migrate một ứng dụng từ Java 8 lên 17/21: những gì **thực sự** gây sự cố?

<details><summary>Đáp án</summary>

**Trả lời ngắn:**
1. **Java EE bị xóa (11)**: `ClassNotFoundException: javax.xml.bind.JAXBContext` → thêm `jakarta.xml.bind-api` + `jaxb-runtime`.
2. **Strong encapsulation (16/17)**: `InaccessibleObjectException` → nâng Lombok, Mockito/ByteBuddy, Spring, Hibernate, Groovy, agent APM.
3. **Build & bytecode**: dùng `--release` (không chỉ `-source/-target`); nâng compiler/surefire plugin; ASM/ByteBuddy phải hiểu class file mới (17 = 61, 21 = 65).
4. **GC đổi mặc định** Parallel → G1; flag cũ (`-XX:+UseConcMarkSweepGC` — CMS xóa ở 14, `-XX:+PrintGCDetails`) làm **JVM không khởi động** → `-Xlog:gc*`.
5. **Locale CLDR (9)**: format ngày/tiền/số thay đổi → test snapshot vỡ, parse chuỗi cũ lỗi; tạm `-Djava.locale.providers=COMPAT,CLDR` (COMPAT bị xóa ở 23).
6. **UTF-8 mặc định (18)**: app trên Windows đọc file không chỉ định charset đổi hành vi.
7. **Parse version string** `1.8.0_292` vỡ với `17.0.2` → `Runtime.version().feature()`.
8. **Container**: Java 8 cũ không nhận cgroup; cgroup v2 cần JDK ≥ 8u372/11.0.16/15+.
9. `Collectors.toList()` → `Stream.toList()` tự động gây UOE.
10. API deprecated for removal: `finalize`, `Thread.stop`, Security Manager → `jdeprscan`.

**Giải thích chi tiết:**
- Công cụ: `jdeps --jdk-internals`, `jdeprscan`, OpenRewrite (`UpgradeToJava17/21`), chạy test suite trên JDK mới trong CI trước khi đổi runtime production.
- Chiến lược 3 bước: (1) **build trên JDK mới với `--release` cũ** (lộ vấn đề tooling); (2) **chạy trên JDK mới** với bytecode cũ (lộ vấn đề reflection/GC/locale); (3) mới **nâng `--release`** và dùng tính năng mới. Mỗi bước canary, so sánh GC log, p99, error rate.

**Câu hỏi nối tiếp:**
- *"Chỉ đổi `JAVA_HOME` rồi chạy được" — đủ chưa?* — Chưa: base image Docker, script khởi động với flag cũ, agent, và hành vi runtime (GC, locale, charset) cần kiểm tra.

**⚠️ Câu trả lời gây điểm trừ:**
- Chỉ nói về tính năng mới mà không nói tới dependency upgrade, GC, strong encapsulation, kế hoạch rollback.

**📖 Ôn lại:** [Phần 11, mục 11.3 — Checklist migrate](../01-giao-trinh/03-modern-java-8-21.md#p11)

</details>

### Q55. 🔴 🧩 Tech lead giao bạn lập kế hoạch nâng 20 microservice từ Java 11 + Spring Boot 2.7 lên Java 21 + Spring Boot 3.x. Bạn trình bày kế hoạch thế nào?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Đi theo từng bước nhỏ, mỗi bước deploy được và rollback được: (1) nâng Boot 2.7 lên bản patch cuối, xử lý hết deprecation; (2) đổi **runtime** sang JDK 17 (Boot 2.7 chạy được trên 17) — đo GC/latency; (3) **Boot 3 + `javax.*` → `jakarta.*`** (OpenRewrite recipe), Hibernate 6, Spring Security 6; (4) lên **JDK 21**; (5) bật **virtual threads** (`spring.threads.virtual.enabled=true`, Boot 3.2+) **có điều kiện** cho service I/O-bound, sau khi kiểm tra pinning và giới hạn connection pool. Bắt đầu bằng 1–2 service ít rủi ro làm mẫu, rồi nhân rộng bằng thư viện/BOM dùng chung.

**Giải thích chi tiết:**
- **Lợi ích đo được**: thời gian hỗ trợ bản vá, hiệu năng GC (G1/Generational ZGC), throughput I/O với virtual threads, tính năng ngôn ngữ, yêu cầu bảo mật/compliance.
- **Rủi ro**: thư viện nội bộ dùng chung (phải nâng trước), Hibernate 6 thay đổi query/type mapping, Spring Security 6 thay đổi DSL cấu hình, agent APM, client library của bên thứ ba còn `javax`.
- **Rollback**: giữ image cũ, feature flag cho thay đổi hành vi, canary theo % traffic, so sánh dashboard (p99, error rate, GC pause, CPU/memory).
- **Virtual threads**: không giới hạn số thread nhưng DB pool vẫn giới hạn → cần bulkhead; `synchronized` quanh I/O gây pinning trên 21 (đã khắc phục ở 24); `ThreadLocal` nặng nhân lên theo số virtual thread.
- **Image Docker**: base image JRE 21 (hoặc `jlink`), cấu hình `-XX:MaxRAMPercentage`.

**Câu hỏi nối tiếp:**
- *Vì sao không nhảy thẳng Boot 3 + Java 21 một lần?* — Gộp nhiều thay đổi làm khó khoanh vùng khi có sự cố; tách bước giúp mỗi lần rollback chỉ hoàn tác một loại thay đổi.

**⚠️ Câu trả lời gây điểm trừ:**
- Kế hoạch "big bang" cho cả 20 service, không có đo lường và rollback.

**📖 Ôn lại:** [Phần 11 — Góc nhìn Senior & bài 11.3](../01-giao-trinh/03-modern-java-8-21.md#p11)

</details>

### Q56. 🟡 🔍 Phiên bản Java **tối thiểu** (không dùng preview) để compile từng đoạn code sau?

```java
var x = 10;                                   // (a)
List.of(1, 2);                                // (b)
record P(int x, int y) {}                     // (c)
String s = """
    hi""";                                    // (d)
stream.toList();                              // (e)
sealed interface S permits A {}               // (f)
switch (o) { case null -> {} default -> {} }  // (g)
list.getFirst();                              // (h)
Thread.ofVirtual().start(task);               // (i)
Collectors.teeing(c1, c2, merger);            // (j)
"ab".repeat(3);                               // (k)
if (o instanceof Point(int px, int py)) {}    // (l)
```

<details><summary>Đáp án</summary>

**Trả lời ngắn:** (a) **10**, (b) **9**, (c) **16**, (d) **15**, (e) **16**, (f) **17**, (g) **21**, (h) **21**, (i) **21**, (j) **12**, (k) **11**, (l) **21**.

**Giải thích chi tiết:**
- Thêm: switch expression `->` **14**; pattern `instanceof` (type pattern) **16**; `HttpClient`, `Optional.isEmpty`, `Predicate.not`, `Files.readString` **11**; `Stream.mapMulti` **16**; `Collectors.toUnmodifiableList` **10**; `takeWhile`, `Optional.or/stream` **9**; unnamed pattern `_` **22**.
- Kiểm chứng: `javac --release N` với image JDK tương ứng (`eclipse-temurin:17-jdk`...).
- Với codebase phải chạy nhiều JDK, `--release` của build quyết định API được phép dùng — không chỉ cú pháp.

**Câu hỏi nối tiếp:**
- *Chỉ đặt `-source 17 -target 17` khi build bằng JDK 21 có vấn đề gì?* — Compiler vẫn cho dùng API của JDK 21 (ví dụ `getFirst()`), chạy trên JRE 17 sẽ `NoSuchMethodError`; `--release 17` kiểm tra cả API.

**⚠️ Câu trả lời gây điểm trừ:**
- Nói (c) là 14 hoặc (g) là 17 (nhầm preview với final).

**📖 Ôn lại:** [Phần 11 — bảng tổng hợp & bài 11.1](../01-giao-trinh/03-modern-java-8-21.md#p11)

</details>

---

> ✅ **Tự đánh giá sau khi luyện:** trả lời trôi chảy ≥ 90% câu 🟢, ≥ 75% câu 🟡, và với mỗi câu 🔴 nói được cơ chế + ít nhất một trade-off hoặc kinh nghiệm production. Đối chiếu thêm với [Checklist tự đánh giá của Module 03](../01-giao-trinh/03-modern-java-8-21.md#checklist-tu-danh-gia).

# Câu hỏi phỏng vấn — Module 02: Collections Framework & Generics

> Giáo trình tương ứng: [Module 02 — Collections Framework & Generics](../01-giao-trinh/02-collections-generics.md)

**Cách dùng:** đọc câu hỏi, **tự trả lời thành tiếng** trong 1–2 phút (với câu đọc code: tự viết output ra giấy) *rồi mới* mở "Đáp án". **Trả lời ngắn** là thứ phải nói được trong 30 giây; **Giải thích chi tiết** và **Câu hỏi nối tiếp** là chỗ interviewer phân biệt Senior. Câu nào sai hoặc ấp úng → bấm link 📖 quay lại giáo trình.

**Mức độ:** 🟢 Cơ bản · 🟡 Senior · 🔴 Xoáy sâu. **Dạng câu:** 🧩 tình huống · 🔍 đọc code.

> 💡 HashMap là chủ đề được hỏi nhiều nhất ở mọi cấp. Nếu chỉ có thời gian ôn một nhóm, hãy ôn **nhóm B** và **nhóm G**.

## Mục lục

| Nhóm | Chủ đề | Câu |
|---|---|---|
| A | [Kiến trúc Collections & List](#nhom-a) | Q1–Q6 |
| B | [`HashMap` internals](#nhom-b) | Q7–Q17 |
| C | [`LinkedHashMap`, `TreeMap`, Set, Comparator](#nhom-c) | Q18–Q23 |
| D | [Queue, Deque, `PriorityQueue`](#nhom-d) | Q24–Q27 |
| E | [Iterator, fail-fast, `ConcurrentModificationException`](#nhom-e) | Q28–Q31 |
| F | [Immutable vs unmodifiable](#nhom-f) | Q32–Q35 |
| G | [Concurrent collections](#nhom-g) | Q36–Q43 |
| H | [Generics](#nhom-h) | Q44–Q53 |
| I | [Chọn collection & memory footprint](#nhom-i) | Q54–Q56 |

---

<a id="nhom-a"></a>
## A. Kiến trúc Collections & List

### Q1. 🟢 Vì sao `Map` không kế thừa `Collection`? Kể tên các class legacy nên tránh và thay bằng gì.

<details><summary>Đáp án</summary>

**Trả lời ngắn:** `Collection` là tập phần tử đơn, `Map` là tập cặp key–value — `add(E)` không có nghĩa rõ ràng với `Map`. Thay vào đó `Map` cung cấp 3 **view sống**: `keySet()`, `values()`, `entrySet()` (sửa view là sửa map). Class legacy cần tránh: `Vector` → `ArrayList`, `Stack` → `ArrayDeque`, `Hashtable` → `HashMap`/`ConcurrentHashMap`.

**Giải thích chi tiết:**
- Legacy class đồng bộ từng method (`synchronized` trên `this`) → chậm do lock mọi thao tác, mà thao tác phức hợp (check-then-act, duyệt) **vẫn không an toàn**.
- `Stack extends Vector` → cho phép `get(i)`, `add(i, e)` phá ngữ nghĩa stack.
- `Collection` (interface) khác `Collections` (utility: `sort`, `unmodifiableList`, `synchronizedMap`, `emptyList`...).
- Lập trình theo interface: khai báo `List<Order> orders = new ArrayList<>()`, nhận tham số kiểu rộng nhất (`Collection<? extends Order>`), trả về collection rỗng thay vì `null` (Effective Java Item 54).

**Câu hỏi nối tiếp:**
- *`map.keySet().add(k)` làm gì?* — `UnsupportedOperationException`: keySet không biết value nào để put.
- *`map.values().removeIf(v -> v == 0)` có xóa entry khỏi map?* — Có, vì là view sống.

**⚠️ Câu trả lời gây điểm trừ:**
- "Map có kế thừa Collection" hoặc nhầm `Collection` với `Collections`.

**📖 Ôn lại:** [Phần 1 — Tổng quan kiến trúc](../01-giao-trinh/02-collections-generics.md#phan-1)

</details>

### Q2. 🟢 Sequenced Collections (Java 21) giải quyết vấn đề gì?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Trước Java 21, lấy phần tử đầu/cuối không thống nhất: `list.get(list.size() - 1)`, `deque.getLast()`, `LinkedHashSet` thì phải duyệt hết, `SortedSet.last()`. JEP 431 thêm interface `SequencedCollection`, `SequencedSet`, `SequencedMap` với `getFirst/getLast`, `addFirst/addLast`, `removeFirst/removeLast` và **`reversed()` trả về view đảo ngược (không copy)**; `SequencedMap` có `firstEntry`, `pollLastEntry`, `putFirst`...

**Giải thích chi tiết:**
- `List`, `Deque`, `LinkedHashSet`, `SortedSet`, `LinkedHashMap`, `SortedMap` được gắn vào hierarchy mới; `HashSet`/`HashMap` thì không (không có thứ tự xác định).
- `reversed()` là view → sửa view là sửa collection gốc.
- Lưu ý migrate: class tự viết implement `List` và có sẵn method `getFirst()` với kiểu trả về khác có thể lỗi compile khi lên 21; ngược lại, thêm method mới vào interface có thể xung đột với method Kotlin extension cùng tên.

**Câu hỏi nối tiếp:**
- *`List.of(1,2,3).addFirst(0)`?* — `UnsupportedOperationException` — list immutable.

**⚠️ Câu trả lời gây điểm trừ:**
- Nghĩ `reversed()` tạo bản sao mới.

**📖 Ôn lại:** [Phần 1, mục 1.2 — Sequenced Collections](../01-giao-trinh/02-collections-generics.md#phan-1)

</details>

### Q3. 🟢 `ArrayList` hoạt động thế nào bên trong? Vì sao `add` cuối là "amortized O(1)"?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** `ArrayList` giữ `Object[] elementData` và `size`. `new ArrayList<>()` dùng mảng rỗng dùng chung (**lazy allocation**, chỉ cấp phát 10 khi `add` đầu tiên). Khi đầy, capacity mới ≈ `old + (old >> 1)` (**1.5 lần**) và copy bằng `System.arraycopy`. Vì tăng theo cấp số nhân, tổng chi phí copy cho n lần add là O(n) → trung bình O(1) mỗi lần.

**Giải thích chi tiết:**
- `add(i, e)`/`remove(i)` dịch phần tử phía sau → O(n − i); xóa cuối O(1).
- `remove`/`clear` **không thu nhỏ** mảng → list từng chứa 1 triệu phần tử vẫn giữ mảng 1 triệu slot; dùng `trimToSize()` hoặc tạo list mới.
- Biết trước kích thước → `new ArrayList<>(n)` tránh ~30 lần grow cho 1 triệu phần tử.
- Phần tử bị xóa phải gán `null` để GC thu hồi ("obsolete reference", Item 7) — `ArrayList` làm điều này; cấu trúc tự viết hay quên.
- `RandomAccess` là marker để thuật toán generic (`Collections.binarySearch`) chọn truy cập theo index.

**Câu hỏi nối tiếp:**
- *Tại sao không grow 2x?* — Trade-off giữa số lần copy và bộ nhớ dư; 1.5x lãng phí ít hơn.

**⚠️ Câu trả lời gây điểm trừ:**
- "ArrayList tăng thêm 10 phần tử mỗi lần" hoặc không giải thích được amortized.

**📖 Ôn lại:** [Phần 2, mục 2.1 — `ArrayList` bên trong](../01-giao-trinh/02-collections-generics.md#phan-2)

</details>

### Q4. 🟡 "Cần chèn/xóa ở giữa nhiều thì dùng `LinkedList`" — bạn đồng ý không?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Gần như không. `LinkedList.add(i, e)` vẫn là **O(n) vì phải tìm node** trước; chỉ O(1) khi đã đứng sẵn ở vị trí qua `ListIterator`. Còn `ArrayList` dịch phần tử bằng `System.arraycopy` trên **bộ nhớ liền mạch** — cực nhanh nhờ CPU cache và prefetch — trong khi duyệt `LinkedList` là pointer chasing qua các node rải rác → cache miss liên tục. Thực tế `ArrayList` thắng trong hầu hết trường hợp; cần thao tác hai đầu thì dùng `ArrayDeque`.

**Giải thích chi tiết:**
- Bộ nhớ: mỗi phần tử `LinkedList` tốn thêm một `Node` (~24 byte) so với ~4 byte reference của `ArrayList` → áp lực GC cao hơn.
- Bug kinh điển: duyệt `LinkedList` bằng `for (i...) list.get(i)` → O(n²). Khi kiểu có thể là `LinkedList`, luôn dùng for-each/iterator.
- Trường hợp `LinkedList` có thể hợp lý: xóa/chèn nhiều trong lúc đang duyệt bằng `ListIterator` trên list rất lớn — nhưng vẫn nên đo bằng JMH.

**Câu hỏi nối tiếp:**
- *Joshua Bloch nói gì về `LinkedList`?* — Ông (tác giả của nó) đùa rằng chính mình cũng không dùng.

**⚠️ Câu trả lời gây điểm trừ:**
- Trả lời theo bảng Big-O sách giáo khoa mà không nói tới cache locality và chi phí tìm node.

**📖 Ôn lại:** [Phần 2, mục 2.2–2.3](../01-giao-trinh/02-collections-generics.md#phan-2)

</details>

### Q5. 🟡 🧩 Cần xóa ~30% phần tử (đơn đã hủy) khỏi một `ArrayList` 5 triệu phần tử. Bạn làm thế nào?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Dùng **`list.removeIf(Order::cancelled)`** — O(n): nó đánh dấu phần tử cần xóa rồi **nén mảng một lần**. Vòng `for` lùi với `remove(i)` hay `iterator.remove()` đều là O(n·k) vì mỗi lần xóa phải `arraycopy` phần đuôi — với 1,5 triệu lần xóa trên 5 triệu phần tử là chậm khủng khiếp.

**Giải thích chi tiết:** Tự viết tương đương bằng two-pointer compaction:

```java
int w = 0;
for (int r = 0; r < list.size(); r++) {
    Order o = list.get(r);
    if (!o.cancelled()) list.set(w++, o);
}
list.subList(w, list.size()).clear(); // removeRange một lần
```

- Hoặc tạo list mới bằng stream `filter` nếu không cần sửa tại chỗ (tốn thêm bộ nhớ tạm).
- Sau đó cân nhắc `trimToSize()` nếu list sống lâu.

**Câu hỏi nối tiếp:**
- *Với `LinkedList` thì `iterator.remove()` có tốt hơn?* — Mỗi lần xóa O(1) nên tổng O(n), nhưng vẫn chịu chi phí duyệt node rải rác; `removeIf` vẫn là lựa chọn gọn nhất cho cả hai.

**⚠️ Câu trả lời gây điểm trừ:**
- "for-each rồi `list.remove(o)`" → CME.

**📖 Ôn lại:** [Phần 2 — Lỗi thường gặp & bài 2.3](../01-giao-trinh/02-collections-generics.md#phan-2)

</details>

### Q6. 🟡 🔍 Đoạn code sau in ra gì? Có bug gì?

```java
List<Integer> list = new ArrayList<>(List.of(1, 2, 2, 3));
for (int i = 0; i < list.size(); i++) {
    if (list.get(i) == 2) list.remove(i);
}
System.out.println(list);
```

<details><summary>Đáp án</summary>

**Trả lời ngắn:** In `[1, 2, 3]` — **bỏ sót** một số 2. Khi xóa ở index 1, phần tử phía sau dịch lên index 1, nhưng `i` tăng lên 2 nên phần tử đó không được kiểm tra. Không có CME vì không dùng iterator.

**Giải thích chi tiết:**
- `list.remove(i)` ở đây xóa theo **index** (đúng ý), vì `i` là `int`. Nếu viết `list.remove(2)` với ý "xóa giá trị 2" thì lại là xóa index 2 (Module 01).
- `list.get(i) == 2`: so `Integer` với `int` → unbox → đúng. Nhưng `list.get(i) == list.get(j)` thì so reference (bẫy cache -128..127).
- Sửa: `list.removeIf(x -> x == 2)`, hoặc duyệt lùi `for (int i = list.size() - 1; i >= 0; i--)`, hoặc `i--` sau khi xóa.

**Câu hỏi nối tiếp:**
- *Đổi sang for-each thì sao?* — `ConcurrentModificationException` ở lần `next()` sau khi xóa (trừ trường hợp phần tử áp chót, xem Q29).

**⚠️ Câu trả lời gây điểm trừ:**
- Trả lời `[1, 3]` hoặc "ném CME".

**📖 Ôn lại:** [Phần 2 — Lỗi thường gặp](../01-giao-trinh/02-collections-generics.md#phan-2)

</details>

---

<a id="nhom-b"></a>
## B. `HashMap` internals

### Q7. 🟡 Trình bày `HashMap` hoạt động thế nào — từ cấu trúc tới thread-safety.

<details><summary>Đáp án</summary>

**Trả lời ngắn (theo khung 6 bước):**
1. **Cấu trúc**: mảng `Node<K,V>[] table` (bins), length luôn là lũy thừa của 2, mặc định 16, cấp phát lười ở lần `put` đầu.
2. **Index**: `hash = h ^ (h >>> 16)` (spreading), `index = (n - 1) & hash`.
3. **Collision**: chaining bằng linked list (tail insertion từ Java 8); bin ≥ 8 node và table ≥ 64 → **red-black tree**.
4. **Resize**: khi `size > capacity × 0.75` → gấp đôi, mỗi node ở lại index `j` hoặc sang `j + oldCap`.
5. **Điều kiện với key**: `equals/hashCode` đúng hợp đồng và **bất biến** khi đang là key.
6. **Không thread-safe**: Java 7 có thể lặp vô hạn khi resize đồng thời; Java 8 vẫn mất dữ liệu → dùng `ConcurrentHashMap`.

**Giải thích chi tiết:**
- `Node` cache `hash` → không tính lại khi resize/so sánh.
- Cho phép 1 key `null` (hash = 0, bucket 0) và nhiều value `null`.
- Duyệt tốn O(capacity + size): map từng phình to rồi xóa gần hết vẫn duyệt chậm (capacity không giảm).
- `map.get(k) == null` không phân biệt "không có key" với "value null" → dùng `containsKey` hoặc tránh lưu null.

**Câu hỏi nối tiếp:** interviewer thường đào tiếp vào từng bước — xem Q8–Q12.

**⚠️ Câu trả lời gây điểm trừ:**
- Chỉ nói "lưu key-value bằng hash, O(1)" mà không có cấu trúc, resize, treeify.

**📖 Ôn lại:** [Phần 3 — `HashMap` internals chuyên sâu](../01-giao-trinh/02-collections-generics.md#phan-3)

</details>

### Q8. 🟡 Vì sao capacity của `HashMap` luôn là lũy thừa của 2? Dòng `h ^ (h >>> 16)` để làm gì?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Với `n` là lũy thừa của 2, `(n - 1) & hash` tương đương `hash % n` nhưng chỉ là **một phép AND** và luôn không âm. Nhược điểm: index chỉ dùng **các bit thấp** của hash; key có hashCode chỉ khác nhau ở bit cao sẽ dồn vào cùng bucket. `h ^ (h >>> 16)` **trộn 16 bit cao xuống bit thấp** với chi phí cực nhỏ để giảm va chạm.

**Giải thích chi tiết:**
- Ví dụ hashCode tệ: `Float`, hoặc giá trị là bội số của 2^k (ID nhân 1024) → bit thấp giống nhau.
- Lũy thừa 2 còn cho phép resize chỉ cần xét **một bit** (Q10).
- Truyền capacity không phải lũy thừa 2 → `tableSizeFor` làm tròn lên (1000 → 1024).

**Câu hỏi nối tiếp:**
- *Vì sao `Hashtable` dùng `%` với capacity lẻ/nguyên tố?* — Phân tán tốt hơn với hash tệ nhưng phép chia chậm; HashMap chọn mask + spreading.

**⚠️ Câu trả lời gây điểm trừ:**
- "Cho đẹp" hoặc không biết vai trò của spreading.

**📖 Ôn lại:** [Phần 3, mục 3.2 — Hash spreading và tính index](../01-giao-trinh/02-collections-generics.md#phan-3)

</details>

### Q9. 🟡 Mô tả từng bước của `put(key, value)`. Khi so sánh key, `HashMap` so cái gì trước?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** (1) Table chưa có → `resize()` cấp phát; (2) tính index, bin rỗng → đặt node mới; (3) bin có node: so **`hash` (int) trước**, rồi `==`, rồi mới `equals` — trùng thì thay value; là `TreeNode` → `putTreeVal`; không thì duyệt list và **nối cuối**, nếu list đạt 8 → `treeifyBin`; (4) key đã tồn tại: thay value, trả value cũ, **không** tăng `modCount`; (5) thêm mới: `++modCount`, `if (++size > threshold) resize()`.

**Giải thích chi tiết:**
- So `hash` trước vì rẻ: hash khác nhau thì chắc chắn khác key, tránh gọi `equals` tốn kém.
- `treeifyBin` với `table.length < 64` sẽ **resize thay vì treeify** — bin dài thường do table quá nhỏ.
- Thay value không phải structural modification → không làm iterator đang chạy ném CME.

**Câu hỏi nối tiếp:**
- *`put` trả về gì?* — Value cũ hoặc `null` (không phân biệt được "chưa có" với "value cũ là null").

**⚠️ Câu trả lời gây điểm trừ:**
- "Gọi `equals` với mọi key trong map".

**📖 Ôn lại:** [Phần 3, mục 3.3 — `put` từng bước](../01-giao-trinh/02-collections-generics.md#phan-3)

</details>

### Q10. 🔴 Resize của `HashMap` diễn ra thế nào? Vì sao chỉ cần xét một bit? Vì sao `HashMap` Java 7 có thể làm CPU 100%?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Capacity và threshold gấp đôi; mỗi node chỉ có thể ở **index cũ `j`** hoặc **`j + oldCap`**, quyết định bởi `(hash & oldCap) == 0` — vì index mới `hash & (2·oldCap − 1)` chỉ khác index cũ đúng ở bit `oldCap`. Java 8 tách bin thành "lo list" và "hi list" **giữ nguyên thứ tự**. Java 7 dùng **head insertion** và đảo thứ tự list khi `transfer` → hai thread resize đồng thời có thể tạo **chu trình** trong linked list → `get()` lặp vô hạn, CPU 100%.

**Giải thích chi tiết:**
- Resize là O(n) và cấp phát mảng mới gấp đôi → **spike latency + rác**; map hàng triệu entry resize giữa giờ cao điểm gây p99 xấu → chỉ định capacity ban đầu (Q14).
- Java 8 hết chu trình nhưng `HashMap` **vẫn không thread-safe**: mất cập nhật (hai thread cùng đặt vào bin rỗng), `size` sai, mất entry trong lúc resize.
- Triệu chứng Java 7 trong thread dump: nhiều thread `RUNNABLE` mãi ở `HashMap.get`/`transfer`.

**Câu hỏi nối tiếp:**
- *Khi resize, `TreeNode` bin xử lý thế nào?* — Tách thành hai phần như list; phần nào còn ≤ 6 node thì untreeify về list.

**⚠️ Câu trả lời gây điểm trừ:**
- "Rehash lại toàn bộ key bằng `hashCode()`" — hash đã được cache trong `Node`.

**📖 Ôn lại:** [Phần 3, mục 3.4 — Resize](../01-giao-trinh/02-collections-generics.md#phan-3)

</details>

### Q11. 🔴 Treeification hoạt động thế nào? Vì sao ngưỡng là 8/6/64? Key không `Comparable` thì sao?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Java 8 (JEP 180): bin có ≥ 8 node **và** table ≥ 64 → chuyển thành red-black tree, tìm trong bin từ O(k) thành **O(log k)**. Cây ≤ 6 node (khi resize tách) → về list; khoảng trễ 8/6 tránh dao động. Table < 64 thì resize thay vì treeify. Với hashCode tốt, xác suất bin có 8 phần tử theo Poisson là ~**0,00000006** → treeify gần như chỉ xảy ra khi hashCode tệ hoặc bị **hash flooding**. Cây sắp xếp theo **hash**, rồi `compareTo` nếu key `Comparable` cùng class; nếu **cùng hash và không `Comparable`**, lookup vẫn có thể phải duyệt cả hai nhánh → gần O(n).

**Giải thích chi tiết:**
- Hash flooding: attacker gửi hàng chục nghìn tham số HTTP/key JSON có cùng `String.hashCode()` (ghép các khối `"Aa"`/`"BB"`) để biến lookup thành O(n) → DoS. Java 8 giảm thiểu vì `String` là `Comparable` → O(log n).
- Phòng thủ ở tầng ứng dụng: giới hạn số tham số request, số key JSON, kích thước body.
- `TreeNode` lớn gấp ~2 lần `Node`.
- `tieBreakOrder` (tên class, `identityHashCode`) chỉ dùng để cân bằng cây khi chèn, không giúp tìm kiếm.

**Câu hỏi nối tiếp:**
- *Worst case `get` Java 7 vs Java 8?* — O(n) vs O(log n) (với key `Comparable`).

**⚠️ Câu trả lời gây điểm trừ:**
- "Bin > 8 là chuyển sang cây" mà không biết điều kiện 64 và vai trò `Comparable`.

**📖 Ôn lại:** [Phần 3, mục 3.5 — Treeification](../01-giao-trinh/02-collections-generics.md#phan-3)

</details>

### Q12. 🟡 🔍 Đoạn code sau in ra gì?

```java
static final class Point {
    int x, y;
    Point(int x, int y) { this.x = x; this.y = y; }
    @Override public boolean equals(Object o) { return o instanceof Point p && p.x == x && p.y == y; }
    @Override public int hashCode() { return 31 * x + y; }
}
Map<Point, String> map = new HashMap<>();
Point p = new Point(1, 2);
map.put(p, "A");
p.x = 100;
System.out.println(map.get(p));
System.out.println(map.get(new Point(1, 2)));
System.out.println(map.size());
System.out.println(map.containsValue("A"));
```

<details><summary>Đáp án</summary>

**Trả lời ngắn:** `null`, `null`, `1`, `true`. Key bị sửa **sau khi** put → hash đổi. `get(p)` tìm ở bucket mới (entry nằm ở bucket cũ). `get(new Point(1,2))` đúng bucket cũ nhưng `equals` so với key đã thành `(100,2)` → false. Entry vẫn còn đó, "mồ côi" — không lấy ra hay xóa được → **memory leak**.

**Giải thích chi tiết:**
- Key của `HashMap`/phần tử `HashSet` phải **bất biến** trong suốt thời gian là key: dùng `String`, wrapper, enum, record với component bất biến.
- Biến thể thực tế: entity JPA có `hashCode` theo ID tự sinh bỏ vào `HashSet` trước khi persist; dùng `List` mutable làm key; `hashCode` phụ thuộc `lastModified`.
- Cùng loại bug với `TreeSet`/`PriorityQueue` khi sửa field tham gia so sánh.

**Câu hỏi nối tiếp:**
- *Có cách nào "cứu" entry mồ côi?* — Duyệt `entrySet()` với iterator và `remove()` (iterator xóa theo node, không theo hash) — chỉ là chữa cháy.

**⚠️ Câu trả lời gây điểm trừ:**
- Trả lời `A` cho dòng đầu.

**📖 Ôn lại:** [Phần 3, mục 3.7 — Bug key mutable](../01-giao-trinh/02-collections-generics.md#phan-3)

</details>

### Q13. 🟢 Độ phức tạp các thao tác của `HashMap`? Vì sao nói O(1) "trung bình"?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** `get/put/remove/containsKey`: O(1) trung bình; xấu nhất O(n) ở Java 7, **O(log n)** ở Java 8+ khi key `Comparable`. `containsValue`: O(n). Duyệt: O(capacity + size). "Trung bình" vì phụ thuộc hashCode phân tán đều và load factor; hashCode tệ (ví dụ trả hằng số) → mọi key vào một bin.

**Giải thích chi tiết:**
- Load factor 0.75 là trade-off thời gian/không gian; hiếm khi cần đổi.
- Duyệt `keySet()` rồi `get(key)` tốn gấp đôi lookup → duyệt `entrySet()` hoặc `forEach((k, v) -> ...)`.
- Với n nhỏ (vài chục phần tử), quét tuyến tính `ArrayList` có thể nhanh hơn vì không phải tính hash và cache locality tốt.

**Câu hỏi nối tiếp:**
- *`hashCode` trả về hằng số có vi phạm hợp đồng không?* — Không, nhưng biến map thành list/cây.

**⚠️ Câu trả lời gây điểm trừ:**
- "Luôn O(1)".

**📖 Ôn lại:** [Phần 3, mục 3.6 — Độ phức tạp](../01-giao-trinh/02-collections-generics.md#phan-3)

</details>

### Q14. 🟡 🔍 `new HashMap<>(1000)` rồi put 1000 entry — có resize không? Khởi tạo đúng thế nào?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** **Có, 1 lần.** Tham số là *capacity*, không phải số entry: `tableSizeFor(1000) = 1024`, threshold = `1024 × 0.75 = 768` → entry thứ 769 gây resize lên 2048. Muốn chứa 1000 entry không resize: `new HashMap<>((int) (1000 / 0.75f) + 1)`, hoặc **`HashMap.newHashMap(1000)`** (Java 19+), hoặc Guava `Maps.newHashMapWithExpectedSize(1000)`.

**Giải thích chi tiết:**
- Tương tự cho `HashSet` (bọc `HashMap`): Java 19 có `HashSet.newHashSet(n)`.
- `new HashMap<>()` không cấp phát mảng ngay — lazy ở lần `put` đầu.
- Ở đa số code, một lần resize không đáng kể; quan trọng với map lớn hoặc map tạo liên tục trong hot path.

**Câu hỏi nối tiếp:**
- *`ArrayList(1000)` có cùng vấn đề?* — Không, `ArrayList` capacity = số phần tử chứa được.

**⚠️ Câu trả lời gây điểm trừ:**
- "Không resize vì đã khai báo 1000".

**📖 Ôn lại:** [Phần 3, mục 3.8 — Khởi tạo capacity đúng cách](../01-giao-trinh/02-collections-generics.md#phan-3)

</details>

### Q15. 🟡 Dùng `merge`, `compute`, `computeIfAbsent` thế nào? Có ràng buộc gì với hàm truyền vào?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** `merge(k, 1, Integer::sum)` là cách đếm tần suất gọn nhất; `computeIfAbsent(k, x -> new ArrayList<>()).add(v)` dựng multimap; `compute` cập nhật dựa trên giá trị cũ. Hàm **trả về `null` sẽ xóa entry**. Hàm truyền vào **không được sửa chính map đó** — `HashMap` Java 9+ có thể ném `ConcurrentModificationException`, `ConcurrentHashMap` có thể ném `IllegalStateException: Recursive update` (Java 9+) hoặc treo (Java 8).

**Giải thích chi tiết:**

```java
for (String w : words) wordCount.merge(w, 1, Integer::sum);
byCustomer.computeIfAbsent(o.customerId(), k -> new ArrayList<>()).add(o);
map.entrySet().removeIf(e -> e.getValue() == 0);
map.replaceAll((k, v) -> v * 2);
```

- `computeIfAbsent` không put nếu hàm trả `null`.
- Memoization đệ quy (Fibonacci bằng `computeIfAbsent` gọi lại chính nó) là ví dụ kinh điển của việc sửa map bên trong hàm → sai.

**Câu hỏi nối tiếp:**
- *`getOrDefault(k, 0)` có put không?* — Không, chỉ đọc.

**⚠️ Câu trả lời gây điểm trừ:**
- Viết `if (map.containsKey(k)) map.put(k, map.get(k) + 1) else map.put(k, 1)` — 3 lần lookup và không atomic khi map concurrent.

**📖 Ôn lại:** [Phần 3, mục 3.9 — Các API Java 8+](../01-giao-trinh/02-collections-generics.md#phan-3)

</details>

### Q16. 🔴 🧩 Một Spring singleton bean có field `private final Map<String, Rate> cache = new HashMap<>();` được nạp lazy khi request đến. Thỉnh thoảng giá trả về sai và `cache.size()` lệch. Phân tích và sửa.

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Bean singleton được nhiều request thread dùng chung → `HashMap` bị ghi đồng thời: mất cập nhật (hai thread cùng đặt vào bin rỗng), `size` sai, mất entry khi resize, và **không có happens-before** nên thread đọc có thể thấy object `Rate` chưa khởi tạo đầy đủ. Trên Java 7 còn có thể treo CPU. Sửa: `ConcurrentHashMap` + `computeIfAbsent` (load nhanh, không blocking) — hoặc tốt hơn, dùng cache chuẩn **Caffeine** có giới hạn kích thước, TTL, refresh, metrics.

**Giải thích chi tiết:**
- Nếu dữ liệu nạp toàn bộ và đổi định kỳ (bảng tỷ giá): build map mới rồi publish qua `volatile` / `AtomicReference<Map<..>>` với `Map.copyOf` — đọc không lock, immutable, nhất quán.
- `computeIfAbsent` của `ConcurrentHashMap` giữ lock bin trong lúc hàm chạy → nếu load là gọi HTTP/DB chậm thì chặn key khác cùng bin; Caffeine `AsyncLoadingCache` hoặc pattern memoizer với `CompletableFuture` (Q41) tránh vấn đề này.
- Cache không giới hạn là memory leak chờ ngày phát nổ.
- Bài học chung: field mutable trong Spring singleton là bug concurrency tiềm ẩn.

**Câu hỏi nối tiếp:**
- *`Collections.synchronizedMap` có được không?* — Đúng về an toàn nhưng một lock cho mọi thao tác, check-then-act vẫn phải tự lock; kém hơn CHM/Caffeine.

**⚠️ Câu trả lời gây điểm trừ:**
- "Thêm `synchronized` vào method service" — đúng nhưng tuần tự hóa toàn bộ request.

**📖 Ôn lại:** [Phần 3 — Lỗi thường gặp](../01-giao-trinh/02-collections-generics.md#phan-3), [Phần 8](../01-giao-trinh/02-collections-generics.md#phan-8)

</details>

### Q17. 🔴 `IdentityHashMap` và `WeakHashMap` dùng khi nào? Có bẫy gì?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** `IdentityHashMap` so key bằng **`==`** (không dùng `equals`), hiện thực bằng open addressing (linear probing) — dùng cho thuật toán duyệt đồ thị object (serialization, deep copy, phát hiện chu trình) nơi hai object "bằng nhau" vẫn phải tách biệt. `WeakHashMap` giữ key bằng **weak reference** — entry tự biến mất khi key không còn ai tham chiếu; dùng cho metadata gắn với vòng đời object khác. Bẫy: **value tham chiếu ngược tới key** → key luôn reachable → không bao giờ được dọn.

**Giải thích chi tiết:**
- `IdentityHashMap` cố ý vi phạm hợp đồng chung của `Map` (dùng `==`), JavaDoc ghi rõ.
- `WeakHashMap` với key là `String` literal hoặc boxed `Integer` trong cache → không bao giờ bị dọn (được giữ bởi pool/cache).
- `WeakHashMap` không thread-safe; cache production dùng Caffeine `weakKeys()` thay vì tự chế.

**Câu hỏi nối tiếp:**
- *`ThreadLocal` liên quan gì?* — `ThreadLocalMap` dùng weak key tới `ThreadLocal`, nhưng value là strong → quên `remove()` trong thread pool gây leak.

**⚠️ Câu trả lời gây điểm trừ:**
- "WeakHashMap là cache tự dọn, dùng thoải mái".

**📖 Ôn lại:** [Phần 3 — Góc nhìn Senior](../01-giao-trinh/02-collections-generics.md#phan-3)

</details>

---

<a id="nhom-c"></a>
## C. `LinkedHashMap`, `TreeMap`, Set, Comparator

### Q18. 🟢 Viết LRU cache đơn giản bằng `LinkedHashMap`. Dùng nó trên production đa luồng có ổn không?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** `LinkedHashMap` có thêm danh sách liên kết đôi (`before/after`) qua mọi entry; constructor `accessOrder = true` đưa entry vừa `get/put` về cuối; override `removeEldestEntry` để xóa entry đầu khi vượt giới hạn. **Không thread-safe**, và bọc `synchronizedMap` thì mọi `get` cũng phải lock (vì `get` sửa thứ tự) → nút thắt. Production dùng **Caffeine** (W-TinyLFU, concurrent, expire/refresh, metrics) — cache mặc định của Spring Boot khi có trên classpath.

**Giải thích chi tiết:**

```java
public class LruCache<K, V> extends LinkedHashMap<K, V> {
    private final int maxEntries;
    public LruCache(int maxEntries) { super(16, 0.75f, true); this.maxEntries = maxEntries; }
    @Override protected boolean removeEldestEntry(Map.Entry<K, V> eldest) { return size() > maxEntries; }
}
// put a,b,c; get a; put d → keySet = [c, a, d]
```

- Insertion order (mặc định): put lại key đã có **không** đổi thứ tự.
- Chi phí thêm 8 byte/entry, `get/put` vẫn O(1).

**Câu hỏi nối tiếp:**
- *Vì sao duyệt `LinkedHashMap` access-order mà gọi `get` lại ném CME?* — Ở chế độ này `get` là structural modification (đổi thứ tự).

**⚠️ Câu trả lời gây điểm trừ:**
- Tự viết LRU bằng `HashMap` + `LinkedList` với `remove(Object)` O(n).

**📖 Ôn lại:** [Phần 4, mục 4.1 — `LinkedHashMap`](../01-giao-trinh/02-collections-generics.md#phan-4)

</details>

### Q19. 🟡 `TreeMap` được hiện thực thế nào? Cho ví dụ bài toán thực tế mà `TreeMap` là lựa chọn đúng.

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Red-black tree tự cân bằng → `get/put/remove` **O(log n) đảm bảo**; thứ tự theo `Comparable` hoặc `Comparator`. Giá trị thật sự nằm ở **`NavigableMap`**: `floorEntry`, `ceilingKey`, `higherKey`, `headMap/tailMap/subMap` (view), `pollFirstEntry`, `descendingMap`. Ví dụ: bảng giá bậc thang (`floorEntry(quantity)`), tra dải IP (`floorEntry(ip)` rồi kiểm tra `end`), **consistent hashing ring** (`ceilingEntry(hash)`, không có thì quay về `firstEntry()`), order book, doanh thu theo bucket thời gian (`subMap(from, to)`).

**Giải thích chi tiết:**
- Natural ordering → `put(null, ...)` ném NPE.
- Mỗi `Entry` ~40 byte (key, value, left, right, parent, color).
- Nếu chỉ cần sắp xếp **một lần** khi xuất → `HashMap` + sort cuối thường nhanh hơn; `TreeMap` khi cần truy vấn có thứ tự liên tục trong lúc dữ liệu thay đổi.
- Đa luồng → `ConcurrentSkipListMap`; hoặc copy-on-write `TreeMap` publish qua `volatile` khi đọc nhiều ghi ít (consistent hashing ring).

**Câu hỏi nối tiếp:**
- *Dọn dữ liệu cũ hơn cửa sổ thời gian?* — `map.headMap(cutoff).clear()` — view nên xóa trực tiếp trên map gốc.

**⚠️ Câu trả lời gây điểm trừ:**
- Chỉ biết "TreeMap là map có sắp xếp" mà không biết API navigable.

**📖 Ôn lại:** [Phần 4, mục 4.2 — `TreeMap`](../01-giao-trinh/02-collections-generics.md#phan-4)

</details>

### Q20. 🟢 `HashSet` được hiện thực thế nào? `LinkedHashSet`, `TreeSet` khác gì?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** `HashSet` **bọc một `HashMap`**: phần tử là key, value là object hằng dùng chung `PRESENT`; `add(e)` là `map.put(e, PRESENT) == null`. Vì vậy cùng đặc tính và cùng chi phí bộ nhớ (~32+ byte/phần tử) như `HashMap`. `LinkedHashSet` bọc `LinkedHashMap` (giữ thứ tự chèn); `TreeSet` bọc `TreeMap` (sắp xếp, `NavigableSet`: `floor`, `ceiling`, `headSet`...).

**Giải thích chi tiết:**
- "Trùng" trong `HashSet` theo `equals/hashCode`; trong `TreeSet` theo `compareTo/compare` (Q22).
- Set của enum → `EnumSet` (bit vector, nhanh và gọn hơn nhiều).
- Set đồng thời → `ConcurrentHashMap.newKeySet()`.

**Câu hỏi nối tiếp:**
- *10 triệu `Long` trong `HashSet` tốn bao nhiêu?* — Cỡ 500–600 MB; dùng `BitSet`/RoaringBitmap/primitive set (Q55).

**⚠️ Câu trả lời gây điểm trừ:**
- Không biết `HashSet` dựa trên `HashMap`.

**📖 Ôn lại:** [Phần 4, mục 4.3](../01-giao-trinh/02-collections-generics.md#phan-4)

</details>

### Q21. 🟢 `Comparable` và `Comparator` khác nhau thế nào? Hợp đồng của `compareTo` là gì?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** `Comparable<T>` (`java.lang`, `compareTo(T)`) nằm **trong class** — định nghĩa natural ordering duy nhất (`String`, `Integer`, `LocalDate`). `Comparator<T>` (`java.util`, `compare(a, b)`) nằm **bên ngoài** — nhiều cách sắp xếp khác nhau. Hợp đồng: `sgn(a.compareTo(b)) == -sgn(b.compareTo(a))`, bắc cầu, và **nên nhất quán với `equals`** (`compareTo == 0` ⇔ `equals`).

**Giải thích chi tiết:**
- Sorted collection dùng **chỉ** `compareTo/compare` để xác định trùng lặp → không nhất quán với `equals` sẽ làm mất phần tử (Q22).
- `BigDecimal` là ví dụ nổi tiếng **không** nhất quán (`1.0` vs `1.00`).

**Câu hỏi nối tiếp:**
- *Comparable có nên implement cho entity?* — Chỉ khi có thứ tự tự nhiên rõ ràng; thường dùng Comparator theo ngữ cảnh.

**⚠️ Câu trả lời gây điểm trừ:**
- Không nêu được yêu cầu nhất quán với `equals`.

**📖 Ôn lại:** [Phần 4, mục 4.4 — `Comparable` vs `Comparator`](../01-giao-trinh/02-collections-generics.md#phan-4)

</details>

### Q22. 🟡 🔍 Đoạn code in ra gì? Vì sao một nhân viên "biến mất"?

```java
Set<BigDecimal> hs = new HashSet<>(List.of(new BigDecimal("1.0"), new BigDecimal("1.00")));
Set<BigDecimal> ts = new TreeSet<>(List.of(new BigDecimal("1.0"), new BigDecimal("1.00")));
System.out.println(hs.size() + " " + ts.size());

Set<Employee> byName = new TreeSet<>(Comparator.comparing(Employee::name));
byName.add(new Employee(1, "An"));
byName.add(new Employee(2, "An"));
System.out.println(byName.size());
```

<details><summary>Đáp án</summary>

**Trả lời ngắn:** `2 1` rồi `1`. `HashSet` dùng `equals` — `BigDecimal.equals` so cả scale nên `1.0 ≠ 1.00`; `TreeSet` dùng `compareTo` — coi bằng nhau. Comparator chỉ so theo tên → hai nhân viên khác id cùng tên bị coi là **trùng**, `add` thứ hai trả `false`.

**Giải thích chi tiết:**
- Sửa comparator: thêm tie-breaker nhất quán với `equals`: `.thenComparing(Employee::id)`.
- Với `TreeMap`, put key "bằng" theo comparator sẽ **ghi đè value** của key cũ (và giữ key cũ).
- Bug thực tế: báo cáo top doanh thu dùng `TreeMap<BigDecimal, Customer>` → khách hàng cùng doanh thu bị mất.

**Câu hỏi nối tiếp:**
- *Chuẩn hóa `BigDecimal` trước khi đưa vào `HashSet`?* — `stripTrailingZeros()` hoặc `setScale` cố định theo nghiệp vụ (ví dụ 2 chữ số cho tiền).

**⚠️ Câu trả lời gây điểm trừ:**
- Trả lời `2 2` và `2`.

**📖 Ôn lại:** [Phần 4, mục 4.4](../01-giao-trinh/02-collections-generics.md#phan-4)

</details>

### Q23. 🟡 🧩 `Collections.sort` ném `IllegalArgumentException: Comparison method violates its general contract!`. Nguyên nhân thường gặp? Viết Comparator chuẩn thế nào?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** TimSort phát hiện comparator **vi phạm hợp đồng** (không đối xứng/không bắc cầu). Nguyên nhân thường gặp: so bằng **phép trừ** `a.age - b.age` (tràn số với giá trị lớn/âm); so `double` bằng `<` với `NaN`; xử lý `null` không nhất quán; comparator phụ thuộc trạng thái thay đổi trong lúc sort (giá trị ngẫu nhiên, thời gian hiện tại). Viết chuẩn bằng factory method của `Comparator`.

**Giải thích chi tiết:**

```java
Comparator<Employee> cmp = Comparator
        .comparing(Employee::department)
        .thenComparing(Employee::salary, Comparator.reverseOrder())
        .thenComparingInt(Employee::age)           // tránh boxing
        .thenComparing(Employee::id);              // tie-breaker
Comparator<Employee> nullSafe = Comparator.comparing(Employee::manager,
        Comparator.nullsLast(Comparator.comparing(Manager::name)));
```

- Exception chỉ xuất hiện với một số dữ liệu cụ thể (TimSort chỉ phát hiện khi gặp) → flaky, khó tái hiện; Java 6 (MergeSort) không ném nên migrate lên Java 7+ mới lộ.
- So `double`: `Double.compare` (xử lý `NaN`, `-0.0`).

**Câu hỏi nối tiếp:**
- *Sửa field tham gia so sánh của phần tử đang ở trong `TreeSet`/`PriorityQueue`?* — Cấu trúc hỏng âm thầm, giống key mutable của HashMap.

**⚠️ Câu trả lời gây điểm trừ:**
- "Thêm `-Djava.util.Arrays.useLegacyMergeSort=true`" như giải pháp chính — che bug thay vì sửa.

**📖 Ôn lại:** [Phần 4 — Lỗi thường gặp](../01-giao-trinh/02-collections-generics.md#phan-4)

</details>

---

<a id="nhom-d"></a>
## D. Queue, Deque, `PriorityQueue`

### Q24. 🟢 `Queue` có hai họ method `add/remove/element` và `offer/poll/peek`. Khác nhau thế nào? Vì sao hầu hết queue cấm `null`?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** `add/remove/element` **ném exception** khi thất bại (queue đầy/rỗng); `offer/poll/peek` **trả giá trị đặc biệt** (`false`/`null`). `BlockingQueue` thêm `put/take` (chặn) và `offer/poll` có timeout. Vì `poll/peek` dùng `null` làm tín hiệu "rỗng", phần tử `null` sẽ gây mơ hồ → hầu hết queue cấm (`LinkedList` là ngoại lệ lịch sử).

**Giải thích chi tiết:**
- Với queue có giới hạn, chọn họ method có chủ đích: `offer` + xử lý `false` (từ chối, trả 429) thay vì `add` ném `IllegalStateException`.
- Đừng trộn hai họ một cách vô ý trong cùng code.

**Câu hỏi nối tiếp:**
- *`put` vs `offer(e, timeout)`?* — `put` chặn vô hạn; `offer` có timeout cho phép back-pressure có kiểm soát.

**⚠️ Câu trả lời gây điểm trừ:**
- Không biết họ method nào ném exception.

**📖 Ôn lại:** [Phần 5, mục 5.1](../01-giao-trinh/02-collections-generics.md#phan-5)

</details>

### Q25. 🟢 Vì sao nên dùng `ArrayDeque` thay cho `Stack` và `LinkedList`?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** `ArrayDeque` là **mảng vòng (circular buffer)** với `head`/`tail`: `addFirst/addLast/pollFirst/pollLast` O(1) amortized, bộ nhớ liền mạch, không tạo node mỗi lần thêm. JavaDoc: *"likely to be faster than `Stack` when used as a stack, and faster than `LinkedList` when used as a queue"*. `Stack` kế thừa `Vector` (đồng bộ thừa, lộ `get(i)/add(i)` phá ngữ nghĩa stack); `LinkedList` tốn node ~24 byte/phần tử và cache miss.

**Giải thích chi tiết:**
- Stack: `push/pop/peek` (thao tác ở đầu); queue: `offer/poll`.
- `ArrayDeque` không thread-safe, không cho `null`, không truy cập theo index.
- Ứng dụng: kiểm tra ngoặc, RPN, BFS, lịch sử undo giới hạn (`pollFirst` khi đầy).

**Câu hỏi nối tiếp:**
- *Cần deque đa luồng?* — `ConcurrentLinkedDeque` (lock-free) hoặc `LinkedBlockingDeque` (blocking).

**⚠️ Câu trả lời gây điểm trừ:**
- Dùng `Stack` trong code mới.

**📖 Ôn lại:** [Phần 5, mục 5.2 — `ArrayDeque`](../01-giao-trinh/02-collections-generics.md#phan-5)

</details>

### Q26. 🟡 🔍 `PriorityQueue` được hiện thực thế nào? Đoạn code sau in ra gì?

```java
PriorityQueue<Integer> pq = new PriorityQueue<>();
for (int x : new int[]{5, 1, 4, 2, 3}) pq.offer(x);
System.out.println(pq);
System.out.println(pq.poll() + " " + pq.peek());
```

<details><summary>Đáp án</summary>

**Trả lời ngắn:** In `[1, 2, 4, 5, 3]` rồi `1 2`. `PriorityQueue` là **binary min-heap trong mảng** (con của `i` là `2i+1`, `2i+2`). `toString()`/iterator duyệt theo **thứ tự mảng heap**, **không** theo thứ tự ưu tiên — chỉ phần tử đầu chắc chắn nhỏ nhất.

**Giải thích chi tiết:**
- `offer`: thêm cuối + sift-up O(log n); `poll`: lấy gốc, đưa phần tử cuối lên + sift-down O(log n); `peek` O(1); `remove(Object)`/`contains` **O(n)**.
- Xây heap từ collection (`new PriorityQueue<>(coll)`) là **O(n)** (heapify).
- Không ổn định: hai phần tử bằng nhau không đảm bảo FIFO → thêm số thứ tự làm tie-breaker nếu cần.
- Max-heap: `new PriorityQueue<>(Comparator.reverseOrder())`.

**Câu hỏi nối tiếp:**
- *Lấy tất cả phần tử theo thứ tự?* — `poll` liên tục (O(n log n)), hoặc copy ra list rồi sort.

**⚠️ Câu trả lời gây điểm trừ:**
- Trả lời `[1, 2, 3, 4, 5]` vì nghĩ PQ đã sắp xếp.

**📖 Ôn lại:** [Phần 5, mục 5.3 — `PriorityQueue`](../01-giao-trinh/02-collections-generics.md#phan-5)

</details>

### Q27. 🔴 🧩 Tìm top-K sản phẩm bán chạy từ luồng N sự kiện (N rất lớn, K = 100). Thiết kế thế nào? Nếu cần "cập nhật độ ưu tiên" của phần tử đã có trong heap thì sao?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Giữ **min-heap kích thước K**: phần tử mới lớn hơn `peek()` thì `poll` rồi `offer` → **O(N log K)** thời gian, **O(K)** bộ nhớ, chạy được với dữ liệu streaming (tốt hơn sort O(N log N) và không cần giữ hết N). `PriorityQueue` không hỗ trợ update priority (remove là O(n)); cách thực tế: **lazy deletion** — chèn bản mới, đánh dấu bản cũ lỗi thời, bỏ qua khi `poll`; hoặc tự viết **indexed heap** (map key → vị trí trong mảng) để `decreaseKey` O(log n).

**Giải thích chi tiết:**

```java
PriorityQueue<Map.Entry<String, Long>> heap = new PriorityQueue<>(k, Map.Entry.comparingByValue());
for (var e : counts.entrySet()) {
    if (heap.size() < k) heap.offer(e);
    else if (e.getValue() > heap.peek().getValue()) { heap.poll(); heap.offer(e); }
}
```

- Nếu đếm theo cửa sổ thời gian: bucket theo phút trong `NavigableMap`, cộng các bucket trong cửa sổ rồi chạy top-K.
- Nhiều instance: top-K cục bộ mỗi node rồi merge (chính xác nếu mỗi key chỉ nằm ở một node — partition theo key), hoặc dùng sketch xấp xỉ (Count-Min + heap) khi chấp nhận sai số.
- Ứng dụng khác của heap: merge K file log đã sắp xếp (PQ kích thước K chứa dòng hiện tại của mỗi file, O(N log K)), scheduler (`DelayQueue`, `ScheduledThreadPoolExecutor`), Dijkstra.

**Câu hỏi nối tiếp:**
- *Trong indexed heap, chỗ hay sai nhất?* — Quên cập nhật map vị trí cho **cả hai** phần tử khi hoán đổi.

**⚠️ Câu trả lời gây điểm trừ:**
- "Sort toàn bộ rồi lấy K phần tử đầu" với N hàng tỷ.

**📖 Ôn lại:** [Phần 5, mục 5.3 & Góc nhìn Senior](../01-giao-trinh/02-collections-generics.md#phan-5)

</details>

---

<a id="nhom-e"></a>
## E. Iterator, fail-fast, `ConcurrentModificationException`

### Q28. 🟢 `ConcurrentModificationException` là gì? Nó có liên quan tới đa luồng không?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Collection `java.util` có `modCount` tăng mỗi khi **structural modification** (thêm/xóa, resize — không tính `set`). Iterator lưu `expectedModCount` lúc tạo; mỗi `next()` so sánh, lệch thì ném CME. **CME không liên quan tới đa luồng** — xảy ra ngay trong một thread khi sửa collection trong lúc for-each. Fail-fast chỉ là **best-effort**: không được dựa vào nó để đảm bảo đúng.

**Giải thích chi tiết:**

```java
for (String s : list) if (s.equals("a")) list.remove(s); // ❌ CME
Iterator<String> it = list.iterator();
while (it.hasNext()) if (it.next().equals("a")) it.remove(); // ✅
list.removeIf(s -> s.startsWith("b"));                      // ✅ gọn nhất
```

- Với đa luồng, `ArrayList`/`HashMap` không có memory barrier → thread duyệt có thể **không thấy** `modCount` mới → không ném CME, dữ liệu hỏng âm thầm.

**Câu hỏi nối tiếp:**
- *`set()` trong for-each có ném CME?* — Không, `set` không phải structural modification.

**⚠️ Câu trả lời gây điểm trừ:**
- "CME chỉ xảy ra khi nhiều thread sửa collection".

**📖 Ôn lại:** [Phần 6, mục 6.1 — Fail-fast](../01-giao-trinh/02-collections-generics.md#phan-6)

</details>

### Q29. 🟡 🔍 Đoạn code sau có ném CME không? In ra gì?

```java
List<String> l = new ArrayList<>(List.of("a", "b", "c"));
for (String s : l) if (s.equals("b")) l.remove(s);
System.out.println(l);
```

<details><summary>Đáp án</summary>

**Trả lời ngắn:** **Không ném CME**, in `[a, c]` — và `"c"` **không bao giờ được duyệt**. `ArrayList.Itr.hasNext()` là `cursor != size`. Sau khi xóa phần tử áp chót, `cursor == 2 == size mới` → `hasNext()` false → vòng lặp kết thúc mà không gọi `next()` (nơi kiểm tra `modCount`).

**Giải thích chi tiết:**
- Đây là minh chứng "fail-fast là best-effort": bug bỏ sót phần tử trôi qua im lặng — nguy hiểm hơn cả CME.
- Đổi `"b"` thành `"a"` → CME ở lần `next()` tiếp theo.
- Bài học: không bao giờ sửa collection trong for-each; dùng `removeIf`/iterator.

**Câu hỏi nối tiếp:**
- *Cũng code đó với `CopyOnWriteArrayList`?* — Không CME, duyệt hết `a, b, c` trên snapshot, kết quả `[a, c]`.

**⚠️ Câu trả lời gây điểm trừ:**
- Khẳng định chắc chắn "ném CME".

**📖 Ôn lại:** [Phần 6, mục 6.1](../01-giao-trinh/02-collections-generics.md#phan-6)

</details>

### Q30. 🟡 Phân biệt iterator fail-fast, snapshot và weakly consistent.

<details><summary>Đáp án</summary>

**Trả lời ngắn:**
- **Fail-fast** (`ArrayList`, `HashMap`): ném CME (best-effort) khi collection bị sửa ngoài iterator; không thread-safe.
- **Snapshot** (`CopyOnWriteArrayList/Set`): duyệt trên bản chụp mảng lúc tạo iterator; không bao giờ CME; **không thấy** thay đổi sau đó; `iterator.remove()` ném `UnsupportedOperationException`.
- **Weakly consistent** (`ConcurrentHashMap`, `ConcurrentLinkedQueue`, `ConcurrentSkipListMap`): không CME, mỗi phần tử tối đa một lần, **có thể hoặc không** phản ánh thay đổi sau khi tạo iterator.

**Giải thích chi tiết:**
- "Fail-safe" không phải thuật ngữ của JavaDoc — nói "snapshot"/"weakly consistent" cho chính xác.
- Iterator của `ConcurrentHashMap` **không** cho ảnh chụp nhất quán của toàn map; cần snapshot thì copy (`Map.copyOf`) — vẫn không atomic với ghi đồng thời.

**Câu hỏi nối tiếp:**
- *Tổng hợp số liệu từ `ConcurrentHashMap` trong lúc đang ghi có chính xác không?* — Chỉ xấp xỉ; cần chính xác thì phải chặn ghi hoặc dùng cấu trúc versioned/snapshot.

**⚠️ Câu trả lời gây điểm trừ:**
- "Iterator của ConcurrentHashMap là snapshot".

**📖 Ôn lại:** [Phần 6, mục 6.2](../01-giao-trinh/02-collections-generics.md#phan-6)

</details>

### Q31. 🔴 🧩 Production log có CME ở một service mà code không hề xóa phần tử trong for-each. Bạn nghi ngờ những gì?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** (1) **Sửa gián tiếp** — trong vòng lặp gọi method khác sửa list, hoặc listener **tự hủy đăng ký khi đang được notify**; (2) collection **dùng chung giữa các thread** (field của singleton bean); (3) duyệt `subList` sau khi list gốc thay đổi; (4) `LinkedHashMap` access-order bị `get` trong lúc duyệt; (5) stream pipeline sửa nguồn của chính nó (`list.stream().forEach(list::add)`); (6) `Collections.synchronizedList` được duyệt mà **không tự `synchronized (list)`**.

**Giải thích chi tiết:**
- Observer an toàn: danh sách listener là `CopyOnWriteArrayList` — đọc nhiều, ghi hiếm, cho phép tự hủy trong lúc notify (listener mới đăng ký trong lúc notify sẽ **không** nhận event hiện tại vì snapshot đã tạo trước).
- Wrapper `synchronized*` chỉ đồng bộ từng method; JavaDoc yêu cầu tự lock khi duyệt.
- Cách điều tra: stack trace của CME chỉ ra chỗ duyệt; tìm mọi chỗ ghi vào collection đó (Find usages), kiểm tra thread name trong log.

**Câu hỏi nối tiếp:**
- *"Sửa" bằng `catch (ConcurrentModificationException ignored)`?* — Sai: che bug, dữ liệu vẫn hỏng.

**⚠️ Câu trả lời gây điểm trừ:**
- Chỉ nghĩ tới đa luồng, hoặc chỉ nghĩ tới for-each trực tiếp.

**📖 Ôn lại:** [Phần 6 — Góc nhìn Senior](../01-giao-trinh/02-collections-generics.md#phan-6)

</details>

---

<a id="nhom-f"></a>
## F. Immutable vs unmodifiable

### Q32. 🟢 `List.of`, `Collections.unmodifiableList` và `Arrays.asList` khác nhau thế nào?

<details><summary>Đáp án</summary>

**Trả lời ngắn:**
- `List.of`/`List.copyOf`: collection **immutable** mới, không có "gốc"; cấm `null` (kể cả `contains(null)` ném NPE).
- `Collections.unmodifiableList(list)`: **view chỉ-đọc** bọc list gốc — **gốc đổi thì view đổi theo**; cho phép null.
- `Arrays.asList(arr)`: **view kích thước cố định** bọc mảng — `add/remove` ném UOE nhưng **`set` được và ghi xuyên xuống mảng**; kiểu là `Arrays$ArrayList`, **không phải** `java.util.ArrayList`.

**Giải thích chi tiết:**

```java
List<String> base = new ArrayList<>(List.of("a", "b"));
List<String> view = Collections.unmodifiableList(base);
List<String> copy = List.copyOf(base);
base.add("c");
// view = [a, b, c], copy = [a, b]
```

- `List.copyOf` của list đã immutable trả về **chính nó** (không copy).
- Tất cả chỉ immutable ở **mức collection** — phần tử mutable bên trong vẫn sửa được.

**Câu hỏi nối tiếp:**
- *Thay `Arrays.asList` bằng `List.of` khi refactor có rủi ro?* — Có: code lưu `null` hoặc gọi `contains(null)` sẽ NPE; code gọi `set` sẽ UOE.

**⚠️ Câu trả lời gây điểm trừ:**
- "Ba cái như nhau, đều immutable".

**📖 Ôn lại:** [Phần 7, mục 7.1](../01-giao-trinh/02-collections-generics.md#phan-7)

</details>

### Q33. 🟡 🔍 Đoạn code in ra gì?

```java
String[] arr = {"x", "y"};
List<String> asList = Arrays.asList(arr);
asList.set(0, "z");
System.out.println(arr[0]);

int[] nums = {1, 2, 3};
System.out.println(Arrays.asList(nums).size());

List<Integer> l = Stream.of(1, 2).toList();
l.add(3);
```

<details><summary>Đáp án</summary>

**Trả lời ngắn:** `z` (ghi xuyên xuống mảng), `1` (đây là `List<int[]>` chứa **một** phần tử là mảng — varargs `T...` không nhận được `int` primitive nên `T = int[]`), rồi `UnsupportedOperationException` vì `Stream.toList()` trả list unmodifiable.

**Giải thích chi tiết:**
- Cách đúng cho mảng primitive: `Arrays.stream(nums).boxed().toList()` hoặc `IntStream.of(nums)`.
- `Stream.toList()` (Java 16) unmodifiable nhưng **cho phép null**; `Collectors.toUnmodifiableList()` (Java 10) cấm null; `Collectors.toList()` không đảm bảo gì (thực tế là `ArrayList`) — muốn chắc mutable dùng `Collectors.toCollection(ArrayList::new)`.

**Câu hỏi nối tiếp:**
- *Migrate `collect(Collectors.toList())` sang `.toList()` có an toàn?* — Không hoàn toàn: chỗ nào sau đó `add/sort` list kết quả sẽ UOE.

**⚠️ Câu trả lời gây điểm trừ:**
- Trả lời `3` cho dòng thứ hai.

**📖 Ôn lại:** [Phần 7, mục 7.1–7.2](../01-giao-trinh/02-collections-generics.md#phan-7)

</details>

### Q34. 🟡 `Set.of` và `Map.of` có những đặc điểm bất ngờ nào?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** (1) Cấm `null` cho phần tử/key/value; (2) **trùng phần tử/key → `IllegalArgumentException`** ngay khi tạo (khác `HashSet` âm thầm bỏ trùng); (3) **thứ tự duyệt được ngẫu nhiên hóa theo mỗi lần chạy JVM** (salt) — cố ý để code không phụ thuộc thứ tự; (4) `Map.of` tối đa 10 cặp, nhiều hơn dùng `Map.ofEntries(Map.entry(k, v), ...)`.

**Giải thích chi tiết:**
- Hệ quả: test so sánh chuỗi output của `Set.of`/`Map.of`/`HashMap` → **flaky test**. So sánh bằng `equals` của collection, hoặc sort trước khi in.
- `Set.copyOf(list)` thì bỏ trùng (không ném), chỉ `Set.of(...)` với đối số trùng mới ném.
- Ưu điểm: không capacity dư, class chuyên biệt cho 0/1/2 phần tử → tiết kiệm bộ nhớ; thread-safe để chia sẻ.

**Câu hỏi nối tiếp:**
- *Cần map immutable giữ thứ tự chèn?* — Guava `ImmutableMap`, hoặc `Collections.unmodifiableMap(new LinkedHashMap<>(src))` (view của bản copy riêng).

**⚠️ Câu trả lời gây điểm trừ:**
- Không biết `Set.of` ném khi trùng hoặc thứ tự bị ngẫu nhiên hóa.

**📖 Ôn lại:** [Phần 7, mục 7.2](../01-giao-trinh/02-collections-generics.md#phan-7)

</details>

### Q35. 🟡 🧩 Production ném `UnsupportedOperationException` từ `AbstractList.add` ở một service xa nơi list được tạo. Bạn điều tra và thiết kế API thế nào để tránh?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** List được tạo bằng `Arrays.asList`/`List.of`/`stream().toList()`/`Collections.emptyList()` rồi truyền qua nhiều tầng; chỗ nhận giả định nó mutable. Điều tra: log `list.getClass()`, tìm nguồn tạo. Thiết kế: **API trả về immutable** một cách rõ ràng (và document); **bên nhận không sửa collection của người khác** — cần sửa thì tự copy (`new ArrayList<>(input)`); value object `List.copyOf` trong constructor.

**Giải thích chi tiết:**
- Interface `List` coi `add/remove` là *optional operation* — điểm thiết kế gây tranh cãi của JCF; đừng giả định khi nhận `List` từ bên ngoài.
- Nguyên tắc: "nhận rộng, trả hẹp" — nhận `Collection<? extends T>`, trả `List<T>` immutable.
- Immutable ở ranh giới API giúp chia sẻ an toàn giữa thread và chặn caller phá invariant.

**Câu hỏi nối tiếp:**
- *`Collections.unmodifiableList(internalList)` trả ra ngoài có đủ?* — Đủ để caller không sửa, nhưng caller vẫn thấy thay đổi nội bộ sau này (view) — tùy ngữ nghĩa mong muốn.

**⚠️ Câu trả lời gây điểm trừ:**
- "Bắt UOE rồi tạo list mới" ở chỗ nhận.

**📖 Ôn lại:** [Phần 7 — Góc nhìn Senior & Lỗi thường gặp](../01-giao-trinh/02-collections-generics.md#phan-7), [Phần 1, mục 1.3](../01-giao-trinh/02-collections-generics.md#phan-1)

</details>

---

<a id="nhom-g"></a>
## G. Concurrent collections

### Q36. 🟢 So sánh `Hashtable`, `Collections.synchronizedMap` và `ConcurrentHashMap`.

<details><summary>Đáp án</summary>

**Trả lời ngắn:** `Hashtable` (legacy) và `synchronizedMap` (wrapper) đều dùng **một lock cho mọi method** → tranh chấp cao; thao tác **phức hợp** (check-then-act, duyệt) vẫn **không atomic** — phải tự lock bên ngoài. `ConcurrentHashMap` được thiết kế cho đồng thời: `get` không lock, ghi lock theo từng bin hoặc CAS, có thao tác atomic (`putIfAbsent`, `compute`, `merge`), iterator weakly consistent.

**Giải thích chi tiết:**
- `synchronizedMap` vẫn có chỗ dùng: bọc `LinkedHashMap`/`TreeMap` khi tải thấp và cần một lock đơn giản.
- `Hashtable` và `ConcurrentHashMap` cấm `null`; `synchronizedMap(new HashMap<>())` cho phép.
- Collection thread-safe **không làm logic của bạn thread-safe**: chuỗi thao tác phải dùng method atomic hoặc lock ngoài.

**Câu hỏi nối tiếp:**
- *Sorted + concurrent?* — `ConcurrentSkipListMap` (lock-free, O(log n) kỳ vọng).

**⚠️ Câu trả lời gây điểm trừ:**
- "Dùng `synchronizedMap` là an toàn tuyệt đối".

**📖 Ôn lại:** [Phần 8, mục 8.1 — Ba thế hệ thread-safe collection](../01-giao-trinh/02-collections-generics.md#phan-8)

</details>

### Q37. 🔴 `ConcurrentHashMap` Java 7 và Java 8+ khác nhau thế nào về cấu trúc và cách lock?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** **Java 7**: lock striping theo `Segment[]` (mặc định 16 = `concurrencyLevel`), mỗi `Segment extends ReentrantLock` chứa một hash table nhỏ → tối đa 16 thread ghi song song; đọc không lock nhờ `volatile`; số segment cố định. **Java 8+**: bỏ Segment, cấu trúc giống `HashMap` với `volatile Node[] table`: `get` **hoàn toàn không lock**; `put` vào bin rỗng bằng **CAS**; bin có dữ liệu thì **`synchronized` trên node đầu bin**; **resize hợp tác** (nhiều thread cùng chuyển, bin đã chuyển thay bằng `ForwardingNode`); đếm size bằng `baseCount` + `CounterCell[]` (kiểu `LongAdder`).

**Giải thích chi tiết:**
- Độ song song Java 8 tỉ lệ với **số bin**, không bị giới hạn 16.
- `size()`/`mappingCount()` là **ước lượng** khi đang có cập nhật — đừng ra quyết định dựa trên nó.
- Treeify như `HashMap` (bin ≥ 8, table ≥ 64), bin cây dùng `TreeBin` có cơ chế khóa đọc/ghi riêng.
- `concurrencyLevel` ở constructor Java 8 chỉ còn là gợi ý kích thước.
- `put` gặp `ForwardingNode` (hash = `MOVED`) → thread đó **giúp resize** (`helpTransfer`) thay vì chờ.

**Câu hỏi nối tiếp:**
- *Vì sao dùng `synchronized` thay vì `ReentrantLock` cho bin?* — Lock mức bin hiếm tranh chấp; `synchronized` không tốn thêm object lock cho mỗi bin và được JVM tối ưu tốt.

**⚠️ Câu trả lời gây điểm trừ:**
- Mô tả Segment như cấu trúc hiện tại của Java 17/21.

**📖 Ôn lại:** [Phần 8, mục 8.2 — `ConcurrentHashMap` Java 7 vs Java 8+](../01-giao-trinh/02-collections-generics.md#phan-8)

</details>

### Q38. 🔴 Vì sao `ConcurrentHashMap` không cho key/value `null`, trong khi `HashMap` cho?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Với map đồng thời, `get(k) == null` phải có **một nghĩa duy nhất**: "không có key". Ở `HashMap` đơn luồng, nếu mơ hồ ta gọi thêm `containsKey(k)`; nhưng ở map đồng thời, giữa `get` và `containsKey` thread khác có thể đã thay đổi → không kiểm tra atomic được. Doug Lea cấm null để loại bỏ sự mơ hồ này; các method như `computeIfAbsent`, `merge` cũng dùng `null` làm tín hiệu "không có/xóa".

**Giải thích chi tiết:**
- Muốn biểu diễn "không có giá trị" → dùng object sentinel hoặc `Optional` làm value (cẩn thận bộ nhớ).
- Migrate `HashMap` → `ConcurrentHashMap` phải kiểm tra chỗ nào đang put null → NPE.

**Câu hỏi nối tiếp:**
- *`Hashtable` cấm null vì lý do gì?* — Lịch sử (gọi `key.hashCode()` trực tiếp), không phải lý do thiết kế đồng thời như CHM.

**⚠️ Câu trả lời gây điểm trừ:**
- "Vì nó dùng `hashCode()` của key nên null bị NPE" — đó là hệ quả, không phải lý do thiết kế.

**📖 Ôn lại:** [Phần 8, mục 8.2 — Ngữ nghĩa quan trọng](../01-giao-trinh/02-collections-generics.md#phan-8)

</details>

### Q39. 🟡 🔍 8 thread cùng chạy đoạn code sau trên một `ConcurrentHashMap` chung. Kết quả có đúng không? Sửa thế nào?

```java
ConcurrentHashMap<String, Integer> counts = new ConcurrentHashMap<>();
// mỗi thread:
for (String w : words) {
    if (!counts.containsKey(w)) counts.put(w, 1);
    else counts.put(w, counts.get(w) + 1);
}
```

<details><summary>Đáp án</summary>

**Trả lời ngắn:** **Sai** — mất cập nhật. Từng method của CHM là thread-safe, nhưng chuỗi `containsKey → get → put` là **check-then-act không atomic**: hai thread cùng đọc giá trị 5, cùng put 6. Sửa: `counts.merge(w, 1, Integer::sum)` (atomic theo key), hoặc nhanh hơn khi tranh chấp cao: `ConcurrentHashMap<String, LongAdder>` với `computeIfAbsent(w, k -> new LongAdder()).increment()`.

**Giải thích chi tiết:**
- `LongAdder` phân tán bộ đếm thành nhiều cell, tránh mọi thread CAS cùng một biến → throughput cao khi nhiều thread cùng tăng một key "nóng".
- Các method atomic khác: `putIfAbsent`, `replace(k, old, new)`, `compute`, `computeIfPresent`.
- Với `HashMap` thường thì còn tệ hơn: mất entry, size sai.

**Câu hỏi nối tiếp:**
- *Đọc kết quả `LongAdder.sum()` trong lúc đang ghi có chính xác?* — Không atomic; là giá trị xấp xỉ tại thời điểm đọc.

**⚠️ Câu trả lời gây điểm trừ:**
- "Đúng, vì ConcurrentHashMap thread-safe".

**📖 Ôn lại:** [Phần 8, mục 8.2](../01-giao-trinh/02-collections-generics.md#phan-8)

</details>

### Q40. 🔴 🧩 Sau khi dùng `cache.computeIfAbsent(key, k -> remoteClient.load(k))` trên `ConcurrentHashMap`, latency toàn hệ thống tăng vọt khi remote chậm. Vì sao? Còn bẫy nào khác của `computeIfAbsent`?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Hàm trong `compute*`/`merge` chạy **trong khi giữ lock của bin** → remote chậm 2 giây thì mọi thread ghi vào **cùng bin** (kể cả key khác) cũng bị chặn 2 giây; thread đọc cùng key cũng phải chờ. Hàm phải **ngắn, không blocking, không sửa map**. Bẫy khác: (1) **đệ quy** — hàm gọi lại `computeIfAbsent` trên chính map: Java 8 có thể lặp vô hạn, Java 9+ ném `IllegalStateException: Recursive update`; (2) **Java 8 khóa bin ngay cả khi key đã tồn tại** → key nóng thành nút thắt (workaround: `get` trước, `null` mới `computeIfAbsent`; Java 9+ đã kiểm tra nhanh không khóa); (3) gọi lồng giữa hai map có thể deadlock.

**Giải thích chi tiết:**
- Giải pháp cho load chậm: lưu **`CompletableFuture<V>`** làm value — `computeIfAbsent` chỉ tạo future (rất nhanh), việc load chạy ngoài lock (Q41); hoặc Caffeine `AsyncLoadingCache`.
- Nhớ xóa future lỗi khỏi map để lần sau thử lại, và đặt timeout cho remote call.

**Câu hỏi nối tiếp:**
- *Thread dump sẽ thấy gì?* — Nhiều thread `BLOCKED` chờ monitor của một `ConcurrentHashMap$Node` trong `computeIfAbsent`/`putVal`.

**⚠️ Câu trả lời gây điểm trừ:**
- "Tăng thread pool" hoặc không biết hàm chạy trong lock.

**📖 Ôn lại:** [Phần 8, mục 8.2 & Lỗi thường gặp](../01-giao-trinh/02-collections-generics.md#phan-8)

</details>

### Q41. 🔴 Viết `Memoizer` đảm bảo: mỗi key chỉ tính **một lần** kể cả khi 50 thread hỏi cùng lúc; key khác tính song song; việc tính **không** chạy trong lock của map; lỗi thì lần sau được thử lại.

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Lưu **future** thay vì giá trị: `ConcurrentHashMap<A, CompletableFuture<V>>`. Thread đầu tiên `putIfAbsent` future rỗng của mình thắng và chạy tính toán **ngoài lock**; các thread khác nhận future đã có và `join()` chờ. Lỗi thì `remove(key, future)` rồi `completeExceptionally` để lần sau thử lại (ý tưởng `Memoizer` trong JCIP chương 5).

**Giải thích chi tiết:**

```java
public V get(A arg) {
    CompletableFuture<V> f = cache.get(arg);
    if (f == null) {
        CompletableFuture<V> created = new CompletableFuture<>();
        f = cache.putIfAbsent(arg, created);         // chỉ một thread thắng
        if (f == null) {
            f = created;
            try { created.complete(fn.apply(arg)); }  // chạy ngoài lock của map
            catch (Throwable t) { cache.remove(arg, created); created.completeExceptionally(t); }
        }
    }
    return f.join();
}
```

- Vì sao không dùng `computeIfAbsent(arg, fn)`: hàm đắt chạy trong lock bin (Q40).
- `remove(key, value)` (hai tham số) chỉ xóa nếu value vẫn là future của mình — tránh xóa nhầm future mới của thread khác.
- Production: thêm giới hạn kích thước/TTL → dùng Caffeine.

**Câu hỏi nối tiếp:**
- *Thread chờ `join()` bị treo mãi nếu `fn` treo?* — Đặt timeout cho `fn` hoặc dùng `orTimeout` trên future.

**⚠️ Câu trả lời gây điểm trừ:**
- `synchronized` cả method `get` — đúng nhưng tuần tự hóa mọi key.

**📖 Ôn lại:** [Phần 8 — bài 8.3 Memoizer đồng thời](../01-giao-trinh/02-collections-generics.md#phan-8)

</details>

### Q42. 🟢 `CopyOnWriteArrayList` hoạt động thế nào? Khi nào nên và không nên dùng?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Mọi thao tác ghi lấy lock, **copy toàn bộ mảng**, sửa bản copy, rồi gán lại tham chiếu `volatile`. Đọc và duyệt **không lock**, trên snapshot. Phù hợp: **đọc rất nhiều, ghi rất ít, kích thước nhỏ** — danh sách listener, cấu hình routing, whitelist. Không phù hợp: ghi thường xuyên hoặc list lớn (mỗi lần ghi O(n) + rác).

**Giải thích chi tiết:**
- Iterator snapshot: không CME, `iterator.remove()` ném UOE.
- Thay thế tương đương cho cấu trúc khác: giữ một collection immutable trong `volatile`/`AtomicReference` và thay toàn bộ khi cập nhật (copy-on-write tự chế) — dùng cho bảng tỷ giá, consistent hashing ring.
- `addAll` một lần tốt hơn `add` trong vòng lặp (mỗi `add` copy một lần).

**Câu hỏi nối tiếp:**
- *Listener mới đăng ký trong lúc đang notify có nhận event hiện tại?* — Không, vì iterator đã giữ snapshot cũ.

**⚠️ Câu trả lời gây điểm trừ:**
- "Dùng thay `ArrayList` cho mọi trường hợp đa luồng".

**📖 Ôn lại:** [Phần 8, mục 8.3](../01-giao-trinh/02-collections-generics.md#phan-8)

</details>

### Q43. 🔴 🧩 Service dùng `Executors.newFixedThreadPool(20)` bị `OutOfMemoryError` khi traffic tăng đột biến. Phân tích và đề xuất thiết kế lại với `BlockingQueue`.

<details><summary>Đáp án</summary>

**Trả lời ngắn:** `newFixedThreadPool` dùng `LinkedBlockingQueue` **không giới hạn** (`Integer.MAX_VALUE`) → khi task đến nhanh hơn xử lý, queue phình to tới OOM (latency tăng vô hạn từ trước đó). `newCachedThreadPool` ngược lại dùng `SynchronousQueue` + tối đa `Integer.MAX_VALUE` thread → bùng nổ thread. Thiết kế lại: tự tạo `ThreadPoolExecutor` với **queue có giới hạn** (`ArrayBlockingQueue(n)`) và `RejectedExecutionHandler` rõ ràng (trả 429, `CallerRunsPolicy` để tạo back-pressure, hoặc ghi metric + bỏ).

**Giải thích chi tiết:**

| Queue | Đặc điểm |
|---|---|
| `ArrayBlockingQueue` | Mảng vòng, **bắt buộc** capacity, một lock + 2 condition, bộ nhớ dự đoán được |
| `LinkedBlockingQueue` | Mặc định không giới hạn, **hai lock** (put/take) → throughput tốt, tạo node mỗi lần put |
| `SynchronousQueue` | Không chứa phần tử, "chuyển tay" trực tiếp |
| `PriorityBlockingQueue`, `DelayQueue` | Không giới hạn, `put` không bao giờ chặn |

```java
if (!queue.offer(job, 100, TimeUnit.MILLISECONDS)) rejectWith429(job); // back-pressure
```

- Kích thước queue gắn với SLA: queue 1000 job × 50ms = chờ tối đa ~2,5 s với 20 worker — quá thì từ chối sớm còn hơn timeout muộn.
- Giám sát: metric độ dài queue, số task bị reject, thời gian chờ trong queue.

**Câu hỏi nối tiếp:**
- *Dừng consumer sạch thế nào?* — Poison pill hoặc interrupt + `shutdown()/awaitTermination()`.

**⚠️ Câu trả lời gây điểm trừ:**
- "Tăng `-Xmx`" hoặc "tăng số thread".

**📖 Ôn lại:** [Phần 8, mục 8.4 — `BlockingQueue`](../01-giao-trinh/02-collections-generics.md#phan-8)

</details>

---

<a id="nhom-h"></a>
## H. Generics

### Q44. 🟢 Generics giải quyết vấn đề gì? Raw type là gì và vì sao không nên dùng?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Trước Java 5 collection chứa `Object` → cast thủ công, lỗi kiểu chỉ lộ ra lúc runtime (`ClassCastException`). Generics chuyển kiểm tra kiểu về **compile-time** và bỏ cast tường minh. **Raw type** (`List` không tham số) chỉ tồn tại để tương thích ngược — dùng nó là tắt kiểm tra kiểu, lỗi dời về runtime ở chỗ xa nơi gây ra (Effective Java Item 26).

**Giải thích chi tiết:**
- Ngoại lệ hợp lệ: class literal `List.class`, `instanceof List` (hoặc `instanceof List<?>`).
- "List bất kỳ" → `List<?>`, không phải `List<Object>` (`List<String>` không gán được cho `List<Object>`).
- Unchecked warning: xử lý từng cái; `@SuppressWarnings("unchecked")` ở phạm vi nhỏ nhất kèm comment chứng minh an toàn (Item 27).

**Câu hỏi nối tiếp:**
- *`List<Object>` khác `List<?>`?* — `List<Object>` thêm được mọi thứ nhưng chỉ nhận đúng `List<Object>`; `List<?>` nhận mọi list nhưng không thêm được gì (trừ `null`).

**⚠️ Câu trả lời gây điểm trừ:**
- Không phân biệt raw type `List` và `List<?>`.

**📖 Ôn lại:** [Phần 9, mục 9.1 & 9.6](../01-giao-trinh/02-collections-generics.md#phan-9)

</details>

### Q45. 🟡 Type erasure là gì? Những điều generics trong Java **không** làm được vì erasure?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Tham số kiểu bị thay bằng **bound** của nó (`T` → `Object`, `T extends Comparable<T>` → `Comparable`), compiler chèn cast tại nơi dùng; lúc runtime `List<String>` và `List<Integer>` là cùng class `List`. Hệ quả: không `new T()`, không `new T[n]`, không `instanceof T`/`instanceof List<String>`, không có `static T`, không overload `m(List<String>)` và `m(List<Integer>)` (cùng erasure), không dùng primitive làm type argument, class generic không được kế thừa `Throwable`.

**Giải thích chi tiết:**
- Thông tin generic của **khai báo** (field, method signature, superclass) vẫn nằm trong attribute `Signature` của class file → reflection đọc được (`getGenericType()`, `getGenericSuperclass()`); của **một instance cụ thể** thì không.
- Vượt qua: truyền `Class<T>` token (`EnumMap(Class<K>)`, `Array.newInstance`), `Supplier<T>` thay `new T()`, super type token (Q46).
- Trade-off có chủ đích: tương thích ngược hoàn toàn với thư viện pre-Java 5, không phình code như C++ template; đổi lại mất kiểu lúc runtime và phải boxing primitive (Project Valhalla nhắm giải quyết).

**Câu hỏi nối tiếp:**
- *`new ArrayList<String>().getClass() == new ArrayList<Integer>().getClass()`?* — `true`.

**⚠️ Câu trả lời gây điểm trừ:**
- "Generics bị xóa hoàn toàn, reflection không đọc được gì".

**📖 Ôn lại:** [Phần 9, mục 9.2 — Type erasure](../01-giao-trinh/02-collections-generics.md#phan-9)

</details>

### Q46. 🔴 🧩 `objectMapper.readValue(json, List.class)` rồi lấy phần tử ra như `User` thì `ClassCastException`. Vì sao? `new TypeReference<List<User>>() {}` hoạt động thế nào?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** `List.class` không mang thông tin `User` (erasure) → Jackson không biết kiểu phần tử, tạo `List<LinkedHashMap>`; cast sang `User` ở chỗ dùng → CCE (heap pollution, Q51). `TypeReference` là **super type token**: tạo **anonymous subclass** `new TypeReference<List<User>>() {}` — kiểu cha có tham số cụ thể được lưu trong class file của subclass, đọc bằng `getClass().getGenericSuperclass()` → `ParameterizedType` → `getActualTypeArguments()[0]` = `List<User>`.

**Giải thích chi tiết:**

```java
abstract class TypeRef<T> {
    final Type type;
    protected TypeRef() {
        this.type = ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }
}
Type t = new TypeRef<Map<String, List<Integer>>>() {}.type; // Map<String, List<Integer>>
```

- Cặp `{}` là bắt buộc — không có subclass thì không có thông tin.
- Cùng kỹ thuật: Spring `ParameterizedTypeReference` (`RestTemplate.exchange`, `WebClient.bodyToMono`), Guava `TypeToken`.
- Trong Jackson còn có thể dùng `objectMapper.getTypeFactory().constructCollectionType(List.class, User.class)`.

**Câu hỏi nối tiếp:**
- *Vì sao chính lớp generic `TypeRef<T>` dùng trực tiếp (`new TypeRef<T>(){}` trong method generic) không hoạt động?* — Khi đó type argument là biến `T`, không phải kiểu cụ thể → nhận `TypeVariable`.

**⚠️ Câu trả lời gây điểm trừ:**
- "Jackson lỗi" mà không giải thích erasure.

**📖 Ôn lại:** [Phần 9, mục 9.2 & Lỗi thường gặp](../01-giao-trinh/02-collections-generics.md#phan-9)

</details>

### Q47. 🔴 🔍 Bridge method là gì? Đoạn code sau ném gì và ở đâu?

```java
class Node<T> {
    T data;
    public void setData(T data) { this.data = data; }
}
class IntNode extends Node<Integer> {
    @Override public void setData(Integer data) { super.setData(data); }
}
Node raw = new IntNode();
raw.setData("hello");
```

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Ném `ClassCastException` (String → Integer) ở dòng `raw.setData("hello")`. Sau erasure `Node.setData(T)` thành `setData(Object)`, còn `IntNode.setData(Integer)` có chữ ký khác → không override theo nghĩa bytecode. Compiler sinh **bridge method** synthetic `setData(Object)` trong `IntNode` gọi `setData((Integer) data)` để giữ đa hình — cast trong bridge là nơi ném CCE.

**Giải thích chi tiết:**
- Xem bằng `javap -p -c IntNode.class`: method `setData(java.lang.Object)` có cờ `ACC_BRIDGE, ACC_SYNTHETIC`.
- Bridge cũng xuất hiện với **covariant return type**.
- Reflection: `getDeclaredMethods()` trả cả bridge → lọc bằng `Method.isBridge()`; Spring có `BridgeMethodResolver` để tìm annotation trên method thật.
- Nếu không có raw type, compiler đã chặn `setData("hello")` lúc compile — raw type là thủ phạm.

**Câu hỏi nối tiếp:**
- *Vì sao annotation đôi khi "biến mất" khi đọc bằng reflection trên method generic?* — Có thể đang đọc trên bridge method (không mang annotation).

**⚠️ Câu trả lời gây điểm trừ:**
- "Lỗi compile" — raw type làm compile được (chỉ có warning).

**📖 Ôn lại:** [Phần 9, mục 9.3 — Bridge methods](../01-giao-trinh/02-collections-generics.md#phan-9)

</details>

### Q48. 🟡 Vì sao `List<Integer>` không phải là `List<Number>`, trong khi `Integer[]` lại là `Number[]`?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Generics **invariant**: nếu cho `List<Number> nums = ints`, ta có thể `nums.add(3.14)` → nhét `Double` vào list `Integer`, hỏng an toàn kiểu mà runtime không phát hiện được (erasure). Mảng **covariant** nhưng **reified** — biết kiểu phần tử lúc runtime và kiểm tra khi ghi: `Object[] objs = new String[1]; objs[0] = 42;` → `ArrayStoreException`. Mảng tự bảo vệ lúc runtime; generics bảo vệ lúc compile.

**Giải thích chi tiết:**
- Muốn linh hoạt có kiểm soát → wildcard: `List<? extends Number>` (đọc ra `Number`, không ghi được), `List<? super Integer>` (ghi `Integer`, đọc ra `Object`).
- Covariance của mảng được coi là lỗi thiết kế lịch sử (lỗi kiểu chỉ phát hiện lúc runtime) → Effective Java Item 28: ưu tiên `List` hơn mảng.

**Câu hỏi nối tiếp:**
- *Vì sao `ArrayStoreException` không cứu được mảng generic?* — Xem Q52.

**⚠️ Câu trả lời gây điểm trừ:**
- "Vì Java chưa hỗ trợ" mà không có ví dụ phá vỡ an toàn kiểu.

**📖 Ôn lại:** [Phần 9, mục 9.4 & 9.7](../01-giao-trinh/02-collections-generics.md#phan-9)

</details>

### Q49. 🟡 🔍 PECS là gì? Dòng nào compile, dòng nào không?

```java
static double sum(Collection<? extends Number> c) { ... }
static void fill(List<? super Integer> l, int n) { ... }

List<Integer> ints = new ArrayList<>();
List<Number> nums = new ArrayList<>();
List<Object> objs = new ArrayList<>();
sum(ints);         // (1)
sum(objs);         // (2)
fill(nums, 3);     // (3)
fill(objs, 3);     // (4)
List<? extends Number> ln = ints;
ln.add(1);         // (5)
Number first = ln.get(0); // (6)
```

<details><summary>Đáp án</summary>

**Trả lời ngắn:** (1) ✅, (2) ❌ (`Object` không phải `Number`), (3) ✅, (4) ✅, (5) ❌ (không ghi được vào `? extends` — compiler không biết kiểu thật là `List<Integer>` hay `List<Double>`; chỉ `add(null)` được), (6) ✅. **PECS — Producer Extends, Consumer Super** (Effective Java Item 31): tham số là nguồn bạn **đọc** từ đó → `? extends T`; là đích bạn **ghi** vào → `? super T`; vừa đọc vừa ghi → kiểu chính xác `List<T>`.

**Giải thích chi tiết:**

```java
public static <T> void copy(List<? super T> dest, List<? extends T> src) // java.util.Collections
static <T> void transfer(Collection<? extends T> src, Collection<? super T> dst) { for (T t : src) dst.add(t); }
```

- `Comparator`/`Comparable`/`Predicate`/`Consumer` là consumer → `? super T`: `Predicate<Object>` dùng được cho `List<String>`.
- Wildcard dùng cho **tham số**, **không** cho kiểu trả về (bắt caller xử lý wildcard).
- Wildcard capture: không `list.set(i, list.get(j))` được với `List<?>` → helper `private static <E> void swapHelper(List<E> l, ...)`.

**Câu hỏi nối tiếp:**
- *Vì sao `Stream.map` nhận `Function<? super T, ? extends R>`?* — Hàm *tiêu thụ* T (super) và *sản xuất* R (extends).

**⚠️ Câu trả lời gây điểm trừ:**
- Đảo ngược extends/super, hoặc khai báo kiểu trả về là `List<? extends T>`.

**📖 Ôn lại:** [Phần 9, mục 9.4 — Invariance, wildcards và PECS](../01-giao-trinh/02-collections-generics.md#phan-9)

</details>

### Q50. 🔴 Giải thích từng phần của chữ ký `public static <T extends Object & Comparable<? super T>> T max(Collection<? extends T> coll)`.

<details><summary>Đáp án</summary>

**Trả lời ngắn:**
- `Collection<? extends T>`: collection là **producer** (chỉ đọc ra) — cho phép truyền collection của kiểu con, ví dụ gọi `Collections.<Number>max(listOfInteger)` vẫn hợp lệ.
- `Comparable<? super T>`: `Comparable` là **consumer** của T — cho phép kiểu mà chính nó không implement `Comparable<chính nó>` mà kế thừa `Comparable` từ cha: `java.sql.Timestamp` (là `Comparable<java.util.Date>`), `LocalDate` (là `Comparable<ChronoLocalDate>`).
- `Object &`: bound đầu tiên quyết định **erasure** → `T` erase thành `Object`, giữ **tương thích nhị phân** với chữ ký `Object max(Collection)` trước Java 5. Không có nó, erasure sẽ là `Comparable` và bytecode cũ gọi `max` sẽ lỗi `NoSuchMethodError`.

**Giải thích chi tiết:**
- Nhiều bound: class trước, interface sau (`T extends Number & Comparable<T>`).
- Recursive bound kiểu này (`T extends Comparable<? super T>`) cũng thấy ở `Collections.sort`, `Stream.sorted`, `Comparator.naturalOrder()`.

**Câu hỏi nối tiếp:**
- *Viết `argMax(Map<K, V>)` trả key có value lớn nhất?* — `<K, V extends Comparable<? super V>> Optional<K> argMax(Map<K, V> m)` dùng `max(Map.Entry.comparingByValue())`.

**⚠️ Câu trả lời gây điểm trừ:**
- Không giải thích được `Object &` hoặc `? super T`.

**📖 Ôn lại:** [Phần 9, mục 9.4–9.5](../01-giao-trinh/02-collections-generics.md#phan-9)

</details>

### Q51. 🔴 🔍 Heap pollution là gì? Vì sao đoạn code sau ném `ClassCastException` dù không có cast nào? `@SafeVarargs` dùng khi nào?

```java
static <T> T[] toArray(T... args) { return args; }
static <T> T[] pickTwo(T a, T b, T c) { return toArray(a, b); }
String[] r = pickTwo("a", "b", "c");
```

<details><summary>Đáp án</summary>

**Trả lời ngắn:** **Heap pollution** (JLS §4.12.2): biến kiểu tham số hóa trỏ tới object không thuộc kiểu đó → CCE xảy ra ở nơi **không có cast nào trong mã nguồn**. Trong `pickTwo`, `T` bị erase thành `Object` nên varargs tạo **`Object[]`**; mảng này bị trả ra ngoài và gán cho `String[]` → cast ngầm do compiler chèn thất bại → CCE. `@SafeVarargs` là lời hứa method **không ghi vào mảng varargs và không để lộ nó ra ngoài**; chỉ đặt được trên method không override được: `static`, `final`, constructor, `private` (Java 9+).

**Giải thích chi tiết:**

```java
List<String> strings = new ArrayList<>();
List raw = strings;
raw.add(42);                 // heap pollution bắt đầu ở đây (unchecked warning)
String s = strings.get(0);   // CCE ở đây — xa nơi gây lỗi
```

- Thay thế an toàn hơn varargs generic: nhận `List<? extends T>` (Item 32).
- Ví dụ `@SafeVarargs` hợp lệ: `List.of(E...)`, `Arrays.asList`, `Stream.of`.
- Gặp CCE ở dòng không có cast → nghĩ ngay tới raw type, unchecked cast, deserialization không type token.

**Câu hỏi nối tiếp:**
- *Vì sao `@SafeVarargs` không cho method có thể override?* — Subclass override có thể vi phạm lời hứa mà annotation không kiểm soát được.

**⚠️ Câu trả lời gây điểm trừ:**
- Thêm `@SuppressWarnings("unchecked")` để "sửa".

**📖 Ôn lại:** [Phần 9, mục 9.6 — Raw types, unchecked warnings, heap pollution](../01-giao-trinh/02-collections-generics.md#phan-9)

</details>

### Q52. 🟡 Vì sao Java cấm `new List<String>[10]`? Vậy `ArrayList<E>` lưu phần tử bằng gì?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Mảng covariant + reified, generics invariant + erased — kết hợp lại sẽ phá hệ thống kiểu mà không có cảnh báo: `Object[] oa = lsa; oa[0] = List.of(42);` — runtime chỉ thấy kiểu `List` nên **không** có `ArrayStoreException`; sau đó `String s = lsa[0].get(0)` → CCE. Vì vậy cấm tạo mảng của kiểu tham số hóa (trừ `List<?>[]`). `ArrayList` dùng **`Object[]`** bên trong và cast `(E)` có `@SuppressWarnings` — an toàn vì mảng **không bao giờ lộ ra ngoài**.

**Giải thích chi tiết:**
- Muốn tạo mảng `T[]` đúng kiểu: `(T[]) Array.newInstance(type, n)` với `Class<T>` token, hoặc `list.toArray(String[]::new)` (Java 11).
- Khuyến nghị: dùng `List<E>` thay mảng khi làm việc với generics (Item 28).

**Câu hỏi nối tiếp:**
- *`list.toArray()` trả về gì?* — `Object[]`, không phải `E[]` — lý do tồn tại overload `toArray(T[])`/`toArray(IntFunction)`.

**⚠️ Câu trả lời gây điểm trừ:**
- "Vì không đủ bộ nhớ" hoặc không có ví dụ phá kiểu.

**📖 Ôn lại:** [Phần 9, mục 9.7 — Vì sao không có mảng generic](../01-giao-trinh/02-collections-generics.md#phan-9)

</details>

### Q53. 🔴 Viết một Builder có thể kế thừa mà method chain không mất kiểu con. Typesafe heterogeneous container là gì?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Dùng **recursive bound / self type**: `abstract class Builder<T extends Builder<T>>` với method trả `self()` — class con `PizzaBuilder extends Builder<PizzaBuilder>` → `new PizzaBuilder().name("x").topping("cheese")` vẫn có kiểu `PizzaBuilder`. Cùng kỹ thuật với `Enum<E extends Enum<E>>`. **Typesafe heterogeneous container** (Item 33): dùng **`Class<T>` làm key** để một container chứa giá trị nhiều kiểu mà vẫn an toàn kiểu: `<T> void put(Key<T> k, T v)`, `<T> T get(Key<T> k)` với `type.cast(...)`.

**Giải thích chi tiết:**

```java
abstract class Builder<T extends Builder<T>> {
    String name;
    T name(String n) { this.name = n; return self(); }
    protected abstract T self();
}
final class PizzaBuilder extends Builder<PizzaBuilder> {
    PizzaBuilder topping(String t) { return this; }
    protected PizzaBuilder self() { return this; }
}

public record Key<T>(String name, Class<T> type) {}
public <T> void put(Key<T> k, T v) { values.put(k, k.type().cast(v)); } // chặn heap pollution qua raw type
public <T> T get(Key<T> k) { return k.type().cast(values.get(k)); }
```

- Hạn chế `Class<T>`: không biểu diễn được `List<String>` → cần super type token.
- Type inference: diamond `<>` (Java 7; anonymous class Java 9), target typing lambda (Java 8), `var` (Java 10); đôi khi cần `Collections.<String>emptyList()`.

**Câu hỏi nối tiếp:**
- *Spring dùng container dị thể ở đâu?* — `ApplicationContext.getBean(Class<T>)`, `Environment.getProperty(key, Class<T>)`.

**⚠️ Câu trả lời gây điểm trừ:**
- Builder con override mọi method của cha chỉ để đổi kiểu trả về.

**📖 Ôn lại:** [Phần 9, mục 9.5 & bài 9.3](../01-giao-trinh/02-collections-generics.md#phan-9)

</details>

---

<a id="nhom-i"></a>
## I. Chọn collection & memory footprint

### Q54. 🟡 🧩 Chọn cấu trúc dữ liệu cho từng tình huống và nêu lý do: (a) 10 triệu user ID đã nhận khuyến mãi, kiểm tra "đã nhận chưa"; (b) lịch job theo thời điểm chạy; (c) bảng tỷ giá đọc hàng nghìn lần/giây, cập nhật 1 lần/phút; (d) đếm request theo endpoint từ 200 thread; (e) 100 thao tác gần nhất để undo.

<details><summary>Đáp án</summary>

**Trả lời ngắn:**
- (a) ID số nguyên dày đặc → **`BitSet`/RoaringBitmap** (vài MB); thưa → primitive set (fastutil `LongOpenHashSet`); nhiều instance → Redis/Bloom filter. Tránh `HashSet<Long>` (~500 MB+).
- (b) **`PriorityQueue`** theo thời điểm (hoặc `DelayQueue` nếu đa luồng; `TreeMap<Instant, List<Job>>` nếu cần hủy job theo thời điểm).
- (c) **Immutable `Map.copyOf`** giữ trong `volatile`/`AtomicReference`, thay toàn bộ mỗi phút → đọc không lock, luôn nhất quán.
- (d) **`ConcurrentHashMap<String, LongAdder>`**.
- (e) **`ArrayDeque`** giới hạn 100 (`pollFirst` khi đầy).

**Giải thích chi tiết — khung trả lời khi được hỏi "chọn collection nào":**
1. Thao tác chủ đạo và tần suất → độ phức tạp.
2. Yêu cầu thứ tự/sắp xếp/trùng lặp/null.
3. Đơn luồng hay đa luồng.
4. Kích thước dữ liệu → bộ nhớ & cache locality.
5. Mutable hay immutable ở ranh giới API.

Luôn nêu một phương án thay thế và lý do không chọn — đó là điều interviewer muốn nghe.

**Câu hỏi nối tiếp:**
- *Vì sao không dùng `ConcurrentHashMap` cho (c)?* — Được, nhưng cập nhật từng key không cho ảnh chụp nhất quán của cả bảng; swap cả map immutable đơn giản và đúng hơn.

**⚠️ Câu trả lời gây điểm trừ:**
- Chọn mà không có lập luận về bộ nhớ/thread-safety.

**📖 Ôn lại:** [Phần 10, mục 10.1 — Bảng quyết định](../01-giao-trinh/02-collections-generics.md#phan-10)

</details>

### Q55. 🔴 Ước lượng bộ nhớ của `HashMap<Long, Long>` 10 triệu entry. Có những kỹ thuật nào để giảm footprint?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Với HotSpot 64-bit, compressed oops (header 12 byte, reference 4 byte, căn lề 8): mỗi entry ≈ `Node` 32 B + key `Long` 16 B + value `Long` 16 B + ~4–8 B slot trong table ≈ **70 B** → 10 triệu entry ≈ **700 MB**, trong khi dữ liệu thật chỉ 160 MB (2 × 8 byte). Giảm: **primitive map** (fastutil `Long2LongOpenHashMap`, Eclipse Collections — open addressing, không `Node`, không boxing → ~2–4 lần nhỏ hơn), chỉ định capacity, `EnumMap/EnumSet` khi key enum, `BitSet` cho tập số nguyên dày, immutable collection, cấu trúc mảng liền mạch (CSR), off-heap (Chronicle Map) hoặc cache ngoài (Redis), xử lý streaming.

**Giải thích chi tiết:**

| Cấu trúc 1 triệu phần tử | Ước lượng |
|---|---|
| `int[]` | ~4 MB |
| `ArrayList<Integer>` | ~20 MB |
| `LinkedList<Integer>` | ~40 MB |
| `HashSet<Integer>` | ~56 MB |
| `HashMap<Long, Long>` / `TreeMap<Long, Long>` | ~72 MB |

- Kiểm chứng bằng **JOL** (`GraphLayout.parseInstance(obj).totalSize()`) hoặc `jcmd <pid> GC.class_histogram`.
- Compact Object Headers (JEP 450 thử nghiệm Java 24, JEP 519 chính thức Java 25) giảm header còn 8 byte — giảm đáng kể footprint collection nhiều object nhỏ.
- Heap > 32 GB tắt compressed oops → reference 8 byte, collection phình thêm; vì vậy heap 31 GB đôi khi chứa được nhiều hơn heap 34 GB.

**Câu hỏi nối tiếp:**
- *Heap dump có `HashMap$Node[]` khổng lồ trong dominator tree — thường là gì?* — Cache tự chế không giới hạn hoặc collection static tích lũy dần (memory leak).

**⚠️ Câu trả lời gây điểm trừ:**
- "16 byte mỗi entry (2 long)" — bỏ qua object header, boxing và `Node`.

**📖 Ôn lại:** [Phần 10, mục 10.2 — Memory footprint](../01-giao-trinh/02-collections-generics.md#phan-10)

</details>

### Q56. 🟡 "Big-O không phải tất cả" — cho ví dụ cụ thể. Bạn chứng minh lựa chọn collection của mình bằng cách nào?

<details><summary>Đáp án</summary>

**Trả lời ngắn:** Với n nhỏ (vài chục–vài trăm), quét tuyến tính `ArrayList` thường **nhanh hơn** `HashMap.get` vì không phải tính hash/`equals` qua nhiều tầng con trỏ và tận dụng cache locality. `ArrayList.add(i, e)` O(n) thường nhanh hơn `LinkedList` "O(1)" vì `arraycopy` trên bộ nhớ liền mạch. Hằng số, cache miss, allocation/GC và boxing thường quan trọng ngang Big-O. Chứng minh bằng **JMH** (có warm-up, `-prof gc` để thấy allocation rate) và **JOL** cho bộ nhớ, đo với dữ liệu kích thước thật.

**Giải thích chi tiết:**
- Microbenchmark tự viết bằng `System.nanoTime()` thường sai do JIT (dead-code elimination, OSR, warm-up).
- Ở production: async-profiler/JFR để xem hot path có thực sự là thao tác collection không — đa số trường hợp nút thắt nằm ở I/O.
- `Map<Long, Boolean>` 50 triệu entry làm "visited set" → `BitSet` vài MB thay vì vài GB — ví dụ về chọn sai cấu trúc chứ không phải sai thuật toán.

**Câu hỏi nối tiếp:**
- *Khi nào đáng tối ưu collection?* — Khi profiler chỉ ra (CPU/allocation) hoặc heap dump cho thấy collection chiếm phần lớn heap.

**⚠️ Câu trả lời gây điểm trừ:**
- Khẳng định một cấu trúc "nhanh hơn" mà không có số đo hay lập luận phần cứng.

**📖 Ôn lại:** [Phần 10 — Góc nhìn Senior](../01-giao-trinh/02-collections-generics.md#phan-10)

</details>

---

> ✅ **Tự đánh giá sau khi luyện:** trả lời trôi chảy ≥ 90% câu 🟢, ≥ 75% câu 🟡, và với mỗi câu 🔴 nói được cơ chế + ít nhất một trade-off hoặc kinh nghiệm production. Đối chiếu thêm với [Checklist tự đánh giá của Module 02](../01-giao-trinh/02-collections-generics.md#checklist-tu-danh-gia).

# Module 02 — Collections Framework & Generics

> **Mục tiêu:** sau module này bạn giải thích được cấu trúc bên trong của các collection quan trọng (`ArrayList`, `LinkedList`, `HashMap`, `LinkedHashMap`, `TreeMap`, `PriorityQueue`, `ArrayDeque`, `ConcurrentHashMap`) tới mức độ phức tạp và chi phí bộ nhớ; chọn được collection phù hợp cho từng bài toán với lập luận trade-off rõ ràng; viết được API generic đúng chuẩn (PECS, bounded type, generic method) và giải thích type erasure, bridge method, heap pollution; debug được các lỗi production như `ConcurrentModificationException`, key mutable "biến mất" khỏi `HashMap`, `UnsupportedOperationException` từ `Arrays.asList`/`List.of`, OOM do queue không giới hạn.
> **Cấp độ:** Cơ bản → Nâng cao (Senior)
> **Thời lượng gợi ý:** 6 ngày (≈ 30–35 giờ, gồm cả bài tập và dự án mini)
> **Yêu cầu trước:** Module 01 (đặc biệt Phần 2 — wrapper/autoboxing và Phần 7 — `equals`/`hashCode`); kiến thức cơ bản về cấu trúc dữ liệu (mảng, danh sách liên kết, cây, heap, bảng băm) và Big-O.
> **Nguồn tham khảo:**
> - Trong kho: [`Java/OCP-Oracle-Certified-Professional-Java-SE-17-Developer-Study-Guide-Exam-1Z0-829pdf.pdf`](../../Java/OCP-Oracle-Certified-Professional-Java-SE-17-Developer-Study-Guide-Exam-1Z0-829pdf.pdf) — Chương 9 (Collections and Generics), Chương 13 (Concurrency — phần concurrent collections)
> - Trong kho: [`Java/OCP Oracle Certified Professional Java SE 21.pdf`](../../Java/OCP%20Oracle%20Certified%20Professional%20Java%20SE%2021.pdf) — phần Collections & Generics (bổ sung Sequenced Collections của Java 21)
> - Trong kho: [`Ebook IT/OCP_ Oracle Certified Professional Java SE 8 Programmer II Study Guide_ Exam 1Z0-809.pdf`](../../Ebook%20IT/OCP_%20Oracle%20Certified%20Professional%20Java%20SE%208%20Programmer%20II%20Study%20Guide_%20Exam%201Z0-809.pdf) — Chương 3 (Generics and Collections)
> - Trong kho: [`Java/ocp-oracle-certified-professional-java-se-11-programmer-i-study-guide.pdf`](../../Java/ocp-oracle-certified-professional-java-se-11-programmer-i-study-guide.pdf) — phần Core Java APIs (ArrayList, wrapper)
> - Trong kho: [`Algorithms/1 - Introduction to Algorithms - Chia sẻ code.pdf`](../../Algorithms/1%20-%20Introduction%20to%20Algorithms%20-%20Chia%20s%E1%BA%BB%20code.pdf) — các chương Heapsort (heap, priority queue), Hash Tables, Red-Black Trees
> - Trong kho: [`Algorithms/5 - Algorithms_Nutshell - Chia sẻ code.pdf`](../../Algorithms/5%20-%20Algorithms_Nutshell%20-%20Chia%20s%E1%BA%BB%20code.pdf), [`Algorithms/4 - Algorithms for Interviews - Chia sẻ code.pdf`](../../Algorithms/4%20-%20Algorithms%20for%20Interviews%20-%20Chia%20s%E1%BA%BB%20code.pdf) — cấu trúc dữ liệu và bài tập áp dụng
> - Ngoài: *Effective Java 3rd ed.* — Item 26–33 (Generics: raw types, unchecked warnings, prefer lists to arrays, bounded wildcards/PECS, generics & varargs, typesafe heterogeneous container), Item 58 (for-each), Item 64 (refer to objects by interfaces)
> - Ngoài: *Java Concurrency in Practice* (Brian Goetz et al.) — Chương 5 (Building Blocks: synchronized vs concurrent collections, BlockingQueue)
> - Ngoài: Java Collections Framework Overview — https://docs.oracle.com/en/java/javase/21/core/java-collections-framework.html ; JavaDoc `java.util` — https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/package-summary.html
> - Ngoài: The Java Tutorials — Generics (Type Erasure, Bridge Methods, Restrictions on Generics) — https://docs.oracle.com/javase/tutorial/java/generics/
> - Ngoài: JLS §4.5–4.8 (Parameterized types, Type erasure, Raw types), §4.12.2 (Heap pollution)
> - Ngoài: OpenJDK JEPs — JEP 180 (HashMap collisions with balanced trees), JEP 269 (Collection factory methods), JEP 431 (Sequenced Collections); mã nguồn OpenJDK `java/util/HashMap.java`, `ConcurrentHashMap.java` (đọc phần comment "Implementation notes" — rất đáng giá)
> - Ngoài: JOL (Java Object Layout) — https://github.com/openjdk/jol — công cụ đo kích thước object

## Mục lục
1. [Tổng quan kiến trúc Collections Framework](#phan-1)
2. [List: `ArrayList` vs `LinkedList`](#phan-2)
3. [`HashMap` internals chuyên sâu](#phan-3)
4. [`LinkedHashMap`, `TreeMap`, `HashSet`, `TreeSet`, Comparable/Comparator](#phan-4)
5. [Queue, Deque, `PriorityQueue`, `ArrayDeque`](#phan-5)
6. [Iterator: fail-fast vs fail-safe, `ConcurrentModificationException`](#phan-6)
7. [Immutable vs unmodifiable collections](#phan-7)
8. [Concurrent collections (tổng quan)](#phan-8)
9. [Generics](#phan-9)
10. [Chọn đúng collection & memory footprint](#phan-10)
11. [Dự án mini của module](#du-an-mini)
12. [Checklist tự đánh giá](#checklist-tu-danh-gia)

---

<a id="phan-1"></a>
## 1. Tổng quan kiến trúc Collections Framework

### 1.1 Cây phân cấp

```
Iterable<E>
└── Collection<E>
    ├── SequencedCollection<E>              (Java 21)
    │   ├── List<E>                          ArrayList, LinkedList, CopyOnWriteArrayList, (Vector, Stack — legacy)
    │   ├── Deque<E>                         ArrayDeque, LinkedList, ConcurrentLinkedDeque, LinkedBlockingDeque
    │   └── SequencedSet<E>                  LinkedHashSet
    │       └── SortedSet<E> → NavigableSet<E>   TreeSet, ConcurrentSkipListSet
    ├── Set<E>                               HashSet, EnumSet, CopyOnWriteArraySet
    └── Queue<E>                             PriorityQueue, ConcurrentLinkedQueue
        └── BlockingQueue<E>                 ArrayBlockingQueue, LinkedBlockingQueue, PriorityBlockingQueue, DelayQueue, SynchronousQueue
            (TransferQueue: LinkedTransferQueue)

Map<K,V>   (KHÔNG kế thừa Collection)
├── HashMap, IdentityHashMap, WeakHashMap, EnumMap, (Hashtable — legacy)
├── SequencedMap<K,V> (Java 21) → LinkedHashMap
│   └── SortedMap<K,V> → NavigableMap<K,V>   TreeMap
└── ConcurrentMap<K,V>                       ConcurrentHashMap
    └── ConcurrentNavigableMap               ConcurrentSkipListMap
```

- **Interface** định nghĩa hợp đồng (`List`: có thứ tự, truy cập theo index, cho phép trùng; `Set`: không trùng theo `equals`; `Queue`: FIFO/ưu tiên; `Map`: key → value).
- **Abstract skeletal class** (`AbstractList`, `AbstractMap`...) giúp tự hiện thực collection nhanh.
- **Utility**: `Collections` (sort, unmodifiable/synchronized wrapper, `emptyList`, `nCopies`, `frequency`...), `Arrays` (`asList`, `sort` — dual-pivot quicksort cho primitive, TimSort cho object), factory `List.of/Set.of/Map.of` (Java 9).

**Vì sao `Map` không phải `Collection`?** `Collection` là tập các phần tử đơn; `Map` là tập các cặp. `add(E)` không có nghĩa rõ ràng với `Map`. Thay vào đó `Map` cung cấp 3 *view*: `keySet()`, `values()`, `entrySet()` — đều là view sống (sửa view → sửa map).

### 1.2 Sequenced Collections (Java 21, JEP 431)

Trước Java 21, lấy phần tử cuối của `List` là `list.get(list.size() - 1)`, của `LinkedHashSet` thì phải duyệt hết, của `Deque` là `getLast()` — không thống nhất. Java 21 thêm:

```java
List<Integer> list = new ArrayList<>(List.of(1, 2, 3));
list.getFirst(); list.getLast();         // 1, 3
list.addFirst(0); list.removeLast();
List<Integer> rev = list.reversed();     // VIEW đảo ngược, không copy

LinkedHashMap<String, Integer> m = new LinkedHashMap<>();
m.putFirst("a", 1);                      // SequencedMap
m.firstEntry(); m.pollLastEntry();
```

### 1.3 Lập trình theo interface

```java
List<Order> orders = new ArrayList<>();          // ✅ khai báo theo interface (Effective Java Item 64)
ArrayList<Order> orders2 = new ArrayList<>();    // ❌ trói vào hiện thực, trừ khi cần API riêng (ensureCapacity, trimToSize)

public List<Order> findAll() { ... }             // API trả về interface
public void process(Collection<? extends Order> in) { ... } // nhận kiểu rộng nhất có thể
```

> 💡 **Góc nhìn Senior:**
> - Phần lớn các "optional operation" (`add`, `remove`...) có thể ném `UnsupportedOperationException` — interface `List` không đảm bảo mutable. Đây là điểm thiết kế gây tranh cãi của JCF; khi nhận `List` từ bên ngoài, đừng giả định có thể sửa nó.
> - Kiểu trả về nên là `List`/`Set`/`Map` (hoặc `Collection`/`Stream` khi phù hợp), và nên trả về **collection rỗng thay vì `null`** (Effective Java Item 54).

> ⚠️ **Lỗi thường gặp:**
> - Dùng `Vector`, `Stack`, `Hashtable` trong code mới — các class legacy đồng bộ hóa từng method (chậm, mà vẫn không an toàn cho thao tác phức hợp). Dùng `ArrayList`, `ArrayDeque`, `HashMap`/`ConcurrentHashMap`.
> - Nhầm `Collection` (interface) với `Collections` (utility class).

### 🛠 Bài tập phần 1

**Bài 1.1 — Vẽ lại hierarchy (Cơ bản)**
- Đề bài: Không nhìn tài liệu, vẽ cây phân cấp gồm ít nhất 20 interface/class; sau đó dùng `Class.getInterfaces()`/`getSuperclass()` viết chương trình in cây thật của `LinkedHashMap`, `ArrayDeque`, `TreeSet`, `LinkedList` để đối chiếu.
- Tiêu chí đạt: chỉ ra được `LinkedList` vừa là `List` vừa là `Deque`; `LinkedHashMap extends HashMap`; `TreeSet` implement `NavigableSet`.

**Bài 1.2 — Map views là view sống (Trung bình)**
- Đề bài: Chứng minh bằng test: (a) `map.keySet().remove(k)` xóa entry khỏi map; (b) `map.values().removeIf(v -> v == 0)` xóa entry; (c) `entry.setValue()` trong lúc duyệt `entrySet()` sửa map; (d) `keySet().add(...)` ném gì?
- Tiêu chí đạt: 4 test pass, giải thích vì sao (d) không được hỗ trợ.

**Bài 1.3 — Tự hiện thực collection bằng skeletal class (Nâng cao)**
- Đề bài: Dùng `AbstractList<Integer>` để viết `RangeList(int from, int to)` — một `List` "ảo" không lưu phần tử (chỉ tính toán), chiếm O(1) bộ nhớ, hỗ trợ `get`, `size`, `contains` O(1) (override), `subList` hoạt động. Thêm `RandomAccess`.
- Tiêu chí đạt: `new RangeList(0, 1_000_000_000)` tạo tức thì; `for-each`, `stream()`, `indexOf`, `equals` với `List.of(...)` đều đúng.

<details>
<summary>Gợi ý lời giải</summary>

Bài 1.3:

```java
public final class RangeList extends AbstractList<Integer> implements RandomAccess {
    private final int from, to;
    public RangeList(int from, int to) { if (from > to) throw new IllegalArgumentException(); this.from = from; this.to = to; }
    @Override public Integer get(int index) { Objects.checkIndex(index, size()); return from + index; }
    @Override public int size() { return to - from; }
    @Override public boolean contains(Object o) { return o instanceof Integer i && i >= from && i < to; }
    @Override public int indexOf(Object o) { return contains(o) ? (Integer) o - from : -1; }
}
```

`AbstractList` cung cấp sẵn iterator, `equals`, `hashCode`, `subList` dựa trên `get`/`size`. Bài 1.2 (d): `keySet()` không biết value nào để put → `UnsupportedOperationException`.

</details>

---

<a id="phan-2"></a>
## 2. List: `ArrayList` vs `LinkedList`

### 2.1 `ArrayList` bên trong

```java
// rút gọn từ OpenJDK 17
public class ArrayList<E> extends AbstractList<E> implements List<E>, RandomAccess, Cloneable, Serializable {
    private static final int DEFAULT_CAPACITY = 10;
    private static final Object[] DEFAULTCAPACITY_EMPTY_ELEMENTDATA = {};
    transient Object[] elementData;   // mảng chứa phần tử
    private int size;                 // số phần tử thực tế (≤ elementData.length)
    // protected transient int modCount (kế thừa từ AbstractList)
}
```

- `new ArrayList<>()` **không cấp phát** mảng 10 phần tử ngay (Java 7u40+/8): dùng mảng rỗng dùng chung, chỉ cấp phát 10 khi `add` lần đầu → tiết kiệm bộ nhớ với các list rỗng (rất phổ biến trong entity/DTO).
- **Grow**: khi đầy, capacity mới = `old + (old >> 1)` (≈ **1.5 lần**), copy sang mảng mới bằng `Arrays.copyOf` (`System.arraycopy`, native, rất nhanh). Nhờ tăng theo cấp số nhân, `add` cuối có chi phí **amortized O(1)**.
- `remove(index)`/`add(index, e)`: dịch các phần tử phía sau bằng `System.arraycopy` → O(n − index). Xóa phần tử cuối là O(1).
- `remove` **không thu nhỏ** mảng. List từng chứa 1 triệu phần tử rồi `clear()` vẫn giữ mảng 1 triệu slot → dùng `trimToSize()` hoặc tạo list mới.
- Mảng liền mạch trong bộ nhớ → **cache-friendly**: duyệt tuần tự tận dụng CPU cache line và prefetch.

### 2.2 `LinkedList` bên trong

```java
private static class Node<E> {
    E item;
    Node<E> next;
    Node<E> prev;
}
transient Node<E> first, last;
```

- Danh sách liên kết **đôi**. `get(i)` duyệt từ đầu hoặc cuối (tùy `i < size/2`) → O(n).
- `addFirst/addLast/removeFirst/removeLast`: O(1).
- `add(index, e)` là O(n) **vì phải tìm node** trước; chỉ O(1) khi đã có sẵn vị trí qua `ListIterator`.
- Mỗi phần tử tốn thêm một object `Node` (~24 byte với compressed oops) nằm rải rác trên heap → cache miss, áp lực GC.

### 2.3 Bảng độ phức tạp

| Thao tác | `ArrayList` | `LinkedList` | `ArrayDeque` |
|---|---|---|---|
| `get(i)` / `set(i)` | **O(1)** | O(n) | — |
| `add(e)` cuối | O(1) amortized | O(1) | O(1) amortized |
| `add(0, e)` đầu | O(n) | **O(1)** | **O(1)** (`addFirst`) |
| `add(i, e)` giữa | O(n) (arraycopy) | O(n) (tìm node) + O(1) | — |
| `remove(i)` | O(n) | O(n) | — |
| `remove` qua iterator | O(n) | **O(1)** | O(n) |
| `contains` / `indexOf` | O(n) | O(n) | O(n) |
| Bộ nhớ / phần tử | ~4 byte (reference) + phần dư capacity | ~24 byte (Node) + 4 | ~4 byte |

> 💡 **Góc nhìn Senior:**
> - Trên thực tế, **`ArrayList` thắng `LinkedList` trong gần như mọi trường hợp**, kể cả nhiều trường hợp chèn giữa với n vừa phải, vì `System.arraycopy` trên bộ nhớ liền mạch cực nhanh còn duyệt `LinkedList` thì cache miss liên tục. Joshua Bloch (tác giả `LinkedList`) từng nói đùa rằng ông cũng không dùng nó. Khi cần thao tác hai đầu → `ArrayDeque`.
> - Biết trước kích thước → `new ArrayList<>(expectedSize)` tránh nhiều lần grow + copy (với 1 triệu phần tử, từ 10 lên ~1 triệu cần khoảng 30 lần grow và rác tạm thời đáng kể).
> - `RandomAccess` là marker interface: thuật toán generic (như `Collections.binarySearch`) kiểm tra nó để chọn chiến lược truy cập theo index hay theo iterator.
> - `Arrays.asList(...)` và `List.of(...)` **không phải** `java.util.ArrayList` (xem Phần 7).

> ⚠️ **Lỗi thường gặp:**
> - Duyệt `LinkedList` bằng `for (int i...; list.get(i))` → O(n²). Luôn dùng for-each/iterator khi kiểu có thể là `LinkedList`.
> - `List<Integer>.remove(1)` xóa theo index (Module 01, Phần 2).
> - Xóa nhiều phần tử trong vòng lặp index tăng dần → bỏ sót phần tử (index bị dịch). Dùng `removeIf` (O(n) cho `ArrayList` — nó đánh dấu rồi nén mảng một lần, thay vì O(n²) khi xóa từng cái).

### 🛠 Bài tập phần 2

**Bài 2.1 — Tự viết `DynamicArray<E>` (Cơ bản)**
- Đề bài: Hiện thực `DynamicArray<E>` với `add`, `add(int, E)`, `get`, `set`, `remove(int)`, `size`, grow 1.5x, `trimToSize`, iterator fail-fast (ném `ConcurrentModificationException` khi bị sửa trong lúc duyệt).
- Tiêu chí đạt: unit test cho biên (index âm, `= size`), tham chiếu tới phần tử bị xóa được gán `null` (tránh memory leak — "obsolete reference", Effective Java Item 7).

**Bài 2.2 — Benchmark ArrayList vs LinkedList (Trung bình)**
- Đề bài: Dùng JMH đo với n = 1.000, 100.000: (a) add cuối, (b) add đầu, (c) add giữa, (d) duyệt for-each tính tổng, (e) `get(i)` ngẫu nhiên, (f) xóa các phần tử chẵn bằng `removeIf` và bằng iterator.
- Tiêu chí đạt: bảng kết quả + giải thích; chỉ ra được trường hợp nào `LinkedList` thật sự thắng (nếu có) và vì sao ít gặp.

**Bài 2.3 — Xóa hàng loạt hiệu quả (Nâng cao)**
- Đề bài: Cho `ArrayList<Order>` 5 triệu phần tử, cần xóa các đơn đã hủy (~30%). So sánh 3 cách: (1) vòng `for` index lùi + `remove(i)`, (2) `iterator.remove()`, (3) `removeIf`. Đo thời gian và giải thích độ phức tạp từng cách.
- Tiêu chí đạt: giải thích (1) và (2) là O(n·k) do arraycopy mỗi lần xóa còn (3) là O(n); tự viết lại thuật toán "two-pointer compaction" tương đương `removeIf`.

<details>
<summary>Gợi ý lời giải</summary>

Bài 2.1 — grow và remove:

```java
private void grow(int minCapacity) {
    int old = elements.length;
    int newCap = Math.max(minCapacity, old + (old >> 1));
    elements = Arrays.copyOf(elements, Math.max(newCap, 10));
}
public E remove(int index) {
    Objects.checkIndex(index, size);
    @SuppressWarnings("unchecked") E old = (E) elements[index];
    int moved = size - index - 1;
    if (moved > 0) System.arraycopy(elements, index + 1, elements, index, moved);
    elements[--size] = null;   // xóa tham chiếu cũ cho GC
    modCount++;
    return old;
}
```

Bài 2.3 — compaction:

```java
int w = 0;
for (int r = 0; r < list.size(); r++) {
    Order o = list.get(r);
    if (!o.cancelled()) list.set(w++, o);
}
list.subList(w, list.size()).clear(); // xóa đuôi một lần (removeRange)
```

</details>

---

<a id="phan-3"></a>
## 3. `HashMap` internals chuyên sâu

Đây là chủ đề **được hỏi nhiều nhất** trong phỏng vấn Java ở mọi cấp. Senior cần trả lời tới mức mã nguồn.

### 3.1 Cấu trúc dữ liệu

```java
// rút gọn từ OpenJDK 17 java.util.HashMap
static final int DEFAULT_INITIAL_CAPACITY = 1 << 4;   // 16
static final int MAXIMUM_CAPACITY = 1 << 30;
static final float DEFAULT_LOAD_FACTOR = 0.75f;
static final int TREEIFY_THRESHOLD = 8;               // bin có >= 8 node → cân nhắc chuyển sang cây
static final int UNTREEIFY_THRESHOLD = 6;             // cây còn <= 6 node (khi resize split) → về lại list
static final int MIN_TREEIFY_CAPACITY = 64;           // table < 64 thì resize thay vì treeify

transient Node<K,V>[] table;   // mảng bucket ("bin"), length luôn là lũy thừa của 2
transient int size;
transient int modCount;
int threshold;                 // = capacity * loadFactor; vượt quá thì resize
final float loadFactor;

static class Node<K,V> implements Map.Entry<K,V> {
    final int hash;            // cache hash đã "spread" → không tính lại khi resize/so sánh
    final K key;
    V value;
    Node<K,V> next;            // chaining
}
```

```
table (capacity 16)
 [0] → null
 [1] → Node(hash=17,"a") → Node(hash=33,"q") → null       (collision: chaining bằng linked list)
 [2] → null
 ...
 [5] → TreeNode (red-black tree, khi bin quá dài và table >= 64)
 ...
[15] → Node(...)
```

### 3.2 Hash spreading và tính index

```java
static final int hash(Object key) {
    int h;
    return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
}
// index = (n - 1) & hash      với n = table.length (lũy thừa của 2)
```

- Vì `n` là lũy thừa của 2, `(n - 1) & hash` tương đương `hash % n` nhưng nhanh hơn nhiều (một phép AND), và luôn không âm.
- Nhược điểm: chỉ dùng **các bit thấp** của hash. Nếu nhiều key có hashCode chỉ khác nhau ở bit cao (ví dụ `Float` hoặc số nhân với 2^k), chúng rơi vào cùng bucket. Phép `h ^ (h >>> 16)` ("spreading") trộn 16 bit cao xuống bit thấp với chi phí cực thấp.
- **Key `null`**: hash = 0 → luôn ở bucket 0. `HashMap` cho phép 1 key null và nhiều value null (`Hashtable`/`ConcurrentHashMap` thì không).

### 3.3 `put` từng bước (`putVal`)

1. Nếu `table == null` → `resize()` để cấp phát lần đầu (**lazy allocation**: `new HashMap<>()` chưa tạo mảng).
2. Tính `i = (n - 1) & hash`. Nếu `table[i] == null` → đặt node mới. Xong.
3. Ngược lại, xét node đầu `p`:
   - Nếu `p.hash == hash && (p.key == key || key.equals(p.key))` → key đã tồn tại → sẽ thay value.
   - Nếu `p` là `TreeNode` → `putTreeVal` (O(log k)).
   - Ngược lại duyệt linked list; nếu không thấy thì **nối vào cuối** (tail insertion, Java 8+). Nếu độ dài list đạt `TREEIFY_THRESHOLD` (8) → `treeifyBin`: nếu `table.length < 64` thì **resize** thay vì treeify (bin dài thường do table quá nhỏ), ngược lại chuyển bin thành red-black tree.
4. Nếu key đã tồn tại: thay value (trừ khi `putIfAbsent` và value cũ khác null), trả về value cũ. **Không** tăng `modCount`.
5. Nếu thêm mới: `++modCount`; `if (++size > threshold) resize()`.

Chú ý thứ tự so sánh: **so `hash` (int) trước**, rồi `==`, rồi mới `equals` — `equals` có thể tốn kém, hash khác nhau thì chắc chắn khác key.

### 3.4 Resize

- Capacity mới = **gấp đôi**; threshold mới = gấp đôi.
- Mỗi node chỉ có thể ở **index cũ `j`** hoặc **`j + oldCap`** — quyết định bởi **một bit**: `(hash & oldCap) == 0` → giữ chỗ ("lo list"), ngược lại → `j + oldCap` ("hi list"). Không cần tính lại hash (đã cache trong `Node.hash`) hay modulo.
- Java 8 giữ **nguyên thứ tự tương đối** trong mỗi list khi tách lo/hi.
- Resize là O(n) và tạo mảng mới gấp đôi → **spike latency + rác**. Map lớn (hàng triệu entry) resize lúc đang phục vụ request có thể gây độ trễ đáng kể.

**Java 7 và vòng lặp vô hạn**: Java 7 chèn **đầu** list (head insertion) và khi `transfer` sang mảng mới thì đảo ngược thứ tự list. Hai thread cùng resize một `HashMap` (vốn không thread-safe) có thể tạo **chu trình** trong linked list → `get()` sau đó lặp vô hạn, CPU 100%. Đây là sự cố production nổi tiếng. Java 8 dùng tail insertion và giữ thứ tự nên không còn chu trình, **nhưng `HashMap` vẫn không thread-safe** (mất cập nhật, `size` sai, có thể mất entry).

### 3.5 Treeification (Java 8, JEP 180)

- Khi một bin có ≥ 8 node **và** table ≥ 64 → chuyển thành **red-black tree** (`TreeNode`) → tìm kiếm trong bin từ O(k) thành **O(log k)**.
- Khi resize tách cây mà phần còn lại ≤ 6 → chuyển về list (khoảng trễ 8/6 tránh dao động qua lại).
- Với hashCode tốt và load factor 0.75, xác suất một bin có 8 phần tử theo phân phối Poisson (tham số ~0.5) là khoảng **0.00000006** (comment trong mã nguồn HashMap) → treeification gần như chỉ xảy ra khi hashCode tệ hoặc bị **tấn công hash flooding** (attacker gửi nhiều key cùng hash, ví dụ tham số HTTP, để biến lookup thành O(n) → DoS).
- `TreeNode` sắp xếp theo: **hash** trước; nếu hash bằng nhau và key là `Comparable` cùng class thì dùng `compareTo`; nếu không thì dùng `tieBreakOrder` (tên class rồi `identityHashCode`) chỉ để cân bằng cây khi chèn. Hệ quả: nếu nhiều key **cùng hash và không `Comparable`**, lookup vẫn có thể phải duyệt cả hai nhánh → gần O(n). Muốn hưởng lợi đầy đủ, key nên implement `Comparable`.
- `TreeNode` lớn gấp khoảng 2 lần `Node` thường.

### 3.6 Độ phức tạp

| Thao tác | Trung bình | Xấu nhất (Java 7) | Xấu nhất (Java 8+, key Comparable) |
|---|---|---|---|
| `get` / `put` / `remove` / `containsKey` | O(1) | O(n) | O(log n) |
| `containsValue` | O(n) | O(n) | O(n) |
| Duyệt toàn bộ | O(capacity + size) | | |

Lưu ý dòng cuối: duyệt `HashMap` tốn thời gian tỉ lệ với **capacity**, không chỉ size. Một map từng phình to rồi bị xóa gần hết (capacity không bao giờ giảm) sẽ duyệt chậm.

### 3.7 Vì sao `equals`/`hashCode` quan trọng — và bug key mutable

```java
public class MutableKeyBug {
    static final class Point {
        int x, y;
        Point(int x, int y) { this.x = x; this.y = y; }
        @Override public boolean equals(Object o) { return o instanceof Point p && p.x == x && p.y == y; }
        @Override public int hashCode() { return 31 * x + y; }
    }

    public static void main(String[] args) {
        Map<Point, String> map = new HashMap<>();
        Point p = new Point(1, 2);
        map.put(p, "A");

        p.x = 100;                                        // sửa key SAU khi đã put → hash đổi
        System.out.println(map.get(p));                   // null — tìm ở bucket mới, entry nằm ở bucket cũ
        System.out.println(map.get(new Point(1, 2)));     // null — đúng bucket cũ nhưng equals so với key (đã là 100,2) → false
        System.out.println(map.size());                   // 1 — entry vẫn còn đó, "mồ côi", không lấy ra/xóa được → memory leak
        System.out.println(map.containsValue("A"));       // true
    }
}
```

Các biến thể thực tế: dùng entity JPA có `hashCode` dựa trên ID tự sinh làm phần tử `HashSet` trước khi persist; dùng `List`/`Set` mutable làm key; key có `hashCode` phụ thuộc field `lastModified`.

**Hợp đồng tối thiểu**: key của `HashMap` phải có `equals`/`hashCode` đúng hợp đồng và **bất biến trong suốt thời gian là key** — tốt nhất dùng `String`, wrapper, enum, record với component bất biến.

### 3.8 Khởi tạo capacity đúng cách

```java
// Muốn chứa 1000 entry mà không resize:
new HashMap<>(1000);                      // capacity → tableSizeFor(1000) = 1024, threshold = 768 → VẪN resize 1 lần khi vượt 768
new HashMap<>((int) (1000 / 0.75f) + 1);  // capacity 2048 → không resize
HashMap.newHashMap(1000);                 // Java 19+: tính sẵn theo số mapping mong muốn
// Guava: Maps.newHashMapWithExpectedSize(1000)
```

### 3.9 Các API Java 8+ hay dùng

```java
Map<String, Integer> wordCount = new HashMap<>();
for (String w : words) wordCount.merge(w, 1, Integer::sum);         // đếm tần suất gọn nhất

Map<String, List<Order>> byCustomer = new HashMap<>();
byCustomer.computeIfAbsent(order.customerId(), k -> new ArrayList<>()).add(order); // multimap

map.getOrDefault(key, 0);
map.putIfAbsent(key, value);
map.compute(key, (k, v) -> v == null ? 1 : v + 1);
map.replaceAll((k, v) -> v * 2);
map.entrySet().removeIf(e -> e.getValue() == 0);
```

- `merge`/`compute*` trả về `null` từ hàm → **xóa** entry.
- Hàm truyền vào `computeIfAbsent`/`compute` **không được sửa chính map đó** — với `HashMap` Java 9+ có thể ném `ConcurrentModificationException`; với `ConcurrentHashMap` Java 8 có thể treo vô hạn (Phần 8).

> 💡 **Góc nhìn Senior:**
> - Trả lời "HashMap hoạt động thế nào" theo khung: *cấu trúc (array of bins) → hash spreading + index bằng mask → xử lý collision (list → tree) → load factor & resize (split lo/hi) → điều kiện về key (equals/hashCode, immutable) → không thread-safe (Java 7 infinite loop; dùng ConcurrentHashMap)*.
> - Load factor 0.75 là trade-off thời gian/không gian: thấp hơn → ít collision hơn nhưng tốn bộ nhớ; cao hơn → tiết kiệm bộ nhớ nhưng bin dài hơn. Hiếm khi cần đổi.
> - Bộ nhớ: mỗi entry ≈ 32 byte `Node` + 4 byte slot trong table (chia theo load factor) + chính key/value. `HashMap<Long, Long>` 10 triệu entry ≈ 10M × (32 + 16 + 16 + ~5) ≈ **700 MB** — vì sao cache lớn trong heap cần cấu trúc chuyên dụng (primitive map, off-heap, Caffeine với giới hạn).
> - `IdentityHashMap` (so sánh `==`, open addressing với linear probing) dùng cho thuật toán duyệt đồ thị object (serialization, deep copy); `WeakHashMap` (key là weak reference) cho metadata gắn với object có vòng đời ngoài — nhưng cẩn thận: value tham chiếu ngược tới key thì key không bao giờ bị GC.

> ⚠️ **Lỗi thường gặp:**
> - Key mutable / `hashCode` không nhất quán với `equals`.
> - Chia sẻ `HashMap` giữa nhiều thread (ví dụ field của Spring singleton bean được nạp lazy) → mất dữ liệu ngẫu nhiên, Java 7 treo CPU.
> - Duyệt `keySet()` rồi `get(key)` từng phần tử thay vì duyệt `entrySet()` (tốn gấp đôi lookup).
> - `map.get(k) == null` không phân biệt "không có key" với "value là null" — dùng `containsKey` hoặc tránh lưu value null.

### 🛠 Bài tập phần 3

**Bài 3.1 — Quan sát bên trong HashMap (Cơ bản)**
- Đề bài: Viết class `BadKey` có `hashCode()` trả về hằng số. Put 10, 100, 10.000 key vào `HashMap`, đo thời gian `get`. Lặp lại với `BadKey implements Comparable<BadKey>`. Lặp lại với hashCode tốt.
- Tiêu chí đạt: bảng thời gian 3 cấu hình; giải thích bằng treeification và vai trò của `Comparable`.

**Bài 3.2 — Tự viết `SimpleHashMap<K,V>` (Trung bình)**
- Đề bài: Hiện thực hash map với separate chaining: capacity lũy thừa 2, hash spreading, load factor, resize split lo/hi giữ thứ tự, hỗ trợ key null, `get/put/remove/size/containsKey`, iterator fail-fast trên entry.
- Tiêu chí đạt: property-based test (hoặc test ngẫu nhiên 100.000 thao tác) so sánh kết quả với `java.util.HashMap`; giải thích vì sao resize chỉ cần xét một bit.

**Bài 3.3 — Tái hiện và phòng chống hash flooding (Nâng cao)**
- Đề bài: Sinh 50.000 chuỗi **khác nhau có cùng `String.hashCode()`** (gợi ý: `"Aa"` và `"BB"` cùng hash; ghép các khối này lại tạo 2^k chuỗi cùng hash). Đo thời gian put/get vào `HashMap` Java 17/21 so với `HashMap` của bài 3.2 (không treeify). Đề xuất phòng chống ở tầng ứng dụng web (giới hạn số tham số, số key JSON...).
- Tiêu chí đạt: số liệu cho thấy bản tự viết suy biến O(n²) tổng thể còn JDK vẫn chấp nhận được (vì `String` là `Comparable`); giải thích rõ.

<details>
<summary>Gợi ý lời giải</summary>

Bài 3.2 — resize:

```java
private void resize() {
    Node<K,V>[] old = table;
    int oldCap = old.length, newCap = oldCap << 1;
    @SuppressWarnings("unchecked") Node<K,V>[] nt = (Node<K,V>[]) new Node[newCap];
    for (int j = 0; j < oldCap; j++) {
        Node<K,V> loHead = null, loTail = null, hiHead = null, hiTail = null;
        for (Node<K,V> e = old[j]; e != null; e = e.next) {
            if ((e.hash & oldCap) == 0) { if (loTail == null) loHead = e; else loTail.next = e; loTail = e; }
            else                        { if (hiTail == null) hiHead = e; else hiTail.next = e; hiTail = e; }
        }
        if (loTail != null) { loTail.next = null; nt[j] = loHead; }
        if (hiTail != null) { hiTail.next = null; nt[j + oldCap] = hiHead; }
    }
    table = nt;
    threshold = (int) (newCap * loadFactor);
}
```

Một bit là đủ vì index mới = `hash & (2·oldCap − 1)`, khác index cũ `hash & (oldCap − 1)` đúng ở bit `oldCap`.

Bài 3.3 — sinh chuỗi va chạm:

```java
static List<String> collisions(int k) {             // 2^k chuỗi độ dài 2k cùng hashCode
    List<String> res = List.of("");
    for (int i = 0; i < k; i++) {
        List<String> next = new ArrayList<>();
        for (String s : res) { next.add(s + "Aa"); next.add(s + "BB"); }
        res = next;
    }
    return res;
}
```

</details>

---

<a id="phan-4"></a>
## 4. `LinkedHashMap`, `TreeMap`, `HashSet`, `TreeSet`, Comparable/Comparator

### 4.1 `LinkedHashMap`

`LinkedHashMap extends HashMap`, mỗi entry (`LinkedHashMap.Entry extends HashMap.Node`) có thêm 2 con trỏ `before`/`after` tạo thành **danh sách liên kết đôi** xuyên qua mọi entry. Hai chế độ:
- **Insertion order** (mặc định): duyệt theo thứ tự chèn lần đầu (put lại key đã có không đổi thứ tự).
- **Access order** (`accessOrder = true`): mỗi `get`/`put`/`getOrDefault`/`compute`... trên một key đưa entry đó về **cuối** danh sách → phần tử đầu là phần tử "lâu nhất chưa dùng" (least recently used).

Hook `removeEldestEntry(Map.Entry eldest)` được gọi sau mỗi lần chèn; trả về `true` thì entry đầu bị xóa → **LRU cache trong ~10 dòng**:

```java
public class LruCache<K, V> extends LinkedHashMap<K, V> {
    private final int maxEntries;

    public LruCache(int maxEntries) {
        super(16, 0.75f, true);                // accessOrder = true
        this.maxEntries = maxEntries;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > maxEntries;
    }

    public static void main(String[] args) {
        LruCache<String, Integer> cache = new LruCache<>(3);
        cache.put("a", 1); cache.put("b", 2); cache.put("c", 3);
        cache.get("a");                        // a thành "mới dùng nhất"
        cache.put("d", 4);                     // vượt 3 → xóa eldest = b
        System.out.println(cache.keySet());    // [c, a, d]
    }
}
```

Độ phức tạp `get`/`put` vẫn O(1); chi phí thêm 8 byte/entry (2 reference) và một ít thao tác con trỏ.

### 4.2 `TreeMap` — red-black tree

- Cây nhị phân tìm kiếm **tự cân bằng** (red-black): chiều cao ≤ 2·log₂(n+1) → `get/put/remove/containsKey` **O(log n)** đảm bảo.
- Mỗi `Entry` có `key, value, left, right, parent, color` (~40 byte).
- Thứ tự theo **natural ordering** (`Comparable`) hoặc `Comparator` truyền vào constructor. Với natural ordering, `put(null, ...)` ném `NullPointerException`.
- Implement `NavigableMap` — đây mới là lý do chính để dùng `TreeMap`:

```java
NavigableMap<LocalTime, String> schedule = new TreeMap<>(Map.of(
        LocalTime.of(8, 0), "Standup", LocalTime.of(10, 30), "Review", LocalTime.of(14, 0), "Planning"));
LocalTime now = LocalTime.of(11, 0);
schedule.floorEntry(now);          // 10:30=Review   (≤ now, lớn nhất)
schedule.ceilingKey(now);          // 14:00          (≥ now, nhỏ nhất)
schedule.higherKey(LocalTime.of(14, 0)); // null
schedule.headMap(now, false);      // view các key < 11:00
schedule.subMap(LocalTime.of(9, 0), true, LocalTime.of(15, 0), false); // view [9:00, 15:00)
schedule.descendingMap();          // view đảo ngược
schedule.pollFirstEntry();         // lấy và xóa entry nhỏ nhất
```

Ứng dụng thực tế: bảng giá theo bậc (tier pricing: `floorEntry(quantity)`), rate limiting theo cửa sổ thời gian, consistent hashing ring (`ceilingEntry(hash)` hoặc quay về `firstEntry()`), order book (giá → danh sách lệnh), tìm khoảng chứa một IP.

### 4.3 `HashSet`, `LinkedHashSet`, `TreeSet`

Các `Set` chuẩn đều là **lớp bọc của `Map` tương ứng**, phần tử là key, value là một object hằng dùng chung:

```java
// java.util.HashSet
private transient HashMap<E,Object> map;
private static final Object PRESENT = new Object();
public boolean add(E e) { return map.put(e, PRESENT) == null; }
```

→ `HashSet` có cùng đặc tính (và cùng chi phí bộ nhớ ~32+ byte/phần tử) như `HashMap`; `LinkedHashSet` giữ thứ tự chèn; `TreeSet` dựa trên `TreeMap` (sorted, `NavigableSet`: `floor`, `ceiling`, `headSet`, `tailSet`...).

### 4.4 `Comparable` vs `Comparator`

| | `Comparable<T>` | `Comparator<T>` |
|---|---|---|
| Package | `java.lang` | `java.util` |
| Method | `int compareTo(T o)` | `int compare(T a, T b)` |
| Nằm ở | Trong chính class (natural ordering) | Bên ngoài, nhiều cách sắp xếp khác nhau |
| Ví dụ | `String`, `Integer`, `LocalDate`, `BigDecimal` | `Comparator.comparing(Employee::salary).reversed()` |

**Hợp đồng `compareTo`**: `sgn(a.compareTo(b)) == -sgn(b.compareTo(a))`, transitive, và **nên nhất quán với `equals`** (`a.compareTo(b) == 0` ⇔ `a.equals(b)`). Sorted collection dùng **chỉ `compareTo`/`compare`** để xác định trùng lặp, không dùng `equals`:

```java
Set<BigDecimal> hs = new HashSet<>(List.of(new BigDecimal("1.0"), new BigDecimal("1.00")));
Set<BigDecimal> ts = new TreeSet<>(List.of(new BigDecimal("1.0"), new BigDecimal("1.00")));
System.out.println(hs.size()); // 2 — equals phân biệt scale
System.out.println(ts.size()); // 1 — compareTo coi bằng nhau

// Comparator chỉ so theo tên → 2 nhân viên cùng tên khác id bị coi là TRÙNG, một người "biến mất" khỏi TreeSet
Set<Employee> byName = new TreeSet<>(Comparator.comparing(Employee::name));
```

**Viết Comparator hiện đại:**

```java
Comparator<Employee> cmp = Comparator
        .comparing(Employee::department)                                // String natural order
        .thenComparing(Employee::salary, Comparator.reverseOrder())     // lương giảm dần
        .thenComparingInt(Employee::age)                                // tránh boxing
        .thenComparing(Employee::id);                                   // tie-breaker → nhất quán với equals

Comparator<Employee> nullSafe = Comparator.comparing(Employee::manager,
        Comparator.nullsLast(Comparator.comparing(Manager::name)));
```

> 💡 **Góc nhìn Senior:**
> - LRU bằng `LinkedHashMap` **không thread-safe**; bọc bằng `Collections.synchronizedMap` thì mọi `get` cũng phải lock (vì `get` sửa thứ tự!) → nút thắt cổ chai. Production dùng **Caffeine** (thuật toán W-TinyLFU, concurrent, có expire/refresh/metrics) — đó là cache mặc định của Spring Boot khi có trên classpath.
> - `TreeMap` vs `HashMap` + sort khi cần: nếu chỉ cần sắp xếp **một lần** khi xuất, `HashMap` + sort cuối thường nhanh hơn. Dùng `TreeMap` khi cần **truy vấn có thứ tự liên tục** (range, floor/ceiling) trong lúc dữ liệu thay đổi.
> - Concurrent sorted map → `ConcurrentSkipListMap` (Phần 8).

> ⚠️ **Lỗi thường gặp:**
> - Comparator bằng phép trừ: `(a, b) -> a.age - b.age` — tràn số với giá trị lớn/âm → thứ tự sai, `TimSort` có thể ném `IllegalArgumentException: Comparison method violates its general contract!`. Dùng `Integer.compare`/`comparingInt`.
> - Comparator không nhất quán với `equals` làm mất phần tử trong `TreeSet`/`TreeMap`.
> - Sửa field tham gia so sánh của phần tử đang nằm trong `TreeSet`/`PriorityQueue` → cấu trúc hỏng âm thầm (giống key mutable của HashMap).
> - Duyệt `LinkedHashMap` access-order và gọi `get` bên trong vòng lặp → `ConcurrentModificationException` (vì `get` là structural modification ở chế độ này).

### 🛠 Bài tập phần 4

**Bài 4.1 — LRU cache có thống kê (Cơ bản)**
- Đề bài: Mở rộng `LruCache` với `hitCount`, `missCount`, `evictionCount`, method `getOrLoad(K, Function<K,V> loader)`.
- Tiêu chí đạt: test kịch bản eviction đúng thứ tự LRU; giải thích tại sao `get` trong chế độ access-order là structural modification.

**Bài 4.2 — Bảng giá bậc thang & tra cứu khoảng (Trung bình)**
- Đề bài: (a) Dùng `TreeMap<Integer, BigDecimal>` hiện thực bảng giá điện theo bậc (0–50 kWh, 51–100, 101–200, ...) và hàm tính tiền lũy tiến. (b) Cho danh sách dải IP (start, end, country) không chồng nhau, viết `lookup(long ip)` O(log n).
- Tiêu chí đạt: dùng `floorEntry`/`subMap`, test biên đúng tại ranh giới bậc.

**Bài 4.3 — Consistent hashing ring (Nâng cao)**
- Đề bài: Hiện thực `ConsistentHashRing<N>` với virtual nodes (mỗi node vật lý 100–200 vnode), hash bằng MurmurHash3 hoặc `MessageDigest` MD5 (không dùng `String.hashCode`), `addNode`, `removeNode`, `nodeFor(String key)`. Thread-safe cho đọc nhiều/ghi ít.
- Tiêu chí đạt: với 5 node và 1 triệu key, độ lệch tải giữa các node < 10%; thêm node thứ 6 chỉ làm ~1/6 số key đổi chỗ; dùng `ConcurrentSkipListMap` hoặc copy-on-write `TreeMap` và giải thích lựa chọn.

<details>
<summary>Gợi ý lời giải</summary>

Bài 4.3 — tra cứu trên ring:

```java
private volatile NavigableMap<Long, N> ring = new TreeMap<>();   // copy-on-write: ghi thì tạo TreeMap mới

public N nodeFor(String key) {
    NavigableMap<Long, N> r = ring;                // đọc snapshot, không lock
    if (r.isEmpty()) throw new IllegalStateException("empty ring");
    Map.Entry<Long, N> e = r.ceilingEntry(hash(key));
    return (e != null ? e : r.firstEntry()).getValue(); // quay vòng
}

public synchronized void addNode(N node) {
    TreeMap<Long, N> copy = new TreeMap<>(ring);
    for (int i = 0; i < VNODES; i++) copy.put(hash(node + "#" + i), node);
    ring = copy;                                   // publish qua volatile
}
```

Bài 4.2 (b): `TreeMap<Long, Range>` key = start; `floorEntry(ip)` rồi kiểm tra `ip <= range.end`.

</details>

---

<a id="phan-5"></a>
## 5. Queue, Deque, `PriorityQueue`, `ArrayDeque`

### 5.1 Hai họ method: ném exception vs trả giá trị đặc biệt

| | Ném exception | Trả giá trị đặc biệt | Chặn (BlockingQueue) | Chặn có timeout |
|---|---|---|---|---|
| Thêm | `add(e)` | `offer(e)` → `false` | `put(e)` | `offer(e, t, unit)` |
| Lấy & xóa | `remove()` | `poll()` → `null` | `take()` | `poll(t, unit)` |
| Xem đầu | `element()` | `peek()` → `null` | — | — |

Vì `poll`/`peek` dùng `null` làm tín hiệu "rỗng", hầu hết queue **không cho phép phần tử `null`** (`LinkedList` là ngoại lệ lịch sử).

### 5.2 `ArrayDeque`

- **Mảng vòng (circular buffer)** với 2 chỉ số `head` và `tail`; grow khi đầy (copy và "duỗi" lại mảng).
- `addFirst/addLast/pollFirst/pollLast/peek*`: **O(1) amortized**. Không có truy cập theo index.
- Dùng làm **stack** (`push/pop/peek` — thao tác ở đầu) và **queue** (`offer/poll`). JavaDoc: *"likely to be faster than `Stack` when used as a stack, and faster than `LinkedList` when used as a queue"*.
- Không thread-safe, không cho `null`.

```java
Deque<Character> stack = new ArrayDeque<>();
for (char c : "{[()]}".toCharArray()) {
    if ("([{".indexOf(c) >= 0) stack.push(c);
    else if (stack.isEmpty() || "([{".indexOf(stack.pop()) != ")]}".indexOf(c)) throw new IllegalStateException("unbalanced");
}
```

### 5.3 `PriorityQueue` — binary heap

- **Min-heap** (mặc định theo natural ordering; truyền `Comparator` để đổi, ví dụ `Comparator.reverseOrder()` cho max-heap) lưu trong **mảng** `Object[] queue`: con của `i` là `2i+1`, `2i+2`; cha là `(i-1)/2`.
- `offer`: thêm cuối rồi **sift-up** — O(log n). `poll`: lấy gốc, đưa phần tử cuối lên gốc rồi **sift-down** — O(log n). `peek`: O(1).
- `remove(Object)`, `contains`: **O(n)** (tìm tuyến tính).
- Xây heap từ collection (`new PriorityQueue<>(collection)`): **O(n)** (heapify), không phải O(n log n).
- **Iterator và `toString()` không theo thứ tự ưu tiên** — chỉ phần tử đầu là nhỏ nhất. Muốn lấy theo thứ tự phải `poll` liên tục.
- Không ổn định (stable): hai phần tử bằng nhau không đảm bảo ra theo thứ tự vào — thêm tie-breaker (số thứ tự) nếu cần FIFO trong cùng mức ưu tiên.

**Bài toán kinh điển — Top-K** (K phần tử lớn nhất trong luồng N phần tử): giữ **min-heap kích thước K** → O(N log K) thời gian, O(K) bộ nhớ (tốt hơn sort O(N log N) và chạy được với dữ liệu streaming).

```java
static List<Integer> topK(Iterable<Integer> stream, int k) {
    PriorityQueue<Integer> heap = new PriorityQueue<>(k);          // min-heap
    for (int x : stream) {
        if (heap.size() < k) heap.offer(x);
        else if (x > heap.peek()) { heap.poll(); heap.offer(x); }  // thay phần tử nhỏ nhất
    }
    List<Integer> result = new ArrayList<>(heap);
    result.sort(Comparator.reverseOrder());
    return result;
}
```

> 💡 **Góc nhìn Senior:**
> - Ứng dụng production của priority queue: scheduler (task theo thời gian chạy — `DelayQueue`, `ScheduledThreadPoolExecutor` dùng heap bên trong), Dijkstra/A*, merge K sorted streams (merge log từ nhiều file), top-K trong analytics.
> - Muốn "update priority" của phần tử đã có trong heap: `PriorityQueue` không hỗ trợ (remove O(n) + offer). Cách thực tế: *lazy deletion* — chèn bản mới, đánh dấu bản cũ là lỗi thời, bỏ qua khi `poll`.
> - Phân biệt `Queue` dùng trong một thread (`ArrayDeque`) với hàng đợi giữa các thread (`BlockingQueue`, Phần 8) — đừng dùng `ArrayDeque` + `synchronized` tự chế khi đã có sẵn công cụ chuẩn.

> ⚠️ **Lỗi thường gặp:**
> - In `PriorityQueue` ra và tưởng nó đã sắp xếp.
> - Dùng `Stack` (kế thừa `Vector`, đồng bộ hóa, cho phép `get(i)`/`add(i)` phá vỡ ngữ nghĩa stack) — dùng `Deque`.
> - Trộn lẫn `add`/`remove` (ném exception) và `offer`/`poll` mà không chủ ý, đặc biệt với queue có giới hạn.

### 🛠 Bài tập phần 5

**Bài 5.1 — Stack/Queue bằng ArrayDeque (Cơ bản)**
- Đề bài: Viết (a) kiểm tra dấu ngoặc hợp lệ, (b) tính biểu thức hậu tố (RPN), (c) BFS trên lưới 2D tìm đường ngắn nhất — đều dùng `ArrayDeque`.
- Tiêu chí đạt: không dùng `Stack`/`LinkedList`; test biên (chuỗi rỗng, không có đường đi).

**Bài 5.2 — Merge K file log đã sắp xếp (Trung bình)**
- Đề bài: Cho K file log, mỗi file đã sắp xếp theo timestamp. Viết `Iterator<LogLine> merge(List<Path> files)` trả về dòng log theo thứ tự thời gian toàn cục, đọc lazy (không nạp hết vào RAM).
- Tiêu chí đạt: dùng `PriorityQueue` kích thước K chứa (dòng hiện tại, reader); O(N log K); đóng đủ các reader; ổn định khi timestamp trùng (ưu tiên file có index nhỏ hơn).

**Bài 5.3 — Indexed priority queue (Nâng cao)**
- Đề bài: Tự viết `IndexedMinHeap<K>` hỗ trợ `insert(key, priority)`, `decreaseKey(key, newPriority)` O(log n), `poll`, `contains(key)` O(1) (dùng `HashMap<K, Integer>` lưu vị trí trong mảng heap). Dùng nó để hiện thực Dijkstra và so sánh với phiên bản lazy deletion dùng `PriorityQueue`.
- Tiêu chí đạt: kết quả Dijkstra giống nhau trên đồ thị ngẫu nhiên 100.000 đỉnh; so sánh thời gian và bộ nhớ, nêu nhận xét.

<details>
<summary>Gợi ý lời giải</summary>

Bài 5.2:

```java
record Head(LogLine line, int fileIdx, BufferedReader reader) {}
PriorityQueue<Head> pq = new PriorityQueue<>(
        Comparator.comparing((Head h) -> h.line().timestamp()).thenComparingInt(Head::fileIdx));
// khởi tạo: đọc dòng đầu của mỗi file → offer
// next(): Head h = pq.poll(); đọc dòng tiếp của h.reader; nếu còn → offer(new Head(next, h.fileIdx, h.reader)); else đóng reader
```

Bài 5.3: khi hoán đổi 2 phần tử trong mảng heap, nhớ cập nhật `positions.put(key, index)` cho cả hai — đây là chỗ hay sai nhất.

</details>

---

<a id="phan-6"></a>
## 6. Iterator: fail-fast vs fail-safe, `ConcurrentModificationException`

### 6.1 Fail-fast

Các collection trong `java.util` (`ArrayList`, `HashMap`, ...) có field `modCount` tăng mỗi khi có **structural modification** (thêm/xóa phần tử, resize — *không* tính `set` thay giá trị). Iterator lưu `expectedModCount` khi được tạo; mỗi `next()` kiểm tra `modCount != expectedModCount` → ném `ConcurrentModificationException` (CME).

```java
List<String> list = new ArrayList<>(List.of("a", "b", "c", "d"));

for (String s : list) {               // for-each = iterator ngầm
    if (s.equals("a")) list.remove(s); // ❌ CME ở lần next() tiếp theo
}

Iterator<String> it = list.iterator();
while (it.hasNext()) {
    if (it.next().equals("a")) it.remove();  // ✅ xóa qua iterator: cập nhật expectedModCount
}

list.removeIf(s -> s.startsWith("b"));        // ✅ gọn nhất (Java 8)
```

Điểm quan trọng:
- **CME không liên quan gì tới đa luồng** — xảy ra ngay trong một thread. Tên gây hiểu lầm.
- Fail-fast chỉ là **best-effort** (JavaDoc nói rõ): không được viết code dựa vào CME để đảm bảo đúng. Ví dụ nổi tiếng: xóa phần tử **áp chót** trong for-each **không ném CME** — vì sau khi xóa, `size` giảm và `hasNext()` (`cursor != size`) trả về `false` → vòng lặp kết thúc âm thầm, bỏ sót phần tử cuối mà không ai biết.

```java
List<String> l = new ArrayList<>(List.of("a", "b", "c"));
for (String s : l) if (s.equals("b")) l.remove(s);   // KHÔNG ném CME! "c" không được duyệt
System.out.println(l);                                // [a, c]
```

- Với đa luồng, `HashMap`/`ArrayList` không có memory barrier nên thread duyệt có thể **không thấy** `modCount` mới → không có gì đảm bảo CME được ném; dữ liệu có thể hỏng âm thầm.

### 6.2 Fail-safe / weakly consistent

Thuật ngữ "fail-safe" không có trong JavaDoc; JDK dùng:
- **Snapshot iterator** (`CopyOnWriteArrayList`, `CopyOnWriteArraySet`): duyệt trên bản chụp mảng tại thời điểm tạo iterator; không bao giờ ném CME; **không thấy** thay đổi sau đó; `iterator.remove()` ném `UnsupportedOperationException`.
- **Weakly consistent iterator** (`ConcurrentHashMap`, `ConcurrentLinkedQueue`, `ConcurrentSkipListMap`, hầu hết `java.util.concurrent`): không ném CME, duyệt mỗi phần tử tối đa một lần, **có thể hoặc không** phản ánh thay đổi xảy ra sau khi tạo iterator.

| | Fail-fast | Snapshot | Weakly consistent |
|---|---|---|---|
| Ví dụ | `ArrayList`, `HashMap` | `CopyOnWriteArrayList` | `ConcurrentHashMap` |
| Sửa khi đang duyệt | CME (best-effort) | Không thấy thay đổi | Có thể thấy |
| Thread-safe | ❌ | ✅ | ✅ |
| Chi phí | Thấp | Copy mảng mỗi lần ghi | Thấp |

> 💡 **Góc nhìn Senior:**
> - Gặp CME trên production thường do: (1) sửa collection trong for-each (kể cả gián tiếp — gọi method khác sửa list, listener tự hủy đăng ký khi đang được notify); (2) collection dùng chung giữa các thread (thường là field của singleton); (3) duyệt `subList` sau khi list gốc thay đổi; (4) `LinkedHashMap` access-order bị `get` trong khi duyệt; (5) stream pipeline sửa nguồn của chính nó (`list.stream().forEach(list::add)`).
> - Pattern observer an toàn: danh sách listener là `CopyOnWriteArrayList` (đọc nhiều, ghi hiếm, cho phép listener tự hủy trong lúc notify).
> - `Collections.synchronizedList` vẫn **phải tự `synchronized (list)` khi duyệt** — wrapper chỉ đồng bộ từng method đơn lẻ (JavaDoc ghi rõ).

> ⚠️ **Lỗi thường gặp:**
> - "Sửa" CME bằng cách bắt và bỏ qua exception.
> - Dùng index-based loop để xóa và quên giảm index.
> - Nghĩ rằng `ConcurrentHashMap` iterator cho "ảnh chụp nhất quán" — không phải.

### 🛠 Bài tập phần 6

**Bài 6.1 — Tái hiện CME và trường hợp "im lặng" (Cơ bản)**
- Đề bài: Viết test cho: xóa phần tử đầu trong for-each (CME), xóa phần tử áp chót (không CME, bỏ sót), `set()` trong for-each (không CME — vì sao?), thêm vào `HashMap` khi duyệt `keySet()`.
- Tiêu chí đạt: giải thích bằng `modCount`, `cursor`, `hasNext()`.

**Bài 6.2 — Event bus an toàn (Trung bình)**
- Đề bài: Viết `EventBus` cho phép listener tự hủy đăng ký hoặc đăng ký listener mới **ngay trong lúc** đang xử lý event. Viết 2 phiên bản: `ArrayList` (tái hiện CME) và `CopyOnWriteArrayList`.
- Tiêu chí đạt: phiên bản 2 không CME; giải thích listener mới đăng ký trong lúc notify có nhận event hiện tại không và vì sao.

**Bài 6.3 — Iterator fail-fast cho cấu trúc tự viết (Nâng cao)**
- Đề bài: Bổ sung `modCount` và iterator fail-fast (có `remove()` hợp lệ) cho `SimpleHashMap` ở Bài 3.2, và `spliterator()` để `stream()` dùng được (có thể dùng `Spliterators.spliterator(iterator, size, characteristics)`).
- Tiêu chí đạt: test đầy đủ: `remove()` hai lần liên tiếp ném `IllegalStateException`; sửa map ngoài iterator ném CME; stream đếm đúng.

<details>
<summary>Gợi ý lời giải</summary>

Bài 6.1: `ArrayList.Itr.hasNext()` là `cursor != size`. Sau khi xóa phần tử áp chót (index `size-2`), `cursor == size-1 == size mới` → `hasNext()` false → thoát vòng lặp mà không gọi `next()` (nơi kiểm tra modCount). `set()` không phải structural modification nên không tăng `modCount`.

Bài 6.2: snapshot iterator của `CopyOnWriteArrayList` được tạo trước khi listener mới được thêm → listener mới **không** nhận event đang phát.

</details>

---

<a id="phan-7"></a>
## 7. Immutable vs unmodifiable collections

### 7.1 Ba thứ trông giống nhau nhưng rất khác

| | `List.of(...)` / `List.copyOf` (Java 9/10) | `Collections.unmodifiableList(list)` | `Arrays.asList(arr)` |
|---|---|---|---|
| Bản chất | Collection **immutable** mới | **View chỉ-đọc** bọc list gốc | **View kích thước cố định** bọc mảng gốc |
| `add`/`remove` | `UnsupportedOperationException` | `UnsupportedOperationException` | `UnsupportedOperationException` |
| `set(i, e)` | UOE | UOE | ✅ **được** — ghi xuyên xuống mảng gốc |
| List/mảng gốc thay đổi | Không ảnh hưởng (không có gốc) | **View thay đổi theo** | **View thay đổi theo** (và ngược lại) |
| Phần tử `null` | ❌ NPE khi tạo; `contains(null)` cũng ném NPE | ✅ | ✅ |
| Kiểu thực tế | `ImmutableCollections.ListN`/`List12` | `Collections.UnmodifiableRandomAccessList` | `Arrays$ArrayList` (**không phải** `java.util.ArrayList`) |
| Serializable | ✅ (dạng đặc biệt `CollSer`) | Nếu list gốc serializable | ✅ |

```java
List<String> base = new ArrayList<>(List.of("a", "b"));
List<String> view = Collections.unmodifiableList(base);
List<String> copy = List.copyOf(base);
base.add("c");
System.out.println(view);  // [a, b, c]  ← "unmodifiable" nhưng KHÔNG immutable
System.out.println(copy);  // [a, b]

String[] arr = {"x", "y"};
List<String> asList = Arrays.asList(arr);
asList.set(0, "z");
System.out.println(arr[0]);           // z — ghi xuyên
// asList.add("w");                   // UOE

int[] nums = {1, 2, 3};
System.out.println(Arrays.asList(nums).size()); // 1 !! — List<int[]> chứa một phần tử là mảng
List<Integer> boxed = Arrays.stream(nums).boxed().toList(); // cách đúng
```

### 7.2 Các nguồn immutable/unmodifiable khác

- `Set.of`, `Map.of` (tối đa 10 cặp), `Map.ofEntries(Map.entry(k, v), ...)`, `Map.copyOf`: không cho `null`, `Set.of`/`Map.of` **ném `IllegalArgumentException` nếu trùng phần tử/key**, và **thứ tự duyệt được ngẫu nhiên hóa theo mỗi lần chạy JVM** (salt) — cố ý để code không phụ thuộc thứ tự.
- `Stream.toList()` (Java 16): trả về list unmodifiable, **cho phép null**.
- `Collectors.toList()`: **không đảm bảo** mutable hay immutable (thực tế là `ArrayList` — nhưng không được dựa vào). `Collectors.toUnmodifiableList()` (Java 10) — không cho null. Muốn chắc chắn mutable: `Collectors.toCollection(ArrayList::new)`.
- `Collections.emptyList()`, `singletonList(x)`: immutable, nhẹ.
- Thư viện: Guava `ImmutableList`/`ImmutableMap` (giữ thứ tự chèn, builder), Eclipse Collections, Vavr (persistent collections).

**Deep vs shallow**: tất cả các dạng trên chỉ immutable ở **mức collection** — phần tử mutable bên trong vẫn sửa được.

> 💡 **Góc nhìn Senior:**
> - API public nên trả về immutable collection (`List.copyOf` trong constructor của value object, `toList()` ở cuối stream) → caller không thể phá invariant, và có thể chia sẻ an toàn giữa thread.
> - `List.of` tiết kiệm bộ nhớ: không có capacity dư, các class chuyên biệt cho 0/1/2 phần tử. `List.copyOf` của một list đã immutable trả về **chính nó** (không copy).
> - Khi migrate code: thay `Arrays.asList` bằng `List.of` có thể làm vỡ code đang lưu `null` hoặc gọi `contains(null)` (NPE). Kiểm tra kỹ.

> ⚠️ **Lỗi thường gặp:**
> - Nhận `List` từ `Arrays.asList`/`List.of`/`stream().toList()` rồi gọi `add` → UOE trên production (thường ở chỗ xa nơi tạo list).
> - Nghĩ `Collections.unmodifiableList` bảo vệ được khỏi thay đổi từ người giữ list gốc.
> - Test phụ thuộc thứ tự duyệt của `Set.of`/`Map.of`/`HashMap` → flaky test.

### 🛠 Bài tập phần 7

**Bài 7.1 — Bảng hành vi (Cơ bản)**
- Đề bài: Viết test tham số hóa (JUnit 5 `@MethodSource`) chạy cùng một bộ thao tác (`add`, `set`, `remove`, `contains(null)`, sửa nguồn rồi đọc lại) trên 6 loại: `new ArrayList`, `List.of`, `List.copyOf`, `Collections.unmodifiableList`, `Arrays.asList`, `stream().toList()`.
- Tiêu chí đạt: bảng kết quả khớp với Phần 7.1; ghi chú điểm bất ngờ.

**Bài 7.2 — Value object với collection (Trung bình)**
- Đề bài: Viết `record Playlist(String name, List<Song> songs)` đảm bảo immutable thực sự (Song cũng là record), có method `with(Song)` và `without(int index)` trả về Playlist mới. So sánh chi phí bộ nhớ/thời gian khi thêm 10.000 bài liên tiếp với phiên bản mutable.
- Tiêu chí đạt: test chứng minh caller không sửa được playlist; nhận xét O(n²) của cách copy toàn bộ mỗi lần và đề xuất giải pháp (builder, persistent collection).

**Bài 7.3 — Defensive API cho thư viện (Nâng cao)**
- Đề bài: Viết class `Permissions` nhận `Map<String, Set<String>>` (role → quyền) từ caller, lưu trữ immutable sâu, cung cấp view `Map<String, Set<String>> asMap()` không cần copy mỗi lần gọi, và `Permissions merge(Permissions other)`.
- Tiêu chí đạt: sửa map/set gốc của caller sau khi tạo không ảnh hưởng; sửa kết quả `asMap()` ném UOE ở mọi tầng; không có `null`.

<details>
<summary>Gợi ý lời giải</summary>

Bài 7.3:

```java
public final class Permissions {
    private final Map<String, Set<String>> map;
    public Permissions(Map<String, Set<String>> src) {
        Map<String, Set<String>> tmp = new HashMap<>();
        src.forEach((role, perms) -> tmp.put(Objects.requireNonNull(role), Set.copyOf(perms)));
        this.map = Map.copyOf(tmp);   // immutable cả 2 tầng
    }
    public Map<String, Set<String>> asMap() { return map; } // an toàn trả thẳng
    public Permissions merge(Permissions o) {
        Map<String, Set<String>> m = new HashMap<>();
        Stream.of(map, o.map).flatMap(x -> x.entrySet().stream())
              .forEach(e -> m.computeIfAbsent(e.getKey(), k -> new HashSet<>()).addAll(e.getValue()));
        return new Permissions(m);
    }
}
```

</details>

---

<a id="phan-8"></a>
## 8. Concurrent collections (tổng quan)

> Phần này chỉ đi đủ sâu để chọn đúng collection và trả lời câu hỏi về internals. Java Memory Model, lock, CAS, thread pool, `CompletableFuture`, virtual threads... được học chi tiết ở **module Concurrency & Multithreading** của giáo trình.

### 8.1 Ba thế hệ "thread-safe collection"

1. **Legacy synchronized** (`Vector`, `Hashtable`): mọi method `synchronized` trên `this`.
2. **Synchronized wrapper** (`Collections.synchronizedList/Map`): bọc collection thường, mỗi method lock một mutex. Vấn đề chung của (1) và (2):
   - Một lock cho toàn bộ → tranh chấp cao.
   - Thao tác **phức hợp** (check-then-act: `if (!map.containsKey(k)) map.put(k, v)`) vẫn **không atomic**.
   - Duyệt phải tự lock cả collection.
3. **Concurrent collections** (`java.util.concurrent`, Java 5+): thiết kế cho đồng thời — lock phân mảnh hoặc lock-free (CAS), có thao tác atomic (`putIfAbsent`, `compute`, `merge`), iterator weakly consistent.

### 8.2 `ConcurrentHashMap` — Java 7 vs Java 8+

**Java 7 — lock striping theo Segment:**
- Map chia thành mảng `Segment[]` (mặc định 16 = `concurrencyLevel`), mỗi `Segment extends ReentrantLock` chứa một hash table nhỏ (`HashEntry[]`).
- Ghi: lock **một segment** → tối đa 16 thread ghi song song (ở 16 segment khác nhau).
- Đọc: **không lock** (dựa trên field `volatile` của `HashEntry`), trừ trường hợp hiếm.
- `size()`: thử cộng size các segment 2 lần không lock, nếu `modCount` thay đổi thì lock **tất cả** segment.
- Nhược điểm: số segment cố định sau khi tạo; tốn bộ nhớ cho map nhỏ.

**Java 8+ — bỏ Segment, lock ở mức từng bin:**
- Cấu trúc giống `HashMap`: `volatile Node<K,V>[] table`, `Node` có `val` và `next` là `volatile`.
- `get`: **hoàn toàn không lock** — đọc volatile phần tử của table (`tabAt` dùng `Unsafe/VarHandle` getAcquire/volatile), duyệt list/tree.
- `put`:
  1. Table chưa có → `initTable` (CAS trên `sizeCtl`).
  2. Bin rỗng → **CAS** đặt node mới, không lock.
  3. Bin đang được chuyển khi resize (node đầu là `ForwardingNode`, hash = `MOVED` = −1) → thread hiện tại **giúp resize** (`helpTransfer`).
  4. Ngược lại → **`synchronized (nodeĐầuBin)`** rồi chèn vào list hoặc cây (`TreeBin`). Chỉ khóa một bin → độ song song tỉ lệ với số bin.
- **Resize hợp tác**: nhiều thread cùng chuyển dữ liệu, mỗi thread nhận một đoạn (stride) bin; bin đã chuyển được thay bằng `ForwardingNode` để `get` tiếp tục tìm ở table mới.
- **Đếm size**: `baseCount` + mảng `CounterCell[]` (giống `LongAdder`) — tránh mọi thread cùng CAS một biến. `size()`/`mappingCount()` là **ước lượng** khi có cập nhật đồng thời.
- Treeify như `HashMap` (bin ≥ 8, table ≥ 64), bin cây dùng `TreeBin` có cơ chế khóa đọc/ghi riêng.
- `concurrencyLevel` ở constructor chỉ còn là gợi ý kích thước.

**Ngữ nghĩa quan trọng:**
- **Không cho phép `null`** key/value: với map đồng thời, `get(k) == null` phải mang nghĩa duy nhất là "không có key" — không thể kiểm tra thêm `containsKey` một cách atomic (Doug Lea giải thích đây là lý do chính).
- `compute`, `computeIfAbsent`, `computeIfPresent`, `merge` là **atomic** cho từng key: hàm được chạy **trong khi giữ lock của bin** → hàm phải **ngắn, không blocking, không sửa map**. Gọi chậm (I/O) trong `computeIfAbsent` sẽ chặn mọi thread khác ghi vào cùng bin.
- `computeIfAbsent` đệ quy (hàm lại gọi `computeIfAbsent` trên chính map với key cùng bin): Java 8 có thể **lặp vô hạn** (bug JDK-8062841); Java 9+ ném `IllegalStateException: Recursive update`.
- Java 8 `computeIfAbsent` khóa bin **ngay cả khi key đã tồn tại** → trên key "nóng" có thể thành nút thắt; Java 9+ kiểm tra nhanh không khóa trước. Workaround Java 8: `V v = map.get(k); if (v == null) v = map.computeIfAbsent(k, f);`.

```java
ConcurrentHashMap<String, LongAdder> hits = new ConcurrentHashMap<>();
hits.computeIfAbsent(endpoint, k -> new LongAdder()).increment();   // đếm đồng thời, atomic, ít tranh chấp

ConcurrentHashMap<String, Integer> counts = new ConcurrentHashMap<>();
counts.merge(word, 1, Integer::sum);                                // atomic

// ❌ Check-then-act không atomic dù map là concurrent:
if (!counts.containsKey(word)) counts.put(word, 1); else counts.put(word, counts.get(word) + 1);
```

Bulk operations (Java 8): `forEach`, `search`, `reduce` với `parallelismThreshold`; `ConcurrentHashMap.newKeySet()` cho concurrent `Set`.

### 8.3 `CopyOnWriteArrayList`

- Mọi thao tác ghi (`add`, `set`, `remove`) lấy lock, **copy toàn bộ mảng**, sửa bản copy, rồi gán lại tham chiếu `volatile`. Đọc và duyệt **không lock**, duyệt trên snapshot.
- Phù hợp: **đọc rất nhiều, ghi rất ít, kích thước nhỏ** — danh sách listener/observer, cấu hình routing, whitelist.
- Không phù hợp: ghi thường xuyên hoặc list lớn (mỗi lần ghi O(n) + rác).

### 8.4 `BlockingQueue` — nền tảng producer/consumer

| Hiện thực | Cấu trúc | Giới hạn | Lock | Ghi chú |
|---|---|---|---|---|
| `ArrayBlockingQueue` | Mảng vòng | **Bắt buộc** chỉ định capacity | Một `ReentrantLock` + 2 `Condition` (notEmpty, notFull) | Tùy chọn fairness; bộ nhớ cố định, dễ dự đoán |
| `LinkedBlockingQueue` | Linked nodes | Tùy chọn — **mặc định `Integer.MAX_VALUE`** | **Hai lock** riêng (`putLock`, `takeLock`) → producer và consumer ít chặn nhau | Throughput tốt hơn, tạo node mỗi lần put |
| `SynchronousQueue` | Không chứa phần tử | 0 | Lock-free (dual stack/queue) | `put` chờ tới khi có `take` — "chuyển tay" trực tiếp |
| `PriorityBlockingQueue` | Heap | Không giới hạn | Một lock | `put` không bao giờ chặn |
| `DelayQueue` | Heap theo thời gian hết hạn | Không giới hạn | Một lock | Phần tử chỉ lấy ra được khi hết delay |
| `LinkedTransferQueue` | Linked, lock-free | Không giới hạn | CAS | `transfer()` chờ consumer nhận |

```java
BlockingQueue<Job> queue = new ArrayBlockingQueue<>(1_000);   // có giới hạn → back-pressure

// Producer
if (!queue.offer(job, 100, TimeUnit.MILLISECONDS)) {
    rejectWith429(job);                                         // không chặn vô hạn, không OOM
}

// Consumer
while (!Thread.currentThread().isInterrupted()) {
    Job j = queue.take();                                       // chặn tới khi có việc
    process(j);
}
```

**Sự cố production kinh điển**: `Executors.newFixedThreadPool(n)` dùng `LinkedBlockingQueue` **không giới hạn** → khi task đến nhanh hơn xử lý, queue phình to tới **`OutOfMemoryError`** (hoặc latency tăng vô hạn trước đó). `Executors.newCachedThreadPool()` dùng `SynchronousQueue` với số thread tối đa `Integer.MAX_VALUE` → bùng nổ số thread. Production nên tự tạo `ThreadPoolExecutor` với **queue có giới hạn** và `RejectedExecutionHandler` rõ ràng.

### 8.5 Các concurrent collection khác

- `ConcurrentSkipListMap`/`ConcurrentSkipListSet`: sorted, concurrent, lock-free, O(log n) kỳ vọng — thay thế `TreeMap` đồng bộ.
- `ConcurrentLinkedQueue`/`ConcurrentLinkedDeque`: lock-free (thuật toán Michael-Scott), không giới hạn, không blocking. `size()` là O(n)!

> 💡 **Góc nhìn Senior:**
> - Chọn: map dùng chung giữa thread → `ConcurrentHashMap` (không phải `synchronizedMap`, `Hashtable`). Sorted + concurrent → `ConcurrentSkipListMap`. Listener → `CopyOnWriteArrayList`. Producer/consumer → `BlockingQueue` **có giới hạn**.
> - Thread-safe collection không làm cho **logic** của bạn thread-safe: mọi chuỗi thao tác phải dùng method atomic (`compute`, `merge`, `putIfAbsent`, `replace(k, old, new)`) hoặc lock bên ngoài.
> - Không dùng `ConcurrentHashMap` làm cache không giới hạn — vẫn là memory leak. Dùng Caffeine (giới hạn kích thước, TTL).

> ⚠️ **Lỗi thường gặp:**
> - Gọi I/O/remote call bên trong `computeIfAbsent` của `ConcurrentHashMap` → chặn các key khác cùng bin, có thể deadlock nếu gọi lồng.
> - Dùng `size()`/`isEmpty()` của concurrent collection để ra quyết định (giá trị có thể đã cũ ngay khi trả về).
> - Dùng queue không giới hạn giữa producer nhanh và consumer chậm.

### 🛠 Bài tập phần 8

**Bài 8.1 — Đếm từ đa luồng (Cơ bản)**
- Đề bài: 8 thread cùng đếm tần suất từ trong một file lớn vào một map chung. Viết 4 phiên bản: `HashMap` (sai), `synchronizedMap` + check-then-act (sai), `ConcurrentHashMap.merge` (đúng), `ConcurrentHashMap<String, LongAdder>` (đúng, nhanh).
- Tiêu chí đạt: test chứng minh 2 phiên bản đầu cho kết quả sai (tổng số từ lệch); đo thời gian 2 phiên bản đúng.

**Bài 8.2 — Producer/consumer có back-pressure (Trung bình)**
- Đề bài: 2 producer sinh 1 triệu job, 4 consumer xử lý (sleep 1ms mỗi job). Dùng `ArrayBlockingQueue(1000)`. Bổ sung cơ chế dừng sạch (poison pill hoặc interrupt).
- Tiêu chí đạt: heap ổn định (quan sát bằng VisualVM/JFR); tất cả consumer kết thúc; so sánh với `LinkedBlockingQueue()` không giới hạn chạy với `-Xmx128m` và job có payload 1KB.

**Bài 8.3 — Memoizer đồng thời (Nâng cao)**
- Đề bài: Viết `Memoizer<A, V>` bọc một hàm tính toán đắt (vài trăm ms), đảm bảo: mỗi key chỉ tính **một lần** kể cả khi nhiều thread hỏi cùng lúc; các key khác nhau tính song song; tính toán **không** chạy bên trong lock của map; nếu tính lỗi thì lần sau được thử lại.
- Tiêu chí đạt: dùng `ConcurrentHashMap<A, CompletableFuture<V>>` hoặc `FutureTask` (theo JCIP chương 5); test với 50 thread cùng key → hàm chỉ được gọi 1 lần.

<details>
<summary>Gợi ý lời giải</summary>

Bài 8.3 (theo ý tưởng `Memoizer` trong JCIP):

```java
public final class Memoizer<A, V> {
    private final ConcurrentHashMap<A, CompletableFuture<V>> cache = new ConcurrentHashMap<>();
    private final Function<A, V> fn;
    public Memoizer(Function<A, V> fn) { this.fn = fn; }

    public V get(A arg) {
        CompletableFuture<V> f = cache.get(arg);
        if (f == null) {
            CompletableFuture<V> created = new CompletableFuture<>();
            f = cache.putIfAbsent(arg, created);     // chỉ một thread thắng, hàm tính chưa chạy trong lock
            if (f == null) {
                f = created;
                try { created.complete(fn.apply(arg)); }
                catch (Throwable t) { cache.remove(arg, created); created.completeExceptionally(t); }
            }
        }
        return f.join();
    }
}
```

</details>

---

<a id="phan-9"></a>
## 9. Generics

### 9.1 Vì sao có generics

Trước Java 5, collection chứa `Object` → cast thủ công, lỗi kiểu chỉ phát hiện lúc runtime (`ClassCastException`). Generics chuyển kiểm tra kiểu về **compile-time** và loại bỏ cast tường minh.

```java
List raw = new ArrayList();          // raw type
raw.add("x"); raw.add(1);
String s = (String) raw.get(1);      // ClassCastException lúc runtime

List<String> typed = new ArrayList<>();
// typed.add(1);                     // lỗi compile — phát hiện sớm
```

### 9.2 Type erasure

Generics trong Java được hiện thực bằng **erasure** (để tương thích ngược với bytecode/thư viện trước Java 5):
- Tham số kiểu bị thay bằng **bound** của nó (`T` → `Object`; `T extends Comparable<T>` → `Comparable`).
- Compiler chèn **cast** tại nơi sử dụng.
- `List<String>` và `List<Integer>` cùng là class `List` lúc runtime: `new ArrayList<String>().getClass() == new ArrayList<Integer>().getClass()` → `true`.
- Thông tin generic của **khai báo** (field, method signature, superclass) vẫn được lưu trong attribute `Signature` của class file → đọc được bằng reflection (`getGenericType()`, `getGenericSuperclass()`). Thông tin tham số kiểu của **một instance cụ thể** thì không.

**Hệ quả — những điều KHÔNG làm được:**

```java
class Box<T> {
    // T t = new T();                       // ❌ không biết T là gì lúc runtime
    // T[] arr = new T[10];                 // ❌ không tạo được mảng generic
    // static T shared;                     // ❌ static thuộc về class, dùng chung cho mọi T
    boolean check(Object o) {
        // return o instanceof T;           // ❌
        // return o instanceof List<String>;// ❌ (instanceof List<?> thì được)
        return true;
    }
    // void m(List<String> a) {} void m(List<Integer> a) {}  // ❌ name clash: cùng erasure m(List)
}
// List<int> nums;                          // ❌ primitive không làm type argument được (cho tới Project Valhalla)
// class MyEx<T> extends Exception {}       // ❌ class generic không được kế thừa Throwable
```

Cách vượt qua: truyền **`Class<T>` token** (`EnumMap(Class<K>)`, `Array.newInstance(type, n)`), `Supplier<T>` thay cho `new T()`, hoặc **super type token** — bắt thông tin kiểu qua anonymous subclass (Jackson `new TypeReference<List<User>>() {}`, Spring `ParameterizedTypeReference`):

```java
abstract class TypeRef<T> {
    final Type type;
    protected TypeRef() {
        this.type = ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }
}
Type t = new TypeRef<Map<String, List<Integer>>>() {}.type;
System.out.println(t); // java.util.Map<java.lang.String, java.util.List<java.lang.Integer>>
```

### 9.3 Bridge methods

Do erasure, override với kiểu cụ thể cần một method "cầu nối" do compiler sinh ra để giữ đa hình:

```java
class Node<T> {
    T data;
    public void setData(T data) { this.data = data; }      // sau erasure: setData(Object)
}
class IntNode extends Node<Integer> {
    @Override public void setData(Integer data) { super.setData(data); } // setData(Integer)
    // compiler sinh thêm (synthetic, bridge):
    // public void setData(Object data) { setData((Integer) data); }
}

Node raw = new IntNode();          // raw type
raw.setData("hello");              // gọi bridge → cast (Integer) "hello" → ClassCastException
```

Xem bằng `javap -p -c IntNode.class` sẽ thấy method `setData(java.lang.Object)` có cờ `ACC_BRIDGE, ACC_SYNTHETIC`. Bridge method cũng xuất hiện với covariant return type. Lưu ý khi dùng reflection (`getDeclaredMethods` trả về cả bridge; lọc bằng `Method.isBridge()`) — Spring có `BridgeMethodResolver` vì lý do này.

### 9.4 Invariance, wildcards và PECS

Generics **bất biến (invariant)**: `List<Integer>` **không phải** subtype của `List<Number>`, dù `Integer` là subtype của `Number`. Nếu cho phép:

```java
List<Integer> ints = new ArrayList<>();
// List<Number> nums = ints;       // giả sử được phép
// nums.add(3.14);                 // thêm Double vào list Integer → hỏng
```

Wildcards cho phép linh hoạt có kiểm soát:

| Wildcard | Đọc ra | Ghi vào | Dùng khi |
|---|---|---|---|
| `List<? extends Number>` | `Number` ✅ | ❌ (chỉ `null`) | Collection là **Producer** (bạn đọc từ nó) |
| `List<? super Integer>` | `Object` (kém hữu ích) | `Integer` ✅ | Collection là **Consumer** (bạn ghi vào nó) |
| `List<?>` | `Object` | ❌ (chỉ `null`) | Không quan tâm kiểu phần tử (`size`, `clear`) |

**PECS — Producer Extends, Consumer Super** (Effective Java Item 31):

```java
// java.util.Collections
public static <T> void copy(List<? super T> dest, List<? extends T> src)

// Ví dụ tự viết: chuyển phần tử từ nguồn sang đích
static <T> void transfer(Collection<? extends T> src, Collection<? super T> dst) {
    for (T t : src) dst.add(t);
}
List<Integer> ints = List.of(1, 2);
List<Number> nums = new ArrayList<>();
List<Object> objs = new ArrayList<>();
transfer(ints, nums);   // T = Integer
transfer(ints, objs);   // ✅ nhờ ? super

// Comparator/Comparable là consumer → super
static <T extends Comparable<? super T>> T max(Collection<? extends T> c) { ... }
// ? super T cho phép T = java.sql.Timestamp (Comparable<java.util.Date>) hay LocalDate (Comparable<ChronoLocalDate>)
```

Chữ ký thật của `Collections.max`: `public static <T extends Object & Comparable<? super T>> T max(Collection<? extends T> coll)` — phần `Object &` để erasure của `T` là `Object` (giữ tương thích nhị phân với chữ ký trước Java 5 trả về `Object`).

**Quy tắc thiết kế API**: wildcard dùng cho **tham số đầu vào**, **không** dùng cho kiểu trả về (bắt caller xử lý wildcard). Nếu một tham số vừa đọc vừa ghi → dùng kiểu chính xác `List<T>`.

**Wildcard capture**: không thể `list.set(i, list.get(j))` với `List<?>` — dùng helper method generic để "bắt" kiểu:

```java
public static void swap(List<?> list, int i, int j) { swapHelper(list, i, j); }
private static <E> void swapHelper(List<E> list, int i, int j) { list.set(i, list.set(j, list.get(i))); }
```

### 9.5 Generic methods, bounded types, recursive bounds

```java
public static <K, V extends Comparable<? super V>> Optional<K> argMax(Map<K, V> map) {
    return map.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey);
}

// Nhiều bound: class trước, interface sau
static <T extends Number & Comparable<T>> T clampMax(T a, T b) { return a.compareTo(b) >= 0 ? a : b; }

// Recursive bound: Enum<E extends Enum<E>>, và "self type" cho builder kế thừa
abstract class Builder<T extends Builder<T>> {
    String name;
    T name(String n) { this.name = n; return self(); }
    protected abstract T self();
}
final class PizzaBuilder extends Builder<PizzaBuilder> {
    PizzaBuilder topping(String t) { return this; }
    protected PizzaBuilder self() { return this; }
}
// new PizzaBuilder().name("x").topping("cheese"); // chain không mất kiểu con
```

Type inference: diamond `<>` (Java 7; với anonymous class từ Java 9), target typing của lambda (Java 8), `var` (Java 10). Đôi khi cần chỉ định tường minh: `Collections.<String>emptyList()`.

### 9.6 Raw types, unchecked warnings, heap pollution

- **Raw type** (`List` không tham số) tồn tại chỉ để tương thích ngược. Dùng raw type tắt kiểm tra kiểu → lỗi dời về runtime, ở chỗ xa nơi gây ra. **Không dùng** trong code mới (Effective Java Item 26), trừ class literal (`List.class`) và `instanceof List`.
- **Unchecked warning**: compiler không đảm bảo được an toàn kiểu. Xử lý từng cái; chỉ `@SuppressWarnings("unchecked")` ở **phạm vi nhỏ nhất** kèm comment chứng minh an toàn (Item 27).
- **Heap pollution** (JLS §4.12.2): biến kiểu tham số hóa trỏ tới object không thuộc kiểu đó → `ClassCastException` xảy ra ở nơi **không có cast nào trong mã nguồn**.

```java
List<String> strings = new ArrayList<>();
List raw = strings;
raw.add(42);                       // unchecked warning — heap pollution bắt đầu từ đây
String s = strings.get(0);         // ClassCastException ở dòng này, xa nơi gây lỗi
```

**Generic varargs** là nguồn heap pollution phổ biến, vì varargs tạo **mảng** `T[]` mà thực ra là `Object[]` (sau erasure):

```java
static <T> T[] toArray(T... args) { return args; }                     // ❌ để lộ mảng varargs ra ngoài
static <T> T[] pickTwo(T a, T b, T c) { return toArray(a, b); }        // T[] thực chất là Object[]
String[] r = pickTwo("a", "b", "c");                                    // ClassCastException: Object[] → String[]

@SafeVarargs                                                           // hứa: không ghi vào mảng, không để lộ mảng
static <T> List<T> flatten(List<? extends T>... lists) {
    List<T> out = new ArrayList<>();
    for (List<? extends T> l : lists) out.addAll(l);
    return out;
}
```

`@SafeVarargs` chỉ đặt được trên method **không thể override**: `static`, `final`, constructor, và (Java 9+) `private` instance method. Thay thế an toàn hơn: nhận `List<? extends T>` thay vì varargs (Item 32).

### 9.7 Vì sao không có mảng generic

Mảng **covariant** và **reified** (biết kiểu phần tử lúc runtime, kiểm tra khi ghi):

```java
Object[] objs = new String[1];
objs[0] = 42;                       // ArrayStoreException lúc runtime — mảng tự bảo vệ
```

Generics **invariant** và **erased**. Nếu cho phép `new List<String>[1]`:

```java
// List<String>[] lsa = new List<String>[1];  // giả sử được phép
// Object[] oa = lsa;                         // hợp lệ vì mảng covariant
// oa[0] = List.of(42);                       // runtime chỉ thấy List → không ArrayStoreException
// String s = lsa[0].get(0);                  // ClassCastException — hệ thống kiểu bị phá mà không có cảnh báo
```

Vì vậy Java cấm tạo mảng của kiểu tham số hóa (trừ unbounded wildcard `List<?>[]`). Khuyến nghị: **ưu tiên `List` hơn mảng** khi làm việc với generics (Item 28). Bên trong thư viện (như `ArrayList`), JDK dùng `Object[]` và cast `(E)` có `@SuppressWarnings` — an toàn vì mảng không bao giờ lộ ra ngoài.

> 💡 **Góc nhìn Senior:**
> - Erasure là trade-off có chủ đích: tương thích ngược hoàn toàn (thư viện pre-Java 5 chạy được), không phình code như C++ template, nhưng mất thông tin kiểu lúc runtime, không hỗ trợ primitive (gây boxing). **Project Valhalla** (value classes, specialized generics) nhắm tới việc giải quyết phần primitive.
> - Thiết kế API thư viện: dùng PECS cho tham số, trả về kiểu cụ thể; tránh lộ wildcard ra kiểu trả về; dùng `Class<T>` token cho container dị thể an toàn kiểu (Item 33: `Favorites.putFavorite(Class<T>, T)`).
> - Khi đọc stack trace có `ClassCastException` ở dòng không có cast → nghĩ ngay tới heap pollution (raw type, unchecked cast, deserialization JSON vào `List<T>` không có type token → Jackson tạo `List<LinkedHashMap>`).

> ⚠️ **Lỗi thường gặp:**
> - Jackson `objectMapper.readValue(json, List.class)` → nhận `List<LinkedHashMap>`, sau đó `ClassCastException` khi lấy phần tử ra như `User`. Dùng `TypeReference<List<User>>`.
> - Dùng `List<Object>` khi ý là "list bất kỳ" — `List<String>` không gán được cho `List<Object>`; dùng `List<?>`.
> - Lạm dụng `@SuppressWarnings("unchecked")` trên cả class.

### 🛠 Bài tập phần 9

**Bài 9.1 — PECS (Cơ bản)**
- Đề bài: Viết các method generic tĩnh: `sum(Collection<? extends Number>)`, `fill(List<? super Integer>, int n)`, `copyIf(Collection<? extends T> src, Collection<? super T> dst, Predicate<? super T> p)`, `max(Collection<? extends T>, Comparator<? super T>)`.
- Tiêu chí đạt: gọi được với `List<Integer>`, `List<Number>`, `List<Object>`, `Predicate<Object>`; giải thích vì sao bỏ wildcard thì một số lời gọi không compile.

**Bài 9.2 — Soi bridge method và erasure (Trung bình)**
- Đề bài: Viết `Node<T>`/`IntNode` như Phần 9.3 và `class Pair<T extends Comparable<T>>`. Dùng `javap -p -c -s` và reflection (`getDeclaredMethods`, `isBridge`, `getGenericParameterTypes`) để in ra: chữ ký sau erasure, các bridge method, thông tin generic còn lưu trong class file.
- Tiêu chí đạt: tái hiện `ClassCastException` qua raw type gọi bridge method; giải thích tại sao `Pair`'s `T` erase thành `Comparable`.

**Bài 9.3 — Typesafe heterogeneous container + TypeRef (Nâng cao)**
- Đề bài: (a) Viết `TypedConfig` lưu cấu hình dị thể: `<T> void put(Key<T> key, T value)`, `<T> T get(Key<T> key)` với `Key<T>` chứa tên và `Class<T>` (hoặc `TypeRef<T>` để hỗ trợ `List<String>`). (b) Viết `@SafeVarargs static <T> List<T> concat(List<? extends T>... lists)` và chứng minh bằng ví dụ vì sao `toArray(T...)` không an toàn.
- Tiêu chí đạt: không có unchecked cast trong code client; cast duy nhất nằm trong `TypedConfig` có chứng minh an toàn bằng comment; `get` với kiểu sai không compile.

<details>
<summary>Gợi ý lời giải</summary>

Bài 9.3 (a) — phiên bản `Class<T>`:

```java
public final class TypedConfig {
    public record Key<T>(String name, Class<T> type) {}
    private final Map<Key<?>, Object> values = new HashMap<>();

    public <T> void put(Key<T> key, T value) {
        values.put(Objects.requireNonNull(key), key.type().cast(value)); // cast động: chặn heap pollution qua raw type
    }
    public <T> T get(Key<T> key) {
        return key.type().cast(values.get(key));                       // an toàn: put đã đảm bảo đúng kiểu
    }
}
// Key<Integer> TIMEOUT = new Key<>("timeout", Integer.class);
// int t = config.get(TIMEOUT);
```

Hạn chế của `Class<T>`: không biểu diễn được `List<String>` (chỉ có `List.class`) → cần `TypeRef<T>` (super type token) và kiểm tra kiểu nông.

</details>

---

<a id="phan-10"></a>
## 10. Chọn đúng collection & memory footprint

### 10.1 Bảng quyết định

| Nhu cầu | Lựa chọn mặc định | Thay thế / ghi chú |
|---|---|---|
| Danh sách có thứ tự, truy cập theo index | `ArrayList` | `List.of` nếu bất biến |
| Thêm/xóa hai đầu, stack, queue một luồng | `ArrayDeque` | Tránh `Stack`, `LinkedList` |
| Tra cứu key → value | `HashMap` | `EnumMap` nếu key là enum |
| Giữ thứ tự chèn | `LinkedHashMap` / `LinkedHashSet` | |
| LRU cache đơn giản (một luồng) | `LinkedHashMap(accessOrder=true)` + `removeEldestEntry` | Production: Caffeine |
| Sắp xếp + truy vấn khoảng / floor / ceiling | `TreeMap` / `TreeSet` | `ConcurrentSkipListMap` khi đa luồng |
| Tập không trùng | `HashSet` | `EnumSet` nếu enum (bit vector) |
| Lấy phần tử min/max liên tục, top-K | `PriorityQueue` | `PriorityBlockingQueue` khi đa luồng |
| Map dùng chung nhiều thread | `ConcurrentHashMap` | Không dùng `Hashtable`/`synchronizedMap` |
| Đọc rất nhiều, ghi rất ít, nhỏ | `CopyOnWriteArrayList` | Hoặc immutable + `volatile` swap |
| Producer/consumer | `ArrayBlockingQueue` / `LinkedBlockingQueue(capacity)` | Luôn có giới hạn |
| So sánh theo identity (`==`) | `IdentityHashMap` | Duyệt đồ thị object |
| Metadata gắn với vòng đời object khác | `WeakHashMap` | Cẩn thận value tham chiếu ngược key |
| Hàng triệu số nguyên/primitive | `int[]`/`long[]` hoặc fastutil/Eclipse Collections/HPPC | Tránh boxing |
| Đếm theo key đa luồng | `ConcurrentHashMap<K, LongAdder>` | |
| Multimap | `Map<K, List<V>>` + `computeIfAbsent` | Guava `Multimap` |

### 10.2 Memory footprint — con số cần nhớ

Giả định HotSpot 64-bit, heap < 32 GB → **compressed oops** bật (reference 4 byte), object header **12 byte** (mark word 8 + class pointer nén 4), mọi object **căn lề 8 byte**. Mảng có header 16 byte (thêm 4 byte độ dài).

| Đối tượng | Kích thước xấp xỉ |
|---|---|
| `new Object()` | 16 B |
| `Integer`, `Long`, `Double` | 16 B (so với 4/8 B primitive) |
| `String` rỗng / ngắn ("hello", Latin-1) | ~24 B object + ~24 B `byte[]` ≈ 48 B |
| `ArrayList` (rỗng, chưa add) | ~24 B |
| `HashMap.Node` | 32 B |
| `LinkedHashMap.Entry` | 40 B |
| `TreeMap.Entry` | 40 B |
| `LinkedList.Node` | 24 B |
| `HashMap.TreeNode` | ~56 B |

**Ví dụ tính 1 triệu phần tử:**

| Cấu trúc | Ước lượng | Ghi chú |
|---|---|---|
| `int[1_000_000]` | ~4 MB | Baseline |
| `ArrayList<Integer>` | ~4 MB (mảng ref) + ~16 MB (`Integer`) ≈ 20 MB | ×5 |
| `LinkedList<Integer>` | ~24 MB (node) + 16 MB ≈ 40 MB | ×10 |
| `HashSet<Integer>` | 32 MB (node) + 16 MB + ~8 MB (table 2^21 ref) ≈ 56 MB | ×14 |
| `HashMap<Long, Long>` | 32 + 16 + 16 + ~8 ≈ 72 MB | |
| `TreeMap<Long, Long>` | 40 + 16 + 16 ≈ 72 MB | không có table |

(Có thể kiểm chứng bằng **JOL**: `GraphLayout.parseInstance(obj).totalSize()`.)

**Kỹ thuật giảm footprint:**
1. Chỉ định capacity ban đầu đúng (tránh mảng dư sau khi grow, tránh resize).
2. `trimToSize()` cho list lớn tồn tại lâu sau khi build xong.
3. Dùng primitive collection (fastutil `Int2IntOpenHashMap`, Eclipse Collections `IntIntHashMap`) — tránh object `Node` và boxing, open addressing.
4. `EnumMap`/`EnumSet` cho key enum.
5. Immutable collection `List.of`/`Map.of` (không capacity dư, class chuyên biệt).
6. Tái sử dụng/khử trùng lặp value (`String` lặp lại → enum hoặc interning có kiểm soát).
7. Dữ liệu quá lớn cho heap → off-heap (Chronicle Map), cache ngoài (Redis), hoặc xử lý streaming.
8. Tương lai: **Compact Object Headers** (JEP 450 thử nghiệm ở Java 24, JEP 519 thành tính năng chính thức ở Java 25) giảm header còn 8 byte — giảm đáng kể footprint của collection nhiều object nhỏ.

> 💡 **Góc nhìn Senior:**
> - Khi phỏng vấn hỏi "chọn collection nào", hãy trả lời theo khung: **(1) thao tác chủ đạo và tần suất → độ phức tạp; (2) yêu cầu thứ tự/sắp xếp/trùng lặp/null; (3) đơn luồng hay đa luồng; (4) kích thước dữ liệu → bộ nhớ & cache locality; (5) mutable hay immutable ở ranh giới API**.
> - Big-O không phải tất cả: với n nhỏ (< vài trăm), duyệt tuyến tính `ArrayList` thường nhanh hơn `HashMap` lookup vì cache locality và không phải tính hash. Đo bằng JMH trước khi kết luận.
> - Heap dump (Eclipse MAT, "Dominator tree") thường lộ ra `HashMap$Node[]` hoặc `Object[]` khổng lồ — nguồn gốc là cache tự chế không giới hạn hoặc collection static tích lũy dần (memory leak kinh điển).

> ⚠️ **Lỗi thường gặp:**
> - `Map<Long, Boolean>` 50 triệu entry để làm "visited set" — dùng `BitSet` hoặc `RoaringBitmap` (vài MB thay vì vài GB).
> - Cache bằng `static HashMap` không bao giờ xóa → OOM sau vài ngày uptime.
> - Chọn `LinkedList` "vì thêm/xóa nhanh" mà không đo.

### 🛠 Bài tập phần 10

**Bài 10.1 — Đo bằng JOL (Cơ bản)**
- Đề bài: Thêm dependency `org.openjdk.jol:jol-core`, đo `totalSize()` của: `int[1M]`, `ArrayList<Integer>` 1M, `LinkedList<Integer>` 1M, `HashSet<Integer>` 1M, `List.of` 1M, `BitSet` 1M bit. In `ClassLayout.parseClass(HashMap.Node.class)` (có thể cần `--add-opens`).
- Tiêu chí đạt: bảng kết quả đối chiếu với Phần 10.2; giải thích chênh lệch; thử chạy với `-XX:-UseCompressedOops` và nhận xét.

**Bài 10.2 — Lựa chọn có lập luận (Trung bình)**
- Đề bài: Với mỗi tình huống, chọn collection và viết 3–5 dòng lập luận: (a) danh sách 50 sản phẩm gợi ý hiển thị theo điểm; (b) 10 triệu user ID đã nhận khuyến mãi, cần kiểm tra "đã nhận chưa" cực nhanh; (c) lịch các job theo thời điểm chạy, thêm/xóa liên tục, lấy job sớm nhất; (d) bảng tỷ giá được đọc hàng nghìn lần/giây, cập nhật 1 lần/phút; (e) đếm số request theo endpoint từ 200 thread; (f) lịch sử 100 thao tác gần nhất để undo.
- Tiêu chí đạt: mỗi lựa chọn nêu được độ phức tạp, bộ nhớ ước lượng, thread-safety; có ít nhất một phương án thay thế và lý do không chọn.

**Bài 10.3 — Tối ưu bộ nhớ cho index lớn (Nâng cao)**
- Đề bài: Xây index `Map<Long productId, int[] categoryIds>` cho 20 triệu sản phẩm. Phiên bản 1: `HashMap<Long, List<Integer>>`. Phiên bản 2: tối ưu bằng primitive map (fastutil `Long2ObjectOpenHashMap<int[]>`) hoặc tự cài đặt open addressing trên `long[]` + offset vào một `int[]` lớn (CSR — compressed sparse row).
- Tiêu chí đạt: đo heap (JOL hoặc `jcmd GC.class_histogram`) và thời gian lookup cho cả 2; giảm bộ nhớ ít nhất 3 lần; giải thích nguồn gốc tiết kiệm (boxing, Node, list overhead, pointer chasing).

<details>
<summary>Gợi ý lời giải</summary>

Bài 10.2 — gợi ý đáp án: (a) `ArrayList` + sort một lần (n nhỏ); (b) `RoaringBitmap`/`BitSet` nếu ID dày đặc, hoặc `LongOpenHashSet` — tránh `HashSet<Long>` (~800MB+); nếu nhiều instance → Redis/Bloom filter; (c) `PriorityQueue` (hoặc `TreeMap<Instant, List<Job>>` nếu cần hủy job theo thời điểm); (d) immutable `Map.copyOf` + tham chiếu `volatile`/`AtomicReference` thay toàn bộ mỗi phút; (e) `ConcurrentHashMap<String, LongAdder>`; (f) `ArrayDeque` giới hạn 100 (`pollFirst` khi đầy).

Bài 10.3 — ý tưởng CSR: sắp xếp productId, lưu `long[] ids` (binary search) hoặc open-addressing `long[] keys` + `int[] slotToOffset`; `int[] offsets` (n+1) và `int[] categories` liền mạch: category của sản phẩm i nằm trong `categories[offsets[i] .. offsets[i+1])`. Mỗi sản phẩm chỉ tốn ~8 + 4 byte + dữ liệu thực.

</details>

---

<a id="du-an-mini"></a>
## Dự án mini

### "InMemory Analytics Engine" — bộ máy thống kê sự kiện trong bộ nhớ

**Bối cảnh:** Một hệ thống thương mại điện tử cần một thư viện Java thuần (Java 17/21, không framework) nhận luồng sự kiện (`PageView`, `AddToCart`, `Purchase`) và trả lời truy vấn thời gian thực. Dự án buộc bạn áp dụng gần như mọi collection và kỹ thuật generics của module.

**Yêu cầu chức năng**
1. **Mô hình sự kiện**: `sealed interface Event permits PageView, AddToCart, Purchase` (record, immutable), mỗi event có `userId`, `productId`, `timestamp`, `Purchase` có `amount` (`BigDecimal`).
2. **Ingest đa luồng**: `EventIngestor` nhận event từ nhiều producer qua `BlockingQueue` **có giới hạn**; 1–N worker xử lý; khi queue đầy thì áp dụng chính sách cấu hình được (chặn có timeout / từ chối và đếm số event bị loại).
3. **Truy vấn** (`AnalyticsService`):
   - `topProducts(int k, Duration window)` — top-K sản phẩm theo số lượt mua trong cửa sổ thời gian gần nhất (dùng min-heap kích thước K).
   - `revenueBetween(Instant from, Instant to)` — doanh thu trong khoảng thời gian (dùng `NavigableMap` theo bucket phút).
   - `recentlyViewed(String userId, int n)` — n sản phẩm xem gần nhất, không trùng, mới nhất trước (LRU theo user: `LinkedHashMap` access-order hoặc `LinkedHashSet` + giới hạn).
   - `uniqueVisitors(Duration window)` — số user duy nhất (thử cả `HashSet` và phương án tiết kiệm bộ nhớ, ví dụ `BitSet` khi userId là số nguyên dày đặc).
   - `conversionRate(String productId)` — purchase / view.
4. **Generic API**: `Aggregator<E extends Event, K, V>` với method `<R> R query(Function<? super Map<K, V>, ? extends R> fn)`; một `Registry` lưu các aggregator theo `Key<T>` an toàn kiểu (typesafe heterogeneous container). Một tiện ích `@SafeVarargs static <T> Predicate<T> allOf(Predicate<? super T>... ps)`.
5. **Snapshot immutable**: mọi truy vấn trả về collection immutable (`List.copyOf`, `Map.copyOf`, `toList()`).
6. **Tự hiện thực** ít nhất một cấu trúc dữ liệu: `SimpleHashMap` (Bài 3.2) hoặc `IndexedMinHeap` (Bài 5.3), dùng nó trong một truy vấn và có test so sánh với cấu trúc của JDK.

**Yêu cầu phi chức năng**
- **Thread-safety**: ingest và query chạy đồng thời không mất dữ liệu, không CME; chỉ dùng `ConcurrentHashMap`, `LongAdder`, `ConcurrentSkipListMap`, `CopyOnWriteArrayList`... đúng chỗ — không dùng `synchronized` thô trên toàn service. Có test đa luồng (≥ 16 thread, 1 triệu event) kiểm tra tổng số đếm đúng.
- **Hiệu năng**: ingest ≥ 500.000 event/giây trên laptop với 4 worker (đo bằng JMH hoặc benchmark có warm-up); `topProducts` với 100.000 sản phẩm < 50 ms.
- **Bộ nhớ**: dữ liệu cũ hơn cửa sổ giữ lại (cấu hình, mặc định 1 giờ) được dọn định kỳ (`headMap(cutoff).clear()`); chạy 10 triệu event với `-Xmx512m` không OOM. Báo cáo footprint đo bằng JOL hoặc heap histogram.
- **Chất lượng**: không raw type, không `@SuppressWarnings` ngoài phạm vi nhỏ có comment; không key mutable; comparator không dùng phép trừ; test coverage ≥ 80%.
- `README` có bảng "collection đã chọn — lý do — độ phức tạp — phương án thay thế".

**Tiêu chí chấm (100 điểm)**

| Hạng mục | Điểm |
|---|---|
| Chọn collection đúng và lập luận trong README (độ phức tạp, bộ nhớ, thread-safety) | 20 |
| Thread-safety: test đa luồng pass ổn định, dùng thao tác atomic đúng | 20 |
| Generics: PECS, typesafe container, `@SafeVarargs` đúng chỗ, không raw type | 15 |
| Cấu trúc tự hiện thực đúng và có test so sánh | 15 |
| Hiệu năng & bộ nhớ đạt ngưỡng, có số liệu đo | 15 |
| API trả về immutable, không lộ trạng thái nội bộ | 10 |
| Code sạch, test rõ ràng | 5 |

---

<a id="checklist-tu-danh-gia"></a>
## Checklist tự đánh giá

- [ ] Tôi vẽ được cây phân cấp Collections Framework (kể cả Sequenced Collections Java 21) và giải thích vì sao `Map` không kế thừa `Collection`.
- [ ] Tôi giải thích được cơ chế grow 1.5x của `ArrayList`, amortized O(1), và vì sao `ArrayList` thường thắng `LinkedList` kể cả khi chèn giữa.
- [ ] Tôi trình bày được `HashMap` từ đầu tới cuối: hash spreading, index bằng mask, put từng bước, load factor, resize split lo/hi, treeify (8/6/64), key null.
- [ ] Tôi giải thích được vì sao `HashMap` Java 7 có thể treo CPU khi dùng đa luồng và vì sao Java 8 vẫn không thread-safe.
- [ ] Tôi tái hiện được bug key mutable và giải thích hợp đồng `equals`/`hashCode` ảnh hưởng tới `HashMap` thế nào.
- [ ] Tôi tự viết được LRU cache bằng `LinkedHashMap` và biết khi nào cần Caffeine thay thế.
- [ ] Tôi dùng thành thạo `NavigableMap` (`floorEntry`, `ceilingKey`, `subMap`...) và biết comparator không nhất quán với `equals` gây mất phần tử.
- [ ] Tôi giải thích được binary heap của `PriorityQueue`, độ phức tạp từng thao tác, và giải được top-K với O(N log K).
- [ ] Tôi biết vì sao nên dùng `ArrayDeque` thay `Stack`/`LinkedList`.
- [ ] Tôi giải thích được fail-fast (`modCount`), trường hợp xóa phần tử áp chót không ném CME, và khác biệt snapshot vs weakly consistent iterator.
- [ ] Tôi phân biệt được `List.of`, `List.copyOf`, `Collections.unmodifiableList`, `Arrays.asList`, `Stream.toList()`, `Collectors.toList()`.
- [ ] Tôi giải thích được `ConcurrentHashMap` Java 7 (Segment) vs Java 8 (CAS + synchronized bin, resize hợp tác, CounterCell), vì sao cấm null, và rủi ro trong `computeIfAbsent`.
- [ ] Tôi chọn đúng `BlockingQueue` và giải thích sự cố OOM của `Executors.newFixedThreadPool`.
- [ ] Tôi giải thích được type erasure, những điều generics không làm được, và cách dùng `Class<T>`/super type token.
- [ ] Tôi giải thích được bridge method và tái hiện được `ClassCastException` qua raw type.
- [ ] Tôi áp dụng PECS đúng và đọc hiểu chữ ký `Collections.max`, `Collections.copy`.
- [ ] Tôi giải thích được heap pollution, `@SafeVarargs` dùng ở đâu, và vì sao Java cấm mảng generic.
- [ ] Tôi ước lượng được bộ nhớ của các collection phổ biến và biết các kỹ thuật giảm footprint (capacity, primitive collections, BitSet, immutable).

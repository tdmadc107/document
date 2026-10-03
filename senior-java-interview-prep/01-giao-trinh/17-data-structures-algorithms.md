# Module 17 — Cấu trúc dữ liệu & Giải thuật cho phỏng vấn

> **Mục tiêu:** sau module này bạn phân tích được độ phức tạp (time/space, amortized, đệ quy) của bất kỳ lời giải nào; nhận diện được **pattern** của một bài coding interview trong 2–3 phút đầu; tự viết lại bằng Java (không cần IDE) các kỹ thuật kinh điển: two pointers, sliding window, prefix sum, binary search trên không gian đáp án, monotonic stack/queue, BFS/DFS, topological sort, union-find, Dijkstra, backtracking, DP; và trình bày được các bài toán "thực chiến" mà Senior hay gặp: LRU/LFU cache, consistent hashing, rate limiter, top-K trên stream, external merge sort.
> **Cấp độ:** Cơ bản → Nâng cao (Senior)
> **Thời lượng gợi ý:** 21 ngày (≈ 60–80 giờ, trong đó ≥ 60% là thời gian tự code bài tập)
> **Yêu cầu trước:** Module Java Core & Collections (HashMap, TreeMap, PriorityQueue, ArrayDeque), Generics, Streams API; nắm cú pháp Java 17+ (`record`, `var`, switch expression).
> **Nguồn tham khảo:**
> - Trong kho: [`Algorithms/1 - Introduction to Algorithms`](../../Algorithms/1%20-%20Introduction%20to%20Algorithms%20-%20Chia%20s%E1%BA%BB%20code.pdf) (CLRS, 3rd ed.) — Ch.3 *Growth of Functions*, Ch.4 *Divide-and-Conquer* (4.5 Master method), Ch.6 *Heapsort*, Ch.7 *Quicksort*, Ch.8 *Sorting in Linear Time*, Ch.10 *Elementary Data Structures*, Ch.11 *Hash Tables*, Ch.12 *Binary Search Trees*, Ch.13 *Red-Black Trees*, Ch.15 *Dynamic Programming*, Ch.16 *Greedy Algorithms*, Ch.17 *Amortized Analysis*, Ch.21 *Disjoint Sets*, Ch.22 *Elementary Graph Algorithms*, Ch.24 *Single-Source Shortest Paths*.
> - Trong kho: [`Algorithms/4 - Algorithms for Interviews`](../../Algorithms/4%20-%20Algorithms%20for%20Interviews%20-%20Chia%20s%E1%BA%BB%20code.pdf) (Aziz & Prakash) — Ch.1 *Searching*, Ch.2 *Sorting*, Ch.3 *Meta-algorithms*, Ch.4 *Algorithms on Graphs*, Ch.5 *Algorithms on Strings*, Ch.8 *Design Problems*, Ch.12 *Strategies For A Great Interview*.
> - Trong kho: [`Algorithms/5 - Algorithms in a Nutshell`](../../Algorithms/5%20-%20Algorithms_Nutshell%20-%20Chia%20s%E1%BA%BB%20code.pdf) — Ch.2 *The Mathematics of Algorithms*, Ch.4 *Sorting Algorithms*, Ch.5 *Searching*, Ch.6 *Graph Algorithms*.
> - Ngoài: *Cracking the Coding Interview* (Gayle L. McDowell, 6th ed.) — phần Big O và quy trình giải bài; [NeetCode Roadmap](https://neetcode.io/roadmap); [LeetCode](https://leetcode.com); mã nguồn JDK: `java.util.DualPivotQuicksort`, `java.util.TimSort`, `java.util.HashMap`, `java.util.PriorityQueue`.

## Mục lục
1. [Big-O và phân tích độ phức tạp](#1-big-o-và-phân-tích-độ-phức-tạp)
2. [Quy trình giải bài coding interview của một Senior](#2-quy-trình-giải-bài-coding-interview-của-một-senior)
3. [Arrays, Strings & Hashing](#3-arrays-strings--hashing)
4. [Two Pointers & Sliding Window](#4-two-pointers--sliding-window)
5. [Prefix Sum & Difference Array](#5-prefix-sum--difference-array)
6. [Binary Search (kể cả trên không gian đáp án)](#6-binary-search-kể-cả-trên-không-gian-đáp-án)
7. [Sorting — thuật toán và những gì JDK thực sự dùng](#7-sorting--thuật-toán-và-những-gì-jdk-thực-sự-dùng)
8. [Stack, Queue, Deque, Monotonic Stack/Queue](#8-stack-queue-deque-monotonic-stackqueue)
9. [Linked List](#9-linked-list)
10. [Trees & Binary Search Tree](#10-trees--binary-search-tree)
11. [Heap & Top-K](#11-heap--top-k)
12. [Trie](#12-trie)
13. [Graph](#13-graph)
14. [Backtracking](#14-backtracking)
15. [Greedy & Intervals](#15-greedy--intervals)
16. [Dynamic Programming](#16-dynamic-programming)
17. [Bit Manipulation](#17-bit-manipulation)
18. [Bài toán thực chiến của Senior](#18-bài-toán-thực-chiến-của-senior)
19. [Lộ trình luyện tập 75 bài theo pattern](#19-lộ-trình-luyện-tập-75-bài-theo-pattern)
20. [Dự án mini](#dự-án-mini)
21. [Checklist tự đánh giá](#checklist-tự-đánh-giá)

---

## 1. Big-O và phân tích độ phức tạp

### 1.1 Khái niệm
**Big-O** mô tả *tốc độ tăng* của chi phí (thời gian hoặc bộ nhớ) khi kích thước input `n` tiến ra vô cùng, bỏ qua hằng số và số hạng bậc thấp. Chính xác hơn (CLRS Ch.3):

| Ký hiệu | Ý nghĩa | Ví dụ |
|---|---|---|
| `O(g(n))` | cận trên: `f(n) ≤ c·g(n)` với mọi `n ≥ n0` | insertion sort là `O(n²)` |
| `Ω(g(n))` | cận dưới | mọi comparison sort là `Ω(n log n)` trong worst case |
| `Θ(g(n))` | cận chặt (vừa O vừa Ω) | merge sort là `Θ(n log n)` |

Trong phỏng vấn, khi nói "O(n)" người ta thường ngầm hiểu là cận chặt của **worst case** (trừ khi nói rõ average/amortized).

Thứ tự cần thuộc lòng:
```
O(1) < O(log n) < O(√n) < O(n) < O(n log n) < O(n²) < O(n³) < O(2ⁿ) < O(n!)
```

**Bảng "ước lượng ngược" từ ràng buộc input** (giả sử ~10⁸ thao tác đơn giản/giây, giới hạn 1–2s):

| n tối đa | Độ phức tạp chấp nhận được | Gợi ý kỹ thuật |
|---|---|---|
| ≤ 10–12 | `O(n!)`, `O(n·2ⁿ)` | permutation, backtracking |
| ≤ 20–25 | `O(2ⁿ)` | subset, bitmask DP |
| ≤ 500 | `O(n³)` | DP 3 chiều, Floyd–Warshall |
| ≤ 5·10³ | `O(n²)` | DP 2 chiều |
| ≤ 10⁶ | `O(n log n)` | sort, heap, binary search |
| ≤ 10⁸ | `O(n)` / `O(log n)` | two pointers, toán học |

### 1.2 Time vs Space, và các quy tắc tính
- **Vòng lặp tuần tự** cộng: `O(a) + O(b)`. **Lồng nhau** nhân: `O(a·b)`. Hai mảng khác nhau kích thước `a`, `b` → đừng gộp thành `n`.
- **Space** gồm bộ nhớ phụ (auxiliary) + **call stack** của đệ quy. DFS đệ quy trên cây lệch có độ sâu `n` → `O(n)` space và có thể `StackOverflowError` (stack mặc định của thread thường ~512KB–1MB, chỉnh bằng `-Xss`).
- **String trong Java là immutable**: `s += c` trong vòng lặp là `O(n²)` tổng cộng → dùng `StringBuilder`.
- `substring` từ Java 7u6 **copy** mảng → `O(k)`, không phải `O(1)`.

```java
// O(n²) ẩn: mỗi lần nối tạo String mới
String bad = "";
for (int i = 0; i < n; i++) bad += i;          // tổng ~ n²/2 ký tự được copy

// O(n)
StringBuilder sb = new StringBuilder();
for (int i = 0; i < n; i++) sb.append(i);
```

### 1.3 Amortized analysis (phân tích khấu hao)
Chi phí *trung bình trên một chuỗi thao tác* trong worst case (CLRS Ch.17) — khác với average case (trung bình theo phân phối input).

Ví dụ kinh điển: `ArrayList.add`. Khi đầy, `ArrayList` tăng capacity lên ~1.5 lần (`newCapacity = old + (old >> 1)`) và copy. Một lần `add` có thể tốn `O(n)`, nhưng tổng chi phí của `n` lần add là `O(n)` (chuỗi hình học), nên **amortized O(1)**.

```java
// Mô phỏng dynamic array để thấy tổng số lần copy ~ O(n)
public class DynArray {
    private int[] data = new int[1];
    private int size;
    long copies;                                  // đếm phần tử bị copy

    public void add(int x) {
        if (size == data.length) {
            int[] bigger = new int[data.length * 2];
            System.arraycopy(data, 0, bigger, 0, size);
            copies += size;
            data = bigger;
        }
        data[size++] = x;
    }

    public static void main(String[] args) {
        DynArray a = new DynArray();
        int n = 1_000_000;
        for (int i = 0; i < n; i++) a.add(i);
        System.out.println("copies/n = " + (double) a.copies / n); // < 2
    }
}
```
Các ví dụ amortized khác: `HashMap` resize, `ArrayDeque`, stack với `multipop`, union-find với path compression (gần như `O(1)` — chính xác là `O(α(n))`), monotonic stack (mỗi phần tử push/pop tối đa 1 lần → tổng `O(n)`).

### 1.4 Phân tích đệ quy
Viết **recurrence** rồi giải bằng một trong ba cách: cây đệ quy (recursion tree), thay thế (substitution), hoặc Master theorem.

- Fibonacci ngây thơ: `T(n) = T(n-1) + T(n-2) + O(1)` → `O(φⁿ) ≈ O(1.618ⁿ)` (thường nói `O(2ⁿ)` làm cận trên).
- Quy tắc nhanh cho cây đệ quy: **số nhánh ^ độ sâu** × công mỗi node. Backtracking sinh tất cả subset: `2ⁿ` lá, mỗi lá copy `O(n)` → `O(n·2ⁿ)`.

**Master theorem (bản rút gọn, CLRS 4.5)** cho `T(n) = a·T(n/b) + f(n)`, với `a ≥ 1, b > 1`. Đặt `c* = log_b(a)`:

| Trường hợp | Điều kiện | Kết quả | Ví dụ |
|---|---|---|---|
| 1 | `f(n) = O(n^c)` với `c < c*` | `Θ(n^c*)` | `T(n)=8T(n/2)+n²` → `Θ(n³)` |
| 2 | `f(n) = Θ(n^c*)` | `Θ(n^c* · log n)` | merge sort `2T(n/2)+n` → `Θ(n log n)`; binary search `T(n/2)+1` → `Θ(log n)` |
| 3 | `f(n) = Ω(n^c)` với `c > c*` (+ điều kiện regularity) | `Θ(f(n))` | `T(n)=2T(n/2)+n²` → `Θ(n²)` |

Master theorem **không** áp dụng cho `T(n) = T(n-1) + n` (không chia theo tỉ lệ) — dạng này giải bằng cộng dồn: `n + (n-1) + … = Θ(n²)` (quicksort worst case).

> 💡 **Góc nhìn Senior:** Big-O chỉ là một nửa câu chuyện. Trên JVM, hằng số và **cache locality** rất quan trọng: `int[]` liên tục trong bộ nhớ nhanh hơn `LinkedList<Integer>` (mỗi node một object, pointer chasing, autoboxing) hàng chục lần dù cùng `O(n)` duyệt. Khi phỏng vấn, nói thêm "về lý thuyết là X, nhưng thực tế tôi chọn Y vì locality/boxing/GC" là điểm cộng lớn.

> ⚠️ **Lỗi thường gặp:**
> - Quên tính chi phí của thao tác thư viện: `list.contains` (`O(n)`), `list.remove(0)` trên `ArrayList` (`O(n)`), `String.substring`/`split` (`O(k)`), `new ArrayList<>(other)` (`O(n)`).
> - Quên stack space của đệ quy khi nói "space O(1)".
> - Nói `HashMap.get` là `O(1)` mà không nói "trung bình"; worst case từ Java 8 là `O(log n)` nhờ treeify bucket (khi bucket ≥ 8 phần tử và table ≥ 64), trước Java 8 là `O(n)`.

### 🛠 Bài tập phần 1

**Bài 1.1 — Đọc độ phức tạp (Cơ bản)**
- Đề bài: cho 4 đoạn code sau, xác định time & space:
  ```java
  // (a)
  for (int i = 1; i < n; i *= 2) for (int j = 0; j < i; j++) work();
  // (b)
  for (int i = 0; i < n; i++) for (int j = i; j < n; j += i + 1) work();
  // (c)
  int f(int n) { return n <= 1 ? 1 : f(n / 2) + f(n / 2); }
  // (d)
  void g(List<Integer> list) { while (!list.isEmpty()) list.remove(0); } // list là ArrayList
  ```
- Tiêu chí đạt: đưa ra được đáp án kèm lập luận (tổng chuỗi, cây đệ quy).

**Bài 1.2 — Master theorem (Trung bình)**
- Đề bài: giải `T(n)=3T(n/2)+n` (Karatsuba), `T(n)=T(n/2)+n`, `T(n)=4T(n/2)+n²`, `T(n)=2T(n-1)+1`. Cái nào không dùng Master theorem được?

**Bài 1.3 — Đo amortized bằng thực nghiệm (Nâng cao)**
- Đề bài: dùng `DynArray` ở trên, đổi hệ số tăng từ ×2 thành `+100` (tăng cố định). Đo `copies/n` với `n = 10⁵, 10⁶`. Giải thích vì sao tăng tuyến tính làm amortized thành `O(n)`. Liên hệ với việc nên khởi tạo `new ArrayList<>(expectedSize)` / `HashMap` với capacity phù hợp.

<details>
<summary>Gợi ý lời giải</summary>

- 1.1(a): vòng ngoài `log n` lần, vòng trong `1+2+4+…+n/2 < n` → **O(n)** time, O(1) space.
- 1.1(b): với mỗi `i`, vòng trong chạy `≈ n/(i+1)` lần → tổng `n·(1 + 1/2 + … + 1/n) = O(n log n)` (chuỗi điều hoà).
- 1.1(c): `T(n)=2T(n/2)+1` → case 1 của Master (`c*=1`, `f=n⁰`) → **O(n)** time; space là độ sâu stack **O(log n)**.
- 1.1(d): mỗi `remove(0)` dịch `size-1` phần tử → `n + (n-1) + … = O(n²)`. Sửa: duyệt từ cuối `list.remove(list.size()-1)` hoặc `list.clear()`.
- 1.2: `3T(n/2)+n` → `c*=log₂3≈1.585 > 1` → `Θ(n^1.585)`; `T(n/2)+n` → case 3 → `Θ(n)`; `4T(n/2)+n²` → case 2 → `Θ(n² log n)`; `2T(n-1)+1` không dùng Master được (giảm trừ, không chia) → `Θ(2ⁿ)`.
- 1.3: tăng `+k` cố định → số lần resize `n/k`, mỗi lần copy trung bình `n/2` → tổng `O(n²/k)` → amortized `O(n)`. Tăng theo cấp số nhân (×1.5, ×2) là điều kiện để amortized O(1).

</details>

---

## 2. Quy trình giải bài coding interview của một Senior

### 2.1 Khung 6 bước (≈ 45 phút)
Phỏng vấn viên đánh giá **cách bạn suy nghĩ và giao tiếp**, không chỉ đáp án. Khung dưới đây tổng hợp từ *Cracking the Coding Interview* và *Algorithms for Interviews* Ch.12:

| Bước | Thời gian | Việc cần làm |
|---|---|---|
| 1. **Clarify** | 3–5' | Hỏi ràng buộc: kích thước `n`, giá trị âm? trùng lặp? input đã sort? rỗng/null? kiểu trả về khi không có đáp án? in-place được không? |
| 2. **Examples** | 2–3' | Tự lấy 1 ví dụ thường + 1–2 edge case (rỗng, 1 phần tử, toàn trùng, số âm, overflow). |
| 3. **Brute force** | 2–3' | Nói ra lời giải ngây thơ + độ phức tạp. *Không cần code*, nhưng chứng minh bạn hiểu bài. |
| 4. **Optimize** | 5–10' | Tìm **bottleneck** & **công việc lặp lại** (BUD: Bottlenecks, Unnecessary work, Duplicated work). Hỏi: sort có giúp không? hashing? hai con trỏ? DP? Ràng buộc `n` gợi ý độ phức tạp nào? |
| 5. **Code** | 15–20' | Code sạch, đặt tên rõ ràng, tách helper. Vừa code vừa nói. |
| 6. **Test & Complexity** | 5' | Chạy tay (dry-run) với ví dụ nhỏ, kiểm tra edge cases, **sau đó** nêu time/space. Đề xuất follow-up (stream? phân tán? đa luồng?). |

### 2.2 Bảng tra "dấu hiệu → pattern"

| Dấu hiệu trong đề | Pattern nên nghĩ tới |
|---|---|
| Mảng **đã sort**, tìm cặp/bộ ba | Two pointers, binary search |
| "subarray/substring liên tiếp dài nhất/ngắn nhất thoả điều kiện" | Sliding window |
| "tổng subarray bằng k", truy vấn tổng đoạn nhiều lần | Prefix sum + HashMap |
| "minimum của maximum" / "maximum của minimum", đáp án đơn điệu | Binary search on answer |
| "phần tử lớn hơn tiếp theo", histogram | Monotonic stack |
| max/min trong cửa sổ trượt | Monotonic deque |
| Top-K, K-th largest, merge K | Heap |
| Prefix của chuỗi, autocomplete | Trie |
| Quan hệ phụ thuộc, thứ tự | Topological sort |
| Thành phần liên thông động, "có cùng nhóm không" | Union-Find |
| Đường đi ngắn nhất không trọng số | BFS; có trọng số không âm → Dijkstra |
| "Liệt kê tất cả" tổ hợp/hoán vị | Backtracking |
| "Số cách", "min/max chi phí", bài toán con chồng lấp | Dynamic programming |
| Khoảng `[start, end]` | Sort theo start/end + greedy, sweep line |
| `n ≤ 20`, tập con | Bitmask |

### 2.3 Code mẫu "phong cách phỏng vấn"
```java
import java.util.*;

public class TwoSum {
    /**
     * Trả về chỉ số i < j sao cho nums[i] + nums[j] == target, hoặc null nếu không có.
     * Time O(n), Space O(n).
     */
    public static int[] twoSum(int[] nums, int target) {
        Objects.requireNonNull(nums, "nums");
        Map<Integer, Integer> seen = new HashMap<>(); // value -> index
        for (int i = 0; i < nums.length; i++) {
            int need = target - nums[i];
            Integer j = seen.get(need);
            if (j != null) return new int[]{j, i};
            seen.put(nums[i], i);                     // put SAU khi check để tránh dùng lại chính nó
        }
        return null;
    }

    public static void main(String[] args) {
        System.out.println(Arrays.toString(twoSum(new int[]{2, 7, 11, 15}, 9))); // [0, 1]
        System.out.println(Arrays.toString(twoSum(new int[]{3, 3}, 6)));         // [0, 1]
        System.out.println(Arrays.toString(twoSum(new int[]{1}, 2)));            // null
    }
}
```

> 💡 **Góc nhìn Senior:** ở level Senior, người phỏng vấn mong bạn **chủ động** nói về trade-off ("HashMap tốn O(n) bộ nhớ; nếu bộ nhớ hạn chế tôi sort rồi dùng two pointers O(n log n) / O(1), nhưng mất chỉ số gốc"), về **edge case production** (overflow `int`, input null, dữ liệu không vừa RAM) và về **khả năng mở rộng** (nếu dữ liệu là stream? nếu chạy trên nhiều máy?).

> ⚠️ **Lỗi thường gặp:**
> - Lao vào code ngay khi chưa clarify → giải sai bài.
> - Im lặng 5 phút. Hãy "think aloud", kể cả khi đang bí.
> - Integer overflow: `(lo + hi) / 2`, `a * b` với `int`. Dùng `lo + (hi - lo) / 2`, `Math.addExact`, hoặc `long`.
> - So sánh `Integer` bằng `==` (chỉ đúng trong cache -128..127). Dùng `equals` hoặc unbox.
> - Không test lại code; để lại bug off-by-one.

### 🛠 Bài tập phần 2

**Bài 2.1 — Clarify (Cơ bản)**
- Đề bài: với đề "Tìm phần tử xuất hiện nhiều nhất trong mảng", hãy viết ra ≥ 6 câu hỏi clarify và với mỗi câu, chỉ ra câu trả lời khác nhau làm lời giải thay đổi thế nào.

**Bài 2.2 — Từ brute force đến tối ưu (Trung bình)**
- Đề bài: *Contains Duplicate II* (LeetCode 219): cho `nums` và `k`, có tồn tại `i != j` sao cho `nums[i] == nums[j]` và `|i - j| ≤ k`? Trình bày đủ 6 bước: brute force `O(n·k)` → tối ưu.

**Bài 2.3 — Mock interview tự quay video (Nâng cao)**
- Đề bài: chọn 1 bài Medium chưa làm (ví dụ LeetCode 3 *Longest Substring Without Repeating Characters*), bật đồng hồ 35 phút, quay màn hình + giọng nói theo khung 6 bước. Xem lại và tự chấm theo 4 tiêu chí: giao tiếp, đúng đắn, chất lượng code, phân tích complexity.

<details>
<summary>Gợi ý lời giải</summary>

- 2.1: Mảng rỗng trả gì? Hoà (nhiều phần tử cùng tần suất) thì trả cái nào? Kiểu phần tử (int/String/object có `equals`)? Phạm vi giá trị (nhỏ → counting array thay HashMap)? Mảng có sort không (sort → đếm run liên tiếp, O(1) space)? Dữ liệu là stream / quá lớn (→ Count-Min Sketch, Misra-Gries)? Có phần tử chiếm > n/2 không (→ Boyer-Moore voting O(1) space)?
- 2.2: brute force 2 vòng lặp `O(n·k)`. Tối ưu: sliding window giữ `HashSet` gồm k phần tử gần nhất.
  ```java
  public boolean containsNearbyDuplicate(int[] nums, int k) {
      Set<Integer> window = new HashSet<>();
      for (int i = 0; i < nums.length; i++) {
          if (!window.add(nums[i])) return true;
          if (window.size() > k) window.remove(nums[i - k]);
      }
      return false;
  } // Time O(n), Space O(min(n,k))
  ```
- 2.3: lời giải tham khảo ở phần 4.

</details>

---

## 3. Arrays, Strings & Hashing

### 3.1 Khái niệm
- **Array**: truy cập ngẫu nhiên `O(1)`, chèn/xoá giữa `O(n)`. Java: `int[]` (primitive, liền mạch) vs `ArrayList<Integer>` (mảng tham chiếu tới object `Integer` → boxing).
- **String**: immutable, `char[]`/`byte[]` bên trong (Compact Strings từ Java 9: Latin-1 dùng 1 byte/char). Với bài chỉ có chữ thường, dùng `int[26]` thay `HashMap<Character,Integer>`.
- **Hashing** (CLRS Ch.11): `HashMap` dùng mảng bucket, `index = (h ^ (h >>> 16)) & (n - 1)`, capacity luôn là luỹ thừa của 2, load factor mặc định 0.75. Từ Java 8, bucket có thể chuyển thành red-black tree (treeify).

```java
import java.util.*;

public class HashingPatterns {
    // 1) Đếm tần suất: anagram (LeetCode 242) — O(n), O(1) space (26 chữ cái)
    static boolean isAnagram(String s, String t) {
        if (s.length() != t.length()) return false;
        int[] cnt = new int[26];
        for (int i = 0; i < s.length(); i++) {
            cnt[s.charAt(i) - 'a']++;
            cnt[t.charAt(i) - 'a']--;
        }
        for (int c : cnt) if (c != 0) return false;
        return true;
    }

    // 2) Gom nhóm theo "chữ ký": Group Anagrams (LeetCode 49) — O(n·k)
    static List<List<String>> groupAnagrams(String[] strs) {
        Map<String, List<String>> groups = new HashMap<>();
        for (String s : strs) {
            int[] cnt = new int[26];
            for (char c : s.toCharArray()) cnt[c - 'a']++;
            String key = Arrays.toString(cnt);                // chữ ký tần suất, tránh sort O(k log k)
            groups.computeIfAbsent(key, x -> new ArrayList<>()).add(s);
        }
        return new ArrayList<>(groups.values());
    }

    // 3) Set để tra cứu O(1): Longest Consecutive Sequence (LeetCode 128) — O(n)
    static int longestConsecutive(int[] nums) {
        Set<Integer> set = new HashSet<>();
        for (int x : nums) set.add(x);
        int best = 0;
        for (int x : set) {
            if (set.contains(x - 1)) continue;               // chỉ bắt đầu đếm từ đầu chuỗi
            int len = 1;
            while (set.contains(x + len)) len++;
            best = Math.max(best, len);
        }
        return best;
    }

    public static void main(String[] args) {
        System.out.println(isAnagram("anagram", "nagaram"));                       // true
        System.out.println(groupAnagrams(new String[]{"eat","tea","tan","ate","nat","bat"}));
        System.out.println(longestConsecutive(new int[]{100, 4, 200, 1, 3, 2}));   // 4
    }
}
```

### 3.2 Bên dưới nắp capo
- `hashCode`/`equals` contract: hai object `equals` thì **phải** cùng `hashCode`. Dùng object mutable làm key rồi sửa field → không tìm lại được.
- `Arrays.hashCode(int[])` / `Arrays.equals` để so sánh nội dung mảng; `int[]` làm key của `HashMap` sẽ so sánh **identity** → sai. Dùng `List<Integer>`, `String`, hoặc `record`.
- `HashMap` iteration order không xác định; cần thứ tự chèn → `LinkedHashMap`; cần sort theo key → `TreeMap` (`O(log n)`).
- Boyer–Moore majority vote: tìm phần tử chiếm > n/2 với `O(1)` space.

> 💡 **Góc nhìn Senior:** biết khi nào **không** dùng HashMap: miền giá trị nhỏ → mảng đếm (nhanh hơn ~10×, không boxing); cần range query / floor / ceiling → `TreeMap`; dữ liệu khổng lồ chỉ cần ước lượng → Bloom filter / HyperLogLog / Count-Min Sketch. Ngoài ra, hash flooding attack (nhiều key cùng hash) chính là lý do Java 8 thêm treeify.

> ⚠️ **Lỗi thường gặp:**
> - `map.get(key) + 1` khi key chưa có → `NullPointerException`. Dùng `map.merge(key, 1, Integer::sum)` hoặc `getOrDefault`.
> - Sửa `HashMap` trong khi duyệt bằng for-each → `ConcurrentModificationException`; dùng `iterator.remove()` hoặc `removeIf`.
> - `s.charAt(i) - 'a'` khi input có chữ hoa/Unicode → index âm.

### 🛠 Bài tập phần 3

**Bài 3.1 — Valid Sudoku (Cơ bản)** — LeetCode 36
- Đề bài: kiểm tra một bảng Sudoku 9×9 đã điền một phần có hợp lệ không (mỗi hàng, cột, ô 3×3 không trùng số 1–9). Không cần giải.

**Bài 3.2 — Product of Array Except Self (Trung bình)** — LeetCode 238
- Đề bài: trả về mảng `ans` với `ans[i]` là tích mọi phần tử trừ `nums[i]`, **không dùng phép chia**, `O(n)` time, `O(1)` space phụ (không tính mảng output).

**Bài 3.3 — Majority Element II (Trung bình)** — LeetCode 229
- Đề bài: tìm mọi phần tử xuất hiện > ⌊n/3⌋ lần, `O(n)` time, `O(1)` space.

**Bài 3.4 — Encode/Decode Strings (Nâng cao)** — LeetCode 271
- Đề bài: thiết kế `encode(List<String>) -> String` và `decode(String) -> List<String>` sao cho chuỗi có thể chứa **bất kỳ ký tự nào** (kể cả dấu phân cách).

<details>
<summary>Gợi ý lời giải</summary>

- 3.1: 27 `HashSet` hoặc một `Set<String>` chứa key như `"r0-5"`, `"c3-5"`, `"b1-5"`; box index = `(r/3)*3 + c/3`. O(81).
- 3.2: lượt 1 lưu prefix product vào `ans`, lượt 2 duyệt ngược nhân suffix product chạy trong biến `suffix`:
  ```java
  int n = nums.length; int[] ans = new int[n];
  ans[0] = 1;
  for (int i = 1; i < n; i++) ans[i] = ans[i - 1] * nums[i - 1];
  int suffix = 1;
  for (int i = n - 1; i >= 0; i--) { ans[i] *= suffix; suffix *= nums[i]; }
  ```
- 3.3: Boyer–Moore mở rộng với 2 ứng viên + 2 bộ đếm, sau đó duyệt lần 2 để xác nhận tần suất thực.
- 3.4: length-prefix: mỗi chuỗi mã hoá thành `len + "#" + s`. Khi decode, đọc số đến `#`, rồi lấy đúng `len` ký tự — không bao giờ nhầm vì không "tìm" dấu phân cách trong nội dung. Đây cũng là nguyên lý framing của nhiều giao thức (HTTP `Content-Length`, Kafka record).

</details>

---

## 4. Two Pointers & Sliding Window

### 4.1 Two pointers
Ba biến thể chính:
1. **Đối đầu** (opposite ends): `l = 0, r = n-1`, dịch dần vào giữa — dùng cho mảng đã sort (2Sum sorted, 3Sum, Container With Most Water, palindrome).
2. **Nhanh/chậm** (fast/slow): xoá trùng in-place, phát hiện chu trình (phần 9).
3. **Hai mảng**: merge hai mảng đã sort.

```java
import java.util.*;

public class TwoPointers {
    // 3Sum (LeetCode 15): O(n²) time, O(1) space phụ (không tính output và sort)
    static List<List<Integer>> threeSum(int[] nums) {
        Arrays.sort(nums);
        List<List<Integer>> res = new ArrayList<>();
        for (int i = 0; i < nums.length - 2; i++) {
            if (i > 0 && nums[i] == nums[i - 1]) continue;   // bỏ trùng phần tử thứ nhất
            if (nums[i] > 0) break;                          // phần tử nhỏ nhất > 0 → không thể tổng 0
            int l = i + 1, r = nums.length - 1;
            while (l < r) {
                int sum = nums[i] + nums[l] + nums[r];
                if (sum < 0) l++;
                else if (sum > 0) r--;
                else {
                    res.add(List.of(nums[i], nums[l], nums[r]));
                    while (l < r && nums[l] == nums[l + 1]) l++;
                    while (l < r && nums[r] == nums[r - 1]) r--;
                    l++; r--;
                }
            }
        }
        return res;
    }

    // Container With Most Water (LeetCode 11): luôn dịch cạnh thấp hơn
    static int maxArea(int[] h) {
        int l = 0, r = h.length - 1, best = 0;
        while (l < r) {
            best = Math.max(best, Math.min(h[l], h[r]) * (r - l));
            if (h[l] < h[r]) l++; else r--;
        }
        return best;
    }

    // Remove Duplicates from Sorted Array (LeetCode 26): slow/fast
    static int removeDuplicates(int[] a) {
        if (a.length == 0) return 0;
        int slow = 0;
        for (int fast = 1; fast < a.length; fast++)
            if (a[fast] != a[slow]) a[++slow] = a[fast];
        return slow + 1;
    }

    public static void main(String[] args) {
        System.out.println(threeSum(new int[]{-1, 0, 1, 2, -1, -4})); // [[-1,-1,2],[-1,0,1]]
        System.out.println(maxArea(new int[]{1, 8, 6, 2, 5, 4, 8, 3, 7})); // 49
    }
}
```
**Vì sao Container With Most Water đúng?** Nếu `h[l] < h[r]`, mọi cặp `(l, r')` với `r' < r` đều có chiều cao ≤ `h[l]` và chiều rộng nhỏ hơn → không thể tốt hơn `(l, r)`. Vậy loại `l` an toàn. Khả năng **chứng minh tính đúng của greedy/two pointers** là thứ phân biệt Senior.

### 4.2 Sliding window
Template cho "cửa sổ dài nhất thoả điều kiện" (cửa sổ co giãn):
```
for right in 0..n-1:
    thêm a[right] vào window
    while (window vi phạm điều kiện):
        bỏ a[left] khỏi window; left++
    cập nhật đáp án với (right - left + 1)
```
Với "cửa sổ **ngắn nhất** thoả điều kiện": trong `while (window thoả)` thì cập nhật đáp án rồi mới co `left`.

```java
import java.util.*;

public class SlidingWindow {
    // LeetCode 3: Longest Substring Without Repeating Characters — O(n)
    static int lengthOfLongestSubstring(String s) {
        Map<Character, Integer> last = new HashMap<>(); // ký tự -> vị trí xuất hiện gần nhất
        int best = 0, left = 0;
        for (int right = 0; right < s.length(); right++) {
            char c = s.charAt(right);
            Integer prev = last.get(c);
            if (prev != null && prev >= left) left = prev + 1;  // nhảy left qua bản trùng
            last.put(c, right);
            best = Math.max(best, right - left + 1);
        }
        return best;
    }

    // LeetCode 76: Minimum Window Substring — O(|s| + |t|)
    static String minWindow(String s, String t) {
        if (t.isEmpty() || s.length() < t.length()) return "";
        int[] need = new int[128];
        for (char c : t.toCharArray()) need[c]++;
        int missing = t.length();          // số ký tự còn thiếu (tính cả lặp)
        int bestL = 0, bestLen = Integer.MAX_VALUE, left = 0;
        for (int right = 0; right < s.length(); right++) {
            if (need[s.charAt(right)]-- > 0) missing--;
            while (missing == 0) {          // window hợp lệ → thử co
                if (right - left + 1 < bestLen) { bestLen = right - left + 1; bestL = left; }
                if (++need[s.charAt(left++)] > 0) missing++;
            }
        }
        return bestLen == Integer.MAX_VALUE ? "" : s.substring(bestL, bestL + bestLen);
    }

    // LeetCode 424: Longest Repeating Character Replacement
    static int characterReplacement(String s, int k) {
        int[] cnt = new int[26];
        int left = 0, maxFreq = 0, best = 0;
        for (int right = 0; right < s.length(); right++) {
            maxFreq = Math.max(maxFreq, ++cnt[s.charAt(right) - 'A']);
            while (right - left + 1 - maxFreq > k) cnt[s.charAt(left++) - 'A']--;
            best = Math.max(best, right - left + 1);
        }
        return best;
    }

    public static void main(String[] args) {
        System.out.println(lengthOfLongestSubstring("abcabcbb")); // 3
        System.out.println(minWindow("ADOBECODEBANC", "ABC"));   // BANC
        System.out.println(characterReplacement("AABABBA", 1));  // 4
    }
}
```

> 💡 **Góc nhìn Senior:** sliding window yêu cầu tính **đơn điệu**: mở rộng cửa sổ chỉ làm điều kiện "xấu đi" theo một hướng. Với mảng có **số âm** và đề "subarray sum = k", sliding window **sai** → phải dùng prefix sum + HashMap (phần 5). Đây là câu bẫy hay gặp. Trong thực tế, sliding window chính là nền tảng của rate limiter (phần 18) và metrics "requests trong 1 phút gần nhất".

> ⚠️ **Lỗi thường gặp:**
> - Trong LeetCode 424, nhiều người "giảm `maxFreq`" khi co cửa sổ — không cần, vì đáp án chỉ tăng khi `maxFreq` tăng.
> - Quên điều kiện `prev >= left` trong LeetCode 3 → `left` bị kéo lùi.
> - Dùng `HashMap<Character,Integer>` khi chỉ có ASCII → chậm vì boxing; `int[128]` đủ.

### 🛠 Bài tập phần 4

**Bài 4.1 — Valid Palindrome (Cơ bản)** — LeetCode 125
- Đề bài: kiểm tra chuỗi có phải palindrome khi chỉ xét chữ và số, không phân biệt hoa thường. `O(1)` space.

**Bài 4.2 — Trapping Rain Water (Trung bình/Khó)** — LeetCode 42
- Đề bài: cho mảng độ cao `h`, tính lượng nước đọng sau mưa. Yêu cầu `O(n)` time, `O(1)` space.

**Bài 4.3 — Permutation in String (Trung bình)** — LeetCode 567
- Đề bài: `s2` có chứa một hoán vị của `s1` dưới dạng substring không?

**Bài 4.4 — Subarrays with K Different Integers (Nâng cao)** — LeetCode 992
- Đề bài: đếm số subarray có **đúng** `k` số nguyên phân biệt.

<details>
<summary>Gợi ý lời giải</summary>

- 4.1: `l`, `r` đối đầu, bỏ qua ký tự `!Character.isLetterOrDigit`, so sánh `Character.toLowerCase`.
- 4.2: two pointers với `leftMax`, `rightMax`; bên nào có max nhỏ hơn thì nước ở đó được quyết định bởi max đó:
  ```java
  int l = 0, r = h.length - 1, lMax = 0, rMax = 0, water = 0;
  while (l < r) {
      if (h[l] < h[r]) { lMax = Math.max(lMax, h[l]); water += lMax - h[l]; l++; }
      else            { rMax = Math.max(rMax, h[r]); water += rMax - h[r]; r--; }
  }
  ```
- 4.3: cửa sổ **cố định** độ dài `|s1|`, so sánh 2 mảng đếm `int[26]` (hoặc duy trì biến `matches` để so sánh O(1)). O(|s2|).
- 4.4: mẹo "đúng k = nhiều nhất k − nhiều nhất (k−1)". Hàm `atMost(k)` dùng sliding window: với mỗi `right`, cộng `right - left + 1` vào kết quả. Tổng O(n).

</details>

---

## 5. Prefix Sum & Difference Array

### 5.1 Khái niệm
`prefix[i] = a[0] + … + a[i-1]` (đặt `prefix[0] = 0` để tránh if). Tổng đoạn `[l, r]` = `prefix[r+1] - prefix[l]` → mỗi truy vấn `O(1)` sau `O(n)` tiền xử lý.

Kết hợp **HashMap** để đếm subarray: "subarray `(j, i]` có tổng k" ⇔ `prefix[i] - prefix[j] = k` ⇔ tìm số `j` có `prefix[j] = prefix[i] - k`.

**Difference array** là phép ngược: cộng `v` vào đoạn `[l, r]` bằng `diff[l] += v; diff[r+1] -= v`, cuối cùng lấy prefix sum → `O(1)` mỗi update, phù hợp nhiều update rồi đọc một lần (đặt phòng, chuyến bay, sweep line).

```java
import java.util.*;

public class PrefixSum {
    // LeetCode 560: Subarray Sum Equals K (có số âm) — O(n)
    static int subarraySum(int[] nums, int k) {
        Map<Integer, Integer> count = new HashMap<>();
        count.put(0, 1);                       // prefix rỗng
        int sum = 0, res = 0;
        for (int x : nums) {
            sum += x;
            res += count.getOrDefault(sum - k, 0);
            count.merge(sum, 1, Integer::sum);
        }
        return res;
    }

    // Prefix sum 2D (LeetCode 304): tổng hình chữ nhật O(1)
    static final class Matrix2D {
        private final int[][] p;
        Matrix2D(int[][] m) {
            int R = m.length, C = R == 0 ? 0 : m[0].length;
            p = new int[R + 1][C + 1];
            for (int i = 0; i < R; i++)
                for (int j = 0; j < C; j++)
                    p[i + 1][j + 1] = m[i][j] + p[i][j + 1] + p[i + 1][j] - p[i][j];
        }
        int sumRegion(int r1, int c1, int r2, int c2) {
            return p[r2 + 1][c2 + 1] - p[r1][c2 + 1] - p[r2 + 1][c1] + p[r1][c1];
        }
    }

    // Difference array: LeetCode 1109 Corporate Flight Bookings
    static int[] corpFlightBookings(int[][] bookings, int n) {
        int[] diff = new int[n + 1];
        for (int[] b : bookings) {             // b = {first, last, seats}, 1-indexed
            diff[b[0] - 1] += b[2];
            diff[b[1]] -= b[2];
        }
        int[] res = new int[n];
        int run = 0;
        for (int i = 0; i < n; i++) res[i] = run += diff[i];
        return res;
    }

    public static void main(String[] args) {
        System.out.println(subarraySum(new int[]{1, -1, 1, 1}, 2)); // 2
        Matrix2D m = new Matrix2D(new int[][]{{3, 0, 1}, {5, 6, 3}, {1, 2, 0}});
        System.out.println(m.sumRegion(1, 1, 2, 2));                  // 11
        System.out.println(Arrays.toString(
            corpFlightBookings(new int[][]{{1, 2, 10}, {2, 3, 20}, {2, 5, 25}}, 5))); // [10,55,45,25,25]
    }
}
```

> 💡 **Góc nhìn Senior:** prefix sum là ý tưởng nền của nhiều hệ thống: **cumulative counters** trong Prometheus (`rate()` = hiệu hai mẫu chia thời gian), summed-area table trong xử lý ảnh, và "running balance" trong hệ thống tài chính. Khi dữ liệu **thay đổi liên tục** và vẫn cần truy vấn đoạn, chuyển sang **Fenwick tree (BIT)** hoặc **segment tree** (`O(log n)` cả update và query).

> ⚠️ **Lỗi thường gặp:**
> - Quên `count.put(0, 1)` → bỏ sót subarray bắt đầu từ index 0.
> - Tổng có thể vượt `int` (n = 10⁵, giá trị 10⁹) → dùng `long[] prefix`.
> - Lệch index khi trộn 0-based và 1-based.

### 🛠 Bài tập phần 5

**Bài 5.1 — Find Pivot Index (Cơ bản)** — LeetCode 724
- Đề bài: tìm index đầu tiên mà tổng bên trái bằng tổng bên phải.

**Bài 5.2 — Continuous Subarray Sum (Trung bình)** — LeetCode 523
- Đề bài: có subarray độ dài ≥ 2 mà tổng là bội của `k` không?

**Bài 5.3 — Range Sum Query Mutable bằng Fenwick Tree (Nâng cao)** — LeetCode 307
- Đề bài: hỗ trợ `update(i, val)` và `sumRange(l, r)` đều `O(log n)`.

<details>
<summary>Gợi ý lời giải</summary>

- 5.1: `total` = tổng mảng; duyệt giữ `leftSum`; `if (leftSum == total - leftSum - nums[i]) return i`.
- 5.2: lưu `Map<Integer,Integer>` từ `prefix % k` → **index sớm nhất**; khởi tạo `map.put(0, -1)`. Nếu gặp lại cùng số dư tại `i` và `i - map.get(mod) >= 2` → true. (Hai prefix cùng số dư ⇒ hiệu chia hết cho k.)
- 5.3: Fenwick tree 1-indexed:
  ```java
  class Fenwick {
      private final long[] tree;
      Fenwick(int n) { tree = new long[n + 1]; }
      void add(int i, long delta) { for (i++; i < tree.length; i += i & -i) tree[i] += delta; }
      long prefix(int i) { long s = 0; for (i++; i > 0; i -= i & -i) s += tree[i]; return s; } // tổng [0..i]
      long range(int l, int r) { return prefix(r) - (l > 0 ? prefix(l - 1) : 0); }
  }
  ```
  `update(i, val)`: giữ mảng gốc `a`, gọi `add(i, val - a[i]); a[i] = val;`. `i & -i` lấy bit thấp nhất (xem phần 17).

</details>

---

## 6. Binary Search (kể cả trên không gian đáp án)

### 6.1 Khái niệm
Binary search không chỉ là "tìm x trong mảng sort". Bản chất: **có một vị từ (predicate) đơn điệu** `P(x)` dạng `false…false true…true`; tìm `x` nhỏ nhất mà `P(x) = true`. `O(log n)` lần đánh giá `P`.

Template **lower bound** (nửa mở `[lo, hi)`) — nên thuộc lòng một template duy nhất và dùng mãi:
```java
public class BinarySearch {
    /** Index nhỏ nhất i trong [0, n] sao cho a[i] >= target (n nếu không có). */
    static int lowerBound(int[] a, int target) {
        int lo = 0, hi = a.length;              // đáp án nằm trong [lo, hi]
        while (lo < hi) {
            int mid = lo + (hi - lo) / 2;       // tránh overflow (hoặc (lo + hi) >>> 1)
            if (a[mid] >= target) hi = mid;     // P(mid) true → đáp án ở mid hoặc bên trái
            else lo = mid + 1;
        }
        return lo;
    }

    static int upperBound(int[] a, int target) {  // index nhỏ nhất a[i] > target
        int lo = 0, hi = a.length;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (a[mid] > target) hi = mid; else lo = mid + 1;
        }
        return lo;
    }

    // LeetCode 33: Search in Rotated Sorted Array (phần tử phân biệt)
    static int searchRotated(int[] a, int target) {
        int lo = 0, hi = a.length - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (a[mid] == target) return mid;
            if (a[lo] <= a[mid]) {                          // nửa trái đã sort
                if (a[lo] <= target && target < a[mid]) hi = mid - 1; else lo = mid + 1;
            } else {                                        // nửa phải đã sort
                if (a[mid] < target && target <= a[hi]) lo = mid + 1; else hi = mid - 1;
            }
        }
        return -1;
    }

    public static void main(String[] args) {
        int[] a = {1, 2, 2, 2, 5, 7};
        System.out.println(lowerBound(a, 2) + " " + upperBound(a, 2)); // 1 4 → có 3 số 2
        System.out.println(searchRotated(new int[]{4, 5, 6, 7, 0, 1, 2}, 0)); // 4
    }
}
```

### 6.2 Binary search trên không gian đáp án
Khi đề hỏi "giá trị **nhỏ nhất** sao cho làm được" và nếu làm được với `x` thì làm được với mọi `x' > x` → binary search trên `x`, mỗi bước kiểm tra `feasible(x)` bằng greedy `O(n)`.

```java
public class AnswerSpace {
    // LeetCode 875: Koko Eating Bananas — tốc độ nhỏ nhất ăn hết trong h giờ
    static int minEatingSpeed(int[] piles, int h) {
        int lo = 1, hi = 0;
        for (int p : piles) hi = Math.max(hi, p);
        while (lo < hi) {
            int mid = lo + (hi - lo) / 2;
            if (hoursNeeded(piles, mid) <= h) hi = mid; else lo = mid + 1;
        }
        return lo;
    }
    private static long hoursNeeded(int[] piles, int speed) {
        long hours = 0;
        for (int p : piles) hours += (p + speed - 1) / speed;   // ceil(p / speed)
        return hours;
    }

    // LeetCode 1011: Capacity To Ship Packages Within D Days
    static int shipWithinDays(int[] w, int days) {
        int lo = 0, hi = 0;
        for (int x : w) { lo = Math.max(lo, x); hi += x; }
        while (lo < hi) {
            int cap = (lo + hi) >>> 1;
            int need = 1, cur = 0;
            for (int x : w) {
                if (cur + x > cap) { need++; cur = 0; }
                cur += x;
            }
            if (need <= days) hi = cap; else lo = cap + 1;
        }
        return lo;
    }

    public static void main(String[] args) {
        System.out.println(minEatingSpeed(new int[]{3, 6, 7, 11}, 8));                 // 4
        System.out.println(shipWithinDays(new int[]{1,2,3,4,5,6,7,8,9,10}, 5));        // 15
    }
}
```

> 💡 **Góc nhìn Senior:** bug `(lo + hi) / 2` overflow tồn tại **9 năm** trong `java.util.Arrays.binarySearch` (Joshua Bloch, 2006). JDK hiện dùng `(low + high) >>> 1`. Ngoài ra, `Arrays.binarySearch` / `Collections.binarySearch` trả `-(insertionPoint) - 1` khi không tìm thấy và **không đảm bảo** trả index đầu tiên khi có trùng — muốn lower bound phải tự viết hoặc dùng `TreeMap.ceilingKey`. Binary search cũng xuất hiện trong thực tế: `git bisect`, tìm phiên bản gây regression, tìm "batch size lớn nhất không OOM".

> ⚠️ **Lỗi thường gặp:**
> - Trộn lẫn template `[lo, hi]` và `[lo, hi)` → vòng lặp vô hạn (`lo = mid` với `mid` làm tròn xuống).
> - Cận trên của không gian đáp án quá nhỏ (VD tổng trọng lượng phải là `long`).
> - Áp binary search khi vị từ **không đơn điệu**.

### 🛠 Bài tập phần 6

**Bài 6.1 — First and Last Position (Cơ bản)** — LeetCode 34
- Đề bài: tìm vị trí đầu và cuối của `target` trong mảng sort, `O(log n)`.

**Bài 6.2 — Find Minimum in Rotated Sorted Array (Trung bình)** — LeetCode 153

**Bài 6.3 — Time Based Key-Value Store (Trung bình)** — LeetCode 981
- Đề bài: `set(key, value, timestamp)` (timestamp tăng dần), `get(key, timestamp)` trả value có timestamp lớn nhất ≤ timestamp truy vấn.

**Bài 6.4 — Median of Two Sorted Arrays (Nâng cao)** — LeetCode 4
- Đề bài: `O(log(min(m, n)))`.

<details>
<summary>Gợi ý lời giải</summary>

- 6.1: `[lowerBound(t), upperBound(t) - 1]`, nếu `lowerBound == n || a[lb] != t` → `[-1, -1]`.
- 6.2: so `a[mid]` với `a[hi]`: `a[mid] > a[hi]` → min nằm bên phải (`lo = mid + 1`), ngược lại `hi = mid`.
- 6.3: `Map<String, TreeMap<Integer,String>>` rồi `floorEntry(timestamp)` — `O(log n)`; hoặc `Map<String, List<Pair>>` + binary search upper bound − 1 (nhanh hơn vì timestamp tăng dần, append O(1)).
- 6.4: binary search vị trí cắt `i` trên mảng ngắn hơn, `j = (m + n + 1)/2 - i`; điều kiện đúng: `A[i-1] <= B[j]` và `B[j-1] <= A[i]` (dùng `Integer.MIN_VALUE/MAX_VALUE` cho biên). Median = max trái (lẻ) hoặc trung bình max trái & min phải (chẵn).

</details>

---

## 7. Sorting — thuật toán và những gì JDK thực sự dùng

### 7.1 So sánh các thuật toán

| Thuật toán | Best | Average | Worst | Space | Stable | Ghi chú |
|---|---|---|---|---|---|---|
| Insertion sort | `O(n)` | `O(n²)` | `O(n²)` | `O(1)` | ✔ | Rất nhanh với mảng nhỏ / gần sort |
| Merge sort | `O(n log n)` | `O(n log n)` | `O(n log n)` | `O(n)` | ✔ | Dễ song song hoá, dùng cho external sort |
| Quicksort | `O(n log n)` | `O(n log n)` | `O(n²)` | `O(log n)` stack | ✘ | Hằng số nhỏ, cache-friendly |
| Heapsort | `O(n log n)` | `O(n log n)` | `O(n log n)` | `O(1)` | ✘ | Worst case đảm bảo, nhưng cache kém |
| Counting / Radix | `O(n + k)` | `O(n + k)` | `O(n + k)` | `O(n + k)` | ✔ | Không so sánh; chỉ cho khoá nguyên phạm vi nhỏ (CLRS Ch.8) |

Mọi **comparison sort** có cận dưới `Ω(n log n)` (CLRS 8.1, lập luận cây quyết định: có `n!` hoán vị → chiều cao ≥ `log₂(n!) = Θ(n log n)`).

**Stable** = các phần tử bằng nhau giữ nguyên thứ tự tương đối. Quan trọng khi sort **nhiều khoá**: sort theo tên, rồi sort ổn định theo phòng ban → trong mỗi phòng ban vẫn theo tên.

### 7.2 Cài đặt
```java
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class Sorts {
    // Quicksort với pivot ngẫu nhiên + Hoare partition
    static void quickSort(int[] a, int lo, int hi) {
        if (lo >= hi) return;
        int p = a[lo + ThreadLocalRandom.current().nextInt(hi - lo + 1)];
        int i = lo, j = hi;
        while (i <= j) {
            while (a[i] < p) i++;
            while (a[j] > p) j--;
            if (i <= j) { swap(a, i, j); i++; j--; }
        }
        quickSort(a, lo, j);
        quickSort(a, i, hi);
    }

    // Merge sort top-down, dùng 1 buffer phụ cấp phát một lần
    static void mergeSort(int[] a) { mergeSort(a, new int[a.length], 0, a.length - 1); }
    private static void mergeSort(int[] a, int[] buf, int lo, int hi) {
        if (lo >= hi) return;
        int mid = (lo + hi) >>> 1;
        mergeSort(a, buf, lo, mid);
        mergeSort(a, buf, mid + 1, hi);
        if (a[mid] <= a[mid + 1]) return;          // đã có thứ tự → bỏ merge (tối ưu như TimSort)
        System.arraycopy(a, lo, buf, lo, hi - lo + 1);
        int i = lo, j = mid + 1, k = lo;
        while (i <= mid && j <= hi) a[k++] = (buf[i] <= buf[j]) ? buf[i++] : buf[j++]; // <= giữ stable
        while (i <= mid) a[k++] = buf[i++];
        while (j <= hi)  a[k++] = buf[j++];
    }

    // Heapsort in-place: build max-heap O(n), rồi n lần sift-down O(log n)
    static void heapSort(int[] a) {
        int n = a.length;
        for (int i = n / 2 - 1; i >= 0; i--) siftDown(a, i, n);
        for (int end = n - 1; end > 0; end--) {
            swap(a, 0, end);
            siftDown(a, 0, end);
        }
    }
    private static void siftDown(int[] a, int i, int n) {
        while (2 * i + 1 < n) {
            int c = 2 * i + 1;
            if (c + 1 < n && a[c + 1] > a[c]) c++;
            if (a[i] >= a[c]) return;
            swap(a, i, c);
            i = c;
        }
    }

    private static void swap(int[] a, int i, int j) { int t = a[i]; a[i] = a[j]; a[j] = t; }

    public static void main(String[] args) {
        int[] x = ThreadLocalRandom.current().ints(20, 0, 100).toArray();
        int[] y = x.clone(), z = x.clone(), expected = x.clone();
        Arrays.sort(expected);
        quickSort(x, 0, x.length - 1); mergeSort(y); heapSort(z);
        System.out.println(Arrays.equals(x, expected) && Arrays.equals(y, expected)
                           && Arrays.equals(z, expected)); // true
    }
}
```

### 7.3 Bên dưới nắp capo: JDK sort gì?
- **`Arrays.sort(int[]/long[]/double[]…)`** (primitive): **Dual-Pivot Quicksort** (Vladimir Yaroslavskiy, từ Java 7). Mảng nhỏ chuyển sang insertion sort; từ JDK 14 cài đặt được viết lại, có thêm nhận diện run để merge, và **fallback sang heapsort** khi đệ quy quá sâu → tránh `O(n²)`; với `byte[]`/`short[]`/`char[]` lớn dùng counting sort. Primitive **không cần stable** (hai số `5` không phân biệt được) nên được chọn quicksort vì nhanh và in-place.
- **`Arrays.sort(Object[])`, `Arrays.sort(T[], Comparator)`, `Collections.sort`, `List.sort`**: **TimSort** (hybrid merge sort + insertion sort của Tim Peters, từ Java 7) — **stable**, `O(n log n)` worst, `O(n)` với dữ liệu đã gần sort (tận dụng "run" có sẵn), cần tới `n/2` bộ nhớ phụ. Object **cần stable** vì hai object `compare` bằng nhau vẫn có thể khác nhau.
- `Collections.sort(list)` gọi `list.sort(null)`; mặc định của `List.sort` là copy ra mảng (`toArray`), `Arrays.sort`, rồi ghi lại → sort `LinkedList` vẫn `O(n log n)`.
- **`Arrays.parallelSort`**: chia mảng, sort song song trên `ForkJoinPool.commonPool()` rồi merge; chỉ có lợi với mảng lớn (ngưỡng granularity 8192 phần tử).
- TimSort ném `IllegalArgumentException: Comparison method violates its general contract!` nếu `Comparator` không nhất quán (không bắc cầu).

```java
import java.util.*;

public class ComparatorDemo {
    record Employee(String name, String dept, int salary) {}

    public static void main(String[] args) {
        List<Employee> list = new ArrayList<>(List.of(
            new Employee("An", "IT", 3000), new Employee("Binh", "HR", 2500),
            new Employee("Chi", "IT", 3000), new Employee("Dung", "HR", 4000)));

        // Sort đa khoá: dept tăng, salary giảm, name tăng
        list.sort(Comparator.comparing(Employee::dept)
                .thenComparing(Employee::salary, Comparator.reverseOrder())
                .thenComparing(Employee::name));
        list.forEach(System.out::println);

        // SAI: overflow khi trừ (a - b) với số lớn trái dấu → vi phạm contract
        Comparator<Integer> bad = (a, b) -> a - b;
        Comparator<Integer> good = Integer::compare;
        System.out.println(bad.compare(Integer.MIN_VALUE, 1) + " vs " + good.compare(Integer.MIN_VALUE, 1));
    }
}
```

> 💡 **Góc nhìn Senior:** câu hỏi "vì sao Java dùng hai thuật toán khác nhau cho primitive và object?" là câu kinh điển. Trả lời đủ ý: (1) stability chỉ có ý nghĩa với object; (2) TimSort tối ưu cho dữ liệu thực tế thường đã có thứ tự cục bộ và giảm số lần `compare()` (vốn đắt với object, gọi qua virtual call); (3) quicksort in-place, không cần bộ nhớ phụ và cache-friendly với primitive. Trên production, đừng sort một `List` hàng triệu phần tử trong mỗi request — hãy để DB `ORDER BY` với index, hoặc duy trì cấu trúc đã có thứ tự (`TreeMap`, heap).

> ⚠️ **Lỗi thường gặp:**
> - Comparator `(a, b) -> a - b` bị overflow.
> - Quicksort chọn pivot cố định `a[lo]` → `O(n²)` trên mảng đã sort, và `StackOverflowError`.
> - `Arrays.sort(int[], Comparator)` không tồn tại — muốn sort giảm dần mảng primitive phải dùng `Integer[]` hoặc sort tăng rồi đảo ngược.

### 🛠 Bài tập phần 7

**Bài 7.1 — Sort Colors (Cơ bản)** — LeetCode 75
- Đề bài: mảng chỉ gồm 0, 1, 2; sort in-place một lượt (Dutch National Flag của Dijkstra).

**Bài 7.2 — Largest Number (Trung bình)** — LeetCode 179
- Đề bài: sắp xếp các số nguyên không âm để ghép thành số lớn nhất, trả về `String`.

**Bài 7.3 — Count of Smaller Numbers After Self (Nâng cao)** — LeetCode 315
- Đề bài: với mỗi `i`, đếm số phần tử bên phải nhỏ hơn `nums[i]`. Gợi ý: biến thể merge sort đếm nghịch thế.

<details>
<summary>Gợi ý lời giải</summary>

- 7.1: ba con trỏ `low, mid, high`: `0` → swap(low++, mid++); `1` → mid++; `2` → swap(mid, high--). O(n), O(1).
- 7.2: chuyển sang `String[]`, sort với comparator `(a, b) -> (b + a).compareTo(a + b)`; nếu phần tử đầu là `"0"` thì trả `"0"`. Tính bắc cầu của comparator này đã được chứng minh nên TimSort không ném lỗi.
- 7.3: merge sort trên mảng **index**; khi merge, lúc lấy một phần tử từ nửa trái, cộng vào `count[idx]` số phần tử nửa phải đã được lấy trước nó (`rightTaken`). O(n log n). Cách khác: Fenwick tree trên giá trị đã nén (coordinate compression), duyệt từ phải sang.

</details>

---

## 8. Stack, Queue, Deque, Monotonic Stack/Queue

### 8.1 Khái niệm & lựa chọn class trong Java
| Nhu cầu | Dùng | Tránh |
|---|---|---|
| Stack (LIFO) | `ArrayDeque<E>`: `push/pop/peek` | `java.util.Stack` (kế thừa `Vector`, synchronized, legacy) |
| Queue (FIFO) | `ArrayDeque<E>`: `offer/poll/peek` | `LinkedList` (tốn bộ nhớ hơn, cache kém) |
| Deque hai đầu | `ArrayDeque<E>`: `offerFirst/offerLast/pollFirst/pollLast` | |
| Queue đa luồng | `ArrayBlockingQueue`, `LinkedBlockingQueue`, `ConcurrentLinkedQueue` | |

Lưu ý: `ArrayDeque` **không cho phép `null`**.

### 8.2 Stack cổ điển
```java
import java.util.*;

public class StackPatterns {
    // LeetCode 20: Valid Parentheses
    static boolean isValid(String s) {
        Deque<Character> st = new ArrayDeque<>();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '(' -> st.push(')');
                case '[' -> st.push(']');
                case '{' -> st.push('}');
                default -> { if (st.isEmpty() || st.pop() != c) return false; }
            }
        }
        return st.isEmpty();
    }

    // LeetCode 150: Evaluate Reverse Polish Notation
    static int evalRPN(String[] tokens) {
        Deque<Integer> st = new ArrayDeque<>();
        for (String t : tokens) {
            switch (t) {
                case "+" -> st.push(st.pop() + st.pop());
                case "*" -> st.push(st.pop() * st.pop());
                case "-" -> { int b = st.pop(), a = st.pop(); st.push(a - b); }
                case "/" -> { int b = st.pop(), a = st.pop(); st.push(a / b); }
                default -> st.push(Integer.parseInt(t));
            }
        }
        return st.pop();
    }

    // LeetCode 155: Min Stack — mỗi phần tử lưu kèm min tại thời điểm push
    static final class MinStack {
        private final Deque<int[]> st = new ArrayDeque<>();   // {value, minSoFar}
        void push(int x) { st.push(new int[]{x, st.isEmpty() ? x : Math.min(x, st.peek()[1])}); }
        void pop() { st.pop(); }
        int top() { return st.peek()[0]; }
        int getMin() { return st.peek()[1]; }
    }

    public static void main(String[] args) {
        System.out.println(isValid("({[]})") + " " + isValid("(]"));          // true false
        System.out.println(evalRPN(new String[]{"2", "1", "+", "3", "*"}));   // 9
    }
}
```

### 8.3 Monotonic stack
Giữ stack mà các phần tử **đơn điệu** (tăng hoặc giảm). Khi phần tử mới phá vỡ tính đơn điệu, pop — và chính lúc pop là lúc ta biết "phần tử lớn hơn tiếp theo" của phần tử bị pop. Mỗi phần tử push/pop đúng 1 lần → **O(n) amortized**.

```java
import java.util.*;

public class MonotonicStack {
    // LeetCode 739: Daily Temperatures — số ngày phải chờ đến ngày ấm hơn
    static int[] dailyTemperatures(int[] t) {
        int[] ans = new int[t.length];
        Deque<Integer> st = new ArrayDeque<>();        // chứa index, nhiệt độ giảm dần
        for (int i = 0; i < t.length; i++) {
            while (!st.isEmpty() && t[st.peek()] < t[i]) {
                int j = st.pop();
                ans[j] = i - j;
            }
            st.push(i);
        }
        return ans;
    }

    // LeetCode 84: Largest Rectangle in Histogram — O(n)
    static int largestRectangleArea(int[] h) {
        Deque<Integer> st = new ArrayDeque<>();        // index, chiều cao tăng dần
        int best = 0, n = h.length;
        for (int i = 0; i <= n; i++) {
            int cur = (i == n) ? 0 : h[i];             // sentinel 0 để xả hết stack
            while (!st.isEmpty() && h[st.peek()] > cur) {
                int height = h[st.pop()];
                int left = st.isEmpty() ? -1 : st.peek(); // cột thấp hơn gần nhất bên trái
                best = Math.max(best, height * (i - left - 1));
            }
            st.push(i);
        }
        return best;
    }

    public static void main(String[] args) {
        System.out.println(Arrays.toString(dailyTemperatures(new int[]{73,74,75,71,69,72,76,73})));
        // [1, 1, 4, 2, 1, 1, 0, 0]
        System.out.println(largestRectangleArea(new int[]{2, 1, 5, 6, 2, 3})); // 10
    }
}
```

### 8.4 Monotonic deque (queue)
Bài "max của mọi cửa sổ độ dài k": giữ deque chứa index với giá trị **giảm dần**; đầu deque luôn là max. Bỏ đầu nếu đã ra khỏi cửa sổ; bỏ cuối nếu nhỏ hơn phần tử mới (chúng không bao giờ là max nữa).

```java
import java.util.*;

public class SlidingWindowMax {
    // LeetCode 239 — O(n)
    static int[] maxSlidingWindow(int[] nums, int k) {
        int n = nums.length;
        int[] res = new int[n - k + 1];
        Deque<Integer> dq = new ArrayDeque<>();
        for (int i = 0; i < n; i++) {
            if (!dq.isEmpty() && dq.peekFirst() <= i - k) dq.pollFirst();       // hết hạn
            while (!dq.isEmpty() && nums[dq.peekLast()] <= nums[i]) dq.pollLast();
            dq.offerLast(i);
            if (i >= k - 1) res[i - k + 1] = nums[dq.peekFirst()];
        }
        return res;
    }

    public static void main(String[] args) {
        System.out.println(Arrays.toString(maxSlidingWindow(new int[]{1,3,-1,-3,5,3,6,7}, 3)));
        // [3, 3, 5, 5, 6, 7]
    }
}
```

> 💡 **Góc nhìn Senior:** stack xuất hiện ở khắp nơi trong hệ thống thật: call stack của JVM (và vì sao đệ quy sâu gây `StackOverflowError` → chuyển sang stack tường minh), parser JSON/XML, undo/redo, trình duyệt back/forward. Monotonic deque là cách hiệu quả để tính "max latency trong 1 phút gần nhất" trên stream metrics với chi phí `O(1)` amortized mỗi điểm dữ liệu.

> ⚠️ **Lỗi thường gặp:**
> - Dùng `java.util.Stack`: vừa chậm (synchronized) vừa có iteration order ngược kỳ vọng so với `Deque`.
> - `ArrayDeque.push` thêm vào **đầu**; `offer/add` thêm vào **cuối** — trộn lẫn sẽ thành sai thứ tự.
> - Lưu **giá trị** thay vì **index** trong monotonic stack → không tính được khoảng cách/độ rộng.
> - Integer trong `Deque<Integer>` so sánh bằng `==` (ví dụ `st.peek() == x`) → sai khi > 127.

### 🛠 Bài tập phần 8

**Bài 8.1 — Implement Queue using Stacks (Cơ bản)** — LeetCode 232
- Đề bài: cài queue chỉ dùng hai stack, amortized `O(1)` cho mỗi thao tác.

**Bài 8.2 — Next Greater Element II (Trung bình)** — LeetCode 503
- Đề bài: mảng vòng tròn, tìm phần tử lớn hơn kế tiếp cho từng vị trí.

**Bài 8.3 — Car Fleet (Trung bình)** — LeetCode 853

**Bài 8.4 — Shortest Subarray with Sum at Least K (Nâng cao)** — LeetCode 862
- Đề bài: mảng có số âm, tìm độ dài subarray ngắn nhất có tổng ≥ K.

<details>
<summary>Gợi ý lời giải</summary>

- 8.1: stack `in` và `out`; `push` vào `in`; `pop/peek` nếu `out` rỗng thì đổ toàn bộ `in` sang `out`. Mỗi phần tử di chuyển tối đa 2 lần → amortized O(1).
- 8.2: duyệt `i` từ `0` đến `2n-1`, dùng `nums[i % n]`; monotonic stack giảm dần chứa index; chỉ push khi `i < n`. Khởi tạo `ans` bằng `-1`.
- 8.3: sort xe theo vị trí giảm dần, tính thời gian đến đích `(target - pos) / speed` (double). Duyệt: nếu thời gian > thời gian của fleet phía trước (đỉnh stack) → fleet mới. Đáp án = số fleet.
- 8.4: prefix sum `long P[]` + monotonic deque **tăng dần** theo `P`. Với mỗi `i`: while `P[i] - P[dq.first] >= K` → cập nhật đáp án, `pollFirst`; while `P[dq.last] >= P[i]` → `pollLast`; `offerLast(i)`. O(n). Sliding window thường sai vì có số âm.

</details>

---

## 9. Linked List

### 9.1 Khái niệm
Danh sách liên kết đơn: mỗi node giữ `val` và `next`. Chèn/xoá tại node đã biết `O(1)`, truy cập theo index `O(n)`. Kỹ thuật cốt lõi: **dummy head** (tránh if cho node đầu), **fast/slow pointers**, **đảo con trỏ**.

```java
public class LinkedListPatterns {
    static final class ListNode {
        int val; ListNode next;
        ListNode(int v) { val = v; }
        ListNode(int v, ListNode n) { val = v; next = n; }
    }

    // LeetCode 206: đảo ngược — iterative O(n)/O(1)
    static ListNode reverse(ListNode head) {
        ListNode prev = null, cur = head;
        while (cur != null) {
            ListNode next = cur.next;
            cur.next = prev;
            prev = cur;
            cur = next;
        }
        return prev;
    }

    // Đệ quy (O(n) stack) — để hiểu, không dùng cho list rất dài
    static ListNode reverseRec(ListNode head) {
        if (head == null || head.next == null) return head;
        ListNode newHead = reverseRec(head.next);
        head.next.next = head;
        head.next = null;
        return newHead;
    }

    // LeetCode 21: merge hai list đã sort với dummy head
    static ListNode merge(ListNode a, ListNode b) {
        ListNode dummy = new ListNode(0), tail = dummy;
        while (a != null && b != null) {
            if (a.val <= b.val) { tail.next = a; a = a.next; }
            else                { tail.next = b; b = b.next; }
            tail = tail.next;
        }
        tail.next = (a != null) ? a : b;
        return dummy.next;
    }

    // LeetCode 142: Floyd — tìm điểm bắt đầu chu trình
    static ListNode detectCycle(ListNode head) {
        ListNode slow = head, fast = head;
        while (fast != null && fast.next != null) {
            slow = slow.next;
            fast = fast.next.next;
            if (slow == fast) {                       // gặp nhau trong chu trình
                ListNode p = head;
                while (p != slow) { p = p.next; slow = slow.next; }
                return p;                             // điểm vào chu trình
            }
        }
        return null;
    }

    // LeetCode 876: middle (với số chẵn trả node giữa thứ hai)
    static ListNode middle(ListNode head) {
        ListNode slow = head, fast = head;
        while (fast != null && fast.next != null) { slow = slow.next; fast = fast.next.next; }
        return slow;
    }

    // LeetCode 19: xoá node thứ n từ cuối, một lượt
    static ListNode removeNthFromEnd(ListNode head, int n) {
        ListNode dummy = new ListNode(0, head), fast = dummy, slow = dummy;
        for (int i = 0; i <= n; i++) fast = fast.next;  // fast đi trước n+1 bước
        while (fast != null) { fast = fast.next; slow = slow.next; }
        slow.next = slow.next.next;
        return dummy.next;
    }

    static String show(ListNode h) {
        StringBuilder sb = new StringBuilder("[");
        for (; h != null; h = h.next) sb.append(h.val).append(h.next != null ? "," : "");
        return sb.append("]").toString();
    }

    public static void main(String[] args) {
        ListNode a = new ListNode(1, new ListNode(3, new ListNode(5)));
        ListNode b = new ListNode(2, new ListNode(4));
        ListNode m = merge(a, b);
        System.out.println(show(m));                         // [1,2,3,4,5]
        System.out.println(show(reverse(m)));                // [5,4,3,2,1]
    }
}
```

**Vì sao Floyd đúng?** Gọi `F` = khoảng cách từ head đến điểm vào chu trình, `C` = độ dài chu trình, `a` = khoảng cách từ điểm vào đến chỗ gặp. Khi gặp: `slow` đi `F + a`, `fast` đi `2(F + a)` = `F + a + kC` ⇒ `F + a = kC` ⇒ `F = kC − a`. Vậy đi thêm `F` bước từ chỗ gặp sẽ tới đúng điểm vào — trùng với con trỏ đi `F` bước từ head.

> 💡 **Góc nhìn Senior:** trong code Java thực tế hầu như **không** dùng `LinkedList` (benchmark thường thua `ArrayList`/`ArrayDeque` cả khi chèn giữa, vì phải duyệt tới vị trí). Tuy nhiên linked list là nền tảng của `LinkedHashMap` (LRU cache — phần 18), `ConcurrentLinkedQueue` (lock-free Michael-Scott queue), chuỗi bucket trong `HashMap`, và skip list trong `ConcurrentSkipListMap`. Floyd cycle detection cũng dùng cho bài "Find the Duplicate Number" (LeetCode 287) — coi mảng như một hàm `i → nums[i]`.

> ⚠️ **Lỗi thường gặp:**
> - Mất tham chiếu `next` trước khi đổi con trỏ.
> - Quên `fast.next != null` → NPE.
> - Không dùng dummy head → nhiều if lồng nhau cho trường hợp xoá node đầu.

### 🛠 Bài tập phần 9

**Bài 9.1 — Palindrome Linked List (Cơ bản)** — LeetCode 234
- Đề bài: `O(n)` time, `O(1)` space.

**Bài 9.2 — Reorder List (Trung bình)** — LeetCode 143
- Đề bài: `L0→L1→…→Ln` thành `L0→Ln→L1→Ln-1→…`, in-place.

**Bài 9.3 — Copy List with Random Pointer (Trung bình)** — LeetCode 138

**Bài 9.4 — Reverse Nodes in k-Group (Nâng cao)** — LeetCode 25

<details>
<summary>Gợi ý lời giải</summary>

- 9.1: tìm middle → đảo nửa sau → so sánh hai nửa → (lịch sự) đảo lại để khôi phục input.
- 9.2: ba bước tái sử dụng: `middle`, `reverse` nửa sau (cắt `mid.next = null` trước), rồi xen kẽ hai list.
- 9.3: `Map<Node,Node>` old→new, hai lượt (O(n) space); hoặc xen node copy ngay sau node gốc `A→A'→B→B'`, gán `A'.random = A.random.next`, rồi tách — O(1) space phụ.
- 9.4: dùng dummy; với mỗi nhóm, kiểm tra còn đủ k node không; đảo k node bằng vòng lặp chuẩn; nối `groupPrev.next` vào đầu mới và đuôi cũ vào nhóm kế tiếp:
  ```java
  ListNode dummy = new ListNode(0, head), groupPrev = dummy;
  while (true) {
      ListNode kth = groupPrev;
      for (int i = 0; i < k && kth != null; i++) kth = kth.next;
      if (kth == null) break;
      ListNode groupNext = kth.next, prev = groupNext, cur = groupPrev.next;
      while (cur != groupNext) { ListNode nx = cur.next; cur.next = prev; prev = cur; cur = nx; }
      ListNode oldFirst = groupPrev.next;
      groupPrev.next = kth;
      groupPrev = oldFirst;
  }
  return dummy.next;
  ```

</details>

---

## 10. Trees & Binary Search Tree

### 10.1 Khái niệm
- **Binary tree**: mỗi node ≤ 2 con. Chiều cao `h`: cây cân bằng `h = O(log n)`, cây lệch `h = O(n)`.
- **Duyệt**: DFS (pre-order: gốc-trái-phải; in-order: trái-gốc-phải; post-order: trái-phải-gốc) và BFS (level-order).
- **BST** (CLRS Ch.12): mọi node trái < gốc < mọi node phải. In-order của BST cho dãy **tăng dần**. Search/insert/delete `O(h)`.
- **Balanced BST** (CLRS Ch.13): AVL (cân bằng chặt, |chênh lệch chiều cao| ≤ 1) và **Red-Black tree** (cân bằng lỏng hơn, chiều cao ≤ `2·log₂(n+1)`, ít rotation khi insert/delete). Java: `TreeMap`/`TreeSet` và bucket đã treeify của `HashMap` đều là **red-black tree**.

```java
import java.util.*;

public class TreeTraversals {
    static final class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int v) { val = v; }
        TreeNode(int v, TreeNode l, TreeNode r) { val = v; left = l; right = r; }
    }

    // Đệ quy: chiều cao (LeetCode 104)
    static int maxDepth(TreeNode root) {
        return root == null ? 0 : 1 + Math.max(maxDepth(root.left), maxDepth(root.right));
    }

    // In-order iterative với stack tường minh (tránh StackOverflow với cây sâu)
    static List<Integer> inorder(TreeNode root) {
        List<Integer> res = new ArrayList<>();
        Deque<TreeNode> st = new ArrayDeque<>();
        TreeNode cur = root;
        while (cur != null || !st.isEmpty()) {
            while (cur != null) { st.push(cur); cur = cur.left; }
            cur = st.pop();
            res.add(cur.val);
            cur = cur.right;
        }
        return res;
    }

    // Pre-order iterative
    static List<Integer> preorder(TreeNode root) {
        List<Integer> res = new ArrayList<>();
        if (root == null) return res;
        Deque<TreeNode> st = new ArrayDeque<>();
        st.push(root);
        while (!st.isEmpty()) {
            TreeNode n = st.pop();
            res.add(n.val);
            if (n.right != null) st.push(n.right);   // push phải trước để trái ra trước
            if (n.left != null) st.push(n.left);
        }
        return res;
    }

    // Post-order iterative: pre-order biến thể (gốc-phải-trái) rồi đảo ngược
    static List<Integer> postorder(TreeNode root) {
        LinkedList<Integer> res = new LinkedList<>();
        if (root == null) return res;
        Deque<TreeNode> st = new ArrayDeque<>();
        st.push(root);
        while (!st.isEmpty()) {
            TreeNode n = st.pop();
            res.addFirst(n.val);
            if (n.left != null) st.push(n.left);
            if (n.right != null) st.push(n.right);
        }
        return res;
    }

    // Level-order BFS (LeetCode 102)
    static List<List<Integer>> levelOrder(TreeNode root) {
        List<List<Integer>> res = new ArrayList<>();
        if (root == null) return res;
        Queue<TreeNode> q = new ArrayDeque<>();
        q.offer(root);
        while (!q.isEmpty()) {
            int size = q.size();                      // chốt số node của level hiện tại
            List<Integer> level = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                TreeNode n = q.poll();
                level.add(n.val);
                if (n.left != null) q.offer(n.left);
                if (n.right != null) q.offer(n.right);
            }
            res.add(level);
        }
        return res;
    }

    public static void main(String[] args) {
        //        4
        //      2   6
        //     1 3 5 7
        TreeNode root = new TreeNode(4,
            new TreeNode(2, new TreeNode(1), new TreeNode(3)),
            new TreeNode(6, new TreeNode(5), new TreeNode(7)));
        System.out.println(inorder(root));    // [1, 2, 3, 4, 5, 6, 7]
        System.out.println(preorder(root));   // [4, 2, 1, 3, 6, 5, 7]
        System.out.println(postorder(root));  // [1, 3, 2, 5, 7, 6, 4]
        System.out.println(levelOrder(root)); // [[4], [2, 6], [1, 3, 5, 7]]
        System.out.println(maxDepth(root));   // 3
    }
}
```

### 10.2 BST operations, validate, LCA
```java
public class BstOps {
    static final class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int v) { val = v; }
    }

    static TreeNode insert(TreeNode root, int key) {
        if (root == null) return new TreeNode(key);
        if (key < root.val) root.left = insert(root.left, key);
        else if (key > root.val) root.right = insert(root.right, key);
        return root;                                  // bỏ qua key trùng
    }

    // LeetCode 450: Delete Node in a BST
    static TreeNode delete(TreeNode root, int key) {
        if (root == null) return null;
        if (key < root.val) root.left = delete(root.left, key);
        else if (key > root.val) root.right = delete(root.right, key);
        else {
            if (root.left == null) return root.right;  // 0 hoặc 1 con
            if (root.right == null) return root.left;
            TreeNode succ = root.right;                 // 2 con: lấy successor (min của cây phải)
            while (succ.left != null) succ = succ.left;
            root.val = succ.val;
            root.right = delete(root.right, succ.val);
        }
        return root;
    }

    // LeetCode 98: Validate BST — truyền khoảng (dùng long để chịu được Integer.MIN/MAX_VALUE)
    static boolean isValidBST(TreeNode n) { return valid(n, Long.MIN_VALUE, Long.MAX_VALUE); }
    private static boolean valid(TreeNode n, long lo, long hi) {
        if (n == null) return true;
        if (n.val <= lo || n.val >= hi) return false;
        return valid(n.left, lo, n.val) && valid(n.right, n.val, hi);
    }

    // LeetCode 235: LCA trong BST — O(h)
    static TreeNode lcaBST(TreeNode root, int p, int q) {
        TreeNode cur = root;
        while (cur != null) {
            if (p < cur.val && q < cur.val) cur = cur.left;
            else if (p > cur.val && q > cur.val) cur = cur.right;
            else return cur;                            // p, q tách nhánh tại đây
        }
        return null;
    }

    // LeetCode 236: LCA trong binary tree bất kỳ — O(n)
    static TreeNode lca(TreeNode root, TreeNode p, TreeNode q) {
        if (root == null || root == p || root == q) return root;
        TreeNode l = lca(root.left, p, q), r = lca(root.right, p, q);
        if (l != null && r != null) return root;        // p và q nằm hai bên
        return l != null ? l : r;
    }

    public static void main(String[] args) {
        TreeNode root = null;
        for (int x : new int[]{5, 3, 8, 2, 4, 7, 9}) root = insert(root, x);
        System.out.println(isValidBST(root));           // true
        System.out.println(lcaBST(root, 2, 4).val);     // 3
        root = delete(root, 5);
        System.out.println(root.val + " " + isValidBST(root)); // 7 true
    }
}
```

**Pattern "đệ quy trả thông tin lên" (post-order)**: rất nhiều bài cây (đường kính, max path sum, balanced check) đều là: hàm đệ quy trả về một giá trị cho cha (ví dụ chiều cao), đồng thời cập nhật một biến toàn cục (đáp án đi qua node hiện tại).

```java
// LeetCode 124: Binary Tree Maximum Path Sum
class MaxPathSum {
    private int best = Integer.MIN_VALUE;
    int maxPathSum(BstOps.TreeNode root) { gain(root); return best; }
    private int gain(BstOps.TreeNode n) {
        if (n == null) return 0;
        int l = Math.max(0, gain(n.left)), r = Math.max(0, gain(n.right)); // bỏ nhánh âm
        best = Math.max(best, n.val + l + r);  // đường đi "uốn" tại n
        return n.val + Math.max(l, r);         // cha chỉ nối được một nhánh
    }
}
```

> 💡 **Góc nhìn Senior:** biết **vì sao** DB index dùng **B+ tree** chứ không phải red-black tree: mỗi node B+ tree chứa hàng trăm key, khớp với một page đĩa (InnoDB 16KB) → chiều cao chỉ 3–4 cho hàng trăm triệu bản ghi, giảm số lần I/O. Red-black tree hợp cho bộ nhớ trong (`TreeMap`). Ngoài ra, `TreeMap` cho các thao tác "điều hướng" `floorKey`, `ceilingKey`, `headMap`, `subMap` — rất hữu ích trong bài toán lịch hẹn, consistent hashing (phần 18).

> ⚠️ **Lỗi thường gặp:**
> - Validate BST chỉ so sánh node với con trực tiếp → sai (cần khoảng giới hạn từ tổ tiên).
> - Dùng `Integer.MIN_VALUE` làm cận trong validate → sai khi node có giá trị đó; dùng `long` hoặc `Integer` nullable.
> - Đệ quy trên cây lệch 10⁵ node → `StackOverflowError`; dùng iterative.

### 🛠 Bài tập phần 10

**Bài 10.1 — Invert Binary Tree & Same Tree (Cơ bản)** — LeetCode 226, 100

**Bài 10.2 — Kth Smallest Element in a BST (Trung bình)** — LeetCode 230
- Follow-up: nếu cây bị insert/delete thường xuyên và hỏi kth nhiều lần thì sao?

**Bài 10.3 — Construct Binary Tree from Preorder and Inorder (Trung bình)** — LeetCode 105

**Bài 10.4 — Serialize and Deserialize Binary Tree (Nâng cao)** — LeetCode 297

<details>
<summary>Gợi ý lời giải</summary>

- 10.1: invert: swap `left/right` rồi đệ quy. Same tree: cả hai null → true; một null → false; so val và đệ quy hai bên.
- 10.2: in-order iterative, dừng ở phần tử thứ k → `O(h + k)`. Follow-up: augment mỗi node với `size` của cây con (order-statistic tree, CLRS 14.1) → `O(h)` mỗi truy vấn.
- 10.3: phần tử đầu preorder là gốc; tìm vị trí trong inorder bằng `HashMap<value,index>` (O(1)) để chia trái/phải; dùng con trỏ toàn cục `preIdx`. O(n).
- 10.4: pre-order với ký hiệu null `#`, phân tách bằng `,`. Deserialize: đọc token bằng `Iterator`/`Deque` rồi đệ quy cùng thứ tự. Hoặc BFS level-order như LeetCode hiển thị.

</details>

---

## 11. Heap & Top-K

### 11.1 Khái niệm
**Binary heap** (CLRS Ch.6) là cây nhị phân đầy đủ lưu trong mảng: con của `i` là `2i+1`, `2i+2`; cha là `(i-1)/2`. Min-heap: cha ≤ con. Thao tác: `peek O(1)`, `offer/poll O(log n)`, build từ mảng `O(n)`.

Java: `PriorityQueue<E>` là **min-heap** mặc định; max-heap dùng `new PriorityQueue<>(Comparator.reverseOrder())`.

| Thao tác `PriorityQueue` | Độ phức tạp |
|---|---|
| `offer`, `poll`, `add`, `remove()` | `O(log n)` |
| `peek`, `size` | `O(1)` |
| `remove(Object)`, `contains` | `O(n)` |
| `new PriorityQueue<>(collection)` | `O(n)` (heapify) |
| iterator / `toString` | **không** theo thứ tự ưu tiên |

### 11.2 Pattern Top-K
"K phần tử lớn nhất" → giữ **min-heap kích thước k**: phần tử mới lớn hơn đỉnh thì thay. `O(n log k)` time, `O(k)` space — tốt hơn sort `O(n log n)` khi `k ≪ n`, và chạy được trên **stream**.

```java
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TopK {
    // LeetCode 215: Kth Largest Element — heap O(n log k)
    static int kthLargest(int[] nums, int k) {
        PriorityQueue<Integer> heap = new PriorityQueue<>(k);
        for (int x : nums) {
            heap.offer(x);
            if (heap.size() > k) heap.poll();
        }
        return heap.peek();
    }

    // Quickselect: O(n) trung bình, O(n²) worst (giảm thiểu bằng pivot ngẫu nhiên)
    static int kthLargestQuickselect(int[] nums, int k) {
        int target = nums.length - k, lo = 0, hi = nums.length - 1;
        Random rnd = new Random();
        while (true) {
            int p = partition(nums, lo, hi, lo + rnd.nextInt(hi - lo + 1));
            if (p == target) return nums[p];
            if (p < target) lo = p + 1; else hi = p - 1;
        }
    }
    private static int partition(int[] a, int lo, int hi, int pivotIdx) {   // Lomuto
        int pivot = a[pivotIdx];
        swap(a, pivotIdx, hi);
        int store = lo;
        for (int i = lo; i < hi; i++) if (a[i] < pivot) swap(a, i, store++);
        swap(a, store, hi);
        return store;
    }
    private static void swap(int[] a, int i, int j) { int t = a[i]; a[i] = a[j]; a[j] = t; }

    // LeetCode 347: Top K Frequent Elements
    static List<Integer> topKFrequent(int[] nums, int k) {
        Map<Integer, Long> freq = Arrays.stream(nums).boxed()
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        PriorityQueue<Map.Entry<Integer, Long>> heap =
            new PriorityQueue<>(Map.Entry.comparingByValue());          // min-heap theo tần suất
        for (Map.Entry<Integer, Long> e : freq.entrySet()) {
            heap.offer(e);
            if (heap.size() > k) heap.poll();
        }
        List<Integer> res = new ArrayList<>();
        while (!heap.isEmpty()) res.add(heap.poll().getKey());
        Collections.reverse(res);                                        // tần suất giảm dần
        return res;
    }

    public static void main(String[] args) {
        int[] a = {3, 2, 1, 5, 6, 4};
        System.out.println(kthLargest(a, 2) + " " + kthLargestQuickselect(a.clone(), 2)); // 5 5
        System.out.println(topKFrequent(new int[]{1, 1, 1, 2, 2, 3}, 2));                 // [1, 2]
    }
}
```

### 11.3 Two heaps — median trên stream
```java
import java.util.*;

// LeetCode 295: Find Median from Data Stream — addNum O(log n), findMedian O(1)
class MedianFinder {
    private final PriorityQueue<Integer> low = new PriorityQueue<>(Comparator.reverseOrder()); // max-heap nửa nhỏ
    private final PriorityQueue<Integer> high = new PriorityQueue<>();                          // min-heap nửa lớn

    void addNum(int x) {
        low.offer(x);
        high.offer(low.poll());                 // đảm bảo mọi phần tử low <= mọi phần tử high
        if (high.size() > low.size()) low.offer(high.poll()); // low luôn bằng hoặc hơn 1 phần tử
    }

    double findMedian() {
        return low.size() > high.size() ? low.peek() : (low.peek() + (long) high.peek()) / 2.0;
    }
}
```

> 💡 **Góc nhìn Senior:** heap có mặt trong: `ScheduledThreadPoolExecutor` (`DelayedWorkQueue` là heap theo thời điểm chạy), `DelayQueue`, timer wheel thay thế khi số timer rất lớn (Netty `HashedWheelTimer`, Kafka purgatory), thuật toán Dijkstra, merge K file (phần 18). Với Top-K trên dữ liệu phân tán: mỗi node tính top-K cục bộ rồi merge — **chỉ đúng tuyệt đối** nếu dữ liệu được partition theo key (cùng key nằm cùng node); nếu không, cần đếm toàn cục trước hoặc dùng Count-Min Sketch + heap (xấp xỉ).

> ⚠️ **Lỗi thường gặp:**
> - Comparator `(a, b) -> b - a` cho max-heap → overflow. Dùng `Comparator.reverseOrder()` / `Integer.compare(b, a)`.
> - In `PriorityQueue` bằng `System.out.println(pq)` rồi tưởng là đã sort.
> - Sửa field của object đang nằm trong heap → heap hỏng invariant (phải remove rồi add lại).
> - Dùng max-heap kích thước n rồi poll k lần: `O(n + k log n)` — ổn, nhưng tốn bộ nhớ `O(n)` và không dùng được cho stream.

### 🛠 Bài tập phần 11

**Bài 11.1 — Last Stone Weight (Cơ bản)** — LeetCode 1046

**Bài 11.2 — K Closest Points to Origin (Trung bình)** — LeetCode 973
- Đề bài: trả k điểm gần gốc toạ độ nhất. So sánh heap và quickselect.

**Bài 11.3 — Task Scheduler (Trung bình)** — LeetCode 621

**Bài 11.4 — IPO (Nâng cao)** — LeetCode 502
- Đề bài: vốn ban đầu `w`, chọn tối đa `k` dự án (mỗi dự án cần `capital[i]`, lãi `profits[i]`) để tối đa vốn cuối.

<details>
<summary>Gợi ý lời giải</summary>

- 11.1: max-heap, mỗi lần poll 2 viên, nếu khác nhau offer hiệu. O(n log n).
- 11.2: max-heap kích thước k theo `x²+y²` (dùng `long` hoặc so sánh int nếu toạ độ ≤ 10⁴), bỏ đỉnh khi size > k. Quickselect O(n) trung bình nhưng sửa mảng input.
- 11.3: công thức: `maxCount` = tần suất lớn nhất, `numMax` = số task có tần suất đó → `max(tasks.length, (maxCount - 1) * (n + 1) + numMax)`. Hoặc mô phỏng bằng max-heap + queue cooldown.
- 11.4: sort dự án theo capital; min-heap "chưa đủ vốn" → đẩy dần những dự án `capital <= w` vào **max-heap theo profit**; mỗi vòng lấy profit lớn nhất. O(n log n). Đây là greedy + 2 heap.

</details>

---

## 12. Trie

### 12.1 Khái niệm
**Trie** (prefix tree): mỗi cạnh là một ký tự, đường đi từ gốc là một prefix. Insert/search `O(L)` với `L` là độ dài từ, **không phụ thuộc số từ**. Dùng cho autocomplete, spell check, routing table (longest prefix match), từ điển trong bài Word Search II.

```java
import java.util.*;

public class Trie {
    private static final class Node {
        final Node[] next = new Node[26];   // chỉ chữ thường; tổng quát dùng HashMap<Character, Node>
        boolean end;
        int passCount;                      // số từ đi qua node — hỗ trợ đếm theo prefix
    }

    private final Node root = new Node();

    public void insert(String word) {
        Node cur = root;
        for (char c : word.toCharArray()) {
            int i = c - 'a';
            if (cur.next[i] == null) cur.next[i] = new Node();
            cur = cur.next[i];
            cur.passCount++;
        }
        cur.end = true;
    }

    public boolean search(String word) { Node n = find(word); return n != null && n.end; }
    public boolean startsWith(String prefix) { return find(prefix) != null; }
    public int countPrefix(String prefix) { Node n = find(prefix); return n == null ? 0 : n.passCount; }

    /** Autocomplete: tối đa `limit` từ có prefix cho trước, theo thứ tự từ điển. */
    public List<String> suggest(String prefix, int limit) {
        List<String> out = new ArrayList<>();
        Node n = find(prefix);
        if (n != null) dfs(n, new StringBuilder(prefix), out, limit);
        return out;
    }
    private void dfs(Node n, StringBuilder sb, List<String> out, int limit) {
        if (out.size() >= limit) return;
        if (n.end) out.add(sb.toString());
        for (int i = 0; i < 26; i++) {
            if (n.next[i] == null) continue;
            sb.append((char) ('a' + i));
            dfs(n.next[i], sb, out, limit);
            sb.deleteCharAt(sb.length() - 1);           // backtrack
        }
    }

    private Node find(String s) {
        Node cur = root;
        for (char c : s.toCharArray()) {
            cur = cur.next[c - 'a'];
            if (cur == null) return null;
        }
        return cur;
    }

    public static void main(String[] args) {
        Trie t = new Trie();
        for (String w : List.of("apple", "app", "apply", "apt", "bat")) t.insert(w);
        System.out.println(t.search("app") + " " + t.search("ap") + " " + t.startsWith("ap")); // true false true
        System.out.println(t.countPrefix("app"));      // 3
        System.out.println(t.suggest("ap", 3));        // [app, apple, apply]
    }
}
```

> 💡 **Góc nhìn Senior:** mảng `Node[26]` nhanh nhưng tốn bộ nhớ (mỗi node ~ 26 tham chiếu × 4–8 byte). Với bộ ký tự lớn (Unicode, tiếng Việt có dấu), dùng `HashMap` hoặc **radix tree / compressed trie** (gộp chuỗi node một con). Trên production, autocomplete thường dùng Elasticsearch completion suggester (FST — finite state transducer) hoặc trie đã tính sẵn **top-k suggestion ở mỗi node** để trả lời `O(L)`; cập nhật offline theo batch.

> ⚠️ **Lỗi thường gặp:**
> - Nhầm `search` (phải là từ hoàn chỉnh, kiểm tra `end`) với `startsWith`.
> - Quên backtrack `StringBuilder` khi DFS.
> - Word Search II: không xoá từ đã tìm thấy khỏi trie → kết quả trùng và chậm.

### 🛠 Bài tập phần 12

**Bài 12.1 — Implement Trie (Cơ bản)** — LeetCode 208

**Bài 12.2 — Design Add and Search Words (Trung bình)** — LeetCode 211
- Đề bài: `search` hỗ trợ ký tự đại diện `.` khớp với mọi chữ cái.

**Bài 12.3 — Word Search II (Nâng cao)** — LeetCode 212
- Đề bài: cho lưới ký tự `m×n` và danh sách từ, tìm tất cả từ có thể tạo bằng đường đi kề nhau (không dùng lại ô).

<details>
<summary>Gợi ý lời giải</summary>

- 12.1: như code trên.
- 12.2: `search(word, i, node)` đệ quy: gặp `.` thì thử mọi con khác null. Worst case `O(26^L)` nhưng thực tế nhỏ.
- 12.3: build trie từ danh sách từ, lưu `String word` tại node kết thúc. DFS từ mỗi ô, đi song song trên trie; tìm thấy thì add vào kết quả và đặt `node.word = null` (tránh trùng). Đánh dấu ô đang thăm bằng cách gán tạm `board[r][c] = '#'`. Tối ưu thêm: cắt tỉa node lá đã dùng hết.

</details>

---

## 13. Graph

### 13.1 Biểu diễn
| Cách | Space | Kiểm tra cạnh (u,v) | Duyệt kề của u | Khi nào dùng |
|---|---|---|---|---|
| Adjacency list `List<List<Integer>>` | `O(V + E)` | `O(deg u)` | `O(deg u)` | Đồ thị thưa (hầu hết bài) |
| Adjacency matrix `boolean[V][V]` | `O(V²)` | `O(1)` | `O(V)` | Đồ thị dày, V nhỏ (≤ ~2000) |
| Edge list `int[][] edges` | `O(E)` | `O(E)` | — | Kruskal, Bellman-Ford |
| Grid `char[][]` | implicit | — | 4/8 hướng | Bài ma trận đảo, mê cung |

### 13.2 BFS & DFS (CLRS Ch.22)
- **BFS** dùng queue, thăm theo lớp → **đường đi ngắn nhất trên đồ thị không trọng số**. `O(V + E)`.
- **DFS** dùng đệ quy/stack → liên thông, chu trình, topological sort, cầu/khớp.

```java
import java.util.*;

public class GraphBasics {
    private static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    // LeetCode 200: Number of Islands — DFS trên grid
    static int numIslands(char[][] g) {
        int count = 0;
        for (int r = 0; r < g.length; r++)
            for (int c = 0; c < g[0].length; c++)
                if (g[r][c] == '1') { count++; sink(g, r, c); }
        return count;
    }
    private static void sink(char[][] g, int r, int c) {
        if (r < 0 || c < 0 || r >= g.length || c >= g[0].length || g[r][c] != '1') return;
        g[r][c] = '0';                                    // đánh dấu đã thăm
        for (int[] d : DIRS) sink(g, r + d[0], c + d[1]);
    }

    // Đường đi ngắn nhất trong mê cung (0 = trống, 1 = tường) — BFS
    static int shortestPath(int[][] grid, int sr, int sc, int tr, int tc) {
        int R = grid.length, C = grid[0].length;
        int[][] dist = new int[R][C];
        for (int[] row : dist) Arrays.fill(row, -1);
        Deque<int[]> q = new ArrayDeque<>();
        q.offer(new int[]{sr, sc});
        dist[sr][sc] = 0;                                 // đánh dấu KHI ĐẨY vào queue
        while (!q.isEmpty()) {
            int[] cur = q.poll();
            if (cur[0] == tr && cur[1] == tc) return dist[tr][tc];
            for (int[] d : DIRS) {
                int nr = cur[0] + d[0], nc = cur[1] + d[1];
                if (nr < 0 || nc < 0 || nr >= R || nc >= C || grid[nr][nc] == 1 || dist[nr][nc] != -1) continue;
                dist[nr][nc] = dist[cur[0]][cur[1]] + 1;
                q.offer(new int[]{nr, nc});
            }
        }
        return -1;
    }

    public static void main(String[] args) {
        char[][] g = {"11000".toCharArray(), "11000".toCharArray(), "00100".toCharArray(), "00011".toCharArray()};
        System.out.println(numIslands(g));  // 3
        int[][] maze = {{0, 0, 0}, {1, 1, 0}, {0, 0, 0}};
        System.out.println(shortestPath(maze, 0, 0, 2, 0)); // 6
    }
}
```

### 13.3 Topological sort & phát hiện chu trình
Trên DAG, topological order là thứ tự mà mọi cạnh `u → v` có `u` đứng trước `v`. Hai cách:
1. **Kahn (BFS theo in-degree)**: đẩy các đỉnh in-degree 0, lấy ra thì giảm in-degree hàng xóm. Nếu số đỉnh lấy ra < V → **có chu trình**.
2. **DFS 3 màu**: WHITE (chưa thăm), GRAY (đang trên stack đệ quy), BLACK (xong). Gặp cạnh tới đỉnh GRAY → **back edge → chu trình** (đồ thị có hướng). Thứ tự topo = đảo ngược thứ tự hoàn thành (post-order).

Đồ thị **vô hướng**: có chu trình khi DFS gặp đỉnh đã thăm mà **không phải cha**, hoặc dùng union-find (cạnh nối hai đỉnh đã cùng tập).

```java
import java.util.*;

public class TopoSort {
    // LeetCode 210: Course Schedule II — Kahn
    static int[] findOrder(int n, int[][] prereq) {
        List<List<Integer>> adj = new ArrayList<>();
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
        int[] indeg = new int[n];
        for (int[] p : prereq) { adj.get(p[1]).add(p[0]); indeg[p[0]]++; } // p[1] → p[0]
        Deque<Integer> q = new ArrayDeque<>();
        for (int i = 0; i < n; i++) if (indeg[i] == 0) q.offer(i);
        int[] order = new int[n];
        int idx = 0;
        while (!q.isEmpty()) {
            int u = q.poll();
            order[idx++] = u;
            for (int v : adj.get(u)) if (--indeg[v] == 0) q.offer(v);
        }
        return idx == n ? order : new int[0];        // còn đỉnh chưa lấy → có chu trình
    }

    // DFS 3 màu: phát hiện chu trình trên đồ thị có hướng
    static boolean hasCycle(int n, List<List<Integer>> adj) {
        int[] color = new int[n];                    // 0 white, 1 gray, 2 black
        for (int i = 0; i < n; i++) if (color[i] == 0 && dfs(i, adj, color)) return true;
        return false;
    }
    private static boolean dfs(int u, List<List<Integer>> adj, int[] color) {
        color[u] = 1;
        for (int v : adj.get(u)) {
            if (color[v] == 1) return true;          // back edge
            if (color[v] == 0 && dfs(v, adj, color)) return true;
        }
        color[u] = 2;
        return false;
    }

    public static void main(String[] args) {
        System.out.println(Arrays.toString(findOrder(4, new int[][]{{1,0},{2,0},{3,1},{3,2}}))); // [0, 1, 2, 3]
        System.out.println(findOrder(2, new int[][]{{1,0},{0,1}}).length);                        // 0
    }
}
```

### 13.4 Union-Find (Disjoint Set Union, CLRS Ch.21)
Hai tối ưu: **path compression** + **union by rank/size** → mỗi thao tác `O(α(n))` amortized (α là hàm Ackermann ngược, ≤ 4 với mọi n thực tế).

```java
public class UnionFind {
    private final int[] parent, size;
    private int components;

    public UnionFind(int n) {
        parent = new int[n]; size = new int[n]; components = n;
        for (int i = 0; i < n; i++) { parent[i] = i; size[i] = 1; }
    }

    public int find(int x) {
        while (parent[x] != x) {
            parent[x] = parent[parent[x]];   // path halving (một dạng path compression, không đệ quy)
            x = parent[x];
        }
        return x;
    }

    /** @return false nếu x và y đã cùng tập (cạnh này tạo chu trình). */
    public boolean union(int x, int y) {
        int a = find(x), b = find(y);
        if (a == b) return false;
        if (size[a] < size[b]) { int t = a; a = b; b = t; }  // gắn cây nhỏ vào cây lớn
        parent[b] = a;
        size[a] += size[b];
        components--;
        return true;
    }

    public int components() { return components; }

    public static void main(String[] args) {
        // LeetCode 684: Redundant Connection — cạnh đầu tiên tạo chu trình
        int[][] edges = {{1, 2}, {1, 3}, {2, 3}};
        UnionFind uf = new UnionFind(4);
        for (int[] e : edges) if (!uf.union(e[0], e[1])) System.out.println(e[0] + "-" + e[1]); // 2-3
    }
}
```

### 13.5 Dijkstra (CLRS Ch.24)
Đường đi ngắn nhất một nguồn, **trọng số không âm**. Dùng `PriorityQueue` với "lazy deletion" (bỏ qua phần tử cũ khi `d > dist[u]`). `O((V + E) log V)`.

```java
import java.util.*;

public class Dijkstra {
    record Edge(int to, int w) {}

    static long[] shortest(List<List<Edge>> adj, int src) {
        long[] dist = new long[adj.size()];
        Arrays.fill(dist, Long.MAX_VALUE);
        dist[src] = 0;
        PriorityQueue<long[]> pq = new PriorityQueue<>(Comparator.comparingLong(a -> a[1])); // {node, dist}
        pq.offer(new long[]{src, 0});
        while (!pq.isEmpty()) {
            long[] cur = pq.poll();
            int u = (int) cur[0];
            if (cur[1] > dist[u]) continue;                      // bản ghi cũ, bỏ qua
            for (Edge e : adj.get(u)) {
                long nd = dist[u] + e.w();
                if (nd < dist[e.to()]) {
                    dist[e.to()] = nd;
                    pq.offer(new long[]{e.to(), nd});
                }
            }
        }
        return dist;
    }

    public static void main(String[] args) {
        // LeetCode 743 Network Delay Time: times = [[2,1,1],[2,3,1],[3,4,1]], n = 4, k = 2 → 2
        int n = 4;
        List<List<Edge>> adj = new ArrayList<>();
        for (int i = 0; i <= n; i++) adj.add(new ArrayList<>());
        int[][] times = {{2, 1, 1}, {2, 3, 1}, {3, 4, 1}};
        for (int[] t : times) adj.get(t[0]).add(new Edge(t[1], t[2]));
        long[] d = shortest(adj, 2);
        long ans = 0;
        for (int i = 1; i <= n; i++) ans = Math.max(ans, d[i]);
        System.out.println(ans == Long.MAX_VALUE ? -1 : ans);   // 2
    }
}
```

**Chọn thuật toán đường đi ngắn nhất:**

| Trường hợp | Thuật toán | Độ phức tạp |
|---|---|---|
| Không trọng số | BFS | `O(V + E)` |
| Trọng số 0/1 | 0-1 BFS (deque) | `O(V + E)` |
| Không âm | Dijkstra | `O((V + E) log V)` |
| Có cạnh âm, phát hiện chu trình âm | Bellman-Ford | `O(V·E)` |
| Mọi cặp, V nhỏ | Floyd–Warshall | `O(V³)` |
| Giới hạn số cạnh `k` | Bellman-Ford k vòng (LeetCode 787) | `O(k·E)` |

Cây khung nhỏ nhất (MST): **Kruskal** (sort cạnh + union-find, `O(E log E)`) hoặc **Prim** (heap).

> 💡 **Góc nhìn Senior:** đồ thị là mô hình của rất nhiều vấn đề hệ thống: dependency resolution của Maven/Gradle và thứ tự khởi tạo bean của Spring (topological sort, và lỗi "circular reference" chính là phát hiện chu trình), deadlock detection (chu trình trong wait-for graph), service mesh routing, social graph ("bạn của bạn" = BFS 2 lớp). Hiểu khi nào cần **graph database** (Neo4j) thay vì JOIN đệ quy trong SQL cũng là câu hỏi system design hay gặp.

> ⚠️ **Lỗi thường gặp:**
> - BFS đánh dấu visited khi **lấy ra** thay vì khi **đẩy vào** → một đỉnh bị đẩy nhiều lần, có thể bùng nổ bộ nhớ.
> - Dijkstra với trọng số âm → sai.
> - Quên đồ thị có thể **không liên thông** → phải loop qua mọi đỉnh làm điểm bắt đầu.
> - DFS đệ quy trên grid 1000×1000 → `StackOverflowError`; chuyển sang BFS/stack tường minh.

### 🛠 Bài tập phần 13

**Bài 13.1 — Clone Graph (Cơ bản)** — LeetCode 133

**Bài 13.2 — Rotting Oranges (Trung bình)** — LeetCode 994
- Đề bài: multi-source BFS, trả số phút tối thiểu để mọi cam đều hỏng, hoặc -1.

**Bài 13.3 — Number of Connected Components / Accounts Merge (Trung bình)** — LeetCode 323, 721

**Bài 13.4 — Cheapest Flights Within K Stops (Nâng cao)** — LeetCode 787

**Bài 13.5 — Alien Dictionary (Nâng cao)** — LeetCode 269
- Đề bài: cho danh sách từ đã sort theo bảng chữ cái lạ, suy ra thứ tự các chữ cái (hoặc `""` nếu mâu thuẫn).

<details>
<summary>Gợi ý lời giải</summary>

- 13.1: DFS/BFS với `Map<Node,Node>` old→clone; tạo clone **trước** khi duyệt hàng xóm để xử lý chu trình.
- 13.2: đẩy **tất cả** cam hỏng vào queue cùng lúc, BFS theo level, đếm số cam tươi còn lại; mỗi level = 1 phút.
- 13.3: union-find; Accounts Merge: union các email cùng tài khoản (map email → id), rồi gom nhóm theo `find(root)`, sort email trong nhóm.
- 13.4: Bellman-Ford `k+1` vòng, mỗi vòng copy `dist` sang `tmp` để mỗi vòng chỉ thêm 1 cạnh. `O(k·E)`. (Dijkstra với state `(node, stops)` cũng được nhưng cần cẩn thận pruning.)
- 13.5: với mỗi cặp từ liền kề, ký tự khác nhau đầu tiên cho cạnh `a → b`; nếu từ trước dài hơn và từ sau là prefix của nó → mâu thuẫn, trả `""`. Topological sort (Kahn); có chu trình → `""`.

</details>

---

## 14. Backtracking

### 14.1 Khái niệm
Backtracking = DFS trên **cây trạng thái**: ở mỗi bước, *chọn* một lựa chọn → *đệ quy* → *huỷ chọn*. Cắt tỉa (pruning) nhánh không thể dẫn tới lời giải. Độ phức tạp thường là hàm mũ: subsets `O(n·2ⁿ)`, permutations `O(n·n!)`.

Template:
```
void backtrack(state, choices):
    if (state là lời giải) { ghi nhận BẢN SAO của state; return; }
    for (choice in choices):
        if (!hợp lệ(choice)) continue;     // pruning
        chọn(choice)
        backtrack(state', choices')
        huỷ chọn(choice)
```

```java
import java.util.*;

public class Backtracking {
    // LeetCode 78: Subsets
    static List<List<Integer>> subsets(int[] nums) {
        List<List<Integer>> res = new ArrayList<>();
        dfsSubsets(nums, 0, new ArrayList<>(), res);
        return res;
    }
    private static void dfsSubsets(int[] nums, int start, List<Integer> cur, List<List<Integer>> res) {
        res.add(new ArrayList<>(cur));                    // mọi node đều là một subset
        for (int i = start; i < nums.length; i++) {
            cur.add(nums[i]);
            dfsSubsets(nums, i + 1, cur, res);
            cur.remove(cur.size() - 1);
        }
    }

    // LeetCode 46: Permutations
    static List<List<Integer>> permute(int[] nums) {
        List<List<Integer>> res = new ArrayList<>();
        dfsPerm(nums, new boolean[nums.length], new ArrayList<>(), res);
        return res;
    }
    private static void dfsPerm(int[] nums, boolean[] used, List<Integer> cur, List<List<Integer>> res) {
        if (cur.size() == nums.length) { res.add(new ArrayList<>(cur)); return; }
        for (int i = 0; i < nums.length; i++) {
            if (used[i]) continue;
            used[i] = true; cur.add(nums[i]);
            dfsPerm(nums, used, cur, res);
            used[i] = false; cur.remove(cur.size() - 1);
        }
    }

    // LeetCode 40: Combination Sum II — có trùng, mỗi số dùng một lần
    static List<List<Integer>> combinationSum2(int[] cands, int target) {
        Arrays.sort(cands);
        List<List<Integer>> res = new ArrayList<>();
        dfsComb(cands, target, 0, new ArrayList<>(), res);
        return res;
    }
    private static void dfsComb(int[] a, int remain, int start, List<Integer> cur, List<List<Integer>> res) {
        if (remain == 0) { res.add(new ArrayList<>(cur)); return; }
        for (int i = start; i < a.length; i++) {
            if (i > start && a[i] == a[i - 1]) continue;  // bỏ trùng ở CÙNG một tầng
            if (a[i] > remain) break;                     // pruning nhờ đã sort
            cur.add(a[i]);
            dfsComb(a, remain - a[i], i + 1, cur, res);
            cur.remove(cur.size() - 1);
        }
    }

    // LeetCode 51: N-Queens — đếm số lời giải, dùng mảng đánh dấu cột & 2 đường chéo
    static int totalNQueens(int n) { return place(0, n, new boolean[n], new boolean[2 * n], new boolean[2 * n]); }
    private static int place(int row, int n, boolean[] col, boolean[] d1, boolean[] d2) {
        if (row == n) return 1;
        int count = 0;
        for (int c = 0; c < n; c++) {
            if (col[c] || d1[row + c] || d2[row - c + n]) continue;
            col[c] = d1[row + c] = d2[row - c + n] = true;
            count += place(row + 1, n, col, d1, d2);
            col[c] = d1[row + c] = d2[row - c + n] = false;
        }
        return count;
    }

    public static void main(String[] args) {
        System.out.println(subsets(new int[]{1, 2, 3}));            // 8 subset
        System.out.println(permute(new int[]{1, 2, 3}).size());     // 6
        System.out.println(combinationSum2(new int[]{10,1,2,7,6,1,5}, 8)); // [[1,1,6],[1,2,5],[1,7],[2,6]]
        System.out.println(totalNQueens(8));                         // 92
    }
}
```

> 💡 **Góc nhìn Senior:** backtracking là nền của constraint solver, sinh test case tổ hợp (pairwise testing), regex engine backtracking (và lỗi **ReDoS** — regex như `(a+)+$` bị nổ hàm mũ trên input xấu; `java.util.regex` là engine backtracking). Khi `n` lớn, hãy nói tới **memoization** (chuyển sang DP) hoặc heuristic/pruning mạnh hơn.

> ⚠️ **Lỗi thường gặp:**
> - `res.add(cur)` thay vì `res.add(new ArrayList<>(cur))` → mọi phần tử của `res` cùng trỏ tới một list (cuối cùng rỗng).
> - Bỏ trùng sai chỗ: `i > 0` thay vì `i > start` → mất lời giải hợp lệ như `[1,1,6]`.
> - Quên huỷ chọn (undo) trạng thái.

### 🛠 Bài tập phần 14

**Bài 14.1 — Letter Combinations of a Phone Number (Cơ bản)** — LeetCode 17

**Bài 14.2 — Word Search (Trung bình)** — LeetCode 79

**Bài 14.3 — Palindrome Partitioning (Trung bình)** — LeetCode 131

**Bài 14.4 — Sudoku Solver (Nâng cao)** — LeetCode 37

<details>
<summary>Gợi ý lời giải</summary>

- 14.1: mảng `String[] map = {"", "", "abc", "def", ...}`; DFS theo vị trí digit, `StringBuilder` append/deleteCharAt. `O(4ⁿ·n)`.
- 14.2: DFS từ mỗi ô khớp ký tự đầu; đánh dấu tạm `board[r][c] = '#'` rồi khôi phục. Pruning: đếm tần suất ký tự trong board trước, nếu không đủ → false ngay; đảo ngược từ nếu ký tự cuối hiếm hơn ký tự đầu.
- 14.3: DFS theo vị trí bắt đầu, thử mọi `end` mà `s[start..end]` là palindrome. Tối ưu: tiền tính `boolean[][] isPal` bằng DP `O(n²)`.
- 14.4: ba mảng `boolean rows[9][10], cols[9][10], boxes[9][10]`; tìm ô trống tiếp theo, thử 1–9 hợp lệ, đệ quy, undo. Heuristic: chọn ô có ít ứng viên nhất trước (MRV) → nhanh hơn nhiều.

</details>

---

## 15. Greedy & Intervals

### 15.1 Greedy
Greedy chọn lựa chọn **tốt nhất cục bộ** ở mỗi bước và không quay lại. Đúng khi bài toán có (CLRS Ch.16): **greedy-choice property** (tồn tại lời giải tối ưu chứa lựa chọn tham lam) và **optimal substructure**. Cách chứng minh hay dùng trong phỏng vấn: **exchange argument** — lấy một lời giải tối ưu bất kỳ, đổi một bước của nó thành lựa chọn tham lam mà không làm tệ hơn.

```java
public class Greedy {
    // LeetCode 55: Jump Game — giữ vị trí xa nhất có thể tới
    static boolean canJump(int[] nums) {
        int reach = 0;
        for (int i = 0; i < nums.length; i++) {
            if (i > reach) return false;
            reach = Math.max(reach, i + nums[i]);
        }
        return true;
    }

    // LeetCode 45: Jump Game II — BFS theo "tầng" ngầm
    static int jump(int[] nums) {
        int jumps = 0, curEnd = 0, farthest = 0;
        for (int i = 0; i < nums.length - 1; i++) {
            farthest = Math.max(farthest, i + nums[i]);
            if (i == curEnd) { jumps++; curEnd = farthest; }
        }
        return jumps;
    }

    // LeetCode 134: Gas Station
    static int canCompleteCircuit(int[] gas, int[] cost) {
        int total = 0, tank = 0, start = 0;
        for (int i = 0; i < gas.length; i++) {
            total += gas[i] - cost[i];
            tank += gas[i] - cost[i];
            if (tank < 0) { start = i + 1; tank = 0; }   // không trạm nào trong [start..i] làm điểm xuất phát được
        }
        return total >= 0 ? start : -1;
    }

    // Kadane — LeetCode 53 Maximum Subarray (greedy/DP)
    static int maxSubArray(int[] a) {
        int best = a[0], cur = a[0];
        for (int i = 1; i < a.length; i++) {
            cur = Math.max(a[i], cur + a[i]);            // bắt đầu lại hay nối tiếp?
            best = Math.max(best, cur);
        }
        return best;
    }

    public static void main(String[] args) {
        System.out.println(canJump(new int[]{2, 3, 1, 1, 4}) + " " + jump(new int[]{2, 3, 1, 1, 4})); // true 2
        System.out.println(canCompleteCircuit(new int[]{1,2,3,4,5}, new int[]{3,4,5,1,2}));         // 3
        System.out.println(maxSubArray(new int[]{-2,1,-3,4,-1,2,1,-5,4}));                         // 6
    }
}
```

### 15.2 Intervals
Ba câu hỏi chính và cách sort tương ứng:
- **Merge** các khoảng chồng nhau → sort theo `start`.
- **Xoá ít nhất để không chồng** / **chọn nhiều nhất không chồng** (activity selection) → sort theo `end`, luôn giữ khoảng kết thúc sớm nhất.
- **Số phòng họp tối thiểu** (max số khoảng chồng tại một thời điểm) → min-heap theo `end`, hoặc **sweep line** (tách start/end thành sự kiện).

```java
import java.util.*;

public class Intervals {
    // LeetCode 56: Merge Intervals
    static int[][] merge(int[][] in) {
        Arrays.sort(in, Comparator.comparingInt(a -> a[0]));
        List<int[]> out = new ArrayList<>();
        for (int[] cur : in) {
            if (out.isEmpty() || out.get(out.size() - 1)[1] < cur[0]) out.add(cur.clone());
            else out.get(out.size() - 1)[1] = Math.max(out.get(out.size() - 1)[1], cur[1]);
        }
        return out.toArray(new int[0][]);
    }

    // LeetCode 435: Non-overlapping Intervals — số khoảng ít nhất phải xoá
    static int eraseOverlapIntervals(int[][] in) {
        Arrays.sort(in, Comparator.comparingInt(a -> a[1]));
        int kept = 0;
        long lastEnd = Long.MIN_VALUE;
        for (int[] cur : in) {
            if (cur[0] >= lastEnd) { kept++; lastEnd = cur[1]; }
        }
        return in.length - kept;
    }

    // LeetCode 253: Meeting Rooms II — sweep line
    static int minMeetingRooms(int[][] in) {
        int n = in.length;
        int[] starts = new int[n], ends = new int[n];
        for (int i = 0; i < n; i++) { starts[i] = in[i][0]; ends[i] = in[i][1]; }
        Arrays.sort(starts); Arrays.sort(ends);
        int rooms = 0, best = 0;
        for (int i = 0, j = 0; i < n; i++) {
            while (j < i && ends[j] <= starts[i]) { j++; rooms--; } // phòng đã trống ([s, e) nửa mở)
            rooms++;
            best = Math.max(best, rooms);
        }
        return best;
    }

    public static void main(String[] args) {
        System.out.println(Arrays.deepToString(merge(new int[][]{{1,3},{8,10},{2,6},{15,18}}))); // [[1,6],[8,10],[15,18]]
        System.out.println(eraseOverlapIntervals(new int[][]{{1,2},{2,3},{3,4},{1,3}}));         // 1
        System.out.println(minMeetingRooms(new int[][]{{0,30},{5,10},{15,20}}));                 // 2
    }
}
```

> 💡 **Góc nhìn Senior:** intervals xuất hiện liên tục trong nghiệp vụ: đặt phòng/lịch hẹn, khoảng giá hiệu lực của sản phẩm, gộp time range trong log, bảo trì hệ thống. Trong DB, kiểm tra chồng lấn hai khoảng `[a1, a2)` và `[b1, b2)` là `a1 < b2 AND b1 < a2` — câu này nên thuộc. PostgreSQL có `tstzrange` + exclusion constraint để chặn đặt trùng ở tầng DB. Greedy cũng phổ biến trong scheduler (Kubernetes scoring, bin packing xấp xỉ).

> ⚠️ **Lỗi thường gặp:**
> - Không làm rõ khoảng **đóng hay mở**: `[1,2]` và `[2,3]` có chồng không?
> - Sort intervals bằng `(a, b) -> a[0] - b[0]` → overflow với giá trị lớn/âm.
> - Áp greedy khi chưa chứng minh được (ví dụ coin change với mệnh giá tuỳ ý — greedy sai, phải DP).

### 🛠 Bài tập phần 15

**Bài 15.1 — Insert Interval (Cơ bản)** — LeetCode 57

**Bài 15.2 — Partition Labels (Trung bình)** — LeetCode 763

**Bài 15.3 — Minimum Number of Arrows to Burst Balloons (Trung bình)** — LeetCode 452

**Bài 15.4 — Employee Free Time / Candy (Nâng cao)** — LeetCode 759, 135

<details>
<summary>Gợi ý lời giải</summary>

- 15.1: ba vòng: thêm các khoảng kết thúc trước `newStart`; gộp mọi khoảng chồng với khoảng mới (`start <= newEnd`); thêm phần còn lại. O(n).
- 15.2: lưu `last[c]` = vị trí xuất hiện cuối; duyệt, `end = max(end, last[c])`; khi `i == end` thì cắt một phần.
- 15.3: sort theo `end` (dùng `Integer.compare`, giá trị có thể là ±2³¹), bắn tại `end` của bóng đầu; bóng nào có `start > arrow` thì cần mũi tên mới.
- 15.4: Employee Free Time: gộp mọi khoảng của mọi nhân viên (sort hoặc heap K-way), khe hở giữa các khoảng đã merge là free time. Candy: hai lượt trái→phải và phải→trái, `candy[i] = max(left[i], right[i])`.

</details>

---

## 16. Dynamic Programming

### 16.1 Khái niệm
DP áp dụng khi bài có (CLRS Ch.15): **optimal substructure** và **overlapping subproblems**. Quy trình 4 bước:
1. **Định nghĩa state**: `dp[i]` / `dp[i][j]` nghĩa là gì (viết thành câu!).
2. **Transition** (công thức truy hồi).
3. **Base case**.
4. **Thứ tự tính** & vị trí đáp án; sau đó **tối ưu space** (rolling array).

| | Memoization (top-down) | Tabulation (bottom-up) |
|---|---|---|
| Cách viết | Đệ quy + cache (`Map`/mảng) | Vòng lặp điền bảng |
| Ưu | Dễ viết từ recurrence, chỉ tính state cần thiết | Không tốn stack, dễ tối ưu space, nhanh hơn (không overhead gọi hàm) |
| Nhược | Stack overflow với n lớn, overhead | Phải xác định thứ tự tính, tính cả state thừa |

```java
import java.util.*;

public class DpBasics {
    // Fibonacci / Climbing Stairs (LeetCode 70) — 3 phiên bản
    static long fibMemo(int n, long[] memo) {
        if (n <= 1) return n;
        if (memo[n] != 0) return memo[n];
        return memo[n] = fibMemo(n - 1, memo) + fibMemo(n - 2, memo);
    }
    static long fibTab(int n) {
        if (n <= 1) return n;
        long[] dp = new long[n + 1];
        dp[1] = 1;
        for (int i = 2; i <= n; i++) dp[i] = dp[i - 1] + dp[i - 2];
        return dp[n];
    }
    static long fibO1(int n) {                    // rolling: chỉ giữ 2 giá trị gần nhất
        long a = 0, b = 1;
        for (int i = 0; i < n; i++) { long t = a + b; a = b; b = t; }
        return a;
    }

    // LeetCode 198: House Robber — dp[i] = max(dp[i-1], dp[i-2] + nums[i])
    static int rob(int[] nums) {
        int prev2 = 0, prev1 = 0;
        for (int x : nums) { int cur = Math.max(prev1, prev2 + x); prev2 = prev1; prev1 = cur; }
        return prev1;
    }

    public static void main(String[] args) {
        System.out.println(fibMemo(50, new long[51]) + " " + fibTab(50) + " " + fibO1(50)); // 12586269025 x3
        System.out.println(rob(new int[]{2, 7, 9, 3, 1}));                                   // 12
    }
}
```

### 16.2 Các bài DP kinh điển
```java
import java.util.*;

public class ClassicDp {
    // 0/1 Knapsack: dp[c] = giá trị lớn nhất với sức chứa c. Duyệt c GIẢM để mỗi vật dùng 1 lần.
    static int knapsack01(int[] w, int[] v, int cap) {
        int[] dp = new int[cap + 1];
        for (int i = 0; i < w.length; i++)
            for (int c = cap; c >= w[i]; c--)
                dp[c] = Math.max(dp[c], dp[c - w[i]] + v[i]);
        return dp[cap];
    }

    // LeetCode 322: Coin Change (unbounded) — số đồng xu ít nhất. Duyệt c TĂNG (dùng lại được).
    static int coinChange(int[] coins, int amount) {
        int[] dp = new int[amount + 1];
        Arrays.fill(dp, amount + 1);                    // "vô cực" an toàn, không overflow
        dp[0] = 0;
        for (int c = 1; c <= amount; c++)
            for (int coin : coins)
                if (coin <= c) dp[c] = Math.min(dp[c], dp[c - coin] + 1);
        return dp[amount] > amount ? -1 : dp[amount];
    }

    // LeetCode 518: Coin Change II — số CÁCH (tổ hợp): coin ở vòng ngoài để không đếm hoán vị
    static int change(int amount, int[] coins) {
        int[] dp = new int[amount + 1];
        dp[0] = 1;
        for (int coin : coins)
            for (int c = coin; c <= amount; c++) dp[c] += dp[c - coin];
        return dp[amount];
    }

    // LeetCode 300: LIS — O(n log n) với "patience sorting"
    static int lengthOfLIS(int[] nums) {
        int[] tails = new int[nums.length];   // tails[k] = phần tử cuối nhỏ nhất của dãy tăng độ dài k+1
        int size = 0;
        for (int x : nums) {
            int lo = 0, hi = size;
            while (lo < hi) { int mid = (lo + hi) >>> 1; if (tails[mid] < x) lo = mid + 1; else hi = mid; }
            tails[lo] = x;
            if (lo == size) size++;
        }
        return size;
    }

    // LeetCode 1143: LCS — dp[i][j] = LCS của a[0..i) và b[0..j)
    static int lcs(String a, String b) {
        int m = a.length(), n = b.length();
        int[][] dp = new int[m + 1][n + 1];
        for (int i = 1; i <= m; i++)
            for (int j = 1; j <= n; j++)
                dp[i][j] = a.charAt(i - 1) == b.charAt(j - 1)
                        ? dp[i - 1][j - 1] + 1
                        : Math.max(dp[i - 1][j], dp[i][j - 1]);
        return dp[m][n];
    }

    // LeetCode 72: Edit Distance (Levenshtein) — space O(n) với 1 hàng + biến diag
    static int minDistance(String a, String b) {
        int m = a.length(), n = b.length();
        int[] dp = new int[n + 1];
        for (int j = 0; j <= n; j++) dp[j] = j;          // từ "" sang b[0..j): j lần insert
        for (int i = 1; i <= m; i++) {
            int diag = dp[0];                             // dp[i-1][j-1]
            dp[0] = i;
            for (int j = 1; j <= n; j++) {
                int up = dp[j];                           // dp[i-1][j]
                if (a.charAt(i - 1) == b.charAt(j - 1)) dp[j] = diag;
                else dp[j] = 1 + Math.min(diag, Math.min(up, dp[j - 1])); // replace, delete, insert
                diag = up;
            }
        }
        return dp[n];
    }

    // LeetCode 416: Partition Equal Subset Sum — knapsack boolean
    static boolean canPartition(int[] nums) {
        int sum = Arrays.stream(nums).sum();
        if (sum % 2 != 0) return false;
        boolean[] dp = new boolean[sum / 2 + 1];
        dp[0] = true;
        for (int x : nums)
            for (int s = sum / 2; s >= x; s--) dp[s] |= dp[s - x];
        return dp[sum / 2];
    }

    public static void main(String[] args) {
        System.out.println(knapsack01(new int[]{1, 3, 4, 5}, new int[]{1, 4, 5, 7}, 7)); // 9
        System.out.println(coinChange(new int[]{1, 2, 5}, 11));                          // 3
        System.out.println(change(5, new int[]{1, 2, 5}));                               // 4
        System.out.println(lengthOfLIS(new int[]{10, 9, 2, 5, 3, 7, 101, 18}));          // 4
        System.out.println(lcs("abcde", "ace"));                                         // 3
        System.out.println(minDistance("horse", "ros"));                                 // 3
        System.out.println(canPartition(new int[]{1, 5, 11, 5}));                        // true
    }
}
```

**Các "họ" DP cần nhận diện:**

| Họ | State điển hình | Bài đại diện |
|---|---|---|
| 1D tuyến tính | `dp[i]` kết thúc/tính đến `i` | 70, 198, 139 Word Break, 91 Decode Ways |
| Knapsack | `dp[i][cap]` → rút gọn `dp[cap]` | 416, 494 Target Sum, 322, 518 |
| Hai chuỗi | `dp[i][j]` trên prefix của hai chuỗi | 1143 LCS, 72 Edit Distance, 10 Regex Matching |
| Grid | `dp[r][c]` | 62 Unique Paths, 64 Min Path Sum |
| Interval | `dp[l][r]` trên đoạn | 516 Longest Palindromic Subsequence, 312 Burst Balloons |
| State machine | `dp[i][trạng thái]` | 309 Stock with Cooldown, 714 |
| Bitmask | `dp[mask]` | 847, TSP với n ≤ 20 |
| Trên cây | trả nhiều giá trị từ con | 337 House Robber III |

> 💡 **Góc nhìn Senior:** DP có mặt trong nhiều hệ thống thật: `diff`/`git diff` (LCS/Myers), spell-check & fuzzy search (Levenshtein — Elasticsearch `fuzziness`, Lucene dùng Levenshtein automaton), query optimizer chọn thứ tự JOIN (Selinger, DP trên tập con bảng), Viterbi trong NLP. Trong phỏng vấn, hãy **luôn bắt đầu bằng recurrence + memoization**, rồi mới chuyển sang bottom-up và tối ưu space — thể hiện tư duy có hệ thống.

> ⚠️ **Lỗi thường gặp:**
> - 0/1 knapsack duyệt capacity **tăng dần** trên mảng 1D → vô tình dùng một vật nhiều lần.
> - Coin Change II đảo thứ tự vòng lặp → đếm **hoán vị** (thành bài 377 Combination Sum IV).
> - Dùng `Integer.MAX_VALUE` làm vô cực rồi `+1` → overflow thành số âm.
> - Memo bằng `HashMap<String, …>` với key ghép chuỗi → chậm; dùng mảng hoặc encode `i * n + j`.

### 🛠 Bài tập phần 16

**Bài 16.1 — Min Cost Climbing Stairs & Unique Paths (Cơ bản)** — LeetCode 746, 62

**Bài 16.2 — Word Break (Trung bình)** — LeetCode 139
- Đề bài: chuỗi `s` có tách được thành các từ trong `wordDict` không?

**Bài 16.3 — Longest Palindromic Substring (Trung bình)** — LeetCode 5

**Bài 16.4 — Best Time to Buy and Sell Stock with Cooldown (Trung bình)** — LeetCode 309

**Bài 16.5 — Burst Balloons (Nâng cao)** — LeetCode 312

<details>
<summary>Gợi ý lời giải</summary>

- 16.1: `dp[i] = cost[i] + min(dp[i-1], dp[i-2])`; Unique Paths: `dp[c] += dp[c-1]` theo từng hàng (space O(n)), hoặc tổ hợp `C(m+n-2, m-1)`.
- 16.2: `dp[i]` = `s[0..i)` tách được; `dp[i] = OR dp[j] && dict.contains(s.substring(j, i))`, chỉ thử `i - j <= maxWordLen`. O(n·L) lần tra cứu (mỗi lần substring O(L)).
- 16.3: expand around center (2n−1 tâm), O(n²) time, O(1) space — đơn giản hơn DP bảng. (Manacher O(n) chỉ cần biết tên.)
- 16.4: state machine 3 trạng thái `hold`, `sold`, `rest`: `hold = max(hold, rest - p)`, `sold = hold + p` (dùng hold cũ), `rest = max(rest, sold cũ)`. Đáp án `max(sold, rest)`.
- 16.5: thêm 1 ở hai đầu; `dp[l][r]` = điểm tối đa khi nổ hết bóng **giữa** `l` và `r` (mở); chọn `k` là quả **nổ cuối cùng**: `dp[l][r] = max(dp[l][k] + a[l]*a[k]*a[r] + dp[k][r])`. Duyệt theo độ dài đoạn tăng dần. O(n³).

</details>

---

## 17. Bit Manipulation

### 17.1 Khái niệm & thủ thuật cần nhớ
Java `int` 32-bit two's complement; `>>` dịch có dấu, `>>>` dịch không dấu.

| Thủ thuật | Biểu thức |
|---|---|
| Kiểm tra bit `i` | `(x >> i) & 1` |
| Bật / tắt / đảo bit `i` | `x \| (1 << i)`, `x & ~(1 << i)`, `x ^ (1 << i)` |
| Bit thấp nhất đang bật | `x & -x` (Fenwick tree) |
| Tắt bit thấp nhất | `x & (x - 1)` |
| Là luỹ thừa của 2 | `x > 0 && (x & (x - 1)) == 0` |
| Đếm bit 1 | `Integer.bitCount(x)` (intrinsic → lệnh `POPCNT`) |
| `a ^ a = 0`, `a ^ 0 = a` | tìm phần tử lẻ loi |
| Duyệt mọi subset của mask | `for (int s = mask; s > 0; s = (s - 1) & mask)` |

```java
public class Bits {
    // LeetCode 136: Single Number — XOR triệt tiêu cặp
    static int singleNumber(int[] nums) { int r = 0; for (int x : nums) r ^= x; return r; }

    // LeetCode 191: Number of 1 Bits — Brian Kernighan
    static int hammingWeight(int n) { int c = 0; while (n != 0) { n &= n - 1; c++; } return c; }

    // LeetCode 338: Counting Bits — DP trên bit
    static int[] countBits(int n) {
        int[] r = new int[n + 1];
        for (int i = 1; i <= n; i++) r[i] = r[i >> 1] + (i & 1);
        return r;
    }

    // LeetCode 371: Sum of Two Integers không dùng + hay -
    static int add(int a, int b) {
        while (b != 0) {
            int carry = (a & b) << 1;
            a ^= b;
            b = carry;
        }
        return a;
    }

    // Sinh mọi subset bằng bitmask (n ≤ 20)
    static void printSubsets(char[] items) {
        int n = items.length;
        for (int mask = 0; mask < (1 << n); mask++) {
            StringBuilder sb = new StringBuilder("{");
            for (int i = 0; i < n; i++) if ((mask & (1 << i)) != 0) sb.append(items[i]);
            System.out.print(sb.append("} "));
        }
        System.out.println();
    }

    public static void main(String[] args) {
        System.out.println(singleNumber(new int[]{4, 1, 2, 1, 2}));        // 4
        System.out.println(hammingWeight(-1));                            // 32
        System.out.println(java.util.Arrays.toString(countBits(5)));       // [0, 1, 1, 2, 1, 2]
        System.out.println(add(-7, 3));                                    // -4
        printSubsets(new char[]{'a', 'b', 'c'});
    }
}
```

> 💡 **Góc nhìn Senior:** bit manipulation có trong code JDK và hệ thống thật: `HashMap` dùng `hash & (n - 1)` thay `%` (vì `n` là luỹ thừa 2) và `tableSizeFor` làm tròn lên luỹ thừa 2 bằng các phép OR-shift; `EnumSet` (RegularEnumSet) là một `long` bitmask; `BitSet`; Bloom filter; quyền Unix `rwx`; feature flags lưu trong một cột `BIGINT`; `ThreadPoolExecutor.ctl` gói run state (3 bit cao) và worker count (29 bit thấp) trong một `AtomicInteger`.

> ⚠️ **Lỗi thường gặp:**
> - `1 << 31` là số âm; `1 << 32` bằng `1` (shift chỉ lấy 5 bit thấp). Với `long` dùng `1L << i`.
> - Dùng `>>` cho số âm khi muốn dịch logic → vòng lặp vô hạn trong `while (n != 0) n >>= 1`. Dùng `>>>`.
> - Ưu tiên toán tử: `x & 1 == 0` được hiểu là `x & (1 == 0)` → lỗi biên dịch; luôn đóng ngoặc `(x & 1) == 0`.

### 🛠 Bài tập phần 17

**Bài 17.1 — Missing Number (Cơ bản)** — LeetCode 268
- Đề bài: mảng chứa `n` số phân biệt trong `[0, n]`, tìm số thiếu, `O(1)` space.

**Bài 17.2 — Reverse Bits (Trung bình)** — LeetCode 190

**Bài 17.3 — Single Number III (Nâng cao)** — LeetCode 260
- Đề bài: mọi số xuất hiện 2 lần trừ **hai** số xuất hiện 1 lần. Tìm hai số đó, `O(n)` time, `O(1)` space.

<details>
<summary>Gợi ý lời giải</summary>

- 17.1: XOR tất cả chỉ số `0..n` với tất cả phần tử; hoặc `n(n+1)/2 - sum` (cẩn thận overflow → `long`).
- 17.2: lặp 32 lần: `res = (res << 1) | (n & 1); n >>>= 1;`. Follow-up gọi nhiều lần: tra bảng 256 phần tử cho từng byte, hoặc `Integer.reverse(n)`.
- 17.3: `xor = a ^ b` (XOR tất cả); lấy bit phân biệt `diff = xor & -xor`; chia mảng thành hai nhóm theo bit đó, XOR từng nhóm ra `a` và `b`.

</details>

---

## 18. Bài toán thực chiến của Senior

Ở vòng Senior, coding round thường chuyển từ "giải đố" sang **bài toán gần với hệ thống**: cài một cache, một rate limiter, xử lý file lớn hơn RAM… Người phỏng vấn đánh giá thêm: thiết kế API, thread-safety, khả năng mở rộng.

### 18.1 LRU Cache (LeetCode 146)
Yêu cầu `get`/`put` `O(1)`. Cấu trúc: **HashMap** (key → node) + **doubly linked list** (thứ tự sử dụng; đầu = mới nhất, cuối = cũ nhất).

**Cách 1 — dựa trên `LinkedHashMap`** (nên nói ra đầu tiên, rồi hỏi người phỏng vấn có muốn tự cài không):
```java
import java.util.*;

public class LruCacheJdk<K, V> extends LinkedHashMap<K, V> {
    private final int capacity;

    public LruCacheJdk(int capacity) {
        super(16, 0.75f, true);              // accessOrder = true: get() đẩy entry về cuối
        this.capacity = capacity;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > capacity;            // được gọi sau mỗi put
    }

    public static void main(String[] args) {
        LruCacheJdk<Integer, String> c = new LruCacheJdk<>(2);
        c.put(1, "a"); c.put(2, "b"); c.get(1); c.put(3, "c"); // 2 bị loại
        System.out.println(c.keySet());                          // [1, 3]
    }
}
```

**Cách 2 — tự cài:**
```java
import java.util.*;

public class LruCache<K, V> {
    private final class Node {
        K key; V val; Node prev, next;
        Node(K k, V v) { key = k; val = v; }
    }

    private final int capacity;
    private final Map<K, Node> map;
    private final Node head = new Node(null, null), tail = new Node(null, null); // sentinel

    public LruCache(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("capacity must be > 0");
        this.capacity = capacity;
        this.map = new HashMap<>(capacity * 4 / 3 + 1);
        head.next = tail; tail.prev = head;
    }

    public V get(K key) {
        Node n = map.get(key);
        if (n == null) return null;
        moveToFront(n);
        return n.val;
    }

    public void put(K key, V val) {
        Node n = map.get(key);
        if (n != null) { n.val = val; moveToFront(n); return; }
        if (map.size() == capacity) {
            Node lru = tail.prev;
            unlink(lru);
            map.remove(lru.key);
        }
        n = new Node(key, val);
        map.put(key, n);
        addFirst(n);
    }

    private void moveToFront(Node n) { unlink(n); addFirst(n); }
    private void unlink(Node n) { n.prev.next = n.next; n.next.prev = n.prev; }
    private void addFirst(Node n) { n.next = head.next; n.prev = head; head.next.prev = n; head.next = n; }

    public static void main(String[] args) {
        LruCache<Integer, Integer> c = new LruCache<>(2);
        c.put(1, 1); c.put(2, 2);
        System.out.println(c.get(1));  // 1
        c.put(3, 3);                   // loại 2
        System.out.println(c.get(2));  // null
        c.put(4, 4);                   // loại 1
        System.out.println(c.get(1) + " " + c.get(3) + " " + c.get(4)); // null 3 4
    }
}
```

### 18.2 LFU Cache (LeetCode 460)
Loại phần tử có **tần suất truy cập thấp nhất**; hoà thì loại phần tử cũ nhất trong nhóm đó. `O(1)` bằng: `key → value`, `key → freq`, `freq → LinkedHashSet<key>` (giữ thứ tự chèn) và biến `minFreq`.

```java
import java.util.*;

public class LfuCache {
    private final int capacity;
    private int minFreq;
    private final Map<Integer, Integer> values = new HashMap<>();
    private final Map<Integer, Integer> freqs = new HashMap<>();
    private final Map<Integer, LinkedHashSet<Integer>> buckets = new HashMap<>();

    public LfuCache(int capacity) { this.capacity = capacity; }

    public int get(int key) {
        Integer v = values.get(key);
        if (v == null) return -1;
        touch(key);
        return v;
    }

    public void put(int key, int value) {
        if (capacity <= 0) return;
        if (values.containsKey(key)) { values.put(key, value); touch(key); return; }
        if (values.size() == capacity) {
            LinkedHashSet<Integer> bucket = buckets.get(minFreq);
            int evict = bucket.iterator().next();           // cũ nhất trong nhóm tần suất thấp nhất
            bucket.remove(evict);
            if (bucket.isEmpty()) buckets.remove(minFreq);
            values.remove(evict);
            freqs.remove(evict);
        }
        values.put(key, value);
        freqs.put(key, 1);
        buckets.computeIfAbsent(1, f -> new LinkedHashSet<>()).add(key);
        minFreq = 1;
    }

    private void touch(int key) {
        int f = freqs.get(key);
        freqs.put(key, f + 1);
        LinkedHashSet<Integer> bucket = buckets.get(f);
        bucket.remove(key);
        if (bucket.isEmpty()) {
            buckets.remove(f);
            if (minFreq == f) minFreq = f + 1;
        }
        buckets.computeIfAbsent(f + 1, x -> new LinkedHashSet<>()).add(key);
    }

    public static void main(String[] args) {
        LfuCache c = new LfuCache(2);
        c.put(1, 1); c.put(2, 2);
        c.get(1);                                  // freq(1)=2
        c.put(3, 3);                               // loại 2 (freq 1)
        System.out.println(c.get(2) + " " + c.get(3)); // -1 3
    }
}
```

> 💡 **Góc nhìn Senior:** trên production, hãy dùng **Caffeine** (thuật toán **W-TinyLFU**: kết hợp LRU window nhỏ + LFU xấp xỉ bằng Count-Min Sketch, hit rate thường tốt hơn cả LRU lẫn LFU thuần) thay vì tự cài. Cache tự cài ở trên **không thread-safe**; bọc `synchronized` thì đúng nhưng nghẽn; `ConcurrentHashMap` + linked list vẫn cần khoá cho danh sách. Caffeine giải quyết bằng ring buffer ghi nhận truy cập và áp dụng theo batch. Redis dùng **approximated LRU/LFU** (lấy mẫu ngẫu nhiên vài key — `maxmemory-samples`) để tránh chi phí duy trì danh sách chính xác.

### 18.3 Consistent Hashing
Vấn đề: phân phối key cho `N` server bằng `hash(key) % N` → khi thêm/bớt server, **gần như mọi key đổi chỗ** (cache miss hàng loạt). Consistent hashing đặt server và key trên một **vòng băm**; key thuộc server đầu tiên theo chiều kim đồng hồ. Thêm/bớt một server chỉ di chuyển ~`1/N` số key. **Virtual nodes** (mỗi server nhiều điểm trên vòng) giúp cân bằng tải.

```java
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class ConsistentHashRing<T> {
    private final TreeMap<Long, T> ring = new TreeMap<>();
    private final int virtualNodes;

    public ConsistentHashRing(int virtualNodes) { this.virtualNodes = virtualNodes; }

    public void addNode(T node) {
        for (int i = 0; i < virtualNodes; i++) ring.put(hash(node + "#" + i), node);
    }

    public void removeNode(T node) {
        for (int i = 0; i < virtualNodes; i++) ring.remove(hash(node + "#" + i));
    }

    public T nodeFor(String key) {
        if (ring.isEmpty()) return null;
        Map.Entry<Long, T> e = ring.ceilingEntry(hash(key)); // O(log(N·V))
        return (e != null ? e : ring.firstEntry()).getValue(); // vòng lại đầu
    }

    private static long hash(String s) {
        try {
            byte[] d = MessageDigest.getInstance("MD5").digest(s.getBytes(StandardCharsets.UTF_8));
            long h = 0;
            for (int i = 0; i < 8; i++) h = (h << 8) | (d[i] & 0xFF);
            return h;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static void main(String[] args) {
        ConsistentHashRing<String> ring = new ConsistentHashRing<>(100);
        List.of("node-A", "node-B", "node-C").forEach(ring::addNode);
        int keys = 100_000;
        Map<String, String> before = new HashMap<>();
        Map<String, Integer> load = new TreeMap<>();
        for (int i = 0; i < keys; i++) {
            String k = "user:" + i, n = ring.nodeFor(k);
            before.put(k, n);
            load.merge(n, 1, Integer::sum);
        }
        System.out.println("Phân phối: " + load);           // mỗi node ~ 30–38%; tăng virtual node để bớt lệch
        ring.addNode("node-D");
        long moved = before.entrySet().stream().filter(e -> !ring.nodeFor(e.getKey()).equals(e.getValue())).count();
        System.out.printf("Key bị di chuyển: %.1f%%%n", 100.0 * moved / keys); // ~20–25% (lý tưởng 1/4), so với ~75% nếu dùng % N
    }
}
```
Ứng dụng: Cassandra/DynamoDB partitioning (token ring), Memcached client (Ketama), load balancer sticky session. Biến thể khác: **rendezvous hashing** (HRW), **jump consistent hash** (Google), và Redis Cluster dùng **16384 hash slot cố định** (`CRC16(key) mod 16384`) thay vì vòng băm.

### 18.4 Rate Limiter
| Thuật toán | Ý tưởng | Ưu | Nhược |
|---|---|---|---|
| Fixed window counter | Đếm request trong mỗi cửa sổ cố định (VD từng phút) | Đơn giản, 1 counter (Redis `INCR` + `EXPIRE`) | Burst gấp đôi ở biên cửa sổ |
| Sliding window log | Lưu timestamp từng request, xoá cái quá hạn | Chính xác | Tốn bộ nhớ `O(limit)` mỗi user |
| Sliding window counter | Ước lượng: `cur + prev × (phần cửa sổ trước còn chồng lên)` | Rẻ, khá chính xác | Xấp xỉ |
| **Token bucket** | Bucket chứa tối đa `B` token, nạp `r` token/s; mỗi request lấy 1 token | Cho phép burst có kiểm soát, phổ biến nhất (Guava `RateLimiter`, Bucket4j, AWS API Gateway) | Cần đồng bộ khi phân tán |
| Leaky bucket | Queue xả với tốc độ cố định | Làm mượt traffic đầu ra | Tăng latency |

```java
import java.util.ArrayDeque;
import java.util.Deque;

public class RateLimiters {
    /** Token bucket, thread-safe, nạp token "lười" khi có request (không cần thread nền). */
    static final class TokenBucket {
        private final long capacity;
        private final double refillPerNano;
        private double tokens;
        private long lastRefill;

        TokenBucket(long capacity, double tokensPerSecond) {
            this.capacity = capacity;
            this.refillPerNano = tokensPerSecond / 1_000_000_000.0;
            this.tokens = capacity;
            this.lastRefill = System.nanoTime();
        }

        synchronized boolean tryAcquire() {
            long now = System.nanoTime();                    // monotonic, không dùng currentTimeMillis
            tokens = Math.min(capacity, tokens + (now - lastRefill) * refillPerNano);
            lastRefill = now;
            if (tokens >= 1) { tokens -= 1; return true; }
            return false;
        }
    }

    /** Sliding window log: tối đa `limit` request trong `windowNanos` gần nhất. */
    static final class SlidingWindowLog {
        private final int limit;
        private final long windowNanos;
        private final Deque<Long> log = new ArrayDeque<>();

        SlidingWindowLog(int limit, long windowNanos) { this.limit = limit; this.windowNanos = windowNanos; }

        synchronized boolean tryAcquire() {
            long now = System.nanoTime();
            while (!log.isEmpty() && now - log.peekFirst() >= windowNanos) log.pollFirst();
            if (log.size() < limit) { log.offerLast(now); return true; }
            return false;
        }
    }

    public static void main(String[] args) {
        TokenBucket tb = new TokenBucket(5, 2);              // burst 5, sau đó 2 req/s
        int ok = 0;
        for (int i = 0; i < 10; i++) if (tb.tryAcquire()) ok++;
        System.out.println("Token bucket cho qua: " + ok);  // 5 (gọi dồn dập ngay lập tức)

        SlidingWindowLog sw = new SlidingWindowLog(3, 1_000_000_000L);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) sb.append(sw.tryAcquire() ? "Y" : "N");
        System.out.println(sb);                              // YYYNN
    }
}
```

> 💡 **Góc nhìn Senior:** rate limiter phân tán thường đặt state trong **Redis** và thực thi bằng **Lua script** (atomic: đọc token, tính nạp, trừ, ghi lại trong một lệnh) hoặc dùng sorted set (`ZADD` timestamp, `ZREMRANGEBYSCORE`, `ZCARD`) cho sliding log. Cần bàn: khoá theo gì (user, IP, API key), fail-open hay fail-closed khi Redis chết, trả `HTTP 429` kèm header `Retry-After`, và đồng hồ giữa các node (dùng thời gian của Redis server thay vì client).

### 18.5 Top-K frequent với Streams & dữ liệu lớn
```java
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TopKWords {
    // LeetCode 692: Top K Frequent Words — tần suất giảm dần, hoà thì theo thứ tự từ điển
    static List<String> topKStreams(List<String> words, int k) {
        return words.stream()
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
            .entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                    .thenComparing(Map.Entry.comparingByKey()))
            .limit(k)
            .map(Map.Entry::getKey)
            .toList();                                         // O(n + u log u), u = số từ khác nhau
    }

    // Phiên bản heap: O(n + u log k), bộ nhớ heap O(k)
    static List<String> topKHeap(List<String> words, int k) {
        Map<String, Long> freq = new HashMap<>();
        for (String w : words) freq.merge(w, 1L, Long::sum);
        Comparator<Map.Entry<String, Long>> worstFirst = Map.Entry.<String, Long>comparingByValue()
            .thenComparing(Map.Entry.<String, Long>comparingByKey().reversed()); // đỉnh heap = phần tử "kém" nhất
        PriorityQueue<Map.Entry<String, Long>> heap = new PriorityQueue<>(worstFirst);
        for (Map.Entry<String, Long> e : freq.entrySet()) {
            heap.offer(e);
            if (heap.size() > k) heap.poll();
        }
        LinkedList<String> res = new LinkedList<>();
        while (!heap.isEmpty()) res.addFirst(heap.poll().getKey());
        return res;
    }

    public static void main(String[] args) {
        List<String> w = List.of("i", "love", "leetcode", "i", "love", "coding");
        System.out.println(topKStreams(w, 2) + " " + topKHeap(w, 2)); // [i, love] [i, love]
    }
}
```
Khi dữ liệu không vừa bộ nhớ (VD log 100GB, hàng tỉ URL):
1. **Partition theo hash**: đọc tuần tự, ghi mỗi từ vào file `hash(word) % 256` → mọi bản sao của một từ nằm cùng file; đếm từng file bằng HashMap, giữ top-K cục bộ, rồi merge các top-K (đúng tuyệt đối vì đã partition theo key). Đây chính là **MapReduce** / Spark `reduceByKey` + `top`.
2. **Xấp xỉ trên stream vô hạn**: Count-Min Sketch + min-heap (heavy hitters), hoặc Misra-Gries / Space-Saving với bộ nhớ `O(k)`.
3. Kafka Streams: `groupByKey().windowedBy(...).count()` rồi tính top-K theo cửa sổ thời gian.

### 18.6 Merge K sorted lists & External merge sort
**Merge K sorted lists (LeetCode 23)**: min-heap chứa phần tử đầu của mỗi list; poll nhỏ nhất, đẩy phần tử kế tiếp của list đó. `O(N log k)` với `N` tổng số phần tử.

```java
import java.util.*;

public class MergeKLists {
    static final class ListNode { int val; ListNode next; ListNode(int v, ListNode n) { val = v; next = n; } }

    static ListNode mergeKLists(ListNode[] lists) {
        PriorityQueue<ListNode> pq = new PriorityQueue<>(Comparator.comparingInt(n -> n.val));
        for (ListNode l : lists) if (l != null) pq.offer(l);
        ListNode dummy = new ListNode(0, null), tail = dummy;
        while (!pq.isEmpty()) {
            ListNode n = pq.poll();
            tail.next = n;
            tail = n;
            if (n.next != null) pq.offer(n.next);
        }
        return dummy.next;
    }

    public static void main(String[] args) {
        ListNode a = new ListNode(1, new ListNode(4, new ListNode(5, null)));
        ListNode b = new ListNode(1, new ListNode(3, new ListNode(4, null)));
        ListNode c = new ListNode(2, new ListNode(6, null));
        StringBuilder sb = new StringBuilder();
        for (ListNode n = mergeKLists(new ListNode[]{a, b, c}); n != null; n = n.next) sb.append(n.val).append(' ');
        System.out.println(sb.toString().trim()); // 1 1 2 3 4 4 5 6
    }
}
```

**External merge sort** — sort một file lớn hơn RAM (*Algorithms for Interviews* Ch.2 có bài tương tự; đây cũng là cách DB thực hiện `ORDER BY` khi vượt `work_mem`/`sort_buffer_size`):
1. **Pha chia**: đọc từng khối vừa bộ nhớ, sort trong RAM, ghi ra file tạm (run).
2. **Pha trộn**: K-way merge các run bằng min-heap, chỉ giữ 1 dòng/run trong bộ nhớ (+ buffer I/O).

```java
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class ExternalSort {
    private record Head(String line, BufferedReader reader) {}

    public static void sort(Path input, Path output, int maxLinesInMemory) throws IOException {
        List<Path> runs = new ArrayList<>();
        try (BufferedReader r = Files.newBufferedReader(input)) {
            List<String> buf = new ArrayList<>(maxLinesInMemory);
            String line;
            while ((line = r.readLine()) != null) {
                buf.add(line);
                if (buf.size() == maxLinesInMemory) { runs.add(writeRun(buf)); buf.clear(); }
            }
            if (!buf.isEmpty()) runs.add(writeRun(buf));
        }
        mergeRuns(runs, output);
    }

    private static Path writeRun(List<String> buf) throws IOException {
        Collections.sort(buf);                                    // TimSort trong RAM
        Path tmp = Files.createTempFile("run-", ".txt");
        Files.write(tmp, buf);
        return tmp;
    }

    private static void mergeRuns(List<Path> runs, Path output) throws IOException {
        PriorityQueue<Head> pq = new PriorityQueue<>(Comparator.comparing(Head::line));
        List<BufferedReader> readers = new ArrayList<>();
        try (BufferedWriter w = Files.newBufferedWriter(output)) {
            for (Path run : runs) {
                BufferedReader r = Files.newBufferedReader(run);
                readers.add(r);
                String first = r.readLine();
                if (first != null) pq.offer(new Head(first, r));
            }
            while (!pq.isEmpty()) {
                Head h = pq.poll();
                w.write(h.line());
                w.newLine();
                String next = h.reader().readLine();
                if (next != null) pq.offer(new Head(next, h.reader()));
            }
        } finally {
            for (BufferedReader r : readers) r.close();
            for (Path run : runs) Files.deleteIfExists(run);
        }
    }

    public static void main(String[] args) throws IOException {
        Path in = Files.createTempFile("input-", ".txt"), out = Files.createTempFile("output-", ".txt");
        Random rnd = new Random(42);
        List<String> data = new ArrayList<>();
        for (int i = 0; i < 10_000; i++) data.add(String.format("%08d", rnd.nextInt(100_000_000)));
        Files.write(in, data);

        sort(in, out, 1_000);                                     // 10 run, mỗi run 1000 dòng

        List<String> expected = new ArrayList<>(data);
        Collections.sort(expected);
        System.out.println(expected.equals(Files.readAllLines(out)));   // true
        Files.delete(in); Files.delete(out);
    }
}
```

> 💡 **Góc nhìn Senior:** các con số cần bàn khi phỏng vấn: với RAM `M`, file `N`, số run ban đầu là `N/M`; nếu số run quá lớn (giới hạn file descriptor, buffer mỗi run quá nhỏ gây random I/O) thì merge **nhiều pha** (multi-pass) với fan-in `k` hợp lý. Tối ưu: buffer I/O lớn (vài MB/run), nén file tạm, dùng `replacement selection` để tạo run dài gấp ~2 lần RAM, song song hoá pha chia. Trong hệ phân tán: Hadoop/Spark sort-shuffle chính là external merge sort trên nhiều máy.

> ⚠️ **Lỗi thường gặp (phần 18):**
> - LRU: quên xoá key khỏi `HashMap` khi loại node khỏi list (rò rỉ bộ nhớ), hoặc quên cập nhật vị trí khi `put` key đã tồn tại.
> - Consistent hashing không có virtual nodes → phân phối lệch nghiêm trọng.
> - Rate limiter dùng `System.currentTimeMillis()` → sai khi đồng hồ hệ thống bị chỉnh (NTP); dùng `System.nanoTime()` cho khoảng thời gian.
> - External sort: không đóng reader / không xoá file tạm; `Comparator` cho chuỗi số không padding (`"10" < "9"` theo thứ tự từ điển).

### 🛠 Bài tập phần 18

**Bài 18.1 — Logger Rate Limiter & Design Hit Counter (Cơ bản)** — LeetCode 359, 362
- Đề bài: `shouldPrintMessage(timestamp, message)` chỉ trả `true` nếu cùng message chưa in trong 10 giây; `HitCounter` đếm số hit trong 300 giây gần nhất. Follow-up: hàng nghìn hit trong cùng một giây?

**Bài 18.2 — Thread-safe LRU (Trung bình)**
- Đề bài: biến `LruCache` thành thread-safe. So sánh 3 phương án: (a) `synchronized` mọi phương thức; (b) `ReentrantReadWriteLock` — vì sao **không** giúp được với `get` của LRU?; (c) phân mảnh (striping) thành `N` LRU nhỏ theo `hash(key) % N`. Viết benchmark JMH đơn giản với 8 thread.

**Bài 18.3 — Consistent hashing có trọng số (Trung bình)**
- Đề bài: mở rộng `ConsistentHashRing` để mỗi node có trọng số (server mạnh gấp đôi nhận gấp đôi key). Đo độ lệch chuẩn của tải với 10, 100, 500 virtual node mỗi đơn vị trọng số.

**Bài 18.4 — Top-K URL trong file 50GB (Nâng cao)**
- Đề bài: máy có 4GB RAM, file log 50GB mỗi dòng một URL. Trình bày thuật toán và viết code Java tìm 100 URL xuất hiện nhiều nhất. Phân tích số lần đọc/ghi đĩa.

<details>
<summary>Gợi ý lời giải</summary>

- 18.1: Logger: `Map<String,Integer> lastPrinted`; nếu `timestamp - last >= 10` thì cập nhật và trả true. (Bộ nhớ tăng vô hạn → dọn định kỳ hoặc dùng hai map xoay vòng mỗi 10s.) HitCounter: mảng vòng `int[300] times, hits`; `idx = timestamp % 300`; nếu `times[idx] != timestamp` thì reset `hits[idx] = 1` ngược lại `hits[idx]++`; `getHits` cộng các ô có `timestamp - times[i] < 300`. O(1) bộ nhớ dù nhiều hit/giây.
- 18.2: (a) đúng và đơn giản; (b) `get` của LRU **sửa** danh sách (move-to-front) nên cũng cần write lock → read lock vô dụng; (c) striping giảm tranh chấp nhưng LRU chỉ còn "xấp xỉ toàn cục". Đây là lý do Caffeine ghi nhận truy cập vào buffer và áp dụng bất đồng bộ.
- 18.3: số virtual node của node = `weight × vnodesPerUnit`. Độ lệch giảm khi số vnode tăng (≈ tỉ lệ `1/√(vnode)`), đổi lại `TreeMap` lớn hơn và add/remove chậm hơn.
- 18.4: Pha 1: đọc tuần tự 50GB, ghi mỗi URL vào một trong 256 file theo `(hash(url) & 0x7fffffff) % 256` (~200MB/file, vừa RAM). Pha 2: với mỗi file, đếm bằng `HashMap<String, Integer>`, giữ min-heap 100 phần tử toàn cục (đúng vì một URL chỉ nằm ở 1 file). I/O: đọc 50GB + ghi 50GB + đọc 50GB ≈ 150GB tuần tự. Nếu một file vẫn quá lớn (URL nóng bị lệch) → chia đệ quy với hash khác. Có thể song song hoá pha 2 bằng `ExecutorService`.

</details>

---

## 19. Lộ trình luyện tập 75 bài theo pattern

Danh sách dưới đây dựa trên tinh thần NeetCode roadmap / Blind 75, được sắp xếp lại theo đúng thứ tự module. Mức độ là phân loại của giáo trình này (CB = Cơ bản, TB = Trung bình, NC = Nâng cao), không nhất thiết trùng nhãn Easy/Medium/Hard của LeetCode. 🔒 = bài premium (có thể tìm đề tương đương trên LintCode).

| # | Pattern | Cơ bản | Trung bình | Nâng cao |
|---|---|---|---|---|
| 1 | Arrays & Hashing (6) | 242 Valid Anagram · 1 Two Sum | 49 Group Anagrams · 347 Top K Frequent Elements · 238 Product of Array Except Self | 128 Longest Consecutive Sequence |
| 2 | Two Pointers (3) | 125 Valid Palindrome | 15 3Sum | 42 Trapping Rain Water |
| 3 | Sliding Window (5) | 121 Best Time to Buy and Sell Stock | 3 Longest Substring Without Repeating Characters · 424 Longest Repeating Character Replacement | 76 Minimum Window Substring · 239 Sliding Window Maximum |
| 4 | Prefix Sum (3) | 303 Range Sum Query – Immutable | 560 Subarray Sum Equals K · 523 Continuous Subarray Sum | |
| 5 | Stack / Monotonic (5) | 20 Valid Parentheses | 155 Min Stack · 150 Evaluate RPN · 739 Daily Temperatures | 84 Largest Rectangle in Histogram |
| 6 | Binary Search (5) | 704 Binary Search | 875 Koko Eating Bananas · 153 Find Min in Rotated Sorted Array · 33 Search in Rotated Sorted Array | 4 Median of Two Sorted Arrays |
| 7 | Linked List (5) | 206 Reverse Linked List · 21 Merge Two Sorted Lists · 141 Linked List Cycle | 143 Reorder List | 23 Merge k Sorted Lists |
| 8 | Trees (8) | 226 Invert Binary Tree · 104 Maximum Depth | 102 Level Order Traversal · 98 Validate BST · 230 Kth Smallest in BST · 236 LCA of Binary Tree · 105 Construct from Preorder & Inorder | 124 Binary Tree Maximum Path Sum |
| 9 | Heap (3) | | 215 Kth Largest Element · 973 K Closest Points | 295 Find Median from Data Stream |
| 10 | Trie (2) | | 208 Implement Trie | 212 Word Search II |
| 11 | Graph (7) | | 200 Number of Islands · 133 Clone Graph · 994 Rotting Oranges · 207 Course Schedule · 684 Redundant Connection · 743 Network Delay Time | 127 Word Ladder |
| 12 | Backtracking (4) | | 78 Subsets · 46 Permutations · 39 Combination Sum · 79 Word Search | |
| 13 | Greedy & Intervals (5) | | 53 Maximum Subarray · 55 Jump Game · 56 Merge Intervals · 435 Non-overlapping Intervals · 253 Meeting Rooms II 🔒 | |
| 14 | Dynamic Programming (8) | 70 Climbing Stairs | 198 House Robber · 322 Coin Change · 300 LIS · 1143 LCS · 139 Word Break · 416 Partition Equal Subset Sum | 72 Edit Distance |
| 15 | Bit Manipulation (2) | 136 Single Number · 268 Missing Number | | |
| 16 | Thiết kế cấu trúc dữ liệu (4) | | 146 LRU Cache · 362 Design Hit Counter 🔒 · 692 Top K Frequent Words | 460 LFU Cache |

Tổng: 6 + 3 + 5 + 3 + 5 + 5 + 5 + 8 + 3 + 2 + 7 + 4 + 5 + 8 + 2 + 4 = **75 bài**.

**Kế hoạch 3 tuần gợi ý** (song song với việc đọc lý thuyết các phần 1–18):

| Tuần | Pattern | Mục tiêu |
|---|---|---|
| 1 | 1 → 7 | ~32 bài; mỗi bài CB ≤ 15', TB ≤ 30'. Ghi "pattern + mẹo then chốt" vào sổ tay sau mỗi bài. |
| 2 | 8 → 12 | ~24 bài; luyện viết cả đệ quy lẫn iterative cho cây/graph. |
| 3 | 13 → 16 | ~19 bài + 3 buổi mock interview 45' (bạn bè, Pramp, hoặc tự quay video). |

Nguyên tắc luyện:
- **Spaced repetition**: làm lại bài sau 1 ngày, 1 tuần, 1 tháng — không xem lời giải.
- Bí quá 25' thì xem gợi ý (không xem code), 40' thì xem lời giải rồi **tự viết lại từ đầu** ngày hôm sau.
- Luôn viết bằng Java "sạch" như production: tên biến có nghĩa, không dùng biến toàn cục bừa bãi, xử lý input rỗng.

### 🛠 Bài tập phần 19

**Bài 19.1 — Phân loại pattern nhanh (Cơ bản)**
- Đề bài: đọc đề (chỉ đọc, không giải) 20 bài ngẫu nhiên trong danh sách trên mà bạn chưa làm; trong ≤ 2 phút/bài, viết ra pattern dự đoán và độ phức tạp mục tiêu. Đối chiếu với lời giải chính thức. Mục tiêu ≥ 80% đúng.

**Bài 19.2 — Mock interview có follow-up (Trung bình)**
- Đề bài: cùng một người bạn, mỗi người ra cho người kia 1 bài TB + 1 follow-up "Senior" (dữ liệu là stream? không vừa RAM? nhiều thread cùng gọi?). 45 phút/lượt, chấm theo khung 6 bước ở phần 2.

**Bài 19.3 — Viết lại "cheat sheet" bằng tay (Nâng cao)**
- Đề bài: không nhìn tài liệu, viết trên giấy trong 60 phút: template binary search, sliding window, BFS, Dijkstra, union-find, topological sort (Kahn), quickselect, LRU, backtracking, knapsack 1D. Sau đó gõ vào IDE và chạy với test — đếm số lỗi biên dịch/logic.

<details>
<summary>Gợi ý lời giải</summary>

- 19.1: dựa vào bảng "dấu hiệu → pattern" ở mục 2.2. Những bài hay đoán nhầm: 560 (tưởng sliding window, thực ra prefix sum vì có số âm), 128 (tưởng sort, thực ra HashSet O(n)), 153 (tưởng duyệt tuyến tính), 300 (O(n²) DP vs O(n log n) binary search).
- 19.2: follow-up mẫu cho 347 Top K: "dữ liệu là stream vô hạn" → Count-Min Sketch + heap hoặc Space-Saving; "phân tán trên 100 máy" → partition theo hash key, top-K cục bộ, merge; "cần cập nhật realtime" → Redis sorted set (`ZINCRBY`, `ZREVRANGE 0 k-1`).
- 19.3: mục tiêu 0 lỗi biên dịch, ≤ 1 lỗi logic. Lỗi hay gặp nhất: off-by-one trong binary search, quên `visited` trong BFS, quên copy list trong backtracking, quên `continue` khi gặp bản ghi cũ trong Dijkstra.

</details>

---

## Dự án mini

### `algo-toolkit` — thư viện cấu trúc dữ liệu & thuật toán "cấp production" (1–2 ngày)

**Bối cảnh:** team của bạn cần một module nội bộ chứa các thành phần thuật toán dùng lại được cho dịch vụ API gateway. Bạn được giao xây dựng phiên bản đầu tiên.

**Yêu cầu chức năng:**
1. `Cache<K, V>` interface với hai cài đặt `LruCache` và `LfuCache` — `get`, `put`, `remove`, `size`, thống kê `hitRate()`; cả hai `O(1)`.
2. `ConsistentHashRing<T>` có virtual node, trọng số, `addNode/removeNode/nodeFor`, và hàm `Map<T, Double> loadDistribution(Collection<String> sampleKeys)`.
3. `RateLimiter` interface với `TokenBucketRateLimiter` và `SlidingWindowRateLimiter`, quản lý **theo key** (VD theo userId) bằng `ConcurrentHashMap<String, Bucket>`; tự dọn bucket không hoạt động.
4. `TopKTracker<T>`: nhận `record(T item)` liên tục, trả `List<T> topK()` — phiên bản chính xác (HashMap + heap) và phiên bản xấp xỉ (Count-Min Sketch + heap).
5. CLI `ExternalSortCommand`: `java -jar algo-toolkit.jar sort --input big.txt --output sorted.txt --memory-lines 1000000`.

**Yêu cầu phi chức năng:**
- Java 17+, Maven/Gradle, không phụ thuộc thư viện ngoài cho phần lõi (được dùng JUnit 5, AssertJ, JMH).
- Thread-safety được tài liệu hoá rõ ràng (Javadoc ghi "thread-safe" hay "not thread-safe") — `RateLimiter` **bắt buộc** thread-safe.
- Test coverage ≥ 85%; có **property-based test** hoặc test ngẫu nhiên so sánh với cài đặt tham chiếu (VD `LruCache` so với `LinkedHashMap(accessOrder=true)` qua 100.000 thao tác ngẫu nhiên; external sort so với `Collections.sort`).
- Benchmark JMH: `LruCache` tự cài vs `LinkedHashMap` vs Caffeine (nếu cho phép thêm dependency trong module benchmark); `TokenBucket` với 1, 4, 16 thread.
- README ngắn: độ phức tạp từng thao tác, trade-off đã chọn.

**Tiêu chí chấm (100 điểm):**

| Tiêu chí | Điểm |
|---|---|
| Đúng chức năng, test ngẫu nhiên pass | 35 |
| Độ phức tạp đạt yêu cầu (`O(1)` cache, `O(log n)` ring lookup, `O(N log k)` merge) | 20 |
| Thread-safety đúng, không race condition (chạy test đa luồng lặp 100 lần) | 15 |
| Chất lượng code: API rõ ràng, generic hợp lý, xử lý input không hợp lệ | 15 |
| Benchmark + phân tích kết quả (giải thích được vì sao nhanh/chậm) | 10 |
| README & Javadoc | 5 |

---

## Checklist tự đánh giá
- [ ] Tôi phân tích được time/space của code bất kỳ, kể cả chi phí ẩn của thư viện (`substring`, `remove(0)`, `contains` trên `List`) và stack của đệ quy.
- [ ] Tôi giải thích được amortized `O(1)` của `ArrayList.add` và vì sao phải tăng capacity theo cấp số nhân.
- [ ] Tôi áp dụng được Master theorem cho 3 trường hợp và biết khi nào không dùng được.
- [ ] Tôi trình bày một bài coding theo đủ 6 bước: clarify → ví dụ → brute force → tối ưu → code → test & complexity.
- [ ] Tôi nhận diện được pattern của một bài mới trong ≤ 3 phút dựa vào ràng buộc và từ khoá trong đề.
- [ ] Tôi tự viết lại không cần tài liệu: lower bound binary search, sliding window co giãn, prefix sum + HashMap, monotonic stack, monotonic deque.
- [ ] Tôi áp dụng được binary search trên không gian đáp án và chứng minh được vị từ đơn điệu.
- [ ] Tôi giải thích được vì sao `Arrays.sort` dùng Dual-Pivot Quicksort cho primitive và TimSort cho object, và stability nghĩa là gì.
- [ ] Tôi tự cài quicksort, merge sort, heapsort, quickselect và biết worst case của từng cái.
- [ ] Tôi đảo ngược linked list (iterative & recursive), phát hiện chu trình bằng Floyd và chứng minh được vì sao đúng.
- [ ] Tôi duyệt cây bằng cả đệ quy và iterative (pre/in/post/level-order), cài được insert/delete/validate BST, LCA.
- [ ] Tôi biết `TreeMap` là red-black tree, và vì sao DB index dùng B+ tree.
- [ ] Tôi dùng thành thạo `PriorityQueue` cho top-K, two heaps (median), merge K; biết độ phức tạp của `remove(Object)`.
- [ ] Tôi cài được Trie với insert/search/startsWith/autocomplete.
- [ ] Tôi cài được BFS, DFS, topological sort (Kahn & DFS 3 màu), union-find (path compression + union by size), Dijkstra, và chọn đúng thuật toán shortest path theo loại trọng số.
- [ ] Tôi viết được backtracking cho subsets/permutations/combinations có xử lý trùng lặp.
- [ ] Tôi chứng minh được tính đúng của một thuật toán greedy bằng exchange argument; giải được các bài interval (merge, non-overlap, meeting rooms).
- [ ] Tôi giải DP theo 4 bước (state, transition, base, order), chuyển được memoization ↔ tabulation, và tối ưu space; làm được knapsack 0/1 & unbounded, LIS `O(n log n)`, LCS, edit distance, coin change.
- [ ] Tôi dùng được các thủ thuật bit (`x & (x-1)`, `x & -x`, XOR) và biết bẫy `1 << 31`, `>>` vs `>>>`.
- [ ] Tôi tự cài LRU (cả bằng `LinkedHashMap` và HashMap + doubly linked list) và LFU `O(1)`, và biết vì sao production nên dùng Caffeine (W-TinyLFU).
- [ ] Tôi giải thích và cài được consistent hashing với virtual nodes; so sánh với hash slot của Redis Cluster.
- [ ] Tôi so sánh được 5 thuật toán rate limiting và cài token bucket thread-safe; biết cách làm phân tán với Redis + Lua.
- [ ] Tôi tìm được top-K trên dữ liệu lớn hơn RAM (partition theo hash) và trên stream (Count-Min Sketch / Space-Saving).
- [ ] Tôi cài được external merge sort và giải thích được số pass, I/O, và liên hệ với `ORDER BY`/shuffle của Spark.
- [ ] Tôi đã làm ≥ 60/75 bài trong lộ trình luyện tập và làm lại được ≥ 20 bài sau 1 tuần không xem lời giải.

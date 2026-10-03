# Câu hỏi phỏng vấn — Module 17: Cấu trúc dữ liệu & Giải thuật (live coding)

> Giáo trình tương ứng: [Module 17 — Cấu trúc dữ liệu & Giải thuật cho phỏng vấn](../01-giao-trinh/17-data-structures-algorithms.md)

**Cách dùng:** đây là bộ **32 bài live coding** hay gặp ở vòng kỹ thuật của các công ty Việt Nam (product, outsourcing lớn, ngân hàng/fintech) lẫn công ty nước ngoài. Với mỗi bài: đọc đề, bật đồng hồ (🟢 ≤ 15', 🟡 ≤ 30', 🔴 ≤ 40'), **nói thành tiếng** theo khung 6 bước — *clarify → ví dụ → brute force → tối ưu → code → test & complexity* — rồi mới mở đáp án. Tự gõ lại lời giải không nhìn tài liệu ngày hôm sau.

**Ký hiệu mức độ:** 🟢 Cơ bản · 🟡 Senior (mức Medium) · 🔴 Xoáy sâu (Hard hoặc bài thiết kế có thread-safety) · 🎬 Bài "thực chiến" sát hệ thống

**Về code:** mọi lời giải là một class Java độc lập (Java 17+, không phụ thuộc thư viện ngoài) kèm `main` tự kiểm tra test case — đã được biên dịch và chạy bằng `javac`/`java` (JDK 21). Chạy nhanh: lưu thành `TwoSum.java` rồi `java TwoSum.java` (single-file source launcher). Trong phỏng vấn thật, bạn chỉ cần viết phần method chính; phần `main`/`check` là để tự luyện.

## Mục lục

1. [Arrays & Hashing](#g1) — Q1–Q6
2. [Two Pointers & Sliding Window](#g2) — Q7–Q11
3. [Prefix Sum & Binary Search](#g3) — Q12–Q14
4. [Stack, Monotonic Stack/Deque](#g4) — Q15–Q17
5. [Linked List](#g5) — Q18–Q20
6. [Trees](#g6) — Q21–Q23
7. [Heap & Graph](#g7) — Q24–Q27
8. [Intervals & Dynamic Programming](#g8) — Q28–Q30
9. [Bài thực chiến của Senior](#g9) — Q31–Q32

> 💡 Ở level Senior, đáp án đúng chỉ là điều kiện cần. Điểm cộng nằm ở: hỏi làm rõ đúng chỗ (overflow, trùng lặp, input rỗng, Unicode), nói trade-off time/space trước khi code, code sạch có tên biến rõ ràng, tự test edge case, và trả lời follow-up "nếu dữ liệu là stream / không vừa RAM / nhiều thread cùng gọi".

---

<a id="g1"></a>
## 1. Arrays & Hashing

### Q1. 🟢 Two Sum (LeetCode 1)

**Đề bài:** Cho mảng `nums` và số `target`, trả về chỉ số `i < j` sao cho `nums[i] + nums[j] == target`. Ví dụ: `nums = [2,7,11,15], target = 9` → `[0,1]`.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Duyệt một lượt, dùng `HashMap<giá trị, chỉ số>`; tại mỗi phần tử tra `target - nums[i]` đã thấy chưa, rồi mới `put` phần tử hiện tại. Time O(n), space O(n).

**Giải thích chi tiết:**

*Câu hỏi làm rõ nên hỏi:* Luôn có đúng một đáp án? Không có thì trả gì (mảng rỗng, `null`, exception)? Có phần tử trùng (`[3,3]`, target 6)? Được dùng một phần tử hai lần không? Mảng đã sort chưa? Giá trị có thể gần `Integer.MAX_VALUE` (overflow)?

*Brute force → tối ưu:* Hai vòng lặp thử mọi cặp → O(n²). Công việc lặp lại là "tìm phần bù" → tra cứu O(1) bằng hash. Nếu mảng **đã sort** hoặc bộ nhớ hạn chế: sort + two pointers O(n log n)/O(1) nhưng mất chỉ số gốc (phải sort mảng chỉ số).

```java
import java.util.*;

public class TwoSum {
    public static int[] twoSum(int[] nums, int target) {
        Map<Integer, Integer> indexOf = new HashMap<>();      // value -> index
        for (int i = 0; i < nums.length; i++) {
            Integer j = indexOf.get(target - nums[i]);
            if (j != null) return new int[]{j, i};
            indexOf.put(nums[i], i);                           // put SAU khi tra để không dùng lại chính nó
        }
        return new int[0];                                     // không có đáp án
    }

    public static void main(String[] args) {
        check(Arrays.equals(twoSum(new int[]{2, 7, 11, 15}, 9), new int[]{0, 1}));
        check(Arrays.equals(twoSum(new int[]{3, 2, 4}, 6), new int[]{1, 2}));
        check(Arrays.equals(twoSum(new int[]{3, 3}, 6), new int[]{0, 1}));
        check(Arrays.equals(twoSum(new int[]{-3, 4, 3, 90}, 0), new int[]{0, 2}));
        check(twoSum(new int[]{1}, 2).length == 0);
        check(twoSum(new int[0], 0).length == 0);
        System.out.println("TwoSum OK");
    }

    static void check(boolean ok) { if (!ok) throw new AssertionError("test failed"); }
}
```

*Độ phức tạp:* Time O(n) trung bình (HashMap), space O(n).

*Edge cases / test:* rỗng, 1 phần tử, phần tử trùng `[3,3]`, số âm, không có đáp án.

**Câu hỏi nối tiếp:**
- *Trả về mọi cặp (không trùng lặp)?* — Sort + two pointers, bỏ qua giá trị trùng (giống 3Sum, Q8).
- *Overflow?* — `target - nums[i]` có thể tràn khi giá trị cực trị; dùng `long` cho phép trừ và map `Long`.
- *Dữ liệu là stream hai chiều `add(x)` / `find(sum)`?* — LeetCode 170: map đếm tần suất; trade-off giữa `add` O(1)/`find` O(n) và ngược lại.

**⚠️ Câu trả lời gây điểm trừ:** `put` trước khi tra (trả về `[i, i]` khi `target = 2*nums[i]`); sort mảng gốc rồi trả chỉ số sau khi sort; không hỏi trường hợp không có đáp án.

**📖 Ôn lại:** [2. Quy trình giải bài coding interview](../01-giao-trinh/17-data-structures-algorithms.md#2-quy-trình-giải-bài-coding-interview-của-một-senior) · [3. Arrays, Strings & Hashing](../01-giao-trinh/17-data-structures-algorithms.md#3-arrays-strings--hashing)

</details>

### Q2. 🟢 Ký tự không lặp đầu tiên (First Unique Character — LeetCode 387)

**Đề bài:** Cho chuỗi `s`, trả về chỉ số của ký tự đầu tiên chỉ xuất hiện đúng một lần, không có thì `-1`. Ví dụ: `"loveleetcode"` → `2` (`'v'`). Đây là bài phone-screen rất phổ biến ở các công ty Việt Nam.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Hai lượt: lượt 1 đếm tần suất, lượt 2 tìm ký tự đầu tiên có tần suất 1. Chỉ chữ thường a–z thì dùng `int[26]` (O(1) space); chuỗi Unicode bất kỳ thì đếm theo **code point** bằng `HashMap`.

**Giải thích chi tiết:**

*Câu hỏi làm rõ nên hỏi:* Bảng chữ cái gì — chỉ `a–z`, ASCII, hay Unicode (tiếng Việt có dấu, emoji)? Phân biệt hoa/thường? Trả chỉ số theo `char` hay theo ký tự hiển thị? Chuỗi rất dài/stream?

*Brute force → tối ưu:* Với mỗi ký tự, quét lại cả chuỗi để đếm → O(n²). Đếm trước một lần → O(n). Lưu ý: `"đ"` là một `char` nhưng emoji như `"😀"` là **hai `char`** (surrogate pair) — đếm theo `charAt` sẽ sai.

```java
import java.util.*;

public class FirstUniqueChar {
    /** Giả định chỉ có chữ thường a-z. Time O(n), space O(1). */
    public static int firstUniqChar(String s) {
        int[] count = new int[26];
        for (int i = 0; i < s.length(); i++) count[s.charAt(i) - 'a']++;
        for (int i = 0; i < s.length(); i++) {
            if (count[s.charAt(i) - 'a'] == 1) return i;
        }
        return -1;
    }

    /** Bản tổng quát cho Unicode: đếm theo code point, trả chỉ số char trong String. */
    public static int firstUniqueCodePoint(String s) {
        Map<Integer, Integer> freq = new HashMap<>();
        s.codePoints().forEach(cp -> freq.merge(cp, 1, Integer::sum));
        for (int i = 0; i < s.length(); ) {
            int cp = s.codePointAt(i);
            if (freq.get(cp) == 1) return i;
            i += Character.charCount(cp);
        }
        return -1;
    }

    public static void main(String[] args) {
        check(firstUniqChar("leetcode") == 0);
        check(firstUniqChar("loveleetcode") == 2);
        check(firstUniqChar("aabb") == -1);
        check(firstUniqChar("") == -1);
        check(firstUniqueCodePoint("đaađ") == -1);
        check(firstUniqueCodePoint("😀a😀") == 2);       // emoji chiếm 2 char
        check(firstUniqueCodePoint("việt nam việt") == 5); // 'n'
        System.out.println("FirstUniqueChar OK");
    }

    static void check(boolean ok) { if (!ok) throw new AssertionError("test failed"); }
}
```

*Độ phức tạp:* O(n) time; space O(1) với bảng chữ cái cố định, O(k) với k ký tự khác nhau.

*Edge cases / test:* chuỗi rỗng, mọi ký tự lặp, ký tự duy nhất ở cuối, Unicode/surrogate pair.

**Câu hỏi nối tiếp:**
- *Chuỗi là stream vô hạn, mỗi lúc hỏi "ký tự không lặp đầu tiên hiện tại"?* — `LinkedHashMap`/queue + mảng đếm: đẩy ký tự mới vào queue, khi hỏi thì bỏ đầu queue những ký tự có count > 1 → amortized O(1).
- *Viết bằng Stream API?* — `groupingBy(identity(), LinkedHashMap::new, counting())` rồi tìm entry đầu tiên có value 1 — gọn nhưng chậm hơn mảng đếm.

**⚠️ Câu trả lời gây điểm trừ:** dùng `HashMap` rồi duyệt `keySet()` để tìm "đầu tiên" (HashMap không giữ thứ tự); không hỏi về bảng chữ cái.

**📖 Ôn lại:** [3. Arrays, Strings & Hashing](../01-giao-trinh/17-data-structures-algorithms.md#3-arrays-strings--hashing)

</details>

### Q3. 🟢 Mua bán cổ phiếu một lần (Best Time to Buy and Sell Stock — LeetCode 121)

**Đề bài:** `prices[i]` là giá ngày `i`. Được mua một lần và bán một lần (bán sau mua). Trả về lợi nhuận lớn nhất (0 nếu không có lãi). Ví dụ: `[7,1,5,3,6,4]` → `5`.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Duyệt một lượt, giữ **giá thấp nhất đã gặp**; lợi nhuận nếu bán hôm nay = `price - minSoFar`, cập nhật max. O(n)/O(1). Đây là dạng "Kadane" thu gọn.

**Giải thích chi tiết:**

*Câu hỏi làm rõ nên hỏi:* Không có lãi thì trả 0 hay số âm? Mảng rỗng/1 phần tử? Có cần trả về ngày mua/bán không? Giá có thể âm (không)?

*Brute force → tối ưu:* Thử mọi cặp (i < j) → O(n²). Nhận xét: với mỗi ngày bán, ngày mua tốt nhất là ngày giá thấp nhất **trước đó** → chỉ cần một biến.

```java
public class BestTimeToBuySellStock {
    public static int maxProfit(int[] prices) {
        int minPrice = Integer.MAX_VALUE;
        int best = 0;
        for (int p : prices) {
            minPrice = Math.min(minPrice, p);
            best = Math.max(best, p - minPrice);
        }
        return best;
    }

    /** Follow-up LeetCode 122: được giao dịch nhiều lần -> cộng mọi đoạn tăng. */
    public static int maxProfitUnlimited(int[] prices) {
        int profit = 0;
        for (int i = 1; i < prices.length; i++) profit += Math.max(0, prices[i] - prices[i - 1]);
        return profit;
    }

    public static void main(String[] args) {
        check(maxProfit(new int[]{7, 1, 5, 3, 6, 4}) == 5);
        check(maxProfit(new int[]{7, 6, 4, 3, 1}) == 0);
        check(maxProfit(new int[]{}) == 0);
        check(maxProfit(new int[]{2}) == 0);
        check(maxProfit(new int[]{2, 4, 1}) == 2);          // min mới (1) xuất hiện sau đỉnh
        check(maxProfitUnlimited(new int[]{7, 1, 5, 3, 6, 4}) == 7);
        System.out.println("BestTimeToBuySellStock OK");
    }

    static void check(boolean ok) { if (!ok) throw new AssertionError("test failed"); }
}
```

*Độ phức tạp:* O(n) time, O(1) space.

*Edge cases / test:* giá giảm dần (0), rỗng, min xuất hiện sau max.

**Câu hỏi nối tiếp:**
- *Tối đa k giao dịch (LeetCode 188)?* — DP `buy[j]`, `sell[j]` cho j = 1..k, O(n·k).
- *Có phí giao dịch / cooldown?* — DP trạng thái (giữ cổ phiếu / không giữ / đang cooldown).

**⚠️ Câu trả lời gây điểm trừ:** lấy `max - min` của cả mảng (bỏ qua ràng buộc mua trước bán sau).

**📖 Ôn lại:** [15. Greedy & Intervals](../01-giao-trinh/17-data-structures-algorithms.md#15-greedy--intervals)

</details>

### Q4. 🟡 Nhóm các từ đảo chữ (Group Anagrams — LeetCode 49)

**Đề bài:** Cho mảng chuỗi, nhóm các chuỗi là anagram của nhau. Ví dụ: `["eat","tea","tan","ate","nat","bat"]` → `[["eat","tea","ate"],["tan","nat"],["bat"]]` (thứ tự tùy ý).

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Mỗi chuỗi ánh xạ tới một **khóa chuẩn hóa** giống nhau cho mọi anagram, rồi `computeIfAbsent(key, ...).add(s)`. Khóa = chuỗi đã sort (O(k log k)) hoặc **mảng đếm 26 ký tự** (O(k)).

**Giải thích chi tiết:**

*Câu hỏi làm rõ nên hỏi:* Chỉ chữ thường a–z? Có chuỗi rỗng? Thứ tự output có quan trọng? Phân biệt hoa thường?

*Brute force → tối ưu:* So sánh từng cặp chuỗi (đếm ký tự) → O(n²·k). Dùng khóa chuẩn hóa + HashMap → O(n·k). Lưu ý: không dùng `Arrays.toString(int[])` hay `int[]` trực tiếp làm key — `int[]` dùng `equals` theo identity.

```java
import java.util.*;

public class GroupAnagrams {
    public static List<List<String>> groupAnagrams(String[] strs) {
        Map<String, List<String>> groups = new HashMap<>();
        for (String s : strs) {
            groups.computeIfAbsent(countKey(s), k -> new ArrayList<>()).add(s);
        }
        return new ArrayList<>(groups.values());
    }

    /** Khóa dạng "1#0#0#...#" — O(k), không cần sort. Dấu # tránh nhập nhằng "1,11" vs "11,1". */
    private static String countKey(String s) {
        int[] count = new int[26];
        for (int i = 0; i < s.length(); i++) count[s.charAt(i) - 'a']++;
        StringBuilder sb = new StringBuilder(26 * 2);
        for (int c : count) sb.append(c).append('#');
        return sb.toString();
    }

    public static void main(String[] args) {
        var result = normalize(groupAnagrams(new String[]{"eat", "tea", "tan", "ate", "nat", "bat"}));
        check(result.equals(List.of(List.of("ate", "eat", "tea"), List.of("bat"), List.of("nat", "tan"))));
        check(normalize(groupAnagrams(new String[]{""})).equals(List.of(List.of(""))));
        check(normalize(groupAnagrams(new String[]{"a"})).equals(List.of(List.of("a"))));
        check(groupAnagrams(new String[0]).isEmpty());
        check(normalize(groupAnagrams(new String[]{"ab", "ba", "abb"})).equals(List.of(List.of("ab", "ba"), List.of("abb"))));
        System.out.println("GroupAnagrams OK");
    }

    /** Sort từng nhóm và danh sách nhóm để so sánh không phụ thuộc thứ tự. */
    private static List<List<String>> normalize(List<List<String>> groups) {
        List<List<String>> res = new ArrayList<>();
        for (List<String> g : groups) { List<String> copy = new ArrayList<>(g); Collections.sort(copy); res.add(copy); }
        res.sort(Comparator.comparing((List<String> g) -> g.get(0)));
        return res;
    }

    static void check(boolean ok) { if (!ok) throw new AssertionError("test failed"); }
}
```

*Độ phức tạp:* O(n·k) time với khóa đếm (k = độ dài chuỗi trung bình), O(n·k) space. Khóa sort: O(n·k log k).

*Edge cases / test:* chuỗi rỗng, một phần tử, chuỗi cùng ký tự khác tần suất (`"ab"` vs `"abb"`).

**Câu hỏi nối tiếp:**
- *Unicode?* — Khóa = code points đã sort (`s.codePoints().sorted()`), hoặc `Map<Integer,Integer>` chuẩn hóa thành chuỗi.
- *Viết bằng Stream?* — `Arrays.stream(strs).collect(groupingBy(GroupAnagrams::countKey))`.
- *Hàng tỷ chuỗi phân tán?* — Đây là bài MapReduce: map ra (key, s), shuffle theo key, reduce gom nhóm.

**⚠️ Câu trả lời gây điểm trừ:** dùng `int[]`/`char[]` làm key HashMap (so theo identity → mỗi chuỗi một nhóm); khóa đếm nối số không có dấu phân cách.

**📖 Ôn lại:** [3. Arrays, Strings & Hashing](../01-giao-trinh/17-data-structures-algorithms.md#3-arrays-strings--hashing)

</details>

### Q5. 🟡 K phần tử xuất hiện nhiều nhất (Top K Frequent Elements — LeetCode 347)

**Đề bài:** Cho `nums` và `k`, trả về `k` phần tử có tần suất cao nhất (thứ tự tùy ý). Ví dụ: `[1,1,1,2,2,3], k = 2` → `[1,2]`.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Đếm tần suất bằng HashMap, sau đó: (a) **min-heap kích thước k** → O(n log k), hoặc (b) **bucket sort theo tần suất** (tần suất ≤ n) → O(n). Nói cả hai, chọn theo ràng buộc.

**Giải thích chi tiết:**

*Câu hỏi làm rõ nên hỏi:* Đồng tần suất ở biên thì chọn ai? Đáp án có duy nhất không? `k` có thể lớn hơn số phần tử khác nhau? Thứ tự output? Dữ liệu là stream / không vừa RAM?

*Brute force → tối ưu:* Đếm rồi sort toàn bộ entry theo tần suất → O(n + u log u). Heap k phần tử: chỉ giữ k "ứng viên tốt nhất", đỉnh heap là phần tử **kém nhất** để loại → O(u log k). Bucket: index = tần suất → O(n) nhưng tốn O(n) bộ nhớ cho bucket.

```java
import java.util.*;

public class TopKFrequent {
    /** Bucket sort theo tần suất. Time O(n), space O(n). */
    public static int[] topKFrequent(int[] nums, int k) {
        Map<Integer, Integer> freq = new HashMap<>();
        for (int x : nums) freq.merge(x, 1, Integer::sum);

        List<List<Integer>> buckets = new ArrayList<>(nums.length + 1);
        for (int i = 0; i <= nums.length; i++) buckets.add(new ArrayList<>());
        freq.forEach((value, f) -> buckets.get(f).add(value));

        int[] res = new int[Math.min(k, freq.size())];
        int idx = 0;
        for (int f = nums.length; f >= 1 && idx < res.length; f--) {
            for (int value : buckets.get(f)) {
                if (idx == res.length) break;
                res[idx++] = value;
            }
        }
        return res;
    }

    /** Min-heap kích thước k. Time O(n + u log k), space O(u + k). */
    public static int[] topKHeap(int[] nums, int k) {
        Map<Integer, Integer> freq = new HashMap<>();
        for (int x : nums) freq.merge(x, 1, Integer::sum);
        PriorityQueue<Map.Entry<Integer, Integer>> heap = new PriorityQueue<>(Map.Entry.comparingByValue());
        for (Map.Entry<Integer, Integer> e : freq.entrySet()) {
            heap.offer(e);
            if (heap.size() > k) heap.poll();                 // loại phần tử tần suất thấp nhất
        }
        int[] res = new int[heap.size()];
        for (int i = res.length - 1; i >= 0; i--) res[i] = heap.poll().getKey();
        return res;
    }

    public static void main(String[] args) {
        check(asSet(topKFrequent(new int[]{1, 1, 1, 2, 2, 3}, 2)).equals(Set.of(1, 2)));
        check(asSet(topKHeap(new int[]{1, 1, 1, 2, 2, 3}, 2)).equals(Set.of(1, 2)));
        check(asSet(topKFrequent(new int[]{1}, 1)).equals(Set.of(1)));
        check(asSet(topKFrequent(new int[]{4, 4, -1, -1, -1, 7}, 1)).equals(Set.of(-1)));
        check(topKFrequent(new int[]{5, 6}, 5).length == 2);   // k > số phần tử khác nhau
        check(topKHeap(new int[0], 3).length == 0);
        System.out.println("TopKFrequent OK");
    }

    private static Set<Integer> asSet(int[] a) { Set<Integer> s = new HashSet<>(); for (int x : a) s.add(x); return s; }
    static void check(boolean ok) { if (!ok) throw new AssertionError("test failed"); }
}
```

*Độ phức tạp:* Bucket O(n)/O(n); heap O(n + u log k)/O(u + k) với u = số giá trị khác nhau.

*Edge cases / test:* k = u, k > u, số âm, mảng rỗng.

**Câu hỏi nối tiếp:**
- *Stream vô hạn (top hashtag realtime)?* — Count-Min Sketch + min-heap, hoặc Space-Saving/Misra-Gries với bộ nhớ O(k), chấp nhận sai số; hoặc Redis sorted set `ZINCRBY` + `ZREVRANGE 0 k-1`.
- *Log 100GB không vừa RAM?* — Partition theo `hash(x) % 256` ra file, đếm từng file, top-K cục bộ rồi merge — đúng tuyệt đối vì mỗi giá trị chỉ nằm ở một file.
- *Quickselect?* — Có thể O(u) trung bình trên mảng entry, nhưng worst case O(u²) và khó code hơn.

**⚠️ Câu trả lời gây điểm trừ:** dùng max-heap chứa toàn bộ rồi poll k lần mà nói là O(n log k); comparator `(a, b) -> b - a` (có thể overflow khi giá trị lớn — dùng `Integer.compare`).

**📖 Ôn lại:** [11.2 Pattern Top-K](../01-giao-trinh/17-data-structures-algorithms.md#112-pattern-top-k) · [18.5 Top-K với dữ liệu lớn](../01-giao-trinh/17-data-structures-algorithms.md#185-top-k-frequent-với-streams--dữ-liệu-lớn)

</details>

### Q6. 🟡 Dãy số liên tiếp dài nhất (Longest Consecutive Sequence — LeetCode 128)

**Đề bài:** Cho mảng chưa sort, tìm độ dài dãy số nguyên **liên tiếp** dài nhất (không cần liền kề trong mảng), yêu cầu O(n). Ví dụ: `[100,4,200,1,3,2]` → `4` (1,2,3,4).

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Đưa tất cả vào `HashSet`. Chỉ bắt đầu đếm từ phần tử `x` là **đầu dãy** (`x - 1` không có trong set), rồi đếm `x+1, x+2…`. Mỗi phần tử được "đi qua" tối đa 2 lần → O(n).

**Giải thích chi tiết:**

*Câu hỏi làm rõ nên hỏi:* Có trùng lặp? Mảng rỗng trả 0? Giá trị có thể là `Integer.MIN_VALUE`/`MAX_VALUE` (overflow khi `x+1`)?

*Brute force → tối ưu:* Sort rồi đếm run → O(n log n) — câu trả lời hợp lý đầu tiên nhưng không đạt yêu cầu O(n). Với set: nếu đếm từ **mọi** phần tử thì O(n²) (dãy dài bị đếm lại nhiều lần) → mấu chốt là điều kiện "đầu dãy".

```java
import java.util.*;

public class LongestConsecutive {
    public static int longestConsecutive(int[] nums) {
        Set<Integer> set = new HashSet<>();
        for (int x : nums) set.add(x);
        int best = 0;
        for (int x : set) {
            if (x != Integer.MIN_VALUE && set.contains(x - 1)) continue;   // không phải đầu dãy
            int len = 1;
            // dùng long để x + len không tràn qua Integer.MIN_VALUE
            while ((long) x + len <= Integer.MAX_VALUE && set.contains(x + len)) len++;
            best = Math.max(best, len);
        }
        return best;
    }

    public static void main(String[] args) {
        check(longestConsecutive(new int[]{100, 4, 200, 1, 3, 2}) == 4);
        check(longestConsecutive(new int[]{0, 3, 7, 2, 5, 8, 4, 6, 0, 1}) == 9);
        check(longestConsecutive(new int[]{}) == 0);
        check(longestConsecutive(new int[]{1, 2, 0, 1}) == 3);
        check(longestConsecutive(new int[]{Integer.MAX_VALUE, Integer.MIN_VALUE}) == 1);   // không "vòng" qua overflow
        check(longestConsecutive(new int[]{Integer.MAX_VALUE - 1, Integer.MAX_VALUE}) == 2);
        System.out.println("LongestConsecutive OK");
    }

    static void check(boolean ok) { if (!ok) throw new AssertionError("test failed"); }
}
```

*Độ phức tạp:* O(n) time trung bình, O(n) space.

*Edge cases / test:* rỗng, trùng lặp, số âm, giá trị cực trị (bản ngây thơ đếm `[MAX_VALUE, MIN_VALUE]` thành 2 vì `MAX_VALUE + 1` tràn thành `MIN_VALUE`).

**Câu hỏi nối tiếp:**
- *Hằng số ẩn của HashSet<Integer>?* — Boxing, bộ nhớ ~ 40–50 byte/phần tử; với n = 10⁷ có thể sort `int[]` O(n log n) lại nhanh hơn thực tế. Nói được điều này là điểm cộng.
- *Union-Find?* — Union `x` với `x±1` nếu có; cũng gần O(n) nhưng phức tạp hơn.

**⚠️ Câu trả lời gây điểm trừ:** nói "dùng HashSet là O(n)" nhưng đếm từ mọi phần tử (thực chất O(n²)); bỏ qua overflow.

**📖 Ôn lại:** [3. Arrays, Strings & Hashing](../01-giao-trinh/17-data-structures-algorithms.md#3-arrays-strings--hashing)

</details>

---

<a id="g2"></a>
## 2. Two Pointers & Sliding Window

### Q7. 🟢 Chuỗi đối xứng (Valid Palindrome — LeetCode 125)

**Đề bài:** Kiểm tra chuỗi có đối xứng không sau khi bỏ ký tự không phải chữ/số và không phân biệt hoa thường. Ví dụ: `"A man, a plan, a canal: Panama"` → `true`.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Hai con trỏ từ hai đầu, bỏ qua ký tự không phải chữ/số, so sánh sau khi `toLowerCase`. O(n) time, O(1) space — không tạo chuỗi mới.

**Giải thích chi tiết:**

*Câu hỏi làm rõ nên hỏi:* "Chữ/số" là ASCII hay Unicode (tiếng Việt có dấu)? Chuỗi rỗng/chỉ dấu câu coi là đối xứng? Có được tạo chuỗi phụ không?

*Brute force → tối ưu:* Lọc thành chuỗi mới rồi so với bản đảo ngược → O(n) time nhưng O(n) space. Hai con trỏ giữ O(1).

```java
public class ValidPalindrome {
    public static boolean isPalindrome(String s) {
        int i = 0, j = s.length() - 1;
        while (i < j) {
            char a = s.charAt(i), b = s.charAt(j);
            if (!Character.isLetterOrDigit(a)) { i++; continue; }
            if (!Character.isLetterOrDigit(b)) { j--; continue; }
            if (Character.toLowerCase(a) != Character.toLowerCase(b)) return false;
            i++;
            j--;
        }
        return true;
    }

    /** Follow-up LeetCode 680: được xóa tối đa một ký tự. */
    public static boolean validPalindromeRemoveOne(String s) {
        int i = 0, j = s.length() - 1;
        while (i < j) {
            if (s.charAt(i) != s.charAt(j)) return isRange(s, i + 1, j) || isRange(s, i, j - 1);
            i++;
            j--;
        }
        return true;
    }

    private static boolean isRange(String s, int i, int j) {
        while (i < j) if (s.charAt(i++) != s.charAt(j--)) return false;
        return true;
    }

    public static void main(String[] args) {
        check(isPalindrome("A man, a plan, a canal: Panama"));
        check(!isPalindrome("race a car"));
        check(isPalindrome(" "));
        check(!isPalindrome("0P"));                        // '0' và 'p' khác nhau
        check(isPalindrome("Ăn, nĂ"));                     // Unicode letters vẫn hoạt động
        check(validPalindromeRemoveOne("abca"));
        check(!validPalindromeRemoveOne("abc"));
        System.out.println("ValidPalindrome OK");
    }

    static void check(boolean ok) { if (!ok) throw new AssertionError("test failed"); }
}
```

*Độ phức tạp:* O(n) time, O(1) space.

*Edge cases / test:* chuỗi rỗng, chỉ dấu câu, `"0P"` (chữ số vs chữ), Unicode.

**Câu hỏi nối tiếp:**
- *Chuẩn hóa tiếng Việt "Á" vs "A" + dấu kết hợp?* — `Normalizer.normalize(s, Form.NFC)` trước khi so; nếu muốn bỏ dấu thì NFD rồi lọc `Mn`.
- *Chuỗi con đối xứng dài nhất (LeetCode 5)?* — Expand around center O(n²), Manacher O(n).

**⚠️ Câu trả lời gây điểm trừ:** `replaceAll("[^a-zA-Z0-9]", "")` + `StringBuilder.reverse()` rồi nói là O(1) space.

**📖 Ôn lại:** [4.1 Two pointers](../01-giao-trinh/17-data-structures-algorithms.md#41-two-pointers)

</details>

### Q8. 🟡 Bộ ba có tổng bằng 0 (3Sum — LeetCode 15)

**Đề bài:** Tìm mọi bộ ba **không trùng lặp** `[a, b, c]` trong mảng sao cho `a + b + c = 0`. Ví dụ: `[-1,0,1,2,-1,-4]` → `[[-1,-1,2],[-1,0,1]]`.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Sort, cố định `a[i]`, dùng **two pointers** trên đoạn còn lại để tìm cặp có tổng `-a[i]`; bỏ qua giá trị trùng cho cả `i`, `lo`, `hi`. O(n²) time, O(1) space phụ (không tính output và sort).

**Giải thích chi tiết:**

*Câu hỏi làm rõ nên hỏi:* "Không trùng lặp" theo giá trị hay theo chỉ số? Output cần sort không? Được sửa mảng input không? Kích thước n (n ≤ 3000 → O(n²) ổn)? Có overflow khi cộng ba số?

*Brute force → tối ưu:* Ba vòng lặp + `Set<List<Integer>>` khử trùng → O(n³). Cố định một phần tử + two-sum bằng HashSet → O(n²) nhưng khử trùng rắc rối. Sort + two pointers → O(n²), khử trùng tự nhiên bằng cách nhảy qua giá trị giống nhau.

```java
import java.util.*;

public class ThreeSum {
    public static List<List<Integer>> threeSum(int[] nums) {
        int[] a = nums.clone();                 // không sửa input của caller
        Arrays.sort(a);
        List<List<Integer>> res = new ArrayList<>();
        for (int i = 0; i < a.length - 2; i++) {
            if (a[i] > 0) break;                // phần tử nhỏ nhất > 0 thì không thể tổng = 0
            if (i > 0 && a[i] == a[i - 1]) continue;
            int lo = i + 1, hi = a.length - 1;
            while (lo < hi) {
                long sum = (long) a[i] + a[lo] + a[hi];
                if (sum < 0) lo++;
                else if (sum > 0) hi--;
                else {
                    res.add(List.of(a[i], a[lo], a[hi]));
                    while (lo < hi && a[lo] == a[lo + 1]) lo++;
                    while (lo < hi && a[hi] == a[hi - 1]) hi--;
                    lo++;
                    hi--;
                }
            }
        }
        return res;
    }

    public static void main(String[] args) {
        check(threeSum(new int[]{-1, 0, 1, 2, -1, -4}).equals(List.of(List.of(-1, -1, 2), List.of(-1, 0, 1))));
        check(threeSum(new int[]{0, 1, 1}).isEmpty());
        check(threeSum(new int[]{0, 0, 0, 0}).equals(List.of(List.of(0, 0, 0))));
        check(threeSum(new int[]{}).isEmpty());
        check(threeSum(new int[]{-2, 0, 1, 1, 2}).equals(List.of(List.of(-2, 0, 2), List.of(-2, 1, 1))));
        System.out.println("ThreeSum OK");
    }

    static void check(boolean ok) { if (!ok) throw new AssertionError("test failed"); }
}
```

*Độ phức tạp:* O(n²) time; sort O(n log n) bị lấn át; space O(log n) cho sort primitive (Dual-Pivot Quicksort) + O(n) cho bản clone.

*Edge cases / test:* toàn 0, ít hơn 3 phần tử, nhiều giá trị trùng, không có đáp án.

**Câu hỏi nối tiếp:**
- *3Sum Closest / 4Sum?* — Cùng khung; kSum tổng quát bằng đệ quy giảm về 2Sum, O(n^(k-1)).
- *Vì sao không dùng `HashSet<List<Integer>>` khử trùng?* — Đúng nhưng tốn bộ nhớ và hằng số lớn; interviewer muốn thấy bạn khử trùng bằng cấu trúc đã sort.

**⚠️ Câu trả lời gây điểm trừ:** quên nhảy qua phần tử trùng (output lặp), hoặc chỉ nhảy trùng ở `i` mà quên `lo`/`hi`.

**📖 Ôn lại:** [4.1 Two pointers](../01-giao-trinh/17-data-structures-algorithms.md#41-two-pointers)

</details>

### Q9. 🟡 Chuỗi con dài nhất không lặp ký tự (LeetCode 3)

**Đề bài:** Tìm độ dài chuỗi con liên tiếp dài nhất không có ký tự lặp. Ví dụ: `"abcabcbb"` → `3` (`"abc"`), `"pwwkew"` → `3` (`"wke"`).

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **Sliding window** co giãn: con trỏ phải mở rộng; lưu vị trí xuất hiện gần nhất của mỗi ký tự; khi gặp ký tự đã có **trong cửa sổ**, nhảy con trỏ trái tới `lastSeen + 1`. O(n).

**Giải thích chi tiết:**

*Câu hỏi làm rõ nên hỏi:* Bộ ký tự (ASCII → `int[128]`; Unicode → `HashMap`)? Phân biệt hoa thường? Trả độ dài hay cả chuỗi con?

*Brute force → tối ưu:* Thử mọi chuỗi con và kiểm tra trùng → O(n³) (hoặc O(n²) với set). Dấu hiệu "chuỗi con liên tiếp dài nhất thỏa điều kiện" → sliding window. Bẫy: ký tự đã thấy nhưng nằm **ngoài** cửa sổ (`"abba"`) — không được kéo `left` lùi lại.

```java
import java.util.*;

public class LongestSubstringNoRepeat {
    public static int lengthOfLongestSubstring(String s) {
        Map<Character, Integer> lastSeen = new HashMap<>();
        int best = 0, left = 0;
        for (int right = 0; right < s.length(); right++) {
            char c = s.charAt(right);
            Integer prev = lastSeen.get(c);
            if (prev != null && prev >= left) left = prev + 1;   // chỉ nhảy khi ký tự lặp nằm TRONG cửa sổ
            lastSeen.put(c, right);
            best = Math.max(best, right - left + 1);
        }
        return best;
    }

    public static void main(String[] args) {
        check(lengthOfLongestSubstring("abcabcbb") == 3);
        check(lengthOfLongestSubstring("bbbbb") == 1);
        check(lengthOfLongestSubstring("pwwkew") == 3);
        check(lengthOfLongestSubstring("") == 0);
        check(lengthOfLongestSubstring("abba") == 2);        // bẫy: 'a' cũ nằm ngoài cửa sổ
        check(lengthOfLongestSubstring(" ") == 1);
        check(lengthOfLongestSubstring("dvdf") == 3);
        System.out.println("LongestSubstringNoRepeat OK");
    }

    static void check(boolean ok) { if (!ok) throw new AssertionError("test failed"); }
}
```

*Độ phức tạp:* O(n) time; O(min(n, Σ)) space với Σ là kích thước bảng ký tự.

*Edge cases / test:* rỗng, toàn ký tự giống nhau, `"abba"`, `"dvdf"`, khoảng trắng.

**Câu hỏi nối tiếp:**
- *Tối đa k ký tự khác nhau (LeetCode 340)?* — Window với map đếm, co `left` khi `map.size() > k`.
- *Trả về chuỗi con?* — Lưu `bestStart` khi cập nhật `best`, cuối cùng `substring`.

**⚠️ Câu trả lời gây điểm trừ:** `left = prev + 1` không kiểm tra `prev >= left` (sai với `"abba"`); dùng `Set` và xóa từng ký tự nhưng không giải thích được vì sao vẫn O(n).

**📖 Ôn lại:** [4.2 Sliding window](../01-giao-trinh/17-data-structures-algorithms.md#42-sliding-window)

</details>

### Q10. 🔴 Cửa sổ nhỏ nhất chứa đủ ký tự (Minimum Window Substring — LeetCode 76)

**Đề bài:** Cho `s` và `t`, tìm chuỗi con ngắn nhất của `s` chứa **mọi** ký tự của `t` (kể cả số lần lặp). Không có thì trả `""`. Ví dụ: `s = "ADOBECODEBANC", t = "ABC"` → `"BANC"`.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Sliding window với mảng `need[c]` và biến `missing` (số ký tự còn thiếu). Mở rộng `right` cho tới khi `missing == 0`, rồi **co `left`** hết mức có thể trong khi vẫn hợp lệ, cập nhật đáp án. Mỗi con trỏ đi tối đa n bước → O(|s| + |t|).

**Giải thích chi tiết:**

*Câu hỏi làm rõ nên hỏi:* Ký tự trong `t` có lặp (`"AA"` cần hai `A`)? Phân biệt hoa thường? Bảng ký tự (ASCII → mảng 128)? Nhiều đáp án cùng độ dài thì trả cái nào (cái đầu tiên)?

*Brute force → tối ưu:* Mọi chuỗi con O(n²) × kiểm tra O(n) → O(n³). Mấu chốt: hàm "cửa sổ hợp lệ" **đơn điệu** — mở rộng thì vẫn hợp lệ, nên với mỗi `right` chỉ cần co `left` tiến về phía trước, không bao giờ lùi. Biến `missing` giúp kiểm tra hợp lệ O(1) thay vì so 128 ô.

```java
public class MinimumWindowSubstring {
    public static String minWindow(String s, String t) {
        if (t.isEmpty() || s.length() < t.length()) return "";
        int[] need = new int[128];                 // giả định ASCII; Unicode -> HashMap
        for (int i = 0; i < t.length(); i++) need[t.charAt(i)]++;
        int missing = t.length();
        int left = 0, bestStart = 0, bestLen = Integer.MAX_VALUE;

        for (int right = 0; right < s.length(); right++) {
            if (need[s.charAt(right)]-- > 0) missing--;          // ký tự này đang được cần
            while (missing == 0) {                                 // cửa sổ hợp lệ -> co trái
                if (right - left + 1 < bestLen) {
                    bestLen = right - left + 1;
                    bestStart = left;
                }
                if (++need[s.charAt(left)] > 0) missing++;        // bỏ ký tự cần thiết -> không còn hợp lệ
                left++;
            }
        }
        return bestLen == Integer.MAX_VALUE ? "" : s.substring(bestStart, bestStart + bestLen);
    }

    public static void main(String[] args) {
        check(minWindow("ADOBECODEBANC", "ABC").equals("BANC"));
        check(minWindow("a", "a").equals("a"));
        check(minWindow("a", "aa").equals(""));
        check(minWindow("ab", "b").equals("b"));
        check(minWindow("aa", "aa").equals("aa"));
        check(minWindow("abc", "").equals(""));
        check(minWindow("bba", "ab").equals("ba"));
        System.out.println("MinimumWindowSubstring OK");
    }

    static void check(boolean ok) { if (!ok) throw new AssertionError("test failed"); }
}
```

*Độ phức tạp:* O(|s| + |t|) time, O(Σ) space.

*Edge cases / test:* `t` dài hơn `s`, `t` có ký tự lặp, `t` rỗng, đáp án ở cuối chuỗi.

**Câu hỏi nối tiếp:**
- *Mẫu chung của sliding window "tìm cửa sổ ngắn nhất"?* — Mở rộng phải cho tới khi hợp lệ, co trái khi còn hợp lệ, cập nhật đáp án **trong** vòng co. "Dài nhất" thì ngược lại: co khi **không** hợp lệ, cập nhật sau vòng co.
- *Permutation in String (LeetCode 567)?* — Cửa sổ kích thước cố định |t| với cùng kỹ thuật đếm.

**⚠️ Câu trả lời gây điểm trừ:** so sánh hai mảng 128 ô ở mỗi bước mà vẫn nói O(n) mà không giải thích hằng số; dùng `s.substring` trong vòng lặp (tạo chuỗi mới mỗi lần).

**📖 Ôn lại:** [4.2 Sliding window](../01-giao-trinh/17-data-structures-algorithms.md#42-sliding-window)

</details>

### Q11. 🔴 Hứng nước mưa (Trapping Rain Water — LeetCode 42)

**Đề bài:** Mảng `height` biểu diễn các cột rộng 1. Tính lượng nước đọng lại sau mưa. Ví dụ: `[0,1,0,2,1,0,1,3,2,1,2,1]` → `6`.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Nước tại cột i = `min(maxLeft[i], maxRight[i]) - h[i]`. Tối ưu bằng **two pointers**: luôn xử lý phía có cột thấp hơn — vì khi `h[left] < h[right]` thì `min(...)` tại `left` chắc chắn bị chặn bởi `leftMax`. O(n) time, O(1) space.

**Giải thích chi tiết:**

*Câu hỏi làm rõ nên hỏi:* Chiều cao âm? Mảng rỗng/ít hơn 3 cột? Tổng nước có thể vượt `int`?

*Brute force → tối ưu:*
1. Với mỗi cột, quét trái/phải tìm max → O(n²).
2. Tiền xử lý hai mảng `maxLeft`, `maxRight` → O(n) time, O(n) space.
3. Two pointers bỏ hai mảng: tại mỗi bước biết chắc phía thấp hơn bị giới hạn bởi max của chính phía đó → O(1) space.
4. (Cách khác) monotonic stack tính nước theo "tầng ngang".

```java
public class TrappingRainWater {
    public static long trap(int[] h) {
        int left = 0, right = h.length - 1;
        int leftMax = 0, rightMax = 0;
        long water = 0;                                // tổng có thể vượt int với input lớn
        while (left < right) {
            if (h[left] < h[right]) {
                leftMax = Math.max(leftMax, h[left]);
                water += leftMax - h[left];           // bên phải chắc chắn có cột >= h[right] > h[left]
                left++;
            } else {
                rightMax = Math.max(rightMax, h[right]);
                water += rightMax - h[right];
                right--;
            }
        }
        return water;
    }

    /** Bản O(n) space dễ giải thích, dùng để đối chiếu. */
    static long trapPrefix(int[] h) {
        int n = h.length;
        if (n == 0) return 0;
        int[] maxLeft = new int[n], maxRight = new int[n];
        maxLeft[0] = h[0];
        for (int i = 1; i < n; i++) maxLeft[i] = Math.max(maxLeft[i - 1], h[i]);
        maxRight[n - 1] = h[n - 1];
        for (int i = n - 2; i >= 0; i--) maxRight[i] = Math.max(maxRight[i + 1], h[i]);
        long water = 0;
        for (int i = 0; i < n; i++) water += Math.min(maxLeft[i], maxRight[i]) - h[i];
        return water;
    }

    public static void main(String[] args) {
        check(trap(new int[]{0, 1, 0, 2, 1, 0, 1, 3, 2, 1, 2, 1}) == 6);
        check(trap(new int[]{4, 2, 0, 3, 2, 5}) == 9);
        check(trap(new int[]{}) == 0);
        check(trap(new int[]{5}) == 0);
        check(trap(new int[]{3, 3, 3}) == 0);
        check(trap(new int[]{5, 4, 3, 2, 1}) == 0);
        java.util.Random rnd = new java.util.Random(7);
        for (int iter = 0; iter < 1000; iter++) {          // đối chiếu ngẫu nhiên với bản prefix
            int[] h = new int[rnd.nextInt(30)];
            for (int i = 0; i < h.length; i++) h[i] = rnd.nextInt(10);
            check(trap(h) == trapPrefix(h));
        }
        System.out.println("TrappingRainWater OK");
    }

    static void check(boolean ok) { if (!ok) throw new AssertionError("test failed"); }
}
```

*Độ phức tạp:* O(n) time, O(1) space.

*Edge cases / test:* rỗng, đơn điệu tăng/giảm (0), mặt phẳng, cột cao ở giữa.

**Câu hỏi nối tiếp:**
- *Bản 2D (LeetCode 407)?* — Min-heap từ viền vào trong (BFS theo độ cao), O(mn log(mn)).
- *Chứng minh two pointers đúng?* — Khi `h[left] < h[right]`, mọi cột bên phải `left` có ít nhất một cột cao ≥ `h[right]` > `h[left]` ≥ ... nên mức nước tại `left` chỉ phụ thuộc `leftMax`.

**⚠️ Câu trả lời gây điểm trừ:** nhảy thẳng vào two pointers mà không giải thích được vì sao đúng; bỏ qua bước O(n) space trung gian.

**📖 Ôn lại:** [4.1 Two pointers](../01-giao-trinh/17-data-structures-algorithms.md#41-two-pointers) · [8.3 Monotonic stack](../01-giao-trinh/17-data-structures-algorithms.md#83-monotonic-stack)

</details>

---

<a id="g3"></a>
## 3. Prefix Sum & Binary Search

### Q12. 🟡 Số mảng con có tổng bằng K (Subarray Sum Equals K — LeetCode 560)

**Đề bài:** Đếm số mảng con **liên tiếp** có tổng bằng `k`. Mảng có thể chứa số âm. Ví dụ: `[1,1,1], k = 2` → `2`.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **Prefix sum + HashMap đếm**: tổng đoạn `(j, i]` = `prefix[i] - prefix[j]`; với mỗi `i`, số đoạn kết thúc tại `i` có tổng `k` = số lần `prefix[i] - k` đã xuất hiện. Khởi tạo `count[0] = 1`. O(n).

**Giải thích chi tiết:**

*Câu hỏi làm rõ nên hỏi:* Có số âm/số 0 không (quyết định có dùng sliding window được hay không)? Tổng có thể tràn `int`? Đếm số đoạn hay trả về các đoạn?

*Brute force → tối ưu:* Mọi cặp (i, j) và cộng dồn → O(n²). Sliding window **sai** khi có số âm vì tổng không đơn điệu khi mở rộng/co cửa sổ — đây là bẫy kinh điển. Prefix sum biến "tổng đoạn" thành "hiệu hai prefix" → bài toán two-sum trên prefix.

```java
import java.util.*;

public class SubarraySumEqualsK {
    public static int subarraySum(int[] nums, int k) {
        Map<Long, Integer> countOfPrefix = new HashMap<>();
        countOfPrefix.put(0L, 1);                         // prefix rỗng: cho phép đoạn bắt đầu từ index 0
        long prefix = 0;
        int count = 0;
        for (int x : nums) {
            prefix += x;
            count += countOfPrefix.getOrDefault(prefix - k, 0);
            countOfPrefix.merge(prefix, 1, Integer::sum);  // put SAU khi tra
        }
        return count;
    }

    static int bruteForce(int[] nums, int k) {
        int count = 0;
        for (int i = 0; i < nums.length; i++) {
            long sum = 0;
            for (int j = i; j < nums.length; j++) if ((sum += nums[j]) == k) count++;
        }
        return count;
    }

    public static void main(String[] args) {
        check(subarraySum(new int[]{1, 1, 1}, 2) == 2);
        check(subarraySum(new int[]{1, 2, 3}, 3) == 2);
        check(subarraySum(new int[]{1, -1, 0}, 0) == 3);
        check(subarraySum(new int[]{}, 0) == 0);
        check(subarraySum(new int[]{3, 4, 7, 2, -3, 1, 4, 2}, 7) == 4);
        Random rnd = new Random(42);
        for (int iter = 0; iter < 500; iter++) {
            int[] a = new int[rnd.nextInt(20)];
            for (int i = 0; i < a.length; i++) a[i] = rnd.nextInt(11) - 5;
            int k = rnd.nextInt(11) - 5;
            check(subarraySum(a, k) == bruteForce(a, k));
        }
        System.out.println("SubarraySumEqualsK OK");
    }

    static void check(boolean ok) { if (!ok) throw new AssertionError("test failed"); }
}
```

*Độ phức tạp:* O(n) time trung bình, O(n) space.

*Edge cases / test:* số âm, số 0 (nhiều đoạn chồng nhau), k = 0, mảng rỗng.

**Câu hỏi nối tiếp:**
- *Chỉ số dương?* — Sliding window O(1) space được.
- *Tổng chia hết cho k (LeetCode 974/523)?* — Map theo `prefix mod k` (chú ý mod âm trong Java: `((p % k) + k) % k`).
- *Nhiều truy vấn tổng đoạn [l, r]?* — Mảng prefix O(1)/truy vấn; có cập nhật → Fenwick/Segment tree.

**⚠️ Câu trả lời gây điểm trừ:** dùng sliding window khi có số âm; quên `countOfPrefix.put(0, 1)` (bỏ sót đoạn bắt đầu từ đầu mảng).

**📖 Ôn lại:** [5. Prefix Sum & Difference Array](../01-giao-trinh/17-data-structures-algorithms.md#5-prefix-sum--difference-array)

</details>

### Q13. 🟡 Tìm kiếm trong mảng sort bị xoay (Search in Rotated Sorted Array — LeetCode 33)

**Đề bài:** Mảng tăng dần, các phần tử **khác nhau**, đã bị xoay tại một điểm chưa biết (vd `[4,5,6,7,0,1,2]`). Tìm chỉ số của `target` trong O(log n), không có thì `-1`.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Binary search biến thể: tại mỗi bước, **ít nhất một nửa** `[lo..mid]` hoặc `[mid..hi]` đã sort. Xác định nửa nào sort (so `a[lo] <= a[mid]`), kiểm tra `target` có nằm trong khoảng của nửa đó không để quyết định đi trái hay phải.

**Giải thích chi tiết:**

*Câu hỏi làm rõ nên hỏi:* Có phần tử trùng không (nếu có, worst case thành O(n) — LeetCode 81)? Mảng có thể không bị xoay? Rỗng?

*Brute force → tối ưu:* Quét tuyến tính O(n). Hoặc tìm điểm xoay bằng binary search rồi binary search thường trên nửa đúng — 2 lần O(log n), đúng nhưng dài hơn. Một lần binary search với điều kiện "nửa nào đã sort" gọn nhất.

```java
import java.util.*;

public class SearchRotatedArray {
    public static int search(int[] a, int target) {
        int lo = 0, hi = a.length - 1;
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;                   // tránh overflow của (lo + hi) / 2
            if (a[mid] == target) return mid;
            if (a[lo] <= a[mid]) {                          // nửa trái [lo..mid] đã sort
                if (a[lo] <= target && target < a[mid]) hi = mid - 1;
                else lo = mid + 1;
            } else {                                        // nửa phải [mid..hi] đã sort
                if (a[mid] < target && target <= a[hi]) lo = mid + 1;
                else hi = mid - 1;
            }
        }
        return -1;
    }

    public static void main(String[] args) {
        check(search(new int[]{4, 5, 6, 7, 0, 1, 2}, 0) == 4);
        check(search(new int[]{4, 5, 6, 7, 0, 1, 2}, 3) == -1);
        check(search(new int[]{1}, 0) == -1);
        check(search(new int[]{1}, 1) == 0);
        check(search(new int[]{3, 1}, 1) == 1);
        check(search(new int[]{5, 1, 3}, 5) == 0);
        check(search(new int[]{}, 5) == -1);
        Random rnd = new Random(1);
        for (int iter = 0; iter < 300; iter++) {            // mọi phép xoay của mảng ngẫu nhiên không trùng
            int n = 1 + rnd.nextInt(15);
            TreeSet<Integer> values = new TreeSet<>();
            while (values.size() < n) values.add(rnd.nextInt(100) - 50);
            List<Integer> sorted = new ArrayList<>(values);
            int shift = rnd.nextInt(n);
            int[] a = new int[n];
            for (int i = 0; i < n; i++) a[i] = sorted.get((i + shift) % n);
            for (int t = -51; t <= 50; t++) {
                int idx = search(a, t);
                check(values.contains(t) ? a[idx] == t : idx == -1);
            }
        }
        System.out.println("SearchRotatedArray OK");
    }

    static void check(boolean ok) { if (!ok) throw new AssertionError("test failed"); }
}
```

*Độ phức tạp:* O(log n) time, O(1) space.

*Edge cases / test:* 1–2 phần tử, không xoay, target ở điểm xoay, target ở hai đầu — test đối chiếu ngẫu nhiên trên mọi phép xoay.

**Câu hỏi nối tiếp:**
- *Có phần tử trùng?* — Khi `a[lo] == a[mid] == a[hi]` không biết nửa nào sort → `lo++`, `hi--`; worst case O(n).
- *Tìm phần tử nhỏ nhất (LeetCode 153)?* — So `a[mid]` với `a[hi]`: lớn hơn thì min ở bên phải.
- *Template binary search an toàn?* — Định nghĩa rõ bất biến (`[lo, hi]` đóng hay `[lo, hi)` nửa mở) và giữ nhất quán; `>>> 1` cũng tránh overflow.

**⚠️ Câu trả lời gây điểm trừ:** `a[lo] < a[mid]` (thiếu dấu `=` → sai khi `lo == mid`); `(lo + hi) / 2` với chỉ số lớn.

**📖 Ôn lại:** [6. Binary Search](../01-giao-trinh/17-data-structures-algorithms.md#6-binary-search-kể-cả-trên-không-gian-đáp-án)

</details>

### Q14. 🟡 Koko ăn chuối (Koko Eating Bananas — LeetCode 875) — binary search trên không gian đáp án

**Đề bài:** Có `piles[i]` quả chuối mỗi đống, `h` giờ. Mỗi giờ Koko chọn một đống và ăn tối đa `speed` quả (đống ít hơn thì ăn hết, giờ đó không ăn đống khác). Tìm `speed` nhỏ nhất để ăn hết trong `h` giờ. Ví dụ: `piles = [3,6,7,11], h = 8` → `4`.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Vị từ "ăn hết trong h giờ với tốc độ s" là **đơn điệu** theo s (s lớn hơn thì càng dễ). Binary search s trên `[1, max(piles)]`, tìm s nhỏ nhất thỏa. Mỗi lần kiểm tra O(n) → tổng O(n log max).

**Giải thích chi tiết:**

*Câu hỏi làm rõ nên hỏi:* `h >= piles.length` (nếu không thì vô nghiệm)? Giá trị đống tới 10⁹ (tổng giờ có thể tràn `int`)? Đây là dạng "minimum of maximum / tốc độ tối thiểu" → dấu hiệu binary search on answer.

*Brute force → tối ưu:* Thử s = 1, 2, 3… → O(max · n), quá chậm với max = 10⁹. Vì vị từ đơn điệu, binary search lower bound trên không gian đáp án.

```java
import java.util.*;

public class KokoEatingBananas {
    public static int minEatingSpeed(int[] piles, int h) {
        int lo = 1, hi = 1;
        for (int p : piles) hi = Math.max(hi, p);          // tốc độ max(piles) luôn đủ nếu h >= n
        while (lo < hi) {                                   // tìm s nhỏ nhất thỏa canFinish (nửa mở [lo, hi])
            int mid = lo + (hi - lo) / 2;
            if (hoursNeeded(piles, mid) <= h) hi = mid;
            else lo = mid + 1;
        }
        return lo;
    }

    private static long hoursNeeded(int[] piles, int speed) {
        long hours = 0;                                     // tổng giờ có thể vượt int
        for (int p : piles) hours += (p + (long) speed - 1) / speed;   // ceil(p / speed) không dùng double
        return hours;
    }

    static int bruteForce(int[] piles, int h) {
        for (int s = 1; ; s++) if (hoursNeeded(piles, s) <= h) return s;
    }

    public static void main(String[] args) {
        check(minEatingSpeed(new int[]{3, 6, 7, 11}, 8) == 4);
        check(minEatingSpeed(new int[]{30, 11, 23, 4, 20}, 5) == 30);
        check(minEatingSpeed(new int[]{30, 11, 23, 4, 20}, 6) == 23);
        check(minEatingSpeed(new int[]{1_000_000_000}, 2) == 500_000_000);
        check(minEatingSpeed(new int[]{312884470}, 312884469) == 2);
        Random rnd = new Random(3);
        for (int iter = 0; iter < 300; iter++) {
            int[] piles = new int[1 + rnd.nextInt(6)];
            for (int i = 0; i < piles.length; i++) piles[i] = 1 + rnd.nextInt(30);
            int h = piles.length + rnd.nextInt(40);
            check(minEatingSpeed(piles, h) == bruteForce(piles, h));
        }
        System.out.println("KokoEatingBananas OK");
    }

    static void check(boolean ok) { if (!ok) throw new AssertionError("test failed"); }
}
```

*Độ phức tạp:* O(n · log(max)) time, O(1) space.

*Edge cases / test:* `h == n` (đáp án = max), một đống rất lớn, `h` cực lớn (đáp án = 1).

**Câu hỏi nối tiếp:**
- *Bài cùng họ?* — Capacity to Ship Packages (1011), Split Array Largest Sum (410), Minimize Max Distance to Gas Station — đều "binary search trên đáp án + kiểm tra greedy O(n)".
- *Thực tế?* — Chọn số worker/throughput tối thiểu để xử lý backlog trong SLA, chọn kích thước batch.

**⚠️ Câu trả lời gây điểm trừ:** dùng `Math.ceil((double) p / speed)` (sai số và chậm); tổng giờ bằng `int` (tràn với input lớn); không chứng minh được vị từ đơn điệu.

**📖 Ôn lại:** [6.2 Binary search trên không gian đáp án](../01-giao-trinh/17-data-structures-algorithms.md#62-binary-search-trên-không-gian-đáp-án)

</details>

---

<a id="g4"></a>
## 4. Stack, Monotonic Stack/Deque

### Q15. 🟢 Dấu ngoặc hợp lệ (Valid Parentheses — LeetCode 20)

**Đề bài:** Chuỗi chỉ gồm `()[]{}`. Kiểm tra mọi ngoặc mở được đóng đúng loại, đúng thứ tự. Ví dụ: `"([]{})"` → `true`, `"([)]"` → `false`.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Stack: gặp ngoặc mở thì **push ngoặc đóng tương ứng**; gặp ngoặc đóng thì stack phải không rỗng và `pop()` phải bằng nó; cuối cùng stack rỗng. Dùng `ArrayDeque`, không dùng `java.util.Stack`.

**Giải thích chi tiết:**

*Câu hỏi làm rõ nên hỏi:* Chuỗi có ký tự khác (chữ, số) — bỏ qua hay coi là không hợp lệ? Chuỗi rỗng là hợp lệ? Độ dài rất lớn?

*Brute force → tối ưu:* Lặp `replace("()", "")`… tới khi không đổi → O(n²). Stack O(n). Mẹo push ngoặc đóng giúp so sánh trực tiếp, không cần map.

```java
import java.util.*;

public class ValidParentheses {
    public static boolean isValid(String s) {
        if (s.length() % 2 == 1) return false;              // cắt sớm
        Deque<Character> stack = new ArrayDeque<>();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '(' -> stack.push(')');
                case '[' -> stack.push(']');
                case '{' -> stack.push('}');
                default -> {
                    if (stack.isEmpty() || stack.pop() != c) return false;
                }
            }
        }
        return stack.isEmpty();
    }

    public static void main(String[] args) {
        check(isValid("()"));
        check(isValid("()[]{}"));
        check(!isValid("(]"));
        check(!isValid("([)]"));
        check(isValid("{[]}"));
        check(isValid(""));
        check(!isValid("(("));
        check(!isValid("))"));
        check(!isValid("(){"));
        System.out.println("ValidParentheses OK");
    }

    static void check(boolean ok) { if (!ok) throw new AssertionError("test failed"); }
}
```

*Độ phức tạp:* O(n) time, O(n) space.

*Edge cases / test:* rỗng, chỉ ngoặc đóng, chỉ ngoặc mở, độ dài lẻ, lồng sai loại.

**Câu hỏi nối tiếp:**
- *Chỉ có một loại ngoặc?* — Một biến đếm `balance`, O(1) space; không được âm giữa chừng.
- *Vì sao không dùng `java.util.Stack`?* — Kế thừa `Vector`, mọi method `synchronized`, lại lộ API truy cập theo index; `ArrayDeque` nhanh hơn và là khuyến nghị của Javadoc.
- *Minimum Remove to Make Valid (LeetCode 1249)?* — Stack lưu chỉ số ngoặc mở chưa khớp.

**⚠️ Câu trả lời gây điểm trừ:** quên kiểm tra stack rỗng trước `pop()` (ném `NoSuchElementException`); quên kiểm tra stack rỗng ở cuối.

**📖 Ôn lại:** [8. Stack, Queue, Deque](../01-giao-trinh/17-data-structures-algorithms.md#8-stack-queue-deque-monotonic-stackqueue)

</details>

### Q16. 🟡 Bao nhiêu ngày nữa thì ấm hơn (Daily Temperatures — LeetCode 739)

**Đề bài:** Với mỗi ngày `i`, cho biết phải đợi bao nhiêu ngày để có nhiệt độ **cao hơn**; không có thì 0. Ví dụ: `[73,74,75,71,69,72,76,73]` → `[1,1,4,2,1,1,0,0]`.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **Monotonic stack** giảm dần chứa chỉ số các ngày "chưa tìm được ngày ấm hơn". Gặp ngày `i` ấm hơn đỉnh stack thì pop và ghi `ans[j] = i - j`. Mỗi chỉ số push/pop một lần → O(n).

**Giải thích chi tiết:**

*Câu hỏi làm rõ nên hỏi:* "Cao hơn" là strictly greater? Phạm vi nhiệt độ (nhỏ → có thể dùng mảng "next occurrence" theo giá trị)? Mảng rỗng?

*Brute force → tối ưu:* Với mỗi ngày quét về phải → O(n²) (n = 10⁵ thì quá chậm). Dấu hiệu "phần tử lớn hơn tiếp theo" → monotonic stack.

```java
import java.util.*;

public class DailyTemperatures {
    public static int[] dailyTemperatures(int[] t) {
        int[] ans = new int[t.length];
        Deque<Integer> stack = new ArrayDeque<>();          // chỉ số, nhiệt độ giảm dần từ đáy lên đỉnh
        for (int i = 0; i < t.length; i++) {
            while (!stack.isEmpty() && t[stack.peek()] < t[i]) {
                int j = stack.pop();
                ans[j] = i - j;
            }
            stack.push(i);
        }
        return ans;                                          // phần tử còn trong stack giữ giá trị 0
    }

    public static void main(String[] args) {
        check(Arrays.equals(dailyTemperatures(new int[]{73, 74, 75, 71, 69, 72, 76, 73}), new int[]{1, 1, 4, 2, 1, 1, 0, 0}));
        check(Arrays.equals(dailyTemperatures(new int[]{30, 40, 50, 60}), new int[]{1, 1, 1, 0}));
        check(Arrays.equals(dailyTemperatures(new int[]{30, 60, 90}), new int[]{1, 1, 0}));
        check(Arrays.equals(dailyTemperatures(new int[]{5, 5, 5}), new int[]{0, 0, 0}));   // bằng nhau không tính
        check(dailyTemperatures(new int[0]).length == 0);
        System.out.println("DailyTemperatures OK");
    }

    static void check(boolean ok) { if (!ok) throw new AssertionError("test failed"); }
}
```

*Độ phức tạp:* O(n) time (amortized: mỗi chỉ số vào/ra stack đúng một lần), O(n) space.

*Edge cases / test:* tăng dần, giảm dần, giá trị bằng nhau, rỗng.

**Câu hỏi nối tiếp:**
- *Next Greater Element trên mảng vòng (LeetCode 503)?* — Duyệt `2n` lần với chỉ số `i % n`.
- *Largest Rectangle in Histogram (LeetCode 84)?* — Monotonic stack tăng dần, khi pop tính diện tích với biên trái là phần tử dưới nó.
- *Ứng dụng thực tế?* — "Khi nào giá vượt mức hiện tại", stock span, phân tích chuỗi thời gian metric.

**⚠️ Câu trả lời gây điểm trừ:** stack chứa **giá trị** thay vì chỉ số (không tính được khoảng cách); nói độ phức tạp O(n²) vì có vòng `while` lồng.

**📖 Ôn lại:** [8.3 Monotonic stack](../01-giao-trinh/17-data-structures-algorithms.md#83-monotonic-stack)

</details>

### Q17. 🔴 Giá trị lớn nhất trong cửa sổ trượt (Sliding Window Maximum — LeetCode 239)

**Đề bài:** Cho `nums` và `k`, trả về max của mọi cửa sổ liên tiếp độ dài `k`. Ví dụ: `[1,3,-1,-3,5,3,6,7], k = 3` → `[3,3,5,5,6,7]`.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **Monotonic deque** chứa chỉ số với giá trị **giảm dần**: đầu deque luôn là max của cửa sổ. Mỗi bước: bỏ chỉ số đã ra khỏi cửa sổ ở đầu; bỏ ở cuối mọi chỉ số có giá trị ≤ phần tử mới (chúng không bao giờ là max nữa); thêm chỉ số mới. O(n).

**Giải thích chi tiết:**

*Câu hỏi làm rõ nên hỏi:* `1 ≤ k ≤ n`? `k > n` thì sao? Mảng rỗng? Dữ liệu là stream (trả kết quả online)?

*Brute force → tối ưu:*
1. Mỗi cửa sổ quét k phần tử → O(n·k).
2. `TreeMap`/heap với lazy deletion → O(n log k).
3. Monotonic deque → O(n), mỗi chỉ số vào/ra một lần.

```java
import java.util.*;

public class SlidingWindowMaximum {
    public static int[] maxSlidingWindow(int[] nums, int k) {
        if (k <= 0 || k > nums.length) {
            if (nums.length == 0) return new int[0];
            throw new IllegalArgumentException("k must be in [1, n]");
        }
        int[] res = new int[nums.length - k + 1];
        Deque<Integer> dq = new ArrayDeque<>();            // chỉ số; nums tương ứng giảm dần
        for (int i = 0; i < nums.length; i++) {
            if (!dq.isEmpty() && dq.peekFirst() <= i - k) dq.pollFirst();         // ra khỏi cửa sổ
            while (!dq.isEmpty() && nums[dq.peekLast()] <= nums[i]) dq.pollLast(); // bị "đè" vĩnh viễn
            dq.offerLast(i);
            if (i >= k - 1) res[i - k + 1] = nums[dq.peekFirst()];
        }
        return res;
    }

    static int[] bruteForce(int[] nums, int k) {
        int[] res = new int[nums.length - k + 1];
        for (int i = 0; i + k <= nums.length; i++) {
            int max = Integer.MIN_VALUE;
            for (int j = i; j < i + k; j++) max = Math.max(max, nums[j]);
            res[i] = max;
        }
        return res;
    }

    public static void main(String[] args) {
        check(Arrays.equals(maxSlidingWindow(new int[]{1, 3, -1, -3, 5, 3, 6, 7}, 3), new int[]{3, 3, 5, 5, 6, 7}));
        check(Arrays.equals(maxSlidingWindow(new int[]{1}, 1), new int[]{1}));
        check(Arrays.equals(maxSlidingWindow(new int[]{9, 8, 7, 6}, 2), new int[]{9, 8, 7}));
        check(Arrays.equals(maxSlidingWindow(new int[]{4, 4, 4}, 2), new int[]{4, 4}));
        check(maxSlidingWindow(new int[0], 3).length == 0);
        Random rnd = new Random(11);
        for (int iter = 0; iter < 500; iter++) {
            int[] a = new int[1 + rnd.nextInt(25)];
            for (int i = 0; i < a.length; i++) a[i] = rnd.nextInt(21) - 10;
            int k = 1 + rnd.nextInt(a.length);
            check(Arrays.equals(maxSlidingWindow(a, k), bruteForce(a, k)));
        }
        System.out.println("SlidingWindowMaximum OK");
    }

    static void check(boolean ok) { if (!ok) throw new AssertionError("test failed"); }
}
```

*Độ phức tạp:* O(n) time amortized, O(k) space cho deque.

*Edge cases / test:* k = 1 (chính mảng), k = n (một phần tử), giá trị bằng nhau, giảm dần — đối chiếu ngẫu nhiên với brute force.

**Câu hỏi nối tiếp:**
- *Dùng `<=` hay `<` khi pop cuối?* — `<=` giữ deque nhỏ hơn; `<` cũng đúng nhưng giữ cả bản trùng.
- *Ứng dụng?* — Max latency trong cửa sổ 1 phút cho dashboard, rate limiter dạng sliding window, "min của max" trong DP tối ưu bằng deque.
- *Median trong cửa sổ?* — Hai heap + lazy deletion hoặc `TreeMap` đếm, O(n log k).

**⚠️ Câu trả lời gây điểm trừ:** dùng `PriorityQueue.remove(Object)` cho mỗi bước (O(k)) mà nói là O(n log k); không giải thích vì sao amortized O(n).

**📖 Ôn lại:** [8.4 Monotonic deque](../01-giao-trinh/17-data-structures-algorithms.md#84-monotonic-deque-queue)

</details>

---

<a id="g5"></a>
## 5. Linked List

### Q18. 🟢 Đảo ngược danh sách liên kết (Reverse Linked List — LeetCode 206)

**Đề bài:** Đảo ngược singly linked list, trả về head mới. Viết cả iterative và recursive. Ví dụ: `1→2→3→4→5` → `5→4→3→2→1`.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Iterative: ba con trỏ `prev`, `cur`, `next` — lưu `next`, đảo `cur.next = prev`, tiến lên. O(n)/O(1). Recursive: đảo phần đuôi, rồi `head.next.next = head; head.next = null` — O(n) stack, có thể `StackOverflowError` với list dài.

**Giải thích chi tiết:**

*Câu hỏi làm rõ nên hỏi:* Được sửa list tại chỗ hay phải tạo list mới? List có thể rất dài (10⁶ node → tránh đệ quy)? `null` head?

```java
import java.util.*;

public class ReverseLinkedList {
    static final class ListNode {
        int val; ListNode next;
        ListNode(int val, ListNode next) { this.val = val; this.next = next; }
    }

    public static ListNode reverse(ListNode head) {
        ListNode prev = null, cur = head;
        while (cur != null) {
            ListNode next = cur.next;     // 1. giữ phần còn lại
            cur.next = prev;              // 2. đảo mũi tên
            prev = cur;                   // 3. tiến hai con trỏ
            cur = next;
        }
        return prev;
    }

    public static ListNode reverseRecursive(ListNode head) {
        if (head == null || head.next == null) return head;
        ListNode newHead = reverseRecursive(head.next);
        head.next.next = head;            // node sau head trỏ ngược về head
        head.next = null;
        return newHead;
    }

    static ListNode of(int... values) {
        ListNode dummy = new ListNode(0, null), tail = dummy;
        for (int v : values) { tail.next = new ListNode(v, null); tail = tail.next; }
        return dummy.next;
    }

    static List<Integer> toList(ListNode head) {
        List<Integer> res = new ArrayList<>();
        for (ListNode n = head; n != null; n = n.next) res.add(n.val);
        return res;
    }

    public static void main(String[] args) {
        check(toList(reverse(of(1, 2, 3, 4, 5))).equals(List.of(5, 4, 3, 2, 1)));
        check(toList(reverseRecursive(of(1, 2, 3, 4, 5))).equals(List.of(5, 4, 3, 2, 1)));
        check(reverse(null) == null);
        check(toList(reverse(of(7))).equals(List.of(7)));
        check(toList(reverseRecursive(of(1, 2))).equals(List.of(2, 1)));
        int[] big = new int[1_000_000];
        for (int i = 0; i < big.length; i++) big[i] = i;
        check(reverse(of(big)).val == 999_999);              // iterative không lo stack overflow
        System.out.println("ReverseLinkedList OK");
    }

    static void check(boolean ok) { if (!ok) throw new AssertionError("test failed"); }
}
```

*Độ phức tạp:* Iterative O(n)/O(1); recursive O(n)/O(n) call stack.

*Edge cases / test:* `null`, một node, hai node, list rất dài (recursive có thể `StackOverflowError` — Java không tối ưu tail call).

**Câu hỏi nối tiếp:**
- *Đảo đoạn [m, n] (LeetCode 92) / theo nhóm k (LeetCode 25)?* — Dùng dummy node, đảo từng đoạn và nối lại hai đầu.
- *Kiểm tra palindrome list O(1) space?* — Tìm giữa bằng slow/fast, đảo nửa sau, so sánh, (đảo lại để trả nguyên trạng).

**⚠️ Câu trả lời gây điểm trừ:** mất tham chiếu `next` trước khi đảo (list bị cắt); quên `head.next = null` trong bản đệ quy (tạo chu trình).

**📖 Ôn lại:** [9. Linked List](../01-giao-trinh/17-data-structures-algorithms.md#9-linked-list)

</details>

### Q19. 🟡 Tìm điểm bắt đầu chu trình (Linked List Cycle II — LeetCode 142)

**Đề bài:** Trả về node nơi chu trình bắt đầu, hoặc `null` nếu không có chu trình. Yêu cầu O(1) bộ nhớ.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **Floyd (rùa và thỏ)**: `slow` đi 1, `fast` đi 2; nếu gặp nhau thì có chu trình. Đặt một con trỏ về `head`, cả hai cùng đi 1 bước — điểm gặp tiếp theo là **đầu chu trình**. O(n)/O(1).

**Giải thích chi tiết:**

*Câu hỏi làm rõ nên hỏi:* Được sửa list không (đánh dấu node)? Bộ nhớ có hạn chế (nếu không, `HashSet<ListNode>` O(n) là đủ)?

*Brute force → tối ưu:* `HashSet` các node đã thăm, node đầu tiên lặp lại là đáp án → O(n)/O(n). Floyd → O(1) space.

*Chứng minh:* gọi `a` = khoảng cách head → đầu chu trình, `b` = đầu chu trình → điểm gặp, `c` = độ dài chu trình. Khi gặp: `fast` đi gấp đôi `slow` ⇒ `2(a + b) = a + b + m·c` ⇒ `a = m·c − b`. Từ điểm gặp đi thêm `a` bước = `m·c − b` bước → về đúng đầu chu trình; con trỏ từ head đi `a` bước cũng tới đó.

```java
public class LinkedListCycleII {
    static final class ListNode {
        int val; ListNode next;
        ListNode(int val) { this.val = val; }
    }

    public static ListNode detectCycle(ListNode head) {
        ListNode slow = head, fast = head;
        while (fast != null && fast.next != null) {
            slow = slow.next;
            fast = fast.next.next;
            if (slow == fast) {                       // so sánh tham chiếu, không phải giá trị
                ListNode p = head;
                while (p != slow) { p = p.next; slow = slow.next; }
                return p;
            }
        }
        return null;
    }

    /** Tạo list từ values; pos = chỉ số node mà node cuối trỏ về (-1: không có chu trình). */
    static ListNode[] build(int[] values, int pos) {
        ListNode[] nodes = new ListNode[values.length];
        for (int i = 0; i < values.length; i++) nodes[i] = new ListNode(values[i]);
        for (int i = 0; i + 1 < values.length; i++) nodes[i].next = nodes[i + 1];
        if (pos >= 0) nodes[values.length - 1].next = nodes[pos];
        return nodes;
    }

    public static void main(String[] args) {
        ListNode[] a = build(new int[]{3, 2, 0, -4}, 1);
        check(detectCycle(a[0]) == a[1]);
        ListNode[] b = build(new int[]{1, 2}, 0);
        check(detectCycle(b[0]) == b[0]);
        ListNode[] c = build(new int[]{1}, -1);
        check(detectCycle(c[0]) == null);
        ListNode[] d = build(new int[]{1}, 0);                // tự trỏ vào chính nó
        check(detectCycle(d[0]) == d[0]);
        check(detectCycle(null) == null);
        for (int n = 1; n <= 30; n++) {
            for (int pos = -1; pos < n; pos++) {
                ListNode[] e = build(new int[n], pos);
                check(detectCycle(e[0]) == (pos < 0 ? null : e[pos]));
            }
        }
        System.out.println("LinkedListCycleII OK");
    }

    static void check(boolean ok) { if (!ok) throw new AssertionError("test failed"); }
}
```

*Độ phức tạp:* O(n) time, O(1) space.

*Edge cases / test:* không chu trình, node tự trỏ vào chính nó, chu trình bắt đầu từ head, list rỗng — test vét cạn mọi vị trí chu trình với n ≤ 30.

**Câu hỏi nối tiếp:**
- *Độ dài chu trình?* — Sau khi gặp, giữ một con trỏ, đi con trỏ kia tới khi gặp lại, đếm bước.
- *Find the Duplicate Number (LeetCode 287)?* — Coi `i → nums[i]` là linked list, áp dụng Floyd, O(1) space không sửa mảng.
- *Ứng dụng?* — Phát hiện vòng trong chuỗi redirect, chuỗi tham chiếu cha–con bị lỗi dữ liệu.

**⚠️ Câu trả lời gây điểm trừ:** so sánh `slow.val == fast.val` (giá trị có thể trùng); nói "Floyd" mà không giải thích được vì sao bước hai tìm đúng đầu chu trình.

**📖 Ôn lại:** [9. Linked List](../01-giao-trinh/17-data-structures-algorithms.md#9-linked-list)

</details>

### Q20. 🔴 Trộn K danh sách đã sort (Merge K Sorted Lists — LeetCode 23)

**Đề bài:** Cho mảng `k` linked list đã sort tăng dần, trộn thành một list sort. Tổng `N` node.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **Min-heap** chứa node đầu của mỗi list; poll nhỏ nhất, nối vào kết quả, đẩy node kế tiếp của list đó. O(N log k) time, O(k) space. Cách khác: **chia để trị** trộn từng cặp (như merge sort) — cũng O(N log k), O(1) space phụ (iterative).

**Giải thích chi tiết:**

*Câu hỏi làm rõ nên hỏi:* Có list rỗng/`null` trong mảng? k lớn cỡ nào so với N? Được tái sử dụng node cũ không (không cần tạo node mới)? Cần ổn định (stable) khi bằng nhau?

*Brute force → tối ưu:*
1. Gom mọi giá trị, sort, dựng list mới → O(N log N), O(N).
2. Trộn tuần tự list 1 với 2, kết quả với 3… → O(N·k) vì list kết quả bị duyệt lại k lần.
3. Heap hoặc chia để trị → O(N log k).

```java
import java.util.*;

public class MergeKSortedLists {
    static final class ListNode {
        int val; ListNode next;
        ListNode(int val, ListNode next) { this.val = val; this.next = next; }
    }

    /** Min-heap: O(N log k) time, O(k) space. */
    public static ListNode mergeKLists(ListNode[] lists) {
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

    /** Chia để trị: trộn từng cặp theo bước nhảy 1, 2, 4... O(N log k), không cần heap. */
    public static ListNode mergeDivideAndConquer(ListNode[] lists) {
        if (lists.length == 0) return null;
        ListNode[] work = lists.clone();
        for (int step = 1; step < work.length; step *= 2) {
            for (int i = 0; i + step < work.length; i += 2 * step) {
                work[i] = mergeTwo(work[i], work[i + step]);
            }
        }
        return work[0];
    }

    static ListNode mergeTwo(ListNode a, ListNode b) {
        ListNode dummy = new ListNode(0, null), tail = dummy;
        while (a != null && b != null) {
            if (a.val <= b.val) { tail.next = a; a = a.next; } else { tail.next = b; b = b.next; }
            tail = tail.next;
        }
        tail.next = (a != null) ? a : b;
        return dummy.next;
    }

    static ListNode of(int... values) {
        ListNode dummy = new ListNode(0, null), tail = dummy;
        for (int v : values) { tail.next = new ListNode(v, null); tail = tail.next; }
        return dummy.next;
    }

    static List<Integer> toList(ListNode head) {
        List<Integer> res = new ArrayList<>();
        for (ListNode n = head; n != null; n = n.next) res.add(n.val);
        return res;
    }

    public static void main(String[] args) {
        check(toList(mergeKLists(new ListNode[]{of(1, 4, 5), of(1, 3, 4), of(2, 6)})).equals(List.of(1, 1, 2, 3, 4, 4, 5, 6)));
        check(toList(mergeDivideAndConquer(new ListNode[]{of(1, 4, 5), of(1, 3, 4), of(2, 6)})).equals(List.of(1, 1, 2, 3, 4, 4, 5, 6)));
        check(mergeKLists(new ListNode[0]) == null);
        check(mergeKLists(new ListNode[]{null, null}) == null);
        check(toList(mergeDivideAndConquer(new ListNode[]{null, of(-1, 0)})).equals(List.of(-1, 0)));
        Random rnd = new Random(5);
        for (int iter = 0; iter < 200; iter++) {
            int k = rnd.nextInt(8);
            ListNode[] a = new ListNode[k], b = new ListNode[k];
            List<Integer> all = new ArrayList<>();
            for (int i = 0; i < k; i++) {
                int[] vals = rnd.ints(rnd.nextInt(6), -20, 20).sorted().toArray();
                for (int v : vals) all.add(v);
                a[i] = of(vals);
                b[i] = of(vals);
            }
            Collections.sort(all);
            check(toList(mergeKLists(a)).equals(all));
            check(toList(mergeDivideAndConquer(b)).equals(all));
        }
        System.out.println("MergeKSortedLists OK");
    }

    static void check(boolean ok) { if (!ok) throw new AssertionError("test failed"); }
}
```

*Độ phức tạp:* O(N log k) time cho cả hai; heap O(k) space, chia để trị O(1) space phụ.

*Edge cases / test:* mảng rỗng, toàn `null`, list độ dài khác nhau, giá trị trùng, số âm.

**Câu hỏi nối tiếp:**
- *K file log đã sort theo timestamp, mỗi file vài GB?* — Chính là pha trộn của **external merge sort**: heap chứa dòng đầu mỗi file + `BufferedReader` lớn; số file quá nhiều (giới hạn FD) thì trộn nhiều pha.
- *Comparator `(a, b) -> a.val - b.val` có vấn đề gì?* — Tràn số với giá trị lớn trái dấu; dùng `Integer.compare`/`comparingInt`.
- *Cần stable?* — Thêm chỉ số list vào phần tử heap làm tie-breaker.

**⚠️ Câu trả lời gây điểm trừ:** trộn tuần tự rồi nói là O(N log k); đẩy `null` vào `PriorityQueue` (NPE).

**📖 Ôn lại:** [18.6 Merge K sorted lists & External merge sort](../01-giao-trinh/17-data-structures-algorithms.md#186-merge-k-sorted-lists--external-merge-sort)

</details>

---

<a id="g6"></a>
## 6. Trees

### Q21. 🟢 Duyệt cây theo tầng (Binary Tree Level Order Traversal — LeetCode 102)

**Đề bài:** Trả về giá trị các node theo từng tầng, trái sang phải. Ví dụ: cây `[3,9,20,null,null,15,7]` → `[[3],[9,20],[15,7]]`.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** BFS với queue; ở mỗi vòng, **chụp `size` của queue** = số node của tầng hiện tại, poll đúng `size` node và đẩy con của chúng. O(n) time, O(w) space (w = độ rộng lớn nhất).

**Giải thích chi tiết:**

*Câu hỏi làm rõ nên hỏi:* Cây rỗng trả `[]`? Cần zigzag/từ dưới lên? Cây có thể rất sâu (đệ quy DFS có rủi ro stack)?

```java
import java.util.*;

public class LevelOrderTraversal {
    static final class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    public static List<List<Integer>> levelOrder(TreeNode root) {
        List<List<Integer>> res = new ArrayList<>();
        if (root == null) return res;
        Deque<TreeNode> queue = new ArrayDeque<>();      // ArrayDeque không nhận null -> chỉ offer node khác null
        queue.offer(root);
        while (!queue.isEmpty()) {
            int size = queue.size();                      // số node của tầng hiện tại
            List<Integer> level = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                TreeNode n = queue.poll();
                level.add(n.val);
                if (n.left != null) queue.offer(n.left);
                if (n.right != null) queue.offer(n.right);
            }
            res.add(level);
        }
        return res;
    }

    /** Dựng cây từ mảng level-order kiểu LeetCode (null = không có node). */
    static TreeNode build(Integer... values) {
        if (values.length == 0 || values[0] == null) return null;
        TreeNode root = new TreeNode(values[0]);
        Deque<TreeNode> q = new ArrayDeque<>(List.of(root));
        int i = 1;
        while (!q.isEmpty() && i < values.length) {
            TreeNode n = q.poll();
            if (i < values.length && values[i] != null) { n.left = new TreeNode(values[i]); q.offer(n.left); }
            i++;
            if (i < values.length && values[i] != null) { n.right = new TreeNode(values[i]); q.offer(n.right); }
            i++;
        }
        return root;
    }

    public static void main(String[] args) {
        check(levelOrder(build(3, 9, 20, null, null, 15, 7)).equals(List.of(List.of(3), List.of(9, 20), List.of(15, 7))));
        check(levelOrder(build()).isEmpty());
        check(levelOrder(build(1)).equals(List.of(List.of(1))));
        check(levelOrder(build(1, 2, null, 3, null, 4)).equals(List.of(List.of(1), List.of(2), List.of(3), List.of(4))));
        System.out.println("LevelOrderTraversal OK");
    }

    static void check(boolean ok) { if (!ok) throw new AssertionError("test failed"); }
}
```

*Độ phức tạp:* O(n) time, O(w) space cho queue (cây đầy đủ: w ≈ n/2).

*Edge cases / test:* cây rỗng, một node, cây lệch hẳn một phía (mỗi tầng một node).

**Câu hỏi nối tiếp:**
- *Zigzag (LeetCode 103)?* — Đảo thứ tự `level` ở tầng lẻ, hoặc dùng `addFirst` của deque.
- *Right Side View (LeetCode 199)?* — Lấy phần tử cuối của mỗi tầng.
- *Độ sâu tối thiểu?* — BFS dừng ở lá đầu tiên — tốt hơn DFS với cây lệch.

**⚠️ Câu trả lời gây điểm trừ:** dùng `queue.size()` trực tiếp trong điều kiện `for` (thay đổi khi offer); đẩy `null` vào `ArrayDeque` (NPE).

**📖 Ôn lại:** [10. Trees & Binary Search Tree](../01-giao-trinh/17-data-structures-algorithms.md#10-trees--binary-search-tree)

</details>

### Q22. 🟡 Kiểm tra cây nhị phân tìm kiếm hợp lệ (Validate BST — LeetCode 98)

**Đề bài:** Kiểm tra cây có phải BST hợp lệ: mọi node bên trái **nhỏ hơn hẳn**, mọi node bên phải **lớn hơn hẳn** node gốc của nó — áp dụng cho **toàn bộ** cây con, không chỉ con trực tiếp.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Truyền **khoảng hợp lệ (low, high)** xuống khi đệ quy: con trái nhận `(low, node.val)`, con phải nhận `(node.val, high)`. Hoặc duyệt **inorder** và kiểm tra dãy tăng nghiêm ngặt. Dùng `Integer`/`long` cho biên để không vỡ với `Integer.MIN_VALUE`/`MAX_VALUE`.

**Giải thích chi tiết:**

*Câu hỏi làm rõ nên hỏi:* Cho phép giá trị trùng không (và nằm bên nào)? Giá trị có thể là `Integer.MIN_VALUE`/`MAX_VALUE`? Cây sâu cỡ nào (đệ quy vs iterative)?

*Bẫy kinh điển:* chỉ so node với con trực tiếp → cây `[5,4,6,null,null,3,7]` bị coi là hợp lệ dù `3` nằm trong cây con phải của `5`.

```java
import java.util.*;

public class ValidateBST {
    static final class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    /** Đệ quy với khoảng (low, high) mở; null = không giới hạn. */
    public static boolean isValidBST(TreeNode root) {
        return valid(root, null, null);
    }

    private static boolean valid(TreeNode n, Integer low, Integer high) {
        if (n == null) return true;
        if ((low != null && n.val <= low) || (high != null && n.val >= high)) return false;
        return valid(n.left, low, n.val) && valid(n.right, n.val, high);
    }

    /** Inorder iterative: dãy phải tăng nghiêm ngặt. Không lo stack overflow với cây sâu. */
    public static boolean isValidBSTInorder(TreeNode root) {
        Deque<TreeNode> stack = new ArrayDeque<>();
        TreeNode cur = root;
        Integer prev = null;
        while (cur != null || !stack.isEmpty()) {
            while (cur != null) { stack.push(cur); cur = cur.left; }
            cur = stack.pop();
            if (prev != null && cur.val <= prev) return false;
            prev = cur.val;
            cur = cur.right;
        }
        return true;
    }

    static TreeNode build(Integer... values) {
        if (values.length == 0 || values[0] == null) return null;
        TreeNode root = new TreeNode(values[0]);
        Deque<TreeNode> q = new ArrayDeque<>(List.of(root));
        int i = 1;
        while (!q.isEmpty() && i < values.length) {
            TreeNode n = q.poll();
            if (i < values.length && values[i] != null) { n.left = new TreeNode(values[i]); q.offer(n.left); }
            i++;
            if (i < values.length && values[i] != null) { n.right = new TreeNode(values[i]); q.offer(n.right); }
            i++;
        }
        return root;
    }

    public static void main(String[] args) {
        Object[][] cases = {
            {new Integer[]{2, 1, 3}, true},
            {new Integer[]{5, 1, 4, null, null, 3, 6}, false},
            {new Integer[]{5, 4, 6, null, null, 3, 7}, false},          // bẫy: 3 nằm trong cây con phải của 5
            {new Integer[]{1, 1}, false},                                // trùng không hợp lệ
            {new Integer[]{Integer.MAX_VALUE}, true},
            {new Integer[]{Integer.MIN_VALUE, null, Integer.MAX_VALUE}, true},
            {new Integer[]{}, true},
        };
        for (Object[] c : cases) {
            TreeNode root = build((Integer[]) c[0]);
            check(isValidBST(root) == (boolean) c[1]);
            check(isValidBSTInorder(root) == (boolean) c[1]);
        }
        System.out.println("ValidateBST OK");
    }

    static void check(boolean ok) { if (!ok) throw new AssertionError("test failed"); }
}
```

*Độ phức tạp:* O(n) time, O(h) space (h = chiều cao; cây lệch h = n).

*Edge cases / test:* cây rỗng, giá trị trùng, giá trị cực trị, vi phạm ở cháu (không phải con trực tiếp).

**Câu hỏi nối tiếp:**
- *Vì sao không dùng `Long.MIN_VALUE` làm biên?* — Dùng được với node `int`; `Integer` nullable hoặc `long` đều ổn — quan trọng là **không** dùng `Integer.MIN_VALUE` làm sentinel.
- *Kth Smallest in BST (LeetCode 230)?* — Inorder iterative dừng ở phần tử thứ k; nếu truy vấn nhiều lần, lưu kích thước cây con trong node.
- *Vì sao `TreeMap` là red-black tree, còn DB index dùng B+ tree?* — RB tree cân bằng tốt trong RAM; B+ tree fan-out lớn → ít lần đọc đĩa/page, lá liên kết cho range scan.

**⚠️ Câu trả lời gây điểm trừ:** chỉ so với con trực tiếp; dùng `Integer.MIN_VALUE` làm giá trị khởi tạo cho `prev`.

**📖 Ôn lại:** [10.2 BST operations, validate, LCA](../01-giao-trinh/17-data-structures-algorithms.md#102-bst-operations-validate-lca)

</details>

### Q23. 🟡 Tổ tiên chung gần nhất (Lowest Common Ancestor — LeetCode 236 & 235)

**Đề bài:** Cho cây nhị phân và hai node `p`, `q` (chắc chắn có trong cây). Tìm tổ tiên chung thấp nhất (một node có thể là tổ tiên của chính nó). Làm tiếp: nếu cây là **BST** thì sao?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Cây thường: DFS hậu thứ tự — nếu node hiện tại là `p` hoặc `q` thì trả về nó; nếu cả cây con trái và phải đều trả về khác `null` thì node hiện tại là LCA; ngược lại trả về phía khác `null`. O(n). BST: đi từ gốc, nếu cả hai nhỏ hơn thì sang trái, cả hai lớn hơn thì sang phải, còn lại là điểm tách = LCA. O(h), O(1) space.

**Giải thích chi tiết:**

*Câu hỏi làm rõ nên hỏi:* `p`, `q` chắc chắn tồn tại trong cây? (Nếu không, phải đếm số node tìm thấy.) Node có con trỏ `parent` không? (Có thì bài thành "giao điểm của hai linked list".) Cây là BST? Truy vấn nhiều lần (→ binary lifting / Euler tour + RMQ)?

*Brute force → tối ưu:* Tìm đường đi gốc → p và gốc → q (hai list), so sánh tiền tố chung → O(n) nhưng hai lần duyệt và O(h) bộ nhớ thêm. Đệ quy một lượt gọn hơn. Với BST tận dụng thứ tự để đạt O(h).

```java
import java.util.*;

public class LowestCommonAncestor {
    static final class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int val) { this.val = val; }
    }

    /** Cây nhị phân bất kỳ; giả định p và q đều có trong cây. */
    public static TreeNode lca(TreeNode root, TreeNode p, TreeNode q) {
        if (root == null || root == p || root == q) return root;
        TreeNode left = lca(root.left, p, q);
        TreeNode right = lca(root.right, p, q);
        if (left != null && right != null) return root;   // p và q nằm ở hai phía
        return left != null ? left : right;
    }

    /** BST: đi xuống tới điểm tách, O(h) time, O(1) space. */
    public static TreeNode lcaBst(TreeNode root, TreeNode p, TreeNode q) {
        TreeNode cur = root;
        while (cur != null) {
            if (p.val < cur.val && q.val < cur.val) cur = cur.left;
            else if (p.val > cur.val && q.val > cur.val) cur = cur.right;
            else return cur;
        }
        return null;
    }

    static TreeNode build(Integer... values) {
        if (values.length == 0 || values[0] == null) return null;
        TreeNode root = new TreeNode(values[0]);
        Deque<TreeNode> q = new ArrayDeque<>(List.of(root));
        int i = 1;
        while (!q.isEmpty() && i < values.length) {
            TreeNode n = q.poll();
            if (i < values.length && values[i] != null) { n.left = new TreeNode(values[i]); q.offer(n.left); }
            i++;
            if (i < values.length && values[i] != null) { n.right = new TreeNode(values[i]); q.offer(n.right); }
            i++;
        }
        return root;
    }

    static TreeNode find(TreeNode root, int val) {
        if (root == null || root.val == val) return root;
        TreeNode l = find(root.left, val);
        return l != null ? l : find(root.right, val);
    }

    public static void main(String[] args) {
        TreeNode t = build(3, 5, 1, 6, 2, 0, 8, null, null, 7, 4);
        check(lca(t, find(t, 5), find(t, 1)).val == 3);
        check(lca(t, find(t, 5), find(t, 4)).val == 5);      // một node là tổ tiên của node kia
        check(lca(t, find(t, 7), find(t, 8)).val == 3);
        check(lca(t, find(t, 6), find(t, 4)).val == 5);
        check(lca(t, find(t, 7), find(t, 7)).val == 7);

        TreeNode bst = build(6, 2, 8, 0, 4, 7, 9, null, null, 3, 5);
        check(lcaBst(bst, find(bst, 2), find(bst, 8)).val == 6);
        check(lcaBst(bst, find(bst, 2), find(bst, 4)).val == 2);
        check(lcaBst(bst, find(bst, 3), find(bst, 5)).val == 4);
        check(lca(bst, find(bst, 3), find(bst, 5)).val == 4);  // bản tổng quát cũng đúng trên BST
        System.out.println("LowestCommonAncestor OK");
    }

    static void check(boolean ok) { if (!ok) throw new AssertionError("test failed"); }
}
```

*Độ phức tạp:* Cây thường O(n) time, O(h) stack. BST O(h) time, O(1) space.

*Edge cases / test:* `p == q`, `p` là tổ tiên của `q`, hai node ở hai cây con khác nhau, cây lệch.

**Câu hỏi nối tiếp:**
- *Nếu `p` hoặc `q` có thể không tồn tại?* — Bản trên sẽ trả `p` dù `q` vắng mặt; phải đếm số node tìm thấy và chỉ trả kết quả khi đếm đủ 2.
- *Node có con trỏ `parent`?* — Hai con trỏ đi lên, chạm gốc thì chuyển sang đầu kia (như Intersection of Two Linked Lists), O(h)/O(1).
- *Ứng dụng?* — Tìm "phòng ban chung" trong cây tổ chức, merge-base trong Git (DAG, không phải cây), category cha chung trong cây danh mục.

**⚠️ Câu trả lời gây điểm trừ:** so sánh bằng `val` trên cây thường có giá trị trùng; với BST vẫn duyệt O(n) mà không tận dụng thứ tự.

**📖 Ôn lại:** [10.2 BST operations, validate, LCA](../01-giao-trinh/17-data-structures-algorithms.md#102-bst-operations-validate-lca)

</details>

---

<a id="g7"></a>
## 7. Heap & Graph

### Q24. 🟡 Phần tử lớn thứ K (Kth Largest Element in an Array — LeetCode 215)

**Đề bài:** Tìm phần tử lớn thứ `k` (theo thứ tự sort, không phải phần tử phân biệt thứ k). Ví dụ: `[3,2,3,1,2,4,5,5,6], k = 4` → `4`.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Hai lời giải chuẩn: (1) **min-heap kích thước k** — O(n log k), O(k), chạy được trên stream; (2) **quickselect** với pivot ngẫu nhiên và phân hoạch 3 ngả — trung bình O(n), worst case O(n²), O(1) bộ nhớ thêm (nếu được sửa mảng). Sort cả mảng O(n log n) là baseline.

**Giải thích chi tiết:**

*Câu hỏi làm rõ nên hỏi:* `1 ≤ k ≤ n`? Được sửa mảng đầu vào không? Dữ liệu là stream/không vừa RAM? Nhiều phần tử trùng (ảnh hưởng quickselect 2 ngả)?

*Brute force → tối ưu:*
1. Sort → O(n log n).
2. Min-heap giữ k phần tử lớn nhất; đỉnh heap là đáp án → O(n log k). Ưu điểm: online, bộ nhớ O(k).
3. Quickselect: chỉ đệ quy vào một phía → trung bình O(n). Pivot ngẫu nhiên chống input xấu có chủ đích; phân hoạch **3 ngả** (Dutch national flag) chống suy biến khi nhiều phần tử bằng nhau.

```java
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class KthLargest {
    /** Min-heap kích thước k: O(n log k) time, O(k) space; dùng được cho stream. */
    public static int kthLargestHeap(int[] nums, int k) {
        validate(nums, k);
        PriorityQueue<Integer> minHeap = new PriorityQueue<>(k);
        for (int x : nums) {
            if (minHeap.size() < k) minHeap.offer(x);
            else if (x > minHeap.peek()) { minHeap.poll(); minHeap.offer(x); }
        }
        return minHeap.peek();
    }

    /** Quickselect, pivot ngẫu nhiên, phân hoạch 3 ngả: trung bình O(n). Không sửa mảng gốc. */
    public static int kthLargestQuickselect(int[] nums, int k) {
        validate(nums, k);
        int[] a = nums.clone();
        int target = a.length - k;                        // chỉ số của đáp án nếu sort tăng dần
        int lo = 0, hi = a.length - 1;
        while (true) {
            int pivot = a[ThreadLocalRandom.current().nextInt(lo, hi + 1)];
            int lt = lo, i = lo, gt = hi;                 // [lo,lt) < pivot, [lt,i) == pivot, (gt,hi] > pivot
            while (i <= gt) {
                if (a[i] < pivot) swap(a, lt++, i++);
                else if (a[i] > pivot) swap(a, i, gt--);
                else i++;
            }
            if (target < lt) hi = lt - 1;
            else if (target > gt) lo = gt + 1;
            else return pivot;                            // target rơi vào vùng bằng pivot
        }
    }

    private static void swap(int[] a, int i, int j) { int t = a[i]; a[i] = a[j]; a[j] = t; }

    private static void validate(int[] nums, int k) {
        if (k < 1 || k > nums.length) throw new IllegalArgumentException("k must be in [1, n]");
    }

    public static void main(String[] args) {
        check(kthLargestHeap(new int[]{3, 2, 1, 5, 6, 4}, 2) == 5);
        check(kthLargestQuickselect(new int[]{3, 2, 1, 5, 6, 4}, 2) == 5);
        check(kthLargestHeap(new int[]{3, 2, 3, 1, 2, 4, 5, 5, 6}, 4) == 4);
        check(kthLargestQuickselect(new int[]{3, 2, 3, 1, 2, 4, 5, 5, 6}, 4) == 4);
        check(kthLargestQuickselect(new int[]{7, 7, 7, 7}, 3) == 7);
        check(kthLargestHeap(new int[]{-1}, 1) == -1);
        Random rnd = new Random(8);
        for (int iter = 0; iter < 1000; iter++) {
            int[] a = new int[1 + rnd.nextInt(30)];
            for (int i = 0; i < a.length; i++) a[i] = rnd.nextInt(10) - 5;   // nhiều giá trị trùng
            int k = 1 + rnd.nextInt(a.length);
            int[] sorted = a.clone();
            Arrays.sort(sorted);
            int expected = sorted[a.length - k];
            check(kthLargestHeap(a, k) == expected);
            check(kthLargestQuickselect(a, k) == expected);
        }
        int[] allSame = new int[200_000];                // 3 ngả: không suy biến O(n²) khi toàn phần tử bằng nhau
        check(kthLargestQuickselect(allSame, 100_000) == 0);
        System.out.println("KthLargest OK");
    }

    static void check(boolean ok) { if (!ok) throw new AssertionError("test failed"); }
}
```

*Độ phức tạp:* Heap O(n log k)/O(k). Quickselect trung bình O(n), worst case O(n²) (xác suất cực nhỏ với pivot ngẫu nhiên), O(n) cho bản sao (O(1) nếu được sửa tại chỗ).

*Edge cases / test:* k = 1, k = n, toàn phần tử bằng nhau, số âm, k ngoài phạm vi → exception.

**Câu hỏi nối tiếp:**
- *Cần worst case O(n) đảm bảo?* — Median of medians (BFPRT), hằng số lớn nên ít dùng thực tế; `introselect` chuyển sang thuật toán an toàn khi đệ quy quá sâu.
- *Top-K từ 1 tỷ bản ghi trên nhiều máy?* — Mỗi máy tính top-K cục bộ bằng heap, gửi về để trộn → chỉ truyền K·số máy phần tử.
- *Vì sao `new PriorityQueue<>((a, b) -> b - a)` nguy hiểm?* — Tràn số khi trừ hai số khác dấu lớn; dùng `Comparator.reverseOrder()`.

**⚠️ Câu trả lời gây điểm trừ:** dùng max-heap chứa toàn bộ n phần tử rồi poll k lần mà nói là tối ưu; quickselect pivot cố định đầu/cuối (O(n²) với mảng đã sort).

**📖 Ôn lại:** [11.2 Pattern Top-K](../01-giao-trinh/17-data-structures-algorithms.md#112-pattern-top-k)

</details>

### Q25. 🟡 Đếm số đảo (Number of Islands — LeetCode 200)

**Đề bài:** Lưới `char[][]` gồm `'1'` (đất) và `'0'` (nước). Đếm số đảo — vùng đất liên thông theo 4 hướng.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Duyệt từng ô; gặp ô đất chưa thăm thì tăng đếm và **BFS/DFS loang** đánh dấu toàn bộ đảo. O(R·C). Với lưới lớn ưu tiên **BFS bằng queue** (hoặc DFS bằng stack tường minh) vì DFS đệ quy có thể `StackOverflowError`. Đánh dấu "đã thăm" **lúc đưa vào queue**, không phải lúc lấy ra.

**Giải thích chi tiết:**

*Câu hỏi làm rõ nên hỏi:* Được sửa lưới đầu vào không (ghi `'0'` để đánh dấu, tiết kiệm bộ nhớ nhưng có side effect)? Liên thông 4 hay 8 hướng? Kích thước tối đa (1000×1000 → đệ quy sâu 10⁶)? Lưới rỗng?

*Brute force → tối ưu:* Bài này lời giải tự nhiên đã tối ưu O(R·C); điểm Senior nằm ở chọn BFS/DFS, tránh stack overflow, không sửa input, đánh dấu đúng thời điểm (đánh dấu lúc poll khiến một ô bị enqueue nhiều lần). Union-Find là lựa chọn khác, đặc biệt khi đất được thêm dần (Number of Islands II).

```java
import java.util.*;

public class NumberOfIslands {
    private static final int[] DR = {1, -1, 0, 0};
    private static final int[] DC = {0, 0, 1, -1};

    public static int numIslands(char[][] grid) {
        if (grid.length == 0) return 0;
        int rows = grid.length, cols = grid[0].length;
        boolean[][] seen = new boolean[rows][cols];      // không sửa input của caller
        Deque<int[]> queue = new ArrayDeque<>();
        int islands = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (grid[r][c] != '1' || seen[r][c]) continue;
                islands++;
                seen[r][c] = true;                        // đánh dấu khi enqueue
                queue.offer(new int[]{r, c});
                while (!queue.isEmpty()) {
                    int[] cur = queue.poll();
                    for (int d = 0; d < 4; d++) {
                        int nr = cur[0] + DR[d], nc = cur[1] + DC[d];
                        if (nr >= 0 && nr < rows && nc >= 0 && nc < cols && grid[nr][nc] == '1' && !seen[nr][nc]) {
                            seen[nr][nc] = true;
                            queue.offer(new int[]{nr, nc});
                        }
                    }
                }
            }
        }
        return islands;
    }

    static char[][] grid(String... rows) {
        char[][] g = new char[rows.length][];
        for (int i = 0; i < rows.length; i++) g[i] = rows[i].toCharArray();
        return g;
    }

    public static void main(String[] args) {
        check(numIslands(grid("11110", "11010", "11000", "00000")) == 1);
        check(numIslands(grid("11000", "11000", "00100", "00011")) == 3);
        check(numIslands(grid("101", "010", "101")) == 5);         // chéo không tính là liên thông
        check(numIslands(new char[0][0]) == 0);
        check(numIslands(grid("0")) == 0);
        char[][] input = grid("1");
        check(numIslands(input) == 1 && input[0][0] == '1');       // input không bị sửa

        int n = 1000;                                              // 10^6 ô đất: DFS đệ quy dễ StackOverflowError
        char[][] big = new char[n][n];
        for (char[] row : big) Arrays.fill(row, '1');
        check(numIslands(big) == 1);
        for (int r = 0; r < n; r++) for (int c = 0; c < n; c++) big[r][c] = ((r + c) % 2 == 0) ? '1' : '0';
        check(numIslands(big) == n * n / 2);                       // bàn cờ: mỗi ô đất là một đảo
        System.out.println("NumberOfIslands OK");
    }

    static void check(boolean ok) { if (!ok) throw new AssertionError("test failed"); }
}
```

*Độ phức tạp:* O(R·C) time; O(R·C) cho `seen` + queue tối đa O(min(R, C)) đường biên (BFS) — nếu được sửa input thì bỏ `seen`.

*Edge cases / test:* lưới rỗng, toàn nước, toàn đất kích thước lớn, bàn cờ (nhiều đảo đơn lẻ), ô nối chéo.

**Câu hỏi nối tiếp:**
- *Đất được thêm dần, trả số đảo sau mỗi lần thêm (LeetCode 305)?* — Union-Find với path compression + union by rank, gần O(α(n)) mỗi thao tác.
- *Lưới không vừa RAM (ảnh vệ tinh khổng lồ)?* — Xử lý theo dải hàng, Union-Find trên nhãn ở biên giữa các dải (connected-component labeling hai lượt).
- *Max Area of Island / Rotting Oranges?* — Cùng khung BFS; Rotting Oranges là **multi-source BFS** (đưa mọi nguồn vào queue ban đầu).

**⚠️ Câu trả lời gây điểm trừ:** DFS đệ quy trên lưới 1000×1000 mà không nhắc rủi ro stack; đánh dấu khi poll (ô bị đưa vào queue nhiều lần, có thể bùng bộ nhớ); âm thầm sửa input.

**📖 Ôn lại:** [13.2 BFS & DFS](../01-giao-trinh/17-data-structures-algorithms.md#132-bfs--dfs-clrs-ch22)

</details>

### Q26. 🟡 Lịch học môn tiên quyết (Course Schedule I & II — LeetCode 207, 210)

**Đề bài:** Có `numCourses` môn (0..n−1) và danh sách `[a, b]` nghĩa là phải học `b` trước `a`. (I) Có học hết được không? (II) Trả về một thứ tự học hợp lệ, không có thì mảng rỗng.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Đây là **topological sort** trên đồ thị có hướng; học hết được ⇔ đồ thị **không có chu trình**. Thuật toán **Kahn**: đếm in-degree, đưa các đỉnh in-degree 0 vào queue, lấy ra thì giảm in-degree các đỉnh kề; nếu số đỉnh lấy ra < n thì có chu trình. O(V + E).

**Giải thích chi tiết:**

*Câu hỏi làm rõ nên hỏi:* Có cạnh trùng, tự vòng `[a, a]`? Có cần thứ tự "đẹp" (vd nhỏ nhất theo từ điển → dùng `PriorityQueue` thay queue)? Cần chỉ ra chu trình cụ thể để báo lỗi cho người dùng?

*Brute force → tối ưu:* Thử mọi hoán vị → O(n!). DFS ba màu (trắng/xám/đen) phát hiện back edge và cho thứ tự post-order đảo ngược — cũng O(V + E) nhưng đệ quy sâu. Kahn dùng vòng lặp, dễ giải thích, tự nhiên cho biết "các môn học được song song theo đợt".

```java
import java.util.*;

public class CourseSchedule {
    /** Kahn's algorithm. Trả về thứ tự hợp lệ, hoặc mảng rỗng nếu có chu trình. */
    public static int[] findOrder(int numCourses, int[][] prerequisites) {
        List<List<Integer>> adj = new ArrayList<>(numCourses);
        for (int i = 0; i < numCourses; i++) adj.add(new ArrayList<>());
        int[] indegree = new int[numCourses];
        for (int[] p : prerequisites) {                  // [a, b]: b -> a
            adj.get(p[1]).add(p[0]);
            indegree[p[0]]++;
        }
        Deque<Integer> queue = new ArrayDeque<>();
        for (int i = 0; i < numCourses; i++) if (indegree[i] == 0) queue.offer(i);
        int[] order = new int[numCourses];
        int taken = 0;
        while (!queue.isEmpty()) {
            int u = queue.poll();
            order[taken++] = u;
            for (int v : adj.get(u)) if (--indegree[v] == 0) queue.offer(v);
        }
        return taken == numCourses ? order : new int[0];  // còn đỉnh in-degree > 0 => nằm trên/sau chu trình
    }

    public static boolean canFinish(int numCourses, int[][] prerequisites) {
        return findOrder(numCourses, prerequisites).length == numCourses;
    }

    static boolean isValidOrder(int n, int[][] prereq, int[] order) {
        if (order.length != n) return false;
        int[] pos = new int[n];
        Arrays.fill(pos, -1);
        for (int i = 0; i < n; i++) { if (pos[order[i]] != -1) return false; pos[order[i]] = i; }
        for (int[] p : prereq) if (pos[p[1]] > pos[p[0]]) return false;
        return true;
    }

    public static void main(String[] args) {
        int[][] p1 = {{1, 0}};
        check(canFinish(2, p1) && isValidOrder(2, p1, findOrder(2, p1)));
        check(!canFinish(2, new int[][]{{1, 0}, {0, 1}}));
        int[][] p2 = {{1, 0}, {2, 0}, {3, 1}, {3, 2}};
        check(isValidOrder(4, p2, findOrder(4, p2)));
        check(!canFinish(1, new int[][]{{0, 0}}));                 // tự vòng
        check(!canFinish(4, new int[][]{{1, 0}, {2, 1}, {3, 2}, {1, 3}}));   // chu trình không chứa đỉnh 0
        check(canFinish(3, new int[0][]));
        check(findOrder(0, new int[0][]).length == 0 && canFinish(0, new int[0][]));

        Random rnd = new Random(13);
        for (int iter = 0; iter < 300; iter++) {
            int n = 1 + rnd.nextInt(12);
            List<Integer> perm = new ArrayList<>();
            for (int i = 0; i < n; i++) perm.add(i);
            Collections.shuffle(perm, rnd);
            List<int[]> edges = new ArrayList<>();
            for (int i = 0; i < n; i++)
                for (int j = i + 1; j < n; j++)
                    if (rnd.nextInt(4) == 0) edges.add(new int[]{perm.get(j), perm.get(i)});  // luôn là DAG
            int[][] pr = edges.toArray(new int[0][]);
            check(isValidOrder(n, pr, findOrder(n, pr)));
            if (!edges.isEmpty()) {                               // thêm cạnh ngược để tạo chu trình
                int[] e = edges.get(rnd.nextInt(edges.size()));
                edges.add(new int[]{e[1], e[0]});
                check(!canFinish(n, edges.toArray(new int[0][])));
            }
        }
        System.out.println("CourseSchedule OK");
    }

    static void check(boolean ok) { if (!ok) throw new AssertionError("test failed"); }
}
```

*Độ phức tạp:* O(V + E) time, O(V + E) space.

*Edge cases / test:* không có cạnh, tự vòng, chu trình không chứa đỉnh 0, cạnh trùng (in-degree đếm 2 lần và giảm 2 lần — vẫn đúng), n = 0 — kèm test ngẫu nhiên: DAG luôn cho thứ tự hợp lệ, thêm cạnh ngược luôn phát hiện chu trình.

**Câu hỏi nối tiếp:**
- *Số học kỳ tối thiểu (học song song không giới hạn)?* — Kahn theo **tầng** (như BFS level order), đếm số tầng.
- *Chỉ ra chu trình cụ thể?* — DFS ba màu, khi gặp đỉnh xám thì lần ngược stack.
- *Ứng dụng thực tế?* — Thứ tự build module Maven/Gradle (reactor), thứ tự chạy migration/task DAG (Airflow), Spring khởi tạo bean theo phụ thuộc và báo lỗi circular dependency.

**⚠️ Câu trả lời gây điểm trừ:** đảo chiều cạnh (b → a) rồi trả về thứ tự ngược mà không nhận ra; DFS chỉ dùng `visited` hai trạng thái (báo nhầm chu trình với đồ thị hình kim cương).

**📖 Ôn lại:** [13.3 Topological sort & phát hiện chu trình](../01-giao-trinh/17-data-structures-algorithms.md#133-topological-sort--phát-hiện-chu-trình)

</details>

### Q27. 🔴 Thời gian lan tín hiệu trong mạng (Network Delay Time — LeetCode 743) — Dijkstra

**Đề bài:** Có `n` node (1..n), cạnh có hướng `times[i] = [u, v, w]` (gửi từ u tới v mất w ≥ 0). Gửi tín hiệu từ `k`; bao lâu thì mọi node nhận được? Không thể thì `-1`. Ví dụ: `[[2,1,1],[2,3,1],[3,4,1]], n = 4, k = 2` → `2`.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Đáp án = khoảng cách ngắn nhất **lớn nhất** từ `k` → bài shortest path một nguồn, trọng số không âm → **Dijkstra** với `PriorityQueue` và **lazy deletion** (bỏ qua bản ghi cũ có khoảng cách lớn hơn `dist` hiện tại). O((V + E) log V).

**Giải thích chi tiết:**

*Câu hỏi làm rõ nên hỏi:* Trọng số có âm không (âm → Bellman-Ford)? Có cạnh song song/tự vòng? Đồ thị thưa hay dày (dày → Dijkstra O(V²) với mảng)? Tổng trọng số có thể tràn `int`?

*Brute force → tối ưu:* Bellman-Ford O(V·E); Floyd–Warshall O(V³) (mọi cặp, dùng làm oracle khi test). Dijkstra đúng vì với trọng số không âm, đỉnh có `dist` nhỏ nhất trong heap đã **chốt** (không đường nào khác ngắn hơn). Java `PriorityQueue` không có decrease-key → đẩy bản ghi mới, bỏ bản cũ khi poll.

```java
import java.util.*;

public class NetworkDelayTime {
    public static long networkDelayTime(int[][] times, int n, int k) {
        List<List<int[]>> adj = new ArrayList<>(n + 1);
        for (int i = 0; i <= n; i++) adj.add(new ArrayList<>());
        for (int[] t : times) adj.get(t[0]).add(new int[]{t[1], t[2]});

        long[] dist = new long[n + 1];
        Arrays.fill(dist, Long.MAX_VALUE);
        dist[k] = 0;
        PriorityQueue<long[]> pq = new PriorityQueue<>(Comparator.comparingLong(e -> e[1]));  // {node, dist}
        pq.offer(new long[]{k, 0});
        while (!pq.isEmpty()) {
            long[] cur = pq.poll();
            int u = (int) cur[0];
            if (cur[1] > dist[u]) continue;               // lazy deletion: bản ghi lỗi thời
            for (int[] e : adj.get(u)) {
                long nd = cur[1] + e[1];
                if (nd < dist[e[0]]) {
                    dist[e[0]] = nd;
                    pq.offer(new long[]{e[0], nd});
                }
            }
        }
        long max = 0;
        for (int i = 1; i <= n; i++) {
            if (dist[i] == Long.MAX_VALUE) return -1;     // có node không tới được
            max = Math.max(max, dist[i]);
        }
        return max;
    }

    /** Oracle cho test: Floyd–Warshall O(n^3). */
    static long floydWarshall(int[][] times, int n, int k) {
        long inf = Long.MAX_VALUE / 4;
        long[][] d = new long[n + 1][n + 1];
        for (long[] row : d) Arrays.fill(row, inf);
        for (int i = 1; i <= n; i++) d[i][i] = 0;
        for (int[] t : times) d[t[0]][t[1]] = Math.min(d[t[0]][t[1]], t[2]);
        for (int m = 1; m <= n; m++)
            for (int i = 1; i <= n; i++)
                for (int j = 1; j <= n; j++)
                    if (d[i][m] + d[m][j] < d[i][j]) d[i][j] = d[i][m] + d[m][j];
        long max = 0;
        for (int i = 1; i <= n; i++) { if (d[k][i] >= inf) return -1; max = Math.max(max, d[k][i]); }
        return max;
    }

    public static void main(String[] args) {
        check(networkDelayTime(new int[][]{{2, 1, 1}, {2, 3, 1}, {3, 4, 1}}, 4, 2) == 2);
        check(networkDelayTime(new int[][]{{1, 2, 1}}, 2, 1) == 1);
        check(networkDelayTime(new int[][]{{1, 2, 1}}, 2, 2) == -1);
        check(networkDelayTime(new int[][]{{1, 2, 10}, {1, 3, 1}, {3, 2, 1}}, 3, 1) == 2);   // đường vòng ngắn hơn
        check(networkDelayTime(new int[0][], 1, 1) == 0);
        check(networkDelayTime(new int[][]{{1, 2, 2_000_000_000}, {2, 3, 2_000_000_000}}, 3, 1) == 4_000_000_000L);
        Random rnd = new Random(21);
        for (int iter = 0; iter < 300; iter++) {
            int n = 1 + rnd.nextInt(8);
            int m = rnd.nextInt(n * n + 1);
            int[][] times = new int[m][];
            for (int i = 0; i < m; i++) times[i] = new int[]{1 + rnd.nextInt(n), 1 + rnd.nextInt(n), rnd.nextInt(10)};
            int k = 1 + rnd.nextInt(n);
            check(networkDelayTime(times, n, k) == floydWarshall(times, n, k));
        }
        System.out.println("NetworkDelayTime OK");
    }

    static void check(boolean ok) { if (!ok) throw new AssertionError("test failed"); }
}
```

*Độ phức tạp:* O((V + E) log E) ≈ O((V + E) log V) time với lazy deletion (heap có thể chứa tới E bản ghi), O(V + E) space.

*Edge cases / test:* node không tới được, n = 1, trọng số 0, cạnh song song, đường gián tiếp ngắn hơn cạnh trực tiếp, tổng vượt `int` — đối chiếu ngẫu nhiên với Floyd–Warshall.

**Câu hỏi nối tiếp:**
- *Vì sao Dijkstra sai với cạnh âm?* — Đỉnh đã chốt có thể được cải thiện qua cạnh âm sau đó; dùng Bellman-Ford (phát hiện chu trình âm) hoặc Johnson cho mọi cặp.
- *Cheapest Flights Within K Stops (LeetCode 787)?* — Trạng thái phải gồm (node, số chặng); Bellman-Ford k+1 vòng hoặc Dijkstra trên trạng thái mở rộng.
- *Lưới/đồ thị trọng số 0/1?* — 0-1 BFS với deque, O(V + E).
- *Thực tế?* — Định tuyến (OSPF), tính ETA giao hàng; đồ thị lớn dùng A*, Contraction Hierarchies.

**⚠️ Câu trả lời gây điểm trừ:** dùng `visited` đánh dấu khi **push** thay vì khi poll (sai kết quả); `PriorityQueue.remove(Object)` để mô phỏng decrease-key (O(n) mỗi lần); không biết điều kiện trọng số không âm.

**📖 Ôn lại:** [13.5 Dijkstra](../01-giao-trinh/17-data-structures-algorithms.md#135-dijkstra-clrs-ch24)

</details>

---

<a id="g8"></a>
## 8. Intervals & Dynamic Programming

### Q28. 🟡 Gộp các khoảng (Merge Intervals — LeetCode 56)

**Đề bài:** Gộp mọi khoảng chồng nhau. Ví dụ: `[[1,3],[2,6],[8,10],[15,18]]` → `[[1,6],[8,10],[15,18]]`; `[[1,4],[4,5]]` → `[[1,5]]`.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **Sort theo điểm đầu**, duyệt và so với khoảng cuối trong kết quả: nếu `start <= lastEnd` thì gộp (`lastEnd = max(lastEnd, end)`), ngược lại thêm khoảng mới. O(n log n).

**Giải thích chi tiết:**

*Câu hỏi làm rõ nên hỏi:* Khoảng đóng hay nửa mở — `[1,4]` và `[4,5]` có chạm là gộp không (với lịch họp `[9h,10h)` và `[10h,11h)` thường **không** gộp)? Input đã sort chưa? Được sửa input không?

*Brute force → tối ưu:* So mọi cặp và gộp lặp tới khi ổn định → O(n²) hoặc tệ hơn. Sau khi sort theo điểm đầu, các khoảng chồng nhau nằm **liên tiếp** → một lượt quét.

```java
import java.util.*;

public class MergeIntervals {
    public static int[][] merge(int[][] intervals) {
        int[][] sorted = intervals.clone();                       // không đổi thứ tự mảng của caller
        Arrays.sort(sorted, Comparator.comparingInt(a -> a[0]));  // không dùng a[0] - b[0] (tràn số)
        List<int[]> merged = new ArrayList<>();
        for (int[] in : sorted) {
            int[] last = merged.isEmpty() ? null : merged.get(merged.size() - 1);
            if (last != null && in[0] <= last[1]) {
                last[1] = Math.max(last[1], in[1]);               // last là mảng mới tạo, sửa an toàn
            } else {
                merged.add(new int[]{in[0], in[1]});              // copy, không giữ tham chiếu input
            }
        }
        return merged.toArray(new int[0][]);
    }

    /** Oracle: tô phủ trên tọa độ nhân đôi (để [1,2] và [3,4] không bị coi là liền nhau). */
    static int[][] bruteForce(int[][] intervals, int maxCoord) {
        boolean[] covered = new boolean[2 * maxCoord + 2];
        for (int[] in : intervals) for (int x = 2 * in[0]; x <= 2 * in[1]; x++) covered[x] = true;
        List<int[]> res = new ArrayList<>();
        for (int x = 0; x < covered.length; x++) {
            if (!covered[x]) continue;
            int start = x;
            while (x + 1 < covered.length && covered[x + 1]) x++;
            res.add(new int[]{start / 2, x / 2});
        }
        return res.toArray(new int[0][]);
    }

    public static void main(String[] args) {
        check(Arrays.deepEquals(merge(new int[][]{{1, 3}, {2, 6}, {8, 10}, {15, 18}}), new int[][]{{1, 6}, {8, 10}, {15, 18}}));
        check(Arrays.deepEquals(merge(new int[][]{{1, 4}, {4, 5}}), new int[][]{{1, 5}}));
        check(Arrays.deepEquals(merge(new int[][]{{1, 4}, {2, 3}}), new int[][]{{1, 4}}));     // nằm trọn bên trong
        check(Arrays.deepEquals(merge(new int[][]{{1, 4}, {0, 4}}), new int[][]{{0, 4}}));
        check(merge(new int[0][]).length == 0);
        int[][] input = {{5, 6}, {1, 2}};
        merge(input);
        check(input[0][0] == 5 && input[0][1] == 6);                                         // input giữ nguyên
        Random rnd = new Random(17);
        for (int iter = 0; iter < 500; iter++) {
            int[][] a = new int[rnd.nextInt(8)][];
            for (int i = 0; i < a.length; i++) {
                int s = rnd.nextInt(20);
                a[i] = new int[]{s, s + rnd.nextInt(5)};
            }
            check(Arrays.deepEquals(merge(a), bruteForce(a, 25)));
        }
        System.out.println("MergeIntervals OK");
    }

    static void check(boolean ok) { if (!ok) throw new AssertionError("test failed"); }
}
```

*Độ phức tạp:* O(n log n) time do sort, O(n) space cho kết quả (và O(log n)–O(n) cho sort của `Arrays.sort` với object — TimSort).

*Edge cases / test:* rỗng, một khoảng, chạm nhau, lồng nhau, chưa sort, điểm đơn `[2,2]`.

**Câu hỏi nối tiếp:**
- *Insert Interval (LeetCode 57) vào danh sách đã sort?* — O(n) không cần sort lại: thêm phần trước, gộp phần chồng, thêm phần sau.
- *Số phòng họp tối thiểu (Meeting Rooms II)?* — Sort điểm đầu + min-heap điểm kết thúc, hoặc sweep line với hai mảng start/end đã sort.
- *Thực tế?* — Gộp khung giờ bận khi tìm lịch rảnh, gộp dải IP/CIDR, gộp khoảng thời gian downtime để tính SLA.

**⚠️ Câu trả lời gây điểm trừ:** comparator `(a, b) -> a[0] - b[0]`; quên `max` khi gộp (sai với khoảng lồng nhau `[1,10],[2,3]`); sửa trực tiếp mảng con của input mà không nói.

**📖 Ôn lại:** [15.2 Intervals](../01-giao-trinh/17-data-structures-algorithms.md#152-intervals)

</details>

### Q29. 🟡 Đổi tiền với ít đồng nhất (Coin Change — LeetCode 322)

**Đề bài:** Cho mệnh giá `coins` (mỗi loại dùng không giới hạn) và `amount`. Tìm số đồng **ít nhất** để đổi đúng `amount`, không được thì `-1`. Ví dụ: `[1,2,5], 11` → `3` (5+5+1).

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** DP **unbounded knapsack**: `dp[x] = min(dp[x - c] + 1)` với mọi đồng `c ≤ x`, `dp[0] = 0`, khởi tạo "vô cực" bằng `amount + 1`. O(amount · số loại đồng). **Greedy sai** với hệ mệnh giá tổng quát: `[1,3,4], 6` greedy cho 4+1+1 = 3 đồng, tối ưu là 3+3 = 2.

**Giải thích chi tiết:**

*Câu hỏi làm rõ nên hỏi:* Số lượng mỗi loại có giới hạn không (bounded → khác bài)? `amount` lớn cỡ nào (10⁴ thì DP mảng ổn; 10⁹ thì không)? Cần cả danh sách đồng cụ thể không?

*Brute force → tối ưu:* Đệ quy thử mọi đồng → lũy thừa. Nhận ra **bài toán con chồng lấp** (`minCoins(x)` được tính lại nhiều lần) và **cấu trúc con tối ưu** → memo top-down hoặc bảng bottom-up. Có thể nhìn như BFS trên đồ thị đỉnh 0..amount, cạnh = một đồng (đường ngắn nhất theo số cạnh).

```java
import java.util.*;

public class CoinChange {
    public static int coinChange(int[] coins, int amount) {
        int inf = amount + 1;                             // không đáp án nào dùng > amount đồng (mệnh giá >= 1)
        int[] dp = new int[amount + 1];
        Arrays.fill(dp, inf);
        dp[0] = 0;
        for (int x = 1; x <= amount; x++) {
            for (int c : coins) {
                if (c <= x && dp[x - c] + 1 < dp[x]) dp[x] = dp[x - c] + 1;
            }
        }
        return dp[amount] >= inf ? -1 : dp[amount];
    }

    /** Oracle: BFS trên các tổng 0..amount. */
    static int bfs(int[] coins, int amount) {
        int[] steps = new int[amount + 1];
        Arrays.fill(steps, -1);
        steps[0] = 0;
        Deque<Integer> q = new ArrayDeque<>(List.of(0));
        while (!q.isEmpty()) {
            int x = q.poll();
            for (int c : coins) {
                int y = x + c;
                if (y <= amount && steps[y] == -1) { steps[y] = steps[x] + 1; q.offer(y); }
            }
        }
        return steps[amount];
    }

    public static void main(String[] args) {
        check(coinChange(new int[]{1, 2, 5}, 11) == 3);
        check(coinChange(new int[]{2}, 3) == -1);
        check(coinChange(new int[]{1}, 0) == 0);
        check(coinChange(new int[]{1, 3, 4}, 6) == 2);                 // greedy cho 3
        check(coinChange(new int[]{186, 419, 83, 408}, 6249) == 20);
        Random rnd = new Random(29);
        for (int iter = 0; iter < 300; iter++) {
            int[] coins = new int[1 + rnd.nextInt(4)];
            for (int i = 0; i < coins.length; i++) coins[i] = 1 + rnd.nextInt(15);
            int amount = rnd.nextInt(120);
            check(coinChange(coins, amount) == bfs(coins, amount));
        }
        System.out.println("CoinChange OK");
    }

    static void check(boolean ok) { if (!ok) throw new AssertionError("test failed"); }
}
```

*Độ phức tạp:* O(amount · k) time (k = số loại đồng), O(amount) space.

*Edge cases / test:* `amount = 0` → 0, không đổi được → -1, mệnh giá lớn hơn amount, phản ví dụ greedy — đối chiếu ngẫu nhiên với BFS.

**Câu hỏi nối tiếp:**
- *Đếm số cách đổi (Coin Change II — LeetCode 518)?* — Vòng ngoài là **đồng**, vòng trong là tổng (đếm tổ hợp); đảo thứ tự vòng lặp sẽ đếm **chỉnh hợp** (1+2 và 2+1 tính hai lần).
- *Khi nào greedy đúng?* — Hệ mệnh giá "canonical" như tiền VND/USD; nhưng không thể giả định cho input tùy ý.
- *Truy vết các đồng đã dùng?* — Lưu `choice[x]` = đồng tạo ra `dp[x]`, lần ngược từ `amount`.
- *Vì sao `inf = amount + 1` thay vì `Integer.MAX_VALUE`?* — `MAX_VALUE + 1` tràn thành số âm, phá phép `min`.

**⚠️ Câu trả lời gây điểm trừ:** đề xuất greedy "lấy đồng lớn nhất trước"; dùng `Integer.MAX_VALUE` rồi `+ 1`; đệ quy không memo.

**📖 Ôn lại:** [16.2 Các bài DP kinh điển](../01-giao-trinh/17-data-structures-algorithms.md#162-các-bài-dp-kinh-điển)

</details>

### Q30. 🔴 Dãy con tăng dài nhất (Longest Increasing Subsequence — LeetCode 300) — O(n log n)

**Đề bài:** Tìm độ dài dãy con (không cần liên tiếp) **tăng nghiêm ngặt** dài nhất. Ví dụ: `[10,9,2,5,3,7,101,18]` → `4` (`2,3,7,101`). Yêu cầu: sau khi có O(n²), tối ưu xuống O(n log n).

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** O(n²): `dp[i] = 1 + max(dp[j])` với `j < i`, `a[j] < a[i]`. O(n log n): duy trì mảng `tails`, trong đó `tails[len]` là **phần tử kết thúc nhỏ nhất** của mọi dãy tăng độ dài `len + 1`; `tails` luôn tăng nên với mỗi `x` binary search **lower bound** để thay thế hoặc nối dài. Độ dài `tails` là đáp án (bản thân `tails` **không** phải là một LIS).

**Giải thích chi tiết:**

*Câu hỏi làm rõ nên hỏi:* Tăng nghiêm ngặt hay không giảm (đổi lower bound thành upper bound)? Cần độ dài hay cả dãy cụ thể? n tới 10⁵ (O(n²) = 10¹⁰ → không đủ)?

*Brute force → tối ưu:*
1. Thử mọi tập con → O(2ⁿ).
2. DP O(n²): đủ cho n ≤ 5000.
3. Patience sorting: giữ cho mỗi độ dài đuôi nhỏ nhất (đuôi càng nhỏ càng dễ nối thêm) → `tails` đơn điệu → binary search.

```java
import java.util.*;

public class LongestIncreasingSubsequence {
    /** O(n log n): tails[i] = phần tử cuối nhỏ nhất của các dãy tăng độ dài i + 1. */
    public static int lengthOfLIS(int[] nums) {
        int[] tails = new int[nums.length];
        int size = 0;
        for (int x : nums) {
            int lo = 0, hi = size;                    // lower bound: vị trí đầu tiên có tails[i] >= x
            while (lo < hi) {
                int mid = (lo + hi) >>> 1;
                if (tails[mid] < x) lo = mid + 1; else hi = mid;
            }
            tails[lo] = x;                            // thay thế (đuôi nhỏ hơn) hoặc nối dài khi lo == size
            if (lo == size) size++;
        }
        return size;
    }

    /** O(n^2) DP — lời giải đầu tiên nên trình bày, cũng dùng làm oracle. */
    static int lengthOfLISQuadratic(int[] nums) {
        int[] dp = new int[nums.length];
        int best = 0;
        for (int i = 0; i < nums.length; i++) {
            dp[i] = 1;
            for (int j = 0; j < i; j++) if (nums[j] < nums[i]) dp[i] = Math.max(dp[i], dp[j] + 1);
            best = Math.max(best, dp[i]);
        }
        return best;
    }

    public static void main(String[] args) {
        check(lengthOfLIS(new int[]{10, 9, 2, 5, 3, 7, 101, 18}) == 4);
        check(lengthOfLIS(new int[]{0, 1, 0, 3, 2, 3}) == 4);
        check(lengthOfLIS(new int[]{7, 7, 7, 7}) == 1);                 // nghiêm ngặt
        check(lengthOfLIS(new int[]{}) == 0);
        check(lengthOfLIS(new int[]{Integer.MIN_VALUE, Integer.MAX_VALUE}) == 2);
        Random rnd = new Random(30);
        for (int iter = 0; iter < 1000; iter++) {
            int[] a = new int[rnd.nextInt(25)];
            for (int i = 0; i < a.length; i++) a[i] = rnd.nextInt(12) - 6;
            check(lengthOfLIS(a) == lengthOfLISQuadratic(a));
        }
        int[] inc = new int[200_000];
        for (int i = 0; i < inc.length; i++) inc[i] = i;
        check(lengthOfLIS(inc) == 200_000);                             // O(n^2) sẽ mất rất lâu
        System.out.println("LongestIncreasingSubsequence OK");
    }

    static void check(boolean ok) { if (!ok) throw new AssertionError("test failed"); }
}
```

*Độ phức tạp:* O(n log n) time, O(n) space (bản DP O(n²)/O(n)).

*Edge cases / test:* rỗng, toàn bằng nhau, giảm dần (đáp án 1), tăng dần dài, giá trị cực trị — đối chiếu ngẫu nhiên với bản O(n²).

**Câu hỏi nối tiếp:**
- *Khôi phục dãy cụ thể?* — Lưu `tailIndex[len]` (chỉ số phần tử tại `tails[len]`) và `parent[i]` (chỉ số phần tử đứng trước i), lần ngược từ `tailIndex[size-1]`.
- *Không giảm (cho phép bằng)?* — Đổi sang upper bound (`tails[mid] <= x` thì đi phải).
- *Russian Doll Envelopes (LeetCode 354)?* — Sort theo rộng tăng, **cao giảm** khi rộng bằng nhau, rồi LIS trên chiều cao.
- *Dùng `Arrays.binarySearch` được không?* — Được vì `tails` tăng nghiêm ngặt (không trùng), lấy `-(i + 1)` khi không tìm thấy; nhưng tự viết lower bound rõ ý hơn khi phỏng vấn.

**⚠️ Câu trả lời gây điểm trừ:** nói `tails` chính là LIS; dùng upper bound cho bài tăng nghiêm ngặt (`[7,7,7]` cho 3); không trình bày được vì sao `tails` luôn tăng.

**📖 Ôn lại:** [16. Dynamic Programming](../01-giao-trinh/17-data-structures-algorithms.md#16-dynamic-programming) · [6. Binary Search](../01-giao-trinh/17-data-structures-algorithms.md#6-binary-search-kể-cả-trên-không-gian-đáp-án)

</details>

---

<a id="g9"></a>
## 9. Bài thực chiến của Senior

> Hai bài dưới đây thường xuất hiện ở vòng "coding + design" của Senior: không chỉ cần đúng thuật toán mà còn phải nói về **API, generic, thread-safety, clock, bộ nhớ, và chạy trên nhiều instance**.

### Q31. 🔴🎬 Thiết kế LRU Cache (LeetCode 146) — generic, O(1)

**Đề bài:** Cài đặt `LruCache<K, V>` dung lượng `capacity` với `get(key)` và `put(key, value)` đều **O(1)**. Khi đầy, `put` key mới sẽ loại key **ít được dùng gần đây nhất**. `get` cũng tính là "dùng". Sau đó: dùng thư viện chuẩn thì viết thế nào? Thread-safe thì sao?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `HashMap<K, Node>` để tra O(1) + **doubly linked list** sắp theo độ mới (đầu = mới nhất, cuối = cũ nhất) để di chuyển/xóa node O(1). Dùng **hai sentinel** `head`/`tail` để bỏ mọi nhánh `null`. Trong JDK: `LinkedHashMap(capacity, 0.75f, true)` (access order) + override `removeEldestEntry`. Production: dùng **Caffeine** thay vì tự viết.

**Giải thích chi tiết:**

*Câu hỏi làm rõ nên hỏi:* `get` key không có trả gì (`null`, `Optional`, -1)? `put` key đã có có tính là "dùng" không (thường có)? Có cho phép value `null`? Cần thread-safe không? Có TTL không? Capacity theo số phần tử hay theo byte?

*Brute force → tối ưu:* `ArrayList` theo thứ tự dùng → di chuyển O(n). `HashMap` + timestamp → loại phần tử phải quét O(n), hoặc `TreeMap` theo timestamp O(log n). HashMap + doubly linked list cho cả hai thao tác O(1); node lưu cả `key` để khi loại node cuối còn xóa được khỏi map.

```java
import java.util.*;

public class LruCache<K, V> {
    private static final class Node<K, V> {
        final K key;
        V value;
        Node<K, V> prev, next;
        Node(K key, V value) { this.key = key; this.value = value; }
    }

    private final int capacity;
    private final Map<K, Node<K, V>> index;
    private final Node<K, V> head = new Node<>(null, null);   // sentinel: head.next = mới dùng nhất
    private final Node<K, V> tail = new Node<>(null, null);   // sentinel: tail.prev = cũ nhất

    public LruCache(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("capacity must be > 0");
        this.capacity = capacity;
        this.index = new HashMap<>((int) (capacity / 0.75f) + 1);   // tránh resize
        head.next = tail;
        tail.prev = head;
    }

    /** Trả về null nếu không có (giả định không lưu value null). */
    public V get(K key) {
        Node<K, V> node = index.get(key);
        if (node == null) return null;
        moveToFront(node);
        return node.value;
    }

    public void put(K key, V value) {
        Node<K, V> node = index.get(key);
        if (node != null) {                       // cập nhật + tính là vừa dùng
            node.value = value;
            moveToFront(node);
            return;
        }
        if (index.size() == capacity) {           // loại phần tử cũ nhất
            Node<K, V> eldest = tail.prev;
            unlink(eldest);
            index.remove(eldest.key);
        }
        node = new Node<>(key, value);
        index.put(key, node);
        addFirst(node);
    }

    public int size() { return index.size(); }

    /** Thứ tự từ mới dùng nhất tới cũ nhất — phục vụ test/debug. */
    public List<K> keysFromMostRecent() {
        List<K> keys = new ArrayList<>(index.size());
        for (Node<K, V> n = head.next; n != tail; n = n.next) keys.add(n.key);
        return keys;
    }

    private void moveToFront(Node<K, V> node) { unlink(node); addFirst(node); }

    private void unlink(Node<K, V> node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }

    private void addFirst(Node<K, V> node) {
        node.next = head.next;
        node.prev = head;
        head.next.prev = node;
        head.next = node;
    }

    // ---------- Bản dùng thư viện chuẩn, làm oracle khi test ----------
    static final class JdkLru<K, V> extends LinkedHashMap<K, V> {
        private static final long serialVersionUID = 1L;
        private final int capacity;
        JdkLru(int capacity) { super(16, 0.75f, true); this.capacity = capacity; }   // accessOrder = true
        @Override protected boolean removeEldestEntry(Map.Entry<K, V> eldest) { return size() > capacity; }
    }

    public static void main(String[] args) {
        LruCache<Integer, Integer> c = new LruCache<>(2);   // ví dụ LeetCode 146
        c.put(1, 1);
        c.put(2, 2);
        check(c.get(1) == 1);
        c.put(3, 3);                                         // loại 2
        check(c.get(2) == null);
        c.put(4, 4);                                         // loại 1
        check(c.get(1) == null);
        check(c.get(3) == 3 && c.get(4) == 4);
        check(c.keysFromMostRecent().equals(List.of(4, 3)));

        Random rnd = new Random(31);
        for (int capacity : new int[]{1, 2, 5, 50}) {
            LruCache<Integer, Integer> mine = new LruCache<>(capacity);
            JdkLru<Integer, Integer> ref = new JdkLru<>(capacity);
            for (int op = 0; op < 100_000; op++) {
                int key = rnd.nextInt(capacity * 3);
                if (rnd.nextBoolean()) {
                    check(Objects.equals(mine.get(key), ref.get(key)));
                } else {
                    int value = rnd.nextInt();
                    mine.put(key, value);
                    ref.put(key, value);
                }
                check(mine.size() == ref.size());
            }
            List<Integer> expectedOrder = new ArrayList<>(ref.keySet());   // LinkedHashMap: cũ nhất trước
            Collections.reverse(expectedOrder);
            check(mine.keysFromMostRecent().equals(expectedOrder));
        }
        System.out.println("LruCache OK");
    }

    static void check(boolean ok) { if (!ok) throw new AssertionError("test failed"); }
}
```

*Độ phức tạp:* `get`/`put` O(1) trung bình; O(capacity) bộ nhớ.

*Edge cases / test:* capacity 1, `put` key đã có (không được loại ai), `get` làm thay đổi thứ tự, capacity ≤ 0 → exception — kèm 100k thao tác ngẫu nhiên đối chiếu với `LinkedHashMap` access-order, so cả kết quả lẫn thứ tự cuối.

**Câu hỏi nối tiếp:**
- *Làm thread-safe?* — Đơn giản nhất: `synchronized` mọi method (hoặc `Collections.synchronizedMap` quanh `LinkedHashMap`) — đúng nhưng **mọi `get` cũng là thao tác ghi** (đổi thứ tự) nên `ReadWriteLock` **không** giúp. Scale tốt hơn: chia **segment/shard** theo hash key, mỗi shard một LRU + lock riêng (LRU xấp xỉ toàn cục). Production: **Caffeine** — ghi nhận truy cập vào ring buffer, xử lý bất đồng bộ theo lô, chính sách **W-TinyLFU** cho hit rate tốt hơn LRU thuần với traffic thực tế.
- *Thêm TTL?* — Lưu `expireAt` trong node, kiểm tra lazily khi `get` + dọn định kỳ; hoặc dùng `expireAfterWrite` của Caffeine.
- *Cache phân tán?* — Redis với `maxmemory-policy allkeys-lru` (Redis dùng **LRU xấp xỉ** bằng lấy mẫu, không phải danh sách liên kết thật).
- *LFU (LeetCode 460)?* — Map key → node, map tần suất → doubly linked list, theo dõi `minFreq`, vẫn O(1).

**⚠️ Câu trả lời gây điểm trừ:** chỉ dùng `LinkedHashMap` mặc định (insertion order — không phải LRU); node không lưu key (không xóa được khỏi map khi loại); đề xuất `ReadWriteLock` cho `get`; dùng `ConcurrentHashMap` + `ConcurrentLinkedDeque` rồi nghĩ là đã thread-safe (hai cấu trúc cập nhật không nguyên tử với nhau).

**📖 Ôn lại:** [18.1 LRU Cache](../01-giao-trinh/17-data-structures-algorithms.md#181-lru-cache-leetcode-146)

</details>

### Q32. 🔴🎬 Rate limiter theo từng client (Token Bucket, thread-safe)

**Đề bài:** Viết `TokenBucketRateLimiter` với `boolean tryAcquire(String clientKey)`: mỗi client có bucket chứa tối đa `capacity` token, nạp lại `refillPerSecond` token/giây; mỗi request tiêu 1 token. Yêu cầu: thread-safe khi nhiều thread gọi cùng lúc, test được mà không phải `Thread.sleep`, không rò bộ nhớ khi có hàng triệu client.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Mỗi key một bucket `(tokens, lastRefill)`, **nạp lười** (lazy refill) khi có request: `tokens = min(capacity, tokens + elapsed / nanosPerToken)`. Thread-safety: cập nhật bucket **nguyên tử theo từng key** bằng `ConcurrentHashMap.compute` (khóa ở mức bin, các key khác không chặn nhau). Clock **inject** được (`LongSupplier`) để test bằng đồng hồ giả. Dọn bucket nhàn rỗi (đã đầy) định kỳ để không rò bộ nhớ. Nhiều instance → chuyển trạng thái sang Redis + Lua script.

**Giải thích chi tiết:**

*Câu hỏi làm rõ nên hỏi:* Giới hạn theo gì (user, API key, IP, endpoint)? Cho phép **burst** không (token bucket cho burst tới `capacity`; leaky bucket làm mượt)? Chạy một instance hay cluster? Khi bị chặn trả gì (HTTP 429 + `Retry-After`)? Độ chính xác cần thế nào (fixed window có hiện tượng gấp đôi ở biên cửa sổ)?

*So sánh thuật toán:*

| Thuật toán | Bộ nhớ/key | Burst | Nhược điểm |
|---|---|---|---|
| Fixed window counter | 1 counter | Có thể gấp đôi ở biên | Không mượt |
| Sliding window log | O(số request) | Chính xác | Tốn bộ nhớ |
| Sliding window counter | 2 counter | Xấp xỉ tốt | Ước lượng |
| **Token bucket** | 2 số | Có, tới `capacity` | Cần clock đơn điệu |

*Các quyết định thiết kế:*
- **Số nguyên, không dùng `double`**: lưu token nguyên + `lastRefill` chỉ tiến đúng bằng số token đã nạp × `nanosPerToken` → không mất phần lẻ thời gian, không sai số dấu phẩy động.
- **`System.nanoTime()`** chứ không `currentTimeMillis()` (đồng hồ hệ thống có thể nhảy lùi khi NTP chỉnh).
- Đọc clock **bên trong** `compute` để thời gian và trạng thái nhất quán dưới cạnh tranh.
- Bucket là `record` bất biến; `compute` thay bằng bản mới.

```java
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.LongSupplier;

public class TokenBucketRateLimiter {
    private record Bucket(long tokens, long lastRefillNanos) {}

    private final long capacity;
    private final long nanosPerToken;
    private final LongSupplier nanoClock;
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public TokenBucketRateLimiter(long capacity, long refillPerSecond) {
        this(capacity, refillPerSecond, System::nanoTime);
    }

    TokenBucketRateLimiter(long capacity, long refillPerSecond, LongSupplier nanoClock) {
        if (capacity <= 0 || refillPerSecond <= 0 || refillPerSecond > 1_000_000_000L)
            throw new IllegalArgumentException("invalid capacity/refill rate");
        this.capacity = capacity;
        this.nanosPerToken = 1_000_000_000L / refillPerSecond;
        this.nanoClock = nanoClock;
    }

    public boolean tryAcquire(String clientKey) {
        boolean[] allowed = new boolean[1];
        buckets.compute(clientKey, (k, old) -> {         // nguyên tử theo từng key
            long now = nanoClock.getAsLong();
            Bucket b = (old == null) ? new Bucket(capacity, now) : refill(old, now);
            if (b.tokens() > 0) {
                allowed[0] = true;
                return new Bucket(b.tokens() - 1, b.lastRefillNanos());
            }
            return b;
        });
        return allowed[0];
    }

    private Bucket refill(Bucket b, long now) {
        long elapsed = now - b.lastRefillNanos();          // so sánh hiệu nanoTime, không so trực tiếp
        if (elapsed < nanosPerToken) return b;
        long newTokens = elapsed / nanosPerToken;
        long tokens = b.tokens() + newTokens;
        if (newTokens >= capacity || tokens >= capacity) return new Bucket(capacity, now);   // đầy: bỏ phần dư
        return new Bucket(tokens, b.lastRefillNanos() + newTokens * nanosPerToken);         // giữ phần lẻ thời gian
    }

    /** Xóa bucket đã đầy (client nhàn rỗi) — gọi định kỳ từ scheduler. Trả về số bucket đã xóa. */
    public int evictIdle() {
        int removed = 0;
        for (String key : buckets.keySet()) {
            boolean[] gone = new boolean[1];
            buckets.computeIfPresent(key, (k, b) -> {
                if (refill(b, nanoClock.getAsLong()).tokens() >= capacity) { gone[0] = true; return null; }
                return b;
            });
            if (gone[0]) removed++;
        }
        return removed;
    }

    int trackedKeys() { return buckets.size(); }

    public static void main(String[] args) throws Exception {
        AtomicLong fakeNanos = new AtomicLong(1_000L);
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(3, 1, fakeNanos::get);   // burst 3, 1 token/s
        check(limiter.tryAcquire("alice") && limiter.tryAcquire("alice") && limiter.tryAcquire("alice"));
        check(!limiter.tryAcquire("alice"));                  // hết token
        check(limiter.tryAcquire("bob"));                     // key khác độc lập
        fakeNanos.addAndGet(500_000_000L);                    // 0.5s: chưa đủ 1 token
        check(!limiter.tryAcquire("alice"));
        fakeNanos.addAndGet(500_000_000L);                    // tổng 1s: đúng 1 token
        check(limiter.tryAcquire("alice"));
        check(!limiter.tryAcquire("alice"));
        fakeNanos.addAndGet(1_500_000_000L);                  // 1.5s -> 1 token, giữ lại 0.5s
        check(limiter.tryAcquire("alice"));
        fakeNanos.addAndGet(500_000_000L);                    // + 0.5s còn giữ = 1 token nữa
        check(limiter.tryAcquire("alice"));
        check(!limiter.tryAcquire("alice"));
        fakeNanos.addAndGet(100_000_000_000L);                // rất lâu: không vượt capacity
        int burst = 0;
        while (limiter.tryAcquire("alice")) burst++;
        check(burst == 3);
        fakeNanos.addAndGet(10_000_000_000L);                 // mọi bucket đã đầy lại
        check(limiter.trackedKeys() == 2 && limiter.evictIdle() == 2 && limiter.trackedKeys() == 0);

        // Cạnh tranh: clock đứng yên, 16 thread x 50 request -> đúng `capacity` request được phép.
        TokenBucketRateLimiter shared = new TokenBucketRateLimiter(100, 1, () -> 42L);
        ExecutorService pool = Executors.newFixedThreadPool(16);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger allowed = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();
        for (int t = 0; t < 16; t++) {
            futures.add(pool.submit(() -> {
                start.await();
                for (int i = 0; i < 50; i++) if (shared.tryAcquire("hot-key")) allowed.incrementAndGet();
                return null;
            }));
        }
        start.countDown();
        for (Future<?> f : futures) f.get();
        pool.shutdown();
        check(pool.awaitTermination(10, TimeUnit.SECONDS));
        check(allowed.get() == 100);
        System.out.println("TokenBucketRateLimiter OK");
    }

    static void check(boolean ok) { if (!ok) throw new AssertionError("test failed"); }
}
```

*Độ phức tạp:* `tryAcquire` O(1) trung bình; bộ nhớ O(số client đang hoạt động) nhờ `evictIdle`.

*Edge cases / test:* hết token, nạp một phần (giữ phần lẻ thời gian), không vượt `capacity` sau thời gian dài, key độc lập, dọn bucket nhàn rỗi, 16 thread tranh cùng một key với clock đứng yên → **đúng** 100 request được phép (bản `get` rồi `put` không nguyên tử sẽ cho nhiều hơn).

**Câu hỏi nối tiếp:**
- *Vì sao không `get` → tính → `put`?* — Race condition check-then-act: hai thread cùng thấy 1 token, cả hai đều được phép. `compute` (hoặc vòng CAS trên `AtomicReference<Bucket>`) giải quyết. Lưu ý hàm trong `compute` phải ngắn, không I/O, không đụng key khác của cùng map (có thể deadlock/`IllegalStateException`).
- *Chạy 10 instance sau load balancer?* — Giới hạn cục bộ sẽ thành 10× hạn mức. Đưa trạng thái ra **Redis**, thực hiện refill + trừ token trong **một Lua script** (nguyên tử trên Redis), dùng thời gian của Redis (`TIME`) để tránh lệch đồng hồ giữa các instance, đặt TTL cho key thay cho `evictIdle`. Trade-off: thêm một round-trip mạng mỗi request; có thể kết hợp hạn mức cục bộ + đồng bộ định kỳ (xấp xỉ).
- *Khi Redis chết?* — Chọn **fail-open** (cho qua, bảo vệ trải nghiệm) hay **fail-closed** (chặn, bảo vệ hệ thống phía sau) tùy endpoint; có circuit breaker và metric.
- *Thư viện có sẵn?* — Bucket4j (hỗ trợ Redis/Hazelcast), Resilience4j `RateLimiter`, Spring Cloud Gateway `RequestRateLimiter` (Redis), hoặc giới hạn ở tầng gateway/Nginx/Envoy.
- *Một tỷ client?* — `evictIdle` quét toàn bộ map tốn kém → dùng Caffeine với `expireAfterAccess` làm kho bucket, hoặc đẩy hoàn toàn sang Redis với TTL.

**⚠️ Câu trả lời gây điểm trừ:** `synchronized` toàn bộ method (một client nóng chặn mọi client khác); `Thread.sleep` trong test; dùng `System.currentTimeMillis()`; lưu token dạng `double` mà không bàn sai số; không nghĩ tới việc map phình vô hạn; không biết rằng limiter in-memory sai khi scale ngang.

**📖 Ôn lại:** [18.4 Rate limiter](../01-giao-trinh/17-data-structures-algorithms.md#184-rate-limiter)

</details>

---

## Checklist sau khi luyện xong module

- [ ] Giải lại được mỗi bài 🟢 trong ≤ 15 phút, 🟡 trong ≤ 30 phút mà không nhìn đáp án.
- [ ] Với mỗi bài, nói được brute force, lời giải tối ưu và **vì sao** tối ưu (bất biến của two pointers/sliding window, tính đơn điệu của binary search, amortized của monotonic stack).
- [ ] Luôn hỏi về overflow, input rỗng/`null`, phần tử trùng, có được sửa input không.
- [ ] Biết dùng đúng cấu trúc trong JDK: `ArrayDeque` (không `Stack`), `PriorityQueue` + `Comparator.comparingInt`, `HashMap.merge`, `TreeMap.floorKey/ceilingKey`.
- [ ] Trả lời được follow-up "dữ liệu là stream / không vừa RAM / nhiều thread / nhiều instance" cho các bài Top-K, Merge K, LRU, Rate limiter.

📖 Toàn bộ lý thuyết: [Module 17 — Cấu trúc dữ liệu & Giải thuật](../01-giao-trinh/17-data-structures-algorithms.md)

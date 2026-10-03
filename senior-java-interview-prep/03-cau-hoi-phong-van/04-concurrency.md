# Câu hỏi phỏng vấn — Module 04: Concurrency & Multithreading

> Giáo trình tương ứng: [Module 04 — Concurrency & Multithreading](../01-giao-trinh/04-concurrency.md)

**Cách dùng:** đọc câu hỏi, **tự trả lời thành tiếng** (như đang ngồi trước interviewer, khoảng 30–60 giây) rồi mới mở "Đáp án". So câu trả lời của bạn với phần *Trả lời ngắn* trước, sau đó đọc *Giải thích chi tiết* và tự trả lời tiếp các *Câu hỏi nối tiếp*. Câu nào trả lời vấp thì quay lại phần giáo trình ở dòng *📖 Ôn lại*.

**Mức độ:** 🟢 Cơ bản (phải trả lời trơn tru) · 🟡 Senior (câu hỏi chuẩn cho vị trí Senior) · 🔴 Xoáy sâu (interviewer đào sâu cơ chế, tình huống production). Các câu có nhãn **[Tình huống]** hoặc **[Đọc code]** là dạng câu hỏi thực tế hay gặp.

## Mục lục

1. [Thread cơ bản & interrupt](#nhom-a) — Q1–Q5
2. [Atomicity, visibility, ordering & Java Memory Model](#nhom-b) — Q6–Q12
3. [`synchronized`, `volatile` & double-checked locking](#nhom-c) — Q13–Q18
4. [wait/notify, `Lock`, `ReadWriteLock`, `StampedLock`](#nhom-d) — Q19–Q23
5. [Atomics, CAS, ABA, `LongAdder`](#nhom-e) — Q24–Q27
6. [Deadlock, starvation & thread dump](#nhom-f) — Q28–Q31
7. [`ExecutorService` & `ThreadPoolExecutor`](#nhom-g) — Q32–Q37
8. [`CompletableFuture` & `ForkJoinPool`](#nhom-h) — Q38–Q43
9. [Synchronizers & concurrent collections](#nhom-i) — Q44–Q49
10. [`ThreadLocal` & truyền context](#nhom-j) — Q50–Q51
11. [Virtual Threads, Structured Concurrency, Scoped Values](#nhom-k) — Q52–Q56
12. [Thiết kế & kiểm thử code đồng thời](#nhom-l) — Q57–Q58

---

<a id="nhom-a"></a>
## 1. Thread cơ bản & interrupt

### Q1. 🟢 Thread trong Java có những trạng thái nào? Một thread đang chờ `ReentrantLock` và một thread đang chờ đọc socket sẽ hiện trạng thái gì trong thread dump?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** 6 trạng thái của `Thread.State`: `NEW`, `RUNNABLE`, `BLOCKED`, `WAITING`, `TIMED_WAITING`, `TERMINATED`. Chỉ chờ monitor của `synchronized` mới là `BLOCKED`; chờ `ReentrantLock.lock()` là `WAITING (parking)` vì AQS dùng `LockSupport.park()`. Thread chờ đọc socket bằng I/O cổ điển lại hiện **`RUNNABLE`**, vì JVM không biết thread đang bị kernel chặn.

**Giải thích chi tiết:**
- `RUNNABLE` gộp cả "đang chạy" lẫn "sẵn sàng chạy". Java không có trạng thái "running" riêng.
- `WAITING`: `Object.wait()`, `Thread.join()`, `LockSupport.park()` (nền của mọi lock trong `java.util.concurrent`). `TIMED_WAITING`: các phiên bản có timeout (`sleep`, `wait(ms)`, `parkNanos`, `tryLock(timeout)`).
- Hệ quả khi đọc dump: 200 thread `RUNNABLE` trong `SocketInputStream.socketRead0` không có nghĩa là CPU đang bận. Thường đó là dấu hiệu downstream chậm hoặc **thiếu read timeout**.

**Câu hỏi nối tiếp:**
- *Thread đang `sleep()` có nhả monitor không?* → Không. `sleep` giữ nguyên mọi lock. `wait()` thì nhả monitor của chính object đó.
- *Làm sao phân biệt thread RUNNABLE thật sự đốt CPU?* → `top -H -p <pid>` lấy TID, đổi sang hex rồi tìm `nid=0x...` trong dump, hoặc dùng async-profiler.

**⚠️ Câu trả lời gây điểm trừ:**
- "Thread chờ lock nào cũng là BLOCKED."
- Kể thêm trạng thái "RUNNING" hay "READY", vốn là khái niệm của hệ điều hành chứ không phải của `Thread.State`.

**📖 Ôn lại:** [Phần 1 — Thread cơ bản](../01-giao-trinh/04-concurrency.md#p1)

</details>

### Q2. 🟢 Khác nhau giữa `start()` và `run()`? Gọi `start()` hai lần thì sao? Vì sao nên implement `Runnable` thay vì kế thừa `Thread`?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `start()` tạo một OS thread mới rồi JVM gọi `run()` trên thread đó. Gọi `run()` trực tiếp chỉ là một lời gọi method bình thường trên thread hiện tại, không có gì chạy song song. `start()` lần hai ném `IllegalThreadStateException`. Nên dùng `Runnable`/`Callable` để tách "việc cần làm" khỏi "cơ chế chạy". Nhờ vậy cùng một task có thể đưa cho executor, virtual thread hay chạy đồng bộ trong test.

**Giải thích chi tiết:**
- Kế thừa `Thread` khoá luôn lớp cha (Java đơn kế thừa) và trộn logic nghiệp vụ với quản lý vòng đời thread.
- Production gần như không `new Thread()` thủ công. Thường dùng `ExecutorService` hoặc, từ Java 21, `Thread.ofVirtual()` / `Executors.newVirtualThreadPerTaskExecutor()`.
- `Callable<V>` trả về giá trị và được phép ném checked exception. `Runnable` không làm được hai việc này.

**Câu hỏi nối tiếp:**
- *Có "giết" được một thread không?* → Không có cách an toàn. `Thread.stop()` deprecated từ 1.2 và ném `UnsupportedOperationException` từ Java 20. Muốn dừng thread thì dùng interrupt (huỷ hợp tác).

**⚠️ Câu trả lời gây điểm trừ:**
- "`run()` cũng chạy trên thread mới, chỉ là không đồng bộ."

**📖 Ôn lại:** [Phần 1.3 — Tạo thread](../01-giao-trinh/04-concurrency.md#p1)

</details>

### Q3. 🟡 **[Đọc code]** Đoạn code sau có vấn đề gì? Interrupt hoạt động thế nào và xử lý `InterruptedException` đúng ra sao?

```java
while (!Thread.currentThread().isInterrupted()) {
    try {
        Job job = queue.take();
        process(job);
    } catch (InterruptedException e) {
        log.warn("interrupted", e);
    }
}
```

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Đây là lỗi **nuốt interrupt**. `take()` ném `InterruptedException` và đồng thời **xoá cờ interrupt**, nên điều kiện `while` vẫn thấy `false` và vòng lặp chạy tiếp mãi. Hậu quả là `shutdownNow()` không dừng được worker và graceful shutdown bị treo. Quy tắc là **ném tiếp hoặc khôi phục cờ** bằng `Thread.currentThread().interrupt()`, sau đó thoát vòng lặp.

**Giải thích chi tiết:**
- `t.interrupt()` chỉ đặt một cờ. Thread đích phải tự kiểm tra cờ và tự dừng, gọi là cooperative cancellation.
- Các method blocking như `sleep`, `wait`, `join`, `BlockingQueue.take`, `lockInterruptibly`, `Future.get` ném `InterruptedException` và xoá cờ. `Thread.interrupted()` vừa đọc vừa **xoá** cờ, còn `isInterrupted()` chỉ đọc.
- Socket I/O cổ điển không phản hồi interrupt. Muốn huỷ thì phải đóng socket. NIO `InterruptibleChannel` thì có phản hồi.

```java
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();   // khôi phục cờ
    break;                                // hoặc return / ném tiếp
}
```

**Câu hỏi nối tiếp:**
- *Task CPU-bound không có lệnh blocking nào thì huỷ thế nào?* → Kiểm tra `isInterrupted()` định kỳ, ví dụ mỗi K vòng lặp.
- *Vì sao điều này quan trọng trên Kubernetes?* → Khi nhận SIGTERM, ứng dụng chỉ có `terminationGracePeriodSeconds` để dừng. Worker nuốt interrupt sẽ khiến pod bị SIGKILL và job đang chạy bị cắt ngang.

**⚠️ Câu trả lời gây điểm trừ:**
- Viết `catch (InterruptedException e) {}` hoặc chỉ log rồi chạy tiếp.
- Bọc thành `RuntimeException` mà không khôi phục cờ.

**📖 Ôn lại:** [Phần 1.4 — Interrupt](../01-giao-trinh/04-concurrency.md#p1)

</details>

### Q4. 🟡 **[Tình huống]** Service có 4 worker thread xử lý job từ queue. Hãy thiết kế graceful shutdown khi nhận SIGTERM. Daemon thread liên quan gì ở đây? `kill -9` thì sao?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Đăng ký shutdown hook (hoặc dùng `@PreDestroy`/`SmartLifecycle` trong Spring) để làm lần lượt: (1) ngừng nhận job mới, (2) gọi `executor.shutdown()` và `awaitTermination(timeout)` để chờ job đang chạy xong, (3) quá hạn thì `shutdownNow()` để interrupt và lấy danh sách job chưa chạy, rồi log hoặc trả chúng về nguồn. JVM thoát khi mọi **non-daemon** thread kết thúc. Daemon thread bị cắt ngang và `finally` của chúng không chắc được chạy. `kill -9` (SIGKILL) không chạy shutdown hook, nên job phải được thiết kế **idempotent / at-least-once**.

**Giải thích chi tiết:**

```java
Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    acceptingJobs = false;
    pool.shutdown();
    try {
        if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
            List<Runnable> pending = pool.shutdownNow();
            log.warn("Bỏ dở {} job", pending.size());
        }
    } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
}));
```

- `setDaemon` phải gọi trước `start()`. Thread con kế thừa trạng thái daemon của thread tạo ra nó.
- Queue trong bộ nhớ sẽ mất dữ liệu khi process chết. Job quan trọng cần nguồn sự thật bền vững như DB (outbox) hoặc broker.

**Câu hỏi nối tiếp:**
- *Spring Boot hỗ trợ gì sẵn?* → `server.shutdown=graceful` cùng `spring.lifecycle.timeout-per-shutdown-phase`. Executor do Spring quản lý dùng `setWaitForTasksToCompleteOnShutdown` và `setAwaitTerminationSeconds`.

**⚠️ Câu trả lời gây điểm trừ:**
- "Để daemon cho JVM tự thoát" với những thread đang ghi dữ liệu.
- Không nhắc tới timeout, khiến hook có thể treo mãi.

**📖 Ôn lại:** [Phần 1 — Bài 1.3 Graceful shutdown](../01-giao-trinh/04-concurrency.md#p1)

</details>

### Q5. 🟢 Exception không được bắt trong một thread thì chuyện gì xảy ra? Trong thread pool thì sao?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Thread đó chết. JVM gọi `UncaughtExceptionHandler`, mặc định chỉ in stack trace ra stderr, và các thread khác vẫn chạy bình thường. Trong `ThreadPoolExecutor`, task gửi bằng `execute()` làm worker chết và pool tạo worker thay thế. Task gửi bằng `submit()` thì exception bị **bọc vào `Future`**, và nếu không ai gọi `get()` thì exception biến mất không để lại dấu vết.

**Giải thích chi tiết:**
- Nên đặt `Thread.setDefaultUncaughtExceptionHandler` hoặc handler riêng qua `ThreadFactory` để log tập trung.
- Với `submit`, có ba cách bắt lỗi: try/catch ngay trong task, override `afterExecute` rồi kiểm tra `Future`, hoặc luôn gọi `get()`.
- `scheduleAtFixedRate`: nếu một lần chạy ném exception thì **mọi lần chạy sau bị huỷ** mà không báo gì. Task định kỳ nên bọc `try/catch (Throwable)`.

**Câu hỏi nối tiếp:**
- *Exception trong `CompletableFuture` thì sao?* → Lỗi đi theo pipeline. Nếu không ai `join`/`handle` thì nó cũng mất (xem Q40).

**⚠️ Câu trả lời gây điểm trừ:**
- "Exception trong thread con làm crash cả ứng dụng."
- "`submit` cũng log exception ra như `execute`."

**📖 Ôn lại:** [Phần 8.6 — Exception và vòng đời](../01-giao-trinh/04-concurrency.md#p8)

</details>

---

<a id="nhom-b"></a>
## 2. Atomicity, visibility, ordering & Java Memory Model

### Q6. 🟢 Bug concurrency quy về những vấn đề cốt lõi nào? Cho ví dụ mỗi loại.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Có ba vấn đề. **Atomicity**: thao tác phức hợp bị xen kẽ, ví dụ `count++` là read-modify-write gồm 3 bước nên mất cập nhật. **Visibility**: thread B không thấy giá trị thread A vừa ghi, ví dụ cờ `stop` không `volatile` khiến vòng lặp chạy mãi. **Ordering**: compiler, JIT và CPU sắp xếp lại lệnh nên thread khác quan sát được thứ tự khác với thứ tự trong code, ví dụ DCL thiếu `volatile`.

**Giải thích chi tiết:**
- Hai dạng race condition kinh điển là **read-modify-write** (`balance -= amount`) và **check-then-act** (`if (!map.containsKey(k)) map.put(k, v)`, lazy init).
- Visibility có nguyên nhân từ store buffer, register, và quan trọng nhất là **JIT hoisting**: JIT kéo phép đọc biến ra khỏi vòng lặp.
- Ordering: trong một thread, kết quả vẫn đúng như khi chạy tuần tự (as-if-serial). Thread khác thì không được đảm bảo điều đó.

**Câu hỏi nối tiếp:**
- *Chỉ một thread ghi, nhiều thread đọc thì có cần đồng bộ không?* → Có. Không cần cho atomicity, nhưng vẫn cần cho visibility và ordering, thường bằng `volatile` hoặc publish object immutable.

**⚠️ Câu trả lời gây điểm trừ:**
- Chỉ biết "race condition" mà không phân biệt được visibility và ordering.

**📖 Ôn lại:** [Phần 2 — Ba vấn đề cốt lõi](../01-giao-trinh/04-concurrency.md#p2)

</details>

### Q7. 🟡 Race condition và data race khác nhau thế nào? Có thể có cái này mà không có cái kia không?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **Data race** là khái niệm của JMM: hai truy cập vào cùng một biến, trong đó ít nhất một là ghi, và không có quan hệ happens-before giữa chúng. **Race condition** là lỗi ở mức logic: kết quả phụ thuộc vào thứ tự xen kẽ của các thread. Có thể có race condition mà không có data race, ví dụ mọi truy cập đều `synchronized` nhưng check và act nằm ở hai khối riêng. Ngược lại, có thể có data race vô hại về mặt logic, ví dụ `String.hashCode` cache kết quả vào field `hash` không volatile. Cách này chấp nhận được vì giá trị tính lại luôn giống nhau.

**Giải thích chi tiết:**

```java
// Không có data race (mỗi method synchronized) nhưng VẪN race condition:
if (!syncMap.containsKey(k)) {   // atomic
    syncMap.put(k, v);           // atomic, nhưng tổ hợp thì không
}
// Sửa: map.putIfAbsent(k, v) hoặc computeIfAbsent, hoặc bọc cả hai trong synchronized(syncMap)
```

**Câu hỏi nối tiếp:**
- *Vì sao loại bỏ data race lại quan trọng?* → Theo SC-DRF guarantee, chương trình không có data race sẽ hành xử như sequentially consistent, nên ta được phép lập luận như thể không có reordering.

**⚠️ Câu trả lời gây điểm trừ:**
- "Hai khái niệm này giống nhau."
- "Dùng `Collections.synchronizedMap` là an toàn với check-then-act."

**📖 Ôn lại:** [Phần 2.1 — Atomicity](../01-giao-trinh/04-concurrency.md#p2)

</details>

### Q8. 🟡 **[Đọc code]** Chương trình sau có dừng không? Nếu thêm `System.out.println(i)` vào vòng lặp thì bug "biến mất". Vì sao, và đó có phải cách sửa không?

```java
static boolean stop = false;
public static void main(String[] args) throws InterruptedException {
    Thread t = new Thread(() -> { long i = 0; while (!stop) i++; });
    t.start();
    Thread.sleep(1000);
    stop = true;
}
```

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Trên HotSpot với C2, thread `t` thường **chạy mãi**. Không có happens-before giữa lệnh ghi `stop = true` và lệnh đọc, nên JIT được phép hoist phép đọc ra ngoài: `if (!stop) while (true) i++;`. Thêm `println` làm bug "biến mất" vì `PrintStream` có `synchronized` bên trong và lời gọi phức tạp ngăn JIT hoist. Tuy vậy, JMM **không đảm bảo** gì trong trường hợp này, nên đó **không** phải cách sửa. Sửa đúng là đánh dấu `volatile boolean stop`, hoặc đọc và ghi qua getter/setter `synchronized` trên cùng một lock, hoặc dùng `AtomicBoolean`.

**Giải thích chi tiết:**
- Chạy với `-Xint` (chỉ interpreter) thì bug thường không xuất hiện, vì mỗi vòng lặp đều đọc lại field. Điều này chứng tỏ thủ phạm là JIT chứ không phải "cache CPU".
- `Thread.sleep` không tạo happens-before nào cả.

**Câu hỏi nối tiếp:**
- *Dùng `volatile` thì `stop` có cần `synchronized` nữa không?* → Không cần, vì chỉ một bên ghi và giá trị mới không phụ thuộc giá trị cũ. Đây đúng là use case chuẩn của `volatile`.

**⚠️ Câu trả lời gây điểm trừ:**
- "Thread đọc từ cache CPU nên không thấy giá trị mới." Cache CPU vốn coherent (MESI). Vấn đề thật là compiler, JIT và store buffer.
- Đề xuất `Thread.sleep` hoặc `println` làm cách sửa.

**📖 Ôn lại:** [Phần 2.2 — Visibility](../01-giao-trinh/04-concurrency.md#p2)

</details>

### Q9. 🟡 Happens-before là gì? Kể các quy tắc happens-before quan trọng.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Happens-before (HB) là quan hệ mà JMM định nghĩa: nếu A HB B thì mọi kết quả của A, cùng mọi thứ xảy ra trước A, đều **nhìn thấy được** ở B. Các quy tắc gồm: program order trong cùng thread; unlock monitor HB lock **sau đó** trên **cùng** monitor; ghi `volatile` HB các lần đọc sau đó của **cùng** biến; `Thread.start()` HB mọi hành động trong thread được start; mọi hành động trong thread HB lúc `join()` return; `interrupt()` HB lúc phát hiện bị interrupt; và tính bắc cầu.

**Giải thích chi tiết:**
- Thư viện `java.util.concurrent` bổ sung thêm: đưa object vào concurrent collection HB lấy object ra; `executor.submit` HB task bắt đầu chạy; task hoàn thành HB `future.get()` return; `countDown()` HB `await()` return; `release()` HB `acquire()`; stage trước của `CompletableFuture` HB stage phụ thuộc.
- Final field: object được construct đúng cách (không có `this` escape) thì mọi thread thấy giá trị các field `final` mà không cần đồng bộ.

**Câu hỏi nối tiếp:**
- *`synchronized(a)` ở thread 1 và `synchronized(b)` ở thread 2 có tạo HB không?* → Không. Quy tắc chỉ áp dụng khi lock **cùng** monitor.

**⚠️ Câu trả lời gây điểm trừ:**
- Định nghĩa HB là "A chạy trước B theo thời gian thực". HB nói về visibility và ordering được bảo đảm, không phải đồng hồ.

**📖 Ôn lại:** [Phần 3.2 — Các quy tắc happens-before](../01-giao-trinh/04-concurrency.md#p3)

</details>

### Q10. 🔴 **[Đọc code]** `data` không volatile, `ready` là volatile. Thread B có chắc chắn in 42 không? Nếu thread A đảo thứ tự hai lệnh ghi thì sao?

```java
int data; volatile boolean ready;
// Thread A                  // Thread B
data = 42;                   if (ready) System.out.println(data);
ready = true;
```

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Có, nếu B đọc thấy `ready == true` thì chắc chắn in 42. Chuỗi suy luận: `data = 42` HB `ready = true` (program order), `ready = true` HB lần đọc `ready` thấy `true` (quy tắc volatile), lần đọc `ready` HB lần đọc `data` (program order). Theo tính bắc cầu, `data = 42` HB lệnh in. Kỹ thuật này gọi là "piggybacking" trên volatile. Nếu A ghi `ready = true` **trước** `data = 42` thì không còn HB từ lệnh ghi `data` tới lệnh đọc `data`, và B có thể in 0.

**Giải thích chi tiết:**
- JVM hiện thực quy tắc bằng memory barrier: các lệnh **trước** volatile write không được đẩy xuống sau nó (release), các lệnh **sau** volatile read không được kéo lên trước nó (acquire).
- Đây chính là cơ chế mà `ConcurrentHashMap`, `FutureTask` và AQS dùng để publish dữ liệu thường thông qua một biến volatile.
- `VarHandle` (Java 9) cho phép dùng các chế độ yếu hơn là `setRelease`/`getAcquire`, đủ cho kiểu publish này và rẻ hơn volatile đầy đủ vì không cần StoreLoad barrier.

**Câu hỏi nối tiếp:**
- *Trên x86 vì sao volatile write đắt hơn volatile read?* → x86 (TSO) chỉ cần StoreLoad barrier (`lock addl`/`mfence`) sau volatile write. Volatile read gần như miễn phí.

**⚠️ Câu trả lời gây điểm trừ:**
- "Không chắc chắn vì `data` không volatile." Câu này bỏ qua tính bắc cầu của HB.
- "Phải đánh dấu mọi field là volatile."

**📖 Ôn lại:** [Phần 3.3 — Piggybacking](../01-giao-trinh/04-concurrency.md#p3)

</details>

### Q11. 🔴 Safe publication là gì? Thế nào là `this` escape? Vì sao field `final` quan trọng với thread-safety?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Publish an toàn nghĩa là thread khác nhìn thấy object ở **trạng thái đầy đủ** sau constructor. Các cách publish an toàn: khởi tạo trong static initializer; lưu vào field `volatile` hoặc `AtomicReference`; lưu vào field `final` của một object được construct đúng cách; lưu vào field được bảo vệ bởi lock; đưa vào concurrent collection. `this` escape xảy ra khi constructor để lộ `this` ra ngoài, ví dụ đăng ký listener hoặc start thread ngay trong constructor. Khi đó thread khác thấy object lúc chưa construct xong, kể cả các field `final`. JLS §17.5 bảo đảm field `final` được nhìn thấy đúng mà không cần đồng bộ, với điều kiện không có `this` escape. Đây là nền tảng của immutable object.

**Giải thích chi tiết:**

```java
static Holder holder;                     // publish KHÔNG an toàn
holder = new Holder(42);                  // phép gán reference có thể bị reorder lên trước lệnh ghi field
// Thread khác: holder != null nhưng holder.n == 0 vẫn được phép xảy ra (JCIP §3.5)
```
- Có ba cách sửa: `volatile Holder holder`, `final int n` trong `Holder`, hoặc publish qua lock hay concurrent collection.
- Để tránh `this` escape: dùng private constructor kèm static factory. Factory construct xong rồi mới `register(listener)` hoặc `start()`.

**Câu hỏi nối tiếp:**
- *Record trong Java 16+ có thread-safe không?* → Các field đều `final` nên publish an toàn. Tuy nhiên, nếu field là collection mutable thì phải copy phòng thủ, ví dụ `List.copyOf`.

**⚠️ Câu trả lời gây điểm trừ:**
- "Object immutable thì publish thế nào cũng được" mà không nhắc tới điều kiện không có `this` escape.
- "`final` chỉ để không gán lại được."

**📖 Ôn lại:** [Phần 3.5 — Safe publication](../01-giao-trinh/04-concurrency.md#p3)

</details>

### Q12. 🔴 **[Tình huống]** Một đoạn code đồng thời chạy đúng nhiều năm trên x86, đến khi chuyển hạ tầng sang AWS Graviton (ARM) thì thỉnh thoảng sai. Bạn giải thích thế nào? Trên x86 có bug reordering nào xảy ra được không?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** x86 có memory model mạnh (TSO): chỉ cho phép một kiểu reordering phần cứng là StoreLoad. ARM có memory model yếu hơn: cho phép reorder cả load-load, store-store, load-store. Vì vậy code có data race "vô tình đúng" trên x86 có thể lộ bug trên ARM. Cách nhìn đúng là: code đó **đã sai theo JMM** từ đầu, phần cứng chỉ làm lộ bug ra. Ngay trên x86 vẫn có kịch bản Dekker `x=1; r1=y` / `y=1; r2=x` cho kết quả `(0,0)`, do lệnh ghi còn nằm trong store buffer. Ngoài ra, JIT reordering và hoisting xảy ra trên mọi kiến trúc.

**Giải thích chi tiết:**
- Không lập luận bằng phần cứng. Hãy lập luận bằng **happens-before**: tìm mọi biến chia sẻ và hỏi điều gì tạo HB giữa lệnh ghi và lệnh đọc của nó.
- Công cụ kiểm chứng là **jcstress**: chạy hàng triệu lần, thống kê mọi kết quả có thể. Đánh dấu `x`, `y` là `volatile` thì `(0,0)` biến mất vì JVM chèn StoreLoad barrier.
- Các ứng viên đáng nghi khi migrate: DCL thiếu `volatile`, cờ trạng thái không volatile, object publish không an toàn, lock-free code tự viết.

**Câu hỏi nối tiếp:**
- *Làm sao giảm rủi ro khi chuyển kiến trúc?* → Chạy jcstress/Lincheck cho cấu trúc tự viết, bật static analysis (Error Prone `@GuardedBy`, SpotBugs `DC_DOUBLECHECK`), stress test trên đúng kiến trúc đích.

**⚠️ Câu trả lời gây điểm trừ:**
- "Do JVM trên ARM có bug."
- "x86 không bao giờ reorder."

**📖 Ôn lại:** [Phần 2.3 — Ordering](../01-giao-trinh/04-concurrency.md#p2) · [Phần 3 — Bài 3.3 jcstress](../01-giao-trinh/04-concurrency.md#p3)

</details>

---

<a id="nhom-c"></a>
## 3. `synchronized`, `volatile` & double-checked locking

### Q13. 🟢 `synchronized` đảm bảo những gì? `static synchronized` và `synchronized` trên instance method lock vào object nào? Hai method đó có chặn nhau không?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `synchronized` đảm bảo hai điều. Thứ nhất là **mutual exclusion**: tại một thời điểm chỉ một thread được ở trong khối. Thứ hai là **visibility/ordering** qua quy tắc unlock HB lock. Instance method lock trên `this`, static method lock trên `Class` object (`Foo.class`). Đó là hai monitor khác nhau nên hai method **không chặn nhau**, và nếu cả hai cùng sửa một field static thì sẽ có race.

**Giải thích chi tiết:**
- Monitor là **reentrant**: thread đang giữ lock có thể vào lại (đệ quy, gọi method synchronized khác). Lock tự nhả khi rời khối, kể cả khi có exception (bytecode có thêm một `monitorexit` trong exception handler).
- `synchronized` không interruptible khi đang chờ, không có timeout, không fair.
- Phương thức **đọc** cũng phải synchronized. Nếu setter synchronized mà getter không, phía đọc không có visibility.
- Nên lock trên `private final Object lock = new Object()` để code bên ngoài không lock nhầm vào cùng object.

**Câu hỏi nối tiếp:**
- *Ở mức bytecode thì sao?* → Khối synchronized biên dịch thành `monitorenter`/`monitorexit`. Method synchronized chỉ là cờ `ACC_SYNCHRONIZED`.

**⚠️ Câu trả lời gây điểm trừ:**
- "Chỉ cần synchronized setter."
- "`static synchronized` và `synchronized` dùng chung một lock."

**📖 Ôn lại:** [Phần 4.1 — synchronized](../01-giao-trinh/04-concurrency.md#p4)

</details>

### Q14. 🟢 `volatile` đảm bảo gì và **không** đảm bảo gì? `volatile int[] arr` có làm phần tử mảng volatile không?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `volatile` **đảm bảo** ba điều: visibility (lần đọc thấy lần ghi gần nhất theo HB), ordering (cấm reorder qua điểm volatile), và atomicity cho đọc/ghi **đơn lẻ** của `long`/`double` (không có word tearing). `volatile` **không đảm bảo** atomicity cho thao tác phức hợp như `count++` hay check-then-act. `volatile int[] arr` chỉ làm **reference** tới mảng là volatile, còn `arr[i] = x` thì không. Muốn phần tử có ngữ nghĩa volatile/atomic thì dùng `AtomicIntegerArray` hoặc `VarHandle`.

**Giải thích chi tiết:**
- Use case chuẩn: cờ trạng thái chỉ một bên ghi; publish reference tới object immutable; biến mà giá trị mới không phụ thuộc giá trị cũ.
- `volatile List<X> list` không làm list thread-safe. Nó chỉ publish an toàn **reference**.
- JLS §17.7: `long`/`double` không volatile có thể bị ghi thành hai lần 32-bit. Trên JVM 64-bit thực tế hiếm gặp, nhưng spec cho phép.

**Câu hỏi nối tiếp:**
- *Khi nào `volatile` thay được `synchronized`?* → Khi không có invariant liên quan nhiều biến và không có read-modify-write.

**⚠️ Câu trả lời gây điểm trừ:**
- "`volatile` làm biến thread-safe."
- Dùng `volatile int counter` rồi gọi `counter++`.

**📖 Ôn lại:** [Phần 4.3 — volatile](../01-giao-trinh/04-concurrency.md#p4)

</details>

### Q15. 🟡 Ứng viên trả lời: "`volatile` nghĩa là biến luôn được đọc/ghi trực tiếp từ main memory, không qua cache CPU". Câu trả lời này đúng hay sai? Bạn sẽ trả lời thế nào ở mức Senior?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Đó là mô hình **sai lệch**. CPU hiện đại có cache coherence (MESI) nên cache không "giấu" giá trị mãi. Vấn đề thật nằm ở **reordering** do compiler/JIT, ở store buffer, và ở việc giữ giá trị trong register. Câu trả lời Senior là: "Ghi volatile happens-before mọi lần đọc sau đó của cùng biến, nên mọi lệnh ghi trước lệnh ghi volatile cũng visible với thread đọc. JVM hiện thực điều này bằng memory barrier, cấm reorder qua điểm volatile và xả store buffer (StoreLoad) sau volatile write."

**Giải thích chi tiết:**
- JSR-133 Cookbook mô tả bốn loại barrier: LoadLoad, LoadStore, StoreStore, StoreLoad. Trên x86 chỉ StoreLoad tốn kém thật sự. Trên ARM dùng `ldar`/`stlr`/`dmb`.
- JIT không được hoist phép đọc volatile ra khỏi vòng lặp. Đây là lý do `volatile` sửa được bug ở Q8.

**Câu hỏi nối tiếp:**
- *Chi phí của volatile read so với đọc thường?* → Trên x86 gần như bằng nhau về lệnh máy, nhưng nó chặn một số tối ưu JIT (hoisting, gộp lệnh đọc).

**⚠️ Câu trả lời gây điểm trừ:**
- Dừng ở "main memory vs cache" mà không nhắc tới happens-before hay reordering.

**📖 Ôn lại:** [Phần 3.4 — Memory barriers](../01-giao-trinh/04-concurrency.md#p3) · [Góc nhìn Senior phần 3](../01-giao-trinh/04-concurrency.md#p3)

</details>

### Q16. 🟡 Viết singleton lazy thread-safe bằng double-checked locking. Vì sao `instance` phải là `volatile`? Có cách nào đơn giản hơn không?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Thiếu `volatile` thì phép gán `instance = new Singleton()` có thể bị reorder lên **trước** khi constructor ghi xong các field. Thread khác, ở lần check đầu tiên không có lock, thấy `instance != null` và dùng một object khởi tạo dở dang. Có hai cách đơn giản hơn: **holder idiom** (lazy và thread-safe nhờ class initialization lock của JVM) và **enum singleton**, chống được cả reflection lẫn serialization.

**Giải thích chi tiết:**

```java
public class SafeSingleton {
    private static volatile SafeSingleton instance;
    public static SafeSingleton get() {
        SafeSingleton local = instance;              // đọc volatile 1 lần
        if (local == null) {
            synchronized (SafeSingleton.class) {
                local = instance;
                if (local == null) instance = local = new SafeSingleton();
            }
        }
        return local;
    }
}

public class HolderSingleton {
    private static class Holder { static final HolderSingleton INSTANCE = new HolderSingleton(); }
    public static HolderSingleton get() { return Holder.INSTANCE; } // Holder chỉ init khi gọi get()
}
```
- DCL vẫn có chỗ dùng: lazy init **field instance** tốn kém (Effective Java Item 83).
- Trong Spring, bean mặc định là singleton do container quản lý, nên hiếm khi phải tự viết.

**Câu hỏi nối tiếp:**
- *Vì sao dùng biến `local`?* → Để giảm số lần đọc volatile (một lần ở đường nhanh). Đây là tối ưu nhỏ, không bắt buộc cho tính đúng.
- *`synchronized` cả method `get()` thì sao?* → Đúng nhưng mọi lần gọi đều phải lấy lock, chậm hơn khi có tranh chấp.

**⚠️ Câu trả lời gây điểm trừ:**
- Viết DCL thiếu `volatile`.
- Giải thích `volatile` "để tránh cache".

**📖 Ôn lại:** [Phần 4.4 — Double-checked locking](../01-giao-trinh/04-concurrency.md#p4)

</details>

### Q17. 🔴 JVM hiện thực `synchronized` như thế nào bên dưới? Biased locking hiện còn không? JIT tối ưu lock ra sao?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Mỗi object có **mark word** trong header, lưu hash, tuổi GC và trạng thái lock. Khi không tranh chấp, JVM dùng **lightweight (thin) lock** bằng CAS trên mark word. Khi có tranh chấp hoặc có lời gọi `wait()`, lock bị **inflate** thành `ObjectMonitor` (cấu trúc native có entry list và wait set). Thread chờ sẽ spin một lúc (adaptive spinning) rồi park ở OS. **Biased locking đã bị tắt mặc định và deprecated từ JDK 15 (JEP 374)** rồi bị loại bỏ ở các bản sau. JIT có thêm **lock elision** (bỏ lock trên object không escape, nhờ escape analysis) và **lock coarsening** (gộp nhiều cặp lock/unlock liên tiếp trên cùng object).

**Giải thích chi tiết:**
- Biased locking bị bỏ vì chi phí revoke phải đi qua safepoint, code JVM phức tạp, và lợi ích giảm hẳn khi ứng dụng hiện đại dùng `j.u.c` thay cho `Vector`/`Hashtable`.
- JDK 21 có chế độ lightweight locking mới dùng lock-stack theo từng thread, trở thành mặc định ở các bản sau.
- Với virtual threads trên JDK 21–23, block bên trong `synchronized` sẽ **pin** carrier thread. JEP 491 (JDK 24) đã sửa điều này.

**Câu hỏi nối tiếp:**
- *Vì sao `synchronized` trên object đã gọi `hashCode()` từng phải inflate?* → Trong thời biased locking, mark word không đủ chỗ chứa cả identity hash lẫn thread ID của bias.

**⚠️ Câu trả lời gây điểm trừ:**
- Mô tả biased locking như thể vẫn là mặc định trên Java 17/21.
- "`synchronized` luôn gọi xuống OS mutex nên rất chậm." Khi không tranh chấp, nó chỉ tốn một lệnh CAS.

**📖 Ôn lại:** [Phần 4.2 — Object header & các trạng thái lock](../01-giao-trinh/04-concurrency.md#p4)

</details>

### Q18. 🟡 **[Đọc code]** Tìm bug trong ba đoạn code sau.

```java
// (1)
private final String LOCK = "LOCK";
void a() { synchronized (LOCK) { ... } }
// (2)
private Integer count = 0;
void inc() { synchronized (count) { count++; } }
// (3)
private List<String> items = new ArrayList<>();
void reload() { items = loadFromDb(); }
void add(String s) { synchronized (items) { items.add(s); } }
```

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:**
1. String literal được **intern** và dùng chung toàn JVM. Thư viện khác cũng lock trên `"LOCK"` sẽ tranh chấp với bạn, thậm chí gây deadlock.
2. `count++` tạo ra **object `Integer` mới** (autoboxing), nên mỗi lần lock là lock một object khác. Thêm nữa, `Integer` từ -128 đến 127 được cache dùng chung toàn JVM. Kết quả là không có mutual exclusion.
3. Lock trên field **không final** và bị gán lại. Thread A lock list cũ, thread B lock list mới, hai bên không loại trừ nhau. `reload()` cũng không có HB nên thread khác có thể vẫn thấy list cũ.

**Giải thích chi tiết:** Quy tắc là luôn lock trên `private final Object lock = new Object()`. Với (2), dùng `AtomicInteger` hoặc `LongAdder`. Với (3), có hai lựa chọn. Cách một: `private final Object lock`, rồi cả `reload()` và `add()` cùng synchronized trên lock đó. Cách hai: giữ list immutable trong `volatile`/`AtomicReference` và thay toàn bộ list mỗi lần cập nhật (copy-on-write ở mức domain).

**Câu hỏi nối tiếp:**
- *Lock trên `this` trong class public có vấn đề gì?* → Client bên ngoài có thể `synchronized(yourObject)` và giữ lock lâu, gây DoS hoặc deadlock.

**⚠️ Câu trả lời gây điểm trừ:**
- Chỉ thấy bug (2) mà bỏ qua chuyện field bị gán lại ở (3).

**📖 Ôn lại:** [Phần 4 — Góc nhìn Senior & Lỗi thường gặp](../01-giao-trinh/04-concurrency.md#p4)

</details>

---

<a id="nhom-d"></a>
## 4. wait/notify, `Lock`, `ReadWriteLock`, `StampedLock`

### Q19. 🟡 Khi dùng `wait()`/`notify()`, vì sao phải gọi trong `synchronized`, vì sao phải dùng `while` thay vì `if`, và khi nào `notify()` gây treo hệ thống?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `wait`/`notify` thao tác trên wait set của monitor, nên thread phải đang giữ monitor của **chính object đó**, nếu không sẽ nhận `IllegalMonitorStateException`. Điều kiện chờ được bảo vệ bởi cùng lock, và `wait()` nhả lock một cách nguyên tử. Phải dùng `while` vì hai lý do: có **spurious wakeup**, và khi thread tỉnh dậy, lấy lại được lock thì điều kiện có thể đã bị thread khác thay đổi. `notify()` chỉ đánh thức **một** thread bất kỳ. Nếu producer và consumer cùng chờ trên một monitor, `notify` có thể đánh thức nhầm loại thread, và tín hiệu bị mất (lost wakeup) khiến hệ thống treo. Mặc định nên dùng `notifyAll()`.

**Giải thích chi tiết:**

```java
public synchronized void put(T t) throws InterruptedException {
    while (count == items.length) wait();   // while, không phải if
    enqueue(t);
    notifyAll();                            // đánh thức cả consumer lẫn producer
}
```
- `Condition` của `ReentrantLock` cho phép tách `notFull`/`notEmpty`, nhờ đó `signal()` gọi đúng loại thread và hiệu quả hơn `notifyAll`.
- Effective Java Item 81: trong code mới nên ưu tiên `BlockingQueue`, `CountDownLatch` hơn `wait`/`notify`.

**Câu hỏi nối tiếp:**
- *`wait()` khác `sleep()` thế nào?* → `wait` nhả monitor và cần được notify (hoặc hết timeout). `sleep` giữ nguyên mọi lock.

**⚠️ Câu trả lời gây điểm trừ:**
- Dùng `if (count == 0) wait();`.
- "`notify` nhanh hơn nên luôn dùng `notify`."

**📖 Ôn lại:** [Phần 5.1 — wait/notify](../01-giao-trinh/04-concurrency.md#p5)

</details>

### Q20. 🟡 So sánh `synchronized` và `ReentrantLock`. Khi nào bạn chọn `ReentrantLock`? Fair lock có nên dùng không?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Mặc định chọn `synchronized` vì gọn và tự unlock. Chọn `ReentrantLock` khi cần: `tryLock`/timeout (tránh deadlock, fail fast), `lockInterruptibly`, nhiều `Condition`, khả năng quan sát (`getQueueLength`, `isLocked`), hoặc **tránh pin virtual thread trên JDK 21–23**. Fair lock giảm starvation nhưng throughput giảm mạnh, vì không cho phép "barging" (thread vừa tới chen ngang khi lock vừa rảnh) và luôn phải đánh thức thread đang ngủ. Chỉ dùng fair lock khi đã đo và thấy starvation thật.

**Giải thích chi tiết:**

| | `synchronized` | `ReentrantLock` |
|---|---|---|
| Unlock | tự động | phải gọi trong `finally` |
| Timeout / interruptible | không | có |
| Condition | 1 wait set | nhiều |
| Virtual thread JDK 21–23 | pin carrier | không pin |

```java
lock.lock();            // đặt NGOÀI try
try { ... } finally { lock.unlock(); }
```
Nếu đặt `lock()` bên trong `try` mà `lock()` ném exception, `finally` sẽ unlock một lock chưa giữ và gây `IllegalMonitorStateException`.

**Câu hỏi nối tiếp:**
- *`tryLock()` không tham số trên fair lock có fair không?* → Không. Nó bỏ qua fairness và lấy lock ngay nếu đang rảnh.

**⚠️ Câu trả lời gây điểm trừ:**
- "`ReentrantLock` luôn nhanh hơn `synchronized`." Từ Java 6 trở đi hai cái tương đương khi không có tranh chấp.
- Quên `unlock` trong `finally`.

**📖 Ôn lại:** [Phần 5.2 — ReentrantLock và Condition](../01-giao-trinh/04-concurrency.md#p5)

</details>

### Q21. 🔴 AQS (`AbstractQueuedSynchronizer`) là gì? Những lớp nào được xây dựng trên nó và nó hoạt động thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** AQS là framework nền của phần lớn `j.u.c.locks`. Nó gồm một `volatile int state`, các thao tác CAS trên `state`, và một **hàng đợi FIFO (biến thể CLH)** chứa các thread đang chờ, được park/unpark qua `LockSupport`. Lớp con chỉ cần định nghĩa ý nghĩa của `state` bằng cách override `tryAcquire`/`tryRelease` (chế độ exclusive) hoặc `tryAcquireShared`/`tryReleaseShared` (chế độ shared). Các lớp dựa trên AQS: `ReentrantLock` (state = số lần reentrant), `Semaphore` (state = số permit), `CountDownLatch` (state = số đếm), `ReentrantReadWriteLock` (16 bit cao cho read, 16 bit thấp cho write), và `Worker` của `ThreadPoolExecutor`.

**Giải thích chi tiết:**
- Luồng acquire: thử CAS `state`. Nếu thất bại thì tạo node, nối vào đuôi hàng đợi, rồi `park`. Khi release, node đầu được `unpark` và thử lại.
- Non-fair: thread mới tới được thử CAS ngay mà không xếp hàng. Fair: kiểm tra `hasQueuedPredecessors()` trước khi thử.
- Đây là lý do thread chờ `ReentrantLock` hiện `WAITING (parking)` trong dump, kèm dòng `parking to wait for <0x...> (a java.util.concurrent.locks.ReentrantLock$NonfairSync)`.

**Câu hỏi nối tiếp:**
- *Khi nào bạn tự viết synchronizer trên AQS?* → Rất hiếm, ví dụ một latch có thể reset, hay mutex không reentrant. Thường thì các lớp có sẵn đã đủ.

**⚠️ Câu trả lời gây điểm trừ:**
- Không biết AQS, hoặc nói "`ReentrantLock` dùng `synchronized` bên trong".

**📖 Ôn lại:** [Phần 5.2 — AQS](../01-giao-trinh/04-concurrency.md#p5)

</details>

### Q22. 🟡 `ReentrantReadWriteLock`: nâng cấp read → write có được không? Khi nào RW lock lại chậm hơn `synchronized`?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **Không nâng cấp được.** Đang giữ read lock mà gọi `writeLock().lock()` sẽ **tự deadlock**, vì writer phải chờ mọi reader nhả lock, kể cả chính nó. Hạ cấp write → read thì được: lấy read lock trong khi đang giữ write lock, rồi nhả write lock. RW lock chậm hơn khi critical section **ngắn** hoặc tỉ lệ ghi cao. Lý do là mọi reader đều CAS vào cùng một `state`, gây cache-line contention, và chi phí quản lý cao hơn một mutex đơn giản.

**Giải thích chi tiết:**
- RW lock chỉ đáng dùng khi số lần đọc vượt hẳn số lần ghi **và** thời gian giữ lock đủ dài. Phải đo bằng JMH với `@Group`/`@GroupThreads`.
- Nếu bài toán là map, dùng `ConcurrentHashMap` gần như luôn tốt hơn `HashMap` + RW lock.
- Với lock non-fair, reader liên tục tới có thể làm writer bị **starvation**.

**Câu hỏi nối tiếp:**
- *Muốn "đọc rồi có thể ghi" thì làm thế nào?* → Nhả read lock, lấy write lock, rồi **kiểm tra lại** điều kiện, vì trạng thái có thể đã đổi trong khoảng giữa.

**⚠️ Câu trả lời gây điểm trừ:**
- "Đọc nhiều thì RW lock luôn nhanh hơn" mà không nhắc tới chuyện đo.

**📖 Ôn lại:** [Phần 5.3 — ReadWriteLock](../01-giao-trinh/04-concurrency.md#p5)

</details>

### Q23. 🔴 `StampedLock` optimistic read hoạt động thế nào? Những bẫy nào khiến nó nguy hiểm hơn RW lock?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `tryOptimisticRead()` trả về một stamp mà **không lock gì cả**. Code đọc các field vào biến local, rồi `validate(stamp)`. Nếu trong lúc đó có writer chen vào thì validate thất bại và code fallback sang `readLock()`. Cách này không chặn writer nên rất nhanh khi ghi hiếm. Các bẫy: `StampedLock` **không reentrant**, gọi lồng nhau sẽ tự deadlock; **không có `Condition`**; dữ liệu đọc trước khi validate có thể **không nhất quán**, nên không được dùng chúng (chia, dereference, lấy làm index mảng) trước khi validate thành công.

**Giải thích chi tiết:**

```java
long stamp = sl.tryOptimisticRead();
double cx = x, cy = y;                 // chỉ copy vào local
if (!sl.validate(stamp)) {
    stamp = sl.readLock();
    try { cx = x; cy = y; } finally { sl.unlockRead(stamp); }
}
return Math.hypot(cx, cy);             // chỉ tính toán SAU validate
```
- Nếu đọc một reference rồi dereference trước khi validate, code có thể thấy object ở trạng thái nửa vời và ném NPE hoặc chạy vòng lặp vô hạn.

**Câu hỏi nối tiếp:**
- *Khi nào bạn chọn StampedLock?* → Cấu trúc nhỏ có tỉ lệ đọc/ghi rất cao và đã đo được contention ở phía đọc, ví dụ toạ độ hay snapshot số liệu. Code nghiệp vụ thông thường hiếm khi cần.

**⚠️ Câu trả lời gây điểm trừ:**
- Coi `StampedLock` như RW lock "xịn hơn" để thay thế hàng loạt.

**📖 Ôn lại:** [Phần 5.4 — StampedLock](../01-giao-trinh/04-concurrency.md#p5)

</details>

---

<a id="nhom-e"></a>
## 5. Atomics, CAS, ABA, `LongAdder`

### Q24. 🟢 CAS là gì? `AtomicInteger.incrementAndGet()` hoạt động thế nào mà không cần lock?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** CAS (Compare-And-Swap) là lệnh CPU nguyên tử: "nếu giá trị hiện tại bằng `expected` thì ghi `new`, rồi trả về thành công hay thất bại". Trên x86 đó là `lock cmpxchg`, trên ARM là LL/SC hoặc lệnh CAS của ARMv8.1. `AtomicInteger` lưu giá trị trong field `volatile` và thực hiện vòng lặp: đọc giá trị, tính giá trị mới, CAS, nếu thua thì thử lại. Không thread nào bị block (lock-free). Thread thua CAS chỉ việc thử lại.

**Giải thích chi tiết:**

```java
public void updateMax(long candidate) {
    long cur;
    do {
        cur = max.get();
        if (candidate <= cur) return;
    } while (!max.compareAndSet(cur, candidate));
}
// Gọn hơn: max.accumulateAndGet(candidate, Math::max);  (hàm phải không có side-effect)
```
- Họ atomic gồm: `AtomicInteger/Long/Boolean/Reference`, mảng atomic, `AtomicXxxFieldUpdater` (CAS trên field volatile có sẵn, tiết kiệm bộ nhớ), `LongAdder`. Từ Java 9, `VarHandle` thay thế `sun.misc.Unsafe`.

**Câu hỏi nối tiếp:**
- *Lock-free có luôn nhanh hơn lock không?* → Không. Dưới contention rất cao, CAS loop đốt CPU khi spin, trong khi lock cho thread ngủ. Ưu điểm thật của lock-free là **progress guarantee**: không bị kẹt khi thread đang giữ lock bị preempt hoặc dính GC pause.

**⚠️ Câu trả lời gây điểm trừ:**
- "Atomic dùng `synchronized` bên trong."
- Viết `atomic.set(atomic.get() + 1)` mà nghĩ là atomic.

**📖 Ôn lại:** [Phần 6.1 — CAS](../01-giao-trinh/04-concurrency.md#p6)

</details>

### Q25. 🟡 Vấn đề ABA là gì? Trong Java có xảy ra không, và giải quyết thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Thread 1 đọc giá trị A rồi bị tạm dừng. Thread 2 đổi A → B → A. Thread 1 CAS(A → C) **thành công** dù trạng thái thực tế đã thay đổi. Trong C/C++, chuyện này phá vỡ cấu trúc lock-free khi node bị giải phóng rồi được cấp phát lại ở cùng địa chỉ. Trong Java, GC đảm bảo node không bị tái sử dụng khi còn tham chiếu nên ABA ít nghiêm trọng hơn. Tuy vậy ABA vẫn xảy ra khi **giá trị** quay vòng (số dư, version), hoặc khi dùng object pool. Cách giải quyết là dùng `AtomicStampedReference` (thêm version `int`) hoặc `AtomicMarkableReference`.

**Giải thích chi tiết:**

```java
AtomicStampedReference<Integer> balance = new AtomicStampedReference<>(100, 0);
int[] stamp = new int[1];
Integer cur = balance.get(stamp);
balance.compareAndSet(cur, cur - 30, stamp[0], stamp[0] + 1);
```
- Ở mức DB, cùng ý tưởng này chính là optimistic locking bằng cột `version` (JPA `@Version`).

**Câu hỏi nối tiếp:**
- *Cho ví dụ ABA gây sai nghiệp vụ?* → Stack lock-free dùng object pool: node X bị pop, trả về pool, rồi được push lại. Thread khác CAS `top` từ X sang `X.next` cũ, và các node ở giữa bị mất khỏi stack.

**⚠️ Câu trả lời gây điểm trừ:**
- "Java có GC nên không bao giờ có ABA."

**📖 Ôn lại:** [Phần 6.2 — Vấn đề ABA](../01-giao-trinh/04-concurrency.md#p6)

</details>

### Q26. 🟡 `AtomicLong` và `LongAdder` khác nhau thế nào? Khi nào **không** được dùng `LongAdder`? False sharing là gì?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Dưới contention cao, `AtomicLong` có rất nhiều CAS thất bại trên **cùng một cache line**, và hiệu năng sụp. `LongAdder` chia giá trị thành `base` cộng một mảng `Cell` đã được pad bằng `@Contended`. Mỗi thread cộng vào cell của riêng mình, còn `sum()` cộng dồn tất cả. Kết quả là ghi nhanh hơn rất nhiều nhưng tốn bộ nhớ hơn, và **`sum()` không phải snapshot nguyên tử**. Vì vậy không dùng `LongAdder` cho logic cần giá trị chính xác tức thời, như sinh ID duy nhất hay kiểm tra quota chặt. Nó hợp với metrics và bộ đếm thống kê. **False sharing** xảy ra khi hai biến độc lập nằm chung một cache line (64 byte) và bị hai core ghi liên tục, khiến cache line bị chuyền qua lại giữa các core như bóng bàn.

**Giải thích chi tiết:**
- Mẫu đếm tần suất kinh điển: `map.computeIfAbsent(k, x -> new LongAdder()).increment()`.
- Có thể chống false sharing bằng `@jdk.internal.vm.annotation.Contended` (code ngoài JDK cần `-XX:-RestrictContended`) hoặc padding thủ công. Kiểm tra layout bằng JOL.
- `size()` của `ConcurrentHashMap` dùng cơ chế giống `LongAdder` (`CounterCell[]`), nên cũng chỉ là xấp xỉ.

**Câu hỏi nối tiếp:**
- *Khi không có contention thì sao?* → `LongAdder` chỉ dùng `base` nên tương đương `AtomicLong`. Mảng cell chỉ được tạo khi CAS trên `base` thất bại.

**⚠️ Câu trả lời gây điểm trừ:**
- "`LongAdder` lúc nào cũng tốt hơn, cứ thay hết `AtomicLong` bằng nó."

**📖 Ôn lại:** [Phần 6.3 — LongAdder & false sharing](../01-giao-trinh/04-concurrency.md#p6)

</details>

### Q27. 🔴 **[Đọc code]** Ba đoạn code sau có gì sai?

```java
// (1)
AtomicReference<Integer> ref = new AtomicReference<>(1000);
boolean ok = ref.compareAndSet(1000, 2000);
// (2)
counter.updateAndGet(x -> { auditLog.write("inc"); return x + 1; });
// (3) invariant: lower <= upper
AtomicInteger lower = new AtomicInteger(0), upper = new AtomicInteger(10);
void setLower(int v) { if (v > upper.get()) throw new IllegalArgumentException(); lower.set(v); }
void setUpper(int v) { if (v < lower.get()) throw new IllegalArgumentException(); upper.set(v); }
```

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:**
1. `compareAndSet` so sánh bằng **`==` (reference)**, không dùng `equals`. Số `1000` nằm ngoài vùng cache của `Integer` (-128..127), nên autoboxing tạo ra object mới và CAS **luôn thất bại**.
2. Hàm truyền vào `updateAndGet` có thể bị **gọi lại nhiều lần** khi CAS thua, nên audit log bị ghi trùng. Hàm này phải không có side-effect.
3. Mỗi atomic riêng lẻ đúng, nhưng **invariant liên quan hai biến** vẫn bị race. Hai thread chạy `setLower(8)` và `setUpper(5)` đồng thời đều qua được bước kiểm tra, và kết quả cuối là `lower=8 > upper=5`.

**Giải thích chi tiết:** Với (3), gom cả hai giá trị vào một object immutable rồi CAS cả cặp:

```java
record Range(int lower, int upper) {}
AtomicReference<Range> range = new AtomicReference<>(new Range(0, 10));
void setLower(int v) {
    range.updateAndGet(r -> {
        if (v > r.upper()) throw new IllegalArgumentException();
        return new Range(v, r.upper());
    });
}
```
Cách khác là dùng một lock bảo vệ cả hai biến.

**Câu hỏi nối tiếp:**
- *Với (1) nên sửa thế nào?* → Đọc reference hiện tại rồi CAS với chính reference đó, hoặc dùng `AtomicInteger` nếu bản chất là số.

**⚠️ Câu trả lời gây điểm trừ:**
- Nói (3) "an toàn vì dùng atomic".

**📖 Ôn lại:** [Phần 6 — Lỗi thường gặp](../01-giao-trinh/04-concurrency.md#p6)

</details>

---

<a id="nhom-f"></a>
## 6. Deadlock, starvation & thread dump

### Q28. 🟢 Deadlock là gì? Nêu 4 điều kiện Coffman và các cách phòng tránh. Livelock và starvation khác deadlock thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Deadlock là tình trạng một tập thread chờ nhau vĩnh viễn. Nó cần đủ bốn điều kiện: **mutual exclusion**, **hold-and-wait**, **no preemption**, **circular wait**. Phá một điều kiện là đủ. Các cách phổ biến: lock ordering toàn cục (phá circular wait); `tryLock` với timeout cộng random backoff (phá hold-and-wait); open call, tức là không gọi code bên ngoài khi đang giữ lock; dùng một lock thô duy nhất; thiết kế không chia sẻ state. **Livelock**: các thread không bị block nhưng cứ nhường nhau mãi nên không tiến triển. **Starvation**: một thread không bao giờ có được tài nguyên, ví dụ do lock không fair hay writer bị reader chặn liên tục.

**Giải thích chi tiết:**
- Random backoff phá được livelock vì hai bên không còn retry cùng nhịp (giống CSMA/CD của Ethernet).
- Deadlock trong DB tuân theo cùng logic. Cách tránh là cập nhật bản ghi theo thứ tự cố định, ví dụ sort theo primary key.

**Câu hỏi nối tiếp:**
- *JVM có tự giải deadlock không?* → Không. JVM chỉ phát hiện được (thread dump, `ThreadMXBean.findDeadlockedThreads()`). Không có cách dừng thread an toàn, nên xử lý thực tế là cảnh báo rồi restart instance.

**⚠️ Câu trả lời gây điểm trừ:**
- Chỉ kể được "hai thread chờ nhau" mà không nêu được cách phòng.

**📖 Ôn lại:** [Phần 7.1–7.2 — Deadlock](../01-giao-trinh/04-concurrency.md#p7)

</details>

### Q29. 🟡 **[Đọc code]** Hàm chuyển tiền sau deadlock khi nào? Sửa thế nào nếu không có ID duy nhất?

```java
void transfer(Account from, Account to, long amt) {
    synchronized (from) { synchronized (to) { from.debit(amt); to.credit(amt); } }
}
```

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Deadlock xảy ra khi một thread chuyển A→B và một thread khác chuyển B→A cùng lúc. Thread 1 giữ A chờ B, thread 2 giữ B chờ A. Cách sửa là **lock theo thứ tự toàn cục**, ví dụ theo id. Nếu không có id duy nhất thì dùng `System.identityHashCode` để xếp thứ tự, và khi hai hash trùng nhau thì lấy thêm một **tie-breaking lock** toàn cục (JCIP §10.1.2).

**Giải thích chi tiết:**

```java
private static final Object TIE_LOCK = new Object();
void transfer(Account from, Account to, long amt) {
    int h1 = System.identityHashCode(from), h2 = System.identityHashCode(to);
    if (h1 < h2)      { synchronized (from) { synchronized (to) { move(from, to, amt); } } }
    else if (h1 > h2) { synchronized (to) { synchronized (from) { move(from, to, amt); } } }
    else synchronized (TIE_LOCK) { synchronized (from) { synchronized (to) { move(from, to, amt); } } }
}
```
- Cũng cần xử lý `from == to`: monitor reentrant nên không deadlock, nhưng nên chặn ngay ở mức nghiệp vụ.
- Một phương án khác là dùng `ReentrantLock.tryLock(timeout)` cho cả hai lock, kèm backoff ngẫu nhiên.

**Câu hỏi nối tiếp:**
- *Trong hệ thống thật, chuyển tiền có làm bằng `synchronized` trong JVM không?* → Không. Có nhiều instance chạy song song nên phải dùng transaction DB (`SELECT ... FOR UPDATE` theo thứ tự id) hoặc optimistic locking. Lock trong JVM chỉ có tác dụng với một process.

**⚠️ Câu trả lời gây điểm trừ:**
- Sửa bằng cách đổi thứ tự lock "cho đúng" ở một chỗ mà không có quy tắc toàn cục.

**📖 Ôn lại:** [Phần 7.2 — Deadlock kinh điển và cách sửa](../01-giao-trinh/04-concurrency.md#p7)

</details>

### Q30. 🟡 **[Đọc code]** Đoạn code sau in ra gì? Hiện tượng này gọi là gì và hay gặp ở đâu trên production?

```java
ExecutorService pool = Executors.newFixedThreadPool(1);
Future<String> outer = pool.submit(() -> {
    Future<String> inner = pool.submit(() -> "inner");
    return inner.get();
});
System.out.println(outer.get(2, TimeUnit.SECONDS));
```

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Đoạn code ném **`TimeoutException`**. Task ngoài chiếm thread duy nhất của pool và chờ task trong. Task trong nằm trong queue, chờ một thread rảnh mà sẽ không bao giờ có. Hiện tượng này là **thread starvation deadlock** (pool-induced deadlock). Nó không xuất hiện dưới dạng "Found one Java-level deadlock" trong thread dump, vì không có monitor nào tham gia vòng chờ.

**Giải thích chi tiết:** Những chỗ hay gặp trên production:
- `CompletableFuture.join()` lồng nhau bên trong task chạy trên `commonPool` hoặc một pool nhỏ.
- `@Async` gọi một `@Async` khác rồi `get()`, khi cả hai dùng chung executor.
- Connection pool cạn do transaction lồng nhau `REQUIRES_NEW`: mỗi request cần 2 connection, pool có 10 connection, và đúng 10 request đồng thời là treo.

Cách sửa: tách pool cho các tầng phụ thuộc nhau, compose bất đồng bộ (`thenCompose`) thay vì block, luôn đặt timeout cho `get()`, và tính kích thước pool theo số tài nguyên mỗi request cần.

**Câu hỏi nối tiếp:**
- *Nhận ra hiện tượng này trong thread dump thế nào?* → Mọi worker của pool đều `WAITING` ở `FutureTask.get`/`CompletableFuture.join`, trong khi queue của pool vẫn còn task.

**⚠️ Câu trả lời gây điểm trừ:**
- "In ra `inner`."
- "Tăng pool lên 2 là xong." Lỗi chỉ lùi lại tới khi tải lớn hơn.

**📖 Ôn lại:** [Phần 7.1 — Thread pool deadlock](../01-giao-trinh/04-concurrency.md#p7)

</details>

### Q31. 🔴 **[Tình huống]** Service production đột nhiên "treo": request timeout, CPU gần 0%, không có log lỗi. Bạn điều tra thế nào? Nếu CPU 100% thì hướng điều tra khác gì?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** "Treo mà CPU thấp" gợi ý thread đang **chờ**: deadlock, pool starvation, connection pool cạn, hoặc downstream không có timeout. Việc đầu tiên là lấy **3–5 thread dump cách nhau 5–10 giây** (`jcmd <pid> Thread.print -l` hoặc `jstack -l`), sau đó nhóm thread theo stack trace. "Treo mà CPU 100%" lại gợi ý vòng lặp vô hạn, livelock, spin CAS, regex backtracking hoặc **GC thrashing**. Khi đó dùng `top -H -p <pid>` để tìm thread nóng, đổi TID sang hex rồi tìm `nid` trong dump, hoặc dùng async-profiler; đồng thời kiểm tra GC log.

**Giải thích chi tiết:** Đọc dump theo các pattern:
- Có "Found one Java-level deadlock" → deadlock monitor. Với `ReentrantLock`, phải chạy `-l` thì dump mới in ownable synchronizers.
- Hàng chục thread `WAITING` ở `HikariPool.getConnection` → connection pool cạn. Tìm xem ai đang giữ connection, có thể là một transaction chậm hoặc bị leak.
- Nhiều thread `RUNNABLE` ở `SocketInputStream.socketRead0` → downstream chậm và **không có read timeout**.
- Nhiều thread `BLOCKED` trên cùng một `<0x...>` → hot lock. Tìm thread đang "locked" monitor đó.
- Mọi worker đều `WAITING` ở `Future.get` → pool starvation (Q30).

Nhớ **giữ bằng chứng trước khi restart**: thread dump, JFR dump, metric. Về lâu dài: đặt timeout ở mọi tầng, dùng bulkhead, chạy một job dùng `ThreadMXBean.findDeadlockedThreads()` định kỳ, đặt tên thread có ý nghĩa qua `ThreadFactory`.

**Câu hỏi nối tiếp:**
- *Dump lấy được bằng những cách nào nếu image chỉ có JRE?* → `kill -3 <pid>` (dump ra stdout), `jattach`, ephemeral debug container. Với virtual thread trên Java 21: `jcmd <pid> Thread.dump_to_file -format=json`.

**⚠️ Câu trả lời gây điểm trừ:**
- "Restart pod rồi xem log." Làm vậy là mất bằng chứng.
- Kết luận từ đúng một thread dump.

**📖 Ôn lại:** [Phần 7.3 — Phát hiện: thread dump](../01-giao-trinh/04-concurrency.md#p7)

</details>

---

<a id="nhom-g"></a>
## 7. `ExecutorService` & `ThreadPoolExecutor`

### Q32. 🟢 Vì sao cần thread pool? Vì sao nhiều coding guideline cấm dùng `Executors.newFixedThreadPool` và `Executors.newCachedThreadPool`?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Platform thread tạo ra rất tốn kém (syscall, khoảng 1MB stack reserve, context switch). Thread pool giúp tái sử dụng thread, **giới hạn concurrency** và quản lý vòng đời. `newFixedThreadPool(n)` dùng `LinkedBlockingQueue` **không giới hạn**: khi task tới nhanh hơn tốc độ xử lý, queue phình ra, latency tăng vô hạn rồi OOM, và hệ thống không có backpressure. `newCachedThreadPool()` có max là `Integer.MAX_VALUE` cùng `SynchronousQueue`: một đợt burst có thể tạo ra hàng nghìn thread, dẫn tới `OutOfMemoryError: unable to create native thread`.

**Giải thích chi tiết:**
- `newSingleThreadExecutor` và `newScheduledThreadPool` cũng dùng queue không giới hạn.
- Trên production, khởi tạo `ThreadPoolExecutor` tường minh: queue có giới hạn, `ThreadFactory` đặt tên thread kèm `UncaughtExceptionHandler`, rejection policy rõ ràng, và có metrics (Micrometer `ExecutorServiceMetrics`).

**Câu hỏi nối tiếp:**
- *Spring `@Async` mặc định trong Boot có bẫy tương tự không?* → Có. `ThreadPoolTaskExecutor` auto-config của Boot có queue capacity và max size mặc định là `Integer.MAX_VALUE`, nên phải cấu hình `spring.task.execution.pool.*`.

**⚠️ Câu trả lời gây điểm trừ:**
- "Fixed pool an toàn vì số thread cố định." Câu này quên mất queue.

**📖 Ôn lại:** [Phần 8.1–8.3 — Thread pool & Executors factory](../01-giao-trinh/04-concurrency.md#p8)

</details>

### Q33. 🟡 **[Đọc code]** Với `new ThreadPoolExecutor(2, 4, 10, SECONDS, new ArrayBlockingQueue<>(2))` (rejection mặc định), submit liên tiếp 10 task, mỗi task `sleep(1s)`. Chuyện gì xảy ra với từng task? Task nào chạy trước?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Thuật toán `execute` của `ThreadPoolExecutor`: nếu số thread < core thì **tạo thread mới**; ngược lại thì **đưa vào queue**; nếu queue đầy mà số thread < max thì tạo thread non-core; nếu queue đầy và đã đạt max thì **reject**. Áp vào bài: task 1–2 tạo core thread, task 3–4 vào queue, task 5–6 tạo 2 thread non-core, task 7 bị reject. Với `AbortPolicy` mặc định, task 7 ném `RejectedExecutionException`, và nếu vòng lặp không bắt exception thì task 8–10 không bao giờ được submit. Thứ tự chạy là **1, 2, 5, 6 trước**, còn 3, 4 chạy sau khoảng 1 giây, nên không theo thứ tự submit.

**Giải thích chi tiết:**
- Hệ quả quan trọng: pool chỉ "nở" thêm thread khi **queue đã đầy**. Điều này ngược trực giác với nhiều người, vốn tưởng pool tăng thread trước rồi mới xếp hàng.
- Với queue không giới hạn, bước tạo thread non-core không bao giờ xảy ra, nên `maximumPoolSize` vô nghĩa.
- Sau `keepAliveTime` rảnh, thread non-core bị huỷ. Gọi `allowCoreThreadTimeOut(true)` thì thread core cũng bị huỷ.

**Câu hỏi nối tiếp:**
- *Muốn pool tăng thread trước rồi mới xếp hàng (như Tomcat) thì làm thế nào?* → Dùng queue có `offer()` trả `false` khi pool chưa đạt max, như `TaskQueue` của Tomcat, hoặc dùng `SynchronousQueue` kèm max lớn.

**⚠️ Câu trả lời gây điểm trừ:**
- "Task 3–4 tạo thread mới vì max là 4."

**📖 Ôn lại:** [Phần 8.2 — Tham số ThreadPoolExecutor](../01-giao-trinh/04-concurrency.md#p8)

</details>

### Q34. 🟢 Có những rejection policy nào? `CallerRunsPolicy` có bẫy gì?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Có bốn policy dựng sẵn. `AbortPolicy` (mặc định) ném `RejectedExecutionException` để caller xử lý, ví dụ trả HTTP 503. `CallerRunsPolicy` để chính thread gọi `execute` tự chạy task, tạo ra **backpressure** tự nhiên. `DiscardPolicy` bỏ task mà không báo gì, gần như không nên dùng. `DiscardOldestPolicy` bỏ task cũ nhất trong queue, hợp với dữ liệu kiểu "mới nhất thắng" như giá hay telemetry. Bẫy của `CallerRunsPolicy`: nếu caller là thread request của Tomcat hoặc event loop của Netty/Kafka consumer, thì chính thread đó bị chậm theo task. Event loop bị block, Kafka consumer có thể vượt `max.poll.interval.ms` và bị rebalance.

**Giải thích chi tiết:** Trên production thường viết policy tuỳ biến: log, tăng metric `rejected`, rồi fallback (ghi DB/Kafka để xử lý sau, hoặc trả lỗi rõ ràng). Thời gian chờ trong queue cũng là một phần latency người dùng cảm nhận. Đôi khi reject sớm (fail fast) tốt hơn xử lý một request mà client đã timeout từ lâu.

**Câu hỏi nối tiếp:**
- *Sau khi `shutdown()` mà vẫn submit thì sao?* → Task cũng bị chuyển cho rejection handler.

**⚠️ Câu trả lời gây điểm trừ:**
- Chọn `DiscardPolicy` cho job nghiệp vụ.

**📖 Ôn lại:** [Phần 8.4 — Rejection policies](../01-giao-trinh/04-concurrency.md#p8)

</details>

### Q35. 🟡 Chọn kích thước thread pool thế nào? Tính cho ví dụ: máy 8 core, mỗi request 10ms CPU + 90ms chờ DB, mục tiêu 80% CPU. DB chỉ có 20 connection thì sao?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Với tác vụ CPU-bound, số thread ≈ `N_cpu + 1`. Với tác vụ I/O-bound, công thức JCIP là `N_cpu × U_cpu × (1 + W/C)`. Áp vào ví dụ: `8 × 0.8 × (1 + 90/10) = 64` thread. Tuy nhiên **tài nguyên downstream** mới là giới hạn thật. DB chỉ có 20 connection thì pool 64 thread sẽ có khoảng 44 thread đứng chờ connection. Theo Little's Law, mỗi connection phục vụ khoảng 1 / 90ms ≈ 11 query mỗi giây, nên trần là khoảng 220 req/s, tăng thread cũng không vượt qua được.

**Giải thích chi tiết:**
- **Little's Law**: `L = λ × W`. Ví dụ 500 req/s × 0.2s = 100 request đồng thời.
- Kích thước pool phải hài hoà với connection pool, rate limit của đối tác, và CPU limit của container. `availableProcessors()` trong container lấy theo CPU quota.
- Cuối cùng vẫn phải **đo** bằng load test và giám sát `activeCount`, `queue.size`, thời gian chờ trong queue, p99.

**Câu hỏi nối tiếp:**
- *Vì sao không đặt 500 thread cho chắc?* → Context switch nhiều hơn, mỗi thread tốn bộ nhớ, và tải bị dồn xuống DB nên DB chậm đi, làm cả hệ thống chậm theo.

**⚠️ Câu trả lời gây điểm trừ:**
- Đưa ra một con số cố định ("200 thread") mà không có lập luận nào.
- Bỏ qua ràng buộc của DB.

**📖 Ôn lại:** [Phần 8.5 — Sizing](../01-giao-trinh/04-concurrency.md#p8)

</details>

### Q36. 🟡 **[Tình huống]** Một job dọn dẹp chạy bằng `scheduleAtFixedRate` mỗi 5 phút. Sau vài ngày, đội vận hành phát hiện job đã ngừng chạy từ lâu mà log không có lỗi nào. Vì sao? Phân biệt `execute` và `submit` về xử lý exception.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Với `scheduleAtFixedRate`/`scheduleWithFixedDelay`, nếu một lần chạy ném exception thì **mọi lần chạy sau bị huỷ**. Exception được lưu trong `ScheduledFuture` mà không ai gọi `get()`, nên không có log nào. Cách sửa là bọc toàn bộ thân task trong `try/catch (Throwable)` rồi log. Với `execute()`, exception làm worker chết và `UncaughtExceptionHandler` được gọi, nên có log. Với `submit()`, exception bị nuốt vào `Future`.

**Giải thích chi tiết:**

```java
scheduler.scheduleAtFixedRate(() -> {
    try { cleanup(); }
    catch (Throwable t) { log.error("cleanup failed", t); }   // không để lọt ra ngoài
}, 0, 5, TimeUnit.MINUTES);
```
- Một giải pháp toàn cục là subclass `ThreadPoolExecutor` và override `afterExecute(r, t)`: nếu `r instanceof Future<?> f && f.isDone()` thì gọi `f.get()` để lấy `ExecutionException` rồi log.
- Với Spring `@Scheduled`, mặc định có `ErrorHandler` log lỗi và lần chạy sau vẫn tiếp tục. Đây là khác biệt so với API JDK thuần.

**Câu hỏi nối tiếp:**
- *`scheduleAtFixedRate` khi task chạy lâu hơn chu kỳ thì sao?* → Các lần chạy không chồng lên nhau. Lần tiếp theo bắt đầu trễ, ngay sau khi lần trước xong.

**⚠️ Câu trả lời gây điểm trừ:**
- "Chắc thread bị chết, restart là được."

**📖 Ôn lại:** [Phần 8.6 — Exception và vòng đời](../01-giao-trinh/04-concurrency.md#p8)

</details>

### Q37. 🔴 **[Đọc code]** Cấu hình sau có vấn đề gì? Pool sẽ có tối đa bao nhiêu thread?

```java
new ThreadPoolExecutor(0, 50, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
```

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Pool chỉ có **tối đa 1 thread**. `corePoolSize = 0` nên mọi task đều đi vào queue. Queue không giới hạn nên không bao giờ đầy, và bước tạo thread non-core không bao giờ chạy. `ThreadPoolExecutor` có một xử lý đặc biệt: khi số worker bằng 0 sau khi enqueue, nó tạo đúng một worker để queue không bị kẹt. Kết quả là `max = 50` vô nghĩa, mọi task chạy tuần tự, và queue có thể phình tới OOM.

**Giải thích chi tiết:**
- Người viết thường muốn "co giãn từ 0 tới 50 thread". Muốn vậy thì phải đặt `core = max = 50` kèm `allowCoreThreadTimeOut(true)` để thread rảnh tự huỷ, và dùng queue có giới hạn.
- Loại bug này lọt qua code review vì tên tham số gây hiểu nhầm. Chỉ load test hoặc metric `poolSize` mới bắt được.

**Câu hỏi nối tiếp:**
- *Tương tự, `newFixedThreadPool(10)` có `maximumPoolSize` bằng bao nhiêu?* → 10, và tham số này cũng không có ý nghĩa vì queue không giới hạn.

**⚠️ Câu trả lời gây điểm trừ:**
- "Tối đa 50 thread."

**📖 Ôn lại:** [Phần 8 — Lỗi thường gặp](../01-giao-trinh/04-concurrency.md#p8)

</details>

---

<a id="nhom-h"></a>
## 8. `CompletableFuture` & `ForkJoinPool`

### Q38. 🟢 `Future` có hạn chế gì mà `CompletableFuture` giải quyết? `thenApply` khác `thenCompose` thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `Future` chỉ cho phép **block** bằng `get()` để lấy kết quả. Nó không có callback, không compose được, không kết hợp được nhiều future, và không hoàn thành thủ công được. `CompletableFuture` vừa là `Future` vừa là `CompletionStage`, nên xây dựng được pipeline không block. `thenApply` giống `map`: hàm `T → U`. `thenCompose` giống `flatMap`: hàm `T → CompletionStage<U>`. Nếu dùng `thenApply` với hàm trả về CF, kết quả sẽ là `CF<CF<U>>`.

**Giải thích chi tiết:**

```java
CompletableFuture<User> userF = supplyAsync(() -> fetchUser(42), IO);
CompletableFuture<List<Order>> ordersF = userF.thenCompose(u -> supplyAsync(() -> fetchOrders(u.id()), IO));
CompletableFuture<Double> rateF = supplyAsync(this::fetchRate, IO);           // độc lập → song song
CompletableFuture<Dashboard> dash = ordersF.thenCombine(rateF, Dashboard::new); // gộp hai nhánh
```
- Các nhóm method cần nhớ: `thenAccept`/`thenRun` (tiêu thụ kết quả), `thenCombine`/`allOf`/`anyOf` (kết hợp), `orTimeout`/`completeOnTimeout` (Java 9), `exceptionallyCompose` (Java 12).
- Java 19 bổ sung cho `Future` các method `resultNow()`, `exceptionNow()`, `state()`.

**Câu hỏi nối tiếp:**
- *`allOf` trả về gì?* → `CF<Void>`. Phải tự `join` từng future con, và lúc đó việc `join` không block vì tất cả đã xong.

**⚠️ Câu trả lời gây điểm trừ:**
- Gọi `.get()` ngay sau mỗi bước, biến pipeline bất đồng bộ thành tuần tự.

**📖 Ôn lại:** [Phần 9.1–9.3 — Future & CompletableFuture](../01-giao-trinh/04-concurrency.md#p9)

</details>

### Q39. 🟡 `thenApply` và `thenApplyAsync` chạy trên thread nào? Không truyền executor thì CF dùng gì? Vì sao điều đó nguy hiểm?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `thenApply(fn)` chạy `fn` trên **thread hoàn thành stage trước**. Nếu stage trước đã xong khi gọi `thenApply` thì `fn` chạy trên thread đang gọi. `thenApplyAsync(fn)` gửi `fn` vào executor, mặc định là `ForkJoinPool.commonPool()`. Có hai nguy hiểm. Thứ nhất, commonPool chỉ có khoảng `N_cpu - 1` thread và dùng chung cho cả JVM (parallel stream, các CF khác); đưa blocking I/O vào đó sẽ làm nghẽn mọi thứ. Thứ hai, nếu parallelism của commonPool nhỏ hơn 2 (container 1–2 CPU), các method `*Async` không có executor sẽ **tạo một thread mới cho mỗi task**, nên hành vi trên máy dev và trên pod khác hẳn nhau.

**Giải thích chi tiết:**
- Một `thenApply` nặng có thể chạy trên thread I/O selector của HTTP client, hoặc trên thread request của caller, mà người viết không hề ngờ tới.
- Quy tắc là **luôn truyền executor riêng** cho tác vụ blocking, và dùng các executor tách biệt theo bulkhead.

**Câu hỏi nối tiếp:**
- *Có chỉnh được commonPool không?* → Được, qua `-Djava.util.concurrent.ForkJoinPool.common.parallelism`, nhưng thay đổi này ảnh hưởng toàn JVM. Tốt hơn là dùng executor riêng.

**⚠️ Câu trả lời gây điểm trừ:**
- "`thenApply` luôn chạy trên thread mới."

**📖 Ôn lại:** [Phần 9.3 — Async hay không async](../01-giao-trinh/04-concurrency.md#p9) · [Góc nhìn Senior phần 9](../01-giao-trinh/04-concurrency.md#p9)

</details>

### Q40. 🟡 **[Đọc code]** Đoạn sau in ra gì? Khác nhau giữa `exceptionally`, `handle` và `whenComplete`?

```java
CompletableFuture<Integer> cf = CompletableFuture
    .supplyAsync(() -> { throw new IllegalStateException("boom"); })
    .thenApply(x -> (Integer) x + 1)
    .handle((v, ex) -> {
        System.out.println(ex instanceof IllegalStateException);
        return ex == null ? v : -1;
    });
System.out.println(cf.join());
```

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Kết quả in ra `false` rồi `-1`. Lỗi lan qua các stage (bước `thenApply` bị bỏ qua) và được **bọc trong `CompletionException`**, nên `ex instanceof IllegalStateException` là `false`. Phải unwrap bằng `ex.getCause()`. `exceptionally` chỉ chạy khi có lỗi và **phục hồi** bằng một giá trị thay thế. `handle` luôn chạy, nhận cả `(value, ex)` và trả về giá trị mới. `whenComplete` luôn chạy nhưng chỉ dùng cho side-effect như log: nó **không đổi** kết quả và không nuốt lỗi.

**Giải thích chi tiết:**

```java
Throwable root = (ex instanceof CompletionException c && c.getCause() != null) ? c.getCause() : ex;
```
- `join()` ném `CompletionException` (unchecked). `get()` ném `ExecutionException` (checked) với cause là lỗi gốc.
- CF thất bại mà không ai xử lý thì lỗi mất. Mọi pipeline nên kết thúc bằng bước xử lý lỗi hoặc log.

**Câu hỏi nối tiếp:**
- *Muốn fallback bằng một lời gọi bất đồng bộ khác thì dùng gì?* → `exceptionallyCompose` (Java 12+), hoặc `handle(...).thenCompose(identity)`.

**⚠️ Câu trả lời gây điểm trừ:**
- Trả lời "true" vì không biết lỗi bị bọc.
- Dùng `whenComplete` mà mong nó "bắt lỗi".

**📖 Ôn lại:** [Phần 9.4 — Exception handling](../01-giao-trinh/04-concurrency.md#p9)

</details>

### Q41. 🔴 **[Tình huống]** API gọi song song 5 nhà cung cấp giá và phải trả kết quả trong 300ms. Bạn dùng `orTimeout` và `cancel(true)` để dừng các lời gọi chậm. Cách làm này có thực sự giải phóng tài nguyên không? Thiết kế đúng thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **Không.** `CompletableFuture.cancel(true)` **không interrupt** task đang chạy, vì tham số `mayInterruptIfRunning` bị bỏ qua. Nó chỉ hoàn thành CF với `CancellationException`. `orTimeout` cũng vậy: CF hoàn thành exceptionally, nhưng HTTP call bên dưới vẫn chạy tiếp và vẫn chiếm thread lẫn connection. Thiết kế đúng có ba phần: (1) đặt **timeout ở chính HTTP client** (connect/read/response timeout) để tài nguyên được giải phóng thật; (2) dùng `completeOnTimeout(fallback)` hoặc `orTimeout` cộng `exceptionally` để từng nhà cung cấp chậm/lỗi được thay bằng `Optional.empty()`; (3) gom kết quả bằng `allOf` rồi chọn giá thấp nhất.

**Giải thích chi tiết:**

```java
List<CompletableFuture<Optional<Double>>> cfs = providers.stream()
    .map(p -> CompletableFuture.supplyAsync(p::quote, IO)      // p.quote() có HTTP timeout ~280ms
        .thenApply(Optional::of)
        .completeOnTimeout(Optional.empty(), 300, MILLISECONDS)
        .exceptionally(ex -> { log.warn("{} failed", p, ex); return Optional.empty(); }))
    .toList();
double best = CompletableFuture.allOf(cfs.toArray(CompletableFuture[]::new))
    .thenApply(v -> cfs.stream().map(CompletableFuture::join).flatMap(Optional::stream)
        .min(Double::compare).orElse(DEFAULT))
    .join();
```
- `allOf` **không fail-fast**: nó chỉ hoàn thành khi mọi CF xong. Muốn huỷ thật các task anh em khi một task lỗi thì dùng Structured Concurrency (Q56), trong đó cancel thực sự interrupt virtual thread.

**Câu hỏi nối tiếp:**
- *`FutureTask.cancel(true)` có interrupt không?* → Có. `FutureTask` (từ `executor.submit`) interrupt thread đang chạy, nhưng task vẫn phải tự phản hồi interrupt.

**⚠️ Câu trả lời gây điểm trừ:**
- "Đặt `orTimeout` là đủ, task chậm sẽ tự dừng."

**📖 Ôn lại:** [Phần 9.4 & 9.5 — cancel, allOf](../01-giao-trinh/04-concurrency.md#p9)

</details>

### Q42. 🟡 Vì sao traceId trong log (MDC) và `SecurityContext` "biến mất" khi xử lý bằng `CompletableFuture` hoặc `@Async`? Khắc phục thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** MDC, `SecurityContextHolder` và transaction của Spring đều lưu trong **`ThreadLocal`**. Khi task chạy sang thread khác, thread đó không có giá trị này, hoặc tệ hơn là còn giá trị cũ của request trước. Cách khắc phục là **wrap task hoặc executor**: capture context lúc submit, set lại lúc chạy, và clear trong `finally`. Các công cụ sẵn có: Spring `TaskDecorator`, Micrometer Context Propagation, `DelegatingSecurityContextExecutor`, `TransmittableThreadLocal`. Trên Java 25 có thể dùng `ScopedValue`.

**Giải thích chi tiết:**

```java
static Runnable withMdc(Runnable task) {
    Map<String, String> ctx = MDC.getCopyOfContextMap();     // capture ở thread gửi
    return () -> {
        Map<String, String> prev = MDC.getCopyOfContextMap();
        if (ctx != null) MDC.setContextMap(ctx); else MDC.clear();
        try { task.run(); }
        finally { if (prev != null) MDC.setContextMap(prev); else MDC.clear(); }
    };
}
```
- **Transaction không đi theo** sang thread khác: code trong `supplyAsync` nằm ngoài transaction của method gọi nó, và cũng không thấy dữ liệu chưa commit.

**Câu hỏi nối tiếp:**
- *`InheritableThreadLocal` có giải quyết được không?* → Không, nếu chạy qua thread pool (xem Q51).

**⚠️ Câu trả lời gây điểm trừ:**
- "Dùng `InheritableThreadLocal` là xong."

**📖 Ôn lại:** [Phần 9 — Góc nhìn Senior (context propagation)](../01-giao-trinh/04-concurrency.md#p9) · [Phần 13.3](../01-giao-trinh/04-concurrency.md#p13)

</details>

### Q43. 🔴 Work stealing của `ForkJoinPool` hoạt động thế nào? Vì sao gọi DB trong parallel stream lại làm chậm `CompletableFuture` ở một chỗ hoàn toàn khác của ứng dụng?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Mỗi worker có một **deque** riêng. Worker push và pop task của mình ở **đầu** deque (LIFO, tốt cho cache locality). Worker rảnh **ăn cắp** task từ **đuôi** deque của worker khác (FIFO, thường là task lớn). Nhờ vậy tải tự cân bằng mà ít tranh chấp. Parallel stream, `CompletableFuture` không truyền executor và `Arrays.parallelSort` đều dùng **chung `commonPool`** với khoảng `N_cpu - 1` thread. Blocking I/O trong parallel stream giữ chặt các worker đó: chúng bị block thì không ăn cắp được task nào, nên mọi CF mặc định khác trong JVM phải chờ.

**Giải thích chi tiết:**
- Mẫu chia để trị đúng: fork một nửa, tự `compute()` nửa còn lại, rồi `join()` nửa đã fork. Fork cả hai rồi join cả hai sẽ lãng phí một worker chỉ để chờ.
- `join()` trong FJP không block một cách ngây thơ: worker sẽ "help" chạy task khác trong lúc chờ.
- FJP có `ManagedBlocker` để bù thread khi phải block, nhưng code nghiệp vụ hiếm khi dùng.
- Scheduler mặc định của virtual thread cũng là một FJP (async mode) với số carrier ≈ số core. Đây là lý do pinning gây hại nghiêm trọng (Q53).

**Câu hỏi nối tiếp:**
- *Parallel stream có thể chạy trên pool riêng không?* → Có một mẹo là gọi stream bên trong `customFjp.submit(...)`, nhưng đó là hành vi không được spec bảo đảm. Với I/O, nên dùng executor riêng hoặc virtual threads thay vì parallel stream.

**⚠️ Câu trả lời gây điểm trừ:**
- "Parallel stream luôn nhanh hơn" và đem dùng cho I/O hoặc tập dữ liệu nhỏ.

**📖 Ôn lại:** [Phần 10 — ForkJoinPool & work stealing](../01-giao-trinh/04-concurrency.md#p10)

</details>

---

<a id="nhom-i"></a>
## 9. Synchronizers & concurrent collections

### Q44. 🟢 `CountDownLatch`, `CyclicBarrier`, `Semaphore` khác nhau thế nào? Cho mỗi loại một use case.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `CountDownLatch(n)` cho phép một hoặc nhiều thread chờ tới khi bộ đếm về 0. Nó chỉ dùng **một lần**. Use case: main chờ N thành phần khởi động xong, hoặc cổng xuất phát trong test. `CyclicBarrier(n)` cho N thread hẹn nhau ở barrier rồi cùng đi tiếp, và **tái sử dụng** được qua nhiều vòng. Use case: mô phỏng theo bước, mỗi thế hệ tính xong mới sang thế hệ sau. `Semaphore(permits)` giới hạn số thread được truy cập tài nguyên **đồng thời**. Use case: tối đa 10 lời gọi đồng thời tới API đối tác. `Phaser` là barrier nhiều pha với số bên tham gia thay đổi động.

**Giải thích chi tiết:**
- `countDown()` phải đặt trong `finally`. Nếu worker ném exception trước khi `countDown()` thì `await()` treo mãi, nên dùng `await(timeout)`.
- `CyclicBarrier`: nếu một thread bị interrupt hoặc timeout thì barrier **broken**, mọi thread đang chờ nhận `BrokenBarrierException` và phải gọi `reset()`.

**Câu hỏi nối tiếp:**
- *Muốn N thread bắt đầu đúng cùng lúc trong stress test thì làm thế nào?* → Dùng `CountDownLatch(1)` làm cổng: mọi thread `await()`, main gọi `countDown()` một lần.

**⚠️ Câu trả lời gây điểm trừ:**
- Dùng `CountDownLatch` cho bài toán lặp nhiều vòng.

**📖 Ôn lại:** [Phần 11 — Synchronizers](../01-giao-trinh/04-concurrency.md#p11)

</details>

### Q45. 🟡 **[Đọc code]** Tìm bug và giải thích: `Semaphore` khác lock ở điểm nào? Semaphore có dùng làm rate limiter được không?

```java
void call() throws Exception {
    try {
        if (!sem.tryAcquire(1, TimeUnit.SECONDS)) throw new BulkheadFullException();
        partnerApi.invoke();
    } finally {
        sem.release();
    }
}
```

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Khi `tryAcquire` thất bại, `finally` vẫn gọi `release()`, nên số permit **tăng vượt giá trị ban đầu** và giới hạn concurrency bị rò dần mà không ai hay. Sửa bằng cách chỉ `release` khi đã acquire thành công, tức là đặt `try/finally` **sau** `tryAcquire`. Semaphore **không có owner**: bất kỳ thread nào cũng `release` được, khác với lock. Semaphore chỉ giới hạn số lời gọi **đồng thời**, không giới hạn **tốc độ** (N request/giây). Rate limiting cần token bucket: Resilience4j `RateLimiter`, Bucket4j, Guava `RateLimiter`.

**Giải thích chi tiết:**

```java
if (!sem.tryAcquire(1, TimeUnit.SECONDS)) throw new BulkheadFullException();
try { partnerApi.invoke(); }
finally { sem.release(); }
```
- Với virtual threads, `Semaphore` trở thành công cụ **chính** để giới hạn concurrency tới tài nguyên khan hiếm, thay cho việc giới hạn bằng kích thước pool.
- `acquire()` không timeout có thể treo vĩnh viễn nếu permit bị rò.

**Câu hỏi nối tiếp:**
- *Làm sao chứng minh in-flight không bao giờ vượt N?* → Dùng một `AtomicInteger inFlight` cộng `maxInFlight.accumulateAndGet(now, Math::max)` trong test.

**⚠️ Câu trả lời gây điểm trừ:**
- Không thấy bug, hoặc nói "Semaphore(10) nghĩa là 10 request/giây".

**📖 Ôn lại:** [Phần 11 — Chi tiết cần biết về Semaphore](../01-giao-trinh/04-concurrency.md#p11)

</details>

### Q46. 🟡 `ConcurrentHashMap` (Java 8+) hoạt động thế nào bên trong? Vì sao không cho phép key/value `null`? `size()` có chính xác không?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** CHM dùng mảng `Node[]`. **Đọc không lock**: dựa vào field `volatile` và getVolatile. Ghi vào bin rỗng dùng **CAS**. Ghi vào bin đã có node thì `synchronized` trên **node đầu bin**, tức là lock theo từng bucket. Bin dài hơn 8 phần tử (và table ≥ 64) chuyển thành **TreeBin** (cây đỏ đen). Việc resize được nhiều thread **cùng tham gia** thông qua `ForwardingNode`. CHM cấm `null` vì `get(k) == null` sẽ mơ hồ (không có key, hay value là null?) và không thể kiểm tra bằng `containsKey` một cách nguyên tử. `size()` dùng `baseCount` cộng `CounterCell[]` (cơ chế giống `LongAdder`), nên chỉ là **xấp xỉ** khi đang có ghi đồng thời.

**Giải thích chi tiết:**
- Java 7 dùng `Segment` (mặc định 16 segment, mỗi segment là một `ReentrantLock`). Java 8 bỏ segment để lock mịn hơn.
- Iterator của CHM là **weakly consistent**: không ném `ConcurrentModificationException`, và có thể phản ánh hoặc không phản ánh các thay đổi xảy ra sau khi tạo iterator.
- Dùng đúng các thao tác atomic: `putIfAbsent`, `computeIfAbsent`, `merge(k, 1, Integer::sum)`, `compute`. Trong `compute`, trả về `null` nghĩa là xoá entry.

**Câu hỏi nối tiếp:**
- *Vì sao `HashMap` dùng đồng thời lại nguy hiểm?* → Mất dữ liệu, và trên Java 7 resize đồng thời có thể tạo vòng lặp trong linked list, gây CPU 100% vô hạn khi `get`.

**⚠️ Câu trả lời gây điểm trừ:**
- Mô tả CHM Java 8 bằng mô hình Segment.
- Dùng `size()` làm điều kiện nghiệp vụ chặt.

**📖 Ôn lại:** [Phần 12.2 — ConcurrentHashMap](../01-giao-trinh/04-concurrency.md#p12)

</details>

### Q47. 🔴 `computeIfAbsent` của `ConcurrentHashMap` có những bẫy gì? Thiết kế một cache tính giá trị tốn kém sao cho mỗi key chỉ tính **đúng một lần** mà không giữ lock của map trong lúc tính.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Có ba bẫy. (1) Hàm mapping chạy **trong khi giữ lock của bin**: tính toán lâu hoặc I/O sẽ chặn các key khác cùng bin. (2) Gọi đệ quy vào chính map (một `computeIfAbsent` lồng `computeIfAbsent`) ném `IllegalStateException: Recursive update` trên Java 9+, còn trên Java 8 có thể **treo vô hạn** (JDK-8062841). (3) Value mutable như `computeIfAbsent(k, x -> new ArrayList<>()).add(v)` từ nhiều thread: map an toàn nhưng **list thì không**. Cách thiết kế memoizer đúng là lưu `CompletableFuture<V>` trong map. Thread thắng `putIfAbsent` tự tính giá trị **ngoài lock**, các thread khác chờ trên future. Nếu tính lỗi thì xoá entry để lần sau tính lại.

**Giải thích chi tiết:**

```java
public V get(K key) throws InterruptedException, ExecutionException {
    CompletableFuture<V> f = cache.get(key);
    if (f == null) {
        CompletableFuture<V> nf = new CompletableFuture<>();
        f = cache.putIfAbsent(key, nf);
        if (f == null) {                         // mình thắng → mình tính
            f = nf;
            try { nf.complete(compute.apply(key)); }
            catch (Throwable t) { cache.remove(key, nf); nf.completeExceptionally(t); }
        }
    }
    return f.get();
}
```
- Trên production nên dùng **Caffeine** (`cache.get(key, loader)` hoặc `AsyncLoadingCache`), vì nó có sẵn eviction, expiry và metrics.

**Câu hỏi nối tiếp:**
- *Bỏ qua `remove` khi lỗi thì sao?* → Lỗi bị cache vĩnh viễn ("negative cache" ngoài ý muốn), mọi request sau đều nhận lại đúng lỗi đó.

**⚠️ Câu trả lời gây điểm trừ:**
- "Dùng `computeIfAbsent` gọi DB là đủ thread-safe" mà không nhắc tới lock của bin.

**📖 Ôn lại:** [Phần 12.2 — Bẫy của compute/merge](../01-giao-trinh/04-concurrency.md#p12) · [Bài 12.3 Memoizer](../01-giao-trinh/04-concurrency.md#p12)

</details>

### Q48. 🟢 `Hashtable`, `Collections.synchronizedMap` và `ConcurrentHashMap` khác nhau thế nào? Khi nào dùng `CopyOnWriteArrayList`?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `Hashtable` và `synchronizedMap` dùng **một lock cho toàn bộ map**, nên mọi thao tác bị tuần tự hoá. Khi iterate phải tự `synchronized(map)`, nếu không sẽ gặp `ConcurrentModificationException`. Các thao tác tổ hợp vẫn không atomic. `ConcurrentHashMap` lock theo từng bin, đọc không lock, iterator weakly consistent và có sẵn thao tác atomic. `CopyOnWriteArrayList` **copy toàn bộ mảng mỗi lần ghi**, đọc và iterate không lock và thấy một snapshot. Nó hợp khi **đọc rất nhiều, ghi rất ít**, ví dụ danh sách listener hay config.

**Giải thích chi tiết:**
- Các collection đồng thời khác: `ConcurrentLinkedQueue` (lock-free, `size()` là O(n)), `ConcurrentSkipListMap` (có thứ tự, thay `TreeMap`).
- Dùng `CopyOnWriteArrayList` cho list ghi nhiều thì mỗi lần ghi tốn O(n) và sinh rất nhiều rác.

**Câu hỏi nối tiếp:**
- *Iterator của `CopyOnWriteArrayList` có hỗ trợ `remove()` không?* → Không, nó ném `UnsupportedOperationException` vì iterator duyệt trên snapshot.

**⚠️ Câu trả lời gây điểm trừ:**
- "`synchronizedMap` và CHM giống nhau, chỉ khác tốc độ."

**📖 Ôn lại:** [Phần 12.1 & 12.3](../01-giao-trinh/04-concurrency.md#p12)

</details>

### Q49. 🟡 **[Tình huống]** Bạn implement producer–consumer bằng `BlockingQueue` để gửi email xác nhận thanh toán. Pod bị kill khi queue còn 5.000 job. Chuyện gì xảy ra và bạn thiết kế lại thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Queue nằm trong bộ nhớ nên **mất cả 5.000 job**. Với SIGKILL hoặc OOMKilled thì không có cơ hội flush. Job quan trọng cần một **nguồn sự thật bền vững**: ghi job vào DB trong cùng transaction với nghiệp vụ (**outbox pattern**) hoặc đẩy lên broker (Kafka, RabbitMQ). Queue trong bộ nhớ chỉ đóng vai trò buffer. Consumer phải xử lý **idempotent**, vì at-least-once có thể gửi trùng. Về phần trong JVM: dùng queue **bounded** (`put` block khi đầy tạo backpressure), dùng poison pill hoặc interrupt để dừng consumer, và khi graceful shutdown thì drain hoặc flush phần còn lại.

**Giải thích chi tiết:**

| Method | Ném exception | Trả giá trị | Block | Timeout |
|---|---|---|---|---|
| Thêm | `add` | `offer` | `put` | `offer(e, t, u)` |
| Lấy | `remove` | `poll` | `take` | `poll(t, u)` |

- `LinkedBlockingQueue()` không tham số có capacity `Integer.MAX_VALUE`, tức là gần như không giới hạn.
- `drainTo(list, max)` cho phép xử lý theo batch, ví dụ ghi DB batch.
- Poison pill: gửi đúng một viên cho mỗi consumer và so sánh bằng reference.

**Câu hỏi nối tiếp:**
- *Gom batch "tối đa 500 phần tử hoặc 200ms" thế nào?* → `poll(timeout)` phần tử đầu, rồi lặp `drainTo` và `poll(phần thời gian còn lại)` tới khi đủ 500 hoặc hết 200ms.

**⚠️ Câu trả lời gây điểm trừ:**
- "Thêm shutdown hook để flush là đủ." Hook không chạy khi SIGKILL hay OOMKilled.

**📖 Ôn lại:** [Phần 12.4 — BlockingQueue và producer–consumer](../01-giao-trinh/04-concurrency.md#p12)

</details>

---

<a id="nhom-j"></a>
## 10. `ThreadLocal` & truyền context

### Q50. 🟡 `ThreadLocal` hoạt động thế nào bên trong? Vì sao nó gây memory leak và **rò dữ liệu giữa các request** trong thread pool?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Mỗi `Thread` có một field `threadLocals` kiểu `ThreadLocalMap`. Trong map đó, **key là `WeakReference<ThreadLocal>`** còn **value là strong reference**. Thread của pool sống mãi, nên map của nó cũng sống mãi. Nếu không gọi `remove()`, value nằm lại đó. Khi object `ThreadLocal` bị GC, key thành `null` nhưng value vẫn bị giữ ("stale entry"). Trên Tomcat, value giữ class của webapp, kéo theo **classloader leak** và cuối cùng là `OutOfMemoryError: Metaspace` sau vài lần redeploy. Nguy hiểm hơn nữa: request của user Y chạy trên cùng thread sẽ thấy `UserContext` của user X. Đó là **lỗ hổng bảo mật**.

**Giải thích chi tiết:**

```java
RequestContext.set(req.user());
try { service.process(req); }
finally { RequestContext.clear(); }   // LUÔN remove trong finally (filter / interceptor)
```
- Khai báo `ThreadLocal` là `static final` để chỉ có một key. Nếu là field instance thì mỗi object tạo thêm một key.
- Với virtual threads (có thể lên tới hàng triệu), dùng ThreadLocal để cache object nặng như buffer hay formatter là anti-pattern, vì bộ nhớ nhân lên theo số thread.

**Câu hỏi nối tiếp:**
- *Vì sao key là weak mà vẫn leak?* → Weak chỉ áp dụng cho **key**. Value vẫn strong và chỉ được dọn khi map tình cờ gọi expunge trong lúc `set/get/remove` khác, hoặc khi thread chết.

**⚠️ Câu trả lời gây điểm trừ:**
- "ThreadLocal dùng WeakReference nên không leak."

**📖 Ôn lại:** [Phần 13.1–13.2 — ThreadLocal & memory leak](../01-giao-trinh/04-concurrency.md#p13)

</details>

### Q51. 🔴 `InheritableThreadLocal` có truyền context đúng vào task chạy trên thread pool không? Vì sao? Giải pháp chuẩn là gì?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **Không.** `InheritableThreadLocal` copy giá trị từ thread cha sang thread con **tại thời điểm tạo thread**. Thread trong pool được tạo một lần, lúc pool khởi tạo hoặc nở thêm, do một thread nào đó ngẫu nhiên kích hoạt, rồi được tái sử dụng. Task gửi sau đó mang context **cũ** từ lúc tạo worker chứ không phải context của thread đang submit. Lỗi này sai âm thầm và rất khó debug. Giải pháp chuẩn là **wrap task lúc submit**: capture, set, rồi restore/clear. Có thể dùng Spring `TaskDecorator`, Micrometer Context Propagation, `TransmittableThreadLocal`, hoặc `ScopedValue` cùng Structured Concurrency (Java 25).

**Giải thích chi tiết:**

```java
static Runnable withContext(Runnable task) {
    String captured = RequestContext.get();            // ở thread submit
    return () -> {
        String previous = RequestContext.get();
        RequestContext.set(captured);
        try { task.run(); }
        finally { if (previous == null) RequestContext.clear(); else RequestContext.set(previous); }
    };
}
// Spring: executor.setTaskDecorator(r -> withContext(r));
```
- Restore giá trị `previous` thay vì chỉ `clear()` để xử lý đúng trường hợp `CallerRunsPolicy`, khi task chạy ngay trên thread gửi.

**Câu hỏi nối tiếp:**
- *Virtual thread có kế thừa ThreadLocal không?* → Virtual thread mới có hỗ trợ `InheritableThreadLocal`. Mỗi task trong `newVirtualThreadPerTaskExecutor` chạy trên một thread mới nên không bị lỗi "context cũ", nhưng bản copy vẫn tốn bộ nhớ. `ScopedValue` là hướng được khuyến nghị.

**⚠️ Câu trả lời gây điểm trừ:**
- "Dùng `InheritableThreadLocal` để truyền traceId qua `@Async`."

**📖 Ôn lại:** [Phần 13.3 — InheritableThreadLocal](../01-giao-trinh/04-concurrency.md#p13)

</details>

---

<a id="nhom-k"></a>
## 11. Virtual Threads, Structured Concurrency, Scoped Values

### Q52. 🟢 Virtual thread là gì? Khác platform thread ở điểm nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Virtual thread (JEP 444, **final ở Java 21**) là thread nhẹ do **JVM** quản lý, không ánh xạ 1:1 với OS thread. Tạo một virtual thread chỉ tốn vài trăm byte đến vài KB, vì stack nằm trên heap dưới dạng các chunk co giãn được, nên có thể tạo **hàng triệu** thread. Virtual thread giữ nguyên mô hình lập trình blocking và thread-per-request quen thuộc, nhưng đạt khả năng mở rộng gần với code bất đồng bộ cho workload I/O-bound. Platform thread là thread OS (khoảng 1MB stack reserve, tạo đắt, context switch ở kernel), nên thường chỉ có được vài nghìn.

**Giải thích chi tiết:**

```java
try (ExecutorService ex = Executors.newVirtualThreadPerTaskExecutor()) {
    IntStream.range(0, 100_000).forEach(i -> ex.submit(() -> { Thread.sleep(Duration.ofSeconds(1)); return i; }));
}   // ~1–2 giây; với newFixedThreadPool(200) mất ~500 giây
```
- Virtual thread luôn là daemon, priority cố định. Spring Boot 3.2+ bật bằng `spring.threads.virtual.enabled=true`.
- Virtual thread tăng **throughput** (số request đồng thời), không làm từng request nhanh hơn.

**Câu hỏi nối tiếp:**
- *`jstack` có thấy virtual thread không?* → Không. Dùng `jcmd <pid> Thread.dump_to_file -format=json <file>`.

**⚠️ Câu trả lời gây điểm trừ:**
- "Virtual thread làm code chạy nhanh hơn" hoặc "giống green thread của Java 1.1".

**📖 Ôn lại:** [Phần 14.1–14.2 — Virtual threads](../01-giao-trinh/04-concurrency.md#p14)

</details>

### Q53. 🟡 Mount/unmount và pinning của virtual thread là gì? Phát hiện và xử lý pinning thế nào? JDK 24 thay đổi gì?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Virtual thread chạy **trên** một carrier thread, là platform thread thuộc một `ForkJoinPool` có số thread ≈ số core. Khi virtual thread gặp blocking mà JDK hỗ trợ (socket I/O, `sleep`, `BlockingQueue.take`, `ReentrantLock`), JDK **unmount**: chép stack của nó lên heap và giải phóng carrier cho virtual thread khác. Khi I/O sẵn sàng, virtual thread được mount lại, có thể trên carrier khác. **Pinning** là khi virtual thread không unmount được trong lúc block. Nguyên nhân thứ nhất là block bên trong `synchronized` trên **JDK 21–23**, đã được **JEP 491 (JDK 24)** sửa, kể cả `Object.wait()`. Nguyên nhân thứ hai là đang chạy native method hoặc foreign function, vẫn còn sau JDK 24. Phát hiện bằng JFR event `jdk.VirtualThreadPinned` (mặc định khi pin quá 20ms), hoặc `-Djdk.tracePinnedThreads=full|short` trên JDK 21–23.

**Giải thích chi tiết:**
- Pinning nguy hiểm vì chỉ cần N virtual thread bị pin trong lúc block I/O (N = số carrier) là **toàn bộ scheduler đứng**. Throughput sụp, thậm chí deadlock.
- Cách sửa trên JDK 21–23: đổi `synchronized` bao quanh I/O sang `ReentrantLock`, và nâng cấp thư viện (JDBC driver, HikariCP, v.v.) lên bản đã cập nhật cho Loom. Một số thao tác "capture" carrier (file I/O trên nhiều OS, DNS), và JDK bù bằng cách tạm tăng số carrier lên tối đa `maxPoolSize`.

**Câu hỏi nối tiếp:**
- *Ví dụ định lượng?* → Trên JDK 21 với 8 carrier, 1.000 task cùng `synchronized { sleep(100ms) }` mất khoảng 12.5 giây. Dùng `ReentrantLock` (hoặc chạy trên JDK 24+) chỉ mất khoảng 100–200ms.

**⚠️ Câu trả lời gây điểm trừ:**
- "Virtual thread không bao giờ block carrier."
- Không biết JEP 491 và khuyên "bỏ hết synchronized" kể cả trên JDK 24+.

**📖 Ôn lại:** [Phần 14.3 — Pinning](../01-giao-trinh/04-concurrency.md#p14)

</details>

### Q54. 🔴 **[Tình huống]** Team bật `spring.threads.virtual.enabled=true` cho một service (JDK 21). Throughput test tăng mạnh, nhưng khi lên production thì DB bị quá tải và xuất hiện nhiều lỗi `Connection is not available, request timed out`. Vì sao? Bạn xử lý thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Trước đây pool Tomcat có 200 thread và **vô tình đóng vai van giới hạn concurrency**. Có virtual threads, mọi request đều được nhận ngay, nên hàng nghìn request đồng thời cùng lao vào DB. Connection pool (ví dụ 20 connection) cạn, request chờ quá `connectionTimeout` thì lỗi, và DB bị dồn query nên chậm thêm. Virtual threads **không tăng tài nguyên hữu hạn**: một triệu virtual thread vẫn chỉ chạy được 20 query cùng lúc. Cách xử lý: đặt **giới hạn concurrency tường minh** (`Semaphore`/bulkhead) ở đầu vào hoặc quanh tầng DB, fail fast với timeout ngắn và trả 503 hoặc retry-after, cấu hình connection pool và timeout hợp lý, và kiểm tra pinning trên staging bằng JFR.

**Giải thích chi tiết:**
- Theo Little's Law, số request đồng thời mà DB chịu được ≈ số connection. Phần dư phải được **xếp hàng có giới hạn** hoặc **từ chối**, không được để tích tụ vô hạn.
- Trên JDK 21, kiểm tra thêm driver hoặc pool dùng `synchronized` quanh I/O, vì virtual thread bị pin khiến carrier bị chiếm và triệu chứng còn tệ hơn.
- Theo dõi metric: `hikaricp_connections_pending`, thời gian chờ connection, p99 latency, tỉ lệ 503.

**Câu hỏi nối tiếp:**
- *Có nên pool virtual thread, ví dụ `newFixedThreadPool(200, Thread.ofVirtual().factory())`, để giới hạn không?* → Không. Đó là anti-pattern. Hãy giới hạn **tài nguyên** bằng `Semaphore`, đừng giới hạn bằng số thread.

**⚠️ Câu trả lời gây điểm trừ:**
- "Tăng connection pool lên 1.000." Làm vậy chỉ chuyển bottleneck sang DB.
- "Tắt virtual threads đi" mà không hiểu nguyên nhân.

**📖 Ôn lại:** [Phần 14.4 — Khi nào virtual threads giúp](../01-giao-trinh/04-concurrency.md#p14) · [Góc nhìn Senior phần 14](../01-giao-trinh/04-concurrency.md#p14)

</details>

### Q55. 🟡 Khi nào **không** nên dùng virtual threads? Virtual threads có thay thế WebFlux/reactive không?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Không nên dùng cho tác vụ **CPU-bound**: số carrier bằng số core nên không nhanh hơn platform thread. Không nên dùng cho code còn `synchronized` quanh I/O trên JDK 21–23 (pinning), hoặc code dựa vào ThreadLocal để cache object nặng. Virtual threads cũng không giúp khi bottleneck là tài nguyên hữu hạn. Và không bao giờ **pool** virtual thread, vì chúng rẻ; muốn giới hạn thì dùng `Semaphore`. So với WebFlux: virtual threads cho **khả năng mở rộng tương đương** với workload I/O-bound mà vẫn viết code blocking, stack trace rõ ràng, debug dễ. Reactive vẫn có giá trị cho streaming, backpressure tinh vi, và hệ thống đã đầu tư sẵn. Với dự án mới trên Java 21+, thread-per-request cùng virtual threads thường đơn giản hơn.

**Giải thích chi tiết:**

| Giúp nhiều | Không giúp / có hại |
|---|---|
| Thread-per-request nhiều I/O chờ | CPU-bound |
| Fan-out nhiều lời gọi I/O | `synchronized` quanh I/O (JDK 21–23) |
| Thay code reactive khó đọc | ThreadLocal cache object nặng |
| 10k–1M tác vụ đồng thời | Bottleneck là DB/connection |

**Câu hỏi nối tiếp:**
- *Virtual thread có priority, daemon=false được không?* → Không. Virtual thread luôn là daemon và có priority cố định.

**⚠️ Câu trả lời gây điểm trừ:**
- "Virtual threads làm reactive lỗi thời hoàn toàn."
- "Dùng virtual thread cho xử lý ảnh, tính toán nặng."

**📖 Ôn lại:** [Phần 14.4 — Khi nào giúp, khi nào không](../01-giao-trinh/04-concurrency.md#p14)

</details>

### Q56. 🔴 Structured Concurrency và Scoped Values giải quyết vấn đề gì so với `CompletableFuture.allOf` và `ThreadLocal`? Trạng thái hiện tại (preview/final) thế nào, có dùng production được không?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **Structured Concurrency** làm cho vòng đời các subtask nằm gọn trong scope của task cha. Task cha không return khi con còn chạy. Một subtask lỗi thì các subtask còn lại bị **huỷ thật** (interrupt virtual thread). Huỷ cha thì huỷ con. Thread dump thể hiện quan hệ cha–con. Nhờ vậy không còn task "mồ côi" chạy tiếp như với `allOf`, vốn không fail-fast và `cancel` không interrupt. **Scoped Values** thay ThreadLocal cho context **bất biến**: binding chỉ tồn tại trong `ScopedValue.where(...).run(...)`, tự hết hạn khi ra khỏi scope (không cần `remove`, không leak), rẻ với hàng triệu virtual thread, và tự kế thừa vào subtask của structured concurrency. Về trạng thái: Structured Concurrency là **preview** từ Java 21 (JEP 453) và **vẫn preview ở Java 25** (JEP 505, API mới dùng `StructuredTaskScope.open(...)` với `Joiner`). Scoped Values là preview ở 21 (JEP 446) và **final ở Java 25** (JEP 506).

**Giải thích chi tiết:**

```java
// Java 21 preview API (--enable-preview)
try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
    var user  = scope.fork(() -> findUser());
    var order = scope.fork(() -> fetchOrder());
    scope.join().throwIfFailed();          // một lỗi → huỷ cái còn lại
    return new Response(user.get(), order.get());
}

static final ScopedValue<String> USER = ScopedValue.newInstance();
ScopedValue.where(USER, req.user()).run(() -> handle());   // USER.get() trong call chain
```
- Production: Scoped Values dùng được từ Java 25 LTS. Structured Concurrency thì **không nên** dùng cho production trừ khi chấp nhận `--enable-preview` và API thay đổi giữa các bản.

**Câu hỏi nối tiếp:**
- *Scoped value có `set` lại được trong scope không?* → Không. Nó bất biến, nhưng có thể rebind trong một scope lồng bên trong.

**⚠️ Câu trả lời gây điểm trừ:**
- Nói Structured Concurrency đã final ở Java 21.
- Không phân biệt được "huỷ CF" và "huỷ thực sự task bên dưới".

**📖 Ôn lại:** [Phần 14.5 — Structured Concurrency](../01-giao-trinh/04-concurrency.md#p14) · [Phần 14.6 — Scoped Values](../01-giao-trinh/04-concurrency.md#p14)

</details>

---

<a id="nhom-l"></a>
## 12. Thiết kế & kiểm thử code đồng thời

### Q57. 🟡 Khi thiết kế một class sẽ được truy cập đồng thời, bạn tiếp cận theo thứ tự ưu tiên nào? Cho ví dụ config có thể reload mà phía đọc không cần lock.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Thứ tự ưu tiên: (1) **Không chia sẻ**: thread confinement, biến local, kiến trúc single-writer. (2) **Bất biến**: mọi field `final`, không có `this` escape, collection được copy phòng thủ. (3) **Uỷ thác** cho thư viện đã kiểm chứng: CHM, `BlockingQueue`, atomics, Caffeine. (4) **Đồng bộ rõ ràng**: mỗi biến mutable dùng chung được bảo vệ bởi **đúng một** lock, ghi chú bằng `@GuardedBy`. (5) **Ghi tài liệu chính sách thread-safety** của class (Effective Java Item 82). Trong code review, câu hỏi đầu tiên luôn là: "State nào được chia sẻ, và cái gì bảo vệ nó?"

**Giải thích chi tiết:**

```java
public final class ConfigHolder {
    public record Config(int timeoutMs, List<String> hosts) {
        public Config { hosts = List.copyOf(hosts); }               // bất biến
    }
    private final AtomicReference<Config> current;
    public ConfigHolder(Config initial) { current = new AtomicReference<>(initial); }
    public Config get() { return current.get(); }                    // reader không lock, snapshot nhất quán
    public void reload(Config next) { current.set(next); }           // thay nguyên object
    public void addHost(String h) {                                  // phụ thuộc giá trị cũ → CAS loop
        current.updateAndGet(c -> { var l = new ArrayList<>(c.hosts()); l.add(h); return new Config(c.timeoutMs(), l); });
    }
}
```

**Câu hỏi nối tiếp:**
- *Phân loại `SimpleDateFormat`, `DateTimeFormatter`, `Random`?* → `SimpleDateFormat` không thread-safe vì giữ `Calendar` nội bộ. `DateTimeFormatter` immutable. `Random` thread-safe (CAS trên seed) nhưng contention cao, nên dùng `ThreadLocalRandom`.

**⚠️ Câu trả lời gây điểm trừ:**
- Câu đầu tiên đã nghĩ tới `synchronized` mà không xét chuyện loại bỏ state chia sẻ.

**📖 Ôn lại:** [Phần 15.1 — Chiến lược thiết kế](../01-giao-trinh/04-concurrency.md#p15)

</details>

### Q58. 🔴 Làm sao kiểm thử code đồng thời? Vì sao test "pass 100 lần" chưa chứng minh được gì? Kể các công cụ bạn dùng.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Bug concurrency phụ thuộc vào interleaving nên unit test thông thường gần như không bắt được. Test pass chỉ chứng minh **chưa tìm thấy lỗi**. Các kỹ thuật: (1) **stress test** với nhiều thread, dùng `CountDownLatch` làm cổng xuất phát, kiểm tra **invariant** (tổng tiền bảo toàn, không mất, không trùng) thay vì giá trị cụ thể, chạy lặp lại với `@RepeatedTest`; (2) **jcstress** để kiểm tra JMM, chạy hàng triệu lần và thống kê mọi kết quả; (3) **Lincheck** để kiểm tra linearizability của cấu trúc dữ liệu; (4) **Awaitility** thay cho `Thread.sleep` trong test bất đồng bộ; (5) thiết kế để test được: tách logic khỏi threading, inject `Executor` (trong test dùng `Runnable::run`) và `Clock`; (6) static analysis như Error Prone `@GuardedBy` và SpotBugs; (7) quan sát runtime bằng JFR và async-profiler `-e lock`.

**Giải thích chi tiết:** Hai bẫy hay gặp trong test:
- **Assertion trong thread con**: JUnit không thấy exception ném ở thread khác, nên test vẫn "xanh". Phải thu thập lỗi lại hoặc gọi `Future.get()` ở thread test.
- **Không đặt `@Timeout`**: test bị deadlock sẽ treo CI hàng giờ.

```java
@RepeatedTest(20) @Timeout(30)
void totalBalanceIsConserved() throws Exception {
    CountDownLatch start = new CountDownLatch(1);
    try (ExecutorService ex = Executors.newFixedThreadPool(16)) {
        for (int t = 0; t < 16; t++) ex.submit(() -> { start.await(); /* chuyển tiền ngẫu nhiên */ return null; });
        start.countDown();
    }
    assertEquals(expected, bank.total());
}
```

**Câu hỏi nối tiếp:**
- *Test concurrency flaky thì làm gì?* → Đừng tắt test. Đó là tín hiệu quý. Hãy chạy lặp nhiều lần với số thread lớn hơn số core để tái hiện, rồi điều tra.

**⚠️ Câu trả lời gây điểm trừ:**
- "Chạy test vài lần thấy đúng là được."
- Dùng `Thread.sleep(1000)` để "chờ thread kia xong" trong test.

**📖 Ôn lại:** [Phần 15.2 — Kiểm thử code đồng thời](../01-giao-trinh/04-concurrency.md#p15) · [Checklist tự đánh giá](../01-giao-trinh/04-concurrency.md#checklist-tu-danh-gia)

</details>

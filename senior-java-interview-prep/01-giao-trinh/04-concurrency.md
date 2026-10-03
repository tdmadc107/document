# Module 04 — Concurrency & Multithreading

> **Mục tiêu:** sau module này bạn giải thích được Java Memory Model và quan hệ happens-before; chọn đúng công cụ đồng bộ (`synchronized`, `volatile`, `Lock`, atomics, concurrent collections, synchronizers) cho từng bài toán; cấu hình `ThreadPoolExecutor` có cơ sở; viết được luồng xử lý bất đồng bộ bằng `CompletableFuture`; chẩn đoán deadlock, thread starvation, ThreadLocal leak từ thread dump; và biết khi nào virtual threads (Java 21) thực sự giúp ích.
> **Cấp độ:** Cơ bản → Nâng cao (Senior)
> **Thời lượng gợi ý:** 8 ngày (≈ 40 giờ)
> **Yêu cầu trước:** Module 01 (Java Core & OOP), Module 02 (Collections), Module 03 (Modern Java — lambda, `CompletableFuture` cơ bản)
> **Nguồn tham khảo:**
> - Trong kho: [`Java/OCP-Oracle-Certified-Professional-Java-SE-17-Developer-Study-Guide-Exam-1Z0-829pdf.pdf`](../../Java/OCP-Oracle-Certified-Professional-Java-SE-17-Developer-Study-Guide-Exam-1Z0-829pdf.pdf) — Chương 13 (Concurrency)
> - Trong kho: [`Java/OCP Oracle Certified Professional Java SE 21.pdf`](../../Java/OCP%20Oracle%20Certified%20Professional%20Java%20SE%2021.pdf) — phần Concurrency và virtual threads
> - Trong kho: [`Ebook IT/OCP_ Oracle Certified Professional Java SE 8 Programmer II Study Guide_ Exam 1Z0-809.pdf`](../../Ebook%20IT/OCP_%20Oracle%20Certified%20Professional%20Java%20SE%208%20Programmer%20II%20Study%20Guide_%20Exam%201Z0-809.pdf) — Chương 7 (Concurrency)
> - Trong kho: [`Java/Head First Java 2nd edition.pdf`](../../Java/Head%20First%20Java%202nd%20edition.pdf) — chương về networking và threads (nhập môn trực quan)
> - Trong kho: [`Java/OCP_ Oracle Certified Professional Java SE 11  Exam 1Z0-819 Practice Test.pdf`](../../Java/OCP_%20Oracle%20Certified%20Professional%20Java%20SE%2011%20%20Exam%201Z0-819%20Practice%20Test.pdf) — câu hỏi luyện tập về concurrency
> - Trong kho: [`Ebook IT/Design Pattern.pdf`](../../Ebook%20IT/Design%20Pattern.pdf) — Singleton (liên quan double-checked locking)
> - Ngoài: *Java Concurrency in Practice* (Brian Goetz và cộng sự, 2006) — "kinh thánh" về concurrency; đặc biệt Chương 3 (Sharing Objects), Chương 8 (Applying Thread Pools), Chương 16 (The Java Memory Model)
> - Ngoài: *Effective Java 3rd ed.* — Chương 11 "Concurrency" (Item 78–84)
> - Ngoài: JLS §17.4 "Memory Model" (https://docs.oracle.com/javase/specs/jls/se21/html/jls-17.html#jls-17.4); Javadoc package `java.util.concurrent` (mục "Memory Consistency Properties")
> - Ngoài: Doug Lea, "The JSR-133 Cookbook for Compiler Writers" (https://gee.cs.oswego.edu/dl/jmm/cookbook.html); Aleksey Shipilëv, "Java Memory Model Pragmatics" (https://shipilev.net/blog/2014/jmm-pragmatics/)
> - Ngoài: JEP 444 (Virtual Threads), JEP 491 (Synchronize Virtual Threads without Pinning), JEP 453/505 (Structured Concurrency), JEP 446/506 (Scoped Values) — https://openjdk.org/jeps/
> - Ngoài: OpenJDK jcstress (https://github.com/openjdk/jcstress)

## Mục lục
1. [Thread cơ bản: lifecycle, tạo thread, daemon, interrupt](#p1)
2. [Ba vấn đề cốt lõi: atomicity, visibility, ordering](#p2)
3. [Java Memory Model & happens-before](#p3)
4. [synchronized, volatile & double-checked locking](#p4)
5. [wait/notify, Lock, Condition, ReadWriteLock, StampedLock](#p5)
6. [Atomics, CAS, ABA, LongAdder](#p6)
7. [Deadlock, livelock, starvation — phát hiện và phòng tránh](#p7)
8. [ExecutorService & ThreadPoolExecutor](#p8)
9. [Future & CompletableFuture](#p9)
10. [ForkJoinPool & work stealing](#p10)
11. [Synchronizers: CountDownLatch, CyclicBarrier, Semaphore, Phaser](#p11)
12. [Concurrent collections & producer–consumer](#p12)
13. [ThreadLocal, InheritableThreadLocal & memory leak](#p13)
14. [Virtual Threads, Structured Concurrency, Scoped Values](#p14)
15. [Chiến lược thiết kế & kiểm thử code đồng thời](#p15)
16. [Dự án mini của module](#du-an-mini)
17. [Checklist tự đánh giá](#checklist-tu-danh-gia)

---

<a id="p1"></a>
## 1. Thread cơ bản: lifecycle, tạo thread, daemon, interrupt

### 1.1 Khái niệm

**Process** có không gian bộ nhớ riêng; **thread** là đơn vị thực thi bên trong process, các thread **chia sẻ heap** nhưng mỗi thread có **stack riêng** (biến local, frame), program counter riêng. Trong HotSpot, mỗi `java.lang.Thread` truyền thống (platform thread) ánh xạ **1:1** với một OS thread — tạo tốn kém (stack mặc định ~1MB virtual memory với `-Xss`, syscall tạo thread, context switch ở kernel).

### 1.2 Các trạng thái (`Thread.State`)

```
NEW ──start()──► RUNNABLE ◄──────────────────────────────┐
                   │  ├─ chờ vào synchronized ──► BLOCKED ─┤ (giành được monitor)
                   │  ├─ wait()/join()/park() ──► WAITING ─┤ (notify/unpark/thread kết thúc)
                   │  └─ sleep(t)/wait(t)/join(t)/parkNanos ─► TIMED_WAITING ─┘
                   └─ run() kết thúc / exception ──► TERMINATED
```

| State | Khi nào |
|---|---|
| `NEW` | đã tạo object, chưa `start()` |
| `RUNNABLE` | đang chạy **hoặc sẵn sàng chạy** — Java không phân biệt "running" với "ready". **Thread đang block ở I/O socket cũng hiện `RUNNABLE`** (rất hay gây nhầm khi đọc thread dump) |
| `BLOCKED` | chờ lấy monitor để vào `synchronized` |
| `WAITING` | `Object.wait()`, `Thread.join()`, `LockSupport.park()` (cả `ReentrantLock.lock()` khi bị tranh chấp!) |
| `TIMED_WAITING` | phiên bản có timeout: `sleep`, `wait(ms)`, `join(ms)`, `parkNanos`, `tryLock(timeout)` |
| `TERMINATED` | đã kết thúc |

Lưu ý: thread chờ `ReentrantLock` hiện **`WAITING` (parking)** chứ không phải `BLOCKED` — chỉ `synchronized` mới gây `BLOCKED`.

### 1.3 Tạo thread

```java
import java.util.concurrent.*;

public class CreateThreads {
    public static void main(String[] args) throws Exception {
        // 1. Runnable (ưu tiên hơn kế thừa Thread: tách "việc" khỏi "cơ chế chạy")
        Thread t1 = new Thread(() -> System.out.println("Hello từ " + Thread.currentThread().getName()), "worker-1");
        t1.start();          // start() tạo OS thread mới rồi gọi run(). Gọi run() trực tiếp = chạy trên thread hiện tại!
        t1.join();

        // 2. Builder (Java 21)
        Thread t2 = Thread.ofPlatform().name("platform-", 0).daemon(false).start(() -> System.out.println("platform"));
        Thread t3 = Thread.ofVirtual().name("virtual-", 0).start(() -> System.out.println("virtual"));
        t2.join(); t3.join();

        // 3. Thực tế production: KHÔNG new Thread thủ công, dùng ExecutorService (phần 8)
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {   // ExecutorService là AutoCloseable từ Java 19
            Future<Integer> f = pool.submit(() -> 6 * 7);                // Callable trả giá trị, ném checked exception được
            System.out.println(f.get());
        }
    }
}
```

**Daemon thread**: JVM thoát khi **tất cả non-daemon thread** kết thúc, không chờ daemon thread. Daemon dùng cho việc nền (GC, monitoring). Pitfall: daemon thread đang ghi file khi JVM thoát → bị cắt ngang, `finally` **không** được đảm bảo chạy. `setDaemon` phải gọi **trước** `start()`. Thread con kế thừa trạng thái daemon của thread tạo ra nó.

### 1.4 Interrupt — cơ chế huỷ hợp tác (cooperative cancellation)

Java **không có** cách an toàn để "giết" thread (`Thread.stop()` đã deprecated từ Java 1.2 vì để lại object ở trạng thái hỏng; từ Java 20 ném `UnsupportedOperationException`). Thay vào đó: `thread.interrupt()` đặt **cờ interrupt**; thread đích phải tự kiểm tra và dừng.

- Các method blocking (`sleep`, `wait`, `join`, `BlockingQueue.take`, `lockInterruptibly`, `Future.get`) ném `InterruptedException` **và xoá cờ**.
- `Thread.interrupted()` (static) đọc **và xoá** cờ; `isInterrupted()` chỉ đọc.
- I/O cổ điển (`InputStream.read` trên socket) **không** phản hồi interrupt; cần đóng socket. NIO `InterruptibleChannel` thì có (đóng channel khi bị interrupt).

```java
public class InterruptDemo {
    public static void main(String[] args) throws InterruptedException {
        Thread worker = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(200);                     // điểm huỷ
                    System.out.println("working...");
                } catch (InterruptedException e) {
                    // ✅ Khôi phục cờ để code phía trên (vòng while) biết mà dừng
                    Thread.currentThread().interrupt();
                }
            }
            System.out.println("dọn dẹp và thoát");
        });
        worker.start();
        Thread.sleep(700);
        worker.interrupt();
        worker.join();
    }
}
```

> 💡 **Góc nhìn Senior:** quy tắc vàng với `InterruptedException`: **hoặc ném tiếp, hoặc khôi phục cờ** (`Thread.currentThread().interrupt()`). Tuyệt đối không nuốt (`catch (InterruptedException e) {}`) — làm `shutdownNow()` của executor không dừng được task, gây treo lúc graceful shutdown (pod Kubernetes bị kill sau `terminationGracePeriodSeconds`). Đặt tên thread có ý nghĩa (qua `ThreadFactory`) — thread dump với tên `pool-7-thread-23` gần như vô dụng khi điều tra sự cố.

> ⚠️ **Lỗi thường gặp:** gọi `run()` thay vì `start()`; `start()` một thread hai lần (`IllegalThreadStateException`); exception trong thread không được ai bắt → chỉ in stack trace ra stderr rồi thread chết lặng lẽ (đặt `Thread.setDefaultUncaughtExceptionHandler` để log tập trung).

### 🛠 Bài tập phần 1

**Bài 1.1 — Quan sát trạng thái (Cơ bản)**
- Đề bài: Viết chương trình tạo thread ở đủ 6 trạng thái và in `getState()` từ main thread: NEW, RUNNABLE (vòng lặp bận), BLOCKED (chờ `synchronized`), WAITING (`wait()`), TIMED_WAITING (`sleep`), TERMINATED.
- Tiêu chí đạt: in đủ 6 trạng thái; dùng `jstack <pid>` hoặc `jcmd <pid> Thread.print` chụp thread dump và chỉ ra từng thread trong dump.

**Bài 1.2 — Cancellable task (Trung bình)**
- Đề bài: Viết task tìm số nguyên tố lớn nhất < N (vòng lặp CPU-bound, không có `sleep`) có thể huỷ bằng interrupt. Viết thêm một task đọc dữ liệu từ `ServerSocket` có thể huỷ (gợi ý: override `interrupt()` hoặc đóng socket).
- Tiêu chí đạt: cả hai task dừng trong < 100ms sau khi bị huỷ; giải thích vì sao socket I/O cổ điển không phản hồi interrupt.

**Bài 1.3 — Graceful shutdown (Nâng cao)**
- Đề bài: Viết ứng dụng có 4 worker thread xử lý job từ queue. Đăng ký shutdown hook (`Runtime.addShutdownHook`) để khi nhận SIGTERM: ngừng nhận job mới, chờ job đang chạy xong tối đa 10 giây, sau đó interrupt, log các job chưa xử lý.
- Tiêu chí đạt: test bằng `kill -TERM <pid>`; không mất job đã nhận mà chưa ghi log; giải thích điều gì xảy ra với `kill -9`.

<details>
<summary>Gợi ý lời giải</summary>

1.1: BLOCKED — main giữ `synchronized(lock)` rồi start thread cũng cố `synchronized(lock)`; WAITING — thread gọi `lock.wait()` trong `synchronized`; ngủ ngắn ở main trước khi đọc state để thread kịp chuyển trạng thái.

1.2: CPU-bound: kiểm tra `Thread.currentThread().isInterrupted()` mỗi K vòng lặp.
```java
class SocketReader extends Thread {
    private final Socket socket;
    @Override public void interrupt() {
        try { socket.close(); } catch (IOException ignored) {} finally { super.interrupt(); }
    }
}
```
(JCIP §7.1.6 "Encapsulating nonstandard cancellation with newTaskFor" mô tả kỹ thuật này.)

1.3:
```java
Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    pool.shutdown();
    try {
        if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
            List<Runnable> pending = pool.shutdownNow();
            log.warn("Bỏ dở {} job", pending.size());
        }
    } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
}));
```
`kill -9` (SIGKILL) không chạy shutdown hook → cần thiết kế job idempotent / at-least-once.
</details>

---

<a id="p2"></a>
## 2. Ba vấn đề cốt lõi: atomicity, visibility, ordering

Mọi bug concurrency đều quy về một (hoặc nhiều) trong ba vấn đề:

### 2.1 Atomicity — race condition

`count++` **không** atomic: gồm 3 bước *read → modify → write*. Hai thread xen kẽ → mất cập nhật (lost update).

```java
public class RaceDemo {
    static int count = 0;
    public static void main(String[] args) throws InterruptedException {
        Runnable inc = () -> { for (int i = 0; i < 1_000_000; i++) count++; };
        Thread a = new Thread(inc), b = new Thread(inc);
        a.start(); b.start(); a.join(); b.join();
        System.out.println(count); // thường < 2_000_000, khác nhau mỗi lần chạy
    }
}
```

Hai dạng race condition kinh điển:
- **Read-modify-write**: `count++`, `balance = balance - amount`.
- **Check-then-act**: `if (!map.containsKey(k)) map.put(k, v);` — giữa check và act, thread khác đã chen vào. Lazy init (`if (instance == null) instance = new X();`) là trường hợp đặc biệt.

**Data race** (thuật ngữ JMM) khác **race condition**: data race = hai truy cập cùng biến, ít nhất một là ghi, không có happens-before giữa chúng. Có thể có race condition mà không data race (mọi truy cập đều synchronized nhưng logic check-then-act tách thành hai khối synchronized riêng) và ngược lại.

### 2.2 Visibility

Thread A ghi biến, thread B có thể **không bao giờ thấy** giá trị mới nếu không có đồng bộ. Nguyên nhân: CPU cache/store buffer, register, và **JIT compiler** có thể hoist việc đọc biến ra khỏi vòng lặp.

```java
public class VisibilityDemo {
    static boolean stop = false;     // thử thêm 'volatile' để sửa
    public static void main(String[] args) throws InterruptedException {
        Thread t = new Thread(() -> {
            long i = 0;
            while (!stop) i++;       // JIT có thể biến thành: if (!stop) while (true) i++;
            System.out.println("stopped at " + i);
        });
        t.start();
        Thread.sleep(1000);
        stop = true;
        System.out.println("main set stop = true");
        // Trên HotSpot server JIT, thread t thường chạy MÃI MÃI.
    }
}
```

### 2.3 Ordering (reordering)

Compiler, JIT và CPU được phép **sắp xếp lại** lệnh miễn là kết quả **trong một thread** không đổi (as-if-serial). Nhưng thread khác có thể quan sát thứ tự khác.

```java
// Thread 1          // Thread 2
x = 1;               r1 = y;
y = 1;               r2 = x;
// Có thể ra r1 == 1 && r2 == 0 ?  → CÓ (nếu không đồng bộ), do reordering hoặc store buffer.
```

> 💡 **Góc nhìn Senior:** trên x86 (TSO — memory model khá mạnh) nhiều bug visibility/ordering "không tái hiện được", nhưng trên ARM (AWS Graviton, Apple Silicon) — memory model yếu hơn — chúng xuất hiện. Code "chạy đúng nhiều năm" có thể vỡ khi chuyển hạ tầng sang Graviton. Đừng lập luận dựa trên phần cứng; hãy lập luận dựa trên **JMM**.

> ⚠️ **Lỗi thường gặp:** nghĩ rằng `Collections.synchronizedMap` làm check-then-act an toàn (mỗi method atomic nhưng **tổ hợp** thì không); dùng `volatile` cho `count++`; nghĩ rằng "chỉ một thread ghi, nhiều thread đọc thì không cần đồng bộ" (vẫn cần cho visibility).

### 🛠 Bài tập phần 2

**Bài 2.1 — Lost update (Cơ bản)**
- Đề bài: Chạy `RaceDemo` 10 lần, ghi kết quả. Sửa bằng 3 cách: `synchronized`, `AtomicInteger`, `LongAdder`. Đo thời gian mỗi cách với 8 thread.
- Tiêu chí đạt: cả 3 cách cho đúng 8_000_000; bảng thời gian kèm nhận xét.

**Bài 2.2 — Visibility (Trung bình)**
- Đề bài: Tái hiện `VisibilityDemo` treo. Chạy lại với `-Xint` (chỉ interpreter) và quan sát. Sửa bằng `volatile` và bằng `synchronized` getter/setter. Thử thêm `System.out.println` trong vòng lặp — vì sao bug "biến mất"?
- Tiêu chí đạt: giải thích được vai trò của JIT (hoisting) và vì sao `println` (có synchronized bên trong + không thể hoist qua lời gọi phức tạp) che giấu bug — và vì sao đó **không phải** cách sửa.

**Bài 2.3 — Check-then-act (Nâng cao)**
- Đề bài: Viết class `SeatBooking` với `Map<String, String> seat → user` dùng `ConcurrentHashMap`. Implement `book(seat, user)` sai (containsKey + put) và đúng (`putIfAbsent`). Viết test 100 thread cùng đặt 1 ghế, chứng minh bản sai có thể cho > 1 người "thành công".
- Tiêu chí đạt: test tái hiện được lỗi với xác suất cao (dùng `CountDownLatch` để các thread xuất phát cùng lúc); bản đúng luôn đúng 1 người thành công.

<details>
<summary>Gợi ý lời giải</summary>

2.2: `-Xint` → mỗi lần lặp đọc lại field từ bộ nhớ, bug thường không xuất hiện. `println` gọi `synchronized` trên `PrintStream` — trên thực tế HotSpot không hoist được đọc `stop` qua lời gọi đó, nhưng JMM **không đảm bảo** visibility trừ khi cùng monitor.

2.3:
```java
boolean bookWrong(String seat, String user) {
    if (seats.containsKey(seat)) return false;
    Thread.onSpinWait();              // tăng xác suất xen kẽ
    seats.put(seat, user); return true;
}
boolean bookRight(String seat, String user) { return seats.putIfAbsent(seat, user) == null; }

CountDownLatch start = new CountDownLatch(1);
// mỗi thread: start.await(); if (bookWrong("A1", "u"+i)) success.incrementAndGet();
start.countDown();
```
</details>

---

<a id="p3"></a>
## 3. Java Memory Model & happens-before

### 3.1 JMM là gì

Java Memory Model (JLS §17.4, viết lại bởi JSR-133 trong Java 5) là **hợp đồng** giữa lập trình viên và JVM: mô tả một lần đọc biến được phép thấy những giá trị ghi nào. JMM không nói về cache hay CPU; nó định nghĩa quan hệ **happens-before (HB)**:

> Nếu hành động A **happens-before** hành động B, thì mọi kết quả của A (và mọi thứ trước A) **nhìn thấy được** bởi B, và A được coi như xảy ra trước B.

Chương trình **correctly synchronized** (không có data race) thì mọi lần thực thi đều như **sequentially consistent** (SC-DRF guarantee). Đây là lý do: *chỉ cần loại bỏ data race bằng HB, bạn được phép suy nghĩ như thể không có reordering.*

### 3.2 Các quy tắc happens-before (cần thuộc)

1. **Program order**: trong cùng một thread, mỗi hành động HB hành động đứng sau nó theo thứ tự chương trình.
2. **Monitor lock**: `unlock` một monitor HB mọi `lock` **sau đó** trên **cùng** monitor.
3. **Volatile**: ghi biến volatile HB mọi lần đọc **sau đó** của **cùng** biến đó.
4. **Thread start**: `t.start()` HB mọi hành động trong thread `t`.
5. **Thread termination**: mọi hành động trong `t` HB việc thread khác phát hiện `t` kết thúc (`t.join()` return, `t.isAlive() == false`).
6. **Interrupt**: `t.interrupt()` HB việc `t` phát hiện bị interrupt.
7. **Finalizer**: kết thúc constructor HB bắt đầu finalizer.
8. **Transitivity**: A HB B, B HB C ⇒ A HB C.

Bổ sung từ thư viện `java.util.concurrent` (Javadoc "Memory Consistency Properties"):
- Đưa object vào concurrent collection HB truy cập/lấy object đó ra ở thread khác.
- `executor.submit(task)` HB bắt đầu thực thi task; task hoàn thành HB `future.get()` trả về.
- `countDown()` HB `await()` return; `release()` của Semaphore HB `acquire()` thành công; các hành động trước `barrier.await()` HB hành động sau barrier ở các thread khác.
- `CompletableFuture`: hành động trong stage trước HB stage phụ thuộc.

**Final field semantics** (JLS §17.5): nếu object được construct đúng cách (`this` không bị "escape" trong constructor), mọi thread thấy được giá trị của các field `final` (và mọi thứ reachable qua chúng tại thời điểm cuối constructor) **mà không cần đồng bộ**. Đây là nền tảng của immutable object an toàn.

### 3.3 "Piggybacking" trên happens-before

```java
class Publisher {
    private int data;                  // KHÔNG volatile
    private volatile boolean ready;    // volatile

    void publish() {                   // Thread A
        data = 42;                     // (1)
        ready = true;                  // (2) volatile write
    }
    void consume() {                   // Thread B
        if (ready) {                   // (3) volatile read thấy true
            System.out.println(data);  // (4) chắc chắn in 42
        }
    }
}
// (1) HB (2) [program order], (2) HB (3) [volatile], (3) HB (4) [program order] ⇒ (1) HB (4)
```

### 3.4 Bên dưới nắp capo: memory barriers

JVM hiện thực HB bằng **memory barrier/fence** (LoadLoad, LoadStore, StoreStore, StoreLoad) theo JSR-133 Cookbook. Trên x86, chỉ StoreLoad tốn kém thực sự (`lock addl` hoặc `mfence`) — vì vậy **volatile write** tốn hơn volatile read. Trên ARM, cần các lệnh `dmb`/`ldar`/`stlr`. `VarHandle` (Java 9) cho phép chọn mức yếu hơn: `getAcquire/setRelease` (acquire/release), `getOpaque/setOpaque`, `getPlain` — dùng trong thư viện hiệu năng cao, hiếm khi cần trong code ứng dụng.

### 3.5 Safe publication

Một object được **publish an toàn** (mọi thread thấy trạng thái đầy đủ của nó) khi reference tới nó được:
- khởi tạo trong static initializer (class init có lock ngầm), hoặc
- lưu vào field `volatile` hoặc `AtomicReference`, hoặc
- lưu vào field `final` của một object được construct đúng cách, hoặc
- lưu vào field được bảo vệ bởi lock, hoặc đưa vào concurrent collection.

Unsafe publication: `static Holder holder; holder = new Holder(42);` — thread khác có thể thấy `holder != null` nhưng field bên trong **chưa được khởi tạo** (giá trị mặc định 0), vì phép gán reference có thể được reorder trước các lệnh ghi field trong constructor.

**`this` escape**: đăng ký listener/khởi động thread trong constructor (`new Thread(this::run).start()` trong constructor; `eventBus.register(this)`) → object bị thread khác thấy khi chưa construct xong, kể cả field `final`. Dùng static factory: construct xong rồi mới đăng ký.

> 💡 **Góc nhìn Senior:** khi phỏng vấn hỏi "volatile hoạt động thế nào", câu trả lời ở mức Senior không dừng ở "đọc từ main memory" (mô hình sai lệch — CPU hiện đại có cache coherence MESI; vấn đề là reordering và store buffer). Hãy trả lời bằng **happens-before**: ghi volatile HB đọc volatile tiếp theo của cùng biến, nên mọi ghi trước đó cũng visible; JVM hiện thực bằng memory barrier ngăn reordering qua điểm volatile.

> ⚠️ **Lỗi thường gặp:** nghĩ rằng `synchronized` trên **hai object khác nhau** tạo HB với nhau (không — phải cùng monitor); publish object mutable qua field không volatile rồi "chỉ đọc"; dùng `Thread.sleep` để "đợi thread kia ghi xong" (sleep không tạo HB).

### 🛠 Bài tập phần 3

**Bài 3.1 — Vẽ chuỗi HB (Cơ bản)**
- Đề bài: Cho 5 đoạn code nhỏ (tự soạn: volatile flag, synchronized trên cùng lock, synchronized trên lock khác nhau, `Thread.join`, `ExecutorService.submit` + `Future.get`). Với mỗi đoạn, vẽ chuỗi HB và kết luận giá trị in ra có được đảm bảo không.
- Tiêu chí đạt: mỗi kết luận dẫn chiếu quy tắc HB cụ thể.

**Bài 3.2 — Unsafe publication (Trung bình)**
- Đề bài: Viết class `Holder { int n; Holder(int n){this.n=n;} void assertSane(){ if (n != n) throw ...; } }` (ví dụ nổi tiếng JCIP §3.5). Giải thích vì sao về lý thuyết `assertSane` có thể ném lỗi khi Holder được publish không an toàn. Sửa bằng 3 cách publish an toàn.
- Tiêu chí đạt: giải thích được vì sao `final int n` cũng sửa được vấn đề.

**Bài 3.3 — jcstress (Nâng cao)**
- Đề bài: Cài jcstress, viết test cho kịch bản reordering `x=1; r1=y` / `y=1; r2=x` và đếm tần suất kết quả `(0,0)`. Thêm `volatile` cho `x`, `y` và chạy lại.
- Tiêu chí đạt: báo cáo kết quả trên máy bạn (ghi rõ kiến trúc CPU); giải thích vì sao `(0,0)` xuất hiện trên x86 dù x86 là TSO (store buffer → StoreLoad reordering).

<details>
<summary>Gợi ý lời giải</summary>

3.3 khung test:
```java
@JCStressTest
@Outcome(id = {"0, 1", "1, 0", "1, 1"}, expect = Expect.ACCEPTABLE, desc = "SC")
@Outcome(id = "0, 0", expect = Expect.ACCEPTABLE_INTERESTING, desc = "StoreLoad reordering")
@State
public class Dekker {
    int x, y;
    @Actor public void a1(II_Result r) { x = 1; r.r1 = y; }
    @Actor public void a2(II_Result r) { y = 1; r.r2 = x; }
}
```
Với `volatile int x, y` → `(0,0)` biến mất vì JVM chèn StoreLoad barrier sau volatile write.
</details>

---

<a id="p4"></a>
## 4. synchronized, volatile & double-checked locking

### 4.1 `synchronized`

Mỗi object Java có một **monitor** (intrinsic lock). `synchronized` đảm bảo:
1. **Mutual exclusion** (atomicity cho khối code),
2. **Visibility/ordering** qua quy tắc HB unlock → lock.

```java
public class Counter {
    private long value;                                   // được bảo vệ bởi 'this'
    public synchronized void inc() { value++; }           // lock = this
    public synchronized long get() { return value; }      // ĐỌC cũng phải synchronized để có visibility

    private static int instances;
    public static synchronized void track() { instances++; } // lock = Counter.class

    private final Object lock = new Object();             // private lock: client bên ngoài không thể lock nhầm
    private int other;
    public void incOther() { synchronized (lock) { other++; } }
}
```

Đặc điểm:
- **Reentrant**: thread đang giữ monitor có thể vào lại (đệ quy, gọi method synchronized khác cùng object) — JVM đếm số lần.
- Lock tự giải phóng khi rời khối, **kể cả khi có exception**.
- **Không** interruptible khi chờ lock, **không** có timeout, **không** fair.
- Ở bytecode: synchronized block → `monitorenter`/`monitorexit` (kèm một `monitorexit` trong exception handler); synchronized method → cờ `ACC_SYNCHRONIZED`.

### 4.2 Object header & các trạng thái lock (tóm tắt)

Mỗi object có header gồm **mark word** (64-bit) + class pointer (nén). Mark word lưu hash code, tuổi GC, và **trạng thái lock**:
- **Unlocked**.
- **Biased locking** — tối ưu cho trường hợp chỉ một thread lock: **bị tắt mặc định và deprecated từ JDK 15 (JEP 374)**, cờ liên quan bị loại bỏ ở các bản sau. Đừng trả lời phỏng vấn như thể biased locking vẫn là mặc định.
- **Lightweight (thin) lock**: khi không tranh chấp, lock bằng CAS trên mark word (legacy: con trỏ tới lock record trên stack; JDK 21 thêm chế độ lightweight locking mới dùng lock-stack theo thread, trở thành mặc định ở các bản sau).
- **Inflated (heavyweight) monitor**: khi có tranh chấp hoặc gọi `wait()`, lock được "phồng" thành `ObjectMonitor` (cấu trúc native có entry list, wait set); thread chờ sẽ spin một chút (adaptive spinning) rồi park ở OS.

JIT còn tối ưu: **lock elision** (bỏ lock trên object không escape — escape analysis), **lock coarsening** (gộp nhiều lock/unlock liên tiếp trên cùng object).

### 4.3 `volatile`

**Đảm bảo:**
- Visibility: đọc volatile luôn thấy lần ghi gần nhất (theo HB).
- Ordering: các lệnh trước volatile write không bị đẩy xuống sau; các lệnh sau volatile read không bị đẩy lên trước.
- Atomicity cho **đọc/ghi đơn lẻ** `long`/`double` (biến `long`/`double` không volatile có thể bị "word tearing" — JLS §17.7 cho phép ghi 64-bit thành hai lần 32-bit).

**KHÔNG đảm bảo:**
- Atomicity cho thao tác phức hợp (`count++`, check-then-act).
- Visibility "sâu": `volatile int[] arr` — chỉ reference là volatile, `arr[i] = x` không volatile (dùng `AtomicIntegerArray`). Tương tự `volatile List` không làm list thread-safe.

Khi nào dùng volatile: cờ trạng thái (`running`, `shutdownRequested`) chỉ một bên ghi; publish một reference tới object immutable; biến mà giá trị mới không phụ thuộc giá trị cũ.

### 4.4 Double-checked locking (DCL)

```java
// ❌ DCL hỏng (trước khi biết JMM)
public class Singleton {
    private static Singleton instance;
    public static Singleton get() {
        if (instance == null) {                         // check 1 không có lock
            synchronized (Singleton.class) {
                if (instance == null) instance = new Singleton(); // ghi reference có thể reorder trước khi constructor chạy xong
            }
        }
        return instance;  // thread khác có thể thấy object chưa khởi tạo xong
    }
}

// ✅ DCL đúng (Java 5+): instance phải volatile
public class SafeSingleton {
    private static volatile SafeSingleton instance;
    private final Config config;
    private SafeSingleton() { config = Config.load(); }
    public static SafeSingleton get() {
        SafeSingleton local = instance;                 // đọc volatile 1 lần (tối ưu nhỏ)
        if (local == null) {
            synchronized (SafeSingleton.class) {
                local = instance;
                if (local == null) instance = local = new SafeSingleton();
            }
        }
        return local;
    }
}

// ✅✅ Đơn giản hơn: Initialization-on-demand holder (lazy + thread-safe nhờ class init của JVM)
public class HolderSingleton {
    private HolderSingleton() {}
    private static class Holder { static final HolderSingleton INSTANCE = new HolderSingleton(); }
    public static HolderSingleton get() { return Holder.INSTANCE; }
}

// ✅✅ Hoặc enum singleton (Effective Java Item 3) — chống cả reflection & serialization
public enum EnumSingleton { INSTANCE; }
```

DCL vẫn có chỗ dùng: lazy init **field instance** (không phải static) tốn kém — Effective Java Item 83 đưa đúng mẫu DCL với biến local như trên.

> 💡 **Góc nhìn Senior:**
> - Giữ lock **càng ngắn càng tốt**; không gọi I/O, không gọi "alien method" (callback, listener do người khác cung cấp) bên trong lock — đó là con đường ngắn nhất tới deadlock (Effective Java Item 79).
> - `synchronized` trên `String` literal, `Integer` (cache -128..127), `Boolean` → vô tình chia sẻ lock với code khác trong JVM. Luôn lock trên `private final Object`.
> - Java 21: `synchronized` trong virtual thread có thể **pin** carrier thread (đến JDK 24 mới được sửa — xem phần 14). Đây là lý do nhiều thư viện chuyển từ `synchronized` sang `ReentrantLock` khi chuẩn bị cho virtual threads.

> ⚠️ **Lỗi thường gặp:** synchronized setter nhưng không synchronized getter; synchronized trên field không final (lock object bị đổi → hai thread lock hai object khác nhau); `volatile` cho counter; đồng bộ trên `this` trong class public (client có thể lock cùng object gây DoS).

### 🛠 Bài tập phần 4

**Bài 4.1 — Thread-safe class (Cơ bản)**
- Đề bài: Viết `BankAccount` với `deposit`, `withdraw` (không cho âm), `getBalance`, `transferTo(BankAccount other, long amount)`. Dùng `synchronized`.
- Tiêu chí đạt: test 16 thread chuyển tiền qua lại ngẫu nhiên giữa 10 tài khoản 100.000 lần; tổng tiền không đổi; **chưa** cần xử lý deadlock (để dành phần 7) — nhưng hãy ghi chú chỗ có thể deadlock.

**Bài 4.2 — So sánh các kiểu singleton (Trung bình)**
- Đề bài: Implement 5 kiểu singleton (eager, synchronized method, DCL volatile, holder, enum). Benchmark JMH truy cập `get()` từ 8 thread. Thử tấn công enum và holder bằng reflection và serialization.
- Tiêu chí đạt: bảng kết quả; giải thích vì sao synchronized method chậm nhất dưới tranh chấp; enum chống được cả hai kiểu tấn công.

**Bài 4.3 — Đọc bytecode & JIT (Nâng cao)**
- Đề bài: `javap -c` một method synchronized block, chỉ ra `monitorenter`/`monitorexit` và exception table. Dùng JMH với `-prof perfasm` (Linux) hoặc chạy benchmark lock trên object không escape để quan sát **lock elision** (so sánh với khi object escape).
- Tiêu chí đạt: giải thích kết quả benchmark; nêu điều kiện để escape analysis loại bỏ lock.

<details>
<summary>Gợi ý lời giải</summary>

4.1: `transferTo` cần lock cả hai tài khoản: `synchronized(this) { synchronized(other) { ... } }` → deadlock khi A→B và B→A đồng thời. Phần 7 sửa bằng lock ordering theo id.

4.3:
```java
@Benchmark public int elided() {
    Object lock = new Object();            // không escape → JIT bỏ lock
    synchronized (lock) { return x++; }
}
@Benchmark public int shared() { synchronized (sharedLock) { return x++; } }
```
</details>

---

<a id="p5"></a>
## 5. wait/notify, Lock, Condition, ReadWriteLock, StampedLock

### 5.1 `wait` / `notify` / `notifyAll`

Cơ chế **guarded wait** gắn với monitor: thread chờ một điều kiện trên trạng thái chia sẻ.

```java
public class BoundedBuffer<T> {
    private final Object[] items;
    private int head, tail, count;
    public BoundedBuffer(int cap) { items = new Object[cap]; }

    public synchronized void put(T t) throws InterruptedException {
        while (count == items.length)   // ✅ LUÔN dùng while, không dùng if
            wait();                     // nhả monitor, vào wait set; khi tỉnh dậy phải lấy lại monitor
        items[tail] = t; tail = (tail + 1) % items.length; count++;
        notifyAll();                    // đánh thức cả consumer lẫn producer đang chờ
    }

    @SuppressWarnings("unchecked")
    public synchronized T take() throws InterruptedException {
        while (count == 0) wait();
        T t = (T) items[head]; items[head] = null; head = (head + 1) % items.length; count--;
        notifyAll();
        return t;
    }
}
```

Quy tắc:
- Gọi `wait/notify` mà không giữ monitor của chính object đó → `IllegalMonitorStateException`.
- **Spurious wakeup**: thread có thể tỉnh dậy mà không ai notify → luôn kiểm tra điều kiện trong `while`.
- `notify()` đánh thức **một** thread bất kỳ trong wait set — nếu producer và consumer cùng chờ trên một monitor, `notify` có thể đánh thức nhầm loại → "lost wakeup", hệ thống treo. Mặc định dùng `notifyAll()` trừ khi chứng minh được mọi thread chờ cùng một điều kiện và mỗi notify chỉ cho phép một thread tiến lên.
- Effective Java Item 81: "Prefer concurrency utilities to wait and notify" — trong code mới, dùng `BlockingQueue`, `CountDownLatch`, `Condition`.

### 5.2 `ReentrantLock` và `Condition`

```java
import java.util.concurrent.locks.*;
import java.util.concurrent.TimeUnit;

public class LockBuffer<T> {
    private final Object[] items;
    private int head, tail, count;
    private final ReentrantLock lock = new ReentrantLock();      // new ReentrantLock(true) = fair
    private final Condition notFull = lock.newCondition();       // nhiều condition trên 1 lock
    private final Condition notEmpty = lock.newCondition();

    public LockBuffer(int cap) { items = new Object[cap]; }

    public boolean offer(T t, long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        lock.lockInterruptibly();                                // có thể huỷ khi đang chờ lock
        try {
            while (count == items.length) {
                if (nanos <= 0) return false;
                nanos = notFull.awaitNanos(nanos);               // trả về thời gian còn lại
            }
            items[tail] = t; tail = (tail + 1) % items.length; count++;
            notEmpty.signal();                                   // signal đúng loại thread → không cần signalAll
            return true;
        } finally {
            lock.unlock();                                       // ✅ LUÔN unlock trong finally
        }
    }

    @SuppressWarnings("unchecked")
    public T take() throws InterruptedException {
        lock.lockInterruptibly();
        try {
            while (count == 0) notEmpty.await();
            T t = (T) items[head]; items[head] = null; head = (head + 1) % items.length; count--;
            notFull.signal();
            return t;
        } finally { lock.unlock(); }
    }
}
```

So sánh `synchronized` vs `ReentrantLock`:

| Tiêu chí | `synchronized` | `ReentrantLock` |
|---|---|---|
| Cú pháp | gọn, tự unlock | phải `try/finally` |
| Timeout / `tryLock` | không | có |
| Interruptible khi chờ | không | `lockInterruptibly()` |
| Fairness | không | tuỳ chọn (fair chậm hơn đáng kể do mất "barging") |
| Nhiều điều kiện | 1 wait set | nhiều `Condition` |
| Quan sát | thread dump | thêm `isLocked`, `getQueueLength`, `hasQueuedThreads` |
| Virtual thread (JDK 21–23) | pin carrier | không pin |
| Hiện thực | monitor trong JVM | `AbstractQueuedSynchronizer` (AQS): một `volatile int state` + hàng đợi CLH các node, park/unpark bằng `LockSupport` |

**AQS** là nền tảng của `ReentrantLock`, `Semaphore`, `CountDownLatch`, `ReentrantReadWriteLock`, `ThreadPoolExecutor.Worker` — hiểu AQS là hiểu phần lớn `j.u.c.locks`.

**Fairness**: lock không công bằng cho phép thread vừa tới "chen ngang" (barging) nếu lock vừa rảnh → throughput cao hơn vì không phải đánh thức thread đang ngủ. Fair lock giảm starvation nhưng throughput giảm mạnh. `tryLock()` (không tham số) **bỏ qua** fairness kể cả với fair lock.

### 5.3 `ReadWriteLock`

`ReentrantReadWriteLock`: nhiều reader đồng thời **hoặc** một writer.

```java
import java.util.*;
import java.util.concurrent.locks.*;

public class RwCache<K, V> {
    private final Map<K, V> map = new HashMap<>();
    private final ReadWriteLock rw = new ReentrantReadWriteLock();

    public V get(K k) {
        rw.readLock().lock();
        try { return map.get(k); } finally { rw.readLock().unlock(); }
    }
    public void put(K k, V v) {
        rw.writeLock().lock();
        try { map.put(k, v); } finally { rw.writeLock().unlock(); }
    }
}
```
- **Không nâng cấp** read → write: giữ read lock rồi gọi `writeLock().lock()` → **deadlock** (writer chờ mọi reader nhả, kể cả chính nó).
- **Hạ cấp** write → read được phép: lấy read lock khi đang giữ write lock, rồi nhả write lock.
- Chỉ đáng dùng khi đọc **nhiều hơn hẳn** ghi **và** thời gian giữ lock đủ dài; với critical section ngắn, chi phí quản lý RW lock (CAS trên state chung giữa các reader → cache line contention) có thể khiến nó chậm hơn `synchronized`. Với map, `ConcurrentHashMap` gần như luôn tốt hơn.

### 5.4 `StampedLock` (Java 8)

Ba chế độ: write, read, và **optimistic read** (không lock gì cả, chỉ lấy "stamp" rồi validate).

```java
import java.util.concurrent.locks.StampedLock;

public class Point {
    private double x, y;
    private final StampedLock sl = new StampedLock();

    public void move(double dx, double dy) {
        long stamp = sl.writeLock();
        try { x += dx; y += dy; } finally { sl.unlockWrite(stamp); }
    }

    public double distanceFromOrigin() {
        long stamp = sl.tryOptimisticRead();          // không chặn writer
        double cx = x, cy = y;                        // đọc vào biến local
        if (!sl.validate(stamp)) {                    // có writer chen vào → fallback read lock
            stamp = sl.readLock();
            try { cx = x; cy = y; } finally { sl.unlockRead(stamp); }
        }
        return Math.hypot(cx, cy);
    }
}
```
Lưu ý: `StampedLock` **không reentrant** (gọi lồng → tự deadlock), **không hỗ trợ `Condition`**, khi optimistic read các giá trị đọc được có thể không nhất quán → chỉ dùng chúng **sau** khi validate thành công, và không được dereference object có thể ở trạng thái nửa vời trước validate.

> 💡 **Góc nhìn Senior:** thứ tự ưu tiên khi chọn: (1) không chia sẻ state (immutable, confinement); (2) dùng cấu trúc có sẵn (`ConcurrentHashMap`, `BlockingQueue`, atomics); (3) `synchronized`; (4) `ReentrantLock` khi cần tính năng nâng cao (timeout, interruptible, nhiều condition, tránh pin virtual thread); (5) `ReadWriteLock`/`StampedLock` chỉ sau khi **đo** thấy contention đọc là bottleneck.

> ⚠️ **Lỗi thường gặp:** `lock.lock()` đặt **bên trong** `try` (nếu `lock()` ném exception, `finally` sẽ `unlock` một lock chưa giữ → `IllegalMonitorStateException`) — đặt `lock()` ngay trước `try`; quên `unlock` ở một nhánh return; dùng `if` thay `while` quanh `await()`; dùng `signal` khi nhiều loại điều kiện cùng chờ một `Condition`.

### 🛠 Bài tập phần 5

**Bài 5.1 — Ping-pong (Cơ bản)**
- Đề bài: Hai thread in luân phiên "ping" / "pong" 10 lần mỗi thread. Làm 2 phiên bản: `wait/notify` và `ReentrantLock + Condition`.
- Tiêu chí đạt: output luôn đúng thứ tự; không dùng `sleep` để điều phối.

**Bài 5.2 — Lock có timeout (Trung bình)**
- Đề bài: Viết `ResourcePool<T>` (pool kết nối giả) với `T acquire(Duration timeout)` ném `TimeoutException` nếu hết thời gian, `release(T)`. Dùng `ReentrantLock` + `Condition`. Hỗ trợ interrupt.
- Tiêu chí đạt: test 50 thread tranh 5 tài nguyên; đo thời gian chờ tối đa; thread bị interrupt khi đang chờ nhận `InterruptedException` và không làm mất tài nguyên.

**Bài 5.3 — Benchmark lock (Nâng cao)**
- Đề bài: JMH benchmark cache đọc/ghi với tỉ lệ đọc:ghi 50:50, 90:10, 99:1, dùng `synchronized`, `ReentrantLock`, `ReentrantReadWriteLock`, `StampedLock` (optimistic), `ConcurrentHashMap`; 1, 4, 16 thread.
- Tiêu chí đạt: bảng kết quả + kết luận khi nào RW lock thắng và khi nào thua; dùng `@Group`/`@GroupThreads` của JMH để mô phỏng reader/writer.

<details>
<summary>Gợi ý lời giải</summary>

5.1:
```java
ReentrantLock lock = new ReentrantLock(); Condition turn = lock.newCondition();
boolean[] pingTurn = {true};
Runnable ping = () -> { for (int i = 0; i < 10; i++) { lock.lock(); try {
        while (!pingTurn[0]) turn.awaitUninterruptibly();
        System.out.println("ping"); pingTurn[0] = false; turn.signalAll();
    } finally { lock.unlock(); } } };
// pong đối xứng
```
5.2: dùng `Deque<T> available`; `acquire`: `lock.lockInterruptibly(); try { while (available.isEmpty()) { if (nanos <= 0) throw new TimeoutException(); nanos = notEmpty.awaitNanos(nanos);} return available.pop(); } finally { lock.unlock(); }`. Cách gọn hơn: `Semaphore` + `ConcurrentLinkedDeque` (phần 11).
</details>

---

<a id="p6"></a>
## 6. Atomics, CAS, ABA, LongAdder

### 6.1 CAS — Compare-And-Swap

CAS là lệnh CPU nguyên tử (`lock cmpxchg` trên x86, LL/SC hoặc `cas` trên ARM): "nếu giá trị hiện tại == expected thì ghi new, trả về thành công/thất bại". Đây là nền tảng của **lock-free** algorithm: không thread nào bị block; thread thua thì **thử lại**.

```java
import java.util.concurrent.atomic.*;

public class CasDemo {
    private final AtomicLong max = new AtomicLong(Long.MIN_VALUE);

    // CAS loop thủ công
    public void updateMax(long candidate) {
        long cur;
        do {
            cur = max.get();
            if (candidate <= cur) return;
        } while (!max.compareAndSet(cur, candidate));
    }

    // Tương đương, gọn hơn (Java 8): hàm phải side-effect free vì có thể bị gọi lại nhiều lần
    public void updateMax2(long candidate) { max.accumulateAndGet(candidate, Math::max); }

    public static void main(String[] args) {
        AtomicInteger ai = new AtomicInteger();
        ai.incrementAndGet();             // ++i
        ai.getAndAdd(5);                  // i += 5, trả giá trị cũ
        ai.updateAndGet(x -> x * 2);
        AtomicReference<String> ref = new AtomicReference<>("a");
        ref.compareAndSet("a", "b");      // so sánh bằng ==, KHÔNG phải equals!
        System.out.println(ai + " " + ref);
    }
}
```

Họ atomics: `AtomicInteger/Long/Boolean/Reference`, `AtomicIntegerArray/LongArray/ReferenceArray`, `AtomicXxxFieldUpdater` (tiết kiệm bộ nhớ: CAS trên field `volatile` có sẵn thay vì bọc thêm object — dùng nhiều trong Netty, JDK), `LongAdder/DoubleAdder/LongAccumulator`. Từ Java 9, `VarHandle` là API cấp thấp thay thế `sun.misc.Unsafe` cho CAS và memory ordering.

### 6.2 Vấn đề ABA

Thread 1 đọc A, bị tạm dừng; thread 2 đổi A → B → A; thread 1 CAS(A → C) **thành công** dù trạng thái đã thay đổi. Gây lỗi trong cấu trúc lock-free dùng con trỏ (stack lock-free: node bị pop, giải phóng/tái sử dụng rồi push lại).

Trong Java, nhờ GC (node không bị tái sử dụng khi còn tham chiếu), ABA ít nghiêm trọng hơn C/C++, nhưng vẫn xảy ra khi **giá trị** quay vòng (ví dụ số dư tài khoản, version reuse, object pool). Giải pháp: `AtomicStampedReference` (kèm số version `int`) hoặc `AtomicMarkableReference` (kèm cờ boolean).

```java
AtomicStampedReference<Integer> balance = new AtomicStampedReference<>(100, 0);
int[] stampHolder = new int[1];
Integer cur = balance.get(stampHolder);
boolean ok = balance.compareAndSet(cur, cur - 30, stampHolder[0], stampHolder[0] + 1);
```

### 6.3 `LongAdder` — giảm contention

Dưới tranh chấp cao, `AtomicLong.incrementAndGet` bị nhiều thread CAS thất bại liên tục trên **cùng một cache line** → hiệu năng sụp đổ. `LongAdder` (Java 8) chia thành một `base` + mảng `Cell` (mỗi cell được `@Contended` pad tránh **false sharing**); mỗi thread cộng vào cell của mình (chọn theo probe hash), `sum()` cộng tất cả.

- Ghi nhanh hơn nhiều dưới contention; tốn bộ nhớ hơn.
- `sum()` **không phải snapshot nguyên tử** — nếu có ghi đồng thời, kết quả là xấp xỉ "tại một thời điểm nào đó". Không dùng cho logic cần giá trị chính xác tức thời (ví dụ cấp phát ID duy nhất, kiểm tra quota chặt).
- Phù hợp: metrics, counter thống kê. `ConcurrentHashMap<K, LongAdder>` + `computeIfAbsent(k, x -> new LongAdder()).increment()` là mẫu đếm tần suất kinh điển.

**False sharing**: hai biến độc lập nằm cùng cache line (64 byte) được hai core ghi liên tục → cache line "bóng bàn" giữa các core. `@jdk.internal.vm.annotation.Contended` (cần `-XX:-RestrictContended` cho code ngoài JDK) hoặc padding thủ công.

> 💡 **Góc nhìn Senior:** lock-free không có nghĩa là nhanh hơn trong mọi trường hợp: dưới contention rất cao, CAS loop đốt CPU (spin) trong khi lock sẽ cho thread ngủ. Lock-free có lợi thế lớn về **progress guarantee** (không bị kẹt khi thread giữ lock bị preempt/GC pause) và **latency đuôi**. Khi phỏng vấn hỏi "AtomicLong vs LongAdder", trả lời kèm trade-off: tốc độ ghi vs độ chính xác của đọc vs bộ nhớ.

> ⚠️ **Lỗi thường gặp:** `atomic.get()` rồi `atomic.set(x + 1)` (mất tính nguyên tử); `AtomicReference<Integer>.compareAndSet(1000, ...)` so sánh **reference** của Integer ngoài cache → luôn thất bại; dùng hàm có side-effect trong `updateAndGet` (bị gọi lại khi retry); nhiều atomic riêng lẻ cho một invariant liên quan nhiều biến (ví dụ `lower <= upper`) → vẫn race; cần gom vào một object immutable trong `AtomicReference` hoặc dùng lock.

### 🛠 Bài tập phần 6

**Bài 6.1 — ID generator (Cơ bản)**
- Đề bài: Viết `SequenceGenerator` thread-safe trả về ID tăng dần duy nhất, wrap về 0 khi tới `Integer.MAX_VALUE` (không bao giờ trả số âm).
- Tiêu chí đạt: test 32 thread × 100.000 ID không trùng (trước khi wrap); dùng CAS loop hoặc `updateAndGet`.

**Bài 6.2 — Lock-free stack (Trung bình)**
- Đề bài: Implement Treiber stack (`push`, `pop`) bằng `AtomicReference<Node<T>>`. Giải thích vì sao trong Java không bị ABA kiểu "giải phóng bộ nhớ", và tạo một kịch bản ABA nếu bạn tái sử dụng node (object pool).
- Tiêu chí đạt: test đa luồng: tổng số phần tử push = pop + còn lại; sửa kịch bản ABA bằng `AtomicStampedReference`.

**Bài 6.3 — Counter benchmark & false sharing (Nâng cao)**
- Đề bài: JMH benchmark tăng counter với `synchronized`, `AtomicLong`, `LongAdder` ở 1/2/4/8/16 thread. Benchmark thêm hai `volatile long` cạnh nhau bị hai thread ghi riêng, so sánh với phiên bản có padding.
- Tiêu chí đạt: biểu đồ throughput theo số thread; giải thích false sharing bằng kích thước cache line; nêu cách kiểm tra layout object bằng JOL (Java Object Layout).

<details>
<summary>Gợi ý lời giải</summary>

```java
// 6.1
private final AtomicInteger seq = new AtomicInteger();
int next() { return seq.getAndUpdate(x -> x == Integer.MAX_VALUE ? 0 : x + 1); }

// 6.2
class TreiberStack<T> {
    private record Node<T>(T value, Node<T> next) {}
    private final AtomicReference<Node<T>> top = new AtomicReference<>();
    void push(T v) { Node<T> old, n; do { old = top.get(); n = new Node<>(v, old); } while (!top.compareAndSet(old, n)); }
    T pop() { Node<T> old; do { old = top.get(); if (old == null) return null; } while (!top.compareAndSet(old, old.next())); return old.value(); }
}
```
6.3: padding thủ công: `long p1..p7; volatile long value; long q1..q7;` (JVM có thể sắp xếp lại field — kiểm tra bằng JOL; kế thừa class để ép thứ tự).
</details>

---

<a id="p7"></a>
## 7. Deadlock, livelock, starvation — phát hiện và phòng tránh

### 7.1 Định nghĩa

- **Deadlock**: tập thread chờ nhau vĩnh viễn. Cần đủ **4 điều kiện Coffman**: mutual exclusion, hold-and-wait, no preemption, circular wait. Phá **một** điều kiện là đủ phòng tránh.
- **Livelock**: thread không bị block nhưng liên tục phản ứng với nhau và không tiến triển (hai người nhường đường nhau mãi; retry cùng nhịp).
- **Starvation**: một thread không bao giờ có được tài nguyên (lock không fair + thread khác giữ liên tục; priority thấp; reader liên tục làm writer chờ mãi).
- **Thread pool deadlock (starvation deadlock)**: task trong pool chờ kết quả của task khác **cùng pool** mà pool đã hết thread → không ai chạy được. Rất phổ biến trên production, đặc biệt với pool kích thước nhỏ hoặc `CompletableFuture.join()` lồng nhau.

```java
import java.util.concurrent.*;

public class PoolDeadlock {
    public static void main(String[] args) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(1);
        Future<String> outer = pool.submit(() -> {
            Future<String> inner = pool.submit(() -> "inner");  // xếp hàng, nhưng thread duy nhất đang bận...
            return inner.get();                                  // ...chờ chính nó → treo vĩnh viễn
        });
        System.out.println(outer.get(2, TimeUnit.SECONDS));      // TimeoutException
    }
}
```

### 7.2 Deadlock kinh điển và cách sửa

```java
public class Transfer {
    record Account(long id, long[] balance) {}

    // ❌ A→B và B→A cùng lúc → deadlock
    static void transferBad(Account from, Account to, long amt) {
        synchronized (from) { synchronized (to) { from.balance()[0] -= amt; to.balance()[0] += amt; } }
    }

    // ✅ Cách 1: Lock ordering toàn cục (phá circular wait)
    static void transfer(Account from, Account to, long amt) {
        Account first = from.id() < to.id() ? from : to;
        Account second = first == from ? to : from;
        synchronized (first) { synchronized (second) {
            from.balance()[0] -= amt; to.balance()[0] += amt;
        } }
    }
}
// Nếu không có id duy nhất: dùng System.identityHashCode + một "tie-breaking lock" khi bằng nhau (JCIP §10.1.2).
```

Các chiến lược phòng tránh:
1. **Lock ordering** toàn cục.
2. **`tryLock` với timeout** + backoff ngẫu nhiên (phá hold-and-wait; ngẫu nhiên để tránh livelock).
3. **Giảm phạm vi lock**: open call — không gọi method bên ngoài khi đang giữ lock.
4. **Một lock duy nhất** cho tổ hợp (coarse-grained) nếu throughput chấp nhận được.
5. Thiết kế **không chia sẻ** (actor, message passing, single-writer).
6. Với DB: deadlock giữa transaction cũng tuân theo cùng logic — cập nhật các bản ghi theo thứ tự cố định (ví dụ sort theo primary key).

### 7.3 Phát hiện: thread dump

Công cụ lấy thread dump:
- `jstack -l <pid>` (thêm thông tin `java.util.concurrent` locks), `jcmd <pid> Thread.print -l`
- `kill -3 <pid>` (SIGQUIT — dump ra stdout của JVM)
- Java 21: `jcmd <pid> Thread.dump_to_file -format=json <file>` (bao gồm virtual threads)
- Lập trình: `ThreadMXBean.findDeadlockedThreads()` (phát hiện cả monitor lẫn `ownable synchronizers`) — có thể chạy định kỳ trong health check.
- GUI: VisualVM, JDK Mission Control; online: fastthread.io.

Ví dụ đoạn dump:
```
Found one Java-level deadlock:
=============================
"T-1":
  waiting to lock monitor 0x00007f...(object 0x000000071a0b2c10, a Transfer$Account),
  which is held by "T-2"
"T-2":
  waiting to lock monitor 0x00007f...(object 0x000000071a0b2c00, a Transfer$Account),
  which is held by "T-1"

"T-1" #21 prio=5 os_prio=0 tid=... nid=... waiting for monitor entry
   java.lang.Thread.State: BLOCKED (on object monitor)
        at Transfer.transferBad(Transfer.java:7)
        - waiting to lock <0x000000071a0b2c10> (a Transfer$Account)
        - locked <0x000000071a0b2c00> (a Transfer$Account)
```

Kỹ năng đọc thread dump (chuẩn Senior):
- Lấy **3–5 dump cách nhau 5–10 giây**; thread nào kẹt ở **cùng stack** qua các dump là đáng nghi.
- Nhóm thread theo stack trace (nhiều thread cùng chờ `HikariPool.getConnection` → connection pool cạn; nhiều thread `RUNNABLE` trong `SocketInputStream.read` → downstream chậm/không có timeout).
- `BLOCKED` nhiều trên cùng một lock → hot lock.
- Kết hợp `top -H -p <pid>` (thread dùng CPU cao, `nid` = thread id ở dạng hex) để tìm thread đốt CPU (vòng lặp vô hạn, livelock, spin CAS).

> 💡 **Góc nhìn Senior:** trên production, deadlock "thuần" giữa hai `synchronized` ít gặp hơn **resource deadlock** (pool starvation, connection pool cạn do transaction lồng, `CompletableFuture.join` trong commonPool). Câu chuyện thực tế hay được hỏi: "service treo, CPU thấp, không lỗi" → nghĩ ngay đến deadlock/starvation → lấy thread dump. "Service treo, CPU 100%" → livelock, vòng lặp vô hạn, GC thrashing (kiểm tra GC log).

> ⚠️ **Lỗi thường gặp:** gọi callback/listener bên trong khối synchronized; `Future.get()` không timeout trong task chạy trên cùng pool; nested transaction `REQUIRES_NEW` trên connection pool nhỏ (mỗi request cần 2 connection → pool 10 connection với 10 request đồng thời = deadlock); thứ tự lock khác nhau giữa các method của cùng class.

### 🛠 Bài tập phần 7

**Bài 7.1 — Tạo & bắt deadlock (Cơ bản)**
- Đề bài: Tạo deadlock bằng `transferBad` với 2 thread. Lấy thread dump bằng `jstack` và `jcmd`, chỉ ra dòng "Found one Java-level deadlock". Lặp lại với `ReentrantLock` và quan sát dump khác gì (thêm `-l`).
- Tiêu chí đạt: chụp 2 dump và chú thích.

**Bài 7.2 — Deadlock watchdog (Trung bình)**
- Đề bài: Viết `DeadlockDetector` chạy mỗi 10 giây bằng `ScheduledExecutorService`, dùng `ThreadMXBean.findDeadlockedThreads()`, log đầy đủ stack trace + lock info của các thread liên quan, và expose qua một health endpoint đơn giản (`com.sun.net.httpserver`).
- Tiêu chí đạt: phát hiện được deadlock của cả `synchronized` lẫn `ReentrantLock`; không tự "giải" deadlock (giải thích vì sao không thể an toàn).

**Bài 7.3 — Livelock & tryLock (Nâng cao)**
- Đề bài: Viết `transfer` dùng `tryLock()` không backoff, tạo kịch bản livelock (hai thread liên tục lấy lock thứ nhất, thất bại lock thứ hai, nhả, thử lại). Đo số lần retry. Sửa bằng `tryLock(timeout)` + random backoff, và bằng lock ordering. So sánh throughput.
- Tiêu chí đạt: số liệu retry trước/sau; giải thích vì sao random backoff phá livelock (tương tự Ethernet CSMA/CD).

<details>
<summary>Gợi ý lời giải</summary>

7.2:
```java
ThreadMXBean mx = ManagementFactory.getThreadMXBean();
long[] ids = mx.findDeadlockedThreads();
if (ids != null) for (ThreadInfo ti : mx.getThreadInfo(ids, true, true)) log.error(ti.toString());
// Lưu ý ThreadInfo.toString() chỉ in tối đa 8 frame — tự duyệt ti.getStackTrace() để in đủ.
```
Không thể "giải" an toàn vì không có cách dừng thread mà không làm hỏng invariant → cách xử lý là cảnh báo + restart instance (Kubernetes liveness probe).

7.3:
```java
while (true) {
    if (a.lock.tryLock(50, MILLISECONDS)) try {
        if (b.lock.tryLock(50, MILLISECONDS)) try { /* chuyển */ return; } finally { b.lock.unlock(); }
    } finally { a.lock.unlock(); }
    Thread.sleep(ThreadLocalRandom.current().nextInt(1, 10));
}
```
</details>

---

<a id="p8"></a>
## 8. ExecutorService & ThreadPoolExecutor

### 8.1 Vì sao cần thread pool

Tạo platform thread tốn kém (syscall, ~1MB stack reserve, đăng ký với GC/safepoint). Thread không giới hạn → hết bộ nhớ native (`OutOfMemoryError: unable to create native thread`), context switch quá nhiều. Pool giúp: tái sử dụng thread, **giới hạn concurrency**, quản lý vòng đời và hàng đợi.

### 8.2 Tham số `ThreadPoolExecutor`

```java
new ThreadPoolExecutor(
    int corePoolSize,                 // số thread "thường trực"
    int maximumPoolSize,              // trần số thread
    long keepAliveTime, TimeUnit unit,// thread vượt core rảnh quá lâu thì bị huỷ
    BlockingQueue<Runnable> workQueue,
    ThreadFactory threadFactory,      // đặt tên, daemon, UncaughtExceptionHandler
    RejectedExecutionHandler handler) // xử lý khi quá tải
```

**Thuật toán khi `execute(task)`** (đây là điểm phỏng vấn hay hỏi và nhiều người trả lời sai):
1. Nếu số thread < `corePoolSize` → **tạo thread mới** (kể cả khi có thread core đang rảnh).
2. Ngược lại → **đưa vào queue**.
3. Nếu queue **đầy** và số thread < `maximumPoolSize` → tạo thread mới (non-core).
4. Nếu queue đầy và đã đạt max → **reject** (gọi `RejectedExecutionHandler`).

Hệ quả: với **queue không giới hạn** (`LinkedBlockingQueue()` không tham số), bước 3 không bao giờ xảy ra → `maximumPoolSize` **vô nghĩa**, queue phình vô hạn. Pool chỉ "nở" khi queue đầy — ngược trực giác với nhiều người tưởng pool sẽ tăng thread trước rồi mới xếp hàng.

### 8.3 Các factory của `Executors` và vì sao nguy hiểm

| Factory | Cấu hình thật | Rủi ro |
|---|---|---|
| `newFixedThreadPool(n)` | core = max = n, `LinkedBlockingQueue` **không giới hạn** | task đến nhanh hơn xử lý → queue phình → **OOM**, latency tăng vô hạn, không có backpressure |
| `newSingleThreadExecutor()` | 1 thread, queue không giới hạn | như trên |
| `newCachedThreadPool()` | core 0, max `Integer.MAX_VALUE`, `SynchronousQueue`, keepAlive 60s | burst → tạo **hàng nghìn thread** → OOM native thread, CPU thrashing |
| `newScheduledThreadPool(n)` | `DelayedWorkQueue` không giới hạn; max không có tác dụng | queue phình; task ném exception sẽ **lặng lẽ dừng lặp lại** |
| `newWorkStealingPool()` | `ForkJoinPool` | không phù hợp task blocking |
| `newVirtualThreadPerTaskExecutor()` (21) | mỗi task một virtual thread | không giới hạn concurrency → phải tự giới hạn tài nguyên downstream |

(Alibaba Java Coding Guidelines cấm dùng `Executors` factory vì lý do này — một câu hỏi phỏng vấn phổ biến ở châu Á.)

### 8.4 Cấu hình đúng

```java
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class PoolConfig {
    public static ThreadPoolExecutor ioPool(String name, int core, int max, int queueCap) {
        ThreadFactory tf = new ThreadFactory() {
            private final AtomicInteger seq = new AtomicInteger();
            @Override public Thread newThread(Runnable r) {
                Thread t = new Thread(r, name + "-" + seq.incrementAndGet());
                t.setDaemon(false);
                t.setUncaughtExceptionHandler((th, ex) -> System.err.println(th.getName() + " died: " + ex));
                return t;
            }
        };
        ThreadPoolExecutor ex = new ThreadPoolExecutor(
            core, max, 60, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(queueCap),            // bounded → có backpressure
            tf,
            new ThreadPoolExecutor.CallerRunsPolicy());    // quá tải → thread gửi tự chạy → làm chậm producer
        ex.allowCoreThreadTimeOut(false);
        ex.prestartAllCoreThreads();                       // tránh latency lúc khởi động
        return ex;
    }

    public static void main(String[] args) throws InterruptedException {
        ThreadPoolExecutor pool = ioPool("report", 4, 16, 100);
        for (int i = 0; i < 300; i++) {
            int id = i;
            pool.execute(() -> { try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } });
        }
        System.out.printf("active=%d pool=%d queue=%d completed=%d%n",
            pool.getActiveCount(), pool.getPoolSize(), pool.getQueue().size(), pool.getCompletedTaskCount());
        pool.shutdown();
        if (!pool.awaitTermination(30, TimeUnit.SECONDS)) pool.shutdownNow();
    }
}
```

**Rejection policies:**

| Policy | Hành vi | Khi nào dùng |
|---|---|---|
| `AbortPolicy` (mặc định) | ném `RejectedExecutionException` | caller xử lý lỗi (trả 503, retry sau) |
| `CallerRunsPolicy` | thread gọi `execute` tự chạy task | backpressure tự nhiên; **cẩn thận** nếu caller là event loop/thread request (làm chậm chính nó) |
| `DiscardPolicy` | bỏ im lặng | gần như không bao giờ (mất dữ liệu không dấu vết) |
| `DiscardOldestPolicy` | bỏ task cũ nhất trong queue rồi thử lại | dữ liệu "mới nhất thắng" (cập nhật giá, telemetry) |
| Custom | log + metric + fallback (ghi DB/Kafka) | production |

**Chọn queue:** `ArrayBlockingQueue` (bounded, một lock), `LinkedBlockingQueue(cap)` (bounded, hai lock put/take — throughput tốt hơn), `SynchronousQueue` (không lưu, handoff trực tiếp — buộc tạo thread hoặc reject), `PriorityBlockingQueue` (unbounded, theo ưu tiên — cẩn thận OOM).

### 8.5 Sizing — bao nhiêu thread là đủ?

- **CPU-bound**: `N_threads ≈ N_cpu + 1` (thêm 1 bù cho page fault/pause hiếm hoi). Nhiều hơn chỉ tăng context switch.
- **I/O-bound** (công thức JCIP §8.2): `N_threads = N_cpu × U_cpu × (1 + W/C)`, với `U_cpu` là mức sử dụng CPU mục tiêu (0..1), `W/C` là tỉ lệ thời gian chờ / thời gian tính. Ví dụ 8 core, mỗi request 10ms CPU + 90ms chờ DB, mục tiêu 80% CPU → `8 × 0.8 × (1 + 9) = 64`.
- **Little's Law**: `L = λ × W` — số request đồng thời = throughput × latency. 500 req/s × 0.2s = 100 thread cần thiết.
- **Ràng buộc tài nguyên downstream**: pool 200 thread gọi DB với connection pool 20 → 180 thread chỉ đứng chờ connection. Kích thước pool phải hài hoà với connection pool, rate limit của API đối tác.
- Cuối cùng: **đo** (load test) và **giám sát** (`activeCount`, `queue.size`, `completedTaskCount`, thời gian chờ trong queue). Micrometer có `ExecutorServiceMetrics`.

### 8.6 Exception và vòng đời

- `execute(Runnable)`: exception → thread chết, gọi `UncaughtExceptionHandler`, pool tạo thread thay thế.
- `submit(...)`: exception bị **bọc trong `Future`** — nếu không ai gọi `get()`, exception **biến mất không dấu vết**. Đây là bug rất phổ biến. Bắt exception bên trong task, hoặc override `afterExecute`.
- `ScheduledExecutorService.scheduleAtFixedRate`: nếu một lần chạy ném exception, **các lần sau bị huỷ** im lặng → luôn bọc `try/catch(Throwable)` trong task định kỳ.
- `shutdown()`: không nhận task mới, chạy hết task đang có. `shutdownNow()`: interrupt thread đang chạy, trả về task chưa chạy. `awaitTermination` để chờ. `close()` (Java 19+) = shutdown + await.
- Pool không shutdown → non-daemon thread giữ JVM sống; trong ứng dụng web, pool tạo theo request mà không shutdown → leak thread.

> 💡 **Góc nhìn Senior:**
> - **Bulkhead pattern**: tách pool riêng cho từng loại downstream (pool gọi payment, pool gọi email) để một downstream chậm không kéo sập cả service.
> - Thời gian chờ trong queue là một phần của latency mà người dùng cảm nhận; queue càng dài, latency càng cao — có khi thà reject sớm (fail fast) còn hơn xử lý một request mà client đã timeout từ lâu.
> - Spring `@Async` mặc định (Boot) dùng `ThreadPoolTaskExecutor` với queue capacity `Integer.MAX_VALUE` — cùng bẫy với `newFixedThreadPool`; cấu hình `spring.task.execution.pool.*`.

> ⚠️ **Lỗi thường gặp:** tạo `Executors.newFixedThreadPool` bên trong method xử lý request và không shutdown; dùng `submit` rồi bỏ quên `Future`; đặt `corePoolSize = 0` với `LinkedBlockingQueue` không giới hạn (chỉ có tối đa 1 thread được tạo!); tin rằng `maximumPoolSize` có tác dụng với unbounded queue.

### 🛠 Bài tập phần 8

**Bài 8.1 — Mô phỏng thuật toán TPE (Cơ bản)**
- Đề bài: Tạo `ThreadPoolExecutor(2, 4, 10s, ArrayBlockingQueue(2))`, submit 10 task mỗi task sleep 1s. In `poolSize`, `queue.size`, và task nào bị reject theo thời gian. Dự đoán trước khi chạy.
- Tiêu chí đạt: giải thích đúng: task 1–2 tạo core thread, 3–4 vào queue, 5–6 tạo thread non-core, 7–10 bị reject.

**Bài 8.2 — Pool có giám sát (Trung bình)**
- Đề bài: Subclass `ThreadPoolExecutor`, override `beforeExecute`/`afterExecute` để đo thời gian chờ trong queue và thời gian thực thi của mỗi task, log exception (kể cả task gửi bằng `submit` — gợi ý: kiểm tra `Future` trong `afterExecute`), expose metrics (p50/p99 queue wait, active, queue size, rejected count).
- Tiêu chí đạt: exception trong task `submit` được log dù không ai gọi `get()`; có test.

**Bài 8.3 — Sizing bằng thực nghiệm (Nâng cao)**
- Đề bài: Giả lập service: mỗi request tốn 5ms CPU (vòng lặp tính toán) + 45ms "DB" (sleep), DB giới hạn 20 kết nối (Semaphore). Chạy load generator 400 req/s. Thử pool 8, 16, 32, 64, 128 thread; đo throughput, p99 latency, CPU.
- Tiêu chí đạt: so sánh kết quả với công thức JCIP và Little's Law; giải thích vì sao vượt quá 20 + một ít thread không cải thiện (bottleneck là connection); đề xuất cấu hình.

<details>
<summary>Gợi ý lời giải</summary>

8.2:
```java
class MonitoredPool extends ThreadPoolExecutor {
    private final ThreadLocal<Long> start = new ThreadLocal<>();
    @Override protected void beforeExecute(Thread t, Runnable r) { start.set(System.nanoTime()); }
    @Override protected void afterExecute(Runnable r, Throwable t) {
        try {
            long dur = System.nanoTime() - start.get();
            if (t == null && r instanceof Future<?> f && f.isDone()) {
                try { f.get(); }
                catch (ExecutionException ee) { t = ee.getCause(); }
                catch (CancellationException ce) { /* bỏ qua */ }
                catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
            if (t != null) log.error("Task failed", t);
        } finally { start.remove(); }
    }
}
```
Thời gian chờ queue: bọc task trong wrapper lưu `enqueuedAt` khi `execute`.

8.3: CPU 8 core: CPU-time/req = 5ms → tối đa ~1600 req/s về CPU; DB: 20 conn / 45ms ≈ 444 req/s → bottleneck DB. Little: 400 × 0.05 = 20 request đồng thời → pool ~24–32 là đủ; thêm thread chỉ tăng chờ semaphore.
</details>

---

<a id="p9"></a>
## 9. Future & CompletableFuture

### 9.1 `Future` và giới hạn

`Future<V>`: `get()` (block), `get(timeout)`, `cancel(mayInterrupt)`, `isDone`, `isCancelled`; Java 19 thêm `resultNow()`, `exceptionNow()`, `state()`. Giới hạn: không compose được, không có callback, phải block để lấy kết quả.

### 9.2 `CompletableFuture` (Java 8)

Vừa là `Future` vừa là `CompletionStage` — cho phép xây dựng **pipeline bất đồng bộ** không block.

```java
import java.util.*;
import java.util.concurrent.*;

public class CfDemo {
    static final ExecutorService IO = Executors.newFixedThreadPool(16, r -> {
        Thread t = new Thread(r, "io-" + UUID.randomUUID().toString().substring(0, 4)); t.setDaemon(true); return t;
    });

    record User(long id, String name) {}
    record Order(long id, double total) {}
    record Dashboard(User user, List<Order> orders, double rate) {}

    static User fetchUser(long id)            { sleep(100); return new User(id, "An"); }
    static List<Order> fetchOrders(long uid)  { sleep(150); return List.of(new Order(1, 99.5)); }
    static double fetchRate()                 { sleep(80);  return 25_000; }

    public static void main(String[] args) {
        long t0 = System.currentTimeMillis();
        CompletableFuture<User> userF = CompletableFuture.supplyAsync(() -> fetchUser(42), IO);

        CompletableFuture<List<Order>> ordersF =
            userF.thenComposeAsync(u -> CompletableFuture.supplyAsync(() -> fetchOrders(u.id()), IO), IO); // phụ thuộc user

        CompletableFuture<Double> rateF = CompletableFuture.supplyAsync(CfDemo::fetchRate, IO)           // độc lập → song song
            .completeOnTimeout(24_000.0, 50, TimeUnit.MILLISECONDS);                                      // Java 9: fallback khi chậm

        CompletableFuture<Dashboard> dash = userF
            .thenCombine(ordersF, (u, os) -> new Object[]{u, os})
            .thenCombine(rateF, (arr, r) -> new Dashboard((User) arr[0], (List<Order>) arr[1], r))
            .orTimeout(1, TimeUnit.SECONDS)                                                               // Java 9
            .exceptionally(ex -> { System.err.println("fallback: " + ex); return null; });

        System.out.println(dash.join() + " in " + (System.currentTimeMillis() - t0) + "ms"); // ~250ms, không phải 330ms
    }
    static void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new CompletionException(e); } }
}
```

### 9.3 Bảng method cần nắm

| Nhóm | Method | Ghi chú |
|---|---|---|
| Khởi tạo | `supplyAsync(Supplier, [Executor])`, `runAsync`, `completedFuture`, `failedFuture` (9) | **Không truyền executor → `ForkJoinPool.commonPool()`** |
| Biến đổi | `thenApply` (map), `thenAccept`, `thenRun` | |
| Nối chuỗi | `thenCompose` (flatMap) | hàm trả về `CompletionStage` — tránh `CF<CF<T>>` |
| Kết hợp | `thenCombine`, `thenAcceptBoth`, `applyToEither`, `allOf`, `anyOf` | `allOf` trả `CF<Void>` — phải tự `join` từng cái |
| Lỗi | `exceptionally` (recover), `handle` (cả kết quả + lỗi → giá trị mới), `whenComplete` (side-effect, **không** đổi kết quả), `exceptionallyCompose` (12) | |
| Thời gian | `orTimeout`, `completeOnTimeout` (9), `CompletableFuture.delayedExecutor` (9) | |
| Hoàn thành tay | `complete`, `completeExceptionally`, `obtrudeValue` | bắc cầu callback API cũ |
| Lấy kết quả | `join()` (ném unchecked `CompletionException`), `get()` (checked `ExecutionException`), `getNow` | |

**Async hay không async?** `thenApply(fn)` chạy `fn` trên **thread hoàn thành stage trước** (hoặc thread gọi `thenApply` nếu stage đã xong). `thenApplyAsync(fn)` gửi `fn` vào executor (mặc định commonPool). Do đó, một `thenApply` nặng có thể chạy trên thread I/O selector của HTTP client hoặc thread của caller — cần chủ động chọn executor.

### 9.4 Exception handling — chi tiết dễ sai

```java
CompletableFuture<Integer> cf = CompletableFuture
    .supplyAsync(() -> { throw new IllegalStateException("boom"); })
    .thenApply(x -> x + 1)                              // bị bỏ qua, lỗi lan truyền
    .handle((v, ex) -> {
        // ex là CompletionException bọc IllegalStateException (khi lỗi đến từ stage phụ thuộc)
        Throwable root = (ex instanceof CompletionException && ex.getCause() != null) ? ex.getCause() : ex;
        System.out.println("root = " + root);
        return ex == null ? v : -1;
    });
System.out.println(cf.join()); // -1
```
- Lỗi lan truyền qua các stage, được bọc trong `CompletionException`; unwrap `getCause()` trước khi xử lý theo kiểu.
- `whenComplete` không "nuốt" lỗi; `exceptionally`/`handle` thì có.
- Một CF thất bại mà không ai `join`/xử lý → lỗi **biến mất** (giống `submit`). Luôn kết thúc pipeline bằng xử lý lỗi hoặc log.
- `cancel(true)` **không interrupt** task đang chạy (tham số `mayInterruptIfRunning` bị bỏ qua) — chỉ hoàn thành CF với `CancellationException`. Task bên dưới vẫn chạy tiếp, tốn tài nguyên. `orTimeout` cũng vậy: CF hoàn thành exceptionally nhưng công việc bên dưới không bị dừng.

### 9.5 Mẫu "fan-out, gom kết quả"

```java
static <T> CompletableFuture<List<T>> allOfList(List<CompletableFuture<T>> cfs) {
    return CompletableFuture.allOf(cfs.toArray(CompletableFuture[]::new))
        .thenApply(v -> cfs.stream().map(CompletableFuture::join).toList()); // join không block vì đã xong
}
```
Nếu một CF lỗi, `allOf` lỗi — nhưng **chỉ sau khi tất cả đã xong**; không fail-fast. Muốn fail-fast cần tự viết (gắn `exceptionally` vào từng CF để hoàn thành sớm CF tổng). Đây là chỗ Structured Concurrency (phần 14) giải quyết gọn gàng.

> 💡 **Góc nhìn Senior:**
> - **Luôn truyền executor riêng** cho tác vụ blocking I/O; `commonPool` chỉ cho tác vụ CPU ngắn. Lưu ý: nếu `commonPool` parallelism < 2 (máy/container 1–2 CPU), `supplyAsync` không executor sẽ tạo **một thread mới cho mỗi task** (`ThreadPerTaskExecutor`) — hành vi khác hẳn giữa máy dev và pod production.
> - Context propagation: MDC (traceId), `SecurityContext`, transaction **không** tự đi theo sang thread khác. Phải wrap executor (Spring `TaskDecorator`, Micrometer Context Propagation).
> - Timeout ở mọi tầng: timeout của CF không dừng HTTP call bên dưới → cấu hình timeout ở chính client.

> ⚠️ **Lỗi thường gặp:** `join()` trong một task đang chạy trên cùng pool (starvation deadlock); dùng `thenApply` với hàm trả về CF (ra `CF<CF<T>>`); xử lý `ex instanceof MyException` mà quên unwrap `CompletionException`; nghĩ `cancel(true)` dừng được công việc.

### 🛠 Bài tập phần 9

**Bài 9.1 — Song song hoá (Cơ bản)**
- Đề bài: Có 3 hàm giả lập I/O (200ms, 300ms, 100ms) độc lập. Viết phiên bản tuần tự và phiên bản CF song song, kết hợp kết quả.
- Tiêu chí đạt: phiên bản CF hoàn thành ~300ms; dùng executor riêng; xử lý lỗi bằng `handle`.

**Bài 9.2 — Price aggregator (Trung bình)**
- Đề bài: Gọi 5 "nhà cung cấp" giá (latency ngẫu nhiên 50–500ms, 20% lỗi). Trả về giá thấp nhất trong các kết quả thành công **trong 300ms**; nhà cung cấp lỗi/chậm bị bỏ qua; nếu không có kết quả nào → giá mặc định.
- Tiêu chí đạt: tổng thời gian ≤ ~320ms; không có exception lọt ra ngoài; log nhà cung cấp lỗi/timeout.

**Bài 9.3 — Retry bất đồng bộ (Nâng cao)**
- Đề bài: Viết `static <T> CompletableFuture<T> retryAsync(Supplier<CompletableFuture<T>> op, int maxAttempts, Duration baseDelay, Predicate<Throwable> retryable, ScheduledExecutorService scheduler)` với exponential backoff, **không block thread nào** trong lúc chờ.
- Tiêu chí đạt: dùng `exceptionallyCompose` hoặc `handle` + `thenCompose`; dùng `delayedExecutor` hoặc scheduler; test số lần gọi; không retry khi `retryable` trả false.

<details>
<summary>Gợi ý lời giải</summary>

9.2:
```java
List<CompletableFuture<Optional<Double>>> cfs = providers.stream()
    .map(p -> CompletableFuture.supplyAsync(p::quote, IO)
        .thenApply(Optional::of)
        .completeOnTimeout(Optional.empty(), 300, MILLISECONDS)
        .exceptionally(ex -> { log.warn("{} failed: {}", p, ex.getMessage()); return Optional.empty(); }))
    .toList();
double best = allOfList(cfs).join().stream().flatMap(Optional::stream)
    .min(Double::compare).orElse(DEFAULT);
```

9.3:
```java
static <T> CompletableFuture<T> retryAsync(Supplier<CompletableFuture<T>> op, int attempt, int max,
                                           Duration base, Predicate<Throwable> retryable) {
    return op.get().exceptionallyCompose(ex -> {
        Throwable root = ex instanceof CompletionException c && c.getCause() != null ? c.getCause() : ex;
        if (attempt + 1 >= max || !retryable.test(root)) return CompletableFuture.failedFuture(root);
        long delay = base.toMillis() << attempt;
        Executor delayed = CompletableFuture.delayedExecutor(delay, TimeUnit.MILLISECONDS);
        return CompletableFuture.supplyAsync(() -> null, delayed)
            .thenCompose(x -> retryAsync(op, attempt + 1, max, base, retryable));
    });
}
```
</details>

---

<a id="p10"></a>
## 10. ForkJoinPool & work stealing

### 10.1 Mô hình

`ForkJoinPool` (Java 7) tối ưu cho **chia để trị (divide and conquer)**: task lớn chia thành task con (`fork`), chờ kết quả (`join`), gộp lại.

**Work stealing**: mỗi worker có một **deque** riêng. Worker push/pop task của mình ở **đầu** (LIFO — tốt cho cache locality, task con vừa fork còn "nóng"); worker rảnh **ăn cắp** task từ **đuôi** deque của worker khác (FIFO — thường là task lớn, đáng ăn cắp). Giảm tranh chấp so với một queue chung.

```java
import java.util.concurrent.*;

public class ParallelSum extends RecursiveTask<Long> {
    private static final int THRESHOLD = 10_000;
    private final long[] arr; private final int lo, hi;
    ParallelSum(long[] arr, int lo, int hi) { this.arr = arr; this.lo = lo; this.hi = hi; }

    @Override protected Long compute() {
        if (hi - lo <= THRESHOLD) {                     // đủ nhỏ → làm tuần tự
            long s = 0; for (int i = lo; i < hi; i++) s += arr[i]; return s;
        }
        int mid = (lo + hi) >>> 1;
        ParallelSum left = new ParallelSum(arr, lo, mid);
        ParallelSum right = new ParallelSum(arr, mid, hi);
        left.fork();                                    // đẩy nửa trái vào deque (có thể bị worker khác ăn cắp)
        long r = right.compute();                       // TỰ làm nửa phải (không fork cả hai!)
        return r + left.join();                         // join: nếu left chưa bị ăn cắp, tự chạy nó
    }

    public static void main(String[] args) {
        long[] data = java.util.stream.LongStream.rangeClosed(1, 50_000_000).toArray();
        try (ForkJoinPool pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors())) { // AutoCloseable từ 19
            System.out.println(pool.invoke(new ParallelSum(data, 0, data.length)));
        }
    }
}
```

Các điểm đáng chú ý:
- **Fork một, compute một** (hoặc `invokeAll(left, right)`) — fork cả hai rồi join cả hai lãng phí một worker chỉ để chờ.
- Ngưỡng (threshold) quá nhỏ → chi phí tạo task lấn át; quá lớn → mất cân bằng tải. Kinh nghiệm: mỗi task lá xử lý ≥ 10.000 "bước tính toán cơ bản" (Javadoc `ForkJoinTask` gợi ý 100–10.000).
- `join()` trong ForkJoinPool **không block worker một cách ngây thơ**: worker sẽ chạy task khác (help-stealing / "helping") trong lúc chờ.
- **Blocking I/O trong FJ task là xấu**: worker bị block không ăn cắp được; pool có thể bù thread qua `ForkJoinPool.managedBlock(ManagedBlocker)`, nhưng code thường không dùng.
- `commonPool` dùng bởi parallel stream, `CompletableFuture` mặc định, `Arrays.parallelSort`. Không có `shutdown` (bị bỏ qua) với commonPool.
- `asyncMode = true` (constructor) dùng FIFO cho task không bao giờ join — phù hợp event-style; virtual thread scheduler mặc định là một `ForkJoinPool` chạy ở async mode.

> 💡 **Góc nhìn Senior:** trong ứng dụng nghiệp vụ, bạn hiếm khi viết `RecursiveTask` trực tiếp — nhưng **luôn** gián tiếp dùng FJP qua parallel stream, CF, virtual thread. Hiểu FJP giúp trả lời: "vì sao parallel stream gọi DB làm chậm CF ở chỗ khác?", "vì sao virtual thread bị pin lại tệ?" (carrier pool là FJP với số thread ≈ số core).

> ⚠️ **Lỗi thường gặp:** gọi `left.join()` trước `right.compute()` (tuần tự hoá); dùng FJP cho task không chia nhỏ được; ném checked exception trong `compute` (phải bọc); task lá sửa shared state không đồng bộ.

### 🛠 Bài tập phần 10

**Bài 10.1 — Merge sort song song (Cơ bản)**
- Đề bài: Viết `RecursiveAction` merge sort mảng `int[]` 20 triệu phần tử. So sánh với `Arrays.sort` và `Arrays.parallelSort`.
- Tiêu chí đạt: kết quả đúng; thử 3 ngưỡng khác nhau; bảng thời gian.

**Bài 10.2 — Duyệt cây thư mục (Trung bình)**
- Đề bài: Dùng `RecursiveTask<Long>` tính tổng kích thước một cây thư mục lớn (mỗi thư mục con là một task). Đây là I/O — so sánh với phiên bản dùng `Files.walk` tuần tự và phiên bản virtual threads.
- Tiêu chí đạt: giải thích vì sao FJP không lý tưởng cho I/O và kết quả đo có khớp không (cache của OS ảnh hưởng: chạy nhiều lần).

**Bài 10.3 — Quan sát work stealing (Nâng cao)**
- Đề bài: Tạo cây task mất cân bằng (nhánh trái nặng gấp 10 nhánh phải). In `pool.getStealCount()`, và log tên worker xử lý mỗi task lá. So sánh với phiên bản dùng `ThreadPoolExecutor` cố định + chia đều trước.
- Tiêu chí đạt: giải thích vì sao work stealing tự cân bằng tải.

<details>
<summary>Gợi ý lời giải</summary>

10.1:
```java
class MergeSort extends RecursiveAction {
    final int[] a, tmp; final int lo, hi;
    protected void compute() {
        if (hi - lo <= THRESHOLD) { Arrays.sort(a, lo, hi); return; }
        int mid = (lo + hi) >>> 1;
        invokeAll(new MergeSort(a, tmp, lo, mid), new MergeSort(a, tmp, mid, hi));
        merge(a, tmp, lo, mid, hi);
    }
}
```
`Arrays.parallelSort` chính là một merge sort trên FJP (với ngưỡng chuyển sang sort tuần tự).
</details>

---

<a id="p11"></a>
## 11. Synchronizers: CountDownLatch, CyclicBarrier, Semaphore, Phaser

| Synchronizer | Ý tưởng | Tái sử dụng | Ví dụ |
|---|---|---|---|
| `CountDownLatch(n)` | chờ đến khi đếm về 0 | **Không** (one-shot) | main chờ N service khởi động; test cho N thread xuất phát cùng lúc |
| `CyclicBarrier(n, action)` | n thread hẹn gặp nhau tại barrier rồi cùng đi tiếp | **Có** | mô phỏng theo bước (mỗi "thế hệ" tính xong mới sang bước sau) |
| `Semaphore(permits)` | giới hạn số thread truy cập tài nguyên đồng thời | có | giới hạn 10 request đồng thời tới API đối tác; pool tài nguyên |
| `Phaser` | barrier nhiều pha, số bên tham gia **thay đổi động** | có | pipeline nhiều giai đoạn với worker tham gia/rời |
| `Exchanger` | hai thread hoán đổi object | có | double buffering |

```java
import java.util.concurrent.*;

public class Synchronizers {
    public static void main(String[] args) throws Exception {
        // CountDownLatch: "cổng xuất phát" + "vạch đích"
        int n = 4;
        CountDownLatch startGate = new CountDownLatch(1), endGate = new CountDownLatch(n);
        for (int i = 0; i < n; i++) {
            int id = i;
            new Thread(() -> {
                try { startGate.await(); System.out.println("runner " + id); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                finally { endGate.countDown(); }            // countDown trong finally!
            }).start();
        }
        startGate.countDown();                              // tất cả xuất phát cùng lúc
        if (!endGate.await(5, TimeUnit.SECONDS)) System.out.println("timeout");

        // CyclicBarrier: 3 worker, mỗi vòng xong thì barrier action chạy 1 lần
        CyclicBarrier barrier = new CyclicBarrier(3, () -> System.out.println("--- hết vòng ---"));
        ExecutorService ex = Executors.newFixedThreadPool(3);
        for (int w = 0; w < 3; w++) {
            ex.submit(() -> {
                for (int round = 0; round < 2; round++) {
                    System.out.println(Thread.currentThread().getName() + " xong vòng " + round);
                    barrier.await();                         // BrokenBarrierException nếu thread khác bị interrupt/timeout
                }
                return null;
            });
        }
        ex.shutdown(); ex.awaitTermination(5, TimeUnit.SECONDS);

        // Semaphore: tối đa 2 "kết nối" đồng thời
        Semaphore sem = new Semaphore(2);
        try (ExecutorService vt = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < 6; i++) {
                int id = i;
                vt.submit(() -> {
                    if (!sem.tryAcquire(1, TimeUnit.SECONDS)) { System.out.println(id + " bị từ chối"); return null; }
                    try { System.out.println(id + " đang gọi API"); Thread.sleep(200); }
                    finally { sem.release(); }               // release trong finally, và CHỈ khi đã acquire
                    return null;
                });
            }
        }

        // Phaser: số bên đăng ký động
        Phaser phaser = new Phaser(1);                      // 1 = main
        for (int i = 0; i < 3; i++) {
            phaser.register();
            int id = i;
            new Thread(() -> {
                System.out.println("task " + id + " phase " + phaser.getPhase());
                phaser.arriveAndAwaitAdvance();              // phase 0 → 1
                System.out.println("task " + id + " phase " + phaser.getPhase());
                phaser.arriveAndDeregister();
            }).start();
        }
        phaser.arriveAndAwaitAdvance();
        phaser.arriveAndDeregister();
    }
}
```

Chi tiết cần biết:
- `Semaphore` **không có khái niệm owner**: thread khác có thể `release` (khác lock); `release` nhiều hơn `acquire` làm **tăng** số permit vượt ban đầu — bug âm thầm.
- `Semaphore(n, true)` fair; `acquire()` không timeout có thể treo vĩnh viễn nếu permit bị rò.
- `CyclicBarrier`: nếu một thread bị interrupt/timeout, barrier chuyển sang trạng thái **broken**, mọi thread đang chờ nhận `BrokenBarrierException`; phải `reset()`.
- `CountDownLatch` không reset được; nếu worker ném exception trước `countDown()` (không đặt trong `finally`), `await()` treo mãi.

> 💡 **Góc nhìn Senior:** với virtual threads, `Semaphore` trở thành công cụ **chính** để giới hạn concurrency tới tài nguyên khan hiếm (thay vì giới hạn bằng kích thước pool). Rate limiting theo thời gian (N request/giây) thì cần token bucket (Guava `RateLimiter`, Resilience4j `RateLimiter`, Bucket4j) — Semaphore chỉ giới hạn **đồng thời**, không giới hạn **tốc độ**.

> ⚠️ **Lỗi thường gặp:** `countDown`/`release` không nằm trong `finally`; `release` trong `finally` dù `tryAcquire` thất bại; dùng `CountDownLatch` cho bài toán lặp nhiều vòng.

### 🛠 Bài tập phần 11

**Bài 11.1 — Khởi động hệ thống (Cơ bản)**
- Đề bài: Mô phỏng ứng dụng cần 4 thành phần (DB, cache, MQ, config) khởi động song song (thời gian ngẫu nhiên); main chờ tất cả xong tối đa 3 giây, nếu có thành phần lỗi thì báo lỗi cụ thể.
- Tiêu chí đạt: dùng `CountDownLatch`; thành phần lỗi vẫn `countDown`; lỗi được thu thập trong `ConcurrentLinkedQueue`.

**Bài 11.2 — Game of Life song song (Trung bình)**
- Đề bài: Chia lưới 1000×1000 thành 4 dải, 4 thread tính song song từng thế hệ, đồng bộ bằng `CyclicBarrier` (barrier action hoán đổi buffer).
- Tiêu chí đạt: kết quả giống bản tuần tự sau 100 thế hệ.

**Bài 11.3 — Bulkhead bằng Semaphore (Nâng cao)**
- Đề bài: Viết `Bulkhead` với `<T> T call(Callable<T> c, Duration maxWait)`: giới hạn N lời gọi đồng thời; chờ tối đa `maxWait`; đếm metrics (accepted, rejected, in-flight max). Dùng với 10.000 virtual thread gọi một API giả lập.
- Tiêu chí đạt: in-flight không bao giờ vượt N (kiểm chứng bằng `AtomicInteger` + `accumulateAndGet(max)`); không rò permit khi `Callable` ném exception.

<details>
<summary>Gợi ý lời giải</summary>

11.3:
```java
public <T> T call(Callable<T> c, Duration maxWait) throws Exception {
    if (!sem.tryAcquire(maxWait.toNanos(), TimeUnit.NANOSECONDS)) { rejected.increment(); throw new BulkheadFullException(); }
    try {
        int now = inFlight.incrementAndGet();
        maxInFlight.accumulateAndGet(now, Math::max);
        return c.call();
    } finally { inFlight.decrementAndGet(); sem.release(); }
}
```
</details>

---

<a id="p12"></a>
## 12. Concurrent collections & producer–consumer

### 12.1 Synchronized wrapper vs concurrent collection

- `Collections.synchronizedMap(map)`/`Vector`/`Hashtable`: một lock cho toàn bộ → mọi thao tác tuần tự hoá; **iteration phải tự `synchronized(map)`** nếu không sẽ `ConcurrentModificationException`; thao tác tổ hợp vẫn không atomic.
- `java.util.concurrent`: thiết kế cho đồng thời — lock chi tiết hoặc lock-free, iterator **weakly consistent** (không ném CME, có thể phản ánh hoặc không các thay đổi sau khi tạo iterator).

### 12.2 `ConcurrentHashMap` (CHM)

**Internals Java 8+** (khác Java 7 dùng `Segment`):
- Mảng `Node[] table`; đọc (`get`) **không lock** — dựa vào field `volatile` và `Unsafe/VarHandle` getVolatile.
- Ghi vào bin rỗng: **CAS**. Ghi vào bin đã có node: `synchronized` trên **node đầu bin** → lock chi tiết theo từng bucket.
- Bin dài > 8 (và table ≥ 64) → chuyển thành **TreeBin** (red-black tree), O(log n) khi hash collision nhiều.
- Resize **hợp tác**: nhiều thread cùng giúp chuyển dữ liệu (`ForwardingNode`).
- `size()` dựa trên cơ chế giống `LongAdder` (`baseCount` + `CounterCell[]`) → **xấp xỉ** khi đang có ghi đồng thời; `mappingCount()` trả `long`.
- **Không cho phép `null`** key hoặc value — vì `get(k) == null` sẽ mơ hồ (không có key hay value là null?) và không thể kiểm tra bằng `containsKey` một cách nguyên tử.

**Dùng đúng các thao tác atomic:**

```java
import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;

public class ChmUsage {
    static final ConcurrentHashMap<String, LongAdder> hits = new ConcurrentHashMap<>();
    static final ConcurrentHashMap<String, Integer> stock = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        // ❌ check-then-act
        // if (!map.containsKey(k)) map.put(k, v);
        // ❌ read-modify-write
        // map.put(k, map.get(k) + 1);

        hits.computeIfAbsent("/home", k -> new LongAdder()).increment();  // ✅
        stock.merge("SKU-1", 10, Integer::sum);                            // ✅ cộng dồn atomic
        stock.compute("SKU-1", (k, v) -> v == null || v < 3 ? null : v - 3); // trả null = xoá entry
        stock.putIfAbsent("SKU-2", 0);

        // Bulk operations (Java 8) với parallelismThreshold
        long total = stock.reduceValuesToLong(1_000, Integer::longValue, 0L, Long::sum);
        System.out.println(hits + " " + stock + " total=" + total);
    }
}
```

**Bẫy của `compute*`/`merge`**:
- Hàm truyền vào chạy **trong khi giữ lock của bin** → phải ngắn, không I/O, không gọi lại chính map (đặc biệt `computeIfAbsent` lồng `computeIfAbsent` trên cùng map → từ Java 9 ném `IllegalStateException: Recursive update`; Java 8 có thể **treo vô hạn** — bug nổi tiếng JDK-8062841).
- Dùng `computeIfAbsent` làm cache cho tác vụ tốn kém (gọi DB) sẽ block các key khác **cùng bin**. Cache nghiêm túc → Caffeine.
- Value là object mutable (`ArrayList`) → map thread-safe nhưng list bên trong **không**: `map.computeIfAbsent(k, x -> new ArrayList<>()).add(v)` từ nhiều thread là race. Dùng `ConcurrentLinkedQueue`/`CopyOnWriteArrayList` hoặc thực hiện `add` bên trong `compute`.

### 12.3 Các concurrent collection khác

| Collection | Đặc điểm | Dùng khi |
|---|---|---|
| `CopyOnWriteArrayList/Set` | mỗi lần ghi **copy toàn bộ mảng**; đọc/iterate không lock, snapshot | đọc rất nhiều, ghi rất ít (danh sách listener, config) |
| `ConcurrentLinkedQueue/Deque` | lock-free (Michael-Scott queue), unbounded, `size()` O(n) | hàng đợi non-blocking |
| `ConcurrentSkipListMap/Set` | sorted, lock-free, O(log n) | cần thứ tự + đồng thời (thay `TreeMap`) |
| `BlockingQueue` (bên dưới) | có thao tác chờ | producer–consumer |

### 12.4 `BlockingQueue` và producer–consumer

| Method | Ném exception | Trả giá trị đặc biệt | Block | Timeout |
|---|---|---|---|---|
| Thêm | `add` | `offer` → false | `put` | `offer(e, t, unit)` |
| Lấy | `remove` | `poll` → null | `take` | `poll(t, unit)` |
| Xem | `element` | `peek` | — | — |

| Implementation | Đặc điểm |
|---|---|
| `ArrayBlockingQueue` | bounded, mảng vòng, một `ReentrantLock` (tuỳ chọn fair) |
| `LinkedBlockingQueue` | tuỳ chọn bound (mặc định `Integer.MAX_VALUE`!), hai lock (put/take) |
| `SynchronousQueue` | dung lượng 0, handoff trực tiếp |
| `PriorityBlockingQueue` | unbounded, theo ưu tiên |
| `DelayQueue` | phần tử chỉ lấy được khi hết delay |
| `LinkedTransferQueue` | `transfer()` — chờ đến khi consumer nhận |
| `LinkedBlockingDeque` | hai đầu |

```java
import java.util.concurrent.*;

public class ProducerConsumer {
    private static final String POISON = "__STOP__";   // poison pill để dừng consumer

    public static void main(String[] args) throws InterruptedException {
        BlockingQueue<String> q = new ArrayBlockingQueue<>(100);   // bounded → producer bị chậm lại khi consumer chậm
        int consumers = 3;
        ExecutorService pool = Executors.newFixedThreadPool(consumers + 1);

        pool.submit(() -> {
            try {
                for (int i = 0; i < 1_000; i++) q.put("job-" + i);  // block khi đầy = backpressure
                for (int i = 0; i < consumers; i++) q.put(POISON);   // mỗi consumer một viên
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            return null;
        });

        for (int c = 0; c < consumers; c++) {
            pool.submit(() -> {
                try {
                    while (true) {
                        String job = q.take();
                        if (job == POISON) break;          // so sánh reference cố ý
                        process(job);
                    }
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                return null;
            });
        }
        pool.shutdown();
        pool.awaitTermination(1, TimeUnit.MINUTES);
    }
    static void process(String job) { /* ... */ }
}
```

`drainTo(collection, max)` lấy nhiều phần tử một lúc → xử lý theo batch (ghi DB batch), giảm overhead lock.

> 💡 **Góc nhìn Senior:** hàng đợi trong bộ nhớ **mất dữ liệu khi process chết**. Với job quan trọng (gửi email thanh toán), queue in-memory chỉ là buffer; nguồn sự thật nên là DB (outbox pattern) hoặc message broker (Kafka, RabbitMQ). Câu hỏi thiết kế: "nếu pod bị kill khi queue còn 5.000 job thì sao?" — phải trả lời được.

> ⚠️ **Lỗi thường gặp:** `new LinkedBlockingQueue<>()` không bound; dùng `CopyOnWriteArrayList` cho list ghi nhiều (O(n) mỗi lần ghi + rác); nghĩ `size()` của CHM chính xác để làm điều kiện nghiệp vụ; `ConcurrentLinkedQueue.size()` trong vòng lặp (O(n)); consumer bắt `InterruptedException` rồi tiếp tục vòng lặp.

### 🛠 Bài tập phần 12

**Bài 12.1 — Đếm truy cập (Cơ bản)**
- Đề bài: 16 thread đọc log access (giả lập) và đếm số request theo URL và theo status code. So sánh `ConcurrentHashMap<String, LongAdder>`, `ConcurrentHashMap<String, Integer>` + `merge`, và `synchronizedMap<HashMap>`.
- Tiêu chí đạt: kết quả giống nhau; thời gian so sánh; giải thích chênh lệch.

**Bài 12.2 — Batch writer (Trung bình)**
- Đề bài: Producer sinh event; consumer gom batch tối đa 500 event **hoặc** tối đa 200ms (cái nào đến trước) rồi "ghi DB" (sleep 20ms). Graceful shutdown: flush batch cuối.
- Tiêu chí đạt: dùng `poll(timeout)` + `drainTo`; không mất event khi shutdown; đo throughput.

**Bài 12.3 — Cache memoizer (Nâng cao)**
- Đề bài: Viết `Memoizer<K,V>` (JCIP §5.6) đảm bảo mỗi key chỉ tính **đúng một lần** dù nhiều thread yêu cầu đồng thời, tính toán **không** chạy trong lock của CHM, và nếu tính toán lỗi thì lần sau được tính lại.
- Tiêu chí đạt: dùng `ConcurrentHashMap<K, CompletableFuture<V>>` (hoặc `FutureTask`) + `putIfAbsent`; test 100 thread cùng key → hàm tính chạy 1 lần; test lỗi → entry bị xoá.

<details>
<summary>Gợi ý lời giải</summary>

12.2:
```java
List<Event> batch = new ArrayList<>(500);
while (running || !q.isEmpty()) {
    Event first = q.poll(200, MILLISECONDS);
    if (first == null) { flush(batch); continue; }
    batch.add(first);
    long deadline = System.nanoTime() + MILLISECONDS.toNanos(200);
    while (batch.size() < 500 && System.nanoTime() < deadline) {
        q.drainTo(batch, 500 - batch.size());
        if (batch.size() < 500) { Event e = q.poll(deadline - System.nanoTime(), NANOSECONDS); if (e != null) batch.add(e); }
    }
    flush(batch);
}
flush(batch);
```

12.3:
```java
public V get(K key) throws InterruptedException, ExecutionException {
    CompletableFuture<V> f = cache.get(key);
    if (f == null) {
        CompletableFuture<V> nf = new CompletableFuture<>();
        f = cache.putIfAbsent(key, nf);
        if (f == null) {                    // mình thắng → mình tính (ngoài lock của CHM)
            f = nf;
            try { nf.complete(compute.apply(key)); }
            catch (Throwable t) { cache.remove(key, nf); nf.completeExceptionally(t); }
        }
    }
    return f.get();
}
```
</details>

---

<a id="p13"></a>
## 13. ThreadLocal, InheritableThreadLocal & memory leak

### 13.1 Khái niệm & internals

`ThreadLocal<T>` cho mỗi thread một bản sao biến riêng — một dạng **thread confinement**. Ứng dụng: `SimpleDateFormat` per-thread (cũ), transaction/connection hiện tại (Spring `TransactionSynchronizationManager`), `SecurityContextHolder`, MDC logging (traceId), request context.

**Internals**: mỗi `Thread` có field `threadLocals` kiểu `ThreadLocal.ThreadLocalMap` (open addressing, linear probing). Key là `WeakReference<ThreadLocal>`, **value là strong reference**.

```
Thread ──► ThreadLocalMap ──► Entry[ WeakRef(ThreadLocal) → value (STRONG) ]
```

### 13.2 Memory leak trong thread pool

1. Thread trong pool **sống mãi** → `ThreadLocalMap` của nó sống mãi.
2. Nếu object `ThreadLocal` bị GC (không còn strong ref, ví dụ class bị unload), key thành `null` nhưng **value vẫn bị giữ** bởi entry ("stale entry") đến khi map tình cờ dọn dẹp lúc `set/get/remove` khác.
3. Trong application server (Tomcat), value là object của class do **webapp classloader** nạp → giữ cả classloader → redeploy nhiều lần → `OutOfMemoryError: Metaspace`. Tomcat in cảnh báo "created a ThreadLocal ... but failed to remove it".
4. Ngay cả khi không leak bộ nhớ, **dữ liệu rò sang request khác**: thread A xử lý request user X đặt `UserContext`, không xoá; request tiếp theo của user Y chạy trên cùng thread thấy context của X → **lỗ hổng bảo mật**.

```java
public final class RequestContext {
    private static final ThreadLocal<String> USER = new ThreadLocal<>();  // static final: chỉ một key, không bị GC
    public static void set(String u) { USER.set(u); }
    public static String get() { return USER.get(); }
    public static void clear() { USER.remove(); }
}

// ✅ Luôn remove trong finally (servlet filter / interceptor)
void handle(Request req) {
    RequestContext.set(req.user());
    try { service.process(req); }
    finally { RequestContext.clear(); }
}
```

### 13.3 `InheritableThreadLocal`

Giá trị được **copy từ thread cha sang thread con tại thời điểm tạo thread** (`childValue()` có thể override để deep copy). Vấn đề với thread pool: thread được tạo **một lần** (lúc pool khởi tạo/nở thêm), sau đó tái sử dụng → task gửi sau **không** nhận giá trị mới của thread gửi, mà mang giá trị cũ từ lúc thread worker được tạo (do thread nào đó ngẫu nhiên gây ra) → sai và khó debug.

Giải pháp truyền context qua thread pool: wrap task (capture context lúc submit, set lúc chạy, clear sau khi chạy) — Spring `TaskDecorator`, Micrometer Context Propagation, `TransmittableThreadLocal` (Alibaba), hoặc **Scoped Values** (phần 14).

```java
static Runnable withContext(Runnable task) {
    String captured = RequestContext.get();           // capture ở thread gửi
    return () -> {
        String previous = RequestContext.get();
        RequestContext.set(captured);
        try { task.run(); }
        finally { if (previous == null) RequestContext.clear(); else RequestContext.set(previous); }
    };
}
```

> 💡 **Góc nhìn Senior:** ThreadLocal là "biến global ẩn" — tiện nhưng làm luồng dữ liệu khó theo dõi, và **không đi theo** code async (`CompletableFuture`, reactive, virtual threads tạo mới). Với virtual threads (có thể có hàng triệu), ThreadLocal chứa object lớn (buffer, cache per-thread) nhân lên hàng triệu lần → tốn bộ nhớ khủng khiếp; caching object đắt trong ThreadLocal là anti-pattern với virtual threads.

> ⚠️ **Lỗi thường gặp:** không `remove()`; khai báo `ThreadLocal` là field instance (tạo nhiều key); dùng `InheritableThreadLocal` mong truyền context vào thread pool; dùng `ThreadLocal.withInitial` tạo object nặng cho mỗi virtual thread.

### 🛠 Bài tập phần 13

**Bài 13.1 — Rò dữ liệu giữa request (Cơ bản)**
- Đề bài: Mô phỏng web server bằng pool 2 thread xử lý 10 "request" của các user khác nhau, mỗi request set `RequestContext`, một số request "quên" clear (ném exception giữa chừng không có finally). In ra request nào thấy user sai.
- Tiêu chí đạt: tái hiện lỗi; sửa bằng `try/finally`.

**Bài 13.2 — InheritableThreadLocal sai (Trung bình)**
- Đề bài: Chứng minh `InheritableThreadLocal` không truyền đúng context qua `newFixedThreadPool(2)` khi submit 10 task từ các thread có context khác nhau. Sửa bằng wrapper `withContext` cho cả `Runnable` và `Callable`, và bằng một `ExecutorService` decorator tự wrap mọi task.
- Tiêu chí đạt: test pass cho 1000 task ngẫu nhiên.

**Bài 13.3 — Classloader leak (Nâng cao)**
- Đề bài: Tạo `URLClassLoader` nạp một class đặt object của nó vào một `ThreadLocal` của thread sống lâu, rồi bỏ mọi tham chiếu tới classloader. Chứng minh classloader không bị GC (dùng `WeakReference` + `System.gc()` + đợi; hoặc heap dump + MAT tìm "Path to GC roots"). Sửa và chứng minh classloader được thu hồi.
- Tiêu chí đạt: có heap dump hoặc log chứng minh; giải thích chuỗi tham chiếu Thread → ThreadLocalMap → Entry → value → Class → ClassLoader.

<details>
<summary>Gợi ý lời giải</summary>

13.3: chuỗi giữ: `Thread (GC root)` → `threadLocals` → `Entry.value` (strong) → instance → `getClass()` → `ClassLoader` → mọi class nó nạp + static field. Kể cả khi key (ThreadLocal) đã bị GC, value vẫn còn. Sửa: gọi `remove()` trên thread đó trước khi bỏ classloader; hoặc dùng thread ngắn hạn.
```java
WeakReference<ClassLoader> ref = new WeakReference<>(loader);
loader = null;
for (int i = 0; i < 10 && ref.get() != null; i++) { System.gc(); Thread.sleep(100); }
System.out.println(ref.get() == null ? "thu hồi" : "LEAK");
```
</details>

---

<a id="p14"></a>
## 14. Virtual Threads, Structured Concurrency, Scoped Values

### 14.1 Vì sao cần virtual threads (Project Loom)

Mô hình **thread-per-request** đơn giản, dễ debug (stack trace rõ ràng), nhưng platform thread đắt → giới hạn vài nghìn thread → server bị giới hạn bởi số thread chứ không phải CPU khi workload chủ yếu chờ I/O. Lối thoát cũ là lập trình **bất đồng bộ/reactive** (CompletableFuture, WebFlux) — hiệu quả nhưng khó viết, khó debug, stack trace vụn.

**Virtual thread** (JEP 444, **final ở Java 21**) là thread nhẹ do **JVM** quản lý: tạo được **hàng triệu**, chi phí tạo ~ vài trăm byte đến vài KB (stack lưu trên heap dưới dạng các chunk, co giãn theo nhu cầu). Giữ nguyên mô hình lập trình đồng bộ, blocking quen thuộc.

### 14.2 Cách hoạt động

- Virtual thread chạy **trên** một **carrier thread** (platform thread) thuộc một `ForkJoinPool` riêng (FIFO mode), parallelism mặc định = số core (`-Djdk.virtualThreadScheduler.parallelism`).
- Khi virtual thread gọi thao tác blocking được JDK hỗ trợ (socket I/O, `Thread.sleep`, `BlockingQueue.take`, `ReentrantLock.lock`, `Future.get`...), JDK **unmount**: copy stack frame của nó lên heap, giải phóng carrier để chạy virtual thread khác. Khi I/O sẵn sàng, virtual thread được **mount** lại (có thể trên carrier khác).
- Không phải mọi blocking đều unmount được. Một số thao tác (file I/O trên nhiều OS, `Object.wait()` trước JDK 24, DNS lookup) "**capture**" carrier — JDK bù bằng cách tạm tăng số carrier (đến `maxPoolSize`, mặc định 256).

```java
import java.time.Duration;
import java.util.concurrent.*;
import java.util.stream.IntStream;

public class VirtualThreadsDemo {
    public static void main(String[] args) {
        long t0 = System.currentTimeMillis();
        try (ExecutorService ex = Executors.newVirtualThreadPerTaskExecutor()) {   // KHÔNG pool virtual threads
            IntStream.range(0, 100_000).forEach(i -> ex.submit(() -> {
                Thread.sleep(Duration.ofSeconds(1));       // unmount, không chiếm carrier
                return i;
            }));
        }   // close() chờ mọi task xong
        System.out.println("100k task trong " + (System.currentTimeMillis() - t0) + "ms"); // ~1–2 giây
        // Với newFixedThreadPool(200): ~500 giây.
    }
}
```

### 14.3 Pinning

Virtual thread bị **pin** (dính chặt vào carrier, không unmount được khi block) khi:
1. Block bên trong khối/method **`synchronized`** — **JDK 21–23**. **JEP 491 (JDK 24)** sửa: virtual thread có thể unmount khi giữ hoặc chờ monitor, và `Object.wait()` cũng unmount.
2. Đang chạy **native method** hoặc **foreign function** (vẫn còn sau JDK 24).

Pin làm mất lợi ích: với N carrier = số core, chỉ cần N virtual thread bị pin khi block I/O là **toàn bộ** scheduler đứng → throughput sụp, thậm chí deadlock (virtual thread giữ lock cần carrier để chạy nhưng mọi carrier bị pin bởi các thread chờ lock đó).

Phát hiện: JFR event `jdk.VirtualThreadPinned` (mặc định khi pin > 20ms); trên JDK 21–23 có thêm `-Djdk.tracePinnedThreads=full|short` in stack trace khi pin (cờ này bị loại bỏ ở JDK 24 sau JEP 491). Sửa (JDK 21–23): đổi `synchronized` bao quanh I/O sang `ReentrantLock`; nâng cấp thư viện (driver JDBC, connection pool như HikariCP, đã được cập nhật cho Loom).

### 14.4 Khi nào virtual threads giúp, khi nào không

| Giúp nhiều | Không giúp / có hại |
|---|---|
| Server thread-per-request với nhiều I/O chờ (gọi DB, HTTP, cache) | Tác vụ **CPU-bound** (số carrier = số core, không nhanh hơn platform thread) |
| Fan-out nhiều lời gọi I/O song song | Code dùng `synchronized` quanh I/O trên JDK 21–23 (pinning) |
| Thay thế code reactive phức tạp bằng code tuần tự dễ đọc | Code phụ thuộc ThreadLocal cache object nặng |
| Số lượng tác vụ đồng thời lớn (10k–1M) | Khi bottleneck là tài nguyên hữu hạn (DB connection 20 → 1 triệu virtual thread vẫn chỉ 20 query đồng thời) |

Quy tắc dùng:
- **Không pool virtual threads** — tạo mới cho mỗi task; chúng rẻ. Pool để **giới hạn concurrency** thì thay bằng `Semaphore`.
- Throughput tăng, không phải latency: một request vẫn chậm như cũ, nhưng server xử lý được nhiều request đồng thời hơn.
- Spring Boot 3.2+: `spring.threads.virtual.enabled=true` cho Tomcat/Jetty, `@Async`, scheduler.
- Virtual thread luôn là **daemon**, priority cố định, không đổi được.
- Thread dump: `jcmd <pid> Thread.dump_to_file -format=json` (jstack truyền thống không liệt kê virtual thread).

### 14.5 Structured Concurrency (preview)

Ý tưởng: các subtask bất đồng bộ có **vòng đời lồng trong scope** của task cha (như cấu trúc khối code) — task cha không return khi con còn chạy; một con lỗi → huỷ các con còn lại; huỷ cha → huỷ con; thread dump thể hiện quan hệ cha–con.

Trạng thái: **preview** từ Java 21 (JEP 453) và vẫn **preview** ở Java 25 (JEP 505, API thay đổi: tạo scope bằng `StructuredTaskScope.open(...)` với `Joiner` thay vì subclass `ShutdownOnFailure`). **Không dùng cho production** trừ khi chấp nhận `--enable-preview` và API thay đổi giữa các bản.

```java
// Java 21 preview API (javac --release 21 --enable-preview)
import java.util.concurrent.StructuredTaskScope;

record Response(String user, String order) {}

Response handle() throws Exception {
    try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
        var user  = scope.fork(() -> findUser());     // mỗi fork chạy trên một virtual thread mới
        var order = scope.fork(() -> fetchOrder());
        scope.join()                                   // chờ cả hai
             .throwIfFailed();                         // một lỗi → huỷ cái còn lại và ném lỗi
        return new Response(user.get(), order.get());
    }   // ra khỏi scope: đảm bảo không subtask nào còn chạy
}
```

So với `CompletableFuture.allOf`: fail-fast và huỷ (interrupt thực sự) task anh em là mặc định, không rò task "mồ côi".

### 14.6 Scoped Values

`ScopedValue<T>` — thay thế ThreadLocal cho việc truyền context **bất biến**, có phạm vi rõ ràng, chi phí thấp, tự kế thừa vào subtask của structured concurrency:

```java
static final ScopedValue<String> USER = ScopedValue.newInstance();

void serve(Request req) {
    ScopedValue.where(USER, req.user()).run(() -> handle());   // binding chỉ tồn tại trong run()
}
void handle() { System.out.println("user = " + USER.get()); }  // đọc ở bất kỳ đâu trong call chain
```
So với ThreadLocal: bất biến (không `set`), tự hết hạn khi ra khỏi scope (không cần `remove`, không leak), rẻ với hàng triệu virtual thread. Trạng thái: preview ở Java 21 (JEP 446), **final ở Java 25** (JEP 506).

> 💡 **Góc nhìn Senior:**
> - Câu trả lời hay khi được hỏi "virtual threads có thay thế WebFlux không?": virtual threads cho phép đạt **khả năng mở rộng tương đương** cho workload I/O-bound mà vẫn viết code blocking; reactive vẫn có giá trị cho streaming, backpressure tinh vi, và hệ thống đã đầu tư sẵn. Nhưng với dự án mới trên Java 21+, thread-per-request + virtual threads thường là lựa chọn đơn giản hơn.
> - Bật virtual threads mà không giới hạn concurrency có thể **dồn tải về downstream**: trước đây pool 200 thread vô tình làm "van" giới hạn; giờ 10.000 request đồng thời ập vào DB. Đặt `Semaphore`/bulkhead, kiểm tra connection pool timeout.
> - Kiểm tra pinning trên staging bằng JFR trước khi bật trên production (đặc biệt JDK 21).

> ⚠️ **Lỗi thường gặp:** tạo `newFixedThreadPool(100)` với `Thread.ofVirtual().factory()` (pool virtual thread — vô nghĩa); dùng virtual threads cho tính toán nặng; nghĩ virtual thread làm từng request nhanh hơn; giữ `synchronized` quanh lời gọi HTTP/JDBC trên JDK 21; dùng preview API trong production.

### 🛠 Bài tập phần 14

**Bài 14.1 — So sánh throughput (Cơ bản)**
- Đề bài: Chạy 10.000 task, mỗi task gọi một HTTP server cục bộ có độ trễ 100ms (`com.sun.net.httpserver` với executor virtual threads). So sánh: `newFixedThreadPool(200)`, `newVirtualThreadPerTaskExecutor()`.
- Tiêu chí đạt: bảng thời gian tổng và số OS thread tối đa (`ThreadMXBean.getPeakThreadCount()` hoặc `top -H`).

**Bài 14.2 — Tái hiện pinning (Trung bình)**
- Đề bài: Trên JDK 21, viết code mà mỗi virtual thread vào `synchronized` rồi `Thread.sleep(100)` (giả lập I/O). Chạy 1.000 task; đo thời gian; bật `-Djdk.tracePinnedThreads=short` và ghi JFR với event `jdk.VirtualThreadPinned`. Sửa bằng `ReentrantLock` và đo lại. (Tuỳ chọn: chạy lại bản `synchronized` trên JDK 24+ và so sánh.)
- Tiêu chí đạt: số liệu trước/sau; trích output JFR (`jfr print --events jdk.VirtualThreadPinned rec.jfr`).

**Bài 14.3 — Giới hạn tài nguyên downstream (Nâng cao)**
- Đề bài: Một service virtual-thread nhận 5.000 request đồng thời, mỗi request cần một "DB connection" từ pool 10 kết nối (giả lập bằng `ArrayBlockingQueue` 10 phần tử với `poll(timeout)`) và một lời gọi HTTP. Thiết kế để: không request nào chờ connection quá 2 giây (trả lỗi nhanh), DB không bao giờ nhận > 10 truy vấn đồng thời, và có metrics.
- Tiêu chí đạt: viết bằng code blocking thuần + `Semaphore`; báo cáo tỉ lệ thành công/thất bại, p99 latency; so sánh phiên bản Structured Concurrency (preview) cho phần fan-out (tuỳ chọn).

<details>
<summary>Gợi ý lời giải</summary>

14.2:
```java
Object lock = new Object();
try (var ex = Executors.newVirtualThreadPerTaskExecutor()) {
    for (int i = 0; i < 1000; i++) ex.submit(() -> {
        synchronized (new Object()) {          // mỗi task lock riêng: không tranh chấp, nhưng vẫn PIN khi sleep
            Thread.sleep(100);
        }
        return null;
    });
}
// JDK 21, 8 carrier: ~1000/8 × 100ms ≈ 12.5s. Với ReentrantLock (hoặc JDK 24+): ~100–200ms.
```
Ghi JFR: `java -XX:StartFlightRecording=filename=rec.jfr,settings=profile ...`.
</details>

---

<a id="p15"></a>
## 15. Chiến lược thiết kế & kiểm thử code đồng thời

### 15.1 Chiến lược thiết kế (theo thứ tự ưu tiên)

1. **Không chia sẻ (thread confinement)**: dữ liệu chỉ thuộc về một thread — biến local (stack confinement), ThreadLocal, hoặc kiến trúc single-writer (một thread duy nhất ghi, như event loop của Netty, LMAX Disruptor, actor).
2. **Bất biến (immutability)**: object immutable thread-safe "miễn phí". Điều kiện (JCIP §3.4): state không thay đổi sau construct; mọi field `final`; construct đúng cách (không `this` escape). Record + collection `List.copyOf` là công cụ chính. Thay đổi = tạo object mới + publish qua `AtomicReference`/`volatile` ("copy-on-write" ở mức domain).
3. **Ủy thác thread-safety** cho thư viện đã kiểm chứng: `ConcurrentHashMap`, `BlockingQueue`, atomics, Caffeine.
4. **Đồng bộ hoá rõ ràng**: mỗi biến chia sẻ mutable được bảo vệ bởi **đúng một** lock, ghi tài liệu (`@GuardedBy("lock")` — annotation của JCIP/Error Prone) để reviewer kiểm tra.
5. **Ghi tài liệu chính sách thread-safety** của class (Effective Java Item 82): immutable / unconditionally thread-safe / conditionally thread-safe / not thread-safe / thread-hostile.

```java
// Bất biến + publish an toàn: config có thể reload mà không cần lock ở phía đọc
public final class ConfigHolder {
    public record Config(int timeoutMs, java.util.List<String> hosts) {
        public Config { hosts = java.util.List.copyOf(hosts); }
    }
    private final java.util.concurrent.atomic.AtomicReference<Config> current;
    public ConfigHolder(Config initial) { current = new java.util.concurrent.atomic.AtomicReference<>(initial); }
    public Config get() { return current.get(); }                    // reader: không lock, luôn thấy snapshot nhất quán
    public void reload(Config next) { current.set(next); }           // writer: thay cả object
    public void addHost(String h) {                                  // cập nhật dựa trên giá trị cũ → CAS loop
        current.updateAndGet(c -> { var l = new java.util.ArrayList<>(c.hosts()); l.add(h); return new Config(c.timeoutMs(), l); });
    }
}
```

### 15.2 Kiểm thử code đồng thời

Bug concurrency phụ thuộc lịch thực thi (interleaving) → **unit test thông thường gần như không bắt được**. Các kỹ thuật:

1. **Test dưới áp lực**: nhiều thread, nhiều vòng lặp, `CountDownLatch` để mọi thread xuất phát cùng lúc (tối đa hoá xen kẽ); kiểm tra **invariant** (tổng tiền không đổi, không phần tử trùng/mất) thay vì giá trị cụ thể; chạy lặp lại (`@RepeatedTest`).
2. **Số thread > số core** để buộc context switch; chèn `Thread.yield()`/`Thread.onSpinWait()` ở chỗ nghi ngờ (chỉ trong bản test).
3. **jcstress** (OpenJDK): framework chuyên kiểm tra JMM — chạy hàng triệu lần, thống kê mọi kết quả có thể.
4. **Lincheck** (JetBrains): kiểm tra linearizability của cấu trúc dữ liệu đồng thời bằng model checking + stress.
5. **Awaitility** cho test bất đồng bộ: `await().atMost(5, SECONDS).until(() -> queue.isEmpty())` — thay vì `Thread.sleep` (chậm và flaky).
6. **Thiết kế để test được**: tách logic khỏi threading (logic thuần test bằng unit test; phần threading mỏng); inject `Executor` (trong test dùng `Runnable::run` — executor đồng bộ — hoặc executor có thể điều khiển); inject `Clock`.
7. **Static analysis**: Error Prone (`@GuardedBy` checker), SpotBugs (các pattern `IS2_INCONSISTENT_SYNC`, `DC_DOUBLECHECK`...), IntelliJ inspections.
8. **Quan sát runtime**: JFR (`jdk.JavaMonitorEnter`, `jdk.ThreadPark`), async-profiler chế độ lock (`-e lock`) để tìm contention.

```java
import org.junit.jupiter.api.RepeatedTest;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class BankConcurrencyTest {
    @RepeatedTest(20)
    void totalBalanceIsConserved() throws Exception {
        int accounts = 10, threads = 16, ops = 10_000;
        Bank bank = new Bank(accounts, 1_000);
        long expected = bank.total();
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService ex = Executors.newFixedThreadPool(threads)) {
            for (int t = 0; t < threads; t++) {
                ex.submit(() -> {
                    start.await();
                    var rnd = ThreadLocalRandom.current();
                    for (int i = 0; i < ops; i++)
                        bank.transfer(rnd.nextInt(accounts), rnd.nextInt(accounts), rnd.nextInt(50));
                    return null;
                });
            }
            start.countDown();
        }   // close() chờ xong; nếu deadlock → test treo → đặt @Timeout
        assertEquals(expected, bank.total());
    }
}
```

> 💡 **Góc nhìn Senior:** test pass **không** chứng minh code đúng — chỉ chứng minh chưa tìm thấy lỗi. Lập luận đúng đắn dựa trên JMM + review + giữ thiết kế đơn giản (ít state chia sẻ) quan trọng hơn. Trong code review, câu hỏi đầu tiên: *"State nào được chia sẻ giữa các thread? Cái gì bảo vệ nó?"*. Test concurrency **flaky** là tín hiệu quý — đừng tắt nó, hãy điều tra.

> ⚠️ **Lỗi thường gặp:** test dùng `Thread.sleep` để "chờ" (flaky); assertion trong thread con (JUnit không thấy exception ở thread khác — phải thu thập lỗi và assert ở thread test, hoặc dùng `Future.get()`); không đặt `@Timeout` cho test có thể deadlock (CI treo hàng giờ).

### 🛠 Bài tập phần 15

**Bài 15.1 — Thread-safety policy (Cơ bản)**
- Đề bài: Chọn 6 class JDK (`String`, `StringBuilder`, `ArrayList`, `ConcurrentHashMap`, `Collections.synchronizedList`, `SimpleDateFormat`, `DateTimeFormatter`, `Random`) và phân loại theo 5 mức của Effective Java Item 82; giải thích `Random` thread-safe nhưng vì sao nên dùng `ThreadLocalRandom` khi đa luồng.
- Tiêu chí đạt: mỗi class có lý do 1–2 câu.

**Bài 15.2 — Test invariant (Trung bình)**
- Đề bài: Viết test stress cho `BoundedBuffer` (phần 5): 4 producer, 4 consumer, 1 triệu phần tử; kiểm tra không mất, không trùng, thứ tự FIFO theo từng producer; có `@Timeout`. Dùng Awaitility ở một test async.
- Tiêu chí đạt: test phát hiện được lỗi khi bạn cố ý đổi `while` thành `if` hoặc `notifyAll` thành `notify` (có thể cần chạy nhiều lần).

**Bài 15.3 — Lincheck hoặc jcstress (Nâng cao)**
- Đề bài: Dùng Lincheck (Kotlin/Java) kiểm tra Treiber stack (phần 6) và một phiên bản cố ý sai (không CAS). Hoặc dùng jcstress kiểm tra DCL thiếu `volatile`.
- Tiêu chí đạt: công cụ tìm ra lỗi với bản sai và pass với bản đúng; trình bày kịch bản xen kẽ mà công cụ báo cáo.

<details>
<summary>Gợi ý lời giải</summary>

15.1: `String` immutable; `StringBuilder` not thread-safe; `ArrayList` not thread-safe; `ConcurrentHashMap` unconditionally thread-safe; `synchronizedList` conditionally thread-safe (iteration cần lock ngoài); `SimpleDateFormat` not thread-safe (giữ `Calendar` nội bộ); `DateTimeFormatter` immutable; `Random` thread-safe (CAS trên seed `AtomicLong`) nhưng contention cao → `ThreadLocalRandom` (seed lưu trong `Thread`).

15.2: dùng `ConcurrentHashMap.newKeySet()` thu thập phần tử đã consume; mỗi producer gửi `(producerId, seq)`; consumer kiểm tra `seq` tăng dần theo `producerId` (chỉ đúng với một consumer; với nhiều consumer, kiểm tra tập hợp đầy đủ và không trùng).
</details>

---

<a id="du-an-mini"></a>
## Dự án mini: "Concurrent Order Processing Engine"

Xây dựng một engine xử lý đơn hàng chạy trong một JVM, mô phỏng backend thương mại điện tử trong đợt flash sale.

### Bối cảnh
- Đơn hàng đến từ một "ingestion" HTTP endpoint (dùng `com.sun.net.httpserver.HttpServer` với executor virtual threads) hoặc từ generator tải.
- Mỗi đơn hàng: kiểm tra tồn kho (in-memory, nhiều SKU "hot" bị tranh chấp), tính giá (gọi "pricing service" giả lập latency 20–200ms, lỗi 5%), trừ tiền ví khách hàng (giả lập), ghi "DB" (giả lập, connection pool 20), gửi thông báo (best-effort).

### Yêu cầu chức năng
1. **Tồn kho**: `InventoryService.reserve(sku, qty)` không bao giờ bán âm (oversell = 0) dưới 1.000 request đồng thời trên cùng SKU; có `release` khi đơn thất bại sau khi đã giữ hàng.
2. **Ví**: `WalletService.transfer(from, to, amount)` không deadlock, tổng tiền hệ thống bảo toàn.
3. **Pipeline**: dùng `CompletableFuture` **hoặc** virtual threads (+ tuỳ chọn Structured Concurrency preview) để gọi pricing và kiểm tra fraud **song song**; timeout tổng 500ms mỗi đơn; retry pricing tối đa 2 lần với backoff cho lỗi tạm thời.
4. **Ghi DB theo batch**: consumer gom batch (≤ 200 bản ghi hoặc ≤ 100ms) từ `BlockingQueue` bounded.
5. **Thông báo**: pool riêng (bulkhead), rejection policy ghi log + đếm; lỗi thông báo không ảnh hưởng đơn hàng.
6. **Context**: mỗi đơn có `traceId` truyền qua mọi thread/task (ThreadLocal + wrapper **hoặc** ScopedValue nếu dùng Java 25), hiện trong mọi dòng log.
7. **Quan sát**: endpoint `/metrics` trả JSON: số đơn thành công/thất bại theo lý do, p50/p95/p99 latency, queue size, active threads, số lần rejected, deadlock detector status.
8. **Graceful shutdown**: SIGTERM → ngừng nhận đơn, xử lý xong đơn đang chạy (tối đa 15 giây), flush batch DB, in thống kê cuối.

### Yêu cầu phi chức năng
- Java 21 (hoặc 25); không dùng thư viện concurrency bên thứ ba (Caffeine, Resilience4j...) — tự viết để luyện tập; được dùng JUnit 5, Awaitility, JMH, jcstress.
- Không dùng `Executors.newFixedThreadPool`/`newCachedThreadPool` (phải cấu hình `ThreadPoolExecutor` tường minh hoặc virtual threads + `Semaphore`).
- Mọi `ThreadLocal` được `remove()`; mọi lock được `unlock` trong `finally`; mọi `InterruptedException` được xử lý đúng.
- Tải thử: 50.000 đơn trong 60 giây với 2.000 client đồng thời: oversell = 0, tiền bảo toàn, không deadlock, p99 < 1 giây (với latency giả lập ở trên).
- Chạy JFR trong lúc tải thử, báo cáo: top lock contention, có/không virtual thread pinning, GC pause.
- `README.md` của dự án giải thích từng quyết định concurrency (vì sao chọn lock/atomic/CHM/queue ở mỗi chỗ) và sizing các pool có tính toán (Little's Law).

### Tiêu chí chấm (100 điểm)
| Hạng mục | Điểm |
|---|---|
| Tính đúng: oversell = 0, bảo toàn tiền, không deadlock (chứng minh bằng test stress + lập luận) | 25 |
| Sử dụng đúng công cụ concurrency (lock ordering, CAS, CHM atomic ops, BlockingQueue, Semaphore) | 20 |
| Bất đồng bộ: timeout, retry, xử lý lỗi, không rò task, context propagation | 15 |
| Cấu hình pool & backpressure có cơ sở (bounded queue, rejection, sizing) | 10 |
| Quan sát được: metrics, thread dump/JFR phân tích, deadlock detector | 10 |
| Graceful shutdown đúng | 5 |
| Kiểm thử: stress test có invariant, `@Timeout`, ít nhất một test jcstress hoặc Lincheck | 10 |
| Tài liệu thiết kế & trade-off | 5 |

---

<a id="checklist-tu-danh-gia"></a>
## Checklist tự đánh giá

- [ ] Tôi kể được 6 trạng thái thread và biết thread chờ `ReentrantLock` là `WAITING`, chờ socket I/O là `RUNNABLE`.
- [ ] Tôi xử lý `InterruptedException` đúng (ném tiếp hoặc khôi phục cờ) và giải thích được cơ chế huỷ hợp tác.
- [ ] Tôi phân biệt atomicity, visibility, ordering và cho ví dụ cho từng loại; phân biệt race condition với data race.
- [ ] Tôi liệt kê được các quy tắc happens-before và dùng chúng để chứng minh một đoạn code đúng/sai.
- [ ] Tôi giải thích được safe publication, `this` escape và final field semantics.
- [ ] Tôi biết `synchronized` đảm bảo gì, reentrancy, và tóm tắt được các trạng thái lock (biết biased locking đã bị tắt từ JDK 15).
- [ ] Tôi nói rõ `volatile` đảm bảo gì và KHÔNG đảm bảo gì; viết được DCL đúng và giải thích vì sao cần `volatile`; biết holder idiom và enum singleton.
- [ ] Tôi viết được producer–consumer bằng `wait/notifyAll`, bằng `Lock + Condition`, và bằng `BlockingQueue`.
- [ ] Tôi so sánh được `synchronized`, `ReentrantLock`, `ReadWriteLock`, `StampedLock` và biết khi nào dùng cái nào; biết AQS là gì.
- [ ] Tôi giải thích CAS, ABA, và khi nào `LongAdder` tốt hơn `AtomicLong` (và hạn chế của `sum()`); biết false sharing.
- [ ] Tôi nêu 4 điều kiện Coffman, các cách phòng deadlock, và đọc được thread dump để tìm deadlock / pool starvation.
- [ ] Tôi mô tả chính xác thuật toán nhận task của `ThreadPoolExecutor` và vì sao `newFixedThreadPool`/`newCachedThreadPool` nguy hiểm.
- [ ] Tôi tính được kích thước pool cho CPU-bound và I/O-bound (công thức JCIP, Little's Law) và biết ràng buộc downstream.
- [ ] Tôi biết exception trong `submit` bị nuốt vào `Future` và task định kỳ dừng khi ném exception.
- [ ] Tôi dùng `CompletableFuture` với executor riêng, compose (`thenCompose`, `thenCombine`, `allOf`), xử lý lỗi (unwrap `CompletionException`), timeout; biết `cancel` không interrupt.
- [ ] Tôi giải thích work stealing và mối liên hệ giữa `ForkJoinPool.commonPool` với parallel stream, CF và virtual threads.
- [ ] Tôi chọn đúng `CountDownLatch`, `CyclicBarrier`, `Semaphore`, `Phaser` cho bài toán.
- [ ] Tôi giải thích internals `ConcurrentHashMap` (Java 8+), vì sao cấm null, bẫy của `computeIfAbsent`, và dùng đúng thao tác atomic.
- [ ] Tôi giải thích được ThreadLocal leak trong thread pool, rò dữ liệu giữa request, và giới hạn của `InheritableThreadLocal`.
- [ ] Tôi giải thích virtual threads (carrier, mount/unmount, pinning — đã sửa cho `synchronized` ở JDK 24), khi nào giúp và khi nào không; biết trạng thái preview/final của Structured Concurrency và Scoped Values.
- [ ] Tôi áp dụng immutability và thread confinement như chiến lược thiết kế đầu tiên.
- [ ] Tôi viết được stress test có invariant, dùng Awaitility, và biết jcstress/Lincheck dùng để làm gì.

# Case 15 — Job `@Scheduled` chạy trên 4 instance xử lý trùng bản ghi: từ Redis lock ngây thơ đến claim bằng `SKIP LOCKED`

> **Chủ đề:** `@Scheduled` khi scale ngang, distributed lock, lock hết hạn do GC pause, ShedLock, Redisson watchdog, fencing token, `SELECT ... FOR UPDATE SKIP LOCKED`
> **Module liên quan:** [M07 §13 — @Async, @Scheduled](../01-giao-trinh/07-spring-core-boot.md#p13) · [M12 §12 — Distributed lock với Redis](../01-giao-trinh/12-redis-caching.md#phan-12) · [M11 §8 — Locking, SELECT FOR UPDATE](../01-giao-trinh/11-database-sql.md#phan-8) · [M09 §10 — Optimistic vs pessimistic locking](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#10-locking) · [M05 §8 — Garbage Collectors](../01-giao-trinh/05-jvm-memory-gc-performance.md#p8) · [M05 §14 — Runbook GC cao](../01-giao-trinh/05-jvm-memory-gc-performance.md#p14) · [M14 §5 — Idempotency](../01-giao-trinh/14-microservices-system-design.md#p5)
> **Độ khó:** ⭐⭐⭐⭐ (Senior)
> **Thời gian tự giải gợi ý:** 40 phút

---

## 1. Bối cảnh hệ thống

`reward-service` (minh họa) trả **cashback** vào ví khách hàng sau khi đơn hàng hết thời hạn đổi trả.

```
reward-service (4 pod, Boot 3.2, Java 17, heap 4 GB, G1)
   @Scheduled(cron = "0 */5 * * * *")  CashbackPayoutJob
      1. SELECT * FROM cashback WHERE status='PENDING' AND eligible_at <= now()
      2. for each: walletClient.credit(customerId, amount)    ──► wallet-service (REST)
                   UPDATE cashback SET status='PAID'
   PostgreSQL 15 (bảng cashback ~ 60 triệu dòng, ~150.000 bản ghi PENDING/ngày)
   Redis 7 (sentinel)
```

| Thông số | Giá trị |
|---|---|
| Bản ghi đến hạn mỗi 5 phút | 500–2.000 bình thường, **~180.000** lúc 00:05 ngày sau sale (đồng loạt hết hạn đổi trả) |
| wallet-service | `POST /credits` — **không** nhận idempotency key (API cũ) |
| Thời gian chạy job | 20–60 s bình thường |

Lịch sử: ban đầu job chạy trên 1 pod. Khi scale lên 4 pod, cashback bị trả **4 lần** trong ngày đầu tiên; team "sửa nhanh" bằng một Redis lock:

```java
@Scheduled(cron = "0 */5 * * * *")
public void payout() {
    Boolean ok = redis.opsForValue().setIfAbsent("lock:cashback-payout", "1", Duration.ofSeconds(120));
    if (!Boolean.TRUE.equals(ok)) return;
    try {
        List<Cashback> due = repo.findByStatusAndEligibleAtBefore(PENDING, Instant.now());   // load tất cả
        for (Cashback c : due) {
            walletClient.credit(c.getCustomerId(), c.getAmount());
            c.setStatus(PAID);
            repo.save(c);
        }
    } finally {
        redis.delete("lock:cashback-payout");                // xóa không kiểm tra chủ sở hữu
    }
}
```

---

## 2. Triệu chứng

Sau đợt sale 9/9, ngày 16/9 lúc 00:05 (đơn hàng sale hết hạn đổi trả đồng loạt):

- Đối soát ví sáng hôm sau: **7.412 khách** được cộng cashback 2 lần, 380 khách 3 lần; tổng chi thừa ~412 triệu VND.
- Log hai pod chạy cùng lúc:

```
00:05:00.012 [pod-a scheduling-1] CashbackPayoutJob : acquired lock, 181.204 due records
00:07:00.004 [pod-c scheduling-1] CashbackPayoutJob : acquired lock, 152.660 due records
00:10:00.009 [pod-b scheduling-1] CashbackPayoutJob : acquired lock, 121.877 due records
00:11:42.871 [pod-a scheduling-1] CashbackPayoutJob : finished 181.204 records in 402.8s
```

- GC log của pod-a:

```
[2026-09-16T00:05:31.204+0700][info][gc] GC(812) Pause Full (G1 Compaction Pause) 3961M->3874M(4096M) 71423.118ms
[2026-09-16T00:06:58.770+0700][info][gc] GC(815) Pause Full (G1 Compaction Pause) 4012M->3990M(4096M) 38817.502ms
```

- Ngoài ra, có lần pod-a `finally` xóa lock trong khi pod-c đang giữ lock → pod-b lấy được lock ngay sau đó → **3 pod** cùng chạy.

---

## 3. Câu hỏi đặt ra

1. Vì sao lock TTL 120 s vẫn để hai, ba pod chạy cùng lúc? Kể hết các lỗi trong đoạn code lock.
2. Nếu dùng Redisson (watchdog) hoặc ShedLock thì có hết lỗi không? Tại sao?
3. Thiết kế lại để **đúng** ngay cả khi có GC pause dài, mạng chậm, pod bị kill giữa chừng.
4. Làm sao xử lý 180.000 bản ghi nhanh mà không cần một instance duy nhất?

> ✋ **Dừng lại và tự giải trước.** Phân biệt lock dùng cho **hiệu quả** (tránh làm thừa) và lock dùng cho **đúng đắn** (không được trùng). Job này thuộc loại nào?

---

## 4. Điều tra từng bước

### Bước 1 — Dựng lại timeline từ log và GC log

```
00:05:00  pod-a SET NX lock (TTL 120s) → OK, load 181k entity vào heap
00:05:31  pod-a Full GC 71 s (STW: mọi thread dừng)
00:07:00  lock hết hạn (pod-a vẫn đang "đứng hình") → pod-c SET NX → OK
00:07:00+ pod-c chạy, SELECT PENDING → gồm cả các bản ghi pod-a đã load nhưng CHƯA kịp update → trả lần 2
00:08:xx  pod-a tỉnh dậy, tiếp tục vòng lặp với danh sách cũ trong bộ nhớ → trả tiếp các bản ghi pod-c đã trả
```

### Bước 2 — Vì sao Full GC 71 s

```bash
jcmd <pid> GC.heap_info
# garbage-first heap total 4194304K, used 4061020K  (gần đầy)
```

Job load **toàn bộ** 181.000 entity JPA (kèm quan hệ `@ManyToOne(fetch = EAGER) Order`) vào persistence context; dirty checking giữ snapshot mỗi entity → heap gần 4 GB toàn object còn sống → G1 không giải phóng được gì → Full GC (song song từ JDK 10, nhưng với heap đầy object sống vẫn mất hàng chục giây). Không phải lỗi GC; là lỗi xử lý dữ liệu theo khối khổng lồ.

### Bước 3 — Danh sách lỗi trong code lock

| # | Lỗi | Hậu quả |
|---|---|---|
| 1 | TTL cố định 120 s, thời gian chạy thực tế 400 s | Lock hết hạn giữa chừng |
| 2 | Giá trị lock là `"1"`, `finally` xóa không kiểm tra token | Pod chậm xóa lock của pod khác → pod thứ ba lấy được |
| 3 | Không có cơ chế phát hiện "tôi đã mất lock" trước khi ghi | Pod tỉnh dậy sau GC vẫn tiếp tục ghi |
| 4 | Danh sách việc lấy một lần ở đầu job, không đánh dấu "đang xử lý" | Hai pod thấy cùng tập bản ghi PENDING |
| 5 | Gọi wallet không idempotent, update trạng thái **sau** khi gọi | Không có lớp chặn cuối ở tài nguyên đích |

### Bước 4 — Đánh giá các "fix" đề xuất trong team

- **Redisson watchdog:** gia hạn lock mỗi ~10 s khi thread còn giữ. Giải quyết lỗi 1 (job chạy lâu) nhưng **không** giải quyết GC pause: watchdog chạy trên thread trong cùng JVM, STW 71 s dừng luôn watchdog → lock (TTL 30 s) hết hạn → pod khác lấy lock. Không giải quyết lỗi 3.
- **ShedLock** (`lockAtMostFor = 30m`): giải quyết lỗi 1, 2 nếu `lockAtMostFor` đủ dài; nhưng vẫn là lock dựa trên thời gian — pause dài hơn `lockAtMostFor`, hoặc lệch đồng hồ giữa các node (ShedLock JDBC mặc định dùng giờ của app, có tùy chọn `usingDbTime()`) vẫn có thể lọt. Nó là công cụ đúng cho "đừng chạy job thừa", không phải cho "tuyệt đối không trả tiền hai lần".
- **Redlock:** thêm chi phí 5 node Redis, vẫn không chống được process pause (lập luận của Kleppmann).

Kết luận: với tiền, **đừng để tính đúng đắn phụ thuộc vào distributed lock**. Lock chỉ giảm tranh chấp; đúng đắn phải nằm ở **tài nguyên đích** (DB constraint, CAS, fencing token).

---

## 5. Nguyên nhân gốc

1. **Tính đúng đắn đặt lên một lock dựa trên thời gian (lease)** trong khi process có thể bị dừng lâu hơn lease (GC pause, CPU throttling, node treo).
2. **Unlock không an toàn** (không so token).
3. **Mô hình "lấy hết rồi xử lý"** không có trạng thái trung gian `PROCESSING` → nhiều worker thấy cùng việc.
4. **Không có idempotency ở đích** (wallet credit không có khóa duy nhất theo `cashbackId`).
5. Xử lý khối lớn trong một persistence context → GC pause dài, là chất xúc tác.

---

## 6. Giải pháp

### 6.1 Ngắn hạn

1. Dừng job (feature flag), chạy đối soát và thu hồi 7.792 khoản trùng (trừ vào cashback lần sau, có thông báo).
2. Wallet-service: thêm cột `reference_id` + unique index `(source, reference_id)` cho bảng `wallet_transaction`; `POST /credits` nhận `referenceId` và trả kết quả cũ nếu trùng. Đây là **lớp chặn cuối** — kể cả khi job chạy trùng, tiền chỉ cộng một lần.
3. Reward job gửi `referenceId = "cashback:" + cashbackId`.
4. Xử lý theo trang 500 bản ghi, `EntityManager.clear()` sau mỗi trang, bỏ `EAGER`.

### 6.2 Dài hạn — claim theo bản ghi bằng `SKIP LOCKED`

Thay vì "một instance giữ lock toàn cục", mọi instance cùng làm việc, mỗi bản ghi được **claim** nguyên tử:

```sql
ALTER TABLE cashback ADD COLUMN claimed_by VARCHAR(64), ADD COLUMN claimed_until TIMESTAMPTZ,
                     ADD COLUMN attempt INT NOT NULL DEFAULT 0;     -- PG 11+: thêm cột có DEFAULT là metadata-only
CREATE INDEX CONCURRENTLY ix_cashback_due ON cashback (eligible_at) WHERE status = 'PENDING';
```

```java
@Component
@RequiredArgsConstructor
class CashbackClaimer {
    private final JdbcTemplate jdbc;

    // Transaction ngắn: claim rồi commit ngay — không giữ row lock trong lúc gọi wallet
    @Transactional
    public List<Claimed> claimBatch(String workerId, int size) {
        return jdbc.query("""
            WITH picked AS (
                SELECT id FROM cashback
                WHERE (status = 'PENDING' AND eligible_at <= now())
                   OR (status = 'PROCESSING' AND claimed_until < now())      -- thu hồi claim của worker chết
                ORDER BY eligible_at
                LIMIT ?
                FOR UPDATE SKIP LOCKED
            )
            UPDATE cashback c
               SET status = 'PROCESSING', claimed_by = ?, claimed_until = now() + interval '5 minutes',
                   attempt = c.attempt + 1
              FROM picked WHERE c.id = picked.id
            RETURNING c.id, c.customer_id, c.amount, c.attempt
            """, claimedMapper, size, workerId);
    }

    @Transactional
    public boolean markPaid(long id, String workerId, int attempt) {
        // fencing: chỉ worker đang giữ claim với đúng attempt mới được chốt trạng thái
        return jdbc.update("""
            UPDATE cashback SET status = 'PAID', paid_at = now()
            WHERE id = ? AND status = 'PROCESSING' AND claimed_by = ? AND attempt = ?
            """, id, workerId, attempt) == 1;
    }
}

@Component
@RequiredArgsConstructor
class CashbackPayoutJob {
    private final CashbackClaimer claimer;
    private final WalletClient wallet;
    private final String workerId = System.getenv("POD_NAME");

    @Scheduled(fixedDelay = 10_000)
    public void run() {
        List<Claimed> batch;
        while (!(batch = claimer.claimBatch(workerId, 200)).isEmpty()) {
            for (Claimed c : batch) {
                wallet.credit(c.customerId(), c.amount(), "cashback:" + c.id());   // idempotent theo referenceId
                if (!claimer.markPaid(c.id(), workerId, c.attempt())) {
                    log.warn("Mất claim cashback {} (attempt {}), worker khác đã tiếp quản", c.id(), c.attempt());
                }
            }
        }
    }
}
```

Phân tích an toàn:

- Hai worker không bao giờ claim cùng bản ghi cùng lúc: `FOR UPDATE SKIP LOCKED` + UPDATE trong cùng transaction.
- Worker chết/treo: sau `claimed_until` bản ghi được claim lại với `attempt + 1`. Worker cũ tỉnh dậy sau GC pause: lời gọi wallet trùng bị chặn bởi `referenceId` unique; `markPaid` với `attempt` cũ trả 0 dòng — `attempt` đóng vai trò **fencing token**.
- Throughput: 4 pod × 200 bản ghi/batch song song; 180.000 bản ghi xong trong ~6 phút thay vì 400 s trên một pod và tăng tuyến tính theo số pod (giới hạn bởi wallet-service).

### 6.3 Khi thật sự cần "chỉ một instance chạy job"

Ví dụ job tổng hợp báo cáo cuối ngày (không chia nhỏ được): **ShedLock** là lựa chọn hợp lý, kèm job idempotent.

```java
@Scheduled(cron = "0 30 0 * * *", zone = "Asia/Ho_Chi_Minh")
@SchedulerLock(name = "dailySettlementReport", lockAtMostFor = "PT50M", lockAtLeastFor = "PT5M")
public void dailyReport() {
    LockAssert.assertLocked();
    reportService.generateFor(LocalDate.now().minusDays(1));   // UPSERT theo (report_date) → chạy lại an toàn
}

@Bean
LockProvider lockProvider(DataSource ds) {
    return new JdbcTemplateLockProvider(JdbcTemplateLockProvider.Configuration.builder()
            .withJdbcTemplate(new JdbcTemplate(ds)).usingDbTime().build());   // dùng giờ DB, tránh lệch đồng hồ
}
```

Fencing token với Redis (khi tài nguyên đích hỗ trợ điều kiện):

```java
long token = redis.opsForValue().increment("fence:inventory-sync");   // tăng đơn điệu mỗi lần lấy lock
// ... gửi token kèm mọi lần ghi:
// UPDATE inventory SET qty=?, fence=? WHERE sku=? AND fence < ?
```

### 6.4 So sánh các phương án

| Phương án | Chống chạy trùng job | Chống ghi trùng khi process pause | Scale ngang xử lý | Hạ tầng | Ghi chú |
|---|---|---|---|---|---|
| Redis `SET NX` TTL cố định | Một phần | Không | Không | Redis | Phải có token + Lua unlock |
| Redisson watchdog | Tốt khi job dài | Không (watchdog cũng bị STW) | Không | Redis | Tiện, reentrant |
| ShedLock (JDBC/Redis) | Tốt | Không | Không | Bảng `shedlock` | Chuẩn cho cron job đơn |
| Quartz cluster mode | Tốt | Không | Phân phối job | Bảng Quartz | Nặng, nhiều tính năng lịch |
| K8s `CronJob` (`concurrencyPolicy: Forbid`) | Tốt | Không | Không | K8s | Tách job khỏi service; cẩn thận `startingDeadlineSeconds` |
| Fencing token ở đích | — | **Có** | — | Đích phải hỗ trợ CAS | Bổ sung cho lock |
| Claim `SKIP LOCKED` + lease + attempt | Không cần lock toàn cục | **Có** (kèm idempotent đích) | **Có** | Chỉ DB | Lựa chọn cho xử lý theo bản ghi |
| Unique constraint/idempotency ở đích | — | **Có** | — | DB | Hàng phòng thủ cuối, luôn nên có |

---

## 7. Phòng ngừa

**Monitoring & alert**

- Metric GC pause max (`jvm_gc_pause_seconds_max`): alert > 2 s; Full GC bất kỳ ở service xử lý tiền là alert.
- Metric job: số bản ghi claim/paid/lost-claim mỗi lần chạy, tuổi bản ghi PENDING lâu nhất (alert > 30 phút), số bản ghi `PROCESSING` quá hạn.
- Đối soát hằng ngày: tổng cashback PAID vs tổng `wallet_transaction` nguồn cashback.

**Test**

- Integration test (Testcontainers Postgres): 4 worker thread song song, 10.000 bản ghi → mỗi bản ghi đúng 1 lần credit (WireMock đếm theo referenceId).
- Test "zombie worker": worker A claim, giả lập pause vượt `claimed_until` (đặt thời gian claim ngắn), worker B claim lại và trả; A tỉnh dậy → credit trùng bị chặn, `markPaid` của A trả false.

**Checklist review cho job/lock**

- [ ] Lock để **hiệu quả** hay **đúng đắn**? Nếu đúng đắn: tài nguyên đích có constraint/fencing không?
- [ ] Unlock có so token (Lua) và nằm trong `finally`?
- [ ] Lease có dài hơn thời gian chạy xấu nhất? Điều gì xảy ra khi process dừng lâu hơn lease?
- [ ] Job có xử lý theo trang, bộ nhớ có giới hạn?
- [ ] Job chạy lại (rerun) có an toàn?

---

## 8. Cách kể lại trong phỏng vấn (STAR)

- **Situation:** "Job trả cashback chạy `@Scheduled` trên 4 pod, được bảo vệ bằng Redis SETNX TTL 120 giây. Sau đợt sale, 180 nghìn bản ghi đến hạn cùng lúc; hơn 7 nghìn khách được cộng tiền 2–3 lần, khoảng 412 triệu đồng."
- **Task:** "Mình được giao tìm nguyên nhân, thu hồi khoản trùng và thiết kế lại cho đúng ngay cả khi scale."
- **Action:** "Ghép log với GC log, mình thấy pod-a load cả 181 nghìn entity, dính Full GC 71 giây; lock hết hạn trong lúc nó đứng hình, pod-c lấy lock và chạy cùng tập dữ liệu; pod-a tỉnh dậy vẫn trả tiếp; thêm nữa unlock không so token nên còn xóa nhầm lock của pod khác. Mình chỉ ra Redisson watchdog hay ShedLock không chữa được vì watchdog cũng bị dừng khi STW. Giải pháp: wallet nhận referenceId với unique constraint làm lớp chặn cuối; job chuyển sang claim từng batch bằng `FOR UPDATE SKIP LOCKED` với lease và attempt làm fencing token; xử lý theo trang để hết GC pause."
- **Result:** "Không còn khoản trùng nào trong 8 tháng tiếp theo; đợt sale sau 210 nghìn bản ghi xử lý trong 7 phút với 4 pod, và đã có lần pod bị OOMKill giữa chừng mà không ảnh hưởng. Bài học: distributed lock chỉ để tiết kiệm công sức; tính đúng đắn phải nằm ở nơi dữ liệu được ghi."

---

## 9. Câu hỏi mở rộng

<details>
<summary>1. `SKIP LOCKED` có trên những DB nào? Khác gì `NOWAIT`?</summary>

PostgreSQL 9.5+, MySQL 8.0+, Oracle (từ lâu, `FOR UPDATE SKIP LOCKED`), SQL Server dùng hint `READPAST`. `SKIP LOCKED` bỏ qua dòng đang bị khóa và lấy dòng khác — phù hợp hàng đợi công việc. `NOWAIT` báo lỗi ngay nếu gặp dòng bị khóa — phù hợp khi muốn fail fast thay vì chờ.
</details>

<details>
<summary>2. Vì sao không giữ transaction (và row lock) suốt thời gian gọi wallet?</summary>

Giữ transaction trong lúc gọi mạng làm connection DB bị chiếm lâu (cạn pool), row lock giữ lâu, và nếu pod chết thì transaction rollback nhưng lời gọi wallet có thể đã thành công → vẫn cần idempotency. Claim bằng transaction ngắn + trạng thái PROCESSING + lease là mô hình bền hơn.
</details>

<details>
<summary>3. `lockAtMostFor` và `lockAtLeastFor` trong ShedLock nghĩa là gì?</summary>

`lockAtMostFor`: thời gian tối đa giữ lock nếu node giữ lock chết (an toàn khi crash) — phải lớn hơn thời gian chạy bình thường, nếu không node khác lấy lock khi job còn chạy. `lockAtLeastFor`: giữ lock tối thiểu bấy nhiêu kể cả khi job xong sớm — chống trường hợp các node lệch đồng hồ vài giây và cùng chạy một lịch cron.
</details>

<details>
<summary>4. Fencing token với Redis lấy từ đâu? Redlock có cung cấp không?</summary>

Redlock không sinh token tăng đơn điệu. Có thể dùng `INCR` một key riêng khi lấy lock (nhưng không nguyên tử với lock trên nhiều node), hoặc dùng hệ thống đồng thuận: etcd (revision tăng đơn điệu), ZooKeeper (zxid/sequence của znode). Điều kiện tiên quyết: tài nguyên đích phải kiểm tra token (CAS), nếu không token vô dụng.
</details>

<details>
<summary>5. Nếu dùng Kubernetes CronJob thay vì `@Scheduled` thì sao?</summary>

`concurrencyPolicy: Forbid` ngăn hai lần chạy chồng nhau do CronJob tạo, nhưng K8s chỉ đảm bảo "khoảng" một lần — có trường hợp hiếm tạo hai Job, hoặc Job bị retry (`backoffLimit`) khi pod chết giữa chừng. Do đó job vẫn phải idempotent. Lợi ích: tách tài nguyên khỏi service phục vụ API, không phụ thuộc số replica.
</details>

<details>
<summary>6. Virtual threads có giúp job này không?</summary>

Có thể tăng song song khi gọi wallet (I/O-bound) — ví dụ xử lý 200 bản ghi trong batch đồng thời bằng `Executors.newVirtualThreadPerTaskExecutor()` với semaphore giới hạn theo năng lực wallet. Nhưng không thay đổi gì về tính đúng đắn: claim, lease, fencing và idempotency vẫn cần y nguyên.
</details>

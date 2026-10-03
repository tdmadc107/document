# Case 04 — Dữ liệu được lưu "một nửa": `@Transactional` không hoạt động như bạn nghĩ

> **Chủ đề:** Spring transaction proxy, self-invocation, rollback rules, propagation `REQUIRES_NEW`, side effect trong transaction
> **Module liên quan:** [M09 §11 — Spring Transactions chuyên sâu](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#11-transactions) · [M07 §7 — Spring AOP, proxy, self-invocation](../01-giao-trinh/07-spring-core-boot.md#p7) · [M09 §2 — HikariCP & pool deadlock do REQUIRES_NEW](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#2-hikaricp) · [M09 §3 — Persistence context, flush](../01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md#3-persistence-context) · [M14 §5 — Outbox, idempotency](../01-giao-trinh/14-microservices-system-design.md#p5) · [M15 §7 — Testcontainers](../01-giao-trinh/15-testing.md#p7)
> **Độ khó:** ⭐⭐⭐ (Senior)
> **Thời gian tự giải gợi ý:** 30 phút

---

## 1. Bối cảnh hệ thống

`billing-service` của một nhà cung cấp phần mềm SaaS B2B, mỗi đầu tháng sinh hóa đơn cho ~42.000 doanh nghiệp.

| Thành phần | Chi tiết |
|---|---|
| Stack | Java 17, Spring Boot 3.1, Spring Data JPA (Hibernate 6.2), PostgreSQL 14, HikariCP (pool 20) |
| Job | `@Scheduled(cron = "0 0 1 1 * *")` — 01:00 ngày 1 hằng tháng, chạy trên 1 pod (ShedLock) |
| Nghiệp vụ mỗi hóa đơn | (1) tạo `invoice` + `invoice_line` (2) render PDF (3) trừ số dư trả trước `customer_balance` + ghi `balance_ledger` (4) đánh dấu `ISSUED` |
| Quy mô | 42.000 hóa đơn/tháng, trung bình 6 dòng/hóa đơn; job chạy ~35 phút |

## 2. Triệu chứng

**Ngày 3, đối soát tài chính báo:**
```
[RECON-ALERT] billing 2026-09
  invoices DRAFT without any line .................. 1,284
  invoices DRAFT with lines, no PDF, not debited ...... 211
  balance debited but invoice not ISSUED ............... 37    (tổng 412,6 triệu VNĐ)
```

**Truy vấn kiểm chứng:**
```sql
-- Hóa đơn không có dòng nào
SELECT i.status, count(*)
FROM invoice i
WHERE i.period = '2026-09'
  AND NOT EXISTS (SELECT 1 FROM invoice_line l WHERE l.invoice_id = i.id)
GROUP BY i.status;
--  status | count
--  DRAFT  |  1284

-- Đã trừ tiền nhưng không có hóa đơn ISSUED tương ứng
SELECT b.customer_id, b.amount, b.invoice_no, i.status
FROM balance_ledger b
LEFT JOIN invoice i ON i.invoice_no = b.invoice_no
WHERE b.period = '2026-09' AND b.type = 'DEBIT'
  AND (i.id IS NULL OR i.status <> 'ISSUED');
--  37 rows; cả 37 hóa đơn đều đang DRAFT (khách hàng không nhận được hóa đơn)
```

**Log của job (mức WARN/ERROR):**
```
01:07:12.441 ERROR [billing-job] c.a.b.InvoiceJob : Invoice for customer 33017 failed
org.springframework.dao.DataIntegrityViolationException: could not execute statement [ERROR: new row for relation "invoice_line"
  violates check constraint "invoice_line_quantity_positive"] ...
01:19:45.102 ERROR [billing-job] c.a.b.InvoiceJob : Invoice for customer 8120 failed
com.acme.billing.pdf.PdfRenderException: Template 'invoice-v7' missing font 'NotoSans-Bold'
01:24:03.557 ERROR [billing-job] c.a.b.InvoiceJob : Invoice for customer 12550 failed
org.springframework.dao.CannotAcquireLockException: could not execute statement [ERROR: canceling statement due to lock timeout]
```

**Khách hàng:** 37 doanh nghiệp gọi tổng đài "bị trừ tiền nhưng không nhận được hóa đơn".

## 3. Câu hỏi đặt ra

1. Code có `@Transactional`, vậy vì sao header được lưu còn line thì không?
2. Vì sao một hóa đơn lỗi render PDF vẫn được ghi xuống DB?
3. Vì sao tiền bị trừ trong khi hóa đơn **không tồn tại**?
4. Bạn chứng minh transaction có/không hoạt động bằng cách nào — không đoán?
5. Sửa code, sửa dữ liệu, và ngăn tái diễn thế nào?

> ✋ **Dừng lại và tự giải trước.** Ba triệu chứng → ít nhất ba lỗi khác nhau. Hãy liệt kê 5 trường hợp `@Transactional` "im lặng không có tác dụng" mà bạn biết.

## 4. Điều tra từng bước

### Bước 1 — Đọc code đường đi chính
```java
@Component
class InvoiceJob {
    @Scheduled(cron = "0 0 1 1 * *")
    @SchedulerLock(name = "monthly-invoice")
    void run() { invoiceService.generateMonthly(YearMonth.now().minusMonths(1)); }
}
```
```java
@Service
public class InvoiceService {
    public void generateMonthly(YearMonth period) {                 // không có @Transactional
        for (Long customerId : customerRepo.findBillableIds(period)) {
            try {
                createInvoice(customerId, period);                   // gọi nội bộ
            } catch (Exception e) {
                log.error("Invoice for customer {} failed", customerId, e);
            }
        }
    }

    @Transactional
    public void createInvoice(Long customerId, YearMonth period) throws PdfRenderException { ... }
}
```
Nghi vấn đầu tiên: `createInvoice` được gọi qua `this` → không đi qua proxy.

### Bước 2 — Bằng chứng, không đoán: bật log transaction
Chạy lại job cho 3 khách hàng trên staging (dữ liệu clone) với:
```yaml
logging.level:
  org.springframework.orm.jpa.JpaTransactionManager: DEBUG
  org.springframework.transaction.interceptor: TRACE
```
```
# customer 33017 — dòng usage lỗi
DEBUG JpaTransactionManager : Creating new transaction with name [org.springframework.data.jpa.repository.support.SimpleJpaRepository.save]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT
DEBUG JpaTransactionManager : Committing JPA transaction on EntityManager [SessionImpl(1781163522<open>)]      ← header COMMIT riêng
DEBUG JpaTransactionManager : Creating new transaction with name [org.springframework.data.jpa.repository.support.SimpleJpaRepository.saveAll]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT
DEBUG JpaTransactionManager : Initiating transaction rollback                                                  ← chỉ lines ROLLBACK
# customer 12550 — lock timeout ở bước cuối
DEBUG JpaTransactionManager : Creating new transaction with name [...SimpleJpaRepository.save]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT
DEBUG JpaTransactionManager : Committing JPA transaction on EntityManager [SessionImpl(55120934<open>)]
DEBUG JpaTransactionManager : Creating new transaction with name [...SimpleJpaRepository.saveAll]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT
DEBUG JpaTransactionManager : Committing JPA transaction on EntityManager [SessionImpl(901277415<open>)]
DEBUG JpaTransactionManager : Creating new transaction with name [com.acme.billing.BalanceService.debit]: PROPAGATION_REQUIRES_NEW,ISOLATION_DEFAULT
DEBUG JpaTransactionManager : Committing JPA transaction on EntityManager [SessionImpl(402114877<open>)]       ← debit COMMIT
DEBUG JpaTransactionManager : Creating new transaction with name [...SimpleJpaRepository.save]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT
DEBUG JpaTransactionManager : Initiating transaction rollback                                                  ← markIssued thất bại
```
Không hề có dòng `Creating new transaction with name [com.acme.billing.InvoiceService.createInvoice]`, và khi vào `debit` (REQUIRES_NEW) cũng không có dòng `Suspending current transaction` — nghĩa là lúc đó **không có transaction nào để tạm treo**. Mỗi `repository.save()` tự mở và commit transaction của chính nó (`SimpleJpaRepository` có `@Transactional` ở mức class). Đây là **xác nhận** self-invocation.

Cách kiểm tra nhanh khác — đặt tạm trong method:
```java
log.info("tx active = {}", TransactionSynchronizationManager.isActualTransactionActive());   // in ra false
```

### Bước 3 — Vì sao lỗi PDF vẫn commit (sau khi giả định đã sửa self-invocation)?
Trên nhánh thử nghiệm, tách `createInvoice` sang bean khác để transaction có hiệu lực, rồi cho render PDF lỗi:
```
TRACE TransactionInterceptor : Completing transaction for [com.acme.billing.InvoiceWriter.createInvoice] after exception: com.acme.billing.pdf.PdfRenderException: Template 'invoice-v7' missing font ...
DEBUG JpaTransactionManager  : Initiating transaction commit                     ← COMMIT dù có exception!
```
`PdfRenderException extends Exception` (checked). Quy tắc mặc định của Spring: **chỉ rollback với `RuntimeException` và `Error`**; checked exception → commit.

### Bước 4 — Vì sao tiền bị trừ nhưng hóa đơn không được phát hành? Sửa (1) đã đủ chưa?
```java
@Service
public class BalanceService {
    @Transactional(propagation = Propagation.REQUIRES_NEW)    // "để giữ lock balance ngắn nhất có thể"
    public void debit(Long customerId, BigDecimal amount, String invoiceNo) { ... }
}
```
Với code hiện tại (không có transaction ngoài), debit commit ngay rồi bước `markIssued` gặp lock timeout → 37 hóa đơn kẹt `DRAFT` nhưng tiền đã trừ; khớp với 37 dòng log `CannotAcquireLockException`.

Câu hỏi quan trọng: **chỉ sửa self-invocation có đủ không?** Không. `REQUIRES_NEW` tạm treo transaction ngoài, mở transaction mới trên **connection khác**, và **commit ngay** khi method kết thúc. Trên nhánh thử nghiệm đã sửa (1), test lỗi ở bước `markIssued` cho kết quả còn tệ hơn: toàn bộ hóa đơn rollback (không còn dấu vết) nhưng khoản trừ vẫn nằm trong `balance_ledger`.

### Bước 5 — Tái hiện bằng integration test (Testcontainers)
```java
@SpringBootTest
@Testcontainers
class InvoiceAtomicityIT {
    @Container @ServiceConnection
    static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:14");

    @Test
    void whenLineInsertFails_nothingIsPersisted() {
        givenCustomerWithInvalidUsage(33017L);                       // quantity = 0 → vi phạm CHECK constraint
        invoiceService.generateMonthly(YearMonth.of(2026, 9));
        assertThat(invoiceRepo.countByCustomerId(33017L)).isZero();  // ĐỎ với code hiện tại: = 1
    }

    @Test
    void whenIssueStepFails_balanceIsNotDebited() {
        givenRowLockHeldOnInvoiceTable(12550L);                      // giữ lock để markIssued bị lock timeout
        invoiceService.generateMonthly(YearMonth.of(2026, 9));
        assertThat(ledgerRepo.countByCustomerId(12550L)).isZero();   // ĐỎ: = 1 (debit đã commit)
    }
}
```
Lưu ý: **không** đặt `@Transactional` trên class test — nó bọc mọi thứ trong một transaction rollback cuối test, che đi chính lỗi cần chứng minh.

## 5. Nguyên nhân gốc

Ba lỗi độc lập, cùng một gốc: **hiểu sai cơ chế proxy và ranh giới transaction**.

```java
@Service
public class InvoiceService {
    public void generateMonthly(YearMonth period) {
        for (Long id : customerRepo.findBillableIds(period)) {
            try { createInvoice(id, period); }                       // ❌ (1) self-invocation: this.createInvoice → bỏ qua proxy
            catch (Exception e) { log.error(...); }
        }
    }

    @Transactional                                                    // ❌ (2) mặc định không rollback checked exception
    public void createInvoice(Long customerId, YearMonth period) throws PdfRenderException {
        Invoice inv = invoiceRepo.save(Invoice.draft(customerId, period));        // commit riêng (do (1))
        lineRepo.saveAll(usageCalculator.lines(inv));                            // lỗi ở đây → header đã nằm trong DB
        inv.setPdfUrl(pdfRenderer.render(inv));                                  // gọi service render 300–800 ms; ném checked exception
        balanceService.debit(customerId, inv.total(), inv.getInvoiceNo());       // ❌ (3) REQUIRES_NEW: commit độc lập
        inv.markIssued();
        invoiceRepo.save(inv);                                                   // lock timeout ở đây → debit đã commit
    }
}
```
| Triệu chứng | Lỗi | Cơ chế |
|---|---|---|
| 1.284 hóa đơn `DRAFT` không có dòng | (1) self-invocation | Không có transaction bao ngoài; mỗi `save` tự commit |
| 211 hóa đơn có dòng nhưng không PDF | (1) + (2) checked exception | Hiện tại do (1); kể cả khi sửa (1), `PdfRenderException` (checked) vẫn khiến Spring **commit** header + lines |
| 37 khoản trừ với hóa đơn `DRAFT` | (1) + (3) `REQUIRES_NEW` | Debit commit trước; sửa (1) mà giữ (3) thì hóa đơn rollback sạch còn debit vẫn ở lại |

Các trường hợp `@Transactional` bị bỏ qua âm thầm khác (đội đã rà soát toàn codebase): method `private` (proxy không override được; Spring 6 hỗ trợ `protected`/package-private với CGLIB, trước đó chỉ `public`), method `final`/`static`, object tạo bằng `new`, gọi trong `@PostConstruct`, và exception bị `catch` rồi nuốt bên trong method.

## 6. Giải pháp

### Ngắn hạn
1. Dừng job tháng sau cho đến khi có bản sửa (bật cờ `billing.monthly-job.enabled=false`).
2. **Sửa dữ liệu** bằng script có review hai người, chạy trong transaction, ghi audit:
   - Hoàn 37 khoản debit (`balance_ledger` type `REVERSAL` tham chiếu `invoice_no`, cộng lại `customer_balance`) và gửi thư xin lỗi.
   - Xóa 1.284 hóa đơn `DRAFT` rỗng, chạy lại job cho đúng các khách hàng đó sau khi deploy bản sửa.
   - 211 hóa đơn có dòng nhưng chưa PDF/chưa trừ tiền: xóa và chạy lại cùng nhóm trên.

### Dài hạn — thiết kế lại ranh giới transaction
```java
@Service
@RequiredArgsConstructor
public class InvoiceService {                                     // điều phối, KHÔNG transaction
    private final InvoiceWriter writer;                           // bean khác → lời gọi đi qua proxy
    private final ApplicationEventPublisher events;

    public BatchResult generateMonthly(YearMonth period) {
        var result = new BatchResult();
        for (Long id : customerRepo.findBillableIds(period)) {
            try {
                writer.createInvoice(id, period);                 // ✅ mỗi hóa đơn một transaction
                result.ok(id);
            } catch (RuntimeException e) {
                result.failed(id, e);                             // một hóa đơn lỗi không kéo theo 42.000 hóa đơn khác
            }
        }
        return result;
    }
}

@Service
@RequiredArgsConstructor
class InvoiceWriter {
    @Transactional(rollbackFor = Exception.class, timeout = 10)   // ✅ rollback cả checked; timeout làm lưới an toàn
    public void createInvoice(Long customerId, YearMonth period) {
        Invoice inv = invoiceRepo.save(Invoice.draft(customerId, period));
        lineRepo.saveAll(usageCalculator.lines(inv));
        balanceService.debit(customerId, inv.total(), inv.getInvoiceNo());     // ✅ REQUIRED: cùng transaction
        inv.markIssued();
        events.publishEvent(new InvoiceIssued(inv.getId()));                    // PDF render sau commit
    }
}

@Service
class BalanceService {
    @Transactional(propagation = Propagation.MANDATORY)          // ✅ bắt buộc chạy trong transaction của caller
    public void debit(Long customerId, BigDecimal amount, String invoiceNo) {
        int updated = balanceRepo.debitIfEnough(customerId, amount);   // UPDATE ... SET amount = amount - ? WHERE ... AND amount >= ?
        if (updated == 0) throw new InsufficientBalanceException(customerId);
        ledgerRepo.save(LedgerEntry.debit(customerId, amount, invoiceNo));
    }
}

@Component
class InvoicePdfListener {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void on(InvoiceIssued e) { pdfJobQueue.enqueue(e.invoiceId()); }       // retry độc lập, không ảnh hưởng dữ liệu tiền
}
```
Điểm mấu chốt:
- **Debit không cần `REQUIRES_NEW` để "giữ lock ngắn"**: dùng một câu `UPDATE` nguyên tử và đưa nó về **cuối** transaction thì lock chỉ giữ vài ms.
- **PDF ra khỏi transaction**: gọi service chậm trong transaction giữ connection và lock (xem case 08). `AFTER_COMMIT` đảm bảo chỉ render hóa đơn đã thật sự tồn tại. Nếu cần chắc chắn không mất sự kiện khi pod chết đúng lúc sau commit, dùng **transactional outbox** (bảng `pdf_job` ghi trong cùng transaction) thay vì event trong bộ nhớ.
- `PdfRenderException` đổi thành unchecked theo quy ước của đội: exception nghiệp vụ/hạ tầng là `RuntimeException`; ngoài ra `rollbackFor = Exception.class` là lưới an toàn.

### So sánh cách sửa self-invocation
| Cách | Ưu | Nhược |
|---|---|---|
| **Tách sang bean khác** | Rõ ràng, đúng trách nhiệm, dễ test | Thêm một class |
| Self-injection (`@Lazy @Autowired InvoiceService self`) | Sửa nhanh | Khó hiểu với người đọc sau, dễ bị "dọn dẹp" nhầm, vòng phụ thuộc |
| `TransactionTemplate` (programmatic) | Ranh giới hiển thị ngay trong code, hợp cho batch theo chunk | Dài dòng hơn; trộn hạ tầng vào logic |
| AspectJ weaving (`mode = ASPECTJ`) | Áp dụng cả self-invocation, private | Cần cấu hình build/agent, ít đội quen |

### So sánh độ hạt transaction cho batch
| Lựa chọn | Ưu | Nhược |
|---|---|---|
| Một transaction cho cả 42.000 hóa đơn | "Tất cả hoặc không" | Transaction 35 phút: giữ lock, bloat MVCC, một lỗi rollback tất cả, persistence context phình to |
| **Mỗi hóa đơn một transaction** | Lỗi cô lập, lock ngắn | Cần cơ chế chạy lại cho phần lỗi (idempotent theo `customerId + period`) |
| Chunk 100 hóa đơn/transaction (`TransactionTemplate`/Spring Batch) | Throughput cao hơn (ít commit) | Một hóa đơn lỗi làm cả chunk rollback → cần retry từng phần tử (skip policy) |

Idempotency của job: unique constraint `(customer_id, period)` trên `invoice` → chạy lại job không tạo hóa đơn trùng.

## 7. Phòng ngừa

- **Test**: integration test với Testcontainers cho mọi use case ghi nhiều bảng, assert **trạng thái DB sau lỗi** (không chỉ happy path); không đặt `@Transactional` lên test kiểu này.
- **ArchUnit**:
```java
@ArchTest
static final ArchRule noTransactionalOnPrivate = methods()
        .that().areAnnotatedWith(Transactional.class)
        .should().notHaveModifier(JavaModifier.PRIVATE)
        .andShould().notHaveModifier(JavaModifier.FINAL);

@ArchTest
static final ArchRule noRequiresNewOutsideAudit = noMethods()
        .that().areDeclaredInClassesThat().resideOutsideOfPackage("..audit..")
        .should(beAnnotatedWithRequiresNew())    // custom ArchCondition kiểm tra propagation
        .because("REQUIRES_NEW chỉ dùng cho audit/log lỗi, phải được review riêng");
```
- **Runtime guard**: method chỉ được gọi trong transaction dùng `Propagation.MANDATORY` → sai là nổ ngay (`IllegalTransactionStateException`) thay vì âm thầm.
- **Đối soát tự động hằng ngày** (đã có và nhờ nó mới phát hiện) — nâng thành chạy ngay sau job, alert trong 1 giờ thay vì 3 ngày.
- **Code review checklist**:
  - [ ] Method `@Transactional` có được gọi từ **bên ngoài** bean không? Có `public` (hoặc không `private`/`final`) không?
  - [ ] Exception nào có thể ném ra? Checked exception có cần `rollbackFor` không? Có `catch` nuốt exception bên trong không?
  - [ ] Có gọi HTTP/gửi email/Kafka/render file **trong** transaction không? → chuyển sang after-commit/outbox.
  - [ ] `REQUIRES_NEW`: có lý do nghiệp vụ (audit độc lập) không? Đã tính connection thứ hai và nguy cơ pool deadlock chưa?
  - [ ] Batch: độ hạt transaction là gì, chạy lại có idempotent không?

## 8. Cách kể lại trong phỏng vấn (STAR, ~1,5 phút)

- **S:** "Job sinh hóa đơn tháng của chúng tôi tạo ~42.000 hóa đơn. Đối soát ngày 3 phát hiện 1.284 hóa đơn không có dòng nào và 37 khách hàng bị trừ tiền nhưng hóa đơn không được phát hành — tổng hơn 400 triệu."
- **T:** "Tôi được giao tìm nguyên nhân, sửa dữ liệu và đảm bảo tháng sau không lặp lại."
- **A:** "Tôi không đoán mà bật log DEBUG của `JpaTransactionManager` trên staging: không có transaction nào cho method `createInvoice`, mỗi `save` tự commit — vì method được gọi nội bộ qua `this`, bỏ qua proxy. Khi tách ra bean riêng, tôi phát hiện thêm lỗi thứ hai: exception render PDF là checked nên Spring commit. Và lỗi thứ ba: `debit` dùng `REQUIRES_NEW` nên kể cả khi transaction ngoài rollback, khoản trừ vẫn commit — sửa lỗi thứ nhất mà không sửa lỗi này thì hóa đơn biến mất hẳn còn tiền vẫn bị trừ. Tôi viết integration test với Testcontainers tái hiện cả ba, sau đó thiết kế lại: mỗi hóa đơn một transaction ở bean riêng, `rollbackFor = Exception.class`, debit bằng `UPDATE` nguyên tử trong cùng transaction với `MANDATORY`, render PDF sau commit qua event. Dữ liệu được sửa bằng script có review và bút toán đảo."
- **R:** "Tháng sau job chạy 42.100 hóa đơn, đối soát 0 lệch, thời gian chạy giảm từ 35 xuống 22 phút vì PDF ra khỏi transaction. Chúng tôi thêm rule ArchUnit, đối soát ngay sau job, và mục transaction trong checklist review."

## 9. Câu hỏi mở rộng

<details>
<summary>1. Vì sao self-invocation bỏ qua <code>@Transactional</code>? JDK proxy và CGLIB có khác nhau không?</summary>

Spring áp dụng transaction bằng AOP proxy: bean được inject cho nơi khác là **proxy** bọc bean thật; logic mở/commit transaction nằm trong proxy. Khi bên trong bean gọi `this.createInvoice()`, `this` là object thật, không phải proxy → không có advice nào chạy. Điều này đúng với cả JDK dynamic proxy (dựa trên interface) lẫn CGLIB (subclass) — CGLIB proxy của Spring vẫn ủy quyền sang instance target riêng, không phải "gọi super". Chỉ AspectJ weaving (sửa bytecode của chính class) mới áp dụng cho self-invocation.
</details>

<details>
<summary>2. <code>REQUIRES_NEW</code> có thể gây deadlock pool như thế nào?</summary>

Transaction ngoài đang giữ một connection; `REQUIRES_NEW` cần **connection thứ hai** trong khi vẫn giữ connection thứ nhất. Nếu pool có 10 connection và 10 thread cùng ở trong transaction ngoài rồi cùng gọi method `REQUIRES_NEW`, không thread nào lấy được connection thứ hai → tất cả chờ đến `connectionTimeout` (`Connection is not available, request timed out after 30000ms`). Pool tối thiểu để không deadlock: `Tn × (Cm − 1) + 1`. Ngoài ra, nếu transaction trong cần khóa đúng dòng mà transaction ngoài đang giữ, nó sẽ chờ transaction ngoài — mà ngoài lại chờ trong kết thúc → tự deadlock (chỉ thoát nhờ lock timeout).
</details>

<details>
<summary>3. Khi nào thì <code>REQUIRES_NEW</code> là lựa chọn đúng?</summary>

Khi phần việc bên trong **phải** tồn tại độc lập với kết quả transaction ngoài: ghi audit/log lỗi ("đã thử thanh toán và thất bại"), cấp số chứng từ không được tái sử dụng, ghi trạng thái "đang xử lý" để job khác thấy. Kèm điều kiện: không đụng cùng dòng với transaction ngoài, pool đủ lớn, và chấp nhận rằng nếu transaction ngoài commit thất bại thì phần bên trong vẫn còn. Trường hợp cần "nếu lỗi thì chỉ bỏ phần này" trong cùng transaction thì là savepoint (`NESTED` với JDBC) chứ không phải `REQUIRES_NEW`.
</details>

<details>
<summary>4. <code>UnexpectedRollbackException</code> là gì và liên quan gì đến case này?</summary>

Nếu `createInvoice` gọi một method `@Transactional` (REQUIRED) khác ném `RuntimeException`, proxy của method trong sẽ đánh dấu transaction chung là **rollback-only**. Nếu method ngoài `catch` exception đó rồi kết thúc bình thường, khi commit Spring phát hiện cờ rollback-only, rollback và ném `UnexpectedRollbackException: Transaction silently rolled back because it has been marked as rollback-only`. Đây là kiểu bug ngược lại của case này: dev nghĩ đã "nuốt" lỗi để commit phần còn lại, nhưng không thể. Muốn phần trong độc lập thì phải thiết kế ranh giới rõ (REQUIRES_NEW có cân nhắc, hoặc không đặt `@Transactional` ở phần trong, hoặc xử lý sau commit).
</details>

<details>
<summary>5. <code>@Transactional(readOnly = true)</code> có ngăn được ghi dữ liệu không? Isolation khai báo ở method bên trong có hiệu lực không?</summary>

`readOnly` là gợi ý tối ưu: Hibernate đặt `FlushMode.MANUAL` và không giữ snapshot dirty-checking; driver có thể gọi `setReadOnly(true)` (PostgreSQL khi đó từ chối câu ghi). Nhưng nó không phải cơ chế bảo mật: `flush()` thủ công hoặc native update vẫn có thể chạy nếu DB không chặn. Isolation và readOnly chỉ áp dụng khi **bắt đầu** transaction mới — method `REQUIRED` tham gia transaction đã có thì khai báo của nó bị bỏ qua (bật `validateExistingTransaction` trên transaction manager để Spring báo lỗi khi không khớp).
</details>

<details>
<summary>6. Spring Boot 2 / Spring 5 có gì khác cần lưu ý?</summary>

Spring 5 chỉ áp dụng `@Transactional` cho method `public` (với proxy), method `protected`/package-private bị bỏ qua âm thầm; Spring 6 mở rộng cho non-private method với class-based proxy. `jakarta.transaction.Transactional` thay cho `javax.transaction.Transactional` (Boot 3) — cả hai đều được Spring hiểu, nhưng `rollbackOn` của JTA có ngữ nghĩa riêng. Spring Framework 6.2 thêm tùy chọn toàn cục `@EnableTransactionManagement(rollbackOn = RollbackOn.ALL_EXCEPTIONS)` — kiểm tra phiên bản trước khi dựa vào.
</details>

# Câu hỏi phỏng vấn — Câu hỏi hành vi & vai trò Senior

> Không có module giáo trình riêng. Phần chuẩn bị tương ứng: [Kế hoạch ôn tập — 8. Chuẩn bị ngoài kỹ thuật](../02-ke-hoach-on-tap.md#8-chuẩn-bị-ngoài-kỹ-thuật)

**Cách dùng:** đọc câu hỏi, **tự trả lời thành tiếng** bằng câu chuyện thật của bạn (bấm giờ: 1,5–3 phút mỗi câu), ghi âm lại rồi mới mở đáp án để so khung trả lời. Mỗi đáp án gồm: interviewer thực sự muốn đánh giá gì, khung trả lời, và một **ví dụ mẫu dạng dàn ý** — đó là *minh họa cấu trúc*, **không phải câu chuyện để học thuộc rồi kể như của mình**. Interviewer giỏi sẽ hỏi xoáy 3–4 tầng (số liệu, ai làm gì, vì sao không làm cách khác) và câu chuyện bịa sẽ lộ ngay.

**Ký hiệu mức độ:** 🟢 Cơ bản · 🟡 Senior · 🔴 Xoáy sâu (câu dễ trượt, hoặc hỏi để phân biệt Senior với Mid) · 🎬 Tình huống (scenario)

## Mục lục

1. [Giới thiệu bản thân & phương pháp STAR](#g1) — Q1–Q4
2. [Sự cố production & ownership](#g2) — Q5–Q8
3. [Bất đồng kỹ thuật & ra quyết định](#g3) — Q9–Q12
4. [Mentoring & code review](#g4) — Q13–Q16
5. [Ước lượng & deadline](#g5) — Q17–Q19
6. [Technical debt](#g6) — Q20–Q21
7. [Làm việc với BA, PO & stakeholder](#g7) — Q22–Q25
8. [Vai trò Senior & phát triển bản thân](#g8) — Q26–Q30
9. [Nghỉ việc, lương & offer — thị trường Việt Nam](#g9) — Q31–Q36
10. [Câu hỏi ngược cho interviewer](#g10) — Q37–Q38
11. [Tình huống tổng hợp](#g11) — Q39–Q40

## Chuẩn bị trước: "ngân hàng câu chuyện" (story bank)

Chuẩn bị sẵn **6–8 câu chuyện thật**, mỗi câu viết theo STAR trên một trang, rồi ánh xạ sang nhiều câu hỏi. Một câu chuyện tốt dùng được cho 3–4 câu hỏi khác nhau.

| Câu chuyện cần có | Dùng cho các câu hỏi |
|---|---|
| Một sự cố production bạn trực tiếp xử lý (có timeline, số liệu ảnh hưởng) | Q5, Q6, Q8, Q39 |
| Một quyết định kiến trúc/công nghệ có trade-off rõ ràng | Q3, Q11, Q12, Q29 |
| Một lần bất đồng kỹ thuật (với đồng nghiệp, lead, hoặc kiến trúc sư) | Q9, Q10, Q40 |
| Một lần mentor junior / thay đổi chất lượng code review của team | Q13, Q14, Q15, Q16 |
| Một lần tối ưu hiệu năng có số đo trước/sau | Q3, Q20, Q34 |
| Một dự án trễ hạn hoặc thất bại và bài học | Q4, Q18, Q19 |
| Một lần làm việc với requirement mơ hồ / stakeholder khó | Q22, Q23, Q25 |
| Một lần trả tech debt hoặc refactor hệ thống legacy | Q20, Q21 |

> 💡 **Quy tắc vàng:** số liệu cụ thể (thời gian, %, số user, QPS, số tiền) + vai trò **của chính bạn** ("tôi", không phải "chúng tôi") + bài học **đã được áp dụng lại** sau đó. Thiếu một trong ba thứ này thì câu chuyện chỉ ở mức Mid.

---

<a id="g1"></a>
## 1. Giới thiệu bản thân & phương pháp STAR

### Q1. 🟢 "Bạn giới thiệu ngắn về bản thân trong khoảng 2 phút."

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Đây là "trailer" định hướng cả buổi phỏng vấn. Cấu trúc **Hiện tại → Quá khứ nổi bật → Vì sao bạn ở đây**: vai trò hiện tại + quy mô hệ thống, 1–2 thành tích có số liệu, chuyên môn sâu, rồi kết nối với vị trí đang ứng tuyển. Không kể lại CV theo năm.

**Giải thích chi tiết:**

*Interviewer muốn đánh giá:* khả năng tóm tắt, trình bày mạch lạc; bạn tự định vị mình thế nào; và **chọn chủ đề để hỏi tiếp** — vì vậy hãy chủ động "cài" những chủ đề bạn mạnh.

*Khung trả lời (≈ 250–300 từ):*
1. **Hiện tại (30s):** chức danh, số năm kinh nghiệm, domain, quy mô (số user, QPS, dữ liệu, số service, team bao nhiêu người).
2. **Điểm nhấn (60s):** 1–2 thành tích có số liệu và vai trò của bạn.
3. **Thế mạnh kỹ thuật (20s):** Java/Spring, JVM tuning, thiết kế hệ thống phân tán… — chọn cái liên quan JD.
4. **Vì sao vị trí này (10–20s):** điều bạn tìm kiếm khớp với công ty.

*Ví dụ mẫu (dàn ý — thay bằng thông tin thật):*
> "Tôi có khoảng 7 năm làm backend Java, hiện là Senior trong team thanh toán của một công ty fintech, phụ trách nhóm service xử lý giao dịch khoảng vài triệu lượt mỗi ngày trên Spring Boot, PostgreSQL và Kafka. Hai việc tôi tâm đắc: dẫn dắt việc tách module đối soát khỏi monolith, giảm thời gian chạy batch từ vài giờ xuống dưới 30 phút; và xây quy trình postmortem giúp số sự cố lặp lại giảm rõ rệt. Tôi mạnh về JVM performance và thiết kế idempotency/outbox. Tôi đang tìm môi trường product có bài toán scale lớn hơn và nơi tôi được đóng góp nhiều hơn vào quyết định kiến trúc — đó là lý do tôi ứng tuyển vị trí này."

**Câu hỏi nối tiếp:**
- "Trong team đó, phần nào do chính bạn làm?" — Chuẩn bị tách bạch vai trò của mình.
- "Con số vài giờ xuống 30 phút đo thế nào?" — Phải nói được cách đo.

**⚠️ Câu trả lời gây điểm trừ:** đọc CV từ năm ra trường; nói quá 4 phút; toàn tính từ ("chăm chỉ, ham học hỏi") không có dữ kiện; nhắc chuyện cá nhân không liên quan; số liệu không trả lời được khi bị hỏi lại.

**📖 Ôn lại:** [8. Chuẩn bị ngoài kỹ thuật](../02-ke-hoach-on-tap.md#8-chuẩn-bị-ngoài-kỹ-thuật)

</details>

### Q2. 🟢 "STAR là gì? Vì sao câu trả lời hành vi nên theo STAR?"

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **S**ituation (bối cảnh) – **T**ask (nhiệm vụ/vai trò của bạn) – **A**ction (bạn đã làm gì, vì sao) – **R**esult (kết quả đo được + bài học). Câu hỏi hành vi dựa trên giả định "hành vi trong quá khứ dự báo hành vi tương lai"; STAR giúp câu trả lời cụ thể, kiểm chứng được và đúng trọng tâm. Tỉ lệ thời gian gợi ý: S+T 20%, **A 60%**, R 20%.

**Giải thích chi tiết:**

*Interviewer muốn đánh giá:* bạn có trả lời bằng **sự việc cụ thể** hay bằng lý thuyết "tôi sẽ…". Nhiều công ty có rubric chấm theo từng năng lực (ownership, collaboration, technical judgment…) và chỉ cho điểm khi có hành động cụ thể.

*Những lỗi STAR phổ biến:*
- S quá dài (kể 2 phút bối cảnh nghiệp vụ).
- A dùng "chúng tôi" — không rõ bạn đóng góp gì.
- R không có số liệu, hoặc thiếu **bài học** và **việc đã thay đổi sau đó**.
- Chọn câu chuyện quá nhỏ cho level Senior (sửa một bug đơn giản).

*Biến thể nâng cao:* **STAR+L** (thêm Learning) hoặc **CARL** (Context–Action–Result–Learning). Với Senior, phần "L" thường quyết định: bạn đã biến bài học thành quy trình, công cụ, checklist cho cả team chưa?

*Ví dụ mẫu (dàn ý):*
- **S:** API tra cứu đơn hàng p99 tăng lên vài giây sau đợt khuyến mãi.
- **T:** Tôi là người on-call và được giao tìm nguyên nhân gốc.
- **A:** Đọc metric → thấy connection pool cạn → bật slow query log → phát hiện truy vấn N+1 từ một thay đổi gần đây → thêm fetch join + index, viết test chặn N+1 bằng đếm số query.
- **R:** p99 về mức cũ; thêm rule review cho truy vấn ORM; viết tài liệu chia sẻ cho team.

**Câu hỏi nối tiếp:**
- "Nếu làm lại, bạn làm gì khác?" — Luôn chuẩn bị sẵn một điều.
- "Người khác trong team phản ứng thế nào?"

**⚠️ Câu trả lời gây điểm trừ:** trả lời bằng giả định "nếu gặp tôi sẽ…" khi câu hỏi là "hãy kể một lần…"; dùng "chúng tôi" từ đầu đến cuối; kết thúc ở Action mà không có Result.

**📖 Ôn lại:** [8. Chuẩn bị ngoài kỹ thuật](../02-ke-hoach-on-tap.md#8-chuẩn-bị-ngoài-kỹ-thuật)

</details>

### Q3. 🟡 "Kể về dự án/thành tựu kỹ thuật bạn tự hào nhất."

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Chọn dự án có **độ phức tạp kỹ thuật + tác động kinh doanh + vai trò dẫn dắt** của bạn. Trình bày: vấn đề và vì sao khó → các phương án đã cân nhắc và **trade-off** → quyết định của bạn → kết quả đo được → điều bạn sẽ làm khác.

**Giải thích chi tiết:**

*Interviewer muốn đánh giá:* chiều sâu kỹ thuật thật (họ sẽ đào sâu vào bất cứ chi tiết nào bạn nhắc tới), khả năng nhìn tác động kinh doanh, và **phạm vi ảnh hưởng** (bản thân → team → nhiều team) — thước đo chính phân biệt Senior.

*Khung trả lời:*
1. **Bối cảnh & vấn đề** — vì sao quan trọng với business (tiền, user, rủi ro).
2. **Ràng buộc** — thời gian, hệ thống legacy, nhân lực, không được downtime…
3. **Phương án & trade-off** — ít nhất 2 phương án, vì sao chọn cái này.
4. **Vai trò của bạn** — thiết kế, thuyết phục, chia việc, xử lý rủi ro.
5. **Kết quả** — số liệu trước/sau; tác động lâu dài.
6. **Nhìn lại** — điều chưa tốt.

*Ví dụ mẫu (dàn ý):* chuyển xử lý thông báo từ gọi đồng bộ sang Kafka + outbox pattern. Phương án cân nhắc: gọi REST có retry (mất message khi crash), dual-write DB + Kafka (không nhất quán), outbox + CDC (chọn — đánh đổi độ trễ vài trăm ms và thêm hạ tầng). Vai trò: viết design doc, làm PoC, chạy song song hai luồng một tuần để so sánh, lên kế hoạch rollback. Kết quả: hết mất thông báo, API đặt hàng giảm độ trễ do không chờ bên thứ ba. Nhìn lại: nên thêm giám sát lag consumer từ đầu.

**Câu hỏi nối tiếp:**
- "Vì sao không dùng giải pháp X?" — Chuẩn bị lý do loại từng phương án.
- "Nếu traffic tăng 10 lần thì thiết kế đó gãy ở đâu?"
- "Ai phản đối và bạn thuyết phục thế nào?"

**⚠️ Câu trả lời gây điểm trừ:** chọn dự án mà bạn chỉ làm một phần nhỏ; chỉ kể công nghệ ("dùng Kafka, Redis, K8s") không có vấn đề và trade-off; không trả lời được câu hỏi đào sâu về chính hệ thống mình kể.

**📖 Ôn lại:** [8. Chuẩn bị ngoài kỹ thuật](../02-ke-hoach-on-tap.md#8-chuẩn-bị-ngoài-kỹ-thuật) · [Module 14 — Phương pháp System Design Interview](../01-giao-trinh/14-microservices-system-design.md#p10)

</details>

### Q4. 🟡 "Kể về một lần bạn thất bại / mắc sai lầm lớn."

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Chọn một thất bại **thật, có hậu quả vừa phải, do bạn chịu trách nhiệm chính**, kể ngắn phần sai lầm, dành phần lớn thời gian cho: bạn phát hiện/khắc phục thế nào, **nhận trách nhiệm** ra sao, và **thay đổi cụ thể** bạn đã áp dụng để nó không lặp lại.

**Giải thích chi tiết:**

*Interviewer muốn đánh giá:* sự trung thực, khả năng tự nhìn nhận (self-awareness), tư duy "học từ lỗi" thay vì đổ lỗi, và mức độ trưởng thành.

*Khung trả lời:*
1. Sai lầm là gì — nói thẳng, không vòng vo.
2. Hậu quả (đo được).
3. Bạn làm gì ngay lúc đó (khắc phục, thông báo).
4. Nguyên nhân gốc — ở **quyết định** của bạn, không phải "do người khác".
5. Thay đổi lâu dài (thói quen, quy trình, công cụ) và bằng chứng nó có hiệu quả.

*Ví dụ mẫu (dàn ý):* ước lượng một tính năng tích hợp đối tác là 2 tuần mà không đọc kỹ tài liệu API của đối tác; thực tế cần cơ chế ký số và môi trường sandbox chậm → trễ 3 tuần, PO phải lùi kế hoạch marketing. Tôi báo sớm khi phát hiện ở tuần đầu, đề xuất cắt phạm vi phiên bản đầu. Bài học áp dụng: luôn có spike 1–2 ngày cho mọi tích hợp bên ngoài trước khi cam kết, và ước lượng theo khoảng (best/likely/worst).

**Câu hỏi nối tiếp:**
- "Ai bị ảnh hưởng và bạn đã nói gì với họ?"
- "Lần gần nhất bạn áp dụng bài học đó là khi nào?"

**⚠️ Câu trả lời gây điểm trừ:** "thất bại" giả (thật ra là khoe — "tôi quá cầu toàn"); đổ lỗi cho người khác/khách hàng; chọn sai lầm quá nghiêm trọng mà không có bài học thuyết phục (vd cố ý bỏ qua quy trình bảo mật); không có thay đổi sau đó.

**📖 Ôn lại:** [8. Chuẩn bị ngoài kỹ thuật](../02-ke-hoach-on-tap.md#8-chuẩn-bị-ngoài-kỹ-thuật)

</details>

---

<a id="g2"></a>
## 2. Sự cố production & ownership

### Q5. 🟡 "Kể về một sự cố production nghiêm trọng mà bạn đã xử lý."

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Kể theo vòng đời sự cố: **phát hiện → đánh giá mức độ → giảm thiểu (mitigate) trước → tìm nguyên nhân gốc → sửa triệt để → postmortem blameless → action items đã hoàn thành**. Nhấn mạnh: ưu tiên khôi phục dịch vụ (rollback, tắt feature flag) trước khi debug; giao tiếp định kỳ với stakeholder.

**Giải thích chi tiết:**

*Interviewer muốn đánh giá:* bình tĩnh dưới áp lực, tư duy có hệ thống khi chẩn đoán, biết dùng observability, ưu tiên đúng (khôi phục > tìm thủ phạm), giao tiếp, và văn hóa postmortem.

*Khung trả lời:*
1. **Tác động:** cái gì hỏng, bao nhiêu user/giao dịch, kéo dài bao lâu, mức độ (SEV1/SEV2).
2. **Phát hiện:** alert hay khách báo? (Nếu khách báo → bài học về monitoring.)
3. **Giảm thiểu:** hành động nhanh nhất để cầm máu — rollback, scale, chặn traffic, failover.
4. **Chẩn đoán:** metric → log → trace → thread dump/heap dump; giả thuyết và cách loại trừ.
5. **Nguyên nhân gốc** (dùng 5 Whys — thường là nguyên nhân hệ thống, không phải một người).
6. **Sửa triệt để + phòng ngừa:** test, alert, giới hạn, runbook.
7. **Giao tiếp:** cập nhật kênh sự cố mỗi 15–30 phút, thông báo cho CS/khách hàng.

*Ví dụ mẫu (dàn ý):* sau một lần deploy, service thanh toán bắt đầu timeout hàng loạt. Tôi là on-call: thấy thread pool của HTTP client cạn, đối tác phản hồi chậm → **rollback** không hết (lỗi nằm ở đối tác), nên bật circuit breaker + giảm timeout từ 30s xuống 3s qua config để giải phóng thread, các chức năng khác hồi phục sau ~10 phút. Nguyên nhân gốc: client gọi đối tác không có timeout hợp lý và dùng chung pool với các luồng khác (không có bulkhead). Action items: timeout/bulkhead cho mọi client ngoài, alert theo latency đối tác, game day giả lập đối tác chậm.

**Câu hỏi nối tiếp:**
- "Bạn biết dịch vụ đã hồi phục bằng cách nào?" — Metric/SLO cụ thể.
- "Postmortem có action item nào chưa làm? Vì sao?"
- "Nếu không rollback được thì sao?" — Feature flag, chuyển hướng traffic, degrade có kiểm soát.

**⚠️ Câu trả lời gây điểm trừ:** debug trên production hàng giờ trước khi nghĩ tới rollback; "hotfix thẳng lên prod không qua review"; đổ lỗi một cá nhân; không có action item phòng ngừa.

**📖 Ôn lại:** [Module 16 — Xử lý sự cố & văn hóa postmortem](../01-giao-trinh/16-devops-build-cloud-security.md#p12) · [Module 14 — Resilience](../01-giao-trinh/14-microservices-system-design.md#p4)

</details>

### Q6. 🔴 "Kể về một sự cố do chính bạn gây ra."

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Thừa nhận thẳng thắn ngay câu đầu, kể nhanh hành động khắc phục, rồi tập trung vào **vì sao hệ thống cho phép lỗi đó lọt qua** (thiếu test, thiếu review, thiếu guardrail) và bạn đã sửa **hệ thống** ra sao — không chỉ "tôi sẽ cẩn thận hơn".

**Giải thích chi tiết:**

*Interviewer muốn đánh giá:* trung thực, ownership, và mức trưởng thành: Mid nói "tôi sẽ cẩn thận hơn"; Senior nói "tôi đã thêm cơ chế để người khác cũng không mắc lỗi này".

*Khung trả lời:* Lỗi gì → phát hiện lúc nào → bạn báo cho ai và **bao lâu sau** (báo ngay là điểm cộng lớn) → khắc phục → nguyên nhân hệ thống → cải tiến → kết quả.

*Ví dụ mẫu (dàn ý):* tôi viết migration thêm cột `NOT NULL` có default cho bảng lớn, chạy tốt trên staging (dữ liệu nhỏ) nhưng trên production khóa bảng nhiều phút, API ghi đơn hàng timeout. Tôi chủ động báo trên kênh sự cố, cùng DBA dừng migration, khôi phục. Cải tiến: tách migration thành nhiều bước an toàn (thêm cột nullable → backfill theo lô → thêm constraint), thêm checklist "migration trên bảng lớn" vào PR template, staging được nạp dữ liệu có kích thước gần production cho các bảng chính.

**Câu hỏi nối tiếp:**
- "Sếp/đồng nghiệp phản ứng thế nào?"
- "Bạn cảm thấy thế nào lúc đó?" — Thừa nhận áp lực nhưng nói cách bạn giữ bình tĩnh.
- "Làm sao để văn hóa team khiến người ta dám báo lỗi của mình?" — Blameless postmortem.

**⚠️ Câu trả lời gây điểm trừ:** nói "tôi chưa từng gây sự cố" (thiếu trung thực hoặc thiếu kinh nghiệm); che giấu/âm thầm sửa; bài học chỉ là "cẩn thận hơn".

**📖 Ôn lại:** [Module 16 — Xử lý sự cố & văn hóa postmortem](../01-giao-trinh/16-devops-build-cloud-security.md#p12) · [Module 16 — Thiết kế pipeline CI/CD](../01-giao-trinh/16-devops-build-cloud-security.md#p8)

</details>

### Q7. 🟡 "Kể một lần bạn làm việc vượt ra ngoài phạm vi được giao (ownership)."

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Ownership = nhìn thấy vấn đề **không ai sở hữu** và tự đưa nó tới kết quả, kể cả khi nó không nằm trong ticket của bạn — nhưng **có phối hợp** (thông báo, xin ưu tiên), không âm thầm làm thay việc của team khác. Câu chuyện tốt có: vấn đề → vì sao không ai xử lý → bạn chủ động gì → kết quả cho team/công ty.

**Giải thích chi tiết:**

*Interviewer muốn đánh giá:* tính chủ động, tư duy như chủ sở hữu sản phẩm, đồng thời biết giới hạn (không "anh hùng cá nhân", không bỏ bê việc chính).

*Các dạng câu chuyện phù hợp:* build CI chậm 40 phút không ai sửa; alert ồn ào khiến on-call bỏ qua; tài liệu onboarding lạc hậu; một thư viện dùng chung có lỗ hổng bảo mật; một quy trình thủ công lặp lại hằng tuần.

*Ví dụ mẫu (dàn ý):* pipeline CI của monorepo mất ~40 phút, cả team than nhưng không ai được giao. Tôi đo thời gian từng bước, đề xuất với lead dành 20% thời gian trong 2 sprint; bật cache dependency, chạy test song song, tách integration test sang job riêng với Testcontainers reuse → còn ~12 phút. Tôi viết lại tài liệu và chuyển giao cho người phụ trách platform để có người sở hữu lâu dài.

**Câu hỏi nối tiếp:**
- "Việc chính của bạn có bị ảnh hưởng không?"
- "Nếu lead không đồng ý cho thời gian thì sao?" — Đưa số liệu chi phí (40 phút × số lần chạy × số người).

**⚠️ Câu trả lời gây điểm trừ:** tự ý thay đổi hệ thống của team khác không báo; làm thêm giờ triền miên như một "chiến tích"; việc làm thêm không có kết quả đo được.

**📖 Ôn lại:** [8. Chuẩn bị ngoài kỹ thuật](../02-ke-hoach-on-tap.md#8-chuẩn-bị-ngoài-kỹ-thuật) · [Module 16 — Thiết kế pipeline CI/CD](../01-giao-trinh/16-devops-build-cloud-security.md#p8)

</details>

### Q8. 🔴🎬 "2 giờ sáng, hệ thống thanh toán lỗi, lead không liên lạc được, bạn là on-call. Bạn làm gì trong 30 phút đầu?"

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Theo thứ tự: **xác nhận & đánh giá mức độ** (bao nhiêu % giao dịch lỗi) → **mở kênh sự cố, tuyên bố sự cố, gọi người theo escalation policy** (không chỉ một người lead) → **giảm thiểu bằng hành động an toàn, đảo ngược được** (rollback deploy gần nhất, tắt feature flag, failover) theo runbook → **ghi timeline** → cập nhật định kỳ. Không thử nghiệm sửa code trực tiếp trên production một mình.

**Giải thích chi tiết:**

*Interviewer muốn đánh giá:* ra quyết định khi thiếu thông tin và thiếu người, khả năng tự chủ nhưng an toàn, hiểu quy trình incident management.

*30 phút đầu, cụ thể:*
1. **Phút 0–5:** xác nhận alert là thật (dashboard, error rate, synthetic check). Xác định phạm vi: tất cả hay một phương thức thanh toán/đối tác?
2. **Phút 5–10:** tuyên bố sự cố trên kênh chuẩn; gọi theo escalation (lead dự phòng, engineering manager, on-call của team phụ thuộc — DB, hạ tầng, đối tác). Chỉ định ai giao tiếp với CS/business nếu có người.
3. **Phút 10–25:** "có gì thay đổi gần đây?" (deploy, config, migration, đối tác thông báo bảo trì). Nếu có deploy → rollback. Nếu do đối tác → bật fallback/chuyển cổng thanh toán dự phòng, hoặc tạm ẩn phương thức đó trên UI. Mọi hành động phải **đảo ngược được** và được ghi lại.
4. **Liên tục:** ghi timeline (giờ, hành động, kết quả); cập nhật trạng thái mỗi 15–30 phút kể cả khi chưa có tiến triển.
5. **Sau khi ổn định:** đối soát giao dịch bị ảnh hưởng (bị trừ tiền nhưng đơn chưa tạo?) — với thanh toán, **tính toàn vẹn dữ liệu** quan trọng ngang khôi phục dịch vụ.

**Câu hỏi nối tiếp:**
- "Rollback xong vẫn lỗi?" — Lỗi không đến từ deploy; chuyển giả thuyết sang hạ tầng/đối tác/dữ liệu.
- "Có nên retry hàng loạt các giao dịch lỗi?" — Chỉ khi API idempotent và có idempotency key; nếu không có thể trừ tiền hai lần.
- "Nếu cần quyền mà bạn không có?" — Đó là action item: on-call phải có quyền và runbook tương ứng.

**⚠️ Câu trả lời gây điểm trừ:** đợi lead đến sáng; một mình sửa code và deploy lúc 2 giờ sáng không review; không thông báo cho ai; retry hàng loạt giao dịch thanh toán mà không nghĩ tới idempotency.

**📖 Ôn lại:** [Module 16 — Xử lý sự cố & văn hóa postmortem](../01-giao-trinh/16-devops-build-cloud-security.md#p12) · [Module 14 — Dữ liệu phân tán & idempotency](../01-giao-trinh/14-microservices-system-design.md#p5)

</details>

---

<a id="g3"></a>
## 3. Bất đồng kỹ thuật & ra quyết định

### Q9. 🟡 "Kể về một lần bạn bất đồng quan điểm kỹ thuật với đồng nghiệp hoặc lead."

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Câu chuyện tốt cho thấy: bạn **hiểu đúng quan điểm của bên kia** (nhắc lại được lý lẽ mạnh nhất của họ), đưa cuộc tranh luận về **dữ liệu và tiêu chí chung** (PoC, benchmark, tiêu chí đánh giá), tách vấn đề khỏi con người, và kết quả là một quyết định tốt cho dự án — dù không nhất thiết là ý của bạn.

**Giải thích chi tiết:**

*Interviewer muốn đánh giá:* khả năng hợp tác, cách ảnh hưởng không dựa vào quyền lực, sự cởi mở (bạn có thể sai), và khả năng "disagree and commit".

*Khung trả lời:*
1. Bất đồng về điều gì, vì sao quan trọng.
2. Quan điểm của hai bên — trình bày công bằng.
3. Bạn đã làm gì để giải quyết: nói chuyện riêng trước, thống nhất **tiêu chí** (hiệu năng, chi phí vận hành, rủi ro, thời gian), làm thử nghiệm nhỏ, viết so sánh.
4. Kết quả và mối quan hệ sau đó.

*Ví dụ mẫu (dàn ý):* đồng nghiệp muốn dùng MongoDB cho module mới vì schema linh hoạt; tôi muốn PostgreSQL + JSONB vì cần transaction với các bảng hiện có và team đã có kinh nghiệm vận hành. Chúng tôi thống nhất tiêu chí: tính nhất quán dữ liệu, chi phí vận hành, truy vấn báo cáo. Tôi làm PoC JSONB với các truy vấn chính, anh ấy liệt kê các trường hợp cần schema linh hoạt. Kết quả chọn PostgreSQL + JSONB cho phần thuộc tính động; anh ấy góp ý đúng về việc cần index GIN — được đưa vào thiết kế.

**Câu hỏi nối tiếp:**
- "Nếu cuối cùng bạn sai thì sao?" — Nói được một lần bạn đã đổi ý.
- "Nếu không đi đến thống nhất?" — Escalate có cấu trúc: viết ADR với hai phương án, người ra quyết định (tech lead/architect) chọn, rồi cùng commit.

**⚠️ Câu trả lời gây điểm trừ:** kể như một trận thắng ("cuối cùng tôi đúng"); bôi xấu đồng nghiệp; bất đồng về chuyện vụn vặt (tab hay space); giải quyết bằng cách "để sếp quyết" ngay từ đầu.

**📖 Ôn lại:** [8. Chuẩn bị ngoài kỹ thuật](../02-ke-hoach-on-tap.md#8-chuẩn-bị-ngoài-kỹ-thuật)

</details>

### Q10. 🔴 "Kể về một lần quyết định cuối cùng đi ngược ý bạn. Bạn đã làm gì sau đó?" (disagree and commit)

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Sau khi đã nêu rõ quan điểm và rủi ro (bằng văn bản), khi quyết định được đưa ra thì **cam kết thực hiện hết mình** như thể đó là ý mình — không phá ngầm, không "đã bảo mà". Đồng thời đề xuất **tín hiệu đo lường/điểm xem lại** để nếu rủi ro xảy ra thì đội phát hiện sớm và điều chỉnh.

**Giải thích chi tiết:**

*Interviewer muốn đánh giá:* sự trưởng thành; Senior thường không phải người ra quyết định cuối nhưng ảnh hưởng tới văn hóa team. Người "thắng thua" khi tranh luận là rủi ro cho team.

*Khung trả lời:*
1. Quyết định là gì, quan điểm của bạn và rủi ro bạn đã nêu.
2. Vì sao người ra quyết định chọn khác (thường có thông tin bạn không có: ngân sách, deadline hợp đồng, chiến lược).
3. Bạn đã commit thế nào: làm tốt phần việc, **giảm thiểu rủi ro** bạn từng nêu bên trong phương án được chọn.
4. Kết quả — và bạn học được gì về góc nhìn của người ra quyết định.

*Ví dụ mẫu (dàn ý):* tôi đề xuất refactor module tính phí trước khi thêm biểu phí mới; PM chọn thêm trực tiếp vì cam kết với đối tác lớn trong 3 tuần. Tôi ghi rủi ro vào ADR, rồi thực hiện theo quyết định, nhưng viết thêm bộ test hồi quy cho mọi biểu phí cũ trước khi sửa (giảm rủi ro mà không làm trễ). Sau đợt ra mắt, tôi dùng số liệu bug và thời gian sửa để đề xuất lại việc refactor — lần này được ưu tiên trong quý sau.

**Câu hỏi nối tiếp:**
- "Nếu quyết định đó vi phạm bảo mật/pháp lý?" — Đây là ranh giới: escalate rõ ràng, không commit với việc vi phạm luật hoặc gây hại cho user.
- "Bạn đã bao giờ thay đổi được quyết định sau khi đã commit?"

**⚠️ Câu trả lời gây điểm trừ:** "tôi vẫn làm theo cách của mình"; làm cầm chừng chờ nó thất bại; kể với giọng oán trách người ra quyết định.

**📖 Ôn lại:** [8. Chuẩn bị ngoài kỹ thuật](../02-ke-hoach-on-tap.md#8-chuẩn-bị-ngoài-kỹ-thuật)

</details>

### Q11. 🟡 "Bạn ghi lại và truyền đạt một quyết định kiến trúc như thế nào?"

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Dùng **ADR (Architecture Decision Record)** — tài liệu ngắn trong repo gồm: bối cảnh, các phương án, quyết định, hệ quả (cả tốt lẫn xấu), trạng thái. Với thay đổi lớn: **design doc/RFC** được review bất đồng bộ trước, họp chỉ để giải quyết điểm còn tranh cãi. Quyết định "đảo ngược được" thì quyết nhanh; "khó đảo ngược" thì đầu tư phân tích.

**Giải thích chi tiết:**

*Interviewer muốn đánh giá:* bạn có làm việc ở quy mô nhiều người/nhiều team không; có hiểu chi phí của việc "không ai nhớ vì sao hệ thống lại thế này".

*Cấu trúc ADR gợi ý:*
- **Tiêu đề & trạng thái:** Proposed / Accepted / Superseded by ADR-xxx.
- **Bối cảnh:** vấn đề, ràng buộc (NFR: latency, throughput, chi phí, kỹ năng team).
- **Phương án:** 2–3 phương án, ưu/nhược theo cùng tiêu chí.
- **Quyết định** và **lý do**.
- **Hệ quả:** điều phải chấp nhận, rủi ro, điều cần theo dõi, khi nào xem lại.

*Thực hành tốt:* ADR nằm cạnh code (`docs/adr/`), được review qua PR như code; không sửa ADR cũ mà tạo ADR mới thay thế; liên kết từ README của service. Với quyết định ảnh hưởng nhiều team: RFC có thời hạn góp ý (vd 1 tuần), người ra quyết định được chỉ định rõ.

*Ví dụ mẫu (dàn ý):* ADR "Dùng outbox + Debezium thay cho dual-write khi phát sự kiện đơn hàng": bối cảnh mất sự kiện khi crash; phương án: dual-write, outbox + polling, outbox + CDC; quyết định: outbox + polling trước (đơn giản, đủ cho tải hiện tại), CDC khi vượt ngưỡng; hệ quả: độ trễ sự kiện tăng nhẹ, cần job dọn bảng outbox.

**Câu hỏi nối tiếp:**
- "Làm sao để người mới biết các quyết định này?" — Onboarding đọc danh sách ADR; liên kết trong code review.
- "Quyết định một chiều vs hai chiều?" — Chọn DB, giao thức public API: một chiều (khó đảo ngược); chọn thư viện nội bộ: hai chiều.

**⚠️ Câu trả lời gây điểm trừ:** "quyết định trong cuộc họp, mọi người đều nhớ"; tài liệu dài 30 trang không ai đọc; tài liệu nằm rời rạc trên chat.

**📖 Ôn lại:** [Module 06 — Kiến trúc: layered, hexagonal, clean architecture](../01-giao-trinh/06-design-principles-patterns.md#p9) · [Module 14 — Phương pháp System Design Interview](../01-giao-trinh/14-microservices-system-design.md#p10)

</details>

### Q12. 🔴 "Team muốn chuyển sang công nghệ mới (vd microservices, reactive/WebFlux, Kotlin, Kafka). Bạn đánh giá thế nào để quyết định?"

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Bắt đầu từ **vấn đề cần giải quyết**, không phải từ công nghệ. Đánh giá theo tiêu chí: giải quyết pain point nào (đo được), chi phí học + vận hành + tuyển dụng, độ trưởng thành/hệ sinh thái, rủi ro migration và đường lui. Thử nghiệm có giới hạn (**PoC → pilot trên một service ít rủi ro**) với tiêu chí thành công định trước, rồi mới mở rộng.

**Giải thích chi tiết:**

*Interviewer muốn đánh giá:* technical judgment — Senior tránh cả hai cực "resume-driven development" và "không bao giờ đổi gì".

*Bộ câu hỏi đánh giá:*
1. **Vấn đề:** hiện tại đau ở đâu? Có số liệu không (latency, chi phí hạ tầng, tốc độ release, số bug)? Có giải pháp đơn giản hơn trong stack hiện tại không (vd Virtual Threads của Java 21 thay vì chuyển sang reactive)?
2. **Chi phí:** đào tạo, thay đổi tooling (monitoring, debug, CI), vận hành (on-call cho công nghệ mới), tuyển người.
3. **Rủi ro:** độ trưởng thành, cộng đồng, license, khóa vendor, bảo mật.
4. **Phạm vi:** thử ở đâu trước, tiêu chí thành công/thất bại, kế hoạch quay lui.
5. **Con người:** team có hứng thú và năng lực không; ai là người "sở hữu" kiến thức.

*Ví dụ mẫu (dàn ý):* team muốn viết lại service gateway bằng WebFlux để chịu tải cao. Tôi đo trước: bottleneck là gọi đồng bộ tới 3 service phía sau với thread pool nhỏ. Thay vì viết lại, PoC Spring Boot 3 + Java 21 virtual threads + tăng giới hạn kết nối → đạt mục tiêu throughput, team không phải học mô hình reactive, code và stack trace vẫn dễ debug. Ghi ADR với điều kiện xem lại (nếu cần backpressure/streaming thực sự).

**Câu hỏi nối tiếp:**
- "Khi nào microservices là lựa chọn sai?" — Team nhỏ, domain chưa rõ ranh giới, chưa có CI/CD và observability tốt → modular monolith trước.
- "Thuyết phục ban lãnh đạo chi tiền cho thay đổi?" — Nói bằng ngôn ngữ business: chi phí, rủi ro, thời gian ra thị trường.

**⚠️ Câu trả lời gây điểm trừ:** "công nghệ mới thì tốt hơn, các công ty lớn đều dùng"; viết lại toàn bộ (big-bang) không có đường lui; không tính chi phí vận hành và con người.

**📖 Ôn lại:** [Module 14 — Monolith, Modular Monolith, Microservices](../01-giao-trinh/14-microservices-system-design.md#p1) · [Module 06 — Kiến trúc](../01-giao-trinh/06-design-principles-patterns.md#p9)

</details>

---

<a id="g4"></a>
## 4. Mentoring & code review

### Q13. 🟢 "Bạn đã mentor junior như thế nào? Kể một ví dụ cụ thể."

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Mentoring hiệu quả = **đánh giá điểm xuất phát → đặt mục tiêu cụ thể → giao việc vừa sức hơi vượt khả năng → hỗ trợ giảm dần (pair → review → tự làm) → phản hồi thường xuyên**. Ví dụ tốt có **sự tiến bộ đo được** của người được mentor (tự làm được việc gì mà trước đây không làm được).

**Giải thích chi tiết:**

*Interviewer muốn đánh giá:* bạn có nhân rộng năng lực cho team không (multiplier) — một trong những kỳ vọng rõ nhất với Senior.

*Kỹ thuật cụ thể nên nhắc:*
- **Hỏi thay vì đưa đáp án:** "Em nghĩ có cách nào khác?", "Nếu input rỗng thì sao?".
- **Pair programming** cho kỹ năng khó truyền đạt bằng lời (debug, đọc stack trace, dùng profiler).
- **Giao một phần sở hữu thật** (một module nhỏ, một ticket từ đầu tới production).
- **1-1 định kỳ** 30 phút/tuần hoặc 2 tuần, có mục tiêu.
- **Tài liệu hóa**: các câu hỏi lặp lại → wiki/onboarding.

*Ví dụ mẫu (dàn ý):* một bạn junior mới vào team, PR thường bị trả lại nhiều lần vì thiếu test và xử lý lỗi. Tôi pair 2 buổi viết test cho chính PR của bạn ấy, chia sẻ checklist tự review trước khi gửi PR, và giao bạn ấy làm trọn một tính năng nhỏ (từ design note tới deploy). Sau khoảng 2 tháng, số vòng review trung bình giảm rõ, bạn ấy bắt đầu review PR cho người mới hơn.

**Câu hỏi nối tiếp:**
- "Junior không tiến bộ dù đã hỗ trợ nhiều?" — Xem lại kỳ vọng có rõ ràng không, mục tiêu có quá lớn không; trao đổi với manager về kế hoạch cải thiện; tách vấn đề kỹ năng và thái độ.
- "Cân bằng thời gian mentor với việc của bạn?" — Đưa mentoring vào kế hoạch sprint, không phải "làm thêm".

**⚠️ Câu trả lời gây điểm trừ:** "tôi trả lời khi họ hỏi"; làm thay junior cho nhanh; mentoring không có kết quả đo được.

**📖 Ôn lại:** [8. Chuẩn bị ngoài kỹ thuật](../02-ke-hoach-on-tap.md#8-chuẩn-bị-ngoài-kỹ-thuật)

</details>

### Q14. 🟡 "Khi review code, bạn tập trung vào những gì? Feedback thế nào để không làm người khác mất động lực?"

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Ưu tiên theo thứ tự: **đúng đắn & an toàn** (logic, concurrency, transaction, bảo mật, dữ liệu) → **thiết kế** (ranh giới, coupling, API) → **khả năng bảo trì** (đặt tên, test, độ phức tạp) → style (để tool tự lo). Feedback hướng vào code không vào người, giải thích **vì sao**, phân loại mức độ (**blocking / nên sửa / nit**), và ghi nhận điểm tốt.

**Giải thích chi tiết:**

*Interviewer muốn đánh giá:* tiêu chuẩn kỹ thuật và cách giao tiếp; code review là nơi Senior tác động tới chất lượng của cả team.

*Checklist Senior hay dùng:*
- Logic & edge case: null, rỗng, overflow, timezone, encoding.
- Concurrency: shared mutable state, race condition, `@Transactional` gọi nội bộ (self-invocation), lock order.
- Dữ liệu: migration an toàn, N+1, index, transaction boundary, idempotency.
- Bảo mật: injection, authorization ở từng endpoint, log lộ dữ liệu nhạy cảm, secrets.
- Vận hành: log/metric đủ để debug, timeout/retry, feature flag, khả năng rollback.
- Test: test đúng hành vi, không over-mock.

*Cách viết comment:*
- Prefix rõ: `[blocking]`, `[suggestion]`, `[nit]`, `[question]`.
- Hỏi thay vì ra lệnh: "Nếu hai request cùng lúc thì `count` có thể bị ghi đè không?".
- PR lớn hoặc tranh luận dài → chuyển sang nói chuyện trực tiếp 10 phút.
- Tự động hóa phần cơ học: formatter, Checkstyle/SpotBugs/Sonar, ArchUnit.

**Câu hỏi nối tiếp:**
- "PR 2000 dòng thì sao?" — Đề nghị tách; về lâu dài thống nhất giới hạn kích thước PR, stacked PR, feature flag.
- "Review nhanh hay kỹ?" — SLA review (vd trong ngày làm việc), vì review chậm làm chậm cả team.

**⚠️ Câu trả lời gây điểm trừ:** tập trung vào format/đặt dấu ngoặc; comment kiểu "sai rồi, viết lại"; approve cho có ("LGTM") không đọc; hoặc chặn PR vì sở thích cá nhân.

**📖 Ôn lại:** [Module 06 — Code review với tư cách Senior](../01-giao-trinh/06-design-principles-patterns.md#p12) · [Module 15 — Static analysis & code review](../01-giao-trinh/15-testing.md#p14)

</details>

### Q15. 🟡 "Bạn nâng chất lượng code của cả team (không chỉ của bạn) bằng cách nào?"

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Biến tiêu chuẩn thành **cơ chế tự động và thói quen chung** thay vì dựa vào việc một người review kỹ: coding convention + formatter, quality gate trong CI (test, coverage trên code mới, static analysis, ArchUnit), PR template/checklist, Definition of Done, chia sẻ kỹ thuật định kỳ, và review luân phiên để kiến thức lan tỏa.

**Giải thích chi tiết:**

*Interviewer muốn đánh giá:* tư duy hệ thống và khả năng ảnh hưởng ở cấp team.

*Các đòn bẩy theo mức chi phí:*
1. **Tự động (rẻ, bền):** formatter (Spotless), static analysis (SpotBugs, Error Prone, Sonar) chỉ fail trên vấn đề mới, ArchUnit cho quy tắc kiến trúc, Dependabot/Renovate cho dependency.
2. **Quy trình:** PR template (đã test gì, rủi ro, cách rollback), Definition of Done có observability và tài liệu.
3. **Kiến thức:** tech talk nội bộ 30 phút, "bug của tuần" phân tích nguyên nhân, tài liệu pattern chuẩn của team (cách viết repository, xử lý lỗi, logging).
4. **Đo lường:** tỉ lệ bug thoát ra production, thời gian review, flaky test — để biết thay đổi có hiệu quả.

*Ví dụ mẫu (dàn ý):* team hay gặp lỗi `@Transactional` không có tác dụng do gọi nội bộ và lỗi N+1. Tôi tổ chức một buổi chia sẻ 30 phút kèm ví dụ thật từ codebase, thêm rule ArchUnit và test đếm số query cho các repository chính, cập nhật PR checklist. Các lỗi cùng loại giảm hẳn trong các quý sau.

**Câu hỏi nối tiếp:**
- "Áp quality gate lên codebase cũ có hàng nghìn vi phạm?" — Baseline/freeze, chỉ chặn vi phạm mới (clean-as-you-code).
- "Có người phản đối vì làm chậm?" — Đo thời gian CI, giữ gate nhanh; cho phép ngoại lệ có lý do.

**⚠️ Câu trả lời gây điểm trừ:** "tôi review thật kỹ mọi PR" (không scale, tạo bottleneck); áp coverage 90% bắt buộc cho toàn codebase; đưa ra quy tắc mà không giải thích vì sao.

**📖 Ôn lại:** [Module 15 — Static analysis & code review](../01-giao-trinh/15-testing.md#p14) · [Module 15 — Architecture tests với ArchUnit](../01-giao-trinh/15-testing.md#p11)

</details>

### Q16. 🔴🎬 "Một PR của junior có vấn đề thiết kế khá lớn, nhưng tính năng phải release ngày mai. Bạn xử lý thế nào?"

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Phân loại vấn đề: cái gì là **rủi ro production** (đúng đắn, bảo mật, mất dữ liệu, hiệu năng nghiêm trọng) thì **phải sửa trước khi release** — kể cả phải lùi lịch hoặc giảm phạm vi; cái gì là **chất lượng thiết kế** thì có thể release kèm **tech debt ticket có chủ sở hữu và thời hạn**. Pair với junior để sửa nhanh, giao tiếp rõ với PO về rủi ro. Sau đó xem lại **vì sao vấn đề thiết kế được phát hiện muộn** (thiếu design review sớm).

**Giải thích chi tiết:**

*Interviewer muốn đánh giá:* cân bằng chất lượng – tốc độ, phán đoán rủi ro, cách hỗ trợ junior dưới áp lực, và tư duy phòng ngừa.

*Các bước:*
1. **Đánh giá trong 30 phút:** liệt kê vấn đề, xếp loại blocking / chấp nhận được tạm thời.
2. **Blocking:** pair với junior sửa ngay (không tự viết lại thay họ trừ khi thực sự hết giờ — và nếu vậy, giải thích sau). Nếu không kịp: đề xuất với PO cắt phạm vi, ẩn sau feature flag, hoặc lùi release — kèm phân tích rủi ro bằng ngôn ngữ business.
3. **Không blocking:** ghi tech debt rõ ràng (ticket, link PR, đề xuất cách sửa), lên lịch sprint sau.
4. **Bảo vệ người junior:** góp ý riêng, không chỉ trích trước nhóm; biến thành bài học.
5. **Phòng ngừa:** với việc có thiết kế đáng kể → design note/khởi động 15 phút trước khi code; PR nhỏ, review sớm (draft PR).

*Ví dụ mẫu (dàn ý):* PR thêm tính năng hoàn tiền gọi trực tiếp API cổng thanh toán trong transaction DB, không idempotency key. Gọi API trong transaction + retry có thể hoàn tiền hai lần → blocking. Tôi pair 2 giờ để thêm idempotency key và tách gọi API ra khỏi transaction. Phần đặt logic trong controller thay vì service → ghi tech debt, sửa ở sprint sau. Sau đó team áp dụng "kickoff thiết kế 15 phút" cho mọi tính năng liên quan tới tiền.

**Câu hỏi nối tiếp:**
- "PO vẫn ép release dù có rủi ro blocking?" — Ghi rõ rủi ro bằng văn bản, escalate tới người có thẩm quyền chấp nhận rủi ro; với rủi ro mất tiền/dữ liệu/bảo mật thì giữ quan điểm.
- "Bạn có tự viết lại cho nhanh không?" — Chỉ khi thật sự cần, và phải có buổi giải thích lại cho junior.

**⚠️ Câu trả lời gây điểm trừ:** approve cho kịp deadline mà không phân loại rủi ro; chặn release vì vấn đề thẩm mỹ code; tự viết lại toàn bộ trong im lặng; trách junior trước team.

**📖 Ôn lại:** [Module 06 — Code review với tư cách Senior](../01-giao-trinh/06-design-principles-patterns.md#p12) · [Module 14 — Triển khai an toàn & tiến hóa API](../01-giao-trinh/14-microservices-system-design.md#p8)

</details>

---

<a id="g5"></a>
## 5. Ước lượng & deadline

### Q17. 🟢 "Bạn ước lượng (estimate) công việc như thế nào?"

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Chia nhỏ công việc tới mức **≤ 1–2 ngày** mỗi phần, ước lượng theo **khoảng** (lạc quan / khả năng cao / bi quan) thay vì một con số, tính đủ phần "ẩn" (test, review, deploy, tài liệu, tích hợp, sửa bug), dùng **dữ liệu lịch sử** (velocity, việc tương tự), và tách rõ **ước lượng ≠ cam kết**. Chỗ chưa rõ → làm **spike** có giới hạn thời gian trước.

**Giải thích chi tiết:**

*Interviewer muốn đánh giá:* bạn có đáng tin về thời hạn không, có nhận biết được sự không chắc chắn và truyền đạt nó không.

*Kỹ thuật:*
- **Work breakdown:** API, DB migration, logic, test, tích hợp, observability, tài liệu, deploy.
- **Three-point estimate (PERT):** `E = (O + 4M + P) / 6` — hữu ích để nói về rủi ro.
- **Story point & planning poker** trong Scrum: so sánh tương đối, giảm ảnh hưởng của người nói trước.
- **Buffer cho rủi ro đã biết:** tích hợp bên thứ ba, hệ thống legacy, phụ thuộc team khác.
- **Theo dõi sai số** giữa ước lượng và thực tế để tự hiệu chỉnh.

*Ví dụ mẫu (dàn ý):* "Tính năng xuất báo cáo Excel: tôi chia ra truy vấn + phân trang, sinh file streaming (tránh OOM với file lớn), job bất đồng bộ + thông báo, test và review. Ước lượng 4–7 ngày; con số 7 nếu cần tối ưu truy vấn trên bảng lớn — tôi đề xuất spike nửa ngày đo truy vấn trước khi chốt."

**Câu hỏi nối tiếp:**
- "Ước lượng của bạn thường sai bao nhiêu?" — Trả lời trung thực và cách bạn cải thiện.
- "Story point có quy đổi ra giờ không?" — Không nên; dùng để dự báo theo velocity của team.

**⚠️ Câu trả lời gây điểm trừ:** đưa một con số chính xác ngay cho việc chưa rõ; chỉ tính thời gian code; nhân đôi bừa "cho chắc" mà không giải thích được.

**📖 Ôn lại:** [8. Chuẩn bị ngoài kỹ thuật](../02-ke-hoach-on-tap.md#8-chuẩn-bị-ngoài-kỹ-thuật) · [Module 14 — Phương pháp System Design Interview & ước lượng](../01-giao-trinh/14-microservices-system-design.md#p10)

</details>

### Q18. 🟡🎬 "PM/khách hàng đặt deadline mà bạn thấy không khả thi. Bạn làm gì?"

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Không nói "không làm được" và cũng không im lặng nhận. Đưa ra **dữ liệu** (breakdown và ước lượng), rồi đưa **các lựa chọn** trên tam giác phạm vi – thời gian – nguồn lực/chất lượng: giảm phạm vi (MVP), giao theo giai đoạn, thêm nguồn lực (lưu ý luật Brooks), hoặc chấp nhận rủi ro có ghi nhận. Để người có thẩm quyền chọn, rồi cam kết với lựa chọn đó.

**Giải thích chi tiết:**

*Interviewer muốn đánh giá:* khả năng "quản lý ngược" (managing up), đàm phán dựa trên dữ kiện, tư duy sản phẩm (cái gì thực sự cần cho ngày đó).

*Các bước:*
1. **Hiểu vì sao có deadline đó:** sự kiện marketing, hợp đồng, quy định pháp lý? Deadline cứng hay mềm?
2. **Chứng minh bằng breakdown:** phần nào tốn thời gian, rủi ro nằm ở đâu.
3. **Đưa phương án:**
   - *Cắt phạm vi:* tính năng cốt lõi cho deadline, phần còn lại phase 2.
   - *Giải pháp tạm:* quy trình thủ công cho tình huống hiếm (vd hoàn tiền do vận hành xử lý tay ban đầu).
   - *Thêm người:* chỉ hiệu quả nếu việc chia được và người mới quen codebase.
   - *Giảm chất lượng có kiểm soát:* ghi tech debt rõ ràng — **không** bao giờ cắt bảo mật, toàn vẹn dữ liệu.
4. **Ghi lại thỏa thuận** và cập nhật tiến độ minh bạch hằng tuần; báo sớm ngay khi có rủi ro mới.

*Ví dụ mẫu (dàn ý):* khách hàng muốn module khuyến mãi đầy đủ (5 loại khuyến mãi, báo cáo, phân quyền) trong 3 tuần cho đợt sale. Tôi chỉ ra 2 loại khuyến mãi chiếm phần lớn doanh thu dự kiến, đề xuất giao 2 loại + cấu hình qua admin đơn giản trong 3 tuần, báo cáo xuất CSV thủ công, phần còn lại 4 tuần sau. Khách đồng ý; đợt sale chạy ổn định.

**Câu hỏi nối tiếp:**
- "Nếu họ vẫn nhất quyết đủ phạm vi?" — Nêu rõ rủi ro bằng văn bản, đề xuất kế hoạch dự phòng; escalate nếu rủi ro lớn.
- "Làm OT để kịp thì sao?" — Có thể ngắn hạn, nhưng không phải chiến lược; OT kéo dài làm tăng bug và kiệt sức.

**⚠️ Câu trả lời gây điểm trừ:** "tôi sẽ cố gắng hết sức" rồi trễ; từ chối thẳng mà không có phương án; âm thầm cắt test/bảo mật để kịp.

**📖 Ôn lại:** [8. Chuẩn bị ngoài kỹ thuật](../02-ke-hoach-on-tap.md#8-chuẩn-bị-ngoài-kỹ-thuật)

</details>

### Q19. 🔴 "Dự án của bạn đang trễ nghiêm trọng giữa chừng. Bạn nhận ra điều đó khi nào và đã làm gì?"

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Senior **phát hiện trễ sớm** qua tín hiệu (burn-down, việc "gần xong" kéo dài, phụ thuộc chưa sẵn sàng) và **báo sớm kèm phương án** — tin xấu báo sớm là tin tốt. Hành động: tìm nguyên nhân thật (phạm vi phình, ước lượng sai, phụ thuộc, nhân sự), lập lại kế hoạch với các lựa chọn, đặt mốc kiểm tra ngắn, và sau dự án thì retrospective để sửa nguyên nhân gốc.

**Giải thích chi tiết:**

*Interviewer muốn đánh giá:* minh bạch, khả năng điều chỉnh, không giấu vấn đề tới phút chót ("watermelon project" — xanh bên ngoài, đỏ bên trong).

*Tín hiệu trễ sớm:*
- Task "90% xong" nhiều ngày liền.
- Phát hiện yêu cầu mới trong lúc làm (scope creep) mà không đổi kế hoạch.
- Môi trường/tích hợp/dữ liệu test chưa có.
- Bug tồn đọng tăng nhanh hơn tốc độ đóng.

*Khung trả lời:*
1. Khi nào bạn nhận ra, dựa trên tín hiệu gì.
2. Bạn báo cho ai, nói gì (con số trễ dự kiến + nguyên nhân + phương án).
3. Phương án được chọn và cách thực hiện (cắt phạm vi, song song hóa, gỡ phụ thuộc, tạm dừng việc ít ưu tiên).
4. Kết quả: trễ bao nhiêu so với dự báo mới.
5. Retrospective: thay đổi quy trình (vd demo theo vertical slice mỗi tuần thay vì làm theo tầng).

*Ví dụ mẫu (dàn ý):* dự án migrate hệ thống báo cáo sang data source mới; giữa tuần 3/6 tôi thấy phần đối chiếu số liệu cũ–mới lệch nhiều hơn dự kiến. Tôi báo ngay với PM: dự kiến trễ 2 tuần nếu giữ phạm vi. Đề xuất: chuyển trước 70% báo cáo đã khớp số liệu, phần còn lại chạy song song hai hệ thống thêm 3 tuần. Kết quả: mốc chính đúng hạn, phần còn lại xong sau 2,5 tuần. Bài học: đối chiếu dữ liệu phải làm từ tuần đầu trên mẫu thật.

**Câu hỏi nối tiếp:**
- "Ai chịu trách nhiệm cho việc trễ?" — Tập trung vào nguyên nhân hệ thống; nếu có phần ước lượng của bạn, nhận phần đó.
- "Thêm người vào dự án đang trễ?" — Luật Brooks: thường làm trễ thêm do chi phí onboarding và giao tiếp, trừ khi việc rất độc lập.

**⚠️ Câu trả lời gây điểm trừ:** báo trễ vào ngày cuối; giải pháp duy nhất là OT; đổ lỗi cho BA/khách hàng thay đổi yêu cầu mà không nói mình đã quản lý thay đổi đó thế nào.

**📖 Ôn lại:** [8. Chuẩn bị ngoài kỹ thuật](../02-ke-hoach-on-tap.md#8-chuẩn-bị-ngoài-kỹ-thuật)

</details>

---

<a id="g6"></a>
## 6. Technical debt

### Q20. 🟡 "Bạn thuyết phục PO/business dành thời gian trả technical debt như thế nào?"

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Dịch tech debt sang **ngôn ngữ business**: chi phí hiện tại (thời gian làm tính năng tăng, số bug, sự cố, thời gian onboarding) và rủi ro (bảo mật, EOL, không scale được). Đề xuất **gắn với lộ trình sản phẩm** (trả nợ ở vùng code sắp sửa nhiều), chia nhỏ, có số đo trước/sau, và dành ngân sách cố định (vd 15–20% capacity mỗi sprint) thay vì xin "một sprint refactor" lớn.

**Giải thích chi tiết:**

*Interviewer muốn đánh giá:* bạn vừa có tiêu chuẩn kỹ thuật vừa hiểu ưu tiên kinh doanh; biết phân biệt nợ đáng trả và nợ có thể sống chung.

*Phân loại nợ (để ưu tiên):*
- **Nợ có lãi cao:** nằm ở vùng code thay đổi thường xuyên (hotspot = churn cao × độ phức tạp cao) → trả trước.
- **Nợ rủi ro:** thư viện hết hỗ trợ, lỗ hổng bảo mật, Java 8 hết bản vá miễn phí → trả theo hạn.
- **Nợ ngủ yên:** code xấu nhưng không ai đụng tới → để yên.

*Cách thuyết phục:*
1. Đo: "mỗi lần thêm biểu phí mất trung bình 5 ngày, 3 lần gần nhất đều gây bug production".
2. Đề xuất: "refactor 8 ngày, sau đó thêm biểu phí còn ~1–2 ngày; trong quý tới có 4 biểu phí mới".
3. Rủi ro khi không làm.
4. Cách làm an toàn: strangler, feature flag, test hồi quy trước.
5. Báo cáo kết quả sau khi làm — tạo uy tín cho lần đề xuất sau.

*Ví dụ mẫu (dàn ý):* module tính phí là chuỗi `if-else` hàng trăm dòng; tôi lấy số liệu từ Jira và Git (số bug, thời gian lead time) để đề xuất tái cấu trúc theo Strategy + bảng cấu hình, làm từng bước trong 2 sprint xen kẽ tính năng. Sau đó thời gian thêm biểu phí giảm rõ, PO tự đề xuất mô hình 20% capacity cho các module khác.

**Câu hỏi nối tiếp:**
- "Đo tech debt bằng gì?" — Lead time cho thay đổi, tỉ lệ bug theo module, hotspot từ Git, chỉ số Sonar (chỉ là tham khảo).
- "Nợ nào bạn chọn không trả?" — Chuẩn bị một ví dụ.

**⚠️ Câu trả lời gây điểm trừ:** "code bẩn quá, cần viết lại" không số liệu; xin dừng tính năng 2 tháng để refactor; lén refactor lớn trong ticket tính năng.

**📖 Ôn lại:** [Module 06 — Code smells & refactoring](../01-giao-trinh/06-design-principles-patterns.md#p2) · [Module 15 — Coverage và mutation testing](../01-giao-trinh/15-testing.md#p10)

</details>

### Q21. 🔴🎬 "Hệ thống legacy Struts 1 + JSP chạy 10 năm, ít test, vẫn mang lại doanh thu. Ban lãnh đạo hỏi: viết lại hay refactor dần?"

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Mặc định nghiêng về **refactor dần theo Strangler Fig**: dựng lớp định tuyến phía trước, chuyển từng chức năng sang Spring Boot, chạy song song và so sánh, tắt dần phần cũ. Big-bang rewrite chỉ hợp lý khi hệ thống nhỏ, nghiệp vụ được hiểu đầy đủ và có thể đóng băng tính năng. Bước đầu luôn là **characterization test** (ghi nhận hành vi hiện tại) + **observability** để biết chức năng nào thực sự được dùng.

**Giải thích chi tiết:**

*Interviewer muốn đánh giá:* hiểu rủi ro của rewrite (mất nghiệp vụ ẩn trong code, hai hệ thống phải duy trì song song, business phải chờ), khả năng lập kế hoạch migration nhiều quý, và nói chuyện với lãnh đạo bằng rủi ro/chi phí.

*Vì sao rewrite thường thất bại:* code cũ chứa hàng trăm quy tắc nghiệp vụ không có tài liệu (bug cũ đã thành "tính năng" mà khách đang dựa vào); trong lúc viết lại, hệ thống cũ vẫn phải thêm tính năng → đích di chuyển; giá trị chỉ xuất hiện ở cuối → rủi ro bị hủy giữa chừng.

*Kế hoạch Strangler (dàn ý):*
1. **Khảo sát:** đo traffic theo action (access log), xác định chức năng chết (xóa luôn), chức năng giá trị cao/đau nhiều.
2. **An toàn trước:** nâng cấp bảo mật khẩn cấp (Struts 1 đã EOL từ lâu, Struts 2 từng có lỗ hổng RCE nghiêm trọng như CVE-2017-5638 qua OGNL), đưa build lên Maven, CI, logging.
3. **Characterization test:** ghi request/response thật (đã ẩn dữ liệu nhạy cảm) làm test hồi quy.
4. **Định tuyến:** reverse proxy/gateway chuyển dần từng URL sang ứng dụng Spring Boot mới; chia sẻ session/SSO.
5. **Dữ liệu:** thường dùng chung DB giai đoạn đầu (anti-corruption layer ở code mới), tách DB sau.
6. **Chạy song song/shadow traffic** với chức năng rủi ro cao, so sánh kết quả.
7. **Đo tiến độ** bằng % traffic đã chuyển, không phải % code đã viết.

*Ví dụ mẫu (dàn ý để trả lời lãnh đạo):* "Viết lại toàn bộ ước tính 12–18 tháng không tạo giá trị mới cho tới cuối, rủi ro lớn. Tôi đề xuất 6 tuần đầu: vá bảo mật, CI, test hồi quy, đo traffic. Sau đó mỗi quý chuyển 2–3 nhóm chức năng có giá trị cao nhất; mỗi bước đều rollback được bằng định tuyến. Sau 2 quý ta có số liệu thực để quyết định tăng tốc hay giữ nhịp."

**Câu hỏi nối tiếp:**
- "Khi nào rewrite là đúng?" — Hệ thống nhỏ, công nghệ không thể vận hành/tuyển người nữa, nghiệp vụ thay đổi hoàn toàn, có thể đóng băng tính năng.
- "Team cũ chỉ biết Struts?" — Đào tạo song song, pair giữa người giỏi nghiệp vụ và người giỏi Spring.
- "Session và bảo mật giữa hai hệ thống?" — SSO/token chung, hoặc gateway xử lý xác thực.

**⚠️ Câu trả lời gây điểm trừ:** "viết lại bằng microservices cho hiện đại" ngay; refactor không có test bảo vệ; bỏ qua vấn đề bảo mật của framework EOL; đo tiến độ bằng số dòng code mới.

**📖 Ôn lại:** [Module 10 — Chiến lược migration sang Spring MVC / Spring Boot](../01-giao-trinh/10-struts.md#10-migration) · [Module 14 — Monolith, Modular Monolith, Microservices](../01-giao-trinh/14-microservices-system-design.md#p1)

</details>

---

<a id="g7"></a>
## 7. Làm việc với BA, PO & stakeholder

### Q22. 🟢 "Bạn làm gì khi requirement mơ hồ hoặc thiếu?"

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Làm rõ **trước** khi code: hỏi "vấn đề của người dùng là gì" (không chỉ "làm gì"), liệt kê **edge case và câu hỏi mở** bằng văn bản, đề xuất giả định mặc định để BA/PO xác nhận, chốt **acceptance criteria** (Given–When–Then) và ví dụ cụ thể. Phần chưa thể rõ → làm nhỏ, demo sớm để lấy phản hồi.

**Giải thích chi tiết:**

*Interviewer muốn đánh giá:* chủ động, tư duy sản phẩm, khả năng giảm lãng phí do làm sai.

*Câu hỏi làm rõ hay dùng (với dev Java backend):*
- Ai dùng, tần suất, khối lượng dữ liệu (ảnh hưởng thiết kế: phân trang, async, batch)?
- Trường hợp lỗi/ngoại lệ: dữ liệu thiếu, trùng, gửi hai lần, timeout đối tác.
- Quyền: ai được xem/sửa?
- Múi giờ, tiền tệ, làm tròn, đa ngôn ngữ.
- Dữ liệu cũ xử lý thế nào khi có quy tắc mới (migration)?
- Đo thành công bằng gì?

*Ví dụ mẫu (dàn ý):* yêu cầu "cho phép khách hủy đơn". Tôi gửi lại danh sách câu hỏi: hủy được tới trạng thái nào? Đơn đã thanh toán thì hoàn tiền tự động hay thủ công? Đơn có nhiều kiện hàng giao một phần? Có giới hạn số lần hủy để chống lạm dụng? Sau 30 phút trao đổi, phạm vi phiên bản đầu rõ ràng và tránh được một tuần làm lại.

**Câu hỏi nối tiếp:**
- "BA cũng không biết câu trả lời?" — Cùng tìm người có quyền quyết định; đề xuất phương án mặc định an toàn và làm cho dễ thay đổi.
- "Requirement được viết rất chi tiết nhưng bạn thấy sai về nghiệp vụ?" — Nêu vấn đề kèm ví dụ cụ thể, không tự ý làm khác.

**⚠️ Câu trả lời gây điểm trừ:** "làm theo đúng ticket"; tự đoán rồi code, đến demo mới phát hiện sai; hỏi lắt nhắt từng câu qua chat thay vì gom thành danh sách.

**📖 Ôn lại:** [8. Chuẩn bị ngoài kỹ thuật](../02-ke-hoach-on-tap.md#8-chuẩn-bị-ngoài-kỹ-thuật)

</details>

### Q23. 🟡🎬 "Giữa sprint, PO yêu cầu thêm/đổi một tính năng 'gấp'. Bạn xử lý thế nào?"

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Không từ chối máy móc, cũng không nhận thêm mà không đổi gì. Hiểu **mức độ gấp thật** (sự cố, pháp lý, khách lớn?), ước lượng nhanh, rồi làm rõ **đánh đổi**: thêm việc này thì việc nào trong sprint bị lùi — để PO (người sở hữu ưu tiên) quyết định. Nếu chuyện này lặp lại thường xuyên → đưa vào retrospective (dành capacity dự phòng, điều chỉnh độ dài sprint).

**Giải thích chi tiết:**

*Interviewer muốn đánh giá:* hiểu vai trò trong Scrum (PO quyết ưu tiên, team quyết cách làm và lượng việc), giữ được sự bền vững của team mà vẫn linh hoạt với business.

*Khung xử lý:*
1. **Hỏi:** chuyện gì xảy ra nếu đợi tới sprint sau? Ai bị ảnh hưởng?
2. **Ước lượng nhanh** và rủi ro kỹ thuật.
3. **Trình bày đánh đổi:** "Nhận việc này thì story A và B sẽ trượt sang sprint sau."
4. **PO quyết**, cập nhật sprint backlog minh bạch, thông báo stakeholder bị ảnh hưởng.
5. **Nhìn lại pattern:** nếu mỗi sprint đều có "việc gấp" → dành 10–20% capacity cho việc không lường trước, hoặc cân nhắc Kanban cho team vận hành nhiều.

*Ví dụ mẫu (dàn ý):* giữa sprint, ngân hàng đối tác thông báo đổi định dạng file đối soát có hiệu lực sau 10 ngày. Đây là gấp thật (không làm thì đối soát hỏng). Tôi ước lượng 3 ngày, đề xuất lùi story cải tiến UI sang sprint sau; PO đồng ý. Retrospective: thêm kênh nhận thông báo kỹ thuật từ đối tác để biết sớm hơn.

**Câu hỏi nối tiếp:**
- "Stakeholder khác (không phải PO) đến nhờ trực tiếp dev?" — Hướng họ tới PO; giúp họ mô tả nhu cầu.
- "Sprint goal bị phá vỡ?" — Có thể hủy sprint (hiếm) và lập kế hoạch lại.

**⚠️ Câu trả lời gây điểm trừ:** "sprint đã chốt, không nhận"; nhận hết rồi OT; tự quyết bỏ việc khác mà không báo PO.

**📖 Ôn lại:** [8. Chuẩn bị ngoài kỹ thuật](../02-ke-hoach-on-tap.md#8-chuẩn-bị-ngoài-kỹ-thuật)

</details>

### Q24. 🟡 "Giải thích một vấn đề kỹ thuật phức tạp cho người không làm kỹ thuật như thế nào?"

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Bắt đầu từ **điều họ quan tâm** (ảnh hưởng tới khách hàng, tiền, thời gian, rủi ro), dùng **ẩn dụ đời thường** đúng bản chất, bỏ thuật ngữ, đưa **lựa chọn kèm hệ quả** để họ ra quyết định, và kiểm tra lại họ hiểu đúng bằng cách nhờ họ tóm tắt.

**Giải thích chi tiết:**

*Interviewer muốn đánh giá:* giao tiếp — Senior thường là cầu nối giữa team và business/khách hàng.

*Khung "BLUF" (Bottom Line Up Front):*
1. **Kết luận/đề xuất trước:** "Chúng ta nên hoãn release 2 ngày."
2. **Vì sao, bằng tác động:** "Nếu không, khoảng 5% khách có thể bị trừ tiền hai lần khi mạng chập chờn."
3. **Lựa chọn:** hoãn 2 ngày / release nhưng tắt thanh toán ví / release và xử lý hoàn tiền thủ công.
4. **Khuyến nghị và việc cần họ quyết.**

*Ẩn dụ hữu ích:*
- Technical debt ↔ khoản vay: vay để đi nhanh, nhưng lãi tăng dần nếu không trả.
- Cache ↔ để đồ hay dùng trên bàn thay vì trong kho.
- Idempotency ↔ bấm nút thang máy nhiều lần vẫn chỉ gọi một lần.
- Message queue ↔ hộp thư: người gửi không phải chờ người nhận.

**Câu hỏi nối tiếp:**
- "Họ vẫn muốn con số chắc chắn khi bạn chưa chắc?" — Đưa khoảng và mức tin cậy, hẹn thời điểm có thông tin chính xác hơn.
- "Viết báo cáo sự cố cho khách hàng?" — Ngắn, không đổ lỗi, có tác động, nguyên nhân ở mức phù hợp, cam kết phòng ngừa.

**⚠️ Câu trả lời gây điểm trừ:** dùng thuật ngữ dày đặc; giải thích chi tiết kỹ thuật mà không đưa đến quyết định; ẩn dụ sai bản chất gây hiểu lầm.

**📖 Ôn lại:** [8. Chuẩn bị ngoài kỹ thuật](../02-ke-hoach-on-tap.md#8-chuẩn-bị-ngoài-kỹ-thuật)

</details>

### Q25. 🔴🎬 "Hai stakeholder (vd Sales và Vận hành) yêu cầu hai điều mâu thuẫn nhau với cùng một tính năng. Bạn làm gì?"

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Dev **không nên tự chọn phe**. Làm rõ **mục tiêu gốc** của từng bên (thường mâu thuẫn ở giải pháp chứ không ở mục tiêu), tìm phương án thỏa cả hai (cấu hình, phân quyền, giai đoạn), trình bày các phương án với chi phí/rủi ro, và đưa lên **người sở hữu ưu tiên** (PO/người có thẩm quyền chung) quyết định. Ghi lại quyết định.

**Giải thích chi tiết:**

*Interviewer muốn đánh giá:* khả năng điều hướng chính trị tổ chức một cách chuyên nghiệp, tư duy giải pháp, biết ranh giới quyết định.

*Các bước:*
1. **Gặp từng bên:** "Vấn đề anh/chị muốn giải quyết là gì? Thành công trông như thế nào?"
2. **Tìm mục tiêu chung:** cả hai đều muốn doanh thu và ít sự cố.
3. **Thiết kế phương án:** cấu hình theo nhóm khách, ngưỡng có thể chỉnh, phê duyệt hai bước, A/B test để có dữ liệu thay vì tranh luận.
4. **Đưa ra quyết định** ở cuộc họp có người có thẩm quyền, kèm bảng so sánh.
5. **Thực hiện và đo** — xem lại sau một thời gian.

*Ví dụ mẫu (dàn ý):* Sales muốn cho phép đặt hàng vượt tồn kho để không mất đơn; Vận hành muốn chặn cứng vì hay phải hủy đơn và bị khách phàn nàn. Mục tiêu chung: tối đa đơn giao thành công. Phương án: cho phép "đặt trước" có nhãn rõ ràng với các SKU có lịch nhập hàng, giới hạn số lượng theo cấu hình, khách được báo thời gian giao dự kiến. PO quyết; sau một tháng đo tỉ lệ hủy đơn để điều chỉnh ngưỡng.

**Câu hỏi nối tiếp:**
- "Một bên có chức vụ cao hơn nhiều?" — Vẫn đưa dữ kiện và rủi ro rõ ràng; quyết định thuộc về người có thẩm quyền, nhưng rủi ro phải được ghi nhận.
- "Không có PO rõ ràng?" — Đề xuất xác định người ra quyết định (RACI) — đó cũng là việc của Senior.

**⚠️ Câu trả lời gây điểm trừ:** làm theo người nói to hơn/chức cao hơn; làm cả hai theo kiểu chắp vá (if-else theo người yêu cầu); tự quyết vì "tôi hiểu hệ thống nhất".

**📖 Ôn lại:** [8. Chuẩn bị ngoài kỹ thuật](../02-ke-hoach-on-tap.md#8-chuẩn-bị-ngoài-kỹ-thuật)

</details>

---

<a id="g8"></a>
## 8. Vai trò Senior & phát triển bản thân

### Q26. 🟢 "Theo bạn, Senior khác Mid-level ở điểm nào?"

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Khác ở **phạm vi tác động và mức độ tự chủ**: Mid hoàn thành tốt task được giao; Senior nhận **vấn đề mơ hồ** và tự biến nó thành giải pháp chạy ổn định trên production, cân nhắc trade-off dài hạn (vận hành, bảo mật, chi phí), **nâng năng lực cả team** (review, mentor, chuẩn hóa), và giao tiếp được với business. Senior cũng biết **khi nào không nên làm** điều gì đó.

**Giải thích chi tiết:**

*Interviewer muốn đánh giá:* bạn hiểu kỳ vọng của vai trò và tự định vị đúng — tốt nhất là chứng minh bằng ví dụ của chính bạn cho từng điểm.

*Bảng so sánh gợi ý:*

| Khía cạnh | Mid | Senior |
|---|---|---|
| Phạm vi | Task/tính năng | Hệ thống/nhiều tính năng, liên team |
| Mơ hồ | Cần requirement rõ | Tự làm rõ, đề xuất hướng |
| Thiết kế | Áp dụng pattern có sẵn | Chọn và giải thích trade-off, ghi ADR |
| Production | Sửa bug được giao | Sở hữu độ tin cậy: observability, on-call, postmortem |
| Team | Đóng góp cá nhân | Nhân rộng: review, mentor, chuẩn hóa |
| Business | Hiểu tính năng | Hiểu vì sao làm, đo tác động, đàm phán phạm vi |

*Cách trả lời mạnh:* nêu 2–3 điểm, mỗi điểm kèm một câu ví dụ ngắn từ kinh nghiệm thật ("Ví dụ ở dự án gần nhất, tôi…").

**Câu hỏi nối tiếp:**
- "Bạn còn thiếu gì để lên Lead/Staff?" — Trả lời trung thực một điểm và kế hoạch phát triển.
- "Senior có cần giỏi thuật toán không?" — Cần nền tảng để chọn cấu trúc dữ liệu và đánh giá độ phức tạp; nhưng giá trị chính là phán đoán kỹ thuật và tác động.

**⚠️ Câu trả lời gây điểm trừ:** "Senior là người có nhiều năm kinh nghiệm hơn"; "Senior biết nhiều công nghệ hơn"; chỉ nói về kỹ năng code.

**📖 Ôn lại:** [8. Chuẩn bị ngoài kỹ thuật](../02-ke-hoach-on-tap.md#8-chuẩn-bị-ngoài-kỹ-thuật)

</details>

### Q27. 🟡 "Bạn cập nhật kiến thức như thế nào? Gần đây bạn học được gì và đã áp dụng ra sao?"

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Nêu **hệ thống học** cụ thể (nguồn chính thống: release notes JDK/Spring, JEP, blog kỹ thuật của các công ty lớn, sách; thực hành bằng side project/PoC; chia sẻ lại cho team) và **một ví dụ gần đây có áp dụng thật** — kèm kết quả và giới hạn của thứ đã học.

**Giải thích chi tiết:**

*Interviewer muốn đánh giá:* sự tò mò, khả năng tự học có chọn lọc (không chạy theo mọi trào lưu), và việc biến kiến thức thành giá trị.

*Nguồn đáng nhắc với Java backend:* JEP và release notes của JDK (Java 21 LTS: virtual threads, record patterns, pattern matching cho switch; Java 25 LTS), Spring Boot release notes và migration guide, InfoQ, blog kỹ thuật của các công ty lớn, sách (*Designing Data-Intensive Applications*, *Effective Java*, *Java Concurrency in Practice*), các hội thảo như Devoxx/Spring I/O (xem lại trên YouTube).

*Ví dụ mẫu (dàn ý):* "Gần đây tôi tìm hiểu virtual threads của Java 21. Tôi đọc JEP 444, làm PoC trên một service I/O-bound: throughput tăng khi bật `spring.threads.virtual.enabled=true`. Nhưng tôi cũng phát hiện vấn đề **pinning** khi code dùng `synchronized` quanh I/O (trên JDK 21; JDK 24 đã cải thiện với JEP 491) và connection pool DB vẫn là giới hạn thật. Tôi trình bày lại cho team kèm checklist trước khi bật trên production."

**Câu hỏi nối tiếp:**
- "Cuốn sách kỹ thuật gần nhất bạn đọc?" — Chuẩn bị một cuốn và một ý bạn đã áp dụng.
- "Làm sao biết công nghệ nào đáng học?" — Liên quan tới vấn đề đang gặp, mức độ trưởng thành, độ phổ biến trên thị trường.

**⚠️ Câu trả lời gây điểm trừ:** liệt kê khóa học/chứng chỉ không có áp dụng; "tôi học khi dự án cần" (bị động hoàn toàn); nói về công nghệ mới mà không trả lời được câu hỏi sâu về nó.

**📖 Ôn lại:** [8. Chuẩn bị ngoài kỹ thuật](../02-ke-hoach-on-tap.md#8-chuẩn-bị-ngoài-kỹ-thuật) · [Module 03 — Modern Java 8 → 21](../01-giao-trinh/03-modern-java-8-21.md)

</details>

### Q28. 🟡 "Bạn đã làm việc với khách hàng Nhật/nước ngoài, team offshore chưa? Điều gì quan trọng nhất?"

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Quan trọng nhất là **giao tiếp chủ động, minh bạch và bằng văn bản**: báo cáo sớm (đặc biệt tin xấu), xác nhận hiểu đúng bằng cách nhắc lại, tôn trọng quy trình và chất lượng tài liệu của khách. Với khách Nhật: tinh thần **Hou-Ren-Sou** (Báo cáo – Liên lạc – Thảo luận), chú trọng chất lượng và kiểm thử, không hứa điều không chắc. Với lệch múi giờ: tận dụng khung giờ chung, tài liệu và câu hỏi gom sẵn.

**Giải thích chi tiết:**

*Interviewer muốn đánh giá:* phù hợp với mô hình outsourcing/offshore rất phổ biến ở Việt Nam, khả năng giao tiếp đa văn hóa.

*Thực hành cụ thể:*
- **Hou-Ren-Sou (報連相):** *Hōkoku* — báo cáo tiến độ định kỳ, kể cả khi không có gì mới; *Renraku* — thông báo thay đổi/thông tin ngay; *Sōdan* — hỏi ý kiến trước khi tự quyết định điều ngoài phạm vi.
- **Q&A sheet:** gom câu hỏi theo mã, có trạng thái, người trả lời, ngày — tránh hỏi lặp, làm bằng chứng khi requirement thay đổi.
- **Chất lượng:** test case/test evidence (ảnh chụp, log) theo yêu cầu; review chéo trước khi giao.
- **Báo trễ:** báo sớm kèm nguyên nhân và kế hoạch khắc phục; tránh trả lời "có" khi chưa chắc — khách Nhật đánh giá cao sự chính xác hơn tốc độ.
- **Múi giờ (Mỹ/Châu Âu):** tài liệu bất đồng bộ, ghi video ngắn demo, có người trực khung giờ chồng lấn.

*Ví dụ mẫu (dàn ý):* dự án cho khách Nhật, tôi là cầu nối kỹ thuật cùng BrSE. Khi phát hiện spec mâu thuẫn với dữ liệu thật, tôi ghi vào Q&A sheet kèm ví dụ dữ liệu và hai phương án, không tự chọn. Báo cáo hằng ngày nêu rõ rủi ro trễ 1 ngày ngay khi thấy, kèm kế hoạch. Khách đánh giá cao vì không có "bất ngờ" vào cuối giai đoạn.

**Câu hỏi nối tiếp:**
- "Khách thay đổi spec liên tục?" — Quy trình change request, ghi nhận tác động tới chi phí/thời gian.
- "Bất đồng với khách về giải pháp kỹ thuật?" — Trình bày rủi ro bằng văn bản, lịch sự, kèm dữ kiện; quyết định cuối thuộc về khách nếu không vi phạm an toàn.

**⚠️ Câu trả lời gây điểm trừ:** "khách khó tính, cứ làm theo là xong"; im lặng khi không hiểu; hứa ngày giao rồi trễ không báo.

**📖 Ôn lại:** [8. Chuẩn bị ngoài kỹ thuật](../02-ke-hoach-on-tap.md#8-chuẩn-bị-ngoài-kỹ-thuật)

</details>

### Q29. 🔴 "Khi nào bạn chọn tốc độ thay vì chất lượng, và ngược lại? Cho ví dụ."

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Đây không phải lựa chọn nhị phân mà là **mức đầu tư phù hợp với rủi ro và khả năng đảo ngược**. Đi nhanh khi: thử nghiệm giả thuyết sản phẩm, quyết định dễ đảo ngược, phạm vi ảnh hưởng nhỏ, có feature flag. Đầu tư chất lượng cao khi: dính tới tiền, dữ liệu, bảo mật, API công khai, schema dữ liệu, thứ nhiều team phụ thuộc — những thứ khó/không thể đảo ngược. Những thứ **không bao giờ cắt**: bảo mật, toàn vẹn dữ liệu, khả năng rollback, observability tối thiểu.

**Giải thích chi tiết:**

*Interviewer muốn đánh giá:* phán đoán kỹ thuật thực dụng — câu hỏi phân biệt người cầu toàn cứng nhắc, người "làm cho xong" và Senior thực thụ.

*Khung quyết định:*
1. **Khả năng đảo ngược:** "cửa hai chiều" (dễ quay lại) → đi nhanh; "cửa một chiều" → cẩn trọng.
2. **Bán kính ảnh hưởng (blast radius):** một trang admin nội bộ vs luồng thanh toán.
3. **Tuổi thọ dự kiến:** prototype 2 tuần vs core domain sống 5 năm.
4. **Chi phí sửa sau:** schema dữ liệu và API công khai rất đắt để sửa sau.
5. **Ghi nhận nợ:** khi đi nhanh, ghi rõ nợ và điều kiện phải trả (vd "trước khi mở cho > 10% user").

*Ví dụ mẫu (dàn ý):* tính năng gợi ý sản phẩm thử nghiệm: tôi chọn làm nhanh trong 1 tuần với logic đơn giản, test vừa đủ, sau feature flag cho 5% user — để đo xem có tăng chuyển đổi không trước khi đầu tư. Ngược lại, thay đổi schema bảng giao dịch: dành thêm 3 ngày thiết kế, review với DBA, migration nhiều bước, test với dữ liệu kích thước production.

**Câu hỏi nối tiếp:**
- "Prototype 'tạm' lại thành production vĩnh viễn?" — Thỏa thuận trước điều kiện "tốt nghiệp" của prototype; feature flag có hạn sử dụng.
- "Đo chất lượng bằng gì?" — Tỉ lệ thay đổi gây lỗi (change failure rate), MTTR, lead time — các chỉ số DORA.

**⚠️ Câu trả lời gây điểm trừ:** "luôn luôn chất lượng là số một" (không thực tế); "khách cần nhanh thì làm nhanh" (không có giới hạn); không nêu được ví dụ cụ thể cho cả hai chiều.

**📖 Ôn lại:** [Module 14 — Triển khai an toàn & tiến hóa API](../01-giao-trinh/14-microservices-system-design.md#p8) · [Module 16 — Thiết kế pipeline CI/CD](../01-giao-trinh/16-devops-build-cloud-security.md#p8)

</details>

### Q30. 🟡 "Bạn dùng AI coding assistant (Copilot, Claude, ChatGPT…) trong công việc thế nào? Kiểm soát chất lượng ra sao?"

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Dùng như **công cụ tăng tốc có kiểm soát**: tốt cho boilerplate, test case, giải thích code lạ, phác thảo phương án, chuyển đổi/migration lặp lại; **mọi output đều được review như code của người khác** — chạy test, đọc kỹ phần concurrency/bảo mật/transaction, kiểm tra API có thật và đúng phiên bản. Tuân thủ chính sách công ty: không dán mã nguồn, dữ liệu khách hàng hay secrets vào công cụ chưa được phê duyệt.

**Giải thích chi tiết:**

*Interviewer muốn đánh giá:* bạn có tận dụng công cụ hiện đại một cách có trách nhiệm không; hiểu rủi ro bảo mật, bản quyền, và "ảo giác" (hallucination); và với Senior — bạn định hướng team dùng ra sao.

*Rủi ro cần nêu:*
- **Sai một cách thuyết phục:** gọi method không tồn tại, dùng API đã deprecated, sai phiên bản (vd code Spring Boot 2 `javax.*` cho dự án Boot 3 `jakarta.*`).
- **Lỗi tinh vi:** race condition, transaction boundary, xử lý lỗi nuốt exception, SQL injection khi nối chuỗi.
- **Rò rỉ dữ liệu:** mã nguồn, dữ liệu khách hàng, credentials gửi ra ngoài.
- **Bản quyền/license** của code sinh ra (tùy chính sách công ty).
- **Junior phụ thuộc** mà không hiểu code.

*Thực hành tốt:* test là "hợp đồng" — viết/đọc test trước khi chấp nhận code sinh ra; PR vẫn qua review bình thường; ghi rõ trong PR phần nào sinh tự động nếu team yêu cầu; dùng cho việc mình **có khả năng đánh giá đúng sai**.

*Ví dụ mẫu (dàn ý):* tôi dùng AI để sinh bộ test tham số hóa cho module tính phí và chuyển hàng trăm test JUnit 4 sang JUnit 5; tiết kiệm nhiều thời gian, nhưng phát hiện vài test assertion yếu (chỉ kiểm tra không null) nên tôi rà lại toàn bộ và chạy mutation testing để kiểm tra chất lượng test.

**Câu hỏi nối tiếp:**
- "Làm sao đảm bảo junior vẫn học được?" — Yêu cầu giải thích code trong PR; bài tập không dùng công cụ trong onboarding.
- "Công ty cấm dùng thì sao?" — Tuân thủ; có thể đề xuất phương án được phê duyệt (bản enterprise, chạy nội bộ) qua đúng kênh.

**⚠️ Câu trả lời gây điểm trừ:** "AI viết, chạy được là merge"; dán log production chứa dữ liệu khách hàng vào công cụ công cộng; ngược lại, từ chối hoàn toàn mà không có lý do cụ thể.

**📖 Ôn lại:** [Module 15 — Coverage và mutation testing](../01-giao-trinh/15-testing.md#p10) · [Module 16 — Supply chain, secrets management & TLS](../01-giao-trinh/16-devops-build-cloud-security.md#p11)

</details>

---

<a id="g9"></a>
## 9. Nghỉ việc, lương & offer — thị trường Việt Nam

> ⚠️ Các thông tin pháp lý dưới đây dựa trên Bộ luật Lao động 2019 và quy định bảo hiểm hiện hành tại thời điểm viết. Mức lương cơ sở, mức trần đóng bảo hiểm, mức giảm trừ gia cảnh và biểu thuế TNCN **thay đổi theo thời gian** — hãy kiểm tra văn bản mới nhất hoặc hỏi HR trước khi tính toán.

### Q31. 🟢 "Vì sao bạn muốn rời công ty hiện tại?"

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Nói theo hướng **tìm kiếm điều gì** (pull) chứ không **chạy trốn điều gì** (push): cơ hội phát triển, bài toán lớn hơn, domain mới, vai trò rộng hơn — và **kết nối với vị trí này**. Trung thực nhưng không nói xấu công ty cũ, sếp cũ, đồng nghiệp cũ. Ngắn gọn (30–60 giây).

**Giải thích chi tiết:**

*Interviewer muốn đánh giá:* động lực, rủi ro bạn lại nghỉ sớm, sự chuyên nghiệp (người nói xấu công ty cũ thì sau này cũng sẽ nói xấu công ty này).

*Các lý do phổ biến và cách diễn đạt:*
- **Hết thử thách:** "Hệ thống hiện tại đã ổn định, phần lớn việc là bảo trì; tôi muốn làm bài toán scale lớn hơn như ở vị trí này."
- **Muốn chuyển từ outsourcing sang product (hoặc ngược lại):** "Tôi muốn sở hữu sản phẩm lâu dài, thấy được tác động của quyết định kỹ thuật lên người dùng."
- **Lương:** có thể nói thật nhưng không nên là lý do duy nhất: "Tôi muốn mức đãi ngộ phản ánh đúng phạm vi công việc hiện tại, và quan trọng hơn là cơ hội…"
- **Công ty tái cấu trúc/layoff:** nói thẳng, ngắn gọn, không cảm xúc (xem Q36).
- **Môi trường không tốt:** chọn khía cạnh trung tính ("tôi muốn môi trường có quy trình kỹ thuật rõ ràng hơn: code review, CI/CD, postmortem").

*Ví dụ mẫu (dàn ý):* "Tôi đã gắn bó 3 năm, đóng góp vào việc đưa hệ thống lên ổn định và học được nhiều. Giai đoạn tới tôi muốn làm sâu hơn về hệ thống phân tán ở quy mô lớn và có tiếng nói trong quyết định kiến trúc — điều mà vị trí này mô tả khá rõ trong JD."

**Câu hỏi nối tiếp:**
- "Nếu công ty hiện tại đáp ứng điều đó, bạn có ở lại không?" — Trả lời nhất quán (xem Q33 về counter-offer).
- "Vì sao bạn ở công ty trước chỉ 1 năm?" — Xem Q36.

**⚠️ Câu trả lời gây điểm trừ:** kể khổ, nói xấu sếp; "chỉ vì lương"; lý do mâu thuẫn với chính vị trí đang ứng tuyển (muốn ít áp lực nhưng ứng tuyển startup đang tăng trưởng nhanh).

**📖 Ôn lại:** [8. Chuẩn bị ngoài kỹ thuật](../02-ke-hoach-on-tap.md#8-chuẩn-bị-ngoài-kỹ-thuật)

</details>

### Q32. 🟡 "Mức lương mong muốn của bạn là bao nhiêu?" — đàm phán lương ở Việt Nam

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Chuẩn bị trước bằng dữ liệu thị trường (báo cáo lương của TopDev, ITviec, Navigos/VietnamWorks, cộng đồng, người quen), nói rõ **Gross hay Net**, đưa ra **một khoảng** mà mức thấp của khoảng vẫn là con số bạn hài lòng, và đàm phán trên **tổng gói đãi ngộ** (lương tháng, thưởng/tháng 13, bảo hiểm, phép, review lương, remote, thiết bị, đào tạo, ESOP) — không chỉ con số tháng. Nếu được, để công ty đưa con số trước hoặc hỏi khung ngân sách của vị trí.

**Giải thích chi tiết:**

*Interviewer/HR muốn đánh giá:* bạn có nằm trong ngân sách không và kỳ vọng có thực tế không.

*Những điểm cần làm rõ (đặc thù Việt Nam):*
- **Gross vs Net:** Gross là lương trước khi trừ bảo hiểm bắt buộc và thuế TNCN; Net là thực nhận. Phần lớn công ty đàm phán theo Gross; nếu bạn quen nghĩ theo Net, hãy quy đổi trước (dùng công cụ tính lương Gross–Net trực tuyến và cập nhật quy định mới nhất).
- **Bảo hiểm bắt buộc:** người lao động đóng BHXH 8% + BHYT 1,5% + BHTN 1% = **10,5%** trên **mức lương đóng bảo hiểm** (có mức trần). Hỏi rõ công ty đóng bảo hiểm **trên toàn bộ lương hay chỉ một phần** — đóng trên mức thấp làm thực nhận hằng tháng cao hơn nhưng thiệt về quyền lợi lâu dài (lương hưu, thai sản, ốm đau, trợ cấp thất nghiệp).
- **Lương tháng 13 và thưởng:** không phải nghĩa vụ bắt buộc theo luật; phụ thuộc chính sách công ty và có thể gắn với kết quả kinh doanh/đánh giá. Hỏi: "tháng 13 có được ghi trong hợp đồng không, và thưởng hiệu suất bình quân những năm gần đây là bao nhiêu tháng lương?"
- **Thử việc:** theo Bộ luật Lao động 2019, tối đa **60 ngày** với công việc yêu cầu trình độ cao đẳng trở lên (180 ngày với người quản lý doanh nghiệp); lương thử việc **ít nhất 85%** lương chính thức. Nhiều công ty trả 100% — có thể đàm phán.
- **Chu kỳ review lương:** 1 hay 2 lần/năm, tháng nào (vào giữa kỳ có thể phải chờ lâu).
- **Phúc lợi khác:** bảo hiểm sức khỏe bổ sung, số ngày phép (luật tối thiểu 12 ngày/năm với điều kiện làm việc bình thường), chế độ remote/hybrid, phụ cấp, ESOP (hỏi rõ vesting và tính thanh khoản).

*Câu trả lời mẫu (dàn ý):* "Dựa trên phạm vi công việc trong JD và khảo sát thị trường cho vị trí Senior Java, tôi kỳ vọng khoảng X–Y triệu **gross** mỗi tháng. Tôi cũng quan tâm tới tổng gói: chính sách thưởng, bảo hiểm và cơ hội phát triển. Khung ngân sách cho vị trí này của công ty là bao nhiêu ạ?"

**Câu hỏi nối tiếp:**
- "Lương hiện tại của bạn là bao nhiêu?" — Có thể trả lời trung thực hoặc lịch sự hướng về kỳ vọng: "Tôi muốn tập trung vào giá trị tôi mang lại cho vị trí này; kỳ vọng của tôi là…". Không bịa số.
- "Công ty chỉ trả được thấp hơn kỳ vọng?" — Đàm phán phần khác: review lương sớm sau thử việc có điều kiện rõ ràng, sign-on bonus, ngày phép, remote.

**⚠️ Câu trả lời gây điểm trừ:** "bao nhiêu cũng được"; đưa một con số không phân biệt gross/net; đòi mức quá xa thị trường không có lý do; chấp nhận offer miệng không có thư mời (offer letter) bằng văn bản.

**📖 Ôn lại:** [8. Chuẩn bị ngoài kỹ thuật](../02-ke-hoach-on-tap.md#8-chuẩn-bị-ngoài-kỹ-thuật)

</details>

### Q33. 🟡 "Bạn có đang phỏng vấn ở nơi khác/có offer khác không? Nếu công ty hiện tại tăng lương giữ bạn lại thì sao?"

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Trung thực ở mức vừa đủ: "Tôi đang trong vài quy trình khác" — không cần nêu tên hay con số trừ khi muốn dùng làm đòn bẩy hợp lý. Nếu có offer có hạn chót, nói rõ để công ty sắp xếp. Về **counter-offer**: trả lời nhất quán với lý do nghỉ việc — nếu lý do chính không phải tiền thì tăng lương không giải quyết vấn đề.

**Giải thích chi tiết:**

*Interviewer muốn đánh giá:* mức độ nghiêm túc, rủi ro bạn nhận offer rồi "quay xe", và tính minh bạch.

*Lưu ý thực tế:*
- **Không bịa offer** để ép giá — thị trường IT ở Việt Nam khá nhỏ, HR và các lead thường biết nhau.
- Nếu có offer thật cao hơn và bạn thích công ty này hơn, có thể nói: "Tôi có một offer ở mức X, nhưng tôi ưu tiên công ty mình vì… Liệu có thể điều chỉnh gần mức đó không?"
- **Counter-offer:** cân nhắc vì sao trước đó công ty không trả mức đó; lý do ra đi (công việc, sếp, định hướng) có còn không; uy tín khi đã nhận offer rồi lại từ chối.
- **Đã nhận offer thì giữ lời:** nếu buộc phải từ chối sau khi đã nhận, báo sớm và lịch sự.
- **Thời gian báo trước khi nghỉ** (Bộ luật Lao động 2019): hợp đồng không xác định thời hạn ít nhất **45 ngày**; xác định thời hạn 12–36 tháng ít nhất **30 ngày**; dưới 12 tháng ít nhất **3 ngày làm việc** (một số ngành nghề đặc thù khác) — hãy kiểm tra hợp đồng và thỏa thuận ngày bắt đầu thực tế với công ty mới, đồng thời tính thời gian bàn giao tử tế.

**Câu hỏi nối tiếp:**
- "Khi nào bạn có thể bắt đầu?" — Tính đúng thời gian báo trước + bàn giao; đừng hứa sớm hơn khả năng.
- "Bạn đang ở vòng nào ở các nơi khác?"

**⚠️ Câu trả lời gây điểm trừ:** bịa offer; dùng offer để ép giá liên tục nhiều vòng; nói "nếu công ty cũ tăng lương tôi sẽ ở lại" (làm interviewer mất niềm tin); hứa ngày bắt đầu không khả thi với thời gian báo trước.

**📖 Ôn lại:** [8. Chuẩn bị ngoài kỹ thuật](../02-ke-hoach-on-tap.md#8-chuẩn-bị-ngoài-kỹ-thuật)

</details>

### Q34. 🟡 "Điểm mạnh và điểm yếu lớn nhất của bạn là gì?"

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **Điểm mạnh:** 1–2 điểm liên quan trực tiếp tới vị trí, mỗi điểm có bằng chứng cụ thể. **Điểm yếu:** một điểm **thật** nhưng không phải năng lực cốt lõi của vị trí, kèm **hành động cải thiện đang làm** và tiến bộ đã đạt được.

**Giải thích chi tiết:**

*Interviewer muốn đánh giá:* mức độ tự nhận thức và trung thực, sự phù hợp với vị trí.

*Điểm mạnh — cấu trúc:* tên điểm mạnh → ví dụ cụ thể → tác động. Ví dụ: "Tôi giỏi chẩn đoán vấn đề hiệu năng JVM: ở dự án gần nhất tôi dùng JFR và phân tích GC log để tìm ra rò rỉ bộ nhớ do cache không giới hạn, giảm số lần restart service từ vài lần/tuần xuống 0."

*Điểm yếu — ví dụ hợp lý cho Senior:*
- "Trước đây tôi hay tự làm thay thay vì giao việc khi deadline gấp; tôi đang tập giao việc kèm hướng dẫn và chấp nhận chậm hơn ngắn hạn để team mạnh lên."
- "Tôi chưa có nhiều kinh nghiệm với frontend hiện đại / hạ tầng cloud ở mức sâu; tôi đang học và đã tự triển khai một service lên Kubernetes cho dự án cá nhân."
- "Thuyết trình trước đông người là điểm tôi còn phải cố gắng; tôi đăng ký chia sẻ nội bộ mỗi quý để luyện."

**Câu hỏi nối tiếp:**
- "Đồng nghiệp cũ sẽ nói gì về điểm yếu của bạn?" — Nhất quán với câu trả lời.
- "Kết quả cải thiện tới đâu rồi?" — Có dữ kiện.

**⚠️ Câu trả lời gây điểm trừ:** điểm yếu "giả" (cầu toàn, làm việc quá chăm chỉ); điểm yếu chạm vào năng lực cốt lõi của vị trí ("tôi không thích viết test", "tôi ngại giao tiếp với team"); điểm mạnh là danh sách tính từ không có ví dụ.

**📖 Ôn lại:** [8. Chuẩn bị ngoài kỹ thuật](../02-ke-hoach-on-tap.md#8-chuẩn-bị-ngoài-kỹ-thuật)

</details>

### Q35. 🟢 "Bạn thấy mình ở đâu sau 3–5 năm nữa?"

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Nêu **hướng phát triển** rõ ràng — chuyên sâu kỹ thuật (Staff/Principal, Architect) hay quản lý (Tech Lead/Engineering Manager) — gắn với những gì vị trí này có thể giúp bạn đạt được, và thể hiện mong muốn gắn bó, đóng góp. Không cần chắc chắn tuyệt đối, nhưng phải có suy nghĩ.

**Giải thích chi tiết:**

*Interviewer muốn đánh giá:* tham vọng có phù hợp với lộ trình công ty không, bạn có ở lại đủ lâu không, bạn có tự định hướng không.

*Khung trả lời:*
1. Hướng đi (kỹ thuật chuyên sâu / dẫn dắt team / kết hợp).
2. Năng lực muốn xây trong 1–2 năm đầu (vd: thiết kế hệ thống phân tán, dẫn dắt dự án liên team).
3. Kết nối với công ty: "Tôi thấy công ty đang mở rộng sang…, đó là môi trường phù hợp để…"

*Ví dụ mẫu (dàn ý):* "Trong 1–2 năm tới tôi muốn làm chủ hệ thống của team và dẫn dắt các quyết định kỹ thuật lớn, đồng thời giúp các bạn trong team phát triển. Trong 3–5 năm, tôi hướng tới vai trò Staff Engineer/Architect — người chịu trách nhiệm chất lượng kỹ thuật ở mức nhiều team. Lộ trình career ladder mà anh/chị chia sẻ khá phù hợp với hướng đó."

**Câu hỏi nối tiếp:**
- "Vì sao không muốn làm quản lý?" (hoặc ngược lại) — Nêu lý do dựa trên điều bạn thích làm hằng ngày.
- "Nếu công ty không có vị trí đó trong 3 năm?"

**⚠️ Câu trả lời gây điểm trừ:** "tôi muốn mở công ty riêng" (với vị trí cần gắn bó dài); "ngồi ghế của anh/chị" kiểu đùa thiếu chuyên nghiệp; "tôi chưa nghĩ tới".

**📖 Ôn lại:** [8. Chuẩn bị ngoài kỹ thuật](../02-ke-hoach-on-tap.md#8-chuẩn-bị-ngoài-kỹ-thuật)

</details>

### Q36. 🔴 "CV của bạn có khoảng trống 8 tháng / nhảy việc 3 lần trong 3 năm / vừa bị layoff. Bạn giải thích thế nào?"

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Trả lời **ngắn, trung thực, không phòng thủ**, rồi chuyển nhanh sang **điều bạn đã làm/học được** và **vì sao lần này khác** (lý do bạn muốn gắn bó với công ty này). Layoff không phải lỗi cá nhân — nói như một sự kiện kinh doanh. Nhảy việc nhiều: chỉ ra mạch phát triển hợp lý hoặc thừa nhận bài học về cách chọn công ty.

**Giải thích chi tiết:**

*Interviewer muốn đánh giá:* rủi ro gắn bó, sự trung thực, cách bạn xử lý giai đoạn khó khăn.

*Theo tình huống:*
- **Layoff:** "Công ty cắt giảm khoảng 30% nhân sự do tái cấu trúc, toàn bộ team sản phẩm X bị giải thể. Tôi đã bàn giao đầy đủ và [có thư giới thiệu từ quản lý]." Thị trường IT 2023–2025 có nhiều đợt layoff, interviewer hiểu điều đó.
- **Khoảng trống:** lý do (sức khỏe, gia đình, học thêm, nghỉ ngơi sau burnout, khởi nghiệp) + những gì bạn đã làm để giữ kỹ năng (khóa học, dự án cá nhân, đóng góp open source, freelance) — có bằng chứng (repo GitHub, chứng chỉ).
- **Nhảy việc nhiều:** nếu có lý do khách quan (dự án kết thúc, công ty đóng cửa) nói rõ; nếu là lựa chọn của bạn, thừa nhận và nói bạn đã rút ra tiêu chí chọn việc gì — và vì sao công ty này phù hợp tiêu chí đó.

*Ví dụ mẫu (dàn ý):* "Tôi rời công ty A sau 10 tháng vì dự án outsource kết thúc và công ty không có dự án Java phù hợp; công ty B là startup đóng cửa sau 8 tháng. Tôi nhận ra mình cần môi trường ổn định và sản phẩm dài hạn hơn, nên lần này tôi tìm hiểu kỹ về tình hình và lộ trình sản phẩm — đó là lý do tôi hỏi nhiều về điều đó ở vòng trước."

**Câu hỏi nối tiếp:**
- "Làm sao chúng tôi biết bạn sẽ không nghỉ sau 1 năm?" — Nêu tiêu chí chọn việc và lý do cụ thể công ty này đáp ứng.
- "Trong thời gian nghỉ bạn cập nhật kỹ năng thế nào?"

**⚠️ Câu trả lời gây điểm trừ:** nói dối về ngày tháng (rất dễ bị phát hiện qua kiểm tra tham chiếu/sổ BHXH); đổ lỗi cho tất cả các công ty cũ; trả lời dài dòng, phòng thủ, cảm xúc.

**📖 Ôn lại:** [8. Chuẩn bị ngoài kỹ thuật](../02-ke-hoach-on-tap.md#8-chuẩn-bị-ngoài-kỹ-thuật)

</details>

---

<a id="g10"></a>
## 10. Câu hỏi ngược cho interviewer

### Q37. 🟢 "Bạn có câu hỏi gì cho chúng tôi không?" — nên hỏi gì?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **Luôn có câu hỏi** — đây là cơ hội đánh giá công ty và thể hiện bạn suy nghĩ như Senior. Chuẩn bị 5–8 câu, chọn 2–4 câu phù hợp với **người đang phỏng vấn** (engineer, lead, manager, HR). Ưu tiên câu hỏi về cách làm việc thực tế, không phải những gì đã có trên website.

**Giải thích chi tiết:**

*Interviewer muốn đánh giá:* mức độ quan tâm nghiêm túc, góc nhìn của bạn về điều quan trọng trong một team kỹ thuật.

*Câu hỏi gợi ý theo chủ đề:*

| Chủ đề | Câu hỏi |
|---|---|
| Kỳ vọng vai trò | "Sau 3 tháng và 6 tháng, Senior ở vị trí này thành công trông như thế nào?" · "Thách thức kỹ thuật lớn nhất team đang gặp là gì?" |
| Kiến trúc | "Hệ thống hiện tại gồm những phần chính nào, khó khăn lớn nhất về scale/độ tin cậy là gì?" |
| Quy trình kỹ thuật | "Một thay đổi đi từ commit tới production mất bao lâu, qua những bước nào?" · "Team review code thế nào?" · "Tỉ lệ test tự động ra sao?" |
| Vận hành | "On-call tổ chức thế nào, tần suất và có phụ cấp không?" · "Sự cố gần nhất team xử lý và postmortem ra sao?" |
| Ra quyết định | "Quyết định kỹ thuật lớn được đưa ra như thế nào? Engineer có tham gia vào roadmap không?" |
| Phát triển | "Career ladder cho kỹ sư? Người gần nhất được thăng tiến lên Staff/Lead đã làm gì?" · "Ngân sách đào tạo, hội thảo?" |
| Team | "Team gồm bao nhiêu người, cơ cấu thế nào? Vì sao vị trí này đang tuyển (mở rộng hay thay thế)?" |
| Tech debt | "Team dành bao nhiêu thời gian cho tech debt và cải tiến?" |

*Mẹo:* hỏi engineer về thực tế hằng ngày; hỏi manager về kỳ vọng và cách đánh giá; hỏi HR về quy trình, phúc lợi, thời gian ra quyết định. Câu kết hay: "Anh/chị có băn khoăn gì về sự phù hợp của tôi với vị trí này không?" — cho bạn cơ hội giải tỏa ngay.

**Câu hỏi nối tiếp:** — (đây là phần bạn hỏi; hãy lắng nghe và hỏi tiếp sâu hơn dựa trên câu trả lời, đó là tín hiệu tốt).

**⚠️ Câu trả lời gây điểm trừ:** "Không, em không có câu hỏi gì"; chỉ hỏi về lương, phép, giờ về ở vòng kỹ thuật; hỏi những thứ có ngay trên website công ty.

**📖 Ôn lại:** [8. Chuẩn bị ngoài kỹ thuật](../02-ke-hoach-on-tap.md#8-chuẩn-bị-ngoài-kỹ-thuật)

</details>

### Q38. 🟡 "Qua buổi phỏng vấn, những dấu hiệu nào cho thấy công ty/team không phù hợp (red flags)?"

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Phỏng vấn là **hai chiều**. Red flags: không ai trả lời được rõ quy trình release/on-call; "chúng tôi không có thời gian viết test"; mọi thứ đều "gấp"; người phỏng vấn thiếu tôn trọng hoặc đến trễ không báo; mô tả công việc thay đổi giữa các vòng; tỉ lệ nghỉ việc cao/"thay thế người vừa nghỉ" liên tục; né câu hỏi về lương/hợp đồng/bảo hiểm; ép nhận offer ngay.

**Giải thích chi tiết:**

*Vì sao Senior cần nghĩ về điều này:* chọn sai môi trường tốn 1–2 năm sự nghiệp. Các câu hỏi ở Q37 chính là công cụ để phát hiện red flags.

*Danh sách kiểm tra:*
- **Kỹ thuật:** deploy thủ công, không có môi trường staging, không có monitoring, "hotfix trên production là bình thường", không review code.
- **Văn hóa:** đổ lỗi khi có sự cố (không có postmortem blameless); OT là chuẩn mực ("ở đây mọi người đều nhiệt huyết"); quyết định kỹ thuật hoàn toàn từ trên xuống.
- **Tuyển dụng:** quy trình lộn xộn, không phản hồi; bài test về nhà quá lớn (giống làm việc miễn phí); người phỏng vấn không hiểu vị trí.
- **Hợp đồng & lương (Việt Nam):** đề nghị đóng bảo hiểm trên mức lương rất thấp mà không nói rõ; thử việc dài hơn quy định; lương thử việc dưới 85%; không có offer letter bằng văn bản; giữ bằng cấp gốc (pháp luật cấm người sử dụng lao động giữ bản chính giấy tờ tùy thân, văn bằng, chứng chỉ của người lao động).
- **Tài chính công ty:** startup không nói được runway; trả lương trễ (hỏi qua cộng đồng/người quen).

*Cách kiểm chứng:* hỏi xin nói chuyện với một engineer trong team; tìm hiểu qua LinkedIn những người từng làm ở đó; đọc review (có chọn lọc) trên các trang đánh giá công ty.

**Câu hỏi nối tiếp:**
- "Nếu offer rất tốt nhưng có vài red flag?" — Cân nhắc mức độ nghiêm trọng; hỏi thẳng để làm rõ; đặt điều kiện vào hợp đồng.

**⚠️ Câu trả lời gây điểm trừ (khi tự đánh giá):** chỉ nhìn con số lương; bỏ qua cảm giác không ổn về người quản lý trực tiếp — người quản lý trực tiếp ảnh hưởng lớn nhất tới trải nghiệm làm việc.

**📖 Ôn lại:** [8. Chuẩn bị ngoài kỹ thuật](../02-ke-hoach-on-tap.md#8-chuẩn-bị-ngoài-kỹ-thuật)

</details>

---

<a id="g11"></a>
## 11. Tình huống tổng hợp

### Q39. 🔴🎬 "Bạn được tuyển làm Senior cho một team đang có hệ thống không ổn định. Kế hoạch 90 ngày đầu của bạn là gì?"

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **30 ngày đầu — học và lắng nghe:** hiểu domain, kiến trúc, con người, số liệu (sự cố, SLO, lead time), làm vài task nhỏ để nắm quy trình; chưa đề xuất thay đổi lớn. **30–60 ngày — thắng nhỏ có ý nghĩa:** chọn 1–2 vấn đề gây đau nhất (vd alert ồn, sự cố lặp lại), sửa và đo. **60–90 ngày — đề xuất lộ trình:** dựa trên dữ liệu, viết kế hoạch cải thiện độ tin cậy cho 2–3 quý, thống nhất với lead/PO, bắt đầu nhân rộng qua quy trình và mentoring.

**Giải thích chi tiết:**

*Interviewer muốn đánh giá:* bạn có tạo tác động nhanh mà không "đập đi xây lại" một cách kiêu ngạo, có tôn trọng bối cảnh và con người hiện tại không.

*Chi tiết từng giai đoạn:*
1. **Ngày 1–30:**
   - 1-1 với từng thành viên, PO, on-call: "Điều gì khiến bạn mất ngủ nhất về hệ thống?"
   - Đọc postmortem, sơ đồ kiến trúc, dashboard; tham gia on-call shadow.
   - Ship một thay đổi nhỏ tới production để hiểu toàn bộ quy trình.
   - Ghi chép "điều tôi thấy lạ" — giá trị của góc nhìn người mới, nhưng chưa kết luận vội.
2. **Ngày 31–60:**
   - Phân loại sự cố 6 tháng gần nhất theo nguyên nhân gốc → chọn nhóm có tần suất × tác động cao nhất.
   - Thắng nhỏ: dọn alert (chỉ giữ alert có hành động), thêm timeout/circuit breaker cho phụ thuộc hay lỗi, runbook cho 3 sự cố phổ biến.
   - Xây quan hệ tin cậy: review code, hỗ trợ khi có sự cố.
3. **Ngày 61–90:**
   - Đề xuất SLO cho các luồng quan trọng, error budget, quy trình postmortem.
   - Lộ trình kỹ thuật (tech debt ưu tiên theo dữ liệu) — thống nhất phân bổ capacity với PO.
   - Kế hoạch nâng năng lực team (on-call training, chia sẻ kiến thức).

*Đo thành công:* số sự cố SEV1/SEV2, MTTR, số alert/tuần, tỉ lệ deploy gây lỗi — so sánh trước/sau.

**Câu hỏi nối tiếp:**
- "Team không muốn thay đổi?" — Bắt đầu bằng vấn đề họ đang đau nhất; để kết quả tự nói; ghi nhận đóng góp của người cũ.
- "Lead hiện tại có quan điểm khác?" — Đồng hành, đề xuất qua dữ liệu, không vượt quyền.

**⚠️ Câu trả lời gây điểm trừ:** "tuần đầu tôi sẽ đề xuất viết lại bằng microservices"; chỉ nói về kỹ thuật mà bỏ qua con người; kế hoạch không có cách đo.

**📖 Ôn lại:** [Module 16 — Xử lý sự cố & văn hóa postmortem](../01-giao-trinh/16-devops-build-cloud-security.md#p12) · [Module 16 — Observability trong thực tế](../01-giao-trinh/16-devops-build-cloud-security.md#p9) · [Module 14 — Observability](../01-giao-trinh/14-microservices-system-design.md#p7)

</details>

### Q40. 🟡🎬 "Một đồng nghiệp cùng cấp thường xuyên không hợp tác: trả lời chậm, review qua loa, không chia sẻ thông tin về module họ phụ trách. Bạn làm gì?"

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Bắt đầu bằng **cuộc nói chuyện riêng, giả định thiện chí**: mô tả hành vi cụ thể và tác động (không gán nhãn tính cách), hỏi để hiểu nguyên nhân (quá tải? ưu tiên khác? cảm thấy bị đe dọa?), cùng thống nhất cách làm việc. Giải quyết **vấn đề hệ thống** (bus factor, quy trình review, tài liệu). Chỉ escalate tới manager khi đã thử trực tiếp mà không cải thiện và vấn đề ảnh hưởng tới kết quả team — kèm dữ kiện cụ thể.

**Giải thích chi tiết:**

*Interviewer muốn đánh giá:* trí tuệ cảm xúc, kỹ năng phản hồi, khả năng giải quyết xung đột mà không leo thang không cần thiết.

*Khung phản hồi SBI (Situation – Behavior – Impact):*
> "Ở sprint vừa rồi (S), PR thay đổi module thanh toán của mình chờ review 4 ngày và nhận approve không có góp ý (B), nên bug về làm tròn tiền lọt lên staging và mình phải làm lại (I). Có chuyện gì khiến bạn khó sắp xếp thời gian review không?"

*Giải pháp hệ thống (để không phụ thuộc thiện chí cá nhân):*
- SLA review và luân phiên reviewer trong team.
- Tài liệu và buổi chia sẻ kiến thức cho module chỉ một người nắm (giảm bus factor — cũng là rủi ro vận hành).
- Pair programming trên module đó.
- Làm rõ ownership trong team (CODEOWNERS).

*Ví dụ mẫu (dàn ý):* sau khi nói chuyện, tôi biết đồng nghiệp đang bị kéo vào hỗ trợ một dự án khác và quá tải. Chúng tôi cùng đề xuất với lead điều chỉnh phân bổ; anh ấy dành một buổi chia sẻ về module, tôi viết lại tài liệu và trở thành reviewer thứ hai cho module đó. Thời gian chờ review giảm, và rủi ro "chỉ một người biết" cũng được giải quyết.

**Câu hỏi nối tiếp:**
- "Nếu nói chuyện rồi vẫn không thay đổi?" — Ghi nhận dữ kiện cụ thể (không phải cảm nhận), trao đổi với manager như một vấn đề ảnh hưởng tới team, đề xuất giải pháp chứ không chỉ phàn nàn.
- "Nếu người đó là lead của bạn?" — Vẫn phản hồi trực tiếp, tôn trọng; dùng 1-1 định kỳ.

**⚠️ Câu trả lời gây điểm trừ:** báo ngay với sếp mà chưa nói chuyện trực tiếp; nói xấu trong nhóm chat; "tôi tự làm luôn phần của anh ấy cho nhanh"; gán nhãn ("anh ấy lười").

**📖 Ôn lại:** [8. Chuẩn bị ngoài kỹ thuật](../02-ke-hoach-on-tap.md#8-chuẩn-bị-ngoài-kỹ-thuật) · [Module 06 — Code review với tư cách Senior](../01-giao-trinh/06-design-principles-patterns.md#p12)

</details>

---

## Checklist trước buổi phỏng vấn

- [ ] Bài giới thiệu 2 phút đã luyện thành tiếng ít nhất 5 lần, có số liệu.
- [ ] 6–8 câu chuyện STAR viết sẵn, mỗi câu có: vai trò "tôi", số liệu, bài học đã áp dụng lại.
- [ ] Mỗi câu chuyện chịu được 3 tầng câu hỏi "vì sao / cụ thể bạn làm gì / nếu làm lại".
- [ ] Đã chuẩn bị: lý do nghỉ việc (tích cực, nhất quán), khoảng lương **gross** dựa trên dữ liệu thị trường, ngày có thể bắt đầu (tính đúng thời gian báo trước).
- [ ] 5–8 câu hỏi ngược, chia theo người phỏng vấn.
- [ ] Đã đọc kỹ JD và tìm hiểu sản phẩm, tin tức gần đây, blog kỹ thuật (nếu có) của công ty.

📖 Kế hoạch tổng thể: [Kế hoạch ôn tập — 8. Chuẩn bị ngoài kỹ thuật](../02-ke-hoach-on-tap.md#8-chuẩn-bị-ngoài-kỹ-thuật)

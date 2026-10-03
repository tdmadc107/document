# Kế hoạch ôn tập Senior Java

> Kế hoạch được xây dựng dựa trên 17 module trong [`01-giao-trinh/`](01-giao-trinh/).
> **Bản chuẩn: 12 tuần** (≈ 2 giờ/ngày thường + 5 giờ/ngày cuối tuần ≈ 20 giờ/tuần, tổng ≈ 240 giờ).
> **Bản rút gọn: 4 tuần** cho người đã có lịch phỏng vấn gần (xem cuối file).

## Mục lục
1. [Nguyên tắc ôn tập](#1-nguyên-tắc-ôn-tập)
2. [Tự đánh giá đầu vào](#2-tự-đánh-giá-đầu-vào-ngày-0)
3. [Nhịp một ngày, một tuần](#3-nhịp-một-ngày-một-tuần)
4. [Kế hoạch 12 tuần chi tiết](#4-kế-hoạch-12-tuần-chi-tiết)
5. [Các mốc kiểm tra (milestone)](#5-các-mốc-kiểm-tra-milestone)
6. [Bảng theo dõi tiến độ](#6-bảng-theo-dõi-tiến-độ)
7. [Kế hoạch rút gọn 4 tuần](#7-kế-hoạch-rút-gọn-4-tuần)
8. [Chuẩn bị ngoài kỹ thuật](#8-chuẩn-bị-ngoài-kỹ-thuật)

---

## 1. Nguyên tắc ôn tập

1. **Đọc → Làm → Giải thích.** Mỗi phần lý thuyết: đọc (40%), làm bài tập ngay (40%), tự giải thích lại bằng lời không nhìn tài liệu (20%). Nếu không giải thích được cho "người mới vào nghề" thì chưa hiểu.
2. **Code thật, chạy thật.** Tạo repo `senior-java-lab` (Maven multi-module, mỗi module giáo trình một sub-module). Bài tập nào cũng phải có test hoặc `main` chạy được. Với Spring/DB/Redis/Kafka, dùng `docker compose` hoặc Testcontainers.
3. **Spaced repetition.** Ghi mỗi khái niệm khó thành một thẻ hỏi–đáp (Anki hoặc file `flashcards.md`). Ôn lại theo nhịp **D+1, D+3, D+7, D+21**.
4. **Ưu tiên chiều sâu ở phần "hay bị hỏi xoáy".** HashMap, Concurrency, JVM/GC, Spring bean lifecycle + AOP + @Transactional, JPA N+1, Index + Isolation, Cache consistency, Kafka delivery semantics, Microservices resilience. Đây là các module có hệ số ×1.5 thời lượng trong kế hoạch.
5. **Gắn với kinh nghiệm thật.** Mỗi module, viết ra 1–2 tình huống bạn từng gặp trong dự án (bug, sự cố, quyết định thiết kế) theo khung **STAR** (Situation, Task, Action, Result). Senior được đánh giá qua câu chuyện thực tế nhiều hơn định nghĩa.
6. **Không bỏ checklist.** Cuối module phải tick ≥ 90% "Checklist tự đánh giá" mới chuyển module. Mục chưa đạt đưa vào danh sách "nợ" của tuần đệm.

## 2. Tự đánh giá đầu vào (Ngày 0)

Chấm điểm 1–5 cho từng module (1: chưa biết, 3: dùng được hằng ngày, 5: giải thích được internals và trade-off). Mở phần Checklist cuối mỗi module để tự chấm cho khách quan.

| Module | Điểm đầu vào | Điểm mục tiêu | Ghi chú |
|---|---|---|---|
| 01 Java Core & OOP | | 5 | |
| 02 Collections & Generics | | 5 | |
| 03 Modern Java 8–21 | | 4 | |
| 04 Concurrency | | 5 | |
| 05 JVM, GC, Performance | | 4 | |
| 06 Clean Code, SOLID, Patterns | | 5 | |
| 07 Spring Core & Boot | | 5 | |
| 08 Spring MVC, REST, Security | | 4 | |
| 09 JPA/Hibernate & Transactions | | 5 | |
| 10 Struts & legacy | | 3 | 4 nếu công ty mục tiêu dùng Struts |
| 11 Database & SQL | | 5 | |
| 12 Redis & Caching | | 4 | |
| 13 Kafka/RabbitMQ | | 4 | |
| 14 Microservices & System Design | | 4 | |
| 15 Testing | | 4 | |
| 16 Build, Docker, K8s, CI/CD, Security | | 3 | |
| 17 DSA | | 4 | |

**Điều chỉnh kế hoạch:** module nào đầu vào ≥ 4 thì giảm 50% thời lượng (chỉ làm bài tập Nâng cao + checklist); module nào ≤ 2 thì dùng thêm thời gian của tuần đệm (tuần 6 và 12).

## 3. Nhịp một ngày, một tuần

**Ngày thường (≈ 2 giờ)**

| Thời lượng | Hoạt động |
|---|---|
| 15' | Ôn flashcard đến hạn (spaced repetition) |
| 45' | Đọc 1–2 phần lý thuyết của module đang học |
| 45' | Làm bài tập 🛠 của phần vừa đọc (ít nhất mức Cơ bản + Trung bình) |
| 15' | DSA: 1 bài theo pattern của tuần (Module 17) |

**Cuối tuần (≈ 5 giờ/ngày)**

| Thứ 7 | Chủ nhật |
|---|---|
| Bài tập mức Nâng cao còn nợ trong tuần | Dự án mini của module (nếu module kết thúc trong tuần) |
| 2 bài DSA mức Trung bình/Khó | Tự phỏng vấn: trả lời thành tiếng 15–20 câu hỏi của module (khi bộ câu hỏi đã có), ghi âm và nghe lại |
| Đọc chương sách PDF tương ứng (xem mục "Nguồn tham khảo" đầu module) | Tổng kết tuần: cập nhật bảng tiến độ, viết 1 câu chuyện STAR |

## 4. Kế hoạch 12 tuần chi tiết

> Ký hiệu: **M01** = Module 01... "Phần x–y" là các mục lý thuyết đánh số trong module.
> DSA chạy song song suốt 12 tuần theo cột riêng.

### Giai đoạn 1 — Nền tảng Java (Tuần 1–5)

| Tuần | Lý thuyết + bài tập | DSA (M17) | Đầu ra cuối tuần |
|---|---|---|---|
| **1** | **M01 Java Core & OOP**: JVM/JDK, kiểu dữ liệu, String, OOP, Object methods (equals/hashCode), immutability, khởi tạo, enum, exception, I/O | Big-O, Arrays & Strings, Two pointers | Dự án mini M01; flashcard ≥ 30 thẻ |
| **2** | **M02 Collections & Generics**: HashMap internals (tự cài đặt một HashMap đơn giản), LinkedHashMap/LRU, TreeMap, Queue, fail-fast, Generics/PECS | Hashing, Sliding window, Prefix sum | Dự án mini M02 |
| **3** | **M03 Modern Java 8→21**: Lambda, Stream (Collectors, parallel), Optional, java.time, record/sealed/pattern matching | Stack, Monotonic stack, Queue | Dự án mini M03; viết lại 1 đoạn code cũ của dự án bằng Stream/record |
| **4** | **M04 Concurrency (phần 1)**: Thread, JMM, happens-before, synchronized, volatile, locks, atomics, deadlock | Linked list, Binary search | Tái hiện một deadlock và đọc thread dump bằng `jstack` |
| **5** | **M04 Concurrency (phần 2)**: ThreadPoolExecutor, CompletableFuture, synchronizers, ThreadLocal, Virtual Threads · **M05 JVM (phần 1)**: class loading, memory areas | Trees (traversal, BST) | Dự án mini M04 |

### Giai đoạn 2 — JVM, thiết kế & Spring (Tuần 6–8)

| Tuần | Lý thuyết + bài tập | DSA (M17) | Đầu ra cuối tuần |
|---|---|---|---|
| **6** | **M05 JVM (phần 2)**: GC (G1, ZGC), memory leak, OOM, JVM flags, công cụ chẩn đoán (jcmd, JFR, MAT), JMH · **Tuần đệm 1**: trả nợ checklist M01–M04 | Heap / Top-K | Dự án mini M05: gây OOM có chủ đích, phân tích heap dump; **Milestone 1** |
| **7** | **M06 Clean Code, SOLID, Design Patterns, Architecture** · **M07 Spring Core & Boot (phần 1)**: IoC/DI, bean scope, bean lifecycle | Graph BFS/DFS, Topological sort | Dự án mini M06 |
| **8** | **M07 (phần 2)**: AOP & proxy, auto-configuration, custom starter, Actuator · **M08 Spring MVC, REST, Security** | Union-Find, Dijkstra | Dự án mini M07 + M08 (REST API có JWT, validation, ProblemDetail) |

### Giai đoạn 3 — Dữ liệu & hệ thống phân tán (Tuần 9–11)

| Tuần | Lý thuyết + bài tập | DSA (M17) | Đầu ra cuối tuần |
|---|---|---|---|
| **9** | **M09 JDBC, JPA/Hibernate, Transactions** (N+1, locking, propagation) · **M11 Database & SQL (phần 1)**: SQL nâng cao, index, execution plan | Backtracking, Greedy | Dự án mini M09; chứng minh N+1 bằng log SQL rồi sửa |
| **10** | **M11 (phần 2)**: isolation, MVCC, locking, deadlock DB, pagination, replication/partition · **M12 Redis & Caching** | Dynamic programming (1D, 2D) | Dự án mini M11 + M12 (cache-aside, distributed lock, rate limiter); **Milestone 2** |
| **11** | **M13 Kafka/RabbitMQ** · **M14 Microservices & System Design** (resilience, saga, outbox, observability, thiết kế hệ thống) | Intervals, Trie, Bit manipulation | Dự án mini M13 + M14; vẽ 2 bản thiết kế hệ thống (URL shortener, order/payment) |

### Giai đoạn 4 — Hoàn thiện & luyện phỏng vấn (Tuần 12)

| Ngày | Nội dung |
|---|---|
| T2 | **M15 Testing**: JUnit 5, Mockito, Testcontainers, test slices. Bổ sung test cho các dự án mini còn thiếu |
| T3 | **M16 Build, Docker, K8s, CI/CD, Security**: đóng gói 1 dự án mini thành image, viết manifest K8s có probes |
| T4 | **M10 Struts & legacy** (đọc nhanh nếu công ty mục tiêu không dùng Struts; học kỹ + làm bài migrate nếu có) |
| T5 | Ôn tổng hợp: lướt lại toàn bộ "💡 Góc nhìn Senior" và "⚠️ Lỗi thường gặp" của 17 module |
| T6 | Luyện bộ câu hỏi phỏng vấn ([`03-cau-hoi-phong-van/`](03-cau-hoi-phong-van/)): chọn ngẫu nhiên 40 câu, trả lời thành tiếng |
| T7 | Luyện case study ([`04-case-study/`](04-case-study/)): tự giải 3 case trong 45' mỗi case trước khi xem lời giải |
| CN | **Milestone 3**: mock interview đầy đủ (xem mục 5) |

> Nếu module 10 (Struts) là trọng tâm của công ty mục tiêu (ngân hàng, bảo hiểm, cơ quan nhà nước), chuyển M10 lên **tuần 8** và dời M08 phần Security sang tuần 12.

## 5. Các mốc kiểm tra (milestone)

| Mốc | Thời điểm | Hình thức | Tiêu chí đạt |
|---|---|---|---|
| **Milestone 1** — Java Core | Cuối tuần 6 | 60' tự kiểm tra: 20 câu hỏi M01–M05 + 1 bài code concurrency (ví dụ: bounded blocking queue bằng `ReentrantLock` + `Condition`) + 1 bài DSA medium | ≥ 80% câu trả lời đúng và giải thích được "tại sao"; code chạy đúng với test đa luồng |
| **Milestone 2** — Spring & Data | Cuối tuần 10 | Xây trong 1 ngày: REST API "đặt hàng" Spring Boot + JPA + PostgreSQL + Redis cache + optimistic locking, có integration test bằng Testcontainers | API chạy, không N+1, không race condition khi 50 request đồng thời mua cùng sản phẩm, test xanh |
| **Milestone 3** — Mock interview | Cuối tuần 12 | 90': 10' giới thiệu + 30' kỹ thuật xoáy sâu + 25' system design + 20' coding + 5' hỏi ngược. Nhờ đồng nghiệp đóng vai interviewer, hoặc tự ghi âm | Trả lời có cấu trúc (định nghĩa → cơ chế → trade-off → ví dụ thực tế); thiết kế có ước lượng tải và xử lý lỗi; coding xong trong thời gian |

Không đạt mốc nào thì dùng 3–5 ngày tiếp theo để ôn lại đúng phần yếu trước khi đi tiếp.

## 6. Bảng theo dõi tiến độ

Copy bảng này vào file riêng (ví dụ `tien-do.md`) và cập nhật mỗi Chủ nhật.

| Module | Lý thuyết | Bài tập Cơ bản | Bài tập TB | Bài tập Nâng cao | Dự án mini | Checklist ≥ 90% | Ngày xong |
|---|---|---|---|---|---|---|---|
| M01 | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | |
| M02 | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | |
| M03 | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | |
| M04 | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | |
| M05 | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | |
| M06 | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | |
| M07 | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | |
| M08 | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | |
| M09 | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | |
| M10 | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | |
| M11 | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | |
| M12 | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | |
| M13 | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | |
| M14 | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | |
| M15 | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | |
| M16 | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | |
| M17 | ☐ | ☐ | ☐ | ☐ | ☐ | ☐ | |

**Chỉ số tuần:** số giờ học thực tế · số bài tập hoàn thành · số bài DSA · số flashcard mới · 1 câu chuyện STAR mới.

## 7. Kế hoạch rút gọn 4 tuần

Dành cho trường hợp đã có lịch phỏng vấn. Học ≈ 4 giờ/ngày. Chỉ làm bài tập **Trung bình + Nâng cao**, bỏ qua Dự án mini (trừ Milestone 2 rút gọn).

| Tuần | Nội dung | Ưu tiên |
|---|---|---|
| 1 | M01 (equals/hashCode, String, immutability, exception) · M02 (HashMap, ConcurrentHashMap, Generics) · M03 (Stream, Optional, record) · M04 (JMM, volatile, synchronized, thread pool, CompletableFuture) | Những phần có 💡 Góc nhìn Senior |
| 2 | M05 (memory areas, GC G1/ZGC, OOM, công cụ chẩn đoán) · M06 (SOLID, Singleton/Factory/Strategy/Proxy/Builder/Observer) · M07 (bean lifecycle, AOP proxy, auto-config) · M08 (request flow, Security filter chain, JWT) | Spring AOP + @Transactional |
| 3 | M09 (N+1, persistence context, propagation, locking) · M11 (index, execution plan, isolation, MVCC) · M12 (cache patterns, consistency, distributed lock) · M13 (Kafka partition/consumer group/delivery semantics, outbox) | Data consistency |
| 4 | M14 (resilience, saga, system design) · lướt M15, M16, M10 · luyện câu hỏi phỏng vấn + case study · 2 buổi mock interview | Kể chuyện thực tế theo STAR |

DSA trong bản rút gọn: mỗi ngày 1 bài theo danh sách luyện tập cuối Module 17, ưu tiên mức Medium.

## 8. Chuẩn bị ngoài kỹ thuật

- **Giới thiệu bản thân 2 phút:** vai trò, quy mô hệ thống (QPS, số user, dữ liệu), đóng góp nổi bật có số liệu.
- **3–5 câu chuyện STAR** bắt buộc có: một sự cố production bạn xử lý, một quyết định kiến trúc có trade-off, một lần mentor/review code cho junior, một lần bất đồng kỹ thuật và cách giải quyết, một lần tối ưu hiệu năng có số đo trước/sau.
- **Câu hỏi ngược cho interviewer:** kiến trúc hiện tại, quy trình release, on-call, cách đo chất lượng, kỳ vọng với Senior trong 6 tháng đầu.
- **Ngày trước phỏng vấn:** chỉ ôn flashcard và câu chuyện STAR, không học kiến thức mới.

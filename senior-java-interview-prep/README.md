# Ôn luyện phỏng vấn Senior Java

Bộ tài liệu tự học để ôn luyện và phỏng vấn vị trí **Senior Java Developer**, viết bằng tiếng Việt, giữ nguyên thuật ngữ tiếng Anh.
Nguồn tham khảo chính là các sách PDF trong kho này (`Java/`, `Ebook IT/`, `Algorithms/`) và tài liệu chính thức (Oracle/OpenJDK, Spring, Hibernate, Redis, Kafka...).

## Cấu trúc

| Thư mục / file | Nội dung | Trạng thái |
|---|---|---|
| [`01-giao-trinh/`](01-giao-trinh/) | Giáo trình lý thuyết, mỗi phần có bài tập thực hành + gợi ý lời giải, dự án mini và checklist cuối module | ✅ |
| [`02-ke-hoach-on-tap.md`](02-ke-hoach-on-tap.md) | Kế hoạch ôn tập 12 tuần (và bản rút gọn 4 tuần) | ✅ |
| `03-cau-hoi-phong-van/` | Bộ câu hỏi phỏng vấn kèm đáp án theo từng module | ⏳ đang soạn |
| `04-case-study/` | Case study thực tế Senior thường gặp trên production | ⏳ đang soạn |

## Lộ trình giáo trình

| # | Module | Trọng tâm |
|---|---|---|
| 01 | [Java Core & OOP](01-giao-trinh/01-java-core-oop.md) | JVM/JDK, String, OOP, equals/hashCode, immutability, Exception, I/O |
| 02 | [Collections & Generics](01-giao-trinh/02-collections-generics.md) | HashMap internals, List/Set/Queue, iterator, type erasure, PECS |
| 03 | [Modern Java 8 → 21](01-giao-trinh/03-modern-java-8-21.md) | Lambda, Stream, Optional, java.time, record, sealed, pattern matching |
| 04 | [Concurrency](01-giao-trinh/04-concurrency.md) | JMM, synchronized/volatile, locks, thread pool, CompletableFuture, Virtual Threads |
| 05 | [JVM, Memory, GC & Performance](01-giao-trinh/05-jvm-memory-gc-performance.md) | Class loading, heap, G1/ZGC, memory leak, OOM, JFR, troubleshooting |
| 06 | [Clean Code, SOLID, Design Patterns](01-giao-trinh/06-design-principles-patterns.md) | SOLID, GoF patterns, Clean/Hexagonal Architecture, DDD |
| 07 | [Spring Core & Spring Boot](01-giao-trinh/07-spring-core-boot.md) | IoC/DI, bean lifecycle, AOP, auto-configuration, Actuator |
| 08 | [Spring MVC, REST & Security](01-giao-trinh/08-spring-web-rest-security.md) | DispatcherServlet, REST design, Spring Security, JWT, OAuth2 |
| 09 | [JDBC, JPA/Hibernate & Transactions](01-giao-trinh/09-jdbc-jpa-hibernate-transactions.md) | Persistence context, N+1, locking, @Transactional propagation |
| 10 | [Apache Struts & hệ thống legacy](01-giao-trinh/10-struts.md) | Struts 1/2, OGNL CVE, migrate sang Spring Boot |
| 11 | [Database & SQL](01-giao-trinh/11-database-sql.md) | Index, execution plan, isolation, MVCC, locking, Oracle/MySQL/PostgreSQL |
| 12 | [Redis & Caching](01-giao-trinh/12-redis-caching.md) | Data structures, persistence, cluster, cache patterns, distributed lock |
| 13 | [Messaging: Kafka, RabbitMQ](01-giao-trinh/13-messaging-kafka-rabbitmq.md) | Partition, consumer group, delivery semantics, outbox, DLT |
| 14 | [Microservices & System Design](01-giao-trinh/14-microservices-system-design.md) | Resilience, Saga, CQRS, observability, thiết kế hệ thống |
| 15 | [Testing](01-giao-trinh/15-testing.md) | JUnit 5, Mockito, Testcontainers, Spring test slices, TDD |
| 16 | [Build, Docker, K8s, CI/CD, Security](01-giao-trinh/16-devops-build-cloud-security.md) | Maven, Git, container, probes, OWASP, observability |
| 17 | [Cấu trúc dữ liệu & Giải thuật](01-giao-trinh/17-data-structures-algorithms.md) | Pattern giải bài coding interview bằng Java |

## Cách dùng bộ tài liệu

1. Làm theo [kế hoạch ôn tập](02-ke-hoach-on-tap.md): mỗi ngày đọc lý thuyết rồi **làm bài tập ngay** cho phần vừa đọc, không đợi hết module.
2. Code bài tập trong một repo riêng (ví dụ `senior-java-lab/`, mỗi module một Maven module) để có thể chạy, test và xem lại.
3. Cuối mỗi module làm **Dự án mini** và tick **Checklist tự đánh giá**. Mục nào chưa tick được thì quay lại đọc trước khi sang module sau.
4. Sau khi xong giáo trình, luyện **bộ câu hỏi phỏng vấn** (tự trả lời thành tiếng trước khi mở đáp án) và **case study** (tự viết hướng xử lý trước khi đọc lời giải).

## Quy ước trong giáo trình

- 💡 **Góc nhìn Senior**: trade-off, kinh nghiệm production, điều interviewer muốn nghe ở level Senior.
- ⚠️ **Lỗi thường gặp**: bug/hiểu lầm hay gặp.
- 🛠 **Bài tập**: chia mức *Cơ bản / Trung bình / Nâng cao*. Gợi ý lời giải nằm trong khối thu gọn, hãy tự làm trước.
- Phiên bản mặc định: **Java 17/21 LTS, Spring Boot 3.x, Spring Framework 6.x**. Khác biệt với Java 8 / Spring Boot 2 được ghi chú khi phỏng vấn hay hỏi.

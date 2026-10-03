# Case study thực tế cho Senior Java

Đây là các tình huống production mà Senior thường gặp, dùng để học và để trả lời những câu như *"kể một sự cố bạn đã xử lý"* hay *"nếu gặp X bạn làm gì"*. Các tình huống mang tính minh họa, nhưng số liệu, log và lệnh điều tra đều sát thực tế.

Mỗi case có cấu trúc: bối cảnh → triệu chứng → câu hỏi đặt ra (**dừng lại tự giải trước**) → điều tra từng bước → nguyên nhân gốc → giải pháp ngắn hạn/dài hạn → phòng ngừa → cách kể lại theo STAR → câu hỏi mở rộng.

## JVM, Concurrency, Spring, Database

| # | Case | Module liên quan |
|---|---|---|
| 01 | [Memory leak: heap tăng dần đến OOM](01-memory-leak-oom.md) | 05, 04 |
| 02 | [CPU 100% do regex catastrophic backtracking](02-cpu-100-percent.md) | 05 |
| 03 | [Cạn thread pool vì downstream chậm không có timeout](03-thread-pool-exhaustion.md) | 04, 14 |
| 04 | [`@Transactional` không hoạt động, dữ liệu lưu một nửa](04-transactional-not-working.md) | 07, 09 |
| 05 | [N+1 làm endpoint từ 80 ms lên 6 s](05-n-plus-1-slow-endpoint.md) | 09 |
| 06 | [Flash sale bán vượt tồn kho](06-overselling-race-condition.md) | 09, 11, 12 |
| 07 | [Query chậm dần khi bảng 50 triệu dòng](07-slow-query-data-growth.md) | 11 |
| 08 | [Cạn HikariCP connection pool](08-connection-pool-exhaustion.md) | 09 |
| 09 | [GC pause dài và pod bị OOMKilled](09-gc-pause-container-oomkilled.md) | 05, 16 |

## Hệ thống phân tán, Cache, Messaging, Vận hành

| # | Case | Module liên quan |
|---|---|---|
| 10 | [Cache inconsistency và cache stampede](10-cache-inconsistency-stampede.md) | 12 |
| 11 | [Trừ tiền hai lần: idempotency key](11-duplicate-payment-idempotency.md) | 14, 12 |
| 12 | [Kafka consumer lag, rebalance, poison message](12-kafka-consumer-lag-rebalance.md) | 13 |
| 13 | [Mất event giữa DB và Kafka: transactional outbox](13-lost-events-outbox.md) | 13, 14 |
| 14 | [Cascading failure giữa các microservice](14-cascading-failure-microservices.md) | 14 |
| 15 | [Job `@Scheduled` chạy trùng trên nhiều instance](15-distributed-lock-scheduled-job.md) | 12, 11 |
| 16 | [Migrate monolith Struts sang Spring Boot](16-struts-monolith-migration.md) | 10 |
| 17 | [Đổi schema bảng 200 triệu dòng không downtime](17-zero-downtime-schema-migration.md) | 11, 09 |
| 18 | [Ứng phó Log4Shell trên hàng chục service](18-log4shell-incident-response.md) | 16 |

## Cách luyện

Với mỗi case, chỉ đọc mục 1–3 rồi dành **30–45 phút** tự viết ra: các giả thuyết, công cụ sẽ dùng, cách giảm thiểu ngay, cách sửa tận gốc. Sau đó mới đọc tiếp và so sánh. Cuối cùng, tập kể lại theo khung STAR ở mục 8 trong khoảng 2 phút.

# Câu hỏi phỏng vấn — Module 16: Build, Git, Docker, Kubernetes, CI/CD, Observability & Security

> Giáo trình tương ứng: [Module 16 — Build, Git, Docker, Kubernetes, CI/CD, Observability & Security](../01-giao-trinh/16-devops-build-cloud-security.md)

**Cách dùng:** đọc câu hỏi, **tự trả lời thành tiếng** trước (30 giây cho ý chính, 2–3 phút cho phần cơ chế và trade-off), sau đó mới mở đáp án. Với nhóm Kubernetes, observability và security, hãy luyện kể kèm **một ví dụ production** của chính bạn — interviewer Senior hầu như luôn hỏi "bạn đã gặp chưa, xử lý thế nào?".

**Ký hiệu mức độ:** 🟢 Cơ bản · 🟡 Senior · 🔴 Xoáy sâu · 🎬 Tình huống (scenario)

## Mục lục

1. [Maven & Gradle](#g1) — Q1–Q7
2. [Git](#g2) — Q8–Q13
3. [Linux cho backend developer](#g3) — Q14–Q18
4. [Docker](#g4) — Q19–Q23
5. [JVM trong container](#g5) — Q24–Q26
6. [Kubernetes](#g6) — Q27–Q33
7. [CI/CD](#g7) — Q34–Q37
8. [Observability](#g8) — Q38–Q43
9. [Application security & supply chain](#g9) — Q44–Q53
10. [Sự cố & postmortem](#g10) — Q54–Q55

---

<a id="g1"></a>
## 1. Maven & Gradle

### Q1. 🟢 Phân biệt lifecycle, phase và goal trong Maven. `mvn verify` và `mvn install` khác nhau thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Maven có 3 lifecycle (`clean`, `default`, `site`); mỗi lifecycle là chuỗi **phase** có thứ tự. Phase tự nó không làm gì — **goal** của plugin (`compiler:compile`, `surefire:test`, `jar:jar`) được bind vào phase. Gọi một phase chạy mọi phase trước nó. `verify` chạy tới kiểm tra integration test; `install` thêm bước copy artifact vào `~/.m2` — trong CI thường không cần.

**Giải thích chi tiết:**
- `validate → compile → test-compile → test → package → integration-test → verify → install → deploy`.
- Binding mặc định theo packaging (`jar`), hoặc thêm bằng `<execution>`.
- Gọi goal trực tiếp chỉ chạy goal đó: `mvn dependency:tree`, `mvn spring-boot:run`.
- CI: `mvn -B verify`; `install` ghi vào local repo dùng chung → ô nhiễm giữa build song song. Multi-module trên máy dev: `mvn -pl module-a -am verify` thay vì `install` chữa cháy.

**Câu hỏi nối tiếp:**
- *Xem goal nào chạy ở phase nào?* — `mvn help:describe -Dcmd=verify`, hoặc log `-X`.

**⚠️ Câu trả lời gây điểm trừ:** nghĩ `mvn test` chạy cả integration test, hoặc dùng `mvn clean install` cho mọi việc.

**📖 Ôn lại:** [1.1 Lifecycle, phase, goal](../01-giao-trinh/16-devops-build-cloud-security.md#p1)

</details>

### Q2. 🟢 Các dependency scope của Maven? Khi nào dùng `provided`, khi nào `runtime`?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `compile` (mặc định, mọi classpath, truyền tiếp), `provided` (compile + test, **không đóng gói** vì container cung cấp — `jakarta.servlet-api` khi deploy WAR, Lombok), `runtime` (không cần lúc compile nhưng cần khi chạy — JDBC driver), `test` (junit, mockito), `system` (tránh), `import` (chỉ trong `dependencyManagement` để nhập BOM).

**Giải thích chi tiết:**
- `runtime` cho driver giúp code không vô tình phụ thuộc vào class nội bộ của driver.
- `<optional>true</optional>`: không truyền tiếp — thư viện hỗ trợ cả Jackson lẫn Gson để consumer tự chọn.
- Gradle tương ứng: `implementation`/`api`, `compileOnly` ≈ `provided`, `runtimeOnly` ≈ `runtime`, `testImplementation` ≈ `test`.

**Câu hỏi nối tiếp:**
- *Lombok nên scope gì?* — `provided` (Maven) / `compileOnly` + `annotationProcessor` (Gradle).

**⚠️ Câu trả lời gây điểm trừ:** để mọi thứ scope `compile`, kể cả driver và thư viện test.

**📖 Ôn lại:** [1.2 Dependency scopes](../01-giao-trinh/16-devops-build-cloud-security.md#p1)

</details>

### Q3. 🟡 App build thành công nhưng chạy thì ném `NoSuchMethodError` trong một thư viện. Nguyên nhân thường gặp và cách bạn chẩn đoán, sửa?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Gần như chắc chắn là **xung đột version transitive**. Maven chọn version theo **nearest wins** (gần root nhất), bằng độ sâu thì khai báo trước thắng — có thể chọn bản cũ hơn bản mà một thư viện khác cần → lỗi chỉ lộ lúc runtime (`NoSuchMethodError`, `NoClassDefFoundError`, `AbstractMethodError`). Chẩn đoán bằng `mvn dependency:tree -Dverbose -Dincludes=...`; sửa bằng ghim version qua `dependencyManagement`/BOM và thêm enforcer `dependencyConvergence`.

**Giải thích chi tiết:**

```
my-app
├── lib-a:1.0 → jackson-databind:2.12.0   (depth 2)  ← Maven chọn
└── lib-b:1.0 → lib-c:1.0 → jackson-databind:2.17.0 (depth 3)
```

- `-Dverbose` hiện cả version bị loại ("omitted for conflict with ...").
- Sửa: import `jackson-bom` (BOM áp cho cả transitive) hoặc `<exclusions>`; kiểm tra lib-a còn tương thích với bản mới.
- Phòng ngừa: `maven-enforcer-plugin` với `dependencyConvergence`, `requireJavaVersion`; Gradle mặc định chọn **highest** và có `failOnVersionConflict()`.
- Với Spring Boot: ưu tiên dùng version do BOM Boot quản lý; override bằng property (`<jackson-bom.version>`) thay vì khai báo cứng ở module con.

**Câu hỏi nối tiếp:**
- *`mvn dependency:analyze` để làm gì?* — Tìm dependency dùng mà chưa khai báo (used-undeclared) và khai báo mà không dùng.

**⚠️ Câu trả lời gây điểm trừ:** "Maven luôn chọn version mới nhất" (nhầm với Gradle), hoặc sửa bằng cách copy jar vào classpath.

**📖 Ôn lại:** [1.3 Dependency mediation & conflict](../01-giao-trinh/16-devops-build-cloud-security.md#p1)

</details>

### Q4. 🟡 `dependencyManagement` khác `dependencies` thế nào? BOM là gì, và thứ tự import BOM có quan trọng không?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `<dependencies>` **thêm** dependency vào classpath; `<dependencyManagement>` chỉ **quy định** version/scope/exclusion **nếu** dependency đó xuất hiện (trực tiếp hoặc transitive), module con khai báo không cần version. BOM là POM chỉ chứa `dependencyManagement`, import bằng `<type>pom</type><scope>import</scope>` (Spring Boot, Jackson, JUnit, Testcontainers). Thứ tự import có quan trọng: **BOM khai báo trước thắng** khi cùng quản lý một artifact.

**Giải thích chi tiết:**
- Công ty có parent riêng → không dùng `spring-boot-starter-parent` được → import `spring-boot-dependencies` như BOM.
- Muốn override version Jackson khi import BOM: khai báo `jackson-bom` **trước** BOM Spring Boot. Khi dùng parent Boot: override bằng property.
- BOM nội bộ nên là artifact riêng, không làm parent của chính các module (tránh vòng).

**Câu hỏi nối tiếp:**
- *Gradle tương đương?* — `implementation(platform("...:bom:x"))`, version catalog `libs.versions.toml`, `constraints {}`.

**⚠️ Câu trả lời gây điểm trừ:** khai báo version cứng ở từng module con, lệch với BOM → xung đột âm thầm.

**📖 Ôn lại:** [1.4 `dependencyManagement` vs `dependencies`, BOM](../01-giao-trinh/16-devops-build-cloud-security.md#p1)

</details>

### Q5. 🟢 Surefire và Failsafe khác nhau thế nào? Vì sao CI nên chạy `mvn verify` thay vì `mvn integration-test`?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Surefire chạy unit test (`*Test`, `*Tests`, `Test*`) ở phase `test` và **fail ngay**. Failsafe chạy integration test (`*IT`, `IT*`, `*ITCase`) ở phase `integration-test` nhưng **chỉ báo lỗi ở `verify`** → `post-integration-test` (dọn môi trường, stop server) vẫn kịp chạy. Gọi `mvn integration-test` sẽ dừng trước `verify` → lỗi IT không làm build đỏ.

**Giải thích chi tiết:**
- Pipeline tách stage: `./mvnw -B verify -DskipITs` (nhanh) rồi `failsafe:integration-test failsafe:verify`.
- Thêm `failIfNoTests` để phát hiện trường hợp không test nào chạy (đặt tên sai pattern, thiếu engine JUnit Platform).

**Câu hỏi nối tiếp:**
- *Đặt tên class `OrderIntegrationTest` thì sao?* — Khớp pattern `*Test` của Surefire → chạy như unit test (và có thể chạy 2 lần nếu cấu hình Failsafe include nó).

**⚠️ Câu trả lời gây điểm trừ:** không biết Failsafe tồn tại, để integration test chạy bằng Surefire ở phase `test`.

**📖 Ôn lại:** [1.1 Surefire vs Failsafe](../01-giao-trinh/16-devops-build-cloud-security.md#p1)

</details>

### Q6. 🟡 So sánh Maven và Gradle. `api` và `implementation` trong Gradle khác nhau thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Maven: khai báo XML, lifecycle cố định, nearest-wins, rất chuẩn hóa và dễ dự đoán. Gradle: build script là code (Kotlin/Groovy), DAG task, **incremental build**, **build cache** (local/remote), daemon, configuration cache, highest-wins — nhanh hơn rõ ở monorepo lớn nhưng dễ thành "script spaghetti". `implementation` giấu dependency khỏi compile classpath của consumer (đổi nó không buộc consumer recompile); `api` (plugin `java-library`) lộ dependency nằm trong public API.

**Giải thích chi tiết:**
- Trade-off: **khả năng dự đoán** (Maven) vs **hiệu năng & linh hoạt** (Gradle). Monorepo hàng trăm module: remote build cache có thể giảm CI từ hàng chục phút xuống vài phút.
- Dù chọn gì: wrapper (`mvnw`/`gradlew`), ghim version plugin, BOM/version catalog, reproducible build (`project.build.outputTimestamp`; Gradle `isPreserveFileTimestamps = false`).
- Gradle anti-pattern: việc nặng trong pha configuration; `allprojects {}`/`subprojects {}` thay vì convention plugin; quên `useJUnitPlatform()`.

**Câu hỏi nối tiếp:**
- *`UP-TO-DATE` vs `FROM-CACHE`?* — UP-TO-DATE: output trong workspace còn khớp input; FROM-CACHE: lấy output từ build cache (sau `clean` hoặc từ máy khác).

**⚠️ Câu trả lời gây điểm trừ:** "Gradle luôn tốt hơn" hoặc ngược lại mà không nêu trade-off.

**📖 Ôn lại:** [2. Gradle và so sánh với Maven](../01-giao-trinh/16-devops-build-cloud-security.md#p2)

</details>

### Q7. 🔴 Tổ chức project multi-module Maven cho một Spring Boot service thế nào? Có những bẫy nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Root POM `packaging=pom` vừa là aggregator (`<modules>`) vừa là parent (dependencyManagement, pluginManagement), các module `domain` (không Spring) → `application` → `infrastructure` → `app` (Spring Boot, chỉ module này dùng `spring-boot-maven-plugin repackage`). Bẫy: repackage module thư viện (class bị đẩy vào `BOOT-INF/classes`, module khác không dùng được), version cứng ở module con, SNAPSHOT/version range trong release, không ghim version plugin.

**Giải thích chi tiết:**
- Aggregation ≠ inheritance — thường gộp chung một POM nhưng là hai khái niệm.
- Reactor: `mvn -pl shop-app -am verify` (module và những gì nó cần), `-amd`, `-T 1C`, `-rf :shop-app`.
- `${revision}` + `flatten-maven-plugin` cho CI-friendly version.
- Giữ ranh giới: ArchUnit hoặc `dependency:tree` để chắc `domain` không kéo Spring.
- Reproducible: `project.build.outputTimestamp`, repository manager (Nexus/Artifactory) làm mirror duy nhất.

**Câu hỏi nối tiếp:**
- *Khi nào tách module, khi nào chỉ tách package?* — Tách module khi cần **ép** ranh giới phụ thuộc lúc compile hoặc tái sử dụng; nếu chỉ cần tổ chức code, package + ArchUnit/Spring Modulith nhẹ hơn.

**⚠️ Câu trả lời gây điểm trừ:** module nào cũng có plugin repackage; mọi module phụ thuộc lẫn nhau tùy ý.

**📖 Ôn lại:** [1.5 Multi-module](../01-giao-trinh/16-devops-build-cloud-security.md#p1)

</details>

---

<a id="g2"></a>
## 2. Git

### Q8. 🟢 Merge và rebase khác nhau thế nào? "Luật vàng" của rebase? `--force` khác `--force-with-lease`?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Merge giữ nguyên lịch sử, thêm merge commit; rebase **viết lại** commit (hash mới) lên base mới → lịch sử tuyến tính. Luật vàng: **không rebase commit đã public mà người khác đang dựa vào**. Khi phải push sau rebase branch cá nhân, dùng `--force-with-lease` — từ chối nếu remote có commit mới bạn chưa thấy; `--force` thì ghi đè vô điều kiện, có thể xóa commit của đồng nghiệp.

**Giải thích chi tiết:**
- Rebase có thể phải giải quyết conflict nhiều lần (mỗi commit); merge một lần.
- Squash merge: mỗi PR thành một commit trên `main` — gọn, dễ revert, mất chi tiết.
- Khi rebase, `--ours` là branch đích, `--theirs` là commit đang replay — ngược trực giác.
- `git rerere` nhớ cách giải quyết conflict để áp lại.

**Câu hỏi nối tiếp:**
- *Dọn commit "wip" trước PR?* — `git rebase -i origin/main` hoặc `git commit --fixup=<sha>` + `git rebase -i --autosquash`.

**⚠️ Câu trả lời gây điểm trừ:** `git push --force` lên branch dùng chung "vì rebase xong phải force".

**📖 Ôn lại:** [3.3 Merge vs rebase](../01-giao-trinh/16-devops-build-cloud-security.md#p3)

</details>

### Q9. 🟡 Git Flow và trunk-based development: chọn cái nào? Feature flag có chi phí gì?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Chọn theo **cách release**. Git Flow (`develop`, `release/*`, `hotfix/*`) hợp sản phẩm phát hành theo version, nhiều version song song (thư viện, app cài đặt, mobile). Trunk-based (tích hợp vào `main` ít nhất mỗi ngày, branch sống 1–2 ngày, tính năng dở ẩn sau feature flag) hợp SaaS deploy nhiều lần/ngày và gắn với hiệu suất cao theo DORA — nhưng cần CI nhanh, test tốt, PR nhỏ. Feature flag tốn: nợ kỹ thuật, tổ hợp cấu hình phải test, phải dọn flag sau rollout.

**Giải thích chi tiết:**
- Git Flow nhược: branch sống lâu → merge hell, tích hợp muộn.
- GitHub Flow là bản giản lược: `main` luôn deploy được, PR ngắn.
- Release branch cắt từ trunk chỉ nhận cherry-pick fix (`-x` để truy vết).
- Flag có owner, ngày hết hạn; flag "kill switch" vận hành khác flag release.

**Câu hỏi nối tiếp:**
- *Team outsourcing giao theo milestone cho khách hàng?* — Release branch/tag theo milestone vẫn kết hợp được với trunk-based ở phía phát triển.

**⚠️ Câu trả lời gây điểm trừ:** "Git Flow là chuẩn, team nào cũng nên dùng".

**📖 Ôn lại:** [3.2 Workflow](../01-giao-trinh/16-devops-build-cloud-security.md#p3)

</details>

### Q10. 🟡 Phân biệt `revert`, `reset --soft/--mixed/--hard`. Lỡ `reset --hard` mất commit thì cứu thế nào? Revert một merge commit ra sao?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `revert` tạo **commit mới đảo ngược** — an toàn trên branch chung. `reset` di chuyển con trỏ branch: `--soft` giữ thay đổi trong staging, `--mixed` (mặc định) giữ trong working dir, `--hard` xóa thay đổi. Mất commit sau reset/rebase → `git reflog` xem HEAD từng ở đâu, `git branch rescue HEAD@{n}`. Revert merge commit: `git revert -m 1 <merge>` (giữ parent 1 làm mainline).

**Giải thích chi tiết:**
- Commit chưa từng được commit (chỉ là thay đổi trong working dir) thì reflog không cứu được.
- Sau khi revert một merge, muốn merge lại branch đó phải "revert cái revert", vì Git coi các commit kia đã được merge.
- Cherry-pick (`-x`) cho hotfix từ `main` sang `release/1.4`; rủi ro: commit trùng nội dung khác hash, thiếu commit phụ thuộc.

**Câu hỏi nối tiếp:**
- *Reflog tồn tại bao lâu?* — Mặc định ~90 ngày cho entry reachable (30 ngày cho unreachable) trước khi `gc` dọn — chỉ ở local repo.

**⚠️ Câu trả lời gây điểm trừ:** dùng `reset --hard` + force push để "xóa" commit lỗi trên `main`.

**📖 Ôn lại:** [3.5 Cherry-pick, revert, reset, reflog](../01-giao-trinh/16-devops-build-cloud-security.md#p3)

</details>

### Q11. 🟡 Một regression xuất hiện giữa v1.3.0 và HEAD (hơn 800 commit). Bạn tìm commit gây lỗi thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `git bisect` — tìm kiếm nhị phân trên lịch sử: đánh dấu `bad` (HEAD) và `good` (v1.3.0), Git checkout commit ở giữa, bạn test và đánh dấu; ~log2(800) ≈ 10 bước. Tự động hóa bằng `git bisect run <script>`: exit 0 = good, 1–127 (trừ 125) = bad, 125 = skip (commit không build được).

**Giải thích chi tiết:**

```bash
git bisect start
git bisect bad
git bisect good v1.3.0
git bisect run ./mvnw -q -pl shop-app -am test -Dtest=PricingRegressionTest -Dsurefire.failIfNoSpecifiedTests=false
git bisect reset
```

- Viết test tái hiện **trước**, đặt ngoài cây code (file tạm/script) để không bị checkout ghi đè.
- Điều kiện: mỗi commit build được → lý do để giữ commit nhỏ, xanh; squash merge giúp bisect theo PR.

**Câu hỏi nối tiếp:**
- *Bisect trúng merge commit lớn?* — Bisect tiếp trong branch được merge, hoặc đọc diff; đây là lý do PR nên nhỏ.

**⚠️ Câu trả lời gây điểm trừ:** đọc tay từng commit, hoặc không biết bisect.

**📖 Ôn lại:** [3.6 `git bisect`](../01-giao-trinh/16-devops-build-cloud-security.md#p3)

</details>

### Q12. 🔴 🎬 Đồng nghiệp lỡ commit và push AWS access key lên repo. Bạn xử lý theo thứ tự nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Coi như secret **đã bị lộ**: (1) **thu hồi/rotate ngay** key (vô hiệu hóa trên IAM), (2) kiểm tra **audit log** (CloudTrail) xem key đã bị dùng chưa và đánh giá thiệt hại, (3) sau đó mới dọn lịch sử Git (`git filter-repo`, force push có phối hợp, yêu cầu mọi người clone lại; xóa cache/fork nếu có), (4) phòng ngừa: secret scanning + push protection, gitleaks pre-commit/CI, chuyển sang secret manager/OIDC.

**Giải thích chi tiết:**
- `git rm` hay commit xóa file **không đủ** — secret vẫn nằm trong lịch sử, fork, clone, cache CI.
- Bot quét GitHub public tìm key trong vài phút sau khi push → tốc độ rotate quan trọng hơn dọn lịch sử.
- Phòng ngừa gốc: CI dùng OIDC (không lưu key dài hạn), app dùng workload identity (IRSA/GKE Workload Identity), secret từ Vault/Secrets Manager.
- Postmortem blameless: hỏi vì sao hệ thống cho phép key dài hạn tồn tại trên máy dev.

**Câu hỏi nối tiếp:**
- *Repo private thì có cần rotate?* — Có: mọi người có quyền đọc repo (kể cả cựu nhân viên, tool bên thứ ba) đều đã thấy.

**⚠️ Câu trả lời gây điểm trừ:** "Revert commit đó là xong" hoặc chỉ viết lại lịch sử mà không rotate.

**📖 Ôn lại:** [3.6 Lỗi thường gặp](../01-giao-trinh/16-devops-build-cloud-security.md#p3) · [11.2 Secrets management](../01-giao-trinh/16-devops-build-cloud-security.md#p11)

</details>

### Q13. 🔴 Hai PR đều xanh CI, merge lần lượt vào `main` không có conflict, nhưng `main` build đỏ. Vì sao? Phòng tránh thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Đó là **conflict ngữ nghĩa**: PR A đổi tên `calculateTotal()` → `total()`; PR B (song song) thêm lời gọi `calculateTotal()` ở file khác. Git chỉ so text nên không báo conflict, nhưng kết quả merge không compile. CI của mỗi PR chạy trên base cũ. Phòng tránh: **merge queue** (GitHub merge queue, GitLab merge trains) chạy CI trên **kết quả merge với `main` mới nhất** trước khi thật sự merge; hoặc yêu cầu branch up-to-date trước khi merge.

**Giải thích chi tiết:**
- Luôn build + test sau khi giải quyết conflict — kể cả merge "sạch".
- "Require branches to be up to date" đơn giản nhưng gây vòng rebase liên tục khi team đông → merge queue mở rộng tốt hơn.
- Phát hiện nhanh: CI chạy trên `main` sau mỗi merge, alert khi đỏ, quy ước "main đỏ là ưu tiên số 1" (revert ngay).

**Câu hỏi nối tiếp:**
- *Migration Flyway trùng số version giữa hai PR?* — Cũng là xung đột không lộ qua text; đổi version của migration mình, kiểm tra trong CI.

**⚠️ Câu trả lời gây điểm trừ:** "Không có conflict thì merge an toàn".

**📖 Ôn lại:** [3.4 Giải quyết conflict](../01-giao-trinh/16-devops-build-cloud-security.md#p3)

</details>

---

<a id="g3"></a>
## 3. Linux cho backend developer

### Q14. 🟢 Service Java trên Linux ăn 100% CPU. Làm sao tìm ra đoạn code gây ra?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `top -H -p <PID>` để thấy **thread** nào ăn CPU (TID), đổi TID sang hex (`printf '%x\n' <TID>`), rồi `jstack <PID>` (hoặc `jcmd <PID> Thread.print`) và tìm `nid=0x<hex>` → stack trace chỉ ra method đang chạy. Lấy vài lần cách nhau vài giây để chắc nó kẹt ở cùng chỗ.

**Giải thích chi tiết:**

```bash
top -H -p 12345                         # thread TID 12401 dùng 99% CPU
printf '%x\n' 12401                     # -> 3071
jstack 12345 | grep -A 20 'nid=0x3071'
```

- Thread ăn CPU có thể là GC thread (heap gần đầy, GC liên tục) → kiểm tra `jcmd <pid> GC.heap_info`, log GC; hoặc JIT compiler thread lúc khởi động.
- Profiling sâu hơn: JFR (`jcmd <pid> JFR.start duration=60s filename=/tmp/rec.jfr`), async-profiler.
- Trong container distroless không có jstack → `kubectl debug` với image JDK, `--target` để chung PID namespace.

**Câu hỏi nối tiếp:**
- *Nguyên nhân hay gặp?* — Vòng lặp vô hạn, regex backtracking thảm họa, `HashMap` bị dùng đồng thời (Java 7 có thể tạo vòng lặp khi resize), GC thrashing.

**⚠️ Câu trả lời gây điểm trừ:** restart ngay mà không thu thread dump.

**📖 Ôn lại:** [4.1 Process và tài nguyên](../01-giao-trinh/16-devops-build-cloud-security.md#p4)

</details>

### Q15. 🟡 Log báo "Too many open files". Bạn chẩn đoán và xử lý thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Process chạm giới hạn **file descriptor** — trên Linux socket cũng là FD. Hai khả năng: giới hạn quá thấp (mặc định 1024 ở nhiều hệ thống) hoặc **rò rỉ** (stream/socket/response không đóng). Kiểm tra giới hạn thực của process (`cat /proc/<PID>/limits`), số FD đang mở (`ls /proc/<PID>/fd | wc -l`) theo thời gian, loại FD (`lsof -p <PID>`). Nếu số FD tăng tuyến tính → leak, tăng limit chỉ trì hoãn.

**Giải thích chi tiết:**
- `ulimit -n` chỉ là của shell hiện tại; process do systemd/Docker chạy có giới hạn riêng: `LimitNOFILE=65536`, `--ulimit nofile=65536:65536`.
- Phân loại bằng `lsof -p <PID> | awk '{print $5}' | sort | uniq -c`: nhiều `IPv4`/`sock` → connection leak (HTTP client không đóng response body, không trả connection về pool); nhiều `REG` → file stream không đóng.
- Sửa gốc: try-with-resources, connection pool có giới hạn, timeout.
- Micrometer có `process.files.open`/`process.files.max` → alert trước khi chạm trần.

**Câu hỏi nối tiếp:**
- *Liên quan gì tới CLOSE_WAIT?* — Socket ở CLOSE_WAIT vẫn giữ FD; nhiều CLOSE_WAIT thường đi cùng FD leak (Q16).

**⚠️ Câu trả lời gây điểm trừ:** "Tăng ulimit lên 1 triệu là xong".

**📖 Ôn lại:** [4.3 File descriptors và giới hạn](../01-giao-trinh/16-devops-build-cloud-security.md#p4)

</details>

### Q16. 🔴 `ss` cho thấy hàng nghìn kết nối `CLOSE_WAIT` trên app, và rất nhiều `TIME_WAIT` trên một service khác. Mỗi hiện tượng nói lên điều gì?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `CLOSE_WAIT` ở app: phía bên kia đã đóng kết nối nhưng **app của bạn chưa gọi `close()`** → lỗi trong code (không đóng response body của HTTP client, không trả connection về pool) → rò FD/connection. `TIME_WAIT` nhiều ở phía client: bên **chủ động đóng** giữ socket một thời gian (2×MSL) — thường do tạo kết nối mới cho mỗi request (không keep-alive/pool) → có thể cạn ephemeral port.

**Giải thích chi tiết:**

```bash
ss -tan | awk 'NR>1 {print $1}' | sort | uniq -c
ss -tan state close-wait '( dport = :443 )' | head
```

- CLOSE_WAIT **không tự hết** cho tới khi app đóng socket → số lượng tăng dần theo thời gian. Tìm code tạo kết nối tới peer đó (cổng đích), kiểm tra đóng `InputStream`/`Response` trong mọi nhánh, kể cả khi lỗi.
- TIME_WAIT là hành vi bình thường của TCP; giải pháp đúng là **tái sử dụng kết nối** (connection pool, HTTP keep-alive), không phải chỉnh kernel bừa bãi.
- `SYN_SENT` treo: không tới được đích (firewall, DNS sai).

**Câu hỏi nối tiếp:**
- *Apache HttpClient/OkHttp hay gặp gì?* — Không `close()` response hoặc không consume entity → connection không về pool → pool cạn, request chờ lease timeout.

**⚠️ Câu trả lời gây điểm trừ:** nhầm lẫn hai trạng thái, hoặc đề xuất bật `tcp_tw_recycle` (đã bị gỡ khỏi kernel mới, gây lỗi sau NAT).

**📖 Ôn lại:** [4.2 Network](../01-giao-trinh/16-devops-build-cloud-security.md#p4)

</details>

### Q17. 🟢 `kill -15`, `kill -9`, `kill -3` khác nhau thế nào với một process Java?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `-15` (SIGTERM, mặc định): JVM chạy shutdown hooks → Spring đóng context, graceful shutdown. `-9` (SIGKILL): chết ngay, không dọn dẹp, mất request đang xử lý. `-3` (SIGQUIT): JVM **in thread dump** ra stdout và **không** chết.

**Giải thích chi tiết:**
- K8s/Docker gửi SIGTERM trước, hết grace period mới SIGKILL; exit code 143 = 128+15, 137 = 128+9.
- Trước khi kill một process đang treo: lấy `jcmd <pid> Thread.print` và `GC.heap_info` để còn bằng chứng.
- SIGTERM chỉ tới được JVM nếu JVM là PID 1 hoặc init forward signal (xem Q21).

**Câu hỏi nối tiếp:**
- *Shutdown hook không chạy khi nào?* — SIGKILL, crash JVM (`SIGSEGV`), `Runtime.halt()`, hoặc OOM-kill của kernel.

**⚠️ Câu trả lời gây điểm trừ:** `kill -9` là thói quen mặc định.

**📖 Ôn lại:** [4.1 Signals](../01-giao-trinh/16-devops-build-cloud-security.md#p4)

</details>

### Q18. 🟡 Đĩa server đầy, bạn `rm` file log 20GB nhưng `df -h` vẫn báo đầy. Vì sao và xử lý thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Process (app Java) **vẫn giữ FD** tới file đã xóa → inode chưa được giải phóng, dung lượng chưa trả lại. Kiểm tra bằng `lsof +L1` (file đã unlink nhưng còn mở). Xử lý: `truncate -s 0` qua `/proc/<PID>/fd/<n>` hoặc cho app reopen/restart; lần sau dùng `truncate -s 0 file` thay vì `rm`, và cấu hình log rotation đúng (logback `RollingFileAppender` với `maxHistory`/`totalSizeCap`, hoặc log ra stdout cho container).

**Giải thích chi tiết:**
- `du` không thấy file đã xóa, `df` thì thấy dung lượng bị chiếm → chênh lệch du/df là dấu hiệu.
- Tìm thủ phạm: `du -sh /var/log/* | sort -h`, `lsof +L1`.
- Đĩa đầy là nguyên nhân sự cố kinh điển (DB không ghi được, app không ghi được tmp) → alert dung lượng đĩa.

**Câu hỏi nối tiếp:**
- *Phân tích log nhanh bằng dòng lệnh?* — `grep -C`, `awk` đếm status theo phút, `jq` cho log JSON, `zgrep` cho log nén.

**⚠️ Câu trả lời gây điểm trừ:** reboot server mà không hiểu nguyên nhân.

**📖 Ôn lại:** [4.3 File descriptors](../01-giao-trinh/16-devops-build-cloud-security.md#p4) · [4.4 Xử lý log bằng dòng lệnh](../01-giao-trinh/16-devops-build-cloud-security.md#p4)

</details>

---

<a id="g4"></a>
## 4. Docker

### Q19. 🟢 Docker image layer và build cache hoạt động thế nào? Sắp xếp Dockerfile ra sao để build nhanh?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Image là chồng **layer chỉ đọc** (mỗi `RUN/COPY/ADD` một layer) định danh theo nội dung; container thêm một layer ghi được. Docker tái dùng layer nếu lệnh và input không đổi; một layer đổi thì **mọi layer sau bị build lại**. Vì vậy đặt thứ **ít thay đổi lên trước** (base image, file build, dependency), **hay thay đổi xuống sau** (source code).

**Giải thích chi tiết:**
- Copy `pom.xml` + `mvnw` trước, `dependency:go-offline`, rồi mới `COPY src` → sửa code không tải lại dependency.
- Xóa file ở layer sau **không** làm image nhỏ đi → dọn trong cùng `RUN` hoặc dùng multi-stage.
- `--mount=type=cache,target=/root/.m2` (BuildKit) giữ cache Maven mà không đưa vào image.
- `.dockerignore` (target/, .git/, .env) — build context nhỏ, không lộ secret, không vô hiệu cache.

**Câu hỏi nối tiếp:**
- *`COPY . .` đầu Dockerfile có vấn đề gì?* — Mọi thay đổi file nào cũng vô hiệu cache của các bước sau.

**⚠️ Câu trả lời gây điểm trừ:** không biết vì sao rebuild mất 5 phút sau khi sửa một dòng code.

**📖 Ôn lại:** [5.1 Image, layer, build cache](../01-giao-trinh/16-devops-build-cloud-security.md#p5)

</details>

### Q20. 🟡 Viết Dockerfile tối ưu cho Spring Boot thế nào? Layered jar giải quyết gì?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Multi-stage: stage build dùng JDK + Maven, stage runtime chỉ dùng **JRE**, chạy **non-root**, ENTRYPOINT **exec form**. Fat jar ~80MB (95% là dependency) copy nguyên một layer → sửa 1 dòng code phải push lại 80MB. Layered jar tách thành `dependencies`, `spring-boot-loader`, `snapshot-dependencies`, `application` → sửa code chỉ đổi layer `application` (vài trăm KB).

**Giải thích chi tiết:**

```dockerfile
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q dependency:go-offline
COPY src src
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q package -DskipTests && cp target/*.jar application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

FROM eclipse-temurin:21-jre
RUN groupadd --system app && useradd --system --gid app --no-create-home app
WORKDIR /app
COPY --from=build /src/extracted/dependencies/ ./
COPY --from=build /src/extracted/spring-boot-loader/ ./
COPY --from=build /src/extracted/snapshot-dependencies/ ./
COPY --from=build /src/extracted/application/ ./
USER app
ENTRYPOINT ["java", "-jar", "application.jar"]
```

- Boot 3.3+: `-Djarmode=tools`; Boot < 3.3: `-Djarmode=layertools` + `JarLauncher` (package `org.springframework.boot.loader.launch` từ Boot 3.2).
- `-DskipTests` vì test đã chạy ở stage CI trước (build once).
- Khởi động nhanh hơn: CDS (`-XX:ArchiveClassesAtExit` training run), Spring AOT.

**Câu hỏi nối tiếp:**
- *Ghim base image thế nào?* — Tag cụ thể, tốt nhất kèm **digest**; Dependabot/Renovate cập nhật định kỳ để nhận bản vá.

**⚠️ Câu trả lời gây điểm trừ:** image 600MB chạy bằng JDK + Maven, `CMD mvn spring-boot:run`.

**📖 Ôn lại:** [5.2 Multi-stage build với layered jar](../01-giao-trinh/16-devops-build-cloud-security.md#p5)

</details>

### Q21. 🔴 Khi rolling update, pod luôn mất ~30 giây mới tắt và log không có dòng "Commencing graceful shutdown"; exit code 137. Nguyên nhân thường gặp?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Rất có thể ENTRYPOINT dùng **shell form** (`ENTRYPOINT java -jar app.jar`) hoặc script shell không `exec`: `/bin/sh -c` là **PID 1**, `sh` không forward SIGTERM cho Java → JVM không biết phải tắt → hết `terminationGracePeriodSeconds` (mặc định 30s) bị **SIGKILL** → exit 137, không graceful shutdown, request đang xử lý bị cắt. Sửa: **exec form** `["java", "-jar", "app.jar"]`, hoặc script kết thúc bằng `exec java ...`, hoặc dùng init nhỏ (`tini`, `docker run --init`).

**Giải thích chi tiết:**
- Exit code: 143 = SIGTERM (128+15) — tắt bình thường theo tín hiệu; 137 = SIGKILL (128+9) — bị giết (cũng là mã của OOMKilled, phân biệt bằng `kubectl describe pod` → `Reason`).
- PID 1 trong Linux còn có ngữ nghĩa đặc biệt: kernel không áp handler mặc định cho tín hiệu với PID 1 → process phải tự xử lý; JVM có handler nên exec form là đủ.
- Sau khi sửa vẫn cần cấu hình graceful shutdown của Spring + preStop (Q30).
- Script tiện (`jcmd 1 ...`) trong runbook cũng giả định Java là PID 1.

**Câu hỏi nối tiếp:**
- *Kiểm chứng nhanh?* — `docker exec <c> ps -o pid,cmd` xem PID 1 là gì; `docker stop` và đọc log + exit code.

**⚠️ Câu trả lời gây điểm trừ:** tăng `terminationGracePeriodSeconds` lên 120s để "cho nó đủ thời gian".

**📖 Ôn lại:** [5.4 Best practices](../01-giao-trinh/16-devops-build-cloud-security.md#p5) · [7.4 Graceful shutdown & preStop](../01-giao-trinh/16-devops-build-cloud-security.md#p7)

</details>

### Q22. 🟡 So sánh Dockerfile, Jib, Cloud Native Buildpacks và base image distroless.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Dockerfile: linh hoạt nhất, tự chịu trách nhiệm layer/non-root/JVM flags. **Jib**: build & push **không cần Docker daemon**, tự tách layer, reproducible — hợp CI không có Docker. **Buildpacks** (`spring-boot:build-image`): không cần Dockerfile, tự layered, memory calculator cho JVM, non-root. **Distroless**: chỉ JRE + thư viện tối thiểu, không shell/package manager → ít CVE, nhưng không `exec sh` được, debug qua ephemeral container.

**Giải thích chi tiết:**
- Alpine (musl) nhỏ nhưng từng khác hành vi với glibc (DNS, hiệu năng) — dùng JRE build cho musl, test kỹ.
- `jlink` + `jdeps --print-module-deps` tạo JRE tùy biến nhỏ hơn.
- Tiêu chí chọn cho team: kích thước, số CVE (Trivy), thời gian build, thời gian khởi động, khả năng debug, mức chuẩn hóa giữa các service.

**Câu hỏi nối tiếp:**
- *Debug pod distroless?* — `kubectl debug -it <pod> --image=eclipse-temurin:21-jdk --target=app -- bash`.

**⚠️ Câu trả lời gây điểm trừ:** chỉ biết một cách và cho rằng nó luôn tốt nhất.

**📖 Ôn lại:** [5.3 Các cách build image khác](../01-giao-trinh/16-devops-build-cloud-security.md#p5)

</details>

### Q23. 🟡 Những best practice bảo mật khi build image? Vì sao không được đưa secret vào `ENV` hay `COPY .env`?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Non-root (`USER`), ghim tag/digest, image runtime tối thiểu (JRE/distroless), quét CVE (Trivy/Grype/Docker Scout) trong CI, label OCI để truy vết commit, `.dockerignore`. Secret trong `ENV`/`COPY` nằm vĩnh viễn trong layer — ai pull image cũng đọc được qua `docker history` hoặc giải nén layer, kể cả khi layer sau đã xóa file. Secret cần lúc build → `RUN --mount=type=secret`; secret lúc chạy → inject runtime (K8s Secret file, secret manager).

**Giải thích chi tiết:**
- K8s bổ sung: `runAsNonRoot`, `readOnlyRootFilesystem`, `allowPrivilegeEscalation: false`, drop ALL capabilities, seccomp `RuntimeDefault` (Pod Security "restricted").
- Ký image (cosign) và admission policy kiểm chữ ký; SBOM đi kèm image.

**Câu hỏi nối tiếp:**
- *Token tải package private lúc build?* — `RUN --mount=type=secret,id=npmrc ...` — không xuất hiện trong layer.

**⚠️ Câu trả lời gây điểm trừ:** "Image để trong registry private nên đưa password vào cũng không sao".

**📖 Ôn lại:** [5.4 Best practices (và vì sao)](../01-giao-trinh/16-devops-build-cloud-security.md#p5)

</details>

---

<a id="g5"></a>
## 5. JVM trong container

### Q24. 🟡 JVM xác định max heap trong container thế nào? Bạn cấu hình memory cho service Spring Boot trên K8s ra sao?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Với `UseContainerSupport` (mặc định từ JDK 10, backport 8u191; cgroup v2 từ JDK 15/11.0.16/8u372), JVM đọc memory limit từ **cgroup**. Mặc định `MaxRAMPercentage=25` → heap chỉ 25% limit, lãng phí. Thường đặt `-XX:MaxRAMPercentage=70..80`, request = limit memory (vd 1Gi), cộng `-XX:+ExitOnOutOfMemoryError` và heap dump vào volume.

**Giải thích chi tiết:**
- JVM cũ (trước 8u191) đọc RAM của **host** → container 1GB trên host 64GB chọn heap 16GB → kernel OOM-kill.
- Kiểm tra JVM thấy gì: `java -XX:+PrintFlagsFinal -version | grep -E 'MaxHeapSize|ActiveProcessorCount'`, `-Xlog:os+container=trace`.
- `-Xms` = `-Xmx` (hoặc Initial = Max percentage) → commit heap sớm, tránh bất ngờ; đánh đổi: dùng RAM ngay từ đầu.
- Container rất nhỏ (< 512MB): tính tay, đặt `-Xmx` tường minh và giới hạn metaspace/direct memory.

**Câu hỏi nối tiếp:**
- *Vì sao không đặt `-Xmx` bằng memory limit?* — Heap chỉ là một phần; tổng process sẽ vượt limit → OOMKilled (Q25).

**⚠️ Câu trả lời gây điểm trừ:** "Đặt `-Xmx1g` cho container 1Gi".

**📖 Ôn lại:** [6.2 Container support hiện đại](../01-giao-trinh/16-devops-build-cloud-security.md#p6) · [6.3 Bộ nhớ: heap chỉ là một phần](../01-giao-trinh/16-devops-build-cloud-security.md#p6)

</details>

### Q25. 🔴 Pod bị restart liên tục với `OOMKilled` nhưng log không có `OutOfMemoryError` và heap dashboard chỉ dùng 60%. Giải thích và cách xử lý.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `OOMKilled` do **kernel (cgroup OOM killer)** giết vì **tổng bộ nhớ process** vượt limit — JVM không kịp ném exception, không có heap dump. Heap 60% không nói lên gì vì phần **non-heap** (metaspace, code cache, thread stacks, direct/NIO buffers của Netty/Kafka, GC structures, malloc arenas, tmpfs) cũng tính vào cgroup. Chẩn đoán bằng Native Memory Tracking, so RSS với limit; xử lý bằng giảm `MaxRAMPercentage`, giới hạn từng vùng, hoặc tăng limit.

**Giải thích chi tiết:**

| | `OutOfMemoryError: Java heap space` | Container `OOMKilled` (exit 137) |
|---|---|---|
| Ai phát hiện | JVM | Kernel |
| Nguyên nhân | Heap đầy | Tổng RSS vượt limit |
| Dấu vết | Exception, heap dump | `kubectl describe pod` → `Reason: OOMKilled` |

- Chẩn đoán: `-XX:NativeMemoryTracking=summary` + `jcmd <pid> VM.native_memory summary`; metric RSS (`container_memory_working_set_bytes`) vs limit.
- Giới hạn: `-XX:MaxDirectMemorySize` (mặc định ≈ max heap!), `-XX:MaxMetaspaceSize`, `-XX:ReservedCodeCacheSize`, `-Xss` (cẩn thận đệ quy sâu), giảm số thread (Tomcat max 200 × stack).
- Đặt giới hạn direct memory → JVM ném `OutOfMemoryError: Cannot reserve direct buffer memory` có kiểm soát thay vì bị kernel giết.
- Nguyên nhân khác: tmpfs `/tmp` ghi file lớn, glibc malloc arenas (`MALLOC_ARENA_MAX`), leak native trong JNI.

**Câu hỏi nối tiếp:**
- *Heap dump khi OOMKilled?* — Không có; chỉ có khi JVM tự ném OOME với `HeapDumpOnOutOfMemoryError` và path trên volume bền.

**⚠️ Câu trả lời gây điểm trừ:** "Tăng `-Xmx`" — làm tình hình tệ hơn.

**📖 Ôn lại:** [6.3 Bộ nhớ: heap chỉ là một phần](../01-giao-trinh/16-devops-build-cloud-security.md#p6)

</details>

### Q26. 🔴 Service có p99 latency tăng vọt định kỳ dù CPU usage trung bình chỉ 40%. Pod có CPU limit = 1. Bạn nghi ngờ gì? Liên quan GC và `availableProcessors()` thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Nghi **CPU throttling (CFS quota)**: limit 1 CPU = 100ms CPU time mỗi chu kỳ 100ms, **cộng dồn trên mọi thread**. GC song song nhiều thread (hoặc JIT, burst request) tiêu hết quota sớm → process bị **dừng hẳn** phần còn lại của chu kỳ → p99 tăng dù trung bình thấp. Kiểm tra `container_cpu_cfs_throttled_periods_total`/`throttled_seconds` hoặc `cpu.stat` (`nr_throttled`, `throttled_usec`). Thêm vào đó, container nhỏ có thể khiến JVM chọn **SerialGC**, còn không đặt limit thì JVM thấy toàn bộ CPU của node.

**Giải thích chi tiết:**
- `availableProcessors()` tính từ CPU **limit** (quota/period, làm tròn lên); từ JDK 19 không còn dùng CPU shares (requests). Không đặt limit → thấy 64 CPU của node → 64 GC thread, common pool 63 thread, Netty 128 event loop trong pod chỉ request 1 CPU.
- GC ergonomics: "server class" cần ≥ 2 CPU và ≥ ~1792MB → container 1 CPU/1GB âm thầm dùng **SerialGC**.
- Cấu hình khởi điểm: request theo nhu cầu đo được, limit CPU cao hơn request (hoặc bỏ limit CPU theo chính sách tổ chức), `-XX:ActiveProcessorCount` khớp kích thước thật, `-XX:+UseG1GC` tường minh, cấp ≥ 2 CPU nếu cần GC song song.
- Khởi động JVM ăn CPU (JIT, class loading) → limit thấp làm startup chậm, startup probe fail → restart loop.

**Câu hỏi nối tiếp:**
- *Vì sao nhiều tổ chức bỏ CPU limit?* — Tránh throttling; đổi lại cần requests chính xác và giám sát noisy neighbor. Memory limit thì vẫn nên đặt.

**⚠️ Câu trả lời gây điểm trừ:** "CPU 40% thì không thể là vấn đề CPU" — không biết throttling.

**📖 Ôn lại:** [6.4 CPU: số core, GC ergonomics và throttling](../01-giao-trinh/16-devops-build-cloud-security.md#p6)

</details>

---

<a id="g6"></a>
## 6. Kubernetes

### Q27. 🟢 Giải thích Pod, Deployment, Service, Ingress, ConfigMap, Secret cho một backend developer.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **Pod**: đơn vị deploy nhỏ nhất, 1+ container chung network namespace và volume, ephemeral (chết là mất, IP mới). **Deployment → ReplicaSet → Pod**: khai báo trạng thái mong muốn, controller liên tục reconcile, rolling update tạo ReplicaSet mới. **Service**: IP ảo + DNS ổn định, load-balance tới các Pod **Ready** khớp selector. **Ingress/Gateway API**: định tuyến HTTP(S) từ ngoài theo host/path, TLS termination. **ConfigMap/Secret**: cấu hình và dữ liệu nhạy cảm, mount env hoặc file.

**Giải thích chi tiết:**
- Service types: `ClusterIP` (mặc định), `NodePort`, `LoadBalancer`, headless (`clusterIP: None`) cho StatefulSet.
- **Secret chỉ là base64**, không phải mã hóa → cần encryption at rest cho etcd, RBAC, hoặc External Secrets/Vault/CSI.
- Gateway API là thế hệ kế tiếp của Ingress (traffic splitting, header matching); ingress-nginx cộng đồng đã thông báo ngừng bảo trì.
- Khác: StatefulSet (danh tính ổn định), Job/CronJob, HPA, PDB, NetworkPolicy.

**Câu hỏi nối tiếp:**
- *Đổi ConfigMap, pod có tự nhận không?* — Qua env: không, cần rollout restart (mẹo: annotation checksum trong pod template). Qua file mount: file được cập nhật sau một lúc nhưng app phải tự reload.

**⚠️ Câu trả lời gây điểm trừ:** "Secret của K8s đã được mã hóa nên an toàn".

**📖 Ôn lại:** [7.1 Các object cốt lõi](../01-giao-trinh/16-devops-build-cloud-security.md#p7)

</details>

### Q28. 🟡 Thiết kế startup, liveness, readiness probe cho Spring Boot thế nào cho đúng?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **Startup**: "đã khởi động xong chưa?" — cho JVM thời gian (vd `failureThreshold: 30 × periodSeconds: 5`), trong lúc đó liveness/readiness tạm hoãn. **Liveness**: "process có kẹt không tự hồi phục?" — fail thì **restart**; chỉ kiểm trạng thái nội bộ (`LivenessState`), **không** kiểm DB/service ngoài. **Readiness**: "có nhận traffic được không?" — fail thì gỡ khỏi endpoints (không restart); phản ánh warm-up, đang shutdown, và cân nhắc dependency thiết yếu. Dùng Actuator `/actuator/health/liveness` và `/readiness` trên cổng management riêng.

**Giải thích chi tiết:**
- Spring Boot `ApplicationAvailability`: `LivenessState` (`CORRECT/BROKEN`), `ReadinessState` (`ACCEPTING_TRAFFIC/REFUSING_TRAFFIC`); khi shutdown, Boot tự chuyển readiness sang REFUSING. Tự publish được: `AvailabilityChangeEvent.publish(ctx, ReadinessState.REFUSING_TRAFFIC)` khi cache chưa warm.
- `management.endpoint.health.probes.enabled=true` (tự bật trên K8s); cổng management (8081) không đi qua Ingress, không bị auth chặn (probe 401 → fail).
- Readiness có DB: DB down → mọi pod unready → Service không còn endpoint → client nhận lỗi kết nối thay vì 503 có ý nghĩa. Nhiều team chỉ để readiness phản ánh trạng thái nội bộ, xử lý dependency bằng circuit breaker.

**Câu hỏi nối tiếp:**
- *Không có startup probe thì sao?* — Phải đặt `initialDelaySeconds` lớn cho liveness; JVM khởi động chậm hơn dự kiến (limit CPU thấp) → bị kill trước khi kịp lên → restart loop.

**⚠️ Câu trả lời gây điểm trừ:** dùng cùng một endpoint `/actuator/health` (gồm cả DB, Redis) cho cả liveness và readiness.

**📖 Ôn lại:** [7.3 Probes — thiết kế đúng](../01-giao-trinh/16-devops-build-cloud-security.md#p7)

</details>

### Q29. 🔴 🎬 Database chậm khoảng 30 giây, sau đó **toàn bộ** pod của service bị restart và hệ thống mất 15 phút mới hồi phục. Chuyện gì đã xảy ra?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Gần như chắc chắn **DB nằm trong liveness probe**. DB chậm → mọi pod fail liveness → K8s restart **tất cả cùng lúc**. Khi DB hồi phục, toàn bộ pod đang khởi động lại: JVM lạnh (JIT, cache rỗng), connection pool tạo đồng loạt (thundering herd lên DB vừa hồi phục), startup probe có thể fail tiếp → sự cố 30 giây thành 15 phút. Sửa: liveness chỉ trạng thái nội bộ; lỗi dependency xử lý bằng timeout + circuit breaker trả 503 nhanh; cân nhắc kỹ dependency trong readiness.

**Giải thích chi tiết:**
- Tái hiện: `management.endpoint.health.group.liveness.include=livenessState,db`, scale 5 replica, dừng PostgreSQL 60s → quan sát restart count.
- Sau sửa: pod không restart; request lỗi nhanh nhờ circuit breaker (Resilience4j), tự hồi phục khi DB lên.
- Thêm: PDB, HPA với stabilization window (CPU spike lúc khởi động làm HPA scale sai), connection pool có `initializationFailTimeout`/backoff để không dồn kết nối.
- Đây là ví dụ điển hình của "cơ chế tự chữa lành làm sự cố tệ hơn" — đáng kể trong phỏng vấn kèm postmortem.

**Câu hỏi nối tiếp:**
- *Vậy liveness nên fail khi nào?* — Deadlock, trạng thái nội bộ hỏng không tự phục hồi (app tự đặt `LivenessState.BROKEN`), event loop kẹt.

**⚠️ Câu trả lời gây điểm trừ:** "Tăng `failureThreshold` của liveness lên" — chỉ trì hoãn, không sửa thiết kế.

**📖 Ôn lại:** [7.3 Probes — sai lầm kinh điển](../01-giao-trinh/16-devops-build-cloud-security.md#p7)

</details>

### Q30. 🔴 Mỗi lần rolling update có vài chục request lỗi 502/connection reset. Giải thích trình tự tắt pod và cấu hình để zero-downtime.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Khi pod bị xóa, **song song**: (a) kubelet chạy `preStop` rồi gửi SIGTERM; (b) endpoint controller gỡ pod khỏi EndpointSlice và kube-proxy/ingress trên mọi node cập nhật (mất vài trăm ms đến vài giây). Nếu app đóng ngay khi nhận SIGTERM trong lúc (b) chưa lan truyền → request mới vẫn tới pod đang tắt → 502. Cấu hình: `preStop: sleep 10` để trì hoãn SIGTERM, `server.shutdown=graceful` + `spring.lifecycle.timeout-per-shutdown-phase=25s`, và `terminationGracePeriodSeconds` > tổng (vd 10 + 25 + 10 = 45).

**Giải thích chi tiết:**
- Sau SIGTERM: readiness → REFUSING, web server ngừng nhận request mới, chờ request đang xử lý, đóng bean (Kafka consumer commit offset, đóng pool).
- `terminationGracePeriodSeconds` tính **từ đầu**, bao gồm cả preStop; hết hạn → SIGKILL.
- Image distroless không có `sh` → dùng `lifecycle.preStop.sleep` (K8s 1.30+).
- Rolling strategy `maxSurge: 1, maxUnavailable: 0` để không giảm capacity; PDB `minAvailable` cho node drain.
- ENTRYPOINT phải exec form để Java nhận SIGTERM (Q21).

**Câu hỏi nối tiếp:**
- *Kafka consumer khi shutdown?* — Graceful close commit offset và rời group → rebalance; xử lý idempotent vì message có thể được xử lý lại.

**⚠️ Câu trả lời gây điểm trừ:** "Lỗi lúc deploy là bình thường, client retry là được".

**📖 Ôn lại:** [7.4 Graceful shutdown & preStop](../01-giao-trinh/16-devops-build-cloud-security.md#p7)

</details>

### Q31. 🟡 `requests` và `limits` khác nhau thế nào? QoS class là gì? Autoscale service Java bằng HPA có gì cần lưu ý?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **requests** dùng cho **scheduling** và tỷ lệ chia CPU khi tranh chấp; **limits**: vượt memory → OOMKilled, vượt CPU → bị throttle. QoS: `Guaranteed` (request = limit cho cả CPU và memory), `Burstable`, `BestEffort` (bị evict đầu tiên). HPA tính % CPU **so với request**; với JVM, CPU spike lúc khởi động (JIT) dễ làm HPA scale sai → dùng stabilization window, cân nhắc metric phản ánh tải thật (req/s, Kafka lag qua Prometheus Adapter/KEDA). Memory không phải metric scale tốt cho JVM.

**Giải thích chi tiết:**
- Heap không co lại nhanh sau GC → HPA theo memory gần như không scale down.
- `behavior.scaleDown.stabilizationWindowSeconds: 300` tránh dao động; `minReplicas` ≥ 2–3 cho HA.
- Requests quá thấp → node bị nhồi quá tải; quá cao → lãng phí và pod pending (`FailedScheduling`).

**Câu hỏi nối tiếp:**
- *Scale theo consumer lag Kafka?* — KEDA Kafka scaler; số replica không vượt số partition (consumer dư sẽ idle).

**⚠️ Câu trả lời gây điểm trừ:** không đặt requests/limits (BestEffort) cho service production.

**📖 Ôn lại:** [7.5 Resources, QoS và HPA](../01-giao-trinh/16-devops-build-cloud-security.md#p7)

</details>

### Q32. 🟡 Rolling update một bản có đổi tên cột DB. Làm sao không downtime và vẫn rollback được?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Trong rolling update, **bản cũ và mới chạy song song** → mọi thay đổi phải tương thích hai chiều. Dùng **expand → migrate → contract** qua nhiều release: (1) thêm cột mới, code ghi cả hai cột; (2) backfill dữ liệu; (3) code chỉ đọc cột mới; (4) xóa cột cũ ở release sau. Migration chạy trước khi pod mới nhận traffic (init container/Job) và không phá code cũ. Rollback (`kubectl rollout undo`) chỉ an toàn khi schema còn tương thích với bản cũ.

**Giải thích chi tiết:**
- API/event: thêm field được; xóa/đổi tên cần versioning hoặc giai đoạn chuyển tiếp.
- Bảng lớn: thêm cột có default hoặc tạo index có thể lock bảng lâu (tùy DB/version) → dùng `CREATE INDEX CONCURRENTLY` (PostgreSQL), online DDL (MySQL), backfill theo batch.
- Feature flag giúp tách việc deploy code khỏi việc bật đọc cột mới.

**Câu hỏi nối tiếp:**
- *Flyway chạy lúc app start ở mọi replica?* — Flyway có lock bảng lịch sử nên an toàn về đồng thời, nhưng migration dài làm startup probe fail → tách thành Job riêng.

**⚠️ Câu trả lời gây điểm trừ:** `ALTER TABLE RENAME COLUMN` trong cùng release với code mới.

**📖 Ôn lại:** [7.6 Rolling update và tương thích ngược](../01-giao-trinh/16-devops-build-cloud-security.md#p7)

</details>

### Q33. 🟢 Pod ở trạng thái `CrashLoopBackOff`. Bạn dùng những lệnh nào để tìm nguyên nhân?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `kubectl describe pod <pod>` (Events: probe failed, OOMKilled, ImagePullBackOff, exit code), `kubectl logs <pod> --previous` (log của container **đã chết**), `kubectl get events --sort-by=.lastTimestamp`. Nguyên nhân hay gặp: thiếu config/secret/biến môi trường, không kết nối được DB lúc start, startup/liveness probe quá chặt, OOMKilled, sai command/entrypoint.

**Giải thích chi tiết:**
- `kubectl logs` không có `--previous` chỉ thấy lần chạy hiện tại (có thể chưa có gì).
- `kubectl debug -it <pod> --image=eclipse-temurin:21-jdk --target=app` để chạy `jcmd` trong pod distroless.
- `ImagePullBackOff`: sai tag, thiếu `imagePullSecrets`; `latest` + `IfNotPresent` → node chạy image cũ.
- `kubectl rollout history/undo` để quay lại bản trước nếu do deploy mới.

**Câu hỏi nối tiếp:**
- *Pod Pending mãi?* — `describe` thấy `FailedScheduling` (thiếu tài nguyên theo requests, node selector/taint, PVC chưa bind).

**⚠️ Câu trả lời gây điểm trừ:** chỉ `kubectl logs` không `--previous` rồi kết luận "không có log".

**📖 Ôn lại:** [7.7 Lệnh debug hằng ngày](../01-giao-trinh/16-devops-build-cloud-security.md#p7)

</details>

---

<a id="g7"></a>
## 7. CI/CD

### Q34. 🟢 Continuous Integration, Continuous Delivery và Continuous Deployment khác nhau thế nào? "Build once, deploy many" nghĩa là gì?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** CI: mọi thay đổi tích hợp vào trunk thường xuyên và được kiểm chứng tự động. Continuous **Delivery**: mọi commit trên trunk *có thể* release bất cứ lúc nào (prod cần một nút bấm). Continuous **Deployment**: tự động deploy prod. **Build once, deploy many**: artifact (image digest) build **một lần**, chính artifact đó được promote dev → staging → prod; khác biệt môi trường nằm ở cấu hình, không ở build.

**Giải thích chi tiết:**
- Build lại image riêng cho từng môi trường → thứ chạy ở prod không phải thứ đã test ở staging.
- Fail fast: bước rẻ và hay fail (compile, unit, lint) chạy trước.
- Đo bằng **DORA**: deployment frequency, lead time for changes, change failure rate, time to restore.

**Câu hỏi nối tiếp:**
- *Tag image bằng gì?* — Git SHA (bất biến) và deploy theo **digest**; không dùng `latest`.

**⚠️ Câu trả lời gây điểm trừ:** dùng ba khái niệm như nhau; build lại cho từng môi trường "vì cấu hình khác".

**📖 Ôn lại:** [8.1 Nguyên tắc](../01-giao-trinh/16-devops-build-cloud-security.md#p8)

</details>

### Q35. 🟡 Thiết kế pipeline CI/CD cho một Spring Boot microservice. Các stage và gate là gì?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** PR/push: (1) build + unit test + format/lint (< 5 phút), (2) static analysis & SAST (SpotBugs/FindSecBugs, Sonar gate, CodeQL/Semgrep), (3) integration test (Testcontainers, Failsafe), (4) SCA (Dependency-Check/Snyk, fail CVSS ≥ 7), (5) build image + SBOM + scan (Trivy), (6) ký & push tag = SHA. Chỉ trên `main`: (7) deploy staging (Helm/Kustomize/GitOps) + migration, (8) smoke/contract (`can-i-deploy`), (9) deploy prod (canary/blue-green, approval nếu cần), (10) post-deploy verification + auto rollback.

**Giải thích chi tiết:**
- `concurrency` hủy build cũ khi push mới; cache Maven; upload báo cáo test kể cả khi fail.
- Helm `--atomic` để release dở dang tự rollback.
- Mục tiêu: PR pipeline < 10 phút, nếu không dev sẽ bỏ qua.
- Deploy prod không bao giờ từ laptop; không `continue-on-error` cho gate.

**Câu hỏi nối tiếp:**
- *Stage nào chạy song song được?* — SAST, SCA, integration test độc lập nhau sau khi compile.

**⚠️ Câu trả lời gây điểm trừ:** pipeline chỉ có "build → deploy", không có gate chất lượng/bảo mật.

**📖 Ôn lại:** [8.2 Các stage](../01-giao-trinh/16-devops-build-cloud-security.md#p8) · [8.3 Ví dụ GitHub Actions](../01-giao-trinh/16-devops-build-cloud-security.md#p8)

</details>

### Q36. 🟡 So sánh rolling update, blue-green, canary, feature flag. GitOps là gì và lợi ích?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Rolling: mặc định K8s, rẻ, cũ/mới chạy song song. Blue-green: hai môi trường đầy đủ, chuyển traffic một lần, rollback tức thì, tốn gấp đôi tài nguyên, DB vẫn dùng chung. Canary: 1% → 10% → 50% → 100% với so sánh metric tự động (Argo Rollouts, Flagger) — phát hiện lỗi với ảnh hưởng nhỏ. Feature flag: tách **deploy** khỏi **release**. GitOps (Argo CD, Flux): trạng thái mong muốn của cluster nằm trong Git, controller tự đồng bộ → audit trail, rollback = `git revert`, CI không cần quyền vào cluster.

**Giải thích chi tiết:**
- Canary cần metric tốt (error rate, latency theo version) và đủ traffic để có ý nghĩa thống kê.
- Mọi chiến lược đều yêu cầu tương thích ngược DB/API.
- GitOps: CI chỉ cập nhật digest image trong repo cấu hình; promote prod bằng PR.

**Câu hỏi nối tiếp:**
- *AnalysisTemplate cho canary?* — Query Prometheus tỉ lệ 5xx của version mới, `successCondition: result[0] < 0.01`, fail thì tự abort.

**⚠️ Câu trả lời gây điểm trừ:** "Blue-green giải quyết luôn vấn đề migration DB".

**📖 Ôn lại:** [8.2 Chiến lược release](../01-giao-trinh/16-devops-build-cloud-security.md#p8)

</details>

### Q37. 🔴 Pipeline CI có quyền deploy prod và đọc secret. Bạn bảo vệ chính pipeline thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** CI là mục tiêu tấn công giá trị (vụ Codecov 2021, action `tj-actions/changed-files` bị chiếm quyền 2025). Biện pháp: `permissions` tối thiểu (mặc định `contents: read`); **ghim third-party action theo commit SHA**; không chạy workflow có secret cho PR từ fork (`pull_request_target` + checkout code PR là cực kỳ nguy hiểm); **OIDC** thay secret dài hạn để đăng nhập cloud; tách quyền deploy prod vào environment có approval; ký artifact (cosign) và kiểm chữ ký khi deploy (admission policy); runner tạm thời, không trạng thái.

**Giải thích chi tiết:**
- Dependabot cho `github-actions` để cập nhật SHA ghim.
- Secret scoping theo environment; không echo secret; mask log.
- SLSA provenance + SBOM để truy vết artifact đến commit và build.
- Audit: ai sửa workflow, ai approve deploy.

**Câu hỏi nối tiếp:**
- *Vì sao tag `@v4` chưa đủ?* — Tag có thể bị dời sang commit độc hại nếu repo action bị chiếm; SHA bất biến.

**⚠️ Câu trả lời gây điểm trừ:** "Pipeline nội bộ nên không cần lo" hoặc lưu AWS access key dài hạn làm secret repo.

**📖 Ôn lại:** [8.3 Góc nhìn Senior — an ninh của chính pipeline](../01-giao-trinh/16-devops-build-cloud-security.md#p8)

</details>

---

<a id="g8"></a>
## 8. Observability

### Q38. 🟢 Ba trụ cột của observability là gì? Chúng liên kết với nhau thế nào khi điều tra sự cố?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **Metrics** trả lời "có vấn đề không, ở mức tổng thể?" (rẻ, dùng cho dashboard/alert). **Logs**: "chuyện gì xảy ra với request/sự kiện này?" (chi tiết, đắt). **Traces**: "request đi qua đâu, chậm ở đâu?". Observability tốt là **liên kết** được: alert (metric) → exemplar/trace chậm → log của đúng request qua `traceId`.

**Giải thích chi tiết:**
- Micrometer Tracing tự đưa `traceId`/`spanId` vào MDC → mọi dòng log có traceId.
- Structured logging (Boot 3.4+: `logging.structured.format.console=ecs`) để lọc theo field trong Loki/Elasticsearch.
- Exemplar trong Prometheus/Grafana nối điểm histogram với trace cụ thể.

**Câu hỏi nối tiếp:**
- *Monitoring khác observability?* — Monitoring trả lời câu hỏi biết trước (dashboard, alert); observability cho phép hỏi câu hỏi mới về hành vi chưa lường trước nhờ dữ liệu giàu ngữ cảnh.

**⚠️ Câu trả lời gây điểm trừ:** "Observability là có Grafana dashboard".

**📖 Ôn lại:** [9.1 Ba trụ cột](../01-giao-trinh/16-devops-build-cloud-security.md#p9)

</details>

### Q39. 🟡 MDC là gì? Vì sao log trong method `@Async`/`CompletableFuture` mất `requestId`? Sửa thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** MDC (Mapped Diagnostic Context) là map key-value gắn vào log, cài bằng **`ThreadLocal`** → không tự đi theo khi chuyển sang thread khác (executor, `@Async`, `CompletableFuture`, Reactor). Sửa: `TaskDecorator` sao chép MDC từ thread gọi sang thread worker (và khôi phục/clear sau), hoặc Micrometer Context Propagation (Reactor: `Hooks.enableAutomaticContextPropagation()`). Filter đặt `requestId` phải `MDC.clear()` trong `finally` vì thread pool tái sử dụng thread.

**Giải thích chi tiết:**

```java
@Bean TaskDecorator mdcTaskDecorator() {
    return runnable -> {
        Map<String, String> ctx = MDC.getCopyOfContextMap();
        return () -> {
            Map<String, String> previous = MDC.getCopyOfContextMap();
            try { if (ctx != null) MDC.setContextMap(ctx); runnable.run(); }
            finally { if (previous != null) MDC.setContextMap(previous); else MDC.clear(); }
        };
    };
}
```

- Boot tự áp `TaskDecorator` bean cho executor auto-configured; executor tự tạo thì phải gắn tay.
- Quên clear → log của request này mang id của request trước — rất khó debug.
- Quy tắc log: không log secret/token/PII; log exception một lần nơi xử lý; JSON encoder tự escape chống log injection.

**Câu hỏi nối tiếp:**
- *Virtual threads có giải quyết?* — Không tự động; mỗi virtual thread có ThreadLocal riêng; vẫn cần propagation (Scoped Values là hướng tương lai).

**⚠️ Câu trả lời gây điểm trừ:** "Dùng biến static để lưu requestId".

**📖 Ôn lại:** [9.2 Structured logging và MDC](../01-giao-trinh/16-devops-build-cloud-security.md#p9)

</details>

### Q40. 🔴 Prometheus bị hết RAM sau khi team thêm một custom metric. Nguyên nhân? Và vì sao không nên dùng percentile tính ở client cho dashboard nhiều pod?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **Cardinality explosion**: metric có tag với giá trị không giới hạn (`userId`, `orderId`, URL thô `/orders/123`) → mỗi giá trị là một time series → Prometheus nổ bộ nhớ. Tag phải có tập giá trị nhỏ, cố định (method, status, uri template, gateway). Percentile tính phía client (`publishPercentiles(0.99)`) **không gộp được** giữa các instance (không thể lấy trung bình p99); dùng **histogram** (`publishPercentileHistogram()`) và `histogram_quantile()` trong PromQL.

**Giải thích chi tiết:**

```promql
histogram_quantile(0.99, sum by (le, uri) (rate(http_server_requests_seconds_bucket{application="order-service"}[5m])))
```

- Spring dùng URI template (`/orders/{id}`) cho tag `uri`; nhưng request 404 không khớp route hoặc tag tự tạo từ path thô vẫn nổ.
- Phát hiện: `count by (__name__)({__name__=~".+"})`, top series theo metric; giới hạn bằng `MeterFilter` (deny/maximumAllowableTags).
- ID cụ thể thuộc về **log/trace**, không phải metric.
- Loại meter: Counter, Gauge, Timer, DistributionSummary, LongTaskTimer; Observation API (`@Observed`) sinh cả metric và span.

**Câu hỏi nối tiếp:**
- *Histogram tốn gì?* — Mỗi bucket là một series; dùng `serviceLevelObjectives`/giới hạn min-max expected để giảm số bucket.

**⚠️ Câu trả lời gây điểm trừ:** "Thêm RAM cho Prometheus" hoặc lấy `avg()` của p99 giữa các pod.

**📖 Ôn lại:** [9.3 Metrics với Micrometer](../01-giao-trinh/16-devops-build-cloud-security.md#p9)

</details>

### Q41. 🟡 RED, USE và Four Golden Signals là gì? Áp dụng cho một Spring Boot service thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **RED** (cho service/request): Rate, Errors, Duration — mỗi service, endpoint quan trọng và **mỗi dependency gọi ra**. **USE** (cho tài nguyên): Utilization, Saturation, Errors — CPU, memory, disk, network và tài nguyên ứng dụng: thread pool, connection pool (`hikaricp.connections.pending`), Kafka consumer lag. **Golden Signals** (Google SRE): latency, traffic, errors, saturation.

**Giải thích chi tiết:**
- Spring Boot có sẵn: `http.server.requests`, `jvm.gc.pause`, `jvm.memory.used`, `hikaricp.connections.*`, `tomcat.threads.*`, `kafka.consumer.*` qua `/actuator/prometheus`.
- Dashboard tốt: hàng đầu là RED của service, sau đó RED của dependency, sau đó USE của JVM/pool — không phải 50 biểu đồ ngang hàng.
- Saturation thường là tín hiệu sớm nhất (pool pending tăng trước khi latency nổ).

**Câu hỏi nối tiếp:**
- *Alert theo CPU 80%?* — Không page; CPU là nguyên nhân, không phải triệu chứng — để trên dashboard chẩn đoán.

**⚠️ Câu trả lời gây điểm trừ:** chỉ theo dõi CPU/RAM của máy.

**📖 Ôn lại:** [9.4 RED, USE, Four Golden Signals](../01-giao-trinh/16-devops-build-cloud-security.md#p9)

</details>

### Q42. 🔴 Định nghĩa SLI, SLO, SLA, error budget. Viết alert theo SLO thế nào để vừa phát hiện nhanh vừa không gây alert fatigue?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **SLI**: tỷ lệ đo được về trải nghiệm người dùng (request thành công & < 300ms / tổng). **SLO**: mục tiêu cho SLI trong cửa sổ (99.9% trong 30 ngày). **SLA**: cam kết hợp đồng, luôn lỏng hơn SLO. **Error budget** = 1 − SLO (99.9%/30 ngày ≈ 43,2 phút). Alert theo **burn rate** multi-window: page khi tốc độ tiêu budget cao ở **cả** cửa sổ dài và ngắn (vd 14,4× trên 1h **và** 5m), ticket cho burn chậm (1× trên 3 ngày/6h).

**Giải thích chi tiết:**
- Burn rate 14,4 trong 1h tiêu 2% budget 30 ngày → đáng page. Cửa sổ ngắn giúp alert **tự tắt nhanh** khi hết sự cố; cửa sổ dài giảm báo động giả.
- Ví dụ tính: lỗi 5% với SLO 99.9% → burn 50× → vượt 14,4× rất nhanh ở cửa sổ 5m; cửa sổ 1h cần ~17 phút lỗi tích lũy nếu trước đó sạch.
- Alert phải actionable, có runbook; không ai xử lý → xóa hoặc hạ thành ticket.
- Error budget biến tranh luận "nhanh vs ổn định" thành con số: còn budget → được thử nghiệm; hết → ưu tiên độ tin cậy.

**Câu hỏi nối tiếp:**
- *SLO 100%?* — Không thể đạt và không còn chỗ cho thay đổi; đo SLI ở gần người dùng (LB/CDN) thay vì chỉ ở server.

**⚠️ Câu trả lời gây điểm trừ:** "Alert khi error rate > 1% trong 1 phút" cho mọi service — dễ flapping, không gắn với SLO.

**📖 Ôn lại:** [9.6 SLI, SLO, error budget và alerting](../01-giao-trinh/16-devops-build-cloud-security.md#p9)

</details>

### Q43. 🟡 Distributed tracing hoạt động thế nào? Head-based và tail-based sampling khác nhau ra sao? Vì sao trace hay "đứt"?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Trace là cây **span**; context truyền giữa service qua header W3C `traceparent` (HTTP) hoặc header message Kafka. OpenTelemetry gồm API, SDK, OTLP, Collector. Java tích hợp bằng **OTel Java agent** (auto-instrument không đổi code) hoặc **Micrometer Tracing** + bridge OTel (Boot 3). Head-based: quyết định lấy mẫu ở đầu trace, rẻ nhưng có thể bỏ sót trace lỗi; tail-based: Collector giữ trace hoàn chỉnh rồi quyết định (giữ mọi trace lỗi/chậm), tốn tài nguyên. Trace đứt khi một hop không propagate context.

**Giải thích chi tiết:**
- Điểm gãy thường gặp: HTTP client tự `new` (không qua builder Spring), thread pool tự tạo, Kafka producer tự viết không thêm header, gateway/proxy bỏ header.
- `management.tracing.sampling.probability=0.1`; agent: `-Dotel.traces.sampler=parentbased_traceidratio`.
- Span thủ công cho logic quan trọng: `Observation.createNotStarted("pricing.calculate", registry).observe(...)`.
- Kiểm chứng bằng **một trace thật** xuyên toàn luồng trước khi tuyên bố "đã có tracing".

**Câu hỏi nối tiếp:**
- *Sampling 100% ở hệ thống lớn?* — Chi phí lưu trữ/network lớn; dùng tỉ lệ thấp + tail sampling cho lỗi.

**⚠️ Câu trả lời gây điểm trừ:** nhầm tracing với log có thêm timestamp.

**📖 Ôn lại:** [9.5 Distributed tracing với OpenTelemetry](../01-giao-trinh/16-devops-build-cloud-security.md#p9)

</details>

---

<a id="g9"></a>
## 9. Application security & supply chain

### Q44. 🟢 Kể các mục OWASP Top 10 bạn nhớ. Broken Access Control/IDOR là gì và phòng chống trong Spring thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Bản 2021: A01 Broken Access Control, A02 Cryptographic Failures, A03 Injection, A04 Insecure Design, A05 Security Misconfiguration, A06 Vulnerable & Outdated Components, A07 Identification & Authentication Failures, A08 Software & Data Integrity Failures, A09 Logging & Monitoring Failures, A10 SSRF. **IDOR/BOLA**: user A đọc/sửa tài nguyên của user B chỉ bằng đổi id. Phòng: kiểm tra quyền **ở server, trên từng đối tượng** — ràng buộc quyền sở hữu ngay trong query hoặc method security.

**Giải thích chi tiết:**

```java
@GetMapping("/api/orders/{id}")
OrderDto get(@PathVariable long id, @AuthenticationPrincipal Jwt jwt) {
    return repo.findByIdAndCustomerId(id, jwt.getSubject())
               .map(mapper::toDto)
               .orElseThrow(OrderNotFoundException::new);   // 404, không lộ sự tồn tại
}
// hoặc: @PreAuthorize("hasRole('ADMIN') or @orderAuthz.isOwner(#id, authentication)")
```

- Dạng khác: thiếu kiểm tra ở endpoint admin, mass assignment (bind request thẳng vào entity có field `role`), CORS `*` với credentials, chỉ kiểm tra ở frontend.
- Nguyên tắc: deny by default; có test tự động cho rule phân quyền.
- Bản 2025 giữ Broken Access Control số 1 (gộp SSRF), thêm mục supply chain — đối chiếu owasp.org khi trích dẫn; dùng OWASP ASVS để kiểm tra có hệ thống.

**Câu hỏi nối tiếp:**
- *Trả 403 hay 404 khi không có quyền?* — 404 cho tài nguyên của người khác để không lộ sự tồn tại; 403 khi người dùng biết tài nguyên tồn tại nhưng thiếu quyền chức năng.

**⚠️ Câu trả lời gây điểm trừ:** "Đã có JWT/đăng nhập là đủ an toàn".

**📖 Ôn lại:** [10.1–10.2 OWASP Top 10, Broken Access Control](../01-giao-trinh/16-devops-build-cloud-security.md#p10)

</details>

### Q45. 🟡 Dùng JPA/`JdbcTemplate` có còn bị SQL injection không? Tham số `sort` từ URL xử lý thế nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Có — bất cứ khi nào **nối chuỗi** input vào SQL hoặc JPQL (`"from User u where u.email = '" + email + "'"`). Luôn dùng tham số hóa (`?`, `:email`, `setParameter`). Tên cột/`ORDER BY`/tên bảng **không tham số hóa được** → dùng **allowlist** ánh xạ tên hợp lệ sang cột thật; input không khớp → 400.

**Giải thích chi tiết:**

```java
private static final Map<String, String> SORTABLE = Map.of("createdAt", "created_at", "total", "total");
String column = Optional.ofNullable(SORTABLE.get(sortParam)).orElseThrow(BadRequestException::new);
```

- Spring Data `Sort` với property name cũng nên giới hạn danh sách cho phép.
- Injection khác: OS command (`Runtime.exec("convert " + f)` → `ProcessBuilder` với danh sách tham số), SpEL/OGNL từ input (nguồn RCE của Struts/Spring — dùng `SimpleEvaluationContext` nếu buộc phải), LDAP, NoSQL, CRLF/log injection, XSS (`th:utext`).
- SAST (FindSecBugs `SQL_INJECTION_JDBC`, CodeQL) bắt được phần lớn mẫu nối chuỗi.

**Câu hỏi nối tiếp:**
- *"Escape dấu nháy" tự viết có được không?* — Không; dễ sót (encoding, dialect), tham số hóa là cách đúng.

**⚠️ Câu trả lời gây điểm trừ:** "Dùng ORM thì không bao giờ bị SQL injection".

**📖 Ôn lại:** [10.3 A03 — Injection](../01-giao-trinh/16-devops-build-cloud-security.md#p10)

</details>

### Q46. 🔴 Tính năng "import ảnh đại diện từ URL" có rủi ro gì? Bạn phòng chống SSRF thế nào cho đúng?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** SSRF: server fetch URL do người dùng cung cấp → kẻ tấn công trỏ vào tài nguyên nội bộ: metadata cloud `http://169.254.169.254/...` (lấy credential IAM), `http://localhost:8081/actuator/env`, admin nội bộ, scheme `file://`, `gopher://`. Phòng nhiều lớp: allowlist host nếu được; chỉ `https`; **resolve DNS rồi kiểm tra IP** không thuộc dải private/loopback/link-local và **kết nối đúng IP đã kiểm tra** (chống DNS rebinding); tắt follow redirect; hạ tầng: egress proxy/NetworkPolicy, IMDSv2, tách service fetch không có quyền.

**Giải thích chi tiết:**

```java
static void assertPublicHttpsUrl(URI uri) throws UnknownHostException {
    if (!"https".equalsIgnoreCase(uri.getScheme())) throw new IllegalArgumentException("only https");
    for (InetAddress a : InetAddress.getAllByName(uri.getHost())) {
        if (a.isAnyLocalAddress() || a.isLoopbackAddress() || a.isLinkLocalAddress()
            || a.isSiteLocalAddress() || a.isMulticastAddress()) throw new IllegalArgumentException("blocked " + a);
    }
}
```

- Bypass kinh điển: IP dạng thập phân `http://2130706433/` (= 127.0.0.1), IPv6 `::1`, redirect từ URL public sang `127.0.0.1`, DNS rebinding (lần resolve thứ hai trả IP nội bộ).
- `isSiteLocalAddress` không bao IPv6 ULA (`fc00::/7`), CGNAT `100.64/10` → dùng thư viện/allowlist đầy đủ.
- `HttpClient.newBuilder().followRedirects(Redirect.NEVER)`, giới hạn kích thước response, timeout, content-type.

**Câu hỏi nối tiếp:**
- *Webhook do khách hàng cấu hình?* — Cùng loại rủi ro: validate khi lưu **và** khi gọi; gọi qua egress proxy có allowlist/denylist.

**⚠️ Câu trả lời gây điểm trừ:** "Regex chặn chữ 'localhost' và '127.0.0.1' là đủ".

**📖 Ôn lại:** [10.4 A10 — SSRF](../01-giao-trinh/16-devops-build-cloud-security.md#p10)

</details>

### Q47. 🔴 Insecure deserialization trong Java nguy hiểm thế nào? Jackson có bị không?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** `ObjectInputStream.readObject()` trên dữ liệu không tin cậy cho phép kẻ tấn công gửi **gadget chain** — chuỗi class hợp lệ có sẵn trên classpath (vd Commons Collections cũ) mà khi deserialize sẽ thực thi lệnh → **RCE**, xảy ra **trước** khi code kịp kiểm tra kiểu. Phòng: không dùng Java serialization cho dữ liệu không tin cậy (dùng JSON/Protobuf có schema); nếu buộc phải, dùng `ObjectInputFilter` allowlist. Jackson **có** rủi ro tương tự khi bật polymorphic typing theo class (`enableDefaultTyping`, `@JsonTypeInfo(use = Id.CLASS)`).

**Giải thích chi tiết:**

```java
ObjectInputFilter filter = ObjectInputFilter.Config.createFilter(
    "com.acme.dto.*;java.base/*;!*;maxdepth=10;maxbytes=65536");
try (var in = new ObjectInputStream(input)) { in.setObjectInputFilter(filter); in.readObject(); }
// hoặc toàn JVM: -Djdk.serialFilter=...  (JEP 290; Java 17 thêm filter factory — JEP 415)
```

- Jackson an toàn: `Id.NAME` + `@JsonSubTypes` allowlist, hoặc `activateDefaultTyping` với `PolymorphicTypeValidator` chặt.
- SnakeYAML < 2.0 với constructor mặc định tạo object bất kỳ → dùng `SafeConstructor`/bản mới.
- Nơi hay ẩn Java serialization: session replication, cache (Redis với JDK serializer), RMI/JMX, message queue cũ.

**Câu hỏi nối tiếp:**
- *Redis cache dùng `JdkSerializationRedisSerializer` có rủi ro?* — Có nếu kẻ tấn công ghi được vào Redis; chuyển sang JSON serializer với kiểu cố định.

**⚠️ Câu trả lời gây điểm trừ:** "Chỉ deserialize rồi cast sang kiểu mong muốn nên an toàn".

**📖 Ôn lại:** [10.5 A08 — Insecure deserialization](../01-giao-trinh/16-devops-build-cloud-security.md#p10)

</details>

### Q48. 🟡 XXE là gì? Cấu hình parser XML trong Java thế nào để an toàn?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** XML External Entity: parser xử lý DTD và external entity → kẻ tấn công đọc file (`file:///etc/passwd`), SSRF, hoặc DoS ("billion laughs"). Cách an toàn nhất: **chặn DTD hoàn toàn** (`disallow-doctype-decl = true`), tắt external general/parameter entities, không load external DTD, tắt XInclude, bật `FEATURE_SECURE_PROCESSING` — áp cho mọi factory (`DocumentBuilderFactory`, `SAXParserFactory`, `XMLInputFactory`, `TransformerFactory`, `SchemaFactory`). Gom vào **một factory dùng chung**.

**Giải thích chi tiết:**

```java
DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
dbf.setXIncludeAware(false);
dbf.setExpandEntityReferences(false);
```

- StAX: `IS_SUPPORTING_EXTERNAL_ENTITIES=false`, `SUPPORT_DTD=false`.
- OWASP 2021 gộp XXE vào A05 Security Misconfiguration.
- Hệ thống tích hợp ngân hàng/thuế (VN hay dùng SOAP/XML) là nơi XXE thường gặp.

**Câu hỏi nối tiếp:**
- *Thư viện bọc (JAXB, Jackson XML) có an toàn mặc định?* — Phụ thuộc version/cấu hình; kiểm tra và truyền factory đã cứng hóa.

**⚠️ Câu trả lời gây điểm trừ:** dùng `DocumentBuilderFactory.newInstance()` mặc định cho file người dùng upload.

**📖 Ôn lại:** [10.6 XXE](../01-giao-trinh/16-devops-build-cloud-security.md#p10)

</details>

### Q49. 🟡 Những cấu hình sai bảo mật phổ biến ở Spring Boot? Lưu mật khẩu và dùng JWT thế nào cho đúng?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Actuator lộ ra ngoài (`/actuator/env` lộ property, `/actuator/heapdump` chứa password/token/dữ liệu khách hàng, `/actuator/loggers` đổi log level) → chỉ expose `health, info, prometheus`, cổng management riêng không qua Ingress. Stack trace trả về client, H2 console ở prod, CORS rộng, thiếu security headers. Mật khẩu: `DelegatingPasswordEncoder` (bcrypt mặc định) hoặc Argon2/scrypt/PBKDF2, không MD5/SHA trần. JWT: kiểm chữ ký với thuật toán cố định, kiểm `exp`/`iss`/`aud`, access token ngắn hạn, không để dữ liệu nhạy cảm trong payload.

**Giải thích chi tiết:**
- Mã hóa dữ liệu: AES-GCM với nonce ngẫu nhiên duy nhất, không ECB, khóa trong KMS/Vault; `SecureRandom` cho token.
- Spring Security Resource Server với `issuer-uri` làm đúng việc kiểm chữ ký/claims mặc định; chống `alg: none`/nhầm thuật toán.
- Brute force: rate limit, lockout có kiểm soát, MFA; thông báo lỗi giống nhau cho "user không tồn tại" và "sai mật khẩu" (chống user enumeration).
- CSRF: API stateless dùng bearer token có thể tắt; app dùng cookie session thì **không**.

**Câu hỏi nối tiếp:**
- *Thu hồi JWT trước khi hết hạn?* — Token ngắn hạn + refresh token có thể thu hồi, hoặc denylist theo `jti` (đánh đổi tính stateless).

**⚠️ Câu trả lời gây điểm trừ:** `management.endpoints.web.exposure.include=*` ở prod; lưu mật khẩu bằng SHA-256 không salt.

**📖 Ôn lại:** [10.7 A05, A02, A07](../01-giao-trinh/16-devops-build-cloud-security.md#p10)

</details>

### Q50. 🔴 Giải thích Log4Shell (CVE-2021-44228): cơ chế, phạm vi, và bài học cho một Senior.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Log4j 2 (log4j-core 2.0-beta9 → 2.14.1) diễn giải chuỗi `${...}` **trong nội dung log message** (message lookup). Lookup `jndi` truy vấn LDAP/RMI do kẻ tấn công kiểm soát, server trả tham chiếu tới class từ xa → JVM tải và thực thi → **RCE không cần xác thực**, CVSS 10. Chỉ cần app **log** một chuỗi người dùng kiểm soát (`User-Agent`, username). Bài học: biết mình đang chạy gì (**SBOM**), khả năng **vá và deploy nhanh** cả fleet, **defense in depth** (egress filtering chặn kết nối LDAP ra ngoài), cảnh giác với tính năng "tiện lợi" khó đoán.

**Giải thích chi tiết:**
- Chuỗi bản vá: 2.15.0 (chưa đủ, CVE-2021-45046) → 2.16.0 (gỡ message lookup, tắt JNDI mặc định) → 2.17.0 (DoS đệ quy lookup, CVE-2021-45105) → 2.17.1 (CVE-2021-44832). Biện pháp tạm: xóa class `JndiLookup` khỏi jar.
- Spring Boot mặc định dùng **Logback** → không bị trực tiếp, trừ khi dùng `spring-boot-starter-log4j2`; `log4j-api`/`log4j-to-slf4j` không chứa lỗ hổng nhưng gây hoảng loạn khi quét theo tên.
- WAF chỉ giúp một phần: payload dễ obfuscate (`${${lower:j}ndi:...}`).
- Nhiều tổ chức mất nhiều ngày chỉ để trả lời "service nào dùng log4j-core version nào, kể cả transitive và image bên thứ ba".

**Câu hỏi nối tiếp:**
- *Spring4Shell (CVE-2022-22965)?* — Data binding truy cập `class.module.classLoader` trên JDK 9+ khi deploy WAR trên Tomcat → ghi JSP → RCE; bài học tương tự: cập nhật framework kịp thời.

**⚠️ Câu trả lời gây điểm trừ:** "Đó là lỗi của Log4j, không liên quan tới chúng tôi" — không nói được quy trình phát hiện và vá.

**📖 Ôn lại:** [10.8 Case study: Log4Shell](../01-giao-trinh/16-devops-build-cloud-security.md#p10)

</details>

### Q51. 🟡 SCA là gì? Khi scanner báo một CVE "Critical" trong dependency, bạn triage thế nào? Kể vài mối đe dọa supply chain.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Software Composition Analysis đối chiếu dependency (phần lớn transitive) với cơ sở dữ liệu lỗ hổng: OWASP Dependency-Check (NVD, cần API key, có false positive do so CPE), Snyk, Dependabot/Renovate, Trivy/Grype cho image, OSV-Scanner. Triage: (1) có trong runtime classpath không (scope `test` rủi ro thấp)? (2) đoạn code lỗi có **reachable** không? (3) có bị khai thác thực tế (CISA KEV, EPSS)? (4) nâng cấp — thường qua **patch version Spring Boot/BOM**, hoặc override trong `dependencyManagement`, hoặc suppress **có lý do và thời hạn**.

**Giải thích chi tiết:**
- Supply chain: typosquatting, **dependency confusion** (package public trùng tên nội bộ → cấu hình repository manager chỉ lấy groupId nội bộ từ repo nội bộ), maintainer bị chiếm tài khoản, build server bị xâm nhập.
- Biện pháp: repository manager làm proxy duy nhất, checksum/signature verification (Gradle `verification-metadata.xml`), SBOM (CycloneDX/SPDX), SLSA provenance, ký image bằng cosign.
- Dependabot: gom nhóm PR (`groups`) để tránh spam, cập nhật cả `docker` và `github-actions`.

**Câu hỏi nối tiếp:**
- *Commons Text 1.9 có vấn đề gì?* — CVE-2022-42889 "Text4Shell" (string interpolation), sửa ở 1.10.0.

**⚠️ Câu trả lời gây điểm trừ:** suppress toàn bộ cảnh báo vì "nhiều false positive quá".

**📖 Ôn lại:** [11.1 Dependency scanning (SCA)](../01-giao-trinh/16-devops-build-cloud-security.md#p11)

</details>

### Q52. 🟡 Secret (password DB, API key) của service bạn được quản lý thế nào? Rotate không downtime ra sao?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Nguồn sự thật là **secret manager** (Vault, AWS Secrets Manager, GCP Secret Manager…); đồng bộ vào cluster qua External Secrets Operator/CSI driver và **mount dạng file** (Spring đọc bằng `spring.config.import=optional:configtree:/etc/secrets/`), hoặc app đọc trực tiếp (Spring Cloud Vault). Credential cloud dùng **workload identity** (IRSA, GKE Workload Identity) thay access key. Rotate không downtime: tồn tại **song song hai credential hợp lệ** trong thời gian chuyển, cập nhật secret, rolling restart (Hikari chỉ dùng password khi tạo connection mới; `maxLifetime` thay dần connection cũ), rồi thu hồi credential cũ.

**Giải thích chi tiết:**
- Mức trưởng thành: hard-code trong Git ❌ → env từ CI → K8s Secret file + encryption at rest + RBAC → secret manager tập trung → **dynamic secrets** (Vault cấp user DB tạm thời theo TTL).
- Env var dễ lộ qua `/actuator/env`, crash dump, `ps e`.
- Phát hiện rò rỉ: gitleaks/trufflehog pre-commit + CI, GitHub push protection; audit log truy cập secret.
- Khi lộ: rotate ngay, kiểm tra audit, rồi mới dọn lịch sử.

**Câu hỏi nối tiếp:**
- *Vì sao đây là câu hỏi phân loại ứng viên?* — Nó cho thấy bạn có từng vận hành production thật hay chỉ code tính năng.

**⚠️ Câu trả lời gây điểm trừ:** "Để trong `application-prod.yml`, repo private".

**📖 Ôn lại:** [11.2 Secrets management](../01-giao-trinh/16-devops-build-cloud-security.md#p11)

</details>

### Q53. 🟡 Gọi API đối tác báo `PKIX path building failed`. Nguyên nhân và cách sửa đúng? mTLS là gì?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** JVM không dựng được chuỗi tin cậy từ chứng chỉ server tới một CA trong **truststore** — CA nội bộ/riêng của đối tác chưa được thêm, hoặc server không gửi kèm intermediate. Sửa đúng: thêm **đúng CA** vào truststore riêng cho client đó (Spring Boot 3.1+ **SSL bundles**), yêu cầu đối tác gửi đủ chain — **không** dùng `TrustManager` "trust all". mTLS: cả hai phía xuất trình chứng chỉ; server xác thực client bằng CA tin cậy (`client-auth: need`).

**Giải thích chi tiết:**
- Keystore (khóa riêng + chứng chỉ của mình) vs truststore (CA mình tin; mặc định `cacerts`).
- TLS 1.3: 1-RTT, forward secrecy bắt buộc; SNI cho biết hostname; TLS 1.0/1.1 đã bị loại bỏ.
- Lỗi khác: `No subject alternative names matching` (hostname không khớp SAN), chứng chỉ hết hạn (theo dõi/alert, cert-manager/ACME tự gia hạn).
- mTLS nội bộ thường do service mesh (Istio, Linkerd) đảm nhận; TLS termination ở Ingress vs end-to-end (zero-trust).

```yaml
spring:
  ssl:
    bundle:
      pem:
        partner-client:
          truststore:
            certificate: "file:/etc/tls/partner-ca.crt"
```

**Câu hỏi nối tiếp:**
- *Debug handshake?* — `-Djavax.net.debug=ssl:handshake`, `openssl s_client -connect host:443 -showcerts`.

**⚠️ Câu trả lời gây điểm trừ:** copy đoạn "trust all certificates" từ StackOverflow vào production.

**📖 Ôn lại:** [11.3 TLS cơ bản cho backend developer](../01-giao-trinh/16-devops-build-cloud-security.md#p11)

</details>

---

<a id="g10"></a>
## 10. Sự cố & postmortem

### Q54. 🔴 🎬 10 phút sau khi deploy order-service, alert error budget burn của checkout bắn: 18% request lỗi 503. Bạn là on-call. Đi qua cách bạn xử lý.

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** **Mitigate trước, root cause sau.** Xác nhận alert và phạm vi, mở kênh sự cố (nhận vai Incident Commander hoặc gọi IC), hỏi ngay "gần đây có gì thay đổi?" — deploy cách đây 10 phút → **rollback** là ứng viên số 1 (một lệnh, rủi ro thấp). Song song thu bằng chứng (thread dump, metric pool, trace) trước khi pod cũ biến mất. Sau khi ổn định: tìm root cause, viết postmortem blameless với action items có chủ và hạn.

**Giải thích chi tiết:**
- Phạm vi: mọi instance hay vài? mọi endpoint hay chỉ checkout? RED của service và **từng dependency**.
- Giả thuyết điển hình: `hikaricp_connections_pending` tăng, API khuyến mãi p99 = 4s → bản mới gọi HTTP (không timeout phù hợp) **bên trong `@Transactional`** → giữ connection DB → cạn pool.
- Thu bằng chứng: `jcmd <pid> Thread.print` (3 lần cách vài giây — nhiều thread chờ `HikariPool.getConnection`), metric, trace chậm, log theo `traceId`.
- Giao tiếp: cập nhật stakeholder định kỳ (vd 30 phút với SEV1) kể cả khi chưa có gì mới; một người thay đổi production tại một thời điểm, ghi timeline.
- Action items: timeout mặc định cho RestClient qua builder dùng chung; rule ArchUnit/Sonar cấm HTTP call trong transaction; bật canary + auto-rollback; runbook "rollback trước nếu sự cố trong 1h sau deploy"; load test có kịch bản dependency chậm.

**Câu hỏi nối tiếp:**
- *Rollback không được vì migration DB không tương thích ngược?* — Đó là lý do expand–contract; mitigate khác: tắt feature flag, scale up tạm, giảm timeout, degrade tính năng.
- *Khi nào escalate?* — Vượt khả năng/thẩm quyền, ảnh hưởng dữ liệu/bảo mật, hoặc quá thời gian mục tiêu của severity.

**⚠️ Câu trả lời gây điểm trừ:** debug code ngay trên production 1 tiếng trước khi cân nhắc rollback; restart hàng loạt không thu bằng chứng; nhiều người cùng thay đổi cấu hình song song.

**📖 Ôn lại:** [12.1 Vòng đời sự cố](../01-giao-trinh/16-devops-build-cloud-security.md#p12) · [12.2 Chẩn đoán có phương pháp](../01-giao-trinh/16-devops-build-cloud-security.md#p12)

</details>

### Q55. 🟡 Postmortem "blameless" là gì? Một postmortem tốt gồm những phần nào?

<details>
<summary>Đáp án</summary>

**Trả lời ngắn:** Mục tiêu là **học từ sự cố để hệ thống tốt hơn**, không tìm người để phạt — nếu sợ bị phạt, mọi người giấu thông tin và tổ chức không học được gì. Giả định mọi người hành động hợp lý với thông tin lúc đó; câu hỏi là *hệ thống* nào cho phép lỗi xảy ra và lan rộng. Gồm: tóm tắt, ảnh hưởng (có số liệu), timeline, nguyên nhân gốc & **các yếu tố góp phần**, điều gì tốt, điều gì chưa tốt/may mắn, **action items cụ thể có chủ và hạn**.

**Giải thích chi tiết:**
- 5 Whys hữu ích nhưng dễ dẫn tới một nguyên nhân tuyến tính; sự cố thật có nhiều yếu tố.
- Tránh kết luận "lỗi con người" — hỏi tiếp: tại sao một lỗi đơn lẻ gây ảnh hưởng lớn?
- Action item "cẩn thận hơn" là vô giá trị — không đo được.
- Phòng ngừa chủ động: runbook, game day/chaos engineering, kiểm tra restore backup định kỳ.

**Câu hỏi nối tiếp:**
- *Ai theo dõi action items?* — Như công việc thật trong backlog, review định kỳ; postmortem không có action item hoàn thành chỉ là tài liệu.

**⚠️ Câu trả lời gây điểm trừ:** "Postmortem để xác định ai gây lỗi và nhắc nhở".

**📖 Ôn lại:** [12.3 Postmortem không đổ lỗi](../01-giao-trinh/16-devops-build-cloud-security.md#p12)

</details>

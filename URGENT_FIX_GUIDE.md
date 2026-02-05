# 🔧 BookPict 프로젝트 긴급 수정 가이드

## 🚨 발견된 문제점 요약

1. ❌ **빌드 시마다 데이터 초기화** - `ddl-auto: create` 설정 문제
2. ❌ **디버그 코드 충돌** - `edu.bookpict.entity.Book`과 `edu.bookpict.domain.book.Book` 중복
3. ❌ **잘못된 데이터 수집** - 베스트셀러 대신 학년별/과목별 참고서를 수집해야 함
4. ❌ **중복 감지 로직 미흡** - API 데이터와 DB 데이터 비교 불완전
5. ❌ **H2 콘솔/수동 SQL 오용** - `bookpict.trace.db`에 잘못된 SQL 구문 또는 파일 잠금 관련 오류가 남음 (예: 단순 테이블명 실행으로 인한 Syntax error, 파일 잠금 등)

---

## ✅ 해결 방법

### 1️⃣ 데이터 초기화 문제 해결

**문제**: `application.yml`에서 `spring.jpa.hibernate.ddl-auto: create` 설정 때문에 매번 테이블이 삭제됨

**해결**:

#### A. application.yml 교체

```bash
# 기존 파일 백업
mv app/src/main/resources/application.yml app/src/main/resources/application.yml.backup

# 수정된 파일 복사
cp application_fixed.yml app/src/main/resources/application.yml
```

**주요 변경사항**:
```yaml
spring:
  datasource:
    # ✅ 파일 기반 H2 (데이터 영구 저장)
    url: jdbc:h2:file:./data/bookpict;MODE=MySQL;DB_CLOSE_ON_EXIT=FALSE

  jpa:
    hibernate:
      # ✅ update로 변경 (기존 데이터 유지)
      ddl-auto: update  # 기존: create
```

#### B. DataInitializer 수정

```bash
# 기존 파일 백업
mv app/src/main/java/edu/bookpict/config/DataInitializer.java \
   app/src/main/java/edu/bookpict/config/DataInitializer.java.backup

# 수정된 파일 복사
cp DataInitializer_Fixed.java \
   app/src/main/java/edu/bookpict/config/DataInitializer.java
```

**주요 변경사항**:
```java
@Override
public void run(String... args) throws Exception {
    // ✅ DB에 데이터가 있으면 초기화하지 않음
    if (bookRepository.count() > 0) {
        log.info("📚 Database already has books. Skipping initialization.");
        return;
    }
    // ...
}
```

---

### 2️⃣ 디버그 코드 충돌 해결

**문제**: 두 개의 Book 엔티티가 존재하여 충돌 발생

**해결**: 디버그 전용 코드 완전 제거

```bash
# 1. 디버그 엔티티 제거
rm app/src/main/java/edu/bookpict/entity/Book.java
rm app/src/main/java/edu/bookpict/entity/BookIsbnMapping.java

# 2. 디버그 레포지토리 제거
rm app/src/main/java/edu/bookpict/repository/BookRepository.java
rm app/src/main/java/edu/bookpict/repository/debugg/BookIsbnMappingRepository.java

# 3. 디버그 서비스 제거
rm app/src/main/java/edu/bookpict/service/BookDebugService.java

# 4. 디버그 컨트롤러 제거
rm app/src/main/java/edu/bookpict/controller/DebugController.java

# 5. 디버그 DTO 제거
rm app/src/main/java/edu/bookpict/dto/AladinBookDto.java

# 6. 디버그 초기화 제거
rm app/src/main/java/edu/bookpict/config/TestDataInitializer.java
```

**대안**: 디버그 기능이 필요하면 테스트 코드로 이동
```bash
# 테스트 디렉토리 생성
mkdir -p app/src/test/java/edu/bookpict/debug

# 디버그 코드를 테스트로 이동 (선택사항)
# mv app/src/main/java/edu/bookpict/service/BookDebugService.java \
#    app/src/test/java/edu/bookpict/debug/
```

---

### 3️⃣ 스케줄러 수정 (학년별/과목별 수집)

### H2 콘솔 접근 정책

- H2 콘솔은 기본적으로 비활성화되었습니다. 로컬에서만 사용하려면 환경변수 `H2_CONSOLE_ENABLED=true`를 설정하거나 `-Dspring.profiles.active=local`을 사용하세요.
- 운영 환경에서 H2 콘솔을 활성화하지 마십시오. 잘못된 수동 SQL 실행(예: 단순 테이블명만 입력)은 `bookpict.trace.db`에 Syntax error 로그를 남기고 서비스 장애를 유발할 수 있습니다.
- 애플리케이션 시작 시 `TraceDbChecker`가 `bookpict.trace.db`를 검사하여 구문 오류 또는 파일 잠금 관련 로그가 발견되면 경고를 기록합니다.

**문제**: 베스트셀러 API 호출 대신 학년별/과목별 참고서를 수집해야 함

**해결**:

```bash
# 기존 파일 백업
mv app/src/main/java/edu/bookpict/aladin/scheduler/AladinSyncScheduler.java \
   app/src/main/java/edu/bookpict/aladin/scheduler/AladinSyncScheduler.java.backup

# 수정된 파일 복사
cp AladinSyncScheduler_Fixed.java \
   app/src/main/java/edu/bookpict/aladin/scheduler/AladinSyncScheduler.java
```

**주요 변경사항**:
```java
@Scheduled(cron = "0 0 5 * * *", zone = "Asia/Seoul")
public void dailySync() {
    // ✅ 베스트셀러가 아닌 학년별/과목별 검색
    syncSchoolLevelSubjects("초등", new String[]{"수학", "영어", "국어"});
    syncSchoolLevelSubjects("중등", new String[]{"수학", "영어", "국어", "과학", "사회"});
    syncSchoolLevelSubjects("고등", new String[]{"수학", "영어", "국어", "과학", "사회"});
}
```

**검색 키워드 예시**:
- "초등 수학 참고서"
- "중등 영어 참고서"
- "고등 과학 참고서"

---

### 4️⃣ 중복 감지 및 비교 로직 개선

**문제**: API에서 가져온 데이터와 DB 데이터 비교가 제대로 안 됨

**해결**:

```bash
# 기존 파일 백업
mv app/src/main/java/edu/bookpict/aladin/service/AladinBookService.java \
   app/src/main/java/edu/bookpict/aladin/service/AladinBookService.java.backup

# 수정된 파일 복사
cp AladinBookService_Fixed.java \
   app/src/main/java/edu/bookpict/aladin/service/AladinBookService.java
```

**주요 개선사항**:

#### A. 개별 트랜잭션 처리
```java
// ✅ 각 도서를 독립적인 트랜잭션으로 처리
@Transactional(propagation = Propagation.REQUIRES_NEW)
public boolean syncItemSafely(AladinResponseDto.Item item) {
    // 하나 실패해도 다른 도서는 계속 처리
}
```

#### B. 스냅샷 변경사항 감지
```java
// ✅ 가격이나 순위가 변경되면 로그 출력
private void logSnapshotChanges(AladinSnapshot old, Item newItem) {
    if (!equals(old.getPriceSales(), newItem.getPriceSales())) {
        log.info("💰 Price changed: {} → {}", 
                 old.getPriceSales(), newItem.getPriceSales());
    }
}
```

#### C. ISBN 검증 강화
```java
// ✅ ISBN-13 체크섬 검증
private boolean isValidIsbn13(String isbn) {
    if (!isbn.matches("\\d{13}")) return false;
    
    int sum = 0;
    for (int i = 0; i < 12; i++) {
        int digit = isbn.charAt(i) - '0';
        sum += (i % 2 == 0) ? digit : digit * 3;
    }
    int checkDigit = (10 - (sum % 10)) % 10;
    return checkDigit == (isbn.charAt(12) - '0');
}
```

#### D. 더 나은 정보로만 업데이트
```java
// ✅ 기존 데이터보다 더 상세한 정보일 때만 업데이트
if (book.getSummary() == null || 
    book.getSummary().length() < newSummary.length()) {
    book.setSummary(newSummary);
}
```

---

## 📋 실행 순서

### Step 1: 백업 및 파일 교체

```bash
# 1. 프로젝트 루트로 이동
cd /path/to/bookpict

# 2. 수정된 파일들 복사
cp application_fixed.yml app/src/main/resources/application.yml
cp DataInitializer_Fixed.java app/src/main/java/edu/bookpict/config/DataInitializer.java
cp AladinSyncScheduler_Fixed.java app/src/main/java/edu/bookpict/aladin/scheduler/AladinSyncScheduler.java
cp AladinBookService_Fixed.java app/src/main/java/edu/bookpict/aladin/service/AladinBookService.java
```

### Step 2: 디버그 코드 제거

```bash
# entity 패키지 삭제
rm -rf app/src/main/java/edu/bookpict/entity/

# repository 패키지 (domain이 아닌) 삭제
rm -rf app/src/main/java/edu/bookpict/repository/

# 디버그 컨트롤러 삭제
rm app/src/main/java/edu/bookpict/controller/DebugController.java

# 디버그 서비스 삭제  
rm app/src/main/java/edu/bookpict/service/BookDebugService.java

# 디버그 DTO 삭제
rm app/src/main/java/edu/bookpict/dto/AladinBookDto.java

# TestDataInitializer 삭제
rm app/src/main/java/edu/bookpict/config/TestDataInitializer.java
```

### Step 3: 기존 H2 데이터베이스 삭제 (선택사항)

```bash
# 깨끗하게 시작하려면 기존 DB 파일 삭제
rm -rf data/bookpict.*
```

### Step 4: 빌드 및 실행

```bash
# Gradle 캐시 정리
./gradlew clean

# 빌드
./gradlew build

# 실행
./gradlew bootRun
```

---

## 🧪 테스트 방법

### 1. 애플리케이션 시작 확인

```bash
# 로그에서 확인
tail -f logs/bookpict.log
```

**예상 로그**:
```
📚 Database already has 3 books. Skipping initialization.
✅ Database initialization completed.
```

### 2. 데이터 영구성 확인

```bash
# 1. 애플리케이션 시작
./gradlew bootRun

# 2. 브라우저에서 확인
# http://localhost:8080

# 3. 애플리케이션 종료 (Ctrl+C)

# 4. 다시 시작
./gradlew bootRun

# 5. 데이터가 그대로 있는지 확인
```

### 3. 스케줄러 수동 테스트

```bash
# API 호출
curl -X POST http://localhost:8080/test/aladin/seed?query=초등+수학+참고서

# 응답 확인
# "Imported books for query: 초등 수학 참고서"
```

### 4. H2 콘솔에서 직접 확인

```
URL: http://localhost:8080/h2-console
JDBC URL: jdbc:h2:file:./data/bookpict
Username: sa
Password: (비워두기)

SQL:
SELECT * FROM book;
SELECT * FROM aladin_snapshot;
SELECT * FROM search_keyword;
```

---

## 🔍 로그 확인 방법

### A. 데이터 동기화 로그
```
✅ Sync complete: 5 new, 10 updated, 0 failed
💰 Price changed for 9788961335345: 16200 → 15800
📈 Rank changed for 9788928330123: 12 → 8
```

### B. 스케줄러 실행 로그
```
====================================
🚀 Starting Daily Reference Book Sync
====================================
📚 Syncing 초등 books...
  ➡️  Searching: 초등 수학 참고서
  ➡️  Searching: 초등 영어 참고서
  ✅ 초등 completed: 3 subjects processed
====================================
✅ Daily Sync Completed: 13 queries processed
====================================
```

### C. 에러 로그
```
❌ Failed to sync item 9780000000000: Invalid ISBN-13
⚠️  Invalid ISBN-13: 9780000000000
```

---

## 🚀 검증 체크리스트

- [ ] 애플리케이션이 정상적으로 시작됨
- [ ] 빌드 오류가 없음 (디버그 코드 충돌 해결)
- [ ] 재시작 후에도 데이터가 유지됨
- [ ] `/api/books` 엔드포인트가 정상 작동
- [ ] 검색 기능이 정상 작동
- [ ] H2 콘솔에서 데이터 확인 가능
- [ ] 로그에 "Skipping initialization" 메시지 출력
- [ ] data/ 폴더에 bookpict.mv.db 파일 생성됨

---

## 💡 추가 개선 사항

### 1. 알라딘 카테고리 ID 활용

알라딘 API는 카테고리 ID로 더 정확한 검색이 가능합니다:

```java
// 참고서 카테고리 ID (예시)
Map<String, String> CATEGORY_IDS = Map.of(
    "초등_수학", "4105",
    "중등_수학", "50933",
    "고등_수학", "50943",
    "초등_영어", "4106",
    // ... 추가
);
```

### 2. 동기화 결과 저장

```java
@Entity
public class SyncHistory {
    @Id
    @GeneratedValue
    private Long id;
    
    private LocalDateTime syncTime;
    private String syncType;  // "daily", "retry", "manual"
    private Integer booksAdded;
    private Integer booksUpdated;
    private Integer booksFailed;
    private String errorMessage;
}
```

### 3. 웹훅/알림 시스템

```java
// 새 도서 발견 시 Slack/Discord/Email 알림
@EventListener
public void onNewBookDiscovered(NewBookEvent event) {
    slackService.sendMessage(
        "📚 New book discovered: " + event.getBook().getTitle()
    );
}
```

---

## 🆘 문제 해결 (Troubleshooting)

### Q1: 빌드 시 "Book 엔티티를 찾을 수 없음" 오류

**원인**: 디버그 코드가 완전히 제거되지 않았거나 import 문제

**해결**:
```bash
# 모든 Book import 확인
grep -r "import.*entity.Book" app/src/main/java/

# 발견되면 수정
# import edu.bookpict.entity.Book;  ← 삭제
# import edu.bookpict.domain.book.Book;  ← 사용
```

### Q2: "Table 'BOOK' already exists" 오류

**원인**: ddl-auto를 update로 변경했는데 충돌

**해결**:
```bash
# DB 삭제 후 재시작
rm -rf data/bookpict.*
./gradlew bootRun
```

### Q3: 스케줄러가 실행되지 않음

**원인**: `@EnableScheduling` 누락

**확인**:
```java
@SpringBootApplication
@EnableScheduling  // ← 이것이 있는지 확인
public class App {
    // ...
}
```

### Q4: API 호출이 너무 느림

**원인**: Rate limit 또는 네트워크 문제

**해결**:
```java
// AladinSyncScheduler에서 딜레이 조정
Thread.sleep(2000);  // 1초 → 2초로 증가
```

---

## 📞 완료 후 확인사항

모든 수정이 완료되면:

1. ✅ 깨끗한 빌드 성공
2. ✅ 데이터 영구 저장 확인
3. ✅ 스케줄러 정상 작동
4. ✅ API 테스트 성공
5. ✅ 로그 정상 출력

이제 안정적으로 운영 가능합니다! 🎉

# 교육 참고서 비교 플랫폼

Spring Boot + Thymeleaf 기반의 교육 참고서 가격 비교 서비스입니다.

## 🎨 디자인

제공하신 `1stTab.html`과 `2ndTab.html` 디자인을 그대로 구현했습니다:
- Olive-green 컬러 스킴 (#6a8571)
- Plus Jakarta Sans 폰트
- 모바일 우선 반응형 디자인
- 탭 전환 UI (도서 목록 / 최근 검색어)
- 아코디언 도서 카드

## 🏗️ 아키텍처

```
Browser
  ↓
Spring Boot
 ├─ Web (Thymeleaf SSR)
 ├─ REST API
 ├─ JPA/Hibernate
 └─ H2 Database (개발용)
```

## 📦 구현된 기능

### 도메인 모델
- ✅ `Book` - 도서 정보 (ISBN, 제목, 저자, 출판사, 카테고리, 학년)
- ✅ `Store` - 서점 정보 (교보문고, YES24, 알라딘, 인터파크, 쿠팡)
- ✅ `StoreBook` - 서점별 가격 정보 (정가, 할인가, 배송비)
- ✅ `SearchLog` - 검색 히스토리

### 서비스 레이어
- ✅ `BookService` - 도서 조회, 검색, 가격 비교
- ✅ `SearchService` - 검색 및 검색 히스토리 관리

### 웹 레이어
- ✅ `PageController` - Thymeleaf 페이지 렌더링
- ✅ `ApiController` - REST API 엔드포인트

### 프론트엔드
- ✅ `index.html` - 메인 페이지 (도서 목록 + 검색 히스토리 탭)
- ✅ 디자인 시스템 (Tailwind CSS)
- ✅ 탭 전환 JavaScript
- ✅ 아코디언 UI

### 데이터
- ✅ `DataInitializer` - 샘플 데이터 자동 생성
  - 10권의 참고서 (수학, 영어, 국어, 과학, 사회)
  - 5개 서점
  - 가격 비교 데이터

## 🚀 실행 방법

### 1. 필수 요구사항
- Java 21
- Gradle 9.2.0 (포함됨)

### 2. 애플리케이션 실행

```bash
# Windows
.\gradlew.bat :app:bootRun

# Linux/Mac
./gradlew :app:bootRun
```

### 3. 접속

- **메인 페이지**: http://localhost:8080
- **H2 콘솔**: http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:mem:bookpict`
  - Username: `sa`
  - Password: (비워두기)

## 📱 화면 구성

### 1stTab - 도서 목록
- 검색 바
- 도서 카드 (아코디언)
  - 기본 정보: 제목, 저자, 카테고리, 학년, 최저가
  - 확장 시: 썸네일, 상세 정보, 가격 비교 버튼

### 2ndTab - 최근 검색어
- 검색 히스토리 목록
- 재검색 기능
- 개별/전체 삭제

## 🔌 API 엔드포인트

### 도서 API
```
GET /api/books                    # 전체 도서 목록
GET /api/books?category=수학       # 카테고리별 조회
GET /api/books?category=수학&grade=고등  # 카테고리+학년 조회
GET /api/books/search?q=개념원리   # 검색
GET /api/books/{id}               # 상세 정보
```

### 검색 API
```
GET /api/search/recent            # 최근 검색어
```

## 📂 프로젝트 구조

```
app/src/main/java/edu/bookpict/
├── App.java                      # Spring Boot 메인 클래스
├── config/
│   └── DataInitializer.java     # 샘플 데이터 생성
├── domain/
│   ├── book/
│   │   ├── Book.java
│   │   └── repository/
│   ├── store/
│   │   ├── Store.java
│   │   ├── StoreBook.java
│   │   └── repository/
│   └── search/
│       ├── SearchLog.java
│       └── repository/
├── service/
│   ├── BookService.java
│   └── SearchService.java
└── web/
    ├── controller/
    │   ├── PageController.java
    │   └── ApiController.java
    └── dto/
        ├── BookListDto.java
        ├── BookDetailDto.java
        └── PriceInfoDto.java

app/src/main/resources/
├── application.yml               # 설정 파일
└── templates/
    ├── layout.html              # 공통 레이아웃
    └── index.html               # 메인 페이지
```

## 🎯 다음 단계

### Phase 1: 크롤러 구현
- [ ] 서점별 파서 (교보문고, YES24, 알라딘)
- [ ] 스케줄러 설정
- [ ] 크롤링 상태 관리

### Phase 2: 추가 기능
- [ ] 도서 상세 페이지
- [ ] 필터링 (카테고리, 학년, 가격대)
- [ ] 정렬 (인기순, 최신순, 가격순)
- [ ] 페이지네이션

### Phase 3: UI 개선
- [ ] 로딩 인디케이터
- [ ] 에러 처리
- [ ] 반응형 최적화

### Phase 4: 프로덕션 준비
- [ ] PostgreSQL 연동
- [ ] 캐싱 (Redis)
- [ ] 로깅 개선
- [ ] 테스트 작성

## 📝 참고사항

- 현재 H2 인메모리 데이터베이스를 사용 중입니다 (재시작 시 데이터 초기화)
- 프로덕션 환경에서는 PostgreSQL로 전환 필요
- 크롤러 기능은 현재 비활성화 상태 (`crawler.enabled=false`)

## 🐛 트러블슈팅

### 포트 충돌
```bash
# 8080 포트가 사용 중인 경우
# application.yml에서 server.port 변경
```

### Gradle 빌드 오류
```bash
# 캐시 정리
./gradlew clean

# 의존성 다시 다운로드
./gradlew --refresh-dependencies
```

## 📄 라이선스

MIT License

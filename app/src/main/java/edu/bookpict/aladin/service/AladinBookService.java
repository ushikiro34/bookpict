package edu.bookpict.aladin.service;

import edu.bookpict.aladin.client.AladinApiClient;
import edu.bookpict.aladin.client.AladinResponseDto;
import edu.bookpict.aladin.mapper.AladinMapper;
import edu.bookpict.domain.book.AladinSnapshot;
import edu.bookpict.domain.book.Book;
import edu.bookpict.domain.book.Edition;
import edu.bookpict.domain.book.Isbn;
import edu.bookpict.domain.book.repository.AladinSnapshotRepository;
import edu.bookpict.domain.book.repository.BookRepository;
import edu.bookpict.domain.book.repository.EditionRepository;
import edu.bookpict.domain.book.repository.IsbnRepository;
import edu.bookpict.domain.search.SearchKeyword;
import edu.bookpict.domain.search.repository.SearchKeywordRepository;
import edu.bookpict.service.RankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AladinBookService {

    private final AladinApiClient aladinApiClient;
    private final AladinMapper aladinMapper;
    private final AladinSnapshotRepository snapshotRepository;
    private final BookRepository bookRepository;
    private final IsbnRepository isbnRepository;
    private final EditionRepository editionRepository;
    private final SearchKeywordRepository searchKeywordRepository;
    private final RankingService rankingService;

    /**
     * 베스트셀러 동기화 (ItemList API)
     * ⚠️ 주의: 이 메서드는 deprecated. dailySync에서는 사용하지 않음
     */
    @Deprecated
    @Transactional
    public void syncBestsellers(String categoryId) {
        log.info("📊 Starting Aladin Bestseller Sync for category: {}", categoryId);
        String response = aladinApiClient.getItemList("Bestseller", 50, 1, categoryId);
        processResponse(response, null);
    }

    /**
     * 도서 상세 정보 동기화 (ItemLookUp API)
     */
    @Transactional
    public void syncBookDetail(String isbn13) {
        log.info("🔍 Starting Aladin Book Detail Sync for ISBN: {}", isbn13);
        String response = aladinApiClient.lookUpItem(isbn13, "ISBN13");
        processResponse(response, null);
    }

    /**
     * 도서 검색 및 동기화 (ItemSearch API)
     * 페이지네이션으로 최대 200건 수집 (50건 x 4페이지)
     */
    @Transactional
    public void searchAndSync(String query) {
        log.info("🔎 Searching Aladin and Syncing for query: '{}'", query);

        int totalPages = 4;
        int pageSize = 50;

        for (int page = 0; page < totalPages; page++) {
            try {
                int startIndex = page * pageSize + 1; // 1, 51, 101, 151
                int startRank = startIndex; // SalesPoint 정렬 순서 = 판매량 순위
                String response = aladinApiClient.searchItems(query, "Keyword", pageSize, startIndex);
                processResponse(response, query, startRank);

                if (page < totalPages - 1) {
                    Thread.sleep(500);
                }
            } catch (Exception e) {
                log.error("❌ Failed to search page {} for query '{}': {}", page + 1, query, e.getMessage());
                break;
            }
        }
    }

    /**
     * 도서 검색 및 동기화 (단일 페이지, 개수 반환)
     * 일회용 전체 수집용
     *
     * @param query 검색 쿼리
     * @param startIndex 시작 인덱스
     * @param pageSize 페이지 크기
     * @return 동기화된 도서 수
     */
    @Transactional
    public int searchAndSyncWithCount(String query, int startIndex, int pageSize) {
        SyncResult result = searchAndSyncWithDedup(query, startIndex, pageSize, new HashSet<>());
        return result.getTotalProcessed();
    }

    /**
     * ✅ 도서 검색 및 동기화 (중복 체크 포함)
     * 세션 내 이미 처리한 ISBN을 건너뛰고, 중복 통계 반환
     *
     * @param query 검색 쿼리
     * @param startIndex 시작 인덱스
     * @param pageSize 페이지 크기
     * @param processedIsbns 이미 처리한 ISBN Set (세션 내 공유)
     * @return SyncResult (처리 수, 신규 ISBN 수, 중복 수)
     */
    @Transactional
    public SyncResult searchAndSyncWithDedup(String query, int startIndex, int pageSize, Set<String> processedIsbns) {
        try {
            String response = aladinApiClient.searchItems(query, "Keyword", pageSize, startIndex);
            return processResponseWithDedup(response, query, startIndex, processedIsbns);
        } catch (Exception e) {
            log.error("❌ Failed to search for query '{}' at index {}: {}", query, startIndex, e.getMessage());
            return new SyncResult(0, 0, 0);
        }
    }

    /**
     * 응답 처리 및 동기화 (개수 반환)
     */
    private int processResponseWithCount(String responseJson, String query, int startRank) {
        SyncResult result = processResponseWithDedup(responseJson, query, startRank, new HashSet<>());
        return result.getTotalProcessed();
    }

    /**
     * ✅ 응답 처리 및 동기화 (중복 체크 포함)
     * 이미 처리한 ISBN은 건너뛰고 통계 반환
     */
    private SyncResult processResponseWithDedup(String responseJson, String query, int startRank, Set<String> processedIsbns) {
        if (responseJson == null || responseJson.trim().isEmpty()) {
            return new SyncResult(0, 0, 0);
        }

        AladinResponseDto dto = aladinMapper.parseResponse(responseJson);
        if (dto == null || dto.getItem() == null || dto.getItem().isEmpty()) {
            return new SyncResult(0, 0, 0);
        }

        // 쿼리에서 과목/학교급 추출
        QueryInfo queryInfo = parseQueryInfo(query);

        int totalProcessed = 0;
        int newIsbns = 0;
        int duplicateIsbns = 0;
        int rank = startRank;

        for (AladinResponseDto.Item item : dto.getItem()) {
            String isbn13 = item.getIsbn13();

            // ✅ 세션 내 중복 체크
            if (isbn13 != null && processedIsbns.contains(isbn13)) {
                duplicateIsbns++;
                totalProcessed++;
                log.debug("⏭️ Skipping duplicate ISBN in session: {}", isbn13);
                rank++; // 랭크는 계속 증가
                continue;
            }

            try {
                int currentRank = rank;
                if (startRank > 0) {
                    item.setBestRank(rank++);
                }

                syncItemSafely(item);
                totalProcessed++;

                // ✅ 처리한 ISBN 기록
                if (isbn13 != null) {
                    processedIsbns.add(isbn13);
                    newIsbns++;
                }

                // 랭킹 저장
                if (query != null && startRank > 0 && isbn13 != null) {
                    try {
                        Book book = findBookByIsbn13(isbn13);
                        if (book != null) {
                            rankingService.saveRanking(
                                isbn13,
                                query,
                                queryInfo.subject,
                                queryInfo.schoolLevel,
                                currentRank,
                                book
                            );
                        }
                    } catch (Exception e) {
                        // 랭킹 저장 실패는 무시
                    }
                }
            } catch (Exception e) {
                log.debug("⚠️ Failed to sync item {}: {}", isbn13, e.getMessage());
                totalProcessed++;
            }
        }

        log.info("    → Page result: {} total, {} new, {} duplicates ({}% dup rate)",
                totalProcessed, newIsbns, duplicateIsbns,
                totalProcessed > 0 ? String.format("%.0f", (double) duplicateIsbns / totalProcessed * 100) : 0);

        return new SyncResult(totalProcessed, newIsbns, duplicateIsbns);
    }

    /**
     * 응답 처리 및 동기화 (순위 없이)
     */
    private void processResponse(String responseJson, String query) {
        processResponse(responseJson, query, 0);
    }

    /**
     * 응답 처리 및 동기화
     * @param startRank 0이면 순위 미설정, 1 이상이면 해당 값부터 순번으로 bestRank 설정
     */
    private void processResponse(String responseJson, String query, int startRank) {
        if (responseJson == null || responseJson.trim().isEmpty()) {
            log.warn("⚠️  Empty response from Aladin API");
            return;
        }

        AladinResponseDto dto = aladinMapper.parseResponse(responseJson);
        if (dto == null || dto.getItem() == null || dto.getItem().isEmpty()) {
            log.warn("⚠️  No items found in Aladin response");
            return;
        }

        log.info("📦 Processing {} items from Aladin (startRank={})", dto.getItem().size(), startRank);

        // 쿼리에서 과목/학교급 추출 (예: "초등 수학 참고서" → 학교급=초등, 과목=수학)
        QueryInfo queryInfo = parseQueryInfo(query);

        int newBooks = 0;
        int updated = 0;
        int failed = 0;
        int rankingsAdded = 0;
        int rank = startRank;

        for (AladinResponseDto.Item item : dto.getItem()) {
            try {
                // SalesPoint 정렬 순서를 bestRank로 설정
                int currentRank = rank;
                if (startRank > 0) {
                    item.setBestRank(rank++);
                }

                boolean existed = syncItemSafely(item);
                if (existed) {
                    updated++;
                } else {
                    newBooks++;
                }

                // ✅ 랭킹 저장 (쿼리 정보가 있고, 순위가 유효할 때)
                if (query != null && startRank > 0 && item.getIsbn13() != null) {
                    try {
                        Book book = findBookByIsbn13(item.getIsbn13());
                        if (book != null) {
                            rankingService.saveRanking(
                                item.getIsbn13(),
                                query,
                                queryInfo.subject,
                                queryInfo.schoolLevel,
                                currentRank,
                                book
                            );
                            rankingsAdded++;
                        }
                    } catch (Exception e) {
                        log.debug("⚠️  Failed to save ranking for {}: {}", item.getIsbn13(), e.getMessage());
                    }
                }
            } catch (Exception e) {
                failed++;
                log.error("❌ Failed to sync item {}: {}", item.getIsbn13(), e.getMessage());
            }
        }

        log.info("✅ Sync complete: {} new, {} updated, {} failed, {} rankings",
                 newBooks, updated, failed, rankingsAdded);

        // 새 도서가 발견된 경우 검색어 저장
        if (newBooks > 0 && query != null && !query.isEmpty()) {
            saveSearchKeywordForLater(query);
        }
    }

    /**
     * 쿼리 정보 파싱 (예: "초등 수학 참고서" → 학교급=초등, 과목=수학)
     */
    private QueryInfo parseQueryInfo(String query) {
        if (query == null || query.isEmpty()) {
            return new QueryInfo(null, null);
        }

        String schoolLevel = null;
        String subject = null;

        // 학교급 추출
        if (query.contains("초등")) schoolLevel = "초등";
        else if (query.contains("중등") || query.contains("중학")) schoolLevel = "중등";
        else if (query.contains("고등")) schoolLevel = "고등";

        // 과목 추출
        if (query.contains("수학")) subject = "수학";
        else if (query.contains("영어")) subject = "영어";
        else if (query.contains("국어")) subject = "국어";
        else if (query.contains("한국사")) subject = "한국사";
        else if (query.contains("세계사")) subject = "세계사";
        else if (query.contains("과학")) subject = "과학";
        else if (query.contains("사회")) subject = "사회";

        return new QueryInfo(schoolLevel, subject);
    }

    /**
     * 쿼리 정보 클래스
     */
    private static class QueryInfo {
        final String schoolLevel;
        final String subject;

        QueryInfo(String schoolLevel, String subject) {
            this.schoolLevel = schoolLevel;
            this.subject = subject;
        }
    }

    /**
     * ✅ 동기화 결과 클래스 (중복 체크용)
     */
    public static class SyncResult {
        private final int totalProcessed;
        private final int newIsbns;
        private final int duplicateIsbns;

        public SyncResult(int totalProcessed, int newIsbns, int duplicateIsbns) {
            this.totalProcessed = totalProcessed;
            this.newIsbns = newIsbns;
            this.duplicateIsbns = duplicateIsbns;
        }

        public int getTotalProcessed() { return totalProcessed; }
        public int getNewIsbns() { return newIsbns; }
        public int getDuplicateIsbns() { return duplicateIsbns; }

        public double getDuplicateRate() {
            return totalProcessed > 0 ? (double) duplicateIsbns / totalProcessed : 0.0;
        }
    }

    /**
     * ISBN13으로 Book 조회
     */
    private Book findBookByIsbn13(String isbn13) {
        return isbnRepository.findById(isbn13)
                .map(Isbn::getBook)
                .orElse(null);
    }

    /**
     * ✅ 개선: 개별 트랜잭션으로 안전하게 동기화
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean syncItemSafely(AladinResponseDto.Item item) {
        String isbn13 = item.getIsbn13();
        if (isbn13 == null || isbn13.isEmpty()) {
            log.debug("⏭️  Skipping item without ISBN");
            return false;
        }

        // ISBN 유효성 검증
        if (!isValidIsbn13(isbn13)) {
            log.warn("⚠️  Invalid ISBN-13: {}", isbn13);
            return false;
        }

        // 1. AladinSnapshot 업데이트 또는 생성
        boolean snapshotUpdated = updateOrCreateSnapshot(item);

        // 2. Book 계층 확인 및 업데이트
        boolean bookExists = ensureBookHierarchy(item);

        return bookExists; // true면 기존 도서, false면 신규 도서
    }

    /**
     * ✅ 개선: AladinSnapshot 업데이트 로직
     * - 기존 스냅샷과 비교하여 변경사항만 로그
     */
    private boolean updateOrCreateSnapshot(AladinResponseDto.Item item) {
        String isbn13 = item.getIsbn13();
        
        Optional<AladinSnapshot> existingOpt = snapshotRepository.findByIsbn13(isbn13);
        
        AladinSnapshot snapshot;
        boolean isNew = false;
        
        if (existingOpt.isPresent()) {
            snapshot = existingOpt.get();
            
            // ✅ 변경사항 로깅
            logSnapshotChanges(snapshot, item);
            
        } else {
            snapshot = new AladinSnapshot();
            snapshot.setIsbn13(isbn13);
            isNew = true;
            log.info("📝 Creating new snapshot for ISBN: {}", isbn13);
        }

        // 데이터 업데이트
        snapshot.setPriceSales(item.getPriceSales());
        snapshot.setPriceStandard(item.getPriceStandard());

        // Rating 및 ReviewCount 매핑
        if (item.getSubInfo() != null && item.getSubInfo().getRatingInfo() != null) {
            snapshot.setRating(item.getSubInfo().getRatingInfo().getRatingScore());
            snapshot.setReviewCount(item.getSubInfo().getRatingInfo().getRatingCount());
        } else if (item.getCustomerReviewRank() != null) {
            snapshot.setRating(BigDecimal.valueOf(item.getCustomerReviewRank()).divide(BigDecimal.valueOf(2)));
            snapshot.setReviewCount(0);
        }

        // BestsellerRank
        if (item.getBestSellerRank() != null) {
            snapshot.setBestsellerRank(item.getBestSellerRank().getRank());
        } else if (item.getBestRank() != null) {
            snapshot.setBestsellerRank(item.getBestRank());
        }

        snapshot.setProductUrl(item.getLink());
        snapshot.setCollectedAt(LocalDateTime.now());
        
        snapshotRepository.save(snapshot);
        
        return !isNew;
    }

    /**
     * ✅ 스냅샷 변경사항 로깅
     */
    private void logSnapshotChanges(AladinSnapshot old, AladinResponseDto.Item newItem) {
        boolean hasChanges = false;
        
        if (!equals(old.getPriceSales(), newItem.getPriceSales())) {
            log.info("💰 Price changed for {}: {} → {}", 
                    old.getIsbn13(), old.getPriceSales(), newItem.getPriceSales());
            hasChanges = true;
        }
        
        if (old.getBestsellerRank() != null && newItem.getBestRank() != null
                && !old.getBestsellerRank().equals(newItem.getBestRank())) {
            log.info("📈 Rank changed for {}: {} → {}", 
                    old.getIsbn13(), old.getBestsellerRank(), newItem.getBestRank());
            hasChanges = true;
        }
        
        if (!hasChanges) {
            log.debug("✓ No significant changes for ISBN: {}", old.getIsbn13());
        }
    }

    /**
     * Book 계층 확인 및 업데이트
     * ✅ 개선: 신규 도서는 자동으로 생성
     */
    private boolean ensureBookHierarchy(AladinResponseDto.Item item) {
        Optional<Isbn> isbnOpt = isbnRepository.findById(item.getIsbn13());
        
        if (isbnOpt.isPresent()) {
            // 기존 ISBN 존재
            Isbn isbn = isbnOpt.get();
            Book book = isbn.getBook();
            
            if (book != null) {
                updateBookMetadata(book, item);
                log.debug("✓ Updated existing book: {}", book.getTitle());
                return true; // 기존 도서
            }
        }
        
        // ✅ 신규 도서 자동 생성
        createNewBookHierarchy(item);
        return false; // 신규 도서
    }

    /**
     * ✅ Book 메타데이터 업데이트 (더 나은 정보로만 덮어쓰기)
     */
    private void updateBookMetadata(Book book, AladinResponseDto.Item item) {
        boolean updated = false;

        // categoryName 업데이트
        if (item.getCategoryName() != null && !item.getCategoryName().trim().isEmpty() && book.getCategoryName() == null) {
            book.setCategoryName(item.getCategoryName());
            updated = true;
        }

        // description 업데이트
        if (item.getDescription() != null && !item.getDescription().trim().isEmpty()) {
            if (book.getDescription() == null || book.getDescription().length() < item.getDescription().length()) {
                book.setDescription(item.getDescription());
                updated = true;
            }
        }

        // fullDescription 업데이트
        if (item.getFullDescription() != null && !item.getFullDescription().trim().isEmpty()) {
            if (book.getFullDescription() == null || book.getFullDescription().length() < item.getFullDescription().length()) {
                book.setFullDescription(item.getFullDescription());
                updated = true;
            }
        }

        // Summary 업데이트 (fullDescription 우선)
        if (item.getFullDescription() != null && !item.getFullDescription().trim().isEmpty()) {
            if (book.getSummary() == null || book.getSummary().length() < item.getFullDescription().length()) {
                book.setSummary(item.getFullDescription());
                updated = true;
            }
        } else if (item.getDescription() != null && book.getSummary() == null) {
            book.setSummary(item.getDescription());
            updated = true;
        }

        // TOC 업데이트
        String newToc = item.getToc();
        if (newToc == null && item.getSubInfo() != null) {
            newToc = item.getSubInfo().getToc();
        }
        if (newToc != null && !newToc.trim().isEmpty()) {
            if (book.getToc() == null || book.getToc().length() < newToc.length()) {
                book.setToc(newToc);
                updated = true;
            }
        }

        // Thumbnail 업데이트 (placeholder가 아닌 경우만)
        if (item.getCover() != null && !item.getCover().contains("placeholder")) {
            if (book.getThumbnailUrl() == null || book.getThumbnailUrl().contains("placeholder")) {
                book.setThumbnailUrl(item.getCover());
                updated = true;
            }
        }

        // 학년/학기 정보가 더 구체적으로 추출되면 업데이트
        edu.bookpict.aladin.parser.GradeParser.GradeInfo gradeInfo = edu.bookpict.aladin.parser.GradeParser.detectGrade(
                item.getCategoryName() != null ? item.getCategoryName() : "",
                item.getTitle() != null ? item.getTitle() : "");
        if (gradeInfo.getGrade() != null && book.getGrade() == null) {
            book.setGrade(gradeInfo.getGrade());
            book.setSemester(gradeInfo.getSemester());
            updated = true;
        }

        if (updated) {
            bookRepository.save(book);
            log.info("📝 Updated metadata for: {}", book.getTitle());
        }
    }

    /**
     * 검색어 저장 (나중에 재처리용)
     */
    private void saveSearchKeywordForLater(String query) {
        if (searchKeywordRepository.findByKeyword(query).isEmpty()) {
            searchKeywordRepository.save(SearchKeyword.builder()
                    .keyword(query)
                    .build());
            log.info("💾 Saved keyword for later processing: '{}'", query);
        }
    }

    /**
     * ✅ 저장된 검색어 재처리
     */
    @Transactional
    public void reprocessSavedKeywords() {
        List<SearchKeyword> keywords = searchKeywordRepository.findAll();
        
        if (keywords.isEmpty()) {
            log.info("ℹ️  No saved keywords to reprocess");
            return;
        }
        
        log.info("🔄 Reprocessing {} saved keywords", keywords.size());
        
        for (SearchKeyword keyword : keywords) {
            try {
                log.info("  ➡️  Reprocessing: '{}'", keyword.getKeyword());
                searchAndSync(keyword.getKeyword());
                
                // 성공하면 삭제
                searchKeywordRepository.delete(keyword);
                
                // Rate limit 방지
                Thread.sleep(1000);
                
            } catch (Exception e) {
                log.error("  ❌ Failed to reprocess '{}': {}", keyword.getKeyword(), e.getMessage());
            }
        }
    }

    /**
     * ✅ 도서 강제 임포트 (테스트/관리용)
     */
    @Transactional
    public void importBooksFromSearch(String query) {
        log.info("📥 Force importing books from Aladin for query: '{}'", query);
        
        String response = aladinApiClient.searchItems(query, "Keyword", 20, 1);
        processImportResponse(response);
    }

    /**
     * 관리용: 기존 DB의 모든 책에 대해 학년/학기 정보를 재추출하여 더 구체적인 값이 나오면 업데이트
     */
    @Transactional
    public int reprocessAllBooksGradeGroup() {
        int updated = 0;
        for (edu.bookpict.domain.book.Book book : bookRepository.findAll()) {
            edu.bookpict.aladin.parser.GradeParser.GradeInfo gradeInfo = edu.bookpict.aladin.parser.GradeParser.detectGrade(
                    book.getSubject() != null ? book.getSubject() : "",
                    book.getTitle() != null ? book.getTitle() : "");
            if (gradeInfo.getGrade() != null && book.getGrade() == null) {
                book.setGrade(gradeInfo.getGrade());
                book.setSemester(gradeInfo.getSemester());
                bookRepository.save(book);
                updated++;
                log.info("🔧 Updated grade for '{}' -> {} {}", book.getTitle(), gradeInfo.getGrade(), gradeInfo.getSemester());
            }
        }
        log.info("✅ Reprocessed grade for {} books", updated);
        return updated;
    }

    private void processImportResponse(String responseJson) {
        if (responseJson == null) return;

        AladinResponseDto dto = aladinMapper.parseResponse(responseJson);
        if (dto == null || dto.getItem() == null) return;

        for (AladinResponseDto.Item item : dto.getItem()) {
            boolean exists = isbnRepository.existsById(item.getIsbn13());
            
            if (!exists) {
                createNewBookHierarchy(item);
            }
            
            // 스냅샷은 항상 업데이트
            syncItemSafely(item);
        }
    }

    /**
     * ✅ 신규 도서 계층 생성
     */
    private void createNewBookHierarchy(AladinResponseDto.Item item) {
        String categoryName = item.getCategoryName() != null ? item.getCategoryName() : "";
        String title = item.getTitle() != null ? item.getTitle() : "";
        
        // 자동 분류
        String subject = detectSubject(categoryName, title);
        String schoolLevel = detectSchoolLevel(categoryName, title);
        edu.bookpict.aladin.parser.GradeParser.GradeInfo gradeInfo = edu.bookpict.aladin.parser.GradeParser.detectGrade(categoryName, title);

        Book book = Book.builder()
                .bookId("ALADIN-" + java.util.UUID.randomUUID().toString().substring(0, 8))
                .title(item.getTitle())
                .publisher(item.getPublisher())
                .subject(subject)
                .schoolLevel(schoolLevel)
                .grade(gradeInfo.getGrade())
                .semester(gradeInfo.getSemester())
                .curriculum("2022 개정")
                .categoryName(item.getCategoryName())
                .description(item.getDescription())
                .fullDescription(item.getFullDescription())
                .summary(item.getFullDescription() != null ? item.getFullDescription() : item.getDescription())
                .toc(item.getToc())
                .thumbnailUrl(item.getCover() != null ? item.getCover() : "/images/placeholder.png")
                .build();

        bookRepository.save(book);

        Isbn isbn = Isbn.builder()
                .isbn(item.getIsbn13())
                .book(book)
                .coverType("paperback")
                .pageCount(0)
                .build();
        isbnRepository.save(isbn);

        Edition edition = Edition.builder()
                .editionId(item.getIsbn13() + "-E1")
                .isbn(isbn)
                .editionNumber(1)
                .printNumber(1)
                .isLatest(true)
                .build();
        editionRepository.save(edition);

        log.info("✨ Created new book: {} (ISBN: {})", book.getTitle(), item.getIsbn13());
    }

    // ========== 유틸리티 메서드 ==========

    /**
     * ✅ ISBN-13 체크섬 검증
     */
    private boolean isValidIsbn13(String isbn) {
        if (isbn == null || !isbn.matches("\\d{13}")) {
            return false;
        }
        
        try {
            int sum = 0;
            for (int i = 0; i < 12; i++) {
                int digit = isbn.charAt(i) - '0';
                sum += (i % 2 == 0) ? digit : digit * 3;
            }
            int checkDigit = (10 - (sum % 10)) % 10;
            return checkDigit == (isbn.charAt(12) - '0');
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 과목 감지
     */
    private String detectSubject(String category, String title) {
        String combined = (category + " " + title).toLowerCase();
        
        if (combined.contains("수학")) return "수학";
        if (combined.contains("영어") || combined.contains("english")) return "영어";
        if (combined.contains("국어")) return "국어";
        if (combined.contains("과학") || combined.contains("물리") || combined.contains("화학") 
            || combined.contains("생물") || combined.contains("지구")) return "과학";
        // 역사 관련: 한국사, 세계사 우선 매핑
        if (combined.contains("한국사")) return "한국사";
        if (combined.contains("세계사")) return "세계사";
        // 일반적인 '역사' 또는 '사회' 키워드는 '사회'로 분류
        if (combined.contains("사회") || combined.contains("역사") || combined.contains("지리")) return "사회";
        
        return "기타";
    }

    /**
     * 학교급 감지
     */
    private String detectSchoolLevel(String category, String title) {
        String combined = (category + " " + title).toLowerCase();
        
        if (combined.contains("고등")) return "고등";
        if (combined.contains("중등") || combined.contains("중학")) return "중등";
        if (combined.contains("초등")) return "초등";
        
        return "기타";
    }

    /**
     * 학년/학기(grade group) 추출
     * 예: "4학년 2학기", "4학년", "1-2", "전학년"
     */


    /**
     * Null-safe equals
     */
    private boolean equals(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }
}

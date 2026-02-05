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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
     * ✅ 개선: 개별 트랜잭션으로 부분 실패 허용
     */
    @Transactional
    public void searchAndSync(String query) {
        log.info("🔎 Searching Aladin and Syncing for query: '{}'", query);
        
        try {
            String response = aladinApiClient.searchItems(query, "Keyword", 20, 1);
            processResponse(response, query);
            
        } catch (Exception e) {
            log.error("❌ Failed to search and sync for query '{}': {}", query, e.getMessage());
            throw new RuntimeException("Aladin search failed", e);
        }
    }

    /**
     * 응답 처리 및 동기화
     */
    private void processResponse(String responseJson, String query) {
        if (responseJson == null || responseJson.trim().isEmpty()) {
            log.warn("⚠️  Empty response from Aladin API");
            return;
        }

        AladinResponseDto dto = aladinMapper.parseResponse(responseJson);
        if (dto == null || dto.getItem() == null || dto.getItem().isEmpty()) {
            log.warn("⚠️  No items found in Aladin response");
            return;
        }

        log.info("📦 Processing {} items from Aladin", dto.getItem().size());

        int newBooks = 0;
        int updated = 0;
        int failed = 0;

        for (AladinResponseDto.Item item : dto.getItem()) {
            try {
                boolean existed = syncItemSafely(item);
                if (existed) {
                    updated++;
                } else {
                    newBooks++;
                }
            } catch (Exception e) {
                failed++;
                log.error("❌ Failed to sync item {}: {}", item.getIsbn13(), e.getMessage());
            }
        }

        log.info("✅ Sync complete: {} new, {} updated, {} failed", newBooks, updated, failed);

        // 새 도서가 발견된 경우 검색어 저장
        if (newBooks > 0 && query != null && !query.isEmpty()) {
            saveSearchKeywordForLater(query);
        }
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
        
        // 신규 도서 생성 필요
        return false;
    }

    /**
     * ✅ Book 메타데이터 업데이트 (더 나은 정보로만 덮어쓰기)
     */
    private void updateBookMetadata(Book book, AladinResponseDto.Item item) {
        boolean updated = false;

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
        String gradeGroup = "전학년";

        Book book = Book.builder()
                .bookId("ALADIN-" + java.util.UUID.randomUUID().toString().substring(0, 8))
                .title(item.getTitle())
                .publisher(item.getPublisher())
                .subject(subject)
                .schoolLevel(schoolLevel)
                .gradeGroup(gradeGroup)
                .curriculum("2022 개정")
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
     * Null-safe equals
     */
    private boolean equals(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }
}

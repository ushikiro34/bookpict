package edu.bookpict.service;

import lombok.extern.slf4j.Slf4j;
import edu.bookpict.domain.book.Book;
import edu.bookpict.domain.book.Edition;
import edu.bookpict.domain.book.Isbn;
import edu.bookpict.domain.book.repository.BookRepository;
import edu.bookpict.domain.book.repository.EditionRepository;
import edu.bookpict.domain.book.repository.IsbnRepository;
import edu.bookpict.web.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.bookpict.service.dto.AladinMetrics;
import edu.bookpict.domain.ranking.SalesRanking;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookService {

        private final BookRepository bookRepository;
        private final IsbnRepository isbnRepository;
        private final EditionRepository editionRepository;
        private final edu.bookpict.aladin.service.AladinBookService aladinBookService;
        private final edu.bookpict.domain.book.repository.AladinSnapshotRepository aladinSnapshotRepository;
        private final RankingService rankingService;

        public List<BookListDto> getAllBooks() {
                return bookRepository.findAll().stream()
                                .map(this::convertToListDto)
                                .collect(Collectors.toList());
        }

        public List<BookListDto> getInitialBooks() {
                return bookRepository.findLatestPerCategory().stream()
                                .map(this::convertToListDto)
                                .sorted(getStandardComparator())
                                .collect(Collectors.toList());
        }

        @Transactional
        public List<BookListDto> searchBooks(String keyword) {
                // 1. Aladin API 검색 및 DB 동기화
                try {
                        aladinBookService.searchAndSync(keyword);
                } catch (Exception e) {
                        // API 오류 발생 시에도 기존 DB 검색은 수행
                        log.error("Aladin API Search Failed: {}", e.getMessage());
                }

                // 2. DB 검색 (동기화된 최신 데이터 포함)
                return bookRepository.searchByKeyword(keyword).stream()
                                .map(this::convertToListDto)
                                .sorted(getStandardComparator())
                                .collect(Collectors.toList());
        }

        /**
         * ✅ 학년 필터 추가 + 랭킹 테이블 기반 정렬
         * - 모든 필터가 null/empty이면 빈 목록 반환
         * - 필터가 하나라도 있으면 필터 적용 결과 반환
         * - 과목+학교급 필터가 있으면 해당 카테고리 랭킹 적용
         */
        public List<BookListDto> getBooks(String subject, String schoolLevel, String grade, String semester) {
                return getBooks(subject, schoolLevel, grade, semester, null);
        }

        public List<BookListDto> getBooks(String subject, String schoolLevel, String grade, String semester, String keyword) {
                String s = (subject == null || subject.equals("null")) ? null : subject;
                String l = (schoolLevel == null || schoolLevel.equals("null")) ? null : schoolLevel;
                String g = (grade == null || grade.equals("null")) ? null : grade;
                String sem = (semester == null || semester.equals("null")) ? null : semester;
                String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();

                // 모든 필터와 검색어가 없으면 빈 목록 반환 (초기 화면 조회 안함)
                if (s == null && l == null && g == null && sem == null && kw == null) {
                        return java.util.Collections.emptyList();
                }

                List<Book> books;

                // 필터가 있으면 필터 기반 조회, 없으면 전체에서 키워드 검색
                boolean hasFilter = s != null || l != null;
                if (hasFilter) {
                        books = bookRepository.findByFilters(s, l);
                } else if (kw != null) {
                        books = bookRepository.searchByKeyword(kw);
                } else {
                        books = java.util.Collections.emptyList();
                }

                // 학년 필터 적용
                if (g != null && !g.isEmpty()) {
                        books = books.stream()
                                .filter(book -> book.getGrade() != null && book.getGrade().equals(g))
                                .collect(Collectors.toList());
                }

                // 학기 필터 적용
                if (sem != null && !sem.isEmpty()) {
                        books = books.stream()
                                .filter(book -> book.getSemester() != null && book.getSemester().equals(sem))
                                .collect(Collectors.toList());
                }

                // 키워드 필터 적용 (필터 조회 결과에서 추가 필터링)
                if (kw != null && hasFilter) {
                        final String kwLower = kw.toLowerCase();
                        books = books.stream()
                                .filter(book -> (book.getTitle() != null && book.getTitle().toLowerCase().contains(kwLower))
                                        || (book.getPublisher() != null && book.getPublisher().toLowerCase().contains(kwLower)))
                                .collect(Collectors.toList());
                }

                // DTO 변환 시 랭킹 정보 포함
                final String finalSubject = s;
                final String finalSchoolLevel = l;

                return books.stream()
                                .map(book -> convertToListDtoWithRanking(book, finalSubject, finalSchoolLevel))
                                .sorted(getRankingComparator(s, l))
                                .collect(Collectors.toList());
        }

        /**
         * ✅ 호환성을 위한 기존 메서드 유지
         */
        public List<BookListDto> getBooks(String subject, String schoolLevel) {
                return getBooks(subject, schoolLevel, null, null);
        }

        public List<BookListDto> getBooks(String subject, String schoolLevel, String grade) {
                return getBooks(subject, schoolLevel, grade, null);
        }

        /**
         * ✅ 랜덤으로 최대 10개의 도서 조회 (페이지 초기 로드, 리셋 버튼 클릭 시 사용)
         */
        public List<BookListDto> getRandomBooks() {
                return bookRepository.findRandomBooks().stream()
                                .map(this::convertToListDto)
                                .sorted(getStandardComparator())
                                .collect(Collectors.toList());
        }

        private Comparator<BookListDto> getStandardComparator() {
                return Comparator.comparing(BookListDto::getBestsellerRank,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                                .thenComparing(BookListDto::getSubject, Comparator.reverseOrder())
                                .thenComparing(BookListDto::getSchoolLevel, Comparator.reverseOrder())
                                .thenComparing(BookListDto::getTitle)
                                .thenComparing(BookListDto::getLatestEditionNumber,
                                                Comparator.nullsLast(Comparator.reverseOrder()));
        }

        /**
         * ✅ 랭킹 기반 정렬 Comparator
         * - 과목+학교급 필터가 있으면 해당 카테고리 랭킹 사용
         * - 필터가 없거나 부분적이면 종합 랭킹 사용
         */
        private Comparator<BookListDto> getRankingComparator(String subject, String schoolLevel) {
                return Comparator.comparing(BookListDto::getBestsellerRank,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                                .thenComparing(BookListDto::getTitle);
        }

        /**
         * ✅ 랭킹 정보를 포함하여 DTO 변환
         */
        private BookListDto convertToListDtoWithRanking(Book book, String subject, String schoolLevel) {
                String latestIsbn = book.getIsbns().stream()
                                .map(Isbn::getIsbn)
                                .findFirst().orElse(null);

                Integer price = null;
                java.math.BigDecimal rating = null;
                Integer reviewCount = null;
                Double bookPictIndex = null;
                Integer bestsellerRank = null;
                String aladinUrl = null;
                String stockStatus = "판매중";

                // 알라딘 스냅샷 데이터 조회
                if (latestIsbn != null) {
                    edu.bookpict.domain.book.AladinSnapshot snapshot = aladinSnapshotRepository
                                    .findByIsbn13(latestIsbn)
                                    .orElse(null);
                    if (snapshot != null) {
                            price = snapshot.getPriceSales();
                            rating = snapshot.getRating();
                            reviewCount = snapshot.getReviewCount();
                            aladinUrl = snapshot.getProductUrl();
                            // Simple BookPict Index Calc
                            if (rating != null) {
                                    double r = rating.doubleValue();
                                    double rc = reviewCount != null ? reviewCount : 0;
                                    bookPictIndex = (r * 2) + Math.log10(rc + 1);
                            }
                    }

                    // ✅ 랭킹 테이블에서 순위 조회
                    bestsellerRank = rankingService.getBestRankForIsbn(latestIsbn, subject);

                    // 랭킹 테이블에 없으면 스냅샷의 bestsellerRank 사용 (fallback)
                    if (bestsellerRank == null && snapshot != null) {
                            bestsellerRank = snapshot.getBestsellerRank();
                    }
                }

                Integer latestEditionNum = book.getIsbns().stream()
                                .flatMap(isbn -> isbn.getEditions().stream())
                                .map(Edition::getEditionNumber)
                                .max(Integer::compareTo)
                                .orElse(null);

                return BookListDto.builder()
                                .bookId(book.getBookId())
                                .title(book.getTitle())
                                .publisher(book.getPublisher())
                                .subject(book.getSubject())
                                .schoolLevel(book.getSchoolLevel())
                                .grade(book.getGrade())
                                .semester(book.getSemester())
                                .thumbnailUrl(book.getThumbnailUrl())
                                .price(price)
                                .rating(rating)
                                .reviewCount(reviewCount)
                                .bookPictIndex(bookPictIndex)
                                .bestsellerRank(bestsellerRank)
                                .aladinUrl(aladinUrl)
                                .latestEditionNumber(latestEditionNum)
                                .latestIsbn(latestIsbn)
                                .curriculum(book.getCurriculum())
                                .stockStatus(stockStatus)
                                .categoryName(book.getCategoryName())
                                .build();
        }

        public BookDetailDto getBookDetail(String bookId) {
            Book book = bookRepository.findById(bookId)
                    .orElseThrow(() -> new RuntimeException("Book not found"));

            Isbn latestIsbn = resolveLatestIsbn(book);

            if (latestIsbn != null) {
                log.info("Book latest ISBN = {}", latestIsbn.getIsbn());
            }

            AladinMetrics metrics = latestIsbn != null
                    ? extractAladinMetrics(latestIsbn.getIsbn())
                    : AladinMetrics.empty();

            List<IsbnInfoDto> isbnInfos = book.getIsbns().stream()
                    .map(isbn -> IsbnInfoDto.builder()
                            .isbn(isbn.getIsbn())
                            .publicationDate(
                                    isbn.getPublicationDate() != null
                                            ? isbn.getPublicationDate().toString()
                                            : null
                            )
                            .coverType(isbn.getCoverType())
                            .pageCount(isbn.getPageCount())
                            .editions(
                                    isbn.getEditions().stream()
                                            .map(this::convertToEditionDto)
                                            .collect(Collectors.toList())
                            )
                            .build()
                    )
                    .collect(Collectors.toList());

            return BookDetailDto.builder()
                    .bookId(book.getBookId())
                    .title(book.getTitle())
                    .publisher(book.getPublisher())
                    .subject(book.getSubject())
                    .schoolLevel(book.getSchoolLevel())
                    .grade(book.getGrade())
                    .semester(book.getSemester())
                    .curriculum(book.getCurriculum())
                    .summary(book.getSummary())
                    .toc(book.getToc())
                    .thumbnailUrl(book.getThumbnailUrl())
                    .isbns(isbnInfos)

                    // Aladin Metrics
                    .rating(metrics.getRating())
                    .reviewCount(metrics.getReviewCount())
                    .bestsellerRank(metrics.getBestsellerRank())
                    .bookPictIndex(metrics.getBookPictIndex())

                    .build();
        }

        private BookListDto convertToListDto(Book book) {
                String latestIsbn = book.getIsbns().stream()
                                .map(Isbn::getIsbn)
                                .findFirst().orElse(null);

                Integer price = null;
                java.math.BigDecimal rating = null;
                Integer reviewCount = null;
                Double bookPictIndex = null;
                Integer bestsellerRank = null;
                String aladinUrl = null;
                String stockStatus = "판매중";

                // 알라딘 데이터 조회
                if (latestIsbn != null) {
                    edu.bookpict.domain.book.AladinSnapshot snapshot = aladinSnapshotRepository
                                    .findByIsbn13(latestIsbn)
                                    .orElse(null);
                    if (snapshot != null) {
                            price = snapshot.getPriceSales();
                            rating = snapshot.getRating();
                            reviewCount = snapshot.getReviewCount();
                            bestsellerRank = snapshot.getBestsellerRank();
                            aladinUrl = snapshot.getProductUrl();
                            // Simple BookPict Index Calc
                            if (rating != null) {
                                    double r = rating.doubleValue();
                                    double rc = reviewCount != null ? reviewCount : 0;
                                    bookPictIndex = (r * 2) + Math.log10(rc + 1);
                            }
                    }
                }

                Integer latestEditionNum = book.getIsbns().stream()
                                .flatMap(isbn -> isbn.getEditions().stream())
                                .map(Edition::getEditionNumber)
                                .max(Integer::compareTo)
                                .orElse(null);

                return BookListDto.builder()
                                .bookId(book.getBookId())
                                .title(book.getTitle())
                                .publisher(book.getPublisher())
                                .subject(book.getSubject())
                                .schoolLevel(book.getSchoolLevel())
                                .grade(book.getGrade())
                                .semester(book.getSemester())
                                .thumbnailUrl(book.getThumbnailUrl())
                                .price(price)
                                .rating(rating)
                                .reviewCount(reviewCount)
                                .bookPictIndex(bookPictIndex)
                                .bestsellerRank(bestsellerRank)
                                .aladinUrl(aladinUrl)
                                .latestEditionNumber(latestEditionNum)
                                .latestIsbn(latestIsbn)
                                .curriculum(book.getCurriculum())
                                .stockStatus(stockStatus)
                                .categoryName(book.getCategoryName())
                                .build();
        }

        private EditionInfoDto convertToEditionDto(Edition edition) {
                return EditionInfoDto.builder()
                                .editionId(edition.getEditionId())
                                .editionNumber(edition.getEditionNumber())
                                .printNumber(edition.getPrintNumber())
                                .isLatest(edition.getIsLatest())
                                .prices(java.util.Collections.emptyList())
                                .build();
        }
        
        private Isbn resolveLatestIsbn(Book book) {
            if (book.getIsbns().isEmpty()) return null;

            // 1. isLatest = true 판이 있는 ISBN
            for (Isbn isbn : book.getIsbns()) {
                boolean hasLatest = isbn.getEditions().stream()
                        .anyMatch(Edition::getIsLatest);
                if (hasLatest) return isbn;
            }

            // 2. editionNumber 기준 최신 ISBN
            return book.getIsbns().stream()
                    .max(Comparator.comparingInt(isbn ->
                            isbn.getEditions().stream()
                                    .map(Edition::getEditionNumber)
                                    .max(Integer::compareTo)
                                    .orElse(0)
                    ))
                    .orElse(book.getIsbns().get(0));
        }
        
        private AladinMetrics extractAladinMetrics(String isbn13) {
        	log.info("Fetching Aladin snapshot for ISBN={}", isbn13);
        	
            return aladinSnapshotRepository.findByIsbn13(isbn13)
                    .map(snapshot -> {

                        BigDecimal rating = snapshot.getRating();
                        Integer reviews = snapshot.getReviewCount();

                        Double index = null;
                        if (rating != null) {
                            double r = rating.doubleValue();
                            double rc = reviews != null ? reviews : 0;
                            index = (r * 2) + Math.log10(rc + 1);
                        }

                        return new AladinMetrics(
                                snapshot.getPriceSales(),
                                rating,
                                reviews,
                                snapshot.getBestsellerRank(),
                                snapshot.getProductUrl(),
                                index
                        );
                    })
                    .orElse(AladinMetrics.empty());
        }
        
}
package edu.bookpict.config;

import edu.bookpict.domain.book.AladinSnapshot;
import edu.bookpict.domain.book.Book;
import edu.bookpict.domain.book.Edition;
import edu.bookpict.domain.book.Isbn;
import edu.bookpict.domain.book.repository.AladinSnapshotRepository;
import edu.bookpict.domain.book.repository.BookRepository;
import edu.bookpict.domain.book.repository.EditionRepository;
import edu.bookpict.domain.book.repository.IsbnRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 데이터 초기화 컴포넌트
 * 
 * ⚠️ 주의: 이 클래스는 DB가 비어있을 때만 실행됩니다
 * - 매 빌드마다 실행되지 않습니다
 * - bookRepository.count() == 0 일 때만 샘플 데이터 생성
 */
@Slf4j
@Component
@Order(1)  // TestDataInitializer보다 먼저 실행
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final BookRepository bookRepository;
    private final IsbnRepository isbnRepository;
    private final EditionRepository editionRepository;
    private final AladinSnapshotRepository aladinSnapshotRepository;

    @Override
    public void run(String... args) throws Exception {
        // ✅ DB에 데이터가 있으면 초기화하지 않음
        if (bookRepository.count() > 0) {
            log.info("📚 Database already has {} books. Skipping initialization.", bookRepository.count());
            return;
        }

        log.info("🚀 Starting database initialization with sample data...");
        //initializeData();
        log.info("✅ Database initialization completed. Total books: {}", bookRepository.count());
    }

	/*
	 * private void initializeData() { // 1. 개념원리 수학 상 (고등) Book math1 = createBook(
	 * "B-MATH-001", "개념원리 수학 상", "개념원리", "수학", "고등", "전학년", "2022 개정",
	 * "고등학교 수학의 기초를 다지는 필수 개념서",
	 * "https://image.aladin.co.kr/product/29636/38/cover500/k662838706_1.jpg" );
	 * 
	 * Isbn isbn1 = createIsbn("9788961335345", math1, LocalDate.of(2022, 6, 15),
	 * "paperback", 432); createEdition(isbn1.getIsbn() + "-01-01", isbn1, 1, 1,
	 * false); createEdition(isbn1.getIsbn() + "-02-01", isbn1, 2, 1, true); // 최신판
	 * 
	 * createAladinSnapshot("9788961335345", 16200, 18000, 4.8, 1250, 5,
	 * "https://www.aladin.co.kr/shop/wproduct.aspx?ItemId=296363878");
	 * 
	 * // 2. 쎈 수학 중1-1 Book math2 = createBook( "B-MATH-002", "쎈 수학 중1-1", "좋은책신사고",
	 * "수학", "중등", "1학년", "2022 개정", "모든 문제를 한 권에 담은 문제 기본서",
	 * "https://image.aladin.co.kr/product/28095/36/cover500/k222734608_2.jpg" );
	 * 
	 * Isbn isbn2 = createIsbn("9788928330123", math2, LocalDate.of(2022, 10, 1),
	 * "paperback", 320); createEdition(isbn2.getIsbn() + "-01-01", isbn2, 1, 1,
	 * true);
	 * 
	 * createAladinSnapshot("9788928330123", 14400, 16000, 4.9, 850, 12,
	 * "https://www.aladin.co.kr/shop/wproduct.aspx?ItemId=280953606");
	 * 
	 * // 3. 능률 VOCA 어원편 (영어) Book eng1 = createBook( "B-ENG-001", "능률 VOCA 어원편",
	 * "NE능률", "영어", "고등", "전학년", "2015 개정", "어원으로 쉽게 익히는 수능 영단어",
	 * "https://image.aladin.co.kr/product/26173/5/cover500/k162737604_1.jpg" );
	 * 
	 * Isbn isbn3 = createIsbn("9788959978123", eng1, LocalDate.of(2021, 1, 5),
	 * "paperback", 280); createEdition(isbn3.getIsbn() + "-01-01", isbn3, 1, 1,
	 * true);
	 * 
	 * createAladinSnapshot("9788959978123", 11700, 13000, 4.7, 5600, 3,
	 * "https://www.aladin.co.kr/shop/wproduct.aspx?ItemId=261730594"); }
	 */

    private Book createBook(String id, String title, String publisher, String subject,
                            String schoolLevel, String grade, String semester, String curriculum,
                            String summary, String thumbnailUrl) {
        Book book = Book.builder()
                .bookId(id)
                .title(title)
                .publisher(publisher)
                .subject(subject)
                .schoolLevel(schoolLevel)
                .grade(grade)
                .semester(semester)
                .curriculum(curriculum)
                .summary(summary)
                .thumbnailUrl(thumbnailUrl)
                .build();
        return bookRepository.save(book);
    }

    private Isbn createIsbn(String isbnValue, Book book, LocalDate pubDate, String cover, Integer pages) {
        Isbn isbn = Isbn.builder()
                .isbn(isbnValue)
                .book(book)
                .publicationDate(pubDate)
                .coverType(cover)
                .pageCount(pages)
                .build();
        return isbnRepository.save(isbn);
    }

    private Edition createEdition(String id, Isbn isbn, Integer edNum, Integer prNum, Boolean latest) {
        Edition edition = Edition.builder()
                .editionId(id)
                .isbn(isbn)
                .editionNumber(edNum)
                .printNumber(prNum)
                .isLatest(latest)
                .build();
        return editionRepository.save(edition);
    }

    private void createAladinSnapshot(String isbn13, Integer priceSales, Integer priceStandard,
                                      Double rating, Integer reviews, Integer rank, String url) {
        AladinSnapshot snapshot = new AladinSnapshot();
        snapshot.setIsbn13(isbn13);
        snapshot.setPriceSales(priceSales);
        snapshot.setPriceStandard(priceStandard);
        snapshot.setRating(BigDecimal.valueOf(rating));
        snapshot.setReviewCount(reviews);
        snapshot.setBestsellerRank(rank);
        snapshot.setProductUrl(url);
        snapshot.setCollectedAt(LocalDateTime.now());

        aladinSnapshotRepository.save(snapshot);
    }
}

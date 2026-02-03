package edu.bookpict.domain.book.repository;

import edu.bookpict.domain.book.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, String> {

        Optional<Book> findByTitleAndPublisher(String title, String publisher);

        List<Book> findBySubject(String subject);

        List<Book> findBySchoolLevel(String schoolLevel);

        @Query("SELECT b FROM Book b WHERE b.bookId IN (" +
                        "SELECT MAX(b2.bookId) FROM Book b2 GROUP BY b2.schoolLevel, b2.subject) " +
                        "ORDER BY b.subject DESC, b.schoolLevel DESC, b.title ASC")
        List<Book> findLatestPerCategory();

        @Query("SELECT b FROM Book b WHERE (:subject IS NULL OR b.subject = :subject) AND (:level IS NULL OR b.schoolLevel = :level) "
                        +
                        "ORDER BY b.subject DESC, b.schoolLevel DESC, b.title ASC")
        List<Book> findByFilters(@Param("subject") String subject, @Param("level") String level);

        @Query("SELECT b FROM Book b WHERE LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                        "OR LOWER(b.publisher) LIKE LOWER(CONCAT('%', :keyword, '%'))")
        List<Book> searchByKeyword(@Param("keyword") String keyword);

        /**
         * ✅ 랜덤으로 최대 10개의 도서 조회
         * ORDER BY RAND() 사용 - 데이터베이스마다 다름 (H2: RAND(), MySQL: RAND())
         */
        @Query(value = "SELECT * FROM book ORDER BY RAND() LIMIT 10", nativeQuery = true)
        List<Book> findRandomBooks();
}

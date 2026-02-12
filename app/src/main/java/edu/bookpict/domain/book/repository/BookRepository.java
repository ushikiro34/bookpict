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
         * 랜덤으로 최대 5개의 도서 조회 (Java 셔플 - DB 종류 무관)
         */
        default List<Book> findRandomBooks() {
                List<Book> all = findAll();
                java.util.Collections.shuffle(all);
                return all.stream().limit(5).collect(java.util.stream.Collectors.toList());
        }
}

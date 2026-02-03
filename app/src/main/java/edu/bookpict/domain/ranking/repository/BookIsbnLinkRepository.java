package edu.bookpict.domain.ranking.repository;

import edu.bookpict.domain.ranking.BookIsbnLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookIsbnLinkRepository extends JpaRepository<BookIsbnLink, Long> {

    /**
     * ISBN13으로 링크 조회
     */
    Optional<BookIsbnLink> findByIsbn13(String isbn13);

    /**
     * ISBN13 존재 여부 확인
     */
    boolean existsByIsbn13(String isbn13);

    /**
     * Book ID로 링크 목록 조회
     */
    List<BookIsbnLink> findByBookBookId(String bookId);

    /**
     * Book ID의 대표 ISBN 링크 조회
     */
    Optional<BookIsbnLink> findByBookBookIdAndIsPrimaryTrue(String bookId);

    /**
     * 여러 ISBN13으로 링크 조회
     */
    @Query("SELECT l FROM BookIsbnLink l WHERE l.isbn13 IN :isbn13List")
    List<BookIsbnLink> findByIsbn13In(@Param("isbn13List") List<String> isbn13List);

    /**
     * Book과 함께 조회 (N+1 방지)
     */
    @Query("SELECT l FROM BookIsbnLink l JOIN FETCH l.book WHERE l.isbn13 = :isbn13")
    Optional<BookIsbnLink> findByIsbn13WithBook(@Param("isbn13") String isbn13);
}

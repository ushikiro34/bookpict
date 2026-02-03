package edu.bookpict.domain.book.repository;

import edu.bookpict.domain.book.Isbn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IsbnRepository extends JpaRepository<Isbn, String> {
    List<Isbn> findByBook_BookId(String bookId);
}

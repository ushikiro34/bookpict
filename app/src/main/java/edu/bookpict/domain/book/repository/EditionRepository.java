package edu.bookpict.domain.book.repository;

import edu.bookpict.domain.book.Edition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EditionRepository extends JpaRepository<Edition, String> {
    List<Edition> findByIsbn_Isbn(String isbn);

    Optional<Edition> findByIsbn_IsbnAndIsLatestTrue(String isbn);
}

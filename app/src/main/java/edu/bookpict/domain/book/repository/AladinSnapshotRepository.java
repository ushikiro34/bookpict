package edu.bookpict.domain.book.repository;

import edu.bookpict.domain.book.AladinSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AladinSnapshotRepository extends JpaRepository<AladinSnapshot, Long> {
    Optional<AladinSnapshot> findByIsbn13(String isbn13);
}

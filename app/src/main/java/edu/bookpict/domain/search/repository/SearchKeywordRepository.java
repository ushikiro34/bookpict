package edu.bookpict.domain.search.repository;

import edu.bookpict.domain.search.SearchKeyword;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SearchKeywordRepository extends JpaRepository<SearchKeyword, Long> {
    Optional<SearchKeyword> findByKeyword(String keyword);
}

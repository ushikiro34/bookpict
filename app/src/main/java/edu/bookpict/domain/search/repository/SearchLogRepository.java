package edu.bookpict.domain.search.repository;

import edu.bookpict.domain.search.SearchLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SearchLogRepository extends JpaRepository<SearchLog, Long> {

    @Query("SELECT sl FROM SearchLog sl WHERE sl.sessionId = :sessionId ORDER BY sl.searchedAt DESC")
    List<SearchLog> findBySessionIdOrderBySearchedAtDesc(@Param("sessionId") String sessionId);

    @Query("SELECT sl.keyword, COUNT(sl) as count FROM SearchLog sl GROUP BY sl.keyword ORDER BY count DESC")
    List<Object[]> findPopularKeywords();

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    void deleteBySessionId(String sessionId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    void deleteBySessionIdAndKeyword(String sessionId, String keyword);
}

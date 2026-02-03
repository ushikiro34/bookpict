package edu.bookpict.domain.ranking.repository;

import edu.bookpict.domain.ranking.PopularRank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PopularRankRepository extends JpaRepository<PopularRank, Long> {
    List<PopularRank> findByYearMonthOrderByRankAsc(String yearMonth);

    @org.springframework.transaction.annotation.Transactional
    void deleteByYearMonth(String yearMonth);
}

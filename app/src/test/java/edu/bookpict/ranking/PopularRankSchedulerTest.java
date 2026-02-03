package edu.bookpict.ranking;

import edu.bookpict.ranking.scheduler.PopularRankScheduler;
import edu.bookpict.domain.ranking.repository.PopularRankRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class PopularRankSchedulerTest {

    @Autowired
    private PopularRankScheduler scheduler;

    @Autowired
    private PopularRankRepository popularRankRepository;

    @Test
    public void runComputeOnce() {
        int saved = scheduler.computeMonthlyPopularRankSync();
        System.out.println("Saved popular ranks: " + saved);
        System.out.println("Total rows in popular_rank: " + popularRankRepository.findAll().size());
        // not asserting non-zero because if no search logs exist it may legitimately be 0
    }
}

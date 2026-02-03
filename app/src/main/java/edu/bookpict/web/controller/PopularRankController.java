package edu.bookpict.web.controller;

import edu.bookpict.ranking.scheduler.PopularRankScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/rankings")
@RequiredArgsConstructor
@Slf4j
public class PopularRankController {

    private final PopularRankScheduler popularRankScheduler;

    @PostMapping("/compute")
    public ResponseEntity<String> computeNow() {
        try {
            int saved = popularRankScheduler.computeMonthlyPopularRankSync();
            return ResponseEntity.ok("Saved " + saved + " popular rank entries");
        } catch (Exception e) {
            log.error("Failed to compute popular ranks via admin endpoint: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
}

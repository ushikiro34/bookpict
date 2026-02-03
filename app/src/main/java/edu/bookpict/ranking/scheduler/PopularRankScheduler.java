package edu.bookpict.ranking.scheduler;

import edu.bookpict.domain.book.Isbn;
import edu.bookpict.domain.ranking.PopularRank;
import edu.bookpict.domain.ranking.repository.PopularRankRepository;
import edu.bookpict.domain.search.repository.SearchLogRepository;
import edu.bookpict.service.BookService;
import edu.bookpict.web.dto.BookListDto;
import edu.bookpict.domain.book.repository.IsbnRepository;
import edu.bookpict.domain.book.repository.AladinSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PopularRankScheduler {

    private final SearchLogRepository searchLogRepository;
    private final PopularRankRepository popularRankRepository;
    private final BookService bookService;
    private final IsbnRepository isbnRepository;
    private final AladinSnapshotRepository aladinSnapshotRepository;

    /**
     * 매일 05:10에 최근 검색 로그를 기반으로 월간 인기 랭킹을 갱신합니다.
     * - 간단한 구현: 인기 검색어별로 첫번째 검색 결과의 ISBN을 랭킹 항목으로 저장
     */
    @Scheduled(cron = "0 10 5 * * *", zone = "Asia/Seoul")
    @Transactional
    public void computeMonthlyPopularRank() {
        try {
            int saved = computeMonthlyPopularRankSync();
            log.info("✅ Scheduled popular rank computation finished (saved {} entries)", saved);
        } catch (org.springframework.dao.DataAccessException dae) {
            log.error("❌ DB error while computing popular ranks (scheduled): {}", dae.getMessage(), dae);
        } catch (Exception e) {
            log.error("❌ Unexpected error while computing popular ranks (scheduled): {}", e.getMessage(), e);
        }
    }

    /**
     * 동기 호출용 메서드 (테스트/수동 트리거용)
     * @return 저장된 엔트리 수
     */
    @Transactional
    public int computeMonthlyPopularRankSync() {
        String yearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        log.info("🔢 Computing popular ranks for {}", yearMonth);

        // 기존 해당 월 랭킹 삭제
        popularRankRepository.deleteByYearMonth(yearMonth);
        log.info("🧹 Cleared existing popular_rank entries for {}", yearMonth);

        // Map of ISBN -> score
        java.util.Map<String, Double> scores = new java.util.HashMap<>();
        List<Object[]> popular = searchLogRepository.findPopularKeywords();

        for (Object[] row : popular) {
            if (row == null || row.length == 0) continue;
            String keyword = (String) row[0];
            Number countNum = row.length > 1 && row[1] instanceof Number ? (Number) row[1] : 1;
            double keywordCount = countNum.doubleValue();

            try {
                // take top 3 results to increase coverage
                List<BookListDto> results = bookService.searchBooks(keyword).stream().limit(3).collect(java.util.stream.Collectors.toList());
                int pos = 1;
                for (BookListDto dto : results) {
                    String latestIsbn = dto.getLatestIsbn();
                    if (latestIsbn == null) continue;

                    double index = dto.getBookPictIndex() != null ? dto.getBookPictIndex() : 0.0;

                    // if index is 0, try reading from snapshot directly
                    if (index == 0.0) {
                        edu.bookpict.domain.book.AladinSnapshot snap = aladinSnapshotRepository.findByIsbn13(latestIsbn).orElse(null);
                        if (snap != null && snap.getRating() != null) {
                            double r = snap.getRating().doubleValue();
                            double rc = snap.getReviewCount() != null ? snap.getReviewCount() : 0;
                            index = (r * 2) + Math.log10(rc + 1);
                        }
                    }

                    // score contribution: keyword frequency * (index + position bonus)
                    double contribution = keywordCount * (index + (1.0 / pos));

                    scores.put(latestIsbn, scores.getOrDefault(latestIsbn, 0.0) + contribution);
                    pos++;
                }

            } catch (Exception e) {
                log.error("Failed while scoring keyword='{}': {}", keyword, e.getMessage(), e);
            }
        }

        // apply minimum score threshold and sort ISBNs by score desc, tie-break by most recent snapshot
        double minScore = 0.1;
        List<java.util.Map.Entry<String, Double>> ranked = scores.entrySet().stream()
                .filter(e -> e.getValue() >= minScore)
                .sorted((e1, e2) -> {
                    int cmp = Double.compare(e2.getValue(), e1.getValue());
                    if (cmp != 0) return cmp;
                    // tie-break: prefer more recent snapshot
                    edu.bookpict.domain.book.AladinSnapshot s1 = aladinSnapshotRepository.findByIsbn13(e1.getKey()).orElse(null);
                    edu.bookpict.domain.book.AladinSnapshot s2 = aladinSnapshotRepository.findByIsbn13(e2.getKey()).orElse(null);
                    if (s1 != null && s2 != null && s1.getCollectedAt() != null && s2.getCollectedAt() != null) {
                        return s2.getCollectedAt().compareTo(s1.getCollectedAt());
                    }
                    return 0;
                })
                .limit(50)
                .collect(java.util.stream.Collectors.toList());

        int rank = 1;
        int savedCount = 0;
        for (java.util.Map.Entry<String, Double> e : ranked) {
            String isbnStr = e.getKey();
            Double score = e.getValue();

            Isbn isbn = isbnRepository.findById(isbnStr).orElse(null);
            if (isbn == null) continue;

            // prefer book-level info from the most recent BookListDto if available
            BookListDto info = bookService.getAllBooks().stream()
                    .filter(b -> isbnStr.equals(b.getLatestIsbn()))
                    .findFirst().orElse(null);

            PopularRank pr = PopularRank.builder()
                    .yearMonth(yearMonth)
                    .rank(rank)
                    .isbn(isbn)
                    .bookPictIndex(info != null ? info.getBookPictIndex() : null)
                    .schoolLevel(info != null ? info.getSchoolLevel() : isbn.getBook().getSchoolLevel())
                    .subject(info != null ? info.getSubject() : isbn.getBook().getSubject())
                    .build();

            popularRankRepository.save(pr);
            savedCount++;
            rank++;
        }

        log.info("✅ Popular rank computation finished for {} (saved {} entries)", yearMonth, savedCount);
        return savedCount;
    }
}

package edu.bookpict.ranking.service;

import edu.bookpict.domain.book.AladinSnapshot;
import edu.bookpict.domain.book.Isbn;
import edu.bookpict.domain.ranking.PopularRank;
import edu.bookpict.domain.ranking.repository.PopularRankRepository;
import edu.bookpict.domain.search.repository.SearchLogRepository;
import edu.bookpict.service.BookService;
import edu.bookpict.web.dto.BookListDto;
import edu.bookpict.domain.book.repository.IsbnRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PopularRankService {

    private final SearchLogRepository searchLogRepository;
    private final PopularRankRepository popularRankRepository;
    private final BookService bookService;
    private final IsbnRepository isbnRepository;
    private final edu.bookpict.domain.book.repository.AladinSnapshotRepository aladinSnapshotRepository;

    @Transactional
    public int computeAndSave(String yearMonth, int topN) {
        log.info("? Computing popular ranks for {} (topN={})", yearMonth, topN);

        popularRankRepository.deleteByYearMonth(yearMonth);

        Map<String, Double> scores = scoreByKeywords();

        // apply threshold and sort
        double minScore = 0.1;
        List<Map.Entry<String, Double>> ranked = scores.entrySet().stream()
                .filter(e -> e.getValue() >= minScore)
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topN)
                .collect(Collectors.toList());

        int rank = 1;
        int saved = 0;
        for (Map.Entry<String, Double> e : ranked) {
            String isbnStr = e.getKey();
            Double score = e.getValue();
            Isbn isbn = isbnRepository.findById(isbnStr).orElse(null);
            if (isbn == null) continue;

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

            try {
                popularRankRepository.save(pr);
                saved++;
                rank++;
            } catch (Exception ex) {
                log.warn("Failed to save popular rank for isbn={} : {}", isbnStr, ex.getMessage());
            }
        }

        log.info("✅ Saved {} popular ranks for {}", saved, yearMonth);
        return saved;
    }

    public Map<String, Double> scoreByKeywords() {
        Map<String, Double> scores = new HashMap<>();
        List<Object[]> popular = searchLogRepository.findPopularKeywords();

        for (Object[] row : popular) {
            if (row == null || row.length == 0) continue;
            String keyword = (String) row[0];
            Number countNum = row.length > 1 && row[1] instanceof Number ? (Number) row[1] : 1;
            double keywordCount = countNum.doubleValue();

            try {
                List<BookListDto> results = bookService.searchBooks(keyword).stream().limit(3).collect(Collectors.toList());
                int pos = 1;
                for (BookListDto dto : results) {
                    String latestIsbn = dto.getLatestIsbn();
                    if (latestIsbn == null) continue;

                    double index = dto.getBookPictIndex() != null ? dto.getBookPictIndex() : 0.0;

                    // try to retrieve snapshot once and use it for both rating fallback and recency
                    AladinSnapshot snap = null;
                    try {
                        snap = aladinSnapshotRepository.findByIsbn13(latestIsbn).orElse(null);
                    } catch (Exception ex) {
                        // ignore DB issues for scoring
                        snap = null;
                    }

                    if (index == 0.0 && snap != null && snap.getRating() != null) {
                        double r = snap.getRating().doubleValue();
                        double rc = snap.getReviewCount() != null ? snap.getReviewCount() : 0;
                        index = (r * 2) + Math.log10(rc + 1);
                    }

                    // recency boost: newer snapshots get slight boost
                    double recencyBoost = 0.0;
                    if (snap != null && snap.getCollectedAt() != null) {
                        long days = java.time.Duration.between(snap.getCollectedAt(), java.time.LocalDateTime.now()).toDays();
                        recencyBoost = Math.max(0, (30 - Math.min(days, 30)) / 30.0); // 0..1
                    }

                    // require minimal signals
                    if (index <= 0 && pos > 1) { pos++; continue; }

                    double contribution = keywordCount * (index + (1.0 / pos) + recencyBoost);
                    scores.put(latestIsbn, scores.getOrDefault(latestIsbn, 0.0) + contribution);
                    pos++;
                }
            } catch (Exception ex) {
                log.error("Error scoring keyword='{}': {}", keyword, ex.getMessage());
            }
        }
        return scores;
    }
}

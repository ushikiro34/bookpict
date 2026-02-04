package edu.bookpict.web.controller;

import edu.bookpict.aladin.service.AladinBookService;
import edu.bookpict.domain.book.repository.AladinSnapshotRepository;
import edu.bookpict.domain.book.repository.BookRepository;
import edu.bookpict.domain.book.repository.EditionRepository;
import edu.bookpict.domain.book.repository.IsbnRepository;
import edu.bookpict.domain.ranking.repository.PopularRankRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/admin/books")
@RequiredArgsConstructor
public class AdminBookController {

    private final AladinBookService aladinBookService;
    private final BookRepository bookRepository;
    private final IsbnRepository isbnRepository;
    private final EditionRepository editionRepository;
    private final AladinSnapshotRepository aladinSnapshotRepository;
    private final PopularRankRepository popularRankRepository;

    @PostMapping("/reprocess-grade")
    public String reprocessGrades() {
        int updated = aladinBookService.reprocessAllBooksGradeGroup();
        return "Updated grade/semester for " + updated + " books.";
    }

    @PostMapping("/truncate-and-reimport")
    @Transactional
    public String truncateAndReimport(@RequestParam(defaultValue = "초등 수학,초등 영어,중등 수학,중등 영어,고등 수학") String queries) {
        // 1. Truncate all tables (order matters due to FK)
        log.info("Truncating all book-related tables...");
        popularRankRepository.deleteAllInBatch();
        editionRepository.deleteAllInBatch();
        aladinSnapshotRepository.deleteAllInBatch();
        isbnRepository.deleteAllInBatch();
        bookRepository.deleteAllInBatch();
        log.info("All tables truncated.");

        // 2. Re-import from Aladin
        StringBuilder result = new StringBuilder();
        result.append("Tables truncated.\n");

        String[] queryList = queries.split(",");
        for (String query : queryList) {
            String q = query.trim();
            try {
                log.info("Importing: '{}'", q);
                aladinBookService.searchAndSync(q);
                result.append("Imported: ").append(q).append("\n");
                Thread.sleep(1000);
            } catch (Exception e) {
                log.error("Failed to import '{}': {}", q, e.getMessage());
                result.append("Failed: ").append(q).append(" -> ").append(e.getMessage()).append("\n");
            }
        }

        // 3. Reprocess grades
        int updated = aladinBookService.reprocessAllBooksGradeGroup();
        result.append("Reprocessed grade/semester for ").append(updated).append(" books.\n");
        result.append("Total books in DB: ").append(bookRepository.count());

        return result.toString();
    }
}

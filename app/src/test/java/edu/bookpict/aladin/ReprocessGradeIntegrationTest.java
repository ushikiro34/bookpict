package edu.bookpict.aladin;

import edu.bookpict.aladin.service.AladinBookService;
import edu.bookpict.domain.book.repository.BookRepository;
import edu.bookpict.domain.book.Book;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import org.junit.jupiter.api.Tag;

import java.util.List;

@Tag("integration")
@SpringBootTest(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
public class ReprocessGradeIntegrationTest {

    @Autowired
    private AladinBookService aladinBookService;

    @Autowired
    private BookRepository bookRepository;

    @Test
    @Transactional
    void reprocessAndSampleBooks() throws Exception {
        // seed a couple of queries so DB has some books to analyze
        aladinBookService.importBooksFromSearch("초등 4-2 참고서");
        aladinBookService.importBooksFromSearch("중등 3-1 수학 참고서");

        int updated = aladinBookService.reprocessAllBooksGradeGroup();

        List<Book> samples = bookRepository.findRandomBooks();

        // 파일에 결과 기록
        java.nio.file.Path out = java.nio.file.Path.of("app", "build", "tmp", "reprocess-sample.txt");
        java.nio.file.Files.createDirectories(out.getParent());
        try (java.io.BufferedWriter w = java.nio.file.Files.newBufferedWriter(out)) {
            w.write("Reprocessed updated count: " + updated + "\n");
            w.write("Sample books (title | schoolLevel | grade | semester):\n");
            for (Book b : samples) {
                w.write(b.getTitle() + " | " + b.getSchoolLevel() + " | " + b.getGrade() + " | " + b.getSemester() + "\n");
            }
        }
    }

    @Test
    @Transactional
    void reprocessProducesNonDefaultGrades() {
        // seed before asserting
        try {
            aladinBookService.importBooksFromSearch("초등 4-2 참고서");
            aladinBookService.importBooksFromSearch("중등 3-1 수학 참고서");
        } catch (Exception e) {
            System.out.println("Seed error: " + e.getMessage());
        }

        aladinBookService.reprocessAllBooksGradeGroup();
        long cnt = bookRepository.findAll().stream().filter(b -> b.getGrade() != null).count();
        System.out.println("Non-default grade count: " + cnt);
        org.junit.jupiter.api.Assertions.assertTrue(cnt >= 1, "Expected at least one book with a specific grade");
    }
}
package edu.bookpict.aladin;

import edu.bookpict.aladin.service.AladinBookService;
import edu.bookpict.domain.book.repository.BookRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.Tag;
import org.springframework.transaction.annotation.Transactional;

@Tag("integration")
@SpringBootTest(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
public class SeedAndReprocessIntegrationTest {

    @Autowired
    private AladinBookService aladinBookService;

    @Autowired
    private BookRepository bookRepository;

    @Test
    @Transactional
    void seedSearchesThenReprocess() throws Exception {
        String[] queries = new String[]{
                "초등 4-2 참고서",
                "중등 3-1 수학 참고서",
                "고등학교 3-1 수학",
                "수학 5-1 참고서",
                "영어 4-2 단원별"
        };

        for (String q : queries) {
            try {
                aladinBookService.importBooksFromSearch(q);
                Thread.sleep(600); // rate-limit friendly
            } catch (Exception e) {
                System.out.println("Seed failed for: " + q + " -> " + e.getMessage());
            }
        }

        int updated = aladinBookService.reprocessAllBooksGradeGroup();
        System.out.println("Seeded queries completed. Reprocessed updated count: " + updated);

        long nonDefaultCount = bookRepository.findAll().stream()
                .filter(b -> b.getGrade() != null).count();

        System.out.println("Non-default grade count after seeding: " + nonDefaultCount);
        org.junit.jupiter.api.Assertions.assertTrue(nonDefaultCount >= 1, "Expected some books to have specific grade after seeding");
    }
}
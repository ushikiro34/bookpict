package edu.bookpict.web.controller;

import edu.bookpict.service.BookService;
import edu.bookpict.service.SearchService;
import edu.bookpict.web.dto.BookDetailDto;
import edu.bookpict.web.dto.BookListDto;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class PageController {

    private final BookService bookService;
    private final SearchService searchService;

    /**
     * ✅ 학년 필터 추가
     * - 필터 없으면: 랜덤 10개 도서 표시
     * - 필터 있으면: 필터 적용 데이터 표시
     */
    @GetMapping("/")
    public String index(
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String schoolLevel,
            @RequestParam(required = false) String grade,
            Model model,
            HttpSession session) {

        populateIndexModel(subject, schoolLevel, grade, model, session);
        return "index";
    }

    @GetMapping("/privacy")
    public String privacy(
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String schoolLevel,
            @RequestParam(required = false) String grade,
            Model model,
            HttpSession session) {

        populateIndexModel(subject, schoolLevel, grade, model, session);
        model.addAttribute("openModal", "privacy");
        return "index";
    }

    @GetMapping("/terms")
    public String terms(
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String schoolLevel,
            @RequestParam(required = false) String grade,
            Model model,
            HttpSession session) {

        populateIndexModel(subject, schoolLevel, grade, model, session);
        model.addAttribute("openModal", "terms");
        return "index";
    }

    private void populateIndexModel(String subject, String schoolLevel, String grade, Model model,
            HttpSession session) {
        // ✅ 필터가 하나라도 있으면 필터 적용, 모두 없으면 빈 목록 (초기 화면 조회 안함)
        List<BookListDto> books;
        boolean hasAnyFilter = (subject != null && !subject.isEmpty()) ||
                (schoolLevel != null && !schoolLevel.isEmpty()) ||
                (grade != null && !grade.isEmpty());

        if (!hasAnyFilter) {
            books = Collections.emptyList();
        } else {
            books = bookService.getBooks(subject, schoolLevel, grade);
        }

        List<String> recentSearches = searchService.getRecentSearches(session.getId());

        // TOP 3 판매량 순위 (bestsellerRank 기준, 낮을수록 상위)
        List<BookListDto> topBooks = books.stream()
                .filter(b -> b.getBestsellerRank() != null)
                .sorted(Comparator.comparing(BookListDto::getBestsellerRank))
                .limit(3)
                .collect(Collectors.toList());

        model.addAttribute("books", books);
        model.addAttribute("topBooks", topBooks);
        model.addAttribute("recentSearches", recentSearches);
        model.addAttribute("activeTab", "books");

        // 필터 값을 모델에 추가 (화면에서 선택 상태 유지용)
        model.addAttribute("selectedSubject", subject);
        model.addAttribute("selectedSchoolLevel", schoolLevel);
        model.addAttribute("selectedGrade", grade);
    }

    @GetMapping("/search")
    public String search(
            @RequestParam("q") String keyword,
            Model model,
            HttpSession session) {

        List<BookListDto> books = searchService.search(keyword, session.getId());
        List<String> recentSearches = searchService.getRecentSearches(session.getId());

        // TOP 3 판매량 순위
        List<BookListDto> topBooks = books.stream()
                .filter(b -> b.getBestsellerRank() != null)
                .sorted(Comparator.comparing(BookListDto::getBestsellerRank))
                .limit(3)
                .collect(Collectors.toList());

        model.addAttribute("books", books);
        model.addAttribute("topBooks", topBooks);
        model.addAttribute("recentSearches", recentSearches);
        model.addAttribute("keyword", keyword);
        model.addAttribute("activeTab", "books");

        return "index";
    }

    @GetMapping("/books/{id}")
    public String bookDetail(@PathVariable("id") String id, Model model) {
        BookDetailDto book = bookService.getBookDetail(id);
        model.addAttribute("book", book);
        return "book-detail";
    }

    @GetMapping("/faq")
    public String faq() {
        return "faq";
    }

    @GetMapping("/guide")
    public String guide() {
        return "guide/index";
    }

    @GetMapping("/guide/{slug}")
    public String guideDetail(@PathVariable("slug") String slug) {
        return "guide/" + slug;
    }
}
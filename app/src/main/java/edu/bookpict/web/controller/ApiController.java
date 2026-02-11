package edu.bookpict.web.controller;

import edu.bookpict.service.BookService;
import edu.bookpict.service.SearchService;
import edu.bookpict.web.dto.BookDetailDto;
import edu.bookpict.web.dto.BookListDto;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    private final BookService bookService;
    private final SearchService searchService;

    /**
     * GET /api/books?subject=수학&schoolLevel=초등&grade=1학년&semester=1학기
     */
    @GetMapping("/books")
    public ResponseEntity<List<BookListDto>> getBooks(
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String schoolLevel,
            @RequestParam(required = false) String grade,
            @RequestParam(required = false) String semester,
            @RequestParam(required = false) String keyword) {

        List<BookListDto> books = bookService.getBooks(subject, schoolLevel, grade, semester, keyword);
        return ResponseEntity.ok(books);
    }

    /**
     * ✅ 랜덤 10개 도서 조회 API
     * GET /api/books/random
     */
    @GetMapping("/books/random")
    public ResponseEntity<List<BookListDto>> getRandomBooks() {
        List<BookListDto> books = bookService.getRandomBooks();
        return ResponseEntity.ok(books);
    }

    @GetMapping("/books/search")
    public ResponseEntity<List<BookListDto>> searchBooks(
            @RequestParam("q") String keyword,
            HttpSession session) {

        List<BookListDto> books = searchService.search(keyword, session.getId());
        return ResponseEntity.ok(books);
    }

    @GetMapping("/books/{id}")
    public ResponseEntity<BookDetailDto> getBookDetail(@PathVariable("id") String id) {
        BookDetailDto book = bookService.getBookDetail(id);
        return ResponseEntity.ok(book);
    }

    @GetMapping("/search/recent")
    public ResponseEntity<List<String>> getRecentSearches(HttpSession session) {
        List<String> searches = searchService.getRecentSearches(session.getId());
        return ResponseEntity.ok(searches);
    }

    @DeleteMapping("/search/recent")
    public ResponseEntity<Void> clearRecentSearches(HttpSession session) {
        searchService.clearRecentSearches(session.getId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/search/recent/{keyword}")
    public ResponseEntity<Void> deleteRecentSearch(@PathVariable("keyword") String keyword, HttpSession session) {
        searchService.deleteRecentSearch(session.getId(), keyword);
        return ResponseEntity.ok().build();
    }

    /**
     * 북픽 랭킹 API (학년/학기 필터 지원)
     */
    @GetMapping("/rankings/bookpict")
    public ResponseEntity<List<BookListDto>> getBookPictRankings(
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String schoolLevel,
            @RequestParam(required = false) String grade,
            @RequestParam(required = false) String semester) {

        List<BookListDto> rankings = bookService.getBooks(subject, schoolLevel, grade, semester);
        return ResponseEntity.ok(rankings);
    }
}
package edu.bookpict.service;

import edu.bookpict.domain.search.SearchLog;
import edu.bookpict.domain.search.repository.SearchLogRepository;
import edu.bookpict.web.dto.BookListDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final SearchLogRepository searchLogRepository;
    private final BookService bookService;

    @Transactional
    public List<BookListDto> search(String keyword, String sessionId) {
        // 검색 로그 저장
        SearchLog log = SearchLog.builder()
                .keyword(keyword)
                .sessionId(sessionId)
                .build();
        searchLogRepository.save(log);

        // [변경] 실시간 크롤링 호출 제거. 수집된 DB 데이터만 사용함.
        // crawlerService.crawlAll(keyword); // 기존 코드 제거

        // 검색 실행
        return bookService.searchBooks(keyword);
    }

    @Transactional(readOnly = true)
    public List<String> getRecentSearches(String sessionId) {
        return searchLogRepository.findBySessionIdOrderBySearchedAtDesc(sessionId).stream()
                .map(SearchLog::getKeyword)
                .distinct()
                .limit(20)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteRecentSearch(String sessionId, String keyword) {
        searchLogRepository.deleteBySessionIdAndKeyword(sessionId, keyword);
    }

    @Transactional
    public void clearRecentSearches(String sessionId) {
        searchLogRepository.deleteBySessionId(sessionId);
    }
}

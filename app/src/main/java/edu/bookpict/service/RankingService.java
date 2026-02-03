package edu.bookpict.service;

import edu.bookpict.domain.book.Book;
import edu.bookpict.domain.book.repository.BookRepository;
import edu.bookpict.domain.ranking.BookIsbnLink;
import edu.bookpict.domain.ranking.SalesRanking;
import edu.bookpict.domain.ranking.repository.BookIsbnLinkRepository;
import edu.bookpict.domain.ranking.repository.SalesRankingRepository;
import edu.bookpict.web.dto.BookListDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RankingService {

    private final SalesRankingRepository salesRankingRepository;
    private final BookIsbnLinkRepository bookIsbnLinkRepository;
    private final BookRepository bookRepository;

    // ==================== 랭킹 저장 ====================

    /**
     * 검색 결과에서 랭킹 저장
     *
     * @param isbn13      ISBN13
     * @param queryKey    검색 쿼리 (예: "초등 수학 참고서")
     * @param subject     과목
     * @param schoolLevel 학교급
     * @param rank        순위 (1부터 시작)
     * @param book        연결할 Book 엔티티
     */
    @Transactional
    public void saveRanking(String isbn13, String queryKey, String subject,
                            String schoolLevel, int rank, Book book) {
        LocalDate today = LocalDate.now();

        // 1. BookIsbnLink 확인 또는 생성
        BookIsbnLink link = bookIsbnLinkRepository.findByIsbn13(isbn13)
                .orElseGet(() -> createBookIsbnLink(isbn13, book));

        // 2. 기존 랭킹 확인 (중복 방지)
        Optional<SalesRanking> existingRanking = salesRankingRepository
                .findByIsbn13AndQueryKeyAndDate(isbn13, queryKey, today);

        if (existingRanking.isPresent()) {
            // 기존 랭킹 업데이트
            SalesRanking ranking = existingRanking.get();
            if (!ranking.getRankPosition().equals(rank)) {
                log.info("📈 Rank updated: {} {} → {} (query: {})",
                        isbn13, ranking.getRankPosition(), rank, queryKey);
                ranking.setRankPosition(rank);
                ranking.setCollectedAt(LocalDateTime.now());
                salesRankingRepository.save(ranking);
            }
        } else {
            // 새 랭킹 저장
            SalesRanking ranking = SalesRanking.builder()
                    .bookIsbnLink(link)
                    .queryKey(queryKey)
                    .subject(subject)
                    .schoolLevel(schoolLevel)
                    .rankPosition(rank)
                    .subjectCode(SalesRanking.getSubjectCodeFor(subject))
                    .collectedAt(LocalDateTime.now())
                    .snapshotDate(today)
                    .build();

            salesRankingRepository.save(ranking);
            log.debug("✅ Saved ranking: {} rank={} query={}", isbn13, rank, queryKey);
        }
    }

    /**
     * BookIsbnLink 생성
     */
    @Transactional
    public BookIsbnLink createBookIsbnLink(String isbn13, Book book) {
        // 해당 Book에 다른 링크가 없으면 대표 ISBN으로 설정
        boolean isPrimary = bookIsbnLinkRepository.findByBookBookId(book.getBookId()).isEmpty();

        BookIsbnLink link = BookIsbnLink.builder()
                .book(book)
                .isbn13(isbn13)
                .isPrimary(isPrimary)
                .build();

        link = bookIsbnLinkRepository.save(link);
        log.info("🔗 Created BookIsbnLink: {} → Book({})", isbn13, book.getBookId());
        return link;
    }

    /**
     * ISBN13으로 BookIsbnLink 조회 또는 생성
     */
    @Transactional
    public BookIsbnLink getOrCreateLink(String isbn13, Book book) {
        return bookIsbnLinkRepository.findByIsbn13(isbn13)
                .orElseGet(() -> createBookIsbnLink(isbn13, book));
    }

    // ==================== 랭킹 조회 ====================

    /**
     * 과목+학교급 필터로 랭킹 조회
     */
    public List<SalesRanking> getRankingsBySubjectAndSchoolLevel(
            String subject, String schoolLevel) {
        LocalDate today = LocalDate.now();
        return salesRankingRepository.findByDateAndSubjectAndSchoolLevel(
                today, subject, schoolLevel);
    }

    /**
     * 과목 필터로 랭킹 조회
     */
    public List<SalesRanking> getRankingsBySubject(String subject) {
        LocalDate today = LocalDate.now();
        return salesRankingRepository.findByDateAndSubject(today, subject);
    }

    /**
     * 학교급 필터로 랭킹 조회
     */
    public List<SalesRanking> getRankingsBySchoolLevel(String schoolLevel) {
        LocalDate today = LocalDate.now();
        return salesRankingRepository.findByDateAndSchoolLevel(today, schoolLevel);
    }

    /**
     * 종합 랭킹 조회 (과목 필터 없을 때)
     */
    public List<SalesRanking> getOverallRankings() {
        LocalDate today = LocalDate.now();
        return salesRankingRepository.findByDateOrderByOverallRank(today);
    }

    /**
     * TOP N 랭킹 조회
     */
    public List<SalesRanking> getTopRankings(String subject, String schoolLevel, int limit) {
        LocalDate today = LocalDate.now();
        return salesRankingRepository.findTopNBySubjectAndSchoolLevel(
                today, subject, schoolLevel, limit);
    }

    /**
     * 특정 ISBN의 오늘 랭킹 조회
     */
    public List<SalesRanking> getRankingsForIsbn(String isbn13) {
        LocalDate today = LocalDate.now();
        return salesRankingRepository.findByIsbn13AndDate(isbn13, today);
    }

    /**
     * 특정 ISBN의 과목별 랭킹 조회 (최소값 반환)
     */
    public Integer getBestRankForIsbn(String isbn13, String subject) {
        List<SalesRanking> rankings = getRankingsForIsbn(isbn13);
        return rankings.stream()
                .filter(r -> subject == null || subject.equals(r.getSubject()))
                .map(SalesRanking::getRankPosition)
                .min(Integer::compareTo)
                .orElse(null);
    }

    // ==================== 관리 ====================

    /**
     * 오래된 랭킹 데이터 정리 (기본 30일)
     */
    @Transactional
    public int cleanupOldRankings(int daysToKeep) {
        LocalDate cutoff = LocalDate.now().minusDays(daysToKeep);
        int deleted = salesRankingRepository.deleteOlderThan(cutoff);
        log.info("🗑️ Cleaned up {} old ranking records (before {})", deleted, cutoff);
        return deleted;
    }

    /**
     * 특정 날짜의 랭킹 통계
     */
    public long countRankingsForDate(LocalDate date) {
        return salesRankingRepository.countBySnapshotDate(date);
    }

    /**
     * 오늘 랭킹 수집 여부 확인
     */
    public boolean hasRankingsForToday(String queryKey) {
        return salesRankingRepository.existsByQueryKeyAndSnapshotDate(
                queryKey, LocalDate.now());
    }
}

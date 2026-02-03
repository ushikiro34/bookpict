package edu.bookpict.domain.ranking.repository;

import edu.bookpict.domain.ranking.SalesRanking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SalesRankingRepository extends JpaRepository<SalesRanking, Long> {

    // ==================== 기본 조회 ====================

    /**
     * 특정 날짜의 과목별 랭킹 조회 (과목 필터 있을 때)
     */
    @Query("SELECT r FROM SalesRanking r " +
           "JOIN FETCH r.bookIsbnLink l " +
           "JOIN FETCH l.book b " +
           "WHERE r.snapshotDate = :date " +
           "AND r.subject = :subject " +
           "ORDER BY r.rankPosition ASC")
    List<SalesRanking> findByDateAndSubject(
            @Param("date") LocalDate date,
            @Param("subject") String subject);

    /**
     * 특정 날짜의 과목+학교급별 랭킹 조회
     */
    @Query("SELECT r FROM SalesRanking r " +
           "JOIN FETCH r.bookIsbnLink l " +
           "JOIN FETCH l.book b " +
           "WHERE r.snapshotDate = :date " +
           "AND r.subject = :subject " +
           "AND r.schoolLevel = :schoolLevel " +
           "ORDER BY r.rankPosition ASC")
    List<SalesRanking> findByDateAndSubjectAndSchoolLevel(
            @Param("date") LocalDate date,
            @Param("subject") String subject,
            @Param("schoolLevel") String schoolLevel);

    /**
     * 특정 날짜의 학교급별 랭킹 조회
     */
    @Query("SELECT r FROM SalesRanking r " +
           "JOIN FETCH r.bookIsbnLink l " +
           "JOIN FETCH l.book b " +
           "WHERE r.snapshotDate = :date " +
           "AND r.schoolLevel = :schoolLevel " +
           "ORDER BY r.rankPosition ASC")
    List<SalesRanking> findByDateAndSchoolLevel(
            @Param("date") LocalDate date,
            @Param("schoolLevel") String schoolLevel);

    /**
     * 특정 날짜의 전체 랭킹 조회 (종합 순위 계산용)
     * 과목 코드를 활용한 종합 랭킹 정렬
     */
    @Query("SELECT r FROM SalesRanking r " +
           "JOIN FETCH r.bookIsbnLink l " +
           "JOIN FETCH l.book b " +
           "WHERE r.snapshotDate = :date " +
           "ORDER BY (r.rankPosition + r.subjectCode * 1000) ASC")
    List<SalesRanking> findByDateOrderByOverallRank(@Param("date") LocalDate date);

    // ==================== 쿼리별 조회 ====================

    /**
     * 특정 쿼리의 당일 랭킹 조회
     */
    List<SalesRanking> findByQueryKeyAndSnapshotDateOrderByRankPositionAsc(
            String queryKey, LocalDate snapshotDate);

    /**
     * 특정 ISBN의 랭킹 조회
     */
    @Query("SELECT r FROM SalesRanking r " +
           "WHERE r.bookIsbnLink.isbn13 = :isbn13 " +
           "AND r.snapshotDate = :date")
    List<SalesRanking> findByIsbn13AndDate(
            @Param("isbn13") String isbn13,
            @Param("date") LocalDate date);

    /**
     * 특정 ISBN의 특정 쿼리 랭킹 조회
     */
    @Query("SELECT r FROM SalesRanking r " +
           "WHERE r.bookIsbnLink.isbn13 = :isbn13 " +
           "AND r.queryKey = :queryKey " +
           "AND r.snapshotDate = :date")
    Optional<SalesRanking> findByIsbn13AndQueryKeyAndDate(
            @Param("isbn13") String isbn13,
            @Param("queryKey") String queryKey,
            @Param("date") LocalDate date);

    // ==================== TOP N 조회 ====================

    /**
     * 과목별 TOP N 조회
     */
    @Query("SELECT r FROM SalesRanking r " +
           "JOIN FETCH r.bookIsbnLink l " +
           "JOIN FETCH l.book b " +
           "WHERE r.snapshotDate = :date " +
           "AND r.subject = :subject " +
           "AND r.schoolLevel = :schoolLevel " +
           "ORDER BY r.rankPosition ASC " +
           "LIMIT :limit")
    List<SalesRanking> findTopNBySubjectAndSchoolLevel(
            @Param("date") LocalDate date,
            @Param("subject") String subject,
            @Param("schoolLevel") String schoolLevel,
            @Param("limit") int limit);

    /**
     * 종합 TOP N 조회
     */
    @Query(value = "SELECT r.* FROM sales_ranking r " +
                   "WHERE r.snapshot_date = :date " +
                   "ORDER BY (r.rank_position + r.subject_code * 1000) ASC " +
                   "LIMIT :limit", nativeQuery = true)
    List<SalesRanking> findTopNOverall(
            @Param("date") LocalDate date,
            @Param("limit") int limit);

    // ==================== 존재 여부/삭제 ====================

    /**
     * 특정 날짜의 쿼리별 랭킹 존재 여부
     */
    boolean existsByQueryKeyAndSnapshotDate(String queryKey, LocalDate snapshotDate);

    /**
     * 특정 날짜의 모든 랭킹 삭제
     */
    @Modifying
    @Transactional
    void deleteBySnapshotDate(LocalDate snapshotDate);

    /**
     * 특정 날짜+쿼리의 랭킹 삭제
     */
    @Modifying
    @Transactional
    void deleteByQueryKeyAndSnapshotDate(String queryKey, LocalDate snapshotDate);

    /**
     * 오래된 랭킹 데이터 삭제 (N일 이전)
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM SalesRanking r WHERE r.snapshotDate < :cutoffDate")
    int deleteOlderThan(@Param("cutoffDate") LocalDate cutoffDate);

    // ==================== 통계 ====================

    /**
     * 특정 날짜의 랭킹 개수
     */
    long countBySnapshotDate(LocalDate snapshotDate);

    /**
     * 특정 쿼리의 랭킹 개수
     */
    long countByQueryKeyAndSnapshotDate(String queryKey, LocalDate snapshotDate);
}

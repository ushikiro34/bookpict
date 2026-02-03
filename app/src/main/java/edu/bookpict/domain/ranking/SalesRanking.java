package edu.bookpict.domain.ranking;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 판매 랭킹 테이블
 * - 검색 쿼리별 판매 순위 저장
 * - 일별 스냅샷으로 관리
 */
@Entity
@Table(name = "sales_ranking",
       uniqueConstraints = @UniqueConstraint(
           name = "uk_link_query_date",
           columnNames = {"link_id", "query_key", "snapshot_date"}
       ),
       indexes = {
           @Index(name = "idx_ranking_date", columnList = "snapshot_date"),
           @Index(name = "idx_ranking_subject_level", columnList = "subject, school_level"),
           @Index(name = "idx_ranking_query_date", columnList = "query_key, snapshot_date"),
           @Index(name = "idx_ranking_position", columnList = "rank_position")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesRanking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "link_id", nullable = false)
    private BookIsbnLink bookIsbnLink;

    /**
     * 검색 쿼리 키 (예: "초등 수학 참고서", "중등 영어 참고서")
     */
    @Column(name = "query_key", nullable = false, length = 100)
    private String queryKey;

    /**
     * 과목 (수학, 영어, 국어, 한국사, 세계사, 과학, 사회, 기타)
     */
    @Column(name = "subject", length = 20)
    private String subject;

    /**
     * 학교급 (초등, 중등, 고등, 기타)
     */
    @Column(name = "school_level", length = 20)
    private String schoolLevel;

    /**
     * 해당 쿼리 내 판매 순위 (1~200)
     * 알라딘 API Sort=SalesPoint 결과 순서
     */
    @Column(name = "rank_position", nullable = false)
    private Integer rankPosition;

    /**
     * 과목 코드 (종합 랭킹 계산용)
     * 수학=1, 영어=2, 국어=3, 한국사=4, 세계사=5, 과학=6, 사회=7, 기타=9
     */
    @Column(name = "subject_code")
    private Integer subjectCode;

    /**
     * 수집 시점
     */
    @Column(name = "collected_at", nullable = false)
    private LocalDateTime collectedAt;

    /**
     * 스냅샷 날짜 (YYYY-MM-DD)
     * 일별 랭킹 비교/히스토리 추적용
     */
    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    /**
     * 종합 랭킹 점수 계산
     * 과목별 필터가 없을 때 사용
     *
     * @return rankPosition + (subjectCode * 1000)
     */
    @Transient
    public Integer getOverallRankScore() {
        int sc = subjectCode != null ? subjectCode : 9;
        return rankPosition + (sc * 1000);
    }

    /**
     * 과목명으로 과목 코드 설정
     */
    public void setSubjectWithCode(String subject) {
        this.subject = subject;
        this.subjectCode = getSubjectCodeFor(subject);
    }

    /**
     * 과목별 코드 매핑
     */
    public static Integer getSubjectCodeFor(String subject) {
        if (subject == null) return 9;
        return switch (subject) {
            case "수학" -> 1;
            case "영어" -> 2;
            case "국어" -> 3;
            case "한국사" -> 4;
            case "세계사" -> 5;
            case "과학" -> 6;
            case "사회" -> 7;
            default -> 9;
        };
    }

    @PrePersist
    protected void onCreate() {
        if (collectedAt == null) {
            collectedAt = LocalDateTime.now();
        }
        if (snapshotDate == null) {
            snapshotDate = LocalDate.now();
        }
        if (subjectCode == null && subject != null) {
            subjectCode = getSubjectCodeFor(subject);
        }
    }
}

package edu.bookpict.aladin.scheduler;

import edu.bookpict.aladin.service.AladinBookService;
import edu.bookpict.aladin.service.AladinBookService.SyncResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 일회용 초등 데이터 전체 수집 스케줄러
 *
 * 실행 일정 (2026-02-03):
 * - 18:50 국어
 * - 19:40 영어
 * - 20:40 수학
 * - 21:40 한국사
 * - 22:40 세계사
 *
 * 페이지 제한 없이 검색되는 모든 도서 수집
 * 완료 후 이 클래스 삭제 필요
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OneTimeElementarySyncScheduler {

    private final AladinBookService aladinBookService;

    /**
     * 18:50 - 초등 국어
     */
    @Scheduled(cron = "0 50 18 3 2 *", zone = "Asia/Seoul")
    public void syncElementaryKorean() {
        runFullSync("초등", "국어");
    }

    /**
     * 19:40 - 초등 영어
     */
    @Scheduled(cron = "0 40 19 3 2 *", zone = "Asia/Seoul")
    public void syncElementaryEnglish() {
        runFullSync("초등", "영어");
    }

    /**
     * 20:40 - 초등 수학
     */
    @Scheduled(cron = "0 40 20 3 2 *", zone = "Asia/Seoul")
    public void syncElementaryMath() {
        runFullSync("초등", "수학");
    }

    // ❌ API 제한으로 비활성화 (2026-02-03)
    // /**
    //  * 21:40 - 초등 한국사
    //  */
    // @Scheduled(cron = "0 40 21 3 2 *", zone = "Asia/Seoul")
    // public void syncElementaryKoreanHistory() {
    //     runFullSync("초등", "한국사");
    // }

    // /**
    //  * 22:40 - 초등 세계사
    //  */
    // @Scheduled(cron = "0 40 22 3 2 *", zone = "Asia/Seoul")
    // public void syncElementaryWorldHistory() {
    //     runFullSync("초등", "세계사");
    // }

    /**
     * 수동 트리거용 메서드
     */
    public void manualFullSync(String schoolLevel, String subject) {
        runFullSync(schoolLevel, subject);
    }

    /**
     * ✅ 페이지 제한 없이 전체 데이터 수집 (다중 쿼리)
     * - 기본 쿼리 + 학년별 쿼리로 검색 범위 확대
     * - 중복 ISBN은 세션 내에서 자동 스킵
     */
    private void runFullSync(String schoolLevel, String subject) {
        // ✅ 다중 쿼리 생성 (검색 커버리지 확대)
        List<String> queries = buildSearchQueries(schoolLevel, subject);

        log.info("====================================");
        log.info("🚀 [ONE-TIME] Full Sync Started: {} {} ({} queries)", schoolLevel, subject, queries.size());
        log.info("====================================");

        // ✅ 세션 전체에서 공유할 ISBN Set
        Set<String> sessionProcessedIsbns = new HashSet<>();
        int totalNewIsbns = 0;
        int totalDuplicates = 0;

        for (String query : queries) {
            try {
                log.info("📌 Query: '{}'", query);
                int[] result = searchAndSyncAllPagesWithSession(query, sessionProcessedIsbns);
                totalNewIsbns += result[0];
                totalDuplicates += result[1];

                // 쿼리 간 딜레이
                Thread.sleep(2000);
            } catch (Exception e) {
                log.error("❌ [ONE-TIME] Sync failed for query '{}': {}", query, e.getMessage(), e);
            }
        }

        log.info("====================================");
        log.info("✅ [ONE-TIME] Full Sync Completed: {} {}", schoolLevel, subject);
        log.info("   📊 Total: {} new ISBNs, {} duplicates, {} unique tracked",
                totalNewIsbns, totalDuplicates, sessionProcessedIsbns.size());
        log.info("====================================");
    }

    /**
     * ✅ 학교급/과목에 따른 다중 검색 쿼리 생성
     */
    private List<String> buildSearchQueries(String schoolLevel, String subject) {
        List<String> queries = new ArrayList<>();

        // 1. 기본 쿼리 (참고서 제외)
        queries.add(schoolLevel + " " + subject);

        // 2. 학년별 쿼리 추가
        String[] grades;
        if ("초등".equals(schoolLevel)) {
            grades = new String[]{"1학년", "2학년", "3학년", "4학년", "5학년", "6학년"};
        } else if ("중등".equals(schoolLevel)) {
            grades = new String[]{"1학년", "2학년", "3학년"};
        } else if ("고등".equals(schoolLevel)) {
            grades = new String[]{"1학년", "2학년", "3학년"};
        } else {
            grades = new String[]{};
        }

        for (String grade : grades) {
            queries.add(schoolLevel + " " + subject + " " + grade);
        }

        return queries;
    }

    /**
     * ✅ 페이지 제한 없이 모든 페이지 수집 (세션 공유 ISBN Set 사용)
     * - 다중 쿼리 간 중복 ISBN 건너뛰기
     * - 중복률 80% 이상 3페이지 연속 시 조기 중단
     * - 알라딘 API 최대 200페이지(10,000건) 제한
     *
     * @return int[]{newIsbns, duplicates}
     */
    private int[] searchAndSyncAllPagesWithSession(String query, Set<String> sessionProcessedIsbns) {
        int pageSize = 50;
        int maxPages = 200; // 알라딘 API 최대 제한
        int totalNewIsbns = 0;
        int totalDuplicates = 0;
        int emptyPages = 0;
        int highDupPages = 0; // 중복률 높은 페이지 연속 카운트
        double dupThreshold = 0.8; // 80% 중복률 임계값

        for (int page = 0; page < maxPages; page++) {
            try {
                int startIndex = page * pageSize + 1;

                log.info("  📄 Page {} (start={}, tracked ISBNs={})", page + 1, startIndex, sessionProcessedIsbns.size());

                // ✅ 세션 공유 ISBN Set으로 중복 체크
                SyncResult result = aladinBookService.searchAndSyncWithDedup(query, startIndex, pageSize, sessionProcessedIsbns);

                totalNewIsbns += result.getNewIsbns();
                totalDuplicates += result.getDuplicateIsbns();

                // 결과가 없으면 중단 (연속 2페이지 빈 결과)
                if (result.getTotalProcessed() == 0) {
                    emptyPages++;
                    if (emptyPages >= 2) {
                        log.info("  ⏹️ No more results. Stopping.");
                        break;
                    }
                } else {
                    emptyPages = 0;
                }

                // ✅ 중복률 체크 - 80% 이상이 중복이면 카운트 증가
                if (result.getDuplicateRate() >= dupThreshold) {
                    highDupPages++;
                    log.warn("  ⚠️ High duplicate rate: {}% ({} consecutive)",
                            (int)(result.getDuplicateRate() * 100), highDupPages);

                    // 연속 3페이지 이상 높은 중복률이면 조기 중단
                    if (highDupPages >= 3) {
                        log.info("  ⏹️ Stopping early: 3+ consecutive pages with >80% duplicates");
                        break;
                    }
                } else {
                    highDupPages = 0; // 리셋
                }

                // API Rate Limit 방지 (1.5초 딜레이)
                Thread.sleep(1500);

            } catch (Exception e) {
                log.error("  ❌ Page {} failed: {}", page + 1, e.getMessage());
                // 오류 발생해도 계속 진행
                try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
            }
        }

        log.info("  📊 Query result: {} new ISBNs, {} duplicates skipped", totalNewIsbns, totalDuplicates);
        return new int[]{totalNewIsbns, totalDuplicates};
    }
}

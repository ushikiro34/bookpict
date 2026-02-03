package edu.bookpict.aladin.scheduler;

import edu.bookpict.aladin.service.AladinBookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 알라딘 데이터 동기화 스케줄러
 *
 * 검색 조건:
 * - 초등: 수학, 영어, 국어, 한국사, 세계사 (5개)
 * - 중등: 수학, 영어, 국어, 한국사, 세계사 (5개)
 * - 고등: 수학, 영어, 국어, 한국사, 세계사 (5개)
 * - 총 15개 검색어, 쿼리당 200건(50건x4페이지)
 * - 매일 05:00, 12:00 동기화 + 08:00, 11:00 재처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AladinSyncScheduler {

    private final AladinBookService aladinBookService;

    /**
     * 매일 새벽 05:00 KST에 학년별/과목별 참고서 데이터 동기화
     * 
     * 검색 쿼리 형식: "{학년} {과목} 참고서"
     * 예) "초등 수학 참고서", "중등 영어 참고서", "고등 과학 참고서"
     */
    @Scheduled(cron = "0 0 5 * * *", zone = "Asia/Seoul")
    public void dailySyncMorning() {
        runDailySync("Morning");
    }

    @Scheduled(cron = "0 0 12 * * *", zone = "Asia/Seoul")
    public void dailySyncNoon() {
        runDailySync("Noon");
    }

    private void runDailySync(String label) {
        log.info("====================================");
        log.info("🚀 Daily Aladin Reference Book Sync Started ({})", label);
        log.info("====================================");

        int totalQueries = 0;

        // 초등: 수학, 영어, 국어, 한국사, 세계사
        totalQueries += syncSchoolLevelSubjects("초등", new String[]{"수학", "영어", "국어", "한국사", "세계사"});

        // 중등: 수학, 영어, 국어, 한국사, 세계사
        totalQueries += syncSchoolLevelSubjects("중등", new String[]{"수학", "영어", "국어", "한국사", "세계사"});

        // 고등: 수학, 영어, 국어, 한국사, 세계사
        totalQueries += syncSchoolLevelSubjects("고등", new String[]{"수학", "영어", "국어", "한국사", "세계사"});

        log.info("====================================");
        log.info("✅ Daily Sync Completed ({}): {} total queries processed", label, totalQueries);
        log.info("====================================");
    }

    /**
     * 특정 학년의 과목별 참고서 동기화
     * 
     * @param schoolLevel 학년 (초등, 중등, 고등)
     * @param subjects 과목 목록
     * @return 처리된 쿼리 개수
     */
    private int syncSchoolLevelSubjects(String schoolLevel, String[] subjects) {
        log.info("📚 Processing {} ({} subjects)...", schoolLevel, subjects.length);
        
        int count = 0;
        for (String subject : subjects) {
            try {
                // 검색 쿼리: "초등 수학 참고서" 형식
                String query = schoolLevel + " " + subject + " 참고서";
                log.info("  ➡️  Query: '{}'", query);
                
                aladinBookService.searchAndSync(query);
                count++;
                
                // API Rate Limit 방지 (1초 딜레이)
                Thread.sleep(1000);
                
            } catch (Exception e) {
                log.error("  ❌ Failed to sync {} {}: {}", schoolLevel, subject, e.getMessage());
            }
        }
        
        log.info("  ✅ {} done: {} queries executed", schoolLevel, count);
        return count;
    }

    /**
     * 재시도 스케줄러 (08:00 KST, 11:00 KST)
     * 
     * SearchKeyword 테이블에 저장된 키워드들을 재처리
     * (이전에 새로운 도서가 발견되어 나중에 재검색이 필요한 경우)
     */
    @Scheduled(cron = "0 0 8,11 * * *", zone = "Asia/Seoul")
    public void retrySync() {
        log.info("====================================");
        log.info("🔄 Retry Sync Started (Saved Keywords)");
        log.info("====================================");

        try {
            // SearchKeyword 테이블에 저장된 키워드들 재처리
            aladinBookService.reprocessSavedKeywords();
            
        } catch (Exception e) {
            log.error("❌ Retry sync failed: {}", e.getMessage(), e);
        }

        log.info("====================================");
        log.info("✅ Retry Sync Completed");
        log.info("====================================");
    }

    /**
     * 수동 동기화 트리거 (테스트용)
     * 
     * 사용법:
     * POST /test/aladin/trigger-sync
     */
    public void manualSync() {
        log.info("Manual sync triggered");
        runDailySync("Manual");
    }
}
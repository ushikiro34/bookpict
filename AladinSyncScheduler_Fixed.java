package edu.bookpict.aladin.scheduler;

import edu.bookpict.aladin.service.AladinBookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 알라딘 데이터 동기화 스케줄러
 * 
 * ✅ 수정된 로직:
 * - 베스트셀러가 아닌 학년별/과목별 참고서 데이터 수집
 * - 초등, 중등, 고등 각 학년의 주요 과목별로 검색
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AladinSyncScheduler {

    private final AladinBookService aladinBookService;

    /**
     * 매일 새벽 05:00 KST에 학년별/과목별 참고서 데이터 동기화
     * 
     * 검색 전략:
     * 1. 초등: 수학, 영어, 국어
     * 2. 중등: 수학, 영어, 국어, 과학, 사회
     * 3. 고등: 수학, 영어, 국어, 과학, 사회
     */
    @Scheduled(cron = "0 0 5 * * *", zone = "Asia/Seoul")
    public void dailySync() {
        log.info("====================================");
        log.info("🚀 Starting Daily Reference Book Sync");
        log.info("====================================");

        int totalSynced = 0;

        // 초등 참고서
        totalSynced += syncSchoolLevelSubjects("초등", new String[]{"수학", "영어", "국어"});

        // 중등 참고서
        totalSynced += syncSchoolLevelSubjects("중등", new String[]{"수학", "영어", "국어", "과학", "사회"});

        // 고등 참고서
        totalSynced += syncSchoolLevelSubjects("고등", new String[]{"수학", "영어", "국어", "과학", "사회"});

        log.info("====================================");
        log.info("✅ Daily Sync Completed: {} queries processed", totalSynced);
        log.info("====================================");
    }

    /**
     * 특정 학년의 과목별 참고서 동기화
     */
    private int syncSchoolLevelSubjects(String schoolLevel, String[] subjects) {
        log.info("📚 Syncing {} books...", schoolLevel);
        
        int count = 0;
        for (String subject : subjects) {
            try {
                String query = schoolLevel + " " + subject + " 참고서";
                log.info("  ➡️  Searching: {}", query);
                
                aladinBookService.searchAndSync(query);
                count++;
                
                // API 요청 간 딜레이 (Rate Limit 방지)
                Thread.sleep(1000);
                
            } catch (Exception e) {
                log.error("  ❌ Failed to sync {} {}: {}", schoolLevel, subject, e.getMessage());
            }
        }
        
        log.info("  ✅ {} completed: {} subjects processed", schoolLevel, count);
        return count;
    }

    /**
     * 재시도 스케줄러 (08:00 KST, 11:00 KST)
     * 
     * SearchKeyword 테이블에 저장된 키워드들을 재처리
     * - 이전에 새로운 도서가 발견되어 저장된 검색어들
     */
    @Scheduled(cron = "0 0 8,11 * * *", zone = "Asia/Seoul")
    public void retrySync() {
        log.info("====================================");
        log.info("🔄 Starting Retry Sync");
        log.info("====================================");

        try {
            // SearchKeyword에 저장된 키워드들 재처리
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
        log.info("🔧 Manual sync triggered");
        dailySync();
    }
}

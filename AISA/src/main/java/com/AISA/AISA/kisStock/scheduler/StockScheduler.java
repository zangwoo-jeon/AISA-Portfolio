package com.AISA.AISA.kisStock.scheduler;

import com.AISA.AISA.kisStock.kisService.KisStockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockScheduler {

    private final KisStockService kisStockService;

    // 매일 오후 4시 30분(16:30)에 실행 (장 마감 후)
    @Scheduled(cron = "0 30 16 * * *")
    public void scheduleDailyIndexFetch() {
        log.info("Starting daily index data fetch...");

        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        try {
            // KOSPI
            log.info("Fetching daily KOSPI data for {}", today);
            kisStockService.saveIndexDailyData("KOSPI", today, "D");

            // API 호출 제한 고려 (잠시 대기)
            Thread.sleep(1000);

            // KOSDAQ
            log.info("Fetching daily KOSDAQ data for {}", today);
            kisStockService.saveIndexDailyData("KOSDAQ", today, "D");

            log.info("Daily index data fetch completed successfully.");
        } catch (Exception e) {
            log.error("Failed to fetch daily index data: {}", e.getMessage());
        }
    }
}

package com.AISA.AISA.kisStock.kisController;

import com.AISA.AISA.global.response.SuccessResponse;
import com.AISA.AISA.kisStock.dto.Index.IndexChartResponseDto;
import com.AISA.AISA.kisStock.dto.StockPrice.StockPriceDto;
import com.AISA.AISA.kisStock.kisService.KisStockService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
public class KisStockController {
    private final KisStockService kisStockService;

    @GetMapping("/{stockCode}/price")
    @Operation(summary = "주식 현재가 조회", description = "특정 주식의 현재가를 조회합니다.")
    public ResponseEntity<SuccessResponse<StockPriceDto>> getStockPrice(@PathVariable String stockCode) {
        StockPriceDto stockPriceDto = kisStockService.getStockPrice(stockCode);
        return ResponseEntity.ok(new SuccessResponse<>(true, "주식 현재가 조회 성공", stockPriceDto));
    }

    @GetMapping("/indices/{marketCode}/chart")
    @Operation(summary = "지수 조회", description = "코스피(kospi) / 코스닥(kosdaq)의 날짜별 정보를 조회합니다.")
    public ResponseEntity<SuccessResponse<IndexChartResponseDto>> getIndexChart(
            @PathVariable String marketCode,
            @RequestParam String date,
            @RequestParam String dateType) {
        IndexChartResponseDto chartData = kisStockService.getIndexChart(marketCode, date, dateType);
        return ResponseEntity.ok(new SuccessResponse<>(true, "기간별 지수 정보 조회 성공", chartData));
    }

    @PostMapping("/indices/{marketCode}/save")
    @Operation(summary = "지수 데이터 저장", description = "특정 날짜의 지수 데이터를 조회하여 DB에 저장합니다.")
    public ResponseEntity<SuccessResponse<Void>> saveIndexDailyData(
            @PathVariable String marketCode,
            @RequestParam String date,
            @RequestParam String dateType) {
        kisStockService.saveIndexDailyData(marketCode, date, dateType);
        return ResponseEntity.ok(new SuccessResponse<>(true, "지수 데이터 저장 성공", null));
    }

    @PostMapping("/indices/{marketCode}/init-history")
    @Operation(summary = "초기 데이터 구축", description = "현재부터 특정 과거 시점까지의 데이터를 반복적으로 수집하여 DB에 저장합니다.")
    public ResponseEntity<SuccessResponse<Void>> initHistoricalData(
            @PathVariable String marketCode,
            @RequestParam String startDate) {
        kisStockService.fetchAndSaveHistoricalData(marketCode, startDate);
        return ResponseEntity.ok(new SuccessResponse<>(true, "초기 데이터 구축 시작", null));
    }
}

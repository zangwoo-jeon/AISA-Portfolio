package com.AISA.AISA.kisStock.kisController;

import com.AISA.AISA.global.response.SuccessResponse;
import com.AISA.AISA.kisStock.dto.StockPrice.StockChartResponseDto;
import com.AISA.AISA.kisStock.dto.StockPrice.StockPriceDto;
import com.AISA.AISA.kisStock.kisService.KisStockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
@Tag(name = "주식 API", description = "주식 관련 API")
public class KisStockController {
    private final KisStockService kisStockService;

    @GetMapping("/{stockCode}/price")
    @Operation(summary = "주식 현재가 조회", description = "특정 주식의 현재 가격 정보를 조회합니다.")
    public ResponseEntity<SuccessResponse<StockPriceDto>> getStockPrice(@PathVariable String stockCode) {
        StockPriceDto stockPrice = kisStockService.getStockPrice(stockCode);
        return ResponseEntity.ok(new SuccessResponse<>(true, "주식 현재가 조회 성공", stockPrice));
    }

    @GetMapping("/{stockCode}/chart")
    @Operation(summary = "주식 차트 데이터 조회", description = "특정 주식의 차트 데이터를 조회합니다.")
    public ResponseEntity<SuccessResponse<StockChartResponseDto>> getStockChart(
            @PathVariable String stockCode,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(defaultValue = "D") String dateType) {
        StockChartResponseDto chartData = kisStockService.getStockChart(stockCode, startDate, endDate, dateType);
        return ResponseEntity.ok(new SuccessResponse<>(true, "주식 차트 데이터 조회 성공", chartData));
    }

    @PostMapping("/{stockCode}/init-history")
    @Operation(summary = "초기 주식 데이터 구축", description = "현재부터 특정 과거 시점까지의 데이터를 반복적으로 수집하여 DB에 저장합니다.")
    public ResponseEntity<SuccessResponse<Void>> initHistoricalData(
            @PathVariable String stockCode,
            @RequestParam String startDate) {
        kisStockService.fetchAndSaveHistoricalStockData(stockCode, startDate);
        return ResponseEntity.ok(new SuccessResponse<>(true, "초기 데이터 구축 시작", null));
    }

    @PostMapping("/init-history/all")
    @Operation(summary = "전체 주식 초기 데이터 구축", description = "모든 주식에 대해 현재부터 특정 과거 시점까지의 데이터를 반복적으로 수집하여 DB에 저장합니다. (비동기 실행 권장)")
    public ResponseEntity<SuccessResponse<String>> initHistoricalDataAll(@RequestParam String startDate) {
        new Thread(() -> kisStockService.fetchAllStocksHistoricalData(startDate)).start();
        return ResponseEntity
                .ok(new SuccessResponse<>(true, "전체 주식 초기 데이터 구축 시작 (백그라운드 실행)", "Started background task"));
    }

}

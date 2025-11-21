package com.AISA.AISA.kisStock.kisController;

import com.AISA.AISA.global.response.SuccessResponse;
import com.AISA.AISA.kisStock.dto.Index.IndexChartResponseDto;
import com.AISA.AISA.kisStock.dto.StockPrice.StockPriceDto;
import com.AISA.AISA.kisStock.kisService.KisStockService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
            @RequestParam String dateType
    ) {
        IndexChartResponseDto chartData = kisStockService.getIndexChart(marketCode, date, dateType);
        return ResponseEntity.ok(new SuccessResponse<>(true, "기간별 지수 정보 조회 성공", chartData));
    }
}

package com.AISA.AISA.kisStock.kisController;

import com.AISA.AISA.global.response.SuccessResponse;
import com.AISA.AISA.kisStock.dto.Dividend.StockDividendInfoDto;

import com.AISA.AISA.kisStock.kisService.DividendService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dividend")
@RequiredArgsConstructor
@Tag(name = "배당 API", description = "배당 관련 API")
public class DividendController {

    private final DividendService dividendService;

    @GetMapping("/{stockCode}/dividend")
    @Operation(summary = "주식 배당 내역 조회", description = "특정 주식의 과거 배당금 지급 내역을 조회합니다.")
    public ResponseEntity<SuccessResponse<List<StockDividendInfoDto>>> getDividendInfo(
            @PathVariable String stockCode,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        List<StockDividendInfoDto> dividendInfoList = dividendService.getDividendInfo(stockCode, startDate, endDate);
        return ResponseEntity.ok(new SuccessResponse<>(true, "주식 배당금 조회 성공", dividendInfoList));
    }

}

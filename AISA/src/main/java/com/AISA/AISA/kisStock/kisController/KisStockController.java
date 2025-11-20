package com.AISA.AISA.kisStock.kisController;

import com.AISA.AISA.global.response.SuccessResponse;
import com.AISA.AISA.kisStock.dto.StockPriceDto;
import com.AISA.AISA.kisStock.dto.StockPriceResponse;
import com.AISA.AISA.kisStock.kisService.KisStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
public class KisStockController {
    private final KisStockService kisStockService;

    @GetMapping("/{stockCode}/price")
    public ResponseEntity<SuccessResponse<StockPriceDto>> getStockPrice(@PathVariable String stockCode) {
        StockPriceDto stockPriceDto = kisStockService.getStockPrice(stockCode);
        return ResponseEntity.ok(new SuccessResponse<>(true, "주식 현재가 조회 성공", stockPriceDto));
    }
}

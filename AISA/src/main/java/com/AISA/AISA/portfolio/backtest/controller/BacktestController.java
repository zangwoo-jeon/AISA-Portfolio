package com.AISA.AISA.portfolio.backtest.controller;

import com.AISA.AISA.global.response.SuccessResponse;
import com.AISA.AISA.portfolio.backtest.dto.BacktestResultDto;
import com.AISA.AISA.portfolio.backtest.service.BacktestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/backtest")
@RequiredArgsConstructor
@Tag(name = "포트폴리오 백테스트 API", description = "포트폴리오 과거 수익률 시뮬레이션 관련 API")
public class BacktestController {

    private final BacktestService backtestService;

    @GetMapping("/{portId}")
    @Operation(summary = "포트폴리오 백테스트 실행", description = "특정 포트폴리오의 과거 기간 수익률을 시뮬레이션합니다.")
    public ResponseEntity<SuccessResponse<BacktestResultDto>> runBacktest(
            @PathVariable UUID portId,
            @RequestParam String startDate,
            @RequestParam String endDate) {

        BacktestResultDto result = backtestService.calculatePortfolioBacktest(portId, startDate, endDate);
        return ResponseEntity.ok(new SuccessResponse<>(true, "백테스트 실행 성공", result));
    }
}

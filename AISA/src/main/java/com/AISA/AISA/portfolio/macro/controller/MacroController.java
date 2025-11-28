package com.AISA.AISA.portfolio.macro.controller;

import com.AISA.AISA.global.response.SuccessResponse;
import com.AISA.AISA.kisStock.dto.Index.IndexChartPriceDto;
import com.AISA.AISA.kisStock.dto.Index.IndexChartResponseDto;
import com.AISA.AISA.kisStock.kisService.KisIndexService;
import com.AISA.AISA.portfolio.macro.dto.MacroIndicatorDto;
import com.AISA.AISA.portfolio.macro.service.EcosService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/macro")
@RequiredArgsConstructor
@Tag(name = "거시경제 지표 API", description = "환율, 통화량 등 거시경제 데이터 조회")
public class MacroController {

    private final EcosService ecosService;

    private final KisIndexService kisIndexService;

    @GetMapping("/exchange-rate")
    @Operation(summary = "원/달러 환율 조회", description = "DB에 저장된 원/달러 환율을 조회합니다. (데이터가 없으면 POST /init을 호출하세요)")
    public ResponseEntity<SuccessResponse<List<MacroIndicatorDto>>> getExchangeRate(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        List<MacroIndicatorDto> data = ecosService.fetchExchangeRate(startDate, endDate);
        return ResponseEntity.ok(new SuccessResponse<>(true, "환율 조회 성공", data));
    }

    @PostMapping("/exchange-rate/init")
    @Operation(summary = "원/달러 환율 데이터 초기화/업데이트", description = "한국은행 ECOS API에서 원/달러 환율 데이터를 가져와 DB에 저장합니다.")
    public ResponseEntity<SuccessResponse<Void>> initExchangeRate(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        ecosService.saveExchangeRateData(startDate, endDate);
        return ResponseEntity.ok(new SuccessResponse<>(true, "환율 데이터 저장 성공", null));
    }

    @GetMapping("/m2")
    @Operation(summary = "M2 통화량 조회", description = "DB에 저장된 M2(광의통화)를 조회합니다. (데이터가 없으면 POST /init을 호출하세요)")
    public ResponseEntity<SuccessResponse<List<MacroIndicatorDto>>> getM2MoneySupply(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        List<MacroIndicatorDto> data = ecosService.fetchM2MoneySupply(startDate, endDate);
        return ResponseEntity.ok(new SuccessResponse<>(true, "M2 통화량 조회 성공", data));
    }

    @PostMapping("/m2/init")
    @Operation(summary = "M2 통화량 데이터 초기화/업데이트", description = "한국은행 ECOS API에서 M2 데이터를 가져와 DB에 저장합니다.")
    public ResponseEntity<SuccessResponse<Void>> initM2MoneySupply(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        ecosService.saveM2Data(startDate, endDate);
        return ResponseEntity.ok(new SuccessResponse<>(true, "M2 데이터 저장 성공", null));
    }

    @GetMapping("/kospi-usd-ratio")
    @Operation(summary = "달러 환산 코스피 지수 조회", description = "코스피 지수를 원/달러 환율로 나누어 달러 기준 가치를 계산합니다. (KOSPI / (환율 / 1000))")
    public ResponseEntity<SuccessResponse<List<MacroIndicatorDto>>> getKospiUsdRatio(
            @RequestParam String startDate,
            @RequestParam String endDate) {

        // 1. Fetch KOSPI Data
        IndexChartResponseDto kospiData = kisIndexService.getIndexChart("KOSPI", startDate, endDate, "D");
        Map<String, BigDecimal> kospiMap = kospiData.getPriceList().stream()
                .collect(Collectors.toMap(
                        IndexChartPriceDto::getDate,
                        dto -> new BigDecimal(dto.getPrice())));

        // 2. Fetch Exchange Rate Data
        List<MacroIndicatorDto> exchangeRateData = ecosService.fetchExchangeRate(startDate, endDate);
        Map<String, BigDecimal> exchangeRateMap = exchangeRateData.stream()
                .collect(Collectors.toMap(
                        MacroIndicatorDto::getDate,
                        dto -> new BigDecimal(dto.getValue())));

        // 3. Calculate Ratio
        List<MacroIndicatorDto> ratioList = new ArrayList<>();
        List<String> sortedDates = new ArrayList<>(kospiMap.keySet());
        Collections.sort(sortedDates);

        for (String date : sortedDates) {
            if (exchangeRateMap.containsKey(date)) {
                BigDecimal kospi = kospiMap.get(date);
                BigDecimal exchangeRate = exchangeRateMap.get(date);

                if (exchangeRate.compareTo(BigDecimal.ZERO) > 0) {
                    // Formula: KOSPI / (ExchangeRate / 1000)
                    BigDecimal ratio = kospi.divide(
                            exchangeRate.divide(new BigDecimal(1000), 4, RoundingMode.HALF_UP),
                            2, RoundingMode.HALF_UP);

                    ratioList.add(new MacroIndicatorDto(date, ratio.toString()));
                }
            }
        }

        return ResponseEntity.ok(new SuccessResponse<>(true, "달러 환산 코스피 조회 성공", ratioList));
    }
}

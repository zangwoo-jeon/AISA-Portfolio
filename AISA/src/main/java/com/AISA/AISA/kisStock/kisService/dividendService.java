package com.AISA.AISA.kisStock.kisService;

import com.AISA.AISA.global.exception.BusinessException;
import com.AISA.AISA.kisStock.Entity.stock.Stock;
import com.AISA.AISA.kisStock.config.KisApiProperties;
import com.AISA.AISA.kisStock.dto.Dividend.KisDividendApiResponse;
import com.AISA.AISA.kisStock.dto.Dividend.StockDividendInfoDto;
import com.AISA.AISA.kisStock.dto.StockPrice.KisPriceApiResponse;
import com.AISA.AISA.kisStock.dto.StockPrice.StockPriceDto;
import com.AISA.AISA.kisStock.dto.StockPrice.StockPriceResponse;
import com.AISA.AISA.kisStock.exception.KisApiErrorCode;
import com.AISA.AISA.kisStock.kisService.Auth.KisAuthService;
import com.AISA.AISA.kisStock.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class dividendService {

    private final WebClient webClient;
    private final KisAuthService kisAuthService;
    private final KisApiProperties kisApiProperties;
    private final StockRepository stockRepository;

    public List<StockDividendInfoDto> getDividendInfo(
            String stockCode,
            String startDate,
            String endDate
    ) {
        String accessToken = kisAuthService.getAccessToken();

        Stock stock = stockRepository.findByStockCode(stockCode)
                .orElseThrow(() -> new BusinessException(KisApiErrorCode.STOCK_NOT_FOUND));

        KisDividendApiResponse apiResponse = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(kisApiProperties.getDividendUrl())
                        .queryParam("CTS", "")
                        .queryParam("GB1", "0") // 0:배당전체, 1:결산배당, 2:중간배당
                        .queryParam("F_DT", startDate)
                        .queryParam("T_DT", endDate)
                        .queryParam("SHT_CD", stockCode)
                        .queryParam("HIGH_GB", "")
                        .build())
                .header("authorization", accessToken) // 접근 토큰
                .header("appKey", kisApiProperties.getAppkey()) // 앱키
                .header("appSecret", kisApiProperties.getAppsecret()) // 앱시크릿키
                .header("tr_id", "HHKDB669102C0") // 배당 조회용 tr_id
                .retrieve()
                .bodyToMono(KisDividendApiResponse.class)// 서버에서 온 JSON을 DTO 클래스(KisPriceApiResponse)로 매핑
                .onErrorMap(error -> {
                    log.error("{}의 배당 정보을 불러오는 데 실패했습니다. 에러: {}", stockCode, error.getMessage());
                    return new BusinessException(KisApiErrorCode.DIVIDEND_FETCH_FAILED);
                })
                .block(); // 동기식 객체로 변환

        if (apiResponse == null || !"0".equals(apiResponse.getRtCd()) || apiResponse.getOutput1() == null) {
            log.warn("{}의 배당 정보가 없습니다.", stockCode);
            return Collections.emptyList();
        }

        return apiResponse.getOutput1().stream()
                .map(apiDto -> StockDividendInfoDto.builder()
                        .stockCode(apiDto.getStockCode())
                        .stockName(apiDto.getStockName())
                                .recordDate(apiDto.getRecordDate())
                                .paymentDate(apiDto.getPaymentDate())
                                .dividendAmount(new BigDecimal(apiDto.getDividendAmount()))
                                .dividendRate(Double.parseDouble(apiDto.getDividendRate()))
                                        .build())
                .collect(Collectors.toList());
    }
}

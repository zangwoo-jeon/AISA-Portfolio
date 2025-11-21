package com.AISA.AISA.kisStock.kisService;


import com.AISA.AISA.kisStock.Entity.Stock;
import com.AISA.AISA.kisStock.Repository.StockRepository;
import com.AISA.AISA.kisStock.config.KisApiProperties;
import com.AISA.AISA.kisStock.dto.Index.*;
import com.AISA.AISA.kisStock.dto.StockPrice.KisPriceApiResponse;
import com.AISA.AISA.kisStock.dto.StockPrice.StockPriceDto;
import com.AISA.AISA.kisStock.dto.StockPrice.StockPriceResponse;
import com.AISA.AISA.kisStock.exception.KisApiErrorCode;
import com.AISA.AISA.kisStock.kisService.Auth.KisAuthService;
import com.AISA.AISA.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class KisStockService {
    private final WebClient webClient;
    private final KisAuthService kisAuthService;
    private final KisApiProperties kisApiProperties;
    private final StockRepository stockRepository;

    public StockPriceDto getStockPrice(String stockCode) {
        String accessToken = kisAuthService.getAccessToken();

        Stock stock = stockRepository.findByStockCode(stockCode)
                .orElseThrow(() -> new BusinessException(KisApiErrorCode.STOCK_NOT_FOUND));


        KisPriceApiResponse apiResponse = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(kisApiProperties.getPriceUrl())
                        .queryParam("fid_cond_mrkt_div_code", "J") // 조건 시장 분류 코드(J:KRX, NX:NXT, UN:통합)
                        .queryParam("fid_input_iscd", stockCode) // 입력 종목코드(종목코드 (ex 005930 삼성전자))
                        .build())
                .header("Authorization", accessToken) // 접근 토큰
                .header("appKey", kisApiProperties.getAppkey()) //앱키
                .header("appSecret", kisApiProperties.getAppsecret()) //앱시크릿키
                .header("tr_id", "FHKST01010100") //거래ID
                .retrieve()
                .bodyToMono(KisPriceApiResponse.class)// 서버에서 온 JSON을 DTO 클래스(KisPriceApiResponse)로 매핑
                .onErrorMap(error -> {
                    log.error("{}의 주식 가격을 불러오는 데 실패했습니다. 에러: {}", stockCode, error.getMessage());
                return new BusinessException(KisApiErrorCode.STOCK_PRICE_FETCH_FAILED);
                })
                .block(); //동기식 객체로 변환

        StockPriceResponse raw = apiResponse.getOutput();

        return new StockPriceDto(
                raw.getStockCode(),
                stock.getStockName(),
                stock.getMarketName(),
                raw.getStockPriceRaw(),
                raw.getPriceChangeRaw(),
                raw.getChangeRateRaw(),
                raw.getAccumulatedVolumeRaw(),
                raw.getOpeningPriceRaw()
        );
    }
    
    public IndexChartResponseDto getIndexChart(String marketCode, String date, String dateType) {
        // 1. 토큰 처리 (Bearer가 이미 붙어있는지 확인 필요)
        String accessToken = kisAuthService.getAccessToken();
        String authorizationHeader = accessToken.startsWith("Bearer ") ? accessToken : "Bearer " + accessToken;

        String fidInputIscd = switch (marketCode.toUpperCase()) {
            case "KOSPI" -> "0001";
            case "KOSDAQ" -> "1001";
            default -> throw new BusinessException(KisApiErrorCode.INVALID_MARKET_CODE);
        };

        KisIndexChartApiResponse apiResponse = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(kisApiProperties.getIndexChartUrl()) // url 확인: /uapi/domestic-stock/v1/quotations/inquire-index-daily-price
                        .queryParam("FID_PERIOD_DIV_CODE", dateType) // D : 일별, W : 주별, M : 월별
                        .queryParam("FID_COND_MRKT_DIV_CODE", "U")
                        .queryParam("FID_INPUT_ISCD", fidInputIscd) // KOSPI
                        .queryParam("FID_INPUT_DATE_1", date)
                        .build())
                .header("authorization", authorizationHeader) // [수정] 명확한 변수명 사용 및 Bearer 접두사 한번만 붙도록 보장
                .header("appkey", kisApiProperties.getAppkey())
                .header("appsecret", kisApiProperties.getAppsecret())
                .header("tr_id", "FHPUP02120000")
                .header("custtype", "P") // [중요] 고객 타입 추가 (P: 개인)
                .retrieve()
                .bodyToMono(KisIndexChartApiResponse.class)
                .onErrorMap(error -> {
                    // 로깅을 통해 실제 API가 뱉는 에러 메시지를 확인하는 것이 좋습니다.
                    log.error("KIS API Error for {}: {}", marketCode, error.getMessage());
                    return new BusinessException(KisApiErrorCode.INDEX_FETCH_FAILED);
                })
                .block();

        // 응답 검증 강화 (output1이 null이거나 비어있으면 실패로 간주)
        if (apiResponse == null || !"0".equals(apiResponse.getRtCd())) { // Check rtCd for success
            log.error("API Response is valid but list is empty. Msg: {}",
                    apiResponse != null ? apiResponse.getMsg1() : "null response");
            throw new BusinessException(KisApiErrorCode.INDEX_FETCH_FAILED);
        }

        IndexChartInfoDto chartInfoDto = IndexChartInfoDto.builder()
                .marketName(marketCode.toUpperCase())
                .currentIndices(apiResponse.getTodayInfo().getCurrentIndices())
                .priceChange(apiResponse.getTodayInfo().getPriceChange())
                .changeRate(apiResponse.getTodayInfo().getChangeRate())
                .build();

        List<IndexChartPriceDto> chartPriceList = apiResponse.getDateInfoList().stream()
                .map(apiPrice -> IndexChartPriceDto.builder()
                        .date(apiPrice.getDate())
                        .price(apiPrice.getPrice())
                        .openPrice(apiPrice.getOpenPrice())
                        .highPrice(apiPrice.getHighPrice())
                        .lowPrice(apiPrice.getLowPrice())
                        .build())
                .collect(java.util.stream.Collectors.toList());

        return IndexChartResponseDto.builder()
                .info(chartInfoDto)
                .priceList(chartPriceList)
                .build();

    }
}

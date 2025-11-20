package com.AISA.AISA.kisStock.kisService;


import com.AISA.AISA.kisStock.Entity.Stock;
import com.AISA.AISA.kisStock.Repository.StockRepository;
import com.AISA.AISA.kisStock.config.KisApiProperties;
import com.AISA.AISA.kisStock.dto.KisPriceApiResponse;
import com.AISA.AISA.kisStock.dto.StockPriceDto;
import com.AISA.AISA.kisStock.dto.StockPriceResponse;
import com.AISA.AISA.kisStock.exception.KisApiErrorCode;
import com.AISA.AISA.kisStock.kisService.Auth.KisAuthService;
import com.AISA.AISA.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

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
                        .queryParam("fid_cond_mrkt_div_code", "J")
                        .queryParam("fid_input_iscd", stockCode)
                        .build())
                .header("Authorization", accessToken)
                .header("appKey", kisApiProperties.getAppkey())
                .header("appSecret", kisApiProperties.getAppsecret())
                .header("tr_id", "FHKST01010100")
                .retrieve()
                .bodyToMono(KisPriceApiResponse.class)
                .onErrorMap(error -> {
                    log.error("{}의 주식 가격을 불러오는 데 실패했습니다. 에러: {}", stockCode, error.getMessage());
                return new BusinessException(KisApiErrorCode.STOCK_PRICE_FETCH_FAILED);
                })
                .block();

        StockPriceResponse raw = apiResponse.getOutput();

        return new StockPriceDto(
                raw.getStockCode(),
                stock.getStockName(),
                raw.getMarketName(),
                raw.getStockPriceRaw(),
                raw.getPriceChangeRaw(),
                raw.getChangeRateRaw(),
                raw.getAccumulatedVolumeRaw(),
                raw.getOpeningPriceRaw()
        );
    }

}

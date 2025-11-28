package com.AISA.AISA.kisStock.kisService;

import com.AISA.AISA.kisStock.Entity.stock.Stock;
import com.AISA.AISA.kisStock.dto.StockPrice.*;
import com.AISA.AISA.kisStock.repository.StockRepository;
import com.AISA.AISA.kisStock.Entity.stock.StockDailyData;
import com.AISA.AISA.kisStock.repository.StockDailyDataRepository;
import com.AISA.AISA.kisStock.config.KisApiProperties;
import com.AISA.AISA.kisStock.exception.KisApiErrorCode;
import com.AISA.AISA.kisStock.kisService.Auth.KisAuthService;
import com.AISA.AISA.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class KisStockService {
        private final WebClient webClient;
        private final KisAuthService kisAuthService;
        private final KisApiProperties kisApiProperties;
        private final StockRepository stockRepository;
        private final StockDailyDataRepository stockDailyDataRepository;

        public StockPriceDto getStockPrice(String stockCode) {
                String accessToken = kisAuthService.getAccessToken();

                Stock stock = stockRepository.findByStockCode(stockCode)
                                .orElseThrow(() -> new BusinessException(KisApiErrorCode.STOCK_NOT_FOUND));

                KisPriceApiResponse apiResponse = webClient.get()
                                .uri(uriBuilder -> uriBuilder
                                                .path(kisApiProperties.getPriceUrl())
                                                .queryParam("fid_cond_mrkt_div_code", "J") // 조건 시장 분류 코드(J:KRX, NX:NXT,
                                                                                           // UN:통합)
                                                .queryParam("fid_input_iscd", stockCode) // 입력 종목코드(종목코드 (ex 005930
                                                                                         // 삼성전자))
                                                .build())
                                .header("Authorization", accessToken) // 접근 토큰
                                .header("appKey", kisApiProperties.getAppkey()) // 앱키
                                .header("appSecret", kisApiProperties.getAppsecret()) // 앱시크릿키
                                .header("tr_id", "FHKST01010100") // 거래ID
                                .retrieve()
                                .bodyToMono(KisPriceApiResponse.class)// 서버에서 온 JSON을 DTO 클래스(KisPriceApiResponse)로 매핑
                                .onErrorMap(error -> {
                                        log.error("{}의 주식 가격을 불러오는 데 실패했습니다. 에러: {}", stockCode, error.getMessage());
                                        return new BusinessException(KisApiErrorCode.STOCK_PRICE_FETCH_FAILED);
                                })
                                .block(); // 동기식 객체로 변환

                StockPriceResponse raw = apiResponse.getOutput();

                return new StockPriceDto(
                                raw.getStockCode(),
                                stock.getStockName(),
                                stock.getMarketName(),
                                raw.getStockPriceRaw(),
                                raw.getPriceChangeRaw(),
                                raw.getChangeRateRaw(),
                                raw.getAccumulatedVolumeRaw(),
                                raw.getOpeningPriceRaw());
        }

        public StockChartResponseDto getStockChart(String stockCode, String startDate, String endDate,
                        String dateType) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
                LocalDate targetStartDate = LocalDate.parse(startDate, formatter);
                LocalDate targetEndDate = LocalDate.parse(endDate, formatter);

                Stock stock = stockRepository.findByStockCode(stockCode)
                                .orElseThrow(() -> new BusinessException(KisApiErrorCode.STOCK_NOT_FOUND));

                try {
                        // 1. API에서 최신 데이터 조회 (최대 100건)
                        // endDate를 기준으로 데이터를 요청합니다.
                        // 100건의 거래일이 정확히 며칠인지 알 수 없으므로, 넉넉한 기간(예: 150일)을 설정하여 요청합니다.
                        // API는 범위 내에서 최신 100건을 반환하거나, 범위가 너무 넓으면 에러를 반환할 수 있습니다.
                        // 여기서는 [max(startDate, endDate-150days), endDate] 범위로 요청합니다.
                        LocalDate apiStartDate = targetEndDate.minusDays(150);
                        if (apiStartDate.isBefore(targetStartDate)) {
                                apiStartDate = targetStartDate;
                        }
                        String apiStartDateStr = apiStartDate.format(formatter);

                        StockChartResponseDto apiResponse = fetchStockChartFromApi(stockCode, apiStartDateStr, endDate,
                                        dateType);

                        // 2. API 데이터를 DB에 저장
                        if (apiResponse.getPriceList() != null && !apiResponse.getPriceList().isEmpty()) {
                                for (StockChartPriceDto priceDto : apiResponse.getPriceList()) {
                                        LocalDate parsedDate = LocalDate.parse(priceDto.getDate(), formatter);
                                        if (stockDailyDataRepository.findByStockAndDate(stock, parsedDate)
                                                        .isPresent()) {
                                                continue;
                                        }

                                        StockDailyData entity = StockDailyData.builder()
                                                        .stock(stock)
                                                        .date(parsedDate)
                                                        .closingPrice(new BigDecimal(priceDto.getClosePrice()))
                                                        .openingPrice(new BigDecimal(priceDto.getOpenPrice()))
                                                        .highPrice(new BigDecimal(priceDto.getHighPrice()))
                                                        .lowPrice(new BigDecimal(priceDto.getLowPrice()))
                                                        .volume(new BigDecimal(priceDto.getVolume()))
                                                        .priceChange(null)
                                                        .changeRate(null)
                                                        .build();
                                        stockDailyDataRepository.save(entity);
                                }
                        }

                        // 3. 요청된 기간에 맞게 API 데이터 필터링
                        List<StockChartPriceDto> filteredApiList = new ArrayList<>();
                        if (apiResponse.getPriceList() != null) {
                                filteredApiList = apiResponse.getPriceList().stream()
                                                .filter(dto -> {
                                                        LocalDate dtoDate = LocalDate.parse(dto.getDate(), formatter);
                                                        return !dtoDate.isBefore(targetStartDate)
                                                                        && !dtoDate.isAfter(targetEndDate);
                                                })
                                                .collect(Collectors.toList());
                        }

                        // 4. 부족한 과거 데이터를 DB에서 조회
                        List<StockChartPriceDto> dbPriceList = new ArrayList<>();
                        if (!filteredApiList.isEmpty()) {
                                String oldestApiDateStr = filteredApiList.get(filteredApiList.size() - 1).getDate();
                                LocalDate oldestApiDate = LocalDate.parse(oldestApiDateStr, formatter);

                                if (oldestApiDate.isAfter(targetStartDate)) {
                                        List<StockDailyData> pastDataList = stockDailyDataRepository
                                                        .findAllByStock_StockCodeAndDateBetweenOrderByDateDesc(
                                                                        stockCode, targetStartDate,
                                                                        oldestApiDate.minusDays(1));
                                        dbPriceList = pastDataList.stream()
                                                        .map(this::convertToDto)
                                                        .collect(Collectors.toList());
                                }
                        } else {
                                // API가 범위 내 데이터를 반환하지 않았거나, 요청 범위가 API의 최신 100건 범위를 완전히 벗어난 경우
                                // DB에서 전체 데이터를 조회 시도
                                List<StockDailyData> pastDataList = stockDailyDataRepository
                                                .findAllByStock_StockCodeAndDateBetweenOrderByDateDesc(
                                                                stockCode, targetStartDate, targetEndDate);
                                dbPriceList = pastDataList.stream()
                                                .map(this::convertToDto)
                                                .collect(Collectors.toList());
                        }

                        // 5. 병합 (API 데이터는 내림차순, DB 데이터도 내림차순)
                        // API 데이터: [최신 ... 과거]
                        // DB 데이터: [최신 ... 과거] (범위: [Start, OldestApi - 1])
                        // 따라서 API 리스트 뒤에 DB 리스트를 추가합니다.
                        List<StockChartPriceDto> mergedList = new ArrayList<>(filteredApiList);
                        mergedList.addAll(dbPriceList);

                        return StockChartResponseDto.builder()
                                        .rtCd(apiResponse.getRtCd())
                                        .msg1(apiResponse.getMsg1())
                                        .priceList(mergedList)
                                        .build();

                } catch (Exception e) {
                        log.warn("API 조회 실패, DB 데이터로 대체합니다: {}", e.getMessage());
                        // 대체: DB에서 전체 조회
                        List<StockDailyData> pastDataList = stockDailyDataRepository
                                        .findAllByStock_StockCodeAndDateBetweenOrderByDateDesc(
                                                        stockCode, targetStartDate, targetEndDate);
                        List<StockChartPriceDto> dbPriceList = pastDataList.stream()
                                        .map(this::convertToDto)
                                        .collect(Collectors.toList());

                        return StockChartResponseDto.builder()
                                        .rtCd("0")
                                        .msg1("Fetched from DB (Fallback)")
                                        .priceList(dbPriceList)
                                        .build();
                }
        }

        private StockChartResponseDto fetchStockChartFromApi(String stockCode,
                        String startDate, String endDate, String dateType) {
                String accessToken = kisAuthService.getAccessToken();

                StockChartResponseDto apiResponse;
                try {
                        apiResponse = webClient.get()
                                        .uri(uriBuilder -> uriBuilder
                                                        .path(kisApiProperties.getStockChartUrl())
                                                        .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                                                        .queryParam("FID_INPUT_ISCD", stockCode)
                                                        .queryParam("FID_INPUT_DATE_1", startDate)
                                                        .queryParam("FID_INPUT_DATE_2", endDate)
                                                        .queryParam("FID_PERIOD_DIV_CODE", dateType)
                                                        .queryParam("FID_ORG_ADJ_PRC", "0")
                                                        .build())
                                        .header("authorization", accessToken)
                                        .header("appkey", kisApiProperties.getAppkey())
                                        .header("appsecret", kisApiProperties.getAppsecret())
                                        .header("tr_id", "FHKST03010100")
                                        .header("custtype", "P")
                                        .retrieve()
                                        .bodyToMono(StockChartResponseDto.class)
                                        .block();
                } catch (Exception e) {
                        log.error("KIS API WebClient Error for {}: {}", stockCode, e.getMessage(), e);
                        throw new BusinessException(KisApiErrorCode.INDEX_FETCH_FAILED);
                }

                if (apiResponse == null || !"0".equals(apiResponse.getRtCd())) {
                        log.error("API Response is valid but list is empty or error. RtCd: {}, Msg: {}",
                                        apiResponse != null ? apiResponse.getRtCd() : "null",
                                        apiResponse != null ? apiResponse.getMsg1() : "null response");
                        throw new BusinessException(KisApiErrorCode.INDEX_FETCH_FAILED);
                }

                return apiResponse;
        }

        @Transactional
        public void fetchAndSaveHistoricalStockData(String stockCode, String startDateStr) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
                LocalDate targetStartDate = LocalDate.parse(startDateStr, formatter);
                LocalDate currentDate = LocalDate.now();

                log.info("Starting historical stock data fetch for {} from {} to {}", stockCode, currentDate,
                                targetStartDate);

                Stock stock = stockRepository.findByStockCode(stockCode)
                                .orElseThrow(() -> new BusinessException(KisApiErrorCode.STOCK_NOT_FOUND));

                int consecutiveFailures = 0;
                final int MAX_CONSECUTIVE_FAILURES = 5;

                while (currentDate.isAfter(targetStartDate)) {
                        String currentDateStr = currentDate.format(formatter);

                        LocalDate queryStartDate = currentDate.minusDays(100);
                        if (queryStartDate.isBefore(targetStartDate)) {
                                queryStartDate = targetStartDate;
                        }
                        String queryStartDateStr = queryStartDate.format(formatter);

                        log.info("Fetching data for {} from {} to {}", stockCode, queryStartDateStr, currentDateStr);

                        try {
                                StockChartResponseDto response = fetchStockChartFromApi(stockCode, queryStartDateStr,
                                                currentDateStr, "D");

                                if (response.getPriceList() == null || response.getPriceList().isEmpty()) {
                                        log.warn("No data returned for {} from {} to {}. Retrying with previous period.",
                                                        stockCode, queryStartDateStr, currentDateStr);
                                        // Move back
                                        currentDate = queryStartDate.minusDays(1);
                                        consecutiveFailures++;
                                        if (consecutiveFailures > MAX_CONSECUTIVE_FAILURES) {
                                                log.error("Too many consecutive failures ({}). Stopping.",
                                                                consecutiveFailures);
                                                break;
                                        }
                                        continue;
                                }

                                consecutiveFailures = 0;

                                for (StockChartPriceDto priceDto : response.getPriceList()) {
                                        LocalDate parsedDate = LocalDate.parse(priceDto.getDate(), formatter);

                                        if (stockDailyDataRepository.findByStockAndDate(stock, parsedDate)
                                                        .isPresent()) {
                                                continue;
                                        }

                                        StockDailyData entity = StockDailyData.builder()
                                                        .stock(stock)
                                                        .date(parsedDate)
                                                        .closingPrice(new BigDecimal(priceDto.getClosePrice()))
                                                        .openingPrice(new BigDecimal(priceDto.getOpenPrice()))
                                                        .highPrice(new BigDecimal(priceDto.getHighPrice()))
                                                        .lowPrice(new BigDecimal(priceDto.getLowPrice()))
                                                        .volume(new BigDecimal(priceDto.getVolume()))
                                                        .priceChange(null) // DTO에 아직 없음
                                                        .changeRate(null) // DTO에 아직 없음
                                                        .build();

                                        stockDailyDataRepository.save(entity);
                                }

                                // Update currentDate to the day before the oldest fetched date
                                String oldestDateStr = response.getPriceList().get(response.getPriceList().size() - 1)
                                                .getDate();
                                LocalDate oldestDate = LocalDate.parse(oldestDateStr, formatter);

                                if (!oldestDate.isBefore(currentDate)) {
                                        // API가 동일한 날짜 범위를 반환하여 무한 루프에 빠지는 것을 방지
                                        log.warn("가장 오래된 날짜 {}가 현재 날짜 {}보다 이전이 아닙니다. 수동으로 이동합니다.",
                                                        oldestDate, currentDate);
                                        currentDate = currentDate.minusDays(100);
                                } else {
                                        currentDate = oldestDate.minusDays(1);
                                }

                                Thread.sleep(100); // 속도 제한

                        } catch (Exception e) {
                                log.error("Error fetching historical data for {}: {}", stockCode, e.getMessage());
                                currentDate = currentDate.minusDays(1); // 뒤로 이동 시도
                                consecutiveFailures++;
                                if (consecutiveFailures > MAX_CONSECUTIVE_FAILURES) {
                                        break;
                                }
                        }
                }
                log.info("Finished historical stock data fetch for {}", stockCode);
        }

        public void fetchAllStocksHistoricalData(String startDateStr) {
                log.info("Starting batch historical stock data fetch from {}", startDateStr);
                List<Stock> allStocks = stockRepository.findAll();

                for (Stock stock : allStocks) {
                        try {
                                log.info("Updating data for stock: {} ({})", stock.getStockName(),
                                                stock.getStockCode());
                                fetchAndSaveHistoricalStockData(stock.getStockCode(), startDateStr);

                                // API 과부하 방지를 위한 지연 추가
                                Thread.sleep(500);
                        } catch (Exception e) {
                                log.error("Failed to update stock: {} ({}) - {}", stock.getStockName(),
                                                stock.getStockCode(), e.getMessage());
                        }
                }
                log.info("Completed batch historical stock data fetch.");
        }

        private StockChartPriceDto convertToDto(StockDailyData entity) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
                return StockChartPriceDto.builder()
                                .date(entity.getDate().format(formatter))
                                .closePrice(entity.getClosingPrice().toString())
                                .openPrice(entity.getOpeningPrice().toString())
                                .highPrice(entity.getHighPrice().toString())
                                .lowPrice(entity.getLowPrice().toString())
                                .volume(entity.getVolume().toString())
                                .build();
        }

}

package com.AISA.AISA.portfolio.macro.service;

import com.AISA.AISA.kisStock.config.EcosApiProperties;
import com.AISA.AISA.portfolio.macro.Entity.MacroDailyData;
import com.AISA.AISA.portfolio.macro.repository.MacroDailyDataRepository;
import com.AISA.AISA.portfolio.macro.dto.MacroIndicatorDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EcosService {

    private final EcosApiProperties ecosApiProperties;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final MacroDailyDataRepository macroDailyDataRepository;

    // 통계표코드 / 주기 / 항목코드
    private static final String STAT_CODE_M2 = "101Y004"; // M2(광의통화, 평잔, 원계열)
    private static final String ITEM_CODE_M2_TOTAL = "BBHA00"; // M2(평잔, 원계열)

    private static final String STAT_CODE_BASE_RATE = "722Y001"; // 한국은행 기준금리 및 여수신금리
    private static final String ITEM_CODE_BASE_RATE = "0101000"; // 한국은행 기준금리

    @Transactional(readOnly = true)
    public List<MacroIndicatorDto> fetchM2MoneySupply(String startDateStr, String endDateStr) {
        String startMonth = startDateStr.substring(0, 6);
        String endMonth = endDateStr.substring(0, 6);
        return getMacroDataFromDb(STAT_CODE_M2, ITEM_CODE_M2_TOTAL, startMonth, endMonth, "M");
    }

    @Transactional(readOnly = true)
    public List<MacroIndicatorDto> fetchBaseRate(String startDateStr, String endDateStr) {
        String startMonth = startDateStr.substring(0, 6);
        String endMonth = endDateStr.substring(0, 6);
        return getMacroDataFromDb(STAT_CODE_BASE_RATE, ITEM_CODE_BASE_RATE, startMonth, endMonth, "M");
    }

    @Transactional
    public void saveM2Data(String startDateStr, String endDateStr) {
        String startMonth = startDateStr.substring(0, 6);
        String endMonth = endDateStr.substring(0, 6);
        fetchAndSaveFromApi(STAT_CODE_M2, "M", ITEM_CODE_M2_TOTAL, startMonth, endMonth);
    }

    @Transactional
    public void saveBaseRate(String startDateStr, String endDateStr) {
        String startMonth = startDateStr.substring(0, 6);
        String endMonth = endDateStr.substring(0, 6);
        fetchAndSaveFromApi(STAT_CODE_BASE_RATE, "M", ITEM_CODE_BASE_RATE, startMonth, endMonth);
    }

    private List<MacroIndicatorDto> getMacroDataFromDb(String statCode, String itemCode, String reqStartDateStr,
            String reqEndDateStr, String cycle) {
        LocalDate queryStartDate = LocalDate.parse(reqStartDateStr + (cycle.equals("M") ? "01" : ""),
                DateTimeFormatter.ofPattern("yyyyMMdd"));
        LocalDate queryEndDate = LocalDate.parse(reqEndDateStr + (cycle.equals("M") ? "01" : ""),
                DateTimeFormatter.ofPattern("yyyyMMdd"));

        if (cycle.equals("M")) {
            queryEndDate = queryEndDate.withDayOfMonth(queryEndDate.lengthOfMonth());
        }

        List<MacroDailyData> dbData = macroDailyDataRepository.findAllByStatCodeAndItemCodeAndDateBetweenOrderByDateAsc(
                statCode, itemCode, queryStartDate, queryEndDate);

        return dbData.stream()
                .map(entity -> new MacroIndicatorDto(
                        entity.getDate().format(DateTimeFormatter.ofPattern("yyyyMMdd")),
                        entity.getValue().toString()))
                .collect(Collectors.toList());
    }

    private void fetchAndSaveFromApi(String statCode, String cycle, String itemCode, String startDate, String endDate) {
        String url = String.format("%s/StatisticSearch/%s/json/kr/1/1000/%s/%s/%s/%s/%s",
                ecosApiProperties.getBaseUrl(),
                ecosApiProperties.getApiKey(),
                statCode,
                cycle,
                startDate,
                endDate,
                itemCode);

        log.info("Fetching ECOS data from API: {} ({} ~ {})", statCode, startDate, endDate);

        try {
            String responseBody = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode statisticSearch = root.path("StatisticSearch");

            if (statisticSearch.isMissingNode()) {
                return;
            }

            JsonNode rows = statisticSearch.path("row");
            if (rows.isArray()) {
                for (JsonNode row : rows) {
                    String dateStr = row.path("TIME").asText();
                    String valueStr = row.path("DATA_VALUE").asText();

                    LocalDate date;
                    if (cycle.equals("M")) {
                        date = LocalDate.parse(dateStr + "01", DateTimeFormatter.ofPattern("yyyyMMdd"));
                    } else {
                        date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyyMMdd"));
                    }

                    if (macroDailyDataRepository.findTopByStatCodeAndItemCodeOrderByDateDesc(statCode, itemCode)
                            .filter(d -> d.getDate().isEqual(date)).isPresent()) {
                        continue;
                    }

                    MacroDailyData entity = MacroDailyData.builder()
                            .statCode(statCode)
                            .itemCode(itemCode)
                            .date(date)
                            .value(new java.math.BigDecimal(valueStr))
                            .build();

                    macroDailyDataRepository.save(entity);
                }
            }

        } catch (Exception e) {
            log.error("Failed to fetch/save ECOS data", e);
        }
    }
}

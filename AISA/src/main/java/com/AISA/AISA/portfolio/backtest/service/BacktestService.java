package com.AISA.AISA.portfolio.backtest.service;

import com.AISA.AISA.kisStock.Entity.stock.Stock;
import com.AISA.AISA.kisStock.Entity.stock.StockDailyData;
import com.AISA.AISA.kisStock.repository.StockDailyDataRepository;
import com.AISA.AISA.portfolio.PortfolioGroup.Portfolio;
import com.AISA.AISA.portfolio.PortfolioGroup.PortfolioRepository;
import com.AISA.AISA.portfolio.PortfolioGroup.exception.PortfolioErrorCode;
import com.AISA.AISA.portfolio.PortfolioStock.PortStock;
import com.AISA.AISA.portfolio.PortfolioStock.PortStockRepository;
import com.AISA.AISA.portfolio.backtest.dto.BacktestResultDto;
import com.AISA.AISA.portfolio.backtest.dto.DailyPortfolioValueDto;
import com.AISA.AISA.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BacktestService {

    private final PortfolioRepository portfolioRepository;
    private final PortStockRepository portStockRepository;
    private final StockDailyDataRepository stockDailyDataRepository;

    @Transactional(readOnly = true)
    public BacktestResultDto calculatePortfolioBacktest(UUID portId, String startDateStr, String endDateStr) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDate startDate = LocalDate.parse(startDateStr, formatter);
        LocalDate endDate = LocalDate.parse(endDateStr, formatter);

        Portfolio portfolio = portfolioRepository.findById(portId)
                .orElseThrow(() -> new BusinessException(PortfolioErrorCode.PORTFOLIO_NOT_FOUND));

        List<PortStock> portStocks = portStockRepository.findByPortfolio(portfolio);
        if (portStocks.isEmpty()) {
            throw new BusinessException(PortfolioErrorCode.PORTFOLIO_STOCK_NOT_FOUND);
        }

        List<Stock> stocks = portStocks.stream().map(PortStock::getStock).collect(Collectors.toList());
        Map<String, Integer> stockQuantityMap = portStocks.stream()
                .collect(Collectors.toMap(ps -> ps.getStock().getStockCode(), PortStock::getQuantity));

        // Fetch all daily data for these stocks in the range
        List<StockDailyData> allDailyData = stockDailyDataRepository.findAllByStockInAndDateBetweenOrderByDateAsc(
                stocks, startDate, endDate);

        // Group by Date
        Map<LocalDate, List<StockDailyData>> dataByDate = allDailyData.stream()
                .collect(Collectors.groupingBy(StockDailyData::getDate));

        List<LocalDate> sortedDates = new ArrayList<>(dataByDate.keySet());
        Collections.sort(sortedDates);

        if (sortedDates.isEmpty()) {
            return BacktestResultDto.builder()
                    .portId(portId)
                    .startDate(startDateStr)
                    .endDate(endDateStr)
                    .initialValue(BigDecimal.ZERO)
                    .finalValue(BigDecimal.ZERO)
                    .totalReturnRate(0.0)
                    .cagr(0.0)
                    .mdd(0.0)
                    .dailyValues(Collections.emptyList())
                    .build();
        }

        List<DailyPortfolioValueDto> dailyValues = new ArrayList<>();
        Map<String, BigDecimal> lastPrices = new HashMap<>();

        BigDecimal maxTotalValue = BigDecimal.ZERO;
        double maxDrawdown = 0.0;

        for (LocalDate date : sortedDates) {
            List<StockDailyData> dailyDataList = dataByDate.get(date);

            // Update last known prices
            for (StockDailyData data : dailyDataList) {
                lastPrices.put(data.getStock().getStockCode(), data.getClosingPrice());
            }

            // Calculate total portfolio value for this date
            BigDecimal currentTotalValue = BigDecimal.ZERO;
            for (String stockCode : stockQuantityMap.keySet()) {
                BigDecimal price = lastPrices.getOrDefault(stockCode, BigDecimal.ZERO);
                BigDecimal quantity = new BigDecimal(stockQuantityMap.get(stockCode));
                currentTotalValue = currentTotalValue.add(price.multiply(quantity));
            }

            // Calculate daily return
            Double dailyReturnRate = 0.0;
            if (!dailyValues.isEmpty()) {
                BigDecimal prevValue = dailyValues.get(dailyValues.size() - 1).getTotalValue();
                if (prevValue.compareTo(BigDecimal.ZERO) > 0) {
                    dailyReturnRate = currentTotalValue.subtract(prevValue)
                            .divide(prevValue, 4, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal(100))
                            .doubleValue();
                }
            }

            dailyValues.add(new DailyPortfolioValueDto(
                    date.format(formatter),
                    currentTotalValue,
                    dailyReturnRate));

            // MDD Calculation
            if (currentTotalValue.compareTo(maxTotalValue) > 0) {
                maxTotalValue = currentTotalValue;
            }

            if (maxTotalValue.compareTo(BigDecimal.ZERO) > 0) {
                double drawdown = maxTotalValue.subtract(currentTotalValue)
                        .divide(maxTotalValue, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal(100))
                        .doubleValue();
                if (drawdown > maxDrawdown) {
                    maxDrawdown = drawdown;
                }
            }
        }

        BigDecimal initialValue = dailyValues.get(0).getTotalValue();
        BigDecimal finalValue = dailyValues.get(dailyValues.size() - 1).getTotalValue();

        Double totalReturnRate = 0.0;
        Double cagr = 0.0;

        if (initialValue.compareTo(BigDecimal.ZERO) > 0) {
            totalReturnRate = finalValue.subtract(initialValue)
                    .divide(initialValue, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal(100))
                    .doubleValue();

            // CAGR Calculation: (Final / Initial)^(1/n) - 1
            long days = java.time.temporal.ChronoUnit.DAYS.between(sortedDates.get(0),
                    sortedDates.get(sortedDates.size() - 1));
            double years = days / 365.0;
            if (years > 0) {
                cagr = (Math.pow(finalValue.doubleValue() / initialValue.doubleValue(), 1.0 / years) - 1) * 100;
            }
        }

        return BacktestResultDto.builder()
                .portId(portId)
                .startDate(startDateStr)
                .endDate(endDateStr)
                .initialValue(initialValue)
                .finalValue(finalValue)
                .totalReturnRate(totalReturnRate)
                .cagr(Math.round(cagr * 100.0) / 100.0) // Round to 2 decimal places
                .mdd(Math.round(maxDrawdown * 100.0) / 100.0)
                .dailyValues(dailyValues)
                .build();
    }
}

package com.AISA.AISA.kisStock.Entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stock_id")
    private Long stockId;

    @Column(nullable = false, unique = true)
    private String stockCode;

    @Column(nullable = false)
    private String stockName;

    @Column(nullable = false)
    private String marketName;

    public static Stock create(String stockCode, String stockName, String marketName) {
        Stock stock = new Stock();
        stock.stockCode = stockCode;
        stock.stockName = stockName;
        stock.marketName = marketName;
        return stock;
    }

    public void updateInfo(String newName, String newMarketName) {
        this.stockName = newName;
        this.marketName = newMarketName;
    }
}

package com.AISA.AISA.portfolio.PortfolioGroup;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "portfolio")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Portfolio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long portId;
    @Column(name = "member_id", nullable = false)
    private UUID memberId;
    @Column(name = "stock_id")
    private Long stockId;
    @Column(name = "port_name", nullable = false)
    private String portName;
    @Column(name= "stock_sequence")
    private Long stockSequence;

    public Portfolio(UUID memberId, String portName) {
        this.memberId = memberId;
        this.portName = portName;
    }

    // 주식 정보 업데이트
    public void updateStock(Long stockId, Long stockSequence) {
        this.stockId = stockId;
        this.stockSequence = stockSequence;
    }

    public void removeStock() {
        this.stockId = null;
        this.stockSequence = null;
    }
}

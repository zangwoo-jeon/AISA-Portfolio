package com.AISA.AISA.portfolio;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PortfolioService {
    private final PortfolioRepository portfolioRepository;

    public List<Portfolio> findPortfolios(UUID memberId) {
        if (memberId == null) {
            return portfolioRepository.findAll();
        }
        return portfolioRepository.findByMemberId(memberId);
    }
}

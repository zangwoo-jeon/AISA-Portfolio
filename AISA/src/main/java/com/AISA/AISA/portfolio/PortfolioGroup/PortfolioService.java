package com.AISA.AISA.portfolio.PortfolioGroup;

import com.AISA.AISA.portfolio.PortfolioGroup.dto.PortfolioCreateRequest;
import com.AISA.AISA.portfolio.PortfolioGroup.dto.PortfolioNameUpdateRequest;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
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

    @Transactional
    public Portfolio createPortfolio(PortfolioCreateRequest request) {
        Portfolio newPortfolio = new Portfolio(request.getMemberId(), request.getPortName());
        return portfolioRepository.save(newPortfolio);
    }

    @Transactional
    public void deletePortfolio(UUID memberId, Long portId) {
        Portfolio portfolioToDelete = portfolioRepository.findByPortIdAndMemberId(portId, memberId)
                .orElseThrow(() -> new EntityNotFoundException("해당 포트폴리오를 찾을 수 없습니다. portId: " + portId + ", memberId: " + memberId));

        portfolioRepository.delete(portfolioToDelete);

    }

    @Transactional
    public void updatePortfolioName(UUID memberId, Long portId, PortfolioNameUpdateRequest request) {
        Portfolio portfolio = portfolioRepository.findByPortIdAndMemberId(portId, memberId)
                .orElseThrow(() -> new EntityNotFoundException("해당 포트폴리오를 찾을 수 없습니다. portId: " + portId + ", memberId: " + memberId));

        portfolio.changeName(request.getNewPortName());
    }
}

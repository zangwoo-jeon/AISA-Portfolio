package com.AISA.AISA.portfolio.PortfolioGroup;

import com.AISA.AISA.global.exception.BusinessException;
import com.AISA.AISA.portfolio.PortfolioGroup.dto.PortfolioCreateRequest;
import com.AISA.AISA.portfolio.PortfolioGroup.dto.PortfolioNameUpdateRequest;
import com.AISA.AISA.portfolio.PortfolioGroup.exception.PortfolioErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
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
        boolean isFirstPortfolio = portfolioRepository.findByMemberId(request.getMemberId()).isEmpty();
        Portfolio newPortfolio = new Portfolio(request.getMemberId(), request.getPortName());

        if (isFirstPortfolio) {
            newPortfolio.designateAsMain();
        }

        return portfolioRepository.save(newPortfolio);
    }

    @Transactional
    public void deletePortfolio(UUID memberId, UUID portId) {
        Portfolio portfolioToDelete = portfolioRepository.findByPortIdAndMemberId(portId, memberId)
                .orElseThrow(() -> new BusinessException(PortfolioErrorCode.PORTFOLIO_NOT_FOUND));

        boolean wasMain = portfolioToDelete.isMainPort();

        portfolioRepository.delete(portfolioToDelete);

        if (wasMain) {
            portfolioRepository.findByMemberId(memberId).stream()
                    .findFirst()
                    .ifPresent(Portfolio::designateAsMain);
        }

    }

    @Transactional
    public void updatePortfolioName(UUID memberId, UUID portId, PortfolioNameUpdateRequest request) {
        Portfolio portfolio = portfolioRepository.findByPortIdAndMemberId(portId, memberId)
                .orElseThrow(() -> new BusinessException(PortfolioErrorCode.PORTFOLIO_NOT_FOUND));

        portfolio.changeName(request.getNewPortName());
    }

    @Transactional
    public void changeMainPortfolio(UUID memberId, UUID portId) {
        List<Portfolio> portfolios = portfolioRepository.findByMemberId(memberId);

        if (portfolios.isEmpty()) {
            throw new BusinessException(PortfolioErrorCode.MEMBER_HAS_NO_PORTFOLIOS);
        }

        portfolios.forEach(p -> {
            if (p.getPortId().equals(portId)) p.designateAsMain();
            else p.unDesignateAsMain();
        });
    }
}

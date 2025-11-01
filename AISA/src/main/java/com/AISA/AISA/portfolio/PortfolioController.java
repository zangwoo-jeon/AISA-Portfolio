package com.AISA.AISA.portfolio;


import com.AISA.AISA.global.response.SuccessResponse;
import com.AISA.AISA.portfolio.dto.PortfolioCreateRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequestMapping("/api/portfolio")
@RestController
@RequiredArgsConstructor
@Tag(name = "포트폴리오 API", description = "포트폴리오 관련 API")
public class PortfolioController {
    private final PortfolioService portfolioService;

    @GetMapping("/list")
    public ResponseEntity<SuccessResponse<List<Portfolio>>> getPortfolios(
            @RequestParam(required = false) UUID memberId
    ) {
        List<Portfolio> portfolios = portfolioService.findPortfolios(memberId);
        String message = (memberId == null) ? "전체 포트폴리오 목록 조회 성공" : memberId + "의 포트폴리오 목록 조회 성공";
        return ResponseEntity.ok(new SuccessResponse<>(true, message, portfolios));

    }

    @PostMapping("/create")
    public ResponseEntity<SuccessResponse<Portfolio>> createPortfolio(
            @RequestBody PortfolioCreateRequest request) {
        Portfolio createdPortfolio = portfolioService.createPortfolio(request);
        return ResponseEntity.ok(new SuccessResponse<>(true, "포트폴리오 생성 성공", createdPortfolio));
    }

}

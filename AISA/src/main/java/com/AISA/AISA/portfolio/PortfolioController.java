package com.AISA.AISA.portfolio;


import com.AISA.AISA.global.response.SuccessResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RequestMapping("/api/portfolio")
@RestController
@RequiredArgsConstructor
@Tag(name = "포트폴리오 API", description = "포트폴리오 관련 API")
public class PortfolioController {
    private final PortfolioService portfolioService;
    private final PortfolioRepository portfolioRepository;

    @GetMapping("/list")
    public ResponseEntity<SuccessResponse<List<Portfolio>>> getPortList(
            @RequestParam(required = false) UUID memberId
    ) {
        List<Portfolio> portfolios = portfolioService.findPortfolios(memberId);
        return ResponseEntity.ok(new SuccessResponse<>(true, "포트폴리오 조회 성공", portfolios));

    }

}

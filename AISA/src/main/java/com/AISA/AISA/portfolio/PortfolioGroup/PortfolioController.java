package com.AISA.AISA.portfolio.PortfolioGroup;


import com.AISA.AISA.global.response.SuccessResponse;
import com.AISA.AISA.portfolio.PortfolioGroup.dto.PortfolioCreateRequest;
import io.swagger.v3.oas.annotations.Operation;
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
    @Operation(summary = "포트폴리오 조회", description = "포트폴리오 목록을 조회합니다. " +
            "memberId 값을 추가하면 해당 member의 포트폴리오를 조회합니다.")
    public ResponseEntity<SuccessResponse<List<Portfolio>>> getPortfolios(
            @RequestParam(required = false) UUID memberId
    ) {
        List<Portfolio> portfolios = portfolioService.findPortfolios(memberId);
        String message = (memberId == null) ? "전체 포트폴리오 목록 조회 성공" : memberId + "의 포트폴리오 목록 조회 성공";
        return ResponseEntity.ok(new SuccessResponse<>(true, message, portfolios));

    }

    @PostMapping("/create")
    @Operation(summary = "포트폴리오 생성", description = "포트폴리오를 생성합니다.")
    public ResponseEntity<SuccessResponse<Portfolio>> createPortfolio(
            @RequestBody PortfolioCreateRequest request) {
        Portfolio createdPortfolio = portfolioService.createPortfolio(request);
        return ResponseEntity.ok(new SuccessResponse<>(true, "포트폴리오 생성 성공", createdPortfolio));
    }

    @DeleteMapping("/remove/{memberId}/{portId}")
    @Operation(summary = "포트폴리오 삭제", description = "포트폴리오를 삭제합니다.")
    public ResponseEntity<SuccessResponse<Portfolio>> removePortfolio(
            @PathVariable UUID memberId, @PathVariable Long portId
    ) {
        portfolioService.deletePortfolio(memberId, portId);
        return ResponseEntity.ok(new SuccessResponse<>(true, "포트폴리오 삭제 성공", null));
    }

}

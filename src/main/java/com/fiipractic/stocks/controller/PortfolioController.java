package com.fiipractic.stocks.controller;

import com.fiipractic.stocks.dto.*;
import com.fiipractic.stocks.service.PortfolioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.UUID;
@RestController
@RequestMapping("/api/portfolios")
public class PortfolioController {

    private static final Logger log = LoggerFactory.getLogger(PortfolioController.class);

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @PostMapping
    public ResponseEntity<PortfolioDTO> createPortfolio(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreatePortfolioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(portfolioService.createPortfolio(jwt, request));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('USER', 'PREMIUM', 'ADMIN')")
    public ResponseEntity<List<PortfolioDTO>> getMyPortfolios(
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(portfolioService.getUserPortfolios(jwt.getSubject()));
    }

    @PostMapping("/{portfolioId}/stocks")
    public ResponseEntity<PortfolioDTO> buyStock(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long portfolioId,
            @Valid @RequestBody BuyStockRequest request) {
        return ResponseEntity.ok(portfolioService.buyStock(jwt, portfolioId, request));
    }

    @DeleteMapping("/{portfolioId}/holdings/{holdingId}")
    @PreAuthorize("hasAnyRole('USER', 'PREMIUM', 'ADMIN')")
    public ResponseEntity<PortfolioDTO> sellHolding(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long portfolioId,
            @PathVariable Long holdingId) {
        return ResponseEntity.ok(portfolioService.sellHolding(jwt, portfolioId, holdingId));
    }

    @PatchMapping("/{portfolioId}/holdings/{holdingId}")
    @PreAuthorize("hasAnyRole('USER', 'PREMIUM', 'ADMIN')")
    public ResponseEntity<PortfolioDTO> sellHoldingQuantity(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long portfolioId,
            @PathVariable Long holdingId,
            @Valid @RequestBody SellHoldingQuantityRequest request) {
        return ResponseEntity.ok(portfolioService.sellHoldingQuantity(jwt, portfolioId, holdingId, request.getQuantity()));
    }

    @DeleteMapping("/{portfolioId}/holdings")
    @PreAuthorize("hasAnyRole('USER', 'PREMIUM', 'ADMIN')")
    public ResponseEntity<PortfolioDTO> sellHoldings(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long portfolioId,
            @Valid @RequestBody SellHoldingsRequest request) {
        return ResponseEntity.ok(portfolioService.sellHoldings(jwt, portfolioId, request.getHoldingIds()));
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PortfolioDTO>> getAllPortfolios(
            @RequestParam(defaultValue = "false") boolean includeDeleted) {
        return ResponseEntity.ok(portfolioService.getAllPortfolios(includeDeleted));
    }

    @DeleteMapping("/{portfolioId}")
    @PreAuthorize("hasAnyRole('USER', 'PREMIUM', 'ADMIN')")
    public ResponseEntity<Void> deletePortfolio(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long portfolioId) {
        portfolioService.deletePortfolio(jwt, portfolioId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{portfolioId}/refresh")
    public ResponseEntity<RefreshResponseDTO> refreshPortfolioPrices(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long portfolioId) {
        String userId = jwt.getSubject();
        String correlationId = UUID.randomUUID().toString();

        try {
            MDC.put("action", "portfolio_refresh_requested");
            MDC.put("portfolioId", String.valueOf(portfolioId));
            MDC.put("userId", userId);
            MDC.put("correlationId", correlationId);
            log.info("Portfolio refresh requested for portfolio {}", portfolioId);
        } finally {
            MDC.clear();
        }

        return ResponseEntity.ok(
            portfolioService.refreshPortfolioPrices(jwt, portfolioId, correlationId)
        );
    }
    @GetMapping("/{portfolioId}/valuation")
    public ResponseEntity<PortfolioValuationDTO> getPortfolioValuation(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long portfolioId) {
        return ResponseEntity.ok(
            portfolioService.calculateValuation(jwt, portfolioId)
        );
    }

    @GetMapping("/insights/overview")
    public ResponseEntity<Map<String, Object>> getOverview(
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(portfolioService.getUserPortfolioOverview(jwt.getSubject()));
    }

    @GetMapping("/insights/exposure")
    public ResponseEntity<List<Map<String, Object>>> getExposure(
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(portfolioService.getUserSymbolExposure(jwt.getSubject()));
    }

    @GetMapping("/insights/top-movers")
    public ResponseEntity<List<Map<String, Object>>> getTopMovers(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(portfolioService.getTopMoversForUser(jwt.getSubject(), limit));
    }

    @GetMapping("/insights/buy-timeline")
    public ResponseEntity<List<Map<String, Object>>> getBuyTimeline(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(portfolioService.getBuyActivityTimeline(jwt.getSubject(), days));
    }

    @GetMapping("/insights/diversified")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getMostDiversified(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(portfolioService.getMostDiversifiedPortfolios(limit));
    }

    @GetMapping("/insights/deleted")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getRecentlyDeleted(
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(portfolioService.getRecentSoftDeletedPortfolios(days));
    }


}

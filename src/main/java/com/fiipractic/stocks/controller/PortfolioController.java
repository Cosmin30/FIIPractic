package com.fiipractic.stocks.controller;

import com.fiipractic.stocks.dto.*;
import com.fiipractic.stocks.service.PortfolioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
@RestController
@RequestMapping("/api/portfolios")
public class PortfolioController {

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
        return ResponseEntity.ok(portfolioService.buyStock(jwt.getSubject(), portfolioId, request));
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
        return ResponseEntity.ok(
                portfolioService.refreshPortfolioPrices(jwt.getSubject(), portfolioId)
        );
    }
    @GetMapping("/{portfolioId}/valuation")
    public ResponseEntity<PortfolioValuationDTO> getPortfolioValuation(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long portfolioId) {
        return ResponseEntity.ok(
                portfolioService.calculateValuation(jwt.getSubject(), portfolioId)
        );
    }


}

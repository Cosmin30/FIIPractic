package com.fiipractic.stocks.controller;
import com.fiipractic.stocks.dto.PriceRefreshResponseDTO;
import com.fiipractic.stocks.dto.StockDTO;
import com.fiipractic.stocks.service.StockService;
import com.fiipractic.stocks.service.PriceRefreshPublisher;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Map;
import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("/api/stocks")
public class StockController {

    private static final Logger log = LoggerFactory.getLogger(StockController.class);

    private final StockService stockService;
    private final PriceRefreshPublisher priceRefreshPublisher;

    public StockController(StockService stockService, PriceRefreshPublisher priceRefreshPublisher) {
        this.stockService = stockService;
        this.priceRefreshPublisher = priceRefreshPublisher;
    }

    @PostMapping
    public ResponseEntity<StockDTO> createStock(@RequestParam @NotBlank String symbol) {
        return ResponseEntity.status(HttpStatus.CREATED).body(stockService.createStock(symbol));
    }

    @GetMapping
    public ResponseEntity<List<StockDTO>> getAllStocks() {
        return ResponseEntity.ok(stockService.getAllStocks());
    }

    @GetMapping("/insights/stale")
    public ResponseEntity<List<Map<String, Object>>> getStaleStocks(
            @RequestParam(defaultValue = "30") int minutes) {
        return ResponseEntity.ok(stockService.getStaleStocks(minutes));
    }

    @GetMapping("/insights/most-held")
    public ResponseEntity<List<Map<String, Object>>> getMostHeldStocks(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(stockService.getMostHeldStocks(limit));
    }

    @GetMapping("/insights/watchlist")
    public ResponseEntity<List<Map<String, Object>>> getWatchlistCandidates(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(stockService.getWatchlistCandidates(jwt.getSubject(), limit));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StockDTO> getStock(@PathVariable Long id) {
        return ResponseEntity.ok(stockService.getStockById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<StockDTO> updateStock(@PathVariable Long id,
                                                @RequestParam @NotBlank String symbol) {
        return ResponseEntity.ok(stockService.updateStock(id, symbol));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStock(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {
        stockService.deleteStock(id, jwt);
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/{symbol}/refresh")
    public ResponseEntity<Map<String, String>> refreshPrice(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String symbol) {
        String userId = jwt.getSubject();
        String correlationId = UUID.randomUUID().toString();

        // Log with structured fields using MDC
        try {
            MDC.put("action", "refresh_requested");
            MDC.put("symbol", symbol.toUpperCase());
            MDC.put("userId", userId);
            MDC.put("correlationId", correlationId);
            log.info("Price refresh requested for {}", symbol.toUpperCase());
        } finally {
            MDC.clear();  // Always clear to prevent field leakage
        }

        priceRefreshPublisher.publishRefresh(symbol, userId, correlationId);

        return ResponseEntity.accepted()
                .body(Map.of(
                        "status", "QUEUED",
                        "symbol", symbol.toUpperCase(),
                        "message", "Price refresh request queued",
                        "correlationId", correlationId
                ));
    }
    @PostMapping("/refresh")
    public ResponseEntity<PriceRefreshResponseDTO> refreshAllPrices(
            @AuthenticationPrincipal Jwt jwt) {

        log.info("[API] Queue refresh for all symbols by user [{}]", jwt.getSubject());
        priceRefreshPublisher.publishRefreshAll(jwt.getSubject());

        return ResponseEntity.accepted()
                .body(new PriceRefreshResponseDTO(
                        "QUEUED",
                        null,
                        "Price refresh queued for all stocks"
                ));
    }

}


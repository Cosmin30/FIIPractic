package com.fiipractic.stocks.controller;
import com.fiipractic.stocks.dto.PriceRefreshResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fiipractic.stocks.dto.StockDTO;
import com.fiipractic.stocks.service.StockService;
import com.fiipractic.stocks.service.PriceRefreshPublisher;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<Void> deleteStock(@PathVariable Long id) {
        stockService.deleteStock(id);
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/{symbol}/refresh")
    public ResponseEntity<PriceRefreshResponseDTO> refreshPrice(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String symbol) {

        log.info("[API] Queue refresh for symbol [{}] by user [{}]", symbol, jwt.getSubject());
        priceRefreshPublisher.publishRefresh(symbol, jwt.getSubject());

        return ResponseEntity.accepted()
                .body(new PriceRefreshResponseDTO(
                        "QUEUED",
                        symbol.toUpperCase(),
                        "Price refresh request queued"
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


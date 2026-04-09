package com.fiipractic.stocks.service;

import com.fiipractic.stocks.dto.StockDTO;
import com.fiipractic.stocks.exception.StockAlreadyExistsException;
import com.fiipractic.stocks.exception.StockInUseException;
import com.fiipractic.stocks.exception.StockNotFoundException;
import com.fiipractic.stocks.model.Stock;
import com.fiipractic.stocks.repository.PortfolioHoldingRepository;
import com.fiipractic.stocks.repository.StockRepository;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StockService {

    private final StockRepository stockRepository;
    private final PortfolioHoldingRepository portfolioHoldingRepository;
    private final AlphaVantageClient alphaVantageClient;

    public StockService(StockRepository stockRepository,
                        PortfolioHoldingRepository portfolioHoldingRepository,
                        AlphaVantageClient alphaVantageClient) {
        this.stockRepository = stockRepository;
        this.portfolioHoldingRepository = portfolioHoldingRepository;
        this.alphaVantageClient = alphaVantageClient;
    }

    @Transactional
    public StockDTO createStock(String symbol) {
        String normalized = normalize(symbol);
        if (stockRepository.findBySymbol(normalized).isPresent()) {
            throw new StockAlreadyExistsException("Stock with symbol '" + normalized + "' already exists");
        }
        Stock saved = stockRepository.save(Stock.builder().symbol(normalized).build());
        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<StockDTO> getAllStocks() {
        return stockRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StockDTO getStockById(Long id) {
        Stock stock = stockRepository.findById(id)
                .orElseThrow(() -> new StockNotFoundException("Stock not found with id: " + id));
        return toDTO(stock);
    }
    @Transactional
    public StockDTO refreshPrice(String symbol) {
        String normalized = normalize(symbol);

        Stock stock = stockRepository.findBySymbol(normalized)
                .orElseThrow(() -> new StockNotFoundException("Stock not found: " + normalized));

        // Call Alpha Vantage API directly
        BigDecimal price = alphaVantageClient.fetchLatestPrice(normalized);

        // Update the stock
        stock.setCurrentPrice(price);
        stock.setLastPriceUpdate(LocalDateTime.now());

        return toDTO(stockRepository.save(stock));
    }
    @Transactional(readOnly = true)
    public StockDTO getStockBySymbol(String symbol) {
        Stock stock = stockRepository.findBySymbol(normalize(symbol))
                .orElseThrow(() -> new StockNotFoundException("Stock not found with symbol: " + symbol));
        return toDTO(stock);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getStaleStocks(int minutes) {
        LocalDateTime staleBefore = LocalDateTime.now().minusMinutes(Math.max(minutes, 1));
        return stockRepository.findStalePriceStocks(staleBefore)
                .stream()
                .map(row -> {
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("stockId", asLong(row[0]));
                    result.put("symbol", asString(row[1]));
                    result.put("currentPrice", asBigDecimal(row[2]));
                    result.put("lastPriceUpdate", asDateTime(row[3]));
                    return result;
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMostHeldStocks(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        return stockRepository.findMostHeldStocks(PageRequest.of(0, safeLimit))
                .stream()
                .map(row -> {
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("symbol", asString(row[0]));
                    result.put("holdingCount", asLong(row[1]));
                    result.put("portfolioCount", asLong(row[2]));
                    result.put("totalQuantity", asLong(row[3]));
                    result.put("currentPrice", asBigDecimal(row[4]));
                    return result;
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getWatchlistCandidates(String userId, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        return stockRepository.findWatchlistCandidatesForUser(userId, PageRequest.of(0, safeLimit))
                .stream()
                .map(row -> {
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("symbol", asString(row[0]));
                    result.put("buys", asLong(row[1]));
                    result.put("totalQuantity", asLong(row[2]));
                    result.put("lastBuyAt", asDateTime(row[3]));
                    result.put("currentPrice", asBigDecimal(row[4]));
                    return result;
                })
                .toList();
    }

    @Transactional
    public StockDTO updateStock(Long id, String symbol) {
        String normalized = normalize(symbol);

        Stock stock = stockRepository.findById(id)
                .orElseThrow(() -> new StockNotFoundException("Stock not found with id: " + id));

        if (stock.getSymbol().equalsIgnoreCase(normalized)) {
            return toDTO(stock);
        }

        stockRepository.findBySymbol(normalized)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new StockAlreadyExistsException("Stock with symbol '" + normalized + "' already exists");
                });

        stock.setSymbol(normalized);
        return toDTO(stockRepository.save(stock));
    }

    @Transactional
    public void deleteStock(Long id, Jwt jwt) {
        if (!stockRepository.existsById(id)) {
            throw new StockNotFoundException("Stock not found with id: " + id);
        }

        String userId = jwt.getSubject();

        portfolioHoldingRepository.deleteByStockIdAndPortfolioDeletedTrue(id);
        portfolioHoldingRepository.deleteByStockIdAndPortfolioUserIdAndPortfolioDeletedFalse(id, userId);

        long remainingActiveUsages = portfolioHoldingRepository.countByStockIdAndPortfolioDeletedFalse(id);
        if (remainingActiveUsages > 0) {
            portfolioHoldingRepository.deleteByStockIdAndPortfolioDeletedFalse(id);
            remainingActiveUsages = portfolioHoldingRepository.countByStockIdAndPortfolioDeletedFalse(id);
        }

        if (remainingActiveUsages > 0) {
            throw new StockInUseException("Simbolul este folosit in alte portofolii active si nu poate fi sters.");
        }

        stockRepository.deleteById(id);
    }

    Stock findOrCreate(String symbol) {
        String normalized = normalize(symbol);
        return stockRepository.findBySymbol(normalized)
                .orElseGet(() -> stockRepository.save(Stock.builder().symbol(normalized).build()));
    }

    private String normalize(String symbol) {
        return symbol == null ? "" : symbol.trim().toUpperCase();
    }

    private StockDTO toDTO(Stock s) {
    return new StockDTO(s.getId(), s.getSymbol(), s.getCurrentPrice(), s.getLastPriceUpdate());

    }

    private long asLong(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private BigDecimal asBigDecimal(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return BigDecimal.ZERO;
    }

    private String asString(Object value) {
        return value == null ? "" : value.toString();
    }

    private LocalDateTime asDateTime(Object value) {
        if (value instanceof LocalDateTime dateTime) {
            return dateTime;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        return null;
    }
}

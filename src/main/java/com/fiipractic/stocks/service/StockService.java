package com.fiipractic.stocks.service;

import com.fiipractic.stocks.dto.StockDTO;
import com.fiipractic.stocks.exception.StockAlreadyExistsException;
import com.fiipractic.stocks.exception.StockNotFoundException;
import com.fiipractic.stocks.model.Stock;
import com.fiipractic.stocks.repository.StockRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StockService {

    private final StockRepository stockRepository;
    private final AlphaVantageClient alphaVantageClient;

    public StockService(StockRepository stockRepository, AlphaVantageClient alphaVantageClient) {
        this.stockRepository = stockRepository;
        this.alphaVantageClient = alphaVantageClient;
    }

    @Transactional
    public StockDTO createStock(String symbol) {
        String normalized = symbol.toUpperCase();
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
        String normalized = symbol.toUpperCase();

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
        Stock stock = stockRepository.findBySymbol(symbol.toUpperCase()).orElseThrow(() -> new StockNotFoundException("Stock not found with symbol: " + symbol));
        return toDTO(stock);
    }

    @Transactional
    public StockDTO updateStock(Long id, String symbol) {
        Stock stock = stockRepository.findById(id)
                .orElseThrow(() -> new StockNotFoundException("Stock not found with id: " + id));
        stock.setSymbol(symbol.toUpperCase());
        return toDTO(stockRepository.save(stock));
    }

    @Transactional
    public void deleteStock(Long id) {
        stockRepository.deleteById(id);
    }

    Stock findOrCreate(String symbol) {
        String normalized = symbol.toUpperCase();
        return stockRepository.findBySymbol(normalized)
                .orElseGet(() -> stockRepository.save(Stock.builder().symbol(normalized).build()));
    }

    private StockDTO toDTO(Stock s) {
    return new StockDTO(s.getId(), s.getSymbol(), s.getCurrentPrice(), s.getLastPriceUpdate());

    }
}

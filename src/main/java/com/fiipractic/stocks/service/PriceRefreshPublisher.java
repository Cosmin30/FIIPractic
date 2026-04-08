package com.fiipractic.stocks.service;

import com.fiipractic.stocks.config.RabbitMQConfig;
import com.fiipractic.stocks.dto.PriceRefreshMessage;
import com.fiipractic.stocks.dto.StockDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PriceRefreshPublisher {

    private static final Logger log = LoggerFactory.getLogger(PriceRefreshPublisher.class);

    private final StockService stockService;
    private final RabbitTemplate rabbitTemplate;

    public PriceRefreshPublisher(StockService stockService, RabbitTemplate rabbitTemplate) {
        this.stockService = stockService;
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishRefresh(String symbol, String requestedBy, String correlationId) {
        PriceRefreshMessage message = new PriceRefreshMessage(
                symbol.toUpperCase(),
                LocalDateTime.now(),
                requestedBy,
                correlationId
        );

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.PRICE_EXCHANGE,
                RabbitMQConfig.ROUTING_KEY,
                message
        );

        log.info("[PRODUCER] Queued price refresh for [{}] by user [{}] - correlationId: {}",
                symbol, requestedBy, correlationId);

        // Log with MDC for structured output
        try {
            MDC.put("action", "price_queued");
            MDC.put("symbol", symbol.toUpperCase());
            MDC.put("requestedBy", requestedBy);
            MDC.put("correlationId", correlationId);
            log.info("Price refresh queued for {}", symbol.toUpperCase());
        } finally {
            MDC.clear();
        }
    }

    public void publishRefreshAll(String requestedBy) {
        List<StockDTO> stocks = stockService.getAllStocks();

        for (StockDTO stock : stocks) {
            String correlationId = UUID.randomUUID().toString();
            publishRefresh(stock.symbol(), requestedBy, correlationId);
        }

        log.info("[PRODUCER] Queued price refresh for ALL {} stocks", stocks.size());
    }
}

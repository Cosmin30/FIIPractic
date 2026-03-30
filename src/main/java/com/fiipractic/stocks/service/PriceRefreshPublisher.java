package com.fiipractic.stocks.service;

import com.fiipractic.stocks.config.RabbitMQConfig;
import com.fiipractic.stocks.dto.PriceRefreshMessage;
import com.fiipractic.stocks.dto.StockDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PriceRefreshPublisher {

    private static final Logger log = LoggerFactory.getLogger(PriceRefreshPublisher.class);

    private final StockService stockService;
    private final RabbitTemplate rabbitTemplate;

    public PriceRefreshPublisher(StockService stockService, RabbitTemplate rabbitTemplate) {
        this.stockService = stockService;
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishRefresh(String symbol, String requestedBy) {
        PriceRefreshMessage message = new PriceRefreshMessage(
                symbol.toUpperCase(),
                LocalDateTime.now(),
                requestedBy
        );

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.PRICE_EXCHANGE,
                RabbitMQConfig.ROUTING_KEY,
                message
        );

        log.info("[PRODUCER] Queued price refresh for [{}] by user [{}]", symbol, requestedBy);
    }

    public void publishRefreshAll(String requestedBy) {
        List<StockDTO> stocks = stockService.getAllStocks();

        for (StockDTO stock : stocks) {
            publishRefresh(stock.symbol(), requestedBy);
        }

        log.info("[PRODUCER] Queued price refresh for ALL {} stocks", stocks.size());
    }
}

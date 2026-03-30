package com.fiipractic.stocks.consumer;

import com.fiipractic.stocks.config.RabbitMQConfig;
import com.fiipractic.stocks.dto.PriceRefreshMessage;
import com.fiipractic.stocks.exception.StockNotFoundException;
import com.fiipractic.stocks.model.Stock;
import com.fiipractic.stocks.repository.StockRepository;
import com.fiipractic.stocks.service.AlphaVantageClient;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class PriceRefreshConsumer {

    private static final Logger log = LoggerFactory.getLogger(PriceRefreshConsumer.class);

    private final AlphaVantageClient alphaVantageClient;
    private final StockRepository stockRepository;

    public PriceRefreshConsumer(AlphaVantageClient alphaVantageClient,
                                StockRepository stockRepository) {
        this.alphaVantageClient = alphaVantageClient;
        this.stockRepository = stockRepository;
    }

    @RabbitListener(
            queues = RabbitMQConfig.PRICE_REFRESH_QUEUE,
            concurrency = "1",
            ackMode = "MANUAL"
    )
    public void onPriceRefreshRequest(
            PriceRefreshMessage message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {

        String symbol = message.symbol().toUpperCase();
        String requestedBy = message.requestedBy();

        log.info(
                "[CONSUMER] Received refresh request for [{}] by [{}] on thread [{}]",
                symbol,
                requestedBy,
                Thread.currentThread().getName()
        );

        try {
            Thread.sleep(1000);

            BigDecimal price = alphaVantageClient.fetchLatestPrice(symbol);

            Stock stock = stockRepository.findBySymbol(symbol)
                    .orElseThrow(() -> new StockNotFoundException("Stock not found: " + symbol));

            stock.setCurrentPrice(price);
            stock.setLastPriceUpdate(LocalDateTime.now());
            stockRepository.save(stock);

            log.info("[CONSUMER] Updated stock [{}] with latest price [{}]", symbol, price);

            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("[CONSUMER] Failed to fetch price for [{}]: {}", message.symbol(), e.getMessage());
            // nack without requeue prevents poison messages from looping forever
            channel.basicNack(deliveryTag, false, false);
        }
    }
    @RabbitListener(queues = RabbitMQConfig.DLQ_NAME)
    public void onDeadLetter(PriceRefreshMessage message) {
        log.warn("[DLQ] Dead-lettered message — symbol: [{}], requestedBy: [{}], at: {}",
                message.symbol(), message.requestedBy(), message.requestedAt());
    }
}

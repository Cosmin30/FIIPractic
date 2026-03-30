package com.fiipractic.stocks.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

@Service
public class AlphaVantageClient {

    private static final Logger log = LoggerFactory.getLogger(AlphaVantageClient.class);

    @Value("${alphavantage.base-url}")
    private String baseUrl;

    @Value("${alphavantage.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Fetches the most recent daily closing price for the given symbol.
     */
    public BigDecimal fetchLatestPrice(String symbol) {
        String url = baseUrl + "?function=TIME_SERIES_DAILY"
                + "&symbol=" + symbol
                + "&outputsize=compact"
                + "&apikey=" + apiKey;

        log.info("[ALPHA_VANTAGE] Calling API for symbol [{}]", symbol);

        Map<String, Object> response = restTemplate.getForObject(url, Map.class);

        if (response == null) {
            throw new RuntimeException("Empty response from Alpha Vantage for: " + symbol);
        }

        // Check for rate limit errors
        if (response.containsKey("Information") || response.containsKey("Note")) {
            String message = (String) response.getOrDefault("Information",
                    response.get("Note"));
            throw new RuntimeException("Alpha Vantage rate limit: " + message);
        }

        // Parse the response
        Map<String, Object> timeSeries = (Map<String, Object>) response.get("Time Series (Daily)");
        if (timeSeries == null || timeSeries.isEmpty()) {
            throw new RuntimeException("No time series data for symbol: " + symbol);
        }

        // Get the most recent day's data (first key)
        String latestDate = timeSeries.keySet().iterator().next();
        Map<String, String> latestBar = (Map<String, String>) timeSeries.get(latestDate);
        String closePrice = latestBar.get("4. close");

        log.info("[ALPHA_VANTAGE] Fetched price for [{}]: ${} (date: {})",
                symbol, closePrice, latestDate);

        return new BigDecimal(closePrice);
    }
}
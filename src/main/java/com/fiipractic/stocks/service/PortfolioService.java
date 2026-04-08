package com.fiipractic.stocks.service;

import com.fiipractic.stocks.exception.PortfolioLimitException;
import com.fiipractic.stocks.exception.UnauthorizedException;
import com.fiipractic.stocks.dto.BuyStockRequest;
import com.fiipractic.stocks.dto.CreatePortfolioRequest;
import com.fiipractic.stocks.dto.HoldingDTO;
import com.fiipractic.stocks.dto.PortfolioValuationDTO;
import com.fiipractic.stocks.dto.PortfolioDTO;
import com.fiipractic.stocks.dto.PositionSummaryDTO;
import com.fiipractic.stocks.dto.RefreshResponseDTO;
import com.fiipractic.stocks.exception.PortfolioNotFoundException;
import com.fiipractic.stocks.model.Portfolio;
import com.fiipractic.stocks.model.PortfolioHolding;
import com.fiipractic.stocks.model.Stock;
import com.fiipractic.stocks.exception.UserNotOwnerOfPortfolioException;
import com.fiipractic.stocks.repository.PortfolioRepository;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
public class PortfolioService {
    private static final long FREE_USER_PORTFOLIO_LIMIT = 3L;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final PortfolioRepository portfolioRepository;
    private final HoldingService holdingService;
    private final StockService stockService;
    private final PriceRefreshPublisher priceRefreshPublisher;

    public PortfolioService(PortfolioRepository portfolioRepository,
                            HoldingService holdingService,
                            StockService stockService,
                            PriceRefreshPublisher priceRefreshPublisher) {
        this.portfolioRepository = portfolioRepository;
        this.holdingService = holdingService;
        this.stockService = stockService;
        this.priceRefreshPublisher = priceRefreshPublisher;
    }

    @Transactional
    public PortfolioDTO createPortfolio(Jwt jwt, CreatePortfolioRequest request) {
        String userId = jwt.getSubject();
        long count = portfolioRepository.countByUserIdAndDeletedFalse(userId);
        if (count >= FREE_USER_PORTFOLIO_LIMIT && !hasAnyRole(jwt, "PREMIUM", "ADMIN")) {
            throw new PortfolioLimitException("Free users can only create 3 portfolios");
        }

        Portfolio portfolio = Portfolio.builder()
                .name(request.getName())
                .description(request.getDescription())
                .userId(userId)
                .holdings(new ArrayList<>())
                .build();

        return toDTO(portfolioRepository.save(portfolio));
    }

    @Transactional(readOnly = true)
    public List<PortfolioDTO> getUserPortfolios(String userId) {
        return portfolioRepository.findByUserIdAndDeletedFalse(userId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PortfolioDTO> getAllPortfolios(boolean includeDeleted) {
        List<Portfolio> portfolios = includeDeleted
                ? portfolioRepository.findAll()
                : portfolioRepository.findAllByDeletedFalse();

        return portfolios
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public PortfolioDTO buyStock(Jwt jwt, Long portfolioId, BuyStockRequest request) {
        Portfolio portfolio = getOwnedPortfolio(jwt.getSubject(), portfolioId);

        Stock stock = stockService.findOrCreate(request.getSymbol());

        PortfolioHolding holding = PortfolioHolding.builder()
                .portfolio(portfolio)
                .stock(stock)
                .quantity(request.getQuantity())
                .purchasePrice(request.getPurchasePrice())
                .build();

        holdingService.saveHolding(holding);
        portfolio.getHoldings().add(holding);
        return toDTO(portfolio);
    }

    @Transactional
    public PortfolioDTO sellHolding(Jwt jwt, Long portfolioId, Long holdingId) {
        String userId = jwt.getSubject();
        getOwnedPortfolio(userId, portfolioId);

        holdingService.deleteHoldingFromPortfolio(holdingId, portfolioId);

        Portfolio refreshedPortfolio = getOwnedPortfolio(userId, portfolioId);
        return toDTO(refreshedPortfolio);
    }

    @Transactional
    public PortfolioDTO sellHoldings(Jwt jwt, Long portfolioId, List<Long> holdingIds) {
        String userId = jwt.getSubject();
        getOwnedPortfolio(userId, portfolioId);

        long deletedCount = holdingService.deleteHoldingsFromPortfolio(holdingIds, portfolioId);
        if (deletedCount == 0) {
            throw new PortfolioNotFoundException("Holdings not found in portfolio: " + portfolioId);
        }

        Portfolio refreshedPortfolio = getOwnedPortfolio(userId, portfolioId);
        return toDTO(refreshedPortfolio);
    }

    @Transactional
    public void deletePortfolio(Jwt jwt, Long portfolioId) {
        Portfolio portfolio = portfolioRepository.findByIdAndDeletedFalse(portfolioId)
                .orElseThrow(() -> new PortfolioNotFoundException("Portfolio not found: " + portfolioId));

        boolean isOwner = portfolio.getUserId().equals(jwt.getSubject());
        boolean isAdmin = hasAnyRole(jwt, "ADMIN");
        if (!isOwner && !isAdmin) {
            throw new UnauthorizedException("Portfolio not found or access denied");
        }

        portfolio.setDeleted(true);
        portfolio.setDeletedBy(jwt.getSubject());
        portfolio.setDeletedAt(LocalDateTime.now());
    }

    @Transactional
    public RefreshResponseDTO refreshPortfolioPrices(Jwt jwt, Long portfolioId) {
        String userId = jwt.getSubject();
        Portfolio portfolio = getAccessiblePortfolio(jwt, portfolioId);

        List<String> symbols = portfolio.getHoldings().stream()
                .map(h -> h.getStock().getSymbol())
                .distinct()
                .toList();

        symbols.forEach(symbol ->
                priceRefreshPublisher.publishRefresh(symbol, userId, UUID.randomUUID().toString())
        );
        return new RefreshResponseDTO(
                portfolioId.toString(),
                symbols,
                symbols.size(),
                "Price refresh queued for " + symbols.size() + " stocks"
        );
    }

    @Transactional(readOnly = true)
    public PortfolioValuationDTO calculateValuation(Jwt jwt, Long portfolioId) {
        Portfolio portfolio = getAccessiblePortfolio(jwt, portfolioId);

        Map<String, List<PortfolioHolding>> holdingsBySymbol = portfolio.getHoldings().stream()
                .collect(Collectors.groupingBy(h -> h.getStock().getSymbol()));

        List<PositionSummaryDTO> positions = holdingsBySymbol.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    String symbol = entry.getKey();
                    List<PortfolioHolding> holdings = entry.getValue();

                    int totalQuantity = holdings.stream()
                            .mapToInt(PortfolioHolding::getQuantity)
                            .sum();

                    BigDecimal totalCost = holdings.stream()
                            .map(h -> h.getPurchasePrice().multiply(BigDecimal.valueOf(h.getQuantity())))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal quantityDecimal = BigDecimal.valueOf(totalQuantity);
                    BigDecimal avgPrice = totalQuantity == 0
                            ? BigDecimal.ZERO
                            : totalCost.divide(quantityDecimal, 2, RoundingMode.HALF_UP);

                    BigDecimal currentPrice = holdings.isEmpty() ? null : holdings.getFirst().getStock().getCurrentPrice();
                    BigDecimal invested = avgPrice.multiply(quantityDecimal);
                    BigDecimal currentValue = currentPrice != null
                            ? currentPrice.multiply(quantityDecimal)
                            : invested;

                    BigDecimal profitLoss = currentValue.subtract(invested);
                    BigDecimal profitLossPercent = calculateProfitLossPercent(profitLoss, invested);

                    return new PositionSummaryDTO(
                            symbol,
                            totalQuantity,
                            avgPrice,
                            currentPrice,
                            invested,
                            currentValue,
                            profitLoss,
                            profitLossPercent
                    );
                })
                .toList();

        BigDecimal totalInvested = positions.stream()
                .map(PositionSummaryDTO::invested)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCurrentValue = positions.stream()
                .map(PositionSummaryDTO::currentValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalProfitLoss = totalCurrentValue.subtract(totalInvested);
        BigDecimal totalProfitLossPercent = calculateProfitLossPercent(totalProfitLoss, totalInvested);

        return new PortfolioValuationDTO(
                portfolio.getId(),
                portfolio.getName(),
                totalInvested,
                totalCurrentValue,
                totalProfitLoss,
                totalProfitLossPercent,
                positions,
                LocalDateTime.now()
        );
    }

    private BigDecimal calculateProfitLossPercent(BigDecimal profitLoss, BigDecimal invested) {
        if (invested.signum() == 0) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }

        return profitLoss
                .divide(invested, 8, RoundingMode.HALF_UP)
                .multiply(ONE_HUNDRED)
                .setScale(4, RoundingMode.HALF_UP);
    }

    private PortfolioDTO toDTO(Portfolio p) {
        PortfolioDTO dto = new PortfolioDTO();
        dto.setId(p.getId());
        dto.setName(p.getName());
        dto.setDescription(p.getDescription());
        dto.setCreatedAt(p.getCreatedAt());
        dto.setDeleted(p.getDeleted());
        dto.setDeletedBy(p.getDeletedBy());
        dto.setDeletedAt(p.getDeletedAt());
        dto.setHoldings(p.getHoldings().stream().map(this::toHoldingDTO).collect(Collectors.toList()));
        return dto;
    }

    private HoldingDTO toHoldingDTO(PortfolioHolding h) {
        return new HoldingDTO(
                h.getId(),
                h.getStock().getSymbol(),
                h.getQuantity(),
                h.getPurchasePrice(),
                h.getPurchasedAt()
        );
    }

    private boolean hasAnyRole(Jwt jwt, String... allowedRoles) {
        Set<String> userRoles = extractRoles(jwt);
        for (String allowedRole : allowedRoles) {
            if (userRoles.contains(allowedRole.toUpperCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private Portfolio getOwnedPortfolio(String userId, Long portfolioId) {
        return portfolioRepository.findByIdAndDeletedFalse(portfolioId)
                .filter(p -> p.getUserId().equals(userId))
                .orElseThrow(() -> new UnauthorizedException("Portfolio not found or access denied"));
    }

    private Portfolio getAccessiblePortfolio(Jwt jwt, Long portfolioId) {
        Portfolio portfolio = portfolioRepository.findByIdAndDeletedFalse(portfolioId)
                .orElseThrow(() -> new UserNotOwnerOfPortfolioException("Portfolio not found or access denied"));

        String userId = jwt.getSubject();
        if (portfolio.getUserId().equals(userId)) {
            return portfolio;
        }
        if (hasAnyRole(jwt, "ADMIN")) {
            return portfolio;
        }
        throw new UserNotOwnerOfPortfolioException("Portfolio not found or access denied");
    }

    private Set<String> extractRoles(Jwt jwt) {
        Object realmAccessObj = jwt.getClaim("realm_access");
        if (!(realmAccessObj instanceof Map<?, ?> realmAccess)) {
            return Set.of();
        }

        Object rolesObj = realmAccess.get("roles");
        if (!(rolesObj instanceof Collection<?> rawRoles)) {
            return Set.of();
        }

        return rawRoles.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(role -> role.toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }
}
package com.fiipractic.stocks.service;
import com.fiipractic.stocks.exception.PortfolioLimitException;
import com.fiipractic.stocks.exception.UnauthorizedException;
import com.fiipractic.stocks.dto.BuyStockRequest;
import com.fiipractic.stocks.dto.CreatePortfolioRequest;
import com.fiipractic.stocks.dto.HoldingDTO;
import com.fiipractic.stocks.dto.PortfolioDTO;
import com.fiipractic.stocks.exception.PortfolioNotFoundException;
import com.fiipractic.stocks.model.Portfolio;
import com.fiipractic.stocks.model.PortfolioHolding;
import com.fiipractic.stocks.model.Stock;
import com.fiipractic.stocks.repository.PortfolioHoldingRepository;
import com.fiipractic.stocks.repository.PortfolioRepository;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PortfolioService {
    private static final long FREE_USER_PORTFOLIO_LIMIT = 3L;

    private final PortfolioRepository portfolioRepository;
    private final PortfolioHoldingRepository holdingRepository;
    private final StockService stockService;

    public PortfolioService(PortfolioRepository portfolioRepository,
                           PortfolioHoldingRepository holdingRepository,
                           StockService stockService) {
        this.portfolioRepository = portfolioRepository;
        this.holdingRepository = holdingRepository;
        this.stockService = stockService;
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
    public PortfolioDTO buyStock(String userId, Long portfolioId, BuyStockRequest request) {
        Portfolio portfolio = portfolioRepository.findByIdAndDeletedFalse(portfolioId)
                .filter(p -> p.getUserId().equals(userId))
                .orElseThrow(() -> new UnauthorizedException("Portfolio not found or access denied"));

        Stock stock = stockService.findOrCreate(request.getSymbol());

        PortfolioHolding holding = PortfolioHolding.builder()
                .portfolio(portfolio)
                .stock(stock)
                .quantity(request.getQuantity())
                .purchasePrice(request.getPurchasePrice())
                .build();

        holdingRepository.save(holding);
        portfolio.getHoldings().add(holding);
        return toDTO(portfolio);
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
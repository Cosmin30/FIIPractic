package com.fiipractic.stocks.service;

import com.fiipractic.stocks.exception.PortfolioNotFoundException;
import com.fiipractic.stocks.model.PortfolioHolding;
import com.fiipractic.stocks.repository.PortfolioHoldingRepository;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Service
public class HoldingService {

    private final PortfolioHoldingRepository holdingRepository;

    public HoldingService(PortfolioHoldingRepository holdingRepository) {
        this.holdingRepository = holdingRepository;
    }

    public PortfolioHolding saveHolding(PortfolioHolding holding) {
        return holdingRepository.save(holding);
    }

    public void deleteHoldingFromPortfolio(Long holdingId, Long portfolioId) {
        long deletedCount = holdingRepository.deleteByIdAndPortfolioId(holdingId, portfolioId);
        if (deletedCount == 0) {
            throw new PortfolioNotFoundException("Holding not found in portfolio: " + holdingId);
        }
    }

    public long deleteHoldingsFromPortfolio(Collection<Long> holdingIds, Long portfolioId) {
        if (holdingIds == null || holdingIds.isEmpty()) {
            return 0;
        }
        return holdingRepository.deleteByPortfolioIdAndIdIn(portfolioId, holdingIds);
    }
}



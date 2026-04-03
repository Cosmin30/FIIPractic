package com.fiipractic.stocks.repository;

import com.fiipractic.stocks.model.PortfolioHolding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PortfolioHoldingRepository extends JpaRepository<PortfolioHolding, Long> {
	boolean existsByStockId(Long stockId);

	boolean existsByStockIdAndPortfolioDeletedFalse(Long stockId);

	long countByStockIdAndPortfolioDeletedFalse(Long stockId);

	long deleteByStockIdAndPortfolioUserIdAndPortfolioDeletedFalse(Long stockId, String userId);

	long deleteByStockIdAndPortfolioDeletedFalse(Long stockId);

	long deleteByStockIdAndPortfolioDeletedTrue(Long stockId);

	Optional<PortfolioHolding> findByIdAndPortfolioId(Long id, Long portfolioId);
}

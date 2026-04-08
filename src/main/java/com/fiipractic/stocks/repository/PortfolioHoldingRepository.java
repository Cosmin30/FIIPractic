package com.fiipractic.stocks.repository;

import com.fiipractic.stocks.model.PortfolioHolding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

	long deleteByIdAndPortfolioId(Long id, Long portfolioId);

	@Modifying
	@Query("delete from PortfolioHolding h where h.portfolio.id = :portfolioId and h.id in :holdingIds")
	long deleteByPortfolioIdAndIdIn(@Param("portfolioId") Long portfolioId,
								@Param("holdingIds") java.util.Collection<Long> holdingIds);
}

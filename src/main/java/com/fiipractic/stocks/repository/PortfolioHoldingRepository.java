package com.fiipractic.stocks.repository;

import com.fiipractic.stocks.model.PortfolioHolding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
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
	@Query(value = """
			delete from portfolio_holdings
			where portfolio_id = :portfolioId
			  and id in (:holdingIds)
			""", nativeQuery = true)
	long deleteByPortfolioIdAndIdIn(@Param("portfolioId") Long portfolioId,
								@Param("holdingIds") java.util.Collection<Long> holdingIds);

	@Query(value = """
			select
				s.symbol as symbol,
				coalesce(sum(h.quantity), 0) as totalQuantity,
				coalesce(sum(h.quantity * h.purchase_price), 0) as invested,
				coalesce(sum(h.quantity * coalesce(s.current_price, h.purchase_price)), 0) as currentValue,
				coalesce(sum(h.quantity * coalesce(s.current_price, h.purchase_price)), 0) -
					coalesce(sum(h.quantity * h.purchase_price), 0) as profitLoss,
				case
					when coalesce(sum(h.quantity * h.purchase_price), 0) = 0 then 0
					else ((coalesce(sum(h.quantity * coalesce(s.current_price, h.purchase_price)), 0) -
						coalesce(sum(h.quantity * h.purchase_price), 0))
						/ coalesce(sum(h.quantity * h.purchase_price), 0)) * 100
				end as profitLossPercent
			from portfolio_holdings h
			join portfolios p on p.id = h.portfolio_id
			join stocks s on s.id = h.stock_id
			where p.deleted = false
			  and p.user_id = :userId
			group by s.symbol
			order by currentValue desc
			""", nativeQuery = true)
	List<Object[]> findUserSymbolExposure(@Param("userId") String userId);

	@Query(value = """
			select
				s.symbol as symbol,
				coalesce(sum(h.quantity), 0) as totalQuantity,
				coalesce(avg(h.purchase_price), 0) as averageBuyPrice,
				coalesce(max(s.current_price), 0) as currentPrice,
				case
					when coalesce(avg(h.purchase_price), 0) = 0 then 0
					else ((coalesce(max(s.current_price), 0) - coalesce(avg(h.purchase_price), 0))
						/ coalesce(avg(h.purchase_price), 0)) * 100
				end as movePercent
			from portfolio_holdings h
			join portfolios p on p.id = h.portfolio_id
			join stocks s on s.id = h.stock_id
			where p.deleted = false
			  and p.user_id = :userId
			group by s.symbol
			order by abs(
				case
					when coalesce(avg(h.purchase_price), 0) = 0 then 0
					else ((coalesce(max(s.current_price), 0) - coalesce(avg(h.purchase_price), 0))
						/ coalesce(avg(h.purchase_price), 0)) * 100
				end
			) desc
			""", nativeQuery = true)
	List<Object[]> findTopMoversForUser(@Param("userId") String userId, Pageable pageable);

	@Query(value = """
			select
				date_trunc('day', h.purchased_at)::date as day,
				count(h.id)::bigint as trades,
				coalesce(sum(h.quantity), 0) as totalQuantity,
				coalesce(sum(h.quantity * h.purchase_price), 0) as invested
			from portfolio_holdings h
			join portfolios p on p.id = h.portfolio_id
			where p.deleted = false
			  and p.user_id = :userId
			  and h.purchased_at >= :fromDate
			group by date_trunc('day', h.purchased_at)
			order by day asc
			""", nativeQuery = true)
	List<Object[]> findBuyActivityTimeline(@Param("userId") String userId,
									  @Param("fromDate") LocalDateTime fromDate);
}

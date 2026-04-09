package com.fiipractic.stocks.repository;

import com.fiipractic.stocks.model.Stock;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {
    Optional<Stock> findBySymbol(String symbol);

    @Query(value = """
            select
                s.id as stockId,
                s.symbol as symbol,
                s.current_price as currentPrice,
                s.last_price_update as lastPriceUpdate
            from stocks s
            where s.last_price_update is null
               or s.last_price_update < :staleBefore
            order by s.last_price_update nulls first, s.symbol asc
            """, nativeQuery = true)
    List<Object[]> findStalePriceStocks(@Param("staleBefore") LocalDateTime staleBefore);

    @Query(value = """
            select
                s.symbol as symbol,
                count(h.id)::bigint as holdingCount,
                count(distinct p.id)::bigint as portfolioCount,
                sum(h.quantity)::bigint as totalQuantity,
                coalesce(avg(s.current_price), 0) as currentPrice
            from stocks s
            join portfolio_holdings h on h.stock_id = s.id
            join portfolios p on p.id = h.portfolio_id
            where p.deleted = false
            group by s.symbol
            order by holdingCount desc, totalQuantity desc
            """, nativeQuery = true)
    List<Object[]> findMostHeldStocks(Pageable pageable);

    @Query(value = """
            select
                s.symbol as symbol,
                count(h.id)::bigint as buys,
                sum(h.quantity)::bigint as totalQuantity,
                max(h.purchased_at) as lastBuyAt,
                coalesce(max(s.current_price), 0) as currentPrice
            from stocks s
            join portfolio_holdings h on h.stock_id = s.id
            join portfolios p on p.id = h.portfolio_id
            where p.deleted = false
              and p.user_id = :userId
            group by s.symbol
            order by buys desc, lastBuyAt desc
            """, nativeQuery = true)
    List<Object[]> findWatchlistCandidatesForUser(@Param("userId") String userId, Pageable pageable);
}

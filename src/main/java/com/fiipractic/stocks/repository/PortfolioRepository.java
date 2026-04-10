package com.fiipractic.stocks.repository;

import com.fiipractic.stocks.model.Portfolio;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {
    List<Portfolio> findByUserIdAndDeletedFalse(String userId);

    long countByUserIdAndDeletedFalse(String userId);

    List<Portfolio> findAllByDeletedFalse();

    Optional<Portfolio> findByIdAndDeletedFalse(Long id);

    @Query(value = """
            select
                p.user_id as userId,
                count(distinct p.id)::bigint as portfolioCount,
                count(h.id)::bigint as holdingCount,
                coalesce(sum(h.quantity * h.purchase_price), 0) as invested,
                coalesce(sum(h.quantity * coalesce(s.current_price, h.purchase_price)), 0) as currentValue
            from portfolios p
            left join portfolio_holdings h on h.portfolio_id = p.id
            left join stocks s on s.id = h.stock_id
            where p.deleted = false
              and p.user_id = :userId
            group by p.user_id
            """, nativeQuery = true)
    List<Object[]> findUserPortfolioOverview(@Param("userId") String userId);

    @Query(value = """
            select
                p.id as portfolioId,
                p.name as portfolioName,
                p.user_id as userId,
                count(distinct h.id)::bigint as positions,
                count(distinct h.stock_id)::bigint as uniqueSymbols,
                coalesce(sum(h.quantity * coalesce(s.current_price, h.purchase_price)), 0) as marketValue
            from portfolios p
            left join portfolio_holdings h on h.portfolio_id = p.id
            left join stocks s on s.id = h.stock_id
            where p.deleted = false
            group by p.id, p.name, p.user_id
            order by uniqueSymbols desc, marketValue desc
            """, nativeQuery = true)
    List<Object[]> findMostDiversifiedPortfolios(Pageable pageable);

    @Query(value = """
            select
                p.id as portfolioId,
                p.name as portfolioName,
                p.user_id as userId,
                p.deleted_by as deletedBy,
                p.deleted_at as deletedAt
            from portfolios p
            where p.deleted = true
              and p.deleted_at >= :fromDate
            order by p.deleted_at desc
            """, nativeQuery = true)
    List<Object[]> findRecentSoftDeletedPortfolios(@Param("fromDate") LocalDateTime fromDate);
}

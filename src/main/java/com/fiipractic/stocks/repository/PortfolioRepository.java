package com.fiipractic.stocks.repository;

import com.fiipractic.stocks.model.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {
    List<Portfolio> findByUserIdAndDeletedFalse(String userId);

    long countByUserIdAndDeletedFalse(String userId);

    List<Portfolio> findAllByDeletedFalse();

    java.util.Optional<Portfolio> findByIdAndDeletedFalse(Long id);
}

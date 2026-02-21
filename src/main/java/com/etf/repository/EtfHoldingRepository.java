package com.etf.repository;

import com.etf.domain.EtfHolding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface EtfHoldingRepository extends JpaRepository<EtfHolding, Long> {

    @Query("SELECT DISTINCT h.scrapeDate FROM EtfHolding h ORDER BY h.scrapeDate DESC")
    List<LocalDate> findDistinctScrapeDates();

    @Query("SELECT DISTINCT h.etfName FROM EtfHolding h WHERE h.scrapeDate = :date")
    List<String> findDistinctEtfNamesByDate(@Param("date") LocalDate date);

    List<EtfHolding> findByScrapeDate(LocalDate scrapeDate);

    List<EtfHolding> findByScrapeDateAndEtfNameOrderByOrderNo(LocalDate scrapeDate, String etfName);

    boolean existsByScrapeDateAndEtfName(LocalDate scrapeDate, String etfName);

    long countByScrapeDateAndEtfName(LocalDate scrapeDate, String etfName);

    void deleteByScrapeDateAndEtfName(LocalDate scrapeDate, String etfName);
}

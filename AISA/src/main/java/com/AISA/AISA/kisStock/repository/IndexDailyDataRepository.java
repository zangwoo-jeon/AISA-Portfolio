package com.AISA.AISA.kisStock.repository;

import com.AISA.AISA.kisStock.Entity.Index.IndexDailyData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface IndexDailyDataRepository extends JpaRepository<IndexDailyData, Long> {
    Optional<IndexDailyData> findByMarketNameAndDate(String marketName, LocalDate date);
}

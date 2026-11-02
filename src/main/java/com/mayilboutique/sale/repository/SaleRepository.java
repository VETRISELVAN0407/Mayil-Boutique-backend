package com.mayilboutique.sale.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mayilboutique.sale.entity.Sale;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    @Query("SELECT s FROM Sale s WHERE s.active = true AND s.startsAt <= :now AND s.endsAt > :now")
    List<Sale> findRunning(@Param("now") Instant now);

    List<Sale> findAllByOrderByStartsAtDescIdDesc();
}

package com.siddharth.urlshortener.repository;

import com.siddharth.urlshortener.model.ClickEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {
    
    @Query("SELECT c FROM ClickEvent c WHERE c.urlId = :urlId ORDER BY c.timestamp DESC")
    List<ClickEvent> findByUrlIdOrderByTimestampDesc(@Param("urlId") Long urlId);
    
    @Query("SELECT c FROM ClickEvent c WHERE c.urlId = :urlId AND c.timestamp BETWEEN :startTime AND :endTime")
    List<ClickEvent> findClicksInTimeRange(@Param("urlId") Long urlId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    
    @Query("SELECT COUNT(c) FROM ClickEvent c WHERE c.urlId = :urlId")
    long countByUrlId(@Param("urlId") Long urlId);
    
    @Query("SELECT c.referrer, COUNT(c) as count FROM ClickEvent c WHERE c.urlId = :urlId GROUP BY c.referrer ORDER BY count DESC")
    List<Object[]> getTopReferrers(@Param("urlId") Long urlId);
    
    @Query("SELECT c.country, COUNT(c) as count FROM ClickEvent c WHERE c.urlId = :urlId GROUP BY c.country ORDER BY count DESC")
    List<Object[]> getTopCountries(@Param("urlId") Long urlId);
    
    @Query("SELECT c.deviceType, COUNT(c) as count FROM ClickEvent c WHERE c.urlId = :urlId GROUP BY c.deviceType ORDER BY count DESC")
    List<Object[]> getDeviceTypeDistribution(@Param("urlId") Long urlId);
}

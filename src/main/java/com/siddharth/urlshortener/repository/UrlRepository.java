package com.siddharth.urlshortener.repository;

import com.siddharth.urlshortener.model.Url;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UrlRepository extends JpaRepository<Url, Long> {
    Optional<Url> findByShortCode(String shortCode);
    Optional<Url> findByCustomAlias(String customAlias);
    List<Url> findByOwnerId(Long ownerId);
    boolean existsByShortCode(String shortCode);
    boolean existsByCustomAlias(String customAlias);
    
    @Query("SELECT u FROM Url u WHERE u.expiresAt IS NOT NULL AND u.expiresAt < :now AND u.isActive = true")
    List<Url> findExpiredUrls(@Param("now") LocalDateTime now);
    
    @Query("SELECT u FROM Url u WHERE u.ownerId = :ownerId ORDER BY u.createdAt DESC LIMIT :limit OFFSET :offset")
    List<Url> findByOwnerIdPaginated(@Param("ownerId") Long ownerId, @Param("limit") int limit, @Param("offset") int offset);
}

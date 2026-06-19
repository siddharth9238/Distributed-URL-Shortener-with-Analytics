package com.siddharth.urlshortener.service;

import com.siddharth.urlshortener.dto.CreateUrlRequest;
import com.siddharth.urlshortener.dto.UrlResponse;
import com.siddharth.urlshortener.exception.ResourceNotFoundException;
import com.siddharth.urlshortener.exception.ValidationException;
import com.siddharth.urlshortener.model.Url;
import com.siddharth.urlshortener.repository.UrlRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * UrlService: Orchestrator for URL shortening logic.
 * Implements cache-aside pattern: check cache → query DB → populate cache → return.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UrlService {

    private final UrlRepository urlRepository;
    private final Base62Encoder base62Encoder;
    private final LRUCache lruCache;
    private final RateLimiterService rateLimiterService;
    private final AnalyticsService analyticsService;

    /**
     * Creates a new shortened URL.
     * Checks rate limit, validates URL, saves to DB, generates short code, populates cache.
     */
    @Transactional
    public UrlResponse createShortUrl(CreateUrlRequest request, Long userId) {
        // Rate limiting
        if (!rateLimiterService.allowRequest(userId)) {
            throw new ValidationException("Rate limit exceeded. Max 60 URLs per minute.");
        }

        // Validate long URL
        validateUrl(request.getLongUrl());

        // Check for custom alias collision
        if (request.getCustomAlias() != null && !request.getCustomAlias().isEmpty()) {
            if (urlRepository.existsByCustomAlias(request.getCustomAlias())) {
                throw new ValidationException("Custom alias already taken: " + request.getCustomAlias());
            }
        }

        // Save URL to database
        Url url = Url.builder()
                .longUrl(request.getLongUrl())
                .ownerId(userId)
                .customAlias(request.getCustomAlias())
                .description(request.getDescription())
                .category(request.getCategory())
                .expiresAt(request.getExpiresAt())
                .isActive(true)
                .clickCount(0L)
                .build();

        url = urlRepository.save(url);

        // Generate short code via base62 encoding
        String shortCode = base62Encoder.encode(url.getId());
        url.setShortCode(shortCode);
        url = urlRepository.save(url);

        // Populate cache
        lruCache.put(shortCode, url.getLongUrl());

        log.info("Created short URL: {} -> {} for user: {}", shortCode, url.getLongUrl(), userId);

        return UrlResponse.fromEntity(url);
    }

    /**
     * Resolves a short code to its long URL.
     * Cache-aside pattern: cache hit → return quickly, cache miss → query DB → populate cache.
     */
    @Transactional
    public String resolveUrl(String shortCode) {
        // Try cache first (fast path)
        String cachedUrl = lruCache.get(shortCode);
        if (cachedUrl != null) {
            log.debug("Cache hit for short code: {}", shortCode);
            return cachedUrl;
        }

        // Cache miss → query database
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("Short URL not found: " + shortCode));

        // Check expiration
        if (url.getExpiresAt() != null && url.getExpiresAt().isBefore(LocalDateTime.now())) {
            url.setIsActive(false);
            urlRepository.save(url);
            throw new ResourceNotFoundException("Short URL has expired: " + shortCode);
        }

        if (!url.getIsActive()) {
            throw new ResourceNotFoundException("Short URL is inactive: " + shortCode);
        }

        // Populate cache for future requests
        lruCache.put(shortCode, url.getLongUrl());

        log.debug("Cache miss for short code: {}, populated cache and returning URL", shortCode);

        return url.getLongUrl();
    }

    /**
     * Records a click asynchronously and returns the redirect URL.
     */
    public String redirect(String shortCode, HttpServletRequest request) {
        String longUrl = resolveUrl(shortCode);

        analyticsService.recordClick(
                shortCode,
                request.getHeader("referer"),
                request.getHeader("user-agent"),
                getClientIp(request),
                request.getHeader("CloudFront-Viewer-Country")
        );

        return longUrl;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            return request.getRemoteAddr();
        }
        return ip;
    }

    /**
     * Gets all URLs for a user.
     */
    public List<UrlResponse> getUserUrls(Long userId) {
        return urlRepository.findByOwnerId(userId)
                .stream()
                .map(UrlResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Gets a specific URL by short code.
     */
    public UrlResponse getUrlByShortCode(String shortCode) {
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("Short URL not found: " + shortCode));
        return UrlResponse.fromEntity(url);
    }

    /**
     * Deletes a short URL.
     */
    @Transactional
    public void deleteUrl(String shortCode, Long userId) {
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("Short URL not found: " + shortCode));

        if (!url.getOwnerId().equals(userId)) {
            throw new ValidationException("You do not have permission to delete this URL");
        }

        urlRepository.delete(url);
        lruCache.put(shortCode, null); // Invalidate cache
        log.info("Deleted short URL: {} for user: {}", shortCode, userId);
    }

    /**
     * Validates that the URL is well-formed and uses http/https.
     */
    private void validateUrl(String longUrl) {
        if (longUrl == null || longUrl.isEmpty()) {
            throw new ValidationException("URL cannot be empty");
        }

        if (longUrl.length() > 2048) {
            throw new ValidationException("URL is too long (max 2048 characters)");
        }

        try {
            URI uri = new URI(longUrl);
            if (!uri.getScheme().matches("https?")) {
                throw new ValidationException("URL must use http or https protocol");
            }
        } catch (URISyntaxException e) {
            throw new ValidationException("Invalid URL format: " + e.getMessage());
        }
    }
}

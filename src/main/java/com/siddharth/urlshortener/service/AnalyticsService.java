package com.siddharth.urlshortener.service;

import com.siddharth.urlshortener.model.ClickEvent;
import com.siddharth.urlshortener.model.Url;
import com.siddharth.urlshortener.repository.ClickEventRepository;
import com.siddharth.urlshortener.repository.UrlRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final ClickEventRepository clickEventRepository;
    private final UrlRepository urlRepository;

    /**
     * Records a click event asynchronously (fire-and-forget).
     * This doesn't block the redirect response.
     */
    @Async
    @Transactional
    public void recordClick(String shortCode, String referrer, String userAgent, String ipAddress, String countryCode) {
        try {
            Url url = urlRepository.findByShortCode(shortCode)
                    .orElseThrow(() -> new IllegalArgumentException("Short URL not found: " + shortCode));

            String ipHash = hashIpAddress(ipAddress);
            String country = extractCountry(countryCode);
            String deviceType = extractDeviceType(userAgent);

            ClickEvent clickEvent = ClickEvent.builder()
                    .urlId(url.getId())
                    .referrer(referrer)
                    .userAgent(userAgent)
                    .ipHash(ipHash)
                    .country(country)
                    .deviceType(deviceType)
                    .build();

            clickEventRepository.save(clickEvent);
            url.setClickCount(url.getClickCount() + 1);
            urlRepository.save(url);

            log.debug("Recorded click for short code: {}", shortCode);
        } catch (Exception e) {
            log.error("Error recording click for short code: {}", shortCode, e);
        }
    }

    /**
     * Gets total click count for a URL.
     */
    public long getClickCount(Long urlId) {
        return clickEventRepository.countByUrlId(urlId);
    }

    /**
     * Gets clicks in a specific time range.
     */
    public List<ClickEvent> getClicksInRange(Long urlId, LocalDateTime startTime, LocalDateTime endTime) {
        return clickEventRepository.findClicksInTimeRange(urlId, startTime, endTime);
    }

    /**
     * Gets top referrers for a URL.
     */
    public Map<String, Long> getTopReferrers(Long urlId) {
        List<Object[]> results = clickEventRepository.getTopReferrers(urlId);
        Map<String, Long> topReferrers = new LinkedHashMap<>();

        for (Object[] row : results) {
            String referrer = (String) row[0];
            Long count = ((Number) row[1]).longValue();
            topReferrers.put(referrer != null ? referrer : "direct", count);
        }

        return topReferrers;
    }

    /**
     * Gets geographic distribution of clicks.
     */
    public Map<String, Long> getCountryDistribution(Long urlId) {
        List<Object[]> results = clickEventRepository.getTopCountries(urlId);
        Map<String, Long> distribution = new LinkedHashMap<>();

        for (Object[] row : results) {
            String country = (String) row[0];
            Long count = ((Number) row[1]).longValue();
            distribution.put(country != null ? country : "unknown", count);
        }

        return distribution;
    }

    /**
     * Gets device type distribution of clicks.
     */
    public Map<String, Long> getDeviceTypeDistribution(Long urlId) {
        List<Object[]> results = clickEventRepository.getDeviceTypeDistribution(urlId);
        Map<String, Long> distribution = new LinkedHashMap<>();

        for (Object[] row : results) {
            String deviceType = (String) row[0];
            Long count = ((Number) row[1]).longValue();
            distribution.put(deviceType != null ? deviceType : "unknown", count);
        }

        return distribution;
    }

    /**
     * Gets recent clicks for a URL.
     */
    public List<ClickEvent> getRecentClicks(Long urlId, int limit) {
        List<ClickEvent> allClicks = clickEventRepository.findByUrlIdOrderByTimestampDesc(urlId);
        return allClicks.size() > limit ? allClicks.subList(0, limit) : allClicks;
    }

    // Helper methods

    private String hashIpAddress(String ip) {
        // Simple hash - in production, use a proper hashing function
        return ip == null ? "unknown" : String.valueOf(ip.hashCode());
    }

    private String extractCountry(String countryCode) {
        // In production, use a GeoIP database or service
        return countryCode != null ? countryCode : "unknown";
    }

    private String extractDeviceType(String userAgent) {
        if (userAgent == null) {
            return "unknown";
        }

        userAgent = userAgent.toLowerCase();
        if (userAgent.contains("mobile") || userAgent.contains("android")) {
            return "mobile";
        } else if (userAgent.contains("tablet") || userAgent.contains("ipad")) {
            return "tablet";
        } else {
            return "desktop";
        }
    }
}

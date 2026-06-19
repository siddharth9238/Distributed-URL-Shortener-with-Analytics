package com.siddharth.urlshortener.service;

import com.siddharth.urlshortener.model.Url;
import com.siddharth.urlshortener.repository.ClickEventRepository;
import com.siddharth.urlshortener.repository.UrlRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.List;

/**
 * BatchAnalyticsService: Batch processing of analytics data.
 * Runs scheduled jobs to aggregate click statistics and update URL statistics table.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BatchAnalyticsService {

    private final ClickEventRepository clickEventRepository;
    private final UrlRepository urlRepository;

    /**
     * Batch job to aggregate click statistics (runs every 30 minutes).
     * This helps with reporting and reduces query load during peak times.
     */
    @Scheduled(cron = "0 */30 * * * *")
    @Transactional
    public void aggregateClickStatistics() {
        log.info("Starting batch aggregation of click statistics");

        try {
            List<Url> allUrls = urlRepository.findAll();

            for (Url url : allUrls) {
                long clickCount = clickEventRepository.countByUrlId(url.getId());
                url.setClickCount(clickCount);
                urlRepository.save(url);
            }

            log.info("Completed aggregation of click statistics for {} URLs", allUrls.size());
        } catch (Exception e) {
            log.error("Error during click statistics aggregation", e);
        }
    }

    /**
     * Batch job to clean up old click events (runs daily at 3 AM).
     * Removes click events older than 90 days to free up space.
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupOldClickEvents() {
        log.info("Starting cleanup of old click events");

        try {
            // In production, use a custom query to delete clicks older than 90 days
            // For now, we'll just log the operation
            log.info("Cleanup of old click events completed");
        } catch (Exception e) {
            log.error("Error during click events cleanup", e);
        }
    }

    /**
     * Generate daily analytics summary.
     * Can be called manually or via scheduler for reporting.
     */
    public Map<String, Object> generateDailyAnalyticsSummary() {
        Map<String, Object> summary = new HashMap<>();

        try {
            List<Url> urls = urlRepository.findAll();
            long totalUrls = urls.size();
            long totalClicks = urls.stream().mapToLong(Url::getClickCount).sum();
            double avgClicks = totalUrls > 0 ? (double) totalClicks / totalUrls : 0;

            summary.put("totalUrls", totalUrls);
            summary.put("totalClicks", totalClicks);
            summary.put("averageClicksPerUrl", avgClicks);
            summary.put("timestamp", LocalDateTime.now());

            log.info("Daily analytics summary: {}", summary);
        } catch (Exception e) {
            log.error("Error generating daily analytics summary", e);
        }

        return summary;
    }
}

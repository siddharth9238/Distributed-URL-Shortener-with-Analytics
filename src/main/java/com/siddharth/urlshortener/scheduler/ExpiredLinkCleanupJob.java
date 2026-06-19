package com.siddharth.urlshortener.scheduler;

import com.siddharth.urlshortener.model.Url;
import com.siddharth.urlshortener.repository.UrlRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExpiredLinkCleanupJob {

    private final UrlRepository urlRepository;

    /**
     * Scheduled job to clean up expired URLs.
     * Runs daily at 2 AM (configurable via cron in application.yml).
     */
    @Scheduled(cron = "${app.scheduler.cleanup.cron:0 0 2 * * *}")
    @Transactional
    public void cleanupExpiredUrls() {
        log.info("Starting cleanup job for expired URLs");

        try {
            LocalDateTime now = LocalDateTime.now();
            List<Url> expiredUrls = urlRepository.findExpiredUrls(now);

            if (expiredUrls.isEmpty()) {
                log.info("No expired URLs found");
                return;
            }

            // Mark as inactive instead of deleting (soft delete)
            for (Url url : expiredUrls) {
                url.setIsActive(false);
                urlRepository.save(url);
            }

            log.info("Cleanup completed: {} expired URLs marked as inactive", expiredUrls.size());
        } catch (Exception e) {
            log.error("Error during cleanup job", e);
        }
    }
}

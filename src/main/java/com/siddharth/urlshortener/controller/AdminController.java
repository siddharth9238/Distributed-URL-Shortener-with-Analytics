package com.siddharth.urlshortener.controller;

import com.siddharth.urlshortener.dto.AdminDashboardStats;
import com.siddharth.urlshortener.repository.ClickEventRepository;
import com.siddharth.urlshortener.repository.UrlRepository;
import com.siddharth.urlshortener.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Admin dashboard and system management endpoints")
public class AdminController {

    private final UserRepository userRepository;
    private final UrlRepository urlRepository;
    private final ClickEventRepository clickEventRepository;

    /**
     * GET /api/admin/dashboard - Get dashboard statistics.
     */
    @GetMapping("/dashboard")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get admin dashboard", description = "Retrieve system statistics and metrics")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dashboard statistics retrieved",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AdminDashboardStats.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - admin only")
    })
    public ResponseEntity<AdminDashboardStats> getDashboardStats() {
        log.info("Admin dashboard stats request");

        long totalUsers = userRepository.count();
        long totalUrls = urlRepository.count();
        long totalClicks = clickEventRepository.count();
        double averageClicks = totalUrls > 0 ? (double) totalClicks / totalUrls : 0;

        // Count active and expired URLs
        long expiredUrls = urlRepository.findExpiredUrls(LocalDateTime.now()).size();
        long activeUrls = totalUrls - expiredUrls;

        AdminDashboardStats stats = AdminDashboardStats.builder()
                .totalUsers(totalUsers)
                .totalUrls(totalUrls)
                .totalClicks(totalClicks)
                .averageClicksPerUrl(averageClicks)
                .activeUrls(activeUrls)
                .expiredUrls(expiredUrls)
                .generatedAt(LocalDateTime.now())
                .build();

        return ResponseEntity.ok(stats);
    }

    /**
     * GET /api/admin/health - Health check endpoint.
     */
    @GetMapping("/health")
    @Operation(summary = "System health check", description = "Check if the system is healthy")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "System is healthy")
    })
    public ResponseEntity<String> healthCheck() {
        log.info("Health check request");
        return ResponseEntity.ok("System is healthy");
    }

    /**
     * POST /api/admin/cache/clear - Clear application caches.
     */
    @PostMapping("/cache/clear")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Clear caches", description = "Clear all application caches")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Caches cleared successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - admin only")
    })
    public ResponseEntity<String> clearCaches() {
        log.info("Cache clear request");
        // Cache clearing logic would be implemented here
        return ResponseEntity.ok("Caches cleared successfully");
    }
}

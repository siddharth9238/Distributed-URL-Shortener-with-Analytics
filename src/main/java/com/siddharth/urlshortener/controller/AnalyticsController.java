package com.siddharth.urlshortener.controller;

import com.siddharth.urlshortener.dto.AnalyticsResponse;
import com.siddharth.urlshortener.dto.ClickEventDto;
import com.siddharth.urlshortener.service.AnalyticsService;
import com.siddharth.urlshortener.service.UrlService;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/urls")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "URL analytics and click tracking endpoints")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final UrlService urlService;

    /**
     * GET /api/urls/{code}/analytics - Get analytics for a short URL.
     */
    @GetMapping("/{code}/analytics")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get URL analytics", description = "Retrieve comprehensive analytics for a shortened URL")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Analytics retrieved",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AnalyticsResponse.class))),
            @ApiResponse(responseCode = "404", description = "URL not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<AnalyticsResponse> getAnalytics(@PathVariable String code) {
        log.info("Fetching analytics for code: {}", code);

        // Get URL info
        var urlResponse = urlService.getUrlByShortCode(code);

        // Get click count and analytics
        long clickCount = analyticsService.getClickCount(urlResponse.getId());
        Map<String, Long> topReferrers = analyticsService.getTopReferrers(urlResponse.getId());
        Map<String, Long> countryDistribution = analyticsService.getCountryDistribution(urlResponse.getId());
        Map<String, Long> deviceDistribution = analyticsService.getDeviceTypeDistribution(urlResponse.getId());

        AnalyticsResponse response = AnalyticsResponse.builder()
                .urlId(urlResponse.getId())
                .shortCode(code)
                .totalClicks(clickCount)
                .topReferrers(topReferrers)
                .countryDistribution(countryDistribution)
                .deviceTypeDistribution(deviceDistribution)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/urls/{code}/analytics/clicks - Get recent clicks for a URL.
     */
    @GetMapping("/{code}/analytics/clicks")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get recent clicks", description = "Retrieve recent click events for a shortened URL")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Clicks retrieved",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ClickEventDto.class))),
            @ApiResponse(responseCode = "404", description = "URL not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<ClickEventDto>> getRecentClicks(
            @PathVariable String code,
            @RequestParam(defaultValue = "100") int limit) {
        log.info("Fetching recent clicks for code: {} with limit: {}", code, limit);

        var urlResponse = urlService.getUrlByShortCode(code);
        var clicks = analyticsService.getRecentClicks(urlResponse.getId(), limit)
                .stream()
                .map(ClickEventDto::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(clicks);
    }

    /**
     * GET /api/urls/{code}/analytics/range - Get clicks in a time range.
     */
    @GetMapping("/{code}/analytics/range")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get clicks in time range", description = "Retrieve click events within a specific time range")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Clicks retrieved",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ClickEventDto.class))),
            @ApiResponse(responseCode = "404", description = "URL not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<ClickEventDto>> getClicksInRange(
            @PathVariable String code,
            @RequestParam LocalDateTime startTime,
            @RequestParam LocalDateTime endTime) {
        log.info("Fetching clicks for code: {} between {} and {}", code, startTime, endTime);

        var urlResponse = urlService.getUrlByShortCode(code);
        var clicks = analyticsService.getClicksInRange(urlResponse.getId(), startTime, endTime)
                .stream()
                .map(ClickEventDto::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(clicks);
    }
}

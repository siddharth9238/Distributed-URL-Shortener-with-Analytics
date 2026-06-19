package com.siddharth.urlshortener.controller;

import com.siddharth.urlshortener.dto.CreateUrlRequest;
import com.siddharth.urlshortener.dto.UrlResponse;
import com.siddharth.urlshortener.service.UrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/urls")
@RequiredArgsConstructor
@Tag(name = "URL Management", description = "URL shortening and management endpoints")
public class UrlController {

    private final UrlService urlService;

    /**
     * POST /api/urls - Create a new short URL.
     */
    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create short URL", description = "Create a new shortened URL with optional custom alias")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Short URL created",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UrlResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input or alias taken"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    public ResponseEntity<UrlResponse> createShortUrl(@RequestBody CreateUrlRequest request) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        log.info("Creating short URL for user: {}", userId);

        UrlResponse response = urlService.createShortUrl(request, userId);

        return ResponseEntity.created(
                URI.create("/api/urls/" + response.getShortCode())
        ).body(response);
    }

    /**
     * GET /api/urls/{code} - Get URL details by short code.
     */
    @GetMapping("/{code}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get URL details", description = "Retrieve URL information by short code")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "URL found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UrlResponse.class))),
            @ApiResponse(responseCode = "404", description = "URL not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<UrlResponse> getUrl(@PathVariable String code) {
        log.info("Fetching URL details for code: {}", code);
        UrlResponse response = urlService.getUrlByShortCode(code);
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/urls/{code} - Delete a short URL.
     */
    @DeleteMapping("/{code}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete short URL", description = "Delete a shortened URL (owner only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "URL deleted successfully"),
            @ApiResponse(responseCode = "404", description = "URL not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - not the owner")
    })
    public ResponseEntity<Void> deleteUrl(@PathVariable String code) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        log.info("Deleting URL with code: {} for user: {}", code, userId);

        urlService.deleteUrl(code, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/urls/user/all - Get all URLs for the authenticated user.
     */
    @GetMapping("/user/all")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get user's URLs", description = "Retrieve all shortened URLs created by the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of URLs",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UrlResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<UrlResponse>> getUserUrls() {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        log.info("Fetching all URLs for user: {}", userId);

        List<UrlResponse> urls = urlService.getUserUrls(userId);
        return ResponseEntity.ok(urls);
    }

    /**
     * GET /api/urls/redirect/{code} - Redirect to original URL (public endpoint).
     */
    @GetMapping("/redirect/{code}")
    @Operation(summary = "Redirect to original URL", description = "Follow redirect to the original long URL")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "301", description = "Redirect to original URL"),
            @ApiResponse(responseCode = "404", description = "Short URL not found or expired"),
            @ApiResponse(responseCode = "410", description = "Short URL has expired")
    })
    public ResponseEntity<Void> redirect(@PathVariable String code, HttpServletRequest request) {
        log.info("Redirect request for code: {}", code);

        String longUrl = urlService.redirect(code, request);

        return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
                .header(HttpHeaders.LOCATION, longUrl)
                .build();
    }
}

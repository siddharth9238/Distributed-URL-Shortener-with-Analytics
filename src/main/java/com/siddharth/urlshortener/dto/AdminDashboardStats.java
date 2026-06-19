package com.siddharth.urlshortener.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminDashboardStats {
    private long totalUsers;
    private long totalUrls;
    private long totalClicks;
    private double averageClicksPerUrl;
    private long activeUrls;
    private long expiredUrls;
    private LocalDateTime generatedAt;
}

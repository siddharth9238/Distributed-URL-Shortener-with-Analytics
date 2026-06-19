package com.siddharth.urlshortener.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyticsResponse {
    private Long urlId;
    private String shortCode;
    private Long totalClicks;
    private Map<String, Long> topReferrers;
    private Map<String, Long> countryDistribution;
    private Map<String, Long> deviceTypeDistribution;
}

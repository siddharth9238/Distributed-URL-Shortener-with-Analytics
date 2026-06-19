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
public class CreateUrlRequest {
    private String longUrl;
    private String customAlias;
    private LocalDateTime expiresAt;
    private String description;
    private String category;
}

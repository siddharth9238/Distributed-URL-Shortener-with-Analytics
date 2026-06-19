package com.siddharth.urlshortener.dto;

import com.siddharth.urlshortener.model.ClickEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClickEventDto {
    private Long id;
    private Long urlId;
    private LocalDateTime timestamp;
    private String referrer;
    private String country;
    private String deviceType;

    public static ClickEventDto fromEntity(ClickEvent clickEvent) {
        return ClickEventDto.builder()
                .id(clickEvent.getId())
                .urlId(clickEvent.getUrlId())
                .timestamp(clickEvent.getTimestamp())
                .referrer(clickEvent.getReferrer())
                .country(clickEvent.getCountry())
                .deviceType(clickEvent.getDeviceType())
                .build();
    }
}

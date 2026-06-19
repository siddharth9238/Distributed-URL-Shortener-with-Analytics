package com.siddharth.urlshortener.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "click_events", indexes = {
    @Index(name = "idx_url_id", columnList = "urlId"),
    @Index(name = "idx_timestamp", columnList = "timestamp"),
    @Index(name = "idx_url_id_timestamp", columnList = "urlId,timestamp")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClickEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long urlId;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @Column(length = 500)
    private String referrer;

    @Column(length = 50)
    private String ipHash;

    @Column(length = 500)
    private String userAgent;

    @Column(length = 10)
    private String country;

    @Column(length = 50)
    private String deviceType;
}

package org.example.offer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.offer.entity.OfferStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OfferResponse {

    private Long id;
    private Long freelancerId;
    private String title;
    private String domain;
    private String description;
    private BigDecimal price;
    private String durationType;
    private OfferStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer applicationsCount;
}
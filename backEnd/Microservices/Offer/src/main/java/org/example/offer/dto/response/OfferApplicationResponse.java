package org.example.offer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.offer.entity.ApplicationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OfferApplicationResponse {

    private Long id;
    private Long offerId;
    private String offerTitle;
    private Long clientId;
    private String message;
    private BigDecimal proposedBudget;
    private ApplicationStatus status;
    private LocalDateTime appliedAt;
    private LocalDateTime respondedAt;
}
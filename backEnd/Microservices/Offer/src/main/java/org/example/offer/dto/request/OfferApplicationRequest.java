package org.example.offer.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OfferApplicationRequest {

    @NotNull(message = "Offer ID is required")
    private Long offerId;

    @NotNull(message = "Client ID is required")
    private Long clientId;

    @NotBlank(message = "Message is required")
    @Size(min = 20, message = "Message must be at least 20 characters")
    private String message;

    @NotNull(message = "Proposed budget is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Budget must be greater than 0")
    private BigDecimal proposedBudget;
}
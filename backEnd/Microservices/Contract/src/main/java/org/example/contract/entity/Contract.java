package org.example.contract.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "contracts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long clientId;
    private Long freelancerId;
    private Long projectApplicationId;
    private Long offerApplicationId;
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String terms;

    /** Stores the Base64-encoded image data (data:image/...;base64,...) of the client's uploaded signature. */
    @Column(name = "client_signature_data", columnDefinition = "MEDIUMTEXT")
    private String clientSignatureUrl;

    /** Stores the Base64-encoded image data (data:image/...;base64,...) of the freelancer's uploaded signature. */
    @Column(name = "freelancer_signature_data", columnDefinition = "MEDIUMTEXT")
    private String freelancerSignatureUrl;
    
    private BigDecimal amount;
    private LocalDate startDate;
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    private ContractStatus status;

    private LocalDateTime signedAt;
    
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @JsonIgnore
    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL)
    private List<Conflict> conflicts;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = ContractStatus.DRAFT;
        }
    }
}

package org.example.offer.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Client pour le microservice Contract.
 * Création automatique d'un contrat lors de l'acceptation d'une OfferApplication (workflow type Fiverr).
 * Associations via identifiants (offerId, clientId, freelancerId) sans dépendance JPA entre microservices.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ContractClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${service.contract.url:http://localhost:8083}")
    private String contractServiceUrl;

    /**
     * Crée un contrat dans le microservice Contract (livraison, validation, paiement gérés par Contract).
     */
    public void createContractFromAcceptedApplication(ContractCreateRequest request) {
        String url = contractServiceUrl + "/api/contracts";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ContractCreateRequest> entity = new HttpEntity<>(request, headers);
        try {
            restTemplate.postForObject(url, entity, Object.class);
            log.info("Contract created for offerApplicationId={}, clientId={}, freelancerId={}",
                    request.getOfferApplicationId(), request.getClientId(), request.getFreelancerId());
        } catch (Exception e) {
            // Log the error but do NOT block the application acceptance.
            // The contract can be created manually later if the Contract service is unavailable.
            log.warn("Contract service unavailable at {}. Application was accepted but contract was NOT auto-created. Error: {}",
                    url, e.getMessage());
        }
    }
}

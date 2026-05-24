package org.example.subcontracting.client;

import org.example.subcontracting.client.dto.OfferApplicationRemoteDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "OFFER", contextId = "offerApplicationsFeignClient", path = "/api/applications")
public interface OfferApplicationFeignClient {

    @GetMapping("/freelancer/{freelancerId}/accepted")
    List<OfferApplicationRemoteDto> listAcceptedForFreelancerOwnedOffers(@PathVariable("freelancerId") Long freelancerId);
}

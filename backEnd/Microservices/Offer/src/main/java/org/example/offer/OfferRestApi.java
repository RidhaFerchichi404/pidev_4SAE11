package org.example.offer;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
@RequestMapping("mic1/offer")
public class OfferRestApi {
    @GetMapping("/hello")
    public String sayHello() {
        return "Hello I'm Microservice 1: " +
                "I work on Candidat and Adress Entities";
    }
}







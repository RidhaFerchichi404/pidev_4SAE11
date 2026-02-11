package com.example.review.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Redirects the root URL to the Swagger UI to make manual testing easier.
 */
@Controller
public class SwaggerUiRedirectConfig {

    @GetMapping("/")
    public String redirectToSwaggerUi() {
        return "redirect:/swagger-ui.html";
    }
}


package tn.esprit.project.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class MlWebClientConfig {

    @Bean
    public WebClient mlInferenceWebClient(
            @Value("${ml.inference.base-url:http://localhost:8102}") String baseUrl) {
        return WebClient.builder().baseUrl(baseUrl).build();
    }
}

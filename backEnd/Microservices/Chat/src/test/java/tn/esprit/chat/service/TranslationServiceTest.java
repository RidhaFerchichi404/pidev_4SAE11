package tn.esprit.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TranslationServiceTest {

    private static final String MYMEMORY_HOST = "api.mymemory.translated.net/get";

    private TranslationService service;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        service = new TranslationService(new ObjectMapper());
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(service, "restTemplate");
        server = MockRestServiceServer.bindTo(restTemplate).build();
    }

    @Test
    void translateReturnsOriginalWhenInputBlank() {
        assertThat(service.translate(" ", "fr", "en")).isEqualTo(" ");
    }

    @Test
    void translateUsesMyMemoryWhenResponseIsValid() {
        server.expect(requestTo(org.hamcrest.Matchers.containsString(MYMEMORY_HOST)))
            .andRespond(withSuccess(
                "{\"responseStatus\":200,\"responseData\":{\"translatedText\":\"bonjour%20monde\"}}",
                MediaType.APPLICATION_JSON
            ));

        String translated = service.translate("hello world", "fr", "en");

        assertThat(translated).isEqualTo("bonjour monde");
        server.verify();
    }

    @Test
    void translateFallsBackToLingvaWhenMyMemoryReturnsErrorText() {
        server.expect(requestTo(org.hamcrest.Matchers.containsString(MYMEMORY_HOST)))
            .andRespond(withSuccess(
                "{\"responseStatus\":200,\"responseData\":{\"translatedText\":\"INVALID LANGUAGE PAIR\"}}",
                MediaType.APPLICATION_JSON
            ));
        server.expect(requestTo(org.hamcrest.Matchers.containsString("lingva.ml/api/v1")))
            .andRespond(withSuccess(
                "{\"translation\":\"salut\"}",
                MediaType.APPLICATION_JSON
            ));

        String translated = service.translate("hello", "fr", "auto");

        assertThat(translated).isEqualTo("salut");
        server.verify();
    }

    @Test
    void translateReturnsOriginalWhenBothProvidersFail() {
        server.expect(requestTo(org.hamcrest.Matchers.containsString(MYMEMORY_HOST)))
            .andRespond(withSuccess(
                "{\"responseStatus\":500,\"responseData\":{\"translatedText\":\"\"}}",
                MediaType.APPLICATION_JSON
            ));
        server.expect(requestTo(org.hamcrest.Matchers.containsString("lingva.ml/api/v1")))
            .andRespond(withSuccess(
                "{\"error\":\"service down\"}",
                MediaType.APPLICATION_JSON
            ));

        String translated = service.translate("keep me", "fr", "en");

        assertThat(translated).isEqualTo("keep me");
        server.verify();
    }
}

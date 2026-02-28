package com.esprit.planning.config;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Collections;

/**
 * Configures Google Calendar API client when {@code google.calendar.enabled=true}
 * and {@code google.calendar.credentials-path} is set.
 */
@Configuration
@Slf4j
public class GoogleCalendarConfig {

    @Bean
    @ConditionalOnProperty(prefix = "google.calendar", name = "enabled", havingValue = "true")
    public Calendar googleCalendar(
            @Value("${google.calendar.credentials-path:}") String credentialsPath) throws IOException, GeneralSecurityException {
        if (credentialsPath == null || credentialsPath.isBlank()) {
            log.warn("Google Calendar is enabled but credentials-path is empty; calendar operations will no-op.");
            return null;
        }
        Path path = Path.of(credentialsPath);
        if (!Files.isRegularFile(path)) {
            log.warn("Google Calendar credentials file not found at: {}; calendar operations will no-op.", credentialsPath);
            return null;
        }
        try (FileInputStream in = new FileInputStream(path.toFile())) {
            GoogleCredentials credentials = GoogleCredentials.fromStream(in)
                    .createScoped(Collections.singleton(CalendarScopes.CALENDAR));
            return new Calendar.Builder(
                            GoogleNetHttpTransport.newTrustedTransport(),
                            GsonFactory.getDefaultInstance(),
                            new HttpCredentialsAdapter(credentials))
                    .setApplicationName("Planning Service")
                    .build();
        }
    }
}

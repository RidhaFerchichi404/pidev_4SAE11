package org.example.subcontracting.service;

import org.example.subcontracting.config.GatewayOnlyFilter;
import org.example.subcontracting.config.MailTemplateConfig;
import org.example.subcontracting.config.SubcontractDataSanitizer;
import org.example.subcontracting.entity.SubcontractMediaType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.thymeleaf.TemplateEngine;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class SubcontractInfraCoverageTest {

    @TempDir
    Path tempDir;

    @Test
    void mediaStorageService_handlesSaveLoadAndGuessContentType() {
        SubcontractMediaStorageService storage = new SubcontractMediaStorageService(tempDir.toString());
        MockMultipartFile file = new MockMultipartFile("file", "clip.mp4", "video/mp4", new byte[]{1, 2, 3});

        var stored = storage.save(file);
        assertThat(stored.mediaType()).isEqualTo(SubcontractMediaType.VIDEO);
        assertThat(storage.guessContentType(stored.storedFileName())).isEqualTo("video/mp4");
        assertThat(storage.loadAsResource(stored.storedFileName())).isNotNull();
        assertThat(storage.loadAsResource("missing.mp3")).isNull();
    }

    @Test
    void gatewayFilter_and_mailTemplate_and_sanitizer_smoke() throws Exception {
        GatewayOnlyFilter filter = new GatewayOnlyFilter();
        MockHttpServletResponse blocked = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest("GET", "/api/subcontracts"), blocked, new MockFilterChain());
        assertThat(blocked.getStatus()).isEqualTo(403);

        MockHttpServletRequest allowedReq = new MockHttpServletRequest("GET", "/api/subcontracts");
        allowedReq.addHeader("X-Internal-Gateway", "true");
        MockHttpServletResponse allowedRes = new MockHttpServletResponse();
        filter.doFilter(allowedReq, allowedRes, new MockFilterChain());
        assertThat(allowedRes.getStatus()).isNotEqualTo(403);

        TemplateEngine engine = new MailTemplateConfig().mailTemplateEngine();
        assertThat(engine).isNotNull();

        JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        when(jdbcTemplate.update(anyString())).thenReturn(1);
        new SubcontractDataSanitizer(jdbcTemplate).sanitizeEnumColumns();
    }
}

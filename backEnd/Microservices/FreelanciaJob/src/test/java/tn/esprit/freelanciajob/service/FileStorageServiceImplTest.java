package tn.esprit.freelanciajob.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import tn.esprit.freelanciajob.Service.FileStorageServiceImpl;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileStorageServiceImplTest {

    @TempDir
    Path tempDir;

    @Test
    void validateFilesRejectsTooManyFiles() {
        FileStorageServiceImpl service = new FileStorageServiceImpl();
        List<MockMultipartFile> files = java.util.stream.IntStream.range(0, 6)
            .mapToObj(i -> new MockMultipartFile("f" + i, "a" + i + ".pdf", "application/pdf", new byte[]{1}))
            .toList();

        assertThatThrownBy(() -> service.validateFiles((List) files))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Too many files");
    }

    @Test
    void validateFilesRejectsUnsupportedType() {
        FileStorageServiceImpl service = new FileStorageServiceImpl();
        MockMultipartFile file = new MockMultipartFile("f", "a.txt", "text/plain", "x".getBytes());

        assertThatThrownBy(() -> service.validateFiles(List.of(file)))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("File type not allowed");
    }

    @Test
    void storeAndDeleteRoundTripWorks() throws Exception {
        FileStorageServiceImpl service = new FileStorageServiceImpl();
        ReflectionTestUtils.setField(service, "uploadBaseDir", tempDir.toString());

        MockMultipartFile file = new MockMultipartFile("f", "cv.pdf", "application/pdf", "pdf-content".getBytes());
        String url = service.storeFile(file, 11L);

        assertThat(url).contains("/uploads/applications/11/");
        Path storedDir = tempDir.resolve("applications").resolve("11");
        assertThat(Files.exists(storedDir)).isTrue();
        assertThat(Files.list(storedDir).count()).isEqualTo(1);

        service.deleteApplicationFiles(11L);
        assertThat(Files.exists(storedDir)).isFalse();
    }
}

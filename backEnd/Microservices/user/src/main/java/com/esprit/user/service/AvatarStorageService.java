package com.esprit.user.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;

/**
 * Stores avatar image files on the local filesystem and builds public URLs.
 */
@Service
@Slf4j
public class AvatarStorageService {

    private static final String AVATARS_PATH_SEGMENT = "api/users/avatars/";
    private static final long MAX_SIZE_BYTES = 5 * 1024 * 1024; // 5MB
    private static final String[] ALLOWED_CONTENT_TYPES = {
            "image/jpeg", "image/png", "image/gif", "image/webp"
    };

    private final Path uploadDir;
    private final String publicBaseUrl;

    public AvatarStorageService(
            @Value("${app.upload-dir:uploads/avatars}") String uploadDir,
            @Value("${app.public-base-url:http://localhost:8078/user}") String publicBaseUrl) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath();
        this.publicBaseUrl = publicBaseUrl.endsWith("/") ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1) : publicBaseUrl;
    }

    /**
     * Saves the multipart file as an avatar and returns the public URL.
     *
     * @param file uploaded file (image)
     * @return the public URL to access the avatar, or empty if invalid
     */
    public Optional<String> store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Optional.empty();
        }
        String contentType = file.getContentType();
        if (contentType == null || !isAllowedContentType(contentType)) {
            log.warn("Rejected avatar upload: unsupported content type {}", contentType);
            return Optional.empty();
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            log.warn("Rejected avatar upload: file too large {} bytes", file.getSize());
            return Optional.empty();
        }
        String extension = extensionFromContentType(contentType);
        String filename = UUID.randomUUID() + extension;
        try {
            Files.createDirectories(uploadDir);
            Path target = uploadDir.resolve(filename).normalize();
            if (!target.startsWith(uploadDir)) {
                log.warn("Rejected avatar upload: path escape attempt");
                return Optional.empty();
            }
            file.transferTo(target);
            String url = publicBaseUrl + "/" + AVATARS_PATH_SEGMENT + filename;
            log.info("Stored avatar: {}", filename);
            return Optional.of(url);
        } catch (IOException e) {
            log.error("Failed to store avatar: {}", e.getMessage(), e);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Unexpected error storing avatar", e);
            return Optional.empty();
        }
    }

    /**
     * Resolves the filesystem path for a filename under the avatars directory.
     */
    public Path resolvePath(String filename) {
        if (filename == null || filename.contains("..")) {
            return null;
        }
        return uploadDir.resolve(filename).normalize();
    }

    private static boolean isAllowedContentType(String contentType) {
        for (String allowed : ALLOWED_CONTENT_TYPES) {
            if (allowed.equalsIgnoreCase(contentType)) return true;
        }
        return false;
    }

    private static String extensionFromContentType(String contentType) {
        if (contentType == null) return ".bin";
        return switch (contentType.toLowerCase()) {
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }
}

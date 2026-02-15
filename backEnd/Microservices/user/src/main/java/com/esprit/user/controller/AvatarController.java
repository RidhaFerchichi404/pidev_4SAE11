package com.esprit.user.controller;

import com.esprit.user.service.AvatarStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Avatar upload (local file) and serve.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Avatar", description = "Upload and serve user avatar images")
public class AvatarController {

    private final AvatarStorageService avatarStorageService;

    @Operation(summary = "Upload avatar", description = "Upload an image file as avatar. Returns URL to use as avatarUrl.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Upload successful"),
            @ApiResponse(responseCode = "400", description = "Invalid or missing file")
    })
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> uploadAvatar(
            @Parameter(description = "Image file (JPEG, PNG, GIF, WebP; max 5MB)")
            @RequestParam(value = "file", required = false) MultipartFile file) {
        if (file == null || file.isEmpty()) {
            log.warn("Avatar upload: no file or empty file");
            return ResponseEntity.badRequest().body(Map.of("error", "No file provided"));
        }
        return avatarStorageService.store(file)
                .map(url -> ResponseEntity.ok(Map.of("avatarUrl", url)))
                .orElse(ResponseEntity.badRequest().body(Map.of("error", "Failed to store image. Check format (JPEG/PNG/GIF/WebP) and size (max 5MB).")));
    }

    @Operation(summary = "Get avatar image", description = "Serves a previously uploaded avatar by filename.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Image found"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    @GetMapping("avatars/{filename}")
    public ResponseEntity<Resource> serveAvatar(
            @Parameter(description = "Avatar filename (from upload response URL)") @PathVariable String filename) {
        var path = avatarStorageService.resolvePath(filename);
        if (path == null || !path.toFile().exists() || !path.toFile().isFile()) {
            return ResponseEntity.notFound().build();
        }
        try {
            Resource resource = new UrlResource(path.toUri());
            String contentType = contentTypeFromFilename(filename);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                    .body(resource);
        } catch (Exception e) {
            log.warn("Could not serve avatar {}", filename, e);
            return ResponseEntity.notFound().build();
        }
    }

    private static String contentTypeFromFilename(String filename) {
        if (filename == null) return "application/octet-stream";
        String lower = filename.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        return "image/jpeg";
    }
}

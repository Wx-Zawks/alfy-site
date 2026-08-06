package com.alfy.api.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Restores versioned seed-media files from the application package into the
 * configured persistent upload directory. SQL migrations create the matching
 * media_asset records; this runner supplies their binary payloads.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "alfy.bootstrap-media", name = "enabled", havingValue = "true", matchIfMissing = true)
public class BundledMediaBootstrapRunner implements ApplicationRunner {

    private static final String MANIFEST = "bootstrap/legacy-news/manifest.json";
    private static final String RESOURCE_ROOT = "bootstrap/legacy-news/";
    private static final String STORAGE_PREFIX = "bootstrap/legacy-news/";

    private final ObjectMapper objectMapper;

    @Value("${alfy.content-import.storage-root:./data/alfy/uploads}")
    private String storageRoot;

    @Override
    public void run(ApplicationArguments args) throws IOException {
        ClassPathResource manifestResource = new ClassPathResource(MANIFEST);
        if (!manifestResource.exists()) {
            return;
        }
        Path root = Path.of(storageRoot).toAbsolutePath().normalize();
        JsonNode manifest;
        try (InputStream input = manifestResource.getInputStream()) {
            manifest = objectMapper.readTree(input);
        }
        int restored = 0;
        for (JsonNode asset : manifest.path("assets")) {
            String storageKey = asset.path("storage_key").asText();
            String resourcePath = asset.path("resource_path").asText();
            if (!storageKey.startsWith(STORAGE_PREFIX) || resourcePath.isBlank()) {
                throw new IOException("Invalid bundled media manifest entry");
            }
            Path target = root.resolve(storageKey).normalize();
            if (!target.startsWith(root)) {
                throw new IOException("Bundled media storage path escapes storage root");
            }
            if (Files.isRegularFile(target)) {
                continue;
            }
            ClassPathResource resource = new ClassPathResource(RESOURCE_ROOT + resourcePath);
            if (!resource.exists()) {
                throw new IOException("Bundled media resource is missing: " + resourcePath);
            }
            Files.createDirectories(target.getParent());
            try (InputStream input = resource.getInputStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
            restored++;
        }
        if (restored > 0) {
            log.info("Restored {} bundled legacy-news media files to {}", restored, root);
        }
    }
}

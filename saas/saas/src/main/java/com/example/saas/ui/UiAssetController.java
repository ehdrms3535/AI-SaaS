package com.example.saas.ui;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.time.Duration;

@RestController
public class UiAssetController {

    private static final Path STATIC_ROOT = Path.of("src", "main", "resources", "static");

    @GetMapping(value = "/index.html", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<Resource> indexHtml() {
        return file("index.html", MediaType.TEXT_HTML);
    }

    @GetMapping(value = "/app.css", produces = "text/css")
    public ResponseEntity<Resource> appCss() {
        return file("app.css", MediaType.valueOf("text/css"));
    }

    @GetMapping(value = "/app.js", produces = "application/javascript")
    public ResponseEntity<Resource> appJs() {
        return file("app.js", MediaType.valueOf("application/javascript"));
    }

    @GetMapping(value = "/favicon.ico")
    public ResponseEntity<Void> favicon() {
        return ResponseEntity.noContent()
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)))
                .build();
    }

    private ResponseEntity<Resource> file(String fileName, MediaType mediaType) {
        Resource resource = new FileSystemResource(STATIC_ROOT.resolve(fileName));
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(mediaType)
                .cacheControl(CacheControl.noCache())
                .body(resource);
    }
}

package com.universidad.mediconnect2.media;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Demo only: private patient files are stored in Mongo GridFS (no public links).
 * Real clinical documents require extra compliance and local encryption controls.
 */
@RestController
@RequestMapping("/api/v1/media/me")
@Profile({"prod", "docker"})
@PreAuthorize("hasRole('PATIENT')")
public class PrivateMediaController {
    private final PrivateMediaService service;

    public PrivateMediaController(PrivateMediaService service) {
        this.service = service;
    }

    @GetMapping
    public List<PrivateMediaService.MediaView> mine(Authentication auth) {
        return service.list(auth.getName());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public PrivateMediaService.MediaView upload(
            Authentication auth, @RequestParam("file") MultipartFile file) throws IOException {
        return service.upload(auth.getName(), file);
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> download(Authentication auth, @PathVariable String id)
            throws IOException {
        PrivateMediaService.PrivateMedia media = service.download(auth.getName(), id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(media.metadata().contentType()));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(media.metadata().filename(), StandardCharsets.UTF_8).build());
        headers.setCacheControl(CacheControl.noStore());
        headers.set("X-Content-Type-Options", "nosniff");
        headers.set("Content-Security-Policy", "sandbox");
        return new ResponseEntity<>(media.bytes(), headers, HttpStatus.OK);
    }
}

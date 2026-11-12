package com.universidad.mediconnect2.media;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrivateMediaServiceTest {
    @Mock GridFsTemplate grid;

    @Test void acceptsWebpByMagicBytesNotByUserSuppliedMime() {
        byte[] bytes = {'R','I','F','F',0,0,0,0,'W','E','B','P',0,0};
        assertEquals("image/webp", PrivateMediaService.checkedType(bytes));
    }

    @Test void rejectsHtmlDisguisedAsImage() {
        byte[] html = "<script>alert('x')</script>".getBytes();
        assertThrows(IllegalArgumentException.class,
                () -> new PrivateMediaService(grid).upload("demo@example.com",
                        new MockMultipartFile("file", "foto.webp", "image/webp", html)));
        verifyNoInteractions(grid);
    }

    @Test void rejectsOversizedFiles() {
        byte[] bytes = new byte[(int)PrivateMediaService.MAX_BYTES + 1];
        assertThrows(IllegalArgumentException.class,
                () -> new PrivateMediaService(grid).upload("demo@example.com",
                        new MockMultipartFile("file", "grande.png", "image/png", bytes)));
        verifyNoInteractions(grid);
    }

    @Test void preventsExecutableExtensionsInDownloads() {
        assertEquals("prueba.webp", PrivateMediaService.safeFilename("prueba.html", "image/webp"));
        assertEquals("consulta.pdf", PrivateMediaService.safeFilename("consulta.js", "application/pdf"));
    }

    @Test void doesNotLookupInvalidObjectId() {
        assertThrows(RuntimeException.class, () -> new PrivateMediaService(grid)
                .download("demo@example.com", "invalid-id"));
        verifyNoInteractions(grid);
    }
}

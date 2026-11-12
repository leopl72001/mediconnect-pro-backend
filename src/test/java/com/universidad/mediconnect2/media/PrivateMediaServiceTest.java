package com.universidad.mediconnect2.media;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.mock.web.MockMultipartFile;
import com.mongodb.client.gridfs.model.GridFSFile;
import org.bson.Document;
import org.bson.BsonObjectId;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.query.Query;

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

    @Test void hidesAnotherUsersFileAsNotFound() {
        ObjectId objectId = new ObjectId();
        GridFSFile stored = mock(GridFSFile.class);
        when(stored.getMetadata()).thenReturn(new Document("owner", "owner@example.com"));
        when(grid.findOne(any(Query.class))).thenReturn(stored);

        assertThrows(RuntimeException.class, () -> new PrivateMediaService(grid)
                .download("other@example.com", objectId.toHexString()));

        verify(grid, never()).getResource(any(GridFSFile.class));
    }

    @Test void doesNotLookupInvalidObjectId() {
        assertThrows(RuntimeException.class, () -> new PrivateMediaService(grid)
                .download("demo@example.com", "invalid-id"));
        verifyNoInteractions(grid);
    }

    @Test void identifiesPngJpegAndPdfUsingMagicBytes() {
        assertEquals("image/png", PrivateMediaService.checkedType(
                new byte[] {(byte) 137,80,78,71,13,10,26,10}));
        assertEquals("image/jpeg", PrivateMediaService.checkedType(
                new byte[] {(byte) 0xff,(byte) 0xd8,(byte) 0xff,0}));
        assertEquals("application/pdf", PrivateMediaService.checkedType(
                new byte[] {'%','P','D','F','-', '1'}));
    }

    @Test void rejectsTruncatedAndUnknownFileSignatures() {
        for (byte[] candidate : new byte[][] {
                {}, {'R','I','F','F'}, {'%','P','D','F'}, {(byte) 137,80,78},
                {'G','I','F','8','9','a'} }) {
            assertThrows(IllegalArgumentException.class,
                    () -> PrivateMediaService.checkedType(candidate));
        }
    }

    @Test void normalizesDangerousAndMissingFilenames() {
        assertEquals("documento.pdf", PrivateMediaService.safeFilename(null, "application/pdf"));
        assertEquals("documento.png", PrivateMediaService.safeFilename("", "image/png"));
        assertEquals("mi_foto.jpg", PrivateMediaService.safeFilename("mi foto.exe", "image/jpeg"));
        assertEquals("foto.webp", PrivateMediaService.safeFilename("foto.svg", "image/webp"));
        assertEquals("documento.jpg", PrivateMediaService.safeFilename(".hidden", "image/jpeg"));
        assertThrows(IllegalArgumentException.class,
                () -> PrivateMediaService.safeFilename("file", "text/html"));
        assertEquals(75, PrivateMediaService.safeFilename("a".repeat(90) + ".webp", "image/webp").length());
    }

    @Test void refusesMissingOrEmptyUploadBeforeCallingMongo() {
        PrivateMediaService service = new PrivateMediaService(grid);
        assertThrows(IllegalArgumentException.class, () -> service.upload("demo@example.com", null));
        assertThrows(IllegalArgumentException.class, () -> service.upload("demo@example.com",
                new MockMultipartFile("file", "empty.png", "image/png", new byte[0])));
        verifyNoInteractions(grid);
    }

    @Test void storesValidatedImageWithCanonicalOwnerAndSafeName() throws IOException {
        byte[] png = {(byte) 137,80,78,71,13,10,26,10,0,0,0};
        ObjectId id = new ObjectId();
        when(grid.store(any(java.io.InputStream.class), eq("foto.png"), eq("image/png"),
                any(Document.class))).thenReturn(id);
        var stored = new PrivateMediaService(grid).upload("  Paciente@Example.COM  ",
                new MockMultipartFile("file", "foto.html", "text/html", png));

        assertEquals(id.toHexString(), stored.id());
        assertEquals("image/png", stored.contentType());
        assertEquals("foto.png", stored.filename());
        assertEquals(png.length, stored.size());
        assertNotNull(stored.uploadedAt());

        org.mockito.ArgumentCaptor<Document> metadata =
                org.mockito.ArgumentCaptor.forClass(Document.class);
        verify(grid).store(any(java.io.InputStream.class), eq("foto.png"), eq("image/png"),
                metadata.capture());
        assertEquals("paciente@example.com", metadata.getValue().getString("owner"));
        assertEquals("image/png", metadata.getValue().getString("kind"));
        assertEquals("foto.png", metadata.getValue().getString("filename"));
    }

    @Test void refusesMissingFileAndMissingMetadataWithoutReturningBytes() {
        ObjectId id = new ObjectId();
        when(grid.findOne(any(Query.class))).thenReturn(null);
        assertThrows(com.universidad.mediconnect2.shared.NotFoundException.class,
                () -> new PrivateMediaService(grid).download("demo@example.com", id.toHexString()));
        verify(grid, never()).getResource(any(GridFSFile.class));
    }

}

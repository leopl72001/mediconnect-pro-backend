package com.universidad.mediconnect2.media;

import com.mongodb.client.MongoCursor;
import com.mongodb.client.gridfs.GridFSFindIterable;
import com.mongodb.client.gridfs.model.GridFSFile;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import com.universidad.mediconnect2.shared.NotFoundException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PrivateMediaGridFsCoverageTest {
    private GridFsTemplate grid;
    private PrivateMediaService service;
    private final ObjectId id = new ObjectId();
    private final Instant when = Instant.parse("2026-11-12T12:00:00Z");

    @BeforeEach void init() {
        grid = mock(GridFsTemplate.class);
        service = new PrivateMediaService(grid);
    }

    private GridFSFile stored(String owner, long size) {
        GridFSFile file = mock(GridFSFile.class);
        when(file.getMetadata()).thenReturn(new Document("owner", owner).append("kind", "image/png"));
        when(file.getObjectId()).thenReturn(id);
        when(file.getFilename()).thenReturn("foto-ficticia.png");
        when(file.getLength()).thenReturn(size);
        when(file.getUploadDate()).thenReturn(Date.from(when));
        return file;
    }

    @Test void listsOnlyAuthorizedOwnersFilesWithMetadata() {
        GridFSFile file = stored("patient@example.test", 12L);
        GridFSFindIterable results = mock(GridFSFindIterable.class);
        @SuppressWarnings("unchecked")
        MongoCursor<GridFSFile> cursor = mock(MongoCursor.class);
        when(grid.find(any(Query.class))).thenReturn(results);
        when(results.iterator()).thenReturn(cursor);
        when(cursor.hasNext()).thenReturn(true, false);
        when(cursor.next()).thenReturn(file);
        List<PrivateMediaService.MediaView> views = service.list(" PATIENT@Example.Test ");
        assertEquals(1, views.size());
        assertEquals(id.toHexString(), views.get(0).id());
        assertEquals("foto-ficticia.png", views.get(0).filename());
        assertEquals("image/png", views.get(0).contentType());
        assertEquals(12L, views.get(0).size());
        assertEquals(when, views.get(0).uploadedAt());
        verify(grid).find(any(Query.class));
    }

    @Test void missingMetadataIsHandledSafelyWhileListing() {
        GridFSFile file = mock(GridFSFile.class);
        when(file.getMetadata()).thenReturn(null);
        when(file.getObjectId()).thenReturn(id);
        when(file.getFilename()).thenReturn("ficticio.bin");
        when(file.getUploadDate()).thenReturn(Date.from(when));
        GridFSFindIterable results = mock(GridFSFindIterable.class);
        @SuppressWarnings("unchecked")
        MongoCursor<GridFSFile> cursor = mock(MongoCursor.class);
        when(grid.find(any(Query.class))).thenReturn(results);
        when(results.iterator()).thenReturn(cursor);
        when(cursor.hasNext()).thenReturn(true, false);
        when(cursor.next()).thenReturn(file);
        assertEquals("application/octet-stream", service.list("patient@example.test").get(0).contentType());
    }

    @Test void rejectsFileWithMissingMetadataWithoutReadingBytes() {
        GridFSFile file = mock(GridFSFile.class);
        when(file.getMetadata()).thenReturn(null);
        when(grid.findOne(any(Query.class))).thenReturn(file);
        assertThrows(NotFoundException.class,
                () -> service.download("patient@example.test", id.toHexString()));
        verify(grid, never()).getResource(any());
    }

    @Test void rejectsFileExceedingFiveMegabytesBeforeDownload() {
        GridFSFile file = mock(GridFSFile.class);
        when(file.getMetadata()).thenReturn(new Document("owner", "patient@example.test"));
        when(file.getLength()).thenReturn(PrivateMediaService.MAX_BYTES + 1);
        when(grid.findOne(any(Query.class))).thenReturn(file);
        assertThrows(IllegalArgumentException.class,
                () -> service.download("PATIENT@example.test", id.toHexString()));
        verify(grid, never()).getResource(any());
    }

    @Test void ownerMayDownloadRealBytesWithTheExpectedMetadata() throws Exception {
        byte[] bytes = {1, 2, 3, 4};
        GridFSFile file = stored("patient@example.test", bytes.length);
        GridFsResource resource = mock(GridFsResource.class);
        when(grid.findOne(any(Query.class))).thenReturn(file);
        when(grid.getResource(file)).thenReturn(resource);
        when(resource.getInputStream()).thenReturn(new ByteArrayInputStream(bytes));
        PrivateMediaService.PrivateMedia result =
                service.download(" PATIENT@example.test ", id.toHexString());
        assertArrayEquals(bytes, result.bytes());
        assertEquals(id.toHexString(), result.metadata().id());
        assertEquals("image/png", result.metadata().contentType());
        assertEquals("foto-ficticia.png", result.metadata().filename());
    }
}

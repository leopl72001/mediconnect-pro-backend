package com.universidad.mediconnect2.media;

import com.mongodb.client.gridfs.model.GridFSFile;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.universidad.mediconnect2.shared.NotFoundException;

@Service
@Profile({"prod", "docker"})
public class PrivateMediaService {
    public static final long MAX_BYTES = 5L * 1024 * 1024;
    private final GridFsTemplate grid;

    public PrivateMediaService(GridFsTemplate grid) {
        this.grid = grid;
    }

    public record MediaView(String id, String filename, String contentType, long size, Instant uploadedAt) {}
    public record PrivateMedia(MediaView metadata, byte[] bytes) {}

    public MediaView upload(String email, MultipartFile part) throws IOException {
        if (part == null || part.isEmpty() || part.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("El archivo debe tener entre 1 byte y 5 MB.");
        }

        byte[] bytes = part.getBytes();
        if (bytes.length == 0 || bytes.length > MAX_BYTES) {
            throw new IllegalArgumentException("El archivo excede el limite de 5 MB.");
        }
        String mime = checkedType(bytes);
        String filename = safeFilename(part.getOriginalFilename(), mime);
        Document metadata = new Document("owner", canonical(email))
                .append("kind", mime)
                .append("filename", filename);
        ObjectId id = grid.store(new java.io.ByteArrayInputStream(bytes), filename, mime, metadata);
        return new MediaView(id.toHexString(), filename, mime, bytes.length, Instant.now());
    }

    public List<MediaView> list(String email) {
        Query query = new Query(Criteria.where("metadata.owner").is(canonical(email)))
                .with(Sort.by(Sort.Direction.DESC, "uploadDate")).limit(100);
        List<MediaView> result = new ArrayList<>();
        for (GridFSFile file : grid.find(query)) {
            result.add(toView(file));
        }
        return result;
    }

    public PrivateMedia download(String email, String id) throws IOException {
        if (!ObjectId.isValid(id)) {
            throw new NotFoundException("Archivo no encontrado.");
        }
        GridFSFile file = grid.findOne(new Query(Criteria.where("_id").is(new ObjectId(id))));
        if (file == null || file.getMetadata() == null ||
                !canonical(email).equals(file.getMetadata().getString("owner"))) {
            // Do not reveal existence of another user's file.
            throw new NotFoundException("Archivo no encontrado.");
        }
        if (file.getLength() > MAX_BYTES) {
            throw new IllegalArgumentException("Archivo excede el limite permitido.");
        }
        GridFsResource resource = grid.getResource(file);
        try (var input = resource.getInputStream()) {
            return new PrivateMedia(toView(file), input.readAllBytes());
        }
    }

    private static MediaView toView(GridFSFile file) {
        Document metadata = file.getMetadata();
        String mime = metadata == null ? "application/octet-stream" : metadata.getString("kind");
        return new MediaView(file.getObjectId().toHexString(), file.getFilename(),
                mime, file.getLength(), file.getUploadDate().toInstant());
    }

    static String checkedType(byte[] bytes) {
        if (bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' &&
                bytes[2] == 'F' && bytes[3] == 'F' &&
                bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return "image/webp";
        }
        if (bytes.length >= 3 && (bytes[0] & 0xff) == 0xff &&
                (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff) {
            return "image/jpeg";
        }
        byte[] png = {(byte)137, 80, 78, 71, 13, 10, 26, 10};
        if (bytes.length >= 8 && java.util.Arrays.equals(
                java.util.Arrays.copyOf(bytes, 8), png)) {
            return "image/png";
        }
        if (bytes.length >= 5 && bytes[0] == '%' && bytes[1] == 'P' &&
                bytes[2] == 'D' && bytes[3] == 'F' && bytes[4] == '-') {
            return "application/pdf";
        }
        throw new IllegalArgumentException("Solo se admiten imagenes PNG, JPEG, WebP o PDF validos.");
    }

    static String safeFilename(String original, String mime) {
        String sanitized = original == null ? "documento" :
                original.replaceAll("[^a-zA-Z0-9._-]", "_");
        String base = sanitized.replaceFirst("\\.[^.]*$", "");
        if (base.isBlank()) base = "documento";
        if (base.length() > 70) base = base.substring(0, 70);
        String extension = switch (mime) {
            case "image/webp" -> ".webp";
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "application/pdf" -> ".pdf";
            default -> throw new IllegalArgumentException("Formato no admitido.");
        };
        return base + extension;
    }

    private static String canonical(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}

package com.cinebook.infra;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;

/**
 * Reads and writes JSON arrays from/to files with crash-safe atomic writes.
 *
 * <p>Write protocol:
 * <ol>
 *   <li>Serialize to {@code filename.tmp}</li>
 *   <li>fsync via {@link FileChannel#force(boolean)}</li>
 *   <li>{@link Files#move} with ATOMIC_MOVE + REPLACE_EXISTING</li>
 * </ol>
 * A crash at any point leaves either the old file intact or the new file
 * fully written — never a partial/corrupt file.
 */
public class JsonFileAdapter {

    private static final Logger log = LoggerFactory.getLogger(JsonFileAdapter.class);
    private final ObjectMapper mapper;

    public JsonFileAdapter() {
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Read a JSON array file into a list of objects.
     *
     * @return the parsed list, or an empty list if the file doesn't exist or is empty
     */
    public <T> List<T> readList(Path file, TypeReference<List<T>> typeRef) throws IOException {
        if (!Files.exists(file)) {
            return Collections.emptyList();
        }
        byte[] bytes = Files.readAllBytes(file);
        if (bytes.length == 0) {
            return Collections.emptyList();
        }
        return mapper.readValue(bytes, typeRef);
    }

    /**
     * Write a list of objects as a JSON array, using atomic write protocol.
     */
    public <T> void writeList(Path file, List<T> data) throws IOException {
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        byte[] bytes = mapper.writeValueAsBytes(data);

        // Write to tmp file and fsync
        try (OutputStream out = new FileOutputStream(tmp.toFile());
             FileChannel channel = ((FileOutputStream) out).getChannel()) {
            out.write(bytes);
            out.flush();
            channel.force(true);
        }

        // Atomic move from tmp to real
        Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        log.debug("Wrote {} entries to {}", data.size(), file);
    }

    /** Expose the configured ObjectMapper for callers that need custom deserialization. */
    public ObjectMapper getMapper() {
        return mapper;
    }
}

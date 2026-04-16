package com.cinebook.infra;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Append-only CSV file adapter.
 *
 * <p>Writes are appended to the file (never rewritten). The first line is
 * treated as a header row. Reads return all non-header rows split by comma.
 */
public class CsvAppendAdapter {

    private static final Logger log = LoggerFactory.getLogger(CsvAppendAdapter.class);

    /**
     * Read all data rows (skipping header) from a CSV file.
     *
     * @return list of string arrays, each representing one row's fields.
     *         Returns empty list if file doesn't exist or has only a header.
     */
    public List<String[]> readAll(Path file) throws IOException {
        if (!Files.exists(file)) {
            return Collections.emptyList();
        }
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        if (lines.size() <= 1) {
            return Collections.emptyList();
        }
        List<String[]> rows = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (!line.isEmpty()) {
                rows.add(parseCsvLine(line));
            }
        }
        return rows;
    }

    /**
     * Append a single row to the CSV file.
     * Creates the file with the given header if it doesn't exist.
     *
     * @param header the CSV header line (used only when creating the file)
     * @param row    comma-joined data values
     */
    public void append(Path file, String header, String row) throws IOException {
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        if (!Files.exists(file)) {
            Files.writeString(file, header + "\n", StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        }
        Files.writeString(file, row + "\n", StandardCharsets.UTF_8,
                StandardOpenOption.APPEND);
        log.debug("Appended row to {}", file);
    }

    /**
     * Read the header line from a CSV file.
     *
     * @return the header, or null if the file doesn't exist or is empty
     */
    public String readHeader(Path file) throws IOException {
        if (!Files.exists(file)) {
            return null;
        }
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        return lines.isEmpty() ? null : lines.get(0);
    }

    /**
     * Parse a CSV line, respecting quoted fields containing commas.
     * Quoted fields use double-quotes: {@code "D6;D7"} keeps the semicolons intact.
     */
    static String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields.toArray(new String[0]);
    }
}

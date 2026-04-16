package com.cinebook.infra;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Write-ahead log (WAL) for crash recovery.
 *
 * <p>Every hold and commit is logged before any state mutation.
 * On startup, uncommitted holds can be detected and rolled back.
 *
 * <p>Format: {@code <timestamp> <operation> <args...>}
 * <ul>
 *   <li>{@code HOLD <showtimeId> <userId> <seat1,seat2,...>}</li>
 *   <li>{@code COMMIT <bookingId>}</li>
 *   <li>{@code ROLLBACK <showtimeId> <seat1,seat2,...>}</li>
 * </ul>
 */
public class TransactionLog {

    private static final Logger log = LoggerFactory.getLogger(TransactionLog.class);
    private final Path logFile;

    public TransactionLog(Path logFile) {
        this.logFile = logFile;
    }

    /** Log a HOLD operation before seats are transitioned. */
    public void logHold(String showtimeId, String userId, List<String> seatCodes)
            throws IOException {
        String entry = Instant.now() + " HOLD " + showtimeId + " " + userId
                + " " + String.join(",", seatCodes);
        appendEntry(entry);
    }

    /** Log a COMMIT after a booking is fully persisted. */
    public void logCommit(String bookingId) throws IOException {
        String entry = Instant.now() + " COMMIT " + bookingId;
        appendEntry(entry);
    }

    /** Log a ROLLBACK when seats are released after failure. */
    public void logRollback(String showtimeId, List<String> seatCodes) throws IOException {
        String entry = Instant.now() + " ROLLBACK " + showtimeId
                + " " + String.join(",", seatCodes);
        appendEntry(entry);
    }

    /** Read all log entries. */
    public List<String> readAll() throws IOException {
        if (!Files.exists(logFile)) {
            return Collections.emptyList();
        }
        List<String> lines = Files.readAllLines(logFile, StandardCharsets.UTF_8);
        List<String> entries = new ArrayList<>();
        for (String line : lines) {
            if (!line.isBlank()) {
                entries.add(line);
            }
        }
        return entries;
    }

    /**
     * Find uncommitted HOLDs — HOLD entries with no matching ROLLBACK.
     *
     * <p>Matching key is {@code showtimeId + seats}. The HOLD format is
     * {@code <ts> HOLD <showtimeId> <userId> <seats>} and ROLLBACK is
     * {@code <ts> ROLLBACK <showtimeId> <seats>}.
     *
     * @return list of raw HOLD entries that lack a corresponding ROLLBACK
     */
    public List<String> findUncommittedHolds() throws IOException {
        List<String> entries = readAll();

        // Collect rollback keys as "showtimeId|seats"
        java.util.Set<String> rolledBack = new java.util.HashSet<>();
        for (String entry : entries) {
            // Format: <timestamp> ROLLBACK <showtimeId> <seats>
            String[] parts = entry.split(" ");
            if (parts.length >= 4 && "ROLLBACK".equals(parts[1])) {
                rolledBack.add(parts[2] + "|" + parts[3]);
            }
        }

        List<String> uncommitted = new ArrayList<>();
        for (String entry : entries) {
            // Format: <timestamp> HOLD <showtimeId> <userId> <seats>
            String[] parts = entry.split(" ");
            if (parts.length >= 5 && "HOLD".equals(parts[1])) {
                String key = parts[2] + "|" + parts[4];
                if (!rolledBack.contains(key)) {
                    uncommitted.add(entry);
                }
            }
        }
        return uncommitted;
    }

    /** Clear the log file (used after successful recovery). */
    public void clear() throws IOException {
        if (Files.exists(logFile)) {
            Files.writeString(logFile, "", StandardCharsets.UTF_8,
                    StandardOpenOption.TRUNCATE_EXISTING);
        }
    }

    private void appendEntry(String entry) throws IOException {
        Path parent = logFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(logFile, entry + "\n", StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        log.debug("WAL: {}", entry);
    }
}

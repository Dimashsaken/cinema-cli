package com.cinebook.infra;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvAppendAdapterTest {

    private CsvAppendAdapter adapter;
    private static final String HEADER = "bookingId,userId,showtimeId,seats,totalPrice,status,createdAt,version";

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        adapter = new CsvAppendAdapter();
    }

    @Test
    void readAll_nonExistentFile_returnsEmpty() throws IOException {
        Path file = tempDir.resolve("nope.csv");
        assertTrue(adapter.readAll(file).isEmpty());
    }

    @Test
    void readAll_headerOnly_returnsEmpty() throws IOException {
        Path file = tempDir.resolve("bookings.csv");
        Files.writeString(file, HEADER + "\n");
        assertTrue(adapter.readAll(file).isEmpty());
    }

    @Test
    void appendAndRead() throws IOException {
        Path file = tempDir.resolve("bookings.csv");
        String row = "BK-001,U001,ST001,\"D6;D7\",200.00,CONFIRMED,2026-04-20T17:35:00Z,1";

        adapter.append(file, HEADER, row);

        List<String[]> rows = adapter.readAll(file);
        assertEquals(1, rows.size());
        assertEquals("BK-001", rows.get(0)[0]);
        assertEquals("U001", rows.get(0)[1]);
    }

    @Test
    void appendMultipleRows() throws IOException {
        Path file = tempDir.resolve("bookings.csv");
        adapter.append(file, HEADER, "BK-001,U001,ST001,\"D6\",100.00,CONFIRMED,2026-04-20T17:35:00Z,1");
        adapter.append(file, HEADER, "BK-002,U002,ST002,\"A1\",150.00,CONFIRMED,2026-04-20T18:00:00Z,1");

        List<String[]> rows = adapter.readAll(file);
        assertEquals(2, rows.size());
        assertEquals("BK-001", rows.get(0)[0]);
        assertEquals("BK-002", rows.get(1)[0]);
    }

    @Test
    void append_createsFileWithHeader() throws IOException {
        Path file = tempDir.resolve("new.csv");
        adapter.append(file, HEADER, "BK-001,U001,ST001,\"D6\",100.00,CONFIRMED,2026-04-20T17:35:00Z,1");

        String header = adapter.readHeader(file);
        assertEquals(HEADER, header);
    }

    @Test
    void parseCsvLine_quotedField() {
        String[] fields = CsvAppendAdapter.parseCsvLine(
                "BK-001,U001,ST001,\"D6;D7\",200.00,CONFIRMED,2026-04-20T17:35:00Z,1");
        assertEquals(8, fields.length);
        assertEquals("D6;D7", fields[3]);
    }

    @Test
    void parseCsvLine_noQuotes() {
        String[] fields = CsvAppendAdapter.parseCsvLine("a,b,c");
        assertEquals(3, fields.length);
        assertEquals("a", fields[0]);
        assertEquals("b", fields[1]);
        assertEquals("c", fields[2]);
    }

    @Test
    void readHeader_nonExistentFile() throws IOException {
        assertNull(adapter.readHeader(tempDir.resolve("nope.csv")));
    }

    @Test
    void readHeader_emptyFile() throws IOException {
        Path file = tempDir.resolve("empty.csv");
        Files.writeString(file, "");
        assertNull(adapter.readHeader(file));
    }

    @Test
    void append_createsParentDirs() throws IOException {
        Path file = tempDir.resolve("sub/dir/bookings.csv");
        adapter.append(file, HEADER, "BK-001,U001,ST001,\"D6\",100.00,CONFIRMED,2026-04-20T17:35:00Z,1");
        assertTrue(Files.exists(file));
    }
}

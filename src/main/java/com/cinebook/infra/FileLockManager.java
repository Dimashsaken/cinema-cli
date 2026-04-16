package com.cinebook.infra;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * File-level locking for cross-process safety.
 *
 * <p>Uses Java NIO {@link FileLock} to coordinate access when multiple
 * JVM processes might access the same data files. Within a single process,
 * in-memory locks (ReentrantLock per Showtime) are the primary mechanism;
 * this class adds a second layer for multi-process scenarios.
 */
public class FileLockManager {

    private static final Logger log = LoggerFactory.getLogger(FileLockManager.class);

    /**
     * Acquire an exclusive lock on a file, execute the action, then release.
     *
     * @param file   the file to lock (a .lock file is created alongside it)
     * @param action the action to run while holding the lock
     * @throws IOException if locking or I/O fails
     */
    public void withLock(Path file, IOAction action) throws IOException {
        Path lockFile = file.resolveSibling(file.getFileName() + ".lock");
        Path parent = lockFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (RandomAccessFile raf = new RandomAccessFile(lockFile.toFile(), "rw");
             FileChannel channel = raf.getChannel();
             FileLock lock = channel.lock()) {
            log.debug("Acquired file lock: {}", lockFile);
            action.run();
        } finally {
            log.debug("Released file lock: {}", lockFile);
        }
    }

    /** An action that may throw IOException. */
    @FunctionalInterface
    public interface IOAction {
        void run() throws IOException;
    }
}

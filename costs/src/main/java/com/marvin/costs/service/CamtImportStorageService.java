package com.marvin.costs.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Persists uploaded CAMT files into the directory watched by {@link DirectoryWatcher}. */
@Service
public class CamtImportStorageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CamtImportStorageService.class);

    private final Path directoryIn;

    /**
     * Constructs a new {@code CamtImportStorageService}.
     *
     * @param directoryIn the directory that {@link DirectoryWatcher} watches for new CAMT files
     */
    public CamtImportStorageService(@Value("${camt.import.file.in}") String directoryIn) {
        this.directoryIn = Path.of(directoryIn);
        ensureDirectory();
    }

    /**
     * Writes the given file bytes into the watched import directory.
     *
     * @param fileName the name of the file to write
     * @param content  the raw bytes of the file
     * @throws IllegalArgumentException if the resolved target path escapes the watched import directory
     */
    public void store(String fileName, byte[] content) {
        final Path target = resolveWithinImportDirectory(fileName);
        try {
            Files.write(target, content);
            LOGGER.info("Stored uploaded CAMT file {} for import.", fileName);
        } catch (IOException e) {
            LOGGER.error("Could not store uploaded CAMT file {}!", fileName, e);
            throw new UncheckedIOException(e);
        }
    }

    private Path resolveWithinImportDirectory(String fileName) {
        final Path normalizedImportDirectory = directoryIn.toAbsolutePath().normalize();
        final Path target = normalizedImportDirectory.resolve(fileName).normalize();
        if (!target.startsWith(normalizedImportDirectory)) {
            throw new IllegalArgumentException("File name escapes the import directory: " + fileName);
        }
        return target;
    }

    private void ensureDirectory() {
        try {
            if (!Files.exists(directoryIn)) {
                Files.createDirectories(directoryIn);
            }
        } catch (IOException e) {
            LOGGER.error("Could not create CAMT import directory!", e);
            throw new UncheckedIOException(e);
        }
    }
}

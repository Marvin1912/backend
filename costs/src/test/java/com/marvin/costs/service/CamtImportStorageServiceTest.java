package com.marvin.costs.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CamtImportStorageServiceTest {

    @TempDir
    private Path tempDir;

    @Test
    void shouldCreateImportDirectoryIfMissing() {
        final Path importDir = tempDir.resolve("camt-in");
        assertThat(importDir).doesNotExist();

        new CamtImportStorageService(importDir.toString());

        assertThat(importDir).exists().isDirectory();
    }

    @Test
    void shouldNotFailWhenImportDirectoryAlreadyExists() throws IOException {
        final Path importDir = tempDir.resolve("camt-in");
        Files.createDirectories(importDir);

        new CamtImportStorageService(importDir.toString());

        assertThat(importDir).exists().isDirectory();
    }

    @Test
    void shouldWriteFileBytesIntoImportDirectory() throws IOException {
        final Path importDir = tempDir.resolve("camt-in");
        final CamtImportStorageService service = new CamtImportStorageService(importDir.toString());
        final byte[] content = "fake-camt-zip-content".getBytes();

        service.store("statement.zip", content);

        final Path storedFile = importDir.resolve("statement.zip");
        assertThat(storedFile).exists();
        assertThat(Files.readAllBytes(storedFile)).isEqualTo(content);
    }

    @Test
    void shouldOverwriteExistingFileWithSameName() throws IOException {
        final Path importDir = tempDir.resolve("camt-in");
        final CamtImportStorageService service = new CamtImportStorageService(importDir.toString());
        service.store("statement.zip", "old-content".getBytes());

        service.store("statement.zip", "new-content".getBytes());

        final Path storedFile = importDir.resolve("statement.zip");
        assertThat(Files.readString(storedFile)).isEqualTo("new-content");
    }

    @Test
    void shouldRejectFileNameThatEscapesImportDirectoryViaPathTraversal() {
        final Path importDir = tempDir.resolve("camt-in");
        final CamtImportStorageService service = new CamtImportStorageService(importDir.toString());
        final String traversalFileName = "../../../etc/passwd-traversal.zip";

        assertThatThrownBy(() -> service.store(traversalFileName, "malicious-content".getBytes()))
                .isInstanceOf(IllegalArgumentException.class);

        final Path escapedTarget = importDir.resolve(traversalFileName).normalize();
        assertThat(escapedTarget).doesNotExist();
    }
}

package com.marvin.costs.service;

import com.marvin.costs.model.event.NewFileEvent;
import jakarta.annotation.PreDestroy;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Watches a directory for new cost import files and publishes {@link NewFileEvent} events. */
@Component
public class DirectoryWatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(DirectoryWatcher.class);

    private static final AtomicBoolean IS_RUNNING = new AtomicBoolean();

    private final ApplicationEventPublisher eventPublisher;
    private final Path directoryIn;
    private final Path directoryDone;

    /**
     * Constructs a new {@code DirectoryWatcher}.
     *
     * @param eventPublisher the Spring application event publisher
     * @param directoryIn    the path of the directory to watch for new files
     * @param directoryDone  the path of the directory to move processed files to
     */
    public DirectoryWatcher(
            ApplicationEventPublisher eventPublisher,
            @Value("${camt.import.file.in}") String directoryIn,
            @Value("${camt.import.file.done}") String directoryDone
    ) {
        this.eventPublisher = eventPublisher;
        this.directoryIn = Path.of(directoryIn);
        this.directoryDone = Path.of(directoryDone);
    }

    /** Starts the watch service when the application is ready. */
    @EventListener(ApplicationReadyEvent.class)
    public void startUpWatchService() {
        new Thread(() -> {
            IS_RUNNING.set(true);
            try {
                watchDirectory();
            } catch (Exception e) {
                LOGGER.error("Error starting WatchService!", e);
            }
        }).start();
    }

    /** Stops the watch service on application shutdown. */
    @PreDestroy
    public void stopUpWatchService() {
        IS_RUNNING.set(false);
    }

    private void watchDirectory() throws Exception {
        final WatchService watchService = FileSystems.getDefault().newWatchService();
        directoryIn.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY);

        WatchKey key;
        while (IS_RUNNING.get() && (key = watchService.take()) != null) {
            for (WatchEvent<?> event : key.pollEvents()) {
                final Path file = Paths.get(directoryIn.toString(),
                        event.context().toString());
                if (!Files.isDirectory(file)) {
                    try {
                        final Path moved = Files.move(file,
                                directoryDone.resolve(file.getFileName()));
                        LOGGER.info("Moved {} to {}!", file.getFileName(), moved);
                        eventPublisher.publishEvent(new NewFileEvent(moved));
                    } catch (Exception e) {
                        LOGGER.error("Failed to move file {} to done directory", file.getFileName(), e);
                    }
                }
            }
            key.reset();
        }
    }
}

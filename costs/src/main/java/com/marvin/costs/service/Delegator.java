package com.marvin.costs.service;

import com.marvin.camt.maintenance.DataMaintainer;
import com.marvin.camt.model.book_entry.BookingEntryDTO;
import com.marvin.camt.parser.CamtFileParser;
import com.marvin.camt.parser.DocumentUnmarshaller;
import com.marvin.costs.importer.DailyCostImportService;
import com.marvin.costs.importer.MonthlyCostImportService;
import com.marvin.costs.importer.SalaryImportService;
import com.marvin.costs.importer.SpecialCostImportService;
import com.marvin.costs.model.event.NewFileEvent;
import java.io.InputStream;
import java.nio.file.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/** Listens for {@link NewFileEvent} and delegates CAMT file processing to cost import services. */
@Component
public class Delegator {

    private static final Logger LOGGER = LoggerFactory.getLogger(Delegator.class);

    private final CamtFileParser camtFileParser;
    private final DocumentUnmarshaller documentUnmarshaller;
    private final DataMaintainer maintainer;
    private final MonthlyCostImportService monthlyCostImportService;
    private final SpecialCostImportService specialCostImportService;
    private final SalaryImportService salaryImportService;
    private final DailyCostImportService dailyCostImportService;

    /**
     * Constructs a new {@code Delegator}.
     *
     * @param camtFileParser            the CAMT file parser
     * @param documentUnmarshaller      the CAMT document unmarshaller
     * @param maintainer                the data maintainer for CAMT entries
     * @param monthlyCostImportService  the monthly cost import service
     * @param specialCostImportService  the special cost import service
     * @param salaryImportService       the salary import service
     * @param dailyCostImportService    the daily cost import service
     */
    public Delegator(
            CamtFileParser camtFileParser,
            DocumentUnmarshaller documentUnmarshaller,
            DataMaintainer maintainer,
            MonthlyCostImportService monthlyCostImportService,
            SpecialCostImportService specialCostImportService,
            SalaryImportService salaryImportService,
            DailyCostImportService dailyCostImportService
    ) {
        this.camtFileParser = camtFileParser;
        this.documentUnmarshaller = documentUnmarshaller;
        this.maintainer = maintainer;
        this.monthlyCostImportService = monthlyCostImportService;
        this.specialCostImportService = specialCostImportService;
        this.salaryImportService = salaryImportService;
        this.dailyCostImportService = dailyCostImportService;
    }

    /**
     * Handles a new CAMT file event by parsing it and delegating to cost import services.
     *
     * @param newFileEvent the event containing the path of the new CAMT file
     * @throws Exception if the file cannot be read or parsed
     */
    @EventListener(NewFileEvent.class)
    public void startUpWatchService(NewFileEvent newFileEvent) throws Exception {
        LOGGER.info("Processing CAMT file: {}", newFileEvent.path().getFileName());

        final Flux<BookingEntryDTO> bookingEntryStream = getBookingEntries(
                Files.newInputStream(newFileEvent.path()))
                .publish().autoConnect(4);

        monthlyCostImportService.importMonthlyCost(bookingEntryStream)
                .subscribe(LOGGER::info, e -> LOGGER.error("Monthly cost import failed", e));

        specialCostImportService.importSpecialCost(bookingEntryStream)
                .subscribe(LOGGER::info, e -> LOGGER.error("Special cost import failed", e));

        salaryImportService.importSalary(bookingEntryStream)
                .subscribe(LOGGER::info, e -> LOGGER.error("Salary import failed", e));

        dailyCostImportService.importDailyCost(bookingEntryStream)
                .subscribe(LOGGER::info, e -> LOGGER.error("Daily cost import failed", e));
    }

    private Flux<BookingEntryDTO> getBookingEntries(InputStream inputStream) {
        try {
            return documentUnmarshaller.unmarshallFile(camtFileParser.unzipFile(inputStream))
                    .flatMap(maintainer::maintainData);
        } catch (Exception e) {
            LOGGER.error("Error getting book entries!", e);
            return Flux.empty();
        }
    }
}

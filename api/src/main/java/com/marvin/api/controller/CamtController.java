package com.marvin.api.controller;

import com.marvin.costs.importer.DailyCostImportService;
import com.marvin.costs.service.CamtImportStorageService;
import com.marvin.camt.model.book_entry.BookingEntryDTO;
import com.marvin.camt.model.book_entry.BookingsDTO;
import com.marvin.camt.model.book_entry.CreditDebitCodeDTO;
import com.marvin.camt.model.book_entry.MonthlyBookingEntriesDTO;
import com.marvin.camt.parser.CamtFileParser;
import com.marvin.camt.parser.DocumentUnmarshaller;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@Tag(name = "CAMT Processing", description = "API for processing CAMT banking files")
public class CamtController {

    private final CamtFileParser camtFileParser;
    private final DocumentUnmarshaller documentUnmarshaller;
    private final CamtImportStorageService camtImportStorageService;

    public CamtController(CamtFileParser camtFileParser,
            DocumentUnmarshaller documentUnmarshaller,
            CamtImportStorageService camtImportStorageService) {
        this.camtFileParser = camtFileParser;
        this.documentUnmarshaller = documentUnmarshaller;
        this.camtImportStorageService = camtImportStorageService;
    }

    private static String replaceSpaces(String value) {
        return value.replaceAll("\\s+", " ");
    }

    @Operation(
        summary = "Parse CAMT booking entries",
        description = "Uploads a zip file containing CAMT.052.001.08 XML files, parses them, and returns categorized booking entries grouped by month"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully parsed CAMT files",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = BookingsDTO.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid file format or parsing error",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
        )
    })
    @PostMapping(
            path = "/camt-entries",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<BookingsDTO> bookings(
            @Parameter(description = "Zip file containing CAMT XML files", required = true)
            @RequestPart("file") Mono<FilePart> fileMono) {

        return fileMono.flatMap(file -> {

            if (!Objects.requireNonNull(file.filename()).toLowerCase().endsWith(".zip")) {
                return Mono.error(new IllegalArgumentException("Only zip files are allowed"));
            }

            return DataBufferUtils.join(file.content())
                    .flatMapMany(dataBuffer -> {

                        final Flux<ByteArrayOutputStream> using = Flux.using(
                                dataBuffer::asInputStream,
                                camtFileParser::unzipFile,
                                inputStream -> {
                                    try {
                                        inputStream.close();
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                        );

                        DataBufferUtils.release(dataBuffer);

                        return using;
                    })
                    .flatMap(fileContent -> unmarshallBookings(fileContent)
                            .map(bookingEntryDTO -> new BookingEntryDTO(
                                    bookingEntryDTO.creditDebitCode(),
                                    bookingEntryDTO.entryInfo(),
                                    bookingEntryDTO.amount(),
                                    bookingEntryDTO.bookingDate(),
                                    bookingEntryDTO.firstOfMonth(),
                                    replaceSpaces(bookingEntryDTO.debitName()),
                                    bookingEntryDTO.debitIban(),
                                    replaceSpaces(bookingEntryDTO.creditName()),
                                    bookingEntryDTO.creditIban(),
                                    bookingEntryDTO.additionalInfo()
                            ))
                    )
                    .collectList()
                    .map(this::getBookingsDTO);
        });
    }

    @Operation(
        summary = "Import a CAMT booking file",
        description = "Uploads a zip file containing CAMT.052.001.08 XML files and stores it in the "
                + "directory watched for asynchronous import. The file is not parsed or previewed here."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "202",
            description = "File accepted and queued for asynchronous import"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid file format",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
        )
    })
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(
            path = "/camt-entries/import",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public Mono<Void> importBookings(
            @Parameter(description = "Zip file containing CAMT XML files", required = true)
            @RequestPart("file") Mono<FilePart> fileMono) {

        return fileMono.flatMap(file -> {

            final String filename = Objects.requireNonNull(file.filename());
            if (!filename.toLowerCase().endsWith(".zip")) {
                return Mono.error(new IllegalArgumentException("Only zip files are allowed"));
            }

            return DataBufferUtils.join(file.content())
                    .doOnNext(dataBuffer -> {
                        final byte[] bytes = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(bytes);
                        DataBufferUtils.release(dataBuffer);
                        camtImportStorageService.store(filename, bytes);
                    })
                    .then();
        });
    }

    private Flux<BookingEntryDTO> unmarshallBookings(ByteArrayOutputStream fileContent) {
        try {
            return documentUnmarshaller.unmarshallFile(Flux.just(fileContent));
        } catch (Exception e) {
            return Flux.error(e);
        }
    }

    private BookingsDTO getBookingsDTO(List<BookingEntryDTO> bookings) {

        final List<MonthlyBookingEntriesDTO> dtos = bookings.stream()
                .collect(Collectors.groupingBy(BookingEntryDTO::firstOfMonth))
                .entrySet().stream()
                .map(entry -> {

                    final List<BookingEntryDTO> usualBookings = new ArrayList<>();
                    final List<BookingEntryDTO> dailyCosts = new ArrayList<>();
                    final List<BookingEntryDTO> incomes = new ArrayList<>();

                    entry.getValue().forEach(dto -> {
                        if (DailyCostImportService.PATTERN.matcher(dto.creditName()).matches()) {
                            dailyCosts.add(dto);
                        } else if (CreditDebitCodeDTO.CRDT == dto.creditDebitCode()) {
                            incomes.add(dto);
                        } else {
                            usualBookings.add(dto);
                        }
                    });

                    final LocalDate date = entry.getKey();

                    return new MonthlyBookingEntriesDTO(
                            date.getYear(), date.getMonth().getValue(),
                            usualBookings, dailyCosts, incomes
                    );
                }).toList();

        return new BookingsDTO(dtos);
    }
}

package com.marvin.grocery.ocr;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/** Tesseract-backed OCR provider. Runs OCR on the bounded-elastic scheduler to avoid blocking the event loop. */
@Service
@Primary
public class TesseractOcrProvider implements OcrProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(TesseractOcrProvider.class);

    private final String tesseractDatapath;

    /**
     * Creates a new TesseractOcrProvider with the configured tessdata path.
     *
     * @param tesseractDatapath path to the tessdata directory
     */
    public TesseractOcrProvider(
            @Value("${grocery.tesseract.datapath:/usr/share/tesseract-ocr/5/tessdata}") String tesseractDatapath) {
        this.tesseractDatapath = tesseractDatapath;
    }

    @Override
    public Mono<String> extractText(byte[] imageBytes) {
        return Mono.fromCallable(() -> runOcr(imageBytes))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Executes Tesseract OCR on the given image bytes.
     *
     * @param imageBytes raw image bytes to process
     * @return extracted text
     * @throws IOException        if the image cannot be read
     * @throws TesseractException if Tesseract processing fails
     */
    private String runOcr(byte[] imageBytes) throws IOException, TesseractException {
        final Tesseract tesseract = new Tesseract();
        tesseract.setDatapath(tesseractDatapath);
        tesseract.setLanguage("deu");
        tesseract.setPageSegMode(6);

        final BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
        LOGGER.debug("Running Tesseract OCR on image of size {} bytes", imageBytes.length);
        return tesseract.doOCR(image);
    }
}

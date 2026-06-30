package com.marvin.nutrition.service;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.marvin.nutrition.dto.DaySummaryDTO;
import com.marvin.nutrition.dto.MealEntryDTO;
import com.marvin.nutrition.dto.ProfileDTO;
import com.marvin.nutrition.dto.SportActivityDTO;
import com.marvin.nutrition.dto.TargetsDTO;
import com.marvin.nutrition.dto.WeightEntryDTO;
import com.marvin.nutrition.entity.MealType;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Assembles nutrition diary data from four concurrent sources and renders it as a
 * PDF document structured for LLM-based meal plan generation.
 */
@Service
public class PdfExportService {

    private static final Font FONT_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
    private static final Font FONT_SECTION = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
    private static final Font FONT_SUBSECTION = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
    private static final Font FONT_BODY = FontFactory.getFont(FontFactory.HELVETICA, 10);
    private static final Font FONT_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
    private static final Font FONT_SMALL = FontFactory.getFont(FontFactory.HELVETICA, 8);
    private static final float TABLE_WIDTH_PCT = 100f;
    private static final int MEAL_TABLE_COLS = 6;
    private static final int ACTIVITY_TABLE_COLS = 3;
    private static final int WEIGHT_TABLE_COLS = 2;
    private static final double ON_TARGET_TOLERANCE = 0.10;
    private static final int SUMMARY_DECIMAL_SCALE = 1;
    private static final Color HEADER_BG_COLOR = new Color(230, 230, 230);

    private final NutritionProfileService profileService;
    private final NutritionTargetService nutritionTargetService;
    private final WeightService weightService;
    private final MealEntryService mealEntryService;

    /**
     * Creates a new PdfExportService with the required data-source dependencies.
     *
     * @param profileService         service for fetching the user's nutrition profile
     * @param nutritionTargetService service for computing daily nutrition targets
     * @param weightService          service for fetching body-weight entries
     * @param mealEntryService       service for fetching daily meal entry summaries
     */
    public PdfExportService(
            NutritionProfileService profileService,
            NutritionTargetService nutritionTargetService,
            WeightService weightService,
            MealEntryService mealEntryService) {
        this.profileService = profileService;
        this.nutritionTargetService = nutritionTargetService;
        this.weightService = weightService;
        this.mealEntryService = mealEntryService;
    }

    /**
     * Generates a PDF nutrition diary export for the given date range.
     * Profile, targets, weight history, and day-by-day meal logs are fetched concurrently.
     * If the profile or targets are unavailable their sections are omitted without failing the export.
     *
     * @param from the first date to include (inclusive)
     * @param to   the last date to include (inclusive)
     * @return a Mono emitting the raw PDF bytes
     */
    public Mono<byte[]> generatePdf(LocalDate from, LocalDate to) {
        final Mono<Optional<ProfileDTO>> profileMono =
                profileService.getProfile().map(Optional::of).onErrorReturn(Optional.empty());
        final Mono<Optional<TargetsDTO>> targetsMono =
                nutritionTargetService.getTargets().map(Optional::of).onErrorReturn(Optional.empty());
        final Mono<List<WeightEntryDTO>> weightsMono = weightService.findAll().collectList();
        final Mono<List<DaySummaryDTO>> daysMono = mealEntryService.getDays(from, to);

        return Mono.zip(profileMono, targetsMono, weightsMono, daysMono)
                .flatMap(tuple -> {
                    final PdfContext ctx = new PdfContext(
                            tuple.getT1(), tuple.getT2(), tuple.getT3(), tuple.getT4());
                    return Mono.fromCallable(() -> buildPdf(ctx, from, to))
                            .subscribeOn(Schedulers.boundedElastic());
                });
    }

    /**
     * Builds the raw PDF bytes by opening a document, adding four sections, and closing it.
     *
     * @param ctx  the assembled data context
     * @param from the first date of the export range
     * @param to   the last date of the export range
     * @return the raw PDF bytes
     * @throws DocumentException if the PDF library encounters an error while building the document
     */
    private byte[] buildPdf(PdfContext ctx, LocalDate from, LocalDate to) throws DocumentException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final Document doc = new Document();
        try {
            PdfWriter.getInstance(doc, baos);
            doc.open();
            doc.add(new Paragraph("Nutrition Diary Export", FONT_TITLE));
            doc.add(new Paragraph("Period: " + from + " - " + to, FONT_BODY));
            doc.add(Chunk.NEWLINE);
            addProfileSection(doc, ctx);
            addPeriodSummarySection(doc, ctx, from, to);
            addWeightTrendSection(doc, ctx, from, to);
            addDailyLogSection(doc, ctx);
        } finally {
            doc.close();
        }
        return baos.toByteArray();
    }

    /**
     * Adds the profile and targets section (section 1) to the document.
     *
     * @param doc the document to write to
     * @param ctx the PDF context containing profile, target, and weight data
     * @throws DocumentException if the PDF library encounters an error
     */
    private void addProfileSection(Document doc, PdfContext ctx) throws DocumentException {
        doc.add(new Paragraph("1. Profile & Targets", FONT_SECTION));
        if (ctx.profile().isEmpty()) {
            doc.add(new Paragraph("Profile not available.", FONT_BODY));
            doc.add(Chunk.NEWLINE);
            return;
        }
        final ProfileDTO p = ctx.profile().get();
        final int age = Period.between(p.birthDate(), LocalDate.now()).getYears();
        doc.add(new Paragraph(
                "Sex: " + p.sex() + "  |  Age: " + age + " yrs  |  Height: " + p.heightCm() + " cm",
                FONT_BODY));
        doc.add(new Paragraph(
                "Activity Level: " + p.activityLevel() + "  |  Goal: " + p.goal(), FONT_BODY));
        if (ctx.targets().isPresent()) {
            final TargetsDTO t = ctx.targets().get();
            doc.add(new Paragraph("BMR: " + t.bmr() + " kcal  |  Maintenance: " + t.maintenanceKcal()
                    + " kcal  |  Target: " + t.targetKcal() + " kcal", FONT_BODY));
            doc.add(new Paragraph("Protein: " + t.proteinG() + " g  |  Fat: " + t.fatG()
                    + " g  |  Carbs: " + t.carbsG() + " g", FONT_BODY));
        }
        if (!ctx.weights().isEmpty()) {
            // ctx.weights() is in descending date order (from WeightService.findAll())
            final WeightEntryDTO latest = ctx.weights().get(0);
            doc.add(new Paragraph(
                    "Latest Weight: " + latest.weightKg() + " kg  (" + latest.entryDate() + ")",
                    FONT_BODY));
        }
        doc.add(Chunk.NEWLINE);
    }

    /**
     * Adds the period summary section (section 2) covering tracked-day statistics to the document.
     *
     * @param doc  the document to write to
     * @param ctx  the PDF context containing the day summaries
     * @param from the start of the export period
     * @param to   the end of the export period
     * @throws DocumentException if the PDF library encounters an error
     */
    private void addPeriodSummarySection(
            Document doc, PdfContext ctx, LocalDate from, LocalDate to) throws DocumentException {
        doc.add(new Paragraph("2. Period Summary", FONT_SECTION));
        final List<DaySummaryDTO> trackedDays = ctx.days().stream()
                .filter(d -> !d.entries().isEmpty())
                .toList();
        final long calendarDays = from.datesUntil(to.plusDays(1)).count();
        doc.add(new Paragraph(
                "Days tracked: " + trackedDays.size() + " / " + calendarDays, FONT_BODY));
        if (!trackedDays.isEmpty()) {
            renderPeriodAverages(doc, trackedDays, ctx);
        }
        doc.add(Chunk.NEWLINE);
    }

    /**
     * Renders average kcal statistics and the on-target count for the tracked days.
     *
     * @param doc         the document to write to
     * @param trackedDays days that have at least one meal entry
     * @param ctx         the PDF context (used for the target kcal reference)
     * @throws DocumentException if the PDF library encounters an error
     */
    private void renderPeriodAverages(
            Document doc, List<DaySummaryDTO> trackedDays, PdfContext ctx) throws DocumentException {
        final int count = trackedDays.size();
        final BigDecimal avgKcal = trackedDays.stream()
                .map(d -> d.totals().kcal())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(count), SUMMARY_DECIMAL_SCALE, RoundingMode.HALF_UP);
        final BigDecimal avgBurned = trackedDays.stream()
                .map(DaySummaryDTO::totalKcalBurned)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(count), SUMMARY_DECIMAL_SCALE, RoundingMode.HALF_UP);
        final BigDecimal avgNet = avgKcal.subtract(avgBurned);
        doc.add(new Paragraph("Avg consumed: " + avgKcal + " kcal  |  Avg burned: " + avgBurned
                + " kcal  |  Avg net: " + avgNet + " kcal", FONT_BODY));
        if (ctx.targets().isPresent()) {
            final int targetKcal = ctx.targets().get().targetKcal();
            final long onTarget = countDaysOnTarget(trackedDays, targetKcal);
            doc.add(new Paragraph(
                    "Days on target (±10%): " + onTarget + " / " + count, FONT_BODY));
        }
    }

    /**
     * Counts how many tracked days have consumed kcal within ±10% of the target.
     *
     * @param trackedDays the days to evaluate
     * @param targetKcal  the daily target kilocalories
     * @return the number of days within the tolerance band
     */
    private long countDaysOnTarget(List<DaySummaryDTO> trackedDays, int targetKcal) {
        final BigDecimal tolerance = BigDecimal.valueOf(targetKcal)
                .multiply(BigDecimal.valueOf(ON_TARGET_TOLERANCE));
        return trackedDays.stream()
                .filter(d -> {
                    final BigDecimal net = d.totals().kcal().subtract(d.totalKcalBurned());
                    return net.subtract(BigDecimal.valueOf(targetKcal)).abs()
                            .compareTo(tolerance) <= 0;
                })
                .count();
    }

    /**
     * Adds the weight trend section (section 3), a table of weight entries within the date range.
     *
     * @param doc  the document to write to
     * @param ctx  the PDF context containing weight entries
     * @param from the start of the date range filter
     * @param to   the end of the date range filter
     * @throws DocumentException if the PDF library encounters an error
     */
    private void addWeightTrendSection(
            Document doc, PdfContext ctx, LocalDate from, LocalDate to) throws DocumentException {
        doc.add(new Paragraph("3. Weight Trend", FONT_SECTION));
        final List<WeightEntryDTO> inRange = ctx.weights().stream()
                .filter(w -> !w.entryDate().isBefore(from) && !w.entryDate().isAfter(to))
                .toList();
        if (inRange.isEmpty()) {
            doc.add(new Paragraph("No weight entries recorded in this period.", FONT_BODY));
            doc.add(Chunk.NEWLINE);
            return;
        }
        final PdfPTable table = new PdfPTable(WEIGHT_TABLE_COLS);
        table.setWidthPercentage(TABLE_WIDTH_PCT);
        addHeaderCell(table, "Date");
        addHeaderCell(table, "Weight (kg)");
        for (final WeightEntryDTO w : inRange) {
            table.addCell(new Phrase(w.entryDate().toString(), FONT_BODY));
            table.addCell(new Phrase(w.weightKg().toString(), FONT_BODY));
        }
        doc.add(table);
        doc.add(Chunk.NEWLINE);
    }

    /**
     * Adds the daily log section (section 4), one subsection per calendar day in the range.
     * {@link MealEntryService#getDays} guarantees exactly one {@link DaySummaryDTO} per calendar
     * day, so {@code ctx.days()} is iterated directly.
     *
     * @param doc the document to write to
     * @param ctx the PDF context containing the day summaries
     * @throws DocumentException if the PDF library encounters an error
     */
    private void addDailyLogSection(Document doc, PdfContext ctx) throws DocumentException {
        doc.add(new Paragraph("4. Daily Log", FONT_SECTION));
        for (final DaySummaryDTO day : ctx.days()) {
            renderDaySection(doc, day);
        }
    }

    /**
     * Renders a single day's section: header with targets, meals by type, activities, and totals.
     *
     * @param doc the document to write to
     * @param day the day summary to render
     * @throws DocumentException if the PDF library encounters an error
     */
    private void renderDaySection(Document doc, DaySummaryDTO day) throws DocumentException {
        doc.add(new Paragraph(day.date().toString(), FONT_SUBSECTION));
        if (day.targets() != null) {
            doc.add(new Paragraph("Target: " + day.targets().targetKcal() + " kcal", FONT_SMALL));
        }
        final Map<MealType, List<MealEntryDTO>> byMealType = day.entries().stream()
                .collect(Collectors.groupingBy(MealEntryDTO::mealType));
        for (final MealType mealType : MealType.values()) {
            final List<MealEntryDTO> entries = byMealType.getOrDefault(mealType, List.of());
            if (!entries.isEmpty()) {
                renderMealGroup(doc, mealType, entries);
            }
        }
        if (!day.activities().isEmpty()) {
            renderActivitiesTable(doc, day.activities());
        }
        renderDayTotals(doc, day);
        doc.add(Chunk.NEWLINE);
    }

    /**
     * Renders a 6-column meal table (food, qty, kcal, protein, carbs, fat) for one meal type.
     *
     * @param doc      the document to write to
     * @param mealType the meal category used as a sub-heading
     * @param entries  the entries to render in the table
     * @throws DocumentException if the PDF library encounters an error
     */
    private void renderMealGroup(
            Document doc, MealType mealType, List<MealEntryDTO> entries) throws DocumentException {
        doc.add(new Paragraph(capitalize(mealType.name()), FONT_BOLD));
        final PdfPTable table = new PdfPTable(MEAL_TABLE_COLS);
        table.setWidthPercentage(TABLE_WIDTH_PCT);
        addHeaderCell(table, "Food");
        addHeaderCell(table, "Qty (g)");
        addHeaderCell(table, "Kcal");
        addHeaderCell(table, "Protein (g)");
        addHeaderCell(table, "Carbs (g)");
        addHeaderCell(table, "Fat (g)");
        for (final MealEntryDTO e : entries) {
            final String name = e.foodName() != null ? e.foodName() : e.description();
            table.addCell(new Phrase(name != null ? name : "-", FONT_BODY));
            table.addCell(new Phrase(e.quantityG() != null ? e.quantityG().toString() : "-", FONT_BODY));
            table.addCell(new Phrase(e.kcal().toString(), FONT_BODY));
            table.addCell(new Phrase(e.proteinG().toString(), FONT_BODY));
            table.addCell(new Phrase(e.carbsG().toString(), FONT_BODY));
            table.addCell(new Phrase(e.fatG().toString(), FONT_BODY));
        }
        doc.add(table);
    }

    /**
     * Renders a 3-column activities table (type, description, kcal burned) for a single day.
     *
     * @param doc        the document to write to
     * @param activities the activities to render
     * @throws DocumentException if the PDF library encounters an error
     */
    private void renderActivitiesTable(
            Document doc, List<SportActivityDTO> activities) throws DocumentException {
        doc.add(new Paragraph("Activities", FONT_BOLD));
        final PdfPTable table = new PdfPTable(ACTIVITY_TABLE_COLS);
        table.setWidthPercentage(TABLE_WIDTH_PCT);
        addHeaderCell(table, "Type");
        addHeaderCell(table, "Description");
        addHeaderCell(table, "Kcal Burned");
        for (final SportActivityDTO a : activities) {
            table.addCell(new Phrase(a.activityType().name(), FONT_BODY));
            table.addCell(new Phrase(a.description() != null ? a.description() : "-", FONT_BODY));
            table.addCell(new Phrase(a.kcalBurned().toString(), FONT_BODY));
        }
        doc.add(table);
    }

    /**
     * Renders the day's macro totals and a surplus or deficit annotation relative to the target.
     *
     * @param doc the document to write to
     * @param day the day summary whose totals to render
     * @throws DocumentException if the PDF library encounters an error
     */
    private void renderDayTotals(Document doc, DaySummaryDTO day) throws DocumentException {
        final BigDecimal consumed = day.totals().kcal();
        final BigDecimal burned = day.totalKcalBurned();
        final BigDecimal net = consumed.subtract(burned);
        doc.add(new Paragraph("Totals - Kcal: " + consumed
                + "  Protein: " + day.totals().proteinG() + " g"
                + "  Carbs: " + day.totals().carbsG() + " g"
                + "  Fat: " + day.totals().fatG() + " g", FONT_BODY));
        doc.add(new Paragraph("Burned: " + burned + " kcal  |  Net: " + net + " kcal", FONT_BODY));
        if (day.targets() != null) {
            final BigDecimal surplus = net.subtract(BigDecimal.valueOf(day.targets().targetKcal()));
            final String annotation = surplus.compareTo(BigDecimal.ZERO) >= 0
                    ? "Surplus: +" + surplus + " kcal"
                    : "Deficit: " + surplus + " kcal";
            doc.add(new Paragraph(annotation, FONT_BODY));
        }
    }

    /**
     * Adds a bold, grey-background header cell to the given table.
     *
     * @param table the table to add the cell to
     * @param text  the header label
     */
    private void addHeaderCell(PdfPTable table, String text) {
        final PdfPCell cell = new PdfPCell(new Phrase(text, FONT_BOLD));
        cell.setBackgroundColor(HEADER_BG_COLOR);
        table.addCell(cell);
    }

    /**
     * Returns the input string with its first character uppercased and the rest lowercased.
     *
     * @param s the string to capitalize
     * @return the capitalized string, or the original value if null or empty
     */
    private String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase(Locale.ROOT);
    }

    /**
     * Bundles the concurrently loaded data sources for a single PDF generation run.
     *
     * @param profile the user's nutrition profile, or empty if unavailable
     * @param targets the computed daily targets, or empty if unavailable
     * @param weights all body-weight entries in descending date order
     * @param days    the day-by-day meal summaries for the export range
     */
    private record PdfContext(
            Optional<ProfileDTO> profile,
            Optional<TargetsDTO> targets,
            List<WeightEntryDTO> weights,
            List<DaySummaryDTO> days
    ) { }
}

package utils;

import Models.Chapitre;
import Models.Cours;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PDFExporter {

    // Custom colors matching your dark theme
    private static final DeviceRgb DARK_BLUE = new DeviceRgb(22, 27, 46);      // #161b2e
    private static final DeviceRgb ACCENT_CYAN = new DeviceRgb(0, 212, 255);   // #00d4ff
    private static final DeviceRgb ACCENT_PURPLE = new DeviceRgb(139, 92, 246); // #8b5cf6
    private static final DeviceRgb TEXT_GRAY = new DeviceRgb(148, 163, 178);   // #94a3b8

    /**
     * Export a single Course to PDF
     */
    public static void exportCourseToPDF(Cours cours, File file) throws IOException {
        PdfWriter writer = new PdfWriter(file);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        // Header
        addHeader(document, "COURSE DETAILS");

        // Course Info Table
        Table infoTable = new Table(UnitValue.createPercentArray(new float[]{30, 70}));
        infoTable.setWidth(UnitValue.createPercentValue(100));
        infoTable.setMarginBottom(20);

        addInfoRow(infoTable, "Title:", cours.getTittre());
        addInfoRow(infoTable, "Category:", cours.getCategorie());
        addInfoRow(infoTable, "Level:", cours.getNiveau());
        addInfoRow(infoTable, "Duration:", cours.getDureeEstimee() + " hours");
        addInfoRow(infoTable, "Status:", cours.getStatus());
        addInfoRow(infoTable, "Created:", cours.getDateCreation());

        document.add(infoTable);

        // Description
        if (cours.getDescription() != null && !cours.getDescription().isBlank()) {
            document.add(new Paragraph("Description")
                    .setFontColor(ACCENT_CYAN)
                    .setFontSize(14)
                    .setBold()
                    .setMarginBottom(8));

            document.add(new Paragraph(cours.getDescription())
                    .setFontColor(ColorConstants.WHITE)
                    .setFontSize(11)
                    .setMarginBottom(20));
        }

        // Footer
        addFooter(document);

        document.close();
    }

    /**
     * Export Course with its Chapters to PDF
     */
    public static void exportCourseWithChaptersToPDF(Cours cours, List<Chapitre> chapitres, File file) throws IOException {
        PdfWriter writer = new PdfWriter(file);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        // Header
        addHeader(document, "COURSE & CHAPTERS REPORT");

        // Course Section
        document.add(new Paragraph("Course Information")
                .setFontColor(ACCENT_PURPLE)
                .setFontSize(16)
                .setBold()
                .setMarginBottom(12));

        Table courseTable = new Table(UnitValue.createPercentArray(new float[]{30, 70}));
        courseTable.setWidth(UnitValue.createPercentValue(100));
        courseTable.setMarginBottom(20);

        addInfoRow(courseTable, "Title:", cours.getTittre());
        addInfoRow(courseTable, "Category:", cours.getCategorie());
        addInfoRow(courseTable, "Level:", cours.getNiveau());
        addInfoRow(courseTable, "Duration:", cours.getDureeEstimee() + " hours");
        addInfoRow(courseTable, "Status:", cours.getStatus());

        document.add(courseTable);

        // Description
        if (cours.getDescription() != null && !cours.getDescription().isBlank()) {
            document.add(new Paragraph("Description")
                    .setFontColor(ACCENT_CYAN)
                    .setFontSize(12)
                    .setBold()
                    .setMarginBottom(6));
            document.add(new Paragraph(cours.getDescription())
                    .setFontColor(ColorConstants.WHITE)
                    .setFontSize(10)
                    .setMarginBottom(20));
        }

        // Separator
        document.add(new Paragraph("────────────────────────────────────────")
                .setFontColor(TEXT_GRAY)
                .setMarginBottom(20));

        // Chapters Section
        document.add(new Paragraph("Chapters (" + chapitres.size() + ")")
                .setFontColor(ACCENT_PURPLE)
                .setFontSize(16)
                .setBold()
                .setMarginBottom(12));

        if (chapitres.isEmpty()) {
            document.add(new Paragraph("No chapters available for this course.")
                    .setFontColor(TEXT_GRAY)
                    .setFontSize(11)
                    .setItalic());
        } else {
            int chapterNum = 1;
            for (Chapitre ch : chapitres) {
                addChapterBlock(document, ch, chapterNum++);
            }
        }

        addFooter(document);
        document.close();
    }

    /**
     * Export multiple Courses to PDF (List/Table format)
     */
    public static void exportCoursesListToPDF(List<Cours> courses, File file) throws IOException {
        PdfWriter writer = new PdfWriter(file);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        addHeader(document, "COURSES CATALOG");

        // Summary
        document.add(new Paragraph("Total Courses: " + courses.size())
                .setFontColor(TEXT_GRAY)
                .setFontSize(11)
                .setMarginBottom(15));

        // Table
        Table table = new Table(UnitValue.createPercentArray(new float[]{5, 25, 15, 15, 10, 15, 15}));
        table.setWidth(UnitValue.createPercentValue(100));

        // Headers
        String[] headers = {"#", "Title", "Category", "Level", "Duration", "Status", "Created"};
        for (String header : headers) {
            table.addHeaderCell(createHeaderCell(header));
        }

        // Data rows
        int index = 1;
        for (Cours c : courses) {
            table.addCell(createDataCell(String.valueOf(index++), TextAlignment.CENTER));
            table.addCell(createDataCell(c.getTittre(), TextAlignment.LEFT));
            table.addCell(createDataCell(c.getCategorie(), TextAlignment.CENTER));
            table.addCell(createDataCell(c.getNiveau(), TextAlignment.CENTER));
            table.addCell(createDataCell(c.getDureeEstimee() + "h", TextAlignment.CENTER));
            table.addCell(createStatusCell(c.getStatus()));
            table.addCell(createDataCell(c.getDateCreation() != null ? c.getDateCreation() : "-", TextAlignment.CENTER));
        }

        document.add(table);
        addFooter(document);
        document.close();
    }

    // Helper methods
    private static void addHeader(Document doc, String title) {
        doc.add(new Paragraph("AIVA Learning Platform")
                .setFontColor(ACCENT_CYAN)
                .setFontSize(10)
                .setTextAlignment(TextAlignment.RIGHT));

        doc.add(new Paragraph(title)
                .setFontColor(ColorConstants.WHITE)
                .setFontSize(24)
                .setBold()
                .setMarginBottom(5));

        doc.add(new Paragraph("Generated on: " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                .setFontColor(TEXT_GRAY)
                .setFontSize(10)
                .setMarginBottom(20));
    }

    private static void addFooter(Document doc) {
        doc.add(new Paragraph("\n\n"));
        doc.add(new Paragraph("© 2026 AIVA Learning Platform - All rights reserved")
                .setFontColor(TEXT_GRAY)
                .setFontSize(8)
                .setTextAlignment(TextAlignment.CENTER));
    }

    private static void addInfoRow(Table table, String label, String value) {
        Cell labelCell = new Cell().add(new Paragraph(label))
                .setFontColor(TEXT_GRAY)
                .setFontSize(11)
                .setBold()
                .setBorder(Border.NO_BORDER)
                .setPadding(5);

        Cell valueCell = new Cell().add(new Paragraph(value != null ? value : "-"))
                .setFontColor(ColorConstants.WHITE)
                .setFontSize(11)
                .setBorder(Border.NO_BORDER)
                .setPadding(5);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private static void addChapterBlock(Document doc, Chapitre ch, int number) {
        // Chapter header
        doc.add(new Paragraph("Chapter " + number + ": " + ch.getTitre())
                .setFontColor(ACCENT_CYAN)
                .setFontSize(13)
                .setBold()
                .setMarginBottom(4));

        // Order badge
        doc.add(new Paragraph("Order: " + ch.getOrdre())
                .setFontColor(ACCENT_PURPLE)
                .setFontSize(9)
                .setMarginBottom(6));

        // Content
        if (ch.getContenu() != null && !ch.getContenu().isBlank()) {
            doc.add(new Paragraph(ch.getContenu())
                    .setFontColor(ColorConstants.WHITE)
                    .setFontSize(10)
                    .setMarginBottom(6));
        }

        // Exercise indicator
        boolean hasExercise = ch.getExercise() != null && !ch.getExercise().isBlank();
        doc.add(new Paragraph(hasExercise ? "✓ Includes Exercise" : "No Exercise")
                .setFontColor(hasExercise ? new DeviceRgb(16, 185, 129) : TEXT_GRAY)
                .setFontSize(9)
                .setItalic()
                .setMarginBottom(15));
    }

    private static Cell createHeaderCell(String text) {
        return new Cell().add(new Paragraph(text))
                .setBackgroundColor(DARK_BLUE)
                .setFontColor(ACCENT_CYAN)
                .setFontSize(10)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setPadding(8);
    }

    private static Cell createDataCell(String text, TextAlignment align) {
        return new Cell().add(new Paragraph(text))
                .setFontColor(ColorConstants.WHITE)
                .setFontSize(10)
                .setTextAlignment(align)
                .setPadding(6);
    }

    private static Cell createStatusCell(String status) {
        DeviceRgb color;
        if (status == null) {
            color = TEXT_GRAY;
        } else {
            switch (status.toLowerCase()) {
                case "actif" -> color = new DeviceRgb(0, 212, 255);
                case "inactif" -> color = new DeviceRgb(239, 68, 68);
                case "en attente" -> color = new DeviceRgb(245, 158, 11);
                case "en cours" -> color = new DeviceRgb(16, 185, 129);
                default -> color = TEXT_GRAY;
            }
        }

        return new Cell().add(new Paragraph(status != null ? status : "-"))
                .setFontColor(color)
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER)
                .setPadding(6);
    }
}
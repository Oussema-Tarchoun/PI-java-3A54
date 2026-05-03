package Services;

import Models.Aliment;
import Models.Repas;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.Image;

import java.io.FileOutputStream;
import java.util.List;

public class PdfService {

    public String exportRepasPdf(Repas r, List<Aliment> aliments, String outputPath) throws Exception {

        Document doc = new Document(PageSize.A4, 40, 40, 50, 50);
        PdfWriter.getInstance(doc, new FileOutputStream(outputPath));
        doc.open();

        // Logo
        try {
            java.net.URL logoUrl = getClass().getResource("/images/img.png");
            if (logoUrl != null) {
                Image logo = Image.getInstance(logoUrl);
                logo.scaleToFit(60, 60);
                logo.setAlignment(Element.ALIGN_CENTER);
                doc.add(logo);
            }
        } catch (Exception ignored) {}

        // Colors
        BaseColor dark     = new BaseColor(10, 14, 26);
        BaseColor teal     = new BaseColor(15, 118, 110);
        BaseColor white    = BaseColor.WHITE;
        BaseColor gray     = new BaseColor(100, 116, 139);
        BaseColor lightBg  = new BaseColor(22, 27, 46);
        BaseColor orange   = new BaseColor(249, 115, 22);

        // Fonts
        Font fontTitle   = new Font(Font.FontFamily.HELVETICA, 22, Font.BOLD,   white);
        Font fontSub     = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, new BaseColor(167, 243, 208));
        Font fontSection = new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD,   white);
        Font fontNormal  = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, new BaseColor(203, 213, 225));
        Font fontMeta    = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, gray);
        Font fontCal     = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD,   orange);

        // ── HEADER ──
        PdfPTable header = new PdfPTable(1);
        header.setWidthPercentage(100);
        PdfPCell headerCell = new PdfPCell();
        headerCell.setBackgroundColor(teal);
        headerCell.setPadding(20);
        headerCell.setBorder(Rectangle.NO_BORDER);

        Paragraph brand = new Paragraph("AIVA", new Font(Font.FontFamily.HELVETICA, 28, Font.BOLD, white));
        brand.setAlignment(Element.ALIGN_CENTER);
        headerCell.addElement(brand);

        Paragraph brandSub = new Paragraph("Rapport détaillé du repas", fontSub);
        brandSub.setAlignment(Element.ALIGN_CENTER);
        headerCell.addElement(brandSub);

        header.addCell(headerCell);
        doc.add(header);
        doc.add(Chunk.NEWLINE);

        // ── REPAS INFO CARD ──
        PdfPTable infoCard = new PdfPTable(2);
        infoCard.setWidthPercentage(100);
        infoCard.setWidths(new float[]{1, 1});

        // Nom + calories
        PdfPCell nomCell = new PdfPCell();
        nomCell.setBackgroundColor(lightBg);
        nomCell.setPadding(16);
        nomCell.setBorder(Rectangle.NO_BORDER);
        nomCell.addElement(new Paragraph("🍽  " + r.getNom(), fontTitle));
        nomCell.addElement(new Paragraph(r.getType() != null ? r.getType() : "—", fontMeta));
        infoCard.addCell(nomCell);

        PdfPCell calCell = new PdfPCell();
        calCell.setBackgroundColor(lightBg);
        calCell.setPadding(16);
        calCell.setBorder(Rectangle.NO_BORDER);
        calCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        calCell.addElement(new Paragraph(r.getCalories() + " kcal", fontCal));
        String score = r.getCalories() <= 400 ? "Score : Excellent" : r.getCalories() <= 700 ? "Score : Bon" : "Score : Élevé";
        calCell.addElement(new Paragraph(score, fontMeta));
        infoCard.addCell(calCell);

        doc.add(infoCard);
        doc.add(Chunk.NEWLINE);

        // ── META INFO ──
        PdfPTable meta = new PdfPTable(3);
        meta.setWidthPercentage(100);
        meta.setWidths(new float[]{1, 1, 1});

        meta.addCell(metaBox("📅 Date", r.getDate() != null ? r.getDate().toString() : "—", lightBg, fontSection, fontNormal));
        meta.addCell(metaBox("🕐 Heure", r.getHeure() != null ? r.getHeure().toString().substring(0,5) : "—", lightBg, fontSection, fontNormal));
        meta.addCell(metaBox("📝 Description", r.getDescription() != null && !r.getDescription().isBlank() ? r.getDescription() : "—", lightBg, fontSection, fontNormal));

        doc.add(meta);
        doc.add(Chunk.NEWLINE);

        // ── ALIMENTS TABLE ──
        Paragraph aliTitle = new Paragraph("🥗  Aliments liés", fontSection);
        aliTitle.setSpacingBefore(10);
        doc.add(aliTitle);
        doc.add(Chunk.NEWLINE);

        if (aliments.isEmpty()) {
            doc.add(new Paragraph("Aucun aliment lié à ce repas.", fontMeta));
        } else {
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3, 1.5f, 1.5f, 2});

            // Headers
            for (String h : new String[]{"NOM", "QUANTITÉ (g)", "CALORIES (kcal)", "MACRO"}) {
                PdfPCell c = new PdfPCell(new Phrase(h, new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, white)));
                c.setBackgroundColor(teal);
                c.setPadding(8);
                c.setBorder(Rectangle.NO_BORDER);
                table.addCell(c);
            }

            // Rows
            boolean alt = false;
            for (Aliment a : aliments) {
                BaseColor rowColor = alt ? new BaseColor(30, 41, 59) : lightBg;
                table.addCell(styledCell(a.getNom(), rowColor, fontNormal));
                table.addCell(styledCell((int)a.getQuantite() + " g", rowColor, fontNormal));
                table.addCell(styledCell((int)a.getCalories() + " kcal", rowColor, fontNormal));
                table.addCell(styledCell(a.getMacro() != null ? a.getMacro() : "—", rowColor, fontNormal));
                alt = !alt;
            }
            doc.add(table);
        }

        // ── FOOTER ──
        doc.add(Chunk.NEWLINE);
        Paragraph footer = new Paragraph("Généré par AIVA Nutrition — " + java.time.LocalDate.now(), fontMeta);
        footer.setAlignment(Element.ALIGN_CENTER);
        doc.add(footer);

        doc.close();
        return outputPath;
    }

    private PdfPCell metaBox(String title, String value, BaseColor bg, Font fTitle, Font fVal) {
        PdfPCell c = new PdfPCell();
        c.setBackgroundColor(bg);
        c.setPadding(12);
        c.setBorder(Rectangle.NO_BORDER);
        c.addElement(new Paragraph(title, fTitle));
        c.addElement(new Paragraph(value, fVal));
        return c;
    }

    private PdfPCell styledCell(String text, BaseColor bg, Font f) {
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        c.setBackgroundColor(bg);
        c.setPadding(8);
        c.setBorder(Rectangle.NO_BORDER);
        return c;
    }
}
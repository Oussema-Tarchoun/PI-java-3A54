package Services;

import Models.Energie;
import Models.Recommandation;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.xwpf.usermodel.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;

public class ExportService {

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

    /**
     * Export a list of Energie records with their recommendations to a PDF file.
     */
    public void exportToPDF(List<Energie> energies, List<Recommandation> recommendations, String filePath) throws IOException {
        com.lowagie.text.Document document = new com.lowagie.text.Document(PageSize.A4.rotate()); // Landscape for more space
        try {
            PdfWriter.getInstance(document, new FileOutputStream(filePath));
            document.open();

            // Title
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22);
            Paragraph title = new Paragraph("Rapport Détaillé Consommation & Conseils - AIVA", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(25);
            document.add(title);

            // Table
            PdfPTable table = new PdfPTable(5); // Date, Type, Source, Valeur, Conseil
            table.setWidthPercentage(100);
            table.setWidths(new float[]{10, 15, 15, 10, 50});
            table.setSpacingBefore(10f);

            // Headers
            String[] headers = {"Date", "Type", "Source", "Valeur", "Conseil IA"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11)));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(8);
                cell.setBackgroundColor(java.awt.Color.LIGHT_GRAY);
                table.addCell(cell);
            }

            // Data
            for (Energie e : energies) {
                table.addCell(new Phrase(dateFormat.format(e.getDate_enregistrement()), FontFactory.getFont(FontFactory.HELVETICA, 10)));
                table.addCell(new Phrase(e.getType_energie(), FontFactory.getFont(FontFactory.HELVETICA, 10)));
                table.addCell(new Phrase(e.getSource(), FontFactory.getFont(FontFactory.HELVETICA, 10)));
                table.addCell(new Phrase(e.getValeur() + " kWh", FontFactory.getFont(FontFactory.HELVETICA, 10)));

                // Find recommendation
                Optional<Recommandation> rec = recommendations.stream()
                        .filter(r -> r.getEnergie_id() == e.getId())
                        .findFirst();
                
                String conseilText = rec.map(r -> r.getTitre() + ": " + r.getDescription()).orElse("Aucun conseil généré.");
                table.addCell(new Phrase(conseilText, FontFactory.getFont(FontFactory.HELVETICA, 9)));
            }

            document.add(table);
            document.close();
        } catch (DocumentException de) {
            throw new IOException(de.getMessage());
        }
    }

    /**
     * Export a list of Energie records with their recommendations to a Word (.docx) file.
     */
    public void exportToWord(List<Energie> energies, List<Recommandation> recommendations, String filePath) throws IOException {
        try (XWPFDocument document = new XWPFDocument();
             FileOutputStream out = new FileOutputStream(filePath)) {

            XWPFParagraph title = document.createParagraph();
            title.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = title.createRun();
            titleRun.setText("Rapport Détaillé Consommation & Conseils - AIVA");
            titleRun.setBold(true);
            titleRun.setFontSize(18);

            XWPFTable table = document.createTable();
            // Headers
            XWPFTableRow headerRow = table.getRow(0);
            headerRow.getCell(0).setText("Date");
            headerRow.addNewTableCell().setText("Type");
            headerRow.addNewTableCell().setText("Source");
            headerRow.addNewTableCell().setText("Valeur");
            headerRow.addNewTableCell().setText("Conseil IA");

            // Data
            for (Energie e : energies) {
                XWPFTableRow row = table.createRow();
                row.getCell(0).setText(dateFormat.format(e.getDate_enregistrement()));
                row.getCell(1).setText(e.getType_energie());
                row.getCell(2).setText(e.getSource());
                row.getCell(3).setText(e.getValeur() + " kWh");

                Optional<Recommandation> rec = recommendations.stream()
                        .filter(r -> r.getEnergie_id() == e.getId())
                        .findFirst();
                
                String conseilText = rec.map(r -> r.getTitre() + ": " + r.getDescription()).orElse("N/A");
                row.getCell(4).setText(conseilText);
            }

            document.write(out);
        }
    }
}

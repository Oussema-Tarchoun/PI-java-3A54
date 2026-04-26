package Controller.back;

import Models.Categorie;
import Models.Depense;
import Services.CategorieService;
import Services.DepenseService;

// ── OpenPDF ──
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

// ── ZXing QR Code ──
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

// ── JavaFX ──
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;

// ── Java standard ──
import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class DepenseController {

    // ── FXML ─────────────────────────────────────────────
    @FXML private VBox formPane;
    @FXML private HBox btnAjouterBox;
    @FXML private HBox btnModifierBox;
    @FXML private TextField descField, montantField;
    @FXML private TextField montantMinField, montantMaxField;
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<String>    statutCombo;
    @FXML private ComboBox<Categorie> catCombo;
    @FXML private TableView<Depense>  table;
    @FXML private TableColumn<Depense, Integer> colId, colCat;
    @FXML private TableColumn<Depense, String>  colDesc, colStatut, colDate;
    @FXML private TableColumn<Depense, Double>  colMontant;
    @FXML private TableColumn<Depense, Void>    colActions;
    @FXML private Label msgLabel;

    // ── Constantes ───────────────────────────────────────
    private static final String STATUT_PAYE     = "Payé";
    private static final String STATUT_ATTENTE  = "En attente";
    private static final String STATUT_NON_PAYE = "Non payé";

    private static final String COULEUR_FOND     = "#0a0e1a";
    private static final String COULEUR_SURFACE  = "#161b2e";
    private static final String COULEUR_ACCENT   = "#00d4ff";
    private static final String COULEUR_BORDURE  = "#2a3142";
    private static final String COULEUR_TEXTE    = "#ffffff";
    private static final String COULEUR_SECOND   = "#94a3b8";
    private static final String COULEUR_PAYE     = "#22c55e";
    private static final String COULEUR_ATTENTE  = "#f59e0b";
    private static final String COULEUR_NON_PAYE = "#ef4444";

    private static final java.awt.Color AWT_FOND     = new java.awt.Color(0x0a, 0x0e, 0x1a);
    private static final java.awt.Color AWT_SURFACE  = new java.awt.Color(0x16, 0x1b, 0x2e);
    private static final java.awt.Color AWT_ACCENT   = new java.awt.Color(0x00, 0xd4, 0xff);
    private static final java.awt.Color AWT_BORDURE  = new java.awt.Color(0x2a, 0x31, 0x42);
    private static final java.awt.Color AWT_TEXTE    = new java.awt.Color(0xff, 0xff, 0xff);
    private static final java.awt.Color AWT_SECOND   = new java.awt.Color(0x94, 0xa3, 0xb8);
    private static final java.awt.Color AWT_PAYE     = new java.awt.Color(0x22, 0xc5, 0x5e);
    private static final java.awt.Color AWT_ATTENTE  = new java.awt.Color(0xf5, 0x9e, 0x0b);
    private static final java.awt.Color AWT_NON_PAYE = new java.awt.Color(0xef, 0x44, 0x44);

    // ── Services & Data ──────────────────────────────────
    private final DepenseService   service    = new DepenseService();
    private final CategorieService catService = new CategorieService();

    private final ObservableList<Depense> masterData = FXCollections.observableArrayList();
    private FilteredList<Depense> filteredData;
    private SortedList<Depense>   sortedData;
    private boolean modeEdition = false;

    // ═══════════════════════════════════════════════════════
    // ║  INITIALIZE                                         ║
    // ═══════════════════════════════════════════════════════

    @FXML
    public void initialize() {
        statutCombo.setItems(FXCollections.observableArrayList(
                STATUT_PAYE, STATUT_ATTENTE, STATUT_NON_PAYE));

        colId.setCellValueFactory(d ->
                new SimpleIntegerProperty(d.getValue().getIdDepense()).asObject());
        colDesc.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getDescription()));
        colMontant.setCellValueFactory(d ->
                new SimpleDoubleProperty(d.getValue().getMontant()).asObject());
        colDate.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getDateDepense() != null
                        ? d.getValue().getDateDepense().toString() : ""));
        colStatut.setCellValueFactory(d ->
                new SimpleStringProperty(normaliserStatut(d.getValue().getStatut())));
        colCat.setCellValueFactory(d ->
                new SimpleIntegerProperty(d.getValue().getIdCategorie()).asObject());

        // Cellule statut colorée
        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                if (item.equals(STATUT_PAYE))
                    setStyle("-fx-text-fill:#22c55e; -fx-font-weight:bold;");
                else if (item.equals(STATUT_ATTENTE))
                    setStyle("-fx-text-fill:#f59e0b; -fx-font-weight:bold;");
                else
                    setStyle("-fx-text-fill:#ef4444; -fx-font-weight:bold;");
            }
        });

        // Colonne Actions : Éditer | Suppr | QR
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit  = new Button("Éditer");
            private final Button btnSuppr = new Button("Suppr");
            private final Button btnQR    = new Button("QR");
            private final HBox   box      = new HBox(4, btnEdit, btnSuppr, btnQR);
            {
                btnEdit.getStyleClass().add("btn-warning");
                btnSuppr.getStyleClass().add("btn-danger");
                btnQR.setStyle(
                        "-fx-background-color:rgba(0,212,255,0.18); -fx-text-fill:#00d4ff;" +
                                "-fx-background-radius:6px; -fx-padding:4 10 4 10;" +
                                "-fx-cursor:hand; -fx-font-size:11px; -fx-font-weight:bold;");

                btnEdit.setPrefWidth(62);  btnEdit.setMinWidth(62);
                btnSuppr.setPrefWidth(52); btnSuppr.setMinWidth(52);
                btnQR.setPrefWidth(42);    btnQR.setMinWidth(42);
                box.setPrefWidth(170);     box.setMinWidth(170);
                box.setAlignment(Pos.CENTER_LEFT);

                btnEdit.setOnAction(e -> {
                    Depense d = getTableView().getItems().get(getIndex());
                    getTableView().getSelectionModel().select(getIndex());
                    remplirFormulaire(d);
                    setModeEdition(true);
                    showForm(true);
                    msg("Modifiez les champs puis cliquez Modifier", false);
                });

                btnSuppr.setOnAction(e -> {
                    Depense d = getTableView().getItems().get(getIndex());
                    try {
                        service.supprimer(d.getIdDepense());
                        masterData.remove(d);
                        msg("Dépense supprimée avec succès", false);
                    } catch (SQLException ex) {
                        msg("Erreur : " + ex.getMessage(), true);
                    }
                });

                btnQR.setOnAction(e -> {
                    Depense d = getTableView().getItems().get(getIndex());
                    afficherPopupQR(d);
                });
            }

            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
                setText(null);
            }
        });

        filteredData = new FilteredList<>(masterData, p -> true);
        sortedData   = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sortedData);

        chargerCategories();
        charger();
    }

    // ═══════════════════════════════════════════════════════
    // ║  NORMALISATION & VALIDATION                         ║
    // ═══════════════════════════════════════════════════════

    private String normaliserStatut(String statut) {
        if (statut == null) return STATUT_NON_PAYE;
        String s = statut.trim().toLowerCase()
                .replace("é","e").replace("è","e").replace("ê","e");
        if (s.equals("paye") || s.equals("pay") || s.equals("payed")) return STATUT_PAYE;
        if (s.contains("attente") || s.contains("cours") || s.contains("progress")) return STATUT_ATTENTE;
        if (s.contains("non") || s.contains("impaye")) return STATUT_NON_PAYE;
        return statut;
    }

    private String[] couleurStatut(String statut) {
        String norm = normaliserStatut(statut);
        if (norm.equals(STATUT_PAYE))    return new String[]{COULEUR_PAYE,    "rgba(34,197,94,0.15)"};
        if (norm.equals(STATUT_ATTENTE)) return new String[]{COULEUR_ATTENTE, "rgba(245,158,11,0.15)"};
        return new String[]{COULEUR_NON_PAYE, "rgba(239,68,68,0.15)"};
    }

    private boolean validerFormulaire() {
        boolean valide = true;
        resetStyles();

        if (descField.getText() == null || descField.getText().trim().isEmpty()) {
            setFieldError(descField, "La description est obligatoire"); valide = false;
        } else if (descField.getText().trim().length() < 3) {
            setFieldError(descField, "La description doit contenir au moins 3 caractères"); valide = false;
        } else if (descField.getText().trim().length() > 100) {
            setFieldError(descField, "La description ne peut pas dépasser 100 caractères"); valide = false;
        }

        if (montantField.getText() == null || montantField.getText().trim().isEmpty()) {
            setFieldError(montantField, "Le montant est obligatoire"); valide = false;
        } else {
            try {
                double val = Double.parseDouble(montantField.getText().trim());
                if (val <= 0) { setFieldError(montantField, "Le montant doit être supérieur à 0"); valide = false; }
                else if (val > 1_000_000) { setFieldError(montantField, "Le montant ne peut pas dépasser 1 000 000"); valide = false; }
            } catch (NumberFormatException e) {
                setFieldError(montantField, "Le montant doit être un nombre valide"); valide = false;
            }
        }

        if (datePicker.getValue() == null) {
            datePicker.setStyle("-fx-border-color:#e05050; -fx-border-radius:10px; -fx-background-radius:10px; -fx-border-width:1px;");
            msg("La date est obligatoire", true); valide = false;
        }

        if (statutCombo.getValue() == null) {
            statutCombo.setStyle("-fx-border-color:#e05050; -fx-border-radius:10px; -fx-background-radius:10px; -fx-border-width:1px;");
            msg("Sélectionnez un statut", true); valide = false;
        }

        if (catCombo.getValue() == null) {
            catCombo.setStyle("-fx-border-color:#e05050; -fx-border-radius:10px; -fx-background-radius:10px; -fx-border-width:1px;");
            msg("Sélectionnez une catégorie", true); valide = false;
        }

        return valide;
    }

    private boolean verifierBudgetMax(double montant) {
        Categorie cat = catCombo.getValue();
        if (cat != null && cat.getBudgetMax() > 0 && montant > cat.getBudgetMax()) {
            setFieldError(montantField, "Le montant dépasse le budget max (" + cat.getBudgetMax() + ")");
            return false;
        }
        return true;
    }

    private void setFieldError(TextField field, String message) {
        field.setStyle("-fx-border-color:#e05050; -fx-border-radius:10px;" +
                "-fx-background-radius:10px; -fx-border-width:1px; -fx-background-color:#2a1a1a;");
        msg(message, true);
    }

    private void resetStyles() {
        descField.setStyle("");
        montantField.setStyle("");
        datePicker.setStyle("");
        statutCombo.setStyle("");
        catCombo.setStyle("");
        if (msgLabel != null) msgLabel.setText("");
    }

    // ═══════════════════════════════════════════════════════
    // ║  CHARGEMENT                                         ║
    // ═══════════════════════════════════════════════════════

    private void chargerCategories() {
        try {
            catCombo.getItems().setAll(catService.afficher());
            catCombo.setConverter(new StringConverter<Categorie>() {
                @Override public String toString(Categorie c) {
                    return c == null ? "" : c.getIdCategorie() + " - " + c.getNomCategorie();
                }
                @Override public Categorie fromString(String s) { return null; }
            });
        } catch (SQLException e) { msg("Erreur catégories : " + e.getMessage(), true); }
    }

    private void setModeEdition(boolean edition) {
        modeEdition = edition;
        if (btnAjouterBox != null) { btnAjouterBox.setVisible(!edition); btnAjouterBox.setManaged(!edition); }
        if (btnModifierBox != null) { btnModifierBox.setVisible(edition); btnModifierBox.setManaged(edition); }
    }

    // ═══════════════════════════════════════════════════════
    // ║  NAVIGATION                                         ║
    // ═══════════════════════════════════════════════════════

    @FXML
    public void showCategories() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/viewsBack/CategorieView.fxml"));
            Stage stage = (Stage) table.getScene().getWindow();
            stage.setScene(new Scene(root, 1280, 800));
            stage.setMaximized(true);
        } catch (IOException e) { msg("Erreur navigation : " + e.getMessage(), true); }
    }

    @FXML public void ouvrirFormulaire() { viderFormulaire(); setModeEdition(false); showForm(true); }
    @FXML public void annuler()          { viderFormulaire(); setModeEdition(false); showForm(false); }

    private void showForm(boolean visible) {
        if (formPane != null) {
            formPane.setVisible(visible);
            formPane.setManaged(visible);
            formPane.setStyle(visible
                    ? "-fx-background-color:#1e3525; -fx-background-radius:8px; -fx-padding:16px;" +
                    "-fx-border-color:#2ecc71; -fx-border-radius:8px; -fx-border-width:1px;"
                    : "-fx-background-color:#1a2d20; -fx-background-radius:8px; -fx-padding:16px;");
        }
    }

    // ═══════════════════════════════════════════════════════
    // ║  CRUD                                               ║
    // ═══════════════════════════════════════════════════════

    @FXML
    public void ajouter() {
        if (!validerFormulaire()) return;
        double montant = Double.parseDouble(montantField.getText().trim());
        if (!verifierBudgetMax(montant)) return;
        try {
            Depense nouvelle = new Depense(
                    descField.getText().trim(), montant,
                    java.sql.Date.valueOf(datePicker.getValue()),
                    statutCombo.getValue(),
                    catCombo.getValue().getIdCategorie());
            service.ajouter(nouvelle);
            masterData.add(nouvelle);
            viderFormulaire();
            showForm(false);
            msg("Dépense ajoutée avec succès !", false);
        } catch (SQLException e) { msg("Erreur BD : " + e.getMessage(), true); }
    }

    @FXML
    public void modifier() {
        Depense sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { msg("Sélectionnez une ligne d'abord", true); return; }
        if (!validerFormulaire()) return;
        double montant = Double.parseDouble(montantField.getText().trim());
        if (!verifierBudgetMax(montant)) return;
        try {
            Depense modifiee = new Depense(
                    sel.getIdDepense(),
                    descField.getText().trim(), montant,
                    java.sql.Date.valueOf(datePicker.getValue()),
                    statutCombo.getValue(),
                    catCombo.getValue().getIdCategorie());
            service.modifier(modifiee);
            for (int i = 0; i < masterData.size(); i++) {
                if (masterData.get(i).getIdDepense() == sel.getIdDepense()) {
                    masterData.set(i, modifiee);
                    break;
                }
            }
            viderFormulaire();
            setModeEdition(false);
            showForm(false);
            msg("Dépense modifiée avec succès !", false);
        } catch (SQLException e) { msg("Erreur BD : " + e.getMessage(), true); }
    }

    @FXML
    public void charger() {
        try {
            masterData.setAll(service.afficher());
            filteredData.setPredicate(p -> true);
            if (montantMinField != null) montantMinField.setText("");
            if (montantMaxField != null) montantMaxField.setText("");
        } catch (SQLException e) { msg("Erreur BD : " + e.getMessage(), true); }
    }

    @FXML
    public void filtrer() {
        if (filteredData == null) return;
        try {
            String minTxt = montantMinField.getText().trim();
            String maxTxt = montantMaxField.getText().trim();
            if (!minTxt.isEmpty() && !maxTxt.isEmpty()) {
                double min = Double.parseDouble(minTxt);
                double max = Double.parseDouble(maxTxt);
                if (min > max) { msg("Le montant min ne peut pas être supérieur au max", true); return; }
            }
            double min = minTxt.isEmpty() ? Double.MIN_VALUE : Double.parseDouble(minTxt);
            double max = maxTxt.isEmpty() ? Double.MAX_VALUE : Double.parseDouble(maxTxt);
            filteredData.setPredicate(d -> d.getMontant() >= min && d.getMontant() <= max);
        } catch (NumberFormatException e) { msg("Montant de filtre invalide", true); }
    }

    @FXML
    public void trierDate() {
        if (sortedData == null) return;
        sortedData.comparatorProperty().unbind();
        sortedData.setComparator((a, b) -> {
            if (a.getDateDepense() == null) return 1;
            if (b.getDateDepense() == null) return -1;
            return b.getDateDepense().compareTo(a.getDateDepense());
        });
    }

    @FXML
    public void trierMontant() {
        if (sortedData == null) return;
        sortedData.comparatorProperty().unbind();
        sortedData.setComparator((a, b) -> Double.compare(b.getMontant(), a.getMontant()));
    }

    // ═══════════════════════════════════════════════════════
    // ║  QR CODE                                            ║
    // ═══════════════════════════════════════════════════════

    private Image bitMatrixToImage(BitMatrix matrix) {
        int width = matrix.getWidth(), height = matrix.getHeight();
        WritableImage image = new WritableImage(width, height);
        PixelWriter writer = image.getPixelWriter();
        for (int x = 0; x < width; x++)
            for (int y = 0; y < height; y++)
                writer.setColor(x, y, matrix.get(x, y)
                        ? javafx.scene.paint.Color.BLACK
                        : javafx.scene.paint.Color.WHITE);
        return image;
    }

    private Image genererQRCode(String texte, int taille) {
        try {
            QRCodeWriter qrWriter = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1);
            BitMatrix bitMatrix = qrWriter.encode(texte, BarcodeFormat.QR_CODE, taille, taille, hints);
            return bitMatrixToImage(bitMatrix);
        } catch (WriterException e) {
            msg("Erreur QR : " + e.getMessage(), true);
            return null;
        }
    }

    private String formaterDetailsDepense(Depense d) {
        return "AIVA - Gestion Financiere\n" +
                "========================\n\n" +
                "ID: " + d.getIdDepense() + "\n" +
                "Description: " + d.getDescription() + "\n" +
                "Montant: " + String.format("%.2f", d.getMontant()) + " TND\n" +
                "Date: " + (d.getDateDepense() != null ? d.getDateDepense().toString() : "-") + "\n" +
                "Statut: " + normaliserStatut(d.getStatut()) + "\n" +
                "Categorie: Cat. " + d.getIdCategorie() + "\n" +
                "\nGenere le: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private void afficherPopupQR(Depense d) {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.setTitle("QR Code - " + d.getDescription());
        popup.setResizable(false);

        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color:" + COULEUR_FOND + "; -fx-padding:30px;");
        root.setPrefWidth(320);

        Label titre = new Label("Scanner pour voir les détails");
        titre.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:" + COULEUR_ACCENT + ";");

        Label nomDep = new Label(d.getDescription() + "  •  " + String.format("%.2f TND", d.getMontant()));
        nomDep.setStyle("-fx-font-size:12px; -fx-text-fill:" + COULEUR_SECOND + ";");

        Image qrImage = genererQRCode(formaterDetailsDepense(d), 200);
        ImageView qrView = new ImageView(qrImage);
        qrView.setFitWidth(200);
        qrView.setFitHeight(200);
        qrView.setPreserveRatio(true);

        // Cadre autour du QR
        VBox qrBox = new VBox(qrView);
        qrBox.setStyle(
                "-fx-background-color:#ffffff; -fx-background-radius:10px;" +
                        "-fx-padding:12px; -fx-border-color:" + COULEUR_ACCENT + ";" +
                        "-fx-border-radius:10px; -fx-border-width:2px;");
        qrBox.setAlignment(Pos.CENTER);

        Label statutLabel = new Label(normaliserStatut(d.getStatut()));
        String[] cols = couleurStatut(d.getStatut());
        statutLabel.setStyle(
                "-fx-font-size:12px; -fx-font-weight:bold;" +
                        "-fx-text-fill:" + cols[0] + ";" +
                        "-fx-background-color:" + cols[1] + ";" +
                        "-fx-background-radius:6px; -fx-padding:4 12 4 12;");

        Button btnFermer = new Button("Fermer");
        btnFermer.setStyle(
                "-fx-background-color:" + COULEUR_SURFACE + "; -fx-text-fill:" + COULEUR_SECOND + ";" +
                        "-fx-background-radius:8px; -fx-padding:10 30 10 30;" +
                        "-fx-border-color:" + COULEUR_BORDURE + "; -fx-border-width:1px;" +
                        "-fx-border-radius:8px; -fx-cursor:hand; -fx-font-size:13px;");
        btnFermer.setOnAction(e -> popup.close());

        root.getChildren().addAll(titre, nomDep, qrBox, statutLabel, btnFermer);
        popup.setScene(new Scene(root));
        popup.showAndWait();
    }

    // ═══════════════════════════════════════════════════════
    // ║  EXPORT PDF                                         ║
    // ═══════════════════════════════════════════════════════

    @FXML
    public void exporterPDF() {
        if (masterData.isEmpty()) { msg("Aucune dépense à exporter", true); return; }

        FileChooser fc = new FileChooser();
        fc.setTitle("Enregistrer le rapport PDF");
        fc.setInitialFileName("depenses_" +
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf"));
        File file = fc.showSaveDialog(table.getScene().getWindow());
        if (file == null) return;

        try {
            genererPDF(new ArrayList<>(masterData), file);
            msg("PDF exporté : " + file.getName(), false);
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(file);
        } catch (Exception e) {
            msg("Erreur export PDF : " + e.getMessage(), true);
        }
    }

    private void genererPDF(List<Depense> depenses, File fichier) throws Exception {
        Document document = new Document(PageSize.A4, 40, 40, 50, 50);
        PdfWriter writer  = PdfWriter.getInstance(document, new FileOutputStream(fichier));
        document.open();

        PdfContentByte cb = writer.getDirectContent();
        float pw = document.getPageSize().getWidth();
        float ph = document.getPageSize().getHeight();

        // Fond de page
        cb.setColorFill(AWT_FOND);
        cb.rectangle(0, 0, pw, ph);
        cb.fill();

        // Polices
        BaseFont bfBold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, false);
        BaseFont bf     = BaseFont.createFont(BaseFont.HELVETICA,      BaseFont.WINANSI, false);

        com.lowagie.text.Font fontEntete    = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 10, com.lowagie.text.Font.BOLD,   AWT_ACCENT);
        com.lowagie.text.Font fontNormal    = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 10, com.lowagie.text.Font.NORMAL, AWT_TEXTE);
        com.lowagie.text.Font fontSecondPdf = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA,  9, com.lowagie.text.Font.NORMAL, AWT_SECOND);
        com.lowagie.text.Font fontTotal     = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 11, com.lowagie.text.Font.BOLD,   AWT_ACCENT);
        com.lowagie.text.Font fontBadgePaye    = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 8, com.lowagie.text.Font.BOLD, AWT_PAYE);
        com.lowagie.text.Font fontBadgeAttente = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 8, com.lowagie.text.Font.BOLD, AWT_ATTENTE);
        com.lowagie.text.Font fontBadgeNonPaye = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 8, com.lowagie.text.Font.BOLD, AWT_NON_PAYE);

        // Bandeau titre
        cb.setColorFill(AWT_SURFACE);
        cb.roundRectangle(30, ph - 100, pw - 60, 80, 12);
        cb.fill();
        cb.setColorStroke(AWT_ACCENT);
        cb.setLineWidth(2f);
        cb.roundRectangle(30, ph - 100, pw - 60, 80, 12);
        cb.stroke();
        cb.setColorFill(AWT_ACCENT);
        cb.roundRectangle(30, ph - 100, 6, 80, 3);
        cb.fill();

        cb.beginText();
        cb.setFontAndSize(bfBold, 26);
        cb.setColorFill(AWT_TEXTE);
        cb.setTextMatrix(50, ph - 65);
        cb.showText("Liste des Depenses");
        cb.endText();

        String dateGen = "Genere le " +
                LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
                "  |  " + depenses.size() + " depense(s)";
        cb.beginText();
        cb.setFontAndSize(bf, 11);
        cb.setColorFill(AWT_SECOND);
        cb.setTextMatrix(50, ph - 85);
        cb.showText(dateGen);
        cb.endText();

        // Espacement
        for (int i = 0; i < 3; i++) document.add(new Paragraph(" "));

        // Cartes statistiques
        double total   = depenses.stream().mapToDouble(Depense::getMontant).sum();
        long nbPaye    = depenses.stream().filter(d -> normaliserStatut(d.getStatut()).equals(STATUT_PAYE)).count();
        long nbAttente = depenses.stream().filter(d -> normaliserStatut(d.getStatut()).equals(STATUT_ATTENTE)).count();
        long nbNonPaye = depenses.stream().filter(d -> normaliserStatut(d.getStatut()).equals(STATUT_NON_PAYE)).count();

        float cardY = ph - 200, cardW = (pw - 100) / 4f, cardH = 60, cardGap = 13, startX = 40;
        Object[][] stats = {
                {"Total",      String.format("%.2f TND", total), AWT_ACCENT,   AWT_SURFACE},
                {"Payes",      String.valueOf(nbPaye),            AWT_PAYE,     AWT_SURFACE},
                {"En attente", String.valueOf(nbAttente),         AWT_ATTENTE,  AWT_SURFACE},
                {"Non payes",  String.valueOf(nbNonPaye),         AWT_NON_PAYE, AWT_SURFACE},
        };

        for (int i = 0; i < stats.length; i++) {
            float cx = startX + i * (cardW + cardGap);
            cb.setColorFill((java.awt.Color) stats[i][3]);
            cb.roundRectangle(cx, cardY, cardW, cardH, 10); cb.fill();
            cb.setColorStroke(AWT_BORDURE); cb.setLineWidth(0.5f);
            cb.roundRectangle(cx, cardY, cardW, cardH, 10); cb.stroke();
            cb.setColorFill((java.awt.Color) stats[i][2]);
            cb.roundRectangle(cx, cardY + cardH - 4, cardW, 4, 2); cb.fill();
            cb.beginText();
            cb.setFontAndSize(bf, 10); cb.setColorFill(AWT_SECOND);
            cb.setTextMatrix(cx + 12, cardY + cardH - 20); cb.showText((String) stats[i][0]);
            cb.setFontAndSize(bfBold, 15); cb.setColorFill((java.awt.Color) stats[i][2]);
            cb.setTextMatrix(cx + 12, cardY + 15); cb.showText((String) stats[i][1]);
            cb.endText();
        }

        for (int i = 0; i < 5; i++) document.add(new Paragraph(" "));

        // Tableau
        PdfPTable pdfTable = new PdfPTable(5);
        pdfTable.setWidthPercentage(100);
        pdfTable.setWidths(new float[]{3.5f, 2f, 2.5f, 2f, 1.5f});
        pdfTable.setSpacingBefore(10);

        for (String h : new String[]{"Description", "Montant (TND)", "Date", "Statut", "Cat."}) {
            PdfPCell cell = new PdfPCell(new Phrase(h, fontEntete));
            cell.setBackgroundColor(AWT_SURFACE);
            cell.setBorderColor(AWT_ACCENT);
            cell.setBorderWidth(0.5f);
            cell.setBorderWidthBottom(2f);
            cell.setPadding(12);
            cell.setHorizontalAlignment(Element.ALIGN_LEFT);
            pdfTable.addCell(cell);
        }

        boolean pair = false;
        for (Depense d : depenses) {
            java.awt.Color bg = pair ? new java.awt.Color(0x12, 0x17, 0x28) : AWT_SURFACE;

            PdfPCell cDesc = new PdfPCell(new Phrase(d.getDescription() != null ? d.getDescription() : "-", fontNormal));
            cDesc.setBackgroundColor(bg); cDesc.setBorderColor(AWT_BORDURE);
            cDesc.setBorderWidth(0.3f); cDesc.setPadding(10);
            pdfTable.addCell(cDesc);

            PdfPCell cMontant = new PdfPCell(new Phrase(String.format("%.2f", d.getMontant()), fontNormal));
            cMontant.setBackgroundColor(bg); cMontant.setBorderColor(AWT_BORDURE);
            cMontant.setBorderWidth(0.3f); cMontant.setPadding(10);
            cMontant.setHorizontalAlignment(Element.ALIGN_RIGHT);
            pdfTable.addCell(cMontant);

            PdfPCell cDate = new PdfPCell(new Phrase(d.getDateDepense() != null ? d.getDateDepense().toString() : "-", fontSecondPdf));
            cDate.setBackgroundColor(bg); cDate.setBorderColor(AWT_BORDURE);
            cDate.setBorderWidth(0.3f); cDate.setPadding(10);
            pdfTable.addCell(cDate);

            String statutNorm = normaliserStatut(d.getStatut());
            com.lowagie.text.Font fontBadge;
            java.awt.Color bgBadge;
            if (statutNorm.equals(STATUT_PAYE))    { fontBadge = fontBadgePaye;    bgBadge = AWT_PAYE; }
            else if (statutNorm.equals(STATUT_ATTENTE)) { fontBadge = fontBadgeAttente; bgBadge = AWT_ATTENTE; }
            else                                    { fontBadge = fontBadgeNonPaye; bgBadge = AWT_NON_PAYE; }

            PdfPCell cStatut = new PdfPCell(new Phrase(statutNorm, fontBadge));
            cStatut.setBackgroundColor(bgBadge); cStatut.setBorderColor(AWT_BORDURE);
            cStatut.setBorderWidth(0.3f); cStatut.setPadding(10);
            cStatut.setHorizontalAlignment(Element.ALIGN_CENTER);
            pdfTable.addCell(cStatut);

            PdfPCell cCat = new PdfPCell(new Phrase("Cat. " + d.getIdCategorie(), fontSecondPdf));
            cCat.setBackgroundColor(bg); cCat.setBorderColor(AWT_BORDURE);
            cCat.setBorderWidth(0.3f); cCat.setPadding(10);
            cCat.setHorizontalAlignment(Element.ALIGN_CENTER);
            pdfTable.addCell(cCat);

            pair = !pair;
        }

        // Ligne TOTAL
        PdfPCell cTotalLabel = new PdfPCell(new Phrase("TOTAL", fontTotal));
        cTotalLabel.setBackgroundColor(AWT_ACCENT); cTotalLabel.setBorderColor(AWT_ACCENT);
        cTotalLabel.setBorderWidth(1f); cTotalLabel.setBorderWidthTop(2f); cTotalLabel.setPadding(12);
        pdfTable.addCell(cTotalLabel);

        PdfPCell cTotalVal = new PdfPCell(new Phrase(String.format("%.2f TND", total), fontTotal));
        cTotalVal.setBackgroundColor(AWT_ACCENT); cTotalVal.setBorderColor(AWT_ACCENT);
        cTotalVal.setBorderWidth(1f); cTotalVal.setBorderWidthTop(2f); cTotalVal.setPadding(12);
        cTotalVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
        pdfTable.addCell(cTotalVal);

        PdfPCell cVide = new PdfPCell(new Phrase(""));
        cVide.setBackgroundColor(AWT_ACCENT); cVide.setBorderColor(AWT_ACCENT);
        cVide.setBorderWidth(1f); cVide.setBorderWidthTop(2f); cVide.setColspan(3);
        pdfTable.addCell(cVide);

        document.add(pdfTable);

        document.add(new Paragraph(" "));
        Paragraph pied = new Paragraph("AIVA  -  Rapport genere automatiquement", fontSecondPdf);
        pied.setAlignment(Element.ALIGN_CENTER);
        pied.setSpacingBefore(20);
        document.add(pied);

        document.close();
    }

    // ═══════════════════════════════════════════════════════
    // ║  STATISTIQUES                                       ║
    // ═══════════════════════════════════════════════════════

    @FXML
    public void ouvrirStatistiques() {
        if (masterData.isEmpty()) { msg("Aucune dépense à analyser", true); return; }

        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.setTitle("Statistiques avancées - AIVA");
        popup.setResizable(true);

        VBox root = new VBox(16);
        root.setStyle("-fx-background-color:" + COULEUR_FOND + "; -fx-padding:28px;");
        root.setPrefWidth(900);

        // Métriques
        double total   = masterData.stream().mapToDouble(Depense::getMontant).sum();
        double avg     = masterData.isEmpty() ? 0 : total / masterData.size();
        long   nbPaye  = masterData.stream().filter(d -> normaliserStatut(d.getStatut()).equals(STATUT_PAYE)).count();
        long   nbImpay = masterData.size() - nbPaye;

        HBox metrics = new HBox(12);
        metrics.getChildren().addAll(
                creerMetricCard("Total dépensé",  String.format("%.2f TND", total), COULEUR_ACCENT),
                creerMetricCard("Moyenne",         String.format("%.2f TND", avg),   COULEUR_SECOND),
                creerMetricCard("Payées",          String.valueOf(nbPaye),             COULEUR_PAYE),
                creerMetricCard("Non réglées",     String.valueOf(nbImpay),            COULEUR_NON_PAYE)
        );

        // Graphique mensuel
        Map<String, Double> parMois    = new java.util.LinkedHashMap<>();
        Map<String, Double> paidMois   = new java.util.LinkedHashMap<>();
        Map<String, Double> unpaidMois = new java.util.LinkedHashMap<>();
        DateTimeFormatter moisFmt = DateTimeFormatter.ofPattern("MMM yyyy");

        for (Depense d : masterData) {
            if (d.getDateDepense() == null) continue;
            LocalDate ld = (d.getDateDepense() instanceof java.sql.Date)
                    ? ((java.sql.Date) d.getDateDepense()).toLocalDate()
                    : d.getDateDepense().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
            String key = ld.format(moisFmt);
            parMois.merge(key, d.getMontant(), Double::sum);
            if (normaliserStatut(d.getStatut()).equals(STATUT_PAYE))
                paidMois.merge(key, d.getMontant(), Double::sum);
            else
                unpaidMois.merge(key, d.getMontant(), Double::sum);
        }

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis   yAxis = new NumberAxis();
        xAxis.setLabel("Mois"); yAxis.setLabel("Montant (TND)");
        LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Évolution mensuelle des dépenses");
        lineChart.setPrefHeight(280);

        XYChart.Series<String, Number> sTotal  = new XYChart.Series<>(); sTotal.setName("Total");
        XYChart.Series<String, Number> sPaid   = new XYChart.Series<>(); sPaid.setName("Payées");
        XYChart.Series<String, Number> sUnpaid = new XYChart.Series<>(); sUnpaid.setName("Non réglées");

        parMois.forEach((k, v)    -> sTotal.getData().add(new XYChart.Data<>(k, v)));
        paidMois.forEach((k, v)   -> sPaid.getData().add(new XYChart.Data<>(k, v)));
        unpaidMois.forEach((k, v) -> sUnpaid.getData().add(new XYChart.Data<>(k, v)));
        lineChart.getData().addAll(sTotal, sPaid, sUnpaid);

        // Top 5
        Label lblTop = new Label("Top 5 dépenses les plus élevées");
        lblTop.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:" + COULEUR_ACCENT + ";");

        VBox topBox = new VBox(6);
        masterData.stream()
                .sorted((a, b) -> Double.compare(b.getMontant(), a.getMontant()))
                .limit(5)
                .forEach(d -> {
                    HBox row = new HBox(12);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.setStyle("-fx-background-color:" + COULEUR_SURFACE +
                            "; -fx-background-radius:8px; -fx-padding:10px;" +
                            "-fx-border-color:" + COULEUR_BORDURE + "; -fx-border-radius:8px; -fx-border-width:1px;");

                    Label desc = new Label(d.getDescription());
                    desc.setStyle("-fx-text-fill:" + COULEUR_TEXTE + "; -fx-font-size:13px;");
                    desc.setMaxWidth(320); HBox.setHgrow(desc, Priority.ALWAYS);

                    Label amt = new Label(String.format("%.2f TND", d.getMontant()));
                    amt.setStyle("-fx-text-fill:" + COULEUR_ACCENT + "; -fx-font-weight:bold; -fx-font-size:13px;");

                    String[] cols = couleurStatut(d.getStatut());
                    Label stat = new Label(normaliserStatut(d.getStatut()));
                    stat.setStyle("-fx-text-fill:" + cols[0] + "; -fx-background-color:" + cols[1] +
                            "; -fx-background-radius:6px; -fx-padding:3 8 3 8;" +
                            "-fx-font-size:11px; -fx-font-weight:bold;");

                    Label catLabel = new Label("Cat. " + d.getIdCategorie());
                    catLabel.setStyle("-fx-text-fill:#64748b; -fx-font-size:11px;");

                    row.getChildren().addAll(desc, amt, stat, catLabel);
                    topBox.getChildren().add(row);
                });

        // Répartition par statut (compteur visuel)
        Label lblRep = new Label("Répartition par statut");
        lblRep.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:" + COULEUR_ACCENT + ";");

        long cPaye    = masterData.stream().filter(d -> normaliserStatut(d.getStatut()).equals(STATUT_PAYE)).count();
        long cAttente = masterData.stream().filter(d -> normaliserStatut(d.getStatut()).equals(STATUT_ATTENTE)).count();
        long cNonPaye = masterData.stream().filter(d -> normaliserStatut(d.getStatut()).equals(STATUT_NON_PAYE)).count();
        long total2   = masterData.size();

        HBox repartition = new HBox(12);
        repartition.getChildren().addAll(
                creerBarreStatut("Payé",        cPaye,    total2, COULEUR_PAYE),
                creerBarreStatut("En attente",  cAttente, total2, COULEUR_ATTENTE),
                creerBarreStatut("Non payé",    cNonPaye, total2, COULEUR_NON_PAYE)
        );

        Button btnFermer = new Button("Fermer");
        btnFermer.setStyle(
                "-fx-background-color:#a855f720; -fx-text-fill:#a855f7;" +
                        "-fx-background-radius:8px; -fx-padding:10 30 10 30;" +
                        "-fx-border-color:#a855f7; -fx-border-width:1px;" +
                        "-fx-border-radius:8px; -fx-cursor:hand; -fx-font-size:14px;");
        btnFermer.setOnAction(e -> popup.close());

        root.getChildren().addAll(metrics, lineChart, lblTop, topBox, lblRep, repartition, btnFermer);

        ScrollPane sp = new ScrollPane(root);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background:transparent; -fx-background-color:transparent;");
        popup.setScene(new Scene(sp, 940, 720));
        popup.showAndWait();
    }

    private VBox creerMetricCard(String label, String valeur, String couleur) {
        VBox card = new VBox(6);
        card.setPrefWidth(190);
        card.setStyle(
                "-fx-background-color:" + COULEUR_SURFACE + "; -fx-background-radius:10px; -fx-padding:16px;" +
                        "-fx-border-color:" + couleur + "44; -fx-border-radius:10px; -fx-border-width:1px;");
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size:12px; -fx-text-fill:" + COULEUR_SECOND + ";");
        Label val = new Label(valeur);
        val.setStyle("-fx-font-size:20px; -fx-font-weight:bold; -fx-text-fill:" + couleur + ";");
        card.getChildren().addAll(lbl, val);
        return card;
    }

    private VBox creerBarreStatut(String label, long count, long total, String couleur) {
        VBox box = new VBox(8);
        box.setPrefWidth(200);
        box.setStyle(
                "-fx-background-color:" + COULEUR_SURFACE + "; -fx-background-radius:10px; -fx-padding:14px;" +
                        "-fx-border-color:" + COULEUR_BORDURE + "; -fx-border-radius:10px; -fx-border-width:1px;");

        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size:13px; -fx-text-fill:" + COULEUR_SECOND + ";");

        Label cnt = new Label(count + " / " + total);
        cnt.setStyle("-fx-font-size:18px; -fx-font-weight:bold; -fx-text-fill:" + couleur + ";");

        double pct = total == 0 ? 0 : (count * 100.0 / total);
        Label pctLabel = new Label(String.format("%.1f%%", pct));
        pctLabel.setStyle("-fx-font-size:12px; -fx-text-fill:" + COULEUR_SECOND + ";");

        // Barre de progression simple
        HBox barBg = new HBox();
        barBg.setPrefHeight(8);
        barBg.setStyle("-fx-background-color:#2a3142; -fx-background-radius:4px;");
        HBox barFill = new HBox();
        barFill.setPrefHeight(8);
        barFill.setPrefWidth(pct * 1.6); // max ~160px pour 100%
        barFill.setStyle("-fx-background-color:" + couleur + "; -fx-background-radius:4px;");
        barBg.getChildren().add(barFill);

        box.getChildren().addAll(lbl, cnt, pctLabel, barBg);
        return box;
    }

    // ═══════════════════════════════════════════════════════
    // ║  UTILITAIRES                                        ║
    // ═══════════════════════════════════════════════════════

    public List<Depense> listerDepenses() {
        try { return service.afficher(); }
        catch (SQLException e) { return Collections.emptyList(); }
    }

    public List<Depense> listerDepensesParCategorie(int idCategorie) {
        try { return service.getByCategorie(idCategorie); }
        catch (SQLException e) { return Collections.emptyList(); }
    }

    private void remplirFormulaire(Depense d) {
        descField.setText(d.getDescription());
        montantField.setText(String.valueOf(d.getMontant()));
        statutCombo.setValue(normaliserStatut(d.getStatut()));
        catCombo.getItems().stream()
                .filter(c -> c.getIdCategorie() == d.getIdCategorie())
                .findFirst().ifPresent(catCombo::setValue);
        if (d.getDateDepense() != null) {
            if (d.getDateDepense() instanceof java.sql.Date)
                datePicker.setValue(((java.sql.Date) d.getDateDepense()).toLocalDate());
            else
                datePicker.setValue(d.getDateDepense().toInstant()
                        .atZone(java.time.ZoneId.systemDefault()).toLocalDate());
        } else datePicker.setValue(null);
    }

    private void viderFormulaire() {
        descField.clear(); montantField.clear();
        statutCombo.setValue(null); catCombo.setValue(null); datePicker.setValue(null);
        if (montantMinField != null) montantMinField.clear();
        if (montantMaxField != null) montantMaxField.clear();
        resetStyles();
    }

    private void msg(String m, boolean erreur) {
        if (msgLabel != null) {
            msgLabel.setText(m);
            msgLabel.setStyle(erreur
                    ? "-fx-text-fill:#e05050; -fx-font-size:12px;"
                    : "-fx-text-fill:#2ecc71; -fx-font-size:12px;");
        }
    }
}

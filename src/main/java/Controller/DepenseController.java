package Controller;

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
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.layout.ColumnConstraints;
import javafx.geometry.HPos;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;

// ── Java standard ──
import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import Services.EmailService;


public class DepenseController {

    @FXML private VBox      formPane;
    @FXML private VBox      overlayPane;
    @FXML private HBox      btnAjouterBox;
    @FXML private HBox      btnModifierBox;
    @FXML private TextField descField, montantField;
    @FXML private TextField montantMinField, montantMaxField;
    @FXML private TextField searchField;
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<String>    statutCombo;
    @FXML private ComboBox<Categorie> catCombo;
    @FXML private FlowPane  cardsPane;
    @FXML private Label     msgLabel;

    private final DepenseService   service    = new DepenseService();
    private final CategorieService catService = new CategorieService();
    private ObservableList<Depense> allDepenses = FXCollections.observableArrayList();
    private boolean modeEdition  = false;
    private Depense selectedDepense = null;

    private static final String STATUT_PAYE     = "Payé";
    private static final String STATUT_ATTENTE  = "En attente";
    private static final String STATUT_NON_PAYE = "Non payé";

    // ── Couleurs du theme ──
    private static final String COULEUR_FOND      = "#0a0e1a";
    private static final String COULEUR_SURFACE   = "#161b2e";
    private static final String COULEUR_ACCENT    = "#00d4ff";
    private static final String COULEUR_BORDURE   = "#2a3142";
    private static final String COULEUR_TEXTE     = "#ffffff";
    private static final String COULEUR_SECOND    = "#94a3b8";
    private static final String COULEUR_PAYE      = "#22c55e";
    private static final String COULEUR_ATTENTE   = "#f59e0b";
    private static final String COULEUR_NON_PAYE  = "#ef4444";

    // ── AWT Colors (used only in PDF generation) ──
    private static final java.awt.Color AWT_FOND      = new java.awt.Color(0x0a, 0x0e, 0x1a);
    private static final java.awt.Color AWT_SURFACE   = new java.awt.Color(0x16, 0x1b, 0x2e);
    private static final java.awt.Color AWT_ACCENT    = new java.awt.Color(0x00, 0xd4, 0xff);
    private static final java.awt.Color AWT_BORDURE   = new java.awt.Color(0x2a, 0x31, 0x42);
    private static final java.awt.Color AWT_TEXTE     = new java.awt.Color(0xff, 0xff, 0xff);
    private static final java.awt.Color AWT_SECOND    = new java.awt.Color(0x94, 0xa3, 0xb8);
    private static final java.awt.Color AWT_PAYE      = new java.awt.Color(0x22, 0xc5, 0x5e);
    private static final java.awt.Color AWT_ATTENTE   = new java.awt.Color(0xf5, 0x9e, 0x0b);
    private static final java.awt.Color AWT_NON_PAYE  = new java.awt.Color(0xef, 0x44, 0x44);

    private final EmailService emailService = new EmailService();

    @FXML
    public void initialize() {
        statutCombo.setItems(FXCollections.observableArrayList(
                STATUT_PAYE, STATUT_ATTENTE, STATUT_NON_PAYE));
        chargerCategories();
        charger();

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.trim().isEmpty()) {
                afficherCartes(allDepenses);
            } else {
                String recherche = newVal.trim().toLowerCase();
                afficherCartes(allDepenses.filtered(d ->
                        d.getDescription() != null &&
                                d.getDescription().toLowerCase().contains(recherche)
                ));
            }
        });
    }

    // ═══════════════════════════════════════════════════════
    // ║  CALENDRIER                                         ║
    // ═══════════════════════════════════════════════════════

    private YearMonth moisCourant = YearMonth.now();

    @FXML
    public void ouvrirCalendrier() {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.setTitle("Calendrier des Dépenses");
        popup.setResizable(false);

        VBox root = new VBox(16);
        root.setAlignment(Pos.TOP_CENTER);
        root.setStyle("-fx-background-color:" + COULEUR_FOND + "; -fx-padding:24px;");
        root.setPrefWidth(520);

        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER);

        Button btnPrev = new Button("◀");
        btnPrev.setStyle(boutonNavigationStyle());
        btnPrev.setOnAction(e -> {
            moisCourant = moisCourant.minusMonths(1);
            rafraichirCalendrier(root);
        });

        Label lblMois = new Label();
        lblMois.setId("lblMois");
        lblMois.setStyle("-fx-font-size:22px; -fx-font-weight:bold; -fx-text-fill:" + COULEUR_ACCENT + ";");
        lblMois.setText(moisCourant.format(DateTimeFormatter.ofPattern("MMMM yyyy")));

        Button btnNext = new Button("▶");
        btnNext.setStyle(boutonNavigationStyle());
        btnNext.setOnAction(e -> {
            moisCourant = moisCourant.plusMonths(1);
            rafraichirCalendrier(root);
        });

        Button btnAujourdhui = new Button("Aujourd'hui");
        btnAujourdhui.setStyle(
                "-fx-background-color:" + COULEUR_SURFACE + "; -fx-text-fill:" + COULEUR_ACCENT + ";" +
                        "-fx-background-radius:8px; -fx-padding:8 16 8 16;" +
                        "-fx-border-color:" + COULEUR_ACCENT + "; -fx-border-width:1px;" +
                        "-fx-border-radius:8px; -fx-cursor:hand; -fx-font-size:13px; -fx-font-weight:bold;");
        btnAujourdhui.setOnAction(e -> {
            moisCourant = YearMonth.now();
            rafraichirCalendrier(root);
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(btnPrev, lblMois, btnNext, spacer, btnAujourdhui);

        HBox legende = new HBox(20);
        legende.setAlignment(Pos.CENTER);
        legende.getChildren().addAll(
                creerLegende("Payé", COULEUR_PAYE),
                creerLegende("En attente", COULEUR_ATTENTE),
                creerLegende("Non payé", COULEUR_NON_PAYE)
        );

        GridPane grille = new GridPane();
        grille.setId("grilleCalendrier");
        grille.setHgap(8);
        grille.setVgap(8);
        grille.setAlignment(Pos.CENTER);

        for (int i = 0; i < 7; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPrefWidth(60);
            cc.setHalignment(HPos.CENTER);
            grille.getColumnConstraints().add(cc);
        }

        root.getChildren().addAll(header, legende, grille);

        remplirGrilleCalendrier(grille);

        Button btnFermer = new Button("Fermer");
        btnFermer.setStyle(
                "-fx-background-color:" + COULEUR_SURFACE + "; -fx-text-fill:" + COULEUR_SECOND + ";" +
                        "-fx-background-radius:8px; -fx-padding:10 40 10 40;" +
                        "-fx-border-color:" + COULEUR_BORDURE + "; -fx-border-width:1px;" +
                        "-fx-border-radius:8px; -fx-cursor:hand; -fx-font-size:14px;");
        btnFermer.setOnAction(e -> popup.close());

        root.getChildren().add(btnFermer);

        Scene scene = new Scene(root);
        popup.setScene(scene);
        popup.showAndWait();
    }

    private void rafraichirCalendrier(VBox root) {
        for (Node node : root.getChildren()) {
            if (node instanceof HBox) {
                HBox header = (HBox) node;
                for (Node child : header.getChildren()) {
                    if (child instanceof Label) {
                        Label lbl = (Label) child;
                        if ("lblMois".equals(lbl.getId())) {
                            lbl.setText(moisCourant.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
                        }
                    }
                }
            }
            if (node instanceof GridPane) {
                GridPane grille = (GridPane) node;
                if ("grilleCalendrier".equals(grille.getId())) {
                    grille.getChildren().clear();
                    remplirGrilleCalendrier(grille);
                }
            }
        }
    }

    private void remplirGrilleCalendrier(GridPane grille) {
        String[] joursSemaine = {"Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"};
        for (int i = 0; i < 7; i++) {
            Label jour = new Label(joursSemaine[i]);
            jour.setStyle("-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:" + COULEUR_SECOND + ";");
            grille.add(jour, i, 0);
        }

        LocalDate premierJour = moisCourant.atDay(1);
        int decalage = premierJour.getDayOfWeek().getValue() - 1;
        int nbJours = moisCourant.lengthOfMonth();

        int ligne = 1;
        int col = decalage;

        for (int jour = 1; jour <= nbJours; jour++) {
            LocalDate date = moisCourant.atDay(jour);
            VBox cellule = creerCelluleJour(date);
            grille.add(cellule, col, ligne);

            col++;
            if (col > 6) {
                col = 0;
                ligne++;
            }
        }
    }

    private VBox creerCelluleJour(LocalDate date) {
        VBox cellule = new VBox(4);
        cellule.setAlignment(Pos.CENTER);
        cellule.setPrefSize(60, 60);
        cellule.setStyle(
                "-fx-background-color:" + COULEUR_SURFACE + ";" +
                        "-fx-background-radius:10px;" +
                        "-fx-border-color:" + COULEUR_BORDURE + ";" +
                        "-fx-border-radius:10px; -fx-border-width:1px;" +
                        "-fx-padding:6px; -fx-cursor:hand;");

        List<Depense> depensesJour = allDepenses.stream()
                .filter(d -> {
                    if (d.getDateDepense() == null) return false;
                    LocalDate dDate;
                    if (d.getDateDepense() instanceof Date) {
                        dDate = ((Date) d.getDateDepense()).toLocalDate();
                    } else {
                        dDate = d.getDateDepense().toInstant()
                                .atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                    }
                    return dDate.equals(date);
                })
                .collect(Collectors.toList());

        Label lblJour = new Label(String.valueOf(date.getDayOfMonth()));
        lblJour.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:" + COULEUR_TEXTE + ";");

        cellule.getChildren().add(lblJour);

        if (!depensesJour.isEmpty()) {
            double total = depensesJour.stream().mapToDouble(Depense::getMontant).sum();
            int nbDepenses = depensesJour.size();

            String couleur = determinerCouleurDominante(depensesJour);

            cellule.setStyle(
                    "-fx-background-color:" + couleur + "22;" +
                            "-fx-background-radius:10px;" +
                            "-fx-border-color:" + couleur + ";" +
                            "-fx-border-radius:10px; -fx-border-width:2px;" +
                            "-fx-padding:6px; -fx-cursor:hand;");

            Label lblMontant = new Label(String.format("%.0f", total));
            lblMontant.setStyle("-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:" + couleur + ";");

            Label lblCount = new Label(nbDepenses + " dép.");
            lblCount.setStyle("-fx-font-size:9px; -fx-text-fill:" + COULEUR_SECOND + ";");

            cellule.getChildren().addAll(lblMontant, lblCount);

            String tooltip = depensesJour.stream()
                    .map(d -> String.format("• %s: %.2f TND (%s)",
                            d.getDescription(), d.getMontant(), normaliserStatut(d.getStatut())))
                    .collect(Collectors.joining("\n"));
            javafx.scene.control.Tooltip.install(cellule, new javafx.scene.control.Tooltip(tooltip));

            cellule.setOnMouseClicked(e -> afficherDepensesJour(date, depensesJour));
        }

        if (date.equals(LocalDate.now())) {
            cellule.setStyle(cellule.getStyle().replace(
                    "-fx-border-color:" + COULEUR_BORDURE,
                    "-fx-border-color:" + COULEUR_ACCENT
            ).replace(
                    "-fx-border-width:1px",
                    "-fx-border-width:2px"
            ));
            lblJour.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:" + COULEUR_ACCENT + ";");
        }

        String styleBase = cellule.getStyle();
        cellule.setOnMouseEntered(e -> {
            if (depensesJour.isEmpty()) {
                cellule.setStyle(styleBase.replace(
                        "-fx-background-color:" + COULEUR_SURFACE,
                        "-fx-background-color:#1e2538"
                ));
            }
        });
        cellule.setOnMouseExited(e -> cellule.setStyle(styleBase));

        return cellule;
    }

    private String determinerCouleurDominante(List<Depense> depenses) {
        long nbPaye    = depenses.stream().filter(d -> normaliserStatut(d.getStatut()).equals(STATUT_PAYE)).count();
        long nbAttente = depenses.stream().filter(d -> normaliserStatut(d.getStatut()).equals(STATUT_ATTENTE)).count();
        long nbNonPaye = depenses.stream().filter(d -> normaliserStatut(d.getStatut()).equals(STATUT_NON_PAYE)).count();

        if (nbPaye > 0 && nbAttente == 0 && nbNonPaye == 0) return COULEUR_PAYE;
        if (nbAttente > 0 && nbPaye == 0 && nbNonPaye == 0) return COULEUR_ATTENTE;
        if (nbNonPaye > 0 && nbPaye == 0 && nbAttente == 0) return COULEUR_NON_PAYE;

        if (nbNonPaye > 0) return COULEUR_NON_PAYE;
        if (nbAttente > 0) return COULEUR_ATTENTE;
        return COULEUR_PAYE;
    }

    private HBox creerLegende(String texte, String couleur) {
        HBox box = new HBox(6);
        box.setAlignment(Pos.CENTER);

        Label dot = new Label("●");
        dot.setStyle("-fx-font-size:12px; -fx-text-fill:" + couleur + ";");

        Label lbl = new Label(texte);
        lbl.setStyle("-fx-font-size:11px; -fx-text-fill:" + COULEUR_SECOND + ";");

        box.getChildren().addAll(dot, lbl);
        return box;
    }

    private String boutonNavigationStyle() {
        return "-fx-background-color:" + COULEUR_SURFACE + "; -fx-text-fill:" + COULEUR_TEXTE + ";" +
                "-fx-background-radius:8px; -fx-padding:8 14 8 14;" +
                "-fx-border-color:" + COULEUR_BORDURE + "; -fx-border-width:1px;" +
                "-fx-border-radius:8px; -fx-cursor:hand; -fx-font-size:14px; -fx-font-weight:bold;";
    }

    private void afficherDepensesJour(LocalDate date, List<Depense> depenses) {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.setTitle("Dépenses du " + date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        popup.setResizable(false);

        VBox root = new VBox(12);
        root.setStyle("-fx-background-color:" + COULEUR_FOND + "; -fx-padding:24px;");
        root.setPrefWidth(400);

        Label titre = new Label(date.format(DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy")));
        titre.setStyle("-fx-font-size:18px; -fx-font-weight:bold; -fx-text-fill:" + COULEUR_ACCENT + ";");

        double total = depenses.stream().mapToDouble(Depense::getMontant).sum();
        Label lblTotal = new Label(String.format("Total: %.2f TND", total));
        lblTotal.setStyle("-fx-font-size:16px; -fx-font-weight:bold; -fx-text-fill:" + COULEUR_TEXTE + ";");

        VBox liste = new VBox(8);
        for (Depense d : depenses) {
            HBox item = new HBox(12);
            item.setAlignment(Pos.CENTER_LEFT);
            item.setStyle("-fx-background-color:" + COULEUR_SURFACE + ";" +
                    "-fx-background-radius:8px; -fx-padding:10px;" +
                    "-fx-border-color:" + COULEUR_BORDURE + ";" +
                    "-fx-border-radius:8px; -fx-border-width:1px;");

            String[] cols = couleurStatut(d.getStatut());

            Label desc = new Label(d.getDescription());
            desc.setStyle("-fx-font-size:14px; -fx-text-fill:" + COULEUR_TEXTE + ";");
            desc.setMaxWidth(200);
            HBox.setHgrow(desc, Priority.ALWAYS);

            Label montant = new Label(String.format("%.2f TND", d.getMontant()));
            montant.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:" + COULEUR_ACCENT + ";");

            Label statut = new Label(normaliserStatut(d.getStatut()));
            statut.setStyle(
                    "-fx-font-size:11px; -fx-font-weight:bold;" +
                            "-fx-text-fill:" + cols[0] + ";" +
                            "-fx-background-color:" + cols[1] + ";" +
                            "-fx-background-radius:6px; -fx-padding:3 10 3 10;");

            item.getChildren().addAll(desc, montant, statut);
            liste.getChildren().add(item);
        }

        ScrollPane scroll = new ScrollPane(liste);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(300);
        scroll.setStyle("-fx-background:transparent; -fx-background-color:transparent;");

        Button btnFermer = new Button("Fermer");
        btnFermer.setStyle(
                "-fx-background-color:" + COULEUR_SURFACE + "; -fx-text-fill:" + COULEUR_SECOND + ";" +
                        "-fx-background-radius:8px; -fx-padding:10 40 10 40;" +
                        "-fx-border-color:" + COULEUR_BORDURE + "; -fx-border-width:1px;" +
                        "-fx-border-radius:8px; -fx-cursor:hand; -fx-font-size:14px;");
        btnFermer.setOnAction(e -> popup.close());

        root.getChildren().addAll(titre, lblTotal, scroll, btnFermer);

        Scene scene = new Scene(root);
        popup.setScene(scene);
        popup.showAndWait();
    }

    // ═══════════════════════════════════════════════════════
    // ║  QR CODE                                            ║
    // ═══════════════════════════════════════════════════════

    private Image bitMatrixToImage(BitMatrix matrix) {
        int width = matrix.getWidth();
        int height = matrix.getHeight();
        WritableImage image = new WritableImage(width, height);
        PixelWriter writer = image.getPixelWriter();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                javafx.scene.paint.Color color = matrix.get(x, y)
                        ? javafx.scene.paint.Color.BLACK
                        : javafx.scene.paint.Color.WHITE;
                writer.setColor(x, y, color);
            }
        }
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
            msg("Erreur generation QR: " + e.getMessage(), true);
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
        root.setPrefWidth(300);

        Label titre = new Label("Scanner pour voir les details");
        titre.setStyle("-fx-font-size:16px; -fx-font-weight:bold; -fx-text-fill:" + COULEUR_ACCENT + ";");

        String qrContent = formaterDetailsDepense(d);
        Image qrImage = genererQRCode(qrContent, 200);

        ImageView qrView = new ImageView(qrImage);
        qrView.setFitWidth(200);
        qrView.setFitHeight(200);
        qrView.setPreserveRatio(true);
        qrView.setStyle("-fx-effect: dropshadow(gaussian, " + COULEUR_ACCENT + ", 10, 0, 0, 0);");

        Button btnFermer = new Button("Fermer");
        btnFermer.setStyle(
                "-fx-background-color:" + COULEUR_SURFACE + "; -fx-text-fill:" + COULEUR_SECOND + ";" +
                        "-fx-background-radius:8px; -fx-padding:10 30 10 30;" +
                        "-fx-border-color:" + COULEUR_BORDURE + "; -fx-border-width:1px;" +
                        "-fx-border-radius:8px; -fx-cursor:hand; -fx-font-size:14px;");
        btnFermer.setOnAction(e -> popup.close());

        root.getChildren().addAll(titre, qrView, btnFermer);

        Scene scene = new Scene(root);
        popup.setScene(scene);
        popup.showAndWait();
    }

    // ═══════════════════════════════════════════════════════
    // ║  CARTES & CRUD                                      ║
    // ═══════════════════════════════════════════════════════

    private String normaliserStatut(String statut) {
        if (statut == null) return STATUT_NON_PAYE;
        String s = statut.trim().toLowerCase()
                .replace("é", "e").replace("è", "e").replace("ê", "e");
        if (s.equals("paye") || s.equals("pay") || s.equals("payed")) return STATUT_PAYE;
        if (s.contains("attente") || s.contains("cours") || s.contains("progress")) return STATUT_ATTENTE;
        if (s.contains("non") || s.contains("impaye") || s.contains("unpaid")) return STATUT_NON_PAYE;
        return statut;
    }

    private String[] couleurStatut(String statut) {
        String norm = normaliserStatut(statut);
        if (norm.equals(STATUT_PAYE))
            return new String[]{COULEUR_PAYE, "rgba(34,197,94,0.15)"};
        if (norm.equals(STATUT_ATTENTE))
            return new String[]{COULEUR_ATTENTE, "rgba(245,158,11,0.15)"};
        return new String[]{COULEUR_NON_PAYE, "rgba(239,68,68,0.15)"};
    }

    private void afficherCartes(List<Depense> depenses) {
        cardsPane.getChildren().clear();
        if (depenses.isEmpty()) {
            Label empty = new Label("Aucune depense trouvee");
            empty.setStyle("-fx-text-fill:#64748b; -fx-font-size:14px;");
            cardsPane.getChildren().add(empty);
            return;
        }
        for (Depense d : depenses) cardsPane.getChildren().add(creerCarte(d));
    }

    private VBox creerCarte(Depense d) {
        VBox card = new VBox(12);
        card.setPrefWidth(340);
        card.setMaxWidth(340);
        card.setStyle(cardStyle(false));

        HBox header = new HBox();
        Label desc = new Label(d.getDescription());
        desc.setStyle("-fx-font-size:16px; -fx-font-weight:bold; -fx-text-fill:#ffffff;");
        desc.setMaxWidth(190);
        HBox.setHgrow(desc, Priority.ALWAYS);
        Label montant = new Label(String.format("%.2f TND", d.getMontant()));
        montant.setStyle("-fx-font-size:16px; -fx-font-weight:bold; -fx-text-fill:" + COULEUR_ACCENT + ";");
        header.getChildren().addAll(desc, montant);

        Label date = new Label("📅  " + (d.getDateDepense() != null ? d.getDateDepense().toString() : "-"));
        date.setStyle("-fx-font-size:14px; -fx-text-fill:" + COULEUR_SECOND + ";");

        String statutNorm = normaliserStatut(d.getStatut());
        String[] couleurs = couleurStatut(d.getStatut());
        Label statutLabel = new Label(statutNorm);
        statutLabel.setStyle(
                "-fx-font-size:13px; -fx-font-weight:bold;" +
                        "-fx-text-fill:" + couleurs[0] + ";" +
                        "-fx-background-color:" + couleurs[1] + ";" +
                        "-fx-background-radius:6px; -fx-padding:5 12 5 12;");

        Label cat = new Label("🏷  Cat. " + d.getIdCategorie());
        cat.setStyle("-fx-font-size:14px; -fx-text-fill:#64748b;");

        Button btnEdit  = new Button("Editer");
        Button btnSuppr = new Button("Supprimer");
        Button btnQR    = new Button("QR");

        btnEdit.setStyle(
                "-fx-background-color:rgba(245,158,11,0.15); -fx-text-fill:#f59e0b;" +
                        "-fx-background-radius:8px; -fx-padding:9 0 9 0; -fx-cursor:hand;" +
                        "-fx-font-size:13px; -fx-font-weight:bold;");
        btnSuppr.setStyle(
                "-fx-background-color:rgba(239,68,68,0.15); -fx-text-fill:#ef4444;" +
                        "-fx-background-radius:8px; -fx-padding:9 0 9 0; -fx-cursor:hand;" +
                        "-fx-font-size:13px; -fx-font-weight:bold;");
        btnQR.setStyle(
                "-fx-background-color:rgba(0,212,255,0.15); -fx-text-fill:" + COULEUR_ACCENT + ";" +
                        "-fx-background-radius:8px; -fx-padding:9 0 9 0; -fx-cursor:hand;" +
                        "-fx-font-size:13px; -fx-font-weight:bold;");

        btnEdit.setPrefWidth(95);
        btnSuppr.setPrefWidth(95);
        btnQR.setPrefWidth(85);

        btnEdit.setOnAction(e -> {
            selectedDepense = d;
            remplirFormulaire(d);
            setModeEdition(true);
            showForm(true);
            msg("Modifiez les champs puis cliquez Modifier", false);
        });
        btnSuppr.setOnAction(e -> {
            try {
                service.supprimer(d.getIdDepense());
                msg("Depense supprimee avec succes", false);
                charger();
            } catch (SQLException ex) { msg("Erreur : " + ex.getMessage(), true); }
        });
        btnQR.setOnAction(e -> afficherPopupQR(d));

        HBox actions = new HBox(8, btnEdit, btnSuppr, btnQR);
        card.getChildren().addAll(header, date, statutLabel, cat,
                new javafx.scene.control.Separator(), actions);
        card.setOnMouseEntered(e -> card.setStyle(cardStyle(true)));
        card.setOnMouseExited(e  -> card.setStyle(cardStyle(false)));
        return card;
    }

    private String cardStyle(boolean hover) {
        return "-fx-background-color:" + (hover ? "#1e2538" : COULEUR_SURFACE) + ";" +
                "-fx-background-radius:14px;" +
                "-fx-border-color:" + (hover ? COULEUR_ACCENT : COULEUR_BORDURE) + ";" +
                "-fx-border-radius:14px; -fx-border-width:1px;" +
                "-fx-padding:20px; -fx-cursor:hand;";
    }

    // ═══════════════════════════════════════════════════════
    // ║  EXPORT PDF                                         ║
    // ═══════════════════════════════════════════════════════

    @FXML
    public void exporterPDF() {
        List<Depense> depenses = allDepenses;
        if (depenses.isEmpty()) {
            msg("Aucune depense a exporter", true);
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le rapport PDF");
        fileChooser.setInitialFileName("depenses_" +
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".pdf");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf"));
        File file = fileChooser.showSaveDialog(cardsPane.getScene().getWindow());
        if (file == null) return;

        try {
            genererPDF(depenses, file);
            msg("PDF exporte : " + file.getName(), false);
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(file);
        } catch (Exception e) {
            msg("Erreur export PDF : " + e.getMessage(), true);
        }
    }

    private void genererPDF(List<Depense> depenses, File fichier) throws Exception {
        java.awt.Color couleurFond    = AWT_FOND;
        java.awt.Color couleurSurface = AWT_SURFACE;
        java.awt.Color couleurAccent  = AWT_ACCENT;
        java.awt.Color couleurBordure = AWT_BORDURE;
        java.awt.Color couleurTexte   = AWT_TEXTE;
        java.awt.Color couleurSecond  = AWT_SECOND;
        java.awt.Color couleurPaye    = AWT_PAYE;
        java.awt.Color couleurAttente = AWT_ATTENTE;
        java.awt.Color couleurNonPaye = AWT_NON_PAYE;

        Document document = new Document(PageSize.A4, 40, 40, 50, 50);
        PdfWriter writer  = PdfWriter.getInstance(document, new FileOutputStream(fichier));
        document.open();

        PdfContentByte cb = writer.getDirectContent();
        float pw = document.getPageSize().getWidth();
        float ph = document.getPageSize().getHeight();

        cb.setColorFill(couleurFond);
        cb.rectangle(0, 0, pw, ph);
        cb.fill();

        com.lowagie.text.Font fontEntete    = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 10, com.lowagie.text.Font.BOLD,   couleurAccent);
        com.lowagie.text.Font fontNormal    = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 10, com.lowagie.text.Font.NORMAL, couleurTexte);
        com.lowagie.text.Font fontSecondPdf = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA,  9, com.lowagie.text.Font.NORMAL, couleurSecond);
        com.lowagie.text.Font fontTotal     = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 11, com.lowagie.text.Font.BOLD,   couleurAccent);
        com.lowagie.text.Font fontBadgePaye    = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 8, com.lowagie.text.Font.BOLD, couleurPaye);
        com.lowagie.text.Font fontBadgeAttente = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 8, com.lowagie.text.Font.BOLD, couleurAttente);
        com.lowagie.text.Font fontBadgeNonPaye = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 8, com.lowagie.text.Font.BOLD, couleurNonPaye);

        cb.setColorFill(couleurSurface);
        cb.roundRectangle(30, ph - 100, pw - 60, 80, 12);
        cb.fill();

        cb.setColorStroke(couleurAccent);
        cb.setLineWidth(2f);
        cb.roundRectangle(30, ph - 100, pw - 60, 80, 12);
        cb.stroke();

        cb.setColorFill(couleurAccent);
        cb.roundRectangle(30, ph - 100, 6, 80, 3);
        cb.fill();

        BaseFont bfBold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, false);
        BaseFont bf     = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, false);

        cb.beginText();
        cb.setFontAndSize(bfBold, 28);
        cb.setColorFill(couleurTexte);
        cb.setTextMatrix(50, ph - 65);
        cb.showText("Liste des Depenses");
        cb.endText();

        String dateGen = "Genere le " +
                LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
                "  |  " + depenses.size() + " depense(s)";

        cb.beginText();
        cb.setFontAndSize(bf, 11);
        cb.setColorFill(couleurSecond);
        cb.setTextMatrix(50, ph - 85);
        cb.showText(dateGen);
        cb.endText();

        document.add(new Paragraph(" "));
        document.add(new Paragraph(" "));
        document.add(new Paragraph(" "));

        double total   = depenses.stream().mapToDouble(Depense::getMontant).sum();
        long nbPaye    = depenses.stream().filter(d -> normaliserStatut(d.getStatut()).equals(STATUT_PAYE)).count();
        long nbAttente = depenses.stream().filter(d -> normaliserStatut(d.getStatut()).equals(STATUT_ATTENTE)).count();
        long nbNonPaye = depenses.stream().filter(d -> normaliserStatut(d.getStatut()).equals(STATUT_NON_PAYE)).count();

        float cardY   = ph - 200;
        float cardW   = (pw - 100) / 4f;
        float cardH   = 60;
        float cardGap = 13;
        float startX  = 40;

        Object[][] stats = {
                { "Total",      String.format("%.2f TND", total), couleurAccent,   couleurSurface },
                { "Payes",      String.valueOf(nbPaye),            couleurPaye,     couleurSurface },
                { "En attente", String.valueOf(nbAttente),         couleurAttente,  couleurSurface },
                { "Non payes",  String.valueOf(nbNonPaye),         couleurNonPaye,  couleurSurface },
        };

        for (int i = 0; i < stats.length; i++) {
            float cx = startX + i * (cardW + cardGap);

            cb.setColorFill((java.awt.Color) stats[i][3]);
            cb.roundRectangle(cx, cardY, cardW, cardH, 10);
            cb.fill();

            cb.setColorStroke(couleurBordure);
            cb.setLineWidth(0.5f);
            cb.roundRectangle(cx, cardY, cardW, cardH, 10);
            cb.stroke();

            cb.setColorFill((java.awt.Color) stats[i][2]);
            cb.roundRectangle(cx, cardY + cardH - 4, cardW, 4, 2);
            cb.fill();

            cb.beginText();
            cb.setFontAndSize(bf, 10);
            cb.setColorFill(couleurSecond);
            cb.setTextMatrix(cx + 12, cardY + cardH - 20);
            cb.showText((String) stats[i][0]);

            cb.setFontAndSize(bfBold, 16);
            cb.setColorFill((java.awt.Color) stats[i][2]);
            cb.setTextMatrix(cx + 12, cardY + 15);
            cb.showText((String) stats[i][1]);
            cb.endText();
        }

        for (int i = 0; i < 5; i++) document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3.5f, 2f, 2.5f, 2f, 1.5f});
        table.setSpacingBefore(10);

        String[] entetes = { "Description", "Montant (TND)", "Date", "Statut", "Cat." };
        for (String h : entetes) {
            PdfPCell cell = new PdfPCell(new Phrase(h, fontEntete));
            cell.setBackgroundColor(couleurSurface);
            cell.setBorderColor(couleurAccent);
            cell.setBorderWidth(0.5f);
            cell.setBorderWidthBottom(2f);
            cell.setPadding(12);
            cell.setHorizontalAlignment(Element.ALIGN_LEFT);
            table.addCell(cell);
        }

        boolean pair = false;
        for (Depense d : depenses) {
            java.awt.Color bgLigne = pair ? new java.awt.Color(0x12, 0x17, 0x28) : couleurSurface;

            PdfPCell cDesc = new PdfPCell(new Phrase(
                    d.getDescription() != null ? d.getDescription() : "-", fontNormal));
            cDesc.setBackgroundColor(bgLigne);
            cDesc.setBorderColor(couleurBordure);
            cDesc.setBorderWidth(0.3f);
            cDesc.setPadding(10);
            table.addCell(cDesc);

            PdfPCell cMontant = new PdfPCell(new Phrase(
                    String.format("%.2f", d.getMontant()), fontNormal));
            cMontant.setBackgroundColor(bgLigne);
            cMontant.setBorderColor(couleurBordure);
            cMontant.setBorderWidth(0.3f);
            cMontant.setPadding(10);
            cMontant.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(cMontant);

            PdfPCell cDate = new PdfPCell(new Phrase(
                    d.getDateDepense() != null ? d.getDateDepense().toString() : "-", fontSecondPdf));
            cDate.setBackgroundColor(bgLigne);
            cDate.setBorderColor(couleurBordure);
            cDate.setBorderWidth(0.3f);
            cDate.setPadding(10);
            table.addCell(cDate);

            String statutNorm = normaliserStatut(d.getStatut());

            com.lowagie.text.Font fontBadge;
            java.awt.Color bgBadge;
            if (statutNorm.equals(STATUT_PAYE)) {
                fontBadge = fontBadgePaye;
                bgBadge   = new java.awt.Color(34,  197, 94);
            } else if (statutNorm.equals(STATUT_ATTENTE)) {
                fontBadge = fontBadgeAttente;
                bgBadge   = new java.awt.Color(245, 158, 11);
            } else {
                fontBadge = fontBadgeNonPaye;
                bgBadge   = new java.awt.Color(239, 68,  68);
            }

            PdfPCell cStatut = new PdfPCell(new Phrase(statutNorm, fontBadge));
            cStatut.setBackgroundColor(bgBadge);
            cStatut.setBorderColor(couleurBordure);
            cStatut.setBorderWidth(0.3f);
            cStatut.setPadding(10);
            cStatut.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cStatut);

            PdfPCell cCat = new PdfPCell(new Phrase("Cat. " + d.getIdCategorie(), fontSecondPdf));
            cCat.setBackgroundColor(bgLigne);
            cCat.setBorderColor(couleurBordure);
            cCat.setBorderWidth(0.3f);
            cCat.setPadding(10);
            cCat.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cCat);

            pair = !pair;
        }

        PdfPCell cTotalLabel = new PdfPCell(new Phrase("TOTAL", fontTotal));
        cTotalLabel.setBackgroundColor(new java.awt.Color(0x00, 0xd4, 0xff));
        cTotalLabel.setBorderColor(couleurAccent);
        cTotalLabel.setBorderWidth(1f);
        cTotalLabel.setBorderWidthTop(2f);
        cTotalLabel.setPadding(12);
        table.addCell(cTotalLabel);

        PdfPCell cTotalVal = new PdfPCell(new Phrase(String.format("%.2f TND", total), fontTotal));
        cTotalVal.setBackgroundColor(new java.awt.Color(0x00, 0xd4, 0xff));
        cTotalVal.setBorderColor(couleurAccent);
        cTotalVal.setBorderWidth(1f);
        cTotalVal.setBorderWidthTop(2f);
        cTotalVal.setPadding(12);
        cTotalVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(cTotalVal);

        PdfPCell cVide = new PdfPCell(new Phrase(""));
        cVide.setBackgroundColor(new java.awt.Color(0x00, 0xd4, 0xff));
        cVide.setBorderColor(couleurAccent);
        cVide.setBorderWidth(1f);
        cVide.setBorderWidthTop(2f);
        cVide.setColspan(3);
        table.addCell(cVide);

        document.add(table);

        document.add(new Paragraph(" "));
        Paragraph pied = new Paragraph("AIVA  -  Rapport genere automatiquement", fontSecondPdf);
        pied.setAlignment(Element.ALIGN_CENTER);
        pied.setSpacingBefore(20);
        document.add(pied);

        document.close();
    }

    // ═══════════════════════════════════════════════════════
    // ║  VALIDATION & FORMULAIRE                            ║
    // ═══════════════════════════════════════════════════════

    private boolean validerFormulaire() {
        boolean valide = true;
        resetStyles();

        if (descField.getText() == null || descField.getText().trim().isEmpty()) {
            setFieldError(descField, "La description est obligatoire"); valide = false;
        } else if (descField.getText().trim().length() < 3) {
            setFieldError(descField, "Min 3 caracteres"); valide = false;
        } else if (descField.getText().trim().length() > 100) {
            setFieldError(descField, "Max 100 caracteres"); valide = false;
        }

        if (montantField.getText() == null || montantField.getText().trim().isEmpty()) {
            setFieldError(montantField, "Le montant est obligatoire"); valide = false;
        } else {
            try {
                double val = Double.parseDouble(montantField.getText().trim());
                if (val <= 0) { setFieldError(montantField, "Montant doit etre > 0"); valide = false; }
                else if (val > 1_000_000) { setFieldError(montantField, "Montant trop eleve"); valide = false; }
            } catch (NumberFormatException e) {
                setFieldError(montantField, "Montant invalide"); valide = false;
            }
        }

        if (datePicker.getValue() == null) {
            datePicker.setStyle("-fx-border-color:#ef4444; -fx-border-radius:8px; -fx-border-width:1px;" +
                    "-fx-background-color:#111827; -fx-font-size:14px;");
            msg("La date est obligatoire", true); valide = false;
        }

        if (statutCombo.getValue() == null) {
            statutCombo.setStyle("-fx-border-color:#ef4444; -fx-border-radius:8px; -fx-border-width:1px;" +
                    "-fx-background-color:#111827; -fx-font-size:14px;");
            msg("Selectionnez un statut", true); valide = false;
        }

        if (catCombo.getValue() == null) {
            catCombo.setStyle("-fx-border-color:#ef4444; -fx-border-radius:8px; -fx-border-width:1px;" +
                    "-fx-background-color:#111827; -fx-font-size:14px;");
            msg("Selectionnez une categorie", true); valide = false;
        }

        return valide;
    }

    private boolean verifierBudgetMax(double montant) {
        Categorie cat = catCombo.getValue();
        if (cat != null && cat.getBudgetMax() > 0 && montant > cat.getBudgetMax()) {
            setFieldError(montantField, "Montant depasse budget max (" + cat.getBudgetMax() + " TND)");
            return false;
        }
        return true;
    }

    private void setFieldError(TextField field, String message) {
        field.setStyle("-fx-border-color:#ef4444; -fx-border-radius:8px; -fx-border-width:1px;" +
                "-fx-background-color:#1a1020; -fx-text-fill:#ffffff;" +
                "-fx-background-radius:8px; -fx-padding:10 12 10 12; -fx-font-size:14px;");
        msg(message, true);
    }

    private void resetStyles() {
        String base = "-fx-background-color:#111827; -fx-text-fill:#ffffff;" +
                "-fx-background-radius:8px; -fx-border-color:#2a3142;" +
                "-fx-border-radius:8px; -fx-border-width:1px;" +
                "-fx-padding:10 12 10 12; -fx-font-size:14px;";
        descField.setStyle(base);
        montantField.setStyle(base);
        datePicker.setStyle("-fx-background-color:#111827; -fx-font-size:14px;" +
                "-fx-background-radius:8px; -fx-border-color:#2a3142;" +
                "-fx-border-radius:8px; -fx-border-width:1px;");
        statutCombo.setStyle("-fx-background-color:#111827; -fx-font-size:14px;" +
                "-fx-background-radius:8px; -fx-border-color:#2a3142;" +
                "-fx-border-radius:8px; -fx-border-width:1px;");
        catCombo.setStyle("-fx-background-color:#111827; -fx-font-size:14px;" +
                "-fx-background-radius:8px; -fx-border-color:#2a3142;" +
                "-fx-border-radius:8px; -fx-border-width:1px;");
        if (msgLabel != null) msgLabel.setText("");
    }

    private void chargerCategories() {
        try {
            catCombo.getItems().setAll(catService.afficher());
            catCombo.setConverter(new StringConverter<Categorie>() {
                @Override public String toString(Categorie c) {
                    return c == null ? "" : c.getIdCategorie() + " - " + c.getNomCategorie();
                }
                @Override public Categorie fromString(String s) { return null; }
            });
        } catch (SQLException e) { msg("Erreur categories : " + e.getMessage(), true); }
    }

    private void setModeEdition(boolean edition) {
        modeEdition = edition;
        if (btnAjouterBox  != null) { btnAjouterBox.setVisible(!edition);  btnAjouterBox.setManaged(!edition); }
        if (btnModifierBox != null) { btnModifierBox.setVisible(edition);  btnModifierBox.setManaged(edition); }
    }

    @FXML public void ouvrirFormulaire() { viderFormulaire(); setModeEdition(false); showForm(true); }
    @FXML public void annuler()          { viderFormulaire(); setModeEdition(false); showForm(false); }

    private void showForm(boolean visible) {
        if (formPane == null) return;
        formPane.setVisible(visible);
        formPane.setManaged(visible);
        if (overlayPane != null) {
            overlayPane.setVisible(visible);
            overlayPane.setManaged(visible);
        }
        if (!visible && msgLabel != null) msgLabel.setText("");
    }

    @FXML
    public void ajouter() {
        if (!validerFormulaire()) return;
        double montant = Double.parseDouble(montantField.getText().trim());
        if (!verifierBudgetMax(montant)) return;
        try {
            service.ajouter(new Depense(
                    descField.getText().trim(), montant,
                    java.sql.Date.valueOf(datePicker.getValue()),
                    statutCombo.getValue(),
                    catCombo.getValue().getIdCategorie()));
            msg("Depense ajoutee avec succes !", false);
            viderFormulaire(); showForm(false); charger();
        } catch (SQLException e) { msg("Erreur BD : " + e.getMessage(), true); }
    }

    @FXML
    public void modifier() {
        if (selectedDepense == null) { msg("Selectionnez une depense d'abord", true); return; }
        if (!validerFormulaire()) return;
        double montant = Double.parseDouble(montantField.getText().trim());
        if (!verifierBudgetMax(montant)) return;
        try {
            service.modifier(new Depense(
                    selectedDepense.getIdDepense(),
                    descField.getText().trim(), montant,
                    java.sql.Date.valueOf(datePicker.getValue()),
                    statutCombo.getValue(),
                    catCombo.getValue().getIdCategorie()));
            msg("Depense modifiee avec succes !", false);
            viderFormulaire(); setModeEdition(false); showForm(false);
            selectedDepense = null; charger();
        } catch (SQLException e) { msg("Erreur BD : " + e.getMessage(), true); }
    }

    @FXML
    public void charger() {
        try {
            allDepenses = FXCollections.observableArrayList(service.afficher());
            afficherCartes(allDepenses);
            if (searchField != null) searchField.clear();
        } catch (SQLException e) { msg("Erreur BD : " + e.getMessage(), true); }
    }

    @FXML
    public void filtrer() {
        try {
            String minTxt = montantMinField.getText().trim();
            String maxTxt = montantMaxField.getText().trim();
            if (!minTxt.isEmpty() && !maxTxt.isEmpty()) {
                double min = Double.parseDouble(minTxt);
                double max = Double.parseDouble(maxTxt);
                if (min > max) { msg("Montant min > max", true); return; }
            }
            double min = minTxt.isEmpty() ? Double.MIN_VALUE : Double.parseDouble(minTxt);
            double max = maxTxt.isEmpty() ? Double.MAX_VALUE : Double.parseDouble(maxTxt);
            afficherCartes(allDepenses.filtered(d -> d.getMontant() >= min && d.getMontant() <= max));
        } catch (NumberFormatException e) { msg("Montant invalide", true); }
    }

    @FXML
    public void trierDate() {
        List<Depense> sorted = new java.util.ArrayList<>(allDepenses);
        sorted.sort((a, b) -> {
            if (a.getDateDepense() == null) return 1;
            if (b.getDateDepense() == null) return -1;
            return b.getDateDepense().compareTo(a.getDateDepense());
        });
        afficherCartes(sorted);
    }

    @FXML
    public void trierMontant() {
        List<Depense> sorted = new java.util.ArrayList<>(allDepenses);
        sorted.sort((a, b) -> Double.compare(b.getMontant(), a.getMontant()));
        afficherCartes(sorted);
    }

    public List<Depense> listerDepenses() {
        try { return service.afficher(); }
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
        resetStyles(); selectedDepense = null;
    }

    private void msg(String m, boolean erreur) {
        if (msgLabel == null) return;
        msgLabel.setText(m);
        msgLabel.setStyle(erreur
                ? "-fx-text-fill:#ef4444; -fx-font-size:12px;"
                : "-fx-text-fill:#00d4ff; -fx-font-size:12px;");
    }

    // ═══════════════════════════════════════════════════════
    // ║  RECOMMANDATIONS AI (Ollama)                        ║
    // ═══════════════════════════════════════════════════════

    private static final String OLLAMA_URL   = "http://localhost:11434/api/generate";
    private static final String OLLAMA_MODEL = "llama3.2";

    @FXML
    public void genererRecommandations() {
        if (allDepenses.isEmpty()) {
            msg("Aucune depense a analyser", true);
            return;
        }

        Stage loadingStage = new Stage();
        loadingStage.initModality(Modality.APPLICATION_MODAL);
        loadingStage.setTitle("Analyse en cours...");
        loadingStage.setResizable(false);

        VBox loadingRoot = new VBox(16);
        loadingRoot.setAlignment(Pos.CENTER);
        loadingRoot.setPrefSize(300, 150);
        loadingRoot.setStyle("-fx-background-color:" + COULEUR_FOND + "; -fx-padding:30px;");

        Label loadingLabel = new Label("Analyse de vos depenses...");
        loadingLabel.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:" + COULEUR_ACCENT + ";");

        Label subLabel = new Label("Ollama reflechit, patientez...");
        subLabel.setStyle("-fx-font-size:12px; -fx-text-fill:" + COULEUR_SECOND + ";");

        loadingRoot.getChildren().addAll(loadingLabel, subLabel);
        loadingStage.setScene(new Scene(loadingRoot));
        loadingStage.show();

        String prompt = construirePrompt();
        Thread thread = new Thread(() -> {
            try {
                String reponse = appellerOllama(prompt);
                javafx.application.Platform.runLater(() -> {
                    loadingStage.close();
                    afficherRecommandations(reponse);
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    loadingStage.close();
                    msg("Erreur Ollama : " + e.getMessage(), true);
                });
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private String construirePrompt() {
        double total     = allDepenses.stream().mapToDouble(Depense::getMontant).sum();
        double moyenne   = allDepenses.stream().mapToDouble(Depense::getMontant).average().orElse(0);
        double max       = allDepenses.stream().mapToDouble(Depense::getMontant).max().orElse(0);
        double min       = allDepenses.stream().mapToDouble(Depense::getMontant).min().orElse(0);
        long   nbTotal   = allDepenses.size();
        long   nbPaye    = allDepenses.stream().filter(d -> normaliserStatut(d.getStatut()).equals(STATUT_PAYE)).count();
        long   nbAttente = allDepenses.stream().filter(d -> normaliserStatut(d.getStatut()).equals(STATUT_ATTENTE)).count();
        long   nbNonPaye = allDepenses.stream().filter(d -> normaliserStatut(d.getStatut()).equals(STATUT_NON_PAYE)).count();

        Map<Integer, Double> parCategorie = new HashMap<>();
        for (Depense d : allDepenses) {
            parCategorie.merge(d.getIdCategorie(), d.getMontant(), Double::sum);
        }
        StringBuilder sbCat = new StringBuilder();
        parCategorie.entrySet().stream()
                .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
                .forEach(e -> sbCat.append("Categorie ").append(e.getKey())
                        .append(" : ").append(String.format("%.2f", e.getValue())).append(" TND\n"));

        StringBuilder sbAll = new StringBuilder();
        allDepenses.forEach(d -> sbAll
                .append("- ").append(d.getDescription())
                .append(" | ").append(String.format("%.2f", d.getMontant())).append(" TND")
                .append(" | ").append(normaliserStatut(d.getStatut()))
                .append(" | Cat.").append(d.getIdCategorie())
                .append(" | ").append(d.getDateDepense() != null ? d.getDateDepense().toString() : "?")
                .append("\n"));

        return "Voici les depenses reelles d'un utilisateur. Analyse-les et donne 5 recommandations SPECIFIQUES basees uniquement sur ces donnees. "
                + "Ne parle que de ce que tu vois dans ces chiffres. Cite les montants reels et les descriptions reelles. "
                + "Reponds en francais, de maniere directe et concrete. Pas d'introduction generale.\n\n"
                + "STATISTIQUES :\n"
                + "- Nombre de depenses : " + nbTotal + "\n"
                + "- Total depense : " + String.format("%.2f", total) + " TND\n"
                + "- Moyenne par depense : " + String.format("%.2f", moyenne) + " TND\n"
                + "- Depense max : " + String.format("%.2f", max) + " TND\n"
                + "- Depense min : " + String.format("%.2f", min) + " TND\n"
                + "- Payees : " + nbPaye + " | En attente : " + nbAttente + " | Non payees : " + nbNonPaye + "\n\n"
                + "TOTAL PAR CATEGORIE :\n" + sbCat + "\n"
                + "LISTE COMPLETE DES DEPENSES :\n" + sbAll + "\n"
                + "RECOMMANDATIONS SPECIFIQUES (cite les noms et montants reels) :\n"
                + "1.";
    }

    private String appellerOllama(String prompt) throws Exception {
        String jsonBody = "{"
                + "\"model\":\"" + OLLAMA_MODEL + "\","
                + "\"prompt\":" + jsonEscape(prompt) + ","
                + "\"stream\":false"
                + "}";

        java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .build();

        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(OLLAMA_URL))
                .header("Content-Type", "application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(jsonBody))
                .timeout(java.time.Duration.ofSeconds(120))
                .build();

        java.net.http.HttpResponse<String> response = client.send(
                request, java.net.http.HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new Exception("Ollama a retourne HTTP " + response.statusCode()
                    + " — verifie qu'Ollama tourne sur localhost:11434");
        }

        String body = response.body();

        int idx = body.indexOf("\"response\":");
        if (idx == -1) throw new Exception("Reponse Ollama invalide : " + body);

        int start = body.indexOf("\"", idx + 11) + 1;
        if (start <= 0) throw new Exception("Impossible de parser la reponse Ollama");

        int end = start;
        while (end < body.length()) {
            char c = body.charAt(end);
            if (c == '\\') {
                end += 2;
            } else if (c == '"') {
                break;
            } else {
                end++;
            }
        }

        if (end <= start) throw new Exception("Impossible de parser la reponse Ollama");

        return body.substring(start, end)
                .replace("\\n", "\n")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    private String jsonEscape(String text) {
        return "\"" + text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                + "\"";
    }

    private void afficherRecommandations(String recommandations) {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.setTitle("Recommandations AI - AIVA");
        popup.setResizable(true);

        VBox root = new VBox(16);
        root.setStyle("-fx-background-color:" + COULEUR_FOND + "; -fx-padding:28px;");
        root.setPrefWidth(620);

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titreBox = new VBox(2);
        Label titre = new Label("Recommandations AI");
        titre.setStyle("-fx-font-size:20px; -fx-font-weight:bold; -fx-text-fill:#a855f7;");
        Label sousTitre = new Label("Analyse de vos " + allDepenses.size() + " depenses · Ollama " + OLLAMA_MODEL);
        sousTitre.setStyle("-fx-font-size:12px; -fx-text-fill:" + COULEUR_SECOND + ";");
        titreBox.getChildren().addAll(titre, sousTitre);
        header.getChildren().add(titreBox);

        javafx.scene.control.Separator sep = new javafx.scene.control.Separator();
        sep.setStyle("-fx-background-color:#a855f7; -fx-opacity:0.4;");

        Label contenu = new Label(recommandations);
        contenu.setWrapText(true);
        contenu.setStyle(
                "-fx-font-size:14px; -fx-text-fill:" + COULEUR_TEXTE + ";" +
                        "-fx-line-spacing:4px; -fx-background-color:" + COULEUR_SURFACE + ";" +
                        "-fx-background-radius:12px; -fx-padding:20px;" +
                        "-fx-border-color:#a855f740; -fx-border-radius:12px; -fx-border-width:1px;");
        contenu.setMaxWidth(560);

        ScrollPane scroll = new ScrollPane(contenu);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(420);
        scroll.setStyle("-fx-background:transparent; -fx-background-color:transparent; -fx-border-width:0;");

        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_RIGHT);

        Label modelInfo = new Label("Powered by Ollama · " + OLLAMA_MODEL);
        modelInfo.setStyle("-fx-font-size:11px; -fx-text-fill:#4a5568;");
        HBox.setHgrow(modelInfo, Priority.ALWAYS);

        Button btnFermer = new Button("Fermer");
        btnFermer.setStyle(
                "-fx-background-color:#a855f720; -fx-text-fill:#a855f7;" +
                        "-fx-background-radius:8px; -fx-padding:10 30 10 30;" +
                        "-fx-border-color:#a855f7; -fx-border-width:1px;" +
                        "-fx-border-radius:8px; -fx-cursor:hand; -fx-font-size:14px; -fx-font-weight:bold;");
        btnFermer.setOnAction(e -> popup.close());

        footer.getChildren().addAll(modelInfo, btnFermer);
        root.getChildren().addAll(header, sep, scroll, footer);

        Scene scene = new Scene(root);
        popup.setScene(scene);
        popup.showAndWait();
    }

    // ═══════════════════════════════════════════════════════
    // ║  ENVOI EMAIL RAPPEL                                 ║
    // ═══════════════════════════════════════════════════════

    @FXML
    public void envoyerRappelMail() {
        // Filtrer les dépenses non payées et en attente
        List<Depense> impayees = allDepenses.stream()
                .filter(d -> {
                    String s = normaliserStatut(d.getStatut());
                    return s.equals(STATUT_NON_PAYE) || s.equals(STATUT_ATTENTE);
                })
                .collect(Collectors.toList());

        if (impayees.isEmpty()) {
            msg("Toutes les dépenses sont déjà payées !", false);
            return;
        }

        // Popup pour saisir l'adresse destinataire
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.setTitle("Envoyer un rappel par email");
        popup.setResizable(false);

        VBox root = new VBox(16);
        root.setAlignment(Pos.CENTER);
        root.setPrefWidth(420);
        root.setStyle("-fx-background-color:" + COULEUR_FOND + "; -fx-padding:32px;");

        Label titre = new Label("📧 Envoyer un rappel");
        titre.setStyle("-fx-font-size:20px; -fx-font-weight:bold; -fx-text-fill:" + COULEUR_TEXTE + ";");

        Label info = new Label(impayees.size() + " dépense(s) non réglée(s) · "
                + String.format("%.2f TND", impayees.stream().mapToDouble(Depense::getMontant).sum()));
        info.setStyle("-fx-font-size:13px; -fx-text-fill:#ef4444;");

        Label lblEmail = new Label("Adresse email du destinataire :");
        lblEmail.setStyle("-fx-font-size:13px; -fx-text-fill:" + COULEUR_SECOND + ";");

        TextField emailField = new TextField();
        emailField.setPromptText("ex: comptable@entreprise.com");
        emailField.setPrefHeight(44);
        emailField.setStyle("-fx-background-color:#111827; -fx-text-fill:#ffffff;" +
                "-fx-background-radius:8px; -fx-border-color:#2a3142;" +
                "-fx-border-radius:8px; -fx-border-width:1px;" +
                "-fx-padding:10 12 10 12; -fx-font-size:14px;");

        Label statusLabel = new Label();
        statusLabel.setStyle("-fx-font-size:12px; -fx-text-fill:" + COULEUR_ACCENT + ";");

        Button btnEnvoyer = new Button("Envoyer le rappel");
        btnEnvoyer.setPrefHeight(44);
        btnEnvoyer.setStyle("-fx-background-color:#00d4ff; -fx-text-fill:#0a0e1a;" +
                "-fx-font-weight:bold; -fx-background-radius:8px;" +
                "-fx-padding:10 28 10 28; -fx-cursor:hand; -fx-font-size:14px;");

        Button btnAnnuler = new Button("Annuler");
        btnAnnuler.setPrefHeight(44);
        btnAnnuler.setStyle("-fx-background-color:#1e2538; -fx-text-fill:#94a3b8;" +
                "-fx-background-radius:8px; -fx-padding:10 28 10 28;" +
                "-fx-border-color:#2a3142; -fx-border-width:1px;" +
                "-fx-border-radius:8px; -fx-cursor:hand; -fx-font-size:14px;");

        btnAnnuler.setOnAction(e -> popup.close());

        btnEnvoyer.setOnAction(e -> {
            String dest = emailField.getText().trim();
            if (dest.isEmpty() || !dest.contains("@")) {
                statusLabel.setText("❌ Adresse email invalide");
                statusLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#ef4444;");
                return;
            }
            btnEnvoyer.setDisable(true);
            statusLabel.setText("⏳ Envoi en cours...");
            statusLabel.setStyle("-fx-font-size:12px; -fx-text-fill:" + COULEUR_ACCENT + ";");

            String finalDest = dest;
            Thread t = new Thread(() -> {
                try {
                    emailService.envoyerRappelDepensesImpayees(finalDest, impayees);
                    javafx.application.Platform.runLater(() -> {
                        statusLabel.setText("✅ Email envoyé avec succès !");
                        statusLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#22c55e;");
                        btnEnvoyer.setDisable(false);
                    });
                } catch (Exception ex) {
                    javafx.application.Platform.runLater(() -> {
                        statusLabel.setText("❌ Erreur : " + ex.getMessage());
                        statusLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#ef4444;");
                        btnEnvoyer.setDisable(false);
                    });
                }
            });
            t.setDaemon(true);
            t.start();
        });

        HBox btnRow = new HBox(10, btnEnvoyer, btnAnnuler);
        btnRow.setAlignment(Pos.CENTER);

        root.getChildren().addAll(titre, info, lblEmail, emailField, btnRow, statusLabel);
        popup.setScene(new Scene(root));
        popup.showAndWait();
    }

    // ═══════════════════════════════════════════════════════
    // ║  STATISTIQUES AVANCÉES                              ║
    // ═══════════════════════════════════════════════════════

    @FXML
    public void ouvrirStatistiques() {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.setTitle("Statistiques avancées - AIVA");
        popup.setResizable(true);

        VBox root = new VBox(16);
        root.setStyle("-fx-background-color:" + COULEUR_FOND + "; -fx-padding:28px;");
        root.setPrefWidth(900);

        // ─── Métriques en-tête ───
        double total   = allDepenses.stream().mapToDouble(Depense::getMontant).sum();
        double avg     = allDepenses.isEmpty() ? 0 : total / allDepenses.size();
        long   nbPaye  = allDepenses.stream().filter(d -> normaliserStatut(d.getStatut()).equals(STATUT_PAYE)).count();
        long   nbImpay = allDepenses.size() - nbPaye;

        HBox metrics = new HBox(12);
        metrics.getChildren().addAll(
                creerMetricCard("Total dépensé",  String.format("%.2f TND", total),  COULEUR_ACCENT),
                creerMetricCard("Moyenne",         String.format("%.2f TND", avg),    COULEUR_SECOND),
                creerMetricCard("Payées",          String.valueOf(nbPaye),             COULEUR_PAYE),
                creerMetricCard("Non réglées",     String.valueOf(nbImpay),            COULEUR_NON_PAYE)
        );
        HBox.setHgrow(metrics.getChildren().get(0), Priority.ALWAYS);

        // ─── Graphique mensuel ───
        Map<String, Double> parMois = new java.util.LinkedHashMap<>();
        Map<String, Double> paidMois = new java.util.LinkedHashMap<>();
        Map<String, Double> unpaidMois = new java.util.LinkedHashMap<>();
        DateTimeFormatter moisFmt = DateTimeFormatter.ofPattern("MMM yyyy");

        for (Depense d : allDepenses) {
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

        javafx.scene.chart.CategoryAxis xAxis = new javafx.scene.chart.CategoryAxis();
        javafx.scene.chart.NumberAxis   yAxis = new javafx.scene.chart.NumberAxis();
        xAxis.setLabel("Mois");
        yAxis.setLabel("Montant (TND)");
        javafx.scene.chart.LineChart<String, Number> lineChart =
                new javafx.scene.chart.LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Évolution mensuelle des dépenses");
        lineChart.setPrefHeight(300);

        javafx.scene.chart.XYChart.Series<String, Number> sTotal = new javafx.scene.chart.XYChart.Series<>();
        sTotal.setName("Total");
        javafx.scene.chart.XYChart.Series<String, Number> sPaid  = new javafx.scene.chart.XYChart.Series<>();
        sPaid.setName("Payées");
        javafx.scene.chart.XYChart.Series<String, Number> sUnpaid = new javafx.scene.chart.XYChart.Series<>();
        sUnpaid.setName("Non réglées");

        parMois.forEach((k, v) -> sTotal.getData().add(new javafx.scene.chart.XYChart.Data<>(k, v)));
        paidMois.forEach((k, v) -> sPaid.getData().add(new javafx.scene.chart.XYChart.Data<>(k, v)));
        unpaidMois.forEach((k, v) -> sUnpaid.getData().add(new javafx.scene.chart.XYChart.Data<>(k, v)));
        lineChart.getData().addAll(sTotal, sPaid, sUnpaid);

        // ─── Top 5 dépenses ───
        Label lblTop = new Label("Top 5 dépenses");
        lblTop.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:" + COULEUR_ACCENT + ";");
        VBox topBox = new VBox(6);
        allDepenses.stream()
                .sorted((a, b) -> Double.compare(b.getMontant(), a.getMontant()))
                .limit(5)
                .forEach(d -> {
                    HBox row = new HBox(12);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.setStyle("-fx-background-color:" + COULEUR_SURFACE + "; -fx-background-radius:8px; -fx-padding:8px;");
                    Label desc = new Label(d.getDescription());
                    desc.setStyle("-fx-text-fill:" + COULEUR_TEXTE + "; -fx-font-size:13px;");
                    desc.setMaxWidth(300); HBox.setHgrow(desc, Priority.ALWAYS);
                    Label amt = new Label(String.format("%.2f TND", d.getMontant()));
                    amt.setStyle("-fx-text-fill:" + COULEUR_ACCENT + "; -fx-font-weight:bold; -fx-font-size:13px;");
                    String[] cols = couleurStatut(d.getStatut());
                    Label stat = new Label(normaliserStatut(d.getStatut()));
                    stat.setStyle("-fx-text-fill:" + cols[0] + "; -fx-background-color:" + cols[1] +
                            "; -fx-background-radius:6px; -fx-padding:3 8 3 8; -fx-font-size:11px; -fx-font-weight:bold;");
                    row.getChildren().addAll(desc, amt, stat);
                    topBox.getChildren().add(row);
                });

        Button btnFermer = new Button("Fermer");
        btnFermer.setStyle("-fx-background-color:#a855f720; -fx-text-fill:#a855f7;" +
                "-fx-background-radius:8px; -fx-padding:10 30 10 30;" +
                "-fx-border-color:#a855f7; -fx-border-width:1px;" +
                "-fx-border-radius:8px; -fx-cursor:hand; -fx-font-size:14px;");
        btnFermer.setOnAction(e -> popup.close());

        root.getChildren().addAll(metrics, lineChart, lblTop, topBox, btnFermer);
        ScrollPane sp = new ScrollPane(root);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background:transparent; -fx-background-color:transparent;");

        popup.setScene(new Scene(sp, 920, 720));
        popup.showAndWait();
    }

    private VBox creerMetricCard(String label, String valeur, String couleur) {
        VBox card = new VBox(6);
        card.setPrefWidth(180);
        card.setStyle("-fx-background-color:" + COULEUR_SURFACE + "; -fx-background-radius:10px; -fx-padding:14px;" +
                "-fx-border-color:" + couleur + "44; -fx-border-radius:10px; -fx-border-width:1px;");
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size:12px; -fx-text-fill:" + COULEUR_SECOND + ";");
        Label val = new Label(valeur);
        val.setStyle("-fx-font-size:20px; -fx-font-weight:bold; -fx-text-fill:" + couleur + ";");
        card.getChildren().addAll(lbl, val);
        return card;
    }
}
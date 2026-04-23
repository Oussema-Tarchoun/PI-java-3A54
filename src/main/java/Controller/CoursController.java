package Controller;

import Models.Cours;
import Services.Iservice;
import Services.ServiceCours;
import utils.PDFExporter;
import utils.QRCodeGenerator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.SQLDataException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CoursController {

    // ── FXML fields ────────────────────────────────────────────────────────────

    @FXML private FlowPane         courseGrid;
    @FXML private VBox             emptyState;
    @FXML private TextField        txtSearch;
    @FXML private ComboBox<String> comboFilterCategorie;
    @FXML private ComboBox<String> comboFilterNiveau;
    @FXML private ComboBox<String> comboFilterStatus;
    @FXML private Label            lblResultCount;

    // ── Stats labels (header) ──────────────────────────────────────────────────
    @FXML private Label       lblTotalCours;
    @FXML private Label       lblActiveCountBadge;   // "↗ X actif" green badge
    @FXML private Label       lblTotalHours;

    // ── Level labels + progress bars ──────────────────────────────────────────
    @FXML private Label       lblDebutant;
    @FXML private Label       lblPctDebutant;
    @FXML private ProgressBar progressDebutant;

    @FXML private Label       lblIntermediaire;
    @FXML private Label       lblPctIntermediaire;
    @FXML private ProgressBar progressIntermediaire;

    @FXML private Label       lblAvance;
    @FXML private Label       lblPctAvance;
    @FXML private ProgressBar progressAvance;

    // ── Status stat cards ─────────────────────────────────────────────────────
    @FXML private Label lblStatActif;
    @FXML private Label lblStatInactif;
    @FXML private Label lblStatEnAttente;
    @FXML private Label lblStatEnCours;

    // ── Category distribution container ───────────────────────────────────────
    @FXML private VBox categoryStatsContainer;

    // ── Quick stats ───────────────────────────────────────────────────────────
    @FXML private Label       lblAvgDuration;
    @FXML private Label       lblTopCategory;
    @FXML private ProgressBar progressActive;
    @FXML private Label       lblActivePct;

    // ── Charts ────────────────────────────────────────────────────────────────
    @FXML private StackPane donutChartPane;
    @FXML private VBox      donutLegend;
    @FXML private VBox      barChartPane;

    // ── State ─────────────────────────────────────────────────────────────────
    private final Iservice<Cours> service;
    private ObservableList<Cours> allCours;
    private Cours                 editingCours = null;

    public CoursController() {
        this.service = new ServiceCours();
    }

    // ── Init ──────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        initializeFilters();
        setupFilterListeners();
        loadData();
    }

    private void initializeFilters() {
        comboFilterCategorie.getItems().add("All Categories");
        comboFilterCategorie.getItems().addAll("Programmation", "Design", "Marketing", "Business", "Autre");
        comboFilterCategorie.setValue("All Categories");

        comboFilterNiveau.getItems().add("All Levels");
        comboFilterNiveau.getItems().addAll("debutant", "intermediaire", "avance");
        comboFilterNiveau.setValue("All Levels");

        comboFilterStatus.getItems().add("All Statuses");
        comboFilterStatus.getItems().addAll("actif", "inactif", "en attente", "en cours");
        comboFilterStatus.setValue("All Statuses");
    }

    private void setupFilterListeners() {
        txtSearch.textProperty().addListener((obs, o, n) -> applyFilters());
        comboFilterCategorie.valueProperty().addListener((obs, o, n) -> applyFilters());
        comboFilterNiveau.valueProperty().addListener((obs, o, n) -> applyFilters());
        comboFilterStatus.valueProperty().addListener((obs, o, n) -> applyFilters());
    }

    @FXML
    private void handleResetFilters() {
        txtSearch.clear();
        comboFilterCategorie.setValue("All Categories");
        comboFilterNiveau.setValue("All Levels");
        comboFilterStatus.setValue("All Statuses");
    }

    // ── Filter / render ───────────────────────────────────────────────────────

    private void applyFilters() {
        if (allCours == null) return;

        String search = txtSearch.getText() == null ? "" : txtSearch.getText().toLowerCase();
        String cat    = comboFilterCategorie.getValue();
        String niveau = comboFilterNiveau.getValue();
        String status = comboFilterStatus.getValue();

        List<Cours> filtered = allCours.stream().filter(c -> {
            boolean matchSearch = search.isBlank()
                    || c.getTittre().toLowerCase().contains(search)
                    || c.getCategorie().toLowerCase().contains(search)
                    || c.getNiveau().toLowerCase().contains(search);
            boolean matchCat    = cat    == null || cat.startsWith("All")    || c.getCategorie().equalsIgnoreCase(cat);
            boolean matchNiveau = niveau == null || niveau.startsWith("All") || c.getNiveau().equalsIgnoreCase(niveau);
            boolean matchStatus = status == null || status.startsWith("All") || c.getStatus().equalsIgnoreCase(status);
            return matchSearch && matchCat && matchNiveau && matchStatus;
        }).collect(Collectors.toList());

        renderCards(filtered);
        lblResultCount.setText(filtered.size() + " course" + (filtered.size() != 1 ? "s" : "") + " found");
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    private void loadData() {
        try {
            List<Cours> list = service.recuperer();
            allCours = FXCollections.observableArrayList(list);
            applyFilters();
            updateStats(list);
        } catch (SQLDataException e) {
            showAlert("Error", "Could not load courses: " + e.getMessage());
        }
    }

    // ── Stats update ──────────────────────────────────────────────────────────

    private void updateStats(List<Cours> list) {
        int total = list.size();

        // ── Header ────────────────────────────────────────────────────────────
        safeText(lblTotalCours, String.valueOf(total));

        long actifCount = list.stream().filter(c -> "actif".equalsIgnoreCase(c.getStatus())).count();
        safeText(lblActiveCountBadge, "↗ " + actifCount + " actif");

        int totalHours = list.stream().mapToInt(Cours::getDureeEstimee).sum();
        safeText(lblTotalHours, totalHours + "h");

        // ── Level bars ────────────────────────────────────────────────────────
        long debutantCount      = list.stream().filter(c -> "debutant".equalsIgnoreCase(c.getNiveau())).count();
        long intermediaireCount = list.stream().filter(c -> "intermediaire".equalsIgnoreCase(c.getNiveau())).count();
        long avanceCount        = list.stream().filter(c -> "avance".equalsIgnoreCase(c.getNiveau())).count();

        double pctD = total > 0 ? (double) debutantCount / total : 0;
        double pctI = total > 0 ? (double) intermediaireCount / total : 0;
        double pctA = total > 0 ? (double) avanceCount / total : 0;

        safeText(lblDebutant, String.valueOf(debutantCount));
        safeText(lblPctDebutant, "(" + pct(pctD) + "%)");
        safeProgress(progressDebutant, pctD);

        safeText(lblIntermediaire, String.valueOf(intermediaireCount));
        safeText(lblPctIntermediaire, "(" + pct(pctI) + "%)");
        safeProgress(progressIntermediaire, pctI);

        safeText(lblAvance, String.valueOf(avanceCount));
        safeText(lblPctAvance, "(" + pct(pctA) + "%)");
        safeProgress(progressAvance, pctA);

        // ── Status cards ──────────────────────────────────────────────────────
        safeText(lblStatActif,     String.valueOf(list.stream().filter(c -> "actif".equalsIgnoreCase(c.getStatus())).count()));
        safeText(lblStatInactif,   String.valueOf(list.stream().filter(c -> "inactif".equalsIgnoreCase(c.getStatus())).count()));
        safeText(lblStatEnAttente, String.valueOf(list.stream().filter(c -> "en attente".equalsIgnoreCase(c.getStatus())).count()));
        safeText(lblStatEnCours,   String.valueOf(list.stream().filter(c -> "en cours".equalsIgnoreCase(c.getStatus())).count()));

        // ── Category distribution ─────────────────────────────────────────────
        if (categoryStatsContainer != null) {
            categoryStatsContainer.getChildren().clear();

            Map<String, Long> byCategory = list.stream()
                    .collect(Collectors.groupingBy(Cours::getCategorie, Collectors.counting()));

            String[] catColors = {"#00d4ff", "#8b5cf6", "#f59e0b", "#10b981", "#ef4444"};
            int colorIdx = 0;

            for (Map.Entry<String, Long> entry : byCategory.entrySet()) {
                double pct = total > 0 ? (double) entry.getValue() / total : 0;
                String color = catColors[colorIdx % catColors.length];
                colorIdx++;

                // Label row
                HBox labelRow = new HBox();
                labelRow.setAlignment(Pos.CENTER_LEFT);
                Label catName = new Label(entry.getKey());
                catName.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                Label catCount = new Label(entry.getValue() + " (" + pct(pct) + "%)");
                catCount.setStyle("-fx-font-size: 11px; -fx-text-fill: " + color + "; -fx-font-weight: bold;");
                labelRow.getChildren().addAll(catName, spacer, catCount);

                // Progress bar
                ProgressBar bar = new ProgressBar(pct);
                bar.setMaxWidth(Double.MAX_VALUE);
                bar.setPrefHeight(6);
                bar.setStyle("-fx-accent: " + color + "; -fx-background-color: #1e2538; -fx-background-radius: 3;");

                VBox catBlock = new VBox(4, labelRow, bar);
                categoryStatsContainer.getChildren().add(catBlock);
            }
        }

        // ── Quick stats ───────────────────────────────────────────────────────
        double avgDuration = total > 0
                ? list.stream().mapToInt(Cours::getDureeEstimee).average().orElse(0)
                : 0;
        safeText(lblAvgDuration, String.format("%.0fh", avgDuration));

        String topCat = list.stream()
                .collect(Collectors.groupingBy(Cours::getCategorie, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("—");
        safeText(lblTopCategory, topCat);

        double activePct = total > 0 ? (double) actifCount / total : 0;
        safeProgress(progressActive, activePct);
        safeText(lblActivePct, pct(activePct) + "% active courses");

        // ── Charts ────────────────────────────────────────────────────────────

    }

    // ── Charts update ─────────────────────────────────────────────────────────
/*
    private void updateCharts(List<Cours> list) {
        int total = list.size();

        // ── Donut Chart: Status Distribution ──────────────────────────────────
        if (donutChartPane != null && donutLegend != null) {
            donutChartPane.getChildren().clear();
            donutLegend.getChildren().clear();

            long actif     = list.stream().filter(c -> "actif".equalsIgnoreCase(c.getStatus())).count();
            long inactif   = list.stream().filter(c -> "inactif".equalsIgnoreCase(c.getStatus())).count();
            long enAttente = list.stream().filter(c -> "en attente".equalsIgnoreCase(c.getStatus())).count();
            long enCours   = list.stream().filter(c -> "en cours".equalsIgnoreCase(c.getStatus())).count();

            String[]   labels = {"Active", "Inactive", "Pending", "In Progress"};
            long[]     counts = {actif, inactif, enAttente, enCours};
            String[]   colors = {"#00d4ff", "#ef4444", "#f59e0b", "#10b981"};

            double radius = 80;
            double hole   = 48;
            double cx = radius + 4;
            double cy = radius + 4;

            // Background circle
            Circle bg = new Circle(cx, cy, radius);
            bg.setFill(Color.web("#1e2538"));

            Pane canvas = new Pane();
            canvas.setPrefSize((radius + 4) * 2, (radius + 4) * 2);
            canvas.getChildren().add(bg);

            double startAngle = 90; // start from top
            for (int i = 0; i < counts.length; i++) {
                if (counts[i] == 0) continue;
                double sweep = total > 0 ? (counts[i] / (double) total) * 360.0 : 0;

                Arc arc = new Arc(cx, cy, radius, radius, startAngle, sweep);
                arc.setType(ArcType.ROUND);
                arc.setFill(Color.web(colors[i]));
                arc.setStroke(Color.web("#111827"));
                arc.setStrokeWidth(2);
                canvas.getChildren().add(arc);

                startAngle += sweep;
            }

            // Donut hole
            Circle hole2 = new Circle(cx, cy, hole);
            hole2.setFill(Color.web("#111827"));
            canvas.getChildren().add(hole2);

            // Center label
            Label centerLabel = new Label(String.valueOf(total));
            centerLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");
            Label centerSub = new Label("total");
            centerSub.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748b;");
            VBox centerBox = new VBox(2, centerLabel, centerSub);
            centerBox.setAlignment(Pos.CENTER);

            donutChartPane.getChildren().addAll(canvas, centerBox);
            donutChartPane.setAlignment(Pos.CENTER);

            // Legend
            for (int i = 0; i < counts.length; i++) {
                if (counts[i] == 0) continue;
                int pct = total > 0 ? (int) Math.round(counts[i] * 100.0 / total) : 0;

                Rectangle dot = new Rectangle(10, 10);
                dot.setArcWidth(3);
                dot.setArcHeight(3);
                dot.setFill(Color.web(colors[i]));

                Label lbl = new Label(labels[i]);
                lbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8;");

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                Label val = new Label(counts[i] + "  (" + pct + "%)");
                val.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + colors[i] + ";");

                HBox row = new HBox(8, dot, lbl, spacer, val);
                row.setAlignment(Pos.CENTER_LEFT);
                donutLegend.getChildren().add(row);
            }
        }

        // ── Bar Chart: Courses per Level ──────────────────────────────────────
        if (barChartPane != null) {
            barChartPane.getChildren().clear();

            long debutant      = list.stream().filter(c -> "debutant".equalsIgnoreCase(c.getNiveau())).count();
            long intermediaire = list.stream().filter(c -> "intermediaire".equalsIgnoreCase(c.getNiveau())).count();
            long avance        = list.stream().filter(c -> "avance".equalsIgnoreCase(c.getNiveau())).count();

            String[] barLabels = {"Beginner", "Intermediate", "Advanced"};
            long[]   barCounts = {debutant, intermediaire, avance};
            String[] barColors = {"#00d4ff", "#f59e0b", "#ef4444"};

            long maxVal = Math.max(1, Math.max(debutant, Math.max(intermediaire, avance)));

            for (int i = 0; i < barLabels.length; i++) {
                double ratio = barCounts[i] / (double) maxVal;
                int pct = total > 0 ? (int) Math.round(barCounts[i] * 100.0 / total) : 0;

                // Label row
                Label nameLbl = new Label(barLabels[i]);
                nameLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8; -fx-min-width: 100;");
                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                Label countLbl = new Label(barCounts[i] + "  (" + pct + "%)");
                countLbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + barColors[i] + ";");
                HBox labelRow = new HBox(nameLbl, spacer, countLbl);
                labelRow.setAlignment(Pos.CENTER_LEFT);

                // Bar track
                StackPane track = new StackPane();
                track.setPrefHeight(14);
                track.setMaxWidth(Double.MAX_VALUE);
                track.setStyle("-fx-background-color: #1e2538; -fx-background-radius: 7;");

                // Filled bar
                Region fill = new Region();
                fill.setPrefHeight(14);
                fill.setStyle("-fx-background-color: " + barColors[i] + "; -fx-background-radius: 7;");
                fill.setMaxWidth(Double.MAX_VALUE);

                // Use HBox to control fill width proportionally
                HBox barRow = new HBox();
                barRow.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(barRow, Priority.ALWAYS);

                Region filledPart = new Region();
                filledPart.setPrefHeight(14);
                filledPart.setStyle("-fx-background-color: " + barColors[i] + "; -fx-background-radius: 7;");
                HBox.setHgrow(filledPart, Priority.NEVER);

                Region emptyPart = new Region();
                emptyPart.setPrefHeight(14);
                emptyPart.setStyle("-fx-background-color: #1e2538; -fx-background-radius: 7;");
                HBox.setHgrow(emptyPart, Priority.ALWAYS);

                // Bind fill width to parent width * ratio
                barRow.getChildren().addAll(filledPart, emptyPart);
                barRow.setStyle("-fx-background-color: #1e2538; -fx-background-radius: 7;");

                final double r = ratio;
                barRow.widthProperty().addListener((obs, oldW, newW) ->
                        filledPart.setPrefWidth(newW.doubleValue() * r));

                VBox barBlock = new VBox(6, labelRow, barRow);
                VBox.setVgrow(barBlock, Priority.NEVER);
                barChartPane.getChildren().add(barBlock);
            }
        }
    }*/

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Null-safe label setText */
    private void safeText(Label lbl, String text) {
        if (lbl != null) lbl.setText(text);
    }

    /** Null-safe progress bar setProgress */
    private void safeProgress(ProgressBar bar, double value) {
        if (bar != null) bar.setProgress(value);
    }

    /** Format a 0-1 ratio as a clean percentage string */
    private int pct(double ratio) {
        return (int) Math.round(ratio * 100);
    }

    // ── Cards ─────────────────────────────────────────────────────────────────

    private void renderCards(List<Cours> list) {
        courseGrid.getChildren().clear();

        boolean isEmpty = list == null || list.isEmpty();
        emptyState.setVisible(isEmpty);
        emptyState.setManaged(isEmpty);

        if (isEmpty) return;

        for (Cours cours : list) {
            courseGrid.getChildren().add(buildCard(cours));
        }
    }

    private VBox buildCard(Cours cours) {
        VBox card = new VBox(12);
        card.setPrefWidth(380);
        card.setMaxWidth(380);
        card.setPadding(new Insets(20));
        card.setStyle(
                "-fx-background-color: #161b2e;" +
                        "-fx-background-radius: 14;" +
                        "-fx-border-radius: 14;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-color: #2a3142;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 16, 0, 0, 4);" +
                        "-fx-cursor: hand;"
        );

        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label(cours.getTittre());
        titleLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(260);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        Label statusBadge = buildStatusBadge(cours.getStatus());
        topRow.getChildren().addAll(titleLabel, statusBadge);

        HBox metaRow = new HBox(14);
        metaRow.setAlignment(Pos.CENTER_LEFT);
        metaRow.getChildren().addAll(
                metaChip("🏷 " + cours.getCategorie()),
                metaChip("📶 " + cours.getNiveau()),
                metaChip("⏱ " + cours.getDureeEstimee() + "h")
        );

        Label descLabel = new Label(cours.getDescription() == null || cours.getDescription().isBlank()
                ? "No description provided." : cours.getDescription());
        descLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        descLabel.setWrapText(true);
        descLabel.setMaxHeight(40);

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button btnQR     = iconButton("📱", "#00d4ff", "Show QR Code");
        Button btnPDF    = iconButton("📄", "#10b981", "Export PDF");
        Button btnEdit   = iconButton("✏️", "#8b5cf6", "Edit");
        Button btnDelete = iconButton("🗑", "#ef4444", "Delete");

        btnQR.setOnAction(e -> handleShowQRCode(cours));
        btnPDF.setOnAction(e -> handleExportPDF(cours));
        btnEdit.setOnAction(e -> handleEdit(cours));
        btnDelete.setOnAction(e -> handleDelete(cours));

        actions.getChildren().addAll(btnQR, btnPDF, btnEdit, btnDelete);
        card.getChildren().addAll(topRow, metaRow, descLabel, actions);
        return card;
    }

    private Label buildStatusBadge(String status) {
        Label badge = new Label(status == null ? "—" : status);
        badge.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 4 10;");
        if (status == null) return badge;
        switch (status.toLowerCase()) {
            case "actif"      -> badge.setStyle(badge.getStyle() + "-fx-background-color: rgba(0,212,255,0.15);  -fx-text-fill: #00d4ff;");
            case "inactif"    -> badge.setStyle(badge.getStyle() + "-fx-background-color: rgba(239,68,68,0.15);  -fx-text-fill: #ef4444;");
            case "en attente" -> badge.setStyle(badge.getStyle() + "-fx-background-color: rgba(245,158,11,0.15); -fx-text-fill: #f59e0b;");
            case "en cours"   -> badge.setStyle(badge.getStyle() + "-fx-background-color: rgba(16,185,129,0.15); -fx-text-fill: #10b981;");
            default           -> badge.setStyle(badge.getStyle() + "-fx-background-color: rgba(100,116,139,0.15); -fx-text-fill: #64748b;");
        }
        return badge;
    }

    private Label metaChip(String text) {
        Label chip = new Label(text);
        chip.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
        return chip;
    }

    private Button iconButton(String icon, String color, String tooltip) {
        Button btn = new Button(icon);
        btn.setTooltip(new Tooltip(tooltip));
        btn.setStyle(
                "-fx-background-color: rgba(255,255,255,0.06);" +
                        "-fx-text-fill: " + color + ";" +
                        "-fx-background-radius: 8;" +
                        "-fx-font-size: 14px;" +
                        "-fx-padding: 8 12;" +
                        "-fx-cursor: hand;"
        );
        return btn;
    }

    // ── CRUD handlers ─────────────────────────────────────────────────────────

    @FXML
    private void handleAdd() {
        editingCours = null;
        openDialog(null);
    }

    private void handleEdit(Cours cours) {
        editingCours = cours;
        openDialog(cours);
    }

    private void handleDelete(Cours cours) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete \"" + cours.getTittre() + "\"?", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    service.supprimer(cours);
                    loadData();
                } catch (SQLDataException e) {
                    showAlert("Error", "Could not delete course: " + e.getMessage());
                }
            }
        });
    }

    private void openChapitres(Cours cours) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Chapitre.fxml"));
            Parent root = loader.load();
            ChapitreController controller = loader.getController();
            controller.setCours(cours);
            Stage stage = new Stage();
            stage.setTitle("Chapters — " + cours.getTittre());
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            showAlert("Error", "Could not open chapters: " + e.getMessage());
        }
    }

    // ── QR Code ───────────────────────────────────────────────────────────────

    private void handleShowQRCode(Cours cours) {
        try {
            String qrContent = String.format("AIVA-COURSE|%d|%s|%s|%s",
                    cours.getId(), cours.getTittre(), cours.getCategorie(), cours.getNiveau());

            BufferedImage qrImage = QRCodeGenerator.generateQRCodeImage(qrContent, 250, 250);
            Image fxImage = SwingFXUtils.toFXImage(qrImage, null);

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initStyle(StageStyle.UNDECORATED);

            ImageView imageView = new ImageView(fxImage);
            imageView.setFitWidth(250);
            imageView.setFitHeight(250);
            imageView.setPreserveRatio(true);

            Label infoLabel = new Label("Scan to view course details");
            infoLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8;");

            Label courseLabel = new Label(cours.getTittre());
            courseLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

            Button btnDownload = new Button("💾 Save QR");
            btnDownload.setStyle("-fx-background-color: #8b5cf6; -fx-text-fill: #ffffff; -fx-background-radius: 8; -fx-padding: 8 16;");
            btnDownload.setOnAction(e -> saveQRCodeToFile(qrImage, cours.getTittre()));

            Button btnClose = new Button("Close");
            btnClose.setStyle("-fx-background-color: #1e2538; -fx-text-fill: #ffffff; -fx-background-radius: 8; -fx-padding: 8 16;");
            btnClose.setOnAction(e -> dialog.close());

            HBox buttons = new HBox(10, btnDownload, btnClose);
            buttons.setAlignment(Pos.CENTER);

            VBox root = new VBox(15, courseLabel, imageView, infoLabel, buttons);
            root.setAlignment(Pos.CENTER);
            root.setPadding(new Insets(30));
            root.setStyle("-fx-background-color: #161b2e; -fx-border-color: #2a3142; -fx-border-radius: 12; -fx-background-radius: 12;");

            StackPane overlay = new StackPane(root);
            overlay.setStyle("-fx-background-color: rgba(0,0,0,0.7);");

            Scene scene = new Scene(overlay, 400, 450);
            scene.setFill(Color.TRANSPARENT);
            dialog.setScene(scene);
            dialog.show();
        } catch (Exception e) {
            showAlert("Error", "Could not generate QR code: " + e.getMessage());
        }
    }

    private void saveQRCodeToFile(BufferedImage qrImage, String courseTitle) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Save QR Code");
        fc.setInitialFileName("QR_" + courseTitle.replaceAll("[^a-zA-Z0-9]", "_") + ".png");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Image", "*.png"));
        File file = fc.showSaveDialog(courseGrid.getScene().getWindow());
        if (file != null) {
            try {
                ImageIO.write(qrImage, "PNG", file);
                showAlert("Success", "QR Code saved to:\n" + file.getAbsolutePath());
            } catch (IOException e) {
                showAlert("Error", "Could not save QR code: " + e.getMessage());
            }
        }
    }

    // ── PDF ───────────────────────────────────────────────────────────────────

    private void handleExportPDF(Cours cours) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Export Course to PDF");
        fc.setInitialFileName(cours.getTittre().replaceAll("[^a-zA-Z0-9]", "_") + ".pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Document", "*.pdf"));
        File file = fc.showSaveDialog(courseGrid.getScene().getWindow());
        if (file != null) {
            try {
                PDFExporter.exportCourseToPDF(cours, file);
                showAlert("Success", "Course exported to PDF:\n" + file.getAbsolutePath());
            } catch (IOException e) {
                showAlert("Error", "Could not export PDF: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleExportAllPDF() {
        if (allCours == null || allCours.isEmpty()) {
            showAlert("Info", "No courses to export.");
            return;
        }
        FileChooser fc = new FileChooser();
        fc.setTitle("Export All Courses to PDF");
        fc.setInitialFileName("AIVA_Courses_Catalog.pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Document", "*.pdf"));
        File file = fc.showSaveDialog(courseGrid.getScene().getWindow());
        if (file != null) {
            try {
                PDFExporter.exportCoursesListToPDF(allCours, file);
                showAlert("Success", "Courses catalog exported to:\n" + file.getAbsolutePath());
            } catch (IOException e) {
                showAlert("Error", "Could not export PDF: " + e.getMessage());
            }
        }
    }

    // ── Add / Edit dialog ─────────────────────────────────────────────────────

    private void openDialog(Cours cours) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.setTitle(cours == null ? "Add Course" : "Edit Course");

        TextField        fTitre       = styledField("Course title");
        TextField        fDuree       = styledField("Duration in hours (e.g. 10)");
        TextArea         fDescription = styledArea("Course description");
        ComboBox<String> fNiveau      = styledCombo("Select level",    "debutant", "intermediaire", "avance");
        ComboBox<String> fCategorie   = styledCombo("Select category", "Programmation", "Design", "Marketing", "Business", "Autre");

        if (cours != null) {
            fTitre.setText(cours.getTittre());
            fDuree.setText(String.valueOf(cours.getDureeEstimee()));
            fDescription.setText(cours.getDescription());
            fNiveau.setValue(cours.getNiveau());
            fCategorie.setValue(cours.getCategorie());
        }

        Label errorLabel = new Label("");
        errorLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px;");

        VBox form = new VBox(14);
        form.getChildren().addAll(
                fieldBlock("Course Title",     fTitre),
                rowBlock(fieldBlock("Level", fNiveau), fieldBlock("Category", fCategorie)),
                fieldBlock("Duration (hours)", fDuree),
                fieldBlock("Description",      fDescription),
                errorLabel
        );

        Button btnCancel = new Button("Cancel");
        btnCancel.setStyle("-fx-background-color: #1e2538; -fx-text-fill: #ffffff; -fx-background-radius: 8; -fx-padding: 10 24; -fx-cursor: hand;");
        btnCancel.setOnAction(e -> dialog.close());

        Button btnSave = new Button(cours == null ? "Add Course" : "Save Changes");
        btnSave.setStyle("-fx-background-color: #00d4ff; -fx-text-fill: #0a0e1a; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10 24; -fx-cursor: hand;");
        btnSave.setOnAction(e -> {
            if (fTitre.getText().isBlank())    { errorLabel.setText("Title is required.");    return; }
            if (fNiveau.getValue() == null)    { errorLabel.setText("Level is required.");    return; }
            if (fCategorie.getValue() == null) { errorLabel.setText("Category is required."); return; }
            if (fDuree.getText().isBlank())    { errorLabel.setText("Duration is required."); return; }
            int duree;
            try {
                duree = Integer.parseInt(fDuree.getText().trim());
                if (duree <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                errorLabel.setText("Duration must be a positive number.");
                return;
            }
            try {
                if (editingCours == null) {
                    Cours newCours = new Cours();
                    newCours.setTittre(fTitre.getText().trim());
                    newCours.setDescription(fDescription.getText().trim());
                    newCours.setNiveau(fNiveau.getValue());
                    newCours.setCategorie(fCategorie.getValue());
                    newCours.setDureeEstimee(duree);
                    newCours.setStatus("actif");
                    newCours.setDateCreation(java.time.LocalDate.now().toString());
                    newCours.setUser_id(1);
                    service.ajouter(newCours);
                } else {
                    editingCours.setTittre(fTitre.getText().trim());
                    editingCours.setDescription(fDescription.getText().trim());
                    editingCours.setNiveau(fNiveau.getValue());
                    editingCours.setCategorie(fCategorie.getValue());
                    editingCours.setDureeEstimee(duree);
                    service.modifier(editingCours);
                }
                dialog.close();
                loadData();
            } catch (SQLDataException ex) {
                errorLabel.setText("Database error: " + ex.getMessage());
            }
        });

        HBox dialogButtons = new HBox(12, btnCancel, btnSave);
        dialogButtons.setAlignment(Pos.CENTER_RIGHT);

        Label dlgTitle = new Label(cours == null ? "Add New Course" : "Edit Course");
        dlgTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        Button btnX = new Button("✕");
        btnX.setStyle("-fx-background-color: transparent; -fx-text-fill: #64748b; -fx-font-size: 16px; -fx-cursor: hand;");
        btnX.setOnAction(e -> dialog.close());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(dlgTitle, spacer, btnX);
        header.setAlignment(Pos.CENTER_LEFT);

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #2a3142;");

        VBox root = new VBox(20, header, sep, form, dialogButtons);
        root.setPadding(new Insets(28));
        root.setStyle(
                "-fx-background-color: #161b2e;" +
                        "-fx-background-radius: 16;" +
                        "-fx-border-radius: 16;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-color: #2a3142;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 30, 0, 0, 8);"
        );
        root.setPrefWidth(520);

        StackPane overlay = new StackPane(root);
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.65);");
        overlay.setPadding(new Insets(40));

        Scene scene = new Scene(overlay, 620, 560);
        scene.setFill(Color.TRANSPARENT);
        dialog.setScene(scene);
        dialog.show();
    }

    // ── Form helpers ──────────────────────────────────────────────────────────

    private TextField styledField(String prompt) {
        TextField f = new TextField();
        f.setPromptText(prompt);
        f.setStyle("-fx-background-color: #111827; -fx-text-fill: #ffffff; -fx-prompt-text-fill: #64748b;" +
                "-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-width: 1;" +
                "-fx-border-color: #2a3142; -fx-padding: 10 14; -fx-font-size: 13px;");
        return f;
    }

    private TextArea styledArea(String prompt) {
        TextArea a = new TextArea();
        a.setPromptText(prompt);
        a.setPrefRowCount(3);
        a.setWrapText(true);
        a.setStyle("-fx-background-color: #111827; -fx-text-fill: #ffffff; -fx-prompt-text-fill: #64748b;" +
                "-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-width: 1;" +
                "-fx-border-color: #2a3142; -fx-padding: 10 14; -fx-font-size: 13px;");
        return a;
    }

    private ComboBox<String> styledCombo(String prompt, String... items) {
        ComboBox<String> c = new ComboBox<>();
        c.setPromptText(prompt);
        c.getItems().addAll(items);
        c.setMaxWidth(Double.MAX_VALUE);
        c.setStyle("-fx-background-color: #111827; -fx-border-color: #2a3142;" +
                "-fx-border-radius: 8; -fx-background-radius: 8;");
        return c;
    }

    private VBox fieldBlock(String label, javafx.scene.Node field) {
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8; -fx-padding: 0 0 4 0;");
        VBox block = new VBox(6, lbl, field);
        VBox.setVgrow(field, Priority.ALWAYS);
        return block;
    }

    private HBox rowBlock(VBox left, VBox right) {
        HBox row = new HBox(14, left, right);
        HBox.setHgrow(left, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);
        return row;
    }

    // ── Alert ─────────────────────────────────────────────────────────────────

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
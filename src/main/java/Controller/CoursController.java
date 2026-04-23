package Controller;

import Models.Cours;
import Services.Iservice;
import Services.ServiceCours;
import utils.EmailService;
import utils.OllamaService;
import utils.PDFExporter;
import utils.QRCodeGenerator;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
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
import java.util.concurrent.CompletableFuture;
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

    // ── Stats labels ───────────────────────────────────────────────────────────
    @FXML private Label       lblTotalCours;
    @FXML private Label       lblActiveCountBadge;
    @FXML private Label       lblTotalHours;

    // ── Level bars ─────────────────────────────────────────────────────────────
    @FXML private Label       lblDebutant;
    @FXML private Label       lblPctDebutant;
    @FXML private ProgressBar progressDebutant;
    @FXML private Label       lblIntermediaire;
    @FXML private Label       lblPctIntermediaire;
    @FXML private ProgressBar progressIntermediaire;
    @FXML private Label       lblAvance;
    @FXML private Label       lblPctAvance;
    @FXML private ProgressBar progressAvance;

    // ── Status cards ───────────────────────────────────────────────────────────
    @FXML private Label lblStatActif;
    @FXML private Label lblStatInactif;
    @FXML private Label lblStatEnAttente;
    @FXML private Label lblStatEnCours;

    // ── Category + quick stats ─────────────────────────────────────────────────
    @FXML private VBox categoryStatsContainer;
    @FXML private Label       lblAvgDuration;
    @FXML private Label       lblTopCategory;
    @FXML private ProgressBar progressActive;
    @FXML private Label       lblActivePct;

    // ── Line Chart ─────────────────────────────────────────────────────────────
    @FXML private LineChart<String, Number> statusLineChart;
    @FXML private CategoryAxis              xAxis;
    @FXML private NumberAxis                yAxis;

    // ── Toggle stats ───────────────────────────────────────────────────────────
    @FXML private Button btnToggleStats;
    @FXML private VBox   statsBody;

    // ── Chat panel (floating) ──────────────────────────────────────────────────
    @FXML private VBox      chatPanel;
    @FXML private VBox      chatMessages;
    @FXML private TextField chatInput;
    @FXML private ScrollPane chatScrollPane;

    // ── Services ───────────────────────────────────────────────────────────────
    private final Iservice<Cours> service;
    private final OllamaService   ollamaService;
    private final EmailService    emailService;
    private ObservableList<Cours> allCours;
    private Cours                 editingCours = null;
    private boolean               chatOpen     = false;

    public CoursController() {
        this.service       = new ServiceCours();
        this.ollamaService = new OllamaService();
        this.emailService  = new EmailService();
    }

    // ── Init ───────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        initializeFilters();
        setupFilterListeners();
        styleLineChart();
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

    private void styleLineChart() {
        if (statusLineChart == null) return;
        statusLineChart.setStyle("-fx-background-color: transparent; -fx-plot-background-color: transparent;");
        if (statusLineChart.lookup(".chart-plot-background") != null) {
            statusLineChart.lookup(".chart-plot-background").setStyle("-fx-background-color: transparent;");
        }
    }

    // ── Toggle stats ───────────────────────────────────────────────────────────

    @FXML
    private void handleToggleStats() {
        boolean visible = statsBody.isVisible();
        statsBody.setVisible(!visible);
        statsBody.setManaged(!visible);
        btnToggleStats.setText(visible ? "▶  Stats" : "▼  Stats");
    }

    @FXML
    private void handleResetFilters() {
        txtSearch.clear();
        comboFilterCategorie.setValue("All Categories");
        comboFilterNiveau.setValue("All Levels");
        comboFilterStatus.setValue("All Statuses");
    }

    // ── Filters ────────────────────────────────────────────────────────────────

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

    // ── Data ───────────────────────────────────────────────────────────────────

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

    // ── Stats ──────────────────────────────────────────────────────────────────

    private void updateStats(List<Cours> list) {
        int total = list.size();
        safeText(lblTotalCours, String.valueOf(total));
        long actifCount = list.stream().filter(c -> "actif".equalsIgnoreCase(c.getStatus())).count();
        safeText(lblActiveCountBadge, "↗ " + actifCount + " actif");
        int totalHours = list.stream().mapToInt(Cours::getDureeEstimee).sum();
        safeText(lblTotalHours, totalHours + "h");

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

        safeText(lblStatActif,     String.valueOf(list.stream().filter(c -> "actif".equalsIgnoreCase(c.getStatus())).count()));
        safeText(lblStatInactif,   String.valueOf(list.stream().filter(c -> "inactif".equalsIgnoreCase(c.getStatus())).count()));
        safeText(lblStatEnAttente, String.valueOf(list.stream().filter(c -> "en attente".equalsIgnoreCase(c.getStatus())).count()));
        safeText(lblStatEnCours,   String.valueOf(list.stream().filter(c -> "en cours".equalsIgnoreCase(c.getStatus())).count()));

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
                HBox labelRow = new HBox();
                labelRow.setAlignment(Pos.CENTER_LEFT);
                Label catName = new Label(entry.getKey());
                catName.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                Label catCount = new Label(entry.getValue() + " (" + pct(pct) + "%)");
                catCount.setStyle("-fx-font-size: 11px; -fx-text-fill: " + color + "; -fx-font-weight: bold;");
                labelRow.getChildren().addAll(catName, spacer, catCount);
                ProgressBar bar = new ProgressBar(pct);
                bar.setMaxWidth(Double.MAX_VALUE);
                bar.setPrefHeight(6);
                bar.setStyle("-fx-accent: " + color + "; -fx-background-color: #1e2538; -fx-background-radius: 3;");
                categoryStatsContainer.getChildren().add(new VBox(4, labelRow, bar));
            }
        }

        double avgDuration = total > 0 ? list.stream().mapToInt(Cours::getDureeEstimee).average().orElse(0) : 0;
        safeText(lblAvgDuration, String.format("%.0fh", avgDuration));
        String topCat = list.stream()
                .collect(Collectors.groupingBy(Cours::getCategorie, Collectors.counting()))
                .entrySet().stream().max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse("—");
        safeText(lblTopCategory, topCat);
        double activePct = total > 0 ? (double) actifCount / total : 0;
        safeProgress(progressActive, activePct);
        safeText(lblActivePct, pct(activePct) + "% active courses");

        updateLineChart(list);
    }

    private void updateLineChart(List<Cours> list) {
        if (statusLineChart == null) return;
        statusLineChart.getData().clear();

        String[] statuses = {"actif", "inactif", "en attente", "en cours"};
        String[] labels   = {"Active", "Inactive", "Pending", "In Progress"};

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Courses");
        for (int i = 0; i < statuses.length; i++) {
            final String s = statuses[i];
            long count = list.stream().filter(c -> s.equalsIgnoreCase(c.getStatus())).count();
            series.getData().add(new XYChart.Data<>(labels[i], count));
        }
        statusLineChart.getData().add(series);

        statusLineChart.applyCss();
        statusLineChart.layout();

        if (series.getNode() != null) {
            series.getNode().setStyle("-fx-stroke: #00d4ff; -fx-stroke-width: 2.5px;");
        }
        for (XYChart.Data<String, Number> data : series.getData()) {
            if (data.getNode() != null) {
                data.getNode().setStyle(
                        "-fx-background-color: #00d4ff, #111827;" +
                                "-fx-background-radius: 6px; -fx-padding: 6px;");
            }
        }
        xAxis.setStyle("-fx-tick-label-fill: #64748b; -fx-font-size: 12px;");
        yAxis.setStyle("-fx-tick-label-fill: #64748b; -fx-font-size: 11px;");
        yAxis.setTickUnit(1);
        yAxis.setMinorTickVisible(false);
        statusLineChart.lookupAll(".chart-plot-background").forEach(n ->
                n.setStyle("-fx-background-color: transparent;"));
        statusLineChart.lookupAll(".chart-horizontal-grid-lines").forEach(n ->
                n.setStyle("-fx-stroke: #1e2538; -fx-stroke-dash-array: 4 4;"));
        statusLineChart.lookupAll(".chart-vertical-grid-lines").forEach(n ->
                n.setStyle("-fx-stroke: transparent;"));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CHAT PANEL
    // ══════════════════════════════════════════════════════════════════════════

    /** Toggle chat panel open/close */
    @FXML
    private void handleToggleChat() {
        chatOpen = !chatOpen;
        chatPanel.setVisible(chatOpen);
        chatPanel.setManaged(chatOpen);
        if (chatOpen && chatMessages.getChildren().isEmpty()) {
            addBotMessage("👋 Bonjour ! Je suis votre assistant d'étude AIVA. Comment puis-je vous aider ?");
        }
    }

    /** Send a chat message */
    @FXML
    private void handleSendChat() {
        String msg = chatInput.getText().trim();
        if (msg.isBlank()) return;
        chatInput.clear();

        // Show user message
        addUserMessage(msg);

        // Show typing indicator
        Label typing = new Label("⏳ En train de répondre...");
        typing.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px; -fx-padding: 4 0;");
        chatMessages.getChildren().add(typing);
        scrollChatToBottom();

        // Call Ollama in background thread
        CompletableFuture.supplyAsync(() -> ollamaService.chat(msg, null))
                .thenAcceptAsync(response -> {
                    chatMessages.getChildren().remove(typing);
                    addBotMessage(response);
                    scrollChatToBottom();
                }, Platform::runLater);
    }

    /** Handle Enter key in chat input */
    @FXML
    private void handleChatKeyPress(javafx.scene.input.KeyEvent event) {
        if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
            handleSendChat();
        }
    }

    /** Clear chat history */
    @FXML
    private void handleClearChat() {
        chatMessages.getChildren().clear();
        ollamaService.clearHistory();
        addBotMessage("💬 Nouvelle conversation démarrée. Comment puis-je vous aider ?");
    }

    private void addUserMessage(String text) {
        Label lbl = new Label(text);
        lbl.setWrapText(true);
        lbl.setMaxWidth(260);
        lbl.setStyle(
                "-fx-background-color: #8b5cf6; -fx-text-fill: #ffffff;" +
                        "-fx-background-radius: 12 12 2 12; -fx-padding: 10 14;" +
                        "-fx-font-size: 13px;");
        HBox row = new HBox(lbl);
        row.setAlignment(Pos.CENTER_RIGHT);
        row.setPadding(new Insets(4, 8, 4, 40));
        chatMessages.getChildren().add(row);
        scrollChatToBottom();
    }

    private void addBotMessage(String text) {
        Label lbl = new Label(text);
        lbl.setWrapText(true);
        lbl.setMaxWidth(260);
        lbl.setStyle(
                "-fx-background-color: #1e2538; -fx-text-fill: #e0e0e0;" +
                        "-fx-background-radius: 12 12 12 2; -fx-padding: 10 14;" +
                        "-fx-font-size: 13px;");
        HBox row = new HBox(lbl);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(4, 40, 4, 8));
        chatMessages.getChildren().add(row);
        scrollChatToBottom();
    }

    private void scrollChatToBottom() {
        if (chatScrollPane != null) {
            chatScrollPane.setVvalue(1.0);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ROADMAP
    // ══════════════════════════════════════════════════════════════════════════

    private void handleGenerateRoadmap(Cours cours) {
        // Show loading dialog
        Stage loadingStage = new Stage();
        loadingStage.initModality(Modality.APPLICATION_MODAL);
        loadingStage.initStyle(StageStyle.TRANSPARENT);

        Label loadingLabel = new Label("🤖 Génération du roadmap en cours...");
        loadingLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 14px; -fx-font-weight: bold;");
        ProgressIndicator indicator = new ProgressIndicator();
        indicator.setStyle("-fx-progress-color: #8b5cf6;");
        indicator.setPrefSize(48, 48);

        VBox loadingBox = new VBox(16, indicator, loadingLabel);
        loadingBox.setAlignment(Pos.CENTER);
        loadingBox.setPadding(new Insets(32));
        loadingBox.setStyle(
                "-fx-background-color: #161b2e; -fx-background-radius: 14;" +
                        "-fx-border-color: #2a3142; -fx-border-radius: 14; -fx-border-width: 1;");

        javafx.geometry.Rectangle2D screen = javafx.stage.Screen.getPrimary().getVisualBounds();
        StackPane overlay = new StackPane(loadingBox);
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.65);");
        Scene loadingScene = new Scene(overlay, screen.getWidth(), screen.getHeight());
        loadingScene.setFill(Color.TRANSPARENT);
        loadingStage.setScene(loadingScene);
        loadingStage.show();

        // Generate roadmap in background
        CompletableFuture.supplyAsync(() ->
                        ollamaService.generateRoadmap(cours.getNiveau(), cours.getTittre()))
                .thenAcceptAsync(roadmap -> {
                    loadingStage.close();
                    showRoadmapDialog(cours, roadmap);
                }, Platform::runLater);
    }

    private void showRoadmapDialog(Cours cours, String roadmap) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.TRANSPARENT);

        // Header
        Label title = new Label("🗺 Roadmap — " + cours.getTittre());
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");
        title.setWrapText(true);

        Label levelBadge = new Label(cours.getNiveau().toUpperCase());
        levelBadge.setStyle(
                "-fx-background-color: rgba(139,92,246,0.2); -fx-text-fill: #8b5cf6;" +
                        "-fx-background-radius: 6; -fx-padding: 4 10; -fx-font-size: 11px; -fx-font-weight: bold;");

        HBox headerRow = new HBox(12, title, levelBadge);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Button btnX = new Button("✕");
        btnX.setStyle("-fx-background-color: transparent; -fx-text-fill: #64748b; -fx-font-size: 16px; -fx-cursor: hand;");
        btnX.setOnAction(e -> dialog.close());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox topRow = new HBox(headerRow, spacer, btnX);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #2a3142;");

        // Roadmap content
        TextArea roadmapArea = new TextArea(roadmap);
        roadmapArea.setEditable(false);
        roadmapArea.setWrapText(true);
        roadmapArea.setPrefRowCount(14);
        roadmapArea.setStyle(
                "-fx-background-color: #111827; -fx-text-fill: #c0c8e8;" +
                        "-fx-background-radius: 10; -fx-border-color: #2a3142;" +
                        "-fx-border-radius: 10; -fx-border-width: 1; -fx-font-size: 13px;");

        // Email status label
        Label emailStatus = new Label("");
        emailStatus.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

        // Buttons
        Button btnEmail = new Button("📧 Envoyer par email");
        btnEmail.setStyle(
                "-fx-background-color: #10b981; -fx-text-fill: #ffffff;" +
                        "-fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10 20; -fx-cursor: hand;");
        btnEmail.setOnAction(e -> {
            btnEmail.setText("⏳ Envoi...");
            btnEmail.setDisable(true);
            String recipient = EmailService.TEST_RECIPIENT;
            CompletableFuture.supplyAsync(() ->
                            emailService.sendRoadmap(recipient, cours.getTittre(), cours.getNiveau(), roadmap))
                    .thenAcceptAsync(success -> {
                        if (success) {
                            emailStatus.setText("✅ Roadmap envoyé à " + recipient);
                            emailStatus.setStyle("-fx-font-size: 12px; -fx-text-fill: #10b981;");
                        } else {
                            emailStatus.setText("❌ Échec de l'envoi. Vérifie la config email.");
                            emailStatus.setStyle("-fx-font-size: 12px; -fx-text-fill: #ef4444;");
                        }
                        btnEmail.setText("📧 Envoyer par email");
                        btnEmail.setDisable(false);
                    }, Platform::runLater);
        });

        Button btnClose = new Button("Fermer");
        btnClose.setStyle(
                "-fx-background-color: #1e2538; -fx-text-fill: #ffffff;" +
                        "-fx-background-radius: 8; -fx-padding: 10 20; -fx-cursor: hand;");
        btnClose.setOnAction(e -> dialog.close());

        HBox buttons = new HBox(12, btnEmail, btnClose);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(20, topRow, sep, roadmapArea, emailStatus, buttons);
        root.setPadding(new Insets(28));
        root.setStyle(
                "-fx-background-color: #161b2e;" +
                        "-fx-background-radius: 16; -fx-border-radius: 16;" +
                        "-fx-border-width: 1; -fx-border-color: #2a3142;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 30, 0, 0, 8);");
        root.setPrefWidth(580);

        javafx.geometry.Rectangle2D screen = javafx.stage.Screen.getPrimary().getVisualBounds();
        StackPane overlay = new StackPane(root);
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.65);");

        Scene scene = new Scene(overlay, screen.getWidth(), screen.getHeight());
        scene.setFill(Color.TRANSPARENT);
        dialog.setScene(scene);
        dialog.show();
    }

    // ── Cards ──────────────────────────────────────────────────────────────────

    private void renderCards(List<Cours> list) {
        courseGrid.getChildren().clear();
        boolean isEmpty = list == null || list.isEmpty();
        emptyState.setVisible(isEmpty);
        emptyState.setManaged(isEmpty);
        if (isEmpty) return;
        for (Cours cours : list) courseGrid.getChildren().add(buildCard(cours));
    }

    private VBox buildCard(Cours cours) {
        VBox card = new VBox(12);
        card.setPrefWidth(380);
        card.setMaxWidth(380);
        card.setPadding(new Insets(20));
        card.setStyle(
                "-fx-background-color: #161b2e;" +
                        "-fx-background-radius: 14; -fx-border-radius: 14;" +
                        "-fx-border-width: 1; -fx-border-color: #2a3142;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 16, 0, 0, 4);" +
                        "-fx-cursor: hand;");

        // Title + status badge
        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label titleLabel = new Label(cours.getTittre());
        titleLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(260);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);
        topRow.getChildren().addAll(titleLabel, buildStatusBadge(cours.getStatus()));

        // Meta chips
        HBox metaRow = new HBox(14);
        metaRow.setAlignment(Pos.CENTER_LEFT);
        metaRow.getChildren().addAll(
                metaChip("🏷 " + cours.getCategorie()),
                metaChip("📶 " + cours.getNiveau()),
                metaChip("⏱ " + cours.getDureeEstimee() + "h"));

        // Description
        Label descLabel = new Label(cours.getDescription() == null || cours.getDescription().isBlank()
                ? "No description provided." : cours.getDescription());
        descLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        descLabel.setWrapText(true);
        descLabel.setMaxHeight(40);

        // Action buttons — including Roadmap
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button btnRoadmap = iconButton("🗺", "#f59e0b", "Generate Roadmap");
        Button btnQR      = iconButton("📱", "#00d4ff", "Show QR Code");
        Button btnPDF     = iconButton("📄", "#10b981", "Export PDF");
        Button btnEdit    = iconButton("✏️", "#8b5cf6", "Edit");
        Button btnDelete  = iconButton("🗑", "#ef4444", "Delete");

        btnRoadmap.setOnAction(e -> handleGenerateRoadmap(cours));
        btnQR.setOnAction(e -> handleShowQRCode(cours));
        btnPDF.setOnAction(e -> handleExportPDF(cours));
        btnEdit.setOnAction(e -> handleEdit(cours));
        btnDelete.setOnAction(e -> handleDelete(cours));

        actions.getChildren().addAll(btnRoadmap, btnQR, btnPDF, btnEdit, btnDelete);
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
                        "-fx-background-radius: 8; -fx-font-size: 14px;" +
                        "-fx-padding: 8 12; -fx-cursor: hand;");
        return btn;
    }

    // ── CRUD ───────────────────────────────────────────────────────────────────

    @FXML private void handleAdd() { editingCours = null; openDialog(null); }

    private void handleEdit(Cours cours) { editingCours = cours; openDialog(cours); }

    private void handleDelete(Cours cours) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete \"" + cours.getTittre() + "\"?", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try { service.supprimer(cours); loadData(); }
                catch (SQLDataException e) { showAlert("Error", "Could not delete: " + e.getMessage()); }
            }
        });
    }

    // ── QR Code ────────────────────────────────────────────────────────────────

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
            imageView.setFitWidth(250); imageView.setFitHeight(250); imageView.setPreserveRatio(true);
            Label courseLabel = new Label(cours.getTittre());
            courseLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");
            Button btnDownload = new Button("💾 Save QR");
            btnDownload.setStyle("-fx-background-color: #8b5cf6; -fx-text-fill: #ffffff; -fx-background-radius: 8; -fx-padding: 8 16;");
            btnDownload.setOnAction(e -> saveQRCodeToFile(qrImage, cours.getTittre()));
            Button btnClose = new Button("Close");
            btnClose.setStyle("-fx-background-color: #1e2538; -fx-text-fill: #ffffff; -fx-background-radius: 8; -fx-padding: 8 16;");
            btnClose.setOnAction(e -> dialog.close());
            VBox root = new VBox(15, courseLabel, imageView,
                    new Label("Scan to view course details") {{ setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8;"); }},
                    new HBox(10, btnDownload, btnClose) {{ setAlignment(Pos.CENTER); }});
            root.setAlignment(Pos.CENTER); root.setPadding(new Insets(30));
            root.setStyle("-fx-background-color: #161b2e; -fx-border-color: #2a3142; -fx-border-radius: 12; -fx-background-radius: 12;");
            StackPane overlay = new StackPane(root);
            overlay.setStyle("-fx-background-color: rgba(0,0,0,0.7);");
            Scene scene = new Scene(overlay, 400, 450);
            scene.setFill(Color.TRANSPARENT);
            dialog.setScene(scene); dialog.show();
        } catch (Exception e) { showAlert("Error", "Could not generate QR code: " + e.getMessage()); }
    }

    private void saveQRCodeToFile(BufferedImage qrImage, String courseTitle) {
        FileChooser fc = new FileChooser();
        fc.setInitialFileName("QR_" + courseTitle.replaceAll("[^a-zA-Z0-9]", "_") + ".png");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Image", "*.png"));
        File file = fc.showSaveDialog(courseGrid.getScene().getWindow());
        if (file != null) {
            try { ImageIO.write(qrImage, "PNG", file); showAlert("Success", "QR Code saved!"); }
            catch (IOException e) { showAlert("Error", "Could not save QR code: " + e.getMessage()); }
        }
    }

    // ── PDF ────────────────────────────────────────────────────────────────────

    private void handleExportPDF(Cours cours) {
        FileChooser fc = new FileChooser();
        fc.setInitialFileName(cours.getTittre().replaceAll("[^a-zA-Z0-9]", "_") + ".pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Document", "*.pdf"));
        File file = fc.showSaveDialog(courseGrid.getScene().getWindow());
        if (file != null) {
            try { PDFExporter.exportCourseToPDF(cours, file); showAlert("Success", "PDF exported!"); }
            catch (IOException e) { showAlert("Error", "Could not export PDF: " + e.getMessage()); }
        }
    }

    @FXML
    private void handleExportAllPDF() {
        if (allCours == null || allCours.isEmpty()) { showAlert("Info", "No courses to export."); return; }
        FileChooser fc = new FileChooser();
        fc.setInitialFileName("AIVA_Courses_Catalog.pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Document", "*.pdf"));
        File file = fc.showSaveDialog(courseGrid.getScene().getWindow());
        if (file != null) {
            try { PDFExporter.exportCoursesListToPDF(allCours, file); showAlert("Success", "Catalog exported!"); }
            catch (IOException e) { showAlert("Error", "Could not export: " + e.getMessage()); }
        }
    }

    // ── Dialog ─────────────────────────────────────────────────────────────────

    private void openDialog(Cours cours) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UNDECORATED);

        TextField        fTitre       = styledField("Course title");
        TextField        fDuree       = styledField("Duration in hours");
        TextArea         fDescription = styledArea("Course description");
        ComboBox<String> fNiveau      = styledCombo("Select level", "debutant", "intermediaire", "avance");
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

        VBox form = new VBox(14,
                fieldBlock("Course Title", fTitre),
                rowBlock(fieldBlock("Level", fNiveau), fieldBlock("Category", fCategorie)),
                fieldBlock("Duration (hours)", fDuree),
                fieldBlock("Description", fDescription),
                errorLabel);

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
            try { duree = Integer.parseInt(fDuree.getText().trim()); if (duree <= 0) throw new NumberFormatException(); }
            catch (NumberFormatException ex) { errorLabel.setText("Duration must be a positive number."); return; }
            try {
                if (editingCours == null) {
                    Cours c = new Cours();
                    c.setTittre(fTitre.getText().trim()); c.setDescription(fDescription.getText().trim());
                    c.setNiveau(fNiveau.getValue()); c.setCategorie(fCategorie.getValue());
                    c.setDureeEstimee(duree); c.setStatus("actif");
                    c.setDateCreation(java.time.LocalDate.now().toString()); c.setUser_id(1);
                    service.ajouter(c);
                } else {
                    editingCours.setTittre(fTitre.getText().trim()); editingCours.setDescription(fDescription.getText().trim());
                    editingCours.setNiveau(fNiveau.getValue()); editingCours.setCategorie(fCategorie.getValue());
                    editingCours.setDureeEstimee(duree);
                    service.modifier(editingCours);
                }
                dialog.close(); loadData();
            } catch (SQLDataException ex) { errorLabel.setText("Database error: " + ex.getMessage()); }
        });

        Label dlgTitle = new Label(cours == null ? "Add New Course" : "Edit Course");
        dlgTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");
        Button btnX = new Button("✕");
        btnX.setStyle("-fx-background-color: transparent; -fx-text-fill: #64748b; -fx-font-size: 16px; -fx-cursor: hand;");
        btnX.setOnAction(e -> dialog.close());
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(dlgTitle, spacer, btnX); header.setAlignment(Pos.CENTER_LEFT);
        Separator sep = new Separator(); sep.setStyle("-fx-background-color: #2a3142;");

        VBox root = new VBox(20, header, sep, form, new HBox(12, btnCancel, btnSave) {{ setAlignment(Pos.CENTER_RIGHT); }});
        root.setPadding(new Insets(28));
        root.setStyle("-fx-background-color: #161b2e; -fx-background-radius: 16; -fx-border-radius: 16;" +
                "-fx-border-width: 1; -fx-border-color: #2a3142; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 30, 0, 0, 8);");
        root.setPrefWidth(520);

        StackPane overlay = new StackPane(root);
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.65);"); overlay.setPadding(new Insets(40));
        Scene scene = new Scene(overlay, 620, 560); scene.setFill(Color.TRANSPARENT);
        dialog.setScene(scene); dialog.show();
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private TextField styledField(String prompt) {
        TextField f = new TextField(); f.setPromptText(prompt);
        f.setStyle("-fx-background-color: #111827; -fx-text-fill: #ffffff; -fx-prompt-text-fill: #64748b;" +
                "-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-width: 1;" +
                "-fx-border-color: #2a3142; -fx-padding: 10 14; -fx-font-size: 13px;");
        return f;
    }

    private TextArea styledArea(String prompt) {
        TextArea a = new TextArea(); a.setPromptText(prompt); a.setPrefRowCount(3); a.setWrapText(true);
        a.setStyle("-fx-background-color: #111827; -fx-text-fill: #ffffff; -fx-prompt-text-fill: #64748b;" +
                "-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-width: 1;" +
                "-fx-border-color: #2a3142; -fx-padding: 10 14; -fx-font-size: 13px;");
        return a;
    }

    private ComboBox<String> styledCombo(String prompt, String... items) {
        ComboBox<String> c = new ComboBox<>(); c.setPromptText(prompt); c.getItems().addAll(items);
        c.setMaxWidth(Double.MAX_VALUE);
        c.setStyle("-fx-background-color: #111827; -fx-border-color: #2a3142; -fx-border-radius: 8; -fx-background-radius: 8;");
        return c;
    }

    private VBox fieldBlock(String label, javafx.scene.Node field) {
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8; -fx-padding: 0 0 4 0;");
        VBox block = new VBox(6, lbl, field); VBox.setVgrow(field, Priority.ALWAYS);
        return block;
    }

    private HBox rowBlock(VBox left, VBox right) {
        HBox row = new HBox(14, left, right);
        HBox.setHgrow(left, Priority.ALWAYS); HBox.setHgrow(right, Priority.ALWAYS);
        return row;
    }

    private void safeText(Label lbl, String text)         { if (lbl != null) lbl.setText(text); }
    private void safeProgress(ProgressBar bar, double v)  { if (bar != null) bar.setProgress(v); }
    private int  pct(double ratio)                        { return (int) Math.round(ratio * 100); }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(message);
        alert.showAndWait();
    }
}
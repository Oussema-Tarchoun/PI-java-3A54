package controllers;

import Models.Objectif;
import Services.ServiceObjectif;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.awt.image.BufferedImage;
import java.sql.SQLDataException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class ObjectifFrontController {

    // ── Stat labels ────────────────────────────────────────
    @FXML private Label statTotalLabel;
    @FXML private Label statEnCoursLabel;
    @FXML private Label statTerminesLabel;
    @FXML private Label statProgressionLabel;

    // ── Analytic labels ────────────────────────────────────
    @FXML private Label statDureeMoyLabel;
    @FXML private Label statTauxLabel;
    @FXML private Label statValeurMoyLabel;

    // ── Card grid ──────────────────────────────────────────
    @FXML private FlowPane cardGrid;
    @FXML private Label    emptyLabel;

    // ── Search and Filter UI (à ajouter dans le FXML) ──────────────
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterStatusCombo;
    @FXML private ComboBox<String> sortCombo;

    // ── Form ───────────────────────────────────────────────
    @FXML private VBox       formPane;
    @FXML private HBox       btnAjouterBox;
    @FXML private HBox       btnModifierBox;
    @FXML private TextField  descField;
    @FXML private TextField  typeField;
    @FXML private TextField  valeurCibleField;
    @FXML private TextField  statutField;
    @FXML private DatePicker dateDebutPicker;
    @FXML private DatePicker dateFinPicker;
    @FXML private Label      msgLabel;

    // ── Charts ─────────────────────────────────────────────
    @FXML private LineChart<String, Number> progressionLineChart;
    @FXML private LineChart<String, Number> valeurCibleLineChart;
    @FXML private LineChart<String, Number> dureeLineChart;
    @FXML private LineChart<String, Number> parMoisLineChart;

    private final ServiceObjectif service = new ServiceObjectif();
    private static final int DEFAULT_USER_ID = 1;
    private Objectif currentEdit = null;
    private List<Objectif> allObjectifs = new ArrayList<>();

    // ── Filtered/Sorted data ─────────────────────────────────
    private FilteredList<Objectif> filteredObjectifs;
    private SortedList<Objectif> sortedObjectifs;

    // ── Filter options ───────────────────────────────────────
    private final String[] STATUS_FILTERS = {"Tous", "En cours", "Terminé"};
    private final String[] SORT_OPTIONS = {"Date de début (plus récent)", "Date de début (plus ancien)",
            "Date de fin (plus proche)", "Date de fin (plus lointain)",
            "Valeur cible (croissant)", "Valeur cible (décroissant)",
            "Progression (décroissante)", "Description (A-Z)"};

    private boolean firstLoad = true;

    // ═══════════════════════════════════════════════════════
    // ║  LIFECYCLE                                           ║
    // ═══════════════════════════════════════════════════════

    @FXML
    public void initialize() {
        initSearchAndFilter();
        chargerAvecNotifications();
    }

    // ═══════════════════════════════════════════════════════
    // ║  SEARCH & FILTER INITIALIZATION                      ║
    // ═══════════════════════════════════════════════════════

    private void initSearchAndFilter() {
        // Initialiser les ComboBox
        if (filterStatusCombo != null) {
            filterStatusCombo.getItems().setAll(STATUS_FILTERS);
            filterStatusCombo.setValue("Tous");
            filterStatusCombo.setOnAction(e -> applyFilterAndSort());
        }

        if (sortCombo != null) {
            sortCombo.getItems().setAll(SORT_OPTIONS);
            sortCombo.setValue("Date de début (plus récent)");
            sortCombo.setOnAction(e -> applyFilterAndSort());
        }

        if (searchField != null) {
            searchField.textProperty().addListener((obs, old, val) -> applyFilterAndSort());
        }
    }

    private void applyFilterAndSort() {
        if (filteredObjectifs == null || allObjectifs.isEmpty()) return;

        // Mettre à jour le filtre
        filteredObjectifs.setPredicate(objectif -> {
            // Filtre par texte de recherche
            String searchText = searchField != null ? searchField.getText().toLowerCase().trim() : "";
            if (!searchText.isEmpty()) {
                boolean matchDesc = objectif.getDescription() != null &&
                        objectif.getDescription().toLowerCase().contains(searchText);
                boolean matchType = objectif.getType() != null &&
                        objectif.getType().toLowerCase().contains(searchText);
                if (!matchDesc && !matchType) return false;
            }

            // Filtre par statut
            String filterStatus = filterStatusCombo != null ? filterStatusCombo.getValue() : "Tous";
            if (!filterStatus.equals("Tous")) {
                if (filterStatus.equals("En cours") && !isEnCours(objectif.getStatut())) return false;
                if (filterStatus.equals("Terminé") && !isTermine(objectif.getStatut())) return false;
            }

            return true;
        });

        // Mettre à jour l'affichage
        buildCardsFromSortedList();
        updateStatsFromFiltered();
    }

    private void buildCardsFromSortedList() {
        if (sortedObjectifs == null) return;

        cardGrid.getChildren().clear();

        if (sortedObjectifs.isEmpty()) {
            if (emptyLabel != null) {
                emptyLabel.setVisible(true);
                emptyLabel.setManaged(true);
                emptyLabel.setText("Aucun objectif ne correspond aux critères.");
            }
            return;
        }

        if (emptyLabel != null) {
            emptyLabel.setVisible(false);
            emptyLabel.setManaged(false);
        }

        for (Objectif o : sortedObjectifs) {
            cardGrid.getChildren().add(buildCard(o));
        }
    }

    private void updateStatsFromFiltered() {
        if (sortedObjectifs == null) return;

        statTotalLabel.setText(String.valueOf(sortedObjectifs.size()));
        long enCours = sortedObjectifs.stream().filter(o -> isEnCours(o.getStatut())).count();
        long termines = sortedObjectifs.stream().filter(o -> isTermine(o.getStatut())).count();
        statEnCoursLabel.setText(String.valueOf(enCours));
        statTerminesLabel.setText(String.valueOf(termines));

        OptionalDouble avgProg = sortedObjectifs.stream()
                .mapToDouble(this::computeProgress)
                .average();
        statProgressionLabel.setText(avgProg.isPresent()
                ? String.format("%.0f%%", avgProg.getAsDouble() * 100)
                : "0%");
    }

    // ═══════════════════════════════════════════════════════
    // ║  NOTIFICATIONS / RAPPELS VISUELS                     ║
    // ═══════════════════════════════════════════════════════

    private void verifierNotifications() {
        List<Objectif> urgents = getObjectifsUrgents(3);
        if (!urgents.isEmpty()) {
            afficherPopupNotifications(urgents);
        }
    }

    private List<Objectif> getObjectifsUrgents(int jours) {
        LocalDate now = LocalDate.now();
        LocalDate limite = now.plusDays(jours);

        return allObjectifs.stream()
                .filter(o -> isEnCours(o.getStatut()))
                .filter(o -> o.getDateFin() != null)
                .filter(o -> !o.getDateFin().isBefore(now))
                .filter(o -> !o.getDateFin().isAfter(limite))
                .sorted(Comparator.comparing(Objectif::getDateFin))
                .collect(Collectors.toList());
    }

    @FXML
    public void ouvrirNotifications() {
        List<Objectif> urgents = getObjectifsUrgents(3);
        if (urgents.isEmpty()) {
            afficherPopupAucuneNotification();
        } else {
            afficherPopupNotifications(urgents);
        }
    }

    private void afficherPopupNotifications(List<Objectif> urgents) {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.setTitle("🔔 Notifications");
        popup.setResizable(false);

        VBox root = new VBox(16);
        root.setStyle("-fx-background-color:#0a0e1a; -fx-padding:28px;");
        root.setPrefWidth(520);

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label("🔔");
        icon.setStyle("-fx-font-size:28px;");

        VBox titleBox = new VBox(2);
        Label titre = new Label("Notifications");
        titre.setStyle("-fx-font-size:20px; -fx-font-weight:bold; -fx-text-fill:#f59e0b;");
        Label sousTitre = new Label(urgents.size() + " rappel(s) d'objectif(s)");
        sousTitre.setStyle("-fx-font-size:13px; -fx-text-fill:#94a3b8;");
        titleBox.getChildren().addAll(titre, sousTitre);

        header.getChildren().addAll(icon, titleBox);

        VBox liste = new VBox(10);
        for (Objectif o : urgents) {
            long joursRestants = ChronoUnit.DAYS.between(LocalDate.now(), o.getDateFin());

            VBox notifItem = new VBox(8);
            notifItem.setStyle(
                    "-fx-background-color:#161b2e; -fx-background-radius:12px; -fx-padding:16px;" +
                            "-fx-border-color:#f59e0b40; -fx-border-radius:12px; -fx-border-width:1px;"
            );

            HBox notifHeader = new HBox(10);
            notifHeader.setAlignment(Pos.CENTER_LEFT);

            Label notifIcon = new Label(joursRestants == 0 ? "🔴" : joursRestants == 1 ? "🟠" : "🟡");
            notifIcon.setStyle("-fx-font-size:20px;");

            VBox notifInfo = new VBox(2);
            HBox.setHgrow(notifInfo, Priority.ALWAYS);

            Label notifTitle = new Label(o.getDescription());
            notifTitle.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#ffffff;");
            notifTitle.setWrapText(true);

            Label notifDetail = new Label(o.getType() + "  •  Fin: " + o.getDateFin());
            notifDetail.setStyle("-fx-font-size:12px; -fx-text-fill:#94a3b8;");

            notifInfo.getChildren().addAll(notifTitle, notifDetail);

            Label badge = new Label(joursRestants == 0 ? "AUJOURD'HUI" :
                    joursRestants == 1 ? "DEMAIN" : joursRestants + " jours");
            badge.setStyle(
                    "-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:#f59e0b;" +
                            "-fx-background-color:#f59e0b20; -fx-background-radius:6px;" +
                            "-fx-padding:4 12 4 12;"
            );

            notifHeader.getChildren().addAll(notifIcon, notifInfo, badge);

            double progress = computeProgress(o);
            ProgressBar bar = new ProgressBar(progress);
            bar.setMaxWidth(Double.MAX_VALUE);
            bar.setStyle("-fx-accent: #f59e0b;");
            bar.setPrefHeight(6);

            HBox progressBox = new HBox(10);
            progressBox.setAlignment(Pos.CENTER_LEFT);
            Label progLabel = new Label(String.format("Progression: %.0f%%", progress * 100));
            progLabel.setStyle("-fx-font-size:11px; -fx-text-fill:#94a3b8;");
            progressBox.getChildren().add(progLabel);

            notifItem.getChildren().addAll(notifHeader, bar, progressBox);
            liste.getChildren().add(notifItem);
        }

        Button btnFermer = new Button("✓  J'ai compris");
        btnFermer.setStyle(
                "-fx-background-color:#f59e0b; -fx-text-fill:#0a0e1a;" +
                        "-fx-font-weight:bold; -fx-background-radius:8px;" +
                        "-fx-padding:12 32 12 32; -fx-cursor:hand; -fx-font-size:14px;"
        );
        btnFermer.setOnAction(e -> popup.close());

        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER);
        footer.getChildren().add(btnFermer);

        root.getChildren().addAll(header, liste, footer);

        Scene scene = new Scene(root);
        popup.setScene(scene);
        popup.showAndWait();
    }

    private void afficherPopupAucuneNotification() {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.setTitle("🔔 Notifications");
        popup.setResizable(false);

        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color:#0a0e1a; -fx-padding:40px;");
        root.setPrefWidth(380);

        Label icon = new Label("✅");
        icon.setStyle("-fx-font-size:48px;");

        Label titre = new Label("Tout est en ordre !");
        titre.setStyle("-fx-font-size:18px; -fx-font-weight:bold; -fx-text-fill:#22c55e;");

        Label msg = new Label("Aucun objectif urgent pour les 3 prochains jours.");
        msg.setStyle("-fx-font-size:13px; -fx-text-fill:#94a3b8;");
        msg.setWrapText(true);

        Button btnFermer = new Button("Fermer");
        btnFermer.setStyle(
                "-fx-background-color:#1e2538; -fx-text-fill:#94a3b8;" +
                        "-fx-background-radius:8px; -fx-padding:10 28 10 28;" +
                        "-fx-border-color:#2a3142; -fx-border-width:1px;" +
                        "-fx-border-radius:8px; -fx-cursor:hand; -fx-font-size:13px;"
        );
        btnFermer.setOnAction(e -> popup.close());

        root.getChildren().addAll(icon, titre, msg, btnFermer);

        Scene scene = new Scene(root);
        popup.setScene(scene);
        popup.showAndWait();
    }

    // ═══════════════════════════════════════════════════════
    // ║  DATA LOADING                                        ║
    // ═══════════════════════════════════════════════════════

    @FXML
    public void charger() {
        try {
            List<Objectif> list = service.recuperer();
            allObjectifs = new ArrayList<>(list);

            // Initialiser les listes filtrées et triées
            ObservableList<Objectif> observableList = FXCollections.observableArrayList(allObjectifs);
            filteredObjectifs = new FilteredList<>(observableList, p -> true);
            sortedObjectifs = new SortedList<>(filteredObjectifs, getComparator("Date de début (plus récent)"));

            updateStats(list);
            buildCards(list);
            updateCharts(list);
        } catch (SQLDataException e) {
            msg("Erreur BD : " + e.getMessage(), true);
        }
    }

    private Comparator<Objectif> getComparator(String sortOption) {
        switch (sortOption) {
            case "Date de début (plus récent)":
                return (a, b) -> {
                    if (a.getDateDebut() == null) return 1;
                    if (b.getDateDebut() == null) return -1;
                    return b.getDateDebut().compareTo(a.getDateDebut());
                };
            case "Date de début (plus ancien)":
                return (a, b) -> {
                    if (a.getDateDebut() == null) return 1;
                    if (b.getDateDebut() == null) return -1;
                    return a.getDateDebut().compareTo(b.getDateDebut());
                };
            case "Date de fin (plus proche)":
                return (a, b) -> {
                    if (a.getDateFin() == null) return 1;
                    if (b.getDateFin() == null) return -1;
                    return a.getDateFin().compareTo(b.getDateFin());
                };
            case "Date de fin (plus lointain)":
                return (a, b) -> {
                    if (a.getDateFin() == null) return 1;
                    if (b.getDateFin() == null) return -1;
                    return b.getDateFin().compareTo(a.getDateFin());
                };
            case "Valeur cible (croissant)":
                return Comparator.comparingInt(Objectif::getValeurCible);
            case "Valeur cible (décroissant)":
                return (a, b) -> Integer.compare(b.getValeurCible(), a.getValeurCible());
            case "Progression (décroissante)":
                return (a, b) -> Double.compare(computeProgress(b), computeProgress(a));
            case "Description (A-Z)":
                return Comparator.comparing(Objectif::getDescription, Comparator.nullsLast(String::compareTo));
            default:
                return (a, b) -> {
                    if (a.getDateDebut() == null) return 1;
                    if (b.getDateDebut() == null) return -1;
                    return b.getDateDebut().compareTo(a.getDateDebut());
                };
        }
    }

    private void chargerAvecNotifications() {
        try {
            List<Objectif> list = service.recuperer();
            allObjectifs = new ArrayList<>(list);

            // Initialiser les listes filtrées et triées
            ObservableList<Objectif> observableList = FXCollections.observableArrayList(allObjectifs);
            filteredObjectifs = new FilteredList<>(observableList, p -> true);
            sortedObjectifs = new SortedList<>(filteredObjectifs, getComparator("Date de début (plus récent)"));

            updateStats(list);
            buildCards(list);
            updateCharts(list);

            if (firstLoad) {
                firstLoad = false;
                Platform.runLater(this::verifierNotifications);
            }
        } catch (SQLDataException e) {
            msg("Erreur BD : " + e.getMessage(), true);
        }
    }

    private void updateStats(List<Objectif> list) {
        statTotalLabel.setText(String.valueOf(list.size()));
        long enCours = list.stream().filter(o -> isEnCours(o.getStatut())).count();
        long termines = list.stream().filter(o -> isTermine(o.getStatut())).count();
        statEnCoursLabel.setText(String.valueOf(enCours));
        statTerminesLabel.setText(String.valueOf(termines));

        OptionalDouble avgProg = list.stream()
                .mapToDouble(this::computeProgress)
                .average();
        statProgressionLabel.setText(avgProg.isPresent()
                ? String.format("%.0f%%", avgProg.getAsDouble() * 100)
                : "0%");
    }

    // ═══════════════════════════════════════════════════════
    // ║  CHARTS                                              ║
    // ═══════════════════════════════════════════════════════

    private void updateCharts(List<Objectif> list) {
        buildProgressionChart(list);
        buildValeurCibleChart(list);
        buildDureeChart(list);
        buildParMoisChart(list);
        buildAnalyticSummary(list);
    }

    private void buildProgressionChart(List<Objectif> list) {
        progressionLineChart.getData().clear();
        progressionLineChart.setLegendVisible(false);
        progressionLineChart.setAnimated(false);
        progressionLineChart.setCreateSymbols(true);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (Objectif o : list) {
            double pct = computeProgress(o) * 100;
            String label = truncate(o.getDescription(), 12);
            series.getData().add(new XYChart.Data<>(label, pct));
        }
        progressionLineChart.getData().add(series);
        styleLineChart(progressionLineChart, "#6366f1");
    }

    private void buildValeurCibleChart(List<Objectif> list) {
        valeurCibleLineChart.getData().clear();
        valeurCibleLineChart.setLegendVisible(false);
        valeurCibleLineChart.setAnimated(false);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (Objectif o : list) {
            series.getData().add(new XYChart.Data<>(truncate(o.getDescription(), 12), o.getValeurCible()));
        }
        valeurCibleLineChart.getData().add(series);
        styleLineChart(valeurCibleLineChart, "#10b981");
    }

    private void buildDureeChart(List<Objectif> list) {
        dureeLineChart.getData().clear();
        dureeLineChart.setLegendVisible(false);
        dureeLineChart.setAnimated(false);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (Objectif o : list) {
            long duree = (o.getDateDebut() != null && o.getDateFin() != null)
                    ? ChronoUnit.DAYS.between(o.getDateDebut(), o.getDateFin()) : 0;
            series.getData().add(new XYChart.Data<>(truncate(o.getDescription(), 12), duree));
        }
        dureeLineChart.getData().add(series);
        styleLineChart(dureeLineChart, "#f59e0b");
    }

    private void buildParMoisChart(List<Objectif> list) {
        parMoisLineChart.getData().clear();
        parMoisLineChart.setLegendVisible(false);
        parMoisLineChart.setAnimated(false);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");
        Map<String, Long> countByMonth = new TreeMap<>();
        for (Objectif o : list) {
            if (o.getDateDebut() != null) {
                String month = o.getDateDebut().format(fmt);
                countByMonth.merge(month, 1L, Long::sum);
            }
        }

        XYChart.Series<String, Number> seriesTotal = new XYChart.Series<>();
        seriesTotal.setName("Total");
        for (Map.Entry<String, Long> entry : countByMonth.entrySet()) {
            seriesTotal.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }
        parMoisLineChart.getData().add(seriesTotal);
        parMoisLineChart.setLegendVisible(true);
        styleLineChart(parMoisLineChart, "#6366f1");
    }

    private void buildAnalyticSummary(List<Objectif> list) {
        OptionalDouble avgDuree = list.stream()
                .filter(o -> o.getDateDebut() != null && o.getDateFin() != null)
                .mapToLong(o -> ChronoUnit.DAYS.between(o.getDateDebut(), o.getDateFin()))
                .average();
        statDureeMoyLabel.setText(avgDuree.isPresent()
                ? String.format("%.1f j", avgDuree.getAsDouble()) : "—");

        if (!list.isEmpty()) {
            long termines = list.stream().filter(o -> isTermine(o.getStatut())).count();
            statTauxLabel.setText(String.format("%.0f%%", (double) termines / list.size() * 100));
        } else {
            statTauxLabel.setText("—");
        }

        OptionalDouble avgVal = list.stream().mapToInt(Objectif::getValeurCible).average();
        statValeurMoyLabel.setText(avgVal.isPresent()
                ? String.format("%.0f", avgVal.getAsDouble()) : "—");
    }

    private void styleLineChart(LineChart<String, Number> chart, String color) {
        Platform.runLater(() -> applySeriesColor(chart, 0, color));
    }

    private void applySeriesColor(LineChart<String, Number> chart, int seriesIndex, String color) {
        if (chart.getData().size() <= seriesIndex) return;
        XYChart.Series<String, Number> s = chart.getData().get(seriesIndex);
        if (s.getNode() != null)
            s.getNode().setStyle("-fx-stroke: " + color + "; -fx-stroke-width: 2.5px;");
        for (XYChart.Data<String, Number> d : s.getData()) {
            if (d.getNode() != null) {
                d.getNode().setStyle(
                        "-fx-background-color: " + color + ", white; " +
                                "-fx-background-insets: 0, 2; " +
                                "-fx-background-radius: 5px; " +
                                "-fx-padding: 5px;"
                );
            }
        }
    }

    // ═══════════════════════════════════════════════════════
    // ║  CARD BUILDER                                        ║
    // ═══════════════════════════════════════════════════════

    private void buildCards(List<Objectif> list) {
        cardGrid.getChildren().clear();
        if (list.isEmpty()) {
            if (emptyLabel != null) {
                emptyLabel.setVisible(true);
                emptyLabel.setManaged(true);
                emptyLabel.setText("Aucun objectif enregistré. Cliquez sur « Nouvel objectif » pour commencer !");
            }
            return;
        }
        if (emptyLabel != null) { emptyLabel.setVisible(false); emptyLabel.setManaged(false); }
        for (Objectif o : list) cardGrid.getChildren().add(buildCard(o));
    }

    private VBox buildCard(Objectif o) {
        VBox card = new VBox(14);
        card.setPrefWidth(300);
        card.setMinWidth(260);
        card.getStyleClass().add("obj-card");

        boolean isUrgent = isObjectifUrgent(o, 3);

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        StackPane icon = new StackPane();
        icon.getStyleClass().addAll("obj-icon", resolveIconColor(o.getStatut()));
        if (isUrgent)
            icon.setStyle("-fx-effect: dropshadow(gaussian, #f59e0b, 10, 0, 0, 0);");
        Label iconLbl = new Label(resolveEmoji(o.getType()));
        iconLbl.setStyle("-fx-font-size:17px;");
        icon.getChildren().add(iconLbl);

        VBox titleBox = new VBox(2);
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        Label titleLbl = new Label(truncate(o.getDescription(), 30));
        titleLbl.getStyleClass().add("obj-title");
        Label typeLbl = new Label(o.getType() != null ? o.getType() : "");
        typeLbl.getStyleClass().add("obj-type");
        titleBox.getChildren().addAll(titleLbl, typeLbl);

        HBox badges = new HBox(6);
        badges.setAlignment(Pos.CENTER_RIGHT);

        if (isUrgent) {
            long joursRestants = ChronoUnit.DAYS.between(LocalDate.now(), o.getDateFin());
            Label urgentBadge = new Label(joursRestants == 0 ? "🔴" : joursRestants == 1 ? "🟠" : "🟡");
            urgentBadge.setStyle("-fx-font-size:14px;");
            badges.getChildren().add(urgentBadge);
        }

        Label statusBadge = new Label(o.getStatut() != null ? o.getStatut() : "—");
        statusBadge.getStyleClass().addAll("obj-badge", resolveBadgeStyle(o.getStatut()));
        badges.getChildren().add(statusBadge);

        header.getChildren().addAll(icon, titleBox, badges);

        VBox progressSection = new VBox(6);
        double progress = computeProgress(o);
        ProgressBar bar = new ProgressBar(progress);
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.getStyleClass().addAll("obj-progress", resolveProgressStyle(o.getStatut()));
        if (isUrgent) bar.setStyle("-fx-accent: #f59e0b;");

        HBox progressMeta = new HBox();
        progressMeta.setAlignment(Pos.CENTER_LEFT);
        Label progPct = new Label(String.format("%.0f%%", progress * 100));
        progPct.getStyleClass().add("obj-progress-pct");
        HBox.setHgrow(progPct, Priority.ALWAYS);

        String fin = o.getDateFin() != null ? "Fin : " + o.getDateFin() : "";
        if (isUrgent) {
            long joursRestants = ChronoUnit.DAYS.between(LocalDate.now(), o.getDateFin());
            fin += "  (" + (joursRestants == 0 ? "AUJOURD'HUI !" : joursRestants + "j restant(s)") + ")";
        }
        Label progDate = new Label(fin);
        progDate.getStyleClass().add("obj-progress-date");
        if (isUrgent) progDate.setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold;");
        progressMeta.getChildren().addAll(progPct, progDate);

        progressSection.getChildren().addAll(bar, progressMeta);

        HBox cibleRow = new HBox(6);
        cibleRow.setAlignment(Pos.CENTER_LEFT);
        cibleRow.getStyleClass().add("obj-cible-row");
        Label cibleLbl = new Label("Valeur cible :");
        cibleLbl.getStyleClass().add("obj-meta-label");
        Label cibleVal = new Label(String.valueOf(o.getValeurCible()));
        cibleVal.getStyleClass().add("obj-meta-value");
        cibleRow.getChildren().addAll(cibleLbl, cibleVal);

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button btnQR = new Button("⬛  QR");
        btnQR.getStyleClass().addAll("act-btn", "act-btn-qr");
        btnQR.setOnAction(e -> afficherQRCode(o));

        Button btnEdit = new Button("✏  Modifier");
        btnEdit.getStyleClass().addAll("act-btn", "act-btn-edit");
        btnEdit.setOnAction(e -> {
            remplirFormulaire(o);
            currentEdit = o;
            setModeEdition(true);
            showForm(true);
            msg("Modifiez les champs puis cliquez Modifier.", false);
        });

        Button btnDel = new Button("✕  Suppr");
        btnDel.getStyleClass().addAll("act-btn", "act-btn-delete");
        btnDel.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Supprimer l'objectif ?", ButtonType.YES, ButtonType.NO);
            confirm.setHeaderText(null);
            confirm.showAndWait().ifPresent(bt -> {
                if (bt == ButtonType.YES) {
                    try { service.supprimer(o); charger(); }
                    catch (SQLDataException ex) { msg("Erreur : " + ex.getMessage(), true); }
                }
            });
        });

        actions.getChildren().addAll(btnQR, btnEdit, btnDel);
        card.getChildren().addAll(header, progressSection, cibleRow, actions);

        if (isUrgent)
            card.setStyle("-fx-border-color: #f59e0b; -fx-border-width: 2px; -fx-border-radius: 14px;");

        return card;
    }

    private boolean isObjectifUrgent(Objectif o, int jours) {
        if (!isEnCours(o.getStatut()) || o.getDateFin() == null) return false;
        LocalDate now = LocalDate.now();
        LocalDate limite = now.plusDays(jours);
        return !o.getDateFin().isBefore(now) && !o.getDateFin().isAfter(limite);
    }

    // ═══════════════════════════════════════════════════════
    // ║  QR CODE                                             ║
    // ═══════════════════════════════════════════════════════

    private void afficherQRCode(Objectif o) {
        String content = String.format(
                "Objectif : %s\nType     : %s\nStatut   : %s\nCible    : %d\nDébut    : %s\nFin      : %s",
                o.getDescription() != null ? o.getDescription() : "—",
                o.getType()        != null ? o.getType()        : "—",
                o.getStatut()      != null ? o.getStatut()      : "—",
                o.getValeurCible(),
                o.getDateDebut()   != null ? o.getDateDebut().toString() : "—",
                o.getDateFin()     != null ? o.getDateFin().toString()   : "—"
        );

        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix    = writer.encode(content, BarcodeFormat.QR_CODE, 300, 300);
            BufferedImage buffered = MatrixToImageWriter.toBufferedImage(matrix);

            ImageView iv = new ImageView(javafx.embed.swing.SwingFXUtils.toFXImage(buffered, null));
            iv.setFitWidth(280);
            iv.setFitHeight(280);

            Label titleLbl = new Label("📱 QR Code");
            titleLbl.setStyle("-fx-font-size:16px; -fx-font-weight:bold; -fx-text-fill:#1e293b;");

            VBox infoBox = new VBox(4);
            infoBox.setStyle("-fx-background-color:#f8fafc; -fx-padding:12; -fx-background-radius:10;");
            infoBox.setMaxWidth(280);
            infoBox.getChildren().addAll(
                    infoRow("Type",   o.getType()),
                    infoRow("Statut", o.getStatut()),
                    infoRow("Cible",  String.valueOf(o.getValeurCible())),
                    infoRow("Début",  o.getDateDebut() != null ? o.getDateDebut().toString() : "—"),
                    infoRow("Fin",    o.getDateFin()   != null ? o.getDateFin().toString()   : "—")
            );

            Button closeBtn = new Button("✕  Fermer");
            closeBtn.setStyle("-fx-background-color:#6366f1; -fx-text-fill:white; -fx-font-size:12px;");
            closeBtn.setOnAction(ev -> ((Stage) closeBtn.getScene().getWindow()).close());

            VBox root = new VBox(14, titleLbl, iv, infoBox, closeBtn);
            root.setAlignment(Pos.CENTER);
            root.setStyle("-fx-background-color:white; -fx-padding:32 36;");

            Stage modal = new Stage();
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.setResizable(false);
            modal.setScene(new Scene(root));
            modal.showAndWait();

        } catch (WriterException ex) {
            msg("Erreur génération QR : " + ex.getMessage(), true);
        }
    }

    private HBox infoRow(String label, String value) {
        Label lbl = new Label(label + " :");
        lbl.setStyle("-fx-font-size:11px; -fx-text-fill:#94a3b8; -fx-min-width:50;");
        Label val = new Label(value != null ? value : "—");
        val.setStyle("-fx-font-size:11px; -fx-text-fill:#1e293b; -fx-font-weight:bold;");
        HBox row = new HBox(8, lbl, val);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    // ═══════════════════════════════════════════════════════
    // ║  HELPERS                                             ║
    // ═══════════════════════════════════════════════════════

    private double computeProgress(Objectif o) {
        if (o.getDateDebut() == null || o.getDateFin() == null) return 0;
        LocalDate now   = LocalDate.now();
        long total  = ChronoUnit.DAYS.between(o.getDateDebut(), o.getDateFin());
        long passed = ChronoUnit.DAYS.between(o.getDateDebut(), now);
        if (total <= 0) return isTermine(o.getStatut()) ? 1.0 : 0.0;
        return Math.max(0, Math.min(1, (double) passed / total));
    }

    private String resolveEmoji(String type) {
        if (type == null) return "🎯";
        String t = type.toLowerCase();
        if (t.contains("poids")  || t.contains("weight"))  return "⚖";
        if (t.contains("endur")  || t.contains("cours"))   return "🏃";
        if (t.contains("muscl")  || t.contains("force"))   return "💪";
        if (t.contains("nutrit") || t.contains("diet"))    return "🥗";
        if (t.contains("sleep")  || t.contains("sommeil")) return "😴";
        return "🎯";
    }

    private String resolveIconColor(String statut) {
        if (isTermine(statut)) return "obj-icon-green";
        if (isEnCours(statut)) return "obj-icon-cyan";
        return "obj-icon-orange";
    }

    private String resolveBadgeStyle(String statut) {
        if (isTermine(statut)) return "obj-badge-done";
        if (isEnCours(statut)) return "obj-badge-active";
        return "obj-badge-pending";
    }

    private String resolveProgressStyle(String statut) {
        if (isTermine(statut)) return "obj-progress-green";
        if (isEnCours(statut)) return "obj-progress-cyan";
        return "obj-progress-orange";
    }

    private boolean isTermine(String s) {
        return s != null && (s.toLowerCase().contains("termin") || s.toLowerCase().contains("done"));
    }

    private boolean isEnCours(String s) {
        return s != null && (s.toLowerCase().contains("cours") || s.toLowerCase().contains("actif"));
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }

    // ═══════════════════════════════════════════════════════
    // ║  FORM HANDLING                                       ║
    // ═══════════════════════════════════════════════════════

    @FXML public void ouvrirFormulaire() {
        currentEdit = null; viderFormulaire(); setModeEdition(false); showForm(true);
    }

    @FXML public void annuler() {
        viderFormulaire(); setModeEdition(false); showForm(false); currentEdit = null;
    }

    private void showForm(boolean v) {
        if (formPane != null) { formPane.setVisible(v); formPane.setManaged(v); }
    }

    private void setModeEdition(boolean e) {
        if (btnAjouterBox  != null) { btnAjouterBox.setVisible(!e);  btnAjouterBox.setManaged(!e); }
        if (btnModifierBox != null) { btnModifierBox.setVisible(e);   btnModifierBox.setManaged(e); }
    }

    // ═══════════════════════════════════════════════════════
    // ║  CRUD                                                ║
    // ═══════════════════════════════════════════════════════

    @FXML
    public void ajouter() {
        if (!valider()) return;
        try {
            Objectif o = buildFromForm(new Objectif());
            if (o.getUserId() <= 0) o.setUserId(DEFAULT_USER_ID);
            service.ajouter(o);
            msg("✓ Objectif ajouté !", false);
            viderFormulaire(); showForm(false); charger();
        } catch (SQLDataException e) { msg("Erreur BD : " + e.getMessage(), true); }
    }

    @FXML
    public void modifier() {
        if (currentEdit == null) { msg("Aucun objectif sélectionné.", true); return; }
        if (!valider()) return;
        try {
            buildFromForm(currentEdit);
            if (currentEdit.getUserId() <= 0) currentEdit.setUserId(DEFAULT_USER_ID);
            service.modifier(currentEdit);
            msg("✓ Objectif modifié !", false);
            viderFormulaire(); setModeEdition(false); showForm(false);
            currentEdit = null; charger();
        } catch (SQLDataException e) { msg("Erreur BD : " + e.getMessage(), true); }
    }

    private Objectif buildFromForm(Objectif o) {
        o.setDescription(descField.getText().trim());
        o.setType(typeField.getText().trim());
        o.setValeurCible(Integer.parseInt(valeurCibleField.getText().trim()));
        o.setDateDebut(dateDebutPicker.getValue());
        o.setDateFin(dateFinPicker.getValue());
        o.setStatut(statutField.getText().trim());
        if (o.getUserId() <= 0) o.setUserId(DEFAULT_USER_ID);
        return o;
    }

    // ═══════════════════════════════════════════════════════
    // ║  VALIDATION                                          ║
    // ═══════════════════════════════════════════════════════

    private boolean valider() {
        resetStyles();
        boolean ok = true;
        if (descField.getText() == null || descField.getText().trim().length() < 3) {
            err(descField, "Description obligatoire (min. 3 car.)"); ok = false;
        }
        if (typeField.getText() == null || typeField.getText().trim().length() < 2) {
            err(typeField, "Type invalide (min. 2 car.)"); ok = false;
        }
        if (valeurCibleField.getText() == null || valeurCibleField.getText().trim().isEmpty()) {
            err(valeurCibleField, "Valeur cible obligatoire"); ok = false;
        } else {
            try {
                int v = Integer.parseInt(valeurCibleField.getText().trim());
                if (v <= 0) { err(valeurCibleField, "Valeur > 0"); ok = false; }
            } catch (NumberFormatException e) { err(valeurCibleField, "Valeur invalide"); ok = false; }
        }
        if (dateDebutPicker.getValue() == null) {
            dateDebutPicker.setStyle("-fx-border-color:#ef4444;");
            msg("Date de début obligatoire", true); ok = false;
        }
        if (dateFinPicker.getValue() == null) {
            dateFinPicker.setStyle("-fx-border-color:#ef4444;");
            msg("Date de fin obligatoire", true); ok = false;
        }
        if (dateDebutPicker.getValue() != null && dateFinPicker.getValue() != null
                && dateFinPicker.getValue().isBefore(dateDebutPicker.getValue())) {
            dateFinPicker.setStyle("-fx-border-color:#ef4444;");
            msg("La fin doit être après le début", true); ok = false;
        }
        if (statutField.getText() == null || statutField.getText().trim().length() < 2) {
            err(statutField, "Statut invalide (min. 2 car.)"); ok = false;
        }
        return ok;
    }

    private void err(TextField f, String m) {
        f.setStyle("-fx-border-color:#ef4444; -fx-border-width:1; -fx-border-radius:8; -fx-background-radius:8;");
        msg(m, true);
    }

    private void resetStyles() {
        for (TextField f : new TextField[]{descField, typeField, valeurCibleField, statutField})
            f.setStyle("");
        dateDebutPicker.setStyle("");
        dateFinPicker.setStyle("");
        if (msgLabel != null) msgLabel.setText("");
    }

    private void remplirFormulaire(Objectif o) {
        descField.setText(o.getDescription());
        typeField.setText(o.getType());
        valeurCibleField.setText(String.valueOf(o.getValeurCible()));
        statutField.setText(o.getStatut() != null ? o.getStatut() : "");
        dateDebutPicker.setValue(o.getDateDebut());
        dateFinPicker.setValue(o.getDateFin());
    }

    private void viderFormulaire() {
        descField.clear(); typeField.clear(); valeurCibleField.clear(); statutField.clear();
        dateDebutPicker.setValue(null); dateFinPicker.setValue(null);
        resetStyles();
    }

    private void msg(String m, boolean err) {
        if (msgLabel != null) {
            msgLabel.setText(m);
            msgLabel.setStyle(err
                    ? "-fx-text-fill:#ef4444; -fx-font-size:12px;"
                    : "-fx-text-fill:#10b981; -fx-font-size:12px;");
        }
    }
}
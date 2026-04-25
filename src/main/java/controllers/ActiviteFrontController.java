package controllers;

import utils.WeatherService;
import Models.Activite;
import Models.Objectif;
import Services.ServiceActivite;
import Services.ServiceObjectif;

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

// ── JavaMail ──
import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;

// ── JavaFX ──
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;

// ── Java standard ──
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.sql.SQLDataException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class ActiviteFrontController {

    // ═══════════════════════════════════════════════════════
    // ║  FXML INJECTIONS                                    ║
    // ═══════════════════════════════════════════════════════

    // ── Stat labels ────────────────────────────────────────
    @FXML private Label statTotalLabel;
    @FXML private Label statCaloriesLabel;
    @FXML private Label statDureeLabel;

    // ── Card grid ────────────────────────────────────────
    @FXML private FlowPane cardGrid;
    @FXML private Label emptyLabel;

    // ── Search and Filter UI (à ajouter dans le FXML) ──────────────
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterTypeCombo;
    @FXML private ComboBox<String> filterIntensiteCombo;
    @FXML private ComboBox<String> sortCombo;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;

    // ── Form ───────────────────────────────────────────────
    @FXML private VBox       formPane;
    @FXML private HBox       btnAjouterBox;
    @FXML private HBox       btnModifierBox;
    @FXML private TextField  typeField;
    @FXML private TextField  dureeField;
    @FXML private TextField  caloriesField;
    @FXML private TextField  intensiteField;
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<Objectif> objectifCombo;
    @FXML private Label      msgLabel;

    // ── Weather ────────────────────────────────────────────
    @FXML private Label weatherLabel;
    @FXML private Label weatherAdviceLabel;

    // ── Services ───────────────────────────────────────────
    private final WeatherService weatherService = new WeatherService();
    private final ServiceActivite service    = new ServiceActivite();
    private final ServiceObjectif objService = new ServiceObjectif();

    // ── Data ───────────────────────────────────────────────
    private Activite currentEdit = null;
    private ObservableList<Activite> allActivites = FXCollections.observableArrayList();

    // ── Filtered/Sorted data ─────────────────────────────────
    private FilteredList<Activite> filteredActivites;
    private SortedList<Activite> sortedActivites;

    // ── Filter options ───────────────────────────────────────
    private final String[] TYPE_FILTERS = {"Tous"};
    private final String[] INTENSITE_FILTERS = {"Tous", "Faible", "Modérée", "Élevée"};
    private final String[] SORT_OPTIONS = {"Date (plus récente)", "Date (plus ancienne)",
            "Calories (décroissant)", "Calories (croissant)",
            "Durée (décroissante)", "Durée (croissante)",
            "Type (A-Z)", "Type (Z-A)"};

    // ═══════════════════════════════════════════════════════
    // ║  THEME COLORS                                       ║
    // ═══════════════════════════════════════════════════════

    private static final String COULEUR_FOND      = "#0a0e1a";
    private static final String COULEUR_SURFACE   = "#161b2e";
    private static final String COULEUR_ACCENT    = "#00d4ff";
    private static final String COULEUR_BORDURE   = "#2a3142";
    private static final String COULEUR_TEXTE     = "#ffffff";
    private static final String COULEUR_SECOND    = "#94a3b8";
    private static final String COULEUR_SUCCES    = "#22c55e";
    private static final String COULEUR_WARNING   = "#f59e0b";
    private static final String COULEUR_ERREUR    = "#ef4444";

    // AWT Colors for PDF
    private static final java.awt.Color AWT_FOND      = new java.awt.Color(0x0a, 0x0e, 0x1a);
    private static final java.awt.Color AWT_SURFACE   = new java.awt.Color(0x16, 0x1b, 0x2e);
    private static final java.awt.Color AWT_ACCENT    = new java.awt.Color(0x00, 0xd4, 0xff);
    private static final java.awt.Color AWT_BORDURE   = new java.awt.Color(0x2a, 0x31, 0x42);
    private static final java.awt.Color AWT_TEXTE     = new java.awt.Color(0xff, 0xff, 0xff);
    private static final java.awt.Color AWT_SECOND    = new java.awt.Color(0x94, 0xa3, 0xb8);
    private static final java.awt.Color AWT_SUCCES    = new java.awt.Color(0x22, 0xc5, 0x5e);
    private static final java.awt.Color AWT_WARNING   = new java.awt.Color(0xf5, 0x9e, 0x0b);
    private static final java.awt.Color AWT_ERREUR    = new java.awt.Color(0xef, 0x44, 0x44);

    // ═══════════════════════════════════════════════════════
    // ║  OLLAMA CONFIG                                      ║
    // ═══════════════════════════════════════════════════════

    private static final String OLLAMA_URL   = "http://localhost:11434/api/generate";
    private static final String OLLAMA_MODEL = "llama3.2";

    // ═══════════════════════════════════════════════════════
    // ║  LIFECYCLE                                          ║
    // ═══════════════════════════════════════════════════════

    @FXML
    public void initialize() {
        initSearchAndFilter();
        chargerObjectifs();
        charger();
        chargerWeather();
    }

    // ═══════════════════════════════════════════════════════
    // ║  SEARCH & FILTER INITIALIZATION                      ║
    // ═══════════════════════════════════════════════════════

    private void initSearchAndFilter() {
        // Initialiser les ComboBox
        if (filterIntensiteCombo != null) {
            filterIntensiteCombo.getItems().setAll(INTENSITE_FILTERS);
            filterIntensiteCombo.setValue("Tous");
            filterIntensiteCombo.setOnAction(e -> applyFilterAndSort());
        }

        if (sortCombo != null) {
            sortCombo.getItems().setAll(SORT_OPTIONS);
            sortCombo.setValue("Date (plus récente)");
            sortCombo.setOnAction(e -> applyFilterAndSort());
        }

        if (searchField != null) {
            searchField.textProperty().addListener((obs, old, val) -> applyFilterAndSort());
        }

        if (startDatePicker != null) {
            startDatePicker.valueProperty().addListener((obs, old, val) -> applyFilterAndSort());
        }

        if (endDatePicker != null) {
            endDatePicker.valueProperty().addListener((obs, old, val) -> applyFilterAndSort());
        }

        // Mettre à jour les types dynamiquement après chargement
        if (filterTypeCombo != null) {
            filterTypeCombo.getItems().setAll("Tous");
            filterTypeCombo.setValue("Tous");
            filterTypeCombo.setOnAction(e -> applyFilterAndSort());
        }
    }

    private void updateTypeFilterOptions() {
        if (filterTypeCombo == null) return;

        Set<String> types = allActivites.stream()
                .map(Activite::getType)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<String> options = new ArrayList<>();
        options.add("Tous");
        options.addAll(types.stream().sorted().collect(Collectors.toList()));
        filterTypeCombo.getItems().setAll(options);

        if (!options.contains(filterTypeCombo.getValue())) {
            filterTypeCombo.setValue("Tous");
        }
    }

    private void applyFilterAndSort() {
        if (filteredActivites == null || allActivites.isEmpty()) return;

        // Mettre à jour le filtre
        filteredActivites.setPredicate(activite -> {
            // Filtre par texte de recherche
            String searchText = searchField != null ? searchField.getText().toLowerCase().trim() : "";
            if (!searchText.isEmpty()) {
                boolean matchType = activite.getType() != null &&
                        activite.getType().toLowerCase().contains(searchText);
                boolean matchIntensite = activite.getIntensite() != null &&
                        activite.getIntensite().toLowerCase().contains(searchText);
                if (!matchType && !matchIntensite) return false;
            }

            // Filtre par type
            String filterType = filterTypeCombo != null && filterTypeCombo.getValue() != null ?
                    filterTypeCombo.getValue() : "Tous";
            if (!filterType.equals("Tous") && activite.getType() != null) {
                if (!activite.getType().equals(filterType)) return false;
            }

            // Filtre par intensité
            String filterIntensite = filterIntensiteCombo != null ? filterIntensiteCombo.getValue() : "Tous";
            if (!filterIntensite.equals("Tous") && activite.getIntensite() != null) {
                if (!activite.getIntensite().equalsIgnoreCase(filterIntensite)) return false;
            }

            // Filtre par date de début
            LocalDate start = startDatePicker != null ? startDatePicker.getValue() : null;
            if (start != null && activite.getDate() != null) {
                if (activite.getDate().isBefore(start)) return false;
            }

            // Filtre par date de fin
            LocalDate end = endDatePicker != null ? endDatePicker.getValue() : null;
            if (end != null && activite.getDate() != null) {
                if (activite.getDate().isAfter(end)) return false;
            }

            return true;
        });

        // Mettre à jour le tri
        String sortOption = sortCombo != null ? sortCombo.getValue() : "Date (plus récente)";
        sortedActivites.setComparator(getComparator(sortOption));

        // Mettre à jour l'affichage
        buildCardsFromSortedList();
        updateStatsFromFiltered();
    }

    private Comparator<Activite> getComparator(String sortOption) {
        switch (sortOption) {
            case "Date (plus récente)":
                return (a, b) -> {
                    if (a.getDate() == null) return 1;
                    if (b.getDate() == null) return -1;
                    return b.getDate().compareTo(a.getDate());
                };
            case "Date (plus ancienne)":
                return (a, b) -> {
                    if (a.getDate() == null) return 1;
                    if (b.getDate() == null) return -1;
                    return a.getDate().compareTo(b.getDate());
                };
            case "Calories (décroissant)":
                return (a, b) -> Double.compare(b.getCaloriesBrulees(), a.getCaloriesBrulees());
            case "Calories (croissant)":
                return (a, b) -> Double.compare(a.getCaloriesBrulees(), b.getCaloriesBrulees());
            case "Durée (décroissante)":
                return (a, b) -> Integer.compare(b.getDuree(), a.getDuree());
            case "Durée (croissante)":
                return (a, b) -> Integer.compare(a.getDuree(), b.getDuree());
            case "Type (A-Z)":
                return Comparator.comparing(Activite::getType, Comparator.nullsLast(String::compareTo));
            case "Type (Z-A)":
                return (a, b) -> {
                    String typeA = a.getType() != null ? a.getType() : "";
                    String typeB = b.getType() != null ? b.getType() : "";
                    return typeB.compareTo(typeA);
                };
            default:
                return (a, b) -> {
                    if (a.getDate() == null) return 1;
                    if (b.getDate() == null) return -1;
                    return b.getDate().compareTo(a.getDate());
                };
        }
    }

    private void buildCardsFromSortedList() {
        if (sortedActivites == null) return;

        cardGrid.getChildren().clear();

        if (sortedActivites.isEmpty()) {
            if (emptyLabel != null) {
                emptyLabel.setVisible(true);
                emptyLabel.setManaged(true);
                emptyLabel.setText("Aucune activité ne correspond aux critères.");
            }
            return;
        }

        if (emptyLabel != null) {
            emptyLabel.setVisible(false);
            emptyLabel.setManaged(false);
        }

        for (Activite a : sortedActivites) {
            cardGrid.getChildren().add(buildCard(a));
        }
    }

    private void updateStatsFromFiltered() {
        if (sortedActivites == null) return;

        statTotalLabel.setText(String.valueOf(sortedActivites.size()));
        double totalCal = sortedActivites.stream().mapToDouble(Activite::getCaloriesBrulees).sum();
        statCaloriesLabel.setText(String.format("%.0f kcal", totalCal));
        int totalDuree = sortedActivites.stream().mapToInt(Activite::getDuree).sum();
        statDureeLabel.setText(totalDuree + " min");
    }

    // ═══════════════════════════════════════════════════════
    // ║  DATA LOADING                                       ║
    // ═══════════════════════════════════════════════════════

    private void chargerObjectifs() {
        try {
            List<Objectif> objs = objService.recuperer();
            objectifCombo.getItems().setAll(objs);
            objectifCombo.setConverter(new StringConverter<>() {
                @Override public String toString(Objectif o) {
                    return o == null ? "" : o.getId() + " – " + o.getDescription();
                }
                @Override public Objectif fromString(String s) { return null; }
            });
        } catch (SQLDataException e) {
            msg("Erreur chargement objectifs : " + e.getMessage(), true);
        }
    }

    @FXML
    public void charger() {
        try {
            List<Activite> list = service.recuperer();
            allActivites = FXCollections.observableArrayList(list);

            // Initialiser les listes filtrées et triées
            filteredActivites = new FilteredList<>(allActivites, p -> true);
            sortedActivites = new SortedList<>(filteredActivites, getComparator("Date (plus récente)"));

            updateTypeFilterOptions();
            updateStats(list);
            buildCards(list);
        } catch (SQLDataException e) {
            msg("Erreur BD : " + e.getMessage(), true);
        }
    }

    private void updateStats(List<Activite> list) {
        statTotalLabel.setText(String.valueOf(list.size()));
        double totalCal = list.stream().mapToDouble(Activite::getCaloriesBrulees).sum();
        statCaloriesLabel.setText(String.format("%.0f kcal", totalCal));
        int totalDuree = list.stream().mapToInt(Activite::getDuree).sum();
        statDureeLabel.setText(totalDuree + " min");
    }

    // ═══════════════════════════════════════════════════════
    // ║  CARD BUILDER                                       ║
    // ═══════════════════════════════════════════════════════

    private void buildCards(List<Activite> list) {
        cardGrid.getChildren().clear();

        if (list.isEmpty()) {
            if (emptyLabel != null) {
                emptyLabel.setVisible(true);
                emptyLabel.setManaged(true);
                emptyLabel.setText("Aucune activité enregistrée. Cliquez sur « Ajouter » pour commencer !");
            }
            return;
        }
        if (emptyLabel != null) { emptyLabel.setVisible(false); emptyLabel.setManaged(false); }

        for (Activite a : list) {
            cardGrid.getChildren().add(buildCard(a));
        }
    }

    private VBox buildCard(Activite a) {
        // ── Card container ──
        VBox card = new VBox(12);
        card.setPrefWidth(280);
        card.setMinWidth(240);
        card.getStyleClass().add("act-card");

        // ── Top row: type icon + type label + intensité badge ──
        HBox top = new HBox(10);
        top.setAlignment(Pos.CENTER_LEFT);

        StackPane icon = new StackPane();
        icon.getStyleClass().addAll("act-icon", resolveIconColor(a.getType()));
        Label iconLbl = new Label(resolveEmoji(a.getType()));
        iconLbl.setStyle("-fx-font-size:18px;");
        icon.getChildren().add(iconLbl);

        VBox titleBox = new VBox(2);
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        Label typeLabel = new Label(a.getType() != null ? capitalize(a.getType()) : "—");
        typeLabel.getStyleClass().add("act-type");
        Label dateLabel = new Label(a.getDate() != null ? a.getDate().toString() : "");
        dateLabel.getStyleClass().add("act-date");
        titleBox.getChildren().addAll(typeLabel, dateLabel);

        Label intensiteBadge = new Label(a.getIntensite() != null ? a.getIntensite() : "—");
        intensiteBadge.getStyleClass().addAll("act-badge", resolveBadgeStyle(a.getIntensite()));

        top.getChildren().addAll(icon, titleBox, intensiteBadge);

        // ── Stats row: durée · calories ──
        HBox stats = new HBox(16);
        stats.setAlignment(Pos.CENTER_LEFT);
        stats.getStyleClass().add("act-stats-row");

        VBox durBox = new VBox(2);
        Label durVal = new Label(a.getDuree() + " min");
        durVal.getStyleClass().add("act-metric-val");
        Label durLbl = new Label("Durée");
        durLbl.getStyleClass().add("act-metric-lbl");
        durBox.getChildren().addAll(durVal, durLbl);

        VBox calBox = new VBox(2);
        Label calVal = new Label(String.format("%.0f kcal", a.getCaloriesBrulees()));
        calVal.getStyleClass().add("act-metric-val");
        Label calLbl = new Label("Calories");
        calLbl.getStyleClass().add("act-metric-lbl");
        calBox.getChildren().addAll(calVal, calLbl);

        stats.getChildren().addAll(durBox, makeDivider(), calBox);

        // ── Action row ──
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button btnEdit = new Button("✏  Modifier");
        btnEdit.getStyleClass().addAll("act-btn", "act-btn-edit");
        btnEdit.setOnAction(e -> {
            remplirFormulaire(a);
            currentEdit = a;
            setModeEdition(true);
            showForm(true);
            msg("Modifiez les champs puis cliquez Modifier.", false);
        });

        Button btnDel = new Button("✕  Supprimer");
        btnDel.getStyleClass().addAll("act-btn", "act-btn-delete");
        btnDel.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Supprimer \"" + a.getType() + "\" ?", ButtonType.YES, ButtonType.NO);
            confirm.setHeaderText(null);
            confirm.showAndWait().ifPresent(bt -> {
                if (bt == ButtonType.YES) {
                    try { service.supprimer(a); charger(); }
                    catch (SQLDataException ex) { msg("Erreur : " + ex.getMessage(), true); }
                }
            });
        });

        actions.getChildren().addAll(btnEdit, btnDel);

        card.getChildren().addAll(top, stats, actions);
        return card;
    }

    // ═══════════════════════════════════════════════════════
    // ║  HELPERS                                            ║
    // ═══════════════════════════════════════════════════════

    private Label makeDivider() {
        Label sep = new Label("|");
        sep.setStyle("-fx-text-fill: #2a3142; -fx-font-size: 16px;");
        return sep;
    }

    private String resolveEmoji(String type) {
        if (type == null) return "🏃";
        String t = type.toLowerCase();
        if (t.contains("natation") || t.contains("swim"))  return "🏊";
        if (t.contains("course") || t.contains("run"))     return "🏃";
        if (t.contains("marche") || t.contains("walk"))    return "🚶";
        if (t.contains("velo") || t.contains("cycl"))      return "🚴";
        if (t.contains("yoga"))                             return "🧘";
        if (t.contains("gym") || t.contains("muscul"))     return "💪";
        if (t.contains("foot") || t.contains("soccer"))    return "⚽";
        return "⚡";
    }

    private String resolveIconColor(String type) {
        if (type == null) return "act-icon-cyan";
        String t = type.toLowerCase();
        if (t.contains("natation") || t.contains("swim"))  return "act-icon-blue";
        if (t.contains("yoga"))                             return "act-icon-purple";
        if (t.contains("gym") || t.contains("muscul"))     return "act-icon-orange";
        if (t.contains("foot"))                             return "act-icon-green";
        return "act-icon-cyan";
    }

    private String resolveBadgeStyle(String intensite) {
        if (intensite == null) return "act-badge-default";
        String i = intensite.toLowerCase();
        if (i.contains("elev") || i.contains("haute") || i.contains("high")) return "act-badge-high";
        if (i.contains("moy") || i.contains("med"))                           return "act-badge-mid";
        if (i.contains("faibl") || i.contains("low"))                         return "act-badge-low";
        return "act-badge-default";
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }

    // ═══════════════════════════════════════════════════════
    // ║  FORM HANDLING                                      ║
    // ═══════════════════════════════════════════════════════

    @FXML public void ouvrirFormulaire() {
        currentEdit = null;
        viderFormulaire();
        setModeEdition(false);
        showForm(true);
    }

    @FXML public void annuler() {
        viderFormulaire();
        setModeEdition(false);
        showForm(false);
        currentEdit = null;
    }

    private void showForm(boolean v) {
        if (formPane != null) { formPane.setVisible(v); formPane.setManaged(v); }
    }

    private void setModeEdition(boolean e) {
        if (btnAjouterBox  != null) { btnAjouterBox.setVisible(!e);  btnAjouterBox.setManaged(!e); }
        if (btnModifierBox != null) { btnModifierBox.setVisible(e);   btnModifierBox.setManaged(e); }
    }

    // ═══════════════════════════════════════════════════════
    // ║  CRUD                                               ║
    // ═══════════════════════════════════════════════════════

    @FXML
    public void ajouter() {
        if (!valider()) return;
        try {
            Activite a = buildFromForm(new Activite());
            service.ajouter(a);
            msg("✓ Activité ajoutée !", false);
            viderFormulaire(); showForm(false); charger();
        } catch (SQLDataException e) { msg("Erreur BD : " + e.getMessage(), true); }
    }

    @FXML
    public void modifier() {
        if (currentEdit == null) { msg("Aucune activité sélectionnée.", true); return; }
        if (!valider()) return;
        try {
            buildFromForm(currentEdit);
            service.modifier(currentEdit);
            msg("✓ Activité modifiée !", false);
            viderFormulaire(); setModeEdition(false); showForm(false);
            currentEdit = null; charger();
        } catch (SQLDataException e) { msg("Erreur BD : " + e.getMessage(), true); }
    }

    private Activite buildFromForm(Activite a) {
        a.setType(typeField.getText().trim());
        a.setDuree(Integer.parseInt(dureeField.getText().trim()));
        a.setCaloriesBrulees(Double.parseDouble(caloriesField.getText().trim()));
        a.setDate(datePicker.getValue());
        a.setIntensite(intensiteField.getText().trim());
        a.setObjectifId(objectifCombo.getValue().getId());
        return a;
    }

    // ═══════════════════════════════════════════════════════
    // ║  VALIDATION                                         ║
    // ═══════════════════════════════════════════════════════

    private boolean valider() {
        resetStyles();
        boolean ok = true;
        if (typeField.getText() == null || typeField.getText().trim().length() < 2) {
            err(typeField, "Type invalide (min. 2 car.)"); ok = false;
        }
        if (dureeField.getText() == null || dureeField.getText().trim().isEmpty()) {
            err(dureeField, "Durée obligatoire"); ok = false;
        } else {
            try {
                int d = Integer.parseInt(dureeField.getText().trim());
                if (d <= 0 || d > 1440) { err(dureeField, "Durée entre 1 et 1440 min"); ok = false; }
            } catch (NumberFormatException e) { err(dureeField, "Durée invalide"); ok = false; }
        }
        if (caloriesField.getText() == null || caloriesField.getText().trim().isEmpty()) {
            err(caloriesField, "Calories obligatoires"); ok = false;
        } else {
            try {
                double c = Double.parseDouble(caloriesField.getText().trim());
                if (c < 0 || c > 10000) { err(caloriesField, "Calories entre 0 et 10 000"); ok = false; }
            } catch (NumberFormatException e) { err(caloriesField, "Calories invalides"); ok = false; }
        }
        if (datePicker.getValue() == null) {
            datePicker.setStyle("-fx-border-color:#ef4444;"); msg("Date obligatoire", true); ok = false;
        }
        if (intensiteField.getText() == null || intensiteField.getText().trim().length() < 2) {
            err(intensiteField, "Intensité invalide (min. 2 car.)"); ok = false;
        }
        if (objectifCombo.getValue() == null) {
            objectifCombo.setStyle("-fx-border-color:#ef4444;"); msg("Sélectionnez un objectif", true); ok = false;
        }
        return ok;
    }

    private void err(TextField f, String m) {
        f.setStyle("-fx-border-color:#ef4444; -fx-border-width:1; -fx-border-radius:8; -fx-background-radius:8;");
        msg(m, true);
    }

    private void resetStyles() {
        for (TextField f : new TextField[]{typeField, dureeField, caloriesField, intensiteField})
            f.setStyle("");
        datePicker.setStyle(""); objectifCombo.setStyle("");
        if (msgLabel != null) msgLabel.setText("");
    }

    private void remplirFormulaire(Activite a) {
        typeField.setText(a.getType());
        dureeField.setText(String.valueOf(a.getDuree()));
        caloriesField.setText(String.valueOf(a.getCaloriesBrulees()));
        intensiteField.setText(a.getIntensite() != null ? a.getIntensite() : "");
        datePicker.setValue(a.getDate());
        objectifCombo.getItems().stream()
                .filter(o -> o.getId() == a.getObjectifId())
                .findFirst().ifPresent(objectifCombo::setValue);
    }

    private void viderFormulaire() {
        typeField.clear(); dureeField.clear(); caloriesField.clear(); intensiteField.clear();
        datePicker.setValue(null); objectifCombo.setValue(null);
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

    // ═══════════════════════════════════════════════════════
    // ║  WEATHER                                            ║
    // ═══════════════════════════════════════════════════════

    private void chargerWeather() {
        try {
            JSONObject data = weatherService.getWeather("Tunis");
            if (data == null) {
                weatherLabel.setText("Erreur météo");
                weatherAdviceLabel.setText("Impossible de récupérer");
                return;
            }

            double temp = data.getJSONObject("main").getDouble("temp");
            String condition = data.getJSONArray("weather")
                    .getJSONObject(0)
                    .getString("main");

            weatherLabel.setText(temp + "°C - " + condition);

            if (temp >= 15 && temp <= 30 &&
                    !condition.toLowerCase().contains("rain") &&
                    !condition.toLowerCase().contains("storm")) {
                weatherAdviceLabel.setText("✅ Activités extérieures recommandées");
                weatherAdviceLabel.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
            } else {
                weatherAdviceLabel.setText("❌ Activités extérieures déconseillées");
                weatherAdviceLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
            }
        } catch (Exception e) {
            weatherLabel.setText("Erreur");
            weatherAdviceLabel.setText("Erreur météo");
            e.printStackTrace();
        }
    }

    // ═══════════════════════════════════════════════════════
    // ║  EXPORT PDF                                         ║
    // ═══════════════════════════════════════════════════════

    @FXML
    public void exporterPDF() {
        if (allActivites.isEmpty()) {
            msg("Aucune activité à exporter", true);
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le rapport PDF");
        fileChooser.setInitialFileName("activites_" +
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".pdf");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf"));
        File file = fileChooser.showSaveDialog(cardGrid.getScene().getWindow());
        if (file == null) return;

        try {
            genererPDF(allActivites, file);
            msg("PDF exporté : " + file.getName(), false);
            if (java.awt.Desktop.isDesktopSupported()) java.awt.Desktop.getDesktop().open(file);
        } catch (Exception e) {
            msg("Erreur export PDF : " + e.getMessage(), true);
        }
    }

    private void genererPDF(List<Activite> activites, File fichier) throws Exception {
        Document document = new Document(PageSize.A4, 40, 40, 50, 50);
        PdfWriter writer  = PdfWriter.getInstance(document, new FileOutputStream(fichier));
        document.open();

        PdfContentByte cb = writer.getDirectContent();
        float pw = document.getPageSize().getWidth();
        float ph = document.getPageSize().getHeight();

        // Background
        cb.setColorFill(AWT_FOND);
        cb.rectangle(0, 0, pw, ph);
        cb.fill();

        // Header box
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

        BaseFont bfBold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, false);
        BaseFont bf     = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, false);

        // Title
        cb.beginText();
        cb.setFontAndSize(bfBold, 28);
        cb.setColorFill(AWT_TEXTE);
        cb.setTextMatrix(50, ph - 65);
        cb.showText("Rapport d'Activités Sportives");
        cb.endText();

        // Subtitle
        double totalCal = activites.stream().mapToDouble(Activite::getCaloriesBrulees).sum();
        int totalDuree = activites.stream().mapToInt(Activite::getDuree).sum();
        String dateGen = "Généré le " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                + "  |  " + activites.size() + " activité(s)  |  "
                + String.format("%.0f kcal", totalCal) + "  |  " + totalDuree + " min";

        cb.beginText();
        cb.setFontAndSize(bf, 11);
        cb.setColorFill(AWT_SECOND);
        cb.setTextMatrix(50, ph - 85);
        cb.showText(dateGen);
        cb.endText();

        document.add(new Paragraph(" "));
        document.add(new Paragraph(" "));
        document.add(new Paragraph(" "));

        // Stats cards
        float cardY   = ph - 200;
        float cardW   = (pw - 100) / 3f;
        float cardH   = 60;
        float cardGap = 13;
        float startX  = 40;

        Map<String, Double> parType = new HashMap<>();
        for (Activite a : activites) {
            parType.merge(a.getType() != null ? a.getType() : "Autre", a.getCaloriesBrulees(), Double::sum);
        }
        String topType = parType.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse("—");

        Object[][] stats = {
                { "Total activités", String.valueOf(activites.size()), AWT_ACCENT,   AWT_SURFACE },
                { "Calories brûlées", String.format("%.0f kcal", totalCal), AWT_SUCCES, AWT_SURFACE },
                { "Durée totale", totalDuree + " min", AWT_WARNING, AWT_SURFACE },
        };

        for (int i = 0; i < stats.length; i++) {
            float cx = startX + i * (cardW + cardGap);

            cb.setColorFill((java.awt.Color) stats[i][3]);
            cb.roundRectangle(cx, cardY, cardW, cardH, 10);
            cb.fill();

            cb.setColorStroke(AWT_BORDURE);
            cb.setLineWidth(0.5f);
            cb.roundRectangle(cx, cardY, cardW, cardH, 10);
            cb.stroke();

            cb.setColorFill((java.awt.Color) stats[i][2]);
            cb.roundRectangle(cx, cardY + cardH - 4, cardW, 4, 2);
            cb.fill();

            cb.beginText();
            cb.setFontAndSize(bf, 10);
            cb.setColorFill(AWT_SECOND);
            cb.setTextMatrix(cx + 12, cardY + cardH - 20);
            cb.showText((String) stats[i][0]);

            cb.setFontAndSize(bfBold, 16);
            cb.setColorFill((java.awt.Color) stats[i][2]);
            cb.setTextMatrix(cx + 12, cardY + 15);
            cb.showText((String) stats[i][1]);
            cb.endText();
        }

        for (int i = 0; i < 5; i++) document.add(new Paragraph(" "));

        // Table
        com.lowagie.text.Font fontEntete = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 10, com.lowagie.text.Font.BOLD, AWT_ACCENT);
        com.lowagie.text.Font fontNormal   = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 10, com.lowagie.text.Font.NORMAL, AWT_TEXTE);
        com.lowagie.text.Font fontSecond  = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 9, com.lowagie.text.Font.NORMAL, AWT_SECOND);
        com.lowagie.text.Font fontTotal   = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 11, com.lowagie.text.Font.BOLD, AWT_ACCENT);

        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2.5f, 1.5f, 2f, 2f, 2f, 1.5f});
        table.setSpacingBefore(10);

        String[] entetes = { "Type", "Durée", "Calories", "Date", "Intensité", "Objectif" };
        for (String h : entetes) {
            PdfPCell cell = new PdfPCell(new Phrase(h, fontEntete));
            cell.setBackgroundColor(AWT_SURFACE);
            cell.setBorderColor(AWT_ACCENT);
            cell.setBorderWidth(0.5f);
            cell.setBorderWidthBottom(2f);
            cell.setPadding(12);
            cell.setHorizontalAlignment(Element.ALIGN_LEFT);
            table.addCell(cell);
        }

        boolean pair = false;
        for (Activite a : activites) {
            java.awt.Color bgLigne = pair ? new java.awt.Color(0x12, 0x17, 0x28) : AWT_SURFACE;

            PdfPCell cType = new PdfPCell(new Phrase(
                    a.getType() != null ? capitalize(a.getType()) : "-", fontNormal));
            cType.setBackgroundColor(bgLigne);
            cType.setBorderColor(AWT_BORDURE);
            cType.setBorderWidth(0.3f);
            cType.setPadding(10);
            table.addCell(cType);

            PdfPCell cDuree = new PdfPCell(new Phrase(a.getDuree() + " min", fontNormal));
            cDuree.setBackgroundColor(bgLigne);
            cDuree.setBorderColor(AWT_BORDURE);
            cDuree.setBorderWidth(0.3f);
            cDuree.setPadding(10);
            cDuree.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cDuree);

            PdfPCell cCal = new PdfPCell(new Phrase(
                    String.format("%.0f", a.getCaloriesBrulees()) + " kcal", fontNormal));
            cCal.setBackgroundColor(bgLigne);
            cCal.setBorderColor(AWT_BORDURE);
            cCal.setBorderWidth(0.3f);
            cCal.setPadding(10);
            cCal.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(cCal);

            PdfPCell cDate = new PdfPCell(new Phrase(
                    a.getDate() != null ? a.getDate().toString() : "-", fontSecond));
            cDate.setBackgroundColor(bgLigne);
            cDate.setBorderColor(AWT_BORDURE);
            cDate.setBorderWidth(0.3f);
            cDate.setPadding(10);
            table.addCell(cDate);

            String intensite = a.getIntensite() != null ? a.getIntensite() : "-";
            com.lowagie.text.Font fontIntensite;
            java.awt.Color bgBadge;
            String iLower = intensite.toLowerCase();
            if (iLower.contains("haute") || iLower.contains("elev") || iLower.contains("high")) {
                fontIntensite = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 8, com.lowagie.text.Font.BOLD, AWT_ERREUR);
                bgBadge = new java.awt.Color(0xef, 0x44, 0x44);
            } else if (iLower.contains("moy") || iLower.contains("med")) {
                fontIntensite = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 8, com.lowagie.text.Font.BOLD, AWT_WARNING);
                bgBadge = new java.awt.Color(0xf5, 0x9e, 0x0b);
            } else {
                fontIntensite = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 8, com.lowagie.text.Font.BOLD, AWT_SUCCES);
                bgBadge = new java.awt.Color(0x22, 0xc5, 0x5e);
            }

            PdfPCell cIntensite = new PdfPCell(new Phrase(intensite, fontIntensite));
            cIntensite.setBackgroundColor(bgBadge);
            cIntensite.setBorderColor(AWT_BORDURE);
            cIntensite.setBorderWidth(0.3f);
            cIntensite.setPadding(10);
            cIntensite.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cIntensite);

            PdfPCell cObj = new PdfPCell(new Phrase("Obj. " + a.getObjectifId(), fontSecond));
            cObj.setBackgroundColor(bgLigne);
            cObj.setBorderColor(AWT_BORDURE);
            cObj.setBorderWidth(0.3f);
            cObj.setPadding(10);
            cObj.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cObj);

            pair = !pair;
        }

        // Total row
        PdfPCell cTotalLabel = new PdfPCell(new Phrase("TOTAL", fontTotal));
        cTotalLabel.setBackgroundColor(AWT_ACCENT);
        cTotalLabel.setBorderColor(AWT_ACCENT);
        cTotalLabel.setBorderWidth(1f);
        cTotalLabel.setBorderWidthTop(2f);
        cTotalLabel.setPadding(12);
        cTotalLabel.setColspan(2);
        table.addCell(cTotalLabel);

        PdfPCell cTotalCal = new PdfPCell(new Phrase(String.format("%.0f kcal", totalCal), fontTotal));
        cTotalCal.setBackgroundColor(AWT_ACCENT);
        cTotalCal.setBorderColor(AWT_ACCENT);
        cTotalCal.setBorderWidth(1f);
        cTotalCal.setBorderWidthTop(2f);
        cTotalCal.setPadding(12);
        cTotalCal.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(cTotalCal);

        PdfPCell cTotalDuree = new PdfPCell(new Phrase(totalDuree + " min", fontTotal));
        cTotalDuree.setBackgroundColor(AWT_ACCENT);
        cTotalDuree.setBorderColor(AWT_ACCENT);
        cTotalDuree.setBorderWidth(1f);
        cTotalDuree.setBorderWidthTop(2f);
        cTotalDuree.setPadding(12);
        cTotalDuree.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(cTotalDuree);

        PdfPCell cVide = new PdfPCell(new Phrase(""));
        cVide.setBackgroundColor(AWT_ACCENT);
        cVide.setBorderColor(AWT_ACCENT);
        cVide.setBorderWidth(1f);
        cVide.setBorderWidthTop(2f);
        cVide.setColspan(2);
        table.addCell(cVide);

        document.add(table);

        document.add(new Paragraph(" "));
        Paragraph pied = new Paragraph("AIVA Sport  -  Rapport généré automatiquement", fontSecond);
        pied.setAlignment(Element.ALIGN_CENTER);
        pied.setSpacingBefore(20);
        document.add(pied);

        document.close();
    }

    // ═══════════════════════════════════════════════════════
    // ║  ENVOI EMAIL RAPPORT                                ║
    // ═══════════════════════════════════════════════════════

    @FXML
    public void envoyerRapportMail() {
        if (allActivites.isEmpty()) {
            msg("Aucune activité à envoyer", true);
            return;
        }

        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.setTitle("Envoyer le rapport par email");
        popup.setResizable(false);

        VBox root = new VBox(16);
        root.setAlignment(Pos.CENTER);
        root.setPrefWidth(420);
        root.setStyle("-fx-background-color:" + COULEUR_FOND + "; -fx-padding:32px;");

        Label titre = new Label("📧 Envoyer le rapport");
        titre.setStyle("-fx-font-size:20px; -fx-font-weight:bold; -fx-text-fill:" + COULEUR_TEXTE + ";");

        double totalCal = allActivites.stream().mapToDouble(Activite::getCaloriesBrulees).sum();
        int totalDuree = allActivites.stream().mapToInt(Activite::getDuree).sum();
        Label info = new Label(allActivites.size() + " activité(s) · "
                + String.format("%.0f kcal", totalCal) + " · " + totalDuree + " min");
        info.setStyle("-fx-font-size:13px; -fx-text-fill:" + COULEUR_ACCENT + ";");

        Label lblEmail = new Label("Adresse email du destinataire :");
        lblEmail.setStyle("-fx-font-size:13px; -fx-text-fill:" + COULEUR_SECOND + ";");

        TextField emailField = new TextField();
        emailField.setPromptText("ex: coach@sport.com");
        emailField.setPrefHeight(44);
        emailField.setStyle("-fx-background-color:#111827; -fx-text-fill:#ffffff;" +
                "-fx-background-radius:8px; -fx-border-color:#2a3142;" +
                "-fx-border-radius:8px; -fx-border-width:1px;" +
                "-fx-padding:10 12 10 12; -fx-font-size:14px;");

        Label statusLabel = new Label();
        statusLabel.setStyle("-fx-font-size:12px; -fx-text-fill:" + COULEUR_ACCENT + ";");

        Button btnEnvoyer = new Button("Envoyer le rapport");
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
                    envoyerMailRapport(finalDest);
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

    private void envoyerMailRapport(String destinataire) throws Exception {
        final String fromEmail = "houssemlemjid02@gmail.com";
        final String password  = "zgis ymnb ntmd pajp";

        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(fromEmail, password);
            }
        });

        double totalCal = allActivites.stream().mapToDouble(Activite::getCaloriesBrulees).sum();
        int totalDuree = allActivites.stream().mapToInt(Activite::getDuree).sum();

        StringBuilder sb = new StringBuilder();
        sb.append("🏃 RAPPORT D'ACTIVITÉS SPORTIVES - AIVA\n");
        sb.append("=====================================\n\n");
        sb.append("📊 Résumé:\n");
        sb.append("   • Total activités: ").append(allActivites.size()).append("\n");
        sb.append("   • Calories brûlées: ").append(String.format("%.0f", totalCal)).append(" kcal\n");
        sb.append("   • Durée totale: ").append(totalDuree).append(" min\n\n");
        sb.append("📋 Détail des activités:\n");

        for (Activite a : allActivites) {
            sb.append("\n─────────────────────────────\n");
            sb.append("🏷 Type: ").append(a.getType()).append("\n");
            sb.append("⏱ Durée: ").append(a.getDuree()).append(" min\n");
            sb.append("🔥 Calories: ").append(String.format("%.0f", a.getCaloriesBrulees())).append(" kcal\n");
            sb.append("📅 Date: ").append(a.getDate()).append("\n");
            sb.append("💪 Intensité: ").append(a.getIntensite()).append("\n");
            sb.append("🎯 Objectif #").append(a.getObjectifId()).append("\n");
        }

        sb.append("\n\nGénéré le ").append(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(fromEmail));
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(destinataire));
        message.setSubject("🏃 Rapport d'Activités Sportives - AIVA");
        message.setText(sb.toString(), "UTF-8");

        Transport.send(message);
    }

    // ═══════════════════════════════════════════════════════
    // ║  COACHING IA (OLLAMA)                               ║
    // ═══════════════════════════════════════════════════════

    @FXML
    public void genererCoaching() {
        if (allActivites.isEmpty()) {
            msg("Aucune activité à analyser pour le coaching", true);
            return;
        }

        Stage loadingStage = new Stage();
        loadingStage.initModality(Modality.APPLICATION_MODAL);
        loadingStage.setTitle("Coach IA en réflexion...");
        loadingStage.setResizable(false);

        VBox loadingRoot = new VBox(16);
        loadingRoot.setAlignment(Pos.CENTER);
        loadingRoot.setPrefSize(300, 150);
        loadingRoot.setStyle("-fx-background-color:" + COULEUR_FOND + "; -fx-padding:30px;");

        Label loadingLabel = new Label("🏋️ Coach IA analyse vos performances...");
        loadingLabel.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:" + COULEUR_ACCENT + ";");

        Label subLabel = new Label("Ollama réfléchit, patientez...");
        subLabel.setStyle("-fx-font-size:12px; -fx-text-fill:" + COULEUR_SECOND + ";");

        loadingRoot.getChildren().addAll(loadingLabel, subLabel);
        loadingStage.setScene(new Scene(loadingRoot));
        loadingStage.show();

        String prompt = construirePromptCoaching();
        Thread thread = new Thread(() -> {
            try {
                String reponse = appellerOllama(prompt);
                javafx.application.Platform.runLater(() -> {
                    loadingStage.close();
                    afficherCoaching(reponse);
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

    private String construirePromptCoaching() {
        double totalCal = allActivites.stream().mapToDouble(Activite::getCaloriesBrulees).sum();
        int totalDuree = allActivites.stream().mapToInt(Activite::getDuree).sum();
        double moyCal = allActivites.isEmpty() ? 0 : totalCal / allActivites.size();
        double moyDuree = allActivites.isEmpty() ? 0 : (double) totalDuree / allActivites.size();
        double maxCal = allActivites.stream().mapToDouble(Activite::getCaloriesBrulees).max().orElse(0);
        double minCal = allActivites.stream().mapToDouble(Activite::getCaloriesBrulees).min().orElse(0);

        Map<String, Long> parType = allActivites.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getType() != null ? a.getType() : "Autre", Collectors.counting()));

        Map<String, Double> calParType = new HashMap<>();
        for (Activite a : allActivites) {
            calParType.merge(a.getType() != null ? a.getType() : "Autre", a.getCaloriesBrulees(), Double::sum);
        }

        StringBuilder sbAll = new StringBuilder();
        allActivites.forEach(a -> sbAll
                .append("- ").append(a.getType())
                .append(" | ").append(a.getDuree()).append(" min")
                .append(" | ").append(String.format("%.0f", a.getCaloriesBrulees())).append(" kcal")
                .append(" | ").append(a.getIntensite())
                .append(" | ").append(a.getDate())
                .append("\n"));

        return "Tu es un coach sportif professionnel et motivant. Analyse les données d'entraînement " +
                "de mon client et donne-lui 5 recommandations de coaching PERSONNALISÉES et CONCRÈTES. " +
                "Sois direct, motivant et cite ses vraies performances. Parle en français.\n\n" +
                "STATISTIQUES GLOBALES:\n" +
                "- Nombre de séances: " + allActivites.size() + "\n" +
                "- Calories totales brûlées: " + String.format("%.0f", totalCal) + " kcal\n" +
                "- Durée totale: " + totalDuree + " min\n" +
                "- Moyenne par séance: " + String.format("%.0f", moyCal) + " kcal, " +
                String.format("%.0f", moyDuree) + " min\n" +
                "- Performance max: " + String.format("%.0f", maxCal) + " kcal\n" +
                "- Performance min: " + String.format("%.0f", minCal) + " kcal\n\n" +
                "RÉPARTITION PAR TYPE:\n" + parType.entrySet().stream()
                .map(e -> "- " + e.getKey() + ": " + e.getValue() + " séance(s)")
                .collect(Collectors.joining("\n")) + "\n\n" +
                "CALORIES PAR TYPE:\n" + calParType.entrySet().stream()
                .map(e -> "- " + e.getKey() + ": " + String.format("%.0f", e.getValue()) + " kcal")
                .collect(Collectors.joining("\n")) + "\n\n" +
                "DÉTAIL DES SÉANCES:\n" + sbAll + "\n\n" +
                "RECOMMANDATIONS DU COACH (5 conseils spécifiques et motivants):\n" +
                "1.";
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
            throw new Exception("Ollama a retourné HTTP " + response.statusCode()
                    + " — vérifie qu'Ollama tourne sur localhost:11434");
        }

        String body = response.body();
        int idx = body.indexOf("\"response\":");
        if (idx == -1) throw new Exception("Réponse Ollama invalide");

        int start = body.indexOf("\"", idx + 11) + 1;
        if (start <= 0) throw new Exception("Impossible de parser la réponse Ollama");

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

    private void afficherCoaching(String coaching) {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.setTitle("🏋️ Coach IA - AIVA Sport");
        popup.setResizable(true);

        VBox root = new VBox(16);
        root.setStyle("-fx-background-color:" + COULEUR_FOND + "; -fx-padding:28px;");
        root.setPrefWidth(680);

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titreBox = new VBox(2);
        Label titre = new Label("🏋️ Recommandations Coach IA");
        titre.setStyle("-fx-font-size:20px; -fx-font-weight:bold; -fx-text-fill:#a855f7;");
        Label sousTitre = new Label("Analyse de vos " + allActivites.size() + " séances · Ollama " + OLLAMA_MODEL);
        sousTitre.setStyle("-fx-font-size:12px; -fx-text-fill:" + COULEUR_SECOND + ";");
        titreBox.getChildren().addAll(titre, sousTitre);
        header.getChildren().add(titreBox);

        javafx.scene.control.Separator sep = new javafx.scene.control.Separator();
        sep.setStyle("-fx-background-color:#a855f7; -fx-opacity:0.4;");

        Label contenu = new Label(coaching);
        contenu.setWrapText(true);
        contenu.setStyle(
                "-fx-font-size:14px; -fx-text-fill:" + COULEUR_TEXTE + ";" +
                        "-fx-line-spacing:4px; -fx-background-color:" + COULEUR_SURFACE + ";" +
                        "-fx-background-radius:12px; -fx-padding:20px;" +
                        "-fx-border-color:#a855f740; -fx-border-radius:12px; -fx-border-width:1px;");
        contenu.setMaxWidth(640);

        ScrollPane scroll = new ScrollPane(contenu);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(480);
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
}
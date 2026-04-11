package controllers;

import Models.Activite;
import Models.Objectif;
import Services.ServiceActivite;
import Services.ServiceObjectif;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.StringConverter;

import java.sql.SQLDataException;
import java.util.List;

public class ActiviteFrontController {

    // ── Stat labels ────────────────────────────────────────
    @FXML private Label    statTotalLabel;
    @FXML private Label    statCaloriesLabel;
    @FXML private Label    statDureeLabel;

    // ── Card grid (replaces TableView) ─────────────────────
    @FXML private FlowPane cardGrid;
    @FXML private Label    emptyLabel;

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

    private final ServiceActivite service    = new ServiceActivite();
    private final ServiceObjectif objService = new ServiceObjectif();

    // Currently selected activite (for edit)
    private Activite currentEdit = null;

    // ── Lifecycle ──────────────────────────────────────────

    @FXML
    public void initialize() {
        chargerObjectifs();
        charger();
    }

    // ── Data ───────────────────────────────────────────────

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

    // ── Card builder ───────────────────────────────────────

    private void buildCards(List<Activite> list) {
        cardGrid.getChildren().clear();

        if (list.isEmpty()) {
            if (emptyLabel != null) { emptyLabel.setVisible(true); emptyLabel.setManaged(true); }
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

    // ── Helpers ────────────────────────────────────────────

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

    // ── Form handling ──────────────────────────────────────

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

    // ── CRUD ───────────────────────────────────────────────

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

    // ── Validation ─────────────────────────────────────────

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
}
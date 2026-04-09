package controllers;

import Models.Activite;
import Models.Objectif;
import Services.ServiceActivite;
import Services.ServiceObjectif;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.IOException;
import java.sql.SQLDataException;
import java.util.Collections;
import java.util.List;

public class ActiviteController {

    @FXML private VBox     formPane;
    @FXML private HBox     btnAjouterBox;
    @FXML private HBox     btnModifierBox;
    @FXML private TextField typeField;
    @FXML private TextField dureeField;
    @FXML private TextField caloriesField;
    @FXML private TextField intensiteField;
    @FXML private TextField dureeMinField;
    @FXML private TextField dureeMaxField;
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<Objectif>             objectifCombo;
    @FXML private TableView<Activite>            table;
    @FXML private TableColumn<Activite, Integer> colId;
    @FXML private TableColumn<Activite, String>  colType;
    @FXML private TableColumn<Activite, Integer> colDuree;
    @FXML private TableColumn<Activite, Double>  colCalories;
    @FXML private TableColumn<Activite, String>  colDate;
    @FXML private TableColumn<Activite, String>  colIntensite;
    @FXML private TableColumn<Activite, Integer> colObjectif;
    @FXML private TableColumn<Activite, Void>    colActions;
    @FXML private Label msgLabel;

    private final ServiceActivite  service    = new ServiceActivite();
    private final ServiceObjectif  objService = new ServiceObjectif();
    private FilteredList<Activite> filteredData;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(d ->
                new SimpleIntegerProperty(d.getValue().getId()).asObject());
        colType.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getType()));
        colDuree.setCellValueFactory(d ->
                new SimpleIntegerProperty(d.getValue().getDuree()).asObject());
        colCalories.setCellValueFactory(d ->
                new SimpleDoubleProperty(d.getValue().getCaloriesBrulees()).asObject());
        colDate.setCellValueFactory(d ->
                new SimpleStringProperty(
                        d.getValue().getDate() != null
                                ? d.getValue().getDate().toString() : ""));
        colIntensite.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getIntensite()));
        colObjectif.setCellValueFactory(d ->
                new SimpleIntegerProperty(d.getValue().getObjectifId()).asObject());

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit  = new Button("Editer");
            private final Button btnSuppr = new Button("Suppr");
            private final HBox   box      = new HBox(4, btnEdit, btnSuppr);
            {
                btnEdit.getStyleClass().add("btn-warning");
                btnSuppr.getStyleClass().add("btn-danger");
                btnEdit.setPrefWidth(75);  btnEdit.setMinWidth(75);
                btnSuppr.setPrefWidth(75); btnSuppr.setMinWidth(75);
                box.setPrefWidth(160);     box.setMinWidth(160);

                btnEdit.setOnAction(e -> {
                    Activite a = getTableView().getItems().get(getIndex());
                    getTableView().getSelectionModel().select(getIndex());
                    remplirFormulaire(a);
                    setModeEdition(true);
                    showForm(true);
                    msg("Modifiez les champs puis cliquez Modifier", false);
                });

                btnSuppr.setOnAction(e -> {
                    Activite a = getTableView().getItems().get(getIndex());
                    try {
                        service.supprimer(a);
                        msg("Activite supprimee avec succes", false);
                        charger();
                    } catch (SQLDataException ex) {
                        msg("Erreur: " + ex.getMessage(), true);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
                setText(null);
            }
        });

        chargerObjectifs();
        charger();
    }

    // ── Validation ─────────────────────────────────────────

    private boolean validerFormulaire() {
        boolean valide = true;
        resetStyles();

        // Type
        if (typeField.getText() == null || typeField.getText().trim().isEmpty()) {
            setFieldError(typeField, "Le type est obligatoire");
            valide = false;
        } else if (typeField.getText().trim().length() < 2) {
            setFieldError(typeField, "Le type doit contenir au moins 2 caracteres");
            valide = false;
        } else if (typeField.getText().trim().length() > 50) {
            setFieldError(typeField, "Le type ne peut pas depasser 50 caracteres");
            valide = false;
        }

        // Duree
        if (dureeField.getText() == null || dureeField.getText().trim().isEmpty()) {
            setFieldError(dureeField, "La duree est obligatoire");
            valide = false;
        } else {
            try {
                int duree = Integer.parseInt(dureeField.getText().trim());
                if (duree <= 0) {
                    setFieldError(dureeField, "La duree doit etre superieure a 0");
                    valide = false;
                } else if (duree > 1440) {
                    setFieldError(dureeField, "La duree ne peut pas depasser 1440 minutes (24h)");
                    valide = false;
                }
            } catch (NumberFormatException e) {
                setFieldError(dureeField, "La duree doit etre un entier valide");
                valide = false;
            }
        }

        // Calories
        if (caloriesField.getText() == null || caloriesField.getText().trim().isEmpty()) {
            setFieldError(caloriesField, "Les calories sont obligatoires");
            valide = false;
        } else {
            try {
                double cal = Double.parseDouble(caloriesField.getText().trim());
                if (cal < 0) {
                    setFieldError(caloriesField, "Les calories ne peuvent pas etre negatives");
                    valide = false;
                } else if (cal > 10_000) {
                    setFieldError(caloriesField, "Les calories ne peuvent pas depasser 10 000");
                    valide = false;
                }
            } catch (NumberFormatException e) {
                setFieldError(caloriesField, "Les calories doivent etre un nombre valide");
                valide = false;
            }
        }

        // Date
        if (datePicker.getValue() == null) {
            datePicker.setStyle("-fx-border-color: #e05050; -fx-border-radius: 10px; -fx-background-radius: 10px; -fx-border-width: 1px;");
            msg("La date est obligatoire", true);
            valide = false;
        }

        // Intensite
        if (intensiteField.getText() == null || intensiteField.getText().trim().isEmpty()) {
            setFieldError(intensiteField, "L'intensite est obligatoire");
            valide = false;
        } else if (intensiteField.getText().trim().length() < 2) {
            setFieldError(intensiteField, "L'intensite doit contenir au moins 2 caracteres");
            valide = false;
        }

        // Objectif
        if (objectifCombo.getValue() == null) {
            objectifCombo.setStyle("-fx-border-color: #e05050; -fx-border-radius: 10px; -fx-background-radius: 10px; -fx-border-width: 1px;");
            msg("Selectionnez un objectif", true);
            valide = false;
        }

        return valide;
    }

    private void setFieldError(TextField field, String message) {
        field.setStyle(
                "-fx-border-color: #e05050;" +
                        "-fx-border-radius: 10px;" +
                        "-fx-background-radius: 10px;" +
                        "-fx-border-width: 1px;" +
                        "-fx-background-color: #2a1a1a;");
        msg(message, true);
    }

    private void resetStyles() {
        typeField.setStyle("");
        dureeField.setStyle("");
        caloriesField.setStyle("");
        intensiteField.setStyle("");
        datePicker.setStyle("");
        objectifCombo.setStyle("");
        if (msgLabel != null) msgLabel.setText("");
    }

    // ── Chargement ─────────────────────────────────────────

    private void chargerObjectifs() {
        try {
            List<Objectif> objs = objService.recuperer();
            objectifCombo.getItems().setAll(objs);
            objectifCombo.setConverter(new StringConverter<Objectif>() {
                @Override public String toString(Objectif o) {
                    return o == null ? "" : o.getId() + " - " + o.getDescription();
                }
                @Override public Objectif fromString(String s) { return null; }
            });
        } catch (SQLDataException e) {
            msg("Erreur chargement objectifs: " + e.getMessage(), true);
        }
    }

    // ── Navigation ─────────────────────────────────────────

    @FXML
    public void showObjectifs() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/ObjectifView.fxml"));
            Stage stage = (Stage) table.getScene().getWindow();
            stage.setScene(new Scene(root, 1200, 700));
        } catch (IOException e) { msg("Erreur navigation: " + e.getMessage(), true); }
    }

    private void setModeEdition(boolean edition) {
        if (btnAjouterBox != null) {
            btnAjouterBox.setVisible(!edition);
            btnAjouterBox.setManaged(!edition);
        }
        if (btnModifierBox != null) {
            btnModifierBox.setVisible(edition);
            btnModifierBox.setManaged(edition);
        }
    }

    @FXML public void ouvrirFormulaire() {
        viderFormulaire();
        setModeEdition(false);
        showForm(true);
    }

    @FXML public void annuler() {
        viderFormulaire();
        setModeEdition(false);
        showForm(false);
    }

    private void showForm(boolean visible) {
        if (formPane != null) {
            formPane.setVisible(visible);
            formPane.setManaged(visible);
            formPane.setStyle(visible
                    ? "-fx-background-color: #1e3525; -fx-background-radius: 8px; -fx-padding: 16px; -fx-border-color: #2ecc71; -fx-border-radius: 8px; -fx-border-width: 1px;"
                    : "-fx-background-color: #1a2d20; -fx-background-radius: 8px; -fx-padding: 16px;");
        }
    }

    // ── CRUD ───────────────────────────────────────────────

    @FXML
    public void ajouter() {
        if (!validerFormulaire()) return;
        try {
            Activite a = new Activite();
            a.setType(typeField.getText().trim());
            a.setDuree(Integer.parseInt(dureeField.getText().trim()));
            a.setCaloriesBrulees(Double.parseDouble(caloriesField.getText().trim()));
            a.setDate(datePicker.getValue());
            a.setIntensite(intensiteField.getText().trim());
            a.setObjectifId(objectifCombo.getValue().getId());
            service.ajouter(a);
            msg("Activite ajoutee avec succes !", false);
            viderFormulaire();
            showForm(false);
            charger();
        } catch (SQLDataException e) { msg("Erreur BD: " + e.getMessage(), true); }
    }

    @FXML
    public void modifier() {
        Activite sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { msg("Selectionnez une ligne d'abord", true); return; }
        if (!validerFormulaire()) return;
        try {
            sel.setType(typeField.getText().trim());
            sel.setDuree(Integer.parseInt(dureeField.getText().trim()));
            sel.setCaloriesBrulees(Double.parseDouble(caloriesField.getText().trim()));
            sel.setDate(datePicker.getValue());
            sel.setIntensite(intensiteField.getText().trim());
            sel.setObjectifId(objectifCombo.getValue().getId());
            service.modifier(sel);
            msg("Activite modifiee avec succes !", false);
            viderFormulaire();
            setModeEdition(false);
            showForm(false);
            charger();
        } catch (SQLDataException e) { msg("Erreur BD: " + e.getMessage(), true); }
    }

    @FXML
    public void charger() {
        try {
            filteredData = new FilteredList<>(
                    FXCollections.observableArrayList(service.recuperer()));
            table.setItems(filteredData);
        } catch (SQLDataException e) { msg("Erreur BD: " + e.getMessage(), true); }
    }

    // ── Filtre / Tri ────────────────────────────────────────

    @FXML
    public void filtrer() {
        if (filteredData == null) return;
        try {
            String minTxt = dureeMinField.getText().trim();
            String maxTxt = dureeMaxField.getText().trim();
            if (!minTxt.isEmpty() && !maxTxt.isEmpty()) {
                int min = Integer.parseInt(minTxt);
                int max = Integer.parseInt(maxTxt);
                if (min > max) {
                    msg("La duree min ne peut pas etre superieure au max", true);
                    return;
                }
            }
            int min = minTxt.isEmpty() ? 0              : Integer.parseInt(minTxt);
            int max = maxTxt.isEmpty() ? Integer.MAX_VALUE : Integer.parseInt(maxTxt);
            filteredData.setPredicate(a -> a.getDuree() >= min && a.getDuree() <= max);
        } catch (NumberFormatException e) { msg("Duree de filtre invalide", true); }
    }

    @FXML
    public void trierDate() {
        if (filteredData == null) return;
        table.setItems(filteredData.sorted((a, b) -> {
            if (a.getDate() == null) return 1;
            if (b.getDate() == null) return -1;
            return b.getDate().compareTo(a.getDate());
        }));
    }

    @FXML
    public void trierCalories() {
        if (filteredData == null) return;
        table.setItems(filteredData.sorted((a, b) ->
                Double.compare(b.getCaloriesBrulees(), a.getCaloriesBrulees())));
    }

    public List<Activite> listerActivites() {
        try { return service.recuperer(); }
        catch (SQLDataException e) { return Collections.emptyList(); }
    }

    // ── Helpers ────────────────────────────────────────────

    private void remplirFormulaire(Activite a) {
        typeField.setText(a.getType());
        dureeField.setText(String.valueOf(a.getDuree()));
        caloriesField.setText(String.valueOf(a.getCaloriesBrulees()));
        intensiteField.setText(a.getIntensite() != null ? a.getIntensite() : "");
        datePicker.setValue(a.getDate());
        objectifCombo.getItems().stream()
                .filter(o -> o.getId() == a.getObjectifId())
                .findFirst()
                .ifPresent(objectifCombo::setValue);
    }

    private void viderFormulaire() {
        typeField.clear();
        dureeField.clear();
        caloriesField.clear();
        intensiteField.clear();
        datePicker.setValue(null);
        objectifCombo.setValue(null);
        if (dureeMinField != null) dureeMinField.clear();
        if (dureeMaxField != null) dureeMaxField.clear();
        resetStyles();
    }

    private void msg(String m, boolean erreur) {
        if (msgLabel != null) {
            msgLabel.setText(m);
            msgLabel.setStyle(erreur
                    ? "-fx-text-fill: #e05050; -fx-font-size: 12px;"
                    : "-fx-text-fill: #2ecc71; -fx-font-size: 12px;");
        }
    }
}
package controllers;

import Models.Objectif;
import Services.ServiceObjectif;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLDataException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

public class ObjectifController {

    @FXML private VBox     formPane;
    @FXML private HBox     btnAjouterBox;
    @FXML private HBox     btnModifierBox;
    @FXML private TextField descField;
    @FXML private TextField typeField;
    @FXML private TextField valeurCibleField;
    @FXML private TextField statutField;
    @FXML private TextField userIdField;
    @FXML private DatePicker dateDebutPicker;
    @FXML private DatePicker dateFinPicker;
    @FXML private TableView<Objectif>               table;
    @FXML private TableColumn<Objectif, Integer>    colId;
    @FXML private TableColumn<Objectif, String>     colDesc;
    @FXML private TableColumn<Objectif, String>     colType;
    @FXML private TableColumn<Objectif, Integer>    colValeurCible;
    @FXML private TableColumn<Objectif, String>     colDateDebut;
    @FXML private TableColumn<Objectif, String>     colDateFin;
    @FXML private TableColumn<Objectif, String>     colStatut;
    @FXML private TableColumn<Objectif, Void>       colActions;
    @FXML private Label msgLabel;

    private final ServiceObjectif service = new ServiceObjectif();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(d ->
                new SimpleIntegerProperty(d.getValue().getId()).asObject());
        colDesc.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getDescription()));
        colType.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getType()));
        colValeurCible.setCellValueFactory(d ->
                new SimpleIntegerProperty(d.getValue().getValeurCible()).asObject());
        colDateDebut.setCellValueFactory(d ->
                new SimpleStringProperty(
                        d.getValue().getDateDebut() != null
                                ? d.getValue().getDateDebut().toString() : ""));
        colDateFin.setCellValueFactory(d ->
                new SimpleStringProperty(
                        d.getValue().getDateFin() != null
                                ? d.getValue().getDateFin().toString() : ""));
        colStatut.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getStatut()));

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
                    Objectif o = getTableView().getItems().get(getIndex());
                    getTableView().getSelectionModel().select(getIndex());
                    remplirFormulaire(o);
                    setModeEdition(true);
                    showForm(true);
                    msg("Modifiez les champs puis cliquez Modifier", false);
                });

                btnSuppr.setOnAction(e -> {
                    Objectif o = getTableView().getItems().get(getIndex());
                    try {
                        service.supprimer(o);
                        msg("Objectif supprime avec succes", false);
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

        charger();
    }

    // ── Validation ─────────────────────────────────────────

    private boolean validerFormulaire() {
        boolean valide = true;
        resetStyles();

        // Description
        if (descField.getText() == null || descField.getText().trim().isEmpty()) {
            setFieldError(descField, "La description est obligatoire");
            valide = false;
        } else if (descField.getText().trim().length() < 3) {
            setFieldError(descField, "La description doit contenir au moins 3 caracteres");
            valide = false;
        } else if (descField.getText().trim().length() > 200) {
            setFieldError(descField, "La description ne peut pas depasser 200 caracteres");
            valide = false;
        }

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

        // Valeur cible
        if (valeurCibleField.getText() == null || valeurCibleField.getText().trim().isEmpty()) {
            setFieldError(valeurCibleField, "La valeur cible est obligatoire");
            valide = false;
        } else {
            try {
                int val = Integer.parseInt(valeurCibleField.getText().trim());
                if (val <= 0) {
                    setFieldError(valeurCibleField, "La valeur cible doit etre superieure a 0");
                    valide = false;
                } else if (val > 100_000) {
                    setFieldError(valeurCibleField, "La valeur cible ne peut pas depasser 100 000");
                    valide = false;
                }
            } catch (NumberFormatException e) {
                setFieldError(valeurCibleField, "La valeur cible doit etre un entier valide");
                valide = false;
            }
        }

        // Date debut
        if (dateDebutPicker.getValue() == null) {
            dateDebutPicker.setStyle("-fx-border-color: #e05050; -fx-border-radius: 10px; -fx-background-radius: 10px; -fx-border-width: 1px;");
            msg("La date de debut est obligatoire", true);
            valide = false;
        }

        // Date fin
        if (dateFinPicker.getValue() == null) {
            dateFinPicker.setStyle("-fx-border-color: #e05050; -fx-border-radius: 10px; -fx-background-radius: 10px; -fx-border-width: 1px;");
            msg("La date de fin est obligatoire", true);
            valide = false;
        }

        // Coherence dates
        if (dateDebutPicker.getValue() != null && dateFinPicker.getValue() != null) {
            if (dateFinPicker.getValue().isBefore(dateDebutPicker.getValue())) {
                dateFinPicker.setStyle("-fx-border-color: #e05050; -fx-border-radius: 10px; -fx-background-radius: 10px; -fx-border-width: 1px;");
                msg("La date de fin doit etre apres la date de debut", true);
                valide = false;
            }
        }

        // Statut
        if (statutField.getText() == null || statutField.getText().trim().isEmpty()) {
            setFieldError(statutField, "Le statut est obligatoire");
            valide = false;
        } else if (statutField.getText().trim().length() < 2) {
            setFieldError(statutField, "Le statut doit contenir au moins 2 caracteres");
            valide = false;
        }

        // User ID
        if (userIdField.getText() == null || userIdField.getText().trim().isEmpty()) {
            setFieldError(userIdField, "L'ID utilisateur est obligatoire");
            valide = false;
        } else {
            try {
                int uid = Integer.parseInt(userIdField.getText().trim());
                if (uid <= 0) {
                    setFieldError(userIdField, "L'ID utilisateur doit etre superieur a 0");
                    valide = false;
                }
            } catch (NumberFormatException e) {
                setFieldError(userIdField, "L'ID utilisateur doit etre un entier valide");
                valide = false;
            }
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
        descField.setStyle("");
        typeField.setStyle("");
        valeurCibleField.setStyle("");
        statutField.setStyle("");
        userIdField.setStyle("");
        dateDebutPicker.setStyle("");
        dateFinPicker.setStyle("");
        if (msgLabel != null) msgLabel.setText("");
    }

    // ── Navigation ─────────────────────────────────────────

    @FXML
    public void showActivites() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/ActiviteView.fxml"));
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
            Objectif o = new Objectif();
            o.setDescription(descField.getText().trim());
            o.setType(typeField.getText().trim());
            o.setValeurCible(Integer.parseInt(valeurCibleField.getText().trim()));
            o.setDateDebut(dateDebutPicker.getValue());
            o.setDateFin(dateFinPicker.getValue());
            o.setStatut(statutField.getText().trim());
            o.setUserId(Integer.parseInt(userIdField.getText().trim()));
            service.ajouter(o);
            msg("Objectif ajoute avec succes !", false);
            viderFormulaire();
            showForm(false);
            charger();
        } catch (SQLDataException e) { msg("Erreur BD: " + e.getMessage(), true); }
    }

    @FXML
    public void modifier() {
        Objectif sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { msg("Selectionnez une ligne d'abord", true); return; }
        if (!validerFormulaire()) return;
        try {
            sel.setDescription(descField.getText().trim());
            sel.setType(typeField.getText().trim());
            sel.setValeurCible(Integer.parseInt(valeurCibleField.getText().trim()));
            sel.setDateDebut(dateDebutPicker.getValue());
            sel.setDateFin(dateFinPicker.getValue());
            sel.setStatut(statutField.getText().trim());
            sel.setUserId(Integer.parseInt(userIdField.getText().trim()));
            service.modifier(sel);
            msg("Objectif modifie avec succes !", false);
            viderFormulaire();
            setModeEdition(false);
            showForm(false);
            charger();
        } catch (SQLDataException e) { msg("Erreur BD: " + e.getMessage(), true); }
    }

    @FXML
    public void charger() {
        try {
            table.setItems(FXCollections.observableArrayList(service.recuperer()));
        } catch (SQLDataException e) { msg("Erreur BD: " + e.getMessage(), true); }
    }

    public List<Objectif> listerObjectifs() {
        try { return service.recuperer(); }
        catch (SQLDataException e) { return Collections.emptyList(); }
    }

    // ── Helpers ────────────────────────────────────────────

    private void remplirFormulaire(Objectif o) {
        descField.setText(o.getDescription());
        typeField.setText(o.getType());
        valeurCibleField.setText(String.valueOf(o.getValeurCible()));
        statutField.setText(o.getStatut() != null ? o.getStatut() : "");
        userIdField.setText(String.valueOf(o.getUserId()));
        dateDebutPicker.setValue(o.getDateDebut());
        dateFinPicker.setValue(o.getDateFin());
    }

    private void viderFormulaire() {
        descField.clear();
        typeField.clear();
        valeurCibleField.clear();
        statutField.clear();
        userIdField.clear();
        dateDebutPicker.setValue(null);
        dateFinPicker.setValue(null);
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
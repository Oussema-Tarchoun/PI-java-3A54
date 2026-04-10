package Controller.back;

import Models.Categorie;
import Services.CategorieService;
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
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

public class CategorieController {

    @FXML private VBox formPane;
    @FXML private HBox btnAjouterBox;
    @FXML private HBox btnModifierBox;
    @FXML private TextField nomField, descField, userField, budgetField;
    @FXML private DatePicker datePicker;
    @FXML private TableView<Categorie> table;
    @FXML private TableColumn<Categorie, Integer> colId, colUser;
    @FXML private TableColumn<Categorie, String>  colNom, colDesc, colDate;
    @FXML private TableColumn<Categorie, Double>  colBudget;
    @FXML private TableColumn<Categorie, Void>    colActions;
    @FXML private Label msgLabel;

    private final CategorieService service = new CategorieService();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(d ->
                new SimpleIntegerProperty(d.getValue().getIdCategorie()).asObject());
        colNom.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getNomCategorie()));
        colDesc.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getDescription()));
        colUser.setCellValueFactory(d ->
                new SimpleIntegerProperty(d.getValue().getIdUser()).asObject());
        colBudget.setCellValueFactory(d ->
                new SimpleDoubleProperty(d.getValue().getBudgetMax()).asObject());
        colDate.setCellValueFactory(d ->
                new SimpleStringProperty(
                        d.getValue().getDateCreation() != null
                                ? d.getValue().getDateCreation().toString() : ""));

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
                    Categorie c = getTableView().getItems().get(getIndex());
                    getTableView().getSelectionModel().select(getIndex());
                    remplirFormulaire(c);
                    setModeEdition(true);
                    showForm(true);
                    msg("Modifiez les champs puis cliquez Modifier", false);
                });

                btnSuppr.setOnAction(e -> {
                    Categorie c = getTableView().getItems().get(getIndex());
                    try {
                        service.supprimer(c.getIdCategorie());
                        msg("Supprimee avec succes", false);
                        charger();
                    } catch (SQLException ex) {
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

    // ── Validation à la soumission uniquement ──────────────

    private boolean validerFormulaire() {
        boolean valide = true;
        resetStyles();

        // Nom
        if (nomField.getText() == null || nomField.getText().trim().isEmpty()) {
            setFieldError(nomField, "Le nom est obligatoire");
            valide = false;
        } else if (nomField.getText().trim().length() < 2) {
            setFieldError(nomField, "Le nom doit contenir au moins 2 caracteres");
            valide = false;
        } else if (nomField.getText().trim().length() > 50) {
            setFieldError(nomField, "Le nom ne peut pas depasser 50 caracteres");
            valide = false;
        }

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

        // ID User
        if (userField.getText() == null || userField.getText().trim().isEmpty()) {
            setFieldError(userField, "L'ID utilisateur est obligatoire");
            valide = false;
        } else {
            try {
                int userId = Integer.parseInt(userField.getText().trim());
                if (userId <= 0) {
                    setFieldError(userField, "L'ID utilisateur doit etre superieur a 0");
                    valide = false;
                }
            } catch (NumberFormatException e) {
                setFieldError(userField, "L'ID utilisateur doit etre un nombre entier");
                valide = false;
            }
        }

        // Budget
        if (budgetField.getText() == null || budgetField.getText().trim().isEmpty()) {
            setFieldError(budgetField, "Le budget est obligatoire");
            valide = false;
        } else {
            try {
                double budget = Double.parseDouble(budgetField.getText().trim());
                if (budget < 0) {
                    setFieldError(budgetField, "Le budget ne peut pas etre negatif");
                    valide = false;
                } else if (budget > 10_000_000) {
                    setFieldError(budgetField, "Le budget ne peut pas depasser 10 000 000");
                    valide = false;
                }
            } catch (NumberFormatException e) {
                setFieldError(budgetField, "Le budget doit etre un nombre valide");
                valide = false;
            }
        }

        // Date
        if (datePicker.getValue() == null) {
            datePicker.setStyle("-fx-border-color: #e05050; -fx-border-radius: 10px; -fx-background-radius: 10px; -fx-border-width: 1px;");
            msg("La date est obligatoire", true);
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
        nomField.setStyle("");
        descField.setStyle("");
        userField.setStyle("");
        budgetField.setStyle("");
        datePicker.setStyle("");
        if (msgLabel != null) msgLabel.setText("");
    }

    // ── Navigation ─────────────────────────────────────────

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

    @FXML
    public void showDepenses() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/viewsBack/DepenseView.fxml"));
            Stage stage = (Stage) table.getScene().getWindow();
            stage.setScene(new Scene(root, 1280, 800));
            stage.setMaximized(true);
        } catch (IOException e) {
            msg("Erreur navigation: " + e.getMessage(), true);
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
            if (visible) {
                formPane.setStyle(
                        "-fx-background-color: #1e3525;" +
                                "-fx-background-radius: 8px;" +
                                "-fx-padding: 16px;" +
                                "-fx-border-color: #2ecc71;" +
                                "-fx-border-radius: 8px;" +
                                "-fx-border-width: 1px;");
            } else {
                formPane.setStyle(
                        "-fx-background-color: #1a2d20;" +
                                "-fx-background-radius: 8px;" +
                                "-fx-padding: 16px;");
            }
        }
    }

    @FXML
    public void ajouter() {
        if (!validerFormulaire()) return;
        try {
            Categorie c = new Categorie(
                    nomField.getText().trim(),
                    descField.getText().trim(),
                    Integer.parseInt(userField.getText().trim()),
                    Double.parseDouble(budgetField.getText().trim()),
                    java.sql.Date.valueOf(datePicker.getValue()));
            service.ajouter(c);
            msg("Categorie ajoutee avec succes !", false);
            viderFormulaire();
            showForm(false);
            charger();
        } catch (SQLException e) { msg("Erreur BD: " + e.getMessage(), true); }
    }

    @FXML
    public void modifier() {
        Categorie sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { msg("Selectionnez une ligne d'abord", true); return; }
        if (!validerFormulaire()) return;
        try {
            Categorie c = new Categorie(
                    sel.getIdCategorie(),
                    nomField.getText().trim(),
                    descField.getText().trim(),
                    Integer.parseInt(userField.getText().trim()),
                    Double.parseDouble(budgetField.getText().trim()),
                    java.sql.Date.valueOf(datePicker.getValue()));
            service.modifier(c);
            msg("Categorie modifiee avec succes !", false);
            viderFormulaire();
            setModeEdition(false);
            showForm(false);
            charger();
        } catch (SQLException e) { msg("Erreur BD: " + e.getMessage(), true); }
    }

    @FXML
    public void supprimer() {
        Categorie sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { msg("Selectionnez une ligne d'abord", true); return; }
        try {
            service.supprimer(sel.getIdCategorie());
            msg("Supprimee avec succes", false);
            charger();
        } catch (SQLException e) { msg("Erreur BD: " + e.getMessage(), true); }
    }

    @FXML
    public void charger() {
        try {
            table.setItems(FXCollections.observableArrayList(service.afficher()));
        } catch (SQLException e) { msg("Erreur BD: " + e.getMessage(), true); }
    }

    public List<Categorie> listerCategories() {
        try { return service.afficher(); }
        catch (SQLException e) { return Collections.emptyList(); }
    }

    public Categorie getCategorieById(int id) {
        try { return service.getById(id); }
        catch (SQLException e) { msg("Erreur: " + e.getMessage(), true); return null; }
    }

    private void remplirFormulaire(Categorie c) {
        nomField.setText(c.getNomCategorie());
        descField.setText(c.getDescription());
        userField.setText(String.valueOf(c.getIdUser()));
        budgetField.setText(String.valueOf(c.getBudgetMax()));
        if (c.getDateCreation() != null) {
            if (c.getDateCreation() instanceof java.sql.Date) {
                datePicker.setValue(((java.sql.Date) c.getDateCreation()).toLocalDate());
            } else {
                datePicker.setValue(c.getDateCreation().toInstant()
                        .atZone(java.time.ZoneId.systemDefault()).toLocalDate());
            }
        } else {
            datePicker.setValue(null);
        }
    }

    private void viderFormulaire() {
        nomField.clear();
        descField.clear();
        userField.clear();
        budgetField.clear();
        datePicker.setValue(null);
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
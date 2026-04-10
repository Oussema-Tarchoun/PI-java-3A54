package Controller;

import Models.Categorie;
import Services.CategorieService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

public class CategorieController {

    @FXML private VBox      formPane;
    @FXML private HBox      btnAjouterBox;
    @FXML private HBox      btnModifierBox;
    @FXML private TextField nomField, descField, userField, budgetField;
    @FXML private DatePicker datePicker;
    @FXML private FlowPane  cardsPane;
    @FXML private Label     msgLabel;

    private final CategorieService service = new CategorieService();
    private ObservableList<Categorie> allCategories = FXCollections.observableArrayList();
    private Categorie selectedCategorie = null;

    @FXML
    public void initialize() {
        charger();
    }

    // ── Cartes ───────────────────────────────────────────

    private void afficherCartes(List<Categorie> categories) {
        cardsPane.getChildren().clear();
        if (categories.isEmpty()) {
            Label empty = new Label("Aucune categorie trouvee");
            empty.setStyle("-fx-text-fill:#64748b; -fx-font-size:13px;");
            cardsPane.getChildren().add(empty);
            return;
        }
        for (Categorie c : categories) {
            cardsPane.getChildren().add(creerCarte(c));
        }
    }

    private VBox creerCarte(Categorie c) {
        VBox card = new VBox(12);
        card.setPrefWidth(340);
        card.setMaxWidth(340);
        card.setStyle(
                "-fx-background-color:#161b2e;" +
                        "-fx-background-radius:14px;" +
                        "-fx-border-color:#2a3142;" +
                        "-fx-border-radius:14px;" +
                        "-fx-border-width:1px;" +
                        "-fx-padding:20px;" +
                        "-fx-cursor:hand;"
        );

        // Header
        HBox header = new HBox(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        javafx.scene.layout.StackPane icon = new javafx.scene.layout.StackPane();
        icon.setPrefSize(40, 40);
        icon.setMaxSize(40, 40);
        icon.setStyle("-fx-background-color:rgba(0,212,255,0.12); -fx-background-radius:10px;");
        Label iconLabel = new Label("🏷");
        iconLabel.setStyle("-fx-font-size:18px;");
        icon.getChildren().add(iconLabel);

        VBox nameBox = new VBox(3);
        Label nom = new Label(c.getNomCategorie());
        nom.setStyle("-fx-font-size:16px; -fx-font-weight:bold; -fx-text-fill:#ffffff;");
        Label idLabel = new Label("ID: " + c.getIdCategorie());
        idLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#64748b;");
        nameBox.getChildren().addAll(nom, idLabel);
        header.getChildren().addAll(icon, nameBox);

        Label desc = new Label(c.getDescription() != null ? c.getDescription() : "-");
        desc.setStyle("-fx-font-size:14px; -fx-text-fill:#94a3b8;");
        desc.setWrapText(true);
        desc.setMaxWidth(300);

        Label budget = new Label(String.format("💰  Budget: %.2f TND", c.getBudgetMax()));
        budget.setStyle("-fx-font-size:14px; -fx-text-fill:#00d4ff; -fx-font-weight:bold;");

        String dateStr = c.getDateCreation() != null ? c.getDateCreation().toString() : "-";
        Label date = new Label("📅  " + dateStr);
        date.setStyle("-fx-font-size:14px; -fx-text-fill:#64748b;");

        javafx.scene.control.Separator sep = new javafx.scene.control.Separator();

        HBox actions = new HBox(10);
        Button btnEdit  = new Button("Editer");
        Button btnSuppr = new Button("Suppr");

        btnEdit.setStyle(
                "-fx-background-color:rgba(245,158,11,0.15);" +
                        "-fx-text-fill:#f59e0b;" +
                        "-fx-background-radius:8px;" +
                        "-fx-padding:9 0 9 0;" +
                        "-fx-cursor:hand; -fx-font-size:14px; -fx-font-weight:bold;");
        btnSuppr.setStyle(
                "-fx-background-color:rgba(239,68,68,0.15);" +
                        "-fx-text-fill:#ef4444;" +
                        "-fx-background-radius:8px;" +
                        "-fx-padding:9 0 9 0;" +
                        "-fx-cursor:hand; -fx-font-size:14px; -fx-font-weight:bold;");

        btnEdit.setPrefWidth(140);
        btnSuppr.setPrefWidth(140);

        btnEdit.setOnAction(e -> {
            selectedCategorie = c;
            remplirFormulaire(c);
            setModeEdition(true);
            showForm(true);
            msg("Modifiez les champs puis cliquez Modifier", false);
        });

        btnSuppr.setOnAction(e -> {
            try {
                service.supprimer(c.getIdCategorie());
                msg("Supprimee avec succes", false);
                charger();
            } catch (SQLException ex) {
                msg("Erreur: " + ex.getMessage(), true);
            }
        });

        actions.getChildren().addAll(btnEdit, btnSuppr);
        card.getChildren().addAll(header, desc, budget, date, sep, actions);

        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color:#1e2538;" +
                        "-fx-background-radius:14px;" +
                        "-fx-border-color:#00d4ff;" +
                        "-fx-border-radius:14px;" +
                        "-fx-border-width:1px;" +
                        "-fx-padding:20px;" +
                        "-fx-cursor:hand;"
        ));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color:#161b2e;" +
                        "-fx-background-radius:14px;" +
                        "-fx-border-color:#2a3142;" +
                        "-fx-border-radius:14px;" +
                        "-fx-border-width:1px;" +
                        "-fx-padding:20px;" +
                        "-fx-cursor:hand;"
        ));
        return card;
    }

    // ── Validation ───────────────────────────────────────

    private boolean validerFormulaire() {
        boolean valide = true;
        resetStyles();

        if (nomField.getText() == null || nomField.getText().trim().isEmpty()) {
            setFieldError(nomField, "Le nom est obligatoire"); valide = false;
        } else if (nomField.getText().trim().length() < 2) {
            setFieldError(nomField, "Le nom doit contenir au moins 2 caracteres"); valide = false;
        } else if (nomField.getText().trim().length() > 50) {
            setFieldError(nomField, "Le nom ne peut pas depasser 50 caracteres"); valide = false;
        }

        if (descField.getText() == null || descField.getText().trim().isEmpty()) {
            setFieldError(descField, "La description est obligatoire"); valide = false;
        } else if (descField.getText().trim().length() < 3) {
            setFieldError(descField, "La description doit contenir au moins 3 caracteres"); valide = false;
        }

        if (userField.getText() == null || userField.getText().trim().isEmpty()) {
            setFieldError(userField, "L'ID utilisateur est obligatoire"); valide = false;
        } else {
            try {
                int userId = Integer.parseInt(userField.getText().trim());
                if (userId <= 0) { setFieldError(userField, "L'ID doit etre superieur a 0"); valide = false; }
            } catch (NumberFormatException e) {
                setFieldError(userField, "L'ID doit etre un nombre entier"); valide = false;
            }
        }

        if (budgetField.getText() == null || budgetField.getText().trim().isEmpty()) {
            setFieldError(budgetField, "Le budget est obligatoire"); valide = false;
        } else {
            try {
                double budget = Double.parseDouble(budgetField.getText().trim());
                if (budget < 0) { setFieldError(budgetField, "Le budget ne peut pas etre negatif"); valide = false; }
                else if (budget > 10_000_000) { setFieldError(budgetField, "Budget trop eleve"); valide = false; }
            } catch (NumberFormatException e) {
                setFieldError(budgetField, "Le budget doit etre un nombre valide"); valide = false;
            }
        }

        if (datePicker.getValue() == null) {
            datePicker.setStyle("-fx-border-color:#ef4444; -fx-border-radius:6px; -fx-border-width:1px;");
            msg("La date est obligatoire", true); valide = false;
        }

        return valide;
    }

    private void setFieldError(TextField field, String message) {
        field.setStyle(
                "-fx-border-color:#ef4444; -fx-border-radius:6px; -fx-border-width:1px;" +
                        "-fx-background-color:#1a1020; -fx-text-fill:#ffffff;" +
                        "-fx-background-radius:6px; -fx-padding:7 10 7 10; -fx-font-size:12px;");
        msg(message, true);
    }

    private void resetStyles() {
        String base = "-fx-background-color:#111827; -fx-text-fill:#ffffff;" +
                "-fx-background-radius:6px; -fx-border-color:#2a3142;" +
                "-fx-border-radius:6px; -fx-border-width:1px;" +
                "-fx-padding:7 10 7 10; -fx-font-size:12px;";
        nomField.setStyle(base);
        descField.setStyle(base);
        userField.setStyle(base);
        budgetField.setStyle(base);
        datePicker.setStyle("-fx-background-color:#111827; -fx-font-size:12px;");
        if (msgLabel != null) msgLabel.setText("");
    }

    // ── Mode édition ─────────────────────────────────────

    private void setModeEdition(boolean edition) {
        if (btnAjouterBox != null) { btnAjouterBox.setVisible(!edition); btnAjouterBox.setManaged(!edition); }
        if (btnModifierBox != null) { btnModifierBox.setVisible(edition); btnModifierBox.setManaged(edition); }
    }

    @FXML
    public void showDepenses() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/ViewDepense.fxml"));
            Stage stage = (Stage) cardsPane.getScene().getWindow();
            stage.setScene(new Scene(root, 1200, 750));
        } catch (IOException e) { msg("Erreur navigation: " + e.getMessage(), true); }
    }

    @FXML public void ouvrirFormulaire() { viderFormulaire(); setModeEdition(false); showForm(true); }
    @FXML public void annuler()          { viderFormulaire(); setModeEdition(false); showForm(false); }

    private void showForm(boolean visible) {
        if (formPane == null) return;
        formPane.setVisible(visible);
        formPane.setManaged(visible);
        formPane.setStyle(visible
                ? "-fx-background-color:#161b2e; -fx-background-radius:10px; -fx-padding:14px;" +
                "-fx-border-color:#00d4ff; -fx-border-radius:10px; -fx-border-width:1px;"
                : "-fx-background-color:#161b2e; -fx-background-radius:10px; -fx-padding:14px;" +
                "-fx-border-color:#2a3142; -fx-border-radius:10px; -fx-border-width:1px;");
    }

    @FXML
    public void ajouter() {
        if (!validerFormulaire()) return;
        try {
            Categorie c = new Categorie(
                    nomField.getText().trim(), descField.getText().trim(),
                    Integer.parseInt(userField.getText().trim()),
                    Double.parseDouble(budgetField.getText().trim()),
                    java.sql.Date.valueOf(datePicker.getValue()));
            service.ajouter(c);
            msg("Categorie ajoutee avec succes !", false);
            viderFormulaire(); showForm(false); charger();
        } catch (SQLException e) { msg("Erreur BD: " + e.getMessage(), true); }
    }

    @FXML
    public void modifier() {
        if (selectedCategorie == null) { msg("Selectionnez une categorie d'abord", true); return; }
        if (!validerFormulaire()) return;
        try {
            Categorie c = new Categorie(
                    selectedCategorie.getIdCategorie(),
                    nomField.getText().trim(), descField.getText().trim(),
                    Integer.parseInt(userField.getText().trim()),
                    Double.parseDouble(budgetField.getText().trim()),
                    java.sql.Date.valueOf(datePicker.getValue()));
            service.modifier(c);
            msg("Categorie modifiee avec succes !", false);
            viderFormulaire(); setModeEdition(false); showForm(false);
            selectedCategorie = null; charger();
        } catch (SQLException e) { msg("Erreur BD: " + e.getMessage(), true); }
    }

    @FXML
    public void charger() {
        try {
            allCategories = FXCollections.observableArrayList(service.afficher());
            afficherCartes(allCategories);
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
        } else { datePicker.setValue(null); }
    }

    private void viderFormulaire() {
        nomField.clear(); descField.clear();
        userField.clear(); budgetField.clear();
        datePicker.setValue(null);
        resetStyles();
        selectedCategorie = null;
    }

    private void msg(String m, boolean erreur) {
        if (msgLabel == null) return;
        msgLabel.setText(m);
        msgLabel.setStyle(erreur
                ? "-fx-text-fill:#ef4444; -fx-font-size:11px;"
                : "-fx-text-fill:#00d4ff; -fx-font-size:11px;");
    }
}
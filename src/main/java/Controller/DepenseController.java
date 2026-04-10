package Controller;

import Models.Categorie;
import Models.Depense;
import Services.CategorieService;
import Services.DepenseService;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

public class DepenseController {

    @FXML private VBox      formPane;
    @FXML private HBox      btnAjouterBox;
    @FXML private HBox      btnModifierBox;
    @FXML private TextField descField, montantField, statutField;
    @FXML private TextField montantMinField, montantMaxField;
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<Categorie> catCombo;
    @FXML private FlowPane  cardsPane;
    @FXML private Label     msgLabel;

    private final DepenseService   service    = new DepenseService();
    private final CategorieService catService = new CategorieService();
    private ObservableList<Depense> allDepenses = FXCollections.observableArrayList();
    private boolean modeEdition = false;
    private Depense selectedDepense = null;

    @FXML
    public void initialize() {
        chargerCategories();
        charger();
    }

    // ── Cartes ───────────────────────────────────────────

    private void afficherCartes(List<Depense> depenses) {
        cardsPane.getChildren().clear();
        if (depenses.isEmpty()) {
            Label empty = new Label("Aucune depense trouvee");
            empty.setStyle("-fx-text-fill:#64748b; -fx-font-size:13px;");
            cardsPane.getChildren().add(empty);
            return;
        }
        for (Depense d : depenses) {
            cardsPane.getChildren().add(creerCarte(d));
        }
    }

    private VBox creerCarte(Depense d) {
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
        HBox header = new HBox();
        Label desc = new Label(d.getDescription());
        desc.setStyle("-fx-font-size:16px; -fx-font-weight:bold; -fx-text-fill:#ffffff;");
        desc.setMaxWidth(190);
        HBox.setHgrow(desc, javafx.scene.layout.Priority.ALWAYS);
        Label montant = new Label(String.format("%.2f TND", d.getMontant()));
        montant.setStyle("-fx-font-size:16px; -fx-font-weight:bold; -fx-text-fill:#00d4ff;");
        header.getChildren().addAll(desc, montant);

        String dateStr = d.getDateDepense() != null ? d.getDateDepense().toString() : "-";
        Label date = new Label("📅  " + dateStr);
        date.setStyle("-fx-font-size:14px; -fx-text-fill:#94a3b8;");

        String statut = d.getStatut() != null ? d.getStatut() : "-";
        Label statutLabel = new Label(statut);
        statutLabel.setStyle(
                "-fx-font-size:13px; -fx-font-weight:bold;" +
                        "-fx-background-color:rgba(0,212,255,0.12);" +
                        "-fx-text-fill:#00d4ff;" +
                        "-fx-background-radius:6px;" +
                        "-fx-padding:5 12 5 12;"
        );

        Label cat = new Label("🏷  Cat. " + d.getIdCategorie());
        cat.setStyle("-fx-font-size:14px; -fx-text-fill:#64748b;");

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
            selectedDepense = d;
            remplirFormulaire(d);
            setModeEdition(true);
            showForm(true);
            msg("Modifiez les champs puis cliquez Modifier", false);
        });

        btnSuppr.setOnAction(e -> {
            try {
                service.supprimer(d.getIdDepense());
                msg("Supprimee avec succes", false);
                charger();
            } catch (SQLException ex) {
                msg("Erreur: " + ex.getMessage(), true);
            }
        });

        actions.getChildren().addAll(btnEdit, btnSuppr);
        card.getChildren().addAll(header, date, statutLabel, cat, sep, actions);

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

        if (descField.getText() == null || descField.getText().trim().isEmpty()) {
            setFieldError(descField, "La description est obligatoire"); valide = false;
        } else if (descField.getText().trim().length() < 3) {
            setFieldError(descField, "La description doit contenir au moins 3 caracteres"); valide = false;
        } else if (descField.getText().trim().length() > 100) {
            setFieldError(descField, "La description ne peut pas depasser 100 caracteres"); valide = false;
        }

        if (montantField.getText() == null || montantField.getText().trim().isEmpty()) {
            setFieldError(montantField, "Le montant est obligatoire"); valide = false;
        } else {
            try {
                double val = Double.parseDouble(montantField.getText().trim());
                if (val <= 0) { setFieldError(montantField, "Le montant doit etre superieur a 0"); valide = false; }
                else if (val > 1_000_000) { setFieldError(montantField, "Le montant ne peut pas depasser 1 000 000"); valide = false; }
            } catch (NumberFormatException e) {
                setFieldError(montantField, "Le montant doit etre un nombre valide"); valide = false;
            }
        }

        if (datePicker.getValue() == null) {
            datePicker.setStyle("-fx-border-color:#ef4444; -fx-border-radius:6px; -fx-border-width:1px;");
            msg("La date est obligatoire", true); valide = false;
        }

        if (statutField.getText() == null || statutField.getText().trim().isEmpty()) {
            setFieldError(statutField, "Le statut est obligatoire"); valide = false;
        } else if (statutField.getText().trim().length() < 2) {
            setFieldError(statutField, "Le statut doit contenir au moins 2 caracteres"); valide = false;
        }

        if (catCombo.getValue() == null) {
            catCombo.setStyle("-fx-border-color:#ef4444; -fx-border-radius:6px; -fx-border-width:1px;");
            msg("Selectionnez une categorie", true); valide = false;
        }

        return valide;
    }

    private boolean verifierBudgetMax(double montant) {
        Categorie cat = catCombo.getValue();
        if (cat != null && montant > cat.getBudgetMax()) {
            setFieldError(montantField, "Le montant depasse le budget max (" + cat.getBudgetMax() + " TND)");
            return false;
        }
        return true;
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
        descField.setStyle(base);
        montantField.setStyle(base);
        statutField.setStyle(base);
        datePicker.setStyle("-fx-background-color:#111827; -fx-font-size:12px;");
        catCombo.setStyle("-fx-background-color:#111827; -fx-font-size:12px;");
        if (msgLabel != null) msgLabel.setText("");
    }

    // ── Chargement ───────────────────────────────────────

    private void chargerCategories() {
        try {
            List<Categorie> cats = catService.afficher();
            catCombo.getItems().setAll(cats);
            catCombo.setConverter(new StringConverter<Categorie>() {
                @Override public String toString(Categorie c) {
                    return c == null ? "" : c.getIdCategorie() + " - " + c.getNomCategorie();
                }
                @Override public Categorie fromString(String s) { return null; }
            });
        } catch (SQLException e) { msg("Erreur categories: " + e.getMessage(), true); }
    }

    private void setModeEdition(boolean edition) {
        modeEdition = edition;
        if (btnAjouterBox != null) { btnAjouterBox.setVisible(!edition); btnAjouterBox.setManaged(!edition); }
        if (btnModifierBox != null) { btnModifierBox.setVisible(edition); btnModifierBox.setManaged(edition); }
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
        double montant = Double.parseDouble(montantField.getText().trim());
        if (!verifierBudgetMax(montant)) return;
        try {
            Depense d = new Depense(
                    descField.getText().trim(), montant,
                    java.sql.Date.valueOf(datePicker.getValue()),
                    statutField.getText().trim(),
                    catCombo.getValue().getIdCategorie());
            service.ajouter(d);
            msg("Depense ajoutee avec succes !", false);
            viderFormulaire();
            showForm(false);
            charger();
        } catch (SQLException e) { msg("Erreur BD: " + e.getMessage(), true); }
    }

    @FXML
    public void modifier() {
        if (selectedDepense == null) { msg("Selectionnez une depense d'abord", true); return; }
        if (!validerFormulaire()) return;
        double montant = Double.parseDouble(montantField.getText().trim());
        if (!verifierBudgetMax(montant)) return;
        try {
            Depense d = new Depense(
                    selectedDepense.getIdDepense(),
                    descField.getText().trim(), montant,
                    java.sql.Date.valueOf(datePicker.getValue()),
                    statutField.getText().trim(),
                    catCombo.getValue().getIdCategorie());
            service.modifier(d);
            msg("Depense modifiee avec succes !", false);
            viderFormulaire();
            setModeEdition(false);
            showForm(false);
            selectedDepense = null;
            charger();
        } catch (SQLException e) { msg("Erreur BD: " + e.getMessage(), true); }
    }

    @FXML
    public void charger() {
        try {
            allDepenses = FXCollections.observableArrayList(service.afficher());
            afficherCartes(allDepenses);
        } catch (SQLException e) { msg("Erreur BD: " + e.getMessage(), true); }
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
            List<Depense> filtered = allDepenses.filtered(d -> d.getMontant() >= min && d.getMontant() <= max);
            afficherCartes(filtered);
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
        statutField.setText(d.getStatut() != null ? d.getStatut() : "");
        catCombo.getItems().stream()
                .filter(c -> c.getIdCategorie() == d.getIdCategorie())
                .findFirst().ifPresent(catCombo::setValue);
        if (d.getDateDepense() != null) {
            if (d.getDateDepense() instanceof java.sql.Date) {
                datePicker.setValue(((java.sql.Date) d.getDateDepense()).toLocalDate());
            } else {
                datePicker.setValue(d.getDateDepense().toInstant()
                        .atZone(java.time.ZoneId.systemDefault()).toLocalDate());
            }
        } else { datePicker.setValue(null); }
    }

    private void viderFormulaire() {
        descField.clear(); montantField.clear(); statutField.clear();
        catCombo.setValue(null); datePicker.setValue(null);
        if (montantMinField != null) montantMinField.clear();
        if (montantMaxField != null) montantMaxField.clear();
        resetStyles();
        selectedDepense = null;
    }

    private void msg(String m, boolean erreur) {
        if (msgLabel == null) return;
        msgLabel.setText(m);
        msgLabel.setStyle(erreur
                ? "-fx-text-fill:#ef4444; -fx-font-size:11px;"
                : "-fx-text-fill:#00d4ff; -fx-font-size:11px;");
    }
}
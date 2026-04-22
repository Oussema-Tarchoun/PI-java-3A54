package Controller.back;

import Models.Categorie;
import Models.Depense;
import Services.CategorieService;
import Services.DepenseService;
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
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

public class DepenseController {

    @FXML private VBox formPane;
    @FXML private HBox btnAjouterBox;
    @FXML private HBox btnModifierBox;
    @FXML private TextField descField, montantField;
    @FXML private TextField montantMinField, montantMaxField;
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<String>    statutCombo;  // ✅ remplace statutField
    @FXML private ComboBox<Categorie> catCombo;
    @FXML private TableView<Depense> table;
    @FXML private TableColumn<Depense, Integer> colId, colCat;
    @FXML private TableColumn<Depense, String>  colDesc, colStatut, colDate;
    @FXML private TableColumn<Depense, Double>  colMontant;
    @FXML private TableColumn<Depense, Void>    colActions;
    @FXML private Label msgLabel;

    private static final String STATUT_PAYE     = "Payé";
    private static final String STATUT_ATTENTE  = "En attente";
    private static final String STATUT_NON_PAYE = "Non payé";

    private final DepenseService   service    = new DepenseService();
    private final CategorieService catService = new CategorieService();
    private FilteredList<Depense>  filteredData;
    private boolean modeEdition = false;

    @FXML
    public void initialize() {
        // ✅ Options statut
        statutCombo.setItems(FXCollections.observableArrayList(
                STATUT_PAYE, STATUT_ATTENTE, STATUT_NON_PAYE
        ));

        colId.setCellValueFactory(d ->
                new SimpleIntegerProperty(d.getValue().getIdDepense()).asObject());
        colDesc.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getDescription()));
        colMontant.setCellValueFactory(d ->
                new SimpleDoubleProperty(d.getValue().getMontant()).asObject());
        colDate.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getDateDepense() != null
                        ? d.getValue().getDateDepense().toString() : ""));
        // ✅ Affiche le statut normalisé dans le tableau
        colStatut.setCellValueFactory(d ->
                new SimpleStringProperty(normaliserStatut(d.getValue().getStatut())));
        colCat.setCellValueFactory(d ->
                new SimpleIntegerProperty(d.getValue().getIdCategorie()).asObject());

        // ✅ Cellule statut colorée dans le tableau
        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                if (item.equals(STATUT_PAYE))
                    setStyle("-fx-text-fill:#22c55e; -fx-font-weight:bold;");
                else if (item.equals(STATUT_ATTENTE))
                    setStyle("-fx-text-fill:#f59e0b; -fx-font-weight:bold;");
                else
                    setStyle("-fx-text-fill:#ef4444; -fx-font-weight:bold;");
            }
        });

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit  = new Button("Éditer");
            private final Button btnSuppr = new Button("Suppr");
            private final HBox   box      = new HBox(4, btnEdit, btnSuppr);
            {
                btnEdit.getStyleClass().add("btn-warning");
                btnSuppr.getStyleClass().add("btn-danger");
                btnEdit.setPrefWidth(75);  btnEdit.setMinWidth(75);
                btnSuppr.setPrefWidth(75); btnSuppr.setMinWidth(75);
                box.setPrefWidth(160);     box.setMinWidth(160);

                btnEdit.setOnAction(e -> {
                    Depense d = getTableView().getItems().get(getIndex());
                    getTableView().getSelectionModel().select(getIndex());
                    remplirFormulaire(d);
                    setModeEdition(true);
                    showForm(true);
                    msg("Modifiez les champs puis cliquez Modifier", false);
                });

                btnSuppr.setOnAction(e -> {
                    Depense d = getTableView().getItems().get(getIndex());
                    try {
                        service.supprimer(d.getIdDepense());
                        msg("Dépense supprimée avec succès", false);
                        charger();
                    } catch (SQLException ex) {
                        msg("Erreur : " + ex.getMessage(), true);
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

        chargerCategories();
        charger();
    }

    // ── Normalisation statut ──────────────────────────────

    private String normaliserStatut(String statut) {
        if (statut == null) return STATUT_NON_PAYE;
        String s = statut.trim().toLowerCase()
                .replace("é", "e").replace("è", "e").replace("ê", "e");
        if (s.equals("paye") || s.equals("pay") || s.equals("payed")) return STATUT_PAYE;
        if (s.contains("attente") || s.contains("cours") || s.contains("progress")) return STATUT_ATTENTE;
        if (s.contains("non") || s.contains("impaye")) return STATUT_NON_PAYE;
        return statut;
    }

    // ── Validation ───────────────────────────────────────

    private boolean validerFormulaire() {
        boolean valide = true;
        resetStyles();

        if (descField.getText() == null || descField.getText().trim().isEmpty()) {
            setFieldError(descField, "La description est obligatoire"); valide = false;
        } else if (descField.getText().trim().length() < 3) {
            setFieldError(descField, "La description doit contenir au moins 3 caractères"); valide = false;
        } else if (descField.getText().trim().length() > 100) {
            setFieldError(descField, "La description ne peut pas dépasser 100 caractères"); valide = false;
        }

        if (montantField.getText() == null || montantField.getText().trim().isEmpty()) {
            setFieldError(montantField, "Le montant est obligatoire"); valide = false;
        } else {
            try {
                double val = Double.parseDouble(montantField.getText().trim());
                if (val <= 0) { setFieldError(montantField, "Le montant doit être supérieur à 0"); valide = false; }
                else if (val > 1_000_000) { setFieldError(montantField, "Le montant ne peut pas dépasser 1 000 000"); valide = false; }
            } catch (NumberFormatException e) {
                setFieldError(montantField, "Le montant doit être un nombre valide"); valide = false;
            }
        }

        if (datePicker.getValue() == null) {
            datePicker.setStyle("-fx-border-color:#e05050; -fx-border-radius:10px; -fx-background-radius:10px; -fx-border-width:1px;");
            msg("La date est obligatoire", true); valide = false;
        }

        if (statutCombo.getValue() == null) {
            statutCombo.setStyle("-fx-border-color:#e05050; -fx-border-radius:10px; -fx-background-radius:10px; -fx-border-width:1px;");
            msg("Sélectionnez un statut", true); valide = false;
        }

        if (catCombo.getValue() == null) {
            catCombo.setStyle("-fx-border-color:#e05050; -fx-border-radius:10px; -fx-background-radius:10px; -fx-border-width:1px;");
            msg("Sélectionnez une catégorie", true); valide = false;
        }

        return valide;
    }

    private boolean verifierBudgetMax(double montant) {
        Categorie cat = catCombo.getValue();
        if (cat != null && montant > cat.getBudgetMax()) {
            setFieldError(montantField, "Le montant dépasse le budget max (" + cat.getBudgetMax() + ")");
            return false;
        }
        return true;
    }

    private void setFieldError(TextField field, String message) {
        field.setStyle("-fx-border-color:#e05050; -fx-border-radius:10px;" +
                "-fx-background-radius:10px; -fx-border-width:1px; -fx-background-color:#2a1a1a;");
        msg(message, true);
    }

    private void resetStyles() {
        descField.setStyle("");
        montantField.setStyle("");
        datePicker.setStyle("");
        statutCombo.setStyle("");
        catCombo.setStyle("");
        if (msgLabel != null) msgLabel.setText("");
    }

    // ── Chargement ───────────────────────────────────────

    private void chargerCategories() {
        try {
            catCombo.getItems().setAll(catService.afficher());
            catCombo.setConverter(new StringConverter<Categorie>() {
                @Override public String toString(Categorie c) {
                    return c == null ? "" : c.getIdCategorie() + " - " + c.getNomCategorie();
                }
                @Override public Categorie fromString(String s) { return null; }
            });
        } catch (SQLException e) { msg("Erreur catégories : " + e.getMessage(), true); }
    }

    private void setModeEdition(boolean edition) {
        modeEdition = edition;
        if (btnAjouterBox != null) { btnAjouterBox.setVisible(!edition); btnAjouterBox.setManaged(!edition); }
        if (btnModifierBox != null) { btnModifierBox.setVisible(edition); btnModifierBox.setManaged(edition); }
    }

    // ── Navigation ───────────────────────────────────────

    @FXML
    public void showCategories() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/viewsBack/CategorieView.fxml"));
            Stage stage = (Stage) table.getScene().getWindow();
            stage.setScene(new Scene(root, 1280, 800));
            stage.setMaximized(true);
        } catch (IOException e) { msg("Erreur navigation : " + e.getMessage(), true); }
    }

    @FXML public void ouvrirFormulaire() { viderFormulaire(); setModeEdition(false); showForm(true); }
    @FXML public void annuler()          { viderFormulaire(); setModeEdition(false); showForm(false); }

    private void showForm(boolean visible) {
        if (formPane != null) {
            formPane.setVisible(visible);
            formPane.setManaged(visible);
            formPane.setStyle(visible
                    ? "-fx-background-color:#1e3525; -fx-background-radius:8px; -fx-padding:16px;" +
                    "-fx-border-color:#2ecc71; -fx-border-radius:8px; -fx-border-width:1px;"
                    : "-fx-background-color:#1a2d20; -fx-background-radius:8px; -fx-padding:16px;");
        }
    }

    @FXML
    public void ajouter() {
        if (!validerFormulaire()) return;
        double montant = Double.parseDouble(montantField.getText().trim());
        if (!verifierBudgetMax(montant)) return;
        try {
            service.ajouter(new Depense(
                    descField.getText().trim(), montant,
                    java.sql.Date.valueOf(datePicker.getValue()),
                    statutCombo.getValue(),
                    catCombo.getValue().getIdCategorie()));
            msg("Dépense ajoutée avec succès !", false);
            viderFormulaire(); showForm(false); charger();
        } catch (SQLException e) { msg("Erreur BD : " + e.getMessage(), true); }
    }

    @FXML
    public void modifier() {
        Depense sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { msg("Sélectionnez une ligne d'abord", true); return; }
        if (!validerFormulaire()) return;
        double montant = Double.parseDouble(montantField.getText().trim());
        if (!verifierBudgetMax(montant)) return;
        try {
            service.modifier(new Depense(
                    sel.getIdDepense(),
                    descField.getText().trim(), montant,
                    java.sql.Date.valueOf(datePicker.getValue()),
                    statutCombo.getValue(),
                    catCombo.getValue().getIdCategorie()));
            msg("Dépense modifiée avec succès !", false);
            viderFormulaire(); setModeEdition(false); showForm(false); charger();
        } catch (SQLException e) { msg("Erreur BD : " + e.getMessage(), true); }
    }

    @FXML
    public void charger() {
        try {
            filteredData = new FilteredList<>(FXCollections.observableArrayList(service.afficher()));
            table.setItems(filteredData);
        } catch (SQLException e) { msg("Erreur BD : " + e.getMessage(), true); }
    }

    @FXML
    public void filtrer() {
        if (filteredData == null) return;
        try {
            String minTxt = montantMinField.getText().trim();
            String maxTxt = montantMaxField.getText().trim();
            if (!minTxt.isEmpty() && !maxTxt.isEmpty()) {
                double min = Double.parseDouble(minTxt);
                double max = Double.parseDouble(maxTxt);
                if (min > max) { msg("Le montant min ne peut pas être supérieur au max", true); return; }
            }
            double min = minTxt.isEmpty() ? Double.MIN_VALUE : Double.parseDouble(minTxt);
            double max = maxTxt.isEmpty() ? Double.MAX_VALUE : Double.parseDouble(maxTxt);
            filteredData.setPredicate(d -> d.getMontant() >= min && d.getMontant() <= max);
        } catch (NumberFormatException e) { msg("Montant de filtre invalide", true); }
    }

    @FXML
    public void trierDate() {
        if (filteredData == null) return;
        table.setItems(filteredData.sorted((a, b) -> {
            if (a.getDateDepense() == null) return 1;
            if (b.getDateDepense() == null) return -1;
            return b.getDateDepense().compareTo(a.getDateDepense());
        }));
    }

    @FXML
    public void trierMontant() {
        if (filteredData == null) return;
        table.setItems(filteredData.sorted((a, b) -> Double.compare(b.getMontant(), a.getMontant())));
    }

    public List<Depense> listerDepenses() {
        try { return service.afficher(); }
        catch (SQLException e) { return Collections.emptyList(); }
    }

    public List<Depense> listerDepensesParCategorie(int idCategorie) {
        try { return service.getByCategorie(idCategorie); }
        catch (SQLException e) { return Collections.emptyList(); }
    }

    private void remplirFormulaire(Depense d) {
        descField.setText(d.getDescription());
        montantField.setText(String.valueOf(d.getMontant()));
        statutCombo.setValue(normaliserStatut(d.getStatut())); // ✅ normalise avant de set
        catCombo.getItems().stream()
                .filter(c -> c.getIdCategorie() == d.getIdCategorie())
                .findFirst().ifPresent(catCombo::setValue);
        if (d.getDateDepense() != null) {
            if (d.getDateDepense() instanceof java.sql.Date)
                datePicker.setValue(((java.sql.Date) d.getDateDepense()).toLocalDate());
            else
                datePicker.setValue(d.getDateDepense().toInstant()
                        .atZone(java.time.ZoneId.systemDefault()).toLocalDate());
        } else datePicker.setValue(null);
    }

    private void viderFormulaire() {
        descField.clear(); montantField.clear();
        statutCombo.setValue(null); catCombo.setValue(null); datePicker.setValue(null);
        if (montantMinField != null) montantMinField.clear();
        if (montantMaxField != null) montantMaxField.clear();
        resetStyles();
    }

    private void msg(String m, boolean erreur) {
        if (msgLabel != null) {
            msgLabel.setText(m);
            msgLabel.setStyle(erreur ? "-fx-text-fill:#e05050; -fx-font-size:12px;"
                    : "-fx-text-fill:#2ecc71; -fx-font-size:12px;");
        }
    }
}
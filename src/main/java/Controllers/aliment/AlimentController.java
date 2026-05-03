package Controllers.aliment;

import Models.Aliment;
import Models.Repas;
import Services.ServiceAliment;
import Services.ServiceRepas;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.net.URL;
import java.sql.SQLDataException;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;

public class AlimentController implements Initializable {

    /*
     * ══════════════════════════════════════════════
     * LIST VIEW
     * ══════════════════════════════════════════════
     */
    @FXML
    private TableView<Aliment> alimentTable;
    @FXML
    private TableColumn<Aliment, Integer> colId;
    @FXML
    private TableColumn<Aliment, String> colNom;
    @FXML
    private TableColumn<Aliment, Double> colQuantite;
    @FXML
    private TableColumn<Aliment, Double> colCalories;
    @FXML
    private TableColumn<Aliment, String> colMacro;
    @FXML
    private TableColumn<Aliment, Void> colActions;
    @FXML
    private Label statTotal;
    @FXML
    private Label statAvgCal;
    @FXML
    private Label statMaxCal;
    @FXML
    private Label statWithMacro;
    @FXML
    private Label alimentCountLabel;
    @FXML
    private TextField searchField;

    /*
     * ══════════════════════════════════════════════
     * ADD / DETAIL VIEW
     * ══════════════════════════════════════════════
     */
    @FXML
    private Label pageTitle;
    @FXML
    private TextField nomField;
    @FXML
    private TextField quantiteField;
    @FXML
    private TextField caloriesField;
    @FXML
    private TextField proteinesField;
    @FXML
    private TextField glucidesField;
    @FXML
    private TextField lipidesField;
    @FXML
    private Label errorLabel;
    @FXML
    private Button btnTabInfo;
    @FXML
    private Button btnTabRepas;
    @FXML
    private VBox paneInfo;
    @FXML
    private VBox paneRepas;

    /* ── Repas liés tab ── */
    @FXML
    private TableView<Repas> repasLiesTable;
    @FXML
    private TableColumn<Repas, Integer> colRepasId;
    @FXML
    private TableColumn<Repas, String> colRepasNom;
    @FXML
    private TableColumn<Repas, String> colRepasType;
    @FXML
    private TableColumn<Repas, String> colRepasDate;
    @FXML
    private TableColumn<Repas, Integer> colRepasCal;

    /*
     * ══════════════════════════════════════════════
     * STATE — services initialized lazily in initialize()
     * ══════════════════════════════════════════════
     */
    private Aliment currentAliment;
    private ObservableList<Aliment> allAliments;

    private ServiceAliment serviceAliment;
    private ServiceRepas serviceRepas;

    /*
     * ══════════════════════════════════════════════
     * INITIALIZE
     * ══════════════════════════════════════════════
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Initialize services here, NOT as field declarations
        try {
            serviceAliment = new ServiceAliment();
            serviceRepas = new ServiceRepas();
            System.out.println("Aliment services initialized OK");
        } catch (Exception e) {
            System.out.println("Aliment service init error: " + e.getMessage());
        }

        if (alimentTable != null) {
            setupColumns();
            loadData();
            setupSearch();
        }

        if (repasLiesTable != null) {
            setupRepasLiesColumns();
        }

        if (quantiteField != null && quantiteField.getText().isBlank()) {
            quantiteField.setText("100");
        }

        hideError();
    }

    /*
     * ══════════════════════════════════════════════
     * SET ALIMENT (called before switching to detail view)
     * ══════════════════════════════════════════════
     */
    public void setAliment(Aliment a) {
        this.currentAliment = a;
        if (pageTitle != null)
            pageTitle.setText("Modifier : " + a.getNom());
        if (nomField != null)
            nomField.setText(a.getNom());
        if (quantiteField != null)
            quantiteField.setText(formatDouble(a.getQuantite()));
        if (caloriesField != null)
            caloriesField.setText(formatDouble(a.getCalories()));
        String[] macros = parseMacro(a.getMacro());
        if (proteinesField != null)
            proteinesField.setText(macros[0]);
        if (glucidesField != null)
            glucidesField.setText(macros[1]);
        if (lipidesField != null)
            lipidesField.setText(macros[2]);
        loadRepasLies();
    }

    /*
     * ══════════════════════════════════════════════
     * LIST VIEW LOGIC
     * ══════════════════════════════════════════════
     */
    private void setupColumns() {
        colId.setCellValueFactory(d -> new javafx.beans.property.SimpleIntegerProperty(
                d.getValue().getId()).asObject());
        colNom.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getNom()));

        colQuantite.setCellValueFactory(d -> new javafx.beans.property.SimpleDoubleProperty(
                d.getValue().getQuantite()).asObject());
        colQuantite.setCellFactory(col -> new TableCell<>() {
            private final Label lbl = new Label();
            {
                lbl.getStyleClass().add("quantite-cell");
                setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(Double q, boolean empty) {
                super.updateItem(q, empty);
                if (empty || q == null) {
                    setGraphic(null);
                    return;
                }
                lbl.setText(formatDouble(q) + " g");
                setGraphic(lbl);
            }
        });

        colCalories.setCellValueFactory(d -> new javafx.beans.property.SimpleDoubleProperty(
                d.getValue().getCalories()).asObject());
        colCalories.setCellFactory(col -> new TableCell<>() {
            private final Label lbl = new Label();
            {
                lbl.getStyleClass().add("calories-cell-double");
                setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(Double c, boolean empty) {
                super.updateItem(c, empty);
                if (empty || c == null) {
                    setGraphic(null);
                    return;
                }
                lbl.setText(formatDouble(c) + " kcal");
                setGraphic(lbl);
            }
        });

        colMacro.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getMacro()));
        colMacro.setCellFactory(col -> new TableCell<>() {
            private final HBox box = new HBox(6);
            {
                box.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(String macro, boolean empty) {
                super.updateItem(macro, empty);
                box.getChildren().clear();
                if (empty || macro == null || macro.isBlank()) {
                    Label none = new Label("—");
                    none.setStyle("-fx-text-fill: rgba(245,245,244,0.30);");
                    box.getChildren().add(none);
                } else {
                    String[] p = parseMacro(macro);
                    addPill(box, "P:" + p[0], "#22c55e");
                    addPill(box, "G:" + p[1], "#f59e0b");
                    addPill(box, "L:" + p[2], "#a78bfa");
                }
                setGraphic(box);
            }
        });

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit = new Button("✎");
            private final Button btnDelete = new Button("✕");
            private final HBox box = new HBox(8, btnEdit, btnDelete);
            {
                box.setAlignment(Pos.CENTER_LEFT);
                btnEdit.getStyleClass().add("btn-action-edit");
                btnDelete.getStyleClass().add("btn-action-delete");
                btnEdit.setTooltip(new Tooltip("Modifier"));
                btnDelete.setTooltip(new Tooltip("Supprimer"));
                addHoverScale(btnEdit);
                addHoverScale(btnDelete);
                btnEdit.setOnAction(e -> openEdit(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e -> handleDelete(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void loadData() {
        try {
            allAliments = FXCollections.observableArrayList(serviceAliment.recuperer());
            alimentTable.setItems(allAliments);
            updateStats();
            FadeTransition ft = new FadeTransition(Duration.millis(400), alimentTable);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();
        } catch (Exception e) {
            System.out.println("loadData aliment error: " + e.getMessage());
            allAliments = FXCollections.observableArrayList();
            alimentTable.setItems(allAliments);
        }
    }

    private void updateStats() {
        int total = allAliments.size();
        if (statTotal != null)
            statTotal.setText(String.valueOf(total));
        if (alimentCountLabel != null)
            alimentCountLabel.setText(total + " aliments au total");
        if (total > 0) {
            double avg = allAliments.stream()
                    .mapToDouble(Aliment::getCalories).average().orElse(0);
            if (statAvgCal != null)
                statAvgCal.setText(formatDouble(avg) + " kcal");
            Aliment max = allAliments.stream()
                    .max(Comparator.comparingDouble(Aliment::getCalories)).orElse(null);
            if (statMaxCal != null)
                statMaxCal.setText(max != null ? max.getNom() : "—");
            long withMacro = allAliments.stream()
                    .filter(a -> a.getMacro() != null && !a.getMacro().isBlank()).count();
            if (statWithMacro != null)
                statWithMacro.setText(String.valueOf(withMacro));
        } else {
            if (statAvgCal != null)
                statAvgCal.setText("0 kcal");
            if (statMaxCal != null)
                statMaxCal.setText("—");
            if (statWithMacro != null)
                statWithMacro.setText("0");
        }
    }

    private void setupSearch() {
        if (searchField == null)
            return;
        searchField.textProperty().addListener((obs, o, newVal) -> {
            if (newVal == null || newVal.isBlank()) {
                alimentTable.setItems(allAliments);
            } else {
                String lower = newVal.toLowerCase();
                alimentTable.setItems(allAliments.filtered(
                        a -> a.getNom().toLowerCase().contains(lower)));
            }
            if (alimentCountLabel != null)
                alimentCountLabel.setText(
                        alimentTable.getItems().size() + " aliments au total");
        });
    }

    @FXML
    private void sortAscCalories() {
        if (alimentTable != null)
            alimentTable.getItems().sort(
                    Comparator.comparingDouble(Aliment::getCalories));
    }

    @FXML
    private void sortDescCalories() {
        if (alimentTable != null)
            alimentTable.getItems().sort(
                    Comparator.comparingDouble(Aliment::getCalories).reversed());
    }

    @FXML
    private void openAddAliment() {
        navigateTo("/view/aliments/AddAliment.fxml");
    }

    private void openEdit(Aliment a) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/aliments/DetailAliment.fxml"));
            Parent root = loader.load();
            AlimentController ctrl = loader.getController();
            ctrl.setAliment(a);
            alimentTable.getScene().setRoot(root);
        } catch (Exception e) {
            showError("Impossible d'ouvrir l'éditeur : " + e.getMessage());
        }
    }

    /*
     * ══════════════════════════════════════════════
     * ADD VIEW LOGIC
     * ══════════════════════════════════════════════
     */
    @FXML
    private void handleCreate() {
        hideError();
        String nom = nomField != null ? nomField.getText() : "";
        if (nom.isBlank()) {
            showError("Le nom de l'aliment est obligatoire.");
            return;
        }
        if (nom.trim().length() < 2) {
            showError("Le nom doit contenir au moins 2 caractères.");
            return;
        }

        double quantite = parseDouble(quantiteField, "Quantité", true);
        if (Double.isNaN(quantite))
            return;
        if (quantite <= 0) {
            showError("La quantité doit être supérieure à 0.");
            return;
        }

        double proteines = parseDouble(proteinesField, "Protéines", false);
        if (Double.isNaN(proteines))
            return;
        double glucides = parseDouble(glucidesField, "Glucides", false);
        if (Double.isNaN(glucides))
            return;
        double lipides = parseDouble(lipidesField, "Lipides", false);
        if (Double.isNaN(lipides))
            return;

        double calories = (proteines * 4) + (glucides * 4) + (lipides * 9);
        if (caloriesField != null && !caloriesField.getText().isBlank()) {
            double manual = parseDouble(caloriesField, "Calories", false);
            if (Double.isNaN(manual))
                return;
            if (manual > 0)
                calories = manual;
        }

        String macro = String.format("P:%.1f;G:%.1f;L:%.1f", proteines, glucides, lipides);
        Aliment aliment = new Aliment(1, nom.trim(), quantite, calories, macro);
        try {
            serviceAliment.ajouter(aliment);
            goBack();
        } catch (SQLDataException e) {
            showError("Erreur lors de la création : " + e.getMessage());
        }
    }

    /*
     * ══════════════════════════════════════════════
     * DETAIL VIEW LOGIC
     * ══════════════════════════════════════════════
     */
    @FXML
    private void showInfoTab() {
        if (paneInfo != null) {
            paneInfo.setVisible(true);
            paneInfo.setManaged(true);
        }
        if (paneRepas != null) {
            paneRepas.setVisible(false);
            paneRepas.setManaged(false);
        }
        if (btnTabInfo != null)
            btnTabInfo.getStyleClass().setAll("form-tab-btn", "active");
        if (btnTabRepas != null)
            btnTabRepas.getStyleClass().setAll("form-tab-btn");
    }

    @FXML
    private void showRepasTab() {
        if (paneRepas != null) {
            paneRepas.setVisible(true);
            paneRepas.setManaged(true);
        }
        if (paneInfo != null) {
            paneInfo.setVisible(false);
            paneInfo.setManaged(false);
        }
        if (btnTabRepas != null)
            btnTabRepas.getStyleClass().setAll("form-tab-btn", "active");
        if (btnTabInfo != null)
            btnTabInfo.getStyleClass().setAll("form-tab-btn");
    }

    @FXML
    private void handleUpdate() {
        hideError();
        String nom = nomField.getText();
        if (nom == null || nom.isBlank()) {
            showError("Le nom est obligatoire.");
            return;
        }
        if (nom.trim().length() < 2) {
            showError("Le nom doit avoir au moins 2 caractères.");
            return;
        }

        double quantite = parseDouble(quantiteField, "Quantité", true);
        if (Double.isNaN(quantite))
            return;
        if (quantite <= 0) {
            showError("La quantité doit être > 0.");
            return;
        }

        double proteines = parseDouble(proteinesField, "Protéines", false);
        if (Double.isNaN(proteines))
            return;
        double glucides = parseDouble(glucidesField, "Glucides", false);
        if (Double.isNaN(glucides))
            return;
        double lipides = parseDouble(lipidesField, "Lipides", false);
        if (Double.isNaN(lipides))
            return;

        double calories = (proteines * 4) + (glucides * 4) + (lipides * 9);
        if (caloriesField != null && !caloriesField.getText().isBlank()) {
            double manual = parseDouble(caloriesField, "Calories", false);
            if (Double.isNaN(manual))
                return;
            if (manual > 0)
                calories = manual;
        }

        currentAliment.setNom(nom.trim());
        currentAliment.setQuantite(quantite);
        currentAliment.setCalories(calories);
        currentAliment.setMacro(
                String.format("P:%.1f;G:%.1f;L:%.1f", proteines, glucides, lipides));
        try {
            serviceAliment.modifier(currentAliment);
            goBack();
        } catch (SQLDataException e) {
            showError("Mise à jour échouée : " + e.getMessage());
        }
    }

    @FXML
    private void handleDelete() {
        if (currentAliment != null)
            confirmAndDelete(currentAliment);
    }

    private void handleDelete(Aliment a) {
        confirmAndDelete(a);
    }

    private void confirmAndDelete(Aliment a) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer l'aliment");
        alert.setHeaderText("Supprimer \"" + a.getNom() + "\" ?");
        alert.setContentText("Cet aliment sera retiré de tous les repas associés.");
        styleDialog(alert);
        alert.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    serviceAliment.supprimer(a);
                    if (alimentTable != null)
                        loadData();
                    else
                        goBack();
                } catch (SQLDataException e) {
                    showError("Suppression échouée : " + e.getMessage());
                }
            }
        });
    }

    private void setupRepasLiesColumns() {
        if (repasLiesTable == null)
            return;
        colRepasId.setCellValueFactory(d -> new javafx.beans.property.SimpleIntegerProperty(
                d.getValue().getId()).asObject());
        colRepasNom.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getNom()));
        colRepasType.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getType()));
        colRepasDate.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getDate() != null ? d.getValue().getDate().toString() : ""));
        colRepasCal.setCellValueFactory(d -> new javafx.beans.property.SimpleIntegerProperty(
                d.getValue().getCalories()).asObject());
    }

    private void loadRepasLies() {
        if (repasLiesTable == null || currentAliment == null)
            return;
        try {
            List<Repas> linked = serviceRepas.getRepasForAliment(currentAliment.getId());
            repasLiesTable.setItems(FXCollections.observableArrayList(linked));
        } catch (Exception e) {
            System.out.println("loadRepasLies error: " + e.getMessage());
        }
    }

    /*
     * ══════════════════════════════════════════════
     * HELPERS
     * ══════════════════════════════════════════════
     */
    private double parseDouble(TextField field, String name, boolean required) {
        if (field == null)
            return 0;
        String txt = field.getText();
        if (txt == null || txt.isBlank()) {
            if (required) {
                showError(name + " est obligatoire.");
                return Double.NaN;
            }
            return 0;
        }
        try {
            double v = Double.parseDouble(txt.trim());
            if (v < 0) {
                showError(name + " ne peut pas être négatif.");
                return Double.NaN;
            }
            return v;
        } catch (NumberFormatException e) {
            showError(name + " : valeur numérique invalide (ex: 3.5).");
            field.requestFocus();
            return Double.NaN;
        }
    }

    private String[] parseMacro(String macro) {
        String[] result = { "0", "0", "0" };
        if (macro == null || macro.isBlank())
            return result;
        try {
            for (String part : macro.split(";")) {
                String[] kv = part.split(":");
                if (kv.length == 2)
                    switch (kv[0].trim().toUpperCase()) {
                        case "P" -> result[0] = kv[1].trim();
                        case "G" -> result[1] = kv[1].trim();
                        case "L" -> result[2] = kv[1].trim();
                    }
            }
        } catch (Exception ignored) {
        }
        return result;
    }

    private void addPill(HBox box, String text, String hex) {
        int r = Integer.parseInt(hex.substring(1, 3), 16);
        int g = Integer.parseInt(hex.substring(3, 5), 16);
        int b = Integer.parseInt(hex.substring(5, 7), 16);
        Label pill = new Label(text);
        pill.setStyle(String.format(
                "-fx-background-color: rgba(%d,%d,%d,0.18);" +
                        "-fx-text-fill: %s;" +
                        "-fx-background-radius: 20;" +
                        "-fx-padding: 2 8 2 8;" +
                        "-fx-font-size: 11px;" +
                        "-fx-font-weight: bold;",
                r, g, b, hex));
        box.getChildren().add(pill);
    }

    private String formatDouble(double d) {
        return d == Math.floor(d) ? String.valueOf((int) d) : String.format("%.1f", d);
    }

    private void addHoverScale(javafx.scene.Node node) {
        ScaleTransition in = new ScaleTransition(Duration.millis(140), node);
        in.setToX(1.13);
        in.setToY(1.13);
        ScaleTransition out = new ScaleTransition(Duration.millis(140), node);
        out.setToX(1.0);
        out.setToY(1.0);
        node.setOnMouseEntered(e -> {
            in.stop();
            in.play();
        });
        node.setOnMouseExited(e -> {
            out.stop();
            out.play();
        });
    }

    private void showError(String msg) {
        if (errorLabel != null) {
            errorLabel.setText("⚠  " + msg);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
    }

    private void hideError() {
        if (errorLabel != null) {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        }
    }

    private void styleDialog(Alert a) {
        URL css = getClass().getResource("/style.css");
        if (css != null)
            a.getDialogPane().getStylesheets().add(css.toExternalForm());
        a.getDialogPane().getStyleClass().add("dialog-pane");
    }

    /*
     * ══════════════════════════════════════════════
     * NAVIGATION
     * ══════════════════════════════════════════════
     */
    @FXML
    private void goBack() {
        navigateTo("/view/aliments/ListAliment.fxml");
    }

    @FXML
    private void handleSearch() {
    }

    private void navigateTo(String path) {
        try {
            javafx.scene.Node anchor = alimentTable != null ? alimentTable
                    : nomField != null ? nomField : pageTitle;
            Parent root = FXMLLoader.load(getClass().getResource(path));
            anchor.getScene().setRoot(root);
        } catch (Exception e) {
            showError("Navigation échouée : " + e.getMessage());
            e.printStackTrace(System.out);
        }
    }

    @FXML
    private void goToDashboard() {
        navigateTo("/view/dashboard.fxml");
    }

    @FXML
    private void goToActivites() {
        navigateTo("/view/activites.fxml");
    }

    @FXML
    private void goToObjectifs() {
        navigateTo("/view/objectifs.fxml");
    }

    @FXML
    private void goToDepenses() {
        navigateTo("/view/depenses.fxml");
    }

    @FXML
    private void goToCategories() {
        navigateTo("/view/categories.fxml");
    }

    @FXML
    private void goToEnergie() {
        navigateTo("/view/energie.fxml");
    }

    @FXML
    private void goToReco() {
        navigateTo("/view/recommandations.fxml");
    }

    @FXML
    private void goToRepas() {
        navigateTo("/view/repas/ListRepas.fxml");
    }

    @FXML
    private void goToCours() {
        navigateTo("/view/cours.fxml");
    }

    @FXML
    private void goToChapitres() {
        navigateTo("/view/chapitres.fxml");
    }

    @FXML
    private void goToStats() {
        navigateTo("/view/stats.fxml");
    }

    @FXML
    private void goToProfil() {
        navigateTo("/view/profil.fxml");
    }
}
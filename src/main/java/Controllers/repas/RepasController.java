package Controllers.repas;

import Models.Aliment;
import Models.Repas;
import Services.ServiceAliment;
import Services.ServiceRepas;
import javafx.geometry.Pos;
import javafx.scene.layout.Priority;
import Services.PdfService;
import java.util.stream.Collectors;
import java.util.ArrayList;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.net.URL;
import java.sql.Date;
import java.sql.SQLDataException;
import java.sql.Time;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;

public class RepasController implements Initializable {

    /* ══════════════════════════════════════════════
       LIST VIEW
    ══════════════════════════════════════════════ */
    @FXML private TableView<Repas>            repasTable;
    @FXML private TableColumn<Repas, Integer> colId;
    @FXML private TableColumn<Repas, String>  colNom;
    @FXML private TableColumn<Repas, String>  colType;
    @FXML private TableColumn<Repas, String>  colDate;
    @FXML private TableColumn<Repas, String>  colHeure;
    @FXML private TableColumn<Repas, Integer> colCalories;
    @FXML private TableColumn<Repas, String>  colScore;
    @FXML private TableColumn<Repas, Void>    colActions;
    @FXML private Label     statTotalRepas;
    @FXML private Label     statAvgCalories;
    @FXML private Label     statCeMois;
    @FXML private Label     statScore;
    @FXML private Label     repasCountLabel;
    @FXML private TextField searchField;


    @FXML private TableColumn<Repas, Void> colPdf;

    /* ══════════════════════════════════════════════
       ADD / DETAIL VIEW
    ══════════════════════════════════════════════ */
    @FXML private Label            pageTitle;
    @FXML private TextField        nomField;
    @FXML private ComboBox<String> typeCombo;
    @FXML private DatePicker       datePicker;
    @FXML private TextField        heureField;
    @FXML private TextField        caloriesField;
    @FXML private TextArea         descriptionArea;
    @FXML private Label            errorLabel;
    @FXML private Button           btnTabInfo;
    @FXML private Button           btnTabAliments;
    @FXML private VBox             paneInfo;
    @FXML private VBox             paneAliments;
    @FXML private ListView<Aliment> alimentsListView;
    @FXML private Label             errorLabelAliments;



    @FXML private ComboBox<Aliment> alimentCombo;
    @FXML private VBox selectedAlimentsBox;
    private List<Aliment> selectedAliments = new ArrayList<>();


    /* ══════════════════════════════════════════════
       STATE — services initialized lazily in initialize()
    ══════════════════════════════════════════════ */
    private Repas currentRepas;
    private ObservableList<Repas> allRepas;
    private static final int CURRENT_USER_ID = 1;

    private ServiceRepas   serviceRepas;
    private ServiceAliment serviceAliment;

    /* ══════════════════════════════════════════════
       INITIALIZE
    ══════════════════════════════════════════════ */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Initialize services here, NOT as field declarations
        try {
            serviceRepas   = new ServiceRepas();
            serviceAliment = new ServiceAliment();
            System.out.println("Services initialized OK");
        } catch (Exception e) {
            System.out.println("Service init error: " + e.getMessage());
        }

        if (repasTable != null) {
            setupColumns();
            loadData();
            setupSearch();
        }

        if (typeCombo != null) {
            typeCombo.setItems(FXCollections.observableArrayList(
                    "Petit-déjeuner", "Déjeuner", "Dîner", "Collation"));
            typeCombo.getSelectionModel().selectFirst();
        }

        if (alimentsListView != null) {
            alimentsListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            alimentsListView.setCellFactory(lv -> new ListCell<>() {
                @Override protected void updateItem(Aliment a, boolean empty) {
                    super.updateItem(a, empty);
                    setText(empty || a == null ? null : a.getNom());
                }
            });
        }
        // Charger combo aliments pour AddRepas
        if (alimentCombo != null) {
            try {
                List<Aliment> aliments = serviceAliment.recuperer();
                alimentCombo.getItems().addAll(aliments);
                alimentCombo.setConverter(new javafx.util.StringConverter<>() {
                    public String toString(Aliment a) { return a == null ? "" : a.getNom() + " (" + (int)a.getCalories() + " kcal)"; }
                    public Aliment fromString(String s) { return null; }
                });
            } catch (Exception e) { System.out.println("combo error: " + e.getMessage()); }
        }

        hideError();
    }

    /* ══════════════════════════════════════════════
       SET REPAS (called before switching to detail view)
    ══════════════════════════════════════════════ */
    public void setRepas(Repas r) {
        this.currentRepas = r;
        if (pageTitle     != null) pageTitle.setText("Modifier : " + r.getNom());
        if (nomField      != null) nomField.setText(r.getNom());
        if (typeCombo     != null) typeCombo.setValue(r.getType());
        if (datePicker    != null && r.getDate() != null)
            datePicker.setValue(r.getDate().toLocalDate());
        if (heureField    != null)
            heureField.setText(r.getHeure() != null ? r.getHeure().toString() : "");
        if (caloriesField != null)
            caloriesField.setText(String.valueOf(r.getCalories()));
        if (descriptionArea != null)
            descriptionArea.setText(r.getDescription() != null ? r.getDescription() : "");
        loadAlimentsList();
    }

    /* ══════════════════════════════════════════════
       LIST VIEW LOGIC
    ══════════════════════════════════════════════ */
    private void setupColumns() {
        colId.setCellValueFactory(d ->
                new javafx.beans.property.SimpleIntegerProperty(
                        d.getValue().getId()).asObject());
        colNom.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(d.getValue().getNom()));
        colDate.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(
                        d.getValue().getDate() != null ? d.getValue().getDate().toString() : ""));
        colHeure.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(
                        d.getValue().getHeure() != null ? d.getValue().getHeure().toString() : ""));

        colType.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(d.getValue().getType()));
        colType.setCellFactory(col -> new TableCell<>() {
            private final Label badge = new Label();
            { setContentDisplay(ContentDisplay.GRAPHIC_ONLY); setAlignment(Pos.CENTER_LEFT); }
            @Override protected void updateItem(String type, boolean empty) {
                super.updateItem(type, empty);
                if (empty || type == null) { setGraphic(null); return; }
                badge.setText(type);
                badge.getStyleClass().setAll("badge", typeToBadgeClass(type));
                setGraphic(badge);
            }
        });

        colCalories.setCellValueFactory(d ->
                new javafx.beans.property.SimpleIntegerProperty(
                        d.getValue().getCalories()).asObject());
        colCalories.setCellFactory(col -> new TableCell<>() {
            private final Label lbl = new Label();
            { lbl.getStyleClass().add("calories-cell"); setAlignment(Pos.CENTER_LEFT); }
            @Override protected void updateItem(Integer kcal, boolean empty) {
                super.updateItem(kcal, empty);
                if (empty || kcal == null) { setGraphic(null); return; }
                lbl.setText(kcal + " kcal");
                setGraphic(lbl);
            }
        });

        colScore.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(
                        computeScore(d.getValue().getCalories())));
        colScore.setCellFactory(col -> new TableCell<>() {
            private final Label badge = new Label();
            { setAlignment(Pos.CENTER_LEFT); }
            @Override protected void updateItem(String score, boolean empty) {
                super.updateItem(score, empty);
                if (empty || score == null) { setGraphic(null); return; }
                badge.setText(score);
                badge.getStyleClass().setAll("badge",
                        score.equals("Excellent") ? "badge-excellent" : "badge-bon");
                setGraphic(badge);
            }
        });

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit   = new Button("✎");
            private final Button btnDelete = new Button("✕");
            private final HBox   box       = new HBox(8, btnEdit, btnDelete);
            {
                box.setAlignment(Pos.CENTER_LEFT);
                btnEdit.getStyleClass().add("btn-action-edit");
                btnDelete.getStyleClass().add("btn-action-delete");
                btnEdit.setTooltip(new Tooltip("Modifier"));
                btnDelete.setTooltip(new Tooltip("Supprimer"));
                addHoverScale(btnEdit);
                addHoverScale(btnDelete);
                btnEdit.setOnAction(e ->
                        openEdit(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e ->
                        handleDelete(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : box);
            }
        });

        colPdf.setCellFactory(col -> new TableCell<>() {
            private final Button btnPdf = new Button("📄");
            {
                btnPdf.setTooltip(new Tooltip("Exporter PDF"));
                btnPdf.getStyleClass().add("btn-action-edit");
                btnPdf.setOnAction(e -> {
                    Repas r = getTableView().getItems().get(getIndex());
                    exportPdf(r);
                });
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : btnPdf);
            }
        });
    }

    private void exportPdf(Repas r) {
        try {
            List<Integer> ids = serviceRepas.getLinkedAlimentIds(r.getId());
            List<Aliment> all = serviceAliment.recuperer();
            List<Aliment> linked = all.stream()
                    .filter(a -> ids.contains(a.getId()))
                    .collect(java.util.stream.Collectors.toList());

            String path = System.getProperty("user.home") + "/Desktop/repas_" + r.getId() + "_" + r.getNom().replaceAll("\\s+", "_") + ".pdf";

            PdfService pdfService = new PdfService();
            pdfService.exportRepasPdf(r, linked, path);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("PDF exporté");
            alert.setHeaderText("✅ PDF généré avec succès !");
            alert.setContentText("Fichier sauvegardé sur le Bureau :\n" + path);
            alert.showAndWait();

        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Export PDF échoué");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    private void loadData() {
        try {
            allRepas = FXCollections.observableArrayList(serviceRepas.recuperer());
            repasTable.setItems(allRepas);
            updateStats();
            FadeTransition ft = new FadeTransition(Duration.millis(400), repasTable);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();
        } catch (Exception e) {
            System.out.println("loadData error: " + e.getMessage());
            allRepas = FXCollections.observableArrayList();
            repasTable.setItems(allRepas);
        }
    }

    private void updateStats() {
        int total = allRepas.size();
        if (statTotalRepas  != null) statTotalRepas.setText(String.valueOf(total));
        if (repasCountLabel != null) repasCountLabel.setText(total + " repas au total");
        if (total > 0) {
            double avg = allRepas.stream().mapToInt(Repas::getCalories).average().orElse(0);
            if (statAvgCalories != null) statAvgCalories.setText((int) avg + " kcal");
            LocalDate now = LocalDate.now();
            long thisMo = allRepas.stream()
                    .filter(r -> r.getDate() != null &&
                            r.getDate().toLocalDate().getMonthValue() == now.getMonthValue() &&
                            r.getDate().toLocalDate().getYear() == now.getYear())
                    .count();
            if (statCeMois != null) statCeMois.setText(String.valueOf(thisMo));
            if (statScore  != null) statScore.setText("Excellent");
        } else {
            if (statAvgCalories != null) statAvgCalories.setText("0 kcal");
            if (statCeMois      != null) statCeMois.setText("0");
            if (statScore       != null) statScore.setText("—");
        }
    }

    private void setupSearch() {
        if (searchField == null) return;
        searchField.textProperty().addListener((obs, o, newVal) -> {
            if (newVal == null || newVal.isBlank()) {
                repasTable.setItems(allRepas);
            } else {
                String lower = newVal.toLowerCase();
                repasTable.setItems(allRepas.filtered(r ->
                        r.getNom().toLowerCase().contains(lower) ||
                                (r.getType() != null && r.getType().toLowerCase().contains(lower))));
            }
            if (repasCountLabel != null)
                repasCountLabel.setText(repasTable.getItems().size() + " repas au total");
        });
    }



    @FXML
    private void handleAddAliment() {
        Aliment selected = alimentCombo.getValue();
        if (selected == null) return;
        if (selectedAliments.stream().anyMatch(a -> a.getId() == selected.getId())) return;
        selectedAliments.add(selected);

        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: #1e293b; -fx-padding: 6 10; -fx-background-radius: 6;");
        Label lbl = new Label("• " + selected.getNom() + " — " + (int)selected.getCalories() + " kcal");
        lbl.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 12px;");
        HBox.setHgrow(lbl, Priority.ALWAYS);
        Button remove = new Button("✕");
        remove.setStyle("-fx-background-color: transparent; -fx-text-fill: #ef4444; -fx-cursor: hand;");
        remove.setOnAction(e -> {
            selectedAliments.remove(selected);
            selectedAlimentsBox.getChildren().remove(row);
        });
        row.getChildren().addAll(lbl, remove);
        selectedAlimentsBox.getChildren().add(row);
        alimentCombo.setValue(null);
    }

    @FXML private void sortAscCalories() {
        if (repasTable != null)
            repasTable.getItems().sort((a, b) ->
                    Integer.compare(a.getCalories(), b.getCalories()));
    }

    @FXML private void sortDescCalories() {
        if (repasTable != null)
            repasTable.getItems().sort((a, b) ->
                    Integer.compare(b.getCalories(), a.getCalories()));
    }

    @FXML private void openAddRepas() { navigateTo("/view/repas/AddRepas.fxml"); }

    private void openEdit(Repas r) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/repas/DetailRepas.fxml"));
            Parent root = loader.load();
            RepasController ctrl = loader.getController();
            ctrl.setRepas(r);
            repasTable.getScene().setRoot(root);
        } catch (Exception e) {
            showError("Impossible d'ouvrir l'éditeur : " + e.getMessage());
        }
    }

    /* ══════════════════════════════════════════════
       ADD VIEW LOGIC
    ══════════════════════════════════════════════ */
    @FXML
    private void handleCreate() {
        hideError();
        String nom = nomField != null ? nomField.getText() : "";
        if (nom.isBlank()) { showError("Le nom du repas est obligatoire."); return; }
        if (typeCombo.getValue() == null) { showError("Veuillez sélectionner un type."); return; }
        if (datePicker.getValue() == null) { showError("Veuillez sélectionner une date."); return; }

        String heureStr = heureField.getText().trim();
        if (heureStr.isBlank()) { showError("L'heure est obligatoire (ex: 12:30:00)."); return; }

        Time heure;
        try {
            if (heureStr.matches("\\d{1,2}:\\d{2}")) heureStr += ":00";
            heure = Time.valueOf(heureStr);
        } catch (IllegalArgumentException e) {
            showError("Format d'heure invalide. Utilisez HH:mm ou HH:mm:ss."); return;
        }

        int calories = 0;
        String calStr = caloriesField.getText().trim();
        if (!calStr.isBlank()) {
            try {
                calories = Integer.parseInt(calStr);
                if (calories < 0) { showError("Les calories ne peuvent pas être négatives."); return; }
            } catch (NumberFormatException e) {
                showError("Les calories doivent être un entier."); return;
            }
        }

        Repas repas = new Repas(CURRENT_USER_ID, nom.trim(), heure, calories,
                descriptionArea.getText() != null ? descriptionArea.getText().trim() : "",
                typeCombo.getValue(), Date.valueOf(datePicker.getValue()));

        try {
            serviceRepas.addWithAliments(repas, selectedAliments);
        } catch (Exception ex) {
            try {
                serviceRepas.ajouter(repas);
            } catch (SQLDataException ex2) {
                showError("Erreur : " + ex2.getMessage());
                return;
            }
        }
        goBack();
    }

    /* ══════════════════════════════════════════════
       DETAIL VIEW LOGIC
    ══════════════════════════════════════════════ */
    @FXML private void showInfoTab() {
        if (paneInfo     != null) { paneInfo.setVisible(true);      paneInfo.setManaged(true);     }
        if (paneAliments != null) { paneAliments.setVisible(false); paneAliments.setManaged(false); }
        if (btnTabInfo     != null) btnTabInfo.getStyleClass().setAll("form-tab-btn", "active");
        if (btnTabAliments != null) btnTabAliments.getStyleClass().setAll("form-tab-btn");
    }

    @FXML private void showAlimentsTab() {
        if (paneAliments != null) { paneAliments.setVisible(true);  paneAliments.setManaged(true);  }
        if (paneInfo     != null) { paneInfo.setVisible(false);     paneInfo.setManaged(false);     }
        if (btnTabAliments != null) btnTabAliments.getStyleClass().setAll("form-tab-btn", "active");
        if (btnTabInfo     != null) btnTabInfo.getStyleClass().setAll("form-tab-btn");
    }

    @FXML
    private void handleUpdate() {
        hideError();
        String nom = nomField.getText();
        if (nom == null || nom.isBlank()) { showError("Le nom est obligatoire."); return; }
        if (typeCombo.getValue() == null)  { showError("Veuillez sélectionner un type."); return; }
        if (datePicker.getValue() == null) { showError("Veuillez sélectionner une date."); return; }

        String heureStr = heureField.getText().trim();
        if (heureStr.isBlank()) { showError("L'heure est obligatoire."); return; }
        Time heure;
        try {
            if (heureStr.matches("\\d{1,2}:\\d{2}")) heureStr += ":00";
            heure = Time.valueOf(heureStr);
        } catch (IllegalArgumentException e) {
            showError("Format d'heure invalide."); return;
        }

        int calories = 0;
        String calStr = caloriesField.getText().trim();
        if (!calStr.isBlank()) {
            try {
                calories = Integer.parseInt(calStr);
                if (calories < 0) { showError("Les calories ne peuvent pas être négatives."); return; }
            } catch (NumberFormatException e) {
                showError("Les calories doivent être un entier."); return;
            }
        }

        currentRepas.setNom(nom.trim());
        currentRepas.setType(typeCombo.getValue());
        currentRepas.setDate(Date.valueOf(datePicker.getValue()));
        currentRepas.setHeure(heure);
        currentRepas.setCalories(calories);
        currentRepas.setDescription(
                descriptionArea.getText() != null ? descriptionArea.getText().trim() : "");
        try {
            serviceRepas.modifier(currentRepas);
            goBack();
        } catch (SQLDataException e) {
            showError("Mise à jour échouée : " + e.getMessage());
        }
    }

    @FXML
    private void handleUpdateAliments() {
        if (errorLabelAliments != null) {
            errorLabelAliments.setVisible(false);
            errorLabelAliments.setManaged(false);
        }
        try {
            serviceRepas.deleteRepasAliments(currentRepas.getId());
            for (Aliment a : alimentsListView.getSelectionModel().getSelectedItems())
                serviceRepas.insertRepasAliment(currentRepas.getId(), a.getId());
            goBack();
        } catch (Exception e) {
            if (errorLabelAliments != null) {
                errorLabelAliments.setText("⚠  " + e.getMessage());
                errorLabelAliments.setVisible(true);
                errorLabelAliments.setManaged(true);
            }
        }
    }

    @FXML
    private void handleDelete() {
        if (currentRepas != null) confirmAndDelete(currentRepas);
    }

    private void handleDelete(Repas r) { confirmAndDelete(r); }

    private void confirmAndDelete(Repas r) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer le repas");
        alert.setHeaderText("Supprimer \"" + r.getNom() + "\" ?");
        alert.setContentText("Cette action est irréversible.");
        styleDialog(alert);
        alert.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    serviceRepas.deleteRepasAliments(r.getId());
                    serviceRepas.supprimer(r);
                    if (repasTable != null) loadData();
                    else goBack();
                } catch (Exception e) {
                    showError("Suppression échouée : " + e.getMessage());
                }
            }
        });
    }

    private void loadAlimentsList() {
        if (alimentsListView == null || currentRepas == null) return;
        try {
            List<Aliment> all = serviceAliment.recuperer();
            alimentsListView.setItems(FXCollections.observableArrayList(all));
            List<Integer> linked = serviceRepas.getLinkedAlimentIds(currentRepas.getId());
            for (int i = 0; i < all.size(); i++)
                if (linked.contains(all.get(i).getId()))
                    alimentsListView.getSelectionModel().select(i);
        } catch (Exception e) {
            System.out.println("loadAlimentsList error: " + e.getMessage());
        }
    }

    /* ══════════════════════════════════════════════
       HELPERS
    ══════════════════════════════════════════════ */
    private String typeToBadgeClass(String type) {
        return switch (type.toLowerCase().trim()) {
            case "déjeuner", "dejeuner"                  -> "badge-dejeuner";
            case "dîner", "diner"                        -> "badge-diner";
            case "collation"                             -> "badge-collation";
            case "petit-déjeuner", "petit déjeuner",
                 "petit-dejeuner", "petit dejeuner"      -> "badge-petitdejeuner";
            default                                      -> "badge-dejeuner";
        };
    }

    private String computeScore(int cal) {
        if (cal <= 400) return "Excellent";
        if (cal <= 700) return "Bon";
        return "Élevé";
    }

    private void addHoverScale(javafx.scene.Node node) {
        ScaleTransition in  = new ScaleTransition(Duration.millis(140), node);
        in.setToX(1.13); in.setToY(1.13);
        ScaleTransition out = new ScaleTransition(Duration.millis(140), node);
        out.setToX(1.0); out.setToY(1.0);
        node.setOnMouseEntered(e -> { in.stop();  in.play();  });
        node.setOnMouseExited (e -> { out.stop(); out.play(); });
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

    /* ══════════════════════════════════════════════
       NAVIGATION
    ══════════════════════════════════════════════ */
    @FXML private void goBack()       { navigateTo("/view/repas/ListRepas.fxml"); }
    @FXML private void handleSearch() { }

    private void navigateTo(String path) {
        try {
            javafx.scene.Node anchor = repasTable != null ? repasTable
                    : nomField != null ? nomField : pageTitle;
            Parent root = FXMLLoader.load(getClass().getResource(path));
            anchor.getScene().setRoot(root);
        } catch (Exception e) {
            showError("Navigation échouée : " + e.getMessage());
            e.printStackTrace(System.out);
        }
    }

    @FXML private void goToDashboard()  { navigateTo("/view/dashboard.fxml"); }
    @FXML private void goToActivites()  { navigateTo("/view/activites.fxml"); }
    @FXML private void goToObjectifs()  { navigateTo("/view/objectifs.fxml"); }
    @FXML private void goToDepenses()   { navigateTo("/view/depenses.fxml"); }
    @FXML private void goToCategories() { navigateTo("/view/categories.fxml"); }
    @FXML private void goToEnergie()    { navigateTo("/view/energie.fxml"); }
    @FXML private void goToReco()       { navigateTo("/view/recommandations.fxml"); }
    @FXML private void goToAliments()   { navigateTo("/view/aliments/ListAliment.fxml"); }
    @FXML private void goToCours()      { navigateTo("/view/cours.fxml"); }
    @FXML private void goToChapitres()  { navigateTo("/view/chapitres.fxml"); }
    @FXML private void goToStats()      { navigateTo("/view/stats.fxml"); }
    @FXML private void goToProfil()     { navigateTo("/view/profil.fxml"); }
}

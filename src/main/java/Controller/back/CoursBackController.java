package Controller.back;

import Models.Cours;
import Services.Iservice;
import Services.ServiceCours;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.sql.SQLDataException;
import java.util.List;
import java.util.stream.Collectors;

public class CoursBackController {


    @FXML private TableView<Cours>            coursTable;
    @FXML private TableColumn<Cours, Integer> colId;
    @FXML private TableColumn<Cours, String>  colTitre;
    @FXML private TableColumn<Cours, String>  colCategorie;
    @FXML private TableColumn<Cours, String>  colNiveau;
    @FXML private TableColumn<Cours, Integer> colDuree;
    @FXML private TableColumn<Cours, String>  colStatus;
    @FXML private TableColumn<Cours, String>  colDateCreation;
    @FXML private TableColumn<Cours, Void>    colActions;


    @FXML private TextField        searchField;
    @FXML private ComboBox<String> filterNiveau;
    @FXML private ComboBox<String> filterCategorie;


    @FXML private Label statTotal;
    @FXML private Label statDebutant;
    @FXML private Label statIntermediaire;
    @FXML private Label statAvance;
    @FXML private Label coursCountLabel;

    // ── State ──────────────────────────────────────────────────────────────────
    private final Iservice<Cours> service;
    private ObservableList<Cours> allCours;
    private Cours                 editingCours = null;

    public CoursBackController() {
        this.service = new ServiceCours();
    }

    @FXML
    public void initialize() {
        setupColumns();
        setupFilters();
        setupSearchListener();
        loadData();
    }



    private void setupColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitre.setCellValueFactory(new PropertyValueFactory<>("tittre"));
        colCategorie.setCellValueFactory(new PropertyValueFactory<>("categorie"));
        colNiveau.setCellValueFactory(new PropertyValueFactory<>("niveau"));
        colDuree.setCellValueFactory(new PropertyValueFactory<>("dureeEstimee"));
        colDateCreation.setCellValueFactory(new PropertyValueFactory<>("dateCreation"));


        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) { setGraphic(null); return; }
                Label badge = new Label(status);
                String color = switch (status.toLowerCase()) {
                    case "actif"      -> "-fx-background-color: rgba(52,211,153,0.18); -fx-text-fill: #34d399;";
                    case "inactif"    -> "-fx-background-color: rgba(220,38,38,0.18);  -fx-text-fill: #ff6b6b;";
                    case "en attente" -> "-fx-background-color: rgba(212,165,116,0.18); -fx-text-fill: #d4a574;";
                    case "en cours"   -> "-fx-background-color: rgba(14,165,233,0.18); -fx-text-fill: #0ea5e9;";
                    default           -> "-fx-background-color: rgba(255,255,255,0.08); -fx-text-fill: #f5f5f4;";
                };
                badge.setStyle(color + "-fx-background-radius: 20; -fx-padding: 4 10; -fx-font-size: 11px; -fx-font-weight: bold;");
                setGraphic(badge);
                setText(null);
            }
        });


        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit   = new Button("✏");
            private final Button btnDelete = new Button("🗑");
            private final HBox   box       = new HBox(6, btnEdit, btnDelete);

            {
                btnEdit.setStyle(
                        "-fx-background-color: rgba(52,211,153,0.10);" +
                        "-fx-background-radius: 8; -fx-border-color: rgba(52,211,153,0.20);" +
                        "-fx-border-radius: 8; -fx-border-width: 1;" +
                        "-fx-text-fill: #34d399; -fx-cursor: hand;" +
                        "-fx-min-width: 32; -fx-min-height: 32; -fx-max-width: 32; -fx-max-height: 32;");
                btnDelete.setStyle(
                        "-fx-background-color: rgba(220,38,38,0.10);" +
                        "-fx-background-radius: 8; -fx-border-color: rgba(220,38,38,0.20);" +
                        "-fx-border-radius: 8; -fx-border-width: 1;" +
                        "-fx-text-fill: #ff6b6b; -fx-cursor: hand;" +
                        "-fx-min-width: 32; -fx-min-height: 32; -fx-max-width: 32; -fx-max-height: 32;");
                box.setAlignment(Pos.CENTER_LEFT);

                btnEdit.setOnAction(e -> {
                    Cours c = getTableView().getItems().get(getIndex());
                    editingCours = c;
                    openDialog(c);
                });
                btnDelete.setOnAction(e -> {
                    Cours c = getTableView().getItems().get(getIndex());
                    handleDelete(c);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void setupFilters() {
        filterNiveau.getItems().add("Tous les niveaux");
        filterNiveau.getItems().addAll("Beginner", "intermediate", "Advanced");
        filterNiveau.setValue("Tous les niveaux");

        filterCategorie.getItems().add("Toutes les catégories");
        filterCategorie.getItems().addAll("Programming", "Design", "Marketing", "Business", "Science", "Autre");
        filterCategorie.setValue("Toutes les catégories");
    }

    private void setupSearchListener() {
        searchField.textProperty().addListener((obs, o, n) -> applyFilters());
    }



    private void loadData() {
        try {
            List<Cours> list = service.recuperer();
            allCours = FXCollections.observableArrayList(list);
            applyFilters();
            updateStats(list);
        } catch (SQLDataException e) {
            showAlert("Erreur", "Impossible de charger les cours : " + e.getMessage());
        }
    }

    private void updateStats(List<Cours> list) {
        statTotal.setText(String.valueOf(list.size()));
        statDebutant.setText(String.valueOf(
                list.stream().filter(c -> "Beginner".equalsIgnoreCase(c.getNiveau())).count()));
        statIntermediaire.setText(String.valueOf(
                list.stream().filter(c -> "intermediate".equalsIgnoreCase(c.getNiveau())).count()));
        statAvance.setText(String.valueOf(
                list.stream().filter(c -> "Advanced".equalsIgnoreCase(c.getNiveau())).count()));
    }



    @FXML
    private void handleFilter() { applyFilters(); }

    @FXML
    private void handleResetFilters() {
        searchField.clear();
        filterNiveau.setValue("Tous les niveaux");
        filterCategorie.setValue("Toutes les catégories");
    }

    @FXML
    private void handleSearch() { applyFilters(); }

    private void applyFilters() {
        if (allCours == null) return;
        String search  = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
        String niveau  = filterNiveau.getValue();
        String cat     = filterCategorie.getValue();

        List<Cours> filtered = allCours.stream().filter(c -> {
            boolean matchSearch = search.isBlank()
                    || c.getTittre().toLowerCase().contains(search)
                    || c.getCategorie().toLowerCase().contains(search);
            boolean matchNiveau = niveau == null || niveau.startsWith("Tous")
                    || c.getNiveau().equalsIgnoreCase(niveau);
            boolean matchCat    = cat    == null || cat.startsWith("Toutes")
                    || c.getCategorie().equalsIgnoreCase(cat);
            return matchSearch && matchNiveau && matchCat;
        }).collect(Collectors.toList());

        coursTable.setItems(FXCollections.observableArrayList(filtered));
        coursCountLabel.setText(filtered.size() + " cours au total");
    }



    @FXML
    private void handleOpenAddDialog() {
        editingCours = null;
        openDialog(null);
    }

    private void handleDelete(Cours cours) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer le cours \"" + cours.getTittre() + "\" ?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) {
                try {
                    service.supprimer(cours);
                    loadData();
                } catch (SQLDataException e) {
                    showAlert("Erreur", "Impossible de supprimer : " + e.getMessage());
                }
            }
        });
    }


    private void openDialog(Cours cours) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.TRANSPARENT);

        // Fields
        TextField      fTitre       = dialogField("Titre du cours");
        TextField      fDuree       = dialogField("Durée en heures (ex: 10)");
        TextArea       fDescription = dialogArea("Description du cours...");
        ComboBox<String> fNiveau    = dialogCombo("Sélectionner le niveau", "Beginner", "intermediate", "Advanced");
        ComboBox<String> fCategorie = dialogCombo("Sélectionner la catégorie", "Programming", "Design", "Marketing", "Business", "Autre");
        ComboBox<String> fStatus    = dialogCombo("Statut",  "inactif", "en attente", "en cours");

        if (cours != null) {
            fTitre.setText(cours.getTittre());
            fDuree.setText(String.valueOf(cours.getDureeEstimee()));
            fDescription.setText(cours.getDescription());
            fNiveau.setValue(cours.getNiveau());
            fCategorie.setValue(cours.getCategorie());
            fStatus.setValue(cours.getStatus());
        } else {
            fStatus.setValue("actif");
        }

        Label errorLabel = new Label("");
        errorLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 12px;");

        VBox form = new VBox(14,
                dialogBlock("Titre", fTitre),
                dialogRow(dialogBlock("Niveau", fNiveau), dialogBlock("Catégorie", fCategorie)),
                dialogRow(dialogBlock("Durée (heures)", fDuree), dialogBlock("Statut", fStatus)),
                dialogBlock("Description", fDescription),
                errorLabel
        );

        // Buttons
        Button btnCancel = new Button("Annuler");
        btnCancel.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 10;" +
                "-fx-border-color: rgba(255,255,255,0.09); -fx-border-radius: 10; -fx-border-width: 1;" +
                "-fx-text-fill: #f5f5f4; -fx-padding: 10 24; -fx-cursor: hand;");
        btnCancel.setOnAction(e -> dialog.close());

        Button btnSave = new Button(cours == null ? "+ Ajouter" : "Enregistrer");
        btnSave.setStyle("-fx-background-color: linear-gradient(to bottom right, #059669, #34d399);" +
                "-fx-background-radius: 10; -fx-text-fill: #ffffff; -fx-font-weight: bold;" +
                "-fx-padding: 10 24; -fx-cursor: hand;");

        btnSave.setOnAction(e -> {
            if (fTitre.getText().isBlank())    { errorLabel.setText("Le titre est obligatoire.");     return; }
            if (fNiveau.getValue() == null)    { errorLabel.setText("Le niveau est obligatoire.");    return; }
            if (fCategorie.getValue() == null) { errorLabel.setText("La catégorie est obligatoire."); return; }
            if (fDuree.getText().isBlank())    { errorLabel.setText("La durée est obligatoire.");     return; }
            int duree;
            try {
                duree = Integer.parseInt(fDuree.getText().trim());
                if (duree <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                errorLabel.setText("La durée doit être un nombre positif.");
                return;
            }
            try {
                if (editingCours == null) {
                    Cours c = new Cours();
                    c.setTittre(fTitre.getText().trim());
                    c.setDescription(fDescription.getText().trim());
                    c.setNiveau(fNiveau.getValue());
                    c.setCategorie(fCategorie.getValue());
                    c.setDureeEstimee(duree);
                    c.setStatus(fStatus.getValue() != null ? fStatus.getValue() : "actif");
                    c.setDateCreation(java.time.LocalDate.now().toString());
                    service.ajouter(c);
                } else {
                    editingCours.setTittre(fTitre.getText().trim());
                    editingCours.setDescription(fDescription.getText().trim());
                    editingCours.setNiveau(fNiveau.getValue());
                    editingCours.setCategorie(fCategorie.getValue());
                    editingCours.setDureeEstimee(duree);
                    editingCours.setStatus(fStatus.getValue());
                    service.modifier(editingCours);
                }
                dialog.close();
                loadData();
            } catch (SQLDataException ex) {
                errorLabel.setText("Erreur base de données : " + ex.getMessage());
            }
        });

        HBox buttons = new HBox(12, btnCancel, btnSave);
        buttons.setAlignment(Pos.CENTER_RIGHT);


        Label dlgTitle = new Label(cours == null ? "Nouveau Cours" : "Modifier le Cours");
        dlgTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #f5f5f4;");
        Button btnX = new Button("✕");
        btnX.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(245,245,244,0.50);" +
                "-fx-font-size: 16px; -fx-cursor: hand;");
        btnX.setOnAction(e -> dialog.close());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(dlgTitle, spacer, btnX);
        header.setAlignment(Pos.CENTER_LEFT);

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: rgba(255,255,255,0.08);");

        VBox root = new VBox(20, header, sep, form, buttons);
        root.setPadding(new Insets(28));
        root.setStyle(
                "-fx-background-color: #0d1a14;" +
                "-fx-background-radius: 14;" +
                "-fx-border-radius: 14;" +
                "-fx-border-width: 1;" +
                "-fx-border-color: rgba(255,255,255,0.09);" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.60), 30, 0, 0, 8);"
        );
        root.setPrefWidth(560);

        javafx.geometry.Rectangle2D screen = javafx.stage.Screen.getPrimary().getVisualBounds();
        StackPane overlay = new StackPane(root);
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.70);");

        Scene scene = new Scene(overlay, screen.getWidth(), screen.getHeight());
        scene.setFill(Color.TRANSPARENT);
        dialog.setScene(scene);
        dialog.show();
    }



    private TextField dialogField(String prompt) {
        TextField f = new TextField();
        f.setPromptText(prompt);
        f.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 10;" +
                "-fx-border-color: rgba(255,255,255,0.09); -fx-border-radius: 10; -fx-border-width: 1;" +
                "-fx-text-fill: #f5f5f4; -fx-prompt-text-fill: rgba(245,245,244,0.35);" +
                "-fx-padding: 11 15; -fx-font-size: 13px;");
        return f;
    }

    private TextArea dialogArea(String prompt) {
        TextArea a = new TextArea();
        a.setPromptText(prompt);
        a.setPrefRowCount(3);
        a.setWrapText(true);
        a.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 10;" +
                "-fx-border-color: rgba(255,255,255,0.09); -fx-border-radius: 10; -fx-border-width: 1;" +
                "-fx-text-fill: #f5f5f4; -fx-prompt-text-fill: rgba(245,245,244,0.35);" +
                "-fx-padding: 10 14; -fx-font-size: 13px;");
        return a;
    }

    private ComboBox<String> dialogCombo(String prompt, String... items) {
        ComboBox<String> c = new ComboBox<>();
        c.setPromptText(prompt);
        c.getItems().addAll(items);
        c.setMaxWidth(Double.MAX_VALUE);
        c.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 10;" +
                "-fx-border-color: rgba(255,255,255,0.09); -fx-border-radius: 10; -fx-border-width: 1;");
        return c;
    }

    private VBox dialogBlock(String label, javafx.scene.Node field) {
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: rgba(245,245,244,0.65);");
        VBox block = new VBox(6, lbl, field);
        VBox.setVgrow(field, Priority.ALWAYS);
        return block;
    }

    private HBox dialogRow(VBox left, VBox right) {
        HBox row = new HBox(14, left, right);
        HBox.setHgrow(left, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);
        return row;
    }



    @FXML private void goToDashboard()  { navigate("/views/back/Dashboard.fxml"); }
    @FXML private void goToChapitres() { navigate("/views/back/ChapitreBack.fxml"); }
    @FXML private void goToAliments()  { navigate("/views/aliment/ListAliment.fxml"); }
    @FXML private void goToRepas()     { navigate("/views/repas/ListRepas.fxml"); }
    @FXML private void goToActivites() { navigate("/views/activite/ListActivite.fxml"); }
    @FXML private void goToDepenses()  { navigate("/views/depense/ListDepense.fxml"); }

    private void navigate(String path) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(path));
            coursTable.getScene().setRoot(root);
        } catch (IOException e) {
            showAlert("Navigation", "Impossible d'ouvrir : " + path);
        }
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}

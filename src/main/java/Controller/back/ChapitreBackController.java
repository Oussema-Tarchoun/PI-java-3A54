package Controller.back;

import Models.Chapitre;
import Models.Cours;
import Services.ServiceChapitre;
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

public class ChapitreBackController {

    // Tablesss
    @FXML private TableView<Chapitre>            chapitreTable;
    @FXML private TableColumn<Chapitre, Integer> colId;
    @FXML private TableColumn<Chapitre, String>  colTitre;
    @FXML private TableColumn<Chapitre, String>  colCours;
    @FXML private TableColumn<Chapitre, Integer> colOrdre;
    @FXML private TableColumn<Chapitre, String>  colContenu;
    @FXML private TableColumn<Chapitre, String>  colExercice;
    @FXML private TableColumn<Chapitre, Void>    colActions;

    //  tri w recherche
    @FXML private TextField          searchField;
    @FXML private ComboBox<Cours>    filterCours;

    // statsss
    @FXML private Label statTotal;
    @FXML private Label statAvecExercice;
    @FXML private Label statSansExercice;
    @FXML private Label statCoursCovered;
    @FXML private Label chapitreCountLabel;


    private final ServiceChapitre  service;
    private final ServiceCours     serviceCours;
    private ObservableList<Chapitre> allChapitres;
    private List<Cours>              allCoursList;
    private Chapitre                 editingChapitre = null;

    public ChapitreBackController() {
        this.service      = new ServiceChapitre();
        this.serviceCours = new ServiceCours();
    }

    @FXML
    public void initialize() {
        setupColumns();
        setupCourseFilter();
        setupSearchListener();
        loadData();
    }



    private void setupColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colOrdre.setCellValueFactory(new PropertyValueFactory<>("ordre"));


        colContenu.setCellValueFactory(new PropertyValueFactory<>("contenu"));
        colContenu.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                setText(item.length() > 60 ? item.substring(0, 60) + "…" : item);
            }
        });


        colCours.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setText(null); return; }
                Chapitre ch = getTableView().getItems().get(getIndex());
                if (allCoursList == null) { setText("—"); return; }
                allCoursList.stream()
                        .filter(c -> c.getId() == ch.getCoursId())
                        .findFirst()
                        .ifPresentOrElse(c -> setText(c.getTittre()), () -> setText("—"));
            }
        });

        // Exercise indicator
        colExercice.setCellValueFactory(new PropertyValueFactory<>("exercise"));
        colExercice.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                boolean has = item != null && !item.isBlank();
                Label badge = new Label(has ? "✔ Oui" : "✘ Non");
                badge.setStyle((has
                        ? "-fx-background-color: rgba(52,211,153,0.18); -fx-text-fill: #34d399;"
                        : "-fx-background-color: rgba(255,255,255,0.06); -fx-text-fill: rgba(245,245,244,0.40);") +
                        "-fx-background-radius: 20; -fx-padding: 4 10; -fx-font-size: 11px; -fx-font-weight: bold;");
                setGraphic(badge);
                setText(null);
            }
        });

        // les action
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
                    Chapitre ch = getTableView().getItems().get(getIndex());
                    editingChapitre = ch;
                    openDialog(ch);
                });
                btnDelete.setOnAction(e -> {
                    Chapitre ch = getTableView().getItems().get(getIndex());
                    handleDelete(ch);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void setupCourseFilter() {
        try {
            allCoursList = serviceCours.recuperer();
            filterCours.getItems().add(null); // "All"
            filterCours.getItems().addAll(allCoursList);
            filterCours.setPromptText("Tous les cours");
            filterCours.setCellFactory(lv -> new ListCell<>() {
                @Override protected void updateItem(Cours c, boolean empty) {
                    super.updateItem(c, empty);
                    setText(empty ? null : (c == null ? "Tous les cours" : c.getTittre()));
                }
            });
            filterCours.setButtonCell(new ListCell<>() {
                @Override protected void updateItem(Cours c, boolean empty) {
                    super.updateItem(c, empty);
                    setText(empty ? null : (c == null ? "Tous les cours" : c.getTittre()));
                }
            });
            filterCours.setOnAction(e -> applyFilters());
        } catch (SQLDataException e) {
            showAlert("Erreur", "Impossible de charger les cours : " + e.getMessage());
        }
    }

    private void setupSearchListener() {
        searchField.textProperty().addListener((obs, o, n) -> applyFilters());
    }



    private void loadData() {
        try {
            // Load ALL chapters across all courses
            List<Chapitre> list = new java.util.ArrayList<>();
            if (allCoursList != null) {
                for (Cours c : allCoursList) {
                    list.addAll(service.getByCours(c.getId()));
                }
            }
            allChapitres = FXCollections.observableArrayList(list);
            applyFilters();
            updateStats(list);
        } catch (SQLDataException e) {
            showAlert("Erreur", "Impossible de charger les chapitres : " + e.getMessage());
        }
    }

    private void updateStats(List<Chapitre> list) {
        statTotal.setText(String.valueOf(list.size()));
        long avec = list.stream().filter(c -> c.getExercise() != null && !c.getExercise().isBlank()).count();
        statAvecExercice.setText(String.valueOf(avec));
        statSansExercice.setText(String.valueOf(list.size() - avec));
        long covered = allCoursList == null ? 0 : allCoursList.stream()
                .filter(c -> list.stream().anyMatch(ch -> ch.getCoursId() == c.getId()))
                .count();
        statCoursCovered.setText(String.valueOf(covered));
    }



    @FXML private void handleFilter()       { applyFilters(); }
    @FXML private void handleSearch()       { applyFilters(); }

    @FXML
    private void handleResetFilters() {
        searchField.clear();
        filterCours.setValue(null);
    }

    private void applyFilters() {
        if (allChapitres == null) return;
        String search = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
        Cours  cours  = filterCours.getValue();

        List<Chapitre> filtered = allChapitres.stream().filter(ch -> {
            boolean matchSearch = search.isBlank()
                    || ch.getTitre().toLowerCase().contains(search)
                    || (ch.getContenu() != null && ch.getContenu().toLowerCase().contains(search));
            boolean matchCours  = cours == null || ch.getCoursId() == cours.getId();
            return matchSearch && matchCours;
        }).collect(Collectors.toList());

        chapitreTable.setItems(FXCollections.observableArrayList(filtered));
        chapitreCountLabel.setText(filtered.size() + " chapitres au total");
    }



    @FXML
    private void handleOpenAddDialog() {
        editingChapitre = null;
        openDialog(null);
    }

    private void handleDelete(Chapitre ch) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer le chapitre \"" + ch.getTitre() + "\" ?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) {
                try {
                    service.supprimer(ch);
                    loadData();
                } catch (SQLDataException e) {
                    showAlert("Erreur", "Impossible de supprimer : " + e.getMessage());
                }
            }
        });
    }


    private void openDialog(Chapitre ch) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.TRANSPARENT);

        TextField      fTitre    = dialogField("Titre du chapitre");
        TextField      fOrdre    = dialogField("Numéro d'ordre (ex: 1)");
        TextArea       fContenu  = dialogArea("Contenu du chapitre...");
        TextArea       fExercice = dialogArea("Exercice (optionnel)...");
        ComboBox<Cours> fCours   = new ComboBox<>();
        fCours.setMaxWidth(Double.MAX_VALUE);
        fCours.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 10;" +
                "-fx-border-color: rgba(255,255,255,0.09); -fx-border-radius: 10; -fx-border-width: 1;");
        if (allCoursList != null) {
            fCours.getItems().addAll(allCoursList);
            fCours.setCellFactory(lv -> new ListCell<>() {
                @Override protected void updateItem(Cours c, boolean empty) {
                    super.updateItem(c, empty);
                    setText(empty || c == null ? null : c.getTittre());
                }
            });
            fCours.setButtonCell(new ListCell<>() {
                @Override protected void updateItem(Cours c, boolean empty) {
                    super.updateItem(c, empty);
                    setText(empty || c == null ? null : c.getTittre());
                }
            });
        }

        if (ch != null) {
            fTitre.setText(ch.getTitre());
            fOrdre.setText(String.valueOf(ch.getOrdre()));
            fContenu.setText(ch.getContenu());
            fExercice.setText(ch.getExercise() != null ? ch.getExercise() : "");
            if (allCoursList != null) {
                allCoursList.stream().filter(c -> c.getId() == ch.getCoursId())
                        .findFirst().ifPresent(fCours::setValue);
            }
        }

        Label errorLabel = new Label("");
        errorLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 12px;");

        VBox form = new VBox(14,
                dialogBlock("Cours", fCours),
                dialogRow(dialogBlock("Titre", fTitre), dialogBlock("Ordre", fOrdre)),
                dialogBlock("Contenu", fContenu),
                dialogBlock("Exercice (optionnel)", fExercice),
                errorLabel
        );

        Button btnCancel = new Button("Annuler");
        btnCancel.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 10;" +
                "-fx-border-color: rgba(255,255,255,0.09); -fx-border-radius: 10; -fx-border-width: 1;" +
                "-fx-text-fill: #f5f5f4; -fx-padding: 10 24; -fx-cursor: hand;");
        btnCancel.setOnAction(e -> dialog.close());

        Button btnSave = new Button(ch == null ? "+ Ajouter" : "Enregistrer");
        btnSave.setStyle("-fx-background-color: linear-gradient(to bottom right, #059669, #34d399);" +
                "-fx-background-radius: 10; -fx-text-fill: #ffffff; -fx-font-weight: bold;" +
                "-fx-padding: 10 24; -fx-cursor: hand;");

        btnSave.setOnAction(e -> {
            if (fCours.getValue() == null)  { errorLabel.setText("Sélectionnez un cours.");     return; }
            if (fTitre.getText().isBlank())  { errorLabel.setText("Le titre est obligatoire.");  return; }
            if (fContenu.getText().isBlank()){ errorLabel.setText("Le contenu est obligatoire."); return; }
            if (fOrdre.getText().isBlank())  { errorLabel.setText("L'ordre est obligatoire.");   return; }
            int ordre;
            try {
                ordre = Integer.parseInt(fOrdre.getText().trim());
                if (ordre <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                errorLabel.setText("L'ordre doit être un nombre positif.");
                return;
            }
            try {
                if (editingChapitre == null) {
                    Chapitre newCh = new Chapitre();
                    newCh.setTitre(fTitre.getText().trim());
                    newCh.setContenu(fContenu.getText().trim());
                    newCh.setOrdre(ordre);
                    newCh.setExercise(fExercice.getText().trim());
                    service.ajouter(newCh, fCours.getValue().getId());
                } else {
                    editingChapitre.setTitre(fTitre.getText().trim());
                    editingChapitre.setContenu(fContenu.getText().trim());
                    editingChapitre.setOrdre(ordre);
                    editingChapitre.setExercise(fExercice.getText().trim());
                    service.modifier(editingChapitre);
                }
                dialog.close();
                loadData();
            } catch (SQLDataException ex) {
                errorLabel.setText("Erreur base de données : " + ex.getMessage());
            }
        });

        HBox buttons = new HBox(12, btnCancel, btnSave);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        Label dlgTitle = new Label(ch == null ? "Nouveau Chapitre" : "Modifier le Chapitre");
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


// style mta text area
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



    @FXML private void goToDashboard() { navigate("/views/back/Dashboard.fxml"); }
    @FXML private void goToCours()     { navigate("/views/back/CoursBack.fxml"); }
    @FXML private void goToAliments()  { navigate("/views/aliment/ListAliment.fxml"); }
    @FXML private void goToRepas()     { navigate("/views/repas/ListRepas.fxml"); }
    @FXML private void goToActivites() { navigate("/views/activite/ListActivite.fxml"); }
    @FXML private void goToDepenses()  { navigate("/views/depense/ListDepense.fxml"); }

    private void navigate(String path) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(path));
            chapitreTable.getScene().setRoot(root);
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

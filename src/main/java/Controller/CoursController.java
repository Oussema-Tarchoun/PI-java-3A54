package Controller;

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
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.sql.SQLDataException;
import java.util.List;
import java.util.stream.Collectors;

public class CoursController {

    // ── FXML bindings ──────────────────────────────────────────────────────────
    @FXML private FlowPane         courseGrid;
    @FXML private VBox             emptyState;
    @FXML private TextField        txtSearch;
    @FXML private ComboBox<String> comboFilterCategorie;
    @FXML private ComboBox<String> comboFilterNiveau;
    @FXML private ComboBox<String> comboFilterStatus;
    @FXML private Label            lblResultCount;
    @FXML private Button           btnAdd;

    // ── Stats ──────────────────────────────────────────────────────────────────
    @FXML private Label lblTotalCours;
    @FXML private Label lblDebutant;
    @FXML private Label lblIntermediaire;
    @FXML private Label lblAvance;

    // ── State ──────────────────────────────────────────────────────────────────
    private final Iservice<Cours> service;
    private ObservableList<Cours> allCours;
    private Cours                 editingCours = null;   // null = add mode

    public CoursController() {
        this.service = new ServiceCours();
    }

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        initializeFilters();
        setupFilterListeners();
        loadData();
    }

    // ── Filters ────────────────────────────────────────────────────────────────

    private void initializeFilters() {
        comboFilterCategorie.getItems().add("All Categories");
        comboFilterCategorie.getItems().addAll("Programmation", "Design", "Marketing", "Business", "Autre");
        comboFilterCategorie.setValue("All Categories");

        comboFilterNiveau.getItems().add("All Levels");
        comboFilterNiveau.getItems().addAll("debutant", "intermediaire", "avance");
        comboFilterNiveau.setValue("All Levels");

        comboFilterStatus.getItems().add("All Statuses");
        comboFilterStatus.getItems().addAll("actif", "inactif", "en attente", "en cours");
        comboFilterStatus.setValue("All Statuses");
    }

    private void setupFilterListeners() {
        txtSearch.textProperty().addListener((obs, o, n) -> applyFilters());
        comboFilterCategorie.valueProperty().addListener((obs, o, n) -> applyFilters());
        comboFilterNiveau.valueProperty().addListener((obs, o, n) -> applyFilters());
        comboFilterStatus.valueProperty().addListener((obs, o, n) -> applyFilters());
    }

    @FXML
    private void handleResetFilters() {
        txtSearch.clear();
        comboFilterCategorie.setValue("All Categories");
        comboFilterNiveau.setValue("All Levels");
        comboFilterStatus.setValue("All Statuses");
    }

    private void applyFilters() {
        if (allCours == null) return;

        String search   = txtSearch.getText() == null ? "" : txtSearch.getText().toLowerCase();
        String cat      = comboFilterCategorie.getValue();
        String niveau   = comboFilterNiveau.getValue();
        String status   = comboFilterStatus.getValue();

        List<Cours> filtered = allCours.stream().filter(c -> {
            boolean matchSearch = search.isBlank()
                    || c.getTittre().toLowerCase().contains(search)
                    || c.getCategorie().toLowerCase().contains(search)
                    || c.getNiveau().toLowerCase().contains(search);
            boolean matchCat    = cat    == null || cat.startsWith("All")    || c.getCategorie().equalsIgnoreCase(cat);
            boolean matchNiveau = niveau == null || niveau.startsWith("All") || c.getNiveau().equalsIgnoreCase(niveau);
            boolean matchStatus = status == null || status.startsWith("All") || c.getStatus().equalsIgnoreCase(status);
            return matchSearch && matchCat && matchNiveau && matchStatus;
        }).collect(Collectors.toList());

        renderCards(filtered);
        lblResultCount.setText(filtered.size() + " course" + (filtered.size() != 1 ? "s" : "") + " found");
    }

    // ── Data ───────────────────────────────────────────────────────────────────

    private void loadData() {
        try {
            List<Cours> list = service.recuperer();
            allCours = FXCollections.observableArrayList(list);
            applyFilters();
            updateStats(list);
        } catch (SQLDataException e) {
            showAlert("Error", "Could not load courses: " + e.getMessage());
        }
    }

    private void updateStats(List<Cours> list) {
        if (lblTotalCours    != null) lblTotalCours.setText(String.valueOf(list.size()));
        if (lblDebutant      != null) lblDebutant.setText(String.valueOf(
                list.stream().filter(c -> "debutant".equalsIgnoreCase(c.getNiveau())).count()));
        if (lblIntermediaire != null) lblIntermediaire.setText(String.valueOf(
                list.stream().filter(c -> "intermediaire".equalsIgnoreCase(c.getNiveau())).count()));
        if (lblAvance        != null) lblAvance.setText(String.valueOf(
                list.stream().filter(c -> "avance".equalsIgnoreCase(c.getNiveau())).count()));
    }

    // ── Card rendering ─────────────────────────────────────────────────────────

    private void renderCards(List<Cours> list) {
        courseGrid.getChildren().clear();

        boolean isEmpty = list == null || list.isEmpty();
        emptyState.setVisible(isEmpty);
        emptyState.setManaged(isEmpty);

        if (isEmpty) return;

        for (Cours cours : list) {
            courseGrid.getChildren().add(buildCard(cours));
        }
    }

    private VBox buildCard(Cours cours) {
        // ── Card container ────────────────────────────────────────────────────
        VBox card = new VBox(12);
        card.setPrefWidth(380);
        card.setMaxWidth(380);
        card.setPadding(new Insets(20));
        card.setStyle(
                "-fx-background-color: #161b2e;" +
                        "-fx-background-radius: 14;" +
                        "-fx-border-radius: 14;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-color: #2a3142;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 16, 0, 0, 4);" +
                        "-fx-cursor: hand;"
        );

        // ── Top row: title + status badge ─────────────────────────────────────
        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label(cours.getTittre());
        titleLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(260);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        Label statusBadge = buildStatusBadge(cours.getStatus());

        topRow.getChildren().addAll(titleLabel, statusBadge);

        // ── Meta row: category · level · duration ─────────────────────────────
        HBox metaRow = new HBox(14);
        metaRow.setAlignment(Pos.CENTER_LEFT);
        metaRow.getChildren().addAll(
                metaChip("🏷 " + cours.getCategorie()),
                metaChip("📶 " + cours.getNiveau()),
                metaChip("⏱ " + cours.getDureeEstimee() + "h")
        );

        // ── Description ───────────────────────────────────────────────────────
        Label descLabel = new Label(cours.getDescription() == null || cours.getDescription().isBlank()
                ? "No description provided." : cours.getDescription());
        descLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        descLabel.setWrapText(true);
        descLabel.setMaxHeight(40);

        // ── Action buttons ────────────────────────────────────────────────────
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);


        Button btnEdit     = iconButton("✏️", "#8b5cf6", "Edit");
        Button btnDelete   = iconButton("🗑", "#ef4444", "Delete");


        btnEdit.setOnAction(e -> handleEdit(cours));
        btnDelete.setOnAction(e -> handleDelete(cours));

        actions.getChildren().addAll( btnEdit, btnDelete);

        card.getChildren().addAll(topRow, metaRow, descLabel, actions);
        return card;
    }

    /** Coloured status pill matching the website design */
    private Label buildStatusBadge(String status) {
        Label badge = new Label(status == null ? "—" : status);
        badge.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 4 10;");
        if (status == null) return badge;
        switch (status.toLowerCase()) {
            case "actif"      -> badge.setStyle(badge.getStyle() + "-fx-background-color: rgba(0,212,255,0.15); -fx-text-fill: #00d4ff;");
            case "inactif"    -> badge.setStyle(badge.getStyle() + "-fx-background-color: rgba(239,68,68,0.15);  -fx-text-fill: #ef4444;");
            case "en attente" -> badge.setStyle(badge.getStyle() + "-fx-background-color: rgba(245,158,11,0.15); -fx-text-fill: #f59e0b;");
            case "en cours"   -> badge.setStyle(badge.getStyle() + "-fx-background-color: rgba(16,185,129,0.15); -fx-text-fill: #10b981;");
            default           -> badge.setStyle(badge.getStyle() + "-fx-background-color: rgba(100,116,139,0.15); -fx-text-fill: #64748b;");
        }
        return badge;
    }

    /** Small meta chip (category / level / duration) */
    private Label metaChip(String text) {
        Label chip = new Label(text);
        chip.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
        return chip;
    }

    /** Round icon button */
    private Button iconButton(String icon, String color, String tooltip) {
        Button btn = new Button(icon);
        btn.setTooltip(new Tooltip(tooltip));
        btn.setStyle(
                "-fx-background-color: rgba(255,255,255,0.06);" +
                        "-fx-text-fill: " + color + ";" +
                        "-fx-background-radius: 8;" +
                        "-fx-font-size: 14px;" +
                        "-fx-padding: 8 12;" +
                        "-fx-cursor: hand;"
        );
        return btn;
    }

    // ── CRUD actions ───────────────────────────────────────────────────────────

    /** Opens dialog in ADD mode */
    @FXML
    private void handleAdd() {
        editingCours = null;
        openDialog(null);
    }

    /** Opens dialog in EDIT mode with course pre-filled */
    private void handleEdit(Cours cours) {
        editingCours = cours;
        openDialog(cours);
    }

    private void handleDelete(Cours cours) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete \"" + cours.getTittre() + "\"?", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    service.supprimer(cours);
                    loadData();
                } catch (SQLDataException e) {
                    showAlert("Error", "Could not delete course: " + e.getMessage());
                }
            }
        });
    }

    private void openChapitres(Cours cours) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Chapitre.fxml"));
            Parent root = loader.load();
            ChapitreController controller = loader.getController();
            controller.setCours(cours);

            Stage stage = new Stage();
            stage.setTitle("Chapters — " + cours.getTittre());
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            showAlert("Error", "Could not open chapters: " + e.getMessage());
        }
    }

    // ── Dialog ─────────────────────────────────────────────────────────────────

    private void openDialog(Cours cours) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.setTitle(cours == null ? "Add Course" : "Edit Course");

        // ── Fields ────────────────────────────────────────────────────────────
        TextField      fTitre       = styledField("Course title");
        TextField      fDuree       = styledField("Duration in hours (e.g. 10)");
        TextArea       fDescription = styledArea("Course description");
        ComboBox<String> fNiveau    = styledCombo("Select level",    "debutant", "intermediaire", "avance");
        ComboBox<String> fCategorie = styledCombo("Select category", "Programmation", "Design", "Marketing", "Business", "Autre");

        if (cours != null) {
            fTitre.setText(cours.getTittre());
            fDuree.setText(String.valueOf(cours.getDureeEstimee()));
            fDescription.setText(cours.getDescription());
            fNiveau.setValue(cours.getNiveau());
            fCategorie.setValue(cours.getCategorie());
        }

        // ── Layout ────────────────────────────────────────────────────────────
        Label errorLabel = new Label("");
        errorLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px;");

        VBox form = new VBox(14);
        form.getChildren().addAll(
                fieldBlock("Course Title",     fTitre),
                rowBlock(fieldBlock("Level", fNiveau), fieldBlock("Category", fCategorie)),
                fieldBlock("Duration (hours)", fDuree),
                fieldBlock("Description",      fDescription),
                errorLabel
        );

        Button btnCancel = new Button("Cancel");
        btnCancel.setStyle("-fx-background-color: #1e2538; -fx-text-fill: #ffffff; -fx-background-radius: 8; -fx-padding: 10 24; -fx-cursor: hand;");
        btnCancel.setOnAction(e -> dialog.close());

        Button btnSave = new Button(cours == null ? "Add Course" : "Save Changes");
        btnSave.setStyle("-fx-background-color: #00d4ff; -fx-text-fill: #0a0e1a; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10 24; -fx-cursor: hand;");
        btnSave.setOnAction(e -> {
            // Validate
            if (fTitre.getText().isBlank()) { errorLabel.setText("Title is required."); return; }
            if (fNiveau.getValue() == null) { errorLabel.setText("Level is required."); return; }
            if (fCategorie.getValue() == null) { errorLabel.setText("Category is required."); return; }
            if (fDuree.getText().isBlank())  { errorLabel.setText("Duration is required."); return; }
            int duree;
            try { duree = Integer.parseInt(fDuree.getText().trim()); if (duree <= 0) throw new NumberFormatException(); }
            catch (NumberFormatException ex) { errorLabel.setText("Duration must be a positive number."); return; }

            try {
                if (editingCours == null) {
                    // ADD
                    Cours newCours = new Cours();
                    newCours.setTittre(fTitre.getText().trim());
                    newCours.setDescription(fDescription.getText().trim());
                    newCours.setNiveau(fNiveau.getValue());
                    newCours.setCategorie(fCategorie.getValue());
                    newCours.setDureeEstimee(duree);
                    newCours.setStatus("actif");
                    newCours.setDateCreation(java.time.LocalDate.now().toString());
                    service.ajouter(newCours);
                } else {
                    // EDIT
                    editingCours.setTittre(fTitre.getText().trim());
                    editingCours.setDescription(fDescription.getText().trim());
                    editingCours.setNiveau(fNiveau.getValue());
                    editingCours.setCategorie(fCategorie.getValue());
                    editingCours.setDureeEstimee(duree);
                    service.modifier(editingCours);
                }
                dialog.close();
                loadData();
            } catch (SQLDataException ex) {
                errorLabel.setText("Database error: " + ex.getMessage());
            }
        });

        HBox dialogButtons = new HBox(12, btnCancel, btnSave);
        dialogButtons.setAlignment(Pos.CENTER_RIGHT);

        // ── Dialog header ─────────────────────────────────────────────────────
        Label dlgTitle = new Label(cours == null ? "Add New Course" : "Edit Course");
        dlgTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        Button btnX = new Button("✕");
        btnX.setStyle("-fx-background-color: transparent; -fx-text-fill: #64748b; -fx-font-size: 16px; -fx-cursor: hand;");
        btnX.setOnAction(e -> dialog.close());

        HBox header = new HBox(dlgTitle, new Region(), btnX);
        HBox.setHgrow(header.getChildren().get(1), Priority.ALWAYS);
        header.setAlignment(Pos.CENTER_LEFT);

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #2a3142;");

        VBox root = new VBox(20, header, sep, form, dialogButtons);
        root.setPadding(new Insets(28));
        root.setStyle(
                "-fx-background-color: #161b2e;" +
                        "-fx-background-radius: 16;" +
                        "-fx-border-radius: 16;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-color: #2a3142;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 30, 0, 0, 8);"
        );
        root.setPrefWidth(520);

        StackPane overlay = new StackPane(root);
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.65);");
        overlay.setPadding(new Insets(40));

        Scene scene = new Scene(overlay, 620, 560);
        scene.setFill(Color.TRANSPARENT);
        dialog.setScene(scene);
        dialog.show();
    }

    // ── Dialog helper builders ─────────────────────────────────────────────────

    private TextField styledField(String prompt) {
        TextField f = new TextField();
        f.setPromptText(prompt);
        f.setStyle("-fx-background-color: #111827; -fx-text-fill: #ffffff; -fx-prompt-text-fill: #64748b;" +
                "-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-width: 1;" +
                "-fx-border-color: #2a3142; -fx-padding: 10 14; -fx-font-size: 13px;");
        return f;
    }

    private TextArea styledArea(String prompt) {
        TextArea a = new TextArea();
        a.setPromptText(prompt);
        a.setPrefRowCount(3);
        a.setWrapText(true);
        a.setStyle("-fx-background-color: #111827; -fx-text-fill: #ffffff; -fx-prompt-text-fill: #64748b;" +
                "-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-width: 1;" +
                "-fx-border-color: #2a3142; -fx-padding: 10 14; -fx-font-size: 13px;");
        return a;
    }

    private ComboBox<String> styledCombo(String prompt, String... items) {
        ComboBox<String> c = new ComboBox<>();
        c.setPromptText(prompt);
        c.getItems().addAll(items);
        c.setMaxWidth(Double.MAX_VALUE);
        c.setStyle("-fx-background-color: #111827; -fx-border-color: #2a3142;" +
                "-fx-border-radius: 8; -fx-background-radius: 8;");
        return c;
    }

    private VBox fieldBlock(String label, javafx.scene.Node field) {
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8; -fx-padding: 0 0 4 0;");
        VBox block = new VBox(6, lbl, field);
        VBox.setVgrow(field, Priority.ALWAYS);
        return block;
    }

    private HBox rowBlock(VBox left, VBox right) {
        HBox row = new HBox(14, left, right);
        HBox.setHgrow(left, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);
        return row;
    }

    // ── Utilities ──────────────────────────────────────────────────────────────

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
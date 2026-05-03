package Controller;

import Models.Chapitre;
import Models.Cours;
import Services.ServiceChapitre;
import Services.ServiceCours;
import utils.PDFExporter;
import utils.QRCodeGenerator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.SQLDataException;
import java.util.List;
import java.util.stream.Collectors;

public class ChapitreController {

    @FXML private Label lblTitle;
    @FXML private Label lblCoursInfo;
    @FXML private FlowPane chapitreGrid;
    @FXML private VBox emptyState;
    @FXML private VBox promptState;
    @FXML private VBox courseSelectorCard;
    @FXML private HBox searchBar;
    @FXML private TextField txtSearch;
    @FXML private Label lblResultCount;
    @FXML private Button btnAdd;
    @FXML private Button btnBack;
    @FXML private ComboBox<Cours> comboCours;
    @FXML private HBox headerButtons;

    private final ServiceChapitre service;
    private final ServiceCours serviceCours;
    private Cours cours;
    private ObservableList<Chapitre> allChapitres;
    private Chapitre editingChapitre = null;
    private boolean sidebarMode = false;

    public ChapitreController() {
        this.service = new ServiceChapitre();
        this.serviceCours = new ServiceCours();
    }

    @FXML
    public void initialize() {
        // Initialize search listener
        if (txtSearch != null) {
            txtSearch.textProperty().addListener((obs, o, n) -> applyFilter(n));
        }
    }

    public void setCours(Cours cours) {
        this.cours = cours;
        this.sidebarMode = false;

        if (courseSelectorCard != null) {
            courseSelectorCard.setVisible(false);
            courseSelectorCard.setManaged(false);
        }
        if (promptState != null) {
            promptState.setVisible(false);
            promptState.setManaged(false);
        }
        if (btnAdd != null) {
            btnAdd.setVisible(true);
            btnAdd.setManaged(true);
        }
        if (btnBack != null) {
            btnBack.setVisible(true);
            btnBack.setManaged(true);
        }
        if (searchBar != null) {
            searchBar.setVisible(true);
            searchBar.setManaged(true);
        }

        if (lblTitle != null) {
            lblTitle.setText("Chapters");
        }
        if (lblCoursInfo != null) {
            lblCoursInfo.setText(cours.getTittre() + " — " + cours.getDescription());
        }

        setupHeaderButtonsForCourseMode();
        loadData();
    }

    public void initWithoutCours() {
        this.sidebarMode = true;
        this.cours = null;

        if (courseSelectorCard != null) {
            courseSelectorCard.setVisible(true);
            courseSelectorCard.setManaged(true);
        }
        if (btnBack != null) {
            btnBack.setVisible(false);
            btnBack.setManaged(false);
        }
        if (btnAdd != null) {
            btnAdd.setVisible(false);
            btnAdd.setManaged(false);
        }
        if (searchBar != null) {
            searchBar.setVisible(false);
            searchBar.setManaged(false);
        }
        if (promptState != null) {
            promptState.setVisible(true);
            promptState.setManaged(true);
        }
        if (lblTitle != null) {
            lblTitle.setText("Chapters");
        }
        if (lblCoursInfo != null) {
            lblCoursInfo.setText("Select a course to browse its chapters");
        }

        loadCourseSelector();
    }

    private void setupHeaderButtonsForCourseMode() {
        if (headerButtons == null) return;

        headerButtons.getChildren().clear();

        Button btnExportPDF = new Button("📄 Export PDF");
        btnExportPDF.setStyle("-fx-background-color: #10b981; -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 16; -fx-cursor: hand;");
        btnExportPDF.setOnAction(e -> handleExportPDF());

        Button btnQR = new Button("📱 QR Code");
        btnQR.setStyle("-fx-background-color: #00d4ff; -fx-text-fill: #0a0e1a; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 16; -fx-cursor: hand;");
        btnQR.setOnAction(e -> handleShowQRCode());

        Button btnAddNew = new Button("＋  New Chapter");
        btnAddNew.setStyle("-fx-background-color: #8b5cf6; -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 16; -fx-cursor: hand;");
        btnAddNew.setOnAction(e -> handleAdd());

        headerButtons.getChildren().addAll(btnExportPDF, btnQR, btnAddNew);
    }

    private void loadCourseSelector() {
        try {
            List<Cours> list = serviceCours.recuperer();
            if (comboCours != null) {
                comboCours.setItems(FXCollections.observableArrayList(list));

                comboCours.setCellFactory(lv -> new ListCell<>() {
                    @Override
                    protected void updateItem(Cours item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty || item == null ? null
                                : item.getTittre() + "  ·  " + item.getNiveau() + "  ·  " + item.getCategorie());
                    }
                });
                comboCours.setButtonCell(new ListCell<>() {
                    @Override
                    protected void updateItem(Cours item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty || item == null ? null
                                : item.getTittre() + "  ·  " + item.getNiveau());
                    }
                });
            }
        } catch (SQLDataException e) {
            showAlert("Error", "Could not load courses: " + e.getMessage());
        }
    }

    @FXML
    private void handleCoursSelected() {
        if (comboCours == null) return;

        Cours selected = comboCours.getValue();
        if (selected == null) return;

        this.cours = selected;

        if (lblTitle != null) {
            lblTitle.setText("Chapters");
        }
        if (lblCoursInfo != null) {
            lblCoursInfo.setText(selected.getTittre() + " — " + selected.getDescription());
        }

        if (searchBar != null) {
            searchBar.setVisible(true);
            searchBar.setManaged(true);
        }
        if (btnAdd != null) {
            btnAdd.setVisible(true);
            btnAdd.setManaged(true);
        }
        if (promptState != null) {
            promptState.setVisible(false);
            promptState.setManaged(false);
        }

        setupHeaderButtonsForCourseMode();
        loadData();
    }

    private void loadData() {
        if (cours == null) return;
        try {
            List<Chapitre> list = service.getByCours(cours.getId());
            allChapitres = FXCollections.observableArrayList(list);
            applyFilter(txtSearch != null ? txtSearch.getText() : "");
        } catch (SQLDataException e) {
            showAlert("Error", "Could not load chapters: " + e.getMessage());
        }
    }

    private void applyFilter(String query) {
        if (allChapitres == null || chapitreGrid == null) return;

        List<Chapitre> filtered;
        if (query == null || query.isBlank()) {
            filtered = allChapitres;
        } else {
            String lower = query.toLowerCase();
            filtered = allChapitres.stream()
                    .filter(c -> c.getTitre().toLowerCase().contains(lower)
                            || (c.getContenu() != null && c.getContenu().toLowerCase().contains(lower)))
                    .collect(Collectors.toList());
        }
        renderCards(filtered);
        if (lblResultCount != null) {
            lblResultCount.setText(filtered.size() + " chapter" + (filtered.size() != 1 ? "s" : ""));
        }
    }

    private void renderCards(List<Chapitre> list) {
        if (chapitreGrid == null) return;

        chapitreGrid.getChildren().clear();

        boolean isEmpty = list == null || list.isEmpty();
        if (emptyState != null) {
            emptyState.setVisible(isEmpty);
            emptyState.setManaged(isEmpty);
        }
        if (isEmpty) return;

        for (Chapitre ch : list) {
            chapitreGrid.getChildren().add(buildCard(ch));
        }
    }

    private VBox buildCard(Chapitre ch) {
        VBox card = new VBox(14);
        card.setPrefWidth(360);
        card.setMaxWidth(360);
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

        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label(ch.getTitre());
        titleLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");
        titleLabel.setWrapText(true);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        Label orderBadge = new Label("Order: " + ch.getOrdre());
        orderBadge.setStyle(
                "-fx-background-color: rgba(139,92,246,0.2);" +
                        "-fx-text-fill: #8b5cf6;" +
                        "-fx-font-size: 11px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 6;" +
                        "-fx-padding: 4 10;"
        );
        topRow.getChildren().addAll(titleLabel, orderBadge);

        Label contenuLabel = new Label(
                ch.getContenu() == null || ch.getContenu().isBlank()
                        ? "No content provided." : ch.getContenu());
        contenuLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8;");
        contenuLabel.setWrapText(true);
        contenuLabel.setMaxHeight(48);

        boolean hasExercise = ch.getExercise() != null && !ch.getExercise().isBlank();
        Label exerciseTag = new Label(hasExercise ? "✅ Has Exercise" : "📝 No Exercise");
        exerciseTag.setStyle("-fx-font-size: 11px; -fx-text-fill: " + (hasExercise ? "#10b981;" : "#64748b;"));

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #2a3142;");

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button btnEdit = iconButton("✏️ Edit", "#8b5cf6");
        Button btnDelete = iconButton("🗑 Delete", "#ef4444");
        btnEdit.setOnAction(e -> handleEdit(ch));
        btnDelete.setOnAction(e -> handleDelete(ch));

        actions.getChildren().addAll(btnEdit, btnDelete);
        card.getChildren().addAll(topRow, contenuLabel, exerciseTag, sep, actions);
        return card;
    }

    private Button iconButton(String text, String color) {
        Button btn = new Button(text);
        btn.setStyle(
                "-fx-background-color: rgba(255,255,255,0.06);" +
                        "-fx-text-fill: " + color + ";" +
                        "-fx-background-radius: 8;" +
                        "-fx-font-size: 13px;" +
                        "-fx-padding: 7 16;" +
                        "-fx-cursor: hand;"
        );
        return btn;
    }

    @FXML
    private void handleAdd() {
        editingChapitre = null;
        openDialog(null);
    }

    private void handleEdit(Chapitre ch) {
        editingChapitre = ch;
        openDialog(ch);
    }

    private void handleDelete(Chapitre ch) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete chapter \"" + ch.getTitre() + "\"?", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    service.supprimer(ch);
                    loadData();
                } catch (SQLDataException e) {
                    showAlert("Error", "Could not delete chapter: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleBack() {
        Stage stage = (Stage) btnBack.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void handleExportPDF() {
        if (cours == null) {
            showAlert("Error", "No course selected.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Course with Chapters to PDF");
        fileChooser.setInitialFileName(cours.getTittre().replaceAll("[^a-zA-Z0-9]", "_") + "_Full.pdf");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Document", "*.pdf")
        );

        File file = fileChooser.showSaveDialog(chapitreGrid != null ? chapitreGrid.getScene().getWindow() : null);
        if (file != null) {
            try {
                PDFExporter.exportCourseWithChaptersToPDF(cours, allChapitres, file);
                showAlert("Success", "Course with chapters exported to:\n" + file.getAbsolutePath());
            } catch (IOException e) {
                showAlert("Error", "Could not export PDF: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleShowQRCode() {
        if (cours == null) {
            showAlert("Error", "No course selected.");
            return;
        }

        try {
            String qrContent = String.format("AIVA-COURSE|%d|%s|%s|%s",
                    cours.getId(),
                    cours.getTittre(),
                    cours.getCategorie(),
                    cours.getNiveau()
            );

            BufferedImage qrImage = QRCodeGenerator.generateQRCodeImage(qrContent, 250, 250);
            Image fxImage = SwingFXUtils.toFXImage(qrImage, null);

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initStyle(StageStyle.UNDECORATED);

            ImageView imageView = new ImageView(fxImage);
            imageView.setFitWidth(250);
            imageView.setFitHeight(250);
            imageView.setPreserveRatio(true);

            Label title = new Label(cours.getTittre());
            title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

            Label subtitle = new Label("Scan to access course");
            subtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8;");

            Button btnSave = new Button("💾 Save");
            btnSave.setStyle("-fx-background-color: #8b5cf6; -fx-text-fill: #ffffff; -fx-background-radius: 8; -fx-padding: 8 16; -fx-cursor: hand;");
            btnSave.setOnAction(e -> {
                FileChooser fc = new FileChooser();
                fc.setTitle("Save QR Code");
                fc.setInitialFileName("QR_" + cours.getTittre().replaceAll("[^a-zA-Z0-9]", "_") + ".png");
                fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG", "*.png"));
                File f = fc.showSaveDialog(dialog);
                if (f != null) {
                    try {
                        ImageIO.write(qrImage, "PNG", f);
                        showAlert("Success", "QR Code saved!");
                    } catch (IOException ex) {
                        showAlert("Error", "Could not save: " + ex.getMessage());
                    }
                }
            });

            Button btnClose = new Button("Close");
            btnClose.setStyle("-fx-background-color: #1e2538; -fx-text-fill: #ffffff; -fx-background-radius: 8; -fx-padding: 8 16; -fx-cursor: hand;");
            btnClose.setOnAction(e -> dialog.close());

            HBox btns = new HBox(10, btnSave, btnClose);
            btns.setAlignment(Pos.CENTER);

            VBox root = new VBox(15, title, imageView, subtitle, btns);
            root.setAlignment(Pos.CENTER);
            root.setPadding(new Insets(30));
            root.setStyle("-fx-background-color: #161b2e; -fx-border-radius: 12; -fx-background-radius: 12; -fx-border-color: #2a3142;");

            StackPane overlay = new StackPane(root);
            overlay.setStyle("-fx-background-color: rgba(0,0,0,0.7);");

            Scene scene = new Scene(overlay, 400, 450);
            scene.setFill(Color.TRANSPARENT);
            dialog.setScene(scene);
            dialog.show();

        } catch (Exception e) {
            showAlert("Error", "Could not generate QR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void openDialog(Chapitre ch) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.TRANSPARENT);
        dialog.setTitle(ch == null ? "New Chapter" : "Edit Chapter");

        TextField fTitre = styledField("Chapter title");
        TextField fOrdre = styledField("Order number (e.g. 1)");
        TextArea fContenu = styledArea("Chapter content…");
        TextArea fExercise = styledArea("Exercise (optional)…");

        if (ch != null) {
            fTitre.setText(ch.getTitre());
            fOrdre.setText(String.valueOf(ch.getOrdre()));
            fContenu.setText(ch.getContenu());
            fExercise.setText(ch.getExercise() != null ? ch.getExercise() : "");
        }

        Label errorLabel = new Label("");
        errorLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px;");

        VBox form = new VBox(14,
                fieldBlock("Chapter Title", fTitre),
                fieldBlock("Order", fOrdre),
                fieldBlock("Content", fContenu),
                fieldBlock("Exercise (optional)", fExercise),
                errorLabel
        );

        Button btnCancel = new Button("Cancel");
        btnCancel.setStyle("-fx-background-color: #1e2538; -fx-text-fill: #ffffff;" +
                "-fx-background-radius: 8; -fx-padding: 10 24; -fx-cursor: hand;");
        btnCancel.setOnAction(e -> dialog.close());

        Button btnSave = new Button(ch == null ? "Add Chapter" : "Save Changes");
        btnSave.setStyle("-fx-background-color: #8b5cf6; -fx-text-fill: #ffffff;" +
                "-fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10 24; -fx-cursor: hand;");

        btnSave.setOnAction(e -> {
            if (fTitre.getText().isBlank()) { errorLabel.setText("Title is required."); return; }
            if (fContenu.getText().isBlank()) { errorLabel.setText("Content is required."); return; }
            if (fOrdre.getText().isBlank()) { errorLabel.setText("Order is required."); return; }
            int ordre;
            try {
                ordre = Integer.parseInt(fOrdre.getText().trim());
                if (ordre <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                errorLabel.setText("Order must be a positive number.");
                return;
            }
            try {
                if (editingChapitre == null) {
                    Chapitre newCh = new Chapitre();
                    newCh.setTitre(fTitre.getText().trim());
                    newCh.setContenu(fContenu.getText().trim());
                    newCh.setOrdre(ordre);
                    newCh.setExercise(fExercise.getText().trim());
                    service.ajouter(newCh, cours.getId());
                } else {
                    editingChapitre.setTitre(fTitre.getText().trim());
                    editingChapitre.setContenu(fContenu.getText().trim());
                    editingChapitre.setOrdre(ordre);
                    editingChapitre.setExercise(fExercise.getText().trim());
                    service.modifier(editingChapitre);
                }
                dialog.close();
                loadData();
            } catch (SQLDataException ex) {
                errorLabel.setText("Database error: " + ex.getMessage());
            }
        });

        HBox dialogButtons = new HBox(12, btnCancel, btnSave);
        dialogButtons.setAlignment(Pos.CENTER_RIGHT);

        Label dlgTitle = new Label(ch == null ? "New Chapter" : "Edit Chapter");
        dlgTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        Button btnX = new Button("✕");
        btnX.setStyle("-fx-background-color: transparent; -fx-text-fill: #64748b;" +
                "-fx-font-size: 16px; -fx-cursor: hand;");
        btnX.setOnAction(e -> dialog.close());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(dlgTitle, spacer, btnX);
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

        javafx.geometry.Rectangle2D screen = javafx.stage.Screen.getPrimary().getVisualBounds();
        StackPane overlay = new StackPane(root);
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.65);");
        overlay.setPadding(new Insets(40));

        Scene scene = new Scene(overlay, screen.getWidth(), screen.getHeight());
        scene.setFill(Color.TRANSPARENT);
        dialog.setScene(scene);
        dialog.show();
    }

    private TextField styledField(String prompt) {
        TextField f = new TextField();
        f.setPromptText(prompt);
        f.setStyle("-fx-background-color: #111827; -fx-text-fill: #ffffff;" +
                "-fx-prompt-text-fill: #64748b; -fx-background-radius: 8;" +
                "-fx-border-radius: 8; -fx-border-width: 1; -fx-border-color: #2a3142;" +
                "-fx-padding: 10 14; -fx-font-size: 13px;");
        return f;
    }

    private TextArea styledArea(String prompt) {
        TextArea a = new TextArea();
        a.setPromptText(prompt);
        a.setPrefRowCount(3);
        a.setWrapText(true);
        a.setStyle("-fx-background-color: #111827; -fx-text-fill: #ffffff;" +
                "-fx-prompt-text-fill: #64748b; -fx-background-radius: 8;" +
                "-fx-border-radius: 8; -fx-border-width: 1; -fx-border-color: #2a3142;" +
                "-fx-padding: 10 14; -fx-font-size: 13px;");
        return a;
    }

    private VBox fieldBlock(String labelText, javafx.scene.Node field) {
        Label lbl = new Label(labelText);
        lbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8; -fx-padding: 0 0 4 0;");
        VBox block = new VBox(6, lbl, field);
        VBox.setVgrow(field, Priority.ALWAYS);
        return block;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
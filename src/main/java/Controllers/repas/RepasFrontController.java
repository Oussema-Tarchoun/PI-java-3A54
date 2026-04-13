package Controllers.repas;

import Models.Repas;
import Services.ServiceRepas;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URL;
import java.sql.Date;
import java.sql.SQLDataException;
import java.sql.Time;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class RepasFrontController implements Initializable {

    // ── Stats labels ──────────────────────────────────────────────────────────
    @FXML private Label statTotal;
    @FXML private Label statAvgCal;
    @FXML private Label statCeMois;
    @FXML private Label statScore;

    // ── Search + filter ───────────────────────────────────────────────────────
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterType;

    // ── Card grid ─────────────────────────────────────────────────────────────
    @FXML private FlowPane cardsPane;

    // ── Add form panel ────────────────────────────────────────────────────────
    @FXML private VBox      formPanel;
    @FXML private Label     formTitle;
    @FXML private TextField nomField;
    @FXML private ComboBox<String> typeCombo;
    @FXML private DatePicker datePicker;
    @FXML private TextField heureField;
    @FXML private TextField caloriesField;
    @FXML private TextArea  descriptionArea;
    @FXML private Label     errorLabel;

    private ServiceRepas serviceRepas;
    private List<Repas>  allRepas;
    private Repas        editingRepas; // null = create, non-null = edit
    private static final int USER_ID = 1;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try {
            serviceRepas = new ServiceRepas();
        } catch (Exception e) {
            System.out.println("RepasFront init error: " + e.getMessage());
        }

        if (filterType != null) {
            filterType.getItems().addAll("Tous", "Petit-déjeuner", "Déjeuner", "Dîner", "Collation");
            filterType.setValue("Tous");
            filterType.setOnAction(e -> applyFilter());
        }

        if (typeCombo != null) {
            typeCombo.getItems().addAll("Petit-déjeuner", "Déjeuner", "Dîner", "Collation");
        }

        if (formPanel != null) formPanel.setVisible(false);

        loadData();
    }

    // ── Data ──────────────────────────────────────────────────────────────────

    private void loadData() {
        try {
            allRepas = serviceRepas.recuperer();
            updateStats();
            renderCards(allRepas);
        } catch (Exception e) {
            System.out.println("loadData error: " + e.getMessage());
        }
    }

    private void updateStats() {
        if (allRepas == null) return;
        int total = allRepas.size();
        if (statTotal != null) statTotal.setText(String.valueOf(total));

        if (total > 0) {
            double avg = allRepas.stream().mapToInt(Repas::getCalories).average().orElse(0);
            if (statAvgCal != null) statAvgCal.setText((int) avg + " kcal");

            LocalDate now = LocalDate.now();
            long thisMo = allRepas.stream()
                .filter(r -> r.getDate() != null &&
                    r.getDate().toLocalDate().getMonthValue() == now.getMonthValue() &&
                    r.getDate().toLocalDate().getYear() == now.getYear())
                .count();
            if (statCeMois != null) statCeMois.setText(String.valueOf(thisMo));

            double avgCal = allRepas.stream().mapToInt(Repas::getCalories).average().orElse(0);
            if (statScore != null) statScore.setText(avgCal <= 500 ? "Excellent" : avgCal <= 700 ? "Bon" : "Élevé");
        } else {
            if (statAvgCal != null) statAvgCal.setText("— kcal");
            if (statCeMois != null) statCeMois.setText("0");
            if (statScore  != null) statScore.setText("—");
        }
    }

    private void renderCards(List<Repas> list) {
        if (cardsPane == null) return;
        cardsPane.getChildren().clear();

        if (list == null || list.isEmpty()) {
            Label empty = new Label("Aucun repas trouvé. Ajoutez votre premier repas !");
            empty.setStyle("-fx-text-fill: #64748b; -fx-font-size: 15px;");
            cardsPane.getChildren().add(empty);
            return;
        }

        for (Repas r : list) {
            cardsPane.getChildren().add(buildCard(r));
        }
    }

    private VBox buildCard(Repas r) {
        VBox card = new VBox(12);
        card.setPrefWidth(280);
        card.setPadding(new Insets(20));
        card.getStyleClass().add("card");
        card.setStyle("-fx-cursor: hand;");

        // ── Top row: type badge + calories ────────────────────────────────────
        HBox topRow = new HBox();
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label typeBadge = new Label(r.getType() != null ? r.getType() : "—");
        typeBadge.setStyle(getBadgeStyle(r.getType()));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label calLabel = new Label(r.getCalories() + " kcal");
        calLabel.setStyle("-fx-text-fill: #00d4ff; -fx-font-size: 13px; -fx-font-weight: bold;");

        topRow.getChildren().addAll(typeBadge, spacer, calLabel);

        // ── Meal name ─────────────────────────────────────────────────────────
        Label nomLabel = new Label(r.getNom());
        nomLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 17px; -fx-font-weight: bold;");
        nomLabel.setWrapText(true);

        // ── Date + time row ───────────────────────────────────────────────────
        HBox metaRow = new HBox(12);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        if (r.getDate() != null) {
            Label dateLabel = new Label("📅 " + r.getDate().toLocalDate().toString());
            dateLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
            metaRow.getChildren().add(dateLabel);
        }
        if (r.getHeure() != null) {
            Label heureLabel = new Label("🕐 " + r.getHeure().toString().substring(0, 5));
            heureLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
            metaRow.getChildren().add(heureLabel);
        }

        // ── Score pill ────────────────────────────────────────────────────────
        String score = r.getCalories() <= 400 ? "Excellent" : r.getCalories() <= 700 ? "Bon" : "Élevé";
        String scoreColor = r.getCalories() <= 400 ? "#10b981" : r.getCalories() <= 700 ? "#f59e0b" : "#ef4444";
        Label scoreLabel = new Label("Score : " + score);
        scoreLabel.setStyle("-fx-text-fill: " + scoreColor + "; -fx-font-size: 12px; " +
            "-fx-background-color: " + scoreColor + "22; -fx-background-radius: 4; -fx-padding: 4 8 4 8;");

        // ── Description (truncated) ───────────────────────────────────────────
        if (r.getDescription() != null && !r.getDescription().isBlank()) {
            Label desc = new Label(r.getDescription().length() > 60
                ? r.getDescription().substring(0, 60) + "…"
                : r.getDescription());
            desc.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");
            desc.setWrapText(true);
            card.getChildren().addAll(topRow, nomLabel, metaRow, scoreLabel, desc);
        } else {
            card.getChildren().addAll(topRow, nomLabel, metaRow, scoreLabel);
        }

        // ── Action buttons ────────────────────────────────────────────────────
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button btnEdit = new Button("✎ Modifier");
        btnEdit.setStyle("-fx-background-color: rgba(0,212,255,0.15); -fx-text-fill: #00d4ff; " +
            "-fx-background-radius: 6; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 6 12 6 12;");
        btnEdit.setOnAction(e -> openEditForm(r));

        Button btnDel = new Button("✕ Supprimer");
        btnDel.setStyle("-fx-background-color: rgba(239,68,68,0.15); -fx-text-fill: #ef4444; " +
            "-fx-background-radius: 6; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 6 12 6 12;");
        btnDel.setOnAction(e -> handleDelete(r));

        actions.getChildren().addAll(btnEdit, btnDel);
        card.getChildren().add(actions);

        return card;
    }

    private String getBadgeStyle(String type) {
        String color = "#8b5cf6";
        if (type != null) {
            color = switch (type.toLowerCase().trim()) {
                case "déjeuner", "dejeuner"                  -> "#00d4ff";
                case "dîner", "diner"                        -> "#8b5cf6";
                case "collation"                             -> "#f59e0b";
                case "petit-déjeuner", "petit déjeuner",
                     "petit-dejeuner", "petit dejeuner"      -> "#10b981";
                default -> "#8b5cf6";
            };
        }
        return "-fx-text-fill: " + color + "; -fx-background-color: " + color + "22; " +
               "-fx-background-radius: 4; -fx-padding: 4 10 4 10; -fx-font-size: 11px; -fx-font-weight: bold;";
    }

    // ── Search & Filter ───────────────────────────────────────────────────────

    @FXML
    private void handleSearch() {
        applyFilter();
    }

    private void applyFilter() {
        if (allRepas == null) return;
        String search = searchField != null ? searchField.getText().toLowerCase().trim() : "";
        String type   = filterType  != null ? filterType.getValue() : "Tous";

        List<Repas> filtered = allRepas.stream()
            .filter(r -> search.isBlank() ||
                r.getNom().toLowerCase().contains(search) ||
                (r.getDescription() != null && r.getDescription().toLowerCase().contains(search)))
            .filter(r -> "Tous".equals(type) || type.equals(r.getType()))
            .collect(Collectors.toList());

        renderCards(filtered);
    }

    // ── Form ─────────────────────────────────────────────────────────────────

    @FXML
    private void openAddForm() {
        editingRepas = null;
        if (formTitle != null) formTitle.setText("➕ Nouveau Repas");
        clearForm();
        showForm(true);
    }

    private void openEditForm(Repas r) {
        editingRepas = r;
        if (formTitle    != null) formTitle.setText("✎ Modifier : " + r.getNom());
        if (nomField     != null) nomField.setText(r.getNom());
        if (typeCombo    != null) typeCombo.setValue(r.getType());
        if (datePicker   != null && r.getDate() != null)
            datePicker.setValue(r.getDate().toLocalDate());
        if (heureField   != null)
            heureField.setText(r.getHeure() != null ? r.getHeure().toString().substring(0, 5) : "");
        if (caloriesField != null) caloriesField.setText(String.valueOf(r.getCalories()));
        if (descriptionArea != null)
            descriptionArea.setText(r.getDescription() != null ? r.getDescription() : "");
        showForm(true);
    }

    @FXML
    private void handleSave() {
        hideError();

        String nom = nomField != null ? nomField.getText().trim() : "";
        if (nom.isBlank()) { showError("Le nom est obligatoire."); return; }
        if (typeCombo.getValue() == null) { showError("Veuillez sélectionner un type."); return; }
        if (datePicker.getValue() == null) { showError("Veuillez sélectionner une date."); return; }

        String heureStr = heureField.getText().trim();
        if (heureStr.isBlank()) { showError("L'heure est obligatoire (ex: 12:30)."); return; }

        Time heure;
        try {
            if (heureStr.matches("\\d{1,2}:\\d{2}")) heureStr += ":00";
            heure = Time.valueOf(heureStr);
        } catch (IllegalArgumentException e) {
            showError("Format d'heure invalide. Utilisez HH:mm."); return;
        }

        int calories = 0;
        String calStr = caloriesField.getText().trim();
        if (!calStr.isBlank()) {
            try {
                calories = Integer.parseInt(calStr);
                if (calories < 0) { showError("Les calories ne peuvent pas être négatives."); return; }
            } catch (NumberFormatException e) {
                showError("Les calories doivent être un nombre entier."); return;
            }
        }

        String desc = descriptionArea != null && descriptionArea.getText() != null
            ? descriptionArea.getText().trim() : "";

        try {
            if (editingRepas == null) {
                Repas r = new Repas(USER_ID, nom, heure, calories, desc,
                    typeCombo.getValue(), Date.valueOf(datePicker.getValue()));
                serviceRepas.ajouter(r);
            } else {
                editingRepas.setNom(nom);
                editingRepas.setType(typeCombo.getValue());
                editingRepas.setDate(Date.valueOf(datePicker.getValue()));
                editingRepas.setHeure(heure);
                editingRepas.setCalories(calories);
                editingRepas.setDescription(desc);
                serviceRepas.modifier(editingRepas);
            }
            showForm(false);
            loadData();
        } catch (SQLDataException e) {
            showError("Erreur : " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        showForm(false);
        hideError();
    }

    private void handleDelete(Repas r) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer");
        alert.setHeaderText("Supprimer \"" + r.getNom() + "\" ?");
        alert.setContentText("Cette action est irréversible.");
        alert.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    serviceRepas.deleteRepasAliments(r.getId());
                    serviceRepas.supprimer(r);
                    loadData();
                } catch (Exception e) {
                    System.out.println("delete error: " + e.getMessage());
                }
            }
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void showForm(boolean show) {
        if (formPanel != null) {
            formPanel.setVisible(show);
            formPanel.setManaged(show);
        }
    }

    private void clearForm() {
        if (nomField       != null) nomField.clear();
        if (typeCombo      != null) typeCombo.setValue(null);
        if (datePicker     != null) datePicker.setValue(null);
        if (heureField     != null) heureField.clear();
        if (caloriesField  != null) caloriesField.clear();
        if (descriptionArea!= null) descriptionArea.clear();
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
}

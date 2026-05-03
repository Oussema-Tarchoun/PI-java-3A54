package Controllers.aliment;

import Models.Aliment;
import Services.ServiceAliment;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import utils.SessionManager;
import Models.User;

import java.net.URL;
import java.sql.SQLDataException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class AlimentFrontController implements Initializable {

    // ── Stats ─────────────────────────────────────────────────────────────────
    @FXML
    private Label statTotal;
    @FXML
    private Label statAvgCal;
    @FXML
    private Label statMaxCal;
    @FXML
    private Label statWithMacro;

    // ── Search ────────────────────────────────────────────────────────────────
    @FXML
    private TextField searchField;

    // ── Card grid ─────────────────────────────────────────────────────────────
    @FXML
    private FlowPane cardsPane;

    // ── Form ──────────────────────────────────────────────────────────────────
    @FXML
    private VBox formPanel;
    @FXML
    private Label formTitle;
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

    private ServiceAliment serviceAliment;
    private List<Aliment> allAliments;
    private Aliment editingAliment;
    private int userId = 1;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        User current = SessionManager.getCurrentUser();
        if (current != null) {
            this.userId = current.getId();
        }

        try {
            serviceAliment = new ServiceAliment();
        } catch (Exception e) {
            System.out.println("AlimentFront init error: " + e.getMessage());
        }

        if (formPanel != null) {
            formPanel.setVisible(false);
            formPanel.setManaged(false);
        }

        if (quantiteField != null)
            quantiteField.setText("100");

        loadData();
    }

    // ── Data ──────────────────────────────────────────────────────────────────

    private void loadData() {
        try {
            allAliments = serviceAliment.recupererParUser(userId);
            updateStats();
            renderCards(allAliments);
        } catch (Exception e) {
            System.out.println("AlimentFront loadData error: " + e.getMessage());
        }
    }

    private void updateStats() {
        if (allAliments == null)
            return;
        int total = allAliments.size();
        if (statTotal != null)
            statTotal.setText(String.valueOf(total));

        if (total > 0) {
            double avg = allAliments.stream().mapToDouble(Aliment::getCalories).average().orElse(0);
            if (statAvgCal != null)
                statAvgCal.setText(fmt(avg) + " kcal");

            Aliment max = allAliments.stream()
                    .max((a, b) -> Double.compare(a.getCalories(), b.getCalories())).orElse(null);
            if (statMaxCal != null)
                statMaxCal.setText(max != null ? max.getNom() : "—");

            long withMacro = allAliments.stream()
                    .filter(a -> a.getMacro() != null && !a.getMacro().isBlank()).count();
            if (statWithMacro != null)
                statWithMacro.setText(String.valueOf(withMacro));
        } else {
            if (statAvgCal != null)
                statAvgCal.setText("— kcal");
            if (statMaxCal != null)
                statMaxCal.setText("—");
            if (statWithMacro != null)
                statWithMacro.setText("0");
        }
    }

    private void renderCards(List<Aliment> list) {
        if (cardsPane == null)
            return;
        cardsPane.getChildren().clear();

        if (list == null || list.isEmpty()) {
            Label empty = new Label("Aucun aliment trouvé. Ajoutez votre premier aliment !");
            empty.setStyle("-fx-text-fill: #64748b; -fx-font-size: 15px;");
            cardsPane.getChildren().add(empty);
            return;
        }

        for (Aliment a : list)
            cardsPane.getChildren().add(buildCard(a));
    }

    private VBox buildCard(Aliment a) {
        VBox card = new VBox(14);
        card.setPrefWidth(260);
        card.setPadding(new Insets(20));
        card.getStyleClass().add("card");

        // ── Top: circle icon + name ───────────────────────────────────────────
        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);

        // Letter avatar
        StackPane avatar = new StackPane();
        avatar.setMinSize(42, 42);
        avatar.setMaxSize(42, 42);
        avatar.setStyle("-fx-background-color: rgba(0,212,255,0.15); -fx-background-radius: 21;");
        Label letter = new Label(a.getNom().substring(0, 1).toUpperCase());
        letter.setStyle("-fx-text-fill: #00d4ff; -fx-font-size: 18px; -fx-font-weight: bold;");
        avatar.getChildren().add(letter);

        VBox nameBox = new VBox(2);
        Label nomLabel = new Label(a.getNom());
        nomLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 16px; -fx-font-weight: bold;");
        Label qtyLabel = new Label(fmt(a.getQuantite()) + " g");
        qtyLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");
        nameBox.getChildren().addAll(nomLabel, qtyLabel);

        topRow.getChildren().addAll(avatar, nameBox);

        // ── Calories badge ────────────────────────────────────────────────────
        HBox calRow = new HBox();
        calRow.setAlignment(Pos.CENTER_LEFT);
        Label calBadge = new Label("🔥 " + fmt(a.getCalories()) + " kcal");
        calBadge.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 14px; -fx-font-weight: bold; " +
                "-fx-background-color: rgba(245,158,11,0.12); -fx-background-radius: 6; -fx-padding: 6 12 6 12;");
        calRow.getChildren().add(calBadge);

        // ── Macro pills ───────────────────────────────────────────────────────
        HBox macroRow = new HBox(8);
        macroRow.setAlignment(Pos.CENTER_LEFT);
        if (a.getMacro() != null && !a.getMacro().isBlank()) {
            String[] m = parseMacro(a.getMacro());
            macroRow.getChildren().addAll(
                    pill("P: " + m[0] + "g", "#10b981"),
                    pill("G: " + m[1] + "g", "#f59e0b"),
                    pill("L: " + m[2] + "g", "#8b5cf6"));
        } else {
            Label none = new Label("Macros non renseignées");
            none.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
            macroRow.getChildren().add(none);
        }

        // ── Action buttons ────────────────────────────────────────────────────
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button btnEdit = new Button("✎ Modifier");
        btnEdit.setStyle("-fx-background-color: rgba(0,212,255,0.15); -fx-text-fill: #00d4ff; " +
                "-fx-background-radius: 6; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 6 12 6 12;");
        btnEdit.setOnAction(e -> openEditForm(a));

        Button btnDel = new Button("✕");
        btnDel.setStyle("-fx-background-color: rgba(239,68,68,0.15); -fx-text-fill: #ef4444; " +
                "-fx-background-radius: 6; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 6 10 6 10;");
        btnDel.setOnAction(e -> handleDelete(a));

        actions.getChildren().addAll(btnEdit, btnDel);

        card.getChildren().addAll(topRow, calRow, macroRow, actions);
        return card;
    }

    private Label pill(String text, String color) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: " + color + "; -fx-background-color: " + color + "22; " +
                "-fx-background-radius: 4; -fx-padding: 3 8 3 8; -fx-font-size: 11px; -fx-font-weight: bold;");
        return l;
    }

    // ── Search ────────────────────────────────────────────────────────────────

    @FXML
    private void handleSearch() {
        if (allAliments == null)
            return;
        String q = searchField != null ? searchField.getText().toLowerCase().trim() : "";
        List<Aliment> filtered = q.isBlank() ? allAliments
                : allAliments.stream()
                        .filter(a -> a.getNom().toLowerCase().contains(q))
                        .collect(Collectors.toList());
        renderCards(filtered);
    }

    // ── Form ──────────────────────────────────────────────────────────────────

    @FXML
    private void openAddForm() {
        editingAliment = null;
        if (formTitle != null)
            formTitle.setText("➕ Nouvel Aliment");
        clearForm();
        if (quantiteField != null)
            quantiteField.setText("100");
        showForm(true);
    }

    private void openEditForm(Aliment a) {
        editingAliment = a;
        if (formTitle != null)
            formTitle.setText("✎ Modifier : " + a.getNom());
        if (nomField != null)
            nomField.setText(a.getNom());
        if (quantiteField != null)
            quantiteField.setText(fmt(a.getQuantite()));
        if (caloriesField != null)
            caloriesField.setText(fmt(a.getCalories()));
        String[] m = parseMacro(a.getMacro());
        if (proteinesField != null)
            proteinesField.setText(m[0]);
        if (glucidesField != null)
            glucidesField.setText(m[1]);
        if (lipidesField != null)
            lipidesField.setText(m[2]);
        showForm(true);
    }

    @FXML
    private void handleSave() {
        hideError();

        String nom = nomField != null ? nomField.getText().trim() : "";
        if (nom.isBlank()) {
            showError("Le nom est obligatoire.");
            return;
        }
        if (nom.length() < 2) {
            showError("Le nom doit faire au moins 2 caractères.");
            return;
        }

        double quantite = parseNum(quantiteField, "Quantité", true);
        if (Double.isNaN(quantite))
            return;
        if (quantite <= 0) {
            showError("La quantité doit être > 0.");
            return;
        }

        double proteines = parseNum(proteinesField, "Protéines", false);
        if (Double.isNaN(proteines))
            return;
        double glucides = parseNum(glucidesField, "Glucides", false);
        if (Double.isNaN(glucides))
            return;
        double lipides = parseNum(lipidesField, "Lipides", false);
        if (Double.isNaN(lipides))
            return;

        double calories = (proteines * 4) + (glucides * 4) + (lipides * 9);
        if (caloriesField != null && !caloriesField.getText().isBlank()) {
            double manual = parseNum(caloriesField, "Calories", false);
            if (!Double.isNaN(manual) && manual > 0)
                calories = manual;
        }

        // Store macros as comma-separated simple values to avoid DB constraint
        String macro = proteines + "," + glucides + "," + lipides;

        try {
            if (editingAliment == null) {
                Aliment a = new Aliment(userId, nom, quantite, calories, macro);
                serviceAliment.ajouter(a);
            } else {
                editingAliment.setNom(nom);
                editingAliment.setQuantite(quantite);
                editingAliment.setCalories(calories);
                editingAliment.setMacro(macro);
                serviceAliment.modifier(editingAliment);
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

    private void handleDelete(Aliment a) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer");
        alert.setHeaderText("Supprimer \"" + a.getNom() + "\" ?");
        alert.setContentText("Cet aliment sera retiré de tous les repas associés.");
        alert.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    serviceAliment.supprimer(a);
                    loadData();
                } catch (Exception e) {
                    System.out.println("delete aliment error: " + e.getMessage());
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
        if (nomField != null)
            nomField.clear();
        if (quantiteField != null)
            quantiteField.clear();
        if (caloriesField != null)
            caloriesField.clear();
        if (proteinesField != null)
            proteinesField.clear();
        if (glucidesField != null)
            glucidesField.clear();
        if (lipidesField != null)
            lipidesField.clear();
    }

    private double parseNum(TextField f, String name, boolean required) {
        if (f == null)
            return 0;
        String t = f.getText();
        if (t == null || t.isBlank()) {
            if (required) {
                showError(name + " est obligatoire.");
                return Double.NaN;
            }
            return 0;
        }
        try {
            double v = Double.parseDouble(t.trim());
            if (v < 0) {
                showError(name + " ne peut pas être négatif.");
                return Double.NaN;
            }
            return v;
        } catch (NumberFormatException e) {
            showError(name + " doit être un nombre (ex: 3.5).");
            f.requestFocus();
            return Double.NaN;
        }
    }

    private String[] parseMacro(String macro) {
        String[] result = { "0", "0", "0" };
        if (macro == null || macro.isBlank())
            return result;
        // Support both formats: "P:3.5;G:23.0;L:10.0" and "3.5,23.0,10.0"
        if (macro.contains(",")) {
            String[] parts = macro.split(",");
            if (parts.length >= 3) {
                result[0] = parts[0].trim();
                result[1] = parts[1].trim();
                result[2] = parts[2].trim();
            }
        } else {
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
        }
        return result;
    }

    private String fmt(double d) {
        return d == Math.floor(d) ? String.valueOf((int) d) : String.format("%.1f", d);
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

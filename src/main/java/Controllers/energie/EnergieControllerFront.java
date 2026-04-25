package Controllers.energie;

import Models.Energie;
import Services.ServiceEnergie;
import Services.GroqService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.application.Platform;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import javafx.collections.FXCollections;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import javafx.scene.chart.*;
import javafx.stage.FileChooser;
import java.io.File;
import Services.ExportService;
import Services.ServiceRecommandation;
import Models.Recommandation;
import javafx.scene.chart.XYChart;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;

public class EnergieControllerFront implements Initializable {

    // Stats
    @FXML private Label statTotal;
    @FXML private Label statAverage;
    @FXML private Label statMax;
    @FXML private Label statMaxType;

    // Search & Sort
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortCombo;

    // Grid
    @FXML private FlowPane cardsPane;

    // Form (Now in Modal)
    @FXML private Label errorLabel;

    // AI Result
    @FXML private VBox aiResultPanel;
    @FXML private TextArea aiTextArea;

    // Chart
    @FXML private LineChart<String, Number> energyChart;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis yAxis;

    private ServiceEnergie serviceEnergie;
    private ServiceRecommandation serviceRecommandation;
    private GroqService groqService;
    private ExportService exportService;
    private List<Energie> allEnergies;
    private Energie editingEnergie;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        serviceEnergie = new ServiceEnergie();
        serviceRecommandation = new ServiceRecommandation();
        groqService = new GroqService();
        exportService = new ExportService();

        if (sortCombo != null) {
            sortCombo.setItems(javafx.collections.FXCollections.observableArrayList(
                "Date (Récent)", "Date (Ancien)", "Valeur (Max)", "Valeur (Min)", "Type"
            ));
        }

        loadData();
    }

    private void loadData() {
        try {
            allEnergies = serviceEnergie.recupererParUser("1");
            updateStats();
            renderCards(allEnergies);
            updateChart();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateStats() {
        if (allEnergies == null || allEnergies.isEmpty()) {
            statTotal.setText("0 kWh");
            statAverage.setText("0 kWh");
            statMax.setText("0 kWh");
            statMaxType.setText("Aucune donnée");
            return;
        }

        double total = allEnergies.stream().mapToDouble(Energie::getValeur).sum();
        double avg = total / allEnergies.size();
        Energie max = allEnergies.stream().max((a, b) -> Float.compare(a.getValeur(), b.getValeur())).get();

        statTotal.setText(String.format("%.1f kWh", total));
        statAverage.setText(String.format("%.1f kWh", avg));
        statMax.setText(String.format("%.1f kWh", max.getValeur()));
        statMaxType.setText("Pic: " + max.getType_energie());
    }

    private void renderCards(List<Energie> list) {
        cardsPane.getChildren().clear();
        if (list == null || list.isEmpty()) {
            Label empty = new Label("Aucun enregistrement trouvé.");
            empty.setStyle("-fx-text-fill: #64748b; -fx-font-size: 15px;");
            cardsPane.getChildren().add(empty);
            return;
        }
        for (Energie e : list) cardsPane.getChildren().add(buildCard(e));
    }

    private VBox buildCard(Energie e) {
        VBox card = new VBox(14);
        card.setPrefWidth(280);
        card.setPadding(new Insets(20));
        card.getStyleClass().add("card");

        // Header: Icon + Type
        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);

        StackPane avatar = new StackPane();
        avatar.setMinSize(42, 42);
        avatar.setMaxSize(42, 42);
        String type = e.getType_energie() != null ? e.getType_energie().toLowerCase() : "inconnu";
        String color = switch (type) {
            case "électricité", "electricite" -> "#f59e0b";
            case "eau" -> "#00d4ff";
            case "gaz" -> "#ef4444";
            default -> "#64748b";
        };
        avatar.setStyle("-fx-background-color: " + color + "22; -fx-background-radius: 21;");

        String icon = switch (type) {
            case "électricité", "electricite" -> "⚡";
            case "eau" -> "💧";
            case "gaz" -> "🔥";
            default -> "📊";
        };
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 18px;");
        avatar.getChildren().add(iconLabel);

        VBox titleBox = new VBox(2);
        HBox typeRow = new HBox(8);
        typeRow.setAlignment(Pos.CENTER_LEFT);
        Label typeLabel = new Label(e.getType_energie());
        typeLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 16px; -fx-font-weight: bold;");

        Label idBadge = new Label("#" + e.getId());
        idBadge.setStyle("-fx-text-fill: rgba(52,211,153,0.5); -fx-font-size: 10px; -fx-font-weight: bold;");
        typeRow.getChildren().addAll(typeLabel, idBadge);

        Label dateLabel = new Label(dateFormat.format(e.getDate_enregistrement()));
        dateLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");
        titleBox.getChildren().addAll(typeRow, dateLabel);

        topRow.getChildren().addAll(avatar, titleBox);

        // Value
        Label valLabel = new Label(String.format("%.1f kWh", e.getValeur()));
        valLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 20px; -fx-font-weight: bold;");

        // Source
        Label sourceLabel = new Label("Source: " + e.getSource());
        sourceLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px;");

        // Période
        Label periodeLabel = new Label("Période: " + (int) e.getPeriode() + " jours");
        periodeLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");

        // Actions (Modifier / Supprimer)
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button btnEdit = new Button("✎");
        btnEdit.setStyle("-fx-background-color: rgba(0,212,255,0.15); -fx-text-fill: #00d4ff; -fx-background-radius: 6; -fx-cursor: hand;");
        btnEdit.setOnAction(ev -> openEditForm(e));

        Button btnDel = new Button("✕");
        btnDel.setStyle("-fx-background-color: rgba(239,68,68,0.15); -fx-text-fill: #ef4444; -fx-background-radius: 6; -fx-cursor: hand;");
        btnDel.setOnAction(ev -> handleDelete(e));

        actions.getChildren().addAll(btnEdit, btnDel);

        card.getChildren().addAll(topRow, valLabel, sourceLabel, actions);
        return card;
    }

    @FXML
    private void handleSearch() {
        if (allEnergies == null) return;
        String q = searchField.getText().toLowerCase().trim();
        List<Energie> filtered = q.isEmpty() ? allEnergies : allEnergies.stream()
                .filter(e -> e.getType_energie().toLowerCase().contains(q) || e.getSource().toLowerCase().contains(q))
                .collect(Collectors.toList());
        renderCards(filtered);
    }

    @FXML
    private void handleSort() {
        if (allEnergies == null || sortCombo.getValue() == null) return;
        String option = sortCombo.getValue();
        switch (option) {
            case "Date (Récent)" -> allEnergies.sort((a, b) -> b.getDate_enregistrement().compareTo(a.getDate_enregistrement()));
            case "Date (Ancien)" -> allEnergies.sort((a, b) -> a.getDate_enregistrement().compareTo(b.getDate_enregistrement()));
            case "Valeur (Max)" -> allEnergies.sort((a, b) -> Float.compare(b.getValeur(), a.getValeur()));
            case "Valeur (Min)" -> allEnergies.sort((a, b) -> Float.compare(a.getValeur(), b.getValeur()));
            case "Type" -> allEnergies.sort((a, b) -> a.getType_energie().compareTo(b.getType_energie()));
        }
        handleSearch();
    }

    @FXML
    private void openAddForm() {
        showModal(null);
    }

    private void openEditForm(Energie e) {
        showModal(e);
    }

    private void showModal(Energie e) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/EnergieForm.fxml"));
            Parent root = loader.load();
            
            EnergieFormController controller = loader.getController();
            controller.setEnergie(e, false);
            
            Stage stage = new Stage();
            stage.setTitle(e == null ? "Nouvelle Consommation" : "Modifier Consommation");
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.initOwner(cardsPane.getScene().getWindow());
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styleFront.css").toExternalForm());
            stage.setScene(scene);
            stage.showAndWait();
            
            Energie result = controller.getResult("1");
            if (result != null) {
                if (e == null) {
                    serviceEnergie.ajouter(result);
                    generateAIRecommendation(result);
                } else {
                    serviceEnergie.modifier(result);
                }
                loadData();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir le formulaire : " + ex.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        // No longer needed
    }

    private void handleDelete(Energie e) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer");
        alert.setHeaderText("Supprimer cet enregistrement ?");
        alert.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    serviceEnergie.supprimer(e);
                    loadData();
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        });
    }

    private void showError(String msg) {
        if (errorLabel != null) {
            errorLabel.setText("⚠ " + msg);
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

    private void generateAIRecommendation(Energie e) {
        aiResultPanel.setVisible(true);
        aiResultPanel.setManaged(true);
        aiTextArea.setText("🤖 Analyse de votre consommation par l'IA Groq...");

        new Thread(() -> {
            try {
                String result = groqService.generateRecommendation(
                    e.getType_energie(), e.getValeur(), e.getSource(), e.getPeriode()
                );
                
                Platform.runLater(() -> aiTextArea.setText(result));

                // Save to DB
                Recommandation reco = new Recommandation(
                    "Conseil IA - " + e.getType_energie(),
                    result,
                    "élevé",
                    new java.util.Date(),
                    e.getId()
                );
                serviceRecommandation.modifier(reco);
            } catch (Exception ex) {
                Platform.runLater(() -> aiTextArea.setText("❌ Erreur IA : " + ex.getMessage()));
            }
        }).start();
    }

    @FXML
    private void generateIA() {
        aiResultPanel.setVisible(true);
        aiResultPanel.setManaged(true);
        aiTextArea.setText("🤖 L'IA analyse vos consommations...");

        new Thread(() -> {
            try {
                if (allEnergies == null || allEnergies.isEmpty()) {
                    Platform.runLater(() -> aiTextArea.setText("Aucune donnée de consommation trouvée."));
                    return;
                }
                Energie e = allEnergies.get(0);
                String result = groqService.generateRecommendation(
                    e.getType_energie(), e.getValeur(), e.getSource(), e.getPeriode()
                );
                Platform.runLater(() -> aiTextArea.setText(result));
            } catch (Exception ex) {
                Platform.runLater(() -> aiTextArea.setText("❌ Erreur IA : " + ex.getMessage()));
                ex.printStackTrace();
            }
        }).start();
    }

    @FXML
    private void closeAI() {
        aiResultPanel.setVisible(false);
        aiResultPanel.setManaged(false);
    }

    private void updateChart() {
        if (energyChart == null) return;
        energyChart.getData().clear();
        if (allEnergies == null || allEnergies.isEmpty()) return;

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Consommation");

        // Sort by date for chart continuity
        List<Energie> sortedForChart = allEnergies.stream()
                .sorted((a, b) -> a.getDate_enregistrement().compareTo(b.getDate_enregistrement()))
                .collect(Collectors.toList());

        for (Energie e : sortedForChart) {
            series.getData().add(new XYChart.Data<>(dateFormat.format(e.getDate_enregistrement()), e.getValeur()));
        }
        energyChart.getData().add(series);
    }

    @FXML
    private void handleExportPDF() {
        if (allEnergies == null || allEnergies.isEmpty()) {
            showAlert("Export", "Aucune donnée à exporter.");
            return;
        }
        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter en PDF");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        fc.setInitialFileName("AIVA_Energie_Rapport.pdf");
        File file = fc.showSaveDialog(cardsPane.getScene().getWindow());
        if (file != null) {
            try {
                List<Recommandation> recs = serviceRecommandation.recupererParUser("1");
                exportService.exportToPDF(allEnergies, recs, file.getAbsolutePath());
                showAlert("Succès", "Export PDF réussi avec conseils IA !");
            } catch (Exception e) {
                showAlert("Erreur", "Erreur export PDF : " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleExportWord() {
        if (allEnergies == null || allEnergies.isEmpty()) {
            showAlert("Export", "Aucune donnée à exporter.");
            return;
        }
        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter en Word");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Word Files", "*.docx"));
        fc.setInitialFileName("AIVA_Energie_Rapport.docx");
        File file = fc.showSaveDialog(cardsPane.getScene().getWindow());
        if (file != null) {
            try {
                List<Recommandation> recs = serviceRecommandation.recupererParUser("1");
                exportService.exportToWord(allEnergies, recs, file.getAbsolutePath());
                showAlert("Succès", "Export Word réussi avec conseils IA !");
            } catch (Exception e) {
                showAlert("Erreur", "Erreur export Word : " + e.getMessage());
            }
        }
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}

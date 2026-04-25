package Controllers.recommandation;

import Models.Recommandation;
import Services.ServiceRecommandation;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import Services.GroqService;
import Services.ServiceEnergie;
import Models.Energie;
import javafx.application.Platform;
import java.io.IOException;

public class RecommandationControllerFront implements Initializable {

    // Stats
    @FXML private Label statTotal;
    @FXML private Label statHighImpact;
    @FXML private Label statLowImpact;

    // Search & Sort
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortCombo;

    // Grid
    @FXML private FlowPane cardsPane;

    // AI Section
    @FXML private VBox aiSection;
    @FXML private TextArea aiResultArea;

    private ServiceRecommandation serviceRecommandation;
    private ServiceEnergie serviceEnergie;
    private GroqService groqService;
    private List<Recommandation> allRecommendations;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        serviceRecommandation = new ServiceRecommandation();
        serviceEnergie = new ServiceEnergie();
        groqService = new GroqService();

        if (sortCombo != null) {
            sortCombo.setItems(javafx.collections.FXCollections.observableArrayList(
                "Date (Récent)", "Date (Ancien)", "Impact", "Titre"
            ));
        }

        loadData();
    }

    private void loadData() {
        try {
            // Using hardcoded user_id "1" for front office
            allRecommendations = serviceRecommandation.recupererParUser("1");
            updateStats();
            renderCards(allRecommendations);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateStats() {
        if (allRecommendations == null || allRecommendations.isEmpty()) {
            statTotal.setText("0");
            statHighImpact.setText("0");
            statLowImpact.setText("0");
            return;
        }

        long total = allRecommendations.size();
        long high = allRecommendations.stream()
                .filter(r -> r.getNiveau_impact().equalsIgnoreCase("Élevé") || r.getNiveau_impact().equalsIgnoreCase("high"))
                .count();
        long low = allRecommendations.stream()
                .filter(r -> r.getNiveau_impact().equalsIgnoreCase("Faible") || r.getNiveau_impact().equalsIgnoreCase("low"))
                .count();

        statTotal.setText(String.valueOf(total));
        statHighImpact.setText(String.valueOf(high));
        statLowImpact.setText(String.valueOf(low));
    }

    private void renderCards(List<Recommandation> list) {
        cardsPane.getChildren().clear();
        if (list == null || list.isEmpty()) {
            Label empty = new Label("Aucun conseil disponible pour le moment.");
            empty.setStyle("-fx-text-fill: #64748b; -fx-font-size: 15px;");
            cardsPane.getChildren().add(empty);
            return;
        }
        for (Recommandation r : list) cardsPane.getChildren().add(buildCard(r));
    }

    private VBox buildCard(Recommandation r) {
        VBox card = new VBox(14);
        card.setPrefWidth(300);
        card.setPadding(new Insets(20));
        card.getStyleClass().add("card");

        // Header: Impact Badge + Date
        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);

        String impact = r.getNiveau_impact() != null ? r.getNiveau_impact().toLowerCase() : "inconnu";
        String impactColor = switch (impact) {
            case "élevé", "high" -> "#ef4444";
            case "moyen", "medium" -> "#f59e0b";
            case "faible", "low" -> "#10b981";
            default -> "#64748b";
        };

        Label impactBadge = new Label(impact.toUpperCase());
        impactBadge.setStyle("-fx-text-fill: " + impactColor + "; -fx-background-color: " + impactColor + "22; " +
                "-fx-background-radius: 6; -fx-padding: 4 10; -fx-font-size: 11px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label dateLabel = new Label(dateFormat.format(r.getDate_generation()));
        dateLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");

        Label idEnergie = new Label("Ref: #" + r.getEnergie_id());
        idEnergie.setStyle("-fx-text-fill: rgba(52,211,153,0.4); -fx-font-size: 10px; -fx-font-weight: bold;");

        topRow.getChildren().addAll(impactBadge, spacer, new VBox(2, dateLabel, idEnergie));

        // Content
        Label titleLabel = new Label(r.getTitre());
        titleLabel.setWrapText(true);
        titleLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 16px; -fx-font-weight: bold;");

        Label descLabel = new Label(r.getDescription());
        descLabel.setWrapText(true);
        descLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px; -fx-line-spacing: 4;");
        descLabel.setMinHeight(80);

        // Icon indicator
        HBox bottomRow = new HBox(8);
        bottomRow.setAlignment(Pos.CENTER_RIGHT);
        Label bulb = new Label("💡");
        bulb.setStyle("-fx-opacity: 0.3; -fx-font-size: 20px;");
        bottomRow.getChildren().add(bulb);

        card.getChildren().addAll(topRow, titleLabel, descLabel, bottomRow);
        return card;
    }

    @FXML
    private void handleSearch() {
        if (allRecommendations == null) return;
        String q = searchField.getText().toLowerCase().trim();
        List<Recommandation> filtered = q.isEmpty() ? allRecommendations : allRecommendations.stream()
                .filter(r -> r.getTitre().toLowerCase().contains(q) || r.getDescription().toLowerCase().contains(q))
                .collect(Collectors.toList());
        renderCards(filtered);
    }

    @FXML
    private void handleSort() {
        if (allRecommendations == null || sortCombo.getValue() == null) return;
        String option = sortCombo.getValue();
        switch (option) {
            case "Date (Récent)" -> allRecommendations.sort((a, b) -> b.getDate_generation().compareTo(a.getDate_generation()));
            case "Date (Ancien)" -> allRecommendations.sort((a, b) -> a.getDate_generation().compareTo(b.getDate_generation()));
            case "Impact" -> allRecommendations.sort((a, b) -> a.getNiveau_impact().compareTo(b.getNiveau_impact()));
            case "Titre" -> allRecommendations.sort((a, b) -> a.getTitre().compareTo(b.getTitre()));
        }
        handleSearch();
    }

    @FXML
    private void generateIA() {
        aiSection.setVisible(true);
        aiSection.setManaged(true);
        aiResultArea.setText("🤖 L'IA analyse vos dernières consommations...");

        new Thread(() -> {
            try {
                // Récupérer les dernières données énergétiques
                List<Energie> energies = serviceEnergie.recupererParUser("1");
                if (energies.isEmpty()) {
                    Platform.runLater(() -> aiResultArea.setText("Aucune donnée de consommation trouvée pour générer une recommandation IA."));
                    return;
                }

                // Utiliser la plus récente
                Energie e = energies.get(0);
                String result = groqService.generateRecommendation(e.getType_energie(), e.getValeur(), e.getSource(), e.getPeriode());

                Platform.runLater(() -> aiResultArea.setText(result));
            } catch (Exception e) {
                Platform.runLater(() -> aiResultArea.setText("❌ Erreur lors de la génération IA : " + e.getMessage()));
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    private void closeAI() {
        aiSection.setVisible(false);
        aiSection.setManaged(false);
    }
}

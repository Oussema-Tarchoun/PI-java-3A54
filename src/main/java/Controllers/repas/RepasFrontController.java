package Controllers.repas;

import Models.Repas;
import Models.Aliment;
import Models.AnalyseNutritionnelle;
import Services.ServiceRepas;
import Services.ServiceAliment;
import Services.EmailService;
import Services.AnalyseService;
import Services.ServiceAnalyse;
import Controllers.ChatBotFrontController;
import Services.ChatRepasParser.ParsedMeal;
import com.fasterxml.jackson.databind.JsonNode;
import utils.SessionManager;
import Models.User;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.net.URL;
import java.sql.Date;
import java.sql.SQLDataException;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import java.net.URL;
import java.sql.Date;
import java.sql.SQLDataException;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class RepasFrontController implements Initializable {

    // ── Stats labels ──────────────────────────────────────────────────────────
    @FXML
    private Label statTotal;
    @FXML
    private Label statAvgCal;
    @FXML
    private Label statCeMois;
    @FXML
    private Label statScore;

    // ── Search + filter ───────────────────────────────────────────────────────
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> filterType;

    // ── Card grid ─────────────────────────────────────────────────────────────
    @FXML
    private FlowPane cardsPane;

    @FXML
    private ComboBox<Aliment> alimentCombo;
    @FXML
    private VBox selectedAlimentsBox;
    private List<Aliment> selectedAliments = new ArrayList<>();

    // ── Add form panel ────────────────────────────────────────────────────────
    @FXML
    private VBox formPanel;
    @FXML
    private Label formTitle;
    @FXML
    private TextField nomField;
    @FXML
    private ComboBox<String> typeCombo;
    @FXML
    private DatePicker datePicker;
    @FXML
    private TextField heureField;
    @FXML
    private TextField caloriesField;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private Label errorLabel;

    private ServiceRepas serviceRepas;
    private List<Repas> allRepas;
    private Repas editingRepas;
    private int userId = 1; // Default fallback
    private ServiceAliment serviceAliment = new ServiceAliment();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        User current = SessionManager.getCurrentUser();
        if (current != null) {
            this.userId = current.getId();
        }

        try {
            serviceRepas = new ServiceRepas();
        } catch (Exception e) {
            System.out.println("RepasFront init error: " + e.getMessage());
        }

        if (filterType != null) {
            filterType.getItems().addAll("Tous", "petit-dejeuner", "dejeuner", "diner", "collation");
            filterType.setValue("Tous");
            filterType.setOnAction(e -> applyFilter());
        }

        if (typeCombo != null) {
            typeCombo.getItems().addAll("petit-dejeuner", "dejeuner", "diner", "collation");
        }
        // Charger aliments dans combo
        try {
            List<Aliment> aliments = serviceAliment.recuperer();
            alimentCombo.getItems().addAll(aliments);
            alimentCombo.setConverter(new javafx.util.StringConverter<>() {
                public String toString(Aliment a) {
                    return a == null ? "" : a.getNom() + " (" + (int) a.getCalories() + " kcal)";
                }

                public Aliment fromString(String s) {
                    return null;
                }
            });
        } catch (Exception e) {
            System.out.println("aliments load error: " + e.getMessage());
        }

        if (formPanel != null)
            formPanel.setVisible(false);

        loadData();
    }

    @FXML
    private void handleAddAliment() {
        Aliment selected = alimentCombo.getValue();
        if (selected == null)
            return;
        if (selectedAliments.stream().anyMatch(a -> a.getId() == selected.getId()))
            return;

        selectedAliments.add(selected);

        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: #1e293b; -fx-padding: 6 10; -fx-background-radius: 6;");

        Label lbl = new Label("• " + selected.getNom() + " — " + (int) selected.getCalories() + " kcal");
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

    @FXML
    private void handleRepasIdealDemain() {
        executor.submit(() -> {
            try {
                // 1. Récupérer repas d'aujourd'hui
                List<Repas> tousLesRepas = serviceRepas.recuperer();
                LocalDate today = LocalDate.now();
                List<Repas> repasAujourdhui = tousLesRepas.stream()
                        .filter(r -> r.getDate() != null && r.getDate().toLocalDate().equals(today))
                        .collect(Collectors.toList());

                // 2. Calculer ce qui manque
                int totalCalAujourdhui = repasAujourdhui.stream().mapToInt(Repas::getCalories).sum();
                int caloriesManquantes = Math.max(0, 2000 - totalCalAujourdhui);

                StringBuilder repasInfo = new StringBuilder();
                repasInfo.append("Repas d'aujourd'hui (").append(today).append("):\n");
                if (repasAujourdhui.isEmpty()) {
                    repasInfo.append("Aucun repas aujourd'hui.\n");
                } else {
                    repasAujourdhui.forEach(r -> repasInfo.append("- ").append(r.getNom())
                            .append(" (").append(r.getCalories()).append(" kcal, ").append(r.getType()).append(")\n"));
                }
                repasInfo.append("Total calories aujourd'hui: ").append(totalCalAujourdhui).append(" kcal\n");
                repasInfo.append("Calories manquantes pour atteindre 2000 kcal: ").append(caloriesManquantes)
                        .append(" kcal\n");

                // 3. Appel Groq
                String prompt = "Tu es un nutritionniste. Voici les repas d'aujourd'hui d'un utilisateur:\n\n"
                        + repasInfo
                        + "\nPropose UN repas idéal pour demain matin qui complète ce qui manque "
                        + "(protéines, glucides, légumes). Réponds en français avec ce format EXACT:\n\n"
                        + "**NOM_DU_PLAT** | TOTAL kcal\n"
                        + "• INGREDIENT — QUANTITE g (CALORIES kcal)\n"
                        + "💡 NOTE: conseil nutritionnel\n"
                        + "RECETTE:\n1. étape\n2. étape";

                Services.GroqService groq = new Services.GroqService();
                String response = groq.chat(prompt);

                // 4. Parser + afficher
                Services.ChatRepasParser.ParsedMeal meal = Services.ChatRepasParser.parse(response);

                Platform.runLater(
                        () -> afficherPopupRepasIdeal(meal, response, totalCalAujourdhui, caloriesManquantes));

            } catch (Exception e) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Erreur");
                    alert.setHeaderText("Impossible de générer le repas");
                    alert.setContentText(e.getMessage());
                    alert.showAndWait();
                });
            }
        });
    }

    private void afficherPopupRepasIdeal(Services.ChatRepasParser.ParsedMeal meal, String rawResponse,
            int calAujourdhui, int calManquantes) {
        Stage popup = new Stage();
        popup.setTitle("🍽️ Repas idéal pour demain");
        popup.initModality(Modality.APPLICATION_MODAL);

        VBox root = new VBox(14);
        root.setStyle("-fx-background-color: #0a0e1a; -fx-padding: 24;");
        root.setPrefWidth(500);

        // Header
        Label title = new Label("🍽️ Repas idéal pour demain");
        title.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 18px; -fx-font-weight: bold;");

        // Stats aujourd'hui
        HBox statsBox = new HBox(12);
        statsBox.getChildren().addAll(
                statCard(calAujourdhui + " kcal", "Aujourd'hui"),
                statCard(calManquantes + " kcal", "Manquantes"),
                statCard("2000 kcal", "Objectif"));

        root.getChildren().addAll(title, statsBox);

        // Barre de progression
        double progress = Math.min(1.0, calAujourdhui / 2000.0);
        String progressColor = progress >= 0.8 ? "#10b981" : progress >= 0.5 ? "#f59e0b" : "#ef4444";
        Label progressLbl = new Label("Progression journalière : " + (int) (progress * 100) + "%");
        progressLbl.setStyle("-fx-text-fill: " + progressColor + "; -fx-font-size: 12px; -fx-font-weight: bold;");
        root.getChildren().add(progressLbl);

        root.getChildren().add(sectionLbl("💡 PROPOSITION POUR DEMAIN MATIN"));

        if (meal != null && meal.isValid()) {
            // Nom + calories
            Label nomLbl = new Label("🍳 " + meal.name + "  —  🔥 " + meal.totalCalories + " kcal");
            nomLbl.setStyle("-fx-text-fill: #f97316; -fx-font-size: 15px; -fx-font-weight: bold;");
            nomLbl.setWrapText(true);
            root.getChildren().add(nomLbl);

            // Ingrédients
            root.getChildren().add(sectionLbl("🥗 INGRÉDIENTS"));
            meal.ingredients.forEach(ing -> {
                HBox row = new HBox(8);
                row.setStyle("-fx-background-color: #161b2e; -fx-padding: 8 12; -fx-background-radius: 6;");
                Label lbl = new Label("• " + ing.name + " — " + ing.quantity + " (" + ing.calories + " kcal)");
                lbl.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 12px;");
                row.getChildren().add(lbl);
                root.getChildren().add(row);
            });

            // Note
            if (meal.note != null && !meal.note.isEmpty()) {
                Label noteLbl = new Label("💡 " + meal.note);
                noteLbl.setWrapText(true);
                noteLbl.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px; -fx-font-style: italic;");
                root.getChildren().add(noteLbl);
            }

            // Étapes
            if (!meal.steps.isEmpty()) {
                root.getChildren().add(sectionLbl("📋 RECETTE"));
                for (int i = 0; i < meal.steps.size(); i++) {
                    Label step = new Label((i + 1) + ". " + meal.steps.get(i));
                    step.setWrapText(true);
                    step.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
                    root.getChildren().add(step);
                }
            }
        } else {
            // Fallback texte brut
            Label raw = new Label(rawResponse.replaceAll("\\*+", "").trim());
            raw.setWrapText(true);
            raw.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 12px;");
            root.getChildren().add(raw);
        }

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #0a0e1a; -fx-background-color: #0a0e1a;");
        popup.setScene(new Scene(scroll, 540, 600));
        popup.show();
    }

    @FXML
    private void handleAnalyseSemaine() {
        executor.submit(() -> {
            try {
                // 1. Récupérer repas des 7 derniers jours
                ServiceAnalyse serviceAnalyse = new ServiceAnalyse();
                List<Repas> repas7j = serviceAnalyse.getRepas7Jours(userId);

                if (repas7j.isEmpty()) {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Analyse");
                        alert.setHeaderText("Aucun repas trouvé");
                        alert.setContentText("Vous n'avez aucun repas enregistré dans les 7 derniers jours.");
                        alert.showAndWait();
                    });
                    return;
                }

                // 2. Appel Groq
                AnalyseService analyseService = new AnalyseService();
                JsonNode result = analyseService.analyserSemaine(repas7j);

                // 3. Sauvegarder en BD
                AnalyseNutritionnelle analyse = new AnalyseNutritionnelle();
                analyse.setUserId(userId);
                analyse.setDateGeneration(new java.sql.Timestamp(System.currentTimeMillis()));
                analyse.setPeriodeDebut(java.sql.Date.valueOf(java.time.LocalDate.now().minusDays(7)));
                analyse.setPeriodeFin(java.sql.Date.valueOf(java.time.LocalDate.now()));
                analyse.setScore(result.get("score").asInt());
                analyse.setContenuJson(result.toString());
                serviceAnalyse.sauvegarder(analyse);

                // 4. Afficher le popup
                Platform.runLater(() -> afficherPopupAnalyse(result));

            } catch (Exception e) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Erreur");
                    alert.setHeaderText("Analyse impossible");
                    alert.setContentText(e.getMessage());
                    alert.showAndWait();
                });
            }
        });
    }

    private void afficherPopupAnalyse(JsonNode data) {
        Stage popup = new Stage();
        popup.setTitle("Analyse de la semaine");
        popup.initModality(Modality.APPLICATION_MODAL);

        VBox root = new VBox(12);
        root.setStyle("-fx-background-color: #0a0e1a; -fx-padding: 24;");
        root.setPrefWidth(500);

        // Score + résumé
        int score = data.get("score").asInt();
        String scoreColor = score >= 75 ? "#10b981" : score >= 50 ? "#f59e0b" : "#ef4444";

        Label scoreLbl = new Label(score + "/100");
        scoreLbl.setStyle("-fx-text-fill: " + scoreColor + "; -fx-font-size: 36px; -fx-font-weight: bold;");

        Label resumeLbl = new Label(data.get("resume").asText());
        resumeLbl.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 14px;");
        resumeLbl.setWrapText(true);

        // Stats
        JsonNode stats = data.get("stats");
        HBox statsBox = new HBox(16);
        statsBox.getChildren().addAll(
                statCard(stats.get("total_repas").asText(), "Repas"),
                statCard(stats.get("total_jours").asText(), "Jours"),
                statCard(stats.get("kcal_moy_par_jour").asText() + " kcal", "kcal/jour moy."));

        root.getChildren().addAll(scoreLbl, resumeLbl, statsBox);

        // Points positifs
        root.getChildren().add(sectionLbl("✅ POINTS POSITIFS"));
        data.get("points_positifs").forEach(p -> root.getChildren().add(bulletLbl(p.asText(), "#86efac")));

        // À améliorer
        root.getChildren().add(sectionLbl("⚠️ À AMÉLIORER"));
        data.get("a_ameliorer").forEach(p -> root.getChildren().add(bulletLbl(p.asText(), "#fbbf24")));

        // Analyse par jour
        root.getChildren().add(sectionLbl("📅 ANALYSE PAR JOUR"));
        data.get("analyse_par_jour").forEach(j -> {
            HBox row = new HBox(10);
            Label date = new Label(j.get("date").asText());
            date.setStyle("-fx-text-fill: #94a3b8; -fx-font-weight: bold; -fx-min-width: 110;");
            Label comm = new Label(j.get("commentaire").asText());
            comm.setWrapText(true);
            comm.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 12px;");
            HBox.setHgrow(comm, Priority.ALWAYS);
            row.getChildren().addAll(date, comm);
            root.getChildren().add(row);
        });

        // Conseils
        root.getChildren().add(sectionLbl("💡 CONSEILS PERSONNALISÉS"));
        data.get("conseils").forEach(p -> root.getChildren().add(bulletLbl(p.asText(), "#93c5fd")));

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #0a0e1a; -fx-background-color: #0a0e1a;");

        popup.setScene(new Scene(scroll, 540, 620));
        popup.show();
    }

    private VBox statCard(String val, String label) {
        VBox box = new VBox(2);
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-background-color: #161b2e; -fx-padding: 10 20; -fx-background-radius: 8;");
        Label v = new Label(val);
        v.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
        Label l = new Label(label);
        l.setStyle("-fx-text-fill: #64748b; -fx-font-size: 10px;");
        box.getChildren().addAll(v, l);
        return box;
    }

    private Label sectionLbl(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #f1f5f9; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 8 0 2 0;");
        return l;
    }

    private Label bulletLbl(String text, String color) {
        Label l = new Label("• " + text);
        l.setWrapText(true);
        l.setMaxWidth(460);
        l.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px; " +
                "-fx-background-color: #161b2e; -fx-padding: 8 12; -fx-background-radius: 6;");
        return l;
    }
    // ── Data ──────────────────────────────────────────────────────────────────

    private void loadData() {
        try {
            allRepas = serviceRepas.recupererParUser(userId);
            updateStats();
            renderCards(allRepas);
        } catch (Exception e) {
            System.out.println("loadData error: " + e.getMessage());
        }
    }

    private void updateStats() {
        if (allRepas == null)
            return;
        int total = allRepas.size();
        if (statTotal != null)
            statTotal.setText(String.valueOf(total));

        if (total > 0) {
            double avg = allRepas.stream().mapToInt(Repas::getCalories).average().orElse(0);
            if (statAvgCal != null)
                statAvgCal.setText((int) avg + " kcal");

            LocalDate now = LocalDate.now();
            long thisMo = allRepas.stream()
                    .filter(r -> r.getDate() != null &&
                            r.getDate().toLocalDate().getMonthValue() == now.getMonthValue() &&
                            r.getDate().toLocalDate().getYear() == now.getYear())
                    .count();
            if (statCeMois != null)
                statCeMois.setText(String.valueOf(thisMo));

            double avgCal = allRepas.stream().mapToInt(Repas::getCalories).average().orElse(0);
            if (statScore != null)
                statScore.setText(avgCal <= 500 ? "Excellent" : avgCal <= 700 ? "Bon" : "Élevé");
        } else {
            if (statAvgCal != null)
                statAvgCal.setText("— kcal");
            if (statCeMois != null)
                statCeMois.setText("0");
            if (statScore != null)
                statScore.setText("—");
        }
    }

    private void renderCards(List<Repas> list) {
        if (cardsPane == null)
            return;
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

        HBox topRow = new HBox();
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label typeBadge = new Label(r.getType() != null ? r.getType() : "—");
        typeBadge.setStyle(getBadgeStyle(r.getType()));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label calLabel = new Label(r.getCalories() + " kcal");
        calLabel.setStyle("-fx-text-fill: #00d4ff; -fx-font-size: 13px; -fx-font-weight: bold;");

        topRow.getChildren().addAll(typeBadge, spacer, calLabel);

        Label nomLabel = new Label(r.getNom());
        nomLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 17px; -fx-font-weight: bold;");
        nomLabel.setWrapText(true);

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

        String score = r.getCalories() <= 400 ? "Excellent" : r.getCalories() <= 700 ? "Bon" : "Élevé";
        String scoreColor = r.getCalories() <= 400 ? "#10b981" : r.getCalories() <= 700 ? "#f59e0b" : "#ef4444";
        Label scoreLabel = new Label("Score : " + score);
        scoreLabel.setStyle("-fx-text-fill: " + scoreColor + "; -fx-font-size: 12px; " +
                "-fx-background-color: " + scoreColor + "22; -fx-background-radius: 4; -fx-padding: 4 8 4 8;");

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

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button btnView = new Button("👁 Voir");
        btnView.setStyle("-fx-background-color: rgba(16,185,129,0.15); -fx-text-fill: #10b981; " +
                "-fx-background-radius: 6; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 6 12 6 12;");
        btnView.setOnAction(e -> afficherDetailRepas(r));

        Button btnEdit = new Button("✎ Modifier");
        btnEdit.setStyle("-fx-background-color: rgba(0,212,255,0.15); -fx-text-fill: #00d4ff; " +
                "-fx-background-radius: 6; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 6 12 6 12;");
        btnEdit.setOnAction(e -> openEditForm(r));

        Button btnDel = new Button("✕ Supprimer");
        btnDel.setStyle("-fx-background-color: rgba(239,68,68,0.15); -fx-text-fill: #ef4444; " +
                "-fx-background-radius: 6; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 6 12 6 12;");
        btnDel.setOnAction(e -> handleDelete(r));

        actions.getChildren().addAll(btnView, btnEdit, btnDel);
        card.getChildren().add(actions);
        return card;
    }

    private String getBadgeStyle(String type) {
        String color = "#8b5cf6";
        if (type != null) {
            color = switch (type.toLowerCase().trim()) {
                case "déjeuner", "dejeuner" -> "#00d4ff";
                case "dîner", "diner" -> "#8b5cf6";
                case "collation" -> "#f59e0b";
                case "petit-déjeuner", "petit déjeuner",
                        "petit-dejeuner", "petit dejeuner" ->
                    "#10b981";
                default -> "#8b5cf6";
            };
        }
        return "-fx-text-fill: " + color + "; -fx-background-color: " + color + "22; " +
                "-fx-background-radius: 4; -fx-padding: 4 10 4 10; -fx-font-size: 11px; -fx-font-weight: bold;";
    }

    private void afficherDetailRepas(Repas r) {
        Stage popup = new Stage();
        popup.setTitle("Détails du repas");
        popup.initModality(Modality.APPLICATION_MODAL);

        VBox root = new VBox(14);
        root.setStyle("-fx-background-color: #0a0e1a; -fx-padding: 24;");
        root.setPrefWidth(480);

        // Header
        Label nom = new Label("🍽️ " + r.getNom());
        nom.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 20px; -fx-font-weight: bold;");

        HBox meta = new HBox(16);
        meta.getChildren().addAll(
                styledLabel("📅 " + (r.getDate() != null ? r.getDate() : "—"), "#94a3b8"),
                styledLabel("🕐 " + (r.getHeure() != null ? r.getHeure().toString().substring(0, 5) : "—"), "#94a3b8"),
                styledLabel("🔥 " + r.getCalories() + " kcal", "#f97316"),
                styledLabel(r.getType() != null ? r.getType() : "—", "#8b5cf6"));

        root.getChildren().addAll(nom, meta);

        if (r.getDescription() != null && !r.getDescription().isBlank()) {
            Label desc = new Label(r.getDescription());
            desc.setWrapText(true);
            desc.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");
            root.getChildren().add(desc);
        }

        // Aliments liés
        root.getChildren().add(sectionLbl("🥗 ALIMENTS LIÉS"));
        try {
            List<Integer> ids = serviceRepas.getLinkedAlimentIds(r.getId());
            if (ids.isEmpty()) {
                root.getChildren().add(styledLabel("Aucun aliment lié.", "#64748b"));
            } else {
                List<Aliment> aliments = serviceAliment.recuperer();
                for (Aliment a : aliments) {
                    if (ids.contains(a.getId())) {
                        HBox row = new HBox(8);
                        row.setStyle("-fx-background-color: #161b2e; -fx-padding: 8 12; -fx-background-radius: 6;");
                        Label lbl = new Label("• " + a.getNom() + " — " + (int) a.getQuantite() + "g — "
                                + (int) a.getCalories() + " kcal");
                        lbl.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 12px;");
                        row.getChildren().add(lbl);
                        root.getChildren().add(row);
                    }
                }
            }
        } catch (Exception e) {
            root.getChildren().add(styledLabel("Erreur chargement aliments.", "#ef4444"));
        }

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #0a0e1a; -fx-background-color: #0a0e1a;");
        popup.setScene(new Scene(scroll, 500, 500));
        popup.show();
    }

    private Label styledLabel(String text, String color) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px;");
        return l;
    }

    // ── Search & Filter ───────────────────────────────────────────────────────

    @FXML
    private void handleSearch() {
        applyFilter();
    }

    private void applyFilter() {
        if (allRepas == null)
            return;
        String search = searchField != null ? searchField.getText().toLowerCase().trim() : "";
        String type = filterType != null ? filterType.getValue() : "Tous";

        List<Repas> filtered = allRepas.stream()
                .filter(r -> search.isBlank() ||
                        r.getNom().toLowerCase().contains(search) ||
                        (r.getDescription() != null && r.getDescription().toLowerCase().contains(search)))
                .filter(r -> "Tous".equals(type) || type.equals(r.getType()))
                .collect(Collectors.toList());

        renderCards(filtered);
    }

    @FXML
    private void handleEnvoyerRapport() {
        executor.submit(() -> {
            try {
                ServiceAnalyse serviceAnalyse = new ServiceAnalyse();
                List<Repas> repas7j = serviceAnalyse.getRepas7Jours(userId);

                if (repas7j.isEmpty()) {
                    Platform.runLater(() -> {
                        Alert a = new Alert(Alert.AlertType.INFORMATION);
                        a.setTitle("Email");
                        a.setHeaderText("Aucun repas");
                        a.setContentText("Aucun repas dans les 7 derniers jours.");
                        a.showAndWait();
                    });
                    return;
                }

                // Générer l'analyse
                AnalyseService analyseService = new AnalyseService();
                com.fasterxml.jackson.databind.JsonNode result = analyseService.analyserSemaine(repas7j);

                // Envoyer le mail
                EmailService emailService = new EmailService();
                emailService.sendRapportNutrition(
                        "mehdi@gmail.com", // ← remplace par le vrai email user
                        "mehdi", // ← remplace par le vrai nom user
                        result);

                Platform.runLater(() -> {
                    Alert a = new Alert(Alert.AlertType.INFORMATION);
                    a.setTitle("Email envoyé");
                    a.setHeaderText("✅ Rapport envoyé !");
                    a.setContentText("Vérifiez votre boîte mail.");
                    a.showAndWait();
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    Alert a = new Alert(Alert.AlertType.ERROR);
                    a.setTitle("Erreur");
                    a.setHeaderText("Envoi échoué");
                    a.setContentText(e.getMessage());
                    a.showAndWait();
                });
            }
        });
    }

    // ── Form ─────────────────────────────────────────────────────────────────

    @FXML
    private void openAddForm() {
        editingRepas = null;
        if (formTitle != null)
            formTitle.setText("➕ Nouveau Repas");
        clearForm();
        showForm(true);
    }

    private void openEditForm(Repas r) {
        editingRepas = r;
        if (formTitle != null)
            formTitle.setText("✎ Modifier : " + r.getNom());
        if (nomField != null)
            nomField.setText(r.getNom());
        if (typeCombo != null)
            typeCombo.setValue(r.getType());
        if (datePicker != null && r.getDate() != null)
            datePicker.setValue(r.getDate().toLocalDate());
        if (heureField != null)
            heureField.setText(r.getHeure() != null ? r.getHeure().toString().substring(0, 5) : "");
        if (caloriesField != null)
            caloriesField.setText(String.valueOf(r.getCalories()));
        if (descriptionArea != null)
            descriptionArea.setText(r.getDescription() != null ? r.getDescription() : "");
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
        if (typeCombo.getValue() == null) {
            showError("Veuillez sélectionner un type.");
            return;
        }
        if (datePicker.getValue() == null) {
            showError("Veuillez sélectionner une date.");
            return;
        }

        String heureStr = heureField.getText().trim();
        if (heureStr.isBlank()) {
            showError("L'heure est obligatoire (ex: 12:30).");
            return;
        }

        Time heure;
        try {
            if (heureStr.matches("\\d{1,2}:\\d{2}"))
                heureStr += ":00";
            heure = Time.valueOf(heureStr);
        } catch (IllegalArgumentException e) {
            showError("Format d'heure invalide. Utilisez HH:mm.");
            return;
        }

        int calories = 0;
        String calStr = caloriesField.getText().trim();
        if (!calStr.isBlank()) {
            try {
                calories = Integer.parseInt(calStr);
                if (calories < 0) {
                    showError("Les calories ne peuvent pas être négatives.");
                    return;
                }
            } catch (NumberFormatException e) {
                showError("Les calories doivent être un nombre entier.");
                return;
            }
        }

        String desc = descriptionArea != null && descriptionArea.getText() != null
                ? descriptionArea.getText().trim()
                : "";

        try {
            if (editingRepas == null) {
                Repas r = new Repas(userId, nom, heure, calories, desc,
                        typeCombo.getValue(), Date.valueOf(datePicker.getValue()));
                try {
                    serviceRepas.addWithAliments(r, selectedAliments);
                } catch (Exception ex) {
                    serviceRepas.ajouter(r);
                }
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

    // ── Chatbot Integration ───────────────────────────────────────────────────

    @FXML
    private void handleChatbotGenerate() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/ChatbotView.fxml"));
            Parent root = loader.load();

            // Get the controller and set callback
            ChatBotFrontController chatController = loader.getController();
            chatController.setAddRepasCallback(this::addRepasFromChatbot);

            Stage stage = new Stage();
            stage.setTitle("Assistant Repas IA");
            stage.initModality(Modality.NONE);
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Chatbot open error: " + e.getMessage());
        }
    }

    /**
     * Called by chatbot when user clicks "Ajouter ce repas"
     * Automatically uses current PC time and today's date
     */
    private void addRepasFromChatbot(ParsedMeal meal) {
        try {
            LocalTime now = LocalTime.now();
            Time currentTime = Time.valueOf(now.withSecond(0).withNano(0));
            Date today = Date.valueOf(LocalDate.now());
            String type = guessTypeFromHour(now.getHour());

            StringBuilder desc = new StringBuilder();
            if (meal.note != null && !meal.note.isEmpty())
                desc.append(meal.note);
            if (!meal.ingredients.isEmpty()) {
                if (desc.length() > 0)
                    desc.append(" | ");
                desc.append("Ingrédients: ");
                desc.append(meal.ingredients.stream()
                        .map(i -> i.name + " (" + i.quantity + ")")
                        .collect(Collectors.joining(", ")));
            }

            Repas repas = new Repas(userId, meal.name, currentTime, meal.totalCalories,
                    desc.toString(), type, today);

            // Créer/trouver les aliments
            List<Aliment> aliments = new ArrayList<>();
            for (Services.ChatRepasParser.ParsedIngredient ing : meal.ingredients) {
                Aliment a = serviceAliment.findByNameOrCreate(userId, ing.name, ing.calories, ing.quantity);
                if (a != null)
                    aliments.add(a);
            }

            // Sauvegarder repas + lier aliments
            try {
                serviceRepas.addWithAliments(repas, aliments);
            } catch (Exception ex) {
                serviceRepas.ajouter(repas);
            }

            loadData();

        } catch (Exception e) {
            System.out.println("addRepasFromChatbot error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Guess meal type based on current hour
     */
    private String guessTypeFromHour(int hour) {
        if (hour >= 6 && hour < 10)
            return "Petit-déjeuner";
        if (hour >= 11 && hour < 15)
            return "Déjeuner";
        if (hour >= 19 && hour < 23)
            return "Dîner";
        return "Collation";
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
        if (typeCombo != null)
            typeCombo.setValue(null);
        if (datePicker != null)
            datePicker.setValue(null);
        if (heureField != null)
            heureField.clear();
        if (caloriesField != null)
            caloriesField.clear();
        if (descriptionArea != null)
            descriptionArea.clear();
        selectedAliments.clear();
        if (selectedAlimentsBox != null)
            selectedAlimentsBox.getChildren().clear();
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

    @FXML
    private void handleHistoriqueAnalyses() {
        try {
            ServiceAnalyse serviceAnalyse = new ServiceAnalyse();
            List<AnalyseNutritionnelle> historique = serviceAnalyse.getHistorique(userId);

            Stage popup = new Stage();
            popup.setTitle("Historique des analyses");
            popup.initModality(Modality.APPLICATION_MODAL);

            VBox root = new VBox(12);
            root.setStyle("-fx-background-color: #0a0e1a; -fx-padding: 24;");
            root.setPrefWidth(560);

            Label title = new Label("🕓 Historique des analyses nutritionnelles");
            title.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 16px; -fx-font-weight: bold;");
            root.getChildren().add(title);

            if (historique.isEmpty()) {
                Label empty = new Label("Aucune analyse enregistrée.");
                empty.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px;");
                root.getChildren().add(empty);
            } else {
                for (AnalyseNutritionnelle a : historique) {
                    root.getChildren().add(buildHistoriqueCard(a));
                }
            }

            ScrollPane scroll = new ScrollPane(root);
            scroll.setFitToWidth(true);
            scroll.setStyle("-fx-background: #0a0e1a; -fx-background-color: #0a0e1a;");

            popup.setScene(new Scene(scroll, 580, 600));
            popup.show();

        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Impossible de charger l'historique");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    private VBox buildHistoriqueCard(AnalyseNutritionnelle a) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: #161b2e; -fx-padding: 14; " +
                "-fx-background-radius: 10; -fx-border-color: #2a3142; " +
                "-fx-border-width: 1; -fx-border-radius: 10;");

        // Header : date + score
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        Label dateLbl = new Label("📅 " + a.getDateGeneration().toLocalDateTime()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        dateLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
        HBox.setHgrow(dateLbl, Priority.ALWAYS);

        int score = a.getScore();
        String scoreColor = score >= 75 ? "#10b981" : score >= 50 ? "#f59e0b" : "#ef4444";
        Label scoreLbl = new Label(score + "/100");
        scoreLbl.setStyle("-fx-text-fill: " + scoreColor + "; -fx-font-size: 18px; -fx-font-weight: bold;");

        header.getChildren().addAll(dateLbl, scoreLbl);
        card.getChildren().add(header);

        // Période
        Label periodeLbl = new Label("Période : " + a.getPeriodeDebut() + " → " + a.getPeriodeFin());
        periodeLbl.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
        card.getChildren().add(periodeLbl);

        // Résumé depuis JSON
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            JsonNode json = mapper.readTree(a.getContenuJson());

            Label resumeLbl = new Label(json.get("resume").asText());
            resumeLbl.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 12px;");
            resumeLbl.setWrapText(true);
            card.getChildren().add(resumeLbl);

            // Bouton "Voir détails"
            Button btnDetail = new Button("👁 Voir détails");
            btnDetail.setStyle("-fx-background-color: #0f766e; -fx-text-fill: white; " +
                    "-fx-background-radius: 6; -fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 5 12;");
            btnDetail.setOnAction(e -> afficherPopupAnalyse(json));
            card.getChildren().add(btnDetail);

        } catch (Exception e) {
            System.out.println("JSON parse error: " + e.getMessage());
        }

        return card;
    }
}
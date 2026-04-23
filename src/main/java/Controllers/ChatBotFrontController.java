package Controllers;

import Services.ChatRepasParser;
import Services.ChatRepasParser.ParsedMeal;
import Services.GroqService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import Services.ServiceAliment;
import Services.ServiceRepas;
import Models.Repas;
import Models.Aliment;
import java.util.ArrayList;
import java.util.List;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatBotFrontController {

    @FXML private VBox chatbotPane;
    @FXML private ScrollPane scrollPane;
    @FXML private VBox messagesContainer;
    @FXML private TextField inputField;
    @FXML private Button sendButton;
    @FXML private Button photoButton;

    private GroqService groqService;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private AddRepasCallback addRepasCallback;

    public interface AddRepasCallback {
        void onAddRepas(ParsedMeal meal);
    }

    public void setAddRepasCallback(AddRepasCallback cb) {
        this.addRepasCallback = cb;
    }

    @FXML
    public void initialize() {
        groqService = new GroqService();
        messagesContainer.heightProperty().addListener((o, ov, nv) -> scrollPane.setVvalue(1.0));
        inputField.setOnAction(e -> handleSend());
        addBotMessage("Bonjour ! 👋 Je suis ton assistant repas.\n💬 Ex: \"propose moi un dîner léger\"\n📷 Ou envoie une photo de ton plat !", null);
    }

    // ─── Handlers ─────────────────────────────────────────────────────────────────

    @FXML
    private void handleSend() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;
        inputField.clear();
        addUserMessage(text);
        sendAsync(text, null, null);
    }

    @FXML
    private void handlePhoto() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Photo du repas");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png", "*.webp")
        );
        File file = fc.showOpenDialog(chatbotPane.getScene().getWindow());
        if (file == null) return;

        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            String b64 = Base64.getEncoder().encodeToString(bytes);
            String mime = getMime(file.getName());
            addUserImageMessage(file);
            sendAsync(null, b64, mime);
        } catch (IOException e) {
            addErrorMessage("Impossible de lire l'image : " + e.getMessage());
        }
    }

    @FXML
    private void handleClose() {
        chatbotPane.setVisible(false);
        chatbotPane.setManaged(false);
    }

    // ─── Async API call ───────────────────────────────────────────────────────────

    private void sendAsync(String text, String b64, String mime) {
        showTyping(true);
        executor.submit(() -> {
            try {
                String response = (b64 != null)
                        ? groqService.chatWithImage(b64, mime)
                        : groqService.chat(text);

                Platform.runLater(() -> {
                    showTyping(false);
                    ParsedMeal meal = null;
                    if (ChatRepasParser.containsMealProposal(response)) {
                        meal = ChatRepasParser.parse(response);
                        if (!meal.isValid()) meal = null; // fallback to plain text if parsing fails
                    }
                    addBotMessage(response, meal);
                });
            } catch (IOException e) {
                Platform.runLater(() -> {
                    showTyping(false);
                    addErrorMessage("Erreur : " + e.getMessage());
                });
            }
        });
    }

    // ─── UI: User messages ────────────────────────────────────────────────────────

    private void addUserMessage(String text) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_RIGHT);
        row.setPadding(new Insets(3, 8, 3, 50));

        Label lbl = new Label(text);
        lbl.setWrapText(true);
        lbl.setMaxWidth(260);
        lbl.setStyle(
                "-fx-background-color: #14b8a6;" +
                        "-fx-text-fill: white;" +
                        "-fx-padding: 10 14;" +
                        "-fx-background-radius: 18 18 4 18;" +
                        "-fx-font-size: 13px;"
        );
        row.getChildren().add(lbl);
        messagesContainer.getChildren().add(row);
    }

    private void addUserImageMessage(File f) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_RIGHT);
        row.setPadding(new Insets(3, 8, 3, 50));

        VBox box = new VBox(4);
        box.setAlignment(Pos.CENTER_RIGHT);
        try {
            ImageView iv = new ImageView(new Image(f.toURI().toString()));
            iv.setFitWidth(180); iv.setFitHeight(130); iv.setPreserveRatio(true);
            iv.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 8, 0, 0, 2);");
            Label nameLabel = new Label("📷 " + f.getName());
            nameLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 10px;");
            box.getChildren().addAll(iv, nameLabel);
        } catch (Exception e) {
            box.getChildren().add(new Label("📷 Image envoyée"));
        }
        row.getChildren().add(box);
        messagesContainer.getChildren().add(row);
    }

    // ─── UI: Bot messages ─────────────────────────────────────────────────────────

    private void addBotMessage(String text, ParsedMeal meal) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.TOP_LEFT);
        row.setPadding(new Insets(3, 50, 3, 8));

        Label avatar = new Label("🤖");
        avatar.setStyle("-fx-font-size: 18px;");
        avatar.setPadding(new Insets(4, 0, 0, 0));

        VBox content = new VBox();
        if (meal != null && meal.isValid()) {
            content.getChildren().add(buildMealCard(meal));
        } else {
            content.getChildren().add(buildPlainBubble(text));
        }

        row.getChildren().addAll(avatar, content);
        messagesContainer.getChildren().add(row);
    }

    private Label buildPlainBubble(String text) {
        Label lbl = new Label(stripMarkdown(text));
        lbl.setWrapText(true);
        lbl.setMaxWidth(280);
        lbl.setStyle(
                "-fx-background-color: #1e293b;" +
                        "-fx-text-fill: #cbd5e1;" +
                        "-fx-padding: 10 14;" +
                        "-fx-background-radius: 4 18 18 18;" +
                        "-fx-font-size: 13px;"
        );
        return lbl;
    }

    // ─── UI: Meal card ────────────────────────────────────────────────────────────

    private VBox buildMealCard(ParsedMeal meal) {
        VBox card = new VBox(0);
        card.setMaxWidth(310);
        card.setStyle(
                "-fx-background-color: #1e293b;" +
                        "-fx-background-radius: 16;" +
                        "-fx-border-color: #334155;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 16;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 12, 0, 0, 4);"
        );

        // ── Header ──
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-padding: 12 14 8 14;");

        Label icon = new Label("🍽️");
        icon.setStyle("-fx-font-size: 15px;");

        Label nameLbl = new Label(meal.name);
        nameLbl.setStyle("-fx-text-fill: #f1f5f9; -fx-font-size: 13px; -fx-font-weight: bold;");
        nameLbl.setWrapText(true);
        nameLbl.setMaxWidth(160);
        HBox.setHgrow(nameLbl, Priority.ALWAYS);

        Label calBadge = new Label("🔥 " + meal.totalCalories + " kcal");
        calBadge.setStyle(
                "-fx-background-color: #f97316;" +
                        "-fx-text-fill: white;" +
                        "-fx-padding: 3 8;" +
                        "-fx-background-radius: 20;" +
                        "-fx-font-size: 10px;" +
                        "-fx-font-weight: bold;"
        );

        header.getChildren().addAll(icon, nameLbl, calBadge);
        card.getChildren().add(header);

        // ── Separator ──
        Separator s1 = new Separator();
        s1.setStyle("-fx-background-color: #334155; -fx-opacity: 0.5;");
        card.getChildren().add(s1);

        // ── Ingrédients ──
        VBox ingsBox = new VBox(4);
        ingsBox.setStyle("-fx-padding: 10 14;");

        Label ingsTitle = new Label("Ingrédients");
        ingsTitle.setStyle("-fx-text-fill: #64748b; -fx-font-size: 10px; -fx-font-weight: bold;");
        ingsBox.getChildren().add(ingsTitle);

        for (ChatRepasParser.ParsedIngredient ing : meal.ingredients) {
            HBox row = new HBox(4);
            row.setAlignment(Pos.CENTER_LEFT);

            Label dot = new Label("•");
            dot.setStyle("-fx-text-fill: #14b8a6; -fx-font-size: 12px; -fx-min-width: 12;");

            Label ingName = new Label(ing.name + (ing.quantity.isEmpty() ? "" : " — " + ing.quantity));
            ingName.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 12px;");
            HBox.setHgrow(ingName, Priority.ALWAYS);

            Label calLbl = new Label("(" + ing.calories + " kcal)");
            calLbl.setStyle("-fx-text-fill: #475569; -fx-font-size: 10px;");

            row.getChildren().addAll(dot, ingName, calLbl);
            ingsBox.getChildren().add(row);
        }
        card.getChildren().add(ingsBox);

        // ── Note ──
        if (meal.note != null && !meal.note.isEmpty()) {
            HBox noteRow = new HBox(6);
            noteRow.setStyle("-fx-padding: 0 14 8 14;");
            noteRow.setAlignment(Pos.TOP_LEFT);
            Label noteIcon = new Label("💡");
            noteIcon.setStyle("-fx-font-size: 11px;");
            Label noteLbl = new Label(meal.note);
            noteLbl.setWrapText(true);
            noteLbl.setMaxWidth(240);
            noteLbl.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px; -fx-font-style: italic;");
            noteRow.getChildren().addAll(noteIcon, noteLbl);
            card.getChildren().add(noteRow);
        }

        // ── Recette (accordéon) ──
        if (!meal.steps.isEmpty()) {
            Separator s2 = new Separator();
            s2.setStyle("-fx-background-color: #334155; -fx-opacity: 0.5;");
            card.getChildren().add(s2);

            TitledPane recipePane = new TitledPane();
            recipePane.setText("📋 Recette");
            recipePane.setExpanded(false);
            recipePane.setStyle(
                    "-fx-background-color: transparent;" +
                            "-fx-text-fill: #94a3b8;" +
                            "-fx-font-size: 12px;" +
                            "-fx-border-color: transparent;"
            );
            VBox stepsBox = new VBox(5);
            stepsBox.setStyle("-fx-background-color: transparent; -fx-padding: 4 8 4 0;");
            for (int i = 0; i < meal.steps.size(); i++) {
                Label step = new Label((i + 1) + ". " + meal.steps.get(i));
                step.setWrapText(true);
                step.setMaxWidth(260);
                step.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
                stepsBox.getChildren().add(step);
            }
            recipePane.setContent(stepsBox);
            card.getChildren().add(recipePane);
        }

        // ── Bouton Ajouter ──
        Separator s3 = new Separator();
        s3.setStyle("-fx-background-color: #334155; -fx-opacity: 0.5;");
        card.getChildren().add(s3);

        Button addBtn = new Button("＋  Ajouter ce repas");
        addBtn.setMaxWidth(Double.MAX_VALUE);
        String btnBase =
                "-fx-background-color: #14b8a6;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 12px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 10 0;" +
                        "-fx-background-radius: 0 0 15 15;" +
                        "-fx-cursor: hand;";
        addBtn.setStyle(btnBase);
        addBtn.setOnMouseEntered(e -> addBtn.setStyle(btnBase.replace("#14b8a6", "#0d9488")));
        addBtn.setOnMouseExited(e -> addBtn.setStyle(btnBase));

        addBtn.setOnAction(e -> {
            try {
                // Créer le repas
                Repas repas = new Repas();
                repas.setUser_id(1); // ← mets le vrai user_id ici
                repas.setNom(meal.name);
                repas.setCalories(meal.totalCalories);
                repas.setType("diner");
                repas.setDate(new java.sql.Date(System.currentTimeMillis()));
                repas.setHeure(java.sql.Time.valueOf(java.time.LocalTime.now()));
                repas.setDescription(meal.note != null ? meal.note : "");

                // Créer/trouver les aliments
                ServiceAliment serviceAliment = new ServiceAliment();
                List<Aliment> aliments = new ArrayList<>();
                for (ChatRepasParser.ParsedIngredient ing : meal.ingredients) {
                    Aliment a = serviceAliment.findByNameOrCreate(ing.name, ing.calories, ing.quantity);
                    if (a != null) aliments.add(a);
                }

                // Sauvegarder repas + lier aliments
                ServiceRepas serviceRepas = new ServiceRepas();
                serviceRepas.addWithAliments(repas, aliments);

                if (addRepasCallback != null) addRepasCallback.onAddRepas(meal);

                addBtn.setText("✅ Repas ajouté !");
                addBtn.setDisable(true);
                addBtn.setStyle(
                        "-fx-background-color: #166534;" +
                                "-fx-text-fill: #86efac;" +
                                "-fx-font-size: 12px;" +
                                "-fx-font-weight: bold;" +
                                "-fx-padding: 10 0;" +
                                "-fx-background-radius: 0 0 15 15;"
                );
            } catch (Exception ex) {
                addErrorMessage("Erreur sauvegarde : " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        card.getChildren().add(addBtn);
        return card;
    }

    // ─── Typing indicator ─────────────────────────────────────────────────────────

    private HBox typingRow;

    private void showTyping(boolean show) {
        if (show) {
            typingRow = new HBox(8);
            typingRow.setId("typing");
            typingRow.setAlignment(Pos.CENTER_LEFT);
            typingRow.setPadding(new Insets(3, 0, 3, 8));

            Label av = new Label("🤖");
            av.setStyle("-fx-font-size: 18px;");

            Label typing = new Label("● ● ●");
            typing.setStyle(
                    "-fx-text-fill: #475569;" +
                            "-fx-background-color: #1e293b;" +
                            "-fx-padding: 10 14;" +
                            "-fx-background-radius: 4 18 18 18;" +
                            "-fx-font-size: 12px;"
            );
            typingRow.getChildren().addAll(av, typing);
            messagesContainer.getChildren().add(typingRow);

            sendButton.setDisable(true);
            inputField.setDisable(true);
        } else {
            if (typingRow != null) messagesContainer.getChildren().remove(typingRow);
            sendButton.setDisable(false);
            inputField.setDisable(false);
            inputField.requestFocus();
        }
    }

    // ─── Error ────────────────────────────────────────────────────────────────────

    private void addErrorMessage(String msg) {
        HBox row = new HBox();
        row.setPadding(new Insets(2, 8, 2, 8));
        Label lbl = new Label("❌ " + msg);
        lbl.setWrapText(true);
        lbl.setStyle("-fx-text-fill: #f87171; -fx-font-size: 11px;");
        row.getChildren().add(lbl);
        messagesContainer.getChildren().add(row);
    }

    // ─── Utils ────────────────────────────────────────────────────────────────────

    private String stripMarkdown(String s) {
        return s.replaceAll("\\*+", "").replaceAll("#{1,6}\\s?", "").trim();
    }

    private String getMime(String name) {
        String l = name.toLowerCase();
        if (l.endsWith(".png")) return "image/png";
        if (l.endsWith(".webp")) return "image/webp";
        return "image/jpeg";
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}
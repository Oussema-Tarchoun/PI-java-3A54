package Controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.io.IOException;

public class MainController {

    @FXML private StackPane contentArea;
    @FXML private Label     topbarTitle;
    @FXML private Label     topbarSubtitle;
    @FXML private Label     topbarUser;
    @FXML private Label     labelUser;
    @FXML private Button    btnDepenses;
    @FXML private Button    btnCategories;

    private Button activeButton;

    private static final String STYLE_ACTIVE =
            "-fx-background-color:rgba(0,212,255,0.1); -fx-text-fill:#00d4ff;" +
                    "-fx-font-size:14px; -fx-font-weight:bold; -fx-padding:12 16 12 16;" +
                    "-fx-alignment:CENTER_LEFT; -fx-cursor:hand; -fx-background-radius:8px;" +
                    "-fx-border-color:rgba(0,212,255,0.2); -fx-border-radius:8px; -fx-border-width:1px;";

    private static final String STYLE_INACTIVE =
            "-fx-background-color:transparent; -fx-text-fill:#94a3b8;" +
                    "-fx-font-size:14px; -fx-padding:12 16 12 16;" +
                    "-fx-alignment:CENTER_LEFT; -fx-cursor:hand; -fx-background-radius:8px;";

    @FXML
    public void initialize() {
        activeButton = btnDepenses;
        openDepenses();
    }

    @FXML
    public void openDepenses() {
        loadPage("ViewDepense.fxml", "Depenses", "Gestion des depenses", btnDepenses);
    }

    @FXML
    public void openCategories() {
        loadPage("ViewCategorie.fxml", "Categories", "Gestion des categories", btnCategories);
    }

    private void loadPage(String fxmlFile, String title, String subtitle, Button navBtn) {
        try {
            Parent page = FXMLLoader.load(getClass().getResource("/fxml/" + fxmlFile));
            contentArea.getChildren().setAll(page);
            if (topbarTitle    != null) topbarTitle.setText(title);
            if (topbarSubtitle != null) topbarSubtitle.setText(subtitle);
            setActiveButton(navBtn);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Could not load: /fxml/" + fxmlFile);
        }
    }

    private void setActiveButton(Button navBtn) {
        if (activeButton != null) activeButton.setStyle(STYLE_INACTIVE);
        if (navBtn       != null) navBtn.setStyle(STYLE_ACTIVE);
        activeButton = navBtn;
    }

    public void setUsername(String username) {
        if (topbarUser != null) topbarUser.setText("Bonjour, " + username);
        if (labelUser  != null) labelUser.setText(username);
    }
}
package Controllers;

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

    @FXML private Button btnDashboard;
    @FXML private Button btnRepas;
    @FXML private Button btnAliments;

    private Button activeButton;

    @FXML
    public void initialize() {
        activeButton = btnDashboard;
        openDashboard();
    }

    @FXML
    public void openDashboard() {
        loadPage("Dashboard.fxml", "Dashboard", "Vue d'ensemble", btnDashboard);
    }

    @FXML
    public void openRepas() {
        loadPage("RepasView.fxml", "Mes Repas", "Gérez vos repas quotidiens", btnRepas);
    }

    @FXML
    public void openAliments() {
        loadPage("AlimentView.fxml", "Mes Aliments", "Bibliothèque nutritionnelle", btnAliments);
    }

    @FXML
    public void handleLogout() {
        try {
            Parent login = FXMLLoader.load(getClass().getResource("/views/Login.fxml"));
            contentArea.getScene().setRoot(login);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setUsername(String username) {
        if (topbarUser != null) topbarUser.setText("Bonjour, " + username);
        if (labelUser  != null) labelUser.setText("● " + username);
    }

    private void loadPage(String fxmlFile, String title, String subtitle, Button navBtn) {
        try {
            Parent page = FXMLLoader.load(getClass().getResource("/views/" + fxmlFile));
            contentArea.getChildren().setAll(page);
            if (topbarTitle    != null) topbarTitle.setText(title);
            if (topbarSubtitle != null) topbarSubtitle.setText(subtitle);
            setActiveButton(navBtn);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Could not load: " + fxmlFile + " — " + e.getMessage());
        }
    }

    private void setActiveButton(Button navBtn) {
        if (activeButton != null) activeButton.getStyleClass().remove("nav-button-active");
        if (navBtn       != null) navBtn.getStyleClass().add("nav-button-active");
        activeButton = navBtn;
    }
}

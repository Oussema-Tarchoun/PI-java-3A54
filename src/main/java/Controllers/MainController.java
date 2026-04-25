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
    @FXML private Button btnEnergie;
    @FXML private Button btnRecommandations;

    private Button activeButton;

    @FXML
    public void initialize() {
        // Load initial page
        openDashboard();
        setUsername("Admin");
    }

    @FXML
    public void openDashboard() {
        loadPage("Dashboard.fxml", "Dashboard", "Vue d'ensemble de vos statistiques", btnDashboard);
    }

    @FXML
    public void openEnergie() {
        loadPage("BackEnergieView.fxml", "Mon Énergie", "Suivi de consommation et performance", btnEnergie);
    }

    @FXML
    public void openRecommandations() {
        loadPage("BackRecommandationView.fxml", "Conseils Éco", "Optimisez votre consommation intelligemment", btnRecommandations);
    }

    // Aliases for the user's specific request names if needed
    public void goToDashboard() { openDashboard(); }
    public void goToEnergie() { openEnergie(); }
    public void goToRecommandation() { openRecommandations(); }

    @FXML
    public void handleLogout() {
        try {
            Parent login = FXMLLoader.load(getClass().getResource("/view/Login.fxml"));
            contentArea.getScene().setRoot(login);
        } catch (IOException e) {
            System.err.println("Logout failed: " + e.getMessage());
        }
    }

    public void setUsername(String username) {
        if (topbarUser != null) topbarUser.setText(username);
        if (labelUser  != null) labelUser.setText("● " + username);
    }

    private void loadPage(String fxmlFile, String title, String subtitle, Button navBtn) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/" + fxmlFile));
            Parent page = loader.load();
            
            contentArea.getChildren().setAll(page);
            
            if (topbarTitle    != null) topbarTitle.setText(title);
            if (topbarSubtitle != null) topbarSubtitle.setText(subtitle);
            
            setActiveButton(navBtn);
            
        } catch (IOException e) {
            System.err.println("Could not load: " + fxmlFile);
            e.printStackTrace();
        }
    }

    private void setActiveButton(Button navBtn) {
        if (activeButton != null) {
            activeButton.getStyleClass().remove("active");
        }
        if (navBtn != null) {
            navBtn.getStyleClass().add("active");
            activeButton = navBtn;
        }
    }
}

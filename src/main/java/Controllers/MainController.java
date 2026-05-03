package Controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import utils.SessionManager;
import Models.User;

import java.io.IOException;

public class MainController {

    @FXML
    private StackPane contentArea;
    @FXML
    private Label topbarTitle;
    @FXML
    private Label topbarSubtitle;
    @FXML
    private Label topbarUser;
    @FXML
    private Label labelUser;

    @FXML
    private Button btnDashboard;
    @FXML
    private Button btnRepas;
    @FXML
    private Button btnAliments;
    @FXML
    private javafx.scene.layout.VBox adminMenu;
    @FXML
    private Button btnUsers;
    @FXML
    private Button btnGlobalReports;

    public static MainController instance;

    private Button activeButton;

    @FXML
    public void initialize() {
        instance = this;
        activeButton = btnDashboard;

        User current = SessionManager.getCurrentUser();
        if (current != null) {
            setUsername(current.getName());
            setAdminMode(current.getRoles() != null && current.getRoles().contains("ROLE_ADMIN"));
        }

        // Default view for user shell: Repas
        openRepas();
    }

    @FXML
    public void openDashboard() {
        loadPage("/Dashboard.fxml", "Tableau de Bord", "Vue d'ensemble nutritionnelle", btnDashboard);
    }

    @FXML
    public void openRepas() {
        loadPage("/view/RepasView.fxml", "Mes Repas", "Planifiez votre alimentation", btnRepas);
    }

    @FXML
    public void openAliments() {
        loadPage("/view/AlimentView.fxml", "Mes Aliments", "Votre bibliothèque nutritionnelle", btnAliments);
    }


    @FXML
    public void openUserManagement() {
        loadPage("/UserList.fxml", "Gestion Utilisateurs", "Gérez les comptes utilisateurs", btnUsers);
    }

    @FXML
    public void openGlobalReports() {
        loadPage("/GlobalReportList.fxml", "Rapports Globaux", "Vue d'ensemble des rapports", btnGlobalReports);
    }

    @FXML
    public void handleLogout() {
        Platform.exit();
    }

    public void setUsername(String username) {
        if (topbarUser != null)
            topbarUser.setText("Bonjour, " + username);
        if (labelUser != null)
            labelUser.setText("● " + username);
    }

    public void setContent(Parent page, String title, String subtitle) {
        contentArea.getChildren().setAll(page);
        if (topbarTitle != null && title != null)
            topbarTitle.setText(title);
        if (topbarSubtitle != null && subtitle != null)
            topbarSubtitle.setText(subtitle);
    }

    public void setAdminMode(boolean isAdmin) {
        if (adminMenu != null) {
            adminMenu.setVisible(isAdmin);
            adminMenu.setManaged(isAdmin);
        }
    }

    public void loadAdminDashboard() {
        loadPage("/Dashboard.fxml", "Tableau de Bord Admin", "Vue globale d'administration", btnDashboard);
    }

    private void loadPage(String fxmlFile, String title, String subtitle, Button navBtn) {
        try {
            java.net.URL resource;
            if (fxmlFile.startsWith("/")) {
                resource = getClass().getResource(fxmlFile);
            } else {
                resource = getClass().getResource("/view/" + fxmlFile);
            }

            if (resource == null) {
                System.err.println("❌ FXML introuvable : /view/" + fxmlFile);
                return;
            }

            Parent page = FXMLLoader.load(resource);
            contentArea.getChildren().setAll(page);
            if (topbarTitle != null)
                topbarTitle.setText(title);
            if (topbarSubtitle != null)
                topbarSubtitle.setText(subtitle);
            setActiveButton(navBtn);

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("❌ Erreur chargement : " + fxmlFile + " — " + e.getMessage());
        }
    }

    private void setActiveButton(Button navBtn) {
        if (activeButton != null)
            activeButton.getStyleClass().remove("nav-button-active");
        if (navBtn != null)
            navBtn.getStyleClass().add("nav-button-active");
        activeButton = navBtn;
    }
}
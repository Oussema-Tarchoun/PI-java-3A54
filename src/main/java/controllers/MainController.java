package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.io.IOException;

public class MainController {

    // ── Injected by FXML ──────────────────────────────────────────────────────
    @FXML private StackPane contentArea;
    @FXML private Label     topbarTitle;
    @FXML private Label     topbarSubtitle;
    @FXML private Label     topbarUser;
    @FXML private Label     labelUser;

    // ── Nav buttons ───────────────────────────────────────────────────────────
    @FXML private Button btnDashboard;
    @FXML private Button btnActivites;
    @FXML private Button btnObjectifs;

    private Button activeButton;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        activeButton = btnDashboard;
        openDashboard();
    }

    // ── Nav handlers ──────────────────────────────────────────────────────────

    @FXML
    public void openDashboard() {
        loadPage("Dashboard.fxml", "Dashboard", "Vue d'ensemble", btnDashboard);
    }

    @FXML
    public void openActivites() {
        loadPage("ActiviteFront.fxml", "Mes Activités", "Gérez vos séances sportives", btnActivites);
    }

    @FXML
    public void openObjectifs() {
        loadPage("ObjectifFront.fxml", "Mes Objectifs", "Suivez votre progression", btnObjectifs);
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    @FXML
    public void handleLogout() {
        try {
            Parent login = FXMLLoader.load(getClass().getResource("/views/Login.fxml"));
            contentArea.getScene().setRoot(login);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ── Called from login controller ──────────────────────────────────────────

    public void setUsername(String username) {
        topbarUser.setText("Bonjour, " + username);
        labelUser.setText("● " + username);
    }

    // ── Core loader ───────────────────────────────────────────────────────────

    private void loadPage(String fxmlFile, String title, String subtitle, Button navBtn) {
        java.net.URL resource = getClass().getResource("/views/" + fxmlFile);
        if (resource == null) {
            System.err.println("[MainController] FXML not found: /views/" + fxmlFile);
            // Show a friendly placeholder instead of crashing
            javafx.scene.control.Label placeholder = new javafx.scene.control.Label(
                    "Page \"" + fxmlFile + "\" introuvable.\nVérifiez que le fichier est dans src/main/resources/views/");
            placeholder.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 14px; -fx-alignment: center;");
            contentArea.getChildren().setAll(placeholder);
            topbarTitle.setText(title);
            topbarSubtitle.setText(subtitle);
            setActiveButton(navBtn);
            return;
        }
        try {
            Parent page = FXMLLoader.load(resource);
            contentArea.getChildren().setAll(page);
            topbarTitle.setText(title);
            topbarSubtitle.setText(subtitle);
            setActiveButton(navBtn);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Could not load: " + fxmlFile);
        }
    }

    private void setActiveButton(Button navBtn) {
        if (activeButton != null) activeButton.getStyleClass().remove("nav-button-active");
        if (navBtn        != null) navBtn.getStyleClass().add("nav-button-active");
        activeButton = navBtn;
    }
}
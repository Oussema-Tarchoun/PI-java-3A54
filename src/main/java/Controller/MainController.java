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
    @FXML private Label topbarTitle;
    @FXML private Label topbarSubtitle;
    @FXML private Label topbarUser;
    @FXML private Label labelUser;

    @FXML private Button btnDashboard;
    @FXML private Button btnDepense;
    @FXML private Button btnApprentissage;
    @FXML private Button btnChapitre;
    @FXML private Button btnAlimentation;
    @FXML private Button btnActivite;
    @FXML private Button btnEnergie;      // will now load ViewCategorie
    @FXML private Button btnObjectif;
    @FXML private Button btnRecommandation;

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
        activeButton = btnDashboard;
        openDashboard();
    }

    @FXML public void openDashboard()      { loadPage("/views/Dashboard.fxml",      "Dashboard",         "Vue d'ensemble",        btnDashboard); }
    @FXML public void openDepense()        { loadPage("/fxml/ViewDepense.fxml",     "Dépenses",          "Suivez vos finances",   btnDepense); }
    @FXML public void openApprentissage()  { loadPage("/views/apprentissage.fxml",  "Apprentissage",     "Cours",                 btnApprentissage); }
    @FXML public void openAlimentation()   { loadPage("/views/Alimentation.fxml",   "Alimentation",      "Gérez vos repas",       btnAlimentation); }
    @FXML public void openActivite()       { loadPage("/views/Activite.fxml",       "Activité physique", "Gérez vos activités",   btnActivite); }
    @FXML public void openEnergie()        { loadPage("/fxml/ViewCategorie.fxml",   "Catégories",        "Gestion des catégories",btnEnergie); }
    @FXML public void openObjectif()       { loadPage("/views/Objectif.fxml",       "Objectif",          "Suivez vos objectifs",  btnObjectif); }
    @FXML public void openRecommandation() { loadPage("/views/Recommandation.fxml", "Recommandation",    "Vos recommandations",   btnRecommandation); }

    @FXML
    public void openChapitre() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Chapitre.fxml"));
            if (loader.getLocation() == null) {
                showError("Chapitre.fxml not found in resources/views/");
                return;
            }
            Parent page = loader.load();
            ChapitreController controller = loader.getController();
            if (controller == null) {
                showError("Could not initialize ChapitreController");
                return;
            }
            controller.initWithoutCours();
            contentArea.getChildren().setAll(page);
            if (topbarTitle    != null) topbarTitle.setText("Chapitres");
            if (topbarSubtitle != null) topbarSubtitle.setText("Parcourez les chapitres par cours");
            setActiveButton(btnChapitre);
        } catch (IOException e) {
            e.printStackTrace();
            showError("Could not load Chapitre page: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            showError("Unexpected error: " + e.getMessage());
        }
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

    private void loadPage(String fxmlPath, String title, String subtitle, Button navBtn) {
        try {
            Parent page = FXMLLoader.load(getClass().getResource(fxmlPath));
            contentArea.getChildren().setAll(page);
            if (topbarTitle    != null) topbarTitle.setText(title);
            if (topbarSubtitle != null) topbarSubtitle.setText(subtitle);
            setActiveButton(navBtn);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Could not load: " + fxmlPath);
        }
    }

    private void setActiveButton(Button navBtn) {
        if (activeButton != null) activeButton.setStyle(STYLE_INACTIVE);
        if (navBtn       != null) navBtn.setStyle(STYLE_ACTIVE);
        activeButton = navBtn;
    }

    private void showError(String message) {
        System.err.println("ERROR: " + message);
    }

    public void setUsername(String username) {
        if (topbarUser != null) topbarUser.setText("Bonjour, " + username);
        if (labelUser  != null) labelUser.setText("● " + username);
    }
}
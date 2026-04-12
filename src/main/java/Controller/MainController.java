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
    @FXML private Button btnActivite;
    @FXML private Button btnObjectif;
    @FXML private Button btnAlimentation;
    @FXML private Button btnDepense;
    @FXML private Button btnEnergie;
    @FXML private Button btnApprentissage;
    @FXML private Button btnChapitre;
    @FXML private Button btnRecommandation;

    private Button activeButton;

    @FXML
    public void initialize() {
        activeButton = btnDashboard;
        openDashboard();
    }

    @FXML public void openDashboard()      { loadPage("Dashboard.fxml",      "Dashboard",        "Vue d'ensemble",       btnDashboard); }
    @FXML public void openActivite()       { loadPage("Activite.fxml",       "Activité physique","Gérez vos activités",  btnActivite); }
    @FXML public void openObjectif()       { loadPage("Objectif.fxml",       "Objectif",         "Suivez vos objectifs", btnObjectif); }
    @FXML public void openAlimentation()   { loadPage("Alimentation.fxml",   "Alimentation",     "Gérez vos repas",      btnAlimentation); }
    @FXML public void openDepense()        { loadPage("Depense.fxml",        "Dépense",          "Suivez vos finances",  btnDepense); }
    @FXML public void openEnergie()        { loadPage("Energie.fxml",        "Énergie",          "Gérez votre énergie",  btnEnergie); }
    @FXML public void openApprentissage()  { loadPage("Apprentissage.fxml",  "Apprentissage",    "Cours",                btnApprentissage); }
    @FXML public void openRecommandation() { loadPage("Recommandation.fxml", "Recommandation",   "Vos recommandations",  btnRecommandation); }

    @FXML
    public void openChapitre() {
        // Chapitre needs special loading so we can call initWithoutCours() on its controller
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Chapitre.fxml"));
            Parent page = loader.load();
            ChapitreController controller = loader.getController();
            controller.initWithoutCours(); // loads course selector instead of waiting for setCours()

            contentArea.getChildren().setAll(page);
            topbarTitle.setText("Chapitres");
            topbarSubtitle.setText("Parcourez les chapitres par cours");
            setActiveButton(btnChapitre);

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Could not load: Chapitre.fxml");
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

    private void loadPage(String fxmlFile, String title, String subtitle, Button navBtn) {
        try {
            Parent page = FXMLLoader.load(getClass().getResource("/views/" + fxmlFile));
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
        if (navBtn != null)       navBtn.getStyleClass().add("nav-button-active");
        activeButton = navBtn;
    }

    public void setUsername(String username) {
        topbarUser.setText("Bonjour, " + username);
        labelUser.setText("● " + username);
    }
}
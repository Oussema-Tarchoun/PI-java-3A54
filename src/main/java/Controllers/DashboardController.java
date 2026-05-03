package Controllers;

import Services.ServiceRepas;
import Services.ServiceAliment;
import Models.User;
import Models.Repas;
import Models.Aliment;
import utils.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.event.ActionEvent;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class DashboardController {

    @FXML
    private Label lblTotalRepas;
    @FXML
    private Label lblAvgCalories;
    @FXML
    private Label lblTotalAliments;
    @FXML
    private Label lblMaxCalAliment;

    private final ServiceRepas serviceRepas = new ServiceRepas();
    private final ServiceAliment serviceAliment = new ServiceAliment();

    @FXML
    public void initialize() {
        loadStats();
    }

    private void loadStats() {
        User current = SessionManager.getCurrentUser();
        if (current == null) return;

        try {
            // 1. Repas Stats
            List<Repas> meals = serviceRepas.recupererParUser(current.getId());
            int totalMeals = meals.size();
            double avgCal = meals.stream().mapToInt(Repas::getCalories).average().orElse(0);

            lblTotalRepas.setText(String.valueOf(totalMeals));
            lblAvgCalories.setText((int) avgCal + " kcal");

            // 2. Aliments Stats
            List<Aliment> aliments = serviceAliment.recupererParUser(current.getId());
            int totalAliments = aliments.size();
            Aliment maxCal = aliments.stream()
                    .max((a, b) -> Double.compare(a.getCalories(), b.getCalories()))
                    .orElse(null);

            lblTotalAliments.setText(String.valueOf(totalAliments));
            lblMaxCalAliment.setText(maxCal != null ? maxCal.getNom() : "—");

        } catch (SQLException e) {
            lblTotalRepas.setText("!");
            lblAvgCalories.setText("!");
            lblTotalAliments.setText("!");
            lblMaxCalAliment.setText("!");
            e.printStackTrace();
        }
    }

    @FXML
    void openRepas() {
        if (MainController.instance != null) {
            MainController.instance.openRepas();
        }
    }

    @FXML
    void openAliments() {
        if (MainController.instance != null) {
            MainController.instance.openAliments();
        }
    }

    @FXML
    private void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) lblTotalRepas.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("AIVA — Connexion");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void switchScene(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            if (MainController.instance != null) {
                MainController.instance.setContent(root, title, "");
            } else {
                Stage stage = (Stage) lblTotalRepas.getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.setTitle(title);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

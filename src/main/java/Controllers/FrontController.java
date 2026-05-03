package Controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;

public class FrontController {

    @FXML private Label lblWelcome;

    @FXML
    public void initialize() {
        lblWelcome.setText("Bienvenue sur l'Espace Utilisateur");
    }

    @FXML
    private void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) lblWelcome.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("AIVA — Connexion");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

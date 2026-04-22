package Controllers;

import Models.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import utils.TotpUtils;

import java.io.IOException;

public class TwoFactorVerifyController {

    @FXML private TextField tfCode;
    @FXML private Label lblError;

    private User user;

    public void setUser(User user) {
        this.user = user;
    }

    @FXML
    private void handleVerify() {
        String code = tfCode.getText().trim();
        if (code.isEmpty()) {
            showError("Veuillez entrer le code.");
            return;
        }

        if (TotpUtils.verifyCode(user.getTotpSecret(), code)) {
            navigateTo("/Dashboard.fxml", "AIVA — Tableau de Bord");
        } else {
            showError("Code incorrect. Veuillez réessayer.");
        }
    }

    @FXML
    private void handleBackToLogin() {
        navigateTo("/Login.fxml", "AIVA — Connexion");
    }

    private void navigateTo(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) tfCode.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle(title);
        } catch (IOException e) {
            showError("Erreur de navigation : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        lblError.setText(message);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }
}

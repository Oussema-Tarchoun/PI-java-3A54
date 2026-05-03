package Controllers;

import Models.User;
import Services.ServiceUser;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import utils.PasswordUtils;
import utils.ValidationUtils;
import utils.SessionManager;

import java.io.IOException;
import java.sql.SQLException;

public class LoginController {

    @FXML
    private TextField tfEmail;
    @FXML
    private PasswordField pfPassword;
    @FXML
    private Label lblError;
    @FXML
    private Label errEmail;
    @FXML
    private Label errPassword;

    private final ServiceUser serviceUser = new ServiceUser();

    @FXML
    public void initialize() {
        lblError.setVisible(false);
        lblError.setManaged(false);
        hideIndividualErrors();
    }

    private void hideIndividualErrors() {
        if (errEmail != null) {
            errEmail.setVisible(false);
            errEmail.setManaged(false);
        }
        if (errPassword != null) {
            errPassword.setVisible(false);
            errPassword.setManaged(false);
        }
    }

    @FXML
    private void handleLogin() {
        String email = tfEmail.getText().trim();
        String password = pfPassword.getText();

        hideIndividualErrors();
        lblError.setVisible(false);

        if (!ValidationUtils.isNotEmpty(email)) {
            showFieldError(errEmail, "Email obligatoire.");
            return;
        }
        if (!ValidationUtils.isValidEmail(email)) {
            showFieldError(errEmail, "Email invalide.");
            return;
        }
        if (!ValidationUtils.isNotEmpty(password)) {
            showFieldError(errPassword, "Mot de passe obligatoire.");
            return;
        }

        try {
            String hashedPassword = PasswordUtils.hashPassword(password);
            User user = serviceUser.login(email, hashedPassword);
            if (user != null) {
                if (user.getIsBlocked() == 1) {
                    showError("Votre compte est bloqué. Contactez l'administrateur.");
                    return;
                }

                if (!user.getIsVerified()) {
                    showError("Votre compte n'est pas vérifié. Vérifiez votre email.");
                    goToVerification(user.getEmail());
                    return;
                }

                if (user.getIs2faEnabled()) {
                    goTo2faVerification(user);
                    return;
                }

                SessionManager.setCurrentUser(user);

                if (user.getRoles() != null && user.getRoles().contains("ROLE_ADMIN")) {
                    // Admin → interface verte ListRepas avec sidebar admin intégrée
                    navigateTo("/view/repas/ListRepas.fxml", "AIVA — Administration");
                } else {
                    // Utilisateur normal → shell Main avec Dashboard/Repas/Aliments
                    navigateTo("/view/Main.fxml", "AIVA — Espace Utilisateur");
                }
            } else {
                showError("Email ou mot de passe incorrect.");
            }
        } catch (SQLException e) {
            showError("Erreur de connexion à la base de données.");
            e.printStackTrace();
        }
    }

    private void goTo2faVerification(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/TwoFactorVerify.fxml"));
            Parent root = loader.load();

            TwoFactorVerifyController controller = loader.getController();
            controller.setUser(user);

            Stage stage = (Stage) tfEmail.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("AIVA — Vérification 2FA");
        } catch (IOException e) {
            showError("Erreur de navigation : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void goToVerification(String email) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/VerifyAccount.fxml"));
            Parent root = loader.load();

            VerifyAccountController controller = loader.getController();
            controller.setUserEmail(email);

            Stage stage = (Stage) tfEmail.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("AIVA — Vérification du compte");
        } catch (IOException e) {
            showError("Erreur de navigation : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void goToRegister() {
        navigateTo("/Register.fxml", "AIVA — Créer un compte");
    }

    @FXML
    private void handleForgotPassword() {
        navigateTo("/ResetPassword.fxml", "AIVA — Réinitialiser le mot de passe");
    }

    private void navigateTo(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) tfEmail.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle(title);
        } catch (IOException e) {
            showError("Erreur de navigation : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        lblError.setText("⚠ " + message);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    private void showFieldError(Label label, String message) {
        if (label != null) {
            label.setText("⚠ " + message);
            label.setVisible(true);
            label.setManaged(true);
        }
    }
}

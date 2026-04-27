package Controllers;

import Models.User;
import Services.ServiceResetPasswordRequest;
import Services.ServiceUser;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import utils.ValidationUtils;

import java.io.IOException;
import java.sql.SQLException;

public class ResetPasswordController {

    @FXML private TextField tfEmail;
    @FXML private VBox step1Email;
    
    @FXML private TextField tfToken;
    @FXML private PasswordField pfNewPassword;
    @FXML private PasswordField pfConfirmPassword;
    @FXML private VBox step2Reset;
    
    @FXML private Label lblError;
    @FXML private Label lblSuccess;
    @FXML private Label lblSubtitle;
    @FXML private Label errEmail;
    @FXML private Label errToken;
    @FXML private Label errNewPassword;
    @FXML private Label errConfirmPassword;

    private int userId;
    private String userEmail;
    private final ServiceResetPasswordRequest serviceReset = new ServiceResetPasswordRequest();
    private final ServiceUser serviceUser = new ServiceUser();

    @FXML
    public void initialize() {
        lblError.setVisible(false);
        lblError.setManaged(false);
        lblSuccess.setVisible(false);
        lblSuccess.setManaged(false);
        
        hideIndividualErrors();

        step1Email.setVisible(true);
        step1Email.setManaged(true);
        step2Reset.setVisible(false);
        step2Reset.setManaged(false);
    }

    private void hideIndividualErrors() {
        Label[] labels = {errEmail, errToken, errNewPassword, errConfirmPassword};
        for (Label l : labels) {
            if (l != null) {
                l.setVisible(false);
                l.setManaged(false);
            }
        }
    }


    @FXML
    private void handleSendCode() {
        String email = tfEmail.getText().trim();
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

        showSuccess("Envoi en cours...");

        new Thread(() -> {
            try {
                User user = serviceUser.findUserByEmail(email);
                if (user == null) {
                    Platform.runLater(() -> showError("Aucun compte n'est associé à cette adresse email."));
                    return;
                }

                this.userId = user.getId();
                this.userEmail = email;
                String code = serviceReset.createRequest(userId);
                serviceReset.sendEmail(email, code);

                Platform.runLater(() -> {
                    showSuccess("Un code a été envoyé !");
                    switchToStep2();
                });

            } catch (SQLException e) {
                Platform.runLater(() -> showError("Erreur de base de données."));
                e.printStackTrace();
            } catch (Exception e) {
                Platform.runLater(() -> showError("Erreur lors de l'envoi : " + e.getMessage()));
                e.printStackTrace();
            }
        }).start();
    }

    private void switchToStep2() {
        step1Email.setVisible(false);
        step1Email.setManaged(false);
        step2Reset.setVisible(true);
        step2Reset.setManaged(true);
        lblSubtitle.setText("Code envoyé à " + userEmail + ". Veuillez définir un nouveau mot de passe.");
    }


    @FXML
    private void handleReset() {
        String code = tfToken.getText().trim();
        String newPassword = pfNewPassword.getText();
        String confirmPassword = pfConfirmPassword.getText();

        hideIndividualErrors();
        lblError.setVisible(false);

        if (!ValidationUtils.isNotEmpty(code)) { showFieldError(errToken, "Code obligatoire."); return; }
        if (!ValidationUtils.isNotEmpty(newPassword)) { showFieldError(errNewPassword, "Mot de passe obligatoire."); return; }
        if (!ValidationUtils.isValidPassword(newPassword)) { showFieldError(errNewPassword, "6 caractères min."); return; }
        if (!newPassword.equals(confirmPassword)) { showFieldError(errConfirmPassword, "Mots de passe différents."); return; }

        try {
            if (serviceReset.validateCode(userId, code)) {
                serviceReset.resetPassword(userId, newPassword);
                showSuccess("Mot de passe mis à jour avec succès !");
                
                new Thread(() -> {
                    try {
                        Thread.sleep(2000);
                        Platform.runLater(this::goToLogin);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
            } else {
                showError("Code invalide ou expiré.");
            }
        } catch (SQLException e) {
            showError("Erreur de base de données.");
            e.printStackTrace();
        }
    }

    @FXML
    private void goToLogin() {
        navigateTo("/Login.fxml", "AIVA — Connexion");
    }

    private void navigateTo(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) tfEmail.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle(title);
        } catch (IOException e) {
            showError("Erreur de navigation.");
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        lblError.setText(message);
        lblError.setVisible(true);
        lblError.setManaged(true);
        lblSuccess.setVisible(false);
        lblSuccess.setManaged(false);
    }

    private void showSuccess(String message) {
        lblSuccess.setText(message);
        lblSuccess.setVisible(true);
        lblSuccess.setManaged(true);
        lblError.setVisible(false);
        lblError.setManaged(false);
    }

    private void showFieldError(Label label, String message) {
        if (label != null) {
            label.setText("⚠ " + message);
            label.setVisible(true);
            label.setManaged(true);
        }
    }
}

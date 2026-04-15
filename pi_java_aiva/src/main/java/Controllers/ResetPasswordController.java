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
        
        step1Email.setVisible(true);
        step1Email.setManaged(true);
        step2Reset.setVisible(false);
        step2Reset.setManaged(false);
    }

    // --- STEP 1: SEND CODE ---
    @FXML
    private void handleSendCode() {
        String email = tfEmail.getText().trim();

        if (email.isEmpty()) {
            showError("Veuillez entrer votre adresse email.");
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

    // --- STEP 2: RESET PASSWORD ---
    @FXML
    private void handleReset() {
        String code = tfToken.getText().trim();
        String newPassword = pfNewPassword.getText();
        String confirmPassword = pfConfirmPassword.getText();

        if (code.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            showError("Veuillez remplir tous les champs.");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            showError("Les mots de passe ne correspondent pas.");
            return;
        }

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
}

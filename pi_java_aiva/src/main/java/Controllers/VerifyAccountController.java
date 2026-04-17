package Controllers;

import Models.User;
import Services.ServiceUser;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import utils.MailUtils;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Random;

public class VerifyAccountController {

    @FXML private TextField tfVerificationCode;
    @FXML private Label lblError;
    @FXML private Label lblInstruction;

    private final ServiceUser serviceUser = new ServiceUser();
    private String userEmail;

    public void setUserEmail(String email) {
        this.userEmail = email;
        lblInstruction.setText("Un code de vérification a été envoyé à : " + email);
    }

    @FXML
    public void initialize() {
        lblError.setVisible(false);
        lblError.setManaged(false);
    }

    @FXML
    private void handleVerify() {
        String code = tfVerificationCode.getText().trim();

        if (code.isEmpty()) {
            showError("Veuillez saisir le code de vérification.");
            return;
        }

        try {
            boolean success = serviceUser.verifyAccount(userEmail, code);
            if (success) {
                showSuccess("Compte vérifié avec succès ! Vous pouvez maintenant vous connecter.");
                // Wait a bit or redirect immediately
                goToLogin();
            } else {
                showError("Code invalide ou expiré.");
            }
        } catch (SQLException e) {
            showError("Erreur lors de la vérification : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleResendCode() {
        showSuccess("Envoi d'un nouveau code...");
        new Thread(() -> {
            try {
                User user = serviceUser.findUserByEmail(userEmail);
                if (user != null) {
                    String code = String.format("%06d", new Random().nextInt(999999));
                    serviceUser.updateVerificationToken(user.getId(), code, LocalDateTime.now().plusHours(24));
                    
                    String subject = "Votre nouveau code de vérification - AIVA";
                    String content = "Bonjour " + user.getName() + ",\n\n"
                            + "Voici votre nouveau code de vérification : " + code + "\n\n"
                            + "Ce code expirera dans 24 heures.\n\n"
                            + "L'équipe AIVA.";
                    
                    MailUtils.sendEmail(userEmail, subject, content);
                    
                    javafx.application.Platform.runLater(() -> {
                        showSuccess("Un nouveau code a été envoyé à " + userEmail);
                    });
                } else {
                    javafx.application.Platform.runLater(() -> {
                        showError("Utilisateur introuvable.");
                    });
                }
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    showError("Erreur lors de l'envoi du code : " + e.getMessage());
                });
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    private void goToLogin() {
        navigateTo("/Login.fxml", "AIVA — Connexion");
    }

    private void navigateTo(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) tfVerificationCode.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle(title);
        } catch (IOException e) {
            showError("Erreur de navigation : " + e.getMessage());
        }
    }

    private void showError(String message) {
        lblError.setText("⚠️ " + message);
        lblError.setStyle("-fx-text-fill: #f87171;");
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    private void showSuccess(String message) {
        lblError.setText("✅ " + message);
        lblError.setStyle("-fx-text-fill: #22c55e;");
        lblError.setVisible(true);
        lblError.setManaged(true);
    }
}

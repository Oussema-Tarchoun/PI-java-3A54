package Controllers;

import Models.User;
import Services.ServiceUser;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import utils.PasswordUtils;

import java.io.IOException;
import java.sql.SQLException;

public class RegisterController {

    @FXML private TextField tfName;
    @FXML private TextField tfEmail;
    @FXML private ComboBox<String> cbRole;
    @FXML private PasswordField pfPassword;
    @FXML private PasswordField pfConfirm;
    @FXML private Label lblError;

    private final ServiceUser serviceUser = new ServiceUser();

    @FXML
    public void initialize() {
        lblError.setVisible(false);
        lblError.setManaged(false);
        
        cbRole.getItems().addAll("Utilisateur", "Administrateur");
        cbRole.setValue("Utilisateur");
    }

    @FXML
    private void handleRegister() {
        String name = tfName.getText().trim();
        String email = tfEmail.getText().trim();
        String roleSelection = cbRole.getValue();
        String password = pfPassword.getText();
        String confirm = pfConfirm.getText();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirm.isEmpty() || roleSelection == null) {
            showError("Veuillez remplir tous les champs.");
            return;
        }

        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            showError("Format d'email invalide.");
            return;
        }

        if (!password.equals(confirm)) {
            showError("Les mots de passe ne correspondent pas.");
            return;
        }

        if (password.length() < 4) {
            showError("Le mot de passe doit contenir au moins 4 caractères.");
            return;
        }

        try {
            if (serviceUser.emailExists(email)) {
                showError("Cet email est déjà utilisé.");
                return;
            }

            User newUser = new User();
            newUser.setName(name);
            newUser.setEmail(email);
            
            String hashedPassword = PasswordUtils.hashPassword(password);
            newUser.setPassword(hashedPassword);
            
            String roleJson = roleSelection.equals("Administrateur") ? "[\"ROLE_ADMIN\"]" : "[\"ROLE_USER\"]";
            newUser.setRoles(roleJson);
            
            newUser.setIsBlocked(0);
            newUser.setIsVerified(false);
            newUser.setIs2faEnabled(false);
            newUser.setExperiencePoints(0);
            newUser.setResetPasswordAttempts(0);

            newUser.setKnownIps("[\"127.0.0.1\"]");
            newUser.setTotpSecret("");
            newUser.setVerificationToken("");

            serviceUser.ajouter(newUser);

            showSuccess("Compte créé avec succès ! Connectez-vous.");
            goToLogin();

        } catch (Exception e) {
            showError("Erreur lors de l'inscription : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void goToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) tfName.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("AIVA — Connexion");
        } catch (Exception e) {
            showError("Erreur de navigation : " + e.getMessage());
            e.printStackTrace();
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

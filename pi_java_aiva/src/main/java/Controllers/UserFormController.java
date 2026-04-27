package Controllers;

import Models.User;
import Services.ServiceUser;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.SQLException;
import utils.PasswordUtils;
import utils.ValidationUtils;

public class UserFormController {

    @FXML private Label         lblFormTitle;
    @FXML private Label         lblFormIcon;
    @FXML private TextField     tfName;
    @FXML private TextField     tfEmail;
    @FXML private PasswordField pfPassword;
    @FXML private ComboBox<String> cmbRoles;
    @FXML private ComboBox<String> cmbBlocked;
    @FXML private ComboBox<String> cmbVerified;
    @FXML private ComboBox<String> cmb2FA;
    @FXML private TextField     tfXP;
    @FXML private Label         lblError;
    @FXML private Label         errName;
    @FXML private Label         errEmail;
    @FXML private Label         errPassword;
    @FXML private Label         errXP;
    @FXML private Button        btnSave;

    private User userToEdit;
    private Runnable onSaved;
    private final ServiceUser serviceUser = new ServiceUser();

    @FXML
    public void initialize() {
        cmbRoles.getSelectionModel().selectFirst();
        cmbBlocked.getSelectionModel().selectFirst();
        cmbVerified.getSelectionModel().selectFirst();
        cmb2FA.getSelectionModel().selectFirst();

        lblError.setText("");
        lblError.setVisible(false);
        lblError.setManaged(false);
        
        hideIndividualErrors();

        // Apply numeric filter to XP field
        ValidationUtils.applyNumericFilter(tfXP);
    }

    private void hideIndividualErrors() {
        Label[] errLabels = {errName, errEmail, errPassword, errXP};
        for (Label l : errLabels) {
            if (l != null) {
                l.setVisible(false);
                l.setManaged(false);
            }
        }
    }

    public void setUserToEdit(User user) {
        this.userToEdit = user;

        if (user == null) {
            lblFormTitle.setText("Ajouter un Utilisateur");
            lblFormIcon.setText("➕");
            btnSave.setText("Créer");
            tfXP.setText("0");
            tfXP.setEditable(false);
            tfXP.setStyle("-fx-opacity: 0.6;");
        } else {
            lblFormTitle.setText("Modifier l'Utilisateur");
            lblFormIcon.setText("✏️");
            btnSave.setText("Mettre à jour");
            pfPassword.setPromptText("Laisser vide pour conserver");

            tfName.setText(user.getName() != null  ? user.getName()  : "");
            tfEmail.setText(user.getEmail() != null ? user.getEmail() : "");
            tfXP.setText(String.valueOf(user.getExperiencePoints()));

            if (user.getRoles() != null) {
                if (user.getRoles().contains("ROLE_ADMIN"))
                    cmbRoles.getSelectionModel().select(1);
                else if (user.getRoles().contains("ROLE_MODERATOR"))
                    cmbRoles.getSelectionModel().select(2);
                else
                    cmbRoles.getSelectionModel().select(0);
            }

            cmbBlocked.getSelectionModel().select(user.getIsBlocked() == 1 ? 1 : 0);
            cmbVerified.getSelectionModel().select(user.getIsVerified() ? 1 : 0);
            cmb2FA.getSelectionModel().select(user.getIs2faEnabled() ? 1 : 0);
        }
    }

    public void setOnSaved(Runnable callback) {
        this.onSaved = callback;
    }

    @FXML
    private void save() {
        hideIndividualErrors();
        
        String name     = tfName.getText().trim();
        String email    = tfEmail.getText().trim();
        String password = pfPassword.getText();

        if (!ValidationUtils.isNotEmpty(name)) { showFieldError(errName, "Nom obligatoire."); return; }
        if (name.length() < 3) { showFieldError(errName, "3 caractères min."); return; }
        if (!ValidationUtils.isValidEmail(email)) { showFieldError(errEmail, "Email invalide."); return; }
        
        if (userToEdit == null) {
            if (!ValidationUtils.isNotEmpty(password)) {
                showFieldError(errPassword, "Mot de passe obligatoire."); return;
            }
            if (!ValidationUtils.isValidPassword(password)) {
                showFieldError(errPassword, "6 caractères min."); return;
            }
        } else if (ValidationUtils.isNotEmpty(password) && !ValidationUtils.isValidPassword(password)) {
            showFieldError(errPassword, "6 caractères min."); return;
        }

        int xp = 0;
        String xpText = tfXP.getText().trim();
        if (ValidationUtils.isNotEmpty(xpText)) {
            xp = Integer.parseInt(xpText);
        }

        int roleIdx = cmbRoles.getSelectionModel().getSelectedIndex();
        String roles = roleIdx == 1 ? "[\"ROLE_ADMIN\"]" :
                       roleIdx == 2 ? "[\"ROLE_MODERATOR\"]" :
                                      "[\"ROLE_USER\"]";

        int     blocked  = cmbBlocked.getSelectionModel().getSelectedIndex();
        boolean verified = cmbVerified.getSelectionModel().getSelectedIndex() == 1;
        boolean twoFA    = cmb2FA.getSelectionModel().getSelectedIndex() == 1;

        try {
            if (userToEdit == null) {
                User newUser = new User();
                newUser.setName(name);
                newUser.setEmail(email);
                newUser.setPassword(PasswordUtils.hashPassword(password));
                newUser.setRoles(roles);
                newUser.setIsBlocked(blocked);
                newUser.setIsVerified(verified);
                newUser.setIs2faEnabled(twoFA);
                newUser.setExperiencePoints(0);
                newUser.setResetPasswordAttempts(0);
                newUser.setKnownIps("[\"127.0.0.1\"]");
                newUser.setTotpSecret("");
                newUser.setVerificationToken("");

                serviceUser.ajouter(newUser);
                showSuccess("Utilisateur créé avec succès !");
            } else {
                userToEdit.setName(name);
                userToEdit.setEmail(email);
                if (!password.isEmpty()) {
                    userToEdit.setPassword(PasswordUtils.hashPassword(password));
                }
                userToEdit.setRoles(roles);
                userToEdit.setIsBlocked(blocked);
                userToEdit.setIsVerified(verified);
                userToEdit.setIs2faEnabled(twoFA);
                userToEdit.setExperiencePoints(xp);

                serviceUser.modifier(userToEdit);
                showSuccess("Utilisateur mis à jour !");
            }

            if (onSaved != null) onSaved.run();
            closeDialog();

        } catch (SQLException e) {
            showError("Erreur SQL : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void cancel() {
        closeDialog();
    }

    private void closeDialog() {
        ((Stage) tfName.getScene().getWindow()).close();
    }

    private void showError(String message) {
        lblError.setText("⚠ " + message);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    private void showSuccess(String message) {
        lblError.setStyle("-fx-text-fill: #22c55e;");
        lblError.setText("✔ " + message);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    private void clearError() {
        lblError.setText("");
        lblError.setVisible(false);
        lblError.setManaged(false);
        lblError.setStyle("-fx-text-fill: #f87171;");
        hideIndividualErrors();
    }

    private void showFieldError(Label label, String message) {
        if (label != null) {
            label.setText("⚠ " + message);
            label.setVisible(true);
            label.setManaged(true);
        }
    }
}

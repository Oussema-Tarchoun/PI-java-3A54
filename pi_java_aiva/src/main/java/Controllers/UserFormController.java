package Controllers;

import Models.User;
import Services.ServiceUser;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.SQLException;
import utils.PasswordUtils;

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
    @FXML private Button        btnSave;

    private User userToEdit;          // null = Add mode, non-null = Edit mode
    private Runnable onSaved;         // callback to refresh the table
    private final ServiceUser serviceUser = new ServiceUser();

    // -------------------------------------------------------
    // Initialization
    // -------------------------------------------------------
    @FXML
    public void initialize() {
        // Default combo selections
        cmbRoles.getSelectionModel().selectFirst();
        cmbBlocked.getSelectionModel().selectFirst();
        cmbVerified.getSelectionModel().selectFirst();
        cmb2FA.getSelectionModel().selectFirst();

        // Hide error label initially
        lblError.setText("");
        lblError.setVisible(false);
        lblError.setManaged(false);
    }

    /** Call AFTER loading the FXML to configure Add vs Edit mode. */
    public void setUserToEdit(User user) {
        this.userToEdit = user;

        if (user == null) {
            // ---- ADD MODE ----
            lblFormTitle.setText("Ajouter un Utilisateur");
            lblFormIcon.setText("➕");
            btnSave.setText("Créer");
            // XP always starts at 0 for new users
            tfXP.setText("0");
            tfXP.setEditable(false);
            tfXP.setStyle("-fx-opacity: 0.6;");
        } else {
            // ---- EDIT MODE ----
            lblFormTitle.setText("Modifier l'Utilisateur");
            lblFormIcon.setText("✏️");
            btnSave.setText("Mettre à jour");
            pfPassword.setPromptText("Laisser vide pour conserver");

            // Pre-fill fields
            tfName.setText(user.getName() != null  ? user.getName()  : "");
            tfEmail.setText(user.getEmail() != null ? user.getEmail() : "");
            tfXP.setText(String.valueOf(user.getExperiencePoints()));

            // Roles
            if (user.getRoles() != null) {
                if (user.getRoles().contains("ROLE_ADMIN"))
                    cmbRoles.getSelectionModel().select(1);
                else if (user.getRoles().contains("ROLE_MODERATOR"))
                    cmbRoles.getSelectionModel().select(2);
                else
                    cmbRoles.getSelectionModel().select(0);
            }

            // Blocked
            cmbBlocked.getSelectionModel().select(user.getIsBlocked() == 1 ? 1 : 0);

            // Verified
            cmbVerified.getSelectionModel().select(user.getIsVerified() ? 1 : 0);

            // 2FA
            cmb2FA.getSelectionModel().select(user.getIs2faEnabled() ? 1 : 0);
        }
    }

    /** Callback to invoke after a successful save (refresh the table). */
    public void setOnSaved(Runnable callback) {
        this.onSaved = callback;
    }

    // -------------------------------------------------------
    // Actions
    // -------------------------------------------------------
    @FXML
    private void save() {
        clearError();

        // --- Validation ---
        String name     = tfName.getText().trim();
        String email    = tfEmail.getText().trim();
        String password = pfPassword.getText();

        if (name.isEmpty()) { showError("Le nom est obligatoire."); return; }
        if (email.isEmpty()) { showError("L'email est obligatoire."); return; }
        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            showError("L'email n'est pas valide."); return;
        }
        if (userToEdit == null && password.isEmpty()) {
            showError("Le mot de passe est obligatoire lors de la création."); return;
        }

        int xp = 0;
        try {
            String xpText = tfXP.getText().trim();
            if (!xpText.isEmpty()) xp = Integer.parseInt(xpText);
        } catch (NumberFormatException e) {
            showError("Les points XP doivent être un nombre entier."); return;
        }

        // --- Roles ---
        int roleIdx = cmbRoles.getSelectionModel().getSelectedIndex();
        String roles = roleIdx == 1 ? "[\"ROLE_ADMIN\"]" :
                       roleIdx == 2 ? "[\"ROLE_MODERATOR\"]" :
                                      "[\"ROLE_USER\"]";

        int     blocked  = cmbBlocked.getSelectionModel().getSelectedIndex();   // 0 or 1
        boolean verified = cmbVerified.getSelectionModel().getSelectedIndex() == 1;
        boolean twoFA    = cmb2FA.getSelectionModel().getSelectedIndex() == 1;

        try {
            if (userToEdit == null) {
                // ---- CREATE ----
                User newUser = new User();
                newUser.setName(name);
                newUser.setEmail(email);
                
                // Hash password before saving
                newUser.setPassword(PasswordUtils.hashPassword(password));
                
                newUser.setRoles(roles);
                newUser.setIsBlocked(blocked);
                newUser.setIsVerified(verified);
                newUser.setIs2faEnabled(twoFA);
                newUser.setExperiencePoints(0);   // Always 0 on creation
                newUser.setResetPasswordAttempts(0);
                
                // Set security fields to satisfy NOT NULL constraints
                newUser.setKnownIps("[\"127.0.0.1\"]");
                newUser.setTotpSecret("");
                newUser.setVerificationToken("");

                serviceUser.ajouter(newUser);
                showSuccess("Utilisateur créé avec succès !");
            } else {
                // ---- UPDATE ----
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

            // Refresh the table and close
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

    // -------------------------------------------------------
    // Helpers
    // -------------------------------------------------------
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
        lblError.setStyle("-fx-text-fill: #f87171;");  // reset to error color
    }
}

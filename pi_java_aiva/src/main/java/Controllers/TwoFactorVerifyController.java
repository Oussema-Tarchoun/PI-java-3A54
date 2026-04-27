package Controllers;

import Models.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import utils.TotpUtils;
import utils.ValidationUtils;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.control.Button;

import java.io.IOException;

public class TwoFactorVerifyController {

    @FXML private TextField tfCode;
    @FXML private Label lblError;
    @FXML private VBox vboxQrCode;
    @FXML private ImageView ivQrCode;
    @FXML private Button btnShowQr;

    private User user;

    public void setUser(User user) {
        this.user = user;
        
        if (user.getIs2faEnabled() && (user.getTotpSecret() == null || user.getTotpSecret().isEmpty())) {
            String newSecret = TotpUtils.generateSecret();
            user.setTotpSecret(newSecret);
            try {
                new Services.ServiceUser().modifier(user);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    public void initialize() {
        ValidationUtils.applyNumericFilter(tfCode);
        ValidationUtils.applyLengthLimit(tfCode, 6);
    }

    @FXML
    private void handleVerify() {
        String code = tfCode.getText().trim();
        if (!ValidationUtils.isNotEmpty(code)) {
            showError("Veuillez entrer le code.");
            return;
        }

        if (code.length() != 6) {
            showError("Le code doit être de 6 chiffres.");
            return;
        }

        if (TotpUtils.verifyCode(user.getTotpSecret(), code)) {
            navigateTo("/Dashboard.fxml", "AIVA — Tableau de Bord");
        } else {
            showError("Code incorrect. Veuillez réessayer.");
        }
    }

    @FXML
    private void handleToggleQr() {
        boolean isVisible = vboxQrCode.isVisible();
        if (!isVisible) {
            try {
                ivQrCode.setImage(TotpUtils.generateQrCodeImage(user.getEmail(), user.getTotpSecret()));
                vboxQrCode.setVisible(true);
                vboxQrCode.setManaged(true);
                btnShowQr.setText("Masquer le QR Code");
            } catch (Exception e) {
                showError("Erreur lors de la génération du QR Code.");
                e.printStackTrace();
            }
        } else {
            vboxQrCode.setVisible(false);
            vboxQrCode.setManaged(false);
            btnShowQr.setText("Afficher le QR Code");
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

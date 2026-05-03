package Controllers;

import Models.User;
import Services.ServiceUser;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import utils.TotpUtils;

import java.sql.SQLException;

public class TwoFactorSetupController {

    @FXML private ImageView ivQrCode;
    @FXML private TextField tfConfirmCode;
    @FXML private Label lblError;

    private User user;
    private String tempSecret;
    private Runnable onSuccess;
    private final ServiceUser serviceUser = new ServiceUser();

    public void init(User user, Runnable onSuccess) {
        this.user = user;
        this.onSuccess = onSuccess;
        this.tempSecret = TotpUtils.generateSecret();
        
        try {
            ivQrCode.setImage(TotpUtils.generateQrCodeImage(user.getEmail(), tempSecret));
        } catch (Exception e) {
            showError("Erreur lors de la génération du QR Code.");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleEnable() {
        String code = tfConfirmCode.getText().trim();
        if (code.isEmpty()) {
            showError("Veuillez entrer le code de confirmation.");
            return;
        }

        if (TotpUtils.verifyCode(tempSecret, code)) {
            try {
                user.setTotpSecret(tempSecret);
                user.setIs2faEnabled(true);
                serviceUser.modifier(user);
                if (onSuccess != null) onSuccess.run();
                close();
            } catch (SQLException e) {
                showError("Erreur lors de la mise à jour de la base de données.");
                e.printStackTrace();
            }
        } else {
            showError("Code de confirmation invalide.");
        }
    }

    @FXML
    private void handleCancel() {
        close();
    }

    private void close() {
        ((Stage) tfConfirmCode.getScene().getWindow()).close();
    }

    private void showError(String message) {
        lblError.setText(message);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }
}

package Controllers;

import Models.User;
import Services.ServiceUser;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;

public class UserProfileController {

    @FXML private Label lblHeaderName;
    @FXML private Label lblName;
    @FXML private Label lblEmail;
    @FXML private Label lblRoleBadge;
    @FXML private Label lblId;
    @FXML private Label lblFullName;
    @FXML private Label lblStatus;
    @FXML private Label lblVerified;
    @FXML private Label lbl2FA;
    @FXML private Label lblXP;
    @FXML private Label lblIPs;
    @FXML private Label lblResetAttempts;
    @FXML private ProgressIndicator progressXP;
    @FXML private Button btnBlock;
    @FXML private Button btn2FA;

    private User user;
    private final ServiceUser serviceUser = new ServiceUser();

    public void setUser(User user) {
        this.user = user;
        populateData();
    }

    private void populateData() {
        if (user == null) return;

        lblHeaderName.setText(user.getName());
        lblName.setText(user.getName());
        lblEmail.setText(user.getEmail());
        lblFullName.setText(user.getName());
        lblId.setText("#" + user.getId());
        
        // Role Badge
        String roles = user.getRoles();
        if (roles != null) {
            if (roles.contains("ROLE_ADMIN")) {
                lblRoleBadge.setText("ADMINISTRATEUR");
                lblRoleBadge.setStyle("-fx-background-color: #6c63ff; -fx-text-fill: white; -fx-padding: 4 12; -fx-background-radius: 20;");
            } else if (roles.contains("ROLE_MODERATOR")) {
                lblRoleBadge.setText("MODÉRATEUR");
                lblRoleBadge.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: white; -fx-padding: 4 12; -fx-background-radius: 20;");
            } else {
                lblRoleBadge.setText("UTILISATEUR");
                lblRoleBadge.setStyle("-fx-background-color: #373a50; -fx-text-fill: white; -fx-padding: 4 12; -fx-background-radius: 20;");
            }
        }

        // Status
        if (user.getIsBlocked() == 1) {
            lblStatus.setText("🔒 BLOQUÉ");
            lblStatus.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
            btnBlock.setText("🔓 Débloquer");
        } else {
            lblStatus.setText("✔ ACTIF");
            lblStatus.setStyle("-fx-text-fill: #22c55e; -fx-font-weight: bold;");
            btnBlock.setText("🔒 Bloquer");
        }

        lblVerified.setText(user.getIsVerified() ? "✅ VÉRIFIÉ" : "❌ NON VÉRIFIÉ");
        lblVerified.setStyle(user.getIsVerified() ? "-fx-text-fill: #6c63ff; -fx-font-weight: bold;" : "-fx-text-fill: #9ca3b0;");

        lbl2FA.setText(user.getIs2faEnabled() ? "ACTIVÉ" : "DÉSACTIVÉ");
        btn2FA.setText(user.getIs2faEnabled() ? "Désactiver" : "Configurer");
        
        // XP
        int xp = user.getExperiencePoints();
        lblXP.setText(String.format("%, d", xp));
        double progress = (xp % 1000) / 1000.0; // Assume levels every 1000 XP
        progressXP.setProgress(progress);

        // Security
        lblIPs.setText(user.getKnownIps() != null ? user.getKnownIps() : "Aucune IP enregistrée");
        lblResetAttempts.setText(String.valueOf(user.getResetPasswordAttempts()));
    }

    @FXML
    private void goBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/UserList.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) lblName.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("AIVA Admin — Gestion des Utilisateurs");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleEdit() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/UserForm.fxml"));
            Parent root = loader.load();

            UserFormController formCtrl = loader.getController();
            formCtrl.setUserToEdit(user);
            formCtrl.setOnSaved(() -> {
                populateData();
            });

            Stage dialog = new Stage();
            dialog.setTitle("Modifier l'utilisateur");
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(lblName.getScene().getWindow());
            dialog.setScene(new Scene(root));
            dialog.setResizable(false);
            dialog.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBlock() {
        boolean isCurrentlyBlocked = user.getIsBlocked() == 1;
        String action = isCurrentlyBlocked ? "débloquer" : "bloquer";

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer l'action");
        confirm.setHeaderText("Voulez-vous " + action + " l'utilisateur : " + user.getName() + " ?");
        confirm.getDialogPane().setStyle("-fx-background-color: #1a1d2e;");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                user.setIsBlocked(isCurrentlyBlocked ? 0 : 1);
                serviceUser.modifier(user);
                populateData();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleDelete() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer la suppression");
        confirm.setHeaderText("Supprimer l'utilisateur : " + user.getName() + " ?");
        confirm.setContentText("Cette action est irréversible.");
        confirm.getDialogPane().setStyle("-fx-background-color: #1a1d2e;");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                serviceUser.supprimer(user);
                goBack();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleExport() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exporter le profil PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF (*.pdf)", "*.pdf"));
        chooser.setInitialFileName("profil_" + user.getName().replace(" ", "_") + ".pdf");
        File file = chooser.showSaveDialog(lblName.getScene().getWindow());
        
        if (file == null) return;

        try {
            Document doc = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(doc, new FileOutputStream(file));
            doc.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, new BaseColor(108, 99, 255));
            Paragraph title = new Paragraph("RAPPORT DE PROFIL UTILISATEUR", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(30);
            doc.add(title);

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);

            BaseColor labelBg = new BaseColor(240, 240, 245);
            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BaseColor.DARK_GRAY);
            Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 11, BaseColor.BLACK);

            addTableCell(table, "NOM COMPLET", user.getName(), labelBg, labelFont, valueFont);
            addTableCell(table, "EMAIL", user.getEmail(), BaseColor.WHITE, labelFont, valueFont);
            addTableCell(table, "RÔLE", user.getRoles(), labelBg, labelFont, valueFont);
            addTableCell(table, "POINTS XP", String.valueOf(user.getExperiencePoints()), BaseColor.WHITE, labelFont, valueFont);
            addTableCell(table, "STATUT", user.getIsBlocked() == 1 ? "BLOQUÉ" : "ACTIF", labelBg, labelFont, valueFont);
            addTableCell(table, "VÉRIFIÉ", user.getIsVerified() ? "OUI" : "NON", BaseColor.WHITE, labelFont, valueFont);

            doc.add(table);

            Paragraph footer = new Paragraph("\n\nDocument généré par AIVA Admin Portal le " + java.time.LocalDate.now(), 
                FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, BaseColor.GRAY));
            footer.setAlignment(Element.ALIGN_CENTER);
            doc.add(footer);

            doc.close();
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "PDF enregistré avec succès !");
            alert.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addTableCell(PdfPTable table, String label, String value, BaseColor bg, Font lFont, Font vFont) {
        PdfPCell c1 = new PdfPCell(new Phrase(label, lFont));
        c1.setBackgroundColor(bg);
        c1.setPadding(10);
        c1.setBorder(Rectangle.NO_BORDER);
        table.addCell(c1);

        PdfPCell c2 = new PdfPCell(new Phrase(value != null ? value : "—", vFont));
        c2.setBackgroundColor(bg);
        c2.setPadding(10);
        c2.setBorder(Rectangle.NO_BORDER);
        table.addCell(c2);
    }

    @FXML
    private void handleSetup2FA() {
        if (user.getIs2faEnabled()) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Désactiver 2FA");
            confirm.setHeaderText("Voulez-vous désactiver la double authentification ?");
            confirm.getDialogPane().setStyle("-fx-background-color: #1a1d2e;");

            if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                try {
                    user.setIs2faEnabled(false);
                    user.setTotpSecret(null);
                    serviceUser.modifier(user);
                    populateData();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        } else {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/TwoFactorSetup.fxml"));
                Parent root = loader.load();

                TwoFactorSetupController controller = loader.getController();
                controller.init(user, this::populateData);

                Stage dialog = new Stage();
                dialog.setTitle("Configurer 2FA");
                dialog.initModality(Modality.APPLICATION_MODAL);
                dialog.initOwner(lbl2FA.getScene().getWindow());
                dialog.setScene(new Scene(root));
                dialog.setResizable(false);
                dialog.showAndWait();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleManageReports() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ReportList.fxml"));
            Parent root = loader.load();

            ReportListController controller = loader.getController();
            controller.setUser(this.user);

            Stage stage = (Stage) lblName.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("Gérer les Rapports — " + user.getName());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

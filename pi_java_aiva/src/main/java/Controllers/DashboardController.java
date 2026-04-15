package Controllers;

import Services.ServiceUser;
import Models.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.event.ActionEvent;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class DashboardController {

    @FXML private Label lblTotalUsers;
    @FXML private Label lblVerifiedUsers;
    @FXML private Label lblBlockedUsers;
    @FXML private Label lblAdminUsers;

    private final ServiceUser serviceUser = new ServiceUser();

    @FXML
    public void initialize() {
        loadStats();
    }

    @FXML
    private void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) lblTotalUsers.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("AIVA — Connexion");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadStats() {
        try {
            List<User> users = serviceUser.recuperer();
            long verified  = users.stream().filter(User::getIsVerified).count();
            long blocked   = users.stream().filter(u -> u.getIsBlocked() == 1).count();
            long admins    = users.stream()
                                  .filter(u -> u.getRoles() != null && u.getRoles().contains("ROLE_ADMIN"))
                                  .count();

            lblTotalUsers.setText(String.valueOf(users.size()));
            lblVerifiedUsers.setText(String.valueOf(verified));
            lblBlockedUsers.setText(String.valueOf(blocked));
            lblAdminUsers.setText(String.valueOf(admins));
        } catch (SQLException e) {
            lblTotalUsers.setText("!");
            lblVerifiedUsers.setText("!");
            lblBlockedUsers.setText("!");
            lblAdminUsers.setText("!");
            e.printStackTrace();
        }
    }

    @FXML
    void openUserManagement() {
        switchScene("/UserList.fxml", "AIVA Admin — Gestion des Utilisateurs");
    }

    @FXML
    void openReportManagement() {
        switchScene("/GlobalReportList.fxml", "AIVA Admin — Gestion Globale des Rapports");
    }

    private void switchScene(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) lblTotalUsers.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle(title);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

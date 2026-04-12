package Controllers;

import Models.Report;
import Models.User;
import Services.ServiceReport;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class ReportListController {

    @FXML private TableView<Report> tableReports;
    @FXML private TableColumn<Report, String> colId;
    @FXML private TableColumn<Report, String> colTitle;
    @FXML private TableColumn<Report, String> colDate;
    @FXML private TableColumn<Report, String> colDescription;
    @FXML private Button btnEdit;
    @FXML private Button btnDelete;

    private User user;
    private final ServiceReport serviceReport = new ServiceReport();

    public void setUser(User user) {
        this.user = user;
        loadReports();
    }

    @FXML
    public void initialize() {
        setupColumns();
        
        tableReports.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            boolean hasSelection = selected != null;
            btnEdit.setDisable(!hasSelection);
            btnDelete.setDisable(!hasSelection);
        });
        
        btnEdit.setDisable(true);
        btnDelete.setDisable(true);
    }

    private void setupColumns() {
        colId.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getId())));
        colTitle.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTitle()));
        colDate.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCreatedAt().toString()));
        colDescription.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDescription()));
    }

    private void loadReports() {
        if (user == null) return;
        List<Report> reports = serviceReport.getReportsByUser(user);
        tableReports.setItems(FXCollections.observableArrayList(reports));
    }

    @FXML
    private void handleAddReport() {
        openReportForm(null);
    }

    @FXML
    private void handleEditReport() {
        Report selected = tableReports.getSelectionModel().getSelectedItem();
        if (selected != null) {
            openReportForm(selected);
        }
    }

    @FXML
    private void handleDeleteReport() {
        Report selected = tableReports.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer la suppression");
        confirm.setHeaderText("Supprimer le rapport : " + selected.getTitle() + " ?");
        confirm.getDialogPane().setStyle("-fx-background-color: #1a1d2e;");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            serviceReport.deleteReport(selected);
            loadReports();
        }
    }

    private void openReportForm(Report reportToEdit) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ReportForm.fxml"));
            Parent root = loader.load();

            ReportFormController controller = loader.getController();
            controller.setReportData(user, reportToEdit, this::loadReports);

            Stage dialog = new Stage();
            dialog.setTitle(reportToEdit == null ? "Nouveau Rapport" : "Modifier le Rapport");
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(tableReports.getScene().getWindow());
            dialog.setScene(new Scene(root));
            dialog.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goBackToProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/UserProfile.fxml"));
            Parent root = loader.load();

            UserProfileController controller = loader.getController();
            controller.setUser(user);

            Stage stage = (Stage) tableReports.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

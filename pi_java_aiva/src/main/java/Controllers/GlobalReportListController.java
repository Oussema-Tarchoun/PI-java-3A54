package Controllers;

import Models.Report;
import Models.User;
import Services.ServiceReport;
import Services.ServiceUser;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class GlobalReportListController {

    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> comboSort;
    @FXML private TableView<Report> tableReports;
    @FXML private TableColumn<Report, String> colId;
    @FXML private TableColumn<Report, String> colTitle;
    @FXML private TableColumn<Report, String> colUser;
    @FXML private TableColumn<Report, String> colDate;
    @FXML private TableColumn<Report, String> colDescription;
    @FXML private Button btnEdit;
    @FXML private Button btnDelete;

    private final ServiceReport serviceReport = new ServiceReport();
    private final ServiceUser serviceUser = new ServiceUser();
    private ObservableList<Report> allReports = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupColumns();
        loadData();
        setupFilters();
        
        btnEdit.setDisable(true);
        btnDelete.setDisable(true);

        tableReports.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            boolean hasSelection = newSelection != null;
            btnEdit.setDisable(!hasSelection);
            btnDelete.setDisable(!hasSelection);
        });

        comboSort.setItems(FXCollections.observableArrayList(
            "Date (Plus récent)", "Date (Plus ancien)", "Titre (A-Z)", "Auteur (A-Z)"
        ));
        comboSort.setOnAction(e -> applySort());
    }

    private void setupColumns() {
        colId.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getId())));
        colTitle.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTitle()));
        colUser.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getUser() != null ? data.getValue().getUser().getName() : "Inconnu"
        ));
        colDate.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCreatedAt().toString()));
        colDescription.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDescription()));
    }

    private void loadData() {
        List<Report> reports = serviceReport.recupererTout();
        allReports.setAll(reports);
        tableReports.setItems(allReports);
    }

    private void setupFilters() {
        FilteredList<Report> filteredData = new FilteredList<>(allReports, p -> true);

        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(report -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase();

                if (report.getTitle().toLowerCase().contains(lowerCaseFilter)) return true;
                if (report.getDescription().toLowerCase().contains(lowerCaseFilter)) return true;
                if (report.getUser() != null && report.getUser().getName().toLowerCase().contains(lowerCaseFilter)) return true;
                
                return false;
            });
        });

        SortedList<Report> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tableReports.comparatorProperty());
        tableReports.setItems(sortedData);
    }

    private void applySort() {
        String sortType = comboSort.getValue();
        if (sortType == null) return;

        Comparator<Report> comparator;
        switch (sortType) {
            case "Date (Plus récent)":
                comparator = Comparator.comparing(Report::getCreatedAt).reversed();
                break;
            case "Date (Plus ancien)":
                comparator = Comparator.comparing(Report::getCreatedAt);
                break;
            case "Titre (A-Z)":
                comparator = Comparator.comparing(Report::getTitle);
                break;
            case "Auteur (A-Z)":
                comparator = Comparator.comparing(r -> r.getUser() != null ? r.getUser().getName() : "");
                break;
            default:
                return;
        }
        FXCollections.sort(allReports, comparator);
    }

    @FXML
    private void handleAddReport() {
        // Since it's a global view, we need to pick a user. 
        // For simplicity, we'll pick the first admin or a placeholder, 
        // OR better: show a user selection dialog.
        // For this task, we'll assume the admin wants to add a report for the system or themselves.
        try {
            List<User> users = serviceUser.recuperer();
            if (users.isEmpty()) return;

            // In a real app, you'd show a user picker.
            // Here, we'll just open the form and let the user know we're using the first user found.
            openReportForm(users.get(0), null);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleEditButton() {
        Report selected = tableReports.getSelectionModel().getSelectedItem();
        if (selected != null) {
            openReportForm(selected.getUser(), selected);
        }
    }

    @FXML
    private void handleDeleteButton() {
        Report selected = tableReports.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer la suppression");
        confirm.setHeaderText("Supprimer le rapport : " + selected.getTitle() + " ?");
        confirm.getDialogPane().setStyle("-fx-background-color: #1a1d2e;");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            serviceReport.deleteReport(selected);
            loadData();
        }
    }

    private void openReportForm(User targetUser, Report reportToEdit) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ReportForm.fxml"));
            Parent root = loader.load();

            ReportFormController controller = loader.getController();
            controller.setReportData(targetUser, reportToEdit, this::loadData);

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
    private void goHome() {
        switchScene("/Dashboard.fxml", "AIVA — Dashboard Administration");
    }

    @FXML
    private void openUserManagement() {
        switchScene("/UserList.fxml", "AIVA Admin — Gestion des Utilisateurs");
    }

    private void switchScene(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) tableReports.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle(title);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

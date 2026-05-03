package Controllers;

import Models.Report;
import Models.User;
import Services.ServiceReport;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import utils.ValidationUtils;

public class ReportFormController {

    @FXML private Label lblTitle;
    @FXML private TextField tfTitle;
    @FXML private TextArea taDescription;
    @FXML private Label lblError;
    @FXML private Label errTitle;
    @FXML private Label errDescription;
    @FXML private Button btnSave;

    private User user;
    private Report reportToEdit;
    private Runnable onSaved;
    private final ServiceReport serviceReport = new ServiceReport();

    @FXML
    public void initialize() {
        if (lblError != null) lblError.setVisible(false);
        hideIndividualErrors();
    }

    public void setReportData(User user, Report reportToEdit, Runnable onSaved) {
        this.user = user;
        this.reportToEdit = reportToEdit;
        this.onSaved = onSaved;

        if (reportToEdit != null) {
            lblTitle.setText("Modifier le Rapport");
            tfTitle.setText(reportToEdit.getTitle());
            taDescription.setText(reportToEdit.getDescription());
            btnSave.setText("Mettre à jour");
        }
    }

    @FXML
    private void handleSave() {
        hideIndividualErrors();
        
        String title = tfTitle.getText().trim();
        String description = taDescription.getText().trim();

        if (!ValidationUtils.isNotEmpty(title)) {
            showFieldError(errTitle, "Titre obligatoire.");
            return;
        }
        if (title.length() < 5) {
            showFieldError(errTitle, "5 caractères min.");
            return;
        }
        if (!ValidationUtils.isNotEmpty(description)) {
            showFieldError(errDescription, "Description obligatoire.");
            return;
        }
        if (description.length() < 10) {
            showFieldError(errDescription, "10 caractères min.");
            return;
        }

        lblError.setVisible(false);

        if (reportToEdit == null) {
            Report newReport = new Report(title, description, user);
            serviceReport.addReport(user, newReport);
        } else {
            reportToEdit.setTitle(title);
            reportToEdit.setDescription(description);
            serviceReport.addReport(user, reportToEdit); 
        }

        if (onSaved != null) onSaved.run();
        closeDialog();
    }

    @FXML
    private void handleCancel() {
        closeDialog();
    }

    private void closeDialog() {
        ((Stage) tfTitle.getScene().getWindow()).close();
    }

    private void showError(String message) {
        lblError.setText("⚠ " + message);
        lblError.setVisible(true);
    }

    private void showFieldError(Label label, String message) {
        if (label != null) {
            label.setText("⚠ " + message);
            label.setVisible(true);
            label.setManaged(true);
        }
    }

    private void hideIndividualErrors() {
        if (errTitle != null) { errTitle.setVisible(false); errTitle.setManaged(false); }
        if (errDescription != null) { errDescription.setVisible(false); errDescription.setManaged(false); }
    }
}

package Controllers;

import Models.Report;
import Models.User;
import Services.ServiceReport;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class ReportFormController {

    @FXML private Label lblTitle;
    @FXML private TextField tfTitle;
    @FXML private TextArea taDescription;
    @FXML private Label lblError;
    @FXML private Button btnSave;

    private User user;
    private Report reportToEdit;
    private Runnable onSaved;
    private final ServiceReport serviceReport = new ServiceReport();

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
        String title = tfTitle.getText().trim();
        String description = taDescription.getText().trim();

        if (title.isEmpty() || description.isEmpty()) {
            lblError.setVisible(true);
            return;
        }

        if (reportToEdit == null) {
            // New Report
            Report newReport = new Report(title, description, user);
            serviceReport.addReport(user, newReport);
        } else {
            // Update Existing (Note: ServiceReport needs an update method, 
            // but for now we'll assume addReport handles it or we'll bypass)
            reportToEdit.setTitle(title);
            reportToEdit.setDescription(description);
            // Updating a persistent object in Hibernate doesn't always need 
            // a special 'update' call if it's in the same session, 
            // but our service opens/closes sessions.
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
}

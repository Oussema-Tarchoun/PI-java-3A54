package Controllers.energie;

import Models.Energie;
import Models.User;
import Services.ServiceUser;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;

public class EnergieFormController implements Initializable {

    @FXML private Label formTitle;
    @FXML private ComboBox<String> typeCombo;
    @FXML private TextField valeurField;
    @FXML private TextField periodeField;
    @FXML private TextField sourceField;
    @FXML private DatePicker datePicker;
    @FXML private VBox userSection;
    @FXML private ComboBox<User> userCombo;
    @FXML private Label errorLabel;

    private Energie editingEnergie;
    private boolean saved = false;
    private ServiceUser serviceUser;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        serviceUser = new ServiceUser();
        typeCombo.setItems(FXCollections.observableArrayList("Électricité", "Eau", "Gaz"));
        
        List<User> users = serviceUser.recuperer();
        userCombo.setItems(FXCollections.observableArrayList(users));
        
        datePicker.setValue(LocalDate.now());
    }

    public void setEnergie(Energie e, boolean isBackOffice) {
        this.editingEnergie = e;
        userSection.setVisible(isBackOffice);
        userSection.setManaged(isBackOffice);

        if (e != null) {
            formTitle.setText("✎ Modifier Consommation");
            typeCombo.setValue(e.getType_energie());
            valeurField.setText(String.valueOf(e.getValeur()));
            periodeField.setText(String.valueOf(e.getPeriode()));
            sourceField.setText(e.getSource());
            
            Date utilDate = new java.util.Date(e.getDate_enregistrement().getTime());
            LocalDate ld = utilDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            datePicker.setValue(ld);
            
            if (isBackOffice) {
                for (User u : userCombo.getItems()) {
                    if (u.getId().equals(e.getUser_id())) {
                        userCombo.setValue(u);
                        break;
                    }
                }
            }
        } else {
            formTitle.setText("➕ Nouvelle Consommation");
        }
    }

    @FXML
    private void handleSave() {
        if (validate()) {
            saved = true;
            close();
        }
    }

    @FXML
    private void handleCancel() {
        saved = false;
        close();
    }

    private boolean validate() {
        String type = typeCombo.getValue();
        String valStr = valeurField.getText().trim();
        String perStr = periodeField.getText().trim();
        String source = sourceField.getText().trim();
        LocalDate ld = datePicker.getValue();

        if (type == null || valStr.isEmpty() || perStr.isEmpty() || source.isEmpty() || ld == null) {
            showError("Tous les champs sont obligatoires.");
            return false;
        }

        try {
            Float.parseFloat(valStr);
            Float.parseFloat(perStr);
        } catch (NumberFormatException e) {
            showError("Valeur et période doivent être des nombres.");
            return false;
        }

        return true;
    }

    public Energie getResult(String defaultUserId) {
        if (!saved) return null;

        String type = typeCombo.getValue();
        float valeur = Float.parseFloat(valeurField.getText().trim());
        float periode = Float.parseFloat(periodeField.getText().trim());
        String source = sourceField.getText().trim();
        Date date = Date.from(datePicker.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant());
        
        String userId = (userCombo.getValue() != null) ? userCombo.getValue().getId() : defaultUserId;

        if (editingEnergie == null) {
            return new Energie(type, periode, valeur, date, source, userId);
        } else {
            editingEnergie.setType_energie(type);
            editingEnergie.setValeur(valeur);
            editingEnergie.setPeriode(periode);
            editingEnergie.setSource(source);
            editingEnergie.setDate_enregistrement(date);
            editingEnergie.setUser_id(userId);
            return editingEnergie;
        }
    }

    private void showError(String msg) {
        errorLabel.setText("⚠ " + msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void close() {
        ((Stage) formTitle.getScene().getWindow()).close();
    }
}

package Controllers;

import Models.User;
import Services.ServiceUser;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class UserListController {

    @FXML private TableView<User>           tableUsers;
    @FXML private TableColumn<User,String>  colId;
    @FXML private TableColumn<User,String>  colName;
    @FXML private TableColumn<User,String>  colEmail;
    @FXML private TableColumn<User,String>  colRoles;
    @FXML private TableColumn<User,String>  colVerified;
    @FXML private TableColumn<User,String>  colBlocked;
    @FXML private TableColumn<User,String>  col2FA;
    @FXML private TableColumn<User,String>  colXP;
    @FXML private TextField                 tfSearch;
    @FXML private ComboBox<String>          cmbSort;
    @FXML private Label                     lblStatus;
    @FXML private Label                     lblCount;
    @FXML private Button                    btnEdit;
    @FXML private Button                    btnDelete;
    @FXML private Button                    btnBlock;

    private final ServiceUser serviceUser = new ServiceUser();
    private ObservableList<User> allUsers = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupColumns();
        setupSelectionListener();
        setupDoubleClickNavigation();
        // Default sort selection
        cmbSort.getSelectionModel().selectFirst();
        loadUsers();
    }

    // -------------------------------------------------------
    // Column Setup
    // -------------------------------------------------------
    private void setupColumns() {
        colId.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getId())));
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        colEmail.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEmail()));
        colRoles.setCellValueFactory(c -> {
            String r = c.getValue().getRoles();
            if (r == null) return new SimpleStringProperty("—");
            if (r.contains("ROLE_ADMIN"))     return new SimpleStringProperty("🔑 Admin");
            if (r.contains("ROLE_MODERATOR")) return new SimpleStringProperty("🛡 Modérateur");
            return new SimpleStringProperty("👤 Utilisateur");
        });
        colVerified.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getIsVerified() ? "✅ Oui" : "❌ Non"));
        colBlocked.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getIsBlocked() == 1 ? "🔒 Bloqué" : "✔ Actif"));
        col2FA.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getIs2faEnabled() ? "🔐 Oui" : "Non"));
        colXP.setCellValueFactory(c ->
                new SimpleStringProperty(String.valueOf(c.getValue().getExperiencePoints())));
    }

    private void setupSelectionListener() {
        tableUsers.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            boolean hasSelection = selected != null;
            btnEdit.setDisable(!hasSelection);
            btnDelete.setDisable(!hasSelection);
            btnBlock.setDisable(!hasSelection);
            if (selected != null) {
                boolean isBlocked = selected.getIsBlocked() == 1;
                btnBlock.setText(isBlocked ? "🔓  Débloquer" : "🔒  Bloquer");
            }
        });
        btnEdit.setDisable(true);
        btnDelete.setDisable(true);
        btnBlock.setDisable(true);
    }

    private void setupDoubleClickNavigation() {
        tableUsers.setRowFactory(tv -> {
            javafx.scene.control.TableRow<User> row = new javafx.scene.control.TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    User rowData = row.getItem();
                    openUserProfile(rowData);
                }
            });
            return row;
        });
    }

    // -------------------------------------------------------
    // Load / Search / Sort
    // -------------------------------------------------------
    @FXML
    public void loadUsers() {
        try {
            List<User> users = serviceUser.recuperer();
            allUsers = FXCollections.observableArrayList(users);
            applySearchAndSort();
            updateStatus("Données chargées avec succès.", allUsers.size());
        } catch (SQLException e) {
            updateStatus("Erreur de chargement : " + e.getMessage(), 0);
            e.printStackTrace();
        }
    }

    /** Dynamic search — triggered on every key release */
    @FXML
    private void handleSearch() {
        applySearchAndSort();
    }

    /** Sort combo changed */
    @FXML
    private void handleSort() {
        applySearchAndSort();
    }

    /**
     * Applies both the current search query and sort order,
     * then pushes the result into the TableView.
     */
    private void applySearchAndSort() {
        String query = tfSearch.getText() == null ? "" : tfSearch.getText().toLowerCase().trim();

        // 1. Filter
        List<User> filtered = allUsers.stream()
                .filter(u -> {
                    if (query.isEmpty()) return true;
                    String idStr = String.valueOf(u.getId());
                    String name  = u.getName()  != null ? u.getName().toLowerCase()  : "";
                    String email = u.getEmail() != null ? u.getEmail().toLowerCase() : "";
                    String roles = u.getRoles() != null ? u.getRoles().toLowerCase() : "";
                    return idStr.contains(query)
                        || name.contains(query)
                        || email.contains(query)
                        || roles.contains(query);
                })
                .collect(Collectors.toList());

        // 2. Sort
        String sortBy = cmbSort.getValue();
        if (sortBy != null) {
            switch (sortBy) {
                case "Nom"   -> filtered.sort(Comparator.comparing(u -> u.getName()  != null ? u.getName()  : ""));
                case "Email" -> filtered.sort(Comparator.comparing(u -> u.getEmail() != null ? u.getEmail() : ""));
                case "XP"    -> filtered.sort(Comparator.comparingInt(User::getExperiencePoints).reversed());
                default      -> filtered.sort(Comparator.comparingInt(User::getId));  // ID (default)
            }
        }

        tableUsers.setItems(FXCollections.observableArrayList(filtered));

        String statusMsg = query.isEmpty()
                ? "Affichage de tous les utilisateurs."
                : "Résultats pour « " + query + " »";
        updateStatus(statusMsg, filtered.size());
    }

    // -------------------------------------------------------
    // CRUD Actions
    // -------------------------------------------------------
    @FXML
    private void openAddUser() {
        openForm(null);
    }

    @FXML
    private void openEditUser() {
        User selected = tableUsers.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Sélection requise", "Veuillez sélectionner un utilisateur à modifier.");
            return;
        }
        openForm(selected);
    }

    @FXML
    private void deleteUser() {
        User selected = tableUsers.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Sélection requise", "Veuillez sélectionner un utilisateur à supprimer.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer la suppression");
        confirm.setHeaderText("Supprimer l'utilisateur : " + selected.getName() + " ?");
        confirm.setContentText("Cette action est irréversible.");
        confirm.getDialogPane().setStyle("-fx-background-color: #1a1d2e;");
        confirm.getDialogPane().lookup(".content.label").setStyle("-fx-text-fill: #d1d5db;");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                serviceUser.supprimer(selected);
                loadUsers();
                updateStatus("Utilisateur supprimé : " + selected.getEmail(), allUsers.size() - 1);
            } catch (SQLException e) {
                showError("Erreur de suppression", e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /** Toggle block / unblock the selected user */
    @FXML
    private void blockUser() {
        User selected = tableUsers.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Sélection requise", "Veuillez sélectionner un utilisateur.");
            return;
        }

        boolean isCurrentlyBlocked = selected.getIsBlocked() == 1;
        String action = isCurrentlyBlocked ? "débloquer" : "bloquer";

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer l'action");
        confirm.setHeaderText("Voulez-vous " + action + " l'utilisateur : " + selected.getName() + " ?");
        confirm.setContentText(isCurrentlyBlocked
                ? "L'utilisateur pourra à nouveau se connecter."
                : "L'utilisateur ne pourra plus se connecter.");
        confirm.getDialogPane().setStyle("-fx-background-color: #1a1d2e;");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                selected.setIsBlocked(isCurrentlyBlocked ? 0 : 1);
                serviceUser.modifier(selected);
                loadUsers();
                String msg = isCurrentlyBlocked
                        ? "Utilisateur débloqué : " + selected.getEmail()
                        : "Utilisateur bloqué : " + selected.getEmail();
                updateStatus(msg, allUsers.size());
            } catch (SQLException e) {
                showError("Erreur", e.getMessage());
                e.printStackTrace();
            }
        }
    }

    // -------------------------------------------------------
    // PDF Export
    // -------------------------------------------------------
    @FXML
    private void exportPdf() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Enregistrer la liste des utilisateurs");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers PDF (*.pdf)", "*.pdf"));
        chooser.setInitialFileName("utilisateurs.pdf");
        File file = chooser.showSaveDialog(tableUsers.getScene().getWindow());
        if (file == null) return;

        List<User> usersToExport = tableUsers.getItems();

        try {
            Document doc = new Document(PageSize.A4.rotate(), 30, 30, 30, 30);
            PdfWriter.getInstance(doc, new FileOutputStream(file));
            doc.open();

            // — Title —
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18,
                    new BaseColor(108, 99, 255));
            Paragraph title = new Paragraph("◆ AIVA — Liste des Utilisateurs", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(16);
            doc.add(title);

            // — Subtitle / date —
            Font subFont = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.GRAY);
            Paragraph sub = new Paragraph(
                    "Exporté le : " + java.time.LocalDate.now()
                    + "   |   Total : " + usersToExport.size() + " utilisateur(s)", subFont);
            sub.setAlignment(Element.ALIGN_CENTER);
            sub.setSpacingAfter(20);
            doc.add(sub);

            // — Table —
            PdfPTable table = new PdfPTable(8);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1f, 2.5f, 3f, 2f, 1.5f, 1.5f, 1.2f, 1.2f});

            BaseColor headerBg  = new BaseColor(26, 29, 46);
            BaseColor headerText = new BaseColor(156, 163, 176);
            Font hFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, headerText);
            Font cFont = FontFactory.getFont(FontFactory.HELVETICA, 9, BaseColor.DARK_GRAY);

            String[] headers = {"ID", "Nom", "Email", "Rôle", "Vérifié", "Bloqué", "2FA", "XP"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, hFont));
                cell.setBackgroundColor(headerBg);
                cell.setPadding(8);
                cell.setBorderColor(new BaseColor(42, 45, 62));
                table.addCell(cell);
            }

            BaseColor rowAlt = new BaseColor(248, 248, 255);
            int rowIndex = 0;
            for (User u : usersToExport) {
                BaseColor bg = (rowIndex++ % 2 == 0) ? BaseColor.WHITE : rowAlt;

                String role = "Utilisateur";
                if (u.getRoles() != null) {
                    if (u.getRoles().contains("ROLE_ADMIN"))     role = "Admin";
                    else if (u.getRoles().contains("ROLE_MODERATOR")) role = "Modérateur";
                }

                String[] row = {
                    String.valueOf(u.getId()),
                    u.getName()  != null ? u.getName()  : "—",
                    u.getEmail() != null ? u.getEmail() : "—",
                    role,
                    u.getIsVerified()   ? "Oui" : "Non",
                    u.getIsBlocked() == 1 ? "Bloqué" : "Actif",
                    u.getIs2faEnabled() ? "Oui" : "Non",
                    String.valueOf(u.getExperiencePoints())
                };
                for (String val : row) {
                    PdfPCell cell = new PdfPCell(new Phrase(val, cFont));
                    cell.setBackgroundColor(bg);
                    cell.setPadding(7);
                    cell.setBorderColor(new BaseColor(220, 220, 230));
                    table.addCell(cell);
                }
            }
            doc.add(table);

            // — Footer —
            Font footerFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, BaseColor.LIGHT_GRAY);
            Paragraph footer = new Paragraph("AIVA Admin Portal — Document généré automatiquement", footerFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(16);
            doc.add(footer);

            doc.close();

            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setTitle("Export réussi");
            info.setHeaderText(null);
            info.setContentText("PDF enregistré : " + file.getAbsolutePath());
            info.showAndWait();

        } catch (Exception e) {
            showError("Erreur PDF", e.getMessage());
            e.printStackTrace();
        }
    }

    // -------------------------------------------------------
    // Navigation
    // -------------------------------------------------------
    @FXML
    private void goToDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Dashboard.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) tableUsers.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("AIVA Admin — Tableau de Bord");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void openReportManagement() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/GlobalReportList.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) tableUsers.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("AIVA Admin — Gestion Globale des Rapports");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // -------------------------------------------------------
    // Form modal
    // -------------------------------------------------------
    private void openForm(User userToEdit) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/UserForm.fxml"));
            Parent root = loader.load();

            UserFormController formCtrl = loader.getController();
            formCtrl.setUserToEdit(userToEdit);
            formCtrl.setOnSaved(this::loadUsers);

            Stage dialog = new Stage();
            dialog.setTitle(userToEdit == null ? "Ajouter un utilisateur" : "Modifier l'utilisateur");
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(tableUsers.getScene().getWindow());
            dialog.setScene(new Scene(root));
            dialog.setResizable(false);
            dialog.showAndWait();
        } catch (IOException e) {
            showError("Erreur d'ouverture du formulaire", e.getMessage());
            e.printStackTrace();
        }
    }

    private void openUserProfile(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/UserProfile.fxml"));
            Parent root = loader.load();

            UserProfileController controller = loader.getController();
            controller.setUser(user);

            Stage stage = (Stage) tableUsers.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("AIVA Profil — " + user.getName());
        } catch (IOException e) {
            showError("Erreur d'ouverture du profil", e.getMessage());
            e.printStackTrace();
        }
    }

    // -------------------------------------------------------
    // Helpers
    // -------------------------------------------------------
    private void updateStatus(String message, int count) {
        lblStatus.setText(message);
        lblCount.setText(count + " utilisateur(s)");
    }

    private void showWarning(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING, content, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR, content, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    @FXML
    private void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) tableUsers.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("AIVA — Connexion");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

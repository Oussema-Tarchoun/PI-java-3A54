package controllers;

import Models.Objectif;
import Services.ServiceObjectif;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.SQLDataException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class ObjectifFrontController {

    // ── Stat labels ────────────────────────────────────────
    @FXML private Label    statTotalLabel;
    @FXML private Label    statEnCoursLabel;
    @FXML private Label    statTerminesLabel;

    // ── Card grid ──────────────────────────────────────────
    @FXML private FlowPane cardGrid;
    @FXML private Label    emptyLabel;

    // ── Form ───────────────────────────────────────────────
    @FXML private VBox       formPane;
    @FXML private HBox       btnAjouterBox;
    @FXML private HBox       btnModifierBox;
    @FXML private TextField  descField;
    @FXML private TextField  typeField;
    @FXML private TextField  valeurCibleField;
    @FXML private TextField  statutField;
    @FXML private DatePicker dateDebutPicker;
    @FXML private DatePicker dateFinPicker;
    @FXML private Label      msgLabel;

    private final ServiceObjectif service = new ServiceObjectif();
    private Objectif currentEdit = null;

    // ── Lifecycle ──────────────────────────────────────────

    @FXML
    public void initialize() {
        charger();
    }

    // ── Data ───────────────────────────────────────────────

    @FXML
    public void charger() {
        try {
            List<Objectif> list = service.recuperer();
            updateStats(list);
            buildCards(list);
        } catch (SQLDataException e) {
            msg("Erreur BD : " + e.getMessage(), true);
        }
    }

    private void updateStats(List<Objectif> list) {
        statTotalLabel.setText(String.valueOf(list.size()));
        long enCours = list.stream().filter(o -> isEnCours(o.getStatut())).count();
        long termines = list.stream().filter(o -> isTermine(o.getStatut())).count();
        statEnCoursLabel.setText(String.valueOf(enCours));
        statTerminesLabel.setText(String.valueOf(termines));
    }

    // ── Card builder ───────────────────────────────────────

    private void buildCards(List<Objectif> list) {
        cardGrid.getChildren().clear();
        if (list.isEmpty()) {
            if (emptyLabel != null) { emptyLabel.setVisible(true);  emptyLabel.setManaged(true); }
            return;
        }
        if (emptyLabel != null) { emptyLabel.setVisible(false); emptyLabel.setManaged(false); }
        for (Objectif o : list) cardGrid.getChildren().add(buildCard(o));
    }

    private VBox buildCard(Objectif o) {
        VBox card = new VBox(14);
        card.setPrefWidth(300);
        card.setMinWidth(260);
        card.getStyleClass().add("obj-card");

        // ── Header: icon + title + status badge ──
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        StackPane icon = new StackPane();
        icon.getStyleClass().addAll("obj-icon", resolveIconColor(o.getStatut()));
        Label iconLbl = new Label(resolveEmoji(o.getType()));
        iconLbl.setStyle("-fx-font-size:17px;");
        icon.getChildren().add(iconLbl);

        VBox titleBox = new VBox(2);
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        Label titleLbl = new Label(truncate(o.getDescription(), 30));
        titleLbl.getStyleClass().add("obj-title");
        titleLbl.setWrapText(false);
        Label typeLbl = new Label(o.getType() != null ? o.getType() : "");
        typeLbl.getStyleClass().add("obj-type");
        titleBox.getChildren().addAll(titleLbl, typeLbl);

        Label statusBadge = new Label(o.getStatut() != null ? o.getStatut() : "—");
        statusBadge.getStyleClass().addAll("obj-badge", resolveBadgeStyle(o.getStatut()));

        header.getChildren().addAll(icon, titleBox, statusBadge);

        // ── Progress bar (time-based) ──
        VBox progressSection = new VBox(6);
        double progress = computeProgress(o);
        ProgressBar bar = new ProgressBar(progress);
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.getStyleClass().addAll("obj-progress", resolveProgressStyle(o.getStatut()));

        HBox progressMeta = new HBox();
        progressMeta.setAlignment(Pos.CENTER_LEFT);
        Label progPct = new Label(String.format("%.0f%%", progress * 100));
        progPct.getStyleClass().add("obj-progress-pct");
        HBox.setHgrow(progPct, Priority.ALWAYS);

        String fin = o.getDateFin() != null ? "Fin : " + o.getDateFin() : "";
        Label progDate = new Label(fin);
        progDate.getStyleClass().add("obj-progress-date");
        progressMeta.getChildren().addAll(progPct, progDate);

        progressSection.getChildren().addAll(bar, progressMeta);

        // ── Cible row ──
        HBox cibleRow = new HBox(6);
        cibleRow.setAlignment(Pos.CENTER_LEFT);
        cibleRow.getStyleClass().add("obj-cible-row");
        Label cibleLbl = new Label("Valeur cible :");
        cibleLbl.getStyleClass().add("obj-meta-label");
        Label cibleVal = new Label(String.valueOf(o.getValeurCible()));
        cibleVal.getStyleClass().add("obj-meta-value");
        cibleRow.getChildren().addAll(cibleLbl, cibleVal);

        // ── Actions ──
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button btnEdit = new Button("✏  Modifier");
        btnEdit.getStyleClass().addAll("act-btn", "act-btn-edit");
        btnEdit.setOnAction(e -> {
            remplirFormulaire(o);
            currentEdit = o;
            setModeEdition(true);
            showForm(true);
            msg("Modifiez les champs puis cliquez Modifier.", false);
        });

        Button btnDel = new Button("✕  Supprimer");
        btnDel.getStyleClass().addAll("act-btn", "act-btn-delete");
        btnDel.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Supprimer l'objectif ?", ButtonType.YES, ButtonType.NO);
            confirm.setHeaderText(null);
            confirm.showAndWait().ifPresent(bt -> {
                if (bt == ButtonType.YES) {
                    try { service.supprimer(o); charger(); }
                    catch (SQLDataException ex) { msg("Erreur : " + ex.getMessage(), true); }
                }
            });
        });

        actions.getChildren().addAll(btnEdit, btnDel);

        card.getChildren().addAll(header, progressSection, cibleRow, actions);
        return card;
    }

    // ── Helpers ────────────────────────────────────────────

    private double computeProgress(Objectif o) {
        if (o.getDateDebut() == null || o.getDateFin() == null) return 0;
        LocalDate now   = LocalDate.now();
        long total  = ChronoUnit.DAYS.between(o.getDateDebut(), o.getDateFin());
        long passed = ChronoUnit.DAYS.between(o.getDateDebut(), now);
        if (total <= 0) return isTermine(o.getStatut()) ? 1.0 : 0.0;
        double p = (double) passed / total;
        return Math.max(0, Math.min(1, p));
    }

    private String resolveEmoji(String type) {
        if (type == null) return "🎯";
        String t = type.toLowerCase();
        if (t.contains("poids") || t.contains("weight")) return "⚖";
        if (t.contains("endur") || t.contains("cours"))  return "🏃";
        if (t.contains("muscl") || t.contains("force"))  return "💪";
        if (t.contains("nutrit") || t.contains("diet"))  return "🥗";
        if (t.contains("sleep") || t.contains("sommeil"))return "😴";
        return "🎯";
    }

    private String resolveIconColor(String statut) {
        if (isTermine(statut))  return "obj-icon-green";
        if (isEnCours(statut))  return "obj-icon-cyan";
        return "obj-icon-orange";
    }

    private String resolveBadgeStyle(String statut) {
        if (isTermine(statut))  return "obj-badge-done";
        if (isEnCours(statut))  return "obj-badge-active";
        return "obj-badge-pending";
    }

    private String resolveProgressStyle(String statut) {
        if (isTermine(statut)) return "obj-progress-green";
        if (isEnCours(statut)) return "obj-progress-cyan";
        return "obj-progress-orange";
    }

    private boolean isTermine(String s) {
        return s != null && (s.toLowerCase().contains("termin") || s.toLowerCase().contains("done"));
    }

    private boolean isEnCours(String s) {
        return s != null && (s.toLowerCase().contains("cours") || s.toLowerCase().contains("actif"));
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }

    // ── Form handling ──────────────────────────────────────

    @FXML public void ouvrirFormulaire() {
        currentEdit = null; viderFormulaire(); setModeEdition(false); showForm(true);
    }

    @FXML public void annuler() {
        viderFormulaire(); setModeEdition(false); showForm(false); currentEdit = null;
    }

    private void showForm(boolean v) {
        if (formPane != null) { formPane.setVisible(v); formPane.setManaged(v); }
    }

    private void setModeEdition(boolean e) {
        if (btnAjouterBox  != null) { btnAjouterBox.setVisible(!e);  btnAjouterBox.setManaged(!e); }
        if (btnModifierBox != null) { btnModifierBox.setVisible(e);   btnModifierBox.setManaged(e); }
    }

    // ── CRUD ───────────────────────────────────────────────

    @FXML
    public void ajouter() {
        if (!valider()) return;
        try {
            Objectif o = buildFromForm(new Objectif());
            service.ajouter(o);
            msg("✓ Objectif ajouté !", false);
            viderFormulaire(); showForm(false); charger();
        } catch (SQLDataException e) { msg("Erreur BD : " + e.getMessage(), true); }
    }

    @FXML
    public void modifier() {
        if (currentEdit == null) { msg("Aucun objectif sélectionné.", true); return; }
        if (!valider()) return;
        try {
            buildFromForm(currentEdit);
            service.modifier(currentEdit);
            msg("✓ Objectif modifié !", false);
            viderFormulaire(); setModeEdition(false); showForm(false);
            currentEdit = null; charger();
        } catch (SQLDataException e) { msg("Erreur BD : " + e.getMessage(), true); }
    }

    private Objectif buildFromForm(Objectif o) {
        o.setDescription(descField.getText().trim());
        o.setType(typeField.getText().trim());
        o.setValeurCible(Integer.parseInt(valeurCibleField.getText().trim()));
        o.setDateDebut(dateDebutPicker.getValue());
        o.setDateFin(dateFinPicker.getValue());
        o.setStatut(statutField.getText().trim());
        return o;
    }

    // ── Validation ─────────────────────────────────────────

    private boolean valider() {
        resetStyles();
        boolean ok = true;
        if (descField.getText() == null || descField.getText().trim().length() < 3) {
            err(descField, "Description obligatoire (min. 3 car.)"); ok = false;
        }
        if (typeField.getText() == null || typeField.getText().trim().length() < 2) {
            err(typeField, "Type invalide (min. 2 car.)"); ok = false;
        }
        if (valeurCibleField.getText() == null || valeurCibleField.getText().trim().isEmpty()) {
            err(valeurCibleField, "Valeur cible obligatoire"); ok = false;
        } else {
            try {
                int v = Integer.parseInt(valeurCibleField.getText().trim());
                if (v <= 0) { err(valeurCibleField, "Valeur > 0"); ok = false; }
            } catch (NumberFormatException e) { err(valeurCibleField, "Valeur invalide"); ok = false; }
        }
        if (dateDebutPicker.getValue() == null) {
            dateDebutPicker.setStyle("-fx-border-color:#ef4444;"); msg("Date de début obligatoire", true); ok = false;
        }
        if (dateFinPicker.getValue() == null) {
            dateFinPicker.setStyle("-fx-border-color:#ef4444;"); msg("Date de fin obligatoire", true); ok = false;
        }
        if (dateDebutPicker.getValue() != null && dateFinPicker.getValue() != null
                && dateFinPicker.getValue().isBefore(dateDebutPicker.getValue())) {
            dateFinPicker.setStyle("-fx-border-color:#ef4444;"); msg("La fin doit être après le début", true); ok = false;
        }
        if (statutField.getText() == null || statutField.getText().trim().length() < 2) {
            err(statutField, "Statut invalide (min. 2 car.)"); ok = false;
        }
        return ok;
    }

    private void err(TextField f, String m) {
        f.setStyle("-fx-border-color:#ef4444; -fx-border-width:1; -fx-border-radius:8; -fx-background-radius:8;");
        msg(m, true);
    }

    private void resetStyles() {
        for (TextField f : new TextField[]{descField, typeField, valeurCibleField, statutField}) f.setStyle("");
        dateDebutPicker.setStyle(""); dateFinPicker.setStyle("");
        if (msgLabel != null) msgLabel.setText("");
    }

    private void remplirFormulaire(Objectif o) {
        descField.setText(o.getDescription());
        typeField.setText(o.getType());
        valeurCibleField.setText(String.valueOf(o.getValeurCible()));
        statutField.setText(o.getStatut() != null ? o.getStatut() : "");
        dateDebutPicker.setValue(o.getDateDebut());
        dateFinPicker.setValue(o.getDateFin());
    }

    private void viderFormulaire() {
        descField.clear(); typeField.clear(); valeurCibleField.clear(); statutField.clear();
        dateDebutPicker.setValue(null); dateFinPicker.setValue(null);
        resetStyles();
    }

    private void msg(String m, boolean err) {
        if (msgLabel != null) {
            msgLabel.setText(m);
            msgLabel.setStyle(err
                    ? "-fx-text-fill:#ef4444; -fx-font-size:12px;"
                    : "-fx-text-fill:#10b981; -fx-font-size:12px;");
        }
    }
}
package Models;

import java.time.LocalDate;

public class Apprentissage {
    private int id;
    private int userId;
    private String sujet; // cours, livre, vidéo, projet
    private String titre;
    private String description;
    private int heuresEtudiees;
    private String niveau; // débutant, intermédiaire, avancé
    private LocalDate dateDebut;
    private LocalDate dateFinEstimee;
    private String statut; // en cours, complété, en attente
    private String notes;

    public Apprentissage() {}

    public Apprentissage(int userId, String sujet, String titre, String description, int heuresEtudiees, 
                        String niveau, LocalDate dateDebut, LocalDate dateFinEstimee, String statut, String notes) {
        this.userId = userId;
        this.sujet = sujet;
        this.titre = titre;
        this.description = description;
        this.heuresEtudiees = heuresEtudiees;
        this.niveau = niveau;
        this.dateDebut = dateDebut;
        this.dateFinEstimee = dateFinEstimee;
        this.statut = statut;
        this.notes = notes;
    }

    public Apprentissage(int id, int userId, String sujet, String titre, String description, int heuresEtudiees, 
                        String niveau, LocalDate dateDebut, LocalDate dateFinEstimee, String statut, String notes) {
        this(userId, sujet, titre, description, heuresEtudiees, niveau, dateDebut, dateFinEstimee, statut, notes);
        this.id = id;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getSujet() { return sujet; }
    public void setSujet(String sujet) { this.sujet = sujet; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getHeuresEtudiees() { return heuresEtudiees; }
    public void setHeuresEtudiees(int heuresEtudiees) { this.heuresEtudiees = heuresEtudiees; }

    public String getNiveau() { return niveau; }
    public void setNiveau(String niveau) { this.niveau = niveau; }

    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }

    public LocalDate getDateFinEstimee() { return dateFinEstimee; }
    public void setDateFinEstimee(LocalDate dateFinEstimee) { this.dateFinEstimee = dateFinEstimee; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    @Override
    public String toString() {
        return "Apprentissage{" +
                "id=" + id +
                ", userId=" + userId +
                ", sujet='" + sujet + '\'' +
                ", titre='" + titre + '\'' +
                ", heuresEtudiees=" + heuresEtudiees +
                ", niveau='" + niveau + '\'' +
                ", statut='" + statut + '\'' +
                '}';
    }
}

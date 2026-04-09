package Models;

import java.time.LocalDate;

public class Objectif {

    private int id;
    private String description;
    private String type;
    private int valeurCible;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private String statut;
    private int userId;

    // ── Constructors ──────────────────────────────
    public Objectif() {}

    public Objectif(String description, String type, int valeurCible,
                    LocalDate dateDebut, LocalDate dateFin, String statut, int userId) {
        this.description = description;
        this.type        = type;
        this.valeurCible = valeurCible;
        this.dateDebut   = dateDebut;
        this.dateFin     = dateFin;
        this.statut      = statut;
        this.userId      = userId;
    }

    public Objectif(int id, String description, String type, int valeurCible,
                    LocalDate dateDebut, LocalDate dateFin, String statut, int userId) {
        this(description, type, valeurCible, dateDebut, dateFin, statut, userId);
        this.id = id;
    }

    // ── Getters & Setters ─────────────────────────
    public int getId()                        { return id; }
    public void setId(int id)                 { this.id = id; }

    public String getDescription()            { return description; }
    public void setDescription(String d)      { this.description = d; }

    public String getType()                   { return type; }
    public void setType(String type)          { this.type = type; }

    public int getValeurCible()               { return valeurCible; }
    public void setValeurCible(int v)         { this.valeurCible = v; }

    public LocalDate getDateDebut()           { return dateDebut; }
    public void setDateDebut(LocalDate d)     { this.dateDebut = d; }

    public LocalDate getDateFin()             { return dateFin; }
    public void setDateFin(LocalDate d)       { this.dateFin = d; }

    public String getStatut()                 { return statut; }
    public void setStatut(String statut)      { this.statut = statut; }

    public int getUserId()                    { return userId; }
    public void setUserId(int userId)         { this.userId = userId; }

    // ── toString ──────────────────────────────────
    @Override
    public String toString() {
        return "Objectif{" +
                "id=" + id +
                ", description='" + description + '\'' +
                ", type='" + type + '\'' +
                ", valeurCible=" + valeurCible +
                ", dateDebut=" + dateDebut +
                ", dateFin=" + dateFin +
                ", statut='" + statut + '\'' +
                ", userId=" + userId +
                '}';
    }
}
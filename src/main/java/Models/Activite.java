package Models;

import java.time.LocalDate;

public class Activite {

    private int id;
    private String type;
    private int duree;              // minutes
    private double caloriesBrulees;
    private LocalDate date;
    private String intensite;
    private int objectifId;

    // ── Constructors ──────────────────────────────
    public Activite() {}

    public Activite(String type, int duree, double caloriesBrulees,
                    LocalDate date, String intensite, int objectifId) {
        this.type            = type;
        this.duree           = duree;
        this.caloriesBrulees = caloriesBrulees;
        this.date            = date;
        this.intensite       = intensite;
        this.objectifId      = objectifId;
    }

    public Activite(int id, String type, int duree, double caloriesBrulees,
                    LocalDate date, String intensite, int objectifId) {
        this(type, duree, caloriesBrulees, date, intensite, objectifId);
        this.id = id;
    }

    // ── Getters & Setters ─────────────────────────
    public int getId()                          { return id; }
    public void setId(int id)                   { this.id = id; }

    public String getType()                     { return type; }
    public void setType(String type)            { this.type = type; }

    public int getDuree()                       { return duree; }
    public void setDuree(int duree)             { this.duree = duree; }

    public double getCaloriesBrulees()          { return caloriesBrulees; }
    public void setCaloriesBrulees(double c)    { this.caloriesBrulees = c; }

    public LocalDate getDate()                  { return date; }
    public void setDate(LocalDate date)         { this.date = date; }

    public String getIntensite()                { return intensite; }
    public void setIntensite(String intensite)  { this.intensite = intensite; }

    public int getObjectifId()                  { return objectifId; }
    public void setObjectifId(int objectifId)   { this.objectifId = objectifId; }

    // ── toString ──────────────────────────────────
    @Override
    public String toString() {
        return "Activite{" +
                "id=" + id +
                ", type='" + type + '\'' +
                ", duree=" + duree +
                ", caloriesBrulees=" + caloriesBrulees +
                ", date=" + date +
                ", intensite='" + intensite + '\'' +
                ", objectifId=" + objectifId +
                '}';
    }
}
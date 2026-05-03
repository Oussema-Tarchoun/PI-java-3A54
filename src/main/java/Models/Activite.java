package Models;

import java.time.LocalDate;
import java.time.LocalTime;

public class Activite {
    private int id;
    private int userId;
    private String type; // running, cycling, swimming, etc
    private double distance; // en km
    private LocalTime duree;
    private LocalDate dateActivite;
    private int caloriesBrulees;
    private String notes;

    public Activite() {}

    public Activite(int userId, String type, double distance, LocalTime duree, LocalDate dateActivite, int caloriesBrulees, String notes) {
        this.userId = userId;
        this.type = type;
        this.distance = distance;
        this.duree = duree;
        this.dateActivite = dateActivite;
        this.caloriesBrulees = caloriesBrulees;
        this.notes = notes;
    }

    public Activite(int id, int userId, String type, double distance, LocalTime duree, LocalDate dateActivite, int caloriesBrulees, String notes) {
        this(userId, type, distance, duree, dateActivite, caloriesBrulees, notes);
        this.id = id;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public double getDistance() { return distance; }
    public void setDistance(double distance) { this.distance = distance; }

    public LocalTime getDuree() { return duree; }
    public void setDuree(LocalTime duree) { this.duree = duree; }

    public LocalDate getDateActivite() { return dateActivite; }
    public void setDateActivite(LocalDate dateActivite) { this.dateActivite = dateActivite; }

    public int getCaloriesBrulees() { return caloriesBrulees; }
    public void setCaloriesBrulees(int caloriesBrulees) { this.caloriesBrulees = caloriesBrulees; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    @Override
    public String toString() {
        return "Activite{" +
                "id=" + id +
                ", userId=" + userId +
                ", type='" + type + '\'' +
                ", distance=" + distance +
                ", duree=" + duree +
                ", dateActivite=" + dateActivite +
                ", caloriesBrulees=" + caloriesBrulees +
                '}';
    }
}

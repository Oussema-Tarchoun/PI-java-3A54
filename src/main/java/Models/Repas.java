package Models;

import java.sql.Date;
import java.sql.Time;

public class Repas {

    private int id;
    private int user_id;
    private String nom;
    private Time heure;
    private int calories;
    private String description;
    private String type;
    private Date date;

    // Default Constructor
    public Repas() {
    }

    // Full Constructor (including id)
    public Repas(int id, int user_id, String nom, Time heure, int calories, String description, String type, Date date) {
        this.id = id;
        this.user_id = user_id;
        this.nom = nom;
        this.heure = heure;
        this.calories = calories;
        this.description = description;
        this.type = type;
        this.date = date;
    }

    // Constructor without id (useful for adding new entries to DB)
    public Repas(int user_id, String nom, Time heure, int calories, String description, String type, Date date) {
        this.user_id = user_id;
        this.nom = nom;
        this.heure = heure;
        this.calories = calories;
        this.description = description;
        this.type = type;
        this.date = date;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUser_id() {
        return user_id;
    }

    public void setUser_id(int user_id) {
        this.user_id = user_id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public Time getHeure() {
        return heure;
    }

    public void setHeure(Time heure) {
        this.heure = heure;
    }

    public int getCalories() {
        return calories;
    }

    public void setCalories(int calories) {
        this.calories = calories;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    @Override
    public String toString() {
        return "Repas{" +
                "id=" + id +
                ", user_id=" + user_id +
                ", nom='" + nom + '\'' +
                ", heure=" + heure +
                ", calories=" + calories +
                ", description='" + description + '\'' +
                ", type='" + type + '\'' +
                ", date=" + date +
                '}' + '\n';
    }
}
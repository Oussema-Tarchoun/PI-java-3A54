package Models;

import java.time.LocalDate;

public class Alimentation {
    private int id;
    private int userId;
    private String type; // petit-déjeuner, déjeuner, dîner, snack
    private String description;
    private int calories;
    private LocalDate dateConsommation;
    private String notes;

    public Alimentation() {}

    public Alimentation(int userId, String type, String description, int calories, LocalDate dateConsommation, String notes) {
        this.userId = userId;
        this.type = type;
        this.description = description;
        this.calories = calories;
        this.dateConsommation = dateConsommation;
        this.notes = notes;
    }

    public Alimentation(int id, int userId, String type, String description, int calories, LocalDate dateConsommation, String notes) {
        this(userId, type, description, calories, dateConsommation, notes);
        this.id = id;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getCalories() { return calories; }
    public void setCalories(int calories) { this.calories = calories; }

    public LocalDate getDateConsommation() { return dateConsommation; }
    public void setDateConsommation(LocalDate dateConsommation) { this.dateConsommation = dateConsommation; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    @Override
    public String toString() {
        return "Alimentation{" +
                "id=" + id +
                ", userId=" + userId +
                ", type='" + type + '\'' +
                ", description='" + description + '\'' +
                ", calories=" + calories +
                ", dateConsommation=" + dateConsommation +
                ", notes='" + notes + '\'' +
                '}';
    }
}

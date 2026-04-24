package Models;

import java.sql.Date;
import java.sql.Timestamp;

public class AnalyseNutritionnelle {
    private int id;
    private int userId;
    private Timestamp dateGeneration;
    private Date periodeDebut;
    private Date periodeFin;
    private int score;
    private String contenuJson; // stocké en String JSON

    public AnalyseNutritionnelle() {}

    // Getters/Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public Timestamp getDateGeneration() { return dateGeneration; }
    public void setDateGeneration(Timestamp dateGeneration) { this.dateGeneration = dateGeneration; }
    public Date getPeriodeDebut() { return periodeDebut; }
    public void setPeriodeDebut(Date periodeDebut) { this.periodeDebut = periodeDebut; }
    public Date getPeriodeFin() { return periodeFin; }
    public void setPeriodeFin(Date periodeFin) { this.periodeFin = periodeFin; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public String getContenuJson() { return contenuJson; }
    public void setContenuJson(String contenuJson) { this.contenuJson = contenuJson; }
}
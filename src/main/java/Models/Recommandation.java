package Models;

import java.util.Date;

public class Recommandation {
    private int id;
    private String titre;
    private String description;
    private String niveau_impact; // changed
    private Date date_generation; // changed
    private int energie_id; // changed

    // Constructors
    public Recommandation() {}

    public Recommandation(String titre, String description, String niveau_impact, Date date_generation, int energie_id) {
        this.titre = titre;
        this.description = description;
        this.niveau_impact = niveau_impact;
        this.date_generation = date_generation;
        this.energie_id = energie_id;
    }

    public Recommandation(int id, String titre, String description, String niveau_impact, Date date_generation, int energie_id) {
        this.id = id;
        this.titre = titre;
        this.description = description;
        this.niveau_impact = niveau_impact;
        this.date_generation = date_generation;
        this.energie_id = energie_id;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getNiveau_impact() { return niveau_impact; }
    public void setNiveau_impact(String niveau_impact) { this.niveau_impact = niveau_impact; }

    public Date getDate_generation() { return date_generation; }
    public void setDate_generation(Date date_generation) { this.date_generation = date_generation; }

    public int getEnergie_id() { return energie_id; }
    public void setEnergie_id(int energie_id) { this.energie_id = energie_id; }

    @Override
    public String toString() {
        return "Recommandation{id=" + id + ", titre='" + titre + "', impact='" + niveau_impact + "'}";
    }
}

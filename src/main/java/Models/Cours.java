package Models;

public class Cours {
    private int id;
    private String tittre;
    private String description;
    private String niveau;
    private int dureeEstimee;
    private String categorie;
    private String dateCreation;
    private String status;
    private int user_id;

    public Cours() {
    }

    public Cours(String tittre, String description, String niveau, int dureeEstimee,
                 String categorie, String status) {
        this.tittre = tittre;
        this.description = description;
        this.niveau = niveau;
        this.dureeEstimee = dureeEstimee;
        this.categorie = categorie;
        this.status = status;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTittre() { return tittre; }
    public void setTittre(String tittre) { this.tittre = tittre; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getNiveau() { return niveau; }
    public void setNiveau(String niveau) { this.niveau = niveau; }
    public int getDureeEstimee() { return dureeEstimee; }
    public void setDureeEstimee(int dureeEstimee) { this.dureeEstimee = dureeEstimee; }
    public String getCategorie() { return categorie; }
    public void setCategorie(String categorie) { this.categorie = categorie; }
    public String getDateCreation() { return dateCreation; }
    public void setDateCreation(String dateCreation) { this.dateCreation = dateCreation; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getUser_id() { return user_id; }
    public void setUser_id(int user_id) { this.user_id = user_id; }

    @Override
    public String toString() {
        return "Cours{" +
                "id=" + id +
                ", tittre='" + tittre + '\'' +
                ", description='" + description + '\'' +
                ", niveau='" + niveau + '\'' +
                ", dureeEstimee=" + dureeEstimee +
                ", categorie='" + categorie + '\'' +
                ", dateCreation='" + dateCreation + '\'' +
                ", status='" + status + '\'' +
                ", user_id=" + user_id +
                '}';
    }
}
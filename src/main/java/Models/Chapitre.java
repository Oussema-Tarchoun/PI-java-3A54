package Models;

public class Chapitre {
    private int id;
    private int idChapitre;
    private String titre;
    private String contenu;
    private int ordre;
    private String exercise;
    private int coursId;

    public Chapitre() {
    }

    public Chapitre(int idChapitre, String titre, String contenu, int ordre) {
        this.idChapitre = idChapitre;
        this.titre = titre;
        this.contenu = contenu;
        this.ordre = ordre;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getIdChapitre() { return idChapitre; }
    public void setIdChapitre(int idChapitre) { this.idChapitre = idChapitre; }
    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }
    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }
    public int getOrdre() { return ordre; }
    public void setOrdre(int ordre) { this.ordre = ordre; }
    public String getExercise() { return exercise; }
    public void setExercise(String exercise) { this.exercise = exercise; }
    public int getCoursId() { return coursId; }
    public void setCoursId(int coursId) { this.coursId = coursId; }

    @Override
    public String toString() {
        return "Chapitre{" +
                "id=" + id +
                ", idChapitre=" + idChapitre +
                ", titre='" + titre + '\'' +
                ", contenu='" + contenu + '\'' +
                ", ordre=" + ordre +
                ", exercise='" + exercise + '\'' +
                ", coursId=" + coursId +
                '}';
    }
}
package Models;

public class Aliment {
    private int id;
    private String nom;
    private double quantite;
    private double calories;
    private String macro;

    public Aliment() {
    }

    // Full constructor
    public Aliment(int id, String nom, double quantite, double calories, String macro) {
        this.id = id;
        this.nom = nom;
        this.quantite = quantite;
        this.calories = calories;
        this.macro = macro;
    }

    // Constructor without ID (for adding)
    public Aliment(String nom, double quantite, double calories, String macro) {
        this.nom = nom;
        this.quantite = quantite;
        this.calories = calories;
        this.macro = macro;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public double getQuantite() { return quantite; }
    public void setQuantite(double quantite) { this.quantite = quantite; }

    public double getCalories() { return calories; }
    public void setCalories(double calories) { this.calories = calories; }

    public String getMacro() { return macro; }
    public void setMacro(String macro) { this.macro = macro; }

    @Override
    public String toString() {
        return "Aliment{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", quantite=" + quantite +
                ", calories=" + calories +
                ", macro='" + macro + '\'' +
                '}' + '\n';
    }
}
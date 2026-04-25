package Models;

import java.util.Date;

public class Energie {
    private int id;
    private String type_energie;
    private float periode;
    private float valeur;
    private Date date_enregistrement;
    private String source;
    private String user_id; // Changé ici

    // Constructors
    public Energie() {}

    public Energie(String type_energie, float periode, float valeur, Date date_enregistrement, String source, String user_id) {
        this.type_energie = type_energie;
        this.periode = periode;
        this.valeur = valeur;
        this.date_enregistrement = date_enregistrement;
        this.source = source;
        this.user_id = user_id;
    }

    public Energie(int id, String type_energie, float periode, float valeur, Date date_enregistrement, String source, String user_id) {
        this.id = id;
        this.type_energie = type_energie;
        this.periode = periode;
        this.valeur = valeur;
        this.date_enregistrement = date_enregistrement;
        this.source = source;
        this.user_id = user_id;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getType_energie() { return type_energie; }
    public void setType_energie(String type_energie) { this.type_energie = type_energie; }

    public float getPeriode() { return periode; }
    public void setPeriode(float periode) { this.periode = periode; }

    public float getValeur() { return valeur; }
    public void setValeur(float valeur) { this.valeur = valeur; }

    public Date getDate_enregistrement() { return date_enregistrement; }
    public void setDate_enregistrement(Date date_enregistrement) { this.date_enregistrement = date_enregistrement; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getUser_id() { return user_id; }
    public void setUser_id(String user_id) { this.user_id = user_id; }

    @Override
    public String toString() {
        return "Energie{id=" + id + ", type='" + type_energie + "', user_id='" + user_id + "'}";
    }
}

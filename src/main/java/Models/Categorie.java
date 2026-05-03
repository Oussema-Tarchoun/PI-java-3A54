package Models;

import java.util.Date;

public class Categorie {

    private int    idCategorie;
    private String nomCategorie;
    private String description;
    private int    idUser;
    private double budgetMax;
    private Date   dateCreation;

    public Categorie() {}

    // Constructeur sans id (INSERT)
    public Categorie(String nomCategorie, String description, int idUser,
                     double budgetMax, Date dateCreation) {
        this.nomCategorie  = nomCategorie;
        this.description   = description;
        this.idUser        = idUser;
        this.budgetMax     = budgetMax;
        this.dateCreation  = dateCreation;
    }

    // Constructeur complet (UPDATE / SELECT)
    public Categorie(int idCategorie, String nomCategorie, String description,
                     int idUser, double budgetMax, Date dateCreation) {
        this.idCategorie   = idCategorie;
        this.nomCategorie  = nomCategorie;
        this.description   = description;
        this.idUser        = idUser;
        this.budgetMax     = budgetMax;
        this.dateCreation  = dateCreation;
    }

    public int    getIdCategorie()               { return idCategorie; }
    public void   setIdCategorie(int v)          { this.idCategorie = v; }

    public String getNomCategorie()              { return nomCategorie; }
    public void   setNomCategorie(String v)      { this.nomCategorie = v; }

    public String getDescription()               { return description; }
    public void   setDescription(String v)       { this.description = v; }

    public int    getIdUser()                    { return idUser; }
    public void   setIdUser(int v)               { this.idUser = v; }

    public double getBudgetMax()                 { return budgetMax; }
    public void   setBudgetMax(double v)         { this.budgetMax = v; }

    public Date   getDateCreation()              { return dateCreation; }
    public void   setDateCreation(Date v)        { this.dateCreation = v; }

    @Override
    public String toString() {
        return "Categorie{"
                + "idCategorie="     + idCategorie
                + ", nomCategorie='" + nomCategorie  + '\''
                + ", description='"  + description   + '\''
                + ", idUser="        + idUser
                + ", budgetMax="     + budgetMax
                + ", dateCreation="  + dateCreation
                + '}';
    }
}
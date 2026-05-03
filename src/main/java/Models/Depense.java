package Models;

import java.util.Date;

public class Depense {


    private int    idDepense;
    private String description;
    private double montant;
    private Date   dateDepense;
    private String statut;      // e.g. "En attente", "Validée", "Refusée"
    private int    idCategorie;




    public Depense() {}


    public Depense(String description, double montant, Date dateDepense,
                   String statut, int idCategorie) {
        this.description  = description;
        this.montant      = montant;
        this.dateDepense  = dateDepense;
        this.statut       = statut;
        this.idCategorie  = idCategorie;
    }


    public Depense(int idDepense, String description, double montant,
                   Date dateDepense, String statut, int idCategorie) {
        this.idDepense    = idDepense;
        this.description  = description;
        this.montant      = montant;
        this.dateDepense  = dateDepense;
        this.statut       = statut;
        this.idCategorie  = idCategorie;
    }



    public int getIdDepense() {
        return idDepense;
    }
    public void setIdDepense(int idDepense) {
        this.idDepense = idDepense;
    }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public double getMontant() {
        return montant;
    }
    public void setMontant(double montant) {
        this.montant = montant;
    }

    public Date getDateDepense() {
        return dateDepense;
    }
    public void setDateDepense(Date dateDepense) {
        this.dateDepense = dateDepense;
    }

    public String getStatut() {
        return statut;
    }
    public void setStatut(String statut) {
        this.statut = statut;
    }

    public int getIdCategorie() {
        return idCategorie;
    }
    public void setIdCategorie(int idCategorie) {
        this.idCategorie = idCategorie;
    }



    @Override
    public String toString() {
        return "Depense{"
                + "idDepense="    + idDepense
                + ", description='" + description + '\''
                + ", montant="      + montant
                + ", dateDepense="  + dateDepense
                + ", statut='"      + statut      + '\''
                + ", idCategorie="  + idCategorie
                + '}';
    }
}
package Models;

import java.time.LocalDate;

public class Finance {
    private int id;
    private int userId;
    private String categorie; // revenu, dépense
    private String description;
    private double montant;
    private LocalDate dateTransaction;
    private String typeTransaction; // salaire, nourriture, transport, etc
    private String notes;

    public Finance() {}

    public Finance(int userId, String categorie, String description, double montant, LocalDate dateTransaction, String typeTransaction, String notes) {
        this.userId = userId;
        this.categorie = categorie;
        this.description = description;
        this.montant = montant;
        this.dateTransaction = dateTransaction;
        this.typeTransaction = typeTransaction;
        this.notes = notes;
    }

    public Finance(int id, int userId, String categorie, String description, double montant, LocalDate dateTransaction, String typeTransaction, String notes) {
        this(userId, categorie, description, montant, dateTransaction, typeTransaction, notes);
        this.id = id;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getCategorie() { return categorie; }
    public void setCategorie(String categorie) { this.categorie = categorie; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getMontant() { return montant; }
    public void setMontant(double montant) { this.montant = montant; }

    public LocalDate getDateTransaction() { return dateTransaction; }
    public void setDateTransaction(LocalDate dateTransaction) { this.dateTransaction = dateTransaction; }

    public String getTypeTransaction() { return typeTransaction; }
    public void setTypeTransaction(String typeTransaction) { this.typeTransaction = typeTransaction; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    @Override
    public String toString() {
        return "Finance{" +
                "id=" + id +
                ", userId=" + userId +
                ", categorie='" + categorie + '\'' +
                ", description='" + description + '\'' +
                ", montant=" + montant +
                ", dateTransaction=" + dateTransaction +
                ", typeTransaction='" + typeTransaction + '\'' +
                '}';
    }
}

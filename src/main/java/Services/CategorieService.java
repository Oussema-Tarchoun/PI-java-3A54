package Services;

import Models.Categorie;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategorieService {

    private final Connection cnx = MyDatabase.getInstance().getConnection();

    public void ajouter(Categorie c) throws SQLException {
        String sql = "INSERT INTO categorie (nom_categorie, description, id_user, budget_max, date_creation) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getNomCategorie());
            ps.setString(2, c.getDescription());
            ps.setInt   (3, c.getIdUser());
            ps.setDouble(4, c.getBudgetMax());
            ps.setDate  (5, new java.sql.Date(c.getDateCreation().getTime()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) c.setIdCategorie(keys.getInt(1));
            }
            System.out.println("✅ Catégorie ajoutée id=" + c.getIdCategorie());
        }
    }

    public List<Categorie> afficher() throws SQLException {
        List<Categorie> list = new ArrayList<>();
        String sql = "SELECT id_categorie, nom_categorie, description, id_user, budget_max, date_creation FROM categorie";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    public Categorie getById(int id) throws SQLException {
        String sql = "SELECT id_categorie, nom_categorie, description, id_user, budget_max, date_creation "
                + "FROM categorie WHERE id_categorie = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        }
        return null;
    }

    public void modifier(Categorie c) throws SQLException {
        String sql = "UPDATE categorie SET nom_categorie=?, description=?, id_user=?, budget_max=?, date_creation=? "
                + "WHERE id_categorie=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, c.getNomCategorie());
            ps.setString(2, c.getDescription());
            ps.setInt   (3, c.getIdUser());
            ps.setDouble(4, c.getBudgetMax());
            ps.setDate  (5, new java.sql.Date(c.getDateCreation().getTime()));
            ps.setInt   (6, c.getIdCategorie());
            int rows = ps.executeUpdate();
            System.out.println(rows > 0 ? "✅ Modifiée id=" + c.getIdCategorie() : "⚠️ Introuvable");
        }
    }

    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM categorie WHERE id_categorie=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            int rows = ps.executeUpdate();
            System.out.println(rows > 0 ? "✅ Supprimée id=" + id : "⚠️ Introuvable");
        }
    }

    // Méthode utilitaire pour mapper un ResultSet → Categorie
    private Categorie map(ResultSet rs) throws SQLException {
        return new Categorie(
                rs.getInt   ("id_categorie"),
                rs.getString("nom_categorie"),
                rs.getString("description"),
                rs.getInt   ("id_user"),
                rs.getDouble("budget_max"),
                rs.getDate  ("date_creation")
        );
    }
}
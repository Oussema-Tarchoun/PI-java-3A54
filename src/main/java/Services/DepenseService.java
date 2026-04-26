package Services;

import Models.Depense;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DepenseService {

    private Connection cnx() {
        return MyDatabase.getInstance().getConnection();
    }

    public void ajouter(Depense d) throws SQLException {
        String sql = "INSERT INTO depense (description, montant, date_depense, statut, id_categorie) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, d.getDescription());
            ps.setDouble(2, d.getMontant());
            ps.setDate  (3, new java.sql.Date(d.getDateDepense().getTime()));
            ps.setString(4, d.getStatut());
            ps.setInt   (5, d.getIdCategorie());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) d.setIdDepense(keys.getInt(1));
            }
            System.out.println("✅ Dépense ajoutée avec id=" + d.getIdDepense());
        }
    }

    public List<Depense> afficher() throws SQLException {
        List<Depense> list = new ArrayList<>();
        String sql = "SELECT id_depense, description, montant, date_depense, statut, id_categorie "
                + "FROM depense";
        try (Statement st = cnx().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Depense(
                        rs.getInt   ("id_depense"),
                        rs.getString("description"),
                        rs.getDouble("montant"),
                        rs.getDate  ("date_depense"),
                        rs.getString("statut"),
                        rs.getInt   ("id_categorie")
                ));
            }
        }
        return list;
    }

    public Depense getById(int id) throws SQLException {
        String sql = "SELECT id_depense, description, montant, date_depense, statut, id_categorie "
                + "FROM depense WHERE id_depense = ?";
        try (PreparedStatement ps = cnx().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Depense(
                            rs.getInt   ("id_depense"),
                            rs.getString("description"),
                            rs.getDouble("montant"),
                            rs.getDate  ("date_depense"),
                            rs.getString("statut"),
                            rs.getInt   ("id_categorie")
                    );
                }
            }
        }
        return null;
    }

    public List<Depense> getByCategorie(int idCategorie) throws SQLException {
        List<Depense> list = new ArrayList<>();
        String sql = "SELECT id_depense, description, montant, date_depense, statut, id_categorie "
                + "FROM depense WHERE id_categorie = ?";
        try (PreparedStatement ps = cnx().prepareStatement(sql)) {
            ps.setInt(1, idCategorie);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Depense(
                            rs.getInt   ("id_depense"),
                            rs.getString("description"),
                            rs.getDouble("montant"),
                            rs.getDate  ("date_depense"),
                            rs.getString("statut"),
                            rs.getInt   ("id_categorie")
                    ));
                }
            }
        }
        return list;
    }

    public void modifier(Depense d) throws SQLException {
        String sql = "UPDATE depense "
                + "SET description = ?, montant = ?, date_depense = ?, statut = ?, id_categorie = ? "
                + "WHERE id_depense = ?";
        try (PreparedStatement ps = cnx().prepareStatement(sql)) {
            ps.setString(1, d.getDescription());
            ps.setDouble(2, d.getMontant());
            ps.setDate  (3, new java.sql.Date(d.getDateDepense().getTime()));
            ps.setString(4, d.getStatut());
            ps.setInt   (5, d.getIdCategorie());
            ps.setInt   (6, d.getIdDepense());
            int rows = ps.executeUpdate();
            if (rows > 0)
                System.out.println("✅ Dépense modifiée (id=" + d.getIdDepense() + ")");
            else
                System.out.println("⚠️  Aucune dépense trouvée avec id=" + d.getIdDepense());
        }
    }

    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM depense WHERE id_depense = ?";
        try (PreparedStatement ps = cnx().prepareStatement(sql)) {
            ps.setInt(1, id);
            int rows = ps.executeUpdate();
            if (rows > 0)
                System.out.println("✅ Dépense supprimée (id=" + id + ")");
            else
                System.out.println("⚠️  Aucune dépense trouvée avec id=" + id);
        }
    }
}
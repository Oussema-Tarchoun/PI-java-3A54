package Services;

import Models.Finance;
import utils.MyDatabase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceFinance {
    private Connection connection;

    public ServiceFinance() {
        connection = MyDatabase.getInstance().getConnection();
    }

    public void ajouter(Finance finance) throws SQLException {
        String sql = "INSERT INTO finance (user_id, categorie, description, montant, date_transaction, type_transaction, notes) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, finance.getUserId());
            ps.setString(2, finance.getCategorie());
            ps.setString(3, finance.getDescription());
            ps.setDouble(4, finance.getMontant());
            ps.setDate(5, Date.valueOf(finance.getDateTransaction()));
            ps.setString(6, finance.getTypeTransaction());
            ps.setString(7, finance.getNotes());
            ps.executeUpdate();
        }
    }

    public void modifier(Finance finance) throws SQLException {
        String sql = "UPDATE finance SET categorie=?, description=?, montant=?, date_transaction=?, type_transaction=?, notes=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, finance.getCategorie());
            ps.setString(2, finance.getDescription());
            ps.setDouble(3, finance.getMontant());
            ps.setDate(4, Date.valueOf(finance.getDateTransaction()));
            ps.setString(5, finance.getTypeTransaction());
            ps.setString(6, finance.getNotes());
            ps.setInt(7, finance.getId());
            ps.executeUpdate();
        }
    }

    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM finance WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public Finance obtenirParId(int id) throws SQLException {
        String sql = "SELECT * FROM finance WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Finance(
                    rs.getInt("id"),
                    rs.getInt("user_id"),
                    rs.getString("categorie"),
                    rs.getString("description"),
                    rs.getDouble("montant"),
                    rs.getDate("date_transaction").toLocalDate(),
                    rs.getString("type_transaction"),
                    rs.getString("notes")
                );
            }
        }
        return null;
    }

    public List<Finance> obtenirParUtilisateur(int userId) throws SQLException {
        String sql = "SELECT * FROM finance WHERE user_id=? ORDER BY date_transaction DESC";
        List<Finance> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Finance(
                    rs.getInt("id"),
                    rs.getInt("user_id"),
                    rs.getString("categorie"),
                    rs.getString("description"),
                    rs.getDouble("montant"),
                    rs.getDate("date_transaction").toLocalDate(),
                    rs.getString("type_transaction"),
                    rs.getString("notes")
                ));
            }
        }
        return list;
    }

    public List<Finance> obtenirTous() throws SQLException {
        String sql = "SELECT * FROM finance ORDER BY date_transaction DESC";
        List<Finance> list = new ArrayList<>();
        try (Statement st = connection.createStatement()) {
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                list.add(new Finance(
                    rs.getInt("id"),
                    rs.getInt("user_id"),
                    rs.getString("categorie"),
                    rs.getString("description"),
                    rs.getDouble("montant"),
                    rs.getDate("date_transaction").toLocalDate(),
                    rs.getString("type_transaction"),
                    rs.getString("notes")
                ));
            }
        }
        return list;
    }
}

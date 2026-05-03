package Services;

import Models.Energie;
import utils.MyDatabase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceEnergie {
    private Connection connection;

    public ServiceEnergie() {
        connection = MyDatabase.getInstance().getConnection();
    }

    public void ajouter(Energie energie) throws SQLException {
        String sql = "INSERT INTO energie (user_id, type, consommation, cout, date_consommation, notes) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, energie.getUserId());
            ps.setString(2, energie.getType());
            ps.setDouble(3, energie.getConsommation());
            ps.setDouble(4, energie.getCout());
            ps.setDate(5, Date.valueOf(energie.getDateConsommation()));
            ps.setString(6, energie.getNotes());
            ps.executeUpdate();
        }
    }

    public void modifier(Energie energie) throws SQLException {
        String sql = "UPDATE energie SET type=?, consommation=?, cout=?, date_consommation=?, notes=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, energie.getType());
            ps.setDouble(2, energie.getConsommation());
            ps.setDouble(3, energie.getCout());
            ps.setDate(4, Date.valueOf(energie.getDateConsommation()));
            ps.setString(5, energie.getNotes());
            ps.setInt(6, energie.getId());
            ps.executeUpdate();
        }
    }

    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM energie WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public Energie obtenirParId(int id) throws SQLException {
        String sql = "SELECT * FROM energie WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Energie(
                    rs.getInt("id"),
                    rs.getInt("user_id"),
                    rs.getString("type"),
                    rs.getDouble("consommation"),
                    rs.getDouble("cout"),
                    rs.getDate("date_consommation").toLocalDate(),
                    rs.getString("notes")
                );
            }
        }
        return null;
    }

    public List<Energie> obtenirParUtilisateur(int userId) throws SQLException {
        String sql = "SELECT * FROM energie WHERE user_id=? ORDER BY date_consommation DESC";
        List<Energie> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Energie(
                    rs.getInt("id"),
                    rs.getInt("user_id"),
                    rs.getString("type"),
                    rs.getDouble("consommation"),
                    rs.getDouble("cout"),
                    rs.getDate("date_consommation").toLocalDate(),
                    rs.getString("notes")
                ));
            }
        }
        return list;
    }

    public List<Energie> obtenirTous() throws SQLException {
        String sql = "SELECT * FROM energie ORDER BY date_consommation DESC";
        List<Energie> list = new ArrayList<>();
        try (Statement st = connection.createStatement()) {
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                list.add(new Energie(
                    rs.getInt("id"),
                    rs.getInt("user_id"),
                    rs.getString("type"),
                    rs.getDouble("consommation"),
                    rs.getDouble("cout"),
                    rs.getDate("date_consommation").toLocalDate(),
                    rs.getString("notes")
                ));
            }
        }
        return list;
    }
}

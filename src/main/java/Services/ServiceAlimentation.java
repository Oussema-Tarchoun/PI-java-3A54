package Services;

import Models.Alimentation;
import utils.MyDatabase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceAlimentation {
    private Connection connection;

    public ServiceAlimentation() {
        connection = MyDatabase.getInstance().getConnection();
    }

    public void ajouter(Alimentation alimentation) throws SQLException {
        String sql = "INSERT INTO alimentation (user_id, type, description, calories, date_consommation, notes) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, alimentation.getUserId());
            ps.setString(2, alimentation.getType());
            ps.setString(3, alimentation.getDescription());
            ps.setInt(4, alimentation.getCalories());
            ps.setDate(5, Date.valueOf(alimentation.getDateConsommation()));
            ps.setString(6, alimentation.getNotes());
            ps.executeUpdate();
        }
    }

    public void modifier(Alimentation alimentation) throws SQLException {
        String sql = "UPDATE alimentation SET type=?, description=?, calories=?, date_consommation=?, notes=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, alimentation.getType());
            ps.setString(2, alimentation.getDescription());
            ps.setInt(3, alimentation.getCalories());
            ps.setDate(4, Date.valueOf(alimentation.getDateConsommation()));
            ps.setString(5, alimentation.getNotes());
            ps.setInt(6, alimentation.getId());
            ps.executeUpdate();
        }
    }

    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM alimentation WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public Alimentation obtenirParId(int id) throws SQLException {
        String sql = "SELECT * FROM alimentation WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Alimentation(
                    rs.getInt("id"),
                    rs.getInt("user_id"),
                    rs.getString("type"),
                    rs.getString("description"),
                    rs.getInt("calories"),
                    rs.getDate("date_consommation").toLocalDate(),
                    rs.getString("notes")
                );
            }
        }
        return null;
    }

    public List<Alimentation> obtenirParUtilisateur(int userId) throws SQLException {
        String sql = "SELECT * FROM alimentation WHERE user_id=? ORDER BY date_consommation DESC";
        List<Alimentation> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Alimentation(
                    rs.getInt("id"),
                    rs.getInt("user_id"),
                    rs.getString("type"),
                    rs.getString("description"),
                    rs.getInt("calories"),
                    rs.getDate("date_consommation").toLocalDate(),
                    rs.getString("notes")
                ));
            }
        }
        return list;
    }

    public List<Alimentation> obtenirTous() throws SQLException {
        String sql = "SELECT * FROM alimentation ORDER BY date_consommation DESC";
        List<Alimentation> list = new ArrayList<>();
        try (Statement st = connection.createStatement()) {
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                list.add(new Alimentation(
                    rs.getInt("id"),
                    rs.getInt("user_id"),
                    rs.getString("type"),
                    rs.getString("description"),
                    rs.getInt("calories"),
                    rs.getDate("date_consommation").toLocalDate(),
                    rs.getString("notes")
                ));
            }
        }
        return list;
    }
}

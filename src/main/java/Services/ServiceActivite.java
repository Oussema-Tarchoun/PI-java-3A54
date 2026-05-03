package Services;

import Models.Activite;
import utils.MyDatabase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceActivite {
    private Connection connection;

    public ServiceActivite() {
        connection = MyDatabase.getInstance().getConnection();
    }

    public void ajouter(Activite activite) throws SQLException {
        String sql = "INSERT INTO activite (user_id, type, distance, duree, date_activite, calories_brulees, notes) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, activite.getUserId());
            ps.setString(2, activite.getType());
            ps.setDouble(3, activite.getDistance());
            ps.setTime(4, Time.valueOf(activite.getDuree()));
            ps.setDate(5, Date.valueOf(activite.getDateActivite()));
            ps.setInt(6, activite.getCaloriesBrulees());
            ps.setString(7, activite.getNotes());
            ps.executeUpdate();
        }
    }

    public void modifier(Activite activite) throws SQLException {
        String sql = "UPDATE activite SET type=?, distance=?, duree=?, date_activite=?, calories_brulees=?, notes=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, activite.getType());
            ps.setDouble(2, activite.getDistance());
            ps.setTime(3, Time.valueOf(activite.getDuree()));
            ps.setDate(4, Date.valueOf(activite.getDateActivite()));
            ps.setInt(5, activite.getCaloriesBrulees());
            ps.setString(6, activite.getNotes());
            ps.setInt(7, activite.getId());
            ps.executeUpdate();
        }
    }

    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM activite WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public Activite obtenirParId(int id) throws SQLException {
        String sql = "SELECT * FROM activite WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Activite(
                    rs.getInt("id"),
                    rs.getInt("user_id"),
                    rs.getString("type"),
                    rs.getDouble("distance"),
                    rs.getTime("duree").toLocalTime(),
                    rs.getDate("date_activite").toLocalDate(),
                    rs.getInt("calories_brulees"),
                    rs.getString("notes")
                );
            }
        }
        return null;
    }

    public List<Activite> obtenirParUtilisateur(int userId) throws SQLException {
        String sql = "SELECT * FROM activite WHERE user_id=? ORDER BY date_activite DESC";
        List<Activite> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Activite(
                    rs.getInt("id"),
                    rs.getInt("user_id"),
                    rs.getString("type"),
                    rs.getDouble("distance"),
                    rs.getTime("duree").toLocalTime(),
                    rs.getDate("date_activite").toLocalDate(),
                    rs.getInt("calories_brulees"),
                    rs.getString("notes")
                ));
            }
        }
        return list;
    }

    public List<Activite> obtenirTous() throws SQLException {
        String sql = "SELECT * FROM activite ORDER BY date_activite DESC";
        List<Activite> list = new ArrayList<>();
        try (Statement st = connection.createStatement()) {
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                list.add(new Activite(
                    rs.getInt("id"),
                    rs.getInt("user_id"),
                    rs.getString("type"),
                    rs.getDouble("distance"),
                    rs.getTime("duree").toLocalTime(),
                    rs.getDate("date_activite").toLocalDate(),
                    rs.getInt("calories_brulees"),
                    rs.getString("notes")
                ));
            }
        }
        return list;
    }
}

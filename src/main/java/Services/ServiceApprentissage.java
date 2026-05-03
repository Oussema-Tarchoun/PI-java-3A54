package Services;

import Models.Apprentissage;
import utils.MyDatabase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceApprentissage {
    private Connection connection;

    public ServiceApprentissage() {
        connection = MyDatabase.getInstance().getConnection();
    }

    public void ajouter(Apprentissage apprentissage) throws SQLException {
        String sql = "INSERT INTO apprentissage (user_id, sujet, titre, description, heures_etudiees, niveau, date_debut, date_fin_estimee, statut, notes) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, apprentissage.getUserId());
            ps.setString(2, apprentissage.getSujet());
            ps.setString(3, apprentissage.getTitre());
            ps.setString(4, apprentissage.getDescription());
            ps.setInt(5, apprentissage.getHeuresEtudiees());
            ps.setString(6, apprentissage.getNiveau());
            ps.setDate(7, Date.valueOf(apprentissage.getDateDebut()));
            ps.setDate(8, Date.valueOf(apprentissage.getDateFinEstimee()));
            ps.setString(9, apprentissage.getStatut());
            ps.setString(10, apprentissage.getNotes());
            ps.executeUpdate();
        }
    }

    public void modifier(Apprentissage apprentissage) throws SQLException {
        String sql = "UPDATE apprentissage SET sujet=?, titre=?, description=?, heures_etudiees=?, niveau=?, date_debut=?, date_fin_estimee=?, statut=?, notes=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, apprentissage.getSujet());
            ps.setString(2, apprentissage.getTitre());
            ps.setString(3, apprentissage.getDescription());
            ps.setInt(4, apprentissage.getHeuresEtudiees());
            ps.setString(5, apprentissage.getNiveau());
            ps.setDate(6, Date.valueOf(apprentissage.getDateDebut()));
            ps.setDate(7, Date.valueOf(apprentissage.getDateFinEstimee()));
            ps.setString(8, apprentissage.getStatut());
            ps.setString(9, apprentissage.getNotes());
            ps.setInt(10, apprentissage.getId());
            ps.executeUpdate();
        }
    }

    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM apprentissage WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public Apprentissage obtenirParId(int id) throws SQLException {
        String sql = "SELECT * FROM apprentissage WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Apprentissage(
                    rs.getInt("id"),
                    rs.getInt("user_id"),
                    rs.getString("sujet"),
                    rs.getString("titre"),
                    rs.getString("description"),
                    rs.getInt("heures_etudiees"),
                    rs.getString("niveau"),
                    rs.getDate("date_debut").toLocalDate(),
                    rs.getDate("date_fin_estimee").toLocalDate(),
                    rs.getString("statut"),
                    rs.getString("notes")
                );
            }
        }
        return null;
    }

    public List<Apprentissage> obtenirParUtilisateur(int userId) throws SQLException {
        String sql = "SELECT * FROM apprentissage WHERE user_id=? ORDER BY date_debut DESC";
        List<Apprentissage> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Apprentissage(
                    rs.getInt("id"),
                    rs.getInt("user_id"),
                    rs.getString("sujet"),
                    rs.getString("titre"),
                    rs.getString("description"),
                    rs.getInt("heures_etudiees"),
                    rs.getString("niveau"),
                    rs.getDate("date_debut").toLocalDate(),
                    rs.getDate("date_fin_estimee").toLocalDate(),
                    rs.getString("statut"),
                    rs.getString("notes")
                ));
            }
        }
        return list;
    }

    public List<Apprentissage> obtenirTous() throws SQLException {
        String sql = "SELECT * FROM apprentissage ORDER BY date_debut DESC";
        List<Apprentissage> list = new ArrayList<>();
        try (Statement st = connection.createStatement()) {
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                list.add(new Apprentissage(
                    rs.getInt("id"),
                    rs.getInt("user_id"),
                    rs.getString("sujet"),
                    rs.getString("titre"),
                    rs.getString("description"),
                    rs.getInt("heures_etudiees"),
                    rs.getString("niveau"),
                    rs.getDate("date_debut").toLocalDate(),
                    rs.getDate("date_fin_estimee").toLocalDate(),
                    rs.getString("statut"),
                    rs.getString("notes")
                ));
            }
        }
        return list;
    }
}

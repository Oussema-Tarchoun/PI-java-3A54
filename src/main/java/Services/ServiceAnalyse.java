package Services;

import Models.Repas;
import Models.AnalyseNutritionnelle;
import utils.MyDatabase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceAnalyse {

    private Connection getConnection() {
        return MyDatabase.getInstance().getConnection();
    }

    public void sauvegarder(AnalyseNutritionnelle a) throws SQLException {
        String sql = "INSERT INTO analyse_nutritionnelle (user_id, date_generation, periode_debut, periode_fin, score, contenu_json) VALUES (?,?,?,?,?,?)";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, a.getUserId());
            ps.setTimestamp(2, a.getDateGeneration());
            ps.setDate(3, a.getPeriodeDebut());
            ps.setDate(4, a.getPeriodeFin());
            ps.setInt(5, a.getScore());
            ps.setString(6, a.getContenuJson());
            ps.executeUpdate();
        }
    }

    public List<AnalyseNutritionnelle> getHistorique(int userId) throws SQLException {
        List<AnalyseNutritionnelle> list = new ArrayList<>();
        String sql = "SELECT * FROM analyse_nutritionnelle WHERE user_id = ? ORDER BY date_generation DESC";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                AnalyseNutritionnelle a = new AnalyseNutritionnelle();
                a.setId(rs.getInt("id"));
                a.setUserId(rs.getInt("user_id"));
                a.setDateGeneration(rs.getTimestamp("date_generation"));
                a.setPeriodeDebut(rs.getDate("periode_debut"));
                a.setPeriodeFin(rs.getDate("periode_fin"));
                a.setScore(rs.getInt("score"));
                a.setContenuJson(rs.getString("contenu_json"));
                list.add(a);
            }
        }
        return list;
    }

    public List<Repas> getRepas7Jours(int userId) throws SQLException {
        List<Repas> list = new ArrayList<>();
        String sql = "SELECT * FROM repas WHERE user_id = ? AND date >= DATE_SUB(CURDATE(), INTERVAL 7 DAY) ORDER BY date ASC";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Repas r = new Repas();
                r.setId(rs.getInt("id"));
                r.setNom(rs.getString("nom"));
                r.setCalories(rs.getInt("calories"));
                r.setType(rs.getString("type"));
                r.setDate(rs.getDate("date"));
                r.setHeure(rs.getTime("heure"));
                list.add(r);
            }
        }
        return list;
    }
}
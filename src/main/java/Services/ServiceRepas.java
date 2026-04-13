package Services;

import Models.Repas;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceRepas implements Iservice<Repas> {

    private Connection getConnection() {
        return MyDatabase.getInstance().getConnection();
    }

    @Override
    public void ajouter(Repas repas) throws SQLDataException {
        String sql = "INSERT INTO repas (user_id, nom, heure, calories, description, type, date) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, repas.getUser_id());
            ps.setString(2, repas.getNom());
            ps.setTime(3, repas.getHeure());
            ps.setInt(4, repas.getCalories());
            ps.setString(5, repas.getDescription());
            ps.setString(6, repas.getType());
            ps.setDate(7, repas.getDate());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("ajouter error: " + e.getMessage());
        }
    }

    @Override
    public void supprimer(Repas repas) throws SQLDataException {
        String sql = "DELETE FROM repas WHERE id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, repas.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("supprimer error: " + e.getMessage());
        }
    }

    @Override
    public void modifier(Repas repas) throws SQLDataException {
        String sql = "UPDATE repas SET user_id=?, nom=?, heure=?, calories=?, description=?, type=?, date=? WHERE id=?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, repas.getUser_id());
            ps.setString(2, repas.getNom());
            ps.setTime(3, repas.getHeure());
            ps.setInt(4, repas.getCalories());
            ps.setString(5, repas.getDescription());
            ps.setString(6, repas.getType());
            ps.setDate(7, repas.getDate());
            ps.setInt(8, repas.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("modifier error: " + e.getMessage());
        }
    }

    @Override
    public List<Repas> recuperer() throws SQLDataException {
        List<Repas> list = new ArrayList<>();
        String sql = "SELECT * FROM repas";
        try (Statement st = getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Repas r = new Repas();
                r.setId(rs.getInt("id"));
                r.setUser_id(rs.getInt("user_id"));
                r.setNom(rs.getString("nom"));
                r.setHeure(rs.getTime("heure"));
                r.setCalories(rs.getInt("calories"));
                r.setDescription(rs.getString("description"));
                r.setType(rs.getString("type"));
                r.setDate(rs.getDate("date"));
                list.add(r);
            }
        } catch (SQLException e) {
            System.out.println("recuperer error: " + e.getMessage());
        }
        return list;
    }

    // ── Junction table helpers ──

    public List<Integer> getLinkedAlimentIds(int repasId) {
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT aliment_id FROM repas_aliment WHERE repas_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, repasId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) ids.add(rs.getInt("aliment_id"));
        } catch (SQLException e) {
            System.out.println("getLinkedAlimentIds error: " + e.getMessage());
        }
        return ids;
    }

    public void deleteRepasAliments(int repasId) throws SQLException {
        String sql = "DELETE FROM repas_aliment WHERE repas_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, repasId);
            ps.executeUpdate();
        }
    }

    public void insertRepasAliment(int repasId, int alimentId) throws SQLException {
        String sql = "INSERT INTO repas_aliment (repas_id, aliment_id) VALUES (?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, repasId);
            ps.setInt(2, alimentId);
            ps.executeUpdate();
        }
    }

    // ── Used by AlimentController to show repas linked to an aliment ──

    public List<Repas> getRepasForAliment(int alimentId) {
        List<Repas> list = new ArrayList<>();
        String sql = "SELECT r.* FROM repas r " +
                "JOIN repas_aliment ra ON r.id = ra.repas_id " +
                "WHERE ra.aliment_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, alimentId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Repas r = new Repas();
                r.setId(rs.getInt("id"));
                r.setUser_id(rs.getInt("user_id"));
                r.setNom(rs.getString("nom"));
                r.setHeure(rs.getTime("heure"));
                r.setCalories(rs.getInt("calories"));
                r.setDescription(rs.getString("description"));
                r.setType(rs.getString("type"));
                r.setDate(rs.getDate("date"));
                list.add(r);
            }
        } catch (SQLException e) {
            System.out.println("getRepasForAliment error: " + e.getMessage());
        }
        return list;
    }
}

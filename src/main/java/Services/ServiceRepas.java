package Services;

import Models.Repas;
import Models.Aliment;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceRepas implements Iservice<Repas> {

    private Connection getConnection() {
        return MyDatabase.getInstance().getConnection();
    }

    // ───────── EXISTING METHODS ─────────

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

    public void deleteRepasAliments(int repasId) throws SQLException {
        String sql = "DELETE FROM repas_aliment WHERE repas_id = ?";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, repasId);
            ps.executeUpdate();
        }
    }

    public List<Integer> getLinkedAlimentIds(int repasId) {
        List<Integer> ids = new ArrayList<>();

        String sql = "SELECT aliment_id FROM repas_aliment WHERE repas_id = ?";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, repasId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                ids.add(rs.getInt("aliment_id"));
            }

        } catch (SQLException e) {
            System.out.println("getLinkedAlimentIds error: " + e.getMessage());
        }

        return ids;
    }

    public List<Repas> getRepasForAliment(int alimentId) throws SQLException {
        List<Repas> list = new ArrayList<>();

        String sql = "SELECT r.* " +
                "FROM repas r " +
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
                r.setCalories(rs.getInt("calories"));
                r.setType(rs.getString("type"));
                r.setDate(rs.getDate("date"));
                r.setHeure(rs.getTime("heure"));
                r.setDescription(rs.getString("description"));
                list.add(r);
            }
        }

        return list;
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
                r.setCalories(rs.getInt("calories"));
                r.setDescription(rs.getString("description"));
                r.setType(rs.getString("type"));
                r.setDate(rs.getDate("date"));
                r.setHeure(rs.getTime("heure"));
                list.add(r);
            }

        } catch (SQLException e) {
            System.out.println("recuperer error: " + e.getMessage());
        }
        return list;
    }

    public List<Repas> recupererParUser(int userId) throws SQLDataException {
        List<Repas> list = new ArrayList<>();
        String sql = "SELECT * FROM repas WHERE user_id = ?";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Repas r = new Repas();
                    r.setId(rs.getInt("id"));
                    r.setUser_id(rs.getInt("user_id"));
                    r.setNom(rs.getString("nom"));
                    r.setCalories(rs.getInt("calories"));
                    r.setDescription(rs.getString("description"));
                    r.setType(rs.getString("type"));
                    r.setDate(rs.getDate("date"));
                    r.setHeure(rs.getTime("heure"));
                    list.add(r);
                }
            }
        } catch (SQLException e) {
            System.out.println("recupererParUser error: " + e.getMessage());
        }
        return list;
    }

    public void insertRepasAliment(int repasId, int alimentId) throws SQLException {
        String sql = "INSERT INTO repas_aliment (repas_id, aliment_id) VALUES (?, ?)";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, repasId);
            ps.setInt(2, alimentId);
            ps.executeUpdate();
        }
    }

    // ───────── NEW METHOD (CHATBOT) ─────────

    public void addWithAliments(Repas repas, List<Aliment> aliments) throws SQLException {

        Connection conn = getConnection();
        conn.setAutoCommit(false);

        try {
            // 1. INSERT repas (FIXED: include heure)
            String sql = "INSERT INTO repas (user_id, nom, heure, calories, description, type, date) VALUES (?, ?, ?, ?, ?, ?, ?)";

            int repasId;

            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                ps.setInt(1, repas.getUser_id());
                ps.setString(2, repas.getNom());

                // ✅ FIX HERE (IMPORTANT)
                ps.setTime(3, repas.getHeure() != null
                        ? repas.getHeure()
                        : Time.valueOf("12:00:00"));

                ps.setInt(4, repas.getCalories());
                ps.setString(5, repas.getDescription());
                ps.setString(6, repas.getType());
                ps.setDate(7, repas.getDate());

                ps.executeUpdate();

                ResultSet rs = ps.getGeneratedKeys();
                if (!rs.next())
                    throw new SQLException("No ID generated");

                repasId = rs.getInt(1);
            }

            // 2. link aliments
            for (Aliment a : aliments) {
                insertRepasAliment(repasId, a.getId());
            }

            conn.commit();

        } catch (Exception e) {
            conn.rollback();
            throw new SQLException(e);
        } finally {
            conn.setAutoCommit(true);
        }
    }
}
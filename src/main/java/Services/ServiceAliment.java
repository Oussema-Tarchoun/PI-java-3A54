package Services;

import Models.Aliment;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceAliment implements Iservice<Aliment> {

    private Connection getConnection() {
        return MyDatabase.getInstance().getConnection();
    }

    // ───────── EXISTING METHODS ─────────
    @Override
    public void supprimer(Aliment aliment) throws SQLDataException {
        String sql = "DELETE FROM aliment WHERE id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, aliment.getId());
            ps.executeUpdate();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }


    @Override
    public void modifier(Aliment aliment) throws SQLDataException {
        String sql = "UPDATE aliment SET nom=?, quantite=?, calories=?, macro=? WHERE id=?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, aliment.getNom());
            ps.setDouble(2, aliment.getQuantite());
            ps.setDouble(3, aliment.getCalories());
            ps.setString(4, aliment.getMacro());
            ps.setInt(5, aliment.getId());
            ps.executeUpdate();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }




    @Override
    public void ajouter(Aliment aliment) throws SQLDataException {
        String sql = "INSERT INTO aliment (nom, quantite, calories, macro) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, aliment.getNom());
            ps.setDouble(2, aliment.getQuantite());
            ps.setDouble(3, aliment.getCalories());
            ps.setString(4, aliment.getMacro());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("ajouter aliment error: " + e.getMessage());
        }
    }

    @Override
    public List<Aliment> recuperer() throws SQLDataException {
        List<Aliment> list = new ArrayList<>();
        String sql = "SELECT * FROM aliment";
        try (Statement st = getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Aliment a = new Aliment();
                a.setId(rs.getInt("id"));
                a.setNom(rs.getString("nom"));
                a.setQuantite(rs.getDouble("quantite"));
                a.setCalories(rs.getDouble("calories"));
                a.setMacro(rs.getString("macro"));
                list.add(a);
            }
        } catch (SQLException e) {
            System.out.println("recuperer aliment error: " + e.getMessage());
        }
        return list;
    }




    // ───────── NEW METHOD (CHATBOT) ─────────

    public Aliment findByNameOrCreate(String name, int calories, String quantity) {
        String searchSql = "SELECT * FROM aliment WHERE LOWER(nom) = LOWER(?) LIMIT 1";
        try (PreparedStatement ps = getConnection().prepareStatement(searchSql)) {
            ps.setString(1, name.trim());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Aliment a = new Aliment();
                a.setId(rs.getInt("id"));
                a.setNom(rs.getString("nom"));
                a.setCalories(rs.getDouble("calories"));
                a.setQuantite(rs.getDouble("quantite"));
                return a;
            }
        } catch (SQLException e) { System.out.println("find error: " + e.getMessage()); }

        // CREATE avec RETURN_GENERATED_KEYS
        String insertSql = "INSERT INTO aliment (nom, quantite, calories, macro) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            double qty = 100;
            try { String num = quantity.replaceAll("[^0-9.]", ""); if (!num.isEmpty()) qty = Double.parseDouble(num); } catch (Exception ignored) {}

            ps.setString(1, name.trim());
            ps.setDouble(2, qty);
            ps.setDouble(3, calories);
            ps.setString(4, "");
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                Aliment a = new Aliment();
                a.setId(rs.getInt(1));
                a.setNom(name.trim());
                a.setCalories(calories);
                a.setQuantite(qty);
                return a;
            }
        } catch (SQLException e) { System.out.println("create error: " + e.getMessage()); }
        return null;
    }
}
package Services;

import Models.Aliment;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceAliment implements Iservice<Aliment> {

    // NO stored connection field — always fetch fresh like ServiceRepas does
    private Connection getConnection() {
        return MyDatabase.getInstance().getConnection();
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
    public void supprimer(Aliment aliment) throws SQLDataException {
        String sql = "DELETE FROM aliment WHERE id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, aliment.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("supprimer aliment error: " + e.getMessage());
        }
    }

    @Override
    public void modifier(Aliment aliment) throws SQLDataException {
        String sql = "UPDATE aliment SET nom = ?, quantite = ?, calories = ?, macro = ? WHERE id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, aliment.getNom());
            ps.setDouble(2, aliment.getQuantite());
            ps.setDouble(3, aliment.getCalories());
            ps.setString(4, aliment.getMacro());
            ps.setInt(5, aliment.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("modifier aliment error: " + e.getMessage());
        }
    }

    @Override
    public List<Aliment> recuperer() throws SQLDataException {
        String sql = "SELECT * FROM aliment";
        List<Aliment> list = new ArrayList<>();
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
}
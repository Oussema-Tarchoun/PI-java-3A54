package Services;

import Models.Activite;
import utils.MyDatabase;

import java.sql.*;
import java.sql.SQLDataException;
import java.util.ArrayList;
import java.util.List;

public class ServiceActivite implements Iservice<Activite> {

    private final Connection cnx = MyDatabase.getInstance().getConnection();

    @Override
    public void ajouter(Activite a) throws SQLDataException {
        String sql = "INSERT INTO activite_physique (type, duree, calories_brulees, date, intensite, objectif_id) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, a.getType());
            ps.setInt(2, a.getDuree());
            ps.setDouble(3, a.getCaloriesBrulees());
            ps.setDate(4, Date.valueOf(a.getDate()));
            ps.setString(5, a.getIntensite());
            ps.setInt(6, a.getObjectifId());
            ps.executeUpdate();
            System.out.println("✅ Activité ajoutée.");
        } catch (SQLException e) { System.err.println("❌ " + e.getMessage()); }
    }

    @Override
    public void modifier(Activite a) throws SQLDataException {
        String sql = "UPDATE activite_physique SET type=?, duree=?, calories_brulees=?, date=?, intensite=?, objectif_id=? WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, a.getType());
            ps.setInt(2, a.getDuree());
            ps.setDouble(3, a.getCaloriesBrulees());
            ps.setDate(4, Date.valueOf(a.getDate()));
            ps.setString(5, a.getIntensite());
            ps.setInt(6, a.getObjectifId());
            ps.setInt(7, a.getId());
            ps.executeUpdate();
            System.out.println("✅ Activité modifiée.");
        } catch (SQLException e) { System.err.println("❌ " + e.getMessage()); }
    }

    @Override
    public void supprimer(Activite a) throws SQLDataException {
        String sql = "DELETE FROM activite_physique WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, a.getId());
            ps.executeUpdate();
            System.out.println("✅ Activité supprimée.");
        } catch (SQLException e) { System.err.println("❌ " + e.getMessage()); }
    }

    @Override
    public List<Activite> recuperer() throws SQLDataException {
        List<Activite> list = new ArrayList<>();
        String sql = "SELECT * FROM activite_physique";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { System.err.println("❌ " + e.getMessage()); }
        return list;
    }

    public Activite getById(int id) {
        String sql = "SELECT * FROM activite_physique WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) { System.err.println("❌ " + e.getMessage()); }
        return null;
    }

    public List<Activite> getByObjectifId(int objectifId) {
        List<Activite> list = new ArrayList<>();
        String sql = "SELECT * FROM activite_physique WHERE objectif_id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, objectifId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { System.err.println("❌ " + e.getMessage()); }
        return list;
    }

    private Activite mapRow(ResultSet rs) throws SQLException {
        return new Activite(
                rs.getInt("id"), rs.getString("type"), rs.getInt("duree"),
                rs.getDouble("calories_brulees"), rs.getDate("date").toLocalDate(),
                rs.getString("intensite"), rs.getInt("objectif_id")
        );
    }
}
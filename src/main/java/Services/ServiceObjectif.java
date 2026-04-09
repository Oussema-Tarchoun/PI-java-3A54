package Services;

import Models.Objectif;
import utils.MyDatabase;

import java.sql.*;
import java.sql.SQLDataException;
import java.util.ArrayList;
import java.util.List;

public class ServiceObjectif implements Iservice<Objectif> {

    private final Connection cnx = MyDatabase.getInstance().getConnection();

    @Override
    public void ajouter(Objectif o) throws SQLDataException {
        String sql = "INSERT INTO objectif (description, type, valeur_cible, date_debut, date_fin, statut, user_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, o.getDescription());
            ps.setString(2, o.getType());
            ps.setInt(3, o.getValeurCible());
            ps.setDate(4, Date.valueOf(o.getDateDebut()));
            ps.setDate(5, Date.valueOf(o.getDateFin()));
            ps.setString(6, o.getStatut());
            ps.setInt(7, o.getUserId());
            ps.executeUpdate();
            System.out.println("✅ Objectif ajouté.");
        } catch (SQLException e) { System.err.println("❌ " + e.getMessage()); }
    }

    @Override
    public void modifier(Objectif o) throws SQLDataException {
        String sql = "UPDATE objectif SET description=?, type=?, valeur_cible=?, date_debut=?, date_fin=?, statut=?, user_id=? WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, o.getDescription());
            ps.setString(2, o.getType());
            ps.setInt(3, o.getValeurCible());
            ps.setDate(4, Date.valueOf(o.getDateDebut()));
            ps.setDate(5, Date.valueOf(o.getDateFin()));
            ps.setString(6, o.getStatut());
            ps.setInt(7, o.getUserId());
            ps.setInt(8, o.getId());
            ps.executeUpdate();
            System.out.println("✅ Objectif modifié.");
        } catch (SQLException e) { System.err.println("❌ " + e.getMessage()); }
    }

    @Override
    public void supprimer(Objectif o) throws SQLDataException {
        String sql = "DELETE FROM objectif WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, o.getId());
            ps.executeUpdate();
            System.out.println("✅ Objectif supprimé.");
        } catch (SQLException e) { System.err.println("❌ " + e.getMessage()); }
    }

    @Override
    public List<Objectif> recuperer() throws SQLDataException {
        List<Objectif> list = new ArrayList<>();
        String sql = "SELECT * FROM objectif";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { System.err.println("❌ " + e.getMessage()); }
        return list;
    }

    public Objectif getById(int id) {
        String sql = "SELECT * FROM objectif WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) { System.err.println("❌ " + e.getMessage()); }
        return null;
    }

    private Objectif mapRow(ResultSet rs) throws SQLException {
        return new Objectif(
                rs.getInt("id"), rs.getString("description"), rs.getString("type"),
                rs.getInt("valeur_cible"), rs.getDate("date_debut").toLocalDate(),
                rs.getDate("date_fin").toLocalDate(), rs.getString("statut"), rs.getInt("user_id")
        );
    }
}
package Services;

import Models.Chapitre;
import utils.MyDatabase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceChapitre implements Iservice<Chapitre> {

    private Connection cnx;

    public ServiceChapitre() {
        cnx = MyDatabase.getInstance().getConnection();
        if (cnx == null) {
            throw new RuntimeException("Database connection is null. Check your MyDatabase configuration.");
        }
    }

    @Override
    public void ajouter(Chapitre chapitre) throws SQLDataException {
        throw new SQLDataException("Utilisez ajouter(Chapitre, int) avec l'ID du cours");
    }

    public void ajouter(Chapitre chapitre, int coursId) throws SQLDataException {
        String req = "INSERT INTO chapitre (titre, contenu, ordre, exercise, id_cours) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(req, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, chapitre.getTitre());
            ps.setString(2, chapitre.getContenu());
            ps.setInt(3, chapitre.getOrdre());
            ps.setString(4, chapitre.getExercise());
            ps.setInt(5, coursId);
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                chapitre.setId(rs.getInt(1));
            }
        } catch (SQLException e) {
            throw new SQLDataException("Erreur lors de l'ajout du chapitre: " + e.getMessage());
        }
    }

    @Override
    public void supprimer(Chapitre chapitre) throws SQLDataException {
        String req = "DELETE FROM chapitre WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, chapitre.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new SQLDataException("Erreur lors de la suppression du chapitre: " + e.getMessage());
        }
    }

    @Override
    public void modifier(Chapitre chapitre) throws SQLDataException {
        String req = "UPDATE chapitre SET titre=?, contenu=?, ordre=?, exercise=? WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setString(1, chapitre.getTitre());
            ps.setString(2, chapitre.getContenu());
            ps.setInt(3, chapitre.getOrdre());
            ps.setString(4, chapitre.getExercise());
            ps.setInt(5, chapitre.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new SQLDataException("Erreur lors de la modification du chapitre: " + e.getMessage());
        }
    }

    @Override
    public List<Chapitre> recuperer() throws SQLDataException {
        List<Chapitre> list = new ArrayList<>();
        String req = "SELECT * FROM chapitre ORDER BY ordre";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(req)) {
            while (rs.next()) {
                Chapitre ch = mapResultSetToChapitre(rs);
                list.add(ch);
            }
        } catch (SQLException e) {
            throw new SQLDataException("Erreur lors de la récupération des chapitres: " + e.getMessage());
        }
        return list;
    }

    public List<Chapitre> getByCours(int coursId) throws SQLDataException {
        List<Chapitre> list = new ArrayList<>();
        String req = "SELECT * FROM chapitre WHERE id_cours = ? ORDER BY ordre";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, coursId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Chapitre ch = mapResultSetToChapitre(rs);
                list.add(ch);
            }
        } catch (SQLException e) {
            throw new SQLDataException("Erreur lors de la récupération des chapitres: " + e.getMessage());
        }
        return list;
    }

    public Chapitre getById(int id) throws SQLDataException {
        String req = "SELECT * FROM chapitre WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapResultSetToChapitre(rs);
            }
            return null;
        } catch (SQLException e) {
            throw new SQLDataException("Erreur lors de la récupération du chapitre: " + e.getMessage());
        }
    }

    private Chapitre mapResultSetToChapitre(ResultSet rs) throws SQLException {
        Chapitre ch = new Chapitre();
        ch.setId(rs.getInt("id"));
        ch.setTitre(rs.getString("titre"));
        ch.setContenu(rs.getString("contenu"));
        ch.setOrdre(rs.getInt("ordre"));
        ch.setExercise(rs.getString("exercise"));
        ch.setCoursId(rs.getInt("id_cours")); // Use id_cours from database
        return ch;
    }
}
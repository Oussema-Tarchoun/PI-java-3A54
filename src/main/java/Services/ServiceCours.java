package Services;

import Models.Cours;
import utils.MyDatabase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceCours implements Iservice<Cours> {

    private Connection cnx;

    public ServiceCours() {
        cnx = MyDatabase.getInstance().getConnection();
        if (cnx == null) {
            throw new RuntimeException("Database connection is null. Check your MyDatabase configuration.");
        }
    }

    @Override
    public void ajouter(Cours cours) throws SQLDataException {
        String req = "INSERT INTO cours (tittre, description, niveau, duree_estimee, categorie, status, date_creation) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(req, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, cours.getTittre());
            ps.setString(2, cours.getDescription());
            ps.setString(3, cours.getNiveau());
            ps.setInt(4, cours.getDureeEstimee());
            ps.setString(5, cours.getCategorie());
            ps.setString(6, cours.getStatus());
            ps.setString(7, cours.getDateCreation());
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                cours.setId(rs.getInt(1));
            }
        } catch (SQLException e) {
            throw new SQLDataException("Erreur lors de l'ajout du cours: " + e.getMessage());
        }
    }

    @Override
    public void supprimer(Cours cours) throws SQLDataException {
        String req = "DELETE FROM cours WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, cours.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new SQLDataException("Erreur lors de la suppression du cours: " + e.getMessage());
        }
    }

    @Override
    public void modifier(Cours cours) throws SQLDataException {
        String req = "UPDATE cours SET tittre=?, description=?, niveau=?, duree_estimee=?, categorie=?, status=? WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setString(1, cours.getTittre());
            ps.setString(2, cours.getDescription());
            ps.setString(3, cours.getNiveau());
            ps.setInt(4, cours.getDureeEstimee());
            ps.setString(5, cours.getCategorie());
            ps.setString(6, cours.getStatus());
            ps.setInt(7, cours.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new SQLDataException("Erreur lors de la modification du cours: " + e.getMessage());
        }
    }

    @Override
    public List<Cours> recuperer() throws SQLDataException {
        List<Cours> list = new ArrayList<>();
        String req = "SELECT * FROM cours ORDER BY date_creation DESC";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(req)) {
            while (rs.next()) {
                Cours c = new Cours();
                c.setId(rs.getInt("id"));
                c.setTittre(rs.getString("tittre"));
                c.setDescription(rs.getString("description"));
                c.setNiveau(rs.getString("niveau"));
                c.setDureeEstimee(rs.getInt("duree_estimee"));
                c.setCategorie(rs.getString("categorie"));
                c.setStatus(rs.getString("status"));
                c.setDateCreation(rs.getString("date_creation"));
                list.add(c);
            }
        } catch (SQLException e) {
            throw new SQLDataException("Erreur lors de la récupération des cours: " + e.getMessage());
        }
        return list;
    }
}
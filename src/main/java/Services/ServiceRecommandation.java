package Services;

import Models.Energie;
import Models.Recommandation;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ServiceRecommandation implements Iservice<Recommandation> {

    private Connection getConnection() {
        return MyDatabase.getInstance().getConnection();
    }

    public ServiceRecommandation() {
    }

    @Override
    public void ajouter(Recommandation reco) throws SQLDataException {
        String query = "INSERT INTO recommandation (titre, description, niveau_impact, date_generation, energie_id) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setString(1, reco.getTitre());
            ps.setString(2, reco.getDescription());
            ps.setString(3, reco.getNiveau_impact());
            ps.setDate(4, new java.sql.Date(reco.getDate_generation().getTime()));
            ps.setInt(5, reco.getEnergie_id());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLDataException("Erreur lors de l'ajout: " + e.getMessage());
        }
    }

    @Override
    public void supprimer(Recommandation reco) throws SQLDataException {
        String query = "DELETE FROM recommandation WHERE id=?";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setInt(1, reco.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLDataException("Erreur lors de la suppression: " + e.getMessage());
        }
    }

    @Override
    public void modifier(Recommandation reco) throws SQLDataException {
        String query = "UPDATE recommandation SET titre=?, description=?, niveau_impact=?, date_generation=? WHERE energie_id=?";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setString(1, reco.getTitre());
            ps.setString(2, reco.getDescription());
            ps.setString(3, reco.getNiveau_impact());
            ps.setDate(4, new java.sql.Date(reco.getDate_generation().getTime()));
            ps.setInt(5, reco.getEnergie_id());
            int rows = ps.executeUpdate();
            
            if (rows == 0) {
                // If the recommendation didn't exist, we should probably add it
                System.out.println("No recommendation found for energie_id " + reco.getEnergie_id() + ". Creating one...");
                ajouter(reco);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLDataException("Erreur lors de la modification: " + e.getMessage());
        }
    }

    @Override
    public List<Recommandation> recuperer() throws SQLDataException {
        List<Recommandation> list = new ArrayList<>();
        String query = "SELECT * FROM recommandation ORDER BY date_generation DESC";
        try (Statement st = getConnection().createStatement();
             ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLDataException("Erreur lors de la récupération: " + e.getMessage());
        }
        return list;
    }

    public void supprimerParEnergie(int energie_id) throws SQLDataException {
        String query = "DELETE FROM recommandation WHERE energie_id=?";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setInt(1, energie_id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLDataException("Erreur lors de la suppression par énergie: " + e.getMessage());
        }
    }

    public List<Recommandation> recupererParEnergie(int energie_id) throws SQLDataException {
        List<Recommandation> list = new ArrayList<>();
        String query = "SELECT * FROM recommandation WHERE energie_id=? ORDER BY date_generation DESC";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setInt(1, energie_id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLDataException("Erreur lors de la récupération par énergie_id: " + e.getMessage());
        }
        return list;
    }

    public List<Recommandation> recupererParUser(String user_id) throws SQLDataException {
        List<Recommandation> list = new ArrayList<>();
        String query = "SELECT r.* FROM recommandation r JOIN energie e ON r.energie_id = e.id WHERE e.user_id=? ORDER BY r.date_generation DESC";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setString(1, user_id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLDataException("Erreur de récupération par utilisateur: " + e.getMessage());
        }
        return list;
    }

    public int getTotalCount() throws SQLDataException {
        String query = "SELECT COUNT(*) FROM recommandation";
        try (Statement st = getConnection().createStatement();
             ResultSet rs = st.executeQuery(query)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            throw new SQLDataException(e.getMessage());
        }
        return 0;
    }

    public Recommandation genererRecommandation(Energie energie) {
        String titre;
        String description;
        String niveau_impact;

        if (energie.getValeur() > 1000) {
            titre = "Consommation élevée";
            description = "Votre consommation de " + energie.getType_energie() +
                          " est très élevée (" + energie.getValeur() + " unités).";
            niveau_impact = "élevé";
        } else if (energie.getValeur() >= 500) {
            titre = "Consommation moyenne";
            description = "Votre consommation de " + energie.getType_energie() +
                          " est dans la moyenne (" + energie.getValeur() + " unités).";
            niveau_impact = "moyen";
        } else {
            titre = "Bonne consommation";
            description = "Félicitations ! Votre consommation de " + energie.getType_energie() +
                          " est faible (" + energie.getValeur() + " unités).";
            niveau_impact = "faible";
        }

        return new Recommandation(titre, description, niveau_impact, new Date(), energie.getId());
    }

    private Recommandation mapResultSet(ResultSet rs) throws SQLException {
        return new Recommandation(
            rs.getInt("id"),
            rs.getString("titre"),
            rs.getString("description"),
            rs.getString("niveau_impact"),
            rs.getDate("date_generation"),
            rs.getInt("energie_id")
        );
    }
}

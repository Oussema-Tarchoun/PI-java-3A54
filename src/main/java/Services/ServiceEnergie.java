package Services;

import Models.Energie;
import Models.Recommandation;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceEnergie implements Iservice<Energie> {

    private final ServiceRecommandation serviceRecommandation;
    private final MailService mailService;
    private final ServiceUser serviceUser;
    private final WeatherService weatherService;
    private final AIWeatherAdvisorService weatherAdvisorService;

    private Connection getConnection() {
        return MyDatabase.getInstance().getConnection();
    }

    public ServiceEnergie() {
        this.serviceRecommandation = new ServiceRecommandation();
        this.mailService = new MailService();
        this.serviceUser = new ServiceUser();
        this.weatherService = new WeatherService();
        this.weatherAdvisorService = new AIWeatherAdvisorService();
    }

    private void checkAlerts(Energie energie, Recommandation reco) {
        Models.User user = serviceUser.getById(energie.getUser_id());
        if (user == null) return;

        // 1. Alerte forte consommation (Règle métier)
        if (energie.getValeur() > 1000) {
            mailService.sendAlert(user, "Forte Consommation", 
                "Votre consommation de " + energie.getValeur() + " unités est supérieure au seuil recommandé. " +
                "L'IA suggère : " + (reco != null ? reco.getDescription() : "Réduire l'utilisation des appareils énergivores."));
        }

        // 2. Alerte Météo (IA)
        try {
            WeatherService.WeatherData weather = weatherService.fetchCurrentWeather();
            if (weather.condition.equalsIgnoreCase("Rain") || weather.temp < 15 || weather.temp > 35) {
                String advice = weatherAdvisorService.getAdvice(weather);
                mailService.sendAlert(user, "Conseil Météo", 
                    "La météo actuelle (" + weather.description + ") impacte vos besoins. Conseil : " + advice);
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la vérification météo pour alerte: " + e.getMessage());
        }
    }

    public Energie getById(int id) throws SQLDataException {
        String query = "SELECT * FROM energie WHERE id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapResultSet(rs);
            }
        } catch (SQLException e) {
            throw new SQLDataException("Erreur de récupération: " + e.getMessage());
        }
        return null;
    }

    @Override
    public void ajouter(Energie energie) throws SQLDataException {
        String query = "INSERT INTO energie (type_energie, periode, valeur, date_enregistrement, source, user_id) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, energie.getType_energie());
            ps.setFloat(2, energie.getPeriode());
            ps.setFloat(3, energie.getValeur());
            ps.setDate(4, new java.sql.Date(energie.getDate_enregistrement().getTime()));
            ps.setString(5, energie.getSource());
            ps.setString(6, energie.getUser_id());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    energie.setId(rs.getInt(1));
                    // Now that we have the ID, generate and add recommendation
                    Recommandation reco = serviceRecommandation.genererRecommandation(energie);
                    serviceRecommandation.ajouter(reco);

                    // --- ENVOI EMAIL ---
                    Models.User user = serviceUser.getById(energie.getUser_id());
                    if (user != null) {
                        mailService.sendEnergyAdded(user, energie, reco);
                        // Vérifier alertes intelligentes
                        checkAlerts(energie, reco);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLDataException("Erreur d'ajout: " + e.getMessage());
        }
    }

    @Override
    public void modifier(Energie energie) throws SQLDataException {
        // Fetch old record for comparison
        Energie oldEnergie = getById(energie.getId());

        String query = "UPDATE energie SET type_energie=?, periode=?, valeur=?, date_enregistrement=?, source=?, user_id=? WHERE id=?";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setString(1, energie.getType_energie());
            ps.setFloat(2, energie.getPeriode());
            ps.setFloat(3, energie.getValeur());
            ps.setDate(4, new java.sql.Date(energie.getDate_enregistrement().getTime()));
            ps.setString(5, energie.getSource());
            ps.setString(6, energie.getUser_id());
            ps.setInt(7, energie.getId());
            int rows = ps.executeUpdate();

            if (rows > 0) {
                // Update or Create recommendation
                Recommandation reco = serviceRecommandation.genererRecommandation(energie);
                serviceRecommandation.modifier(reco);

                // --- ENVOI EMAIL ---
                Models.User user = serviceUser.getById(energie.getUser_id());
                if (user != null && oldEnergie != null) {
                    mailService.sendEnergyModified(user, oldEnergie, energie, reco);
                    // Vérifier alertes intelligentes après modif
                    checkAlerts(energie, reco);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLDataException("Erreur de modification: " + e.getMessage());
        }
    }

    @Override
    public void supprimer(Energie energie) throws SQLDataException {
        try {
            // Supprimer d'abord les recommandations associées
            serviceRecommandation.supprimerParEnergie(energie.getId());

            String query = "DELETE FROM energie WHERE id=?";
            try (PreparedStatement ps = getConnection().prepareStatement(query)) {
                ps.setInt(1, energie.getId());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLDataException("Erreur de suppression: " + e.getMessage());
        }
    }

    @Override
    public List<Energie> recuperer() throws SQLDataException {
        List<Energie> list = new ArrayList<>();
        String query = "SELECT * FROM energie ORDER BY date_enregistrement DESC";
        try (Statement st = getConnection().createStatement();
             ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLDataException("Erreur de récupération: " + e.getMessage());
        }
        return list;
    }

    public List<Energie> recupererParUser(String user_id) throws SQLDataException {
        List<Energie> list = new ArrayList<>();
        String query = "SELECT * FROM energie WHERE user_id=? ORDER BY date_enregistrement DESC";
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

    public List<Energie> filtrerParType(String type_energie) throws SQLDataException {
        List<Energie> list = new ArrayList<>();
        String query = "SELECT * FROM energie WHERE type_energie=? ORDER BY date_enregistrement DESC";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setString(1, type_energie);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLDataException("Erreur de filtrage: " + e.getMessage());
        }
        return list;
    }

    public List<Energie> recupererTrieParValeur() throws SQLDataException {
        List<Energie> list = new ArrayList<>();
        String query = "SELECT * FROM energie ORDER BY valeur DESC";
        try (Statement st = getConnection().createStatement();
             ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLDataException("Erreur tri: " + e.getMessage());
        }
        return list;
    }

    public int getTotalCount() throws SQLDataException {
        String query = "SELECT COUNT(*) FROM energie";
        try (Statement st = getConnection().createStatement();
             ResultSet rs = st.executeQuery(query)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLDataException(e.getMessage());
        }
        return 0;
    }

    public double getMoyenneValeur() throws SQLDataException {
        String query = "SELECT AVG(valeur) FROM energie";
        try (Statement st = getConnection().createStatement();
             ResultSet rs = st.executeQuery(query)) {
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLDataException(e.getMessage());
        }
        return 0;
    }

    public double getTotalValeurParType(String type) throws SQLDataException {
        String query = "SELECT SUM(valeur) FROM energie WHERE type_energie=?";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setString(1, type);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLDataException(e.getMessage());
        }
        return 0;
    }

    private Energie mapResultSet(ResultSet rs) throws SQLException {
        return new Energie(
            rs.getInt("id"),
            rs.getString("type_energie"),
            rs.getFloat("periode"),
            rs.getFloat("valeur"),
            rs.getDate("date_enregistrement"),
            rs.getString("source"),
            rs.getString("user_id") // Modifié ici
        );
    }
}

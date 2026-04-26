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



    private String estimateMacro(String name, int calories) {
        if (calories == 0) return "0,0,0";
        String n = name.toLowerCase();
        double p, g, l;

        if (n.matches(".*(poulet|dinde|thon|saumon|boeuf|veau|porc|crevette|moule|sardine|cabillaud|oeuf).*")) {
            p = calories * 0.50 / 4; g = calories * 0.10 / 4; l = calories * 0.40 / 9;
        } else if (n.matches(".*(huile|beurre|margarine|noix|amande|noisette|cacahuete|avocat).*")) {
            p = calories * 0.03 / 4; g = calories * 0.05 / 4; l = calories * 0.92 / 9;
        } else if (n.matches(".*(pain|riz|pate|spaghetti|farine|pomme de terre|couscous|quinoa|sucre|miel|avoine|biscuit).*")) {
            p = calories * 0.10 / 4; g = calories * 0.78 / 4; l = calories * 0.12 / 9;
        } else if (n.matches(".*(lait|yaourt|fromage|creme).*")) {
            p = calories * 0.25 / 4; g = calories * 0.28 / 4; l = calories * 0.47 / 9;
        } else if (n.matches(".*(tomate|salade|epinard|carotte|brocoli|courgette|poivron|oignon|pomme|banane|fraise|orange|citron|datte|raisin|mangue).*")) {
            p = calories * 0.12 / 4; g = calories * 0.72 / 4; l = calories * 0.16 / 9;
        } else if (n.matches(".*(sel|poivre|epice|basilic|thym|cumin|cannelle|paprika|curcuma).*")) {
            p = 0; g = 0; l = 0;
        } else {
            p = calories * 0.20 / 4; g = calories * 0.50 / 4; l = calories * 0.30 / 9;
        }

        return String.format("%.1f,%.1f,%.1f", p, g, l);
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
                a.setQuantite(rs.getDouble("quantite"));
                double existingCal = rs.getDouble("calories");
                String existingMacro = rs.getString("macro");

                // Update si calories = 0 ou macro vide
                if ((existingCal == 0 && calories > 0) || (existingMacro == null || existingMacro.isBlank())) {
                    double newCal = existingCal == 0 ? calories : existingCal;
                    String newMacro = (existingMacro == null || existingMacro.isBlank())
                            ? estimateMacro(name.trim(), (int) newCal) : existingMacro;
                    String updateSql = "UPDATE aliment SET calories = ?, macro = ? WHERE id = ?";
                    try (PreparedStatement upd = getConnection().prepareStatement(updateSql)) {
                        upd.setDouble(1, newCal);
                        upd.setString(2, newMacro);
                        upd.setInt(3, a.getId());
                        upd.executeUpdate();
                    } catch (SQLException e) { System.out.println("update error: " + e.getMessage()); }
                    a.setCalories(newCal);
                    a.setMacro(newMacro);
                } else {
                    a.setCalories(existingCal);
                    a.setMacro(existingMacro);
                }
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
            ps.setString(4, estimateMacro(name.trim(), calories));            ps.executeUpdate();
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
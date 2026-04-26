package Services;

import Models.User;
import utils.MyDatabase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceUser {
    public List<User> recuperer() {
        List<User> list = new ArrayList<>();
        String query = "SELECT id, name, email FROM user"; // Added email
        try (Connection conn = MyDatabase.getInstance().getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            
            while (rs.next()) {
                list.add(new User(String.valueOf(rs.getInt("id")), rs.getString("name"), rs.getString("email")));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching users: " + e.getMessage());
            // Fallback
            if (list.isEmpty()) {
                list.add(new User("1", "Hamza (Fallback)", "hamza@example.com"));
            }
        }
        return list;
    }

    public User getById(String id) {
        String query = "SELECT id, name, email FROM user WHERE id = ?";
        try (Connection conn = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, Integer.parseInt(id));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new User(String.valueOf(rs.getInt("id")), rs.getString("name"), rs.getString("email"));
                }
            }
        } catch (SQLException | NumberFormatException e) {
            System.err.println("Error fetching user by id: " + e.getMessage());
        }
        // Fallback for demo if id is 1
        if ("1".equals(id)) return new User("1", "Hamza (Fallback)", "hamza@example.com");
        return null;
    }
}

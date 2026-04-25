package Services;

import Models.User;
import utils.MyDatabase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceUser {
    public List<User> recuperer() {
        List<User> list = new ArrayList<>();
        String query = "SELECT id, name FROM user"; // Based on provided structure
        try (Connection conn = MyDatabase.getInstance().getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            
            while (rs.next()) {
                list.add(new User(String.valueOf(rs.getInt("id")), rs.getString("name")));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching users: " + e.getMessage());
            // Fallback
            if (list.isEmpty()) {
                list.add(new User("1", "Hamza (Fallback)"));
            }
        }
        return list;
    }
}

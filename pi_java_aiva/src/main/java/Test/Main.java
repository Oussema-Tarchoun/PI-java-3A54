package Test;

import Models.User;
import Services.ServiceUser;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class Main {
    public static void main(String[] args) {
        ServiceUser service = new ServiceUser();

        try {
            // 1. Create a simple user object
            // (Leaving most fields with basic values for simplicity)
            User newUser = new User();
            newUser.setEmail("test_" + System.currentTimeMillis() + "@test.com"); // Unique email
            newUser.setPassword("password123");
            newUser.setName("seconde User");
            newUser.setRoles("[\"ROLE_USER\"]");           // Valid JSON array for roles
            newUser.setKnownIps("[\"127.0.0.1\"]");        // Valid JSON array for IPs
            newUser.setTotpSecret("ABCDEF123456");
            newUser.setIsBlocked(0);
            newUser.setResetPasswordAttempts(0);
            newUser.setIsVerified(true);
            newUser.setVerificationToken("token-123");
            newUser.setExperiencePoints(0);
            newUser.setIs2faEnabled(false);

            // 2. Add to database
            System.out.println("Adding user...");
            service.ajouter(newUser);

            // 3. Show all users in the console
            System.out.println("Current Users in DB:");
            System.out.println(service.recuperer());

        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
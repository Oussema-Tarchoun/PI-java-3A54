package Services;

import Models.ResetPasswordRequest;
import Models.User;
import utils.MyDatabase;
import utils.PasswordUtils;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;

public class ServiceResetPasswordRequest {
    private Connection connection;

    public ServiceResetPasswordRequest() {
        connection = MyDatabase.getInstance().getConnection();
    }

    public String createRequest(int userId) throws SQLException {
        String selector = UUID.randomUUID().toString().substring(0, 20);
        String code = String.format("%06d", new Random().nextInt(999999));
        String hashedToken = PasswordUtils.hashPassword(code);
        LocalDateTime requestedAt = LocalDateTime.now();
        LocalDateTime expiresAt = requestedAt.plusMinutes(15);

        String sql = "INSERT INTO reset_password_request (selector, hashed_token, requested_at, expires_at, user_id) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, selector);
            ps.setString(2, hashedToken);
            ps.setTimestamp(3, Timestamp.valueOf(requestedAt));
            ps.setTimestamp(4, Timestamp.valueOf(expiresAt));
            ps.setInt(5, userId);
            ps.executeUpdate();
        }
        return code;
    }

    public void sendEmail(String recipientEmail, String code) throws MessagingException {
        String subject = "Réinitialisation de votre mot de passe - AIVA";
        String content = "Bonjour,\n\n"
                + "Votre code de réinitialisation est : " + code + "\n\n"
                + "Ce code expirera dans 15 minutes.\n\n"
                + "L'équipe AIVA.";
        
        utils.MailUtils.sendEmail(recipientEmail, subject, content);
    }

    public boolean validateCode(int userId, String code) throws SQLException {
        String hashedCode = PasswordUtils.hashPassword(code);
        String sql = "SELECT * FROM reset_password_request WHERE user_id = ? AND hashed_token = ? AND expires_at > NOW() ORDER BY requested_at DESC LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, hashedCode);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void resetPassword(int userId, String newPassword) throws SQLException {
        String hashedPassword = PasswordUtils.hashPassword(newPassword);
        String sql = "UPDATE user SET password = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, hashedPassword);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
        
        String deleteSql = "DELETE FROM reset_password_request WHERE user_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(deleteSql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }
}

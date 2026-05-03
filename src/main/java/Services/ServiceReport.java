package Services;

import Models.Report;
import Models.User;
import utils.MyDatabase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ServiceReport {

    private Connection connection;

    public ServiceReport() {
        connection = MyDatabase.getInstance().getConnection();
    }

    private void checkConnection() throws SQLException {
        if (connection == null) {
            throw new SQLException("La connexion à la base de données a échoué.");
        }
    }


    public void addReport(User user, Report report) {
        String sql;
        boolean isUpdate = report.getId() > 0;
        
        if (isUpdate) {
            sql = "UPDATE report SET title=?, description=?, user_id=? WHERE id=?";
        } else {
            sql = "INSERT INTO report (title, description, created_at, user_id) VALUES (?, ?, ?, ?)";
        }

        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            checkConnection();
            ps.setString(1, report.getTitle());
            ps.setString(2, report.getDescription());
            
            if (isUpdate) {
                ps.setInt(3, user.getId());
                ps.setInt(4, report.getId());
                ps.executeUpdate();
            } else {
                ps.setTimestamp(3, Timestamp.valueOf(report.getCreatedAt() != null ? report.getCreatedAt() : LocalDateTime.now()));
                ps.setInt(4, user.getId());
                ps.executeUpdate();
                
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        report.setId(rs.getInt(1));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public List<Report> getReportsByUser(User user) {
        List<Report> reports = new ArrayList<>();
        String sql = "SELECT * FROM report WHERE user_id = ?";
        
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            checkConnection();
            ps.setInt(1, user.getId());
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    reports.add(mapResultSetToReport(rs, user));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return reports;
    }


    public List<Report> recupererTout() {
        List<Report> reports = new ArrayList<>();
        String sql = "SELECT r.*, u.email, u.roles, u.password, u.name, u.is_blocked, u.totp_secret, u.reset_password_attempts, u.known_ips, u.is_verified, u.verification_token, u.token_expires_at, u.experience_points, u.last_points_awarded_at, u.is2fa_enabled FROM report r JOIN user u ON r.user_id = u.id";
        
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            checkConnection();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    User user = new User();
                    user.setId(rs.getInt("user_id"));
                    user.setEmail(rs.getString("email"));
                    user.setRoles(rs.getString("roles"));
                    user.setPassword(rs.getString("password"));
                    user.setName(rs.getString("name"));
                    user.setIsBlocked(rs.getInt("is_blocked"));
                    user.setTotpSecret(rs.getString("totp_secret"));
                    user.setResetPasswordAttempts(rs.getInt("reset_password_attempts"));
                    user.setKnownIps(rs.getString("known_ips"));
                    user.setIsVerified(rs.getBoolean("is_verified"));
                    user.setVerificationToken(rs.getString("verification_token"));
                    
                    Timestamp expires = rs.getTimestamp("token_expires_at");
                    if (expires != null) user.setTokenExpiresAt(expires.toLocalDateTime());
                    
                    user.setExperiencePoints(rs.getInt("experience_points"));
                    
                    Timestamp lastAward = rs.getTimestamp("last_points_awarded_at");
                    if (lastAward != null) user.setLastPointsAwardedAt(lastAward.toLocalDateTime());
                    
                    user.setIs2faEnabled(rs.getBoolean("is2fa_enabled"));
                    
                    reports.add(mapResultSetToReport(rs, user));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return reports;
    }


    public void deleteReport(Report report) {
        String sql = "DELETE FROM report WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            checkConnection();
            ps.setInt(1, report.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    private Report mapResultSetToReport(ResultSet rs, User user) throws SQLException {
        Report report = new Report();
        report.setId(rs.getInt("id"));
        report.setTitle(rs.getString("title"));
        report.setDescription(rs.getString("description"));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            report.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        report.setUser(user);
        return report;
    }
}

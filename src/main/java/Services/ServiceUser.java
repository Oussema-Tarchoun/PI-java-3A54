package Services;

import Models.User;
import utils.MyDatabase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class ServiceUser implements Iservice<User> {
    private Connection connection;

    public ServiceUser() {
        connection = MyDatabase.getInstance().getConnection();
    }

    private void checkConnection() throws SQLException {
        if (connection == null) {
            throw new SQLException("La connexion à la base de données a échoué. Veuillez vérifier que votre serveur MySQL est lancé.");
        }
    }

    @Override
    public void ajouter(User user) throws SQLDataException {
        try {
            checkConnection();
        }catch(SQLException e){
            System.out.println( e.getMessage() );
        }

        String sql = "INSERT INTO user (email, roles, password, name, is_blocked, totp_secret, " +
                "reset_password_attempts, known_ips, is_verified, verification_token, " +
                "token_expires_at, experience_points, last_points_awarded_at, is2fa_enabled) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, user.getEmail());
            ps.setString(2, user.getRoles());
            ps.setString(3, user.getPassword());
            ps.setString(4, user.getName());
            ps.setInt(5, user.getIsBlocked()); // Fixed: removed null check
            ps.setString(6, user.getTotpSecret());
            ps.setInt(7, user.getResetPasswordAttempts()); // Fixed: removed null check
            ps.setString(8, user.getKnownIps());
            ps.setBoolean(9, user.getIsVerified()); // Fixed: removed null check
            ps.setString(10, user.getVerificationToken());
            ps.setTimestamp(11, user.getTokenExpiresAt() != null ? Timestamp.valueOf(user.getTokenExpiresAt()) : null);
            ps.setInt(12, user.getExperiencePoints()); // Fixed: removed null check
            ps.setTimestamp(13, user.getLastPointsAwardedAt() != null ? Timestamp.valueOf(user.getLastPointsAwardedAt()) : null);
            ps.setBoolean(14, user.getIs2faEnabled()); // Fixed: removed null check

            ps.executeUpdate();
        }catch (SQLException e){
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void supprimer(User user) throws SQLDataException {
        try {
            checkConnection();
        }catch(SQLException e){
            System.out.println( e.getMessage() );
        }        String sql = "DELETE FROM user WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, user.getId());
            ps.executeUpdate();
        }catch (SQLException e){
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void modifier(User user) throws SQLDataException {
        try {
            checkConnection();
        }catch(SQLException e){
            System.out.println( e.getMessage() );
        }        String sql = "UPDATE user SET email=?, roles=?, password=?, name=?, is_blocked=?, " +
                "totp_secret=?, reset_password_attempts=?, known_ips=?, is_verified=?, " +
                "verification_token=?, token_expires_at=?, experience_points=?, " +
                "last_points_awarded_at=?, is2fa_enabled=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, user.getEmail());
            ps.setString(2, user.getRoles());
            ps.setString(3, user.getPassword());
            ps.setString(4, user.getName());
            ps.setInt(5, user.getIsBlocked());
            ps.setString(6, user.getTotpSecret());
            ps.setInt(7, user.getResetPasswordAttempts());
            ps.setString(8, user.getKnownIps());
            ps.setBoolean(9, user.getIsVerified());
            ps.setString(10, user.getVerificationToken());
            ps.setTimestamp(11, user.getTokenExpiresAt() != null ? Timestamp.valueOf(user.getTokenExpiresAt()) : null);
            ps.setInt(12, user.getExperiencePoints());
            ps.setTimestamp(13, user.getLastPointsAwardedAt() != null ? Timestamp.valueOf(user.getLastPointsAwardedAt()) : null);
            ps.setBoolean(14, user.getIs2faEnabled());
            ps.setInt(15, user.getId());

            ps.executeUpdate();
        }catch (SQLException e){
            System.out.println(e.getMessage());
        }
    }

    @Override
    public List<User> recuperer() throws SQLDataException {
        try {
            checkConnection();
        }catch(SQLException e){
            System.out.println( e.getMessage() );
        }        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM user";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        }catch (SQLException e){
            System.out.println(e.getMessage());
        }
        return users;
    }

    public User login(String email, String password) throws SQLException {
        checkConnection();
        String sql = "SELECT * FROM user WHERE email = ? AND password = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        }
        return null;
    }

    public boolean emailExists(String email) throws SQLException {
        checkConnection();
        String sql = "SELECT COUNT(*) FROM user WHERE email = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    public User findUserByEmail(String email) throws SQLException {
        checkConnection();
        String sql = "SELECT * FROM user WHERE email = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        }
        return null;
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setEmail(rs.getString("email"));
        u.setRoles(rs.getString("roles"));
        u.setPassword(rs.getString("password"));
        u.setName(rs.getString("name"));
        u.setIsBlocked(rs.getInt("is_blocked"));
        u.setTotpSecret(rs.getString("totp_secret"));
        u.setResetPasswordAttempts(rs.getInt("reset_password_attempts"));
        u.setKnownIps(rs.getString("known_ips"));
        u.setIsVerified(rs.getBoolean("is_verified"));
        u.setVerificationToken(rs.getString("verification_token"));

        Timestamp expires = rs.getTimestamp("token_expires_at");
        if (expires != null) u.setTokenExpiresAt(expires.toLocalDateTime());

        u.setExperiencePoints(rs.getInt("experience_points"));

        Timestamp lastAward = rs.getTimestamp("last_points_awarded_at");
        if (lastAward != null) u.setLastPointsAwardedAt(lastAward.toLocalDateTime());

        u.setIs2faEnabled(rs.getBoolean("is2fa_enabled"));
        return u;
    }

    public void updateVerificationToken(int userId, String token, java.time.LocalDateTime expiry) throws SQLException {
        checkConnection();
        String sql = "UPDATE user SET verification_token = ?, token_expires_at = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, token);
            ps.setTimestamp(2, Timestamp.valueOf(expiry));
            ps.setInt(3, userId);
            ps.executeUpdate();
        }
    }

    public boolean verifyAccount(String email, String token) throws SQLException {
        checkConnection();
        String sql = "SELECT id FROM user WHERE email = ? AND verification_token = ? AND token_expires_at > NOW()";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, token);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int userId = rs.getInt("id");
                    String updateSql = "UPDATE user SET is_verified = 1, verification_token = NULL, token_expires_at = NULL WHERE id = ?";
                    try (PreparedStatement psUpdate = connection.prepareStatement(updateSql)) {
                        psUpdate.setInt(1, userId);
                        psUpdate.executeUpdate();
                    }
                    return true;
                }
            }
        }
        return false;
    }
}
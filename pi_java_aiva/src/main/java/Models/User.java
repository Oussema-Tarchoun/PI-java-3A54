package Models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.*;

@Entity
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(columnDefinition = "JSON")
    private String roles;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Column(name = "is_blocked")
    private int isBlocked;

    @Column(name = "totp_secret")
    private String totpSecret;

    @Column(name = "reset_password_attempts")
    private int resetPasswordAttempts;

    @Column(name = "known_ips", columnDefinition = "JSON")
    private String knownIps;

    @Column(name = "is_verified")
    private boolean isVerified;

    @Column(name = "verification_token")
    private String verificationToken;

    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt;

    @Column(name = "experience_points")
    private int experiencePoints;

    @Column(name = "last_points_awarded_at")
    private LocalDateTime lastPointsAwardedAt;

    @Column(name = "is2fa_enabled")
    private boolean is2faEnabled;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Report> reports = new ArrayList<>();

    public User() {}

    // Constructor without ID (for adding new users)
    public User(String email, String roles, String password, String name, int isBlocked, String totpSecret,
                int resetPasswordAttempts, String knownIps, boolean isVerified, String verificationToken,
                LocalDateTime tokenExpiresAt, int experiencePoints, LocalDateTime lastPointsAwardedAt,
                boolean is2faEnabled) {
        this.email = email;
        this.roles = roles;
        this.password = password;
        this.name = name;
        this.isBlocked = isBlocked;
        this.totpSecret = totpSecret;
        this.resetPasswordAttempts = resetPasswordAttempts;
        this.knownIps = knownIps;
        this.isVerified = isVerified;
        this.verificationToken = verificationToken;
        this.tokenExpiresAt = tokenExpiresAt;
        this.experiencePoints = experiencePoints;
        this.lastPointsAwardedAt = lastPointsAwardedAt;
        this.is2faEnabled = is2faEnabled;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRoles() { return roles; }
    public void setRoles(String roles) { this.roles = roles; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getIsBlocked() { return isBlocked; }
    public void setIsBlocked(int isBlocked) { this.isBlocked = isBlocked; }

    public String getTotpSecret() { return totpSecret; }
    public void setTotpSecret(String totpSecret) { this.totpSecret = totpSecret; }

    public int getResetPasswordAttempts() { return resetPasswordAttempts; }
    public void setResetPasswordAttempts(int resetPasswordAttempts) { this.resetPasswordAttempts = resetPasswordAttempts; }

    public String getKnownIps() { return knownIps; }
    public void setKnownIps(String knownIps) { this.knownIps = knownIps; }

    public boolean getIsVerified() { return isVerified; }
    public void setIsVerified(boolean isVerified) { this.isVerified = isVerified; }

    public String getVerificationToken() { return verificationToken; }
    public void setVerificationToken(String verificationToken) { this.verificationToken = verificationToken; }

    public LocalDateTime getTokenExpiresAt() { return tokenExpiresAt; }
    public void setTokenExpiresAt(LocalDateTime tokenExpiresAt) { this.tokenExpiresAt = tokenExpiresAt; }

    public int getExperiencePoints() { return experiencePoints; }
    public void setExperiencePoints(int experiencePoints) { this.experiencePoints = experiencePoints; }

    public LocalDateTime getLastPointsAwardedAt() { return lastPointsAwardedAt; }
    public void setLastPointsAwardedAt(LocalDateTime lastPointsAwardedAt) { this.lastPointsAwardedAt = lastPointsAwardedAt; }

    public boolean getIs2faEnabled() { return is2faEnabled; }
    public void setIs2faEnabled(boolean is2faEnabled) { this.is2faEnabled = is2faEnabled; }
}
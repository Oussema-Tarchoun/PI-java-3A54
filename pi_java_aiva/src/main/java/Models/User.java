package Models;

import java.time.LocalDateTime;

public class User {

    private int id;
    private String email;
    private String roles;
    private String password;
    private String name;
    private int isBlocked;
    private String totpSecret;
    private int resetPasswordAttempts;
    private String knownIps;
    private boolean isVerified;
    private String verificationToken;
    private LocalDateTime tokenExpiresAt;
    private int experiencePoints;
    private LocalDateTime lastPointsAwardedAt;
    private boolean is2faEnabled;

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
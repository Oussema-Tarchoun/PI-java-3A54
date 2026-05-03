package Models;

import java.time.LocalDateTime;

public class Report {

    private int id;

    private String title;

    private String description;

    private LocalDateTime createdAt;

    private User user;

    public Report() {
        this.createdAt = LocalDateTime.now();
    }

    public Report(String title, String description, User user) {
        this.title = title;
        this.description = description;
        this.user = user;
        this.createdAt = LocalDateTime.now();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}

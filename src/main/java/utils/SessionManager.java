package utils;

import Models.User;

public class SessionManager {
    private static SessionManager instance;
    private User currentUser;

    private SessionManager() {}

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public boolean isAdmin() {
        return currentUser != null && currentUser.isAdmin();
    }

    public boolean isUser() {
        return currentUser != null && currentUser.isUser();
    }

    public void logout() {
        currentUser = null;
    }

    public int getCurrentUserId() {
        return currentUser != null ? currentUser.getId() : -1;
    }
}

package utils;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Singleton service that manages in-app notifications.
 * Any controller can push a notification; the bell badge
 * reacts automatically via JavaFX property binding.
 */
public class NotificationService {

    // ── Singleton ──────────────────────────────────────────────────────────────
    private static NotificationService instance;

    public static NotificationService getInstance() {
        if (instance == null) instance = new NotificationService();
        return instance;
    }

    private NotificationService() {}

    // ── Model ──────────────────────────────────────────────────────────────────
    public static class AppNotification {
        private final String message;
        private final String type;   // "course" | "chapter" | "info"
        private final String time;
        private boolean read;

        public AppNotification(String message, String type) {
            this.message = message;
            this.type    = type;
            this.time    = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("HH:mm · dd MMM"));
            this.read    = false;
        }

        public String getMessage() { return message; }
        public String getType()    { return type; }
        public String getTime()    { return time; }
        public boolean isRead()    { return read; }
        public void markRead()     { this.read = true; }
    }

    // ── State ──────────────────────────────────────────────────────────────────
    private final ObservableList<AppNotification> notifications =
            FXCollections.observableArrayList();

    /** Unread count — bind the bell badge to this. */
    private final IntegerProperty unreadCount = new SimpleIntegerProperty(0);

    // ── API ────────────────────────────────────────────────────────────────────

    public void push(String message, String type) {
        notifications.add(0, new AppNotification(message, type)); // newest first
        unreadCount.set(unreadCount.get() + 1);
    }

    /** Convenience helpers */
    public void pushCourse(String courseTitle, String action) {
        push("📚 Course " + action + ": \"" + courseTitle + "\"", "course");
    }

    public void pushChapter(String chapterTitle, String courseTitle, String action) {
        push("📖 Chapter " + action + ": \"" + chapterTitle + "\" in " + courseTitle, "chapter");
    }

    public void markAllRead() {
        notifications.forEach(AppNotification::markRead);
        unreadCount.set(0);
    }

    public void clearAll() {
        notifications.clear();
        unreadCount.set(0);
    }

    public ObservableList<AppNotification> getNotifications() { return notifications; }
    public IntegerProperty unreadCountProperty()              { return unreadCount; }
    public int getUnreadCount()                               { return unreadCount.get(); }
}
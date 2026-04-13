package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDatabase {

    private static final String URL      = "jdbc:mysql://localhost:3306/aiva";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "";

    private Connection connection;
    private static MyDatabase instance;

    // ── Singleton ────────────────────────────────────────────────────────────
    public static synchronized MyDatabase getInstance() {
        if (instance == null) {
            instance = new MyDatabase();
        }
        return instance;
    }

    private MyDatabase() {
        connect();
    }

    // ── Internal connect ─────────────────────────────────────────────────────
    private void connect() {
        try {
            connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            System.out.println("Connected to database successfully");
        } catch (SQLException e) {
            System.out.println("Database connection FAILED: " + e.getMessage());
            connection = null;
        }
    }

    // ── Public getter — always returns a live connection ─────────────────────
    public Connection getConnection() {
        try {
            // isValid(2) sends a ping — reconnects if dead or null
            if (connection == null || !connection.isValid(2)) {
                System.out.println("Connection lost — reconnecting...");
                connect();
            }
        } catch (SQLException e) {
            System.out.println("isValid check failed — reconnecting: " + e.getMessage());
            connect();
        }
        return connection;
    }
}
package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDatabase {
    private final String URL = "jdbc:mysql://localhost:3306/aiva";
    private final String USERNAME = "root";
    private final String PASSWORD = "root"; // <-- ADD YOUR PASSWORD HERE IF YOU HAVE ONE
    private Connection connection;
    private static MyDatabase instance;

    public static MyDatabase getInstance() {
        if (instance == null) {
            instance = new MyDatabase();
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }

    private MyDatabase() {
        try {
            connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            System.out.println("Connected to database successfully");
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
            connection = null; // Explicitly set to null on failure
        }
    }
}
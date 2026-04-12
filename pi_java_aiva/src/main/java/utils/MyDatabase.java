package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDatabase {
    private   final String URl = "jdbc:mysql://localhost:3306/aiva";
    private final String USERNAME = "root";
    private final String PASSWORD = "";
    private Connection connection;
    private  static MyDatabase instance ;

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
            System.out.println("Attempting to connect to database at: " + URl);
            connection = DriverManager.getConnection(URl, USERNAME, PASSWORD);
            if (connection != null) {
                System.out.println("✅ Connected to database successfully.");
            } else {
                System.out.println("❌ Connection failed: connection object is null.");
            }
        } catch (SQLException e) {
            System.out.println("❌ Database Connection Error: " + e.getMessage());
            System.out.println("State: " + e.getSQLState());
            System.out.println("Error Code: " + e.getErrorCode());
        }
    }
}

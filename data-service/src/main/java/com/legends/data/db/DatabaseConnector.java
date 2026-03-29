package db;

import java.sql.*;

/**
 * Singleton that provides JDBC connections.
 */
public class DatabaseConnector {
    private static final String URL  = "jdbc:mysql://localhost:3306/legends_db";
    private static final String USER = "root";
    private static final String PASS = "1234";
    private static DatabaseConnector instance;

    private DatabaseConnector() {}
    public static DatabaseConnector getInstance() {
        if (instance==null) instance=new DatabaseConnector(); return instance;
    }
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}

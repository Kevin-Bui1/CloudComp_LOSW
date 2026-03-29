package db;
import java.sql.Connection;
public interface IDBConnection {
    Connection getConnection() throws java.sql.SQLException;
}

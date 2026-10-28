package LAB3;

import java.sql.SQLException;


public interface PinDAO {
    void createTableIfNotExists() throws SQLException;
    boolean hasPin(String customerId) throws SQLException;
    void savePin(String customerId, String pin) throws SQLException;      // create or change
    boolean verifyPin(String customerId, String pin) throws SQLException;
}
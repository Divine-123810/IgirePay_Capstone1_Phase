package LAB2;

import java.sql.Connection;
import java.sql.SQLException;

public interface ProcessedRequestDAO {
    boolean CreateTable () throws SQLException;
    public boolean isProcessed(String referenceId) throws SQLException;
    boolean markProcessed(Connection conn, String referenceId) throws SQLException;


}

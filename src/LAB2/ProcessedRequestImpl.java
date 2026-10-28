package LAB2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ProcessedRequestImpl implements ProcessedRequestDAO {

    @Override
    public boolean CreateTable() throws SQLException {
        String createTable = """ 
                        CREATE TABLE processed_requests (
                        id SERIAL PRIMARY KEY,
                        reference_id VARCHAR(64) NOT NULL UNIQUE,
                        processed_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP)
                """;
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(createTable)) {
            statement.executeUpdate();

            return true;
        }
    }

    @Override
    public boolean isProcessed(String referenceId) throws SQLException {
        String sql = "SELECT * FROM processed_requests WHERE reference_id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, referenceId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }
    @Override
    public boolean markProcessed(Connection conn, String referenceId) throws SQLException {
        String sql = "INSERT INTO processed_requests(reference_id) VALUES (?) "
                + "ON CONFLICT (reference_id) DO NOTHING";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, referenceId);
            return ps.executeUpdate() == 1;
        }
    }

}

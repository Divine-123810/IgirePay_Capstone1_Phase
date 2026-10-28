package LAB3;

import LAB2.DBConnection;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.*;
import java.util.HexFormat;


public class PinDAOImpl implements PinDAO {

    @Override
    public void createTableIfNotExists() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS customer_pins (
                    customer_id VARCHAR(50) PRIMARY KEY
                        REFERENCES customers(customer_id) ON DELETE CASCADE,
                    pin_hash    VARCHAR(64) NOT NULL,
                    salt        VARCHAR(64) NOT NULL,
                    updated_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """;
        try (Connection connection = DBConnection.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    @Override
    public boolean hasPin(String customerId) throws SQLException {
        String sql = "SELECT 1 FROM customer_pins WHERE customer_id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, customerId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    @Override
    public void savePin(String customerId, String pin) throws SQLException {
        String salt = newSalt();
        String sql = """
                INSERT INTO customer_pins (customer_id, pin_hash, salt) VALUES (?, ?, ?)
                ON CONFLICT (customer_id)
                DO UPDATE SET pin_hash = EXCLUDED.pin_hash, salt = EXCLUDED.salt, updated_at = NOW()
                """;
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, customerId);
            statement.setString(2, hash(pin, salt));
            statement.setString(3, salt);
            statement.executeUpdate();
        }
    }

    @Override
    public boolean verifyPin(String customerId, String pin) throws SQLException {
        String sql = "SELECT pin_hash, salt FROM customer_pins WHERE customer_id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, customerId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }
                String savedHash = rs.getString("pin_hash");
                String typedHash = hash(pin, rs.getString("salt"));
                return MessageDigest.isEqual(savedHash.getBytes(), typedHash.getBytes());
            }
        }
    }

    private String newSalt() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }


    private String hash(String pin, String salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(pin.toCharArray(), salt.getBytes(), 100_000, 256);
            byte[] hash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            return HexFormat.of().formatHex(hash);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Could not hash the PIN", e);
        }
    }


}
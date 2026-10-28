
package LAB2;

import LAB1.Transaction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TransactionDAOImp implements TransactionDAO {

    @Override
    public boolean CreateTable() throws SQLException {
        String createTransaction = """
            CREATE TABLE IF NOT EXISTS transactions (
                transaction_id   SERIAL PRIMARY KEY,
                account_id       VARCHAR(50) NOT NULL,
                reference_id     VARCHAR(100) NOT NULL,
                transaction_type VARCHAR(50) NOT NULL,
                amount           DECIMAL(20,4) NOT NULL,
                created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (account_id) REFERENCES accounts(account_id)
            )
            """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(createTransaction)) {

            statement.executeUpdate();
            return true;
        }
    }

    // Find transactions belonging to one account
    @Override
    public List<Transaction> findByAccount(String id) throws SQLException {
        String sql = """
            SELECT * FROM transactions
            WHERE account_id = ?
            ORDER BY created_at
            """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                return mapTransactions(resultSet);
            }
        }
    }

    // Find all transactions
    @Override
    public List<Transaction> findAll() throws SQLException {
        String sql = "SELECT * FROM transactions ORDER BY created_at";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            return mapTransactions(resultSet);
        }
    }

    // Find transactions on a particular date
    @Override
    public List<Transaction> findByDate(LocalDate date)
            throws SQLException {

        String sql = """
            SELECT * FROM transactions
            WHERE created_at >= ? AND created_at < ?
            ORDER BY created_at
            """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setTimestamp(1, Timestamp.valueOf(date.atStartOfDay()));
            statement.setTimestamp(2,
                    Timestamp.valueOf(date.plusDays(1).atStartOfDay()));

            try (ResultSet resultSet = statement.executeQuery()) {
                return mapTransactions(resultSet);
            }
        }
    }

    // Find transactions belonging to a particular customer
    @Override
    public List<Transaction> findByCustomer(String customerId)
            throws SQLException {

        String sql = """
            SELECT t.*
            FROM transactions t
            INNER JOIN accounts a ON t.account_id = a.account_id
            WHERE a.customer_id = ?
            ORDER BY t.created_at
            """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, customerId);

            try (ResultSet resultSet = statement.executeQuery()) {
                return mapTransactions(resultSet);
            }
        }
    }

    // Convert database rows into Transaction objects
    private List<Transaction> mapTransactions(ResultSet resultSet)
            throws SQLException {

        List<Transaction> transactions = new ArrayList<>();

        while (resultSet.next()) {
            Transaction transaction = new Transaction();

            transaction.setTransactionId(
                    resultSet.getInt("transaction_id"));

            transaction.setAccountId(
                    resultSet.getString("account_id"));

            transaction.setReferenceId(
                    resultSet.getString("reference_id"));

            transaction.setTransactionType(
                    resultSet.getString("transaction_type"));

            transaction.setAmount(
                    resultSet.getDouble("amount"));

            transaction.setTimestamp(
                    resultSet.getObject(
                            "created_at",
                            java.time.LocalDateTime.class));

            transactions.add(transaction);
        }

        return transactions;
    }
}

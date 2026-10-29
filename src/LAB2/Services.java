package LAB2;

import java.sql.*;

public class Services {

    private final ProcessedRequestImpl processedRequest = new ProcessedRequestImpl();


    public boolean deposit(String accountId, double amount, String referenceId)
            throws SQLException {

        validate(amount, referenceId);

        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {
                if (!processedRequest.markProcessed(connection, referenceId)) {
                    connection.rollback();
                    return false;
                }

                double balance = getBalance(connection, accountId);
                String accountType;
                String sql = "SELECT account_type FROM accounts WHERE account_id = ?";

                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setString(1, accountId);

                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (!resultSet.next()) {
                            throw new IllegalArgumentException("Account not found.");
                        }

                        accountType = resultSet.getString("account_type");
                    }
                }
                if ("SAVING".equalsIgnoreCase(accountType) && amount <= 1000) {
                    throw new IllegalArgumentException(
                            "Deposit into a saving account must be greater than 1000 RWF.");
                }

                setBalance(connection, accountId, balance + amount);
                saveTransaction(connection, accountId, referenceId, "DEPOSIT", amount);

                connection.commit();
                return true;

            } catch (Exception e) {
                connection.rollback();
                throw e;
            }
        }
    }


    public boolean withdraw(String accountId, double amount, String referenceId)
            throws SQLException {

        validate(amount, referenceId);

        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {
                if (!processedRequest.markProcessed(connection, referenceId)) {
                    connection.rollback();
                    return false;
                }

                double balance = getBalance(connection, accountId);

                String accountType;
                String sql = "SELECT account_type FROM accounts WHERE account_id = ?";

                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setString(1, accountId);

                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (!resultSet.next()) {
                            throw new IllegalArgumentException("Account not found.");
                        }

                        accountType = resultSet.getString("account_type");
                    }
                }
                if (balance < amount) {
                    throw new IllegalStateException(
                            "Insufficient funds. Balance: " + balance);
                }
                if ("SAVING".equalsIgnoreCase(accountType)
                        && balance - amount < 1000) {
                    throw new IllegalStateException(
                            "A saving account must maintain a minimum balance of 1000 RWF.");
                }

                setBalance(connection, accountId, balance - amount);
                saveTransaction(connection, accountId, referenceId, "WITHDRAWAL", amount);

                connection.commit();
                return true;

            } catch (Exception e) {
                connection.rollback();
                throw e;
            }
        }
    }


    public boolean transfer(String fromAccountId, String toAccountId, double amount, String referenceId)
            throws SQLException {
        validate(amount, referenceId);
        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException("Cannot send money to the same account");
        }

        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                if (!processedRequest.markProcessed(connection, referenceId)) {
                    connection.rollback();
                    return false;
                }
                double fromBalance = getBalance(connection, fromAccountId);
                double toBalance = getBalance(connection, toAccountId);
                if (fromBalance < amount) {
                    throw new IllegalStateException("Insufficient funds. Balance: " + fromBalance);
                }
                setBalance(connection, fromAccountId, fromBalance - amount);
                setBalance(connection, toAccountId, toBalance + amount);
                saveTransaction(connection, fromAccountId, referenceId, "WITHDRAWAL", amount);
                saveTransaction(connection, toAccountId, referenceId, "DEPOSIT", amount);

                connection.commit();
                return true;
            } catch (Exception e) {
                connection.rollback();
                throw e;
            }
        }
    }

    private void validate(double amount, String referenceId) {
        if (!Double.isFinite(amount) || amount <= 0) {
            throw new IllegalArgumentException(
                    "Amount must be a finite number greater than zero");
        }

        if (referenceId == null ||
                !referenceId.matches("[A-Za-z0-9_-]{3,50}")) {
            throw new IllegalArgumentException("Invalid reference id");
        }
    }

    private double getBalance(Connection connection, String accountId) throws SQLException {
        String sql = "SELECT balance FROM accounts WHERE account_id = ? FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, accountId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("Account " + accountId + " does not exist");
                }
                return rs.getDouble("balance");
            }
        }
    }

    private void setBalance(Connection connection, String accountId, double newBalance) throws SQLException {
        String sql = "UPDATE accounts SET balance = ? WHERE account_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDouble(1, newBalance);
            statement.setString(2, accountId);
            statement.executeUpdate();
        }
    }

    private void saveTransaction(Connection connection, String accountId, String referenceId,
                                 String type, double amount) throws SQLException {
        String sql = "INSERT INTO transactions (account_id, reference_id, transaction_type, amount) "
                + "VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, accountId);
            statement.setString(2, referenceId);
            statement.setString(3, type);
            statement.setDouble(4, amount);
            statement.executeUpdate();
        }
    }

//    public static void main(String[] args) {
//        Services wallet = new Services();
//
//        try {
//            System.out.println("Deposit 50000:          " + wallet.deposit("1", 50000, "DEP-001"));
//            System.out.println("Same deposit again:     " + wallet.deposit("1", 50000, "DEP-001"));
//            System.out.println("Withdraw 20000:         " + wallet.withdraw("1", 20000, "WD-001"));
//            System.out.println("Send 20000 from 1 to 2: " + wallet.transfer("1", "2", 20000, "TRF-001"));
//        } catch (Exception e) {
//            System.out.println("Error: " + e.getMessage());
//        }
//
//        try {
//            wallet.withdraw("1", 999999, "WD-002");
//        } catch (Exception e) {
//            System.out.println("Withdraw 999999:        " + e.getMessage());
//        }
//
//        try {
//            wallet.deposit("1", 4000000, "x' OR '1'='1");
//        } catch (Exception e) {
//            System.out.println("Bad reference id:       " + e.getMessage());
//        }
//    }
}
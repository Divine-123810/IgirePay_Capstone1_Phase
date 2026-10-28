package LAB2;

import LAB1.Account;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AccountDAOImpl implements AccountDAO {

    @Override
    public boolean CreateTable() throws SQLException {

        String createSql = """
            CREATE TABLE accounts (
                account_id VARCHAR(50) PRIMARY KEY,
                customer_id VARCHAR(5) NOT NULL,
                account_type VARCHAR(255) NOT NULL CHECK(account_type IN('SAVING','WALLET')),
                balance DECIMAL(20,5) NOT NULL,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (customer_id)
                    REFERENCES customers(customer_id)
            )
            """;

        try (Connection connection = DBConnection.getConnection();
             Statement statement = connection.createStatement()) {

            statement.executeUpdate(createSql);
            return true;
        }
    }

    @Override
    public Account registerAccount(Account account) throws SQLException {

        String checkSql = """
            SELECT 1
            FROM customers
            WHERE customer_id = ?
            """;

        String insertSql = """
            INSERT INTO accounts
                (account_id, account_type, balance, created_at, customer_id)
            VALUES (?, ?, ?, ?, ?)
            """;

        try (Connection connection = DBConnection.getConnection()) {
            try (PreparedStatement check = connection.prepareStatement(checkSql)) {

                check.setString(1, account.getCustomerId());

                try (ResultSet result = check.executeQuery()) {
                    if (!result.next()) {
                        throw new SQLException(
                                "Customer ID " + account.getCustomerId()
                                        + " does not exist in the customers table."
                        );
                    }
                }
            }

            // Insert the account
            try (PreparedStatement statement =
                         connection.prepareStatement(insertSql)) {

                statement.setString(1, account.getAccountId());
                statement.setString(2, account.getAccountType());
                statement.setDouble(3, account.getBalance());
                statement.setObject(4, account.getCreatedAt());
                statement.setString(5, account.getCustomerId());

                statement.executeUpdate();
            }
        }

        return account;
    }

    @Override
    public Account findById(String id) throws SQLException {
        String findAccountSql = "SELECT * FROM accounts WHERE account_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(findAccountSql)) {

            statement.setString(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    Account account = new Account();
                    account.setAccountId(resultSet.getString("account_id"));
                    account.setAccountType(resultSet.getString("account_type"));
                    account.setBalance(resultSet.getDouble("balance"));
                    account.setCreatedAt(resultSet.getObject("created_at", LocalDateTime.class));
                    account.setCustomerId(resultSet.getString("customer_id"));
                    return account;
                }
            }
        }
        return null;

    }

    @Override
    public List<Account> findAll() throws SQLException {
        String display = "select * from accounts";
        List<Account> accounts = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(display);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                Account account = new Account();
                account.setAccountId(resultSet.getString("account_Id"));
                account.setAccountType(resultSet.getString("account_type"));
                account.setBalance(resultSet.getDouble("balance"));
                account.setCreatedAt(resultSet.getObject("created_at", LocalDateTime.class));            account.setCustomerId(resultSet.getString("customer_id"));
                accounts.add(account);
            }
        }
        return accounts;
    }

    @Override
    public void registerAccount(String accountId, String customerId, String type) throws SQLException {
        String insertSql = "INSERT INTO accounts (account_id, customer_id, account_type, balance) VALUES (?, ?, ?, 0)";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(insertSql)) {
            statement.setString(1, accountId);
            statement.setString(2, customerId);
            statement.setString(3, type);
            statement.executeUpdate();
        }
    }

    @Override
    public List<Account> findByCustomer(String customerId) throws SQLException {
        String sql = "SELECT * FROM accounts WHERE customer_id = ? ORDER BY account_id";
        List<Account> accounts = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, customerId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Account account = new Account();
                    account.setAccountId(resultSet.getString("account_id"));
                    account.setAccountType(resultSet.getString("account_type"));
                    account.setBalance(resultSet.getDouble("balance"));
                    account.setCreatedAt(resultSet.getObject("created_at", LocalDateTime.class));
                    account.setCustomerId(resultSet.getString("customer_id"));
                    accounts.add(account);
                }
            }
        }
        return accounts;
    }

    @Override
    public int deleteInactiveAccounts(int days) throws SQLException {
        String sql = """
                DELETE FROM accounts a
                WHERE a.balance = 0
                  AND a.created_at < NOW() - make_interval(days => ?)
                  AND NOT EXISTS (SELECT 1 FROM transactions t WHERE t.account_id = a.account_id)
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, days);
            return statement.executeUpdate();
        }
    }


    @Override
    public boolean updateAccount (Account account) throws SQLException {
        String updateSql = """
            update accounts
            set customer_id = ?, account_type = ?, balance= ?,created_at =?
            where account_id = ?
            """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(updateSql)) {

            statement.setString(1, account.getCustomerId());   // 1st ?
            statement.setString(2, account.getAccountType());          // 2nd ?
            statement.setDouble(3, account.getBalance());
            statement.setObject(4, account.getCreatedAt());     // 4th ?
            statement.setString(5, account.getAccountId());     // 5th ? (WHERE)

            int rowsUpdated = statement.executeUpdate();

            if (rowsUpdated == 0) {
                System.out.println("Account " + account.getAccountId() + " is not found");
                return false;
            }

            System.out.println(" Account" + account.getAccountId() + " updated");
        }
        return true;
    }

    @Override
    public boolean deleteAccount (String id) throws SQLException {

        String deleteSql= "delete from accounts where account_id =?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(deleteSql)) {

            statement.setString(1, id);
            return statement.executeUpdate() == 1;
        }
    }

}
package LAB2;

import LAB1.Account;


import java.sql.SQLException;
import java.util.List;

public interface AccountDAO {


    boolean CreateTable()throws SQLException;
    Account registerAccount(Account account) throws SQLException;
    Account findById(String id) throws SQLException;
    List<Account> findAll() throws SQLException;
    boolean updateAccount(Account account) throws SQLException;
    boolean deleteAccount(String id) throws SQLException;

    void registerAccount(String accountId, String customerId, String type) throws SQLException;
    List<Account> findByCustomer(String customerId) throws SQLException;
    int deleteInactiveAccounts(int days) throws SQLException;
}
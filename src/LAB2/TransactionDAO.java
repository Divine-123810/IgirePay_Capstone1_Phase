
package LAB2;

import LAB1.Transaction;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public interface TransactionDAO {

    boolean CreateTable() throws SQLException;

    List<Transaction> findByAccount(String id) throws SQLException;

    List<Transaction> findAll() throws SQLException;
    List<Transaction> findByDate(LocalDate date) throws SQLException;
    List<Transaction> findByCustomer(String customerId) throws SQLException;
}

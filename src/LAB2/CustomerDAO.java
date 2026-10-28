package LAB2;

import LAB1.Customer;

import java.sql.SQLException;
import java.util.List;

public interface CustomerDAO {

    boolean CreateTable()throws SQLException;
    Customer registerCustomer(Customer customer) throws SQLException;
    Customer findById(String id) throws SQLException;
    Customer findByEmail(String email) throws SQLException;
    List<Customer> findAll() throws SQLException;
    boolean updateCustomer(Customer customer) throws SQLException;
    boolean deleteCustomer(String id) throws SQLException;
}
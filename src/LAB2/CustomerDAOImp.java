package LAB2;

import LAB1.Customer;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CustomerDAOImp implements CustomerDAO {

    @Override
    public boolean CreateTable() throws SQLException{
        String CreateSql = """
                create table customers(customer_Id varchar(50) primary key,
                customer_Name varchar(255) not null,
                email varchar(255) not null,
                phone_number varchar(50) not null)

                """;

        try (Connection connection = DBConnection.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(CreateSql);
        }
        return true;
    }



    @Override
    public Customer registerCustomer(Customer customer) throws SQLException {
        String insertSql= """
                insert into customers(customer_Id,customer_Name,email,phone_number)
                values(?,?,?,?)
               """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement st= connection.prepareStatement(insertSql)) {
            st.setString(1,customer.getCustomerId());
            st.setString(2,customer.getCustomerName());
            st.setString(3,customer.getEmail());
            st.setString(4,customer.getPhoneNumber());

            st.executeUpdate();
        }

        return customer;

    }

    @Override

    public Customer findById(String id) throws SQLException {
        String findCustomerSql = "select * from customers where customer_Id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(findCustomerSql)) {

            statement.setString(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    Customer customer = new Customer();
                    customer.setCustomerId(resultSet.getString("customer_Id"));
                    customer.setCustomerName(resultSet.getString("customer_Name"));
                    customer.setEmail(resultSet.getString("email"));
                    customer.setPhoneNumber(resultSet.getString("phone_number"));
                    return customer;
                }
            }
        }
        return null;
    }

    @Override
    public Customer findByEmail(String email) throws SQLException {
        String findCustomerSql = "select * from customers where lower(email) = lower(?)";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(findCustomerSql)) {

            statement.setString(1, email.trim());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    Customer customer = new Customer();
                    customer.setCustomerId(resultSet.getString("customer_Id"));
                    customer.setCustomerName(resultSet.getString("customer_Name"));
                    customer.setEmail(resultSet.getString("email"));
                    customer.setPhoneNumber(resultSet.getString("phone_number"));
                    return customer;
                }
            }
        }
        return null;
    }

    @Override
    public List<Customer> findAll() throws SQLException {
        String display="select * from customers";
        List<Customer> allCustomer=new ArrayList<>();

        try (Connection connection= DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(display);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                Customer customer = new Customer();
                customer.setCustomerId(resultSet.getString("customer_Id"));
                customer.setCustomerName(resultSet.getString("customer_Name"));
                customer.setEmail(resultSet.getString("email"));
                customer.setPhoneNumber(resultSet.getString("phone_number"));
                allCustomer.add(customer);
            }
        }
        return allCustomer;
    }


    @Override
    public boolean updateCustomer(Customer customer) throws SQLException {
        String updateSql = """
            update customers
            set customer_Name = ?, email = ?, phone_number = ?
            where customer_Id = ?
            """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(updateSql)) {

            statement.setString(1, customer.getCustomerName());   // 1st ?
            statement.setString(2, customer.getEmail());          // 2nd ?
            statement.setString(3, customer.getPhoneNumber());    // 3rd ?
            statement.setString(4, customer.getCustomerId());     // 4th ? (WHERE)

            int rowsUpdated = statement.executeUpdate();

            if (rowsUpdated == 0) {
                System.out.println("Customer " + customer.getCustomerId() + " is not found");
                return false;
            }

            System.out.println("Customer " + customer.getCustomerId() + " updated");
            return true;
        }
    }

    @Override
    public boolean deleteCustomer(String id) throws SQLException {
        String deleteSql= "delete from customers where customer_id =?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(deleteSql)) {

            statement.setString(1, id);
            return statement.executeUpdate() == 1;
        }
    }

}
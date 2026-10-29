package LAB3;

import LAB2.AccountDAO;
import LAB2.AccountDAOImpl;
import LAB2.CustomerDAO;
import LAB2.CustomerDAOImp;
import LAB2.DBConnection;
import LAB2.Services;
import LAB2.TransactionDAO;
import LAB2.TransactionDAOImp;
import LAB1.Account;
import LAB1.Customer;
import LAB1.Transaction;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.UUID;

public class Main {

    private static final Scanner scanner = new Scanner(System.in);
    private static final Random random = new Random();
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final CustomerDAO customerDAO = new CustomerDAOImp();
    private static final AccountDAO accountDAO = new AccountDAOImpl();
    private static final TransactionDAO transactionDAO = new TransactionDAOImp();
    private static final Services wallet = new Services();
    private static final TransactionReportService reportService = new TransactionReportService();
    private static final PinDAO pinDAO = new PinDAOImpl();

    private static Customer loggedIn;

    public static void main(String[] args) {

        System.out.println("        IgirePay PayMents  -  Digital Wallet (Rwanda)      ");

        if (!prepareDatabase()) {
            return;
        }

        while (true) {
            System.out.println();
            System.out.println("---------------- WELCOME ----------------");
            System.out.println(" 1. Register as a new customer");
            System.out.println(" 2. Login");
            System.out.println(" 0. Exit");
            String choice = ask("Choose");

            try {
                switch (choice) {
                    case "1" -> register();
                    case "2" -> login();
                    case "0" -> {
                        System.out.println("Thank you for using IgirePay!");
                        return;
                    }
                    default -> System.out.println("[!] Invalid option.");
                }
            } catch (SQLException e) {
                showDatabaseError(e);
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.out.println("[!] " + e.getMessage());
            }
        }
    }

    private static boolean prepareDatabase() {
        try (Connection connection = DBConnection.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS processed_requests (
                        id  SERIAL PRIMARY KEY,
                        reference_id VARCHAR(100) NOT NULL UNIQUE,
                        processed_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
            statement.executeUpdate("CREATE SEQUENCE IF NOT EXISTS customer_id_seq START 1001");
            pinDAO.createTableIfNotExists();
            return true;
        } catch (SQLException e) {
            System.out.println("[!] Cannot connect to the database.");
            System.out.println(" Reason: " + e.getMessage());
            System.out.println(" Check that PostgreSQL is running, that the database 'IgirePay' exists,");
            System.out.println(" and that the user/password in LAB2/DBConnection.java are correct.");
            return false;
        }
    }


    private static void register() throws SQLException {
        System.out.println();
        System.out.println("---------- NEW CUSTOMER ----------");
        String name = askRequired("Full name");
        String email = askMatching("Email", "[^@\\s]+@[^@\\s]+\\.[^@\\s]+", "Enter a valid email, e.g. name@example.com");
        if (customerDAO.findByEmail(email) != null) {
            System.out.println("[!] This email is already registered. Please log in instead.");
            return;
        }
        String phone = askMatching("Phone number (07XXXXXXXX)", "07\\d{8}", "Enter 10 digits starting with 07");

        String customerId = newCustomerId();
        Customer customer = new Customer(customerId, name, email, phone);
        customerDAO.registerCustomer(customer);

        System.out.println();
        System.out.println("Now create a 4-digit PIN. You will use it to log in and to confirm payments.");
        pinDAO.savePin(customerId, askNewPin());

        System.out.println();
        System.out.println("[OK] Registration complete!");
        System.out.println("     Your customer ID is: " + customerId);
        System.out.println("     To log in, use your email (" + email + ") and your PIN.");

        if (askYesNo("Open a WALLET account now?")) {
            String accountId = newAccountId("WALLET");
            accountDAO.registerAccount(accountId, customerId, "WALLET");
            System.out.println("[OK] Wallet account " + accountId + " opened with balance 0.00 RWF.");
        }
    }

    private static void login() throws SQLException {
        System.out.println();
        System.out.println("---------- LOGIN ----------");
        Customer customer = customerDAO.findByEmail(ask("Email"));
        if (customer == null) {
            System.out.println("[!] No customer with that email. Register first (option 1).");
            return;
        }

        if (!pinDAO.hasPin(customer.getCustomerId())) {
            System.out.println("This customer has no PIN yet. Confirm your phone number to create one.");
            if (!ask("Phone number on file").equals(customer.getPhoneNumber())) {
                System.out.println("[!] Phone number does not match.");
                return;
            }
            pinDAO.savePin(customer.getCustomerId(), askNewPin());
            System.out.println("[OK] PIN created.");
        }

        for (int attemptsLeft = 3; attemptsLeft > 0; attemptsLeft--) {
            if (pinDAO.verifyPin(customer.getCustomerId(), ask("PIN"))) {
                loggedIn = customer;
                dashboard();
                return;
            }
            System.out.println("[!] Wrong PIN. Attempts left: " + (attemptsLeft - 1));
        }
        System.out.println("[!] Too many wrong attempts. Back to the welcome screen.");
    }


    private static void dashboard() {
        System.out.println();
        System.out.println("[OK] Welcome, " + loggedIn.getCustomerName() + "!");

        while (true) {
            System.out.println();
            System.out.println("=========== " + loggedIn.getCustomerName() + " (" + loggedIn.getCustomerId() + ") ===========");
            System.out.println("-- My accounts --");
            System.out.println(" 1. Open a new account");
            System.out.println(" 2. View my accounts");
            System.out.println(" 3. View account details / balance");
            System.out.println(" 4. Change account type");
            System.out.println(" 5. Delete an inactive account");
            System.out.println("-- Money --");
            System.out.println(" 6. Deposit");
            System.out.println(" 7. Withdraw");
            System.out.println(" 8. Transfer to another account");
            System.out.println(" 9. Transaction history");
            System.out.println("13. Export transaction history to CSV");
            System.out.println("14. View daily transaction summary");
            System.out.println("15. View customer transaction statement");
            System.out.println("-- Profile & security --");
            System.out.println("10. View my profile");
            System.out.println("11. Update my details");
            System.out.println("12. Change PIN");
            System.out.println(" 0. Logout");
            String choice = ask("Choose");

            try {
                switch (choice) {
                    case "1" -> openAccount();
                    case "2" -> viewMyAccounts();
                    case "3" -> viewAccountDetails();
                    case "4" -> changeAccountType();
                    case "5" -> deleteInactiveAccount();
                    case "6" -> deposit();
                    case "7" -> withdraw();
                    case "8" -> transfer();
                    case "9" -> history();
                    case "13" -> exportTransactionHistory();
                    case "14" -> dailyTransactionSummary();
                    case "15" -> customerTransactionStatement();
                    case "10" -> viewProfile();
                    case "11" -> updateProfile();
                    case "12" -> changePin();
                    case "0" -> {
                        System.out.println("[OK] Logged out. Goodbye, " + loggedIn.getCustomerName() + ".");
                        loggedIn = null;
                        return;
                    }
                    default -> System.out.println("[!] Invalid option.");
                }
            } catch (SQLException e) {
                showDatabaseError(e);
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.out.println("[!] " + e.getMessage());
            }
        }
    }

    private static void openAccount() throws SQLException {
        System.out.println(" 1. Wallet account");
        System.out.println(" 2. Savings account");
        String choice = ask("Account type");
        String type;
        if (choice.equals("1")) {
            type = "WALLET";
        } else if (choice.equals("2")) {
            type = "SAVING";
        } else {
            System.out.println("[!] Invalid account type.");
            return;
        }
        String accountId = newAccountId(type);
        accountDAO.registerAccount(accountId, loggedIn.getCustomerId(), type);
        System.out.println("[OK] " + type + " account " + accountId + " opened with balance 0.00 RWF.");
    }

    /** READ (all my accounts) */
    private static void viewMyAccounts() throws SQLException {
        List<Account> accounts = accountDAO.findByCustomer(loggedIn.getCustomerId());
        if (accounts.isEmpty()) {
            System.out.println("You have no accounts yet. Choose option 1 to open one.");
            return;
        }
        System.out.println();
        System.out.printf("%-10s %-9s %20s   %s%n", "ACCOUNT", "TYPE", "BALANCE", "OPENED");
        double total = 0;
        for (Account a : accounts) {
            System.out.printf("%-10s %-9s %20s   %s%n", a.getAccountId(), a.getAccountType(),
                    money(a.getBalance()), a.getCreatedAt().format(DATE));
            total += a.getBalance();
        }
        System.out.printf("%-20s %20s%n", "TOTAL", money(total));
    }


    private static void viewAccountDetails() throws SQLException {
        Account account = chooseMyAccount("Account ID");
        if (account == null) return;

        int count = transactionDAO.findByAccount(account.getAccountId()).size();
        System.out.println();
        System.out.println("Account     : " + account.getAccountId());
        System.out.println("Type        : " + account.getAccountType());
        System.out.println("Balance     : " + money(account.getBalance()));
        System.out.println("Opened      : " + account.getCreatedAt().format(DATE));
        System.out.println("Transactions: " + count);
    }

    private static void changeAccountType() throws SQLException {
        Account account = chooseMyAccount("Account ID");
        if (account == null) return;

        String newType = account.getAccountType().equals("WALLET") ? "SAVINGS" : "WALLET";
        if (!askYesNo("Change account " + account.getAccountId() + " from "
                + account.getAccountType() + " to " + newType + "?")) {
            System.out.println("Cancelled.");
            return;
        }
        account.setAccountType(newType);
        accountDAO.updateAccount(account);
        System.out.println("[OK] Account " + account.getAccountId() + " is now a " + newType + " account.");
    }

    private static void deleteInactiveAccount() throws SQLException {
        Account account = chooseMyAccount("Account ID to delete");
        if (account == null) return;

        if (account.getBalance() != 0) {
            System.out.println("[!] Account still holds " + money(account.getBalance())
                    + ". Withdraw or transfer the money first.");
            return;
        }
        if (!transactionDAO.findByAccount(account.getAccountId()).isEmpty()) {
            System.out.println("[!] This account has transaction history, so it is kept for your records.");
            return;
        }
        if (!askYesNo("Delete account " + account.getAccountId() + " permanently?")) {
            System.out.println("Cancelled.");
            return;
        }
        accountDAO.deleteAccount(account.getAccountId());
        System.out.println("[OK] Account " + account.getAccountId() + " deleted.");
    }


    private static void deposit() throws SQLException {
        Account account = chooseMyAccount("Deposit to account");
        if (account == null) return;
        double amount = askAmount();
        String reference = askReference("DEP");

        boolean processed = wallet.deposit(account.getAccountId(), amount, reference);
        showResult(processed, reference, account.getAccountId());
    }

    private static void withdraw() throws SQLException {
        Account account = chooseMyAccount("Withdraw from account");
        if (account == null) return;
        double amount = askAmount();
        if (!confirmWithPin()) return;
        String reference = askReference("WD");

        boolean processed = wallet.withdraw(account.getAccountId(), amount, reference);
        showResult(processed, reference, account.getAccountId());
    }

    private static void transfer() throws SQLException {
        Account from = chooseMyAccount("From my account");
        if (from == null) return;

        Account to = accountDAO.findById(ask("To account ID"));
        if (to == null) {
            System.out.println("[!] Destination account not found.");
            return;
        }
        if (to.getAccountId().equals(from.getAccountId())) {
            System.out.println("[!] Cannot transfer to the same account.");
            return;
        }
        Customer receiver = customerDAO.findById(to.getCustomerId());
        double amount = askAmount();

        String receiverName = (receiver != null) ? receiver.getCustomerName() : "unknown";
        if (!askYesNo("Send " + money(amount) + " to " + receiverName + " (" + to.getAccountId() + ")?")) {
            System.out.println("Cancelled.");
            return;
        }
        if (!confirmWithPin()) return;
        String reference = askReference("TRF");

        boolean processed = wallet.transfer(from.getAccountId(), to.getAccountId(), amount, reference);
        showResult(processed, reference, from.getAccountId());
    }

    private static void history() throws SQLException {
        Account account = chooseMyAccount("Account ID");
        if (account == null) return;

        List<Transaction> list = transactionDAO.findByAccount(account.getAccountId());
        System.out.println();
        System.out.println("History of " + account.getAccountId() + " (" + account.getAccountType()
                + ") - current balance " + money(account.getBalance()));
        if (list.isEmpty()) {
            System.out.println("   No transactions yet.");
            return;
        }
        System.out.printf("%-17s %-11s %20s   %s%n", "DATE", "TYPE", "AMOUNT", "REFERENCE");
        for (Transaction t : list) {
            String sign = t.getTransactionType().equals("DEPOSIT") ? "+" : "-";
            System.out.printf("%-17s %-11s %20s   %s%n", t.getTimestamp().format(DATE),
                    t.getTransactionType(), sign + money(t.getAmount()), t.getReferenceId());
        }
    }

    private static void exportTransactionHistory() throws SQLException {
        Account account = chooseMyAccount("Account ID to export");

        if (account == null) {
            return;
        }

        List<Transaction> transactions = transactionDAO.findByAccount(account.getAccountId());

        if (transactions.isEmpty()) {
            System.out.println("[!] This account has no transactions to export.");
            return;
        }

        try {
            java.nio.file.Path file = reportService.exportToCSV(transactions, account.getAccountId());

            System.out.println("[OK] Transaction history exported successfully.");
            System.out.println("File location: " + file.toAbsolutePath());

        } catch (java.io.IOException e) {
            System.out.println("[!] Could not create the CSV file.");
            System.out.println("Reason: " + e.getMessage());
        }
    }

    private static void dailyTransactionSummary() throws SQLException {
        System.out.println();
        System.out.println("Enter date in YYYY-MM-DD format.");

        String input = ask("Date");
        LocalDate date;

        try {
            date = LocalDate.parse(input);
        } catch (DateTimeParseException e) {
            System.out.println("[!] Invalid date. Example: 2026-10-28");
            return;
        }

        List<Transaction> customerTransactions = transactionDAO.findByCustomer(loggedIn.getCustomerId());

        List<Transaction> dailyTransactions = new ArrayList<>();

        for (Transaction transaction : customerTransactions) {
            if (transaction.getTimestamp() != null && transaction.getTimestamp().toLocalDate().equals(date)) {
                dailyTransactions.add(transaction);
            }
        }

        reportService.printDailySummary(dailyTransactions, date);
    }


    private static void customerTransactionStatement() throws SQLException {
        String customerId = loggedIn.getCustomerId();

        List<Transaction> transactions = transactionDAO.findByCustomer(customerId);

        reportService.printCustomerStatement(transactions, customerId);
    }

    private static void viewProfile() {
        System.out.println();
        System.out.println("Customer ID : " + loggedIn.getCustomerId());
        System.out.println("Name        : " + loggedIn.getCustomerName());
        System.out.println("Email       : " + loggedIn.getEmail());
        System.out.println("Phone       : " + loggedIn.getPhoneNumber());
    }

    private static void updateProfile() throws SQLException {
        System.out.println("Press Enter to keep the current value.");
        String name = ask("Full name [" + loggedIn.getCustomerName() + "]");
        String email = ask("Email [" + loggedIn.getEmail() + "]");
        String phone = ask("Phone [" + loggedIn.getPhoneNumber() + "]");

        if (!email.isEmpty() && !email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            System.out.println("[!] Invalid email. Nothing was changed.");
            return;
        }
        if (!phone.isEmpty() && !phone.matches("07\\d{8}")) {
            System.out.println("[!] Phone must be 10 digits starting with 07. Nothing was changed.");
            return;
        }
        if (!email.isEmpty()) {
            Customer owner = customerDAO.findByEmail(email);
            if (owner != null && !owner.getCustomerId().equals(loggedIn.getCustomerId())) {
                System.out.println("[!] That email is used by another customer. Nothing was changed.");
                return;
            }
        }
        if (!name.isEmpty())  loggedIn.setCustomerName(name);
        if (!email.isEmpty()) loggedIn.setEmail(email);
        if (!phone.isEmpty()) loggedIn.setPhoneNumber(phone);

        customerDAO.updateCustomer(loggedIn);
        System.out.println("[OK] Profile saved.");
    }

    private static void changePin() throws SQLException {
        if (!pinDAO.verifyPin(loggedIn.getCustomerId(), ask("Current PIN"))) {
            System.out.println("[!] Wrong PIN. PIN not changed.");
            return;
        }
        String newPin = askNewPin();
        pinDAO.savePin(loggedIn.getCustomerId(), newPin);
        System.out.println("[OK] PIN changed successfully.");
    }

    private static Account chooseMyAccount(String label) throws SQLException {
        List<Account> mine = accountDAO.findByCustomer(loggedIn.getCustomerId());
        if (mine.isEmpty()) {
            System.out.println("[!] You have no accounts yet. Choose option 1 to open one.");
            return null;
        }
        System.out.print("Your accounts:");
        for (Account a : mine) {
            System.out.print("  " + a.getAccountId() + " (" + a.getAccountType() + ", " + money(a.getBalance()) + ")");
        }
        System.out.println();

        String id = ask(label);
        for (Account a : mine) {
            if (a.getAccountId().equals(id)) {
                return a;
            }
        }
        System.out.println("[!] Account " + id + " is not one of your accounts.");
        return null;
    }
    private static boolean confirmWithPin() throws SQLException {
        for (int attemptsLeft = 3; attemptsLeft > 0; attemptsLeft--) {
            if (pinDAO.verifyPin(loggedIn.getCustomerId(), ask("Enter your PIN to confirm"))) {
                return true;
            }
            System.out.println("[!] Wrong PIN. Attempts left: " + (attemptsLeft - 1));
        }
        System.out.println("[!] Operation cancelled.");
        return false;
    }

    private static void showResult(boolean processed, String reference, String accountId) throws SQLException {
        if (processed) {
            Account updated = accountDAO.findById(accountId);
            System.out.println("[OK] Done. Reference: " + reference
                    + " | New balance of " + accountId + ": " + money(updated.getBalance()));
        } else {
            System.out.println("[!] Duplicate request: reference " + reference
                    + " was already processed. Nothing was charged again.");
        }
    }

    private static void showDatabaseError(SQLException e) {
        String state = e.getSQLState();
        if (state != null && state.startsWith("08")) {          // 08xxx = connection problems
            System.out.println("[!] Lost connection to the database. Check that PostgreSQL is running and try again.");
        } else {
            System.out.println("[!] Database error: " + e.getMessage());
        }
    }


    private static String newCustomerId() throws SQLException {
        try (Connection connection = DBConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT nextval('customer_id_seq')")) {
            rs.next();
            return "C" + rs.getLong(1);
        }
    }

    private static String newAccountId(String type) throws SQLException {
        String prefix = type.equals("WALLET") ? "W" : "S";
        String id;
        do {
            id = prefix + (10000 + random.nextInt(90000));            // e.g. W48213
        } while (accountDAO.findById(id) != null);
        return id;
    }

    private static String ask(String label) {
        System.out.print(label + ": ");
        if (!scanner.hasNextLine()) {                                // input closed
            System.exit(0);
        }
        return scanner.nextLine().trim();
    }

    private static String askRequired(String label) {
        while (true) {
            String value = ask(label);
            if (!value.isEmpty()) return value;
            System.out.println("[!] This field is required.");
        }
    }

    private static String askMatching(String label, String pattern, String hint) {
        while (true) {
            String value = ask(label);
            if (value.matches(pattern)) return value;
            System.out.println("[!] " + hint);
        }
    }

    private static boolean askYesNo(String question) {
        while (true) {
            String answer = ask(question + " (y/n)").toLowerCase();
            if (answer.equals("y") || answer.equals("yes")) return true;
            if (answer.equals("n") || answer.equals("no")) return false;
            System.out.println("[!] Please answer y or n.");
        }
    }

    private static double askAmount() {
        while (true) {
            try {
                double amount = Double.parseDouble(ask("Amount (RWF)"));
                if (amount > 0) return amount;
                System.out.println("[!] Amount must be greater than zero.");
            } catch (NumberFormatException e) {
                System.out.println("[!] Enter a number, e.g. 5000");
            }
        }
    }

    private static String askNewPin() {
        while (true) {
            String pin = ask("New 4-digit PIN");
            if (!pin.matches("\\d{4}")) {
                System.out.println("[!] The PIN must be exactly 4 digits.");
                continue;
            }
            if (!pin.equals(ask("Confirm PIN"))) {
                System.out.println("[!] PINs do not match. Try again.");
                continue;
            }
            return pin;
        }
    }

    private static String askReference(String prefix) {
        String reference = ask("Reference ID (press Enter to auto-generate)");
        if (reference.isEmpty()) {
            reference = prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            System.out.println("    Reference: " + reference);
        }
        return reference;
    }

    private static String money(double amount) {
        return String.format("%,.2f RWF", amount);
    }
}
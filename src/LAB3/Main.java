
package LAB3;

import LAB1.Account;
import LAB1.Customer;
import LAB1.Transaction;
import LAB2.*;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

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
        System.out.println("       IgirePay Payments - Digital Wallet (Rwanda)");

        if (!prepareDatabase()) return;

        while (true) {
            menu("WELCOME", "1. Register as a new customer", "2. Login", "0. Exit");

            switch (ask("Choose")) {
                case "1" -> runSafely(() -> register());
                case "2" -> runSafely(() -> login());
                case "0" -> {
                    System.out.println("Thank you for using IgirePay!");
                    return;
                }
                default -> System.out.println("[!] Invalid option.");
            }
        }
    }

    private static void menu(String title, String... options) {
        System.out.println("\n------------- " + title + " -------------");
        for (String option : options) System.out.println(option);
    }

    private static void runSafely(SqlAction action) {
        try {
            action.run();
        } catch (SQLException e) {
            showDatabaseError(e);
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println("[!] " + e.getMessage());
        }
    }

    @FunctionalInterface
    private interface SqlAction {
        void run() throws SQLException;
    }

    private static boolean prepareDatabase() {
        try (Connection c = DBConnection.getConnection();
             Statement s = c.createStatement()) {
            s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS processed_requests (
                    id SERIAL PRIMARY KEY,
                    reference_id VARCHAR(100) NOT NULL UNIQUE,
                    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """);
            s.executeUpdate(
                    "CREATE SEQUENCE IF NOT EXISTS customer_id_seq START 1001");
            pinDAO.createTableIfNotExists();
            return true;
        } catch (SQLException e) {
            System.out.println("[!] Cannot connect to the database.");
            System.out.println("Reason: " + e.getMessage());
            System.out.println("Check PostgreSQL, the IgirePayDB database,");
            System.out.println("and the credentials in LAB2/DBConnection.java.");
            return false;
        }
    }

    private static void register() throws SQLException {
        menu("NEW CUSTOMER");
        String name = askRequired("Full name");
        String email = askMatching("Email", "[^@\\s]+@[^@\\s]+\\.[^@\\s]+", "Enter a valid email, e.g. name@example.com");

        if (customerDAO.findByEmail(email) != null) {
            System.out.println("[!] Email already registered. Please log in.");
            return;
        }

        String phone = askMatching("Phone number (07XXXXXXXX)", "07\\d{8}", "Enter 10 digits starting with 07");
        String id = newCustomerId();

        customerDAO.registerCustomer(new Customer(id, name, email, phone));
        pinDAO.savePin(id, askNewPin());

        System.out.println("[OK] Registration complete. Customer ID: " + id);

        if (askYesNo("Open a WALLET account now?")) {
            String accountId = newAccountId("WALLET");
            accountDAO.registerAccount(accountId, id, "WALLET");
            System.out.println("[OK] Wallet account opened: " + accountId);
        }
    }

    private static void login() throws SQLException {
        menu("LOGIN");
        Customer customer = customerDAO.findByEmail(ask("Email"));

        if (customer == null) {
            System.out.println("[!] Customer not found. Register first.");
            return;
        }

        String id = customer.getCustomerId();

        if (!pinDAO.hasPin(id)) {
            System.out.println("Create a PIN after verifying your phone number.");
            if (!ask("Phone number on file").equals(customer.getPhoneNumber())) {
                System.out.println("[!] Phone number does not match.");
                return;
            }
            pinDAO.savePin(id, askNewPin());
        }

        for (int i = 3; i > 0; i--) {
            if (pinDAO.verifyPin(id, ask("PIN"))) {
                loggedIn = customer;
                dashboard();
                return;
            }
            System.out.println("[!] Wrong PIN. Attempts left: " + (i - 1));
        }
        System.out.println("[!] Too many wrong attempts.");
    }

    private static void dashboard() {
        while (loggedIn != null) {
            menu(loggedIn.getCustomerName() + " (" + loggedIn.getCustomerId() + ")",
                    "-- My accounts --",
                    "1. Open account", "2. View my accounts",
                    "3. Account details / balance", "4. Change account type",
                    "5. Delete inactive account",
                     "6. Deposit", "7. Withdraw",
                    "8. Transfer", "9. Transaction history",
                    "10. View profile",
                    "11. Update details", "12. Change PIN",
                    "13. Export history to CSV",
                    "14. Daily summary",
                    "15. Customer statement",
                    "0. Logout");

            try {
                switch (ask("Choose")) {
                    case "1" -> openAccount();
                    case "2" -> viewMyAccounts();
                    case "3" -> viewAccountDetails();
                    case "4" -> changeAccountType();
                    case "5" -> deleteInactiveAccount();
                    case "6" -> deposit();
                    case "7" -> withdraw();
                    case "8" -> transfer();
                    case "9" -> history();
                    case "10" -> viewProfile();
                    case "11" -> updateProfile();
                    case "12" -> changePin();
                    case "13" -> exportTransactionHistory();
                    case "14" -> dailyTransactionSummary();
                    case "15" -> customerTransactionStatement();
                    case "0" -> {
                        System.out.println("Logged out. Goodbye, " + loggedIn.getCustomerName() + ".");
                        loggedIn = null;
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
        String choice = ask("1. Wallet  2. Savings");
        String type = choice.equals("1") ? "WALLET" : choice.equals("2") ? "SAVING" : null;

        if (type == null) {
            System.out.println("[!] Invalid account type.");
            return;
        }

        String id = newAccountId(type);
        accountDAO.registerAccount(id, loggedIn.getCustomerId(), type);
        System.out.println("[OK] " + type + " account opened: " + id);
    }

    private static void viewMyAccounts() throws SQLException {
        List<Account> accounts = accountDAO.findByCustomer(loggedIn.getCustomerId());

        if (accounts.isEmpty()) {
            System.out.println("You have no accounts. Choose option 1 to open one.");
            return;
        }

        System.out.printf("%-10s %-10s %18s   %s%n", "ACCOUNT", "TYPE", "BALANCE", "OPENED");
        double total = 0;

        for (Account a : accounts) {
            System.out.printf("%-10s %-10s %18s   %s%n", a.getAccountId(), a.getAccountType(), money(a.getBalance()), a.getCreatedAt().format(DATE));
            total += a.getBalance();
        }
        System.out.println("TOTAL: " + money(total));
    }

    private static void viewAccountDetails() throws SQLException {
        Account a = chooseMyAccount("Account ID");
        if (a == null) return;

        System.out.println("Account      : " + a.getAccountId());
        System.out.println("Type         : " + a.getAccountType());
        System.out.println("Balance      : " + money(a.getBalance()));
        System.out.println("Opened       : " + a.getCreatedAt().format(DATE));
        System.out.println("Transactions : " + transactionDAO.findByAccount(a.getAccountId()).size());
    }

    private static void changeAccountType() throws SQLException {
        Account a = chooseMyAccount("Account ID");
        if (a == null) return;

        String type = a.getAccountType().equals("WALLET") ? "SAVING" : "WALLET";
        if (!askYesNo("Change " + a.getAccountId() + " to " + type + "?"))
            return;

        a.setAccountType(type);
        accountDAO.updateAccount(a);
        System.out.println("[OK] Account type updated.");
    }

    private static void deleteInactiveAccount() throws SQLException {
        Account a = chooseMyAccount("Account ID to delete");
        if (a == null) return;

        if (a.getBalance() != 0 || !transactionDAO.findByAccount(a.getAccountId()).isEmpty()) {
            System.out.println("[!] Account has a balance or transaction history.");
            return;
        }

        if (askYesNo("Permanently delete " + a.getAccountId() + "?")) {
            accountDAO.deleteAccount(a.getAccountId());
            System.out.println("[OK] Account deleted.");
        }
    }

    private static void deposit() throws SQLException {
        processMoney("Deposit to account", "DEP", "DEPOSIT");
    }

    private static void withdraw() throws SQLException {
        processMoney("Withdraw from account", "WD", "WITHDRAWAL");
    }

    private static void processMoney(String prompt, String prefix, String type) throws SQLException {
        Account a = chooseMyAccount(prompt);
        if (a == null) return;

        double amount = askAmount();
        if (type.equals("WITHDRAWAL") && !confirmWithPin()) return;

        String ref = askReference(prefix);
        boolean success = type.equals("DEPOSIT") ? wallet.deposit(a.getAccountId(), amount, ref) : wallet.withdraw(a.getAccountId(), amount, ref);
        showResult(success, ref, a.getAccountId());
    }

    private static void transfer() throws SQLException {
        Account from = chooseMyAccount("From my account");
        if (from == null)
            return;

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
        String name = receiver == null ? "unknown" : receiver.getCustomerName();
        double amount = askAmount();

        if (!askYesNo("Send " + money(amount) + " to " + name + " (" + to.getAccountId() + ")?") || !confirmWithPin()) return;

        String ref = askReference("TRF");
        showResult(wallet.transfer(from.getAccountId(), to.getAccountId(), amount, ref), ref, from.getAccountId());
    }

    private static void history() throws SQLException {
        Account a = chooseMyAccount("Account ID");
        if (a == null)
            return;

        List<Transaction> transactions = transactionDAO.findByAccount(a.getAccountId());

        System.out.println("History of " + a.getAccountId() + " - balance " + money(a.getBalance()));

        if (transactions.isEmpty()) {
            System.out.println("No transactions yet.");
            return;
        }

        System.out.printf("%-17s %-12s %18s   %s%n", "DATE", "TYPE", "AMOUNT", "REFERENCE");

        for (Transaction t : transactions) {
            String sign = t.getTransactionType().equals("DEPOSIT") ? "+" : "-";
            System.out.printf("%-17s %-12s %18s   %s%n", t.getTimestamp().format(DATE), t.getTransactionType(), sign + money(t.getAmount()), t.getReferenceId());
        }
    }

    private static void exportTransactionHistory() throws SQLException {
        Account a = chooseMyAccount("Account ID to export");
        if (a == null) return;

        List<Transaction> list = transactionDAO.findByAccount(a.getAccountId());
        if (list.isEmpty()) {
            System.out.println("[!] No transactions to export.");
            return;
        }

        try {
            System.out.println("[OK] CSV saved to: " + reportService.exportToCSV(list, a.getAccountId()).toAbsolutePath());
        } catch (java.io.IOException e) {
            System.out.println("[!] Could not create CSV: " + e.getMessage());
        }
    }

    private static void dailyTransactionSummary() throws SQLException {
        LocalDate date;
        try {
            date = LocalDate.parse(ask("Date (YYYY-MM-DD)"));
        } catch (DateTimeParseException e) {
            System.out.println("[!] Invalid date. Example: 2026-10-28");
            return;
        }

        List<Transaction> daily = new ArrayList<>();
        for (Transaction t : transactionDAO.findByCustomer(loggedIn.getCustomerId())) {
            if (t.getTimestamp() != null && t.getTimestamp().toLocalDate().equals(date)) {
                daily.add(t);
            }
        }
        reportService.printDailySummary(daily, date);
    }

    private static void customerTransactionStatement() throws SQLException {
        reportService.printCustomerStatement(transactionDAO.findByCustomer(loggedIn.getCustomerId()), loggedIn.getCustomerId());
    }

    private static void viewProfile() {
        System.out.println("Customer ID : " + loggedIn.getCustomerId());
        System.out.println("Name        : " + loggedIn.getCustomerName());
        System.out.println("Email       : " + loggedIn.getEmail());
        System.out.println("Phone       : " + loggedIn.getPhoneNumber());
    }

    private static void updateProfile() throws SQLException {
        System.out.println("Press Enter to keep the current value.");
        String name = ask("Name [" + loggedIn.getCustomerName() + "]");
        String email = ask("Email [" + loggedIn.getEmail() + "]");
        String phone = ask("Phone [" + loggedIn.getPhoneNumber() + "]");

        if (!email.isEmpty() && !email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            System.out.println("[!] Invalid email. Nothing changed.");
            return;
        }
        if (!phone.isEmpty() && !phone.matches("07\\d{8}")) {
            System.out.println("[!] Phone must start with 07 and contain 10 digits.");
            return;
        }
        if (!email.isEmpty()) {
            Customer owner = customerDAO.findByEmail(email);
            if (owner != null && !owner.getCustomerId().equals(loggedIn.getCustomerId())) {
                System.out.println("[!] Email already used. Nothing changed.");
                return;
            }
        }

        if (!name.isEmpty()) loggedIn.setCustomerName(name);
        if (!email.isEmpty()) loggedIn.setEmail(email);
        if (!phone.isEmpty()) loggedIn.setPhoneNumber(phone);

        customerDAO.updateCustomer(loggedIn);
        System.out.println("[OK] Profile saved.");
    }

    private static void changePin() throws SQLException {
        if (!pinDAO.verifyPin(loggedIn.getCustomerId(), ask("Current PIN"))) {
            System.out.println("[!] Wrong PIN.");
            return;
        }
        pinDAO.savePin(loggedIn.getCustomerId(), askNewPin());
        System.out.println("[OK] PIN changed.");
    }

    private static Account chooseMyAccount(String prompt) throws SQLException {
        List<Account> accounts = accountDAO.findByCustomer(loggedIn.getCustomerId());

        if (accounts.isEmpty()) {
            System.out.println("[!] You have no accounts. Open one first.");
            return null;
        }

        for (Account a : accounts) {
            System.out.println(a.getAccountId() + " (" + a.getAccountType() + ", " + money(a.getBalance()) + ")");
        }

        String id = ask(prompt);
        for (Account a : accounts) {
            if (a.getAccountId().equals(id))
                return a;
        }

        System.out.println("[!] That account does not belong to you.");
        return null;
    }

    private static boolean confirmWithPin() throws SQLException {
        for (int i = 3; i > 0; i--) {
            if (pinDAO.verifyPin(loggedIn.getCustomerId(), ask("Enter your PIN to confirm")))
                return true;
            System.out.println("[!] Wrong PIN. Attempts left: " + (i - 1));
        }
        System.out.println("[!] Operation cancelled.");
        return false;
    }

    private static void showResult(boolean success, String ref, String id) throws SQLException {
        if (!success) {
            System.out.println("[!] Reference already processed: " + ref);
            return;
        }

        Account updated = accountDAO.findById(id);
        System.out.println("[OK] Reference: " + ref + " | New balance: " + (updated == null ? "Unavailable" : money(updated.getBalance())));
    }

    private static void showDatabaseError(SQLException e) {
        if (e.getSQLState() != null && e.getSQLState().startsWith("08")) {
            System.out.println("[!] Database connection problem. Check PostgreSQL.");
        } else {
            System.out.println("[!] Database error: " + e.getMessage());
        }
    }

    private static String newCustomerId() throws SQLException {
        try (Connection c = DBConnection.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT nextval('customer_id_seq')")) {
            rs.next();
            return "C" + rs.getLong(1);
        }
    }

    private static String newAccountId(String type) throws SQLException {
        String prefix = type.equals("WALLET") ? "W" : "S";
        String id;
        do {
            id = prefix + (10000 + random.nextInt(90000));
        } while (accountDAO.findById(id) != null);
        return id;
    }

    private static String ask(String label) {
        System.out.print(label + ": ");
        if (!scanner.hasNextLine())
            System.exit(0);
        return scanner.nextLine().trim();
    }

    private static String askRequired(String label) {
        while (true) {
            String value = ask(label);
            if (!value.isEmpty())
                return value;
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
                if (amount > 0)
                    return amount;
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
                System.out.println("[!] PIN must be exactly four digits.");
                continue;
            }
            if (!pin.equals(ask("Confirm PIN"))) {
                System.out.println("[!] PINs do not match.");
                continue;
            }
            return pin;
        }
    }

    private static String askReference(String prefix) {
        String ref = ask("Reference ID (press Enter to generate)");
        if (ref.isEmpty()) {
            ref = prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            System.out.println("Reference: " + ref);
        }
        return ref;
    }

    private static String money(double amount) {
        return String.format("%,.2f RWF", amount);
    }
}

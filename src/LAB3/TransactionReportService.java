package LAB3;

import LAB1.Transaction;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.time.LocalDate;

public class TransactionReportService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Path exportToCSV(List<Transaction> transactions, String accountId) throws IOException {

        String fileName = "transaction_history_" + accountId + "_" + System.currentTimeMillis() + ".csv";

        Path file = Path.of(fileName);

        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {


            writer.write("Transaction ID,Account ID,Date,Type,Amount,Reference ID");
            writer.newLine();

            for (Transaction transaction : transactions) {

                writer.write(
                        transaction.getTransactionId() + ","
                                + csv(transaction.getAccountId()) + ","
                                + csv(transaction.getTimestamp()
                                .format(DATE_FORMAT)) + ","
                                + csv(transaction.getTransactionType()) + ","
                                + transaction.getAmount() + ","
                                + csv(transaction.getReferenceId())
                );

                writer.newLine();
            }
        }

        return file.toAbsolutePath();
    }
    private String csv(String value) {

        if (value == null) {
            return "";
        }

        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    public void printDailySummary(List<Transaction> transactions, LocalDate date) {

        int totalTransactions = transactions.size();

        double totalDeposits = 0;
        double totalWithdrawals = 0;

        for (Transaction transaction : transactions) {

            if ("DEPOSIT".equalsIgnoreCase(transaction.getTransactionType())) {
                totalDeposits += transaction.getAmount();

            } else if ("WITHDRAWAL".equalsIgnoreCase(transaction.getTransactionType())) {
                totalWithdrawals += transaction.getAmount();
            }
        }

        System.out.println();
        System.out.println("========== DAILY TRANSACTION SUMMARY ==========");
        System.out.println("Date: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));

        System.out.println("Total transactions: " + totalTransactions);

        System.out.printf("Total deposits:     %.2f%n", totalDeposits);

        System.out.printf("Total withdrawals:  %.2f%n", totalWithdrawals);

        System.out.printf("Net movement:       %.2f%n",totalDeposits - totalWithdrawals);

        System.out.println("===============================================");
    }

    public void printCustomerStatement(List<Transaction> transactions, String customerId) {

        System.out.println();
        System.out.println("=============== CUSTOMER STATEMENT ===============");
        System.out.println("Customer ID: " + customerId);

        if (transactions.isEmpty()) {
            System.out.println("No transactions found.");
            return;
        }

        System.out.printf("%-6s %-14s %-20s %-12s %12s %-20s%n", "ID", "ACCOUNT", "DATE", "TYPE", "AMOUNT", "REFERENCE");

        for (Transaction transaction : transactions) {

            System.out.printf(
                    "%-6d %-14s %-20s %-12s %12.2f %-20s%n",
                    transaction.getTransactionId(),
                    transaction.getAccountId(),
                    transaction.getTimestamp().format(DATE_FORMAT),
                    transaction.getTransactionType(),
                    transaction.getAmount(),
                    transaction.getReferenceId()
            );
        }

        System.out.println("==================================================");
    }
}

package LAB1;

import java.time.LocalDateTime;

public class Transaction {
    private int transactionId;
    private String referenceId;
    private double amount;
    private String transactionType;
    private LocalDateTime Timestamp;
    private String accountId;

    public Transaction(int transactionId, String referenceId, double amount, String transactionType, LocalDateTime timestamp, String accountId) {
        this.transactionId = transactionId;
        this.referenceId = referenceId;
        this.amount = amount;
        this.transactionType = transactionType;
        Timestamp = timestamp;
        this.accountId = accountId;
    }

    public Transaction() {

    }

    public int getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(int transactionId) {
        this.transactionId = transactionId;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public LocalDateTime getTimestamp() {
        return Timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        Timestamp = timestamp;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "transactionId='" + transactionId + '\'' +
                ", referenceId=" + referenceId +
                ", amount=" + amount +
                ", transactionType='" + transactionType + '\'' +
                ", Timestamp=" + Timestamp +
                ", accountId='" + accountId + '\'' +
                '}';
    }
}
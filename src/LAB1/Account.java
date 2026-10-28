package LAB1;

import java.time.LocalDateTime;

    public class Account {

        private String accountId;
        private String accountNumber;
        private String accountType;
        private double balance;
        private LocalDateTime createdAt = LocalDateTime.now() ;
        private String customerId;

        public Account(String accountId, String accountNumber, String accountType, double balance, LocalDateTime createdAt, String customerId) {
            this.accountId = accountId;
            this.accountNumber = accountNumber;
            this.accountType = accountType;
            this.balance = balance;
            this.createdAt = createdAt;
            this.customerId = customerId;
        }

        public Account(String accountId, String accountNumber, String accountType, String customerId) {
            this.accountId = accountId;
            this.accountNumber = accountNumber;
            this.accountType = accountType;
            this.customerId = customerId;
        }

        public Account() {

        }

        public String getAccountId() {
            return accountId;
        }

        public void setAccountId(String accountId) {
            this.accountId = accountId;
        }

        public String getAccountNumber() {
            return accountNumber;
        }

        public void setAccountNumber(String accountNumber) {
            this.accountNumber = accountNumber;
        }

        public String getAccountType() {
            return accountType;
        }

        public void setAccountType(String accountType) {
            this.accountType = accountType;
        }

        public double getBalance() {
            return balance;
        }

        public void setBalance(double balance) {
            this.balance = balance;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }

        public String getCustomerId() {
            return customerId;
        }

        public void setCustomerId(String customerId) {
            this.customerId = customerId;
        }

        public void withdraw(double amount){

            if(amount >= balance){
                System.out.println("Insufficient balance");
            }
            else if(amount<0){
                System.out.println("the minimum withdraw must be greater than 0");
            }
            else{
                balance-=amount;
                System.out.println("successfully  withdraw:"+balance);
            }

        }
        public void deposit(double amount){
            if(amount>0){
                balance+=amount;
                System.out.println("the new balance is:"+balance);
            }
            else{
                System.out.println("amount must be greater than zero");
            }
        }

        public void processTransaction(double amount,String type){
            if(type.equals("Deposit")){
                deposit(amount);
            }
            else if(type.equals("withdraw")){
                withdraw(amount);
            }
            else{
                System.out.println("Invalid transaction type");
            }

        }


        @Override
        public String toString() {
            return "Account{" +
                    "accountId='" + accountId + '\'' +
                    ", accountNumber='" + accountNumber + '\'' +
                    ", accountType='" + accountType + '\'' +
                    ", balance=" + balance +
                    ", createdAt=" + createdAt +
                    ", customerId='" + customerId + '\'' +
                    '}';
        }
    }

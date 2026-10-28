package LAB1;

import java.time.LocalDateTime;

    public class SavingAccount extends Account{

        private double interest;
        private double newBalance;

        public double getInterest() {
            return interest;
        }

        public void setInterest(double interest) {
            this.interest = interest;
        }

        public double getNewBalance() {
            return newBalance;
        }

        public void setNewBalance(double newBalance) {
            this.newBalance = newBalance;
        }

        public SavingAccount(String accountId, String accountNumber, String accountType, double balance, LocalDateTime createdAt, String customerId, double interest, double newBalance) {
            super(accountId, accountNumber, accountType, balance, createdAt, customerId);

            this.interest=interest;
            this.newBalance=newBalance;


        }

        public SavingAccount(String accountId, String accountNumber, String accountType, LocalDateTime createdAt, String customerId) {
            super(accountId, accountNumber, accountType,  customerId);
        }
        @Override
        public void deposit(double amount){
            if(amount>1000){

                newBalance=(getBalance()+amount);
                System.out.println("the new balance is:"+newBalance);
            }
            else{
                System.out.println("amount must be greater than 1000");
            }
        }
        @Override

        public void withdraw(double amount){

            if(amount >= getBalance()){
                System.out.println("Insufficient balance");
            }
            else if(amount<0){
                System.out.println("the minimum withdraw must be greater than 0");

            }
            else{
                interest=getBalance()*0.08;
                newBalance=amount+interest;
                System.out.println("successfully  withdraw:"+newBalance);
            }

        }
        @Override
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
            return "SavingAccount{" +
                    "interest=" + interest +
                    ", newBalance=" + newBalance +
                    '}';
        }
    }

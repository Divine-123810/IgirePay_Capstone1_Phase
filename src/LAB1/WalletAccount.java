package LAB1;

import java.time.LocalDateTime;


    public class WalletAccount extends LAB1.Account {

        private double charges;
        private double newBalance;




        public WalletAccount(String accountId, String accountNumber, String accountType, double balance, LocalDateTime createdAt, String customerId, double charges, double newBalance) {
            super(accountId, accountNumber, accountType, balance, createdAt, customerId);

            this.charges=charges;
            this.newBalance=newBalance;

        }

        public double getCharges() {
            return charges;
        }

        public void setCharges(double charges) {
            this.charges = charges;
        }

        public double getNewBalance() {
            return newBalance;
        }

        public void setNewBalance(double newBalance) {
            this.newBalance = newBalance;
        }

        @Override
        public void deposit(double amount){
            if(amount>0){

                setBalance(getBalance()+amount);
                System.out.println("the new balance is:");
            }
            else{
                System.out.println("amount must be greater than zero");
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
                charges=(amount*0.02);
                setBalance(getBalance()-charges);
                System.out.println("successfully  withdraw:"+amount);

                newBalance=getBalance()-charges-amount;
                System.out.println("the new balance is:"+newBalance);
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
            return "WalletAccount{" +
                    "charges=" + charges +
                    ", newBalance=" + newBalance +
                    '}';
        }
    }

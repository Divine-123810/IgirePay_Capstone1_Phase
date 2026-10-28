package LAB1;

public class Customer {
        private String customerId;
        private String customerName;
        private String email;
        private String phoneNumber;

        public Customer(String customerId, String customerName, String email, String phoneNumber) {
            this.customerId = customerId;
            this.customerName = customerName;
            this.email = email;
            this.phoneNumber = phoneNumber;
        }


        public Customer() {
        }

        public String getCustomerId() {
            return customerId;
        }

        public void setCustomerId(String customerId) {
            this.customerId = customerId;
        }

        public String getCustomerName() {
            return customerName;
        }

        public void setCustomerName(String customerName) {
            this.customerName = customerName;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPhoneNumber() {
            return phoneNumber;
        }

        public void setPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
        }

        @Override
        public String toString() {
            return "Customer{" +
                    "customerId='" + customerId + '\'' +
                    ", customerName='" + customerName + '\'' +
                    ", email='" + email + '\'' +
                    ", phoneNumber='" + phoneNumber + '\'' +
                    '}';
        }
    }
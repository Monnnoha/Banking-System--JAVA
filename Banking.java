import java.sql.*;
import java.util.Scanner;

public class Banking {
    private static final String url = "jdbc:mysql://localhost:3306/database";
    private static final String username = "root";
    private static final String password = "password";

    public static void main(String[] args) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return;
        }

        try (
                Connection connection = DriverManager.getConnection(url, username, password);
                Scanner scanner = new Scanner(System.in)
        ) {
            boolean running = true;

            while (running) {
                System.out.println("\nBanking System...");
                System.out.println("1. Create Account");
                System.out.println("2. View Balance");
                System.out.println("3. Deposit Money");
                System.out.println("4. Withdraw Money");
                System.out.println("5. Transfer Money");
                System.out.println("6. Exit");
                System.out.print("Enter Choice: ");

                int choice = scanner.nextInt();
                switch (choice) {
                    case 1:
                        createAccount(connection, scanner);
                        break;
                    case 2:
                        viewBalance(connection, scanner);
                        break;
                    case 3:
                        depositMoney(connection, scanner);
                        break;
                    case 4:
                        withdrawMoney(connection, scanner);
                        break;
                    case 5:
                        transferMoney(connection, scanner);
                        break;
                    case 6:
                        running = false;
                        System.out.println("Exiting... Thank you!");
                        break;
                    default:
                        System.out.println("Enter a valid choice.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void createAccount(Connection connection, Scanner scanner) {
        try {
            System.out.print("Enter Account Number: ");
            int accNo = scanner.nextInt();
            scanner.nextLine();
            System.out.print("Enter Name: ");
            String name = scanner.nextLine();
            System.out.print("Enter Initial Balance: ");
            double balance = scanner.nextDouble();

            String query = "INSERT INTO accounts(account_number, name, balance) VALUES(?, ?, ?)";
            try (PreparedStatement ps = connection.prepareStatement(query)) {
                ps.setInt(1, accNo);
                ps.setString(2, name);
                ps.setDouble(3, balance);
                ps.executeUpdate();
                System.out.println("Account Created Successfully.");
            }
        } catch (SQLException e) {
            System.out.println("Error creating account: " + e.getMessage());
        }
    }

    private static void viewBalance(Connection connection, Scanner scanner) {
        try {
            System.out.print("Enter the account number: ");
            int accNo = scanner.nextInt();

            String query = "SELECT balance FROM accounts WHERE account_number = ?";
            try (PreparedStatement ps = connection.prepareStatement(query)) {
                ps.setInt(1, accNo);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    System.out.println("BALANCE: " + rs.getDouble("balance"));
                } else {
                    System.out.println("Account not Found!...");
                }
            }
        } catch (SQLException e) {
            System.out.println("Error in view balance: " + e.getMessage());
        }
    }

    private static void depositMoney(Connection connection, Scanner scanner) {
        try {
            System.out.print("Enter the account number: ");
            int accNo = scanner.nextInt();
            System.out.print("Enter amount to deposit: ");
            double amount = scanner.nextDouble();

            String query = "UPDATE accounts SET balance = balance + ? WHERE account_number = ?";
            try (PreparedStatement ps = connection.prepareStatement(query)) {
                ps.setDouble(1, amount);
                ps.setInt(2, accNo);
                int rows = ps.executeUpdate();
                if (rows > 0) {
                    System.out.println("Deposit Successful...");
                } else {
                    System.out.println("Account not found");
                }
            }
        } catch (SQLException e) {
            System.out.println("Error in deposit: " + e.getMessage());
        }
    }

    private static void withdrawMoney(Connection connection, Scanner scanner) {
        try {
            System.out.print("Enter the account number: ");
            int accNo = scanner.nextInt();
            System.out.print("Enter the amount to withdraw: ");
            double amount = scanner.nextDouble();

            if (isSufficient(connection, accNo, amount)) {
                String query = "UPDATE accounts SET balance = balance - ? WHERE account_number = ?";
                try (PreparedStatement ps = connection.prepareStatement(query)) {
                    ps.setDouble(1, amount);
                    ps.setInt(2, accNo);
                    ps.executeUpdate();
                    System.out.println("Withdrawal Successful...");
                }
            } else {
                System.out.println("Insufficient balance or account not found");
            }
        } catch (SQLException e) {
            System.out.println("Error in withdraw: " + e.getMessage());
        }
    }

    private static void transferMoney(Connection connection, Scanner scanner) {
        try {
            System.out.print("Enter the source account number: ");
            int fromAcc = scanner.nextInt();
            System.out.print("Enter the destination account number: ");
            int toAcc = scanner.nextInt();
            System.out.print("Enter amount to transfer: ");
            double amount = scanner.nextDouble();

            if (isSufficient(connection, fromAcc, amount)) {
                connection.setAutoCommit(false); // begin transaction

                try (PreparedStatement debitstmt = connection.prepareStatement(
                        "UPDATE accounts SET balance = balance - ? WHERE account_number = ?");
                     PreparedStatement creditstmt = connection.prepareStatement(
                             "UPDATE accounts SET balance = balance + ? WHERE account_number = ?")) {

                    debitstmt.setDouble(1, amount);
                    debitstmt.setInt(2, fromAcc);
                    creditstmt.setDouble(1, amount);
                    creditstmt.setInt(2, toAcc);

                    debitstmt.executeUpdate();
                    creditstmt.executeUpdate();

                    connection.commit();
                    System.out.println("Transfer Successful...");
                } catch (SQLException e) {
                    try {
                        connection.rollback();
                    } catch (SQLException ex) {
                        System.out.println("Rollback failed: " + ex.getMessage());
                    }
                    System.out.println("Error in transfer: " + e.getMessage());
                } finally {
                    connection.setAutoCommit(true);
                }
            } else {
                System.out.println("Insufficient funds in source account.");
            }
        } catch (SQLException e) {
            System.out.println("Error in transfer: " + e.getMessage());
        }
    }

    private static boolean isSufficient(Connection connection, int accNo, double amount) {
        String query = "SELECT balance FROM accounts WHERE account_number = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, accNo);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getDouble("balance") >= amount;
            }
        } catch (SQLException e) {
            System.out.println("Error checking balance: " + e.getMessage());
        }
        return false;
    }
}

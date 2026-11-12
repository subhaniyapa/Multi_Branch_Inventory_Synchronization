
package client;

import java.io.PrintWriter;
import java.util.Scanner;

public class BranchManager {

    private final String branchName;
    private final PrintWriter output;
    private final Scanner scanner;

    public BranchManager(String branchName,
                         PrintWriter output,
                         Scanner scanner) {
        this.branchName = branchName;
        this.output = output;
        this.scanner = scanner;
    }

    public void showMenu() {

        while (true) {

            System.out.println("\n===== " + branchName + " BRANCH =====");
            System.out.println("1. View Inventory");
            System.out.println("2. Add Stock");
            System.out.println("3. Sell Stock");
            System.out.println("4. Transfer Stock");
            System.out.println("5. Exit");

            System.out.print("Enter choice: ");
            String choice = scanner.nextLine().trim();

            String command;

            switch (choice) {

                case "1":
                    command = "GET_STOCK";
                    break;

                case "2":
                    System.out.print("Product ID: ");
                    String addId = scanner.nextLine().trim();

                    if (!isValidIdentifier(addId)) {
                        System.out.println("Invalid Product ID!");
                        continue;
                    }

                    System.out.print("Quantity: ");
                    String addQty = scanner.nextLine().trim();

                    if (!isValidQuantity(addQty)) {
                        System.out.println(
                            "Invalid quantity! Enter a positive integer."
                        );
                        continue;
                    }

                    command = "ADD," + branchName + ","
                            + addId + "," + addQty;
                    break;

                case "3":
                    System.out.print("Product ID: ");
                    String sellId = scanner.nextLine().trim();

                    if (!isValidIdentifier(sellId)) {
                        System.out.println("Invalid Product ID!");
                        continue;
                    }

                    System.out.print("Quantity: ");
                    String sellQty = scanner.nextLine().trim();

                    if (!isValidQuantity(sellQty)) {
                        System.out.println(
                            "Invalid quantity! Enter a positive integer."
                        );
                        continue;
                    }

                    command = "SELL," + branchName + ","
                            + sellId + "," + sellQty;
                    break;

                case "4":
                    System.out.print("Destination Branch: ");
                    String destination = scanner.nextLine().trim();

                    if (!isValidIdentifier(destination)
                            || destination.equalsIgnoreCase(branchName)) {
                        System.out.println("Invalid destination branch!");
                        continue;
                    }

                    System.out.print("Product ID: ");
                    String transferId = scanner.nextLine().trim();

                    if (!isValidIdentifier(transferId)) {
                        System.out.println("Invalid Product ID!");
                        continue;
                    }

                    System.out.print("Quantity: ");
                    String transferQty = scanner.nextLine().trim();

                    if (!isValidQuantity(transferQty)) {
                        System.out.println(
                            "Invalid quantity! Enter a positive integer."
                        );
                        continue;
                    }

                    command = "TRANSFER," + branchName + ","
                            + destination + ","
                            + transferId + "," + transferQty;
                    break;

                case "5":
                    System.out.println("Exiting branch client...");
                    return;

                default:
                    System.out.println("Invalid choice. Try again.");
                    continue;
            }

            output.println(command);

            if (output.checkError()) {
                System.out.println("Failed to send command.");
                return;
            }

            System.out.println("Request sent: " + command);
        }
    }

    
    private boolean isValidQuantity(String quantity) {
        try {
            int value = Integer.parseInt(quantity);
            return value > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    
    private boolean isValidIdentifier(String value) {
        return value != null
                && !value.isBlank()
                && value.matches("[A-Za-z0-9_-]+");
    }
}

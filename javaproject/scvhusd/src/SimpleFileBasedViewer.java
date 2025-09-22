import java.util.Scanner;

/**
 * Simple file-based transaction viewer
 * Works without any external dependencies
 */
public class SimpleFileBasedViewer {
    
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("=== Simple File-Based Transaction Viewer ===");
        System.out.println("Data is stored in transactions.txt and line_items.txt files");
        
        while (true) {
            displayMenu();
            System.out.print("Enter your choice: ");
            String choice = scanner.nextLine().trim();
            
            switch (choice) {
                case "1" -> SimpleFileBasedDataStore.displayAllTransactions();
                case "2" -> SimpleFileBasedDataStore.displayTransactionSummary();
                case "3" -> {
                    System.out.println("Goodbye!");
                    return;
                }
                default -> System.out.println("Invalid choice. Please try again.");
            }
            
            System.out.println("\nPress Enter to continue...");
            scanner.nextLine();
        }
    }
    
    private static void displayMenu() {
        String separator = "==================================================";
        System.out.println("\n" + separator);
        System.out.println("SIMPLE FILE-BASED TRANSACTION VIEWER");
        System.out.println(separator);
        System.out.println("1. View All Transactions");
        System.out.println("2. View Transaction Summary");
        System.out.println("3. Exit");
        System.out.println(separator);
    }
}


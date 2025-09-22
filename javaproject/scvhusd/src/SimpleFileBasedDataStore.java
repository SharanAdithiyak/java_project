import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Simple file-based data storage for transactions
 * Works without complex Transaction classes
 */
public class SimpleFileBasedDataStore {
    
    private static final String TRANSACTIONS_FILE = "transactions.txt";
    private static final String LINE_ITEMS_FILE = "line_items.txt";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * Simple transaction data structure
     */
    public static class SimpleTransaction {
        public int transactionId;
        public LocalDateTime transactionDate;
        public double subtotal;
        public double taxRatePercent;
        public double taxAmount;
        public double totalDue;
        public String paymentMethod;
        public double amountPaid;
        public double changeAmount;
        public String cardNumberMasked;
        public String cardHolderName;
        public String cardExpiry;
        public List<SimpleLineItem> lineItems;
        
        public SimpleTransaction() {
            this.lineItems = new ArrayList<>();
        }
    }
    
    /**
     * Simple line item data structure
     */
    public static class SimpleLineItem {
        public int transactionId;
        public String description;
        public int quantity;
        public double unitPrice;
        public double lineTotal;
    }
    
    /**
     * Saves a transaction to file
     */
    public static void saveTransaction(SimpleTransaction transaction) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(TRANSACTIONS_FILE, true))) {
            writer.println(transaction.transactionId + "|" +
                          transaction.transactionDate.format(DATE_FORMATTER) + "|" +
                          transaction.subtotal + "|" +
                          transaction.taxRatePercent + "|" +
                          transaction.taxAmount + "|" +
                          transaction.totalDue + "|" +
                          transaction.paymentMethod + "|" +
                          transaction.amountPaid + "|" +
                          transaction.changeAmount + "|" +
                          (transaction.cardNumberMasked != null ? transaction.cardNumberMasked : "") + "|" +
                          (transaction.cardHolderName != null ? transaction.cardHolderName : "") + "|" +
                          (transaction.cardExpiry != null ? transaction.cardExpiry : ""));
            
            // Save line items
            if (transaction.lineItems != null) {
                try (PrintWriter lineWriter = new PrintWriter(new FileWriter(LINE_ITEMS_FILE, true))) {
                    for (SimpleLineItem item : transaction.lineItems) {
                        lineWriter.println(transaction.transactionId + "|" +
                                         item.description + "|" +
                                         item.quantity + "|" +
                                         item.unitPrice + "|" +
                                         item.lineTotal);
                    }
                }
            }
            
            System.out.println("Transaction saved to file with ID: " + transaction.transactionId);
            
        } catch (IOException e) {
            System.err.println("Error saving transaction: " + e.getMessage());
        }
    }
    
    /**
     * Loads all transactions from file
     */
    public static List<SimpleTransaction> loadAllTransactions() {
        List<SimpleTransaction> transactions = new ArrayList<>();
        
        try (Scanner scanner = new Scanner(new File(TRANSACTIONS_FILE))) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] parts = line.split("\\|");
                
                if (parts.length >= 9) {
                    SimpleTransaction transaction = new SimpleTransaction();
                    transaction.transactionId = Integer.parseInt(parts[0]);
                    transaction.transactionDate = LocalDateTime.parse(parts[1], DATE_FORMATTER);
                    transaction.subtotal = Double.parseDouble(parts[2]);
                    transaction.taxRatePercent = Double.parseDouble(parts[3]);
                    transaction.taxAmount = Double.parseDouble(parts[4]);
                    transaction.totalDue = Double.parseDouble(parts[5]);
                    transaction.paymentMethod = parts[6];
                    transaction.amountPaid = Double.parseDouble(parts[7]);
                    transaction.changeAmount = Double.parseDouble(parts[8]);
                    
                    if (parts.length > 9 && !parts[9].isEmpty()) {
                        transaction.cardNumberMasked = parts[9];
                    }
                    if (parts.length > 10 && !parts[10].isEmpty()) {
                        transaction.cardHolderName = parts[10];
                    }
                    if (parts.length > 11 && !parts[11].isEmpty()) {
                        transaction.cardExpiry = parts[11];
                    }
                    
                    // Load line items for this transaction
                    transaction.lineItems = loadLineItemsForTransaction(transaction.transactionId);
                    
                    transactions.add(transaction);
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("No transactions file found. Starting fresh.");
        } catch (Exception e) {
            System.err.println("Error loading transactions: " + e.getMessage());
        }
        
        return transactions;
    }
    
    /**
     * Loads line items for a specific transaction
     */
    private static List<SimpleLineItem> loadLineItemsForTransaction(int transactionId) {
        List<SimpleLineItem> lineItems = new ArrayList<>();
        
        try (Scanner scanner = new Scanner(new File(LINE_ITEMS_FILE))) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] parts = line.split("\\|");
                
                if (parts.length >= 5 && Integer.parseInt(parts[0]) == transactionId) {
                    SimpleLineItem item = new SimpleLineItem();
                    item.transactionId = transactionId;
                    item.description = parts[1];
                    item.quantity = Integer.parseInt(parts[2]);
                    item.unitPrice = Double.parseDouble(parts[3]);
                    item.lineTotal = Double.parseDouble(parts[4]);
                    lineItems.add(item);
                }
            }
        } catch (FileNotFoundException e) {
            // No line items file found, return empty list
        } catch (Exception e) {
            System.err.println("Error loading line items: " + e.getMessage());
        }
        
        return lineItems;
    }
    
    /**
     * Gets the next transaction ID
     */
    public static int getNextTransactionId() {
        List<SimpleTransaction> transactions = loadAllTransactions();
        int maxId = 0;
        for (SimpleTransaction t : transactions) {
            if (t.transactionId > maxId) {
                maxId = t.transactionId;
            }
        }
        return maxId + 1;
    }
    
    /**
     * Displays all transactions
     */
    public static void displayAllTransactions() {
        List<SimpleTransaction> transactions = loadAllTransactions();
        
        if (transactions.isEmpty()) {
            System.out.println("No transactions found.");
            return;
        }
        
        System.out.println("\n--- ALL TRANSACTIONS ---");
        System.out.printf("%-5s %-20s %-10s %-10s %-10s %-8s %-10s%n", 
            "ID", "Date", "Subtotal", "Tax", "Total", "Method", "Paid");
        System.out.println("-".repeat(80));
        
        for (SimpleTransaction transaction : transactions) {
            System.out.printf("%-5d %-20s $%-9.2f $%-9.2f $%-9.2f %-8s $%-9.2f%n",
                transaction.transactionId,
                transaction.transactionDate.format(DATE_FORMATTER),
                transaction.subtotal,
                transaction.taxAmount,
                transaction.totalDue,
                transaction.paymentMethod,
                transaction.amountPaid
            );
        }
        
        System.out.println("\nTotal transactions: " + transactions.size());
    }
    
    /**
     * Displays transaction summary
     */
    public static void displayTransactionSummary() {
        List<SimpleTransaction> transactions = loadAllTransactions();
        
        if (transactions.isEmpty()) {
            System.out.println("No transactions found.");
            return;
        }
        
        System.out.println("\n--- TRANSACTION SUMMARY ---");
        
        double totalSales = 0.0;
        double totalTax = 0.0;
        int cashCount = 0;
        int cardCount = 0;
        double cashTotal = 0.0;
        double cardTotal = 0.0;
        
        for (SimpleTransaction transaction : transactions) {
            totalSales += transaction.totalDue;
            totalTax += transaction.taxAmount;
            
            if ("CASH".equals(transaction.paymentMethod)) {
                cashCount++;
                cashTotal += transaction.totalDue;
            } else {
                cardCount++;
                cardTotal += transaction.totalDue;
            }
        }
        
        System.out.printf("Total Transactions: %d%n", transactions.size());
        System.out.printf("Total Sales: $%.2f%n", totalSales);
        System.out.printf("Total Tax Collected: $%.2f%n", totalTax);
        System.out.printf("Average Transaction: $%.2f%n", totalSales / transactions.size());
        System.out.println();
        System.out.printf("Cash Transactions: %d (%.1f%%) - $%.2f%n", 
            cashCount, (double)cashCount/transactions.size()*100, cashTotal);
        System.out.printf("Card Transactions: %d (%.1f%%) - $%.2f%n", 
            cardCount, (double)cardCount/transactions.size()*100, cardTotal);
    }
}


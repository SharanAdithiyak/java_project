import java.time.LocalDateTime;

/**
 * Test class for simple file-based data storage
 * Works without any external dependencies
 */
public class SimpleFileBasedTest {
    
    public static void main(String[] args) {
        System.out.println("=== Simple File-Based Data Storage Test ===");
        System.out.println("Data will be saved to text files in the current directory.");
        System.out.println();
        
        // Test saving a transaction
        testSaveTransaction();
        
        // Test loading and displaying transactions
        testLoadTransactions();
        
        System.out.println("=== Test completed ===");
        System.out.println("Check transactions.txt and line_items.txt files for saved data.");
    }
    
    private static void testSaveTransaction() {
        System.out.println("Testing transaction saving...");
        
        // Create a test transaction
        SimpleFileBasedDataStore.SimpleTransaction transaction = new SimpleFileBasedDataStore.SimpleTransaction();
        transaction.transactionId = SimpleFileBasedDataStore.getNextTransactionId();
        transaction.transactionDate = LocalDateTime.now();
        transaction.subtotal = 25.00;
        transaction.taxRatePercent = 8.5;
        transaction.taxAmount = 2.13;
        transaction.totalDue = 27.13;
        transaction.paymentMethod = "CASH";
        transaction.amountPaid = 30.00;
        transaction.changeAmount = 2.87;
        
        // Create test line items
        SimpleFileBasedDataStore.SimpleLineItem item1 = new SimpleFileBasedDataStore.SimpleLineItem();
        item1.transactionId = transaction.transactionId;
        item1.description = "Coffee";
        item1.quantity = 2;
        item1.unitPrice = 5.00;
        item1.lineTotal = 10.00;
        transaction.lineItems.add(item1);
        
        SimpleFileBasedDataStore.SimpleLineItem item2 = new SimpleFileBasedDataStore.SimpleLineItem();
        item2.transactionId = transaction.transactionId;
        item2.description = "Sandwich";
        item2.quantity = 1;
        item2.unitPrice = 15.00;
        item2.lineTotal = 15.00;
        transaction.lineItems.add(item2);
        
        // Save transaction
        SimpleFileBasedDataStore.saveTransaction(transaction);
        System.out.println("✓ Test transaction saved successfully");
    }
    
    private static void testLoadTransactions() {
        System.out.println("\nTesting transaction loading...");
        
        // Display all transactions
        SimpleFileBasedDataStore.displayAllTransactions();
        
        // Display summary
        SimpleFileBasedDataStore.displayTransactionSummary();
        
        System.out.println("✓ Transaction loading and display completed");
    }
}


import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Menu-driven payment console application
 * Provides a simple interface for processing payments
 */
public class MenuDrivenPaymentConsole {
    
    private static final double TAX_RATE = 8.5; // 8.5% tax rate
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * Product class for menu items
     */
    public static class Product {
        public String name;
        public double price;
        public String description;
        
        public Product(String name, double price, String description) {
            this.name = name;
            this.price = price;
            this.description = description;
        }
    }
    
    /**
     * Line item for transactions
     */
    public static class LineItem {
        public Product product;
        public int quantity;
        public double lineTotal;
        
        public LineItem(Product product, int quantity) {
            this.product = product;
            this.quantity = quantity;
            this.lineTotal = product.price * quantity;
        }
    }
    
    /**
     * Payment method enumeration
     */
    public enum PaymentMethod {
        CASH, CARD
    }
    
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("=== Menu-Driven Payment Console ===");
        System.out.println("Welcome to the Payment Processing System");
        
        // Initialize products
        List<Product> products = initializeProducts();
        
        while (true) {
            displayMainMenu();
            System.out.print("Enter your choice: ");
            String choice = scanner.nextLine().trim();
            
            switch (choice) {
                case "1" -> processNewTransaction(scanner, products);
                case "2" -> viewTransactions();
                case "3" -> viewTransactionSummary();
                case "4" -> {
                    System.out.println("Thank you for using the Payment Console!");
                    return;
                }
                default -> System.out.println("Invalid choice. Please try again.");
            }
            
            System.out.println("\nPress Enter to continue...");
            scanner.nextLine();
        }
    }
    
    private static void displayMainMenu() {
        String separator = "==================================================";
        System.out.println("\n" + separator);
        System.out.println("PAYMENT CONSOLE MAIN MENU");
        System.out.println(separator);
        System.out.println("1. Process New Transaction");
        System.out.println("2. View All Transactions");
        System.out.println("3. View Transaction Summary");
        System.out.println("4. Exit");
        System.out.println(separator);
    }
    
    private static List<Product> initializeProducts() {
        List<Product> products = new ArrayList<>();
        // Apparel
        products.add(new Product("Classic T-Shirt", 14.99, "100% cotton unisex tee"));
        products.add(new Product("Slim Fit Jeans", 39.99, "Denim with stretch comfort"));
        products.add(new Product("Hoodie", 29.99, "Fleece-lined pullover hoodie"));
        products.add(new Product("Lightweight Jacket", 49.99, "Windbreaker for everyday wear"));
        products.add(new Product("Sneakers", 59.99, "Breathable everyday sneakers"));

        // Accessories
        products.add(new Product("Backpack", 34.99, "Water-resistant daypack, 20L"));
        products.add(new Product("Water Bottle", 12.99, "Insulated stainless steel, 600ml"));
        products.add(new Product("Sunglasses", 19.99, "UV400 polarized lenses"));
        products.add(new Product("Cap", 11.99, "Adjustable cotton baseball cap"));
        products.add(new Product("Wallet", 17.49, "Slim RFID-blocking wallet"));

        // Tech & peripherals
        products.add(new Product("Wireless Earbuds", 49.99, "Bluetooth 5.3 with charging case"));
        products.add(new Product("Phone Charger", 9.99, "20W USB-C fast charger"));
        products.add(new Product("USB-C Cable", 6.99, "1m braided fast-charge cable"));
        products.add(new Product("Smartphone Case", 15.99, "Shock-absorbing protective case"));
        products.add(new Product("Wireless Mouse", 18.99, "Silent click ergonomic mouse"));

        // Stationery
        products.add(new Product("Notebook", 7.49, "A5 dotted journal, 120 pages"));
        products.add(new Product("Pen Set", 5.99, "Pack of 5 gel pens, 0.5mm"));
        return products;
    }
    
    private static void processNewTransaction(Scanner scanner, List<Product> products) {
        System.out.println("\n=== NEW TRANSACTION ===");
        
        List<LineItem> lineItems = new ArrayList<>();
        boolean addMore = true;
        
        while (addMore) {
            displayProducts(products);
            System.out.print("Select product (1-" + products.size() + "): ");
            
            try {
                int productIndex = Integer.parseInt(scanner.nextLine()) - 1;
                if (productIndex < 0 || productIndex >= products.size()) {
                    System.out.println("Invalid product selection.");
                    continue;
                }
                
                Product selectedProduct = products.get(productIndex);
                System.out.print("Enter quantity: ");
                int quantity = Integer.parseInt(scanner.nextLine());
                
                if (quantity <= 0) {
                    System.out.println("Quantity must be positive.");
                    continue;
                }
                
                LineItem lineItem = new LineItem(selectedProduct, quantity);
                lineItems.add(lineItem);
                
                System.out.printf("Added: %d x %s = $%.2f%n", 
                    quantity, selectedProduct.name, lineItem.lineTotal);
                
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
                continue;
            }
            
            System.out.print("Add another item? (y/n): ");
            String response = scanner.nextLine().trim().toLowerCase();
            addMore = response.equals("y") || response.equals("yes");
        }
        
        if (lineItems.isEmpty()) {
            System.out.println("No items added. Transaction cancelled.");
            return;
        }
        
        // Calculate totals
        double subtotal = lineItems.stream().mapToDouble(item -> item.lineTotal).sum();
        double taxAmount = subtotal * (TAX_RATE / 100);
        double totalDue = subtotal + taxAmount;
        
        // Display order summary
        displayOrderSummary(lineItems, subtotal, taxAmount, totalDue);
        
        // Process payment
        processPayment(scanner, lineItems, subtotal, taxAmount, totalDue);
    }
    
    private static void displayProducts(List<Product> products) {
        System.out.println("\n--- PRODUCT MENU ---");
        for (int i = 0; i < products.size(); i++) {
            Product product = products.get(i);
            System.out.printf("%d. %-15s $%-6.2f - %s%n", 
                i + 1, product.name, product.price, product.description);
        }
    }
    
    private static void displayOrderSummary(List<LineItem> lineItems, double subtotal, double taxAmount, double totalDue) {
        System.out.println("\n--- ORDER SUMMARY ---");
        System.out.printf("%-20s %-8s %-10s %-10s%n", "Item", "Qty", "Price", "Total");
        System.out.println("--------------------------------------------------");
        
        for (LineItem item : lineItems) {
            System.out.printf("%-20s %-8d $%-9.2f $%-9.2f%n",
                item.product.name, item.quantity, item.product.price, item.lineTotal);
        }
        
        System.out.println("--------------------------------------------------");
        System.out.printf("%-20s %-8s %-10s $%-9.2f%n", "Subtotal", "", "", subtotal);
        System.out.printf("%-20s %-8s %-10s $%-9.2f%n", "Tax (" + TAX_RATE + "%)", "", "", taxAmount);
        System.out.printf("%-20s %-8s %-10s $%-9.2f%n", "TOTAL", "", "", totalDue);
    }
    
    private static void processPayment(Scanner scanner, List<LineItem> lineItems, double subtotal, double taxAmount, double totalDue) {
        System.out.println("\n--- PAYMENT PROCESSING ---");
        System.out.println("1. Cash");
        System.out.println("2. Card");
        System.out.print("Select payment method (1=Cash, 2=Card) or type 'Cash'/'Card': ");
        
        String paymentChoice = scanner.nextLine().trim().toLowerCase();
        PaymentMethod paymentMethod;
        
        if (paymentChoice.equals("1") || paymentChoice.equals("cash")) {
            paymentMethod = PaymentMethod.CASH;
            processCashPayment(scanner, lineItems, subtotal, taxAmount, totalDue);
        } else if (paymentChoice.equals("2") || paymentChoice.equals("card")) {
            paymentMethod = PaymentMethod.CARD;
            processCardPayment(scanner, lineItems, subtotal, taxAmount, totalDue);
        } else {
            System.out.println("Invalid payment method. Please enter 1 for Cash or 2 for Card.");
            return;
        }
    }
    
    private static void processCashPayment(Scanner scanner, List<LineItem> lineItems, double subtotal, double taxAmount, double totalDue) {
        System.out.printf("Total due: $%.2f%n", totalDue);
        System.out.print("Enter amount paid: $");
        
        try {
            double amountPaid = Double.parseDouble(scanner.nextLine());
            
            if (amountPaid < totalDue) {
                System.out.println("Insufficient payment. Transaction cancelled.");
                return;
            }
            
            double change = amountPaid - totalDue;
            System.out.printf("Change due: $%.2f%n", change);
            
            // Save transaction
            saveTransaction(lineItems, subtotal, taxAmount, totalDue, "CASH", amountPaid, change, null, null, null);
            
            System.out.println("Transaction completed successfully!");
            
        } catch (NumberFormatException e) {
            System.out.println("Invalid amount. Transaction cancelled.");
        }
    }
    
    private static void processCardPayment(Scanner scanner, List<LineItem> lineItems, double subtotal, double taxAmount, double totalDue) {
        System.out.println("Card Payment Processing");
        System.out.print("Enter card number (last 4 digits): ");
        String cardNumber = scanner.nextLine().trim();
        
        if (cardNumber.length() != 4 || !cardNumber.matches("\\d{4}")) {
            System.out.println("Invalid card number format. Transaction cancelled.");
            return;
        }
        
        System.out.print("Enter cardholder name: ");
        String cardHolderName = scanner.nextLine().trim();
        
        System.out.print("Enter expiry date (MM/YY): ");
        String cardExpiry = scanner.nextLine().trim();
        
        // Mask the card number
        String maskedCardNumber = "****-****-****-" + cardNumber;
        
        // Save transaction
        saveTransaction(lineItems, subtotal, taxAmount, totalDue, "CARD", totalDue, 0.0, 
                       maskedCardNumber, cardHolderName, cardExpiry);
        
        System.out.println("Card payment processed successfully!");
    }
    
    private static void saveTransaction(List<LineItem> lineItems, double subtotal, double taxAmount, 
                                      double totalDue, String paymentMethod, double amountPaid, 
                                      double changeAmount, String cardNumberMasked, 
                                      String cardHolderName, String cardExpiry) {
        
        // Create transaction
        SimpleFileBasedDataStore.SimpleTransaction transaction = new SimpleFileBasedDataStore.SimpleTransaction();
        transaction.transactionId = SimpleFileBasedDataStore.getNextTransactionId();
        transaction.transactionDate = LocalDateTime.now();
        transaction.subtotal = subtotal;
        transaction.taxRatePercent = TAX_RATE;
        transaction.taxAmount = taxAmount;
        transaction.totalDue = totalDue;
        transaction.paymentMethod = paymentMethod;
        transaction.amountPaid = amountPaid;
        transaction.changeAmount = changeAmount;
        transaction.cardNumberMasked = cardNumberMasked;
        transaction.cardHolderName = cardHolderName;
        transaction.cardExpiry = cardExpiry;
        
        // Convert line items
        for (LineItem item : lineItems) {
            SimpleFileBasedDataStore.SimpleLineItem simpleItem = new SimpleFileBasedDataStore.SimpleLineItem();
            simpleItem.transactionId = transaction.transactionId;
            simpleItem.description = item.product.name;
            simpleItem.quantity = item.quantity;
            simpleItem.unitPrice = item.product.price;
            simpleItem.lineTotal = item.lineTotal;
            transaction.lineItems.add(simpleItem);
        }
        
        // Save to file
        SimpleFileBasedDataStore.saveTransaction(transaction);
    }
    
    private static void viewTransactions() {
        System.out.println("\n=== VIEWING TRANSACTIONS ===");
        SimpleFileBasedDataStore.displayAllTransactions();
    }
    
    private static void viewTransactionSummary() {
        System.out.println("\n=== TRANSACTION SUMMARY ===");
        SimpleFileBasedDataStore.displayTransactionSummary();
    }
}

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

public class EcommerceServer {

    private static final int PORT = 8080;
    private static final double TAX_RATE = 8.5; // must match MenuDrivenPaymentConsole

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // API routes
        server.createContext("/api/products", new ProductsHandler());
        server.createContext("/api/transactions", new TransactionsHandler());
        server.createContext("/api/checkout", new CheckoutHandler());

        // Static files from ../web relative to src when launched via run.bat
        server.createContext("/", new StaticFileHandler());

        server.setExecutor(null);
        System.out.println("E-commerce server started at http://localhost:" + PORT);
        server.start();
    }

    // --- Handlers ---
    static class ProductsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                send(exchange, 405, "Method Not Allowed", "text/plain");
                return;
            }
            List<Map<String, Object>> products = getProducts();
            String json = toJsonArray(products);
            send(exchange, 200, json, "application/json");
        }
    }

    static class TransactionsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                send(exchange, 405, "Method Not Allowed", "text/plain");
                return;
            }
            List<SimpleFileBasedDataStore.SimpleTransaction> list = SimpleFileBasedDataStore.loadAllTransactions();
            String json = transactionsToJson(list);
            send(exchange, 200, json, "application/json");
        }
    }

    static class CheckoutHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                send(exchange, 405, "Method Not Allowed", "text/plain");
                return;
            }
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            try {
                Map<String, Object> payload = parseJsonObject(body);
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> items = (List<Map<String, Object>>) payload.get("items");
                String paymentMethod = String.valueOf(payload.get("paymentMethod"));

                if (items == null || items.isEmpty()) {
                    send(exchange, 400, "{\"error\":\"No items provided\"}", "application/json");
                    return;
                }

                // Calculate totals
                double subtotal = 0.0;
                List<LineItemTmp> lineItems = new ArrayList<>();
                for (Map<String, Object> it : items) {
                    String name = String.valueOf(it.get("name"));
                    double price = toDouble(it.get("price"));
                    int qty = (int) Math.round(toDouble(it.get("quantity")));
                    if (qty <= 0) continue;
                    LineItemTmp li = new LineItemTmp(name, price, qty);
                    lineItems.add(li);
                    subtotal += li.lineTotal;
                }
                double tax = subtotal * (TAX_RATE / 100.0);
                double totalDue = subtotal + tax;

                // Build transaction
                SimpleFileBasedDataStore.SimpleTransaction tx = new SimpleFileBasedDataStore.SimpleTransaction();
                tx.transactionId = SimpleFileBasedDataStore.getNextTransactionId();
                tx.transactionDate = LocalDateTime.now();
                tx.subtotal = subtotal;
                tx.taxRatePercent = TAX_RATE;
                tx.taxAmount = tax;
                tx.totalDue = totalDue;

                if ("CASH".equalsIgnoreCase(paymentMethod)) {
                    tx.paymentMethod = "CASH";
                    double amountPaid = toDouble(payload.getOrDefault("amountPaid", totalDue));
                    if (amountPaid < totalDue) {
                        send(exchange, 400, "{\"error\":\"Insufficient cash payment\"}", "application/json");
                        return;
                    }
                    tx.amountPaid = amountPaid;
                    tx.changeAmount = amountPaid - totalDue;
                } else {
                    tx.paymentMethod = "CARD";
                    tx.amountPaid = totalDue;
                    tx.changeAmount = 0.0;
                    tx.cardNumberMasked = "****-****-****-" + String.valueOf(payload.getOrDefault("cardLast4", "0000"));
                    tx.cardHolderName = String.valueOf(payload.getOrDefault("cardHolderName", ""));
                    tx.cardExpiry = String.valueOf(payload.getOrDefault("cardExpiry", ""));
                }

                for (LineItemTmp li : lineItems) {
                    SimpleFileBasedDataStore.SimpleLineItem s = new SimpleFileBasedDataStore.SimpleLineItem();
                    s.transactionId = tx.transactionId;
                    s.description = li.name;
                    s.quantity = li.quantity;
                    s.unitPrice = li.price;
                    s.lineTotal = li.lineTotal;
                    tx.lineItems.add(s);
                }

                SimpleFileBasedDataStore.saveTransaction(tx);

                String resp = "{\"success\":true,\"transactionId\":" + tx.transactionId + ",\"totalDue\":" + round2(totalDue) + "}";
                send(exchange, 200, resp, "application/json");
            } catch (Exception ex) {
                ex.printStackTrace();
                send(exchange, 400, "{\"error\":\"Invalid request\"}", "application/json");
            }
        }
    }

    static class StaticFileHandler implements HttpHandler {
        private final Path baseDir;

        StaticFileHandler() {
            // When run from src using run.bat, static files are in ../web
            this.baseDir = Paths.get("..", "web").toAbsolutePath().normalize();
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/") || path.isEmpty()) {
                serveFile(exchange, baseDir.resolve("index.html"));
                return;
            }
            // Basic path normalization
            path = URLDecoder.decode(path, StandardCharsets.UTF_8);
            Path target = baseDir.resolve(path.substring(1)).normalize();
            if (!target.startsWith(baseDir)) { // prevent path traversal
                send(exchange, 403, "Forbidden", "text/plain");
                return;
            }
            if (Files.exists(target) && !Files.isDirectory(target)) {
                serveFile(exchange, target);
            } else {
                // SPA fallback to index.html
                serveFile(exchange, baseDir.resolve("index.html"));
            }
        }

        private void serveFile(HttpExchange exchange, Path file) throws IOException {
            if (!Files.exists(file)) {
                send(exchange, 404, "Not Found", "text/plain");
                return;
            }
            byte[] bytes = Files.readAllBytes(file);
            String contentType = guessContentType(file.getFileName().toString());
            send(exchange, 200, bytes, contentType);
        }
    }

    // --- Utilities ---

    private static List<Map<String, Object>> getProducts() {
        List<Map<String, Object>> list = new ArrayList<>();
        // Apparel
        addProduct(list, "Classic T-Shirt", 14.99, "100% cotton unisex tee");
        addProduct(list, "Slim Fit Jeans", 39.99, "Denim with stretch comfort");
        addProduct(list, "Hoodie", 29.99, "Fleece-lined pullover hoodie");
        addProduct(list, "Lightweight Jacket", 49.99, "Windbreaker for everyday wear");
        addProduct(list, "Sneakers", 59.99, "Breathable everyday sneakers");

        // Accessories
        addProduct(list, "Backpack", 34.99, "Water-resistant daypack, 20L");
        addProduct(list, "Water Bottle", 12.99, "Insulated stainless steel, 600ml");
        addProduct(list, "Sunglasses", 19.99, "UV400 polarized lenses");
        addProduct(list, "Cap", 11.99, "Adjustable cotton baseball cap");
        addProduct(list, "Wallet", 17.49, "Slim RFID-blocking wallet");

        // Tech & peripherals
        addProduct(list, "Wireless Earbuds", 49.99, "Bluetooth 5.3 with charging case");
        addProduct(list, "Phone Charger", 9.99, "20W USB-C fast charger");
        addProduct(list, "USB-C Cable", 6.99, "1m braided fast-charge cable");
        addProduct(list, "Smartphone Case", 15.99, "Shock-absorbing protective case");
        addProduct(list, "Wireless Mouse", 18.99, "Silent click ergonomic mouse");

        // Stationery
        addProduct(list, "Notebook", 7.49, "A5 dotted journal, 120 pages");
        addProduct(list, "Pen Set", 5.99, "Pack of 5 gel pens, 0.5mm");
        return list;
    }

    private static void addProduct(List<Map<String, Object>> list, String name, double price, String description) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("price", price);
        m.put("description", description);
        list.add(m);
    }

    private static String toJsonArray(List<Map<String, Object>> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(toJsonObject(list.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    private static String toJsonObject(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        int i = 0;
        for (Map.Entry<String, Object> e : map.entrySet()) {
            if (i++ > 0) sb.append(',');
            sb.append('"').append(escape(e.getKey())).append('"').append(":");
            Object v = e.getValue();
            if (v == null) {
                sb.append("null");
            } else if (v instanceof Number || v instanceof Boolean) {
                sb.append(v.toString());
            } else if (v instanceof Map) {
                // no deep maps expected
                sb.append(toJsonObject((Map<String, Object>) v));
            } else if (v instanceof List) {
                // no deep lists expected
                @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>) v;
                sb.append('[');
                for (int j = 0; j < list.size(); j++) {
                    if (j > 0) sb.append(',');
                    Object iv = list.get(j);
                    if (iv instanceof Map) {
                        sb.append(toJsonObject((Map<String, Object>) iv));
                    } else if (iv instanceof Number || iv instanceof Boolean) {
                        sb.append(iv.toString());
                    } else {
                        sb.append('"').append(escape(String.valueOf(iv))).append('"');
                    }
                }
                sb.append(']');
            } else {
                sb.append('"').append(escape(String.valueOf(v))).append('"');
            }
        }
        sb.append('}');
        return sb.toString();
    }

    private static String transactionsToJson(List<SimpleFileBasedDataStore.SimpleTransaction> list) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(',');
            SimpleFileBasedDataStore.SimpleTransaction t = list.get(i);
            sb.append('{')
              .append("\"transactionId\":").append(t.transactionId).append(',')
              .append("\"date\":\"").append(escape(String.valueOf(t.transactionDate))).append("\",")
              .append("\"subtotal\":").append(round2(t.subtotal)).append(',')
              .append("\"tax\":").append(round2(t.taxAmount)).append(',')
              .append("\"total\":").append(round2(t.totalDue)).append(',')
              .append("\"method\":\"").append(escape(t.paymentMethod)).append("\",")
              .append("\"lineItems\":[");
            for (int j = 0; j < t.lineItems.size(); j++) {
                if (j > 0) sb.append(',');
                SimpleFileBasedDataStore.SimpleLineItem li = t.lineItems.get(j);
                sb.append('{')
                  .append("\"description\":\"").append(escape(li.description)).append("\",")
                  .append("\"quantity\":").append(li.quantity).append(',')
                  .append("\"unitPrice\":").append(round2(li.unitPrice)).append(',')
                  .append("\"lineTotal\":").append(round2(li.lineTotal))
                  .append('}');
            }
            sb.append(']')
              .append('}');
        }
        sb.append(']');
        return sb.toString();
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static double toDouble(Object o) {
        if (o == null) return 0.0;
        if (o instanceof Number) return ((Number) o).doubleValue();
        try { return Double.parseDouble(String.valueOf(o)); } catch (Exception e) { return 0.0; }
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    // Very tiny JSON parser for the expected shapes (object with arrays of simple values)
    // This is NOT a general JSON parser; it's just enough for our simple payloads
    private static Map<String, Object> parseJsonObject(String json) {
        json = json.trim();
        if (json.startsWith("{") && json.endsWith("}")) {
            json = json.substring(1, json.length() - 1).trim();
        }
        Map<String, Object> map = new LinkedHashMap<>();
        int i = 0;
        while (i < json.length()) {
            // parse key
            while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;
            if (i >= json.length()) break;
            if (json.charAt(i) == ',') { i++; continue; }
            if (json.charAt(i) != '"') break;
            int j = json.indexOf('"', i + 1);
            String key = json.substring(i + 1, j);
            i = j + 1;
            while (i < json.length() && (json.charAt(i) == ' ' || json.charAt(i) == ':')) i++;
            // parse value (string, number, object, array)
            if (i >= json.length()) break;
            char c = json.charAt(i);
            if (c == '"') {
                int k = json.indexOf('"', i + 1);
                String val = json.substring(i + 1, k);
                map.put(key, val);
                i = k + 1;
            } else if (c == '[') {
                int k = findMatching(json, i);
                String arr = json.substring(i + 1, k);
                map.put(key, parseJsonArray(arr));
                i = k + 1;
            } else if (c == '{') {
                int k = findMatching(json, i);
                String obj = json.substring(i, k + 1);
                map.put(key, parseJsonObject(obj));
                i = k + 1;
            } else {
                int k = i;
                while (k < json.length() && ",}\n ".indexOf(json.charAt(k)) == -1) k++;
                String num = json.substring(i, k);
                try { map.put(key, Double.parseDouble(num)); }
                catch (Exception e) { map.put(key, num); }
                i = k;
            }
            // skip to next
            while (i < json.length() && json.charAt(i) != ',') i++;
            if (i < json.length() && json.charAt(i) == ',') i++;
        }
        return map;
    }

    private static List<Map<String, Object>> parseJsonArray(String json) {
        List<Map<String, Object>> list = new ArrayList<>();
        int i = 0;
        while (i < json.length()) {
            while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;
            if (i >= json.length()) break;
            if (json.charAt(i) == ',') { i++; continue; }
            if (json.charAt(i) == '{') {
                int k = findMatching(json, i);
                String obj = json.substring(i, k + 1);
                list.add(parseJsonObject(obj));
                i = k + 1;
            } else {
                // unsupported element types not used here
                break;
            }
        }
        return list;
    }

    private static int findMatching(String json, int i) {
        char open = json.charAt(i);
        char close = (open == '{') ? '}' : ']';
        int depth = 0;
        for (int k = i; k < json.length(); k++) {
            char c = json.charAt(k);
            if (c == '"') {
                // skip strings
                k++;
                while (k < json.length() && json.charAt(k) != '"') {
                    if (json.charAt(k) == '\\') k++; // escape
                    k++;
                }
                continue;
            }
            if (c == open) depth++;
            else if (c == close) depth--;
            if (depth == 0) return k;
        }
        return json.length() - 1;
    }

    static class LineItemTmp {
        String name; double price; int quantity; double lineTotal;
        LineItemTmp(String n, double p, int q) { name = n; price = p; quantity = q; lineTotal = p * q; }
    }

    private static void send(HttpExchange exchange, int status, String text, String contentType) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        send(exchange, status, bytes, contentType);
    }

    private static void send(HttpExchange exchange, int status, byte[] bytes, String contentType) throws IOException {
        Headers h = exchange.getResponseHeaders();
        h.set("Content-Type", contentType + "; charset=utf-8");
        // Allow XHR from same origin; CORS not needed for same origin
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String guessContentType(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        if (n.endsWith(".html") || n.endsWith(".htm")) return "text/html";
        if (n.endsWith(".css")) return "text/css";
        if (n.endsWith(".js")) return "application/javascript";
        if (n.endsWith(".json")) return "application/json";
        if (n.endsWith(".png")) return "image/png";
        if (n.endsWith(".jpg") || n.endsWith(".jpeg")) return "image/jpeg";
        if (n.endsWith(".svg")) return "image/svg+xml";
        return "text/plain";
    }
}

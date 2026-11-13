package lk.ruhuna.network.inventory;

/** A product's quantity at one branch. */
public final class InventoryItem {
    private final String branchId;
    private final String productId;
    private final String productName;
    private final int quantity;

    public InventoryItem(String branchId, String productId, String productName, int quantity) {
        this.branchId = requireText(branchId, "branchId");
        this.productId = requireText(productId, "productId");
        this.productName = requireText(productName, "productName");
        if (quantity < 0) {
            throw new IllegalArgumentException("quantity cannot be negative");
        }
        this.quantity = quantity;
    }

    public String getBranchId() { return branchId; }
    public String getProductId() { return productId; }
    public String getProductName() { return productName; }
    public int getQuantity() { return quantity; }

    private static String requireText(String value, String name) {
        if (value == null || value.trim().isEmpty() || value.contains(",") || value.contains("\n")) {
            throw new IllegalArgumentException(name + " must be non-empty and cannot contain protocol delimiters");
        }
        return value.trim();
    }
}

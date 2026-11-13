package lk.ruhuna.network.inventory;

import java.util.Collections;
import java.util.List;

/** Result passed back to the server layer; the server formats it using the agreed TCP protocol. */
public final class InventoryResult {
    private final boolean success;
    private final String message;
    private final List<InventoryItem> updatedItems;

    private InventoryResult(boolean success, String message, List<InventoryItem> updatedItems) {
        this.success = success;
        this.message = message;
        this.updatedItems = Collections.unmodifiableList(updatedItems);
    }

    public static InventoryResult success(String message, List<InventoryItem> updatedItems) {
        return new InventoryResult(true, message, updatedItems);
    }

    public static InventoryResult failure(String message) {
        return new InventoryResult(false, message, Collections.emptyList());
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public List<InventoryItem> getUpdatedItems() { return updatedItems; }
}

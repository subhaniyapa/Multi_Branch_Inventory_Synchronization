package lk.ruhuna.network.inventory;

/** An expected inventory rule or persistence failure suitable for an ERROR response. */
public class InventoryException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public InventoryException(String message) { super(message); }
    public InventoryException(String message, Throwable cause) { super(message, cause); }
}

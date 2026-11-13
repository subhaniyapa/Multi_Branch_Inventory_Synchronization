package lk.ruhuna.network.inventory;

import java.util.List;

/** Persistence boundary. A transfer implementation must commit both branch changes atomically. */
public interface InventoryRepository {
    void initialize();
    void registerBranch(String branchId);
    void registerProduct(String productId, String productName);
    void setInitialStock(String branchId, String productId, int quantity);
    List<InventoryItem> findAllStock();
    InventoryItem findStock(String branchId, String productId);
    InventoryItem addStock(String branchId, String productId, int amount);
    InventoryItem sellStock(String branchId, String productId, int amount);
    List<InventoryItem> transferStock(String fromBranch, String toBranch, String productId, int amount);
}

package lk.ruhuna.network.inventory;

import java.util.Collections;
import java.util.List;

/** Inventory rules used by the TCP server. All mutations are serialized within this server process. */
public final class InventoryService {
    private final InventoryRepository repository;

    public InventoryService(InventoryRepository repository) {
        if (repository == null) throw new IllegalArgumentException("repository is required");
        this.repository = repository;
        repository.initialize();
    }

    public List<InventoryItem> getStock() {
        return Collections.unmodifiableList(repository.findAllStock());
    }

    public InventoryItem getStock(String branchId, String productId) {
        return repository.findStock(branchId, productId);
    }

    public synchronized InventoryResult add(String branchId, String productId, int amount) {
        if (amount <= 0) return InventoryResult.failure("Quantity must be greater than zero");
        try {
            InventoryItem item = repository.addStock(branchId, productId, amount);
            return InventoryResult.success("Stock updated", Collections.singletonList(item));
        } catch (InventoryException e) {
            return InventoryResult.failure(e.getMessage());
        }
    }

    public synchronized InventoryResult sell(String branchId, String productId, int amount) {
        if (amount <= 0) return InventoryResult.failure("Quantity must be greater than zero");
        try {
            InventoryItem item = repository.sellStock(branchId, productId, amount);
            return InventoryResult.success("Stock updated", Collections.singletonList(item));
        } catch (InventoryException e) {
            return InventoryResult.failure(e.getMessage());
        }
    }

    public synchronized InventoryResult transfer(String fromBranch, String toBranch, String productId, int amount) {
        if (amount <= 0) return InventoryResult.failure("Quantity must be greater than zero");
        if (fromBranch == null || toBranch == null || fromBranch.equals(toBranch)) {
            return InventoryResult.failure("Source and destination branches must be different");
        }
        try {
            List<InventoryItem> updated = repository.transferStock(fromBranch, toBranch, productId, amount);
            return InventoryResult.success("Stock transferred", updated);
        } catch (InventoryException e) {
            return InventoryResult.failure(e.getMessage());
        }
    }
}

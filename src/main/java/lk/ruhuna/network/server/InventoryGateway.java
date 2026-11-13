package lk.ruhuna.network.server;

import lk.ruhuna.network.inventory.InventoryItem;
import lk.ruhuna.network.inventory.InventoryResult;
import lk.ruhuna.network.inventory.InventoryService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The ONLY door from the network layer to Member 2's InventoryService.
 * - runs each change and broadcasts its UPDATE lines under one lock, so every client sees
 *   updates in the same order they were committed (no stale quantity shown after a newer one);
 * - broadcasts only AFTER the change succeeded;
 * - maps case-insensitive branch/product ids ("kandy", "lp001") to the stored ones, because the
 *   database compares ids case-sensitively.
 */
public class InventoryGateway {
    private final InventoryService service;
    private final ClientManager manager;
    private final Object orderLock = new Object();
    private volatile Map<String, String> branches = new HashMap<>();
    private volatile Map<String, String> products = new HashMap<>();

    public InventoryGateway(InventoryService service, ClientManager manager) {
        this.service = service;
        this.manager = manager;
        refresh();
    }

    // ---------- id lookup ----------

    private void refresh() {
        Map<String, String> b = new HashMap<>(), p = new HashMap<>();
        for (InventoryItem i : service.getStock()) {
            b.put(i.getBranchId().toLowerCase(), i.getBranchId());
            p.put(i.getProductId().toLowerCase(), i.getProductId());
        }
        branches = b;
        products = p;
    }

    /** @return the stored branch id, or null if unknown */
    public String resolveBranch(String text) {
        String key = text.trim().toLowerCase();
        String v = branches.get(key);
        if (v == null) { refresh(); v = branches.get(key); }
        return v;
    }

    /** @return the stored product id, or null if unknown */
    public String resolveProduct(String text) {
        String key = text.trim().toLowerCase();
        String v = products.get(key);
        if (v == null) { refresh(); v = products.get(key); }
        return v;
    }

    public List<InventoryItem> getStock() {
        return service.getStock();
    }

    // ---------- changes (ids must already be resolved) ----------

    public InventoryResult add(String branch, String product, int qty) {
        return run(() -> service.add(branch, product, qty));
    }

    public InventoryResult sell(String branch, String product, int qty) {
        return run(() -> service.sell(branch, product, qty));
    }

    public InventoryResult transfer(String from, String to, String product, int qty) {
        return run(() -> service.transfer(from, to, product, qty));
    }

    private InventoryResult run(Supplier<InventoryResult> operation) {
        synchronized (orderLock) {
            InventoryResult result;
            try {
                result = operation.get();
            } catch (RuntimeException e) {
                System.err.println("[GATEWAY] inventory call failed: " + e);
                return InventoryResult.failure("Server error");
            }
            if (result.isSuccess()) {
                for (InventoryItem i : result.getUpdatedItems()) {
                    manager.broadcast("UPDATE," + i.getBranchId() + "," + i.getProductId() + "," + i.getQuantity());
                }
            }
            return result;
        }
    }
}

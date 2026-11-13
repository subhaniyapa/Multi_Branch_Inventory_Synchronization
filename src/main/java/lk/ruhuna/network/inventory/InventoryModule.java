package lk.ruhuna.network.inventory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Startup entry point for the inventory module, intended to be called by the TCP server. */
public final class InventoryModule {
    private static final String DEFAULT_DATABASE_FILE = "data/inventory.db";

    private InventoryModule() { }

    /** Opens the default server-side database and inserts demo data on first startup only. */
    public static InventoryService openDefault() {
        return open(DEFAULT_DATABASE_FILE);
    }

    /** Opens a database at the supplied path and inserts demo data on first startup only. */
    public static InventoryService open(String databaseFile) {
        Path databasePath = Paths.get(databaseFile).toAbsolutePath();
        Path parent = databasePath.getParent();
        try {
            if (parent != null) Files.createDirectories(parent);
        } catch (IOException e) {
            throw new InventoryException("Could not create inventory database directory", e);
        }

        SQLiteInventoryRepository repository = new SQLiteInventoryRepository(databasePath.toString());
        InventoryService service = new InventoryService(repository);
        repository.initializeDemoDataIfEmpty();
        return service;
    }
}

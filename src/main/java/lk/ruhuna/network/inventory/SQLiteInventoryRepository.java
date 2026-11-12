package lk.ruhuna.network.inventory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** SQLite/JDBC storage. Keep the database file on the server computer; clients use the TCP server. */
public final class SQLiteInventoryRepository implements InventoryRepository {
    private final String jdbcUrl;

    public SQLiteInventoryRepository(String databaseFile) {
        if (databaseFile == null || databaseFile.trim().isEmpty()) {
            throw new IllegalArgumentException("databaseFile is required");
        }
        this.jdbcUrl = "jdbc:sqlite:" + databaseFile;
    }

    @Override
    public void initialize() {
        try (Connection connection = open(); Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("CREATE TABLE IF NOT EXISTS branches (branch_id TEXT PRIMARY KEY)");
            statement.execute("CREATE TABLE IF NOT EXISTS products (product_id TEXT PRIMARY KEY, product_name TEXT NOT NULL)");
            statement.execute("CREATE TABLE IF NOT EXISTS branch_stock (" +
                    "branch_id TEXT NOT NULL, product_id TEXT NOT NULL, quantity INTEGER NOT NULL CHECK(quantity >= 0), " +
                    "PRIMARY KEY(branch_id, product_id), " +
                    "FOREIGN KEY(branch_id) REFERENCES branches(branch_id), " +
                    "FOREIGN KEY(product_id) REFERENCES products(product_id))");
        } catch (SQLException e) {
            throw persistenceFailure("Could not initialize inventory database", e);
        }
    }

    @Override
    public void registerBranch(String branchId) {
        requireText(branchId, "branchId");
        execute("INSERT OR IGNORE INTO branches(branch_id) VALUES (?)", branchId);
    }

    @Override
    public void registerProduct(String productId, String productName) {
        requireText(productId, "productId");
        requireText(productName, "productName");
        try (Connection c = open(); PreparedStatement p = c.prepareStatement(
                "INSERT INTO products(product_id, product_name) VALUES (?, ?) " +
                        "ON CONFLICT(product_id) DO UPDATE SET product_name = excluded.product_name")) {
            p.setString(1, productId);
            p.setString(2, productName);
            p.executeUpdate();
        } catch (SQLException e) {
            throw persistenceFailure("Could not register product", e);
        }
    }

    @Override
    public void setInitialStock(String branchId, String productId, int quantity) {
        if (quantity < 0) throw new IllegalArgumentException("quantity cannot be negative");
        try (Connection c = open(); PreparedStatement p = c.prepareStatement(
                "INSERT INTO branch_stock(branch_id, product_id, quantity) VALUES (?, ?, ?) " +
                        "ON CONFLICT(branch_id, product_id) DO UPDATE SET quantity = excluded.quantity")) {
            p.setString(1, branchId);
            p.setString(2, productId);
            p.setInt(3, quantity);
            p.executeUpdate();
        } catch (SQLException e) {
            throw persistenceFailure("Could not set initial stock; check that branch and product exist", e);
        }
    }

    @Override
    public List<InventoryItem> findAllStock() {
        List<InventoryItem> items = new ArrayList<>();
        String sql = "SELECT bs.branch_id, bs.product_id, p.product_name, bs.quantity " +
                "FROM branch_stock bs JOIN products p ON p.product_id = bs.product_id " +
                "ORDER BY bs.branch_id, bs.product_id";
        try (Connection c = open(); Statement s = c.createStatement(); ResultSet r = s.executeQuery(sql)) {
            while (r.next()) items.add(readItem(r));
            return items;
        } catch (SQLException e) {
            throw persistenceFailure("Could not read inventory", e);
        }
    }

    @Override
    public InventoryItem findStock(String branchId, String productId) {
        String sql = "SELECT b.branch_id, p.product_id, p.product_name, COALESCE(bs.quantity, 0) AS quantity " +
                "FROM branches b CROSS JOIN products p LEFT JOIN branch_stock bs " +
                "ON bs.branch_id = b.branch_id AND bs.product_id = p.product_id " +
                "WHERE b.branch_id = ? AND p.product_id = ?";
        try (Connection c = open(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, branchId);
            p.setString(2, productId);
            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) throw new InventoryException("Unknown branch or product");
                return readItem(r);
            }
        } catch (SQLException e) {
            throw persistenceFailure("Could not read stock", e);
        }
    }

    @Override
    public InventoryItem addStock(String branchId, String productId, int amount) {
        return inTransaction(c -> {
            requireKnown(c, branchId, productId);
            try (PreparedStatement p = c.prepareStatement(
                    "INSERT INTO branch_stock(branch_id, product_id, quantity) VALUES (?, ?, ?) " +
                            "ON CONFLICT(branch_id, product_id) DO UPDATE SET quantity = quantity + excluded.quantity")) {
                p.setString(1, branchId);
                p.setString(2, productId);
                p.setInt(3, amount);
                p.executeUpdate();
            }
            return findStock(c, branchId, productId);
        });
    }

    @Override
    public InventoryItem sellStock(String branchId, String productId, int amount) {
        return inTransaction(c -> {
            requireKnown(c, branchId, productId);
            try (PreparedStatement p = c.prepareStatement(
                    "UPDATE branch_stock SET quantity = quantity - ? WHERE branch_id = ? AND product_id = ? AND quantity >= ?")) {
                p.setInt(1, amount);
                p.setString(2, branchId);
                p.setString(3, productId);
                p.setInt(4, amount);
                if (p.executeUpdate() != 1) throw new InventoryException("Insufficient stock");
            }
            return findStock(c, branchId, productId);
        });
    }

    @Override
    public List<InventoryItem> transferStock(String fromBranch, String toBranch, String productId, int amount) {
        return inTransaction(c -> {
            requireKnown(c, fromBranch, productId);
            requireKnown(c, toBranch, productId);
            try (PreparedStatement p = c.prepareStatement(
                    "UPDATE branch_stock SET quantity = quantity - ? WHERE branch_id = ? AND product_id = ? AND quantity >= ?")) {
                p.setInt(1, amount);
                p.setString(2, fromBranch);
                p.setString(3, productId);
                p.setInt(4, amount);
                if (p.executeUpdate() != 1) throw new InventoryException("Insufficient stock");
            }
            try (PreparedStatement p = c.prepareStatement(
                    "INSERT INTO branch_stock(branch_id, product_id, quantity) VALUES (?, ?, ?) " +
                            "ON CONFLICT(branch_id, product_id) DO UPDATE SET quantity = quantity + excluded.quantity")) {
                p.setString(1, toBranch);
                p.setString(2, productId);
                p.setInt(3, amount);
                p.executeUpdate();
            }
            List<InventoryItem> updated = new ArrayList<>();
            updated.add(findStock(c, fromBranch, productId));
            updated.add(findStock(c, toBranch, productId));
            return updated;
        });
    }

    private Connection open() throws SQLException {
        Connection connection = DriverManager.getConnection(jdbcUrl);
        try (Statement s = connection.createStatement()) {
            s.execute("PRAGMA foreign_keys = ON");
            s.execute("PRAGMA busy_timeout = 5000");
        }
        return connection;
    }

    private void execute(String sql, String value) {
        try (Connection c = open(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, value);
            p.executeUpdate();
        } catch (SQLException e) {
            throw persistenceFailure("Could not update inventory database", e);
        }
    }

    private interface SqlWork<T> { T run(Connection connection) throws SQLException; }

    private <T> T inTransaction(SqlWork<T> work) {
        try (Connection c = open()) {
            c.setAutoCommit(false);
            try {
                T result = work.run(c);
                c.commit();
                return result;
            } catch (Exception e) {
                try { c.rollback(); } catch (SQLException rollbackError) { e.addSuppressed(rollbackError); }
                if (e instanceof InventoryException) throw (InventoryException) e;
                if (e instanceof SQLException) throw persistenceFailure("Inventory transaction failed", (SQLException) e);
                if (e instanceof RuntimeException) throw (RuntimeException) e;
                throw persistenceFailure("Inventory transaction failed", new SQLException(e));
            }
        } catch (SQLException e) {
            throw persistenceFailure("Could not open inventory transaction", e);
        }
    }

    private void requireKnown(Connection c, String branchId, String productId) throws SQLException {
        try (PreparedStatement p = c.prepareStatement(
                "SELECT 1 FROM branches b CROSS JOIN products pr WHERE b.branch_id = ? AND pr.product_id = ?")) {
            p.setString(1, branchId);
            p.setString(2, productId);
            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) throw new InventoryException("Unknown branch or product");
            }
        }
    }

    private InventoryItem findStock(Connection c, String branchId, String productId) throws SQLException {
        String sql = "SELECT b.branch_id, p.product_id, p.product_name, COALESCE(bs.quantity, 0) AS quantity " +
                "FROM branches b CROSS JOIN products p LEFT JOIN branch_stock bs " +
                "ON bs.branch_id = b.branch_id AND bs.product_id = p.product_id " +
                "WHERE b.branch_id = ? AND p.product_id = ?";
        try (PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, branchId);
            p.setString(2, productId);
            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) throw new InventoryException("Unknown branch or product");
                return readItem(r);
            }
        }
    }

    private InventoryItem readItem(ResultSet r) throws SQLException {
        return new InventoryItem(r.getString("branch_id"), r.getString("product_id"),
                r.getString("product_name"), r.getInt("quantity"));
    }

    private static void requireText(String value, String name) {
        if (value == null || value.trim().isEmpty() || value.contains(",") || value.contains("\n")) {
            throw new IllegalArgumentException(name + " must be non-empty and cannot contain protocol delimiters");
        }
    }

    private static InventoryException persistenceFailure(String message, SQLException cause) {
        return new InventoryException(message, cause);
    }
}

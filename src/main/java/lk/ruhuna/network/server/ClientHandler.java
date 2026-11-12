package lk.ruhuna.network.server;

import lk.ruhuna.network.inventory.InventoryItem;
import lk.ruhuna.network.inventory.InventoryResult;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Handles ONE connected client on a worker thread.
 * Protocol: UTF-8, one message per line, fields separated by ','.
 *
 *   CONNECT,branch                       -> CONNECTED,branch
 *   GET_STOCK [,branch]                  -> STOCK,branch,productId,name,qty ... STOCK_END      (alias CHECK_STOCK)
 *   ADD,branch,productId,qty             -> SUCCESS,msg | ERROR,msg                            (alias ADD_STOCK)
 *   SELL,branch,productId,qty            -> SUCCESS,msg | ERROR,msg                            (alias REMOVE_STOCK)
 *   TRANSFER,from,to,productId,qty       -> SUCCESS,msg | ERROR,msg                            (alias TRANSFER_STOCK)
 *   broadcast to everyone after a change -> UPDATE,branch,productId,newQty
 */
public class ClientHandler implements Runnable {
    private final Socket socket;
    private final InventoryGateway inventory;
    private final ClientManager manager;
    private volatile PrintWriter out;
    private volatile String branchId;          // null until CONNECT succeeds

    public ClientHandler(Socket socket, InventoryGateway inventory, ClientManager manager) {
        this.socket = socket;
        this.inventory = inventory;
        this.manager = manager;
    }

    public String getBranchId() {
        return branchId;
    }

    /** Thread-safe: replies and broadcasts can arrive from different threads at the same time. */
    public synchronized void send(String message) {
        PrintWriter w = out;
        if (w == null) return;
        w.print(message + "\n");
        w.flush();
        if (w.checkError()) {                  // client gone
            closeQuietly();
        }
    }

    @Override
    public void run() {
        String t = Thread.currentThread().getName();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            String line;
            while ((line = in.readLine()) != null) {          // null = client disconnected
                if (line.isBlank()) continue;
                System.out.println("[" + t + "] <- " + line);
                try {
                    handle(line);
                } catch (RuntimeException e) {                // a bad request must never kill the handler
                    System.err.println("[" + t + "] request failed: " + e);
                    send("ERROR,Server error");
                }
            }
        } catch (IOException e) {
            System.out.println("[" + t + "] connection closed: " + e.getMessage());
        } finally {
            manager.remove(this);
            closeQuietly();
            System.out.println("[" + t + "] " + (branchId == null ? "client" : branchId) + " disconnected");
        }
    }

    // ---------------- command handling ----------------

    private void handle(String line) {
        String[] p = line.trim().split(",", -1);
        for (int i = 0; i < p.length; i++) p[i] = p[i].trim();
        String cmd = p[0].toUpperCase();

        if (cmd.equals("CONNECT")) { connect(p); return; }
        if (cmd.equals("PING")) { send("PONG"); return; }
        if (branchId == null) { send("ERROR,Not connected"); return; }

        switch (cmd) {
            case "GET_STOCK", "CHECK_STOCK" -> getStock(p);
            case "ADD", "ADD_STOCK" -> change(p, true);
            case "SELL", "REMOVE_STOCK" -> change(p, false);
            case "TRANSFER", "TRANSFER_STOCK" -> transfer(p);
            default -> send("ERROR,Unknown command");
        }
    }

    private void connect(String[] p) {
        if (p.length != 2) { send("ERROR,Invalid command"); return; }
        if (branchId != null) { send("ERROR,Already connected"); return; }
        String b = inventory.resolveBranch(p[1]);
        if (b == null) { send("ERROR,Unknown branch"); return; }
        branchId = b;
        manager.register(this);
        send("CONNECTED," + b);
    }

    /** GET_STOCK  (all branches)  |  GET_STOCK,branch */
    private void getStock(String[] p) {
        if (p.length > 2) { send("ERROR,Invalid command"); return; }
        String only = null;
        if (p.length == 2) {
            only = inventory.resolveBranch(p[1]);
            if (only == null) { send("ERROR,Unknown branch"); return; }
        }
        for (InventoryItem i : inventory.getStock()) {
            if (only == null || only.equals(i.getBranchId())) {
                send("STOCK," + i.getBranchId() + "," + i.getProductId() + "," + i.getProductName() + "," + i.getQuantity());
            }
        }
        send("STOCK_END");
    }

    /** ADD,branch,product,qty  |  SELL,branch,product,qty */
    private void change(String[] p, boolean add) {
        if (p.length != 4) { send("ERROR,Invalid command"); return; }
        String b = inventory.resolveBranch(p[1]);
        if (b == null) { send("ERROR,Unknown branch"); return; }
        if (!b.equals(branchId)) { send("ERROR,Branch mismatch"); return; }
        String prod = inventory.resolveProduct(p[2]);
        if (prod == null) { send("ERROR,Unknown product"); return; }
        Integer qty = parseQty(p[3]);
        if (qty == null) { send("ERROR,Invalid quantity"); return; }
        reply(add ? inventory.add(b, prod, qty) : inventory.sell(b, prod, qty));
    }

    /** TRANSFER,fromBranch,toBranch,product,qty  (from must be this client's own branch) */
    private void transfer(String[] p) {
        if (p.length != 5) { send("ERROR,Invalid command"); return; }
        String from = inventory.resolveBranch(p[1]);
        String to = inventory.resolveBranch(p[2]);
        if (from == null || to == null) { send("ERROR,Unknown branch"); return; }
        if (!from.equals(branchId)) { send("ERROR,Branch mismatch"); return; }
        String prod = inventory.resolveProduct(p[3]);
        if (prod == null) { send("ERROR,Unknown product"); return; }
        Integer qty = parseQty(p[4]);
        if (qty == null) { send("ERROR,Invalid quantity"); return; }
        reply(inventory.transfer(from, to, prod, qty));
    }

    private void reply(InventoryResult r) {
        String msg = r.getMessage().replace(',', ';').replace('\n', ' ');   // keep the protocol parseable
        send((r.isSuccess() ? "SUCCESS," : "ERROR,") + msg);
    }

    private static Integer parseQty(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void closeQuietly() {
        try { socket.close(); } catch (IOException ignored) { }
    }
}

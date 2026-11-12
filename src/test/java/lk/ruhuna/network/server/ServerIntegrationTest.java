package lk.ruhuna.network.server;

import lk.ruhuna.network.inventory.*;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/**
 * Real sockets + the real InventoryService, with an in-memory repository (no SQLite needed to run this).
 * Run: java -cp target/classes;target/test-classes lk.ruhuna.network.server.ServerIntegrationTest
 */
public class ServerIntegrationTest {
    static int failures = 0;
    static final int PORT = 5056;

    static void check(String name, boolean ok) {
        System.out.println((ok ? "PASS  " : "FAIL  ") + name);
        if (!ok) failures++;
    }

    /** Same demo data as SQLiteInventoryRepository.initializeDemoDataIfEmpty(). */
    static class MemoryRepository implements InventoryRepository {
        final Map<String, String> productNames = new LinkedHashMap<>();
        final Map<String, Map<String, Integer>> stock = new LinkedHashMap<>();

        public void initialize() { }
        public void registerBranch(String b) { stock.putIfAbsent(b, new LinkedHashMap<>()); }
        public void registerProduct(String id, String name) { productNames.put(id, name); }
        public void setInitialStock(String b, String p, int q) { stock.get(b).put(p, q); }

        synchronized MemoryRepository seed() {
            String[] br = {"Kandy", "Colombo", "Galle"};
            String[][] pr = {{"LP001", "Dell Inspiron 15"}, {"LP002", "HP Pavilion 14"}, {"LP003", "Lenovo ThinkPad E14"}};
            int[][] q = {{5, 4, 3}, {8, 6, 5}, {3, 2, 2}};
            for (int b = 0; b < 3; b++) {
                registerBranch(br[b]);
                for (int p = 0; p < 3; p++) { registerProduct(pr[p][0], pr[p][1]); setInitialStock(br[b], pr[p][0], q[b][p]); }
            }
            return this;
        }

        private void known(String b, String p) {
            if (!stock.containsKey(b) || !productNames.containsKey(p)) throw new InventoryException("Unknown branch or product");
        }
        private InventoryItem item(String b, String p) {
            return new InventoryItem(b, p, productNames.get(p), stock.get(b).getOrDefault(p, 0));
        }
        public synchronized List<InventoryItem> findAllStock() {
            List<InventoryItem> l = new ArrayList<>();
            for (String b : stock.keySet()) for (String p : productNames.keySet()) l.add(item(b, p));
            return l;
        }
        public synchronized InventoryItem findStock(String b, String p) { known(b, p); return item(b, p); }
        public synchronized InventoryItem addStock(String b, String p, int n) {
            known(b, p); stock.get(b).merge(p, n, Integer::sum); return item(b, p);
        }
        public synchronized InventoryItem sellStock(String b, String p, int n) {
            known(b, p);
            if (stock.get(b).getOrDefault(p, 0) < n) throw new InventoryException("Insufficient stock");
            stock.get(b).merge(p, -n, Integer::sum); return item(b, p);
        }
        public synchronized List<InventoryItem> transferStock(String f, String t, String p, int n) {
            known(f, p); known(t, p);
            if (stock.get(f).getOrDefault(p, 0) < n) throw new InventoryException("Insufficient stock");
            stock.get(f).merge(p, -n, Integer::sum); stock.get(t).merge(p, n, Integer::sum);
            return List.of(item(f, p), item(t, p));
        }
    }

    static class TestClient implements AutoCloseable {
        final Socket s;
        final BufferedReader in;
        final PrintWriter out;

        TestClient() throws IOException {
            s = new Socket("127.0.0.1", PORT);
            s.setSoTimeout(3000);
            in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
            out = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8), true);
        }
        void send(String m) { out.println(m); }
        String read() { try { return in.readLine(); } catch (IOException e) { return null; } }
        List<String> until(String prefix) {
            List<String> lines = new ArrayList<>();
            String l;
            while ((l = read()) != null) { lines.add(l); if (l.startsWith(prefix)) break; }
            return lines;
        }
        String ask(String m, String prefix) {
            send(m);
            List<String> ls = until(prefix);
            return ls.isEmpty() ? null : ls.get(ls.size() - 1);
        }
        /** This client's own SUCCESS/ERROR reply, skipping UPDATE broadcasts. */
        String reply() {
            String l;
            while ((l = read()) != null) if (l.startsWith("SUCCESS") || l.startsWith("ERROR")) return l;
            return "?";
        }
        public void close() throws IOException { s.close(); }
    }

    static TestClient connect(String branch) throws IOException {
        TestClient c = new TestClient();
        c.send("CONNECT," + branch);
        c.until("CONNECTED");
        return c;
    }

    static int qty(InventoryService s, String b, String p) { return s.getStock(b, p).getQuantity(); }

    public static void main(String[] args) throws Exception {
        InventoryService svc = new InventoryService(new MemoryRepository().seed());
        InventoryServer server = new InventoryServer(PORT, svc);
        Thread st = new Thread(() -> { try { server.start(); } catch (IOException e) { e.printStackTrace(); } });
        st.setDaemon(true);
        st.start();
        Thread.sleep(500);

        try (TestClient kandy = new TestClient(); TestClient colombo = new TestClient(); TestClient galle = new TestClient()) {
            check("command before CONNECT rejected", "ERROR,Not connected".equals(kandy.ask("GET_STOCK", "ERROR")));
            check("CONNECT unknown branch rejected", "ERROR,Unknown branch".equals(kandy.ask("CONNECT,Matara", "ERROR")));
            check("CONNECT Kandy", "CONNECTED,Kandy".equals(kandy.ask("CONNECT,Kandy", "CONNECTED")));
            check("CONNECT is case-insensitive (colombo)", "CONNECTED,Colombo".equals(colombo.ask("CONNECT,colombo", "CONNECTED")));
            check("CONNECT Galle", "CONNECTED,Galle".equals(galle.ask("CONNECT,Galle", "CONNECTED")));

            kandy.send("GET_STOCK");
            List<String> all = kandy.until("STOCK_END");
            check("GET_STOCK lists all 9 rows then STOCK_END", all.size() == 10 && all.contains("STOCK,Kandy,LP001,Dell Inspiron 15,5"));
            kandy.send("GET_STOCK,Galle");
            check("GET_STOCK,branch lists only that branch", kandy.until("STOCK_END").size() == 4);

            // real-time sync
            kandy.send("SELL,Kandy,LP001,2");
            check("requester gets SUCCESS", "SUCCESS,Stock updated".equals(kandy.reply()));
            check("Colombo receives UPDATE,Kandy,LP001,3", "UPDATE,Kandy,LP001,3".equals(colombo.read()));
            check("Galle receives UPDATE,Kandy,LP001,3", "UPDATE,Kandy,LP001,3".equals(galle.read()));
            kandy.send("sell,kandy,lp002,1");
            check("lower-case command and ids work", "SUCCESS,Stock updated".equals(kandy.reply()));
            colombo.read(); galle.read();

            // validation
            check("insufficient stock", "ERROR,Insufficient stock".equals(kandy.ask("SELL,Kandy,LP001,500", "ERROR")));
            check("branch mismatch", "ERROR,Branch mismatch".equals(kandy.ask("SELL,Colombo,LP001,1", "ERROR")));
            check("invalid quantity text", "ERROR,Invalid quantity".equals(kandy.ask("SELL,Kandy,LP001,abc", "ERROR")));
            check("zero quantity (message from Member 2's service)", kandy.ask("SELL,Kandy,LP001,0", "ERROR").startsWith("ERROR,Quantity must be greater than zero"));
            check("negative quantity", kandy.ask("ADD,Kandy,LP001,-4", "ERROR").startsWith("ERROR,Quantity must be greater"));
            check("wrong field count", "ERROR,Invalid command".equals(kandy.ask("ADD,Kandy", "ERROR")));
            check("unknown command", "ERROR,Unknown command".equals(kandy.ask("FLY_AWAY", "ERROR")));
            check("unknown product", "ERROR,Unknown product".equals(kandy.ask("ADD,Kandy,XX999,1", "ERROR")));
            check("old command names still work (REMOVE_STOCK)", "SUCCESS,Stock updated".equals(sendAndReply(kandy, "REMOVE_STOCK,Kandy,LP003,1")));
            colombo.read(); galle.read();

            // transfer
            kandy.send("TRANSFER,Kandy,Galle,LP003,2");
            kandy.reply();
            Set<String> got = new HashSet<>(List.of(colombo.read(), colombo.read()));
            check("transfer broadcasts both changes", got.equals(Set.of("UPDATE,Kandy,LP003,0", "UPDATE,Galle,LP003,4")));
            galle.read(); galle.read();
            check("failed transfer -> error", "ERROR,Insufficient stock".equals(kandy.ask("TRANSFER,Kandy,Galle,LP003,50", "ERROR")));
            check("failed transfer leaves BOTH branches unchanged", qty(svc, "Kandy", "LP003") == 0 && qty(svc, "Galle", "LP003") == 4);
            check("transfer to same branch rejected", kandy.ask("TRANSFER,Kandy,Kandy,LP001,1", "ERROR").startsWith("ERROR,Source and destination"));
        }

        // Scenario 3 over the network: two cashiers at Kandy, stock 10, sell 7 and 6 at the same time
        svc.add("Kandy", "LP002", 10 - qty(svc, "Kandy", "LP002"));
        try (TestClient a = connect("Kandy"); TestClient b = connect("Kandy")) {
            CountDownLatch go = new CountDownLatch(1);
            ExecutorService ex = Executors.newFixedThreadPool(2);
            Future<String> ra = ex.submit(() -> { go.await(); a.send("SELL,Kandy,LP002,7"); return a.reply(); });
            Future<String> rb = ex.submit(() -> { go.await(); b.send("SELL,Kandy,LP002,6"); return b.reply(); });
            go.countDown();
            String x = ra.get(), y = rb.get();
            ex.shutdown();
            int left = qty(svc, "Kandy", "LP002");
            System.out.println("      replies: " + x + " | " + y + " | left=" + left);
            long ok = Arrays.asList(x, y).stream().filter(s -> s.startsWith("SUCCESS")).count();
            check("7 vs 6 over TCP: exactly one succeeds, left is 3 or 4", ok == 1 && (left == 3 || left == 4));
        }

        // Many cashiers at once: final stock must match the successful sales
        int cashiers = 8, sellsEach = 40, start = 1000;
        svc.add("Colombo", "LP001", start);
        int before = qty(svc, "Colombo", "LP001");
        List<TestClient> cs = new ArrayList<>();
        for (int i = 0; i < cashiers; i++) cs.add(connect("Colombo"));
        ExecutorService ex = Executors.newFixedThreadPool(cashiers);
        List<Future<Integer>> fs = new ArrayList<>();
        for (TestClient c : cs) {
            fs.add(ex.submit(() -> {
                int ok = 0;
                for (int i = 0; i < sellsEach; i++) { c.send("SELL,Colombo,LP001,1"); if (c.reply().startsWith("SUCCESS")) ok++; }
                return ok;
            }));
        }
        int sold = 0;
        for (Future<Integer> f : fs) sold += f.get();
        ex.shutdown();
        check("load: " + cashiers * sellsEach + " concurrent sales, final == start - successful (sold " + sold + ")",
                qty(svc, "Colombo", "LP001") == before - sold && sold == cashiers * sellsEach);
        for (TestClient c : cs) c.close();

        // Disconnect: server survives a client that vanishes
        TestClient gone = connect("Galle");
        TestClient alive = connect("Colombo");
        gone.close();
        Thread.sleep(300);
        svc.add("Kandy", "LP001", 1);
        // add through the service directly does not broadcast; use the network for that:
        alive.send("ADD,Colombo,LP001,1");
        check("after a client disconnects, requests still work", alive.reply().startsWith("SUCCESS"));
        check("server still answers after disconnect", "PONG".equals(alive.ask("PING", "PONG")));
        alive.close();

        server.stop();
        System.out.println(failures == 0 ? "\nALL INTEGRATION TESTS PASSED" : "\n" + failures + " TEST(S) FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    static String sendAndReply(TestClient c, String cmd) { c.send(cmd); return c.reply(); }
}

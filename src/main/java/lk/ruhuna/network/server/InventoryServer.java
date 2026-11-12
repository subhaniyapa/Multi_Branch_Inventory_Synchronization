package lk.ruhuna.network.server;

import lk.ruhuna.network.inventory.InventoryModule;
import lk.ruhuna.network.inventory.InventoryService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Usage: java lk.ruhuna.network.server.InventoryServer [port] [databaseFile]
 * The main thread only accepts connections; a bounded thread pool handles the clients.
 */
public class InventoryServer {
    public static final int DEFAULT_PORT = 5000;
    private static final int POOL_SIZE = 10;

    private final int port;
    private final ClientManager manager = new ClientManager();
    private final InventoryGateway gateway;
    private final ExecutorService pool = Executors.newFixedThreadPool(POOL_SIZE);
    private volatile ServerSocket serverSocket;

    public InventoryServer(int port, InventoryService service) {
        this.port = port;
        this.gateway = new InventoryGateway(service, manager);
    }

    /** Blocks until stop() is called. */
    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("[SERVER] Listening on port " + port);
        try {
            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();
                System.out.println("[SERVER] New connection from " + socket.getRemoteSocketAddress());
                pool.submit(new ClientHandler(socket, gateway, manager));
            }
        } catch (IOException e) {
            if (!serverSocket.isClosed()) throw e;      // closed on purpose = normal stop
        } finally {
            pool.shutdown();
        }
    }

    public void stop() throws IOException {
        if (serverSocket != null) serverSocket.close();
    }

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        InventoryService service = args.length > 1 ? InventoryModule.open(args[1]) : InventoryModule.openDefault();
        new InventoryServer(port, service).start();
    }
}

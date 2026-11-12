package lk.ruhuna.network.server;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Thread-safe registry of connected clients. Several clients may use the same branch (e.g. 2 cashiers). */
public class ClientManager {
    private final Set<ClientHandler> clients = ConcurrentHashMap.newKeySet();

    public void register(ClientHandler handler) {
        clients.add(handler);
        System.out.println("[MANAGER] " + handler.getBranchId() + " online. Connected clients: " + clients.size());
    }

    public void remove(ClientHandler handler) {
        if (clients.remove(handler)) {
            System.out.println("[MANAGER] " + handler.getBranchId() + " offline. Connected clients: " + clients.size());
        }
    }

    /** Sends to every connected client. One broken client never stops the others. */
    public void broadcast(String message) {
        for (ClientHandler c : clients) {
            c.send(message);
        }
    }

    public int count() {
        return clients.size();
    }
}

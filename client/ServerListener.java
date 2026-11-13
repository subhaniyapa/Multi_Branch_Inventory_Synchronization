
package client;

import java.io.BufferedReader;
import java.io.IOException;

public class ServerListener implements Runnable {

    private final BufferedReader input;

    public ServerListener(BufferedReader input) {
        this.input = input;
    }

    @Override
    public void run() {
        try {
            String message;

            while ((message = input.readLine()) != null) {
                System.out.println();
                System.out.println("[SERVER] " + message);
            }

            System.out.println("Server disconnected.");

        } catch (IOException e) {
            System.out.println(
                "Connection closed: " + e.getMessage()
            );
        }
    }
}

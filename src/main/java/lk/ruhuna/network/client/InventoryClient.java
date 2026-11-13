package lk.ruhuna.network.client;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/** Console client. Usage: java lk.ruhuna.network.client.InventoryClient Kandy [host] [port] */
public class InventoryClient {
    public static void main(String[] args) throws IOException {
        String branch = args.length > 0 ? args[0] : "Kandy";
        String host = args.length > 1 ? args[1] : "127.0.0.1";
        int port = args.length > 2 ? Integer.parseInt(args[2]) : 5000;

        try (Socket socket = new Socket(host, port);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
             BufferedReader keyboard = new BufferedReader(new InputStreamReader(System.in))) {

            // Receiver thread: prints everything the server sends (replies AND real-time UPDATE broadcasts)
            Thread receiver = new Thread(() -> {
                try {
                    String msg;
                    while ((msg = in.readLine()) != null) System.out.println("SERVER: " + msg);
                } catch (IOException ignored) { }
                System.out.println("Disconnected from server.");
            }, "receiver");
            receiver.setDaemon(true);
            receiver.start();

            out.println("CONNECT," + branch);
            System.out.println("Connecting as " + branch + ". Examples: GET_STOCK | SELL," + branch
                    + ",LP001,1 | ADD," + branch + ",LP001,5 | TRANSFER," + branch + ",Galle,LP001,1 | exit");
            String line;
            while ((line = keyboard.readLine()) != null && !line.equalsIgnoreCase("exit")) {
                out.println(line);
            }
        }
    }
}

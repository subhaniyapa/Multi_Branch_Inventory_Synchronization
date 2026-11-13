
package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class BranchClient {

    private static final String SERVER_HOST = "127.0.0.1";
    private static final int SERVER_PORT = 5000;

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        System.out.println("=== Branch Inventory Client ===");

        System.out.print("Enter Branch Name: ");
        String branchName = scanner.nextLine().trim();

        if (branchName.isEmpty() || branchName.contains(",")) {
            System.out.println("Invalid branch name.");
            return;
        }

        System.out.println("Connecting to server...");

        try (
            Socket socket = new Socket(SERVER_HOST, SERVER_PORT);

            BufferedReader input = new BufferedReader(
                new InputStreamReader(
                    socket.getInputStream(),
                    StandardCharsets.UTF_8
                )
            );

            PrintWriter output = new PrintWriter(
                socket.getOutputStream(),
                true,
                StandardCharsets.UTF_8
            )
        ) {

            System.out.println("Connected successfully!");

            
            output.println("CONNECT," + branchName);

            
            ServerListener listener = new ServerListener(input);

            Thread listenerThread = new Thread(listener, "ServerListener");
            listenerThread.setDaemon(true);
            listenerThread.start();

            System.out.println("Type GET_STOCK to view inventory.");
            System.out.println("Type EXIT to disconnect.");

            
BranchManager branchManager = new BranchManager(
    branchName,
    output,
    scanner
);

branchManager.showMenu();

System.out.println("Disconnecting from server...");


        } catch (IOException e) {
            System.out.println("Connection error: " + e.getMessage());
            System.out.println("Make sure the server is running.");
        }

        System.out.println("Client closed.");
    }
}


package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class BranchClient {
    private static String serverHost = "127.0.0.1";
    private static int serverPort = 5000;
    
    public static void main(String[] args) {
        if (args.length > 0) {
    serverHost = args[0];
}

if (args.length > 1) {
    try {
        serverPort = Integer.parseInt(args[1]);

        if (serverPort < 1 || serverPort > 65535) {
            System.out.println("Invalid port number. Use 1-65535.");
            return;
        }
    } catch (NumberFormatException e) {
        System.out.println("Invalid port number. Enter a valid integer.");
        return;
    }
}

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
            
            Socket socket = new Socket(serverHost, serverPort);

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

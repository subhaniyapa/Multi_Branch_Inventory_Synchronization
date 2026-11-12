# Multi-Branch Inventory Synchronization

## Project Overview

This project is a Java network programming application for managing inventory across multiple branches. It enables clients connected to a central server to check and update branch stock, while keeping inventory data consistent when multiple clients make requests at the same time.

The system models branches as data managed by the application. The branches are not divided among group members: the same inventory management logic is used for every branch.

## Main Goals

- Manage product quantities separately for each branch.
- Allow clients to check and update inventory through the network.
- Support stock transfers between branches.
- Prevent invalid stock changes, such as reducing stock below zero.
- Keep inventory consistent when concurrent client requests affect the same stock.
- Persist inventory data using the Java-based storage approach selected by the group.
- Send real-time inventory updates to connected clients.

## Planned System Flow

1. A client sends an inventory request to the server over a socket connection.
2. The server handles the client connection and passes the request to the inventory logic.
3. The inventory logic validates and applies the requested operation safely.
4. The updated data is saved, and the server returns the operation result.
5. Connected clients are notified of relevant inventory changes.

## Group Members – Work Division

| Member | Responsibility |
|---|---|
| Member 1 | Java server development, socket programming, thread pool management, and concurrent client handling |
| Member 2 | Inventory management logic, thread synchronization, database management, and transaction handling |
| Member 3 | Branch management, real-time inventory updates, testing, and system integration |

### Responsibility Boundaries

- **Member 1** manages network communication and client connections. The server passes valid client requests to the inventory management component.
- **Member 2** implements the stock rules shared by all branches, including stock checks, additions, removals, transfers, concurrency protection, and persistent data operations.
- **Member 3** manages branch-related operations and coordinates how inventory changes are reflected to connected clients, then integrates and tests the complete system.

All members should agree on the request/response format and shared data model before connecting their components. Branches are represented as records in the system; each branch uses the same inventory logic.

## Concurrent Inventory and Transfers

Inventory operations must be protected so simultaneous requests cannot produce incorrect quantities. A stock transfer should validate the source stock and update both the source and destination as one logical operation. If the source does not have enough stock, the transfer should fail without changing either branch's quantity.

## Multi-Computer Demonstration Plan

The application can be demonstrated using three computers on the same local network:

1. Run the Java server on one computer.
2. Connect client applications from the other two computers to the server's local IP address and configured port.
3. Update or transfer stock from one client and show the resulting quantity on the other client.
4. Demonstrate that simultaneous requests do not make the stock inconsistent.
5. Restart the server and show that saved inventory data can be loaded again, if persistence is included in the implementation.

## Technology

- Java
- Java socket programming
- Java concurrency utilities
- Persistent storage/database approach to be finalized by the group

## Project Status

The project topic and group responsibilities have been selected. Implementation details, including the shared request/response format and storage approach, should be agreed by the group and updated here as development progresses.

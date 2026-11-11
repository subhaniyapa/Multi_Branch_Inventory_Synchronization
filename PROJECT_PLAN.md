# Real-Time Multi-Branch Inventory Synchronization System

**Module:** EC5205 – Network Programming  
**Project type:** Java-based multithreaded client-server application  
**Proposed architecture:** Separate Java Swing branch clients + Core Java TCP server

---

## 1. Project Overview

The Real-Time Multi-Branch Inventory Synchronization System manages inventory across multiple branches of one organization.

For the initial implementation, the system will support three branches:

- Colombo
- Galle
- Matara

Each branch runs a separate Java client application. All clients connect to a central Java server using TCP socket communication.

When a branch performs an inventory operation—such as a sale, stock-in, or stock transfer—the server validates and processes the request, updates the shared inventory data, and notifies the other connected branches.

The main technical focus is **Java network programming, multithreading, synchronization, and reliable inventory updates**.

## 2. Main Objectives

1. Connect multiple branch clients to one central server.
2. Process requests from multiple clients concurrently.
3. Maintain accurate branch-wise inventory quantities.
4. Prevent race conditions when multiple clients update the same stock.
5. Broadcast inventory changes to connected branches in real time.
6. Record inventory operations and their results.
7. Demonstrate Java networking and multithreading clearly during evaluation.

## 3. Recommended Technology Stack

| Component | Proposed technology |
|---|---|
| Programming language | Java |
| Client user interface | Java Swing |
| Server | Core Java |
| Network communication | TCP sockets (`ServerSocket`, `Socket`) |
| Client request handling | `ExecutorService` / thread pool |
| Synchronization | `synchronized` and/or `ReentrantLock` |
| Database connectivity | JDBC |
| Database | MySQL, subject to lecturer approval |
| Build tool | Maven |
| IDE | Eclipse or IntelliJ IDEA |

**Java-only requirement:** The application logic, server, client interface, networking, and concurrency implementation will be written in Java. MySQL is a database rather than a programming language, but confirm that an external database is permitted. If not, use Java file I/O as the initial storage option.

## 4. System Architecture

```text
       JAVA SWING BRANCH CLIENTS
  +------------+ +------------+ +------------+
  |  Colombo   | |   Galle    | |   Matara   |
  |   Client   | |   Client   | |   Client   |
  +------+-----+ +------+-----+ +------+-----+
         |              |              |
         +--------------+--------------+
                        |
                 TCP SOCKET CONNECTIONS
                        |
              +---------v----------+
              |  CORE JAVA SERVER  |
              |                    |
              |   ServerSocket     |
              |   ClientManager    |
              |   ExecutorService  |
              +---------+----------+
                        |
              +---------v----------+
              |  INVENTORY SERVICE |
              |                    |
              | Validation         |
              | Stock operations   |
              | Synchronization    |
              | Notification       |
              +---------+----------+
                        |
              +---------v----------+
              |    DATA STORAGE    |
              |   MySQL + JDBC     |
              +--------------------+
```

### Main components

**Branch Client (Java Swing)**
- Login and branch identification.
- Display branch-wise product quantities.
- Submit sales and stock-in requests.
- Display server responses and real-time notifications.
- Show connection status.

**Central Java Server**
- Accept incoming TCP connections using `ServerSocket`.
- Assign each connected client a handler task.
- Use a bounded `ExecutorService` thread pool to process clients concurrently.
- Validate requests and call inventory services.
- Send responses to the requesting client.
- Broadcast stock changes to other connected clients.

**Inventory Service**
- Check stock availability.
- Process sales and stock-in operations.
- Handle branch-to-branch transfers.
- Protect shared inventory updates against race conditions.
- Coordinate database transactions.

**Data Storage**
- Store branches, products, inventory quantities, users, and transaction history.
- Use JDBC for Java-to-database communication if MySQL is approved.

## 5. How Multithreading Is Used

Multithreading is a central part of this project, not an extra feature.

| Java concept | Planned use |
|---|---|
| Main server thread | Accept new client connections |
| `ExecutorService` | Manage a bounded pool of worker threads |
| `Runnable` / tasks | Represent client-handling work |
| Client handler | Read and process requests from a connected branch |
| Client receiver thread | Receive server messages and real-time updates |
| `synchronized` / `ReentrantLock` | Protect shared inventory operations |
| `ScheduledExecutorService` | Run periodic monitoring tasks |
| Thread-safe client registry | Safely manage connected clients |

### Example: Concurrent client requests

Colombo, Galle, and Matara may send requests at nearly the same time. The server accepts their connections and submits client-handling tasks to its worker pool. Multiple worker threads can process independent requests concurrently.

A thread pool is preferable to creating an unlimited new thread for every request because it limits resource usage and provides more predictable server behaviour.

### Example: Race condition

Assume a product has 10 units in stock.

- Thread A attempts to sell 7 units.
- Thread B attempts to sell 6 units.
- Both requests arrive at nearly the same time.

Without safe concurrency control, both operations could read the old quantity before either writes its update. This can cause an incorrect result.

The system must ensure that the stock check and stock deduction happen as one protected operation. Only one sale can proceed at a time for that inventory record. In this example, one sale succeeds and the other is rejected because the remaining stock is insufficient.

**Important:** Java locks protect shared data inside one server process. If a database is used, database transactions and suitable row-level locking or atomic conditional updates must also protect the stored quantity. A Java `synchronized` method alone does not guarantee database consistency.

## 6. Real-Time Synchronization Workflow

Example: Colombo sells two laptops.

1. The Colombo client sends a structured `SALE` request to the server over TCP.
2. A worker thread receives the request.
3. The server validates the branch, product, and requested quantity.
4. The inventory service safely checks and updates the stock.
5. The server commits the database transaction, if a database is used.
6. The server sends the result to Colombo.
7. The server broadcasts the updated inventory information to the other connected branches.
8. Galle and Matara clients update their displays without requiring a manual refresh.

A real-time notification should be sent only after the inventory update has succeeded.

### Example request

```json
{
  "type": "SALE",
  "branchId": "COLOMBO",
  "productId": 101,
  "quantity": 2
}
```

### Example notification

```json
{
  "type": "INVENTORY_UPDATED",
  "branchId": "COLOMBO",
  "productId": 101,
  "newQuantity": 8
}
```

These are illustrative message structures. The implementation can use a defined JSON library or a simple documented Java message format. TCP is a byte stream, so the application must define message framing—for example, one newline-terminated JSON message per line or a length-prefixed message.

## 7. Functional Requirements

### Essential features

1. Connect and identify branch clients.
2. View products and branch-wise inventory.
3. Add or register products.
4. Record stock-in operations.
5. Process product sales.
6. Validate stock availability.
7. Synchronize inventory updates across branches.
8. Handle simultaneous client requests safely.
9. Display transaction results.
10. Maintain transaction history.

### Additional features (after the core system works)

- Transfer stock between branches.
- Low-stock notifications.
- Branch online/offline status.
- Client reconnection and state refresh.
- Admin dashboard.
- Concurrent client load testing.

## 8. Proposed Java Project Structure

```text
InventorySync/
|
+-- pom.xml
|
+-- src/
    +-- main/
    |   +-- java/
    |       +-- com/inventorysync/
    |           +-- server/
    |           |   +-- InventoryServer.java
    |           |   +-- ClientHandler.java
    |           |   +-- ClientManager.java
    |           |
    |           +-- client/
    |           |   +-- InventoryClient.java
    |           |   +-- ClientReceiver.java
    |           |
    |           +-- service/
    |           |   +-- InventoryService.java
    |           |   +-- NotificationService.java
    |           |
    |           +-- model/
    |           |   +-- Product.java
    |           |   +-- Branch.java
    |           |   +-- InventoryTransaction.java
    |           |
    |           +-- database/
    |           |   +-- DatabaseConnection.java
    |           |   +-- InventoryRepository.java
    |           |
    |           +-- ui/
    |               +-- BranchDashboard.java
    |               +-- LoginFrame.java
    |
    +-- test/
        +-- java/
            +-- InventoryConcurrencyTest.java
```

Start with the server and a console-based client. Add the GUI after the network communication works. This keeps early debugging manageable.

## 9. Development Phases

### Phase 1 – Basic TCP communication
- Create the Maven Java project.
- Implement `InventoryServer` using `ServerSocket`.
- Implement a basic client using `Socket`.
- Connect three clients to one server.
- Exchange messages and verify connections.

### Phase 2 – Multithreading
- Add an `ExecutorService` worker pool.
- Create a client handler task.
- Display thread names in server logs.
- Test simultaneous requests from multiple clients.
- Handle client disconnects and resource cleanup.

### Phase 3 – Inventory logic
- Create branch, product, and inventory models.
- Implement stock-in, sale, and stock-view operations.
- Validate quantities and reject invalid operations.

### Phase 4 – Synchronization and consistency
- Reproduce a concurrent stock-update scenario.
- Add appropriate locks or synchronized critical sections.
- Ensure database updates are transactional if a database is used.
- Test that stock never becomes negative.

### Phase 5 – Data persistence
- Confirm the database choice with the lecturer.
- Create tables for branches, products, inventory, and transactions.
- Connect through JDBC.
- Persist and retrieve inventory data.

### Phase 6 – Real-time updates
- Maintain a thread-safe registry of connected clients.
- Broadcast successful inventory changes.
- Add a receiver task to each client.
- Update the user interface safely on the Swing Event Dispatch Thread.

### Phase 7 – Java Swing interface
- Build login and branch dashboard screens.
- Add product and inventory tables.
- Add stock-in and sales forms.
- Show connection status and notifications.

### Phase 8 – Testing and final preparation
- Run three or more branch clients simultaneously.
- Test concurrent sales against limited stock.
- Test disconnect and reconnect behaviour.
- Test invalid requests and insufficient stock.
- Prepare architecture diagrams, screenshots, test results, and the project report.

## 10. Suggested Three-Member Work Division

| Member | Main responsibility |
|---|---|
| Member 1 | TCP server, sockets, client connections, thread pool |
| Member 2 | Inventory logic, synchronization, database and transactions |
| Member 3 | Java Swing client interface, client-side communication and testing |

All members should understand the overall architecture, especially how threads, sockets, and synchronization work.

## 11. Important Demonstration Scenarios

The final demonstration should prove that the system works under concurrent network activity.

**Scenario 1 – Multiple connections**
- Start the central server.
- Connect Colombo, Galle, and Matara clients.
- Show that all clients are connected.

**Scenario 2 – Real-time update**
- Sell two laptops from Colombo.
- Show the server processing the request.
- Show the other branch clients receiving the update automatically.

**Scenario 3 – Concurrent stock access**
- Set a product's stock to 10.
- Submit two simultaneous sales: 7 and 6 units.
- Show that only one sale succeeds.
- Verify the final quantity is 3 or 4, depending on which sale succeeds.

**Scenario 4 – Concurrent load test**
- Send many requests from multiple clients.
- Log worker thread names.
- Verify the final inventory against the successful transactions.

**Scenario 5 – Disconnection**
- Disconnect a branch client.
- Show that the server detects the disconnection.
- Reconnect the client and retrieve the latest inventory state.

## 12. Important Design Decisions to Confirm

Before finalising the implementation, confirm these points with the lecturer:

1. Is MySQL or another external database permitted, or must all data storage also be implemented using Java file handling?
2. Are Java libraries such as JDBC drivers and JSON libraries permitted?
3. Is Java Swing acceptable for the graphical interface?
4. Is the application expected to run on multiple computers over a LAN, or are multiple clients on one computer sufficient for the demonstration?

The safest initial implementation is Core Java TCP sockets, Java thread pools, Java Swing, and file-based persistence. MySQL can be introduced once database use is confirmed.

## 13. Final Project Goal

The final system should demonstrate more than ordinary inventory management. It should clearly prove:

- Multiple Java clients communicating with one server.
- Concurrent request processing using Java threads.
- Thread-safe inventory updates.
- Prevention of race conditions.
- Real-time communication between branches.
- Reliable data persistence and recovery.

**Proposed final title:**

*Real-Time Multi-Branch Inventory Synchronization System Using Java Multithreading and TCP Socket Programming*

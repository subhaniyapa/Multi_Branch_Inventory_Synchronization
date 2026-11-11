# Real-Time Multi-Branch Inventory Synchronization System

A Java-based multithreaded client-server application designed to manage and synchronize inventory across multiple branches of an organization in real time.

## Project Overview

The Real-Time Multi-Branch Inventory Synchronization System enables multiple branches to manage their inventory through a centralized Java server.

Each branch operates as a separate client application connected to the central server using TCP socket communication. When a branch performs an inventory operation, such as a sale or stock-in, the server processes the request and synchronizes the updated inventory information with other connected branches.

The system focuses on Java Network Programming, Multithreading, Synchronization, and Real-Time Data Communication.

## Main Objectives

- Connect multiple branch clients to a central server.
- Process multiple client requests concurrently.
- Maintain accurate branch-wise inventory quantities.
- Prevent race conditions during simultaneous stock updates.
- Synchronize inventory changes across branches in real time.
- Maintain inventory transaction records.

## Supported Branches

- Colombo
- Galle
- Matara

## Technologies Used

| Component | Technology |
|---|---|
| Programming Language | Java |
| Client Interface | Java Swing |
| Server | Core Java |
| Network Communication | TCP Socket Programming |
| Multithreading | ExecutorService |
| Synchronization | synchronized / ReentrantLock |
| Database Connectivity | JDBC |
| Database | MySQL (subject to lecturer approval) |
| Build Tool | Maven |

## Main Features

- Multiple Branch Connections
- Branch-wise Inventory Management
- Product Management
- Stock-in Operations
- Sales Management
- Real-Time Inventory Synchronization
- Concurrent Request Processing
- Thread-Safe Stock Updates
- Transaction History

## System Architecture

The system follows a centralized Client-Server architecture.

- **Branch Clients:** Java Swing applications representing individual branches.
- **Central Server:** Handles TCP connections and concurrent client requests.
- **Inventory Service:** Processes inventory operations and manages synchronization.
- **Data Storage:** Stores inventory, branch, product, and transaction information.

## Project Structure

The project is organized into separate modules:

- `server` – Server-side networking and client management.
- `client` – Client-side network communication.
- `service` – Inventory and notification services.
- `model` – Data models.
- `database` – Database connectivity and repositories.
- `ui` – Java Swing user interfaces.

## Project Development Phases

1. Basic TCP Communication
2. Multithreading Implementation
3. Inventory Management Logic
4. Synchronization and Data Consistency
5. Data Persistence
6. Real-Time Updates
7. Java Swing Interface Development
8. Testing and Final Preparation

## Academic Information

- **Module:** EC5205 – Network Programming
- **Project Type:** Java-based Multithreaded Client-Server Application
- **Project Title:** Real-Time Multi-Branch Inventory Synchronization System

---

**Note:** This project is developed for academic purposes as part of the EC5205 Network Programming module.
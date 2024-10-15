# Java_Sockets_RMI_Cryptography_Inventory_System

This project is a secure distributed system for managing stock inventory using Java, RMI (Remote Method Invocation), and socket communication. The system allows multiple clients to manage inventory, receive real-time notifications, and ensures secure communication using 256-bit public-key cryptography.

## Key Features:
- **Inventory Management**: The server manages a list of products and their quantities. Clients can request stock information, update product quantities, and view the current inventory.
- **Real-time Notifications**: Clients can subscribe to notifications and receive real-time updates about stock changes via socket communication.
- **Secure Communication**: The system uses 256-bit public-key cryptography to ensure secure data transmission between clients and the server.
- **Multi-client Support**: Multiple clients can connect to the server simultaneously via RMI or socket, and the server handles concurrent connections.

## Security: 256-bit Public-Key Cryptography

This system implements **256-bit Public-Key Cryptography** for secure communication. Here's how the security mechanism works:

1. The server generates a pair of cryptographic keys: a **256-bit public key** and a **private key**.
2. The **public key** is distributed to clients. They use this key to encrypt any sensitive information before sending it to the server (e.g., stock updates).
3. The server holds the **private key**, which is used to decrypt the incoming encrypted data from clients.

### Benefits of 256-bit Public-Key Cryptography:
- **Strong Security**: 256-bit encryption provides a high level of security, making it extremely difficult for unauthorized parties to decrypt the data.
- **Confidentiality**: The encryption ensures that only the server, which holds the private key, can read the messages from clients.
- **Authentication**: The server can verify the authenticity of clients and their requests.

## Project Structure:
- `ClienteRMI.java`: A client that connects to the server via RMI to request stock data or update product quantities.
- `ClienteSocket.java`: A client that uses sockets to receive real-time notifications about inventory changes.
- `Inventario.java`: Manages the inventory of products and handles file persistence (`inventario.dat`).
- `Produto.java`: Represents a product with attributes like name, quantity, and product ID.
- `Servidor.java`: The main server application that listens for client connections via both RMI and socket communication.
- `SecureDirectNotification.java`: Interface for handling real-time notifications.
- `SecureDirectNotificationImpl.java`: Implementation of the notification interface for secure communication.
- `StockServer.java`: RMI interface for clients to interact with the inventory management system.

## How to Run:

1. Clone the repository:
   ```bash
   git clone https://github.com/pedromigueldmn/Java_Sockets_RMI_Cryptography_Inventory_System.git

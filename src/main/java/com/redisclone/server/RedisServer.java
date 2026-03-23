package com.redisclone.server;

import com.redisclone.command.CommandProcessor;
import com.redisclone.core.RedisStore;
import com.redisclone.persistence.PersistenceManager;
import com.redisclone.protocol.RespParser;
import com.redisclone.protocol.RespWriter;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
public class RedisServer {
    private final int port;
    private final RedisStore store;
    private final CommandProcessor commandProcessor;
    private final PersistenceManager persistenceManager;
    private ServerSocket serverSocket;
    private ExecutorService clientExecutor;
    private ScheduledExecutorService cleanupExecutor;
    private ScheduledExecutorService persistenceExecutor;
    private volatile boolean running = false;
    
    public RedisServer(int port) {
        this.port = port;
        this.store = RedisStore.getInstance();
        this.commandProcessor = new CommandProcessor(store);
        this.persistenceManager = new PersistenceManager("./data");
        
        try {
            persistenceManager.load(store);
            System.out.println("Loaded existing data from disk");
        } catch (IOException e) {
            System.out.println("No existing data found or error loading: " + e.getMessage());
        }
    }
    
    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        clientExecutor = Executors.newCachedThreadPool();
        cleanupExecutor = Executors.newScheduledThreadPool(1);
        persistenceExecutor = Executors.newScheduledThreadPool(1);
        
        cleanupExecutor.scheduleAtFixedRate(store::cleanupExpired, 10, 10, TimeUnit.SECONDS);
        
        persistenceExecutor.scheduleAtFixedRate(() -> {
            try {
                persistenceManager.save(store);
                System.out.println("Data saved to disk");
            } catch (IOException e) {
                System.err.println("Error saving data: " + e.getMessage());
            }
        }, 60, 60, TimeUnit.SECONDS);
        
        running = true;
        System.out.println("Redis server started on port " + port);
        
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                clientExecutor.submit(new ClientHandler(clientSocket));
            } catch (IOException e) {
                if (running) {
                    System.err.println("Error accepting client connection: " + e.getMessage());
                }
            }
        }
    }
    
    public void stop() {
        running = false;
        
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing server socket: " + e.getMessage());
            }
        }
        
        if (clientExecutor != null) {
            clientExecutor.shutdown();
            try {
                if (!clientExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    clientExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                clientExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        if (cleanupExecutor != null) {
            cleanupExecutor.shutdown();
            try {
                if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    cleanupExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                cleanupExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        if (persistenceExecutor != null) {
            persistenceExecutor.shutdown();
            try {
                if (!persistenceExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    persistenceExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                persistenceExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        try {
            persistenceManager.save(store);
            System.out.println("Final data save completed");
        } catch (IOException e) {
            System.err.println("Error saving data on shutdown: " + e.getMessage());
        }
        
        System.out.println("Redis server stopped");
    }
    
    private class ClientHandler implements Runnable {
        private final Socket clientSocket;
        
        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }
        
        @Override
        public void run() {
            try (InputStream inputStream = clientSocket.getInputStream();
                 OutputStream outputStream = clientSocket.getOutputStream()) {
                
                RespParser parser = new RespParser(inputStream);
                RespWriter writer = new RespWriter(outputStream);
                
                System.out.println("Client connected: " + clientSocket.getRemoteSocketAddress());
                
                while (!clientSocket.isClosed()) {
                    try {
                        RespParser.RespValue command = parser.parse();
                        commandProcessor.processCommand(command, writer);
                        writer.flush();
                    } catch (EOFException e) {
                        break;
                    } catch (IOException e) {
                        System.err.println("Error processing command: " + e.getMessage());
                        writer.writeError("ERR " + e.getMessage());
                        writer.flush();
                    }
                }
                
            } catch (IOException e) {
                System.err.println("Error handling client: " + e.getMessage());
            } finally {
                try {
                    clientSocket.close();
                    System.out.println("Client disconnected: " + clientSocket.getRemoteSocketAddress());
                } catch (IOException e) {
                    System.err.println("Error closing client socket: " + e.getMessage());
                }
            }
        }
    }
    
    public static void main(String[] args) {
        int port = 6379; // Default Redis port
        
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--port") && i + 1 < args.length) {
                try {
                    port = Integer.parseInt(args[i + 1]);
                } catch (NumberFormatException e) {
                    System.err.println("Invalid port number: " + args[i + 1]);
                    System.exit(1);
                }
                i++;
            } else if (args[i].equals("--help")) {
                System.out.println("Redis Clone Server");
                System.out.println("Usage: java RedisServer [--port <port>] [--help]");
                System.out.println("  --port <port>  Port to listen on (default: 6379)");
                System.out.println("  --help         Show this help message");
                System.exit(0);
            }
        }
        
        RedisServer server = new RedisServer(port);
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutting down server...");
            server.stop();
        }));
        
        try {
            server.start();
        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
            System.exit(1);
        }
    }
}

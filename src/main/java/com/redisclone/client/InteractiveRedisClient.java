package com.redisclone.client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

/**
 * Interactive Redis client for testing custom commands
 */
public class InteractiveRedisClient {
    
    public static void main(String[] args) {
        String host = "localhost";
        int port = 6379;
        
        if (args.length >= 1) {
            host = args[0];
        }
        if (args.length >= 2) {
            port = Integer.parseInt(args[1]);
        }
        
        System.out.println("Interactive Redis Client");
        System.out.println("Connecting to " + host + ":" + port + "...");
        

        // Socket : connects to the redis server
        //out: sends data to the redis server
        //in : receives data from the redis server
        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             Scanner scanner = new Scanner(System.in)) {
            
            System.out.println("Connected successfully!");
            System.out.println("\nCommands:");
            System.out.println("  SET key value [EX seconds]");
            System.out.println("  GET key");
            System.out.println("  HSET key field value");
            System.out.println("  HGET key field");
            System.out.println("  HGETALL key");
            System.out.println("  LPUSH key value1 [value2 ...]");
            System.out.println("  RPUSH key value1 [value2 ...]");
            System.out.println("  LPOP key");
            System.out.println("  RPOP key");
            System.out.println("  LRANGE key start stop");
            System.out.println("  SADD key member1 [member2 ...]");
            System.out.println("  SMEMBERS key");
            System.out.println("  SISMEMBER key member");
            System.out.println("  ZADD key score member");
            System.out.println("  ZSCORE key member");
            System.out.println("  ZRANGE key start stop");
            System.out.println("  KEYS pattern");
            System.out.println("  EXISTS key");
            System.out.println("  DEL key");
            System.out.println("  EXPIRE key seconds");
            System.out.println("  TTL key");
            System.out.println("  PING [message]");
            System.out.println("  INFO");
            System.out.println("  FLUSHALL");
            System.out.println("  quit - Exit the client");
            System.out.println("\nEnter commands (or 'quit' to exit):");
            
            while (true) {
                System.out.print("redis> ");
                String input = scanner.nextLine().trim();
                
                if (input.isEmpty()) {
                    continue;
                }
                
                if (input.equalsIgnoreCase("quit") || input.equalsIgnoreCase("exit")) {
                    System.out.println("Goodbye!");
                    break;
                }
                
                try {
                    String respCommand = convertToResp(input);
                    sendCommand(out, in, respCommand);
                } catch (Exception e) {
                    System.err.println("Error: " + e.getMessage());
                }
            }
            
        } catch (IOException e) {
            System.err.println("Error connecting to Redis server: " + e.getMessage());
            System.err.println("Make sure the Redis clone server is running on " + host + ":" + port);
        }
    }
    

    // RESP : redis serialization protocol , converts the command to the resp format.
    // resp is a text-based protocol which converts string ccmmands into a resp format which is understood by redis.
    private static String convertToResp(String command) {


        // Simple command parser that handles quoted strings
        java.util.List<String> parts = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        char quoteChar = '"';
        
        for (int i = 0; i < command.length(); i++) {
            char c = command.charAt(i);
            
            if (!inQuotes && (c == '"' || c == '\'')) {
                inQuotes = true;
                quoteChar = c;
            } else if (inQuotes && c == quoteChar) {
                inQuotes = false;
            } else if (!inQuotes && Character.isWhitespace(c)) {
                if (current.length() > 0) {
                    parts.add(current.toString());
                    current = new StringBuilder();
                }
            } else {
                current.append(c);
            }
        }
        
        if (current.length() > 0) {
            parts.add(current.toString());
        }
        
        StringBuilder resp = new StringBuilder();
        
        // Start array
        resp.append("*").append(parts.size()).append("\r\n");
        
        for (String part : parts) {
            // Add bulk string
            resp.append("$").append(part.length()).append("\r\n");
            resp.append(part).append("\r\n");
        }
        
        return resp.toString();
    }


    // decodes the response received from the redis server , into executable string.
    
    private static void sendCommand(PrintWriter out, BufferedReader in, String command) {
        try {
            out.print(command);
            out.flush();
            
            String response = in.readLine();
            if (response != null) {
                // Handle bulk string responses
                if (response.startsWith("$") && !response.equals("$-1")) {
                    try {
                        int length = Integer.parseInt(response.substring(1));
                        if (length > 0) {
                            String value = in.readLine();
                            System.out.println(value);
                        } else {
                            System.out.println("(empty string)");
                        }
                    } catch (NumberFormatException e) {
                        System.out.println(response);
                    }
                }
                // Handle array responses
                else if (response.startsWith("*")) {
                    try {
                        int count = Integer.parseInt(response.substring(1));
                        if (count == -1) {
                            System.out.println("(nil)");
                        } else {
                            for (int i = 0; i < count; i++) {
                                String element = in.readLine();
                                if (element.startsWith("$") && !element.equals("$-1")) {
                                    try {
                                        int length = Integer.parseInt(element.substring(1));
                                        if (length > 0) {
                                            String value = in.readLine();
                                            System.out.println((i + 1) + ") \"" + value + "\"");
                                        } else {
                                            System.out.println((i + 1) + ") \"\"");
                                        }
                                    } catch (NumberFormatException e) {
                                        System.out.println((i + 1) + ") " + element);
                                    }
                                } else if (element.equals("$-1")) {
                                    System.out.println((i + 1) + ") (nil)");
                                } else {
                                    System.out.println((i + 1) + ") " + element);
                                }
                            }
                        }
                    } catch (NumberFormatException e) {
                        System.out.println(response);
                    }
                }
                // Handle simple string responses
                else if (response.startsWith("+")) {
                    System.out.println(response.substring(1));
                }
                // Handle integer responses
                else if (response.startsWith(":")) {
                    System.out.println("(integer) " + response.substring(1));
                }
                // Handle error responses
                else if (response.startsWith("-")) {
                    System.out.println("(error) " + response.substring(1));
                }
                else {
                    System.out.println(response);
                }
            }
            
        } catch (IOException e) {
            System.err.println("Error sending command: " + e.getMessage());
        }
    }
}

package com.redisclone.client;

import java.io.*;
import java.net.Socket;

public class RedisClientExample {
    
    public static void main(String[] args) {
        String host = "localhost";
        int port = 6379;
        
        if (args.length >= 1) {
            host = args[0];
        }
        if (args.length >= 2) {
            port = Integer.parseInt(args[1]);
        }
        
        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            
            System.out.println("Connected to Redis clone server at " + host + ":" + port);
            
            System.out.println("\n=== Testing PING ===");
            sendCommand(out, in, "*1\r\n$4\r\nPING\r\n");
        
            System.out.println("\n=== Testing String Operations ===");
            sendCommand(out, in, "*3\r\n$3\r\nSET\r\n$3\r\nkey\r\n$5\r\nvalue\r\n");
            sendCommand(out, in, "*2\r\n$3\r\nGET\r\n$3\r\nkey\r\n");
            sendCommand(out, in, "*2\r\n$6\r\nEXISTS\r\n$3\r\nkey\r\n");
            
            System.out.println("\n=== Testing Hash Operations ===");
            sendCommand(out, in, "*4\r\n$4\r\nHSET\r\n$4\r\nuser\r\n$4\r\nname\r\n$4\r\nJohn\r\n");
            sendCommand(out, in, "*4\r\n$4\r\nHSET\r\n$4\r\nuser\r\n$3\r\nage\r\n$2\r\n30\r\n");
            sendCommand(out, in, "*3\r\n$4\r\nHGET\r\n$4\r\nuser\r\n$4\r\nname\r\n");
            sendCommand(out, in, "*2\r\n$7\r\nHGETALL\r\n$4\r\nuser\r\n");
            
            System.out.println("\n=== Testing List Operations ===");
            sendCommand(out, in, "*3\r\n$5\r\nLPUSH\r\n$4\r\nlist\r\n$5\r\nworld\r\n");
            sendCommand(out, in, "*3\r\n$5\r\nLPUSH\r\n$4\r\nlist\r\n$5\r\nhello\r\n");
            sendCommand(out, in, "*4\r\n$6\r\nLRANGE\r\n$4\r\nlist\r\n$1\r\n0\r\n$2\r\n-1\r\n");
            sendCommand(out, in, "*2\r\n$4\r\nLPOP\r\n$4\r\nlist\r\n");
            
            System.out.println("\n=== Testing Set Operations ===");
            sendCommand(out, in, "*3\r\n$4\r\nSADD\r\n$4\r\nset\r\n$6\r\nmember1\r\n");
            sendCommand(out, in, "*3\r\n$4\r\nSADD\r\n$4\r\nset\r\n$6\r\nmember2\r\n");
            sendCommand(out, in, "*2\r\n$8\r\nSMEMBERS\r\n$4\r\nset\r\n");
            sendCommand(out, in, "*3\r\n$9\r\nSISMEMBER\r\n$4\r\nset\r\n$6\r\nmember1\r\n");
            
            System.out.println("\n=== Testing Sorted Set Operations ===");
            sendCommand(out, in, "*4\r\n$4\r\nZADD\r\n$6\r\nleader\r\n$1\r\n1\r\n$6\r\nplayer1\r\n");
            sendCommand(out, in, "*4\r\n$4\r\nZADD\r\n$6\r\nleader\r\n$1\r\n2\r\n$6\r\nplayer2\r\n");
            sendCommand(out, in, "*3\r\n$6\r\nZSCORE\r\n$6\r\nleader\r\n$6\r\nplayer1\r\n");
            sendCommand(out, in, "*4\r\n$6\r\nZRANGE\r\n$6\r\nleader\r\n$1\r\n0\r\n$2\r\n-1\r\n");
            
            System.out.println("\n=== Testing Expiration ===");
            sendCommand(out, in, "*5\r\n$3\r\nSET\r\n$4\r\ntemp\r\n$5\r\nvalue\r\n$2\r\nEX\r\n$1\r\n1\r\n");
            sendCommand(out, in, "*2\r\n$3\r\nTTL\r\n$4\r\ntemp\r\n");
            
            System.out.println("\n=== Testing Server Info ===");
            sendCommand(out, in, "*1\r\n$4\r\nINFO\r\n");
            
            System.out.println("\n=== All tests completed ===");
            
        } catch (IOException e) {
            System.err.println("Error connecting to Redis server: " + e.getMessage());
            System.err.println("Make sure the Redis clone server is running on " + host + ":" + port);
        }
    }
    
    private static void sendCommand(PrintWriter out, BufferedReader in, String command) {
        try {
            out.print(command);
            out.flush();
            
            String response = in.readLine();
            if (response != null) {
                System.out.println("Command: " + command.replaceAll("\r\n", "\\\\r\\\\n"));
                System.out.println("Response: " + response);
                
                if (response.startsWith("$") && !response.equals("$-1")) {
                    try {
                        int length = Integer.parseInt(response.substring(1));
                        if (length > 0) {
                            String value = in.readLine();
                            System.out.println("Value: " + value);
                        }
                    } catch (NumberFormatException e) {
                    }
                }
                
                if (response.startsWith("*")) {
                    try {
                        int count = Integer.parseInt(response.substring(1));
                        System.out.println("Array with " + count + " elements:");
                        for (int i = 0; i < count; i++) {
                            String element = in.readLine();
                            System.out.println("  [" + i + "]: " + element);
                            
                            if (element.startsWith("$") && !element.equals("$-1")) {
                                try {
                                    int length = Integer.parseInt(element.substring(1));
                                    if (length > 0) {
                                        String value = in.readLine();
                                        System.out.println("    Value: " + value);
                                    }
                                } catch (NumberFormatException e) {
                                }
                            }
                        }
                    } catch (NumberFormatException e) {
                        System.out.println(response);
                    }
                }
            }
            System.out.println();
            
        } catch (IOException e) {
            System.err.println("Error sending command: " + e.getMessage());
        }
    }
}

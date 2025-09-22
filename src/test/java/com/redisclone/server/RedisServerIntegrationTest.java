package com.redisclone.server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class RedisServerIntegrationTest {
    private RedisServer server;
    private CompletableFuture<Void> serverFuture;
    private static final int TEST_PORT = 6380;
    
    @BeforeEach
    void setUp() throws Exception {
        server = new RedisServer(TEST_PORT);
        serverFuture = CompletableFuture.runAsync(() -> {
            try {
                server.start();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        
        // Wait for server to start
        Thread.sleep(100);
    }
    
    @AfterEach
    void tearDown() throws Exception {
        server.stop();
        serverFuture.get(5, TimeUnit.SECONDS);
    }
    
    private String sendCommand(String command) throws IOException {
        try (Socket socket = new Socket("localhost", TEST_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            
            out.println(command);
            return in.readLine();
        }
    }
    
    @Test
    void testPingCommand() throws IOException {
        String response = sendCommand("*1\r\n$4\r\nPING\r\n");
        assertEquals("+PONG", response);
    }
    
    @Test
    void testSetAndGetCommand() throws IOException {
        // SET command
        String setResponse = sendCommand("*3\r\n$3\r\nSET\r\n$3\r\nkey\r\n$5\r\nvalue\r\n");
        assertEquals("+OK", setResponse);
        
        // GET command
        String getResponse = sendCommand("*2\r\n$3\r\nGET\r\n$3\r\nkey\r\n");
        assertEquals("$5", getResponse); // Bulk string length
        String value = sendCommand("value\r\n");
        assertEquals("value", value);
    }
    
    @Test
    void testSetWithExpiration() throws IOException {
        // SET with EX
        String setResponse = sendCommand("*5\r\n$3\r\nSET\r\n$3\r\nkey\r\n$5\r\nvalue\r\n$2\r\nEX\r\n$1\r\n1\r\n");
        assertEquals("+OK", setResponse);
        
        // GET immediately
        String getResponse = sendCommand("*2\r\n$3\r\nGET\r\n$3\r\nkey\r\n");
        assertEquals("$5", getResponse);
        String value = sendCommand("value\r\n");
        assertEquals("value", value);
        
        // Wait for expiration
        try {
            Thread.sleep(1100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Test interrupted");
        }
        
        // GET after expiration
        String expiredResponse = sendCommand("*2\r\n$3\r\nGET\r\n$3\r\nkey\r\n");
        assertEquals("$-1", expiredResponse); // NULL
    }
    
    @Test
    void testHashCommands() throws IOException {
        // HSET
        String hsetResponse = sendCommand("*4\r\n$4\r\nHSET\r\n$4\r\nhash\r\n$5\r\nfield\r\n$5\r\nvalue\r\n");
        assertEquals(":1", hsetResponse);
        
        // HGET
        String hgetResponse = sendCommand("*3\r\n$4\r\nHGET\r\n$4\r\nhash\r\n$5\r\nfield\r\n");
        assertEquals("$5", hgetResponse);
        String value = sendCommand("value\r\n");
        assertEquals("value", value);
        
        // HGETALL
        String hgetallResponse = sendCommand("*2\r\n$7\r\nHGETALL\r\n$4\r\nhash\r\n");
        assertEquals("*2", hgetallResponse); // Array of 2 elements
    }
    
    @Test
    void testListCommands() throws IOException {
        // LPUSH
        String lpushResponse = sendCommand("*3\r\n$5\r\nLPUSH\r\n$4\r\nlist\r\n$5\r\nvalue\r\n");
        assertEquals(":1", lpushResponse);
        
        // RPUSH
        String rpushResponse = sendCommand("*3\r\n$5\r\nRPUSH\r\n$4\r\nlist\r\n$5\r\nvalue\r\n");
        assertEquals(":2", rpushResponse);
        
        // LRANGE
        String lrangeResponse = sendCommand("*4\r\n$6\r\nLRANGE\r\n$4\r\nlist\r\n$1\r\n0\r\n$2\r\n-1\r\n");
        assertEquals("*2", lrangeResponse); // Array of 2 elements
    }
    
    @Test
    void testSetCommands() throws IOException {
        // SADD
        String saddResponse = sendCommand("*3\r\n$4\r\nSADD\r\n$4\r\nset\r\n$5\r\nvalue\r\n");
        assertEquals(":1", saddResponse);
        
        // SISMEMBER
        String sismemberResponse = sendCommand("*3\r\n$9\r\nSISMEMBER\r\n$4\r\nset\r\n$5\r\nvalue\r\n");
        assertEquals(":1", sismemberResponse);
        
        // SMEMBERS
        String smembersResponse = sendCommand("*2\r\n$8\r\nSMEMBERS\r\n$4\r\nset\r\n");
        assertEquals("*1", smembersResponse); // Array of 1 element
    }
    
    @Test
    void testSortedSetCommands() throws IOException {
        // ZADD
        String zaddResponse = sendCommand("*4\r\n$4\r\nZADD\r\n$4\r\nzset\r\n$1\r\n1\r\n$5\r\nvalue\r\n");
        assertEquals(":1", zaddResponse);
        
        // ZSCORE
        String zscoreResponse = sendCommand("*3\r\n$6\r\nZSCORE\r\n$4\r\nzset\r\n$5\r\nvalue\r\n");
        assertEquals("$1", zscoreResponse);
        String score = sendCommand("1\r\n");
        assertEquals("1", score);
        
        // ZRANGE
        String zrangeResponse = sendCommand("*4\r\n$6\r\nZRANGE\r\n$4\r\nzset\r\n$1\r\n0\r\n$2\r\n-1\r\n");
        assertEquals("*1", zrangeResponse); // Array of 1 element
    }
    
    @Test
    void testKeysCommand() throws IOException {
        // Set some keys
        sendCommand("*3\r\n$3\r\nSET\r\n$3\r\nkey1\r\n$5\r\nvalue1\r\n");
        sendCommand("*3\r\n$3\r\nSET\r\n$3\r\nkey2\r\n$5\r\nvalue2\r\n");
        
        // KEYS *
        String keysResponse = sendCommand("*2\r\n$4\r\nKEYS\r\n$1\r\n*\r\n");
        assertTrue(keysResponse.startsWith("*")); // Array response
    }
    
    @Test
    void testExistsCommand() throws IOException {
        // Set a key
        sendCommand("*3\r\n$3\r\nSET\r\n$3\r\nkey\r\n$5\r\nvalue\r\n");
        
        // EXISTS
        String existsResponse = sendCommand("*2\r\n$6\r\nEXISTS\r\n$3\r\nkey\r\n");
        assertEquals(":1", existsResponse);
        
        // EXISTS non-existent
        String notExistsResponse = sendCommand("*2\r\n$6\r\nEXISTS\r\n$6\r\nnokey\r\n");
        assertEquals(":0", notExistsResponse);
    }
    
    @Test
    void testDelCommand() throws IOException {
        // Set a key
        sendCommand("*3\r\n$3\r\nSET\r\n$3\r\nkey\r\n$5\r\nvalue\r\n");
        
        // DEL
        String delResponse = sendCommand("*2\r\n$3\r\nDEL\r\n$3\r\nkey\r\n");
        assertEquals(":1", delResponse);
        
        // Verify key is deleted
        String getResponse = sendCommand("*2\r\n$3\r\nGET\r\n$3\r\nkey\r\n");
        assertEquals("$-1", getResponse); // NULL
    }
    
    @Test
    void testUnknownCommand() throws IOException {
        String response = sendCommand("*1\r\n$7\r\nUNKNOWN\r\n");
        assertTrue(response.startsWith("-ERR"));
    }
    
    @Test
    void testWrongNumberOfArguments() throws IOException {
        String response = sendCommand("*1\r\n$3\r\nGET\r\n");
        assertTrue(response.startsWith("-ERR"));
    }
}

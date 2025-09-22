package com.redisclone.persistence;

import com.redisclone.core.RedisStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PersistenceManagerTest {
    private PersistenceManager persistenceManager;
    private RedisStore store;
    private String testDataDir = "./test_data";
    
    @BeforeEach
    void setUp() throws IOException {
        // Clean up any existing test data
        tearDown();
        
        persistenceManager = new PersistenceManager(testDataDir);
        store = new RedisStore();
    }
    
    @AfterEach
    void tearDown() throws IOException {
        if (persistenceManager != null) {
            persistenceManager.deleteDataFile();
        }
        
        // Clean up test directory
        Path testDir = Paths.get(testDataDir);
        if (Files.exists(testDir)) {
            Files.walk(testDir)
                    .sorted((a, b) -> b.compareTo(a)) // Reverse order for deletion
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            // Ignore cleanup errors
                        }
                    });
        }
    }
    
    @Test
    void testSaveAndLoadEmptyStore() throws IOException {
        // Save empty store
        persistenceManager.save(store);
        assertTrue(persistenceManager.hasDataFile());
        
        // Load into new store
        RedisStore newStore = new RedisStore();
        persistenceManager.load(newStore);
        assertEquals(0, newStore.size());
    }
    
    @Test
    void testSaveAndLoadStringValues() throws IOException {
        // Add some string values
        store.set("key1", "value1");
        store.set("key2", "value2");
        store.set("key3", "value3", 10); // With expiration
        
        // Save
        persistenceManager.save(store);
        
        // Load into new store
        RedisStore newStore = new RedisStore();
        persistenceManager.load(newStore);
        
        // Verify data
        assertEquals(3, newStore.size());
        assertEquals("value1", newStore.get("key1"));
        assertEquals("value2", newStore.get("key2"));
        assertEquals("value3", newStore.get("key3"));
        
        // Verify expiration
        assertTrue(newStore.ttl("key3") > 0);
        assertEquals(-1, newStore.ttl("key1")); // No expiration
    }
    
    @Test
    void testSaveAndLoadHashValues() throws IOException {
        // Add hash values
        store.hset("hash1", "field1", "value1");
        store.hset("hash1", "field2", "value2");
        store.hset("hash2", "field1", "value3");
        
        // Save
        persistenceManager.save(store);
        
        // Load into new store
        RedisStore newStore = new RedisStore();
        persistenceManager.load(newStore);
        
        // Verify data
        assertEquals("value1", newStore.hget("hash1", "field1"));
        assertEquals("value2", newStore.hget("hash1", "field2"));
        assertEquals("value3", newStore.hget("hash2", "field1"));
        
        // Verify hash structure
        var hash1 = newStore.hgetall("hash1");
        assertEquals(2, hash1.size());
        assertTrue(hash1.containsKey("field1"));
        assertTrue(hash1.containsKey("field2"));
    }
    
    @Test
    void testSaveAndLoadListValues() throws IOException {
        // Add list values
        store.lpush("list1", "item1", "item2");
        store.rpush("list1", "item3");
        store.lpush("list2", "item4");
        
        // Save
        persistenceManager.save(store);
        
        // Load into new store
        RedisStore newStore = new RedisStore();
        persistenceManager.load(newStore);
        
        // Verify data
        var list1 = newStore.lrange("list1", 0, -1);
        assertEquals(3, list1.size());
        assertEquals("item2", list1.get(0)); // LPUSH adds to front
        assertEquals("item1", list1.get(1));
        assertEquals("item3", list1.get(2)); // RPUSH adds to back
        
        var list2 = newStore.lrange("list2", 0, -1);
        assertEquals(1, list2.size());
        assertEquals("item4", list2.get(0));
    }
    
    @Test
    void testSaveAndLoadSetValues() throws IOException {
        // Add set values
        store.sadd("set1", "member1", "member2", "member3");
        store.sadd("set2", "member4");
        
        // Save
        persistenceManager.save(store);
        
        // Load into new store
        RedisStore newStore = new RedisStore();
        persistenceManager.load(newStore);
        
        // Verify data
        var set1 = newStore.smembers("set1");
        assertEquals(3, set1.size());
        assertTrue(set1.contains("member1"));
        assertTrue(set1.contains("member2"));
        assertTrue(set1.contains("member3"));
        
        var set2 = newStore.smembers("set2");
        assertEquals(1, set2.size());
        assertTrue(set2.contains("member4"));
    }
    
    @Test
    void testSaveAndLoadSortedSetValues() throws IOException {
        // Add sorted set values
        store.zadd("zset1", 1.0, "member1");
        store.zadd("zset1", 2.0, "member2");
        store.zadd("zset1", 1.5, "member3");
        store.zadd("zset2", 3.0, "member4");
        
        // Save
        persistenceManager.save(store);
        
        // Load into new store
        RedisStore newStore = new RedisStore();
        persistenceManager.load(newStore);
        
        // Verify data
        assertEquals(1.0, newStore.zscore("zset1", "member1"));
        assertEquals(2.0, newStore.zscore("zset1", "member2"));
        assertEquals(1.5, newStore.zscore("zset1", "member3"));
        assertEquals(3.0, newStore.zscore("zset2", "member4"));
        
        // Verify ordering
        var zset1 = newStore.zrange("zset1", 0, -1);
        assertEquals(3, zset1.size());
        assertEquals("member1", zset1.get(0)); // Lowest score first
        assertEquals("member3", zset1.get(1));
        assertEquals("member2", zset1.get(2)); // Highest score last
    }
    
    @Test
    void testSaveAndLoadMixedDataTypes() throws IOException {
        // Add mixed data types
        store.set("string", "value");
        store.hset("hash", "field", "value");
        store.lpush("list", "item");
        store.sadd("set", "member");
        store.zadd("zset", 1.0, "member");
        
        // Save
        persistenceManager.save(store);
        
        // Load into new store
        RedisStore newStore = new RedisStore();
        persistenceManager.load(newStore);
        
        // Verify all data types
        assertEquals("value", newStore.get("string"));
        assertEquals("value", newStore.hget("hash", "field"));
        assertEquals("item", newStore.lrange("list", 0, -1).get(0));
        assertTrue(newStore.smembers("set").contains("member"));
        assertEquals(1.0, newStore.zscore("zset", "member"));
    }
    
    @Test
    void testLoadNonExistentFile() throws IOException {
        // Try to load from non-existent file
        RedisStore newStore = new RedisStore();
        persistenceManager.load(newStore); // Should not throw exception
        assertEquals(0, newStore.size());
    }
    
    @Test
    void testDataFileSize() throws IOException {
        // Initially no file
        assertEquals(0, persistenceManager.getDataFileSize());
        
        // Add some data
        store.set("key1", "value1");
        store.set("key2", "value2");
        
        // Save and check size
        persistenceManager.save(store);
        assertTrue(persistenceManager.getDataFileSize() > 0);
    }
    
    @Test
    void testExpiredKeysNotSaved() throws IOException {
        // Add key with very short expiration
        store.set("expired", "value", 1);
        
        // Wait for expiration
        try {
            Thread.sleep(1100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Save - expired key should not be saved
        persistenceManager.save(store);
        
        // Load into new store
        RedisStore newStore = new RedisStore();
        persistenceManager.load(newStore);
        
        // Expired key should not be loaded
        assertEquals(0, newStore.size());
        assertNull(newStore.get("expired"));
    }
}

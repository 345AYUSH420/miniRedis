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
        tearDown();
        
        persistenceManager = new PersistenceManager(testDataDir);
        store = new RedisStore();
    }
    
    @AfterEach
    void tearDown() throws IOException {
        if (persistenceManager != null) {
            persistenceManager.deleteDataFile();
        }
        
        Path testDir = Paths.get(testDataDir);
        if (Files.exists(testDir)) {
            Files.walk(testDir)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                        }
                    });
        }
    }
    
    @Test
    void testSaveAndLoadEmptyStore() throws IOException {
        persistenceManager.save(store);
        assertTrue(persistenceManager.hasDataFile());
        
        RedisStore newStore = new RedisStore();
        persistenceManager.load(newStore);
        assertEquals(0, newStore.size());
    }
    
    @Test
    void testSaveAndLoadStringValues() throws IOException {
        store.set("key1", "value1");
        store.set("key2", "value2");
        store.set("key3", "value3", 10);
        
        persistenceManager.save(store);
        
        RedisStore newStore = new RedisStore();
        persistenceManager.load(newStore);
        
        assertEquals(3, newStore.size());
        assertEquals("value1", newStore.get("key1"));
        assertEquals("value2", newStore.get("key2"));
        assertEquals("value3", newStore.get("key3"));
        
        assertTrue(newStore.ttl("key3") > 0);
        assertEquals(-1, newStore.ttl("key1"));
    }
    
    @Test
    void testSaveAndLoadHashValues() throws IOException {
        store.hset("hash1", "field1", "value1");
        store.hset("hash1", "field2", "value2");
        store.hset("hash2", "field1", "value3");
        
        persistenceManager.save(store);
        
        RedisStore newStore = new RedisStore();
        persistenceManager.load(newStore);
        
        assertEquals("value1", newStore.hget("hash1", "field1"));
        assertEquals("value2", newStore.hget("hash1", "field2"));
        assertEquals("value3", newStore.hget("hash2", "field1"));
        
        var hash1 = newStore.hgetall("hash1");
        assertEquals(2, hash1.size());
        assertTrue(hash1.containsKey("field1"));
        assertTrue(hash1.containsKey("field2"));
    }
    
    @Test
    void testSaveAndLoadListValues() throws IOException {
        store.lpush("list1", "item1", "item2");
        store.rpush("list1", "item3");
        store.lpush("list2", "item4");
        
        persistenceManager.save(store);
        
        RedisStore newStore = new RedisStore();
        persistenceManager.load(newStore);
        
        var list1 = newStore.lrange("list1", 0, -1);
        assertEquals(3, list1.size());
        assertEquals("item2", list1.get(0));
        assertEquals("item1", list1.get(1));
        assertEquals("item3", list1.get(2));
        
        var list2 = newStore.lrange("list2", 0, -1);
        assertEquals(1, list2.size());
        assertEquals("item4", list2.get(0));
    }
    
    @Test
    void testSaveAndLoadSetValues() throws IOException {
        store.sadd("set1", "member1", "member2", "member3");
        store.sadd("set2", "member4");

        persistenceManager.save(store);
        
        RedisStore newStore = new RedisStore();
        persistenceManager.load(newStore);
        
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
        store.zadd("zset1", 1.0, "member1");
        store.zadd("zset1", 2.0, "member2");
        store.zadd("zset1", 1.5, "member3");
        store.zadd("zset2", 3.0, "member4");
        
        persistenceManager.save(store);
        
        RedisStore newStore = new RedisStore();
        persistenceManager.load(newStore);
        
        assertEquals(1.0, newStore.zscore("zset1", "member1"));
        assertEquals(2.0, newStore.zscore("zset1", "member2"));
        assertEquals(1.5, newStore.zscore("zset1", "member3"));
        assertEquals(3.0, newStore.zscore("zset2", "member4"));
        
        var zset1 = newStore.zrange("zset1", 0, -1);
        assertEquals(3, zset1.size());
        assertEquals("member1", zset1.get(0));
        assertEquals("member3", zset1.get(1));
        assertEquals("member2", zset1.get(2));
    }
    
    @Test
    void testSaveAndLoadMixedDataTypes() throws IOException {
        store.set("string", "value");
        store.hset("hash", "field", "value");
        store.lpush("list", "item");
        store.sadd("set", "member");
        store.zadd("zset", 1.0, "member");
        
        persistenceManager.save(store);
        
        RedisStore newStore = new RedisStore();
        persistenceManager.load(newStore);
        
        assertEquals("value", newStore.get("string"));
        assertEquals("value", newStore.hget("hash", "field"));
        assertEquals("item", newStore.lrange("list", 0, -1).get(0));
        assertTrue(newStore.smembers("set").contains("member"));
        assertEquals(1.0, newStore.zscore("zset", "member"));
    }
    
    @Test
    void testLoadNonExistentFile() throws IOException {
        RedisStore newStore = new RedisStore();
        persistenceManager.load(newStore); // Should not throw exception
        assertEquals(0, newStore.size());
    }
    
    @Test
    void testDataFileSize() throws IOException {
        assertEquals(0, persistenceManager.getDataFileSize());
        
        store.set("key1", "value1");
        store.set("key2", "value2");
        
        persistenceManager.save(store);
        assertTrue(persistenceManager.getDataFileSize() > 0);
    }
    
    @Test
    void testExpiredKeysNotSaved() throws IOException {
        store.set("expired", "value", 1);
        
        try {
            Thread.sleep(1100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        persistenceManager.save(store);
        
        RedisStore newStore = new RedisStore();
        persistenceManager.load(newStore);
        
        assertEquals(0, newStore.size());
        assertNull(newStore.get("expired"));
    }
}

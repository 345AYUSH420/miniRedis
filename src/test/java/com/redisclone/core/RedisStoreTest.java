package com.redisclone.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class RedisStoreTest {
    private RedisStore store;
    
    @BeforeEach
    void setUp() {
        store = new RedisStore();
    }
    
    @Test
    void testStringOperations() {
        // Test SET and GET
        assertEquals("OK", store.set("key1", "value1"));
        assertEquals("value1", store.get("key1"));
        
        // Test GET non-existent key
        assertNull(store.get("nonexistent"));
        
        // Test EXISTS
        assertTrue(store.exists("key1"));
        assertFalse(store.exists("nonexistent"));
        
        // Test DEL
        assertEquals(1, store.del("key1"));
        assertFalse(store.exists("key1"));
        
        // Test DEL non-existent key
        assertEquals(0, store.del("nonexistent"));
    }
    
    @Test
    void testKeysOperation() {
        store.set("key1", "value1");
        store.set("key2", "value2");
        store.set("test", "value3");
        
        Set<String> allKeys = store.keys("*");
        assertEquals(3, allKeys.size());
        assertTrue(allKeys.contains("key1"));
        assertTrue(allKeys.contains("key2"));
        assertTrue(allKeys.contains("test"));
        
        Set<String> keyPattern = store.keys("key*");
        assertEquals(2, keyPattern.size());
        assertTrue(keyPattern.contains("key1"));
        assertTrue(keyPattern.contains("key2"));
    }
    
    @Test
    void testExpiration() {
        // Test SET with TTL
        store.set("key1", "value1", 1);
        assertEquals("value1", store.get("key1"));
        
        // Test TTL
        long ttl = store.ttl("key1");
        assertTrue(ttl > 0 && ttl <= 1);
        
        // Test EXPIRE
        store.set("key2", "value2");
        assertEquals(1, store.expire("key2", 1));
        
        // Test TTL on non-existent key
        assertEquals(-2, store.ttl("nonexistent"));
        
        // Test TTL on key without expiration
        store.set("key3", "value3");
        assertEquals(-1, store.ttl("key3"));
    }
    
    @Test
    void testHashOperations() {
        // Test HSET
        assertEquals(1, store.hset("hash1", "field1", "value1"));
        assertEquals(0, store.hset("hash1", "field1", "value1_updated")); // Update existing field
        
        // Test HGET
        assertEquals("value1_updated", store.hget("hash1", "field1"));
        assertNull(store.hget("hash1", "nonexistent"));
        assertNull(store.hget("nonexistent", "field1"));
        
        // Test HGETALL
        store.hset("hash1", "field2", "value2");
        Map<String, String> hash = store.hgetall("hash1");
        assertEquals(2, hash.size());
        assertEquals("value1_updated", hash.get("field1"));
        assertEquals("value2", hash.get("field2"));
        
        // Test HDEL
        assertEquals(1, store.hdel("hash1", "field1"));
        assertEquals(0, store.hdel("hash1", "nonexistent"));
        
        // Test HDEL on non-existent key
        assertEquals(0, store.hdel("nonexistent", "field1"));
    }
    
    @Test
    void testListOperations() {
        // Test LPUSH
        assertEquals(1, store.lpush("list1", "item1"));
        assertEquals(2, store.lpush("list1", "item2"));
        
        // Test RPUSH
        assertEquals(3, store.rpush("list1", "item3"));
        
        // Test LRANGE
        List<String> range = store.lrange("list1", 0, -1);
        assertEquals(3, range.size());
        assertEquals("item2", range.get(0)); // LPUSH adds to front
        assertEquals("item1", range.get(1));
        assertEquals("item3", range.get(2)); // RPUSH adds to back
        
        // Test LPOP
        assertEquals("item2", store.lpop("list1"));
        assertEquals(2, store.lrange("list1", 0, -1).size());
        
        // Test RPOP
        assertEquals("item3", store.rpop("list1"));
        assertEquals(1, store.lrange("list1", 0, -1).size());
        
        // Test operations on non-existent list
        assertNull(store.lpop("nonexistent"));
        assertNull(store.rpop("nonexistent"));
        assertTrue(store.lrange("nonexistent", 0, -1).isEmpty());
    }
    
    @Test
    void testSetOperations() {
        // Test SADD
        assertEquals(1, store.sadd("set1", "member1"));
        assertEquals(0, store.sadd("set1", "member1")); // Duplicate
        assertEquals(1, store.sadd("set1", "member2"));
        
        // Test SMEMBERS
        Set<String> members = store.smembers("set1");
        assertEquals(2, members.size());
        assertTrue(members.contains("member1"));
        assertTrue(members.contains("member2"));
        
        // Test SISMEMBER
        assertTrue(store.sismember("set1", "member1"));
        assertFalse(store.sismember("set1", "member3"));
        assertFalse(store.sismember("nonexistent", "member1"));
        
        // Test SREM
        assertEquals(1, store.srem("set1", "member1"));
        assertEquals(0, store.srem("set1", "member1")); // Already removed
        assertEquals(0, store.srem("nonexistent", "member1"));
        
        // Test operations on non-existent set
        assertTrue(store.smembers("nonexistent").isEmpty());
    }
    
    @Test
    void testSortedSetOperations() {
        // Test ZADD
        assertEquals(1, store.zadd("zset1", 1.0, "member1"));
        assertEquals(1, store.zadd("zset1", 2.0, "member2"));
        assertEquals(1, store.zadd("zset1", 1.5, "member1")); // Update score
        
        // Test ZSCORE
        assertEquals(1.5, store.zscore("zset1", "member1"));
        assertEquals(2.0, store.zscore("zset1", "member2"));
        assertNull(store.zscore("zset1", "member3"));
        assertNull(store.zscore("nonexistent", "member1"));
        
        // Test ZRANGE
        List<String> range = store.zrange("zset1", 0, -1);
        assertEquals(2, range.size());
        assertEquals("member1", range.get(0)); // Lower score first
        assertEquals("member2", range.get(1));
        
        // Test operations on non-existent sorted set
        assertTrue(store.zrange("nonexistent", 0, -1).isEmpty());
    }
    
    @Test
    void testWrongTypeOperations() {
        // Set a string value
        store.set("key1", "string_value");
        
        // Try to perform hash operations on string
        assertThrows(ClassCastException.class, () -> store.hget("key1", "field1"));
        assertThrows(ClassCastException.class, () -> store.lpush("key1", "item1"));
        assertThrows(ClassCastException.class, () -> store.sadd("key1", "member1"));
        assertThrows(ClassCastException.class, () -> store.zadd("key1", 1.0, "member1"));
    }
    
    @Test
    void testMultipleKeyOperations() {
        store.set("key1", "value1");
        store.set("key2", "value2");
        store.set("key3", "value3");
        
        // Test DEL with multiple keys
        assertEquals(2, store.del("key1", "key3"));
        assertFalse(store.exists("key1"));
        assertTrue(store.exists("key2"));
        assertFalse(store.exists("key3"));
        
        // Test EXISTS with individual keys
        store.set("key1", "value1");
        assertTrue(store.exists("key1"));
        assertTrue(store.exists("key2"));
        assertFalse(store.exists("key3"));
        assertFalse(store.exists("key4"));
    }
}

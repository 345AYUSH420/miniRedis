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
        store = RedisStore.getInstance();
        store.clear();
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
        store.set("key1", "value1", 1);
        assertEquals("value1", store.get("key1"));
        
        long ttl = store.ttl("key1");
        assertTrue(ttl > 0 && ttl <= 1);
        
        store.set("key2", "value2");
        assertEquals(1, store.expire("key2", 1));
        
        assertEquals(-2, store.ttl("nonexistent"));
        
        store.set("key3", "value3");
        assertEquals(-1, store.ttl("key3"));
    }
    
    @Test
    void testHashOperations() {
        assertEquals(1, store.hset("hash1", "field1", "value1"));
        assertEquals(0, store.hset("hash1", "field1", "value1_updated")); // Update existing field
        
        assertEquals("value1_updated", store.hget("hash1", "field1"));
        assertNull(store.hget("hash1", "nonexistent"));
        assertNull(store.hget("nonexistent", "field1"));
        
        store.hset("hash1", "field2", "value2");
        Map<String, String> hash = store.hgetall("hash1");
        assertEquals(2, hash.size());
        assertEquals("value1_updated", hash.get("field1"));
        assertEquals("value2", hash.get("field2"));
        
        assertEquals(1, store.hdel("hash1", "field1"));
        assertEquals(0, store.hdel("hash1", "nonexistent"));
        
        assertEquals(0, store.hdel("nonexistent", "field1"));
    }
    
    @Test
    void testListOperations() {
        assertEquals(1, store.lpush("list1", "item1"));
        assertEquals(2, store.lpush("list1", "item2"));
        
        assertEquals(3, store.rpush("list1", "item3"));
        
        List<String> range = store.lrange("list1", 0, -1);
        assertEquals(3, range.size());
        assertEquals("item2", range.get(0));
        assertEquals("item1", range.get(1));
        assertEquals("item3", range.get(2));
        
        assertEquals("item2", store.lpop("list1"));
        assertEquals(2, store.lrange("list1", 0, -1).size());
        
        assertEquals("item3", store.rpop("list1"));
        assertEquals(1, store.lrange("list1", 0, -1).size());
        
        assertNull(store.lpop("nonexistent"));
        assertNull(store.rpop("nonexistent"));
        assertTrue(store.lrange("nonexistent", 0, -1).isEmpty());
    }
    
    @Test
    void testSetOperations() {
        // Test SADD
        assertEquals(1, store.sadd("set1", "member1"));
        assertEquals(0, store.sadd("set1", "member1"));
        assertEquals(1, store.sadd("set1", "member2"));
        
        Set<String> members = store.smembers("set1");
        assertEquals(2, members.size());
        assertTrue(members.contains("member1"));
        assertTrue(members.contains("member2"));
        
        assertTrue(store.sismember("set1", "member1"));
        assertFalse(store.sismember("set1", "member3"));
        assertFalse(store.sismember("nonexistent", "member1"));
        
        assertEquals(1, store.srem("set1", "member1"));
        assertEquals(0, store.srem("set1", "member1"));
        assertEquals(0, store.srem("nonexistent", "member1"));
        
        assertTrue(store.smembers("nonexistent").isEmpty());
    }
    
    @Test
    void testSortedSetOperations() {
        assertEquals(1, store.zadd("zset1", 1.0, "member1"));
        assertEquals(1, store.zadd("zset1", 2.0, "member2"));
        assertEquals(1, store.zadd("zset1", 1.5, "member1"));
        
        assertEquals(1.5, store.zscore("zset1", "member1"));
        assertEquals(2.0, store.zscore("zset1", "member2"));
        assertNull(store.zscore("zset1", "member3"));
        assertNull(store.zscore("nonexistent", "member1"));
        
        List<String> range = store.zrange("zset1", 0, -1);
        assertEquals(2, range.size());
        assertEquals("member1", range.get(0));
        assertEquals("member2", range.get(1));
        
        assertTrue(store.zrange("nonexistent", 0, -1).isEmpty());
    }
    
    @Test
    void testWrongTypeOperations() {
        store.set("key1", "string_value");
        
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
        
        assertEquals(2, store.del("key1", "key3"));
        assertFalse(store.exists("key1"));
        assertTrue(store.exists("key2"));
        assertFalse(store.exists("key3"));
        
        store.set("key1", "value1");
        assertTrue(store.exists("key1"));
        assertTrue(store.exists("key2"));
        assertFalse(store.exists("key3"));
        assertFalse(store.exists("key4"));
    }
}

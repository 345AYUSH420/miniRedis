package com.redisclone.core;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

/**
 * Core Redis store implementation with thread-safe operations
 */
public class RedisStore {
    private final ConcurrentHashMap<String, RedisValue> store;
    private final ConcurrentHashMap<String, Instant> expirationTimes;
    
    public RedisStore() {
        this.store = new ConcurrentHashMap<>();
        this.expirationTimes = new ConcurrentHashMap<>();
    }
    
    // String operations
    public String set(String key, String value) {
        store.put(key, new RedisValue(RedisValue.DataType.STRING, value));
        expirationTimes.remove(key); // Clear any existing expiration
        return "OK";
    }
    
    public String set(String key, String value, long ttlSeconds) {
        Instant expiresAt = Instant.now().plusSeconds(ttlSeconds);
        store.put(key, new RedisValue(RedisValue.DataType.STRING, value, ttlSeconds));
        expirationTimes.put(key, expiresAt);
        return "OK";
    }
    
    public String get(String key) {
        RedisValue value = store.get(key);
        if (value == null || value.isExpired()) {
            if (value != null && value.isExpired()) {
                store.remove(key);
                expirationTimes.remove(key);
            }
            return null;
        }
        return value.getStringValue();
    }
    
    public boolean exists(String key) {
        RedisValue value = store.get(key);
        if (value == null || value.isExpired()) {
            if (value != null && value.isExpired()) {
                store.remove(key);
                expirationTimes.remove(key);
            }
            return false;
        }
        return true;
    }
    
    public long del(String... keys) {
        long deleted = 0;
        for (String key : keys) {
            if (store.remove(key) != null) {
                expirationTimes.remove(key);
                deleted++;
            }
        }
        return deleted;
    }
    
    public Set<String> keys(String pattern) {
        return store.keySet().stream()
                .filter(key -> matchesPattern(key, pattern))
                .filter(key -> !isExpired(key))
                .collect(Collectors.toSet());
    }
    
    public long expire(String key, long seconds) {
        RedisValue value = store.get(key);
        if (value == null || value.isExpired()) {
            return 0;
        }
        Instant expiresAt = Instant.now().plusSeconds(seconds);
        value.setExpiresAt(expiresAt);
        expirationTimes.put(key, expiresAt);
        return 1;
    }
    
    public long ttl(String key) {
        RedisValue value = store.get(key);
        if (value == null) {
            return -2; // Key doesn't exist
        }
        if (value.isExpired()) {
            store.remove(key);
            expirationTimes.remove(key);
            return -2;
        }
        return value.getTTL();
    }
    
    // Hash operations
    public long hset(String key, String field, String value) {
        RedisValue redisValue = store.get(key);
        ConcurrentHashMap<String, String> hash;
        
        if (redisValue == null || redisValue.isExpired()) {
            hash = new ConcurrentHashMap<>();
            store.put(key, new RedisValue(RedisValue.DataType.HASH, hash));
        } else if (redisValue.getType() != RedisValue.DataType.HASH) {
            throw new ClassCastException("WRONGTYPE Operation against a key holding the wrong kind of value");
        } else {
            hash = redisValue.getHashValue();
        }
        
        boolean isNewField = !hash.containsKey(field);
        hash.put(field, value);
        return isNewField ? 1 : 0;
    }
    
    public String hget(String key, String field) {
        RedisValue redisValue = store.get(key);
        if (redisValue == null || redisValue.isExpired() || redisValue.getType() != RedisValue.DataType.HASH) {
            return null;
        }
        return redisValue.getHashValue().get(field);
    }
    
    public Map<String, String> hgetall(String key) {
        RedisValue redisValue = store.get(key);
        if (redisValue == null || redisValue.isExpired() || redisValue.getType() != RedisValue.DataType.HASH) {
            return new HashMap<>();
        }
        return new HashMap<>(redisValue.getHashValue());
    }
    
    public long hdel(String key, String... fields) {
        RedisValue redisValue = store.get(key);
        if (redisValue == null || redisValue.isExpired() || redisValue.getType() != RedisValue.DataType.HASH) {
            return 0;
        }
        
        ConcurrentHashMap<String, String> hash = redisValue.getHashValue();
        long deleted = 0;
        for (String field : fields) {
            if (hash.remove(field) != null) {
                deleted++;
            }
        }
        return deleted;
    }
    
    // List operations
    public long lpush(String key, String... values) {
        RedisValue redisValue = store.get(key);
        CopyOnWriteArrayList<String> list;
        
        if (redisValue == null || redisValue.isExpired()) {
            list = new CopyOnWriteArrayList<>();
            store.put(key, new RedisValue(RedisValue.DataType.LIST, list));
        } else if (redisValue.getType() != RedisValue.DataType.LIST) {
            throw new ClassCastException("WRONGTYPE Operation against a key holding the wrong kind of value");
        } else {
            list = redisValue.getListValue();
        }
        
        for (String value : values) {
            list.add(0, value);
        }
        return list.size();
    }
    
    public long rpush(String key, String... values) {
        RedisValue redisValue = store.get(key);
        CopyOnWriteArrayList<String> list;
        
        if (redisValue == null || redisValue.isExpired()) {
            list = new CopyOnWriteArrayList<>();
            store.put(key, new RedisValue(RedisValue.DataType.LIST, list));
        } else if (redisValue.getType() != RedisValue.DataType.LIST) {
            throw new ClassCastException("WRONGTYPE Operation against a key holding the wrong kind of value");
        } else {
            list = redisValue.getListValue();
        }
        
        list.addAll(Arrays.asList(values));
        return list.size();
    }
    
    public String lpop(String key) {
        RedisValue redisValue = store.get(key);
        if (redisValue == null || redisValue.isExpired() || redisValue.getType() != RedisValue.DataType.LIST) {
            return null;
        }
        
        CopyOnWriteArrayList<String> list = redisValue.getListValue();
        return list.isEmpty() ? null : list.remove(0);
    }
    
    public String rpop(String key) {
        RedisValue redisValue = store.get(key);
        if (redisValue == null || redisValue.isExpired() || redisValue.getType() != RedisValue.DataType.LIST) {
            return null;
        }
        
        CopyOnWriteArrayList<String> list = redisValue.getListValue();
        return list.isEmpty() ? null : list.remove(list.size() - 1);
    }
    
    public List<String> lrange(String key, long start, long stop) {
        RedisValue redisValue = store.get(key);
        if (redisValue == null || redisValue.isExpired() || redisValue.getType() != RedisValue.DataType.LIST) {
            return new ArrayList<>();
        }
        
        CopyOnWriteArrayList<String> list = redisValue.getListValue();
        int size = list.size();
        
        // Handle negative indices
        if (start < 0) start = size + start;
        if (stop < 0) stop = size + stop;
        
        start = Math.max(0, start);
        stop = Math.min(size - 1, stop);
        
        if (start > stop) {
            return new ArrayList<>();
        }
        
        return new ArrayList<>(list.subList((int) start, (int) stop + 1));
    }
    
    // Set operations
    public long sadd(String key, String... members) {
        RedisValue redisValue = store.get(key);
        CopyOnWriteArraySet<String> set;
        
        if (redisValue == null || redisValue.isExpired()) {
            set = new CopyOnWriteArraySet<>();
            store.put(key, new RedisValue(RedisValue.DataType.SET, set));
        } else if (redisValue.getType() != RedisValue.DataType.SET) {
            throw new ClassCastException("WRONGTYPE Operation against a key holding the wrong kind of value");
        } else {
            set = redisValue.getSetValue();
        }
        
        long added = 0;
        for (String member : members) {
            if (set.add(member)) {
                added++;
            }
        }
        return added;
    }
    
    public Set<String> smembers(String key) {
        RedisValue redisValue = store.get(key);
        if (redisValue == null || redisValue.isExpired() || redisValue.getType() != RedisValue.DataType.SET) {
            return new HashSet<>();
        }
        return new HashSet<>(redisValue.getSetValue());
    }
    
    public boolean sismember(String key, String member) {
        RedisValue redisValue = store.get(key);
        if (redisValue == null || redisValue.isExpired() || redisValue.getType() != RedisValue.DataType.SET) {
            return false;
        }
        return redisValue.getSetValue().contains(member);
    }
    
    public long srem(String key, String... members) {
        RedisValue redisValue = store.get(key);
        if (redisValue == null || redisValue.isExpired() || redisValue.getType() != RedisValue.DataType.SET) {
            return 0;
        }
        
        CopyOnWriteArraySet<String> set = redisValue.getSetValue();
        long removed = 0;
        for (String member : members) {
            if (set.remove(member)) {
                removed++;
            }
        }
        return removed;
    }
    
    // Sorted Set operations
    public long zadd(String key, double score, String member) {
        RedisValue redisValue = store.get(key);
        ConcurrentSkipListSet<RedisValue.ScoredMember> sortedSet;
        
        if (redisValue == null || redisValue.isExpired()) {
            sortedSet = new ConcurrentSkipListSet<>();
            store.put(key, new RedisValue(RedisValue.DataType.SORTED_SET, sortedSet));
        } else if (redisValue.getType() != RedisValue.DataType.SORTED_SET) {
            throw new ClassCastException("WRONGTYPE Operation against a key holding the wrong kind of value");
        } else {
            sortedSet = redisValue.getSortedSetValue();
        }
        
        // Remove existing member if it exists
        sortedSet.removeIf(sm -> sm.getMember().equals(member));
        
        // Add new member
        sortedSet.add(new RedisValue.ScoredMember(score, member));
        return 1;
    }
    
    public Double zscore(String key, String member) {
        RedisValue redisValue = store.get(key);
        if (redisValue == null || redisValue.isExpired() || redisValue.getType() != RedisValue.DataType.SORTED_SET) {
            return null;
        }
        
        return redisValue.getSortedSetValue().stream()
                .filter(sm -> sm.getMember().equals(member))
                .map(RedisValue.ScoredMember::getScore)
                .findFirst()
                .orElse(null);
    }
    
    public List<String> zrange(String key, long start, long stop) {
        RedisValue redisValue = store.get(key);
        if (redisValue == null || redisValue.isExpired() || redisValue.getType() != RedisValue.DataType.SORTED_SET) {
            return new ArrayList<>();
        }
        
        ConcurrentSkipListSet<RedisValue.ScoredMember> sortedSet = redisValue.getSortedSetValue();
        List<RedisValue.ScoredMember> members = new ArrayList<>(sortedSet);
        
        int size = members.size();
        if (start < 0) start = size + start;
        if (stop < 0) stop = size + stop;
        
        start = Math.max(0, start);
        stop = Math.min(size - 1, stop);
        
        if (start > stop) {
            return new ArrayList<>();
        }
        
        return members.subList((int) start, (int) stop + 1)
                .stream()
                .map(RedisValue.ScoredMember::getMember)
                .collect(Collectors.toList());
    }
    
    // Utility methods
    private boolean matchesPattern(String key, String pattern) {
        if (pattern.equals("*")) {
            return true;
        }
        // Simple pattern matching - can be enhanced for more complex patterns
        return key.matches(pattern.replace("*", ".*"));
    }
    
    private boolean isExpired(String key) {
        RedisValue value = store.get(key);
        if (value == null) {
            return false;
        }
        if (value.isExpired()) {
            store.remove(key);
            expirationTimes.remove(key);
            return true;
        }
        return false;
    }
    
    public void cleanupExpired() {
        Instant now = Instant.now();
        expirationTimes.entrySet().removeIf(entry -> {
            if (now.isAfter(entry.getValue())) {
                store.remove(entry.getKey());
                return true;
            }
            return false;
        });
    }
    
    public int size() {
        return store.size();
    }
    
    public void clear() {
        store.clear();
        expirationTimes.clear();
    }
    
    // Method for persistence support
    public Set<Map.Entry<String, RedisValue>> entrySet() {
        return store.entrySet();
    }
    
    public void put(String key, RedisValue value) {
        store.put(key, value);
        if (value.getExpiresAt() != null) {
            expirationTimes.put(key, value.getExpiresAt());
        }
    }
}

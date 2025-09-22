package com.redisclone.persistence;

import com.redisclone.core.RedisStore;
import com.redisclone.core.RedisValue;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Simple persistence manager for Redis clone
 * Implements basic RDB-like functionality
 */
public class PersistenceManager {
    private final String rdbFile;
    
    public PersistenceManager(String dataDir) {
        this.rdbFile = Paths.get(dataDir, "dump.rdb").toString();
        
        // Create data directory if it doesn't exist
        try {
            Files.createDirectories(Paths.get(dataDir));
        } catch (IOException e) {
            throw new RuntimeException("Failed to create data directory: " + dataDir, e);
        }
    }
    
    /**
     * Save the Redis store to disk
     */
    public void save(RedisStore store) throws IOException {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(rdbFile))) {
            // Write header
            out.writeUTF("REDIS_CLONE_RDB");
            out.writeInt(1); // Version
            
            // Write store size
            out.writeInt(store.size());
            
            // Write each key-value pair
            for (Map.Entry<String, RedisValue> entry : store.entrySet()) {
                String key = entry.getKey();
                RedisValue value = entry.getValue();
                
                // Skip expired keys
                if (value.isExpired()) {
                    continue;
                }
                
                out.writeUTF(key);
                writeRedisValue(out, value);
            }
            
            out.flush();
        }
    }
    
    /**
     * Load the Redis store from disk
     */
    public void load(RedisStore store) throws IOException {
        Path rdbPath = Paths.get(rdbFile);
        if (!Files.exists(rdbPath)) {
            return; // No existing data file
        }
        
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(rdbFile))) {
            // Read header
            String header = in.readUTF();
            if (!"REDIS_CLONE_RDB".equals(header)) {
                throw new IOException("Invalid RDB file format");
            }
            
            int version = in.readInt();
            if (version != 1) {
                throw new IOException("Unsupported RDB version: " + version);
            }
            
            // Read store size
            int size = in.readInt();
            
            // Read each key-value pair
            for (int i = 0; i < size; i++) {
                String key = in.readUTF();
                RedisValue value = readRedisValue(in);
                
                // Only load non-expired keys
                if (!value.isExpired()) {
                    store.put(key, value);
                }
            }
        }
    }
    
    private void writeRedisValue(ObjectOutputStream out, RedisValue value) throws IOException {
        out.writeUTF(value.getType().name());
        
        switch (value.getType()) {
            case STRING:
                out.writeUTF(value.getStringValue());
                break;
                
            case HASH:
                ConcurrentHashMap<String, String> hash = value.getHashValue();
                out.writeInt(hash.size());
                for (Map.Entry<String, String> entry : hash.entrySet()) {
                    out.writeUTF(entry.getKey());
                    out.writeUTF(entry.getValue());
                }
                break;
                
            case LIST:
                CopyOnWriteArrayList<String> list = value.getListValue();
                out.writeInt(list.size());
                for (String item : list) {
                    out.writeUTF(item);
                }
                break;
                
            case SET:
                CopyOnWriteArraySet<String> set = value.getSetValue();
                out.writeInt(set.size());
                for (String member : set) {
                    out.writeUTF(member);
                }
                break;
                
            case SORTED_SET:
                ConcurrentSkipListSet<RedisValue.ScoredMember> sortedSet = value.getSortedSetValue();
                out.writeInt(sortedSet.size());
                for (RedisValue.ScoredMember member : sortedSet) {
                    out.writeDouble(member.getScore());
                    out.writeUTF(member.getMember());
                }
                break;
        }
        
        // Write expiration info
        if (value.getExpiresAt() != null) {
            out.writeBoolean(true);
            out.writeLong(value.getExpiresAt().getEpochSecond());
        } else {
            out.writeBoolean(false);
        }
    }
    
    private RedisValue readRedisValue(ObjectInputStream in) throws IOException {
        RedisValue.DataType type = RedisValue.DataType.valueOf(in.readUTF());
        Object value;
        
        switch (type) {
            case STRING:
                value = in.readUTF();
                break;
                
            case HASH:
                ConcurrentHashMap<String, String> hash = new ConcurrentHashMap<>();
                int hashSize = in.readInt();
                for (int i = 0; i < hashSize; i++) {
                    String field = in.readUTF();
                    String fieldValue = in.readUTF();
                    hash.put(field, fieldValue);
                }
                value = hash;
                break;
                
            case LIST:
                CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();
                int listSize = in.readInt();
                for (int i = 0; i < listSize; i++) {
                    list.add(in.readUTF());
                }
                value = list;
                break;
                
            case SET:
                CopyOnWriteArraySet<String> set = new CopyOnWriteArraySet<>();
                int setSize = in.readInt();
                for (int i = 0; i < setSize; i++) {
                    set.add(in.readUTF());
                }
                value = set;
                break;
                
            case SORTED_SET:
                ConcurrentSkipListSet<RedisValue.ScoredMember> sortedSet = new ConcurrentSkipListSet<>();
                int sortedSetSize = in.readInt();
                for (int i = 0; i < sortedSetSize; i++) {
                    double score = in.readDouble();
                    String member = in.readUTF();
                    sortedSet.add(new RedisValue.ScoredMember(score, member));
                }
                value = sortedSet;
                break;
                
            default:
                throw new IOException("Unknown data type: " + type);
        }
        
        RedisValue redisValue = new RedisValue(type, value);
        
        // Read expiration info
        boolean hasExpiration = in.readBoolean();
        if (hasExpiration) {
            long expirationEpoch = in.readLong();
            redisValue.setExpiresAt(Instant.ofEpochSecond(expirationEpoch));
        }
        
        return redisValue;
    }
    
    /**
     * Check if a data file exists
     */
    public boolean hasDataFile() {
        return Files.exists(Paths.get(rdbFile));
    }
    
    /**
     * Delete the data file
     */
    public void deleteDataFile() throws IOException {
        Files.deleteIfExists(Paths.get(rdbFile));
    }
    
    /**
     * Get the size of the data file in bytes
     */
    public long getDataFileSize() throws IOException {
        Path rdbPath = Paths.get(rdbFile);
        if (!Files.exists(rdbPath)) {
            return 0;
        }
        return Files.size(rdbPath);
    }
}

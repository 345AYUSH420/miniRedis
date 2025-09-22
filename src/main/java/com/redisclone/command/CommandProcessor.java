package com.redisclone.command;

import com.redisclone.core.RedisStore; //  it is a key value store which uses the redis in memeory data structure store to store data.
import com.redisclone.protocol.RespParser;
import com.redisclone.protocol.RespWriter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Processes Redis commands and returns appropriate responses
 */
public class CommandProcessor {
    private final RedisStore store;
    
    public CommandProcessor(RedisStore store) {
        this.store = store;
    }
    
    public void processCommand(RespParser.RespValue command, RespWriter writer) throws IOException {
        if (command.getType() != RespParser.RespValue.Type.ARRAY) {
            writer.writeError("ERR Protocol error: expected array");
            return;
        }
        
        List<RespParser.RespValue> args = command.getArray();
        if (args.isEmpty()) {
            writer.writeError("ERR Protocol error: empty command");
            return;
        }
        
        String cmd = args.get(0).getString().toUpperCase();
        
        try {
            switch (cmd) {
                // String commands
                case "SET":
                    handleSet(args, writer);
                    break;
                case "GET":
                    handleGet(args, writer);
                    break;
                case "EXISTS":
                    handleExists(args, writer);
                    break;
                case "DEL":
                    handleDel(args, writer);
                    break;
                case "KEYS":
                    handleKeys(args, writer);
                    break;
                case "EXPIRE":
                    handleExpire(args, writer);
                    break;
                case "TTL":
                    handleTTL(args, writer);
                    break;
                
                // Hash commands
                case "HSET":
                    handleHset(args, writer);
                    break;
                case "HGET":
                    handleHget(args, writer);
                    break;
                case "HGETALL":
                    handleHgetall(args, writer);
                    break;
                case "HDEL":
                    handleHdel(args, writer);
                    break;
                
                // List commands
                case "LPUSH":
                    handleLpush(args, writer);
                    break;
                case "RPUSH":
                    handleRpush(args, writer);
                    break;
                case "LPOP":
                    handleLpop(args, writer);
                    break;
                case "RPOP":
                    handleRpop(args, writer);
                    break;
                case "LRANGE":
                    handleLrange(args, writer);
                    break;
                
                // Set commands
                case "SADD":
                    handleSadd(args, writer);
                    break;
                case "SMEMBERS":
                    handleSmembers(args, writer);
                    break;
                case "SISMEMBER":
                    handleSismember(args, writer);
                    break;
                case "SREM":
                    handleSrem(args, writer);
                    break;
                
                // Sorted Set commands
                case "ZADD":
                    handleZadd(args, writer);
                    break;
                case "ZSCORE":
                    handleZscore(args, writer);
                    break;
                case "ZRANGE":
                    handleZrange(args, writer);
                    break;
                
                // Server commands
                case "PING":
                    handlePing(args, writer);
                    break;
                case "INFO":
                    handleInfo(args, writer);
                    break;
                case "FLUSHALL":
                    handleFlushall(args, writer);
                    break;
                
                default:
                    writer.writeError("ERR unknown command '" + cmd + "'");
            }
        } catch (Exception e) {
            writer.writeError("ERR " + e.getMessage());
        }
    }
    
    // String command handlers
    private void handleSet(List<RespParser.RespValue> args, RespWriter writer) {
        if (args.size() < 3) {
            writer.writeError("ERR wrong number of arguments for 'set' command");
            return;
        }
        
        String key = args.get(1).getString();
        String value = args.get(2).getString();
        
        if (args.size() == 3) {
            writer.writeSimpleString(store.set(key, value));
        } else if (args.size() == 5 && args.get(3).getString().equalsIgnoreCase("EX")) {
            long ttl = Long.parseLong(args.get(4).getString());
            writer.writeSimpleString(store.set(key, value, ttl));
        } else {
            writer.writeError("ERR syntax error");
        }
    }
    
    private void handleGet(List<RespParser.RespValue> args, RespWriter writer) {
        if (args.size() != 2) {
            writer.writeError("ERR wrong number of arguments for 'get' command");
            return;
        }
        
        String key = args.get(1).getString();
        String value = store.get(key);
        
        if (value == null) {
            writer.writeNull();
        } else {
            writer.writeBulkString(value);
        }
    }
    
    private void handleExists(List<RespParser.RespValue> args, RespWriter writer) {
        if (args.size() < 2) {
            writer.writeError("ERR wrong number of arguments for 'exists' command");
            return;
        }
        
        String[] keys = new String[args.size() - 1];
        for (int i = 1; i < args.size(); i++) {
            keys[i - 1] = args.get(i).getString();
        }
        
        long count = 0;
        for (String key : keys) {
            if (store.exists(key)) {
                count++;
            }
        }
        
        writer.writeInteger(count);
    }
    
    private void handleDel(List<RespParser.RespValue> args, RespWriter writer) {
        if (args.size() < 2) {
            writer.writeError("ERR wrong number of arguments for 'del' command");
            return;
        }
        
        String[] keys = new String[args.size() - 1];
        for (int i = 1; i < args.size(); i++) {
            keys[i - 1] = args.get(i).getString();
        }
        
        writer.writeInteger(store.del(keys));
    }
    
    private void handleKeys(List<RespParser.RespValue> args, RespWriter writer) {
        if (args.size() != 2) {
            writer.writeError("ERR wrong number of arguments for 'keys' command");
            return;
        }
        
        String pattern = args.get(1).getString();
        Set<String> keys = store.keys(pattern);
        
        writer.writeArray(keys.toArray(new String[0]));
    }
    
    private void handleExpire(List<RespParser.RespValue> args, RespWriter writer) {
        if (args.size() != 3) {
            writer.writeError("ERR wrong number of arguments for 'expire' command");
            return;
        }
        
        String key = args.get(1).getString();
        long seconds = Long.parseLong(args.get(2).getString());
        
        writer.writeInteger(store.expire(key, seconds));
    }
     

    // tells you remaining time to live for a key.
    private void handleTTL(List<RespParser.RespValue> args, RespWriter writer) {
        if (args.size() != 2) {
            writer.writeError("ERR wrong number of arguments for 'ttl' command");
            return;
        }
        
        String key = args.get(1).getString();
        writer.writeInteger(store.ttl(key));
    }
    
    // Hash command handlers
    private void handleHset(List<RespParser.RespValue> args, RespWriter writer) {
        if (args.size() != 4) {
            writer.writeError("ERR wrong number of arguments for 'hset' command");
            return;
        }
        
        String key = args.get(1).getString();
        String field = args.get(2).getString();
        String value = args.get(3).getString();
        
        writer.writeInteger(store.hset(key, field, value));
    }
    
    private void handleHget(List<RespParser.RespValue> args, RespWriter writer) {
        if (args.size() != 3) {
            writer.writeError("ERR wrong number of arguments for 'hget' command");
            return;
        }
        
        String key = args.get(1).getString();
        String field = args.get(2).getString();
        String value = store.hget(key, field);
        
        if (value == null) {
            writer.writeNull();
        } else {
            writer.writeBulkString(value);
        }
    }
    
    private void handleHgetall(List<RespParser.RespValue> args, RespWriter writer) {
        if (args.size() != 2) {
            writer.writeError("ERR wrong number of arguments for 'hgetall' command");
            return;
        }
        
        String key = args.get(1).getString();
        Map<String, String> hash = store.hgetall(key);
        
        String[] result = new String[hash.size() * 2];
        int i = 0;
        for (Map.Entry<String, String> entry : hash.entrySet()) {
            result[i++] = entry.getKey();
            result[i++] = entry.getValue();
        }
        
        writer.writeArray(result);
    }

    // delete that particular field from the hash.
    
    private void handleHdel(List<RespParser.RespValue> args, RespWriter writer) {
        if (args.size() < 3) {
            writer.writeError("ERR wrong number of arguments for 'hdel' command");
            return;
        }
        
        String key = args.get(1).getString();
        String[] fields = new String[args.size() - 2];
        for (int i = 2; i < args.size(); i++) {
            fields[i - 2] = args.get(i).getString();
        }
        
        writer.writeInteger(store.hdel(key, fields));
    }
    
    //push elements to the left of the list. basically a stack.
    private void handleLpush(List<RespParser.RespValue> args, RespWriter writer) {
        if (args.size() < 3) {
            writer.writeError("ERR wrong number of arguments for 'lpush' command");
            return;
        }
        
        String key = args.get(1).getString();
        String[] values = new String[args.size() - 2];
        for (int i = 2; i < args.size(); i++) {
            values[i - 2] = args.get(i).getString();
        }
        
        writer.writeInteger(store.lpush(key, values));
    }
    
    // add elements to the right of the list. a normal addition to the list.
    private void handleRpush(List<RespParser.RespValue> args, RespWriter writer) {
        if (args.size() < 3) {
            writer.writeError("ERR wrong number of arguments for 'rpush' command");
            return;
        }
        
        String key = args.get(1).getString();
        String[] values = new String[args.size() - 2];
        for (int i = 2; i < args.size(); i++) {
            values[i - 2] = args.get(i).getString();
        }
        
        writer.writeInteger(store.rpush(key, values));
    }
    

    // remove elements from the left of the list. basically pop the element from the stack.
    private void handleLpop(List<RespParser.RespValue> args, RespWriter writer) {
        if (args.size() != 2) {
            writer.writeError("ERR wrong number of arguments for 'lpop' command");
            return;
        }
        
        String key = args.get(1).getString();
        String value = store.lpop(key);
        
        if (value == null) {
            writer.writeNull();
        } else {
            writer.writeBulkString(value);
        }
    }
    // removes the last element. same as list removal.
    private void handleRpop(List<RespParser.RespValue> args, RespWriter writer) {
        if (args.size() != 2) {
            writer.writeError("ERR wrong number of arguments for 'rpop' command");
            return;
        }
        
        String key = args.get(1).getString();
        String value = store.rpop(key);
        
        if (value == null) {
            writer.writeNull();
        } else {
            writer.writeBulkString(value);
        }
    }
    

    // return the list of elements in a given range
    private void handleLrange(List<RespParser.RespValue> args, RespWriter writer) {
        if (args.size() != 4) {
            writer.writeError("ERR wrong number of arguments for 'lrange' command");
            return;
        }
        
        String key = args.get(1).getString();
        long start = Long.parseLong(args.get(2).getString());
        long stop = Long.parseLong(args.get(3).getString());
        
        List<String> values = store.lrange(key, start, stop);
        writer.writeArray(values.toArray(new String[0]));
    }
    
    // add members to the set.
    private void handleSadd(List<RespParser.RespValue> args, RespWriter writer) {
        if (args.size() < 3) {
            writer.writeError("ERR wrong number of arguments for 'sadd' command");
            return;
        }
        
        String key = args.get(1).getString();
        String[] members = new String[args.size() - 2];
        for (int i = 2; i < args.size(); i++) {
            members[i - 2] = args.get(i).getString();
        }
        
        writer.writeInteger(store.sadd(key, members));
    }
    

    // return all the members in the set.
    private void handleSmembers(List<RespParser.RespValue> args, RespWriter writer) {
        if (args.size() != 2) {
            writer.writeError("ERR wrong number of arguments for 'smembers' command");
            return;
        }
        
        String key = args.get(1).getString();
        Set<String> members = store.smembers(key);
        writer.writeArray(members.toArray(new String[0]));
    }
    

    // check whether a particulr member is present in the set.
    private void handleSismember(List<RespParser.RespValue> args, RespWriter writer) {
        if (args.size() != 3) {
            writer.writeError("ERR wrong number of arguments for 'sismember' command");
            return;
        }
        
        String key = args.get(1).getString();
        String member = args.get(2).getString();
        
        writer.writeInteger(store.sismember(key, member) ? 1 : 0);
    }
    

    // remove members from the set. can remove multiple members at once.
    private void handleSrem(List<RespParser.RespValue> args, RespWriter writer) {
        if (args.size() < 3) {
            writer.writeError("ERR wrong number of arguments for 'srem' command");
            return;
        }
        
        String key = args.get(1).getString();
        String[] members = new String[args.size() - 2];
        for (int i = 2; i < args.size(); i++) {
            members[i - 2] = args.get(i).getString();
        }
        
        writer.writeInteger(store.srem(key, members));
    }
    
    // adds a member to the sorted set with a score.
    private void handleZadd(List<RespParser.RespValue> args, RespWriter writer) {
        if (args.size() != 4) {
            writer.writeError("ERR wrong number of arguments for 'zadd' command");
            return;
        }
        
        String key = args.get(1).getString();
        double score = Double.parseDouble(args.get(2).getString());
        String member = args.get(3).getString();
        
        writer.writeInteger(store.zadd(key, score, member));
    }

    // gets score of a particular member in the sorted set.
    
    private void handleZscore(List<RespParser.RespValue> args, RespWriter writer) {
        if (args.size() != 3) {
            writer.writeError("ERR wrong number of arguments for 'zscore' command");
            return;
        }
        
        String key = args.get(1).getString();
        String member = args.get(2).getString();
        Double score = store.zscore(key, member);
        
        if (score == null) {
            writer.writeNull();
        } else {
            writer.writeBulkString(score.toString());
        }
    }
    
    private void handleZrange(List<RespParser.RespValue> args, RespWriter writer) {
        if (args.size() != 4) {
            writer.writeError("ERR wrong number of arguments for 'zrange' command");
            return;
        }
        
        String key = args.get(1).getString();
        long start = Long.parseLong(args.get(2).getString());
        long stop = Long.parseLong(args.get(3).getString());
        
        List<String> members = store.zrange(key, start, stop);
        writer.writeArray(members.toArray(new String[0]));
    }
    
    // check server connectivity
    private void handlePing(List<RespParser.RespValue> args, RespWriter writer) {
        if (args.size() == 1) {
            writer.writeSimpleString("PONG");
        } else if (args.size() == 2) {
            writer.writeBulkString(args.get(1).getString());
        } else {
            writer.writeError("ERR wrong number of arguments for 'ping' command");
        }
    }
    
    private void handleInfo(List<RespParser.RespValue> args, RespWriter writer) {
        StringBuilder info = new StringBuilder();
        info.append("# Server\n");
        info.append("redis_version:1.0.0\n");
        info.append("redis_mode:standalone\n");
        info.append("os:Java\n");
        info.append("arch_bits:64\n");
        info.append("uptime_in_seconds:").append(System.currentTimeMillis() / 1000).append("\n");
        info.append("\n# Memory\n");
        info.append("used_memory:").append(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()).append("\n");
        info.append("used_memory_human:").append((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024).append("M\n");
        info.append("\n# Stats\n");
        info.append("total_commands_processed:0\n");
        info.append("keyspace_hits:0\n");
        info.append("keyspace_misses:0\n");
        
        writer.writeBulkString(info.toString());
    }
    
    // clear all the data in the redis store.
    private void handleFlushall(List<RespParser.RespValue> args, RespWriter writer) {
        if (args.size() != 1) {
            writer.writeError("ERR wrong number of arguments for 'flushall' command");
            return;
        }
        
        store.clear();
        writer.writeOk();
    }
}

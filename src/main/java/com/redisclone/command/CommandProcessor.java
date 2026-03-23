package com.redisclone.command;

import com.redisclone.command.commands.*;
import com.redisclone.core.RedisStore;
import com.redisclone.protocol.RespParser;
import com.redisclone.protocol.RespWriter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Command Pattern implementation.
 *
 * Each Redis command is a separate class implementing the Command interface.
 * Commands are registered in a HashMap and looked up by name — no switch/if needed.
 * To add a new command: create a new class + add one line to the registry below.
 */
public class CommandProcessor {
    private final Map<String, Command> commandRegistry = new HashMap<>();

    public CommandProcessor(RedisStore store) {
        // String commands
        commandRegistry.put("SET",       new SetCommand(store));
        commandRegistry.put("GET",       new GetCommand(store));
        commandRegistry.put("DEL",       new DelCommand(store));
        commandRegistry.put("EXISTS",    new ExistsCommand(store));
        commandRegistry.put("KEYS",      new KeysCommand(store));
        commandRegistry.put("EXPIRE",    new ExpireCommand(store));
        commandRegistry.put("TTL",       new TtlCommand(store));

        // Hash commands
        commandRegistry.put("HSET",      new HSetCommand(store));
        commandRegistry.put("HGET",      new HGetCommand(store));
        commandRegistry.put("HGETALL",   new HGetAllCommand(store));
        commandRegistry.put("HDEL",      new HDelCommand(store));

        // List commands
        commandRegistry.put("LPUSH",     new LPushCommand(store));
        commandRegistry.put("RPUSH",     new RPushCommand(store));
        commandRegistry.put("LPOP",      new LPopCommand(store));
        commandRegistry.put("RPOP",      new RPopCommand(store));
        commandRegistry.put("LRANGE",    new LRangeCommand(store));

        // Set commands
        commandRegistry.put("SADD",      new SAddCommand(store));
        commandRegistry.put("SMEMBERS",  new SMembersCommand(store));
        commandRegistry.put("SISMEMBER", new SIsMemberCommand(store));
        commandRegistry.put("SREM",      new SRemCommand(store));

        // Sorted Set commands
        commandRegistry.put("ZADD",      new ZAddCommand(store));
        commandRegistry.put("ZSCORE",    new ZScoreCommand(store));
        commandRegistry.put("ZRANGE",    new ZRangeCommand(store));

        // Server commands
        commandRegistry.put("PING",      new PingCommand());
        commandRegistry.put("INFO",      new InfoCommand());
        commandRegistry.put("FLUSHALL",  new FlushAllCommand(store));
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
        Command handler = commandRegistry.get(cmd);

        if (handler == null) {
            writer.writeError("ERR unknown command '" + cmd + "'");
            return;
        }

        try {
            handler.execute(args, writer);
        } catch (Exception e) {
            writer.writeError("ERR " + e.getMessage());
        }
    }
}

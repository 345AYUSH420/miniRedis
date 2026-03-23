package com.redisclone.command.commands;

import com.redisclone.command.Command;
import com.redisclone.core.RedisStore;
import com.redisclone.protocol.RespParser;
import com.redisclone.protocol.RespWriter;

import java.util.List;

public class SetCommand implements Command {
    private final RedisStore store;

    public SetCommand(RedisStore store) {
        this.store = store;
    }

    @Override
    public void execute(List<RespParser.RespValue> args, RespWriter writer) {
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
}

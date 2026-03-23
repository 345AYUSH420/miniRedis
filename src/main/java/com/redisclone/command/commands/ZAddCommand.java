package com.redisclone.command.commands;

import com.redisclone.command.Command;
import com.redisclone.core.RedisStore;
import com.redisclone.protocol.RespParser;
import com.redisclone.protocol.RespWriter;

import java.util.List;

public class ZAddCommand implements Command {
    private final RedisStore store;

    public ZAddCommand(RedisStore store) {
        this.store = store;
    }

    @Override
    public void execute(List<RespParser.RespValue> args, RespWriter writer) {
        if (args.size() != 4) {
            writer.writeError("ERR wrong number of arguments for 'zadd' command");
            return;
        }
        String key = args.get(1).getString();
        double score = Double.parseDouble(args.get(2).getString());
        String member = args.get(3).getString();
        writer.writeInteger(store.zadd(key, score, member));
    }
}

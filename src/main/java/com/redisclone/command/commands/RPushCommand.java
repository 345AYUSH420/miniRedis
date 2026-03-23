package com.redisclone.command.commands;

import com.redisclone.command.Command;
import com.redisclone.core.RedisStore;
import com.redisclone.protocol.RespParser;
import com.redisclone.protocol.RespWriter;

import java.util.List;

public class RPushCommand implements Command {
    private final RedisStore store;

    public RPushCommand(RedisStore store) {
        this.store = store;
    }

    @Override
    public void execute(List<RespParser.RespValue> args, RespWriter writer) {
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
}

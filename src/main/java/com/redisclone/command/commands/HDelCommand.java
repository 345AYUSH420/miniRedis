package com.redisclone.command.commands;

import com.redisclone.command.Command;
import com.redisclone.core.RedisStore;
import com.redisclone.protocol.RespParser;
import com.redisclone.protocol.RespWriter;

import java.util.List;

public class HDelCommand implements Command {
    private final RedisStore store;

    public HDelCommand(RedisStore store) {
        this.store = store;
    }

    @Override
    public void execute(List<RespParser.RespValue> args, RespWriter writer) {
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
}

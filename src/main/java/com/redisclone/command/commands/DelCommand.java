package com.redisclone.command.commands;

import com.redisclone.command.Command;
import com.redisclone.core.RedisStore;
import com.redisclone.protocol.RespParser;
import com.redisclone.protocol.RespWriter;

import java.util.List;

public class DelCommand implements Command {
    private final RedisStore store;

    public DelCommand(RedisStore store) {
        this.store = store;
    }

    @Override
    public void execute(List<RespParser.RespValue> args, RespWriter writer) {
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
}

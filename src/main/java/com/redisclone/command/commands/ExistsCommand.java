package com.redisclone.command.commands;

import com.redisclone.command.Command;
import com.redisclone.core.RedisStore;
import com.redisclone.protocol.RespParser;
import com.redisclone.protocol.RespWriter;

import java.util.List;

public class ExistsCommand implements Command {
    private final RedisStore store;

    public ExistsCommand(RedisStore store) {
        this.store = store;
    }

    @Override
    public void execute(List<RespParser.RespValue> args, RespWriter writer) {
        if (args.size() < 2) {
            writer.writeError("ERR wrong number of arguments for 'exists' command");
            return;
        }
        long count = 0;
        for (int i = 1; i < args.size(); i++) {
            if (store.exists(args.get(i).getString())) {
                count++;
            }
        }
        writer.writeInteger(count);
    }
}

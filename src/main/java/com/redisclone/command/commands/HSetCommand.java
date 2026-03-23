package com.redisclone.command.commands;

import com.redisclone.command.Command;
import com.redisclone.core.RedisStore;
import com.redisclone.protocol.RespParser;
import com.redisclone.protocol.RespWriter;

import java.util.List;

public class HSetCommand implements Command {
    private final RedisStore store;

    public HSetCommand(RedisStore store) {
        this.store = store;
    }

    @Override
    public void execute(List<RespParser.RespValue> args, RespWriter writer) {
        if (args.size() != 4) {
            writer.writeError("ERR wrong number of arguments for 'hset' command");
            return;
        }
        writer.writeInteger(store.hset(args.get(1).getString(), args.get(2).getString(), args.get(3).getString()));
    }
}

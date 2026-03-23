package com.redisclone.command.commands;

import com.redisclone.command.Command;
import com.redisclone.core.RedisStore;
import com.redisclone.protocol.RespParser;
import com.redisclone.protocol.RespWriter;

import java.util.List;

public class TtlCommand implements Command {
    private final RedisStore store;

    public TtlCommand(RedisStore store) {
        this.store = store;
    }

    @Override
    public void execute(List<RespParser.RespValue> args, RespWriter writer) {
        if (args.size() != 2) {
            writer.writeError("ERR wrong number of arguments for 'ttl' command");
            return;
        }
        writer.writeInteger(store.ttl(args.get(1).getString()));
    }
}

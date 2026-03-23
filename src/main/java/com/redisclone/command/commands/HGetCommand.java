package com.redisclone.command.commands;

import com.redisclone.command.Command;
import com.redisclone.core.RedisStore;
import com.redisclone.protocol.RespParser;
import com.redisclone.protocol.RespWriter;

import java.util.List;

public class HGetCommand implements Command {
    private final RedisStore store;

    public HGetCommand(RedisStore store) {
        this.store = store;
    }

    @Override
    public void execute(List<RespParser.RespValue> args, RespWriter writer) {
        if (args.size() != 3) {
            writer.writeError("ERR wrong number of arguments for 'hget' command");
            return;
        }
        String value = store.hget(args.get(1).getString(), args.get(2).getString());
        if (value == null) {
            writer.writeNull();
        } else {
            writer.writeBulkString(value);
        }
    }
}

package com.redisclone.command.commands;

import com.redisclone.command.Command;
import com.redisclone.core.RedisStore;
import com.redisclone.protocol.RespParser;
import com.redisclone.protocol.RespWriter;

import java.util.List;

public class GetCommand implements Command {
    private final RedisStore store;

    public GetCommand(RedisStore store) {
        this.store = store;
    }

    @Override
    public void execute(List<RespParser.RespValue> args, RespWriter writer) {
        if (args.size() != 2) {
            writer.writeError("ERR wrong number of arguments for 'get' command");
            return;
        }
        String key = args.get(1).getString();
        String value = store.get(key);
        if (value == null) {
            writer.writeNull();
        } else {
            writer.writeBulkString(value);
        }
    }
}

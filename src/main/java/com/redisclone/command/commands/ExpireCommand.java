package com.redisclone.command.commands;

import com.redisclone.command.Command;
import com.redisclone.core.RedisStore;
import com.redisclone.protocol.RespParser;
import com.redisclone.protocol.RespWriter;

import java.util.List;

public class ExpireCommand implements Command {
    private final RedisStore store;

    public ExpireCommand(RedisStore store) {
        this.store = store;
    }

    @Override
    public void execute(List<RespParser.RespValue> args, RespWriter writer) {
        if (args.size() != 3) {
            writer.writeError("ERR wrong number of arguments for 'expire' command");
            return;
        }
        String key = args.get(1).getString();
        long seconds = Long.parseLong(args.get(2).getString());
        writer.writeInteger(store.expire(key, seconds));
    }
}

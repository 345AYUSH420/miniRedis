package com.redisclone.command.commands;

import com.redisclone.command.Command;
import com.redisclone.core.RedisStore;
import com.redisclone.protocol.RespParser;
import com.redisclone.protocol.RespWriter;

import java.util.List;

public class ZRangeCommand implements Command {
    private final RedisStore store;

    public ZRangeCommand(RedisStore store) {
        this.store = store;
    }

    @Override
    public void execute(List<RespParser.RespValue> args, RespWriter writer) {
        if (args.size() != 4) {
            writer.writeError("ERR wrong number of arguments for 'zrange' command");
            return;
        }
        String key = args.get(1).getString();
        long start = Long.parseLong(args.get(2).getString());
        long stop = Long.parseLong(args.get(3).getString());
        List<String> members = store.zrange(key, start, stop);
        writer.writeArray(members.toArray(new String[0]));
    }
}

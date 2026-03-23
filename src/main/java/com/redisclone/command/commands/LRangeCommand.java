package com.redisclone.command.commands;

import com.redisclone.command.Command;
import com.redisclone.core.RedisStore;
import com.redisclone.protocol.RespParser;
import com.redisclone.protocol.RespWriter;

import java.util.List;

public class LRangeCommand implements Command {
    private final RedisStore store;

    public LRangeCommand(RedisStore store) {
        this.store = store;
    }

    @Override
    public void execute(List<RespParser.RespValue> args, RespWriter writer) {
        if (args.size() != 4) {
            writer.writeError("ERR wrong number of arguments for 'lrange' command");
            return;
        }
        String key = args.get(1).getString();
        long start = Long.parseLong(args.get(2).getString());
        long stop = Long.parseLong(args.get(3).getString());
        List<String> values = store.lrange(key, start, stop);
        writer.writeArray(values.toArray(new String[0]));
    }
}

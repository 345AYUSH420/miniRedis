package com.redisclone.command.commands;

import com.redisclone.command.Command;
import com.redisclone.core.RedisStore;
import com.redisclone.protocol.RespParser;
import com.redisclone.protocol.RespWriter;

import java.util.List;
import java.util.Set;

public class KeysCommand implements Command {
    private final RedisStore store;

    public KeysCommand(RedisStore store) {
        this.store = store;
    }

    @Override
    public void execute(List<RespParser.RespValue> args, RespWriter writer) {
        if (args.size() != 2) {
            writer.writeError("ERR wrong number of arguments for 'keys' command");
            return;
        }
        Set<String> keys = store.keys(args.get(1).getString());
        writer.writeArray(keys.toArray(new String[0]));
    }
}

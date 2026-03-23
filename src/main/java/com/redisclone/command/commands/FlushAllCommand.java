package com.redisclone.command.commands;

import com.redisclone.command.Command;
import com.redisclone.core.RedisStore;
import com.redisclone.protocol.RespParser;
import com.redisclone.protocol.RespWriter;

import java.util.List;

public class FlushAllCommand implements Command {
    private final RedisStore store;

    public FlushAllCommand(RedisStore store) {
        this.store = store;
    }

    @Override
    public void execute(List<RespParser.RespValue> args, RespWriter writer) {
        if (args.size() != 1) {
            writer.writeError("ERR wrong number of arguments for 'flushall' command");
            return;
        }
        store.clear();
        writer.writeOk();
    }
}

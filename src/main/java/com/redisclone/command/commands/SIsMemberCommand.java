package com.redisclone.command.commands;

import com.redisclone.command.Command;
import com.redisclone.core.RedisStore;
import com.redisclone.protocol.RespParser;
import com.redisclone.protocol.RespWriter;

import java.util.List;

public class SIsMemberCommand implements Command {
    private final RedisStore store;

    public SIsMemberCommand(RedisStore store) {
        this.store = store;
    }

    @Override
    public void execute(List<RespParser.RespValue> args, RespWriter writer) {
        if (args.size() != 3) {
            writer.writeError("ERR wrong number of arguments for 'sismember' command");
            return;
        }
        writer.writeInteger(store.sismember(args.get(1).getString(), args.get(2).getString()) ? 1 : 0);
    }
}

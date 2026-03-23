package com.redisclone.command.commands;

import com.redisclone.command.Command;
import com.redisclone.core.RedisStore;
import com.redisclone.protocol.RespParser;
import com.redisclone.protocol.RespWriter;

import java.util.List;
import java.util.Set;

public class SMembersCommand implements Command {
    private final RedisStore store;

    public SMembersCommand(RedisStore store) {
        this.store = store;
    }

    @Override
    public void execute(List<RespParser.RespValue> args, RespWriter writer) {
        if (args.size() != 2) {
            writer.writeError("ERR wrong number of arguments for 'smembers' command");
            return;
        }
        Set<String> members = store.smembers(args.get(1).getString());
        writer.writeArray(members.toArray(new String[0]));
    }
}

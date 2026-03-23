package com.redisclone.command.commands;

import com.redisclone.command.Command;
import com.redisclone.core.RedisStore;
import com.redisclone.protocol.RespParser;
import com.redisclone.protocol.RespWriter;

import java.util.List;

public class ZScoreCommand implements Command {
    private final RedisStore store;

    public ZScoreCommand(RedisStore store) {
        this.store = store;
    }

    @Override
    public void execute(List<RespParser.RespValue> args, RespWriter writer) {
        if (args.size() != 3) {
            writer.writeError("ERR wrong number of arguments for 'zscore' command");
            return;
        }
        Double score = store.zscore(args.get(1).getString(), args.get(2).getString());
        if (score == null) writer.writeNull();
        else writer.writeBulkString(score.toString());
    }
}

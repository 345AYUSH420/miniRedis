package com.redisclone.command.commands;

import com.redisclone.command.Command;
import com.redisclone.core.RedisStore;
import com.redisclone.protocol.RespParser;
import com.redisclone.protocol.RespWriter;

import java.util.List;
import java.util.Map;

public class HGetAllCommand implements Command {
    private final RedisStore store;

    public HGetAllCommand(RedisStore store) {
        this.store = store;
    }

    @Override
    public void execute(List<RespParser.RespValue> args, RespWriter writer) {
        if (args.size() != 2) {
            writer.writeError("ERR wrong number of arguments for 'hgetall' command");
            return;
        }
        Map<String, String> hash = store.hgetall(args.get(1).getString());
        String[] result = new String[hash.size() * 2];
        int i = 0;
        for (Map.Entry<String, String> entry : hash.entrySet()) {
            result[i++] = entry.getKey();
            result[i++] = entry.getValue();
        }
        writer.writeArray(result);
    }
}

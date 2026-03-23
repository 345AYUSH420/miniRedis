package com.redisclone.command.commands;

import com.redisclone.command.Command;
import com.redisclone.protocol.RespParser;
import com.redisclone.protocol.RespWriter;

import java.util.List;

public class PingCommand implements Command {
    @Override
    public void execute(List<RespParser.RespValue> args, RespWriter writer) {
        if (args.size() == 1) {
            writer.writeSimpleString("PONG");
        } else if (args.size() == 2) {
            writer.writeBulkString(args.get(1).getString());
        } else {
            writer.writeError("ERR wrong number of arguments for 'ping' command");
        }
    }
}

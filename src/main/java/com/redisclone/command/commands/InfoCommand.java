package com.redisclone.command.commands;

import com.redisclone.command.Command;
import com.redisclone.protocol.RespParser;
import com.redisclone.protocol.RespWriter;

import java.util.List;

public class InfoCommand implements Command {
    @Override
    public void execute(List<RespParser.RespValue> args, RespWriter writer) {
        StringBuilder info = new StringBuilder();
        info.append("# Server\n");
        info.append("redis_version:1.0.0\n");
        info.append("redis_mode:standalone\n");
        info.append("os:Java\n");
        info.append("arch_bits:64\n");
        info.append("uptime_in_seconds:").append(System.currentTimeMillis() / 1000).append("\n");
        info.append("\n# Memory\n");
        info.append("used_memory:").append(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()).append("\n");
        info.append("used_memory_human:").append((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024).append("M\n");
        info.append("\n# Stats\n");
        info.append("total_commands_processed:0\n");
        info.append("keyspace_hits:0\n");
        info.append("keyspace_misses:0\n");
        writer.writeBulkString(info.toString());
    }
}

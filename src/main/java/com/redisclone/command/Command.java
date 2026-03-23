package com.redisclone.command;

import com.redisclone.protocol.RespParser;
import com.redisclone.protocol.RespWriter;

import java.util.List;

/**
 * Command Pattern interface.
 * Each Redis command implements this interface and provides its own execute logic.
 */
public interface Command {
    void execute(List<RespParser.RespValue> args, RespWriter writer);
}

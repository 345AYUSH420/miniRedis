package com.redisclone.protocol;

import java.io.*;
public class RespWriter {
    private final PrintWriter writer;
    
    public RespWriter(OutputStream outputStream) {
        this.writer = new PrintWriter(new OutputStreamWriter(outputStream), true);
    }
    
    public void writeSimpleString(String value) {
        writer.println("+" + value);
    }
    
    public void writeError(String error) {
        writer.println("-" + error);
    }
    
    public void writeInteger(long value) {
        writer.println(":" + value);
    }
    
    public void writeBulkString(String value) {
        if (value == null) {
            writer.println("$-1");
        } else {
            writer.println("$" + value.length());
            writer.println(value);
        }
    }
    
    public void writeArray(Object[] values) {
        writer.println("*" + values.length);
        for (Object value : values) {
            if (value == null) {
                writeBulkString(null);
            } else if (value instanceof String) {
                writeBulkString((String) value);
            } else if (value instanceof Long) {
                writeInteger((Long) value);
            } else if (value instanceof Integer) {
                writeInteger(((Integer) value).longValue());
            } else {
                writeBulkString(value.toString());
            }
        }
    }
    
    public void writeArray(String[] values) {
        writer.println("*" + values.length);
        for (String value : values) {
            writeBulkString(value);
        }
    }
    
    public void writeOk() {
        writeSimpleString("OK");
    }
    
    public void writeNull() {
        writeBulkString(null);
    }
    
    public void writeZero() {
        writeInteger(0);
    }
    
    public void writeOne() {
        writeInteger(1);
    }
    
    public void flush() {
        writer.flush();
    }
    
    public void close() {
        writer.close();
    }
}

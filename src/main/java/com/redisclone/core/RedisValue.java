package com.redisclone.core;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
public class RedisValue {
    private final DataType type;
    private final Object value;
    private Instant expiresAt;
    
    public enum DataType {
        STRING, HASH, LIST, SET, SORTED_SET
    }
    
    public RedisValue(DataType type, Object value) {
        this.type = type;
        this.value = value;
    }
    
    public RedisValue(DataType type, Object value, long ttlSeconds) {
        this.type = type;
        this.value = value;
        this.expiresAt = Instant.now().plusSeconds(ttlSeconds);
    }
    
    public DataType getType() {
        return type;
    }
    
    public Object getValue() {
        return value;
    }
    
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }
    
    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
    
    public Instant getExpiresAt() {
        return expiresAt;
    }
    
    public void setTTL(long seconds) {
        this.expiresAt = Instant.now().plusSeconds(seconds);
    }
    
    public long getTTL() {
        if (expiresAt == null) {
            return -1; // No expiration
        }
        long remaining = expiresAt.getEpochSecond() - Instant.now().getEpochSecond();
        return Math.max(0, remaining);
    }
    @SuppressWarnings("unchecked")
    public String getStringValue() {
        if (type != DataType.STRING) {
            throw new ClassCastException("Value is not a string");
        }
        return (String) value;
    }
    
    public ConcurrentHashMap<String, String> getHashValue() {
        if (type != DataType.HASH) {
            throw new ClassCastException("Value is not a hash");
        }
        return (ConcurrentHashMap<String, String>) value;
    }
    
    @SuppressWarnings("unchecked")
    public CopyOnWriteArrayList<String> getListValue() {
        if (type != DataType.LIST) {
            throw new ClassCastException("Value is not a list");
        }
        return (CopyOnWriteArrayList<String>) value;
    }
    
    @SuppressWarnings("unchecked")
    public CopyOnWriteArraySet<String> getSetValue() {
        if (type != DataType.SET) {
            throw new ClassCastException("Value is not a set");
        }
        return (CopyOnWriteArraySet<String>) value;
    }
    
    @SuppressWarnings("unchecked")
    public ConcurrentSkipListSet<ScoredMember> getSortedSetValue() {
        if (type != DataType.SORTED_SET) {
            throw new ClassCastException("Value is not a sorted set");
        }
        return (ConcurrentSkipListSet<ScoredMember>) value;
    }
    
    public static class ScoredMember implements Comparable<ScoredMember> {
        private final double score;
        private final String member;
        
        public ScoredMember(double score, String member) {
            this.score = score;
            this.member = member;
        }
        
        public double getScore() {
            return score;
        }
        
        public String getMember() {
            return member;
        }
        
        @Override
        public int compareTo(ScoredMember other) {
            int scoreCompare = Double.compare(this.score, other.score);
            if (scoreCompare != 0) {
                return scoreCompare;
            }
            return this.member.compareTo(other.member);
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            ScoredMember that = (ScoredMember) obj;
            return Double.compare(that.score, score) == 0 && member.equals(that.member);
        }
        
        @Override
        public int hashCode() {
            return member.hashCode();
        }
    }
}

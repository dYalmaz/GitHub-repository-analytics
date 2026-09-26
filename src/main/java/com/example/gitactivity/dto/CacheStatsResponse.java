package com.example.gitactivity.dto;

public class CacheStatsResponse {

    private long hits;
    private long misses;

    public CacheStatsResponse() {
    }

    public long getHits() {
        return hits;
    }

    public void setHits(long hits) {
        this.hits = hits;
    }

    public long getMisses() {
        return misses;
    }

    public void setMisses(long misses) {
        this.misses = misses;
    }

}

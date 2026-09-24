package com.example.gitactivity.service;

public final class ContributorCacheKey {

    private static final String PREFIX = "contributors:";
    public static final long TTL_MINUTES = 10;

    private ContributorCacheKey() {
        // private constructor to prevent instantiation
    }

    public static String create(String owner, String repo) {
        return PREFIX + owner + ":" + repo;
    }

}

package com.example.gitactivity.service;

public final class RepositoryCacheKey {

    private static final String PREFIX = "repository:";
    public static final long TTL_MINUTES = 10;

    private RepositoryCacheKey() {
        // private constructor to prevent instantiation
    }

    public static String create(String owner, String repo) {
        return PREFIX + owner + ":" + repo;
    }


}

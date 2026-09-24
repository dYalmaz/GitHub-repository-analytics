package com.example.gitactivity.service;

public class RepositoryCacheKey {

    private static final String PREFIX = "repository:";

    private RepositoryCacheKey() {
        // private constructor to prevent instantiation
    }

    public static String create(String owner, String repo) {
        return PREFIX + owner + ":" + repo;
    }


}

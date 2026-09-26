package com.example.gitactivity.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public class GitHubRepositoryResponse {

    private String name;
    private String description;

    @JsonAlias("stargazers_count")
    private int stars;

    @JsonAlias("forks_count")
    private int forks;

    private String language;

    // Getters and setters

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getStars() {
        return stars;
    }

    public void setStars(int stars) {
        this.stars = stars;
    }

    public int getForks() {
        return forks;
    }

    public void setForks(int forks) {
        this.forks = forks;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

}

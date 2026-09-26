package com.example.gitactivity.dto;

public class RepositoryAnalyticsResponse {

    private String repository;
    private int stars;
    private int forks;
    private double forkToStarRatio;

    public String getRepository() {
        return repository;
    }
    public void setRepository(String repository) {
        this.repository = repository;
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

    public double getForkToStarRatio() {
        return forkToStarRatio;
    }
    public void setForkToStarRatio(double forkToStarRatio) {
        this.forkToStarRatio = forkToStarRatio;
    }

}

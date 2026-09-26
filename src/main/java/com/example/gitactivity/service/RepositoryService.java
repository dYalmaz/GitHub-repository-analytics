package com.example.gitactivity.service;

import com.example.gitactivity.client.GitHubClient;
import com.example.gitactivity.dto.CacheStatsResponse;
import com.example.gitactivity.dto.GitHubContributorResponse;
import com.example.gitactivity.dto.GitHubRepositoryResponse;
import com.example.gitactivity.dto.RepositoryAnalyticsResponse;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class RepositoryService {

    private final GitHubClient gitClient;
    private final RedisTemplate<String, Object> redisTemplate;
    private AtomicLong cacheHits = new AtomicLong(0);
    private AtomicLong cacheMisses = new AtomicLong(0);


    public RepositoryService(GitHubClient gitClient, RedisTemplate<String, Object> redisTemplate) {
        this.gitClient = gitClient;
        this.redisTemplate = redisTemplate;

    }

    public GitHubRepositoryResponse getRepoDetails(String owner, String repo) {

        String cacheKey = RepositoryCacheKey.create(owner, repo);

        Object cachedValue = redisTemplate.opsForValue().get(cacheKey);

        if (cachedValue != null) {
            cacheHits.incrementAndGet();
            return (GitHubRepositoryResponse) cachedValue;
        }

        cacheMisses.incrementAndGet();

        GitHubRepositoryResponse repositoryResponse = gitClient.getRepository(owner, repo);

        redisTemplate.opsForValue().set(
                cacheKey,
                repositoryResponse,
                RepositoryCacheKey.TTL_MINUTES,
                TimeUnit.MINUTES
        );

        return repositoryResponse;
    }

    public GitHubContributorResponse[] getContributors(String owner, String repo) {

        String cacheKey = ContributorCacheKey.create(owner, repo);

        GitHubContributorResponse[] cached = (GitHubContributorResponse[]) redisTemplate.opsForValue().get(cacheKey);

        if (cached != null) {
            cacheHits.incrementAndGet();
            return cached;
        }

        cacheMisses.incrementAndGet();

        GitHubContributorResponse[] response = gitClient.getContributors(owner, repo);

        redisTemplate.opsForValue().set(
                cacheKey,
                response,
                ContributorCacheKey.TTL_MINUTES,
                TimeUnit.MINUTES
        );

        return response;
    }

    public RepositoryAnalyticsResponse getRepositoryAnalytics(String owner, String repo) {

        GitHubRepositoryResponse repository = getRepoDetails(owner, repo);

        RepositoryAnalyticsResponse analytics = new RepositoryAnalyticsResponse();

        analytics.setRepository(repository.getName());
        analytics.setForks(repository.getForks());
        analytics.setStars(repository.getStars());

        double forkToStarRatio = repository.getStars() == 0 ? 0 : (double) repository.getForks() / repository.getStars();
        analytics.setForkToStarRatio(forkToStarRatio);

        return analytics;

    }

    public CacheStatsResponse getCacheStats() {

        CacheStatsResponse cacheStats = new CacheStatsResponse();
        cacheStats.setHits(cacheHits.get());
        cacheStats.setMisses(cacheMisses.get());

        return cacheStats;
    }




}

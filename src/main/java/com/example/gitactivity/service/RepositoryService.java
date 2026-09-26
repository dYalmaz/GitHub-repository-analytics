package com.example.gitactivity.service;

import com.example.gitactivity.client.GitHubClient;
import com.example.gitactivity.dto.CacheStatsResponse;
import com.example.gitactivity.dto.GitHubContributorResponse;
import com.example.gitactivity.dto.GitHubRepositoryResponse;
import com.example.gitactivity.dto.RepositoryAnalyticsResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

import java.util.concurrent.atomic.AtomicLong;

@Service
public class RepositoryService {

    private final GitHubClient gitClient;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final AtomicLong cacheHits = new AtomicLong();
    private final AtomicLong cacheMisses = new AtomicLong();


    public RepositoryService(GitHubClient gitClient, StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.gitClient = gitClient;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public GitHubRepositoryResponse getRepoDetails(String owner, String repo) {

        String cacheKey = RepositoryCacheKey.create(owner, repo);

        String cachedJson =
                redisTemplate.opsForValue().get(cacheKey);

        if (cachedJson != null) {
            try {
                GitHubRepositoryResponse cachedRepository =
                        objectMapper.readValue(
                                cachedJson,
                                GitHubRepositoryResponse.class
                        );

                cacheHits.incrementAndGet();

                return cachedRepository;

            } catch (Exception e) {
                redisTemplate.delete(cacheKey);
            }
        }

        cacheMisses.incrementAndGet();

        GitHubRepositoryResponse repositoryResponse = gitClient.getRepository(owner, repo);

        String json =
                objectMapper.writeValueAsString(repositoryResponse);

        redisTemplate.opsForValue().set(
                cacheKey,
                json,
                Duration.ofMinutes(RepositoryCacheKey.TTL_MINUTES)
        );

        return repositoryResponse;
    }

    public GitHubContributorResponse[] getContributors(String owner, String repo) {

        String cacheKey = ContributorCacheKey.create(owner, repo);

        String cached =
                redisTemplate.opsForValue().get(cacheKey);

        if (cached != null) {
            try {
                GitHubContributorResponse[] cachedContributors =
                        objectMapper.readValue(
                                cached,
                                GitHubContributorResponse[].class
                        );

                cacheHits.incrementAndGet();

                return cachedContributors;

            } catch (Exception e) {
                redisTemplate.delete(cacheKey);
            }
        }

        cacheMisses.incrementAndGet();

        GitHubContributorResponse[] response = gitClient.getContributors(owner, repo);

        String json =
                objectMapper.writeValueAsString(response);

        redisTemplate.opsForValue().set(
                cacheKey,
                json,
                Duration.ofMinutes(ContributorCacheKey.TTL_MINUTES)
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

    public long getCacheHits() {
        return cacheHits.get();
    }

    public long getCacheMisses() {
        return cacheMisses.get();
    }




}

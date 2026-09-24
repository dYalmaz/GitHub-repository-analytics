package com.example.gitactivity.service;

import com.example.gitactivity.client.GitHubClient;
import com.example.gitactivity.dto.GitHubRepositoryResponse;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

import static java.lang.reflect.Array.set;

@Service
public class RepositoryService {

    private final GitHubClient gitClient;
    private final RedisTemplate<String, Object> redisTemplate;

    public RepositoryService(GitHubClient gitClient, RedisTemplate<String, Object> redisTemplate) {
        this.gitClient = gitClient;
        this.redisTemplate = redisTemplate;

    }

    public GitHubRepositoryResponse getRepoDetails(String owner, String repo) {

        String cacheKey = RepositoryCacheKey.create(owner, repo);

        Object cachedValue = redisTemplate.opsForValue().get(cacheKey);

        if (cachedValue != null) {
            return (GitHubRepositoryResponse) cachedValue;
        }

        GitHubRepositoryResponse repositoryResponse = gitClient.getRepository(owner, repo);

        redisTemplate.opsForValue().set(
                cacheKey,
                repositoryResponse,
                RepositoryCacheKey.TTL_MINUTES,
                TimeUnit.MINUTES
        );

        return repositoryResponse;
    }

}

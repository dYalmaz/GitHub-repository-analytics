package com.example.gitactivity.service;

import com.example.gitactivity.client.GitHubClient;
import com.example.gitactivity.dto.GitHubRepositoryResponse;
import com.example.gitactivity.exception.GitHubRateLimitException;
import com.example.gitactivity.exception.GitHubServiceException;
import com.example.gitactivity.exception.RepositoryNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RepositoryServiceTest {

    @Mock
    private GitHubClient gitHubClient;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private RepositoryService repositoryService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
    }

    @Test
    void shouldReturnRepositoryFromGitHubClient() {

        GitHubRepositoryResponse repository = new GitHubRepositoryResponse();

        when(gitHubClient.getRepository("spring-projects", "spring-boot")).thenReturn(repository);

        GitHubRepositoryResponse result = repositoryService.getRepoDetails("spring-projects", "spring-boot");

        assertSame(repository, result);

        verify(gitHubClient).getRepository("spring-projects", "spring-boot");

    }

    @Test
    void shouldPropagateRepositoryNotFoundException(){

        RepositoryNotFoundException exception = new RepositoryNotFoundException("Repository not found: spring-projects/non-existent");

        when(gitHubClient.getRepository("spring-projects", "non-existent")).thenThrow(exception);

        RepositoryNotFoundException thrown = assertThrows(
                RepositoryNotFoundException.class,
                () -> repositoryService.getRepoDetails("spring-projects", "non-existent")
        );

        assertSame(exception, thrown);

        verify(gitHubClient).getRepository("spring-projects", "non-existent");

    }

    @Test
    void shouldPropagateRateLimitExceededException(){

        GitHubRateLimitException exception = new GitHubRateLimitException("GitHub API rate limit exceeded");

        when(gitHubClient.getRepository("spring-projects", "spring-boot")).thenThrow(exception);

        GitHubRateLimitException thrown = assertThrows(
                GitHubRateLimitException.class,
                () -> repositoryService.getRepoDetails("spring-projects", "spring-boot")
        );

        assertSame(exception, thrown);

        verify(gitHubClient).getRepository("spring-projects", "spring-boot");
    }

    @Test
    void shouldPropagateServiceUnavailableException(){

        GitHubServiceException exception = new GitHubServiceException("GitHub API service unavailable");

        when(gitHubClient.getRepository("spring-projects", "spring-boot")).thenThrow(exception);

        GitHubServiceException thrown = assertThrows(
                GitHubServiceException.class,
                () -> repositoryService.getRepoDetails("spring-projects", "spring-boot")
        );

        assertSame(exception, thrown);

        verify(gitHubClient).getRepository("spring-projects", "spring-boot");
    }

    @Test
    void shouldFetchFromGitHubAndCacheResponse(){

        String owner="spring-projects";
        String repo="spring-boot";

        GitHubRepositoryResponse response = new GitHubRepositoryResponse();

        when(valueOperations.get(anyString())).thenReturn(null);
        when(gitHubClient.getRepository(owner, repo)).thenReturn(response);

        GitHubRepositoryResponse result = repositoryService.getRepoDetails(owner, repo);

        assertSame(response, result);

        verify(gitHubClient).getRepository(owner, repo);

        verify(valueOperations).set(
                eq(RepositoryCacheKey.create(owner, repo)),
                eq(response),
                eq(RepositoryCacheKey.TTL_MINUTES),
                eq(TimeUnit.MINUTES)
        );

    }

    @Test
    void shouldReturnCachedResponseWithoutCallingGitHub(){

        String owner="spring-projects";
        String repo="spring-boot";

        GitHubRepositoryResponse cachedResponse = new GitHubRepositoryResponse();

        when(valueOperations.get(RepositoryCacheKey.create(owner, repo))).thenReturn(cachedResponse);

        GitHubRepositoryResponse result = repositoryService.getRepoDetails(owner, repo);

        assertSame(cachedResponse, result);

        verify(gitHubClient, never()).getRepository(owner, repo);

    }

    @Test
    void shouldCallGitHubOnCacheMiss(){

        String owner="spring-projects";
        String repo="spring-boot";

        GitHubRepositoryResponse response = new GitHubRepositoryResponse();

        when(valueOperations.get(RepositoryCacheKey.create(owner, repo))).thenReturn(null);
        when(gitHubClient.getRepository(owner, repo)).thenReturn(response);

        GitHubRepositoryResponse result = repositoryService.getRepoDetails(owner, repo);

        assertSame(response, result);

        verify(gitHubClient, times(1)).getRepository(owner, repo);

    }




}


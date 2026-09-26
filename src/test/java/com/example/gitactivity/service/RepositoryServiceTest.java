package com.example.gitactivity.service;

import com.example.gitactivity.client.GitHubClient;
import com.example.gitactivity.dto.GitHubContributorResponse;
import com.example.gitactivity.dto.GitHubRepositoryResponse;
import com.example.gitactivity.dto.RepositoryAnalyticsResponse;
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

import static org.junit.jupiter.api.Assertions.*;
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

    private void setupRedisCache() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
    }

    @Test
    void shouldReturnRepositoryFromGitHubClient() {

        setupRedisCache();

        GitHubRepositoryResponse repository = new GitHubRepositoryResponse();

        when(gitHubClient.getRepository("spring-projects", "spring-boot")).thenReturn(repository);

        GitHubRepositoryResponse result = repositoryService.getRepoDetails("spring-projects", "spring-boot");

        assertSame(repository, result);

        verify(gitHubClient).getRepository("spring-projects", "spring-boot");

    }

    @Test
    void shouldPropagateRepositoryNotFoundException(){

        setupRedisCache();

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

        setupRedisCache();

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

        setupRedisCache();

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

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

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

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

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
        setupRedisCache();

        String owner="spring-projects";
        String repo="spring-boot";

        GitHubRepositoryResponse response = new GitHubRepositoryResponse();

        when(gitHubClient.getRepository(owner, repo)).thenReturn(response);

        GitHubRepositoryResponse result = repositoryService.getRepoDetails(owner, repo);

        assertSame(response, result);

        verify(gitHubClient, times(1)).getRepository(owner, repo);

    }

    @Test
    void shouldReturnContributorsFromGitHubClient() {

        setupRedisCache();

        GitHubContributorResponse contributor = new GitHubContributorResponse();

        GitHubContributorResponse[] expected = {contributor};

        when(gitHubClient.getContributors("spring-projects", "spring-boot")).thenReturn(expected);

        GitHubContributorResponse[] actual = repositoryService.getContributors("spring-projects", "spring-boot");

        assertSame(expected, actual);

        verify(gitHubClient).getContributors("spring-projects", "spring-boot");

    }

    @Test
    void shouldCallGitHubOnContributorsCacheMiss() {
        setupRedisCache();

        String owner = "spring-projects";
        String repo = "spring-boot";

        GitHubContributorResponse contributor = new GitHubContributorResponse();
        GitHubContributorResponse[] expected = {contributor};

        when(gitHubClient.getContributors(owner, repo)).thenReturn(expected);

        GitHubContributorResponse[] actual = repositoryService.getContributors(owner, repo);

        assertSame(expected, actual);

        verify(gitHubClient).getContributors(owner, repo);
    }

    @Test
    void shouldReturnContributorsFromCache() {

        String owner = "spring-projects";
        String repo = "spring-boot";

        GitHubContributorResponse contributor = new GitHubContributorResponse();

        GitHubContributorResponse[] cached = {contributor};

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        when(valueOperations.get(ContributorCacheKey.create(owner, repo))).thenReturn(cached);

        GitHubContributorResponse[] actual = repositoryService.getContributors(owner, repo);

        assertSame(cached, actual);

        verify(gitHubClient, never()).getContributors(owner, repo);
    }

    @Test
    void shouldStoreContributorsInCacheAfterCacheMiss() {

        setupRedisCache();

        String owner = "spring-projects";
        String repo = "spring-boot";

        GitHubContributorResponse contributor = new GitHubContributorResponse();
        GitHubContributorResponse[] expected = {contributor};

        String cacheKey = ContributorCacheKey.create(owner, repo);

        when(gitHubClient.getContributors(owner, repo)).thenReturn(expected);

        repositoryService.getContributors(owner, repo);

        verify(valueOperations).set(
                cacheKey,
                expected,
                ContributorCacheKey.TTL_MINUTES,
                TimeUnit.MINUTES
        );

    }

    @Test
    void shouldStoreContributorsWithCorrectTtl(){

        setupRedisCache();

        String owner = "spring-projects";
        String repo = "spring-boot";

        GitHubContributorResponse contributor = new GitHubContributorResponse();
        GitHubContributorResponse[] expected = {contributor};

        String cacheKey = ContributorCacheKey.create(owner, repo);

        when(gitHubClient.getContributors(owner, repo)).thenReturn(expected);

        repositoryService.getContributors(owner, repo);

        verify(valueOperations).set(
                cacheKey,
                expected,
                10,
                TimeUnit.MINUTES
        );

    }

    @Test
    void shouldCalculateRepositoryAnalytics(){

        String owner="spring-projects";
        String repo="spring-boot";

        GitHubRepositoryResponse repositoryResponse = new GitHubRepositoryResponse();

        repositoryResponse.setName(repo);
        repositoryResponse.setStars(100);
        repositoryResponse.setForks(25);

        when(gitHubClient.getRepository(owner, repo)).thenReturn(repositoryResponse);

        setupRedisCache();

        RepositoryAnalyticsResponse actual = repositoryService.getRepositoryAnalytics(owner, repo);

        assertEquals("spring-boot", actual.getRepository());
        assertEquals(100, actual.getStars());
        assertEquals(25, actual.getForks());
        assertEquals(0.25, actual.getForkToStarRatio());

    }

    @Test
    void shouldReturnZeroForkToStarRatioWhenStarsAreZero(){

        String owner="spring-projects";
        String repo="spring-boot";

        GitHubRepositoryResponse repositoryResponse = new GitHubRepositoryResponse();

        repositoryResponse.setName(repo);
        repositoryResponse.setStars(0);
        repositoryResponse.setForks(25);

        when(gitHubClient.getRepository(owner, repo)).thenReturn(repositoryResponse);

        setupRedisCache();

        RepositoryAnalyticsResponse actual = repositoryService.getRepositoryAnalytics(owner, repo);

        assertEquals(0.0, actual.getForkToStarRatio());

    }

    @Test
    void shouldCalculateAnalyticsFromCachedRepository(){

        String owner="spring-projects";
        String repo="spring-boot";

        GitHubRepositoryResponse cachedRepository = new GitHubRepositoryResponse();

        cachedRepository.setName(repo);
        cachedRepository.setStars(200);
        cachedRepository.setForks(50);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        when(valueOperations.get(RepositoryCacheKey.create(owner, repo))).thenReturn(cachedRepository);

        RepositoryAnalyticsResponse actual = repositoryService.getRepositoryAnalytics(owner, repo);

        assertEquals("spring-boot", actual.getRepository());
        assertEquals(200, actual.getStars());
        assertEquals(50, actual.getForks());
        assertEquals(0.25, actual.getForkToStarRatio());

        verify(valueOperations).get(RepositoryCacheKey.create(owner, repo));

        verify(gitHubClient, never()).getRepository(owner, repo);

    }

    @Test
    void shouldFetchRepositoryFromGitHubIfNotCachedWhenCalculatingAnalyticsOnCacheMiss(){

        String owner="spring-projects";
        String repo="spring-boot";

        GitHubRepositoryResponse repositoryResponse = new GitHubRepositoryResponse();

        repositoryResponse.setName(repo);
        repositoryResponse.setStars(150);
        repositoryResponse.setForks(30);

        setupRedisCache();

        when(gitHubClient.getRepository(owner, repo)).thenReturn(repositoryResponse);

        RepositoryAnalyticsResponse actual = repositoryService.getRepositoryAnalytics(owner, repo);

        assertEquals("spring-boot", actual.getRepository());
        assertEquals(150, actual.getStars());
        assertEquals(30, actual.getForks());
        assertEquals(0.2, actual.getForkToStarRatio());

        verify(gitHubClient).getRepository(owner, repo);

        verify(valueOperations).set(
                RepositoryCacheKey.create(owner, repo),
                repositoryResponse,
                RepositoryCacheKey.TTL_MINUTES,
                TimeUnit.MINUTES
        );

    }

}


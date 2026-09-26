package com.example.gitactivity.service;

import com.example.gitactivity.client.GitHubClient;
import com.example.gitactivity.dto.CacheStatsResponse;
import com.example.gitactivity.dto.GitHubContributorResponse;
import com.example.gitactivity.dto.GitHubRepositoryResponse;
import com.example.gitactivity.dto.RepositoryAnalyticsResponse;
import com.example.gitactivity.exception.GitHubRateLimitException;
import com.example.gitactivity.exception.GitHubServiceException;
import com.example.gitactivity.exception.RepositoryNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RepositoryServiceTest {

    @Mock
    private GitHubClient gitHubClient;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private ObjectMapper objectMapper;
    private RepositoryService repositoryService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        repositoryService = new RepositoryService(
                gitHubClient,
                redisTemplate,
                objectMapper
        );
    }



    private void setupRedisCache() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
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

        when(valueOperations.get(RepositoryCacheKey.create(owner, repo))).thenReturn(null);
        when(gitHubClient.getRepository(owner, repo)).thenReturn(response);

        GitHubRepositoryResponse result = repositoryService.getRepoDetails(owner, repo);

        assertSame(response, result);

        verify(valueOperations).get(RepositoryCacheKey.create(owner, repo));

        verify(gitHubClient).getRepository(owner, repo);

        verify(valueOperations).set(
                eq(RepositoryCacheKey.create(owner, repo)),
                anyString(),
                eq(Duration.ofMinutes(RepositoryCacheKey.TTL_MINUTES))
        );

    }

    @Test
    void shouldReturnCachedResponseWithoutCallingGitHub(){

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        String owner="spring-projects";
        String repo="spring-boot";

        String cachedJson =
                "{\"name\":\"spring-boot\",\"stars\":100,\"forks\":25}";

        when(valueOperations.get(RepositoryCacheKey.create(owner, repo)))
                .thenReturn(cachedJson);

        GitHubRepositoryResponse result = repositoryService.getRepoDetails(owner, repo);

        assertEquals("spring-boot", result.getName());
        assertEquals(100, result.getStars());
        assertEquals(25, result.getForks());

        verify(valueOperations).get(RepositoryCacheKey.create(owner, repo));

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

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        String cachedJson =
                "[{\"login\":\"test-user\",\"avatar_url\":\"avatar.png\",\"contributions\":10,\"html_url\":\"github.com/test-user\"}]";

        when(valueOperations.get(
                ContributorCacheKey.create(owner, repo)
        )).thenReturn(cachedJson);

        GitHubContributorResponse[] actual =
                repositoryService.getContributors(owner, repo);

        assertEquals(1, actual.length);
        assertEquals("test-user", actual[0].getLogin());
        assertEquals(10, actual[0].getContributions());

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
                eq(cacheKey),
                anyString(),
                eq(Duration.ofMinutes(ContributorCacheKey.TTL_MINUTES))
        );

    }

    @Test
    void ShouldFetchFromGitHubWhenCachedRepositoryHasExpired(){

        String owner="spring-projects";
        String repo="spring-boot";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        GitHubRepositoryResponse response = new GitHubRepositoryResponse();

        when(valueOperations.get(RepositoryCacheKey.create(owner, repo))).thenReturn(null);

        when(gitHubClient.getRepository(owner, repo)).thenReturn(response);

        GitHubRepositoryResponse result = repositoryService.getRepoDetails(owner, repo);

        assertSame(response, result);

        verify(valueOperations).get(RepositoryCacheKey.create(owner, repo));

        verify(gitHubClient).getRepository(owner, repo);

        verify(valueOperations).set(
                eq(RepositoryCacheKey.create(owner, repo)),
                anyString(),
                eq(Duration.ofMinutes(RepositoryCacheKey.TTL_MINUTES))
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

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        String cachedJson =
                "{\"name\":\"spring-boot\",\"stars\":200,\"forks\":50}";

        when(valueOperations.get(RepositoryCacheKey.create(owner, repo)))
                .thenReturn(cachedJson);

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
                eq(RepositoryCacheKey.create(owner, repo)),
                anyString(),
                eq(Duration.ofMinutes(RepositoryCacheKey.TTL_MINUTES))
        );

    }

    @Test
    void shouldReturnCacheStats() {


        // Generate one cache hit
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        GitHubRepositoryResponse cachedRepository =
                new GitHubRepositoryResponse();

        String cachedJson =
                "{\"name\":\"spring-boot\",\"stars\":200,\"forks\":50}";

        when(valueOperations.get(
                RepositoryCacheKey.create("spring-projects", "spring-boot")
        )).thenReturn(cachedJson);

        repositoryService.getRepoDetails(
                "spring-projects",
                "spring-boot"
        );

        // Generate one cache miss
        when(valueOperations.get(
                RepositoryCacheKey.create("test-owner", "test-repo")
        )).thenReturn(null);

        GitHubRepositoryResponse repository =
                new GitHubRepositoryResponse();

        when(gitHubClient.getRepository("test-owner", "test-repo"))
                .thenReturn(repository);

        repositoryService.getRepoDetails(
                "test-owner",
                "test-repo"
        );

        CacheStatsResponse stats =
                repositoryService.getCacheStats();

        assertEquals(1, stats.getHits());
        assertEquals(1, stats.getMisses());
    }

    @Test
    void shouldTrackContributorCacheStats() {

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        String owner = "spring-projects";
        String repo = "spring-boot";

        GitHubContributorResponse contributor =
                new GitHubContributorResponse();

        GitHubContributorResponse[] contributors = {contributor};

        // Cache miss
        when(valueOperations.get(
                ContributorCacheKey.create(owner, repo)
        )).thenReturn(null);

        when(gitHubClient.getContributors(owner, repo))
                .thenReturn(contributors);

        repositoryService.getContributors(owner, repo);

        // Cache hit
        String cachedJson =
                "[{\"login\":\"test-user\",\"avatar_url\":\"avatar.png\",\"contributions\":10,\"html_url\":\"github.com/test-user\"}]";

        when(valueOperations.get(
                ContributorCacheKey.create(owner, repo)
        )).thenReturn(cachedJson);

        repositoryService.getContributors(owner, repo);

        CacheStatsResponse stats =
                repositoryService.getCacheStats();

        assertEquals(1, stats.getHits());
        assertEquals(1, stats.getMisses());
    }

    @Test
    void shouldReturnZeroCacheStatsInitially() {

        CacheStatsResponse stats = repositoryService.getCacheStats();

        assertEquals(0, stats.getHits());
        assertEquals(0, stats.getMisses());
    }

    @Test
    void shouldStoreRepositoryAsJsonInCache() {

        setupRedisCache();

        String owner = "spring-projects";
        String repo = "spring-boot";

        GitHubRepositoryResponse response =
                new GitHubRepositoryResponse();

        response.setName(repo);
        response.setStars(100);
        response.setForks(25);

        when(gitHubClient.getRepository(owner, repo))
                .thenReturn(response);

        repositoryService.getRepoDetails(owner, repo);

        verify(valueOperations).set(
                eq(RepositoryCacheKey.create(owner, repo)),
                anyString(),
                eq(Duration.ofMinutes(RepositoryCacheKey.TTL_MINUTES))
        );
    }

    @Test
    void shouldStoreContributorsAsJsonInCache() {

        setupRedisCache();

        String owner = "spring-projects";
        String repo = "spring-boot";

        GitHubContributorResponse contributor =
                new GitHubContributorResponse();

        contributor.setLogin("test-user");
        contributor.setContributions(10);

        GitHubContributorResponse[] contributors =
                {contributor};

        when(gitHubClient.getContributors(owner, repo))
                .thenReturn(contributors);

        repositoryService.getContributors(owner, repo);

        verify(valueOperations).set(
                eq(ContributorCacheKey.create(owner, repo)),
                anyString(),
                eq(Duration.ofMinutes(ContributorCacheKey.TTL_MINUTES))
        );
    }

    @Test
    void shouldFetchFromGitHubWhenCachedRepositoryJsonIsInvalid() {

        setupRedisCache();

        String owner = "spring-projects";
        String repo = "spring-boot";

        GitHubRepositoryResponse expected =
                new GitHubRepositoryResponse();

        when(valueOperations.get(
                RepositoryCacheKey.create(owner, repo)
        )).thenReturn("not-valid-json");

        when(gitHubClient.getRepository(owner, repo))
                .thenReturn(expected);

        GitHubRepositoryResponse result =
                repositoryService.getRepoDetails(owner, repo);

        assertSame(expected, result);

        verify(gitHubClient).getRepository(owner, repo);

        verify(redisTemplate).delete(
                RepositoryCacheKey.create(owner, repo)
        );
    }

    @Test
    void shouldCountInvalidCachedJsonAsCacheMiss() {

        setupRedisCache();

        String owner = "spring-projects";
        String repo = "spring-boot";

        GitHubRepositoryResponse expected =
                new GitHubRepositoryResponse();

        when(valueOperations.get(
                RepositoryCacheKey.create(owner, repo)
        )).thenReturn("not-valid-json");

        when(gitHubClient.getRepository(owner, repo))
                .thenReturn(expected);

        repositoryService.getRepoDetails(owner, repo);

        assertEquals(0, repositoryService.getCacheHits());
        assertEquals(1, repositoryService.getCacheMisses());
    }

    @Test
    void shouldFetchContributorsWhenCachedJsonIsInvalid() {

        setupRedisCache();

        String owner = "spring-projects";
        String repo = "spring-boot";

        String cacheKey = ContributorCacheKey.create(owner, repo);

        when(valueOperations.get(cacheKey))
                .thenReturn("invalid-json");

        GitHubContributorResponse contributor =
                new GitHubContributorResponse();

        contributor.setLogin("test-user");
        contributor.setContributions(10);

        GitHubContributorResponse[] expected =
                new GitHubContributorResponse[]{contributor};

        when(gitHubClient.getContributors(owner, repo))
                .thenReturn(expected);

        GitHubContributorResponse[] result =
                repositoryService.getContributors(owner, repo);

        assertSame(expected, result);

        verify(redisTemplate).delete(cacheKey);

        verify(gitHubClient).getContributors(owner, repo);

        verify(valueOperations).set(
                eq(cacheKey),
                anyString(),
                eq(Duration.ofMinutes(ContributorCacheKey.TTL_MINUTES))
        );
    }


}


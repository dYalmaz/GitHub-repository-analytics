package com.example.gitactivity.controller;

import com.example.gitactivity.dto.CacheStatsResponse;
import com.example.gitactivity.dto.GitHubContributorResponse;
import com.example.gitactivity.dto.GitHubRepositoryResponse;
import com.example.gitactivity.dto.RepositoryAnalyticsResponse;
import com.example.gitactivity.exception.GitHubRateLimitException;
import com.example.gitactivity.exception.GitHubServiceException;
import com.example.gitactivity.exception.RepositoryNotFoundException;
import com.example.gitactivity.service.RepositoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RepositoryController.class)
class RepositoryControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RepositoryService repositoryService;

    @Test
    void shouldReturnRepositoryDetails() throws Exception {

        GitHubRepositoryResponse repository =
                new GitHubRepositoryResponse();

        repository.setName("spring-boot");
        repository.setDescription("Spring Boot");
        repository.setStars(100);
        repository.setForks(25);
        repository.setLanguage("Java");

        when(repositoryService.getRepoDetails(
                "spring-projects",
                "spring-boot"
        )).thenReturn(repository);

        mockMvc.perform(
                        get("/api/repositories/spring-projects/spring-boot")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("spring-boot"))
                .andExpect(jsonPath("$.description").value("Spring Boot"))
                .andExpect(jsonPath("$.stars").value(100))
                .andExpect(jsonPath("$.forks").value(25))
                .andExpect(jsonPath("$.language").value("Java"));
    }

    @Test
    void shouldReturnContributors() throws Exception {

        GitHubContributorResponse contributor =
                new GitHubContributorResponse();

        contributor.setLogin("test-user");
        contributor.setContributions(10);

        GitHubContributorResponse[] contributors =
                new GitHubContributorResponse[]{contributor};

        when(repositoryService.getContributors(
                "spring-projects",
                "spring-boot"
        )).thenReturn(contributors);

        mockMvc.perform(
                        get("/api/repositories/spring-projects/spring-boot/contributors")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].login").value("test-user"))
                .andExpect(jsonPath("$[0].contributions").value(10));
    }

    @Test
    void shouldReturnRepositoryAnalytics() throws Exception {

        RepositoryAnalyticsResponse analytics =
                new RepositoryAnalyticsResponse();

        analytics.setRepository("spring-boot");
        analytics.setStars(100);
        analytics.setForks(50);
        analytics.setForkToStarRatio(0.5);

        when(repositoryService.getRepositoryAnalytics(
                "spring-projects",
                "spring-boot"
        )).thenReturn(analytics);

        mockMvc.perform(
                        get("/api/repositories/spring-projects/spring-boot/analytics")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repository").value("spring-boot"))
                .andExpect(jsonPath("$.stars").value(100))
                .andExpect(jsonPath("$.forks").value(50))
                .andExpect(jsonPath("$.forkToStarRatio").value(0.5));
    }

    @Test
    void shouldReturnCacheStats() throws Exception {

        CacheStatsResponse stats =
                new CacheStatsResponse();

        stats.setHits(10);
        stats.setMisses(5);

        when(repositoryService.getCacheStats())
                .thenReturn(stats);

        mockMvc.perform(
                        get("/api/repositories/cache/stats")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hits").value(10))
                .andExpect(jsonPath("$.misses").value(5));
    }

    @Test
    void shouldReturnNotFoundWhenRepositoryDoesNotExist() throws Exception {

        when(repositoryService.getRepoDetails(
                "invalid-owner",
                "invalid-repo"
        )).thenThrow(
                new RepositoryNotFoundException("Repository not found")
        );

        mockMvc.perform(
                        get("/api/repositories/invalid-owner/invalid-repo")
                )
                .andExpect(status().isNotFound())
                .andExpect(content().string("Repository not found"));
    }

    @Test
    void shouldReturnTooManyRequestsWhenRateLimitExceeded() throws Exception {

        when(repositoryService.getRepoDetails(
                "spring-projects",
                "spring-boot"
        )).thenThrow(
                new GitHubRateLimitException("GitHub API rate limit exceeded")
        );

        mockMvc.perform(
                        get("/api/repositories/spring-projects/spring-boot")
                )
                .andExpect(status().isTooManyRequests())
                .andExpect(content().string("GitHub API rate limit exceeded"));
    }

    @Test
    void shouldReturnBadGatewayWhenGitHubServiceFails() throws Exception {

        when(repositoryService.getRepoDetails(
                "spring-projects",
                "spring-boot"
        )).thenThrow(
                new GitHubServiceException("GitHub service failed")
        );

        mockMvc.perform(
                        get("/api/repositories/spring-projects/spring-boot")
                )
                .andExpect(status().isBadGateway())
                .andExpect(content().string("GitHub service failed"));
    }

}
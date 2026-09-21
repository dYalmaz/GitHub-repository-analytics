package com.example.gitactivity.service;

import com.example.gitactivity.client.GitHubClient;
import com.example.gitactivity.dto.GitHubRepositoryResponse;
import com.example.gitactivity.exception.GitHubRateLimitException;
import com.example.gitactivity.exception.GitHubServiceException;
import com.example.gitactivity.exception.RepositoryNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepositoryServiceTest {

    @Mock
    private GitHubClient gitHubClient;

    @InjectMocks
    private RepositoryService repositoryService;

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


}


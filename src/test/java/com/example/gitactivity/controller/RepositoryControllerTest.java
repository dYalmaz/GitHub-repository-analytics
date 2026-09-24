package com.example.gitactivity.controller;

import com.example.gitactivity.dto.GitHubRepositoryResponse;
import com.example.gitactivity.exception.GitHubRateLimitException;
import com.example.gitactivity.exception.GitHubServiceException;
import com.example.gitactivity.exception.RepositoryNotFoundException;
import com.example.gitactivity.service.RepositoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class RepositoryControllerTest {

    private RepositoryService repositoryService;
    private RepositoryController repositoryController;

    @BeforeEach
    void setUp(){
        repositoryService = mock(RepositoryService.class);
        repositoryController = new RepositoryController(repositoryService);
    }

    @Test
    void getRepositoryShouldReturnRepositoryDetails() {

        GitHubRepositoryResponse repository = new GitHubRepositoryResponse();
        repository.setName("spring-boot");

        when(repositoryService.getRepoDetails("spring-projects", "spring-boot")).thenReturn(repository);

        GitHubRepositoryResponse response = repositoryController.getRepository("spring-projects", "spring-boot");

        assertEquals("spring-boot", response.getName());

        verify(repositoryService).getRepoDetails("spring-projects", "spring-boot");



    }

    @Test
    void getRepositoryShouldPropagateRepositoryNotFoundException(){

        when(repositoryService.getRepoDetails("invalid-owner","invalid-repo")).thenThrow(new RepositoryNotFoundException("Repository not found"));

        assertThrows(RepositoryNotFoundException.class, () -> {
            repositoryController.getRepository("invalid-owner", "invalid-repo");
        });

    }

    @Test
    void getRepositoryShouldPropagateRateLimitExceededException(){

        when(repositoryService.getRepoDetails("spring-projects", "spring-boot")).thenThrow(new GitHubRateLimitException("Rate limit exceeded"));

        assertThrows(GitHubRateLimitException.class, () -> {repositoryController.getRepository("spring-projects", "spring-boot");});

    }

    @Test
    void getRepositoryShouldPropagateGitHubServiceException(){

        when(repositoryService.getRepoDetails("spring-projects", "spring-boot")).thenThrow(new GitHubServiceException("GitHub service error"));

        assertThrows(GitHubServiceException.class, () -> {repositoryController.getRepository("spring-projects", "spring-boot");});

    }




}

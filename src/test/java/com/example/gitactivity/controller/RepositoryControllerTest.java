package com.example.gitactivity.controller;

import com.example.gitactivity.dto.GitHubRepositoryResponse;
import com.example.gitactivity.service.RepositoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    }


}

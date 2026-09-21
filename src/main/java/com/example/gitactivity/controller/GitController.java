package com.example.gitactivity.controller;

import com.example.gitactivity.dto.GitHubRepositoryResponse;
import com.example.gitactivity.service.RepositoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/repositories")
public class GitController {
    private final RepositoryService repositoryService;

    public GitController(RepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }

    @GetMapping("/{owner}/{repo}")
    public GitHubRepositoryResponse getRepository(@PathVariable String owner, @PathVariable String repo) {

        return repositoryService.getRepoDetails(owner, repo);



    }

}

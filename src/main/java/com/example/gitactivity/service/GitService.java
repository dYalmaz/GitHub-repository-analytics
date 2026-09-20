package com.example.gitactivity.service;

import com.example.gitactivity.client.GitHubClient;
import com.example.gitactivity.dto.GitHubRepositoryResponse;
import org.springframework.stereotype.Service;

@Service
public class GitService {

    private final GitHubClient gitClient;

    public GitService(GitHubClient gitClient) {
        this.gitClient = gitClient;
    }

    public GitHubRepositoryResponse getRepoDetails(String owner, String repo) {

        return gitClient.getRepository(owner, repo);
    }

}

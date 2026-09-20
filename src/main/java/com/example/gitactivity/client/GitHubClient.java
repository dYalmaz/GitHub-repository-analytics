package com.example.gitactivity.client;

import com.example.gitactivity.dto.GitHubRepositoryResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class GitHubClient {

    private final RestClient restClient;

    public GitHubClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public GitHubRepositoryResponse getRepository(
            String owner,
            String repo) {

        return restClient.get()
                .uri("/repos/{owner}/{repo}", owner, repo)
                .retrieve()
                .body(GitHubRepositoryResponse.class);
    }
}

package com.example.gitactivity.client;

import com.example.gitactivity.dto.GitHubContributorResponse;
import com.example.gitactivity.dto.GitHubRepositoryResponse;
import com.example.gitactivity.exception.GitHubRateLimitException;
import com.example.gitactivity.exception.GitHubServiceException;
import com.example.gitactivity.exception.RepositoryNotFoundException;
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
                .onStatus(
                        status -> status.value() == 404,
                        (request, response) -> {
                            throw new RepositoryNotFoundException(
                                    "Repository not found: " + owner + "/" + repo
                            );
                        }
                )
                .onStatus(
                        status -> status.value() == 403,
                        (request, response) -> {

                            String remaining =
                                    response.getHeaders().getFirst("X-RateLimit-Remaining");

                            if ("0".equals(remaining)) {
                                throw new GitHubRateLimitException(
                                        "GitHub rate limit exceeded"
                                );
                            }
                        }
                )
                .onStatus(
                        status -> status.is5xxServerError(),
                        (request, response) -> {
                            throw new GitHubServiceException(
                                    "GitHub service is currently unavailable"
                            );
                        }
                )
                .body(GitHubRepositoryResponse.class);
    }

    public GitHubContributorResponse[] getContributors(
            String owner,
            String repo) {

        return restClient.get()
                .uri("/repos/{owner}/{repo}/contributors", owner, repo)
                .retrieve()
                .body(GitHubContributorResponse[].class);
    }


}

package com.example.gitactivity.client;

import com.example.gitactivity.dto.GitHubRepositoryResponse;
import com.example.gitactivity.exception.GitHubRateLimitException;
import com.example.gitactivity.exception.GitHubServiceException;
import com.example.gitactivity.exception.RepositoryNotFoundException;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import okhttp3.Headers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class GitHubClientTest {

    private GitHubClient gitHubClient;
    private MockWebServer mockWebServer;

    @BeforeEach
    void setUp() throws IOException {

        mockWebServer = new MockWebServer();
        mockWebServer.start();

        RestClient restClient = RestClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .build();

        gitHubClient = new GitHubClient(restClient);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.close();
    }

    @Test
    void shouldReturnRepositoryDetails() throws Exception {

        mockWebServer.enqueue(
                new MockResponse(
                        200,
                        new Headers.Builder()
                                .add("Content-Type", "application/json")
                                .build(),
                        """
                        {
                            "name": "spring-boot",
                            "description": "Spring Boot",
                            "stargazers_count": 100,
                            "forks_count": 50,
                            "open_issues_count": 10,
                            "language": "Java",
                            "watchers_count": 100,
                            "created_at": "2013-04-01T00:00:00Z",
                            "updated_at": "2026-09-20T00:00:00Z"
                        }
                        """
                )
        );

        GitHubRepositoryResponse result = gitHubClient.getRepository("spring-projects", "spring-boot");

        assertEquals("spring-boot", result.getName());

        var request = mockWebServer.takeRequest();
        assertEquals("/repos/spring-projects/spring-boot", request.getTarget().toString());
    }

    @Test
    void shouldThrowRepositoryNotFoundException() throws Exception {

        mockWebServer.enqueue(
                new MockResponse(
                        404,
                        new Headers.Builder().build(),
                        ""
                )
        );

        assertThrows(
                RepositoryNotFoundException.class,
                () -> gitHubClient.getRepository("spring-projects", "non-existent")
        );

    }

    @Test
    void shouldThrowGitHubRateLimitException() throws Exception {

        mockWebServer.enqueue(
                new MockResponse(
                        403,
                        new Headers.Builder()
                                .add("X-RateLimit-Remaining", "0")
                                .build(),
                        ""
                )
        );

        assertThrows(
                GitHubRateLimitException.class,
                () -> gitHubClient.getRepository("spring-projects", "spring-boot")
        );

    }

    @Test
    void shouldThrowGitHubServiceException() throws Exception {

        mockWebServer.enqueue(
                new MockResponse(
                        500,
                        new Headers.Builder().build(),
                        ""
                )
        );

        assertThrows(
                GitHubServiceException.class,
                () -> gitHubClient.getRepository("spring-projects", "spring-boot")
        );

    }

}

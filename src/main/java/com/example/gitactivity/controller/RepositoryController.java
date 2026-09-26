package com.example.gitactivity.controller;

import com.example.gitactivity.dto.CacheStatsResponse;
import com.example.gitactivity.dto.GitHubContributorResponse;
import com.example.gitactivity.dto.GitHubRepositoryResponse;
import com.example.gitactivity.dto.RepositoryAnalyticsResponse;
import com.example.gitactivity.service.RepositoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/repositories")
public class RepositoryController {
    private final RepositoryService repositoryService;

    public RepositoryController(RepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }

    @GetMapping("/{owner}/{repo}")
    public GitHubRepositoryResponse getRepository(@PathVariable String owner, @PathVariable String repo) {

        return repositoryService.getRepoDetails(owner, repo);

    }

    @GetMapping("/{owner}/{repo}/contributors")
    public ResponseEntity<GitHubContributorResponse[]> getContributors(@PathVariable String owner, @PathVariable String repo) {

        GitHubContributorResponse[] contributors = repositoryService.getContributors(owner, repo);

        return ResponseEntity.ok(contributors);

    }

    @GetMapping("/{owner}/{repo}/analytics")
    public ResponseEntity<RepositoryAnalyticsResponse> getRepositoryAnalytics(@PathVariable String owner, @PathVariable String repo){
        RepositoryAnalyticsResponse analytics = repositoryService.getRepositoryAnalytics(owner, repo);
        return ResponseEntity.ok(analytics);
    }

    @GetMapping("/cache/stats")
    public ResponseEntity<CacheStatsResponse> getCacheStats() {
        CacheStatsResponse stats = repositoryService.getCacheStats();
        return ResponseEntity.ok(stats);
    }

}

package com.hdg.prysm.enrichment;

import com.hdg.prysm.context.PrContext;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.env.MockEnvironment;
import tools.jackson.databind.ObjectMapper;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GithubPullRequestMetadataProviderTest {

    /**
     * 应从 GitHub API 获取 PR 标题、正文和少量 commit message。
     */
    @Test
    void shouldFetchPullRequestMetadataFromGithubApi() throws Exception {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("GITHUB_TOKEN", "ghs_test_token");
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> pullRequestResponse = successfulResponse("""
                {
                  "title": "Add review input",
                  "body": "This pull request adds input assembly."
                }
                """);
        HttpResponse<String> commitsResponse = successfulResponse("""
                [
                  {"commit": {"message": "feat: add input assembly\\n\\nbody"}},
                  {"commit": {"message": "test: add coverage"}}
                ]
                """);
        when(httpClient.send(any(HttpRequest.class), anyStringBodyHandler()))
                .thenReturn(pullRequestResponse)
                .thenReturn(commitsResponse);
        GithubPullRequestMetadataProvider provider = newProvider(environment, httpClient, 100, 2, 100);

        PullRequestMetadata metadata = provider.fetch(new PrContext("owner", "repo", 9));

        assertEquals("Add review input", metadata.getTitle());
        assertEquals("This pull request adds input assembly.", metadata.getBody());
        assertEquals(List.of("feat: add input assembly", "test: add coverage"), metadata.getCommitMessages());
        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient, org.mockito.Mockito.times(2)).send(requestCaptor.capture(), anyStringBodyHandler());
        assertEquals("https://api.github.test/repos/owner/repo/pulls/9", requestCaptor.getAllValues().get(0).uri().toString());
        assertEquals("https://api.github.test/repos/owner/repo/pulls/9/commits?per_page=2", requestCaptor.getAllValues().get(1).uri().toString());
    }

    /**
     * 应裁剪过长的 PR body 和 commit message。
     */
    @Test
    void shouldLimitBodyAndCommitMessages() throws Exception {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("GITHUB_TOKEN", "ghs_test_token");
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> pullRequestResponse = successfulResponse("""
                {
                  "title": "Title",
                  "body": "abcdef"
                }
                """);
        HttpResponse<String> commitsResponse = successfulResponse("""
                [
                  {"commit": {"message": "uvwxyz"}}
                ]
                """);
        when(httpClient.send(any(HttpRequest.class), anyStringBodyHandler()))
                .thenReturn(pullRequestResponse)
                .thenReturn(commitsResponse);
        GithubPullRequestMetadataProvider provider = newProvider(environment, httpClient, 3, 1, 4);

        PullRequestMetadata metadata = provider.fetch(new PrContext("owner", "repo", 9));

        assertEquals("abc", metadata.getBody());
        assertEquals(List.of("uvwx"), metadata.getCommitMessages());
        assertEquals("pull request body truncated", metadata.getNote());
    }

    /**
     * 缺少 GitHub token 时不应发请求。
     */
    @Test
    void shouldRequireGithubTokenBeforeCallingApi() throws Exception {
        MockEnvironment environment = new MockEnvironment();
        HttpClient httpClient = mock(HttpClient.class);
        GithubPullRequestMetadataProvider provider = newProvider(environment, httpClient, 100, 2, 100);

        assertThrows(IllegalStateException.class, () -> provider.fetch(new PrContext("owner", "repo", 9)));
        verify(httpClient, never()).send(any(HttpRequest.class), anyStringBodyHandler());
    }

    /**
     * GitHub 非成功响应应失败。
     */
    @Test
    void shouldRejectGithubApiFailureStatus() throws Exception {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("GITHUB_TOKEN", "ghs_test_token");
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(500);
        when(response.body()).thenReturn("{}");
        when(httpClient.send(any(HttpRequest.class), anyStringBodyHandler())).thenReturn(response);
        GithubPullRequestMetadataProvider provider = newProvider(environment, httpClient, 100, 2, 100);

        assertThrows(IllegalStateException.class, () -> provider.fetch(new PrContext("owner", "repo", 9)));
    }

    /**
     * 创建测试用 provider。
     */
    private static GithubPullRequestMetadataProvider newProvider(
            MockEnvironment environment,
            HttpClient httpClient,
            int maxBodyCharacters,
            int maxCommits,
            int maxCommitMessageCharacters
    ) {
        return new GithubPullRequestMetadataProvider(
                environment,
                new ObjectMapper(),
                httpClient,
                "https://api.github.test/",
                maxBodyCharacters,
                maxCommits,
                maxCommitMessageCharacters
        );
    }

    /**
     * 创建成功响应。
     */
    private static HttpResponse<String> successfulResponse(String body) {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(body);
        return response;
    }

    /**
     * 匹配字符串响应 BodyHandler。
     */
    private static HttpResponse.BodyHandler<String> anyStringBodyHandler() {
        return org.mockito.ArgumentMatchers.any();
    }
}

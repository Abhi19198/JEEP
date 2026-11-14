package com.jeep.repository.github;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * The {@link RestClient} the GitHub integration uses. It points at the
 * public GitHub API with short timeouts so a stalled GitHub cannot stall
 * JEEP requests. No authentication is configured: requests are
 * unauthenticated and no token is stored anywhere.
 */
@Configuration
public class GitHubClientConfig {

    @Bean
    public RestClient githubRestClient(RestClient.Builder builder) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        return builder
            .baseUrl(GitHubApiClient.API_BASE_URL)
            .requestFactory(requestFactory)
            .build();
    }
}

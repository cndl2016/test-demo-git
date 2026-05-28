package com.dm.cn.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.client.reactive.JdkClientHttpConnector;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.http.HttpClient;

@Configuration
public class HttpClientConfig {
    private HttpClient httpClient() {
        return HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
    }

    @Bean
    @Primary
    public RestClient.Builder customRestClientBuilder() {
        return RestClient.builder().requestFactory(new JdkClientHttpRequestFactory(httpClient()));
    }

    @Bean
    @Primary
    public WebClient.Builder customWebClientBuilder() {
        return WebClient.builder().clientConnector(new JdkClientHttpConnector(httpClient()));
    }
}

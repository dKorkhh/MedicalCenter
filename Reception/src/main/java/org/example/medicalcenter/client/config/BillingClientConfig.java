package org.example.medicalcenter.client.config;

import org.example.medicalcenter.client.BillingClient;
import org.example.medicalcenter.client.interceptor.CorrelationIdInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class BillingClientConfig {

    @Value("${billing.service.url}")
    private String billingServiceUrl;

    @Bean
    public JdkClientHttpRequestFactory jdkClientHttpRequestFactory() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(3));
        return requestFactory;
    }

    @Bean
    public RestClient billingRestClient(JdkClientHttpRequestFactory requestFactory) {
        return RestClient.builder()
                .baseUrl(billingServiceUrl)
                .requestFactory(requestFactory)
                .requestInterceptor(new CorrelationIdInterceptor())
                .build();
    }

    @Bean
    public HttpServiceProxyFactory httpServiceProxyFactory(RestClient billingRestClient) {
        RestClientAdapter adapter = RestClientAdapter.create(billingRestClient);
        return HttpServiceProxyFactory.builderFor(adapter).build();
    }

    @Bean
    public BillingClient billingClient(HttpServiceProxyFactory httpServiceProxyFactory) {
        return httpServiceProxyFactory.createClient(BillingClient.class);
    }
}

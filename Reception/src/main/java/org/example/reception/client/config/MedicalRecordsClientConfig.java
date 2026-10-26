package org.example.reception.client.config;

import org.example.reception.client.MedicalRecordsClient;
import org.example.reception.client.interceptor.CorrelationIdInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class MedicalRecordsClientConfig {

    @Value("${medical-records.service.url:http://localhost:8083}")
    private String medicalRecordsServiceUrl;

    @Bean
    public MedicalRecordsClient medicalRecordsClient(JdkClientHttpRequestFactory requestFactory) {
        RestClient restClient = RestClient.builder()
                .baseUrl(medicalRecordsServiceUrl)
                .requestFactory(requestFactory)
                .requestInterceptor(new CorrelationIdInterceptor())
                .build();
        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();
        return factory.createClient(MedicalRecordsClient.class);
    }
}

package org.example.reception.grpc;

import lombok.Getter;
import org.example.reception.grpc.proto.BillingGrpcServiceGrpc;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.ImportGrpcClients;

@Getter
@Configuration
@ImportGrpcClients(
        target = "billing",
        types = {
                BillingGrpcServiceGrpc.BillingGrpcServiceBlockingStub.class,
                BillingGrpcServiceGrpc.BillingGrpcServiceStub.class
        }
)
public class BillingGrpcClientConfig {

    @Value("${spring.grpc.client.channel.billing.target:dns:///localhost:9090}")
    private String target;

    @Value("${spring.grpc.client.channel.billing.service-config.method[0].retry.max-attempts:4}")
    private int maxAttempts;

    @Value("${spring.grpc.client.channel.billing.service-config.method[0].retry.initial-backoff:100ms}")
    private String initialBackoff;

    @Value("${spring.grpc.client.channel.billing.service-config.method[0].retry.max-backoff:1s}")
    private String maxBackoff;

    @Value("${spring.grpc.client.channel.billing.service-config.method[0].retry.backoff-multiplier:2.0}")
    private double backoffMultiplier;
}

package org.example.reception.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.example.reception.dto.GrpcBillingHistoryEventDto;
import org.example.reception.dto.GrpcClientStreamQuoteRequest;
import org.example.reception.dto.GrpcLiveValidationItemRequestDto;
import org.example.reception.dto.GrpcLiveValidationItemResponseDto;
import org.example.reception.dto.GrpcServiceQuoteItemDto;
import org.example.reception.dto.GrpcServicesQuoteSummaryDto;
import org.example.reception.dto.GrpcUnaryInvoiceResultDto;
import org.example.reception.grpc.proto.BillingGrpcServiceGrpc;
import org.example.reception.grpc.proto.GrpcBillingHistoryEvent;
import org.example.reception.grpc.proto.GrpcInvoiceLookupRequest;
import org.example.reception.grpc.proto.GrpcInvoiceLookupResponse;
import org.example.reception.grpc.proto.GrpcLiveValidationItemRequest;
import org.example.reception.grpc.proto.GrpcLiveValidationItemResponse;
import org.example.reception.grpc.proto.GrpcPatientBillingStreamRequest;
import org.example.reception.grpc.proto.GrpcServiceQuoteItemRequest;
import org.example.reception.grpc.proto.GrpcServicesQuoteSummaryResponse;
import org.example.reception.mapper.ReceptionGrpcMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BillingGrpcIntegrationTest {

    private Server grpcServer;
    private ManagedChannel channel;
    private BillingGrpcClientService clientService;

    @BeforeEach
    void setUp() throws Exception {
        grpcServer = ServerBuilder.forPort(0)
                .addService(new EmbeddedTestBillingGrpcService())
                .build()
                .start();

        int serverPort = grpcServer.getPort();
        Map<String, Object> retryServiceConfig = buildRetryServiceConfig();

        channel = ManagedChannelBuilder.forAddress("localhost", serverPort)
                .usePlaintext()
                .defaultServiceConfig(retryServiceConfig)
                .enableRetry()
                .maxRetryAttempts(4)
                .build();

        BillingGrpcClientConfig config = new BillingGrpcClientConfig();
        ReflectionTestUtils.setField(config, "target", "dns:///localhost:" + serverPort);
        ReflectionTestUtils.setField(config, "maxAttempts", 4);
        ReflectionTestUtils.setField(config, "initialBackoff", "100ms");
        ReflectionTestUtils.setField(config, "maxBackoff", "500ms");
        ReflectionTestUtils.setField(config, "backoffMultiplier", 2.0);

        ReceptionGrpcMapper mapper = Mappers.getMapper(ReceptionGrpcMapper.class);
        BillingGrpcStreamCollector streamCollector = new BillingGrpcStreamCollector(mapper);

        clientService = new BillingGrpcClientService(
                BillingGrpcServiceGrpc.newBlockingStub(channel),
                BillingGrpcServiceGrpc.newStub(channel),
                config,
                null,
                mapper,
                streamCollector);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (channel != null) {
            channel.shutdownNow();
            channel.awaitTermination(5, TimeUnit.SECONDS);
        }
        if (grpcServer != null) {
            grpcServer.shutdownNow();
            grpcServer.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void shouldExecuteUnaryCallAndRecoverFromSimulatedUnavailableFailuresViaRetryPolicy() {
        GrpcUnaryInvoiceResultDto result = clientService.fetchInvoiceUnaryWithRetry("inv-retry-100", 2);

        assertThat(result.invoice()).isNotNull();
        assertThat(result.invoice().invoiceId()).isEqualTo("inv-retry-100");
        assertThat(result.invoice().serverAttemptsUsed()).isEqualTo(3);
        assertThat(result.totalAttemptsExecuted()).isEqualTo(3);
        assertThat(result.retryExecutionLogs()).isNotEmpty();
    }

    @Test
    void shouldFailUnaryCallWhenSimulatedUnavailableFailuresExceedMaxRetryAttempts() {
        assertThatThrownBy(() -> clientService.fetchInvoiceUnaryWithRetry("inv-fail-999", 6))
                .isInstanceOf(StatusRuntimeException.class)
                .satisfies(ex -> {
                    StatusRuntimeException statusEx = (StatusRuntimeException) ex;
                    assertThat(statusEx.getStatus().getCode()).isEqualTo(Status.Code.UNAVAILABLE);
                });
    }

    @Test
    void shouldReceiveAllEventsFromServerStreamingCall() {
        List<GrpcBillingHistoryEventDto> events = clientService.streamPatientBillingHistory("patient-001");

        assertThat(events).hasSize(2);
        assertThat(events.get(0).recordType()).isEqualTo("INVOICE");
        assertThat(events.get(1).recordType()).isEqualTo("REFUND");
    }

    @Test
    void shouldAggregateItemsAndApplyPackageDiscountInClientStreamingCall() {
        GrpcClientStreamQuoteRequest quoteRequest = GrpcClientStreamQuoteRequest.builder()
                .patientId("patient-001")
                .items(List.of(
                        GrpcServiceQuoteItemDto.builder()
                                .serviceId("srv-1")
                                .serviceName("Первинний прийом терапевта")
                                .unitPrice("500.00")
                                .quantity(1)
                                .currency("UAH")
                                .build(),
                        GrpcServiceQuoteItemDto.builder()
                                .serviceId("srv-2")
                                .serviceName("УЗД серця")
                                .unitPrice("800.00")
                                .quantity(2)
                                .currency("UAH")
                                .build()))
                .build();

        GrpcServicesQuoteSummaryDto summary = clientService.calculateClinicServicesQuote(quoteRequest);

        assertThat(summary.totalItemsCount()).isEqualTo(2);
        assertThat(summary.totalUnitsCount()).isEqualTo(3);
        assertThat(summary.subtotalAmount()).isEqualTo("2100.00");
        assertThat(summary.discountAmount()).isEqualTo("210.00");
        assertThat(summary.finalTotalAmount()).isEqualTo("1890.00");
    }

    @Test
    void shouldProcessRealTimeResponsesInBidirectionalStreamingCall() {
        List<GrpcLiveValidationItemRequestDto> requests = List.of(
                GrpcLiveValidationItemRequestDto.builder()
                        .stepId("step-1")
                        .invoiceId("inv-1")
                        .serviceName("Консультація кардіолога")
                        .requestedAmount("600.00")
                        .currency("UAH")
                        .build(),
                GrpcLiveValidationItemRequestDto.builder()
                        .stepId("step-2")
                        .invoiceId("inv-2")
                        .serviceName("ЕКГ з розшифровкою")
                        .requestedAmount("400.00")
                        .currency("UAH")
                        .build());

        List<GrpcLiveValidationItemResponseDto> responses = clientService.runLiveBillingValidation(requests);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).valid()).isTrue();
        assertThat(responses.get(0).runningTotal()).isEqualTo("600.00");
        assertThat(responses.get(1).valid()).isTrue();
        assertThat(responses.get(1).runningTotal()).isEqualTo("1000.00");
    }

    private Map<String, Object> buildRetryServiceConfig() {
        Map<String, Object> retryPolicy = new LinkedHashMap<>();
        retryPolicy.put("maxAttempts", 4.0);
        retryPolicy.put("initialBackoff", "0.1s");
        retryPolicy.put("maxBackoff", "0.5s");
        retryPolicy.put("backoffMultiplier", 2.0);
        retryPolicy.put("retryableStatusCodes", List.of("UNAVAILABLE"));

        Map<String, Object> serviceNameEntry = new LinkedHashMap<>();
        serviceNameEntry.put("service", BillingGrpcServiceGrpc.SERVICE_NAME);
        serviceNameEntry.put("method", "GetInvoiceWithRetry");

        Map<String, Object> methodConfigEntry = new LinkedHashMap<>();
        methodConfigEntry.put("name", List.of(serviceNameEntry));
        methodConfigEntry.put("retryPolicy", retryPolicy);

        Map<String, Object> serviceConfig = new LinkedHashMap<>();
        serviceConfig.put("methodConfig", List.of(methodConfigEntry));
        return serviceConfig;
    }

    private static class EmbeddedTestBillingGrpcService extends BillingGrpcServiceGrpc.BillingGrpcServiceImplBase {

        private final ConcurrentHashMap<String, AtomicInteger> attemptsByRequest = new ConcurrentHashMap<>();

        @Override
        public void getInvoiceWithRetry(
                GrpcInvoiceLookupRequest request,
                StreamObserver<GrpcInvoiceLookupResponse> observer) {
            int attempt = attemptsByRequest
                    .computeIfAbsent(request.getRequestId(), key -> new AtomicInteger(0))
                    .incrementAndGet();
            if (attempt <= request.getFailFirstAttempts()) {
                observer.onError(Status.UNAVAILABLE
                        .withDescription("Simulated UNAVAILABLE on attempt " + attempt)
                        .asRuntimeException());
                return;
            }
            observer.onNext(GrpcInvoiceLookupResponse.newBuilder()
                    .setInvoiceId(request.getInvoiceId())
                    .setAppointmentId("appt-100")
                    .setPatientId("patient-001")
                    .setAmount("500.00")
                    .setCurrency("UAH")
                    .setPaymentStatus("PENDING")
                    .setServerAttemptsUsed(attempt)
                    .setProcessedAt(LocalDateTime.now().toString())
                    .build());
            observer.onCompleted();
        }

        @Override
        public void streamPatientBillingHistory(
                GrpcPatientBillingStreamRequest request,
                StreamObserver<GrpcBillingHistoryEvent> observer) {
            observer.onNext(GrpcBillingHistoryEvent.newBuilder()
                    .setRecordType("INVOICE")
                    .setRecordId("inv-100")
                    .setAppointmentId("appt-100")
                    .setPatientId(request.getPatientId())
                    .setAmount("500.00")
                    .setCurrency("UAH")
                    .setStatus("REFUNDED")
                    .setDescription("Initial invoice")
                    .setTimestamp(LocalDateTime.now().toString())
                    .build());
            observer.onNext(GrpcBillingHistoryEvent.newBuilder()
                    .setRecordType("REFUND")
                    .setRecordId("ref-100")
                    .setAppointmentId("appt-100")
                    .setPatientId(request.getPatientId())
                    .setAmount("500.00")
                    .setCurrency("UAH")
                    .setStatus("PROCESSED")
                    .setDescription("Cancellation refund")
                    .setTimestamp(LocalDateTime.now().toString())
                    .build());
            observer.onCompleted();
        }

        @Override
        public StreamObserver<GrpcServiceQuoteItemRequest> calculateServicesQuote(
                StreamObserver<GrpcServicesQuoteSummaryResponse> responseObserver) {
            return new StreamObserver<>() {
                private int itemsCount = 0;
                private int unitsCount = 0;
                private BigDecimal subtotal = BigDecimal.ZERO;
                private final List<String> names = new ArrayList<>();

                @Override
                public void onNext(GrpcServiceQuoteItemRequest item) {
                    itemsCount++;
                    unitsCount += item.getQuantity();
                    subtotal = subtotal.add(new BigDecimal(item.getUnitPrice()).multiply(BigDecimal.valueOf(item.getQuantity())));
                    names.add(item.getServiceName());
                }

                @Override
                public void onError(Throwable throwable) {
                }

                @Override
                public void onCompleted() {
                    BigDecimal discount = unitsCount >= 3
                            ? subtotal.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                    responseObserver.onNext(GrpcServicesQuoteSummaryResponse.newBuilder()
                            .setPatientId("patient-001")
                            .setTotalItemsCount(itemsCount)
                            .setTotalUnitsCount(unitsCount)
                            .setSubtotalAmount(subtotal.setScale(2, RoundingMode.HALF_UP).toPlainString())
                            .setDiscountAmount(discount.toPlainString())
                            .setFinalTotalAmount(subtotal.subtract(discount).setScale(2, RoundingMode.HALF_UP).toPlainString())
                            .setCurrency("UAH")
                            .addAllIncludedServices(names)
                            .build());
                    responseObserver.onCompleted();
                }
            };
        }

        @Override
        public StreamObserver<GrpcLiveValidationItemRequest> liveBillingValidation(
                StreamObserver<GrpcLiveValidationItemResponse> responseObserver) {
            return new StreamObserver<>() {
                private BigDecimal runningTotal = BigDecimal.ZERO;

                @Override
                public void onNext(GrpcLiveValidationItemRequest item) {
                    runningTotal = runningTotal.add(new BigDecimal(item.getRequestedAmount())).setScale(2, RoundingMode.HALF_UP);
                    responseObserver.onNext(GrpcLiveValidationItemResponse.newBuilder()
                            .setStepId(item.getStepId())
                            .setInvoiceId(item.getInvoiceId())
                            .setServiceName(item.getServiceName())
                            .setValid(true)
                            .setValidationStatus("VALIDATED")
                            .setRunningTotal(runningTotal.toPlainString())
                            .setCurrency(item.getCurrency())
                            .setMessage("OK")
                            .build());
                }

                @Override
                public void onError(Throwable throwable) {
                }

                @Override
                public void onCompleted() {
                    responseObserver.onCompleted();
                }
            };
        }
    }
}

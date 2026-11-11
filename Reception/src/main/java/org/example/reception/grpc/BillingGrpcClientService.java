package org.example.reception.grpc;

import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.reception.dto.GrpcBillingHistoryEventDto;
import org.example.reception.dto.GrpcClientStreamQuoteRequest;
import org.example.reception.dto.GrpcInvoiceDto;
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
import org.example.reception.grpc.proto.GrpcPatientBillingStreamRequest;
import org.example.reception.grpc.proto.GrpcServiceQuoteItemRequest;
import org.example.reception.mapper.ReceptionGrpcMapper;
import org.example.reception.repository.ClinicServiceRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillingGrpcClientService {

    private final BillingGrpcServiceGrpc.BillingGrpcServiceBlockingStub blockingStub;
    private final BillingGrpcServiceGrpc.BillingGrpcServiceStub asyncStub;
    private final BillingGrpcClientConfig clientConfig;
    private final ClinicServiceRepository clinicServiceRepository;
    private final ReceptionGrpcMapper receptionGrpcMapper;
    private final BillingGrpcStreamCollector streamCollector;

    private final CopyOnWriteArrayList<String> recentRetryLogs = new CopyOnWriteArrayList<>();

    public GrpcUnaryInvoiceResultDto fetchInvoiceUnaryWithRetry(String invoiceId, int failFirstAttempts) {
        String requestId = "req-" + UUID.randomUUID().toString().substring(0, 8);
        List<String> callLogs = new ArrayList<>();
        GrpcInvoiceLookupRequest request = receptionGrpcMapper.toInvoiceLookupRequest(requestId, invoiceId, failFirstAttempts);

        recordCallLog(callLogs, String.format(
                "[%s][gRPC Client][Unary GetInvoiceWithRetry] Starting call requestId=%s, invoiceId=%s, failFirstAttempts=%d",
                LocalDateTime.now(), requestId, invoiceId, failFirstAttempts));

        GrpcInvoiceDto invoiceDto = receptionGrpcMapper.toInvoiceDto(executeUnaryWithRetryDiagnostics(request, callLogs));

        recordCallLog(callLogs, String.format(
                "[%s][gRPC Client][Unary GetInvoiceWithRetry] Completed requestId=%s, invoiceId=%s, status=%s, serverAttemptsUsed=%d",
                LocalDateTime.now(), requestId, invoiceDto.invoiceId(), invoiceDto.paymentStatus(), invoiceDto.serverAttemptsUsed()));

        return receptionGrpcMapper.toUnaryResultDto(
                requestId, invoiceDto, failFirstAttempts, getRetryConfigurationSummary(), callLogs);
    }

    public List<GrpcBillingHistoryEventDto> streamPatientBillingHistory(String patientId) {
        log.info("[gRPC Client][ServerStreaming StreamPatientBillingHistory] Requesting history stream for patientId={}", patientId);
        GrpcPatientBillingStreamRequest request = receptionGrpcMapper.toPatientBillingStreamRequest(patientId);

        Iterator<GrpcBillingHistoryEvent> iterator = blockingStub
                .withDeadlineAfter(BillingGrpcStreamCollector.STREAM_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .streamPatientBillingHistory(request);
        List<GrpcBillingHistoryEventDto> events = new ArrayList<>();
        while (iterator.hasNext()) {
            events.add(receptionGrpcMapper.toHistoryEventDto(iterator.next()));
        }
        return events;
    }

    public GrpcServicesQuoteSummaryDto calculateClinicServicesQuote(GrpcClientStreamQuoteRequest quoteRequest) {
        List<GrpcServiceQuoteItemRequest> itemsToStream = resolveItemsForQuoteStream(quoteRequest);
        log.info("[gRPC Client][ClientStreaming CalculateServicesQuote] Streaming {} items for patientId={}",
                itemsToStream.size(), quoteRequest.patientId());

        return streamCollector.collectSingleSummaryFromClientStream(
                itemsToStream,
                observer -> asyncStub
                        .withDeadlineAfter(BillingGrpcStreamCollector.STREAM_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                        .calculateServicesQuote(observer));
    }

    public List<GrpcLiveValidationItemResponseDto> runLiveBillingValidation(List<GrpcLiveValidationItemRequestDto> items) {
        List<GrpcLiveValidationItemRequest> protoItems = items.stream()
                .map(receptionGrpcMapper::toLiveValidationProto)
                .toList();
        log.info("[gRPC Client][BidiStreaming LiveBillingValidation] Starting bidirectional validation for {} steps", protoItems.size());

        return streamCollector.collectListFromBidiStream(
                protoItems,
                observer -> asyncStub
                        .withDeadlineAfter(BillingGrpcStreamCollector.STREAM_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                        .liveBillingValidation(observer));
    }

    public Map<String, Object> getRetryConfigurationSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("targetService", BillingGrpcServiceGrpc.SERVICE_NAME);
        summary.put("target", clientConfig.getTarget());
        summary.put("maxAttempts", clientConfig.getMaxAttempts());
        summary.put("initialBackoff", clientConfig.getInitialBackoff());
        summary.put("maxBackoff", clientConfig.getMaxBackoff());
        summary.put("backoffMultiplier", clientConfig.getBackoffMultiplier());
        summary.put("retryableStatusCodes", List.of("UNAVAILABLE"));
        summary.put("recentRetryLogs", new ArrayList<>(recentRetryLogs));
        return summary;
    }

    private GrpcInvoiceLookupResponse executeUnaryWithRetryDiagnostics(
            GrpcInvoiceLookupRequest request,
            List<String> callLogs) {
        int maxAttempts = Math.max(1, clientConfig.getMaxAttempts());
        try {
            GrpcInvoiceLookupResponse response = blockingStub
                    .withDeadlineAfter(BillingGrpcStreamCollector.STREAM_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .getInvoiceWithRetry(request);
            recordSuccessfulRetries(response.getServerAttemptsUsed(), maxAttempts, callLogs);
            return response;
        } catch (StatusRuntimeException ex) {
            recordExhaustedRetries(ex, maxAttempts, callLogs);
            throw ex;
        }
    }

    private void recordSuccessfulRetries(int serverAttemptsUsed, int maxAttempts, List<String> callLogs) {
        for (int retriedAttempt = 1; retriedAttempt < serverAttemptsUsed; retriedAttempt++) {
            String nativeRetryLog = String.format(
                    "[%s][gRPC Channel RetryPolicy] Attempt %d/%d returned UNAVAILABLE -> automatic gRPC retry triggered (backoffMultiplier=%.1f)",
                    LocalDateTime.now(), retriedAttempt, maxAttempts, clientConfig.getBackoffMultiplier());
            log.warn(nativeRetryLog);
            appendLog(callLogs, nativeRetryLog);
        }
    }

    private void recordExhaustedRetries(StatusRuntimeException ex, int maxAttempts, List<String> callLogs) {
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            String failureLog = String.format(
                    "[%s][gRPC Channel RetryPolicy] Attempt %d/%d failed with status=%s (%s)",
                    LocalDateTime.now(), attempt, maxAttempts, ex.getStatus().getCode(), ex.getStatus().getDescription());
            log.warn(failureLog);
            appendLog(callLogs, failureLog);
        }
    }

    private List<GrpcServiceQuoteItemRequest> resolveItemsForQuoteStream(GrpcClientStreamQuoteRequest quoteRequest) {
        List<GrpcServiceQuoteItemRequest> resolved = new ArrayList<>();
        String patientId = quoteRequest.patientId() != null ? quoteRequest.patientId() : "anonymous";

        if (quoteRequest.clinicServiceIds() != null && clinicServiceRepository != null) {
            for (String serviceId : quoteRequest.clinicServiceIds()) {
                clinicServiceRepository.findById(serviceId)
                        .map(item -> receptionGrpcMapper.toServiceQuoteItemProto(item, patientId))
                        .ifPresent(resolved::add);
            }
        }

        if (quoteRequest.items() != null) {
            for (GrpcServiceQuoteItemDto item : quoteRequest.items()) {
                resolved.add(receptionGrpcMapper.toServiceQuoteItemProto(item, patientId));
            }
        }
        return resolved;
    }

    private void recordCallLog(List<String> callLogs, String message) {
        log.info(message);
        appendLog(callLogs, message);
    }

    private void appendLog(List<String> callLogs, String entry) {
        callLogs.add(entry);
        recentRetryLogs.add(entry);
        while (recentRetryLogs.size() > 50) {
            recentRetryLogs.remove(0);
        }
    }
}

package org.example.reception.grpc;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.reception.dto.GrpcLiveValidationItemResponseDto;
import org.example.reception.dto.GrpcServicesQuoteSummaryDto;
import org.example.reception.grpc.proto.GrpcLiveValidationItemResponse;
import org.example.reception.grpc.proto.GrpcServicesQuoteSummaryResponse;
import org.example.reception.mapper.ReceptionGrpcMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

@Slf4j
@Component
@RequiredArgsConstructor
public class BillingGrpcStreamCollector {

    public static final long STREAM_TIMEOUT_SECONDS = 10L;

    private final ReceptionGrpcMapper receptionGrpcMapper;

    public <R> GrpcServicesQuoteSummaryDto collectSingleSummaryFromClientStream(
            List<R> itemsToSend,
            Function<StreamObserver<GrpcServicesQuoteSummaryResponse>, StreamObserver<R>> streamStarter) {
        CountDownLatch finishLatch = new CountDownLatch(1);
        AtomicReference<GrpcServicesQuoteSummaryDto> responseRef = new AtomicReference<>();
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        StreamObserver<GrpcServicesQuoteSummaryResponse> responseObserver = createQuoteResponseObserver(
                responseRef, errorRef, finishLatch);
        StreamObserver<R> requestObserver = streamStarter.apply(responseObserver);
        itemsToSend.forEach(requestObserver::onNext);
        requestObserver.onCompleted();

        awaitStreamCompletion(finishLatch, errorRef, "CalculateServicesQuote");
        return responseRef.get();
    }

    public <R> List<GrpcLiveValidationItemResponseDto> collectListFromBidiStream(
            List<R> itemsToSend,
            Function<StreamObserver<GrpcLiveValidationItemResponse>, StreamObserver<R>> streamStarter) {
        CountDownLatch finishLatch = new CountDownLatch(1);
        List<GrpcLiveValidationItemResponseDto> responses = new CopyOnWriteArrayList<>();
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        StreamObserver<GrpcLiveValidationItemResponse> responseObserver = createLiveValidationResponseObserver(
                responses, errorRef, finishLatch);
        StreamObserver<R> requestObserver = streamStarter.apply(responseObserver);
        itemsToSend.forEach(requestObserver::onNext);
        requestObserver.onCompleted();

        awaitStreamCompletion(finishLatch, errorRef, "LiveBillingValidation");
        return new ArrayList<>(responses);
    }

    private StreamObserver<GrpcServicesQuoteSummaryResponse> createQuoteResponseObserver(
            AtomicReference<GrpcServicesQuoteSummaryDto> responseRef,
            AtomicReference<Throwable> errorRef,
            CountDownLatch finishLatch) {
        return new StreamObserver<>() {
            @Override
            public void onNext(GrpcServicesQuoteSummaryResponse summary) {
                log.info("[gRPC Client][ClientStreaming CalculateServicesQuote] Received summary finalTotal={} {}",
                        summary.getFinalTotalAmount(), summary.getCurrency());
                responseRef.set(receptionGrpcMapper.toQuoteSummaryDto(summary));
            }

            @Override
            public void onError(Throwable throwable) {
                errorRef.set(throwable);
                finishLatch.countDown();
            }

            @Override
            public void onCompleted() {
                finishLatch.countDown();
            }
        };
    }

    private StreamObserver<GrpcLiveValidationItemResponse> createLiveValidationResponseObserver(
            List<GrpcLiveValidationItemResponseDto> responses,
            AtomicReference<Throwable> errorRef,
            CountDownLatch finishLatch) {
        return new StreamObserver<>() {
            @Override
            public void onNext(GrpcLiveValidationItemResponse response) {
                log.info("[gRPC Client][BidiStreaming LiveBillingValidation] Received live response stepId={}, status={}, runningTotal={}",
                        response.getStepId(), response.getValidationStatus(), response.getRunningTotal());
                responses.add(receptionGrpcMapper.toLiveValidationResponseDto(response));
            }

            @Override
            public void onError(Throwable throwable) {
                errorRef.set(throwable);
                finishLatch.countDown();
            }

            @Override
            public void onCompleted() {
                finishLatch.countDown();
            }
        };
    }

    private void awaitStreamCompletion(CountDownLatch finishLatch, AtomicReference<Throwable> errorRef, String operationName) {
        try {
            if (!finishLatch.await(STREAM_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw Status.DEADLINE_EXCEEDED
                        .withDescription("Timed out waiting for gRPC stream " + operationName)
                        .asRuntimeException();
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw Status.CANCELLED.withDescription("Interrupted during gRPC stream " + operationName).asRuntimeException();
        }
        Throwable streamError = errorRef.get();
        if (streamError != null) {
            if (streamError instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("gRPC stream failed in " + operationName, streamError);
        }
    }
}

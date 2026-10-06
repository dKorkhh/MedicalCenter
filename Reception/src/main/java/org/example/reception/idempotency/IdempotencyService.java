package org.example.reception.idempotency;

import org.example.reception.exception.ApiProblemException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    public static final String HEADER = "Idempotency-Key";
    private static final Pattern VALID_KEY = Pattern.compile("[A-Za-z0-9._:\\-]{8,128}");

    private final IdempotencyStore store;

    public <R> IdempotentResult<R> execute(String key, Object requestFingerprint, Supplier<R> action,
                                           Function<R, String> idExtractor, Function<String, R> loader) {
        if (key == null || !VALID_KEY.matcher(key).matches()) {
            throw new ApiProblemException(HttpStatus.BAD_REQUEST, "invalid-idempotency-key", "Invalid Idempotency-Key",
                    "Header " + HEADER + " is required and must be 8-128 characters: letters, digits, '.', '_', ':', '-'.");
        }
        String hash = sha256(String.valueOf(requestFingerprint));

        boolean acquired = store.tryAcquire(
                new IdempotencyRecord(key, hash, IdempotencyRecord.IN_PROGRESS, null, Instant.now()));

        if (acquired) {
            try {
                R result = action.get();
                store.complete(key, idExtractor.apply(result));
                return new IdempotentResult<>(result, false);
            } catch (RuntimeException e) {
                store.release(key);
                throw e;
            }
        }

        IdempotencyRecord existing = store.find(key).orElseThrow(() -> new ApiProblemException(
                HttpStatus.CONFLICT, "idempotency-conflict", "Idempotency conflict",
                "The key was just released or expired. Please retry the request."));

        if (!existing.getRequestHash().equals(hash)) {
            throw new ApiProblemException(HttpStatus.UNPROCESSABLE_ENTITY, "idempotency-key-reuse",
                    "Idempotency-Key reuse", "This Idempotency-Key was already used with a different request body.");
        }
        if (!IdempotencyRecord.COMPLETED.equals(existing.getStatus()) || existing.getResourceId() == null) {
            throw new ApiProblemException(HttpStatus.CONFLICT, "request-in-progress", "Request in progress",
                    "A request with this Idempotency-Key is still being processed.");
        }
        return new IdempotentResult<>(loader.apply(existing.getResourceId()), true);
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}

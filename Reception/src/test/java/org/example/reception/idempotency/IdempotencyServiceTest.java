package org.example.reception.idempotency;

import org.example.reception.exception.ApiProblemException;
import org.example.reception.support.InMemoryIdempotencyStore;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdempotencyServiceTest {

    record Resource(String id) {
    }

    private final IdempotencyService service = new IdempotencyService(new InMemoryIdempotencyStore());

    @Test
    void sameKeyAndBodyExecutesActionOnlyOnce() {
        AtomicInteger executions = new AtomicInteger();

        IdempotentResult<Resource> first = service.execute("key-12345678", "body-A",
                () -> new Resource("r-" + executions.incrementAndGet()), Resource::id, Resource::new);
        IdempotentResult<Resource> second = service.execute("key-12345678", "body-A",
                () -> new Resource("r-" + executions.incrementAndGet()), Resource::id, Resource::new);

        assertThat(executions.get()).isEqualTo(1);
        assertThat(first.replayed()).isFalse();
        assertThat(second.replayed()).isTrue();
        assertThat(second.value().id()).isEqualTo(first.value().id());
    }

    @Test
    void sameKeyWithDifferentBodyIsRejectedWith422() {
        service.execute("key-12345678", "body-A", () -> new Resource("r-1"), Resource::id, Resource::new);

        assertThatThrownBy(() -> service.execute("key-12345678", "body-B",
                () -> new Resource("r-2"), Resource::id, Resource::new))
                .isInstanceOfSatisfying(ApiProblemException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
    }

    @Test
    void failedActionReleasesKeySoClientCanRetry() {
        assertThatThrownBy(() -> service.execute("key-12345678", "body-A",
                () -> {
                    throw new IllegalStateException("boom");
                }, Resource::id, Resource::new))
                .isInstanceOf(IllegalStateException.class);

        IdempotentResult<Resource> retry = service.execute("key-12345678", "body-A",
                () -> new Resource("r-ok"), Resource::id, Resource::new);

        assertThat(retry.replayed()).isFalse();
        assertThat(retry.value().id()).isEqualTo("r-ok");
    }

    @Test
    void invalidKeyIsRejectedWith400() {
        assertThatThrownBy(() -> service.execute("short", "body",
                () -> new Resource("r-1"), Resource::id, Resource::new))
                .isInstanceOfSatisfying(ApiProblemException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }
}

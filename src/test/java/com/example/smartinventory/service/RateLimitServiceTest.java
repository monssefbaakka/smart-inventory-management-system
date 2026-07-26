package com.example.smartinventory.service;

import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import com.example.smartinventory.dto.RateLimitDecision;

class RateLimitServiceTest {

    private static final long NANOS_PER_SECOND = 1_000_000_000L;

    private final AtomicLong clock = new AtomicLong();

    private RateLimitService serviceWith(boolean enabled, long requests, long windowSeconds) {
        return new RateLimitService(enabled, requests, windowSeconds, clock::get);
    }

    @Test
    void allowsRequestsUpToTheLimit() {
        RateLimitService service = serviceWith(true, 3, 60);

        assertThat(service.check("user:a").allowed()).isTrue();
        assertThat(service.check("user:a").allowed()).isTrue();

        RateLimitDecision last = service.check("user:a");
        assertThat(last.allowed()).isTrue();
        assertThat(last.remaining()).isZero();
        assertThat(last.limit()).isEqualTo(3);
    }

    @Test
    void rejectsRequestsBeyondTheLimit() {
        RateLimitService service = serviceWith(true, 2, 60);
        service.check("user:a");
        service.check("user:a");

        RateLimitDecision decision = service.check("user:a");

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.remaining()).isZero();
        assertThat(decision.retryAfterSeconds()).isPositive();
    }

    @Test
    void refillsTokensAsTheWindowElapses() {
        RateLimitService service = serviceWith(true, 2, 60);
        service.check("user:a");
        service.check("user:a");
        assertThat(service.check("user:a").allowed()).isFalse();

        clock.addAndGet(30 * NANOS_PER_SECOND);

        assertThat(service.check("user:a").allowed()).isTrue();
        assertThat(service.check("user:a").allowed()).isFalse();
    }

    @Test
    void refillNeverExceedsCapacity() {
        RateLimitService service = serviceWith(true, 2, 60);
        service.check("user:a");
        service.check("user:a");

        clock.addAndGet(10 * 60 * NANOS_PER_SECOND);

        assertThat(service.check("user:a").allowed()).isTrue();
        assertThat(service.check("user:a").allowed()).isTrue();
        assertThat(service.check("user:a").allowed()).isFalse();
    }

    @Test
    void tracksClientsIndependently() {
        RateLimitService service = serviceWith(true, 1, 60);
        service.check("user:a");

        assertThat(service.check("user:a").allowed()).isFalse();
        assertThat(service.check("ip:127.0.0.1").allowed()).isTrue();
    }

    @Test
    void allowsEveryRequestWhenDisabled() {
        RateLimitService service = serviceWith(false, 1, 60);

        assertThat(service.isEnabled()).isFalse();
        assertThat(service.check("user:a").allowed()).isTrue();
        assertThat(service.check("user:a").allowed()).isTrue();
        assertThat(service.check("user:a").allowed()).isTrue();
    }

    @Test
    void clampsNonPositiveConfigurationToUsableValues() {
        RateLimitService service = serviceWith(true, 0, 0);

        RateLimitDecision first = service.check("user:a");

        assertThat(first.allowed()).isTrue();
        assertThat(first.limit()).isEqualTo(1);
        assertThat(service.check("user:a").allowed()).isFalse();
    }

}

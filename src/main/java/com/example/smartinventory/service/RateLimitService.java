package com.example.smartinventory.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.smartinventory.dto.RateLimitDecision;

/**
 * In-memory token-bucket rate limiter, keyed by client identity.
 *
 * <p>Each client gets a bucket holding {@code rate-limit.requests} tokens that refills at a
 * constant rate over {@code rate-limit.window-seconds}, so a client may burst up to the full
 * limit and then settles into a steady request rate. State lives in this JVM only; a clustered
 * deployment would need a shared store.
 */
@Service
public class RateLimitService {

    private static final long NANOS_PER_SECOND = 1_000_000_000L;

    /** Bucket count above which idle buckets are purged, bounding memory use. */
    private static final int MAX_TRACKED_CLIENTS = 10_000;

    /** Idle windows after which a bucket is considered stale and may be purged. */
    private static final int STALE_WINDOWS = 2;

    private final boolean enabled;

    private final long capacity;

    private final long windowSeconds;

    private final LongSupplier nanoTime;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Creates the limiter from configuration.
     *
     * @param enabled       whether rate limiting is applied at all
     * @param requests      maximum requests allowed per client per window (clamped to at least 1)
     * @param windowSeconds length of the refill window in seconds (clamped to at least 1)
     */
    @Autowired
    public RateLimitService(@Value("${rate-limit.enabled:true}") boolean enabled,
            @Value("${rate-limit.requests:100}") long requests,
            @Value("${rate-limit.window-seconds:60}") long windowSeconds) {
        this(enabled, requests, windowSeconds, System::nanoTime);
    }

    RateLimitService(boolean enabled, long requests, long windowSeconds, LongSupplier nanoTime) {
        this.enabled = enabled;
        this.capacity = Math.max(1L, requests);
        this.windowSeconds = Math.max(1L, windowSeconds);
        this.nanoTime = nanoTime;
    }

    /**
     * Reports whether rate limiting is switched on.
     *
     * @return {@code true} when requests are being limited
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Consumes one token for the given client and reports whether the request may proceed.
     *
     * @param clientKey identity of the caller (username or remote address)
     * @return the rate-limit decision for this request
     */
    public RateLimitDecision check(String clientKey) {
        if (!enabled) {
            return RateLimitDecision.allowed(capacity, capacity);
        }
        purgeStaleBucketsIfCrowded();
        Bucket bucket = buckets.computeIfAbsent(clientKey, key -> new Bucket(capacity, nanoTime.getAsLong()));
        return bucket.tryConsume(capacity, nanosPerToken(), nanoTime.getAsLong());
    }

    private double nanosPerToken() {
        return (double) windowSeconds * NANOS_PER_SECOND / capacity;
    }

    private void purgeStaleBucketsIfCrowded() {
        if (buckets.size() <= MAX_TRACKED_CLIENTS) {
            return;
        }
        long cutoffNanos = nanoTime.getAsLong() - STALE_WINDOWS * windowSeconds * NANOS_PER_SECOND;
        buckets.values().removeIf(bucket -> bucket.lastRefillNanos() - cutoffNanos < 0);
    }

    /** Token bucket for a single client; all state changes happen under the instance monitor. */
    private static final class Bucket {

        private double tokens;

        private long lastRefillNanos;

        private Bucket(long capacity, long nowNanos) {
            this.tokens = capacity;
            this.lastRefillNanos = nowNanos;
        }

        private synchronized RateLimitDecision tryConsume(long capacity, double nanosPerToken, long nowNanos) {
            refill(capacity, nanosPerToken, nowNanos);
            if (tokens >= 1.0d) {
                tokens -= 1.0d;
                return RateLimitDecision.allowed(capacity, (long) Math.floor(tokens));
            }
            double nanosUntilNextToken = (1.0d - tokens) * nanosPerToken;
            long retryAfterSeconds = Math.max(1L, (long) Math.ceil(nanosUntilNextToken / NANOS_PER_SECOND));
            return RateLimitDecision.rejected(capacity, retryAfterSeconds);
        }

        private synchronized long lastRefillNanos() {
            return lastRefillNanos;
        }

        private void refill(long capacity, double nanosPerToken, long nowNanos) {
            long elapsedNanos = nowNanos - lastRefillNanos;
            lastRefillNanos = nowNanos;
            if (elapsedNanos <= 0) {
                return;
            }
            tokens = Math.min(capacity, tokens + elapsedNanos / nanosPerToken);
        }

    }

}

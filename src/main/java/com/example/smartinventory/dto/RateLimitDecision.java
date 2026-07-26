package com.example.smartinventory.dto;

/**
 * Outcome of a rate-limit check for a single request.
 *
 * @param allowed           whether the request may proceed
 * @param limit             maximum number of requests allowed per window
 * @param remaining         requests still available in the current window
 * @param retryAfterSeconds seconds until the next request is allowed; {@code 0} when allowed
 */
public record RateLimitDecision(boolean allowed, long limit, long remaining, long retryAfterSeconds) {

    /**
     * Creates a decision allowing the request.
     *
     * @param limit     maximum number of requests allowed per window
     * @param remaining requests still available in the current window
     * @return an allowing decision
     */
    public static RateLimitDecision allowed(long limit, long remaining) {
        return new RateLimitDecision(true, limit, remaining, 0);
    }

    /**
     * Creates a decision rejecting the request.
     *
     * @param limit             maximum number of requests allowed per window
     * @param retryAfterSeconds seconds until the next request is allowed
     * @return a rejecting decision
     */
    public static RateLimitDecision rejected(long limit, long retryAfterSeconds) {
        return new RateLimitDecision(false, limit, 0, retryAfterSeconds);
    }

}

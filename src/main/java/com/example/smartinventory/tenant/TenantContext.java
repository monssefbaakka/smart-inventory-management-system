package com.example.smartinventory.tenant;

/**
 * Holds the tenant that the current thread is working on behalf of.
 *
 * <p>{@link TenantFilter} sets the value once a request has been authenticated and clears it again
 * when the request completes; {@link TenantIdentifierResolver} reads it for every Hibernate
 * statement. Work running outside a request (startup, scheduled jobs) sees no value and falls back
 * to the configured default tenant.
 */
public final class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
    }

    /**
     * Binds a tenant to the current thread.
     *
     * @param tenantId the tenant slug to work on behalf of
     */
    public static void setTenantId(String tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    /**
     * Returns the tenant bound to the current thread.
     *
     * @return the tenant slug, or {@code null} when the thread is not serving a tenant
     */
    public static String getTenantId() {
        return CURRENT_TENANT.get();
    }

    /** Unbinds the tenant from the current thread, so pooled threads never leak it. */
    public static void clear() {
        CURRENT_TENANT.remove();
    }

}

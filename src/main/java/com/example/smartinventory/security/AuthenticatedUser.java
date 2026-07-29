package com.example.smartinventory.security;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import lombok.Getter;

/**
 * {@link org.springframework.security.core.userdetails.UserDetails} carrying the tenant the account
 * belongs to, so the request can be scoped to that tenant once authentication succeeds.
 */
@Getter
public class AuthenticatedUser extends User {

    private final String tenantId;

    /**
     * Creates an authenticated principal bound to a tenant.
     *
     * @param username    the account's email address
     * @param password    the stored password hash
     * @param tenantId    slug of the tenant owning the account
     * @param authorities the roles granted to the account
     */
    public AuthenticatedUser(String username, String password, String tenantId,
            Collection<? extends GrantedAuthority> authorities) {
        super(username, password, authorities);
        this.tenantId = tenantId;
    }

}

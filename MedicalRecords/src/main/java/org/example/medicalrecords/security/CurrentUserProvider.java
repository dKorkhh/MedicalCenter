package org.example.medicalrecords.security;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserProvider {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_DOCTOR = "ROLE_DOCTOR";

    public String currentUserId() {
        return requireAuthentication().getName();
    }

    public boolean isAdmin() {
        return hasAuthority(ROLE_ADMIN);
    }

    public boolean isDoctor() {
        return hasAuthority(ROLE_DOCTOR);
    }

    private boolean hasAuthority(String authority) {
        return requireAuthentication().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority::equals);
    }

    private Authentication requireAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthenticationCredentialsNotFoundException("Authentication is required");
        }
        return authentication;
    }
}

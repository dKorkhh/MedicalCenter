package org.example.notification.security;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RoleExpressions {

    public static final String ADMIN = "hasRole('ADMIN')";
}

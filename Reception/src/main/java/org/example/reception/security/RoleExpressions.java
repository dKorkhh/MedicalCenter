package org.example.reception.security;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RoleExpressions {

    public static final String ADMIN = "hasRole('ADMIN')";
    public static final String PATIENT_OR_ADMIN = "hasAnyRole('PATIENT', 'ADMIN')";
    public static final String PATIENT_DOCTOR_OR_ADMIN = "hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')";
    public static final String DOCTOR_OR_ADMIN = "hasAnyRole('DOCTOR', 'ADMIN')";
}

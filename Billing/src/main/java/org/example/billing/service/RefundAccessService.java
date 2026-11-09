package org.example.billing.service;

import lombok.RequiredArgsConstructor;
import org.example.billing.dto.RefundResponse;
import org.example.billing.security.CurrentUserProvider;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RefundAccessService {

    private final RefundService refundService;
    private final CurrentUserProvider currentUserProvider;

    public RefundResponse getRefundVisibleToCurrentUser(String id) {
        RefundResponse refund = refundService.getRefundById(id);
        if (!currentUserProvider.isAdmin() && !currentUserProvider.currentUserId().equals(refund.patientId())) {
            throw new AccessDeniedException("Refund " + refund.id() + " does not belong to the current user");
        }
        return refund;
    }

    public List<RefundResponse> getRefundsVisibleToCurrentUser(String requestedPatientId) {
        if (!currentUserProvider.isAdmin()) {
            return refundService.getRefundsByPatientId(currentUserProvider.currentUserId());
        }
        return (requestedPatientId == null || requestedPatientId.isBlank())
                ? refundService.getAllRefunds()
                : refundService.getRefundsByPatientId(requestedPatientId);
    }
}

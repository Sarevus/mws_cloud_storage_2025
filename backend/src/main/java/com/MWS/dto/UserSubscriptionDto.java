package com.MWS.dto;

import com.MWS.model.SubscriptionPlan;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserSubscriptionDto(
        UUID id,
        UUID userId,
        SubscriptionPlan plan,
        LocalDateTime startedAt,
        LocalDateTime expiresAt,
        Boolean isActive,
        String billingPeriod,
        Boolean autoRenew,
        LocalDateTime lastPaymentAt,
        LocalDateTime nextPaymentAt
) {
}

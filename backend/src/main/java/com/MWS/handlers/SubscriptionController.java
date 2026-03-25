package com.MWS.handlers;

import com.MWS.dto.UserSubscriptionDto;
import com.MWS.model.SubscriptionPlan;
import com.MWS.model.UserSubscription;
import com.MWS.service.SubscriptionPlanService;
import com.MWS.service.UserSubscriptionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private final SubscriptionPlanService subscriptionPlanService;
    private final UserSubscriptionService userSubscriptionService;

    @Autowired
    public SubscriptionController(SubscriptionPlanService subscriptionPlanService, UserSubscriptionService userSubscriptionService) {
        this.subscriptionPlanService = subscriptionPlanService;
        this.userSubscriptionService = userSubscriptionService;
    }

    @GetMapping("/plans")
    public ResponseEntity<List<SubscriptionPlan>> getAllPlans(HttpSession session) {
        UUID userId = (UUID) session.getAttribute("userId");

        if (userId == null) {
            throw new IllegalArgumentException("Id не может быть null");
        }

        return ResponseEntity.ok(subscriptionPlanService.getAllPlans());
    }

    @PostMapping("/subscribe")
    public ResponseEntity<UserSubscriptionDto> subscribe(
            @RequestParam UUID planId,
            @RequestParam UserSubscription.BillingPeriod period,
            HttpSession session) {

        UUID userId = (UUID) session.getAttribute("userId");
        if (userId == null) {
            throw new IllegalArgumentException("Пользователь не авторизован");
        }

        UserSubscriptionDto subscription = subscriptionPlanService.subscribe(userId, planId, period);
        return ResponseEntity.ok(subscription);
    }

    @GetMapping("/current")
    public ResponseEntity<UserSubscriptionDto> getCurrentSubscription(HttpSession session) {
        UUID userId = (UUID) session.getAttribute("userId");
        if (userId == null) {
            throw new IllegalArgumentException("Пользователь не авторизован");
        }

        return ResponseEntity.ok().body(userSubscriptionService.getCurrentSubscription(userId));
    }
}
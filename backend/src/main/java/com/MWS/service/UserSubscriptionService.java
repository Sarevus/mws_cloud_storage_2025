package com.MWS.service;

import com.MWS.config.SubscriptionProperties;
import com.MWS.dto.SubscribeMapper;
import com.MWS.dto.UserSubscriptionDto;
import com.MWS.model.SubscriptionPlan;
import com.MWS.model.User;
import com.MWS.model.UserSubscription;
import com.MWS.repository.SubscriptionPlanRepository;
import com.MWS.repository.UserRepository;
import com.MWS.repository.UserSubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class UserSubscriptionService {
    private static final Logger logger = LoggerFactory.getLogger(SubscriptionPlanService.class);

    @Autowired
    private final SubscriptionProperties subscriptionProperties;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final UserRepository userRepository;
    private final SubscribeMapper subscribeMapper;

    public UserSubscriptionService(SubscriptionPlanRepository subscriptionPlanRepository,
                                   SubscriptionProperties subscriptionProperties,
                                   UserRepository userRepository,
                                   UserSubscriptionRepository userSubscriptionRepository,
                                   SubscribeMapper subscribeMapper) {
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.subscriptionProperties = subscriptionProperties;
        this.userRepository = userRepository;
        this.userSubscriptionRepository = userSubscriptionRepository;
        this.subscribeMapper = subscribeMapper;
    }

    public UserSubscriptionDto getCurrentSubscription(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("Такого пользователя не существует: " + userId));

        return userSubscriptionRepository.findByUserIdAndIsActiveTrue(userId)
                .map(this::toDto)
                .orElseGet(() -> {
                    SubscriptionPlan freePlan = subscriptionPlanRepository.findByName("FREE")
                            .orElseThrow(() -> new RuntimeException("FREE план не найден"));
                    return new UserSubscriptionDto(
                            null,
                            userId,
                            freePlan,
                            LocalDateTime.now(),
                            null,
                            true,
                            "LIFETIME",
                            false,
                            null,
                            null
                    );
                });

    }

    private UserSubscriptionDto toDto(UserSubscription userSubscription) {
        if (userSubscription == null) {
            throw new IllegalArgumentException("Не должно быть null");
        }

        return subscribeMapper.toDto(userSubscription);
    }
}

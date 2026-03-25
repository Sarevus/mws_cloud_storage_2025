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

import jakarta.annotation.PostConstruct;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SubscriptionPlanService {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionPlanService.class);

    private final SubscriptionProperties subscriptionProperties;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final UserRepository userRepository;
    private final SubscribeMapper subscribeMapper;

    @Autowired
    public SubscriptionPlanService(SubscriptionPlanRepository subscriptionPlanRepository,
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

    /**
     * Синхронизировать тарифы при старте приложения.
     * Загружаем из ямлика в БД если пустая.
     * Обновляем при изменении.
     */
    @PostConstruct
    public void syncPlans() {
        logger.info("Синхронизация тарифов");

        if (subscriptionPlanRepository.count() == 0) {
            loadAllPlans();
        } else {
            updateAllPlans();
        }

        logger.info("Синхронизация тарифов завершена");
    }

    public void loadAllPlans() {
        subscriptionProperties
                .getPlans()
                .forEach((key, config) -> {
                    SubscriptionPlan plan = createPlanFromConfig(config);
                    subscriptionPlanRepository.save(plan);
                    logger.info("Тариф {} загружен", config.getName());
                });

        logger.info("Все тарифы загружены");
    }

    public void updateAllPlans() {
        int updatedCount = 0;

        for (Map.Entry<String, SubscriptionProperties.PlanConfig> entry : subscriptionProperties.getPlans().entrySet()) {
            String planName = entry.getKey();
            SubscriptionProperties.PlanConfig config = entry.getValue();

            SubscriptionPlan existingPlan = subscriptionPlanRepository.findByName(planName).orElse(null);

            if (existingPlan == null) {
                // Новый тариф
                SubscriptionPlan newPlan = createPlanFromConfig(config);
                subscriptionPlanRepository.save(newPlan);
                logger.info("Добавлен новый тариф: {}", planName);
            } else {
                // Существующий тариф
                boolean hasChanges = false;

                if (!existingPlan.getStorageLimitBytes().equals(config.getStorageLimitBytes())) {
                    existingPlan.setStorageLimitBytes(config.getStorageLimitBytes());
                    hasChanges = true;
                }
                if (existingPlan.getPricePerMonth().compareTo(BigDecimal.valueOf(config.getPricePerMonth())) != 0) {
                    existingPlan.setPricePerMonth(BigDecimal.valueOf(config.getPricePerMonth()));
                    hasChanges = true;
                }
                if (existingPlan.getPricePerYear().compareTo(BigDecimal.valueOf(config.getPricePerYear())) != 0) {
                    existingPlan.setPricePerYear(BigDecimal.valueOf(config.getPricePerYear()));
                    hasChanges = true;
                }
                if (config.getMaxFileSizeBytes() != null &&
                        (existingPlan.getMaxFileSizeBytes() == null ||
                                !existingPlan.getMaxFileSizeBytes().equals(config.getMaxFileSizeBytes()))) {
                    existingPlan.setMaxFileSizeBytes(config.getMaxFileSizeBytes());
                    hasChanges = true;
                }
                if (config.getMaxFilesCount() != null &&
                        (existingPlan.getMaxFilesCount() == null ||
                                !existingPlan.getMaxFilesCount().equals(config.getMaxFilesCount()))) {
                    existingPlan.setMaxFilesCount(config.getMaxFilesCount());
                    hasChanges = true;
                }
                if (existingPlan.getPriority() != config.getPriority()) {
                    existingPlan.setPriority(config.getPriority());
                    hasChanges = true;
                }

                if (config.getCanShareFiles() != null &&
                        (existingPlan.getCanShareFiles() == null ||
                                !existingPlan.getCanShareFiles().equals(config.getCanShareFiles()))) {
                    existingPlan.setCanShareFiles(config.getCanShareFiles());
                    hasChanges = true;
                }

                if (hasChanges) {
                    subscriptionPlanRepository.save(existingPlan);
                    updatedCount++;
                    logger.info("🔄 Обновлён тариф: {}", planName);
                } else {
                    logger.debug("Тариф {} без изменений", planName);
                }
            }
        }

        logger.info("Обновлено {} тарифов", updatedCount);
    }

    public List<SubscriptionPlan> getAllPlans() {
        return subscriptionPlanRepository.findAll();
    }

    private SubscriptionPlan createPlanFromConfig(SubscriptionProperties.PlanConfig config) {
        return SubscriptionPlan.builder()
                .name(config.getName())
                .storageLimitBytes(config.getStorageLimitBytes())
                .pricePerMonth(BigDecimal.valueOf(config.getPricePerMonth()))
                .pricePerYear(BigDecimal.valueOf(config.getPricePerYear()))
                .maxFileSizeBytes(config.getMaxFileSizeBytes())
                .maxFilesCount(config.getMaxFilesCount())
                .canShareFiles(true)
                .canCreateFolders(true)
                .priority(config.getPriority())
                .isActive(true)
                .build();
    }

    @Transactional
    public UserSubscriptionDto subscribe(UUID userId, UUID planId, UserSubscription.BillingPeriod period) {
        SubscriptionPlan plan = subscriptionPlanRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Тарифный план не найден"));

        List<UserSubscription> allSubscriptions = userSubscriptionRepository.findByUserId(userId);
        userSubscriptionRepository.deleteAll(allSubscriptions);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = period == UserSubscription.BillingPeriod.MONTHLY
                ? now.plusMonths(1)
                : now.plusYears(1);

        UserSubscription subscription = UserSubscription.builder()
                .userId(userId)
                .plan(plan)
                .startedAt(now)
                .expiresAt(expiresAt)
                .isActive(true)
                .billingPeriod(period)
                .autoRenew(true)
                .lastPaymentAt(now)
                .nextPaymentAt(expiresAt)
                .build();

        userSubscriptionRepository.save(subscription);

        User user = userRepository.findById(userId).orElseThrow();
        user.setSubscriptionId(planId);
        user.setStorageLimit(plan.getStorageLimitBytes());
        userRepository.updateSubscription(user);

        return subscribeMapper.toDto(subscription);
    }
}
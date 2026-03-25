package com.MWS.service;

import com.MWS.dto.UserStorageDtoInfo;
import com.MWS.model.SubscriptionPlan;
import com.MWS.model.User;
import com.MWS.repository.SubscriptionPlanRepository;
import com.MWS.repository.UserRepository;
import com.MWS.repository.UserStorageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserStorageService {

    private final UserStorageRepository userStorageRepository;
    private final UserRepository userRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;

    @Autowired
    public UserStorageService(UserStorageRepository userStorageRepository,
                              UserRepository userRepository,
                              SubscriptionPlanRepository subscriptionPlanRepository) {
        this.userStorageRepository = userStorageRepository;
        this.userRepository = userRepository;
        this.subscriptionPlanRepository = subscriptionPlanRepository;
    }

    public UserStorageDtoInfo getUserStorage(UUID userId) {
        return userStorageRepository.getUserStorage(userId)
                .orElse(new UserStorageDtoInfo(userId, 0));
    }

    /**
     * Используемое место в байтах
     */
    public long getUsedBytes(UUID userId) {
        return getUserStorage(userId).usedBytes();
    }

    // лимит из подписки пользователя
    public long getUserStorageLimit(UUID userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user != null && user.getSubscriptionId() != null) {
            SubscriptionPlan plan = subscriptionPlanRepository.findById(user.getSubscriptionId()).orElse(null);
            if (plan != null) {
                return plan.getStorageLimitBytes();
            }
        }
        // дефолт - FREE план
        SubscriptionPlan freePlan = subscriptionPlanRepository.findByName("FREE").orElse(null);
        return freePlan != null ? freePlan.getStorageLimitBytes() : 1073741824L;
    }

    /**
     * Свободное место в байтах
     */
    public long getFreeBytes(UUID userId) {
        long used = getUsedBytes(userId);
        long limit = getUserStorageLimit(userId);
        return Math.max(0, limit - used);
    }

    /**
     * Проверить, есть ли место для файла
     */
    public boolean hasEnoughSpace(UUID userId, long fileSize) {
        return getFreeBytes(userId) >= fileSize;
    }

    /**
     * Получить данные для прогресс-бара
     */
    public StorageInfo getStorageInfo(UUID userId) {
        long used = getUsedBytes(userId);
        long total = getUserStorageLimit(userId);
        int percent = total > 0 ? (int) Math.min(100, (used * 100) / total) : 0;

        return new StorageInfo(used, total, percent);
    }

    // сверяемся с подпиской
    public void checkUploadAllowed(UUID userId, long fileSize, int currentFilesCount) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        SubscriptionPlan plan = null;
        if (user.getSubscriptionId() != null) {
            plan = subscriptionPlanRepository.findById(user.getSubscriptionId()).orElse(null);
        }
        if (plan == null) {
            plan = subscriptionPlanRepository.findByName("FREE").orElse(null);
        }
        if (plan == null) return;

        // Проверка размера файла
        if (plan.getMaxFileSizeBytes() != null && fileSize > plan.getMaxFileSizeBytes()) {
            throw new RuntimeException(
                    String.format("Файл слишком большой. Максимальный размер для тарифа %s: %s",
                            plan.getName(), formatFileSize(plan.getMaxFileSizeBytes()))
            );
        }

        // Проверка количества файлов
        if (plan.getMaxFilesCount() != null && currentFilesCount >= plan.getMaxFilesCount()) {
            throw new RuntimeException(
                    String.format("Достигнут лимит количества файлов (%d) для тарифа %s",
                            plan.getMaxFilesCount(), plan.getName())
            );
        }
    }

    private String formatFileSize(long bytes) {
        if (bytes >= 1099511627776L) return String.format("%.1f TB", bytes / 1099511627776.0);
        if (bytes >= 1073741824) return String.format("%.1f GB", bytes / 1073741824.0);
        if (bytes >= 1048576) return String.format("%.1f MB", bytes / 1048576.0);
        if (bytes >= 1024) return String.format("%.1f KB", bytes / 1024.0);
        return bytes + " B";
    }

    public void checkShareAllowed(UUID userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        SubscriptionPlan plan = null;

        if (user.getSubscriptionId() != null) {
            plan = subscriptionPlanRepository.findById(user.getSubscriptionId()).orElse(null);
        }
        if (plan == null) {
            plan = subscriptionPlanRepository.findByName("FREE").orElse(null);
        }
        if (plan == null) return;

        if (plan.getCanShareFiles() != null && !plan.getCanShareFiles()) {
            throw new RuntimeException(
                    String.format("Функция обмена файлами недоступна в тарифе %s. Для обмена файлами необходим платный тариф (BASIC, PREMIUM или BUSINESS).",
                            plan.getName())
            );
        }
    }

    public record StorageInfo(long used, long total, int percent) {
    }
}
package com.MWS.service;

import com.MWS.dto.UserStorageDtoInfo;
import com.MWS.repository.UserStorageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserStorageService {

    private final UserStorageRepository userStorageRepository;

    @Value("${storage.max-size-per-user:10485760}")
    private long maxStoragePerUser;

    @Autowired
    public UserStorageService(UserStorageRepository userStorageRepository) {
        this.userStorageRepository = userStorageRepository;
    }

    /**
     * Получить информацию о хранилище пользователя
     */
    public UserStorageDtoInfo getUserStorage(UUID userId) {
        return userStorageRepository.getUserStorage(userId)
                .orElse(new UserStorageDtoInfo(userId, 0));
    }

    /**
     * Получить используемое место в байтах
     */
    public long getUsedBytes(UUID userId) {
        return getUserStorage(userId).usedBytes();
    }

    /**
     * Получить свободное место в байтах
     */
    public long getFreeBytes(UUID userId) {
        long used = getUsedBytes(userId);
        return maxStoragePerUser - used;
    }

    /**
     * Проверить, достаточно ли места для загрузки файла
     */
    public boolean hasEnoughSpace(UUID userId, long fileSize) {
        return getFreeBytes(userId) >= fileSize;
    }

    /**
     * Получить данные для прогресс-бара
     */
    public StorageInfo getStorageInfo(UUID userId) {
        UserStorageDtoInfo storage = getUserStorage(userId);
        long used = storage.usedBytes();
        int percent = (int) Math.min(100, (used * 100) / maxStoragePerUser);

        return new StorageInfo(used, maxStoragePerUser, percent);
    }

    /**
     * Внутренний класс для данных прогресс-бара
     */
    public record StorageInfo(long used, long total, int percent) {
    }
}
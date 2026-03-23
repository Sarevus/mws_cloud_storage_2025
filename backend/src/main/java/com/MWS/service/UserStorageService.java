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

    public UserStorageDtoInfo getUserStorage(UUID userId) {
        return userStorageRepository.getUserStorage(userId)
                .orElse(new UserStorageDtoInfo(userId, 0));
    }

    //используемое место в байтах
    public long getUsedBytes(UUID userId) {
        return getUserStorage(userId).usedBytes();
    }

    //свободное место в байтах
    public long getFreeBytes(UUID userId) {
        long used = getUsedBytes(userId);
        return maxStoragePerUser - used;
    }

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

    public record StorageInfo(long used, long total, int percent) {
    }
}
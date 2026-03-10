package com.MWS.repository;

import com.MWS.dto.UserStorageDtoInfo;

import java.util.Optional;
import java.util.UUID;

public interface UserStorageRepository {
    Optional<UserStorageDtoInfo> getUserStorage(UUID userId);
}

package com.MWS.dto;

import java.util.UUID;

public record UserStorageDtoInfo(
        UUID userId,
        long usedBytes
) {}
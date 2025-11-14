package com.MWS.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public record FileDto(
        @JsonProperty("user_id") UUID userId,
        @JsonProperty("link") String link
) {}

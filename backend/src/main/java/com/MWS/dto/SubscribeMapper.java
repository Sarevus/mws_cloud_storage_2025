package com.MWS.dto;

import com.MWS.model.UserSubscription;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SubscribeMapper {
    UserSubscriptionDto toDto(UserSubscription userSubscription);
}

package com.deshko.userservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import com.deshko.userservice.dto.PaymentCardRequestDto;
import com.deshko.userservice.dto.PaymentCardResponseDto;
import com.deshko.userservice.entity.PaymentCard;

@Mapper(componentModel = "spring")
public interface PaymentCardMapper {
    @Mapping(target = "userId", source = "user.id")
    PaymentCardResponseDto toDto(PaymentCard card);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    PaymentCard toEntity(PaymentCardRequestDto dto);
}

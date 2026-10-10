package com.deshko.userservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import com.deshko.userservice.dto.UserRequestDto;
import com.deshko.userservice.dto.UserResponseDto;
import com.deshko.userservice.entity.User;

@Mapper(componentModel = "spring", uses = PaymentCardMapper.class)
public interface UserMapper {
    UserResponseDto toDto(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "cards", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User toEntity(UserRequestDto dto);
}

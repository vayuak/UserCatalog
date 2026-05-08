package com.UserCatalogServiceOne.UserCatalog.Others;

import com.UserCatalogServiceOne.UserCatalog.DTOs.UserRegistrationRequest;
import com.UserCatalogServiceOne.UserCatalog.Models.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "phoneNumber", ignore = true)
    @Mapping(target = "bio", ignore = true)
    @Mapping(target = "profilePictureUrl", ignore = true)

    User toEntity(UserRegistrationRequest request);
}
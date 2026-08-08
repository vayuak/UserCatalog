package com.UserCatalogServiceOne.UserCatalog.Others;

import com.UserCatalogServiceOne.UserCatalog.DTOs.UserRegistrationRequest;
import com.UserCatalogServiceOne.UserCatalog.Models.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    // 🟢 FIXED: Removed references to non-existent fields (email, phoneNumber)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)

    @Mapping(target = "profilePictureUrl", ignore = true)
    User toEntity(UserRegistrationRequest request);
}
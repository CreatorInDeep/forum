package com.masterSE.forum.mapper;

import com.masterSE.forum.domain.UserEntity;
import com.masterSE.forum.gen.model.CreateUser;
import com.masterSE.forum.gen.model.UserResponse;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "passwordHash", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "username", expression = "java(createUser.getUsername().trim())")
	@Mapping(target = "role", expression = "java(com.masterSE.forum.domain.UserRole.valueOf(createUser.getRole().name()))")
	UserEntity toNewEntity(CreateUser createUser);

	@Mapping(target = "createdAt", expression = "java(entity.getCreatedAt() == null ? null : java.time.OffsetDateTime.ofInstant(entity.getCreatedAt(), java.time.ZoneOffset.UTC))")
	@Mapping(target = "role", expression = "java(com.masterSE.forum.gen.model.UserRole.valueOf(entity.getRole().name()))")
	UserResponse toResponse(UserEntity entity);

	List<UserResponse> toResponseList(List<UserEntity> entities);
}

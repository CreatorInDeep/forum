package com.masterSE.forum.mapper;

import com.masterSE.forum.domain.UserEntity;
import com.masterSE.forum.gen.model.CreateUser;
import com.masterSE.forum.gen.model.UserResponse;
import com.masterSE.forum.gen.model.UserSummary;

import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Component
public class UserMapper {

	public UserEntity toNewEntity(CreateUser createUser) {
		UserEntity entity = new UserEntity();
		entity.setUsername(createUser.getUsername().trim());
		entity.setEmail(normalizedEmail(createUser.getEmail()));
		entity.setRole(com.masterSE.forum.domain.UserRole.valueOf(createUser.getRole().name()));
		return entity;
	}

	public UserResponse toResponse(UserEntity entity) {
		UserResponse response = new UserResponse();
		response.setId(entity.getId());
		response.setUsername(entity.getUsername());
		response.setEmail(entity.getEmail());
		response.setRole(com.masterSE.forum.gen.model.UserRole.valueOf(entity.getRole().name()));
		response.setCreatedAt(toOffsetDateTime(entity.getCreatedAt()));
		return response;
	}

	public UserSummary toSummary(UserEntity entity) {
		if (entity == null) {
			return null;
		}
		UserSummary summary = new UserSummary();
		summary.setId(entity.getId());
		summary.setUsername(entity.getUsername());
		summary.setRole(com.masterSE.forum.gen.model.UserRole.valueOf(entity.getRole().name()));
		return summary;
	}

	public List<UserResponse> toResponseList(List<UserEntity> entities) {
		return entities.stream()
				.map(this::toResponse)
				.toList();
	}

	public String normalizedEmail(String email) {
		if (email == null) {
			return null;
		}
		String trimmed = email.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private OffsetDateTime toOffsetDateTime(java.time.Instant instant) {
		return instant == null ? null : OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
	}
}

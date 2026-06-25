package com.masterSE.forum.api.dto;

import com.masterSE.forum.gen.model.UserRole;

import java.time.Instant;

public record AuthResponse(
		String tokenType,
		String accessToken,
		Instant expiresAt,
		Long userId,
		String username,
		UserRole role) {
}

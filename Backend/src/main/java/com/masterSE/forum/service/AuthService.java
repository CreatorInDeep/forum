package com.masterSE.forum.service;

import com.masterSE.forum.api.dto.AuthResponse;
import com.masterSE.forum.api.dto.LoginRequest;
import com.masterSE.forum.domain.UserEntity;
import com.masterSE.forum.repository.UserRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtEncoder jwtEncoder;
	private final Duration tokenTtl;

	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtEncoder jwtEncoder,
			@Value("${forum.security.jwt.token-ttl}") Duration tokenTtl) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtEncoder = jwtEncoder;
		this.tokenTtl = tokenTtl;
	}

	@Transactional(readOnly = true)
	public AuthResponse login(LoginRequest request) {
		String username = request.username().trim();
		UserEntity user = userRepository.findByUsername(username)
				.filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPasswordHash()))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));

		Instant issuedAt = Instant.now();
		Instant expiresAt = issuedAt.plus(tokenTtl);
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer("forum")
				.subject(user.getUsername())
				.issuedAt(issuedAt)
				.expiresAt(expiresAt)
				.claim("userId", user.getId())
				.claim("roles", List.of(user.getRole().name()))
				.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

		com.masterSE.forum.gen.model.UserRole responseRole =
				com.masterSE.forum.gen.model.UserRole.valueOf(user.getRole().name());
		return new AuthResponse("Bearer", token, expiresAt, user.getId(), user.getUsername(), responseRole);
	}
}

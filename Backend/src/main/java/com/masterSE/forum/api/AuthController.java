package com.masterSE.forum.api;

import com.masterSE.forum.api.dto.AuthResponse;
import com.masterSE.forum.api.dto.LoginRequest;
import com.masterSE.forum.api.dto.RegisterRequest;
import com.masterSE.forum.gen.model.UserResponse;
import com.masterSE.forum.service.AuthService;
import com.masterSE.forum.service.UserService;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {

	private static final Logger log = LogManager.getLogger(AuthController.class);

	private final AuthService authService;
	private final UserService userService;

	public AuthController(AuthService authService, UserService userService) {
		this.authService = authService;
		this.userService = userService;
	}

	@PostMapping("/login")
	public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
		log.debug("Handling POST /auth/login for username={}", request.username());
		return ResponseEntity.ok(authService.login(request));
	}

	@PostMapping("/register")
	public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
		log.debug("Handling POST /auth/register for username={}", request.username());
		return ResponseEntity.status(HttpStatus.CREATED).body(userService.register(request));
	}
}

package com.masterSE.forum.service;

import com.masterSE.forum.domain.UserEntity;
import com.masterSE.forum.domain.UserRole;
import com.masterSE.forum.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CurrentUserService {

	private final UserRepository userRepository;

	public CurrentUserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public UserEntity requireCurrentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()
				|| "anonymousUser".equals(authentication.getPrincipal())) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required");
		}
		return userRepository.findByUsername(authentication.getName())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User no longer exists"));
	}

	public boolean canEdit(UserEntity currentUser, UserEntity owner) {
		if (currentUser.getRole() == UserRole.ADMIN || currentUser.getRole() == UserRole.MODERATOR) {
			return true;
		}
		return owner != null && owner.getId() != null && owner.getId().equals(currentUser.getId());
	}
}

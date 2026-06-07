package com.masterSE.forum.service;

import com.masterSE.forum.domain.UserEntity;
import com.masterSE.forum.gen.model.CreateUser;
import com.masterSE.forum.gen.model.UpdateUser;
import com.masterSE.forum.gen.model.UserResponse;
import com.masterSE.forum.mapper.UserMapper;
import com.masterSE.forum.repository.UserRepository;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

	private static final Logger log = LogManager.getLogger(UserService.class);

	private final UserRepository userRepository;
	private final UserMapper userMapper;
	private final PasswordEncoder passwordEncoder;

	public UserService(UserRepository userRepository, UserMapper userMapper, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.userMapper = userMapper;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional(readOnly = true)
	public List<UserResponse> findAll() {
		return userMapper.toResponseList(userRepository.findAll());
	}

	@Transactional(readOnly = true)
	public Optional<UserResponse> findById(Long id) {
		return userRepository.findById(id).map(userMapper::toResponse);
	}

	@Transactional
	public UserResponse create(CreateUser createUser) {
		String username = createUser.getUsername().trim();
		if (userRepository.existsByUsername(username)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
		}
		UserEntity entity = userMapper.toNewEntity(createUser);
		entity.setUsername(username);
		entity.setPasswordHash(passwordEncoder.encode(createUser.getPassword()));
		UserEntity saved = userRepository.save(entity);
		log.info("Created user id={}", saved.getId());
		return userMapper.toResponse(saved);
	}

	@Transactional
	public Optional<UserResponse> update(Long id, UpdateUser update) {
		Optional<UserEntity> opt = userRepository.findById(id);
		if (opt.isEmpty()) {
			return Optional.empty();
		}
		UserEntity entity = opt.get();
		if (update.getUsername() != null && !update.getUsername().isBlank()) {
			String next = update.getUsername().trim();
			if (!next.equals(entity.getUsername()) && userRepository.existsByUsername(next)) {
				throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
			}
			entity.setUsername(next);
		}
		if (update.getEmail() != null) {
			String e = update.getEmail().trim();
			entity.setEmail(e.isEmpty() ? null : e);
		}
		if (update.getRole() != null) {
			entity.setRole(com.masterSE.forum.domain.UserRole.valueOf(update.getRole().name()));
		}
		if (update.getPassword() != null && !update.getPassword().isEmpty()) {
			entity.setPasswordHash(passwordEncoder.encode(update.getPassword()));
		}
		UserEntity saved = userRepository.save(entity);
		log.info("Updated user id={}", saved.getId());
		return Optional.of(userMapper.toResponse(saved));
	}

	@Transactional
	public boolean deleteById(Long id) {
		if (!userRepository.existsById(id)) {
			return false;
		}
		userRepository.deleteById(id);
		log.info("Deleted user id={}", id);
		return true;
	}
}

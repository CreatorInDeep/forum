package com.masterSE.forum.api;

import com.masterSE.forum.gen.api.UsersApi;
import com.masterSE.forum.gen.model.CreateUser;
import com.masterSE.forum.gen.model.UpdateUser;
import com.masterSE.forum.gen.model.UserResponse;
import com.masterSE.forum.service.UserService;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import jakarta.validation.Valid;

@RestController
public class UserController implements UsersApi {

	private static final Logger log = LogManager.getLogger(UserController.class);

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@Override
	public ResponseEntity<List<UserResponse>> listUsers() {
		log.debug("Handling GET /user");
		return ResponseEntity.ok(userService.findAll());
	}

	@Override
	public ResponseEntity<UserResponse> getUserById(Long id) {
		log.debug("Handling GET /user/{}", id);
		return userService.findById(id)
				.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.notFound().build());
	}

	@Override
	public ResponseEntity<UserResponse> createUser(@Valid CreateUser createUser) {
		log.debug("Handling POST /user");
		UserResponse created = userService.create(createUser);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	@Override
	public ResponseEntity<UserResponse> updateUser(Long id, @Valid UpdateUser updateUser) {
		log.debug("Handling PUT /user/{}", id);
		return userService.update(id, updateUser)
				.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.notFound().build());
	}

	@Override
	public ResponseEntity<Void> deleteUser(Long id) {
		log.debug("Handling DELETE /user/{}", id);
		if (userService.deleteById(id)) {
			return ResponseEntity.noContent().build();
		}
		return ResponseEntity.notFound().build();
	}
}

package com.masterSE.forum.api;

import com.masterSE.forum.gen.api.PostsApi;
import com.masterSE.forum.gen.model.CreatePost;
import com.masterSE.forum.gen.model.PostResponse;
import com.masterSE.forum.service.PostService;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import jakarta.validation.Valid;

@RestController
public class PostController implements PostsApi {

	private static final Logger log = LogManager.getLogger(PostController.class);

	private final PostService postService;

	public PostController(PostService postService) {
		this.postService = postService;
	}

	@Override
	public ResponseEntity<List<PostResponse>> listPosts() {
		log.debug("Handling GET /post");
		return ResponseEntity.ok(postService.findAll());
	}

	@Override
	public ResponseEntity<PostResponse> getPostById(Long id) {
		log.debug("Handling GET /post/{}", id);
		return postService.findById(id)
				.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.notFound().build());
	}

	@Override
	public ResponseEntity<PostResponse> createPost(@Valid CreatePost createPost) {
		log.debug("Handling POST /post");
		PostResponse created = postService.create(createPost);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}
}

package com.masterSE.forum.api;

import com.masterSE.forum.gen.api.PostsApi;
import com.masterSE.forum.gen.model.CreatePost;
import com.masterSE.forum.gen.model.CreatePostReply;
import com.masterSE.forum.gen.model.PostResponse;
import com.masterSE.forum.gen.model.RepliesPage;
import com.masterSE.forum.gen.model.ReplyResponse;
import com.masterSE.forum.gen.model.UpdatePost;
import com.masterSE.forum.service.ReplyService;
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
	private final ReplyService replyService;

	public PostController(PostService postService, ReplyService replyService) {
		this.postService = postService;
		this.replyService = replyService;
	}

	@Override
	public ResponseEntity<List<PostResponse>> listPosts() {
		log.debug("Handling GET /post");
		return ResponseEntity.ok(postService.findAll());
	}

	@Override
	public ResponseEntity<PostResponse> getPostById(Long id) {
		log.debug("Handling GET /post/{}", id);
		return postService.openById(id)
				.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.notFound().build());
	}

	@Override
	public ResponseEntity<PostResponse> createPost(@Valid CreatePost createPost) {
		log.debug("Handling POST /post");
		PostResponse created = postService.create(createPost);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	@Override
	public ResponseEntity<PostResponse> updatePost(Long id, @Valid UpdatePost updatePost) {
		log.debug("Handling PUT /post/{}", id);
		return postService.update(id, updatePost)
				.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.notFound().build());
	}

	@Override
	public ResponseEntity<RepliesPage> listPostReplies(Long id, Integer page, Integer size) {
		log.debug("Handling GET /post/{}/replies", id);
		return ResponseEntity.ok(replyService.findByPostId(id, page, size));
	}

	@Override
	public ResponseEntity<ReplyResponse> createPostReply(Long id, @Valid CreatePostReply createPostReply) {
		log.debug("Handling POST /post/{}/replies", id);
		ReplyResponse created = replyService.createForPost(id, createPostReply.getContent());
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}
}

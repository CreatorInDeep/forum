package com.masterSE.forum.service;

import com.masterSE.forum.domain.PostEntity;
import com.masterSE.forum.domain.UserEntity;
import com.masterSE.forum.gen.model.CreatePost;
import com.masterSE.forum.gen.model.PostResponse;
import com.masterSE.forum.gen.model.UpdatePost;
import com.masterSE.forum.mapper.PostMapper;
import com.masterSE.forum.repository.PostRepository;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class PostService {

	private static final Logger log = LogManager.getLogger(PostService.class);

	private final PostRepository postRepository;
	private final PostMapper postMapper;
	private final CurrentUserService currentUserService;

	public PostService(PostRepository postRepository, PostMapper postMapper, CurrentUserService currentUserService) {
		this.postRepository = postRepository;
		this.postMapper = postMapper;
		this.currentUserService = currentUserService;
	}

	@Transactional(readOnly = true)
	public List<PostResponse> findAll() {
		log.debug("Finding all posts");
		return postMapper.toResponseList(postRepository.findAll());
	}

	@Transactional(readOnly = true)
	public Optional<PostResponse> findById(Long id) {
		log.debug("Finding post by id={}", id);
		return postRepository.findById(id).map(postMapper::toResponse);
	}

	@Transactional
	public Optional<PostResponse> openById(Long id) {
		log.debug("Opening post by id={}", id);
		if (postRepository.incrementViewCount(id) == 0) {
			return Optional.empty();
		}
		return postRepository.findById(id).map(postMapper::toResponse);
	}

	@Transactional
	public PostResponse create(CreatePost dto) {
		String title = requireText(dto.getTitle(), "Title is required");
		if (postRepository.existsByTitleIgnoreCase(title)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Topic title already exists");
		}
		UserEntity currentUser = currentUserService.requireCurrentUser();
		PostEntity postEntity = postMapper.toEntity(dto);
		postEntity.setTitle(title);
		postEntity.setCreatedBy(currentUser);
		PostEntity saved = postRepository.save(postEntity);
		log.info("Created post id={}", saved.getId());
		return postMapper.toResponse(saved);
	}

	@Transactional
	public Optional<PostResponse> update(Long id, UpdatePost dto) {
		Optional<PostEntity> opt = postRepository.findById(id);
		if (opt.isEmpty()) {
			return Optional.empty();
		}
		PostEntity post = opt.get();
		UserEntity currentUser = currentUserService.requireCurrentUser();
		if (!currentUserService.canEdit(currentUser, post.getCreatedBy())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only owners, moderators, and admins can edit topics");
		}
		if (dto.getTitle() != null) {
			String title = requireText(dto.getTitle(), "Title is required");
			if (!title.equalsIgnoreCase(post.getTitle()) && postRepository.existsByTitleIgnoreCaseAndIdNot(title, id)) {
				throw new ResponseStatusException(HttpStatus.CONFLICT, "Topic title already exists");
			}
			post.setTitle(title);
		}
		if (dto.getContent() != null) {
			post.setContent(requireText(dto.getContent(), "Content is required"));
		}
		PostEntity saved = postRepository.save(post);
		log.info("Updated post id={}", saved.getId());
		return Optional.of(postMapper.toResponse(saved));
	}

	private String requireText(String value, String message) {
		if (value == null || value.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
		}
		return value.trim();
	}
}

package com.masterSE.forum.service;

import com.masterSE.forum.domain.PostEntity;
import com.masterSE.forum.gen.model.CreatePost;
import com.masterSE.forum.gen.model.PostResponse;
import com.masterSE.forum.mapper.PostMapper;
import com.masterSE.forum.repository.PostRepository;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PostService {

	private static final Logger log = LogManager.getLogger(PostService.class);

	private final PostRepository postRepository;
	private final PostMapper postMapper;

	public PostService(PostRepository postRepository, PostMapper postMapper) {
		this.postRepository = postRepository;
		this.postMapper = postMapper;
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
	public PostResponse create(CreatePost dto) {
		PostEntity postEntity = postMapper.toEntity(dto);
		PostEntity saved = postRepository.save(postEntity);
		log.info("Created post id={}", saved.getId());
		return postMapper.toResponse(saved);
	}
}

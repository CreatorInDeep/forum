package com.masterSE.forum.service;

import com.masterSE.forum.domain.PostEntity;
import com.masterSE.forum.domain.ReplyEntity;
import com.masterSE.forum.gen.model.CreateReply;
import com.masterSE.forum.gen.model.ReplyResponse;
import com.masterSE.forum.mapper.ReplyMapper;
import com.masterSE.forum.repository.PostRepository;
import com.masterSE.forum.repository.ReplyRepository;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class ReplyService {

	private static final Logger log = LogManager.getLogger(ReplyService.class);

	private final ReplyRepository replyRepository;
	private final PostRepository postRepository;
	private final ReplyMapper replyMapper;

	public ReplyService(ReplyRepository replyRepository, PostRepository postRepository, ReplyMapper replyMapper) {
		this.replyRepository = replyRepository;
		this.postRepository = postRepository;
		this.replyMapper = replyMapper;
	}

	@Transactional(readOnly = true)
	public List<ReplyResponse> findAll() {
		log.debug("Finding all replies");
		return replyMapper.toResponseList(replyRepository.findAll());
	}

	@Transactional(readOnly = true)
	public Optional<ReplyResponse> findById(Long id) {
		log.debug("Finding reply by id={}", id);
		return replyRepository.findById(id).map(replyMapper::toResponse);
	}

	@Transactional
	public ReplyResponse create(CreateReply dto) {
		PostEntity post = postRepository.findById(dto.getPostId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
		ReplyEntity replyEntity = replyMapper.toEntity(dto);
		replyEntity.setPost(post);
		ReplyEntity saved = replyRepository.save(replyEntity);
		log.info("Created reply id={} for post id={}", saved.getId(), post.getId());
		return replyMapper.toResponse(saved);
	}
}

package com.masterSE.forum.service;

import com.masterSE.forum.domain.PostEntity;
import com.masterSE.forum.domain.ReplyEntity;
import com.masterSE.forum.domain.UserEntity;
import com.masterSE.forum.gen.model.CreateReply;
import com.masterSE.forum.gen.model.RepliesPage;
import com.masterSE.forum.gen.model.ReplyResponse;
import com.masterSE.forum.gen.model.UpdateReply;
import com.masterSE.forum.mapper.ReplyMapper;
import com.masterSE.forum.repository.PostRepository;
import com.masterSE.forum.repository.ReplyRepository;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
	private final CurrentUserService currentUserService;

	public ReplyService(ReplyRepository replyRepository, PostRepository postRepository, ReplyMapper replyMapper,
			CurrentUserService currentUserService) {
		this.replyRepository = replyRepository;
		this.postRepository = postRepository;
		this.replyMapper = replyMapper;
		this.currentUserService = currentUserService;
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

	@Transactional(readOnly = true)
	public RepliesPage findByPostId(Long postId, Integer page, Integer size) {
		if (!postRepository.existsById(postId)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found");
		}
		int safePage = page == null || page < 0 ? 0 : page;
		int safeSize = size == null ? 10 : Math.max(1, Math.min(size, 50));
		Page<ReplyEntity> replies = replyRepository.findByPostIdOrderByCreatedAtAsc(
				postId,
				PageRequest.of(safePage, safeSize));

		RepliesPage response = new RepliesPage();
		response.setPage(replies.getNumber());
		response.setSize(replies.getSize());
		response.setTotalItems(replies.getTotalElements());
		response.setTotalPages(replies.getTotalPages());
		response.setItems(replyMapper.toResponseList(replies.getContent()));
		return response;
	}

	@Transactional
	public ReplyResponse create(CreateReply dto) {
		return createForPost(dto.getPostId(), dto.getContent());
	}

	@Transactional
	public ReplyResponse createForPost(Long postId, String content) {
		PostEntity post = postRepository.findById(postId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found"));
		UserEntity currentUser = currentUserService.requireCurrentUser();
		ReplyEntity replyEntity = new ReplyEntity();
		replyEntity.setContent(requireText(content, "Content is required"));
		replyEntity.setPost(post);
		replyEntity.setCreatedBy(currentUser);
		ReplyEntity saved = replyRepository.save(replyEntity);
		log.info("Created reply id={} for post id={}", saved.getId(), post.getId());
		return replyMapper.toResponse(saved);
	}

	@Transactional
	public Optional<ReplyResponse> update(Long id, UpdateReply dto) {
		Optional<ReplyEntity> opt = replyRepository.findById(id);
		if (opt.isEmpty()) {
			return Optional.empty();
		}
		ReplyEntity reply = opt.get();
		UserEntity currentUser = currentUserService.requireCurrentUser();
		if (!currentUserService.canEdit(currentUser, reply.getCreatedBy())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only owners, moderators, and admins can edit replies");
		}
		reply.setContent(requireText(dto.getContent(), "Content is required"));
		ReplyEntity saved = replyRepository.save(reply);
		log.info("Updated reply id={}", saved.getId());
		return Optional.of(replyMapper.toResponse(saved));
	}

	private String requireText(String value, String message) {
		if (value == null || value.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
		}
		return value.trim();
	}
}

package com.masterSE.forum.api;

import com.masterSE.forum.gen.api.RepliesApi;
import com.masterSE.forum.gen.model.CreateReply;
import com.masterSE.forum.gen.model.ReplyResponse;
import com.masterSE.forum.service.ReplyService;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import jakarta.validation.Valid;

@RestController
public class ReplyController implements RepliesApi {

	private static final Logger log = LogManager.getLogger(ReplyController.class);

	private final ReplyService replyService;

	public ReplyController(ReplyService replyService) {
		this.replyService = replyService;
	}

	@Override
	public ResponseEntity<List<ReplyResponse>> listReplies() {
		log.debug("Handling GET /reply");
		return ResponseEntity.ok(replyService.findAll());
	}

	@Override
	public ResponseEntity<ReplyResponse> getReplyById(Long id) {
		log.debug("Handling GET /reply/{}", id);
		return replyService.findById(id)
				.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.notFound().build());
	}

	@Override
	public ResponseEntity<ReplyResponse> createReply(@Valid CreateReply createReply) {
		log.debug("Handling POST /reply");
		ReplyResponse created = replyService.create(createReply);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}
}

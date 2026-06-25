package com.masterSE.forum.mapper;

import com.masterSE.forum.domain.ReplyEntity;
import com.masterSE.forum.gen.model.CreateReply;
import com.masterSE.forum.gen.model.ReplyResponse;

import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Component
public class ReplyMapper {

	private final UserMapper userMapper;

	public ReplyMapper(UserMapper userMapper) {
		this.userMapper = userMapper;
	}

	public ReplyEntity toEntity(CreateReply createReply) {
		ReplyEntity entity = new ReplyEntity();
		entity.setContent(createReply.getContent().trim());
		return entity;
	}

	public ReplyResponse toResponse(ReplyEntity entity) {
		ReplyResponse response = new ReplyResponse();
		response.setId(entity.getId());
		response.setPostId(entity.getPost() == null ? null : entity.getPost().getId());
		response.setContent(entity.getContent());
		response.setCreatedBy(userMapper.toSummary(entity.getCreatedBy()));
		response.setCreatedAt(toOffsetDateTime(entity.getCreatedAt()));
		response.setUpdatedAt(toOffsetDateTime(entity.getUpdatedAt()));
		return response;
	}

	public List<ReplyResponse> toResponseList(List<ReplyEntity> entities) {
		return entities.stream()
				.map(this::toResponse)
				.toList();
	}

	private OffsetDateTime toOffsetDateTime(java.time.Instant instant) {
		return instant == null ? null : OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
	}
}

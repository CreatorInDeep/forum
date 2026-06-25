package com.masterSE.forum.mapper;

import com.masterSE.forum.domain.PostEntity;
import com.masterSE.forum.gen.model.CreatePost;
import com.masterSE.forum.gen.model.PostResponse;

import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Component
public class PostMapper {

	private final UserMapper userMapper;

	public PostMapper(UserMapper userMapper) {
		this.userMapper = userMapper;
	}

	public PostEntity toEntity(CreatePost createPost) {
		PostEntity entity = new PostEntity();
		entity.setTitle(createPost.getTitle().trim());
		entity.setContent(createPost.getContent().trim());
		return entity;
	}

	public PostResponse toResponse(PostEntity entity) {
		PostResponse response = new PostResponse();
		response.setId(entity.getId());
		response.setTitle(entity.getTitle());
		response.setContent(entity.getContent());
		response.setCreatedBy(userMapper.toSummary(entity.getCreatedBy()));
		response.setCreatedAt(toOffsetDateTime(entity.getCreatedAt()));
		response.setUpdatedAt(toOffsetDateTime(entity.getUpdatedAt()));
		response.setViewCount(entity.getViewCount());
		return response;
	}

	public List<PostResponse> toResponseList(List<PostEntity> entities) {
		return entities.stream()
				.map(this::toResponse)
				.toList();
	}

	private OffsetDateTime toOffsetDateTime(java.time.Instant instant) {
		return instant == null ? null : OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
	}
}

package com.masterSE.forum.mapper;

import com.masterSE.forum.domain.ReplyEntity;
import com.masterSE.forum.gen.model.CreateReply;
import com.masterSE.forum.gen.model.ReplyResponse;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ReplyMapper {

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "post", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "content", expression = "java(createReply.getContent().trim())")
	ReplyEntity toEntity(CreateReply createReply);

	@Mapping(target = "postId", expression = "java(entity.getPost() == null ? null : entity.getPost().getId())")
	@Mapping(target = "createdAt", expression = "java(entity.getCreatedAt() == null ? null : java.time.OffsetDateTime.ofInstant(entity.getCreatedAt(), java.time.ZoneOffset.UTC))")
	ReplyResponse toResponse(ReplyEntity entity);

	List<ReplyResponse> toResponseList(List<ReplyEntity> entities);
}

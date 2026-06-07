package com.masterSE.forum.mapper;

import com.masterSE.forum.domain.PostEntity;
import com.masterSE.forum.gen.model.CreatePost;
import com.masterSE.forum.gen.model.PostResponse;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PostMapper {

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "title", expression = "java(createPost.getTitle().trim())")
	PostEntity toEntity(CreatePost createPost);

	@Mapping(target = "createdAt", expression = "java(entity.getCreatedAt() == null ? null : java.time.OffsetDateTime.ofInstant(entity.getCreatedAt(), java.time.ZoneOffset.UTC))")
	PostResponse toResponse(PostEntity entity);

	List<PostResponse> toResponseList(List<PostEntity> entities);
}

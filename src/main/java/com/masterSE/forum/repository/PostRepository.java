package com.masterSE.forum.repository;

import com.masterSE.forum.domain.PostEntity;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<PostEntity, Long> {
}

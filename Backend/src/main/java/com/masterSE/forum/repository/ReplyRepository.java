package com.masterSE.forum.repository;

import com.masterSE.forum.domain.ReplyEntity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReplyRepository extends JpaRepository<ReplyEntity, Long> {

	Page<ReplyEntity> findByPostIdOrderByCreatedAtAsc(Long postId, Pageable pageable);
}

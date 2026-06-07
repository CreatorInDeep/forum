package com.masterSE.forum.repository;

import com.masterSE.forum.domain.ReplyEntity;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReplyRepository extends JpaRepository<ReplyEntity, Long> {
}

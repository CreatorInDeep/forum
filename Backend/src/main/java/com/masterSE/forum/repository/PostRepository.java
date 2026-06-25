package com.masterSE.forum.repository;

import com.masterSE.forum.domain.PostEntity;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<PostEntity, Long> {

	boolean existsByTitleIgnoreCase(String title);

	boolean existsByTitleIgnoreCaseAndIdNot(String title, Long id);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update PostEntity post set post.viewCount = coalesce(post.viewCount, 0) + 1 where post.id = :id")
	int incrementViewCount(@Param("id") Long id);
}

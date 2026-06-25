package com.masterSE.forum;

import com.masterSE.forum.support.PostgresIntegrationTestSupport;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ForumApplicationTests extends PostgresIntegrationTestSupport {

	@Test
	void contextLoads() {
	}

}

package com.masterSE.forum.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public abstract class PostgresIntegrationTestSupport {

	private static final String TEST_JWT_SECRET = Base64.getEncoder()
			.encodeToString("01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8));

	protected static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
			.withDatabaseName("forum")
			.withUsername("admin")
			.withPassword("admin");

	static {
		postgres.start();
	}

	@DynamicPropertySource
	static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
		registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
		registry.add("forum.security.jwt.secret", () -> TEST_JWT_SECRET);
	}
}

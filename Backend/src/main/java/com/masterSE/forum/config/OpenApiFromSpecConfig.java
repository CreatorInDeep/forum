package com.masterSE.forum.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
public class OpenApiFromSpecConfig {

	/**
	 * Loads the spec from the classpath without {@code readLocation("classpath:...")}, which uses
	 * {@link java.nio.file.Paths#get} and fails on Windows with {@code InvalidPathException}.
	 */
	@Bean
	public OpenAPI openAPI() throws IOException {
		var resource = new ClassPathResource("openapi/openapi.yaml");
		if (!resource.exists()) {
			throw new IllegalStateException("Missing classpath resource: openapi/openapi.yaml");
		}
		String yaml = new String(resource.getContentAsByteArray(), StandardCharsets.UTF_8);
		var result = new OpenAPIV3Parser().readContents(yaml, null, null);
		if (result.getOpenAPI() == null) {
			throw new IllegalStateException("Failed to parse openapi.yaml: " + result.getMessages());
		}
		return result.getOpenAPI();
	}
}

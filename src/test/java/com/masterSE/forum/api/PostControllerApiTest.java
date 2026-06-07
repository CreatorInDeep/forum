package com.masterSE.forum.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.masterSE.forum.repository.PostRepository;
import com.masterSE.forum.support.PostgresIntegrationTestSupport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PostControllerApiTest extends PostgresIntegrationTestSupport {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PostRepository postRepository;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@BeforeEach
	void cleanDatabase() {
		postRepository.deleteAll();
	}

	@Test
	void listPostsReturnsEmptyArrayWhenDatabaseHasNoPosts() throws Exception {
		mockMvc.perform(get("/post"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$").isEmpty());
	}

	@Test
	void createPostRequiresAuthentication() throws Exception {
		mockMvc.perform(post("/post")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "First post",
								  "content": "Body"
								}
								"""))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void createPostPersistsAndReturnsCreatedPostForAuthenticatedUser() throws Exception {
		String token = loginAsAdmin();

		mockMvc.perform(post("/post")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "  First post  ",
								  "content": "Body"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString(MediaType.APPLICATION_JSON_VALUE)))
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.title").value("First post"))
				.andExpect(jsonPath("$.content").value("Body"))
				.andExpect(jsonPath("$.createdAt").isNotEmpty());

		mockMvc.perform(get("/post"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].title").value("First post"))
				.andExpect(jsonPath("$[0].content").value("Body"));
	}

	private String loginAsAdmin() throws Exception {
		String responseBody = mockMvc.perform(post("/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "username": "admin",
								  "password": "admin123"
								}
								"""))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		JsonNode response = objectMapper.readTree(responseBody);
		return response.get("accessToken").asText();
	}
}

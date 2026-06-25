package com.masterSE.forum.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.masterSE.forum.repository.PostRepository;
import com.masterSE.forum.repository.ReplyRepository;
import com.masterSE.forum.repository.UserRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

	@Autowired
	private ReplyRepository replyRepository;

	@Autowired
	private UserRepository userRepository;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@BeforeEach
	void cleanDatabase() {
		replyRepository.deleteAll();
		postRepository.deleteAll();
		userRepository.findAll().stream()
				.filter(user -> !"admin".equals(user.getUsername()))
				.forEach(userRepository::delete);
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

		String responseBody = mockMvc.perform(post("/post")
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
				.andExpect(jsonPath("$.createdBy.username").value("admin"))
				.andExpect(jsonPath("$.createdAt").isNotEmpty())
				.andExpect(jsonPath("$.updatedAt").isNotEmpty())
				.andExpect(jsonPath("$.viewCount").value(0))
				.andReturn()
				.getResponse()
				.getContentAsString();

		Long postId = objectMapper.readTree(responseBody).get("id").asLong();

		mockMvc.perform(get("/post"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].title").value("First post"))
				.andExpect(jsonPath("$[0].content").value("Body"));

		mockMvc.perform(get("/post/{id}", postId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.viewCount").value(1));

		mockMvc.perform(get("/post/{id}", postId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.viewCount").value(2));
	}

	@Test
	void createPostRequiresUniqueTitle() throws Exception {
		String token = loginAsAdmin();
		createPost(token, "Unique topic", "Body");

		mockMvc.perform(post("/post")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "unique topic",
								  "content": "Other"
								}
								"""))
				.andExpect(status().isConflict());
	}

	@Test
	void registerCreatesStandardUserWhoCanLogin() throws Exception {
		mockMvc.perform(post("/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "username": "user-one",
								  "email": "user-one@example.com",
								  "password": "password123"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.username").value("user-one"))
				.andExpect(jsonPath("$.role").value("user"));

		String token = login("user-one", "password123");
		org.assertj.core.api.Assertions.assertThat(token).isNotBlank();
	}

	@Test
	void repliesForTopicArePaginatedByTenByDefault() throws Exception {
		String token = loginAsAdmin();
		Long postId = createPost(token, "Paged replies", "Body");
		for (int i = 1; i <= 11; i++) {
			createReply(token, postId, "Reply " + i);
		}

		mockMvc.perform(get("/post/{id}/replies", postId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.page").value(0))
				.andExpect(jsonPath("$.size").value(10))
				.andExpect(jsonPath("$.totalItems").value(11))
				.andExpect(jsonPath("$.totalPages").value(2))
				.andExpect(jsonPath("$.items.length()").value(10))
				.andExpect(jsonPath("$.items[0].createdBy.username").value("admin"));

		mockMvc.perform(get("/post/{id}/replies?page=1", postId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.page").value(1))
				.andExpect(jsonPath("$.items.length()").value(1));
	}

	@Test
	void createReplyForTopicUsesTopicPath() throws Exception {
		String token = loginAsAdmin();
		Long postId = createPost(token, "Topic path reply", "Body");

		mockMvc.perform(post("/post/{id}/replies", postId)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "content": "  Reply created inside topic  "
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.postId").value(postId))
				.andExpect(jsonPath("$.content").value("Reply created inside topic"))
				.andExpect(jsonPath("$.createdBy.username").value("admin"))
				.andExpect(jsonPath("$.createdAt").isNotEmpty())
				.andExpect(jsonPath("$.updatedAt").isNotEmpty());

		mockMvc.perform(get("/post/{id}/replies", postId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalItems").value(1))
				.andExpect(jsonPath("$.items[0].content").value("Reply created inside topic"));
	}

	@Test
	void usersCanEditOwnContentAndModeratorsCanEditForeignContent() throws Exception {
		register("owner", "password123");
		register("other", "password123");
		createModerator("moderator");
		String ownerToken = login("owner", "password123");
		String otherToken = login("other", "password123");
		String moderatorToken = login("moderator", "password123");

		Long postId = createPost(ownerToken, "Owner topic", "Owner body");

		mockMvc.perform(put("/post/{id}", postId)
						.header("Authorization", "Bearer " + otherToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "Other edit"
								}
								"""))
				.andExpect(status().isForbidden());

		mockMvc.perform(put("/post/{id}", postId)
						.header("Authorization", "Bearer " + ownerToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "Owner edit"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Owner edit"));

		mockMvc.perform(put("/post/{id}", postId)
						.header("Authorization", "Bearer " + moderatorToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "Moderator edit"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Moderator edit"));

		Long replyId = createReply(ownerToken, postId, "Owner reply");

		mockMvc.perform(put("/reply/{id}", replyId)
						.header("Authorization", "Bearer " + otherToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "content": "Other reply edit"
								}
								"""))
				.andExpect(status().isForbidden());

		mockMvc.perform(put("/reply/{id}", replyId)
						.header("Authorization", "Bearer " + moderatorToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "content": "Moderator reply edit"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").value("Moderator reply edit"));
	}

	private String loginAsAdmin() throws Exception {
		return login("admin", "admin123");
	}

	private String login(String username, String password) throws Exception {
		String responseBody = mockMvc.perform(post("/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "username": "%s",
								  "password": "%s"
								}
								""".formatted(username, password)))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		JsonNode response = objectMapper.readTree(responseBody);
		return response.get("accessToken").asText();
	}

	private void register(String username, String password) throws Exception {
		mockMvc.perform(post("/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "username": "%s",
								  "password": "%s"
								}
								""".formatted(username, password)))
				.andExpect(status().isCreated());
	}

	private void createModerator(String username) throws Exception {
		mockMvc.perform(post("/user")
						.header("Authorization", "Bearer " + loginAsAdmin())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "username": "%s",
								  "role": "moderator",
								  "password": "password123"
								}
								""".formatted(username)))
				.andExpect(status().isCreated());
	}

	private Long createPost(String token, String title, String content) throws Exception {
		String responseBody = mockMvc.perform(post("/post")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "%s",
								  "content": "%s"
								}
								""".formatted(title, content)))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		return objectMapper.readTree(responseBody).get("id").asLong();
	}

	private Long createReply(String token, Long postId, String content) throws Exception {
		String responseBody = mockMvc.perform(post("/reply")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "postId": %d,
								  "content": "%s"
								}
								""".formatted(postId, content)))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		return objectMapper.readTree(responseBody).get("id").asLong();
	}
}

package com.masterSE.forum.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.masterSE.forum.maintenance.MaintenanceService;
import com.masterSE.forum.support.PostgresIntegrationTestSupport;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MaintenanceApiTest extends PostgresIntegrationTestSupport {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MaintenanceService maintenanceService;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@AfterEach
	void finishMaintenance() {
		maintenanceService.finish();
	}

	@Test
	void restoreModeReturns503WithRetryAfterForNormalTraffic() throws Exception {
		startRestore();

		mockMvc.perform(get("/post"))
				.andExpect(status().isServiceUnavailable())
				.andExpect(header().string(HttpHeaders.RETRY_AFTER, matchesPattern("\\d+")))
				.andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
				.andExpect(jsonPath("$.status").value(503))
				.andExpect(jsonPath("$.maintenanceMode").value("RESTORE"))
				.andExpect(jsonPath("$.detail").value("Restoring database"));

		mockMvc.perform(post("/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "username": "admin",
								  "password": "admin123"
								}
								"""))
				.andExpect(status().isServiceUnavailable())
				.andExpect(header().exists(HttpHeaders.RETRY_AFTER));
	}

	@Test
	void maintenanceEndpointsRemainAvailableDuringRestore() throws Exception {
		String token = loginAsAdmin();
		startRestore(token);

		mockMvc.perform(get("/maintenance/status")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.active").value(true))
				.andExpect(jsonPath("$.mode").value("RESTORE"));

		mockMvc.perform(post("/maintenance/finish")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.active").value(false))
				.andExpect(jsonPath("$.mode").value("NONE"));

		mockMvc.perform(get("/post"))
				.andExpect(status().isOk());
	}

	@Test
	void backupModeAllowsReadsButBlocksWrites() throws Exception {
		String token = loginAsAdmin();
		mockMvc.perform(post("/maintenance/backup/start")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "retryAfterSeconds": 120,
								  "message": "Taking a backup"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.mode").value("BACKUP"));

		mockMvc.perform(get("/post"))
				.andExpect(status().isOk());

		mockMvc.perform(post("/post")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "Blocked during backup",
								  "content": "Body"
								}
								"""))
				.andExpect(status().isServiceUnavailable())
				.andExpect(header().string(HttpHeaders.RETRY_AFTER, matchesPattern("\\d+")))
				.andExpect(jsonPath("$.maintenanceMode").value("BACKUP"));
	}

	@Test
	void systemHealthcheckReportsOutOfServiceDuringRestore() throws Exception {
		startRestore();

		mockMvc.perform(get("/healthz"))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.status").value("OUT_OF_SERVICE"));
	}

	private void startRestore() throws Exception {
		startRestore(loginAsAdmin());
	}

	private void startRestore(String token) throws Exception {
		mockMvc.perform(post("/maintenance/restore/start")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "retryAfterSeconds": 120,
								  "message": "Restoring database"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.active").value(true))
				.andExpect(jsonPath("$.mode").value("RESTORE"))
				.andExpect(jsonPath("$.message").value("Restoring database"));
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
				.andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString(MediaType.APPLICATION_JSON_VALUE)))
				.andReturn()
				.getResponse()
				.getContentAsString();

		JsonNode response = objectMapper.readTree(responseBody);
		return response.get("accessToken").asText();
	}
}

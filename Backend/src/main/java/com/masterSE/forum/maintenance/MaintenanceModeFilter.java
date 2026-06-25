package com.masterSE.forum.maintenance;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

public class MaintenanceModeFilter extends OncePerRequestFilter {

	private static final Set<String> MUTATING_METHODS = Set.of(
			HttpMethod.POST.name(),
			HttpMethod.PUT.name(),
			HttpMethod.PATCH.name(),
			HttpMethod.DELETE.name());

	private final MaintenanceService maintenanceService;

	public MaintenanceModeFilter(MaintenanceService maintenanceService) {
		this.maintenanceService = maintenanceService;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		MaintenanceState current = maintenanceService.current();
		if (!shouldBlock(request, current)) {
			filterChain.doFilter(request, response);
			return;
		}

		long retryAfterSeconds = maintenanceService.retryAfterSeconds(current);
		response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
		response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(retryAfterSeconds));
		response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.getWriter().write(problemBody(current, retryAfterSeconds));
	}

	private boolean shouldBlock(HttpServletRequest request, MaintenanceState current) {
		if (!current.active() || bypassPath(request.getRequestURI())) {
			return false;
		}
		if (current.mode() == MaintenanceMode.RESTORE) {
			return true;
		}
		return current.mode() == MaintenanceMode.BACKUP && MUTATING_METHODS.contains(request.getMethod());
	}

	private boolean bypassPath(String path) {
		return path.equals("/healthz")
				|| path.startsWith("/actuator/health")
				|| path.startsWith("/maintenance/");
	}

	private String problemBody(MaintenanceState current, long retryAfterSeconds) {
		return """
				{"type":"about:blank","title":"Service temporarily unavailable","status":503,"detail":"%s","maintenanceMode":"%s","retryAfterSeconds":%d}
				""".formatted(escapeJson(current.message()), current.mode(), retryAfterSeconds);
	}

	private String escapeJson(String value) {
		if (value == null) {
			return "";
		}
		return value
				.replace("\\", "\\\\")
				.replace("\"", "\\\"")
				.replace("\r", "\\r")
				.replace("\n", "\\n");
	}
}

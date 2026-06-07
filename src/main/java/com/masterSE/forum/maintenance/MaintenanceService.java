package com.masterSE.forum.maintenance;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class MaintenanceService {

	private static final Duration DEFAULT_RETRY_AFTER = Duration.ofMinutes(5);

	private final AtomicReference<MaintenanceState> state = new AtomicReference<>(MaintenanceState.inactive());

	public MaintenanceState current() {
		return state.get();
	}

	public MaintenanceState start(MaintenanceMode mode, Duration retryAfter, String message) {
		if (mode == MaintenanceMode.NONE) {
			return finish();
		}
		Instant now = Instant.now();
		Duration effectiveRetryAfter = retryAfter == null ? DEFAULT_RETRY_AFTER : retryAfter;
		String effectiveMessage = message == null || message.isBlank()
				? defaultMessage(mode)
				: message.trim();
		MaintenanceState next = new MaintenanceState(mode, now, now.plus(effectiveRetryAfter), effectiveMessage);
		state.set(next);
		return next;
	}

	public MaintenanceState finish() {
		MaintenanceState next = MaintenanceState.inactive();
		state.set(next);
		return next;
	}

	public long retryAfterSeconds(MaintenanceState current) {
		if (current.retryAfter() == null) {
			return DEFAULT_RETRY_AFTER.toSeconds();
		}
		long seconds = Duration.between(Instant.now(), current.retryAfter()).toSeconds();
		return Math.max(1, seconds);
	}

	private String defaultMessage(MaintenanceMode mode) {
		if (mode == MaintenanceMode.RESTORE) {
			return "System restore is in progress. Please retry later.";
		}
		return "System backup is in progress. Please retry later.";
	}
}

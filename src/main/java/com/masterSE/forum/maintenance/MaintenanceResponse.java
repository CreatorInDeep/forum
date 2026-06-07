package com.masterSE.forum.maintenance;

import java.time.Instant;

public record MaintenanceResponse(
		boolean active,
		MaintenanceMode mode,
		Instant startedAt,
		Instant retryAfter,
		Long retryAfterSeconds,
		String message) {

	public static MaintenanceResponse from(MaintenanceState state, MaintenanceService maintenanceService) {
		Long retryAfterSeconds = state.active() ? maintenanceService.retryAfterSeconds(state) : null;
		return new MaintenanceResponse(
				state.active(),
				state.mode(),
				state.startedAt(),
				state.retryAfter(),
				retryAfterSeconds,
				state.message());
	}
}

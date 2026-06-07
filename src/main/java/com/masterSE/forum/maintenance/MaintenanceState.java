package com.masterSE.forum.maintenance;

import java.time.Instant;

public record MaintenanceState(
		MaintenanceMode mode,
		Instant startedAt,
		Instant retryAfter,
		String message) {

	public boolean active() {
		return mode != MaintenanceMode.NONE;
	}

	public static MaintenanceState inactive() {
		return new MaintenanceState(MaintenanceMode.NONE, null, null, null);
	}
}

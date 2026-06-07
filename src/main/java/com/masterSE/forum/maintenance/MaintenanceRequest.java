package com.masterSE.forum.maintenance;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record MaintenanceRequest(
		@Min(1) @Max(86400) Long retryAfterSeconds,
		String message) {
}

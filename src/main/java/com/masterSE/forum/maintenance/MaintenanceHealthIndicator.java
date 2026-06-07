package com.masterSE.forum.maintenance;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("maintenanceHealthIndicator")
public class MaintenanceHealthIndicator implements HealthIndicator {

	private final MaintenanceService maintenanceService;

	public MaintenanceHealthIndicator(MaintenanceService maintenanceService) {
		this.maintenanceService = maintenanceService;
	}

	@Override
	public Health health() {
		MaintenanceState current = maintenanceService.current();
		if (current.mode() == MaintenanceMode.RESTORE) {
			return Health.outOfService()
					.withDetail("mode", current.mode())
					.withDetail("retryAfter", current.retryAfter())
					.withDetail("message", current.message())
					.build();
		}
		return Health.up()
				.withDetail("mode", current.mode())
				.build();
	}
}

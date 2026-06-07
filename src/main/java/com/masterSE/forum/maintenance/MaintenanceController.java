package com.masterSE.forum.maintenance;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import java.time.Duration;

@RestController
@RequestMapping("/maintenance")
public class MaintenanceController {

	private final MaintenanceService maintenanceService;

	public MaintenanceController(MaintenanceService maintenanceService) {
		this.maintenanceService = maintenanceService;
	}

	@GetMapping("/status")
	public ResponseEntity<MaintenanceResponse> status() {
		return ResponseEntity.ok(MaintenanceResponse.from(maintenanceService.current(), maintenanceService));
	}

	@PostMapping("/backup/start")
	public ResponseEntity<MaintenanceResponse> startBackup(@Valid @RequestBody(required = false) MaintenanceRequest request) {
		return ResponseEntity.ok(start(MaintenanceMode.BACKUP, request));
	}

	@PostMapping("/restore/start")
	public ResponseEntity<MaintenanceResponse> startRestore(@Valid @RequestBody(required = false) MaintenanceRequest request) {
		return ResponseEntity.ok(start(MaintenanceMode.RESTORE, request));
	}

	@PostMapping("/finish")
	public ResponseEntity<MaintenanceResponse> finish() {
		return ResponseEntity.ok(MaintenanceResponse.from(maintenanceService.finish(), maintenanceService));
	}

	private MaintenanceResponse start(MaintenanceMode mode, MaintenanceRequest request) {
		Duration retryAfter = request == null || request.retryAfterSeconds() == null
				? null
				: Duration.ofSeconds(request.retryAfterSeconds());
		String message = request == null ? null : request.message();
		return MaintenanceResponse.from(maintenanceService.start(mode, retryAfter, message), maintenanceService);
	}
}

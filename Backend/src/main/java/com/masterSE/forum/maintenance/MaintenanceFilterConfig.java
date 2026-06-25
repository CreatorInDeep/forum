package com.masterSE.forum.maintenance;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class MaintenanceFilterConfig {

	@Bean
	public FilterRegistrationBean<MaintenanceModeFilter> maintenanceModeFilter(MaintenanceService maintenanceService) {
		FilterRegistrationBean<MaintenanceModeFilter> registration = new FilterRegistrationBean<>();
		registration.setFilter(new MaintenanceModeFilter(maintenanceService));
		registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
		registration.addUrlPatterns("/*");
		return registration;
	}
}

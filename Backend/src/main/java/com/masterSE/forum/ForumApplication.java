package com.masterSE.forum;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication
@EnableMethodSecurity
public class ForumApplication {

	private static final Logger log = LogManager.getLogger(ForumApplication.class);

	public static void main(String[] args) {
		log.info("Starting {}", ForumApplication.class.getSimpleName());
		SpringApplication.run(ForumApplication.class, args);
	}

}

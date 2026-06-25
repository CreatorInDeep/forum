package com.masterSE.forum.config;

import com.masterSE.forum.domain.UserRole;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.security.oauth2.jwt.JwtException;

import java.util.Base64;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http,
			JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
						.requestMatchers(HttpMethod.POST, "/auth/register").permitAll()
						.requestMatchers("/scalar.html", "/openapi.yaml", "/v3/api-docs/**").permitAll()
						.requestMatchers("/actuator/health", "/actuator/health/**", "/healthz").permitAll()
						.requestMatchers("/maintenance/**").hasRole(UserRole.ADMIN.name())
						.requestMatchers(HttpMethod.GET, "/post", "/post/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/reply", "/reply/**").permitAll()
						.requestMatchers("/user", "/user/**").hasRole(UserRole.ADMIN.name())
						.requestMatchers(HttpMethod.POST, "/post", "/post/*/replies", "/reply").hasAnyRole(
								UserRole.ADMIN.name(),
								UserRole.MODERATOR.name(),
								UserRole.USER.name())
						.requestMatchers(HttpMethod.PUT, "/post/**", "/reply/**").hasAnyRole(
								UserRole.ADMIN.name(),
								UserRole.MODERATOR.name(),
								UserRole.USER.name())
						.anyRequest().authenticated())
				.oauth2ResourceServer(oauth2 -> oauth2
						.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));

		return http.build();
	}

	@Bean
	public JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
		grantedAuthoritiesConverter.setAuthoritiesClaimName("roles");
		grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");

		JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
		jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
		return jwtAuthenticationConverter;
	}

	@Bean
	public JwtEncoder jwtEncoder(@Value("${forum.security.jwt.secret}") String secret) {
		return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey(secret)));
	}

	@Bean
	public JwtDecoder jwtDecoder(@Value("${forum.security.jwt.secret}") String secret) {
		return NimbusJwtDecoder.withSecretKey(jwtSecretKey(secret))
				.macAlgorithm(MacAlgorithm.HS256)
				.build();
	}

	private SecretKey jwtSecretKey(String secret) {
		if (!StringUtils.hasText(secret)) {
			throw new JwtException("JWT secret must not be blank");
		}
		byte[] bytes = Base64.getDecoder().decode(secret);
		if (bytes.length < 32) {
			throw new JwtException("JWT secret must be at least 256 bits for HS256");
		}
		return new SecretKeySpec(bytes, "HmacSHA256");
	}
}

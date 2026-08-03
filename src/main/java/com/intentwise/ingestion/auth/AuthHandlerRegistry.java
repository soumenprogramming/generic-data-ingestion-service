package com.intentwise.ingestion.auth;

import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.intentwise.ingestion.config.IngestionProperties.AuthConfig;
import com.intentwise.ingestion.config.IngestionProperties.SourceConfig;

@Component
public class AuthHandlerRegistry {

	private final Map<String, AuthHandler> handlers;

	public AuthHandlerRegistry(List<AuthHandler> handlers) {
		this.handlers = handlers.stream()
				.collect(Collectors.toMap(h -> h.type().toLowerCase(), Function.identity()));
	}

	public AuthHandler resolve(String type) {
		AuthHandler handler = handlers.get(type == null ? "none" : type.toLowerCase());
		if (handler == null) {
			throw new IllegalArgumentException("Unsupported auth type: " + type
					+ ". Supported: " + handlers.keySet());
		}
		return handler;
	}

	@Component
	static class NoneAuthHandler implements AuthHandler {
		@Override
		public String type() {
			return "none";
		}

		@Override
		public void apply(HttpRequest.Builder builder, SourceConfig source, AuthConfig auth) {
			// no-op
		}
	}

	@Component
	static class BearerAuthHandler implements AuthHandler {
		@Override
		public String type() {
			return "bearer";
		}

		@Override
		public void validate(AuthConfig auth) {
			if (auth.getValue() == null || auth.getValue().isBlank()) {
				throw new IllegalArgumentException("bearer auth requires auth.value");
			}
		}

		@Override
		public void apply(HttpRequest.Builder builder, SourceConfig source, AuthConfig auth) {
			validate(auth);
			String prefix = auth.getPrefix() == null ? "Bearer " : auth.getPrefix();
			builder.header(auth.getHeaderName(), prefix + auth.getValue());
		}
	}

	@Component
	static class ApiKeyAuthHandler implements AuthHandler {
		@Override
		public String type() {
			return "api_key";
		}

		@Override
		public void validate(AuthConfig auth) {
			if (auth.getValue() == null || auth.getValue().isBlank()) {
				throw new IllegalArgumentException("api_key auth requires auth.value");
			}
			if ((auth.getHeaderName() == null || auth.getHeaderName().isBlank())
					&& (auth.getQueryParam() == null || auth.getQueryParam().isBlank())) {
				throw new IllegalArgumentException("api_key auth requires headerName or queryParam");
			}
		}

		@Override
		public void apply(HttpRequest.Builder builder, SourceConfig source, AuthConfig auth) {
			validate(auth);
			if (auth.getQueryParam() == null || auth.getQueryParam().isBlank()) {
				builder.header(auth.getHeaderName(), auth.getValue());
			}
		}

		@Override
		public void applyQueryParams(Map<String, String> query, AuthConfig auth) {
			if (auth.getQueryParam() != null && !auth.getQueryParam().isBlank()) {
				query.put(auth.getQueryParam(), auth.getValue());
			}
		}
	}

	@Component
	static class BasicAuthHandler implements AuthHandler {
		@Override
		public String type() {
			return "basic";
		}

		@Override
		public void validate(AuthConfig auth) {
			if (auth.getUsername() == null) {
				throw new IllegalArgumentException("basic auth requires auth.username");
			}
		}

		@Override
		public void apply(HttpRequest.Builder builder, SourceConfig source, AuthConfig auth) {
			validate(auth);
			String raw = auth.getUsername() + ":" + (auth.getPassword() == null ? "" : auth.getPassword());
			String encoded = Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
			builder.header("Authorization", "Basic " + encoded);
		}
	}
}

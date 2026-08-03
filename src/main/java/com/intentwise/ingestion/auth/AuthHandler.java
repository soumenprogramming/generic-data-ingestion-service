package com.intentwise.ingestion.auth;

import java.net.http.HttpRequest;
import java.util.Map;

import com.intentwise.ingestion.config.IngestionProperties.AuthConfig;
import com.intentwise.ingestion.config.IngestionProperties.SourceConfig;

/**
 * Strategy for attaching credentials to outbound HTTP requests.
 * New auth styles = new handler + registry entry, not pipeline changes.
 */
public interface AuthHandler {

	String type();

	void apply(HttpRequest.Builder builder, SourceConfig source, AuthConfig auth);

	default void applyQueryParams(Map<String, String> query, AuthConfig auth) {
		// optional: some APIs put keys in the query string
	}

	default void validate(AuthConfig auth) {
		// optional
	}
}

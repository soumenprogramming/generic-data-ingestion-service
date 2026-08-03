package com.intentwise.ingestion.fetch;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.intentwise.ingestion.auth.AuthHandler;
import com.intentwise.ingestion.auth.AuthHandlerRegistry;
import com.intentwise.ingestion.config.IngestionProperties;
import com.intentwise.ingestion.config.IngestionProperties.SourceConfig;

/**
 * HTTP client with timeouts, retries, and auth injection.
 * Treats external APIs as unreliable: retries 429/5xx with backoff.
 */
@Component
public class HttpDataFetcher {

	private static final Logger log = LoggerFactory.getLogger(HttpDataFetcher.class);

	private final HttpClient httpClient;
	private final IngestionProperties properties;
	private final AuthHandlerRegistry authHandlerRegistry;

	public HttpDataFetcher(IngestionProperties properties, AuthHandlerRegistry authHandlerRegistry) {
		this.properties = properties;
		this.authHandlerRegistry = authHandlerRegistry;
		var http = properties.getHttp();
		this.httpClient = HttpClient.newBuilder()
				.connectTimeout(Duration.ofMillis(http.getConnectTimeoutMs()))
				.followRedirects(HttpClient.Redirect.NORMAL)
				.build();
	}

	public FetchResponse fetch(SourceConfig source, URI uri) {
		AuthHandler auth = authHandlerRegistry.resolve(source.getAuth().getType());
		int attempts = Math.max(1, properties.getHttp().getMaxRetries() + 1);
		IOException lastIo = null;
		RuntimeException lastRuntime = null;

		for (int attempt = 1; attempt <= attempts; attempt++) {
			try {
				HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
						.timeout(Duration.ofMillis(properties.getHttp().getReadTimeoutMs()))
						.header("Accept", "application/json")
						.header("User-Agent", properties.getHttp().getUserAgent());

				source.getHeaders().forEach(builder::header);
				auth.apply(builder, source, source.getAuth());

				String method = source.getMethod() == null ? "GET" : source.getMethod().toUpperCase();
				if ("GET".equals(method)) {
					builder.GET();
				}
				else {
					builder.method(method, HttpRequest.BodyPublishers.noBody());
				}

				HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
				int status = response.statusCode();

				if (status == 429 || status >= 500) {
					log.warn("Retryable HTTP {} from {} (attempt {}/{})", status, uri, attempt, attempts);
					sleepBackoff(attempt);
					continue;
				}
				if (status >= 400) {
					throw new FetchException("HTTP " + status + " from " + uri + ": " + truncate(response.body()));
				}
				return new FetchResponse(uri, status, response.body(), response.headers().map());
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				throw new FetchException("Interrupted while fetching " + uri, ex);
			}
			catch (IOException ex) {
				lastIo = ex;
				log.warn("IO error fetching {} (attempt {}/{}): {}", uri, attempt, attempts, ex.getMessage());
				sleepBackoff(attempt);
			}
			catch (FetchException ex) {
				throw ex;
			}
			catch (RuntimeException ex) {
				lastRuntime = ex;
				throw ex;
			}
		}

		if (lastIo != null) {
			throw new FetchException("Failed to fetch " + uri + " after retries", lastIo);
		}
		if (lastRuntime != null) {
			throw lastRuntime;
		}
		throw new FetchException("Failed to fetch " + uri + " after retries");
	}

	public Map<String, String> initialQuery(SourceConfig source) {
		Map<String, String> query = new LinkedHashMap<>(source.getQuery());
		AuthHandler auth = authHandlerRegistry.resolve(source.getAuth().getType());
		auth.applyQueryParams(query, source.getAuth());
		return query;
	}

	private void sleepBackoff(int attempt) {
		try {
			long sleep = properties.getHttp().getRetryBackoffMs() * attempt;
			Thread.sleep(sleep);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}

	private static String truncate(String body) {
		if (body == null) {
			return "";
		}
		return body.length() <= 300 ? body : body.substring(0, 300) + "...";
	}

	public record FetchResponse(URI uri, int statusCode, String body, Map<String, java.util.List<String>> headers) {
		public FetchResponse {
			Objects.requireNonNull(uri);
			Objects.requireNonNull(body);
		}
	}

	public static class FetchException extends RuntimeException {
		public FetchException(String message) {
			super(message);
		}

		public FetchException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}

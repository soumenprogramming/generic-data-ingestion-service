package com.intentwise.ingestion.pagination;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.intentwise.ingestion.config.IngestionProperties.PaginationConfig;
import com.intentwise.ingestion.config.IngestionProperties.SourceConfig;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

@Component
public class PaginationHandlerRegistry {

	private final Map<String, PaginationHandler> handlers;

	public PaginationHandlerRegistry(List<PaginationHandler> handlers) {
		this.handlers = handlers.stream()
				.collect(Collectors.toMap(h -> h.type().toLowerCase(), Function.identity()));
	}

	public PaginationHandler resolve(String type) {
		PaginationHandler handler = handlers.get(type == null ? "none" : type.toLowerCase());
		if (handler == null) {
			throw new IllegalArgumentException("Unsupported pagination type: " + type
					+ ". Supported: " + handlers.keySet());
		}
		return handler;
	}

	static URI buildUri(SourceConfig source, Map<String, String> query) {
		String base = trimTrailingSlash(source.getBaseUrl());
		String path = source.getPath() == null || source.getPath().isBlank() ? "/" : source.getPath();
		if (!path.startsWith("/")) {
			path = "/" + path;
		}
		StringBuilder sb = new StringBuilder(base).append(path);
		if (query != null && !query.isEmpty()) {
			sb.append('?');
			boolean first = true;
			for (Map.Entry<String, String> e : query.entrySet()) {
				if (!first) {
					sb.append('&');
				}
				first = false;
				sb.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8))
						.append('=')
						.append(URLEncoder.encode(e.getValue() == null ? "" : e.getValue(), StandardCharsets.UTF_8));
			}
		}
		return URI.create(sb.toString());
	}

	static String trimTrailingSlash(String url) {
		if (url == null) {
			throw new IllegalArgumentException("baseUrl is required");
		}
		return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
	}

	static void applyApiKeyQuery(SourceConfig source, Map<String, String> query) {
		var auth = source.getAuth();
		if (auth != null
				&& "api_key".equalsIgnoreCase(auth.getType())
				&& auth.getQueryParam() != null
				&& !auth.getQueryParam().isBlank()
				&& auth.getValue() != null) {
			query.put(auth.getQueryParam(), auth.getValue());
		}
	}

	@Component
	static class NonePaginationHandler implements PaginationHandler {
		@Override
		public String type() {
			return "none";
		}

		@Override
		public URI firstPageUri(SourceConfig source, Map<String, String> query) {
			return buildUri(source, query);
		}

		@Override
		public Optional<URI> nextPageUri(SourceConfig source, URI currentUri, String responseBody, int pageIndex) {
			return Optional.empty();
		}
	}

	@Component
	static class PagePaginationHandler implements PaginationHandler {
		@Override
		public String type() {
			return "page";
		}

		@Override
		public URI firstPageUri(SourceConfig source, Map<String, String> query) {
			PaginationConfig p = source.getPagination();
			Map<String, String> q = new LinkedHashMap<>(query);
			q.put(p.getPageParam(), String.valueOf(p.getStartPage()));
			if (p.getSizeParam() != null && !p.getSizeParam().isBlank()) {
				q.put(p.getSizeParam(), String.valueOf(p.getPageSize()));
			}
			return buildUri(source, q);
		}

		@Override
		public Optional<URI> nextPageUri(SourceConfig source, URI currentUri, String responseBody, int pageIndex) {
			PaginationConfig p = source.getPagination();
			if (pageIndex >= p.getMaxPages()) {
				return Optional.empty();
			}
			Map<String, String> q = new LinkedHashMap<>(source.getQuery());
			applyApiKeyQuery(source, q);
			q.put(p.getPageParam(), String.valueOf(p.getStartPage() + pageIndex));
			if (p.getSizeParam() != null && !p.getSizeParam().isBlank()) {
				q.put(p.getSizeParam(), String.valueOf(p.getPageSize()));
			}
			return Optional.of(buildUri(source, q));
		}
	}

	@Component
	static class OffsetPaginationHandler implements PaginationHandler {
		@Override
		public String type() {
			return "offset";
		}

		@Override
		public URI firstPageUri(SourceConfig source, Map<String, String> query) {
			PaginationConfig p = source.getPagination();
			Map<String, String> q = new LinkedHashMap<>(query);
			q.put(p.getOffsetParam(), "0");
			q.put(p.getSizeParam(), String.valueOf(p.getPageSize()));
			return buildUri(source, q);
		}

		@Override
		public Optional<URI> nextPageUri(SourceConfig source, URI currentUri, String responseBody, int pageIndex) {
			PaginationConfig p = source.getPagination();
			if (pageIndex >= p.getMaxPages()) {
				return Optional.empty();
			}
			int offset = pageIndex * p.getPageSize();
			Map<String, String> q = new LinkedHashMap<>(source.getQuery());
			applyApiKeyQuery(source, q);
			q.put(p.getOffsetParam(), String.valueOf(offset));
			q.put(p.getSizeParam(), String.valueOf(p.getPageSize()));
			return Optional.of(buildUri(source, q));
		}
	}

	/**
	 * Follows an absolute or relative "next" URL selected via JsonPath
	 * (e.g. Rick and Morty {@code $.info.next}).
	 */
	@Component
	static class NextUrlPaginationHandler implements PaginationHandler {
		@Override
		public String type() {
			return "next_url";
		}

		@Override
		public URI firstPageUri(SourceConfig source, Map<String, String> query) {
			return buildUri(source, query);
		}

		@Override
		public Optional<URI> nextPageUri(SourceConfig source, URI currentUri, String responseBody, int pageIndex) {
			PaginationConfig p = source.getPagination();
			if (pageIndex >= p.getMaxPages()) {
				return Optional.empty();
			}
			Object next;
			try {
				next = JsonPath.read(responseBody, p.getNextPath());
			}
			catch (PathNotFoundException ex) {
				return Optional.empty();
			}
			if (next == null) {
				return Optional.empty();
			}
			String nextStr = String.valueOf(next).trim();
			if (nextStr.isEmpty() || "null".equalsIgnoreCase(nextStr)) {
				return Optional.empty();
			}
			URI nextUri = URI.create(nextStr);
			if (!nextUri.isAbsolute()) {
				nextUri = currentUri.resolve(nextUri);
			}
			return Optional.of(nextUri);
		}
	}
}

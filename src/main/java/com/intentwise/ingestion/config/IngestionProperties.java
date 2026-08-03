package com.intentwise.ingestion.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Declarative source registry. Adding a new API = adding YAML, not rewriting Java.
 */
@ConfigurationProperties(prefix = "ingestion")
public class IngestionProperties {

	private HttpDefaults http = new HttpDefaults();
	private Map<String, SourceConfig> sources = new LinkedHashMap<>();

	public HttpDefaults getHttp() {
		return http;
	}

	public void setHttp(HttpDefaults http) {
		this.http = http;
	}

	public Map<String, SourceConfig> getSources() {
		return sources;
	}

	public void setSources(Map<String, SourceConfig> sources) {
		this.sources = sources;
	}

	public static class HttpDefaults {
		private int connectTimeoutMs = 5_000;
		private int readTimeoutMs = 30_000;
		private int maxRetries = 3;
		private long retryBackoffMs = 500;
		private String userAgent = "generic-data-ingestion-service/0.1";

		public int getConnectTimeoutMs() {
			return connectTimeoutMs;
		}

		public void setConnectTimeoutMs(int connectTimeoutMs) {
			this.connectTimeoutMs = connectTimeoutMs;
		}

		public int getReadTimeoutMs() {
			return readTimeoutMs;
		}

		public void setReadTimeoutMs(int readTimeoutMs) {
			this.readTimeoutMs = readTimeoutMs;
		}

		public int getMaxRetries() {
			return maxRetries;
		}

		public void setMaxRetries(int maxRetries) {
			this.maxRetries = maxRetries;
		}

		public long getRetryBackoffMs() {
			return retryBackoffMs;
		}

		public void setRetryBackoffMs(long retryBackoffMs) {
			this.retryBackoffMs = retryBackoffMs;
		}

		public String getUserAgent() {
			return userAgent;
		}

		public void setUserAgent(String userAgent) {
			this.userAgent = userAgent;
		}
	}

	public static class SourceConfig {
		private String name;
		private String baseUrl;
		private String path = "/";
		private String method = "GET";
		private Map<String, String> query = new HashMap<>();
		private Map<String, String> headers = new HashMap<>();
		private AuthConfig auth = new AuthConfig();
		private PaginationConfig pagination = new PaginationConfig();
		private ExtractionConfig extraction = new ExtractionConfig();
		private SinkConfig sink = new SinkConfig();

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getBaseUrl() {
			return baseUrl;
		}

		public void setBaseUrl(String baseUrl) {
			this.baseUrl = baseUrl;
		}

		public String getPath() {
			return path;
		}

		public void setPath(String path) {
			this.path = path;
		}

		public String getMethod() {
			return method;
		}

		public void setMethod(String method) {
			this.method = method;
		}

		public Map<String, String> getQuery() {
			return query;
		}

		public void setQuery(Map<String, String> query) {
			this.query = query;
		}

		public Map<String, String> getHeaders() {
			return headers;
		}

		public void setHeaders(Map<String, String> headers) {
			this.headers = headers;
		}

		public AuthConfig getAuth() {
			return auth;
		}

		public void setAuth(AuthConfig auth) {
			this.auth = auth;
		}

		public PaginationConfig getPagination() {
			return pagination;
		}

		public void setPagination(PaginationConfig pagination) {
			this.pagination = pagination;
		}

		public ExtractionConfig getExtraction() {
			return extraction;
		}

		public void setExtraction(ExtractionConfig extraction) {
			this.extraction = extraction;
		}

		public SinkConfig getSink() {
			return sink;
		}

		public void setSink(SinkConfig sink) {
			this.sink = sink;
		}
	}

	public static class AuthConfig {
		/** none | api_key | bearer | basic */
		private String type = "none";
		private String headerName = "Authorization";
		private String queryParam;
		private String value;
		private String username;
		private String password;
		private String prefix = "Bearer ";

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public String getHeaderName() {
			return headerName;
		}

		public void setHeaderName(String headerName) {
			this.headerName = headerName;
		}

		public String getQueryParam() {
			return queryParam;
		}

		public void setQueryParam(String queryParam) {
			this.queryParam = queryParam;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}

		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		public String getPassword() {
			return password;
		}

		public void setPassword(String password) {
			this.password = password;
		}

		public String getPrefix() {
			return prefix;
		}

		public void setPrefix(String prefix) {
			this.prefix = prefix;
		}
	}

	public static class PaginationConfig {
		/**
		 * none | page | offset | next_url
		 * <ul>
		 * <li>page — increments a page query param until empty / max-pages</li>
		 * <li>offset — increments offset by page-size until empty / max-pages</li>
		 * <li>next_url — follows a JsonPath-selected absolute/relative next URL</li>
		 * </ul>
		 */
		private String type = "none";
		private String pageParam = "page";
		private String sizeParam = "limit";
		private String offsetParam = "offset";
		private int startPage = 1;
		private int pageSize = 20;
		private int maxPages = 5;
		private String nextPath = "$.info.next";
		private String resultsPath;

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public String getPageParam() {
			return pageParam;
		}

		public void setPageParam(String pageParam) {
			this.pageParam = pageParam;
		}

		public String getSizeParam() {
			return sizeParam;
		}

		public void setSizeParam(String sizeParam) {
			this.sizeParam = sizeParam;
		}

		public String getOffsetParam() {
			return offsetParam;
		}

		public void setOffsetParam(String offsetParam) {
			this.offsetParam = offsetParam;
		}

		public int getStartPage() {
			return startPage;
		}

		public void setStartPage(int startPage) {
			this.startPage = startPage;
		}

		public int getPageSize() {
			return pageSize;
		}

		public void setPageSize(int pageSize) {
			this.pageSize = pageSize;
		}

		public int getMaxPages() {
			return maxPages;
		}

		public void setMaxPages(int maxPages) {
			this.maxPages = maxPages;
		}

		public String getNextPath() {
			return nextPath;
		}

		public void setNextPath(String nextPath) {
			this.nextPath = nextPath;
		}

		public String getResultsPath() {
			return resultsPath;
		}

		public void setResultsPath(String resultsPath) {
			this.resultsPath = resultsPath;
		}
	}

	public static class ExtractionConfig {
		/** JsonPath to the array of records. Use "$" when the body itself is an array. */
		private String recordsPath = "$";
		private List<String> idFields = new ArrayList<>(List.of("id"));

		public String getRecordsPath() {
			return recordsPath;
		}

		public void setRecordsPath(String recordsPath) {
			this.recordsPath = recordsPath;
		}

		public List<String> getIdFields() {
			return idFields;
		}

		public void setIdFields(List<String> idFields) {
			this.idFields = idFields;
		}
	}

	public static class SinkConfig {
		/** Logical collection / table partition for this source. */
		private String collection;
		/** Replace existing rows for this source on each successful job (demo-friendly). */
		private boolean replaceExisting = true;

		public String getCollection() {
			return collection;
		}

		public void setCollection(String collection) {
			this.collection = collection;
		}

		public boolean isReplaceExisting() {
			return replaceExisting;
		}

		public void setReplaceExisting(boolean replaceExisting) {
			this.replaceExisting = replaceExisting;
		}
	}
}

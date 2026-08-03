package com.intentwise.ingestion.pagination;

import java.net.URI;
import java.util.List;
import java.util.Map;

import com.intentwise.ingestion.config.IngestionProperties.PaginationConfig;
import com.intentwise.ingestion.config.IngestionProperties.SourceConfig;

/**
 * Strategy for walking multi-page API responses.
 */
public interface PaginationHandler {

	String type();

	/**
	 * Build the first request URI for this source.
	 */
	URI firstPageUri(SourceConfig source, Map<String, String> query);

	/**
	 * Given the latest response body and the URI that produced it, return the next page URI,
	 * or empty if pagination is complete.
	 */
	java.util.Optional<URI> nextPageUri(SourceConfig source, URI currentUri, String responseBody, int pageIndex);

	/**
	 * Whether the extracted records indicate an empty terminal page.
	 */
	default boolean isTerminalEmptyPage(List<?> records) {
		return records == null || records.isEmpty();
	}
}

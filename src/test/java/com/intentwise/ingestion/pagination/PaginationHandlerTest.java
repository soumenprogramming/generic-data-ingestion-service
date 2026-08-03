package com.intentwise.ingestion.pagination;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.intentwise.ingestion.config.IngestionProperties.PaginationConfig;
import com.intentwise.ingestion.config.IngestionProperties.SourceConfig;
import com.intentwise.ingestion.pagination.PaginationHandlerRegistry.NextUrlPaginationHandler;
import com.intentwise.ingestion.pagination.PaginationHandlerRegistry.NonePaginationHandler;
import com.intentwise.ingestion.pagination.PaginationHandlerRegistry.PagePaginationHandler;

class PaginationHandlerTest {

	@Test
	void noneHasNoNextPage() {
		NonePaginationHandler handler = new NonePaginationHandler();
		SourceConfig source = source("https://example.com", "/items");
		URI first = handler.firstPageUri(source, Map.of());
		assertEquals("https://example.com/items", first.toString());
		assertTrue(handler.nextPageUri(source, first, "[]", 1).isEmpty());
	}

	@Test
	void pageIncrementsUntilMax() {
		PagePaginationHandler handler = new PagePaginationHandler();
		SourceConfig source = source("https://example.com", "/users");
		PaginationConfig pagination = new PaginationConfig();
		pagination.setType("page");
		pagination.setPageParam("page");
		pagination.setSizeParam("per_page");
		pagination.setStartPage(1);
		pagination.setPageSize(10);
		pagination.setMaxPages(2);
		source.setPagination(pagination);

		URI first = handler.firstPageUri(source, Map.of());
		assertTrue(first.toString().contains("page=1"));
		Optional<URI> second = handler.nextPageUri(source, first, "{}", 1);
		assertTrue(second.isPresent());
		assertTrue(second.get().toString().contains("page=2"));
		assertTrue(handler.nextPageUri(source, second.get(), "{}", 2).isEmpty());
	}

	@Test
	void nextUrlFollowsJsonPath() {
		NextUrlPaginationHandler handler = new NextUrlPaginationHandler();
		SourceConfig source = source("https://rickandmortyapi.com", "/api/character");
		PaginationConfig pagination = new PaginationConfig();
		pagination.setType("next_url");
		pagination.setNextPath("$.info.next");
		pagination.setMaxPages(5);
		source.setPagination(pagination);

		URI first = handler.firstPageUri(source, Map.of());
		String body = "{\"info\":{\"next\":\"https://rickandmortyapi.com/api/character?page=2\"},\"results\":[]}";
		Optional<URI> next = handler.nextPageUri(source, first, body, 1);
		assertEquals(URI.create("https://rickandmortyapi.com/api/character?page=2"), next.orElseThrow());

		String terminal = "{\"info\":{\"next\":null},\"results\":[]}";
		assertTrue(handler.nextPageUri(source, first, terminal, 1).isEmpty());
	}

	private static SourceConfig source(String baseUrl, String path) {
		SourceConfig source = new SourceConfig();
		source.setBaseUrl(baseUrl);
		source.setPath(path);
		return source;
	}
}

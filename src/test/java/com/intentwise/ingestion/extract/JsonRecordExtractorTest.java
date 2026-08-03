package com.intentwise.ingestion.extract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.intentwise.ingestion.config.IngestionProperties.ExtractionConfig;

import tools.jackson.databind.json.JsonMapper;

class JsonRecordExtractorTest {

	private final JsonRecordExtractor extractor = new JsonRecordExtractor(JsonMapper.builder().build());

	@Test
	void extractsRootArray() {
		String body = "[{\"id\":1,\"title\":\"a\"},{\"id\":2,\"title\":\"b\"}]";
		ExtractionConfig config = new ExtractionConfig();
		config.setRecordsPath("$");
		config.setIdFields(List.of("id"));

		List<JsonRecordExtractor.ExtractedRecord> records = extractor.extract(body, config);

		assertEquals(2, records.size());
		assertEquals("1", records.get(0).externalId());
		assertEquals("2", records.get(1).externalId());
	}

	@Test
	void extractsNestedResults() {
		String body = "{\"info\":{\"next\":\"https://example.com/page/2\"},\"results\":[{\"id\":10,\"name\":\"Rick\"}]}";
		ExtractionConfig config = new ExtractionConfig();
		config.setRecordsPath("$.results");
		config.setIdFields(List.of("id"));

		List<JsonRecordExtractor.ExtractedRecord> records = extractor.extract(body, config);

		assertEquals(1, records.size());
		assertEquals("10", records.get(0).externalId());
		assertTrue(records.get(0).payloadJson().contains("Rick"));
	}

	@Test
	void hashesWhenIdMissing() {
		String body = "[{\"name\":\"no-id\"}]";
		ExtractionConfig config = new ExtractionConfig();
		config.setRecordsPath("$");
		config.setIdFields(List.of("id"));

		List<JsonRecordExtractor.ExtractedRecord> records = extractor.extract(body, config);

		assertEquals(1, records.size());
		assertTrue(records.get(0).externalId().startsWith("hash:"));
	}
}

package com.intentwise.ingestion.extract;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.intentwise.ingestion.config.IngestionProperties.ExtractionConfig;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Extracts record maps from heterogeneous JSON payloads via JsonPath.
 */
@Component
public class JsonRecordExtractor {

	private final JsonMapper jsonMapper;
	private final Configuration jsonPathConfig = Configuration.builder()
			.options(Option.SUPPRESS_EXCEPTIONS, Option.ALWAYS_RETURN_LIST)
			.build();

	public JsonRecordExtractor(JsonMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
	}

	@SuppressWarnings("unchecked")
	public List<ExtractedRecord> extract(String responseBody, ExtractionConfig config) {
		List<Object> raw = JsonPath.using(jsonPathConfig).parse(responseBody).read(config.getRecordsPath());
		if (raw == null) {
			return List.of();
		}

		// When recordsPath is "$" and body is an array, JsonPath ALWAYS_RETURN_LIST wraps once.
		// When body is an object and path selects an array, we get that array.
		List<Object> items = flattenIfNestedSingletonList(raw);
		List<ExtractedRecord> records = new ArrayList<>(items.size());

		for (Object item : items) {
			Map<String, Object> asMap;
			if (item instanceof Map<?, ?> map) {
				asMap = (Map<String, Object>) map;
			}
			else {
				asMap = new LinkedHashMap<>();
				asMap.put("value", item);
			}
			String externalId = resolveExternalId(asMap, config.getIdFields());
			String json;
			try {
				json = jsonMapper.writeValueAsString(asMap);
			}
			catch (JacksonException ex) {
				throw new IllegalStateException("Failed to serialize record", ex);
			}
			records.add(new ExtractedRecord(externalId, json, asMap));
		}
		return records;
	}

	private static List<Object> flattenIfNestedSingletonList(List<Object> raw) {
		if (raw.size() == 1 && raw.get(0) instanceof List<?> inner) {
			return new ArrayList<>(inner);
		}
		return raw;
	}

	private String resolveExternalId(Map<String, Object> record, List<String> idFields) {
		if (idFields != null) {
			List<String> parts = new ArrayList<>();
			for (String field : idFields) {
				Object value = record.get(field);
				if (value != null) {
					parts.add(String.valueOf(value));
				}
			}
			if (!parts.isEmpty()) {
				return String.join(":", parts);
			}
		}
		return "hash:" + sha256(record.toString());
	}

	private static String sha256(String input) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash).substring(0, 16);
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException(ex);
		}
	}

	public record ExtractedRecord(String externalId, String payloadJson, Map<String, Object> payload) {
	}
}

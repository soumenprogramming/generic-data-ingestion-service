package com.intentwise.ingestion.api;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.intentwise.ingestion.config.IngestionProperties;
import com.intentwise.ingestion.config.IngestionProperties.SourceConfig;
import com.intentwise.ingestion.persistence.IngestedRecordEntity;
import com.intentwise.ingestion.persistence.IngestedRecordRepository;
import com.intentwise.ingestion.persistence.IngestionJobEntity;
import com.intentwise.ingestion.persistence.IngestionJobRepository;
import com.intentwise.ingestion.pipeline.IngestionPipeline;
import com.intentwise.ingestion.pipeline.IngestionPipeline.UnknownSourceException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@RestController
@RequestMapping("/api/v1")
public class IngestionController {

	private final IngestionPipeline pipeline;
	private final IngestionProperties properties;
	private final IngestionJobRepository jobRepository;
	private final IngestedRecordRepository recordRepository;
	private final JsonMapper jsonMapper;

	public IngestionController(
			IngestionPipeline pipeline,
			IngestionProperties properties,
			IngestionJobRepository jobRepository,
			IngestedRecordRepository recordRepository,
			JsonMapper jsonMapper) {
		this.pipeline = pipeline;
		this.properties = properties;
		this.jobRepository = jobRepository;
		this.recordRepository = recordRepository;
		this.jsonMapper = jsonMapper;
	}

	@GetMapping("/health")
	public Map<String, Object> health() {
		return Map.of(
				"status", "UP",
				"sources", properties.getSources().keySet());
	}

	@GetMapping("/sources")
	public List<Map<String, Object>> listSources() {
		return properties.getSources().entrySet().stream().map(e -> {
			SourceConfig s = e.getValue();
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("id", e.getKey());
			m.put("name", s.getName());
			m.put("baseUrl", s.getBaseUrl());
			m.put("path", s.getPath());
			m.put("authType", s.getAuth().getType());
			m.put("paginationType", s.getPagination().getType());
			m.put("collection", s.getSink().getCollection());
			return m;
		}).toList();
	}

	@PostMapping("/ingest/{sourceId}")
	public ResponseEntity<JobResponse> ingestOne(@PathVariable String sourceId) {
		IngestionJobEntity job = pipeline.run(sourceId);
		return ResponseEntity.accepted().body(JobResponse.from(job));
	}

	@PostMapping("/ingest")
	public ResponseEntity<List<JobResponse>> ingestMany(@Valid @RequestBody IngestRequest request) {
		List<IngestionJobEntity> jobs = pipeline.runAll(request.sourceIds());
		return ResponseEntity.accepted().body(jobs.stream().map(JobResponse::from).toList());
	}

	@GetMapping("/jobs")
	public List<JobResponse> listJobs() {
		return jobRepository.findAll().stream()
				.sorted((a, b) -> b.getStartedAt().compareTo(a.getStartedAt()))
				.map(JobResponse::from)
				.toList();
	}

	@GetMapping("/jobs/{jobId}")
	public ResponseEntity<JobResponse> getJob(@PathVariable UUID jobId) {
		return jobRepository.findById(jobId)
				.map(job -> ResponseEntity.ok(JobResponse.from(job)))
				.orElse(ResponseEntity.notFound().build());
	}

	@GetMapping("/records")
	public List<RecordResponse> listRecords(
			@RequestParam(required = false) String sourceId,
			@RequestParam(required = false) String collection,
			@RequestParam(defaultValue = "20") int limit) {
		int size = Math.min(Math.max(limit, 1), 200);
		List<IngestedRecordEntity> rows;
		if (sourceId != null && collection != null) {
			rows = recordRepository.findBySourceIdAndCollectionOrderByIngestedAtDesc(
					sourceId, collection, PageRequest.of(0, size));
		}
		else if (collection != null) {
			rows = recordRepository.findByCollectionOrderByIngestedAtDesc(collection, PageRequest.of(0, size));
		}
		else {
			rows = recordRepository.findAll(PageRequest.of(0, size)).getContent();
		}
		return rows.stream().map(r -> RecordResponse.from(r, jsonMapper)).toList();
	}

	@ExceptionHandler(UnknownSourceException.class)
	public ResponseEntity<Map<String, String>> unknownSource(UnknownSourceException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException ex) {
		return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
	}

	public record IngestRequest(@NotEmpty List<String> sourceIds) {
	}

	public record JobResponse(
			UUID id,
			String sourceId,
			String sourceName,
			String status,
			String startedAt,
			String finishedAt,
			int pagesFetched,
			int recordsPersisted,
			String errorMessage) {

		static JobResponse from(IngestionJobEntity job) {
			return new JobResponse(
					job.getId(),
					job.getSourceId(),
					job.getSourceName(),
					job.getStatus().name(),
					job.getStartedAt() != null ? job.getStartedAt().toString() : null,
					job.getFinishedAt() != null ? job.getFinishedAt().toString() : null,
					job.getPagesFetched(),
					job.getRecordsPersisted(),
					job.getErrorMessage());
		}
	}

	public record RecordResponse(
			UUID id,
			String sourceId,
			String collection,
			String externalId,
			Object payload,
			String ingestedAt,
			UUID jobId) {

		static RecordResponse from(IngestedRecordEntity entity, JsonMapper mapper) {
			Object payload;
			try {
				payload = mapper.readValue(entity.getPayloadJson(), new TypeReference<Map<String, Object>>() {
				});
			}
			catch (Exception ex) {
				payload = entity.getPayloadJson();
			}
			return new RecordResponse(
					entity.getId(),
					entity.getSourceId(),
					entity.getCollection(),
					entity.getExternalId(),
					payload,
					entity.getIngestedAt() != null ? entity.getIngestedAt().toString() : null,
					entity.getJobId());
		}
	}
}

package com.intentwise.ingestion.pipeline;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.intentwise.ingestion.config.IngestionProperties;
import com.intentwise.ingestion.config.IngestionProperties.SourceConfig;
import com.intentwise.ingestion.extract.JsonRecordExtractor;
import com.intentwise.ingestion.extract.JsonRecordExtractor.ExtractedRecord;
import com.intentwise.ingestion.fetch.HttpDataFetcher;
import com.intentwise.ingestion.fetch.HttpDataFetcher.FetchResponse;
import com.intentwise.ingestion.model.JobStatus;
import com.intentwise.ingestion.pagination.PaginationHandler;
import com.intentwise.ingestion.pagination.PaginationHandlerRegistry;
import com.intentwise.ingestion.persistence.IngestionJobEntity;
import com.intentwise.ingestion.persistence.IngestionJobRepository;
import com.intentwise.ingestion.sink.DataSink;

@Service
public class IngestionPipeline {

	private static final Logger log = LoggerFactory.getLogger(IngestionPipeline.class);

	private final IngestionProperties properties;
	private final HttpDataFetcher fetcher;
	private final PaginationHandlerRegistry paginationRegistry;
	private final JsonRecordExtractor extractor;
	private final DataSink sink;
	private final IngestionJobRepository jobRepository;

	public IngestionPipeline(
			IngestionProperties properties,
			HttpDataFetcher fetcher,
			PaginationHandlerRegistry paginationRegistry,
			JsonRecordExtractor extractor,
			DataSink sink,
			IngestionJobRepository jobRepository) {
		this.properties = properties;
		this.fetcher = fetcher;
		this.paginationRegistry = paginationRegistry;
		this.extractor = extractor;
		this.sink = sink;
		this.jobRepository = jobRepository;
	}

	public IngestionJobEntity run(String sourceId) {
		SourceConfig source = properties.getSources().get(sourceId);
		if (source == null) {
			throw new UnknownSourceException(sourceId);
		}
		return execute(sourceId, source);
	}

	public List<IngestionJobEntity> runAll(List<String> sourceIds) {
		List<IngestionJobEntity> jobs = new ArrayList<>();
		for (String sourceId : sourceIds) {
			jobs.add(run(sourceId));
		}
		return jobs;
	}

	private IngestionJobEntity execute(String sourceId, SourceConfig source) {
		IngestionJobEntity job = new IngestionJobEntity();
		job.setId(UUID.randomUUID());
		job.setSourceId(sourceId);
		job.setSourceName(source.getName() != null ? source.getName() : sourceId);
		job.setStatus(JobStatus.RUNNING);
		job.setStartedAt(Instant.now());
		job.setPagesFetched(0);
		job.setRecordsPersisted(0);
		jobRepository.save(job);

		try {
			PaginationHandler pagination = paginationRegistry.resolve(source.getPagination().getType());
			Map<String, String> query = fetcher.initialQuery(source);
			URI uri = pagination.firstPageUri(source, query);

			List<ExtractedRecord> allRecords = new ArrayList<>();
			int pageIndex = 0;

			while (uri != null) {
				log.info("Fetching source={} page={} uri={}", sourceId, pageIndex + 1, uri);
				FetchResponse response = fetcher.fetch(source, uri);
				List<ExtractedRecord> pageRecords = extractor.extract(response.body(), source.getExtraction());
				job.setPagesFetched(job.getPagesFetched() + 1);

				if (pagination.isTerminalEmptyPage(pageRecords)) {
					break;
				}
				allRecords.addAll(pageRecords);

				pageIndex++;
				Optional<URI> next = pagination.nextPageUri(source, uri, response.body(), pageIndex);
				if (next.isEmpty()) {
					break;
				}
				uri = next.get();
			}

			int persisted = sink.persist(sourceId, source, job.getId(), allRecords);
			job.setRecordsPersisted(persisted);
			job.setStatus(JobStatus.SUCCEEDED);
			job.setFinishedAt(Instant.now());
			log.info("Ingestion succeeded source={} pages={} records={}", sourceId, job.getPagesFetched(), persisted);
			return jobRepository.save(job);
		}
		catch (Exception ex) {
			log.error("Ingestion failed source={}: {}", sourceId, ex.getMessage(), ex);
			job.setStatus(JobStatus.FAILED);
			job.setFinishedAt(Instant.now());
			job.setErrorMessage(ex.getMessage());
			return jobRepository.save(job);
		}
	}

	public static class UnknownSourceException extends RuntimeException {
		public UnknownSourceException(String sourceId) {
			super("Unknown source id: " + sourceId);
		}
	}
}

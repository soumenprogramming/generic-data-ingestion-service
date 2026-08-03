package com.intentwise.ingestion.sink;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.intentwise.ingestion.config.IngestionProperties.SourceConfig;
import com.intentwise.ingestion.extract.JsonRecordExtractor.ExtractedRecord;
import com.intentwise.ingestion.persistence.IngestedRecordEntity;
import com.intentwise.ingestion.persistence.IngestedRecordRepository;

@Component
public class DatabaseSink implements DataSink {

	private final IngestedRecordRepository repository;

	public DatabaseSink(IngestedRecordRepository repository) {
		this.repository = repository;
	}

	@Override
	public String type() {
		return "database";
	}

	@Override
	@Transactional
	public int persist(String sourceId, SourceConfig source, UUID jobId, List<ExtractedRecord> records) {
		String collection = source.getSink().getCollection();
		if (collection == null || collection.isBlank()) {
			collection = sourceId;
		}

		if (source.getSink().isReplaceExisting()) {
			repository.deleteBySourceIdAndCollection(sourceId, collection);
		}

		Instant now = Instant.now();
		List<IngestedRecordEntity> entities = new ArrayList<>(records.size());
		for (ExtractedRecord record : records) {
			IngestedRecordEntity entity = new IngestedRecordEntity();
			entity.setId(UUID.randomUUID());
			entity.setSourceId(sourceId);
			entity.setCollection(collection);
			entity.setExternalId(record.externalId());
			entity.setPayloadJson(record.payloadJson());
			entity.setIngestedAt(now);
			entity.setJobId(jobId);
			entities.add(entity);
		}
		repository.saveAll(entities);
		return entities.size();
	}
}

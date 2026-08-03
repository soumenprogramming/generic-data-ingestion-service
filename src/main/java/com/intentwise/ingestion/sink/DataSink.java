package com.intentwise.ingestion.sink;

import java.util.List;
import java.util.UUID;

import com.intentwise.ingestion.config.IngestionProperties.SourceConfig;
import com.intentwise.ingestion.extract.JsonRecordExtractor.ExtractedRecord;

/**
 * Destination abstraction. Database is the demo sink; S3 (or others) can implement the same contract.
 */
public interface DataSink {

	String type();

	int persist(String sourceId, SourceConfig source, UUID jobId, List<ExtractedRecord> records);
}

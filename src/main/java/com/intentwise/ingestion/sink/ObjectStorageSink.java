package com.intentwise.ingestion.sink;

/**
 * Placeholder showing how a second destination would plug in.
 * Not wired for the demo — implementing {@link DataSink} + a Spring bean is enough.
 */
public class ObjectStorageSink implements DataSink {

	@Override
	public String type() {
		return "s3";
	}

	@Override
	public int persist(String sourceId,
			com.intentwise.ingestion.config.IngestionProperties.SourceConfig source,
			java.util.UUID jobId,
			java.util.List<com.intentwise.ingestion.extract.JsonRecordExtractor.ExtractedRecord> records) {
		throw new UnsupportedOperationException(
				"S3 sink is intentionally unimplemented for the take-home demo. "
						+ "Same DataSink contract — upload JSON lines / parquet under sourceId/collection/jobId.");
	}
}

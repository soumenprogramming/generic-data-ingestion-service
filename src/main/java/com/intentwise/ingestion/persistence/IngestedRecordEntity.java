package com.intentwise.ingestion.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "ingested_records", indexes = {
		@Index(name = "idx_records_source_collection", columnList = "sourceId,collection"),
		@Index(name = "idx_records_external_id", columnList = "sourceId,externalId")
})
public class IngestedRecordEntity {

	@Id
	private UUID id;

	@Column(nullable = false)
	private String sourceId;

	@Column(nullable = false)
	private String collection;

	/** Stable id derived from source payload fields (or a hash fallback). */
	@Column(nullable = false)
	private String externalId;

	@Lob
	@Column(nullable = false, columnDefinition = "TEXT")
	private String payloadJson;

	@Column(nullable = false)
	private Instant ingestedAt;

	private UUID jobId;

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public String getSourceId() {
		return sourceId;
	}

	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

	public String getCollection() {
		return collection;
	}

	public void setCollection(String collection) {
		this.collection = collection;
	}

	public String getExternalId() {
		return externalId;
	}

	public void setExternalId(String externalId) {
		this.externalId = externalId;
	}

	public String getPayloadJson() {
		return payloadJson;
	}

	public void setPayloadJson(String payloadJson) {
		this.payloadJson = payloadJson;
	}

	public Instant getIngestedAt() {
		return ingestedAt;
	}

	public void setIngestedAt(Instant ingestedAt) {
		this.ingestedAt = ingestedAt;
	}

	public UUID getJobId() {
		return jobId;
	}

	public void setJobId(UUID jobId) {
		this.jobId = jobId;
	}
}

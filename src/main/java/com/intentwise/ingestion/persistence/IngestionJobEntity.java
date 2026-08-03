package com.intentwise.ingestion.persistence;

import java.time.Instant;
import java.util.UUID;

import com.intentwise.ingestion.model.JobStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "ingestion_jobs")
public class IngestionJobEntity {

	@Id
	private UUID id;

	@Column(nullable = false)
	private String sourceId;

	@Column(nullable = false)
	private String sourceName;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private JobStatus status;

	@Column(nullable = false)
	private Instant startedAt;

	private Instant finishedAt;

	private int pagesFetched;

	private int recordsPersisted;

	@Lob
	@Column(columnDefinition = "TEXT")
	private String errorMessage;

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

	public String getSourceName() {
		return sourceName;
	}

	public void setSourceName(String sourceName) {
		this.sourceName = sourceName;
	}

	public JobStatus getStatus() {
		return status;
	}

	public void setStatus(JobStatus status) {
		this.status = status;
	}

	public Instant getStartedAt() {
		return startedAt;
	}

	public void setStartedAt(Instant startedAt) {
		this.startedAt = startedAt;
	}

	public Instant getFinishedAt() {
		return finishedAt;
	}

	public void setFinishedAt(Instant finishedAt) {
		this.finishedAt = finishedAt;
	}

	public int getPagesFetched() {
		return pagesFetched;
	}

	public void setPagesFetched(int pagesFetched) {
		this.pagesFetched = pagesFetched;
	}

	public int getRecordsPersisted() {
		return recordsPersisted;
	}

	public void setRecordsPersisted(int recordsPersisted) {
		this.recordsPersisted = recordsPersisted;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}
}

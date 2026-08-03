package com.intentwise.ingestion.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IngestionJobRepository extends JpaRepository<IngestionJobEntity, UUID> {
}

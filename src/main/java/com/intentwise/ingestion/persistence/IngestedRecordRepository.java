package com.intentwise.ingestion.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IngestedRecordRepository extends JpaRepository<IngestedRecordEntity, UUID> {

	List<IngestedRecordEntity> findBySourceIdAndCollectionOrderByIngestedAtDesc(
			String sourceId, String collection, Pageable pageable);

	List<IngestedRecordEntity> findByCollectionOrderByIngestedAtDesc(String collection, Pageable pageable);

	@Modifying(clearAutomatically = true)
	@Query("delete from IngestedRecordEntity r where r.sourceId = :sourceId and r.collection = :collection")
	int deleteBySourceIdAndCollection(@Param("sourceId") String sourceId, @Param("collection") String collection);

	long countBySourceIdAndCollection(String sourceId, String collection);
}

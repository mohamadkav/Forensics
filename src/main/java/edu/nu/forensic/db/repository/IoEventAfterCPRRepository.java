package edu.nu.forensic.db.repository;

import edu.nu.forensic.db.entity.IoEventAfterCPR;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface IoEventAfterCPRRepository extends CrudRepository<IoEventAfterCPR, UUID> {
    @Query(
            value = "select count(distinct predicate_object_path) from io_event where names = 'FileIoRead' and thread_id = ?1 and end_timestamp_nanos <= ?2", nativeQuery = true
    )
    Long countBackwardDependentReadEvents(Integer thread_id, Long end_timestamp_nanos);
    @Query(
            value = "select count(distinct thread_id) from io_event where names = 'FileIoRead' and predicate_object_path = ?1 and start_timestamp_nanos >= ?2", nativeQuery = true
    )
    Long countForwardDependentReadEvents(String predicate_object_path, Long start_timestamp_nanos);

    @Query(
            value = "select count(distinct thread_id) from io_event where names = 'FileIoWrite' and predicate_object_path = ?1 and end_timestamp_nanos <= ?2", nativeQuery = true
    )
    Long countBackwardDependentWriteEvents(String predicate_object_path, Long end_timestamp_nanos);
    @Query(
            value = "select count(distinct predicate_object_path) from io_event where names = 'FileIoWrite' and thread_id = ?1 and start_timestamp_nanos >= ?2", nativeQuery = true
    )
    Long countForwardDependentWriteEvents(Integer thread_id, Long start_timestamp_nanos);
}
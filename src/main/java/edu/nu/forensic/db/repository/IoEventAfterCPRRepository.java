package edu.nu.forensic.db.repository;

import edu.nu.forensic.db.entity.IoEventAfterCPR;
import org.springframework.data.repository.CrudRepository;

import java.util.Set;
import java.util.UUID;

public interface IoEventAfterCPRRepository extends CrudRepository<IoEventAfterCPR, UUID> {
    Set<IoEventAfterCPR> findByNamesEquals(String name);
}
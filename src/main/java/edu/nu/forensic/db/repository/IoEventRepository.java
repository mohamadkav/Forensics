package edu.nu.forensic.db.repository;

import edu.nu.forensic.db.entity.IoEvent;
import org.springframework.data.repository.CrudRepository;

import java.util.Set;
import java.util.UUID;

public interface IoEventRepository extends CrudRepository<IoEvent, UUID> {
    Set<IoEvent> findByNamesEquals(String name);
}
package edu.nu.forensic.db.repository;

import edu.nu.forensic.db.entity.UnitDependency;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface UnitDependencyRepository extends CrudRepository<UnitDependency, UUID> {

}
package edu.nu.forensic.db.repository;

import edu.nu.forensic.db.entity.RegistryKeyObject;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface RegistryKeyObjectRepository extends CrudRepository<RegistryKeyObject, UUID> {

}
package edu.nu.forensic.db.repository;

import edu.nu.forensic.db.entity.Principal;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface PrincipalRepository extends CrudRepository<Principal, UUID> {

}
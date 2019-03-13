package edu.nu.forensic.db.repository;

import edu.nu.forensic.db.entity.Object;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface ObjectRepository extends CrudRepository<Object, UUID> {

}
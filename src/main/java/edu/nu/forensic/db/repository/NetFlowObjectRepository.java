package edu.nu.forensic.db.repository;

import edu.nu.forensic.db.entity.NetFlowObject;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface NetFlowObjectRepository extends CrudRepository<NetFlowObject, UUID> {

}
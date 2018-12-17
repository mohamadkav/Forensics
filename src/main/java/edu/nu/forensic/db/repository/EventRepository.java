package edu.nu.forensic.db.repository;

import edu.nu.forensic.db.entity.Event;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface EventRepository extends CrudRepository<Event, UUID> {

}
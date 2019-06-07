package edu.nu.forensic.db.repository;

import edu.nu.forensic.db.entity.Event;
import org.springframework.data.repository.CrudRepository;

import java.util.Set;
import java.util.UUID;

public interface EventRepository extends CrudRepository<Event, UUID> {
    Set<Event> OrderByTimestampNanosAsc();
    Set<Event> findByNamesEqualsOrNamesEqualsOrderByTimestampNanosAsc(String name1, String name2);
    Set<Event> findByNamesEquals(String name);
}
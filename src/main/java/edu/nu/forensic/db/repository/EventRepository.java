package edu.nu.forensic.db.repository;

import edu.nu.forensic.db.entity.Event;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.Set;
import java.util.UUID;

public interface EventRepository extends CrudRepository<Event, UUID> {
    //Set<Event> OrderByTimestampNanosAsc();
    //Set<Event> findByNamesEqualsOrNamesEqualsOrderByTimestampNanosAsc(String name1, String name2);
    //Set<Event> findByNamesEquals(String name);
    @Query(
            value = "select * from event where timestamp_nanos >= ?1 and timestamp_nanos < ?2", nativeQuery = true
    )
    Set<Event> findMyEvents(Long from, Long to);
    Event findTop1ByOrderByTimestampNanosAsc();
    Event findTop1ByOrderByTimestampNanosDesc();
}
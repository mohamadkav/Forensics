package edu.nu.forensic.db.repository;

import edu.nu.forensic.db.entity.Subject;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface SubjectRepository extends CrudRepository<Subject, UUID> {

}
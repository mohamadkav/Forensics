package edu.nu.forensic.db.repository;

import edu.nu.forensic.db.entity.FileObject;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface FileObjectRepository extends CrudRepository<FileObject, UUID> {

}
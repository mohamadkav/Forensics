package edu.nu.forensic.db.entity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import java.util.UUID;

@Entity
@Inheritance
public abstract class Object {
    @Id
    private UUID id;
}

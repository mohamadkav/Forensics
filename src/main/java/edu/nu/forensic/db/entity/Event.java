/*
package edu.nu.forensic.db.entity;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import java.util.UUID;

@Entity
public class Event {

    private UUID id;

    public Long sequence;

    public String type;

    public Integer threadId;

    @ManyToOne
    public UUID subject;

    public UUID predicateObject;

    public CharSequence predicateObjectPath;

    public UUID predicateObject2;

    public CharSequence predicateObject2Path;

    public long timestampNanos;

    public List<CharSequence> names;

    public List<Value> parameters;

    public Long location;

    public Long size;

    public CharSequence programPoint;

    protected Event() {}

    public Event(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }

    @Override
    public String toString() {
        return String.format(
                "Customer[firstName='%s', lastName='%s']",
                 firstName, lastName);
    }

}*/

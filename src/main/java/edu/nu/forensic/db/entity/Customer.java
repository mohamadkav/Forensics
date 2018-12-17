package edu.nu.forensic.db.entity;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.UUID;

@Entity
public class Customer {
    @Id
    @GenericGenerator(name="system-uuid", strategy = "uuid2")
    @GeneratedValue(generator = "system-uuid")
    @Type(type = "uuid-char")
    private UUID id;

    private String firstName;
    private String lastName;

    protected Customer() {}

    public Customer(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }

    @Override
    public String toString() {
        return String.format(
                "Customer[firstName='%s', lastName='%s']",
                 firstName, lastName);
    }

}
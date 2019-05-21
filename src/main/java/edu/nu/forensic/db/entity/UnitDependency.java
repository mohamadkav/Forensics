package edu.nu.forensic.db.entity;

import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import java.io.Serializable;
import java.util.UUID;

@Entity
public class UnitDependency {
    @EmbeddedId
    private UnitDepId id;

    @ManyToOne
    private Subject oldSubject;

    @ManyToOne
    private Subject newSubject;

    public UnitDependency(Subject oldSubject, Subject newSubject) {
        this.oldSubject = oldSubject;
        this.newSubject = newSubject;
        id=new UnitDepId(oldSubject.getUuid(),newSubject.getUuid());
    }

    public UnitDependency() {
    }
}

@Embeddable
class UnitDepId implements Serializable {
    private String oldSubject;
    private String newSubject;

    public UnitDepId() {
    }

    public UnitDepId(String oldSubject, String newSubject) {
        this.oldSubject = oldSubject;
        this.newSubject = newSubject;
    }
}
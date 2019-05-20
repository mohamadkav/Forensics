package edu.nu.forensic.db.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import java.util.UUID;

@Entity
public class IoEvent {
    @Id
    private UUID id;

    private Long sequence;

    private String type;

    private Integer threadId;

    @ManyToOne
    private Subject subject;

    @ManyToOne
    private Object predicateObject;

    @Column(length = 2048)
    private String predicateObjectPath;

    @ManyToOne
    private Object predicateObject2;

    @Column(length = 2048)
    private String predicateObject2Path;

    private long startTimestampNanos;

    private long endTimestampNanos;

    private String names;

    private Long location;

    private Long size;

    private String programPoint;

    public IoEvent() {
    }

    public IoEvent(UUID id, Long sequence, String type, Integer threadId, Subject subject, Object predicateObject, String predicateObjectPath, Object predicateObject2, String predicateObject2Path, long startTimestampNanos, long endTimestampNanos, String names, Long location, Long size, String programPoint) {
        this.id = id;
        this.sequence = sequence;
        this.type = type;
        this.threadId = threadId;
        this.subject = subject;
        this.predicateObject = predicateObject;
        this.predicateObjectPath = predicateObjectPath;
        this.predicateObject2 = predicateObject2;
        this.predicateObject2Path = predicateObject2Path;
        this.startTimestampNanos = startTimestampNanos;
        this.endTimestampNanos = endTimestampNanos;
        this.names = names;
        this.location = location;
        this.size = size;
        this.programPoint = programPoint;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Long getSequence() {
        return sequence;
    }

    public void setSequence(Long sequence) {
        this.sequence = sequence;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getThreadId() {
        return threadId;
    }

    public void setThreadId(Integer threadId) {
        this.threadId = threadId;
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public Object getPredicateObject() {
        return predicateObject;
    }

    public void setPredicateObject(Object predicateObject) {
        this.predicateObject = predicateObject;
    }

    public String getPredicateObjectPath() {
        return predicateObjectPath;
    }

    public void setPredicateObjectPath(String predicateObjectPath) {
        this.predicateObjectPath = predicateObjectPath;
    }

    public Object getPredicateObject2() {
        return predicateObject2;
    }

    public void setPredicateObject2(Object predicateObject2) {
        this.predicateObject2 = predicateObject2;
    }

    public String getPredicateObject2Path() {
        return predicateObject2Path;
    }

    public void setPredicateObject2Path(String predicateObject2Path) {
        this.predicateObject2Path = predicateObject2Path;
    }

    public long getStartTimestampNanos() {
        return startTimestampNanos;
    }

    public void setStartTimestampNanos(long startTimestampNanos) {
        this.startTimestampNanos = startTimestampNanos;
    }

    public long getEndTimestampNanos() {
        return endTimestampNanos;
    }

    public void setEndTimestampNanos(long endTimestampNanos) {
        this.endTimestampNanos = endTimestampNanos;
    }

    public String getNames() {
        return names;
    }

    public void setNames(String names) {
        this.names = names;
    }

    public Long getLocation() {
        return location;
    }

    public void setLocation(Long location) {
        this.location = location;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public String getProgramPoint() {
        return programPoint;
    }

    public void setProgramPoint(String programPoint) {
        this.programPoint = programPoint;
    }
}
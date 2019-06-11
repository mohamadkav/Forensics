package edu.nu.forensic.db.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.UUID;

@Entity
public class IoEventAfterCPR {
    @Id
    private UUID id;

 //   private Long sequence;

    private String type;

    private Integer threadId;

  //  @ManyToOne
    private UUID subjectUUID;

//    @ManyToOne
//    private Object predicateObject;

    @Column(length = 2048)
    private String predicateObjectPath;

//    @ManyToOne
//    private Object predicateObject2;
//
//    @Column(length = 2048)
//    private String predicateObject2Path;

    private long startTimestampNanos;

    private long endTimestampNanos;

    private String names;

//    private Long location;
//
//    private Long size;
//
//    private String programPoint;

    public IoEventAfterCPR() {
    }

    public IoEventAfterCPR(UUID id, String type, Integer threadId, UUID subjectUUID, String predicateObjectPath, long startTimestampNanos, long endTimestampNanos, String names) {
        this.id = id;
        this.type = type;
        this.threadId = threadId;
        this.subjectUUID = subjectUUID;
        this.predicateObjectPath = predicateObjectPath;
        this.startTimestampNanos = startTimestampNanos;
        this.endTimestampNanos = endTimestampNanos;
        this.names = names;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public UUID getSubjectUUID() {
        return subjectUUID;
    }

    public void setSubjectUUID(UUID subjectUUID) {
        this.subjectUUID = subjectUUID;
    }

    public String getPredicateObjectPath() {
        return predicateObjectPath;
    }

    public void setPredicateObjectPath(String predicateObjectPath) {
        this.predicateObjectPath = predicateObjectPath;
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
}
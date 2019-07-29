package edu.nu.forensic.db.entity;

import java.util.UUID;

public class Event {
    private UUID id;

  //  private Long sequence;

//    private String type;

    private Integer typeNum;

    private Integer threadId;

//    @ManyToOne
    private UUID subjectUUID;

//    @ManyToOne
//    private Object predicateObject;

//    @Column(length = 2048)
    private String predicateObjectPath;

//    @ManyToOne
//    private Object predicateObject2;
//
//    @Column(length = 2048)
//    private String predicateObject2Path;

    private long timestampNanos;

    private String names;

    private boolean needWritingToObjectTable;

//    private Long location;
//
//    private Long size;
//
//    private String programPoint;

    public Event() {
    }

//    public Event(UUID id, String type, Integer threadId, UUID subject, String predicateObjectPath, long timestampNanos, String names, boolean needWritingToObjectTable) {
//        this.id = id;
//        this.type = type;
//        this.threadId = threadId;
//        this.subjectUUID = subject;
//        this.predicateObjectPath = predicateObjectPath;
//        this.timestampNanos = timestampNanos;
//        this.names = names;
//        this.needWritingToObjectTable=needWritingToObjectTable;
//    }

    public Event(UUID id, Integer typeNum, Integer threadId, UUID subject, String predicateObjectPath, long timestampNanos, String names, boolean needWritingToObjectTable) {
        this.id = id;
        this.typeNum = typeNum;
        this.threadId = threadId;
        this.subjectUUID = subject;
        this.predicateObjectPath = predicateObjectPath;
        this.timestampNanos = timestampNanos;
        this.names = names;
        this.needWritingToObjectTable=needWritingToObjectTable;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public int getTypeNum(){return typeNum;}

    public void setTypeNum(Integer typeNum){this.typeNum = typeNum;}

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

    public long getTimestampNanos() {
        return timestampNanos;
    }

    public void setTimestampNanos(long timestampNanos) {
        this.timestampNanos = timestampNanos;
    }

    public String getNames() {
        return names;
    }

    public void setNames(String names) {
        this.names = names;
    }

    public boolean getNeedWritingToObjectTable() {
        return needWritingToObjectTable;
    }

    public void setNeedWritingToObjectTable(boolean needWritingToObjectTable) {
        this.needWritingToObjectTable = needWritingToObjectTable;
    }
}
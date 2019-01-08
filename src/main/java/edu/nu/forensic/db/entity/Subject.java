package edu.nu.forensic.db.entity;

import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import java.util.UUID;

@Entity
public class Subject {
    @Id
    @Type(type = "uuid-char")
    private UUID uuid;

    private String type;

    private int cid;

    @ManyToOne
    private Subject parentSubject;

    @ManyToOne
    private Principal localPrincipal;

    private Long startTimestampNanos;

    @Column(length = 2048)
    private String cmdLine;

    private String privilegeLevel;

    public Subject() {
    }

    public Subject(UUID uuid, String type, int cid, Subject parentSubject, Principal localPrincipal, Long startTimestampNanos, String cmdLine, String privilegeLevel) {
        this.uuid = uuid;
        this.type = type;
        this.cid = cid;
        this.parentSubject = parentSubject;
        this.localPrincipal = localPrincipal;
        this.startTimestampNanos = startTimestampNanos;
        this.cmdLine = cmdLine;
        this.privilegeLevel = privilegeLevel;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getCid() {
        return cid;
    }

    public void setCid(int cid) {
        this.cid = cid;
    }

    public Subject getParentSubject() {
        return parentSubject;
    }

    public void setParentSubject(Subject parentSubject) {
        this.parentSubject = parentSubject;
    }

    public Principal getLocalPrincipal() {
        return localPrincipal;
    }

    public void setLocalPrincipal(Principal localPrincipal) {
        this.localPrincipal = localPrincipal;
    }

    public Long getStartTimestampNanos() {
        return startTimestampNanos;
    }

    public void setStartTimestampNanos(Long startTimestampNanos) {
        this.startTimestampNanos = startTimestampNanos;
    }

    public String getCmdLine() {
        return cmdLine;
    }

    public void setCmdLine(String cmdLine) {
        this.cmdLine = cmdLine;
    }

    public String getPrivilegeLevel() {
        return privilegeLevel;
    }

    public void setPrivilegeLevel(String privilegeLevel) {
        this.privilegeLevel = privilegeLevel;
    }
}
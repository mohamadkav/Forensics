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

    private Integer typeNum;

    private int cid;

 //   @ManyToOne
    private UUID parentSubjectUUID;

//    @ManyToOne
//    private Principal localPrincipal;

    private Long startTimestampNanos;

    @Column(length = 2048)
    private String cmdLine;

    private String Usersid;

    private boolean visibleWindowInfo;

 //   private String privilegeLevel;

    public Subject() {
    }

    public Subject(UUID uuid, String type, int cid, UUID parentSubject, Principal localPrincipal, Long startTimestampNanos, String cmdLine, String privilegeLevel) {
        this.uuid = uuid;
        this.type = type;
        this.cid = cid;
        this.parentSubjectUUID = parentSubject;
       // this.localPrincipal = localPrincipal;
        this.startTimestampNanos = startTimestampNanos;
        this.cmdLine = cmdLine;
     //   this.privilegeLevel = privilegeLevel;
    }

    public Subject(UUID uuid, String type, int cid, UUID parentSubject, Principal localPrincipal, Long startTimestampNanos, String cmdLine,
                   String privilegeLevel, String Usersid, boolean visibleWindowInfo) {
        this.uuid = uuid;
        this.type = type;
        this.cid = cid;
        this.parentSubjectUUID = parentSubject;
        this.startTimestampNanos = startTimestampNanos;
        this.cmdLine = cmdLine;
        this.Usersid = Usersid;
        this.visibleWindowInfo = visibleWindowInfo;
    }

    public Subject(UUID uuid, Integer typeNum, int cid, UUID parentSubject,  Long startTimestampNanos, String cmdLine,
                   String Usersid, boolean visibleWindowInfo) {
        this.uuid = uuid;
        this.typeNum = typeNum;
        this.cid = cid;
        this.parentSubjectUUID = parentSubject;
        this.startTimestampNanos = startTimestampNanos;
        this.cmdLine = cmdLine;
        this.Usersid = Usersid;
        this.visibleWindowInfo = visibleWindowInfo;
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

    public Integer getTypeNum(){return typeNum;}

    public void setTypeNum(Integer typeNum){this.typeNum = typeNum;}

    public int getCid() {
        return cid;
    }

    public void setCid(int cid) {
        this.cid = cid;
    }

    public UUID getParentSubject() {
        return parentSubjectUUID;
    }

    public void setParentSubject(UUID parentSubject) {
        this.parentSubjectUUID = parentSubject;
    }

    public void setUsersid(String sid){this.Usersid = sid;}

    public String getUsersid(){return this.Usersid;}

//
//    public Principal getLocalPrincipal() {
//        return localPrincipal;
//    }
//
//    public void setLocalPrincipal(Principal localPrincipal) {
//        this.localPrincipal = localPrincipal;
//    }

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

    public void setVisibleWindowInfo(boolean visibleWindowInfo){this.visibleWindowInfo = visibleWindowInfo;}

    public boolean getVisibleWindowInfo(){return this.visibleWindowInfo;}

//    public String getPrivilegeLevel() {
//        return privilegeLevel;
//    }
//
//    public void setPrivilegeLevel(String privilegeLevel) {
//        this.privilegeLevel = privilegeLevel;
//    }

}
package edu.nu.forensic.db.entity;


import javax.persistence.Column;
import javax.persistence.Entity;
import java.util.UUID;

@Entity
public class NetFlowObject extends Object{
    private UUID id;

    private String localAddress;

    private Integer localPort;

    private String remoteAddress;

    private Integer remotePort;

    private Integer ipProtocol;

    private String type;

    private Integer threadId;

    //  @ManyToOne
    private UUID subjectUUID;

    private long startTimestampNanos;

    public NetFlowObject(UUID id,String localAddress, Integer localPort, String remoteAddress, Integer remotePort, Integer ipProtocol) {
        this.setId(id);
        this.localAddress = localAddress;
        this.localPort = localPort;
        this.remoteAddress = remoteAddress;
        this.remotePort = remotePort;
        this.ipProtocol = ipProtocol;
    }

    public NetFlowObject(UUID id, String localAddress, Integer localPort, String remoteAddress, Integer remotePort,
                         UUID subjectUUID, long startTimestampNanos, String type, Integer threadId) {
        this.setId(id);
        this.localAddress = localAddress;
        this.localPort = localPort;
        this.remoteAddress = remoteAddress;
        this.remotePort = remotePort;
        this.subjectUUID = subjectUUID;
        this.startTimestampNanos = startTimestampNanos;
        this.type = type;
        this.threadId = threadId;
    }

    public NetFlowObject() {
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

    public long getStartTimestampNanos() { return startTimestampNanos; }

    public void setStartTimestampNanos(long startTimestampNanos) {
        this.startTimestampNanos = startTimestampNanos;
    }

    public UUID getSubjectUUID() {
        return subjectUUID;
    }

    public void setSubjectUUID(UUID subjectUUID) {
        this.subjectUUID = subjectUUID;
    }

    public String getLocalAddress() {
        return localAddress;
    }

    public void setLocalAddress(String localAddress) {
        this.localAddress = localAddress;
    }

    public Integer getLocalPort() {
        return localPort;
    }

    public void setLocalPort(Integer localPort) {
        this.localPort = localPort;
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

    public void setRemoteAddress(String remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    public Integer getRemotePort() {
        return remotePort;
    }

    public void setRemotePort(Integer remotePort) {
        this.remotePort = remotePort;
    }

    public Integer getIpProtocol() {
        return ipProtocol;
    }

    public void setIpProtocol(Integer ipProtocol) {
        this.ipProtocol = ipProtocol;
    }
}

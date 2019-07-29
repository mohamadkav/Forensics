package edu.nu.forensic.db.entity;


import java.util.UUID;

public class NetFlowObject extends Object{
    private UUID id;

    private Integer localAddress;

    private Integer localPort;

    private Integer remoteAddress;

    private Integer remotePort;

    private Integer ipProtocol;

    private String type;

    private Integer typeNum;

    private Integer threadId;

    //  @ManyToOne
    private UUID subjectUUID;

    private long startTimestampNanos;

    public NetFlowObject(UUID id, Integer localAddress, Integer localPort, Integer remoteAddress, Integer remotePort, Integer ipProtocol) {
        this.setId(id);
        this.localAddress = localAddress;
        this.localPort = localPort;
        this.remoteAddress = remoteAddress;
        this.remotePort = remotePort;
        this.ipProtocol = ipProtocol;
    }

    public NetFlowObject(Integer localAddress, Integer localPort, Integer remoteAddress, Integer remotePort,
                         UUID subjectUUID, long startTimestampNanos, String type, Integer threadId) {
        this.localAddress = localAddress;
        this.localPort = localPort;
        this.remoteAddress = remoteAddress;
        this.remotePort = remotePort;
        this.subjectUUID = subjectUUID;
        this.startTimestampNanos = startTimestampNanos;
        this.type = type;
        this.threadId = threadId;
    }

    public NetFlowObject(Integer localAddress, Integer localPort, Integer remoteAddress, Integer remotePort,
                         UUID subjectUUID, long startTimestampNanos, Integer typeNum, Integer threadId) {
        this.localAddress = localAddress;
        this.localPort = localPort;
        this.remoteAddress = remoteAddress;
        this.remotePort = remotePort;
        this.subjectUUID = subjectUUID;
        this.startTimestampNanos = startTimestampNanos;
        this.typeNum = typeNum;
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

    public Integer getTypeNum(){return typeNum;}

    public void setTypeNum(Integer typeNum){this.typeNum = typeNum;}

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

    public Integer getLocalAddress() {
        return localAddress;
    }

    public void setLocalAddress(int localAddress) {
        this.localAddress = localAddress;
    }

    public Integer getLocalPort() {
        return localPort;
    }

    public void setLocalPort(Integer localPort) {
        this.localPort = localPort;
    }

    public int getRemoteAddress() {
        return remoteAddress;
    }

    public void setRemoteAddress(int remoteAddress) {
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

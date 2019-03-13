package edu.nu.forensic.db.entity;


import javax.persistence.Entity;
import java.util.UUID;

@Entity
public class NetFlowObject extends Object{
    private String localAddress;

    private Integer localPort;

    private String remoteAddress;

    private Integer remotePort;

    private Integer ipProtocol;

    public NetFlowObject(UUID id,String localAddress, Integer localPort, String remoteAddress, Integer remotePort, Integer ipProtocol) {
        this.setId(id);
        this.localAddress = localAddress;
        this.localPort = localPort;
        this.remoteAddress = remoteAddress;
        this.remotePort = remotePort;
        this.ipProtocol = ipProtocol;
    }

    public NetFlowObject() {
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

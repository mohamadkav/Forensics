package edu.nu.forensic.db.entity;

import java.util.HashMap;
import java.util.Map;

public enum EventNameToNum {
    FileIoRead(1), FileIoWrite(2),Keylogger(3), ScreenGrab(4),InitSignal(5),VisibleWindowInfo(6), ProcessDCStart(7),ImageDCStart(8),TcpIpSendIPV4(9),
    ThreadStart(10),TcpIpRecvIPV4(11),ThreadEnd(12),UdpIpRecvIPV4(13),TcpIpDisconnectIPV4(14),UdpIpSendIPV4(15),FileIoRename(16),FileIoDelete(17),
    TcpIpConnectIPV4(18),ProcessEnd(19),ProcessStart(20),ImageLoad(21),HealthCheck(22),ProcessInjection(23),ThreadDCStart(24);

    private int number;
    EventNameToNum(int num){this.number = num;}
    int getNumber() {return this.number;}

    public Map<String, Integer> getEventNameToNum(){
        Map<String, Integer> result = new HashMap<>();
        for(EventNameToNum eventNameToNum: EventNameToNum.values()){
            result.put(eventNameToNum.toString(), eventNameToNum.getNumber());
        }
        return result;
    }
}

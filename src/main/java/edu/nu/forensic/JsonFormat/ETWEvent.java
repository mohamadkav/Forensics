package edu.nu.forensic.JsonFormat;

import java.util.List;
import java.util.Map;

public class ETWEvent {
    public int processID;
    public int threadID;
    public String EventName;
    public long providerId;
    public int opcode;
    public long TimeStamp;
    public String callstack;
    public arguments arguments;
    public static class arguments{
        public int ParentId;
        public String UserSID;
        public String CommandLine;
        public String ImageFileName;
        public int ProcessId;
        public String FileKey;
        public String FileName;
        public String FileObject;
        public int TTID;
    }

    @Override
    public String toString() {
        return "{\"processId\":" + processID + ",\"threadId\":" + threadID + ",\"eventName\":\"" + EventName + "\",\"timestamp\":" + TimeStamp + ",\"callstack\":\"" + callstack + "}}";
    }
}
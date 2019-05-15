package edu.nu.forensic.JsonFormat;

import java.util.HashMap;
import java.util.Map;

public class ETWJsonFormat {
    public int processId;
    public int threadId;
    public String eventName;
    public long providerId;
    public int opcode;
    public long timestamp;
    public String callstack;
    public Map<String, String> arguments = new HashMap<>();

    @Override
    public String toString() {
        return "{\"processId\":" + processId + ",\"threadId\":" + threadId + ",\"eventName\":\"" + eventName + "\",\"timestamp\":" + timestamp + ",\"callstack\":\"" + callstack + "\",\"arguments\":{" + map2OfflineFormat(arguments) + "}}";
    }

    private static String map2OfflineFormat(Map<String, String> map) {
        if (map.size() == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            sb.append("\"" + entry.getKey() + "\":\"" + entry.getValue() + "\"");
            sb.append(",");
        }
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }
}
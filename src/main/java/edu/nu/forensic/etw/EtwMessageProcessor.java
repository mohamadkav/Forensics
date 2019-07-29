package edu.nu.forensic.etw;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EtwMessageProcessor {
    public static List<EtwEventType> preCheck(Etwdata.EventRecords eventRecords) {
        int eventRecordSize = eventRecords.getProcessIDCount();
        Map<EtwEventType, Integer> eventType2Count = new HashMap<>();
        List<EtwEventType> etwEventTypeList = new ArrayList<>(eventRecordSize);
        int dataIndex = 0;
        for (int i = 0; i < eventRecordSize; i++) {
            long providerId = eventRecords.getProviderID(i);
            int opcode = eventRecords.getOpcode(i);
            long eventKey = providerId + opcode * 31;
            EtwEventType etwEventType = EventFormat.getEventTypeUsingEventKey(eventKey);
            etwEventTypeList.add(etwEventType);

            if (EtwEventType.Unknown.equals(etwEventType)) {
                System.err.println("unknown event, providerId: "+providerId+", opcode: {}"+opcode);
            }

            if (eventType2Count.containsKey(etwEventType)) {
                eventType2Count.put(etwEventType, eventType2Count.get(etwEventType) + 1);
            } else {
                eventType2Count.put(etwEventType, 1);
            }
            Integer lengthOfThis = EventFormat.getArgSizeUsingEventType(etwEventType);
            dataIndex += lengthOfThis;
        }

        if (dataIndex != eventRecords.getDataCount()) {
            try (BufferedWriter bw = new BufferedWriter(new FileWriter("error_report.txt"))) {
                System.err.println("format error");
                bw.write("format: " + dataIndex + " real: " + eventRecords.getDataCount() + "\n");
                for (Map.Entry<EtwEventType, Integer> entry : eventType2Count.entrySet()) {
                    bw.write(entry.toString());
                    bw.newLine();
                }
                bw.close();
                return null;
            } catch (Exception e) {
                System.err.println("buffered writer error"+ e);
            }
        }
        return etwEventTypeList;
    }
    public static List<EventRecord> constructEventRecord(Etwdata.EventRecords eventRecords, List<EtwEventType> etwEventTypeList) {
        int eventRecordsSize = etwEventTypeList.size();
        int dataIndex = 0;
        int callstackIndex = 0;
        EventRecord currentPreviousRecord = null;
        List<EventRecord> records=new ArrayList<>();
        for (int i = 0; i < eventRecordsSize; i++) {
            EtwEventType etwEventType = etwEventTypeList.get(i);
            int processId = eventRecords.getProcessID(i);
            int threadId = eventRecords.getThreadID(i);
            long timestamp = eventRecords.getTimestamp(i);
            EventRecord record = new EventRecord(etwEventType, processId, threadId, timestamp);
            List<String> argNames = EventFormat.getArgNamesUsingEventType(etwEventType);
            int argSize = EventFormat.getArgSizeUsingEventType(etwEventType);
            for (int j = 0; j < argSize; j++) {
                Etwdata.datad data = eventRecords.getData(dataIndex + j);
                if (data.getSCount() != 0) {
                    record.getArgument().put(argNames.get(j), data.getS(0).toStringUtf8());
                } else {
                    record.getArgument().put(argNames.get(j), data.getD(0));
                }
            }
            dataIndex += argSize;
            if (etwEventType == EtwEventType.Callstack) {
                record.setCallstack(eventRecords.getCallstack(callstackIndex++).toStringUtf8());
            }
            if (!record.duplicatesWith(currentPreviousRecord)) {
                records.add(record);
                currentPreviousRecord = record;
            }
        }
        return records;
    }

}

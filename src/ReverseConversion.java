
import com.bbn.tc.schema.avro.cdm20.*;

import java.nio.ByteBuffer;
import java.util.HashMap;

public class ReverseConversion {

    private static HashMap<UUID, Integer> subject2ThreadId; // thread;
    private static HashMap<UUID, Integer> subject2process;
    private static HashMap<UUID, String> process2CmdLine;
    private static HashMap<UUID, UUID> subject2Parent;

    private static HashMap<UUID, UUID> threadUUIDtoProcessUUID;

    private static HashMap<Integer, Integer> threadId2ProcessId;
    // objects
    private static HashMap<UUID, String> fileObject2FilePath;
    private static HashMap<UUID, String> registryObject2Path;
    private static HashMap<UUID, String> principal2Sid;
    private static HashMap<UUID, String> subjectToPrincipal;

    private static HashMap<UUID, HashMap<String,String> > networkObject2Parameters;
    public static void init(){
        System.out.println("Init");

        subject2ThreadId = new HashMap<>();
        subject2process = new HashMap<>();
        process2CmdLine = new HashMap<>();
        threadId2ProcessId = new HashMap<>();

        fileObject2FilePath = new HashMap<>();
        registryObject2Path = new HashMap<>();
        networkObject2Parameters = new HashMap<>();
        principal2Sid=new HashMap<>();
        subjectToPrincipal=new HashMap<>();
        subject2Parent=new HashMap<>();
        threadUUIDtoProcessUUID=new HashMap<>();
    }


    public static EventRecord parse(TCCDMDatum CDMdatum){
        Object datum = CDMdatum.getDatum();
        EventRecord tempRecord = new EventRecord();
//        System.out.println(CDMdatum.getDatum().getClass().getName());
        // reserve conversion here
        if (datum instanceof  Event){
            parseEvent(CDMdatum,tempRecord);
        }
        else if(datum instanceof Subject){
            parseSubject(CDMdatum,tempRecord);
        }
        else if(datum instanceof FileObject || datum instanceof RegistryKeyObject || datum instanceof NetFlowObject){
            parseObject(CDMdatum,tempRecord);
        }
        else if(datum instanceof Principal){
            parsePrincipal(CDMdatum,tempRecord);
        }
        else{
            System.out.println(CDMdatum.getDatum().toString());
        }
        // -----------------------

        return tempRecord;
    }

    private static void parsePrincipal(TCCDMDatum datum, EventRecord tempRecord){
        Principal record= (Principal)datum.getDatum();
        principal2Sid.put(record.getUuid(), record.getUserId().toString());
        tempRecord.eventName="";
    }
    private static void parseEvent(TCCDMDatum datum, EventRecord tempRecord){
        Event record = (com.bbn.tc.schema.avro.cdm20.Event) datum.getDatum();

        // unified operation
        //tempRecord.pcId = 0;
        tempRecord.timeStamp = record.getTimestampNanos();
//        tempRecord.eventName = record.getName().toString();

        //THE TID OF THE PROCESS WHICH PERFORMS THE ACTION
        tempRecord.threadId = ((Event) datum.getDatum()).getThreadId();//subject2ThreadId.get(record.getSubject());

        //THE PID OF OWNER
        if(subject2ThreadId.containsKey(((Event) datum.getDatum()).getSubject())&&
                threadId2ProcessId.containsKey(subject2ThreadId.get(((Event) datum.getDatum()).getSubject())))
            tempRecord.processId=threadId2ProcessId.get(subject2ThreadId.get(((Event) datum.getDatum()).getSubject()));
        else if(subject2process.containsKey(((Event) datum.getDatum()).getSubject()))
            tempRecord.processId=subject2process.get(((Event) datum.getDatum()).getSubject());
        else
            throw new RuntimeException("PID not found");
   //     tempRecord.processId = threadId2ProcessId.get(tempRecord.threadId);

        if(record.getProperties()!=null) {
            for (CharSequence cs : record.getProperties().keySet()) {
                if (cs.toString().equals("Callstack"))
                    tempRecord.callstack.add(record.getProperties().get(cs).toString());
                else
                    tempRecord.arguments.put(cs.toString(), record.getProperties().get(cs).toString());
            }
        }

        switch (record.getType()){
            case EVENT_EXECUTE: parseEventExecute(record,tempRecord); break; // ProcessStart
            case EVENT_EXIT: parseEventExit(record,tempRecord); break;  // ProcessEnd

            case EVENT_LOADLIBRARY: parseEventLoadLibrary(record,tempRecord); break; // ImageLoad
            case EVENT_CLOSE: parseEventClose(record,tempRecord); break; // ImageUnLoad

            case EVENT_SENDMSG: parseEventSendMsg(record,tempRecord); break; // ALPCALPC-Send-Message
            case EVENT_RECVMSG: parseEventRecvMsg(record,tempRecord); break; // ALPCALPC-Receive-Message
            case EVENT_OTHER: parseEventOther(record,tempRecord); break; // ALPCALPC-Unwait, ALPCALPC-Wait-For-Reply, RegistryEnumerateKey, System call
            case EVENT_CONNECT: parseEventConnect(record,tempRecord); break; // ALPCALPC-Unwait, ALPCALPC-Wait-For-Reply, RegistryEnumerateKey, System call

            case EVENT_UNLINK: parseEventUnlink(record,tempRecord); break;

            case EVENT_CREATE_OBJECT: parseEventCreateObject(record,tempRecord); break; // FileIoCreate

            case EVENT_RENAME: parseEventRename(record,tempRecord); break; // FileIoCreate

            case EVENT_WRITE: parseEventWrite(record,tempRecord); break; // DiskIoWrite

            case EVENT_READ: parseEventRead(record,tempRecord); break; // DiskIoWrite

            case EVENT_CREATE_THREAD: parseEventCreateThread(record,tempRecord); break; // DiskIoWrite


            default: tempRecord.eventName = "";
        }
    }

    private static void parseEventExecute(Event record,EventRecord tempRecord) {
        tempRecord.eventName = "ProcessStart";
        tempRecord.arguments.put("FileName",record.getPredicateObjectPath().toString());
        //tempRecord.arguments.put("CommandLine", process2CmdLine.get(threadUUIDtoProcessUUID.get(record.getSubject())));
        if(record.getPredicateObjectPath()!=null)
            tempRecord.arguments.put("CommandLine", process2CmdLine.get(record.getPredicateObject()));
        if(subject2Parent.containsKey(record.getSubject()))
            tempRecord.arguments.put("ParentId",subject2process.get(subject2Parent.get(record.getSubject())).toString());
        if(subjectToPrincipal.containsKey(record.getPredicateObject()))
            tempRecord.arguments.put("userId",subjectToPrincipal.get(record.getPredicateObject()));
    }

    private static void parseEventExit(Event record,EventRecord tempRecord) {
        if(record.getPredicateObjectPath()!=null) {
            tempRecord.eventName = "ProcessEnd";
            tempRecord.arguments.put("FileName", record.getPredicateObjectPath().toString());
            tempRecord.arguments.put("CommandLine", process2CmdLine.get(record.getPredicateObject()));
        }
        else {
            tempRecord.eventName = "ThreadEnd";
            tempRecord.arguments.put("CommandLine", process2CmdLine.get(threadUUIDtoProcessUUID.get(record.getSubject())));
        }
    }

    private static void parseEventLoadLibrary(Event record,EventRecord tempRecord){
        tempRecord.eventName = "ImageLoad";
        tempRecord.arguments.put("ImageFileName", record.getPredicateObjectPath().toString());
    }

    private static void parseEventClose(Event record,EventRecord tempRecord){
        if(networkObject2Parameters.containsKey(record.getPredicateObject())) {
            tempRecord.eventName="NetworkClose";
            for(String param:networkObject2Parameters.get(record.getPredicateObject()).keySet())
                tempRecord.arguments.put(param,networkObject2Parameters.get(record.getPredicateObject()).get(param));
        }
        else {
            tempRecord.eventName = "ImageUnLoad";
            tempRecord.arguments.put("ImageFileName", record.getPredicateObjectPath().toString());
        }
    }

    private static void parseEventSendMsg(Event record,EventRecord tempRecord){
        tempRecord.eventName="NetworkSendMsg";
        if(networkObject2Parameters.containsKey(record.getPredicateObject())) {
            for(String param:networkObject2Parameters.get(record.getPredicateObject()).keySet())
                tempRecord.arguments.put(param,networkObject2Parameters.get(record.getPredicateObject()).get(param));
        }
        tempRecord.arguments.put("size",record.getSize()+"");
    }

    private static void parseEventRecvMsg(Event record,EventRecord tempRecord){
        tempRecord.eventName="NetworkRecvMsg";
        if(networkObject2Parameters.containsKey(record.getPredicateObject())) {
            for(String param:networkObject2Parameters.get(record.getPredicateObject()).keySet())
                tempRecord.arguments.put(param,networkObject2Parameters.get(record.getPredicateObject()).get(param));
        }
        tempRecord.arguments.put("size",record.getSize()+"");
    }

    private static void parseEventConnect(Event record,EventRecord tempRecord){
        tempRecord.eventName="NetworkConnect";
        if(networkObject2Parameters.containsKey(record.getPredicateObject())) {
            for(String param:networkObject2Parameters.get(record.getPredicateObject()).keySet())
                tempRecord.arguments.put(param,networkObject2Parameters.get(record.getPredicateObject()).get(param));
        }
        tempRecord.arguments.put("size",record.getSize()+"");
    }

    private static void parseEventUnlink(Event record,EventRecord tempRecord){
        if(registryObject2Path.containsKey(record.getPredicateObject())){
            tempRecord.eventName = "RegistryDelete";
            tempRecord.arguments.put("KeyName", record.getPredicateObjectPath().toString());
        }
        else{
            tempRecord.eventName = "FileIoDelete";
            tempRecord.arguments.put("FileName", record.getPredicateObjectPath().toString());
        }
    }


    private static void parseEventOther(Event record,EventRecord tempRecord){
        switch (record.getNames().get(0).toString()){
            case "ALPC Wait For Reply": tempRecord.eventName = "ALPCALPC-Wait-For-Reply"; break;
            case "ALPC Unwait": tempRecord.eventName = "ALPCALPC-Unwait"; break;
            case "ALPC Wait For New Message": tempRecord.eventName = "ALPCALPC-Wait-For-New-Message"; break;
            case "Delete File": tempRecord.eventName = "FileIoDelete"; tempRecord.arguments.put("FileName", record.getPredicateObjectPath().toString()); break;
            case "Enumerate value key event": tempRecord.eventName = "RegistryEnumerateValueKey"; tempRecord.arguments.put("KeyName", record.getPredicateObjectPath().toString()); break;
            case "Delete Registry": tempRecord.eventName = "RegistryDelete"; tempRecord.arguments.put("KeyName", record.getPredicateObjectPath().toString()); break;
            case "RegistryEnumerateKey": tempRecord.eventName = record.getNames().get(0).toString(); tempRecord.arguments.put("KeyName", record.getPredicateObjectPath().toString()); break;
            case "RegistryDeleteValue": tempRecord.eventName = record.getNames().get(0).toString(); tempRecord.arguments.put("KeyName", record.getPredicateObjectPath().toString()); break;
            case "RegistrySetValue": tempRecord.eventName = record.getNames().get(0).toString(); tempRecord.arguments.put("KeyName", record.getPredicateObjectPath().toString()); break;
            case "RegistrySetInformation": tempRecord.eventName = record.getNames().get(0).toString(); tempRecord.arguments.put("KeyName", record.getPredicateObjectPath().toString()); break;
            case "RegistryQueryValue": tempRecord.eventName = record.getNames().get(0).toString(); tempRecord.arguments.put("KeyName", record.getPredicateObjectPath().toString()); break;
            case "RegistryQuery": tempRecord.eventName = record.getNames().get(0).toString(); tempRecord.arguments.put("KeyName", record.getPredicateObjectPath().toString()); break;
            case "RegistryOpen": tempRecord.eventName = record.getNames().get(0).toString(); tempRecord.arguments.put("KeyName", record.getPredicateObjectPath().toString()); break;
            case "RegistryKCBDelete": tempRecord.eventName = record.getNames().get(0).toString(); tempRecord.arguments.put("KeyName", record.getPredicateObjectPath().toString()); break;
            case "RegistryKCBCreate": tempRecord.eventName = record.getNames().get(0).toString(); tempRecord.arguments.put("KeyName", record.getPredicateObjectPath().toString()); break;
            case "Registry Event": tempRecord.eventName = record.getNames().get(0).toString(); tempRecord.arguments.put("KeyName", record.getPredicateObjectPath().toString()); break;

            case "FileIoCleanup": tempRecord.eventName = record.getNames().get(0).toString(); tempRecord.arguments.put("FileName", record.getPredicateObjectPath().toString()); break;
            case "FileIoClose": tempRecord.eventName = record.getNames().get(0).toString(); tempRecord.arguments.put("FileName", record.getPredicateObjectPath().toString()); break;
            case "FileIoDirEnum": tempRecord.eventName = record.getNames().get(0).toString(); tempRecord.arguments.put("FileName", record.getPredicateObjectPath().toString()); break;
            case "FileIoFileDelete": tempRecord.eventName = record.getNames().get(0).toString(); tempRecord.arguments.put("FileName", record.getPredicateObjectPath().toString()); break;
            case "FileIoQueryInfo": tempRecord.eventName = record.getNames().get(0).toString(); tempRecord.arguments.put("FileName", record.getPredicateObjectPath().toString()); break;
            case "FileIoRead": tempRecord.eventName = record.getNames().get(0).toString(); tempRecord.arguments.put("FileName", record.getPredicateObjectPath().toString()); break;
            case "FileIoSetInfo": tempRecord.eventName = record.getNames().get(0).toString(); tempRecord.arguments.put("FileName", record.getPredicateObjectPath().toString()); break;
            case "FileIoWrite": tempRecord.eventName = record.getNames().get(0).toString(); tempRecord.arguments.put("FileName", record.getPredicateObjectPath().toString()); break;
            case "FileIoFSControl": tempRecord.eventName = record.getNames().get(0).toString(); tempRecord.arguments.put("FileName", record.getPredicateObjectPath().toString()); break;
            case "FileIO event": tempRecord.eventName = record.getNames().get(0).toString(); tempRecord.arguments.put("FileName", record.getPredicateObjectPath().toString()); break;
            case "UdpIp Fail event": tempRecord.eventName = record.getNames().get(0).toString(); break;
            case "VisibleWindowInfo": tempRecord.eventName = record.getNames().get(0).toString(); break;
            case "MouseDownPositionInfo": tempRecord.eventName = record.getNames().get(0).toString(); break;
            case "KeyBoardInfo": tempRecord.eventName = record.getNames().get(0).toString(); break;
            case "PowerShell": tempRecord.eventName = record.getNames().get(0).toString(); break;

            default: {
                if(record.getNames().get(0).toString().contains("@")){
                    tempRecord.eventName="UIInfo";
                    tempRecord.arguments.put("UIInfo",record.getNames().get(0).toString());
                }
                else if(record.getNames().get(0).toString().contains(".")){
                    tempRecord.eventName="IPConfigInfo";
                    tempRecord.arguments.put("IPConfigInfo",record.getNames().get(0).toString());
                }
                else
                    tempRecord.eventName = record.getNames().get(0).toString();
            }
        }
    }

    private static void parseEventCreateObject(Event record,EventRecord tempRecord){
        if(fileObject2FilePath.containsKey(record.getPredicateObject())){
            tempRecord.eventName = "FileIoCreate";
            tempRecord.arguments.put("FileName", record.getPredicateObjectPath().toString());
        }
        else if(registryObject2Path.containsKey(record.getPredicateObject())) {
            tempRecord.eventName = "RegistryCreate";
            tempRecord.arguments.put("KeyName", record.getPredicateObjectPath().toString());
        }
        else
            tempRecord.eventName = "";
    }

    private static void parseEventRename(Event record,EventRecord tempRecord){
        if(fileObject2FilePath.containsKey(record.getPredicateObject())){
            tempRecord.eventName = "FileIoRename";
        }
        else
            tempRecord.eventName = "";
    }

    private static void parseEventWrite(Event record,EventRecord tempRecord){
        tempRecord.eventName = "FileIoWrite";
        tempRecord.arguments.put("FileName", record.getPredicateObjectPath().toString());
    }

    private static void parseEventRead(Event record,EventRecord tempRecord){
        tempRecord.eventName = "FileIoRead";
        tempRecord.arguments.put("FileName", record.getPredicateObjectPath().toString());
    }

    private static void parseEventCreateThread(Event record, EventRecord tempRecord){
        tempRecord.eventName = "ThreadCreate";
        if(subject2ThreadId.containsKey(record.getPredicateObject())){
            tempRecord.arguments.put("threadId",subject2ThreadId.get(record.getPredicateObject()).toString());
        }
        if(threadId2ProcessId.containsKey((record.getThreadId()))){
            tempRecord.arguments.put("processId",threadId2ProcessId.get((record.getThreadId())).toString());
        }
        if(subject2Parent.containsKey(record.getPredicateObject()))
            if(subject2process.containsKey(subject2Parent.get(record.getPredicateObject())))
                tempRecord.arguments.put("ParentId",subject2process.get(subject2Parent.get(record.getPredicateObject())).toString());
    }


    private static void parseSubject(TCCDMDatum datum,EventRecord tempRecord){
        Subject record = (com.bbn.tc.schema.avro.cdm20.Subject) datum.getDatum();

        if(record.getType() == SubjectType.SUBJECT_PROCESS){
            try {
                subject2process.put(record.getUuid(),record.getCid());
                process2CmdLine.put(record.getUuid(),record.getCmdLine()!=null?record.getCmdLine().toString():null);
                if(record.getLocalPrincipal()!=null)
                    subjectToPrincipal.put(record.getUuid(),principal2Sid.get(record.getLocalPrincipal()));
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
        else if(record.getType() == SubjectType.SUBJECT_THREAD){
            try {
                subject2ThreadId.put(record.getUuid(), record.getCid());
                threadId2ProcessId.put(record.getCid(), subject2process.get(record.getParentSubject()));
                threadUUIDtoProcessUUID.put(record.getUuid(), record.getParentSubject());

            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
        if(record.getParentSubject()!=null)
            subject2Parent.put(record.getUuid(),record.getParentSubject());

        tempRecord.eventName = "";
    }

    private static void parseObject(TCCDMDatum datum,EventRecord tempRecord){
        String objectName=datum.getDatum().getClass().getName();
        if(objectName.endsWith("FileObject")) {
            FileObject record = (com.bbn.tc.schema.avro.cdm20.FileObject) datum.getDatum();
            fileObject2FilePath.put(record.getUuid(), "");
        }
        else if(objectName.endsWith("RegistryKeyObject")){
            RegistryKeyObject record = (com.bbn.tc.schema.avro.cdm20.RegistryKeyObject) datum.getDatum();
            registryObject2Path.put(record.getUuid(), record.getKey().toString());
        }
        else if(objectName.endsWith("NetFlowObject")){
            NetFlowObject record=(com.bbn.tc.schema.avro.cdm20.NetFlowObject)datum.getDatum();
            HashMap<String,String> params=new HashMap<>();
            params.put("remotePort",record.getRemotePort().toString());
            params.put("remoteAddress",record.getRemoteAddress().toString());
            params.put("localPort",record.getLocalPort().toString());
            params.put("localAddress",record.getLocalAddress().toString());
            params.put("ipProtocol",record.getIpProtocol().toString());
            networkObject2Parameters.put(record.getUuid(),params);
        }

        tempRecord.eventName = "";
    }

}

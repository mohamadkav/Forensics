package edu.nu.forensic.reducer;

import edu.nu.forensic.db.entity.Event;
import edu.nu.forensic.db.entity.Subject;
import edu.nu.forensic.etw.EtwEventType;

import java.util.*;

public class CPRStandAloneReducer {
    private Event lastSavedEvent = null;
    private HashMap<UUID, List<SubEvent>> eventsStartFromKey = new HashMap<>();
    private HashMap<UUID, List<SubEvent>> eventsEndAtKey = new HashMap<>();
    //private HashMap<String, HashSet<Integer>> fileToProcessesWhichHaveAccessedIt = new HashMap<>();
    private HashMap<UUID, HashMap<UUID,Stack<SubEvent>>> stackMaps = new HashMap<>();
    class SubEvent{
        private UUID u;
        private UUID v;
        private long timeStamp;
        public SubEvent(UUID u, UUID v, long timeStamp) {
            this.u = u;
            this.v = v;
            this.timeStamp = timeStamp;
        }
    }
    public boolean canBeRemoved(Subject subject){
        UUID u=subject.getParentSubject();
        UUID v=subject.getUuid();
        SubEvent event=new SubEvent(u,v,subject.getStartTimestampNanos());
        if(!eventsStartFromKey.containsKey(u))
            eventsStartFromKey.put(u,new ArrayList<>());
        eventsStartFromKey.get(u).add(event);
        if(!eventsEndAtKey.containsKey(v))
            eventsEndAtKey.put(v,new ArrayList<>());
        eventsEndAtKey.get(v).add(event);
        return canBeRemoved(event);
    }
    public boolean canBeRemoved(Event event){
        if(event.getTypeNum()!= EtwEventType.FileIoRead.ordinal() && event.getTypeNum()!= EtwEventType.FileIoWrite.ordinal())
            return false;
        if(lastSavedEvent==null) {
            lastSavedEvent = event;
            return false;
        }
        if (event.getNames().equals(lastSavedEvent.getNames()) && event.getPredicateObjectPath().equals(lastSavedEvent.getPredicateObjectPath())  && event.getThreadId().equals(lastSavedEvent.getThreadId()) ) {
            // update the ioEvent's time window.
            //toBeSavedIoEvent.setEndTimestampNanos(event.getTimestampNanos());
            return true;
        }
        // o.w., we hit another time window.
        else {
            boolean returnResult;
            SubEvent subEvent;
            if(event.getTypeNum()==EtwEventType.FileIoRead.ordinal()) {
                subEvent = new SubEvent(event.getId(), event.getSubjectUUID(), event.getTimestampNanos());
                UUID u=event.getId();
                UUID v=event.getSubjectUUID();
                if(!eventsStartFromKey.containsKey(u))
                    eventsStartFromKey.put(u,new ArrayList<>());
                eventsStartFromKey.get(u).add(subEvent);
                if(!eventsEndAtKey.containsKey(v))
                    eventsEndAtKey.put(v,new ArrayList<>());
                eventsEndAtKey.get(v).add(subEvent);
            }
            else { //e.g: write: if(event.getTypeNum()==EtwEventType.FileIoWrite.ordinal())
                subEvent = new SubEvent(event.getSubjectUUID(), event.getId(), event.getTimestampNanos());
                UUID u=event.getSubjectUUID();
                UUID v=event.getId();
                if(!eventsStartFromKey.containsKey(u))
                    eventsStartFromKey.put(u,new ArrayList<>());
                eventsStartFromKey.get(u).add(subEvent);
                if(!eventsEndAtKey.containsKey(v))
                    eventsEndAtKey.put(v,new ArrayList<>());
                eventsEndAtKey.get(v).add(subEvent);
            }
            returnResult=canBeRemoved(subEvent);
            lastSavedEvent=event;
            return returnResult;
        }

    }
    private boolean canBeRemoved(SubEvent e){
        if(!stackMaps.containsKey(e.u)) {
            HashMap<UUID,Stack<SubEvent>> newMap=new HashMap<>();
            Stack<SubEvent> newStack=new Stack<>();
            newStack.push(e);
            newMap.put(e.v,newStack);
            stackMaps.put(e.u,newMap);
            return false;
        }
        SubEvent e_p=stackMaps.get(e.u).get(e.v).pop();
        if(forward_check(e_p,e,e.v)&&backward_check(e_p,e,e.u)){
            //merge(); We actually preserve the shadowed event and remove the other one.
            stackMaps.get(e.u).get(e.v).push(e_p);
            return true;
        }
        else
            stackMaps.get(e.u).get(e.v).push(e);
        return false;
    }

/*    public void notifyUUIDDelete(UUID fileUUID){ //Results in dependency explosion
        eventsStartFromKey.remove(fileUUID);
        eventsEndAtKey.remove(fileUUID);
        stackMaps.remove(fileUUID);
    }*/
/*    public void notifyFileRename(UUID oldFileName, String newFileName){
        if(eventsEndAtKey.containsKey(oldFileName)){
            eventsEndAtKey.put(newFileName,eventsEndAtKey.get(oldFileName));
            eventsEndAtKey.remove(oldFileName);
        }
        if(eventsStartFromKey.containsKey(oldFileName)){
            eventsStartFromKey.put(newFileName,eventsStartFromKey.get(oldFileName));
            eventsStartFromKey.remove(oldFileName);
        }
        if(fileToProcessesWhichHaveAccessedIt.containsKey(oldFileName)){
            fileToProcessesWhichHaveAccessedIt.put(newFileName,fileToProcessesWhichHaveAccessedIt.get(oldFileName));
            fileToProcessesWhichHaveAccessedIt.remove(oldFileName);
        }
        if(stackMaps.containsKey(oldFileName)){
            stackMaps.put(newFileName,stackMaps.get(oldFileName));
            stackMaps.remove(oldFileName);
        }
    }*/
    /*private boolean doStep0(Event event){
        if (!eventsStartFromKey.containsKey(event.getPredicateObjectPath())) {
            HashSet<Event> ioEventSubset = new HashSet<>();
            eventsStartFromKey.put(event.getPredicateObjectPath(), ioEventSubset);
        }
        if (!eventsEndAtKey.containsKey(event.getPredicateObjectPath())) {
            HashSet<Event> ioEventSubset = new HashSet<>();
            eventsEndAtKey.put(event.getPredicateObjectPath(), ioEventSubset);
        }
        if (!eventsStartFromKey.containsKey(event.getThreadId().toString())) {
            HashSet<Event> ioEventSubset = new HashSet<>();
            eventsStartFromKey.put(event.getThreadId().toString(), ioEventSubset);
        }
        if (!eventsEndAtKey.containsKey(event.getThreadId().toString())) {
            HashSet<Event> ioEventSubset = new HashSet<>();
            eventsEndAtKey.put(event.getThreadId().toString(), ioEventSubset);
        }
        if (event.getTypeNum()== EtwEventType.FileIoRead.ordinal() ) {
            eventsStartFromKey.get(event.getPredicateObjectPath()).add(event);
            eventsEndAtKey.get(event.getThreadId().toString()).add(event);
        }
        else if (event.getTypeNum() == EtwEventType.FileIoWrite.ordinal()) {
            eventsStartFromKey.get(event.getThreadId().toString()).add(event);
            eventsEndAtKey.get(event.getPredicateObjectPath()).add(event);
        }
        return doStep1(event);
    }
    private boolean doStep1(Event event) {
        if (fileToProcessesWhichHaveAccessedIt.containsKey(event.getPredicateObjectPath())) {
            HashSet<Integer> threadSet = fileToProcessesWhichHaveAccessedIt.get(event.getPredicateObjectPath());
            if (!threadSet.contains(event.getThreadId())) {
                threadSet.add(event.getThreadId());
            }
        }
        else {
            HashSet<Integer> threadSet = new HashSet<>();
            threadSet.add(event.getThreadId());
            fileToProcessesWhichHaveAccessedIt.put(event.getPredicateObjectPath(), threadSet);
        }

        String file = event.getPredicateObjectPath();
        String thread = event.getThreadId().toString();
        // create stack(file, thread)
        if (!stackMaps.containsKey(file)) {
            HashMap<String, Stack<Event>> stackMap = new HashMap<>();
            stackMaps.put(file, stackMap);
        }
        HashMap<String, Stack<Event>> stackMap = stackMaps.get(file);
        if (!stackMap.containsKey(thread)) {
            Stack<Event> stack = new Stack<>();
            stackMap.put(thread, stack);
        }
        // create stack(thread, file)
        if (!stackMaps.containsKey(thread)) {
            stackMap = new HashMap<>();
            stackMaps.put(thread, stackMap);
        }
        stackMap = stackMaps.get(thread);
        if (!stackMap.containsKey(file)) {
            Stack<Event> stack = new Stack<>();
            stackMap.put(file, stack);
        }
        return doStep2(event);
    }
    private boolean doStep2(Event event) {
        String u;
        String v;
        if (event.getTypeNum()!= EtwEventType.FileIoRead.ordinal()) {
            // fetch stack(file, thread)
            u = event.getPredicateObjectPath();
            v = event.getThreadId().toString();
        }
        else{ // FileIoWrite // fetch stack(thread, file)
            u = event.getThreadId().toString();
            v = event.getPredicateObjectPath();
        }
        Stack<Event> stack = stackMaps.get(u).get(v);
        if (stack.empty()) {
            stack.push(event);
            return false;
        }
        else {
            Event priorEvent = stack.pop();
            if (forward_check(priorEvent, event, v) && backward_check(priorEvent, event, u)) {
                // priorEvent may be different from event in ioEventSet?
                // tune attributes of priorEvent ?
                stack.push(priorEvent);
                return true;
            }
            else {
                stack.push(event);
                return false;
            }
        }
    }*/
    private boolean forward_check(SubEvent e_p, SubEvent e_l, UUID v) {
        //System.out.println("eventsStartFromKey.get(v).size() = "+ eventsStartFromKey.get(v).size());
        for (SubEvent subEvent : eventsStartFromKey.get(v)) {
            // time window of this event has overlapped the start time interval of e_p and e_l
            if (!(subEvent.timeStamp <= e_p.timeStamp || e_l.timeStamp <= subEvent.timeStamp))
                return false;
        }
        //System.out.println("get forward");
        return true;
    }

    private boolean backward_check(SubEvent e_p, SubEvent e_l, UUID u) {
        // System.out.println("eventsEndAtKey.get(u).size() = "+ eventsEndAtKey.get(u).size());
        for (SubEvent subEvent : eventsEndAtKey.get(u)) {
            // time window of this event has overlapped the end time interval of e_p and e_l
            if (!(subEvent.timeStamp <= e_p.timeStamp || e_l.timeStamp <= subEvent.timeStamp))
                return false;
        }
        //System.out.println("get backward");
        return true;
    }
}

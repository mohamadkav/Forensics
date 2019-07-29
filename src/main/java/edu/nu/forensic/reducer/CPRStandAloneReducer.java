package edu.nu.forensic.reducer;

import edu.nu.forensic.db.entity.Event;
import edu.nu.forensic.etw.EtwEventType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

public class CPRStandAloneReducer {
    private Event lastSavedEvent = null;
    private HashMap<String, HashSet<Event>> eventsStartFromKey = new HashMap<>();
    private HashMap<String, HashSet<Event>> eventsEndAtKey = new HashMap<>();
    private HashMap<String, HashSet<Integer>> fileToProcessesWhichHaveAccessedIt = new HashMap<>();
    private HashMap<String, HashMap<String, Stack<Event>>> stackMaps = new HashMap<>();

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
            boolean returnResult=doStep0(lastSavedEvent);
            lastSavedEvent=event;
            return returnResult;
        }

    }

    public void notifyFileDelete(String filename){
        eventsStartFromKey.remove(filename);
        eventsEndAtKey.remove(filename);
        fileToProcessesWhichHaveAccessedIt.remove(filename);
        stackMaps.remove(filename);
    }
    public void notifyFileRename(String oldFileName, String newFileName){
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
    }
    private boolean doStep0(Event event){
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
    }
    private boolean forward_check(Event e_p, Event e_l, String v) {
        //System.out.println("eventsStartFromKey.get(v).size() = "+ eventsStartFromKey.get(v).size());
        for (Event ioEvent : eventsStartFromKey.get(v)) {
            // time window of this event has overlapped the start time interval of e_p and e_l
            if (!(ioEvent.getTimestampNanos() <= e_p.getTimestampNanos() || e_l.getTimestampNanos() <= ioEvent.getTimestampNanos()))
                return false;
        }
        //System.out.println("get forward");
        return true;
    }

    private boolean backward_check(Event e_p, Event e_l, String u) {
        // System.out.println("eventsEndAtKey.get(u).size() = "+ eventsEndAtKey.get(u).size());
        for (Event ioEvent : eventsEndAtKey.get(u)) {
            // time window of this event has overlapped the end time interval of e_p and e_l
            if (!(ioEvent.getTimestampNanos() <= e_p.getTimestampNanos() || e_l.getTimestampNanos() <= ioEvent.getTimestampNanos()))
                return false;
        }
        //System.out.println("get backward");
        return true;
    }
}

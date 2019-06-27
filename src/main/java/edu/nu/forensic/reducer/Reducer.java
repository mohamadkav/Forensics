package edu.nu.forensic.reducer;


import edu.nu.forensic.db.entity.*;
import edu.nu.forensic.db.entity.Object;
import edu.nu.forensic.db.repository.EventRepository;
import edu.nu.forensic.db.repository.IoEventAfterCPRRepository;
import edu.nu.forensic.db.repository.IoEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import java.util.*;
import java.util.concurrent.TimeUnit;


@Component
public class Reducer {
    @Autowired
    private EntityManager entityManager;

    @Autowired
    EventRepository eventRepository;

    @Autowired
    IoEventRepository ioEventRepository;

    @Autowired
    IoEventAfterCPRRepository ioEventAfterCPRRepository;

    @Autowired
    NodeMergeReducer nodeMergeReducer;


    private Set<IoEvent> ioEventSet = new HashSet<>();
    private Set<IoEventAfterCPR> ioEventAfterCPRSet = new HashSet<>();
    private List<IoEvent> toBeSavedIoEvents = new ArrayList<>();
    private HashMap<String, HashSet<Integer>> fileToProcessesWhichHaveAccessedIt = new HashMap<>();
    // CPR optimization
    private HashMap<String, HashSet<IoEvent>> eventsStartFromKey = new HashMap();
    private HashMap<String, HashSet<IoEvent>> eventsEndAtKey = new HashMap();

    private boolean forward_check(IoEvent e_p, IoEvent e_l, String v) {
        //System.out.println("eventsStartFromKey.get(v).size() = "+ eventsStartFromKey.get(v).size());
        for (IoEvent ioEvent : eventsStartFromKey.get(v)) {
            // time window of this event has overlapped the start time interval of e_p and e_l
            if (!(ioEvent.getEndTimestampNanos() <= e_p.getStartTimestampNanos() || e_l.getStartTimestampNanos() <= ioEvent.getStartTimestampNanos()))
                return false;
        }
        //System.out.println("get forward");
        return true;
    }

    private boolean backward_check(IoEvent e_p, IoEvent e_l, String u) {
       // System.out.println("eventsEndAtKey.get(u).size() = "+ eventsEndAtKey.get(u).size());
        for (IoEvent ioEvent : eventsEndAtKey.get(u)) {
            // time window of this event has overlapped the end time interval of e_p and e_l
            if (!(ioEvent.getEndTimestampNanos() <= e_p.getEndTimestampNanos() || e_l.getEndTimestampNanos() <= ioEvent.getStartTimestampNanos()))
                return false;
        }
        //System.out.println("get backward");
        return true;
    }

    public void reduce(){
        System.out.println("Fetching events from database...");
        Event firstEvent = eventRepository.findTop1ByOrderByTimestampNanosAsc();
        Event lastEvent = eventRepository.findTop1ByOrderByTimestampNanosDesc();
        System.out.println("Begin at: "+ firstEvent.getTimestampNanos());
        System.out.println("End at: "+ lastEvent.getTimestampNanos());
        long timeSpan = lastEvent.getTimestampNanos() - firstEvent.getTimestampNanos();
        int chunkNum = 100;
        long chunkSpan = timeSpan / chunkNum;


        // these three cols are used to decide whether two adjacent events are the same.
        String lastNames = "";
        String lastPredicateObjectPath = "";
        String lastThreadId = "";
        // this is a merged ioEvent of one or more events.
        IoEvent toBeSavedIoEvent = null;
        // this is a flag where true indicates that there is no time swindow before current one.
        boolean noWindow = true;

        long counter = 0;
        for (int i = 0; i < 100; i++){
            entityManager.clear();
            Set<Event> eventSubSet = eventRepository.findMyEvents(firstEvent.getTimestampNanos()+i*chunkSpan, firstEvent.getTimestampNanos() + (i+1)*chunkSpan);
            System.out.println("id = " + i +  ", eventSubSet size = " + eventSubSet.size());
            counter += eventSubSet.size();

            // merge all io events into ioEvents by timestamp.
            for (Event event : eventSubSet) {
                // if current event has same type, object_path, thread_id as the last event, we are still in same time window.
                if (event.getNames().equals(lastNames) && event.getPredicateObjectPath().equals(lastPredicateObjectPath)  && event.getThreadId().toString().equals(lastThreadId) ) {
                    // update the ioEvent's time window.
                    toBeSavedIoEvent.setEndTimestampNanos(event.getTimestampNanos());
                }
                // o.w., we hit another time window.
                else {
                    if (noWindow) {
                        noWindow = false;
                    }
                    else {
                        //toBeSavedIoEvents.add(toBeSavedIoEvent);
                        ioEventSet.add(toBeSavedIoEvent);
                    }
                    // create a new ioEvent
                    toBeSavedIoEvent = new IoEvent(event.getId(), event.getType(), event.getThreadId(), event.getSubjectUUID(), event.getPredicateObjectPath(),event.getTimestampNanos(), event.getTimestampNanos(), event.getNames());
                    lastNames = event.getNames();
                    lastPredicateObjectPath = event.getPredicateObjectPath();
                    lastThreadId = event.getThreadId().toString();
                }
                //if(toBeSavedIoEvents.size() >= 1000) {
                    //System.out.println("Saving 1000 ioEvents...");
                    //ioEventRepository.saveAll(toBeSavedIoEvents);
                    //toBeSavedIoEvents = new ArrayList<>();
                //}
            }
          //  System.out.println("ioEventSet.size() = " + ioEventSet.size());
        }
        //toBeSavedIoEvents.add(toBeSavedIoEvent);
        ioEventSet.add(toBeSavedIoEvent);
        //System.out.println("Saving the rest ioEvents...");
        //ioEventRepository.saveAll(toBeSavedIoEvents);

        System.out.println("ioEventSet.size() = " + ioEventSet.size());
        System.out.println("counter = " + counter);

        // CPR algorithm
        // step 0. CPR optimization
        System.out.println("step 0 ...");
        for (IoEvent ioEvent: ioEventSet) {
            if (!eventsStartFromKey.containsKey(ioEvent.getPredicateObjectPath())) {
                HashSet<IoEvent> ioEventSubset = new HashSet<>();
                eventsStartFromKey.put(ioEvent.getPredicateObjectPath(), ioEventSubset);
            }
            if (!eventsEndAtKey.containsKey(ioEvent.getPredicateObjectPath())) {
                HashSet<IoEvent> ioEventSubset = new HashSet<>();
                eventsEndAtKey.put(ioEvent.getPredicateObjectPath(), ioEventSubset);
            }
            if (!eventsStartFromKey.containsKey(ioEvent.getThreadId().toString())) {
                HashSet<IoEvent> ioEventSubset = new HashSet<>();
                eventsStartFromKey.put(ioEvent.getThreadId().toString(), ioEventSubset);
            }
            if (!eventsEndAtKey.containsKey(ioEvent.getThreadId().toString())) {
                HashSet<IoEvent> ioEventSubset = new HashSet<>();
                eventsEndAtKey.put(ioEvent.getThreadId().toString(), ioEventSubset);
            }
        }
        for (IoEvent ioEvent: ioEventSet) {
            if (ioEvent.getNames().equals("FileIoRead") ) {
                eventsStartFromKey.get(ioEvent.getPredicateObjectPath()).add(ioEvent);
                eventsEndAtKey.get(ioEvent.getThreadId().toString()).add(ioEvent);
            }
            else if (ioEvent.getNames().equals("FileIoWrite") ) {
                eventsStartFromKey.get(ioEvent.getThreadId().toString()).add(ioEvent);
                eventsEndAtKey.get(ioEvent.getPredicateObjectPath()).add(ioEvent);
            }
        }
        // step 1. find all possible edges.
        System.out.println("step 1 ...");
        for (IoEvent ioEvent : ioEventSet) {
            if (fileToProcessesWhichHaveAccessedIt.containsKey(ioEvent.getPredicateObjectPath())) {
                HashSet<Integer> threadSet = fileToProcessesWhichHaveAccessedIt.get(ioEvent.getPredicateObjectPath());
                if (!threadSet.contains(ioEvent.getThreadId())) {
                    threadSet.add(ioEvent.getThreadId());
                }
            }
            else {
                HashSet<Integer> threadSet = new HashSet<>();
                threadSet.add(ioEvent.getThreadId());
                fileToProcessesWhichHaveAccessedIt.put(ioEvent.getPredicateObjectPath(), threadSet);
            }
        }
        // step 2. prepare stacks for all possible edges.
        System.out.println("step 2 ...");
        HashMap<String, HashMap<String, Stack<IoEvent>>> stackMaps = new HashMap<>();
        fileToProcessesWhichHaveAccessedIt.forEach((key, value) -> {
            value.forEach((threadId) -> {
                String file = key;
                String thread = threadId.toString();
                // create stack(file, thread)
                if (!stackMaps.containsKey(file)) {
                    HashMap<String, Stack<IoEvent>> stackMap = new HashMap<>();
                    stackMaps.put(file, stackMap);
                }
                HashMap<String, Stack<IoEvent>> stackMap = stackMaps.get(file);
                if (!stackMap.containsKey(thread)) {
                    Stack<IoEvent> stack = new Stack<>();
                    stackMap.put(thread, stack);
                }
                // create stack(thread, file)
                if (!stackMaps.containsKey(thread)) {
                    stackMap = new HashMap<>();
                    stackMaps.put(thread, stackMap);
                }
                stackMap = stackMaps.get(thread);
                if (!stackMap.containsKey(file)) {
                    Stack<IoEvent> stack = new Stack<>();
                    stackMap.put(file, stack);
                }
            });
        });
        // step 3. merge aggregable events.
        System.out.println("step 3 ...");
        //int counter = 0;
        Set<IoEvent> toBeRemovedEventSet = new HashSet<>();
        for (IoEvent ioEvent : ioEventSet) {
            // debug
            //counter += 1;
            //System.out.println(counter);
            String u;
            String v;
            if (ioEvent.getNames().equals("FileIoRead") ) {
                // fetch stack(file, thread)
                u = ioEvent.getPredicateObjectPath();
                v = ioEvent.getThreadId().toString();
            }
            else{ // FileIoWrite // fetch stack(thread, file)
                u = ioEvent.getThreadId().toString();
                v = ioEvent.getPredicateObjectPath();
            }
            Stack<IoEvent> stack = stackMaps.get(u).get(v);
            if (stack.empty()) {
                stack.push(ioEvent);
            }
            else {
                IoEvent priorEvent = stack.pop();
                if (forward_check(priorEvent, ioEvent, v) && backward_check(priorEvent, ioEvent, u)) {
                    // priorEvent may be different from event in ioEventSet?
                    priorEvent.setEndTimestampNanos(ioEvent.getEndTimestampNanos());
                    // tune attributes of priorEvent ?
                    toBeRemovedEventSet.add(ioEvent);
                    stack.push(priorEvent);
                }
                else {
                    stack.push(ioEvent);
                }
            }
        }
        System.out.println("Before remove we have:");
        System.out.println(ioEventSet.size());
        ioEventRepository.deleteAll();
      //  ioEventRepository.saveAll(ioEventSet);
        for (IoEvent toBeRemovedEvent : toBeRemovedEventSet) {
            ioEventSet.remove(toBeRemovedEvent);
        }
        for (IoEvent ioEvent: ioEventSet) {
            IoEventAfterCPR ioEventAfterCPR = new IoEventAfterCPR(ioEvent.getId(), ioEvent.getType(), ioEvent.getThreadId(), ioEvent.getSubjectUUID(), ioEvent.getPredicateObjectPath(),ioEvent.getStartTimestampNanos(), ioEvent.getEndTimestampNanos(), ioEvent.getNames());
            if(!nodeMergeReducer.reduce(ioEventAfterCPR))
                ioEventAfterCPRSet.add(ioEventAfterCPR);
        }
        System.out.println("After remove we have");
        System.out.println(ioEventAfterCPRSet.size());
        ioEventAfterCPRRepository.deleteAll();
        ioEventAfterCPRRepository.saveAll(ioEventAfterCPRSet);
        System.out.println("DONE");

    }
}